# XP-balans Het Vadsrijk (W-BALANS, 2026-11-08)

Gemaakt met `python tools/vadsrijk/xpbalans.py` (opties: `--kpm`, `--onderweg`, `--zone N`, `--schaal z:hoofd:zij` voor wat-als).
Bron: de quest-JSON's (`data/guhs/vadsrijk/npc/quests*.json`) + de xp-formules uit de Java-code (Levels-curve, mob-xp per zone,
bazen 0,35 level, kerker-run = 3+4 mobs + elite + grotbaas 0,15 level). Het **generieke** level telt voor quest-poorten.

## Het antwoord op de vraag: De Lampionkust (zone 4)

- De Lampionkust = **zone 4**, levelband **50-56**: eerste hoofdquest vraagt lv 50, de laatste (`z4_hq07_opstijgpier`) lv 55.
  Zone 5 vraagt lv 55 voor `z5_hq01` en **lv 56** voor `z5_hq02`. Mobzones 50-56 (rotsjes, Bloemeneiland, Vliegereiland,
  Koraalbaai, Wrak, Mysterie-eiland 55-56), bazen Zeeslurf (54) en Vliegervanger (56).
- **Er is een grot/kerker:** de **Koraalgrot** (lv 53-55, baas 56). Eén solo-run = ca. **5.080 xp** (~0,33 level, ~9 min grinden
  waard). Hij reset 10 min na een clear.
- **Vóór de fix** (zone binnen op precies lv 50, solo, zonder bonussen):
  - hoofd + alle zijquests + de twee bazen = **lv 56,75**, zonder één extra kill. Je haalt 56 dus net in de zone zelf, met
    maar ~0,75 level marge: wie 3-4 zijquests overslaat, komt tekort.
  - Alleen hoofdquests = lv 55,45, met ~**50 min grinden tussen de stappen** (de poorten 51→55 stijgen 1 level per stap, een
    stap gaf maar 45 % van een level) en daarna nog ~15 min tot 56.
- **Na de fix** (hoofdstap 65 % van een level, zijquest 22 %):
  - hoofd + zij + bazen = **lv 58,5** (zonder extra kills) tot **lv 60,4** (met ~5 kills onderweg per quest). 56 wordt dus
    ruim gehaald, ook als je de helft van de zijquests overslaat. **Niet terug naar zone 3.**
  - Alleen hoofdquests = lv 55,7: van 55,7 naar 56 is nog ~4.000-5.300 xp = **~7-10 min grinden** aan de Mysterie-kust
    (lv 55-56 mobs, ~560 xp/min bij 2 kills/min). Totaal grinden voor een hoofd-only speler in zone 4: 12-29 min (was 48-65).
- In de praktijk komen spelers zone 4 bovendien binnen op ~lv 51 (alleen hoofd) of ~lv 57 (hoofd+zij), omdat zone 2 erg veel
  xp geeft (zie "open punten").

## Tabellen (na de fix)

Aannames: solo, geen bonussen (rust/eten/bond/reeks/groep), grinden 2 kills/min in de beste mobzone van de zone zelf, 5 kills onderweg per quest (normaal spelen).
A = alleen hoofdquests; B = hoofd + zij + 1 klasse-quest + eerste keer elke baas; C = B + 1 kerker-run (solo).
Grind = minuten mobs in DEZE zone, voor levelpoorten onderweg + tot het doel.

### Keten (zone binnen op het eigen level uit de vorige zone)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 21.56 | 28.57 | 28.72 | 71 min | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 21.6 / 28.6 | 41.65 | 48.00 | 48.23 | 0 | 0 | 0 | 348 xp/min (mobs lv 35) |
| 3 De Schemerwortel | 50 | 41.6 / 48.0 | 50.99 | 57.10 | 57.52 | 0 | 0 | 0 | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 51.0 / 57.1 | 56.43 | 65.88 | 66.46 | 0 | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 56.4 / 65.9 | 62.38 | 70.00 | 70.00 | 0 | 0 | 0 | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 62.4 / 70.0 | 70.00 | 70.00 | 70.00 | 0 | 0 | 0 | 798 xp/min (mobs lv 70) |

### Los (zone binnen op PRECIES het poortlevel: de strenge toets)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 21.56 | 28.57 | 28.72 | 71 min | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 20.0 / 20.0 | 41.12 | 44.85 | 45.03 | 0 | 0 | 0 | 348 xp/min (mobs lv 35) |
| 3 De Schemerwortel | 50 | 30.0 / 30.0 | 50.01 | 50.01 | 50.01 | 226 min | 175 min | 165 min | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 50.0 / 50.0 | 55.74 | 60.36 | 60.65 | 12 min | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 55.0 / 55.0 | 61.55 | 65.26 | 65.55 | 12 min | 12 min | 12 min | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 60.0 / 60.0 | 69.89 | 70.00 | 70.00 | 19 min | 0 | 0 | 798 xp/min (mobs lv 70) |

### Bronnen per zone (scenario C, los)

