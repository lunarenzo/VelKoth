# VelKoth

A modern, high-performance **King of the Hill (KoTH)** plugin built for PaperMC 1.21+ utilizing Java 21 features. 

VelKoth supports multiple capture modes (`CAPTURE` and `SCORE`), O(1) mathematical region boundary checks for supreme performance, an automated integrated scheduler, and a robust configurable database-backed reward and statistics system.

## 🚀 Features

- **Modern Architecture**: Compiled against Paper 1.21+ using Java 21, **Folia-supported Regional/Entity Schedulers**, and the Incendo Cloud V2 native Brigadier command framework.
- **Multiple Capture Modes**: 
  - `CAPTURE`: Traditional KoTH where one player must hold the hill uninterrupted for the duration.
  - `SCORE`: Point-based KoTH where players accumulate capture seconds until they reach the target score.
- **High Performance Regions**: Both Cuboid and Cylinder regions utilise O(1) containment algorithms removing loop-based block checks for zero server TPS impact.
- **Team Plugin Integrations**: Fully supports team and faction capturing! Players in the same team, faction, or party can capture hills together without contesting. Supports 12+ plugins natively.
- **Advanced Display Visuals**: Leverages modern Paper Adventure API to push ActionBars, custom BossBars, on-screen Titles, Particle effects, and Sounds. Includes **FastBoard-powered Scoreboards** (with smart-overrides for plugins like TAB and SimpleScore) and native **Paper TextDisplay Holograms** that are fully chunk-safe.
- **Robust Storage System**: Fully asynchronous SQLite/MySQL HikariCP connection pooling storing player total wins, daily wins, and weekly wins.
- **bStats Metrics Integration**: Uses anonymous metrics tracking to keep developers informed on adoption rates and usage.
- **Scheduler Integration**: Define exactly when specific arenas should run via Cron-like syntax or simple time formats. Now supports **Dynamic Scheduling** via in-game commands.
- **Dynamic Configuration**: Built on Okaeri Configs for instant YAML hot-reloading. Every system message, GUI layout, and internal setting can be modified.
- **Comprehensive Rewards**: Execute commands, distribute ItemStacks (with overflow drop handling), or give Vault Economy money to the winners.
- **Enhanced PlaceholderAPI**: Comprehensive support for multi-arena events, absolute/specific next event countdowns, and human-readable time formatting.
- **Developer API**: Features a full suite of Bukkit Events (`KothStartEvent`, `KothWinEvent`, `KothCaptureStartEvent`, etc.) and a static `VelKothAPI` state accessor for developers.

---

## ⚙️ Dependencies

- **PaperMC** 1.21.x+
- **Java 21**
- [Vault](https://www.spigotmc.org/resources/vault.34315/) *(Optional - Required for Economy rewards)*
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) *(Optional - Exposes extensive placeholders)*
  - `%koth_arena[_<id>]%`, `%koth_time[_formatted][_<id>]%`
  - `%koth_owner[_<id>]%`, `%koth_players[_<id>]%`
  - `%koth_next_arena[_<id>]%`, `%koth_next_time[_formatted][_<id>]%`
- **Supported Team Plugins** *(Optional - Allows team members to capture together)*
  - BetterTeams, Factions (UUID/Saber), GangsPlus, Guilds, KingdomsX
  - SuperiorSkyblock2, Towny Advanced, UltimateClans, Parties, AxParties
  - LandClaimPlugin (Custom hook)

---

## 🛠️ Usage & Setup

### Creating an Arena

1. Grab the KoTH Wand by typing `/koth wand`.
2. Left-Click one corner of your hill, and Right-Click the opposite corner.
3. Type `/koth create <arenaName>` to save the region.
4. Modify specific rules, timers, and rewards for this arena in the generated `plugins/VelKoth/arenas.yml` configuration!

### Administering Events

