@@ZONE 1
## Nieuw in ronde U: zone 1 (Knuffelkamp, Zwamzoet, Sporenwoud)
- **Knuffelkamp:** alle paden verlicht met guh-lantaarnpalen (u-deco).
```
/time set night
/guhs vadsrijk wereld plek kampvuur_kamp
```
- **De Reuzenhoed** (nieuw uitkijkpunt bovenop de tempelzwam van Zwamzoet): neem de tempel-lift en loop over het plankenpad
  naar het balkon.
```
/guhs vadsrijk indeling plek uitkijk_hoedtop
```
- **De Rustende Ridders-zaal** staat nu in de wereld, in Zwamzoet tussen de Kruidenier en de boulevard. De beelden komen er
  vanzelf in zodra de zaal geladen is.
```
/guhs vadsrijk karakter zaal tp
```
- **De Zwamgrot** (lv 11-13), onder het Sporenwoud. De ingang is een holle reuzenpaddenstoel met een wenteltrap. Binnen:
  Het Echolied (puzzel), de Oude Zwamwachter (elite), een schat en plukken + mijnen.
```
/guhs vadsrijk grot tp zwamgrot
/guhs vadsrijk grot tp zwamgrot puzzel
```
- **Twee rijdende guhs** in zone 1: de koerier Haven ↔ Zwamzoet en de hoofdweg Zwamzoet ↔ Sporenwoud. Klik erop.
```
/guhs vadsrijk ruiters tp z1_koerier
/guhs vadsrijk ruiters tp z1_hoofdweg
```
- **Specialisatie op lv 17:** de quest `spec17_<klasse>` bij de Grootmeester (vanaf lv 16, na de Druipgrotten).
- **Hoedenpaden:** elk hoed-plateau heeft nu een open ingang in zijn hekring, ook de hoed bij (-374, 103, 82), die was
  dichtgegroeid.
```
/execute in guhs:vadsrijk run tp @s -374 105 82
```
- **Kwartiermeester** (huizen) en een **Bondenbord** (Knuffelbond) op het plein van Zwamzoet, en een Bondenbord bij het
  kampvuur van het Knuffelkamp.

@@ZONE 2
## Nieuw in ronde U: zone 2 (Kandijkom, De Splinterpieken)
- **Gondel:** bij aankomst geen aanvallende mobs meer (24 blokken vrij rond beide stations).
```
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk udeco vrij
```
- **Geen dubbele Kristalwacht Splinter meer:**
```
/guhs vadsrijk npc tp z2_kapitein
/guhs vadsrijk dubbel scan
```
- **De Kristalgrot** (lv 22-24), in de Galmtop. De ingang is een kristallen boog in de westflank. Binnen: een ijsmeer om te
  vissen, De IJsspiegels (lichtpuzzel), de IJskoude Wacht (elite) en een schat.
```
/guhs vadsrijk grot tp kristalgrot
```
- **Vlaggetjes, slingers en tralies** in de Splinterpieken zijn verbonden (zie §1 bij U-HEKKEN).
- **Uitkijkpunt Galmtop** (hoogste naald, wenteltrap). De oude uitkijkpunten bij de havens zijn weg.

@@ZONE 3
## Nieuw in ronde U: zone 3 (De Schemerwortel, de Boom)
- **Honingtak:**
  - om de ~10 blokken een lantaarnpaal;
  - bankjes, prullenbakjes en bloembakken;
  - een kring rond de fontein;
  - overdag bijen en guhbijtjes (u-deco).
```
/time set day
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk udeco bijen
```
- **De wenteltrap in de Boom:** gekleurde banden per verdieping, banieren en lantaarntjes op de bordessen, en een naambord
  boven elk bordes. De hekken zijn verbonden.
