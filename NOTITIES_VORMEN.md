# NOTITIES_VORMEN — slice "vormen" van Guhs 1.9 "Een opknapper!"

Branch `190-opk-vormen`. Alles van deze slice: `feature/erfenis/vorm/**`, `data/guhs/erfenis/vormen/**`,
`tools/autocheck/erfenis_vormen.txt`, de exporter in `renders-erfenis/_bron/` (nieuwe bestanden) en één regel in `Features.java`.

## Wat er is
- **Exporter** (`renders-erfenis/_bron/export_vormen.py` + `export_extra.py`; deco/plukjes in `erfdeco.py`, voorbeeldvellen
  via `erfdeco_proef.py` -> `renders-erfenis/_deco_proef/`). Opnieuw maken (vanuit `_bron`):
  `python export_vormen.py <worktree>/src/main/resources/data/guhs/erfenis/vormen` (~5 s). De renderscripts zelf zijn niet veranderd.
  - Draait de echte bouwfuncties van de renders (hoeve.py, gebouwen.py, de opknap-families) voor elk level in **alle 5 paletten**
    en vergelijkt blok voor blok: gelijk in alle paletten = letterlijke blockstate, anders = paletrol (`@naam`, 5 states in
    `paletten.json`; naam = de STIJLGIDS-rol waar die herkenbaar is, bv. `@wand`, `@dak[facing=...]`, anders `@v12`). Daardoor
    klopt elk palet precies met wat de render-code tekent, ook waar de code op het palet vertakt (luikjes alleen T1 enz.).
  - Uitvoer: `index.json` (maten, levels, vorm-tier per level, deco-catalogus, paden, plukjes), `paletten.json`,
    `<id>/L<n>.json` (lokaal palet + RLE-volume y/z/x + wezens), `deco/<id>.json`, `plukje/<0..5>.json`, `pad.json`.
  - Wezens = markers: dieren (echte Guhs-mobs), `npc` (GuhNpcEntity-kind, zoals gerenderd: loketguh, bakkerguh, ...), `guh`
    (GuhEntity met variant uit de skin; mammoetguh 2,1x, zittende guhs). Tekstballonnetjes en de stippelrand: overgeslagen.
- **Java** `vorm.GebouwVormen`: precies de contract-API (stempel, wis, maat, vormTier, stempelPlukje, stempelDeco, decoMaat, pad)
  + `levels(id)`, `gebouwen()`, `decoIds()`, `decoInfo(id)`, `padSoorten()`, `bezig()`, `klaarNu()`.
  - Draaien: posities + generiek alle horizontale Direction-eigenschappen, assen, north/east/south/west (hekken, muurtjes,
    ranken) en `rotation` 0-15, dus ook guhs-blokken zonder eigen `rotate`. Daarna één verbind-ronde
    (`updateFromNeighbourShapes`) voor hekken/muurtjes/ruiten/trappen in de footprint.
  - Zetten zonder buur-updates (zoals de POC), alleen waar het verschilt; wachtrij: 12.000 blokwijzigingen per tick (dus bijna
    altijd meteen klaar), de rest de volgende ticks in volgorde.
  - `DecoWezens`: tag `guhs_erfenis_deco`, onkwetsbaar, `setPersistenceRequired`, geen drops, geen interactie (geen eitjes/wol/
    melk/quests), items die naast een deco-wezen ontstaan worden tegengehouden; home = midden van de footprint, wie erbuiten
    komt wordt teruggezet. Gewone guhs en NPC's staan stil (guhs zonder AI), dieren en mammoetguhs lopen rond.
- **Dev-commando's** (op): `/guhs erfenis vorm <id> <level> <palet 0=eigen> <rot>`, `vorm alle <level> <palet>`,
  `vorm plukje <0-5>`, `vorm deco <id> <rot>`, `vorm decoalle`, `vorm pad <soort>`, `vorm wis <b> <d>`.
  Alles komt ten zuidoosten van je voeten op het blok waar je op staat.
- **Gametests** `VormenGameTests` (batch `erfenis_vormen`): alle vormen/deco/plukjes/paden geldig en bekend; elk gebouw x elk level
  x 5 paletten x 4 rotaties echt gestempeld: niets buiten de footprint verandert, wezens binnen, wissen laat gras en geen wezens.

## Keuzes waar de documenten niets zeiden
- **Overhang weggeknipt.** De render-daken steken 1 blok buiten het perceel (dakranden, oortjes, luikjes) en de molenwieken tot
  4 blokken. Het contract wil "precies de footprint", dus alles buiten de footprint (boven de grond) valt weg: daken eindigen
  gelijk met de perceelrand en de molenwieken zijn op 7 breed afgeknipt. Wil de lead overhang (bv. 1 blok, alleen in lucht),
  dan kan de exporter die apart meegeven.
- **L3/L4/L7/L8** (niet gerenderd): vorm-tier volgens override 1 (L3/L4 = tier van L2, L7/L8 = tier van L6). Groei in de stijl van de
  scripts: L3 = L2 + 1 extraatje, L4 = de indeling van L5 in de vorm van L2, L7 = L6 + 1 extraatje, L8 = de indeling van L9 in de
  vorm van L6. Valt die vorm samen met de volgende (T4-gebouwen), dan: vorige + 2 extraatjes. Extraatjes = lantaarntje, bankje,
  hooibalen, ton, bloempot op vrije grasplekjes (nooit naast een deur/poortje).
- Kippenhok/schaapskooi/guhhutje volgen de vorm-rij van T1-gebouwen (zoals hun renders: T1, T1, T3, T3, T5, T5); de Hoeve heeft
  alle 10 renders (tier = (level+1)/2).
