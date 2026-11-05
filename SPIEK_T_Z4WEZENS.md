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
