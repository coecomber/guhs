# Testen: Guhs 1.9 "Een opknapper!" (branch `190-opknapper`)

Alles in één: de brief van de makelaar, het Erfhekje en de Leverkist, je eigen dorp (33 gebouwen, 10 levels, de echte
vormen uit de renders), de makelaarstour tot Hoeve 2, decoschuurtje, bestellingen en karretjes van de buurdorpjes, de
wereldkaart, Handje helpen en het hele Sherlock-verhaal.

## Starten

- Dev-client: `gradlew runClient` vanuit `guhs_opk/w-int` (nieuwe wereld, cheats aan).
- Speel in **survival** als je de echte weg wilt zien: zonder gekochte erfenis zeggen de toets, de wereldkaart (M) en
  `/guhs erfenis` "Je hebt nog geen erfenis". In creative mag je altijd (dev-gemak).

## De hele reis (zo speel je hem)

1. **Brief**: na 30 min speeltijd (vanaf je eerste Guhmension-bezoek) komt de Postguh. Snel: `/guhs erfenis brief`
   (Postguh nu) of `/guhs erfenis brief klok 29`. Lees de brief, **Kopen voor 1 kaasknabbel** → Erfhekje + Leverkist.
2. **Hekje en kist**: zet het Erfhekje en de Leverkist neer (de kist is van wie hem neerzet). Loop door het hekje.
3. **Aankomst**: je staat in je krakkemikkige dorp. Makelaar Guhrrit staat naast je en begint de **makelaarstour**:
   plukje opruimen → gratis Kippenhok → eerste bewoner → eerste bestelling → Hoeve 2 → hij gaat op vakantie (borstbeeld en
   brievenbus in je decoschuurtje). Klik op hem om de huidige stap nog eens te horen. Daarna stuurt hij bij Hoeve 3 t/m 7
   een **ansichtkaartje** van vakantie (de Postguh brengt het; het hangt ook op je Kaartjesbord, +2 Knusheid per kaartje).
4. **Bovenaanzicht** (toets, of `/guhs erfenis bovenaanzicht`): winkel onderin, gebouwpanelen, belletjes om op te halen,
   links Opslag / Naar huis / Alles ophalen / **Deco** / Loket / Logés.
5. **Deco**: knop **Deco (n)** → Neerzetten (spookje volgt je muis, R draait, klik zet neer) of Opbergen. Vondsten,
   bestel-bedankjes, Burenhulp en Sherlock-beloningen komen hier. Een gebouw op een decostuk zet het stuk terug in het schuurtje.
6. **Bestellingen**: paneel van het Marktplein → **Bestellingen**, of rechtsklik op het Marktplein in je dorp, of de
   wereldkaart (toets M / `/guhs erfenis kaart`) → een buurdorpje → Bestellingen.
7. **Sherlock**: zodra je een Detectivebureau bouwt trekt Sherlock in. Ligt er een zaak klaar, dan zweeft er een
   **vergrootglas-wolkje** boven het bureau in het bovenaanzicht (klik = Speurboekje). Rechtsklik op het bureau in je dorp
   werkt ook. Sherlock, Vadson en de anderen staan bij een scène **voor de deur van je bureau** (ben je ver weg of heb je er
   nog geen, dan naast je).
   Beloningen doen nu echt iets: zaak 2 → recept **Opa's theetaartjes** in de Bakkerij, zaak 4 → **Oma Breiguh** bestelt vaak
   zelf (+50% en een extraatje), zaak 5 → **Opa's kelder** onder de Hoeve (luik in de vloer), zaak 6 → **Bertha** zingt op de
   Muziektent, zaak 9 → **Opa Knabbels testament** als boek, rechtszaak → het bureau in zijn **eindlook**.
8. **Naar huis**: venster Naar huis → kies goed + aantal → het komt in je Leverkist in de hoofdwereld (vlaggetje omhoog).
   Glasscherven mogen nu ook mee (thuis smelten tot glas). Het Leverkist-plafond per uur geldt.
