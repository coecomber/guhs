# Notities slice KOPPELING (Guhs 1.9 "Een opknapper!")

Branch `190-opk-koppeling`. Java: `feature/erfenis/koppeling/**` (+ de API-gevels `feature/erfenis/LeverkistPost.java` en
`feature/erfenis/ErfenisVadskracht.java`). Resources: `tools/features/erfenis_koppeling.py` (standalone te draaien).
AutoCheck: `tools/autocheck/190_koppeling.txt`. Gametests: `KoppelingGameTests` (batch `erfenis_koppeling`).

## Wat er is

- **De brief** (`guhs:makelaarsbrief`, `Makelaarsbrief`): klok in SPEELTIJD (alleen online geteld, opgeslagen in
  `KoppelingData`), start bij het eerste bezoek aan de Guhmension (`Bezocht.dimensie`). Na 30 min verschijnt de Postguh
  (een echte `GuhNpcEntity` Kind POSTGUH, geen AI, onkwetsbaar, 5 s, puf + hartjes) voor je neus en geeft de brief.
  Niet tijdens een cutscene, niet op iets rijdend, niet in de erfenis-dimensie zelf. Lezen = briefscherm (crème papier,
  briefhoofd "Makelaardij Guhrrit & Zn.", stempel "BOUWKUNDIG RAPPORT: VADS"). Knop "Kopen voor 1 kaasknabbel" →
  1 kaasknabbel weg, je krijgt het **Erfhekje + een Leverkist**, moment `brief_gelezen` gaat af.
  Daarna leest de brief nog steeds (stempel "VERKOCHT!") met de knop "Extra Erfhekje (1 kaasknabbel)".
- **Erfhekje** (`guhs:erfhekje`): 2 blokken hoog (als een deur). Roze palen met hartje, knuffelsteen-gezicht op de balk,
  roze-hooibaal-oortjes, roze tulp bovenop, en een zacht gloeiende geanimeerde doorgang (eigen textuur, light_emission 15,
  lichtniveau 11/9) met roze/lila stofjes, hartjes en end-rod-vonkjes. Doorlopen (of rechtsklik) → `Erfenis.gaNaar(p,
  eigenBase)`. Terug (`/guhs erfenis terug` of de knop in het dorp) zet je 1,2 blok VOOR het hekje dat je gebruikte, aan
  de kant waar je vandaan kwam, met je rug naar het hekje (geschreven in kern's `ErfenisData.zetTerug`, plus eigen
  `KoppelingData.Speler.hekje`). Meer hekjes mag; het laatst gebruikte telt. 3 s rust na een reis.
- **Leverkist** (`guhs:leverkist`): schatkistje met roze deksel, goudrand, brievengleuf en een brievenbusvlaggetje dat
  omhoog staat zolang er iets in zit (blockstate `post`). 27 vakjes, gewone kistscherm, trechters/buizen kunnen eruit
  halen (en erin stoppen). `LeverkistPost.verstuur(server, eigenaar, spul)` → wachtrij per eigenaar (opgeslagen), wordt
  bezorgd zodra een kist van die eigenaar geladen is; meerdere kisten vullen op plaatsvolgorde; wat niet past wacht.
  Eigenaar = wie hem plaatst (een kist van /setblock wordt van wie hem het eerst openklikt). Melding in de actiebalk bij post.
- **Leverkist-plafond** (`LeverkistPlafond`, balans 9.4): per materiaal max het plafond per uur, spaart tot 8 uur, begint
  vol; wol en niet-bouwmateriaal (eten, guh-spul) vrij. `beperk(server, eigenaar, lijst, nuMillis)` → (mee, blijft) en
  `ruimte(...)` voor de UI. Soorten: stammen 150, planken 600, steen_ruw (steen/keien/klei/grind) 900, steen_bewerkt 500,
  zand 900, glas (glas + glasscherven) 180, bouwkeet (de Bouwkeet-blokken) 500, kaasknabbels 80.
- **Dorpszender** (`guhs:dorpszender`): guh-machine (roze + goud, antenne, huisje op het luikje). Recept (vormloos):
  Vadszender + 4 guh_kristal + 1 block_of_kaasknabbels. Staat op het vadskrachtnet als gewone verbruiker en vraagt wat
  hij zendt: rechtsklik hoger, sluipen + rechtsklik lager (10/20/40/60/80/100/140/200 VK, start 40). Als zijn net draait
  komt er 75% aan: `ErfenisVadskracht.beschikbaar(server, eigenaar)` = som van alle draaiende zenders van die eigenaar
  na verlies (kern kapt af met de Vadsmast).
- **Items**: `guhs:glasscherf` (smelt in een oven → 1 glas), `guhs:guhldenzakje_klein` (100), `guhs:guhldenzakje` (500),
  `guhs:guhldenzakje_groot` (2000): rechtsklik → `ErfenisEconomie.geef(.., "guhlden", n)`, sluipen = hele stapel;
  `guhs:decobon`: rechtsklik → +1 Decobon op je dorp (`Dorpsbeloningen.decobonnen/neemDecobon/geefDecobon` voor kern's
  decowinkel). Zakjes en bonnen werken pas als je de opknapper hebt gekocht (anders blijven ze in je zak).
