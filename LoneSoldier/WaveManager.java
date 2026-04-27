import java.util.List;
import java.util.Random;

/**
 * WaveManager - Endless continuous spawning.
 * Enemies spawn constantly; waves auto-advance every 30 seconds.
 */
public class WaveManager {
    private int   currentWave   = 1;
    private int   spawnCooldown = 40;
    private int   waveTicks     = 0;
    private static final int WAVE_DURATION = 60 * 30; // 30s per wave
    private final Random rand = new Random();

    private float speedBoost = 0f;
    private boolean eliteBoost = false;

    public void setSpeedBoost(float boost) { this.speedBoost = boost; }
    public void setEliteBoost(boolean boost) { this.eliteBoost = boost; }

    public void update(List<Enemy> enemies) {
        waveTicks++;
        if (waveTicks >= WAVE_DURATION) { currentWave++; waveTicks = 0; }
        if (--spawnCooldown <= 0) {
            int batch = 1 + currentWave / 5;
            for (int i = 0; i < batch; i++) spawnEnemy(enemies);
            spawnCooldown = Math.max(12, 55 - currentWave * 3);
        }
    }

    private void spawnEnemy(List<Enemy> enemies) {
        float ex, ey;
        switch (rand.nextInt(4)) {
            case 0 -> { ex = rand.nextFloat() * GameFrame.WIDTH; ey = -Enemy.SIZE - 5; }
            case 1 -> { ex = rand.nextFloat() * GameFrame.WIDTH; ey = GameFrame.HEIGHT + 5; }
            case 2 -> { ex = -Enemy.SIZE - 5; ey = 55 + rand.nextFloat() * (GameFrame.HEIGHT - 60); }
            default->{ ex = GameFrame.WIDTH + 5;  ey = 55 + rand.nextFloat() * (GameFrame.HEIGHT - 60); }
        }
        int   hp    = 1 + (currentWave - 1) / 3;
        float speed = 1.0f + currentWave * 0.10f + speedBoost;
        boolean elite = (currentWave >= 3 || eliteBoost) && rand.nextInt(8) == 0;
        if (elite) { hp *= 3; speed *= 0.75f; }
        enemies.add(new Enemy(ex, ey, hp, speed, elite));
    }

    public int   getCurrentWave()   { return currentWave; }
    public float getWaveProgress()  { return (float) waveTicks / WAVE_DURATION; }
    public void  reset()            { currentWave = 1; waveTicks = 0; spawnCooldown = 40; speedBoost = 0f; eliteBoost = false; }
}