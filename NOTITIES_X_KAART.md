# NOTITIES X-KAART (ronde X, 2026-11-09): kaartgeneratoren

Branch `110-x-kaart` from b9ef85f4c. Alleen generator-/toolbronnen gecommit; de gegenereerde tegels, `*WereldPlekken.java` en
`POI_*.md` zijn lokaal geregenereerd om te verifiëren en daarna teruggezet. **Integratie regenereert zone 2, 5 en 6**
(zone 4 vóór zone 5 als zone 4 ook moet). Spiekbrief: `SPIEK_X_KAART.md`.

## 1. De walvistrap (zone 5, `zone5/bouw5.py` `staarttrap()`, herschreven)

**Wat er mis was:** de ingang van w-bereik zat in een tunnel recht achter de troon, op 1 blok achter het troonpodium.

**Nieuw ontwerp** (alles in spelcoördinaten in de code, via `G()` naar de kit omgezet):
- **De poort:** 3 breed en 4 hoog door de achterwand van de troonzaal, aan het eind van het noordelijke zijpad (z -532..-530).
  - Gelijk met de vloer.
  - Een gouden lijst aan beide kanten van de muur.
  - Aan de binnenkant een bord "Naar het / Staartpark / & de / Staarthaven".
- **Het pad:** een lichtblauwe loper van de roze hoofdloper (x 2829) naar het zijpad en dan rechtdoor naar de poort.
- **Het bordes** (x 2855..2857, y 232):
  - een balustrade aan de noord- en oostkant, met een lantaarn op de hoek;
  - een gouden boog boven de trapkop, met hetzelfde bord en een lantaarn op de boogpost.
- **Trap 1:** 14 treden naar het zuiden (3 breed).
- **Tussenbordes:** z -515..-513, y 217.
- **Trap 2:** 13 treden terug naar het noorden, uit op het gras bij z -529/-530. Er staat een lantaarnpaal naast de uitgang.
- **Onder en naast de trap:**
  - massief onder elke trede en elk bordes (een `fundament()` die ook door bladeren en planten heen vult);
  - een balustrade op de scheidingswand en aan de buitenkant;
  - lantaarns alleen op leuningen.
- **POI's** `staarttrap_boven` = 2856,233,-531 en `staarttrap_onder` = 2860,204,-530 (de oude tunnel achter de troon is dicht).

**Borden met tekst in de tegels (nieuw):**
- `bord_nbt()` maakt de block-entity-nbt van een gewaxt bord, met 26.1-tekstcomponenten: `translate` plus een Nederlandse `fallback`.
- `maak5.schrijf()` geeft die nbt door aan `nbtsnel` (`info["bordjes"]`). Verbind/vrijpad en kaartpak houden block-entity-nbt al heel.
- **NIET in een client gezien.** Als de borden leeg blijken: het nbt-formaat (`messages` als lijst van compounds) is de verdachte.
- Taalsleutels `vadsrijk.z5.bord.staarttrap.1..4`:
  - in `nl_nl.json` en `en_us.json`;
  - Engels in de nieuwe overlay `tools/lang/en/c416_vr_x_kaart.json`;
  - engelscheck OK.

## 2. Zone 6 buiten de stadjes

### Checker: `tools/vadsrijk/funderingcheck.py` (nieuw)
Per kolom: b is het laagste gebouwde blok boven de grond T.

| Soort | Wat het meet |
|---|---|
| GAT | 1-6 blokken lucht/planten onder de onderkant van een bouwwerk |
| ZWEEFT | meer dan 6 blokken |
| LEK | water met lucht ernaast of eronder |
| BROK | een los klompje van 8 blokken of minder, helemaal vrij |

Hoe de checker telt:
- **Alleen de onderkant telt:** een kolom telt alleen als er in de 5x5 eromheen geen lagere b is, over tegelgrenzen heen. Dakranden, bogen over doorgangen en balkons tellen dus niet.
- **Holle binnenkant telt niet:** de holgemaakte binnenkant van massieve blokken telt niet (de gat-cellen moeten met de buitenlucht verbonden zijn).
- **Overgeslagen:** de stadsvakken; kolommen over water; een muur die op een tapijt staat.
- **Zone 6 gebruikt de echte grondhoogte van de generator** (`_cache/z6/terrein.npz`, geschreven door maak6). De rotsbanden van zone 6 zijn ook muurmateriaal, dus op naam alleen kun je de grond daar niet herkennen. Draai dus eerst maak6, dan de check.
- **Bewust zwevend:** de ringen en de lens van de Sterrendovende Toren (gebied `z6_toren`) tellen niet als ZWEEFT.
- **Zones 1-5 gaan op naam en zijn alleen een indicatie.** Er zitten veel bewuste bouwsels tussen: de platforms van de ijsnaalden, gebouwen op palen, tentdaken, de paddenstoelenbossen van zone 3, het walvisvel. Ook worden daar geen stadsvakken overgeslagen (de Gebied-regex vindt er geen).

