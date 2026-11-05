# WERELDINDELING W-3: de wereld hangt aan elkaar (T-WERELD)

De Vadsrijk-wereld is opnieuw ingedeeld: Knuffelkamp linksonder, zone 1 in het midden, zone 2 (Kandijkom) erboven naar het
noordoosten, zone 3 (de Boom) rechts daarvan, zone 4 (eilandjes) en Hoogvads nog verder naar rechts.
**Maak een NIEUWE wereld** (of gooi `dimensions/guhs/vadsrijk` weg): oude stukken wereld houden de oude kaart.

Handig:
- `/guhs vadsrijk indeling zone`: in welke zone sta je, sta je in "vrij gebied", en welke barrière duwt je terug.
- `/guhs vadsrijk indeling barriere uit` / `aan`: de terugduw-barrières uit/aan (om rond te zwemmen/vliegen tijdens het testen).
- Barrières werken NIET in creatief. Test ze in `/gamemode survival` (of adventure).

---

## W1. De overgangen bekijken (de randen tussen de zones)
```
/gamemode survival
/guhs vadsrijk indeling rand splinterwand
```
- Je dobbert in zee onder de **noordkliffen van zone 1**. Voor je (noord) staat **de Splinterwand**: een rij spitse rotsen en
  blauwe kristalnaalden, 90-145 blokken hoog, met gaten ertussen waardoor je zone 2 ziet liggen.
- Zwem naar het noorden: je wordt **zachtjes teruggeduwd** (bubbels + spatjes) en op de actiebalk staat
  "De Splinterwand is te steil en de wind te hard...". Nergens een zichtbare muur.
```
/guhs vadsrijk indeling rand splinterwand_west
/guhs vadsrijk indeling rand hoog_wind
```
- `hoog_wind`: je hangt hoog boven de wand. Pak een elytra / vlieg met een rijdier: de **windmuur** duwt je terug en omlaag
  ("Een harde windmuur blaast je terug...").
```
/guhs vadsrijk indeling rand wolkenkloof
/guhs vadsrijk indeling rand wolkenkloof_noord
```
- **De Wolkenkloof** (zone 2 -> 3): een kronkelend kanaal open water tussen dikke wolkenkliffen. Links (west) de oostkust
  van zone 2 die in wolkenbanken uiteenvalt, rechts (oost) de wolkenzee rond de Boom. Zwem/vaar oost of west: terugduwen
  met "De Wolkenkloof is te diep en te breed...".
```
/guhs vadsrijk indeling rand wolkenval
```
- **De Wolkenval** (zone 3 -> 4): de wolkenbanken rond de Boom worden dunner en vallen uit elkaar, daarachter open zee naar
  Lampionhaven. Terugduwen: "De Wolkenval en het tij duwen je terug...".
```
/guhs vadsrijk indeling rand zoetezee
```
- **De Zoete-Zeestroom** tussen Knuffelkamp en zone 1: open zee; zwem je te ver, dan duwt de stroming je terug.
```
/guhs vadsrijk indeling rand z2_zuidkliffen
/guhs vadsrijk indeling rand z2_oostkust
/guhs vadsrijk indeling rand kust_oost_z1
/guhs vadsrijk indeling rand kust_zw_z1
```
- **Natuurlijke kusten**: nergens meer een recht, vierkant eiland. Zone 1 heeft kapen en baaien (een baai bij de Klodderweide,
  een schiereiland in het zuidwesten naar het kamp, een kaap in het noorden). Zone 2 heeft steile ijskliffen in het zuiden en
  een afbrokkelende oostkust. De wolkjes langs de oude vierkante rand zijn weg.
- **Geen zwarte leegte** meer in zee: tussen de zones is overal zee (dieper naar de verte, ondiep rond zone 2).
- Barrières zijn er alleen tussen de zones: rond je eigen zone zwemmen kan gewoon (een stuk de zee in).

## W2. De havens (vertrekplatforms met vertrekbord)
Het luchtschip zelf (dienstregeling, meevliegen) is van T-LUCHTSCHIP. Hier: de plekken.
```
/guhs vadsrijk indeling plek haven_z1_z2_vertrek
```
- **De Noordklifhaven** (zone 1, op de noordkliffen): een pleintje met lantaarns, een lange steiger over de klif, een platform
  met reling, een **meerpaal met guh-vlag** en een **donker vertrekbord** op palen. Lopend bereikbaar: een nieuw pad begint
  aan de noordkant van de **fossielbrug** (over de Kloof) en klimt rustig (geen sprongen) naar het noorden, de klifrand op.
  Loop het eens helemaal: `/guhs vadsrijk wereld plek haven_z1_z2_plein` en dan het pad zuidwaarts terug naar de fossielbrug.
```
/guhs vadsrijk indeling plek haven_z2_z1_vertrek
/guhs vadsrijk indeling plek haven_z2_z3_vertrek
```
- **Kandijkom-dok** (zone 2, de bekende pier): er staat nu ook een **vertrekbord** op de pier.
- **Het Galmdok** (zone 2, op de zuidoostkaap): pleintje + steiger naar het oosten, platform, meerpaal met blauwe vlag,
  vertrekbord. Lopend: het pad langs de warme bron (Galmhellingen) loopt door naar het dok.
```
/guhs vadsrijk indeling plek haven_z3_z2_vertrek
/guhs vadsrijk indeling plek haven_z3_z4_vertrek
```
- **Weststeiger** en **Oosttaksteiger** (zone 3, op de takken van Honingtak): die bestonden al; ze hebben nu de gedeelde namen.
```
/guhs vadsrijk indeling plek haven_z4_z3_vertrek
```
- **Lampionhaven** (zone 4) wordt door T-Z4WERELD gebouwd; zonder hun kaart kom je op de geplande plek in zee uit.

