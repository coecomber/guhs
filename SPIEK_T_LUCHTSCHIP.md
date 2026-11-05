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
| 0 | Kampsteiger (-235, 87, 1008) | Haven Zwamzoet (92, 65, 500) | kampverhaal (zoals de oude bel) |
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
