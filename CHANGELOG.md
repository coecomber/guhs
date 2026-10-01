# Changelog

All notable changes to Guhs. Versions follow `MAJOR.MINOR.PATCH`.
(The history from before the first public release is in [CHANGELOG-DEV.md](CHANGELOG-DEV.md).)

## 1.1.3 — Minecraft 26.1.2

A fix for the FTB Quests chapters. Same requirements as 1.1.2.

### FTB Quests: nothing is locked any more

- **No quest waits for "Guh!" any more.** Before, every quest hung (through a chain of lines) behind the very first
  quest. FTB Quests did remember what you had already done, but didn't tick it off: if you claimed "Guh!" late, a whole
  Guhdex of seen guhs could stay at 0 completed. Now every quest ticks itself off as soon as you've done it, in any order,
  also when you did it before (seen guhs, tamed guhs, advancements). Only the stomach sizes in De Guhmaag still come
  after each other.
- **Existing worlds are repaired.** Quests that were stuck at "done, but not ticked off" are ticked off by themselves a
  few seconds after you join.
- The lines between quests are gone; the section headers still say what comes logically first ("Komt na: ...").

## 1.1.2 — Minecraft 26.1.2

A new balance of the buildings in the Guhmension, after a look at the official server (1,399 buildings in 6 x 6 km:
caves everywhere, the minigames kilometres away). Same requirements as 1.1.1. Only the Guhmension changes (the
overworld stays as it was); in an existing world the new rules apply to land you haven't visited yet.

### Guhmension

- **Every minigame near spawn.** Each minigame now has one guaranteed building **700 to 1500 blocks from 0,0**: the
  Guhcircuit, the Guhdoolhof, the golf course, the race track, the Knabbelspelen, the Mika-Mephal, the Vadsig-Eetfestijn,
  the Beauty-theater, the disco, the kermis, the Verstopguh-huis, the Guhvis-vijver, the sjoelhuisje, the Knabbelkatapult,
  the sterrenwacht, the ballonfestival and the Knuffelbad. They lie spread around spawn, each on a spot where it fits
  (the right biome, flat ground, room), and they go before the ordinary buildings. The ordinary minigame buildings are also
  more common than before (every 32 chunks instead of every 44).
- **Landmarks within reach.** One guaranteed Guhkasteel, Guhdorp, Guhbibliotheek, Kaasmijn, Hemelkapelletje and set of
  Zwevende Eilanden **1500 to 2500 blocks from spawn**; the other copies stay where they were.
- **Story places further out.** The places of the stories (Nomguh, the sleehut, Guhwai'i, the kloon-eiland, the
  Hemelkapelletje, the Vadsig-heiligdom and Mika-kamp, the Knuffeldal town, the Elf-Guhjestocht, the Guhbubbel, Piep's
  nest, the Evil Mika home, the kampeerplekjes, the Guhkasteel and the barbecueput) never lie within **600 blocks** of
  spawn any more, so they feel like a journey. Nomguh is also rarer: about one per Sneeuwguhtoendra, no second one a
  few hundred blocks away.
- **Fewer of the most common buildings.** About half as many guh caves, gatenkaas mine shafts, challenging guh caves,
  mini picnics and quartz statues.
- `/guhs bouwcheck` also reports the guaranteed buildings (how many, how far) and the story building nearest to spawn.

### Wild animals no longer pile up

- Wild animals, critters and Mikas no longer pile up in the world (the same kind of bug as the guhs in 1.1.1; on the
  official server 2,901 zeemeeuwtjes, 2,495 pluisvinkjes, 2,219 guh bees and 1,054 Mikas were loaded). The birds' top-up
  around players only counted 64 blocks and every new flock was saved with its chunk.
- A wild one that a spawner brings while you play (the natural spawner, a mob spawner block, the birds' top-up, a
  ladybird coming to your tuintje) now comes and goes: it isn't saved when its chunk unloads and it despawns when every
  player is more than 128 blocks away (fish: 64), like a vanilla monster. New ones keep coming around you.
- Everything you keep stays exactly as before: tame, named, leashed, from a bucket, bred, placed by a building, Big Mika
  and the other bosses, the muisjes in the buildings. The animals the world is made with stay too, like vanilla cows.
- The birds' top-up also waits when there are 40 birds within 128 blocks, and a tidy-up every 30 s removes far-away wild
  ones of a kind when there are too many (above 200 + 100 per player), also the ones older worlds already saved.

## 1.1.1 — Minecraft 26.1.2