## W3. De uitkijkpunten bij de overgangen
Eerste keer op het punt = het panorama-filmpje + "uitkijkpunt gevonden".
```
/guhs vadsrijk indeling plek uitkijk_noordkliftop
/guhs vadsrijk indeling plek uitkijk_kandijkom_balkon
/guhs vadsrijk indeling plek uitkijk_galmkam
/guhs vadsrijk indeling plek uitkijk_westtak
/guhs vadsrijk indeling plek uitkijk_oosttak
```
- **Noordkliftop**: hoek van het Noordklif-platform; je kijkt over de Splinterwand naar Kandijkom.
- **Kandijkom-balkon**: eind van de Kandijkom-pier; je kijkt naar het zuiden, naar de noordkust van zone 1.
- **Galmkam**: hoek van het Galmdok-platform; je kijkt oost over de Wolkenkloof naar de Boom.
- **Westtak / Oosttak**: de uitkijkjes bij de steigers van Honingtak (zone 3).
- Kijk ook vanuit Zwamzoet naar de horizon: Kandijkom (kristalspitsen) staat nu in het noordnoordoosten, de Boom in het
  oostnoordoosten met Hoogvads erachter.

## W4. Zone 1 + Knuffelkamp opnieuw testen (alles staat op een nieuwe plek)
Het kamp is **384 blokken naar het westen** geschoven; zone 1 zelf bleef staan (alleen de kust en het noorden veranderden).
```
/guhs vadsrijk
```
- Nieuwe wereld: je komt aan op de **zuidsteiger van het kamp** (nu rond x -384, z 1130). Trap omhoog de klif op, kampvuur,
  ordetent, training, tempeltje, vijver (Klodderreus-arena), vuurtoren: alles gewoon, alleen verder naar het westen.
```
/guhs vadsrijk wereld plek aankomst
/guhs vadsrijk wereld plek kampvuur_kamp
/guhs vadsrijk wereld plek ordetent
/guhs vadsrijk wereld plek arena_klodderreus.deur
/guhs vadsrijk wereld plek kampvuur_vuurtoren
/guhs vadsrijk wereld plek terugportaal_kamp
/guhs vadsrijk npc tp kapitein
/guhs vadsrijk npc tp visser_dobber
```
- Het **luchtschip kamp -> De Haven** (kapitein): de vlucht buigt nu van het westen naar de Haven; het schip aan anker en de
  wolkenbanken staan langs de nieuwe route. Kijk of de vlucht niet door land of rotsen gaat.
- Questvolger in het kamp: wijst hij naar de goede plekken (rekruteerder, kok, priesteres)? En de BAM (de draak over het kamp).
- Zone 1 binnenland is niet verplaatst: `wereld plek plein`, `wereld plek haven_plein`, `wereld plek arena_koning_klodder.deur`,
  `wereld plek uitkijk_druipspits`, `wereld plek uitkijk_klodderkijk` werken als vroeger. Let op de **nieuwe kust**: staan alle
  kampvuren, uitkijknaalden en de Haven nog op het land? Is het noorden (onder de Zwamtempel) nu een kale, besneeuwde
  klifrand?

## W5. Zone 2 opnieuw testen (de hele zone ligt nu op +704, +1344)
Zone 2 is 704 naar het oosten en 1344 naar het zuiden geschoven (Kandijkom rond x 704, z -1056).
```
/guhs vadsrijk z3npc klaarzetten
/guhs vadsrijk z2wereld vlieg kandijkom
/guhs vadsrijk z2wereld plek plein
/guhs vadsrijk z2wereld plek dok_landing
/guhs vadsrijk z2wereld plek arena_galmwolf_deur
/guhs vadsrijk z2wereld plek deur_spiegelburcht_voor
/guhs vadsrijk z2wereld plek arena_boormeester_deur
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk z2wereld plek brug_3_zuid
/guhs vadsrijk z2wereld plek meer_eiland
/guhs vadsrijk npc tp z2_kapitein
```
- De openingsfilm naar Kandijkom (storm, naald, wolf) en de film terug: camera op de goede plek?
- **Spiegelburcht**: puzzels (lichtkamer/prismazaal), de spiegelgang en Vrouwe Spiegelzucht staan mee verschoven. Doe één
  puzzel en de baas.
- Galmwolf, Boormeester, het Mika-fort, de gondel, de bevroren knuffels onder het ijs (Kleine Vlokje), de draak op het zadel.
- Wezens spawnen alleen in zone 2 (niet in zee); geen wezens in Kandijkom / bij de kampvuren.
- Biomen: `F3` in Kandijkom = kandijkom, in de Galmhellingen = galmhellingen.
- Kandijkom-stadsmuziek klinkt alleen in Kandijkom.

## W6. Zone 3 (stond al op zijn plek; nu met zee eromheen)
```
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk indeling plek uitkijk_nest
```
- Rond het wortel-eiland ligt nu **zee met een wolkenzee erop**; onder het eiland geen leegte meer. Kijk of nergens water in
  de wortelgrotten/het Hart staat (`z3wereld plek` naar `hart_poort`) en of de wortelrand niet half onder water staat.

## W7. Trappencheck (worldgen)
- Zie NOTITIES_T_WERELD.md, "Trappenrapport": welke trappen de checker nog meldt. Loop er een paar na met de coördinaten daar.
