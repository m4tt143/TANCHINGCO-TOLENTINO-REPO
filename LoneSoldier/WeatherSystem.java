import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WeatherSystem {
    private List<float[]> rain = new ArrayList<>();
    private int lightningTimer = 0;
    private int lightningFlash = 0;
    private boolean active = true;
    private final int w, h;

    public WeatherSystem(int w, int h) {
        this.w = w; this.h = h;
        for (int i = 0; i < 180; i++) {
            rain.add(new float[]{
                (float)Math.random() * w,
                (float)Math.random() * h,
                3 + (float)Math.random() * 4,
                5 + (float)Math.random() * 7
            });
        }
    }

    public void update() {
        if (!active) return;
        for (float[] d : rain) {
            d[1] += d[3];
            d[0] -= d[3] * 0.25f;
            if (d[1] > h) { d[1] = -10; d[0] = (float)Math.random() * w; }
            if (d[0] < -10) d[0] = w + 10;
        }
        if (lightningTimer-- <= 0) {
            if (Math.random() < 0.004) {
                lightningFlash = 6 + (int)(Math.random() * 5);
                lightningTimer = 400 + (int)(Math.random() * 800);
            }
        }
        if (lightningFlash > 0) lightningFlash--;
    }

    public void draw(Graphics2D g) {
        if (!active) return;
        g.setColor(new Color(140, 150, 190, 35));
        for (float[] d : rain) {
            g.drawLine((int)d[0], (int)d[1], (int)(d[0] - d[3]*0.25f), (int)(d[1] + d[2]));
        }
        if (lightningFlash > 0) {
            int a = lightningFlash * 35;
            g.setColor(new Color(230, 240, 255, a));
            g.fillRect(0, 0, w, h);
            if (lightningFlash > 3) {
                g.setColor(new Color(255, 255, 255, 220));
                int lx = w/2 + (int)((Math.random()-0.5)*300);
                int ly = 0;
                while (ly < h) {
                    int nx = lx + (int)((Math.random()-0.5)*50);
                    int ny = ly + 25 + (int)(Math.random()*35);
                    g.drawLine(lx, ly, nx, ny);
                    lx = nx; ly = ny;
                }
            }
        }
    }

    public boolean isFlashing() { return lightningFlash > 0; }
    public void setActive(boolean a) { this.active = a; }
}