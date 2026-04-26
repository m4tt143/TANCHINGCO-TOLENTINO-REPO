import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class AttendanceTracker {

    public static void main(String[] args) {

        // Main window
        JFrame frame = new JFrame("Attendance Tracker");
        frame.setSize(400, 300); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        // Panel with padding
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new GridLayout(7, 2, 5, 5)); 

        // Labels
        JLabel nameLabel = new JLabel("Full Name:");
        JLabel idLabel = new JLabel("Student ID:");
        JLabel courseLabel = new JLabel("Course:");
        JLabel yearLabel = new JLabel("Year Level:");
        JLabel timeLabel = new JLabel("Time In:");
        JLabel signLabel = new JLabel("E-Signature:");

        // Fields
        JTextField nameField = new JTextField();
        JTextField idField = new JTextField();
        JTextField courseField = new JTextField();
        JTextField yearField = new JTextField();
        JTextField timeField = new JTextField();
        JTextField signField = new JTextField();

        // Auto time + signature
        timeField.setText(LocalDateTime.now().toString());
        timeField.setEditable(false);

        signField.setText(UUID.randomUUID().toString());
        signField.setEditable(false);

        // Submit button
        JButton submitButton = new JButton("Submit");

        submitButton.addActionListener(e -> {
            if (nameField.getText().isEmpty() ||
                idField.getText().isEmpty() ||
                courseField.getText().isEmpty() ||
                yearField.getText().isEmpty()) {

                JOptionPane.showMessageDialog(frame,
                        "Please fill in all fields!",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Attendance Recorded!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Add to panel (clean order)
        panel.add(nameLabel); panel.add(nameField);
        panel.add(idLabel); panel.add(idField);
        panel.add(courseLabel); panel.add(courseField);
        panel.add(yearLabel); panel.add(yearField);
        panel.add(timeLabel); panel.add(timeField);
        panel.add(signLabel); panel.add(signField);
        panel.add(new JLabel()); panel.add(submitButton);

        // Show UI
        frame.add(panel);
        frame.setVisible(true);
    }
}
