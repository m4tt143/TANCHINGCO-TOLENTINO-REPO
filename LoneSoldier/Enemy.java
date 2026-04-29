import java.awt.*;
import java.util.Random;

public class Enemy {
    public float x, y;
    public static int SIZE = 26;

    public int hp, maxHp;
    public float speed;
    public boolean elite;
    public int coinDrop;
    public EnemyType type = EnemyType.ZOMBIE;

    // Behavior state
    public int shootCooldown = 0;
    public boolean isCharging = false;
    public int chargeCooldown = 0;
    public float chargeVx, chargeVy;
    public int explodeTimer = -1;
    private boolean explodeSignaled = false; // prevents double-trigger

    // Status effects
    public int slowTimer = 0;       // ticks remaining of slow
    public float slowFactor = 1f;   // multiplied into speed (< 1 = slower)
    public int poisonTicks = 0;     // remaining DOT ticks (1 dmg each)
    private int poisonInterval = 0; // tick counter between poison hits

    // Tank passive: knockback resistance
    private static final float TANK_KNOCKBACK_RESIST = 0.35f;

    // Boss phase 2 (triggered at 50% HP)
    private boolean bossPhase2 = false;

    private Color skinColor;
    private Color shirtColor;
    private int woundType;

    public float spawnScale = 0f;
    public int hitFlash = 0;
    public float knockbackX = 0, knockbackY = 0;

    private static final Random rand = new Random();

    // ── Constructors ─────────────────────────────────────────────────────────────

    /** Legacy constructor — defaults to ZOMBIE type */
    public Enemy(float x, float y, int hp, float speed, boolean elite) {
        this(x, y, hp, speed, elite, EnemyType.ZOMBIE);
    }

    public Enemy(float x, float y, int hp, float speed, boolean elite, EnemyType type) {
        this.x = x; this.y = y;
        this.type = type;
        this.maxHp = (int)(hp * type.hpMult);
        this.hp    = this.maxHp;
        this.speed = speed * type.speedMult; // FIX: speed is now properly stored
        this.elite = elite;
        this.coinDrop = (elite ? 4 : 1) + (type.isBoss ? 10 : 0) + rand.nextInt(3);
        this.woundType = rand.nextInt(3);

        // Appearance
        if (elite && !type.isBoss) {
            skinColor  = new Color(130 + rand.nextInt(30), 80 + rand.nextInt(20), 160 + rand.nextInt(30));
            shirtColor = new Color(60, 20, 80);
        } else {
            skinColor  = new Color(80 + rand.nextInt(40), 110 + rand.nextInt(35), 50 + rand.nextInt(30));
            shirtColor = new Color(55 + rand.nextInt(30), 45 + rand.nextInt(20), 35 + rand.nextInt(20));
        }

        // Type color overrides
        switch (type) {
            case RUNNER   -> { skinColor = new Color(210 + rand.nextInt(45), 60 + rand.nextInt(40), 40 + rand.nextInt(30));  shirtColor = new Color(130, 40, 25); }
            case TANK     -> { skinColor = new Color(45, 50, 65 + rand.nextInt(30));    shirtColor = new Color(30, 33, 45); }
            case RANGED   -> { skinColor = new Color(50, 120 + rand.nextInt(40), 100 + rand.nextInt(30)); shirtColor = new Color(20, 65, 50); }
            case EXPLODER -> { skinColor = new Color(210 + rand.nextInt(45), 190 + rand.nextInt(50), 30 + rand.nextInt(40)); shirtColor = new Color(170, 150, 20); }
            case BOSS     -> { skinColor = new Color(90, 15, 15); shirtColor = new Color(50, 8, 8); }
            default -> {}
        }

        if (type == EnemyType.RANGED)  shootCooldown = 50 + rand.nextInt(60);
        if (type == EnemyType.RUNNER)  chargeCooldown = 100 + rand.nextInt(80);
    }

    // ── Update ───────────────────────────────────────────────────────────────────

