# NOTITIES T-Z4WERELD - zone 4 "De Lampionkust": de kaart

Nachtronde T (2026-11-05/06). Worktree `guhs_vadsrijk/t-z4wereld`, branch `110-t-z4wereld` (van dbf82899d).

## Wat er gebouwd is

### De kaartgenerator (`tools/vadsrijk/zone4/`)
- `kit/` = KOPIE van de goedgekeurde render-generators (`renders-lampionkust/_bron`: kust, haven, eilanden, bouwsels + de
  render-kit schemerkit/vrkit/erfkit/mijnkit/vibekit/mcrender/mcextra/stadkit/mountkit/mounts/kamp/export) + `indeling.py`
  (kopie van `guhs1100-avontuur/wereldindeling/_bron/indeling.py`). De repo is zo zelfstandig; alleen de asset-zips voor de
  blokvormen (opaak/hol maken) worden van buiten gelezen (`MineScapeStory/renders-lampionkust/_bron/jars` + `renders-erfenis`),
  net als zone 3. Alle aanpassingen in de kopie zijn gemarkeerd met `[zone 4 kaart]`:
  - `kust.py`: indeling.py naast het bestand; de zee kreeg geen rotslaag (lucht onder een bodem van 4 dik) -> nu massief tot y 0;
  - `haven.py`: de paalwoningen op de noordarm op kadeniveau (waren tot 13 hoog boven de kade, onbereikbaar); de lampionsnoeren
    boven de steigers 2 hoger (de lampions hingen op hoofdhoogte);
  - `bouwsels.py`: `HUIZEN`-register (voor POI's, interieurs, trappen); lampionsnoer = echte blokken (`touw_blokken`);
    vliegers = echte blokken (`vlieger_blokken`) + `VLIEGERS`-lijst.
- `plan4.py`: raster (tx 27..39, tz -12..-5), gebieden (ellipsen per eiland), mobzones, Kringstroom/mist-parameters, ALLE POI's.
- `bouw4.py`: het spel-deel bovenop de kit (zie hieronder).
- `maak4.py`: tegels 64x320x64 (5 werkers, ~2,5 min), daarna POI's snappen op de echte blokken, `kaart_z4.png`.
- `poi4.py`: `Z4WereldPlekken.java` + `POI_Z4.md` (gegenereerd, NIET committen).
- Checkers: `loopcheck4.py` (loopcheck over de hele zone, zaad = Havenplein), `poicheck4.py` (sta-baarheid + snap),
  `audit4.py` (de zone-1 audit over alleen de zone-4 tegels; schrijft AUDIT_Z4.md + `_cache/veilige_plekken_z4.json`, raakt
  de gedeelde `veilige_plekken.json` NIET), `kijk4.py` (debug: doorsnede printen).
- Hook: `maak_kaart.py --zone 4` en `--zone alle` (eigen proces, zoals zone 3).

### Spel-deel (`bouw4.py`)
- Grond onder y 30: het venster begint op y 0 en de kolommen zijn massief (hol gemaakt: alleen wat je kunt zien blijft).
- Huizen (43): interieur schoon uitgehold, echte vloer, hangende lampion, meubels per soort (toonbank + NPC-plek erachter voor
  winkels/bank/havenmeester/botenverhuur/visrestaurant/opvangcentrum; bed/tafel/ton voor woningen/herberg; werkbank/weefgetouw/
  smoker in de werkplaatsen; hooi in de stal). Trap van elke deur naar beneden als de grond > 1 lager ligt.
- Lampionhaven: doorlopende loopplanken langs de noord- en zuidarm, steegjes tussen paalwoningen gevuld, ladders aan de 3
  steigers, een wei bij de stal, het questbord op het plein.
- Zeilbootsteigers op elk eiland (`boot_dok_*`, met ladder uit het water), en bij het Wrak en de Schildpad.
- Grote vuurtoren: deur + wenteltrap (kwartstrappen om een kern) naar de galerij (`uitkijk_vuurtoren`).
- Koraalbaai: rond vlot in de lagune = arena Grauwe Zeeslurf, drijvend looppad vanaf de duiksteiger, 3 onderwatergrotjes met
  een schacht + bellenkolom (soul sand) naar boven.
- Het Wrak van de Vliegervanger: echte kleine kerker (romp met 3 kamers onder dek, luik + trappen, baas op het voordek met de
  grauwe oorlogsvlieger), steiger met ladder, oprit over de rots.
- Kampvuren: 9 (elk eiland + Lampionhaven), unlit = kern-checkpoint. Alle kit-kampvuren ook unlit.
- Geheimen: 5 knuffels + 4 kisten.
- Paden van de steiger naar het hart van steile eilanden (vuurtoren, vlieger, pierkop, mysterie, koraal), 4-verbonden, 2 breed.

### Java (`feature/vadsrijk/zone4/wereld/`)
- `Z4WereldPlekken` (gegenereerd): ALLE, GEBIEDEN (ellipsen), MOBZONES, ARENAS, DOKKEN, KAMPVUREN, UITKIJKEN, SCHILDPAD +
  SCHILDPAD_ROUTE, SCHOORSTENEN, `stroming(x, z)`, `mist(x, z)`, `inZone4`, `gebied`, `mobzone`, `arena`.
- `Z4WereldRegistry`: kern-gebieden (vlakke dozen binnen de ellipsen, `voegVooraan`, eilanden eerst, `z4_zee` laatst), de
  Kringstroom (zwemmers en voertuigen zonder eigen stroming drijven zacht terug, melding 1x per 30 s), uitzichtpunten
  (panorama), biome per eiland (`biome(x, z)`), dev-commando's `/guhs vadsrijk z4wereld plek|eiland|dok|gebied|uitkijk|stroming|mist`.
- `Z4Stroming.EIGEN`: haak voor t-z4boot (het zeilbootje doet zijn eigen stroming; dan duw ik hem niet nog eens).
- `client/Z4WereldClient`: de Grauwe Mistbank (grijze mistkleur + zicht tot ~14 blokken in de kern, wolkjes), zacht in/uit.
- `Z4WereldGameTests` (batch `vadsrijk_z4wereld`): ids, randen (stroming/mist), kern-gebieden, biomen via de echte biome source.
- Data/teksten: `tools/features/vadsrijk_t_z4wereld.py` (12 biomen "Het Vadsrijk - <eiland>", gebiedsnamen, Kringstroom-tekst).

## Zelf gekozen (de gebruiker sliep)
- **Lampionsnoer-touw:** bestaande blokken: `guhs:vlaggetjes` voor snoeren met vlaggetjes, `guhs:vadstouw` (ketting) voor de
  rest. Geen nieuw blok.
- **Vliegers:** statische wol-vliegers (ruit, wit kruis, oortjes, oogjes) aan een vadstouw-lijn naar de grond. De posities
  staan in de kit (`bouwsels.VLIEGERS`) voor een latere entity-versie. Muurvliegers van de werkplaats zijn weggelaten.
- **De schildpad:** blijft een STILSTAAND eiland (fallback) op het beginpunt van de zwemronde; `SCHILDPAD_ROUTE` (24 punten,
  tegen de klok in) is er voor een bewegende entity (t-z4wezens/t-z4verhaal). Geen eigen entity gebouwd.
- **Mist:** client-fog (geen blokken), ellips om (2490, -320), kern 60 x 42.
- **Stroming:** start bij 93% van de Kringstroom-ellips OF 26 blokken van de tegelrand (de ellips steekt in het zuiden/oosten
  buiten de tegels, daar is leegte).
- **Gebouwen:** herberg, bank, winkel, kookhuis, havenmeester, botenverhuur en stal zijn huisjes uit de render die ik een rol
  gaf (zie `plan4.HUIS_NAMEN`). Visrestaurant "De Blije Bot" = eten.
- **Arena Zeeslurf:** het vlot op de lagune (niet het grotje uit de render: onder water vechten is niet fijn).
- **Wrak:** de render had alleen een scheef dek; ik bouwde een romp met 3 kamers. De render-versie (`eilanden.wrak`) wordt niet
  gebruikt, net als het bootje van de speler (`speler_boot`).
- Haven-ids: lijn C (zone 3 -> 4) = `haven_3_4_*` op de luchthaven van Lampionhaven, lijn D (4 -> 5) = `haven_4_5_*` op de
  Opstijgpier (`opstijgpier*` als extra namen).

## Gedeelde bestanden (add-only, gemarkeerd `T-Z4WERELD`)
- `wereld/WereldRegistry.java`: 1 regel `Z4WereldRegistry.register(modBus)`.
- `wereld/client/WereldClient.java`: 1 regel `Z4WereldClient.init(modBus)`.
- `biomen/VadsrijkBiomen.java`: 12 ids achteraan `IDS`; `index(x, z)` vraagt eerst `Z4WereldRegistry.biome` (VOOR zone 3:
  de marge van `Z3Lagen.inZone3` reikt tot x 1991, dus over Lampionhaven); `index(x, y, z)` slaat zone 3 over binnen zone 4.
- `tools/features/__init__.py`: 1 regel `vadsrijk_t_z4wereld` in FEATURES.
- `tools/vadsrijk/maak_kaart.py`: keuze `--zone 4`, `zone4(a)`, en `alle` bouwt zone 4 mee.
- lang nl_nl/en_us: via mijn generator (25 sleutels).

## Open punten / voor de integratie
- **Zone 5 deelt tx 38..39** (Hoogvads boven y 170): mijn tegels daar bevatten alleen zone 4 (onder y ~105). De naad-stap
  die beide combineert (WERELDINDELING §3) is NIET gebouwd: wie zone 5 bouwt moet die tegels samenvoegen (of zone 5 schrijft
  er bovenop met behoud van mijn blokken onder y 170).
- De zone-3 client-lucht (`Z3Lucht.sterkte`, marge 200) kan tot boven Lampionhaven kleuren; niet aangepast.
- `audit.py` (zone 1) laadt alle tegels met tz > -20: na de wereldindeling zitten daar ook zone 3 en 4 in. Ik gebruik
  `audit4.py`; de integratie moet audit.py zelf beperken.
- De veilige plekken van zone 4 (`_cache/veilige_plekken_z4.json`) zijn niet in de gedeelde json gezet.

## Checks (laatste run, 2026-11-06 ~01:30)
- `maak_kaart.py --zone 4` (via maak4, 4-5 werkers): 104 tegels, ~9,1 M blokken, ~25 MB, ~2,5 min.
- `loopcheck4.py`: **0 bevindingen** (geen VAL/TRAP/LEEG), alle 371 POI's bereikbaar vanaf het Havenplein (zwemmen telt).
- `poicheck4.py`: 0 van 371 niet sta-baar (35 plekken op de echte blokken gesnapt).
- `audit4.py` (AUDIT_Z4.md): 0 kuilen, 0 onder de kaart, 0 trappen, 0 deuren; over: 2 padstappen van 2 op het vuurtorenpad
  (2376,68,-499 en 2389,85,-495; 2 breed, je kunt eromheen), 59x "gras onder een dak" (dakranden/onder de luchthaven: geen
  interieurs), 10x diep water in de Koraalbaai (de onderwatergrotjes met bellenkolom + de diepste plekjes van de lagune, >10
  zwemslagen van de kant: bewust, het is de duikplek), de lampionmast op het plein als "boom op het pad".
- Gametests `-Pgt=Z4Wereld,VadsrijkBiomen`: **4/4 geslaagd** (Z4WereldGameTests plekkenEnRanden + biomen, VadsrijkBiomen).
- Geen AutoCheck-client gedraaid (RAM gedeeld met de andere nachtagents).

## Hervatpunt
- Code + generator gecommit (head zie git log). Na een checkout: `python tools/vadsrijk/maak_kaart.py --zone 4` (maakt tegels,
  Z4WereldPlekken.java, POI_Z4.md) -> dan compileert de Java (Z4WereldPlekken is gegenereerd en niet gecommit).
- Een vroege versie van POI_Z4.md + Z4WereldPlekken.java staat ook in `guhs_vadsrijk/` (voor t-z4wezens/t-z4verhaal/t-z4boot);
  de ids zijn stabiel, alleen y-waarden kunnen nog een paar blokken schuiven bij een nieuwe kaart.
