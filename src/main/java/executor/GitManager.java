package executor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

/**
 * Manages all Git-related operations for the PythonExecutor application.
 */
public class GitManager {

    /** The main application frame, used as a parent for dialogs. */
    private final JFrame parentFrame;
    /** The service to run background Git commands without blocking the UI. */
    private final ExecutorService executorService;
    /** A consumer for logging output to the main application's output area. */
    private final Consumer<String> logger;
    /** The currently selected directory for scripts, used to find a Git repository. */
    private final Path scriptDirectory;
    /** The currently selected directory for input files, used to find a Git repository. */
    private final Path inputDirectory;

    /**
     * Constructs a new GitManager.
     *
     * @param parentFrame     The main application frame, used for parenting dialogs.
     * @param executorService The shared executor service for running background tasks.
     * @param logger          A consumer function to log messages to the application's UI.
     * @param scriptDirectory The current script directory path.
     * @param inputDirectory  The current input directory path.
     */
    public GitManager(JFrame parentFrame, ExecutorService executorService, Consumer<String> logger, Path scriptDirectory, Path inputDirectory) {
        this.parentFrame = parentFrame;
        this.executorService = executorService;
        this.logger = logger;
        this.scriptDirectory = scriptDirectory;
        this.inputDirectory = inputDirectory;
    }

