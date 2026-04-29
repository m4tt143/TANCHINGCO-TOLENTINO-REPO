   # LAST OUTPOST: Last Stand

   ### Programming 2 Finals Project — UPHSD Molino, CCS
   **Group 20:** Tolentino & Tanchingco

   **Repository:** [GitHub - TANCHINGCO-TOLENTINO-REPO](https://github.com/yourusername/TANCHINGCO-TOLENTINO-REPO)

   ---

   ## 📖 Game Title & Description

   **LAST OUTPOST: Last Stand** is a **Survival Defense** game built in pure Java (Swing/AWT), featuring real-time action gameplay with strategic upgrade mechanics.

   You are a lone soldier trapped in an open battlefield. Endless waves of enemy soldiers swarm toward you from all sides. You must survive as long as possible — moving to dodge incoming fire while your character auto-aims and auto-shoots at the nearest threat. After clearing each wave, select one of three upgrades to strengthen your soldier's combat effectiveness.

   Inspired by the mobile game **"Real War: Not Fake"** by Heseri Games.

   ### Genre
   **Survival Defense** — A single-unit survival game focusing on real-time wave management, entity-based projectile systems, and dynamic difficulty progression through upgrades.

   ---

   ## ✨ Key Features

   - **Main Menu:** Start a new game, view high scores, or exit
   - **Character Selection:** Choose between different soldier types (initial selection)
   - **Wave-Based Gameplay:** Progressive enemy waves with increasing difficulty
   - **Real-Time Combat:** Auto-targeting and auto-shooting mechanics
   - **Dynamic Upgrade System:** Choose 1 of 3 random upgrades between waves to enhance abilities
   - **In-Game Shop:** Purchase power-ups and enhancements during gameplay
   - **Weather System:** Dynamic environmental effects
   - **Visual Effects:** Damage numbers, hit markers, kill streaks, combo displays
   - **Particle Effects:** Loot drops, explosive barrels with area damage
   - **Score System:** Track kills, survive time, and maintain high scores
   - **Game Over State:** Final statistics and score reveal with restart option
   - **Audio System:** Sound manager for game effects and music

   ---

   ## 🎮 Game States & Gameplay Loop

   ### Game States
   1. **Main Menu** - Start screen with options to begin, view settings, or exit
   2. **Character Select** - Choose your soldier before starting the game
   3. **Playing** - Core gameplay loop with enemies, combat, and survival mechanics
   4. **Upgrade** - Between-wave decision point to select combat improvements
   5. **Game Over** - End state displaying final statistics and high scores
   6. **Shop** - In-game store for purchasing power-ups

   ### Core Gameplay Loop
   1. **Spawn & Move** - Navigate the battlefield using WASD or arrow keys
   2. **Combat** - Character automatically shoots at nearest enemy while you evade
   3. **Defeat Waves** - Eliminate all enemies in current wave to progress
   4. **Upgrade Selection** - Choose one of three random upgrades to enhance your abilities
   5. **Next Wave** - Continue to increasingly difficult enemy waves
   6. **Game Over** - Survive until defeated, then view statistics

   ---

   ## 🚀 Setup Instructions

   ### System Requirements
   - **Java Development Kit (JDK) 17 or higher** installed
   - Windows, macOS, or Linux
   - Minimum 2GB RAM
   - Graphics card with Java 2D acceleration support

   ### Installation & Running

   #### Option 1: Using Command Line

   1. **Clone the repository:**
      ```bash
      git clone https://github.com/yourusername/TANCHINGCO-TOLENTINO-REPO.git
      cd TANCHINGCO-TOLENTINO-REPO/LoneSoldier
      ```

   2. **Compile the Java source files:**
      ```bash
      javac *.java
      ```

   3. **Run the game:**
      ```bash
      java Main
      ```

   #### Option 2: Using an IDE (IntelliJ IDEA, Eclipse, VS Code)

   1. Open the project in your IDE
   2. Set the **Project SDK** to JDK 17 or higher
   3. Right-click `Main.java` and select **Run** (or press Shift+F10 in IntelliJ)

   ---

   ## 🎮 Controls

   | Key / Input         | Action                              |
   |---------------------|-------------------------------------|
   | **W** / **↑ Arrow**   | Move Up                             |
   | **S** / **↓ Arrow**   | Move Down                           |
   | **A** / **← Arrow**   | Move Left                           |
   | **D** / **→ Arrow**   | Move Right                          |
   | **1** / **2** / **3** | Select upgrade (during UPGRADE state)|
   | **ENTER**            | Start new game / Confirm            |
   | **Mouse Click**      | Interact with menu buttons          |

   ---

   ## 📸 Screenshots

   ### Main Menu
   The main menu presents your starting point with options to begin your survival journey.

   ### Character Selection
   Choose your soldier class before entering the battlefield.

   ### Gameplay in Action
   Real-time combat with waves of enemies, auto-targeting mechanics, and visual feedback systems.

   ### Upgrade Selection Screen
   Between waves, choose one of three strategic upgrades to improve your combat capabilities.

   ### Game Over Screen
   Final statistics display including total kills, survival time, and high score tracking.

   ---

   ## 📁 Project Structure

   ```
   LoneSoldier/
   ├── Main.java              # Entry point
   ├── GameFrame.java         # Swing JFrame setup
   ├── GamePanel.java         # Main game logic and rendering
   ├── Player.java            # Player character mechanics
   ├── Enemy.java             # Enemy entities
   ├── EnemyType.java         # Enemy type enumeration
   ├── Bullet.java            # Player projectiles
   ├── EnemyProjectile.java   # Enemy projectiles
   ├── DamageNumber.java      # Floating damage text
   ├── ExplosiveBarrel.java   # Interactive environmental objects
   ├── LootDrop.java          # Collectible items
   ├── WaveManager.java       # Wave spawning logic
   ├── WeatherSystem.java     # Environmental effects
   ├── SoundManager.java      # Audio management
   ├── SaveManager.java       # Game data persistence
   └── assets/
      └── images/            # Game sprites and textures
   ```

   ---

   ## 🔧 Technology Stack

   - **Language:** Java 17+
   - **Graphics:** Java Swing (JPanel, Graphics2D)
   - **Build:** Standard Java Compiler (javac)
   - **Version Control:** Git
   - **IDE Support:** IntelliJ IDEA, Eclipse, VS Code (with Java extensions)

   ---

   ## 📝 Code Structure & Design Patterns

   - **Object-Oriented Design:** Modular class structure with clear separation of concerns
   - **State Pattern:** Game states (MENU, PLAYING, UPGRADE, GAME_OVER)
   - **Entity-Component Pattern:** Separate systems for enemies, bullets, and effects
   - **Render Loop:** Continuous game update and paint cycles
   - **Event-Driven Input:** KeyListener and MouseListener for player controls

   ---

   ## 🎯 Gameplay Tips

   1. **Keep Moving** - Constant movement is essential; never stay still
   2. **Watch Your Surroundings** - Enemies spawn from all directions
   3. **Prioritize Upgrades** - Choose upgrades strategically based on difficulty
   4. **Learn Enemy Patterns** - Different enemy types have different behaviors
   5. **Manage Your Space** - Use the large battlefield to maintain distance

   ---

   ## 🐛 Known Issues & Future Enhancements

   ### Potential Improvements
   - [ ] Difficulty settings (Easy, Normal, Hard)
   - [ ] Leaderboard system with persistent storage
   - [ ] More character types with unique abilities
   - [ ] Additional enemy varieties and bosses
   - [ ] Multiplayer modes
   - [ ] Mobile version compatibility

   ---

   ## 📄 License & Credits

   - **Project Type:** Academic - Programming 2 Finals Project
   - **School:** UPHSD Molino, CCS
   - **Developers:** Tolentino & Tanchingco (Group 20)
   - **Inspiration:** "Real War: Not Fake" by Heseri Games

   ---

   ## 📞 Support & Feedback

   For issues, bug reports, or suggestions, please:
   1. Create an issue on the GitHub repository
   2. Provide detailed reproduction steps
   3. Include Java version and OS information

   ---

   **Last Updated:** April 29, 2026  
   **Status:** Complete & Functional
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

   ## 📸 Screenshots

### Main Menu 1
![Main Menu 1](LoneSoldier/DocPics/Screenshot%20(121).png)

### Main UI / MENU
![Main UI](LoneSoldier/DocPics/Screenshot%20(122).png)

### Select Character
![Select](LoneSoldier/DocPics/Screenshot%20(123).png)

### LoadOUT SHOP
![Loadout](LoneSoldier/DocPics/Screenshot%20(124).png)

### Game1
![Game1](LoneSoldier/DocPics/Screenshot%20(125).png)

### Game2
![Game2](LoneSoldier/DocPics/Screenshot%20(126).png)

### Shop
![Shop](LoneSoldier/DocPics/Screenshot%20(127).png)









   ---

   ## Developers

   | Name       | Role                                          |
   |------------|-----------------------------------------------|
   | Tolentino  | Player logic, WaveManager, game loop          |
   | Tanchingco | Enemy AI, Bullet system, UI/rendering         |

   ---

   *UPHSD Molino — College of Computer Studies | Programming 2 | AY 2025–2026*