A bugfix for servers. Same requirements as 1.1.0 (Minecraft 26.1.2, NeoForge 26.1.2.71+, GeckoLib 5.5.2+, Java 25).

### Fixes

- **Server freeze in the Guhmension.** Wild guhs in the Guhmension come and go (they despawn when you walk away), but
  the ones in chunks that unloaded before they despawned were saved with the chunk and piled up over time: on the
  official server there were 70,000 entities (58,000 guhs) loaded in the Guhmension, and every step a player took made
  the server walk all of them, until one tick took more than 60 seconds and the watchdog stopped the server.
  Now these wild come-and-go guhs are never written to disk (tamed, named, kept or story guhs are, as before), the
  Guhmension spawner also waits when there are 150 guhs within 128 blocks of you, and a safety net tidies away wild
  guhs far from every player when there are more than 300 (+150 per player) in the Guhmension. Worlds that already
  have too many clean themselves up.
- **Riding loops are impossible.** Nothing can ride itself or something that already rides it any more, also not by a
  forced ride from Guhs or another mod (Minecraft itself doesn't check "rides itself"). A riding loop would freeze the
  server the same way.
- **Server crash from an eekhoorntje.** A pluiseekhoorntje (tamed and following you, or wild) that has just dug a
  knabbel stash crashed the server (`NullPointerException` in its stash goal, "Ticking entity"): the goal was ticked once more after
  the stash was done. Fixed, and the same slip is guarded in the Kikkerguh's tongue, the vadsige guh's snack hunt and
  the huisje/Guhkamer chores.

## 1.1.0 — Minecraft 26.1.2

Minecraft **26.1.2** · NeoForge **26.1.2.71+** (built on 26.1.2.112) · GeckoLib **5.5.2+** · Java **25**. All in-game texts
are in Dutch. Guhs 1.1.x is the 26.1.2 line; **1.0.x stays on Minecraft 1.21.1** (NeoForge 21.1, GeckoLib 4.8+, Java 21)
for packs that stay there.

The same mod as 1.0.0 (all guhs, dimensions, minigames, stories, quests and the Guhdex), ported to Minecraft 26.1.2.
Make a backup before you open a 1.0.0 world with 1.1.0: a world that was opened in 26.1.2 can't go back to 1.21.1.

### Official server

- **The official Guhs server in your server list.** The first time the title screen opens, *Guhs Server*
  (`guhs.nl`, online 24/7) is added once to the top of the multiplayer server list. It happens only once per
  installation (a marker file in `config/` remembers it), so a server you remove never comes back, and nothing is
  added when `guhs.nl`, `play.guhs.nl` or `2.28.142.15` is already in the list. Modpack makers can turn it off
  with `addOfficialServer = false` in `config/guhs-client.toml` (also in the mod list's Config screen).
- **Wiki:** a new page *Play on the official server* (Prism Launcher step by step, rules, commands, common problems).
- **guhs.nl:** the site moves to [guhs.nl](https://guhs.nl/): a new landing page (server address, live status, live
  map, how to join) and the wiki at [guhs.nl/wiki/](https://guhs.nl/wiki/). Old wiki links are sent on to the new place.

### Updating from 1.0.0

- **Worlds:** Guhs' saved data (Bank Guh, top-3 boards, Reisguhs, nests, band, huisjes, Guheinde fight, race ghosts, ...)
  is moved to Minecraft 26.1's new place in the world folder the first time it is needed.
- **FTB Quests:** FTB Quests for 26.1 only reads JSON5 files, so the *Guhs* chapter group (13 chapters, 734 quests) is now
  installed as `config/ftbquests/quests/chapters/guhs_*.json5`, with its texts in `lang/en_us/chapters/` (and in
  `lang/nl_nl/chapters/` when your pack has that folder). The old `.snbt` files of 1.0.x are not read by FTB Quests 26.1
  and are left alone; delete them if you like. Chapters you edit in the quest book are still never overwritten.
- **Spawn eggs** look the same, but are plain textures now (Minecraft 26.1 has no tinted spawn eggs).

### Small differences (Minecraft 26.1 works differently)

- **Fog**: the Vadswoud mist, the Kaasmoeras mist, the Sneeuwstorm blizzard and the Barbecuether smoke use Minecraft 26.1's
  fog (same distances and fade), which has no round or cylinder shapes any more: looking down from high up can be a
  little hazier than in 1.0.0.
- **Skies**: the Guhmension sun, pink moon and pink stars follow 26.1's sky (same day cycle); the night sky is a shade more
  purple near the top. The Guheinde swirl and the Guhpolder guh-snow look the same.
- **Blocks**: cutout textures with half-transparent pixels may render a little more see-through (26.1 picks the render
  layer from the texture). Three models (set tea table, set party buffet, lit Elftocht fire basket) got fixed texture
  coordinates so they load in 26.1.
- **Food and drinks** (kaasmelk, kokosmelk, kaashoning, tea, bakery pastries, warme chocovet, snert, stille knabbel,
  medicine drinks) use 26.1's drinking/eating system: same sounds, times and effects, vanilla's crumbs and timing.
  The Kaasmelkdrankje clears all effects before it heals (26.1 has no "milk-curable" list).
- **Armour and tools** (guh armour, Vads set, Kaashouweel, Leenhouweel, Duikhelm, Knabbelkroon, Guhvleugels) are
  26.1 component items with the same numbers. The Guhvleugels now also show on armour stands and other humanoids.
- **Saddling a guh** still works by right-clicking it with a saddle, but uses Guhs' own saddle (not 26.1's saddle slot),
  so shears don't take it off.
- **Guh villager trades** still come from Guhs itself (two random offers per level, like 1.0.0).
- **Knabbelkristallen** in the Guheinde are their own entity now (same look, beam and fight).
- The **VAHOEG!** emote text disappears when its centre leaves the screen; a few NPCs may show a hurt flash when hit
  (they still take no damage). GUI entity previews and item icons are lit a little brighter (26.1).
- Players are woken up before Guhs teleports them (Guhmaag, Reisguh, verstoppertje, castle gate).

### Fixes

- The katapult advancements *Katapult gevonden*, *Drie sterren* and *Alle sterren* (and their quests) never loaded in
  1.0.0 (their icon had no item); they work now.
- Giant and scaled guhs and Mikas in structures have their proper size again.

### For modpack makers and server owners

- Needs Java 25 (Minecraft 26.1 does too). Install Guhs and GeckoLib on the server **and** every client.
- `/test` ids are `guhs:<class>.<method>` as before; the headless test run is `./gradlew runGameTestServer`.

## 1.0.0 — first public release

Minecraft 1.21.1 · NeoForge 21.1.0+ · GeckoLib 4.8+. All in-game texts are in Dutch.

- **The guh**: a chubby pink plush mouse in every overworld biome, in random sizes. Tame it with kaas knabbels,
  name it, pick it up, ride it with a saddle, and manage it in the guh menu (behaviour, sounds, emotes, wardrobe,
  diary).
- **20+ guh variants** and the **Guhdex** collection book, with tabs for your guhs, the Knuffeldal, minigame
  records, clothes and stories.
- **Hearts & friendship**: a heart meter per tamed guh with three levels, secret favourites, guh friendships,
  **guhhuisjes** (guh-head houses) with ten kinds of chores, toys, the Guhkamer and a diary per guh.
- **Clothes**: 150+ unlockable pieces in seven slots, guh armour and a guh backpack.
- **The Guhmension**: a pink dimension (Block of Kaasknabbels portal) with many biomes (Knuffeldal, Guhpolder,
  Guhwai'i, Sneeuwguhtoendra, Diepe Guhzee, Gatenkaasgrotten, Kaasmoeras, Vadswoud and more), guh villages with
  six guh professions, rare buildings (Guhland, the guh castle, the kermis, floating islands, the Guhramid, ...),
  seasons, events and a super compass.
- **Minigames**: more than twenty, each with a host guh, coins, a shop, an outfit and world top-3 boards.
- **Stories**: four story questlines with talking screens and a story guh per player (Baltoguh en Nomguh,
  het kloon-eiland, het Hemelkapelletje, Guhwai'i), plus the Timmerguh's house-building quest.
- **Dimensions**: your own Guhmaag (pocket dimension), the Guhbarbecuether and the endgame Guheinde with Opper-Mika.
- **Critters**: pieppiepmuisjes, plush turtles, birds, ducklings, butterflies, fireflies, bunnies, hedgehogs,
  squirrels, guhxolotls and more.
- **Blocks & items**: Bank Guh (infinite storage), guh wheel + guh wire (redstone), frying pan, kaas saus fluid,
  vads tools and armour, sled + rails, furniture, food and decoration.
- **FTB Quests**: a self-installing *Guhs* chapter group (13 chapters, 700+ quests, nothing locked).
- **Advancements** in their own tabs.
- Overworld guhs are a little rarer than sheep and cows (but in every biome); story buildings are rare finds.
