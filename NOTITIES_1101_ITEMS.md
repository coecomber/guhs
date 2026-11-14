# NOTITIES 1101-ITEMS: wat doe je met oude spullen? (Opruimen)

Branch `1101-items` vanaf `release-1.10.0` (9bd6a4052). Spiekbrief: `SPIEK_1101_ITEMS.md`. Taalchunk: `c470_vr_1101_opruimen`.

Vraag van de gebruiker: "Maak het duidelijker wat je met oude items kan doen die slechter zijn dan nieuwe items." Keuze: **ontmantelen + verkopen**.

## Wat er nu is
- **Ontmantelen** (nieuw). Vadsrijk-uitrusting wordt smeedmateriaal. Dat kan op drie plekken:
  - bij de smid (knop "Opruimen (n slechtere)" in de smidse);
  - met de knop **⚒** links naast je inventaris (alleen ontmantelen en op slot zetten);
  - met "Ontmantel alle slechtere" op het verkooptabblad van elke winkel die koopt.
- **Op slot zetten** (nieuw). Druk op **L** in je inventaris terwijl je muis boven een stuk staat, of klik het slotje in het Opruimen-scherm.
  - Opgeslagen als component `guhs:vr_vergrendeld` (KernRegistry.VERGRENDELD).
  - Een stuk op slot wordt nooit verkocht of ontmanteld. Het kan ook niet op de markt, niet in de ruil en niet in de gildebank (`EcoUtil.verhandelbaar`).
- **Tooltip**: bij elk Vadsrijk-stuk staan nu ook:
  - de winkelwaarde;
  - "Ontmantelen: 2x Smeedstofje";
  - het slotje, of de hint "L: op slot zetten" als je in een inventaris kijkt.
- **De vergelijkregel** van U-SPECS:
  - zegt nu "▲ Beter / ▼ Slechter dan wat je draagt";
  - zet het grootste verschil erachter, bijv. "(-12 Schade per klap)".
  - Wat je draagt wordt nu op één plek bepaald: `GearVergelijk.gedragen`. De client (tooltip) en de server ("alle slechtere") gebruiken die allebei.
- **"Verkoop alle slechtere" + "Ontmantel alle slechtere"**: in elke winkel die koopt (verkooptabblad) en in het Opruimen-scherm van de smid.
  - Ze pakken alleen tasstukken die slechter zijn dan wat je in dat vak draagt (`GearVergelijk`, -3 %).
  - Ze slaan over: stukken op slot, wat je draagt (hand, harnas, sieraden), stukken met sterren, met een kristal in een sokje en verpluisde stukken.
  - Eerst komt een venster met de aantallen: per grijs/groen/blauw+, plus de munten of het materiaal dat je krijgt.
- **"Verkoop alle grijze" blijft**. Het slaat nu wel stukken op slot en het wapen in je hand over.
- **Losse verkoop** in een winkel weigert nu ook stukken op slot en het wapen in je hand (`Opruimen.verkoop`).

## Materialen en opbrengst
Nieuwe items (EcoSpul, stapel 99, iconen in `textures/item/vadsrijk/economie/`):

| item | id | komt van | winkelwaarde |
|---|---|---|---|
| Smeedstofje | `vr_item_smeedstofje` | grijs (Gewoon) | 2 brons |
| Smeedsplinter | `vr_item_smeedsplinter` | groen (Ongewoon) | 10 brons |
| Smeedkristal | `vr_item_smeedkristal` | blauw, paars, oranje, roze | 40 brons |

**Hoeveel je krijgt** (`Opruimen.aantal`):

| zeldzaamheid | basis | per level | extra |
|---|---|---|---|
| Gewoon | 2 stofjes | +1 per 20 levels | +1 per 2 sterren |
| Ongewoon | 1 splinter | +1 per 20 levels | +1 per 2 sterren |
| Zeldzaam | 1 kristal | +1 per 25 levels | +1 per 2 sterren |
| Episch | 2 kristallen | +1 per 25 levels | +1 per 2 sterren |
| Legendarisch | 4 kristallen | +1 per 25 levels | +1 per 2 sterren |
| Guhddelijk | 8 kristallen | +1 per 25 levels | +1 per 2 sterren |

**Omsmelten bij de smid** (`Opruimen.OMSMELTEN`). Zo gaat het materiaal naar de sterren en de ambachten. Ik heb bestaande materialen hergebruikt: geen nieuwe receptregels, geen nieuwe smidkosten.

| in | uit | waarvoor |
|---|---|---|
| 3 Smeedstofjes | 1 Verbetersteentje | sterren |
| 1 Smeedsplinter | 1 Verbetersteentje | sterren |
| 1 Smeedsplinter | 1 Pluisdraad | repareren |
| 2 Smeedkristallen | 1 Glinstergruis (kwaliteit gewoon) | ambacht: sieraden, T5-gear, mijnhelm, mini-koning |
| 3 Smeedkristallen | 1 Herrolknabbel | herrollen |

