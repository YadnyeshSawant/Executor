package executor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * A dialog window for managing application settings.
 */
public class SettingsDialog extends JDialog {

    /** The main application frame, used for accessing and modifying settings. */
    private final PythonExecutor executor;

    /**
     * Constructs the settings dialog.
     *
     * @param owner The main {@link PythonExecutor} application frame, which owns
     *              this dialog and whose settings will be modified.
     */
    public SettingsDialog(PythonExecutor owner) {
        super(owner, "Settings", true);
        this.executor = owner;

        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(owner);

        // Panels for settings
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Buttons for the left panel
        JButton shortcutsButton = new JButton("Shortcuts");
        JButton themesButton = new JButton("Themes");
        JButton fontSizeButton = new JButton("Font Size");
        JButton languageButton = new JButton("Language");
        JButton generalButton = new JButton("General");

        // Set a uniform height for the buttons and allow them to stretch horizontally
        Dimension buttonSize = new Dimension(Short.MAX_VALUE, 35);
        for (JButton btn : Arrays.asList(shortcutsButton, themesButton, fontSizeButton, languageButton, generalButton)) {
            btn.setMaximumSize(buttonSize);
            leftPanel.add(btn);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
        }

        // Panels for settings
        final CardLayout cardLayout = new CardLayout();
        final JPanel rightPanel = new JPanel(cardLayout);

        // --- Create the different settings panels ---
        JPanel shortcutsPanel = createShortcutsPanel();
        JPanel themesPanel = new JPanel(); // Placeholder
        themesPanel.add(new JLabel("Theme settings will be coming soon."));
        JPanel fontPanel = createFontSizePanel();
        JPanel langPanel = new JPanel(); // Placeholder
        langPanel.add(new JLabel("Language settings will be coming soon."));
        JPanel generalPanel = createGeneralSettingsPanel();

        // Add panels to the CardLayout
        rightPanel.add(shortcutsPanel, "Shortcuts");
        rightPanel.add(themesPanel, "Themes");
        rightPanel.add(fontPanel, "Font Size");
        rightPanel.add(langPanel, "Language");
        rightPanel.add(generalPanel, "General");

        // Add action listeners to the buttons to switch cards
        shortcutsButton.addActionListener(e -> cardLayout.show(rightPanel, "Shortcuts"));
        themesButton.addActionListener(e -> cardLayout.show(rightPanel, "Themes"));
        fontSizeButton.addActionListener(e -> cardLayout.show(rightPanel, "Font Size"));
        languageButton.addActionListener(e -> cardLayout.show(rightPanel, "Language"));
        generalButton.addActionListener(e -> cardLayout.show(rightPanel, "General"));

        JSplitPane settingsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        settingsSplitPane.setResizeWeight(0.10); // buttons panel width for settings panel

        add(settingsSplitPane);

        // Show the shortcuts panel by default when opening settings
        cardLayout.show(rightPanel, "Shortcuts");
    }

    /**
     * Creates the panel that displays a table of all application keyboard shortcuts.
     * @return A {@link JPanel} containing the shortcuts table.
     */
    private JPanel createShortcutsPanel() {
        String[] columnNames = {"Action", "Shortcut"};
        final Object[][] actionData = {
            {"Select Script Folder", "selectScriptFolder"},
            {"Select Input Folder", "selectInputFolder"},
            {"Refresh File Lists", "refreshAll"},
            {"Increase Font Size", "increaseFontSize"},
            {"Decrease Font Size", "decreaseFontSize"},
            {"Save Script", "saveScript"},
            {"Run Script", "runScript"},
            {"Toggle Output Panel", "toggleOutput"},
            {"Toggle File Explorer Panel", "toggleFileExplorer"}, // This entry will be removed
            {"Toggle Input Panel", "toggleInputPanel"},
            {"Toggle Control Panels", "toggleControls"},
            {"Enable Editing", "enableEditing"},
            {"Open Git Bash", "openGitBash"},
            {"Undo", "undo"},
            {"Redo", "redo"},
            {"Git Add", "gitAdd"},
            {"Git Commit", "gitCommit"},
            {"Git Pull", "gitPull"},
            {"Git Push", "gitPush"},
            {"Git Log", "gitLog"},
            {"Toggle Settings Panel", "toggleSettings"}
        };

        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Object[] action : actionData) {
            model.addRow(new Object[]{action[0], getShortcutStringForAction((String) action[1])});
        }

