# Spiekbrief T-Z4BOOT: je eigen guh-zeilbootje (zone 4, De Lampionkust)

Alle commando's werken met op 2. In het Vadsrijk testen (`/guhs vadsrijk` brengt je erheen).

## 1. Snel varen (overal op water, ook zonder zone 4)
```
/guhs vadsrijk boot varen
```
- Geeft je het bootje (+ questvlag) en zet **proefvaart** aan: dan mag het bootje overal op water. Het verschijnt op het dichtstbijzijnde open water (3x3 water met lucht erboven, binnen 6 blokken) en je zit er meteen in.
- Geen water in de buurt? Dan krijg je "Hier is geen plek voor je bootje". Zoek een meertje of de zee op en probeer opnieuw.
- **Wat je moet zien:** een roze bootje met een guhgezichtje op de boeg (met oortjes), een zeil met een guhgezicht en oortjes bovenop, een hartjeswimpel bovenin de mast, een guhkopje als boegbeeld, een klein guh-matroosje voorin met een matrozenpetje, en achterin een lantaarntje aan een paaltje dat over het water hangt (gloeit). Jij zit achterin op het bankje bij het roer.

## 2. Besturing (het "zeilgevoel")
- **W** = zeil hijsen: het bootje komt langzaam op gang (ongeveer 3 seconden tot volle vaart, ~12 blokken per seconde op level 1).
- **Loslaten** = het bootje glijdt nog even door en wordt langzaam trager.
- **S** = remmen, en daarna heel langzaam achteruit.
- **A/D** = de boeg draaien. Je beeld draait mee. Op volle snelheid draai je wat trager.
- Het bootje **dobbert** zachtjes op en neer en schommelt (animatie). Bij snelheid zie je spetters bij de boeg en belletjes in het kielzog, en hoor je af en toe het water.
- **Wind:** rechts op je scherm staat een windpaneeltje met een pijltje (pijl omhoog = wind van achteren), "mee!" / "zijwind" / "tegenwind" en je snelheid in blokken per seconde. Met de wind mee ga je ~15% sneller, ertegenin ~15% trager. De wind draait langzaam mee met de dag.
- Op het strand of op land schuurt het bootje en komt het bijna niet vooruit (het klimt niet op het land).
- `/guhs vadsrijk boot waar` laat zien hoe de wind staat, waar je bent t.o.v. de Kringstroom en of er een steiger in de buurt is.

## 3. Rijdier-toets X en het N-scherm
- **X in het water of op een steiger (zee van zone 4)** = je bootje. **X op het land** = je landrijdier (ook als je het bootje als actief rijdier hebt gekozen).
- Heb je alleen het bootje en sta je op het land: "Je zeilbootje vaart alleen op het water".
- **N** (Rijdieren): het Zeilbootje staat in de lijst (6e rij) met level, xp en snelheid. Varen geeft xp, net als rijden.
- Bovenaan zit een nieuw **tabblad "Zeilbootje"** (tussen Rijdieren en Huisdieren). Ook: `/guhs vadsrijk boot scherm`.

## 4. Aankleden (varianten)
In het tabblad Zeilbootje: links het draaiende bootje, rechts drie rijen: **Zeil**, **Wimpel**, **Boegbeeld**.
- Klik een vakje: het voorbeeld laat het meteen zien. Daarna "Dit wil ik!" (heb je het al) of "Kopen (prijs)".
- Zeilen: roze (heb je meteen), munt / lavendel / zeeblauw (600 brons), honing (900), sterrennacht (beloning).
- Wimpels: hartje (meteen), ster / streepjes (300), Vadsrijk (beloning).
- Boegbeelden: guhkopje (meteen), dolfijntje / sterretje (800), draakje (beloning).
- Alles in één keer: `/guhs vadsrijk boot varianten`. Eén kiezen: `/guhs vadsrijk boot variant zeil_sterrennacht` (of `boeg_draakje`, `wimpel_ster`, ...).
- **Kijk:** als je vaart en iets anders kiest, verandert het bootje meteen.

## 5. Steigers (aanmeren) en uitloggen op zee
```
/guhs vadsrijk boot steigers
/guhs vadsrijk boot naar lampionhaven
```
- `steigers` toont alle aanlegsteigers met afstand (* = je laatste). **Let op:** zolang zone 4 nog niet gebouwd is, zijn dit placeholders (bij de eilandmiddens van de wereldindeling, y 64). Daar is dan nog geen water: proefvaart (punt 1) is dan de manier om te testen.
- Vaar je binnen ~10 blokken langs een steiger: in beeld "Steiger X: druk op Shift om aan te meren". Die steiger wordt je "laatste steiger".
- **Shift** naast een steiger = je stapt op de planken van de steiger (niet in het water). Het bootje poeft weg.
- **Uitloggen op zee** (terwijl je in het bootje zit): bij het inloggen sta je op je laatste steiger, met een berichtje. Snel testen zonder uit te loggen: `/guhs vadsrijk boot thuis`.

## 6. De Kringstroom (de rand van zone 4)
- Een ellips rond alle eilanden (midden 2140, -491; 470 x 290 blokken). In de buitenste rand (de laatste ~7%) duwt de stroom je terug: je snelheid naar buiten valt weg, je boeg draait vanzelf terug naar binnen, je ziet schuimwolkjes en "De Kringstroom duwt je bootje terug naar de eilanden!".
- **Wat moet gebeuren:** je komt er NOOIT doorheen, ook niet met volle zeilen recht naar buiten, en je raakt nooit vast: loslaten = je drijft vanzelf terug naar open water.
- Kan pas echt getest worden als de zee van zone 4 er is (t-z4wereld).

## 7. Geen andere bootjes
- Probeer in het Vadsrijk een gewone eiken boot of bamboevlot te plaatsen: dat gaat niet ("Andere bootjes drijven hier niet: in het Vadsrijk vaar je met je eigen guh-zeilbootje"). Dat geldt ook voor boten van andere mods (alles wat *boat* of *raft* heet).

## 8. Het bootje krijgen (verhaal) en de rest
- `/guhs vadsrijk boot geef`: zoals het verhaal het geeft (questvlag + tip "Je eigen zeilbootje!").
- `/guhs vadsrijk boot fles`: het item "Bootje in een fles" (rechtsklik = het bootje is van jou).
- `/guhs vadsrijk boot status`: bootje, questvlag, op zee, laatste steiger, level, gekozen uiterlijk.
- `/guhs vadsrijk boot proefvaart aan|uit`, `/guhs vadsrijk boot reset` (alles van het bootje weg).
- Gewone rijdier-commando's werken ook: `/guhs vadsrijk mount rijd zeilbootje`.
