# Testen: Het Vadsrijk (Guhs 1.10): zone 1 t/m 6 + alle systemen (na ronde W)

Dit is de ENE testgids voor het hele Vadsrijk. Snel alles langs met commando's: `SPIEKBRIEF.md` (hoofdstuk 0 Questlopen,
hoofdstuk 1 al je feedback van ronde W, hoofdstuk 2 de nieuwe systemen, daarna B1-B8 uit ronde U). Wat af is en wat niet:
`EINDRAPPORT_RONDE_W.md`.

> **Ronde W (2026-11-09):** de testclient is nu **`guhs_vadsrijk\w-intun-client.bat`** (branch `110-w-int`). Nieuw: al je
> playtest-feedback verwerkt, alles lopend bereikbaar (bereik-check), alle geheimpjes vindbaar, dungeons met een echte deur en
> wachtrij, een Party-tab, nep-spelers en Engels voor alle Vadsrijk-teksten (je test alleen in het Nederlands). Hieronder eerst de
> testlijst van ronde W; de lijsten van ronde U en ouder gelden nog steeds.

## Nieuw in ronde W: de testlijst

Speel vanuit **`guhs_vadsrijk\w-intun-client.bat`** in een **nieuwe wereld**: de kaart is opnieuw gegenereerd (zone 1-6).
De commando's per punt staan in SPIEKBRIEF.md, hoofdstuk 1 (feedback) en 2 (systemen).

| # | Onderdeel | Test | Goed als |
|---|---|---|---|
| W1 | Huisdiertjes + steiger | SPIEK 1.1: `leven tp knuffelkamp`, `leven spawn wandelaar`, tp naar de kampsteiger. | Diertje ~2 blokken achter de guh; op de steiger maar een paar guhs. |
| W2 | Blobs | SPIEK 1.2: `models reus`, `models toon zone1`. | Vierkante slijmblokken met kapje + oortjes; de Klodderreus is weer het grijze slijmblok. |
| W3 | Blubs in de stad | SPIEK 1.2: loop door het Knuffelkamp. | De blauwe diertjes zijn Blubs (zelfde model als in de droom). |
| W4 | Muisjes | SPIEK 1.3: `leven status` in het kamp en in de Kandijkom. | Kamp ~2 muisjes, Kandijkom ~6. |
| W5 | Cutscenes | SPIEK 1.4: `z3npc f2`, `z3npc cliffhanger`, `z6verhaal ringeling`. | Eerst "De wereld wordt geladen..." als het nodig is; F2 vliegt naar de toren; F4 begint bij de toren, niet in lege lucht. |
| W6 | Luchtschepen | SPIEK 1.5: elke haven, in survival: oplopen, pierpunt klikken, van de punt af lopen zonder schip. | Schip strak tegen de open punt; instappen op 3 manieren; vangnet zet je terug op de pier. Lijn 0 en de Staarthaven werken. |
| W7 | Kootje + walvis | SPIEK 1.6: de ladder in de Wolkwaldroom; lopend van de Paleisheuvel naar het Staartpark. | Ladder brengt je naar het spuitgat; de witte kliftrap loopt door tot het gras. |
| W8 | Kussens | SPIEK 1.6: het kussen bij Roosje. | Zitten zonder door de vloer te zakken; opstaan op/naast het kussen. |
| W9 | Zonenamen | SPIEK 1.7: tp naar de kathedraal. | Echte namen, nooit een `vadsrijk.kern...`-sleutel. |
| W10 | Figuranten | SPIEK 1.8: kathedraal, Spiegelzaal. | Minder kerkgangers, iedereen kijkt naar het altaar / middenpad. |
| W11 | Duimdop, kathedraal, deuren | SPIEK 1.9 (lopend). | Rechtop de Gildehal in; geen zwevend muurtje boven de trap; nepdeuren zijn ijzer. |
| W12 | Website | SPIEK 1.10: guhs.nl/vadsrijk-preview/ | Geen nepspelers; begint altijd op "Toon tot zone 1" met het briefje; nieuwe klodder-renders. |
| W13 | Xp | SPIEK 1.11: `npc quest klaar z4_hq01_lampionkust`. | +8.522 xp. |
| W14 | Geheimpjes | SPIEK 1.12: de tp's in de tabel. | Alles lopend te pakken (Mosje op de bosgrond, sneeuwtrap bij de grotten, ...). |
| W15 | Dungeons | SPIEK 2.1: `dungeon deur zwamgrot tp`, rechtsklik de deur; `dungeon nep zwamgrot 3`. | Dichte deur met klopper; venster met buit/wachttijd; klaar-check van 30 s; solo/groep/wachtrij werken. |
| W16 | Party-tab | SPIEK 2.2: H → Party (met een tweede speler als het kan). | Uitnodigen, accepteren (toast + chat), kroon, Leider/Weg; de party blijft na een herstart. |
| W17 | Nep-spelers | SPIEK 2.2: `nepspeler spawn`, rekruteren, wachtrij vullen, uitzetten in Instellingen. | `[NPC]`-naambordje; volgt en helpt; vult de dungeon-rij na ~1 min; uit = weg voor jou. |
| W18 | Questlopen | Hoofdstuk 0: [Sla stap over] en [Naar veilige plek], ook midden in een gevecht. | Je blijft nooit vast in een arena. |

## Nieuw in ronde U (feedback ronde 4): de testlijst

Speel vanuit **`guhs_vadsrijk\v-int\run-client.bat`** in een **nieuwe wereld**: de kaart is opnieuw gegenereerd.
De commando's per punt staan in SPIEKBRIEF.md, hoofdstuk 8. Hieronder staat per onderdeel wat je test en wat goed is.