**Waarom deze getallen.** Een Verbetersteentje kost 50 brons in de winkel, een Herrolknabbel 600.
- Grijs lv1 verkopen geeft 12 brons. Ontmantelen geeft 2 stofjes, ongeveer 33 brons aan steentjes. Vroeg in het spel is ontmantelen dus beter.
- Grijs lv10 verkopen geeft 48 brons tegen ongeveer 33 aan steentjes. Daar is verkopen beter.
- Groen lv10 verkopen geeft 96 brons. Ontmantelen geeft 1 splinter, ongeveer 50 brons.
- Blauw lv10 verkopen geeft 192 brons. Ontmantelen geeft 1 kristal, ongeveer 1/3 knabbel (200).
- Zo is er een echte keuze: munten of materiaal. Er is geen geldlus: de materialen verkopen ver onder hun bron.
- De bestaande verkoopwaarden, smidprijzen en droprates heb ik **niet** aangeraakt (balanspas).

## Regels: wat nooit weg kan
- Alleen Vadsrijk-uitrusting (`Rollen.van != null`). Survival-items, materialen en munten worden geweigerd (`geen_uitrusting`).
- Alleen tasvakken 0-35, en niet het geselecteerde hotbarvak (dat wapen draag je). Harnas en schildhand (36-40) en sieraden zijn "gedragen".
- Stukken op slot worden geweigerd (`vergrendeld`).
- Om bevestiging vragen doen: blauw en beter, stukken met sterren en stukken met een kristal in een sokje (`Opruimen.kostbaar`). De server weigert zonder `ok=true` (`bevestig`).
- De acties checken alles eerst en veranderen pas daarna, in één tick.
- De dimensiecheck zit bij de aanroepers: `Opruimen.doe`, `Winkels.doe` en de payload. Daardoor zijn de regels in een gametest te testen.

## Tutorial: hq03c "Opruimen" (Knuffelkamp)
- De keten is nu hq03b (Opa Sterrenstof) → **hq03c_opruimen** → hq04 (kampvuur).
  - Opa Sterrenstof stuurt je nu eerst naar Sergeant Pluizebol op de steiger (zijn laatste regel is aangepast).
  - hq04 heeft nu `na = hq03c`.
- De sergeant legt het in 3 regels uit en geeft je bij het aannemen 2 "oude oefenwapens" (actie `opruimspullen`): Gewoon, lv1, wapen van je klasse, zonder bonussen. Die zijn slechter dan het hq03-wapen en het startwapen.
- Doelen:
  - `meld opruimen:verkocht` = verkoop een oud stuk in het **Kampwinkeltje** van Kok Knapperkorst (nieuwe winkel `kampwinkel`: Klein vadsdrankje + Verbetersteentje, zelfde prijzen als de stad).
  - `meld opruimen:ontmanteld` = ontmantel een oud stuk in de **Kampsmidse** van Sergeant Pluizebol (opent Opruimen in smid-modus).
- Inleveren bij de kok, die daarna meteen hq04 geeft.
- Beloning: 40 xp, 0 munten (je krijgt al de verkoopprijs). Bewust klein. De balanspas mag dit aanpassen.
- NPC-keuzes: `QuestMotor.EXTRA` voor "kok" (Kampwinkeltje) en "rekruteerder" (Kampsmidse), gekoppeld in `NpcRollen.koppelAlles`.
  - QuestMotor toont de extra keuzes nu ook in het "hoe gaat het"-gesprek van een NPC zonder dienstrol. Anders kon je ze tijdens de quest niet bereiken.
- **Bestaande spelers**:
  - `Questlog.INGEVOEGD` laat hq03c tellen als klaar voor iedereen die hq04 al gestart of gehaald heeft. Er wordt niks in hun data geschreven.
  - Wie hq03b af heeft maar hq04 nog niet heeft aangenomen, zit nog in de tutorial en krijgt de nieuwe stap.
- De questtracker en Questlopen wijzen naar de kok of de sergeant (`QuestStap`, `QlPlek`).
- Gids-pagina "Wat doe ik met oude spullen?" (`gids.opruimen`, staat in QuestlogScherm.GIDS na "sterren"). Tip `tip.opruimen` verschijnt bij het aannemen.

## Wiki
- `tools/wiki_site/vadsrijk/systemen2.py` (pagina Uitrusting 2.0) heeft een nieuwe sectie "Oude spullen: verkopen of ontmantelen": de opbrengsttabel, de omsmeltrecepten, de knoppen en het kampwinkeltje/de kampsmidse.
- `blokuitleg.py` hoeft niet: de smid en de winkels zijn NPC's, geen blokken.
- De nieuwe items komen in de itemdatabase via de normale export.
- De wiki is niet opnieuw gebouwd en niet gepubliceerd.

