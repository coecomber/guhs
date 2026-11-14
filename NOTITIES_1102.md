# Guhs 1.10.2 (release-branch klaar, NIET gepusht)

## Samenvatting voor jou

**1. De grote Zwamtempeldeur ging niet open (hq11 "Klik op de grote deur").**
Oorzaak: de tempel heeft twee klokken die elkaar niet kenden. Als iemand de deur opent (ook gewoon door ernaartoe te lopen
op level 18+), schuift de bewegende wereld-deur na **30 seconden vanzelf weer dicht**. Maar de tempel zelf onthield 5 minuten
lang "de deur is open", en zolang er iemand binnen 12 blokken van de deur stond (jullie, bij Tempelwachter Steeltje) bleef
dat voor altijd zo. Een klik zei dan alleen nog "is al open" (de quest-stap telde wel), maar de deur bleef dicht. Met z'n
tweeën voor de deur wachten = altijd raak. Het level had er niets mee te maken (19 en 20 mag altijd).
Fix: een klik op de dichte deur opent hem weer (zonder het filmpje nog een keer), een speler die mag en ernaartoe loopt opent
hem ook weer, en een deur die al dicht is houdt het "open"-venster nooit meer vast. De quest-stap telt bij elke klik.

**2. De schildpad ten zuidoosten van zone 1.**
Daar ligt echt iets: het **Schildpadeiland** met het geheim van zone 1 (boom + kist op het schild, `geheim_schildpad`
524,76,722). Het eiland had een eigen "vrij gebied", maar dat raakte het vrije gebied van de kust maar in één punt. Ertussen
lag een strook water van 30-40 blokken waar de Zoete-Zeestroom je terugduwde, dus zwemmend of met de boot kwam je er nooit.
Fix: een extra vrij vaarwater tussen de kust en het eiland. De stroming op de echte rand van de kaart blijft.

**3. Terugkomen bij het portaal waar je wegging.**
Vroeger: elke keer dat je het Vadsrijk weer in ging, begon je op de steiger van Knuffelkamp. Nu: je komt terug bij het
portaal (of de plek) waar je het Vadsrijk verliet, naast het portaal en met je rug ernaartoe (zodat je er niet meteen weer
doorheen loopt). De eerste keer ooit begin je nog steeds op de steiger. Ligt je plek in een zone die nu dicht is, of is daar
geen vloer meer, dan kom je bij de aankomstplek van de hoogste open zone (zone 1 = De Haven). Ga je weg vanuit een dungeon,
dan onthouden we de plek voor de dungeondeur. Andersom (Vadsrijk uit) ging al goed: je komt terug bij het survival-portaal
waar je in ging; nieuw is dat je nu naast dat portaal uitkomt in plaats van erin.

**4. Balans: alle wezens en bazen +25 % leven en +20 % schade.**
Bovenop 1.10.1. Gewone wezens en elites: `[mobs] schade` 1,69 -> **2,028** en `[mobs] leven` 1,4 -> **1,75**. Alle bazen
(zone-, kerker- en raidbazen) en baas-hulpjes: nieuwe sectie **`[bazen]`** met `leven = 1.25` (max Vads) en `schade = 1.2`
(op de eindschade, na Verdediging). Niets telt dubbel: een baas krijgt nooit `[mobs]`, hulpjes slaan via `[mobs] schade`.
Het configbestand gaat bij de start vanzelf naar `versie = 3` (alleen waarden die nog op de oude standaard staan; met de hand
gezette blijven), met een regel in de log. **Servercheck na de uitrol:** `/guhs vadsrijk balans toon` ->
`[mobs] schade=2.03 leven=1.75 versie=3.00` en `[bazen] leven=1.25 schade=1.20`.

Niets gepusht. (mod_version is inmiddels 1.10.2, zie hieronder.)

---

(technische details hieronder)

## Technisch

Branch `release-1.10.2` in `guhs_vadsrijk/rel1102`, vanaf `c84327c74` (live v1.10.1). Commits:
- `b12923be5` Zwamtempeldeur
- `08a086780` Schildpadeiland
- `2ae22db25` Terugkeer bij het portaal

Geen nieuwe taalsleutels (engelscheck en taalcheck staan op 0). mod_version niet verhoogd.

### 1. Zwamtempeldeur (`bazen/opperzwam/Zwamtempel.java`, `wereld/WereldDeuren.java`)
- Deur: `deur_zwamtempel` min 324,152,-392 / max 336,175,-392 / voor 330,152,-388 (ongewijzigd, klopt met de tegels).
- Oorzaak: `WereldDeuren.OPEN_TICKS` = 30 s, terwijl `Zwamtempel.DEUR_OPEN_TICKS` = 5 min. `deurOpenTot` werd alleen
  gereset als niemand binnen 12 blokken stond. Klikken in dat venster gaf alleen `meldDeurOpen`, dus de deur ging niet open.