```
/guhs vadsrijk z3wereld plek trap_bordes_2
/guhs vadsrijk z3wereld plek trap_bordes_5
```
- **Het Kruinnest** (`uitkijk_nest`): goede naam, één ring als reling, de lantaarns op de vloer.
```
/guhs vadsrijk indeling plek uitkijk_nest
```
- **De Kruinroofvogel-arena** heeft van onderen een organische nestkom (geen plaat meer). Er is altijd maar 1 Kruinroofvogel.
```
/guhs vadsrijk z3wereld plek honinglift_kruin_onder
/guhs vadsrijk z3bazen tp kruinroofvogel
/guhs vadsrijk dubbel scan
```
- **Rijdende guhs op Pluimguhs** (20 in zone 3):
  - over het Rijpad en de binnentrap;
  - rondjes op Honingtak;
  - over de takpaden;
  - bij de Pluimstal (met een verzorger);
  - op het oefenovaal en over de Wortelweg.
```
/guhs vadsrijk ruiters lijst
/guhs vadsrijk ruiters tp z3_wenteltrap
/guhs vadsrijk ruiters tp z3_honingtak
/guhs vadsrijk ruiters tp z3_pluimstal
```
- **De Wortelgrot** (lv 44-46), onder de Wortelvelden ten noorden van de stam. De ingang is een holle stronk naast de Wortelweg.
  Binnen: De Wortelknopen (schuifpuzzel), de Grote Wortelslang (elite) en een schat.
```
/guhs vadsrijk grot tp wortelgrot
```
- **Job-advancement 2 (lv 35):** bij Ordemeester Wasja in de Nestkapel, na de Kaaszeppelinwerf en de Pluimstal.

@@ZONE 4
## Nieuw in ronde U: zone 4 (De Lampionkust)
- **Schildpadeiland:** de bloemetjes, lampionpaaltjes en de palm staan nu op het schild, en geheime knuffel 6 zit niet meer
  op Schelpie.
```
/guhs vadsrijk z4wereld plek schildpad_rug
/guhs vadsrijk z4wereld plek geheim_knuffel_6
```
- **De Koraalgrot** (lv 53-55), via een **duik**:
  - het vlot met lantaarn staat bij de oostrand van de Koraalbaai;
  - onder water een koraalboog, een tunnel met 3 luchtbellen;
  - de droge uitgang is een koraalschoorsteen met een wenteltrap.
  - Binnen: Het Schelpenpad (puzzel), Koning Mopperkrab (elite) en een schat.
```
/guhs vadsrijk grot tp koraalgrot
```
- **Ambachten T4:**
  - verzamelplekken in zone 4;
  - koraalduiken (houd vast en laat los: "Duiken!");
  - lampionhoning;
  - T4-recepten ook in de werkplaats van Lampionhaven.
```
/guhs vadsrijk ambacht tp z4_duik_koraalbaai
```
- **Uitkijkpunten met een naam:** De Vuurtorengalerij, De Lampionduin, De Pierkop, De Vliegerheuvel.
- **Natuurlijke overgangen:** de diepe kom van zone 4 loopt golvend af naar de open zee, en de wolken van zone 3 dunnen uit.

@@ZONE 5
## Nieuw in ronde U: zone 5 (Hoogvads op Moeder Wolkwal)
- **Gebouwen met diepte (u-z5):**
  - de kathedraal is gotisch: steunberen, luchtbogen, pinakels, roosvenster, diepe portalen en ribgewelven;
  - de universiteit kreeg hoge gotische ramen en een bibliotheekgalerij;
  - het paleis, de raadszaal, de academie en het station kregen reliëf;
  - de hele stad kreeg vensterkappen.
- **Interieurs (u-z5binnen):**
  - echte ronde kroonluchters (een nieuw 3D-blok), met kaarsen en kristallen;
  - lichtere zalen met dambordvloeren, wandluchters, kandelaars en licht in de gewelfribben.
- **Figuranten** zien er weer uit als normale guhs.
- **De staartvin:** het pad vanaf de staartlift loopt nu door tot de Donderbuik-arena, de Staartvlakte en Mistmoeder (die 4 plekken
  waren onbereikbaar).
