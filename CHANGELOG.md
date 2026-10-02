# Changelog

All notable changes to Guhs. Versions follow `MAJOR.MINOR.PATCH`.
(The history from before the first public release is in [CHANGELOG-DEV.md](CHANGELOG-DEV.md).)

## 1.2.5 — Minecraft 26.1.2

- **JEI's "+" works in the Bank Guh.** With JEI installed, the "+" on a crafting recipe fills the Bank Guh's crafting grid:
  items come from the Bank Guh first and from your inventory second (shift-click: as many as fit). Missing items are
  shown in red, like at a crafting table.

## 1.2.4 — Minecraft 26.1.2

- **The Timmerguh finishes the last bits himself.** With 2 or fewer roof spots left, talk to the Timmerguh: those spots
  are tricky to reach, so he taps them in himself and the roof is done. Works for existing building sites too.

## 1.2.3 — Minecraft 26.1.2

- **The Timmerguh's roof can always be finished.** A few ghost tiles of the dome lie right under its top and could only be
  clicked from inside. Now a roof fluff on the roof right next to an open spot lays that spot, and when you talk to the
  Timmerguh while the roof isn't done, little sparkles show where the open spots are.

## 1.2.2 — Minecraft 26.1.2

- **Moving in cheese sauce, for real this time.** 1.2.1 added the movement but in a place Minecraft 26.1 never calls, so
  you were still stuck in kaas saus, stomach acid and the Barbecuether's frying sauce. Now you wade through them slowly,
  like lava (a new test walks a pig through all three).

## 1.2.1 — Minecraft 26.1.2

Small fixes. Same requirements as 1.2.0.

- **Moving in cheese sauce.** You couldn't move at all in kaas saus (cheese sauce) or stomach acid. Now you wade and
  swim through it, slowly, like through lava.
- **Bedrock floor in the Guhmension.** The bottom layer is bedrock instead of netherrack (for land you haven't visited
  yet; existing chunks keep their floor).
- **Tamed guhs are safe.** You can't hurt someone else's tamed guh any more (also not with arrows), and a tamed guh never
  hurts a player.
- **Travel Guhs in buildings stay put.** A Travel Guh that came with a building can't be picked up any more (you couldn't
  put it down in a building again either).

## 1.2.0 — Minecraft 26.1.2

Guhs speaks English! Until now every text in the mod was Dutch, also with Minecraft set to English. Now the whole mod
is in English and in Dutch. Same requirements as 1.1.3 (Minecraft 26.1.2, NeoForge 26.1.2.71+, GeckoLib 5.5.2+,
Java 25).

### A full English translation

- **Everything in English:** item and block names, tooltips, menus, the Guhdex, dialogues and answers, chat messages,
  advancements, the guh menu, the Super Compass, minigame scoreboards, the stories and the books.
- **The guh way, in English.** The guh words have English twins (*vadsig* is *chonky*, *njeg* is *nyeg*, *vahoeg* is
  *wahoog*), the puns are English puns, and the places and characters have English names: Knuffeldal is
  **Snuggledale**, the Guhmaag is the **Guhbelly**, the Guheinde is **the Guh End**, Opper-Mika is **Overlord Mika**,
  kaasknabbels are **cheese nibbles**. Guh, guhs, the Guhmension, Mika and the Guhdex keep their names.
- **The FTB quest book in both languages:** all 13 chapters with their 734 quests, including the chapter and section
  pictures (they have an English version).
- **Signs and books in the buildings** (the Eleven Guhtowns Tour, the golf course, the Guhfish Pond, Guhwai'i, the Mika
  Camp...) are shown in the language of each player, also on a server.

### The language switch

- **Auto** (the default) follows your Minecraft language: Dutch when Minecraft is set to Dutch (`nl_nl`), English
  for every other language.
- **The first time you join** a world or a server after installing, a small screen asks *"Welke taal wil je voor
  Guhs? / Which language for Guhs?"* (in both languages): **Nederlands**, **English** or **Automatisch / Automatic**
  (follows Minecraft). It asks once per installation (`languageChosen` in `config/guhs-client.toml`).
