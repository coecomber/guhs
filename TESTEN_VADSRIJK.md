# Testen: Het Vadsrijk (Guhs 1.10), zone 1 na de fixronde + zone 2 "De Splinterpieken"

Dit is de ENE testgids voor het hele Vadsrijk. Hij vervangt `TESTEN_VADSRIJK_ZONE1.md` en de losse slice-gidsen (die blijven
alleen als naslag). Gebouwd in `guhs_vadsrijk\f-int` (branch `110-fix-int`).

## Starten
1. Dubbelklik `run-client.bat` in `guhs_vadsrijk\f-int` (NIET w-poc: dat is de oude POC).
2. Maak een **nieuwe** wereld (Creatief, cheats aan). De `saves`-map is leeg: oude werelden kennen de nieuwe kaart niet.
3. `/gamemode survival` als je wilt vechten (in creatief vallen de wezens je niet aan; bazen doen hun aanvallen wel, maar raken je niet).
4. Erin: `/guhs vadsrijk`. Nog een keer = terug naar huis. In het Vadsrijk heb je een eigen, lege rugzak.

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
| K | je skillboom (nu 6 rijen: rij 4 vanaf level 20, rij 5 vanaf level 26) |
| J | je questlog (hoofdverhaal bovenaan, "Nu: ..."; Volgen/Niet volgen voor zijquests; Gids met de elementendriehoek) |
| (geen toets) | questvolger aan/uit en lichtstraal aan/uit: knoppen in de questlog, of zelf een toets kiezen |
| Rechtsklik | NPC's, kampvuren, deuren, de scheepsbel, munten, sieraden, de draaispiegels in de Spiegelburcht |
| Sluipen + rechtsklik | op een speler: ruilen. Op de **scheepsbel in De Haven**: de lijn naar Kandijkom |
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

## Zone 2: De Splinterpieken (lv 20-35)
**Erheen**: zone 1 klaar (`zone1_klaar`) + level 20. Praat met Stuurguh Zeppie in De Haven ("Naar Kandijkom") of sluip + rechtsklik
op de Haven-bel. De eerste keer: het openingsfilmpje (sneeuwstorm, ijsschotsen, licht uit, een Galmwolf huilt op een naald, de naald
barst, de warme geode). Terug: de bel op de steiger van Kandijkom of Kapitein Poolster.
- **Kandijkom**: roze kwarts-geode, chocomelkhuisjes met sneeuwkappen, schaatsvijver, Kristalkapel (3e titel op level 30),
  juwelier (edelstenen inzetten/uithalen), Kandijkraam (grote drankjes, warme chocomelk = ontdooien), café met geheime kelder,
  **gondel** naar de Galmtop.
- **Galmhellingen** (20-25): naaldvelden, lawinebanen, de **Galmwolf** (wereldbaas): schuil achter de ijspilaren als hij huilt.
- **Bevroren Knuffelmeer** (25-30): knuffels onder het ijs (Vlokje's zijquest), ijsvissers, het drakenzadel.
- **Splinterkloof** (30-34): ravijn met hangbruggen en Mika-boortorens; wezens ook op de kloofbodem.
- **Spiegelburcht** (33-35): Lichtkamer (straal met draaispiegels naar het slot), Spiegelgang (scherven + 3 golven), Prismazaal
  (2 stralen), dan **Vrouwe Spiegelzucht**: alleen het spiegelbeeld ZONDER schaduw is zij; klap de spiegel plat op de lijn van het
  oplichtende oog om haar te verblinden.
- **Mika-boormeester** (geheim): vind de 5 aanwijzingen; oververhit de 3 boorkoppen in de goede volgorde (het bedieningspaneel
  speelt de volgorde voor met tonen).
- Nieuw in de systemen: level-cap 35, 3e job-advancement op 30 (Kristalkampioen / Poolstervalk / Kristalorakel), **sokjes**
  (0-2) met kristallen (rozekwarts, zonnesteen, galmkristal, spiegelkwarts, vonkkristal, ijsglans; ruw / geslepen / stralend),
  status **Bevroren** (trager; zware klappen +30 %; staat als blauwe regel boven je balken).
- 12 hoofdquests, 23 zijquests, 3 klassenproeven, 50 NPC's, geheimen (10 knuffels onder het ijs, 5 aanwijzingen, 5 schatten).

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
**Klassen**: `/guhs vadsrijk klasse kies | boom | balie | wissel <klasse> | wapen <type> | skill 0-3 | adv | respecvrij | reset | punten <level>`.
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
- Zone 2 heeft nog geen "veilige plekken"-lijst voor U: het valt terug op kampvuren/je checkpoint.
- Geen muziek; geluiden zijn vanilla. Modellen en plaatjes zijn een eerste gegenereerde versie. Balans is een eerste gok.
- Er is maar één opening naar Kandijkom (die van de wereld-slice); de Galmwolf daarin is een geluid, geen model.
- De karavaanroute is uitgerekend op de kaart van vóór de fixronde; hij is gecontroleerd op de nieuwe kaart (geen water, kleine
  opstapjes), maar rijdt in Zwamzoet onder een plankenbrug door.
- De twee wezens-sferen (critters/deeltjes van `leven` en de mist/storm van `sfeer`) lopen allebei in het Sporenwoud: kijk of het
  niet te druk wordt.
