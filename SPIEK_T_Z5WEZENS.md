## Zone 5 "Hoogvads": gevechten (T-Z5WEZENS)

De stad zelf is veilig. Je vecht op vier afgesloten plekken: de **Proefarena van de Wolkwacht** (bij de Academie), de **Buikgangen** (in de walvis, via een luik in de kelder van de Soepkeuken), de **Wolkrand** (de vinnen en de staartvin, via wolkenliften) en het geheime **Schuilnest** (onder de Luchthaven, achter een losse plank).

> Alle plekken zijn nog **placeholders** tot t-z5wereld de stad bouwt. Staat er nog geen stad/vloer? Typ eerst:
> `/guhs vadsrijk z5wezens bouw alles`. Dat bouwt de Buikgangen, het Schuilnest en witte stand-in-vloeren. Het zet alleen blokken in lucht, dus de stad wordt nooit overschreven.

### 0. Snel overzicht
- `/guhs vadsrijk z5wezens alle` zet alle 9 wezens op een rij (zonder AI, de vermomde ook zichtbaar).
  - Kijk naar: de bezeten stropop met grijs pluis, de ridder-guh met kussenzwaard en roze pluim, de Mika's (saboteur met moersleutel, lontlegger met kaasbommen, harpoenier), de grijze pluismijt, het wolkduiveltje met hoorntjes, de mistsluier en de Mika in Wolkwacht-kostuum.
- `/guhs vadsrijk z5wezens plekken`: alle plek-ids met coördinaten.
- `/guhs vadsrijk z5wezens spawners`: aantallen per plek (max ~50 levend in heel zone 5).
- `/guhs vadsrijk z5wezens opruimen`: haalt alle zone-5-wezens binnen 64 blokken weg.

### 1. De Proefarena (lv 56-57)
- `/guhs vadsrijk z5wezens tp proefarena`
  - **Bezeten Oefenpop**: staat stil als een gewone pop (geen naambordje). Loop binnen 5 blokken: "De oefenpop beweegt!".
    - Rode cirkel = hij draait rond: stap weg.
    - Verslagen: grijs pluis waait eruit.
  - Leider **Opperpop Strobaal** (elite, gouden gloed).
- **Sparren met een ridder-guh** (doet NOOIT pijn):
  - Typ `/guhs vadsrijk z5wezens sparren`, of rechtsklik een Sparringridder in de arena.
  - Rode strook = kussenzwaai: stap eruit. Drie "Kussenklap!" en je verliest de ronde. Je Vads blijven vol.
  - Roze glinster bij zijn schild = hij blokt: je klappen doen dan niets ("Geblokt!").
  - Sla zijn balk naar 5%: hij buigt, "Goed gevochten!". Je krijgt xp, een Proefpenning-teller, de eerste keer zeker een Wolkwacht-stuk, daarna 30% kans.
  - Klappen van iemand anders dan je sparpartner doen niets.

### 2. De Buikgangen + de kerker (lv 57-59)
- **Luik**: de kelder van de Soepkeuken heeft een luik (rechtsklik: "naar de Buikgangen"). Snel: `/guhs vadsrijk z5wezens tp buikgangen`.
- **De kamers**: de Ingangshal, dan de Ribbengang (botten-bogen), de Kleppenzaal (koperen pijpen) en een gang naar de ronde Ketelkamer.
  - Wezens: Mika-saboteur (rode cirkel = gassleutel, grijze wolk = plakkerig), Mika-lontlegger (blijft op afstand, kaasbom = rode cirkel), Grauwe Pluismijten (in groepjes).
  - Terug naar boven: het luik bij de ingang (rechtsklik).
- **Kerkerbaas Opzichter Gorgonzola** in de Ketelkamer: `/guhs vadsrijk z5wezens baas gorgonzola` (reset + naar de deur).
  - 4 warme-luchtkleppen (roze wiel = open, grijs = dicht).
  - Om de ~25 s paarse lijn = hij draait een klep dicht. Elke dichte klep: hij neemt 15% minder schade, en iedereen krijgt elke 3 s een beetje kou.
  - **Rechtsklik een dichte klep 3x** om hem open te draaien.
  - Verder: moersleutelzwaai (rode cirkel om hem), kaasgasbom (rode cirkel op iemand), buikschuiver (rode strook).
  - Op 60% fluit hij 2 saboteurs. Op 25% gaan ALLE kleppen dicht en zakt hij niet onder 15% tot er weer minstens drie open zijn.
  - Snel testen: `/guhs vadsrijk z5wezens mechaniek kleppen` (sluit alle kleppen van de dichtstbijzijnde Gorgonzola).

