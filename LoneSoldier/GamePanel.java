import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GamePanel - Endless Waves Survival style.
 * Continuous spawning, survival timer, coins, XP/level, upgrade cards.
 */
public class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {

    // ── States ───────────────────────────────────────────────────
    public enum GameState { CHARACTER_SELECT, SHOP, MENU, PLAYING, UPGRADE, IN_GAME_SHOP, GAME_OVER }
    private GameState state = GameState.MENU;

    // ── Game Objects ─────────────────────────────────────────────
    private Player       player;
    private List<Enemy>  enemies;
    private List<Bullet> bullets;
    private WaveManager  waveManager;
    
    // ── Character Images ──────────────────────────────────────────
    private static final java.util.Map<Player.CharacterType, BufferedImage> characterImages = new java.util.HashMap<>();
    
    // ── Character & Shop ─────────────────────────────────────────
    private Player.CharacterType selectedCharacter = Player.CharacterType.SOLDIER;
    private int shopCoins = 0;  // Coins earned from previous run
    private boolean[] shopBought = new boolean[4]; // Track bought perks

    // ── Economy & Progression ────────────────────────────────────
    private int   coins      = 0;
    private int   totalKills = 0;
    private int   highScore  = 0;   // best kill count
    private int   xp         = 0;
    private int   level      = 1;
    private int   xpToNext   = 10;  // XP needed for next level

    // ── Survival Timer ───────────────────────────────────────────
    private int   survivalTicks = 0; // total ticks alive

    // ── Upgrade System ───────────────────────────────────────────
    private String[] upgradeOptions = new String[3];

    // ── Feedback ─────────────────────────────────────────────────
    private int   damageFlashTicks = 0;
    private int   killFlashTicks   = 0;
    private int   waveFlashTicks   = 0;
    private int   lastWaveShown    = 0;

    // ── Animation ────────────────────────────────────────────────
    private long  animTick    = 0;
    private int   hoveredCard = -1;
    
    // ── Screen Effects ────────────────────────────────────────────
    private float screenShakeX = 0, screenShakeY = 0;
    private int   screenShakeTicks = 0;
    private float characterBobOffset = 0;
    private int   crosshairPulse    = 0;  // expands on shoot
    private int   hoveredCharCard   = -1; // character select hover
    private int   statRevealTick    = 0;  // game over stat reveal animation

    // ── Particles [x,y,vx,vy,life,maxLife,r,g,b] ─────────────────
    private final List<float[]> particles = new ArrayList<>();

    // ── Floating text [x,y,vx,vy,life,text,r,g,b] ────────────────
    private final List<Object[]> floatTexts = new ArrayList<>();

    // ── Mouse ─────────────────────────────────────────────────────
    private int  mouseX = GameFrame.WIDTH/2, mouseY = GameFrame.HEIGHT/2;
    private boolean mouseDown = false;
    
    // ── Performance Caches ────────────────────────────────────────
    private FontMetrics cachedFM = null;
    private List<Bullet> deadBulletsPool = new ArrayList<>();
    private List<Enemy> deadEnemiesPool = new ArrayList<>();
    private List<Enemy> hitPlayerPool = new ArrayList<>();

    // ── Game Loop ─────────────────────────────────────────────────
    private final Timer gameTimer;
    private static final int FPS = 60;

    // ─────────────────────────────────────────────────────────────

