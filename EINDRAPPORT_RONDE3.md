# Eindrapport: Het Vadsrijk, ronde 3 + zone 3 "De Schemerwortel"

Datum: 2026-11-04. Er is niets gepusht of uitgebracht. Wel live gezet: de hotfix 1.9.2 (Erfenis-toetsen F9/F10).

## Zo test je
1. **Starten:** dubbelklik `guhs_vadsrijk\k-int\run-client.bat` en maak een **nieuwe wereld** (cheats aan, weergaveafstand 20-32).
2. **Spiekbriefje:** `k-int\SPIEKBRIEF.md` (copy-paste commando's per onderdeel).
   - **Z1-Z7:** zone 3. Snel klaarzetten met `/guhs vadsrijk z3npc klaarzetten` (zone 2 klaar + level 30).
   - **R1-R10:** fixes uit ronde 3 (zone 1/2).
   - **1-13:** ronde 2 (klassen, balans, mounts, huisdieren, reiskaart, zone 2).
3. **Volledige testgids:** `k-int\TESTEN_VADSRIJK.md` (alle toetsen en dev-commando's).

**Toetsen in het Vadsrijk:**

| Toets | Wat |
|---|---|
| R / V / G | vaardigheden |
| Z | ult |
| C | Knuffelsprong |
| U | losknuffelen |
| K | skillboom |
| J | questlog |
| X | op- en afstijgen |
| N | rijdieren |
| P | huisdieren |
| B | Verkenningsboek |

**Status:**
- Gametests: 116 Vadsrijk-gametests; na de laatste fixes allemaal groen.
- AutoCheck-wandeling over zone 3: 32 screenshots, zonder crash.

## Wat er deze ronde (en sinds de vorige samenvatting) gebeurd is

### Zone 1 + 2: fixes en verbeteringen
- **Melee:**
  - Het startzwaard doet 2× schade.
  - Linkermuis ingedrukt houden blijft meppen.
  - "Soms geen schade" is opgelost: het lag aan de vanilla-laadeis en het tick-verschil. Een te vroege klik wordt nu onthouden.
  - De vijver-arena van de Klodderreus is groter, met droge grond eromheen.
- **Buitenstaanders:** Makelaar Guhrrit, de Postguh, je eigen guhs, volgers en Momo komen niet meer in het Vadsrijk. Post wacht tot je thuis bent.
- **Huisdieren:** geen naambordje meer.
- **Reiskaart:** sluit met ✕.
- **Collectieschermen:** tabs tussen Rijdieren, Huisdieren en het Verkenningsboek.
- **Rijdieren:**
  - Hoedhupper en Klodderros zijn weg.
  - Snelheden: wolf 370 %, Pluimguh 350 %, hert 320 % (hoogste sprong), schaapje 300 %, Slakkie 85 %.
  - Staphoogte 2: ze lopen ook bewust over hekken en muurtjes.
  - Rijdieren krijg je pas in zone 3. Ook de stal verkoopt pas daarna.
  - Wolkenschaapje (zone 1), Knuffelhert (zone 1) en Guhwolf (zone 2) zijn terug-quests. De questgevers geven vooraf een teaser.
- **Verkenningsboek (B):**
  - Per zone x/y voor verhaal, zijquests, uitkijkpunten, kampvuren, geheimen, knuffels, bazen, huisdieren en gebieden.
  - Mijlpalen bij 25/50/75 %; titel en munten bij 100 %.
  - Het boek groeit mee met elke zone; zone 3 zit er al in.
- **Zone 2:**
  - Alle hangbruggen sluiten aan, ook die bij Touwtje.
  - De aankomst vanaf het dok in Kandijkom is natuurlijker:
    - Het zwevende glas is weg; er zijn poortbogen en een glooiende bergpas.
    - Lampions, lichtsnoeren, vuurkorven, een chocomelk-kiosk en een welkomstboog.
- **Stadsmuziek en -geluid:**
  - Elk stadje heeft 2 eigen lieve deuntjes: Zwamzoet, Knuffelkamp, De Haven, Kandijkom en nu ook Honingtak.
  - Na jouw feedback zonder wind: vogeltjes en belletjes, kabbelend water in De Haven, een knisperende haard met arrensleebelletjes in Kandijkom.
- **Kaart:**
  - Het stalletje van de stalmeester staat op het echte maaiveld, met een pad.
  - De deuren in Zwamzoet en De Haven zijn vrij.
  - De hekken op de pieren (De Haven, baai van Zwamzoet) zijn weg.
- **Lore (15 Keizer-verwijzingen in zone 1 en 2):**
  - Oma Truus over de starende tweede maan.
  - Een "Aan de Pink"-briefje.
  - Een speldje met een opgestoken pinkje.
  - De boormeester verspreekt zich één keer ("De Keizer…").
  - Aan het eind van zone 1 knippert de maan, en in de flashback van zone 2 is een arm aan de horizon te zien.
  - De cliffhanger van zone 2 wijst nu naar het **oosten**.

### Het grote verhaal (nieuw)
- **Wereldverhaal:** `guhs1100-avontuur\WERELDVERHAAL.md`, met de getekende kaart in `guhs1100-avontuur\wereldkaart\wereldkaart.png`.
- **De Grauwe Keizer-Mika:** de echte eindbaas, aan de verre (oostelijke) kant van de wereld.
  - De draak was een slachtoffer.
  - Zijn generaals heten de "Vingers"; Graaf Piepvel is de eerste.
- **De reis:** ~20 zones in 6 delen. De hoofdstad Hoogvads (zone 5) is het zwevende kasteel aan de horizon.

### Zone 3 "De Schemerwortel" (lv 30-50), helemaal nieuw
- **De wereldboom:** in het oosten, met lagen kruin → takken → holle stam → wortels.
  - **Honingtak:** een honingstadje met interieurs, de Pluimstal, de Nestkapel en een eigen muziekje.
  - **Vijf gebieden:** De Kruinzee, De Takkenbanen, De Schorsstad, De Wortelvelden en Het Wortelduister. In het Wortelduister geldt een lantaarnregel: buiten het licht zijn vijanden silhouetten die halve schade krijgen.
  - **Omhoog en omlaag:** 7 windkolommen, honingliften, glijden met de Pluimguh, en rijroutes.
  - **Biomes per laag:** "Het Vadsrijk - De Kruinzee" enz.
- **Rijdieren:** stap 3 van de hoofdquest. Stalmeester Veertje laat je kiezen uit een roze, munt- of lavendel-**Pluimguh**, met het chocobo-grapje erbij. Daarna volgen je eerste rit en je eerste glijvlucht.
- **Wezens:**
  - 14 soorten: rot, schimmel, Mika's, geesten en harsgolems.
  - 5 gebiedsleiders en 8 contract-elites (alleen via het premiebord).
  - 3 sets (Bloesem, Schors, Wortel) en sieraden.
- **Bazen:**
  - **Kruinroofvogel** (wereldbaas, optioneel): grijpt spelers vast en vlucht. De laatste fase is een achtervolging op je rijdier.
  - **Kaaszeppelinwerf:** Taleggio (touwen), Brokkel (kaaswielen en kanonnen), Kapitein Roquefort (de zeppelin zinkt).
  - **Graaf Piepvel** (lv 42), de centrale generaal:
    - Hij draagt een cape van afgevallen muisjesvachtjes.
    - Kooien openen breekt zijn schild.
    - In de donkere fase steek je lantaarns aan.
    - Daarna grijpt een lange grijze arm van de Keizer hem weg (flash 1). De muisjes krijgen hun vachtje terug.
  - **Hart van de Boom:** de Wortelwurger (hartslag), de Oude Nestwachter (wordt weer goed), en Baron Bleu met de Schimmelkolos. Bleu grijpt naar een lok-ei.
  - **Finale Vahoegnir:**
    - Je zet zijn 5 hartsplinters terug (1 uit zone 2, 4 uit zone 3), en hij wordt weer roze.
    - Het ei komt uit: **Vahoegje**, een groeiend huisdier.
    - Zijn roze straal wijst naar Hoogvads, en daarna volgt de troonzaal van de Keizer (flash 2).
- **Verhaal en NPC's:**
  - 49 NPC's en 15 hoofdstappen (elke stap zegt wie, waar en wat).
  - 5 lichte zijquests.
  - Geheimen: 10 knuffels, 5 aanwijzingen, 5 schatten, 8 muurverhalen en de eerste Grauwe brief.
  - De geheime quest van de Mika-imker eindigt voorlopig in een lore-scène; de baas zelf is nog niet gebouwd.
- **Systemen:**
  - Level-cap 50.
  - Skillboom rij 6 en 7 (keystones).
  - 4e job-advancement op klasse-lv 50: Nestwachter, Kruinvalk en Schemerwijze.
  - 6 wapens, 4 nieuwe kristallen, een 3e sokje vanaf lv 45.
  - Premiebord, licht gehouden: 3 premies per dag, reset 06:00, 1 gratis wissel, Jachtpenningen-winkel, optioneel weekdoel, géén lockouts.
  - Vahoeg-kerkermodus.
  - **Gouden Veren:** gouden Pluimguh met 2 % kans, of gegarandeerd na 25 veren.
  - Honingkraam (Reuzen Vadsdrankje).
- **Huisdieren:** Kruinroofvogel, Piepje en Vahoegje.

## Waar op letten (jouw oordeel nodig)
- **Zone 3 is nog niet in een echte client gezien, behalve door de AutoCheck:**
  - het uiterlijk van de boom;
  - Honingtak;
  - de gebieden;
  - de bazenmodellen;
  - de muziek.
- **Balans:** alle getallen zijn eerste gokjes: xp-tempo (lv 30→50 in ~6-8 u), bazen, rijdiersnelheden en prijzen.
- **Nieuw geluid:** het stadsgeluid zonder wind en de muziek van Honingtak.
- **Kleine kuiltjes in zone 3:** een vijverkuil bij (2608,67,367) en ~15 kleine plekjes van 1-3 blokjes. Gebruik U als je vastzit, en meld de plek.
- **Labeltje:** bij de poort van de Schorsstad (y 74) staat "Het Hart van de Boom" als gebiedsnaam.
- **Tegelgrootte:** de kaarttegels samen zijn ~125 MB (zone 3 62 MB). Dat is groot voor het pack. Verder verkleinen vraagt ontwerpkeuzes.
- **Mika-imker:** de baas moet nog gebouwd worden (nu een lore-scène).
- **Engels:** de Engelse teksten van de zone-2/3-wiki en de FTB van zone 3 zijn nog Nederlands.

## Mappen
- `guhs_vadsrijk\k-int`: **de testclient van nu** (branch `110-k-int`, head ba501dd1c).
- Oudere testclients (niet meer nodig): `j-int`, `h-melee`, `g-int`, `f-int`, `w-poc`.
- Ontwerp: `guhs1100-avontuur\GUHS1100_AVONTUUR.md` (§9 alle besluiten, rondes 1-19), `ZONE3_PLAN.md`, `WERELDVERHAAL.md`, `VOORTGANG.md` (logboek van elke agent).
- Renders: `renders-vadsrijk`, `renders-mounts`, `renders-schemerwortel`.