### 3. De Wolkrand (lv 58-60)
- **Wolkenliften** (witte geglazuurde blokken, rechtsklik) aan de stadsrand. Snel: `tp zuidvin`, `tp noordvin` of `tp staartvlakte`.
- Wezens:
  - Grauwe Wolkduivel: vliegt, en rode streep = duik.
  - Mistslier: grijze cirkel = mist (blind + plakkerig).
  - Mika-harpoenier: rode lijn = harpoen. Die trekt je NAAR hem toe, nooit naar de rand.
  - Leider **Moeder Mist** (grote mistcirkel).
- **Vangnet**: val je van een vin of de staart, dan vangt een wolkje je op ("Een wolkje vangt je op!") en sta je weer bij de lift.
- **Wereldbaas Donderbuik** (een dikke grauwe onweerswolk) op de staartvin: `/guhs vadsrijk z5wezens baas donderbuik`.
  - Bliksem: een rode cirkel onder iedereen.
  - Regenlaan: een brede rode strook (plakkerig).
  - **Zuigwind**: drie ROZE wolkenkussens verschijnen. Sta erop, anders zuigt hij je naar zijn buik (35%). Testen: `mechaniek zuig`.
  - 60%: 3 wolkduiveltjes + Donderslag (gele cirkel: sta er samen in).
  - Aan het eind wordt hij ROZE, giechelt en drijft weg, met roze regen. Huisdier: **Donderbuikje**.
  - Respawn 20 min.

### 4. Het Schuilnest (lv 58-60)
- Een losse plank achter de kratten op de Luchthaven (rechtsklik). Snel: `tp schuilnest`.
- **Vermomde Mika's** staan er als Wolkwachters (geen naambordje). Kom binnen 4 blokken: "Ontmaskerd!", het kostuum ploft eraf.
  - Rode cirkel achter je = daar verschijnt hij. Stap weg.
- Leider **Meester-Vermommer Feta**.

### 5. De spion: Kootje Zilverstaart (de finale van "Verraad in de stad")
- Het verhaal start hem (t-z5verhaal: `Z5Finale.start` → onze `Z5Bazen.start(speler, "spion")`). Testen: `/guhs vadsrijk z5wezens baas zilverstaart` (zet je bij de Wolkwaldroom en roept hem op).
- Fase 1: speldsprong (rode strook), rookbom (grijze cirkel: blind), daarna duikt hij op achter wie het verst staat.
- Fase 2 (70%), **De Vermommingen**: vijf Wolkwachters in een kring, één is hem.
  - Zoek de ZILVEREN glinster bij de staart.
  - Raak je een lokpop: "Een lokpop!" en dat kost jou 10%.
  - Raak je hem: ontmaskerd en +50% schade, 8 s lang.
  - Niet gevonden binnen 25 s: hij springt eruit (25% op iedereen).
  - Testen: `mechaniek vermom`.
- Fase 3 (35%), **De ontsnapping**: drie grauwgasventielen bij het spuitgat. Rechtsklik elk 3x. Tot ze alle drie dicht zijn zakt hij niet onder 10%. Testen: `mechaniek ontsnap`.
- Verslagen: hij vlucht in rook. Het verhaal gaat door naar de Grauwe Droom (die fase speelt de verhaal-scène van t-z5verhaal).

### 6. Buit (lv 56-60)
- `/guhs vadsrijk z5wezens set`: de **Wolkwachtset** + sieraden.
  - Met de nieuwe slots van t-items zijn het 6 stukken: helm, kuras, beenstukken, laarzen, handschoenen, cape.
  - Sieraden: Wolkring, Walvisketting, Regendruppeloorbel, Stormarmband.
  - Setbonussen in de tooltip:
    - (2) Verdediging +10 en Vads +60;
    - (4) Schade tegen bazen +8%;
    - (6) Wolkkussen: een schild onder 30% Vads.
- `/guhs vadsrijk z5wezens wapens`: de 6 klassewapens + de 7 benoemde baasstukken + 3 trofeeën.
  - Klassewapens: Wolkwachtzwaard, Walvisbotknots, Stormboog, Harpoenkruisboog, Wolkenstaf, Bliksemstaf.
  - Baasstukken: Hoedenspeld, Mantel der Vermommingen, Moersleutel, Kleppenstaf, Donderboog, Onweerstaf, en het Guhddelijke Regenjasje.
- `/guhs vadsrijk z5wezens loot <wezen|baas> [level] [elite]`: rolt een tabel (een baas = zijn kist; de eerste keer zeker een Legendarisch + trofee).
- **Huisdieren** (eerste kill): Donderbuikje (Donderbuik), Warmpje (Gorgonzola), Lokpopje (Zilverstaart).
