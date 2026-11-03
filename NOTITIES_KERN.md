# Notities kern-slice: Guhs 1.9 "Een opknapper!" (branch `190-opk-kern`)

De POC-economie (Bouwspul, stadhuis, molen, muurtjes) is vervangen door de echte dorpseconomie uit
`ontwerp-guhs190-erfenis.md` + `erfenis-voorstellen/balans.md`, met de overrides uit `CONTRACT_OPKNAPPER.md`
(10 levels per gebouw, halffabricaten).

## Wat er werkt

- **33 gebouwen** (`GebouwType`, contract-ids): maat (b x d, draait mee), familie, opent op Hoeve-level, aantal per
  Hoeve-level (balans 2.3), max level = Hoeve-level (Mammoetwei: 3 levels, H9 → 2, H10 → 3).
- **Kosten** (`Prijzen`): Guhlden per klasse A/B/C + eigen tabellen (Timmerschuur, Marktplein, Detectivebureau, Café,
  Muziektent, Loket, Vadsmast, Kaarsenmakerij, Mammoetwei), goederen per level (x0,5 klein / x1,5 groot), bouwtijden
  (klein/middel/groot), hoofdwereld-spullen via het loket, de Hoeve-tabel 2.1 met doelen, opruimen en kavels (balans 6).
- **Goederen** (`Goed`, 42 contract-ids) in één gedeelde opslag (Hoeve 150 + Opslagschuren), Guhlden in de Hoeve met cap
  per Hoeve-level. API voor de andere slices: `ErfenisEconomie.geef/heeft/neem` (id `guhlden` voor geld).
- **Productie in echte tijd, ook offline** (`Productie` + `Base.bijwerken`): van gebeurtenis naar gebeurtenis (bouw klaar,
  bewoner trekt in), per stuk constante snelheid. Mandje 6 uur (L1-6) / 8 uur (L7-10), daarna stopt het gebouw.
  Bijproducten (veren, mest, eendeneieren, was, fruit, klei, pluis van wolkenschaapjes vanaf H7 + L7).
  Tweestapsketens: werkplaatsen nemen hun grondstof uit de opslag terwijl ze werken (geen grondstof = wachten).
- **Werkers:** bewoners automatisch ingedeeld (eerst iedereen 1, dan 2...), lege werkplek -25%, nooit onder 25%.
  Werkplekken: dieren/akkers 1 (2 vanaf L7), werkplaatsen/bronnen 1 / 2 (L5) / 3 (L9).
- **Bewoners:** Guhhutje 2..9, Speeltuin +1..+12 en twee keer zo snel; elke 15 min (7,5) één erbij zolang er plek is.
- **Bonussen:** Café + Muziektent +5/10/15% (L1-4/5-8/9-10), logé-guhs +5% (max 2 per gebouw).
- **Vadskracht:** `ErfenisVadskracht` (koppeling zet de bron; standaard 0), gecapt door de Vadsmast (40 → 140 over 10
  levels). Zonder genoeg VK werken Houtzagerij/Steenbakkerij/Glasblazerij/Bouwblokkenwerkplaats als L6, Mammoetwei en
  Kaarsenmakerij slapen. Toewijzing: in gebouw-id-volgorde (wie eerst gebouwd is, eerst).
- **Bouwers 1 → 3** (Timmerschuur L1 / L7), voor gebouwen, verbeteringen én plukjes opruimen. In het bovenaanzicht een
  timer boven elke klus en de lijst in de bouwers-tooltip.
- **Het perceel is 104x104** (`Plot`): open midden 24x24, 72 plukjes van 8x8 in 3 ringen (16/24/32), 16 kavels rond het
  geheel (4 hoeken 16x16, 12 zijstukken 24x16); eiland 136 breed. Bouwen kan alleen op open grond.
- **Opruimen:** ring 1 vanaf H1 (25 Ƒ, 2 min), ring 2 vanaf H3 (150 Ƒ, 10 min), ring 3 vanaf H5 (500 Ƒ, 30 min); altijd
  hout/steen(/klei/zand), 30% Guhldenzakje, 10/15/20% deco-vondst, 5% zeldzaam zaadje/diertje (ring 2+), vaste
  vondsten op vaste plukjes (ansichtkaarten 1-3, het oude kistje, de Trekguh), en het roestige sleuteltje (proloog
  Sherlock) bij de eerste bijzondere vondst, uiterlijk bij het 3e plukje.