| Zone | Hoofd-xp | Zij-xp | Kills (quest + onderweg) | Bazen (1e keer) | 1 kerker-run | Kerker | Zij niet gehaald (level) |
|---|---|---|---|---|---|---|---|
| 1 | 9490 | 20990 | 15204 | 11475 | 791 | Zwamgrot (lv 11-13) | - |
| 2 | 90500 | 30500 | 21550 | 20814 | 1925 | Kristalgrot (lv 22-24) | - |
| 3 | 57787 | 10200 | 18565 | 33329 | 3905 | Wortelgrot (lv 44-46) | - |
| 4 | 63820 | 47735 | 31902 | 13506 | 5080 | Koraalgrot (lv 53-55) | - |
| 5 | 73761 | 39235 | 32555 | 25985 | 5758 | Wolkengrot (lv 58-60) | - |
| 6 | 154548 | 63349 | 43390 | 39506 | 6785 | Zingende Zoutgrot (lv 64-66) | - |

Puur quest-xp vanaf level 1, zonder een enkele kill (cumulatief): z1 hoofd 14.4 / alles 22.5, z2 hoofd 35.4 / alles 41.5, z3 hoofd 42.1 / alles 47.8, z4 hoofd 48.0 / alles 55.9, z5 hoofd 53.6 / alles 62.6, z6 hoofd 62.9 / alles 70.0.

**Doel** = het level dat de volgende zone vraagt (z1→20, z2→30, z3→50, z4→56 = `z5_hq02`, z5→60, z6→69 = de laatste hoofdquest).
"Los" is de strenge toets (precies op het poortlevel binnen); "Keten" is wat een speler echt meemaakt.

## Voor en na (los, precies op poortlevel binnen)

| Zone | Scenario | Vóór (0 / 5 kills onderweg) | Na (0 / 5 kills onderweg) |
|---|---|---|---|
| 4 | hoofd | uit 55,45 / 55,54; grind 65 / 48 min | uit 55,65 / 55,74; grind 29 / 12 min |
| 4 | hoofd+zij+bazen | uit 56,75 / 58,68; grind 0 | uit 58,50 / 60,36; grind 0 |
| 6 | hoofd | uit 69,45 / 69,54; grind 122 / 100 min | uit 69,80 / 69,89; grind 40 / 19 min |
| 6 | hoofd+zij+bazen | grind **47** / 0 min (poorten 65-69) | grind 0 / 0 |

## Per zone, kort

| Zone | Oordeel | Wat |
|---|---|---|
| 1 Klodderdal (1-20) | OK met zij | hoofd+zij → ~28. Alleen hoofd: ~70-90 min grinden (de poorten 6/12/16/18 bij lage hoofd-xp). Niet aangepast: zone 1 is de tutorial, de zijquests (premies) staan meteen open. |
| 2 Splinterpieken (20-30) | Veel te ruim | alleen hoofd al 20 → 41! Hoofdquests geven vaste 3.000-15.000 xp (hq11 = 3 levels). Niet aangepast (geen gat), wel open punt. |
| 3 Schemerwortel (30-50) | OK in de keten | Los vanaf 30 komt hij ~175-225 min tekort (de zone is ontworpen met ~10 levels uit kills). Omdat zone 2 iedereen op 41-48 aflevert, is er in de praktijk geen grind. Niet aangepast. |
| 4 Lampionkust (50-56) | **Aangepast** | zie boven. |
| 5 Hoogvads (55-60) | OK | Binnen op 56 (nieuw doel van zone 4): geen grind. Wie op precies 55 binnenkomt: ~12 min voor de lv-56-poort van `z5_hq02`. Niet aangepast. |
| 6 Zijdeland (60-69) | **Aangepast** | hoofdstap 45 % → 80 % van een level: 10 stappen voor 9 levels. hoofd+zij heeft nu geen grind meer nodig; alleen hoofd 19-40 min. |

## Kerkers (grotten)

Elke zone heeft een kerker: Zwamgrot (z1, 11-13), Kristalgrot (z2, 22-24), Wortelgrot (z3, 44-46), Koraalgrot (z4, 53-55),
Wolkengrot (z5, 58-60), Zingende Zoutgrot (z6, 64-66; zonder poort). **Geen zone mist een kerker.** Een solo-run is
~0,3 level waard (~5-9 min grinden); het is dus leuk extra, geen vervanging voor quests. Kerkercode niet aangeraakt (w-grot).

## Aannames

- Solo, geen bonussen: rust-xp (U-HUIZEN, tot 2x), eten (gevechtXp), bond (1,5x), reeks (tot 1,2x), groep. Met bonussen gaat alles sneller.
- Grinden: 2 kills/min (Z3Xp-ontwerp) in de beste mobzone van de zone zelf met minLv ≤ level+2; mob-xp z3-z6 = stap(lv)/55.
- Bazenlevels zijn geschat uit de registries (z3-bazen zijn in Z3Xp gemaximeerd op lv 50); de optionele wereldbazen van z5/z6 tellen 1x mee in B/C.
- Zijquests worden gedaan zodra ze openstaan en je level het toelaat; één klasse-quest per zone telt mee bij B.
