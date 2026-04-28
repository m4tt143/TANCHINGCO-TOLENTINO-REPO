import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

public class Player {
    public enum CharacterType { SOLDIER, MAGE, TANK, ROGUE }
    public CharacterType character;

    public float x, y;
    public static final int SIZE = 64;

    public int hp, maxHp;
    public float speed;
    public int damage;
    public int fireRateTicks;

    public boolean multishot = false;
    public int armor = 0;

    public int superCooldown = 0;
    public int superDuration = 0;
    public boolean superActive = false;
    public static final int SUPER_COOLDOWN_MAX = 300;
    public static final int SUPER_DURATION_MAX = 90;

    public float enemySlowPercent = 0f;
    public float critChance = 0f;
    public float coinMultiplier = 1f;

    public int regenTicksLeft = 0;
    public float bulletSpreadAngle = 0f;
    
    // ── Character-Specific Passives ──
    public int soldierExplosiveCharge = 0;  // Soldier: Every 5th shot explodes
    public int mageChainLightning = 0;      // Mage: Kill hits chain to nearby enemies
    public int tankShieldRegen = 0;         // Tank: Regen armor over time
    public int rogueEvasionChance = 0;      // Rogue: Chance to dodge attacks

    // Temporary buffs
    public int speedBoostTimer = 0;
    public int damageBoostTimer = 0;
    public int shieldTimer = 0;
    public int coinMagnetTimer = 0;

    public boolean up, down, left, right;
    private int fireCooldown = 0;

    public float dashVx = 0, dashVy = 0;
    public int dashCooldown = 0;
    public int dashDuration = 0;
    public static final int DASH_COOLDOWN_MAX = 180;
    public static final int DASH_DURATION_MAX = 15;

    public Player(float startX, float startY, CharacterType type) {
        x = startX; y = startY; character = type;
        if (type == CharacterType.SOLDIER) { 
            maxHp = 5; speed = 3f; damage = 1; fireRateTicks = 30; 
            soldierExplosiveCharge = 0; // Passive: Every 5 shots explode
        }
        else if (type == CharacterType.MAGE) { 
            maxHp = 3; speed = 3.5f; damage = 2; fireRateTicks = 25; 
            mageChainLightning = 0; // Passive: Chain kills to nearby enemies
        }
        else if (type == CharacterType.TANK) { 
            maxHp = 8; speed = 2f; damage = 1; fireRateTicks = 40; 
            tankShieldRegen = 0; // Passive: Regen 1 armor every 3 seconds
        }
        else if (type == CharacterType.ROGUE) { 
            maxHp = 4; speed = 4f; damage = 1; fireRateTicks = 20; 
            rogueEvasionChance = 0; // Passive: 15% dodge chance
        }
        hp = maxHp;
    }

    public void update() {
        float moveX = 0, moveY = 0;
        float effectiveSpeed = getEffectiveSpeed();
        if (up) moveY -= effectiveSpeed;
        if (down) moveY += effectiveSpeed;
        if (left) moveX -= effectiveSpeed;
        if (right) moveX += effectiveSpeed;

        if (dashDuration > 0) { x += dashVx; y += dashVy; dashDuration--; }
        else { x += moveX; y += moveY; }

        x = Math.max(0, Math.min(GameFrame.WIDTH - SIZE, x));
        y = Math.max(55, Math.min(GameFrame.HEIGHT - SIZE, y));

        if (fireCooldown > 0) fireCooldown--;
        if (dashCooldown > 0) dashCooldown--;
        if (superCooldown > 0) superCooldown--;
        if (superDuration > 0) {
            superDuration--;
            if (superDuration <= 0) superActive = false;
        }
        if (speedBoostTimer > 0) speedBoostTimer--;
        if (damageBoostTimer > 0) damageBoostTimer--;
        if (shieldTimer > 0) shieldTimer--;
        if (coinMagnetTimer > 0) coinMagnetTimer--;
    }

    public boolean tryDash() {
        if (dashCooldown > 0) return false;
        float dirX = 0, dirY = 0;
        if (up) dirY -= 1; if (down) dirY += 1;
        if (left) dirX -= 1; if (right) dirX += 1;
        if (dirX == 0 && dirY == 0) return false;
        float len = (float)Math.sqrt(dirX*dirX + dirY*dirY);
        dirX /= len; dirY /= len;
        float effectiveSpeed = getEffectiveSpeed();
        dashVx = dirX * 12f * (effectiveSpeed / speed);
        dashVy = dirY * 12f * (effectiveSpeed / speed);
        dashDuration = DASH_DURATION_MAX;
        dashCooldown = DASH_COOLDOWN_MAX;
        SoundManager.dashAbility();
        return true;
    }

