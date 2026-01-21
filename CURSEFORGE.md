# Safari Dimension (Cobblemon)

Daily‑resetting Safari dimension for Cobblemon 1.7.1 (MC 1.21.1).

## Features

- Custom Safari dimension with daily seed rotation.
- Automatic reset at midnight (Europe/Paris) with evacuation + folder wipe.
- Nether‑style portal frame (custom block) + flint & steel ignition.
- Safari Ball item with Cobblemon throw physics and sounds.
- No send‑out inside Safari (prevents battles).
- Spawn structure at (0,0) in the sky every reset.
- World border 2000x2000 centered at (0,0).

## Commands

- `/safari enter`
- `/safari leave`
- `/safari info`
- `/safari buy balls 5`
- `/safari buy balls 10`
- `/safari reset` (admin)
- `/safari reload` (admin)

## Config

World‑specific config:
`world/<your-world>/safari-config.json`

Key fields:
- `commonCatchRate`, `uncommonCatchRate`, `rareCatchRate`
- `safariSpawnY`, `safariSpawnOffsetY`
- `spawnStructureId`
- `safariMinLevel`, `safariMaxLevel`

## Dependencies

- Fabric API
- Cobblemon 1.7.1
- Cobblemon Economy
