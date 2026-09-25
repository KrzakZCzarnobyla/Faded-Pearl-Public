# Faded Pearl

<p align="center">
  <img src="docs/media/faded-pearl-curseforge-icon-400.png" width="240" height="240" alt="Faded Pearl icon">
</p>

Faded Pearl is an emotional companion mod for Minecraft Forge 1.20.1.

Somewhere beneath the surface, you may find a wounded Enderman crying in the dark. He is a
Fade — one of the rare Endermen who escaped the Ender Dragon's influence. Healing him begins a
longer relationship built through trust, exploration and the way you treat him.

## Features

- natural wounded Fade encounters in underground caves;
- a gradual healing and trust system instead of instant taming;
- Follow, Stay, Rest and Home companion commands;
- protection, rescue, carrying and safe joint teleportation;
- reactions to items, animals, weather, villages, discoveries and other Fade companions;
- a discoverable Enderman Journal containing memories, controls and recipes;
- the Resonating Anchor, companion recovery and the permanent Escape Pearl upgrade;
- separate companion relationships for different players;
- English and Polish localization.

## Current release

The current public build is **1.8.0-beta.3**.

Singleplayer was tested on a fresh world and a migrated disposable world. Multiplayer is
available as an experimental beta feature, but the current build has not received a complete
dedicated-server regression for restarts, disconnects, recovery and dimension travel. Back up
the world and use the exact same Faded Pearl JAR on the server and every client.

[Download Faded Pearl 1.8.0-beta.3](https://github.com/KrzakZCzarnobyla/Faded-Pearl-Public/releases/tag/v1.8.0-beta.3)

## Requirements

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17
- GeckoLib 4.8.4
- SmartBrainLib 1.15

See the [installation and update guide](docs/INSTALLATION.md),
[world configuration reference](docs/CONFIGURATION.md) and
[known beta limitations](docs/KNOWN_LIMITATIONS.md) before using the mod in an important world.

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
