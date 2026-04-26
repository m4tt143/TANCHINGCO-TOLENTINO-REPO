import java.util.List;
import java.util.Random;

/**
 * WaveManager - Controls enemy spawning per wave.
 * Each wave has more enemies that are faster and tougher.
 */
public class WaveManager {

    private int  currentWave    = 1;
    private int  enemiesToSpawn = 0;
    private int  spawnCooldown  = 0;
    private boolean allSpawned  = false;

    // Ticks between each enemy spawn (decreases per wave)
    private static final int BASE_INTERVAL = 50;

    private final Random rand = new Random();

    public WaveManager() {
        prepareWave();
    }

    /** Set up the enemy count and reset spawn state for current wave. */
    private void prepareWave() {
        // Wave 1 = 8 enemies, each wave +5 more
        enemiesToSpawn = 8 + (currentWave - 1) * 5;
        allSpawned     = false;
        spawnCooldown  = 40;  // Short delay before first enemy
    }

    /**
     * Called every game tick. Spawns enemies at timed intervals.
     * @param enemies  The shared enemy list to add new enemies into.
     */
    public void update(List<Enemy> enemies) {
        if (allSpawned) return;

        spawnCooldown--;
        if (spawnCooldown <= 0) {
            spawnEnemy(enemies);
            enemiesToSpawn--;

            // Faster spawning as waves progress
            int interval = Math.max(18, BASE_INTERVAL - (currentWave * 3));
            spawnCooldown = interval;

            if (enemiesToSpawn <= 0) {
                allSpawned = true;
            }
        }
    }

    /** Spawn one enemy at a random edge of the screen. */
    private void spawnEnemy(List<Enemy> enemies) {
        float ex, ey;
        int side = rand.nextInt(4);

        switch (side) {
            case 0 -> { // Top
                ex = rand.nextFloat() * GameFrame.WIDTH;
                ey = -Enemy.SIZE - 5;
            }
            case 1 -> { // Bottom
                ex = rand.nextFloat() * GameFrame.WIDTH;
                ey = GameFrame.HEIGHT + 5;
            }
            case 2 -> { // Left
                ex = -Enemy.SIZE - 5;
                ey = 60 + rand.nextFloat() * (GameFrame.HEIGHT - 60);
            }
            default -> { // Right
                ex = GameFrame.WIDTH + 5;
                ey = 60 + rand.nextFloat() * (GameFrame.HEIGHT - 60);
            }
        }

        // Every 3 waves, enemies gain +1 HP
        int hp    = 1 + (currentWave - 1) / 3;

        // Speed scales with each wave
        float speed = 1.2f + currentWave * 0.12f;

        enemies.add(new Enemy(ex, ey, hp, speed));
    }

    /**
     * Advance to the next wave.
     * Called after the player picks an upgrade.
     */
    public void nextWave() {
        currentWave++;
        prepareWave();
    }

    // --- Getters ---

    /** Returns true when all enemies for this wave have been spawned. */
    public boolean isAllSpawned() { return allSpawned; }

    public int getCurrentWave()      { return currentWave; }
    public int getRemainingToSpawn() { return enemiesToSpawn; }
}
