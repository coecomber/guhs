# Spiekbrief: de Vadsrijk-website (U-SITE)

## Online (verborgen voorbeeld, staat nergens gelinkt)
- Start: https://guhs.nl/vadsrijk-preview/
- Kaart (alle 5 zones): https://guhs.nl/vadsrijk-preview/kaart.html
- Zone 4: https://guhs.nl/vadsrijk-preview/zones/zone4.html · Zone 5: https://guhs.nl/vadsrijk-preview/zones/zone5.html
- Nieuwe systemen: https://guhs.nl/vadsrijk-preview/systemen/index.html (Ambachten, Uitrusting 2.0, Karakters, Kledingkast, Puzzels)
- Een baas van zone 4: https://guhs.nl/vadsrijk-preview/bazen/zeeslurf.html

**Kijk:**
- bovenaan een gele balk "Voorbeeld van Guhs 1.10.0 ... voorbeelddata";
- de server, spelers, highscores en live-tegels zijn NEP (voorbeelddata), dat staat er ook bij;
- rechtsboven "Toon tot zone 5": zet hem op zone 3 en zone 4/5 verdwijnen (ook van de kaart);
- de kaart: De Lampionkust (eilandjes) en Hoogvads (de walvis) rechts, met kampvuren, bazen, luchtschiplijnen C en D.

## Ronde 2: kijk ook naar
- Bazen: https://guhs.nl/vadsrijk-preview/bazen/index.html — alle zones dicht, klik er een open; elke baas heeft een plaatje.
  Een baaspagina (bijv. bazen/opperzwam.html): onderaan "Alle buit" met kansen (rijdier 10%, zeker na 15 keer).
- Wezens/Items: tabel met bladzijdes (onderaan 1 2 3 ...), klik op een kolomkop = sorteren, filters + zoeken.
- Quests: alle zones dicht, klik er een open.
- Verhaal: https://guhs.nl/vadsrijk-preview/verhaal.html — wereldkaart, tijdlijn, personages, hoofdstukken per zone.
- Kaart: laag "Spelers tonen (live)" rechtsboven; 3 nep-spelers + een luchtschip (voorbeelddata, beweegt niet).
- In het spel (mod): `/guhs vadsrijk kaart verberg` = jij staat niet meer op de kaart, `/guhs vadsrijk kaart toon` = weer wel.

## Lokaal
```
cd C:\Users\Juiced\Documents\MineScapeStory\guhs_vadsrijk\u-site
python tools/make_vadsrijk_site.py
python -m http.server 8000 -d site_build
```
Open http://localhost:8000/vadsrijk/

## Weghalen van guhs.nl
GitHub -> coecomber/guhs -> Actions -> "Wiki (GitHub Pages)" -> Run workflow (main). Daarna is /vadsrijk-preview/ weg (404);
de gewone site blijft precies hetzelfde. (Elke gewone push naar main doet dit ook vanzelf.)
