package executor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class StudentRegistrationForm extends JFrame {

    // Color Palette based on the image
    private static final Color BG_COLOR = Color.decode("#0f172a");       // Dark background
    private static final Color FIELD_BG_COLOR = Color.decode("#1e293b"); // Input field background
    private static final Color ACCENT_COLOR = Color.decode("#2563eb");   // Blue button
    private static final Color TEXT_COLOR = Color.decode("#f8fafc");     // White text
    private static final Color LABEL_COLOR = Color.decode("#cbd5e1");    // Light gray labels
    private static final Color PLACEHOLDER_COLOR = Color.decode("#64748b"); // Darker gray for placeholders

    public StudentRegistrationForm() {
        setTitle("User Information");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 750);
        setLocationRelativeTo(null); // Center on screen
        
        // Main Panel with dark background
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0); // Bottom padding
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // --- 1. Logo (Drawn programmatically) ---
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new LogoPanel(), gbc);

        // --- 2. Title ---
        gbc.gridy++;
        JLabel titleLabel = new JLabel("User Information");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, gbc);

        // --- 3. Subtitle ---
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 30, 0); // More space after subtitle
        JLabel subtitleLabel = new JLabel("Please fill out the form to proceed to the coding exam.");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(LABEL_COLOR);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(subtitleLabel, gbc);

        // Reset alignment for form fields
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 15, 0); // Gap between label and field is small, gap after field is larger

        // --- 4. Form Fields ---
        
        // Name
        gbc.gridy++;
        addLabel(mainPanel, "Name", gbc);
        gbc.gridy++;
        mainPanel.add(new CustomTextField("Enter your full name"), gbc);

        // PRN
        gbc.gridy++;
        addLabel(mainPanel, "PRN", gbc);
        gbc.gridy++;
        mainPanel.add(new CustomTextField("Enter your Permanent Registration Number"), gbc);

        // College Email
        gbc.gridy++;
        addLabel(mainPanel, "College Email", gbc);
        gbc.gridy++;
        mainPanel.add(new CustomTextField("you@example.com"), gbc);

        // Class and Division (Split Row)
        gbc.gridy++;
        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 20, 0)); // 20px gap between cols
        splitPanel.setBackground(BG_COLOR);
        
        // Left side (Class)
        JPanel classPanel = new JPanel(new BorderLayout());
        classPanel.setBackground(BG_COLOR);
        JLabel classLabel = new JLabel("Class");
        classLabel.setForeground(TEXT_COLOR);
        classLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        classPanel.add(classLabel, BorderLayout.NORTH);
        JPanel classFieldWrapper = new JPanel(new BorderLayout()); // Wrapper for margin
        classFieldWrapper.setBackground(BG_COLOR);
        classFieldWrapper.setBorder(new EmptyBorder(5,0,0,0));
        classFieldWrapper.add(new CustomTextField("e.g. TE"));
        classPanel.add(classFieldWrapper, BorderLayout.CENTER);

        // Right side (Division)
        JPanel divPanel = new JPanel(new BorderLayout());
        divPanel.setBackground(BG_COLOR);
        JLabel divLabel = new JLabel("Division");
        divLabel.setForeground(TEXT_COLOR);
        divLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        divPanel.add(divLabel, BorderLayout.NORTH);
        JPanel divFieldWrapper = new JPanel(new BorderLayout());
        divFieldWrapper.setBackground(BG_COLOR);
        divFieldWrapper.setBorder(new EmptyBorder(5,0,0,0));
        divFieldWrapper.add(new CustomTextField("e.g. A"));
        divPanel.add(divFieldWrapper, BorderLayout.CENTER);

        splitPanel.add(classPanel);
        splitPanel.add(divPanel);
        mainPanel.add(splitPanel, gbc);

        // Contact
        gbc.gridy++;
        addLabel(mainPanel, "Contact", gbc);
        gbc.gridy++;
        mainPanel.add(new CustomTextField("Enter your contact number"), gbc);

        // --- 5. Submit Button ---
        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 0, 0); // More space before button
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JButton submitBtn = new CustomButton("Submit");
        mainPanel.add(submitBtn, gbc);
    }

    private void addLabel(JPanel panel, String text, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        // Temporary tweak to insets for label to sit closer to its field
        Insets oldInsets = gbc.insets;
        gbc.insets = new Insets(0, 0, 5, 0); 
        panel.add(label, gbc);
        // Restore insets
        gbc.insets = oldInsets;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new StudentRegistrationForm().setVisible(true);
        });
    }

    // --- Custom UI Components Helper Classes ---

    /**
     * Custom Text Field with Rounded Corners, Dark BG, and Placeholder support
     */
    class CustomTextField extends JTextField {
        private String placeholder;
        private Shape shape;

        public CustomTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false); // As we paint the background manually
            setForeground(TEXT_COLOR);
            setCaretColor(TEXT_COLOR);
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setBorder(new EmptyBorder(10, 15, 10, 15)); // Padding inside text field

            // Logic to handle placeholder painting
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }
                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Paint Background
            g2.setColor(FIELD_BG_COLOR);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

            // Paint Border (Optional, slightly lighter)
            g2.setColor(FIELD_BG_COLOR.brighter().brighter());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

            super.paintComponent(g);

            // Paint Placeholder if empty
            if (getText().isEmpty() && !hasFocus()) {
                g2.setColor(PLACEHOLDER_COLOR);
                g2.setFont(getFont());
                // Calculate vertical centering
                FontMetrics fm = g2.getFontMetrics();
                int x = getInsets().left;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, x, y);
            }
            g2.dispose();
        }
    }

    /**
     * Custom Button with Blue Background, Rounded Corners and Hover effect
     */
    class CustomButton extends JButton {
        private boolean isHovered = false;

        public CustomButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 15));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Add hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Change color on hover
            if (isHovered) {
                g2.setColor(ACCENT_COLOR.darker());
            } else {
                g2.setColor(ACCENT_COLOR);
            }

            // Draw rounded rectangle
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Draws the blue stacked logo icon programmatically
     */
    class LogoPanel extends JPanel {
        public LogoPanel() {
            setPreferredSize(new Dimension(50, 50));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(ACCENT_COLOR);
            
            // Draw three stacked ellipses to mimic the logo
            int width = 30;
            int height = 14;
            int x = (getWidth() - width) / 2;
            
            g2.fillOval(x, 5, width, height);
            g2.fillOval(x, 15, width, height);
            g2.fillOval(x, 25, width, height);
        }
    }
}