    /**
     * @param targetX   player center X
     * @param targetY   player center Y
     * @param moveSpeed wave-difficulty speed multiplier (1.0 = normal)
     *                  Combined with this.speed (which already has type scaling)
     */
    public void update(float targetX, float targetY, float moveSpeed) {
        // Spawn-in scale animation
        if (spawnScale < 1f) spawnScale = Math.min(1f, spawnScale + 0.06f);

        // Hit flash
        if (hitFlash > 0) hitFlash--;

        // Knockback (Tank resists more)
        float kbResist = (type == EnemyType.TANK) ? TANK_KNOCKBACK_RESIST : 1f;
        x += knockbackX * kbResist;
        y += knockbackY * kbResist;
        knockbackX *= 0.85f;
        knockbackY *= 0.85f;

        // Slow effect
        if (slowTimer > 0) {
            slowTimer--;
            if (slowTimer == 0) slowFactor = 1f; // restore speed when expired
        }

        // Poison DOT
        if (poisonTicks > 0) {
            poisonInterval++;
            if (poisonInterval >= 30) { // deal 1 dmg every 30 ticks (0.5s)
                poisonInterval = 0;
                poisonTicks--;
                hp = Math.max(0, hp - 1);
                hitFlash = 4;
            }
        }

        // FIX: effective speed now uses this.speed (type-scaled) × wave multiplier × slow
        float effectiveSpeed = this.speed * moveSpeed * slowFactor;

        // Ranged: decrement shoot cooldown here (was missing)
        if (type == EnemyType.RANGED && shootCooldown > 0) shootCooldown--;

        // Boss phase 2 trigger at 50% HP
        if (type == EnemyType.BOSS && !bossPhase2 && hp <= maxHp / 2) {
            bossPhase2 = true;
            hitFlash = 15; // flash to signal phase change
        }

        // Type-specific AI
        switch (type) {
            case BOSS    -> updateBoss(targetX, targetY, effectiveSpeed);
            case RANGED  -> updateRanged(targetX, targetY, effectiveSpeed);
            case RUNNER  -> updateRunner(targetX, targetY, effectiveSpeed);
            case EXPLODER-> updateExploder(targetX, targetY, effectiveSpeed);
            case TANK    -> updateTank(targetX, targetY, effectiveSpeed);
            default      -> chaseTarget(targetX, targetY, effectiveSpeed);
        }
    }

    // ── AI Behaviours ─────────────────────────────────────────────────────────────

    private void updateBoss(float tx, float ty, float spd) {
        float dx = tx - getCenterX(), dy = ty - getCenterY();
        float dist = dist(dx, dy);
        if (dist == 0) return;

        float phase2Mult = bossPhase2 ? 1.6f : 1f;

        if (dist > 280) {
            // Approach
            x += (dx / dist) * spd * 0.35f * phase2Mult;
            y += (dy / dist) * spd * 0.35f * phase2Mult;
        } else if (dist < 200) {
            // Retreat — boss keeps distance (phase 2: closer)
            float minDist = bossPhase2 ? 140 : 200;
            if (dist < minDist) {
                x -= (dx / dist) * spd * 0.25f;
                y -= (dy / dist) * spd * 0.25f;
            }
        }
    }

    private void updateRanged(float tx, float ty, float spd) {
        float dx = tx - getCenterX(), dy = ty - getCenterY();
        float dist = dist(dx, dy);
        if (dist == 0) return;
        if (dist > 320) {
            x += (dx / dist) * spd * 0.55f;
            y += (dy / dist) * spd * 0.55f;
        } else if (dist < 220) {
            x -= (dx / dist) * spd * 0.45f;
            y -= (dy / dist) * spd * 0.45f;
        }
    }

