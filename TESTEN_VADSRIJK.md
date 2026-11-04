# Testen: Het Vadsrijk (Guhs 1.10), na fixronde 2: zone 1 + de nieuwe zone 2 "De Splinterpieken"

Dit is de ENE testgids voor het hele Vadsrijk. Hij vervangt `TESTEN_VADSRIJK_ZONE1.md` en de losse slice-gidsen (die blijven
alleen als naslag). Gebouwd in `guhs_vadsrijk\g-int` (branch `110-g-int`, fixronde 2 = tweede playtest).

## Starten
1. Dubbelklik `run-client.bat` in `guhs_vadsrijk\g-int` (NIET f-int of w-poc: dat zijn de oude rondes).
2. Maak een **nieuwe** wereld (Creatief, cheats aan). De `saves`-map is leeg: oude werelden kennen de nieuwe kaart (en de nieuwe
   zone 2) niet.
3. Zet je **weergaveafstand** hoog (32 chunks, minstens 20): het Vadsrijk is met de hand gebouwd en kan dat makkelijk aan.
4. `/gamemode survival` als je wilt vechten (in creatief vallen de wezens je niet aan; bazen doen hun aanvallen wel, maar raken je niet).
5. Erin: `/guhs vadsrijk` (of in survival via het portaaltempeltje in de Guhmensie, na een praatje met Werver Wimpel). Nog een keer = terug naar huis. In het Vadsrijk heb je een eigen, lege rugzak.

Je komt aan op het lage steigertje aan de zuidkust van Knuffelkamp. De houten trap loopt nu helemaal door, als een tunnel in de
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
| **X** | **opstijgen / afstijgen** (nieuw): je actieve rijdier. Icoontje rechts naast de vaardigheden op de HUD. |
| **N** | **rijdierenscherm** (nieuw): alle rijdieren, wat je nog niet hebt als silhouet, kleuren van de Pluimguh |
| **P** | **huisdierenscherm** (nieuw): "Verzameld x/y", kies wie er met je meeloopt |
| J | je questlog (hoofdverhaal bovenaan, "Nu: ..."; Volgen/Niet volgen voor zijquests; Gids met de elementendriehoek) |
| (geen toets) | questvolger aan/uit en lichtstraal aan/uit: knoppen in de questlog, of zelf een toets kiezen |
| Rechtsklik | NPC's, kampvuren, deuren, de scheepsbel, munten, sieraden, de draaispiegels in de Spiegelburcht |
| Rechtsklik op een scheepsbel | **de reiskaart** (nieuw): kies waar je heen vliegt (Knuffelkamp, Zwamzoet, Kandijkom) |
| Sluipen + rechtsklik | op een speler: ruilen. Op de **scheepsbel in De Haven**: meteen de lijn naar Kandijkom |
| Sluipen tijdens een rondvlucht | de luchtschipvlucht overslaan (pas na de eerste keer) |
| F9 / F10 | (Guhs 1.9, buiten het Vadsrijk) Erfenis-bovenaanzicht / wereldkaart. Waren O/M; een oude O/M-toets wordt één keer vanzelf verhuisd. |

**Questvolger** (rechtsboven, onder de bazenbalk als die er is): wat, bij wie, waar, pijltje + afstand. Een roze lichtstraal
boven je doel (vastgepinde zijquest: lichtgeel). Werkt nu ook in zone 2 en wijst je de luchtschiplijn aan als je doel aan de
andere kant ligt.

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
- Geen muziek; geluiden zijn vanilla. Modellen en plaatjes zijn een eerste gegenereerde versie. Balans is een eerste gok.
- Er is maar één opening naar Kandijkom (die van de wereld-slice); de Galmwolf daarin is een geluid, geen model.
- De karavaanroute is uitgerekend op de kaart van vóór de fixronde; hij is gecontroleerd op de nieuwe kaart (geen water, kleine
  opstapjes), maar rijdt in Zwamzoet onder een plankenbrug door.
- De twee wezens-sferen (critters/deeltjes van `leven` en de mist/storm van `sfeer`) lopen allebei in het Sporenwoud: kijk of het
  niet te druk wordt.