- Uitgesloten: (a) level (19/20 ≥ 18), (b) geen vanilla deuren/luiken/hekken in de deurdoos (tegels gescand),
  (d) de doos van `bijDeur` klopt, (e) geen klik-listener met hogere prioriteit (HIGHEST) die eerder annuleert.
- Fix: `WereldDeuren.gaatOpen(id)`, `Zwamtempel.deurStaatOpen()`, `heropen()` (geen tweede scene, wel melding +
  quest-stap); een speler die mag en binnen 6 blokken staat heropent de deur; een al dichte deur beëindigt het venster.
  `klikDeurIn(sl, p, pos)` en `deur(sl, nu)` zijn public voor de gametest.
- Andere levelpoorten vs 1.10.1-xp (sim met alleen hoofdquests): de deur (16 met hq10 = lv van hq10, 18 altijd = lv van
  hq11), luchtschip naar Kandijkom (20 = z2_hq01) en naar Schemerwortel (30) zijn consistent. Spelers die alleen
  hoofdquests doen komen 1-3 levels onder de lv-eis van veel hoofdquests uit (bv. hq10 14,9/16, hq11 16,4/18, zone 4-6 ~2
  levels). Dat zijn eisen om een quest aan te nemen; xpbalans rekent de grindtijd mee. Er is geen deur, baas of stap die een
  ACTIEVE quest op level blokkeert. Niet aangepast.

### 2. Schildpadeiland (`indeling/Barrieres.java`)
- Geheim: `geheim_schildpad` 524,76,722 (`NpcPlekken.SCHAT.schildpad`), eiland `schildpad_eiland` 520,76,720.
  Een zone-1-geheim, dus bedoeld om te bereiken.
- Vrije gebieden: `z1` (0,-20 715x660) en `z1_schildpad` (520,720 90x90) raakten elkaar alleen rond (520,630). Bij x=520
  lag er water tussen z≈588 en z≈630 dat "stroming" was (melding `vadsrijk.indeling.barriere.stroming`).
- Nieuw: `z1_schildpad_vaarweg` (470,640, half 90x60, p4-superellips) = water x≈380..560, z≈580..700.
  De open zee voorbij het eiland (bv. 680,720 en 520,880) en de Zoete-Zeestroom-straat (-380,760) blijven duwen.
- bereikcheck rekende vroeger geen barrières mee (zwemmen telde gewoon). Nieuw is de vlag `--barriere`:
  vóór de fix stond zone 1 op geheim 59/61 (schildpad + schildpad_eiland niet bereikt, dichtste cel 485,63,601), ná de fix
  op 61/61.
  Let op: met `--barriere` komen zones 4-6 op 0, omdat de luchtschippier z3→z4 (1622,167,-485) in dat model niet bereikt
  wordt (dichtste 11,3 blokken). Zonder barrières bereikt de check die pier via een route door een barrière. Dat moet
  onderzocht worden zodra zone 3/4 opengaan (zone 2+ is nu dicht). Daarom staat de vlag standaard uit.

### 3. Terugkeer (`wereld/Terugkeer.java`, `kern/VadsrijkCommando.java`)
- Vandaag: `naarBinnen` zette je altijd op `Gebieden.AANKOMST` (-384,65,1130, de steiger van Knuffelkamp).
  `terug` zette je precies op de survival-plek waar je in ging, dus midden in het portaal.
- Nu: `terug()` roept `Terugkeer.onthoud(p)` aan en bewaart `SpelerData.extra("wereld").uitgang` {x,y,z}.
  `naarBinnen` vanaf buiten (alleen als de kaart er is) gebruikt `Terugkeer.ingang`:
  - niets onthouden (eerste keer): de steiger;
  - zone dicht (`ZonePoort.dicht`), plek in een dungeon of buiten `Barrieres.VRIJ`, of geen vloer binnen 4 blokken:
    de aankomst van de hoogste open zone t/m de zone van de plek (1 `HAVEN_LANDING` 92,65,484; 2 `Z2 DOK_LANDING`;
    3 `Z3 DOK_LANDING`; 4 `Z4 LANDING`; 5/6 `AANKOMST`), via `WereldReizen.veiligeLanding`, anders de steiger;
  - anders: de dichtstbijzijnde vloer met 2 vrije blokken (geen vloeistof, geen portaal), minstens 2 blokken (horizontaal)
    van elk portaalblok, met je gezicht van het portaal af.
  - Uit een dungeon (`Dungeon.bevat`) wordt `DungeonDeur.hoofd(id).voor()` onthouden. Tijdens een scene, in de
    proeftuin of buiten de eilandzee wordt niets onthouden (de vorige goede plek blijft staan).
