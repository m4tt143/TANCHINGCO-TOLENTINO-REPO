import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SoundManager - Procedurally synthesized audio for all game events.
 * No external audio files needed — all sounds are generated in code.
 *
 * Sounds:
 *   shoot()        — short snappy gunshot crack
 *   enemyHit()     — dull thud when a bullet connects
 *   enemyDie()     — descending pop when an enemy is killed
 *   playerHit()    — deep bass thump when the player takes damage
 *   waveClear()    — ascending fanfare when a wave is cleared
 *   waveStart()    — tense alert tone at wave start
 *   gameOver()     — low dramatic drone on death
 */
public class SoundManager {

    // Thread pool so sounds never block the game loop
    private static final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sound-thread");
        t.setDaemon(true);
        return t;
    });

    private static final float SAMPLE_RATE = 44100f;

    // ── Public API ────────────────────────────────────────────────

    public static void shoot()      { pool.execute(() -> playShoot()); }
    public static void enemyHit()   { pool.execute(() -> playEnemyHit()); }
    public static void enemyDie()   { pool.execute(() -> playEnemyDie()); }
    public static void playerHit()  { pool.execute(() -> playPlayerHit()); }
    public static void waveClear()  { pool.execute(() -> playWaveClear()); }
    public static void waveStart()  { pool.execute(() -> playWaveStart()); }
    public static void gameOver()   { pool.execute(() -> playGameOver()); }

    // ── Sound Synthesis ──────────────────────────────────────────

    /** Short snappy crack — layered noise burst + quick pitch drop */
    private static void playShoot() {
        int frames = (int)(SAMPLE_RATE * 0.10f); // 100ms
        byte[] buf = new byte[frames];
        for (int i = 0; i < frames; i++) {
            double t = i / SAMPLE_RATE;
            // Noise burst that decays fast
            double noise = (Math.random() * 2 - 1);
            // Tonal click component — high frequency descending
            double tone  = Math.sin(2 * Math.PI * (1200 - i * 8) * t);
            double env   = Math.exp(-t * 30);  // fast decay
            buf[i] = clamp((noise * 0.6 + tone * 0.4) * env * 90);
        }
        play(buf, 0.55f);
    }

    /** Dull fleshy thud — mid-frequency thump */
    private static void playEnemyHit() {
        int frames = (int)(SAMPLE_RATE * 0.08f);
        byte[] buf = new byte[frames];
        for (int i = 0; i < frames; i++) {
            double t   = i / SAMPLE_RATE;
            double freq = 280 - i * 1.5;  // slight pitch drop
            double tone = Math.sin(2 * Math.PI * freq * t);
            double env  = Math.exp(-t * 40);
            buf[i] = clamp(tone * env * 100);
        }
        play(buf, 0.45f);
    }

    /** Descending pop — enemy eliminated */
    private static void playEnemyDie() {
        int frames = (int)(SAMPLE_RATE * 0.18f);
        byte[] buf = new byte[frames];
        for (int i = 0; i < frames; i++) {
            double t    = i / SAMPLE_RATE;
            double freq = 500 * Math.exp(-t * 8);  // fast descending
            double tone = Math.sin(2 * Math.PI * freq * t);
            double noise = (Math.random() * 2 - 1) * 0.15;
            double env   = Math.exp(-t * 12);
            buf[i] = clamp((tone + noise) * env * 110);
        }
        play(buf, 0.5f);
    }

    /** Deep bass hit + distortion — player takes damage */
    private static void playPlayerHit() {
        int frames = (int)(SAMPLE_RATE * 0.35f);
        byte[] buf = new byte[frames];
        for (int i = 0; i < frames; i++) {
            double t    = i / SAMPLE_RATE;
            // Low rumble
            double bass = Math.sin(2 * Math.PI * 60 * t);
            // Mid crunch
            double mid  = Math.sin(2 * Math.PI * 180 * t);
            // Noise layer
            double noise = (Math.random() * 2 - 1) * 0.3;
            double env   = Math.exp(-t * 8);
            double sample = (bass * 0.5 + mid * 0.3 + noise) * env;
            // Soft clipping for crunch
            sample = Math.tanh(sample * 2.0) * 0.85;
            buf[i] = clamp(sample * 115);
        }
        play(buf, 0.75f);
    }

    /** Ascending 3-note fanfare — wave cleared! */
    private static void playWaveClear() {
        double[] notes = { 523.25, 659.25, 783.99 }; // C5, E5, G5
        int noteDur = (int)(SAMPLE_RATE * 0.18f);
        int total   = noteDur * notes.length;
        byte[] buf  = new byte[total];
        for (int n = 0; n < notes.length; n++) {
            for (int i = 0; i < noteDur; i++) {
                double t    = i / SAMPLE_RATE;
                double tone = Math.sin(2 * Math.PI * notes[n] * t);
                // Add harmonics for richness
                double harm = Math.sin(2 * Math.PI * notes[n] * 2 * t) * 0.3;
                double env  = Math.exp(-t * 4) * (1 - Math.exp(-t * 80)); // attack + decay
                buf[n * noteDur + i] = clamp((tone + harm) * env * 100);
            }
        }
        play(buf, 0.6f);
    }

    /** Tense two-tone alert — new wave incoming */
    private static void playWaveStart() {
        double[] freqs = { 330.0, 440.0 };
        int noteDur = (int)(SAMPLE_RATE * 0.12f);
        byte[] buf  = new byte[noteDur * 2];
        for (int n = 0; n < 2; n++) {
            for (int i = 0; i < noteDur; i++) {
                double t    = i / SAMPLE_RATE;
                double tone = Math.sin(2 * Math.PI * freqs[n] * t);
                double env  = Math.exp(-t * 6) * (1 - Math.exp(-t * 60));
                buf[n * noteDur + i] = clamp(tone * env * 95);
            }
        }
        play(buf, 0.5f);
    }

    /** Low dramatic descending drone — game over */
    private static void playGameOver() {
        int frames = (int)(SAMPLE_RATE * 1.2f);
        byte[] buf = new byte[frames];
        for (int i = 0; i < frames; i++) {
            double t    = i / SAMPLE_RATE;
            double freq = 220 * Math.exp(-t * 0.8);   // slow descent
            double tone = Math.sin(2 * Math.PI * freq * t);
            double sub  = Math.sin(2 * Math.PI * freq * 0.5 * t) * 0.4; // sub bass
            double env  = Math.exp(-t * 1.8);
            buf[i] = clamp((tone + sub) * env * 120);
        }
        play(buf, 0.8f);
    }

    // ── Playback Utility ─────────────────────────────────────────

    private static void play(byte[] buf, float volume) {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) return;

            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt);

            // Apply volume scaling
            byte[] scaled = new byte[buf.length];
            for (int i = 0; i < buf.length; i++) {
                scaled[i] = clamp(buf[i] * volume);
            }

            line.start();
            line.write(scaled, 0, scaled.length);
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
            // Silently skip if audio system is busy
        }
    }

    /** Clamp a double sample to [-127, 127] as a byte. */
    private static byte clamp(double v) {
        return (byte) Math.max(-127, Math.min(127, (int) v));
    }
}
