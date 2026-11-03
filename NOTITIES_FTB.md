# Notities: FTB-slice (Guhs 1.9 "Een opknapper!", branch 190-opk-ftb)

Het FTB Quests-hoofdstuk **"Een opknapper!"** (sub "Bouwkundig rapport: vads"), chapter-id `guhs_erfenis`, in de groep
Guhs (na Guhdromenland). De 76-quest-versie van `erfenis-voorstellen/ftb.md` (ronde 15) + de Sherlock-zaken (ronde 15:
"Sherlock-zaken komen gewoon mee"): **87 quests + "Hoe kom je hier?"**, 11 secties, één FTB_SLOT-plaatje aan het eind
("Mika-strooptochten: binnenkort!", met Morris-Mika). **Niks op slot** (geen deps).

## Bestanden
- `tools/features/erfenis_ftb.py` — de quests (`FTB_CHAPTER`, `FTB_SECTIES`, `FTB_SLOT`, `ftb(fq)`), de 54 verborgen
  advancements `guhs:quest/erfenis_*` en de namen van de 11 statistieken (`stat.guhs.erfenis_*`, NL = EN).
  Los draaien: `PYTHONPATH="tools;tools/features" python tools/features/erfenis_ftb.py`.
- `tools/make_ftbquests.py` — `CHAPTERS["guhs_erfenis"]` (titel, banner, welkom, intro) en `CHAPTER_VERSION = 56`.
- Gegenereerd: `src/main/resources/ftbquests/{chapters,lang/*}/guhs_erfenis.json5`, `index.txt`, plaatjes in
  `textures/ftbquests/guhs_erfenis/` + `icon_guhs_erfenis.png`, `tools/ftbquests_art.json`, `tools/lang/source_ftb.json`
  (alle andere hoofdstukken: alleen `guhs_chapter_version: 56`).
- Java `feature/erfenis/ftb/`: `ErfenisFtbFeature` (statistieken-register, luisteraars, payload `Geopend`),
  `ErfenisFtb` (de logica + eigen SavedData `guhs_erfenis_ftb`), `client/ErfenisFtbClient`, `ErfenisFtbGameTests`
  (batch `erfenis_ftb`, 3 tests).
- Gedeelde bestanden, één regel per plek: `Features.java` (register + payloads), `FeaturesClient.java` (init),
  `tools/features/__init__.py` (`'erfenis_ftb'`).

## Hoe de quests afvinken (ErfenisFtb.kijk)
Elke seconde voor online spelers, bij inloggen, en meteen bij elk `ErfenisMomenten`-moment. Advancements blijven af,
statistieken gaan alleen omhoog (FTB `stat`-taak toont "n / doel").
- **Gebouwd** (`Base.besteLevel >= 1`): kippenhok, timmer, schaap, konijn, eend, koe, bij, mammoet, akker, boomgaard,
  groeve, speeltuin, cafe, feest (Muziektent), detective; bosje = Bosje + Houtzagerij; glas = Zandkuil + Glasblazerij.
