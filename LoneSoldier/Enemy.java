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

    private Color skinColor;
    private Color shirtColor;
    private int woundType;

    public float spawnScale = 0f;
    public int hitFlash = 0;
    public float knockbackX = 0, knockbackY = 0;

    private static final Random rand = new Random();

    // Legacy constructor
    public Enemy(float x, float y, int hp, float speed, boolean elite) {
        this(x, y, hp, speed, elite, EnemyType.ZOMBIE);
    }

    public Enemy(float x, float y, int hp, float speed, boolean elite, EnemyType type) {
        this.x = x; this.y = y;
        this.type = type;
        this.maxHp = (int)(hp * type.hpMult);
        this.hp = this.maxHp;
        this.speed = speed * type.speedMult;
        this.elite = elite;
        this.coinDrop = (elite ? 4 : 1) + (type.isBoss ? 10 : 0) + rand.nextInt(3);
        this.woundType = rand.nextInt(3);

        if (elite && !type.isBoss) {
            skinColor = new Color(130 + rand.nextInt(30), 80 + rand.nextInt(20), 160 + rand.nextInt(30));
            shirtColor = new Color(60, 20, 80);
        } else {
            skinColor = new Color(80 + rand.nextInt(40), 110 + rand.nextInt(35), 50 + rand.nextInt(30));
            shirtColor = new Color(55 + rand.nextInt(30), 45 + rand.nextInt(20), 35 + rand.nextInt(20));
        }

        // Type color overrides
        if (type == EnemyType.RUNNER) {
            skinColor = new Color(210 + rand.nextInt(45), 60 + rand.nextInt(40), 40 + rand.nextInt(30));
            shirtColor = new Color(130, 40, 25);
        } else if (type == EnemyType.TANK) {
            skinColor = new Color(45, 50, 65 + rand.nextInt(30));
            shirtColor = new Color(30, 33, 45);
        } else if (type == EnemyType.RANGED) {
            skinColor = new Color(50, 120 + rand.nextInt(40), 100 + rand.nextInt(30));
            shirtColor = new Color(20, 65, 50);
        } else if (type == EnemyType.EXPLODER) {
            skinColor = new Color(210 + rand.nextInt(45), 190 + rand.nextInt(50), 30 + rand.nextInt(40));
            shirtColor = new Color(170, 150, 20);
        } else if (type == EnemyType.BOSS) {
            skinColor = new Color(90, 15, 15);
            shirtColor = new Color(50, 8, 8);
        }

        if (type == EnemyType.RANGED) shootCooldown = 50 + rand.nextInt(60);
        if (type == EnemyType.RUNNER) chargeCooldown = 100 + rand.nextInt(80);
    }

    public void update(float targetX, float targetY, float moveSpeed) {
        if (spawnScale < 1f) spawnScale += 0.06f;
        if (hitFlash > 0) hitFlash--;
        x += knockbackX; y += knockbackY;
        knockbackX *= 0.85f; knockbackY *= 0.85f;

        if (type == EnemyType.BOSS) {
            float dx = targetX - getCenterX();
            float dy = targetY - getCenterY();
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if (dist > 280) {
                x += (dx/dist) * moveSpeed * 0.35f;
                y += (dy/dist) * moveSpeed * 0.35f;
            } else if (dist < 200 && dist > 0) {
                x -= (dx/dist) * moveSpeed * 0.25f;
                y -= (dy/dist) * moveSpeed * 0.25f;
            }
        } else if (type == EnemyType.RANGED) {
            float dx = targetX - getCenterX();
            float dy = targetY - getCenterY();
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if (dist > 320 && dist > 0) {
                x += (dx/dist) * moveSpeed * 0.55f;
                y += (dy/dist) * moveSpeed * 0.55f;
            } else if (dist < 220 && dist > 0) {
                x -= (dx/dist) * moveSpeed * 0.45f;
                y -= (dy/dist) * moveSpeed * 0.45f;
            }
        } else if (type == EnemyType.RUNNER) {
            if (isCharging) {
                x += chargeVx; y += chargeVy;
                chargeVx *= 0.92f; chargeVy *= 0.92f;
                if (Math.abs(chargeVx) < 0.6f && Math.abs(chargeVy) < 0.6f) isCharging = false;
            } else {
                float dx = targetX - getCenterX();
                float dy = targetY - getCenterY();
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                if (dist > 0) {
                    x += (dx/dist) * moveSpeed;
                    y += (dy/dist) * moveSpeed;
                }
                if (chargeCooldown-- <= 0 && dist < 380) {
                    chargeCooldown = 140 + rand.nextInt(90);
                    isCharging = true;
                    chargeVx = (dx/dist) * 15f;
                    chargeVy = (dy/dist) * 15f;
                }
            }
        } else if (type == EnemyType.EXPLODER) {
            float dx = targetX - getCenterX();
            float dy = targetY - getCenterY();
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if (dist > 0) {
                float spd = dist < 90 ? moveSpeed * 2.0f : moveSpeed * 1.3f;
                x += (dx/dist) * spd;
                y += (dy/dist) * spd;
            }
            if (dist < 75 && explodeTimer < 0) explodeTimer = 90;
            if (explodeTimer > 0) {
                explodeTimer--;
                if (explodeTimer % 8 < 4) hitFlash = 3;
            }
        } else {
            float dx = targetX - getCenterX();
            float dy = targetY - getCenterY();
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if (dist > 0) {
                x += (dx/dist) * moveSpeed;
                y += (dy/dist) * moveSpeed;
            }
        }
    }

    public float getCenterX() { return x + getSize() / 2f; }
    public float getCenterY() { return y + getSize() / 2f; }
    public int getSize() {
        if (type == EnemyType.BOSS) return SIZE + 30;
        if (type == EnemyType.TANK) return SIZE + 10;
        if (elite) return SIZE + 10;
        return SIZE;
    }

    public Rectangle getBounds() {
        int s = getSize();
        return new Rectangle((int)x + 2, (int)y + 2, s - 4, s - 4);
    }

    public void draw(Graphics2D g) {
        if (spawnScale <= 0.01f) return;
        Graphics2D eg = (Graphics2D) g.create();
        float cx = x + getSize() / 2f;
        float cy = y + getSize() / 2f;
        eg.translate(cx, cy);
        eg.scale(spawnScale, spawnScale);
        eg.translate(-cx, -cy);
        g = eg;

        int s = getSize();
        int ex = (int)x, ey = (int)y;

        // Exploder fuse warning
        if (type == EnemyType.EXPLODER && explodeTimer > 0) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.02) * 0.5 + 0.5);
            g.setColor(new Color(255, 40 + (int)(pulse*120), 0, (int)(pulse*180)));
            g.fillOval(ex - 10, ey - 10, s + 20, s + 20);
        }

        Color glowColor = elite ? new Color(180, 60, 255, 60)
            : type == EnemyType.BOSS ? new Color(255, 30, 30, 70)
            : type == EnemyType.EXPLODER ? new Color(255, 180, 0, 55)
            : new Color(60, 200, 60, 50);
        g.setColor(glowColor);
        g.fillOval(ex - 5, ey - 5, s + 10, s + 10);

        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(ex + 2, ey + s - 4, s - 4, 6);

        Color pantsColor = new Color(
            Math.max(0, shirtColor.getRed() - 15),
            Math.max(0, shirtColor.getGreen() - 10),
            Math.max(0, shirtColor.getBlue() - 10)
        );
        g.setColor(pantsColor);
        g.fillRect(ex + s/2 - 8, ey + s - 6, 5, 8);
        g.fillRect(ex + s/2 + 3, ey + s - 6, 5, 8);
        g.setColor(new Color(25, 20, 15));
        g.fillRect(ex + s/2 - 9, ey + s + 1, 6, 3);
        g.fillRect(ex + s/2 + 2, ey + s + 1, 6, 3);

        g.setColor(shirtColor);
        g.fillRect(ex + 3, ey + s/2, s - 6, s/2 - 2);
        g.setColor(new Color(
            Math.max(0, shirtColor.getRed() - 30),
            Math.max(0, shirtColor.getGreen() - 20),
            Math.max(0, shirtColor.getBlue() - 15)
        ));
        g.fillRect(ex + 5, ey + s/2 + 2, 3, 4);
        g.fillRect(ex + s - 9, ey + s/2 + 5, 3, 5);
        g.setColor(skinColor);
        g.fillRect(ex + 5, ey + s/2 + 2, 2, 3);
        g.fillRect(ex + s - 9, ey + s/2 + 5, 2, 4);

        g.setColor(skinColor);
        g.fillRect(ex - 4, ey + s/2 + 1, 6, 4);
        g.fillRect(ex + s - 2, ey + s/2 + 1, 6, 4);
        Color clawColor = new Color(
            Math.max(0, skinColor.getRed() - 15),
            Math.min(255, skinColor.getGreen() + 10),
            Math.max(0, skinColor.getBlue() - 5)
        );
        g.setColor(clawColor);
        g.fillRect(ex - 7, ey + s/2, 2, 2);
        g.fillRect(ex - 7, ey + s/2 + 2, 2, 2);
        g.fillRect(ex - 7, ey + s/2 + 4, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2 + 2, 2, 2);
        g.fillRect(ex + s + 5, ey + s/2 + 4, 2, 2);

        g.setColor(skinColor);
        g.fillRect(ex + s/2 - 4, ey + s/2 - 3, 8, 5);
        g.setColor(skinColor);
        g.fillOval(ex + 3, ey, s - 6, s/2 + 4);

        // Boss crown
        if (type == EnemyType.BOSS) {
            g.setColor(new Color(180, 30, 30));
            int[] xp = {ex + s/2, ex + s/2 - 10, ex + s/2 + 10};
            int[] yp = {ey - 8, ey + 8, ey + 8};
            g.fillPolygon(xp, yp, 3);
            g.setColor(new Color(255, 60, 60));
            g.fillOval(ex + s/2 - 3, ey - 4, 6, 6);
        }

        g.setColor(new Color(25, 18, 8));
        g.fillOval(ex + 3, ey, s - 6, s/4 + 1);
        g.fillOval(ex + 2, ey + 1, 5, 4);
        g.fillOval(ex + s - 8, ey + 1, 5, 4);
        g.fillOval(ex + s/2 - 4, ey - 2, 7, 4);
        g.setColor(skinColor);
        g.fillRect(ex + 5, ey + s/4 - 1, s - 10, 5);

        Color eyeGlow = elite ? new Color(210, 100, 255, 220)
            : type == EnemyType.BOSS ? new Color(255, 60, 60, 240)
            : type == EnemyType.EXPLODER ? new Color(255, 200, 40, 230)
            : new Color(150, 255, 50, 220);
        g.setColor(eyeGlow);
        g.fillOval(ex + 5, ey + s/4, 6, 5);
        g.fillOval(ex + s - 12, ey + s/4, 6, 5);
        g.setColor(new Color(5, 10, 0));
        g.fillOval(ex + 7, ey + s/4 + 1, 2, 3);
        g.fillOval(ex + s - 10, ey + s/4 + 1, 2, 3);

        g.setColor(new Color(15, 5, 5));
        g.fillRect(ex + s/2 - 6, ey + s/2 - 2, 12, 3);
        g.setColor(new Color(210, 200, 170));
        g.fillRect(ex + s/2 - 5, ey + s/2 - 2, 2, 2);
        g.fillRect(ex + s/2 - 1, ey + s/2 - 2, 2, 2);
        g.fillRect(ex + s/2 + 3, ey + s/2 - 2, 2, 2);

        g.setColor(new Color(150, 8, 8, 190));
        if (woundType == 0) g.fillOval(ex + s/2 - 2, ey + s/2 - 4, 5, 3);
        else if (woundType == 1) g.fillRect(ex + s/2 - 4, ey + s/4 - 1, 7, 2);
        else g.fillOval(ex + s/2 - 3, ey + s/2 + 4, 6, 5);

        if (elite || type == EnemyType.BOSS) {
            g.setColor(elite ? new Color(180, 60, 255, 100) : new Color(255, 40, 40, 110));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(ex - 3, ey - 3, s + 6, s + 6);
            g.setStroke(new BasicStroke(1f));
        }

        if (hp < maxHp) {
            g.setColor(new Color(20, 20, 20, 180));
            g.fillRect(ex, ey - 8, s, 4);
            float ratio = (float) hp / maxHp;
            Color hpc = elite ? new Color(160, 50, 220)
                : type == EnemyType.BOSS ? new Color(255, 40, 40)
                : new Color(80, 220, 80);
            g.setColor(hpc);
            g.fillRect(ex, ey - 8, (int)(s * ratio), 4);
        }

        if (hitFlash > 0) {
            g.setColor(new Color(255, 255, 255, hitFlash * 55));
            g.fillOval(ex - 4, ey - 4, s + 8, s + 8);
        }

        eg.dispose();
    }
}