### Generatorpass: `zone6/fundering6.py` (nieuw, aangeroepen door maak6)
- **De grondhoogte:** `terrein_top()` direct na land + wegen.
- **De pass:** `bouw()` helemaal aan het eind van de wereldbouw. Dezelfde regel als GAT, tot 8 blokken (palmstammen/boomstammen tot 16):
  - onder een gewoon vol blok (muur, tentdoek, stam) komt hetzelfde blok;
  - onder iets anders (trap, plaat, hek, deur, goudlijst, glas) komt het eigen grondblok (glooiend).
- **Nooit gevuld:**
  - in stadsvakken;
  - boven water;
  - waar de grond later is weggegraven;
  - de zwevende delen van de toren;
  - gaten van 2+ boven een weg (doorgang onder een boog/brug blijft open).
- **Deurstoepjes:** voor elke deur buiten de steden, als de stoep hangt. Er waren er 0 nodig.
- **`losse_brokjes()`:** losse blokken met 26 lege buren weg.
- **Uitgeprobeerd en weer weggehaald:** een "waterspiegels gelijktrekken"-pass. Die tilde de gedeelde zee op.

### Wegen door water (`zone6/bouw6.py` `pad()`)
- **Wat er mis was:** het profiel volgde de rivier- of meerbodem, en het wegbed groef een droge geul door het water. Water stroomde erin, en de weg lag onder de waterspiegel. Zo bij het rivierpad (Palmenkloven) en de Oostroute (Moederoase).
- **Nu:** het profiel is minstens de waterspiegel (een dijkje). Waar de vereffening toch onder de spiegel uitkomt, wordt er niet onder de spiegel uitgegraven: dan is het een doorwaadplaats.

### Cijfers zone 6, buiten de steden (cellen / plekken)

| | GAT | ZWEEFT | LEK | BROK |
|---|---|---|---|---|
| vóór (w-int-tegels, zelfde checker) | 399 / 32 | 169 / 31 (alle niet-toren) | 992 / 16 | 36 / 32 |
| na (lokale regen) | **0 / 0** | **0 / 0** | 48 / 2 | 18 / 18 |

- **De ergste plek vóór:** de tufstenen voet van de Sterrendovende Toren (258 cellen, 4 hoog).
- **Daarna:**
  - de goudranden van het Palm-, Duin- en Ruïnebaken (1 hoog);
  - de tenten van het karavaankamp en de Duinput (1-6 hoog);
  - zwevende palmstammen (7-11 hoog).

### Zones 1-5 (dezelfde checker, op naam, alleen indicatie)

| zone | GAT | ZWEEFT | LEK | BROK |
|---|---|---|---|---|
| 1 | 1604/896 | 4958/2585 | 340/5 | 677/549 |
| 2 vóór → na | 965/97 → 966/96 | 612/98 → 612/97 | 0 | 143/129 |
| 3 | 383/208 | 3608/2180 | 3808/39 | 2114/1721 |
| 4 | 115/27 | 0 | 0 | 55/53 |
| 5 vóór → na | 421/138 → 421/138 | 876/129 → 882/128 | 1356/60 | 505/404 |

- De steekproef van de grootste zone-2- en zone-4-plekken toont allemaal bewuste bouwsels: de ijsnaaldplatforms, het kwartsgebouw op palen, marktkraamdaken van wol.
- In zones 1-5 heb ik dus niets gerepareerd behalve de Galmtop.
- Een echte zones-1-5-check heeft per zone de grondhoogte van de generator nodig, zoals bij zone 6.