    public GamePanel() {
        setPreferredSize(new Dimension(GameFrame.WIDTH, GameFrame.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        loadCharacterImages();
        initObjects();
        gameTimer = new Timer(1000/FPS, e -> { update(); repaint(); });
        gameTimer.start();
    }

    // ── Init ─────────────────────────────────────────────────────

    private void loadCharacterImages() {
        String[] characters = {"SOLDIER", "MAGE", "TANK", "ROGUE"};
        Player.CharacterType[] types = Player.CharacterType.values();
        
        // Try multiple path locations
        String[] pathAttempts = {
            "assets/images/",
            "LoneSoldier/assets/images/",
            "./assets/images/"
        };
        
        for (int i = 0; i < characters.length && i < types.length; i++) {
            boolean loaded = false;
            for (String pathBase : pathAttempts) {
                try {
                    File imgFile = new File(pathBase + characters[i] + ".png");
                    if (imgFile.exists()) {
                        BufferedImage img = ImageIO.read(imgFile);
                        characterImages.put(types[i], img);
                        System.out.println("✓ Loaded " + characters[i] + " from: " + imgFile.getAbsolutePath());
                        loaded = true;
                        break;
                    }
                } catch (Exception e) {
                    // Try next path
                }
            }
            if (!loaded) {
                System.out.println("✗ Could not load image for " + characters[i]);
            }
        }
    }

    private void initObjects() {
        float cx = GameFrame.WIDTH/2f - Player.SIZE/2f;
        float cy = GameFrame.HEIGHT/2f - Player.SIZE/2f;
        player      = new Player(cx, cy, selectedCharacter);
        enemies     = new ArrayList<>();
        bullets     = new ArrayList<>();
        waveManager = new WaveManager();
        coins = 0; totalKills = 0; xp = 0; level = 1; xpToNext = 10;
        survivalTicks = 0; damageFlashTicks = 0; waveFlashTicks = 0; lastWaveShown = 0;
        particles.clear(); floatTexts.clear();
        // Reset new upgrades
        player.enemySlowPercent = 0f;
        player.critChance = 0f;
        player.coinMultiplier = 1.0f;
        player.regenTicksLeft = 0;
        player.bulletSpreadAngle = 0f;
        
        // Apply shop perks if bought
        if (shopBought[0]) player.maxHp += 2;      // Extra HP perk
        if (shopBought[1]) player.damage += 1;     // Damage perk
        if (shopBought[2]) player.speed += 0.5f;   // Speed perk
        if (shopBought[3]) player.coinMultiplier += 0.25f; // Coin perk
        player.hp = player.maxHp;
    }

    private void addScreenShake(int intensity, int duration) {
        screenShakeTicks = Math.max(screenShakeTicks, duration);
        // Intensity affects the magnitude but we handle that in the update
    }

    private void startNewGame() { initObjects(); state = GameState.PLAYING; }

    // ── Update ────────────────────────────────────────────────────

    private void update() {
        if (state != GameState.PLAYING) { animTick++; if(state==GameState.GAME_OVER&&statRevealTick<120) statRevealTick++; return; }

        animTick++;
        survivalTicks++;

        // Regen tick
        if (player.regenTicksLeft > 0) {
            player.regenTicksLeft--;
            if (player.regenTicksLeft % 300 == 0) { // Every 5 seconds at 60 FPS
                player.hp = Math.min(player.maxHp, player.hp + 1);
                spawnFloatText(player.getCenterX(), player.getCenterY()-25, "+1", 100,255,100);
            }
        }
        particles.removeIf(p -> p[4] <= 0);
        for (float[] p : particles) { p[0]+=p[2]; p[1]+=p[3]; p[2]*=0.91f; p[3]*=0.91f; p[4]--; }

        // Tick float texts
        floatTexts.removeIf(ft -> ((int)ft[3]) <= 0);
        for (Object[] ft : floatTexts) { ft[0]=(float)ft[0]+(float)ft[2]; ft[1]=(float)ft[1]-0.9f; ft[3]=(int)ft[3]-1; }

        // Wave check for banner
        int w = waveManager.getCurrentWave();
        if (w != lastWaveShown) { lastWaveShown = w; waveFlashTicks = 150; SoundManager.waveStart(); }
        if (waveFlashTicks > 0) waveFlashTicks--;

        // Spawn enemies
        waveManager.update(enemies);

        // Player update + shooting
        player.update();
        if (mouseDown && player.canShoot()) { fireTowardMouse(); player.resetCooldown(); SoundManager.shoot(); crosshairPulse = 8; }

        // Bullet collisions - use pooled lists to reduce allocations
        deadBulletsPool.clear();
        deadEnemiesPool.clear();
        for (Bullet b : bullets) {
            b.update();
            if (b.isOffScreen()) { deadBulletsPool.add(b); continue; }
            for (Enemy e : enemies) {
                if (!deadEnemiesPool.contains(e) && b.getBounds().intersects(e.getBounds())) {
                    e.hp -= b.damage;
                    deadBulletsPool.add(b);
                    if (e.hp <= 0) {
                        deadEnemiesPool.add(e);
                        totalKills++; killFlashTicks = 10; 
                        int baseCoins = e.coinDrop;
                        int earnedCoins = (int)(baseCoins * player.coinMultiplier);
                        coins += earnedCoins;
                        gainXP(e.elite ? 5 : 1);
                        // Add screen shake for enemy kills
                        addScreenShake(e.elite ? 6 : 4, e.elite ? 8 : 6);
                        SoundManager.enemyDie();
                        spawnDeathParticles(e);
                        spawnFloatText(e.getCenterX(), e.getCenterY()-10, "+" + earnedCoins, 255,215,0);
                    } else {
                        SoundManager.enemyHit();
                        spawnHitSparks(e);
                    }
                    break;
                }
            }
        }
        bullets.removeAll(deadBulletsPool);
        enemies.removeAll(deadEnemiesPool);

        // Enemy → player collision - use pooled list
        hitPlayerPool.clear();
        for (Enemy e : enemies) {
            float effectiveSpeed = e.speed * (1f - player.enemySlowPercent);
            e.update(player.getCenterX(), player.getCenterY(), effectiveSpeed);
            if (e.getBounds().intersects(player.getBounds())) {
                player.takeDamage(e.elite ? 2 : 1);
                hitPlayerPool.add(e);
                damageFlashTicks = 14;
                // Add screen shake for damage
                addScreenShake(8, 6);
                SoundManager.playerHit();
                spawnFloatText(player.getCenterX(), player.getCenterY()-20, "-" + (e.elite?2:1)+" HP", 255,60,60);
            }
        }
        enemies.removeAll(hitPlayerPool);

        // Update screen shake
        if (screenShakeTicks > 0) {
            screenShakeTicks--;
            screenShakeX = (float)(Math.random() - 0.5) * (screenShakeTicks / 2f);
            screenShakeY = (float)(Math.random() - 0.5) * (screenShakeTicks / 2f);
        } else {
            screenShakeX = screenShakeY = 0;
        }

        // Character bobbing animation (when moving)
        boolean isMoving = player.up || player.down || player.left || player.right;
        if (isMoving) {
            characterBobOffset = (float)Math.sin(animTick * 0.3) * 2f; // Gentle up-down bob
        } else {
            characterBobOffset *= 0.9f; // Settle back to center when stopping
        }

        if (damageFlashTicks > 0) damageFlashTicks--;
        if (crosshairPulse   > 0) crosshairPulse--;
        if (killFlashTicks   > 0) killFlashTicks--;

        // Check upgrade (every level-up)
        // (handled in gainXP)

        // Death
        if (player.hp <= 0) {
            if (totalKills > highScore) highScore = totalKills;
            shopCoins = coins; // Save coins for shop
            state = GameState.GAME_OVER; statRevealTick = 0;
            SoundManager.gameOver();
        }
    }

    private void gainXP(int amount) {
        xp += amount;
        if (xp >= xpToNext) {
            xp -= xpToNext;
            level++;
            xpToNext = 10 + level * 5;
            prepareUpgrades();
            state = GameState.UPGRADE;
            SoundManager.waveClear();
        }
    }

    private void fireTowardMouse() {
        float dx=mouseX-player.getCenterX(), dy=mouseY-player.getCenterY();
        float len=(float)Math.sqrt(dx*dx+dy*dy); if(len==0)return;
        float nx=dx/len, ny=dy/len;
        
        int baseDamage = player.getEffectiveDamage();
        if (Math.random() < player.critChance) baseDamage += 2;
        
        bullets.add(new Bullet(player.getCenterX(),player.getCenterY(),nx,ny,baseDamage));
        if (player.multishot) {
            float a=0.28f + player.bulletSpreadAngle;
            float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
            bullets.add(new Bullet(player.getCenterX(),player.getCenterY(),nx*ca-ny*sa,nx*sa+ny*ca,baseDamage));
            float cb=(float)Math.cos(-a),sb=(float)Math.sin(-a);
            bullets.add(new Bullet(player.getCenterX(),player.getCenterY(),nx*cb-ny*sb,nx*sb+ny*cb,baseDamage));
        }
    }

    // ── Particles ─────────────────────────────────────────────────

    private void spawnDeathParticles(Enemy e) {
        float cx=e.getCenterX(), cy=e.getCenterY();
        Color c = e.elite ? new Color(180,60,255) : new Color(200,30,30);
        
        // Main explosion particles
        for (int i=0;i<20;i++) {
            float ang=(float)(Math.random()*Math.PI*2), spd=2f+(float)Math.random()*4f;
            float life = 25 + (float)Math.random()*15;
            particles.add(new float[]{cx,cy,(float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,life,life,c.getRed(),c.getGreen(),c.getBlue()});
        }
        
        // Secondary spark particles
        for (int i=0;i<8;i++) {
            float ang=(float)(Math.random()*Math.PI*2), spd=1f+(float)Math.random()*2f;
            particles.add(new float[]{cx,cy,(float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,15,20,255,255,150});
        }
    }

    private void spawnHitSparks(Enemy e) {
        // Enhanced hit sparks with more particles and colors
        for (int i=0;i<12;i++) {
            float ang=(float)(Math.random()*Math.PI*2), spd=1.5f+(float)Math.random()*3f;
            // Mix of yellow and orange sparks
            int r = 255, g = 180 + (int)(Math.random()*75), b = 50 + (int)(Math.random()*100);
            particles.add(new float[]{e.getCenterX(),e.getCenterY(),(float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,12,15,r,g,b});
        }
        
        // Small impact flash
        for (int i=0;i<3;i++) {
            float ang=(float)(Math.random()*Math.PI*2), spd=0.5f+(float)Math.random()*1f;
            particles.add(new float[]{e.getCenterX(),e.getCenterY(),(float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,8,10,255,255,255});
        }
    }

    private void spawnFloatText(float x, float y, String text, int r, int g, int b) {
        floatTexts.add(new Object[]{x, y, (float)((Math.random()-0.5)*1.0), 45, text, r, g, b});
    }

    // ── Upgrades ─────────────────────────────────────────────────

    private void prepareUpgrades() {
        List<String> pool = new ArrayList<>();
        pool.add("HEAL|Restore 3 HP|<3");
        pool.add("HP UP|+3 Max Health|HP");
        pool.add("SWIFT|Move Speed +0.8|>>");
        pool.add("POWER|Bullet Damage +1|**");
        pool.add("RAPID|Faster Fire Rate|!!");
        if (!player.multishot) pool.add("MULTI|Triple Shot|:::");
        pool.add("ARMOR|Block next 5 hits|[]");
        pool.add("SLOW|Enemies 20% slower|--");
        pool.add("CRIT|20% crit +2 dmg|^^");
        pool.add("COINS|+50% gold earned|$");
        pool.add("REGEN|Heal 1 HP each 5s|+-");
        pool.add("WIDE|Bullet spread wider|><");
        Collections.shuffle(pool);
        for (int i=0;i<3;i++) upgradeOptions[i] = pool.get(i);
    }

    private void applyUpgrade(int index) {
        String[] parts = upgradeOptions[index].split("\\|");
        String tag = parts[0];
        switch (tag) {
            case "HEAL"  -> player.hp = Math.min(player.maxHp, player.hp + 3);
            case "HP UP" -> { player.maxHp += 3; player.hp = player.maxHp; }
            case "SWIFT" -> player.speed += 0.8f;
            case "POWER" -> player.damage += 1;
            case "RAPID" -> player.fireRateTicks = Math.max(8, player.fireRateTicks - 5);
            case "MULTI" -> player.multishot = true;
            case "ARMOR" -> player.armor += 5;
            case "SLOW"  -> player.enemySlowPercent += 0.15f;
            case "CRIT"  -> player.critChance += 0.15f;
            case "COINS" -> player.coinMultiplier += 0.3f;
            case "REGEN" -> player.regenTicksLeft = Integer.MAX_VALUE; // infinite regen at 5s intervals
            case "WIDE"  -> player.bulletSpreadAngle += 0.15f;
        }
        state = GameState.PLAYING;
        SoundManager.upgradeSelect();
        SoundManager.levelUp();
    }

    // ─────────────────────────────────────────────────────────────
    //  Rendering
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // Performance optimizations
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        switch (state) {
            case CHARACTER_SELECT -> drawCharacterSelect(g2);
            case SHOP            -> drawShop(g2);
            case MENU            -> drawMenu(g2);
            case PLAYING         -> drawGame(g2);
            case UPGRADE         -> drawUpgrade(g2);
            case IN_GAME_SHOP    -> { drawGame(g2); drawInGameShop(g2); }
            case GAME_OVER       -> drawGameOver(g2);
        }
    }

    // ── CHARACTER SELECT ──
    
    private void drawCharacterSelect(Graphics2D g) {
        // Dark bg
        g.setColor(new Color(5,8,12)); g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);
        
        g.setFont(new Font("Monospaced",Font.BOLD,36));
        FontMetrics fm=g.getFontMetrics();
        String title="SELECT  YOUR  SOLDIER";
        // Animated colour
        double tp=Math.sin(animTick*0.05)*0.5+0.5;
        g.setColor(new Color((int)(50+tp*30),220,(int)(80+tp*40)));
        g.drawString(title,(GameFrame.WIDTH-fm.stringWidth(title))/2,56);
        // Underline
        int tw=fm.stringWidth(title);
        g.setColor(new Color(40,140,60,120));
        g.fillRect((GameFrame.WIDTH-tw)/2,60,tw,1);
        
        // Character cards
        Player.CharacterType[] chars = Player.CharacterType.values();
        int cardW=200, cardH=280, totalW=cardW*4+60, startX=(GameFrame.WIDTH-totalW)/2, cardY=100;
        
        for (int i=0; i<chars.length; i++) {
            int cx = startX + i*(cardW+20);
            drawCharacterCard(g, cx, cardY, cardW, cardH, chars[i], i+1);
        }
        
        g.setFont(new Font("Monospaced",Font.PLAIN,13)); g.setColor(new Color(90,140,100));
        String hint="Press [ 1 ] [ 2 ] [ 3 ] [ 4 ]  or  click a character"; fm=g.getFontMetrics();
        g.drawString(hint,(GameFrame.WIDTH-fm.stringWidth(hint))/2,cardY+cardH+40);
    }
    
    private void drawCharacterCard(Graphics2D g, int x, int y, int w, int h, Player.CharacterType chr, int key) {
        Player.CharacterType[] allChars=Player.CharacterType.values();
        int idx=0; for(int i=0;i<allChars.length;i++) if(allChars[i]==chr) idx=i;
        boolean hovered=(idx==hoveredCharCard);
        boolean selected=(chr==selectedCharacter);
        int lift=hovered?5:0; y-=lift;

        // Outer glow for hovered/selected
        if(hovered||selected){
            Color glowC=selected?new Color(255,200,0,40):new Color(60,220,80,30);
            g.setColor(glowC); g.fillRoundRect(x-6,y-6,w+12,h+12,16,16);
        }
        // Card bg
        g.setColor(hovered?new Color(16,36,22):new Color(12,28,18)); g.fillRoundRect(x,y,w,h,12,12);
        // Top accent bar
        Color chrColor2=switch(chr){case SOLDIER->new Color(40,110,220);case MAGE->new Color(150,80,220);case TANK->new Color(220,150,40);case ROGUE->new Color(100,200,80);};
        g.setColor(new Color(chrColor2.getRed(),chrColor2.getGreen(),chrColor2.getBlue(),hovered?180:100));
        g.fillRoundRect(x,y,w,5,4,4);
        Color borderCol=selected?new Color(255,210,0,220):hovered?new Color(80,220,100,200):new Color(40,140,60,140);
        g.setColor(borderCol); g.setStroke(new BasicStroke(selected?2.5f:hovered?2f:1.5f));
        g.drawRoundRect(x,y,w,h,12,12); g.setStroke(new BasicStroke(1));
        
        // Color by character
        Color chrColor = switch(chr) {
            case SOLDIER -> new Color(40, 110, 220);
            case MAGE -> new Color(150, 80, 220);
            case TANK -> new Color(220, 150, 40);
            case ROGUE -> new Color(100, 200, 80);
        };
        
        // Character name
        g.setFont(new Font("Monospaced",Font.BOLD,18)); 
        FontMetrics fm=g.getFontMetrics();
        g.setColor(chrColor); String name=chr.toString();
        g.drawString(name,x+(w-fm.stringWidth(name))/2,y+40);
        
        // Character image (after name)
        if (characterImages.containsKey(chr)) {
            BufferedImage img = characterImages.get(chr);
            int imgW = 120, imgH = 130;  // Larger image for bigger card
            int imgX = x + (w - imgW) / 2;
            int imgY = y + 50;
            
            // High-quality scaling
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(img, imgX, imgY, imgW, imgH, null);
        }
        
        // Stats
        String[] stats = switch(chr) {
            case SOLDIER -> new String[]{"HP: 5", "DMG: 1", "SPD: 3.0"};
            case MAGE -> new String[]{"HP: 3", "DMG: 2", "SPD: 3.5"};
            case TANK -> new String[]{"HP: 8", "DMG: 1", "SPD: 2.0"};
            case ROGUE -> new String[]{"HP: 4", "DMG: 1", "SPD: 4.0"};
        };
        
        g.setFont(new Font("Monospaced",Font.PLAIN,12));
        g.setColor(new Color(130,170,140));
        for (int i=0; i<stats.length; i++) {
            g.drawString(stats[i],x+12,y+190+i*18);
        }
        
        // Super power
        String superDesc = switch(chr) {
            case SOLDIER -> "RAPID FIRE: 3x attack speed";
            case MAGE -> "POWER SURGE: 2x damage";
            case TANK -> "SHIELD: Temporary invincibility";
            case ROGUE -> "SPEED BOOST: 2x movement speed";
        };
        g.setFont(new Font("Monospaced",Font.BOLD,10));
        g.setColor(chrColor);
        g.drawString("SUPER:", x+12, y+190+stats.length*18+8);
        g.setFont(new Font("Monospaced",Font.PLAIN,10));
        g.setColor(new Color(180,200,160));
        g.drawString(superDesc, x+12, y+190+stats.length*18+20);
        
        // Key badge (top center)
        g.setColor(new Color(30,110,50,35)); g.fillRoundRect(x+w/2-22,y+10,44,22,6,6);
        g.setFont(new Font("Monospaced",Font.BOLD,14)); fm=g.getFontMetrics();
        g.setColor(new Color(60,160,80));
        String badge="["+key+"]"; g.drawString(badge,x+(w-fm.stringWidth(badge))/2,y+28);
    }
    
    // ── SHOP ──
    
    private void drawShop(Graphics2D g) {
        g.setColor(new Color(5,8,12)); g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);
        
        g.setFont(new Font("Monospaced",Font.BOLD,36));
        FontMetrics fm=g.getFontMetrics(); String title="LOADOUT SHOP";
        g.setColor(new Color(255,215,40)); g.drawString(title,(GameFrame.WIDTH-fm.stringWidth(title))/2,60);
        
        g.setFont(new Font("Monospaced",Font.PLAIN,13)); 
        g.setColor(new Color(180,180,120));
        g.drawString("Coins from last run: "+shopCoins,(GameFrame.WIDTH-150)/2,100);
        
        // Shop items
        String[][] items = {
            {"EXTRA HP", "Cost: 30", "Max HP +2"},
            {"POWER", "Cost: 25", "Damage +1"},
            {"SWIFT", "Cost: 20", "Speed +0.5"},
            {"FORTUNE", "Cost: 35", "Coins +25%"}
        };
        
        int itemW=200, itemH=100, totalW=itemW*4+60, startX=(GameFrame.WIDTH-totalW)/2, itemY=150;
        
        for (int i=0; i<items.length; i++) {
            int ix = startX + i*(itemW+20);
            boolean bought = shopBought[i];
            int cost = Integer.parseInt(items[i][1].split(": ")[1]);
            boolean canAfford = shopCoins >= cost && !bought;
            drawShopItem(g, ix, itemY, itemW, itemH, items[i][0], cost, items[i][2], canAfford, bought, i+1);
        }
        
        g.setFont(new Font("Monospaced",Font.PLAIN,13)); g.setColor(new Color(90,140,100));
        String hint="Click items to buy, then ENTER to play"; fm=g.getFontMetrics();
        g.drawString(hint,(GameFrame.WIDTH-fm.stringWidth(hint))/2,itemY+itemH+50);
        
        g.setFont(new Font("Monospaced",Font.BOLD,16));
        g.setColor(new Color(80,255,120)); g.drawString("[ PRESS ENTER ]",GameFrame.WIDTH/2-80,GameFrame.HEIGHT-50);
    }
    
    private void drawShopItem(Graphics2D g, int x, int y, int w, int h, String name, int cost, String desc, boolean affordable, boolean bought, int key) {
        // Card
        g.setColor(bought ? new Color(30,60,30) : new Color(12,28,18)); 
        g.fillRoundRect(x,y,w,h,12,12);
        
        Color cardColor = bought ? new Color(80,200,80) : affordable ? new Color(40,140,60) : new Color(80,80,80);
        g.setColor(cardColor); g.setStroke(new BasicStroke(2)); 
        g.drawRoundRect(x,y,w,h,12,12); g.setStroke(new BasicStroke(1));
        
        // Name
        g.setFont(new Font("Monospaced",Font.BOLD,14)); 
        FontMetrics fm=g.getFontMetrics();
        g.setColor(cardColor); g.drawString(name,x+(w-fm.stringWidth(name))/2,y+25);
        
        // Desc
        g.setFont(new Font("Monospaced",Font.PLAIN,11));
        g.setColor(new Color(130,170,140)); g.drawString(desc,x+10,y+50);
        
        // Cost or BOUGHT
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        if (bought) {
            g.setColor(new Color(100,255,100)); g.drawString("OWNED",x+(w-fm.stringWidth("OWNED"))/2,y+h-15);
        } else {
            g.setColor(affordable ? new Color(255,215,0) : new Color(100,100,100));
            String costStr="$"+cost; g.drawString(costStr,x+(w-fm.stringWidth(costStr))/2,y+h-15);
        }
    }

    // ── IN-GAME SHOP ──
    
    private void drawInGameShop(Graphics2D g) {
        // Semi-transparent overlay
        g.setColor(new Color(0,0,0,200)); 
        g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);
        
        g.setFont(new Font("Monospaced",Font.BOLD,32));
        FontMetrics fm=g.getFontMetrics(); String title="IN-GAME SHOP";
        g.setColor(new Color(255,215,40)); g.drawString(title,(GameFrame.WIDTH-fm.stringWidth(title))/2,80);
        
        g.setFont(new Font("Monospaced",Font.PLAIN,16)); 
        g.setColor(new Color(180,180,120));
        String coinsStr = "Current Coins: " + coins;
        g.drawString(coinsStr,(GameFrame.WIDTH-fm.stringWidth(coinsStr))/2,120);
        
        g.setFont(new Font("Monospaced",Font.PLAIN,12));
        g.setColor(new Color(150,150,150));
        String instr = "Press 1-6 to buy upgrades | ESC to close";
        g.drawString(instr,(GameFrame.WIDTH-fm.stringWidth(instr))/2,GameFrame.HEIGHT-40);
        
        // Shop items - same as wave upgrades but with costs
        String[][] items = {
            {"DAMAGE +1", "Cost: 15", "Increase bullet damage"},
            {"FIRE RATE +", "Cost: 12", "Shoot faster"},
            {"TRIPLE SHOT", "Cost: 25", "Shoot 3 bullets at once"},
            {"ARMOR +5", "Cost: 18", "Block next 5 hits"},
            {"SLOW ENEMIES", "Cost: 20", "Enemies 15% slower"},
            {"CRIT CHANCE", "Cost: 22", "20% crit +2 damage"}
        };
        
        int itemW=180, itemH=80, totalW=itemW*3+40, startX=(GameFrame.WIDTH-totalW)/2, itemY=160;
        for (int i=0; i<items.length; i++) {
            int ix = startX + (i%3)*(itemW+20);
            int iy = itemY + (i/3)*(itemH+20);
            int cost = Integer.parseInt(items[i][1].split(": ")[1]);
            boolean canAfford = coins >= cost;
            drawInGameShopItem(g, ix, iy, itemW, itemH, items[i][0], cost, items[i][2], canAfford, i+1);
        }
    }
    
    private void drawInGameShopItem(Graphics2D g, int x, int y, int w, int h, String name, int cost, String desc, boolean canAfford, int number) {
        // Border
        g.setColor(canAfford ? new Color(100,150,100) : new Color(80,80,80));
        g.fillRect(x,y,w,h);
        g.setColor(canAfford ? new Color(150,200,150) : new Color(120,120,120));
        g.drawRect(x,y,w,h);
        
        // Number
        g.setFont(new Font("Monospaced",Font.BOLD,14));
        g.setColor(canAfford ? Color.WHITE : Color.GRAY);
        g.drawString(""+number, x+10, y+20);
        
        // Name
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        g.drawString(name, x+30, y+20);
        
        // Description
        g.setFont(new Font("Monospaced",Font.PLAIN,10));
        g.drawString(desc, x+10, y+40);
        
        // Cost
        g.setColor(canAfford ? new Color(255,215,0) : new Color(150,150,150));
        String costStr = "$" + cost;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(costStr, x + (w - fm.stringWidth(costStr))/2, y + h - 10);
    }

    private void buyInGameUpgrade(int index) {
        int[] costs = {15, 12, 25, 18, 20, 22};
        if (coins >= costs[index]) {
            coins -= costs[index];
            switch (index) {
                case 0 -> player.damage += 1; // DAMAGE +1
                case 1 -> player.fireRateTicks = Math.max(8, player.fireRateTicks - 5); // FIRE RATE +
                case 2 -> player.multishot = true; // TRIPLE SHOT
                case 3 -> player.armor += 5; // ARMOR +5
                case 4 -> player.enemySlowPercent += 0.15f; // SLOW ENEMIES
                case 5 -> player.critChance += 0.15f; // CRIT CHANCE
            }
            SoundManager.upgradeSelect();
            // Add floating text for purchase
            spawnFloatText(GameFrame.WIDTH/2, GameFrame.HEIGHT/2, "UPGRADE PURCHASED!", 100, 255, 100);
        }
    }

    // ── Background ──

    private void drawBackground(Graphics2D g) {
        // Base ground colour
        g.setColor(new Color(14, 30, 20)); g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);

        // Dirt patch blobs (static, seeded)
        java.util.Random sr = new java.util.Random(42);
        for(int i=0;i<18;i++){
            int bx=sr.nextInt(GameFrame.WIDTH),by=56+sr.nextInt(GameFrame.HEIGHT-80);
            int bw=30+sr.nextInt(50),bh=20+sr.nextInt(30);
            g.setColor(new Color(18,38,22,80)); g.fillOval(bx,by,bw,bh);
        }
        // Grass tufts
        for(int i=0;i<40;i++){
            int gx=sr.nextInt(GameFrame.WIDTH),gy=56+sr.nextInt(GameFrame.HEIGHT-80);
            int ga=(int)(30+20*Math.sin(animTick*0.02+i));
            g.setColor(new Color(30,70,30,ga));
            g.fillRect(gx,gy,2,5); g.fillRect(gx+3,gy+1,2,4); g.fillRect(gx-2,gy+2,2,3);
        }
        // Subtle animated dot grid
        g.setColor(new Color(25,55,38,90));
        for(int x=0;x<GameFrame.WIDTH;x+=32)
            for(int y=56;y<GameFrame.HEIGHT;y+=32)
                g.fillRect(x,y,1,1);
        // Centre worn path (tactical feel)
        g.setColor(new Color(20,42,28,60));
        g.fillOval(GameFrame.WIDTH/2-120,GameFrame.HEIGHT/2-80,240,160);
        // Heavy vignette
        for(int i=0;i<60;i++){g.setColor(new Color(0,0,0,(int)(i*2.0f)));g.drawRect(i,i,GameFrame.WIDTH-i*2,GameFrame.HEIGHT-i*2);}
    }

    // ── MENU ──
private void drawMenu(Graphics2D g) {
    int W = GameFrame.WIDTH, H = GameFrame.HEIGHT;

    g.setColor(new Color(8, 12, 10));
    g.fillRect(0, 0, W, H);

    // Dark grid (tactical map feel)
    g.setColor(new Color(20, 35, 25, 80));
    for (int x = 0; x < W; x += 40) g.drawLine(x, 0, x, H);
    for (int y = 0; y < H; y += 40) g.drawLine(0, y, W, y);

    // Diagonal lines
    g.setColor(new Color(15, 30, 20, 40));
    for (int i = -H; i < W + H; i += 60) g.drawLine(i, 0, i + H, H);

    // Animated floating particles
    long t = System.currentTimeMillis();
    for (int i = 0; i < 20; i++) {
        double px = ((t * (0.008 + i * 0.002) + i * 173) % W);
        double py = 80 + ((t * (0.005 + i * 0.002) + i * 97) % (H - 120));
        int alpha = (int)(50 + 40 * Math.sin(t * 0.002 + i));
        g.setColor(new Color(60, 200, 90, alpha));
        g.fillOval((int) px, (int) py, 2 + (i % 3), 2 + (i % 3));
    }

    // Glow behind title
    for (int r = 120; r > 0; r -= 20) {
        g.setColor(new Color(30, 120, 50, (int)(8 + 4 * Math.sin(animTick * 0.03))));
        g.fillOval(W / 2 - r, 70 - r / 2, r * 2, r);
    }

    // Title
    g.setFont(new Font("Monospaced", Font.BOLD, 54));
    FontMetrics fm = g.getFontMetrics();
    String line1 = "LAST OUTPOST";
    int tx = (W - fm.stringWidth(line1)) / 2;
    g.setColor(new Color(0, 80, 30, 80));
    g.drawString(line1, tx + 4, 124);
    g.setColor(new Color(50, 220, 90));
    g.drawString(line1, tx, 120);

    // Subtitle
    g.setFont(new Font("Monospaced", Font.BOLD, 18));
    fm = g.getFontMetrics();
    String line2 = "LAST STAND";
    g.setColor(new Color(60, 130, 70));
    g.drawString(line2, (W - fm.stringWidth(line2)) / 2, 148);

    // Divider
    g.setColor(new Color(40, 120, 55, 150));
    g.fillRect(W / 2 - 160, 158, 320, 1);

    // Best score
    if (highScore > 0) {
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        fm = g.getFontMetrics();
        String hs = "★  Best: " + highScore + " kills  ★";
        int bw = fm.stringWidth(hs) + 24;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(W / 2 - bw / 2, 164, bw, 22, 6, 6);
        g.setColor(new Color(255, 210, 40));
        g.drawString(hs, (W - fm.stringWidth(hs)) / 2, 180);
    }

    // Controls panel
    int panelX = W / 2 - 170, panelY = 200, panelW = 340, panelH = 150;
    g.setColor(new Color(0, 0, 0, 160));
    g.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);
    g.setColor(new Color(35, 100, 50, 140));
    g.setStroke(new BasicStroke(1.5f));
    g.drawRoundRect(panelX, panelY, panelW, panelH, 12, 12);
    g.setStroke(new BasicStroke(1f));

    g.setFont(new Font("Monospaced", Font.BOLD, 11));
    fm = g.getFontMetrics();
    g.setColor(new Color(50, 160, 70));
    g.drawString("── CONTROLS ──", (W - fm.stringWidth("── CONTROLS ──")) / 2, panelY + 18);

    String[][] ctrls = {
        { "WASD / ARROWS", "Move" },
        { "LEFT CLICK",    "Shoot" },
        { "SPACE",         "Dash" },
        { "E",             "Super Power" },
        { "1 / 2 / 3",     "Pick upgrade" },
        { "ESC",           "Menu" }
    };
    g.setFont(new Font("Monospaced", Font.PLAIN, 12));
    int ry = panelY + 36;
    for (String[] row : ctrls) {
        g.setColor(new Color(80, 210, 110));
        g.drawString(row[0], panelX + 20, ry);
        g.setColor(new Color(140, 190, 150));
        g.drawString(row[1], panelX + 200, ry);
        ry += 20;
    }

    // Blinking ENTER button
    double blink = Math.sin(animTick * 0.07) * 0.5 + 0.5;
    int btnW = 260, btnH = 38, btnX = W / 2 - 130, btnY = 365;
    g.setColor(new Color(30, 150, 60, (int)(blink * 40)));
    g.fillRoundRect(btnX - 6, btnY - 6, btnW + 12, btnH + 12, 14, 14);
    g.setColor(new Color(10, 35, 18));
    g.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
    g.setColor(new Color(50, (int)(160 + blink * 80), 70, (int)(160 + blink * 80)));
    g.setStroke(new BasicStroke(2f));
    g.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
    g.setStroke(new BasicStroke(1f));
    g.setFont(new Font("Monospaced", Font.BOLD, 16));
    fm = g.getFontMetrics();
    String enter = "[ PRESS ENTER TO START ]";
    g.setColor(new Color(70, (int)(200 + blink * 55), 90));
    g.drawString(enter, (W - fm.stringWidth(enter)) / 2, btnY + 25);

    // Credits
    g.setFont(new Font("Monospaced", Font.PLAIN, 11));
    fm = g.getFontMetrics();
    String cred = "Tolentino & Tanchico  |  Prog 2 Finals  |  UPHSD Molino";
    g.setColor(new Color(35, 75, 45));
    g.drawString(cred, (W - fm.stringWidth(cred)) / 2, H - 10);
}