    /**
     * Creates a TitledBorder with a bold and slightly larger font.
     * @param title The title for the border.
     * @return A {@link TitledBorder} with custom styling.
     */
    private TitledBorder createBoldTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        Font currentFont = border.getTitleFont();
        if (currentFont != null) {
            border.setTitleFont(currentFont.deriveFont(Font.BOLD, currentFont.getSize() + 4f));
        }
        return border;
    }

    /**
     * Finds the root directory of a Git repository by searching upwards from
     * the currently selected script or input directories.
     *
     * @return The {@link Path} to the Git repository root, or {@code null} if
     * not found.
     */
    private Path findGitRepository() {
        // Prioritize script directory, then input directory
        Path[] pathsToSearch = {scriptDirectory, inputDirectory};
        for (Path startPath : pathsToSearch) {
            Path current = startPath;
            while (current != null) {
                if (Files.isDirectory(current.resolve(".git"))) {
                    return current; // Found the root of the git repo
                }
                current = current.getParent(); // Move up one directory
            }
        }
        return null; // Not found
    }

    /**
     * Initiates the Git commit process by checking for staged files and then
     * opening a dialog for the user to enter a commit message.
     */
    public void showGitCommitDialog() {
        executorService.submit(() -> {
            Path repoPath = findGitRepository();
            if (repoPath == null) {
                SwingUtilities.invokeLater(() -> logger.accept(String.format("\n[%s] [ERROR] No .git repository found to commit to.", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))));
                return;
            }

            try {
                // Get the list of staged files
                ProcessBuilder pb = new ProcessBuilder("git", "diff", "--name-only", "--cached");
                pb.directory(repoPath.toFile());
                Process process = pb.start();

                List<String> stagedFiles = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach(stagedFiles::add);
                }
                process.waitFor();

                if (stagedFiles.isEmpty()) {
                    SwingUtilities.invokeLater(() -> logger.accept(String.format("\n[%s] Nothing to commit. Use 'Git Add' to stage files first.", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))));
                    return;
                }

                // Show the commit dialog on the EDT
                SwingUtilities.invokeLater(() -> createAndShowCommitDialog(repoPath, stagedFiles));

            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] Error checking for staged files: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates and displays a dialog for committing staged files.
     *
     * @param repoPath The path to the Git repository.
     * @param stagedFiles A list of files that are currently staged and will be
     * committed.
     */
    private void createAndShowCommitDialog(Path repoPath, List<String> stagedFiles) {
        JDialog dialog = new JDialog(parentFrame, "Commit Staged Files", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(parentFrame);

        // Text area for staged files (read-only)
        JTextArea stagedFilesArea = new JTextArea(String.join("\n", stagedFiles));
        stagedFilesArea.setEditable(false);
        stagedFilesArea.setBorder(createBoldTitledBorder("Files to be Committed "));
        JScrollPane stagedScrollPane = new JScrollPane(stagedFilesArea);

        // Text area for commit message
        JTextArea commitMessageArea = new JTextArea();
        commitMessageArea.setBorder(createBoldTitledBorder("Commit Message "));
        JScrollPane commitMessageScrollPane = new JScrollPane(commitMessageArea);

        // Split pane to separate staged files and commit message
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stagedScrollPane, commitMessageScrollPane);
        splitPane.setResizeWeight(0.4);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton commitBtn = new JButton("Commit");
        JButton cancelBtn = new JButton("Cancel");

        commitBtn.addActionListener(e -> {
            String commitMessage = commitMessageArea.getText().trim();
            if (commitMessage.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Commit message cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            runGitCommit(repoPath, commitMessage);
            dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(commitBtn);

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(splitPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Executes the `git commit` command with the provided message.
     *
     * @param repoPath The path to the Git repository.
     * @param message The commit message.
     */
    private void runGitCommit(Path repoPath, String message) {
        executorService.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "commit", "-m", message);
                pb.directory(repoPath.toFile());
                Process process = pb.start();
                process.waitFor();
                SwingUtilities.invokeLater(() -> logger.accept(String.format("\n[%s] Commit successful.", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))));
            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] Error running git commit: " + e.getMessage()));
            }
        });
    }

    /**
     * Executes the `git pull` command in the determined repository directory.
     */
    public void runGitPull() {
        executorService.submit(() -> {
            Path repoPath = findGitRepository();
            if (repoPath == null) {
                SwingUtilities.invokeLater(() -> {
                    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                    logger.accept(String.format("\n[%s] [ERROR] No .git repository found to pull from.", timestamp));
                });
                return;
            }

            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            SwingUtilities.invokeLater(() -> logger.accept(String.format("\n\n[%s] Attempting to pull from remote...\n", timestamp)));

            try {
                ProcessBuilder pb = new ProcessBuilder("git", "pull");
                pb.directory(repoPath.toFile());
                Process process = pb.start();

                // Capture and display output in real-time
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        SwingUtilities.invokeLater(() -> logger.accept(outputLine + "\n"));
                    }
                    while ((line = errorReader.readLine()) != null) {
                        final String errorLine = line;
                        SwingUtilities.invokeLater(() -> logger.accept("[GIT PULL] " + errorLine + "\n"));
                    }
                }

                int exitCode = process.waitFor();
                final int finalExitCode = exitCode;
                SwingUtilities.invokeLater(() -> logger.accept(String.format("\n--- Git pull finished with exit code %d. ---\n", finalExitCode)));

            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] An error occurred during git pull: " + e.getMessage() + "\n"));
            }
        });
    }

    /**
     * Executes the `git push` command in the determined repository directory.
     */
    public void runGitPush() {
        executorService.submit(() -> {
            Path repoPath = findGitRepository();
            if (repoPath == null) {
                SwingUtilities.invokeLater(() -> {
                    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                    logger.accept(String.format("\n[%s] [ERROR] No .git repository found to push from.", timestamp));
                });
                return;
            }

            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            SwingUtilities.invokeLater(() -> logger.accept(String.format("\n\n[%s] Attempting to push to remote...\n", timestamp)));

            try {
                ProcessBuilder pb = new ProcessBuilder("git", "push");
                pb.directory(repoPath.toFile());
                Process process = pb.start();

                // Capture and display output in real-time, as push can be interactive or slow
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        SwingUtilities.invokeLater(() -> logger.accept(outputLine + "\n"));
                    }
                    while ((line = errorReader.readLine()) != null) {
                        final String errorLine = line;
                        SwingUtilities.invokeLater(() -> logger.accept("[GIT PUSH] " + errorLine + "\n"));
                    }
                }
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] An error occurred during git push: " + e.getMessage() + "\n"));
            }
        });
    }

    /**
     * Initiates the `git add` process by checking for uncommitted changes and
     * displaying a dialog for the user to select which files to stage.
     */
    public void showGitAddDialog() {
        executorService.submit(() -> {
            Path repoPath = findGitRepository();
            if (repoPath == null) {
                SwingUtilities.invokeLater(() -> {
                    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                    logger.accept(String.format("\n[%s] [ERROR] No .git repository found to add files from.", timestamp));
                });
                return;
            }

            try {
                ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
                pb.directory(repoPath.toFile());
                Process process = pb.start();

                List<String> changedFiles = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // The file path always starts at the 4th character (index 3)
                        changedFiles.add(line.substring(3));
                    }
                }
                process.waitFor();

                if (changedFiles.isEmpty()) {
                    final Path finalRepoPath = repoPath;
                    SwingUtilities.invokeLater(() -> {
                        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                        logger.accept(String.format("\n[%s] No changes to add in repository: %s", timestamp, finalRepoPath.getFileName()));
                    });
                    return;
                }

                // Create and show the dialog on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> createAndShowAddDialog(repoPath, changedFiles));

            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] Error getting git status: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates and displays a dialog allowing the user to select uncommitted
     * files to stage.
     *
     * @param repoPath The path to the Git repository.
     * @param files A list of uncommitted files.
     */
    private void createAndShowAddDialog(Path repoPath, List<String> files) {
        String dialogTitle = String.format("Add Files to Staging (%d uncommitted files)", files.size());
        JDialog dialog = new JDialog(parentFrame, "Add Files to Staging", true); // Title bar of the window
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parentFrame);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setBorder(createBoldTitledBorder(dialogTitle));
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (String file : files) {
            JCheckBox cb = new JCheckBox(file);
            cb.setIconTextGap(10); // Add space between the checkbox and the file name text
            checkBoxes.add(cb);
            checkboxPanel.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Add Selected Files");
        JButton cancelBtn = new JButton("Cancel");

        addBtn.addActionListener(e -> {
            List<String> filesToAdd = new ArrayList<>();
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    filesToAdd.add(cb.getText());
                }
            }
            if (!filesToAdd.isEmpty()) {
                runGitAdd(repoPath, filesToAdd);
            }
            dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(addBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Executes the `git add` command for the specified list of files.
     *
     * @param repoPath The path to the Git repository.
     * @param filesToAdd A list of file paths (relative to the repo root) to add
     * to staging.
     */
    private void runGitAdd(Path repoPath, List<String> filesToAdd) {
        executorService.submit(() -> {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            try {
                List<String> command = new ArrayList<>();
                command.add("git");
                command.add("add");
                command.addAll(filesToAdd);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(repoPath.toFile());
                Process process = pb.start();
                process.waitFor();

                SwingUtilities.invokeLater(() -> logger.accept(String.format("\n[%s] Added %d file(s) to staging.", timestamp, filesToAdd.size())));
            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] Error running git add: " + e.getMessage()));
            }
        });
    }

    /**
     * Opens a Git Bash terminal window in the appropriate working directory
     * (either the Git repository root or the selected script folder).
     */
    public void openGitBash() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            logger.accept(String.format("\n[%s] [INFO] Git Bash is a Windows-specific feature. On macOS/Linux, please use your system's terminal.", timestamp));
            return;
        }

        // Check if any directory has been selected
        if (scriptDirectory == null && inputDirectory == null) {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            logger.accept(String.format("\n[%s] [ERROR] No working directory selected. Please select a script or input folder first.", timestamp));
            return;
        }

        // Determine the working directory for Git Bash
        Path repoPath = findGitRepository();
        File workingDirectory;
        String locationMessage;

        if (repoPath != null) {
            workingDirectory = repoPath.toFile();
            locationMessage = String.format("in Git repository: %s", workingDirectory.getAbsolutePath());
        } else {
            // Fallback if no .git directory is found
            workingDirectory = (scriptDirectory != null) ? scriptDirectory.toFile() : new File(System.getProperty("user.home"));
            locationMessage = String.format("in directory: %s (No .git repo found)", workingDirectory.getAbsolutePath());
        }

        // Find the Git Bash executable
        String[] potentialPaths = {
            "C:\\Program Files\\Git\\git-bash.exe",
            "C:\\Program Files (x86)\\Git\\git-bash.exe",
            System.getenv("ProgramFiles") + "\\Git\\git-bash.exe",
            System.getenv("ProgramFiles(x86)") + "\\Git\\git-bash.exe"
        };

        File gitBashExe = null;
        for (String path : potentialPaths) {
            if (path == null) continue;
            File f = new File(path);
            if (f.exists()) {
                gitBashExe = f;
                break;
            }
        }

        try {
            String gitBashCommand;
            if (gitBashExe == null) {
                // If not found in common locations, assume it's in the PATH.
                gitBashCommand = "git-bash.exe";
            } else {
                // If found, use its full, quoted path.
                gitBashCommand = "\"" + gitBashExe.getAbsolutePath() + "\"";
            }
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "Git Bash", gitBashCommand);
            pb.directory(workingDirectory);
            pb.start();
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            logger.accept(String.format("\n[%s] Opening Git Bash %s", timestamp, locationMessage));
        } catch (IOException e) {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            logger.accept(String.format("\n[%s] [ERROR] Could not open Git Bash. Make sure it is installed and accessible.\nGo to https://git-scm.com/downloads to install it. ", timestamp));
            e.printStackTrace();
        }
    }

    /**
     * Opens a new terminal window and displays the `git log` for the current
     * repository.
     */
    public void showGitLog() {
        executorService.submit(() -> {
            Path repoPath = findGitRepository();
            if (repoPath == null) {
                SwingUtilities.invokeLater(() -> {
                    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                    logger.accept(String.format("\n[%s] [ERROR] No .git repository found in the hierarchy of the selected script or input directories.", timestamp));
                });
                return;
            }

            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    // On Windows, 'start' opens a new window. 'git log' is passed as the initial command.
                    new ProcessBuilder("cmd.exe", "/c", "start", "git", "log").directory(repoPath.toFile()).start();
                } else if (os.contains("mac")) {
                    // On macOS, we can use osascript to tell the Terminal app to run the command.
                    String command = String.format("tell app \"Terminal\" to do script \"cd %s && git log\"", repoPath.toAbsolutePath());
                    new ProcessBuilder("osascript", "-e", command).start();
                } else { // Assume Linux/other Unix
                    // For Linux, we try to open with x-terminal-emulator, which is a common default.
                    new ProcessBuilder("x-terminal-emulator", "-e", "sh -c 'git log | less'").directory(repoPath.toFile()).start();
                }
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                SwingUtilities.invokeLater(() -> logger.accept(String.format("\n[%s] Opened interactive git log in a new terminal window.", timestamp)));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> logger.accept("\n[FATAL] Could not open terminal for git log: " + e.getMessage() + "\n"));
                e.printStackTrace();
            }
        });
    }
}