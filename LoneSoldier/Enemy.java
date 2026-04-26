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
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(ex + 2, ey + 2, SIZE, SIZE);

        // Body
        g.setColor(bodyColor);
        g.fillOval(ex, ey, SIZE, SIZE);

        // Helmet
        g.setColor(helmetColor);
        g.fillArc(ex + 3, ey, SIZE - 6, SIZE / 2 + 3, 0, 180);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(ex + 5,  ey + 11, 5, 5);
        g.fillOval(ex + 15, ey + 11, 5, 5);

        // Red angry eyes
        g.setColor(new Color(220, 0, 0));
        g.fillOval(ex + 6,  ey + 12, 3, 3);
        g.fillOval(ex + 16, ey + 12, 3, 3);

        // HP bar (visible only when damaged)
        if (hp < maxHp) {
            g.setColor(new Color(80, 0, 0));
            g.fillRect(ex, ey - 6, SIZE, 4);
            float ratio = (float) hp / maxHp;
            g.setColor(new Color(200, 40, 40));
            g.fillRect(ex, ey - 6, (int) (SIZE * ratio), 4);
        }
    }
}
