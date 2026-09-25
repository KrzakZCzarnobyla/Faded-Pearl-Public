# Known limitations of Faded Pearl 1.8.0-beta.3

This document separates confirmed problems from areas that have not yet been fully tested.

## Experimental multiplayer

The basic loop for separate player relationships was confirmed in an earlier three-player
session, but `1.8.0-beta.3` has not received a complete dedicated-server regression covering
restarts, one player disconnecting, simultaneous recovery and dimension travel by several
owners. This is untested scope, not a confirmed defect. Back up the world before playing and
use the exact same JAR on the server and every client.

## Compatibility with other mods

Faded Pearl does not require JEI, Jade or WTHIT to start. An earlier JEI test confirmed recipe
visibility in a previous candidate, but a complete runtime matrix for exact versions of recipe
viewers, overlays, transport mods and entity-serialization mods remains open for beta.3. A mod
not appearing here is neither a confirmed incompatibility nor confirmed support.

## Performance

Automated tests and ordinary singleplayer sessions did not reveal a failure, but comparative
profiling with `0/1/10/25` active Fades has not yet been accepted. Large servers should treat
this build as a beta and monitor TPS/MSPT, especially with many active relationships.

## Migration

A disposable world from version `1.4.1` preserved one companion, the relationship, Save/Quit,
the Escape Pearl, dimension travel and recovery. However, a complete baseline for every optional
field was not recorded before the first update, and the final log after portal travel and the
last recovery case was not rechecked. The first migration log was clean. Always update a copy
of an important world first.

## Reporting a problem

Include the Minecraft, Forge, GeckoLib, SmartBrainLib and Faded Pearl versions, whether the game
was singleplayer or a server, the last actions performed, and `latest.log`. If a Fade is lost or
duplicated, preserve a copy of the world and do not attempt further repairs on the only important
save.
