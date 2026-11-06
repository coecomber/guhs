# Spiekbrief: Het Vadsrijk na de nachtronde T (zone 1 t/m 5 + alle systemen)

Dit is de lange spiekbrief van de nachtronde (2026-11-05/06). Alles wat 's nachts gebouwd is, zit in één testclient:
**`guhs_vadsrijk\t-int\run-client.bat`**. Daarin staan de wereldindeling (alle zones aan elkaar), zone 4 "De Lampionkust",
zone 5 "Hoogvads" en alle nieuwe systemen.

**Volgorde van deze spiekbrief:** eerst starten en de nieuwe wereld, dan zone 1 → 5, dan de systemen. Elk blok test één
onderdeel: copy-paste de commando's in de chat en kijk of gebeurt wat erbij staat.

## Starten
- Dubbelklik `run-client.bat`. Maak een **nieuwe** wereld (Creatief, cheats aan); de saves-map is leeg.
- `/guhs vadsrijk` om erin te gaan.
- Wil je geraakt kunnen worden: `/gamemode survival` (in creatief vallen de wezens je niet aan).
- De wereld is verschoven: het Knuffelkamp ligt 384 blokken westelijker, zone 2 ligt nu ten noordoosten van zone 1, zone 3 ten
  oosten daarvan, dan zone 4 en daarboven zone 5. Alle teleportcommando's gebruiken plek-namen, dus die kloppen vanzelf.

## Handig tussendoor
- `/guhs vadsrijk vol`: Vads, energie en ult vol, cooldowns gewist.
- `/guhs vadsrijk info`: je data.
- `/guhs vadsrijk level 60`: het level-plafond is nu **60** (was 50). Zone 4 is lv 50-56, zone 5 lv 56-60.
- `/guhs vadsrijk tips reset`: alle tips opnieuw.
- Overal: `/guhs vadsrijk wereld plek <id>` (zone 1), `/guhs vadsrijk z2wereld plek <id>`, `/guhs vadsrijk z3wereld plek <id>`,
  en voor zone 4/5 de commando's in hun eigen blok hieronder.

## Toetsen (definitief na de nachtronde)
Alle Vadsrijk-toetsen werken alleen in het Vadsrijk. Je vindt ze in Opties > Besturing, categorie Vadsrijk.

| toets | wat |
|---|---|
| R / V / G | vaardigheid 1 / 2 / 3 |
| Z | ultieme vaardigheid |
| C | knuffelsprong |
| U | losknuffelen (vast? naar een veilig plekje) |
| K | skillboom |
| J | questlog |
| B | Verkenningsboek |
| X | rijdier: op- en afstappen (op het water van zone 4: je zeilbootje) |
| N | rijdieren-scherm (aankleden, Zeilbootje-tab) |
| P | huisdieren-scherm |
| **Y** | **Ambachten** (nieuw, t-ambacht) |
| **I** | **Knapzak** (nieuw, t-opslag) |
| **O** | **Kledingkast** (nieuw, t-kleding) |
| **Alt+1 / Alt+2 / Alt+3** | **drankjesriem** vak 1 / 2 / 3 (nieuw, t-items; Alt ingedrukt houden + cijfer, je hotbar-vakje wisselt dan niet) |
| F9 / F10 | Erfenis (bovenaanzicht / kaart), buiten het Vadsrijk |
| H | Vadskunst (Guheinde), alleen in het Guheinde |
| (geen) | questvolger aan/uit, lichtstraal aan/uit: zelf een toets geven als je wilt |

Om te checken: druk Alt+1 met een drankje in vak 1 van de riem. Je drinkt het, en je hotbar springt **niet** naar vak 1.
Als dat toch gebeurt: zeg het, dan gaat de riem terug naar Y/I/O en schuiven Knapzak/Kledingkast naar [ en ].

## Integratie (t-int): wat er bij het samenvoegen is aangesloten
Kort testen; elk punt is een koppeling tussen twee nachtslices.
- **Klodderreus + VFX:** `/guhs vadsrijk models reus vecht`. Bij zijn sprong duikt hij in elkaar (wind-up, hij blijft gehurkt tot
  de sprong), bij het landen een "pop". Bij een fase-wissel steigert hij, als hij verslagen is springt hij blij rond.
  (`/guhs vadsrijk models reus anim vfx_juich` speelt het los.)
- **Zeilbootje ↔ zone 4:** de steigers zijn nu de echte `boot_dok_*` van de kaart (11 stuks, ook Pierkop, Schildpad en het Wrak).
  Aanmeren met Shift bij een steiger. De Kringstroom van de boot is dezelfde als die van de kaart.
- **Boot in het verhaal:** z4_hq02 geeft je echt het bootje. Botenguh Zeiltje (de steiger-NPC) opent het bootscherm.
- **Ambachten ↔ Knapzak:** verzamelde materialen gaan in je **materiaaltas** (I), recepten pakken eerst uit je inventaris, dan uit de tas.
- **Vissen in zone 4:** 3 visplekken (visplek Lampionhaven, de steigers, de Koraalbaai). Elke vangst telt mee voor het verhaal (z4_vis).
- **Hoogvads = hoofdstad van de ambachten:** de leermeesters in het Ambachtsgilde (koken, brouwen, smeden, naaien) openen hun
  werkplaats; de imker en de Gildemeester openen het Ambachtenboek. Lampionhaven's werkplaats-NPC opent de smidse.
- **Kluis (bank):** ook bij de bankier in Lampionhaven en bij Raadsheer Gouddraad (aan de bankbalie in Hoogvads).
- **Eén cast-balk:** een baas die een aanval laadt toont één balk onder de baasbalk (die van t-vfx). De questvolger schuift mee omlaag.
- **Drankjesriem-HUD:** rechts naast de hotbar, met A1/A2/A3 erboven; de drink-cooldown staat ook in de buff-rij.
- **Rijdier-beloningen uit zone 4:** zijquest "Lampionnen" (z4_s14) → de **Lampion-Pluimguh**; zijquest "Strandavond" (z4_s15) → de
  **Duinhupper**.
- **Kledingkast ↔ zone 4:** de Lampion-vleugels (Verkenningsboek zone 4 op 100 %) en de Lampionspeld (50 %).
- **Karakters:** de knapzak is per karakter, kluis en materiaaltas zijn van je account; mount-uiterlijk en het bootje ook.
  Eénvads: flauwvallen in een scene of in de Proefarena (sparren in Hoogvads) laat je ridder NIET met pensioen gaan.
- **Geen zweven** in het Wrak, de Buikgangen, het Schuilnest, de Proefarena, de Droomwolk en in elke baasarena.
- **Luchtschepen:** de havens van zone 4 en 5 zijn de echte havens van de kaart. Lijn D (Opstijgpier → Hoogvads) gaat pas open na het
  zone-4-verhaal (vlag `zone4_klaar`). Lijn A vliegt nu hoog over de Splinterwand-pieken. Lijn 0 (kamp ↔ De Haven) is nog uit:
  die havens hebben nog geen eigen steiger (de oude scènevlucht blijft).
- **De Grauwe Droom** speelt (nog) het thema van de Vahoegnir; een eigen slaapliedje-baasmuziek komt later.
- **Zone 4 heeft geen vast blokken-luchtschip meer** bij Lampionhaven: het echte schip van lijn C legt daar aan.
- **Oma Dobber:** het schildpadeiland is Oma Dobber (zij slaapt al honderd jaar); Ouwe Dobber zwemt rondjes om haar heen.
  Schelpie vertelt het nu zo.

---


# 1. START EN DE NIEUWE WERELD

Eerst: kijk hoe alles aan elkaar hangt (havens, barrières, de zee tussen de zones), dan de luchtschepen en je karakters.


## WERELDINDELING W-3: de wereld hangt aan elkaar (T-WERELD)

De Vadsrijk-wereld is opnieuw ingedeeld: Knuffelkamp linksonder, zone 1 in het midden, zone 2 (Kandijkom) erboven naar het
noordoosten, zone 3 (de Boom) rechts daarvan, zone 4 (eilandjes) en Hoogvads nog verder naar rechts.
**Maak een NIEUWE wereld** (of gooi `dimensions/guhs/vadsrijk` weg): oude stukken wereld houden de oude kaart.

Handig:
- `/guhs vadsrijk indeling zone`: in welke zone sta je, sta je in "vrij gebied", en welke barrière duwt je terug.
- `/guhs vadsrijk indeling barriere uit` / `aan`: de terugduw-barrières uit/aan (om rond te zwemmen/vliegen tijdens het testen).
- Barrières werken NIET in creatief. Test ze in `/gamemode survival` (of adventure).

---

### W1. De overgangen bekijken (de randen tussen de zones)
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

### W2. De havens (vertrekplatforms met vertrekbord)
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

### W3. De uitkijkpunten bij de overgangen
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

### W4. Zone 1 + Knuffelkamp opnieuw testen (alles staat op een nieuwe plek)
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

### W5. Zone 2 opnieuw testen (de hele zone ligt nu op +704, +1344)
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

### W6. Zone 3 (stond al op zijn plek; nu met zee eromheen)
```
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk indeling plek uitkijk_nest
```
- Rond het wortel-eiland ligt nu **zee met een wolkenzee erop**; onder het eiland geen leegte meer. Kijk of nergens water in
  de wortelgrotten/het Hart staat (`z3wereld plek` naar `hart_poort`) en of de wortelrand niet half onder water staat.

### Nieuwe coördinaten (voor `/tp`, oude → nieuw)
| plek | oud | nieuw |
|---|---|---|
| aankomst (zuidsteiger kamp) | 0 65 1130 | -384 65 1130 |
| kampvuur kamp | 0 87 1025 | -384 87 1025 |
| kampsteiger (luchtschip kamp) | 151 87 1015 | -233 87 1015 |
| Kandijkom plein | 0 91 -2412 | 704 91 -1068 |
| Kandijkom-dok | 0 91 -2244 | 704 91 -900 |
| Galmwolf-deur | 146 115 -2318 | 850 115 -974 |
| Spiegelburcht-deur | 150 176 -2629 | 854 176 -1285 |
| Boormeester-deur | -30 66 -2559 | 674 66 -1215 |
| Noordklifhaven (nieuw) | - | 150 127 -653 |
| Galmdok (nieuw) | - | 980 117 -856 |
| Weststeiger / Oosttaksteiger (zone 3) | - | 1201 167 -601 / 1607 165 -488 |

Alle andere zone-2-plekken: oud + (704, 0, 1344). Zone 1 (behalve het kamp) en zone 3 zijn niet verschoven.

### W7. Trappencheck (worldgen)
- Zie NOTITIES_T_WERELD.md, "Trappenrapport": welke trappen de checker nog meldt. Loop er een paar na met de coördinaten daar.


## Luchtschepen (dienstregeling) + verre schimmen + zichtafstand (T-LUCHTSCHIP)

**Wat is er nieuw?**
- Grote guh-luchtschepen vliegen nu echt. Elke lijn heeft twee schepen die elke ~3 minuten vertrekken (aan beide kanten tegelijk, ze passeren elkaar halverwege). Een vlucht duurt 60-90 seconden.
- Je staat met maximaal 16 spelers op het dek en kunt vrij rondkijken. Tijdens de vlucht kun je er NIET af (sneaken geeft "Njeg!") en kan niets je pijn doen.
- Elke haven heeft een zwevend vertrekbord (3 regels) met aftelling. Sta je bij de pier (binnen ~9 blokken), dan zie je de vertrektijd ook in de actiebalk.
- De oude reiskaart-vlucht (kapitein / scheepsbel) blijft, maar is nu **luxe**: de eerste keer naar een plek is gratis, daarna kost het 400 brons. Het grote schip is altijd gratis.
- Verre **schimmen** (silhouetten) van De Boom, de Splinterpieken, de Kaaszeppelinwerf en Moeder Wolkwal met Hoogvads staan op hun ECHTE plek, ook ver buiten je renderafstand. Ze vervagen vanzelf zodra het echte ding geladen is.
- De Vadsrijk-dimensie heeft een eigen **zichtafstand** van 20 chunks (instelbaar tot 32), zonder server.properties aan te raken. Je eigen render-instelling blijft de bovengrens.

