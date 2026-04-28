import java.awt.*;

/**
 * Bullet - Projectile fired by the player toward the nearest enemy.
 */
public class Bullet {

    public float x, y;
    float dx, dy;     // Velocity components (package-private for knockback)
    public int damage;

    private static final float SPEED = 10f;
    private static final int   SIZE  = 8;

    public Bullet(float x, float y, float dirX, float dirY, int damage) {
        this.x      = x;
        this.y      = y;
        this.dx     = dirX * SPEED;
        this.dy     = dirY * SPEED;
        this.damage = damage;
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public boolean isOffScreen() {
        return x < -30 || x > GameFrame.WIDTH  + 30
            || y < -30 || y > GameFrame.HEIGHT + 30;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }

    public void draw(Graphics2D g) {
        // ── Bullet Trail ──
        int tx = (int)(x - dx * 1.2f);
        int ty = (int)(y - dy * 1.2f);
        GradientPaint trail = new GradientPaint(x, y, new Color(255, 235, 50, 160),
                                                tx, ty, new Color(255, 200, 50, 0));
        g.setPaint(trail);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine((int)x, (int)y, tx, ty);
        g.setStroke(new BasicStroke(1f));

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