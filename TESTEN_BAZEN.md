# Testen: de bazen van het Vadsrijk (Koning Klodder, de Opperzwam, de Mika-spion)

Alle commando's beginnen met `/guhs vadsrijk bazen` en vragen OP-rechten. Ga eerst het Vadsrijk in (`/guhs vadsrijk`) en zet je level
(`/guhs vadsrijk level 20`). Staat de kaart van de wereld-slice er nog niet, bouw dan eerst een noodversie: `bouw koning`,
`bouw tempel` of `bouw mika` (vult alleen lucht, nooit iets van de echte kaart).

## Handig
- `tp koning | tempel | opperzwam | markt | mika` — naar de arena, de tempeldeur, de bazenzaal, de kaaskraam of de eerste hoed.
- `spawn koning_klodder | opperzwam | mika_spion` — een losse baas vóór je (zonder arena; de Mika begint meteen te vechten).
- `fase 2|3|4` — de dichtstbijzijnde baas meteen naar die fase. `split` — Koning Klodder splitst nu.
- `/guhs vadsrijk baas reset bazen:koning_klodder` (of `bazen:opperzwam`) — baas meteen terug, vol.
- `loot koning_klodder|opperzwam|mika_spion` — een buitkist van die baas voor jezelf. `items` — alle benoemde items + trofeeën.
- `herstel` — alle tijdelijke blokwijzigingen terug (plafond, tempeldeur). `tempel reset | open | oplossing`.
- `mika ontmasker` — speelt de onthulling (normaal doet de npc-quest dit). `mika reset` — Akim weer bij zijn kraam.
- `/guhs vadsrijk tips reset` — baas-intro's en tips nog een keer zien.

## Koning Klodder (Klodderweide, wereldbaas)
1. Loop de arena in: de eerste keer een camerazwiep + grote titelkaart "KONING KLODDER – Heerser der Weide", daarna kort.
2. Fase 1: rode cirkel = waar hij landt, rode cirkel om hem heen = schokgolf. Wegwezen.
3. Op 60 % spat hij in drie Kroonklodders (roze, blauw, geel). Vrolijk er één op en kijk naar de aftelling: haal je de andere
   twee binnen 12 s, dan kloddert hij wankel weer in elkaar op 35 %. Te traag: ze smelten terug en hij heelt (probeer beide!).
4. Fase 3: hij springt de lucht in, de hele weide wordt donker op 3 roze cirkels na. Sta in een roze cirkel als hij landt.
5. Verslagen: roze plof, een buitkist per speler die >5 % hielp (alleen jij kunt de jouwe openen), respawnbord "terug over 19:59".

## De Zwamtempel en de Opperzwam
1. Loop naar de tempeldeur (level 16+): scène + titel "DE ZWAMTEMPEL", de deur schuift open (gaat na 5 min weer dicht).
2. Lichtzwamkamer: de 5 zwammen lichten in een volgorde op; rechtsklik ze in dezelfde volgorde. Fout = alles uit en opnieuw tonen.
3. Hendelkamer: zet de 4 hendels zoals de zwammen aan de overkant (spiegelverkeerd!) en stap op de drukplaat.
   (`tempel oplossing` verklapt beide.)
4. Ridderzaal: 3 golven Tempelzwam-ridders (zolang de wezens-slice er nog niet is: grijze proefklodders), dan gaat de poort open.
5. De Opperzwam: roze cirkels = wortels, sporenwolk = ga op de roze plek staan. Op 66 % wordt het donker: alleen zijn ogen,
   mist, en je steekt de 4 Lichtzwammen in de hoeken aan. Op 33 %: de zaal schudt, hij wordt reusachtig en het plafond breekt
   open naar de lucht. Let erop dat het plafond ~10 s na het gevecht weer dicht is (en na een `baas reset` of een herstart).

## De Mika-spion (geheime baas)
1. `tp markt`: "Akim de Kaasguh" staat bij zijn kaasje (roze, met brilletje en neppe snor). Rechtsklik: verdachte praatjes.
2. `mika ontmasker`: de vermomming valt eraf, hij vlucht de zwammenhoeden op; daarna sta je zelf op de hoed.
3. Grijze cirkels = ontvadsings-poeder (Gesporen). Hij poeft rond, roept grijze kloddertjes. Op 70 % en 40 % springt hij naar de
   volgende hoed en schieten de guhs je erachteraan.
4. Op 15 % zweeft hij weg aan een zwarte ballon: schiet of mep de ballon kapot binnen 20 s. Gelukt: knuffelnet, kist, en een
   cliffhanger over de andere Mika's. Mislukt: hij ontsnapt en staat 2 min later weer vermomd op de markt.
5. Eerste keer: gegarandeerd De Neppe Snor van de Spion (Guhddelijk).

## Loot om te proberen (`items`)
Klodderknots (elke 5e klap een roze hulpslijmpje), Plakkerige Pluisboog, Klodderkroontje (glinster boven je hoofd + Vahoeg),
Sporenstaf, Wortelwambuis, Lichtzwamhelm (geen mist bij de Opperzwam), Rookbommetjeslaarzen, De Neppe Snor. Alles uit een
buitkist is "Gebonden". De drie trofeeën zijn voor de deco (economie-slice).

## Bekende gaten
- Geen bazenmuziek (geen audio; haakje staat klaar). Model en items zijn gegenereerd, Blockbench-bestanden in `blockbench/vadsrijk/`.
- Coördinaten zijn voorlopig (zie POI_VERZOEKEN.md); zonder de echte kaart eerst `bouw ...` gebruiken.
