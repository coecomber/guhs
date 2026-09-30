# Guhs development history (before 1.0.0)

Guhs was built privately from 23 to 30 September 2026 and went through internal versions 1.x to 3.0.0. The
public release restarted the numbering: **internal 3.0.0 + a small fix round = public 1.0.0**. This file keeps the
old history for the curious; it is not needed to play. Player-facing changes from 1.0.0 on are in
[CHANGELOG.md](CHANGELOG.md).

Worlds from the internal versions were never meant to carry over; start a new world for 1.0.0.

## Public 1.0.0 fix round (on top of internal 3.0.0)
- Overworld guh spawns made much rarer (a bit rarer than sheep/cows, still in every biome).
- Crash fix: the story sled no longer sends a vanilla-reserved entity event (it was read as a sniffer event by clients).
- New Guhdex tab **Verhalen**: your progress in every story (current step, what you need, where to go next).
- Story buildings rarer; the Diepe Guhzee a bit less common.
- The kloon-eiland tower stairs fixed (headroom at the top, entrance step height).
- Version reset to 1.0.0; the wiki became a multi-page site on GitHub Pages.

## 3.0.0 — Guhverhalen (30 Sep 2026)
- Four story questlines, each with its own place, characters, a talking screen with answer buttons, clothes,
  advancements, FTB quests and a **story guh** tameable once per player: **Baltoguh en Nomguh** (new biome
  Sneeuwguhtoendra, a steered medicine sled ride through a storm, the repeatable sledesprint, your own sneeuwslee),
  **het kloon-eiland** (lab notes, the kloontank, Mieuwguh and the floating Guhtwo), **het Hemelkapelletje**
  (the Knuffelhart revives your lost tamed guhs; a dead guh leaves a *Herinnering* star) and **Guhwai'i** (new
  tropical biome, the ohana questline, the wall-climbing 626-guh, the vadsigheid-scanner).
- **De Timmerguh**: a building site in every Knuffeldal town; guhhuisje recipes need his bouwboekje; owner-only huisjes.
- **Surfen & hula** on Guhwai'i: waves and tricks, a rhythm game on three original songs, the Tiki shop.
- **Diertjes van de Guhmensie**: 13 critters (birds, guhxolotl, ducklings, insects, hedgehog, bunny, squirrel, Sjokkel).
- 16 new clothes, 29 new Guhdex pages (95), superkompas tab *Verhalen*, two advancement tabs, FTB chapters 12-13
  (734 quests, `CHAPTER_VERSION` 19).
- Tested: full GameTest suite (738 tests), a 3028-structure overlap check, five client screenshot rounds.

## 2.10.1 — small fixes (released with 3.0.0)
- Dagboekje opens the right guh; the Rookguh got its own Guhdex page; Guhkamer buttons in the guh menu; more
  elfstempels; soft guh-snow in the Guhpolder instead of vanilla snow; Reisguhs in the big places.

## 2.10.0 — Lieve vadsjes van elkaar (29 Sep 2026)
- **Hartjesmeter** per tamed guh (100 / 600 / 2000: *lieve vadsjes*, *mega lieve vadsjes*, *zielsguh bff 5evr <3*),
  each level with clothes and an emote.
- **Favorietjes** (eight secret favourites per guh), the **Guhhuisje** (3/5/8 residents), **ten klusjes**, toys,
  *Samen* reactions in all minigames, guh friendships, the Guhdex tab *Mijn guhs* (diary), the **Guhkamer** + Guhbel.
- Fixes: land meets sunk buildings, circuit reset loop, three golf tees per hole, floating doolhof knabbels, a
  rebuilt Elf-Guhjestocht. FTB: 593 quests. Full suite: 647 tests.

## 2.9.0 — De Grote Guhspelen (28 Sep 2026)
- Six new minigames: Sjoelhuisje, Guhdoolhof, Knabbelkatapult, Knabbelspelen (+ Grote Zeskamp), the
  **Elf-Guhjestocht** (a skating tour past 11 villages in the new biome **Guhpolder**, with the **Pinguh**) and the
  **Guh-Circuit** (three tracks, ghosts).
