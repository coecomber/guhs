# NOTITIES_FIX_GEVECHT — fix round 1 (2026-11-04), branch 110-fix-gevecht

User feedback: bows spam too well (magic too, maybe melee), a universal leap, the training dummies ignore arrows/magic, and the blue
"W" before mob names. Lead decisions: DPS ratio **melee 100 / ranged 70 / magic 85**; dash = **Knuffelsprong**, key **C**;
basic attacks must not one-shot zone mobs of the same level (dense MMO packs).

## 1. Attack speed is a weapon stat (`kern.Aanvalstempo`)

- Base interval per weapon kind (`Aanvalstempo.SOORT`), per base type override (`Aanvalstempo.TYPE`):

| kind | s | | kind | s | | kind | s |
|---|---|---|---|---|---|---|---|
| dolk / kling | 0.7 | | boog | 1.1 | | toverstok | 0.9 |
| zwaard | 1.0 | | kruisboog | 2.0 (big hit, pierce) | | staf | 1.3 (splash) |
| knots | 1.4 | | katapult | 1.6 (splash) | | boek | 1.6 (strong homing) |
| hamer | 2.4 | | punch (no weapon) | 0.6 | | | |
| **pluishamer** (type) | **3.5** | | **klodderkling** (type) | **0.7** | | | |

- Aanvalssnelheid% divides the interval (clamped -50..+150 %). Tooltip line "Aanvalssnelheid: 1,1 s" (base interval), the damage line
  is now "N schade per klap".
- **Melee**: kern sets the vanilla ATTACK_SPEED attribute every tick to 1 / interval (`KernEvents.aanvalTempo`, replaces the old fixed
  1.6/s mapping), so the vanilla crosshair indicator is the ready indicator. `Schade.basisaanval`: a hit only lands with the vanilla
  cooldown ≥ 92 % AND past a server timestamp (`Schade.VOLGENDE_KLAP`); otherwise nothing at all (a soft "no damage" whoosh). The old
  25 % spam hits are gone.
- **Ranged/magic** (`klassen.Basisaanval`): the server interval is the same `Aanvalstempo` value (the old per-kind tick numbers 6/13/9/6/10/12
  are removed from `Basisaanval.Soort`). Holding the button: the client sends every 3 ticks, the server fires at most once per interval
  (a press up to 4 ticks early is buffered). The client restarts its own attack-cooldown ticker when it fires (prediction), so the
  vanilla crosshair indicator also works for bows/staffs.

### Damage model
- A weapon's `schade + perLevel x lv` is now its **damage per second rating** (Gewoon, before stats). `Spelers.wapenSchade(p)` =
  rating x class factor (this is what skills multiply). One basic hit = `Spelers.klapSchade(p)` = that x the base interval.
  So heavy weapons do big single hits with the same DPS.
- Class factor (`Aanvalstempo.FACTOR_*`): melee 1.0, ranged **0.68**, magic **0.79** = 70 % / 85 % divided by the extra value the
  jager (10 % crit, +10 % crit damage) and magier (+5 % elementschade, 11 Wijsheid) get from their own class stats.
- New ratings (Gewoon): kussenzwaard 5.5 + 1.1/lv, knuffelknots 5.3 + 1.05, pluishamer 5.5 + 1.1, klodderkling 5.2 + 1.05,
  pluisboog / druppelboog 5.5 + 1.1, snoepkruisboog 5.7 + 1.15, eikelkatapult 5.2 + 1.05, rozenstaf / vlammenstaf / zwamstokje
  5.5 + 1.1, slijmboek 5.3 + 1.05. **Bazen** named weapons (`bazen.BazenLoot`, outside my package, numbers only):
  baas_klodderknots / baas_pluisboog / baas_sporenstaf 6.5 + 1.3 (were 12/10/11 per hit: would have been 2x every other weapon).

### Skills (`klassen.Balans`, units = `Spelers.wapenSchade`)
Kussenklap 1.6 → **1.5**, Stormsprong 1.3, Vahoege Plof 3; Pluizenregen 3 x 0.6, Knuffelpijl 1.8 → **2.0**, Sterrenschot 4, Saltosalvo 0.7,
Plakbom 0.8; Roze Vuurbal 2.0 → **2.8** and cooldown 5 s → **4.5 s**, Glitterblink 1.2, Meteoorguh 5. Skill texts in the klassen generator updated.

### DPS table (single target, sustained, Gewoon weapon of that level, no gear bonuses/tree; `Balans.dps`, checked by `dpsTabelKlopt`)
Units = 1 (basic) + skill multiplier per second of cooldown (energy and ult ignored, AoE counted once):
ridder 1 + 1.5/3 + 1.3/8 = 1.66; jager 1 + 1.8/6 + 2.0/7 + Rolsalto-crit 0.08 = 1.66; magier 1 + 2.8/4.5 = 1.62.

| lv | ridder (kussenzwaard) | jager (pluisboog) | magier (rozenstaf) | jager/ridder | magier/ridder | weakest normal mob Vads |
|---|---|---|---|---|---|---|
| 1 | ~14.0 | ~9.9 | ~11.5 | 0.70 | 0.82 | 30 (Klodder 36) |
| 10 | ~51 | ~36 | ~42 | 0.70 | 0.83 | 111 (133) |
| 20 | ~101 | ~71 | ~84 | 0.70 | 0.83 | 201 (241) |

