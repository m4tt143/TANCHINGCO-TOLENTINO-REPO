import java.awt.*;
import java.util.Random;

/**
 * Enemy - Zombie that shambles toward the player.
 * Normal zombies glow green. Elite zombies glow purple and are bigger.
 */
public class Enemy {

    public float x, y;
    public static final int SIZE = 26;

    public int hp, maxHp;
    public float speed;
    public boolean elite;
    public int coinDrop;

    private Color skinColor;
    private Color shirtColor;
    private int woundType;

    // ── Visual Effects ──
    public float spawnScale = 0f;
    public int hitFlash = 0;
    public float knockbackX = 0, knockbackY = 0;

    private static final Random rand = new Random();

    public Enemy(float x, float y, int hp, float speed, boolean elite) {
        this.x     = x;
        this.y     = y;
        this.hp    = hp;
        this.maxHp = hp;
        this.speed = speed;
        this.elite = elite;
        this.coinDrop = elite ? 3 + rand.nextInt(3) : 1;
        this.woundType = rand.nextInt(3);

        if (elite) {
            skinColor  = new Color(130 + rand.nextInt(30), 80 + rand.nextInt(20), 160 + rand.nextInt(30));
            shirtColor = new Color(60, 20, 80);
        } else {
            skinColor  = new Color(80 + rand.nextInt(40), 110 + rand.nextInt(35), 50 + rand.nextInt(30));
            shirtColor = new Color(55 + rand.nextInt(30), 45 + rand.nextInt(20), 35 + rand.nextInt(20));
        }
    }