        JTable shortcutsTable = new JTable(model);
        shortcutsTable.setFillsViewportHeight(true);
        shortcutsTable.setFont(new Font("SansSerif", Font.PLAIN, 16));
        shortcutsTable.setRowHeight(shortcutsTable.getRowHeight() + 10);
        shortcutsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));

        shortcutsTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        shortcutsTable.getColumnModel().getColumn(1).setPreferredWidth(250);

        JScrollPane scrollPane = new JScrollPane(shortcutsTable);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Finds all KeyStrokes associated with a given action key and formats them into a user-friendly string.
     * @param actionMapKey The key for the action in the ActionMap.
     * @return A string representing the shortcuts.
     */
    private String getShortcutStringForAction(String actionMapKey) {
        InputMap inputMap = executor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        java.util.List<String> shortcuts = new java.util.ArrayList<>();
        KeyStroke[] keyStrokes = inputMap.keys();

        if (keyStrokes != null) {
            for (KeyStroke ks : keyStrokes) {
                if (actionMapKey.equals(inputMap.get(ks))) {
                    shortcuts.add(keyStrokeToString(ks));
                }
            }
        }
        return String.join(" / ", shortcuts);
    }

    /**
     * Converts a KeyStroke object into a human-readable string format.
     * @param ks The KeyStroke to format.
     * @return A formatted string (e.g., "Ctrl + Shift + S").
     */
    private String keyStrokeToString(KeyStroke ks) {
        if (ks == null) return "";
        StringBuilder sb = new StringBuilder();
        int modifiers = ks.getModifiers();

        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) sb.append("Ctrl + ");
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) sb.append("Alt + ");
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) sb.append("Shift + ");

        sb.append(KeyEvent.getKeyText(ks.getKeyCode()));
        return sb.toString();
    }

    /**
     * Creates the panel for adjusting editor font and tab sizes.
     * @return A {@link JPanel} containing font and tab size controls.
     */
    private JPanel createFontSizePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // --- Font Size Slider ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Editor Font Size:"), gbc);

        int initialSize = executor.getScriptTextArea().getFont().getSize();
        JSlider fontSizeSlider = new JSlider(8, 48, initialSize);
        JLabel currentSizeLabel = new JLabel(String.valueOf(initialSize));
        currentSizeLabel.setFont(currentSizeLabel.getFont().deriveFont(Font.BOLD));
        currentSizeLabel.setPreferredSize(new Dimension(30, 30));
        currentSizeLabel.setHorizontalAlignment(JLabel.CENTER);

        // --- Preview Area ---
        JTextArea previewArea = new JTextArea(
                "# This is a preview of the editor settings.\n"
                + "def example_function():\n"
                + "\t# Press Tab to see the new size.\n"
                + "\tprint(\"Hello, World!\")"
        );
        previewArea.setFont(executor.getScriptTextArea().getFont());
        previewArea.setEditable(false);
        previewArea.setOpaque(true);
        previewArea.setBackground(executor.getScriptTextArea().getBackground());
        previewArea.setForeground(executor.getScriptTextArea().getForeground());
        previewArea.setBorder(executor.createBoldTitledBorder("Preview"));

        // --- Listeners ---
        fontSizeSlider.addChangeListener(e -> {
            int newSize = fontSizeSlider.getValue();
            executor.setFontSize(newSize);
            currentSizeLabel.setText(String.valueOf(newSize));
            previewArea.setFont(previewArea.getFont().deriveFont((float) newSize));
        });

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(fontSizeSlider, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(currentSizeLabel, gbc);

        // --- Tab Size ---
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Tab Size (Indentations):"), gbc);

        SpinnerNumberModel tabSizeModel = new SpinnerNumberModel(executor.getCurrentTabSize(), 1, 16, 1);
        JSpinner tabSizeSpinner = new JSpinner(tabSizeModel);
        tabSizeSpinner.setPreferredSize(new Dimension(10, tabSizeSpinner.getPreferredSize().height));

        tabSizeSpinner.addChangeListener(e -> {
            int newTabSize = (Integer) tabSizeSpinner.getValue();
            executor.setTabSize(newTabSize);
            previewArea.setTabSize(newTabSize);
        });

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tabSizeSpinner, gbc);

        // --- Preview Area ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(previewArea), gbc);

        return panel;
    }

    /**
     * Creates the "General" settings panel.
     * @return A {@link JPanel} containing general application settings.
     */
    private JPanel createGeneralSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        panel.setBorder(executor.createBoldTitledBorder("General Settings"));

        JButton resetFoldersBtn = new JButton("Reset Folder Selections");
        resetFoldersBtn.setToolTipText("Clears the selected script and input folders and removes them from preferences.");
        resetFoldersBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to reset all saved folder paths?\nThis will clear your current script and input folder selections.",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                executor.resetFolderSelections();
                JOptionPane.showMessageDialog(this, "Folder selections have been reset.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton toggleFileExplorerModeBtn = new JButton(executor.isExplorerMode() ? "Toggle Classic Mode" : "Toggle File Explorer mode");
        toggleFileExplorerModeBtn.setToolTipText("Toggles the file explorer mode.");
        toggleFileExplorerModeBtn.addActionListener(e -> {
            executor.toggleFileExplorerMode();
            // Update button text after toggling
            toggleFileExplorerModeBtn.setText(executor.isExplorerMode() ? "Toggle Classic Mode" : "Toggle File Explorer mode");
        });

        JButton toggleGitControlsBtn = new JButton(executor.areGitControlsVisible() ? "Hide Git Controls" : "Show Git Controls");
        toggleGitControlsBtn.setToolTipText("Toggles the visibility of the Git version control panel.");
        toggleGitControlsBtn.addActionListener(e -> {
            executor.toggleGitControlsVisibility();
            // Update button text after toggling
            toggleGitControlsBtn.setText(executor.areGitControlsVisible() ? "Hide Git Controls" : "Show Git Controls");
        });

        JButton reloadWindowBtn = new JButton("Reload Window");
        reloadWindowBtn.setToolTipText("Saves settings and reloads the application window.");
        reloadWindowBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to reload the window?\nUnsaved changes in the script editor will be lost.",
                    "Confirm Reload",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                executor.reloadWindow();
            }
        });

        JButton loadQuestionBtn = new JButton("Load Question");
        loadQuestionBtn.setToolTipText("Loads a question from a specified source.");
        loadQuestionBtn.addActionListener(e -> showLoadQuestionDialog());

        JButton toggleExamModeBtn = new JButton("Toggle Exam Mode");
        toggleExamModeBtn.setToolTipText("Toggles a simplified UI for exam environments.");
        // Set a consistent height for all buttons in this panel
        for (JButton btn : Arrays.asList(resetFoldersBtn, toggleFileExplorerModeBtn, reloadWindowBtn, loadQuestionBtn, toggleExamModeBtn)) {
            Dimension size = btn.getPreferredSize();
            size.height += 7;
            btn.setPreferredSize(size);
            panel.add(btn);
        }

        // Handle the toggle button separately to ensure its width is sufficient for both labels.
        Dimension hideSize = new JButton("Hide Git Controls").getPreferredSize();
        Dimension showSize = new JButton("Show Git Controls").getPreferredSize();
        int maxWidth = Math.max(hideSize.width, showSize.width);
        Dimension preferredSize = new Dimension(maxWidth, hideSize.height + 7);
        toggleGitControlsBtn.setPreferredSize(preferredSize);
        panel.add(toggleGitControlsBtn);

        return panel;
    }

    /**
     * Creates and displays a dialog for loading and viewing problem statements from text files.
     */
    private void showLoadQuestionDialog() {
        JDialog dialog = new JDialog(this, "Load Problem Statements", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table to display problem statements
        String[] columnNames = {"Problem Title", "Statement"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells non-editable
            }
        };
        JTable problemsTable = new JTable(tableModel);
        problemsTable.setRowHeight(30);
        problemsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        problemsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));
        problemsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        problemsTable.getColumnModel().getColumn(1).setPreferredWidth(600);

        // Make the "Statement" column wrap text
        problemsTable.getColumnModel().getColumn(1).setCellRenderer(new MultiLineCellRenderer());

        JScrollPane scrollPane = new JScrollPane(problemsTable);

        // Button to select a folder
        JButton selectFolderBtn = new JButton("Select Folder with Problems...");
        selectFolderBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            Path currentProblemDir = executor.getProblemDirectory();
            if (currentProblemDir != null && Files.isDirectory(currentProblemDir)) {
                chooser.setCurrentDirectory(currentProblemDir.toFile());
            }
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Problem Statements Folder");

            int result = chooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                Path selectedFolder = chooser.getSelectedFile().toPath();
                executor.setProblemDirectory(selectedFolder);
                loadProblemsFromFolder(selectedFolder, tableModel, problemsTable);
            }
        });

        // Automatically load problems from the last used folder, if it exists.
        Path lastProblemDir = executor.getProblemDirectory();
        if (lastProblemDir != null && Files.isDirectory(lastProblemDir)) {
            loadProblemsFromFolder(lastProblemDir, tableModel, problemsTable);
        }

        mainPanel.add(selectFolderBtn, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Loads problem statements from .txt files in a given folder into a table.
     * @param folder The folder to read from.
     * @param model The table model to populate.
     * @param table The table to adjust row heights for.
     */
    private void loadProblemsFromFolder(Path folder, DefaultTableModel model, JTable table) {
        model.setRowCount(0); // Clear existing problems

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".txt"))
                 .forEach(filePath -> {
                     try {
                         String title = filePath.getFileName().toString().replace(".txt", "");
                         String content = Files.readString(filePath);
                         model.addRow(new Object[]{title, content});
                     } catch (IOException ex) {
                         System.err.println("Failed to read file: " + filePath);
                     }
                 });
            
            // Adjust row heights to fit the content of the multi-line renderer
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, 1);
                Component comp = table.prepareRenderer(renderer, row, 1);
                int height = comp.getPreferredSize().height;
                if (table.getRowHeight(row) != height) {
                    table.setRowHeight(row, height);
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading folder: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * A custom TableCellRenderer that uses a JTextArea to display multi-line text.
     */
    private static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {
        /**
         * Constructs a MultiLineCellRenderer, initializing it to wrap text.
         */
        public MultiLineCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add some padding
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }
}