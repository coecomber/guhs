# Testen: De Erfenis (proof of concept, branch `190-erfenis-poc`)

Dit is een **systeemtest**, geen content. Het gaat om: een eigen basis op een plot, basis-levels, het
**bovenaanzicht** (bouwen, verplaatsen, verbeteren met de muis) en of dat lekker voelt. Gebouwen zijn simpele
blokvormen; namen en vormen zijn werktitels.

## Client starten

1. Dubbelklik `run-client.bat` in `guhs_erfenis\` (eigen `run\`-map, los van je andere worktrees; de eerste
   keer duurt het bouwen een paar minuten).
2. **Singleplayer → Create New World**, Game Mode **Creative**, *Allow Commands* aan. Een gewone nieuwe
   wereld is prima (de Erfenis is een aparte, verborgen dimensie).

## Naar je erfenis

- Druk op **O** (Bovenaanzicht). Buiten de Erfenis vraagt hij: *"Naar je erfenis?"* → **Ja, njeg!**
  (de eerste keer wordt je plot gemaakt). *(Geen item of brief gebouwd: de toets is de weg zonder commando's.)*
- Of: `/guhs erfenis`. Terug naar waar je was: `/guhs erfenis terug` (of de knop *Terug naar de wereld* in het
  bovenaanzicht).
- Je staat op het roze pad aan de zuidkant van je plot (64x64), op een grasiland in de lucht. Opa-guh liet je
  achter: een stadhuis (level 1, met zijn bordje *"Van opa-guh, met liefde. Njeg. <3"*), een Bouwspulmolen,
  zijn oude hekje van 4 muurtjes en een bloesemboompje. Je kunt in de dimensie niks slopen of plaatsen en geen
  schade krijgen; val je eraf, dan sta je weer op het pad.

## Het bovenaanzicht (O op je plot, of `/guhs erfenis bovenaanzicht`)

| Wat | Hoe |
|---|---|
| Schuiven | **WASD** / pijltjes, of **slepen** met links over de grond |
| Zoomen | **scrollwiel** |
| Draaien | **Q / E** (kwartslagen) |
| Selecteren | klik een gebouw (paneel rechts) |
| Verplaatsen | knop *Verplaatsen* en klik waar het heen moet, of **sleep** een geselecteerd gebouw |
| Bouwen | kies onderin in de winkelbalk; groen spook = mag, rood = mag niet; klik = neerzetten; **R** draait het spook |
| Muurtjes | kies *Muurtje* en **sleep een rij**: je blijft in bouwmodus |
| Stoppen | **rechtermuis** of **Esc** (Esc nog een keer = selectie weg, nog een keer = sluiten), of **O** |
| Ophalen | klik het belletje boven de molen, of *Ophalen* in het paneel |

Bovenin: Bouwspul (hoeveel/max), Knusheid, stadhuis-level, de bouwer (vrij of een timer).

## Spelregels in deze test

- **Eén bouwer**: één bouw of verbetering tegelijk ("Bouwer is bezig"). Muurtjes en het boompje staan meteen
  (geen bouwer), maar muurtjes verbeteren kost wel bouwertijd.
- Tijd is echte tijd (loopt door als je weg bent): 10 s per nieuw gebouw, 10-50 s per verbetering.
- Het **stadhuis-level** bepaalt hoeveel je van alles mag en tot welk level ("Ontgrendeld op stadhuis-level X").
  Schuurtje pas vanaf stadhuis 2; tweede molen vanaf 2.
- Materialen per level (stadhuis, muurtjes, en de levels van molen/schuurtje): L1 eik, L2 cobblestone, L3 ijzer,
  L4 goud, L5 diamant; elk level hoger, met torens en een vlag.
- Molen: 6/12/20 Bouwspul per minuut, tot hij vol zit; Bouwspul past tot je opslag vol is (stadhuis + schuurtjes).

## Testcommando's (op)

- `/guhs erfenis bouwspul <n>` – zet je Bouwspul (mag boven je opslag)
- `/guhs erfenis snel aan|uit` – bouwen gaat meteen (rechtsboven staat dan SNEL)
- `/guhs erfenis reset` – je erfenis terug naar opa's begin
- `/guhs erfenis npc [level] [naam]` – maakt een guhdorp (willekeurige gebouwen) en brengt je erheen: open daar het
  bovenaanzicht = **alleen kijken** (geen winkel, geen knoppen)
- `/guhs erfenis bezoek <speler>` – naar iemand anders z'n erfenis (alleen kijken)
- `/guhs erfenis info` – lijst van alle erfenissen

## Wat proberen

1. O → Ja → rondkijken; O → bovenaanzicht: schuiven, zoomen, Q/E.
2. Molen-belletje klikken (+Bouwspul). Stadhuis selecteren → Verbeteren → steiger + timer → klaar (level 2,
   cobblestone).
3. Een schuurtje en een tweede molen bouwen (pas na stadhuis 2), een rij muurtjes slepen, iets verplaatsen
   (ook slepen), draaien.
4. Bouwen op een bezette plek / buiten de rand: rood spook, en een melding.
5. `/guhs erfenis npc 5` → bovenaanzicht van het dorp (alleen kijken).
6. Uitloggen terwijl iets bouwt, wachten, terugkomen: het moet klaar zijn.

## Nog niet / bekend wankel

- **Niet zelf gezien in een echte client**: alles hieronder is gebouwd en getest met gametests, een dev-server en
  (als het lukte) één AutoCheck-screenshotronde; hoe het *voelt* (camerasnelheid, zoom, kleuren, panelen) moet jij
  beoordelen.
- Er is **geen echte animatie**: gebouwen springen in één keer naar hun nieuwe vorm.
- Klikken op gebouwen gebruikt hun buitenste doos: bij hoge gebouwen kun je soms "door" een ander gebouw heen
  klikken op wat erachter staat.
- Verplaatsen draait niet mee (eerst verplaatsen, dan *Draaien*); **R** werkt alleen bij nieuw bouwen.
- Het paneel past krap op een klein scherm (GUI-schaal 4 of een laag venster): de uitleg wordt dan ingekort.
- Geen slopen/verkopen, geen tweede bouwer, geen grondstoffen buiten Bouwspul.
- Geen wereldkaart / bezoeken-menu (fase 2, ander werk); NPC-dorpen groeien nog niet.
- Engelse teksten = dezelfde Nederlandse teksten (POC).
- De dimensie zit in de mod; een server die deze build draait krijgt hem ook (geen wereldgen-veranderingen elders).
