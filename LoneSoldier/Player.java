import java.awt.*;
import java.awt.geom.AffineTransform;

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
    public static final int SIZE = 64;

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

    // SUPER POWER ability (E key)
    public int superCooldown = 0;          // Cooldown counter
    public int superDuration = 0;          // How long super power lasts
    public boolean superActive = false;    // Is super power currently active
    public static final int SUPER_COOLDOWN_MAX = 300; // 5 seconds at 60 FPS
    public static final int SUPER_DURATION_MAX = 90;  // 1.5 seconds at 60 FPS

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
        float effectiveSpeed = getEffectiveSpeed();
        if (up)    moveY -= effectiveSpeed;
        if (down)  moveY += effectiveSpeed;
        if (left)  moveX -= effectiveSpeed;
        if (right) moveX += effectiveSpeed;
        
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
        if (superCooldown > 0) superCooldown--;
        if (superDuration > 0) {
            superDuration--;
            if (superDuration <= 0) {
                superActive = false;
            }
        }
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
        float effectiveSpeed = getEffectiveSpeed();
        dashVx = dirX * 12f * (effectiveSpeed / speed); // Scale dash speed by speed multiplier
        dashVy = dirY * 12f * (effectiveSpeed / speed);
        dashDuration = DASH_DURATION_MAX;
        dashCooldown = DASH_COOLDOWN_MAX;
        return true;
    }
    
    /** Activate super power based on character type */
    public boolean trySuperPower() {
        if (superCooldown > 0 || superActive) return false; // Still on cooldown or already active
        
        superActive = true;
        superDuration = SUPER_DURATION_MAX;
        superCooldown = SUPER_COOLDOWN_MAX;
        
        // Character-specific super power effects are handled in the getters
        return true;
    }
    
    public boolean isDashing() { return dashDuration > 0; }
    public boolean canShoot()    { return fireCooldown <= 0; }
    public void   resetCooldown() { fireCooldown = getEffectiveFireRateTicks(); }
    
    /** Get the effective fire rate ticks, modified by super powers */
    public int getEffectiveFireRateTicks() {
        if (superActive) {
            switch (character) {
                case SOLDIER -> { return Math.max(5, fireRateTicks / 3); } // 3x faster for soldier
                default -> { return fireRateTicks; }
            }
        }
        return fireRateTicks;
    }
    
    /** Get the effective damage, modified by super powers */
    public int getEffectiveDamage() {
        if (superActive) {
            switch (character) {
                case MAGE -> { return damage * 2; } // Double damage for mage
                default -> { return damage; }
            }
        }
        return damage;
    }
    
    /** Get the effective speed, modified by super powers */
    public float getEffectiveSpeed() {
        if (superActive) {
            switch (character) {
                case ROGUE -> { return speed * 2.0f; } // Double speed for rogue
                default -> { return speed; }
            }
        }
        return speed;
    }
    
    /** Check if player is invincible due to super power */
    public boolean isSuperInvincible() {
        return superActive && character == CharacterType.TANK;
    }
    public void   takeDamage(int dmg) { 
        if (isSuperInvincible()) return; // Tank super power: temporary invincibility
        if(armor>0){armor=Math.max(0,armor-dmg);return;} 
        hp = Math.max(0, hp - dmg); 
    }

    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }

    public Rectangle getBounds() {
        // Slightly smaller hitbox than visual for fairness
        return new Rectangle((int) x + 5, (int) y + 5, SIZE - 10, SIZE - 10);
    }

    /** Draw the player rotated to face the mouse cursor.
     *  aimAngle is in radians; 0 = facing up, increases clockwise. */
    public void draw(Graphics2D g, double aimAngle, java.util.Map<CharacterType, java.awt.image.BufferedImage> characterImages) {
        int px = (int) x;
        int py = (int) y;
        float cx = getCenterX();
        float cy = getCenterY();

        // Draw character image first (before any rotation)
        if (characterImages != null && characterImages.containsKey(character)) {
            java.awt.image.BufferedImage img = characterImages.get(character);
            // High-quality scaling for gameplay
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            
            // Draw dash trail (semi-transparent copies behind current position)
            if (isDashing()) {
                float trailAlpha = 0.3f;
                for (int i = 1; i <= 3; i++) {
                    float trailX = px - dashVx * i * 2;
                    float trailY = py - dashVy * i * 2;
                    if (trailX >= 0 && trailX <= GameFrame.WIDTH - SIZE && trailY >= 55 && trailY <= GameFrame.HEIGHT - SIZE) {
                        Composite oldComposite = g2d.getComposite();
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trailAlpha / i));
                        g2d.drawImage(img, (int)trailX, (int)trailY, SIZE, SIZE, null);
                        g2d.setComposite(oldComposite);
                        trailAlpha *= 0.7f;
                    }
                }
            }
            
            g2d.drawImage(img, px, py, SIZE, SIZE, null);
        }

        // Save transform, rotate around player center for dash effect only
        java.awt.geom.AffineTransform old = g.getTransform();
        g.rotate(aimAngle, cx, cy);

        // DASH visual effect (cyan aura during dash)
        if (isDashing()) {
            float dashAlpha = (float) dashDuration / DASH_DURATION_MAX;
            g.setColor(new Color(0, 200, 255, (int)(dashAlpha * 180)));
            g.setStroke(new BasicStroke(2.5f));
            int glowSize = (int)(SIZE * (1f + 0.3f * (1f - dashAlpha)));
            g.drawOval(px - glowSize/2 + SIZE/2, py - glowSize/2 + SIZE/2, glowSize, glowSize);
            g.setStroke(new BasicStroke(1));
        }

        // SUPER POWER visual effects
        if (superActive) {
            float superAlpha = (float) superDuration / SUPER_DURATION_MAX;
            Color superColor;
            switch (character) {
                case SOLDIER -> superColor = new Color(255, 255, 0, (int)(superAlpha * 150)); // Yellow for attack speed
                case MAGE -> superColor = new Color(255, 0, 255, (int)(superAlpha * 150)); // Magenta for damage
                case TANK -> superColor = new Color(0, 255, 0, (int)(superAlpha * 150)); // Green for invincibility
                case ROGUE -> superColor = new Color(255, 165, 0, (int)(superAlpha * 150)); // Orange for speed
                default -> superColor = new Color(255, 255, 255, (int)(superAlpha * 150));
            }
            g.setColor(superColor);
            g.setStroke(new BasicStroke(3.0f));
            int superGlowSize = (int)(SIZE * (1f + 0.5f * superAlpha));
            g.drawOval(px - superGlowSize/2 + SIZE/2, py - superGlowSize/2 + SIZE/2, superGlowSize, superGlowSize);
            g.setStroke(new BasicStroke(1));
        }

        // Restore transform
        g.setTransform(old);
    }
}