- Change it later with the **language button under your Guhdex** (*Lang* / *Taal*: Auto, NL or EN; the Super
  Compass has one too), or with *Guhs language* in the mod's settings (*Mods* → *Guhs* → *Config*; in the file:
  `language = "AUTO"`, `"NL"` or `"EN"` in `config/guhs-client.toml`). Lost your Guhdex? You get a new one when
  you go to the Guhmension.
- The choice is per player and only on your own computer: on a server, every player reads Guhs in their own language.
  Minecraft's own texts keep following the Minecraft language.

### Good to know

- **Old saves keep their old texts.** Names and lines your world saved before 1.2.0 (band names, diary lines, guh
  house names, ...) stay as they were, in Dutch. Signs in buildings that were already generated before 1.2.0 stay
  Dutch too; buildings in new land get the two-language signs.
- **Chat stays as it was written:** messages you got before switching the language don't change. A sign you look at
  switches at once; a minigame scoreboard switches when it refreshes.
- Some English texts are longer than the Dutch ones: a Guhdex page with a long description uses a smaller font.
- The [wiki](https://guhs.nl/wiki/) uses the new English names too.

### The official server

- **Guhs no longer adds the official server to your server list.** The *Guhs Server* entry (and the
  `addOfficialServer` option in `config/guhs-client.toml`) is gone. Already have it in your list? It stays there.
- The English side of the wiki no longer covers the official server; it's on the Dutch side only.

### Petting

- **A short right-click on your own guh is now only petting.** It no longer makes your guh sit down or stand up: use
  *Sit* / *Stand Up* in the guh menu (hold right-click). A big guh with a saddle still gets ridden with a tap, and
  sneak + right-click still picks it up.
- **Every pet is a sweet moment**, also when today's petting hearts are used up (the hearts still have a daily cap):
  your guh squishes flat and wide with its eyes happily shut, wiggles, pushes its head and a paw up against your
  hand, pink hearts pop up with a happy squeak, and the action bar says *"You pet ...! ♥"*. The first three times it
  also reminds you that holding right-click opens the guh menu.

### Also new

- **Wild Guh Shoo-Sign** (*Wilde-guhweerder* in Dutch): a little pink sign with guh ears and a sleeping guh face
  ("a chonky lives here already") for your house or base. No wild guhs spawn in the area around it (8/16/24/32/48
  blocks, right-click to choose); your own guhs, babies and spawn-egg guhs are welcome, and guhs already there stay.
  *Show the Area* shows it as the same blue dome as the Guh House's chore area. Only the one who placed it changes
  or breaks it. Recipe: 5 pink wool, a cheese nibble and a stick.

### Fixes

- **The Timmerguh's roof works in survival.** Laying a roof fluff on the ghost tiles of the half-built guh house in
  Snuggledale said "the town is so cozy, nothing may change here" and put the tile back (only creative worked).
- **No more raw text keys.** The Timmerguh's little helper (Tappy, *Timmertje* in Dutch) showed
  `entity.guhs.bewoner.timmertje` above his head; 41 block tooltips (the Baltoguh statue, party garlands, the cradle,
  the Chonkiness Scanner...) showed `item.guhs.<name>.lore`; ordinary guhs in the Cloud Chapel showed
  `entity.guhs.guh.normal`. A new test checks every name, tooltip and building text.

## 1.1.3 — Minecraft 26.1.2

A fix for the FTB Quests chapters. Same requirements as 1.1.2.

### FTB Quests: nothing is locked any more

- **No quest waits for "Guh!" any more.** Before, every quest hung (through a chain of lines) behind the very first
  quest. FTB Quests did remember what you had already done, but didn't tick it off: if you claimed "Guh!" late, a whole
  Guhdex of seen guhs could stay at 0 completed. Now every quest ticks itself off as soon as you've done it, in any order,
  also when you did it before (seen guhs, tamed guhs, advancements). Only the stomach sizes in De Guhmaag still come
  after each other.
- **Existing worlds are repaired.** Quests that were stuck at "done, but not ticked off" are ticked off by themselves a
  few seconds after you join (and then every 2 minutes).
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
