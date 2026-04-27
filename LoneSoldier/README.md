# LAST OUTPOST: Last Stand
### Programming 2 Finals Project — UPHSD Molino, CCS
**Group 20:** Tolentino & Tanchico

---

## Game Description

**LAST OUTPOST: Last Stand** is a **Survival Defense** game built in Java (Swing/AWT).

You are a single soldier trapped in an open battlefield. Waves of enemy soldiers swarm toward you from all sides. You must survive as long as possible — moving to dodge, while your character auto-aims and auto-shoots at the nearest threat. After clearing each wave, choose one upgrade to improve your soldier.

Inspired by the mobile game **"Real War: Not Fake"** (Heseri Games).

---

## Genre
**Survival Defense** — Withstanding waves with a single unit. Focuses on real-time upgrades and entity management.

---

## How to Run

### Requirements
- Java JDK 17 or higher installed

### Steps
1. Clone or download this repository.
2. Open a terminal/command prompt in the project folder.
3. Compile:
   ```
   javac *.java
   ```
4. Run:
   ```
   java Main
   ```

---

## Controls

| Key             | Action                     |
|-----------------|----------------------------|
| `W` / `↑`       | Move Up                    |
| `S` / `↓`       | Move Down                  |
| `A` / `←`       | Move Left                  |
| `D` / `→`       | Move Right                 |
| `1` / `2` / `3` | Choose upgrade (after wave)|
| `ENTER`         | Start / Restart game       |
| `ESC`           | Return to Main Menu        |

> **Shooting is automatic** — your soldier auto-aims and fires at the nearest enemy.

---

## Upgrades

After clearing each wave, choose 1 of 3 random upgrades:

| Upgrade       | Effect                              |
|---------------|-------------------------------------|
| HEAL          | Restore 2 HP                        |
| HP UP         | +2 Max Health                       |
| SWIFT         | Move Speed +0.8                     |
| POWER         | Bullet Damage +1                    |
| RAPID         | Faster fire rate                    |
| MULTI         | Triple shot (fires 3 bullets at once)|

---

## Wave Progression

| Wave  | Enemies | Enemy HP     | Enemy Speed     |
|-------|---------|--------------|-----------------|
| 1     | 8       | 1 HP         | Slow            |
| 2     | 13      | 1 HP         | Slightly faster |
| 3     | 18      | 1 HP         | Faster          |
| 4+    | +5/wave | +1 HP every 3 waves | Increases |

---

## Project Structure

```
LoneSoldier/
├── Main.java         — Entry point
├── GameFrame.java    — JFrame window setup
├── GamePanel.java    — Game loop, state machine, input, rendering
├── Player.java       — Player entity (movement, HP, upgrades)
├── Enemy.java        — Enemy entity (AI, HP, rendering)
├── Bullet.java       — Bullet projectile logic
└── WaveManager.java  — Controls enemy spawning per wave
```

---

## OOP Concepts Used

- **Classes & Objects** — Player, Enemy, Bullet, WaveManager
- **Encapsulation** — Private fields with public methods
- **Enum** — GameState (MENU, PLAYING, UPGRADE, GAME_OVER)
- **ArrayList (Data Structure)** — Dynamic lists for enemies and bullets
- **Interfaces** — KeyListener for input
- **Inheritance** — GamePanel extends JPanel
- **Game Loop Pattern** — javax.swing.Timer at 60 FPS

---

## Screenshots

> *(Add screenshots of your final build here before submission)*

---

## Developers

| Name       | Role                                          |
|------------|-----------------------------------------------|
| Tolentino  | Player logic, WaveManager, game loop          |
| Tanchico   | Enemy AI, Bullet system, UI/rendering         |

---

*UPHSD Molino — College of Computer Studies | Programming 2 | AY 2025–2026*
