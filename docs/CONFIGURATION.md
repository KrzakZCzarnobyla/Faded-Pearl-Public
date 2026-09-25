# Configuring Faded Pearl

Since version 1.7, the main server-side settings are stored in the world's
`serverconfig/faded_pearl-server.toml` file. On a dedicated server, the server administrator
controls this file; in singleplayer, it belongs to that specific world. Edit it while the world
or server is stopped. Values are loaded the next time the world starts.

The defaults preserve the mod's standard behaviour.

## Cave encounters

| Key | Default | Range | Meaning |
|---|---:|---:|---|
| `encounters.enabled` | `true` | `true/false` | Enables natural wounded Fade encounters. |
| `encounters.checkIntervalTicks` | `200` | `20–72000` | Time between attempts; 20 ticks equal one second. |
| `encounters.maximumY` | `45` | `-64–320` | The player must be at or below this height. |
| `encounters.minimumHorizontalSpacing` | `100` | `32–1024` | Minimum horizontal spacing between recorded encounter locations. |

The one-encounter-per-locally-connected-cave-area rule still applies independently of the
minimum spacing value.

## Trust

| Key | Default | Range | Meaning |
|---|---:|---:|---|
| `trust.gainPercent` | `100` | `0–500` | Percentage scale for positive trust changes. `0` disables gains. |
| `trust.lossPercent` | `100` | `0–500` | Percentage scale for trust penalties. `0` disables losses. |
| `trust.anchorRecallReward` | `1` | `0–10` | Base reward for a successful Anchor recall, before gain scaling. |

Any non-zero percentage preserves at least one point for an event, so small reactions are not
lost through rounding.

## Recovery

| Key | Default | Range | Meaning |
|---|---:|---:|---|
| `recovery.graceSeconds` | `30` | `5–300` | How long the system waits before treating a missing companion as requiring recovery. |

A very short grace period increases the risk of recovery reacting while a dimension loads
slowly. The configuration deliberately does not allow values below five seconds.
