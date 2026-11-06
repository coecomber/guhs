# Spiekbrief zone 4 "De Lampionkust": het verhaal (t-z4verhaal)

Alles hieronder speel je in de Vadsrijk-dimensie, als OP. Een commando begint altijd met `/guhs vadsrijk z4verhaal ...`.

**Wat je ziet als iets van een ander nog ontbreekt:**
- **Het zeilbootje** is van t-z4boot. Zonder bootje zegt Botenguh Zeiltje dat het nog gebouwd wordt, maar de stap telt wel. Steek dan zwemmend over, of tp.
- **De bazen** (Zeeslurf, Kootje) zijn van een andere slice. Zonder die slice krijg je een kort gesprekje in plaats van een gevecht.
- **Het echte luchtschip** is van t-luchtschip. Zonder dat vlieg je met een camerafilmpje.
- **De kaart** is van t-z4wereld. Zonder die kaart staan NPC's en plekken op de render-coördinaten.

## Snel klaarzetten
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

## De hoofdlijn "De Grote Knuffeltij" (7 stappen, lv 50-55)
Volg steeds de tracker (rechtsboven) en de lichtstraal. Elke stap zegt wie, waar en wat.

### 1. Lampions aan de kust (`z4_hq01_lampionkust`)
- **Start:** Ordemeester Wasja in de Nestkapel (zone 3).
- **Wat gebeurt er:** na "Ja" vlieg je naar Lampionhaven.
  - De eerste keer zie je een filmpje: door de Wolkenval, over de eilandjes, Hoogvads op de walvis en grauwe vlekjes op het Knuffeltij-strand.
  - Je landt op het luchthavenplatform.
- **Daarna:** praat met **Havenmeester Ankertje** op de kade, bij de middelste steiger.
- **Kijk naar:**
  - Komt het filmpje mooi aan bij Lampionhaven?
  - Staat Ankertje echt op de kade?
  - Op de reiskaart heet zone 4 nu "De Lampionkust" (was "???").

### 2. De Grote Knuffeltij (`z4_hq02_knuffeltij`)
- Klik op **Botenguh Zeiltje** aan de noordelijke steiger: je krijgt je zeilbootje (of "wordt nog gebouwd").
- Vaar naar het **Knuffeltij-strand** (zuidoost) en praat met **Wasguh Sopje** bij de knuffelwas.
- Loop langs de vloedlijn naar **6 grauwe knuffels** (grijze stofjes in de branding).
  - Elke knuffel vertelt een klein verhaaltje en wordt roze, met hartjes.
  - De teller loopt tot x van de 12.
- Lever in bij **Opvangmoeder Zachtje** in het opvangcentrum.
- Snel testen: `tp tij.strand_1` ... `tij.strand_6`.

### 3. Waar kom jij vandaan? (`z4_hq03_waarvandaan`)
Er zijn 4 grote knuffels, elk met een **zachte flash van het grauwe oosten**: een grijze titel, grauwe as en 3 herinneringen.
- Strandfeesteiland zuidkust: de walrus. Posters "MAGER IS MOOI", de zuigende buis.
- Bloemeneiland westkust: de giraf. De lange grauwe weg, karren vol warme dingen, de burcht.
- Koraalbaai zuidring: de kwal. De Grauwe Vloot, de kapitein met ringen om haar tenen.
- Op de schildpad: de draak. De troonzaal, vijf vingertjes tik-tik-tik, "Meer."

Daarna praat je met **Opa Lichtstraal** onder de vuurtoren. Hij zegt: "ze komen uit de Grauwe Mistbank".

Snel testen: `tp poi.z4_grote_knuffel_1` tot `_4`.

### 4. De Grauwe Mistbank (`z4_hq04_mist`)
- Ga naar de **grauwe poort op het Mysterie-eiland** (zuidoost).
  - Je ziet de **mistflash**: een grauwe weg, Mika-karren, de zuigende buis, en een burcht als een kom onder een maan die kijkt.
- Praat met **Verdwaalde Nevel** (een vluchteling). Hij vertelt over de Grauwe Vloot en de Ringvinger.
- Lever in bij **Zachtje**: "Er zijn knuffels gestolen van de waslijn!" Stap 5 start vanzelf.

### 5. De Vliegervanger (`z4_hq05_vliegervanger`)
- Ga naar het **Wrak** ten noorden van de Koraalbaai en dan het ruim in.
  - Met de bazen-slice: het gevecht tegen Kootje.
  - Zonder: een grappig gesprekje (zijn vlieger zit vast aan zijn eigen staart, plons).
- Pak de **2 gestolen knuffels** in het ruim. Dat zijn nummer 11 en 12: "Alle twaalf knuffels zijn weer roze!"
- Lever in bij **Zachtje**.

### 6. Het terug-naar-huis-feest (`z4_hq06_feest`)
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

### 7. De Opstijgpier (`z4_hq07_opstijgpier`)
- Vaar naar de **Pierkop** (oost). Praat met **Pierwachter Wolkje** en loop de pier op naar **Kapitein Hemelhoog**.
- Lever in. Na het gesprek volgt de **cliffhanger**:
  - het luchtschip stijgt op langs het oog van de walvis, en je ziet Hoogvads;
  - terug op de pier dwarrelt uit de mouw van een guh met een wit-gouden cape een **grauw pluisje** (een hint naar de spion van zone 5);
  - daarna verschijnt "Wordt vervolgd...".
- **Kijk naar:** `status` geeft nu `zone4_klaar=true` en `z5_luchtschip_open=true`.

## Zijquests (16)
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

## Geheimen (Verkenningsboek, toets B, pagina "De Lampionkust")
- **8 knuffels** (hartjes): `tp knuffel.knuffel_1` t/m `_8`.
- **6 flessenpost** (blauwe glinsters): lore over de Keizer, Nooitgenoeg en de Ringvinger. `tp fles.fles_1` t/m `_6`.
- **4 schatten:** `schat.duintop`, `schat.lagune`, `schat.vliegertop`, `schat.mist`.
- **4 uitzichtpunten:** de Boom achter je, Hoogvads voor je, de Mistbank. `uitzicht.duin`, `uitzicht.vlieger`, `uitzicht.vuurtoren`, `uitzicht.pierkop`.
- **Verder in het boek:**
  - de bazen (Zeeslurf, Vliegervanger);
  - de kampvuren van 8 eilanden;
  - de gebieden;
  - een titel "Kenner van de Lampionkust".

## Diensten in Lampionhaven
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

## Wat moet je in de dev-client vooral checken
1. **NPC-plekken op de echte kaart:**
   - Staat niemand in een muur of in het water?
   - Staan de vluchtelingen bij het opvangcentrum?
2. **De knuffelplekken langs de vloedlijn:** liggen er echte grauwe knuffelblokjes? Dat is van t-z4wereld.
3. **De camerapaden:**
   - opening, mist, feest, wolkwal, cliffhanger;
   - kijkt de cliffhanger echt naar de walviskop?
4. **De tracker:** wijst hij vanuit zone 3 eerst naar Kapitein Zoemzeil, en in zone 4 naar het juiste eiland?
5. **De reiskaart:** staat "De Lampionkust" open na zone 3 en vlieg je erheen?