- Start an event manually: `/koth start <arenaName>`
- Pause an ongoing event (maintaining times/scores): `/koth pause <arenaName>`
- Stop an event completely: `/koth stop <arenaName>`
- Configure automatic daily/weekly events in `plugins/VelKoth/config.yml`.

---

## 🪧 Commands

VelKoth features a robust, auto-completing Brigadier command hierarchy.

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/koth help` | `velkoth.use` | View available commands |
| `/koth stats` | `velkoth.use` | View your personal KoTH wins (Daily/Weekly/All-time) |
| `/koth next <name>` | `velkoth.use` | See when is the next Koth gonna start |
| `/koth list` | `velkoth.admin` | View all arenas and their current status |
| `/koth wand` | `velkoth.admin` | Receive the region selection wand |
| `/koth create <name>` | `velkoth.admin` | Create a new arena from your wand selection |
| `/koth delete <name>` | `velkoth.admin` | Permanently delete an arena |
| `/koth start <name>` | `velkoth.admin` | Force start an arena immediately |
| `/koth pause <name>` | `velkoth.admin` | Pause an active capture session |
| `/koth resume <name>` | `velkoth.admin` | Resume a paused capture session |
| `/koth stop <name>` | `velkoth.admin` | Stop an active arena prematurely |
| `/koth schedule add <day> <time> <arena>` | `velkoth.admin` | Add a new event to the automated schedule |
| `/koth schedule list` | `velkoth.admin` | View all currently scheduled events |
| `/koth schedule remove <index>` | `velkoth.admin` | Remove a scheduled event by its list index |
| `/koth set mode <arena> <CAPTURE\|SCORE>` | `velkoth.admin` | Change the win condition (Hold timer or Cumulative points) |
| `/koth set time <arena> <seconds>` | `velkoth.admin` | Set the required capture time |
| `/koth set score <arena> <maxScore>` | `velkoth.admin` | Set the target score for SCORE mode |
| `/koth set grace <arena> <seconds>` | `velkoth.admin` | Set the grace period before capture resets |
| `/koth reward add <arena> <reward>` | `velkoth.admin` | Add a new win reward (supports Commands, Economy, Items) |
| `/koth reward list <arena>` | `velkoth.admin` | View all configured rewards for an arena in detail |
| `/koth reward remove <arena> <index>` | `velkoth.admin` | Remove a specific reward by index |
| `/koth reward clear <arena>` | `velkoth.admin` | Clear all rewards for an arena |
| `/koth reload` | `velkoth.admin` | Hot-reload all `.yml` configuration files |

---

## 🎁 Rewards

VelKoth supports a powerful reward system that can be managed entirely in-game or via `arenas.yml`. Rewards are granted to the winning player (or all online members of a team/faction if team hooks are active).

### Supported Reward Types

| Type | Format | Example |
| :--- | :--- | :--- |
| **Command** | `COMMAND:<cmd>` or `<cmd>` | `COMMAND:give %player% netherite_ingot 1` |
| **Economy** | `ECONOMY:<amount>` | `ECONOMY:1000` (Requires Vault) |
| **Item** | `ITEM:<material>[:<amount>]` | `ITEM:GOLD_BLOCK:3` |

### Reward Placeholders
* `%player%`: Name of the winner.
* `%arena%`: ID of the arena.

### Viewing Rewards
You can see exactly what rewards are configured for an arena using:
` /koth reward list <arena>`
This will display a numbered list of rewards with human-readable descriptions (e.g., *Command: give <player> diamond 1*).

---

## 📜 Permissions

- **`velkoth.use`**: Default permission granted to all players allowing them to view help and check their personal statistics.
- **`velkoth.admin`**: Default OP permission granting full administrative access to edit arenas, bypass timers, and trigger events.

---

## 📦 Building from Source

To compile the standalone shadow JAR natively (including the Cloud command libraries and Okaeri configs):

```bash
git clone https://github.com/YourName/VelKoth.git
cd VelKoth
./gradlew clean shadowJar
```

Your compiled plugin will be deposited in `/build/libs/`.