9. **Terug**: knop Terug in het bovenaanzicht of `/guhs erfenis terug` → je staat voor het hekje dat je gebruikte.

## Handige dev-commando's (op)

| Commando | Wat |
|---|---|
| `/guhs erfenis demo` | kant-en-klaar dorp (Hoeve 6, ~25 gebouwen) |
| `/guhs erfenis snel aan` / `tijd <min>` | alle klussen meteen klaar / tijd laten verstrijken |
| `/guhs erfenis hoeve <n>`, `geef <goed\|guhlden\|alles> <n>` | Hoeve-level, spullen |
| `/guhs erfenis opruimen alles\|<n>`, `kavels <n>` | land openmaken |
| `/guhs erfenis doe plaats <type> <x> <z> [rot]` | bouwen zoals een klik (ook `verbeter <type>`, `ophalen <type>`, `allesophalen`, `opruimen <plukje>`, `deco <id> <x> <z> [rot]`, `naarhuis <goed> <n>`, `bureau`, `markt`) |
| `/guhs erfenis ui venster deco` | het decoschuurtje openen in het bovenaanzicht |
| `/guhs erfenis brief [status\|reset\|klok <min>]` | de makelaarsbrief |
| `/guhs erfenis leverpost [n]`, `dorpszender`, `decobon` | koppeling-testjes |
| `/guhs erfenis kaart`, `kaart bestellingen`, `kaart bestel snel aan\|vul`, `kaart npc groei` | kaart en bestellingen |
| `/guhs erfenis sherlock moment <id>`, `zaak <id>`, `vind`, `volgende`, `bureau`, `rechtbank` | Sherlock |
| `/guhs erfenis ansicht <4-8>` | een ansichtkaartje van de makelaar nu |
| `/guhs erfenis beloning <vlag>` | een Sherlock-beloning meteen (`recept_theetaartje`, `oma_bestelling`, `opas_kelder`, `bertha_optreden`, `testament`, `bureau_eindlook`) |
| `/guhs erfenis kelder` | naar Opa's kelder (na de beloning) |
| `/guhs erfenis vorm <id> <level> <palet> <rot>`, `vorm alle <level> <palet>`, `vorm decoalle` | alle vormen bekijken |

## Deco, paden en hekjes (decowinkel)

- **Decowinkel**: in de winkelbalk onderin het laatste tabje **Deco** (of klik op het Knusheid-chipje bovenin). Rijtje
  categorieën: Paden, Hekjes, Bloemen, Zitjes, Licht, Water, Beelden, Boerderij, Bijzonder. Per kaartje prijs, slotje + "H5"
  als het pas op een hoger Hoeve-level komt, en een groen "x2" als er al stuks in je schuurtje/voorraad liggen.
- **Een stukje kopen**: klik op het kaartje → spookje aan je muis (R draait) → klik op het gras = betalen en neerzetten
  (het spookje blijft, dus je kunt er meer neerzetten; rechtermuis/Esc stopt). Ligt er al eentje in je schuurtje, dan
  pakt hij eerst die (gratis). **Shift-klik** op het kaartje = kopen voor in je decoschuurtje.
- **Decobon**: heb je bonnen ingeleverd (`/guhs erfenis decobon` geeft er een), dan staat rechts in de categorierij
  "Decobon: n". Aanzetten → je volgende stukje (geen pad/hekje) kost een bon in plaats van Guhlden.
- **Paden en hekjes trekken**: klik op een pad- of hekjeskaartje en **sleep** over het gras. Spookjes: groen = nieuw,
  goud = gratis uit je voorraad, rood = kan niet / geld op, wit = ligt er al. Naast de muis: aantal tegels en de prijs.
  Hoekjes, T-splitsingen en kruisingen gaan vanzelf (ook tussen twee soorten pad). Een ander pad eroverheen wisselt het om.
  Hekjes sluiten vanzelf op elkaar aan. Een hekje mag niet midden op een pad (dan struikelt iedereen).
