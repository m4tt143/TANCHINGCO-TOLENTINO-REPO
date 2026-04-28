import javax.swing.SwingUtilities;
import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setResizable(true);
            frame.setExtendedState(JFrame.NORMAL);
        });
    }
}