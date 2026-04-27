import java.awt.*;

/**
 * Player - The hero soldier controlled by the user.
 * Auto-aims and shoots at the nearest enemy.
 * Upgradeable stats between waves.
 */
public class Player {

    // ── Character Type ──────────────────────────────────────────
    public enum CharacterType { SOLDIER, MAGE, TANK, ROGUE }
    public CharacterType character;

    // Position & Size
    public float x, y;
    public static final int SIZE = 32;

    // Stats
    public int hp, maxHp;
    public float speed;
    public int damage;
    public int fireRateTicks;  // Ticks between shots (lower = faster)
    public boolean multishot;  // Shoot 3 bullets at once
    public int armor = 0;         // Absorbs hits before HP damage
    
    // New upgrades
    public float enemySlowPercent = 0f;    // Reduce enemy speed (0.2 = 20% slower)
    public float critChance = 0f;          // 0.2 = 20% chance
    public float coinMultiplier = 1.0f;    // 1.5 = 50% more coins
    public int regenTicksLeft = 0;         // Ticks until next regen
    public float bulletSpreadAngle = 0f;   // Extra spread for multishot

    // DASH ability (Spacebar)
    public float dashVx = 0, dashVy = 0;   // Dash velocity
    public int dashCooldown = 0;           // Cooldown counter
    public int dashDuration = 0;           // How long dash lasts
    public static final int DASH_COOLDOWN_MAX = 180; // 3 seconds at 60 FPS
    public static final int DASH_DURATION_MAX = 15;  // 0.25 seconds

    // Movement flags (set by KeyListener)
    public boolean up, down, left, right;

    // Internal fire cooldown counter
    private int fireCooldown = 0;

    public Player(float startX, float startY, CharacterType character) {
        this.x = startX;
        this.y = startY;
        this.character = character;
        
        // Base stats by character
        switch (character) {
            case SOLDIER -> {
                this.maxHp      = 5;
                this.speed      = 3.0f;
                this.damage     = 1;
                this.fireRateTicks = 30;
            }
            case MAGE -> {
                this.maxHp      = 3;
                this.speed      = 3.5f;
                this.damage     = 2;
                this.fireRateTicks = 25;
            }
            case TANK -> {
                this.maxHp      = 8;
                this.speed      = 2.0f;
                this.damage     = 1;
                this.fireRateTicks = 40;
            }
            case ROGUE -> {
                this.maxHp      = 4;
                this.speed      = 4.0f;
                this.damage     = 1;
                this.fireRateTicks = 20;
            }
        }
        
        this.hp         = maxHp;
        this.multishot  = false;
    }

    /** Called every game tick to move the player and tick cooldowns. */
    public void update() {
        // Normal movement
        float moveX = 0, moveY = 0;
        if (up)    moveY -= speed;
        if (down)  moveY += speed;
        if (left)  moveX -= speed;
        if (right) moveX += speed;
        
        // Apply dash velocity (invincible frames during dash)
        if (dashDuration > 0) {
            x += dashVx;
            y += dashVy;
            dashDuration--;
        } else {
            // Normal movement only when not dashing
            x += moveX;
            y += moveY;
        }

        // Keep player inside screen bounds (below HUD)
        x = Math.max(0, Math.min(GameFrame.WIDTH  - SIZE, x));
        y = Math.max(55, Math.min(GameFrame.HEIGHT - SIZE, y));

        // Tick cooldowns
        if (fireCooldown > 0) fireCooldown--;
        if (dashCooldown > 0) dashCooldown--;
    }
    
    /** Activate dash in the direction of current movement */
    public boolean tryDash() {
        if (dashCooldown > 0) return false; // Still on cooldown
        
        float dirX = 0, dirY = 0;
        if (up)    dirY -= 1;
        if (down)  dirY += 1;
        if (left)  dirX -= 1;
        if (right) dirX += 1;
        
        if (dirX == 0 && dirY == 0) return false; // Not moving
        
        // Normalize direction
        float len = (float)Math.sqrt(dirX*dirX + dirY*dirY);
        dirX /= len; dirY /= len;
        
        // Set dash velocity (fast burst)
        dashVx = dirX * 12f;
        dashVy = dirY * 12f;
        dashDuration = DASH_DURATION_MAX;
        dashCooldown = DASH_COOLDOWN_MAX;
        return true;
    }
    
    public boolean isDashing() { return dashDuration > 0; }
    public boolean canShoot()    { return fireCooldown <= 0; }
    public void   resetCooldown() { fireCooldown = fireRateTicks; }
    public void   takeDamage(int dmg) { if(armor>0){armor=Math.max(0,armor-dmg);return;} hp = Math.max(0, hp - dmg); }

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
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(px + 3, py + 3, SIZE, SIZE);

        // Body color by character
        Color bodyColor = switch(character) {
            case SOLDIER -> new Color(40, 110, 220);   // Blue
            case MAGE -> new Color(150, 80, 220);      // Purple
            case TANK -> new Color(220, 150, 40);      // Orange
            case ROGUE -> new Color(100, 200, 80);     // Green
        };
        
        g.setColor(bodyColor);
        g.fillOval(px, py, SIZE, SIZE);

        // Helmet (darker shade — top half points in aim direction)
        g.setColor(bodyColor.darker());
        g.fillArc(px + 4, py, SIZE - 8, SIZE / 2 + 4, 0, 180);

        // Eyes (white)
        g.setColor(Color.WHITE);
        g.fillOval(px + 6,  py + 14, 7, 6);
        g.fillOval(px + 19, py + 14, 7, 6);

        // Pupils (dark)
        g.setColor(Color.BLACK);
        g.fillOval(px + 8,  py + 15, 3, 3);
        g.fillOval(px + 21, py + 15, 3, 3);

        // Restore transform so the star badge stays upright in world space
        g.setTransform(old);

        // DASH visual effect (cyan aura during dash)
        if (isDashing()) {
            float dashAlpha = (float) dashDuration / DASH_DURATION_MAX;
            g.setColor(new Color(0, 200, 255, (int)(dashAlpha * 180)));
            g.setStroke(new BasicStroke(2.5f));
            int glowSize = (int)(SIZE * (1f + 0.3f * (1f - dashAlpha)));
            g.drawOval(px - glowSize/2 + SIZE/2, py - glowSize/2 + SIZE/2, glowSize, glowSize);
            g.setStroke(new BasicStroke(1));
        }

        // Character badge
        g.setFont(new Font("Arial", Font.BOLD, 11));
        String badge = character.toString().substring(0, 1);
        g.setColor(new Color(255, 215, 0));
        g.drawString(badge, px + SIZE / 2 - 4, py - 3);
    }
}