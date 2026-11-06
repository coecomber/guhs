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