- **Kavels:** pas als alle 72 plukjes weg zijn; kavel n = n x 600 Ƒ + n x 20 planken; max per Hoeve 2/4/8/12/16 (H6-H10).
- **Keuzes per gebouw:** gewas (Akker), recept (verwerking; kaasknabbels vanaf Kaasmakerij L3), kaarskleur (17 kleuren),
  Bouwkeet-blokken (venster met de 5 sets, meerdere = willekeurige mix).
- **Verven:** knop in het paneel, palet T1 t/m de bereikte Hoeve-tier, gratis en meteen opnieuw gestempeld, plus "Hele
  dorp in dit palet". Niet geverfd = het palet volgt de vorm-tier van het eigen level.
- **Loket:** venster met wat de volgende verbeteringen uit de hoofdwereld vragen, je zakken, en Inleveren.
- **Naar huis sturen:** venster, kies goed + aantal → `LeverkistPost.verstuur`. Halffabricaten: alleen stammen, klei, zand,
  glasscherven als bouwgrondstof; wol en Bouwkeet-blokken kant-en-klaar; eten/guh-spul ook. Leverkist-plafond per uur
  (spaart 8 uur): stammen 150, klei 900, zand 900, glasscherven 180, Bouwkeet-blokken 500, kaasknabbels 80; wol vrij.
- **Guhpension:** venster zoals de Knabbelvelden (links al je guhs en waar ze zijn, rechts de logés met de limiet);
  de guh wordt bewaard als data (`GuhOpslag`, "logeert in het Guhpension van ..."), helpt een gebouw naar keuze (+5%),
  en zolang er iemand op het perceel is loopt er een stand-in met zijn looks en naam rond (nooit opgeslagen).
  Naar huis = als opgepakte guh in je zakken.
- **Knusheid:** gebouwen 2 per level (Hoeve 10), paden 1 per 10 tegels, deco via `Knusheid.zet(base, n)`. Datamodel staat
  er al: `Base.deco` (geplaatste stukken), `Base.decoOpslag` (decoschuurtje), `Base.paden` (laag per tegel) +
  `padSoorten`; Stempel tekent paden met de verbindingen (N1 E2 S4 W8) via `GebouwVormen.pad`.
- **Momenten** (`ErfenisMomenten`): alle contract-ids worden afgevuurd (`aangekomen`, `plukje_opgeruimd:n`, `vondst:id`,
  `gebouw_gebouwd:id`, `gebouw_level:id:lvl`, `hoeve_level:n`, `bestelling_geleverd:n`, `bewoners:n`, `knusheid:n`);
  is de speler offline, dan wachten ze tot de volgende login. `brief_gelezen` vuurt de koppeling-slice zelf
  (`ErfenisMomenten.melding` is publiek).
- **Bovenaanzicht:** winkelbalk met tabjes per familie (groen stipje = hier kun je iets nieuws bouwen), slotjes, prijs;
  zijpaneel per gebouw (scrollt); bovenbalk met Guhlden, opslag, bewoners, Knusheid, Hoeve, bouwers; links Opslag, Naar
  huis, Alles ophalen, Loket, Logés; belletjes boven gebouwen met iets klaar (klik = ophalen). Ophalen kan ook door in
  de wereld naast/in een gebouw te lopen.

## Stubs (de lead vervangt ze bij de merge)

- `vorm/GebouwVormen.java` — `// STUB-vormen`: simpele doos in het palet, bloemetjes in de rest van de maat, plukjes en
  paden. `minHoek` = de hoek van de voetafdruk OP het gras (y = Plot.Y); gebouwen staan vanaf `minHoek.above()`.
  Level 0 = bouwplaats (steigers). Bij een lopende verbetering zet de kern zelf steigers op de hoeken.
- `LeverkistPost.java` — `// STUB-koppeling`: stopt de spullen meteen in je zakken.
- `sherlock/Sherlock.java` — `// STUB-sherlock`: `openBureau` geeft een melding.
- `kaart/Bestellingen.java` — `// STUB-kaart`: `open` geeft een melding.

## Voor de andere slices

