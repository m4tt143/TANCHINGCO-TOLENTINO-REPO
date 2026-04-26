import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class JavaApplication30 {

    // UI Components
    private JFrame frame;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JComboBox<String> courseBox;
    private JComboBox<String> yearBox;
    private JButton signBtn;
    
    // Global variable to store the signature image after the popup closes
    private BufferedImage capturedSignature = null;

    // Design Colors
    private final Color COLOR_PRIMARY = new Color(51, 122, 183);
    private final Color COLOR_SUCCESS = new Color(40, 167, 69);
    private final Color COLOR_BG      = new Color(245, 245, 245);

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(JavaApplication30::new);
    }

    public JavaApplication30() {
        initializeFrame();
        createComponents();
        frame.setVisible(true);
    }

    private void initializeFrame() {
        frame = new JFrame("Attendance Tracker");
        frame.setSize(400, 300); // Fixed size as requested
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false); // Keeps the 400x300 layout consistent
    }

    private void createComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4); // Spacing between components
        gbc.weightx = 1.0;

        // --- SECTION: Header ---
        JLabel title = new JLabel("STUDENT ATTENDANCE", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(COLOR_PRIMARY);
        gbc.gridwidth = 2; gbc.gridy = 0; gbc.gridx = 0;
        mainPanel.add(title, gbc);

        // --- SECTION: Name Input ---
        gbc.gridwidth = 1; gbc.gridy = 1;
        firstNameField = new JTextField();
        firstNameField.setBorder(BorderFactory.createTitledBorder("First Name"));
        mainPanel.add(firstNameField, gbc);

        gbc.gridx = 1;
        lastNameField = new JTextField();
        lastNameField.setBorder(BorderFactory.createTitledBorder("Last Name"));
        mainPanel.add(lastNameField, gbc);

        // --- SECTION: Dropdowns ---
        gbc.gridx = 0; gbc.gridy = 2;
        courseBox = new JComboBox<>(new String[]{"BSIT", "BSCS", "BSCpE", "BSBA"});
        courseBox.setBorder(BorderFactory.createTitledBorder("Course"));
        mainPanel.add(courseBox, gbc);

        gbc.gridx = 1;
        yearBox = new JComboBox<>(new String[]{"1st Yr", "2nd Yr", "3rd Yr", "4th Yr"});
        yearBox.setBorder(BorderFactory.createTitledBorder("Year"));
        mainPanel.add(yearBox, gbc);

        // --- SECTION: Signature Trigger ---
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        signBtn = new JButton("Open Signature Pad");
        signBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signBtn.addActionListener(e -> openSignaturePopup());
        mainPanel.add(signBtn, gbc);

        // --- SECTION: Submit Button ---
        gbc.gridy = 4;
        JButton submitBtn = new JButton("SUBMIT ATTENDANCE");
        submitBtn.setBackground(COLOR_SUCCESS);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        submitBtn.addActionListener(e -> handleSubmit());
        mainPanel.add(submitBtn, gbc);

        frame.add(mainPanel);
    }

    /**
     * Creates and displays the Signature Popup Window (JDialog)
     */
    private void openSignaturePopup() {
        JDialog dialog = new JDialog(frame, "Digital Signature", true); // 'true' makes it modal
        dialog.setSize(400, 250);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(frame);

        SignaturePanel pad = new SignaturePanel();
        dialog.add(new JLabel(" Sign below using your mouse:", SwingConstants.CENTER), BorderLayout.NORTH);
        dialog.add(pad, BorderLayout.CENTER);

        // Popup Buttons
        JPanel southPanel = new JPanel();
        JButton saveBtn = new JButton("Save & Close");
        JButton clearBtn = new JButton("Clear");

        saveBtn.addActionListener(e -> {
            if (!pad.isBlank()) {
                capturedSignature = pad.getSignatureImage();
                signBtn.setText("✓ Signature Captured");
                signBtn.setForeground(COLOR_SUCCESS);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please provide a signature.");
            }
        });

        clearBtn.addActionListener(e -> pad.clear());

        southPanel.add(clearBtn);
        southPanel.add(saveBtn);
        dialog.add(southPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * Handles the validation and submission logic
     */
    private void handleSubmit() {
        if (firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter your full name.");
            return;
        }
        if (capturedSignature == null) {
            JOptionPane.showMessageDialog(frame, "Signature is required.");
            return;
        }

        // Logic for successful submission
        JOptionPane.showMessageDialog(frame, "Attendance recorded for " + firstNameField.getText());
        
        // Reset the form
        firstNameField.setText("");
        lastNameField.setText("");
        capturedSignature = null;
        signBtn.setText("Open Signature Pad");
        signBtn.setForeground(Color.BLACK);
    }

    // --- INNER CLASS: The actual drawing surface for the signature ---
    class SignaturePanel extends JPanel {
        private BufferedImage image;
        private Graphics2D g2;
        private int prevX, prevY;
        private boolean isBlank = true;

        public SignaturePanel() {
            setBackground(Color.WHITE);
            setBorder(new LineBorder(Color.GRAY, 1));
            
            // Mouse Listener to start the line
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    prevX = e.getX();
                    prevY = e.getY();
                    isBlank = false;
                }
            });

            // Mouse Motion Listener to draw the line
            addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (g2 == null) initImage();
                    g2.drawLine(prevX, prevY, e.getX(), e.getY());
                    repaint();
                    prevX = e.getX();
                    prevY = e.getY();
                }
            });
        }

        private void initImage() {
            image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            g2 = image.createGraphics();
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) g.drawImage(image, 0, 0, null);
        }

        public void clear() {
            image = null;
            g2 = null;
            isBlank = true;
            repaint();
        }

        public boolean isBlank() { return isBlank; }
        public BufferedImage getSignatureImage() { return image; }
    }
}