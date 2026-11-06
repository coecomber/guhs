## Rijdieren aankleden, 3 nieuwe rijdieren en huisdieren met een rol (T-RIJDIER)

Alles in het Vadsrijk testen (`/guhs vadsrijk`). Eerst even alles geven:
```
/guhs vadsrijk mount alle
/guhs vadsrijk rijdier garderobe alles
/guhs vadsrijk huisdier alle
```

### 1. Drie nieuwe landrijdieren
```
/guhs vadsrijk mount rijd duinhupper
/guhs vadsrijk mount rijd wolkwalkalfje
/guhs vadsrijk mount rijd zwamzwijntje
```
- **Duinhupper** (strandhaas, lange oren): hupt bij het lopen, springt het allerhoogst (bijna 6 blokken), iets langzamer.
- **Wolkwalkalfje** (wolkwalvisje op een eigen wolkje): zweeft, valt zacht en kan in de lucht nog een keer springen. Vinnetjes en staart wapperen.
- **Zwamzwijntje** (biggetje met een rode paddenstoel als zadel): stevig. Test de eerste tik:
  ```
  /guhs vadsrijk rijdier schrik
  /guhs vadsrijk rijdier schrik
  ```
  Eerste keer: "schudt de tik van zich af", je blijft zitten. Tweede keer (binnen 10 s): het rent weg.
- Druk **N**: de lijst heeft nu 8 rijdieren (kleinere rijen), de nieuwe staan er als silhouet bij als je ze nog niet hebt, met waar je ze haalt.

Hoe je ze echt krijgt (zonder dev):
- Duinhupper: vind 10 verstopte knuffels (alle zones samen). Het scherm toont "Al gevonden: x / 10".
- Wolkwalkalfje: 20 Gouden Veren, knop **Ruilen** in N. Veren geven: `/guhs vadsrijk rijdier veren 25`
- Zwamzwijntje: versla de Opperzwam (zone 1).
- Nieuwe Pluimguh-kleuren: **Kristal** (versla Spiegelzucht, zone 2) en **Lampion** (12 Gouden Veren, knop Ruilen bij de kleur).

### 2. Aankleden (N, rechts het knopje "Aankleden")
```
/guhs vadsrijk rijdier garderobe alles
```
Druk N, kies een rijdier, klik **Aankleden**. Drie rijen vakjes:
- **Vacht**: per rijdier 3 tot 5 kleuren (bijv. Sneeuwhert, Nachthert, Zonnewolf, Suikerspin-schaapje, Munthuisje voor Slakkie, Glimzwam voor het biggetje). De Pluimguh houdt zijn eigen kleurvakjes onder het plaatje.
- **Patroon**: vlekjes, streepjes, hartjes, sterretjes, glitter (glitter licht op in het donker).
- **Tuig**: zadeldekje, bloemenkrans, belletjes, hoge hoed, lampionnetjes (gloeien), kroontje. Het × -vakje = uittrekken.

Wat moet er gebeuren:
- Klik een vakje: het draaiende rijdier past het meteen (ook als je het nog niet hebt).
- Knop **Aantrekken**: daarna roept X het rijdier in die look. Zit je erop, dan verandert het meteen.
- Vacht en patroon veranderen NOOIT de snelheid of sprong.
- Zonder garderobe (`/guhs vadsrijk rijdier garderobe reset`): de vakjes hebben een "?", de tooltip zegt waar je het haalt.
  Bij de stal (`/guhs vadsrijk mount stal`) staat dan **Kopen (prijs)**; bij veren-dingen staat **Ruilen (n veren)**.

Snel een look op een rijdier zetten:
```
/guhs vadsrijk rijdier look knuffelhert vacht nacht
/guhs vadsrijk rijdier look knuffelhert patroon sterretjes
/guhs vadsrijk rijdier look knuffelhert tuig hoedje
/guhs vadsrijk rijdier look guhwolf tuig lampionnetjes
/guhs vadsrijk rijdier look pluimguh patroon glitter
/guhs vadsrijk rijdier look zwamzwijntje tuig geen
```
Let op (graag in de devclient kijken): zit het hoedje/de krans goed op het hoofd, hangen de belletjes voor de borst, hangen de lampionnetjes netjes aan de zijkant, ligt het dekje op het zadel? Bij elk rijdier anders; de plekken staan in `UiterlijkLaag.ANKERS`.

Waar alles vandaan komt (prestaties, bazen, geheimen) en wat je al gehaald hebt:
```
/guhs vadsrijk rijdier bronnen
/guhs vadsrijk rijdier garderobe inhalen
```
Een los ding geven: `/guhs vadsrijk rijdier geef tuig:kroontje` (of `patroon:hartjes`, `vacht:guhwolf:zon`).
Tuig bestaat ook als item (`vr_rijtuig_hoedje` enz.): rechtsklik = in je kast.

### 3. Huisdieren met een rol (P)
Huisdieren vechten NIET mee en worden nooit geraakt. Elk huisdier is **Steuner** of **Snuffelaar** en heeft een eigen kracht + een klein "Altijd"-bonusje. Ze worden level 1-10 door gevechten (baas = 10 xp).

Druk **P**: rechts staat nu Level x/10 met een balkje, een gekleurd label (roze Steuner / geel Snuffelaar), de kracht en wat die doet, en "Altijd: ...". Het naambordje "<huisdier> van <speler>" is hetzelfde gebleven.

Testen per huisdier (`laag` = Vads naar 30 %, energie 0, je telt als "in gevecht", cooldowns klaar):
```
/guhs vadsrijk maatje test klodderreus
/guhs vadsrijk maatje laag
```
-> binnen 1 s: "geeft je een klodderschildje!" (schild, slijm-deeltjes, hartjes).
```
/guhs vadsrijk maatje test koning_klodder
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje gevecht uit
```
-> buiten gevecht loopt je Vads elke seconde op (hartjes boven je hoofd).
```
/guhs vadsrijk maatje test galmwolf
/guhs vadsrijk maatje laag
```
-> je energie komt terug (muzieknootjes).
```
/guhs vadsrijk maatje test piepje
/guhs vadsrijk maatje strooi
```
-> 5 hoopjes goud 3 blokken verderop; Piepje raapt ze voor je op (alleen jouw buit).
```
/guhs vadsrijk maatje test kruinroofvogel
/guhs vadsrijk maatje snuffel
```
-> loop in de buurt van een geheim (knuffel, schat, aanwijzing) dat je nog niet hebt: elke paar seconden een gouden spoortje van het huisdier naar het geheim, en één keer "ruikt iets! Volg het gouden spoor...". `snuffel` zegt waar het dichtstbijzijnde geheim ligt.
```
/guhs vadsrijk maatje test vahoegje
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje vahoegje middel
/guhs vadsrijk maatje laag
/guhs vadsrijk maatje vahoegje auto
```
-> klein: een roze vlammetje geneest je. Middel (na zone 5): Vahoegje wordt zichtbaar groter, grotere vlam + schildje. In P staat "Klein draakje" of "Jonge draak".

Levels: `/guhs vadsrijk maatje level 10` (kracht ~45 % sterker) of `/guhs vadsrijk maatje xp 50` (level-op berichtje + groene deeltjes).