| # | Onderdeel | Test | Goed als |
|---|---|---|---|
| U1 | Vadsrijk-menu (H) | Druk H in het Vadsrijk en klik alle hoofdtabs af; Ctrl+Tab bladert. | Eén kader, even grote tabs, niets buiten beeld, GUI-schaal 3/4 ook netjes. |
| U2 | H buiten het Vadsrijk | In het Guheinde: H ingedrukt houden. | Het krachtenwiel (Vadskunst) komt, geen Vadsrijk-menu. Geen rode botsing in Besturing. |
| U3 | Toetsen in het pauzemenu | Druk Esc in het Vadsrijk. | Rechts het toetsenoverzicht, met H (menu), `;` (emotes), Alt+1/2/3 (riem). |
| U4 | Karakters | Nieuwe wereld: het welkomstscherm na het filmpje. Probeer daarna de ⚔-knop, de tab Ridders en shift+klik op een kampvuur. | Maken lukt de eerste keer; 3 vaste slots (Normaal / Knuffel / Eénvads), de modus is niet te wisselen. |
| U5 | Rustende Ridders-zaal | `/guhs vadsrijk karakter zaal tp` | De zaal staat er zonder `zaal bouw`. |
| U6 | Luchtschip | Klik op ballon, staart en oortjes; loop met WASD over het dek; probeer een dichte lijn met een nieuw karakter. | Je stapt in, loopt vrij rond tot de reling, en ziet uitleg over de quest + het level. |
| U7 | Overgangen | `/guhs vadsrijk indeling rand wolkenkloof_noord`, en vlieg rond. | Geen rechte kleur- of wolkenlijnen, geen onderwaterklif. |
| U8 | Hekken | Kijk bij de kampen, de Boomtrap, de vlaggetjes in de Splinterpieken, Hoogvads, de kastelen en de huizenwijk. | Overal doorlopende lijnen. |
| U9 | Dubbel | `npc tp z2_kapitein` / `z3bazen tp kruinroofvogel`, weg en terug, dan `/guhs vadsrijk dubbel scan`. | Steeds 1 Splinter en 1 Kruinroofvogel; de scan meldt 0. |
| U10 | Zichtbaarheid | `z3npc klaarzetten` + F5; vraag een tweede speler om te kijken. | Cape, handschoenen en sieraden zijn zichtbaar, ook voor anderen. |
| U11 | Deco | Kamp 's nachts, Honingtak, de wenteltrap, de arena-onderkant, het nest, het Schildpadeiland. | Zie SPIEKBRIEF 8.3, 8.10-8.13 en 8.16. |
| U12 | Specialisaties | `/guhs vadsrijk spec ...` (SPIEK_U_SPECS), spec kiezen op lv 17. Bekijk de Karakter-tab en de party-frames. | Er staat "Specialisatie: X"; respec gratis bij de klasse-NPC; de tooltip toont ▲/▼. |
| U13 | Party, emotes, HUD | `/guhs vadsrijk hud party nep`, `;` voor het emotewiel, Instellingen › HUD aanpassen. | Frames links, de poses zien er goed uit, en wat je versleept blijft bewaard. |
| U14 | Grotten | `/guhs vadsrijk grot tp <zwamgrot/kristalgrot/wortelgrot/koraalgrot/wolkengrot>` | Een ontdekkaart; de schat gaat pas open na de puzzel; de puzzel telt 1× in het boek en staat in de tab Puzzels. |
| U15 | Ruiters | `/guhs vadsrijk ruiters lijst` en de tp's. | Ze rijden soepel over de trappen, zonder dubbele ruiters. Stem de zadelhoogte en snelheid af op wat jij ziet. |
| U16 | Audio | `/guhs vadsrijk geluid fase <baas>`, `fanfare <baas>`, en loop door de stadjes en grotten. | De fase-overgang is hoorbaar, er klinkt een fanfare, en de wereldgeluiden zijn subtiel. |
| U17 | Ambachten T4/T5 | `/guhs vadsrijk ambacht tp z4_duik_koraalbaai`, de Knutselwinkel en het Opdrachtenbord. | Duiken werkt; T5 kan alleen in het Gilde; het Opdrachtenbord kan niet dupen. |
| U18 | Knuffelbond | Tab Knuffelbond, `/bond`, oprichten bij Zegeltje (Hoogvads), de Bondenpoort, een kasteel bouwen. | Het kasteel staat met verbonden hekken; de banier krijgt kleur; de kluis werkt. |
| U19 | Huizen | Kwartiermeester op een plein, een huis kopen, inrichten, de brievenbus. | Lantaarnpalen = guh-lantaarnpaal; de rechten kloppen; 1 huis per account. |
| U20 | Zone 5 | De kathedraal, universiteit, troonzaal, staartvin en Wolkengrotten. | Diepte, kroonluchters, licht; de 4 staartvinplekken zijn te belopen. |
| U21 | Vliegend tapijt | SPIEK_U_TAPIJT | Glijden werkt; de eerste rit toont alleen de tapijt-tip. |
| U22 | Zone 6 rondkijken | SPIEKBRIEF hoofdstuk 6: luchthaven → Siq → Saffraanpoort lopen in survival; Schatkamer van het plein af bekijken. | Geen sprongen > 1 blok, diepe Schatkamer-gevel met vrijstaande zuilen, reliëf in de kloof. |
| U23 | Zone-6-verhaal | Questlopen (hoofdstuk 0) of zelf: zone 5 af → zwemfilm → lijn E → "Land in zicht!" … tot Ringeling. | Alle films spelen, bakens gaan aan, Ringeling wordt opgevrolijkt en staat daarna als NPC in Saffraanpoort. |
| U24 | Raid | `/guhs vadsrijk z6raid tp wachtzaal`, `z6raid start 2` (met nep-leden), de 4 bazen. | Raid-frames rechts (verschuifbaar in HUD aanpassen), torendrone in de Wachtzaal, bazenmuziek met fases, kist per clear. |
| U25 | Rijdier-drops | `/guhs vadsrijk loot kansen`, `loot pech <bron> 15`, `loot rol <bron>`. | 10 % per worp; na 15 keer mis zeker. Sterrennacht-Bultguh krijgt ook de Bultguh erbij. |
| U26 | Questlopen | `/guhs vadsrijk questlopen` (of H › Instellingen › Questlopen). Reset hoofdlijn, dan Volgende… | Je loopt de hele hoofdlijn zone 1 → 6 door met echte gesprekken/films; bazen zelf of laten verslaan. |
| U27 | Grotkerkers | `/guhs vadsrijk grot run`, ga een grot in, versla de eindbaas. | Poort dicht tijdens de run, eindbaas + eigen kist, na 10 min reset (ongeopende kisten weg). |
| U28 | Stadsleven | `/guhs vadsrijk leven tp <stad>`, `leven speeltuin <stad>`, overdag en 's nachts. | Speeltuin met verbonden hekjes, terrassen, beestjes; 's nachts winkels dicht maar quest-NPC's op hun plek. |
| U29 | Ritme + weer | `/guhs vadsrijk ritme status`, een emote (`;`) bij een guh, het Pluizenblaadje. | Reacties + knuffelmeter, weer met charme, nachtbel. Werkt ook in Rozenkloof/Saffraanpoort. |
| U30 | Geroezemoes | Loop over de Marktlaan in Hoogvads en door de bazaar van Saffraanpoort. | Zacht guh-geroezemoes, schuif "geroezemoes" in Instellingen. Niet te aanwezig. |
| U31 | Opslag | Probeer het eerste extra knapzak-vak te kopen; kijk naar de andere vakken. | Eerst de VIP++-grap (eenmalig), dan de koop. Andere vakken: "Binnenkort". |
| U32 | Website | https://guhs.nl/vadsrijk-preview/ | Zone 6, raid, kansen, renders, tabellen, live posities. |

## Nieuw in de nachtronde T (kort)
- **De wereld hangt aan elkaar:** Knuffelkamp (westelijker) → zone 1 → zone 2 (noordoost) → zone 3 → zone 4 → zone 5 (boven zone 4).
  Tussen de zones ligt zee met zachte barrières (je wordt teruggeduwd). Reizen gaat met **luchtschepen op een dienstregeling**
  (lijnen A, B, C, D; vertrekborden bij elke haven).
- **Zone 4 "De Lampionkust"** (lv 50-56): eilandjes, je eigen **zeilbootje**, de Kringstroom, Ouwe Dobber de schildpad, vliegers,
  de Grauwe Zeeslurf en het Wrak van de Vliegervanger, het verhaal "De Grote Knuffeltij".
- **Zone 5 "Hoogvads"** (lv 56-60) op Moeder Wolkwal: de stad, de detective-hoofdlijn "Verraad in de stad", de Proefarena,
  de Buikgangen, de Wolkrand, het Schuilnest, en de finale De Grauwe Droom.
- **Levelplafond 60** (was 50), generiek én klasse.
- **Systemen:** karakterslots + spelmodi (Eénvads), opslag (knapzak/kluis/materiaaltas) + HUD, kledingkast + vleugels,
  rijdieren aankleden + 3 nieuwe rijdieren, itemisatie 2.0 (ring 2, armband, handschoenen, cape, Erfstukken, drankjesriem),
  9 ambachten, puzzels en geheimpjes, de geheime baas Mika de Imker, het nieuwe praatscherm, muziek en geluid, mooiere modellen,
  bazen-effecten (telegraphs op de grond, cast-balk).

## Starten
1. Dubbelklik `run-client.bat` in `guhs_vadsrijk\t-int` (NIET r-int, k-int, j-int, h-melee, g-int, f-int of w-poc: dat zijn de oude rondes).
2. Maak een **nieuwe** wereld (Creatief, cheats aan). De `saves`-map is leeg: oude werelden kennen de nieuwe kaart (en de nieuwe
   zone 2) niet.
3. Zet je **weergaveafstand** hoog (32 chunks, minstens 20): het Vadsrijk is met de hand gebouwd en kan dat makkelijk aan.
4. `/gamemode survival` als je wilt vechten (in creatief vallen de wezens je niet aan; bazen doen hun aanvallen wel, maar raken je niet).
5. Erin: `/guhs vadsrijk` (of in survival via het portaaltempeltje in de Guhmensie, na een praatje met Werver Wimpel). Nog een keer = terug naar huis. In het Vadsrijk heb je een eigen, lege rugzak.

Je komt aan op het lage steigertje aan de zuidkust van Knuffelkamp (nu op x -384, z 1130: het kamp schoof 384 blokken naar het westen). De houten trap loopt nu helemaal door, als een tunnel in de
klif, tot boven op het kampplateau.

