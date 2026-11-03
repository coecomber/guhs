# Testen: Het Vadsrijk (POC kern, Guhs 1.10.0)

Dit is een gevoelstest van het vechten en de systemen. Alles ziet er nog placeholder uit.

## Starten
1. Dubbelklik `run-client.bat` in deze map (`guhs_vadsrijk\w-poc`).
2. Maak een nieuwe wereld in **Creatief** (cheats aan).
3. Typ `/gamemode survival`, anders word je nooit geraakt.
4. Typ `/guhs vadsrijk`. Je komt in de **Proeftuin** bij het Knuffelkamp.
5. Nog een keer `/guhs vadsrijk` en je bent weer thuis, met je eigen spullen.

## Eerst even uitrusten
- `/guhs vadsrijk loot zeldzaam kussenzwaard` laat een zwaard vallen. Pak het op en houd het vast.
- `/guhs vadsrijk loot episch knuffelharnas` en `/guhs vadsrijk loot guhddelijk pluishelm` geven harnas. Trek het aan in de gewone harnasvakjes.
- `/guhs vadsrijk level 5` zet je meteen op niveau 5.

## Toetsen
De toetsen staan bij Opties > Besturing, categorie **Vadsrijk**, en werken alleen in Het Vadsrijk.

| Toets | Wat |
|---|---|
| Linkermuis | Gewone mep (wacht even tussen meppen: dan krijg je volle kracht en kans op kritiek) |
| R | **Kussenklap**: zwaai in een kegel voor je, mobs vliegen weg |
| V | **Stormsprong**: spring naar wat je aankijkt (of 8 blokken vooruit), schokgolf bij de landing |
| G | **Knuffelschild**: 3 s bijna geen schade, de helft kaatst terug, alle mobs komen naar jou |
| Z | **Vahoege Plof**: een reuzenkussen uit de lucht. Verdooft 3 s en healt je groepje. Laadt op door schade te doen (het percentage staat in het vakje) |

## Wat je kunt proberen
- **Klodders:** loop naar het oosten van het kamp. Daar huppelen grijze proefklodders. Mep ze op en kijk naar:
  - de roze plof en het "opgevrolijkt"-tellertje links van de hotbar;
  - de xp-balk;
  - buit met een lichtstraal in de kleur van de zeldzaamheid. Alleen jij kunt die oppakken.
- **Elementen:** de letter op het naamplaatje is het element van de mob (W = water, N = natuur). Dit merk je eraan:
  - Een Pluishamer (vuur) doet weinig tegen W en veel tegen N. Klodders in het westen zijn N.
  - Roze is sterk tegen alles wat grijs ("ontvadst") is.
  - Gekleurde schadegetallen betekenen een zwakte; grijze getallen betekenen dat het element zwak is tegen die mob.
- **Kampvuur:** klik het kampvuur in het kamp aan. Het gaat branden en wordt je checkpoint.
- **Flauwvallen:** ga bij de Klodderreus staan tot je neergaat. Dan gebeurt dit:
  - je wordt wakker bij de arenadeur (of bij het kampvuur);
  - je verliest niks;
  - je bent 30 s duizelig.
- **De Grauwe Klodderreus:** loop vanaf het kamp naar het noorden de ronde arena in.
  - De eerste keer krijg je een korte camera-intro met de titelkaart. Daarna alleen de titelkaart.
  - **Fase 1:** hij springt. De rode cirkel is waar hij landt.
  - **Fase 2 (50%):** hij spuugt kloddertjes. Gebruik R op het groepje!
  - **Fase 3 (20%):** hij rolt rondjes. Ontwijk de rode strook.
  - **Einde:** grote roze plof en een eigen buitkist (klik erop). Op het bord bij de deur zie je wanneer hij terugkomt (60 s).
- **Eigen inventaris:**
  1. Neem buiten van alles mee (harnas, een vol inventaris, iets in je cursor).
  2. Ga erin: je hebt een lege Vadsrijk-rugzak.
  3. Ga eruit: alles is terug, precies op zijn plek.
  4. Ga weer naar binnen: je Vadsrijk-spullen zijn ook terug.
  5. Probeer het ook met uitloggen terwijl je binnen bent.
- **Party:** met twee spelers doe je `/guhs vadsrijk party uitnodigen <naam>` en daarna `/guhs vadsrijk party accepteer`. Dan:
  - zie je links elkaars Vads;
  - krijg je gedeelde xp;
  - heeft de baas meer HP;
  - kun je elkaar niet raken.

## Dev-commando's
| Commando | Wat |
|---|---|
| `/guhs vadsrijk` | erin / eruit |
| `/guhs vadsrijk proeftuin` | naar de proeftuin |
| `/guhs vadsrijk level <1-20>` / `xp <n>` / `munten <n>` | niveau, xp, Vadsmunten |
| `/guhs vadsrijk loot <zeldzaamheid> [type] [niveau]` | buit laten vallen (types: kussenzwaard, knuffelknots, pluishamer, pluishelm, knuffelharnas) |
| `/guhs vadsrijk vol` | Vads, energie en ult vol, cooldowns weg |
| `/guhs vadsrijk baas reset` | Klodderreus meteen terug |
| `/guhs vadsrijk tips reset` | tooltips en baas-intro opnieuw zien |
| `/guhs vadsrijk kist` | spullen die de wissel niet kwijt kon, terugkrijgen |
| `/guhs vadsrijk info` | wat staat er opgeslagen |

## Wat is placeholder
- **Uiterlijk:**
  - De wapens en het harnas gebruiken vanilla-plaatjes (gouden zwaard, knots, bijl, gouden helm en borstplaat).
  - Klodders en de reus zijn het guh-slijmmodel: grijs als ze ontvadst zijn, roze na de plof.
  - Het kussen van Z is een roze wolblok.
- **Effecten:** vanilla deeltjes en geluiden. Er is geen baasmuziek.
- **De wereld:** alleen de Proeftuin (vlakte, vijver, 3 tentjes, kampvuur, arena). De echte kaart, Knuffelkamp-eiland en Zwamzoet komen van andere slices.
- **Mobs:** alleen proefklodders. De echte Klodder, Bubbelklodder en de rest komen van de wezens-slice.
- **De HUD-balken en het naamplaatje** zijn simpele vormen en tekst.

## Waar je op moet letten
- Voelt het meppen en de vaardigheden goed (snelheid, afkoeltijden, energie)?
- Is de baas te makkelijk of te moeilijk op niveau 1, 3 en 5?
- Lees je de HUD makkelijk: Vads, energie, skills, baasbalk?
- Hoe voelen de buit en de lichtstralen?
