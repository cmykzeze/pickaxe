# Pickaxe Enchants (Fabric, Minecraft 26.3)

Six pickaxe enchantments.

**Haste (I-X)** (gold): while you hold the pickaxe you get the real Haste effect: level I = Haste I ... level X = Haste X.
- Levels I-V come from the enchanting table (and random loot).
- Levels VI-X are only found as books in rare chests: ancient city (8%), bastion treasure (10%), end city (8%) and
  woodland mansion (8%). Higher levels are rarer (VI 30, VII 25, VIII 20, IX 15, X 10 weight).
- Anvil note: two books of the same level still combine into the next level (two V books make VI) like any
  enchantment, the game can't be told to block that without a mixin.
- Effects from a potion or beacon are never replaced by a weaker one.

**Speed (I-II)** (mint green): while you hold the pickaxe you get the real Speed effect: level I = Speed I, level II = Speed II.
- Both levels come from the enchanting table (books too), random chest loot and librarian villagers.
- Two Speed I books combine into Speed II in an anvil.
- Effects from a potion or beacon are never replaced by a weaker one.

**Night Vision (I)** (cyan): while you hold the pickaxe you get the Night Vision effect. Common and cheap: enchanting
table and random loot.

**Ore Smelter (I)** (copper): ores you mine drop ingots instead of raw ore: iron, gold, copper and ancient debris
(netherite scrap). Works with Fortune, and on the blocks broken by Explosive and Excavation too.
Can't be combined with Silk Touch. (Typed "Orn Smelter" in the request; named Ore Smelter.)

**Explosive (I-X)** (orange): every block you mine has a chance to "explode": 10% per level (I = 10% ... X = 100%).
An explosion breaks all pickaxe blocks within 2 blocks of the one you mined.
- It is only a visual/sound explosion: it never damages you, mobs, items or anything except blocks.
- Broken blocks go straight into your inventory. If it is full, what doesn't fit drops on the ground.
- Fortune and Silk Touch on the pickaxe work on those blocks, ores give their experience.
- Never breaks unbreakable blocks, or blocks with contents (chests, spawners, furnaces...).
- Levels I-V from the enchanting table (rarer than Haste), VI-X by combining books in an anvil.

**Excavation (I)** (purple, rare): every block you mine also breaks the whole 3x3x3 cube around it (same rules as Explosive:
into the inventory, else on the ground).
- Can't be combined with Explosive.
- Only source: books in ancient city chests (4% per chest). Not in the enchanting table, not in other loot.

Only blocks a pickaxe is made for (stone, ores, ...) are affected, and only in survival mode. The extra blocks
do not use durability.

Build: `./gradlew build` -> `build/libs/pickaxe-enchants-1.0.0.jar`
Test in dev: `./gradlew runClient`
/give @s enchanted_book[stored_enchantments={"pickaxeenchants:speed":2}]
/give @s netherite_pickaxe[enchantments={"pickaxeenchants:speed":2,"pickaxeenchants:haste":10,"pickaxeenchants:night_vision":1,"pickaxeenchants:ore_smelter":1,"pickaxeenchants:explosive":10}]
/give @s netherite_pickaxe[enchantments={"pickaxeenchants:excavation":1,"pickaxeenchants:ore_smelter":1}]
