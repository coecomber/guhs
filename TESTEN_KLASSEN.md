# Testen: klassen (Het Vadsrijk, Guhs 1.10.0)

Dit stuk gaat over de Pluisjager, de Vadsmagiër, de skillbomen, de ordetempel (resetten en van klasse wisselen) en de job-advancement.

## Snel starten (dev)
- `/guhs vadsrijk proeftuin`: je staat in de proeftuin. De klodders lopen rond bij 3130 66 3120.
- `/guhs vadsrijk klasse kies`: het keuzescherm van het klasse-tempeltje. Kies bijvoorbeeld de Pluisjager. Je startwapen komt in je rugzak.
- `/guhs vadsrijk level 12`: snel naar niveau 12, zodat je 11 skillpunten hebt.
- `/guhs vadsrijk klasse wapen <type>`: een Zeldzaam wapen op jouw niveau in je hand. De types zijn:
  - Pluisjager: `pluisboog`, `druppelboog`, `snoepkruisboog`, `eikelkatapult`
  - Vadsmagiër: `rozenstaf`, `vlammenstaf`, `zwamstokje`, `slijmboek`
  - Knuffelridder: `klodderkling` (en kerns `kussenzwaard`, `knuffelknots`, `pluishamer`)
- `/guhs vadsrijk vol`: Vads, energie en je ultieme lading vol, afkoeltijden weg.

## Toetsen
| toets | wat |
|---|---|
| Linkermuis vasthouden | Schieten met een boog, kruisboog, katapult, staf, toverstok of boek. Een zwaard of knots mept gewoon. |
| R / V / G | Vaardigheden 1, 2 en 3 |
| Z | Je ultieme vaardigheid (laadt op door schade te doen) |
| K | Je skillboom (alleen in het Vadsrijk) |

## Wat moet je proberen
1. **Schieten.**
   - Houd met de pluisboog de linkermuis in. Je ziet pijlen echt vliegen, met een roze spoortje en een "tik" als ze raken.
   - De kruisboog schiet langzaam maar hard en gaat door 1 klodder heen.
   - De katapult gooit een boogje en ploft rond de inslag.
   - Een staf schiet een toverbol in de kleur van zijn element. Het toverstokje schiet snel en een beetje zoekend, het boek trager en sterk zoekend.
2. **Pluisjager.**
   - R Pluizenregen: er komt een roze cirkel en daarna 3 golven pijlen.
   - V Rolsalto: een salto achteruit, en je volgende schot is een gouden voltreffer.
   - G Knuffelpijl: schiet op een vriend om hem te genezen, of op een klodder voor schade.
   - Z Sterrenschot: 1 s opladen, dan een laserstraal dwars door alles heen.
3. **Vadsmagiër.**
   - R Roze Vuurbal: ontploft in het element van je staf. Probeer de vlammenstaf (brandt), de rozenstaf (roze) en het slijmboek (plakt).
   - V Blink: een korte teleport die netjes voor muren stopt.
   - G Vadsbron: een roze heal-cirkel op de grond.
   - Z Meteoorguh: een gigantisch guhblok valt uit de lucht en laat 6 s roze vuur achter.
4. **Skillboom (K).**
   - Klik een knoop één keer om hem te kiezen en nog een keer om hem te leren.
   - Van de twee R-varianten (bijv. Vuurkussen en Slijmkussen) kun je er maar één nemen.
   - Op niveau 20 heb je 19 punten. Dat is niet genoeg voor alles.
   - De tooltips vertellen precies wat een knoop doet. Probeer een paar knopen en kijk of je het ook echt merkt (bijv. Vuurregen, Drievuur, Jachtroes, Slijmkussen).
5. **Ordetempel.** `/guhs vadsrijk klasse balie` opent de balie van de ordetempel op de plek waar je staat.
   - Wissel van klasse: elke klasse houdt zijn eigen niveau en boom. De eerste keer krijg je een startwapen.
   - Reset je boom: dat is gratis, daarna moet je 1 uur (echte tijd) wachten. `/guhs vadsrijk klasse respecvrij` haalt die wachttijd weg.
6. **Job-advancement.**
   - Ga met `/guhs vadsrijk level 10` naar niveau 10, open de balie en klik op "✦ Ceremonie!" (of gebruik `/guhs vadsrijk klasse adv`).
   - De camera draait om je heen, er stijgen lichtstralen op en je krijgt een grote titelkaart met je nieuwe titel.
   - De titels zijn:
     - Knuffelridder: Kussenkampioen, daarna Vahoege Paladijn
     - Pluisjager: Sterrenschutter, daarna Vahoege Hemelvalk
     - Vadsmagiër: Sterrenwever, daarna Vahoege Aartsmagiër
   - Op niveau 20 volgt de tweede. Elke advancement geeft iets nieuws, bijvoorbeeld de Dubbelklap of de Saltosalvo. Je ziet het onderaan in de boom.
7. `/guhs vadsrijk klasse` laat je titel, niveau, vrije punten en knopen zien. `/guhs vadsrijk klasse reset` vergeet alles van de klassen (ook je eerste keuze).

## Let op
- Het klasse-tempeltje in Knuffelkamp en de ordetempel-NPC komen van de wereld- en npc-slices. Tot die er zijn, gebruik je de commando's hierboven.
- De wapenplaatjes zijn met code getekende pixel-art (een eerste versie).
