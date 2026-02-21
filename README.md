# Orbis Depot

A Hytale mod that adds a shared (per-player) void storage system — think of it as a magical chest network that follows you around. Inspired by Satisfactory's Dimensional Depot.

## What it does

- **Orbis Depot** - A placeable block (chest) that connects to your personal void storage. Drop items into the input slot and they get absorbed into storage over time. Works with external mods for automation.
- **Orbis Sigil** - A hand-held book that opens the same void storage from anywhere, no chest needed.
- **Field Crafting** - While you have a Sigil in your inventory, the pocket crafting window pulls ingredients directly from void storage.
- **Auto-replenish blocks** - Placed blocks are automatically replenished from storage.
- **Per-player settings** - Toggle auto-place and crafting integration on or off through an in-game settings panel.

## Building

```
./gradlew build
```

The built jar lands in `build/libs/`. Drop it (along with the resource files) into your server's mods folder.

## License

This project is licensed under the MIT License.
