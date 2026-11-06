## U-Z5: Hoogvads met diepte (kathedraal, universiteit, interieurs) + de figuranten

**Wat is er veranderd?** De gebouwen van Hoogvads zijn niet meer vlak:

- De **Ordekathedraal** en de **universiteit** zijn gotisch gemaakt, met diepte.
- **Paleis, Raadszaal, academie en station** hebben reliëf gekregen.
- **Elk raam in de stad** (ook van de huizen) heeft nu een druiplijst en een vensterbank.
- **Binnen** hangen grote kroonluchters aan kettingen.
- De **figuranten** (de guhs die in Hoogvads rondstaan) zien er weer uit als gewone guhs.

Vergelijk met de renders in `renders-hoogvads/ronde3/` (`voor_*` = oud, de rest = nieuw).

### De kathedraal van buiten

```
/guhs vadsrijk z5wereld plek kathedraal_plein
```

Kijk naar de westgevel (de kant met de Grote Poort). Je ziet:

- **Steunberen** tegen de torens. Ze springen naar boven toe terug, met schuine kapjes, een nis met een guh-beeldje, een guh-waterspuwer en een pinakel bovenop.
- **Blinde zijportalen** onderin de twee torens: twee lagen bogen met colonnetjes en gouden deuren.
- **De koningsgalerij**: een rij nissen met guh-heiligen boven de zijportalen.
- **Diepe torenramen** met een omlijsting die naar buiten trapt, met stenen maaswerk.
- **Galmgaten** met een omlijsting.
- **Het guh-roosvenster** met twee ringen eromheen (wit en goud), plus hogels.
- **In de Grote Poort**: colonnetjes met beelden op consoles langs de zijkanten.
- **Bovenop de torens**: grote pinakels op de hoeken, een kroon van spitsjes rond de grote spitsen en gouden hogels langs de spitsen.

Loop daarna om de kathedraal heen (de zuidflank):

```
/execute in guhs:vadsrijk run tp @s 2647 226 -553
```

Daar zie je:

- **Zijbeukramen** met een terugspringende omlijsting en maaswerk, met erboven een wimperg (een puntgeveltje met een pinakel).
- **Een open borstwering** op de dakrand.
- **Steunberen** met een nis met een beeld en een waterspuwer.
- **Twee rijen luchtbogen** naar het schip.
- **Pilasters** tussen de hoge ramen.

Aan de transeptgevels zie je:

- **Een diep portaal** met drie lagen.
- **Een roosvenster** met een ring eromheen.
- **Twee traptorentjes** met slanke spitsen.

Op het dak zie je **dakkapellen**, een gouden **nokkam** en **vier spitsjes** rond de koepel.

### De kathedraal van binnen

```
/guhs vadsrijk z5wereld plek kathedraal_schip
```

Kijk omhoog. Je ziet:

- **Kruisribgewelven** met gouden ribben.
- **Bundelpijlers**: slanke witte diensten die van de vloer tot het gewelf lopen, met gouden kapitelen.
- **Een triforium**: een rij kleine boogjes boven de arcade.
- **Grote kroonluchters** in het schip: drie ringen met kaarsen en lantaarns, aan lange kettingen.

```
/guhs vadsrijk z5wereld plek kathedraal_altaar
```

De kruising is nu **open tot in de gouden koepel**. Er hangt een **reuzenkroonluchter** (vier ringen) aan de koepel, en bij het altaar staan kandelaars. In de zijbeuken hangen kleine kroonluchters.

**Loop even rond:** de banken, het middenpad, het altaar, de torentrap en de klokkenvloer werken nog.

```
/guhs vadsrijk z5wereld plek kathedraal_geveltoren
```

### De universiteit

```
/guhs vadsrijk z5wereld plek universiteit_poort
```

Je ziet:

- **Een diep portaal** voor de poort, met pinakels. De trap naar de straat loopt erdoor.
- **Twee hangende hoektorentjes** met spitsjes aan het poortgebouw.

Loop rond de gevels. Je ziet:

- **Hoge gotische ramen** van twee verdiepingen, met goud, maaswerk en een omlijsting.
- **Getrapte steunberen** met pinakels op de dakrand.
- **Puntgeveltjes (dakkapellen)** met een rond raampje op de dakrand.
- **Lijsten** op de verdiepingsbanden.
- **Gouden hogels** op de torenspitsen.

```
/guhs vadsrijk z5wereld plek universiteit_bibliotheek
/guhs vadsrijk z5wereld plek universiteit_bibliotheek_galerij
```

In de bibliotheek zie je:

- **Een galerij langs beide boekenwanden**, met een reling en lantaarns. Je komt er met de **ladder** aan de westkant.
- **Kroonluchters** in plaats van de kale lampjes.

### Paleis, troonzaal en Raadszaal

```
/guhs vadsrijk z5wereld plek paleis_hof
/guhs vadsrijk z5wereld plek troonzaal_audientie
/guhs vadsrijk z5wereld plek raadszaal_zaal
```

- **Paleisgevel:** pinakels op de balustrade, guh-waterspuwers, lijsten onder de banden, en hogels op de vier ronde torens.
- **Troonzaal:** drie grote kroonluchters.
- **Raadszaal:** kroonluchters, wandpilasters met gouden kapitelen en een gouden cassettenplafond.

### Academie, station en huizen

```
/guhs vadsrijk z5wereld plek academie_oefenplein
/guhs vadsrijk z5wereld plek luchthaven_station
/guhs vadsrijk z5wereld plek zachtmoedlaan
```

- **Academie:** de muren en de donjon hebben een uitkragende borstwering op consoles (mezekouwen), en pinakels op de donjon.
- **Station:**
  - Tussen de bogen staan hangende steunberen met pinakels. Ze beginnen pas boven je hoofd, dus je loopt er gewoon langs.
  - De roosvensters op de kopse kanten hebben een ring.
- **Huizen:** elk raam heeft een druiplijst erboven en, als het hoger zit, een vensterbank eronder.

### Figuranten (de bug)

Voorheen leken de figuranten op een stapel kleren: alle zadels, harnassen en kledingstukken van het guh-model tegelijk. Nu horen ze eruit te zien als normale guhs, zoals Kadet Dons.

```
/guhs vadsrijk z5wereld plek markt_fontein
/guhs vadsrijk z5wereld bewoners
```

Kijk naar de marktgangers, de vluchtelingen bij de Soepkeuken en de kerkgangers in de banken:

- **Gewone guhs** in hun vachtkleur: geen zadel, harnas of hoedjes, en alleen de eigen vachtextra's (pluisguh, wolkguh, bloesemguh).
- **De bakker, burgemeester, kapitein, oma, luchtnon en professor** staan rechtop als hun eigen NPC-model.
- **Konijntjes** zijn echte konijntjes.
- **De ridders** zijn Wolkwacht-ridders.

**Het zaakje vernieuwen** (nieuwe figuranten laten spawnen):

```
/guhs vadsrijk z5wereld bewoners uit
/guhs vadsrijk z5wereld bewoners aan
```
