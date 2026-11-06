## Karakters: drie vaste ridders (U-KARAKTER)

Nieuw: je hebt 3 vaste plekken, elk met een eigen spelmodus die NOOIT verandert:
- **links = Normaal**, **midden = Knuffelmodus** (☁, halve schade, 10% minder xp), **rechts = Eénvads** (❤, één leven, 10% meer xp).
- Je bestaande ridder staat automatisch in het slot van zijn modus (meestal links).
- Een Eénvads-ridder die flauwvalt buiten een stad gaat met pensioen: standbeeld in de Rustende Ridders-zaal, je speelt
  daarna verder met je linker (of middelste) ridder, en rechts kun je een nieuwe Eénvads-ridder maken.
- Wisselen: in een stad of bij een kampvuur (zoals het was).

### 1. Het welkomstscherm (de eerste keer, zonder commando)
Nieuwe wereld, eerste keer door het portaal. Of in je bestaande wereld:
```
/guhs vadsrijk karakter welkom vergeet
```
en dan het Vadsrijk uit en weer in. Wat moet gebeuren: NA het openingsfilmpje (of na ~2 s als er geen filmpje is) gaat
"Je ridders" vanzelf open met bovenin "Welkom in Het Vadsrijk! Kies hoe je wilt spelen...". In de chat staat waar je het
later terugvindt. Direct openen kan ook:
```
/guhs vadsrijk karakter welkom
```

### 2. Een ridder maken (de bug van de eerste keer)
In het scherm: middelste kaart "Maak een Knuffelridder" -> naam typen -> **Enter** (of "Maken en spelen").
Kijk: het scherm sluit, je staat als nieuwe ridder bij de Knuffelkamp en het verhaal begint opnieuw.
Mag het hier niet (bv. tijdens een filmpje)? Dan staat de rode reden nu BOVEN de knoppen in het maakpaneel.
Elke kaart toont bovenin zijn vaste modus (gekleurde balk; muis erop = uitleg). Er is geen "Makkelijker..." meer.

### 3. Het scherm vinden zonder commando
- Inventaris (E) in het Vadsrijk: derde knopje **⚔** links (onder knapzak en materiaaltas).
- Rijdieren/Huisdieren-scherm: tabblad **Ridders** helemaal links in de tabbladenrij.
- **Shift + rechtsklik op een kampvuur** (bij je eerste kampvuur komt hier 1x een tip over).
- Praat met de Grootmeester (ordetempel Zwamzoet) of een kampwacht: "Karakters: wissel van ridder of maak een nieuwe".
  De Rekruteerder (Knuffelkamp): "Karakters: hoe zat dat met die drie ridders?".
- Werver Wimpel bij het portaal vertelt het nu ook (3 nieuwe pagina's aan het eind). Opnieuw horen:
```
/guhs vadsrijk npc werver vergeet
```

### 4. Eénvads met pensioen
```
/guhs vadsrijk karakter maak eenvads Dappere Dirk
/guhs vadsrijk karakter wissel eenvads
/guhs vadsrijk karakter sterf-test
```
Kijk: titel "Met pensioen!", chatbericht voor iedereen, en ~8 s later wissel je vanzelf naar je linker ridder en gaat het
scherm open. De rechterkaart: "✦ Dappere Dirk - Rust in de Rustende Ridders-zaal" met de knop "Nieuwe Eénvads-ridder".
"Spelen" kan niet meer bij hem. Maak een nieuwe:
```
/guhs vadsrijk karakter maak eenvads Dirk Twee
/guhs vadsrijk karakter archief
```
`archief` toont dat de oude Dirk bewaard is (met data).

### 5. De Rustende Ridders-zaal (nu in de wereldgeneratie)
```
/guhs vadsrijk karakter zaal tp
```
Je staat BINNEN in de zaal (Zwamzoet, west van het plein, naast de Kruidenier, deur naar de havenpromenade).
Kijk: roze baksteen + kersenhouten dak met dichte gevels, glas-in-lood, hartjestegels met een roze loper, 11 kwarts sokkels
met de beelden, hanglampen, bloempotjes achterin; buiten een klinkerpad, twee lantaarnpaaltjes en twee guhvaandels.
Nep-ridders om beelden te zien:
```
/guhs vadsrijk karakter zaal test
/guhs vadsrijk karakter zaal lijst
/guhs vadsrijk karakter zaal leeg
```
LET OP: de zaal zit in de kaarttegels. Een wereld waarvan Zwamzoet al eerder gegenereerd was, heeft hem niet: dan
eenmalig `/guhs vadsrijk karakter zaal bouw` (noodgreep). In een nieuwe wereld hoort dat NIET nodig te zijn.

### 6. Overige dev-commando's
```
/guhs vadsrijk karakter lijst
/guhs vadsrijk karakter wissel normaal
/guhs vadsrijk karakter naam knuffel Sir Pluis
/guhs vadsrijk karakter migratie-test
/guhs vadsrijk karakter v1-test
```
`v1-test` controleert de omzetting van de oude (vrije) slots naar de vaste: allemaal "OK" verwacht.
