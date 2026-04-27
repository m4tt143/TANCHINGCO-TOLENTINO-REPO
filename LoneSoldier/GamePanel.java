import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GamePanel - The heart of the game.
 *
 * Responsibilities:
 *   - Game loop (via javax.swing.Timer)
 *   - State machine: MENU -> PLAYING -> UPGRADE -> GAME_OVER
 *   - Input handling (WASD / arrow keys)
 *   - Collision detection
 *   - Rendering all screens
 */
public class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {

    // ── Game States ──────────────────────────────────────────────
    public enum GameState { MENU, PLAYING, UPGRADE, GAME_OVER }
    private GameState state = GameState.MENU;

    // ── Game Objects ─────────────────────────────────────────────
    private Player      player;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private WaveManager  waveManager;

    // ── Score Tracking ───────────────────────────────────────────
    private int score     = 0;
    private int highScore = 0;

    // ── Upgrade System ───────────────────────────────────────────
    private String[] upgradeOptions = new String[3];

    // ── Wave Announcement ────────────────────────────────────────
    private int waveAnnounceTicks = 0;  // Display "Wave X!" banner briefly

    // ── Game Loop ────────────────────────────────────────────────
    private final Timer gameTimer;
    private static final int FPS = 60;

    // ── Damage Flash ─────────────────────────────────────────────
    private int damageFlashTicks = 0;

    // ── Mouse Aim & Shoot ─────────────────────────────────────────
    private int  mouseX   = GameFrame.WIDTH  / 2;
    private int  mouseY   = GameFrame.HEIGHT / 2;
    private boolean mouseDown = false;

