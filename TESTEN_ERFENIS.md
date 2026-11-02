# Testen: De Erfenis + de Wereldkaart (proof of concept, branch `190-erfenis-poc`)

Dit is een **systeemtest**, geen content. Twee delen zitten nu samen in één client:

1. **De Erfenis**: een eigen basis op een plot, basis-levels, het **bovenaanzicht** (bouwen, verplaatsen,
   verbeteren met de muis).
2. **De Wereldkaart**: alle erfenissen op één kaart (Grepolis/Tribal Wars-stijl), met **levende NPC-dorpjes** die
   vanzelf groeien, een ranglijst en een nieuwsfeed. Je kunt elk dorpje **bezoeken** en van bovenaf bekijken.

Gebouwen zijn simpele blokvormen; namen en vormen zijn werktitels.

## Client starten

1. Dubbelklik `run-client.bat` in `guhs_erfenis\` (eigen `run\`-map, los van je andere worktrees; de eerste
   keer duurt het bouwen een paar minuten).
2. **Singleplayer → Create New World**, Game Mode **Creative**, *Allow Commands* aan. Een gewone nieuwe
   wereld is prima (de Erfenis is een aparte, verborgen dimensie).

## De toetsen

| Toets | Wat |
|---|---|
| **O** | Bovenaanzicht van de plot waar je op staat. Buiten de Erfenis: *"Naar je erfenis?"* → **Ja, njeg!** |
| **M** | De Wereldkaart (overal, ook buiten de Erfenis) |

(Allebei aan te passen bij Opties → Besturing → Guhs.)

## Naar je erfenis

- **O** buiten de Erfenis → *Ja, njeg!* (de eerste keer wordt je plot gemaakt), of `/guhs erfenis`.
- Terug naar waar je was: `/guhs erfenis terug`, of de knop *Terug naar de wereld* in het bovenaanzicht.
- Je staat op het roze pad aan de zuidkant van je plot (64x64), op een grasiland in de lucht. Opa-guh liet je
  achter: een stadhuis (level 1, met zijn bordje), een Bouwspulmolen, zijn oude hekje van 4 muurtjes en een
  bloesemboompje. Je kunt in de dimensie niks slopen of plaatsen en geen schade krijgen; val je eraf, dan sta je
  weer op het pad.

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

- Bovenin: Bouwspul (hoeveel/max), Knusheid, stadhuis-level, de bouwer (vrij of een timer).
- Linksboven de knoppen: *Sluiten*, *Terug naar de wereld*, **Wereldkaart** (opent de kaart; daar brengt
  *Terug naar bovenaanzicht* je weer hier).
- Bij iemand anders (of een NPC-dorpje) staat er *Erfenis van <naam>* / *Guhdorp <naam>* en **alleen kijken**:
  geen winkel, geen knoppen, wel de knop **Terug naar mijn erfenis** (brengt je naar huis en opent je eigen
  bovenaanzicht).
- De HUD (hotbar, en ook de JourneyMap-minimap in de dev-client) is weg zolang het bovenaanzicht of de kaart open is.

## De Wereldkaart (M, of `/guhs erfenis kaart`, of de knop in het bovenaanzicht)

- Pastel eilandjes met dorp-icoontjes (groter = hoger stadhuis). **Geel** = jij, **blauw** = andere spelers,
  **roze** = NPC-dorpjes. Slepen = schuiven, scrollen of **+ / -** = zoomen.
- Rechts: **Ranglijst** (punten = Knusheid; klik een regel = de kaart vliegt erheen) en **Nieuws**.
- **Naar mijn dorp** (rechtsonder): de kaart vliegt naar jouw erfenis.
- Klik een dorp → paneel linksonder: **Bezoeken** (je wordt erheen gebracht), **Bovenaanzicht** (erheen + meteen
  het bovenaanzicht, alleen kijken), *Aanvallen* (grijs: komt later). Bij je eigen dorp heet Bezoeken *Naar huis*.
- De eerste keer dat iemand de kaart opent, worden er 30-60 NPC-dorpjes gezaaid (zelfde wereld = zelfde kaart).
  Elke ~4 minuten groeien er een paar (nieuw gebouw, verbetering, hoger stadhuis, soms een nieuw dorpje), en dat
  staat in het Nieuws. Ook spelers komen in het nieuws: *"X heeft een erfenis gekregen!"* en als jouw stadhuis
  een level omhoog gaat.
- Elk NPC-dorpje is een **echte erfenis** in de dimensie: bij je eerste bezoek wordt het neergezet, en als het
  groeit terwijl jij er staat, zie je het meteen veranderen.

## Spelregels in deze test

- **Eén bouwer**: één bouw of verbetering tegelijk ("Bouwer is bezig"). Muurtjes en het boompje staan meteen
  (geen bouwer), maar muurtjes verbeteren kost wel bouwertijd.
- Tijd is echte tijd (loopt door als je weg bent): 10 s per nieuw gebouw, 10-50 s per verbetering.
- Het **stadhuis-level** bepaalt hoeveel je van alles mag en tot welk level ("Ontgrendeld op stadhuis-level X").
  Schuurtje pas vanaf stadhuis 2; tweede molen vanaf 2.
- Materialen per level: L1 eik, L2 cobblestone, L3 ijzer, L4 goud, L5 diamant; elk level hoger, met torens en een vlag.
- Molen: 6/12/20 Bouwspul per minuut, tot hij vol zit; Bouwspul past tot je opslag vol is (stadhuis + schuurtjes).

## Testcommando's (op)

Erfenis:
- `/guhs erfenis bouwspul <n>` – zet je Bouwspul (nooit meer dan je opslag; vraag je meer, dan zegt hij dat)
- `/guhs erfenis snel aan|uit` – bouwen gaat meteen (rechtsboven staat dan SNEL)
- `/guhs erfenis reset` – je erfenis terug naar opa's begin
- `/guhs erfenis bezoek <speler>` – naar iemand anders z'n erfenis (alleen kijken)
- `/guhs erfenis npc [level] [naam]` – een los test-guhdorp (staat **niet** op de kaart, zie hieronder)
- `/guhs erfenis info` – lijst van alle erfenissen (ook alle NPC-dorpjes van de kaart)

Kaart:
- `/guhs erfenis kaart` – opent de kaart
- `/guhs erfenis kaart npc groei [n]` – n groei-rondes nu (veel nieuws en groei in één keer)
- `/guhs erfenis kaart npc nieuw <n>` – n nieuwe NPC-dorpjes
- `/guhs erfenis kaart npc snel aan|uit` – groeien elke 15 s in plaats van elke 4 minuten

## Wat proberen

1. O → Ja → rondkijken; O → bovenaanzicht: schuiven, zoomen, Q/E.
2. Molen-belletje klikken (+Bouwspul). Stadhuis selecteren → Verbeteren → steiger + timer → klaar (level 2).
   Kijk daarna op de kaart (M) bij Nieuws: jouw stadhuis staat erin.
3. Een schuurtje en een tweede molen bouwen (na stadhuis 2), een rij muurtjes slepen, iets verplaatsen en draaien.
4. In het bovenaanzicht **Wereldkaart** → een NPC-dorpje kiezen (ranglijst of op de kaart klikken) → **Bezoeken**:
   je staat op het pad van dat dorpje. Druk **O**: alleen kijken. **Terug naar mijn erfenis**.
5. Op de kaart een dorpje → **Bovenaanzicht**: je gaat erheen en na een tel gaat het bovenaanzicht vanzelf open.
6. `/guhs erfenis kaart npc snel aan`, ga in een NPC-dorpje staan en wacht een paar minuten: groeit het onder je
   neus? (Het is toeval wélk dorpje groeit; `/guhs erfenis kaart npc groei 20` helpt.)
7. Uitloggen terwijl iets bouwt, wachten, terugkomen: het moet klaar zijn.

## Nog niet / bekend wankel

- **Niet zelf gezien in een echte client**: alles is getest met gametests en AutoCheck-screenshotrondes (een
  automatische client). Hoe het *voelt* (camerasnelheid, zoom, kleuren, panelen, de kaart) moet jij beoordelen.
- **Niet in een screenshot gezien**: een NPC-dorpje dat groeit terwijl je erop staat (het opnieuw neerzetten
  gebeurt dan in één klap); de nieuwsregel als jouw stadhuis omhoog gaat; twee echte spelers die elkaar bezoeken.
- Een NPC-dorpje dat groeit wordt **helemaal opnieuw neergezet** (geen animatie, gebouwen springen).
- De test-dorpjes van `/guhs erfenis npc` staan niet op de kaart (die zijn van vóór de kaart; de kaart-dorpjes
  komen van `kaart npc`).
- Klikken op gebouwen gebruikt hun buitenste doos: bij hoge gebouwen klik je soms "door" een gebouw heen.
- Verplaatsen draait niet mee (eerst verplaatsen, dan *Draaien*); **R** werkt alleen bij nieuw bouwen.
- Het paneel past krap op een klein scherm (GUI-schaal 4 of een laag venster).
- Geen slopen/verkopen, geen tweede bouwer, geen grondstoffen buiten Bouwspul; *Aanvallen* is nog grijs.
- NPC-dorpjes hebben geen bewoners en doen niks met jou; ze groeien alleen.
- Het verbergen van de JourneyMap-minimap gaat via de HUD-events; als een andere minimap-mod op een andere
  manier tekent, kan die toch blijven staan.
- Engelse teksten = dezelfde Nederlandse teksten (POC).
- De dimensie zit in de mod; een server die deze build draait krijgt hem ook (geen wereldgen-veranderingen elders).
