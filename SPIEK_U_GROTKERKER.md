# SPIEK U-GROTKERKER: elke grot is nu een herspeelbare mini-kerker

Elke zonegrot (De Zwamgrot, De Kristalgrot, De Wortelgrot, De Koraalgrot, De Wolkengrotten en, zodra zone 6 erin zit, De Zingende Zoutgrot) is een **run**:
grotkamer 1 (een groepje grotbewoners) → grotkamer 2 (een groter groepje + de elite) → de **grotpoort** gaat open → de **grotbaas** → een **eigen eindkist** voor iedereen die meer dan 5% meehielp. 10 minuten na de clear valt de grot weer in slaap en kun je opnieuw.

De puzzel, de eenmalige schat, de verzamelplekjes, de ontdekkingskaart en de boekjes van U-GROTTEN blijven gewoon zoals ze waren.

> Test in een **nieuwe wereld** (of met nieuwe tegels): de grotten zitten in de kaarttegels.
> Let op: het oude `/guhs vadsrijk grot reset <id>` (jouw ontdekt/elite/schat wissen) heet nu **`/guhs vadsrijk grot wis <id>`**. `reset` is nu de run.

## Snel overzicht

| Grot | Baas | Model | Kenmerkende aanval (vanaf de helft) | Zeldzaam huisdier | Zeldzame Pluimguh-kleur |
|---|---|---|---|---|---|
| `zwamgrot` (z1, lv 11-14) | Moeder Glimhoed | grote Sporenpriester | **Glimschuil**: alleen in de roze cirkels ben je veilig | Glimhoedje | Glimzwam |
| `kristalgrot` (z2, lv 22-25) | Tante Tinkelpoot | grote Kristalspin | **Kristalzang**: onderbreken (paars), anders heelt ze | Tinkeltje | Amethist |
| `wortelgrot` (z3, lv 44-48) | Ouwe Knoestbuik | grote Harsgolem | **Samen tillen!**: gele cirkel, samen erin staan (alleen = 60%) | Knoestje | Wortelhout |
| `koraalgrot` (z4, lv 53-56) | De Grote Zeurslak | grote Zeurslak | **Getij**: eerst de cirkel dichtbij, dan de ring (in-UIT) + Schelpzeur (onderbreken) | Zeurtje | Koraal |
| `wolkengrot` (z5, lv 58-60) | Opperpluis | reuze Grauwe Pluismijt | **Wolkenschuil**: eerst de ring, dan de cirkel (UIT-in) | Pluisje | Onweer |
| `zoutgrot` (z6, lv 64-67) | Koning Galmschaar | reuze Zoutkreeft | **Kristalschuil**: alleen de roze plekken zijn veilig | Galmpje | Zoutroze |

Op de helft van zijn Vads roept elke baas **2 grotbewoners** te hulp.

## 1. Een hele run (solo)

```
/guhs vadsrijk grot run zwamgrot
```
- Je staat in grotkamer 1. In de chat: "⚔ De grot wordt wakker! ...". Er staan **3** grotbewoners in kamer 1.
- Loop naar de grote zaal (kamer 2): **4** grotbewoners + **de elite** (Oude Zwamwachter).
- Loop naar de gang naar het elitehol: daar zit de **grotpoort** dicht (onzichtbare muur met roze glinsters + een bordje "Grotpoort - Versla eerst de grotbewoners: N over"). Tegen de poort aan lopen geeft "De grotpoort zit dicht...".
- Per lege kamer: "✔ Grotkamer 1 is leeg! (1/2)". Elite: "✔ De elite is verslagen!".
- Alles verslagen: "De grotpoort gaat open... de grotbaas wacht!" (deursgeluid), de poort is weg.
- Loop het elitehol in: **titelkaart** van de baas (de eerste keer ooit met camerazwaai), de baasbalk + **baasmuziek** (een geleend thema: Opperzwam / Spiegelzucht / Wortelwurger / Zeeslurf / Donderbuik / Zoutkolos).
- Kijk naar de aanvallen: roze/rode **vloervlekken** (decals) en de laadbalk onder de baasbalk ("Moeder Glimhoed laadt: Sporenkegel"). Op de helft: fase-moment + 2 helpers + de kenmerkende aanval.
- Baas verslagen: confetti, je **eigen eindkist** valt neer midden in het elitehol, titelkaart "Grot uitgespeeld! - 1 keer uitgespeeld", chat "★ Moeder Glimhoed verslagen! Je eindkist staat klaar." en "De grot slaapt over 10:00 weer in".
- Het bordje bij de poort telt af: "Open! Volgende run over 9:59".

Elke grot even proberen (snel naar binnen):
```
/guhs vadsrijk grot run kristalgrot
/guhs vadsrijk grot run wortelgrot
/guhs vadsrijk grot run koraalgrot
/guhs vadsrijk grot run wolkengrot
```

## 2. Meteen naar de baas (packs overslaan)

