# Testen: economie van het Vadsrijk (munten, winkels, smid, markt, ruilen, rijdieren, cosmetica)

De NPC's (smidguh, winkelguhs, marktmeester, stalmeester) komen van de npc-slice. Tot die er zijn open je alles met dev-commando's
(op nodig). Ga eerst het Vadsrijk in: `/guhs vadsrijk`.

## Snel starten
- `/guhs vadsrijk munten 50000` (5 goud) en `/guhs vadsrijk eco spul` (steentjes, herrolknabbels, pluisdraad, drankjes, munten).
- `/guhs vadsrijk loot episch kussenzwaard 8` voor iets om op te smeden.
- `/guhs vadsrijk eco ga smid|drankjes|uitrusting|markt|cosmetica|stal` brengt je naar de plek uit de kaart.

## Wat proberen
1. **Munten**: rechtsklik een munt-item (sluipen: alle munten). Bij elke verandering springt rechts boven je hotbar een muntje met "+12b".
2. **Winkels**: `/guhs vadsrijk eco winkel drankjes` (ook `uitrusting`, `smidwaren`, `cosmetica`, `algemeen`).
   - Kopen (x1 en x5), bij uitrusting eerst een level kiezen (Lv 1/5/10/15).
   - Tab Verkopen: verkoop loot, of "Verkoop alle grijze".
   - Drink een drankje (Vads, energie, Vahoeg, Snelpootjes).
   - Is het scherm roze en goed leesbaar?
3. **Smid**: `/guhs vadsrijk eco smid`. Kies een stuk links.
   - "Ster erbij!": hamer op het aambeeld, dan GELUKT met fanfare, of een roze plof.
   - Klik een bonus aan (hartje = vast) en kies Herrollen.
   - Snel naar +6: houd het stuk vast en doe `/guhs vadsrijk eco ster 6`. Probeer tot het verpluisd raakt (of `/guhs vadsrijk eco verpluis`), dan Repareren.
   - Het stuk gaat nooit weg; sterren en bonussen blijven.
4. **Markt**: `/guhs vadsrijk eco markt`.
   - Tab Verkopen: klik iets, typ een prijs ("2z 50b") en zet het te koop.
   - Tab Kopen: zoek en filter (slot, klasse, zeldzaamheid, level). Je eigen spullen staan erbij met "Intrekken".
   - Met een tweede account: koop iets, en haal bij de verkoper de opbrengst op (dat werkt ook als de verkoper offline was; bij het inloggen krijg je een berichtje).
5. **Ruilen** (2 accounts): sluip en rechtsklik de ander met lege hand, of `/guhs vadsrijk eco handel <naam>`. De ander klikt [Ja, ruilen!].
   - Leg spullen en munten erin, allebei Klaar, allebei Ruilen!.
   - Verander je iets, dan moeten jullie allebei opnieuw. Gebonden spullen zijn grijs.
6. **Rijdieren**: `/guhs vadsrijk eco rijdier hoedhupper` (zoals de quest: rijdier + rijfluitje). Blaas op het fluitje en je zit erop.
   - Spatie = grote hup. Laat je raken door een klodder: hij schrikt en poef. Afstappen: poef.
   - `/guhs vadsrijk eco rijdierxp 2000` voor levels.
   - `/guhs vadsrijk eco rijdier klodderros` (of het zadel-item), en dan `/guhs vadsrijk eco stal` om te kiezen.
7. **Cosmetica**: `/guhs vadsrijk eco winkel cosmetica`. Koop kleding, een beeldje of het vaandel, en een titel.
   - Ga terug naar huis (`/guhs vadsrijk`): het pakketje komt in je gewone inventaris.
   - Trek de kleding aan bij je guh; zet het beeldje neer; kies de titel in de Guhdex.

## Gametests
`runGameTestServer -Pgt=VadsrijkEconomie` (4 tests):
- sterren: nooit permanent verlies, verpluisd is altijd te repareren;
- markt: kopen en verkopen gaan helemaal of helemaal niet;
- ruilen: tweezijdig bevestigen, met hercontrole;
- rijdier-xp.
