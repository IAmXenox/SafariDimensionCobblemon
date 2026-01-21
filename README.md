# Safari Dimension Mod

Custom Minecraft 1.21.1 Fabric mod for Cobblemon that adds a daily‑resetting Safari dimension with portal access, economy integration, and Safari‑style captures.

## Features

- **Safari Dimension** with a custom chunk generator seeded daily.
- **Daily reset** (00:00 Europe/Paris) with automatic evacuation and dimension folder wipe.
- **Portal frame** block (Nether‑style size detection) lit by flint & steel.
- **Safari Ball item** (`safari:safari_ball`) with Cobblemon throw physics and sounds.
- **No send‑out** inside Safari (prevents battles).
- **Spawn structure** placed at `(0, 0)` in the sky each reset.
- **World border** 2000x2000 centered on `(0,0)`.
- **World‑specific config** stored in each world save.

## Dependencies

Place these jars in your `mods` folder:
1. `fabric-api`
2. `cobblemon` (1.7.1)
3. `cobblemon-economy`

## Configuration

World‑specific config file:
`world/<your-world>/safari-config.json`

Default values:
```json
{
  "sessionTimeMinutes": 2,
  "initialSafariBalls": 25,
  "safariBallItem": "safari:safari_ball",
  "carryOverSafariBalls": false,
  "logoutClearInventory": true,
  "allowMultiplayerSessions": true,
  "entrancePrice": 500,
  "pack5BallsPrice": 150,
  "pack10BallsPrice": 250,
  "maxBallsPurchasable": 20,
  "commonCatchRate": 0.45,
  "uncommonCatchRate": 0.18,
  "rareCatchRate": 0.1,
  "resetTimezone": "Europe/Paris",
  "dimensionSize": 2000,
  "coreRadius": 350,
  "resetOffsetRange": 100000,
  "safariSpawnY": 160,
  "safariSpawnOffsetY": 3,
  "spawnStructureId": "safari:safari_spawn_island",
  "allowedBiomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:jungle",
    "minecraft:swamp",
    "minecraft:forest",
    "minecraft:badlands"
  ],
  "spawnRateMultiplier": 1.5,
  "safariMinLevel": 5,
  "safariMaxLevel": 30
}
```

## Portal

1. Build a Nether‑style frame with `safari:safari_portal_frame`.
2. Light it with flint & steel.
3. Walk into the portal to start a Safari session.

## Commands

- `/safari enter`
- `/safari leave`
- `/safari info`
- `/safari buy balls 5`
- `/safari buy balls 10`
- `/safari reset` (admin)
- `/safari reload` (admin)

## Building

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
```

Jar output:
`build/libs/safari-dimension-0.0.1.jar`

## Notes

- The dimension is wiped on reset (folder deletion) and regenerated using the daily seed.
- The Safari spawn is always at `(0, 0)` with a floating structure placed at `safariSpawnY`.
- Players need at least one empty inventory slot to enter (Safari Balls are added).
