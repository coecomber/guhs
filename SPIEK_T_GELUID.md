# SPIEK T-GELUID: muziek + omgevingsgeluid in het Vadsrijk

Besluit R3 was "muziek ja, zacht". Dit blok heeft vier delen:
- **zachte gebiedsmuziek**, met lange stiltes ertussen;
- **een thema per baas**;
- **stingers** (korte ta-daa's);
- **een rijkere omgevingsgeluid-laag**, zonder harde wind.

De muziek is gemaakt met een eigen pijplijn: MIDI → SoundFont (GeneralUser GS) → OGG. De instrumenten zijn speeldoos, celesta, harp, fluit, marimba, kalimba, zachte strijkers en koor. Alleen de bazen klinken episch.

> **Smaak gaat voor.** Vind je een stuk niet mooi? Zeg dan welk stuk en wat eraan schort, bijvoorbeeld "te druk", "te somber", "ander instrument" of "trager". Elk stuk is één regel data in `tools/vadsrijk/geluid/stukken.py` en is in ~10 s opnieuw gemaakt.

## Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van ALLE tracks (klik = luisteren) | `/guhs vadsrijk geluid` |
| Eén gebiedsthema | `/guhs vadsrijk geluid gebied klodderweide` |
| Eén baasthema | `/guhs vadsrijk geluid baas vahoegnir` |
| Eén stinger | `/guhs vadsrijk geluid stinger baas_verslagen` |
| Eén omgevingsbed (12 s) | `/guhs vadsrijk geluid bed krekels` |
| Alles stil | `/guhs vadsrijk geluid stop` |
| De gebiedsmuziek hier NU starten (sla de stilte over) | `/guhs vadsrijk geluid nu` |
| Waar ben ik, wat hoort hier, hoe staan de schuifjes | `/guhs vadsrijk geluid info` |

Je kunt de OGG's ook buiten het spel afspelen. Ze staan in `src/main/resources/assets/guhs/sounds/vadsrijk/muziek/{gebied,baas,stinger}/` en `.../vadsrijk/sfeer/`.

## Schuifjes
Ga naar **Mods → Guhs → Config** (het bestand `guhs-vadsrijk-geluid.toml`). Elke waarde is een schuif van 0-100.
- **gebiedsmuziek:** standaard 70.
- **bazenmuziek:** standaard 85.
- **stingers:** standaard 80.
- **omgeving:** standaard 70.
- **pauze_min / pauze_max:** de stilte tussen twee gebiedsstukken. Standaard 60 / 180 s.

De muziek volgt OOK de schuif **Muziek** van Minecraft, en het omgevingsgeluid volgt **Omgeving**. Zet je een schuif op 0, dan staat dat deel uit. Staat gebiedsmuziek op 0, dan speelt in de wildernis weer de gewone Minecraft-muziek.

Testen:
1. Zet gebiedsmuziek op 0. → Na ~5 s stopt het stuk.
2. Zet hem terug op 70. → Daarna start `/guhs vadsrijk geluid nu` hem weer.

## 1. Gebiedsmuziek (zones 1-3 + Lampionkust + Hoogvads)
Zo werkt het:
- Je loopt een gebied in. Na 5-15 s komt het thema zacht op (~1-1,5 min).
- Daarna is het **1-3 min stil**, met alleen omgevingsgeluid. Dan speelt het thema opnieuw.
- Loop je een ander gebied in, dan vervaagt het oude stuk binnen ~9 s. Kort daarna komt het nieuwe.
- In steden speelt de bestaande stadsmuziek (die blijft zoals hij was).
- Tijdens een baasgevecht, scène of cutscene speelt er geen gebiedsmuziek.

| Gebied | Commando | Karakter |
|---|---|---|
| Klodderweide (+ zone-1-rand) | `/guhs vadsrijk geluid gebied klodderweide` | vrolijk pastoraal: fluit, marimba, harp, pizzicato |
| Sporenwoud | `... gebied sporenwoud` | lydisch toverbos-walsje: ocarina, celesta, speeldoos, glaspad |
| Druipgrotten | `... gebied druipgrotten` | rustig mineur: kalimba met echo, druppelklanken |
| Zwamtempel | `... gebied zwamtempel` | plechtig dorisch: panfluit, koor-oehs, harp, buisklok |
| Knuffelmeer | `... gebied knuffelmeer` | dromerig meer-walsje: klarinet, vibrafoon, harp |
| Galmhellingen (+ zone-2-rand) | `... gebied galmhellingen` | blokfluit met ECHO (galm!), glockenspiel, hoorn |
| Splinterkloof | `... gebied splinterkloof` | 6/8 avontuur in mineur: hobo, viool, pizzicato-ostinato |
| Spiegelburcht | `... gebied spiegelburcht` | spiegel-wals: celesta met glockenspiel-echo, klavecimbel |
| Wortelvelden (+ zone-3-rand) | `... gebied wortelvelden` | mixolydisch: ocarina, gitaar-arpeggio, marimba, shaker |
| Wortelduister | `... gebied wortelduister` | donker maar lief: kalimba met echo, lage strijkers |
| Het Hart | `... gebied hart` | heilig en traag: celesta, harp, koor |
| Schorsstad | `... gebied schorsstad` | huppelend: hakkebord, klarinet, fagot, accordeon |
| Stamspiraal | `... gebied stamspiraal` | opklimmend: panfluit, harp, pizzicato |
| Takkenbanen | `... gebied takkenbanen` | luchtig 6/8: fluit, piccolo, harp, glockenspiel |
| Kruinzee | `... gebied kruinzee` | breed en zwevend: viool, celesta, strijkers |
| Zeppelinwerf | `... gebied zeppelinwerf` | bedrijvig-grappig: klarinet, xylofoon, tuba, fagot |
| Lampionkust overdag | `... gebied lampionkust_dag` | eilandjes: steeldrum, ukelele, fluit, **met het Vadsrijk-leidmotief** in het middenstuk |
| Lampionkust 's avonds | `... gebied lampionkust_avond` | lampionfeest-wiegje: speeldoos, vibrafoon, **leidmotief** |
| Hoogvads | `... gebied hoogvads` | feestelijk-groots: hoorn, trompet, strijkers, koor, pauk, **leidmotief** |
| Hoogvads markt | `... gebied hoogvads_markt` | markt-wals: fluit, accordeon, pizzicato |

- **Lampionkust:** overdag speelt dag, vanaf de avond (tijd 11500-23500) avond.
- **Hoogvads:** de twee stukken wisselen elkaar af.
- **Zones 4 en 5:** die herkent het spel aan een gebied-id `z4_*` / `z5_*`, of anders aan de W-3-vakken (zone 4: x 1728..2559, z -768..-257; zone 5 erboven vanaf y 150).

**Kijk/luister:**
1. Ga naar de Klodderweide.
2. Wacht ~10 s. → Het stuk komt zacht op. Na afloop is het stil.
3. Vraag `/guhs vadsrijk geluid info`. → Je ziet "stilte, nog N s".
4. Loop naar Zwamzoet. → Het gebiedsstuk vervaagt en het stadsdeuntje neemt over.

## 2. Baasthema's
Het thema start zodra de baasbalk verschijnt en loopt naadloos rond. Gaat de balk weg, dan vervaagt het. Elke baas heeft een eigen thema:

| Zone | Baas → commando |
|---|---|
| 1 | Klodderreus `baas klodderreus` · Koning Klodder `baas koning_klodder` · Opperzwam `baas opperzwam` · Mika-spion `baas mika_spion` |
| 2 | Boormeester `baas boormeester` · Galmwolf `baas galmwolf` · Vrouwe Spiegelzucht `baas spiegelzucht` (ook haar spiegelbeeld) |
| 3 | Baron Bleu `baas baron_bleu` · Oude Nestwachter `baas nestwachter` · Wortelwurger `baas wortelwurger` · Graaf Piepvel `baas piepvel` · Kruinroofvogel `baas kruinroofvogel` · **Vahoegnir** `baas vahoegnir` · Bombardier Brokkel `baas brokkel` · Kapitein Roquefort `baas roquefort` · Takelmeester Taleggio `baas taleggio` |
| 4 | Grauwe Zeeslurf `baas zeeslurf` · de Vliegervanger `baas vliegervanger` |
| nieuw/onbekend | `baas algemeen` (vangnet voor elke baas zonder eigen thema) |

Elk commando begint met `/guhs vadsrijk geluid`.

**Echt testen:**
1. Vecht met een baas, bijvoorbeeld `/guhs vadsrijk baas reset` bij een arena.
2. Bij de balk → het eigen thema.
3. Win je → het thema vervaagt en de stinger **baas_verslagen** (koperfanfare) speelt.

Zone-4-bazen worden herkend aan een entity-id met `zeeslurf` / `vliegervanger` erin. Een baas kan ook zelf een geluid-id teruggeven via `muziek()`.

## 3. Stingers
| Stinger | Wanneer | Commando |
|---|---|---|
| ontdekking | de EERSTE keer dat je een gebied inloopt (niet de grote zone-randen) | `/guhs vadsrijk geluid stinger ontdekking` |
| quest_klaar | een quest ingeleverd | `... stinger quest_klaar` |
| level_op | level omhoog (één ta-daa, ook als generiek + klasse tegelijk stijgen) | `... stinger level_op` |
| baas_verslagen | een baas opgevrolijkt (iedereen die hielp) | `... stinger baas_verslagen` |
| geheim | een schat/geheim gevonden (de "schat"-titelkaart van zone 1-3) | `... stinger geheim` |

- Tijdens een stinger zakt de muziek even weg.
- Twee stingers tegelijk spelen na elkaar.
- Snel testen: `/guhs vadsrijk level <hoger dan nu>` (of `/guhs vadsrijk xp 5000`) → stinger level_op.

## 4. Omgevingsgeluid (de §5.3-pas)
Per gebied speelt een zacht "bed" (een lus van 22 s). Elke 5-12 s klinkt ergens om je heen een los geluidje.

| Bed | Waar | Wat je hoort |
|---|---|---|
| weide | Klodderweide, zone-1-rand, Hoogvads | vogeltjes, zacht bladgeritsel |
| bos | Wortelvelden, Schorsstad, Stamspiraal | meer vogels, af en toe een krakend takje |
| sporen | Sporenwoud | toverglinstering, zachte zoemtoon, een enkel vogeltje |
| grot | Druipgrotten + overal ondergronds | druppels met galm, diepe grotruis |
| tempel | Zwamtempel, Spiegelburcht, Het Hart | zachte zoemtoon, druppels |
| meer | Knuffelmeer | kabbelend water, vogels, een kikkertje |
| berg | Galmhellingen | belletjes (windgong), verre beek, vogeltje: **géén wind** |
| kloof | Splinterkloof | beekje (stroompje), druppels |
| wortel | Wortelduister | diepe druppels met lange galm |
| kruin | Takkenbanen, Kruinzee, Zeppelinwerf | bladeren + vogels in de kruin |
| honing | Honingtak (bovenop het stadsbed) | bijengezoem |
| kust | Lampionkust (zone 4) | golven, zeemeeuwen, windgong |
| krekels | 's nachts (tijd 13000-23000), buiten | krekels, uiltje, zachte windgong |

- 's Nachts maken de vogelbedden plaats voor de krekels. Bij water (meer, kloof, kust) komen de krekels erbovenop.
- De oude Z2-wind (Balto-wind op de Galmhellingen, het Knuffelmeer en de Splinterkloof) is vervangen door een zacht belletje.

**Luister:**
1. Speel `/guhs vadsrijk geluid bed kust` (12 s), daarna `bed krekels` en `bed honing`.
2. Ga 's nachts op de Klodderweide staan (`/time set 18000`). → Je hoort krekels en een uiltje.
3. Ga een grot in. → Je hoort druppels.

## Bekend / nog niet
- De muziek is door een computer gecomponeerd (regels + zaadjes). Hij klinkt lief en netjes, maar niet als een echte componist. Het blijft smaakwerk: zeg welke stukken weg of anders moeten.
- Bazen hebben nog geen aparte fase-2-laag: het thema is één lus van ~40-70 s.
- Er zijn nog geen puntbronnen uit de POI-lijsten (watervallen, bijenkorven, smidse). Er is ook nog geen grot-echo op voetstappen.
- De Lampionkust en Hoogvads gebruiken nu één thema per zone (+ dag/avond, + markt). Een thema per eiland of wijk kan later.