    public void update(float targetX, float targetY, float moveSpeed) {
        // Spawn animation
        if (spawnScale < 1f) spawnScale += 0.06f;

        // Knockback recovery
        if (hitFlash > 0) hitFlash--;
        x += knockbackX;
        y += knockbackY;
        knockbackX *= 0.85f;
        knockbackY *= 0.85f;

        float dx   = targetX - getCenterX();
        float dy   = targetY - getCenterY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            x += (dx / dist) * moveSpeed;
            y += (dy / dist) * moveSpeed;
        }
    }

    public float getCenterX() { return x + getSize() / 2f; }
    public float getCenterY() { return y + getSize() / 2f; }
    public int   getSize()    { return elite ? SIZE + 10 : SIZE; }

    public Rectangle getBounds() {
        int s = getSize();
        return new Rectangle((int) x + 2, (int) y + 2, s - 4, s - 4);
    }

    public void draw(Graphics2D g) {
        if (spawnScale <= 0.01f) return;

        // Apply spawn-scale transform
        Graphics2D eg = (Graphics2D) g.create();
        float cx = x + getSize() / 2f;
        float cy = y + getSize() / 2f;
        eg.translate(cx, cy);
        eg.scale(spawnScale, spawnScale);
        eg.translate(-cx, -cy);
        g = eg; // redirect all drawing

        int s  = getSize();
        int ex = (int) x;
        int ey = (int) y;

        // ── Outer glow ring ──
        Color glowColor = elite
            ? new Color(180, 60, 255, 60)
            : new Color(60, 200, 60, 50);
        g.setColor(glowColor);
        g.fillOval(ex - 5, ey - 5, s + 10, s + 10);

        // ── Drop shadow ──
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(ex + 2, ey + s - 4, s - 4, 6);

        // ── Legs ──
        Color pantsColor = new Color(
            Math.max(0, shirtColor.getRed() - 15),
            Math.max(0, shirtColor.getGreen() - 10),
            Math.max(0, shirtColor.getBlue() - 10)
        );
        g.setColor(pantsColor);
        g.fillRect(ex + s/2 - 8, ey + s - 6,  5, 8);
        g.fillRect(ex + s/2 + 3, ey + s - 6,  5, 8);

        g.setColor(new Color(25, 20, 15));
        g.fillRect(ex + s/2 - 9, ey + s + 1, 6, 3);
        g.fillRect(ex + s/2 + 2, ey + s + 1, 6, 3);

        // ── Body ──
        g.setColor(shirtColor);
        g.fillRect(ex + 3, ey + s/2, s - 6, s/2 - 2);

        g.setColor(new Color(
            Math.max(0, shirtColor.getRed() - 30),
            Math.max(0, shirtColor.getGreen() - 20),
            Math.max(0, shirtColor.getBlue() - 15)
        ));
        g.fillRect(ex + 5,      ey + s/2 + 2, 3, 4);
        g.fillRect(ex + s - 9,  ey + s/2 + 5, 3, 5);

        g.setColor(skinColor);
        g.fillRect(ex + 5,      ey + s/2 + 2, 2, 3);
        g.fillRect(ex + s - 9,  ey + s/2 + 5, 2, 4);

        // ── Arms ──
        g.setColor(skinColor);
        g.fillRect(ex - 4, ey + s/2 + 1, 6, 4);
        g.fillRect(ex + s - 2, ey + s/2 + 1, 6, 4);

        Color clawColor = new Color(
            Math.max(0, skinColor.getRed() - 15),
            Math.min(255, skinColor.getGreen() + 10),
            Math.max(0, skinColor.getBlue() - 5)
        );
        g.setColor(clawColor);
        g.fillRect(ex - 7, ey + s/2,     2, 2);
        g.fillRect(ex - 7, ey + s/2 + 2, 2, 2);
        g.fillRect(ex - 7, ey + s/2 + 4, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2,     2, 2);
        g.fillRect(ex + s + 5, ey + s/2 + 2, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2 + 4, 2, 2);

        // ── Neck ──
        g.setColor(skinColor);
        g.fillRect(ex + s/2 - 4, ey + s/2 - 3, 8, 5);

        // ── Head ──
        g.setColor(skinColor);
        g.fillOval(ex + 3, ey, s - 6, s/2 + 4);

        // ── Hair ──
        g.setColor(new Color(25, 18, 8));
        g.fillOval(ex + 3,       ey,      s - 6, s/4 + 1);
        g.fillOval(ex + 2,       ey + 1,  5, 4);
        g.fillOval(ex + s - 8,   ey + 1,  5, 4);
        g.fillOval(ex + s/2 - 4, ey - 2,  7, 4);

        g.setColor(skinColor);
        g.fillRect(ex + 5, ey + s/4 - 1, s - 10, 5);

        // ── Eyes ──
        Color eyeGlow = elite
            ? new Color(210, 100, 255, 220)
            : new Color(150, 255, 50, 220);
        g.setColor(eyeGlow);
        g.fillOval(ex + 5,      ey + s/4,     6, 5);
        g.fillOval(ex + s - 12, ey + s/4,     6, 5);

        g.setColor(new Color(5, 10, 0));
        g.fillOval(ex + 7,      ey + s/4 + 1, 2, 3);
        g.fillOval(ex + s - 10, ey + s/4 + 1, 2, 3);

        // ── Mouth ──
        g.setColor(new Color(15, 5, 5));
        g.fillRect(ex + s/2 - 6, ey + s/2 - 2, 12, 3);
        g.setColor(new Color(210, 200, 170));
        g.fillRect(ex + s/2 - 5, ey + s/2 - 2, 2, 2);
        g.fillRect(ex + s/2 - 1, ey + s/2 - 2, 2, 2);
        g.fillRect(ex + s/2 + 3, ey + s/2 - 2, 2, 2);

        // ── Blood / wound ──
        g.setColor(new Color(150, 8, 8, 190));
        if (woundType == 0) {
            g.fillOval(ex + s/2 - 2, ey + s/2 - 4, 5, 3);
        } else if (woundType == 1) {
            g.fillRect(ex + s/2 - 4, ey + s/4 - 1, 7, 2);
        } else {
            g.fillOval(ex + s/2 - 3, ey + s/2 + 4, 6, 5);
        }

        // ── Elite glow ring ──
        if (elite) {
            g.setColor(new Color(180, 60, 255, 100));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(ex - 3, ey - 3, s + 6, s + 6);
            g.setStroke(new BasicStroke(1f));
        }

        // ── HP bar ──
        if (hp < maxHp) {
            g.setColor(new Color(20, 20, 20, 180));
            g.fillRect(ex, ey - 8, s, 4);
            float ratio = (float) hp / maxHp;
            g.setColor(elite ? new Color(160, 50, 220) : new Color(80, 220, 80));
            g.fillRect(ex, ey - 8, (int)(s * ratio), 4);
        }

        // ── Hit flash overlay ──
        if (hitFlash > 0) {
            g.setColor(new Color(255, 255, 255, hitFlash * 55));
            g.fillOval(ex - 4, ey - 4, s + 8, s + 8);
        }

        eg.dispose();
    }
}