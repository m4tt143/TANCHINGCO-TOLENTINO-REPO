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
    private static final int REGEN_INTERVAL = 60; // ticks between each regen heal
    private int regenTickInterval = 0;

    public float bulletSpreadAngle = 0f;

    // ── Character-Specific Passives ──
    // Soldier: Every 5th shot explodes
    public int soldierExplosiveCharge = 0;
    private int soldierShotCounter = 0;
    private static final int SOLDIER_EXPLOSIVE_EVERY = 5;

    // Mage: Chain lightning on kill (flag set externally, checked by game loop)
    public int mageChainLightning = 0; // radius in pixels; 0 = off
    public boolean mageChainTriggered = false;

    // Tank: Regen 1 armor every 3 seconds (180 ticks) up to a max
    public int tankShieldRegen = 0;     // points of armor to regen; 0 = off
    private int tankRegenTick = 0;
    private static final int TANK_REGEN_INTERVAL = 180;
    private static final int TANK_MAX_ARMOR = 5;

    // Rogue: Percent chance (0–100) to dodge attacks
    public int rogueEvasionChance = 0;

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

    // ── Constructor ──────────────────────────────────────────────────────────────

    public Player(float startX, float startY, CharacterType type) {
        x = startX; y = startY; character = type;
        switch (type) {
            case SOLDIER -> {
                maxHp = 5; speed = 3f; damage = 1; fireRateTicks = 30;
                soldierExplosiveCharge = SOLDIER_EXPLOSIVE_EVERY; // track 5-shot explosive
            }
            case MAGE -> {
                maxHp = 3; speed = 3.5f; damage = 2; fireRateTicks = 25;
                mageChainLightning = 150; // chain to enemies within 150px
            }
            case TANK -> {
                maxHp = 8; speed = 2f; damage = 1; fireRateTicks = 40;
                tankShieldRegen = 1; // regen 1 armor every 3s
            }
            case ROGUE -> {
                maxHp = 4; speed = 4f; damage = 1; fireRateTicks = 20;
                rogueEvasionChance = 15; // 15% dodge chance
            }
        }
        hp = maxHp;
    }

    // ── Update ───────────────────────────────────────────────────────────────────

    public void update() {
        // Movement
        float moveX = 0, moveY = 0;
        float effectiveSpeed = getEffectiveSpeed();
        if (up)    moveY -= effectiveSpeed;
        if (down)  moveY += effectiveSpeed;
        if (left)  moveX -= effectiveSpeed;
        if (right) moveX += effectiveSpeed;

        if (dashDuration > 0) { x += dashVx; y += dashVy; dashDuration--; }
        else                  { x += moveX;  y += moveY; }

        x = Math.max(0, Math.min(GameFrame.WIDTH  - SIZE, x));
        y = Math.max(55, Math.min(GameFrame.HEIGHT - SIZE, y));

        // Cooldowns
        if (fireCooldown    > 0) fireCooldown--;
        if (dashCooldown    > 0) dashCooldown--;
        if (superCooldown   > 0) superCooldown--;
        if (speedBoostTimer > 0) speedBoostTimer--;
        if (damageBoostTimer> 0) damageBoostTimer--;
        if (shieldTimer     > 0) shieldTimer--;
        if (coinMagnetTimer > 0) coinMagnetTimer--;

        if (superDuration > 0) {
            superDuration--;
            if (superDuration <= 0) superActive = false;
        }

        // HP Regen (e.g. from potion or upgrade)
        if (regenTicksLeft > 0) {
            regenTickInterval++;
            if (regenTickInterval >= REGEN_INTERVAL) {
                regenTickInterval = 0;
                regenTicksLeft--;
                if (hp < maxHp) hp++;
            }
        } else {
            regenTickInterval = 0; // reset so next regen starts cleanly
        }

        // Tank passive: regen 1 armor every TANK_REGEN_INTERVAL ticks
        if (tankShieldRegen > 0 && armor < TANK_MAX_ARMOR) {
            tankRegenTick++;
            if (tankRegenTick >= TANK_REGEN_INTERVAL) {
                tankRegenTick = 0;
                armor = Math.min(TANK_MAX_ARMOR, armor + tankShieldRegen);
            }
        }
    }

    // ── Abilities ────────────────────────────────────────────────────────────────

    public boolean tryDash() {
        if (dashCooldown > 0) return false;
        float dirX = 0, dirY = 0;
        if (up)    dirY -= 1;
        if (down)  dirY += 1;
        if (left)  dirX -= 1;
        if (right) dirX += 1;
        if (dirX == 0 && dirY == 0) return false;

        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        dirX /= len; dirY /= len;
        // Dash speed is fixed — not scaled by current speed buff to avoid abuse
        dashVx = dirX * 12f;
        dashVy = dirY * 12f;
        dashDuration = DASH_DURATION_MAX;
        dashCooldown  = DASH_COOLDOWN_MAX;
        SoundManager.dashAbility();
        return true;
    }

    public boolean trySuperPower() {
        if (superCooldown > 0 || superActive) return false;
        superActive   = true;
        superDuration = SUPER_DURATION_MAX;
        superCooldown = SUPER_COOLDOWN_MAX;
        SoundManager.superAbility(); // was missing
        return true;
    }

    // ── Soldier Explosive Shot Tracking ─────────────────────────────────────────

    /**
     * Call this every time the Soldier fires a bullet.
     * Returns true on every 5th shot (triggers explosion).
     */
    public boolean onSoldierShot() {
        if (character != CharacterType.SOLDIER) return false;
        soldierShotCounter++;
        if (soldierShotCounter >= SOLDIER_EXPLOSIVE_EVERY) {
            soldierShotCounter = 0;
            return true; // caller should spawn explosion
        }
        return false;
    }

    // ── Damage / Combat ──────────────────────────────────────────────────────────

    /**
     * Returns the damage this player deals, including crit rolls, boosts,
     * and character super effects. Call once per shot — crit is randomized.
     */
    public int rollDamage() {
        int d = damage;
        if (damageBoostTimer > 0) d += 2;
        if (superActive && character == CharacterType.MAGE) d *= 2;

        // Crit
        if (critChance > 0 && Math.random() < critChance) d *= 2;
        return d;
    }

    /**
     * @deprecated Use {@link #rollDamage()} for shots; this kept for
     * any callers that need a deterministic (no crit) value.
     */
    @Deprecated
    public int getEffectiveDamage() {
        int d = damage;
        if (damageBoostTimer > 0) d += 2;
        if (superActive && character == CharacterType.MAGE) d *= 2;
        return d;
    }

    public void takeDamage(int dmg) {
        if (isSuperInvincible()) return;
        if (shieldTimer > 0)     return;

        // Rogue passive: evasion dodge
        if (character == CharacterType.ROGUE && rogueEvasionChance > 0) {
            if (Math.random() * 100 < rogueEvasionChance) return; // dodged!
        }

        if (armor > 0) { armor = Math.max(0, armor - dmg); return; }
        hp = Math.max(0, hp - dmg);
    }

    // ── Getters / Helpers ────────────────────────────────────────────────────────

    public boolean isAlive()   { return hp > 0; }
    public boolean isDashing() { return dashDuration > 0; }
    public boolean canShoot()  { return fireCooldown <= 0; }
    public void resetCooldown() { fireCooldown = getEffectiveFireRateTicks(); }

    public int getEffectiveFireRateTicks() {
        if (superActive && character == CharacterType.SOLDIER)
            return Math.max(5, fireRateTicks / 3);
        return fireRateTicks;
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

    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }
    public Rectangle getBounds() {
        return new Rectangle((int)x + 5, (int)y + 5, SIZE - 10, SIZE - 10);
    }

    // ── Drawing ──────────────────────────────────────────────────────────────────

    public void draw(Graphics2D g, double aimAngle,
                     Map<CharacterType, BufferedImage> characterImages) {
        int   px = (int) x, py = (int) y;
        float cx = getCenterX(), cy = getCenterY();

        // Dash ghost trail
        if (isDashing()) {
            for (int i = 1; i <= 3; i++) {
                int tx = (int)(px - dashVx * i * 2);
                int ty = (int)(py - dashVy * i * 2);
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.15f));
                BufferedImage img = characterImages != null
                        ? characterImages.get(character) : null;
                if (img != null) g.drawImage(img, tx, ty, SIZE, SIZE, null);
            }
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1f));
        }

        AffineTransform old = g.getTransform();
        g.rotate(aimAngle, cx, cy);

        BufferedImage img = characterImages != null
                ? characterImages.get(character) : null;
        if (img != null) {
            g.drawImage(img, px, py, SIZE, SIZE, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(px, py, SIZE, SIZE);
        }

        // Dash ring
        if (isDashing()) {
            g.setColor(new Color(0, 255, 255, 120));
            g.drawOval(px - 5, py - 5, SIZE + 10, SIZE + 10);
        }

        // Super glow
        if (superActive) {
            float superAlpha = (float) superDuration / SUPER_DURATION_MAX;
            Color superColor = switch (character) {
                case SOLDIER -> new Color(255, 255,   0, (int)(superAlpha * 150));
                case MAGE    -> new Color(255,   0, 255, (int)(superAlpha * 150));
                case TANK    -> new Color(  0, 255,   0, (int)(superAlpha * 150));
                case ROGUE   -> new Color(255, 165,   0, (int)(superAlpha * 150));
            };
            g.setColor(superColor);
            g.setStroke(new BasicStroke(3.0f));
            int glow = (int)(SIZE * (1f + 0.5f * superAlpha));
            g.drawOval(px - glow / 2 + SIZE / 2,
                       py - glow / 2 + SIZE / 2, glow, glow);
            g.setStroke(new BasicStroke(1));
        }

        // Buff overlays
        if (shieldTimer > 0) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.01) * 0.5 + 0.5);
            g.setColor(new Color(80, 80, 255, (int)(60 + pulse * 60)));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(px - 4, py - 4, SIZE + 8, SIZE + 8);
            g.setStroke(new BasicStroke(1));
        }
        if (speedBoostTimer > 0) {
            g.setColor(new Color(0, 200, 255, 40));
            g.drawOval(px - 8, py - 8, SIZE + 16, SIZE + 16);
        }
        if (damageBoostTimer > 0) {
            g.setColor(new Color(255, 100, 0, 40));
            g.drawOval(px - 6, py - 6, SIZE + 12, SIZE + 12);
        }

        g.setTransform(old);
        drawHPBar(g);
    }

    public void drawHPBar(Graphics2D g) {
        int barW = 60, barH = 8;
        int px = (int)x + 2, py = (int)y - 12;

        g.setColor(Color.RED);
        g.fillRect(px, py, barW, barH);
        g.setColor(Color.GREEN);
        g.fillRect(px, py, (int)((hp / (float)maxHp) * barW), barH);

        // Show armor as a blue overlay segment
        if (armor > 0) {
            g.setColor(new Color(80, 140, 255, 200));
            float armorFraction = Math.min(1f, armor / (float)TANK_MAX_ARMOR);
            g.fillRect(px, py, (int)(armorFraction * barW), barH);
        }

        g.setColor(Color.BLACK);
        g.drawRect(px, py, barW, barH);
    }
}