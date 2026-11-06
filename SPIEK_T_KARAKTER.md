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
