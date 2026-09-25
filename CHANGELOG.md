# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

No public changes have been announced after `1.8.0-beta.3`.

## [1.8.0-beta.3] — public beta

This candidate combines the frozen 1.7 feature set with persistence, migration, presentation
and packaging fixes. Fresh singleplayer and migration of a disposable world from `1.4.1`
passed healing, Save/Quit, commands, Anchor, dimension travel and recovery tests. The final log
was not rechecked after the last two migration cases; the first migration log was clean.

Multiplayer remains **experimental**. An earlier three-player session confirmed the basic loop,
but beta.3 has not received a complete dedicated-server regression covering restarts,
disconnects, recovery and dimension travel. Back up the world before multiplayer use and install
the exact same JAR on the server and every client.

### Added

- a persistent Fade companion, trust, Journal, home and recovery for each player;
- natural encounters with additional wounded Fades, with ownership assigned through healing;
- neutral reactions to other players and autonomous interactions between Fade companions;
- server configuration for encounters, trust, recovery and Anchor rewards;
- the permanent Escape Pearl upgrade, protecting a Fade from accidental hits by his bonded player;
- the Resonating Anchor, Enderman Journal, curiosity behaviours and small-animal carrying;
- refreshed pearl textures and in-Journal guidance for healing and crafting.

### Fixed

- raised the Fade's nameplate so it no longer covers his face;
- synchronized trust-stage appearance selection for textures and eye/pearl lighting;
- improved head tracking without accumulating unwanted rotation during movement and reactions;
- kept the wounded Fade focused on the player while crying instead of nearby animals;
- prevented world comments from firing immediately after direct player interactions;
- rejected foreign or incomplete recovery copies before epoch selection and safely migrated
  partial or incorrectly typed legacy data;
- corrected finger attachment in animations without changing model geometry or mechanics;
- added an automated artifact contract for metadata, dependencies and required resources.

## [1.4.1] — 2026-08-19

### Added

- trust levels and trust-dependent reactions;
- gesture, weather and environment detection;
- the downed state and player rescue behaviours;
- randomized English and Polish dialogue;
- a run animation and colour-tinted glow mask;
- a flower-gift cooldown;
- expanded Anchor, travel and protection logic.
