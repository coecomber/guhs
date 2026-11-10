# SPIEK Y-FTB: het FTB-hoofdstuk "Het Vadsrijk" (compact) + claimen

Open het FTB-boek, hoofdstuk **Het Vadsrijk** (groep Guhs). Je ziet drie kopjes: De Knuffelorde, Het avontuur, Geheimpjes.

## 1. Wat zie je bij gate 1 (eerste release)?
```
/guhs vadsrijk zones open 1
/guhs vadsrijk ftb status
```
Verwacht in het boek: alleen **Word rekruut van De Knuffelorde**, **Zone 1 uitgespeeld** en **Alle geheimpjes van zone 1**
(plus "Hoe kom je hier?"). Geen zone 2-6, geen raid, geen zonenamen. Status toont per zone wat af is en "poort dicht".

```
/guhs vadsrijk zones open 2
```
Binnen 10 s: **Zone 2 is open!** (afgevinkt) + **Zone 2 uitgespeeld** + **Alle geheimpjes van zone 2** verschijnen.
Daarna weer `/guhs vadsrijk zones open 1` (wat al verscheen, blijft staan).

## 2. Een zone uitspelen (echte trigger: de laatste hoofdquest inleveren)
```
/guhs vadsrijk npc quest af hq12_schaduw
```
Verwacht: in het boek vinkt **Zone 1 uitgespeeld** zichzelf af (beloning: Knuffelzwamlampje).
Andere zones (laatste hoofdquest): `z2_hq12_hartsplinter`, `z3_hq15_hoogvads`, `z4_hq07_opstijgpier`, `z5_hq10_stormrand`,
`z6_hq10_moederoase`. Rekruut: `/guhs vadsrijk npc quest af hq05_klodderreus`.
Wat is de laatste hoofdquest van zone N: `/guhs vadsrijk ftb zone 3`.
Snel zonder quest: `/guhs vadsrijk ftb geef zone3` (of `rekruut`, `raid`, `geheimen1`, `poort2` ...). Alles terug: `/guhs vadsrijk ftb vergeet`
(de FTB-voortgang zelf reset je met `/ftbquests change_progress @s reset <quest>`).

Elk karakter telt: maak zone 1 met je linker ridder, wissel naar de Eénvads-ridder: zone 1 blijft afgevinkt.

## 3. Claimen: thuis wel, in het Vadsrijk niet
1. Vink zone 1 af (zie 2) terwijl je **in het Vadsrijk** staat. Open het boek, klik de beloning (of "claim all").
   Verwacht: goud bericht **"Beloningen claim je in je eigen wereld, niet in het Vadsrijk."** en je krijgt niets; de beloning blijft claimbaar.
2. Ga naar huis (overworld of Guhmensie), klik de beloning. Verwacht: het **Knuffelzwamlampje** in je inventaris.
3. Ook in het Knuffelkwartier (spelershuis) mag claimen (daar heb je je thuis-inventaris).
Dit geldt voor alle FTB-beloningen, ook van andere hoofdstukken.

## 4. De pronkstukken (in je eigen wereld)
Alles in één keer: `/guhs vadsrijk ftb trofeeen` (of uit de Guhs-creative-tab). Los:
```
/give @s guhs:vadsrijk_trofee_rekruut
/give @s guhs:vadsrijk_trofee_zone1
/give @s guhs:vadsrijk_trofee_zone2
/give @s guhs:vadsrijk_trofee_zone3
/give @s guhs:vadsrijk_trofee_zone4
/give @s guhs:vadsrijk_trofee_zone5
/give @s guhs:vadsrijk_trofee_zone6
/give @s guhs:vadsrijk_trofee_raid
/give @s guhs:vadsrijk_trofee_geheim1
/give @s guhs:vadsrijk_trofee_geheim2
/give @s guhs:vadsrijk_trofee_geheim3
/give @s guhs:vadsrijk_trofee_geheim4
/give @s guhs:vadsrijk_trofee_geheim5
/give @s guhs:vadsrijk_trofee_geheim6
```
Verwacht: Rekruutvaantje, Knuffelzwamlampje, Splinternaaldjes, Schemerwortellantaarn, Lampionvliegertje, Hoogvads-roosvenstertje,
Sterrentorentje, Sterrendovende trofee, 6x Geheimpjesdoosje (deksel in de zonekleur). Tooltip: "Beloning: Zone N van Het Vadsrijk
uitgespeeld" (enz.). Neerzetten in survival: kijkt naar jou; de lampjes geven licht. In het Vadsrijk neerzetten lukt niet
("Dit pronkstuk is voor je eigen wereld...").
De rekruut-quest geeft ook de **Vadsrijk-vitrine** (`guhs:vr_vitrine`, van slice y-vitrine; pas na de merge een echt item).