- **Statistieken**: plukjes (`opgeruimdTotaal`), bewoners, hoeve (level), kavels, knusheid, bestellingen
  (max van `Base.bestellingen` en kaart's `BestelData.geleverd`), burenstempels, ansichtkaarten (vondsten `ansichtkaart*`),
  planken/baksteen (gemaakt), naar_huis (bouwspul).
- **Gemaakt** (molen, brood, koek, stof, kaas, kaars, reuzenwol, planken 500, baksteen 200): elke stijging van opslag +
  gebouwbuffers van dat goed sinds de vorige blik (de eerste blik is alleen een nulpunt; wat er al lag telt niet).
  Molen = meel of rozemeel; koek = koek/wortelcake/knabbelkoek; stof = stof/knuffelstof; kaars = kaars/lantaarn.
- **Naar huis**: het Leverkist-tegoed van de gecapte goederen zakt alleen bij versturen (`Base.naarHuis`), dus
  (verwacht tegoed - echt tegoed) = wat er weg ging. Bouwpakket telt stammen, klei, zand, glasscherven en bouwblokken.
  "Naar huis sturen" (eerste zending) gaat ook af zodra er post op een Leverkist wacht (`LeverkistPost.wacht > 0`),
  dus ook voor wol/eten.
- **Kaart**: ster-bestelling / hoger dorp = een geleverd karretje dat terugrijdt (`fase TERUG`, niet geweigerd; ster
  of `relatie > 0`); handje = `stempels > 0` of iemand geholpen.
- **Sherlock**: `SherlockData.als(..).isOpgelost(zaak)` → `erfenis_zaak_<id>`, de rechtszaak → `erfenis_bezwaar`.
- **Overige**: koop = `Makelaarsbrief.gekocht`; bezoek = op het perceel van een ander dorp staan (`Erfenis.onder`);
  loket = iets ingeleverd; gekleurde wol = een `*_wool` (niet wit) in het loket; vadsmast = `vkInDorp >= 40`;
  pension = een logé; deco = iets geplaatst; fontein = `spuitguhfontein` geplaatst; vondst = een `deco_*`-vondst of de
  trekguh; bedankje = een bestelling-deco (mika_schrik, knabbelbessenstruik, de_eerste_knabbel, kaassausfonteintje,
  mammoetje_van_heg) in opslag of geplaatst; wolkenschaap = Schaapskooi 7 + Hoeve 7 (zoals Productie);
  kleurglas = een Kaarsenmakerij met een kleur; werkplaats = een andere keuze dan alleen de roze pastelbaksteen.
- **Overzicht / wereldkaart**: de client meldt het openen van `OverzichtScherm` / `KaartScherm` (payload `Geopend`).
- Item-taken: makelaarsbrief, erfhekje, leverkist, dorpszender. Dimensie-taak: `guhs:erfenis`.

## Keuzes waar de documenten stil waren
- **Sherlock**: één quest per zaak (11; de finale heet "BEZWAAR, NJEG!" zoals in ftb.md) in plaats van de 4
  plaatsvervangers. Totaal dus 87 i.p.v. 76. Beloningen klein (de zaken geven zelf al Guhlden): K / GZ / KR, finale GZ groot + DB.
- **"Een kleurtje erbij"** (gekleurd glas met vadskracht) bestaat niet in kern: het werd **kaarsjes in een kleur** in
  de Kaarsenmakerij (die draait op vadskracht). Icoon roze kaars.
- **"Groetjes van de makelaar"**: kern heeft 3 ansichtkaarten (vondsten), niet 8 → doel **3**. Komen er later meer
  (post van Guhrrit), dan alleen het getal in `erfenis_ftb.py` ophogen.
- **"Dorpsfeest!"**: er is (nog) geen apart feestje-moment → de quest gaat af bij de **Muziektent** (dekt zo ook dat gebouw).
- **"Wolkjes op de wei"**: Schaapskooi **level 7** (+ Hoeve 7), want daar maakt Productie de wolkenschaapjes (ftb.md zei L4 op de oude schaal).
- **Timmerschuur**: alleen "Twee handjes extra" (76-versie); de tekst noemt de 3e bouwer op level 7.
- **Opslagschuur**: geen eigen quest (76-versie), genoemd in de tekst van Hoeve 3 (zoals ftb.md voorstelt).
- **Kleur** van het lint: `lime` (boerderijgroen). Banner: makelaar Guhrrit + Sherlock; welkom: Guhrrit, Sherlock, Vadson, een guh.

## Afhankelijk van andere slices (werkt vanzelf zodra die het doen)
- `ErfenisDeco.geef` is in deze tree nog een **STUB** (doet niks) → "Een bedankje van de buren" gaat pas af als kern de
  bestelling-deco echt in `decoOpslag` legt.
- `erfenis_fontein` verwacht deco-id `spuitguhfontein` in `Base.deco` (zo heet de vorm in vormen/deco).
- Geen eigen stubs gemaakt.

## Testen in de dev-client
1. FTB Quests geïnstalleerd: het hoofdstuk "Een opknapper!" staat in de groep Guhs (na Guhdromenland).
2. `/guhs erfenis brief klok 29` (na een Guhmensie-bezoek) → brief → kopen → "Post voor guh!", "Eén knabbel...", hekje.
3. Door het hekje: "Bouwkundig rapport: vads". Druk **O** → "Van bovenaf". Druk **M** → "Op de kaart".
4. Ruim een plukje op, bouw een kippenhok, enz.: de quests vinken binnen een seconde af. Snel: `/guhs erfenis ...`
   dev-commando's van kern (hoeve-level, snel) en `/guhs erfenis sherlock volgende` voor de zaken.
5. Statistieken: Esc → Statistieken → Algemeen ("Plukjes opgeruimd", "Level van je Hoeve", ...).

## Stand
- Generator + `check_ftbquests_assets.py` (0 problemen) gedraaid; plaatjes bekeken (titel, welkom, koppen, slot).
- **Let op**: de basis van deze branch (190-opknapper b25d6e1dc) compileert zelf NIET (`ErfenisHulp` gebruikt de oude
  `Base.bouw`; de integratie-commit 5ad444a74 in w-int lost dat op). Om te testen heb ik die `ErfenisHulp.java` tijdelijk
  overgenomen en daarna weer teruggezet; hij zit niet in mijn commit.
- Daarmee: `compileJava` groen; gametests `-Pgt=ErfenisFtbGameTests,ftbquests,TaalsleutelsGameTests,KoppelingGameTests,SherlockGameTests`
  allemaal groen op één na: `ftbQuestsChapterInstallsOnceAndMergesTheTexts` miste in zijn vaste lijst al `guhs_dromen`
  (bestond al vóór mij); ik heb `guhs_dromen` + `guhs_erfenis` aan die lijst toegevoegd (1 regel in `GuhGameTests.java`),
  daarna `ErfenisFtbGameTests` + `ftbquests` (+ `ftbquests_compleet`): 8/8 groen.
