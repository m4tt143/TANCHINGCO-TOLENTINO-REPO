import java.awt.*;

public class DamageNumber {
    public float x, y, vy;
    public String text;
    public Color color;
    public int life, maxLife;
    public boolean isCrit;

    public DamageNumber(float x, float y, int damage, boolean isCrit) {
        this.x = x; this.y = y;
        this.vy = -1.8f - (float)Math.random();
        this.text = String.valueOf(damage);
        this.isCrit = isCrit;
        this.maxLife = isCrit ? 55 : 38;
        this.life = maxLife;
        this.color = isCrit ? new Color(255, 60, 0) : new Color(255, 255, 220);
    }

    public void update() {
        y += vy;
        vy += 0.09f;
        life--;
    }

    public boolean isDead() { return life <= 0; }

    public void draw(Graphics2D g) {
        float alpha = (float)life / maxLife;
        int a = (int)(alpha * 255);
        int size = isCrit ? 18 : 13;
        g.setFont(new Font("Monospaced", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        g.setColor(new Color(0, 0, 0, a/3));
        g.drawString(text, (int)x - w/2 + 1, (int)y + 1);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
        g.drawString(text, (int)x - w/2, (int)y);
    }
}