    // ── GAMEPLAY ──

    private void drawGame(Graphics2D g) {
        // Apply screen shake to entire game world
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(screenShakeX, screenShakeY);
        
        drawBackground(g2);

        // Particles (behind entities)
        for (float[] p : particles) {
            float lr=p[4]/p[5]; int a=(int)(lr*210);
            int r = Math.max(0, Math.min(255, (int)p[6]));
            int g_val = Math.max(0, Math.min(255, (int)p[7]));
            int b = Math.max(0, Math.min(255, (int)p[8]));
            g2.setColor(new Color(r, g_val, b, Math.max(0, Math.min(255, a))));
            int sz=Math.max(2,(int)(5*lr)); g2.fillOval((int)p[0]-sz/2,(int)p[1]-sz/2,sz,sz);
        }

        // Damage border vignette
        if (damageFlashTicks>0) {
            int fa=Math.min(180,damageFlashTicks*14);
            for (int i=0;i<22;i++) { g2.setColor(new Color(220,0,0,Math.max(0, Math.min(255, fa-i*8)))); g2.drawRect(i,i,GameFrame.WIDTH-i*2,GameFrame.HEIGHT-i*2); }
        }

        // Entities
        for (Bullet b : bullets) b.draw(g2);
        for (Enemy  e : enemies) e.draw(g2);
        double aim=Math.atan2(mouseY-player.getCenterY(),mouseX-player.getCenterX())+Math.PI/2;
        // Apply character bobbing offset
        Graphics2D playerG = (Graphics2D) g2.create();
        playerG.translate(0, -characterBobOffset);
        player.draw(playerG, aim, characterImages);
        playerG.dispose();

        // Floating texts
        g2.setFont(new Font("Monospaced",Font.BOLD,13));
        for (Object[] ft : floatTexts) {
            int life=(int)ft[3]; float lifeR=life/45f;
            int a=(int)(lifeR*220);
            g2.setColor(new Color((int)ft[5],(int)ft[6],(int)ft[7],Math.min(255,a)));
            g2.drawString((String)ft[4],(int)(float)(Float)ft[0],(int)(float)(Float)ft[1]);
        }

        // Wave banner
        if (waveFlashTicks>0) {
            float alpha=Math.min(1f,waveFlashTicks/40f);
            int bw=320,bh=64,bx=GameFrame.WIDTH/2-bw/2,by=GameFrame.HEIGHT/2-bh/2;
            g2.setColor(new Color(0,200,80,(int)(alpha*25))); g2.fillRoundRect(bx-8,by-8,bw+16,bh+16,24,24);
            g2.setColor(new Color(5,15,10,(int)(alpha*210))); g2.fillRoundRect(bx,by,bw,bh,16,16);
            g2.setColor(new Color(60,200,80,(int)(alpha*160))); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(bx,by,bw,bh,16,16); g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("Monospaced",Font.BOLD,26)); g2.setColor(new Color(100,255,130,(int)(alpha*255)));
            String wt="-- WAVE "+waveManager.getCurrentWave()+" --"; FontMetrics fm=g2.getFontMetrics();
            g2.drawString(wt,GameFrame.WIDTH/2-fm.stringWidth(wt)/2,GameFrame.HEIGHT/2+9);
        }
        
