import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final String TITLE = "LAST OUTPOST: Last Stand";

    public GameFrame() {

        GamePanel gamePanel = new GamePanel();

        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setLayout(new BorderLayout());
        this.add(gamePanel, BorderLayout.CENTER);

        this.setSize(WIDTH, HEIGHT);
        this.setLocationRelativeTo(null);

        this.setResizable(true);

        this.setVisible(true);
    }
}