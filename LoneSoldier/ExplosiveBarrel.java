import java.awt.*;
import java.util.List;

public class ExplosiveBarrel {
    public float x, y;
    public static final int SIZE = 30;
    public int hp = 3;
    public boolean exploded = false;
    public int explodeTimer = 0;
    public int flash = 0;

    public ExplosiveBarrel(float x, float y) {
        this.x = x; this.y = y;
    }

    public void takeDamage(int dmg) {
        hp -= dmg;
        flash = 4;
        if (hp <= 0 && !exploded) {
            exploded = true;
            explodeTimer = 6;
        }
    }

    public Rectangle getBounds() { return new Rectangle((int)x, (int)y, SIZE, SIZE); }

    public void draw(Graphics2D g) {
        if (exploded && explodeTimer <= 0) return;
        int ex = (int)x, ey = (int)y;
        // Body
        g.setColor(new Color(140, 70, 25));
        g.fillRoundRect(ex, ey, SIZE, SIZE, 6, 6);
        // Rims
        g.setColor(new Color(90, 45, 15));
        g.fillRect(ex, ey+5, SIZE, 5);
        g.fillRect(ex, ey+SIZE-10, SIZE, 5);
        // Warning stripe
        g.setColor(new Color(220, 180, 30));
        g.fillRect(ex+8, ey+SIZE/2-3, SIZE-16, 6);
        // Symbol
        g.setColor(Color.BLACK);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.drawString("!", ex+11, ey+20);

        if (flash > 0) {
            flash--;
            g.setColor(new Color(255, 200, 100, 120));
            g.fillRoundRect(ex, ey, SIZE, SIZE, 6, 6);
        }

        if (exploded && explodeTimer > 0) {
            float expand = (6 - explodeTimer) / 6f;
            int r = (int)(expand * 140);
            g.setColor(new Color(255, 100, 20, (int)(180 * (1-expand))));
            g.fillOval(ex + SIZE/2 - r, ey + SIZE/2 - r, r*2, r*2);
        }
    }
}