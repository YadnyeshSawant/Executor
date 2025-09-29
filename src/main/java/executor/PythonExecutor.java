package executor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * A Swing-based desktop application for executing Python scripts. It provides a
 * user interface to select, edit, and run Python scripts, manage input files,
 * and view the output. Includes Git integration and other utility features.
 */
public class PythonExecutor extends JFrame {

    /**
     * Preference key for storing the last used script directory path.
     */
    private static final String PREF_SCRIPT_DIR = "scriptDirectory";
    /**
     * Preference key for storing the last used input directory path.
     */
    private static final String PREF_INPUT_DIR = "inputDirectory";
    /**
     * Preference key for storing the last opened script file name.
     */
    private static final String PREF_LAST_SCRIPT = "lastScriptFile";
    /**
     * Preference key for storing the list of recently opened folders.
     */
    private static final String PREF_RECENT_FOLDERS = "recentFolders";
    /**
     * Preference key for storing the file explorer mode state.
     */
    private static final String PREF_EXPLORER_MODE = "fileExplorerMode";

    /**
     * Placeholder text for the script text area when it's empty.
     */
    private static final String SCRIPT_PLACEHOLDER = "Start typing or load a script...";
    /**
     * Single-threaded executor service for running background tasks like script
     * execution and Git commands, ensuring they don't block the UI thread.
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private JButton runBtn;
    private JPanel mainPanel;
    private JSplitPane mainSplitPane;
    private JSplitPane middleSplitPane;
    private JComboBox<Object> scriptFileCombo;
    private JComboBox<Object> inputFileCombo;
    private RSyntaxTextArea scriptTextArea;
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private JLabel currentScriptLabel;
    private JDialog settingsDialog;
    private JLabel scriptFolderPathLabel;
    private JLabel inputFolderPathLabel;
    private Path scriptDirectory;
    private Path inputDirectory;
    private List<Path> allScriptFiles = new ArrayList<>();
    private List<Path> allInputFiles = new ArrayList<>();
    private int lastDividerLocation = -1;
    private int lastMiddleDividerLocation = -1;
    private int lastExplorerSplitPaneLocation = -1;
    private int lastMiddleDividerLocationExplorer = -1;
    private boolean isOutputVisible = true;
    private JButton toggleOutputBtn;
    private JLabel errorStatusLabel;
    private JTextField searchField;
    private int totalErrors = 0;
    private JPopupMenu searchResultPopup;

    private RoundedPanel scriptSelectionPanel;
    private JPanel allControlsPanel;
    private JButton hideControlsBtn;
    private boolean areControlsVisible = true;
    private boolean isInputPanelVisible = true;
    private boolean isFileExplorerMode = false;
    private boolean isInputExplorerCollapsed = false;

    private DefaultListModel<Path> scriptListModel;
    private DefaultListModel<Path> inputListModel;

    private java.util.LinkedList<Path> recentFolders = new java.util.LinkedList<>();
    private JPopupMenu recentFoldersPopup;
    private JButton recentFoldersBtn;
    private static final int MAX_RECENT_FOLDERS = 5;

    private UndoManager undoManager;
    private int currentTabSize = 4; // Default Python tab size

    private RTextScrollPane scriptScrollPane;
    private JTree fileExplorerTree;
    private JPopupMenu explorerContextMenu;
    private JMenuItem editMenuItem;
    private JMenuItem renameMenuItem;
    private JMenuItem moveMenuItem;
    private JMenuItem deleteMenuItem;
    private Path contextMenuPath;
    private JSplitPane explorerSplitPane;
    private JSplitPane explorerMainSplitPane;
    private JPanel fileExplorerContainer;
    private CardLayout fileExplorerCardLayout;
    private static final String EXPLORER_TREE_VIEW = "EXPLORER_TREE_VIEW";
    private static final String EXPLORER_EMPTY_VIEW = "EXPLORER_EMPTY_VIEW";
    private JTree inputFileExplorerTree;
    private JScrollPane inputFileExplorerScrollPane;
    private JPanel inputFileExplorerContainer;
    private CardLayout inputFileExplorerCardLayout;
    private static final String INPUT_EXPLORER_TREE_VIEW = "INPUT_EXPLORER_TREE_VIEW";
    private static final String INPUT_EXPLORER_EMPTY_VIEW = "INPUT_EXPLORER_EMPTY_VIEW";

    private JScrollPane fileExplorerScrollPane;

    private final boolean isPythonAvailable;
    private GitManager gitManager;
    private JButton toggleFileExplorerModeBtn;
    private JDialog findDialog;

    /**
     * Constructs the PythonExecutor application window and initializes all UI
     * components and event listeners.
     *
     * @param pythonExists A boolean indicating if a Python executable was found
     * on the system PATH during startup.
     */
    public PythonExecutor(boolean pythonExists) {
        this.isPythonAvailable = pythonExists;
        // Set up the main frame
        setTitle("Python Executor v2.1.1");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Save preferences before closing
                savePreferences();
                // Gracefully shut down the executor service before closing
                executorService.shutdownNow();
                dispose();
            }
        });
        setMinimumSize(new Dimension(800, 600));

        // --- Set Application Icon ---
        // The path to the image should be relative to the classpath.
        // For example, if you have a 'resources' folder in your source path,
        // and 'logo.png' is inside it, the path would be "/logo.png".
        try {
            java.net.URL iconURL = getClass().getResource("/logo.png"); // Assumes logo.png is at the root of the classpath
            if (iconURL != null) {
                setIconImage(new ImageIcon(iconURL).getImage());
            } else {
                System.err.println("Warning: Could not find 'logo.png' in classpath. Using default icon.");
            }
        } catch (Exception e) {
            System.err.println("Error loading application icon: " + e.getMessage());
        }

        // Use a custom dark theme for the look and feel
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("control", new Color(40, 40, 40));
            UIManager.put("info", new Color(40, 40, 40));
            UIManager.put("nimbusBase", new Color(20, 20, 20));
            UIManager.put("nimbusAlertYellow", new Color(248, 182, 0));
            UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
            UIManager.put("nimbusFocus", new Color(115, 164, 209));
            UIManager.put("nimbusLightBackground", new Color(60, 60, 60));
            UIManager.put("nimbusOrange", new Color(191, 98, 4));
            UIManager.put("nimbusRed", new Color(169, 46, 34));
            UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
            UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
            UIManager.put("textForeground", new Color(200, 200, 200));
            UIManager.put("ToolBar.background", new Color(40, 40, 40));
            UIManager.put("nimbusBlueGrey", new Color(50, 50, 50));
            UIManager.put("OptionPane.background", new Color(40, 40, 40));
            UIManager.put("Panel.background", new Color(40, 40, 40));
            UIManager.put("Button.background", new Color(60, 60, 60));
            UIManager.put("Button.foreground", new Color(200, 200, 200));
            UIManager.put("TextField.background", new Color(50, 50, 50));
            UIManager.put("TextField.foreground", new Color(200, 200, 200));
            UIManager.put("TextArea.background", new Color(50, 50, 50));
            UIManager.put("TextArea.foreground", new Color(200, 200, 200));
            UIManager.put("ComboBox.background", new Color(60, 60, 60));
            UIManager.put("ComboBox.foreground", new Color(200, 200, 200));
            UIManager.put("Label.foreground", new Color(200, 200, 200));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Script Selection Panel
        scriptSelectionPanel = new RoundedPanel(10, new Color(40, 40, 40));
        scriptSelectionPanel.setLayout(new GridBagLayout());
        scriptSelectionPanel.setBorder(createBoldTitledBorder("File & Input Selection "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // --- Row 0: Script Folder ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        scriptSelectionPanel.add(new JLabel("Script Folder:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        JButton changeFolderBtn = new JButton("Change Folder...");
        changeFolderBtn.setFocusPainted(false);
        changeFolderBtn.addActionListener(e -> selectScriptFolder());
        changeFolderBtn.setPreferredSize(new Dimension(200, 40));
        scriptSelectionPanel.add(changeFolderBtn, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scriptFolderPathLabel = new JLabel("");
        scriptSelectionPanel.add(scriptFolderPathLabel, gbc);

        // --- Row 1: Script File ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        scriptSelectionPanel.add(new JLabel("Script File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        scriptFileCombo = new JComboBox<>();
        scriptFileCombo.addItem("Select a script");
        scriptFileCombo.setPreferredSize(new Dimension(200, 40));
        scriptFileCombo.setBackground(new Color(0, 0, 0));
        scriptFileCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Path) {
                    setText(((Path) value).getFileName().toString());
                }
                return this;
            }
        });
        scriptFileCombo.addActionListener(e -> loadSelectedScript());
        scriptSelectionPanel.add(scriptFileCombo, gbc);

        // --- Row 2: Input Folder ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        scriptSelectionPanel.add(new JLabel("Input Folder:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        JButton selectInputBtn = new JButton("Select Input Folder...");
        selectInputBtn.setFocusPainted(false);
        selectInputBtn.addActionListener(e -> selectInputFolder());
        selectInputBtn.setPreferredSize(new Dimension(200, 40));
        scriptSelectionPanel.add(selectInputBtn, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        inputFolderPathLabel = new JLabel("");
        scriptSelectionPanel.add(inputFolderPathLabel, gbc);

        // --- Row 3: Input File ---
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        scriptSelectionPanel.add(new JLabel("Input File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        inputFileCombo = new JComboBox<>();
        inputFileCombo.addItem("Select an input file");
        inputFileCombo.setPreferredSize(new Dimension(200, 40));
        inputFileCombo.setBackground(new Color(0, 0, 0));
        inputFileCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Path) {
                    setText(((Path) value).getFileName().toString());
                }
                return this;
            }
        });
        inputFileCombo.addActionListener(e -> loadSelectedInputFile());
        scriptSelectionPanel.add(inputFileCombo, gbc);

        // Execution Controls Panel
        RoundedPanel execControlsPanel = new RoundedPanel(10, new Color(40, 40, 40));
        execControlsPanel.setBorder(createBoldTitledBorder("Execution Controls "));
        execControlsPanel.setLayout(new GridLayout(2, 3, 5, 5)); // 2 rows, 3 columns
        Dimension execButtonSize = new Dimension(125, 30);
        runBtn = new JButton("\u25B6 Run");
        runBtn.setFocusPainted(false);
        runBtn.addActionListener(e -> executePythonScript());
        runBtn.setPreferredSize(execButtonSize);
        JButton saveBtn = new JButton("Save");
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveScript());
        saveBtn.setPreferredSize(execButtonSize);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshAll());
        refreshBtn.setPreferredSize(execButtonSize);
        JButton editBtn = new JButton("Edit");
        editBtn.setFocusPainted(false);
        editBtn.addActionListener(e -> enableEditing());
        editBtn.setPreferredSize(execButtonSize);
        JButton clearOutputBtn = new JButton("Clear Output");
        clearOutputBtn.setFocusPainted(false);
        clearOutputBtn.addActionListener(e -> outputTextArea.setText(""));
        clearOutputBtn.addActionListener(e -> resetErrorCount());
        clearOutputBtn.setPreferredSize(execButtonSize);
        toggleOutputBtn = new JButton("Hide Output");
        toggleOutputBtn.setPreferredSize(execButtonSize);
        toggleOutputBtn.addActionListener(e -> toggleOutputVisibility());

        execControlsPanel.add(runBtn);
        execControlsPanel.add(editBtn);
        execControlsPanel.add(saveBtn);
        execControlsPanel.add(refreshBtn);
        execControlsPanel.add(clearOutputBtn);
        execControlsPanel.add(toggleOutputBtn);

        // Version Control Panel
        RoundedPanel versionControlPanel = new RoundedPanel(10, new Color(40, 40, 40));
        versionControlPanel.setBorder(createBoldTitledBorder("Version Control "));
        versionControlPanel.setLayout(new GridLayout(2, 3, 5, 5));

        JButton gitBashBtn = new JButton("GitBash");
        gitBashBtn.setFocusPainted(false);
        gitBashBtn.addActionListener(e -> gitManager.openGitBash());
        gitBashBtn.setPreferredSize(execButtonSize);
        // Color colorbash = new Color(0xA3BE8C);
        // gitBashBtn.setBackground(colorbash);

        JButton gitAddBtn = new JButton("Git Add");
        gitAddBtn.setFocusPainted(false);
        gitAddBtn.addActionListener(e -> gitManager.showGitAddDialog());
        gitAddBtn.setPreferredSize(execButtonSize);

        JButton gitCommitBtn = new JButton("Git Commit");
        gitCommitBtn.setFocusPainted(false);
        gitCommitBtn.addActionListener(e -> gitManager.showGitCommitDialog());
        gitCommitBtn.setPreferredSize(execButtonSize);

        JButton gitPullBtn = new JButton("\u2B07\uFE0F Git Pull");
        gitPullBtn.setFocusPainted(false);
        gitPullBtn.addActionListener(e -> gitManager.runGitPull());
        gitPullBtn.setPreferredSize(execButtonSize);

        JButton gitPushBtn = new JButton("\u2B06\uFE0F Git Push");
        gitPushBtn.setFocusPainted(false);
        gitPushBtn.addActionListener(e -> gitManager.runGitPush());
        gitPushBtn.setPreferredSize(execButtonSize);

        JButton gitLogBtn = new JButton("Git Log");
        gitLogBtn.setFocusPainted(false);
        gitLogBtn.addActionListener(e -> gitManager.showGitLog());
        gitLogBtn.setPreferredSize(execButtonSize);

        versionControlPanel.add(gitBashBtn);
        versionControlPanel.add(gitAddBtn);
        versionControlPanel.add(gitCommitBtn);
        versionControlPanel.add(gitPullBtn);
        versionControlPanel.add(gitPushBtn);
        versionControlPanel.add(gitLogBtn);

        // Python Script Text Area
        scriptTextArea = new RSyntaxTextArea();
        scriptTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        scriptTextArea.setBackground(new Color(43, 43, 43));
        scriptTextArea.setCurrentLineHighlightColor(new Color(50, 50, 50));
        scriptTextArea.setCodeFoldingEnabled(true);

        // Customize syntax highlighting for the dark theme
        SyntaxScheme scheme = scriptTextArea.getSyntaxScheme();
        scheme.getStyle(Token.RESERVED_WORD).foreground = new Color(204, 120, 50);   // 'def', 'if', 'for', etc.
        scheme.getStyle(Token.RESERVED_WORD_2).foreground = new Color(204, 120, 50); // 'self', etc.
        scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = new Color(204, 120, 50); // 'True', 'False'
        scheme.getStyle(Token.FUNCTION).foreground = new Color(130, 170, 255);      // built-in functions like 'print'
        scriptTextArea.revalidate();

        undoManager = new UndoManager();
        scriptTextArea.getDocument().addUndoableEditListener(undoManager);
        scriptTextArea.setBorder(createBoldTitledBorder("Python Script "));
        addPlaceholder(scriptTextArea, SCRIPT_PLACEHOLDER);
        scriptTextArea.getInputMap().put(KeyStroke.getKeyStroke("control J"), "none");
        scriptTextArea.getInputMap().put(KeyStroke.getKeyStroke("control H"), "none");
        setupScriptAreaIndentation();
        scriptTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                clearErrorHighlights();
            }

            public void removeUpdate(DocumentEvent e) {
                clearErrorHighlights();
            }

            public void changedUpdate(DocumentEvent e) {
                clearErrorHighlights();
            }
        });

        scriptScrollPane = new RTextScrollPane(scriptTextArea);

        // Input Text Area
        inputTextArea = new JTextArea();
        inputTextArea.setBorder(createBoldTitledBorder("Input Data "));
        inputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control J"), "none");
        inputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control H"), "none");
        JScrollPane inputScrollPane = new JScrollPane(inputTextArea);

        // Output Panel
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control J"), "none");
        outputTextArea.setBorder(createBoldTitledBorder("Output "));
        outputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control H"), "none");
        String welcomeTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        outputTextArea.setText(String.format("[%s] Welcome! Please select a folder containing your Python scripts to begin.", welcomeTimestamp));
        // Add a listener to automatically scroll to the bottom when text is added
        outputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength()));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                /* Do nothing */ }

            @Override
            public void changedUpdate(DocumentEvent e) {
                /* Do nothing */ }
        });
        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);

        // Initialize GitManager now that outputTextArea exists
        this.gitManager = new GitManager(this, executorService, outputTextArea::append, scriptDirectory, inputDirectory);

        // Status Bar
        RoundedPanel statusBar = new RoundedPanel(10, new Color(50, 50, 50));
        statusBar.setLayout(new BorderLayout());
        statusBar.setPreferredSize(new Dimension(0, 30)); // Set a fixed height
        statusBar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Adjust padding for vertical centering
        currentScriptLabel = new JLabel("  Current Script: None");
        errorStatusLabel = new JLabel("Errors: 0");
        JLabel languageStatusLabel = new JLabel("Python");
        languageStatusLabel.setForeground(new Color(169, 46, 34));

        // Panel for right-aligned items to group them together
        JPanel rightStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5)); // hgap, vgap
        rightStatusPanel.setOpaque(false); // Make it transparent
        rightStatusPanel.add(languageStatusLabel);
        rightStatusPanel.add(errorStatusLabel);

        statusBar.add(currentScriptLabel, BorderLayout.WEST);
        statusBar.add(rightStatusPanel, BorderLayout.EAST);

        // Bottom panel to hold both status and interactive controls
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusBar, BorderLayout.CENTER);

        // --- Assemble Layout ---
        // Left Panel (Script selection and script area) - No change needed here as it's just a container
        JPanel scriptPanel = new JPanel(new BorderLayout(0, 5));
        scriptPanel.add(scriptSelectionPanel, BorderLayout.NORTH);
        scriptPanel.add(scriptScrollPane, BorderLayout.CENTER);

        // Right Panel (Input/Exec controls and input area)
        allControlsPanel = new JPanel();
        allControlsPanel.setLayout(new BoxLayout(allControlsPanel, BoxLayout.Y_AXIS));
        allControlsPanel.add(execControlsPanel);
        allControlsPanel.add(versionControlPanel);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.add(allControlsPanel, BorderLayout.NORTH);
        rightPanel.add(inputScrollPane, BorderLayout.CENTER);

        // --- SPLIT PANES ---
        middleSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scriptPanel, rightPanel);
        middleSplitPane.setResizeWeight(0.5); // Give script area 80% of the space
        middleSplitPane.setDividerSize(3); // Make the divider thinner

        mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, middleSplitPane, outputScrollPane);
        mainSplitPane.setResizeWeight(0.5); // Give editor areas more space
        mainSplitPane.setDividerSize(3); // Make the divider thinner

        // Add all panels to the main frame
        JPanel topPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout for more control

        // Search Field
        searchField = new JTextField();
        searchField.setToolTipText("Type here to filter script and input files by name.");
        // Make the text field transparent so the rounded panel's background shows through
        searchField.setOpaque(false);
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 8)); // Add some padding

        searchField.getInputMap().put(KeyStroke.getKeyStroke("control H"), "none");
        RoundedPanel searchPanel = new RoundedPanel(10, new Color(50, 50, 50));
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setPreferredSize(new Dimension(400, 30)); // Give search a fixed preferred width
        initializeSearchPopup();
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateAndShowSearchResults();
            }

            public void removeUpdate(DocumentEvent e) {
                updateAndShowSearchResults();
            }

            public void changedUpdate(DocumentEvent e) {
                updateAndShowSearchResults();
            }
        });
        // Add a FocusListener to hide the popup when the search field loses focus
        searchField.addFocusListener(new FocusAdapter() {
            private static final String PLACEHOLDER = "Search Files...";

            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(UIManager.getColor("TextField.foreground"));
                    searchField.setHorizontalAlignment(JTextField.LEFT);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Use a timer to allow a click on the popup to be processed before hiding it
                Timer timer = new Timer(200, ae -> {
                    if (searchResultPopup.isVisible() && !searchField.isFocusOwner() && !searchResultPopup.isFocusOwner()) {
                        searchResultPopup.setVisible(false);
                    }
                    if (searchField.getText().isEmpty()) {
                        searchField.setText(PLACEHOLDER);
                        searchField.setForeground(Color.GRAY);
                        searchField.setHorizontalAlignment(JTextField.CENTER);
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });
        // Trigger focusLost initially to set the placeholder text
        SwingUtilities.invokeLater(() -> {
            searchField.getFocusListeners()[searchField.getFocusListeners().length - 1].focusLost(null);
        });

        GridBagConstraints topGbc = new GridBagConstraints();

        // --- Left Buttons (Undo/Redo) ---
        JButton undoBtn = new JButton("Undo");
        undoBtn.setToolTipText("Undo last action (Ctrl+Z)");
        undoBtn.setFocusPainted(false);
        undoBtn.addActionListener(e -> undo());
        undoBtn.setPreferredSize(new Dimension(80, 35));

        JButton redoBtn = new JButton("Redo");
        redoBtn.setToolTipText("Redo last action (Ctrl+Y)");
        redoBtn.setFocusPainted(false);
        redoBtn.addActionListener(e -> redo());
        redoBtn.setPreferredSize(new Dimension(80, 35));

        initializeRecentFolders();

        JPanel leftButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftButtonsPanel.setOpaque(false);
        leftButtonsPanel.add(undoBtn);
        leftButtonsPanel.add(redoBtn);
        leftButtonsPanel.add(recentFoldersBtn);

        topGbc.gridx = 0;
        topGbc.weightx = 0.0;
        topGbc.anchor = GridBagConstraints.WEST;
        topPanel.add(leftButtonsPanel, topGbc);

        // Add an empty component to push the search bar to the center
        topGbc.gridx = 0;
        topGbc.gridx = 1;
        topGbc.weightx = 1.0;
        topPanel.add(Box.createHorizontalGlue(), topGbc);

        // Add the search panel to the center
        topGbc.gridx = 1;
        topGbc.gridx = 2;
        topGbc.weightx = 0; // Do not let the search bar expand
        topGbc.fill = GridBagConstraints.NONE;
        topPanel.add(searchPanel, topGbc);

        // // Clear Button
        // // Add flexible space in the middle to push settings button to the right
        // // Add flexible space in the middle to push the settings button to the right
        // topGbc.gridx = 2;
        // topGbc.weightx = 0;
        // topGbc.fill = GridBagConstraints.NONE;
        // topGbc.insets = new Insets(0, 5, 0, 0);
        // JButton clearSearchBtn = new JButton("Clear");
        // clearSearchBtn.setFocusPainted(false);
        // clearSearchBtn.addActionListener(e -> searchField.setText(""));
        // clearSearchBtn.setPreferredSize(new Dimension(clearSearchBtn.getPreferredSize().width, 35));
        // topPanel.add(clearSearchBtn, topGbc);
        // topGbc.weightx = 1.0; // This will take up all extra horizontal space
        // topPanel.add(Box.createHorizontalGlue(), topGbc);
        // Settings Button
        JButton settingsBtn = new JButton("Settings"); // Gear icon
        settingsBtn.setMargin(new Insets(0, 0, 0, 4));
        settingsBtn.setToolTipText("Settings");
        settingsBtn.setFocusPainted(false);
        Font settingsFont = settingsBtn.getFont();
        settingsBtn.setFont(settingsFont.deriveFont(settingsFont.getSize()));
        settingsBtn.addActionListener(e -> openSettingsDialog());
        settingsBtn.setPreferredSize(new Dimension(80, 35));

        // Hide Controls Button
        hideControlsBtn = new JButton("Hide Controls");
        hideControlsBtn.setToolTipText("Toggle visibility of control panels");
        hideControlsBtn.setFocusPainted(false);
        hideControlsBtn.addActionListener(e -> toggleControlsVisibility());
        hideControlsBtn.setPreferredSize(new Dimension(120, 35));

        // Find Button
        JButton findBtn = new JButton("Find");
        findBtn.setToolTipText("Find text in the script area");
        findBtn.setFocusPainted(false);
        findBtn.addActionListener(e -> showFindDialog());
        findBtn.setPreferredSize(new Dimension(80, 35));

        // Panel for right-aligned buttons
        JPanel rightButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightButtonsPanel.setOpaque(false);
        rightButtonsPanel.add(findBtn);
        rightButtonsPanel.add(hideControlsBtn);
        rightButtonsPanel.add(settingsBtn);

        // Add the buttons panel to the right
        topGbc.gridx = 2;
        topGbc.gridx = 3;
        topGbc.weightx = 1.0; // This will push the button to the right
        topGbc.anchor = GridBagConstraints.EAST;
        topGbc.insets = new Insets(0, 10, 0, 0);
        topPanel.add(rightButtonsPanel, topGbc);

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Initialize components that are not visible by default
        initializeFileExplorer();
        createExplorerContextMenu();

        // --- Keyboard Shortcuts ---
        JRootPane rootPane = this.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // Shortcut for selecting script folder: Ctrl + Shift + S
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "selectScriptFolder");
        actionMap.put("selectScriptFolder", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectScriptFolder();
            }
        });

        // Shortcut for selecting input folder: Ctrl + Shift + I
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "selectInputFolder");
        actionMap.put("selectInputFolder", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectInputFolder();
            }
        });

        // Shortcut for refreshing file lists: Ctrl + Shift + R
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "refreshAll");
        actionMap.put("refreshAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshAll();
            }
        });

        // --- Font Size Shortcuts ---
        // Ctrl + Plus (or Equals) to increase font size
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "increaseFontSize");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "increaseFontSize"); // Numpad plus
        actionMap.put("increaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(1);
            }
        });

        // Ctrl + Minus to decrease font size
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "decreaseFontSize");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "decreaseFontSize"); // Numpad minus
        actionMap.put("decreaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(-1);
            }
        });

        // Shortcut for saving the script: Ctrl + S
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveScript");
        actionMap.put("saveScript", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveScript();
            }
        });

        // Shortcut for running the script: Shift + Enter
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "runScript");
        actionMap.put("runScript", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executePythonScript();
            }
        });

        // Shortcut for toggling output panel: Ctrl + J
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK), "toggleOutput");
        actionMap.put("toggleOutput", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleOutputVisibility();
            }
        });

        // Shortcut for toggling input panel: Ctrl + I
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "toggleInputPanel");
        actionMap.put("toggleInputPanel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleInputPanelVisibility();
            }
        });

        // Shortcut for toggling control panels: Ctrl + H
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), "toggleControls");
        actionMap.put("toggleControls", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleControlsVisibility();
            }
        });

        // Shortcut for enabling the edit mode : Ctrl + e
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "enableEditing");
        actionMap.put("enableEditing", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableEditing();
            }
        });

        // Shortcut for Git Bash: Ctrl + Shift + B
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "openGitBash");
        actionMap.put("openGitBash", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gitManager.openGitBash();
            }
        });

        // Shortcut for Git Add: Ctrl + Shift + A
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "gitAdd");
        actionMap.put("gitAdd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gitManager.showGitAddDialog();
            }
        });

        // Shortcut for Git Commit: Ctrl + Shift + C
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "gitCommit");
        actionMap.put("gitCommit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gitManager.showGitCommitDialog();
            }
        });

        // Shortcut for Git Pull: Ctrl + Shift + U
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "gitPull");
        actionMap.put("gitPull", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gitManager.runGitPull();
            }
        });

        // Shortcut for Git Push: Ctrl + Shift + P
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "gitPush");
        actionMap.put("gitPush", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gitManager.runGitPush();
            }
        });

        // Shortcut for Git Log: Ctrl + Shift + L
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "gitLog");
        actionMap.put("gitLog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gitManager.showGitLog();
            }
        });

        // Shortcut for toggling settings panel: Ctrl + ,
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK), "toggleSettings");
        actionMap.put("toggleSettings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (settingsDialog != null && settingsDialog.isVisible()) {
                    settingsDialog.setVisible(false);
                } else {
                    openSettingsDialog();
                }
            }
        });

        // Shortcut for Undo: Ctrl + Z
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        // Shortcut for Redo: Ctrl + Y
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        add(mainPanel);
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Load preferences after the UI is constructed.
        // Using invokeLater ensures this runs after the constructor completes and the
        // component hierarchy is established, preventing race conditions with UI
        // component initialization.
        SwingUtilities.invokeLater(this::loadPreferences);
    }

    /**
     * Initializes the "Recent Folders" button and its associated popup menu.
     */
    private void initializeRecentFolders() {
        recentFoldersPopup = new JPopupMenu();
        recentFoldersBtn = new JButton("Recent"); // Down-pointing triangle
        recentFoldersBtn.setToolTipText("View Recently opened folders");
        recentFoldersBtn.setFocusPainted(false);
        recentFoldersBtn.setMargin(new Insets(2, 4, 2, 4));
        recentFoldersBtn.setPreferredSize(new Dimension(80, 35));

        recentFoldersBtn.addActionListener(e -> {
            updateRecentFoldersPopup();
            recentFoldersPopup.show(recentFoldersBtn, 0, recentFoldersBtn.getHeight());
        });
    }

    /**
     * Clears and repopulates the "Recent Folders" popup menu with the current
     * list of recent folders.
     */
    private void updateRecentFoldersPopup() {
        recentFoldersPopup.removeAll();
        if (recentFolders.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No recent folders");
            emptyItem.setEnabled(false);
            recentFoldersPopup.add(emptyItem);
        } else {
            for (Path folder : recentFolders) {
                JMenuItem menuItem = new JMenuItem(folder.toString());
                menuItem.addActionListener(e -> openRecentFolder(folder));
                recentFoldersPopup.add(menuItem);
            }
        }
    }

    /**
     * Handles the selection of a folder from the "Recent Folders" menu,
     * prompting the user to set it as either the script or input folder.
     *
     * @param folder The {@link Path} of the folder selected by the user.
     */
    private void openRecentFolder(Path folder) {
        Object[] options = {"Script Folder", "Input Folder", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Set '" + folder.getFileName() + "' as:",
                "Open Recent Folder",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == JOptionPane.YES_OPTION) { // Script Folder
            scriptDirectory = folder;
            scriptFolderPathLabel.setText(scriptDirectory.toString());
            loadPythonScripts();
            updateGitManager();
            populateFileExplorer();
            if (isFileExplorerMode) {
                fileExplorerCardLayout.show(fileExplorerContainer, EXPLORER_TREE_VIEW);
            }
            addRecentFolder(folder);
        } else if (choice == JOptionPane.NO_OPTION) { // Input Folder
            inputDirectory = folder;
            inputFolderPathLabel.setText(inputDirectory.toString());
            loadInputFiles();
            updateGitManager();
            populateInputFileExplorer();
            if (isFileExplorerMode) {
                inputFileExplorerCardLayout.show(inputFileExplorerContainer, INPUT_EXPLORER_TREE_VIEW);
            }
            addRecentFolder(folder);
        }
    }

    private void updateGitManager() {
        this.gitManager = new GitManager(this, executorService, outputTextArea::append, scriptDirectory, inputDirectory);
    }

    /**
     * Creates and displays the settings dialog window if it doesn't already
     * exist.
     */
    private void openSettingsDialog() {
        if (settingsDialog == null) {
            settingsDialog = new JDialog(this, "Settings", true);
            settingsDialog.setSize(1000, 700);
            settingsDialog.setMinimumSize(new Dimension(800, 500));
            settingsDialog.setLocationRelativeTo(this);
            // Panels for settings
            // Use a 5px empty border for some padding around the edges
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
            shortcutsButton.setMaximumSize(buttonSize);
            themesButton.setMaximumSize(buttonSize);
            fontSizeButton.setMaximumSize(buttonSize);
            languageButton.setMaximumSize(buttonSize);
            generalButton.setMaximumSize(buttonSize);

            // Add buttons to the left panel
            leftPanel.add(shortcutsButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
            leftPanel.add(themesButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
            leftPanel.add(fontSizeButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
            leftPanel.add(languageButton);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
            leftPanel.add(generalButton);

            // Panels for settings
            final CardLayout cardLayout = new CardLayout();
            final JPanel rightPanel = new JPanel(cardLayout);

            // --- Create the different settings panels ---
            JPanel shortcutsPanel = createShortcutsPanel();
            JPanel themesPanel = new JPanel(); // Placeholder
            themesPanel.add(new JLabel("Theme settings will be comming soon."));
            JPanel fontPanel = createFontSizePanel();
            JPanel langPanel = new JPanel(); // Placeholder
            langPanel.add(new JLabel("Language settings will be comming soon."));
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

            JSplitPane settingsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    leftPanel, rightPanel);
            settingsSplitPane.setResizeWeight(0.10);    //buttons pannel width for settings pannel

            settingsDialog.add(settingsSplitPane);

            // Show the shortcuts panel by default when opening settings
            cardLayout.show(rightPanel, "Shortcuts");
        }
        settingsDialog.setVisible(true);
    }

    /**
     * Creates the panel that displays a table of all application keyboard
     * shortcuts.
     *
     * @return A {@link JPanel} containing the shortcuts table.
     */
    private JPanel createShortcutsPanel() {
        final Map<String, String> pendingShortcuts = new HashMap<>();

        String[] columnNames = {"Action", "Shortcut"};
        // The third column holds the internal action key, which is not visible to the user.
        final Object[][] actionData = {
            {"Select Script Folder", "selectScriptFolder"},
            {"Select Input Folder", "selectInputFolder"},
            {"Refresh File Lists", "refreshAll"},
            {"Increase Font Size", "increaseFontSize"},
            {"Decrease Font Size", "decreaseFontSize"},
            {"Save Script", "saveScript"},
            {"Run Script", "runScript"},
            {"Toggle Output Panel", "toggleOutput"},
            {"Toggle Control Panels", "toggleControls"},
            {"Enable Editing", "enableEditing"},
            {"Toggle Input Panel", "toggleInputPanel"},
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
            public boolean isCellEditable(int row, int column) {
                return false; // Cells are not directly editable; editing is done via double-click.
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

        // Set column widths
        shortcutsTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        shortcutsTable.getColumnModel().getColumn(1).setPreferredWidth(250);

        JScrollPane scrollPane = new JScrollPane(shortcutsTable);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(scrollPane, BorderLayout.CENTER);
        // return panel;

        // --- Bottom Panel for Save button ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveChangesButton = new JButton("Save Changes");
        saveChangesButton.setEnabled(false); // Initially disabled
        bottomPanel.add(saveChangesButton);

        // // Add a mouse listener to handle double-click editing on the shortcut column
        // shortcutsTable.addMouseListener(new MouseAdapter() {
        //     public void mouseClicked(MouseEvent e) {
        //         if (e.getClickCount() == 2) { // Check for double-click
        //             int row = shortcutsTable.rowAtPoint(e.getPoint());
        //             int col = shortcutsTable.columnAtPoint(e.getPoint());
        //             if (row >= 0 && col == 1) { // Clicked on the "Shortcut" column
        //                 String actionKey = (String) actionData[row][1];
        //                 String currentShortcut = (String) model.getValueAt(row, 1);
        //                 String newShortcut = JOptionPane.showInputDialog(
        //                         panel, "Enter new shortcut for \"" + model.getValueAt(row, 0) + "\":\n(e.g., Ctrl + S / Shift + Enter)", currentShortcut);
        //                 if (newShortcut != null && !newShortcut.trim().isEmpty()) {
        //                     model.setValueAt(newShortcut, row, 1);
        //                     pendingShortcuts.put(actionKey, newShortcut);
        //                     saveChangesButton.setEnabled(true);
        //                 }
        //             }
        //         }
        //     }
        // });
        // saveChangesButton.addActionListener(e -> {
        //     for (Map.Entry<String, String> entry : pendingShortcuts.entrySet()) {
        //         updateShortcut(entry.getKey(), entry.getValue());
        //     }
        //     pendingShortcuts.clear();
        //     saveChangesButton.setEnabled(false);
        //     JOptionPane.showMessageDialog(panel, "Shortcuts have been updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
        // });
        // panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Updates the keyboard shortcut for a specific action. It removes the old
     * KeyStroke and adds the new one to the component's InputMap.
     *
     * @param actionMapKey The key identifying the action in the ActionMap.
     * @param newShortcutString The new shortcut in a human-readable format
     * (e.g., "Ctrl + S").
     */
    private void updateShortcut(String actionMapKey, String newShortcutString) {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        Action action = actionMap.get(actionMapKey);

        if (action == null) {
            System.err.println("Warning: No action found for key: " + actionMapKey);
            return;
        }

        // 1. Remove all old KeyStrokes associated with this action
        List<KeyStroke> toRemove = new ArrayList<>();
        for (KeyStroke oldKs : inputMap.keys()) {
            if (actionMapKey.equals(inputMap.get(oldKs))) {
                toRemove.add(oldKs);
            }
        }
        toRemove.forEach(inputMap::remove);

        // 2. Parse and add the new KeyStroke(s)
        String[] shortcutParts = newShortcutString.split("/");
        for (String part : shortcutParts) {
            String formattedPart = part.trim().replaceAll("\\s*\\+\\s*", " ");
            KeyStroke newKs = KeyStroke.getKeyStroke(formattedPart.toUpperCase());

            if (newKs != null) {
                inputMap.put(newKs, actionMapKey);
            } else {
                JOptionPane.showMessageDialog(settingsDialog,
                        "Invalid shortcut format: '" + part.trim() + "'. The change was not applied.",
                        "Invalid Shortcut", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Finds all KeyStrokes associated with a given action key and formats them
     * into a user-friendly string.
     *
     * @param actionMapKey The key for the action in the ActionMap (e.g.,
     * "saveScript").
     * @return A string representing the shortcuts (e.g., "Ctrl + S").
     */
    private String getShortcutStringForAction(String actionMapKey) {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        List<String> shortcuts = new ArrayList<>();
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
     *
     * @param ks The KeyStroke to format.
     * @return A formatted string (e.g., "Ctrl + Shift + S").
     */
    private String keyStrokeToString(KeyStroke ks) {
        if (ks == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int modifiers = ks.getModifiers();

        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            sb.append("Ctrl + ");
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            sb.append("Alt + ");
        }
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            sb.append("Shift + ");
        }

        // Use KeyEvent.getKeyText() for a user-friendly name of the key
        sb.append(KeyEvent.getKeyText(ks.getKeyCode()));

        return sb.toString();
    }

    /**
     * Sets the tab size (number of spaces) for the script and input text areas.
     *
     * @param size The number of spaces to use for a tab.
     */
    private void setTabSize(int size) {
        if (size < 1) {
            size = 1;
        }
        if (size > 16) {
            size = 16; // A reasonable upper limit

        }
        this.currentTabSize = size;
        if (scriptTextArea != null) {
            scriptTextArea.setTabSize(size);
            inputTextArea.setTabSize(size);
        }
    }

    /**
     * Creates the panel for adjusting editor font and tab sizes within the
     * settings dialog.
     *
     * @return A {@link JPanel} containing font and tab size controls and a
     * preview area.
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
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Editor Font Size:"), gbc);

        // Use the scriptTextArea's current font size as the initial value
        int initialSize = scriptTextArea.getFont().getSize();
        JSlider fontSizeSlider = new JSlider(8, 48, initialSize);
        JLabel currentSizeLabel = new JLabel(String.valueOf(initialSize));
        currentSizeLabel.setFont(currentSizeLabel.getFont().deriveFont(Font.BOLD));
        currentSizeLabel.setPreferredSize(new Dimension(30, 30)); // Give it a fixed width
        currentSizeLabel.setHorizontalAlignment(JLabel.CENTER);

        // --- Preview Area ---
        JTextArea previewArea = new JTextArea(
                "# This is a preview of the editor settings.\n"
                + "def example_function():\n"
                + "\t# Press Tab to see the new size.\n"
                + "\tprint(\"Hello, World!\")"
        );
        previewArea.setFont(scriptTextArea.getFont()); // Start with current font
        previewArea.setEditable(false);
        previewArea.setOpaque(true);
        previewArea.setBackground(scriptTextArea.getBackground());
        previewArea.setForeground(scriptTextArea.getForeground());
        previewArea.setBorder(createBoldTitledBorder("Preview"));

        // --- Listeners ---
        fontSizeSlider.addChangeListener(e -> {
            int newSize = fontSizeSlider.getValue();
            setFontSize(newSize);
            currentSizeLabel.setText(String.valueOf(newSize));
            previewArea.setFont(previewArea.getFont().deriveFont((float) newSize));
        });

        // --- Layout: Font Size ---
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(fontSizeSlider, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(currentSizeLabel, gbc);

        // --- Layout: Tab Size ---
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Tab Size (Indentations):"), gbc);

        SpinnerNumberModel tabSizeModel = new SpinnerNumberModel(currentTabSize, 1, 16, 1);
        JSpinner tabSizeSpinner = new JSpinner(tabSizeModel);
        // Set a preferred size to prevent it from being too wide
        tabSizeSpinner.setPreferredSize(new Dimension(10, tabSizeSpinner.getPreferredSize().height));

        tabSizeSpinner.addChangeListener(e -> {
            int newTabSize = (Integer) tabSizeSpinner.getValue();
            setTabSize(newTabSize);
            previewArea.setTabSize(newTabSize);
        });

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tabSizeSpinner, gbc);

        // --- Layout: Preview Area ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3; // Span across the first 3 columns
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(previewArea), gbc);

        return panel;
    }

    /**
     * Creates the "General" settings panel, which includes options like
     * resetting folder paths and toggling UI modes.
     *
     * @return A {@link JPanel} containing general application settings.
     */
    private JPanel createGeneralSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        panel.setBorder(createBoldTitledBorder("General Settings"));

        JButton resetFoldersBtn = new JButton("Reset Folder Selections");
        resetFoldersBtn.setToolTipText("Clears the selected script and input folders and removes them from preferences.");
        resetFoldersBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    settingsDialog,
                    "Are you sure you want to reset all saved folder paths?\nThis will clear your current script and input folder selections.",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                resetFolderSelections();
                JOptionPane.showMessageDialog(settingsDialog, "Folder selections have been reset.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        toggleFileExplorerModeBtn = new JButton("Toggle File Explorer mode");
        toggleFileExplorerModeBtn.setToolTipText("Toggles the file explorer mode.");
        toggleFileExplorerModeBtn.addActionListener(e -> toggleFileExplorerMode());

        JButton toggleExamModeBtn = new JButton("Toggle Exam Mode");
        toggleExamModeBtn.setToolTipText("Toggles a simplified UI for exam environments.");
        // toggleExamModeBtn.addActionListener(e -> toggleExamMode()); // Action to be implemented

        // Increase button height and add to panel
        for (JButton btn : Arrays.asList(resetFoldersBtn, toggleFileExplorerModeBtn, toggleExamModeBtn)) {
            Dimension size = btn.getPreferredSize();
            size.height += 7;
            btn.setPreferredSize(size);
            panel.add(btn);
        }

        return panel;
    }

    /**
     * Resets the application's saved script and input folder selections,
     * clearing them from the UI and user preferences.
     */
    private void resetFolderSelections() {
        Preferences prefs = Preferences.userNodeForPackage(PythonExecutor.class);
        prefs.remove(PREF_SCRIPT_DIR);
        prefs.remove(PREF_INPUT_DIR);
        prefs.remove(PREF_LAST_SCRIPT);

        scriptDirectory = null;
        inputDirectory = null;
        scriptFolderPathLabel.setText("");
        inputFolderPathLabel.setText("");

        loadPythonScripts(); // This will clear and reset the combo box
        loadInputFiles();    // This will clear and reset the combo box

        // Also reset the script text area to its initial placeholder state
        scriptTextArea.setForeground(Color.GRAY);
        scriptTextArea.setText(SCRIPT_PLACEHOLDER);
        scriptTextArea.setEditable(true);
        currentScriptLabel.setText("  Current Script: None");

        // If in explorer mode, update the explorer views to show they are empty.
        if (isFileExplorerMode) {
            populateFileExplorer(); // This will clear the tree model
            populateInputFileExplorer(); // This will clear the tree model
            fileExplorerCardLayout.show(fileExplorerContainer, EXPLORER_EMPTY_VIEW);
            inputFileExplorerCardLayout.show(inputFileExplorerContainer, INPUT_EXPLORER_EMPTY_VIEW);
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        outputTextArea.append(String.format("\n[%s] Folder selections have been reset.", timestamp));
    }

    /**
     * Initializes the file explorer components, including the JTree, context
     * menu, and card layouts for empty/populated views.
     */
    private void initializeFileExplorer() {
        // Create a root node that is not visible
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        fileExplorerTree = new JTree(root);
        fileExplorerTree.setRootVisible(false); // We only want to see the folders/files
        fileExplorerTree.setShowsRootHandles(true);

        // Add a custom renderer to show only the file name
        fileExplorerTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObject instanceof Path) {
                        setText(((Path) userObject).getFileName().toString());
                    }
                }
                return this;
            }
        });

        // Add a mouse listener for the context menu
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JTree tree = (JTree) e.getSource();
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row != -1) {
                        showContextMenu(tree, row, e);
                    }
                }
            }
        };
        fileExplorerTree.addMouseListener(ml);

        // Add a listener to handle file selection
        fileExplorerTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileExplorerTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.isLeaf()) {
                Object userObject = selectedNode.getUserObject();
                if (userObject instanceof Path) {
                    Path selectedPath = (Path) userObject;
                    // Find the path in the combo box and select it
                    // This will trigger the existing logic to load the script
                    for (int i = 0; i < scriptFileCombo.getItemCount(); i++) {
                        Object item = scriptFileCombo.getItemAt(i);
                        if (selectedPath.equals(item)) {
                            scriptFileCombo.setSelectedItem(item);
                            break;
                        }
                    }
                }
            }
        });

        // --- Create the full explorer panel with a custom header ---
        JPanel scriptExplorerPanel = new JPanel(new BorderLayout());
        scriptExplorerPanel.setOpaque(false);
        ActionListener newScriptFileAction = e -> createNewItem(scriptDirectory, false);
        ActionListener newScriptFolderAction = e -> createNewItem(scriptDirectory, true);
        scriptExplorerPanel.add(createExplorerHeaderPanel("File Explorer", newScriptFolderAction, newScriptFileAction, null), BorderLayout.NORTH);
        fileExplorerScrollPane = new JScrollPane(fileExplorerTree);
        fileExplorerScrollPane.setBorder(null);
        scriptExplorerPanel.add(fileExplorerScrollPane, BorderLayout.CENTER);

        // --- Input File Explorer Tree ---
        DefaultMutableTreeNode inputRoot = new DefaultMutableTreeNode();
        inputFileExplorerTree = new JTree(inputRoot);
        inputFileExplorerTree.setRootVisible(false);
        inputFileExplorerTree.setShowsRootHandles(true);
        inputFileExplorerTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObject instanceof Path) { // This will handle file nodes
                        setText(((Path) userObject).getFileName().toString());
                    } // Directory nodes will use their default string name
                }
                return this;
            }
        });
        inputFileExplorerTree.addMouseListener(ml);

        inputFileExplorerTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) inputFileExplorerTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.isLeaf()) {
                Object userObject = selectedNode.getUserObject();
                if (userObject instanceof Path) {
                    Path selectedPath = (Path) userObject;
                    // Find the path in the combo box and select it
                    for (int i = 0; i < inputFileCombo.getItemCount(); i++) {
                        Object item = inputFileCombo.getItemAt(i);
                        if (selectedPath.equals(item)) {
                            inputFileCombo.setSelectedItem(item);
                            break;
                        }
                    }
                }
            }
        });
        JPanel inputExplorerPanel = new JPanel(new BorderLayout());
        inputExplorerPanel.setOpaque(false);
        ActionListener newInputFileAction = e -> createNewItem(inputDirectory, false);
        ActionListener newInputFolderAction = e -> createNewItem(inputDirectory, true);
        ActionListener collapseAction = e -> toggleInputExplorerCollapse((JButton) e.getSource());
        inputExplorerPanel.add(createExplorerHeaderPanel("Input Explorer", newInputFolderAction, newInputFileAction, collapseAction), BorderLayout.NORTH);
        inputFileExplorerScrollPane = new JScrollPane(inputFileExplorerTree);
        inputFileExplorerScrollPane.setBorder(null);
        inputExplorerPanel.add(inputFileExplorerScrollPane, BorderLayout.CENTER);

        // --- CardLayout for switching between tree and empty view ---
        fileExplorerCardLayout = new CardLayout();
        fileExplorerContainer = new JPanel(fileExplorerCardLayout);

        // Create the "empty" view with an "Open Folder" button
        JPanel emptyViewPanel = new JPanel(new BorderLayout());
        emptyViewPanel.add(createExplorerHeaderPanel("File Explorer", newScriptFolderAction, newScriptFileAction, null), BorderLayout.NORTH);
        JPanel openButtonContainer = new JPanel(new GridBagLayout());
        JButton openFolderBtn = new JButton("Open Script Folder");
        openFolderBtn.addActionListener(e -> selectScriptFolder());
        openButtonContainer.add(openFolderBtn);
        emptyViewPanel.add(openButtonContainer, BorderLayout.CENTER);

        // Add both views (the tree and the empty panel) to the container
        fileExplorerContainer.add(scriptExplorerPanel, EXPLORER_TREE_VIEW);
        fileExplorerContainer.add(emptyViewPanel, EXPLORER_EMPTY_VIEW);

        // --- CardLayout for the Input File Explorer ---
        inputFileExplorerCardLayout = new CardLayout();
        inputFileExplorerContainer = new JPanel(inputFileExplorerCardLayout);
        JPanel emptyInputViewPanel = new JPanel(new BorderLayout());
        emptyInputViewPanel.add(createExplorerHeaderPanel("Input Explorer", newInputFolderAction, newInputFileAction, collapseAction), BorderLayout.NORTH);
        JPanel openInputButtonContainer = new JPanel(new GridBagLayout());
        JButton openInputFolderBtn = new JButton("Open Input Folder");
        openInputFolderBtn.addActionListener(e -> selectInputFolder());
        openInputButtonContainer.add(openInputFolderBtn);
        emptyInputViewPanel.add(openInputButtonContainer, BorderLayout.CENTER);
        inputFileExplorerContainer.add(inputExplorerPanel, INPUT_EXPLORER_TREE_VIEW);
        inputFileExplorerContainer.add(emptyInputViewPanel, INPUT_EXPLORER_EMPTY_VIEW);

        explorerMainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fileExplorerContainer, inputFileExplorerContainer);
        explorerMainSplitPane.setResizeWeight(0.5);
        explorerMainSplitPane.setDividerSize(3);

        // This is the new top-level split pane for the explorer mode
        explorerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        explorerSplitPane.setDividerSize(3);
    }

    /**
     * Populates the script file explorer tree with the contents of the
     * currently selected script directory.
     */
    private void populateFileExplorer() {
        if (scriptDirectory == null || fileExplorerTree == null) {
            // Clear the tree if no directory is selected
            javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) fileExplorerTree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            model.reload();
            return;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(scriptDirectory.getFileName().toString());
        addNodes(root, scriptDirectory.toFile());

        javax.swing.tree.DefaultTreeModel model = new javax.swing.tree.DefaultTreeModel(root);
        fileExplorerTree.setModel(model);
    }

    /**
     * Recursively adds nodes to the file explorer tree, representing the
     * directory structure and Python files within a given folder.
     *
     * @param parentNode The parent {@link DefaultMutableTreeNode} to which new
     * file/folder nodes will be added.
     * @param parentFile The parent {@link File} whose contents will be listed.
     */
    private void addNodes(DefaultMutableTreeNode parentNode, File parentFile) {
        File[] files = parentFile.listFiles();
        if (files == null) {
            return;
        }

        // Sort files: directories first, then files, alphabetically
        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            }
            if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            }
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File file : files) {
            if (file.isDirectory()) {
                DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(file.toPath());
                parentNode.add(dirNode);
                addNodes(dirNode, file); // Recurse
            } else {
                // Only add python files to the explorer
                if (file.getName().toLowerCase().endsWith(".py")) {
                    // Store the full Path object for later use
                    parentNode.add(new DefaultMutableTreeNode(file.toPath()));
                }
            }
        }
    }

    /**
     * Creates the right-click context menu for the file explorer trees.
     */
    private void createExplorerContextMenu() {
        explorerContextMenu = new JPopupMenu();
        editMenuItem = new JMenuItem("Edit");
        renameMenuItem = new JMenuItem("Rename");
        moveMenuItem = new JMenuItem("Move to...");
        deleteMenuItem = new JMenuItem("Delete");

        editMenuItem.addActionListener(e -> editSelectedFile());
        renameMenuItem.addActionListener(e -> renameSelectedItem());
        moveMenuItem.addActionListener(e -> moveSelectedItem());
        deleteMenuItem.addActionListener(e -> deleteSelectedItem());

        explorerContextMenu.add(editMenuItem);
        explorerContextMenu.addSeparator();
        explorerContextMenu.add(renameMenuItem);
        explorerContextMenu.add(moveMenuItem);
        explorerContextMenu.add(deleteMenuItem);
    }

    /**
     * Displays the file explorer context menu at the specified mouse location.
     * It also sets the context path for the menu actions.
     *
     * @param tree The JTree where the right-click occurred.
     * @param row The tree row that was clicked.
     * @param e The MouseEvent that triggered the context menu.
     */
    private void showContextMenu(JTree tree, int row, MouseEvent e) {
        tree.setSelectionRow(row);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null || !(node.getUserObject() instanceof Path)) {
            return;
        }

        this.contextMenuPath = (Path) node.getUserObject();

        boolean isFile = Files.isRegularFile(contextMenuPath);
        editMenuItem.setEnabled(isFile);

        explorerContextMenu.show(tree, e.getX(), e.getY());
    }

    /**
     * Handles the "Edit" action from the file explorer context menu, loading
     * the selected file into the appropriate editor.
     */
    private void editSelectedFile() {
        if (contextMenuPath == null || !Files.isRegularFile(contextMenuPath)) {
            return;
        }

        if (contextMenuPath.toString().toLowerCase().endsWith(".py")) {
            scriptFileCombo.setSelectedItem(contextMenuPath);
            // loadSelectedScript makes it non-editable, so we must enable it after.
            enableEditing();
        } else {
            inputFileCombo.setSelectedItem(contextMenuPath);
            // loadSelectedInputFile makes it non-editable, so we make it editable.
            inputTextArea.setEditable(true);
            inputTextArea.requestFocusInWindow();
        }
        // Refresh explorers to ensure UI consistency, as requested.
        refreshAllExplorers();
    }

    /**
     * Handles the "Rename" action from the file explorer context menu,
     * prompting the user for a new name and renaming the file/folder.
     */
    private void renameSelectedItem() {
        if (contextMenuPath == null) {
            return;
        }

        String oldName = contextMenuPath.getFileName().toString();
        String newName = JOptionPane.showInputDialog(this, "Enter new name for:", oldName);

        if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
            try {
                Path newPath = contextMenuPath.resolveSibling(newName.trim());
                Files.move(contextMenuPath, newPath);
                refreshExplorerForPath(contextMenuPath);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error renaming: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles the "Delete" action from the file explorer context menu,
     * confirming with the user before deleting the file/folder.
     */
    private void deleteSelectedItem() {
        if (contextMenuPath == null) {
            return;
        }

        String itemType = Files.isDirectory(contextMenuPath) ? "folder" : "file";
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete this " + itemType + "?\n" + contextMenuPath.getFileName(),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                if (Files.isDirectory(contextMenuPath)) {
                    try (Stream<Path> walk = Files.walk(contextMenuPath)) {
                        walk.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                    }
                } else {
                    Files.delete(contextMenuPath);
                }
                refreshExplorerForPath(contextMenuPath);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles the "Move" action from the file explorer context menu, allowing
     * the user to move the selected item to a different directory.
     */
    private void moveSelectedItem() {
        if (contextMenuPath == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Move '" + contextMenuPath.getFileName() + "' to...");
        chooser.setCurrentDirectory(contextMenuPath.getParent().toFile());

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path destinationDir = chooser.getSelectedFile().toPath();
            Path newPath = destinationDir.resolve(contextMenuPath.getFileName());
            try {
                Files.move(contextMenuPath, newPath);
                refreshAllExplorers();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error moving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Refreshes the file explorer and combo box for a directory that has been
     * modified (e.g., by a rename, delete, or move operation).
     *
     * @param path A path within the directory that needs refreshing.
     */
    private void refreshExplorerForPath(Path path) {
        // Save the expanded paths before refreshing to restore them later
        List<TreePath> scriptExpandedPaths = new ArrayList<>();
        if (fileExplorerTree != null) {
            for (int i = 0; i < fileExplorerTree.getRowCount(); i++) {
                if (fileExplorerTree.isExpanded(i)) {
                    scriptExpandedPaths.add(fileExplorerTree.getPathForRow(i));
                }
            }
        }
        List<TreePath> inputExpandedPaths = new ArrayList<>();
        if (inputFileExplorerTree != null) {
            for (int i = 0; i < inputFileExplorerTree.getRowCount(); i++) {
                if (inputFileExplorerTree.isExpanded(i)) {
                    inputExpandedPaths.add(inputFileExplorerTree.getPathForRow(i));
                }
            }
        }

        if (scriptDirectory != null && path.startsWith(scriptDirectory)) {
            loadPythonScripts();
            populateFileExplorer();
            // Restore expanded paths
            scriptExpandedPaths.forEach(p -> fileExplorerTree.expandPath(p));
        }
        if (inputDirectory != null && path.startsWith(inputDirectory)) {
            loadInputFiles();
            populateInputFileExplorer();
            // Restore expanded paths
            inputExpandedPaths.forEach(p -> inputFileExplorerTree.expandPath(p));
        }
    }

    /**
     * A convenience method that refreshes all file explorers and lists.
     */
    private void refreshAllExplorers() {
        refreshAll(); // Use the main refresh method which handles selection restoration
    }

    /**
     * Creates a standardized header panel for the file explorers.
     *
     * @param title The title to display in the header.
     * @param newFolderAction The action to perform when the "New Folder" button
     * is clicked.
     * @param newFileAction The action to perform when the "New File" button is
     * clicked.
     * @param collapseAction The action to perform for the collapse/expand
     * button (can be null).
     * @return A {@link JPanel} configured as an explorer header.
     */
    private JPanel createExplorerHeaderPanel(String title, ActionListener newFolderAction, ActionListener newFileAction, ActionListener collapseAction) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(80, 80, 80)));

        JLabel titleLabel = new JLabel("  " + title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 2f));
        titleLabel.setForeground(new Color(200, 200, 200));

        JButton newFileBtn = new JButton("\uD83D\uDCC4"); // 📄 Page Facing Up
        newFileBtn.setToolTipText("Create new file");
        styleToolbarButton(newFileBtn);
        newFileBtn.addActionListener(newFileAction);

        JButton newFolderBtn = new JButton("\uD83D\uDCC1"); // 📁 File Folder
        newFolderBtn.setToolTipText("Create new folder");
        styleToolbarButton(newFolderBtn);
        newFolderBtn.addActionListener(newFolderAction);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonsPanel.setOpaque(false);

        if (collapseAction != null) {
            JButton collapseBtn = new JButton("\u25BC"); // ▼ Down-pointing triangle
            collapseBtn.setToolTipText("Collapse");
            styleToolbarButton(collapseBtn);
            collapseBtn.addActionListener(collapseAction);
            buttonsPanel.add(collapseBtn);
        }
        buttonsPanel.add(newFileBtn);
        buttonsPanel.add(newFolderBtn);

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(buttonsPanel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Applies a consistent style to toolbar buttons used in explorer headers.
     *
     * @param button The {@link JButton} to style.
     */
    private void styleToolbarButton(JButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setOpaque(true);
                button.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setOpaque(false);
                button.setBackground(null);
            }
        });
    }

    /**
     * Creates a new file or folder in the specified parent directory after
     * prompting the user for a name.
     *
     * @param parentDir The directory in which to create the new item.
     * @param isFolder True to create a folder, false to create a file.
     */
    private void createNewItem(Path parentDir, boolean isFolder) {
        if (parentDir == null) {
            JOptionPane.showMessageDialog(this, "Please open a folder first.", "Cannot Create Item", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemType = isFolder ? "folder" : "file";
        String itemName = JOptionPane.showInputDialog(this, "Enter new " + itemType + " name:", "Create New " + (isFolder ? "Folder" : "File"), JOptionPane.PLAIN_MESSAGE);

        if (itemName == null || itemName.trim().isEmpty()) {
            return; // User cancelled or entered nothing
        }

        Path newItemPath = parentDir.resolve(itemName.trim());

        try {
            if (isFolder) {
                if (Files.exists(newItemPath)) {
                    throw new IOException("A folder with that name already exists.");
                }
                Files.createDirectory(newItemPath);
            } else {
                if (Files.exists(newItemPath)) {
                    throw new IOException("A file with that name already exists.");
                }
                Files.createFile(newItemPath);
            }

            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            outputTextArea.append(String.format("\n[%s] Created %s: %s", timestamp, itemType, newItemPath.getFileName()));

            // Refresh the explorer that contains the new item. This also reloads the combo boxes.
            refreshExplorerForPath(parentDir);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not create " + itemType + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Toggles the collapsed/expanded state of the input file explorer panel.
     *
     * @param collapseButton The button that triggered the action, used to
     * update its text/icon.
     */
    private void toggleInputExplorerCollapse(JButton collapseButton) {
        isInputExplorerCollapsed = !isInputExplorerCollapsed;
        if (isInputExplorerCollapsed) {
            // Collapse the panel
            lastExplorerSplitPaneLocation = explorerMainSplitPane.getDividerLocation();

            // --- New logic to collapse leaving header visible ---
            Component bottomComponent = explorerMainSplitPane.getBottomComponent();
            int headerHeight = 35; // A sensible default/fallback height

            if (bottomComponent instanceof JPanel) {
                JPanel container = (JPanel) bottomComponent;
                // Find the visible card within the CardLayout
                for (Component comp : container.getComponents()) {
                    if (comp.isVisible() && comp instanceof JPanel) {
                        JPanel cardPanel = (JPanel) comp;
                        if (cardPanel.getLayout() instanceof BorderLayout) {
                            Component northComponent = ((BorderLayout) cardPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                            if (northComponent != null) {
                                headerHeight = northComponent.getPreferredSize().height;
                            }
                        }
                        break; // Found the visible card
                    }
                }
            }
            int dividerSize = explorerMainSplitPane.getDividerSize();
            int newLocation = explorerMainSplitPane.getHeight() - headerHeight - dividerSize;
            explorerMainSplitPane.setDividerLocation(newLocation);

            collapseButton.setText("\u25B2"); // ▲ Up-pointing triangle
            collapseButton.setToolTipText("Expand");
        } else {
            // Expand the panel
            if (lastExplorerSplitPaneLocation != -1) {
                explorerMainSplitPane.setDividerLocation(lastExplorerSplitPaneLocation);
            } else {
                explorerMainSplitPane.setResizeWeight(0.5); // Fallback to 50/50 split
            }
            collapseButton.setText("\u25BC"); // ▼ Down-pointing triangle
            collapseButton.setToolTipText("Collapse");
        }
    }

    /**
     * A custom JPanel that paints a rounded rectangular background.
     */
    public static class RoundedPanel extends JPanel {

        private final int cornerRadius;
        private final Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            super();
            this.cornerRadius = radius;
            this.backgroundColor = bgColor;
            setOpaque(false); // We will paint our own background
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension arcs = new Dimension(cornerRadius, cornerRadius);
            int width = getWidth();
            int height = getHeight();
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draws the rounded panel with a background color.
            graphics.setColor(backgroundColor);
            graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
            graphics.setColor(getForeground()); // You can set a border color here if you want
            // graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);
        }
    }

    /**
     * Increases or decreases the font size of the main text areas.
     *
     * @param delta The amount to change the font size by (e.g., 1 or -1).
     */
    private void changeFontSize(int delta) {
        Font currentFont = scriptTextArea.getFont();
        float newSize = currentFont.getSize() + delta;
        setFontSize(newSize);
    }

    /**
     * Sets the font size for the script, input, and output text areas.
     *
     * @param newSize The new font size.
     */
    private void setFontSize(float newSize) {
        // Prevent font from becoming too small or too large
        if (newSize < 8) {
            newSize = 8;
        }
        if (newSize > 72) {
            newSize = 72;
        }

        Font newFont = scriptTextArea.getFont().deriveFont(newSize);
        scriptTextArea.setFont(newFont);
        inputTextArea.setFont(newFont);
        outputTextArea.setFont(newFont);
    }

    /**
     * Initializes the popup menu used for displaying file search results.
     */
    private void initializeSearchPopup() {
        searchResultPopup = new JPopupMenu();
        searchResultPopup.setFocusable(false); // Prevent the popup itself from stealing focus
        searchResultPopup.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        scriptListModel = new DefaultListModel<>();
        JList<Path> scriptResultList = new JList<>(scriptListModel);

        inputListModel = new DefaultListModel<>();
        JList<Path> inputResultList = new JList<>(inputListModel);

        ListCellRenderer<Path> fileRenderer = (list, value, index, isSelected, cellHasFocus) -> {
            String fileName = (value != null) ? value.getFileName().toString() : "";
            JLabel label = new JLabel(fileName);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            if (isSelected) {
                label.setBackground(new Color(0x4A6D9A)); // A nice selection color
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(new Color(60, 60, 60)); // Dark background
                label.setForeground(new Color(200, 200, 200)); // Light text
            }
            return label;
        };

        scriptResultList.setCellRenderer(fileRenderer);
        inputResultList.setCellRenderer(fileRenderer);

        scriptResultList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    Path selected = scriptResultList.getSelectedValue();
                    if (selected != null) {
                        scriptFileCombo.setSelectedItem(selected);
                        searchResultPopup.setVisible(false);
                        searchField.setText(""); // Clear search field after selection
                    }
                }
            }
        });

        inputResultList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    Path selected = inputResultList.getSelectedValue();
                    if (selected != null) {
                        inputFileCombo.setSelectedItem(selected);
                        searchResultPopup.setVisible(false);
                        searchField.setText(""); // Clear search field after selection
                    }
                }
            }
        });

        JPanel scriptPanel = new JPanel(new BorderLayout());
        scriptPanel.add(new JLabel("  Scripts"), BorderLayout.NORTH);
        scriptPanel.add(new JScrollPane(scriptResultList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("  Input Files"), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputResultList), BorderLayout.CENTER);

        JSplitPane popupSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scriptPanel, inputPanel);
        popupSplitPane.setResizeWeight(0.5);
        popupSplitPane.setBorder(null);

        JPanel mainPopupPanel = new JPanel(new BorderLayout());
        mainPopupPanel.add(popupSplitPane, BorderLayout.CENTER);
        searchResultPopup.add(mainPopupPanel);
    }

    /**
     * Filters the script and input file lists based on the current search term
     * and displays the results in a popup menu below the search field.
     */
    private void updateAndShowSearchResults() {
        String searchTerm = searchField.getText().toLowerCase();

        if (searchTerm.isEmpty() || searchTerm.equals("search files...")) {
            searchResultPopup.setVisible(false);
            return;
        }

        scriptListModel.clear();
        allScriptFiles.stream()
                .filter(p -> p.getFileName().toString().toLowerCase().contains(searchTerm))
                .forEach(scriptListModel::addElement);

        inputListModel.clear();
        allInputFiles.stream()
                .filter(p -> p.getFileName().toString().toLowerCase().contains(searchTerm))
                .forEach(inputListModel::addElement);

        if (!scriptListModel.isEmpty() || !inputListModel.isEmpty()) {
            searchResultPopup.show(searchField, 0, searchField.getHeight());
            searchResultPopup.pack();
            searchResultPopup.setPopupSize(searchField.getWidth(), searchResultPopup.getPreferredSize().height);
        } else {
            searchResultPopup.setVisible(false);
        }
    }

    /**
     * Creates a TitledBorder with a bold and slightly larger font.
     *
     * @param title The title for the border.
     * @return A {@link TitledBorder} with custom styling.
     */
    private TitledBorder createBoldTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        Font currentFont = border.getTitleFont();
        border.setTitleFont(currentFont.deriveFont(Font.BOLD, currentFont.getSize() + 4f));
        return border;
    }

    /**
     * Toggles the UI between "File Explorer" mode and "Classic" mode.
     */
    private void toggleFileExplorerMode() {
        setFileExplorerMode(!this.isFileExplorerMode);
    }

    /**
     * Toggles the visibility of the top control panels (file selection,
     * execution controls).
     */
    private void toggleControlsVisibility() {
        areControlsVisible = !areControlsVisible;
        scriptSelectionPanel.setVisible(areControlsVisible);
        allControlsPanel.setVisible(areControlsVisible);
        hideControlsBtn.setText(areControlsVisible ? "Hide Controls" : "Show Controls");
        revalidate();
        repaint();
    }

    /**
     * Switches the main UI layout between "Classic" mode (with dropdowns) and
     * "File Explorer" mode (with a file tree).
     *
     * @param enabled True to enable File Explorer mode, false to switch to
     * Classic mode.
     */
    private void setFileExplorerMode(boolean enabled) {
        if (this.isFileExplorerMode == enabled) {
            return;
        }

        this.isFileExplorerMode = enabled;
        JPanel scriptPanel = (JPanel) middleSplitPane.getLeftComponent();

        if (isFileExplorerMode) {
            // Entering File Explorer Mode
            if (toggleFileExplorerModeBtn != null) {
                toggleFileExplorerModeBtn.setText("Toggle Classic Mode");
            }

            // Remove the file selection panel to expand the script editor
            scriptPanel.remove(scriptSelectionPanel);

            // Re-parent the mainSplitPane into the new explorerSplitPane
            mainPanel.remove(mainSplitPane);
            explorerSplitPane.setLeftComponent(explorerMainSplitPane);
            explorerSplitPane.setRightComponent(mainSplitPane);
            mainPanel.add(explorerSplitPane, BorderLayout.CENTER);
            explorerSplitPane.setDividerLocation(250);

            // Show the correct view: the tree if a directory is open, or the button otherwise.
            if (scriptDirectory != null) {
                populateFileExplorer();
                fileExplorerCardLayout.show(fileExplorerContainer, EXPLORER_TREE_VIEW);
            } else {
                fileExplorerCardLayout.show(fileExplorerContainer, EXPLORER_EMPTY_VIEW);
            }
            if (inputDirectory != null) {
                populateInputFileExplorer();
                inputFileExplorerCardLayout.show(inputFileExplorerContainer, INPUT_EXPLORER_TREE_VIEW);
            } else {
                inputFileExplorerCardLayout.show(inputFileExplorerContainer, INPUT_EXPLORER_EMPTY_VIEW);
            }
        } else {
            // Exiting File Explorer Mode
            if (toggleFileExplorerModeBtn != null) {
                toggleFileExplorerModeBtn.setText("Toggle File Explorer mode");
            }

            // Add the file selection panel back to its original position
            scriptPanel.add(scriptSelectionPanel, BorderLayout.NORTH);

            // Re-parent the mainSplitPane back to the mainPanel
            mainPanel.remove(explorerSplitPane);
            mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        }
        scriptPanel.revalidate();
        scriptPanel.repaint();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Toggles the visibility of the right-hand panel containing the input text
     * area and controls.
     */
    private void toggleInputPanelVisibility() {
        if (isInputPanelVisible) {
            lastMiddleDividerLocation = middleSplitPane.getDividerLocation();
            middleSplitPane.setDividerLocation(1.0); // Hide right component
            isInputPanelVisible = false;
        } else {
            if (lastMiddleDividerLocation != -1) {
                middleSplitPane.setDividerLocation(lastMiddleDividerLocation);
            } else {
                // Fallback to default if no previous location is stored
                middleSplitPane.setDividerLocation(0.5);
            }
            isInputPanelVisible = true;
        }
    }

    /**
     * Toggles the visibility of the bottom output panel.
     */
    private void toggleOutputVisibility() {
        if (isOutputVisible) {
            lastDividerLocation = mainSplitPane.getDividerLocation();
            mainSplitPane.setDividerLocation(1.0); // Hide by moving divider to the bottom
            toggleOutputBtn.setText("Show Output");
            isOutputVisible = false;
        } else {
            if (lastDividerLocation != -1) {
                mainSplitPane.setDividerLocation(lastDividerLocation);
            } else {
                // Fallback to default if no previous location is stored
                mainSplitPane.setDividerLocation(0.65);
                mainSplitPane.setDividerLocation(0.5);
            }
            toggleOutputBtn.setText("Hide Output");
            isOutputVisible = true;
        }
    }

    /**
     * Adds placeholder text to a JTextArea that appears when the text area is
     * empty and not in focus.
     *
     * @param textArea The JTextArea to add the placeholder to.
     * @param placeholder The placeholder text to display.
     */
    private void addPlaceholder(JTextArea textArea, String placeholder) {
        // Set initial placeholder state
        textArea.setText(placeholder);
        textArea.setForeground(Color.GRAY);

        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(placeholder)) {
                    textArea.setText("");
                    textArea.setForeground(UIManager.getColor("TextArea.foreground"));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText(placeholder);
                }
            }
        });
    }

    /**
     * Executes the Python script currently in the script text area. It runs the
     * script in a separate process, captures its standard output and error
     * streams, and displays them in the output text area.
     */
    private void executePythonScript() {
        if (!isPythonAvailable) {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            outputTextArea.append(String.format("\n[%s] [ERROR] Python not found in system PATH. Script execution is unavailable.", timestamp));
            incrementErrorCount();
            return;
        }

        String scriptContent = scriptTextArea.getText();
        if (scriptContent.trim().isEmpty() || scriptContent.equals(SCRIPT_PLACEHOLDER)) {
            outputTextArea.append("\n[ERROR] Script is empty. Nothing to run.");
            return;
        }

        clearErrorHighlights();
        resetErrorCount(); // Reset errors before a new run

        // Determine script name for logging
        String scriptName;
        if (scriptFileCombo.getSelectedItem() instanceof Path) {
            scriptName = ((Path) scriptFileCombo.getSelectedItem()).getFileName().toString();
        } else {
            scriptName = "unsaved script";
        }

        // Format timestamp and start message
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        outputTextArea.append(String.format("\n\n[%s] Running: %s\n", timestamp, scriptName));

        executorService.submit(() -> {
            Path tempScript = null;
            Process runningProcess = null;
            try {
                tempScript = Files.createTempFile("tempscript_", ".py");
                Files.writeString(tempScript, scriptContent);

                ProcessBuilder pb = new ProcessBuilder("python", tempScript.toString());

                if (scriptDirectory != null && Files.isDirectory(scriptDirectory)) {
                    pb.directory(scriptDirectory.toFile());
                    Map<String, String> env = pb.environment();
                    String scriptDirPath = scriptDirectory.toAbsolutePath().toString();
                    String pythonPath = env.get("PYTHONPATH");
                    String newPythonPath = (pythonPath == null || pythonPath.isEmpty())
                            ? scriptDirPath
                            : scriptDirPath + File.pathSeparator + pythonPath;
                    env.put("PYTHONPATH", newPythonPath);
                }

                runningProcess = pb.start();

                final Process process = runningProcess; // Create a final variable for use in lambdas

                // Use a separate executor for I/O streams to avoid deadlocks
                ExecutorService ioExecutor = Executors.newFixedThreadPool(3);

                // Thread to handle script input
                ioExecutor.submit(() -> {
                    String inputContent = inputTextArea.getText();
                    if (inputContent != null && !inputContent.isEmpty()) {
                        try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                            writer.print(inputContent);
                            writer.flush();
                        }
                    }
                    // IMPORTANT: Close the process's output stream to signal 'end of input' to the script.
                    try {
                        process.getOutputStream().close();
                    } catch (IOException e) {
                        // This might happen if the process terminates quickly.
                    }
                });

                // Thread to handle script standard output
                ioExecutor.submit(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String outputLine = line;
                            SwingUtilities.invokeLater(() -> outputTextArea.append(outputLine + "\n"));
                        }
                    } catch (IOException e) {
                        /* Ignore, stream might be closed */ }
                });

                // Thread to handle script standard error
                ioExecutor.submit(() -> {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            final String errorLine = line;
                            SwingUtilities.invokeLater(() -> {
                                outputTextArea.append("[ERROR] " + errorLine + "\n");
                                parseAndHighlightError(errorLine);
                            });
                            incrementErrorCount();
                        }
                    } catch (IOException e) {
                        /* Ignore, stream might be closed */ }
                });

                boolean finished = runningProcess.waitFor(60, TimeUnit.SECONDS);
                if (!finished) {
                    runningProcess.destroyForcibly();
                }
                int exitCode = runningProcess.exitValue();
                final String finishMessage = "\n--- Script finished with exit code " + exitCode + " ---\n";
                SwingUtilities.invokeLater(() -> outputTextArea.append(finishMessage));

                // Shut down the I/O executor
                ioExecutor.shutdownNow();

            } catch (IOException | InterruptedException e) {
                SwingUtilities.invokeLater(() -> outputTextArea.append("\n[FATAL] An error occurred: " + e.getMessage() + "\n"));
                e.printStackTrace();
            } finally {
                if (tempScript != null) try {
                    Files.delete(tempScript);
                } catch (IOException e) {
                    /* ignore */
                }
            }
        });
    }

    /**
     * Refreshes the file lists for both scripts and inputs by reloading them
     * from their respective directories, attempting to preserve the current
     * selection.
     */
    private void refreshAll() {
        // Store the current selections
        Object selectedScript = scriptFileCombo.getSelectedItem();
        Object selectedInput = inputFileCombo.getSelectedItem();

        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        resetErrorCount();
        outputTextArea.append(String.format("\n[%s] Refreshing file lists...", timestamp));

        // Reload the file lists from the directories
        loadPythonScripts();
        loadInputFiles();

        // Also refresh the explorer views
        populateFileExplorer();
        populateInputFileExplorer();

        // Try to restore the previous selections
        // The ActionListeners on the combo boxes will handle reloading the content
        scriptFileCombo.setSelectedItem(selectedScript);
        inputFileCombo.setSelectedItem(selectedInput);
    }

    /**
     * Saves the content of the script text area. If a file is already selected,
     * it overwrites it. Otherwise, it prompts the user for a new file location.
     */
    private void saveScript() {
        String scriptContent = scriptTextArea.getText();
        if (scriptContent.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Script is empty. Nothing to save.", "Empty Script", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // If a script is already selected, save to it directly.
        if (scriptFileCombo.getSelectedItem() instanceof Path) {
            saveToFile((Path) scriptFileCombo.getSelectedItem(), scriptContent);
            return;
        }

        // Otherwise, open the "Save As" dialog.
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Python Script");
        chooser.setFileFilter(new FileNameExtensionFilter("Python Scripts (*.py)", "py"));
        chooser.setSelectedFile(new File("new_script.py"));

        if (scriptDirectory != null) {
            chooser.setCurrentDirectory(scriptDirectory.toFile());
        }

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".py")) {
                fileToSave = new File(filePath + ".py");
            }

            if (fileToSave.exists()) {
                int overwriteResult = JOptionPane.showConfirmDialog(
                        this,
                        "The file \"" + fileToSave.getName() + "\" already exists.\nDo you want to replace it?",
                        "Confirm Save",
                        JOptionPane.YES_NO_OPTION
                );
                if (overwriteResult != JOptionPane.YES_OPTION) {
                    return; // User cancelled the overwrite
                }
            }

            saveToFile(fileToSave.toPath(), scriptContent);
        }
    }

    /**
     * Writes the given content to the specified file path in a background
     * thread. Updates the UI upon completion.
     *
     * @param path The {@link Path} of the file to save.
     * @param content The string content to write to the file.
     */
    private void saveToFile(Path path, String content) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                Files.writeString(path, content);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions from doInBackground
                    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                    outputTextArea.append(String.format("\n[%s] Script saved to: %s", timestamp, path.getFileName()));
                    // Refresh the script list if the file was saved in the current script directory
                    if (scriptDirectory != null && path.getParent().equals(scriptDirectory)) {
                        // Avoid re-loading if the item is already there
                        if (!isPathInComboBox(path)) {
                            loadPythonScripts();
                        }
                        scriptFileCombo.setSelectedItem(path);
                    }
                } catch (Exception e) {
                    outputTextArea.append("\n[ERROR] Failed to save script: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    /**
     * Enables editing on the script text area if it is currently read-only.
     */
    private void enableEditing() {
        if (!scriptTextArea.isEditable()) {
            scriptTextArea.setEditable(true);
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            Object selectedScript = scriptFileCombo.getSelectedItem();
            String scriptName = (selectedScript instanceof Path) ? ((Path) selectedScript).getFileName().toString() : "current script";
            outputTextArea.append(String.format("\n[%s] Editing enabled for %s.", timestamp, scriptName));
            scriptTextArea.requestFocusInWindow();
        } else {
            scriptTextArea.requestFocusInWindow();
        }
    }

    /**
     * Increments the total error count and updates the status bar label.
     */
    private void incrementErrorCount() {
        totalErrors++;
        errorStatusLabel.setText(String.format("Errors: %d", totalErrors));
    }

    /**
     * Resets the total error count to zero and updates the status bar label.
     */
    private void resetErrorCount() {
        totalErrors = 0;
        errorStatusLabel.setText("Errors: 0");
    }

    /**
     * Checks if a given path is already present in the script file combo box.
     *
     * @param path The {@link Path} to check for.
     * @return True if the path exists in the combo box, false otherwise.
     */
    private boolean isPathInComboBox(Path path) {
        for (int i = 0; i < scriptFileCombo.getItemCount(); i++) {
            Object item = scriptFileCombo.getItemAt(i);
            if (item instanceof Path && item.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens a file chooser dialog to allow the user to select a directory
     * containing Python scripts.
     */
    private void selectScriptFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Script Folder");
        if (scriptDirectory != null) {
            chooser.setCurrentDirectory(scriptDirectory.toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            scriptDirectory = chooser.getSelectedFile().toPath();
            scriptFolderPathLabel.setText(scriptDirectory.toString());
            addRecentFolder(scriptDirectory);
            updateGitManager();
            loadPythonScripts();
            populateFileExplorer();

            // If we are in explorer mode, switch from the "Open Folder" view to the tree view.
            if (isFileExplorerMode) {
                fileExplorerCardLayout.show(fileExplorerContainer, EXPLORER_TREE_VIEW);
            }
        }
    }

    /**
     * Loads all `.py` files from the selected script directory into the script
     * file combo box and the internal file list.
     */
    private void loadPythonScripts() {
        scriptFileCombo.removeAllItems();
        allScriptFiles.clear();
        scriptFileCombo.addItem("Select a script");
        if (scriptDirectory != null && Files.isDirectory(scriptDirectory)) {
            try (Stream<Path> paths = Files.list(scriptDirectory)) {
                allScriptFiles = paths.filter(p -> !Files.isDirectory(p) && p.toString().toLowerCase().endsWith(".py"))
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());
                allScriptFiles.forEach(scriptFileCombo::addItem);
            } catch (IOException e) {
                e.printStackTrace();
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                outputTextArea.append(String.format("\n[%s] [ERROR] Could not read directory: %s", timestamp, e.getMessage()));
                populateFileExplorer(); // Also update explorer on failure
            }
        }
    }

    /**
     * Loads the content of the script file selected in the combo box into the
     * main script text area.
     */
    private void loadSelectedScript() {
        Object selected = scriptFileCombo.getSelectedItem();
        if (selected instanceof Path) {
            final Path scriptPath = (Path) selected;
            // Disable UI to prevent interaction while loading
            scriptFileCombo.setEnabled(false);
            runBtn.setEnabled(false);

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws IOException {
                    return Files.readString(scriptPath);
                }

                @Override
                protected void done() {
                    try {
                        String content = get();
                        scriptTextArea.setText(content);
                        scriptTextArea.setForeground(Color.WHITE); // Set text color to white
                        scriptTextArea.setEditable(false); // Make non-editable
                        scriptTextArea.setCaretPosition(0); // Scroll to top
                        currentScriptLabel.setText("  Current Script: " + scriptPath.getFileName());
                        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                        outputTextArea.append(String.format("\n[%s] Loaded script: %s. Click 'Edit' to make changes.", timestamp, scriptPath.getFileName()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                        outputTextArea.append(String.format("\n[%s] [ERROR] Could not read file: %s", timestamp, e.getCause().getMessage()));
                    } finally {
                        scriptFileCombo.setEnabled(true);
                        runBtn.setEnabled(true);
                    }
                }
            }.execute();
        } else {
            // This handles the "Select a script" case
            if (scriptTextArea.getText().isEmpty()) { // Only reset if user hasn't typed anything
                addPlaceholder(scriptTextArea, SCRIPT_PLACEHOLDER);
            }
            scriptTextArea.setEditable(true);
            currentScriptLabel.setText("  Current Script: None");
        }
    }

    /**
     * Opens a file chooser dialog to allow the user to select a directory
     * containing input files.
     */
    private void selectInputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Input File Folder");
        if (inputDirectory != null) {
            chooser.setCurrentDirectory(inputDirectory.toFile());
        } else if (scriptDirectory != null) { // Fallback to script dir
            chooser.setCurrentDirectory(scriptDirectory.toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            inputDirectory = chooser.getSelectedFile().toPath();
            inputFolderPathLabel.setText(inputDirectory.toString());
            addRecentFolder(inputDirectory);
            updateGitManager();
            loadInputFiles();
            populateInputFileExplorer();

            if (isFileExplorerMode) {
                inputFileExplorerCardLayout.show(inputFileExplorerContainer, INPUT_EXPLORER_TREE_VIEW);
            }
        }
    }

    /**
     * Updates the text and tooltip of the search field to show the current
     * working directory.
     */
    private void updateWorkingDirectory() {
        // This method's logic was complex and tied to findGitRepository.
        // It is now deprecated and its functionality (showing a path) is handled elsewhere.
    }

    /**
     * Loads all files from the selected input directory into the input file
     * combo box and the internal file list.
     */
    private void loadInputFiles() {
        inputFileCombo.removeAllItems();
        allInputFiles.clear();
        inputFileCombo.addItem("Select an input file");
        if (inputDirectory != null && Files.isDirectory(inputDirectory)) {
            try (Stream<Path> paths = Files.list(inputDirectory)) {
                allInputFiles = paths.filter(p -> !Files.isDirectory(p))
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());
                allInputFiles.forEach(inputFileCombo::addItem);
            } catch (IOException e) {
                e.printStackTrace();
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                outputTextArea.append(String.format("\n[%s] [ERROR] Could not read input directory: %s", timestamp, e.getMessage()));
            }
        }
    }

    /**
     * Loads the content of the file selected in the input file combo box into
     * the input text area.
     */
    private void loadSelectedInputFile() {
        Object selected = inputFileCombo.getSelectedItem();
        if (selected instanceof Path) {
            final Path inputPath = (Path) selected;
            // Disable UI to prevent interaction while loading
            inputFileCombo.setEnabled(false);
            runBtn.setEnabled(false);

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws IOException {
                    return Files.readString(inputPath);
                }

                @Override
                protected void done() {
                    try {
                        String content = get();
                        inputTextArea.setText(content);
                        inputTextArea.setCaretPosition(0);
                        inputTextArea.setEditable(false);
                        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                        outputTextArea.append(String.format("\n[%s] Loaded input from: %s", timestamp, inputPath.getFileName()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
                        outputTextArea.append(String.format("\n[%s] [ERROR] Could not read input file: %s", timestamp, e.getCause().getMessage()));
                    } finally {
                        inputFileCombo.setEnabled(true);
                        runBtn.setEnabled(true);
                    }
                }
            }.execute();
        } else {
            inputTextArea.setText("");
            inputTextArea.setEditable(true);
        }
    }

    /**
     * Creates and shows the non-modal find dialog if it's not already visible.
     */
    private void showFindDialog() {
        if (findDialog == null) {
            createFindDialog();
        }
        findDialog.setVisible(true);
    }

    /**
     * Creates the non-modal find dialog with its components and listeners.
     */
    private void createFindDialog() {
        findDialog = new JDialog(this, "Find", false); // false for non-modal
        findDialog.setSize(450, 220);
        findDialog.setLocationRelativeTo(this);
        findDialog.setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Find field
        gbc.gridx = 0;
        gbc.gridy = 0;
        findDialog.add(new JLabel("Find:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField findField = new JTextField(20);
        findDialog.add(findField, gbc);

        // Replace field
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        findDialog.add(new JLabel("Replace with:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField replaceField = new JTextField(20);
        findDialog.add(replaceField, gbc);

        // Buttons
        gbc.gridy = 2;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton findNextBtn = new JButton("Find Next");
        findDialog.add(findNextBtn, gbc);

        gbc.gridx = 2;
        JButton findPrevBtn = new JButton("Find Previous");
        findDialog.add(findPrevBtn, gbc);

        gbc.gridy = 3;
        gbc.gridx = 1;
        JButton replaceBtn = new JButton("Replace");
        findDialog.add(replaceBtn, gbc);

        gbc.gridx = 2;
        JButton replaceAllBtn = new JButton("Replace All");
        findDialog.add(replaceAllBtn, gbc);

        // Checkbox
        gbc.gridy = 4;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        final JCheckBox caseSensitiveCheck = new JCheckBox("Case Sensitive");
        findDialog.add(caseSensitiveCheck, gbc);

        // --- Listeners ---
        findNextBtn.addActionListener(e -> findInScriptArea(findField.getText(), true, caseSensitiveCheck.isSelected()));
        findPrevBtn.addActionListener(e -> findInScriptArea(findField.getText(), false, caseSensitiveCheck.isSelected()));
        replaceBtn.addActionListener(e -> replaceInScriptArea(findField.getText(), replaceField.getText(), caseSensitiveCheck.isSelected()));
        replaceAllBtn.addActionListener(e -> replaceAllInScriptArea(findField.getText(), replaceField.getText(), caseSensitiveCheck.isSelected()));

        // Find next on Enter press
        findField.addActionListener(e -> findInScriptArea(findField.getText(), true, caseSensitiveCheck.isSelected()));
    }

    /**
     * Replaces the currently selected text if it matches the find text, then
     * finds the next occurrence.
     *
     * @param findText The text to search for.
     * @param replaceText The text to replace with.
     * @param caseSensitive True for case-sensitive search.
     */
    private void replaceInScriptArea(String findText, String replaceText, boolean caseSensitive) {
        String selectedText = scriptTextArea.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            // If nothing is selected, just find the next occurrence
            findInScriptArea(findText, true, caseSensitive);
            return;
        }

        String comparisonFind = caseSensitive ? findText : findText.toLowerCase();
        String comparisonSelected = caseSensitive ? selectedText : selectedText.toLowerCase();

        if (comparisonSelected.equals(comparisonFind)) {
            scriptTextArea.replaceSelection(replaceText);
            // After replacing, automatically find the next one
            findInScriptArea(findText, true, caseSensitive);
        } else {
            // The selection doesn't match the find text, so just find the next one
            findInScriptArea(findText, true, caseSensitive);
        }
    }

    /**
     * Replaces all occurrences of the find text with the replace text.
     *
     * @param findText The text to search for.
     * @param replaceText The text to replace with.
     * @param caseSensitive True for case-sensitive search.
     */
    private void replaceAllInScriptArea(String findText, String replaceText, boolean caseSensitive) {
        if (findText == null || findText.isEmpty()) {
            return;
        }

        String originalContent = scriptTextArea.getText();
        String newContent;
        int replacementCount = (originalContent.length() - originalContent.replace(findText, "").length()) / findText.length();

        newContent = caseSensitive ? originalContent.replace(findText, replaceText) : originalContent.replaceAll("(?i)" + java.util.regex.Pattern.quote(findText), replaceText);

        scriptTextArea.setText(newContent);
        JOptionPane.showMessageDialog(findDialog, "Replaced " + replacementCount + " occurrence(s).", "Replace All Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Finds text in the script area.
     *
     * @param findText The text to search for.
     * @param forward True to search forward, false to search backward.
     * @param caseSensitive True for case-sensitive search.
     */
    private void findInScriptArea(String findText, boolean forward, boolean caseSensitive) {
        if (findText == null || findText.isEmpty()) {
            return;
        }

        String content = scriptTextArea.getText();
        if (!caseSensitive) {
            content = content.toLowerCase();
            findText = findText.toLowerCase();
        }

        int foundIndex;
        if (forward) {
            int fromIndex = scriptTextArea.getCaretPosition();
            foundIndex = content.indexOf(findText, fromIndex);
            if (foundIndex == -1) { // Wrap around
                foundIndex = content.indexOf(findText, 0);
            }
        } else { // Backward
            int fromIndex = scriptTextArea.getCaretPosition() - findText.length() - 1;
            foundIndex = content.lastIndexOf(findText, fromIndex);
            if (foundIndex == -1) { // Wrap around
                foundIndex = content.lastIndexOf(findText);
            }
        }

        if (foundIndex != -1) {
            scriptTextArea.requestFocusInWindow();
            scriptTextArea.select(foundIndex, foundIndex + findText.length());
        } else {
            java.awt.Toolkit.getDefaultToolkit().beep(); // Beep if not found
        }
    }

    /**
     * Populates the input file explorer tree with the contents of the currently
     * selected input directory.
     */
    private void populateInputFileExplorer() {
        if (inputDirectory == null || inputFileExplorerTree == null) {
            javax.swing.tree.DefaultTreeModel model = (javax.swing.tree.DefaultTreeModel) inputFileExplorerTree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            model.reload();
            return;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(inputDirectory.getFileName().toString());
        addInputNodes(root, inputDirectory.toFile());

        javax.swing.tree.DefaultTreeModel model = new javax.swing.tree.DefaultTreeModel(root);
        inputFileExplorerTree.setModel(model);
    }

    /**
     * Recursively adds nodes to the input file explorer tree, representing the
     * directory structure and files within a given folder.
     *
     * @param parentNode The parent {@link DefaultMutableTreeNode} to which new
     * file/folder nodes will be added.
     * @param parentFile The parent {@link File} whose contents will be listed.
     */
    private void addInputNodes(DefaultMutableTreeNode parentNode, File parentFile) {
        File[] files = parentFile.listFiles();
        if (files == null) {
            return;
        }

        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            }
            if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            }
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File file : files) {
            if (file.isDirectory()) {
                DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(file.toPath());
                parentNode.add(dirNode);
                addInputNodes(dirNode, file); // Recurse
            } else {
                // Add all files, not just Python scripts
                parentNode.add(new DefaultMutableTreeNode(file.toPath()));
            }
        }
    }

    /**
     * Sets up custom key bindings for the script text area to handle
     * Python-like indentation on Enter and Tab presses.
     */
    private void setupScriptAreaIndentation() {
        InputMap inputMap = scriptTextArea.getInputMap();
        ActionMap actionMap = scriptTextArea.getActionMap();

        // --- Handle Enter Key for Auto-Indentation ---
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "customEnter");
        actionMap.put("customEnter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int caretPos = scriptTextArea.getCaretPosition();
                    int lineNum = scriptTextArea.getLineOfOffset(caretPos);
                    int lineStartOffset = scriptTextArea.getLineStartOffset(lineNum);

                    String lineText = scriptTextArea.getText(lineStartOffset, caretPos - lineStartOffset);

                    // Calculate current indentation
                    StringBuilder indent = new StringBuilder();
                    for (char c : lineText.toCharArray()) {
                        if (c == ' ' || c == '\t') {
                            indent.append(c);
                        } else {
                            break;
                        }
                    }

                    // If the line being broken ends with a colon, add an extra indent level
                    if (lineText.trim().endsWith(":")) {
                        indent.append(" ".repeat(currentTabSize));
                    }

                    scriptTextArea.insert("\n" + indent, caretPos);

                } catch (javax.swing.text.BadLocationException ex) {
                    // Fallback to default behavior on error
                    scriptTextArea.insert("\n", scriptTextArea.getCaretPosition());
                }
            }
        });

        // --- Handle Tab Key to Insert 4 Spaces ---
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "customTab");
        actionMap.put("customTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scriptTextArea.replaceSelection(" ".repeat(currentTabSize));
            }
        });

        // --- Handle Shift+Tab to De-indent ---
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "customShiftTab");
        actionMap.put("customShiftTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int lineNum = scriptTextArea.getLineOfOffset(scriptTextArea.getCaretPosition());
                    int lineStart = scriptTextArea.getLineStartOffset(lineNum);
                    int len = Math.min(currentTabSize, scriptTextArea.getDocument().getLength() - lineStart);
                    String line = scriptTextArea.getText(lineStart, len);
                    if (line.equals(" ".repeat(len))) {
                        scriptTextArea.replaceRange("", lineStart, lineStart + len);
                    }
                } catch (javax.swing.text.BadLocationException ex) {
                    /* Ignore */ }
            }
        });
    }

    /**
     * A custom painter for drawing a squiggly red underline for errors.
     */
    private static class SquigglyUnderlinePainter extends javax.swing.text.LayeredHighlighter.LayerPainter {

        private static final int WAVE_HEIGHT = 2;
        private static final int WAVE_LENGTH = 4;

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, javax.swing.text.JTextComponent c, View view) {
            g.setColor(Color.RED);
            // This method is called by the highlighter to draw the squiggly line.
            // It calculates the screen coordinates for the start and end of the highlight
            // and then calls drawWave to do the actual drawing.

            try {
                Rectangle r0 = c.modelToView(offs0);
                Rectangle r1 = c.modelToView(offs1);
                int y = r0.y + r0.height - WAVE_HEIGHT;
                int startX = r0.x;
                int endX = (r0.y == r1.y) ? r1.x : c.getWidth(); // Underline to end of line if multi-line

                drawWave(g, startX, endX, y);

            } catch (javax.swing.text.BadLocationException e) {
                // Cannot render, do nothing
            }
            return bounds;
        }

        /**
         * Draws a wavy (squiggly) line.
         *
         * @param g The Graphics context.
         * @param x The starting x-coordinate.
         * @param endX The ending x-coordinate.
         * @param y The y-coordinate for the wave.
         */
        private void drawWave(Graphics g, int x, int endX, int y) {
            for (int i = x; i < endX; i += WAVE_LENGTH) {
                g.drawLine(i, y + WAVE_HEIGHT, i + WAVE_LENGTH / 2, y);
                g.drawLine(i + WAVE_LENGTH / 2, y, i + WAVE_LENGTH, y + WAVE_HEIGHT);
            }
        }

        @Override
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            // This method is required by the HighlightPainter interface.
            // The actual rendering is done in paintLayer, so this can be left empty.
        }

    }

    /**
     * Parses a Python error message to find a line number and then calls to
     * highlight that line in the script editor.
     *
     * @param errorLine The line of text from the standard error stream.
     */
    private void parseAndHighlightError(String errorLine) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(", line (\\d+)").matcher(errorLine);
        if (matcher.find()) {
            try {
                int lineNumber = Integer.parseInt(matcher.group(1));
                highlightErrorLine(lineNumber);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Adds a squiggly red underline highlight to a specific line in the script
     * text area.
     *
     * @param lineNumber The 1-based line number to highlight.
     */
    private void highlightErrorLine(int lineNumber) {
        try {
            int line = lineNumber - 1; // Convert to 0-based index
            int startIndex = scriptTextArea.getLineStartOffset(line);
            int endIndex = scriptTextArea.getLineEndOffset(line);
            scriptTextArea.getHighlighter().addHighlight(startIndex, endIndex, new SquigglyUnderlinePainter());
        } catch (javax.swing.text.BadLocationException ignored) {
        }
    }

    /**
     * Removes all squiggly red error highlights from the script text area.
     */
    private void clearErrorHighlights() {
        javax.swing.text.Highlighter highlighter = scriptTextArea.getHighlighter();
        for (javax.swing.text.Highlighter.Highlight h : highlighter.getHighlights()) {
            if (h.getPainter() instanceof SquigglyUnderlinePainter) {
                highlighter.removeHighlight(h);
            }
        }
    }

    /**
     * Performs an undo action on the script text area.
     */
    private void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    /**
     * Performs a redo action on the script text area.
     */
    private void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }

    /**
     * Adds a folder to the list of recently used folders.
     *
     * @param folder The {@link Path} of the folder to add.
     */
    private void addRecentFolder(Path folder) {
        if (folder == null) {
            return;
        }

        // Remove if it already exists to move it to the top
        recentFolders.remove(folder);
        // Add to the front of the list
        recentFolders.addFirst(folder);
        // Trim the list if it's too long
        while (recentFolders.size() > MAX_RECENT_FOLDERS) {
            recentFolders.removeLast();
        }
    }

    /**
     * Saves the current folder paths and selected script to user preferences.
     * This is called when the application window is closing.
     */
    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(PythonExecutor.class);

        if (scriptDirectory != null) {
            prefs.put(PREF_SCRIPT_DIR, scriptDirectory.toAbsolutePath().toString());
        } else {
            prefs.remove(PREF_SCRIPT_DIR);
        }

        if (inputDirectory != null) {
            prefs.put(PREF_INPUT_DIR, inputDirectory.toAbsolutePath().toString());
        } else {
            prefs.remove(PREF_INPUT_DIR);
        }

        Object selectedScript = scriptFileCombo.getSelectedItem();
        if (selectedScript instanceof Path) {
            prefs.put(PREF_LAST_SCRIPT, ((Path) selectedScript).getFileName().toString());
        } else {
            prefs.remove(PREF_LAST_SCRIPT);
        }

        // Save recent folders
        String recentFoldersString = recentFolders.stream()
                .map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator));
        prefs.put(PREF_RECENT_FOLDERS, recentFoldersString);

        // Save the file explorer mode
        prefs.putBoolean(PREF_EXPLORER_MODE, isFileExplorerMode);
    }

    /**
     * Loads folder paths and the last selected script from user preferences.
     * This is called on application startup.
     */
    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(PythonExecutor.class);

        // Load recent folders
        String recentFoldersString = prefs.get(PREF_RECENT_FOLDERS, "");
        if (!recentFoldersString.isEmpty()) {
            Arrays.stream(recentFoldersString.split(File.pathSeparator))
                    .map(Path::of)
                    .filter(Files::isDirectory)
                    .forEach(recentFolders::add);
        }

        String scriptDirPath = prefs.get(PREF_SCRIPT_DIR, null);
        if (scriptDirPath != null) {
            Path path = Path.of(scriptDirPath);
            if (Files.isDirectory(path)) {
                scriptDirectory = path;
                scriptFolderPathLabel.setText(scriptDirectory.toString());
                loadPythonScripts(); // This populates the combo box
            }
        }

        String inputDirPath = prefs.get(PREF_INPUT_DIR, null);
        if (inputDirPath != null) {
            Path path = Path.of(inputDirPath);
            if (Files.isDirectory(path)) {
                inputDirectory = path;
                inputFolderPathLabel.setText(inputDirectory.toString());
                loadInputFiles();
            }
        }

        // This must be done AFTER loadPythonScripts() has populated the combo box
        String lastScriptFileName = prefs.get(PREF_LAST_SCRIPT, null);
        if (lastScriptFileName != null && scriptDirectory != null) {
            for (int i = 0; i < scriptFileCombo.getItemCount(); i++) {
                Object item = scriptFileCombo.getItemAt(i);
                if (item instanceof Path) {
                    Path scriptPath = (Path) item;
                    if (scriptPath.getFileName().toString().equals(lastScriptFileName)) {
                        // Setting the selected item will trigger the action listener,
                        // which in turn calls loadSelectedScript().
                        scriptFileCombo.setSelectedItem(scriptPath);
                        break;
                    }
                }
            }
        }

        // Load and apply the file explorer mode preference
        boolean startInExplorerMode = prefs.getBoolean(PREF_EXPLORER_MODE, false); // Default to classic mode
        if (startInExplorerMode) {
            setFileExplorerMode(true);
        }
    }

    /**
     * A container for the loading screen components to allow them to be updated
     * from the main method.
     *
     * @param window The JWindow of the loading screen.
     * @param progressBar The JProgressBar to be updated.
     * @param statusLabel The JLabel to show status messages.
     */
    private record LoadingScreen(JWindow window, JProgressBar progressBar, JLabel statusLabel) {

    }

    /**
     * Creates and configures the loading window that appears on startup.
     *
     * @return A LoadingScreen object containing the window and its updatable
     * components.
     */
    private static LoadingScreen createLoadingWindow() {
        JWindow window = new JWindow();
        // Make the window transparent to see the rounded panel
        window.setBackground(new Color(0, 0, 0, 0));

        // Use a RoundedPanel for a modern look
        RoundedPanel contentPanel = new RoundedPanel(15, new Color(45, 45, 45));
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30)); // Padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;

        // --- Application Logo ---
        try {
            java.net.URL iconURL = PythonExecutor.class.getResource("/logo.png");
            if (iconURL != null) {
                ImageIcon logoIcon = new ImageIcon(iconURL);
                // Scale it down a bit for the splash screen
                Image image = logoIcon.getImage();
                Image scaledImage = image.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                gbc.insets = new Insets(5, 5, 10, 5); // More space below logo
                contentPanel.add(logoLabel, gbc);
            }
        } catch (Exception e) {
            System.err.println("Splash screen logo not found, skipping.");
        }

        // --- Main Title ---
        JLabel titleLabel = new JLabel("Python Executor");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(220, 220, 220));
        gbc.insets = new Insets(5, 5, 5, 5);
        contentPanel.add(titleLabel, gbc);

        // --- Status Text ---
        JLabel statusLabel = new JLabel("Initializing...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(180, 180, 180));
        gbc.insets = new Insets(15, 5, 5, 5); // Add more space above the progress bar
        contentPanel.add(statusLabel, gbc);

        // --- Progress Bar ---
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(false); // Make it a determinate progress bar
        progressBar.setStringPainted(false); // Do not show percentage on the bar itself
        progressBar.setPreferredSize(new Dimension(350, 5)); // Make it slim
        progressBar.setForeground(new Color(16, 185, 129)); // A nice, modern green
        progressBar.setBackground(new Color(60, 60, 60));
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));
        gbc.insets = new Insets(5, 5, 5, 5);
        contentPanel.add(progressBar, gbc);

        window.add(contentPanel);
        window.pack();
        window.setLocationRelativeTo(null); // Center on screen
        return new LoadingScreen(window, progressBar, statusLabel);
    }

    /**
     * A container for the results of the startup system checks.
     */
    private static class SystemChecksResult {

        boolean javaVersionOk = false;
        boolean tempDirOk = false;
        boolean pythonExists = false;
        boolean gitExists = false;
        boolean prefsOk = false;
        String osName;
        String javaVersion;
    }

    /**
     * Checks if the application has write access to the system's temporary
     * directory.
     *
     * @return {@code true} if a temporary file can be created and deleted,
     * {@code false} otherwise.
     */
    private static boolean checkTempDirAccess() {
        try {
            Path tempFile = Files.createTempFile("pyexec_check", ".tmp");
            Files.delete(tempFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the application can read from and write to the Java Preferences
     * store.
     *
     * @return {@code true} if preferences can be accessed, {@code false}
     * otherwise.
     */
    private static boolean checkPreferencesAccess() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(PythonExecutor.class);
            prefs.put("pref_check", "test");
            prefs.flush();
            prefs.remove("pref_check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the `python` command is available on the system's PATH.
     *
     * @return {@code true} if Python is found, {@code false} otherwise.
     */
    private static boolean isPythonInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            // IOException (e.g., file not found) means python is not in PATH
            return false;
        }
    }

    /**
     * Checks if the `git` command is available on the system's PATH.
     *
     * @return {@code true} if Git is found, {@code false} otherwise.
     */
    private static boolean isGitInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "--version");
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * The main entry point for the application. It shows a loading screen,
     * performs system checks, and then launches the main application window.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Create the loading screen components and make the window visible
        LoadingScreen loadingScreen = createLoadingWindow();
        SwingUtilities.invokeLater(() -> loadingScreen.window.setVisible(true));

        // Use a SwingWorker to perform startup checks in the background
        new SwingWorker<SystemChecksResult, Object[]>() {
            private Timer animationTimer;
            private String currentStatusText = "Initializing...";

            @Override
            protected SystemChecksResult doInBackground() throws InterruptedException {
                SystemChecksResult results = new SystemChecksResult();
                final int stepDelay = 500; // ms

                // Check 1: OS Info
                publish(new Object[]{"Detecting operating system...", 10});
                results.osName = System.getProperty("os.name");
                Thread.sleep(stepDelay);

                // Check 2: Java Version
                publish(new Object[]{"Verifying Java environment...", 25});
                results.javaVersion = System.getProperty("java.version");
                // This application uses features from Java 11+
                results.javaVersionOk = !results.javaVersion.startsWith("1."); // Simple check for modern versions
                Thread.sleep(stepDelay);

                // Check 3: Temp Directory Access
                publish(new Object[]{"Checking file permissions...", 40});
                results.tempDirOk = checkTempDirAccess();
                Thread.sleep(stepDelay);

                // Check 4: Preferences Access
                publish(new Object[]{"Verifying preferences store...", 55});
                results.prefsOk = checkPreferencesAccess();
                Thread.sleep(stepDelay);

                // Check 5: Python
                publish(new Object[]{"Checking for Python installation...", 70});
                results.pythonExists = isPythonInstalled();
                Thread.sleep(stepDelay);

                // Check 6: Git
                publish(new Object[]{"Checking for Git installation...", 85});
                results.gitExists = isGitInstalled();
                Thread.sleep(stepDelay);

                publish(new Object[]{"Launching application...", 100});
                Thread.sleep(stepDelay);

                return results;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                // Update UI on the EDT with progress from publish()
                if (!chunks.isEmpty()) {
                    Object[] latest = chunks.get(chunks.size() - 1); // Get the most recent update
                    currentStatusText = (String) latest[0];
                    int targetProgress = (Integer) latest[1];

                    if (animationTimer != null && animationTimer.isRunning()) {
                        animationTimer.stop();
                    }

                    // Animate the progress bar from its current value to the target value
                    animationTimer = new Timer(15, e -> { // 15ms delay for a smooth slide
                        int currentValue = loadingScreen.progressBar.getValue();
                        if (currentValue < targetProgress) {
                            int nextValue = currentValue + 1;
                            loadingScreen.progressBar.setValue(nextValue);
                            loadingScreen.statusLabel.setText(String.format("%s   %d%%", currentStatusText, nextValue));
                        } else {
                            ((Timer) e.getSource()).stop();
                        }
                    });
                    animationTimer.start();
                }
            }

            @Override
            protected void done() {
                try {
                    SystemChecksResult results = get();
                    if (!results.javaVersionOk) {
                        JOptionPane.showMessageDialog(null,
                                "You are using an old version of Java (" + results.javaVersion + ").\n"
                                + "This application requires Java 11 or newer to function correctly.",
                                "Unsupported Java Version", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }

                    if (!results.tempDirOk) {
                        JOptionPane.showMessageDialog(null,
                                "The application could not write to the system's temporary directory.\n"
                                + "Please check your user permissions. The application cannot run without this.",
                                "Permission Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }

                    if (!results.prefsOk) {
                        JOptionPane.showMessageDialog(null,
                                "Could not access user preferences. Settings will not be saved.",
                                "Preferences Warning", JOptionPane.WARNING_MESSAGE);
                    }
                    // Create the main application frame.
                    // This is a heavy operation, but the loading screen is still visible,
                    // providing a better user experience than a blank screen.
                    PythonExecutor app = new PythonExecutor(results.pythonExists);

                    // Now that the app is fully constructed, dispose the loading screen...
                    loadingScreen.window.dispose();

                    // ...and immediately make the main app visible.
                    SwingUtilities.invokeLater(() -> {
                        app.setVisible(true);
                        if (!results.gitExists) {
                            JOptionPane.showMessageDialog(null,
                                    "Git is not installed or not found in your system's PATH.\n"
                                    + "Version control features will be unavailable.",
                                    "Git Not Found", JOptionPane.WARNING_MESSAGE);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }.execute();
    }
}
