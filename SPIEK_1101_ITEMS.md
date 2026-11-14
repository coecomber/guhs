# SPIEK 1101-ITEMS: oude spullen opruimen (ontmantelen + verkopen)

Testen in de dev-client, in het Nederlands. Alles speelt in Het Vadsrijk.

## Voorbereiden
- Pak een paar oude spullen en wat beters:
  - `/guhs vadsrijk items geef kussenzwaard gewoon 1` (oud), `... ongewoon 1`, `... zeldzaam 1`;
  - `/guhs vadsrijk items geef kussenzwaard episch 20` (iets goeds voor in je hand);
  - of een paar grijze stukken uit de Uitrusting-winkel van Meester Pluisnaald (Lv 1).
- Houd een goed wapen in je hand en draag een helm.

## 1. De tooltip
Beweeg je muis boven een Vadsrijk-stuk in je inventaris. Je ziet:
- "▼ Slechter dan wat je draagt (-12 Schade per klap)" of "▲ Beter dan wat je draagt (...)", met het grootste verschil tussen haakjes;
- "Winkelwaarde: ..." (goud);
- "Ontmantelen: 2x Smeedstofje";
- onderaan "L: op slot zetten".

Druk op **L** boven het stuk. Je krijgt de melding "... zit nu op slot." en de tooltip toont "🔒 Op slot: ...". Nog een keer L haalt het slot er weer af.

## 2. Het Opruimen-scherm (knop ⚒)
- Open je inventaris. Links staat onder de andere knopjes de **⚒**. Klik erop.
- Per stuk zie je:
  - het oordeel (beter/slechter/leeg vak/in je hand);
  - de winkelwaarde;
  - het materiaal dat je krijgt;
  - een slotje en "Ontmantel".
- Ontmantel een grijs stuk. Je krijgt meteen Smeedstofjes, onderaan telt "Smeedmateriaal" mee.
- Ontmantel een blauw stuk. Eerst komt er een venster "... ontmantelen?" met Ja/Nee. Nee doet niks, Ja ontmantelt.
- Het wapen in je hand en een stuk op slot hebben een grijze Ontmantel-knop: die doet niks.
- "Ontmantel alle slechtere (n)" bovenaan:
  - eerst komt een venster met de aantallen (grijs · groen · blauw of beter) en wat je krijgt;
  - Ja ontmantelt alleen die stukken. Stukken op slot, met sterren of beter dan wat je draagt blijven staan.

## 3. Bij de smid (Zwamzoet)
- In de smidse staat linksonder de knop "Opruimen (n slechtere)". Die opent "Opruimen bij de smid".
- Hier kun je ook **verkopen** en zie je onderaan **Omsmelten**:
  - 3 stofjes → Verbetersteentje
  - 1 splinter → Verbetersteentje
  - 1 splinter → Pluisdraad
  - 2 kristallen → Glinstergruis
  - 3 kristallen → Herrolknabbel
- Klik op ✓ en het materiaal wordt omgesmolten. Heb je te weinig, dan is de knop grijs.
- "Terug naar de smid" brengt je terug naar de sterren.

## 4. In een winkel
- Open het tabblad Verkopen. Naast "Verkoop alle grijze" staan nu ook "Verkoop alle slechtere (n)" en "Ontmantel alle slechtere (n)". Allebei vragen ze eerst met de aantallen.
- Bij een stuk op slot staat "🔒 Op slot", bij je handwapen "In je hand": die kun je niet verkopen.
- "Verkoop alle grijze" verkoopt je handwapen en stukken op slot niet meer.
- Een stuk op slot kan ook niet op de markt en niet in de ruil.

## 5. De tutorial (nieuw personage, of `/guhs vadsrijk questlopen`)
- Na "De elementendriehoek" stuurt Opa Sterrenstof je naar **Sergeant Pluizebol** op de steiger.
- De sergeant geeft "Opruimen" en legt het in 3 regels uit. Je krijgt 2 oude oefenwapens. De tooltip zegt "Slechter dan wat je draagt" als je je questwapen vasthoudt.
- Klik op Kok Knapperkorst en kies **Kampwinkeltje**. Verkoop één oud wapen: stap 1 is klaar.
- Klik op de sergeant en kies **Kampsmidse (ontmantelen)**. Ontmantel het andere: stap 2 is klaar.
- Lever in bij de kok. Daarna geeft hij meteen "Een warm plekje" (kampvuur).
- In je questlog (J) staat bij Gids "Wat doe ik met oude spullen?".
- Een bestaand personage dat al verder is, ziet geen nieuwe quest bij de sergeant.

## Robot
- `/guhs vadsrijk questrobot zone 1`: hq03c_opruimen en hq04_kampvuur moeten "gehaald" zijn.
- `/guhs vadsrijk questrobot alles`: alle zones moeten nog net zo goed gaan als voorheen.
