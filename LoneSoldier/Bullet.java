import java.awt.*;

/**
 * Bullet - Projectile fired by the player toward the nearest enemy.
 */
public class Bullet {

    public float x, y;
    private float dx, dy;     // Velocity components
    public int damage;

    private static final float SPEED = 10f;
    private static final int   SIZE  = 8;

    /**
     * @param x      Starting X (player center)
     * @param y      Starting Y (player center)
     * @param dirX   Normalized direction X
     * @param dirY   Normalized direction Y
     * @param damage Damage dealt on hit
     */
    public Bullet(float x, float y, float dirX, float dirY, int damage) {
        this.x      = x;
        this.y      = y;
        this.dx     = dirX * SPEED;
        this.dy     = dirY * SPEED;
        this.damage = damage;
    }

    /** Move the bullet forward each tick. */
    public void update() {
        x += dx;
        y += dy;
    }

    /** Returns true if bullet has left the screen. */
    public boolean isOffScreen() {
        return x < -30 || x > GameFrame.WIDTH  + 30
            || y < -30 || y > GameFrame.HEIGHT + 30;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }

    /** Draw bullet as a glowing yellow pellet. */
    public void draw(Graphics2D g) {
        // Glow effect
        g.setColor(new Color(255, 200, 50, 100));
        g.fillOval((int) x - SIZE, (int) y - SIZE, SIZE * 2, SIZE * 2);

        // Core
        g.setColor(new Color(255, 235, 50));
        g.fillOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);

        // Bright center
        g.setColor(Color.WHITE);
        g.fillOval((int) x - 2, (int) y - 2, 4, 4);
    }
}
