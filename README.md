# MonkeyMetrics Server

This is the MonkeyMetrics ingest server for receiving telemetry data and allowing Prometheus to consume it.

Metrics are publicly available [here](https://analytics.offsetmonkey538.top).

Docker setup used for the instance is available on the docker-stack/master branch.

## What is collected?
My mods collect the following anonymous usage data every time the game is launched:
- Minecraft version - the Minecraft version you're using
- Environment - if you're running on client/server
- Mod loader - the mod loader you're using
- Mods - list of mods. Only includes my mod, the full mod list is *not* sent.

Example of JSON data:
```json
{
  "minecraft": "1.21.4",
  "env": "s",
  "loader": "fabric",
  "mods": [
    "monkeylib538",
    "loot-table-modifier"
  ]
}
```