- **kaart:** na een levering `ErfenisEconomie.bestellingGeleverd(server, eigenaar)` aanroepen (telt voor de Hoeve-doelen
  en vuurt `bestelling_geleverd:<totaal>`). Aantal karretjes: `ErfenisEconomie.marktKarren(server, eigenaar)` (3..7).
  De NPC-plannen van de kaart gebruiken nog de POC-ids; `GebouwType.van` vertaalt `stadhuis` → hoeve en `schuurtje` →
  opslagschuur, muur/boompje vallen weg. NPC-bases zijn helemaal open grond. De kaart-test `zelfdeRegelsAlsDeKern`
  vergelijkt nog met de POC-getallen en faalt dus tegen deze kern tot de kaart-slice is bijgewerkt.
- **koppeling:** `ErfenisVadskracht.zet((server, eigenaar) -> vk)`; de kern capt op de Vadsmast. `guhs:glasscherf`:
  zolang dat item niet bestaat, weigert "Naar huis" glasscherven netjes ("bestaat nog niet").
- **sherlock:** luister met `ErfenisMomenten.luister`. Vondst-ids: `roestig_sleuteltje`, `ansichtkaart_1..3`, `oud_kistje`,
  `trekguh`, `deco_<weerhaan|strooguh|melkkar|knabbeltrommel>`, `zaadje_*`, `dier_*`, `guhldenzakje` (zie `Base.opgeruimd`).
- **vormen:** `GebouwVormen.vormTier` bepaalt het standaardpalet van een nieuw/verbeterd gebouw (de stub volgt de tabel
  "Level → vorm" uit RENDERS.md).

## Beslissingen waar de documenten stil waren

- **Startpakket:** 300 Ƒ (balans) + 50 planken en 10 eieren "van opa's zolder" (anders kun je niks bouwen: elk gebouw
  vraagt planken en de Houtzagerij heeft zelf planken nodig). Het eerste Kippenhok is gratis (balans 3.1).
