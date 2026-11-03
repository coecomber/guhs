# Testen: de wereld van zone 1 (Het Vadsrijk)

## Snel rondkijken
- `/guhs vadsrijk` brengt je naar de aankomst: het lage steigertje aan de zuidkust van Knuffelkamp. Loop de houten trap op
  naar het kamp op de klif.
- `/guhs vadsrijk wereld plek <id>` zet je op elke plek uit POI_VADSRIJK.md (tab-aanvulling werkt), bijvoorbeeld:
  `plein`, `ordetempel_binnen`, `zwamtempel_voorhal`, `arena_koning_klodder.centrum`, `uitkijk_druipspits`, `kristalkamer`,
  `slaapheuvel`, `fossielbrug.van`, `schildpad_eiland`, `luchtschip_haven_bel`.

## Waar op letten
- **Knuffelkamp**: tenten, Ordetent, kampvuur (onaangestoken, klik = checkpoint), trainingsveld, klasse-tempeltje, vijver,
  vuurtoren met guh-oortjes (met een geheim kelderluik in de vloer), het ballonschip aan de oostelijke steiger.
- **Zwamzoet**: 9 reuzenzwammen met huisjes op de hoeden, touwbruggen, **wolkenliftjes** (in de schacht staan: omhoog; de
  kolom ernaast: zacht omlaag), markt + plein met guhbeeld, questbord, winkels met inrichting (smidse, veilinghuis, bank,
  uitrusting, kruidenier, kleermaker, havenmeester), de stal aan de westkant, de kade met bootjes.
  Bovenop: de Tempel van De Knuffelorde met de Knuffeldeur. De zijdeur (oostkant) is altijd open.
- **De Haven** (zuid, bij de monding van de baai): luchtschipmast, pakhuis, kraampjes. **Klik op de scheepsbel** -> de
  luchtschip-scène naar Knuffelkamp (en terug met de bel op de kampsteiger).
- **Klodderweide**: vijvers, de slapende reuzenslijm-heuvel (met een tunneltje aan de noordkant), de arena van Koning Klodder.
- **Sporenwoud**: reuzenpaddenstoelen, lichtstralen (deeltjes), een waterval met een kamer erachter.
- **Druipgrotten**: rotsheuvels, 3 grotingangen, grotten met druipsteen; de verborgen kristalkamer via een smalle spleet.
- **De Kloof** met de **drakenfossiel-brug**, het bergpad omhoog en de **Zwamtempel** (deur, voorhal, drie kamers, de zaal met koepel).
- **Uitkijknaalden** (5): loop de wenteltrap op; bovenaan speelt één keer een panorama. Opnieuw: `/guhs vadsrijk wereld uitkijk <naam>`.
- **Grauwe littekens**: grijze stroken met verwelkte bloempjes. `/guhs vadsrijk wereld littekens heel` kleurt ze terug
  (alleen voor jou), `... grijs` maakt ze weer grauw.
- **Lucht**: roze-gouden gloed om de zon, een tweede (roze guh-)maan, 's nachts sterrenbeelden in guhvorm, verre silhouetten aan
  de horizon, en af en toe de grauwe draak ver weg. (`/time set night` om de sterren te zien.)
- **Geluid**: vogels, sporen-tinkels, druppels, "meeuwen", kampvuurgeknetter (alleen vanilla geluiden, geen muziek).

## Deuren
- `/guhs vadsrijk wereld deur ordetempel open` (of `zwamtempel`): de eerste keer een ceremonie-scène, daarna schuiven de deuren open
  en na 30 s weer dicht. `... vergeet` = ceremonie opnieuw zien.

## Het portaal in de Guhmension
- De ingestorte ordetempel is zeldzaam (ongeveer als de kermis), alleen in **nieuwe** Guhmension-chunks.
  Snel vinden: `/locate structure guhs:vr_w_portaaltempel`.
- Het portaal is grijs en slaapt. Erin lopen -> het ontwaakt (scène, ver weg brult de draak) en je gaat naar het Vadsrijk.
- Daarna kun je zelf een portaal bouwen: raam van **Vadsrijk-poortsteen** (8 per recept: roze knuffelsteen, amethist, enderparel),
  aansteken met een **Vadsrijk-vonk** (amethist + roze kleurstof + glowstone + vuursteen). Werkt pas na je eerste bezoek
  (`/guhs vadsrijk wereld vergeetportaal` om dat te testen).
- In het Vadsrijk brengt een portaal (steiger van Knuffelkamp, Zwamzoet ten westen van het plein, De Haven) je terug naar waar je vandaan kwam.

## Tests
- Gametests: `-Pgt=VadsrijkWereld` (tegels laden + steigerplank onder de aankomst, deur schuift open en herstelt, zelfgebouwd portaal).