        g2.dispose(); // Clean up the transformed graphics

        // Off-screen enemy danger arrows
        drawDangerArrows(g);

        // Draw UI elements without screen shake
        drawCrosshair(g, mouseX, mouseY);
        drawHUD(g);
        drawAbilityIcons(g);
    }

    /** Draw red arrows at screen edges pointing toward off-screen enemies. */
    private void drawDangerArrows(Graphics2D g) {
        int margin=36, cx=GameFrame.WIDTH/2, cy=GameFrame.HEIGHT/2;
        for(Enemy e : enemies) {
            float ex2=e.getCenterX(), ey2=e.getCenterY();
            if(ex2>=0&&ex2<=GameFrame.WIDTH&&ey2>=52&&ey2<=GameFrame.HEIGHT) continue;
            // Direction from screen center to enemy
            float dx=ex2-cx, dy=ey2-cy;
            float len=(float)Math.sqrt(dx*dx+dy*dy); if(len==0) continue;
            dx/=len; dy/=len;
            // Clamp arrow position to screen edge
            float ax=cx+dx*(GameFrame.WIDTH/2f-margin);
            float ay=cy+dy*(GameFrame.HEIGHT/2f-margin);
            ax=Math.max(margin,Math.min(GameFrame.WIDTH-margin,ax));
            ay=Math.max(margin+52,Math.min(GameFrame.HEIGHT-margin,ay));
            // Draw arrow
            double ang=Math.atan2(dy,dx);
            java.awt.geom.AffineTransform old2=g.getTransform();
            g.translate(ax,ay); g.rotate(ang);
            int a=e.elite?200:140;
            g.setColor(new Color(255,60,60,a));
            g.setStroke(new BasicStroke(2));
            g.fillPolygon(new int[]{0,-8,-8},new int[]{0,-5,5},3);
            g.drawLine(-8,0,-16,0);
            g.setTransform(old2); g.setStroke(new BasicStroke(1));
        }
    }

    /** Draw circular ability cooldown icons (bottom right). */
    private void drawAbilityIcons(Graphics2D g) {
        int baseX=GameFrame.WIDTH-120, baseY=GameFrame.HEIGHT-46;

        // DASH circle icon
        float dashR=1f-(float)player.dashCooldown/Player.DASH_COOLDOWN_MAX;
        drawCircleIcon(g, baseX, baseY, dashR, player.isDashing(),
            player.dashCooldown==0?new Color(0,220,255):new Color(0,140,200), "DASH");

        // SUPER circle icon
        float superR=1f-(float)player.superCooldown/Player.SUPER_COOLDOWN_MAX;
        String superLabel=switch(player.character){
            case SOLDIER->"FIRE"; case MAGE->"MAGE"; case TANK->"SHLD"; case ROGUE->"SPNT";
        };
        Color superCol=switch(player.character){
            case SOLDIER->new Color(255,220,0); case MAGE->new Color(220,60,255);
            case TANK->new Color(60,220,60); case ROGUE->new Color(255,140,0);
        };
        drawCircleIcon(g, baseX+60, baseY, superR, player.superActive, superCol, superLabel);
    }

    private void drawCircleIcon(Graphics2D g, int x, int y, float fill, boolean active, Color col, String label) {
        int r=20;
        // Background circle
        g.setColor(new Color(0,0,0,140)); g.fillOval(x-r,y-r,r*2,r*2);
        // Fill arc (clockwise from top)
        g.setColor(active?col:col.darker());
        g.setStroke(new BasicStroke(3));
        g.drawArc(x-r,y-r,r*2,r*2,90,-(int)(360*fill));
        // Ready glow
        if(fill>=1f){
            g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),60));
            g.fillOval(x-r,y-r,r*2,r*2);
        }
        // Border
        g.setColor(new Color(0,0,0,80)); g.setStroke(new BasicStroke(1));
        g.drawOval(x-r,y-r,r*2,r*2);
        // Label
        g.setFont(new Font("Monospaced",Font.BOLD,8));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(fill>=1f?col:new Color(150,150,150));
        g.drawString(label,x-fm.stringWidth(label)/2,y+3);
        g.setStroke(new BasicStroke(1));
    }

    private void drawCrosshair(Graphics2D g, int cx, int cy) {
        // Expands on shoot, shrinks back
        float expand = crosshairPulse / 8f;
        int gap = (int)(6 + expand*8), len = (int)(9 - expand*3);
        float alpha = 1f - expand*0.3f;
        g.setStroke(new BasicStroke(1.5f + expand));
        // Shadow
        g.setColor(new Color(0,0,0,80));
        g.drawLine(cx-gap-len-1,cy,cx-gap-1,cy); g.drawLine(cx+gap+1,cy,cx+gap+len+1,cy);
        g.drawLine(cx,cy-gap-len-1,cx,cy-gap-1); g.drawLine(cx,cy+gap+1,cx,cy+gap+len+1);
        // Main lines
        int r=(int)(80),gv=(int)(200+55*alpha),b=(int)(120);
        g.setColor(new Color(r,gv,b,(int)(200*alpha)));
        g.drawLine(cx-gap-len,cy,cx-gap,cy); g.drawLine(cx+gap,cy,cx+gap+len,cy);
        g.drawLine(cx,cy-gap-len,cx,cy-gap); g.drawLine(cx,cy+gap,cx,cy+gap+len);
        // Centre dot (red when pulsing)
        g.setColor(crosshairPulse>0?new Color(255,80,80,200):new Color(80,255,120,180));
        g.fillOval(cx-2,cy-2,4,4);
        // Outer ring on pulse
        if(crosshairPulse>4){
            float ringR=expand*18;
            g.setColor(new Color(255,200,80,(int)(crosshairPulse*18)));
            g.setStroke(new BasicStroke(1));
            g.drawOval(cx-(int)ringR,cy-(int)ringR,(int)(ringR*2),(int)(ringR*2));
        }
        g.setStroke(new BasicStroke(1));
    }

    // ── HUD ──

    private void drawHUD(Graphics2D g) {
        // ── Top bar ──
        g.setColor(new Color(0,0,0,190)); g.fillRect(0,0,GameFrame.WIDTH,52);
        g.setColor(new Color(40,120,60,60)); g.fillRect(0,50,GameFrame.WIDTH,2);

        // Survival timer (center-top) like EWS
        int secs = survivalTicks/60, mins=secs/60; secs%=60;
        String timer = String.format("%02d:%02d",mins,secs);
        g.setFont(new Font("Monospaced",Font.BOLD,22));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(0,0,0,100)); g.fillRoundRect(GameFrame.WIDTH/2-46,7,92,36,8,8);
        g.setColor(new Color(100,220,130)); g.drawString(timer,GameFrame.WIDTH/2-fm.stringWidth(timer)/2,33);

        // Wave badge (left)
        g.setFont(new Font("Monospaced",Font.BOLD,13));
        g.setColor(new Color(0,0,0,110)); g.fillRoundRect(8,8,115,34,6,6);
        g.setColor(new Color(60,180,80,130)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(8,8,115,34,6,6); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(80,220,100)); g.drawString("WAVE "+waveManager.getCurrentWave(),16,31);

        // Kill counter (right side, skull icon style)
        g.setFont(new Font("Monospaced",Font.BOLD,13));
        String ks = totalKills+" kills";
        fm=g.getFontMetrics(); int ksw=fm.stringWidth(ks);
        g.setColor(new Color(0,0,0,110)); g.fillRoundRect(GameFrame.WIDTH-ksw-24,8,ksw+16,34,6,6);
        g.setColor(killFlashTicks>0?new Color(255,230,0):new Color(160,200,160));
        g.drawString(ks,GameFrame.WIDTH-ksw-16,31);

        // Coin display (right of wave badge)
        g.setFont(new Font("Monospaced",Font.BOLD,13));
        String cs="$ "+coins;
        fm=g.getFontMetrics();
        g.setColor(new Color(255,215,40)); g.drawString(cs,134,25);
        g.setFont(new Font("Monospaced",Font.PLAIN,10));
        g.setColor(new Color(200,200,150)); g.drawString("B: Shop",134,40);

        // ── Bottom HUD bar ──
        g.setColor(new Color(0,0,0,170)); g.fillRect(0,GameFrame.HEIGHT-48,GameFrame.WIDTH,48);
        g.setColor(new Color(40,120,60,50)); g.fillRect(0,GameFrame.HEIGHT-48,GameFrame.WIDTH,1);

        int barX=10, barY=GameFrame.HEIGHT-36, barW=180, barH=16;

        // HP bar
        g.setColor(new Color(60,0,0)); g.fillRoundRect(barX,barY,barW,barH,6,6);
        float hpR=(float)player.hp/player.maxHp;
        Color hpC=hpR>0.6f?new Color(60,210,80):hpR>0.3f?new Color(220,160,0):new Color(220,40,40);
        if(hpR<0.3f){double p2=0.7+0.3*Math.sin(animTick*0.2);hpC=new Color((int)(210*p2),40,40);}
        g.setColor(hpC); g.fillRoundRect(barX,barY,(int)(barW*hpR),barH,6,6);
        g.setColor(new Color(255,255,255,35)); g.fillRoundRect(barX,barY,(int)(barW*hpR),barH/2,6,6);
        g.setFont(new Font("Monospaced",Font.BOLD,11));
        g.setColor(new Color(240,240,240,200)); g.drawString("HP "+player.hp+"/"+player.maxHp,barX+4,barY+12);
        g.setColor(new Color(120,120,120,80)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(barX,barY,barW,barH,6,6); g.setStroke(new BasicStroke(1));

        // XP bar (below HP)
        int xpX=barX, xpY=barY+20, xpW=barW, xpH=8;
        g.setColor(new Color(0,0,60)); g.fillRoundRect(xpX,xpY,xpW,xpH,4,4);
        float xpR=(float)xp/xpToNext;
        g.setColor(new Color(60,100,255)); g.fillRoundRect(xpX,xpY,(int)(xpW*xpR),xpH,4,4);
        g.setColor(new Color(100,140,255,60)); g.fillRoundRect(xpX,xpY,(int)(xpW*xpR),xpH/2,4,4);

        // Level badge
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        g.setColor(new Color(0,0,0,120)); g.fillRoundRect(barX+barW+6,barY,40,36,6,6);
        g.setColor(new Color(80,120,255)); g.drawString("Lv",barX+barW+10,barY+13);
        g.setFont(new Font("Monospaced",Font.BOLD,14)); g.setColor(new Color(140,170,255)); g.drawString(""+level,barX+barW+13,barY+30);

        // Armor indicator (if > 0)
        if (player.armor > 0) {
            g.setFont(new Font("Monospaced",Font.BOLD,12));
            g.setColor(new Color(100,180,255)); g.drawString("["+player.armor+"]",barX+barW+58,barY+13);
        }

        // Dash cooldown indicator
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        float dashReady = 1f - (float)player.dashCooldown / Player.DASH_COOLDOWN_MAX;
        if (dashReady < 1f) {
            g.setColor(new Color(100,180,255)); g.drawString("DASH", barX+barW+100, barY+13);
            // Small cooldown bar
            g.setColor(new Color(40,80,120)); g.fillRect(barX+barW+100,barY+16,40,4);
            g.setColor(new Color(0,200,255)); g.fillRect(barX+barW+100,barY+16,(int)(40*dashReady),4);
        } else {
            g.setColor(new Color(0,255,150)); g.drawString("DASH", barX+barW+100, barY+13);
            g.setColor(new Color(0,255,150)); g.fillRect(barX+barW+100,barY+16,40,4);
        }

        // Super power cooldown indicator
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        float superReady = 1f - (float)player.superCooldown / Player.SUPER_COOLDOWN_MAX;
        String superName = switch(player.character) {
            case SOLDIER -> "RAPID";
            case MAGE -> "POWER";
            case TANK -> "SHIELD";
            case ROGUE -> "SPEED";
        };
        if (superReady < 1f || player.superActive) {
            Color superColor = player.superActive ? Color.YELLOW : new Color(150,100,200);
            g.setColor(superColor); g.drawString(superName, barX+barW+100, barY+25);
            // Small cooldown bar
            g.setColor(new Color(60,40,80)); g.fillRect(barX+barW+100,barY+28,40,4);
            g.setColor(superColor); g.fillRect(barX+barW+100,barY+28,(int)(40*superReady),4);
        } else {
            g.setColor(new Color(200,150,255)); g.drawString(superName, barX+barW+100, barY+25);
            g.setColor(new Color(200,150,255)); g.fillRect(barX+barW+100,barY+28,40,4);
        }
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        String en=enemies.size()+" on field";
        fm=g.getFontMetrics();
        g.setColor(new Color(200,100,100)); g.drawString(en,GameFrame.WIDTH-fm.stringWidth(en)-12,GameFrame.HEIGHT-14);

        // Wave progress bar (thin bar at very bottom)
        int wpW=(int)(GameFrame.WIDTH*waveManager.getWaveProgress());
        g.setColor(new Color(60,200,80,60)); g.fillRect(0,GameFrame.HEIGHT-3,wpW,3);
    }

    // ── UPGRADE Screen ──

    private void drawUpgrade(Graphics2D g) {
        drawBackground(g);
        g.setColor(new Color(0,0,0,210)); g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);

        // Header
        double pulse=Math.sin(animTick*0.07)*0.5+0.5;
        g.setFont(new Font("Monospaced",Font.BOLD,30));
        FontMetrics fm=g.getFontMetrics(); String hdr="-- LEVEL UP! --";
        int hx=(GameFrame.WIDTH-fm.stringWidth(hdr))/2;
        g.setColor(new Color(0,200,80,(int)(35+pulse*25))); g.drawString(hdr,hx-2,96); g.drawString(hdr,hx+2,96);
        g.setColor(new Color(80,255,120)); g.drawString(hdr,hx,94);

        g.setFont(new Font("Monospaced",Font.PLAIN,15)); g.setColor(new Color(120,180,130));
        String sub="Level "+level+" reached -- pick an upgrade"; fm=g.getFontMetrics();
        g.drawString(sub,(GameFrame.WIDTH-fm.stringWidth(sub))/2,122);

        // Divider
        g.setColor(new Color(40,120,60,100)); g.fillRect(200,132,400,1);

        // Cards
        int cardW=200,cardH=160,totalW=cardW*3+40,startX=(GameFrame.WIDTH-totalW)/2,cardY=148;
        for (int i=0;i<3;i++) drawUpgradeCard(g,startX+i*(cardW+20),cardY,cardW,cardH,upgradeOptions[i],i+1,hoveredCard==i);

        g.setFont(new Font("Monospaced",Font.PLAIN,13)); g.setColor(new Color(90,140,100));
        String hint="Press [ 1 ] [ 2 ] [ 3 ]  or  click a card"; fm=g.getFontMetrics();
        g.drawString(hint,(GameFrame.WIDTH-fm.stringWidth(hint))/2,cardY+cardH+30);

        // Stats row
        g.setColor(new Color(0,0,0,130)); g.fillRoundRect(170,GameFrame.HEIGHT-44,460,28,8,8);
        g.setFont(new Font("Monospaced",Font.PLAIN,12)); g.setColor(new Color(100,180,110));
        String stats="HP "+player.hp+"/"+player.maxHp
                +"  DMG "+player.damage
                +"  SPD "+String.format("%.1f",player.speed)
                +"  $ "+coins
                +"  Kills "+totalKills;
        fm=g.getFontMetrics(); g.drawString(stats,(GameFrame.WIDTH-fm.stringWidth(stats))/2,GameFrame.HEIGHT-24);
    }

    private void drawUpgradeCard(Graphics2D g, int cx, int cy, int w, int h, String text, int key, boolean hov) {
        String[] parts=text.split("\\|");
        String tag=parts[0], desc=parts.length>1?parts[1]:"", icon=parts.length>2?parts[2]:"*";

        // Rarity colour: power/multi/crit = gold (rare), heal/hp = green (common), others = blue
        boolean rare=tag.equals("POWER")||tag.equals("MULTI")||tag.equals("CRIT")||tag.equals("COINS");
        boolean uncommon=tag.equals("RAPID")||tag.equals("SLOW")||tag.equals("REGEN")||tag.equals("WIDE");
        Color rarityCol=rare?new Color(255,200,40):uncommon?new Color(80,160,255):new Color(60,200,80);

        int lift=hov?6:0; cy-=lift;

        // Outer glow
        if(hov||rare){
            g.setColor(new Color(rarityCol.getRed(),rarityCol.getGreen(),rarityCol.getBlue(),hov?50:20));
            g.fillRoundRect(cx-6,cy-6,w+12,h+12,18,18);
        }
        // Card bg — slightly tinted by rarity
        int bgR=rare?20:uncommon?14:12, bgG=rare?30:uncommon?28:28, bgB=rare?10:uncommon?38:18;
        g.setColor(hov?new Color(bgR+6,bgG+12,bgB+4):new Color(bgR,bgG,bgB));
        g.fillRoundRect(cx,cy,w,h,12,12);

        // Top accent bar (rarity coloured)
        g.setColor(new Color(rarityCol.getRed(),rarityCol.getGreen(),rarityCol.getBlue(),hov?200:120));
        g.fillRoundRect(cx,cy,w,5,4,4);

        // Border
        g.setColor(hov?new Color(rarityCol.getRed(),rarityCol.getGreen(),rarityCol.getBlue(),220):new Color(rarityCol.getRed(),rarityCol.getGreen(),rarityCol.getBlue(),100));
        g.setStroke(new BasicStroke(hov?2:1.5f)); g.drawRoundRect(cx,cy,w,h,12,12); g.setStroke(new BasicStroke(1));

        // Rarity label (top right)
        g.setFont(new Font("Monospaced",Font.BOLD,9));
        String rlabel=rare?"RARE":uncommon?"RARE":"";
        if(!rlabel.isEmpty()){g.setColor(rarityCol);FontMetrics rfm=g.getFontMetrics();g.drawString(rlabel,cx+w-rfm.stringWidth(rlabel)-6,cy+13);}

        // Icon (large, rarity coloured)
        g.setFont(new Font("Monospaced",Font.BOLD,22));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(hov?rarityCol.brighter():rarityCol);
        g.drawString(icon,cx+(w-fm.stringWidth(icon))/2,cy+36);

        // Tag
        g.setFont(new Font("Monospaced",Font.BOLD,16)); fm=g.getFontMetrics();
        g.setColor(hov?rarityCol.brighter():new Color(Math.min(255,rarityCol.getRed()+60),Math.min(255,rarityCol.getGreen()+60),Math.min(255,rarityCol.getBlue()+60)));
        g.drawString(tag,cx+(w-fm.stringWidth(tag))/2,cy+58);

        // Desc (word-wrap)
        g.setFont(new Font("Monospaced",Font.PLAIN,12)); g.setColor(hov?new Color(200,235,200):new Color(130,170,140));
        String[] words=desc.split(" "); StringBuilder line=new StringBuilder(); int ly=cy+80;
        for (String word:words) {
            String test=line+word+" ";
            if(g.getFontMetrics().stringWidth(test)>w-16&&!line.isEmpty()){
                String l=line.toString().trim(); g.drawString(l,cx+(w-g.getFontMetrics().stringWidth(l))/2,ly); ly+=17; line=new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if(!line.toString().trim().isEmpty()){String l=line.toString().trim();g.drawString(l,cx+(w-g.getFontMetrics().stringWidth(l))/2,ly);}

        // Key badge
        g.setColor(hov?new Color(60,200,80,55):new Color(30,110,50,35)); g.fillRoundRect(cx+w/2-20,cy+h-28,40,20,6,6);
        g.setFont(new Font("Monospaced",Font.BOLD,14)); fm=g.getFontMetrics();
        g.setColor(hov?new Color(100,255,130):new Color(60,160,80));
        String badge="["+key+"]"; g.drawString(badge,cx+(w-fm.stringWidth(badge))/2,cy+h-12);
    }

    // ── GAME OVER ──

    private void drawGameOver(Graphics2D g) {
        // Dark overlay on frozen game
        g.setColor(new Color(5,8,8)); g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);
        // Red scatter particles
        long t=System.currentTimeMillis();
        for (int i=0;i<20;i++){
            double ox=((t*(0.01+i*0.004)+i*113)%GameFrame.WIDTH);
            double oy=((t*(0.012+i*0.003)+i*79)%GameFrame.HEIGHT);
            g.setColor(new Color(180,30,30,(int)(50+40*Math.sin(t*0.002+i)))); g.fillOval((int)ox,(int)oy,2,2);
        }

        // GAME OVER title
        g.setFont(new Font("Monospaced",Font.BOLD,62));
        FontMetrics fm=g.getFontMetrics(); String got="GAME OVER";
        int gx=(GameFrame.WIDTH-fm.stringWidth(got))/2;
        double pulse=Math.sin(animTick*0.05)*0.5+0.5;
        for(int gl=14;gl>0;gl-=4){g.setColor(new Color(160,0,0,(int)(pulse*25)));g.drawString(got,gx-gl/2,186+gl/2);}
        g.setColor(new Color(210,35,35)); g.drawString(got,gx,184);

        // Stats panel
        g.setColor(new Color(0,0,0,170)); g.fillRoundRect(200,205,400,148,14,14);
        g.setColor(new Color(100,20,20,120)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(200,205,400,148,14,14); g.setStroke(new BasicStroke(1));

        g.setFont(new Font("Monospaced",Font.BOLD,18));
        int secs2=survivalTicks/60,mins2=secs2/60; secs2%=60;
        String[] statLines={
            "Kills:          "+totalKills,
            "Survived:       "+String.format("%02d:%02d",mins2,secs2),
            "Coins earned:   "+coins,
            "Wave reached:   "+waveManager.getCurrentWave(),
        };
        int sy=232;
        for(int si=0;si<statLines.length;si++){
            int revealAt=si*22; // stagger reveal
            if(statRevealTick<revealAt) break;
            float lineAlpha=Math.min(1f,(statRevealTick-revealAt)/15f);
            g.setColor(new Color(255,255,255,(int)(lineAlpha*220)));
            fm=g.getFontMetrics(); g.drawString(statLines[si],(GameFrame.WIDTH-fm.stringWidth(statLines[si]))/2,sy);
            sy+=28;
        }

        // High score
        g.setFont(new Font("Monospaced",Font.BOLD,15));
        g.setColor(new Color(255,215,40));
        String hs="Best: "+highScore+" kills"; fm=g.getFontMetrics();
        g.drawString(hs,(GameFrame.WIDTH-fm.stringWidth(hs))/2,sy+6);

        // Buttons - Spread them out much wider
        g.setColor(new Color(100,20,20,80)); g.fillRect(100,370,600,1);
        long bp=(System.currentTimeMillis()/600)%2;
        g.setFont(new Font("Monospaced",Font.BOLD,16));
        
        // Play again button (left side)
        int btn1X = 140;
        int btn1W = 180;
        g.setColor(new Color(0,0,0,130)); g.fillRoundRect(btn1X,383,btn1W,34,8,8);
        g.setColor(bp==0?new Color(60,200,80):new Color(40,140,50)); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(btn1X,383,btn1W,34,8,8); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(100,240,120)); g.drawString("[ ENTER ]",(btn1X+btn1W/2)-35,406);
        
        // Menu button (right side)
        int btn2X = 480;
        int btn2W = 180;
        g.setColor(new Color(0,0,0,130)); g.fillRoundRect(btn2X,383,btn2W,34,8,8);
        g.setColor(new Color(80,100,140,140)); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(btn2X,383,btn2W,34,8,8); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(140,160,200)); g.drawString("[ ESC ]",(btn2X+btn2W/2)-28,406);
        
        // Labels - positioned below buttons with more space
        g.setFont(new Font("Monospaced",Font.PLAIN,12));
        g.setColor(new Color(80,160,90)); 
        String playLabel="Play again"; fm=g.getFontMetrics(); 
        g.drawString(playLabel,(btn1X+btn1W/2)-(fm.stringWidth(playLabel)/2),435);
        g.setColor(new Color(100,120,160)); 
        String menuLabel="Main menu"; fm=g.getFontMetrics(); 
        g.drawString(menuLabel,(btn2X+btn2W/2)-(fm.stringWidth(menuLabel)/2),435);
    }

    // ── Util ──

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm=g.getFontMetrics(); g.drawString(text,(GameFrame.WIDTH-fm.stringWidth(text))/2,y);
    }

    // ─────────────────────────────────────────────────────────────
    //  Input
    // ─────────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int k=e.getKeyCode();
        
        if (state==GameState.CHARACTER_SELECT) {
            if(k==KeyEvent.VK_1) { selectedCharacter=Player.CharacterType.SOLDIER; state=GameState.SHOP; }
            if(k==KeyEvent.VK_2) { selectedCharacter=Player.CharacterType.MAGE; state=GameState.SHOP; }
            if(k==KeyEvent.VK_3) { selectedCharacter=Player.CharacterType.TANK; state=GameState.SHOP; }
            if(k==KeyEvent.VK_4) { selectedCharacter=Player.CharacterType.ROGUE; state=GameState.SHOP; }
        }
        
        if (state==GameState.SHOP) {
            if(k==KeyEvent.VK_1) tryBuyShopItem(0);
            if(k==KeyEvent.VK_2) tryBuyShopItem(1);
            if(k==KeyEvent.VK_3) tryBuyShopItem(2);
            if(k==KeyEvent.VK_4) tryBuyShopItem(3);
            if(k==KeyEvent.VK_ENTER) { startNewGame(); state=GameState.PLAYING; }
        }
        
        if (state==GameState.MENU && k==KeyEvent.VK_ENTER) { state=GameState.CHARACTER_SELECT; }
        if (state==GameState.PLAYING) {
            if(k==KeyEvent.VK_W||k==KeyEvent.VK_UP)    player.up=true;
            if(k==KeyEvent.VK_S||k==KeyEvent.VK_DOWN)  player.down=true;
            if(k==KeyEvent.VK_A||k==KeyEvent.VK_LEFT)  player.left=true;
            if(k==KeyEvent.VK_D||k==KeyEvent.VK_RIGHT) player.right=true;
            if(k==KeyEvent.VK_SPACE) player.tryDash();
            if(k==KeyEvent.VK_E) player.trySuperPower();
            if(k==KeyEvent.VK_B) state = GameState.IN_GAME_SHOP; // Open in-game shop
        }
        if (state==GameState.IN_GAME_SHOP) {
            if(k==KeyEvent.VK_1) buyInGameUpgrade(0);
            if(k==KeyEvent.VK_2) buyInGameUpgrade(1);
            if(k==KeyEvent.VK_3) buyInGameUpgrade(2);
            if(k==KeyEvent.VK_4) buyInGameUpgrade(3);
            if(k==KeyEvent.VK_5) buyInGameUpgrade(4);
            if(k==KeyEvent.VK_6) buyInGameUpgrade(5);
            if(k==KeyEvent.VK_ESCAPE) { state = GameState.PLAYING; return; } // Close shop
        }
        if (state==GameState.UPGRADE) {
            if(k==KeyEvent.VK_1) applyUpgrade(0);
            if(k==KeyEvent.VK_2) applyUpgrade(1);
            if(k==KeyEvent.VK_3) applyUpgrade(2);
        }
        if (state==GameState.GAME_OVER) {
            if(k==KeyEvent.VK_ENTER)  { state=GameState.CHARACTER_SELECT; }
            if(k==KeyEvent.VK_ESCAPE) state=GameState.MENU;
        }
        if (k==KeyEvent.VK_ESCAPE && state==GameState.PLAYING) state=GameState.MENU;
    }
    
    private void tryBuyShopItem(int index) {
        int[] costs = {30, 25, 20, 35};
        if (shopCoins >= costs[index] && !shopBought[index]) {
            shopCoins -= costs[index];
            shopBought[index] = true;
            SoundManager.shoot();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k=e.getKeyCode();
        if(k==KeyEvent.VK_W||k==KeyEvent.VK_UP)    player.up=false;
        if(k==KeyEvent.VK_S||k==KeyEvent.VK_DOWN)  player.down=false;
        if(k==KeyEvent.VK_A||k==KeyEvent.VK_LEFT)  player.left=false;
        if(k==KeyEvent.VK_D||k==KeyEvent.VK_RIGHT) player.right=false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override public void mousePressed(MouseEvent e)  { if(e.getButton()==MouseEvent.BUTTON1) mouseDown=true; }
    @Override public void mouseReleased(MouseEvent e) { if(e.getButton()==MouseEvent.BUTTON1) mouseDown=false; }
    @Override public void mouseClicked(MouseEvent e)  {
        if(state==GameState.UPGRADE&&e.getButton()==MouseEvent.BUTTON1&&hoveredCard>=0) applyUpgrade(hoveredCard);
        
        // Character select click
        if(state==GameState.CHARACTER_SELECT&&e.getButton()==MouseEvent.BUTTON1) {
            Player.CharacterType[] chars = Player.CharacterType.values();
            int cardW=200, cardH=280, totalW=cardW*4+60, startX=(GameFrame.WIDTH-totalW)/2, cardY=100;
            for (int i=0; i<chars.length; i++) {
                int cx = startX + i*(cardW+20);
                if(e.getX()>=cx&&e.getX()<=cx+cardW&&e.getY()>=cardY&&e.getY()<=cardY+cardH) {
                    selectedCharacter=chars[i];
                    state=GameState.SHOP;
                    break;
                }
            }
        }
        
        // Shop click
        if(state==GameState.SHOP&&e.getButton()==MouseEvent.BUTTON1) {
            int itemW=200, itemH=100, totalW=itemW*4+60, startX=(GameFrame.WIDTH-totalW)/2, itemY=150;
            for (int i=0; i<4; i++) {
                int ix = startX + i*(itemW+20);
                if(e.getX()>=ix&&e.getX()<=ix+itemW&&e.getY()>=itemY&&e.getY()<=itemY+itemH) {
                    tryBuyShopItem(i);
                    break;
                }
            }
        }
    }
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
    @Override public void mouseMoved(MouseEvent e)    { mouseX=e.getX(); mouseY=e.getY(); updateCardHover(e.getX(),e.getY()); updateCharCardHover(e.getX(),e.getY()); }
    @Override public void mouseDragged(MouseEvent e)  { mouseX=e.getX(); mouseY=e.getY(); updateCardHover(e.getX(),e.getY()); updateCharCardHover(e.getX(),e.getY()); }

    private void updateCharCardHover(int mx, int my) {
        if(state!=GameState.CHARACTER_SELECT){hoveredCharCard=-1;return;}
        int cardW=200,cardH=280,totalW=cardW*4+60,startX=(GameFrame.WIDTH-totalW)/2,cardY=100;
        hoveredCharCard=-1;
        Player.CharacterType[] chars=Player.CharacterType.values();
        for(int i=0;i<chars.length;i++){int cx=startX+i*(cardW+20);if(mx>=cx&&mx<=cx+cardW&&my>=cardY&&my<=cardY+cardH){hoveredCharCard=i;break;}}
    }

    private void updateCardHover(int mx, int my) {
        if(state!=GameState.UPGRADE){hoveredCard=-1;return;}
        int cw=200,ch=160,sx=(GameFrame.WIDTH-(cw*3+40))/2,sy=148;
        hoveredCard=-1;
        for(int i=0;i<3;i++){int cx=sx+i*(cw+20);if(mx>=cx&&mx<=cx+cw&&my>=sy-6&&my<=sy+ch){hoveredCard=i;break;}}
    }
}