- **De Hoeve bewaart ook 150 goederen** (opa's oude schuurtje), zodat er vóór de eerste Opslagschuur iets op te halen valt.
- **Opslag = één gedeelde ruimte** voor alle goederen (Hay Day-silo), niet per soort.
- **Levels 3-level-gebouwen naar 10:** Detectivebureau/Café/Muziektent/Vadsmast/Loket: stappen op L1, L5, L9 (daar
  verandert ook het effect), lineair ertussen, L10 = 1,3 x L9. Timmerschuur: 500 (L1, 2e bouwer) … 12.000 + 8 vadsstaven
  (L7, 3e bouwer) … 22.000 (L10). Kaarsenmakerij L1 = de volle 4.000 (balans), want L1 vraagt al hoofdwereld-spul.
- **Bewoners per hutje per level:** 2,3,3,4,4,5,6,7,8,9; Speeltuin 1,1,2,2,3,4,6,7,10,12; Pension 2,2,3,3,3,4,5,6,7,8 —
  zo dat elk Hoeve-doel (bewoners) haalbaar is op het level ervoor (gecontroleerd voor H2..H10).
- **Opslagschuur:** 210, 300, 450, 600, 900, 1.200, 2.100, 3.000, 4.500, 6.000 per schuur.
- **Akkergewassen:** graan 40-180/u; roze graan, wortels, bloemen 24-108/u; pluisbolletjes 16-72/u.
- **Recepten:** Houtzagerij 1 stam → 4 planken (of 1 balk); Steenbakkerij baksteen van klei óf steen, gladde steen;
  Glasblazerij glas óf glasscherven (voor thuis); Bouwblokkenwerkplaats 1 plank → 1 Bouwkeet-blok; Weverij ook
  1 reuzenwol → 16 wol (dan rekent de capaciteit in reuzenwol per uur, anders kan de Mammoetwei nooit op).
  Lantaarn: halve snelheid t.o.v. kaarsen. Gekleurd glas (balans L4+) zit er niet in: er is geen goed-id voor.
- **Hoeve 7 vraagt ook een gebouwd Loket** (anders kan het hoofdwereld-spul nergens heen).
- **Hoofdwereld-spullen bij gewone gebouwen:** Houtzagerij/Steenbakkerij/Glasblazerij L9 (half) en L10 (balans L5),
  Bouwblokkenwerkplaats L9/L10, Timmerschuur L7, Kaarsenmakerij L1, Vadsmast L1/L5/L9, Mammoetwei elk level
  (`guhs:fossiel_mammoet`). "Gekleurde wol (4 kleuren)" = roze, lichtblauw, geel, lime (x16); 8 kleuren + paars, oranje,
  cyaan, rood; "pastelbakstenen" = `guhs:pastelbaksteen_roze`. Items die niet bestaan worden overgeslagen.
- **Verbeteren tijdens productie:** een gebouw dat verbeterd wordt blijft op zijn oude level doorwerken (lief, geen verlies).
- **Kaarsen naar huis** krijgen de kleur die de (eerste) Kaarsenmakerij nu heeft.
- **Bouwkeet-blokken naar huis:** de opslag houdt per bloksoort bij wat er gemaakt is; upgrades pakken eerst de grootste stapel.
- **Leverkist-tegoed** begint vol (8 uur) bij een nieuwe erfenis.
- **Guhldenzakje boven de cap** gaat verloren tot de cap (Hoeve vol = vol); de melding zegt hoeveel erbij kwam.
- **Logé-stand-ins** lopen over het hele perceel (thuis = midden, straal 52), beginnen bij het gebouw dat ze helpen.
- **Aankomst:** in het open midden aan de zuidkant (niet meer op het buitenpad: dat zou door de bosjes lopen zijn); staat
  daar een gebouw, dan land je erbovenop.
- **Ophalen door te lopen:** naast of in een gebouw met iets klaar staan = ophalen (hooguit eens per 4 s).

## Testen in de dev-client (commando's)

Alle dev-commando's werken op je eigen erfenis (`/guhs erfenis ...`, op):

| Commando | Wat |
|---|---|
| `demo` | kant-en-klaar dorp: Hoeve 6, alles opgeruimd, ~25 gebouwen, bewoners, geld, volle mandjes |
| `geef <goed\|guhlden\|alles> <n>` | goederen/Guhlden erbij (tot de opslag/cap vol is) |
| `hoeve <level>` | Hoeve meteen op dat level |
| `tijd <minuten>` | er gaat zoveel tijd voorbij (productie, bouwers, bewoners) |
| `snel aan\|uit` | alle klussen meteen klaar |
| `bewoners <n>`, `bestellingen <n>` | tellers zetten (bestellingen = Hoeve-doelen zonder de kaart-slice) |
| `opruimen alles\|<n>`, `kavels <n>` | land openmaken |
| `vk <n>` | extra vadskracht bij de Vadsmast |
| `reset`, `info`, `npc [level] [naam]` | zoals in de POC |
| `ui <wat> [arg]` | (AutoCheck) iets kiezen in het open bovenaanzicht: `kies <gebouw-id>`, `plukje <n>`, `kavel <k>`, `tab <familie>`, `verven`, `venster opslag\|naarhuis\|loket\|bouwkeet\|pension`, `camera x,z,afstand`, `dicht` |

Gametests: `gradlew runGameTestServer -Pgt=ErfenisGameTests` (via `gradle_slot.sh`). AutoCheck:
`gradlew runAutocheckClient -PautocheckScript=tools/autocheck/190_opknapper_kern.txt`.

## Stand bij oplevering

- compileJava groen; `runGameTestServer -Pgt=ErfenisGameTests`: alle 15 groen. (`KaartGameTests.zelfdeRegelsAlsDeKern`
  faalt verwacht tot de kaart-slice naar de nieuwe GebouwType-getallen is omgezet.)
- AutoCheck `tools/autocheck/190_opknapper_kern.txt` is één keer gedraaid (opk_01..15 bekeken: start, plukje, winkel,
  demodorp, panelen kippenhok/Hoeve/molen/werkplaats, vensters opslag/naar huis/loket/bouwkeet/pension: alles netjes).
  Daarna gerepareerd zonder nieuwe client-run (regel gebruiker: geen client): `ui camera x,z,afstand` faalde (argument
  met komma's → nu greedyString), demo zette 24 bewoners bij plek voor 18 (nu begrensd), uitleg in het paneel 6 regels.
  **Nog te doen met AutoCheck zodra het weer mag:** het script opnieuw draaien en opk_04 (camera) + opk_05..09 bekijken.
- Itemnamen in het loket staan in de client-taal (Engels in AutoCheck); dat zijn de bestaande item-vertalingen.

## STAND BIJ PAUZE 2 (WIP-commit "WIP 190-opk: pauze 2")

- Laatste fixes (camera-argument, demo-bewoners, 6 uitlegregels) zitten erin; gametests daarna opnieuw: alles groen
  (14:22, "All 20 required tests passed").
- **Resterend na resume:** alleen nog de AutoCheck-herhaling (zodra clients weer mogen) en de eindcommit + rapport.
