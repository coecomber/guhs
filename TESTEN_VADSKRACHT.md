# Testen: 1.8.0 thema 2, Vadskracht

Start: `C:\Users\Juiced\Documents\MineScapeStory\guhs_work\w-int\run-client.bat`. Deze branch is 180-int.
Tip: zet creative aan (`/gamemode creative`) en pak de blokken uit de Guhs-tab, of gebruik de testcommando's hieronder.
Niets hiervan is al in de client bekeken: het gaat vooral om uiterlijk, gevoel en balans.

## Basis (begane grond)
- **Guhraden**, afnemende opbrengst: zet 6-10 Guhraden met guhs aan één Guhdraad-net. Kijk naar de uitlezing en tooltip: de eerste 4 tellen volledig mee, elk volgend rad levert 75% van het vorige, en na 16 raderen telt een rad niet meer.
- **Weer vadser**: standaardstraal 10. Wolkenantenne I/II/III erop, ernaast of rechtsklik erin geeft straal 20/35/60 (kost 30/45/70 VK). Het Knabbel-weerstation (Koepel) binnen 16 blokken geeft straal 150.
- **Knuffelsteen-generator**: kaassaus (emmer, Sausvat of Sausbuis) plus een waterbron ernaast en 4 VK. Er komt 1 knuffelsteen per seconde naar de kist, bank of buis.
- **Knabbelketel**: knabbels of knabbelblokken erin, levert 80 VK. Telt niet mee voor de afnemende opbrengst.
- **Sausbuis / Saustank**: buis van een vat naar de tank, met de meter op de tank.

## De Fabriekshal
- **Bouwblokken-fabriek**:
  - leg een voorbeeldblok in het middelste vakje;
  - koppel een bank met een banksleutel, of een kist met sneak + lege hand en daarna de kist aanklikken;
  - twee keer sneak-klikken maakt de koppeling los.
- **Wolkenschaapjes**: tammen met pluisgras of wolkensuikerspin, oppakken met sneak + lege hand, kweken met wolkensuikerspin, scheren voor pluis.
- **Klusjes**: "Scheren" en "Bloemetjes plukken".
- **Machines**:
  - Spinmachine: 2 pluis wordt 1 wol.
  - Verfbad: verft wol.
  - Knuffelrecycler: knuffels, kleding en wol worden pluis.
  - Scheerstal: scheert schaapjes binnen 6 blokken.
- **Kleurmolen**: een bloem geeft kleurstof plus 1-2 extra. **Regenboogpers**: pluis plus kaassaus geeft de gekozen kleur (sneak + lege hand = volgende kleur).
- **Kaaskas**:
  - geef de kern vadskracht en kaassaus en bouw zelf glas eromheen;
  - gewassen binnen 9×9 groeien snel (kaasknabbels, Pluisbolletjesplant, roze graan);
  - het klusje "Kassier" plukt en herplant.
- **Sorteerband**: leg banden achter elkaar. Het klusje "Sorteerder" legt items in de kist waar al zo'n item in ligt.
- **Hamsterbuis-net**:
  - één Hamsterknooppunt per speler, met een buffer van 4000 VK;
  - zet stations neer, ook in andere dimensies;
  - stap in de buis, kies een bestemming, en de reis kost 100 VK uit het net van het doel;
  - stations zijn openbaar of privé; vrienden komen uit de droomwolkje-vriendenlijst.

## De Koepel
- **Mega-oven**: 5 programma's. Lege hand wisselt van programma, sneak + lege hand haalt de baksels eruit.
- **Knabbel-weerstation**: eet 1 knabbel per 30 s. Een Weer vadser binnen 16 blokken krijgt straal 150.
- **Sterrenkoepel**: knabbels erin.
  - Per niveau licht een sterrenbeeld op; er zijn er 12.
  - Bij een nieuw niveau krijgt iedereen online 10 min "uitgerust".
  - Lege hand toont de top 5.
- **Draaimolen**: een knabbel is een rit van 30 s. Guhs in de buurt worden blij.
- **Wolkenvanger**: onder open lucht. Hoe hoger, hoe meer pluis.
- **Mega-knuffelcentrale** (5×3):
  - 16 guhs komen liggen en leveren tot 352 VK;
  - voer erin (knabbel, roze broodje, knabbelblok) en 1 wol per dag voor dekentjes;
  - zonder voer 25%, zonder dekentjes 75%.