    public boolean trySuperPower() {
        if (superCooldown > 0 || superActive) return false;
        superActive = true;
        superDuration = SUPER_DURATION_MAX;
        superCooldown = SUPER_COOLDOWN_MAX;
        return true;
    }

    public boolean isDashing() { return dashDuration > 0; }
    public boolean canShoot() { return fireCooldown <= 0; }
    public void resetCooldown() { fireCooldown = getEffectiveFireRateTicks(); }

    public int getEffectiveFireRateTicks() {
        if (superActive && character == CharacterType.SOLDIER) return Math.max(5, fireRateTicks / 3);
        return fireRateTicks;
    }

    public int getEffectiveDamage() {
        int d = damage;
        if (damageBoostTimer > 0) d += 2;
        if (superActive && character == CharacterType.MAGE) d *= 2;
        return d;
    }

    public float getEffectiveSpeed() {
        float s = speed;
        if (speedBoostTimer > 0) s *= 1.6f;
        if (superActive && character == CharacterType.ROGUE) s *= 2.0f;
        return s;
    }

    public boolean isSuperInvincible() {
        return superActive && character == CharacterType.TANK;
    }

    public void takeDamage(int dmg) {
        if (isSuperInvincible()) return;
        if (shieldTimer > 0) return;
        if (armor > 0) { armor = Math.max(0, armor - dmg); return; }
        hp = Math.max(0, hp - dmg);
    }

    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }
    public Rectangle getBounds() { return new Rectangle((int)x + 5, (int)y + 5, SIZE - 10, SIZE - 10); }

    public void draw(Graphics2D g, double aimAngle, Map<CharacterType, BufferedImage> characterImages) {
        int px = (int)x, py = (int)y;
        float cx = getCenterX(), cy = getCenterY();

        if (isDashing()) {
            for (int i = 1; i <= 3; i++) {
                int tx = (int)(px - dashVx * i * 2);
                int ty = (int)(py - dashVy * i * 2);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                if (characterImages != null && characterImages.containsKey(character)) {
                    g.drawImage(characterImages.get(character), tx, ty, SIZE, SIZE, null);
                }
            }
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        AffineTransform old = g.getTransform();
        g.rotate(aimAngle, cx, cy);

        if (characterImages != null && characterImages.containsKey(character)) {
            g.drawImage(characterImages.get(character), px, py, SIZE, SIZE, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(px, py, SIZE, SIZE);
        }

        if (isDashing()) {
            g.setColor(new Color(0, 255, 255, 120));
            g.drawOval(px - 5, py - 5, SIZE + 10, SIZE + 10);
        }

        if (superActive) {
            float superAlpha = (float) superDuration / SUPER_DURATION_MAX;
            Color superColor = switch (character) {
                case SOLDIER -> new Color(255, 255, 0, (int)(superAlpha * 150));
                case MAGE -> new Color(255, 0, 255, (int)(superAlpha * 150));
                case TANK -> new Color(0, 255, 0, (int)(superAlpha * 150));
                case ROGUE -> new Color(255, 165, 0, (int)(superAlpha * 150));
            };
            g.setColor(superColor);
            g.setStroke(new BasicStroke(3.0f));
            int glow = (int)(SIZE * (1f + 0.5f * superAlpha));
            g.drawOval(px - glow/2 + SIZE/2, py - glow/2 + SIZE/2, glow, glow);
            g.setStroke(new BasicStroke(1));
        }

        // Buff overlays
        if (shieldTimer > 0) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.01) * 0.5 + 0.5);
            g.setColor(new Color(80, 80, 255, (int)(60 + pulse * 60)));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(px-4, py-4, SIZE+8, SIZE+8);
            g.setStroke(new BasicStroke(1));
        }
        if (speedBoostTimer > 0) {
            g.setColor(new Color(0, 200, 255, 40));
            g.drawOval(px-8, py-8, SIZE+16, SIZE+16);
        }
        if (damageBoostTimer > 0) {
            g.setColor(new Color(255, 100, 0, 40));
            g.drawOval(px-6, py-6, SIZE+12, SIZE+12);
        }

        g.setTransform(old);
        drawHPBar(g);
    }

    public void drawHPBar(Graphics2D g) {
        int width = 60, height = 8;
        int px = (int)x + 2, py = (int)y - 12;
        g.setColor(Color.RED);
        g.fillRect(px, py, width, height);
        g.setColor(Color.GREEN);
        g.fillRect(px, py, (int)((hp / (float)maxHp) * width), height);
        g.setColor(Color.BLACK);
        g.drawRect(px, py, width, height);
    }
}