    private void updateRunner(float tx, float ty, float spd) {
        float dx = tx - getCenterX(), dy = ty - getCenterY();
        float dist = dist(dx, dy);
        if (isCharging) {
            x += chargeVx; y += chargeVy;
            chargeVx *= 0.92f; chargeVy *= 0.92f;
            if (Math.abs(chargeVx) < 0.6f && Math.abs(chargeVy) < 0.6f) isCharging = false;
        } else {
            if (dist > 0) {
                x += (dx / dist) * spd;
                y += (dy / dist) * spd;
            }
            chargeCooldown--;
            if (chargeCooldown <= 0 && dist < 380 && dist > 0) {
                chargeCooldown = 140 + rand.nextInt(90);
                isCharging = true;
                chargeVx = (dx / dist) * 15f;
                chargeVy = (dy / dist) * 15f;
            }
        }
    }

    private void updateExploder(float tx, float ty, float spd) {
        float dx = tx - getCenterX(), dy = ty - getCenterY();
        float dist = dist(dx, dy);
        if (dist > 0) {
            float rushSpd = dist < 90 ? spd * 2.0f : spd * 1.3f;
            x += (dx / dist) * rushSpd;
            y += (dy / dist) * rushSpd;
        }
        if (dist < 75 && explodeTimer < 0) explodeTimer = 90;
        if (explodeTimer > 0) {
            explodeTimer--;
            if (explodeTimer % 8 < 4) hitFlash = 3;
        }
    }

    /** Tank: slow, heavy — walks straight at the player, ignores knockback */
    private void updateTank(float tx, float ty, float spd) {
        // Tanks are slower but unstoppable — knockback resist handled in update()
        chaseTarget(tx, ty, spd * 0.75f);
    }

    /** Default chase: walk straight toward target */
    private void chaseTarget(float tx, float ty, float spd) {
        float dx = tx - getCenterX(), dy = ty - getCenterY();
        float dist = dist(dx, dy);
        if (dist > 0) {
            x += (dx / dist) * spd;
            y += (dy / dist) * spd;
        }
    }

    // ── Public State Queries ──────────────────────────────────────────────────────

    public boolean isAlive() { return hp > 0; }

    /**
     * Returns true exactly once when the exploder's fuse reaches zero.
     * Call this every tick from your game loop to detect detonation.
     */
    public boolean shouldExplode() {
        if (explodeTimer == 0 && !explodeSignaled) {
            explodeSignaled = true;
            return true;
        }
        return false;
    }

    /**
     * Apply a slow to this enemy.
     * @param factor    speed fraction (e.g. 0.4 = 40% of normal speed)
     * @param durationTicks how many ticks the slow lasts
     */
    public void applySlow(float factor, int durationTicks) {
        slowFactor = Math.min(slowFactor, factor); // take the stronger slow
        slowTimer  = Math.max(slowTimer, durationTicks);
    }

    /**
     * Apply poison DOT to this enemy.
     * @param ticks number of 1-damage poison hits (1 per 30 ticks)
     */
    public void applyPoison(int ticks) {
        poisonTicks = Math.max(poisonTicks, ticks);
    }

    public boolean isBossPhase2() { return bossPhase2; }

    // ── Geometry ─────────────────────────────────────────────────────────────────

    public float getCenterX() { return x + getSize() / 2f; }
    public float getCenterY() { return y + getSize() / 2f; }

    public int getSize() {
        if (type == EnemyType.BOSS)  return SIZE + 30;
        if (type == EnemyType.TANK)  return SIZE + 10;
        if (elite)                   return SIZE + 10;
        return SIZE;
    }

    public Rectangle getBounds() {
        int s = getSize();
        return new Rectangle((int)x + 2, (int)y + 2, s - 4, s - 4);
    }

