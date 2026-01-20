# Safari Dimension Mod

A custom Minecraft 1.21.1 Fabric mod for Cobblemon that adds a "Safari Zone" dimension with daily resets, economy integration, and custom capture mechanics.

## Features

- **Safari Dimension:** A dedicated dimension that resets its generation seed every day at midnight (Europe/Paris).
- **Session System:** 25-minute sessions with inventory sandboxing (your items are saved/restored, you get a Safari Kit).
- **Custom Capture:** No battles allowed! Throw Safari Balls directly. Catch rates are calculated based on species rarity (Common 22%, Uncommon 15%, Rare 8%).
- **Economy Integration:** Entry fee (500 Pokédollars) and ability to buy more balls/time.
- **Safety:** Prevents falling/void damage on entry, handles disconnects/deaths gracefully.

## Dependencies

Place these jars in your `mods` folder:
1. `fabric-api`
2. `cobblemon` (1.7+)
3. `cobblemon-economy`
4. `academy` (for the Safari Ball item)

## Configuration

The config file is located at `config/safari-config.json`.
It is generated on first launch.

### Default Config
```json
{
  "sessionTimeMinutes": 25,
  "initialSafariBalls": 25,
  "safariBallItem": "cobblemon:safari_ball",
  "carryOverSafariBalls": false,
  "logoutClearInventory": true,
  "allowMultiplayerSessions": true,
  "entrancePrice": 500,
  "pack5BallsPrice": 150,
  "pack10BallsPrice": 250,
  "maxBallsPurchasable": 20,
  "commonCatchRate": 0.22,
  "uncommonCatchRate": 0.15,
  "rareCatchRate": 0.08,
  "resetTimezone": "Europe/Paris",
  "dimensionSize": 2000,
  "coreRadius": 350,
  "allowedBiomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:jungle",
    "minecraft:swamp",
    "minecraft:forest",
    "minecraft:badlands"
  ],
  "spawnRateMultiplier": 1.5
}
```

## Commands

- `/safari enter`: Pay the fee and enter the Safari.
- `/safari leave`: Quit early (restores inventory).
- `/safari info`: Check remaining time.
- `/safari buy balls 5`: Buy 5 balls for 150$.
- `/safari buy balls 10`: Buy 10 balls for 250$.

## Building

This project uses Gradle.
```bash
./gradlew build
```
The output jar will be in `build/libs/`.

## Developer Notes

- **Capture Logic:** Uses a Mixin into `PokeBallEntity` to cancel standard battle logic and apply a custom RNG chance.
- **World Reset:** Does not delete files. It shifts the coordinate center of the Safari dimension by 5000 blocks every day based on the configured Timezone.
