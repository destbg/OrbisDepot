# Orbis Depot

A Hytale mod that adds a shared (per-player) void storage system. Think of it as a magical chest network that follows you around. Inspired by Satisfactory's Dimensional Depot.

## What this mod adds

- **Orbis Depot**: a placeable chest block that uploads items into your personal void storage.
- **Orbis Sigil**: a hand-held book to manage storage from anywhere.
- **Pocket crafting integration**: if you have a Sigil in your inventory, crafting can pull ingredients from void storage.
- **Auto-replenish placed blocks**: also with Sigil, your placed blocks can be topped up from storage.
- **Automation-friendly**: designed to play nicely with external storage/network mods.

## Showcase

### You can deposit items from the Orbis Depot and Orbis Sigil

[![Depositing items](https://github.com/destbg/OrbisDepot/raw/main/docs/sigil_depositing.gif)](https://github.com/destbg/OrbisDepot/blob/main/docs/sigil_depositing.gif)

### You can deposit items into the Orbis Depot using other storage mods

[![Conveyor Transfer](https://github.com/destbg/OrbisDepot/raw/main/docs/conveyor_transfer.gif)](https://github.com/destbg/OrbisDepot/blob/main/docs/conveyor_transfer.gif)

[![Conveyor Depositing](https://github.com/destbg/OrbisDepot/raw/main/docs/conveyor_depositing.gif)](https://github.com/destbg/OrbisDepot/blob/main/docs/conveyor_depositing.gif)

### You can automatically replenish your placed items from your Orbis Sigil

[![Auto Replenishing](https://github.com/destbg/OrbisDepot/raw/main/docs/auto_replenish.gif)](https://github.com/destbg/OrbisDepot/blob/main/docs/auto_replenish.gif)

### You can take items directly from your Sigil when crafting items

[![Crafting Integration](https://github.com/destbg/OrbisDepot/raw/main/docs/crafting_integration.gif)](https://github.com/destbg/OrbisDepot/blob/main/docs/crafting_integration.gif)

### And of course, you can easily take items from your storage

[![Taking Items](https://github.com/destbg/OrbisDepot/raw/main/docs/take_items.gif)](https://github.com/destbg/OrbisDepot/blob/main/docs/take_items.gif)

## How it works

- Items go into a Depot/Sigil “input” slot and are **uploaded into per-player storage** at a rate of 1 item per 2 seconds.
- Storage is **not global**: each player has their own void storage.
- The Orbis Depot allows player/external pipes to deposit items and they will be uploaded automatically up to a stack.
- The Orbis Sigil allows only the player to upload up to 4 different items at a time with a maximum of 2 stacks.

## Permissions

By default, the `Adventure` group is granted:

- `orbisdepot.depot.use`
- `orbisdepot.sigil.use`

## License

This project is licensed under the MIT License.
