# Notities KAART-slice (Guhs 1.9 "Een opknapper!")

Branch `190-opk-kaart`. Alles in `feature/erfenis/kaart/**` + `tools/features/erfenis_kaart.py` + stubs (zie onder).

## Wat er af is

### NPC-dorpjes volgens de regel van de gebruiker (`KaartLeven`)
- Eerste speler: 3 dorpjes (afwijking -1, 0, +1 Hoeve-level). Elke volgende speler: 1 dorpje (afwijking -1/0/+1 uit het zaad).
- Elk dorpje onthoudt wie het volgt (`NpcDorp.volgt` + `afwijking`) en loopt elke groeitik (4 min, dev: 15 s) één
  Hoeve-level richting `spelerlevel + afwijking` (begrensd 1..10). Lager dan het doel (reset-base) = direct opnieuw plannen.
- Plek: op de dichtstbijzijnde ring vanaf 3 cellen rond de speler, en altijd minimaal **3 cellen (Chebyshev)** van elke andere
  base (POC: buurcellen, iconen lagen over elkaar). Op de kaart is dat ~42 px bij de standaardzoom.
- Geen willekeurige nieuwe dorpjes meer (de oude `npc nieuw` is weg); wel nog gezellige groei binnen het level + nieuws.
- Plannen gebruiken nu de 1.9-ids en maten uit het contract en de aantallen uit balans 2.3 (`KaartGebouwType`), max level =
  Hoeve-level (override 1), Mammoetwei 3 levels. Niet-vierkante gebouwen krijgen alleen rotatie 0/2 (dan hoeft de kaart niet
  te weten hoe de kern een oneven draai van een rechthoek doet). Knusheid: 2 per level, de Hoeve 10 per level (deco.md).
- **Migratie** van een POC-wereld (`KaartData.versie` < 2): een oud dorpje op een goede plek (3..6 cellen van een speler, met
  ruimte) wordt geadopteerd en opnieuw gepland op het doel-level; de rest gaat van de kaart en hun NPC-base uit ErfenisData.
  Spelersbases worden nooit aangeraakt. De blokken van verwijderde dorpjes blijven in hun (verre) cel staan.

### Bestellingen (`Bestellingen`, `Bestelling`, `BestelGoed`, `BestelData`)
- Buren van een speler = de 3 dichtstbijzijnde NPC-dorpjes (eerste speler: zijn eigen drie). Lager/gelijk/hoger = level van
  dat dorp t.o.v. de speler (factor 2,5 / 3 / 3,5; ster 4).
- Karretjes = Marktplein-level: geen Marktplein 2, L1-3 3, L4-5 4, L6-7 5, L8-9 6, L10 7 (balans 3.4 op 10 levels).
  Per buur 1-3 (een nieuwe bestelling gaat naar de buur met de minste). Wachttijd na thuiskomst: 10/10/10/10/10/12/15/17/20/22/25 min.
- Cyclus per kar: HEEN (reistijd per tier: 2 / 3-4 / 4-6 / 5-7 / 6-8 min) -> OPEN (verloopt nooit) -> TERUG -> WACHT -> nieuw.
  Ook na uren offline worden alle fases netjes ingehaald. De allereerste karretjes staan er meteen (tutorial).
- Weigeren: kar rijdt leeg terug, komt na de normale wachttijd met iets anders.
- Sterbestelling: 1 per dag (servertijdzone), groot (x1,6 waarde, max soorten), x4, gouden karretje, gegarandeerd decostuk.
  Geleverd = morgen pas een nieuwe. Geweigerd = na de wachttijd een andere ster.