```
/guhs vadsrijk grot baas zwamgrot
/guhs vadsrijk grot tp zwamgrot elite
```
- De run start (als hij sliep), alle grotbewoners zijn weg, de poort is open, de baas staat in het elitehol.
- Zo kun je elke baas snel bekijken: vervang `zwamgrot` door `kristalgrot`, `wortelgrot`, `koraalgrot` of `wolkengrot`.
- Let op het **uiterlijk** (grootte, kleurtint), of de **aanvallen** passen in de kamer, en of de baas niet vast komt te zitten.

## 3. De eindkist en de buit

```
/guhs vadsrijk grot kist zwamgrot
```
- Er valt een eindkist voor je neer (2 blokken voor je). Open hem: munten (zone 1: 700 brons), **een uitrustingsstuk** op baas-level (de allereerste clear van een grot: Episch, daarna Zeldzaam met 12% kans op Episch), **3-5 ertsen** van de zone, soms (25%) de **zonekern**.
- Zeldzaam, direct gegeven (niet in de kist):
  - **Huisdier** (een mini-baas): 6% per clear, zeker na 25 keer mis.
  - **Pluimguh-kleur** (rijdier): 10% per speler per clear, zeker na 15 keer mis (de regel uit V-INT). Alleen als je al een Pluimguh hebt. De grotkleuren staan pas in het kleurenrijtje als je ze hebt.
- Pech-tellers bekijken/zetten:
```
/guhs vadsrijk loot kansen
/guhs vadsrijk loot pech grot_zwamgrot_pluim 15
/guhs vadsrijk grot kist zwamgrot
```
(de tweede kist geeft dan zeker de Glimzwam-Pluimguh als je een Pluimguh hebt)

## 4. Reset en opnieuw

```
/guhs vadsrijk grot run
```
- Lijst van alle grotkerkers: SLAAPT / BEZIG (nog x van y, spelers, poort dicht/open) / BAAS / KLAAR (reset over m:ss), en jouw clears per grot.
```
/guhs vadsrijk grot reset zwamgrot
```
- De grot valt meteen in slaap: alle grotbewoners en de baas weg, ongeopende eindkisten weg, poort open. Sta je nog in de grot, dan start er meteen een nieuwe run (nieuwe grotbewoners, poort weer dicht).
- Regels zonder commando:
  - **10 minuten** na een clear: de grot slaapt weer in (alleen als niemand meer achter de poort staat, anders wacht hij).
  - Een **onafgemaakte run** reset **1 minuut** nadat de laatste speler de grot uit is.
  - Een **uitgespeelde** grot wacht altijd de 10 minuten (ook als iedereen weggaat), zodat je niet kunt farmen door even naar buiten te lopen.

## 5. Met meer spelers (gedeelde wereld)

- Start je met 2 spelers in de grot: "(2 spelers: meer grotbewoners, een sterkere baas)", **4 + 5** grotbewoners, de elite krijgt 1 affix (3+ spelers: 2).
- De baas krijgt +75% Vads per extra speler die in de grot is op het moment dat hij wakker wordt.
- Komt er iemand bij tijdens een run: "⚔ Je doet mee met de run die al bezig is in De Zwamgrot." Er is nooit een tweede kopie; nieuwkomers doen gewoon mee.
- Een eindkist krijgt iedereen die **meer dan 5%** meehielp tegen de baas (schade, healen of tanken). Te weinig: "(Te weinig meegeholpen voor een eindkist...)" maar de clear telt wel.

## 6. Boek

- Verkenningsboek, zone 1-5, categorie **Grotten**: per grot nu 5 regels: ontdekt, elite, puzzel, schat en **"Grotbaas van de Zwamgrot verslagen"**.
- Zone 6 (als die erin zit): een eigen categorie "Grotten" met de Zingende Zoutgrot.

## 7. Oude commando's van U-GROTTEN (werken nog)

```
/guhs vadsrijk grot lijst
/guhs vadsrijk grot tp zwamgrot schat
/guhs vadsrijk grot elite zwamgrot
/guhs vadsrijk grot wis zwamgrot
/guhs vadsrijk grot kaart zwamgrot
```
- `elite` spawnt nu de elite van de run (de run start als de grot sliep).
- `wis` = jouw ontdekt/elite/schat van die grot vergeten (was `reset`).

## Wat je kunt melden
- Past de baas in de kamer (grootte, hitbox), loopt hij niet vast in de deco (paddenstoelen, stalagmieten)?
- Zijn de veilige plekken (roze) altijd bereikbaar, niet in een muur of meertje?
- Staan de grotbewoners niet in water of in de muur? (Ze zoeken zelf een vrije vloerplek in de kamer.)
- Is de poort echt dicht (niet ernaast langs te lopen) en gaat hij netjes open?
- Kleurtint en grootte van de baas: mooi of te gek? (Grootte/tint staan per baas in `GrotBazen.java`.)
- Het huisdiertje: goede grootte? Direct bekijken:
```
/guhs vadsrijk huisdier geef glimhoedje
/guhs vadsrijk huisdier kies glimhoedje
```
  (de andere: `tinkeltje`, `knoestje`, `zeurtje`, `pluisje`, `galmpje`)
