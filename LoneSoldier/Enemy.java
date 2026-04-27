import java.awt.*;
import java.util.Random;

/**
 * Enemy - Red soldier that marches toward the player.
 * Gets tougher and faster each wave.
 */
public class Enemy {

    // Position & Size
    public float x, y;
    public static final int SIZE = 26;

    // Stats
    public int hp;
    public int maxHp;
    public float speed;

    // Visual variety
    private Color bodyColor;
    private Color helmetColor;

    private static final Random rand = new Random();

    public Enemy(float x, float y, int hp, float speed) {
        this.x    = x;
        this.y    = y;
        this.hp   = hp;
        this.maxHp = hp;
        this.speed = speed;

        // Slight red color variation so the crowd looks alive
        int r = 180 + rand.nextInt(55);
        int g = 30  + rand.nextInt(25);
        int b = 30  + rand.nextInt(25);
        this.bodyColor   = new Color(r, g, b);
        this.helmetColor = new Color(Math.max(0, r - 50), 10, 10);
    }

    /**
     * Move directly toward the player's position.
     * @param targetX  Player center X
     * @param targetY  Player center Y
     */
    public void update(float targetX, float targetY) {
        float dx   = targetX - getCenterX();
        float dy   = targetY - getCenterY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            x += (dx / dist) * speed;
            y += (dy / dist) * speed;
        }
    }

    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }

    public Rectangle getBounds() {
        return new Rectangle((int) x + 2, (int) y + 2, SIZE - 4, SIZE - 4);
    }

    /** Draw the enemy as a red soldier character. */
    public void draw(Graphics2D g) {
        int ex = (int) x;
        int ey = (int) y;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(ex + 1, ey + 20, SIZE - 2, 5);

        // Legs (dark red/maroon)
        g.setColor(new Color(140, 30, 30));
        g.fillRect(ex + 6, ey + 17, 4, 8);
        g.fillRect(ex + 16, ey + 17, 4, 8);

        // Body/Armor (blood red)
        g.setColor(bodyColor);
        g.fillRect(ex + 3, ey + 9, SIZE - 6, 11);

        // Chest plate (darker red)
        g.setColor(new Color(Math.max(0, bodyColor.getRed() - 40), 
                             Math.max(0, bodyColor.getGreen() - 20),
                             Math.max(0, bodyColor.getBlue() - 20)));
        g.fillRect(ex + 11, ey + 9, 4, 11);

        // Arms (reddish)
        g.setColor(new Color(180, 40, 40));
        g.fillRect(ex + 2, ey + 10, 2, 7);
        g.fillRect(ex + 22, ey + 10, 2, 7);

        // Helmet (dark with red tint)
        g.setColor(helmetColor);
        g.fillOval(ex + 4, ey + 1, SIZE - 8, 10);

        // Eyes (angry red glow)
        g.setColor(new Color(255, 150, 150));
        g.fillOval(ex + 6,  ey + 4, 3, 3);
        g.fillOval(ex + 17, ey + 4, 3, 3);

        // Angry pupils (dark)
        g.setColor(new Color(40, 0, 0));
        g.fillOval(ex + 7,  ey + 5, 2, 2);
        g.fillOval(ex + 18, ey + 5, 2, 2);

        // HP bar (visible only when damaged)
        if (hp < maxHp) {
            g.setColor(new Color(50, 0, 0));
            g.fillRect(ex, ey - 7, SIZE, 3);
            float ratio = (float) hp / maxHp;
            g.setColor(new Color(255, 100, 100));
            g.fillRect(ex, ey - 7, (int) (SIZE * ratio), 3);
        }
    }
}
