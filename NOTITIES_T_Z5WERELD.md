# NOTITIES T-Z5WERELD - zone 5 "Hoogvads" op Moeder Wolkwal: de kaart

Nachtronde T (2026-11-05/06). Worktree `guhs_vadsrijk/t-z5wereld`, branch `110-t-z5wereld` (van abb8a027c = r-int).

## Wat er gebouwd is

### De kaartgenerator (`tools/vadsrijk/zone5/`)
- `kit/` = KOPIE van de goedgekeurde ronde-2-generators (`renders-hoogvads/_bron`: wolkwal, stad, kathedraal, wijken, huizen2,
  markt2, gebouwen, figuren, palet, bouwhulp + de render-kit schemerkit/vrkit/erfkit/mijnkit/vibekit/mcrender/mcextra/stadkit/
  mountkit/mounts + wereld/export). De asset-zips worden van buiten gelezen (`renders-hoogvads/_bron/jars`, net als zone 3/4); de
  Hoogvads-blokken zelf komen uit de repo (hoogkit zipt ze in `_cache/z5`). Elke aanpassing in de kopie heeft `[zone 5 kaart]`:
  - `wereld.py`: venster tot lokaal y 340 (spel 320) + cache in `tools/vadsrijk/_cache/z5`;
  - `palet.py`: "nieuw" = de echte mod-blokken (zonder `hv_`); niet-toegevoegde conceptblokken -> bestaande Guhs-blokken;
  - `kathedraal.py`: HOGER (schip 58, torens 58, galmgaten op +46, spitsen tot lokaal 336): **Grote Poort ~57 hoog** (was ~42);
  - `stad.py`: **terrassen van 1 blok** (was 4: overal muurtjes van 4 hoog zonder trappen, half de stad was onbereikbaar);
    het luchthavenvak wordt gereserveerd (er stonden huizen IN het station); watervallen eindigen boven zone 4;
  - `wijken.py`: het vaste luchtschip tussen pier 1 en 2 is weg (pier 2 = dok van het echte luchtschip, lijn D);
  - `hoogkit.py`: jar-paden + de repo-blokken.
- `plan5.py`: plaatsing (gx = 2702 - x, **gy = y - 20**, gz = -520 - z), wijken (gebieden/biomen), ALLE POI's (ook alle ids van
  t-z5verhaal en t-z5wezens), de wolkplatforms.