## Toetsen (alleen in het Vadsrijk; Opties > Besturing, categorie Vadsrijk)
| toets | wat |
|---|---|
| Linkermuis | mep (ridder) of vasthouden = schieten. Elk wapen heeft nu een eigen **aanvalstempo** (dolk 0,7 s ... pluishamer 3,5 s); het vanilla-kruisje vult zich als je weer mag. Spamklikken doet niks meer. |
| R / V / G | vaardigheid 1 / 2 / 3 van je klasse |
| Z | je ultieme vaardigheid |
| **C** | **Knuffelsprong** (nieuw): ~20 blokken vooruit, ~5 hoog, elke 5 s, geen valschade. Elke klasse. 5e vakje op de HUD. |
| **U** | **Losknuffelen** (nieuw): vast? Sta 3 tellen stil en de knuffelwind zet je op het dichtstbijzijnde veilige plekje. Elke minuut, niet in een bazengevecht. Ook: `/guhs vadsrijk vast`. |
| K | je skillboom (nu 6 rijen: rij 4 vanaf level 20, rij 5 vanaf level 26), met tabs per klasse |
| **B** | **Verkenningsboek** (ronde 3): per zone wat je al gedaan hebt, in %. Nog een keer B = dicht. (Als snuffelhond is B blaffen; in het Vadsrijk wint het boek.) |
| **X** | **opstijgen / afstijgen** (nieuw; zonder rijdier: "Nog geen rijdier..."): je actieve rijdier. Icoontje rechts naast de vaardigheden op de HUD. |
| **N** | **rijdierenscherm** (nieuw): alle rijdieren, wat je nog niet hebt als silhouet, kleuren van de Pluimguh |
| **P** | **huisdierenscherm** (nieuw): "Verzameld x/y", kies wie er met je meeloopt. N, P en B hebben tabjes bovenaan: klik om te wisselen. |
| J | je questlog (hoofdverhaal bovenaan, "Nu: ..."; Volgen/Niet volgen voor zijquests; Gids met de elementendriehoek) |
| (geen toets) | questvolger aan/uit en lichtstraal aan/uit: knoppen in de questlog, of zelf een toets kiezen |
| Rechtsklik | NPC's, kampvuren, deuren, de scheepsbel, munten, sieraden, de draaispiegels in de Spiegelburcht |
| Rechtsklik op een scheepsbel | **de reiskaart** (nieuw): kies waar je heen vliegt (Knuffelkamp, Zwamzoet, Kandijkom, De Schemerwortel) |
| Sluipen + rechtsklik | op een speler: ruilen. Op de **scheepsbel in De Haven**: meteen de lijn naar Kandijkom |
| Sluipen tijdens een rondvlucht | de luchtschipvlucht overslaan (pas na de eerste keer) |
| F9 / F10 | (Guhs 1.9, buiten het Vadsrijk) Erfenis-bovenaanzicht / wereldkaart. Waren O/M; een oude O/M-toets wordt één keer vanzelf verhuisd. |
| **Y** | **Ambachten** (nachtronde): het Ambachtenboek (overzicht, recepten, bestellingen, ranglijst, plekken) |
| **I** | **Knapzak** (nachtronde): per karakter; tabjes voor de materiaaltas en (bij een bankier) de kluis |
| **O** | **Kledingkast** (nachtronde): vleugels, capes, hoofddingen, effecten, verf, transmog |
| **Alt+1 / Alt+2 / Alt+3** | **Drankjesriem** (nachtronde): drink meteen uit vak 1/2/3 (vakjes rechts naast de hotbar met A1/A2/A3). Je hotbar-vakje wisselt hierbij niet. |
| X op het water (zone 4) | je **zeilbootje** in plaats van een rijdier. W = zeil, S = remmen, A/D = sturen, Shift bij een steiger = aanmeren |

**Questvolger** (rechtsboven, onder de bazenbalk als die er is): wat, bij wie, waar, pijltje + afstand. Een roze lichtstraal
boven je doel (vastgepinde zijquest: lichtgeel). Werkt nu ook in zone 2 en wijst je de luchtschiplijn aan als je doel aan de
andere kant ligt.

## ZONE 3: De Schemerwortel (vernieuwd, lv 30-50)
Een boom zo groot als een berg op een zwevend wortel-eiland, ten noordoosten van Zwamzoet (de stam staat op x 1404, z -576).
De kaart is **helemaal nieuw** (revamp). Van boven naar beneden:
- **het nest** bovenop de boom (Vahoegnir), daaronder **3 kruinverdiepingen** (kruin 1 met de Kale Kruin, kruin 2, kruin 3);
- **de grote takken** (Westtak, Bijentak, Zuidwesttak, Molentak, Noordtak, Oosttak) met 3 imkerdorpjes: **Zoemrust** (Bijentak),
  **Raatweide** (Zuidwesttak) en **Molenhoek** (Molentak);
- **Honingtak**: een ringstadje om de stam (Bloesemplein, Imkerstraat), met 3 grote honingraatcellen eronder (café, Nestkapel,
  Raattrossen), elk met een liftje;
- **de holle stam**: de **binnentrap** (één wenteltrap van de vloer op y 80 tot in het nest), de **Schorsstad** (huisjes in de
  stamwand), de Lantaarntoren (7 ringen) en het **binnenhof** waar de twee stamliften samenkomen;
- **de Pelszaal** ligt nu ONDER de vloer van de Schorsstad (trap omlaag vanaf de vloer);
- **de grond** van het eiland: Mosvoet (gehuchtje bij de grote Honinglift), de Pluimstal (noord, voet van het rijpad), de
  Wortelgewelf-ruïne, 3 kampjes, de ringweg met Pluimposten;
- **eronder**: de Wortelpoort (oost), de Wortelgang omlaag naar de Hartslag-zaal en het **Hart van de Boom** (een ring kamers);
- **de Kaaszeppelinwerf** op een eigen eilandje in het noordoosten, over de **kabelbrug**;
- havens: de **Weststeiger** (lijn B uit zone 2, Galmdok) en de **Oosttaksteiger** (lijn C naar Lampionhaven, zone 4, later).

