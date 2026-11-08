# SPIEK U-GROOTTE: kleinere mod + soepeler beeld

De mod-jar is ongeveer **190 MB kleiner**: de Vadsrijk-kaart (130 MB) en alle andere bouwwerken (70 MB) zitten nu samengeperst in
twee kleine pakbestanden (samen ~10 MB). De wereld die eruit komt is **precies hetzelfde**, blok voor blok.

## 1. Komt de kaart uit het pakbestand?
```
/guhs vadsrijk grootte status
```
- Groen: "Kaartpak gevonden: 1116 tegels, 937 verschillende. Tegel 0,0 geladen (… ms)."
- Oranje "NIET gevonden" betekent: de oude losse bestanden worden gebruikt (mag in een testomgeving, hoort niet in de echte jar).

## 2. Ziet de wereld er nog hetzelfde uit?
Maak een NIEUWE wereld (of ga naar een stuk Vadsrijk waar je nog nooit was) en loop rond:
```
/guhs vadsrijk
```
- Zwamzoet, de steiger bij aankomst, Hoogvads, de Lampionkust: alles staat er zoals altijd.
- Kisten, bordjes en andere blokken met inhoud zijn er ook nog.
- Andere bouwwerken buiten het Vadsrijk (kasteel, golfbaan, Guhmension-dingen) laden ook gewoon.

## 3. Soepeler beeld (FPS) in drukke plekken
Ga naar Hoogvads (veel bewoners) of een stad met veel NPC's en ruiters:
```
/guhs vadsrijk
```
- Bewoners/figuranten verder dan ~96 blokken worden niet meer getekend (de reuzen, zoals de vinnen van Moeder Wolkwal, wel).
- Kleding en patronen van guhs en rijdieren verder dan 64 blokken weg worden niet meer getekend (op die afstand is het een pixel).
- Dichtbij moet alles er precies hetzelfde uitzien als eerst.
- Druk **F3** en kijk naar de fps op dezelfde plek: die hoort hoger te zijn dan in de vorige versie.
- De spuitende Moeder Wolkwal is van ver iets minder wolkerig (de helft van de deeltjes), van dichtbij hetzelfde.

## Wat NIET veranderd is (jouw keuze)
- Geluid (117 MB) en plaatjes (172 MB) zijn niet aangepast: dat zou het geluid of het uiterlijk veranderen.
  Voorstel staat in NOTITIES_U_GROOTTE.md (lange muziek iets lager van kwaliteit, de grootste plaatjes half zo groot). Zeg het maar.
