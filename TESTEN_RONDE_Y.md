# TESTEN ronde Y (Guhs 1.10.0 "Het Vadsrijk", testclient y-int)

Start: dubbelklik `guhs_vadsrijk\y-int\run-client.bat`, maak een **nieuwe** wereld (Creatief, cheats aan). De client staat op
alle zones open (openZones 6) en heeft FTB Essentials, net als de server. De commando's per punt staan in `SPIEKBRIEF.md`
hoofdstuk 1. Vink af wat klopt; schrijf erbij wat niet klopt (met de plek: F3-coördinaten).

Grensregels gelden niet voor ops. Test ze met `/guhs grenscheck start` eerst (dan tel je als gewone speler tot
`/guhs grenscheck rapport`), of met een tweede, niet-op account.

## A. Het FTB-hoofdstuk "Het Vadsrijk" (1.1)
- [ ] Het boek: hoofdstuk Het Vadsrijk, drie kopjes, 14 quests; elke beloning is één deco-blok (geen knabbels, geen xp).
- [ ] `/guhs vadsrijk zones open 1`: alleen rekruut, zone 1 en geheimpjes 1 zichtbaar. `open 2`: binnen 10 s "Zone 2 is open!"
      + de twee zone-2-quests. Daarna `open 6`.
- [ ] Claimen in het Vadsrijk: goud "Beloningen claim je in je eigen wereld, niet in het Vadsrijk.", niks gekregen, blijft claimbaar.
- [ ] Claimen thuis: het blok komt in je inventaris. In het Knuffelkwartier mag het ook.
- [ ] De rekruut-quest geeft het Rekruutvaantje + de Vadsrijk-vitrine.

## B. De pronkstukken (1.2)
- [ ] `/guhs vadsrijk ftb trofeeen`: alle 14 zien er goed uit (jouw oordeel), kijken naar je, de lampjes geven licht.
- [ ] In het Vadsrijk neerzetten lukt niet (melding boven de hotbar).

## C. De Vadsrijk-vitrine (1.3)
- [ ] Thuis neerzetten: "Je Vadsrijk-vitrine staat!", beeldje van je rijdier, bordje erboven (naam, klasse, level, badges).
- [ ] Rechtsklik met lege hand: overzicht in de chat; met een blok in je hand zet je dat blok ertegen.
- [ ] Poort 1: badges ① ? ? ? ? ?, geen zonenamen, geen raid-regel. Poort 6: alles binnen 5 s.
- [ ] In het Vadsrijk / Knuffelkwartier neerzetten lukt niet.
- [ ] Het glas over het beeldje, grote rijdiers door het glas? (jouw oordeel)

## D. De Garderobe (1.4)
- [ ] Thuis O of `/guhs garderobe`: titel "Garderobe", 8 tabs, geen Harnas-tab, de "alleen voor de looks"-regel.
- [ ] Knuffelorde-cape + Strikje thuis aan: zichtbaar in F5 (en voor een tweede speler).
- [ ] Vleugels thuis zijn alleen een look; met een echte elytra aan verdwijnen ze.
- [ ] Poort 1: geen vergrendelde stukken van zone 2-6 in de lijst.

## E. De scheiding (1.5)
- [ ] xp, effecten, hartjes en honger wisselen bij de grens en komen thuis precies terug (geen gratis genezing).
- [ ] Binnen: `/enderchest`, `/home`, `/back`, `/tpa`, `/spawn` geven "werkt niet in het Vadsrijk" (FTB Essentials 26.1.2.4 heeft geen `/ec`).
- [ ] Volle tas + item aan je muis bij `/guhs vadsrijk`: er valt niets op de grond.
- [ ] Een paard / dier aan een leidtouw blijft aan zijn kant.
- [ ] Binnen krijg je een survival-beloning (bv. Sherlock) pas thuis, niet in je Vadsrijk-tas.

## F. Je twee besluiten (1.7)
- [ ] Mollie (Knuffelkwartier, markt): alleen stoel, tafel, bank, zitzak, kussen, vlaggetjes, vaandel (6 + 1 op de tweede pagina).
      Geen planken, boekenkasten, lantaarns, bed, glas, tapijt, bloempotten, taart, bloemen meer.
- [ ] De Post brengt alleen nog deco en kleding thuis (ambacht-hoedjes, schortje, vr_eco-deco, vliegertje, hartjesbril).
- [ ] Binnen: `/open anvil`, `/open crafting`, `/open smithing`, `/open stonecutter` geven "werkt niet in het Vadsrijk"; thuis
      werken ze (alleen voor ops: gewone spelers hebben ze in FTB Essentials 26.1.2.4 niet).

## G. Het verhaal (Questlopen, hoofdstuk 0)
- [ ] `/guhs vadsrijk questlopen`: zone 1 helemaal doorklikken; de rekruut- en zone-1-quest in het FTB-boek vinken vanzelf af.

## Wat de automatische checks al zeiden (EINDRAPPORT_RONDE_Y.md)
De echte grens is in de client getest met `/guhs grenscheck` (AutoCheck, zie EINDRAPPORT): inventaris, xp, effecten, hartjes en
honger, een gevallen item bij de grens, `/ec`, `/home`, de werkbanken, FTB-claimen binnen/thuis en vitrine/pronkstuk neerzetten.
Wat je met je ogen moet beoordelen: hoe de pronkstukken, de vitrine en de Garderobe eruitzien.