- `bouw5.py`: het spel-deel (zie hieronder).
- `maak5.py`: bouwt alles in 1 venster (~15 s), dan 88 tegels (tx 38..48, tz -12..-5), hol gemaakt, ~0,9 mln blokken, 2,5 MB, ~45 s.
  **Gedeelde tegels tx 38..39:** de blokken van een bestaande tegel ONDER y 118 (zone 4) blijven staan, zone 5 komt erboven
  (getest met t-z4wereld's t_38_-9: 49974 + 16227 blokken). Zone 5 moet dus NA zone 4 draaien (`maak_kaart --zone alle` doet 5 als laatste).
- `poi5.py`: snapt elke plek op de echte blokken, schrijft `Z5WereldPlekken.java` + `POI_Z5.md` + `_cache/plekken_z5.json`
  (gegenereerd, NIET gecommit; een kopie staat in `guhs_vadsrijk/POI_Z5.md` + `Z5WereldPlekken.java` voor de andere slices).
- Checkers: `loopcheck5.py` (de gedeelde loopcheck over heel zone 5, y 118..319, zaden = station + kathedraalplein, liften als
  links), `bereik5.py` (snelle 2,5D-bereikbaarheid op de lokale wereld: zegt WAAR het misgaat), `kijk5.py` (doorsnedes printen).
- Hook: `maak_kaart.py --zone 5` en `--zone alle` (eigen proces, als laatste).

### Spel-deel (`bouw5.py`)
- **Hoogte:** de walvis hangt 20 lager (buik y ~134, 31 boven de mast van de Opstijgpier y 103; zone 4 komt nergens onder de
  walvis boven y ~110). Stad y ~214..236, kathedraalvloer y 224, spitsen tot y ~316.
- **Huizen (51):** echte deur (kersenhout), begane grond hol met houten vloer, hanglamp, meubels; trap van de deur naar de straat
  (omhoog of omlaag). Geen luiken/lantaarns meer vlak voor deuren. Rollen: herberg "Het Hemelbed", bank, 8 winkels (kleding, smid,
  bakker, wapens, kruiden, juwelen, kaarten, speelgoed) met toonbank + plek erachter; de rest woningen (`HUIZEN` in de Java).
  Bovenverdiepingen blijven dicht (alleen de begane grond is te betreden).
- **Raadszaal:** hol, gouden deuropening oost, lessenaar op een podium, banken, zijkamer met deur (lessenaar met boek, kast, kist).
- **Paleis:** poort van de voorgevel naar de troonzaal, loper, lantaarns, gang + deur naar de **Kanselarij** (bureau, kasten,
  kaartentafel). De troon zelf is decor (de koningin-plek `troonzaal_troon`), `troonzaal_audientie` = ervoor.
- **Universiteit:** alle vleugels hol (begane grond), deuren naar de kloostergang; **bibliotheek** 2 verdiepingen hoog met kasten
  en leestafels; archief; collegezaal; **Gildehuis** in de oostvleugel (hal met deur naar het plein + 5 werkplaatsen: keuken,
  brouwerij, smidse, atelier, bijenstal); **sterrenwacht** met een wenteltrap tot boven. Gevels met diepte: steunberen tussen de
  ramen, vensterbanken + druiplijsten, bloembakken, 4 erkers.
- **Academie:** donjon-hal (kapitein, kazerne met bedden, kastjes), deur naar het oefenplein, oosttoren met ladder naar een
  uitkijkbalkon, de **Proefarena** (t-z5wezens, zand, hekring met 4 openingen). De greppel voor de poort is dicht, trap omhoog.
- **Kathedraal:** de gouden deur in de Grote Poort is een open doorgang (9 x 15), de buried trap van het plein is vrij, de
  zuidelijke geveltoren heeft een wenteltrap naar de klokkenvloer (uitkijk, balustrade in de galmgaten).
- **Station/luchthaven:** banken, havenmeesterbalie (bel, logboek), kratten met de losse plank, vertrekborden bij de 3 pieren,
  poorten door de stadsmuur op alle pieren; **pier 2** = lijn D: geen meertoren maar 2 meermasten, een hek met poortjes, een bel.
- **Rand/vallen:** stadsmuur + hekken op pieren; de watervallen hebben een rooster van kettingen in de muur; wie toch valt (pier,
  waterval, vin) wordt door de **Wolkvangnet** opgevangen (Java, zie onder).
- **t-z5wezens:** hun kamers in de walvis (Buikgangen, Schuilnest) zijn vrijgemaakt (lokaal = hun render-y; in het spel 20 lager),
  met ECHTE toegang naast hun teleport-luiken: ladderschacht van de Soepkeuken naar de Buikgangen, van de losse plank in het station
  naar het Schuilnest; ladderschachten van binnen de stadsmuur door de flank naar beide vinnen; een pad met hekjes over de
  staartsteel naar de staartvin (beide lobben). Wolkplatforms (vlak, geen gaten) op de vinnen en de staartvin (Donderbuik r 20).
- **Wolkwaldroom** (t-z5verhaal finale): ronde kamer r 17 in de walvis onder de kathedraal (alleen via hun teleport).
- **Huid:** de grote vlakke trapvlakken van de walvishuid buiten de stad hebben nu vlekken in 3 tinten, wolkpokjes-plukjes en
  zachte wolkbolletjes (beslissing 4).
- Kampvuren (unlit = kern-checkpoint) bij luchthaven, kathedraal, markt, paleis, academie, staartpark, ooggaarde.

### Blokken (`tools/features/vadsrijk_t_z5blokken.py`, Java `zone5/wereld/Z5Blokken`)
30 blockstates, de "moet"-set uit BLOKKEN_VOORSTEL: roze marmer (+ gepolijst, trap, plaat, muurtje, zuil, guhgezicht), wolkmarmer
(idem zonder gezicht), gouden kroonlijst (+ trap, plaat), gouden zuilkop, gouden dakpannen (+ trap, plaat), kathedraalglas
(guh/hartje/roos/ruitjes, met gouden lood), walvishuid (+ licht, buik, wolkpokje), Hoogvads-vaandel (boven/onder, plat doek zonder
botsing). Textures/modellen 1-op-1 uit `hv-concept.zip` (gekopieerd naar `textures/block/hoogvads/`). In de tab "Guhs: Bouwen"
(via `BouwBlokken.TAB_EXTRA`), te maken (shapeless + steenzaag), loot, tags. NL + EN namen.
Niet toegevoegd (palet gebruikt bestaande blokken): marmertegels, goudrand, dambord, filigraan, beeldmarmer, wolkensteen.

### Java (`feature/vadsrijk/zone5/wereld/`)
- `Z5WereldPlekken` (gegenereerd): ALLE, WIJKEN, HUIZEN, BEWONERS (612 + NPC's), UITKIJKEN, VANGNET, KAMPVUREN, `inZone5`, `wijk`.
- `Z5WereldRegistry`: kern-gebieden per wijk met een y-bereik (vanaf y 118: zone 4 eronder blijft zone 4), lv 56-60, stad = veilig
  (de walvis buiten de muur niet); biome per wijk (`biome(x,y,z)`); de **Wolkvangnet**; dev-commando's
  `/guhs vadsrijk z5wereld plek|huis|wijk|vangnet|bewoners`.
- `Z5Bewoners`: de figuren van de renders (marktgangers, vluchtelingen, studenten, ridders, kerkgangers) als passieve guh-props
  (`Z3Figurant`, geen hitbox, niet opgeslagen) binnen 56 blokken van een speler, max 160.
- `Z5WereldGameTests` (batch `vadsrijk_z5wereld`): blokken, alle gevraagde ids, wijken/biomen op hoogte, Wolkvangnet.
- Data/teksten: `tools/features/vadsrijk_t_z5wereld.py` (10 biomen "Het Vadsrijk - Hoogvads <wijk>" + "Moeder Wolkwal", gebiedsnamen,
  vangnet-tekst).

## Zelf gekozen (de gebruiker sliep)
- **20 lager: JA** (past boven zone 4 en de lijn-D-route; zie boven). Grote Poort ~57 i.p.v. 60: bij 60 komt de topgevel boven y 320.
- **Oortjes** op Moeder Wolkwal: gehouden.
- **Terrassen van 1 blok** in de woonwijken (de monumentale vlakken blijven plateaus met sierkeermuren): anders niet beloopbaar.
- **Alleen de begane grond** van de stadshuizen is hol (bovenverdiepingen dicht, ramen blijven ramen).
- **Wolkwacht-ridder-model: NIET gemaakt** (niet goedkoop genoeg); de ridders van de renders zijn guh-props met de
  `npc_wachterguh`-textuur. **Bewegende vinnen/staart: overgeslagen** (statische blokken).
- Uitzichtpunten staan in `UITKIJKEN` maar krijgen (nog) geen panorama-scène (zone 1/3 hebben dat wel).
- Pier-ids: lijn D = `haven_z5_z4_vertrek` (+ `haven_5_4_landing/bel/vertrekbord/kapitein/luchtschip/mast`) op pier 2, open kant
  = zuid (yaw 0); pier 3 heeft al een bord `haven_5_6_vertrekbord` voor lijn E (later).

## Gedeelde bestanden (add-only, gemarkeerd `T-Z5WERELD`)
- `wereld/WereldRegistry.java`: 1 regel `Z5WereldRegistry.register(modBus)`.
- `biomen/VadsrijkBiomen.java`: 10 ids achteraan `IDS`; `index(x, y, z)` vraagt EERST `Z5WereldRegistry.biome` (hoogte!).
  Conflict met t-z4wereld (die voegt ook ids toe en een check in index): beide houden, zone 5 eerst.
- `tools/features/__init__.py`: 2 regels (`vadsrijk_t_z5blokken`, `vadsrijk_t_z5wereld`).
- `tools/vadsrijk/maak_kaart.py`: keuze `--zone 5`, `zone5(a)`, `alle` bouwt zone 5 als laatste.
- lang nl_nl/en_us + minecraft-tags (stairs/slabs/walls/mineable): alleen toevoegingen via mijn generators.

## Voor de integratie
- Volgorde: zone 4 vóór zone 5 (gedeelde tegels tx 38..39). Na merge met t-z4wereld: `maak_kaart --zone alle` (of `--zone 4` dan `--zone 5`).
- t-luchtschip: `Lijnen.ZOEKERS.add(Z5WereldPlekken::plek)`; WALVISRUGHAVEN wordt dan `haven_z5_z4_vertrek` (2630, 217, -360), het
  schip dokt 9 zuidelijk (open lucht). De laatste via-punten van lijn D (y 236..262) moeten ~45 omlaag, naar y ~220 bij z ~-330.
- t-z5wezens/t-z5verhaal: alle ids staan in POI_Z5 (ook de genummerde: burger_*, wolkwacht_*, wolkbes_*, ...). Wezens: BUIK_Y/NEST_Y
  en hun y-bereiken 20 omlaag (of `z5w_buikgangen_min/max`, `z5w_schuilnest_min/max`).
- `audit.py`/veilige plekken (unstuck): zone 5 is niet in de gedeelde `veilige_plekken.json` gezet.

## Checks (eindstand)
- `loopcheck5.py`: **alle gepubliceerde plekken bereikbaar en terug** (behalve bewuste props: troon, luchtschip, mast, de
  teleportkamers). VAL-gebieden liggen allemaal ONDER de stad (y < 190: van de rand/vin vallen) = Wolkvangnet. TRAP ~470 = zitbanken
  (kathedraal, raadszaal, collegezaal, station) en dakranden; TRAP- ~355 = dakrandtrappen (daken zijn via erkers/steunberen te beklimmen).
- Gametests: `-Pgt=Z5WereldGameTests` 4/4 groen (blokken, gevraagde ids, wijken/biomen op hoogte, Wolkvangnet).

## Hervatpunt
Alles staat; regen = `python tools/vadsrijk/maak_kaart.py --zone 5` (~45 s) en daarna `python tools/vadsrijk/zone5/loopcheck5.py`.
Open: Wolkwacht-ridder-model, bewegende vinnen, panorama op de uitkijkpunten, betreedbare bovenverdiepingen, zone-5 veilige plekken.
