# Installing Faded Pearl

## Requirements

- Minecraft `1.20.1`;
- Forge `47.4.22`;
- Java `17`;
- GeckoLib `4.8.4`;
- SmartBrainLib `1.15`;
- a Faded Pearl file built for the same Minecraft and Forge versions.

Faded Pearl and both libraries must be installed in the `mods` directory on the client and the
server. In multiplayer, the server and every player must use the exact same Faded Pearl JAR.
Do not mix different beta candidates in one session.

## Experimental multiplayer

The basic multiplayer loop was tested in an earlier candidate with three players, but the
current build has not received a complete regression covering server restarts, disconnects,
recovery and dimension travel. Multiplayer is available as an experimental feature rather than
fully verified server support. Back up the world before playing. The server and every client
must use the exact same mod JAR.

## New world or server

1. Close Minecraft and stop the server.
2. Remove every older Faded Pearl JAR from the active `mods` directory. Keeping two versions
   installed at once may prevent the game from starting.
3. Place Faded Pearl, GeckoLib and SmartBrainLib in the `mods` directory.
4. Start the game or server and confirm that Faded Pearl appears in the mod list.
5. For multiplayer, connect with a client using the same set of versions.

World configuration is created at `serverconfig/faded_pearl-server.toml`. The available settings
are described in `docs/CONFIGURATION.md`. Edit the file while the world or server is stopped.

## Updating an existing world

Beta builds are intended for testing and should not be opened first on the only copy of an
important world.

1. Close the game or stop the server and wait for saving to finish.
2. Copy the entire world directory to a safe location.
3. Keep the previous Faded Pearl JAR outside the `mods` directory.
4. Remove the older JAR from every active `mods` directory, then install the new JAR on the
   server and every client.
5. Open a copy of the world first.
6. Confirm the correct Fade, name, trust, home, Journal and any item entrusted for examination.
7. In multiplayer, verify each player's companion separately, then test a server restart and a
   portal transition.

If a Fade disappears, a duplicate of the same character appears, or a companion becomes bound
to the wrong player, do not treat that state as the new primary save. Preserve the test world
and logs, return to the backup, and report the versions and sequence of events.

## Removing the mod or downgrading

Do not remove the mod or install an older JAR on the only copy of a world already saved by a
newer version. Older code may not understand newer relationship data. A safe rollback restores
both the world backup made before the update and the matching older mod set.

## Release status

Current public builds, downloads and release notes are available on
[GitHub Releases](https://github.com/KrzakZCzarnobyla/Faded-Pearl-Public/releases). Read the
[known limitations](KNOWN_LIMITATIONS.md) before using the beta in an important world.

## Verifying the download

Compare the file name, size and SHA-256 with the values published for the release. The prepared
`faded_pearl-1.8.0-beta.3.jar` candidate is `1469680` bytes and has this SHA-256:
`8D10D32A53D121CD7101AB3125902B6053B1BF189AE0901AAC47170BC01D9A56`.

Windows PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath .\faded_pearl-1.8.0-beta.3.jar
```

Linux/macOS:

```bash
sha256sum faded_pearl-1.8.0-beta.3.jar
```