- Levels (makkelijk / medium / lastig) for the older games; real songs in the Guhdisco.
- **Clothes became unlocks** (use a piece once, all your guhs can wear it), a new wardrobe, the ear slot, one source
  per piece. Guhdex and superkompas got icon tabs. **Beroepen**: four one-time jobs with work outfits.
- Menus for the muisje and turtles, sleeping guhs close their eyes.

## 2.8.1 — Piep! (27 Sep 2026)
- The **pieppiepmuisje** (shoulder pet, hide and seek), the plush turtles **Poepschilly** and **Schilly**, the
  **Roze Guh Koek**, and the **kaasknabbel-nest** boss fight (Boze Oppernabbel). Guhdex section and FTB chapter *Piep!*.

## 2.8.0 — Het Knuffeldal
- The cosy biome **Knuffeldal** with one town per world: the Grote Knusfeest, Kruimel-Mika's, the Pluisguh,
  seasons and a day rhythm for guhs.
- Town buildings: bakery, tea house, hairdresser (hair slot + dyes), crèche; around it: farm, gardens, observatory,
  balloon festival, campsites and the Knuffelbad water slides.
- Guhleven: the ice-cream guh, a guh choir, the claw machine with 21 plushies. FTB quests became a chapter group.

## 2.7.0 — De Guhbarbecuether
- A Nether parody dimension (five biomes, grillkool portal, Grillguh, Rookguh, Vonk-/Knekel-Mika, Asguh, the
  Spiesburcht and Mika-grillpaleis, brewing, the boss Aangebrande Mika, the Knabbelbaken).
- A livelier Guhmension: Gatenkaasgrotten (with the Stille Voorraadkelder), Kaasmoeras, Vadswoud with a treehouse
  village, guh families and nests, the **Diepe Guhzee**, a Highscores tab. Guhs are always passive.

## 2.6.0 — Het Guheinde
- The endgame: an End parody. Mika-tranen, the Oog van Vadsig, four Knabbelkelders, the Guheinde dimension with
  Opper-Mika on a starved Enderguh, knabbelkristallen, and rewards (Knabbelkroon, Vahoege Enderguh, Guhvleugels).

## 2.5.0
- **Guh events** (kaasregen, Vadsparade, sterrenregen), **guh emotes**, the underwater **Guh Bubble** with the
  Zeemeerguh, and guh music on a record.

## 2.4.0 (25 Sep 2026)
- Seven minigames in their own buildings (Guh Beauty, Guhrace, Mika meppen, Guhdisco, Guhgolf, Vadsig eetfestijn,
  Guhvis-wedstrijd) with hosts, tickets, outfits and top-3 boards; the floating islands (Wolkguh), the cheese mine
  and the guh library. Protected buildings. Built in parallel feature packages (`feature/<name>`).

## 2.3.0
- The **guh castle** with the Koningguh, guh paintings, **Reisguhs** (waypoints) and the **super compass**.

## 2.2.0
- The **verstopguh house**: a hide-and-seek minigame in an 80x80 dollhouse with one-way glass.

## 2.1.0
- The **guh kermis** with a real sled coaster, coaster rail pieces, the flying **Ender guh**, wider menus.

## 2.0.0 (24 Sep 2026) and 2.0.1
- The **Guhmaag** pocket dimension and its questline *De ontvoerde guh*, the **guh sled** and rails, launching
  from your guh, 41 clothes with a wardrobe and backpack, pink moon and stars, the Guh Sea, crystal mines, guh
  bees/slimes/fish and Nether Mikas, the **Guhdex**, food and furniture, kaasknabbel farming. 2.0.1: guh blossom trees.

## 1.x (23 Sep 2026, up to 1.8.0)
- The guh itself (Lieke's model), kaas knabbels and kaasknabbel ores, the Guh Portal and the **Guhmension** with its
  first biomes, hamster houses, Mika and Big Mika, guh caves and challenge caves, cheese fountains, Guhland, the
  frying pan, guh wheel and guh wire, the Hungry Guh picnic and the **Bank Guh**, kaas saus, guh armour, saddles,
  names, the guh menu.
- 1.8.0: vads ore, tools and armour; guh variants; personalities; clothes; guh villages with six guh professions;
  the Guhramid; hand-built decorations imported from a build world.
