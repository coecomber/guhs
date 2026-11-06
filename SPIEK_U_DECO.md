# Spiekbrief U-DECO (ronde U: verlichting, Honingtak, arena-onderkant, wenteltrap, Schildpadeiland, gondel)

Alle commando's werken ook van buiten het Vadsrijk. Handig erbij: `/time set night` (verlichting) en `/time set day` (bijen).

## 1. Knuffelkamp: alle paden verlicht
Overal langs de paden van het kamp-eiland staat nu om de ~14 blokken een guh-lantaarnpaal (naast het pad, nooit erop),
zodat geen stuk pad 's nachts donker is. Niet alleen het aankomstpad maar ook de paden naar de vijver, het trainingsveld,
het tempeltje en de vuurtoren.
```
/time set night
/guhs vadsrijk wereld plek kampvuur_kamp
/guhs vadsrijk wereld plek aankomst
/guhs vadsrijk wereld plek tempeltje
/guhs vadsrijk wereld plek vijver
/guhs vadsrijk wereld plek training
/guhs vadsrijk wereld plek kampvuur_vuurtoren
```
Kijk: loop van het kampvuur naar elk van deze plekken; je zou steeds de volgende lantaarnpaal al moeten zien. Geen paal midden
op een pad of voor een tentdeur.

## 2. Honingtak: licht, straatmeubels en bijen
- Op de ringstraat (tussen de huizen en de stam) staat om de ~10 blokken een guh-lantaarnpaal, met daartussen om en om een
  bankje (kijkt uit over de straat) of een prullenbakje + een bloembakje.
- Op de buitenpromenade (waar die breed genoeg is) ook palen.
- Op het Honingplein: een kring van 4 palen en 4 bankjes rond de fontein, palen en prullenbakjes op de hoeken.
- Overdag vliegen er rond je (op het dek) gewone bijen en guhbijtjes (half/half, max 8) plus de honingbijtjes van zone 3. Ze
  steken nooit, worden nooit boos (ook niet als je ze slaat) en gaan niet in de korven; 's nachts en bij regen zijn ze weg.
```
/time set day
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk z3wereld plek markt
/guhs vadsrijk z3wereld plek gebouw.smid.deur
/guhs vadsrijk z3wereld plek gebouw.burgemeester.deur
/guhs vadsrijk udeco bijen
/time set night
```
Kijk: is het dek 's nachts goed verlicht? Staat er niets voor een deur of een kraampje? `udeco bijen` telt de bijen om je heen.
Sla er eens een: niks gebeurt, er steekt er geen een.

## 3. Kruinroofvogel-arena: geen platte plaat meer onder
De onderkant van de Kale Kruin is nu een organische, taps toelopende nestkom: diep in het midden, dun aan de rand, met een
grillige rand, gevlochten takken die naar een punt onder het midden lopen, en hangend bleek mos, hangende wortels,
gloeibessen-ranken en honingdruppels (raat + honingblok).
```
/guhs vadsrijk z3wereld plek honinglift_kruin_onder
/guhs vadsrijk z3wereld plek arena_kruinroofvogel
```
Kijk: vanaf Honingtak (eerste commando) omhoog kijken richting het noordwesten: geen vlakke plaat meer. Het tweede commando: de
arena zelf is hetzelfde gebleven (vlak dak, oprit vanaf kruin 1).

## 4. Uitkijkpunt `uitkijk_nest` en de wenteltrap in de boom
- Bovenop het nest staan het hekje langs de trapsleuf en de 2 lantaarns nu óp de nestvloer (ze zweefden 1 blok erboven).
- De lantaarns bij de deuren van de bordessen staan nu op een paaltje (ze zweefden naast het deurkozijn).
- Verdiepingsindicatie op de wenteltrap:
  - een gekleurde band op de stamwand net boven je hoofd die de trap volgt, in de kleur van de verdieping waar je naartoe
    klimt (bruin Schorsstad, oranje Honingtak, roze Kruin 1, magenta Kruin 2, paars Kruin 3, wit Het Nest);
  - op elk bordes is de band 2 rijen hoog, met aan beide uiteinden een banier in die kleur en een hangend lantaarntje;
  - boven elk bordes hangt een naambord: de naam (bv. **Honingtak**), "Verdieping 2 van 6" en waar je omhoog/omlaag uitkomt.
- De hekken zijn verbonden door de centrale verbindingspas van u-hekken (gemerged).
```
/guhs vadsrijk z3wereld plek trap_bordes_1
/guhs vadsrijk z3wereld plek trap_bordes_2
/guhs vadsrijk z3wereld plek trap_bordes_3
/guhs vadsrijk z3wereld plek trap_bordes_4
/guhs vadsrijk z3wereld plek trap_bordes_5
/guhs vadsrijk z3wereld plek trap_bordes_6
/guhs vadsrijk z3wereld plek uitkijk_nest
/guhs vadsrijk udeco borden
```
Kijk: loop een stuk van de trap: zie je steeds in welke kleur/verdieping je zit? Op elk bordes het naambord (Nederlands).
`udeco borden` zet de naamborden opnieuw neer als je er een mist. Bovenop het nest: geen zwevende lantaarns of hekjes meer.

## 5. Schildpadeiland (zone 4)
- De bloemetjes, de 2 lampionpaaltjes en de palm op de rug van de schildpad staan nu op de echte hoogte van het schild (ze
  stonden allemaal op de hoogte van de top van de koepel, dus 1 blok te hoog op de lagere plekken).
- Geheime knuffel 6 lag precies op Kluizenaar Schelpie (zijn plek op de rug); die heeft nu een eigen plekje 8 blokken verder
  op het schild (met een knuffelblokje).
- Elders in zone 4 ook rechtgezet: de magere knuffels in het Opvangcentrum zweefden 1 blok boven de vloer, en losse
  plantjes die zweefden waar later een pad/huis de grond weghaalde, worden nu opgeruimd.
```
/guhs vadsrijk z4wereld plek schildpad_rug
/guhs vadsrijk z4wereld plek geheim_knuffel_6
/guhs vadsrijk z4wereld plek gebouw.schildpadhutje.deur
/guhs vadsrijk z4wereld plek gebouw.opvangcentrum.binnen
```
Kijk: niets zweeft op het schild, Schelpie zit vrij (geen knuffel/hartjes op hem), de knuffels in het Opvangcentrum staan op
de vloer tegen de achterwand.

## 6. Gondel (zone 2) en andere aankomstplekken: geen mobs meer bij aankomst
Er spawnen geen mobs meer binnen 24 blokken van:
- beide gondelstations;
- elke luchtschiphaven (landing, bel, vertrekbord, plein);
- elke bootsteiger;
- de luchthaven-aankomst en de terugportalen;
- kampvuren en stadsranden (een spawner die zelf vlak bij een kampvuur staat, zoals in een kerker, houdt de oude 12 blokken).
```
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk udeco vrij
/guhs vadsrijk z2wereld plek gondel_berg
/guhs vadsrijk udeco vrij
/guhs vadsrijk z2wereld plek dok_landing
/guhs vadsrijk z4wereld plek haven_3_4_landing
```
Kijk: rondom de stations geen aanvallende mobs meer (mobs die al rondliepen kunnen nog even in de buurt zijn; wacht een minuut
of kom terug). `udeco vrij` laat de dichtstbijzijnde vrije plekken zien en of je erbinnen staat.