- **Wiki**: blokuitleg voor erfhekje, leverkist, dorpszender (`tools/wiki_site/blokuitleg.py`, tussen markers).

## API voor de andere slices

- `erfenis.LeverkistPost.verstuur(MinecraftServer, UUID, List<ItemStack>)` (+ `wacht(...)`): de plafonds doet de AANROEPER
  (contract): gebruik `koppeling.LeverkistPlafond.beperk(server, eigenaar, spul, System.currentTimeMillis())` en stuur
  alleen `mee()`; `blijft()` blijft in de Opslagschuur.
- `erfenis.ErfenisVadskracht.beschikbaar(MinecraftServer, UUID)`.
- `koppeling.Makelaarsbrief.gekocht(MinecraftServer, UUID)`: heeft de speler zijn erfenis gekocht? (Advies voor kern: de
  bovenaanzicht-toets buiten de dimensie en `/guhs erfenis` pas toestaan als dit true is, of in creative.)
- `koppeling.Dorpsbeloningen.decobonnen / neemDecobon / geefDecobon`.
- `koppeling.ErfhekjeBlock.laatste(server, speler)`: het laatst gebruikte hekje (GlobalPos).

## Stubs (STUB-kern, vervangen bij de merge)

- `feature/erfenis/ErfenisMomenten.java`: `melding(server, uuid, moment)` + `luister(BiConsumer)` (minimaal werkend).
- `feature/erfenis/ErfenisEconomie.java`: `geef/heeft/neem` doen niks.
- `ErfenisVadskracht.java` en `LeverkistPost.java` zijn ECHT (eigenaar koppeling); als kern ook een `ErfenisVadskracht`
  maakte: houd de mijne of laat kern's versie `Dorpszender.beschikbaar` aanroepen.

## Keuzes waar de documenten stil waren

- De brief-klok telt alleen speeltijd waarin je online bent; spelers die de Guhmension al eerder bezochten beginnen te
  tellen vanaf deze update. Brief kwijt vóór het kopen: na 20 min extra speeltijd brengt de Postguh een nieuwe.
- `brief_gelezen` gaat af bij het KOPEN (niet al bij het openen), want dan begint het verhaal echt.
- Bij de opknapper zit ook één Leverkist (het hekje zonder kist voelt half). Extra Leverkisten: recept
  ` K ` / `WCW` / `PPP` (K kaasknabbel, W roze wol, C kist, P planken). Het Erfhekje heeft GEEN recept: alleen via de brief.
- Erfhekje: creative-spelers mogen er altijd door (dev-gemak); anderen pas na het kopen. Het hekje van een ander brengt je
  naar JOUW erfenis.
- Glasscherf: 1 scherf smelt tot 1 glas (half-product; thuis in een oven/Guhoven), geen craftrecept.
- Dorpszender: ook een onbelaste (base niet geladen) zender telt mee zoals hij er de laatste keer bij stond; anders werkt
  het dorp nooit terwijl je erin staat (dan is je basis juist niet geladen).
- Leverkist-plafond: tegoed op echte tijd (millis), begint vol, slaat op per speler.
- Leverkist in creative/setblock zonder eigenaar: eerste rechtsklik claimt hem.

## Testen in de dev-client (commando's, op)

- `/guhs erfenis brief` → de Postguh komt nu met de brief. `/guhs erfenis brief status | reset | klok <minuten>`
  (`klok 29` = brief binnen een minuut, na een eerste Guhmension-bezoek).
- Brief lezen (rechtsklik), kopen met een kaasknabbel in je zak → Erfhekje + Leverkist. Hekje neerzetten, erdoor lopen,
  `/guhs erfenis terug` → je staat voor het hekje.
- `/guhs erfenis leverpost [n]` stuurt testpakketjes (knabbels, wol, stammen) naar je Leverkisten; `/guhs erfenis leverpost info`.
- Dorpszender: `/give @s guhs:dorpszender`, op een net met een Guhrad (of `/setblock ~ ~ ~ guhs:vadskracht_testbron[kracht=10]`
  ernaast met Guhdraad), rechtsklik om te zetten, `/guhs erfenis dorpszender` = wat er in je dorp aankomt.
- `/guhs erfenis decobon` = je Decobonnen.

## Stand (klaar)

- compileJava groen; gametests `-Pgt=KoppelingGameTests,ErfenisGameTests`: 13/13 groen (6 van koppeling).
- AutoCheck `tools/autocheck/190_koppeling.txt` bekeken: blokken in de wereld (hekje voor/zij/doorgang, Leverkist vlag
  neer/op, Dorpszender slaapt/werkt), Postguh met de brief, briefscherm (aanbod en VERKOCHT, tekst past; layout van de
  onderkant en de knop "Extra hekje: 1 knabbel" na de check verbeterd), kopen geeft hekje + Leverkist, door het hekje
  naar de erfenis en terug voor het hekje.
