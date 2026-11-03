# Testen: "Een opknapper!" (Guhs 1.9) — het dorp (kern-slice, branch `190-opk-kern`)

De proof of concept is nu een echte dorpseconomie: je hebt voor één kaasknabbel een overwoekerde boerderij gekocht
(bouwkundig rapport: vads) en knapt hem op. De **vormen van de gebouwen zijn nog simpele dozen** (de echte, mooie
vormen komen uit de vormen-slice bij de merge); de wereldkaart werkt nog zoals in de POC.

## Client starten

1. Dubbelklik `run-client.bat` in deze worktree (eigen `run\`-map; de eerste keer duurt het bouwen een paar minuten).
2. **Singleplayer → Create New World**, Game Mode **Creative**, *Allow Commands* aan.
3. Een oude POC-erfenis? Doe eerst `/guhs erfenis reset` (de POC-gebouwen bestaan niet meer).

## De toetsen

| Toets | Wat |
|---|---|
| **O** | Bovenaanzicht van de erfenis waar je op staat. Buiten de Erfenis: *"Naar je erfenis?"* → **Ja, njeg!** |
| **M** | De Wereldkaart |

## Het begin

- **O** → *Ja, njeg!* (of `/guhs erfenis`). Je staat aan de zuidkant van het **open midden** (24x24). Daar staat het
  krakkemikkige schuurtje (de Hoeve, level 1). Alles eromheen is overwoekerd: **72 plukjes** van 8x8 in drie ringen,
  en helemaal buitenom **16 kavels** te koop. Samen 104x104.
- Je krijgt van de makelaar **Ƒ300**, plus **50 planken en 10 eieren** van opa's zolder. Het eerste **Kippenhok is gratis**.

## Het bovenaanzicht

| Wat | Hoe |
|---|---|
| Schuiven / zoomen / draaien | WASD of slepen · scrollwiel · Q/E |
| Selecteren | klik een gebouw, een overwoekerd plukje of een kavel → paneel rechts |
| Bouwen | winkelbalk onderin: tabjes per familie (**groen stipje** = hier kan iets nieuws), klik een kaartje, groen spook = mag, **R** draait, klik = neerzetten |
| Verplaatsen | knop *Verplaatsen* of sleep een geselecteerd gebouw |
| Ophalen | klik het **belletje** boven een gebouw, *Ophalen* in het paneel, **Alles ophalen** links, of loop er in de wereld naartoe |
| Paneel scrollen | scrollwiel boven het paneel |
| Stoppen | rechtermuis / Esc (Esc: eerst het venster, dan de modus, dan de selectie, dan dicht) |

- **Bovenbalk rechts:** Guhlden (in de Hoeve, met maximum), opslag (klik = opslagvenster), bewoners (tooltip: werkplekken,
  volgende intrek), Knusheid, Hoeve-level (klik = de Hoeve en haar doelen), bouwers (tooltip: alle klussen met timers).
- **Links:** Opslag, Naar huis, Alles ophalen, en zodra je ze hebt: Loket en Logés.
- **Paneel van een gebouw:** wat het maakt per uur (met werkers, bonus, vadskracht), het mandje (6 uur, vanaf level 7
  8 uur) met *Ophalen*, keuzes (gewas, recept, kaarskleur, *Blokken kiezen*), wat het doet, de volgende level met alle
  kosten (rood = tekort; hoofdwereld-spul = via het loket) en de tijd, en de knoppen *Verbeteren*, *Verplaatsen*,
  *Draaien*, *Verven...* (T1 t/m je Hoeve-tier, gratis, meteen; *Hele dorp in T#*), plus de eigen knop
  (*Naar het bureau*, *Bestellingen*, *Logés*, *Loket*, *Opslag bekijken*).

## De regels

- **Hoeve-level** bepaalt wat er open is, hoeveel je van iets mag en tot welk level (= het Hoeve-level). De Hoeve zelf
  wil per level Guhlden, goederen (vanaf H7 ook hoofdwereld-spul via het loket) en **doelen**: bewoners, bestellingen,
  plukjes, dierenverblijven, Knusheid, vadskracht, kavels.
- **Bouwers:** 1; de Timmerschuur geeft een 2e (level 1) en een 3e (level 7). Bouwen, verbeteren én opruimen kost een bouwer.
- **Productie loopt door in echte tijd, ook als je weg bent**, tot het mandje vol is. Werkplaatsen pakken hun grondstof
  uit de opslag (graan → meel → brood). Lege werkplek = -25%.
- **Bewoners** trekken elke 15 minuten in zolang er plek is (Guhhutjes; met een Speeltuin 7,5 min) en gaan vanzelf werken.
- **Opruimen** kost Guhlden en een bouwer; je vindt altijd hout of steen, soms een Guhldenzakje, een decostukje
  (decoschuurtje), een zeldzaam zaadje/diertje, of iets voor het verhaal (ansichtkaarten, het oude kistje, de Trekguh,
  het roestige sleuteltje). Ring 2 vanaf Hoeve 3, ring 3 vanaf Hoeve 5.
- **Kavels** kun je pas kopen als alle 72 plukjes weg zijn (Hoeve 6: 2 stuks, … Hoeve 10: alle 16).
- **Naar huis sturen** gaat naar je Leverkist (tot de koppeling-slice erbij is: rechtstreeks in je zakken). Planken,
  baksteen en glas blijven in het dorp; naar huis gaan stammen, klei, zand, glasscherven, witte wol, Bouwkeet-blokken,
  eten en guh-spul. Per uur is er een maximum (spaart 8 uur op); wol heeft geen maximum.
- **Vadskracht** komt pas met de koppeling-slice (Dorpszender). Tot dan: `/guhs erfenis vk <n>` + een Vadsmast.
- **Guhpension:** links al je guhs en waar ze zijn, rechts de logés. Een logé loopt over je erfenis rond en helpt het
  gebouw dat je kiest (+5%, max 2 per gebouw). *Naar huis* = als opgepakte guh in je zakken.

## Testcommando's (op, op je eigen erfenis)

- `/guhs erfenis demo` – een kant-en-klaar dorp (Hoeve 6, alles opgeruimd, ~25 gebouwen, volle mandjes)
- `/guhs erfenis geef <goed|guhlden|alles> <n>` – goederen of Guhlden erbij (tab vult de goed-namen aan)
- `/guhs erfenis hoeve <level>` – de Hoeve meteen op dat level
- `/guhs erfenis tijd <minuten>` – er gaat zoveel tijd voorbij (productie, bouwers, bewoners lopen bij)
- `/guhs erfenis snel aan|uit` – alles meteen klaar (rechtsboven staat dan SNEL)
- `/guhs erfenis bewoners <n>` · `bestellingen <n>` – tellers zetten (bestellingen komen pas met de kaart-slice)
- `/guhs erfenis opruimen alles|<n>` · `kavels <n>` – land vrijmaken
- `/guhs erfenis vk <n>` – extra vadskracht bij je Vadsmast
- `/guhs erfenis reset` · `info` · `bezoek <speler>` · `npc [level] [naam]`
- Kaart: `/guhs erfenis kaart`, `kaart npc groei [n]`, `kaart npc nieuw <n>`, `kaart npc snel aan|uit`

## Wat proberen

1. Begin: Hoeve aanklikken (doelen!), een plukje van ring 1 opruimen, het gratis Kippenhok neerzetten, een Guhhutje,
   een Bosje + Houtzagerij. `/guhs erfenis tijd 30` en kijk hoe alles bijloopt; belletjes ophalen.
2. `/guhs erfenis bestellingen 1` en de Hoeve naar level 2 brengen.
3. `/guhs erfenis demo`: kijk elk paneel na (Kippenhok, Akker met gewassen, Molen met recepten, Bouwblokkenwerkplaats
   → *Blokken kiezen*), *Verven* en *Hele dorp in T#*, het opslagvenster, *Naar huis* (bv. 64 witte wol en 200 stammen:
   de tweede keer zegt hij dat het maximum op is), het Loket.
4. Een Guhpension (in het demodorp staat er een): tem een guh in de hoofdwereld, ga terug, *Logés* → *Logeren*;
   loop over je erfenis: daar loopt hij rond. Kies wat hij helpt, en haal hem weer *Naar huis*.
5. Loop in de wereld naar een gebouw met een vol mandje: je haalt het vanzelf op.
6. Uitloggen terwijl er iets bouwt en produceert, wachten, terugkomen: alles moet bijgelopen zijn.

## Nog niet / bekend wankel

- De gebouwen zijn **stand-in-dozen** (vormen-slice); dieren/bewoners lopen nog niet rond (ook vormen-slice).
- Bestellingen, karretjes, Sherlock, Leverkist, Erfhekje en de Dorpszender komen van de andere slices; hun knoppen
  geven tot de merge een vriendelijke melding.
- De NPC-dorpjes van de kaart gebruiken nog de oude POC-gebouwen (alleen Hoeve/Molen/Opslagschuur komen mee).
- Paden en deco: het datamodel is er (en paden worden al getekend), maar de deco-winkel en de padtools komen in fase 2.
- Gekleurd glas (Glasblazerij L7+) zit er niet in.
- Engelse teksten = dezelfde Nederlandse teksten.