- **Gum** (rechts in de rij bij Paden/Hekjes): sleep over paden en hekjes om ze weg te halen. Paden gaan in je
  voorraad (staat in het decoschuurtje-venster), hekjes in je schuurtje; later gratis opnieuw trekken. Nooit geld terug.
- **Deco kiezen**: klik op een neergezet stukje → paneel rechts met Verplaatsen / Draaien / Opbergen. Een gekozen stukje
  kun je ook gewoon **slepen**. Opbergen = terug in het schuurtje.
- **Gebouw erover**: een gebouw (of decostuk) neerzetten/verplaatsen/draaien over paden of deco: deco terug in het
  schuurtje, pad-tegels in je voorraad, de paden ernaast krijgen hun nieuwe hoekjes.
- **Knusheid**: chipje bovenin (tooltip: gebouwen / paden / deco en de volgende mijlpaal). Paden 1 per 10 tegels, hekjes 1 per
  5 (Knuffelhaag en vadshekje 1 per 4). Mijlpalen 50, 100, 250, 500, 1.000, 1.500, 2.000, 2.500, 5.000 geven één keer een
  vrolijk berichtje; titels **Knusse Guh** (250), **Knusvads** (1.000), **Knusser Bestaat Niet** (2.500) in je Guhdex.
  Opbergen haalt je titel nooit weg (hoogste stand telt).
- Snel testen: `/guhs erfenis demo`, dan in het bovenaanzicht het Deco-tabje. Dev: `/guhs erfenis doe lijn <soort> x0 z0 x1 z1`
  (trekt een lijn), `doe koop <id> [x z rot]`, `doe gum x,z;x,z`, `doe decodraai x z`, `doe decoverplaats x z nx nz [rot]`.
  AutoCheck: `tools/autocheck/190_deco.txt`.

## Wat zeker het proberen waard is

- (afwerking) Survival zonder brief: M opent de kaart niet. `/guhs erfenis demo`: je staat op een vrij plekje, niet in een
  gebouw. Bestelscherm telt je echte leveringen. Rechtsklik op een Leverkist opent hem.
- (afwerking) `/guhs erfenis beloning opas_kelder`, open het luik in de Hoeve-vloer en klim naar beneden (of `/guhs erfenis kelder`).

- Survival zonder brief: toets/commando weigeren netjes. Dan de brief kopen en door het hekje.
- De makelaarstour van begin tot Hoeve 2 (met `snel aan` gaat het vlot).
- Een plukje opruimen: bij het 1e-3e plukje vind je het roestige sleuteltje (Sherlock-proloog in de chat).
- Deco neerzetten, draaien, opbergen; een gebouw er bovenop zetten (stuk gaat terug naar het schuurtje).
- Een tuintje met de decowinkel: een paadje met een bocht en een kruising slepen, een hekje eromheen, bankje + fontein erin.
- Bestelling leveren: Guhlden erbij, telt voor de Hoeve-doelen, soms een decostuk.
- Detectivebureau bouwen → wolkje → Speurboekje → zaak 1.
- Stammen/wol/glasscherven naar huis en kijken of de Leverkist ze krijgt.
- Met een tweede speler (LAN): elkaars dorp op de kaart, Handje helpen (versnelt de klus die het eerst klaar is, max 25%,
  of als er niks gebouwd wordt een stukje productie).

## Bekende gaten

- Getuigen van een lopende zaak staan nog op hun vaste tegel (niet bij "hun" gebouw).
- Bekende rode tests in de volledige suite die ook op main 1.8.1 rood zijn: zie `NOTITIES_AFWERKING.md`.
- Zie ook `NOTITIES_INTEGRATIE.md` en de slice-notities (`NOTITIES_*.md`).
