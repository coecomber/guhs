## U-Z5BINNEN: ronde kroonluchters en lichtere zalen in Hoogvads

**Wat is er veranderd?**
- De kroonluchters zijn geen houten roosters meer. Het zijn nu **ronde hangende kroonluchters**: gouden ringen met kaarsjes, kristallen druppels en kleine ronde kroontjes, aan kettingen van het plafond.
- Alle grote zalen zijn **veel lichter**:
  - marmeren dambordvloeren met gouden rozetten;
  - wandluchters (gouden wandkandelaars);
  - staande gouden kandelaars;
  - gloeiend goud in de gewelfribben.
- In de kathedraal liggen **gekleurde lichtvlekken** op de vloer onder de glas-in-loodramen.

Vergelijk met de renders `renders-hoogvads/ronde3/binnen2_voor_*` (oud) en `binnen2_*` (nieuw).

Bekijk alles in de **dev client, op een nacht**. Dan zie je het licht van de lampen het best:
```
/time set midnight
```

### De kathedraal
```
/guhs vadsrijk z5wereld plek kathedraal_schip
```
- **Boven het schip** hangen grote ronde kroonluchters. Ze hebben een ring van kaarsjes met kristallen druppels, kleine ronde kroontjes op de ring en hangers eronder, en ze hangen aan kettingen.
  - Lijkt er nog iets op een houten rooster of een steiger? Dat is fout.
- **Op elke pijler** zie je twee gouden wandluchters (op ooghoogte en hoger) en een vaandel.
- **Langs het middenpad** staan gouden kandelaars. De banken hebben gouden uiteinden.
- **De vloer** is wit en roze marmer in een dambordpatroon.
- **In de zijbeuken** liggen onder de ramen gekleurde tapijtvlekken in de vorm van een spitsboog (de "lichtvlekken").
- **Kijk omhoog:** sommige gouden ribben in de gewelven gloeien.
- **Het is nergens echt donker meer**, ook niet tussen de banken.

```
/guhs vadsrijk z5wereld plek kathedraal_altaar
```
- **Boven de kruising** hangt de reuzenkroonluchter. Hij heeft drie ringen met acht kroontjes en een gordijn van kristallen hangers.
- **Op de vloer** ligt een grote gouden rozet.
- **Het gouden guh-beeld** in de apsis is goed verlicht:
  - om de sokkel staan kandelaars;
  - achter het hoofd zit een stralenkrans van gloeiend goud.

### De bibliotheek
```
/guhs vadsrijk z5wereld plek universiteit_bibliotheek
```
- De vloer is van marmer.
- De bureaus en de galerijen zijn van licht hout (bleek eiken).
- In de boekenwanden loopt een gouden band.
- Onder en boven de galerijen hangen wandluchters.
- In het plafond zit gloeiend goud, en er hangen kleine ronde kroonluchters.

### De Raadszaal
```
/guhs vadsrijk z5wereld plek raadszaal_zaal
```
- Het **gouden rooster onder het plafond is weg**. Er zit nu gloeiend goud vlak in het plafond; er hangt geen rooster meer.
- Er hangen 3 ronde kroonluchters.
- Tussen de pilasters zie je wandluchters en vaandels.
- Bij de lessenaar staan 4 kandelaars.
- De vloer is een marmeren dambord met een rozet.

### De troonzaal en de kanselarij
```
/guhs vadsrijk z5wereld plek troonzaal_audientie
```
- De vloer is een marmeren dambord met een rozet.
- Er hangen ronde kroonluchters.
- Aan de zuilen hangen wandluchters en tussen de zuilen staan kandelaars.
- De koningin op de troon is goed verlicht.

```
/guhs vadsrijk z5wereld plek paleis_kanselarij
```
- Er hangen twee kleine ronde kroonluchters en aan de muren hangen wandluchters.
- De vloer is van marmer.

### De academiehal
```
/guhs vadsrijk z5wereld plek academie_kapitein
```
- Er hangen 2 ronde kroonluchters en aan de muren hangen wandluchters.
- Bij de kapiteinstafel staan kandelaars.
- De vloer is van marmer met een gouden rozet.

### Loopcheck (survival)
- Loop door alle zalen. Je mag **nergens blijven haken**:
  - kandelaars hebben een dun paaltje;
  - kroonluchters, wandluchters en hangers hebben geen botsing.
- De tapijtvlekken zijn gewoon tapijt.
- Alle deuren, trappen en plekken zijn nog vrij.

### De nieuwe blokken zelf
In de creative-tab **"Guhs: Bouwen"** staan:
- Kroonluchter;
- Gouden kaarsenrand;
- Kristallen hanger;
- Gouden wandluchter;
- Gouden kandelaar;
- Gouden kroonlijstmuurtje;
- Gouden lichtlijst.

Of geef ze jezelf:
```
/give @s guhs:kroonluchter 4
/give @s guhs:gouden_kaarsenrand 32
/give @s guhs:gouden_wandluchter 8
/give @s guhs:gouden_kandelaar 4
/give @s guhs:kristallen_hanger 8
```
Probeer dit:
1. Hang een kroonluchter aan het plafond. Dat is een rond 3D-model van ongeveer 3 blokken breed.
2. Leg een rij kaarsenranden naast elkaar. Ze verbinden zich met elkaar en met een kroonluchter.
3. Breek er één weg. De buren laten dan los.
4. Zet een wandluchter tegen een muur. Hij steekt van de muur af.
5. De kandelaar is 2 blokken hoog.
6. Alles geeft licht.