- **Fossiel-broedmachine**:
  - fossiel plus 16 knabbels plus 20 VK geeft na 1 in-game dag een baby-oerguh: Mammoet-guh, T-Guh, Pteroguh of Oer-stokstaartje;
  - fossielen vind je in de kisten van de fossiel-opgraving, de fossielmijn, het reuzenguh-fossiel, de verlaten guhmijn en de grotschilderingen.
- **Kaasraket**:
  - `/guhs raket stap|spullen|bemanning|lanceer|maan|terug|reset`;
  - echt spelen: lanceerplatform → romp → motor → brandstof → bemanning (3 tamme guhs in een astronautenpakje en -helm) → lancering naar de Kaasmaan;
  - op de Kaasmaan: lage zwaartekracht, kaasmaankristal, Astronaut Kaasje, vlag planten en de terugkeercapsule.

## Export
- Vadsmonteur, Mijnwerker, Kristalslijper en Toverguh hebben elke dag 2 export-trades. Ze kopen bulk en geven zeldzame spullen.

## Nog niet in deze build
- De beurs zelf (begane grond-aanbouw, Fabriekshal en Koepel als echte structuur) volgt hierna.
- Het FTB-hoofdstuk Guh-technologie wordt daarna vereenvoudigd en compleet gemaakt.

## De vernieuwde Guhtechniek-beurs
- **Vinden**:
  - Superkompas, tab Knus;
  - of in de Guhmension `/locate structure guhs:guhtechniek_beurs`, dan `/tp`.
- **Snel een eigen beurs neerzetten** (creative, op een vlakke plek): `/place template guhs:techbeurs/guhtechniek_beurs ~ ~ ~`. Binnen een seconde zet de Beursbeheer de aanbouw (oostkant) en de Fabriekshal + Koepel (op het dak) erbij.
- **Bestaande werelden**: een beurs die er al stond, krijgt de aanbouw, Fabriekshal en Koepel vanzelf zodra hij geladen wordt. Staat er al iets van een speler (kist, bordje, guhs-blok) waar de aanbouw komt, dan doet hij het niet en staat er een waarschuwing in de log.
- **De route** (van simpel naar ingewikkeld):
  1. ingang west, door de hal;
  2. de deur in de oostmuur naar de aanbouw: Hapluikje, Snuffelsensor + Guhteller, Knuffelsteen-generator, Mika-rad, 4 Weer vadsers (straal 10, 20, 35, 60), Guhrad + Knabbelketel;
  3. trap of wolkenliftje naar het dakterras, dan de Fabriekshal in;
  4. Fabriekshal: noord de fabriek (Bouwblokken-fabriek, Wolfabriek, Knuffelrecycler, Scheerstal met Wolkenschaapjes, Kleurmolen, Regenboogpers, ketelstand), zuid de tier 5-logistiek (Vadszender/-ontvanger, Guhterminal + Bestelbalie, Knabbelpoort-buis + Thuispoort, Bouwkeet, Luchtblazer + Waterdrager, Korstduwer + Vuurkorf, Hamsterbuis-stations), midden de Kaaskas (met Kassier), Sorteerband (met Sorteerder), Saustank → Sausbuis → Saustank en de raderen-hoek (6 raderen op 1 net: afnemende opbrengst);
  5. trap of wolkenliftje (west) naar de Koepel: Mega-oven, Knabbel-weerstation, Draaimolen, Wolkenvanger, Fossiel-broedmachine, Hamsterknooppunt, Mega-knuffelcentrale met 16 guhs, de halve Kaasraket, Enderguhrad, Komeetkern, fossielvitrines en onder de glazen koepel de Sterrenkoepel.
- **Let op**:
  - alles draait echt (schakelaartjes uit/aan);
  - in survival kun je niets in de stands aanklikken behalve schakelaars en bordjes;
  - de Weer vadsers staan op normaal (ze doen dus niets aan het weer);
  - de Sterrenkoepel staat op niveau 6 en eet niets: anders zou de beurs iedereen bonussen geven;
  - de broedmachine broedt niet: anders komen er elke dag oerguhs bij.