- **Mammoetwei**: levels 1-3 = render L1/L5/L10, altijd vorm T5.
- Wezens worden met de chunk opgeslagen (geen verlies na een herstart); elke stempel haalt eerst de oude in de footprint weg,
  dus ze stapelen nooit. (Het contract zei "non-saving"; opslaan + opruimen bij stempelen is robuuster.)
- Deco heeft geen palet (vaste materialen van de tier waarin het vrijkomt). Hekjes 10-15 zijn 1x1 en sluiten aan op buren.
  Nr 12 is drie stukken (roze/mint/hemelsblauw). Paden 1-9 gaan via `pad(...)`, niet via `stempelDeco`.
- **Paden** (`pad.json`): recht = hoofdblok; hoekjes, kruisingen/T's en eindjes een eigen blok (automatische hoekjes).
  Stapsteentjes: om en om steen/gras op wereldpositie. Bloesemblaadjespad: grof zand met roze blaadjes (meer op hoeken/kruisingen,
  de kant van het pad op). Sterrenpad = glimtegels.

### Deco-ids (nr id maat)
10 wilgenhekje, 11 guhhekje_wit, 12 guhhekje_roze / guhhekje_mint / guhhekje_hemelsblauw, 13 mosmuurtje, 14 knuffelhaag,
15 vadshekje (allemaal 1x1, soort hek); 16 boogpoortje 3x1, 17 bloembakje, 18 tulpguhsperkje 2x2, 19 zonnebloemrij 1x3,
20 kaasbloemenperk 2x2, 21 rozenboog 3x1, 22 guhbloesempje 2x2, 23 knabbelbessentobbe, 24 wolkenwilg 3x3, 25 boomstamkrukje,
26 guhbankje_hout 2x1, 27 guhbankje_roze 2x1, 28 picknicktafel 3x2, 29 schommelbank 3x2, 30 lantaarntje, 31 guhlantaarnpaal,
32 lichtjesslinger 3x1, 33 vadsstraatlantaarn, 34 kristalsterlamp, 35 drinkbak 2x1, 36 pompguh, 37 badeendjesvijver 3x3,
38 spuitguhfontein 3x3, 39 kaassausfonteintje 2x2, 40 kristalfontein 3x3, 41 kaboeter, 42 strooguh, 43 eerste_knabbel 2x2,
44 borstbeeld_makelaar 2x2, 45 gouden_guh 2x2, 46 mammoetje_heg 3x3, 47 tyrannoguhrus_beeld 3x3, 48 hooiguh,
49 hooibalenstapel 2x2, 50 kruiguhwagen 1x2, 51 vogelverschriknjeg, 52 melkbussen, 53 vogelhuisje_paal, 54 windvaantje,
55 hooiwagen 3x2, 56 trekguh 3x2, 57 brievenbus, 58 guhballon, 59 reuzengatenkaas 2x2, 60 hangmat 3x1, 61 mikaschrik,
62 roestige_weerhaan, 63 oude_melkkar 2x1, 64 kaartjesbord 1x2, 65 knabbeltrommel, 66 burenhulp_bloempot, 67 bouwvakkersbankje 2x1.
Paden: zandpaadje, knarspad, stapsteentjes, klinkguhpad, pootjespad, kinderkopjes, bloesemblaadjespad, vadsgoudpad, sterrenpad.
Plukjes: 0 struik, 1 omgevallen boom, 2 rotsblok, 3 brandnetels, 4 oude kar, 5 molshoopjes.

## Gestubd
Niets: deze slice roept geen API's van andere slices aan. Kern vervangt de POC-`Vormen`/`Stempel` door `GebouwVormen`.

## Testen in de dev-client
- `/guhs erfenis vorm hoeve 10 0 0` (eigen palet), `/guhs erfenis vorm hoeve 10 3 1` (palet T3, kwartslag gedraaid).
- `/guhs erfenis vorm alle 6 0` zet alle 33 gebouwen op L6 op een rij naar het oosten.
- `/guhs erfenis vorm decoalle`, `/guhs erfenis vorm plukje 4`, `/guhs erfenis vorm pad bloesemblaadjespad`.
- AutoCheck: `gradlew runAutocheckClient -PautocheckScript=tools/autocheck/erfenis_vormen.txt`.
- Gametests: `gradlew runGameTestServer -Pgt=VormenGameTests`.

## PAUZE (2026-11-03, op verzoek van de gebruiker)
Waar ik ben:
- Klaar en gecommit: exporter + alle vormen/deco/plukjes/paden in resources, GebouwVormen/DecoWezens/Draai/VormBestand,
  dev-commando, gametests `VormenGameTests`: groen (3/3 geslaagd; 6460 echte stempels in 3,7 s, geen onbekende blokken).
- AutoCheck `tools/autocheck/erfenis_vormen.txt`: run 1 was zwart (camera zat onder de grond: de autocheck-wereld is niet
  superflat), het script is aangepast (alles op y=200 in de lucht + nachtzicht) en run 2 is net klaar (BUILD SUCCESSFUL), maar
  de screenshots in `run/screenshots/autocheck/vorm_*.png` heb ik nog NIET bekeken.
Wat er nog moet:
1. De screenshots van run 2 bekijken en vergelijken met `renders-erfenis/<naam>_L<n>.png`; verschillen met de renders oplossen
   (exporter of draaien/verbinden), daarna opnieuw exporteren + gametests (`-Pgt=VormenGameTests`).
2. Daarna het eindrapport aan de lead (overhang-keuze noemen: daken/molenwieken afgeknipt op de footprint).