    private static float dist(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ── Drawing ───────────────────────────────────────────────────────────────────

    public void draw(Graphics2D g) {
        if (spawnScale <= 0.01f) return;
        Graphics2D eg = (Graphics2D) g.create();
        float cx = x + getSize() / 2f;
        float cy = y + getSize() / 2f;
        eg.translate(cx, cy);
        eg.scale(spawnScale, spawnScale);
        eg.translate(-cx, -cy);
        g = eg;

        int s  = getSize();
        int ex = (int) x, ey = (int) y;

        // Slow effect: blue tint overlay
        if (slowTimer > 0) {
            g.setColor(new Color(100, 180, 255, 70));
            g.fillOval(ex - 6, ey - 6, s + 12, s + 12);
        }

        // Poison effect: green tint overlay
        if (poisonTicks > 0) {
            g.setColor(new Color(80, 220, 60, 60));
            g.fillOval(ex - 4, ey - 4, s + 8, s + 8);
        }

        // Exploder fuse warning
        if (type == EnemyType.EXPLODER && explodeTimer > 0) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.02) * 0.5 + 0.5);
            g.setColor(new Color(255, 40 + (int)(pulse * 120), 0, (int)(pulse * 180)));
            g.fillOval(ex - 10, ey - 10, s + 20, s + 20);
        }

        // Boss phase 2 glow — red pulsing ring
        if (bossPhase2) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.015) * 0.5 + 0.5);
            g.setColor(new Color(255, 0, 0, (int)(60 + pulse * 80)));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(ex - 8, ey - 8, s + 16, s + 16);
            g.setStroke(new BasicStroke(1f));
        }

        Color glowColor = elite             ? new Color(180,  60, 255,  60)
                        : type == EnemyType.BOSS     ? new Color(255,  30,  30,  70)
                        : type == EnemyType.EXPLODER ? new Color(255, 180,   0,  55)
                        : new Color(60, 200, 60, 50);
        g.setColor(glowColor);
        g.fillOval(ex - 5, ey - 5, s + 10, s + 10);

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(ex + 2, ey + s - 4, s - 4, 6);

        // Legs
        Color pantsColor = new Color(
            Math.max(0, shirtColor.getRed()   - 15),
            Math.max(0, shirtColor.getGreen() - 10),
            Math.max(0, shirtColor.getBlue()  - 10)
        );
        g.setColor(pantsColor);
        g.fillRect(ex + s/2 - 8, ey + s - 6, 5, 8);
        g.fillRect(ex + s/2 + 3, ey + s - 6, 5, 8);
        g.setColor(new Color(25, 20, 15));
        g.fillRect(ex + s/2 - 9, ey + s + 1, 6, 3);
        g.fillRect(ex + s/2 + 2, ey + s + 1, 6, 3);

        // Body / shirt
        g.setColor(shirtColor);
        g.fillRect(ex + 3, ey + s/2, s - 6, s/2 - 2);
        g.setColor(new Color(
            Math.max(0, shirtColor.getRed()   - 30),
            Math.max(0, shirtColor.getGreen() - 20),
            Math.max(0, shirtColor.getBlue()  - 15)
        ));
        g.fillRect(ex + 5,     ey + s/2 + 2, 3, 4);
        g.fillRect(ex + s - 9, ey + s/2 + 5, 3, 5);
        g.setColor(skinColor);
        g.fillRect(ex + 5,     ey + s/2 + 2, 2, 3);
        g.fillRect(ex + s - 9, ey + s/2 + 5, 2, 4);

        // Arms
        g.setColor(skinColor);
        g.fillRect(ex - 4, ey + s/2 + 1, 6, 4);
        g.fillRect(ex + s - 2, ey + s/2 + 1, 6, 4);
        Color clawColor = new Color(
            Math.max(0, skinColor.getRed()   - 15),
            Math.min(255, skinColor.getGreen() + 10),
            Math.max(0, skinColor.getBlue()  -  5)
        );
        g.setColor(clawColor);
        g.fillRect(ex - 7, ey + s/2,     2, 2);
        g.fillRect(ex - 7, ey + s/2 + 2, 2, 2);
        g.fillRect(ex - 7, ey + s/2 + 4, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2,     2, 2);
        g.fillRect(ex + s + 5, ey + s/2 + 2, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2 + 4, 2, 2);

        // Neck + head
        g.setColor(skinColor);
        g.fillRect(ex + s/2 - 4, ey + s/2 - 3, 8, 5);
        g.fillOval(ex + 3, ey, s - 6, s/2 + 4);

        // Boss crown
        if (type == EnemyType.BOSS) {
            g.setColor(bossPhase2 ? new Color(220, 0, 0) : new Color(180, 30, 30));
            int[] xp = { ex + s/2, ex + s/2 - 10, ex + s/2 + 10 };
            int[] yp = { ey - 8, ey + 8, ey + 8 };
            g.fillPolygon(xp, yp, 3);
            g.setColor(bossPhase2 ? new Color(255, 80, 80) : new Color(255, 60, 60));
            g.fillOval(ex + s/2 - 3, ey - 4, 6, 6);
        }

        // Hair / top of head
        g.setColor(new Color(25, 18, 8));
        g.fillOval(ex + 3, ey, s - 6, s/4 + 1);
        g.fillOval(ex + 2, ey + 1, 5, 4);
        g.fillOval(ex + s - 8, ey + 1, 5, 4);
        g.fillOval(ex + s/2 - 4, ey - 2, 7, 4);
        g.setColor(skinColor);
        g.fillRect(ex + 5, ey + s/4 - 1, s - 10, 5);

        // Eyes
        Color eyeGlow = elite             ? new Color(210, 100, 255, 220)
                      : type == EnemyType.BOSS     ? (bossPhase2 ? new Color(255, 20, 20, 255) : new Color(255, 60, 60, 240))
                      : type == EnemyType.EXPLODER ? new Color(255, 200, 40, 230)
                      : new Color(150, 255, 50, 220);
        g.setColor(eyeGlow);
        g.fillOval(ex + 5,      ey + s/4, 6, 5);
        g.fillOval(ex + s - 12, ey + s/4, 6, 5);
        g.setColor(new Color(5, 10, 0));
        g.fillOval(ex + 7,      ey + s/4 + 1, 2, 3);
        g.fillOval(ex + s - 10, ey + s/4 + 1, 2, 3);

        // Mouth
        g.setColor(new Color(15, 5, 5));
        g.fillRect(ex + s/2 - 6, ey + s/2 - 2, 12, 3);
        g.setColor(new Color(210, 200, 170));
        g.fillRect(ex + s/2 - 5, ey + s/2 - 2, 2, 2);
        g.fillRect(ex + s/2 - 1, ey + s/2 - 2, 2, 2);
        g.fillRect(ex + s/2 + 3, ey + s/2 - 2, 2, 2);

        // Wound
        g.setColor(new Color(150, 8, 8, 190));
        if      (woundType == 0) g.fillOval(ex + s/2 - 2, ey + s/2 - 4, 5, 3);
        else if (woundType == 1) g.fillRect(ex + s/2 - 4, ey + s/4 - 1, 7, 2);
        else                     g.fillOval(ex + s/2 - 3, ey + s/2 + 4, 6, 5);

        // Elite / boss ring
        if (elite || type == EnemyType.BOSS) {
            g.setColor(elite ? new Color(180, 60, 255, 100) : new Color(255, 40, 40, 110));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(ex - 3, ey - 3, s + 6, s + 6);
            g.setStroke(new BasicStroke(1f));
        }

        // HP bar
        if (hp < maxHp) {
            g.setColor(new Color(20, 20, 20, 180));
            g.fillRect(ex, ey - 8, s, 4);
            float ratio = (float) hp / maxHp;
            Color hpc = elite             ? new Color(160,  50, 220)
                      : type == EnemyType.BOSS ? (bossPhase2 ? new Color(255, 20, 20) : new Color(255, 40, 40))
                      : new Color(80, 220, 80);
            g.setColor(hpc);
            g.fillRect(ex, ey - 8, (int)(s * ratio), 4);
        }

        // Hit flash
        if (hitFlash > 0) {
            g.setColor(new Color(255, 255, 255, hitFlash * 55));
            g.fillOval(ex - 4, ey - 4, s + 8, s + 8);
        }

        eg.dispose();
    }
}