- Andersom: ligt de survival-plek in of naast een portaal (ons portaal of een netherportaal), dan kom je ernaast uit, met
  je gezicht ervan af (`Terugkeer.uitPortaal`).
- De inventariswissel (scheiding) is ongewijzigd: het zijn dezelfde teleports via `naarBinnen`/`terug` + `KernEvents.verzoen`.
  Dit geldt voor de vaste tempel, zelfgebouwde portalen en `/guhs vadsrijk`.

### Tests
- `VadsrijkBazenGevechtTests.tempeldeurGaatWeerOpen` (nieuw): een speler van level 18 met hq11. Ceremonie, de deur gaat
  vanzelf dicht, een klik opent hem weer en de hq11-stap telt; erheen lopen opent hem weer; een vastgelopen venster wordt
  opgeruimd. In de code van 1.10.1 faalt deze test na de klik (de deur blijft dicht).
- `IndelingGameTests.schildpadEilandBereikbaar` (nieuw).
- `VadsrijkTerugkeerGameTests` (nieuw, 3 tests): terug bij het portaal (ernaast, niet erin, ≥ 2 blokken, gezicht ervan af,
  ook de survival-kant), eerste keer, gesloten zone → De Haven, geen vloer → De Haven, dungeon → voor de deur.
  Draait in de Vadsrijk-dimensie als die er is, anders in het testlevel.
- Bestaande tests mee gedraaid: Indeling, Scheiding, YInt, Tempel, Werver, ZonePoort (+LootOpruimen), QuestRobot (zone 1),
  BazenGevecht, Escorte. Alle 47 groen; daarna Terugkeer nog eens apart: 4/4 groen.
- bereikcheck `--barriere --zone 1`: zone 1 hoofd 24/24, zij 40/40, npc 52/52, geheim 61/61.


---

## Afronding release 1.10.2 (2026-11-14)

**Op de server:** `/guhs vadsrijk zones open 3` (openZones = 3). Nieuwe serverconfig-waarde in `guhs-vadsrijk-zones.toml`:
`verkenningsboekProcent = 75` (0 = boek-eis uit). Patch notes voor Discord: `python tools/patchnote_discord.py 1.10.2 --open-zones 3`
(zonder `--open-zones 3` vallen de zone-2/3-blokken weg, de standaard is 1).

### Wat er bij kwam (na de eerste drie fixes)
5. **Zones 2 en 3 open**: wiki/site met `--open-zones 3` (pages.yml, build + zone1check). zone1check-vals-positieven opgelost met
   `poort.OOK_VANAF` (naam -> vanaf welke zone toegestaan): Spiegelzaal (2, ook de troonzaal van Spiegelzucht), Hoogvads en
   De Zwevende Stad (3, de zone-3-finale noemt ze), Sterretje/Lampionnetje (3, zitten in "Sterretjes"/"Lampionnetjes" van het
   rijdiertuig). 1.10.0-patchnotes: de Lampionkust/Hoogvads-plaatjes staan nu achter `:::zone 4`/`:::zone 5`, "Wordt vervolgd"
   (eind zone 1) achter `:::tot 2`. Zone-3-einde: "Wordt vervolgd" + de "vaart binnenkort"-regel bij Ordemeester Wasja (gever van
   z4_hq01) en nu ook bij `z3_kapitein` (`ZonePoort.VERTREK`).
6. **Per-speler zonepoort** (`ZonePoort.toegang`): naar zone N+1 = hoofdverhaal zone N (dezelfde check als luchtschiplijn N) EN
   Verkenningsboek zone N >= 75 %. Bovenop de servergrens. Opgeslagen als `extra.zonepoort.toegang`; wie er al was of al een
   quest van die zone had, houdt toegang. Terug kan altijd, ops/creative mogen altijd. Kapitein/reiskaart/luchtschipbord tonen
   "Naar X: ✔ hoofdverhaal · ✘ Verkenningsboek 62% / 75%", de questvolger "Volgende: verken X tot 75%", het boek een streepje
   bij 75 % ("Nodig voor de volgende zone"), en een melding "De weg naar X is open!". Gids-onderwerp "Naar de volgende zone".
   Questrobot telt het boek als gehaald (`ZonePoort.BOEK_VRIJ`). Let op: de reiskaarttekst noemt vast 75 %.
7. **Level-plafond per speler** (`ZonePoort.plafond`): top van de hoogste BETREDEN zone + 5: zone 1 -> **25**, 2 -> **35**,
   3 -> **55**, 4 -> **61**, 5 -> **65**, 6 -> 70 (geen plafond). "Betreden" = in die zone gestaan (vangnet-tik) of een quest van
   die zone afgerond; oude saves krijgen eenmalig krediet voor alle afgeronde quests. De xp-bank werkt zoals voorheen.