```
/guhs vadsrijk z5wereld plek kathedraal_schip
/guhs vadsrijk z5wereld plek universiteit_bibliotheek_galerij
/guhs vadsrijk z5wereld plek troonzaal_audientie
/guhs vadsrijk z5wereld plek z5w_lift_staart_onder
/guhs vadsrijk z5wereld plek z5w_donderbuik_arena
```
- **De Wolkengrotten** (lv 58-60) in de kop van de walvis. De ingang is Het Blaasgat voor de stadspoort. Binnen: Het Wolkenslot
  (symboolpuzzel), de Grauwe Wolkduivel (elite) en een schat.
```
/guhs vadsrijk grot tp wolkengrot
```
- **Ambachten T5 + cap 60:** alleen bij de leermeesters van het Ambachtsgilde. Hoogvads is de ambachtshoofdstad.
- **Knuffelbond oprichten:**
  - bij Bondsklerk Zegeltje (paleis, kanselarij), na zone 4 of vanaf lv 56;
  - de Bondenpoort in het Staartpark brengt je naar de Bondenvallei.
```
/bond
```
- **Job-advancement 3 (lv 58):** bij Kapitein Veerkracht (Academie), na de finale.

@@ZONE 6
## Zone 6 in één oogopslag (nieuw in ronde U)
Het Zijdeland is het eerste stuk van het grote continent: lv 60-70, roze duinen, oases, karavanen, bazaars en zwevende koepels.
Het verhaal is **De Sterrenkaravaan**: Hertogin Ringeling (de Ringvinger) dooft de sterrenbakens; jij steekt ze weer aan, vindt de
Moederoase en beklimt de raid **De Sterrendovende Toren**. Het levelplafond is nu **70**.

**Snel rondkijken:**
```
/guhs vadsrijk level 65
/guhs vadsrijk z6wereld plek haven_z6_z5_vertrek
/guhs vadsrijk z6wereld plek rozenkloof_schatkamer
/guhs vadsrijk z6wereld plek bazaar_kruising
/guhs vadsrijk z6wereld plek moederpalm
/guhs vadsrijk z6wereld plek toren_voet
```
**Hoe kom je er (verhaal):** zone 5 afmaken → Havenmeester Windvaan → de film "De grote zwemtocht" (Moeder Wolkwal zwemt met heel
Hoogvads naar het oosten) → lijn E (Staarthaven ↔ Rozenkloof) gaat open → quest "Land in zicht!".

**Wat er is (elk heeft hieronder een eigen blok):**
- **De kaart** (u-z6wereld): Rozenkloof (kloofstad, aankomst), Saffraanpoort (hoofdstad), 5 gebieden met elk een sterrenbaken,
  De Zingende Zoutgrot, De Verzonken Sterrenwacht (kerker), de toren. De Schatkamer is opnieuw gebouwd als diepe
  Petra/Al-Khazneh-gevel (vrijstaande zuilen, verdiept portiek, gebroken fronton, ronde tholos) en de kloofwanden hebben reliëf.
- **Het verhaal** (u-z6verhaal): 10 hoofdstappen, 15 zijquests, Ringeling wordt opgevrolijkt en bondgenoot.
- **Wezens en bazen** (u-z6wezens): 13 wezens, 2 veldbazen (Koningin Wormelien, de Zoutkolos), de kerker met 3 bazen,
  de **Bultguh** (rijdier met 2 zitplekken) en het **vliegende tapijt** (u-tapijt, zweverig en glijderig).
- **De raid** (u-z6raid): 2-10 spelers, 4 verdiepingen, schaalt mee; zie het raid-blok en de loot-tabel bovenaan.
- **Muziek en sfeer** (u-z6muziek, u-geroezemoes): eigen gebiedsthema's, stadsmuziek, bazenmuziek met fases, bazaar-geroezemoes.
- **Leven in de stad** (u-leven-stad, u-leven-ritme): ook in Saffraanpoort een speeltuin, terrassen, beestjes en een dag/nacht-ritme.
- **De grotkerker** (u-grotkerker): ook de Zoutgrot is een herhaalbare mini-kerker met eindbaas.

**Controleren dat het klopt:** `/guhs vadsrijk z6wereld gebied` (gebied, biome en levelband), en in survival van de luchthaven via de
Siq naar Saffraanpoort lopen: geen sprongen hoger dan 1 blok, geen gaten.
