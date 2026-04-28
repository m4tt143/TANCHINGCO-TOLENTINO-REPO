
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * FULLY COMPATIBLE Player.java
 * Improved but still works with your old GamePanel.java
 */
public class Player {

    // ===============================
    // CHARACTER TYPES
    // ===============================
    public enum CharacterType {
        SOLDIER, MAGE, TANK, ROGUE
    }

    public CharacterType character;

    // ===============================
    // POSITION
    // ===============================
    public float x, y;
    public static final int SIZE = 64;

    // ===============================
    // STATS
    // ===============================
    public int hp, maxHp;
    public float speed;
    public int damage;
    public int fireRateTicks;

    public boolean multishot = false;
    public int armor = 0;

<<<<<<< Updated upstream
    // SUPER POWER ability (E key)
    public int superCooldown = 0;          // Cooldown counter
    public int superDuration = 0;          // How long super power lasts
    public boolean superActive = false;    // Is super power currently active
    public static final int SUPER_COOLDOWN_MAX = 300; // 5 seconds at 60 FPS
    public static final int SUPER_DURATION_MAX = 90;  // 1.5 seconds at 60 FPS

    // Movement flags (set by KeyListener)
=======
    // Upgrades
    public float enemySlowPercent = 0f;
    public float critChance = 0f;
    public float coinMultiplier = 1f;

    // REQUIRED BY GAMEPANEL
    public int regenTicksLeft = 0;
    public float bulletSpreadAngle = 0f;

    // ===============================
    // MOVEMENT
    // ===============================
>>>>>>> Stashed changes
    public boolean up, down, left, right;

    // ===============================
    // SHOOTING
    // ===============================
    private int fireCooldown = 0;

    // ===============================
    // DASH
    // ===============================
    public float dashVx = 0;
    public float dashVy = 0;

    public int dashCooldown = 0;
    public int dashDuration = 0;

    // REQUIRED OLD NAMES
    public static final int DASH_COOLDOWN_MAX = 180;
    public static final int DASH_DURATION_MAX = 15;

    // ===============================
    // CONSTRUCTOR
    // ===============================
    public Player(float startX, float startY, CharacterType type) {

        x = startX;
        y = startY;
        character = type;

        if (type == CharacterType.SOLDIER) {
            maxHp = 5;
            speed = 3f;
            damage = 1;
            fireRateTicks = 30;
        }

        else if (type == CharacterType.MAGE) {
            maxHp = 3;
            speed = 3.5f;
            damage = 2;
            fireRateTicks = 25;
        }

        else if (type == CharacterType.TANK) {
            maxHp = 8;
            speed = 2f;
            damage = 1;
            fireRateTicks = 40;
        }

        else if (type == CharacterType.ROGUE) {
            maxHp = 4;
            speed = 4f;
            damage = 1;
            fireRateTicks = 20;
        }

        hp = maxHp;
    }

    // ===============================
    // UPDATE
    // ===============================
    public void update() {
<<<<<<< Updated upstream
        // Normal movement
        float moveX = 0, moveY = 0;
        float effectiveSpeed = getEffectiveSpeed();
        if (up)    moveY -= effectiveSpeed;
        if (down)  moveY += effectiveSpeed;
        if (left)  moveX -= effectiveSpeed;
        if (right) moveX += effectiveSpeed;
        
        // Apply dash velocity (invincible frames during dash)
=======

        float moveX = 0;
        float moveY = 0;

        if (up) moveY -= speed;
        if (down) moveY += speed;
        if (left) moveX -= speed;
        if (right) moveX += speed;

>>>>>>> Stashed changes
        if (dashDuration > 0) {
            x += dashVx;
            y += dashVy;
            dashDuration--;
        } else {
            x += moveX;
            y += moveY;
        }

        // Screen limits
        x = Math.max(0, Math.min(GameFrame.WIDTH - SIZE, x));
        y = Math.max(55, Math.min(GameFrame.HEIGHT - SIZE, y));

<<<<<<< Updated upstream
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
=======
        if (fireCooldown > 0)
            fireCooldown--;

        if (dashCooldown > 0)
            dashCooldown--;
>>>>>>> Stashed changes
    }

