import java.awt.*;
import java.util.Random;

/**
 * Enemy - Zombie that shambles toward the player.
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

    // Visual variety — rotting flesh tones
    private Color skinColor;
    private Color clothColor;
    private int   bloodSplat;   // random wound detail per zombie

    private static final Random rand = new Random();

    public Enemy(float x, float y, int hp, float speed) {
        this.x     = x;
        this.y     = y;
        this.hp    = hp;
        this.maxHp = hp;
        this.speed = speed;

        // Rotting green/grey skin variation
        int r = 80  + rand.nextInt(40);
        int g = 110 + rand.nextInt(40);
        int b = 50  + rand.nextInt(30);
        this.skinColor  = new Color(r, g, b);

        // Torn, dirty clothing — muted browns/greys
        int cr = 60 + rand.nextInt(40);
        int cg = 50 + rand.nextInt(30);
        int cb = 40 + rand.nextInt(30);
        this.clothColor = new Color(cr, cg, cb);

        this.bloodSplat = rand.nextInt(3); // 0, 1, or 2 = different wound positions
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

    /** Draw the enemy as a shambling zombie. */
    public void draw(Graphics2D g) {
        int ex = (int) x;
        int ey = (int) y;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(ex + 1, ey + 20, SIZE - 2, 5);

        // Legs — torn dark trousers
        g.setColor(new Color(
            Math.max(0, clothColor.getRed() - 20),
            Math.max(0, clothColor.getGreen() - 15),
            Math.max(0, clothColor.getBlue() - 10)));
        g.fillRect(ex + 6,  ey + 17, 5, 9);
        g.fillRect(ex + 15, ey + 17, 5, 9);

        // Shoe / foot stumps (darker)
        g.setColor(new Color(30, 25, 20));
        g.fillRect(ex + 5,  ey + 24, 6, 3);
        g.fillRect(ex + 14, ey + 24, 6, 3);

        // Body — tattered shirt
        g.setColor(clothColor);
        g.fillRect(ex + 3, ey + 9, SIZE - 6, 11);

        // Torn shirt detail (darker patches simulating rips)
        g.setColor(new Color(
            Math.max(0, clothColor.getRed() - 35),
            Math.max(0, clothColor.getGreen() - 25),
            Math.max(0, clothColor.getBlue() - 20)));
        g.fillRect(ex + 5,  ey + 11, 3, 4);
        g.fillRect(ex + 17, ey + 13, 3, 5);

        // Exposed rotting skin peek at torn areas
        g.setColor(skinColor);
        g.fillRect(ex + 5,  ey + 11, 2, 3);
        g.fillRect(ex + 17, ey + 13, 2, 4);

        // Arms — outstretched and reaching (zombie pose)
        // Left arm raised/out
        g.setColor(skinColor);
        g.fillRect(ex - 3, ey + 7,  5, 4);   // forearm out to the side
        g.fillRect(ex + 1, ey + 9,  2, 6);   // upper arm

        // Right arm raised/out
        g.fillRect(ex + 23, ey + 7,  5, 4);
        g.fillRect(ex + 23, ey + 9,  2, 6);

        // Bony claw-fingers (left hand)
        g.setColor(new Color(
            Math.max(0, skinColor.getRed() - 20),
            Math.max(0, skinColor.getGreen() + 10),
            Math.max(0, skinColor.getBlue() - 10)));
        g.fillRect(ex - 5, ey + 7,  2, 2);
        g.fillRect(ex - 5, ey + 9,  2, 2);
        g.fillRect(ex - 5, ey + 11, 2, 2);

        // Bony claw-fingers (right hand)
        g.fillRect(ex + 29, ey + 7,  2, 2);
        g.fillRect(ex + 29, ey + 9,  2, 2);
        g.fillRect(ex + 29, ey + 11, 2, 2);

        // Neck (rotting skin)
        g.setColor(skinColor);
        g.fillRect(ex + 10, ey + 7, 6, 4);

        // Head — misshapen zombie skull
        g.setColor(skinColor);
        g.fillOval(ex + 3, ey, SIZE - 6, 10);

        // Messy matted hair (clumps, not a clean helmet)
        g.setColor(new Color(30, 22, 10));
        g.fillOval(ex + 3,  ey,     SIZE - 6, 5);   // main hair mass
        g.fillOval(ex + 2,  ey + 1, 5, 4);           // left clump
        g.fillOval(ex + 19, ey + 1, 5, 4);           // right clump
        g.fillOval(ex + 10, ey - 1, 6, 4);           // top tuft

        // Rotting forehead skin
        g.setColor(skinColor);
        g.fillRect(ex + 5, ey + 3, SIZE - 10, 4);

        // Glowing hollow eyes — yellow-green undead glow
        g.setColor(new Color(180, 230, 50, 200));
        g.fillOval(ex + 5,  ey + 3, 5, 4);
        g.fillOval(ex + 16, ey + 3, 5, 4);

        // Dark slit pupils
        g.setColor(new Color(10, 20, 0));
        g.fillOval(ex + 7,  ey + 4, 2, 3);
        g.fillOval(ex + 18, ey + 4, 2, 3);

        // Rotting open mouth / exposed teeth
        g.setColor(new Color(20, 10, 10));
        g.fillRect(ex + 8, ey + 7, 10, 2);  // mouth gap
        g.setColor(new Color(220, 210, 180));
        g.fillRect(ex + 9,  ey + 7, 2, 2);  // tooth 1
        g.fillRect(ex + 13, ey + 7, 2, 2);  // tooth 2
        g.fillRect(ex + 16, ey + 7, 2, 2);  // tooth 3

        // Blood / wound details (vary per zombie)
        g.setColor(new Color(160, 10, 10, 200));
        if (bloodSplat == 0) {
            // Bite wound on neck
            g.fillOval(ex + 11, ey + 7, 4, 3);
        } else if (bloodSplat == 1) {
            // Gash on forehead
            g.fillRect(ex + 8, ey + 3, 6, 2);
        } else {
            // Chest wound
            g.fillOval(ex + 9, ey + 12, 5, 4);
        }

        // HP bar (visible only when damaged)
        if (hp < maxHp) {
            g.setColor(new Color(20, 40, 0));
            g.fillRect(ex, ey - 7, SIZE, 3);
            float ratio = (float) hp / maxHp;
            g.setColor(new Color(120, 200, 40));
            g.fillRect(ex, ey - 7, (int) (SIZE * ratio), 3);
        }
    }
}
