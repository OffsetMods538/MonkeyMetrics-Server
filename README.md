# MonkeyMetrics Server

This is the MonkeyMetrics ingest server for receiving telemetry data from mods and allowing Prometheus to consume it.

Metrics are publicly available [here](https://analytics.offsetmonkey538.top).

Docker setup used for the instance is available on the docker-stack/master branch.

## What is collected?
My mods collect the following anonymous usage data every time the game is launched:
- Minecraft version - the Minecraft version you're using
- Mod loader - the mod loader you're using
- Mods - list of mods and their versions. Only includes my mod, the full mod list is *not* sent.

Example of JSON data:
```json
{
  "minecraft_version": "1.21.4",
  "mod_loader": "fabric",
  "mods": {
    "monkeylib538": "3.0.0+alpha.2",
    "loot-table-modifier": "2.0.0+beta.1"
  }
}
```
