import java.awt.*;

public class EnemyProjectile {
    public float x, y, dx, dy;
    public static final int SIZE = 7;
    public int life = 200;
    public boolean fromBoss;

    public EnemyProjectile(float x, float y, float dx, float dy, boolean fromBoss) {
        this.x = x; this.y = y; this.dx = dx; this.dy = dy;
        this.fromBoss = fromBoss;
    }

    public void update() { x += dx; y += dy; life--; }
    public boolean isOffScreen() {
        return x < -30 || x > GameFrame.WIDTH+30 || y < -30 || y > GameFrame.HEIGHT+30 || life <= 0;
    }
    public Rectangle getBounds() { return new Rectangle((int)x-4, (int)y-4, 8, 8); }

    public void draw(Graphics2D g) {
        g.setColor(fromBoss ? new Color(255, 60, 60) : new Color(255, 120, 120));
        g.fillOval((int)x-4, (int)y-4, 8, 8);
        g.setColor(fromBoss ? new Color(255, 80, 80, 120) : new Color(255, 150, 150, 80));
        g.fillOval((int)x-8, (int)y-8, 16, 16);
        g.setColor(Color.WHITE);
        g.fillOval((int)x-2, (int)y-2, 4, 4);
    }
}