- Inhoud (balans 4+5): tier = Hoeve-tier van de speler; eerst 1 goed uit de eigen tier, rest uit alles t/m die tier;
  waarde-doel uit de Ƒ-band van de tier / 3; aantallen "mooi" (boven 20 veelvoud van 5); te veel stuks (planken!) wordt
  omlaag geschaald tot het max van de tier. Beloning = echte waarde x factor, afgerond op 5. Hoger dorp: 50% kans op 1 soort
  uit de volgende tier, alleen als je die al hebt (`heeft > 0` als "kan maken"). Nooit kaasknabbels/glasscherven/hoofdwereld.
- Leveren: alles eerst gecheckt, dan `ErfenisEconomie.neem` per goed, `geef(..., "guhlden", ...)`, deco-kans 3/8/15%,
  moment `bestelling_geleverd:<totaal>`. `Bestellingen.geleverd(server, uuid)` geeft het totaal (voor Hoeve-doelen/FTB).
- Grappige teksten: "Knabbeldorp wil 12 eieren voor de vadse paasbrunch." (20 redenen, 8 ster-redenen; eigen meervoudsnamen
  `erfenis.kaart.goed.*`, los van kern's `guhs.erfenis.goed.*`).
- Scherm `BestelScherm`: kaartjes in een raster (ster eerst), heb/nodig in groen/rood, beloning, hulpbonus, cadeau/kans,
  Leveren/Weigeren, voortgangsbalkje voor rijdende karren. `Bestellingen.open(ServerPlayer)` voor kern's Marktplein-knop;
  vanaf de kaart met "Terug naar de kaart".
- Kaart: stippelweggetjes naar je buren, rijdende karretjes (heen en terug, wieltjes draaien), geparkeerde karretjes onder je
  dorp, badge met aantal open bestellingen + gouden ster op de buren, regel in het paneel en de tooltip.

### Handje helpen (`Handje`)
- Knop op de kaart bij ECHTE spelers (niet jezelf, niet NPC) i.p.v. "Aanvallen". Keuze: "Sneller bouwen" (15 min, via
  `ErfenisHulp.versnel`, kern bewaakt max 25%) of "Bestelling +20%" (eerste open/aankomende bestelling zonder hulp).
- Eens per dag per dorp. Helper: 20 Ƒ x eigen tier + 1 burenstempel, alleen voor de eerste 5 dorpen per dag.
- Stempels: 10 -> Burenhulp-bloempot, daarna elke 25 (35, 60, ...) het volgende burenstuk (bankje, vlaggetjes; daarna
  vlaggetjes opnieuw).
- De geholpene krijgt "X hielp je een handje!" meteen, of bij de volgende login (opgeslagen).

## Stubs (lead vervangt bij de merge)
- `feature/erfenis/ErfenisEconomie.java` — STUB-kern, voorraad alleen in geheugen.
- `feature/erfenis/ErfenisMomenten.java` — STUB-kern, alleen `melding` (leeg).
- `feature/erfenis/ErfenisHulp.java` — STUB-kern: `int versnel(MinecraftServer, UUID eigenaar, int minuten)` geeft de
  echt gewonnen minuten terug (0 = niks bezig). Stub werkt op de POC-bouwer (`Base.bouw`), max 25%.
- `feature/erfenis/ErfenisDeco.java` — STUB-kern: `geef(MinecraftServer, UUID, String decoId)`. Niet in het contract;
  kern moet dit (of iets gelijks) aanbieden voor de decoschuurtje-lijst.

## MERGE-punten (alles in `KaartBronErfenis`)
- `hoeve(Base)` = nu `stadhuisLevel()` (POC 1..5) -> kern's Hoeve-level.
- `marktplein(Base)` zoekt een gebouw met id `marktplein`.
- `plan(NpcDorp)`: zolang de POC-GebouwType geen 1.9-ids kent, wordt de Hoeve als POC-stadhuis gestempeld en vallen de
  andere types weg. Met kern's GebouwType mapt alles op id.
- `npcWeg`: haalt een NPC-base via reflectie uit ErfenisData (geen remove in de POC). Graag `ErfenisData.verwijder(UUID)`.
- **Spelersplek**: kern's `ErfenisData.vrijeCel()` zet spelers in buurcellen (spiraal). Dan kan speler 2 naast een
  NPC-dorpje van speler 1 landen (afstand < 3). Advies: `KaartLeven.spelerPlek(bezetteCellen)` gebruiken (spiraal op een
  rooster van 3 cellen, met ruimte). De gametest doet dat ook.
- Lang: oude POC-sleutels (`erfenis.kaart.gebouw.stadhuis` enz.) staan nog in de json (het script werkt alleen bij);
  niemand gebruikt ze meer.

## Keuzes waar de docs zwijgen
- "Verversen na een paar uur" vs. "verloopt nooit": nooit verlopen wint. Een open bestelling blijft staan tot je levert of
  weigert; verversen = na levering/weigering komt de kar na de wachttijd met iets nieuws.
- Zonder Marktplein 2 karretjes (tutorial: eerste bestelling vóór Hoeve 2).
- Bestel-deco (deco.md, bron "Bestelling"): mika_schrik (H4), knabbelbessenstruik (H5), de_eerste_knabbel (H5, alleen
  hoger dorp), kaassausfonteintje (H7); ster ook mammoetje_van_heg (H9, vaker). Onder H4 geen gewone deco; de ster geeft dan
  toch de Mika-schrik (gegarandeerd is gegarandeerd). Deco-ids zelf gekozen, vormen moet ze kennen.
- Burenstukken na de bloempot: burenhulp_bankje, burenhulp_vlaggetjes (nieuw, staan niet in deco.md; vormen/kern: als die
  er niet komen, mag de bloempot herhalen).
- "Eens per dag" = kalenderdag in de tijdzone van de server.
- Een NPC-dorpje dat geen buur van jou is: "Bestellingen" grijs met uitleg.

## Testen in de dev client (Nederlandse commando's)
- `/guhs erfenis kaart` (toets M) — kaart met je dorp + je 3 buren, weggetjes, karretjes onder je dorp.
- `/guhs erfenis kaart bestellingen` — het bestelscherm (ook via een buurdorp -> "Bestellingen").
- `/guhs erfenis kaart bestel snel aan` — karren rijden/rusten 10 s. `bestel vul` — genoeg spul voor alle open bestellingen
  (stub-opslag). `bestel goed <id> <n>` — los goed erbij. `bestel ster` — de sterbestelling van vandaag opnieuw.
- `/guhs erfenis kaart npc groei [n]`, `npc snel aan|uit` — dorpjes volgen je level / groeien.
- `/guhs erfenis kaart hulp reset` — vandaag opnieuw helpen. Handje helpen zelf: met een tweede speler (LAN) op de kaart
  diens dorp kiezen -> "Handje helpen".
- Gametests: `gradlew runGameTestServer -Pgt=erfenis_kaart` (7 tests). AutoCheck: `tools/autocheck/190_opk_kaart.txt`.

## PAUZE (2026-11-03 ~13:15) — waar ik ben
- Alles hierboven is geschreven en compileert (`compileJava` groen).
- Gametests (`-Pgt=KaartGameTests,ErfenisGameTests`): 13 van 14 groen. Rood: `bestellingenEnBeloningen` —
  "T4 equal orders pay about balans 4 (346)": T4/T5-bestellingen betalen te weinig, omdat de max-stuks-klem (MAX_N 50/60)
  bij goedkope goederen (plank 0,3, steen/zand 0,5) de waarde omlaag duwt. Fix-idee: goedkope goederen (< 1) in T4/T5 weren
  of hun gewicht verlagen, of eerst de dure eigen-tier-goederen vullen. Daarna opnieuw de gametests.
- Nog te doen: die fix; AutoCheck `tools/autocheck/190_opk_kaart.txt` draaien en de screenshots bekijken (kaart met karretjes,
  bestelscherm) en zo nodig layout bijwerken; definitieve commit (deze is WIP).