**De lijnen** (havens zijn nu nog plaatshouders tot t-wereld de POI's bouwt):

| Lijn | Van | Naar | Slot (heenweg) |
|---|---|---|---|
| 0 | Kampsteiger (-235, 87, 1008) | Haven Zwamzoet (92, 65, 500) | kampverhaal (zoals de oude bel); vliegt pas vanzelf als t-wereld eigen pieren voor lijn 0 heeft gebouwd (daar ligt nu het oude blok-luchtschip). Testen kan wel: `rij 0` |
| A | Noordklifhaven (150, 121, -600) | Kandijkom-dok (704, 91, -900) | zelfde als de kapitein naar Kandijkom |
| B | Galmdok (955, 111, -835) | Weststeiger (1190, 171, -590) | zone2_klaar + lv 30 |
| C | Oosttaksteiger (1630, 171, -500) | Lampionhaven (1800, 71, -470) | zone3_klaar |
| D | De Opstijgpier (2515, 73, -565) | Walvisrughaven (2630, 263, -520) | vlag zone4_klaar (finale zone 4) |

De terugweg is altijd open. In creative geldt geen slot en mag je wel uitstappen.

**Snel testen** (alles in de dev-client):

```
/guhs vadsrijk luchtschip lijnen
```
Toont per lijn: lengte, vluchtduur, vertrek over m:ss, en waar beide schepen nu zijn ("(er)" = het schip bestaat nu als entity).

```
/guhs vadsrijk luchtschip rij A
```
Je staat op de Noordklifhaven, het schip ligt klaar, je staat al aan boord, en over 8 seconden vertrekt het.
- **Kijk naar:** het vertrekbord boven de pier telt af. De actiebalk zegt "Aan boord naar Kandijkom-dok - vertrek over ...".
- **Bij vertrek:** een scheepsbel en "Daar gaan we!". Probeer te sneaken: "Njeg! Blijf lekker aan boord".
- **Onderweg:** ~85 s langs de Splinterwand. Kijk rond. De propellers draaien, de ballon deint, en het tweede schip komt je halverwege tegemoet (iets opzij).
- **Bij de landing:** het schip draait bij, je staat op de pier van Kandijkom-dok en ziet de titel "Kandijkom-dok - Geland met het luchtschip".

Andere lijnen en de terugweg:
```
/guhs vadsrijk luchtschip rij B
/guhs vadsrijk luchtschip rij C
/guhs vadsrijk luchtschip rij D
/guhs vadsrijk luchtschip rij 0
/guhs vadsrijk luchtschip rij A terug
```

Zelf instappen, zoals een speler dat doet:
```
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
/guhs vadsrijk luchtschip vertrek A 60
```
Je staat bij de pier en het schip vertrekt over 60 s. Klik met rechts op het schip om in te stappen. Zolang het nog ligt, kun je met sneaken weer uitstappen (je komt dan terug op de pier).

**Vertrek forceren / landen / alles terug:**
```
/guhs vadsrijk luchtschip vertrek C 5
/guhs vadsrijk luchtschip land
/guhs vadsrijk luchtschip reset
```
- `vertrek <lijn> [s]`: verschuift de klok van die lijn.
- `land`: zet je meteen af op de bestemming.
- `reset`: zet alle verschuivingen terug naar de echte dienstregeling.

**Randgevallen om te proberen:**
- **Uitloggen tijdens de vlucht:** log opnieuw in. Je staat op de pier van de bestemming.
- **Server stoppen tijdens de vlucht** (dev-client afsluiten midden in de lucht): na het opnieuw laden sta je op de bestemming, met de melding "Je vlucht is netjes afgemaakt".
- **Doodgaan** (`/kill`) aan boord: je spawnt gewoon bij je kampvuur, en de vlucht is vergeten.
- **Creative:** je mag uitstappen waar je wilt.

**Luxe reiskaart:**
```
/gamemode survival
```
Klik op een scheepsbel of praat met een kapitein, en kies een plek op de reiskaart:
- de eerste keer: "Luxe vlucht! Deze eerste keer trakteert de kapitein";
- de tweede keer naar dezelfde plek: 400 brons, of de melding "Een luxe vlucht kost 400 brons...".

**Schimmen (silhouetten):**
```
/guhs vadsrijk luchtschip schimmen altijd
/guhs vadsrijk luchtschip haven haven_z4_z3_vertrek
```
- `altijd`: alle schimmen op volle sterkte, ook als het echte ding dichtbij is.
- Vanaf Lampionhaven zie je De Boom achter je (west) en Moeder Wolkwal met Hoogvads voor je (oost).

Daarna weer op normaal, en de standaard-renderafstand:
```
/guhs vadsrijk luchtschip schimmen aan
```
- Zet je client-renderafstand op 12 en kijk vanaf de Noordklifhaven naar het noordoosten: de Splinterpieken staan er als schim.
- Zet de renderafstand op 32 en kom dichterbij: de schim vervaagt en het echte land neemt het over.
- De oude vaste plaatjes aan de horizon (kristalspitsen, boom, Hoogvads) zijn weg, behalve de vulkaan.

Helemaal uit (om te vergelijken):
```
/guhs vadsrijk luchtschip schimmen uit
```

**Zichtafstand van de dimensie:**
```
/guhs vadsrijk luchtschip zicht
/guhs vadsrijk luchtschip zicht 32
/guhs vadsrijk luchtschip zicht 20
/guhs vadsrijk luchtschip zicht 0
```
- Zonder getal: toont de huidige waarde. Met een getal: zet die meteen, en ook in `config/guhs-vadsrijk-zicht.properties`.
- `0` = uit: de dimensie gebruikt dan de gewone serverafstand.
- **Kijk naar:** zet je client op 32 en `zicht 32`. Vanaf Strandfeesteiland zie je dan de kruin én de walviskop echt.
- Andere dimensies blijven op de gewone afstand. Ga naar de Guhmension en terug: je client krijgt telkens de juiste radius.

**Tests:**
```
gradlew runGameTestServer -Pgt=VadsrijkLuchtschip
```


## Karakterslots + spelmodi (T-KARAKTER)

Je hebt nu tot **3 ridders** per account. Elke ridder heeft eigen levels, klasse-levels, skillbomen, Vadsrijk-inventaris
(+ harnas en sieraden), quests, verhaalfilmpjes, verkenningsboek, zonevlaggen/deuren, kampvuur en Vadsmunten.
**Gedeeld** (accountbreed): rijdieren (met hun levels), huisdieren, titels, de kleding-post, de bank/materiaaltas/kledingkast
(zodra die er zijn) en de tips die je al zag. Je gewone (buiten-)inventaris is natuurlijk ook gewoon van jou.

Spelmodi (kies je bij het maken, kan later alleen MAKKELIJKER):
- **Normaal**: zoals altijd.
- **Eénvads** (gouden hartje ❤ bij je naam): één leven. Flauwvallen BUITEN een stad = je ridder gaat met pensioen.
  Hij wordt niet gewist: hij krijgt een standbeeld in de **Rustende Ridders-zaal** in Zwamzoet, speelt verder als Normaal
  en krijgt een gouden sterretje ✦. Zolang hij leeft: +10% xp.
- **Knuffelmodus** (roze wolkje ☁): monsters doen maar de helft schade bij jou, je krijgt 10% minder xp.
- Geen IJzerguh.

### Snel testen (dev-commando's, OP nodig)
```
/guhs vadsrijk karakter
```
Opent het scherm "Je ridders" (mag iedereen). Drie kaarten: naam + badge, modus, level, klasse, munten; knoppen
Spelen / Naam wijzigen / Makkelijker... Een lege kaart: "Nieuw karakter" -> naam intikken, modus kiezen, "Maken en spelen".
Kijk: buiten een stad/kampvuur is "Spelen" grijs met de rode uitleg eronder.

```
/guhs vadsrijk karakter lijst
```
Alle slots in de chat: naam, modus, level, munten, welke actief is, en de sleutel ("uuid/slot").

```
/guhs vadsrijk karakter maak 1 normaal Pluisridder
/guhs vadsrijk karakter wissel 1
```
Maakt slot 1 en wisselt erheen (dev-wissel mag overal). Wat moet gebeuren: kersenbloesempjes, je staat bij de Knuffelkamp-aankomst
met een LEGE Vadsrijk-inventaris, level 1, 0 munten, en het openingsfilmpje start (nieuwe ridder = nieuw verhaal).
Titel "Pluisridder - Een nieuw avontuur begint!". Je rijdier (X), huisdier (P) en titels zijn er nog wel.

```
/guhs vadsrijk karakter wissel 0
```
Terug naar je eerste ridder: je staat weer precies waar je die achterliet, met al je spullen, levels, quests en munten.

```
/guhs vadsrijk karakter maak 2 eenvads Dappere Dirk
/guhs vadsrijk karakter wissel 2
/guhs vadsrijk karakter sterf-test
```
Eénvads: gouden ❤ voor je naam (tablijst, chat, boven je hoofd). `sterf-test` = meteen met pensioen: na ~3 s de titel
"Met pensioen!", een chatbericht voor iedereen, je naam krijgt ✦, en `lijst` toont "met pensioen" + modus normaal.
`sterf-test echt` = echt flauwvallen (gaat alleen met pensioen als je NIET in een stad bent).

```
/guhs vadsrijk karakter zaal tp
```
Teleport naar de Rustende Ridders-zaal in Zwamzoet (west van het plein, op het grasveld naast de Kruidenier, ingang aan de
zuidkant bij de havenpromenade, rond -27, 64, 76). Kijk: roze bakstenen hal met kersenhouten dak, glas-in-lood, hartjestegels,
een open ingang van 3 breed, een klinkerpaadje naar de promenade, en binnen standbeelden (pantserstandaard met het hoofd van de
speler, roze harnas, gouden zwaard, naambordje "✦ Naam lv X Klasse - Speler").
- Staat hij er niet? `/guhs vadsrijk karakter zaal bouw` bouwt hem op de vaste plek, `zaal bouw hier` op je eigen plek.
- `zaal test` voegt een nep-ridder toe (beeld erbij), `zaal lijst` toont ze, `zaal leeg` haalt alle beelden weg.

```
/guhs vadsrijk karakter modus 1 knuffel
```
Zet de modus van slot 1 (dev mag ook omhoog). In het scherm kan het alleen omlaag ("Makkelijker...").
Knuffelmodus: laat een klodder je slaan: de rode schadegetallen zijn de helft.

```
/guhs vadsrijk karakter naam 1 Sir Pluis
/guhs vadsrijk karakter scherm
/guhs vadsrijk karakter migratie-test
```
`scherm` = het scherm zoals bij binnenkomst (met "Verder als ..."). `migratie-test` doet alsof je data een oude save is en
controleert dat slot 0 precies hetzelfde blijft: allemaal "OK" verwacht.

### In het echt (zonder commando's)
- Praat met de **Grootmeester** (ordetempel Zwamzoet), de **Rekruteerder** (Knuffelkamp) of een **kampwacht** bij een kampvuur:
  keuze "Ik wil een andere ridder spelen" opent het scherm.
- Wisselen mag: in een stad, binnen 8 blokken van een kampvuur, of in de eerste minuut na binnenkomst. Niet in een baasgevecht
  of tijdens een filmpje.
- Heb je 2+ ridders, dan krijg je bij binnenkomst in het Vadsrijk het scherm met "Verder als <naam>".
- Met maar 1 ridder krijg je 1x een tip dat je er 3 kunt hebben.

### Gametests
`-Pgt=VadsrijkKarakter` (migratie, slotIsolatie, accountBreed, modiEnPensioen, namen).


# 2. ZONE 1 EN ZONE 2: COMPLETE HERTEST (de kaart is verschoven)

Het Knuffelkamp schoof 384 blokken naar het westen en zone 2 ligt nu op (+704, +1344) van zijn oude plek. Alle oude testblokken
hieronder werken met plek-namen, dus ze kloppen vanzelf op de nieuwe kaart. Loop ze allemaal nog een keer door: let vooral op
de overgangen (havens, de Splinterwand, de zee rond de eilanden) en of NPC's, kampvuren en arena's op hun plek staan.


## De belangrijkste plekken met hun NIEUWE coördinaten

### Knuffelkamp en zone 1 (Zwamzoet)

| plek | waar (x, y, z) | commando |
|---|---|---|
| aankomst | -384, 65, 1130 | `/guhs vadsrijk wereld plek aankomst` |
| steiger_eind | -233, 87, 1015 | `/guhs vadsrijk wereld plek steiger_eind` |
| ordetent | -384, 87, 991 | `/guhs vadsrijk wereld plek ordetent` |
| training | -322, 87, 981 | `/guhs vadsrijk wereld plek training` |
| kampvuur_kamp | -384, 87, 1025 | `/guhs vadsrijk wereld plek kampvuur_kamp` |
| haven_plein | 90, 65, 470 | `/guhs vadsrijk wereld plek haven_plein` |
| plein | 0, 64, 59 | `/guhs vadsrijk wereld plek plein` |
| questbord | -7, 64, 31 | `/guhs vadsrijk wereld plek questbord` |
| kampvuur_klodderweide_1 | 240, 69, -126 | `/guhs vadsrijk wereld plek kampvuur_klodderweide_1` |
| kampvuur_sporenwoud_1 | -228, 65, 17 | `/guhs vadsrijk wereld plek kampvuur_sporenwoud_1` |
| kampvuur_druipgrotten_1 | -260, 98, -311 | `/guhs vadsrijk wereld plek kampvuur_druipgrotten_1` |
| kampvuur_zwamtempel_1 | 138, 79, -236 | `/guhs vadsrijk wereld plek kampvuur_zwamtempel_1` |
| druipschacht_ingang | -463, 41, -471 | `/guhs vadsrijk wereld plek druipschacht_ingang` |
| slaapheuvel | 300, 96, 300 | `/guhs vadsrijk wereld plek slaapheuvel` |
| haven_z1_z2_vertrek | 150, 127, -653 | `/guhs vadsrijk wereld plek haven_z1_z2_vertrek` |
| uitkijk_noordkliftop | 154, 127, -656 | `/guhs vadsrijk wereld plek uitkijk_noordkliftop` |

### Zone 2 (De Splinterpieken)

| plek | waar (x, y, z) | commando |
|---|---|---|
| dok_landing | 704, 91, -900 | `/guhs vadsrijk z2wereld plek dok_landing` |
| plein | 704, 91, -1068 | `/guhs vadsrijk z2wereld plek plein` |
| kampvuur_galmhellingen_1 | 814, 100, -1046 | `/guhs vadsrijk z2wereld plek kampvuur_galmhellingen_1` |
| kampvuur_knuffelmeer_1 | 604, 100, -986 | `/guhs vadsrijk z2wereld plek kampvuur_knuffelmeer_1` |
| kampvuur_splinterkloof_1 | 508, 114, -1168 | `/guhs vadsrijk z2wereld plek kampvuur_splinterkloof_1` |
| warmwaterbron | 900, 120, -908 | `/guhs vadsrijk z2wereld plek warmwaterbron` |
| haven_z2_z3_vertrek | 980, 117, -856 | `/guhs vadsrijk z2wereld plek haven_z2_z3_vertrek` |
| uitkijk_galmkam | 983, 117, -860 | `/guhs vadsrijk z2wereld plek uitkijk_galmkam` |
| uitkijk_kandijkom_balkon | 704, 91, -882 | `/guhs vadsrijk z2wereld plek uitkijk_kandijkom_balkon` |
| arena_galmwolf | 886, 117, -974 | `/guhs vadsrijk z2wereld plek arena_galmwolf` |
| arena_boormeester | 674, 66, -1270 | `/guhs vadsrijk z2wereld plek arena_boormeester` |
| arena_spiegelzucht | 854, 176, -1397 | `/guhs vadsrijk z2wereld plek arena_spiegelzucht` |


## Oude testblokken ronde 3 (zone 1 + 2)


### R1. Makelaar en andere buitenstaanders blijven buiten
In het Vadsrijk:
```
/summon guhs:guh_npc ~ ~ ~2
/guhs erfenis brief
```
- **Summon:** er verschijnt niks. Guhs-wezens van thuis (makelaar Guhrrit, de Postguh, de Sherlock-cast) mogen er niet in.
- **Brief:** de makelaarsbrief komt niet in het Vadsrijk. Hij wacht tot je weer thuis bent (`/guhs vadsrijk` = terug).
- **Roepen:** "Roep naar mij" (guh-band) of de Guhkamer-roep in het Vadsrijk zegt "Je guhs blijven lekker thuis...".
- **Volgers:** een Libelkonijnguh, Sponsguh, Gerrit of Momo op je schouder gaat niet mee naar binnen, en staat er weer als je terug bent.

### R2. Huisdier zonder naambordje
```
/guhs vadsrijk huisdier geef koning_klodder
```
- Druk P, kies "Meenemen".
- Boven je huisdier staat **geen** naambordje meer (eerst stond er "entity.guhs.vr_huisdier").

### R3. Reiskaart: het kruisje
```
/guhs vadsrijk npc tp kapitein
```
- Klik op de kapitein. Rechtsboven in de roze titelbalk van de reiskaart staat een ✕ ("Sluiten" als je eroverheen gaat).
- Klik erop: de kaart gaat dicht, met een klikgeluidje.

### R4. Tabbladen: Rijdieren / Huisdieren / Verkenningsboek
```
/guhs vadsrijk mount alle
/guhs vadsrijk huisdier alle
```
- Druk N. Boven het paneel, tussen de guh-oortjes, zitten drie tabjes: **Rijdieren**, **Huisdieren**, **Verkenningsboek**.
- Klik heen en weer. Het actieve tabje hangt vast aan de roze titelbalk.
- Bij de stalmeester (`/guhs vadsrijk mount stal`) zijn er géén tabjes.

### R5. Rijdieren (ronde 3)
```
/guhs vadsrijk mount reset
```
- **Nog geen rijdier:** druk X en N: "Nog geen rijdier: dat krijg je in De Schemerwortel."
- **Teasers:** `/guhs vadsrijk npc tp stalmeester`. Hoefje plaagt over het Wolkenschaapje en de stal verkoopt nog niks.
  Ook bij Boswachter Mos (`npc tp boswachter_mos`) en in Kandijkom bij Breister Wolwinter (Guhwolf).
- **Weg:** de Hoedhupper en de Klodderros bestaan niet meer. Er zijn er nu 5.
```
/guhs vadsrijk mount alle
```
- **Snelheid:** Guhwolf het snelst (springt laag), Pluimguh, Knuffelhert (springt het hoogst, ~5 blokken), Wolkenschaapje (dubbele
  sprong), Slakkie lekker traag.
- **Opstapjes:** rijd zonder springen tegen een randje van 2 blokken: je rijdier stapt erop. Let op: een hek (1,5 hoog) stapt hij
  nu ook over. Zeg het als je dat niet wilt.
- **Instellen:** `/guhs vadsrijk balans rijdieren snelheid 1.2` (sectie `[rijdieren]` in `run/config/guhs-vadsrijk-balans.toml`).

### R6. Zone 2: hangbruggen
```
/guhs vadsrijk z2wereld plek brug_3_zuid
```
- Dit is de grote brug bij Kloofwachter Touwtje naar de Spiegelburcht. Loop eroverheen **zonder te springen**: elke stap is
  hooguit een half blokje, aan beide kanten sluit hij aan op de grond.
- Ook: `brug_1_zuid`, `brug_2_zuid`, `brug_4_zuid` (Splinterkloof) en `brug_5_zuid` (Galmspleet).

### R7. Zone 2: aankomst bij het dok en de poorten
```
/guhs vadsrijk z2wereld plek dok_landing
```
- **Glas weg:** het zwevende roze glas boven de poort is weg. Er staat nu een calciet-poortboog met een kristallamp.
- **Bergpas:** de weg naar de zuidpoort loopt glooiend door een pas, niet meer tussen rechte wanden.
- **Aankleding:** lampions en lichtsnoeren over de weg, vuurkorven, een bankje, een **chocomelk-kiosk** (`z2wereld plek dok_kiosk`)
  en een houten **welkomstboog** bij de pier (`z2wereld plek dok_welkom`). Sneeuw en kristallen langs de randen.
- **Andere poorten:** loop vanaf het plein van Kandijkom de noord-, oost- en westpoort uit. Dezelfde boog en een glooiende pas.

### R8. Muziek in de steden
```
/guhs vadsrijk npc tp bakker_bram
/guhs vadsrijk wereld plek haven_plein
/guhs vadsrijk z2wereld vlieg kandijkom
```
- Zet de schuif **Muziek** aan (en **Omgeving** voor de achtergrond).
- Na ~2 s begint een lief deuntje (Zwamzoet = wals, Knuffelkamp = ochtendfolk, De Haven = zeemanslied, Kandijkom = winterspeeldoos).
  Elke stad heeft er twee, met wat stilte ertussen.
- **Achtergrond:** vogeltjes, guh-gegiechel, belletjes; golfjes in De Haven, een haardvuur in Kandijkom; geknetter bij de kampvuren.
- **Weglopen:** loop de stad uit. Na ~3 s fadet de muziek weg; in de wildernis is er geen muziek.
- **Baas of scène:** bij een bazenbalk of een filmpje stopt het deuntje.
- **Uit:** in de mod-config (sfeer) `stadsmuziek = false`.

### R9. Het Verkenningsboek (B)
```
/guhs vadsrijk boek scherm
```
- Druk **B** (of klik het derde tabje). Een open roze boek met een bladwijzer per zone (De Knuffelzwammen, De Splinterpieken).
- **Links:** groot %, "x van de y gedaan", de beloning bij 100 %, en de categorieën (hoofdverhaal, zijmissies, uitkijkpunten,
  kampvuren, geheimen, verloren knuffels, bazen, huisdieren, gebieden).
- **Rijdieren (later):** die categorie staat er wel (zone 1: Wolkenschaapje + Knuffelhert, zone 2: Guhwolf), maar telt **niet** mee
  voor je %; rijdieren krijg je pas in zone 3. Hover erover voor de uitleg.
- **Rechts:** wat je hebt (groen vinkje), waar je mee bezig bent (geel) en wat nog niet (grijs met een hint; geheimen zijn "???").
- **Mijlpalen:** steek een paar kampvuren aan of klim een uitkijknaald op. Bij 25/50/75 % krijg je een roze toast.
  Bij 100 %: "Helemaal verkend!", een titel en Vadsmunten.
- `/guhs vadsrijk boek tel` laat je tellingen zien, `boek reset` begint opnieuw.
- **B en de snuffelhond:** B is ook "blaffen" als hond op Het Snuffeleiland. In het Vadsrijk wint het boek; als hond blaf je gewoon.

### R10. Kleine geheimpjes en de horizon (lore)
```
/guhs vadsrijk npc cliffhanger
/guhs vadsrijk z2npc flashback
/guhs vadsrijk z2npc cliffhanger
```
- Er zitten nu een paar hele kleine hints in zone 1 en 2 (praatjes, tooltips, de filmpjes). Let in de filmpjes goed op: soms is er
  maar één seconde iets te zien. Het einde van zone 2 wijst nu naar het **oosten**.
- **Horizon:** in zone 1 staat de reuzenboom (zone 3) nu in het oosten, naast het zwevende kasteel. Kijk vanaf Knuffelkamp naar het
  oosten (F3: richting "east").

---


## Oude testblokken ronde 2 (zone 1 + 2, nog steeds geldig)


### 1. Klassen: het wapen is je klasse
```
/guhs vadsrijk level 12
/guhs vadsrijk klasse wapen kussenzwaard
/guhs vadsrijk klasse wapen pluisboog
/guhs vadsrijk klasse wapen rozenstaf
/guhs vadsrijk wezens spawn klodder 10
```
- **HUD:** links van de hotbar staan twee regels, "Generiek nv 12" en de klasse.
- **Vasthouden:** hou de boog alleen vast. Je blijft dan ridder.
- **Aanvallen:** schiet één keer. Je wordt Pluisjager en R/V/G staan op volle cooldown.
- **Leeg:** met een lege hand wordt de klasseregel grijs en heb je geen skills.
- **Te hoog wapen:** `/guhs vadsrijk loot zeldzaam pluisboog 20`. De tooltip zegt in het rood "Vereist: Pluisjager nv 20", en aanvallen gaat niet.
- **Klasse-level apart zetten:** `/guhs vadsrijk klasse klasselevel pluisjager 15` en `/guhs vadsrijk klasse generiek 18`.
- **Skillboom:** druk K en bekijk de tabs per klasse. Hover over een skill voor uitleg met ✔ en ✖.

### 2. Balans (live aanpassen)
```
/guhs vadsrijk balans toon
/guhs vadsrijk balans knuffelridder schade 1.5
/guhs vadsrijk balans mobs schade 1.0
/guhs vadsrijk balans herlaad
```
- Het bestand staat in `run/config/guhs-vadsrijk-balans.toml`. Bewerken mag, en het wordt binnen 5 s opnieuw gelezen.
- **Verdediging:**
  1. Doe harnas aan: `/guhs vadsrijk loot episch knuffelharnas 12`.
  2. Laat je raken door `/guhs vadsrijk wezens spawn hoedjesbol 12`, dan door een hoger wezen (`... 25`).
  3. Bij het hogere wezen helpt je verdediging minder.

### 3. Drankjes
```
/guhs vadsrijk eco winkel drankjes
```
- Koop een Klein Vadsdrankje (120), een Vadsdrankje (300) en een Energiedrankje.
- Drinken duurt 2 s. Daarna staan alle drankjes 10 s op cooldown; je ziet het grijs over het item.

### 4. Melee en de tutorial (melee-fix: deze map, h-melee)
```
/guhs vadsrijk klasse wapen kussenzwaard
/guhs vadsrijk wezens spawn klodder 1
/guhs vadsrijk npc bam
```
- **Ingedrukt houden:** hou linkermuis ingedrukt met het zwaard. Hij blijft meppen op het tempo van het wapen.
- **Klik net te vroeg:** een klik vlak voordat je weer mag slaan wordt onthouden en landt alsnog.
- **Startzwaard:** het zwaard dat je in Knuffelkamp krijgt doet 2× schade. Een klodder is in 2-3 klappen weg.
- **Arena:** de vijver-arena van de Grauwe Klodderreus is groter, met droge vlakke grond eromheen.
- **Door gras heen:** slaan lukt door gras en riet heen, maar niet door blokken.

### 5. Sieraden en UI
```
/guhs vadsrijk loot zeldzaam roze_ring 5
/guhs vadsrijk loot zeldzaam pluisoorbel 5
```
- **Tooltip:** lees de uitleg "Rechtsklik om te dragen".
- **Aandoen:** open je inventaris en doe shift+klik op het sieraad.
- **Hoveren:** ga met de muis over het vakje rechts. Je ziet de volledige stats en "Klik om af te doen".
- **Sets:** `/guhs vadsrijk z2wezens set` geeft set-stukken. De tooltip toont "Set: Kristalset (x/4)".

### 6. Rijdieren (X / N)
```
/guhs vadsrijk mount starter
/guhs vadsrijk mount alle
```
- **Starterkeuze:** de eerste regel opent de keuze uit 3 Pluimguhs. Kies er één en lees het chocobo-grapje.
- **Alle rijdieren:** met de tweede regel heb je ze allemaal. N opent het scherm met kleuren, en daar kies je "Hierop rijden".
- **Rijden:** X is op- en afstijgen. Rijd met de Pluimguh, het Wolkenschaapje (dubbele sprong) en Slakkie.
- **Ronde 3, eerst zonder rijdier:** doe `/guhs vadsrijk mount reset`. Druk X en N: "Nog geen rijdier...". Praat met Stalmeester
  Hoefje (`/guhs vadsrijk npc tp stalmeester`): hij plaagt over het Wolkenschaapje, en de stal verkoopt nog niks.
- **Ronde 3:** er zijn nu 5 rijdieren (de Hoedhupper en de Klodderros zijn weg). Voel het verschil: de Guhwolf is het snelst
  maar springt laag, het Knuffelhert springt het hoogst, Slakkie is lekker traag.
- **Opstapjes:** loop zonder springen tegen een randje van 2 blokken: je rijdier stapt er zo op.
- **Silhouetten:** zie je die? Doe `/guhs vadsrijk mount reset` en open N opnieuw.
- **Afstijgen bij een baas:** `/guhs vadsrijk bazen tp koning` en rijd de arena in.
- **Stal:** `/guhs vadsrijk mount stal`.

### 7. Huisdieren (P)
```
/guhs vadsrijk huisdier geef koning_klodder
/guhs vadsrijk huisdier alle
/guhs vadsrijk huisdier weg
```
- **Scherm:** druk P. Je ziet "Verzameld x/y", met silhouetten voor wat je nog niet hebt.
- **Meenemen:** kies "Meenemen" en loop rond.
- **Teleport:** `huisdier weg` zet je huisdier 20 blokken weg. Kijk of hij met een poef terugkomt.
- **Echt verdienen:**
  1. `/guhs vadsrijk huisdier reset`.
  2. `/guhs vadsrijk baas reset bazen:koning_klodder` en dan `/guhs vadsrijk bazen tp koning`.
  3. Versla hem. Dan verschijnt "Nieuw huisdier!".

### 8. Reiskaart en kapiteins
```
/guhs vadsrijk npc tp kapitein
```
- **Kapitein:** klik op hem. Je krijgt de reiskaart, met grijze "?"-plekken en de teasers voor zone 3 en 4.
- **Kaart:** sleep met de muis om te schuiven en klik een open plek aan om te vliegen.
- **Scheepsbel:** rechtsklik op de bel opent ook de reiskaart.
- **Zone 2 vrijspelen:**
  1. `/guhs vadsrijk npc quest af hq12` en daarna `/guhs vadsrijk klasse generiek 20`.
  2. Kandijkom gaat dan open op de kaart.

### 9. Werver Wimpel (Guhmension)
- Ga terug: `/guhs vadsrijk`.
- Zoek de ruïne: `/locate structure guhs:vr_w_portaaltempel`, of zet Wimpel aan je voeten met `/guhs vadsrijk npc werver`.
- Raak het portaal aan vóór je met hem praat. Er gebeurt niks.
- Praat met Wimpel. Je krijgt het Wynn-grapje en het advies over de weergave. Daarna wordt het portaal wakker.
- Opnieuw testen: `/guhs vadsrijk npc werver vergeet` en `/guhs vadsrijk wereld vergeetportaal`.

### 10. Terug-quests (Hert en Wolf)
```
/guhs vadsrijk mount geef pluimguh
/guhs vadsrijk npc quest start rijdier_knuffelhert
/guhs vadsrijk npc tp boswachter_mos
```
- **Knuffelhert:**
  1. Je hebt al een rijdier, dus de quest staat in de questvolger.
  2. Steek de 3 lantaarns aan in het Sporenwoud.
  3. Volg de hoefsporen naar de open plek.
- **Guhwolf:** `/guhs vadsrijk npc quest start z2_rijdier_guhwolf` (zone 2, Kandijkom).

### 11. Questvolger en job-advancement
```
/guhs vadsrijk klasse klasselevel knuffelridder 10
```
- Rechtsboven verschijnt een lichtblauw blok met ✦, met een lichtblauwe straal naar de Grootmeester.
- **Tempeldeur:** `/guhs vadsrijk klasse generiek 18` en `/guhs vadsrijk bazen tp tempel`. Rechtsklik op de grote deur; hij gaat open.
- **Puzzelkamer:** loop een kamer in. De tip blijft onderin beeld staan.

### 12. Kaart zone 1
```
/guhs vadsrijk wereld plek aankomst
```
- **Kamp:** loop de kliftrap op. Er staat geen boom op de uitgang, en een pad met lantaarns loopt naar het kampvuur.
- **Bram:** `/guhs vadsrijk npc tp bakker_bram`. Hij staat in zijn kraam.
- **Kasteeltje:** `/guhs vadsrijk wereld plek ordetempel_binnen`. Kijk naar de torens van het kasteeltje.
- **Biomes:** F3+F6 of JourneyMap toont "Het Vadsrijk - <plek>".
- **Draak:** `/guhs vadsrijk sfeer draak stad` (50% groter). Zie je de opening graag nog eens: `/guhs vadsrijk npc opening`.
- **Opgevrolijkte wezens:** versla een klodder. Hij wordt doorzichtig en je slaat erdoorheen.

### 13. De nieuwe zone 2
```
/guhs vadsrijk z2wereld vlieg kandijkom
```
- **Kandijkom:** de terrassen, de klokkentoren, de kerstmarkt, interieurs (ga een huisje in), schaatsers en pinguhs.
- **Poorten:** loop alle vier de poorten uit, vooral oost naar de Galmhellingen. Er staat geen muur meer.
- **Gondel:** `/guhs vadsrijk z2wereld gondel op`.
- **Spiegelburcht:** `/guhs vadsrijk z2wereld plek burcht_trap_voet`.
- **Galmwolf:** `/guhs vadsrijk z2wereld plek arena_galmwolf_deur`.
- **Knuffelmeer:** loop het ijs op en weer af.

---
Valt je iets op? Noteer het commando-blok en wat je zag; dan kan ik het gericht fixen.


# 3. ZONE 3: DE SCHEMERWORTEL


## De zone-3-tests (r-int, vernieuwde kaart)


### Z1. Erheen + de openingsfilm
```
/guhs vadsrijk z3npc klaarzetten
/guhs vadsrijk npc tp z2_kapitein
```
- Klaarzetten = zone 2 klaar + level 30. Klik Kapitein Poolster: "De Schemerwortel" staat in de keuzes en op de reiskaart.
  De stippellijnen op de reiskaart: Kandijkom → Schemerwortel (lijn B) en Schemerwortel → zone 4 (lijn C, nog "binnenkort").
- Kies hem: de **openingsfilm** (~26 s). Kijk of de camera nergens in een tak of blad zit. Je landt op de **Weststeiger**.
  Hoor je de **honingwals**?
- Nog een keer: `/guhs vadsrijk z3wereld vlieg opnieuw`. Terug: de scheepsbel bij de mast.

### Z2. Honingtak, de route en de Pluimguh-starter
```
/guhs vadsrijk npc questlog
```
- Raatje staat op het **Bloesemplein** (zuidkant van het ringdek). hq02: de Nestkapel is een honingraatcel ONDER het dek (kapellift).
  Daarna stuurt Raatje je OMLAAG: binnenhoflift (`schorslift_1`) en de noordpoort naar de **Pluimstal** op de grond.
- hq03: Veertje rijdt voor je uit het **rijpad** op (4 rondjes), langs het Bloesemplein, door de bladtunnel naar kruin 1. Zweef van
  het zweefplatform op de zuidrand van kruin 1 (spatie ingedrukt).
- Kijk rond: korfhuisjes met interieurs, de 3 cellen onder het dek (café, Nestkapel, Raattrossen) met hun liftjes, het marktje.

### Z3. Rijpad, binnentrap, windkolommen en liften
```
/guhs vadsrijk z3wereld plek rijpad_start
/guhs vadsrijk z3wereld plek trap_bordes_1
/guhs vadsrijk z3wereld plek binnenhof
/guhs vadsrijk z3wereld plek wind_honingtak_onder
/guhs vadsrijk z3wereld lift schorslift_2 op
```
- **Het Rijpad** (gebiedsnaam, was "De Stamspiraal"): rij het van `rijpad_start` tot `rijpad_eind` (kruin 1) op je Pluimguh, zonder te
  springen. Afslagen naar Honingtak en de takken, balkonnetjes om af te zweven.
- **De binnentrap**: van `trap_bordes_1` (vloer 80) tot `trap_bordes_6` (nest). Halve stapjes, leuning, deuren naar Honingtak en elke kruin.
- Windkolommen (zie TESTEN voor alle 7): erin lopen = omhoog naar het landingsdek.
- Liften: honinglift (pas na hq09), honinglift_kruin, lift_cafe, lift_kapel, lift_raattros, schorslift_1, schorslift_2.

### Z4. Bazen
```
/guhs vadsrijk level 50
/guhs vadsrijk z3bazen arenas
/guhs vadsrijk z3bazen spawn kruinroofvogel
/guhs vadsrijk z3bazen tp werf
/guhs vadsrijk z3bazen spawn piepvel
/guhs vadsrijk z3bazen tp hart
/guhs vadsrijk z3bazen vahoegnir
```
- **Kruinroofvogel**: de Kale Kruin (dode mesa aan kruin 1). Fase 3: op je rijdier het roze spoor om de boom volgen.
- **Werf** (eigen eiland, kabelbrug): Taleggio (Dok 1), Brokkel (Dok 2, toren), Roquefort (Dok 3, masttoren; wrak + trap terug).
- **Piepvel**: de Pelszaal ligt ONDER de Schorsstad-vloer (trap omlaag). Kijk naar zijn **cape** van muizenvelletjes (oortjes,
  staartjes; per kooi een lap minder). F1: grijze mist + twee Mika-handlangers. Ga daarna op de Schorsstad-vloer erboven staan:
  géén bazenbalk/intro.
- **Hart**: een ring kamers om de Hartslag-zaal; Wortelwurger, Nestwachter, Baron Bleu in looprichting.
- **Vahoegnir**: de **nieuwe draak** (roze, guh-kop, grote oren, 6 kleurstappen, paarse gloei-ogen, vleugels in een V in rust).
  `vahoegnir waar` / `vahoegnir reset` als hij zoek is.

### Z4b. Bazen-testgids (m-bazen): elke zone-3-baas, in kerkervolgorde
Eerst `/guhs vadsrijk level 50`, `/gamemode survival`, `/guhs vadsrijk vol`.
- **Eén gevecht tegelijk** ("Je bent nog in gevecht met ...") en **kerkervolgorde** ("Versla eerst ..."). `tp <baas>` en
  `spawn <baas>` geven de volgorde vrij voor jou; `spawn` zet de baas terug en jou bij zijn bord.
1. `spawn kruinroofvogel` (geen volgorde).
2. Werf: `tp werf`, dan `spawn taleggio`, `spawn brokkel`, `spawn roquefort`. Kamers: `tp loods`, `kerkers loods`, `kerkers reset`.
   Intro-check: vecht Taleggio, loop naar zijn dekrand: geen intro van Brokkel/Roquefort.
3. `spawn piepvel` (cape bekijken).
4. Hart: `tp hart` (chat geeft route + bazen), `spawn wortelwurger`, `spawn nestwachter`, `spawn bleu`; kamers `tp aderloop|kwekerij|ringen`.
5. Vahoegnir: `vahoegnir` (zet je aan de nestrand), `vahoegnir herinnering` (zonder verhaal), `vahoegnir waar|reset`.
- Fase overslaan: `/guhs vadsrijk z3bazen fase 2|3|4`. Vahoeg-vorm: `/guhs vadsrijk z3bazen vahoeg werf|hart`.

### Z5. De roze draak, de beestjes en de naambordjes
```
/guhs vadsrijk vriendjes draak nest
/guhs vadsrijk vriendjes draak bezoek
/guhs vadsrijk huisdier geef vahoegje
```
- `draak nest`: je staat aan de nestrand; de **roze Vahoegnir rust** daar (normaal alleen met `zone3_klaar`). Rechtsklik = aaien
  (hartjes, spinnen, buiging, een lief zinnetje). Tot 32 **beestjes** komen aanvliegen/huppelen: vinkjes, meesjes (op zijn rug),
  eekhoorntjes, konijntjes, roze muisjes, vlindertjes. Loop weg: ze vliegen/huppelen weg.
- `draak bezoek` (in zone 1/2, bij een plein/kampvuur): hij landt in de buurt, blijft 60-90 s, vliegt weer weg.
- Vahoegje: P, "Meenemen": het nieuwe **chibi-draakje**. Kijk ernaar: naambordje "Vahoegje van <jouw naam>" (weg met F1).

### Z6. Premiebord en het Verkenningsboek
```
/guhs vadsrijk z3systemen bord
/guhs vadsrijk z3systemen penningen 100
```
- Premiebord: drie tabs. B: pagina "De Schemerwortel"; staan "Het Rijpad", de dorpen en de binnentrap goed bij de gebieden?

### Z7. Lopen (survival)
- Rijpad helemaal op en af, binnentrap van de vloer tot het nest, de Pelszaal-trap, de Wortelgang naar de Hartslag-zaal en terug,
  de kabelbrug naar de werf en de torentrappen, de imkerdorpjes op de takken (huisjes leeg van blad?).
- Overal: kun je uit elk gat weer omhoog? (`U` = losknuffelen als het toch misgaat.)

### Z8. Praatscherm-portretten (q-dialoog)
```
/guhs vadsrijk npc portrettest
```
- Je ziet achter elkaar: een regel van jezelf (jouw eigen portret), Oma Truus als stand-in (zone 1, ver weg), een zone-3-imker als
  stand-in, de verteller (een boekje), en daarna een echte NPC uit de buurt.
- Praat daarna met een paar NPC's in Honingtak: staat er bij elke pagina een portret (ook bij jouw antwoorden)?
- Met F3+H: onderin een grijs regeltje (sleutel | tekst | spreker | bron van het portret).

---


## De geheime baas: Mika de Imker (zone 3, T-IMKER)

De geheime quest "De grauwe honing" (z3_geheim_imker) eindigt nu niet meer in een losse scène, maar in een echt baasgevecht
in de raatzaal onder de Raattros (Honingtak). Lv 47, Natuur. Beloning: huisdier **Zoempje** (een goudgeel bijtje), een trofee,
een buitkist.

> Let op: de raatzaal is groter gemaakt (straal 11, 10 hoog) en een stukje verschoven, zodat de ladder aan de rand uitkomt.
> Dat zit in de kaartgenerator; pas na de integratie-regen (`maak_kaart --zone 3`) staat de nieuwe zaal in de wereld.

### Snel testen
```
/guhs vadsrijk imker spawn
```
Je staat in de raatzaal, hij wordt meteen wakker (volle Vads, fase 1). Kijk naar:
- in het midden-noorden de **Koninginnekorf** (strokorf, koninginnetje bovenop), aan de rand **drie rookpotjes**;
- titelkaart **MIKA DE IMKER** / "Spion van de Keizer. Imker voor spek en bonen." en een tip-kaartje;
- een Mika in een wit imkerpak met strohoed, gaassluier, roker en een speldje van de Keizer op zijn borst.

**Fase 1 "Kwaliteitscontrole" (100-66%)**
- Rode cirkel = grauwe honingklodder. Daarna blijft een grijze **plas** liggen (20 tellen): erin = plakkerig (traag) + klein beetje schade.
- Rode strook voor hem = **kaasrook-puf** (opzij stappen!).
- "Een boze zwerm zoemt achter jou aan!": een zwart-gele wolk volgt je. **Rechtsklik een rookpotje** (rookwolk 8 tellen) en loop
  door de rook: "De zwerm wordt suf in de rook en valt in slaap." In de rook steekt geen bij je. Een potje moet daarna afkoelen
  (rechtsklik = "nog X tellen").

**Fase 2 "De grauwe koningin" (66-33%)**
- Hij springt op een stok naast de korf en giet grauwe honing erin (grijs draadje). Hij is dan **onkwetsbaar**
  ("Hij giet en is nergens te raken!").
- Elke 6 tellen spuit de korf bijen langs een **rode baan** uit het gat. Sta er niet in!
- **Rechtsklik de korf** = gat een zesde slag draaien (shift + rechtsklik = terug). Wijst de baan naar zijn stok, dan steken de
  bijen HEM: "AU! Niet mij! IK ben de imker!" -> 5 tellen verdoofd, je raakt hem extra hard, de grauw-meter zakt.
  Daarna klimt hij op een andere stok.
- Duurt het te lang (30 tellen gieten): "De koningin wordt grauw!" -> flinke klap op iedereen. De korf wordt zichtbaar grijzer.

**Fase 3 "De grote rookpot" (33-0%)**
- Hij komt naar beneden en jaagt weer; twee zwermen tegelijk, meer klodders.
- **Grote kaasrookbom**: gele cirkel bij de korf. Ga er (samen) in staan: de klap wordt gedeeld. Te weinig mensen erin = iedereen geraakt.
- Na 7 minuten: ALLES OF NIKS (enrage).

**Verslagen:** hij vlucht in een rookwolk ("Dit komt in mijn rapport! Met drie uitroeptekens!"), de korf wordt weer goud,
buitkist + Zoempje-moment.

### Het hele verhaal (zoals een speler het speelt)
```
/guhs vadsrijk imker klaarzetten
```
Quest loopt, 5/5 sporen, het luik is open. Je staat bij het luik in de Raattros-cel. Klim de ladder af:
1. In de zaal start de **intro-scène**. Controleer het **portret**: dat moet de Mika-imker zelf zijn (wit pak, strohoed), niet een boek.
   De verteller-pagina's tonen het boek (zo hoort het).
2. Scène dicht -> hij wordt wakker, titelkaart, gevecht.
3. Na de overwinning: **vervolgscène** (notitieboekje "Rapport aan de Keizer...", de koningin zingt, Zoempje), en de quest-stap
   "Versla de Mika-imker" vinkt af. Lever in bij de Raatwever.
4. Daarna kun je hem opnieuw bevechten (elke 10 minuten), zonder scènes; hij zegt dan "Jij weer?!".

### Losse commando's
| Commando | Wat |
|---|---|
| `/guhs vadsrijk imker spawn` | meteen het gevecht (fase 1, volle Vads) |
| `/guhs vadsrijk imker klaarzetten` | quest + sporen + luik open, je staat bij het luik |
| `/guhs vadsrijk imker fase 2` / `fase 3` | spring naar fase 2 / 3 |
| `/guhs vadsrijk imker steek` | (fase 2) draai de korf naar zijn stok en laat hem spuiten |
| `/guhs vadsrijk imker zwerm` | een zwerm achter jou aan |
| `/guhs vadsrijk imker bom` | de grote kaasrookbom (gele cirkel) |
| `/guhs vadsrijk imker intro` | alleen de intro-scène (portret checken) |
| `/guhs vadsrijk imker na` | alleen de vervolgscène |
| `/guhs vadsrijk imker zoempje` | huisdier Zoempje meteen krijgen |
| `/guhs vadsrijk imker waar` | waar is de zaal, fase, Vads, korfrichting, grauw-meter |
| `/guhs vadsrijk imker reset` | baas weg, scènes vergeten |
| `/guhs vadsrijk imker tp` | naar het luik |

Huisdier: in het huisdierenmenu staat **Zoempje** ("Een goudgeel bijtje van de bijenkoningin. Steekt nooit, zoemt altijd."),
een zwevend bijtje met fladderende vleugeltjes.

Gametests: `runGameTestServer -Pgt=VadsrijkImker` (3 tests).


# 4. ZONE 4: DE LAMPIONKUST (lv 50-56)


## SPIEK T-Z4WERELD - zone 4 "De Lampionkust" (de kaart)

Zone 4 is een slinger van eilandjes in een roze zee, rechts van de Schemerwortel (x 1740..2540, z -716..-266, lv 50-56).
Alles staat op de kaart; de wezens, het verhaal en het zeilbootje komen van andere slices. Je komt er nu met de dev-commando's
(teleport). Elke regel `/guhs vadsrijk z4wereld plek <id>` brengt je erheen (ook van buiten de dimensie). Tab-aanvullen werkt.
Losse coordinaten staan erachter als je liever `/tp` gebruikt (in de dimensie `guhs:vadsrijk`).

**Kaart opnieuw maken:** `python tools/vadsrijk/maak_kaart.py --zone 4` (~3 min). Controle: `python tools/vadsrijk/zone4/loopcheck4.py`,
`python tools/vadsrijk/zone4/poicheck4.py`, `python tools/vadsrijk/zone4/audit4.py`. Een NIEUWE wereld/regen is nodig om de tegels te zien
(chunks die al bestaan veranderen niet).

### Lampionhaven (de hub)

- `/guhs vadsrijk z4wereld plek respawn`  (1848 67 -467) Het Havenplein: fonteintje met guh-beeldje, de lampionmast met snoeren als een tentje, kampvuur ernaast (unlit = checkpoint).
- `/guhs vadsrijk z4wereld plek kade`  (1866 66 -470) De kade met de 3 steigers en bootjes. Loop de steigers op: de lampionsnoeren hangen nu boven je hoofd, aan het eind een ladder uit het water.
- `/guhs vadsrijk z4wereld plek haven_3_4_landing`  (1796 71 -468) De luchthaven (lijn C naar de Boom): platform op palen met het aangemeerde luchtschip, vertrekbord. Trap aan de oostkant naar beneden.
- `/guhs vadsrijk z4wereld plek gebouw.vliegerwinkel.deur`  (1844 75 -481) Vliegerwinkel (winkel): toonbank, weefgetouw. Trap van de deur naar beneden moet aansluiten.
- `/guhs vadsrijk z4wereld plek gebouw.winkel.deur`  (1843 67 -445) Algemene winkel: toonbank + kist.
- `/guhs vadsrijk z4wereld plek gebouw.herberg.deur`  (1829 68 -466) Herberg: bed, tafeltjes.
- `/guhs vadsrijk z4wereld plek gebouw.bank.deur`  (1829 72 -480) Bank: toonbank + goudblok (kluis).
- `/guhs vadsrijk z4wereld plek gebouw.lampionwerkplaats.deur`  (1835 67 -448) Lampionwerkplaats (ambacht): werkbank + weefgetouw, afdak vol lampions.
- `/guhs vadsrijk z4wereld plek gebouw.kookhuis.deur`  (1852 67 -445) Kookhuis (ambacht): smoker + werkbank.
- `/guhs vadsrijk z4wereld plek gebouw.havenmeester.deur`  (1860 66 -447) Havenmeester: toonbank + lessenaar.
- `/guhs vadsrijk z4wereld plek gebouw.botenverhuur.deur`  (1870 66 -490) Botenverhuur op de noordarm (paalwoning, loopplank vast aan de kade).
- `/guhs vadsrijk z4wereld plek gebouw.visrestaurant.deur`  (1909 66 -492) Visrestaurant 'De Blije Bot' met terras boven het water.
- `/guhs vadsrijk z4wereld plek gebouw.stal.deur`  (1818 67 -452) Stal + de wei met hek (hooi, waterbak).
- `/guhs vadsrijk z4wereld plek boot_dok_lampionhaven`  (1891 65 -464) Het eind van de middelste steiger: hier vertrekt het zeilbootje (t-z4boot).
- `/guhs vadsrijk z4wereld plek uitkijk_lampionduin`  (1860 90 -517) Uitkijkpunt op de Lampionduin: panorama op de Boom (zone 3). Trappenpad vanaf het plein.
- `/guhs vadsrijk z4wereld plek vuurtorentje`  (1912 64 -436) Het vuurtorentje met oortjes op de punt van de zuidarm.

### De eilandjes

- `/guhs vadsrijk z4wereld plek eiland_knuffeltij`  (1935 67 -345) Knuffeltij-strand: grauwe knuffels in de branding, het Opvangcentrum (binnen: knuffels op de toonbank), knuffelwas, droogwaslijnen, soepkraampje.
- `/guhs vadsrijk z4wereld plek eiland_strandfeest`  (1995 68 -605) Strandfeesteiland: parasols, kampvuur (checkpoint) met dansplek, schelpenlimonadebar, beachvolleybal.
- `/guhs vadsrijk z4wereld plek eiland_bloemeneiland`  (2085 71 -405) Bloemeneiland: bloemenstroken, bijenboerderij (binnen), bijenkorven, picknickkleedjes.
- `/guhs vadsrijk z4wereld plek eiland_koraalbaai`  (2185 64 -565) Koraalbaai: atol met roze lagune. Duiksteiger vanaf de noordring, het ronde vlot in het midden = de arena van de Grauwe Zeeslurf.
- `/guhs vadsrijk z4wereld plek eiland_vliegereiland`  (2299 89 -665) Vliegereiland: vliegers (wol, met oortjes en oogjes) aan touwtjes op de helling, wedstrijdpaal op de top, vliegerwerkplaats.
- `/guhs vadsrijk z4wereld plek eiland_vuurtoreneiland`  (2404 83 -490) Vuurtoreneiland: de grote vuurtoren (deur aan de zuidkant, wenteltrap naar de galerij) + het huisje van de wachter.
- `/guhs vadsrijk z4wereld plek eiland_pierkop`  (2470 73 -565) Pierkop: de Opstijgpier (pier op palen naar het oosten, rond platform met meermast).
- `/guhs vadsrijk z4wereld plek eiland_mysterie`  (2480 79 -330) Mysterie-eiland: GRIJZE MIST (de Grauwe Mistbank, zicht ~14 blokken), vervallen grauwe poort, bleke bomen.
- `/guhs vadsrijk z4wereld plek schildpad`  (2239 72 -445) Schildpadeiland (stilstaande versie): schild met hutje, palm en bloementuintje; drijvende steiger aan de flank met pad omhoog.
- `/guhs vadsrijk z4wereld plek wrak_ingang`  (2178 67 -700) Het Wrak van de Vliegervanger (kerker): gestrand Mika-schip op een rots in het zuiden.

### Het Wrak van de Vliegervanger (kleine kerker)

- `/guhs vadsrijk z4wereld plek wrak_aanleg`  (2192 64 -700) Steiger met ladder; loop over de rots, 3 treden op naar het achterdek.
- `/guhs vadsrijk z4wereld plek wrak_luik`  (2176 67 -705) Het luik in het achterdek: 9 treden naar beneden het ruim in.
- `/guhs vadsrijk z4wereld plek wrak_ruim`  (2175 57 -700) Kamer 1, het ruim (tonnen). Deuropening naar het westen.
- `/guhs vadsrijk z4wereld plek wrak_kombuis`  (2165 57 -700) Kamer 2, de kombuis (smoker).
- `/guhs vadsrijk z4wereld plek wrak_knuffelkooi`  (2155 57 -700) Kamer 3, de knuffelkooi: gestolen knuffels achter tralies, schatkist. Trap omhoog naar het voordek.
- `/guhs vadsrijk z4wereld plek arena_vliegervanger`  (2158 67 -700) Het voordek = de baasarena (de Vliegervanger), met de grauwe oorlogsvlieger aan de mast.

### Koraalbaai (wereldbaas + duiken)

- `/guhs vadsrijk z4wereld plek duiksteiger`  (2185 64 -605) Duiksteiger vanaf de noordring de lagune in; aan het eind een trapje en een drijvend looppad naar het vlot.
- `/guhs vadsrijk z4wereld plek arena_zeeslurf`  (2185 64 -565) Het vlot in de lagune = de arena van de Grauwe Zeeslurf (lampionnetjes langs de rand).
- `/guhs vadsrijk z4wereld plek grotje_1_uitgang`  (2171 63 -565) Boven grotje 1. Duik naar beneden: elk grotje heeft een schacht met een BELLENKOLOM omhoog (snel weer boven + lucht).

### Opstijgpier (naar zone 5)

- `/guhs vadsrijk z4wereld plek opstijgpier_begin`  (2472 71 -565) Begin van de pier op het eiland.
- `/guhs vadsrijk z4wereld plek haven_4_5_landing`  (2517 73 -562) Het ronde platform: hier landt/vertrekt het luchtschip naar Hoogvads (lijn D). Vertrekbord, meermast met oortjes.
- `/guhs vadsrijk z4wereld plek opstijgpier_cliffhanger`  (2521 73 -565) Het eind van de pier (de cliffhanger-plek van het verhaal).

### Kampvuren (allemaal unlit = checkpoint; rechtsklik)

- `/guhs vadsrijk z4wereld plek kampvuur_bloemeneiland`  (2089 70 -401)
- `/guhs vadsrijk z4wereld plek kampvuur_knuffeltij`  (1949 67 -343)
- `/guhs vadsrijk z4wereld plek kampvuur_koraalbaai`  (2177 71 -615)
- `/guhs vadsrijk z4wereld plek kampvuur_lampionhaven`  (1839 67 -460)
- `/guhs vadsrijk z4wereld plek kampvuur_mysterie`  (2466 77 -324)
- `/guhs vadsrijk z4wereld plek kampvuur_pierkop`  (2466 71 -573)
- `/guhs vadsrijk z4wereld plek kampvuur_strandfeest`  (1989 68 -595)
- `/guhs vadsrijk z4wereld plek kampvuur_vliegereiland`  (2278 64 -641)
- `/guhs vadsrijk z4wereld plek kampvuur_vuurtoreneiland`  (2404 82 -486)

### Zeilbootsteigers (elk met een ladder uit het water)

- `/guhs vadsrijk z4wereld plek boot_dok_bloemeneiland`  (2028 65 -405)
- `/guhs vadsrijk z4wereld plek boot_dok_knuffeltij`  (2002 65 -345)
- `/guhs vadsrijk z4wereld plek boot_dok_koraalbaai`  (2114 65 -565)
- `/guhs vadsrijk z4wereld plek boot_dok_lampionhaven`  (1891 65 -464)
- `/guhs vadsrijk z4wereld plek boot_dok_mysterie`  (2441 65 -330)
- `/guhs vadsrijk z4wereld plek boot_dok_pierkop`  (2470 65 -530)
- `/guhs vadsrijk z4wereld plek boot_dok_schildpad`  (2258 64 -431)
- `/guhs vadsrijk z4wereld plek boot_dok_strandfeest`  (1995 65 -558)
- `/guhs vadsrijk z4wereld plek boot_dok_vliegereiland`  (2300 65 -621)
- `/guhs vadsrijk z4wereld plek boot_dok_vuurtoreneiland`  (2365 65 -500)
- `/guhs vadsrijk z4wereld plek boot_dok_wrak`  (2192 64 -700)

### Uitzichtpunten (panorama bij eerste bezoek)

- `/guhs vadsrijk z4wereld plek uitkijk_lampionduin`  (1860 90 -517)
- `/guhs vadsrijk z4wereld plek uitkijk_pierkop`  (2509 73 -559)
- `/guhs vadsrijk z4wereld plek uitkijk_vliegerheuvel`  (2301 91 -663)
- `/guhs vadsrijk z4wereld plek uitkijk_vuurtoren`  (2400 128 -495)

### Geheimen (een knuffel of een kist op de plek)

- `/guhs vadsrijk z4wereld plek geheim_knuffel_1`  (1916 63 -426)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_2`  (2201 48 -563)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_3`  (2350 70 -585)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_4`  (2488 81 -326)
- `/guhs vadsrijk z4wereld plek geheim_knuffel_5`  (1790 69 -610)
- `/guhs vadsrijk z4wereld plek geheim_schat_1`  (2519 64 -564)
- `/guhs vadsrijk z4wereld plek geheim_schat_2`  (2240 64 -640)
- `/guhs vadsrijk z4wereld plek geheim_schat_3`  (2183 46 -579)
- `/guhs vadsrijk z4wereld plek geheim_schat_4`  (2425 63 -400)

### Randen, mist, gebieden

- `/guhs vadsrijk z4wereld stroming`: je staat in het water aan de zuidrand. **Wat moet er gebeuren:** je drijft langzaam terug naar het noorden
  (de Kringstroom) en ziet even "De Kringstroom duwt je zachtjes terug naar de eilandjes...". Zwem je harder naar buiten, dan kom je niet ver.
- `/guhs vadsrijk z4wereld mist`: het strand van het Mysterie-eiland. **Wat moet er gebeuren:** grijze mist, kort zicht, mistwolkjes;
  vaar/zwem weg naar het westen en de mist trekt zacht op.
- `/guhs vadsrijk z4wereld gebied`: zegt het gebied (z4_...), het kern-gebied, de mobzone, de biome, de stroming en de mist waar je staat.
  F3 laat de biome zien als "Het Vadsrijk - <eiland>" (alleen in een NIEUWE wereld/nieuwe chunks).
- `/guhs vadsrijk z4wereld eiland <naam>` (tab) / `/guhs vadsrijk z4wereld dok <naam>` / `/guhs vadsrijk z4wereld uitkijk <naam>`.

### Wat er (nog) NIET is

- Geen wezens/NPC's/quests (andere slices). De NPC-plekken staan klaar in POI_Z4.md (`gebouw.*.npc`, `npc_*`).
- De schildpad zwemt nog niet: het eiland ligt stil op het begin van de ronde (de route staat in `SCHILDPAD_ROUTE`).
- De vliegers zijn blokken (wol + touw), geen bewegende entities. Hoogvads (zone 5) hangt NIET in deze tegels.


## Spiekbrief zone 4 "De Lampionkust": het verhaal (t-z4verhaal)

Alles hieronder speel je in de Vadsrijk-dimensie, als OP. Een commando begint altijd met `/guhs vadsrijk z4verhaal ...`.

**Wat je ziet als iets van een ander nog ontbreekt:**
- **Het zeilbootje** is van t-z4boot. Zonder bootje zegt Botenguh Zeiltje dat het nog gebouwd wordt, maar de stap telt wel. Steek dan zwemmend over, of tp.
- **De bazen** (Zeeslurf, Kootje) zijn van een andere slice. Zonder die slice krijg je een kort gesprekje in plaats van een gevecht.
- **Het echte luchtschip** is van t-luchtschip. Zonder dat vlieg je met een camerafilmpje.
- **De kaart** is van t-z4wereld. Zonder die kaart staan NPC's en plekken op de render-coördinaten.

### Snel klaarzetten
| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z4verhaal klaarzetten` | Zone 2 en 3 zijn klaar en je bent minstens level 50. Wasja in de Nestkapel biedt nu "Lampions aan de kust" aan. |
| `/guhs vadsrijk z4verhaal hoofdlijn 1` ... `7` | Spring naar hoofdstap 1-7: alles ervoor is ingeleverd en de stap loopt. Je level wordt minstens 49+stap. |
| `/guhs vadsrijk z4verhaal skip` | Alle doelen van de lopende zone-4-hoofdquest staan op klaar. Lever zelf in bij de NPC die de tracker aanwijst. |
| `/guhs vadsrijk z4verhaal quest <id> start` / `klaar` / `af` | Eén quest starten, alle doelen klaarzetten, of meteen afronden (met beloning, zonder gesprek). |
| `/guhs vadsrijk z4verhaal tp <plek>` | Teleport naar een NPC (`z4_havenmeester`), een POI (`poi.z4_wrak`), een Knuffeltij-knuffel (`tij.strand_1`), een geheim (`knuffel.knuffel_1`, `fles.fles_1`, `schat.mist`, `uitzicht.pierkop`). Tab vult aan. |
| `/guhs vadsrijk z4verhaal status` | Toont zone4_klaar, de zone-5-luchtschipvlag, het bootje en de tellers (knuffeltij x/12, knuffels x/8, flessenpost x/6). |
| `/guhs vadsrijk z4verhaal vergeet` | Wist alle zone-4-quests, knuffels, geheimen en gezien-scènes (om opnieuw te testen). |
| `/guhs vadsrijk z4verhaal opening` / `vlieg` / `mist` / `feest` / `wolkwal` / `cliffhanger` / `wrak` / `zeeslurf` | Speel één scène los af. Let op: `cliffhanger` zet ook zone4_klaar en de zone-5-vlag. |

De algemene questcommando's werken ook: `/guhs vadsrijk npc quest af z4_hq03_waarvandaan` en `/guhs vadsrijk npc tp z4_opvang`.

### De hoofdlijn "De Grote Knuffeltij" (7 stappen, lv 50-55)
Volg steeds de tracker (rechtsboven) en de lichtstraal. Elke stap zegt wie, waar en wat.

#### 1. Lampions aan de kust (`z4_hq01_lampionkust`)
- **Start:** Ordemeester Wasja in de Nestkapel (zone 3).
- **Wat gebeurt er:** na "Ja" vlieg je naar Lampionhaven.
  - De eerste keer zie je een filmpje: door de Wolkenval, over de eilandjes, Hoogvads op de walvis en grauwe vlekjes op het Knuffeltij-strand.
  - Je landt op het luchthavenplatform.
- **Daarna:** praat met **Havenmeester Ankertje** op de kade, bij de middelste steiger.
- **Kijk naar:**
  - Komt het filmpje mooi aan bij Lampionhaven?
  - Staat Ankertje echt op de kade?
  - Op de reiskaart heet zone 4 nu "De Lampionkust" (was "???").

#### 2. De Grote Knuffeltij (`z4_hq02_knuffeltij`)
- Klik op **Botenguh Zeiltje** aan de noordelijke steiger: je krijgt je zeilbootje (of "wordt nog gebouwd").
- Vaar naar het **Knuffeltij-strand** (zuidoost) en praat met **Wasguh Sopje** bij de knuffelwas.
- Loop langs de vloedlijn naar **6 grauwe knuffels** (grijze stofjes in de branding).
  - Elke knuffel vertelt een klein verhaaltje en wordt roze, met hartjes.
  - De teller loopt tot x van de 12.
- Lever in bij **Opvangmoeder Zachtje** in het opvangcentrum.
- Snel testen: `tp tij.strand_1` ... `tij.strand_6`.

#### 3. Waar kom jij vandaan? (`z4_hq03_waarvandaan`)
Er zijn 4 grote knuffels, elk met een **zachte flash van het grauwe oosten**: een grijze titel, grauwe as en 3 herinneringen.
- Strandfeesteiland zuidkust: de walrus. Posters "MAGER IS MOOI", de zuigende buis.
- Bloemeneiland westkust: de giraf. De lange grauwe weg, karren vol warme dingen, de burcht.
- Koraalbaai zuidring: de kwal. De Grauwe Vloot, de kapitein met ringen om haar tenen.
- Op de schildpad: de draak. De troonzaal, vijf vingertjes tik-tik-tik, "Meer."

Daarna praat je met **Opa Lichtstraal** onder de vuurtoren. Hij zegt: "ze komen uit de Grauwe Mistbank".

Snel testen: `tp poi.z4_grote_knuffel_1` tot `_4`.

#### 4. De Grauwe Mistbank (`z4_hq04_mist`)
- Ga naar de **grauwe poort op het Mysterie-eiland** (zuidoost).
  - Je ziet de **mistflash**: een grauwe weg, Mika-karren, de zuigende buis, en een burcht als een kom onder een maan die kijkt.
- Praat met **Verdwaalde Nevel** (een vluchteling). Hij vertelt over de Grauwe Vloot en de Ringvinger.
- Lever in bij **Zachtje**: "Er zijn knuffels gestolen van de waslijn!" Stap 5 start vanzelf.

#### 5. De Vliegervanger (`z4_hq05_vliegervanger`)
- Ga naar het **Wrak** ten noorden van de Koraalbaai en dan het ruim in.
  - Met de bazen-slice: het gevecht tegen Kootje.
  - Zonder: een grappig gesprekje (zijn vlieger zit vast aan zijn eigen staart, plons).
- Pak de **2 gestolen knuffels** in het ruim. Dat zijn nummer 11 en 12: "Alle twaalf knuffels zijn weer roze!"
- Lever in bij **Zachtje**.

#### 6. Het terug-naar-huis-feest (`z4_hq06_feest`)
- Haal de spullen voor het feest:
  - lampions bij **Gloeiwiek** (Lampionhaven, westkant van het plein);
  - vliegers bij **Wimpel** (Vliegereiland);
  - limonade bij **Bubbel** (Strandfeesteiland).
- Ga naar de **feestplek** op het Knuffeltij-strand. Het feestfilmpje begint:
  - lampions, 12 vliegers met knuffels, en Grijsje vindt haar knuffel Muis terug;
  - Opa Dun lacht;
  - Zachtje zegt: "Welkom thuis".
- Meteen daarna **zakt Moeder Wolkwal** laag over de zee. Je hoort een walvislied en ziet een moe oog en een grauw sliertje.
- Lever in bij **Havenmeester Ankertje**.

#### 7. De Opstijgpier (`z4_hq07_opstijgpier`)
- Vaar naar de **Pierkop** (oost). Praat met **Pierwachter Wolkje** en loop de pier op naar **Kapitein Hemelhoog**.
- Lever in. Na het gesprek volgt de **cliffhanger**:
  - het luchtschip stijgt op langs het oog van de walvis, en je ziet Hoogvads;
  - terug op de pier dwarrelt uit de mouw van een guh met een wit-gouden cape een **grauw pluisje** (een hint naar de spion van zone 5);
  - daarna verschijnt "Wordt vervolgd...".
- **Kijk naar:** `status` geeft nu `zone4_klaar=true` en `z5_luchtschip_open=true`.

### Zijquests (16)
| Id | Bij wie / waar | Wat (en hoe snel testen) |
|---|---|---|
| `z4_s01_vlieger` | Wimpel, Vliegereiland | 4 vliegeronderdelen op het eiland (wolkjes). Je krijgt een guh-vlieger. |
| `z4_s02_wedstrijd` | Zwiep, wedstrijdpaal op de top | Blijf 30 s met de vlieger in je tas bij de paal: windstoten, en een titel als je wint. Loop je weg, dan begin je opnieuw. |
| `z4_s03_vissen` | Visser Dobber, zuidsteiger | Vang 5 vissen met een hengel, ergens in zone 4. |
| `z4_s04_parels` | Duikmeester Parel, duiksteiger Koraalbaai | 4 parels onder water (`tp poi.z4_grotje_1`). |
| `z4_s05_zeeslurf` | Parel | Duik naar grotje 1: de Grauwe Zeeslurf. Zonder bazen-slice: een gesprekje met roze bellen. |
| `z4_s06_volleybal` | Smash, Strandfeesteiland | Een klikspel: raad links, midden of rechts. **Tip: het wiebelende oortje wijst de kant aan** (bij "midden" fluit hij). Wie 3 punten heeft, wint. |
| `z4_s07_limonade` | Bubbel | 6 schelpjes op de stranden van het Strandfeesteiland. |
| `z4_s08_vuurtoren` | Opa Lichtstraal | Klim de vuurtoren op en blijf 5 s bij de lens. Hij vertelt over de Grauwe Vloot en een Mika die met één vinger wees. |
| `z4_s09_bijen` | Imker Nectar, Bloemeneiland | 5 bijtjes. |
| `z4_s10_kransen` | Madelief, Bloemeneiland | 6 bloemen. Je krijgt een bloemenkrans. |
| `z4_s11_schildpad` | Kluizenaar Schelpie, op de schildpad | 5 zeewier van zone-4-wezens (40% kans per wezen). |
| `z4_s12_mistbrieven` | Verdwaalde Nevel | 3 briefjes in de mist. Je leest zijn vluchtdagboek. |
| `z4_s13_soep` | Soepguh Lepeltje | Breng 4 kommen soep naar Grijsje, Opa Dun, Vaaltje en Mama Pluis. |
| `z4_s14_lampionnen` | Gloeiwiek | Hang 5 lampions op bij de lege haken in Lampionhaven (gouden glinsters). |
| `z4_s15_strandavond` | Confetti, kampvuur Strandfeest | Blijf 10 s bij het vuur. Je danst. |
| `z4_s16_flessenpost` | Grijsje (na stap 5) | Vaar met haar flesje naar de Kringstroom ten zuiden van het strand. |

**Hoe snel testen:** start een quest met `quest <id> start`, tp naar de plek en loop erheen. Of zet alles klaar met `quest <id> klaar` en lever in.

### Geheimen (Verkenningsboek, toets B, pagina "De Lampionkust")
- **8 knuffels** (hartjes): `tp knuffel.knuffel_1` t/m `_8`.
- **6 flessenpost** (blauwe glinsters): lore over de Keizer, Nooitgenoeg en de Ringvinger. `tp fles.fles_1` t/m `_6`.
- **4 schatten:** `schat.duintop`, `schat.lagune`, `schat.vliegertop`, `schat.mist`.
- **4 uitzichtpunten:** de Boom achter je, Hoogvads voor je, de Mistbank. `uitzicht.duin`, `uitzicht.vlieger`, `uitzicht.vuurtoren`, `uitzicht.pierkop`.
- **Verder in het boek:**
  - de bazen (Zeeslurf, Vliegervanger);
  - de kampvuren van 8 eilanden;
  - de gebieden;
  - een titel "Kenner van de Lampionkust".

### Diensten in Lampionhaven
| NPC | Dienst |
|---|---|
| Luchthavenguh Windvaan (luchthavenplatform) | Reiskaart en vluchten terug |
| Bankier Muntschelp | Kluis (rol `bank`) |
| Werkmeester Krul | Werkplaats (rol `werkplaats`, van t-ambacht) |
| Winkelguh Strandtas | Winkel |
| Smidguh Ankerslag | Smid |
| Harpoentje | Wapens |
| Zeemeeuw | Kleermaker |
| Schelpje | Markt |
| Kok Garnaal (De Blije Bot) | Gratis vissoep, 1x per 2 min |
| Bubbel | Limonade |
| Lepeltje | Soep |

**Kustwacht:** bij elk kampvuur staat een kustwacht.

### Wat moet je in de dev-client vooral checken
1. **NPC-plekken op de echte kaart:**
   - Staat niemand in een muur of in het water?
   - Staan de vluchtelingen bij het opvangcentrum?
2. **De knuffelplekken langs de vloedlijn:** liggen er echte grauwe knuffelblokjes? Dat is van t-z4wereld.
3. **De camerapaden:**
   - opening, mist, feest, wolkwal, cliffhanger;
   - kijkt de cliffhanger echt naar de walviskop?
4. **De tracker:** wijst hij vanuit zone 3 eerst naar Kapitein Zoemzeil, en in zone 4 naar het juiste eiland?
5. **De reiskaart:** staat "De Lampionkust" open na zone 3 en vlieg je erheen?


## Spiekbrief T-Z4BOOT: je eigen guh-zeilbootje (zone 4, De Lampionkust)

Alle commando's werken met op 2. In het Vadsrijk testen (`/guhs vadsrijk` brengt je erheen).

### 1. Snel varen (overal op water, ook zonder zone 4)
```
/guhs vadsrijk boot varen
```
- Geeft je het bootje (+ questvlag) en zet **proefvaart** aan: dan mag het bootje overal op water. Het verschijnt op het dichtstbijzijnde open water (3x3 water met lucht erboven, binnen 6 blokken) en je zit er meteen in.
- Geen water in de buurt? Dan krijg je "Hier is geen plek voor je bootje". Zoek een meertje of de zee op en probeer opnieuw.
- **Wat je moet zien:** een roze bootje met een guhgezichtje op de boeg (met oortjes), een zeil met een guhgezicht en oortjes bovenop, een hartjeswimpel bovenin de mast, een guhkopje als boegbeeld, een klein guh-matroosje voorin met een matrozenpetje, en achterin een lantaarntje aan een paaltje dat over het water hangt (gloeit). Jij zit achterin op het bankje bij het roer.

### 2. Besturing (het "zeilgevoel")
- **W** = zeil hijsen: het bootje komt langzaam op gang (ongeveer 3 seconden tot volle vaart, ~12 blokken per seconde op level 1).
- **Loslaten** = het bootje glijdt nog even door en wordt langzaam trager.
- **S** = remmen, en daarna heel langzaam achteruit.
- **A/D** = de boeg draaien. Je beeld draait mee. Op volle snelheid draai je wat trager.
- Het bootje **dobbert** zachtjes op en neer en schommelt (animatie). Bij snelheid zie je spetters bij de boeg en belletjes in het kielzog, en hoor je af en toe het water.
- **Wind:** rechts op je scherm staat een windpaneeltje met een pijltje (pijl omhoog = wind van achteren), "mee!" / "zijwind" / "tegenwind" en je snelheid in blokken per seconde. Met de wind mee ga je ~15% sneller, ertegenin ~15% trager. De wind draait langzaam mee met de dag.
- Op het strand of op land schuurt het bootje en komt het bijna niet vooruit (het klimt niet op het land).
- `/guhs vadsrijk boot waar` laat zien hoe de wind staat, waar je bent t.o.v. de Kringstroom en of er een steiger in de buurt is.

### 3. Rijdier-toets X en het N-scherm
- **X in het water of op een steiger (zee van zone 4)** = je bootje. **X op het land** = je landrijdier (ook als je het bootje als actief rijdier hebt gekozen).
- Heb je alleen het bootje en sta je op het land: "Je zeilbootje vaart alleen op het water".
- **N** (Rijdieren): het Zeilbootje staat in de lijst (6e rij) met level, xp en snelheid. Varen geeft xp, net als rijden.
- Bovenaan zit een nieuw **tabblad "Zeilbootje"** (tussen Rijdieren en Huisdieren). Ook: `/guhs vadsrijk boot scherm`.

### 4. Aankleden (varianten)
In het tabblad Zeilbootje: links het draaiende bootje, rechts drie rijen: **Zeil**, **Wimpel**, **Boegbeeld**.
- Klik een vakje: het voorbeeld laat het meteen zien. Daarna "Dit wil ik!" (heb je het al) of "Kopen (prijs)".
- Zeilen: roze (heb je meteen), munt / lavendel / zeeblauw (600 brons), honing (900), sterrennacht (beloning).
- Wimpels: hartje (meteen), ster / streepjes (300), Vadsrijk (beloning).
- Boegbeelden: guhkopje (meteen), dolfijntje / sterretje (800), draakje (beloning).
- Alles in één keer: `/guhs vadsrijk boot varianten`. Eén kiezen: `/guhs vadsrijk boot variant zeil_sterrennacht` (of `boeg_draakje`, `wimpel_ster`, ...).
- **Kijk:** als je vaart en iets anders kiest, verandert het bootje meteen.

### 5. Steigers (aanmeren) en uitloggen op zee
```
/guhs vadsrijk boot steigers
/guhs vadsrijk boot naar lampionhaven
```
- `steigers` toont alle aanlegsteigers met afstand (* = je laatste). **Let op:** zolang zone 4 nog niet gebouwd is, zijn dit placeholders (bij de eilandmiddens van de wereldindeling, y 64). Daar is dan nog geen water: proefvaart (punt 1) is dan de manier om te testen.
- Vaar je binnen ~10 blokken langs een steiger: in beeld "Steiger X: druk op Shift om aan te meren". Die steiger wordt je "laatste steiger".
- **Shift** naast een steiger = je stapt op de planken van de steiger (niet in het water). Het bootje poeft weg.
- **Uitloggen op zee** (terwijl je in het bootje zit): bij het inloggen sta je op je laatste steiger, met een berichtje. Snel testen zonder uit te loggen: `/guhs vadsrijk boot thuis`.

### 6. De Kringstroom (de rand van zone 4)
- Een ellips rond alle eilanden (midden 2140, -491; 470 x 290 blokken). In de buitenste rand (de laatste ~7%) duwt de stroom je terug: je snelheid naar buiten valt weg, je boeg draait vanzelf terug naar binnen, je ziet schuimwolkjes en "De Kringstroom duwt je bootje terug naar de eilanden!".
- **Wat moet gebeuren:** je komt er NOOIT doorheen, ook niet met volle zeilen recht naar buiten, en je raakt nooit vast: loslaten = je drijft vanzelf terug naar open water.
- Kan pas echt getest worden als de zee van zone 4 er is (t-z4wereld).

### 7. Geen andere bootjes
- Probeer in het Vadsrijk een gewone eiken boot of bamboevlot te plaatsen: dat gaat niet ("Andere bootjes drijven hier niet: in het Vadsrijk vaar je met je eigen guh-zeilbootje"). Dat geldt ook voor boten van andere mods (alles wat *boat* of *raft* heet).

### 8. Het bootje krijgen (verhaal) en de rest
- `/guhs vadsrijk boot geef`: zoals het verhaal het geeft (questvlag + tip "Je eigen zeilbootje!").
- `/guhs vadsrijk boot fles`: het item "Bootje in een fles" (rechtsklik = het bootje is van jou).
- `/guhs vadsrijk boot status`: bootje, questvlag, op zee, laatste steiger, level, gekozen uiterlijk.
- `/guhs vadsrijk boot proefvaart aan|uit`, `/guhs vadsrijk boot reset` (alles van het bootje weg).
- Gewone rijdier-commando's werken ook: `/guhs vadsrijk mount rijd zeilbootje`.


## T-Z4WEZENS: de wezens, de Grauwe Zeeslurf en het Wrak van de Vliegervanger (zone 4, De Lampionkust)

**Wat is er nieuw?**
- **9 grauwe kustwezens** die je **opvrolijkt** (net als in zone 1): ze zijn grijs en chagrijnig, en na de laatste klap krijg je een
  **roze plof**. Ze worden fel van kleur met een blij gezichtje en blosjes, en huppelen nog ~10 seconden rond.
- **De Mika-vliegerknechten** in het Wrak worden NIET opgevrolijkt: die vluchten in een rookwolkje ("Dit komt in mijn rapport!").
- **Licht gevecht:**
  - weinig wezens (max ~40 tegelijk in de hele zone);
  - elk speciaal trucje heeft een rode of paarse waarschuwing;
  - geen wezens op Lampionhaven, het Knuffeltij-strand, het Strandfeesteiland, het Vuurtoreneiland, het Schildpadeiland en de Pierkop.
- **Wereldbaas De Grauwe Zeeslurf** (lv 54) in de lagune van de Koraalbaai, met duiken.
- **Kleine kerker: het Wrak van de Vliegervanger**, met eindbaas **Kootje de Vliegervanger** (lv 56): een Mika met een oorlogsvlieger.
- **Buit:**
  - de Schelpenset en de Lampionset (lv 50-56);
  - 3 sieraden, 6 klassenwapens en 2 kristallen;
  - 6 benoemde baasitems met een effect;
  - 2 trofeeën en 2 huisdieren (Zeeslurfje en Vliegertje);
  - 5 stukken voor de nieuwe slots (wanten, cape, armband): die verschijnen pas na de merge met t-items.

**Let op:** de plekken komen uit POI_Z4 v2 van t-z4wereld. Die staat in deze branch nog NIET op de classpath, dus in een losse
t-z4wezens-client zijn het placeholders (dezelfde coördinaten). Na de integratie leest de code `Z4WereldPlekken` zelf (via reflectie).
- `/guhs vadsrijk z4wezens spawners` zegt welke van de twee het is;
- `/guhs vadsrijk z4wezens herlaad` leest de plekken opnieuw.

### 1. Alle wezens op een rij (sneller dan zoeken)
```
/guhs vadsrijk z4wezens alle
```
Er komt een zandvloer voor je met alle 10 wezens op een rij (lv 52, ze staan stil).

**Kijk naar:**
- ze zijn grijs, met een boos guh-gezichtje;
- de Lampionkwal heeft een zwak lampje.

**Sla er een paar:** je ziet een roze plof, hartjes en belletjes. Het wezen wordt fel en blij (blosjes), wordt half doorzichtig en
huppelt nog ~10 s. De Vliegerknecht verdwijnt in rook.

**De wezens, met wat ze doen:**

| wezen | waar | trucje (wat je ziet) |
|---|---|---|
| Grauwe Mopperkrab | Koraalbaai, rotsjes | Houdt hij zijn scharen voor zijn gezicht? Dan doen klappen van voren de helft ("Scharen dicht!"). Loop om hem heen. |
| Koraalkobold | Koraalbaai | Houdt ~6 blokken afstand en gooit roze koraalscherfjes. |
| Stekelpluis | Koraalbaai, rotsjes | Begint nooit zelf. Na een klap: een rode cirkel (1 s) en dan prikt hij. |
| Zeurslak | Koraalbaai, Bloemeneiland | Laat een slijmspoor achter (Plakkerig). Onder de helft kruipt hij 2,5 s in zijn huisje ("Verstopt!", geen schade). |
| Grauwe Lampionkwal | Koraalbaai, Mysterie-eiland | Zweeft. Een rode cirkel onder hem (1 s), dan prikken de tentakels + Plakkerig. |
| Mokmeeuw | Vliegereiland, Bloemeneiland | Cirkelt boven je, dan een rode streep (0,9 s) en een duikvlucht. Daarna zit hij 2 s op de grond: je kans. |
| Bromvlieger | Vliegereiland, het Wrak | Een paarse brede streep (0,9 s) = een windstoot die je wegblaast. Komt hij laag, dan zwiept zijn staart. |
| Pruilrob | rotsjes | Trommelt met zijn flippers (rode streep, 1 s) en glijdt dan op zijn buik langs de streep. |
| Mistmopje | Mysterie-eiland | Smelt weg in de mist (niet te raken) en duikt achter je op: een rode cirkel en dan 3 s donker. |
| Vliegerknecht (Mika) | het Wrak | Een rode streep = zijn enterhaak: Plakkerig en een rukje naar hem toe. |

**Losse commando's:**
```
/guhs vadsrijk z4wezens spawn mopperkrab
/guhs vadsrijk z4wezens spawn mokmeeuw 54 true
/guhs vadsrijk z4wezens opruimen
```
- De tweede maakt een **elite** (25 % groter, met een gouden gloed).
- `opruimen` haalt alles binnen 64 blokken weg.

### 2. De eilanden met wezens (spawners)
```
/guhs vadsrijk z4wezens spawners
/guhs vadsrijk z4wezens tp koraalbaai
/guhs vadsrijk z4wezens tp vliegereiland
/guhs vadsrijk z4wezens tp bloemeneiland
/guhs vadsrijk z4wezens tp mysterie
/guhs vadsrijk z4wezens tp rotsjes
/guhs vadsrijk z4wezens tp wrak
```
**Wat je moet zien:**
- per plek 2-3 wezens, rustig verspreid;
- op het Bloemeneiland alleen aan de buitenkant (niet bij de bijen of de picknick);
- bij een kampvuur of in een dorp vergeten ze je en lopen ze terug;
- in het Wrak staan Vliegerknechten in het ruim, in de kombuis, bij de knuffelkooi (onderdeks) en op het dek.

### 3. De Grauwe Zeeslurf (wereldbaas, lv 54)
```
/guhs vadsrijk z4bazen spawn zeeslurf
```
Je staat bij de duiksteiger. De Zeeslurf zwemt in de lagune (het gevecht begint op het vlot).

**Fase 1:**
- **Slurfspuit:** een rode streep vanaf zijn slurf, dan een straal water (duwt je weg).
- **Mokwolk:** 2-3 rode cirkels onder spelers, daarna 3 s donker.

**Fase 2 (66 %), "Het grotje":**
- Hij zakt naar zijn grotje en is **onkwetsbaar** (melding).
- Duik naar de **3 grauwe poliepen** op de bodem en sla ze kapot.
- **Roze ringen** op de bodem geven lucht (en 3 % Vads).
- Elke 6 s trekt een **paarse stroming** zwemmers naar het grotje.
- Zijn alle poliepen kapot, dan komt hij boven: 5 s suf, en je doet +25 % schade.

**Fase 3 (33 %):**
- **Golfslag:** een rode ring van 9 om hem heen. Zwem weg, of duik eronderdoor.
- **Samen aaien:** een gele cirkel. Ga er samen in staan (solo: alleen jij). Dan moet hij giechelen: 4 s suf en +25 %.

**Het einde:** hij wordt **roze**, zingt (muzieknootjes) en blijft 10 s blij drijven. Daarna komt de buitkist.
- Bij de eerste keer: een Legendarisch item + de trofee;
- en het huisdier **Zeeslurfje**.

**Handig bij testen:**
```
/guhs vadsrijk z4bazen fase 2
/guhs vadsrijk z4bazen poliepen
/guhs vadsrijk z4bazen fase 3
/guhs vadsrijk z4bazen reset zeeslurf
```
- `fase 2` zet hem op 66 %.
- `poliepen` slaat alle poliepen in één keer kapot.
- `fase 3` zet hem op 33 %.
- `reset zeeslurf` zet hem terug met volle Vads.

**Geen lagune in je testwereld?**
```
/guhs vadsrijk z4bazen bouw
```
Dit bouwt een testlagune (zand, water tot y 62, een vlot en een grotje) en een noodek voor het Wrak. Terugzetten met
`/guhs vadsrijk z4bazen herstel`. **Alleen in een testwereld gebruiken.**

### 4. Het Wrak van de Vliegervanger (kleine kerker)
```
/guhs vadsrijk z4bazen tp wrak
/guhs vadsrijk z4bazen spawn vliegervanger
```
- **De route:** het dek op, door het luik naar beneden (het ruim, de kombuis, de knuffelkooi, met Vliegerknechten), en dan naar het
  voordek: daar staat **Kootje de Vliegervanger** (lv 56).
- **Fase 1:**
  - **Knuffelnet:** een rode streep, dan een net (Plakkerig).
  - **Kaasbom:** 2 rode cirkels.
  - Op 80 % fluit hij 2 Vliegerknechten erbij.
- **Fase 2 (60 %), "Opstijgen!":** hij hangt onder zijn **oorlogsvlieger**, 9 blokken boven het dek, en is **onkwetsbaar**.
  - Op het dek staan **3 lieren** met een paars vlaggetje: **rechtsklik elke lier 3 keer** ("Lier: 1/3").
  - Je ziet paarse touwtjes van de lieren naar de vlieger.
  - Ondertussen gooit hij knuffelzakken (rode cirkels) en komen er windvlagen (een paarse streep die je wegduwt).
  - Zijn alle 3 lieren binnen: "KRAK!", de vlieger stort neer. Hij ligt 6 s plat en je doet +50 % schade.
- **Fase 3 (25 %), "Knuffelregen":** er vallen gestolen knuffels uit zijn zak, met een gele cirkel. Vang ze SAMEN op
  ("Knuffel gered! 1/3").
- **Het einde:** hij vlucht in een rookwolk ("Dit komt in mijn rapport!"). Daarna komt de buitkist, het huisdier **Vliegertje** en een
  verhaalmelding voor t-z4verhaal.

**Handig bij testen:**
```
/guhs vadsrijk z4bazen fase 2
/guhs vadsrijk z4bazen lieren
/guhs vadsrijk z4bazen fase 3
/guhs vadsrijk z4bazen reset vliegervanger
```
`lieren` draait alle 3 lieren in één keer binnen.

### 5. De buit
```
/guhs vadsrijk z4wezens set schelpen
/guhs vadsrijk z4wezens set lampion
/guhs vadsrijk z4wezens sieraden
/guhs vadsrijk z4wezens wapens
/guhs vadsrijk z4wezens kristallen
/guhs vadsrijk z4wezens loot mokmeeuw 54 true
/guhs vadsrijk z4bazen items
/guhs vadsrijk z4bazen loot zeeslurf
```
- **Schelpenset** (Kracht/Behendigheid/Verdediging):
  - (2) +12 Verdediging;
  - (4) **Schade tegen bazen +8%**.
- **Lampionset** (Wijsheid):
  - (2) +8 Wijsheid;
  - (4) +10 Heal-kracht, en je geeft zelf licht in het donker.
- **Kijk bij de sets naar:** de tooltip toont "Set: Schelpenset (n/4 gedragen)" en de bonussen worden groen.
- **Sieraden:** Parelring, Schelpenketting, Lampionoorbel.
- **Wapens:**
  - Ankerknots en Golfbreker (Ridder);
  - Vliegerboog en Harpoenkruisboog (Jager);
  - Lampionstaf en Koraalstaf (Magier).
- **Kristallen:** Parelkristal (+Vads) en Zeeglas (+Elementweerstand). Elk wezen in zone 4 laat ze soms vallen.
- **Baasitems (met een effect):**
  - Slurfstaf: +15 % schade op wezens in het water;
  - Zeekoeienhuid: in het water 15 % minder schade;
  - Lagunelampion (Guhddelijk, 2 %): je geeft licht in het donker;
  - Oorlogsvliegerboog: +20 % op vliegende wezens;
  - Kootjes Enterhaak: +15 % op Plakkerige wezens;
  - Kaasdoek: onder 30 % Vads 3 s sneller lopen.
- **Na de merge met t-items** komen er 5 stukken voor de nieuwe slots bij:
  - Schelpenwanten (telt mee voor de Schelpenset);
  - Lampioncape (telt mee voor de Lampionset);
  - Koraalwantjes, Vliegercape en Zeeglasarmband.

### 6. Wat nog NIET kan / let op
- Het uiterlijk is niet in een client bekeken: **beoordeel de modellen zelf** (schattig genoeg?).
- De VFX-kit van t-vfx (cast-balk, wind-up) werkt via reflectie en pas na de merge. Zonder de merge zie je de gewone gekleurde stofcirkels.
- Het Schildpadeiland heeft geen wezens; de schildpad zelf is van t-z4wereld.


## Zone 4: Ouwe Dobber (de reuzenschildpad) en de guh-vliegers (T-Z4DIEREN)

### Ouwe Dobber, de reuzenschildpad
Een grote, lieve schildpad (schild van 14 blokken, guh-oortjes, bloemetjes op zijn rug) die langzaam zijn rondje zwemt
langs het Schildpadeiland. Bij de steiger van het eiland wacht hij steeds 30 seconden, daarna zwemt hij een rondje van
ongeveer 2,5 minuut. Het eiland zelf (met het hutje en Schelpie) blijft gewoon liggen (dat is Oma Dobber, die slaapt).
Schelpie, de grote knuffel en de zeewier-quest blijven op het eiland; Ouwe Dobber komt elk rondje 30 s naast de steiger liggen.

| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z4dieren schildpad tp` | Je staat op het eind van de steiger van het Schildpadeiland en kijkt naar zijn halte. |
| `/guhs vadsrijk z4dieren schildpad halte` | Ouwe Dobber springt naar de halte en wacht daar 30 s (handig om op te stappen). |
| `/guhs vadsrijk z4dieren schildpad rijd` | Je wordt meteen op zijn schild gezet, waar hij nu ook zwemt. |
| `/guhs vadsrijk z4dieren schildpad route` | 20 s lang zie je zijn hele route als lichtpuntjes (groen = de halte), plus info in de chat. |

Wat je moet zien / proberen:
- `schildpad tp` en dan `schildpad halte`: hij ligt naast de steiger. Klik op hem: "Ouwe Dobber neemt je mee op zijn rondje!".
  Je zit op zijn schild. Na 30 s zwemt hij weg ("dobber, dobber"); de vinnen peddelen en de kop knikt.
- Met z'n tweeen/meer: er zijn 8 plekjes op zijn schild. De 9e krijgt "Ouwe Dobber zit vol".
- Sluipen op zee: je plonst naast hem in het water. Klik weer op hem om op te stappen.
- Sluipen bij de steiger: je staat netjes op de steiger.
- Duik onder water bij hem tot je lucht bijna op is: hij vist je eruit en zet je op zijn rug.
- Uitloggen op zijn rug en weer inloggen: je staat op de steiger.
- Het ritje moet soepel zijn (geen schokken), ook met meer spelers. Hij zwemt nergens door het eiland of de steiger heen.

### De guh-vliegers
Echte bewegende vliegers met oortjes en een gezichtje, aan een touwtje naar een paaltje: 14 op het Vliegereiland, de grote
wedstrijdvlieger aan de wedstrijdpaal, en 3 kleintjes op het dak van de vliegerwinkel in Lampionhaven.

| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z4dieren vliegers tp` | Naar het Vliegereiland. |
| `/guhs vadsrijk z4dieren vliegers uit` | Alle vliegers weg (om te vergelijken / als het te druk is). |
| `/guhs vadsrijk z4dieren vliegers aan` | Ze komen terug (binnen een seconde). |
| `/guhs vadsrijk z4wereld plek gebouw.vliegerwinkel.deur` | De vliegerwinkel in Lampionhaven: kijk omhoog naar het dak. |

Wat je moet zien:
- De vliegers dobberen, zwaaien en kantelen een beetje in de wind, elk op een eigen ritme. Ze kijken naar het paaltje.
- Dichtbij: een staart met gekleurde strikjes die wappert, en een touw dat een beetje doorhangt.
- Ver weg (meer dan 64 blokken): geen staart meer, wel nog de vlieger en het touw (dat scheelt werk voor je pc).
- Geen wol-vliegers van blokken meer op de kaart.


# 5. ZONE 5: HOOGVADS (lv 56-60)


## Zone 5 "Hoogvads" op Moeder Wolkwal (T-Z5WERELD)

De hoofdstad ligt op de rug van de walvis, BOVEN het oosten van zone 4 (kop naar het westen, boven de Opstijgpier).
Alle teleports: `/guhs vadsrijk z5wereld plek <id>` (tab vult de ids aan; alle ids staan in `POI_Z5.md`).
Het kost ~10 s voor de tegels om een wijk heen geladen zijn: even rondkijken voor je oordeelt.

### Aankomst en luchthaven
- `/guhs vadsrijk z5wereld plek luchthaven_station` - station met glazen tongewelf: banken, balie van de havenmeester (bel, logboek), kratten in de hoek met een open luikje (de losse plank).
- `/guhs vadsrijk z5wereld plek haven_z5_z4_vertrek` - eind van pier 2 (lijn D): 2 meermasten, poortjes, bel. Kijk terug: poorten door de stadsmuur op alle 3 pieren, 2 vaste luchtschepen naast pier 1 en 3, vertrekborden bij de pierkoppen.
- Wolkvangnet: `/guhs vadsrijk z5wereld vangnet` zet je net voorbij de pier in de lucht. Je valt, en een wolkje zet je terug (paarse melding "Een zacht wolkje vangt je op..."). Ook van een waterval of een vin.

### Ordekathedraal
- `/guhs vadsrijk z5wereld plek kathedraal_plein` - het plein met de westgevel: de Grote Poort is nu ~57 hoog (walvis 20 lager, kathedraal hoger). Loop de trap op en door de open gouden deur.
- `/guhs vadsrijk z5wereld plek kathedraal_schip` - binnen: banken, gouden ribben, kathedraalglas met gouden lood, kroonluchters.
- `/guhs vadsrijk z5wereld plek kathedraal_altaar` - de kruising met het altaar en de gouden koepel; de apsis met "de Eerste Knuffel".
- `/guhs vadsrijk z5wereld plek kathedraal_geveltoren_voet` - wenteltrap in de zuidelijke geveltoren tot de klokkenvloer (`kathedraal_geveltoren`): uitzicht door de galmgaten.

### Marktlaan en Grote Soepkeuken
- `/guhs vadsrijk z5wereld plek markt_fontein` - kraampjes (6 soorten), parasols, lampionslingers, de fontein met het guhbeeld.
- `/guhs vadsrijk z5wereld plek soepkeuken_ketel` - reuzenketel, lange tafels. Achterin het open luikje bij `z5w_buik_luik`: een ECHTE ladder omlaag naar de Buikgangen (t-z5wezens' kerker in de walvis).

### Paleisheuvel
- `/guhs vadsrijk z5wereld plek paleis_trap` - de monumentale trap omhoog (sierlijke keermuren).
- `/guhs vadsrijk z5wereld plek raadszaal_ingang` - de Raadszaal: gouden deuropening, lessenaar op een podium, banken, zijkamer met deur (`raadszaal_zijkamer`).
- `/guhs vadsrijk z5wereld plek paleis_poort` - via de poort naar de troonzaal (`troonzaal_audientie`): loper, zuilen, troon met baldakijn.
- `/guhs vadsrijk z5wereld plek paleis_kanselarij` - de Kanselarij (gang met deur vanuit de troonzaal): bureau, kasten, kaartentafel.

### Universiteit en Gildehuis
- `/guhs vadsrijk z5wereld plek universiteit_poort` - door de poort (trap omlaag) naar de binnenhof met kloostergang en fontein.
- `/guhs vadsrijk z5wereld plek universiteit_bibliotheek` - 2 verdiepingen hoge bibliotheek, leestafels.
- `/guhs vadsrijk z5wereld plek universiteit_archief` - archief + collegezaal in de westvleugel.
- `/guhs vadsrijk z5wereld plek gilde_hal` - het Gildehuis (oostvleugel): hal + keuken, brouwerij, smidse, atelier, bijenstal.
- `/guhs vadsrijk z5wereld plek universiteit_sterrenwacht` - deur in de noordvleugel, wenteltrap tot boven (`universiteit_sterrenwacht_top`).
- Kijk ook van buiten: steunberen tussen de ramen, vensterbanken, druiplijsten, bloembakken, 4 erkers (minder vlakke muren).

### Academie van de Wolkwacht
- `/guhs vadsrijk z5wereld plek academie_poort` - trap omlaag door de poort naar het oefenplein (`academie_oefenplein`), ridders in formatie.
- `/guhs vadsrijk z5wereld plek academie_kazerne` - de donjon: hal van de kapitein, bedden, kastjes.
- `/guhs vadsrijk z5wereld plek academie_toren_voet` - deur in de oosttoren, ladder omhoog naar een uitkijkbalkon (`academie_toren`).
- `/guhs vadsrijk z5wereld plek z5w_proefarena` - de Proefarena (zand, hekring met 4 openingen).

### Huizen, herberg, bank, winkels
- `/guhs vadsrijk z5wereld huis herberg_hemelbed`, `huis bank_hoogvads`, `huis winkel_smid` (en `winkel_kleding/bakker/wapens/kruiden/juwelen/kaarten/speelgoed`, `woning_1..`).
- Elk huis: echte deur, begane grond met houten vloer, lamp, meubels; trapje van de deur naar de straat. Bovenverdiepingen zijn dicht.

### Parken, rand en walvis
- `/guhs vadsrijk z5wereld plek staartpark_prieel` - vijver, brug, gouden prieel, bloesembomen.
- `/guhs vadsrijk z5wereld plek staartpark_westeinde` - het pad met hekjes over de staartsteel naar de staartvin: Donderbuik-arena (`z5w_donderbuik_arena`) en Staartvlakte (`z5w_staartvlakte_noord`) op wolkplatforms.
- `/guhs vadsrijk z5wereld plek z5w_lift_zuidvin_boven` - luikje binnen de muur: ladder door de flank naar de zuidvin (`z5w_zuidvin`, wolkplatform). Idem `noordvin`.
- `/guhs vadsrijk z5wereld plek ooggaarde_tuin` en `balkon_oog` - tuin aan de kop, balkon boven het oog met de oortjes.
- Van buiten bekijken: `/tp @s 2560 260 -760` (naast de kop: oog, oortje, vin), `/tp @s 3060 260 -520` (achter de staart). Huid met vlekken, wolkpokjes en wolkbolletjes, watervallen langs de flanken (met kettingrooster in de muur).

### Wijken, biomen, bewoners
- `/guhs vadsrijk z5wereld wijk` - zegt de wijk + kern-gebied waar je staat; F3 toont "Het Vadsrijk - Hoogvads - ..." (onder de walvis is het weer zone 4).
- `/guhs vadsrijk z5wereld bewoners` (aan/uit) - de guhs van de renders lopen niet, ze staan/zitten als decor als je dichtbij bent (max 160).

### Nieuwe blokken
- Creative-tab "Guhs: Bouwen", achteraan: roze marmer, wolkmarmer (+ gepolijst/trap/plaat/muurtje/zuil), guhgezicht, gouden kroonlijst, zuilkop, gouden dakpannen, kathedraalglas (4), walvishuid (4), Hoogvads-vaandel. Allemaal in de stad gebruikt.

### Bekend / niet gedaan
- Geen eigen Wolkwacht-ridder-model (ridders = guh met wachter-textuur), geen bewegende vinnen, geen panorama op de uitkijkpunten.
- De troon zelf is decor (de koningin staat erop via het verhaal).


## Spiekbrief zone 5 "Hoogvads": het verhaal (t-z5verhaal)

Hoogvads is de feestelijke hoofdstad op de rug van Moeder Wolkwal (lv 56-60). Het is er veilig. Het hoofdverhaal heet **"Verraad in de stad"**: een speurdersverhaal over een Mika-spion in de Hoge Raad. De walvis is de inzet.

> **Let op:** de stad zelf (de blokken) bouwt t-z5wereld later. Tot die tijd staan de NPC's op **voorlopige plekken** (ze zweven als er nog geen vloer is). Het verhaal en de dialogen kun je al helemaal spelen met de commando's hieronder.

Alle commando's beginnen met `/guhs vadsrijk z5verhaal`. Je moet in het Vadsrijk zijn voor `tp` en `vlieg`.

### 1. Klaarzetten
| Commando | Wat er gebeurt |
|---|---|
| `/guhs vadsrijk z5verhaal klaarzetten` | Zone 1-4 klaar (alle hoofdquests + vlaggen `zone1..4_klaar`), level 56 (zolang de levelgrens nog 50 is: level 50). **z5_hq01 "De Zwevende Stad"** start vanzelf. |
| `/guhs vadsrijk z5verhaal vlieg` | De vlucht naar Hoogvads. De eerste keer: de **aankomstfilm** (vanaf de Opstijgpier langs de kop van de walvis, haar oog gaat open en lacht, over de rand, de stad in het goud, landing op de Luchthaven). |
| `/guhs vadsrijk z5verhaal quest <1-10>` | Spring naar hoofdquest 1-10 (alles ervoor wordt afgevinkt, ook de sporen in je boekje). |
| `/guhs vadsrijk z5verhaal stap` | Vinkt de huidige stap van je zone-5-quest af (overslaan). |
| `/guhs vadsrijk z5verhaal tp <naam>` | Naar een NPC of plek. Tab geeft de lijst, bv. `koningin`, `inspecteur`, `havenmeester`, `zachtpels`, `mientje`, `spuitgat`, `ooggaarde_balkon`. |
| `/guhs vadsrijk z5verhaal vergeet` | Wist alles van zone 5 (quests, sporen, geheimen, gezien-films). |

### 2. Het hoofdverhaal (10 stappen)
| # | Quest | Wie / waar | Wat moet er gebeuren |
|---|---|---|---|
| 1 | De Zwevende Stad | Havenmeester Windvaan (Luchthaven) → Koningin Zachtmoed (troonzaal) | Aankomstfilm, meld je bij de havenmeester, loop de Zachtmoedlaan op, de trap op naar de troonzaal. Je krijgt een stukje kaas. |
| 2 | Verraad in de stad | Koningin → Inspecteur Snuffelaar (zijkamer Raadszaal) | De gescheurde bladzijde is gestolen. Je krijgt het **Speurdersboekje**. Lessenaar onderzoeken (spoor: grijze haartjes), **verhoor** Raadsvrouwe Inktvlek. |
| 3 | De mol in de Wolkwacht | Kapitein Veerkracht (Academie) | Kastjes van de kadetten (spoor: bevelbriefje met pootjesstempel), **verhoor** Kadet Plukje. |
| 4 | Soep en geruchten | Kok Pollepel (Grote Soepkeuken) | **Verhoor** de kok, deel 4 kommen soep uit (klik op de 4 vluchtelingen in de rij), **verhoor** Mager Mientje → de **herinnering** (flash F3: haar vlucht uit Grauwhoven). |
| 5 | Wie vertrekt er stiekem? | Havenmeester → nachtlogboek → Raadsheer Gouddraad (bank) | Spoor: het logboek ("Z.P." naar de Stormrand), **verhoor** Gouddraad (zegel van de Kanselarij). |
| 6 | De laatste twee verhoren | Generaal Donsveer (toren Academie) + Kanselier Zachtpels (Kanselarij) | **Verhoren** in willekeurige volgorde. Bij Zachtpels: de kaastest! |
| 7 | Het bewijsbord | Inspecteur Snuffelaar (of rechtsklik op je boekje) | **Beschuldig de verrader.** Fout = uitleg, geen straf. Goed = de **ontmaskering** in de Raadszaal (een rits in een guh-pak: Kootje Zilverstaart van de Ringvinger). |
| 8 | De Wolkwaldroom | Het middelste spuitgat | Achtervolging; de **finale**: Zilverstaart verslaan + de Grauwe Droom van Moeder Wolkwal opvrolijken. Daarna leest de Koningin de bladzijde voor: de legende van de Keizer en de Lege Schaal. |
| 9 | Het slaaplied | Balkon van de Ooggaarde | Zing het slaaplied: Moeder Wolkwal wordt wakker en knipoogt. Titelkaart **"Ridder van Hoogvads"**, vlag `zone5_klaar`. |
| 10 | De Stormrand | Kanselarij → oostpier → Havenmeester | De **Grauwe brief** "Aan de Ringvinger", en de **cliffhanger**: een bliksemmuur, de Grauwe Vloot, De Gouden Greep, tien ringen aan tien tenen. "Wat een mooi lampionnetje, die walvis. Die wil ik." |

**Waar je op let:** de tracker (rechtsboven + lichtstraal) wijst elke stap aan met wie/waar/wat. Na stap 7 is Kanselier Zachtpels voor jou een "kartonnen Kanselier" (grapje, omdat NPC's voor iedereen zichtbaar blijven).

### 3. Speurwerk los testen
| Commando | Wat je ziet |
|---|---|
| `/guhs vadsrijk z5verhaal verhoor <wie>` | Een verhoor met keuzes: `inktvlek`, `plukje`, `kok`, `mientje`, `gouddraad`, `donsveer`, `zachtpels`. Per verhoor levert één vraag het spoor op ("Nieuw spoor in je Speurdersboekje: ... (n/10)"). Je mag alle vragen stellen; daarna verschijnt "Dat was het. Dank u." |
| `/guhs vadsrijk z5verhaal scene bord` | Het **bewijsbord**: pagina 1 de sporen (✔ of ???), pagina 2 de verdachten (vrijgepleite krijgen "(vrijgepleit)"), pagina 3 als de 3 doorslaggevende sporen er zijn. |
| `/guhs vadsrijk z5verhaal bewijs` | Alle 10 sporen in je boekje (om het bord vol te zien). |
| `/guhs vadsrijk z5verhaal quest 7` en dan het bord | Nu staan er knoppen "Beschuldig ...". Probeer eerst een foute (uitleg met het bewijs dat hem vrijpleit), dan Zachtpels → ontmaskering. |

### 4. Losse scènes
`/guhs vadsrijk z5verhaal scene <naam>` met: `aankomst`, `herinnering` (F3), `ontmasker`, `spion` + `droom` (de finale zonder baasgevecht), `ontwaken`, `brief`, `cliffhanger`, `klaar` (titelkaart + vlag).

`/guhs vadsrijk z5verhaal finale` start de volgende fase van de finale (tijdens quest 8, na het spuitgat). Zonder de bazen van t-z5wezens speelt er een filmpje dat de fase zelf afrondt.

### 5. Zijquests (11)
| Quest | Bij wie | Wat |
|---|---|---|
| Een kom voor iedereen | Kok Pollepel | 8 wolkbessen (roze glinsters) in het Staartpark en de Ooggaarde |
| Pluisje's knuffel | Pluisje (soepkeuken) | Knuffelkonijn Flap achter het beeld van de Eerste Knuffel (kathedraal) |
| Brieven naar huis | Opa Grauwsnor | 3 brieven: havenmeester, Zuster Zachtzang, Pluisje |
| Het Oog meten | Professor Brilsma | De sterrenwacht van de universiteit |
| De adem van Moeder Wolkwal | Professor (na quest 9) | 3 spuitgaten |
| Proef: het Wolkenparcours | Kapitein Veerkracht | 4 palen op het oefenplein |
| Proef: de verdwaalde kadet | Kapitein Veerkracht | Kadet Dons van de Luchthaven naar de Academie begeleiden (hij loopt mee) |
| Marktdrukte | Kraamguh Lientje | Pakjes naar bakker, kleermaker, juwelier |
| Water voor het Staartpark | Tuinguh Stekje | 4 bloesembomen |
| De kroon van wol | Hofdame Breinaald | 3 bolletjes roze wol (markt, paleishof, universiteit) |
| Het lied van de Eerste Knuffel | Zuster Zachtzang (na quest 9) | 2 roosvensters + het beeld |

### 6. Stad, teasers en ambachten
- **4e klasse (alleen teasers, niet speelbaar):** Vadszanger Lieflied bij de kathedraaltrappen, Pluisninja Schaduwdons bij het oefenplein. Klik → "Wat staat er in de vitrine?" → de Guhfluit / de Pluisdolkjes, "Binnenkort." (`tp vadszanger`, `tp pluisninja`)
- **Ambachtsgilde** naast de universiteit: Gildemeester Duimdop + leermeesters koken, brouwen, smeden, naaien, imkeren. Nu nog een "binnenkort"-zinnetje; de ambachten-slice kan ze later koppelen. (`tp gildemeester`)
- **Winkels:** bakker (drankjes), kleermaker, smid, juwelier, Lientje (veilinghuis), stalmeester bij de Luchthaven.
- **Reizen:** Havenmeester Windvaan → reiskaart (Hoogvads staat er nu op, rechtsboven) en vluchten naar alle eilanden. Na zone 4 staat "Naar Hoogvads" ook bij de andere kapiteins.

### 7. Verkenningsboek (toets B), pagina "Hoogvads"
Hoofd- en zijquests, geheimen (3 schatten + de Grauwe brief), 8 knuffels, de bazen (Zilverstaart, de Grauwe Droom), 4 uitkijkpunten (Ooggaarde-balkon, Geveltoren, Staartvin, Sterrenwacht: panoramafilmpje), 4 kampvuren (Luchthaven, markt, Staartpark, Ooggaarde) en 5 gebieden. Titel bij 100%: **Kenner van Hoogvads**.

### 8. Wat (nog) niet kan
- De stad, de kampvuurblokken en de vitrines bestaan pas als t-z5wereld ze bouwt. De plekken zijn nu schattingen uit de concept-renders.
- De echte finale-gevechten zijn van t-z5wezens; tot dan speelt een filmpje.
- De herinnering van Mientje (F3) is een verteld filmpje, nog niet speelbaar (dat kan pas met een kaart van het keizerrijk).


## Zone 5 "Hoogvads": gevechten (T-Z5WEZENS)

De stad zelf is veilig. Je vecht op vier afgesloten plekken: de **Proefarena van de Wolkwacht** (bij de Academie), de **Buikgangen** (in de walvis, via een luik in de kelder van de Soepkeuken), de **Wolkrand** (de vinnen en de staartvin, via wolkenliften) en het geheime **Schuilnest** (onder de Luchthaven, achter een losse plank).

> Alle plekken zijn nog **placeholders** tot t-z5wereld de stad bouwt. Staat er nog geen stad/vloer? Typ eerst:
> `/guhs vadsrijk z5wezens bouw alles`. Dat bouwt de Buikgangen, het Schuilnest en witte stand-in-vloeren. Het zet alleen blokken in lucht, dus de stad wordt nooit overschreven.

### 0. Snel overzicht
- `/guhs vadsrijk z5wezens alle` zet alle 9 wezens op een rij (zonder AI, de vermomde ook zichtbaar).
  - Kijk naar: de bezeten stropop met grijs pluis, de ridder-guh met kussenzwaard en roze pluim, de Mika's (saboteur met moersleutel, lontlegger met kaasbommen, harpoenier), de grijze pluismijt, het wolkduiveltje met hoorntjes, de mistsluier en de Mika in Wolkwacht-kostuum.
- `/guhs vadsrijk z5wezens plekken`: alle plek-ids met coördinaten.
- `/guhs vadsrijk z5wezens spawners`: aantallen per plek (max ~50 levend in heel zone 5).
- `/guhs vadsrijk z5wezens opruimen`: haalt alle zone-5-wezens binnen 64 blokken weg.

### 1. De Proefarena (lv 56-57)
- `/guhs vadsrijk z5wezens tp proefarena`
  - **Bezeten Oefenpop**: staat stil als een gewone pop (geen naambordje). Loop binnen 5 blokken: "De oefenpop beweegt!".
    - Rode cirkel = hij draait rond: stap weg.
    - Verslagen: grijs pluis waait eruit.
  - Leider **Opperpop Strobaal** (elite, gouden gloed).
- **Sparren met een ridder-guh** (doet NOOIT pijn):
  - Typ `/guhs vadsrijk z5wezens sparren`, of rechtsklik een Sparringridder in de arena.
  - Rode strook = kussenzwaai: stap eruit. Drie "Kussenklap!" en je verliest de ronde. Je Vads blijven vol.
  - Roze glinster bij zijn schild = hij blokt: je klappen doen dan niets ("Geblokt!").
  - Sla zijn balk naar 5%: hij buigt, "Goed gevochten!". Je krijgt xp, een Proefpenning-teller, de eerste keer zeker een Wolkwacht-stuk, daarna 30% kans.
  - Klappen van iemand anders dan je sparpartner doen niets.

### 2. De Buikgangen + de kerker (lv 57-59)
- **Luik**: de kelder van de Soepkeuken heeft een luik (rechtsklik: "naar de Buikgangen"). Snel: `/guhs vadsrijk z5wezens tp buikgangen`.
- **De kamers**: de Ingangshal, dan de Ribbengang (botten-bogen), de Kleppenzaal (koperen pijpen) en een gang naar de ronde Ketelkamer.
  - Wezens: Mika-saboteur (rode cirkel = gassleutel, grijze wolk = plakkerig), Mika-lontlegger (blijft op afstand, kaasbom = rode cirkel), Grauwe Pluismijten (in groepjes).
  - Terug naar boven: het luik bij de ingang (rechtsklik).
- **Kerkerbaas Opzichter Gorgonzola** in de Ketelkamer: `/guhs vadsrijk z5wezens baas gorgonzola` (reset + naar de deur).
  - 4 warme-luchtkleppen (roze wiel = open, grijs = dicht).
  - Om de ~25 s paarse lijn = hij draait een klep dicht. Elke dichte klep: hij neemt 15% minder schade, en iedereen krijgt elke 3 s een beetje kou.
  - **Rechtsklik een dichte klep 3x** om hem open te draaien.
  - Verder: moersleutelzwaai (rode cirkel om hem), kaasgasbom (rode cirkel op iemand), buikschuiver (rode strook).
  - Op 60% fluit hij 2 saboteurs. Op 25% gaan ALLE kleppen dicht en zakt hij niet onder 15% tot er weer minstens drie open zijn.
  - Snel testen: `/guhs vadsrijk z5wezens mechaniek kleppen` (sluit alle kleppen van de dichtstbijzijnde Gorgonzola).

### 3. De Wolkrand (lv 58-60)
- **Wolkenliften** (witte geglazuurde blokken, rechtsklik) aan de stadsrand. Snel: `tp zuidvin`, `tp noordvin` of `tp staartvlakte`.
- Wezens:
  - Grauwe Wolkduivel: vliegt, en rode streep = duik.
  - Mistslier: grijze cirkel = mist (blind + plakkerig).
  - Mika-harpoenier: rode lijn = harpoen. Die trekt je NAAR hem toe, nooit naar de rand.
  - Leider **Moeder Mist** (grote mistcirkel).
- **Vangnet**: val je van een vin of de staart, dan vangt een wolkje je op ("Een wolkje vangt je op!") en sta je weer bij de lift.
- **Wereldbaas Donderbuik** (een dikke grauwe onweerswolk) op de staartvin: `/guhs vadsrijk z5wezens baas donderbuik`.
  - Bliksem: een rode cirkel onder iedereen.
  - Regenlaan: een brede rode strook (plakkerig).
  - **Zuigwind**: drie ROZE wolkenkussens verschijnen. Sta erop, anders zuigt hij je naar zijn buik (35%). Testen: `mechaniek zuig`.
  - 60%: 3 wolkduiveltjes + Donderslag (gele cirkel: sta er samen in).
  - Aan het eind wordt hij ROZE, giechelt en drijft weg, met roze regen. Huisdier: **Donderbuikje**.
  - Respawn 20 min.

### 4. Het Schuilnest (lv 58-60)
- Een losse plank achter de kratten op de Luchthaven (rechtsklik). Snel: `tp schuilnest`.
- **Vermomde Mika's** staan er als Wolkwachters (geen naambordje). Kom binnen 4 blokken: "Ontmaskerd!", het kostuum ploft eraf.
  - Rode cirkel achter je = daar verschijnt hij. Stap weg.
- Leider **Meester-Vermommer Feta**.

### 5. De spion: Kootje Zilverstaart (de finale van "Verraad in de stad")
- Het verhaal start hem (t-z5verhaal: `Z5Finale.start` → onze `Z5Bazen.start(speler, "spion")`). Testen: `/guhs vadsrijk z5wezens baas zilverstaart` (zet je bij de Wolkwaldroom en roept hem op).
- Fase 1: speldsprong (rode strook), rookbom (grijze cirkel: blind), daarna duikt hij op achter wie het verst staat.
- Fase 2 (70%), **De Vermommingen**: vijf Wolkwachters in een kring, één is hem.
  - Zoek de ZILVEREN glinster bij de staart.
  - Raak je een lokpop: "Een lokpop!" en dat kost jou 10%.
  - Raak je hem: ontmaskerd en +50% schade, 8 s lang.
  - Niet gevonden binnen 25 s: hij springt eruit (25% op iedereen).
  - Testen: `mechaniek vermom`.
- Fase 3 (35%), **De ontsnapping**: drie grauwgasventielen bij het spuitgat. Rechtsklik elk 3x. Tot ze alle drie dicht zijn zakt hij niet onder 10%. Testen: `mechaniek ontsnap`.
- Verslagen: hij vlucht in rook. Het verhaal gaat door naar de Grauwe Droom (die fase speelt de verhaal-scène van t-z5verhaal).

### 6. Buit (lv 56-60)
- `/guhs vadsrijk z5wezens set`: de **Wolkwachtset** + sieraden.
  - Met de nieuwe slots van t-items zijn het 6 stukken: helm, kuras, beenstukken, laarzen, handschoenen, cape.
  - Sieraden: Wolkring, Walvisketting, Regendruppeloorbel, Stormarmband.
  - Setbonussen in de tooltip:
    - (2) Verdediging +10 en Vads +60;
    - (4) Schade tegen bazen +8%;
    - (6) Wolkkussen: een schild onder 30% Vads.
- `/guhs vadsrijk z5wezens wapens`: de 6 klassewapens + de 7 benoemde baasstukken + 3 trofeeën.
  - Klassewapens: Wolkwachtzwaard, Walvisbotknots, Stormboog, Harpoenkruisboog, Wolkenstaf, Bliksemstaf.
  - Baasstukken: Hoedenspeld, Mantel der Vermommingen, Moersleutel, Kleppenstaf, Donderboog, Onweerstaf, en het Guhddelijke Regenjasje.
- `/guhs vadsrijk z5wezens loot <wezen|baas> [level] [elite]`: rolt een tabel (een baas = zijn kist; de eerste keer zeker een Legendarisch + trofee).
- **Huisdieren** (eerste kill): Donderbuikje (Donderbuik), Warmpje (Gorgonzola), Lokpopje (Zilverstaart).


## Zone 5: de restjes van Hoogvads (T-WOLKWAL)

Wolkwacht-ridder-model, bewegende vinnen en staart van Moeder Wolkwal, spuitgaten, twee panorama's en betreedbare bovenverdiepingen.
Alles onder `/guhs vadsrijk wolkwal ...` (de plekken zelf: `/guhs vadsrijk z5wereld plek <id>`).

### Wolkwacht-ridders (eigen model)
- `/guhs vadsrijk wolkwal ridders` - 6 ridders op een rij voor je neus, in alle 6 vachten (gewoon, sneeuw, goud, mint, choco, wolk). De laatste zit. Na 60 s weg.
  Kijk naar: een echte guh (oortjes, ogen, staartje bewegen) in wolkblauw/wit harnas met gouden randen. Helm met gouden rand, neusstuk en wangplaatjes, een wit-blauwe pluim die zacht wiebelt. Borstplaat met een gouden wolkje, gouden riem, wolkenkraag, kort cape'je, een klein wolkenschild op de linkerflank, een zwaard in de schede rechts, laarsjes met een gouden band.
- `/guhs vadsrijk z5wereld plek academie_oefenplein` - de ridders in formatie op het oefenplein zijn nu Wolkwacht-ridders (geen guh met wachter-textuur meer). Ook bij de poorten en in de Proefarena (`z5w_proefarena`).
- De Wolkwacht-NPC's van het verhaal (t-z5verhaal, `z5_wacht_1..6`: ridderhelm + wolkenkraag) krijgen in het spel hetzelfde model in hun eigen vacht. Werkt pas na de integratie met t-z5verhaal (in deze tak bestaan ze nog niet).
- De Sparringridder (t-z5wezens) kan hetzelfde uiterlijk krijgen: na de integratie `python tools/vadsrijk/wolkwal/maak_modellen.py --sparring-over` (kussenzwaard rechts, groot wolkenschild links, hun animaties blijven).

### Moeder Wolkwal zwemt
- `/guhs vadsrijk wolkwal kijk vin` - naast de zuidvin. Het puntje van de vin wuift langzaam op en neer (4 s), het uiterste stukje loopt iets achter. Wolkslierten waaien naar achteren (oost: zij zwemt naar het westen).
- `/guhs vadsrijk wolkwal kijk staart` - naast de staartvin: beide punten van de staart slaan zacht op en neer (5 s).
- `/guhs vadsrijk wolkwal kijk spuitgat` dan `/guhs vadsrijk wolkwal spuit` - beide spuitgaten op haar kop spuiten: wolkjes schieten omhoog langs de wolkenzuil, een pluim bovenin, druppeltjes, een zacht "pfffsh". Vanzelf elke 20 s.
- `/guhs vadsrijk wolkwal kijk kop` - voor haar neus (om alles in één keer te zien).
- `/guhs vadsrijk wolkwal beweging` - status (hoeveel bewegende punten er nu zijn, max 4). `... beweging uit` / `aan` om te vergelijken met de statische blokken.
- Let op: de bewegende puntjes zijn alleen beeld (geen botsing). De beloopbare wolkplatforms op de vinnen en de staart zijn niet veranderd. Ze verschijnen pas binnen ~190 blokken.

### Panorama's (eenmalig, vanzelf)
- `/guhs vadsrijk wolkwal uitzicht reset`, dan `/guhs vadsrijk z5wereld plek ooggaarde_tuin` - zodra je aankomt: een korte camerarit (zonder klikken) over haar kop, langs haar oog, voor haar neus langs en terug over de stad. Titel "De Ooggaarde".
- `/guhs vadsrijk z5wereld plek staartpark_prieel` - camerarit over de staart, langs de wuivende staartvin, achter haar met de hele stad en de kathedraal. Titel "Het Staartpark".
- Direct afspelen zonder te lopen: `/guhs vadsrijk wolkwal uitzicht ooggaarde` / `... uitzicht staartpark`.

### Bovenverdiepingen (betreedbaar)
- `/guhs vadsrijk z5wereld huis herberg_hemelbed` - binnen: achterin een houten trap omhoog (langs de zijmuur), boven een gastenkamer met bedden langs de voorgevel, een kist en een hanglamp. Hekje rond het trapgat.
- Of meteen naar boven: `/guhs vadsrijk wolkwal boven herberg_hemelbed`.
- Zelfde voor `bank_hoogvads` (boven: de kluis met goud en kisten, een lessenaar), `winkel_smid`, `winkel_bakker`, `winkel_wapens`, `winkel_kruiden` (boven: voorraad, het bed van de winkelier, een werkbank).
- Kijk naar: de trap begint achter de toonbank (via de opening aan de zijkant), elke trede sluit aan, de laatste trede ligt gelijk met de vloer boven, de ramen boven zijn nu echte ramen.
- Andere huizen (en de kledingwinkel: te klein voor een trap) hebben nog een dichte bovenverdieping.


## De Grauwe Droom (zone 5, finale fase DROOM) — T-DROOM

**Wat is het?** Na Kootje Zilverstaart (de spion) in De Wolkwaldroom zak je in de **droom van Moeder Wolkwal**. Je komt op
**de Droomwolk** (een ronde wolkvloer in de lucht, r 17, ten oosten van de staartvin). Daar zwemt **De Grauwe Droom** rondjes:
een grote grijze walvis van mist met traantjes (lv 60). Je verslaat hem niet, je **vrolijkt hem op**: aan het eind wordt hij roze
en speelt het echte slaapliedje op een fluit. Daarna word je **wakker bij het middelste spuitgat**.

### Snel testen (in Het Vadsrijk, op level 2)
```
/guhs vadsrijk droom start
```
Je ziet "Je zakt in een droom...", je staat op de wolk, na 1,5 s de intro (camera + titel "De Grauwe Droom").

| Commando | Wat er moet gebeuren |
|---|---|
| `/guhs vadsrijk droom start` | Droom + gevecht (als herinnering, geen quest nodig). Je party binnen 24 blokken gaat mee. |
| `/guhs vadsrijk droom info` | Plek van de wolk, levens, fase, neurie-meter, ranken, nacht, slaapliedjes goed/mis. |
| `/guhs vadsrijk droom wiegelied` | De 4 nootstenen op de rand lichten één voor één op (kleur + fluittoon). Daarna: rechtsklik ze **in die volgorde**. Goed = roze hartjes, kaart "Het slaapliedje klinkt weer goed!", de walvis staat 8 s stil en krijgt 10 s +50% schade. Een verkeerde noot = "Vals! Begin weer bij de eerste noot." (geen schade). Te laat (14-16 s) = "Nachtmerrie!": 30% bij iedereen, hij heelt 4%. |
| `/guhs vadsrijk droom ranken` | 3 grijze ranken groeien uit de wolk. Ze grijpen de dichtstbijzijnde speler (grijs lijntje, plakkerig, 4% per 2 s) en maken de droom 15% taaier per rank. Sla ze stuk: roze pluf, "Doorgeknipt!". |
| `/guhs vadsrijk droom omkeer` | De droom draait om: de wolkvloer wordt **donkergrijs/zwart** (omgekeerde kleuren), vier **roze wolkjes** (roze cirkels) drijven rond. Wie niet op een roze wolkje staat: 7% per 2 s. Na 20 s wordt de vloer weer wit. |
| `/guhs vadsrijk droom fase 2` | Zet hem op 66%: de volgende klap start fase 2 ("De droom draait om!" + omkeer, daarna het slaapliedje). |
| `/guhs vadsrijk droom fase 3` | Zet hem op 31%: de volgende klap start fase 3 "Samen neuriën!": een **gele cirkel** in het midden. Hij zakt niet onder 30% ("De droom is te zwaar: neurie eerst...") tot de neurie-meter (actiebalk) vol is: sta in de gele cirkel (solo ~15 s, met meer spelers sneller; muzieknootjes boven je hoofd). Vol = "Moeder Wolkwal neuriet mee!", hij wordt **lila** en krijgt +25% schade. |
| `/guhs vadsrijk droom neurie` | Neurie-meter meteen vol (lila). |
| `/guhs vadsrijk droom win` | Meteen opvrolijken: roze, hartjes, slaapliedje op de fluit, hij zweeft omhoog. Je kist staat op de wolk. Open je kist → je wordt wakker bij het spuitgat ("Je wordt wakker / Moeder Wolkwal slaapt weer zoet"). Zonder kist open: na 90 s. |
| `/guhs vadsrijk droom wakker` | Meteen wakker worden bij het middelste spuitgat. |
| `/guhs vadsrijk droom tp` | Alleen naar de wolk (geen gevecht). |
| `/guhs vadsrijk droom bouw` | De wolkvloer opnieuw leggen (alleen in lucht, sloopt nooit iets). |
| `/guhs vadsrijk droom reset` | Grauwe Droom weg (vloer wordt weer wit). |
| `/guhs vadsrijk droom loot [level]` | Een kistworp in je tas: 1e keer zeker een benoemd Legendarisch wapen + de trofee. |
| `/guhs vadsrijk droom verhaal` | Roept de echte verhaal-haak (`Z5Finale.start`) aan: doet alleen iets tijdens z5_hq08, na het spuitgat. |

### Via het verhaal
1. `z5_hq08` → naar het middelste spuitgat → Kootje Zilverstaart (t-z5wezens) → verslagen → **meteen de droom** (deze slice).
2. Na het opvrolijken telt het doel "De Grauwe Droom" voor je hele party en ga je terug naar de koningin.
3. Zonder deze slice speelde er een korte camerascène; die is nu vervangen door het echte gevecht.

### Opletten / kijken
- **Van de wolk vallen:** tijdens het gevecht vangt een droomwolkje je op ("Een droomwolkje vangt je op...") en sta je weer aan de zuidrand. Na het gevecht = van de wolk stappen is wakker worden.
- **Herinnering (opnieuw spelen, voor loot + huisdier):** na het verhaal **2 s hurken op het middelste spuitgat** → "Je ogen worden zwaar..." → je droomt het nog een keer.
- **Huisdier:** *Droomwalvisje* (klein roze wolkwalvisje) na de eerste keer opvrolijken.
- **Loot:** de zone-5 bazenkist (Wolkwachtset, sieraden, wapens) + **Wiegeliedstaf** (Magiër), **Zwaard van de Zoete Droom** (Ridder), **Slaapsterrenboog** (Jager), heel zeldzaam **Moeder Wolkwals Slaapmuts** (Guhddelijk, hoofd), trofee **Een Fijne Droom (in een potje)**.
- **Uiterlijk** (zelf beoordelen): grijze walvis met traantjes en wolkjes op zijn rug; lila met "o"-mondje na het neuriën; roze en blij aan het eind. Nootstenen: wolksteen met een gouden randje en een draaiende muzieknoot (roze, geel, lichtblauw, lila). Ranken: zachte grijze sliertjes met blaadjes.
- **Muziek:** hook-id `guhs:vr.z5droom.muziek.grauwe_droom` (nog geen geluid; t-geluid kan het vullen).


# 6. DE SYSTEMEN


## DIALOOG: het nieuwe Vadsrijk-praatscherm (T-DIALOOG)

Alle gesprekken **in het Vadsrijk** krijgen nu het nieuwe praatscherm. Buiten het Vadsrijk (Knuffeldal, Erfenis, de verhalen)
blijft het oude scherm precies zoals het was.

Test dit op **GUI-schaal 2, 3 en 4**:
- Opties > Video > GUI-schaal.
- Doe de demo op elke schaal één keer.

### D1. De demo: alles in één gesprek
```
/guhs vadsrijk dialoog demo
```
Dit werkt overal, ook buiten het Vadsrijk. Klik je door de 8 bladzijden heen.

**Bladzijde 1, de verteller:**
- Je ziet een hele perkamentpagina in schuine letters, zonder portret, met een boekje in de hoek.
- De tekst **typt zichzelf**, met een zacht piepje per woord.
- **Klik of spatie** = meteen de hele tekst. Nog een keer = de volgende bladzijde.

**Bladzijde 2, Oma Truus (blij):**
- Het portret **hopt** op en neer.
- Er staat een roze **♥** in de hoek.
- Het naambordje is **roze** (zone 1). Ernaast staat waar ze woont.

**Bladzijde 3, Poollicht Pien (verdrietig):**
- Het portret **zakt in** en kijkt naar beneden.
- Er staat een blauw **…** in de hoek.
- Het naambordje is **ijsblauw** (De Splinterpieken).

**Bladzijde 4, Imker Zoemzang (verrast):**
- Het portret maakt een **sprongetje** en wordt even iets groter.
- Er staat een **!** in de hoek.
- Het naambordje is **honinggeel** (De Schemerwortel).

**Bladzijde 5, de Mika-imker (boos):**
- Het portret is een **Mika** (dat was eerst een boekje).
- Het **schudt** en er staat een rood **╬** in de hoek.
- Het naambordje is **grijs**, met een ander (sluw) stemmetje.

**Bladzijde 6, jij:**
- Je **eigen portret staat rechts**.
- De ballon is lichtgroen en het bordje **mintgroen**.

**Bladzijde 7, de trainer:**
- Het naambordje is **paars**, met "Knuffelorde" ernaast.

**Bladzijde 8, een lange tekst:**
- Past alles? De ballon groeit mee.
- Is het scherm te laag, dan scroll je de tekst met het **muiswiel** of met **spatie**.

**De keuzes op de laatste bladzijde, elk met een icoontje:**
- een boek: een opdracht aannemen;
- een smaragd: een winkel;
- een kaart: de reiskaart;
- een veertje: reizen;
- een deur: "Nu even niet";
- een kompas: **"Wat moet ik doen?"**;
- **Doei!** heeft ook een deur.

Rechts in elke knop staat het **cijfer**. Druk op 1-6 om meteen te kiezen.
- Kies je een gewone keuze, dan sluit het scherm en zie je in de chat "Dialoog-demo: je koos keuze N."

**Boven de keuzes staat de opdrachtstrook:**
- "**Opdracht:** Breng 3 knuffelkoekjes naar Oma Truus, bij de bakkerij in Zwamzoet".
- Wijs hem aan voor de hele tekst.

**"Wat moet ik doen?":**
- Oma Truus zegt de opdracht dan als een gewone zin: wie, wat en waar.
- Met **« Terug** ga je terug naar de keuzes.

**Bladeren en de log:**
- Onder de tekst staan **stipjes**: één per bladzijde, en de huidige in de kleur van de spreker.
- **« Terug** of **backspace** = een bladzijde terug.
- **Shift+spatie** = meteen naar de keuzes. De keuzes sla je nooit over.
- **L** of het knopje **≡** rechtsboven opent het **gesprekslog**: alles wat er in deze sessie gezegd is, met de kleur van de spreker en scrollen met het muiswiel.
- **L** of **Esc** sluit de log weer. Daarna ben je terug in het gesprek, het scherm gaat niet dicht.

**De instellingen (het tandwieltje ⚙ rechtsboven):**
- **"Tekst: normaal"**: klik om te wisselen tussen meteen, langzaam, normaal en snel.
- **"Stemmetjes: 60%"**: een schuif. Bij het schuiven hoor je een voorbeeldpiepje. Op 0 = uit.
- Je vindt allebei ook onder Mods > Guhs > Config (`config/guhs-vadsrijk-dialoog.toml`).

### D2. Een vol scherm (GUI-schaal 3-4)
```
/guhs vadsrijk dialoog vol
```
Dit is een lange tekst met 9 keuzes.
- Op GUI-schaal 4, of in een klein venster, komen de keuzes in **twee kolommen**.
- De tekst scrollt, met een knipperend ▼ als er nog meer staat.
- Er mag **niets** boven of onder buiten beeld vallen.
- "Naar Kandijkom (nog op slot)" staat met **grijze** tekst.

### D3. Echte quest-NPC's: "Wat moet ik doen?"
In een nieuwe wereld, in het Vadsrijk:
```
/guhs vadsrijk npc tp rekruteerder
```
**Klik de sergeant:**
- Hij biedt hq01 aan. Heb je hq01 al (dan zie je meteen "Nu:"), test het aanbod dan bij de volgende questgever (volg de questvolger).
- Boven de keuzes staat "**Opdracht:**" met de eerste stap (wat, waar).
- Het kompasknopje "Wat moet ik doen?" zegt het als een zin: *Als je ja zegt ("..."): ..., ....*
- Neem de quest aan en klik hem nog een keer. Er staat nu "**Nu:**" met de huidige stap.

**Klik een NPC zonder quest (een burger):**
- Je ziet géén kompasknop en geen strook.

**Zone 2 en 3:**
- Ook daar krijgen de gesprekken het nieuwe scherm.
- Probeer `/guhs vadsrijk npc tp z2_kapitein` en klik.
- Het bordje is ijsblauw en de reisknoppen hebben een veertje en een kaart.

### D4. Vergelijken met het oude scherm
```
/guhs vadsrijk dialoog oud
/guhs vadsrijk dialoog demo
/guhs vadsrijk dialoog oud
```
- De eerste keer `oud` geeft je het oude scherm. De tweede keer krijg je weer het nieuwe.
- Het oude scherm moet net zo werken als vroeger.

**Buiten het Vadsrijk:**
- Een Knuffeldal-NPC, een Erfenis-gesprek en een verhaal-NPC houden **altijd** het oude scherm.
- Even één aanklikken is genoeg.

### D5. Ontwikkelaarshulp
**F3+H aan (geavanceerde tooltips):**
- Onder het scherm staat een grijze regel met: sleutel | tekstsleutel | factie/stem/emotie (een ! = vast gezet, ~ = geraden uit de tekst) | portretbron.

**Een emotie per regel in questdata:**
- Zet bij een spreker `"npc:blij"` (of `verdrietig`, `verrast`, `boos`) in plaats van `"npc"`.
- Zonder zo'n emotie raadt het scherm die uit de tekst: "Hoera" = blij, "?!" of "Ooh" = verrast, "Grr" = boos, "helaas" of "..." = verdrietig.


## SPIEK T-GELUID: muziek + omgevingsgeluid in het Vadsrijk

Besluit R3 was "muziek ja, zacht". Dit blok heeft vier delen:
- **zachte gebiedsmuziek**, met lange stiltes ertussen;
- **een thema per baas**;
- **stingers** (korte ta-daa's);
- **een rijkere omgevingsgeluid-laag**, zonder harde wind.

De muziek is gemaakt met een eigen pijplijn: MIDI → SoundFont (GeneralUser GS) → OGG. De instrumenten zijn speeldoos, celesta, harp, fluit, marimba, kalimba, zachte strijkers en koor. Alleen de bazen klinken episch.

> **Smaak gaat voor.** Vind je een stuk niet mooi? Zeg dan welk stuk en wat eraan schort, bijvoorbeeld "te druk", "te somber", "ander instrument" of "trager". Elk stuk is één regel data in `tools/vadsrijk/geluid/stukken.py` en is in ~10 s opnieuw gemaakt.

### Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van ALLE tracks (klik = luisteren) | `/guhs vadsrijk geluid` |
| Eén gebiedsthema | `/guhs vadsrijk geluid gebied klodderweide` |
| Eén baasthema | `/guhs vadsrijk geluid baas vahoegnir` |
| Eén stinger | `/guhs vadsrijk geluid stinger baas_verslagen` |
| Eén omgevingsbed (12 s) | `/guhs vadsrijk geluid bed krekels` |
| Alles stil | `/guhs vadsrijk geluid stop` |
| De gebiedsmuziek hier NU starten (sla de stilte over) | `/guhs vadsrijk geluid nu` |
| Waar ben ik, wat hoort hier, hoe staan de schuifjes | `/guhs vadsrijk geluid info` |

Je kunt de OGG's ook buiten het spel afspelen. Ze staan in `src/main/resources/assets/guhs/sounds/vadsrijk/muziek/{gebied,baas,stinger}/` en `.../vadsrijk/sfeer/`.

### Schuifjes
Ga naar **Mods → Guhs → Config** (het bestand `guhs-vadsrijk-geluid.toml`). Elke waarde is een schuif van 0-100.
- **gebiedsmuziek:** standaard 70.
- **bazenmuziek:** standaard 85.
- **stingers:** standaard 80.
- **omgeving:** standaard 70.
- **pauze_min / pauze_max:** de stilte tussen twee gebiedsstukken. Standaard 60 / 180 s.

De muziek volgt OOK de schuif **Muziek** van Minecraft, en het omgevingsgeluid volgt **Omgeving**. Zet je een schuif op 0, dan staat dat deel uit. Staat gebiedsmuziek op 0, dan speelt in de wildernis weer de gewone Minecraft-muziek.

Testen:
1. Zet gebiedsmuziek op 0. → Na ~5 s stopt het stuk.
2. Zet hem terug op 70. → Daarna start `/guhs vadsrijk geluid nu` hem weer.

### 1. Gebiedsmuziek (zones 1-3 + Lampionkust + Hoogvads)
Zo werkt het:
- Je loopt een gebied in. Na 5-15 s komt het thema zacht op (~1-1,5 min).
- Daarna is het **1-3 min stil**, met alleen omgevingsgeluid. Dan speelt het thema opnieuw.
- Loop je een ander gebied in, dan vervaagt het oude stuk binnen ~9 s. Kort daarna komt het nieuwe.
- In steden speelt de bestaande stadsmuziek (die blijft zoals hij was).
- Tijdens een baasgevecht, scène of cutscene speelt er geen gebiedsmuziek.

| Gebied | Commando | Karakter |
|---|---|---|
| Klodderweide (+ zone-1-rand) | `/guhs vadsrijk geluid gebied klodderweide` | vrolijk pastoraal: fluit, marimba, harp, pizzicato |
| Sporenwoud | `... gebied sporenwoud` | lydisch toverbos-walsje: ocarina, celesta, speeldoos, glaspad |
| Druipgrotten | `... gebied druipgrotten` | rustig mineur: kalimba met echo, druppelklanken |
| Zwamtempel | `... gebied zwamtempel` | plechtig dorisch: panfluit, koor-oehs, harp, buisklok |
| Knuffelmeer | `... gebied knuffelmeer` | dromerig meer-walsje: klarinet, vibrafoon, harp |
| Galmhellingen (+ zone-2-rand) | `... gebied galmhellingen` | blokfluit met ECHO (galm!), glockenspiel, hoorn |
| Splinterkloof | `... gebied splinterkloof` | 6/8 avontuur in mineur: hobo, viool, pizzicato-ostinato |
| Spiegelburcht | `... gebied spiegelburcht` | spiegel-wals: celesta met glockenspiel-echo, klavecimbel |
| Wortelvelden (+ zone-3-rand) | `... gebied wortelvelden` | mixolydisch: ocarina, gitaar-arpeggio, marimba, shaker |
| Wortelduister | `... gebied wortelduister` | donker maar lief: kalimba met echo, lage strijkers |
| Het Hart | `... gebied hart` | heilig en traag: celesta, harp, koor |
| Schorsstad | `... gebied schorsstad` | huppelend: hakkebord, klarinet, fagot, accordeon |
| Stamspiraal | `... gebied stamspiraal` | opklimmend: panfluit, harp, pizzicato |
| Takkenbanen | `... gebied takkenbanen` | luchtig 6/8: fluit, piccolo, harp, glockenspiel |
| Kruinzee | `... gebied kruinzee` | breed en zwevend: viool, celesta, strijkers |
| Zeppelinwerf | `... gebied zeppelinwerf` | bedrijvig-grappig: klarinet, xylofoon, tuba, fagot |
| Lampionkust overdag | `... gebied lampionkust_dag` | eilandjes: steeldrum, ukelele, fluit, **met het Vadsrijk-leidmotief** in het middenstuk |
| Lampionkust 's avonds | `... gebied lampionkust_avond` | lampionfeest-wiegje: speeldoos, vibrafoon, **leidmotief** |
| Hoogvads | `... gebied hoogvads` | feestelijk-groots: hoorn, trompet, strijkers, koor, pauk, **leidmotief** |
| Hoogvads markt | `... gebied hoogvads_markt` | markt-wals: fluit, accordeon, pizzicato |

- **Lampionkust:** overdag speelt dag, vanaf de avond (tijd 11500-23500) avond.
- **Hoogvads:** de twee stukken wisselen elkaar af.
- **Zones 4 en 5:** die herkent het spel aan een gebied-id `z4_*` / `z5_*`, of anders aan de W-3-vakken (zone 4: x 1728..2559, z -768..-257; zone 5 erboven vanaf y 150).

**Kijk/luister:**
1. Ga naar de Klodderweide.
2. Wacht ~10 s. → Het stuk komt zacht op. Na afloop is het stil.
3. Vraag `/guhs vadsrijk geluid info`. → Je ziet "stilte, nog N s".
4. Loop naar Zwamzoet. → Het gebiedsstuk vervaagt en het stadsdeuntje neemt over.

### 2. Baasthema's
Het thema start zodra de baasbalk verschijnt en loopt naadloos rond. Gaat de balk weg, dan vervaagt het. Elke baas heeft een eigen thema:

| Zone | Baas → commando |
|---|---|
| 1 | Klodderreus `baas klodderreus` · Koning Klodder `baas koning_klodder` · Opperzwam `baas opperzwam` · Mika-spion `baas mika_spion` |
| 2 | Boormeester `baas boormeester` · Galmwolf `baas galmwolf` · Vrouwe Spiegelzucht `baas spiegelzucht` (ook haar spiegelbeeld) |
| 3 | Baron Bleu `baas baron_bleu` · Oude Nestwachter `baas nestwachter` · Wortelwurger `baas wortelwurger` · Graaf Piepvel `baas piepvel` · Kruinroofvogel `baas kruinroofvogel` · **Vahoegnir** `baas vahoegnir` · Bombardier Brokkel `baas brokkel` · Kapitein Roquefort `baas roquefort` · Takelmeester Taleggio `baas taleggio` |
| 4 | Grauwe Zeeslurf `baas zeeslurf` · de Vliegervanger `baas vliegervanger` |
| nieuw/onbekend | `baas algemeen` (vangnet voor elke baas zonder eigen thema) |

Elk commando begint met `/guhs vadsrijk geluid`.

**Echt testen:**
1. Vecht met een baas, bijvoorbeeld `/guhs vadsrijk baas reset` bij een arena.
2. Bij de balk → het eigen thema.
3. Win je → het thema vervaagt en de stinger **baas_verslagen** (koperfanfare) speelt.

Zone-4-bazen worden herkend aan een entity-id met `zeeslurf` / `vliegervanger` erin. Een baas kan ook zelf een geluid-id teruggeven via `muziek()`.

### 3. Stingers
| Stinger | Wanneer | Commando |
|---|---|---|
| ontdekking | de EERSTE keer dat je een gebied inloopt (niet de grote zone-randen) | `/guhs vadsrijk geluid stinger ontdekking` |
| quest_klaar | een quest ingeleverd | `... stinger quest_klaar` |
| level_op | level omhoog (één ta-daa, ook als generiek + klasse tegelijk stijgen) | `... stinger level_op` |
| baas_verslagen | een baas opgevrolijkt (iedereen die hielp) | `... stinger baas_verslagen` |
| geheim | een schat/geheim gevonden (de "schat"-titelkaart van zone 1-3) | `... stinger geheim` |

- Tijdens een stinger zakt de muziek even weg.
- Twee stingers tegelijk spelen na elkaar.
- Snel testen: `/guhs vadsrijk level <hoger dan nu>` (of `/guhs vadsrijk xp 5000`) → stinger level_op.

### 4. Omgevingsgeluid (de §5.3-pas)
Per gebied speelt een zacht "bed" (een lus van 22 s). Elke 5-12 s klinkt ergens om je heen een los geluidje.

| Bed | Waar | Wat je hoort |
|---|---|---|
| weide | Klodderweide, zone-1-rand, Hoogvads | vogeltjes, zacht bladgeritsel |
| bos | Wortelvelden, Schorsstad, Stamspiraal | meer vogels, af en toe een krakend takje |
| sporen | Sporenwoud | toverglinstering, zachte zoemtoon, een enkel vogeltje |
| grot | Druipgrotten + overal ondergronds | druppels met galm, diepe grotruis |
| tempel | Zwamtempel, Spiegelburcht, Het Hart | zachte zoemtoon, druppels |
| meer | Knuffelmeer | kabbelend water, vogels, een kikkertje |
| berg | Galmhellingen | belletjes (windgong), verre beek, vogeltje: **géén wind** |
| kloof | Splinterkloof | beekje (stroompje), druppels |
| wortel | Wortelduister | diepe druppels met lange galm |
| kruin | Takkenbanen, Kruinzee, Zeppelinwerf | bladeren + vogels in de kruin |
| honing | Honingtak (bovenop het stadsbed) | bijengezoem |
| kust | Lampionkust (zone 4) | golven, zeemeeuwen, windgong |
| krekels | 's nachts (tijd 13000-23000), buiten | krekels, uiltje, zachte windgong |

- 's Nachts maken de vogelbedden plaats voor de krekels. Bij water (meer, kloof, kust) komen de krekels erbovenop.
- De oude Z2-wind (Balto-wind op de Galmhellingen, het Knuffelmeer en de Splinterkloof) is vervangen door een zacht belletje.

**Luister:**
1. Speel `/guhs vadsrijk geluid bed kust` (12 s), daarna `bed krekels` en `bed honing`.
2. Ga 's nachts op de Klodderweide staan (`/time set 18000`). → Je hoort krekels en een uiltje.
3. Ga een grot in. → Je hoort druppels.

### Bekend / nog niet
- De muziek is door een computer gecomponeerd (regels + zaadjes). Hij klinkt lief en netjes, maar niet als een echte componist. Het blijft smaakwerk: zeg welke stukken weg of anders moeten.
- Bazen hebben nog geen aparte fase-2-laag: het thema is één lus van ~40-70 s.
- Er zijn nog geen puntbronnen uit de POI-lijsten (watervallen, bijenkorven, smidse). Er is ook nog geen grot-echo op voetstappen.
- De Lampionkust en Hoogvads gebruiken nu één thema per zone (+ dag/avond, + markt). Een thema per eiland of wijk kan later.


## T-MODELS: mooiere mobs (R5 hybride) en de Klodderreus/Vahoegnir als showcase

Wat is er veranderd? Alle gewone wezens van zone 1-3 zijn opnieuw gemaakt met de verbeterde generator:
- **Rondere vormen:** grote blokken hebben afgeronde randen en hoeken (een paar trapjes, zoals een kussen), poten lopen iets taps toe.
- **Betere textures:** licht van boven (rug lichter, buik donkerder), een zachte schaduw onderaan, glansplekken op slijm, zachte
  vlekjes en stippen, vachtstreepjes op vacht en pluis, kristallen met lichte randjes.
- **Gezichtjes:** groter (ze lopen nu over de ronde voorkant door), zachte randjes en een extra glimpje in de ogen.
- **Gloed:** gloeiende delen (kristallen, ogen, lampjes) zijn in het midden feller en aan de rand zachter.
- **Wiebelen:** oren, staarten en manen wiebelen een beetje na. Slijmen hebben een wiebelend kapje bovenop (squash & stretch).
- **De Grauwe Klodderreus** is geen vanilla-slijmblok meer: een eigen groot model met guh-oren, boze 3D-wenkbrauwen, paarse
  gloei-ogen, druppels langs zijn lijf, een mond die opengaat, en een roze blije versie na de plof.
- **Vahoegnir:** hetzelfde goedgekeurde ontwerp (ook de grote oren), alleen met meer volume (licht van boven, buik in de schaduw)
  en zijn oren wiebelen mee als hij vliegt.
- Voor/na-plaatjes: `guhs_vadsrijk\t-models\voorbeelden\` (zone1_*.png, zone2_*.png, zone3_*.png, klodderreus.png, vahoegnir.png).

### Showroom: alle wezens op een rij (stil, onkwetsbaar)
Ga op een vlakke, open plek staan en kijk de kant op waar ze moeten komen.
```
/guhs vadsrijk models toon zone1
/guhs vadsrijk models toon zone2
/guhs vadsrijk models toon zone3
/guhs vadsrijk models toon critters
/guhs vadsrijk models plof
/guhs vadsrijk models weg
```
- `toon` zet een raster neer (6 per rij, 4 blokken uit elkaar) van alle wezens van die groep. Ze staan stil maar spelen hun
  idle-animatie: let op het wiebelen van oren, staarten en het slijmkapje.
- `plof` vrolijkt alle showroom-wezens op: zo zie je de roze, blije textures (zone 1) en de plof-doorzichtigheid.
- `weg` ruimt alles op (alles met de showroom-tag binnen 96 blokken). `toon alles` kan ook, maar dat zijn er ~55.
- Kijk vooral naar: zijn de vormen ronder, lopen de gezichtjes netjes over de voorkant, zitten er geen gaten of rare randjes
  aan de afgeronde hoeken, en lezen de ogen goed op afstand.

### Eén wezen in actie (lopen, aanvallen)
```
/guhs vadsrijk wezens spawn klodder 3
/guhs vadsrijk wezens spawn hoedjesbol 3
/guhs vadsrijk wezens spawn kristalklodder 3 true
/guhs vadsrijk z2wezens spawn sneeuwknabbelaar
/guhs vadsrijk z3wezens spawn rotzwijn
```
- Met `true` erachter (zone 1) krijg je een elite: de gouden gloed moet nog netjes over het nieuwe model liggen.

### De Grauwe Klodderreus
```
/guhs vadsrijk models reus
/guhs vadsrijk models reus anim spuug
/guhs vadsrijk models reus anim brul
/guhs vadsrijk models reus anim aanloop
/guhs vadsrijk models weg
/guhs vadsrijk models reus vecht
```
- `reus` zet hem stil neer (7 blokken voor je), om rond te lopen en te bekijken. `reus anim ...` speelt een actie bij alle
  reuzen binnen 32 blokken: spuug (mond open, lijf zwelt op), brul (bij een nieuwe fase), aanloop (hij duikt in elkaar voor een sprong).
- `reus vecht` = een echt gevecht (lv 3) zonder arena. Let op: in de lucht wordt hij lang, bij het landen plat (squash),
  bij het rollen in fase 3 rolt hij echt over de kop, en in fase 2 spuugt hij kloddertjes: die hebben nu ook het ronde
  klodder-model in plaats van het vanilla-slijmblok. Na de plof: roze, blij, doorzichtig, zonder gloei-ogen.
- Het echte gevecht bij de kampvijver (BAM-quest) gebruikt hetzelfde model.

### Vahoegnir
```
/guhs vadsrijk wezens draak
/guhs vadsrijk z3bazen vahoegnir
```
- `wezens draak` laat de grijze overvlieger langskomen: zijn grote ronde oren wiebelen nu na op de vleugelslag.
- `z3bazen vahoegnir` voor de baas (grijs -> roze per stap): zelfde ontwerp, iets meer diepte (buik en poten donkerder, rug lichter).


## T-VFX: bazen-effecten, telegraphs op de grond, cast-balk en de bazenbouwdoos

**Wat is er nieuw?**
- Alle rode/roze/paarse/gele cirkels op de grond zijn nu **echte vlakken op de grond** (decals) in plaats van stofjes. Ze lopen
  vol tot de klap komt en flitsen vlak ervoor.
- Er zijn nieuwe vormen: **kegel, lijn, donut** (veilig in het midden), **kruis** en **"veilige plek"** (alles is gevaarlijk behalve
  de roze cirkel met hartjes).
- Er zijn **eigen deeltjes**: sterretjes, roze pluis, slijmspatjes, sporenwolkjes, kristalsplinters, honing, confetti, hartjes,
  veren, blaadjes en rook.
- Er zijn **schokgolven, stralen, flitsen, sporen** (lint achter een sprong/duik) en **lichtzuilen**.
- Er is een **cast-balk** onder de baasbalk: "Koning Klodder laadt: **Koninklijk Bad**".
  - Een paarse balk met "Onderbreek!" eronder betekent: raak hem hard (of verdoof hem), dan stopt de aanval en is de baas even suf.
- Bazen hebben **animaties**:
  - aanloop (inzakken en trillen tot de klap);
  - de klap;
  - een schokje als je ze raakt;
  - een faseovergang (steigeren met een schokgolf en een lichtzuil);
  - woede na 6 minuten;
  - de opgevrolijkt-finale (wiegen, huppen, zwaaien, confetti).
- **Elke baas uit zone 1-3 heeft minstens één nieuwe aanval** (de lijst staat hieronder).

**Het schuifje.** Je VFX-niveau zet je met `/guhs vadsrijk vfx niveau vol|zacht|uit`. Het staat ook in Mods > Guhs > Config
(`guhs-vadsrijk-vfx.toml`).
- **zacht** = minder deeltjes;
- **uit** = geen effecten.
- De cirkels op de grond blijven ALTIJD zichtbaar.
- Het schermschudden heeft zijn eigen schuif: "Vadsrijk: schermschud" (die was er al).

### Snel alles zien (zonder baas)
```
/guhs vadsrijk vfx demo alles
```
Kijk een kant op en voer het uit. Je ziet:
- een cirkel voor je;
- een kegel in je kijkrichting;
- een paarse lijn;
- een donut om je heen;
- een roze veilige plek met hartjes;
- een geel kruis;
- 13 soorten deeltjes in een kring;
- een schokgolf, een straal, een flits, een spoor achter jou (loop rond!) en een lichtzuil.

Losse onderdelen: `/guhs vadsrijk vfx demo cirkel|kegel|lijn|donut|veilig|kruis|deeltjes|golf|straal|flits|spoor|zuil`.

Bij een baas in de buurt: `demo cast` (een paarse cast-balk met "Onderbreek!") en `demo pose`.

**Let op:**
- De vlakken liggen netjes óp de grond, ook op een trapje of helling: ze zweven niet en zakken niet weg.
- Ze vullen zich gelijkmatig en flitsen op het eind.
- Zet `/guhs vadsrijk vfx niveau uit` en doe de demo nog eens: de cirkels zijn er nog, de deeltjes niet.

### Een baas testen
1. `/guhs vadsrijk vfx spawn <baas>`. Dit roept het juiste bestaande zone-commando aan (reset + tp, of spawn).
2. `/guhs vadsrijk vfx aanval <id>` start een nieuwe aanval meteen op de dichtstbijzijnde baas.
3. `/guhs vadsrijk vfx aanvallen` geeft de lijst.

Meer commando's:
- `/guhs vadsrijk vfx anim vfx_windup|vfx_treffer|vfx_geraakt|vfx_fase|vfx_woede|vfx_juich` speelt een animatie af op de
  dichtstbijzijnde baas.
- `/guhs vadsrijk vfx woede` zet de woede nu aan (de aanvallen komen 2x zo vaak, met rode rook).
- `/guhs vadsrijk vfx stop` stopt een lopende aanval.
- Fase overslaan kan met de bestaande commando's: `/guhs vadsrijk z3bazen fase 2|3|4` en `/guhs vadsrijk z2bazen fase 2|3`.
  Je ziet dan de faseovergang: steigeren, twee schokgolven, een lichtzuil en sterretjes.

In een gewoon gevecht komen de nieuwe aanvallen vanzelf: ongeveer om de 9-20 s, tussen de eigen aanvallen van de baas door.

### Per baas: wat moet je zien?
| Baas | Spawn | Nieuwe aanvallen (`vfx aanval <id>`) | Let op |
|---|---|---|---|
| **Grauwe Klodderreus** | `vfx spawn klodderreus` (los, zonder baasbalk; of in zijn vijver bij Knuffelkamp) | `kloddertjesregen` (6 slijmcirkels + 1 onder jou, plakkerig), `kloddergolf` (fase 2: donut, sta DICHT bij hem) | Sprong: hij zakt trillend in (aanloop), de cast-balk zegt "Plettersprong", hij laat een spoor in de lucht achter, en bij de landing komen een schokgolf en slijmspatjes. Rollen: "Rolaanval" met een groen spoor. |
| **Koning Klodder** | `vfx spawn koning_klodder` | `kroonregen` (sterrencirkels op iedereen + 4 los), `koninklijk_bad` (fase 2, PAARS: onderbreek met ~5% schade, anders heelt hij 8%) | De schokgolf heet "Grote Plof" in de cast-balk. Hij zakt in voor zijn sprong, en bij de landing komt er een blauwe schokgolf. |
| **Opperzwam** | `vfx spawn opperzwam` | `sporenkegel` (fase 1: kegel voor hem), `sporenkruis` (fase 2+: voor+achter, daarna links+rechts) | Stamp: aanloop + "Sporenstamp", dan per plek een paarse sporengolf. In het donker doet hij geen extra aanvallen. |
| **Galmwolf** | `vfx spawn galmwolf` | `echobrul` (kegel voor, dan achter hem), `galmring` (fase 2: eerst de cirkel om hem heen, dan de ring: eerst WEG, dan TERUG) | Kristalsplinters bij elke klap. |
| **Vrouwe Spiegelzucht** | `vfx spawn spiegelzucht` | `spiegelscherven` (3 lijnen in een waaier), `spiegelzaal` (fase 2: alleen de 2 roze hartjesplekken zijn veilig) | De rode ring rond de roze plekken loopt tot de rand van de zaal. |
| **Boormeester** | `vfx spawn boormeester` | `boorlinie` (3 lijnen na elkaar), `overdruk` (fase 2, PAARS: onderbreek met ~6%, of ren naar een roze plek) | Bij het onderbreken: "ONDERBROKEN!" in de balk, sterren boven zijn hoofd, en hij staat 2,5 s suf. |
| **Kruinroofvogel** | `vfx spawn kruinroofvogel` | `veerregen` (8 cirkels + 1 onder iedereen), `windkegel` (fase 2: duwt je weg) | Veren dwarrelen. |
| **Takelmeester Taleggio** | `vfx spawn taleggio` | `touwzwiep` (brede kegel), `kaasvaten` (fase 2: plakkerige kaascirkels) | Honingdruppels. |
| **Bombardier Brokkel** | `vfx spawn brokkel` | `kruisvuur` (kruis op jou), `dubbel_kruisvuur` (fase 2: + een kruis 45° gedraaid) | Stralen langs de armen van het kruis. |
| **Kapitein Roquefort** | `vfx spawn roquefort` | `sabelsnede` (tankbuster: grote klap op zijn doelwit, ga weg uit de cirkel), `sabelstorm` (fase 2: draaiend kruis om hem heen) | |
| **Graaf Piepvel** | `vfx spawn piepvel` | `capezwiep` (kegel voor, dan achter), `piepkoor` (fase 2, PAARS: onderbreek, of sta dicht bij hem; niet onderbroken = hij heelt 6%) | Rookwolkjes. |
| **De Wortelwurger** | `vfx spawn wortelwurger` | `wortelgreep` (cirkel onder iedereen + 5 los, plakkerig) | Blaadjes. |
| **De Nestwachter** | `vfx spawn nestwachter` | `nestschok` (eerst de ring: kom DICHTBIJ, dan de cirkel: ga WEG; het omgekeerde van de Galmwolf) | |
| **Baron Bleu** | `vfx spawn bleu` | `degenreeks` (3 snelle lijnen), `duet` (fase 2, GEEL: stapel, samen erin staan deelt de schade; alleen = 60%) | |
| **Vahoegnir** | `vfx spawn vahoegnir` (of `/guhs vadsrijk z3bazen vahoegnir herinnering`) | `vuurregen` (10 vuurcirkels + 1 onder iedereen), `hemelstorm` (fase 2-3: 2 roze veilige plekken), `vlammenkruis` (fase 3: een groot kruis in het midden, draait 45°, dan terug), `laatste_brul` (fase 3, PAARS: onderbreek, of sta in het midden) | Eigen aanvallen met een cast-balk: "Grauwe Huil", "Vleugelwind" (aanloop + verenschokgolf), "Grauw Vuur" (aanloop + een grijze vuurstraal langs de lijn). Een gouden spoor bij zijn duiken. In fase 4 (het ei en het einde) komen geen extra aanvallen. |

### Altijd checken
- **Verslagen:** de baas wiegt, huppelt en zwaait. Je ziet confetti, hartjes, een roze flits en een roze lichtzuil.
  Zone-2-bazen spatten in grijze kristalscherven.
- **Raken:** een klein schokje (niet vaker dan om de 1,5 s). Tijdens een aanloop schokt hij niet.
- **FPS:** zet alles op VOL en laat een baas een paar aanvallen doen. Het zijn maximaal 700 deeltjes. Op ZACHT zijn het er
  maximaal 250.
- **Shaders (Iris, indien aanwezig):** de vlakken en gloed moeten zichtbaar blijven.


## Itemisatie 2.0 (T-ITEMS): nieuwe vakken, nieuwe stats, Erfstukken, smid-vangnetten, drankjesriem

Alles onder `/guhs vadsrijk items ...` (op 2). Test in het Vadsrijk (de vakken en de riem werken alleen daar).

### 1. Nieuwe vakken: 2e ring, armband, handschoenen, cape
```
/guhs vadsrijk items nieuwe
/guhs vadsrijk items ringen
```
- Je krijgt alle 17 nieuwe stukken (Zeldzaam, jouw niveau) en 2 ringen.
- Open je inventaris (E).
- **Rechts staat een kolom van 7 vakjes:** R, R2, K, O, A, H, C (Ring, Ring 2, Ketting, Oorbel, Armband, Handschoenen, Cape).
- **Aandoen:** Shift+klik een stuk in je inventaris, of rechtsklik het in je hand.
  - De 1e ring gaat naar R, de 2e naar R2.
  - Een 3e ring wisselt met de ring in R.
- **Afdoen:** klik op het vakje.
- **Wat moet je zien:**
  - de tooltip van een armband/oorbel/handschoen/cape toont lagere getallen dan een ring van vroeger (het "vakbudget": 70 % voor sieraden, 60 % voor handschoenen en cape);
  - na het aandoen stijgen je stats.
- **Overzicht en reset:**
  - `/guhs vadsrijk items slots` laat zien wat je draagt;
  - `/guhs vadsrijk items uittrekken` doet alles af.
- **Stats bekijken:** `/guhs vadsrijk items stats` (de 4 nieuwe stats staan in het paars).
- **De inventariswissel:** loop het Vadsrijk uit en weer in. Je vakken moeten er nog zijn, en buiten het Vadsrijk tellen ze niet.
- **Sets:** de Kloddergrijpers, Sporencape, Kristalhandschoenen, Buitcape, Bloesemhandjes, Schorshandschoenen en Wortelcape tellen als setstuk. De tooltip zegt bij welke set.
  - Test: 3 stukken van de Klodderset + de Kloddergrijpers = de 4-stukbonus.
  - Voorbeeld: `/guhs vadsrijk items geef klodderkapje` (en klodderhesje, klodderbroek), plus de grijpers.
- **Losse stukken:** `/guhs vadsrijk items geef <basistype> [zeldzaamheid] [level]` (tab-aanvulling op de id).

### 2. Duidelijke stat-namen + 4 nieuwe stats
Hover over gear. Elke regel is gewoon te lezen, bijvoorbeeld:
- "+3 Kracht (schade van dichtbij)"
- "+2,5% Kans op kritieke treffer"
- "+4% Kortere wachttijd vaardigheden"

**De 4 nieuwe stats:**
- **Verdediging negeren %:** neemt dat deel terug van wat je klap kleiner maakt: een slecht element (x0,7), schilden en blokken van wezens, en hun Knuffelschild. Max 50 %.
- **Schade tegen bazen %:** meer schade op elke baas.
- **Leven per treffer:** zoveel Vads terug per raak (max 4 per tik bij een vlakaanval).
- **Kans op effect %:** je element zet zijn status:
  - vuur = Brandend
  - natuur = Gesporen
  - water = Plakkerig
  - roze = kort Verdoofd (niet op bazen)

  Max 40 %.

**Waar ze vallen:**
- alleen op wapens, ringen, handschoenen, armbanden, kettingen, oorbellen en capes;
- nooit op helm, borst, benen of voeten.

**Test:**
```
/guhs vadsrijk items geef pluisarmband legendarisch
```
Dat stuk heeft 4-5 bonussen; herrol het een paar keer bij de smid tot er een nieuwe stat op staat. Sla daarna een wezen en kijk naar de Vads/status.

### 3. Smid: tabs, geduld (pity), Knuffeltalisman, polijsten, previews
```
/guhs vadsrijk items smidspul
/guhs vadsrijk items smid
```
`smidspul` geeft steentjes, knabbels, draad, 5 talismannen, 5 groeipluis en 50 zilver.

**Bovenaan het aambeeld staan tabs:** Sterren · Bonussen · Groeien · Overvadsen.

**Sterren:**
- Zichtbaar: de kans, het risico, de prijs, en "Als het lukt:" met de echte stat/schade-verandering van de volgende ster.
- **Geduld:**
  - elke mislukte poging geeft +5 % op de volgende (tot ster 7);
  - het balkje "Smidguh wordt ongeduldig..." vult;
  - na een succes is het weer leeg.
  - Snel testen: neem een stuk in je hand, `/guhs vadsrijk items pity 4`, kies het bij de smid → "+20.0% geduld".
- **Knuffeltalisman:**
  - vink "☐ Knuffeltalisman" aan;
  - mislukt een ster die je normaal een ster kost? Dan krijg je "De Knuffeltalisman beschermde je ster!" en is er 1 talisman op;
  - "Koop 1" koopt er een voor munten;
  - verpluisd kan nog steeds (vanaf +6).
  - Sterrenvangen bestaat niet (zoals besloten).

**Bonussen:**
- Herrollen werkt als vroeger (klik een bonus = ♥ vasthouden).
- **Polijsten:** klik een bonus en je ziet "kan X tot Y worden (nu Z)". De knop Polijsten geeft een nieuwe waarde binnen dat bereik (kan ook lager!).

### 4. Erfstukken: Vertrouwd, Doorgroeien, Overvadsen
**Vertrouwd:**
- Gedragen gear (wapen in je hand, harnas, alle 7 vakken) krijgt item-xp per opgevrolijkt wezen (elite 3, baas 15).
- Tooltip: "Vertrouwd 2 (130/280 xp): +4% basisstats"; bij 5 een gouden regel.
- Bij elk niveau komt er een chatberichtje.
- Snel: `/guhs vadsrijk items vertrouwd 300` (alles wat je draagt).

**Doorgroeien:**
```
/guhs vadsrijk items legendarisch
```
- Je krijgt een Legendarisch wapen van je klasse dat al Vertrouwd 5 is.
- Smid → tab **Groeien**: je ziet "Niveau X → X+10" en de stat-verschillen, en wat het kost (Groeipluis + munten).
- "Laat groeien!" → het stuk is Gebonden, "Doorgegroeid I", Vertrouwd begint opnieuw.
- **Grenzen:**
  - Legendarisch groeit 2x (de 3e keer zegt hij dat het op is);
  - Guhddelijk groeit altijd (tot niveau 50);
  - Episch en lager groeit nooit.
- Groeipluis valt bij elke baas (1 per helper).

**Overvadsen:**
- Smid, tab **Overvadsen**:
  1. klik een ander (oud) stuk;
  2. kies links een bonus van het oude stuk, en rechts welke bonus hij vervangt (of "een nieuwe erbij");
  3. je ziet de nieuwe waarde en de kans (15 %) op een "lieve mislukking" (iets lager).
- Het oude stuk verdwijnt; het doel blijft altijd heel.
- Het mag niet als:
  - het doel die bonus al heeft;
  - de bonus niet op dat soort stuk mag;
  - het stuk verpluisd is.

### 5. Drankjesriem
```
/guhs vadsrijk items drankjes
```
- Dit vult de riem met Vads-, energie- en Vahoegdrankjes.
- **Links naast je hotbar** staan 3 vakjes met de toetsen **Alt+1 / Alt+2 / Alt+3** (Alt ingedrukt houden + cijfer; je hotbar-vakje wisselt dan niet). Druk erop om meteen te drinken.
  - Het gedeelde drankjes-wachten (10 s) telt: het vakje wordt donker.
  - Spam = "Je drankjes moeten nog even afkoelen."
- **In je inventaris:** een kolom "Riem" rechts van de sieradenkolom.
  - Klik met een drankje op je cursor = erin.
  - Klik leeg = eruit.
  - Shift+klik een drankje in je inventaris = in de riem.
- Alleen drinkbare dingen passen (geen steen e.d.).
- De toetsen kun je veranderen in Opties → Besturing → Vadsrijk.

### 6. Balans (één plek)
```
/guhs vadsrijk balans toon
/guhs vadsrijk balans items budget_sieraad 0.8
```
- `toon` laat de sectie `[items]` zien.
- Het tweede voorbeeld zet het sieradenbudget op 80 %, live.
- Alle sleutels en waarom: `NOTITIES_T_ITEMS.md` → "Balans".


## Rijdieren aankleden, 3 nieuwe rijdieren en huisdieren met een rol (T-RIJDIER)

Alles in het Vadsrijk testen (`/guhs vadsrijk`). Eerst even alles geven:
```
/guhs vadsrijk mount alle
/guhs vadsrijk rijdier garderobe alles
/guhs vadsrijk huisdier alle
```

### 1. Drie nieuwe landrijdieren
```
/guhs vadsrijk mount rijd duinhupper
/guhs vadsrijk mount rijd wolkwalkalfje
/guhs vadsrijk mount rijd zwamzwijntje
```
- **Duinhupper** (strandhaas, lange oren): hupt bij het lopen, springt het allerhoogst (bijna 6 blokken), iets langzamer.
- **Wolkwalkalfje** (wolkwalvisje op een eigen wolkje): zweeft, valt zacht en kan in de lucht nog een keer springen. Vinnetjes en staart wapperen.
- **Zwamzwijntje** (biggetje met een rode paddenstoel als zadel): stevig. Test de eerste tik:
  ```
  /guhs vadsrijk rijdier schrik
  /guhs vadsrijk rijdier schrik
  ```
  Eerste keer: "schudt de tik van zich af", je blijft zitten. Tweede keer (binnen 10 s): het rent weg.
- Druk **N**: de lijst heeft nu 8 rijdieren (kleinere rijen), de nieuwe staan er als silhouet bij als je ze nog niet hebt, met waar je ze haalt.

Hoe je ze echt krijgt (zonder dev):
- Duinhupper: vind 10 verstopte knuffels (alle zones samen). Het scherm toont "Al gevonden: x / 10".
- Wolkwalkalfje: 20 Gouden Veren, knop **Ruilen** in N. Veren geven: `/guhs vadsrijk rijdier veren 25`
- Zwamzwijntje: versla de Opperzwam (zone 1).
- Nieuwe Pluimguh-kleuren: **Kristal** (versla Spiegelzucht, zone 2) en **Lampion** (12 Gouden Veren, knop Ruilen bij de kleur).

### 2. Aankleden (N, rechts het knopje "Aankleden")
```
/guhs vadsrijk rijdier garderobe alles
```
Druk N, kies een rijdier, klik **Aankleden**. Drie rijen vakjes:
- **Vacht**: per rijdier 3 tot 5 kleuren (bijv. Sneeuwhert, Nachthert, Zonnewolf, Suikerspin-schaapje, Munthuisje voor Slakkie, Glimzwam voor het biggetje). De Pluimguh houdt zijn eigen kleurvakjes onder het plaatje.
- **Patroon**: vlekjes, streepjes, hartjes, sterretjes, glitter (glitter licht op in het donker).
- **Tuig**: zadeldekje, bloemenkrans, belletjes, hoge hoed, lampionnetjes (gloeien), kroontje. Het × -vakje = uittrekken.

Wat moet er gebeuren:
- Klik een vakje: het draaiende rijdier past het meteen (ook als je het nog niet hebt).
- Knop **Aantrekken**: daarna roept X het rijdier in die look. Zit je erop, dan verandert het meteen.
- Vacht en patroon veranderen NOOIT de snelheid of sprong.
- Zonder garderobe (`/guhs vadsrijk rijdier garderobe reset`): de vakjes hebben een "?", de tooltip zegt waar je het haalt.
  Bij de stal (`/guhs vadsrijk mount stal`) staat dan **Kopen (prijs)**; bij veren-dingen staat **Ruilen (n veren)**.

Snel een look op een rijdier zetten:
```
/guhs vadsrijk rijdier look knuffelhert vacht nacht
/guhs vadsrijk rijdier look knuffelhert patroon sterretjes
/guhs vadsrijk rijdier look knuffelhert tuig hoedje
/guhs vadsrijk rijdier look guhwolf tuig lampionnetjes
/guhs vadsrijk rijdier look pluimguh patroon glitter
/guhs vadsrijk rijdier look zwamzwijntje tuig geen
```
Let op (graag in de devclient kijken): zit het hoedje/de krans goed op het hoofd, hangen de belletjes voor de borst, hangen de lampionnetjes netjes aan de zijkant, ligt het dekje op het zadel? Bij elk rijdier anders; de plekken staan in `UiterlijkLaag.ANKERS`.

Waar alles vandaan komt (prestaties, bazen, geheimen) en wat je al gehaald hebt:
```
/guhs vadsrijk rijdier bronnen
/guhs vadsrijk rijdier garderobe inhalen
```
Een los ding geven: `/guhs vadsrijk rijdier geef tuig:kroontje` (of `patroon:hartjes`, `vacht:guhwolf:zon`).
Tuig bestaat ook als item (`vr_rijtuig_hoedje` enz.): rechtsklik = in je kast.

### 3. Huisdieren met een rol (P)
Huisdieren vechten NIET mee en worden nooit geraakt. Elk huisdier is **Steuner** of **Snuffelaar** en heeft een eigen kracht + een klein "Altijd"-bonusje. Ze worden level 1-10 door gevechten (baas = 10 xp).

Druk **P**: rechts staat nu Level x/10 met een balkje, een gekleurd label (roze Steuner / geel Snuffelaar), de kracht en wat die doet, en "Altijd: ...". Het naambordje "<huisdier> van <speler>" is hetzelfde gebleven.

Testen per huisdier (`laag` = Vads naar 30 %, energie 0, je telt als "in gevecht", cooldowns klaar):
```
/guhs vadsrijk maatje test klodderreus
/guhs vadsrijk maatje laag
```
-> binnen 1 s: "geeft je een klodderschildje!" (schild, slijm-deeltjes, hartjes).
```
/guhs vadsrijk maatje test koning_klodder
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje gevecht uit
```
-> buiten gevecht loopt je Vads elke seconde op (hartjes boven je hoofd).
```
/guhs vadsrijk maatje test galmwolf
/guhs vadsrijk maatje laag
```
-> je energie komt terug (muzieknootjes).
```
/guhs vadsrijk maatje test piepje
/guhs vadsrijk maatje strooi
```
-> 5 hoopjes goud 3 blokken verderop; Piepje raapt ze voor je op (alleen jouw buit).
```
/guhs vadsrijk maatje test kruinroofvogel
/guhs vadsrijk maatje snuffel
```
-> loop in de buurt van een geheim (knuffel, schat, aanwijzing) dat je nog niet hebt: elke paar seconden een gouden spoortje van het huisdier naar het geheim, en één keer "ruikt iets! Volg het gouden spoor...". `snuffel` zegt waar het dichtstbijzijnde geheim ligt.
```
/guhs vadsrijk maatje test vahoegje
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje vahoegje middel
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje vahoegje auto
```
-> klein: een roze vlammetje geneest je. Middel (na zone 5): Vahoegje wordt zichtbaar groter, grotere vlam + schildje. In P staat "Klein draakje" of "Jonge draak".

Levels: `/guhs vadsrijk maatje level 10` (kracht ~45 % sterker) of `/guhs vadsrijk maatje xp 50` (level-op berichtje + groene deeltjes).


## Opslag + HUD (T-OPSLAG): knapzak, materiaaltas, kluis, buffs, doelwit, gietbalk

Alles werkt alleen **in het Vadsrijk** (met je Vadsrijk-inventaris). Buiten het Vadsrijk gaat er niets in of uit.

### 1. Knapzak (per karakter): toets **I**
- Druk op **I** in het Vadsrijk. Of klik op het knopje **▤** links naast je gewone inventaris (E).
- `/guhs vadsrijk opslag knapzak` (of `... knapzak 2` voor vak 2)
- **Kijk naar:** een kistscherm met 3 rijen. Erboven staan **flapjes 1-5**. Rechts staan 3 knoppen: **Sorteren**, **Materialen ➜ tas** en **Materiaaltas**.
- **Wat moet er gebeuren:**
  - Begin: alleen flapje **1** is open. De andere tonen "?" of "+".
  - Hover over een dicht flapje: je ziet hoe je het krijgt (verhaal zone 1, verhaal zone 2, kopen, beloning later). Wat al klaar is, staat groen met ✔.
  - Klik op het "+"-flapje: de tekst vraagt "Klik nog een keer". Klik nog eens: je betaalt **25 zilver** en het vak gaat open.
  - Te weinig munten? Dan zie je een rode melding en gebeurt er niets.
  - Wisselen van vak: klik op een flapje, of druk **Ctrl+1..5**.
  - Druk nog een keer op **I**: het scherm gaat dicht.
- Vakken testen zonder verhaal:
  - `/guhs vadsrijk opslag knapzak ontgrendel proef1` → je krijgt een vak erbij (in de chat).
  - `/guhs vadsrijk opslag knapzak ontgrendel proef2`, enz. Er zijn maximaal 5 vakken.
  - `/guhs vadsrijk opslag knapzak reset` → de vakken gaan weer dicht. **De spullen blijven bewaard.**
  - Munten nodig? `/guhs vadsrijk munten 100000` (= 10 goud, in brons).

### 2. Materiaaltas (accountbreed, onbeperkt stapelen)
- Open hem met de knop **Materiaaltas** in je knapzak, met het knopje **✿** links naast je inventaris, of met `/guhs vadsrijk opslag tas`.
- **Automatisch oprapen:**
  - `/guhs vadsrijk opslag strooi`: er vallen 6 stapels rond je neer. Loop erdoorheen.
  - Steentjes, draad, knabbels en kristallen gaan **meteen in de tas**. Ze verschijnen niet in je inventaris. Boven je hotbar zie je "+5 Verbetersteentje in je Materiaaltas (5)". Je hoort een zacht tas-geluidje.
  - Het **brood** is geen materiaal. Dat gaat gewoon in je inventaris.
  - Werkt ook met een **volle inventaris** (vul je inventaris eerst met troep).
- **In het scherm:**
  - Je ziet een raster met alle soorten en hun aantal. Grote getallen worden afgekort (1.2k).
  - Er is een zoekvak.
  - Linksklik = een stapel aan je muis. Rechtsklik = de helft. Shift-klik = rechtstreeks naar je inventaris.
  - Klik met een materiaal aan je muis op het raster = terug erin. Shift-klik op een materiaal in je inventaris = erin.
  - **Alles opbergen** = alle materialen uit je inventaris gaan in de tas.
  - **Oprapen: aan/uit**: zet je het uit, dan gaan opgeraapte materialen weer gewoon in je inventaris.
- **Kristallen:** ruw en geslepen staan apart, als twee soorten.
- **De Smid:** die pakt Verbetersteentjes, Herrolknabbels en Pluisdraad **zelf uit je tas**. Je hoeft ze dus niet eerst te pakken.
  - Test: `/guhs vadsrijk opslag materiaal 50` en ga dan verbeteren bij de Smid.
- `/guhs vadsrijk opslag stand` → in de chat zie je hoeveel er in je knapzak, kluis en tas zit.

### 3. Kluis bij de bankier (accountbreed, in elke stad)
- Praat met een van deze bankiers. Je krijgt de keuze **"Naar mijn kluis"**.
  - **Zwamzoet:** de Bankier (gouden guh met monocle, in de bank).
  - **Knuffelkamp:** de Kok (zijn voorraadkast).
  - **De Haven:** de Havenmeester.
  - **Kandijkom:** de Veilingmeester (in het bankgebouw).
  - **Honingtak:** de Juwelier.
- `/guhs vadsrijk opslag bank` → opent de kluis, alsof je bij een bankier staat.
- **Kijk naar:** een kistscherm met 6 rijen (54 vakjes) en flapjes erboven.
  - Vak 2 kost 10 zilver, vak 3 50 zilver, vak 4 2 goud en vak 5 5 goud. Kopen gaat op dezelfde manier: klik op het "+"-flapje, en daarna nog een keer.
  - `/guhs vadsrijk opslag bank tabs 4` → alle vakken zijn open (dev).
- Loop je meer dan 8 blokken weg, dan gaat de kluis vanzelf dicht.

### 4. Dupe-veiligheid (even proberen te breken!)
- Pak iets op in de knapzak of kluis en houd het aan je muis. Doe dan een van deze dingen:
  - wissel van vak;
  - druk op Esc;
  - ga door een portaal terug naar huis;
  - log uit.
- **Wat moet er gebeuren:** het ding komt altijd precies één keer terug, in je Vadsrijk-inventaris. Nooit dubbel en nooit weg.
- Ga met de knapzak open terug naar huis: het scherm sluit vanzelf. **Thuis** opent I niets ("Dat kan alleen in het Vadsrijk").

### 5. HUD: buff-rij, doelframe, gietbalk
- **Buff-rij** (boven de Vads- en energiebalk):
  - `/guhs vadsrijk opslag hud alles` → je krijgt alle statussen die geen schade doen.
  - Links staan de **buffs** (groen randje), rechts de **debuffs** (rood randje). Elk icoontje heeft een eigen pixelplaatje, een donkere "zandloper" die volloopt, en de seconden in klein schrift.
  - Een nieuwe status flitst 2 s met zijn naam boven het icoontje.
  - Los: `/guhs vadsrijk opslag hud buff vahoeg 20` (of `bevroren`, `knuffelschild`, `snelpootjes`, `plakkerig`...).
- **Doelframe** (bovenaan in het midden):
  - Kijk naar een wezen (tot 24 blokken) of sla er een.
  - Je ziet: element-icoontje, (Elite), naam, "Nv X", een HP-balk met getallen, en eronder de statussen op dat wezen (als je het geslagen hebt).
  - Het blijft nog 5 s staan nadat je bent weggekeken.
  - Bij een baas met de grote baasbalk is het er niet.
  - `/guhs vadsrijk opslag hud doel` → het dichtstbijzijnde wezen wordt je doelwit (en krijgt even "Brandend" om te tonen).
- **Gietbalk:**
  - **Losknuffelen** (toets U) toont nu een balk "Losknuffelen..." boven de buff-rij. Als je beweegt of geraakt wordt, kleurt hij rood met "Onderbroken!".
  - `/guhs vadsrijk opslag hud giet 3` → een proefbalk van 3 s.
  - `/guhs vadsrijk opslag hud baasgiet 4` → een rode baas-gietbalk ("Grote Plonsstamp") onder de baasbalk, met een **gouden rand** (= onderbreekbaar). De questlijst rechtsboven schuift dan iets omlaag.
- **GUI-schaal:** check alles op GUI-schaal 2, 3 en 4. Er mag niets over de skills, de balken of de hotbar heen vallen.

### Toetsen
- **I** = knapzak. Ctrl+1..5 = van vak wisselen in de knapzak of kluis.


## Kledingkast, vleugels en effecten (T-KLEDING)

**Openen:** toets **O** (overal, ook thuis), of het tabje "Kledingkast" bovenaan Rijdieren/Huisdieren/Verkenningsboek, of de kleermakers (Zwamzoet, de breister in Kandijkom, de kleermaker in zone 3): keuze **"Naar de Kledingkast"**, of:
```
/guhs vadsrijk kledingkast
```
Alles is **per account** (niet per karakter): wat je ontgrendelt, blijft voor al je ridders.

### Snel alles testen
```
/guhs vadsrijk kleding alles
/guhs vadsrijk kleding kast
```
→ alles ontgrendeld (ook de "later"-dingen) + alle Vadsrijk-harnaslooks. Links zie je jezelf draaien met de muis; klik in een categorie op **Kies**: je ziet het meteen aan je poppetje.
- Overzicht wat je hebt + bron: `/guhs vadsrijk kleding lijst`
- Alles weer weg (ook voor opnieuw testen): `/guhs vadsrijk kleding wis`
- Eén ding geven / afpakken / aandoen: `/guhs vadsrijk kleding geef vleugel:kristal`, `.../kleding neem vleugel:kristal`, `.../kleding kies kaap:sterren`

### Vleugels + zweven (alleen in het Vadsrijk)
```
/guhs vadsrijk kleding vahoegnir
```
→ zoals na Vahoegnir (roze, einde zone 3): titelkaart **"Vahoegveertjes"**, een tip, en je hebt ze meteen om.
- **Zweven:** spring van iets hoogs en **houd spatie ingedrukt** → je zakt heel langzaam (nooit omhoog). Vooruit ingedrukt = ongeveer loopsnelheid. Geen valschade bij landen.
- Kijk of het mag: `/guhs vadsrijk kleding zweef` (zegt: ontgrendeld, welke vleugels, mag hier, zweeft nu).
- **Mag NIET** in bazen-arena's en kerkers (Spiegelburcht, Zeppelinwerf, Hart van de Boom): daar val je gewoon.
- **Thuis** zie je je vleugels wél op je rug, maar zweven doet niets.

Elke vleugel één eigen beweeg-trucje (geen gevechtsstats). Test met `kleding kies <id>`:
| vleugel | id | trucje | hoe test je het |
|---|---|---|---|
| Vahoegveertjes | `vleugel:vahoegveertjes` | gewoon zweven | spatie vasthouden in de lucht |
| Kristalvleugels | `vleugel:kristal` | dubbele sprong | spring, en tik in de lucht nog eens op spatie (1x per sprong) |
| Honingbijvleugeltjes | `vleugel:honingbij` | sneller zweven | zweven + W: duidelijk vlotter vooruit |
| Goudveren Vleugels | `vleugel:goudveer` | langer zweven | je zakt ongeveer half zo snel |
| Schemervleugels | `vleugel:schemer` | luchtdash | in de lucht nog eens spatie tikken: zwiepje vooruit |
| Pluisvleugeltjes | `vleugel:pluis` | zachte landing | spring van hoog zónder zweven: geen schade + roze plofje |
| Lampionvlieger | `vleugel:lampion` | langer zweven | (zone 4, komt later) |
| Sterrenstofvleugels | `vleugel:sterrenstof` | dubbele sprong | puzzelbeloning (t-puzzels) |
| Poollichtvleugels | `vleugel:poollicht` | langer zweven | puzzelbeloning |
| Lantaarnmotvleugels | `vleugel:lantaarnmot` | zachte landing | puzzelbeloning |

**Wat je moet zien aan de vleugels:** stilstaan/lopen = schuin naar achteren op je rug, springen/vallen = verder uitgeklapt, zweven = klapperen (op en neer). Verder dan 24 blokken: stilstaand model (LOD). Verf (tab Verf) kleurt vleugels + hoofdding.

**Waar komen ze vandaan (echt spelen):** Vahoegveertjes = Vahoegnir roze; Kristal = Splinterpieken 100% (B-boek); Honingbij = Schemerwortel 100%; Goudveren = 25 Gouden Veren verzameld; Schemer = zeldzaam (3%, laatste baas 8%) van een baas in Vahoeg-vorm; Pluis = geheim (één zweefvlucht van 100+ blokken omlaag, bv. van de kruin van de Schemerwortel naar beneden); Lampion = zone 4.

### Geheimen testen
```
/guhs vadsrijk kleding geheim zweefvlucht
/guhs vadsrijk kleding geheim kleermakers
```
→ Pluisvleugeltjes / Reiscape. Echt: Reiscape = de Kledingkast openen bij alle drie de kleermakers (Zwamzoet, Kandijkom, zone 3).

### Puzzelbeloningen (van t-puzzels)
```
/guhs vadsrijk kleding puzzel pz_vleugels_sterrenstof
/guhs vadsrijk kleding puzzel pz_cape_mos
```
→ doet hetzelfde als het oplossen van die puzzel. In de Kledingkast staan ze als "???" tot je ze hebt.

### Cape (rug-vak, over de stat-cape heen)
`kleding kies kaap:knuffelorde` (ook `roze`, `sterren`, `bloesem`, `reis`, `sneeuwvlok`, `honingraat`, `mos`) → in F5 een cape op je rug. Vleugels én cape tegelijk kan niet (één rugvak). Met vleugels om wordt een gewone cape verborgen.

### Hoofd, harnas, naam, titel
- **Hoofd:** `accessoire:oortjes`, `strikje`, `paddenhoedje`, `kristaltiara`, `honingkroon`, `feestmuts` (+ `lampionspeld` later). Zit ook netjes bovenop een helm.
- **Harnas (transmog):** tab Harnas, per stuk `<` `>`: "Je eigen stuk" / "Verborgen" / elk Vadsrijk-harnas dat je ooit in je tas had. Alleen zichtbaar **in het Vadsrijk** en alleen als je in dat vak echt iets draagt. Helm verbergen = "Verborgen" bij Helm.
- **Naamkleur:** tab Naamkleur → je naam boven je hoofd, in de tablijst en in de chat in die kleur (titel erachter blijft).
- **Titel:** tab Titel = de bestaande titels (zelfde als in de Guhdex).

### Effecten (één van elk actief)
- **Opvrolijken** (bij JOUW laatste klap op een wezen; bovenop de roze plof): confetti, regenboogje, hartjesballon, bloemenregen, knuffelregen, guh-giechel, "Njeg!"-letters. Snel kijken zonder vechten:
```
/guhs vadsrijk kleding plof plof:njeg
/guhs vadsrijk kleding plof plof:regenboog
```
- **Spoor** (lopen of rijden): bloemblaadjes, sneeuwvlokjes, sterretjes, honingdruppels, muzieknootjes.
- **Aura** (pas na ~3 s stilstaan, weg zodra je beweegt): roze gloed, pluisjes, vuurvliegjes.
- **Schuifjes** links onder in de Kledingkast: **Anderen: Alle/Minder/Uit** en **Eigen: Alle/Minder/Uit** (bewaard in `config/guhs_kleding.properties`). In een bazen-arena: sporen minder, aura's uit.
- Teller-beloningen testen: `/guhs vadsrijk kleding kills 1000` en dan `/guhs vadsrijk kleding check` → guh-giechel, paarse naam, Njeg!-letters.


## Spiekbrief: Ambachten (t-ambacht)

Alles hieronder werkt in het Vadsrijk. Commando's beginnen met `/guhs vadsrijk ambacht` (op 2). Toets **Y** = het Ambachtenboek
(ook als tabblad naast Rijdieren / Huisdieren / Verkenningsboek).

### 0. Snel alles klaarzetten
```
/guhs vadsrijk ambacht level alle 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht boek
```
- **Kijk:** 9 tegels (Mijnen, Houthakken, Plukken, Vissen, Imkeren, Smeden, Naaien, Koken, Brouwen), elk "Niveau 50 / 50" met een xp-balk.
- Klik op een tegel: je springt naar de recepten van dat ambacht.
- Terugzetten: `/guhs vadsrijk ambacht reset`.

### 1. Verzamelen (Mijnen, Houthakken, Plukken, Vissen)
```
/guhs vadsrijk ambacht reset
/guhs vadsrijk ambacht tp z1_pluk_kamp
```
- **Kijk:** rondom zie je kleine plantjes (azalea-bosjes) staan. Alleen JIJ ziet ze (een tweede speler ziet zijn eigen plekken).
- Kijk ernaar: onder je kruisje staat "Plukken · tier 1 (rechtsklik)".
- **Rechtsklik** = het spelletje (2-4 s):
  - Plukken: houd rechtsklik vast, laat los in het groene stuk.
  - Mijnen: drie keer tikken op het roze.
  - Houthakken: twee keer snel tikken.
  - Vissen: wacht op "BEET!" en klik meteen.
- Perfect = "extra vadsig", goed = "vadsig", niks doen = na 4 s gewoon. **Sluip + rechtsklik** = meteen gewoon oogsten.
- **Daarna:**
  - de plek verdwijnt voor jou en komt na 1-2 min ergens anders terug;
  - boven de hotbar "+30 Plukken-xp";
  - het materiaal zit in je tas.
- Andere zones / ambachten:
```
/guhs vadsrijk ambacht tp z1_mijn_grot_1
/guhs vadsrijk ambacht tp z1_hout_bos_1
/guhs vadsrijk ambacht tp z1_vis_vijver
/guhs vadsrijk ambacht tp z2_mijn_kloof_1
/guhs vadsrijk ambacht tp z3_vis_moeras
```
- **Te laag niveau** (bijv. zone 2 met Mijnen 1): de plek is half doorzichtig, met "niveau 20 nodig".
- Snel testen:
  - lijst van plekken in de buurt: `/guhs vadsrijk ambacht plekken`;
  - alles meteen weer vol: `/guhs vadsrijk ambacht vul`;
  - de dichtste plek oogsten zonder spelletje: `/guhs vadsrijk ambacht oogst`.
- **Zeldzame plek:** `/guhs vadsrijk ambacht zeldzaam`.
  - De dichtste plek wordt goudkleurig en glinstert, met "Glinsterplek: driedubbele oogst!".
  - Oogsten geeft 3x, en een kans op Glinstergruis, Oerbast, Gouden Klaver of Parelschelp.
- **Vissen:** 's nachts en bij regen bijten andere soorten. Een nieuwe soort geeft een chatregel "Nieuwe vis gevangen".
- **Anti-AFK:** blijf >10 min op dezelfde plek oogsten en je krijgt de helft (met een melding).
- **Uitgerust:** `/guhs vadsrijk ambacht uitgerust 600`.
  - Links onder staat "Uitgerust: 10 min dubbele ambachts-xp".
  - Oogsten geeft dubbele xp ("uitgerust: dubbel!").
  - Offline bouwt dit vanzelf op: 1 s per 6 s weg, max 2 uur.

### 2. Imkeren (zone 3)
```
/guhs vadsrijk ambacht tp z3_korf_bloesemdal
```
- **Kijk:** bijenkorfjes (half doorzichtig) bij de bloemen: "Lege korfplaats: klik om je korf te zetten".
- **Klik:** je korf staat (een hint-kaartje verschijnt) en de korf wordt een echte bijenkorf.
- Gele stipjes (bijen) vliegen tussen de korf en de bloemen.
- Raten vullen in echte tijd: 1 per ~10 min, sneller met meer bloemen, max 8. Snel vullen: `/guhs vadsrijk ambacht vul`.
- **Oogsten** = rook geven: houd vast en laat los in het groen.
  - Te vroeg = "chagrijnige bijen": je plakt even. Nooit schade.
  - Je krijgt honing per raat (Bloesemhoning / Schemerhoning / Wortelhoning, één per wei) + bijenwas + soms Koninginnengelei.
- Meer korven: 1 op niveau 1, 2 op 15, 3 op 30, 4 op 45.

### 3. Werkplaatsen in de steden (leermeester-guhs)
```
/guhs vadsrijk ambacht npcs
/guhs vadsrijk ambacht tpnpc amb_kok_z1
```
- De guhs staan in een ring rond het plein: Zwamzoet (8 + bordguh), Kandijkom (4 + bord), Honingtak (4 + bord + Imkerguh Zoemie bij de Raatweide).
  - Ze verschijnen pas als je in de buurt van het plein komt (zoeken zelf een vrij plekje).
  - Staat er een raar? Dan zoek je opnieuw met `/guhs vadsrijk ambacht scan` (en loop weg en terug).
- **Klik op een guh:** een praatscherm met de keuzes Werkplaats / Receptenboek / Vertel eens... / Dag!
  - Verzamelmeesters (Bonk, Spaan, Madelief, Dobbertje, Zoemie) hebben "Waar vind ik plekken?": uitleg + de dichtste plek met x/z.
- Zonder lopen: `/guhs vadsrijk ambacht werk koken` (of `smeden` / `naaien` / `brouwen`).

### 4. Koken (eten met buffs)
```
/guhs vadsrijk ambacht level koken 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht werk koken
```
- Links de recepten (grijs = nog te laag), rechts wat je nodig hebt (groen = genoeg).
- **Maak:** een balkje met een roze stuk; druk "Nu!" als het streepje in het roze staat.
  - Perfect = hogere Vadsigheid.
  - De tooltip toont "Vadsigheid ▮▮▮▮▮▮▮▮▯▯ 84%" en "Gemaakt door <jij>".
- **Maak 10x:** zonder spelletje, altijd 60%.
- **Eet** (rechtsklik):
  - links onder staat "Eten: Klodderpudding (29m 59s)";
  - in het Verkenningsboek van je stats zie je Vads +60 en Kracht +3 (x1,0 tot x1,3 door de Vadsigheid);
  - één buff tegelijk, 30 min, en hij blijft bij flauwvallen.
- **Feestmaal:** iedereen in je party binnen 10 blokken krijgt de buff + "Gezellig" (Kritiek +1, Heal-kracht/Elementschade/Elementweerstand +2%).
- Snel: `/guhs vadsrijk ambacht eten ridderontbijt` (dit geeft ook +5% gevechts-xp).

### 5. Brouwen (drankjes + kristallen slijpen)
```
/guhs vadsrijk ambacht werk brouwen
```
- Gebrouwen drankjes: dezelfde getallen als in de winkel, maar tot +30% door de Vadsigheid. Ze zitten in dezelfde 10 s drankjes-cooldown.
- **Kristal slijpen (geslepen):** kies rechts een kristalsoort (het getal = hoeveel ruwe je hebt). Drie ruwe worden één geslepen.
- **Stralend:** drie geslepen + 1 Glinstergruis worden één stralend (Brouwen 40).
- **Honingelixer:** Vads + energie, en het wast Plakkerig en Gesporen af.

### 6. Smeden + Naaien: Maatwerk en Meesterwerk (fase B)
```
/guhs vadsrijk ambacht level smeden 50
/guhs vadsrijk ambacht geef alles
/guhs vadsrijk ambacht werk smeden
```
- **Maatwerk:** kies bijv. "Amberkoperen Zwaard".
  - Kies 2 bonussen (de knopjes met de stat-namen; groen streepje = gekozen) en druk Maak.
  - Je krijgt een **Episch** stuk met jouw 2 bonussen + willekeurige, en minstens 1 sokje.
  - De tooltip toont "Maatwerk", "Zelf gekozen: ...", "Maatwerk telt niet voor een setbonus" en "Gemaakt door".
  - Met 100% Vadsigheid zitten je gekozen bonussen bovenaan hun bereik, en de naam wordt "Vadsig ...".
- **Naaien** maakt de broek + schoentjes, Smeden het zwaard / de boog / de staf / de helm / de borstplaat.
- **Meesterwerk-sieraden** (BiS) hebben een bazenkern nodig (zit in `geef alles`).
  - Kies 3 bonussen + 1 eigenschap, bijv. "Schade tegen bazen +8%".
  - Je krijgt een **Legendarisch**, gebonden sieraad. Draag het met rechtsklik.
- **Bazenkernen:**
  - elke baas 15% per helper (Zwamkern zone 1, Splinterkern zone 2, Wortelkern zone 3);
  - elites 3% een Kernsplinter;
  - 5 splinters = 1 kern in de smidse.
- Snel een stuk: `/guhs vadsrijk ambacht maatwerk zwaard 3` / `/guhs vadsrijk ambacht maatwerk ring 3`.

### 7. Bestelbord (fase B)
```
/guhs vadsrijk ambacht bestel
```
- 3 bestellingen van stadsguhs ("Bakker Bram wil 10 Pluisijzer.").
  - Ze passen bij jouw niveaus: met alles op 1 krijg je beginnersbestellingen.
  - Elke dag om 06:00 nieuwe.
- **Leveren** (als het balkje groen is) geeft ambachts-xp + munten.
- **Ruilen:** 1 gratis per dag.
- Na 12 bestellingen in een week: de **Ambachtskist** (rechtsklik: kernsplinters, iets zeldzaams, 5 zilver).
- Snel testen:
  - een bestelling klaarzetten (inclusief de spullen): `/guhs vadsrijk ambacht bestel klaar 0`;
  - de volgende levering geeft de kist: `/guhs vadsrijk ambacht bestel week`;
  - nieuwe set: `/guhs vadsrijk ambacht bestel nieuw`;
  - kist direct: `/guhs vadsrijk ambacht kist`.

### 8. Ranglijst (Ambachtsbord) + website (fase C)
- Bij de bordguh (Klerk Krabbel / Kriebel / Kraaltje) of het boek, tab "Ranglijst":
  - de top 10 per ambacht + "Totaal";
  - onderaan "Jij staat op plek X van de Y". Nooit meldingen in de chat.
- **Website-export:** `/guhs vadsrijk ambacht export`.
  - Dit schrijft `guhs-web/live/hiscores/ambacht_<ambacht>.json` + `ambacht_totaal.json` + `ambachten_recepten.json` (het pad staat in de chat).
  - Het gebeurt ook vanzelf elke 5 min en bij uitloggen.

### 9. Verhaaltjes + titels + spullen naar huis (fase C)
- Klik een leermeester → "Vertel eens...".
  - Per ambacht 5 stapjes, op niveau 1 / 10 / 20 / 35 / 50 (bijv. Koken: Oma Truus' receptenboek, Smeden: het Aambeeld van de Orde).
  - Elke stap geeft munten en een beetje xp.
  - De laatste stap stuurt een aandenken met de **Post** naar huis.
- Snel: `/guhs vadsrijk ambacht level koken 50` en `/guhs vadsrijk ambacht verhaal koken 4`, dan "Vertel eens...".
- Zonder de guh te zoeken: `/guhs vadsrijk ambacht npc amb_kok_z1`.
- **Naar huis (alleen versiering):**
  - Naaien maakt kokschort, vissershoedje, tuinhoedje, imkershoed, vaandel en klodderreus-knuffel;
  - Smeden maakt mijnhelmpje, Opperzwambeeld en Mini Koning Klodder;
  - alles gaat met de Post: buiten het Vadsrijk komt het in je gewone tas.
- **Titels:** Leerling-<ambacht> op niveau 25, Gezel-<ambacht> op 50, en "Ambachtskenner" voor 100% van het Ambachtsboek.
- **Verkenningsboek (B):** een eigen pagina "Ambachten" met de gevonden plekken per zone, vissen, honing en recepten. Telt NIET mee voor de zonepercentages.

### 10. Markt en winkel
- Materialen stapelen tot 64 (alleen dezelfde kwaliteit stapelt). Op de markt zet je dus een hele stapel tegelijk te koop.
- De winkelguhs kopen materialen, gerechten en drankjes nu ook:
  - T1 3 brons per stuk, T2 8, T3 15, zeldzaam 60;
  - vadsig x1,5, extra vadsig x2.


## Spiekbrief: Puzzels en geheimpjes (T-PUZZELS)

22 puzzels en geheimpjes in zone 1-3 (8 + 7 + 7). Elk heeft een beloning (Vadsmunten, soms een gescheurde bladzijde over de Keizer, spullen of iets voor je kledingkast). Ze staan in het **Verkenningsboek** onder **"Puzzels en geheimpjes"** en tellen mee voor het zonepercentage.

### Zo werkt het (voor jou)
- **Waar staan ze?** Een puzzel verschijnt vanzelf de eerste keer dat je binnen ~70 blokken van zijn plek komt. Dat is een vlak plekje vlak bij een kampvuur of POI. Daarna blijft hij daar staan.
- **Onderaan je scherm** staat steeds wat je moet doen (de "puzzeltip constant in beeld"). Geheimpjes zeggen niks.
- **Na 3 foute pogingen** krijg je tip 1 in de chat, **na 6** tip 2. Daarna staat de tip onderaan je scherm.
- **Opgelost?** Iedereen die vlakbij staat krijgt de beloning, maar **één keer per speler**. De puzzel zet zichzelf na ~20-30 s terug. Een half opgeloste puzzel zonder spelers in de buurt zet zich na 2 minuten terug.
- **Tijdritten en raadsels** zijn per speler (iedereen zijn eigen stopwatch of eigen antwoord).
- **Je antwoord op een raadsel** zien andere spelers niet in de chat.

### Dev-commando's (op 2)
| commando | wat het doet |
|---|---|
| `/guhs vadsrijk puzzel lijst` | alle puzzels: ✔ = jij hebt hem, met de plek (of "nog niet geplaatst" + anker) |
| `/guhs vadsrijk puzzel tp <id>` | naar de puzzel (staat hij er nog niet? Dan naar het anker; hij verschijnt binnen 1 s; doe dan nog eens tp) |
| `/guhs vadsrijk puzzel los <id>` | lost hem op via de echte invoer (klikken/platen/duwen/chat), dus je krijgt ook de beloning |
| `/guhs vadsrijk puzzel reset <id>` | zet de puzzel terug + vergeet je foute pogingen (tips weer weg) |
| `/guhs vadsrijk puzzel hint <id>` | toont uitleg, tip 1, tip 2, de boek-aanwijzing (en de antwoorden van een raadsel) |
| `/guhs vadsrijk puzzel vergeet <id>` / `vergeet alles` | jouw beloning vergeten (dan krijg je hem opnieuw) |
| `/guhs vadsrijk puzzel nacht` | sterrenpuzzels doen alsof het nacht is (aan/uit) |
| `/guhs vadsrijk puzzel plaats <id>` / `plaats <id> opnieuw` | nu plaatsen / een nieuw plekje zoeken |
| `/guhs vadsrijk puzzel hier <id>` | een testkopie vlak voor je neerzetten (overal, ook buiten het Vadsrijk; telt niet als de echte plek) |
| `/guhs vadsrijk puzzel bladzijde <n>` | bladzijde n (1-9) lezen + krijgen |
| `/guhs vadsrijk puzzel herlaad` | de puzzeldata opnieuw lezen |

Snelle rondgang: `/guhs vadsrijk puzzel tp z1_kleurplaten` → wacht 1 s → nog eens → puzzelen → `/guhs vadsrijk puzzel lijst`.

### Zone 1: De Knuffelzwammen
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z1_kleurplaten` | De Kleurplaten | drukplaten in volgorde | bij kampvuur Klodderweide 1 | **groen, roze, blauw, geel** (stap erop) | 4 zilver + bladzijde 1 |
| `z1_zwammenlied` | Het Zwammenlied | klankstenen (lied naspelen) | bij kampvuur Sporenwoud 1 | klik eerst de voorspeelsteen; dan **links, rechts, midden, rechts, voorste** | 5 zilver + 2 verbetersteentjes |
| `z1_lichtstraal` | De Verdwaalde Lichtstraal | spiegels + lichtstraal | bij lichtstraal 1 (Sporenwoud) | straal: 2× omlaag, 1× rechts; spiegel linksonder en rechtsboven niet nodig | 6 zilver + bladzijde 2 |
| `z1_gebarsten_muur` | De Gebarsten Muur | **geheimpje** (barst) | bij kampvuur Druipgrotten 3 | klik op de barst in het muurtje → de muur zakt in | 3,5 zilver + bladzijde 3 |
| `z1_fluistersteen` | De Fluistersteen | raadsel (chat) | bij kampvuur Klodderweide 3 | klik de steen, typ **vads** | 3 zilver + herrolknabbel |
| `z1_sterrenveld` | Het Sterrenveld | sterrenbeeld ('s nachts!) | Hoedhupperwei | klik de sterrenkaart; het is een **hartje**: rij 1: 2+4, rij 2: 1+3+5, rij 3: 1+5, rij 4: 2+4, rij 5: 3; dan de knop | 7 zilver + **Sterrenstofvleugels** (ontgrendeling) |
| `z1_vuurtorenschuur` | Het Schuurtje bij de Vuurtoren | **geheimpje** (verstopte hendel) | Knuffelkamp, bij de vuurtoren | hendeltje **achter** het schuurtje tussen de struikjes → de deur gaat open | 2 zilver + Klodderreus-knuffel |
| `z1_symboolslot` | Het Tempelslot | symboolslot | op weg naar de Zwamtempel (kampvuur 1) | **hart, ster, maan**, dan de knop | 8 zilver + 3 verbetersteentjes |

### Zone 2: De Splinterpieken
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z2_ijsschuif` | De Ijsblokken | schuifblokken | bij kampvuur Knuffelmeer 1 | linkerblok 2× omlaag, rechterblok 2× omhoog (klik = duwen van je af); vast? de gele herstelknop | 18 zilver + bladzijde 4 |
| `z2_kristallied` | Het Galmlied | klankstenen | bij kampvuur Galmhellingen 1 | **midden, linksonder, linksboven, rechtsonder, rechtsboven, midden** | 20 zilver + 3 verbetersteentjes |
| `z2_kristalspiegels` | De Kristalspiegels | spiegels + lichtstraal | bij kampvuur Splinterkloof 3 | omhoog bij de 1e spiegel, rechts, omhoog, dan helemaal naar rechts | 24 zilver + bladzijde 5 |
| `z2_poollicht` | Het Poollichtveld | sterrenbeeld ('s nachts!) | bij kampvuur Knuffelmeer 2 | **ruit met een stip**: rij 1: 3, rij 2: 2+4, rij 3: 1+3+5, rij 4: 2+4, rij 5: 3; dan de knop | 26 zilver + **Poollichtvleugels** |
| `z2_lantaarnsprint` | De Lantaarnsprint | tijdrit (25 s) | bij kampvuur Galmhellingen 2 | klik de lantaarn, spring over de kristalpilaren naar de gouden eindplaat bovenop | 22 zilver + **Sneeuwvlokcape** |
| `z2_ijsgrot` | Het Bevroren Holletje | **geheimpje** (barst) | bij kampvuur Splinterkloof 1 | klik op de barst | 15 zilver + bladzijde 6 |
| `z2_raadselsteen` | De Bibberende Steen | raadsel (chat) | bij het Meerkampje | klik de steen, typ **ijs** | 16 zilver + 2 herrolknabbels |

### Zone 3: De Schemerwortel (alles beneden langs de Wortelweg)
| id (tp) | naam | soort | waar | oplossing | beloning |
|---|---|---|---|---|---|
| `z3_wortelplaten` | De Wortelplaten | drukplaten | bij kampvuur Wortelweg 1 (oost) | **geel, blauw, roze, groen** | 45 zilver + 4 verbetersteentjes |
| `z3_bijenlied` | Het Bijenlied | klankstenen | bij kampvuur Wortelweg 2 | achterste rij 1+2, voorste rij rechts + links, achterste rij 3+4 | 50 zilver + bladzijde 7 + **Honingraatcape** |
| `z3_wortelschuif` | De Wortelknoesten | schuifblokken (3) | bij kampvuur Wortelweg 3 (west) | begin rechtsonder: 1× omlaag; dan de andere twee naar hun vakje | 55 zilver + 4 verbetersteentjes |
| `z3_lantaarnloop` | De Lantaarnloop | tijdrit (30 s) | bij kampvuur Wortelweg 4 | onderste rij naar rechts, rechterkant omhoog, bovenste rij naar links (de pilaren in het midden zijn doodlopend) | 60 zilver + bladzijde 9 + **Lantaarnmotvleugels** |
| `z3_mossymbolen` | Het Mosslot | symboolslot (4) | bij kampvuur Wortelweg 5 | **blad, druppel, zon, blad** | 50 zilver + **Moscape** |
| `z3_schorsdeur` | Het Schorshuisje | **geheimpje** (verstopte hendel) | bij kampvuur Wortelweg 6 | hendeltje achter het huisje | 40 zilver + bladzijde 8 |
| `z3_honingfluister` | De Zoemsteen | raadsel (chat) | bij de Mosvoet | klik de steen, typ **bij** (of bijtje/bijen) | 40 zilver + 3 herrolknabbels |

### Wat moet je zien
- **Bij aankomst:** een netjes vlak plekje met een echte vloer (mos, sneeuw of aangestampte modder per zone). Is er hoogteverschil? Dan lopen er traptreden naar het omringende land. Geen planten of struiken in de weg.
- **Drukplaten:** de plaat licht op als je erop stapt. Fout? Alles gaat uit met een bromtoon en je ziet "Hmm, dat klopt niet".
- **Klankstenen:** de voorspeelsteen speelt het lied en laat de stenen één voor één oplichten. Elke steen heeft zijn eigen toon.
- **Spiegels:** een geel lichtspoor (deeltjes) laat zien waar de straal nu loopt. Raakt hij het doel? Dan licht het doel roze op.
- **Schuifblokken:** klik = het blok schuift van je af. Een roze vakje licht op als er een blok op staat.
- **Tijdrit:** de lantaarn wordt goud en onderaan telt "⏱ Nog 24" af. Te laat: "De tijd is om!".
- **Sterren:** overdag zegt een steen "De sterrenstenen slapen nog...". 's Nachts (of met `puzzel nacht`) lichten ze op als je klikt. De sterrenkaart (houten blok) zet het sterrenbeeld als ★ in de chat.
- **Geheimpjes:** de barst of het hendeltje glinstert heel zachtjes (lichtniveau 3). Opgelost: de muur of deur gaat ~30 s open, met de titel "✦ Geheimpje gevonden".
- **Beloning:** een titel in beeld + chatregels: "+ 4 zilver", "+ een gescheurde bladzijde: ...", "+ ontgrendeld voor je kledingkast: ...". Een bladzijde lees je met rechtsklik.
- **Verkenningsboek:** onder "Puzzels en geheimpjes" (amethist-icoon) zie je per zone x/y. Geheimpjes heten "???" tot je ze vindt. De hint is een aanwijzing zoals "Bij het eerste kampvuur van de Klodderweide liggen vier gekleurde platen."

### De 9 gescheurde bladzijdes (de legende van de Lege Schaal)
1 Een gewone kleine Mika · 2 De eerste kaasknabbel · 3 Wat elke guh weet · 4 Lange armen · 5 Een gestolen kroontje · 6 De Lege Schaal · 7 Nooitgenoeg · 8 Het Oog · 9 Een eenzaam katje. Lezen: `/guhs vadsrijk puzzel bladzijde <n>`.