Basic-attack-only time to defeat a same-level Klodder: ~4.5-5 s melee, ~6.5 s ranged, ~5.5 s magic (+ splash). Before this fix:
jager ~28 dps at lv 1 (6-tick bow) = 1.3 s per Klodder; ridder ~15 (and spam-click hits).
**One-shot check** (`dpsTabelKlopt`): no Gewoon class weapon's non-crit, neutral-element basic hit one-shots the weakest normal mob of
its level (30 Vads +30 %/lv). The pluishamer comes closest (lv 1: ~28 vs 30). Note: a crit or a strong element (x1.5) can still one-shot
with the hammer, and rarer weapons (x1.1..x1.75) shorten every TTK. Global knob if fights feel too slow: raise all ratings together.

## 2. Knuffelsprong (`kern.Knuffelsprong`)
- Every class, from the start; key **C** (`key.guhs.vr_sprong`, category Vadsrijk, rebindable) = `KernPayloads.Skill(4)`; 5th small
  HUD slot (pink arc + heart, key letter, cooldown sweep + seconds) right of the 4 skill slots; `KernPayloads.Hud` cd/cdMax now 5 long.
- 5 s cooldown, no energy. A small hop first (on the ground the first tick's block friction would eat ~45 % of the forward speed),
  2 ticks later the arc (forward 1.95 b/t, up 0.85 b/t) → ~20 blocks far, ~5 high (simulated with vanilla air drag 0.91 / gravity 0.08 x 0.98; tune `VOORUIT`/`OMHOOG`). The client flies
  it with normal collisions (walls stop you, no clipping). Not while stunned, riding, gliding, flying or in a scene.
- No fall damage: protected from launch until 0.5 s after landing (max 5 s): fall distance reset every tick + `KernEvents` fall damage
  goes through `Knuffelsprong.valSchade`.
- Tip `vadsrijk.kern.tip.knuffelsprong`: 12 s after the first class choice (klassen `KlasseBalie` "kies"), or on the first use.

## 3. Training dummies count every attack
- Kern hook `Haken.BLOK_GERAAKT(ServerPlayer, BlockPos)`: fired by `klassen.schot.Schoten` when a shot hits a block, and by
  `Vaardigheden.gebruik` for the block under the crosshair (24 blocks) after a skill.
- **npc package edit** (allowed by the brief): `NpcHaken.klikBlok` → `NpcHaken.popGeraakt(p, pos)` (same rules: target/hay block in
  Knuffelkamp, max one count per 8 ticks), registered on `Haken.BLOK_GERAAKT`.

## 4. Element icons
- `Element.teken`: glyphs U+F8A1..U+F8A4 (heart Roze, flame Vuur, leaf Natuur/Zwam, drop Water/Slijm; GEEN = nothing), 16x16 pixel art at
  half size in `minecraft:default` (`assets/minecraft/font/default.json` + `assets/guhs/textures/font/vr_element.png`, generated by
  `vadsrijk_kern.py` `_iconen`). Nameplates show the icon instead of "[W]"; the weapon tooltip shows it before the element name.
- Tip `vadsrijk.kern.tip.elementen` (with the icons) on the first hit with/on an element (`Schade.doe`).
- **npc texts edited** (lang + `vadsrijk_npc.py`): Avonturiersgids "Elementen" explains the 4 icons; "Vaardigheden" mentions the attack
  speed/crosshair and the Knuffelsprong (C); the npc's own `tip.elementen` (after the first element mob) says "icoontje" now.
  It partly overlaps the new kern tip: npc may drop theirs.

## Tests
- `-Pgt=VadsrijkKlassen`: new `aanvalstempoHoudtInterval`, `dpsTabelKlopt`, `knuffelsprongGeenValschade` (+ the 3 old ones).
  **7/7 passed** (the gt filter showed a stale foreign value once: deleted `.gradle/configuration-cache`, then correct).
- AutoCheck `tools/autocheck/fix_gevecht.txt` (dev command `/guhs vadsrijk sprong` = pressing C), 0 problems:
  - leap from the south jetty (0,65,1140) north: 12 blocks + 4 up after 10 ticks, landed 18 blocks on and 5 higher at the top of the
    jetty stairs (stopped by the cliff wall, no clipping); a 2nd use during the cooldown is refused; next leap against the wall: 1 block.
    A full 20-block flight on open ground was not measured (the user should feel it in the dev client; tune `VOORUIT`/`OMHOOG`).
  - the 5th HUD slot (arc + heart, "C", cooldown digit), the tip card;
  - nameplates with drop / leaf / flame icons (Druipslijm, Hoedjesbol, Klodder), the Elementen tip with the icons in the text.
- Not checked in a client: the tooltip lines, the gids page (text only), the crosshair indicator with a bow (prediction).

## Resume point / open
- Done and committed on 110-fix-gevecht.
- Open: the PUA codepoints could clash with another mod's font glyphs (none in this pack known); the ridder's energy (6/s) vs
  Kussenklap spam (6.7/s) is near break-even by design.
