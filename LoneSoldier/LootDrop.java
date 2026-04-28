import java.awt.*;

public class LootDrop {
    public float x, y;
    public enum Type { HEALTH, SPEED, DAMAGE, SHIELD, NUKE, MAGNET }
    public Type type;
    public int life = 900; // 15 seconds
    public float bobOffset;

    public LootDrop(float x, float y, Type type) {
        this.x = x; this.y = y; this.type = type;
        this.bobOffset = (float)(Math.random() * Math.PI * 2);
    }

    public void update() { life--; bobOffset += 0.08f; }
    public boolean isExpired() { return life <= 0; }
    public Rectangle getBounds() { return new Rectangle((int)x-14, (int)y-14, 28, 28); }

    public void draw(Graphics2D g) {
        float bob = (float)Math.sin(bobOffset) * 4;
        int dy = (int)(y + bob);
        g.setColor(getGlowColor());
        g.fillOval((int)x-18, dy-18, 36, 36);
        g.setColor(getColor());
        g.fillOval((int)x-11, dy-11, 22, 22);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        String icon = getIcon();
        g.drawString(icon, (int)x - fm.stringWidth(icon)/2, dy + 5);
    }

    public Color getColor() {
        return switch(type) {
            case HEALTH -> new Color(255, 50, 50);
            case SPEED -> new Color(0, 200, 255);
            case DAMAGE -> new Color(255, 100, 0);
            case SHIELD -> new Color(80, 80, 255);
            case NUKE -> new Color(255, 40, 255);
            case MAGNET -> new Color(255, 215, 0);
        };
    }

    private Color getGlowColor() {
        Color c = getColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 50);
    }

    private String getIcon() {
        return switch(type) {
            case HEALTH -> "+";
            case SPEED -> "»";
            case DAMAGE -> "x";
            case SHIELD -> "O";
            case NUKE -> "*";
            case MAGNET -> "$";
        };
    }
}