## 3. Zone 2 `uitkijk_galmtop` (`zone2/bouw2.py`)
- **Wat er mis was:** de wenteltrap van `ijsnaald()` begon altijd op hoek 0. Bij de Galmtop is dat de klifkant: de eerste trede zweefde 18 boven de grond.
- **Nu:** `_naald_start()` kiest de starthoek waar de helling de trap raakt.
  - Score: de eerste 3 treden mogen niet hangen; meer dan 3 diep begraven kost een beetje.
  - Hoek 0 blijft staan als die goed is, dus Meerkijk en Kloofkijk veranderen niet.
- `_naald_aanloop()` legt zo nodig treden onder de eerste, op een rotsvoet. Bij de Galmtop waren er 0 nodig: de eerste trede (917,162,-1099) ligt op de helling.

## 4. Checkerfix poicheck6/loopcheck6
- **Nieuw:** `zone6/z6plekken.py` leest `Z6WereldPlekken.ALLE`, de gesnapte plekken die het spel gebruikt. Alleen als de Java ontbreekt, leest hij de json-cache.
- **Uitkomst:**
  - poicheck6: 0 van 334 slecht;
  - loopcheck6: POI alleen `toren_top` (het lensanker, bewust onbereikbaar).
- loopcheck6 maakt nu ook `_cache` aan in een verse worktree.

## Checks (na lokale regen van zone 2, 5, 6)
- **funderingcheck zone 6:**
  - GAT 0, ZWEEFT 0;
  - LEK 48/2 (de zeespiegelsprong aan de westrand, zie open punten);
  - BROK 18.
- **poicheck6:** 0/334.
- **zweefcheck:**
  - zone 6: 0;
  - zones 2 en 5: 20 en 129, gelijk aan w-int (ongewijzigd).
- **kijkcheck6:** OK.
- **verbindcheck:** 0 (los 0, fout 0, as 0, gat 0).
- **schepen_check:** 23 instappunten, 0 fout.
- **taalcheck en engelscheck:** taalcheck 0 ontbrekend, engelscheck OK.
- **loopcheck6:**
  - TRAP 332, TRAP- 111 (vóór 337/114);
  - geen enkele POI-melding meer.
  - Een tussenversie zette `z6w_mobs_palmenkloven_2` op een lantaarnpaal in de rivier. Opgelost: geen lantaarnpalen in het water naast een dijkje.
- **bereikcheck --strikt:** exit 0. Alle zones zijn hoofd/npc 100 %. Zone 6: zij 37/38, geheim 45/47 (zelfde als ronde W).
- Geen gametests of client gedraaid (alleen Python-generators en checks).

## Gedeelde bestanden (gemarkeerd X-KAART)
- `tools/vadsrijk/zone5/bouw5.py` (staarttrap herschreven), `zone5/maak5.py` (borden-nbt)
- `tools/vadsrijk/zone6/maak6.py` (aanroep fundering6 + terrein-cache), `zone6/bouw6.py` (`pad()` door water)
- `tools/vadsrijk/zone2/bouw2.py` (`ijsnaald`)
- `tools/vadsrijk/zone6/poicheck6.py`, `zone6/loopcheck6.py`
- `src/main/resources/assets/guhs/lang/nl_nl.json`, `en_us.json` (4 sleutels)
- Nieuw:
  - `tools/vadsrijk/funderingcheck.py`;
  - `zone6/fundering6.py`;
  - `zone6/z6plekken.py`;
  - `tools/lang/en/c416_vr_x_kaart.json`.

## Open punten
- De borden en de walvistrap zijn niet in een client bekeken. Integratie: na de regen even kijken (SPIEK §1).
- BROK zone 6: 18 over. Bladeren tellen niet als buur in de checker: losse vruchten (oranje terracotta) en de tussenstukken van
  scheve palmstammen in kronen blijven gemeld. De pass haalt alleen echt vrijstaande blokjes weg.
- LEK zone 6: 48 cellen op 2 plekken aan de westrand (x 3275, z -985 / -1027).
  - Daar ligt de zee op y 63 naast water op y 62: een treetje van 1 in de waterspiegel, met wat stromend water.
  - Gelijktrekken tilde de gedeelde zee op en is dus weer weggehaald.
  - De echte fix zit in `zijwereld.velden` (één zeespiegel); niet aangeraakt.
- Paden met 1-bloksprongen: loopcheck6 TRAP-meldingen (vooral decoratieve trappen) niet nagelopen; zie `_cache/loopcheck_z6.json`.
