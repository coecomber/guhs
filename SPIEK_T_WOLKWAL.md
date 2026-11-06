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
