package executor;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * A non-modal search and replace bar for a JTextComponent.
 */
public class FindReplaceBar extends JPanel {

    private final JTextComponent textComponent;
    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox caseSensitiveCheckBox;

    public FindReplaceBar(JTextComponent textComponent) {
        this.textComponent = textComponent;

        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));

        // --- Components ---
        findField = new JTextField(20);
        replaceField = new JTextField(20);
        caseSensitiveCheckBox = new JCheckBox("Case Sensitive");

        JButton findNextBtn = new JButton("Find Next");
        JButton findPrevBtn = new JButton("Find Prev");
        JButton replaceBtn = new JButton("Replace");
        JButton closeBtn = new JButton("X");

        // --- Styling ---
        styleButton(findNextBtn);
        styleButton(findPrevBtn);
        styleButton(replaceBtn);
        styleButton(closeBtn);
        closeBtn.setForeground(Color.RED);

        // --- Layout ---
        add(new JLabel("Find:"));
        add(findField);
        add(findNextBtn);
        add(findPrevBtn);
        add(new JLabel("Replace:"));
        add(replaceField);
        add(replaceBtn);
        add(caseSensitiveCheckBox);
        add(closeBtn);

        // --- Listeners ---
        findNextBtn.addActionListener(e -> find(true));
        findPrevBtn.addActionListener(e -> find(false));
        replaceBtn.addActionListener(e -> replace());
        closeBtn.addActionListener(e -> setVisible(false));

        // Add key listener to find field for "Enter" to find next
        findField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    find(true);
                }
            }
        });

        setVisible(false); // Initially hidden
    }

    private void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setMargin(new java.awt.Insets(2, 5, 2, 5));
    }

    /**
     * Makes the find bar visible and requests focus for the find field.
     */
    public void activate() {
        setVisible(true);
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    /**
     * Finds the next or previous occurrence of the text in the find field.
     *
     * @param forward true to search forward, false to search backward.
     */
    private void find(boolean forward) {
        String findText = findField.getText();
        if (findText.isEmpty()) {
            return;
        }

        String content = textComponent.getText();
        boolean caseSensitive = caseSensitiveCheckBox.isSelected();

        if (!caseSensitive) {
            findText = findText.toLowerCase();
            content = content.toLowerCase();
        }

        int foundIndex;
        if (forward) {
            int fromIndex = textComponent.getCaretPosition();
            foundIndex = content.indexOf(findText, fromIndex);
            if (foundIndex == -1) { // Wrap around
                foundIndex = content.indexOf(findText, 0);
            }
        } else { // Backward search
            int fromIndex = textComponent.getCaretPosition() - findText.length() - 1;
            foundIndex = content.lastIndexOf(findText, fromIndex);
            if (foundIndex == -1) { // Wrap around
                foundIndex = content.lastIndexOf(findText);
            }
        }

        if (foundIndex != -1) {
            textComponent.requestFocusInWindow();
            textComponent.select(foundIndex, foundIndex + findText.length());
        } else {
            // Optionally, provide feedback that the text was not found
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Replaces the currently selected text with the text from the replace field.
     */
    private void replace() {
        String selectedText = textComponent.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            // If nothing is selected, just find the next occurrence
            find(true);
            return;
        }

        String findText = findField.getText();
        boolean caseSensitive = caseSensitiveCheckBox.isSelected();
        String comparisonFind = caseSensitive ? findText : findText.toLowerCase();
        String comparisonSelected = caseSensitive ? selectedText : selectedText.toLowerCase();

        if (comparisonSelected.equals(comparisonFind)) {
            textComponent.replaceSelection(replaceField.getText());
            // After replacing, automatically find the next one
            find(true);
        } else {
            // The selection doesn't match the find text, so just find the next one
            find(true);
        }
    }
}