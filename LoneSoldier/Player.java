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

    /** Draw the player as a blue soldier character. */
    public void draw(Graphics2D g) {
        int px = (int) x;
        int py = (int) y;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(px + 3, py + 3, SIZE, SIZE);

        // Body (blue)
        g.setColor(new Color(40, 110, 220));
        g.fillOval(px, py, SIZE, SIZE);

        // Helmet (darker blue top half)
        g.setColor(new Color(20, 70, 160));
        g.fillArc(px + 4, py, SIZE - 8, SIZE / 2 + 4, 0, 180);

        // Eyes (white)
        g.setColor(Color.WHITE);
        g.fillOval(px + 6,  py + 14, 7, 6);
        g.fillOval(px + 19, py + 14, 7, 6);

        // Pupils (black)
        g.setColor(Color.BLACK);
        g.fillOval(px + 8,  py + 15, 3, 3);
        g.fillOval(px + 21, py + 15, 3, 3);

        // Star badge above player
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(new Color(255, 215, 0));
        g.drawString("*", px + SIZE / 2 - 4, py - 3);
    }
}
