# Known limitations of Faded Pearl

This document separates confirmed problems from areas that have not yet been fully tested.

## 2.0.0

Faded Pearl 2.0.0 passed automated regression and artifact validation, fresh-world play,
migration on copied 1.8 and 1.9 worlds, and functional checks in both a clean Forge profile and
the creator's real modpack. The items below are deliberately recorded as unverified scope, not
as completed tests and not as confirmed defects.

### Deferred verification for 2.1

- a real portal transition that briefly leaves both dimension copies loaded (`portal grace`);
- rejection of a late-loaded stale companion copy by the recovery epoch;
- comparative performance profiles with `0`, `1`, `10` and `25` active Fades;
- extended observation of combat teleport presentation and the reaction to a Creeper;
- final targeted regression of settling near home and animal carrying on the exact 2.0 release
  artifact;
- remaining observational coverage for End memories, Resonating Anchor presentation and
  ambient interruption/natural-frequency cases;
- repeated relationship-memory observations covering reunion after absence, night watch,
  building and armour progress, taming and named pets, villages and diamonds;
- remaining knowledge/crafting dialogue and world-interest observations.

Back up an important world before updating. If a Fade disappears, is duplicated or becomes
bound to the wrong player, stop using that copy of the world, preserve it with `latest.log`, and
report the exact sequence that led to the problem.

### Experimental multiplayer

Multiplayer remains available but is not presented as fully supported. Earlier sessions,
including a three-player game and an interrupted downed-state case, produced positive results;
the complete matrix for restarts, disconnects, simultaneous recovery, several owners and
dimension travel is deferred to version 2.5. The server and every client must use the exact same
Faded Pearl JAR.

## 1.9.0-beta.1 community beta

The 1.9 beta has passed automated regression, resource validation, a clean development-client
startup and controlled triggers for shared gaze, resting near the player and settling at home.
Natural frequency, full interruption coverage, world interest, relationship memories, End story
memories and the Resonating Anchor presentation still include unverified observational cases.
These are open beta test areas, not confirmed defects.

A copied `1.8.x` world preserved the companion UUID and data through Save/Quit and an
Overworld–Nether–Overworld round trip without a persistent duplicate. A controlled loss and
recovery of the canonical companion remains open because the migrated test world's ownership
ledger could not be safely reconstructed through manual NBT editing. Do not use the beta on the
only copy of an important world.

Full multiplayer development and dedicated-server regression are deferred to version 2.5. The
existing multiplayer code remains experimental; use the same JAR on all clients and the server,
and back up the world.

## 1.8.0-beta.3 public beta

### Experimental multiplayer

The basic loop for separate player relationships was confirmed in an earlier three-player
session, but `1.8.0-beta.3` has not received a complete dedicated-server regression covering
restarts, one player disconnecting, simultaneous recovery and dimension travel by several
owners. This is untested scope, not a confirmed defect. Back up the world before playing and
use the exact same JAR on the server and every client.

### Compatibility with other mods

Faded Pearl does not require JEI, Jade or WTHIT to start. An earlier JEI test confirmed recipe
visibility in a previous candidate, but a complete runtime matrix for exact versions of recipe
viewers, overlays, transport mods and entity-serialization mods remains open for beta.3. A mod
not appearing here is neither a confirmed incompatibility nor confirmed support.

### Performance

Automated tests and ordinary singleplayer sessions did not reveal a failure, but comparative
profiling with `0/1/10/25` active Fades has not yet been accepted. Large servers should treat
this build as a beta and monitor TPS/MSPT, especially with many active relationships.

### Migration

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
