<p align="center"><img src="docs/wiki/img/guh_front.png" width="160" alt="A guh"></p>

<h1 align="center">Guhs</h1>

<p align="center"><i>Add lieve vadsige guhs to Minecraft!</i></p>

<p align="center">
NeoForge 1.21.1 · requires GeckoLib 4.8+ · version 1.0.0<br>
<a href="https://www.curseforge.com/minecraft/mc-mods/guhs">CurseForge</a> ·
<a href="https://modrinth.com/mod/guhs">Modrinth</a> ·
<a href="https://guhs.nl/">guhs.nl</a> ·
<a href="https://guhs.nl/wiki/">Wiki</a> ·
<a href="CHANGELOG.md">Changelog</a>
</p>

---

**Guhs** adds the *guh*: a chubby pink plush mouse that is very, very *vadsig* (lovably chubby). Tame one with
kaas knabbels (cheese puffs), dress it up, ride it, cuddle it until you are *zielsguh bff 5evr <3*, and follow it
through a portal into the **Guhmension**, a whole pink world full of guhs, cheese, minigames and little stories.

Guhs are always friendly. Nothing in the mod makes them hurt you, and the one grumpy guh (Mika) only shoves.

> **Language:** all in-game names and texts are in **Dutch** (also when your game is set to English), with a lot of
> guh puns. The [wiki](https://guhs.nl/wiki/) is in English and Dutch.

## Features

- **The guh.** Spawns in every overworld biome in random sizes (tiny to three blocks long). Tame it with kaas
  knabbels, name it, pick it up (sneak + right-click), ride big ones with a saddle, and hold right-click for the
  **guh menu**: sit, follow, behaviour, sounds, emotes, wardrobe, diary and more.
- **20+ guh variants** (Mint, Choco, Ghost, Starry, Rainbow, Golden, Pinguh, Ender guh you can fly, Zeemeerguh that
  swims, ...), each with a page in the **Guhdex**, your in-game collection book.
- **Hearts & friendship.** Every tamed guh has a heart meter with you, eight secret favourites to discover, friends
  among other guhs, chores it can do from its **guhhuisje** (a house shaped like a guh head), toys, and a diary.
- **Clothes.** More than 150 outfit pieces in seven slots (hats, glasses, ears, hair, jackets, backpacks...), each
  unlocked once from its own source and then wearable by all your guhs. Plus guh armour and a backpack.
- **The Guhmension.** Pink wool hills, cheese-sauce rivers, a pink moon, and many biomes: the cosy Knuffeldal,
  the icy Guhpolder, the tropical Guhwai'i, a snowy tundra, deep guh seas, cheese caves, a cheese swamp and giant
  guh forests.
- **Places to find.** Hamster-house playsets, guh villages with guh villagers and guh professions, a 128x128 guh
  theme park, a gothic guh castle with a Koningguh on the throne, a guh fair with a real sled coaster, floating
  islands, a pink pyramid, cheese fountains and much more. A **super compass** points the way.
- **Minigames.** More than twenty of them, each in its own building with its own guh host, coins, shop, outfit and
  world top-3 boards: guh race, golf, disco, fishing, sjoelen, a hedge maze, a catapult, a skating tour past eleven
  villages, a kart circuit, surfing, hula, baking, hairdressing and more.
- **Stories.** Four story questlines with their own characters, talking screens with answers, and a special story
  guh to tame at the end (a medicine sled ride through a snowstorm, a clone-island lab, a chapel in the clouds that
  brings lost guhs back, and a tropical *ohana* island).
- **Three extra dimensions.** Your own **Guhmaag** (a pocket dimension inside a giant guh), the fiery
  **Guhbarbecuether** and the endgame **Guheinde**, where Opper-Mika has stolen every kaasknabbel in the kingdom.
- **Little critters.** Pieppiepmuisjes, plush turtles, birds, ducklings, bunnies, hedgehogs, squirrels, axolotl
  guhs and more, most of them tameable.
- **Useful blocks.** The **Bank Guh** (infinite storage with search and a crafting grid), the guh wheel and guh wire
  (redstone), the frying pan, sled rails, furniture, food and lots of decoration blocks.
- **FTB Quests support.** If FTB Quests is installed, a *Guhs* chapter group (13 chapters, 700+ quests, nothing
  locked) installs itself.
- **The official Guhs server.** Play together on `guhs.nl` (24/7, open to everyone): Guhs adds *Guhs Server* to
  your multiplayer list once (turn it off with `addOfficialServer = false` in `config/guhs-client.toml`). How to
  join: [Play on the official server](https://guhs.nl/wiki/server.html).

Everything is explained, with pictures, in the **[Guhs wiki](https://guhs.nl/wiki/)**.

## Requirements

| | |
|---|---|
| Minecraft | **1.21.1** |
| Loader | **NeoForge** 21.1.0 or newer |
| Required | **[GeckoLib](https://modrinth.com/mod/geckolib)** 4.8 or newer |
| Optional | FTB Quests (adds the Guhs quest chapters), JEI, Jade |
| Recommended for servers | **[Lootr](https://modrinth.com/mod/lootr)**: every player gets their own loot from structure chests |

Install Guhs on **both** the server and every client.

## Installing

1. Install NeoForge for Minecraft 1.21.1 (or make a NeoForge 1.21.1 instance in Prism Launcher, the CurseForge app
   or the Modrinth app).
2. Put `guhs-1.0.0.jar` and GeckoLib in the `mods` folder (launchers can download both for you).
3. Start the game and make a **new world**. Find a guh, give it kaas knabbels, and enjoy.

Want everything ready to go? Try the **Guhs Pack** modpack (Guhs + GeckoLib, JEI, Jade, JourneyMap, AppleSkin,
Mouse Tweaks, Lootr and FTB Quests).

## Modpacks, videos and servers

You may put Guhs in any modpack and make videos or streams about it (monetised is fine). You may not re-upload it
on its own. See [LICENSE](LICENSE).

On first start Guhs adds the official server (*Guhs Server*, `guhs.nl`) once to the top of the multiplayer server
list. Making a pack with its own server list? Set `addOfficialServer = false` in `config/guhs-client.toml`.

## Building from source

You need a **JDK 21**.

```sh
./gradlew build              # -> build/libs/guhs-<version>.jar
./gradlew runClient          # a dev Minecraft with the mod
./gradlew runGameTestServer  # the automated in-game tests (headless)
```

The generators for models, textures, sounds, structures, FTB quests and the wiki live in `tools/` (Python 3.10+,
`pip install -r tools/requirements-wiki.txt`). Run them from the project root. The Blockbench model sources are in
`blockbench/`.

## Credits

- **Guh model:** Lieke (the original Blockbench guh).
- **Everything else** (code, sounds, music, textures, structures, stories): Juiced.
- Built with [NeoForge](https://neoforged.net/) and [GeckoLib](https://github.com/bernie-g/geckolib).

Guhs is a fan-made mod and is not affiliated with Mojang or Microsoft. The stories are affectionate parodies; the
names and characters they poke fun at belong to their owners.

---

## Nederlands

**Guhs** voegt de *guh* toe: een mollig roze knuffelmuisje dat heel erg *vadsig* is. Tem hem met kaasknabbels, kleed
hem aan, rij op hem, knuffel hem tot jullie *zielsguh bff 5evr <3* zijn, en volg hem door een portaal naar de
**Guhmensie**: een hele roze wereld vol guhs, kaas, minigames en verhaaltjes. Guhs zijn altijd lief.

**Wat zit erin?**
- De guh: tem, noem, pak op, rij, en open het guhmenu (rechtsklik ingedrukt houden).
- Meer dan 20 guh-varianten en de **Guhdex** om ze te verzamelen.
- Een hartjesmeter per guh, geheime favorietjes, vriendjes, klusjes vanuit je **guhhuisje**, speelgoed en een dagboekje.
- Meer dan 150 kledingstukjes in zeven vakjes, guh-harnas en een rugzak.
- De **Guhmensie** met veel biomen: Knuffeldal, Guhpolder, Guhwai'i, de sneeuwtoendra, diepe guhzeeën, kaasgrotten en meer.
- Gebouwen om te ontdekken: hamsterhuizen, guhdorpen met guh-dorpelingen, het pretpark Guhland, een guhkasteel, een kermis met achtbaan, zwevende eilanden en veel meer, met een **superkompas**.
- Meer dan twintig **minigames**, elk met een eigen gebouw, gastguh, munten, winkel, pakje en top-3-borden.
- Vier **verhalen** met personages, gesprekken met antwoordknoppen en een bijzondere verhaalguh als beloning.
- Drie extra dimensies: je eigen **Guhmaag**, de **Guhbarbecuether** en het **Guheinde** met Opper-Mika.
- Diertjes: pieppiepmuisjes, knuffelschildpadjes, vogeltjes, eendjes, konijntjes, egeltjes en meer.
- Handige blokken: de **Bankguh** (oneindige opslag), het guhwiel en guhdraad, de frituurpan, sledebanen en meubels.
- **FTB Quests**: is het geïnstalleerd, dan komt er vanzelf een Guhs-hoofdstukkengroep bij (13 hoofdstukken, 700+ quests).
- De **officiële Guhs-server** `guhs.nl` (dag en nacht aan): Guhs zet *Guhs Server* één keer in je serverlijst (uitzetten: `addOfficialServer = false` in `config/guhs-client.toml`). Zo speel je mee: [Speel op de officiële server](https://guhs.nl/wiki/server.html).

**Nodig:** Minecraft 1.21.1, NeoForge 21.1.0+, GeckoLib 4.8+. Installeer Guhs op de server én bij elke speler.
Alle teksten in het spel zijn Nederlands. **Aanrader voor servers:** Lootr, zodat elke speler zijn eigen buit uit
kisten krijgt.

**Modpacks en video's:** je mag Guhs in elk modpack stoppen en er (ook betaalde) video's en streams over maken. Los
opnieuw uploaden mag niet. Zie [LICENSE](LICENSE).

**Met dank aan:** Lieke voor het guh-model. Al het andere (code, geluidjes, muziek, gebouwen, verhalen): Juiced.

Alles staat met plaatjes in de **[Guhs-wiki](https://guhs.nl/wiki/)**.