## Tests
`economie/VadsrijkOpruimenGameTests` (batch `vadsrijk_opruimen`, `-Pgt=VadsrijkOpruimen`):
- `opbrengstPerZeldzaamheid`: materiaal en aantal per zeldzaamheid, level, sterren, bevestiging voor blauw, omsmelten.
- `weigertGedragenVergrendeldSurvival`: hand, gedragen helm, diamanten zwaard, slot. Ook voor Verkoop alle grijze, markt en ruil.
- `slechtereSelectie`: alleen de slechtere stukken zonder slot en zonder sterren. Een leeg vak telt niet. Samenvatting en betaling kloppen.
- `tooltipVergelijk`: de gedeelde `GearVergelijk.gedragen` (hand, wapen van een andere klasse, leeg vak, zwakste ring) en de tooltipwaarde = winkelwaarde.
- `tutorialOpruimen`: volgorde hq03b → hq03c → hq04, bestaande spelers niet geraakt, de quest met de hand uitgespeeld.
- `questrobotZone1`: de questrobot speelt zone 1 en haalt hq03c en hq04.

Uitslagen (2026-11-14):
- `-Pgt=VadsrijkOpruimen,VadsrijkEconomie,VadsrijkSpecs,VadsrijkQuestLopen,VadsrijkNpc,VadsrijkQuestRobot`: alles groen. Alleen `VadsrijkNpcGameTests.hoofdlijnHeeftAltijdWieEnWaar` telde nog 13 hoofdquests in zone 1. Die test is aangepast naar 14 (2 asserts).
- De herhaalde run `VadsrijkNpc,VadsrijkOpruimen`: 13/13 groen. Daarnaast `VadsrijkTaal,Taalsleutels,VadsrijkTOpslag,VadsrijkItems,VadsrijkMenu`: 22/22 groen.
- Questrobot zones 1-6: zone 1 42 gehaald, zone 2 19, zone 3 24, zone 4 23, zone 5 24, zone 6 25. Overal 0 vast en 0 niet gestart.
- `tools/vadsrijk/questrobot/statisch.py`: 157 quests, 0 bevindingen (ook de MELD-bron "opruimen").
- Taal: `engelscheck.py` geeft OK (na `--merk`), `taalcheck.py` geeft 0 ontbrekend, `apply_en.py --no-ftb` geeft 0 nog Nederlands.

## Gedeelde bestanden (voor de merge)
- `kern/KernRegistry.java` (+VERGRENDELD).
- `economie/{EcoSpul,EcoPayloads,EcoUtil,EconomieRegistry,Smid,Winkels}.java`; client: `EcoScherm` (bevestigvenster), `WinkelScherm`, `SmidScherm`, `EconomieClient`.
- `klassen/spec/GearVergelijk.java`, `klassen/spec/client/SpecClient.java`.
- `npc/{NpcRollen,QuestMotor,Questlog,QuestStap}.java`, `npc/client/QuestlogScherm.java`, `questlopen/QlPlek.java`. Let op: 1101-fix werkt ook in npc (escorte).
- `tools/features/vadsrijk_npc.py` (hq03b-regel, hq03c, hq04.na, TIPS/GIDS) en `data/guhs/vadsrijk/npc/quests.json`, + `advancement/quest/vr_hq03c_opruimen.json`.
- `tools/features/vadsrijk_u_specs.py` (de 2 vergelijkteksten).
- Lang: `nl_nl.json`/`en_us.json`, `tools/lang/chunks.json` (+c470), `tools/lang/en/c470_vr_1101_opruimen.json`, `c300_vr_u_specs.json` en `c401_vr_npc_quests.json` (3 waarden), `tools/vadsrijk/engels_bron.json` (--merk).
- **Let op met de generator.** `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_npc.py` los draaien:
  - gooit ~270 sleutels van andere generators weg (zone 2/3-opties, amb_smid ...);
  - verwijdert de `vr_ftb_*`-advancements.
  - Ik heb de generator gedraaid, daarna alleen mijn eigen wijzigingen overgenomen en de rest teruggezet. Bij een volledige `make_resources.py` gaat het goed.

## Wat nog open staat
- Niet in een client gezien. Nog te checken: de layout van het Opruimen-scherm, het bevestigvenster, de knop **⚒** en de L-toets.
- Bestaand en niet van mij: de knoppen ⚔ (karakter) en ⚑ (bond) staan allebei op y+44 naast de inventaris. Ze overlappen elkaar. Mijn ⚒ staat op y+88.
- In de recepten zelf telt Smeedkristal niet als Glinstergruis. Het loopt via omsmelten. Daardoor bleven de recepten, de website-export en de ambachttests ongemoeid.
- De xp van hq03c (40) mag de balanspas aanpassen. In `X_ZONE1_XP` staat hij niet.
