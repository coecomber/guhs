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
