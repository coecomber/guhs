## U-OVERGANG: natuurlijke zone-overgangen + epische uitkijkpunten

**Belangrijk:** dit werkt alleen in een **nieuwe Vadsrijk-wereld** (of op plekken waar nog geen chunks waren). Bestaande chunks
houden de oude zee en de oude biomen.

### 1. De naden tussen de zones
Wat er veranderd is:
- **Waterkleur, lucht en mist** verlopen nu geleidelijk over de hele zee tussen twee zones (8 stapjes), in plaats van een
  rechte lijn waar de kleur ineens omslaat. De zee rond de Boom (zone 3) is niet meer turquoise.
- **De zeebodem** sluit overal aan: geen onderwaterklif meer op de tegelgrens, geen ineens andere bodemkleur. De diepe kom van
  zone 4 loopt met een golvende rand af naar de open zee.
- **De wolkenzee van zone 3** stopt niet meer op een rechte lijn, maar rafelt uit en dunt uit in de open zee (ook aan de
  Splinterpieken-kant van de Wolkenkloof).
- **Losse rotsjes** steken hier en daar uit de open zee bij de kusten.

Kijk vooral van bovenaf (vliegen, of vanaf een hoge plek) en onder water. Druk **F3**: de biome-naam op open zee is
"Het Vadsrijk - De Zee".
```
/guhs vadsrijk indeling rand wolkenkloof_noord
/guhs vadsrijk indeling rand wolkenkloof
/guhs vadsrijk indeling plek haven_z2_z3_vertrek
```
- **Wolkenkloof:** de wolkbanken langs de kloof lopen door tot in zone 2 en waaieren uit. Geen rechte wolkenrand meer, en het
  water verkleurt langzaam van ijsblauw (zone 2) naar lila/honing (zone 3).
- **Onder het Galmdok** (`haven_z2_z3_vertrek`): kijk naar beneden en naar het oosten. De bodem loopt rustig door, geen
  rechte afgrond.

Meer naden om te bekijken (vlieg rond, kijk naar water, bodem en wolken):
```
/execute in guhs:vadsrijk run tp @s 1024 90 -1040
/execute in guhs:vadsrijk run tp @s 1730 90 -500
/execute in guhs:vadsrijk run tp @s 1400 90 -110
/execute in guhs:vadsrijk run tp @s 380 90 -1100
/execute in guhs:vadsrijk run tp @s 700 90 -740
/execute in guhs:vadsrijk run tp @s 2140 90 -250
/execute in guhs:vadsrijk run tp @s 2140 90 -780
/execute in guhs:vadsrijk run tp @s 2560 90 -420
/execute in guhs:vadsrijk run tp @s 900 90 -400
/execute in guhs:vadsrijk run tp @s -380 90 860
```
1. Zone 2 / zone 3, noordkant van de Wolkenkloof (x 1024).
2. Zone 3 / zone 4, de Wolkenval (x 1728): de wolkjes van zone 3 dunnen uit richting de Lampionkust.
3. Zuidrand van zone 3 (z -129): de wolkenzee rafelt uit.
4. Westkant van zone 2 (x 384).
5. Splinterwand / zone 2 (z -768).
6. Zuidrand van zone 4 (z -257).
7. Noordrand van zone 4 (z -768).
8. Oostrand van zone 4 onder Hoogvads (x 2560).
9. Open zee tussen zone 1 en zone 3 (hier staan losse rotsjes).
10. Zee tussen Knuffelkamp en zone 1.

### 2. Uitkijkpunten (W3): nu op epische plekken
Eerste keer bovenaan = panorama-filmpje + "uitkijkpunt gevonden" met de **naam** (geen rare tekst meer).
```
/guhs vadsrijk indeling plek uitkijk_hoedtop
/guhs vadsrijk indeling plek uitkijk_nest
/guhs vadsrijk indeling plek uitkijk_galmtop
/guhs vadsrijk indeling plek uitkijk_kroon
/guhs vadsrijk indeling plek uitkijk_vuurtoren
/guhs vadsrijk z5verhaal tp ooggaarde_balkon
```
- **De Reuzenhoed** (NIEUW, Zwamzoet): neem de **tempel-lift** omhoog. Op het voorplein van de tempel loopt een
  plankenpad met reling over de rand van de hoed naar een rond balkon op palen. Uitzicht over heel Zwamzoet en de zee.
  Test: lift op, pad af, balkon op, zonder springen.
- **Het Kruinnest** (bovenop de Boom): de naam klopt nu. De reling en de 2 lantaarns staan óp de nestvloer (niet meer
  zwevend), en de reling is één doorlopend hek.
- **Galmtop:** de hoogste naald van de Splinterpieken, met de wenteltrap omhoog.
- **De Kroon**, **De Vuurtorengalerij** (zone 4, wenteltrap in de vuurtoren; heeft nu een naam), **Ooggaarde-balkon** (Hoogvads).
- Zone 4 heeft nu ook namen voor **De Lampionduin**, **De Pierkop** en **De Vliegerheuvel**:
```
/guhs vadsrijk z4wereld plek uitkijk_lampionduin
/guhs vadsrijk z4wereld plek uitkijk_pierkop
/guhs vadsrijk z4wereld plek uitkijk_vliegerheuvel
```
- **Weg:** de uitkijkpunten bij de ballonhavens (Noordkliftop, Galmkam, Kandijkom-balkon, Westtak, Oosttak). De havens
  zelf zijn er nog gewoon.
