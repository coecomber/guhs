# NOTITIES T-WOLKWAL - de zone-5-restjes van T-Z5WERELD

Nachtronde T (2026-11-06). Worktree `guhs_vadsrijk/t-wolkwal`, branch `110-t-wolkwal` (van 110-t-z5wereld 01d19715c).

## Wat er gebouwd is

### 1. Wolkwacht-ridder (eigen GeckoLib-model)
- Generator `tools/vadsrijk/wolkwal/maak_modellen.py` (geen Blockbench; PIL + json, ook aangeroepen door de feature
  `tools/features/vadsrijk_t_wolkwal.py`). Het model = de KERNbotten van `guh.geo.json` (root, body, tail, 4 poten, head, oren:
  zelfde UV's) + harnasbotten: `helm` (kap, kruin, gouden rand, neusstuk, wangplaten), `pluim` (wit/blauw, wiebelt), `kuras`
  (borst + rugplaat, gouden riem, gouden wolkje), `kraag` (wolkenkraag), `cape`, `schild` (klein wolkenschild links), `zwaard`
  (schede + gevest rechts), `laars_*` (laarsjes met gouden band). UV-ruimte 256x256, 4 texels per eenheid: links-boven = de
  echte guh-textuur van die vacht, de rest = het harnas. 6 texturen: guh, guh_snow, guh_golden, guh_mint, guh_choco, guh_wolk.
- Animaties: `wolkwacht_ridder.animation.json` = guh idle/walk/sit + pluim-wiebel. Alle guh-animaties passen (zelfde botnamen).
- Java `zone5/wolkwal/Wolkwacht`: model/textuur/vacht-keuze, `isWolkwacht(hoofd, nek)` (ridderhelm + wolkenkraag), `prop(...)`.
- **Z5Bewoners**: de ridders (soort 2) zijn nu Wolkwacht-ridders (vacht wisselt per index, schaal 0.85).
- **NPC's (t-z5verhaal `z5_wacht_*`)**: `zone5/wolkwal/client/WolkwachtGuhRenderer` = `GuhRenderer` met een omhulsel-GeoModel:
  een `RijkGuh` met KNIGHT_HELMET (hoofd) + WOLKENKRAAG (nek) krijgt het riddermodel + de riddertextuur in zijn eigen vacht, de
  kleren worden niet getekend. Alle andere NPC's: precies als voorheen. Geregistreerd in `NpcClient` (1 regel, zie onder).
  Sync is niet nodig (de kleren staan al op de client). Werkt dus vanzelf na de merge met t-z5verhaal.
- **Sparringridder (t-z5wezens)**: zelfde ridder op HUN botten (root/lijf/helm/arm_l/schild/arm_r/wapen + de guh eronder), met
  pootjes, kussenzwaard en een groot wolkenschild: `geckolib/models/entity/vadsrijk/zone5/wolkwal/sparringridder.geo.json` +
  `textures/vadsrijk/zone5/wolkwal/sparringridder{,_elite,_gloed}.png`. NIET over hun bestanden gezet (die bestaan in deze tak
  niet; anders merge-conflict). **Integratie:** `python tools/vadsrijk/wolkwal/maak_modellen.py --sparring-over` kopieert ze over
  `vadsrijk/zone5/wezens/sparringridder.*` (hun renderer/entity/animaties blijven). Hun animaties draaien om MIJN pivots (lijf
  [0,2,0], armen [+-8.5,7,-6]): even in de client kijken of het slaan/blokken/buigen er goed uitziet.

### 2. Moeder Wolkwal beweegt (`zone5/wolkwal/WolkwalBeweging`)
- 4 bewegende puntjes (2 borstvinnen, 2 staartlobben): `Z3Figurant`-props (geen hitbox, niet opgeslagen, noPhysics) met een eigen
  model (`vintip`, `staarttip`; 1 modelpixel = 1 blok bij schaal 16, getegeld met de echte walvishuid/walvisbuik-bloktextuur).
  Ze zitten OP het statische puntje en wuiven (vin 4 s, staart 5 s; het buitenste segment ijlt na). Alleen als er een speler binnen
  192 blokken is (weg boven 240): max 4 entities. Posities uit de kaart (kit/wolkwal.py; zie de javadoc).
- Wolkslierten achter de 4 puntjes (CLOUD + WHITE_ASH, drijven oostwaarts), alleen naar spelers binnen 160 blokken.
- Spuitgaten: elke 400 ticks (20 s) 2,5 s spuiten langs de bestaande statische wolkenzuil: wolkjes omhoog, een pluim bovenin,
  druppels (FALLING_WATER), FIRE_EXTINGUISH laag. Dev: `wolkwal spuit`.
- De beloopbare wolkplatforms van t-z5wezens (vinnen/staart) zijn niet aangeraakt; gametest checkt >25 blokken afstand.

### 3. Panorama's (`zone5/wolkwal/WolkwalUitzicht`)
- Ooggaarde (trigger binnen 9 van `ooggaarde_tuin`/`balkon_oog`/`kampvuur_ooggaarde`) en Staartpark (`staartpark_prieel`/`_vijver`/
  `kampvuur_staartpark`): eenmalig (`SpelerData.gezien "vr_w_uitkijk_z5_..."`), `RijkScene` zonder klikken (~180-200 ticks), titel +
  ondertitel, chime + het bestaande "uitkijk gevonden"-bericht. Camerapunten relatief aan de kaartplekken, in de open lucht.

### 4. Bovenverdiepingen (`tools/vadsrijk/zone5/bouw5.py`, `bovenverdieping`, gemarkeerd `[T-WOLKWAL]`)
- Herberg, bank, winkel_smid, winkel_bakker, winkel_wapens, winkel_kruiden (de eerste die passen uit `BOVEN_GEWENST`;
  winkel_kleding is te klein): eerste verdieping hol, plankenvloer, echte ramen, hanglamp, meubels per rol (herberg: gastenbedden
  + kist; bank: kluis met goud/kisten + lessenaar; winkel: voorraad, bed van de winkelier, werkbank).
- Trap: rechte houten trap achter de toonbank, "A" langs de achtermuur (breed genoeg) of "B" langs de zijmuur (diep genoeg; de
  herberg). De laatste trede ligt IN de verdiepingsband, dus gelijk met de vloer boven (geen sprong); trapgat met hekjes.
- Plekken `<huis>_boven` (het bordes) in Z5WereldPlekken (gegenereerd). `WolkwalRegistry.BOVEN` moet gelijk blijven met wat bouw5 kiest
  (de log van maak5 zegt het: "bovenverdieping (T-WOLKWAL): ...").
- Loopcheck: alle gepubliceerde plekken bereikbaar en terug (ook de 6 `_boven`), LEEG 671 / TRAP 472 / TRAP- 355 / VAL 496 = zelfde
  als de basis van t-z5wereld (die zijn allemaal walvis-randen/zitbanken/dakranden, zie NOTITIES_T_Z5WERELD).

### 5. Dev-commando's, teksten, tests
- `/guhs vadsrijk wolkwal ridders | beweging [aan|uit] | spuit | kijk <vin|staart|spuitgat|kop> | uitzicht <ooggaarde|staartpark|reset> | boven <huis>`.
- Teksten via `tools/features/vadsrijk_t_wolkwal.py` (NL + EN): de 2 panoramatitels + ondertitels.
- Gametests `WolkwalGameTests` (batch `vadsrijk_wolkwal`): riddermodel (kernbotten + alle botten van guh idle/walk, texturen,
  Sparringridder-botten), wie is Wolkwacht + vachten, ridder-prop, bewegende walvis (posities, uit de buurt van de platforms,
  spuittimer), panorama's (keyframes, duur, teksten, trigger), bovenverdiepingen (plek bestaat, 1 verdieping boven de begane grond).

## Testresultaat
- `compileJava` groen; `runGameTestServer -Pgt=WolkwalGameTests,Z5WereldGameTests`: **All 11 required tests passed** (6 nieuw + 5 van
  t-z5wereld). Loopcheck zone 5: alle plekken bereikbaar. Geen AutoCheck-client gedraaid.

## Zelf gekozen (de gebruiker sliep)
- Ridder = viervoetige guh (zoals alle guhs) met harnas, geen rechtopstaande ridder; schild/zwaard aan de flanken (guhs hebben geen
  armen). Alleen de Sparringridder heeft pootjes (hun animaties zwaaien met armen).
- NPC-herkenning op kleding (helm + wolkenkraag) i.p.v. een nieuw gesynct veld: geen gedeelde entity-wijziging nodig.
- Bewegende vinnen als props (Z3Figurant) i.p.v. een BlockEntityRenderer: geen nieuwe blokken/entitytypes, LOD = spawnen/opruimen.
- Bank via trap "A" (8 breed), herberg via "B" (6 breed, 12 diep). 4 winkels i.p.v. "3-4".
- Geluid van de spuitgaten = FIRE_EXTINGUISH laag (geen eigen geluid gemaakt).

## Gedeelde bestanden (add-only, gemarkeerd `T-WOLKWAL`)
- `npc/client/NpcClient.java`: de NPC-renderer is nu `WolkwachtGuhRenderer::new` (subklasse van GuhRenderer; gedraagt zich gelijk
  voor alle niet-Wolkwacht-NPC's).
- `zone5/wereld/Z5WereldRegistry.java`: 1 regel `WolkwalRegistry.register()`.
- `zone5/wereld/Z5Bewoners.java`: ridders via `Wolkwacht.prop`.
- `tools/vadsrijk/zone5/bouw5.py`: `bovenverdieping` + aanroep in `huizen()` + log.
- `tools/features/__init__.py`: 1 regel `vadsrijk_t_wolkwal`. lang nl_nl/en_us: 4 sleutels (toevoegingen).

## Voor de integratie
- Na merge met t-z5wezens: `python tools/vadsrijk/wolkwal/maak_modellen.py --sparring-over` (en committen).
- Na merge met t-z5verhaal: niets (de `z5_wacht_*` dragen KNIGHT_HELMET + WOLKENKRAAG -> ridder). Als zij hun kleding veranderen:
  `Wolkwacht.isWolkwacht` aanpassen.
- `maak_kaart --zone 5` opnieuw draaien (bovenverdiepingen + `_boven`-plekken zitten in de gegenereerde tegels/Z5WereldPlekken).
- Opgemerkt (niet van mij, niet aangepast): `z5w_noordvin` (2610, 244, -392) ligt op y 244, de noordvin zelf ligt op y ~140; het
  noordelijke wolkplatform staat dus niet op de vin maar hoog op iets anders (de zuidvin klopt: y 154). Voor t-z5wereld/integratie.

## Hervatpunt
Klaar. Regen: `python tools/vadsrijk/wolkwal/maak_modellen.py` (modellen) en `python tools/vadsrijk/maak_kaart.py --zone 5` +
`python tools/vadsrijk/zone5/loopcheck5.py` (kaart). Open: het uiterlijk (ridder, vinpunten, camerapaden) is alleen in renders/
gametests gecontroleerd, niet in een client: de gebruiker beoordeelt het in de dev-client.
