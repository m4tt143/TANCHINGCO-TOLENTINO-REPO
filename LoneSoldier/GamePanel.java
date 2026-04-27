import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GamePanel - Endless Waves Survival style.
 * Continuous spawning, survival timer, coins, XP/level, upgrade cards.
 */
public class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {

    // ── States ───────────────────────────────────────────────────
    public enum GameState { MENU, PLAYING, UPGRADE, GAME_OVER }
    private GameState state = GameState.MENU;

    // ── Game Objects ─────────────────────────────────────────────
    private Player       player;
    private List<Enemy>  enemies;
    private List<Bullet> bullets;
    private WaveManager  waveManager;

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

    // ── Particles [x,y,vx,vy,life,maxLife,r,g,b] ─────────────────
    private final List<float[]> particles = new ArrayList<>();

    // ── Floating text [x,y,vx,vy,life,text,r,g,b] ────────────────
    private final List<Object[]> floatTexts = new ArrayList<>();

    // ── Mouse ─────────────────────────────────────────────────────
    private int  mouseX = GameFrame.WIDTH/2, mouseY = GameFrame.HEIGHT/2;
    private boolean mouseDown = false;

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
        initObjects();
        gameTimer = new Timer(1000/FPS, e -> { update(); repaint(); });
        gameTimer.start();
    }

    // ── Init ─────────────────────────────────────────────────────

    private void initObjects() {
        float cx = GameFrame.WIDTH/2f - Player.SIZE/2f;
        float cy = GameFrame.HEIGHT/2f - Player.SIZE/2f;
        player      = new Player(cx, cy);
        enemies     = new ArrayList<>();
        bullets     = new ArrayList<>();
        waveManager = new WaveManager();
        coins = 0; totalKills = 0; xp = 0; level = 1; xpToNext = 10;
        survivalTicks = 0; damageFlashTicks = 0; waveFlashTicks = 0; lastWaveShown = 0;
        particles.clear(); floatTexts.clear();
    }

    private void startNewGame() { initObjects(); state = GameState.PLAYING; }

    // ── Update ────────────────────────────────────────────────────

    private void update() {
        if (state != GameState.PLAYING) { animTick++; return; }

        animTick++;
        survivalTicks++;

        // Tick particles
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
        if (mouseDown && player.canShoot()) { fireTowardMouse(); player.resetCooldown(); SoundManager.shoot(); }

        // Bullet collisions
        List<Bullet> deadBullets = new ArrayList<>();
        List<Enemy>  deadEnemies = new ArrayList<>();
        for (Bullet b : bullets) {
            b.update();
            if (b.isOffScreen()) { deadBullets.add(b); continue; }
            for (Enemy e : enemies) {
                if (!deadEnemies.contains(e) && b.getBounds().intersects(e.getBounds())) {
                    e.hp -= b.damage;
                    deadBullets.add(b);
                    if (e.hp <= 0) {
                        deadEnemies.add(e);
                        totalKills++; killFlashTicks = 10; coins += e.coinDrop;
                        gainXP(e.elite ? 5 : 1);
                        SoundManager.enemyDie();
                        spawnDeathParticles(e);
                        spawnFloatText(e.getCenterX(), e.getCenterY()-10, "+" + e.coinDrop, 255,215,0);
                    } else {
                        SoundManager.enemyHit();
                        spawnHitSparks(e);
                    }
                    break;
                }
            }
        }
        bullets.removeAll(deadBullets);
        enemies.removeAll(deadEnemies);

        // Enemy → player collision
        List<Enemy> hitPlayer = new ArrayList<>();
        for (Enemy e : enemies) {
            e.update(player.getCenterX(), player.getCenterY());
            if (e.getBounds().intersects(player.getBounds())) {
                player.takeDamage(e.elite ? 2 : 1);
                hitPlayer.add(e);
                damageFlashTicks = 14;
                SoundManager.playerHit();
                spawnFloatText(player.getCenterX(), player.getCenterY()-20, "-" + (e.elite?2:1)+" HP", 255,60,60);
            }
        }
        enemies.removeAll(hitPlayer);

        if (damageFlashTicks > 0) damageFlashTicks--;
        if (killFlashTicks   > 0) killFlashTicks--;

        // Check upgrade (every level-up)
        // (handled in gainXP)

        // Death
        if (player.hp <= 0) {
            if (totalKills > highScore) highScore = totalKills;
            state = GameState.GAME_OVER;
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
        bullets.add(new Bullet(player.getCenterX(),player.getCenterY(),nx,ny,player.damage));
        if (player.multishot) {
            float a=0.28f;
            float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
            bullets.add(new Bullet(player.getCenterX(),player.getCenterY(),nx*ca-ny*sa,nx*sa+ny*ca,player.damage));
            float cb=(float)Math.cos(-a),sb=(float)Math.sin(-a);
            bullets.add(new Bullet(player.getCenterX(),player.getCenterY(),nx*cb-ny*sb,nx*sb+ny*cb,player.damage));
        }
    }

    // ── Particles ─────────────────────────────────────────────────

    private void spawnDeathParticles(Enemy e) {
        float cx=e.getCenterX(), cy=e.getCenterY();
        Color c = e.elite ? new Color(180,60,255) : new Color(200,30,30);
        for (int i=0;i<12;i++) {
            float ang=(float)(Math.random()*Math.PI*2), spd=1.5f+(float)Math.random()*3f;
            particles.add(new float[]{cx,cy,(float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,20+i,25,c.getRed(),c.getGreen(),c.getBlue()});
        }
    }

    private void spawnHitSparks(Enemy e) {
        for (int i=0;i<5;i++) {
            float ang=(float)(Math.random()*Math.PI*2), spd=1f+(float)Math.random()*2f;
            particles.add(new float[]{e.getCenterX(),e.getCenterY(),(float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,8,10,255,200,50});
        }
    }

    private void spawnFloatText(float x, float y, String text, int r, int g, int b) {
        floatTexts.add(new Object[]{x, y, (float)((Math.random()-0.5)*1.0), 45, text, r, g, b});
    }

    // ── Upgrades ─────────────────────────────────────────────────

    private void prepareUpgrades() {
        List<String> pool = new ArrayList<>();
        pool.add("HEAL|Restore 3 HP|<3");
        pool.add("HP UP|+3 Max Health (full heal)|HP");
        pool.add("SWIFT|Move Speed +0.8|>>");
        pool.add("POWER|Bullet Damage +1|**");
        pool.add("RAPID|Faster Fire Rate|!!");
        if (!player.multishot) pool.add("MULTI|Triple Shot|:::"); 
        pool.add("ARMOR|Reduce next 5 hits|[]");
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
        }
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        switch (state) {
            case MENU      -> drawMenu(g2);
            case PLAYING   -> drawGame(g2);
            case UPGRADE   -> drawUpgrade(g2);
            case GAME_OVER -> drawGameOver(g2);
        }
    }

    // ── Background ──

    private void drawBackground(Graphics2D g) {
        // Dark teal ground like EWS
        g.setColor(new Color(14, 30, 24));
        g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);
        // Subtle dot grid
        g.setColor(new Color(25,50,40,120));
        for (int x=0;x<GameFrame.WIDTH;x+=32)
            for (int y=0;y<GameFrame.HEIGHT;y+=32)
                g.fillRect(x,y,1,1);
        // Vignette
        for (int i=0;i<50;i++) { g.setColor(new Color(0,0,0,(int)(i*2.2f))); g.drawRect(i,i,GameFrame.WIDTH-i*2,GameFrame.HEIGHT-i*2); }
    }

    // ── MENU ──

    private void drawMenu(Graphics2D g) {
        // Dark bg
        g.setColor(new Color(5,8,12)); g.fillRect(0,0,GameFrame.WIDTH,GameFrame.HEIGHT);
        // Stars
        long t = System.currentTimeMillis();
        for (int i=0;i<80;i++) {
            int sx=(int)((i*137+43)%GameFrame.WIDTH), sy=(int)((i*97+17)%(GameFrame.HEIGHT-80));
            int a=(int)(120+100*Math.sin(t*0.001+i*0.7));
            g.setColor(new Color(200,200,255,a)); g.fillRect(sx,sy,1,1);
        }
        // Floating embers
        for (int i=0;i<14;i++) {
            double ox=((t*(0.01+i*0.003)+i*137)%GameFrame.WIDTH);
            double oy=80+((t*(0.006+i*0.002)+i*79)%(GameFrame.HEIGHT-130));
            int a=(int)(60+50*Math.sin(t*0.002+i));
            g.setColor(new Color(80,200,120,a)); int sz=1+(i%3); g.fillOval((int)ox,(int)oy,sz+1,sz+1);
        }

        // Title - pixel block style
        g.setFont(new Font("Monospaced",Font.BOLD,52));
        FontMetrics fm=g.getFontMetrics();
        String t1="LONE SOLDIER", t2="LAST STAND";
        // Shadow
        g.setColor(new Color(0,180,80,40));
        g.drawString(t1,(GameFrame.WIDTH-fm.stringWidth(t1))/2+3,143);
        // Main
        g.setColor(new Color(140,255,160));
        g.drawString(t1,(GameFrame.WIDTH-fm.stringWidth(t1))/2,140);

        g.setFont(new Font("Monospaced",Font.BOLD,22));
        fm=g.getFontMetrics();
        g.setColor(new Color(80,160,100));
        g.drawString(t2,(GameFrame.WIDTH-fm.stringWidth(t2))/2,172);

        // Stats badge row
        if (highScore > 0) {
            g.setFont(new Font("Monospaced",Font.BOLD,13));
            g.setColor(new Color(0,0,0,140)); g.fillRoundRect(GameFrame.WIDTH/2-110,188,220,24,6,6);
            g.setColor(new Color(255,215,40));
            String hs="Best: "+highScore+" kills"; fm=g.getFontMetrics();
            g.drawString(hs,(GameFrame.WIDTH-fm.stringWidth(hs))/2,205);
        }

        // Controls panel
        g.setColor(new Color(0,0,0,130)); g.fillRoundRect(230,218,340,145,10,10);
        g.setColor(new Color(40,120,60,160)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(230,218,340,145,10,10); g.setStroke(new BasicStroke(1));
        String[][] ctrls={
            {"WASD","Move"},{"Mouse","Aim"},{"Left Click","Shoot"},
            {"1/2/3","Pick upgrade"},{"ESC","Menu"}
        };
        g.setFont(new Font("Monospaced",Font.PLAIN,13)); int ry=240;
        for (String[] row:ctrls) {
            g.setColor(new Color(100,220,120)); g.drawString(row[0],265,ry);
            g.setColor(new Color(160,200,160)); g.drawString(row[1],390,ry); ry+=22;
        }

        // Enter prompt
        double ep=Math.sin(animTick*0.07)*0.5+0.5;
        g.setFont(new Font("Monospaced",Font.BOLD,17));
        fm=g.getFontMetrics(); String enter="[ PRESS ENTER TO START ]";
        int ex=(GameFrame.WIDTH-fm.stringWidth(enter))/2;
        g.setColor(new Color(80,255,120,(int)(ep*80))); g.drawString(enter,ex-1,397); g.drawString(enter,ex+1,397);
        g.setColor(new Color(80,(int)(180+ep*60),100)); g.drawString(enter,ex,396);

        // Credits
        g.setFont(new Font("Monospaced",Font.PLAIN,11)); g.setColor(new Color(50,90,60));
        String cred="Tolentino & Tanchico  |  Prog 2 Finals  |  UPHSD Molino";
        fm=g.getFontMetrics(); g.drawString(cred,(GameFrame.WIDTH-fm.stringWidth(cred))/2,GameFrame.HEIGHT-12);
    }

    // ── GAMEPLAY ──

    private void drawGame(Graphics2D g) {
        drawBackground(g);

        // Particles (behind entities)
        for (float[] p : particles) {
            float lr=p[4]/p[5]; int a=(int)(lr*210);
            g.setColor(new Color((int)p[6],(int)p[7],(int)p[8],Math.min(255,a)));
            int sz=Math.max(2,(int)(5*lr)); g.fillOval((int)p[0]-sz/2,(int)p[1]-sz/2,sz,sz);
        }

        // Damage border vignette
        if (damageFlashTicks>0) {
            int fa=Math.min(180,damageFlashTicks*14);
            for (int i=0;i<22;i++) { g.setColor(new Color(220,0,0,Math.max(0,fa-i*8))); g.drawRect(i,i,GameFrame.WIDTH-i*2,GameFrame.HEIGHT-i*2); }
        }

        // Entities
        for (Bullet b : bullets) b.draw(g);
        for (Enemy  e : enemies) e.draw(g);
        double aim=Math.atan2(mouseY-player.getCenterY(),mouseX-player.getCenterX())+Math.PI/2;
        player.draw(g,aim);

        // Floating texts
        g.setFont(new Font("Monospaced",Font.BOLD,13));
        for (Object[] ft : floatTexts) {
            int life=(int)ft[3]; float lifeR=life/45f;
            int a=(int)(lifeR*220);
            g.setColor(new Color((int)ft[5],(int)ft[6],(int)ft[7],Math.min(255,a)));
            g.drawString((String)ft[4],(int)(float)(Float)ft[0],(int)(float)(Float)ft[1]);
        }

        // Crosshair
        drawCrosshair(g,mouseX,mouseY);
        // HUD
        drawHUD(g);

        // Wave banner
        if (waveFlashTicks>0) {
            float alpha=Math.min(1f,waveFlashTicks/40f);
            int bw=320,bh=64,bx=GameFrame.WIDTH/2-bw/2,by=GameFrame.HEIGHT/2-bh/2;
            g.setColor(new Color(0,200,80,(int)(alpha*25))); g.fillRoundRect(bx-8,by-8,bw+16,bh+16,24,24);
            g.setColor(new Color(5,15,10,(int)(alpha*210))); g.fillRoundRect(bx,by,bw,bh,16,16);
            g.setColor(new Color(60,200,80,(int)(alpha*160))); g.setStroke(new BasicStroke(2)); g.drawRoundRect(bx,by,bw,bh,16,16); g.setStroke(new BasicStroke(1));
            g.setFont(new Font("Monospaced",Font.BOLD,26)); g.setColor(new Color(100,255,130,(int)(alpha*255)));
            String wt="-- WAVE "+waveManager.getCurrentWave()+" --"; FontMetrics fm=g.getFontMetrics();
            g.drawString(wt,GameFrame.WIDTH/2-fm.stringWidth(wt)/2,GameFrame.HEIGHT/2+9);
        }
    }

    private void drawCrosshair(Graphics2D g, int cx, int cy) {
        int gap=6,len=9; g.setStroke(new BasicStroke(2));
        g.setColor(new Color(0,0,0,100));
        g.drawLine(cx-gap-len-1,cy,cx-gap-1,cy); g.drawLine(cx+gap+1,cy,cx+gap+len+1,cy);
        g.drawLine(cx,cy-gap-len-1,cx,cy-gap-1); g.drawLine(cx,cy+gap+1,cx,cy+gap+len+1);
        g.setColor(new Color(80,255,120,200));
        g.drawLine(cx-gap-len,cy,cx-gap,cy); g.drawLine(cx+gap,cy,cx+gap+len,cy);
        g.drawLine(cx,cy-gap-len,cx,cy-gap); g.drawLine(cx,cy+gap,cx,cy+gap+len);
        g.setColor(new Color(80,255,120,180)); g.fillOval(cx-2,cy-2,4,4);
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
        g.setColor(new Color(255,215,40)); g.drawString(cs,134,31);

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

        // Enemies on screen (right side bottom)
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

        int lift=hov?6:0; cy-=lift;

        // Glow
        if(hov){g.setColor(new Color(60,200,80,35));g.fillRoundRect(cx-6,cy-6,w+12,h+12,18,18);}

        // Card bg
        g.setColor(hov?new Color(18,40,24):new Color(12,28,18)); g.fillRoundRect(cx,cy,w,h,12,12);

        // Top accent
        g.setColor(hov?new Color(60,220,80,200):new Color(40,140,60,140)); g.fillRoundRect(cx,cy,w,5,4,4);

        // Border
        g.setColor(hov?new Color(60,220,80,220):new Color(30,110,50,160));
        g.setStroke(new BasicStroke(hov?2:1.5f)); g.drawRoundRect(cx,cy,w,h,12,12); g.setStroke(new BasicStroke(1));

        // Icon (large)
        g.setFont(new Font("Monospaced",Font.BOLD,22));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(hov?new Color(100,255,130):new Color(60,200,90));
        g.drawString(icon,cx+(w-fm.stringWidth(icon))/2,cy+36);

        // Tag
        g.setFont(new Font("Monospaced",Font.BOLD,16)); fm=g.getFontMetrics();
        g.setColor(hov?new Color(160,255,160):new Color(100,220,120));
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

        g.setFont(new Font("Monospaced",Font.BOLD,18)); g.setColor(Color.WHITE);
        int secs2=survivalTicks/60,mins2=secs2/60; secs2%=60;
        String[] statLines={
            "Kills:          "+totalKills,
            "Survived:       "+String.format("%02d:%02d",mins2,secs2),
            "Coins earned:   "+coins,
            "Wave reached:   "+waveManager.getCurrentWave(),
        };
        int sy=232;
        for (String sl:statLines) { fm=g.getFontMetrics(); g.drawString(sl,(GameFrame.WIDTH-fm.stringWidth(sl))/2,sy); sy+=28; }

        // High score
        g.setFont(new Font("Monospaced",Font.BOLD,15));
        g.setColor(new Color(255,215,40));
        String hs="Best: "+highScore+" kills"; fm=g.getFontMetrics();
        g.drawString(hs,(GameFrame.WIDTH-fm.stringWidth(hs))/2,sy+6);

        // Buttons
        g.setColor(new Color(100,20,20,80)); g.fillRect(200,370,400,1);
        long bp=(System.currentTimeMillis()/600)%2;
        g.setFont(new Font("Monospaced",Font.BOLD,16));
        // Play again
        g.setColor(new Color(0,0,0,130)); g.fillRoundRect(215,383,155,34,8,8);
        g.setColor(bp==0?new Color(60,200,80):new Color(40,140,50)); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(215,383,155,34,8,8); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(100,240,120)); g.drawString("[ ENTER ]",230,406);
        // Menu
        g.setColor(new Color(0,0,0,130)); g.fillRoundRect(390,383,135,34,8,8);
        g.setColor(new Color(80,100,140,140)); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(390,383,135,34,8,8); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(140,160,200)); g.drawString("[ ESC ]",408,406);
        // Labels
        g.setFont(new Font("Monospaced",Font.PLAIN,12));
        g.setColor(new Color(80,160,90)); g.drawString("Play again",232,422);
        g.setColor(new Color(100,120,160)); g.drawString("Main menu",408,422);
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
        if (state==GameState.MENU && k==KeyEvent.VK_ENTER) startNewGame();
        if (state==GameState.PLAYING) {
            if(k==KeyEvent.VK_W||k==KeyEvent.VK_UP)    player.up=true;
            if(k==KeyEvent.VK_S||k==KeyEvent.VK_DOWN)  player.down=true;
            if(k==KeyEvent.VK_A||k==KeyEvent.VK_LEFT)  player.left=true;
            if(k==KeyEvent.VK_D||k==KeyEvent.VK_RIGHT) player.right=true;
        }
        if (state==GameState.UPGRADE) {
            if(k==KeyEvent.VK_1) applyUpgrade(0);
            if(k==KeyEvent.VK_2) applyUpgrade(1);
            if(k==KeyEvent.VK_3) applyUpgrade(2);
        }
        if (state==GameState.GAME_OVER) {
            if(k==KeyEvent.VK_ENTER)  startNewGame();
            if(k==KeyEvent.VK_ESCAPE) state=GameState.MENU;
        }
        if (k==KeyEvent.VK_ESCAPE && state==GameState.PLAYING) state=GameState.MENU;
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
    }
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
    @Override public void mouseMoved(MouseEvent e)    { mouseX=e.getX(); mouseY=e.getY(); updateCardHover(e.getX(),e.getY()); }
    @Override public void mouseDragged(MouseEvent e)  { mouseX=e.getX(); mouseY=e.getY(); updateCardHover(e.getX(),e.getY()); }

    private void updateCardHover(int mx, int my) {
        if(state!=GameState.UPGRADE){hoveredCard=-1;return;}
        int cw=200,ch=160,sx=(GameFrame.WIDTH-(cw*3+40))/2,sy=148;
        hoveredCard=-1;
        for(int i=0;i<3;i++){int cx=sx+i*(cw+20);if(mx>=cx&&mx<=cx+cw&&my>=sy-6&&my<=sy+ch){hoveredCard=i;break;}}
    }
}