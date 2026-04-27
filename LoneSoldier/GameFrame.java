import javax.swing.*;

/**
 * GameFrame - Sets up the game window (JFrame)
 */
public class GameFrame extends JFrame {

    public static final int WIDTH  = 800;
    public static final int HEIGHT = 600;
    public static final String TITLE = "LAST OUTPOST: Last Stand";

    public GameFrame() {
        GamePanel gamePanel = new GamePanel();

        this.add(gamePanel);
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.pack();
        this.setLocationRelativeTo(null); // Center on screen
        this.setVisible(true);

        gamePanel.requestFocusInWindow();
    }
}
