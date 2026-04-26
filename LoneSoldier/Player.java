import java.awt.*;

/**
 * Player - The hero soldier controlled by the user.
 * Auto-aims and shoots at the nearest enemy.
 * Upgradeable stats between waves.
 */
public class Player {

    // Position & Size
    public float x, y;
    public static final int SIZE = 32;

    // Stats
    public int hp, maxHp;
    public float speed;
    public int damage;
    public int fireRateTicks;  // Ticks between shots (lower = faster)
    public boolean multishot;  // Shoot 3 bullets at once

    // Movement flags (set by KeyListener)
    public boolean up, down, left, right;

    // Internal fire cooldown counter
    private int fireCooldown = 0;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        this.maxHp      = 5;
        this.hp         = maxHp;
        this.speed      = 3.0f;
        this.damage     = 1;
        this.fireRateTicks = 30;
        this.multishot  = false;
    }

    /** Called every game tick to move the player and tick cooldowns. */
    public void update() {
        if (up)    y -= speed;
        if (down)  y += speed;
        if (left)  x -= speed;
        if (right) x += speed;

        // Keep player inside screen bounds (below HUD)
        x = Math.max(0, Math.min(GameFrame.WIDTH  - SIZE, x));
        y = Math.max(55, Math.min(GameFrame.HEIGHT - SIZE, y));

        if (fireCooldown > 0) fireCooldown--;
    }

    public boolean canShoot()    { return fireCooldown <= 0; }
    public void   resetCooldown() { fireCooldown = fireRateTicks; }
    public void   takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }

    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }

    public Rectangle getBounds() {
        // Slightly smaller hitbox than visual for fairness
        return new Rectangle((int) x + 5, (int) y + 5, SIZE - 10, SIZE - 10);
    }

    /** Draw the player rotated to face the mouse cursor.
     *  aimAngle is in radians; 0 = facing up, increases clockwise. */
    public void draw(Graphics2D g, double aimAngle) {
        int px = (int) x;
        int py = (int) y;
        float cx = getCenterX();
        float cy = getCenterY();

        // Save transform, rotate around player center toward mouse
        java.awt.geom.AffineTransform old = g.getTransform();
        g.rotate(aimAngle, cx, cy);

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(px + 1, py + 26, SIZE - 2, 6);

        // Legs (khaki/tan)
        g.setColor(new Color(210, 180, 140));
        g.fillRect(px + 7, py + 20, 5, 10);
        g.fillRect(px + 20, py + 20, 5, 10);

        // Body/Armor (military green with darker panels)
        g.setColor(new Color(85, 120, 70));
        g.fillRect(px + 3, py + 10, SIZE - 6, 13);

        // Chest armor stripe (darker)
        g.setColor(new Color(60, 90, 50));
        g.fillRect(px + 12, py + 10, 8, 13);

        // Arms (green)
        g.setColor(new Color(100, 140, 85));
        g.fillRect(px + 2, py + 12, 3, 8);
        g.fillRect(px + 27, py + 12, 3, 8);

        // Helmet (dark with visor)
        g.setColor(new Color(50, 50, 50));
        g.fillOval(px + 5, py + 2, SIZE - 10, 10);

        // Helmet visor (reflective)
        g.setColor(new Color(100, 140, 160));
        g.fillRect(px + 7, py + 3, SIZE - 14, 6);

        // Eyes through visor
        g.setColor(new Color(220, 220, 100));
        g.fillOval(px + 8,  py + 5, 3, 3);
        g.fillOval(px + 21, py + 5, 3, 3);

        // Gun barrel (pointing forward)
        g.setColor(new Color(40, 40, 40));
        g.fillRect(px + 14, py - 3, 4, 6);

        // Restore transform so the star badge stays upright in world space
        g.setTransform(old);

        // Star badge above player (always upright)
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(new Color(255, 215, 0));
        g.drawString("*", px + SIZE / 2 - 4, py - 3);
    }
}