# SPIEK W-BEREIK: kun je overal lopend komen? (ronde W)

Alles hieronder in **survival** testen (`/gamemode survival`), zonder vliegen. Na de teleport loop je het laatste stuk zelf.

## De meldingen van de speler

| Wat | Commando | Wat je moet zien |
|---|---|---|
| **Kootje Zilverstaart: er weer uit** | `/tp @s 2587 167 -520` | Je staat in de Wolkwaldroom (de vechtzaal in de walvis). Naast je, tegen de wand, een **ladder met een open luikje**. Rechtsklik de ladder: "Je klimt de ladder op, terug naar het middelste spuitgat." en je staat bij het spuitgat (2558, 224, -520). Normaal kom je hier via het verhaal (z5_hq08): bij het middelste spuitgat brengt het verhaal je naar de deur. |
| **De walvis: lopend naar de staart (Staartpark, lijn E naar zone 6)** | `/tp @s 2848 233 -523` | Je staat op de Paleisheuvel. Loop naar het oosten: in de borstwering zit een opening met een bordes, dan een **brede witte trap langs de klif** naar beneden (twee stukken, met een tussenbordes), tot op het gras van het Staartpark (2860, 204, -524). Daarna kun je verder naar de Staarthaven (2910, 205, -520) en de staartlift. |
| **Gildemeester Duimdop: door de deur** | `/tp @s 2692 218 -483` | Loop de trap af naar de deur van de Gildehal. Vlak voor de deur is nu een **vlak bordesje** op deurhoogte: je loopt rechtop naar binnen, zonder je hoofd te stoten. Duimdop staat binnen (2702, 215, -484). |
| **Kathedraal: muurtjes boven de trap** | `/tp @s 2692 221 -520` | Loop de grote trap op naar de Grote Poort. Er zweeft **geen wit muurtje** meer 3 blokken boven de treden (dat was een balustrade die in de lucht hing: je liep eronderdoor). |
| **Lantaarns en kettingen op trappen/paden (alle zones)** | `/tp @s 2692 221 -520` en daarna door de stad lopen | Geen lantaarn of losse ketting meer óp een traptrede of midden op een pad, en geen hangende lantaarn op hoofdhoogte. Lantaarns aan muren, palen en hoog aan plafonds blijven. |
| **Saffraanpoort: 33 nepdeuren** | `/tp @s 3646 71 -842` | De deuren zonder kamer erachter zijn nu **ijzeren deuren** (met de hand niet open te krijgen): duidelijk "op slot". De echte huizen hebben nog hun houten deur en een kamer. |

## Andere fixes die de check vond

| Wat | Commando | Wat je moet zien |
|---|---|---|
| Opstijgpier (zone 4): de hemelkapitein | `/tp @s 2500 73 -565` | Loop over de pier naar het oosten het ronde platform op: het witte hekje is **open waar de pier aankomt**. De hemelkapitein staat op het platform (2512, 73, -564). |
| Nestkapel-lift (zone 3): Wasja | `/tp @s 1393 167 -641` | De bovenste liftstop staat niet meer achter het hek van het takdek: je loopt er zo op, rechtsklik = lift naar beneden, naar de kapel en Wasja (1396, 152, -628). Aan de kant van de afgrond staat wel een hekje. |
| Kaaszeppelinwerf (zone 3): trap naar de kajuit | `/tp @s 1683 212 -803` | Loop de toren-trap omhoog naar het dek van Dok 3 en de kajuit van Roquefort (1695, 224, -827): **geen kaas-/betonblokken** meer op hoofdhoogte boven de treden. |
| Galmnaald (zone 2, z2_hq04_naalden) | `/tp @s 891 121 -1019` | Het doel "bij de Galmnaald" telt nu ook vanaf de scherven aan de voet (straal 9 i.p.v. 6: de naald zelf is niet te bereiken). |
| Gondel boven (zone 2) | `/tp @s 886 151 -1132` | De gondelguh staat **in** het bovenstation (884, 151, -1131), niet meer op het dak. |
| Pleinwacht oost (Kandijkom) | `/tp @s 718 91 -1068` | De wacht staat op het plein, niet op een muurtje. |
| Roosvensters kathedraal (z5_s11_knuffellied) | `/tp @s 2619 225 -553` | Het doel ligt nu vóór de balustrade van het transept, bereikbaar (vroeger lag het achter het hekje). |
| Zone 6 geheimen (knuffels/schatten) | `/tp @s 3797 99 -890` | De 8 knuffels en 3 schatten van zone 6 staan nu op hun echte kaartplek (de code zocht een verkeerde naam en gebruikte oude, zwevende plekken). |

## De check zelf
```
python tools/vadsrijk/bereikcheck.py
```
Schrijft `BEREIK_RAPPORT.md`: per zone hoeveel hoofdquest-doelen, zijquest-doelen, NPC's, geheimen enz. je lopend bereikt, en per gemist doel de dichtstbijzijnde plek waar je wél kunt komen.