    // ===============================
    // DASH
    // ===============================
    public boolean tryDash() {
<<<<<<< Updated upstream
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
=======

        if (dashCooldown > 0)
            return false;

        float dirX = 0;
        float dirY = 0;

        if (up) dirY--;
        if (down) dirY++;
        if (left) dirX--;
        if (right) dirX++;

        if (dirX == 0 && dirY == 0)
            return false;

        float len = (float)Math.sqrt(dirX * dirX + dirY * dirY);

        dirX /= len;
        dirY /= len;

        dashVx = dirX * 12f;
        dashVy = dirY * 12f;

>>>>>>> Stashed changes
        dashDuration = DASH_DURATION_MAX;
        dashCooldown = DASH_COOLDOWN_MAX;
        
        // Trigger dash sound effect
        SoundManager.dashAbility();

        return true;
    }
<<<<<<< Updated upstream
    
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
=======
>>>>>>> Stashed changes

    public boolean isDashing() {
        return dashDuration > 0;
    }

    // ===============================
    // SHOOTING
    // ===============================
    public boolean canShoot() {
        return fireCooldown <= 0;
    }

    public void resetCooldown() {
        fireCooldown = fireRateTicks;
    }

    // ===============================
    // DAMAGE
    // ===============================
    public void takeDamage(int dmg) {

        if (isDashing())
            return;

        if (armor > 0) {
            armor -= dmg;

            if (armor < 0)
                armor = 0;

            return;
        }

        hp -= dmg;

        if (hp < 0)
            hp = 0;
    }

    // ===============================
    // HELPERS
    // ===============================
    public float getCenterX() {
        return x + SIZE / 2f;
    }

    public float getCenterY() {
        return y + SIZE / 2f;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x + 5, (int)y + 5, SIZE - 10, SIZE - 10);
    }

    // ===============================
    // DRAW
    // ===============================
    public void draw(Graphics2D g, double aimAngle,
                     Map<CharacterType, BufferedImage> characterImages) {

        int px = (int)x;
        int py = (int)y;

        float cx = getCenterX();
        float cy = getCenterY();

        // Dash trail
        if (isDashing()) {

            for (int i = 1; i <= 3; i++) {

                int tx = (int)(px - dashVx * i * 2);
                int ty = (int)(py - dashVy * i * 2);

                g.setComposite(
                    AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.15f
                    )
                );

                if (characterImages != null &&
                    characterImages.containsKey(character)) {

                    g.drawImage(
                        characterImages.get(character),
                        tx, ty, SIZE, SIZE, null
                    );
                }
            }

            g.setComposite(
                AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1f
                )
            );
        }

        // Rotate
        AffineTransform old = g.getTransform();
        g.rotate(aimAngle, cx, cy);

        // Draw image
        if (characterImages != null &&
            characterImages.containsKey(character)) {

            g.drawImage(
                characterImages.get(character),
                px, py, SIZE, SIZE, null
            );

        } else {
            g.setColor(Color.GREEN);
            g.fillRect(px, py, SIZE, SIZE);
        }

        // Dash glow
        if (isDashing()) {
            g.setColor(new Color(0, 255, 255, 120));
            g.drawOval(px - 5, py - 5, SIZE + 10, SIZE + 10);
        }

<<<<<<< Updated upstream
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
=======
>>>>>>> Stashed changes
        g.setTransform(old);

        drawHPBar(g);
    }

    // ===============================
    // HP BAR
    // ===============================
    public void drawHPBar(Graphics2D g) {

        int width = 60;
        int height = 8;

        int px = (int)x + 2;
        int py = (int)y - 12;

        g.setColor(Color.RED);
        g.fillRect(px, py, width, height);

        g.setColor(Color.GREEN);

        int hpWidth =
            (int)((hp / (float)maxHp) * width);

        g.fillRect(px, py, hpWidth, height);

        g.setColor(Color.BLACK);
        g.drawRect(px, py, width, height);
    }
}