8. **Wakker bij het dichtstbijzijnde kampvuur** (`Kampvuren.dichtstbij`): van alle aangestoken kampvuren (Verkenningsboek-geheugen
   + checkpoint) het dichtste vanaf de plek waar je flauwviel, alleen in zones die voor jou open zijn en als het kampvuur er nog is.
   Arena's/raid houden hun eigen deur. Geen kampvuur = oude regel.
9. **Servercrash (Sfeer.java:56, 15:27 en 15:36)**: `TikWacht.alle()` (kopie zonder nulls) voor alle 17 `getAllEntities()`-lussen
   in heel Guhs; Sfeer gooit pas na de lus weg. Alle 131 server-tick-listeners van Guhs (74 lambda's + 57 method refs) lopen nu via
   `TikWacht.post/pre`: een fout wordt gelogd (eerste keer met stacktrace, daarna max 1x per minuut) en die feature slaat één tik
   over. Niet omgezet: ~13 `@SubscribeEvent`-tickhandlers buiten het Vadsrijk (safari, guhrio, piep, ...).
10. **Kristalklodder-spiegel** (`Weerkaatsen`): max 1 rol per spreuk (speler+skill+tik), nooit Liedpuls/plasjes/kettingbolt/
    status, max 15 % van je max Vads, gewone magieschade (Verdediging telt), geen `[mobs]`/`[bazen]`-vermenigvuldiger.
11. **Flauwval-log**: elke flauwte geeft een serverregel `[Vadsrijk] <naam> fainted at <dim> x,y,z <klasse> (max Vads): last hit
    <bron> skill=<id> amount=<n> vadsBefore=<n>` (max 1 per speler per 5 s).
12. **Vadsmagiër**: Vuurbal/Meteoorguh kritiek nu op kans (was altijd), Tijdsprongetje halveert de Vuurbal-wachttijd. **Set-harnas**:
    hoofdstat-groei 25 % -> 15 % per level voor alle harnas/sieraden van alle zones (`BasisType.HOOFD_GROEI`), ook bestaande stukken.
13. **Balans**: +25 % HP en +20 % schade blijft zoals gebouwd (`[mobs]` 2.028/1.75, `[bazen]` 1.25/1.2).

14. **Rijdieren**: de terugbreng-quests (Wolkenschaapje, Knuffelhert, Guhwolf) hebben de `@rijdier`-poort niet meer
    (`RijdierQuests.poortOpen` = altijd waar; geen teaser meer). Rijden (rijdiertoets, rijfluitje) pas na de zone-3-Pluimstal
    (`Rijdieren.magRijden` = starter gekozen of z3_hq03 klaar; ops/creative altijd). Eerder gekregen rijdieren blijven in de stal,
    met de melding "Je <rijdier> wacht op je! Rijden leer je in zone 3 bij de Pluimstal." (ook bovenaan de stal). Questlopen
    plaatst de rijdierquests nu na hun eigen voorwaarde (hq06).
15. Vooraf al kapotte tests gerepareerd: `VadsrijkLuchtschip.dichtUitleg` (draaide met servergrens 1, zet nu 6) en
    `VadsrijkVoltooiing.telling` (zone 1 heeft sinds 1.10.1 14 hoofdquests door hq03c_opruimen).

### Discord-tekst (na live, met `--open-zones 3`)

Zones 2 en 3 zijn nu open! ⛵
Het schip aan het eind van zone 1 vaart eindelijk. In **De Splinterpieken** (lv 20-30) wacht Kandijkom, een stad van snoepkleurig ijs. In **De Schemerwortel** (lv 30-50) woont een hele stad in een reuzenboom en ligt bovenin een oud nest... van iets héél groots. En aan het eind? Weer een schip. Dat vaart binnenkort. 😏

🧭 Naar de volgende zone? Speel het hoofdverhaal uit én verken minstens 75% van de zone (Verkenningsboek, toets B).
📈 Je max level is het level van de hoogste zone waar je bent geweest + 5.
💪 Wezens en bazen: +25% HP en +20% schade
🔥 Vuurbal en Meteoorguh van de Vadsmagiër waren altijd kritiek (bug): nu een kans zoals bij de andere klassen. Tijdsprongetje halveert de Vuurbal-wachttijd; setharnas groeit 15% per level (was 25%)
🦌 Rijdier-quests kun je nu al eerder doen; rijden zelf leer je in zone 3 bij de Pluimstal
💎 Kristalklodders spiegelen je spreuken niet meer dodelijk terug
🏕️ Flauwgevallen? Wakker bij het dichtstbijzijnde kampvuur
🚪 Zwamtempeldeur gaat weer open · 🐢 schildpadeiland bereikbaar · 🌀 terug bij je portaal
🛠️ Stabieler: geen servercrashes meer door opruimende vlindertjes

Alles op guhs.nl/vadsrijk. Njeg!