    public GamePanel() {
        this.setPreferredSize(new Dimension(GameFrame.WIDTH, GameFrame.HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        initObjects();

        // Game loop: update + repaint ~60 times per second
        gameTimer = new Timer(1000 / FPS, e -> {
            update();
            repaint();
        });
        gameTimer.start();
    }

    // ─────────────────────────────────────────────────────────────
    //  Initialization
    // ─────────────────────────────────────────────────────────────

    private void initObjects() {
        float cx = GameFrame.WIDTH  / 2f - Player.SIZE / 2f;
        float cy = GameFrame.HEIGHT / 2f - Player.SIZE / 2f;
        player      = new Player(cx, cy);
        enemies     = new ArrayList<>();
        bullets     = new ArrayList<>();
        waveManager = new WaveManager();
        score       = 0;
        damageFlashTicks   = 0;
        waveAnnounceTicks  = 120;  // Show "Wave 1!" for 2 seconds at start
    }

    private void startNewGame() {
        initObjects();
        state = GameState.PLAYING;
    }

    // ─────────────────────────────────────────────────────────────
    //  Game Loop - Update
    // ─────────────────────────────────────────────────────────────

    private void update() {
        if (state != GameState.PLAYING) return;

        // Tick announcement banner
        if (waveAnnounceTicks > 0) waveAnnounceTicks--;

        // Spawn enemies this tick
        waveManager.update(enemies);

        // Check wave clear: all spawned AND none remain
        if (waveManager.isAllSpawned() && enemies.isEmpty()) {
            updateHighScore();
            prepareUpgrades();
            state = GameState.UPGRADE;
            SoundManager.waveClear();
            return;
        }

        // Update player
        player.update();

        // Mouse-driven shooting: fire toward cursor when left button held
        if (mouseDown && player.canShoot()) {
            fireTowardMouse();
            player.resetCooldown();
            SoundManager.shoot();
        }

        // Update bullets; remove off-screen ones
        List<Bullet> deadBullets  = new ArrayList<>();
        List<Enemy>  deadEnemies  = new ArrayList<>();

        for (Bullet b : bullets) {
            b.update();
            if (b.isOffScreen()) {
                deadBullets.add(b);
                continue;
            }

            // Bullet vs Enemy collision
            for (Enemy e : enemies) {
                if (!deadEnemies.contains(e) && b.getBounds().intersects(e.getBounds())) {
                    e.hp -= b.damage;
                    deadBullets.add(b);
                    if (e.hp <= 0) {
                        deadEnemies.add(e);
                        score++;
                        SoundManager.enemyDie();
                    } else {
                        SoundManager.enemyHit();
                    }
                    break;
                }
            }
        }

        bullets.removeAll(deadBullets);
        enemies.removeAll(deadEnemies);

        // Update surviving enemies; check if they reached the player
        List<Enemy> reachedPlayer = new ArrayList<>();
        for (Enemy e : enemies) {
            e.update(player.getCenterX(), player.getCenterY());
            if (e.getBounds().intersects(player.getBounds())) {
                player.takeDamage(1);
                reachedPlayer.add(e);
                damageFlashTicks = 12;
                SoundManager.playerHit();
            }
        }
        enemies.removeAll(reachedPlayer);

        // Damage flash cooldown
        if (damageFlashTicks > 0) damageFlashTicks--;

        // Check death
        if (player.hp <= 0) {
            updateHighScore();
            state = GameState.GAME_OVER;
            SoundManager.gameOver();
        }
    }

    /** Fire bullet(s) toward the current mouse cursor position. */
    private void fireTowardMouse() {
        float dx  = mouseX - player.getCenterX();
        float dy  = mouseY - player.getCenterY();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        float nx = dx / len;
        float ny = dy / len;

        bullets.add(new Bullet(player.getCenterX(), player.getCenterY(), nx, ny, player.damage));

        if (player.multishot) {
            float angle = 0.28f;
            // Left spread
            float cosA = (float) Math.cos(angle),  sinA = (float) Math.sin(angle);
            bullets.add(new Bullet(player.getCenterX(), player.getCenterY(),
                    nx * cosA - ny * sinA, nx * sinA + ny * cosA, player.damage));
            // Right spread
            float cosB = (float) Math.cos(-angle), sinB = (float) Math.sin(-angle);
            bullets.add(new Bullet(player.getCenterX(), player.getCenterY(),
                    nx * cosB - ny * sinB, nx * sinB + ny * cosB, player.damage));
        }
    }

    private void updateHighScore() {
        if (score > highScore) highScore = score;
    }

    // ─────────────────────────────────────────────────────────────
    //  Upgrade System
    // ─────────────────────────────────────────────────────────────

    private void prepareUpgrades() {
        List<String> pool = new ArrayList<>();
        pool.add("HEAL: Restore 2 HP");
        pool.add("HP UP: +2 Max Health");
        pool.add("SWIFT: Move Speed +1");
        pool.add("POWER: Bullet Damage +1");
        pool.add("RAPID: Faster Fire Rate");
        if (!player.multishot) pool.add("MULTI: Triple Shot");

        Collections.shuffle(pool);
        for (int i = 0; i < 3; i++) upgradeOptions[i] = pool.get(i);
    }

    private void applyUpgrade(int index) {
        String chosen = upgradeOptions[index];

        if      (chosen.startsWith("HEAL"))   player.hp = Math.min(player.maxHp, player.hp + 2);
        else if (chosen.startsWith("HP UP"))  { player.maxHp += 2; player.hp = player.maxHp; }
        else if (chosen.startsWith("SWIFT"))  player.speed += 0.8f;
        else if (chosen.startsWith("POWER"))  player.damage += 1;
        else if (chosen.startsWith("RAPID"))  player.fireRateTicks = Math.max(8, player.fireRateTicks - 6);
        else if (chosen.startsWith("MULTI"))  player.multishot = true;

        waveManager.nextWave();
        waveAnnounceTicks = 120;
        state = GameState.PLAYING;
        SoundManager.waveStart();
    }

    // ─────────────────────────────────────────────────────────────
    //  Rendering
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (state) {
            case MENU      -> drawMenu(g2);
            case PLAYING   -> drawGame(g2);
            case UPGRADE   -> drawUpgrade(g2);
            case GAME_OVER -> drawGameOver(g2);
        }
    }

    // ── Background helper ──
    private void drawBackground(Graphics2D g) {
        g.setColor(new Color(38, 65, 38));
        g.fillRect(0, 0, GameFrame.WIDTH, GameFrame.HEIGHT);
        g.setColor(new Color(42, 70, 42));
        for (int x = 0; x < GameFrame.WIDTH;  x += 50) g.drawLine(x, 0, x, GameFrame.HEIGHT);
        for (int y = 0; y < GameFrame.HEIGHT; y += 50) g.drawLine(0, y, GameFrame.WIDTH, y);
    }

    // ── MENU Screen ──
    private void drawMenu(Graphics2D g) {
        // Beautiful gradient background
        g.setColor(new Color(20, 35, 20));
        g.fillRect(0, 0, GameFrame.WIDTH, GameFrame.HEIGHT);
        
        // Subtle grid pattern
        g.setColor(new Color(35, 55, 35));
        g.setStroke(new BasicStroke(1));
        for (int x = 0; x < GameFrame.WIDTH;  x += 60) g.drawLine(x, 0, x, GameFrame.HEIGHT);
        for (int y = 0; y < GameFrame.HEIGHT; y += 60) g.drawLine(0, y, GameFrame.WIDTH, y);
        
        // Decorative header box
        g.setColor(new Color(30, 50, 30, 180));
        g.fillRoundRect(100, 80, 600, 120, 20, 20);
        g.setColor(new Color(100, 180, 100));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(100, 80, 600, 120, 20, 20);

        // Game Title with glow effect
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(new Color(255, 230, 100));
        drawCentered(g, "LONE SOLDIER", 140);
        
        // Subtitle
        g.setFont(new Font("Courier New", Font.ITALIC, 26));
        g.setColor(new Color(150, 220, 150));
        drawCentered(g, "« Last Stand »", 175);

        // Divider with style
        g.setColor(new Color(100, 180, 100));
        g.fillRect(180, 200, 440, 2);

        // Main instructions box
        g.setColor(new Color(25, 45, 25, 170));
        g.fillRoundRect(120, 220, 560, 160, 15, 15);
        g.setColor(new Color(120, 200, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(120, 220, 560, 160, 15, 15);

        // Controls heading
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(new Color(255, 200, 80));
        drawCentered(g, "HOW TO PLAY", 244);

        // Controls
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(new Color(220, 235, 220));
        String[] lines = {
            "◆ Move: W A S D  or  Arrow Keys",
            "◆ Aim: Move Your Mouse",
            "◆ Shoot: Hold Left Mouse Button",
        };
        int lineY = 272;
        for (String line : lines) { drawCentered(g, line, lineY); lineY += 25; }

        // Instructions box
        g.setColor(new Color(25, 45, 25, 170));
        g.fillRoundRect(120, 395, 560, 80, 15, 15);
        g.setColor(new Color(120, 200, 120));
        g.drawRoundRect(120, 395, 560, 80, 15, 15);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(200, 225, 200));
        drawCentered(g, "Survive endless waves of enemies  •  Choose upgrades between waves", 415);
        drawCentered(g, "Defeat more soldiers to increase difficulty and gain more points!", 438);

        // ENTER prompt (pulsing effect)
        long pulse = (System.currentTimeMillis() / 500) % 2;
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(pulse == 0 ? new Color(255, 220, 60) : new Color(200, 160, 30));
        g.setStroke(new BasicStroke(2));
        drawCentered(g, "▶  PRESS  ENTER  TO START  ◀", 495);

        // High score box
        if (highScore > 0) {
            g.setColor(new Color(40, 70, 40, 180));
            g.fillRoundRect(220, 520, 360, 50, 10, 10);
            g.setColor(new Color(255, 190, 50));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(220, 520, 360, 50, 10, 10);
            
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(new Color(255, 210, 80));
            drawCentered(g, "★ BEST SCORE: " + highScore + " KILLS ★", 548);
        }

        // Credits
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(100, 140, 100));
        drawCentered(g, "Made by Tolentino & Tanchico  |  Programming 2  |  UPHSD Molino", GameFrame.HEIGHT - 18);
    }

    // ── GAMEPLAY Screen ──
    private void drawGame(Graphics2D g) {
        drawBackground(g);

        // Red flash on damage
        if (damageFlashTicks > 0) {
            g.setColor(new Color(200, 0, 0, Math.min(120, damageFlashTicks * 10)));
            g.fillRect(0, 0, GameFrame.WIDTH, GameFrame.HEIGHT);
        }

        // Draw entities
        for (Bullet b : bullets) b.draw(g);
        for (Enemy  e : enemies) e.draw(g);
        // Calculate angle from player center to mouse (offset by -PI/2 so helmet faces up by default)
        double aimAngle = Math.atan2(mouseY - player.getCenterY(), mouseX - player.getCenterX()) + Math.PI / 2;
        player.draw(g, aimAngle);

        // HUD overlay
        drawHUD(g);

        // Wave announcement banner
        if (waveAnnounceTicks > 0) {
            float alpha = Math.min(1f, waveAnnounceTicks / 30f);
            g.setColor(new Color(0, 0, 0, (int)(alpha * 160)));
            g.fillRoundRect(GameFrame.WIDTH / 2 - 160, GameFrame.HEIGHT / 2 - 35, 320, 70, 20, 20);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(new Color(255, 220, 50, (int)(alpha * 255)));
            drawCentered(g, "WAVE " + waveManager.getCurrentWave() + "  INCOMING!", GameFrame.HEIGHT / 2 + 12);
        }
    }

    // ── HUD (Heads-Up Display) ──
    private void drawHUD(Graphics2D g) {
        // Top bar
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, GameFrame.WIDTH, 50);

        // Wave
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 220, 50));
        g.drawString("Wave: " + waveManager.getCurrentWave(), 16, 32);

        // Kills (score) - centered
        g.setColor(Color.WHITE);
        drawCentered(g, "Kills: " + score, 32);

        // Enemy count - right
        g.setColor(new Color(255, 140, 140));
        String eCount = "Enemies: " + enemies.size();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(eCount, GameFrame.WIDTH - fm.stringWidth(eCount) - 14, 32);

        // HP bar (bottom left)
        int bx = 16, by = GameFrame.HEIGHT - 36;
        int bw = 180, bh = 20;

        g.setColor(new Color(80, 0, 0));
        g.fillRoundRect(bx, by, bw, bh, 8, 8);

        float ratio = (float) player.hp / player.maxHp;
        Color hpColor = ratio > 0.5f ? new Color(60, 200, 60)
                      : ratio > 0.25f ? new Color(220, 160, 0)
                      : new Color(200, 40, 40);
        g.setColor(hpColor);
        g.fillRoundRect(bx, by, (int)(bw * ratio), bh, 8, 8);

        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("HP  " + player.hp + " / " + player.maxHp, bx + 8, by + 14);

        // Incoming warning
        if (!waveManager.isAllSpawned()) {
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            g.setColor(new Color(255, 160, 50));
            String warn = "Incoming: " + waveManager.getRemainingToSpawn();
            fm = g.getFontMetrics();
            g.drawString(warn, GameFrame.WIDTH - fm.stringWidth(warn) - 14, GameFrame.HEIGHT - 18);
        }
    }

    // ── UPGRADE Screen ──
    private void drawUpgrade(Graphics2D g) {
        drawBackground(g);

        // Dark overlay
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, GameFrame.WIDTH, GameFrame.HEIGHT);

        // Header
        g.setFont(new Font("Arial", Font.BOLD, 34));
        g.setColor(new Color(255, 215, 40));
        drawCentered(g, "WAVE " + (waveManager.getCurrentWave() - 1) + " CLEARED!", 110);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(new Color(220, 220, 220));
        drawCentered(g, "Choose 1 upgrade:", 148);

        // Three upgrade cards
        int cardW = 200, cardH = 130;
        int totalW = cardW * 3 + 40;
        int startX = (GameFrame.WIDTH - totalW) / 2;
        int cardY  = 180;

        for (int i = 0; i < 3; i++) {
            int cx = startX + i * (cardW + 20);
            drawUpgradeCard(g, cx, cardY, cardW, cardH, upgradeOptions[i], i + 1);
        }

        // Key hint
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(Color.LIGHT_GRAY);
        drawCentered(g, "Press  1 ,  2 ,  or  3  to choose", cardY + cardH + 40);

        // Current stats bar
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(160, 210, 160));
        String stats = "HP: " + player.hp + "/" + player.maxHp
                + "   DMG: " + player.damage
                + "   SPD: " + String.format("%.1f", player.speed)
                + "   Kills: " + score;
        drawCentered(g, stats, GameFrame.HEIGHT - 25);
    }

    private void drawUpgradeCard(Graphics2D g, int cx, int cy, int w, int h, String text, int key) {
        // Card background
        g.setColor(new Color(20, 40, 75));
        g.fillRoundRect(cx, cy, w, h, 14, 14);

        // Border
        g.setColor(new Color(70, 120, 210));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(cx, cy, w, h, 14, 14);
        g.setStroke(new BasicStroke(1));

        // Split tag and description
        String[] parts = text.split(": ", 2);
        String tag  = parts.length > 0 ? parts[0] : text;
        String desc = parts.length > 1 ? parts[1] : "";

        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(new Color(255, 220, 80));
        int tagW = g.getFontMetrics().stringWidth(tag);
        g.drawString(tag, cx + (w - tagW) / 2, cy + 38);

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(Color.WHITE);
        // Word-wrap description
        String[] words = desc.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = cy + 62;
        for (String word : words) {
            String test = line + word + " ";
            if (g.getFontMetrics().stringWidth(test) > w - 20 && !line.isEmpty()) {
                String l = line.toString().trim();
                g.drawString(l, cx + (w - g.getFontMetrics().stringWidth(l)) / 2, lineY);
                lineY += 18;
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.toString().trim().isEmpty()) {
            String l = line.toString().trim();
            g.drawString(l, cx + (w - g.getFontMetrics().stringWidth(l)) / 2, lineY);
        }

        // Key badge
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 215, 40));
        String badge = "[ " + key + " ]";
        int bw = g.getFontMetrics().stringWidth(badge);
        g.drawString(badge, cx + (w - bw) / 2, cy + h - 12);
    }

    // ── GAME OVER Screen ──
    private void drawGameOver(Graphics2D g) {
        drawBackground(g);
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, GameFrame.WIDTH, GameFrame.HEIGHT);

        g.setFont(new Font("Arial", Font.BOLD, 66));
        g.setColor(new Color(210, 40, 40));
        drawCentered(g, "GAME OVER", 215);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        drawCentered(g, "Total Kills : " + score, 290);
        drawCentered(g, "Reached Wave : " + waveManager.getCurrentWave(), 330);

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(new Color(255, 210, 50));
        drawCentered(g, "Best Score : " + highScore + " kills", 380);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(Color.LIGHT_GRAY);
        drawCentered(g, "[ ENTER ]  Play Again", 440);
        drawCentered(g, "[ ESC ]    Main Menu",  472);
    }

    // ── Utility: center a string horizontally ──
    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (GameFrame.WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    // ─────────────────────────────────────────────────────────────
    //  Input Handling
    // ─────────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // MENU
        if (state == GameState.MENU && key == KeyEvent.VK_ENTER) {
            startNewGame();
        }

        // PLAYING - movement
        if (state == GameState.PLAYING) {
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP)    player.up    = true;
            if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN)  player.down  = true;
            if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)  player.left  = true;
            if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) player.right = true;
        }

        // UPGRADE - choose card
        if (state == GameState.UPGRADE) {
            if (key == KeyEvent.VK_1) applyUpgrade(0);
            if (key == KeyEvent.VK_2) applyUpgrade(1);
            if (key == KeyEvent.VK_3) applyUpgrade(2);
        }

        // GAME OVER - restart or menu
        if (state == GameState.GAME_OVER) {
            if (key == KeyEvent.VK_ENTER)  startNewGame();
            if (key == KeyEvent.VK_ESCAPE) state = GameState.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP)    player.up    = false;
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN)  player.down  = false;
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)  player.left  = false;
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) player.right = false;
    }

    @Override
    public void keyTyped(KeyEvent e) { /* not used */ }

    // ─────────────────────────────────────────────────────────────
    //  Mouse Input Handling
    // ─────────────────────────────────────────────────────────────

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseDown = true;
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseDown = false;
    }
    @Override public void mouseMoved(MouseEvent e)   { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e)  {}
}