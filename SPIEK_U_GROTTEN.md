## Grotten (U-GROTTEN): in elke zone één sfeergrot

Elke zone heeft nu een handgebouwde grot met een eigen thema. In elke grot vind je:
- 2 soorten **ambachtplekjes**;
- 1 **elite** (komt na 5 minuten terug);
- een **puzzel**;
- een **schatkist** die pas opengaat als de puzzel is opgelost.

Het Verkenningsboek heeft per zone een categorie **Grotten** met 4 vinkjes per grot: ontdekt, elite, puzzel en schat.

**Let op: nieuwe wereld nodig.** De grotten zitten in de kaarttegels. Een al gegenereerd stuk wereld verandert niet, dus test in een verse wereld (of laat de chunks opnieuw genereren).

### Snel rondkijken
```
/guhs vadsrijk grot lijst
/guhs vadsrijk grot tp zwamgrot
/guhs vadsrijk grot tp kristalgrot
/guhs vadsrijk grot tp wortelgrot
/guhs vadsrijk grot tp koraalgrot
/guhs vadsrijk grot tp wolkengrot
```
`tp <grot>` zet je bij de ingang (buiten). Met een plek erachter ga je direct naar binnen: `binnen`, `hal`, `puzzel`, `elite`, `schat`, `amb_1`, `amb_2` (en bij de koraalgrot ook `uitgang`). Bijvoorbeeld:
```
/guhs vadsrijk grot tp zwamgrot hal
```

### Per grot: wat moet je zien?

**1. De Zwamgrot (zone 1, Sporenwoud, lv 11-13).**
- **Ingang:** een reuzenpaddenstoel. Hij is hol, met een deur naar het oosten en lantaarntjes ernaast. Een paadje loopt naar het bospad.
- **Binnen:** een wenteltrap rond een gloeiende steel naar beneden.
- **Zalen:** de Sporenhal, dan de grote Glimzaal. Daar staan gloeiende paddenstoelen, kleine reuzenzwammen, shroomlights in het plafond en zwevende sporen. Er zijn pluk- en mijnplekjes.
- **Puzzel:** de puzzelhal met "Het Echolied" (zingende stenen).
- **Elite:** het hol van de Oude Zwamwachter.
- **Schat:** een schatnisje met een kist op goud.

**2. De Kristalgrot (zone 2, in de Galmtop, lv 22-24).**
- **Ingang:** een kristallen poort in de westflank, met zielenlantaarns.
- **Zalen:** de Kristalhal (mijnplekjes), daarna een zaal met een **ijsmeer**. Het meer heeft een bevroren rand en een open midden: daar zijn de visstekjes.
- **Sfeer:** amethist- en kristalpieken en sneeuwsterretjes.
- **Puzzel:** "De IJsspiegels" (lichtstraal).
- **Elite:** de IJskoude Wacht.

**3. De Wortelgrot (zone 3, onder de Wortelvelden ten noorden van de stam, lv 44-46).**
- **Ingang:** een holle wortelstronk vlak naast de Wortelweg, met een wenteltrap naar beneden.
- **Wortelhal:** wortelpilaren (houthakplekjes), mijnplekjes, hangende wortels en gloeibessen.
- **Boomwortels:** de echte wortels van de Wereldboom die door het plafond steken, zijn blijven staan.
- **Puzzel:** "De Wortelknopen" (schuifpuzzel).
- **Elite:** de Grote Wortelslang.

**4. De Koraalgrot (zone 4, onder de oostrand van de Koraalbaai, lv 53-55).**
- **Ingang:** **duiken!** Op het water ten oosten van de atol drijft een vlotje met een lantaarn. Recht daaronder ligt een koraalboog op de zeebodem.
- **De gang:** volg de zeelantaarns en het zeegras. Om de ~8 blokken zit een **luchtbel** om adem te halen.
- **Koraalhal:** je komt boven in een poeltje (met zeeaugurken) in een droge hal, met nepkoraal en zeelantaarns. Daar zijn vis- en mijnplekjes.
- **Uitgang:** een wenteltrap omhoog naar een koraalschoorsteen op de rand van de atol. Dit is ook de weg naar binnen voor wie niet wil duiken.
- **Puzzel:** "Het Schelpenpad" (gekleurde platen).
- **Elite:** Koning Mopperkrab.

**5. De Wolkengrotten (zone 5, in de kop van Moeder Wolkwal, lv 58-60).**
- **Ingang:** het **Blaasgat** bovenop de walviskop, vóór de stadspoort, met een hekje eromheen en de opening naar het oosten.
- **Binnen:** een wenteltrap naar beneden.
- **Zalen:** de Wolkenhal (mijn- en plukplekjes) met wolkenpluimen, parelmoerlampen en witte asdeeltjes.
- **Puzzel:** "Het Wolkenslot" (symboolstenen).
- **Elite:** de Grauwe Wolkduivel, in een grijs wolkenhol.

### Testen per onderdeel
- **Ontdekkingskaart.** De eerste keer dat je een grot inloopt, zie je eerst de normale gebiedstitel. Een tel later komt een gouden titelkaart (naam + "Grot ontdekt!"), met een belletje en een chatregel. Opnieuw zien:
  ```
  /guhs vadsrijk grot wis zwamgrot
  ```
  Of alleen de kaart tonen:
  ```
  /guhs vadsrijk grot kaart zwamgrot
  ```
- **Biome.** Druk binnen in de grot op F3. Je hoort "Het Vadsrijk - De Zwamgrot" enz. te zien. Op de grond erboven zie je weer het normale gebied.
- **Geluid.** Binnen speelt het grot-bed: druppels. Af en toe hoor je een druppel of kristal-ping met een zachte echo.
- **Elite.** De elite staat in zijn hol. Versla hem: je krijgt een chatregel, een klein buitje munten, en de eerste keer ook een zeldzaam stuk uitrusting. Direct spawnen (sta minstens 12 blokken weg):
  ```
  /guhs vadsrijk grot elite zwamgrot
  ```
- **Puzzel.**
  ```
  /guhs vadsrijk puzzel tp grot_zwamgrot
  ```
  Dat werkt ook met `grot_kristalgrot`, `grot_wortelgrot`, `grot_koraalgrot` en `grot_wolkengrot`. Snel oplossen:
  ```
  /guhs vadsrijk puzzel los grot_zwamgrot
  ```
- **Schat.** Loop naar de kist.
  - Zonder puzzel: "De schatkist zit op slot..." (plus een slotgeluidje).
  - Met puzzel: een gouden titel "Grotschat!", munten en een episch stuk uitrusting. Dit krijg je één keer.
  - Glinsteringen bij de kist laten zien dat er nog iets te halen valt.
- **Ambachten.** Toets Y > Plekken, of bekijk de plekjes in de hallen. Per grot zijn er 2 groepen, bijvoorbeeld in de Zwamgrot plukken + mijnen.
- **Boek.** Zie het Verkenningsboek > zone 1..5 > Grotten. Daar staan 4 regels per grot.

### Wat je kunt melden
- **Uiterlijk:** hoe de ingangen eruitzien (zijn ze uitnodigend genoeg?), de lichthoeveelheid, en of de deeltjes te veel of te weinig zijn.
- **De duikgang van de Koraalgrot:** zitten de luchtbellen ver genoeg uit elkaar?
- **De wenteltrappen:** lopen ze lekker, zonder springen?
