import javax.swing.*;

/**
 * GameFrame - Main Game Window
 */
public class GameFrame extends JFrame {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final String TITLE = "LAST OUTPOST: Last Stand";

    public GameFrame() {

        GamePanel gamePanel = new GamePanel();

        this.add(gamePanel);
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Allow resize / maximize
        this.setResizable(true);

        this.setSize(WIDTH, HEIGHT);

        // Open at center of screen
        this.setLocationRelativeTo(null);

        // Show minimize/maximize buttons
        this.setExtendedState(JFrame.NORMAL);

        this.setVisible(true);

        gamePanel.requestFocusInWindow();
    }
}