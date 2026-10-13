# Faded Pearl

Faded Pearl is an emotional companion mod for Minecraft Forge 1.20.1.

Somewhere beneath the surface, you may find a wounded Enderman crying in the dark. He is a
Fade — one of the rare Endermen who escaped the Ender Dragon's influence. Healing him begins a
long relationship shaped by trust, exploration and the way you treat him.

## Features

- natural wounded Fade encounters in underground caves;
- healing, trust and a persistent personal companion;
- Follow, Stay, Rest and Home commands;
- protection, rescue, carrying and safe shared teleportation;
- reactions to items, animals, weather, villages, discoveries and other Fades;
- calm routines, world interests and persistent relationship memories;
- End-related story memories discovered through shared exploration;
- a discoverable Enderman Journal with memories, controls and recipes;
- the Resonating Anchor, companion recovery and the permanent Escape Pearl upgrade;
- separate companion relationships for different players.

## Faded Pearl 2.0.0

Version 2.0.0 is the first full release. Automated checks, fresh-world play, copied 1.8 and 1.9
worlds, a clean Forge profile and the owner's real modpack passed. Remaining targeted
observations and stress profiles are listed in
[Known limitations](docs/KNOWN_LIMITATIONS.md) and continue in the 2.1 verification cycle.

Multiplayer is available as an **experimental feature**. An earlier three-player session
confirmed the basic companion loop, but 2.0.0 has not received a complete dedicated-server
regression covering restarts, disconnects, recovery and dimension travel. Back up important
worlds and install the exact same Faded Pearl JAR on the server and every client.

[Download Faded Pearl 2.0.0](https://github.com/KrzakZCzarnobyla/Faded-Pearl-Public/releases/tag/v2.0.0)

## Requirements

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17
- GeckoLib 4.8.4
- SmartBrainLib 1.15

Before updating an important world, make a complete backup. See the
[installation and update guide](docs/INSTALLATION.md),
[server configuration reference](docs/CONFIGURATION.md) and
[known limitations](docs/KNOWN_LIMITATIONS.md).

Problems can be reported through the
[public issue tracker](https://github.com/KrzakZCzarnobyla/Faded-Pearl-Public/issues).

## Building from source

Faded Pearl requires Java 17. On Windows run:

```powershell
.\gradlew.bat clean build
```

On Linux or macOS run:

```bash
./gradlew clean build
```

The reobfuscated JAR is created in `build/libs/`.

## License

The source code and assets are **All Rights Reserved**. See [LICENSE](LICENSE).