**Erheen (snel, met commando's)**
- `/guhs vadsrijk z3npc klaarzetten` = zone 2 klaar + level 30. Dan Kapitein Poolster (`/guhs vadsrijk npc tp z2_kapitein`) of een
  scheepsbel: kies **De Schemerwortel**. Op de reiskaart lopen de stippellijnen nu Kandijkom → Schemerwortel → zone 4 (lijnen B/C).
- Of direct: `/guhs vadsrijk z3wereld vlieg honingtak` (`... vlieg opnieuw` = de openingsfilm nog een keer).
- De eerste keer: de **openingsfilm** (~26 s): langs de schorswand omhoog, de gouden kruin, langs de stam omlaag de wortels in,
  twee ogen in het donker, en terug naar de Weststeiger. Kijk: zit de camera nergens in een tak of in de bladeren?

**De nieuwe route (hoofdverhaal)**: Raatje op het Bloesemplein (hq01) → Nestkapel in de cel onder het dek, dan met de binnenhoflift
en de noordpoort OMLAAG naar de Pluimstal (hq02) → Veertje rijdt voor je uit het **rijpad** op (hq03) en je zweeft van het
zweefplatform op kruin 1 → grauwe vlekken op kruin 1 en 2 (hq04) → het nest bovenop (hq05) → Raattrossen (hq06) → Imkerdorp
Zoemrust op de Bijentak (hq07) → de werf over de kabelbrug (hq08) → **De binnentrap**: door de trapdeur aan het Bloesemplein omlaag
naar Knoest (hq09) → Lantaarntoren + Pelszaal onder de vloer (hq10) → Mosvoet + ketels (hq11) → Wortelpoort en Wortelgang (hq12) →
het Hart (hq13) → ontsnappen over het rijpad en de windkolommen naar het nest (hq14) → cliffhanger vanaf het Bloesemplein (hq15).

**Het Rijpad** (de rijspiraal buitenom de stam; de gebiedsnaam was "De Stamspiraal"): 9 breed in halve stapjes, hek met lampions,
afslagen naar Honingtak, naar elke grote tak en naar kruin 1, balkonnetjes om vanaf te zweven. Begin bij de Pluimstal
(`/guhs vadsrijk z3wereld plek rijpad_start`), halverwege `rijpad_honingplein`, eind op kruin 1 `rijpad_eind`. Rij hem helemaal
op je Pluimguh: nergens springen nodig? Ruiters (3 Pluimguhs) rijden op en neer.

**De binnentrap** (in de holle stam): één wenteltrap 80 → 292 in halve stapjes, 8 breed, met een leuning. Bordessen:
`trap_bordes_1` (vloer 80) t/m `trap_bordes_6` (nest); deuren naar buiten op Honingtak (166) en elke kruin. Ook de stamliften:
`schorslift_1` (vloer ↔ binnenhof), `schorslift_2` (binnenhof ↔ kruin 1) en `honinglift_kruin` (Honingtak ↔ kruin 1).

**Windkolommen** (bloesemring onderaan, omhoog): `wind_honingtak_onder` (grond → Honingtak), `wind_werf_tak_onder` (Westtak → kruin 1),
`wind_hangnest_onder` (Bijentak → kruin 1), `wind_paddenbloesem_tak_onder` (Zuidwesttak → kruin 1), `wind_spiraal_kruin_onder`
(Molentak → kruin 1), `wind_mosvoet_kruin_onder` (kruin 1 → 2), `wind_lichtkever_kruin_onder` (kruin 2 → 3). Erheen:
`/guhs vadsrijk z3wereld plek <id>`. **Liften**: rechtsklik bij een liftstation; de grote Honinglift (grond ↔ Honingtak) pas na
hq09 (`/guhs vadsrijk z3wereld lift <id> op|neer`; ids: honinglift, honinglift_kruin, lift_cafe, lift_kapel, lift_raattros,
schorslift_1, schorslift_2).

**Bazen** (`/guhs vadsrijk z3bazen tp <naam>` of `spawn <baas>`; overzicht `/guhs vadsrijk z3bazen arenas`):
- **De Kruinroofvogel** (lv 38): de Kale Kruin, een dode mesa die uit kruin 1 steekt (zuidwest). In fase 3 vliegt hij om de boom
  heen: op je rijdier het roze spoor volgen.
- **De Kaaszeppelinwerf** (lv 38-40), eigen eiland: Taleggio (Dok 1, op de grond), Brokkel (Dok 2, steigertoren), Roquefort (Dok 3,
  masttoren, wrak eronder met een trap terug). Eén gevecht tegelijk, kerkervolgorde.
- **Graaf Piepvel** (lv 42): de **Pelszaal onder de Schorsstad-vloer**. Zijn **cape** = een lappendeken van muizenvelletjes met
  oortjes en staartjes; per opengebroken kooi gaat er een lap af. Daarna F1: grijze mist door het kwastgat, twee Mika-handlangers
  slepen hem mee (geen arm meer). Op de Schorsstad-vloer erboven zit je NIET in zijn arena.
- **Het Hart van de Boom** (lv 49-50): een ring kamers om de Hartslag-zaal, poort in de westwand; looprichting = vechtvolgorde
  (Wortelwurger, Nestwachter, Baron Bleu). Het ei ligt in de wortelmand midden in de zaal.
- **Vahoegnir** (lv 50, het nest bovenop): **nieuw uiterlijk**: majestueus en roze, met een guh-knuffelkop en grote ronde oren;
  hij kleurt in 6 stappen van grauw naar roze (vleugelpunten, vleugels, staart, lijf, kop) en de ogen gloeien paars tot het eind.
  In rust staan de vleugels in een V omhoog (kijk of dat goed staat). Dev: `/guhs vadsrijk z3bazen vahoegnir` (+ `waar`, `reset`).

**De roze draak na zone 3** (vanaf `zone3_klaar`): Vahoegnir **rust in het nest** (alleen zichtbaar voor wie zone 3 uit heeft).
Rechtsklik = aaien (hartjes, spinnen, buiging). Om hem heen komen tot 32 **beestjes**: vinkjes en meesjes (vliegen, landen, zitten
op zijn rug), eekhoorntjes, konijntjes, roze muisjes, vlindertjes. Elke 20-40 min komt hij ook op **bezoek** in zone 1/2 bij een
plein of kampvuur. Dev: `/guhs vadsrijk vriendjes draak nest` (zie je hem ook zonder zone3_klaar), `... draak bezoek`, `... draak timer`.

**Huisdieren met naambordje**: kijk naar je huisdier: "<huisdier> van <speler>" (alleen als je ernaar kijkt, weg met F1).
**Vahoegje** is nu een chibi-draakje (eigen model): `/guhs vadsrijk huisdier geef vahoegje`, dan P en "Meenemen".

**Praatscherm met portret** (q-dialoog): elk gesprek toont nu links een portret: jijzelf bij je eigen regels, de NPC (ook als die
ver weg is: een stand-in), een boekje bij de verteller. Dev: `/guhs vadsrijk npc portrettest`. Met F3+H staat onderin een grijs
regeltje met de bron van het portret.

**Premiebord**, **Vahoeg-modus**, **Verkenningsboek**: zoals eerder (Jagershut op Honingtak; altaren bij de werfingang en in de
Hartslag-zaal; B, pagina "De Schemerwortel", gebiedsnamen nu ook "Het Rijpad").

**Sfeer**: rook uit de schoorsteentjes, bijenzwermen bij de korven, 's avonds lampions en vuurvliegjes, laagjesmist per verdieping,
imkers in Raatweide/Molenhoek, reizigers bij de kampjes, guhs op de bruggen.

**Overige dev-commando's**: `/guhs vadsrijk z3npc opening|bam|nest|f1|ei|rit|finale|cliffhanger|vergeet`,
`/guhs vadsrijk z3wezens spawn <soort>|alle|licht|spawners`, `/guhs vadsrijk z3wereld gebied` (waar sta ik), `/guhs vadsrijk level 50`.

**Lantaarnregel** (Wortelduister/Hart): buiten het licht zijn wezens een silhouet en nemen ze -50 % schade.

**Niet gezien in een client** (juist kijken): de hele nieuwe kaart, het rijpad en de binnentrap op je rijdier/te voet, de
Pelszaal onder de vloer, de nieuwe draak en de rustende roze draak met beestjes, de opening en alle scènes op de nieuwe plek.

## Wat is nieuw (fixronde 3, derde playtest)
- **Buitenstaanders blijven buiten:** makelaar Guhrrit, de Postguh, de Sherlock-cast, guhs van thuis, volgers (Libelkonijnguh,
  Sponsguh, Gerrit) en Momo komen niet in het Vadsrijk. "Roep naar mij" en de Guhkamer-roep werken daar niet ("Je guhs blijven
  lekker thuis..."). Post (makelaarsbrief, kaartjes, Opa's kelderpost) wacht tot je weer thuis bent.
- **Huisdieren:** geen naambordje meer boven je huisdier.
- **Reiskaart:** een ✕ rechtsboven om hem te sluiten.
- **Tabjes:** Rijdieren (N), Huisdieren (P) en het Verkenningsboek (B) hebben tabjes boven het paneel; de oude "Huisdieren"-knop in
  het rijdierenscherm is weg. Niet bij de stalmeester.
- **Rijdieren:** de Hoedhupper en de Klodderros zijn weg. Rijdieren beginnen in zone 3 (zie hieronder bij Rijdieren). Nieuwe
  snelheden en sprongen, en elk rijdier stapt een randje van 2 blokken op.
- **Zone 2, hangbruggen:** alle 5 bruggen sluiten vlak aan op de grond, elke stap hooguit een half blok (ook de grote brug bij
  Kloofwachter Touwtje).
- **Zone 2, aankomst bij het dok:** het zwevende roze glas is weg. Een calciet-poortboog met kristallamp, een glooiende bergpas in
  plaats van rechte wanden, een weg met lampions, lichtsnoeren en vuurkorven, een bankje, een chocomelk-kiosk en een welkomstboog
  bij de pier. De noord-, oost- en westpoort kregen dezelfde boog en pas.
- **Muziek in de steden:** Zwamzoet, Knuffelkamp, De Haven en Kandijkom hebben elk twee lieve deuntjes (schuif Muziek) en eigen
  achtergrondgeluid (schuif Omgeving). Weg bij bazen en filmpjes; in de wildernis alleen sfeer. Uit: `stadsmuziek = false` in de
  sfeer-config.
- **Verkenningsboek (B):** per zone een pagina met % en categorieën (hoofdverhaal, zijmissies, uitkijkpunten, kampvuren, geheimen,
  verloren knuffels, bazen, huisdieren, gebieden). Hints voor wat je nog niet hebt; geheimen zijn "???". Bij 25/50/75 % een toast,
  bij 100 % een titel ("Kenner van Zwamzoet" / "Kenner van de Splinterpieken") en Vadsmunten. Rijdieren staan erin als
  "(later)": je ziet ze, maar ze tellen niet mee (die krijg je pas in zone 3). Dev: `/guhs vadsrijk boek scherm | tel | check | reset`.

## Wat is nieuw (fixronde 1)
**Kaart en lopen (survival)**
- Knuffelkamp: de aankomsttrap loopt door tot het plateau; overal ladders uit de zee (steiger, eilandkust, luchtschipsteiger);
  lantaarnpalen hebben weer een lamp; tenten met plankenvloer; vuurtoren: ladder uit de kelder én (nieuw) een ladder in de toren
  naar de omloop bovenin.
- De Haven: organische vorm, kade langs de kust, grote luchtschiptoren, aankomstplein met boog, kranen; ladders uit het water.
- Luchtschip Knuffelkamp <-> De Haven = een rondvlucht van ~32 s (zwaaiende guhs op een ander schip), overslaan met sluipen.
- Zwamzoet: geen gras meer op plein/paden/binnen; lantaarns naast de paden; wolkenlift boven met een nette uitstap; deur in de
  steel met een halletje; wenteltrappen komen boven op de hoed uit; bruggen in halve stapjes; wolkenpaden; vloeren per gebouw;
  marktwaren, banieren, tuintjes. Ladders uit de baai bij de kade (elke 10 blokken), de steigers en het vuurtoreneiland.
- Wildernis: bomen niet meer op paden; slijmheuvel sluit aan op de grond; de **drakenfossiel-brug ligt nu dwars over de Kloof**;
  nieuw bergpad naar de Zwamtempel met een volle trap; **5 naalden met een wenteltrap zonder springen**; hellingen uit de Kloof;
  kuilen gevuld; grotten met vlakke vloeren; mijnwerkerskampjes en sprookjeskringen langs de paden.
- **Hoedenpaden** (nieuw, Sporenwoud): een tweede laag 30-50 hoog over de reuzenzwammen: touwbruggen, uitkijkhutten, een
  elite-nest en een verstopte schatkamer (geheim `hoedenschat`). Drie wegen omhoog: Wenteltrapzwam, Wortelpad, Holle Steel.
- **Druipschacht** (nieuw, Druipgrotten): een grot van 80 hoog met een kristallen dakraam, meer met 3 watervallen en een
  wenteltrap langs de wand naar een balkon. Ingang via een tunnel vanaf grotknoop N4.

**Gevecht**
- Aanvalstempo als wapenstat; DPS ridder 100 / jager 70 / magiër 85; geen one-shots op wezens van je eigen level.
- Trainingspoppen tellen nu ook pijlen, magie en vaardigheden. Elementen zijn icoontjes (vlam, blad, druppel, hartje) voor de naam.
- Beleving: hit-stop en schermschud bij zware klappen (schuifje in Guhs > Config: `vadsrijkSchud`), geluid per wapen,
  schadegetallen (goud ★ = voltreffer, "Zwak!"/"Taai"), **Vrolijke reeks** (x10/x25/x50 = meer xp), fasemoment bij elke
  bazenfase (camera, letterbox, fasenaam), overwinning ("Opgevrolijkt!" + schokgolf), **vallende buitkist** met lichtzuil.
- Bazen vechten echt: kiezen zelf hun doelwit (ook wie van ver schiet), springen terug in de arena, raken je niet van buiten de
  arena ("Te ver weg!"), resetten na 30 s lege arena. Klodderreus sterker; Koning Klodder met kroon gooien en schild; Opperzwam
  met sporenlast/spreidsporen en een wortelentoren in fase 3; Mika-spion met lokpoppen en kaaswielen. De Zwamtempel gaat pas open
  op level 16 met quest hq10.

**Wereld en leven**
- Veel meer wezens (MMO-dicht): groepjes op en naast de paden, patrouilles, kampen, elites bij oriëntatiepunten; leiband,
  groepsaggro; opgevrolijkte wezens huppelen ~10 s rond.
- Critters (vlindertjes, kikkerguhs, visjes, motten, glimguhtjes, vleermuizen) en sfeerdeeltjes per gebied.
- Sfeer: mist per gebied, lichtstralen in het Sporenwoud, **de draak vliegt soms laag over de stad** (elke 30-45 min),
  **lichtgolf** bij schemer in Zwamzoet, dagelijkse sporenstorm, roze buitjes + regenboog, noorderlicht boven de tempelberg,
  lichtzuil na de Opperzwam. Instelling `config/guhs-vadsrijk-sfeer.toml`: VOL / ZACHT / UIT.
- Momenten: de **terugkleurgolf** na zone 1 (de littekens kleuren live weer roze) en de **karavaan** (slakkenkar met handelaar
  Kruimel tussen De Haven, het Kustpad, Zwamzoet en het Sporenwoud; soms aangevallen: help hem).

**Quests**
- Elke stap zegt wie, waar en wat. Nieuwe quest **hq03b De elementendriehoek** (Opa Sterrenstof, een overhoring).
- hq10 en hq12 starten vanzelf (geen vastloper meer); "Weet je nog?" bij wie je het laatst iets inleverde.
- Nieuw geheim: de verstopte hoedenhut (schat `hoedenschat`).

## Wat is nieuw (fixronde 2, tweede playtest)
**Reizen en uitleg**
- **Reiskaart**: een grote getekende kaart van het Vadsrijk. Klik op een kapitein (Knuffelkamp, De Haven, Kandijkom) of luid een
  scheepsbel. Open plekken klik je aan (je vliegt meteen); plekken die je nog niet ontdekt hebt zijn grijs met een "?" en een regel
  wat je nog moet doen. De Schemerwortel (zone 3) en een vierde gebied zijn een voorproefje ("Binnenkort..."). Slepen met de muis =
  schuiven. De kapiteins geven ook gewoon de keuzes in het gesprek ("Naar Kandijkom (nog op slot)" zegt waarom).
- **Werver Wimpel** (Knuffelorde) staat bij het portaaltempeltje in de Guhmensie. Het portaal slaapt tot je met hem gepraat hebt:
  hij werft je, maakt een grapje (de "Wynn"-grap zit nu bij hem) en geeft weergave-advies. Dat advies staat ook in een tip en in
  de Avonturiersgids.
- **Losknuffelen (U)** wordt uitgelegd: Meester Kussensloop vertelt het, de welkomstregel noemt het, en de Avonturiersgids heeft
  een pagina "Vastzitten? Druk op U".
- **Job-advancement in de questvolger**: een lichtblauw blok met ✦ wijst naar de Grootmeester (of de Ordemeester in zone 2); in de
  questlog een rij "Klasse" onder het hoofdverhaal.
- **Zwamtempel**: de grote deur gaat vanaf **generiek nv 18 altijd** open (rechtsklik); met het verhaal al vanaf 16. hq11 begint
  met "Klik op de grote deur van de Zwamtempel". De regel onder de poort noemt je level.
- **Puzzelkamers**: de tip van de kamer staat onderin beeld zolang je erin staat (Zwamtempel en Spiegelburcht).
- NPC's staan op de goede plek: Bakker Bram in zijn kraam (niet op het dak), Bakker Speculaas voor zijn deur; nooit meer in een
  donker gat onder een huis.

**Kaart (zone 1)**
- Knuffelkamp: vanaf de bovenkant van de kliftrap loopt een **pad met randjes en lantaarns** naar het kampvuur (geen boom meer op
  de uitgang).
- Het **kasteeltje van Grootmeester Vadsvoet** (ordetempel) staat op een opgehoogde hoed: niets zweeft meer, de torens staan op
  een sokkel. De hoedhuisjes hebben een fundering (de wenteltrap naast hoed_noordo komt gewoon boven uit).
- **Biomes per plek**: F3 (F3+F6 voor de biome-regel) en JourneyMap tonen "Het Vadsrijk - Knuffelkamp / Zwamzoet / De Haven /
  Klodderweide / Sporenwoud / Druipgrotten / De Kloof / Zwamtempel / Tussenland / De Zee", in zone 2 "Kandijkom / Galmhellingen /
  Bevroren Knuffelmeer / Splinterkloof / Spiegelburcht / De Splinterpieken". Turquoise water in de Druipgrotten. (Oude chunks
  houden "Het Vadsrijk": nog een reden voor een nieuwe wereld.)

**UI**
- **Sieraden**: Shift+klik in je inventaris om om te doen; uitleg in de tooltip; een gedragen sieraad toont de volledige tooltip +
  "Klik om af te doen".
- **Skill-icoontjes** op de HUD en in de skillboom. Hover op een HUD-vakje = uitleg (energie, afkoeltijd, welke boomknopen en
  job-advancements hem verbeteren; wat je geleerd hebt in het groen met ✔).
- Harnas: "Set: Kristalset (2/4 gedragen)" met de vier stukken (gedragen in het groen); losse stukken: "Los stuk: hoort bij geen
  set." (Het Kristalhelmpje is een los zone-1-stuk, geen deel van de Kristalset.)

**Balans** (een gok: zeg wat te zwaar of te licht voelt)
- **Verdediging** werkt tegen het level van de aanvaller: verd / (verd + 25 + 15 x level), max 60 %. Harnassen geven 25 % minder
  verdediging dan eerst.
- **Gewone wezens en elites slaan 30 % harder** (bazen niet). Instelbaar: `[mobs] schade = 1.3` in het balansbestand (zie Klassen).
- **Drankjes** in vaste stappen: Klein 120 / Vadsdrankje 300 / Groot 700 Vads, Energie +50 / Groot +120. Drinken duurt 2 s, alle
  drankjes delen 10 s afkoeltijd (zichtbaar op het item). Het Klein Vadsdrankje is nieuw (8 brons).
- **Geen wilde spawns binnen ~5 blokken van een pad** (evenveel wezens, maar naast de paden).
- **Vahoegnir is 50 % groter**; de opening-vlucht ligt iets hoger. Kijk of het filmpje nog mooi in beeld is.
- **Opgevrolijkte wezens** zijn doorzichtig en hebben geen hitbox meer (pijlen en klappen gaan erdoor).

**Rijdieren (X / N)**
- X = opstijgen/afstijgen (1 s afkoeling). Je stijgt af als je geraakt wordt (5 s "schrik!"), in een bazenarena en bij flauwvallen.
  In dorpen mag je blijven rijden. Werkt in het hele Vadsrijk.
- N = rijdierenscherm: alle rijdieren (wat je nog niet hebt als donker silhouet + "?"), een draaiend voorbeeld,
  level/xp/snelheid/sprong, "Hierop rijden". Bij de stal ook kopen.
- **Pluimguh** (hoofdrijdier, placeholder-model; kleuren roze/mint/lavendel = starters, honing (stal), wolkwit (quest),
  sterrennacht (bazenkist van Koning Klodder of de Galmwolf, 5 %), goud (geheim: Opperzwam of Spiegelzucht, 2 %)),
  **Wolkenschaapje** (terugkeerquest van Stalmeester Hoefje, of de stal; zweeft, dubbele sprong), **Slakkie** (stal; traag maar
  luxe), **Knuffelhert** en **Guhwolf** (terugkeerquests). De Hoedhupper en de Klodderros zijn weg (ronde 3).
- **Rijdieren beginnen in zone 3** (ronde 3): je eerste rijdier is de starter-Pluimguh in De Schemerwortel. Daarvoor zeggen X en N
  "Nog geen rijdier: dat krijg je in De Schemerwortel." en verkoopt de stal niks ("Eerst je eigen Pluimguh in De Schemerwortel.").
  De drie terugkeerquests (Wolkenschaapje bij Hoefje, Knuffelhert bij Boswachter Mos, Guhwolf bij Breister Wolwinter) komen pas
  daarna; praat je eerder met die drie, dan vertellen ze er alvast een plagerig stukje over. Dev-commando's geven gewoon rijdieren.
- **Snelheden** (ronde 3, % van gewoon lopen, level 1): Guhwolf 370 (lage sprong, ~2,4 blokken), Pluimguh 350 (~3,3), Knuffelhert
  320 (hoogste sprong, ~5 blokken), Wolkenschaapje 300 (+ dubbele sprong), Slakkie 85. Per level +1,5 % snelheid (max +13,5 %).
  Instelbaar in `config/guhs-vadsrijk-balans.toml`, sectie `[rijdieren]` (snelheid, sprong, groei_snelheid, groei_sprong).
- **Opstapjes:** elk rijdier loopt zonder springen een randje van 2 blokken op (trappen, halve blokken). 2,5 blok of hoger: springen.
- De starterkeuze (1 van de 3 Pluimguhs gratis) hoort bij de zone-3-quest; testen met `/guhs vadsrijk mount starter`.
- **Terugkeerquests** (pas zichtbaar als je een rijdier hebt): "Een eigen Wolkenschaapje" (zone 1, Stalmeester Hoefje), "Het Knuffelhert" (zone 1, Boswachter Mos, lv 10: steek 3
  lantaarns aan in het Sporenwoud en volg de hoefsporen naar de open plek) en "De verdwaalde Guhwolf" (zone 2, Breister Wolwinter
  in Kandijkom, lv 22: zoek het wolfje op de kloofbodem, vrolijk 5 wezens op in de Splinterkloof).

**Huisdieren (P)**
- Versla een baas en zijn mini-versie loopt voortaan met je mee (één keer per baas): Mini Grauwe Klodderreus, Mini Koning Klodder,
  Mini Galmwolf. Titelkaart "Nieuw huisdier!" na de overwinning. Je eerste huisdier loopt meteen mee.
- P = huisdierenscherm: draaiende minimodelletjes (onbekend = silhouet), "Meenemen" / "Thuislaten". Ook een knop "Huisdieren" in
  het rijdierenscherm.
- Huisdieren doen niks in gevechten, zijn niet te raken, volgen je (teleporteren mee als je ver weg bent) en zijn alleen in het
  Vadsrijk.

## Klassen: altijd alle drie (fixronde 2, g-klassen)
- **Je bent altijd Knuffelridder, Pluisjager en Vadsmagiër.** Je klasse is het wapen in je hand: zwaard/knots/hamer/kling =
  Knuffelridder, boog/kruisboog/katapult = Pluisjager, staf/toverstok/boek = Vadsmagiër.
- Je wisselt pas als je **aanvalt** (mep, schot of R/V/G/Z) met een wapen van een andere klasse, niet bij vasthouden of scrollen.
  Dan beginnen R/V/G van die klasse op hun **volle cooldown** en de ult-meter op 0. De Knuffelsprong (C) blijft gewoon.
- Geen klassewapen in je hand: geen vaardigheden, alleen een zwak klapje; de klasseregel op de HUD wordt grijs.
- **Twee niveaus**, links van de hotbar: "Generiek nv X" (goud) en "<klasse/titel> nv Y" (roze). Alle xp gaat volledig naar
  generiek EN naar de klasse van het wapen (bij een verslagen wezen: de klasse die er de meeste schade op deed). De xp-balk:
  goud = generiek, dun roze lijntje eronder = klasse.
  - Generiek: Vads, Verdediging, gebieden, quests, de tempeldeur (nv 16 + quest), het luchtschip naar zone 2 (nv 20),
    harnassen en sieraden ("Vereist: Generiek nv X").
  - Klasse: Kracht/Behendigheid/Wijsheid, skillpunten van die klasse, wapens ("Vereist: Pluisjager nv 14", rood als je het
    niet haalt), job-advancements (klasse nv 10/20/30).
  - Een wapen dat je nog niet mag: vasthouden mag, aanvallen niet (rode melding), en je wisselt er ook niet mee van klasse.
- Knuffelkamp: het klasse-tempeltje = **kies je startwapen**. De andere startwapens: bij de **trainers op het trainingsveld**
  ("Mag ik een Pluisboog?") of aan de ordetempelbalie in Zwamzoet (eerste per klasse gratis, daarna 30 brons). De balie doet verder respec per klasse en de ceremonies; wisselen hoeft daar niet meer.
- Skillboom (K): de boom van je actieve klasse, met **tabs** bovenaan voor de andere twee (⚔ = actief). Punten per klasse komen
  van het klasseniveau; respec per klasse.
- Knuffelridder is taaier: 20 % minder schade zolang hij met een melee-wapen actief is, en +4 Vads per klasseniveau.
- **Oude saves**: generieke xp = de hoogste klasse-xp (niet de som), klasseniveaus blijven.

**Balans per klasse (live, zonder herstart)**: `config/guhs-vadsrijk-balans.toml` (in de run-map; wordt de eerste keer met
standaardwaarden geschreven). Per klasse `[knuffelridder]` / `[pluisjager]` / `[vadsmagier]`: `schade`, `skillschade`, `vads`,
`verdediging`, `cooldowns`, `energie` (vermenigvuldigers, 1.0 = normaal), `melee_reductie` (0.20 = 20 % minder schade met een
melee-wapen), `vads_per_klasselevel`. `[algemeen]`: `xp_schaal_generiek`, `xp_schaal_klasse`. `[mobs]`: `schade` (1.3 = gewone
wezens +30 %).
- `/guhs vadsrijk balans toon` - alles bekijken
- `/guhs vadsrijk balans <klasse|algemeen|mobs> <sleutel> <waarde>` - meteen actief en meteen in het bestand
- `/guhs vadsrijk balans herlaad` - het bestand opnieuw lezen (een bewerkt bestand wordt ook vanzelf binnen 5 s gelezen)

## Zone 2: De Splinterpieken (lv 20-30), opnieuw gebouwd
**Erheen**: zone 1 klaar (`zone1_klaar`) + generiek level 20. Via de reiskaart (kapitein of scheepsbel) of sluip + rechtsklik op
de Haven-bel. De eerste keer: het openingsfilmpje (sneeuwstorm, de Galmwolf huilt op een naald, de naald barst, de warme geode).
Terug: de bel op de steiger van Kandijkom (reiskaart) of Kapitein Poolster.

**Nieuw**: zone 2 is nu **compact** (~500 x 575 in plaats van 1200 x 1200), een eiland in een wolkenzee, met meer roze/kristal,
meer leven, minder wit en meer hoogte. Alles ligt dichter bij elkaar.
- **Kandijkom** (in de geode, kleiner en hoger): twee **terrassen** tegen de geodewand met trappen naast elke poortstraat en
  bruggetjes, een **kristallen klokkentoren** (34 hoog) midden op het plein met lampionslingers, een **kerstmarkt** (kraampjes, een
  chocomelkkraam bij de schaatsvijver, vuurkorven), de versierde kerstboom, **19 gebouwen met echte interieurs** (open haard,
  bedden, boeken, chocomelkbekers; de chocoladekelder onder het café) en kristallen in de geodewand. Stadsleven: schaatsers,
  sneeuwbalkinderen, pinguguhs, een sleefamilie, marktguhs. De stal staat buiten de zuidpoort.
- **Vier open poorten** (oost, west, noord, zuid): de poortstraten lopen vlak door de wand en dan geleidelijk omhoog.
- **Galmhellingen** (oost, lv 20-23): de Galmspleet met de gondel erboven, de bevroren lawinegolf over het pad, de **Galmwolf**.
- **Bevroren Knuffelmeer** (west, lv 23-26): knuffels onder het ijs, de versteende guhs, het drakenzadel. De oever is overal één
  stapje hoog (je loopt het ijs weer af).
- **Splinterkloof** (noord, lv 26-29): ravijn met 4 hangbruggen, bevroren watervallen en richels; wezens op de kloofbodem; de
  **Mika-boormeester**-grot (geheim).
- **Spiegelburcht** (noordoost op zijn piek, lv 28-30): Lichtkamer, Spiegelgang, Prismazaal, **Vrouwe Spiegelzucht** (alleen het
  spiegelbeeld ZONDER schaduw is zij).
- Kristalbossen, geodegrotten, gehuchtjes met hutjes, lampionpalen langs elk pad, een warmwaterbron.
- **Quests**: het hoofdverhaal (12) + **5 korte zijquests** (knuffels onder het ijs, schaatsles, het echokoor, ijsbloemen voor de
  chocomelk, "Naar de top!" met de gondel) + de klassenproeven (klasse nv 30) + het geheim van de boormeester + De verdwaalde Guhwolf.
- Systemen zoals eerst: sokjes met kristallen, juwelier, Kandijkraam, status **Bevroren**.

## ZONE 4: De Lampionkust (nachtronde, lv 50-56)
Een keten eilandjes ten oosten van de Schemerwortel (x 1740..2540, z -716..-266). Erheen: lijn C vanaf de Oosttaksteiger, of
`/guhs vadsrijk z4verhaal klaarzetten` + de reiskaart ("De Lampionkust" opent na zone 3 + lv 50). Alles staat in de SPIEKBRIEF
(hoofdstuk 4); waar je vooral op let:
- **Lampionhaven** (de stad, kampvuur, bank, werkplaatsen, de luchthaven waar het schip van lijn C aanlegt), het **Knuffeltij-strand**
  met de vluchtelingen, het Strandfeest-, Bloemen-, Vlieger-, Vuurtoren- en Mysterie-eiland, de Koraalbaai, de Pierkop met de Opstijgpier.
- **Je zeilbootje** (krijg je in z4_hq02): X op het water, zeilen met de wind, de Kringstroom duwt je terug aan de rand, aanmeren bij
  11 steigers. N → tab Zeilbootje voor zeilen/wimpels/boegbeelden.
- **Ouwe Dobber** zwemt zijn rondje rond Oma Dobber (het schildpadeiland) en legt aan bij de steiger: je kunt meerijden. Vliegers in de lucht.
- **Bazen:** de Grauwe Zeeslurf (in het diepste grotje van de Koraalbaai, het vlot) en Kootje de Vliegervanger (in het ruim van het Wrak).
- **Verhaal:** "De Grote Knuffeltij" (7 hoofdquests) + 16 zijquests; de cliffhanger op de Opstijgpier opent lijn D naar Hoogvads.
- **Lopen:** loopcheck4 0 bevindingen, 371 plekken sta-baar; mist rond het Mysterie-eiland is opzet.

## ZONE 5: Hoogvads (nachtronde, lv 56-60)
De stad op de rug van Moeder Wolkwal, hoog boven zone 4 (y 200+). Erheen: lijn D vanaf de Opstijgpier (na het zone-4-verhaal), of
`/guhs vadsrijk z5verhaal klaarzetten`. Alles in de SPIEKBRIEF (hoofdstuk 5); waar je vooral op let:
- **De stad:** paleis, universiteit, academie, markt, kathedraal, luchthaven met 3 pieren, staartpark, ooggaarde; interieurs en
  bovenverdiepingen; het **Wolkvangnet** vangt je op als je van de walvis valt.
- **Verhaal:** "Verraad in de stad" (10 hoofdquests, speuren met aanwijzingen en verhoren) + 11 zijquests; de finale eindigt in
  **De Grauwe Droom** op de Droomwolk.
- **Gevechten:** de Proefarena (sparren met de Wolkwacht), de Buikgangen (Opzichter Gorgonzola), de Wolkrand (Donderbuik), het
  Schuilnest (de spion Kootje Zilverstaart: een collega van de Vliegervanger, een andere Kootje).
- **Ambachtsgilde:** Hoogvads is de hoofdstad van de ambachten; de leermeesters openen hun werkplaats.

## Dev-commando's (op nodig)
**Kern**: `/guhs vadsrijk` (erin/eruit), `proeftuin`, `level <1-35>`, `xp <n>`, `munten <n>`, `loot <zeldzaamheid> [type] [niveau]`,
`vol`, `vast`, `sprong` (= C), `baas reset <arena>` (`npc:kampvijver`, `bazen:koning_klodder`, `bazen:opperzwam`,
`z2bazen:galmwolf`, `z2bazen:spiegelzucht`), `tips reset`, `kist`, `info`, `party uitnodigen <naam>` / `party accepteer`.

**Beleving**: `/guhs vadsrijk beleving getallen | reeks <n> | klap | schud | fase | overwinning | versla | kist <zeldzaamheid> [pos]`.

**Wereld**: `/guhs vadsrijk wereld plek <id>` (alle ids uit POI_VADSRIJK.md), `deur ordetempel|zwamtempel|spiegelburcht open`,
`vlieg haven|kamp` (rondvlucht), `littekens heel|grijs`, `uitkijk <naam>`, `vergeetportaal`.
**Sfeer**: `/guhs vadsrijk sfeer draak [stad|hier] | golf | storm | bui | noorderlicht | stop | zuil aan|uit | status`.
**Momenten**: `/guhs vadsrijk momenten golf [grijs|plan]`, `momenten karavaan info|tp|hier|aanval|winkel`.

**NPC's en quests**: `/guhs vadsrijk npc log | questlog | quest start|klaar|af|vergeet <id> | tp <npc|poi.<id>|knuffel.<id>|aanwijzing.<id>|schat.<id>> | reset | opening | bam | cliffhanger | vlieg stad|kamp | npcs`.
**Wezens**: `/guhs vadsrijk wezens spawn <soort> [level] [elite] | affix ... | opruimen | kampklodder | overvlucht | draak <ticks> | set ... | loot ... | spawners`.
**Bazen (zone 1)**: `/guhs vadsrijk bazen tp koning|tempel|opperzwam|markt|mika | spawn ... | fase 2|3|4 | split | loot <baas> | items | herstel | tempel reset|open|oplossing | mika ontmasker|reset`.
**Rijdieren**: `/guhs vadsrijk mount geef <soort> [kleur] | alle | reset | starter | scherm | stal | rijd <soort> [kleur]`.
**Huisdieren**: `/guhs vadsrijk huisdier geef <klodderreus|koning_klodder|galmwolf> | alle | reset | scherm | kies <id|geen> | weg`.
**Werver/praten**: `/guhs vadsrijk npc werver` (Wimpel aan je voeten) / `npc werver vergeet`; `npc praat <id>` (een NPC aanklikken).
**Klassen**: `/guhs vadsrijk klasse kies | boom | balie | wissel <klasse> | wapen <type> | skill 0-3 | adv | respecvrij | reset | punten <level> | klasselevel <klasse> <n> | generiek <n>`; `level <n>` (kern) zet generiek + de actieve klasse. **Balans**: `/guhs vadsrijk balans toon | herlaad | <klasse|algemeen|mobs> <sleutel> <waarde>`.
**Economie**: `/guhs vadsrijk eco winkel <id> | smid | markt | stal | spul | ster <n> | verpluis | handel <speler> | rijdier ... | rijdierxp <n> | ga <plek>`.

**Zone 2**
| commando | wat |
|---|---|
| `/guhs vadsrijk z2wereld plek <id>` | elke plek uit POI_Z2.md |
| `/guhs vadsrijk z2wereld vlieg kandijkom|haven` / `opnieuw` | de luchtschiplijn (eerste keer = opening) / opening opnieuw |
| `/guhs vadsrijk z2wereld gondel op|neer` | de gondel |
| `/guhs vadsrijk z2npc tp <npc> | vlieg kandijkom|haven | gondel op|af | opening | draak | flashback | cliffhanger | reset` | zone-2 verhaal |
| `/guhs vadsrijk z2wezens spawn <soort> [level] [elite] | alle | affix | bevroren | set | sieraden | loot | opruimen | spawners` | zone-2 wezens |
| `/guhs vadsrijk z2bazen tp ... | fase 2|3 | split | boor ontgrendel|reset|volgorde | burcht reset|los|oplossing | loot | items | herstel` | zone-2 bazen |
| `/guhs vadsrijk z2systemen juwelier | winkel | kristal <soort> <0-2> [n] | sokjes <0-2> | bevries` | sokjes, kristallen, Bevroren |

Soorten zone-2 wezens: splinterling, galmvleermuis, sneeuwknabbelaar, kristalnaaldkruiper, ijzelgeest, schotsbijter,
versteende_wacht, ijspuffer, mika_mijnwerker, mika_boorder, kloofgier, kristalspin, spiegelwacht, scherfgeest.

## Waar op letten
- **Fixronde 3:** klinken de stadsdeuntjes lief (niet te hard, niet te vaak)? Ziet de dok-aankomst in Kandijkom er goed uit
  (poortboog, bergpas, kiosk)? Loop je zonder springen over alle hangbruggen? Voelen de nieuwe rijdiersnelheden goed, en is het
  OK dat een rijdier over een hek stapt? Is het Verkenningsboek duidelijk, en kloppen de tabjes N/P/B?
- **Fixronde 2**: voelt de klasse-wissel via het wapen goed? Zijn de twee levels duidelijk? Is vechten nu te zwaar (+30 %)?
  Rijden met X/N, huisdieren met P, de reiskaart, Werver Wimpel, en de nieuwe zone 2 (Kandijkom, poorten, het meer, hoogte).
- De draak is 50 % groter: past hij nog mooi in de opening-vlucht en de drakenpas over Zwamzoet?
- **Lopen in survival**: kom je overal weer uit (water, kuilen, trappen)? Blijf je ergens hangen: druk U en meld de plek.
- De draak over de stad, de lichtgolf bij schemer, de sporenstorm: te veel of te weinig? (Sfeer-instelling ZACHT/UIT bestaat.)
- Knuffelsprong: voelt ~20 ver en ~5 hoog goed? Aanvalstempo per wapen: niet te traag?
- Bazen op afstand: rennen ze naar je toe / gooien ze iets als je van ver schiet?
- De questvolger: klopt "wie en waar" bij elke stap, ook in zone 2 (luchtschip-hint)?
- Zone 2: de opening, Kandijkom, gondel, de drie gebieden met wezens, de Galmwolf-pilaren, de spiegelpuzzels, Spiegelzucht's
  schaduwloze beeld, de boormeester-volgorde.

## Bekende gaten
- **Audit** (AUDIT_VADSRIJK.md, na de laatste kaart): 0 keer onder de kaart; 6 "kuilen": de vuurtorenkelder (vals alarm: een open
  luik boven een ladder), een dichte holte onder de ordetempelvloer (de audit telt hem, je kunt er niet in), 1 blokje in Zwamzoet,
  4 kolommen op de Hoedenpaden, en de Druipschacht (de eigen checker van de schacht zegt: 0 vallen; de audit kent zijn muurrailing
  niet). 14 "trappen" = dakpannen van de A-tentjes. 5 waterplekken: een stuk van de baai van Zwamzoet (verder dan 10 slagen van een
  ladder), het Druipschacht-meer (de wenteltrap begint aan de oever).
- Twee decoratieve deuren in Zwamzoet zitten dicht (stal-zijdeur, een hoeddeur); gras onder overkappingen buiten.
- Zone 2: U gebruikt de kampvuren en de plekken van de kaart (geen fijnmazige audit-lijst zoals zone 1). loopcheck zone 2: een
  paar dichte holtes onder de terrashuizen van Kandijkom (je kunt er niet in) en losse blokjes; de "trappen" zijn stoelen/bankjes.
- Het Pluimguh-model is een placeholder; prijzen en snelheden van de rijdieren zijn een gok. Huisdieren: maat en animatie nog niet
  beoordeeld; de Mini Klodderreus is grijs.
- De starterkeuze van de rijdieren hoort bij zone 3 (nog niet gebouwd); de reiskaart toont zone 3/4 als voorproefje.
- Muziek alleen in de steden (ronde 3, gesynthetiseerd); verder zijn geluiden vanilla. Modellen en plaatjes zijn een eerste gegenereerde versie. Balans is een eerste gok.
- Er is maar één opening naar Kandijkom (die van de wereld-slice); de Galmwolf daarin is een geluid, geen model.
- De karavaanroute is uitgerekend op de kaart van vóór de fixronde; hij is gecontroleerd op de nieuwe kaart (geen water, kleine
  opstapjes), maar rijdt in Zwamzoet onder een plankenbrug door.
- De twee wezens-sferen (critters/deeltjes van `leven` en de mist/storm van `sfeer`) lopen allebei in het Sporenwoud: kijk of het
  niet te druk wordt.
- **Ronde U, nog niet in een client gezien** (alleen gametests en checkers): het Vadsrijk-menu en de subtabs, de spec-rijen 8/9 op
  kleine schermen, de raid-frames, de kastelen en lantaarns van de huizen, de speeltuinen, de VIP++-grap, het Questlopen-paneel.
- **zweefcheck:** zone 1 115, zone 2 20, zone 4 26, zone 5 141 "zwevende" decoblokken: muurlantaarns in Zwamzoet en
  lampionslingers in Hoogvads (zo bedoeld); zone 3 en 6: 0.
- **verbindcheck:** 8 meldingen bij de Raadszaal (de `up`-vlag van een muur, alleen cosmetisch).
- **Lijn 0** (kamp ↔ De Haven) is nog uit: die havens hebben nog geen steiger.

