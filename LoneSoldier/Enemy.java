import java.awt.*;
import java.util.Random;

/**
 * Enemy - Marches toward the player.
 * Supports elite variant (larger, tougher, different color).
 */
public class Enemy {
    public float x, y;
    public static final int SIZE = 26;
    public int hp, maxHp;
    public float speed;
    public boolean elite;
    public int coinDrop; // coins dropped on death

    private Color bodyColor, helmetColor;
    private static final Random rand = new Random();

    public Enemy(float x, float y, int hp, float speed, boolean elite) {
        this.x = x; this.y = y; this.hp = hp; this.maxHp = hp;
        this.speed = speed; this.elite = elite;
        this.coinDrop = elite ? 3 + rand.nextInt(3) : 1;

        if (elite) {
            // Purple elite
            bodyColor   = new Color(120 + rand.nextInt(40), 30, 160 + rand.nextInt(40));
            helmetColor = new Color(80, 10, 110);
        } else {
            int r = 170 + rand.nextInt(55), gg = 20 + rand.nextInt(20), b = 20 + rand.nextInt(20);
            bodyColor   = new Color(r, gg, b);
            helmetColor = new Color(Math.max(0, r - 60), 8, 8);
        }
    }

    public void update(float tx, float ty) {
        float dx = tx - getCenterX(), dy = ty - getCenterY();
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist > 0) { x += (dx/dist)*speed; y += (dy/dist)*speed; }
    }

    public float getCenterX() { return x + getSize() / 2f; }
    public float getCenterY() { return y + getSize() / 2f; }
    public int   getSize()    { return elite ? SIZE + 10 : SIZE; }

    public Rectangle getBounds() {
        int s = getSize();
        return new Rectangle((int)x+2, (int)y+2, s-4, s-4);
    }

    public void draw(Graphics2D g) {
        int s = getSize(), ex = (int)x, ey = (int)y;
        // Shadow
        g.setColor(new Color(0,0,0,60)); g.fillOval(ex+2, ey+2, s, s);
        // Body
        g.setColor(bodyColor); g.fillOval(ex, ey, s, s);
        // Elite glow ring
        if (elite) {
            g.setColor(new Color(200, 80, 255, 80));
            g.setStroke(new BasicStroke(3));
            g.drawOval(ex-2, ey-2, s+4, s+4);
            g.setStroke(new BasicStroke(1));
        }
        // Helmet
        g.setColor(helmetColor); g.fillArc(ex+3, ey, s-6, s/2+3, 0, 180);
        // Eyes
        int eyeOff = elite ? 4 : 3;
        g.setColor(Color.WHITE);
        g.fillOval(ex+eyeOff, ey+s/2-1, 5, 5);
        g.fillOval(ex+s-eyeOff-5, ey+s/2-1, 5, 5);
        g.setColor(elite ? new Color(200,80,255) : new Color(220,0,0));
        g.fillOval(ex+eyeOff+1, ey+s/2, 3, 3);
        g.fillOval(ex+s-eyeOff-4, ey+s/2, 3, 3);
        // HP bar (only when damaged)
        if (hp < maxHp) {
            g.setColor(new Color(60,0,0)); g.fillRect(ex, ey-7, s, 4);
            g.setColor(elite ? new Color(180,60,220) : new Color(200,40,40));
            g.fillRect(ex, ey-7, (int)(s*(float)hp/maxHp), 4);
        }
    }
}