# XP-balans Het Vadsrijk

Gemaakt met `python tools/vadsrijk/xpbalans.py` (opties: `--kpm`, `--onderweg`, `--zone N`, `--runs N` (scenario D), `--schaal z:hoofd:zij` voor wat-als).
Bron: de quest-JSON's (`data/guhs/vadsrijk/npc/quests*.json`) + de xp-formules uit de Java-code (Levels-curve, mob-xp per zone,
bazen 0,35 level, kerker-run = 3+4 mobs + elite + grotbaas, sinds 1.10.1 samen ~0,8 level van het baaslevel). Het **generieke** level telt voor quest-poorten.
A = alleen hoofdquests, B = hoofd + zij + 1 klasse-quest + eerste keer elke baas, C = B + 1 kerker-run, D = alleen hoofd + N kerker-runs. "Onderweg" = extra
gewone kills per quest (normaal spelen; 0 = alleen wat de quests zelf vragen).

## Ronde 1.10.1 (1101-balans, 2026-11-14)

**Gevraagd:** zone 1 voelde in de tweede helft te makkelijk; hoofdquests droegen 87 % van de xp (hq08 en hq09 gaven elk 3,4-3,9
levels). Doel: alleen-hoofd ("los", precies op het poortlevel binnen, 5 kills onderweg) ~10-30 min grinden per zone (2 kills/min);
hoofd + 2-4 kerker-runs per zone: geen grind. Kerkers moeten de moeite waard worden. Analyse: `BALANS_1101_ANALYSE.md` sectie 1.

**Wat er veranderde:**
- **Hoofdquest-xp** (alleen `soort == "hoofd"`; zij-, rijdier- en klasse-quests ongewijzigd), als factor bovenop `X_ZONE1_XP`
  in de generators (blok `BALANS_1101_HOOFD`, afgerond op 10):
  - zone 1: tutorial hq01-hq07 vol; **x0,6 vanaf hq08** (hq08 3000 -> 1800, hq09 5900 -> 3540, hq10 4400 -> 2640,
    hq11 3000 -> 1800, hq12 2000 -> 1200); totaal hoofd 19.590 -> 12.270;
  - zone 2 **x0,6** (18.900 -> 11.340), zone 3 **x0,8** (137.500 -> 110.000), zone 4 **ongewijzigd** (70.900),
    zone 5 **x0,65** (61.400 -> 39.920), zone 6 **x0,7** (160.400 -> 112.290).
- **Kerkerbaas** (`GrotBaas.xp()`): van `max(150, 0,15 * stap(baasLv))` naar `max(150, f * stap(baasLv))` met **f = 0,50 in
  zone 1-2 en 0,63 in zone 3-6**, zodat een solo-run (7 pack-mobs + 1 elite + baas) **~0,8 level van het baaslevel** geeft:
  Zwamgrot 791 -> 1.401, Kristalgrot 1.925 -> 3.448, Wortelgrot 3.905 -> 9.801, Koraalgrot 5.080 -> 12.621,
  Wolkengrot 5.758 -> 14.176, Zingende Zoutgrot 6.785 -> 16.825. (Het verschil komt doordat mobs in zone 1-2 relatief meer xp
  geven: daar doen pack + elite al ~0,3 level, in zone 3-6 maar ~0,17.)
- Nieuw in `xpbalans.py`: **scenario D** = alleen hoofd + N kerker-runs (geen zijquests), tabel "Alleen hoofd + kerker-runs"
  (`--runs N`, standaard 3). Runs worden gedaan zodra je level >= kerker-minLv - 1.
- Levelpoorten, curve, mob-xp, bazen buiten de kerkers en zijquests: **niet** aangeraakt.

**Grind per zone (los, 5 kills onderweg, 2 kills/min):**

| Zone | A vóór | A na | Hoofd + 2 runs vóór / na | Hoofd + 3 runs vóór / na | Hoofd + 4 runs vóór / na |
|---|---|---|---|---|---|
| 1 Klodderdal | 0 | **30 min** | 0 / 7 min | 0 / **0** | 0 / 0 |
| 2 Splinterpieken | 1 min | **23 min** | 0 / 0 | 0 / **0** | 0 / 0 |
| 3 Schemerwortel | 10 min | **72 min** | 0 / 24 min | 0 / **13 min** | 0 / 13 min |
| 4 Lampionkust | 1 min | **1 min** | 0 / 0 | 0 / **0** | 0 / 0 |
| 5 Hoogvads | 19 min | **39 min** | 19 min / 28 min | 19 min / **28 min** | 19 min / 28 min |
| 6 Zijdeland | 7 min | **64 min** | 6 min / 20 min | 6 min / **18 min** | 6 min / 18 min |

**VOOR (ronde X)** en **NA (1.10.1)**, volledige tabellen van de tool (standaard, 5 kills onderweg):

#### VOOR (ronde X)

##### Los (zone binnen op PRECIES het poortlevel: de strenge toets)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 22.02 | 28.11 | 28.26 | 0 | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 20.0 / 20.0 | 31.86 | 33.10 | 33.40 | 1 min | 0 | 0 | 332 xp/min (mobs lv 33) |
| 3 De Schemerwortel | 50 | 30.0 / 30.0 | 50.42 | 51.09 | 51.38 | 10 min | 0 | 0 | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 50.0 / 50.0 | 56.07 | 58.58 | 58.89 | 1 min | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 55.0 / 55.0 | 61.09 | 63.52 | 63.83 | 19 min | 16 min | 16 min | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 60.0 / 60.0 | 69.70 | 70.00 | 70.00 | 7 min | 0 | 0 | 798 xp/min (mobs lv 70) |

##### Alleen hoofd + kerker-runs (scenario D, los, 5 kills onderweg; runs zodra je level >= kerker-minLv - 1)

| Zone | Kerker (1 run) | Grind 0 runs | Grind 2 runs | Grind 3 runs | Grind 4 runs | Uit (3 runs) |
|---|---|---|---|---|---|---|
| 1 Het Klodderdal | Zwamgrot 791 xp (0.45 lv van 14) | 0 | 0 | 0 | 0 | 22.70 |
| 2 De Splinterpieken | Kristalgrot 1925 xp (0.44 lv van 25) | 1 min | 0 | 0 | 0 | 32.74 |
| 3 De Schemerwortel | Wortelgrot 3905 xp (0.32 lv van 48) | 10 min | 0 | 0 | 0 | 51.00 |
| 4 De Lampionkust | Koraalgrot 5080 xp (0.32 lv van 56) | 1 min | 0 | 0 | 0 | 57.02 |
| 5 Hoogvads | Wolkengrot 5758 xp (0.33 lv van 60) | 19 min | 19 min | 19 min | 19 min | 62.05 |
| 6 Het Zijdeland | Zingende Zoutgrot 6785 xp (0.32 lv van 67) | 7 min | 6 min | 6 min | 6 min | 70.00 |

#### NA (1.10.1)

##### Keten (zone binnen op het eigen level uit de vorige zone)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 20.95 | 26.60 | 26.91 | 30 min | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 21.0 / 26.6 | 31.60 | 35.82 | 36.49 | 11 min | 0 | 0 | 348 xp/min (mobs lv 35) |
| 3 De Schemerwortel | 50 | 31.6 / 35.8 | 50.09 | 52.05 | 53.15 | 42 min | 0 | 0 | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 50.1 / 52.1 | 56.12 | 60.24 | 61.85 | 0 | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 56.1 / 60.2 | 60.57 | 66.36 | 68.41 | 10 min | 0 | 0 | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 60.6 / 66.4 | 69.44 | 70.00 | 70.00 | 49 min | 0 | 0 | 798 xp/min (mobs lv 70) |

##### Los (zone binnen op PRECIES het poortlevel: de strenge toets)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 20.95 | 26.60 | 26.91 | 30 min | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 20.0 / 20.0 | 31.60 | 31.91 | 32.48 | 23 min | 0 | 0 | 332 xp/min (mobs lv 33) |
| 3 De Schemerwortel | 50 | 30.0 / 30.0 | 50.09 | 50.09 | 50.09 | 72 min | 37 min | 14 min | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 50.0 / 50.0 | 56.07 | 58.58 | 59.33 | 1 min | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 55.0 / 55.0 | 60.57 | 62.51 | 63.28 | 39 min | 20 min | 20 min | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 60.0 / 60.0 | 69.44 | 70.00 | 70.00 | 64 min | 6 min | 6 min | 798 xp/min (mobs lv 70) |

##### Bronnen per zone (scenario C, los)

| Zone | Hoofd-xp | Zij-xp | Kills (quest + onderweg) | Bazen (1e keer) | 1 kerker-run | Kerker | Zij niet gehaald (level) |
|---|---|---|---|---|---|---|---|
| 1 | 12270 | 8960 | 14859 | 11355 | 1401 | Zwamgrot (lv 11-13) | - |
| 2 | 11340 | 2000 | 20290 | 20394 | 3448 | Kristalgrot (lv 22-24) | - |
| 3 | 110000 | 6213 | 18680 | 33429 | 9801 | Wortelgrot (lv 44-46) | - |
| 4 | 70900 | 10900 | 31342 | 13506 | 12621 | Koraalgrot (lv 53-55) | - |
| 5 | 39920 | 16019 | 32215 | 25985 | 14176 | Wolkengrot (lv 58-60) | - |
| 6 | 112290 | 14390 | 42175 | 39456 | 16825 | Zingende Zoutgrot (lv 64-66) | - |

Puur quest-xp vanaf level 1, zonder een enkele kill (cumulatief): z1 hoofd 15.9 / alles 19.6, z2 hoofd 20.4 / alles 23.6, z3 hoofd 39.5 / alles 41.4, z4 hoofd 46.5 / alles 48.9, z5 hoofd 49.8 / alles 53.1, z6 hoofd 57.6 / alles 61.0.

##### Alleen hoofd + kerker-runs (scenario D, los, 5 kills onderweg; runs zodra je level >= kerker-minLv - 1)

| Zone | Kerker (1 run) | Grind 0 runs | Grind 2 runs | Grind 3 runs | Grind 4 runs | Uit (3 runs) |
|---|---|---|---|---|---|---|
| 1 Het Klodderdal | Zwamgrot 1401 xp (0.80 lv van 14) | 30 min | 7 min | 0 | 0 | 21.08 |
| 2 De Splinterpieken | Kristalgrot 3448 xp (0.79 lv van 25) | 23 min | 0 | 0 | 0 | 32.27 |
| 3 De Schemerwortel | Wortelgrot 9801 xp (0.80 lv van 48) | 72 min | 24 min | 13 min | 13 min | 50.48 |
| 4 De Lampionkust | Koraalgrot 12621 xp (0.80 lv van 56) | 1 min | 0 | 0 | 0 | 58.41 |
| 5 Hoogvads | Wolkengrot 14176 xp (0.81 lv van 60) | 39 min | 28 min | 28 min | 28 min | 62.56 |
| 6 Het Zijdeland | Zingende Zoutgrot 16825 xp (0.80 lv van 67) | 64 min | 20 min | 18 min | 18 min | 70.00 |

##### Keten bij 0 / 2 / 5 kills onderwegper quest (uit-level per zone, A = alleen hoofd, B = hoofd + zij + bazen)

| Kills onderweg | z1 A / B | z2 A / B | z3 A / B | z4 A / B | z5 A / B | z6 A / B | B haalt 70 bij |
|---|---|---|---|---|---|---|---|
| 0 | 20.7 / 24.2 | 31.3 / 32.3 | 49.8 / 49.8 | 55.9 / 56.7 | 60.3 / 61.5 | 69.3 / 70.0 | - |
| 2 | 20.8 / 25.1 | 31.4 / 33.7 | 49.9 / 50.0 | 55.9 / 57.4 | 60.4 / 62.8 | 69.4 / 70.0 | z6 z6_hq09_spiegelzaal |
| 5 | 21.0 / 26.6 | 31.6 / 35.8 | 50.1 / 52.1 | 56.1 / 60.2 | 60.6 / 66.4 | 69.4 / 70.0 | z6 z6_s10_koerier |

**Conclusie:**
- **Zone 1, 2 en 4 halen het doel:** alleen-hoofd 30 / 23 / 1 min grinden (zone 4 bewust ongewijzigd), met 2-3 Zwamgrot- of
  1-2 Kristalgrot-runs 0 min. Het level bij hq09/hq10/hq11 zakt ~0,5-1 level; einde zone 1 nu ~21 (was ~22).
- **Zone 3, 5 en 6 missen de 10-30 min met de gekozen factoren** (niet aangepast, keuze van de gebruiker): alleen-hoofd
  72 / 39 / 64 min. Met kerker-runs zakt dat naar 13 / 28 / 18 min, maar **niet naar 0**: alle resterende grind zit bij
  levelpoorten *vóórdat* de kerker op level is (Wortelgrot vanaf ~43, Wolkengrot vanaf ~57, Zoutgrot vanaf ~63). Een vierde run
  helpt dan niet meer. Wie de kerker eerder (onder level) doet, of de zijquests meeneemt (B), heeft in zone 3 nog 37 min (B) /
  14 min (C), in zone 5 20 min, in zone 6 6 min nodig.
- In de **keten** (binnen op het eigen level uit de vorige zone) is alleen-hoofd: 30 / 11 / 42 / 0 / 10 / 49 min. Hoofd + zij
  (B) heeft nergens grind nodig; B haalt de cap 70 nu pas bij z6_s10 (5 kills onderweg) of z6_hq09 (2 kills), bij 0 kills niet.

## Ronde X (X-ZONE1, 2026-11-09): zone 1 als losse release + een gladde keten

**Gevraagd:** zone 1 alleen-hoofd max ~15 min grinden (was 70-90); hoofd+zij eindigt rond het startlevel van de volgende zone;
zone 2 gaf veel te veel xp (hoofd+zij zat op de cap 70 aan het eind van zone 5); de cap 70 moet rond het eind van zone 6 vallen.

**Wat er veranderde (alleen quest-xp, in de generators: blok `X_ZONE1_XP`):**
- **Zone 1:** hoofdquests hq08/hq09/hq10 omhoog (700/1100/1400 -> 3000/5900/4400), zodat je bij de poorten 12/16/18 al op
  level bent; zijquests en rijdier-quests x0,35 (bv. premie tempel 2500 -> 880). Tutorial hq01-hq07, hq11 (Opperzwam), hq12,
  klasse-quest en de spion ongewijzigd.
- **Zone 2:** hoofdquests van 2.500-15.000 naar ~1.050-3.000 (0,35 level per stap; de bazen Galmwolf/Spiegelzucht/Boormeester
  geven zelf al ~20.000 xp); zijquests 2.500-9.000 -> ~310-380.
- **Zone 3:** hoofdquests omhoog (2.615-5.528 -> 3.500-16.600): de zone moet nu echt van 30 naar 50 brengen (eerst deed zone 2
  dat al tot 41). Zijquests ~0,08 level.
- **Zone 4/5/6:** hoofdstappen opnieuw verdeeld (zone 4 ~0,6-0,95 level, zone 5 ~0,3-0,63, zone 6 0,5-1,5 bij de grote
  poortsprongen 63 -> 65 -> 67); zijquests ~0,05 level (zone 4: 2.884 -> 660, zone 6: 3.858 -> 880).
- Methode: per zone een "floor" (vast deel van een level per hoofdstap) en waar een poort meer vraagt precies genoeg om de
  volgende poort te halen (A, 2 kills onderweg); een stap boven 1,5 level schuift het teveel naar de stap ervoor (gladder).
- Levelpoorten (`lv`), curve, cap (70), mob-xp, bazen en kerkers: **niet** aangeraakt. De questrobot kijkt niet naar xp.

**Resultaat (keten, 2 kills/min grinden):**


**VOOR (ronde W)** – keten bij 0 / 2 / 5 kills onderweg per quest (uit-level per zone, A = alleen hoofd, B = hoofd + zij + bazen)

| Kills onderweg | z1 A / B | z2 A / B | z3 A / B | z4 A / B | z5 A / B | z6 A / B | B haalt 70 bij |
|---|---|---|---|---|---|---|---|
| 0 | 21.3 / 26.3 | 40.6 / 45.7 | 49.7 / 53.8 | 55.7 / 61.5 | 61.2 / 68.5 | 69.8 / 70.0 | z6 z6_s02_tuinen |
| 2 | 21.4 / 27.2 | 41.0 / 46.6 | 49.8 / 55.1 | 55.7 / 63.3 | 61.5 / 70.0 | 69.9 / 70.0 | z5 z5_hq10_stormrand |
| 5 | 21.6 / 28.6 | 41.6 / 48.0 | 51.0 / 57.1 | 56.4 / 65.9 | 62.4 / 70.0 | 70.0 / 70.0 | z5 z5_hq06_verhoren |

**NA (ronde X)** – keten bij 0 / 2 / 5 kills onderweg per quest (uit-level per zone, A = alleen hoofd, B = hoofd + zij + bazen)

| Kills onderweg | z1 A / B | z2 A / B | z3 A / B | z4 A / B | z5 A / B | z6 A / B | B haalt 70 bij |
|---|---|---|---|---|---|---|---|
| 0 | 21.3 / 25.9 | 31.6 / 34.5 | 50.2 / 51.9 | 55.9 / 58.3 | 60.6 / 64.0 | 69.5 / 70.0 | z6 z6_hq07_zoutvlakte |
| 2 | 21.5 / 26.8 | 31.8 / 35.8 | 50.4 / 53.2 | 56.0 / 60.1 | 60.8 / 66.2 | 69.6 / 70.0 | z6 z6_hq06_tapijt |
| 5 | 22.0 / 28.1 | 32.9 / 37.8 | 51.5 / 55.1 | 57.3 / 62.7 | 62.5 / 69.5 | 70.0 / 70.0 | z6 z6_hq02_karavaan |

- **Zone 1 alleen-hoofd: 0 min grinden** bij 2 of 5 kills onderweg, **5 min** bij 0 kills onderweg (was 71-91 min). Einde ~21,3-22.
- Elke zone: alleen-hoofd haalt het startlevel van de volgende zone met 0 min grinden in de keten (in de strenge "los"-toets,
  precies op het poortlevel binnen, hooguit 1-19 min).
- Hoofd+zij (B) zonder extra kills: 25,9 / 34,5 / 51,9 / 58,3 / 64,0 en de cap 70 pas bij z6_hq07 (van 10). Was: 26,3 / 45,7 /
  53,8 / 61,5 / 68,5. Met 2 kills onderweg: 70 bij z6_hq06 (was al bij z5_hq10).
- **Eerlijk:** B loopt in zone 1 vanzelf ~5 levels voor (zone 1 heeft 28 zijquests met eigen kill-doelen + Koning Klodder +
  de spion); zelfs met 0 xp voor zijquests eindigt B in zone 1 op ~26 terwijl A 20 moet halen. Met de zone-poort op 1 merk je
  dat niet: het level stopt op 20 en het teveel wordt gespaard (hieronder). In zones 4-5 (smalle levelband, veel zijquests)
  loopt B weer ~2-4 levels voor; de cap 70 valt voor B daardoor in de tweede helft van zone 6, niet op de laatste quest.
  Verder omlaag kan alleen met minder xp voor kills/bazen van zijcontent (mob-xp, optionele wereldbazen): niet gedaan.

### Zachte cap (zone-poort, `ZonePoort.knip`)
- Poort op N (< 6): level (generiek en klasse) stopt op het startlevel van zone N+1: **20** / 30 / 50 / 56 / 60. De xp-balk
  staat dan vol, het Karakter-menu zegt "Hoogste level voor nu: <zone> gaat binnenkort open. Gespaarde xp: ...".
- Het teveel wordt **gespaard, max 3 levels** (bij poort 1: xp van 20 -> 23 = ~10.600 xp). Eén chatregel bij het bereiken.
- Gaat de poort omhoog: elke xp-winst krijgt **hetzelfde bedrag extra uit de spaarpot** (dubbele xp) tot de pot leeg is. Geen
  sprong in één keer, niets te migreren. Ops/creative hebben geen cap.

## Tabellen (ronde X, standaard: 5 kills onderweg)

Aannames: solo, geen bonussen (rust/eten/bond/reeks/groep), grinden 2 kills/min in de beste mobzone van de zone zelf, 5 kills onderweg per quest (normaal spelen).
A = alleen hoofdquests; B = hoofd + zij + 1 klasse-quest + eerste keer elke baas; C = B + 1 kerker-run (solo).
Grind = minuten mobs in DEZE zone, voor levelpoorten onderweg + tot het doel.

### Keten (zone binnen op het eigen level uit de vorige zone)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 22.02 | 28.11 | 28.26 | 0 | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 22.0 / 28.1 | 32.85 | 37.78 | 38.13 | 0 | 0 | 0 | 348 xp/min (mobs lv 35) |
| 3 De Schemerwortel | 50 | 32.9 / 37.8 | 51.48 | 55.12 | 55.59 | 0 | 0 | 0 | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 51.5 / 55.1 | 57.30 | 62.74 | 63.40 | 0 | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 57.3 / 62.7 | 62.46 | 69.50 | 70.00 | 0 | 0 | 0 | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 62.5 / 69.5 | 70.00 | 70.00 | 70.00 | 0 | 0 | 0 | 798 xp/min (mobs lv 70) |

### Los (zone binnen op PRECIES het poortlevel: de strenge toets)

| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |
|---|---|---|---|---|---|---|---|---|---|
| 1 Het Klodderdal | 20 | 1.0 / 1.0 | 22.02 | 28.11 | 28.26 | 0 | 0 | 0 | 152 xp/min (mobs lv 20) |
| 2 De Splinterpieken | 30 | 20.0 / 20.0 | 31.86 | 33.10 | 33.40 | 1 min | 0 | 0 | 332 xp/min (mobs lv 33) |
| 3 De Schemerwortel | 50 | 30.0 / 30.0 | 50.42 | 51.09 | 51.38 | 10 min | 0 | 0 | 462 xp/min (mobs lv 49) |
| 4 De Lampionkust | 56 | 50.0 / 50.0 | 56.07 | 58.58 | 58.89 | 1 min | 0 | 0 | 572 xp/min (mobs lv 56) |
| 5 Hoogvads | 60 | 55.0 / 55.0 | 61.09 | 63.52 | 63.83 | 19 min | 16 min | 16 min | 638 xp/min (mobs lv 60) |
| 6 Het Zijdeland | 69 | 60.0 / 60.0 | 69.70 | 70.00 | 70.00 | 7 min | 0 | 0 | 798 xp/min (mobs lv 70) |

### Bronnen per zone (scenario C, los)

| Zone | Hoofd-xp | Zij-xp | Kills (quest + onderweg) | Bazen (1e keer) | 1 kerker-run | Kerker | Zij niet gehaald (level) |
|---|---|---|---|---|---|---|---|
| 1 | 19590 | 8960 | 14844 | 11355 | 791 | Zwamgrot (lv 11-13) | - |
| 2 | 18900 | 2000 | 20310 | 20394 | 1925 | Kristalgrot (lv 22-24) | - |
| 3 | 137500 | 6213 | 19010 | 33599 | 3905 | Wortelgrot (lv 44-46) | - |
| 4 | 70900 | 10900 | 31302 | 13466 | 5080 | Koraalgrot (lv 53-55) | - |
| 5 | 61400 | 16019 | 32175 | 25985 | 5758 | Wolkengrot (lv 58-60) | - |
| 6 | 160400 | 14390 | 42315 | 39456 | 6785 | Zingende Zoutgrot (lv 64-66) | - |

Puur quest-xp vanaf level 1, zonder een enkele kill (cumulatief): z1 hoofd 19.0 / alles 21.9, z2 hoofd 24.6 / alles 27.0, z3 hoofd 43.9 / alles 45.5, z4 hoofd 50.0 / alles 52.1, z5 hoofd 54.4 / alles 57.3, z6 hoofd 63.9 / alles 66.9.


# Geschiedenis: W-BALANS (ronde W)

### Voor en na (los, precies op poortlevel binnen)

| Zone | Scenario | Vóór (0 / 5 kills onderweg) | Na (0 / 5 kills onderweg) |
|---|---|---|---|
| 4 | hoofd | uit 55,45 / 55,54; grind 65 / 48 min | uit 55,65 / 55,74; grind 29 / 12 min |
| 4 | hoofd+zij+bazen | uit 56,75 / 58,68; grind 0 | uit 58,50 / 60,36; grind 0 |
| 6 | hoofd | uit 69,45 / 69,54; grind 122 / 100 min | uit 69,80 / 69,89; grind 40 / 19 min |
| 6 | hoofd+zij+bazen | grind **47** / 0 min (poorten 65-69) | grind 0 / 0 |

### Per zone, kort

| Zone | Oordeel | Wat |
|---|---|---|
| 1 Klodderdal (1-20) | OK met zij | hoofd+zij → ~28. Alleen hoofd: ~70-90 min grinden (de poorten 6/12/16/18 bij lage hoofd-xp). Niet aangepast: zone 1 is de tutorial, de zijquests (premies) staan meteen open. |
| 2 Splinterpieken (20-30) | Veel te ruim | alleen hoofd al 20 → 41! Hoofdquests geven vaste 3.000-15.000 xp (hq11 = 3 levels). Niet aangepast (geen gat), wel open punt. |
| 3 Schemerwortel (30-50) | OK in de keten | Los vanaf 30 komt hij ~175-225 min tekort (de zone is ontworpen met ~10 levels uit kills). Omdat zone 2 iedereen op 41-48 aflevert, is er in de praktijk geen grind. Niet aangepast. |
| 4 Lampionkust (50-56) | **Aangepast** | zie boven. |
| 5 Hoogvads (55-60) | OK | Binnen op 56 (nieuw doel van zone 4): geen grind. Wie op precies 55 binnenkomt: ~12 min voor de lv-56-poort van `z5_hq02`. Niet aangepast. |
| 6 Zijdeland (60-69) | **Aangepast** | hoofdstap 45 % → 80 % van een level: 10 stappen voor 9 levels. hoofd+zij heeft nu geen grind meer nodig; alleen hoofd 19-40 min. |

### Kerkers (grotten)

Elke zone heeft een kerker: Zwamgrot (z1, 11-13), Kristalgrot (z2, 22-24), Wortelgrot (z3, 44-46), Koraalgrot (z4, 53-55),
Wolkengrot (z5, 58-60), Zingende Zoutgrot (z6, 64-66; zonder poort). **Geen zone mist een kerker.** Een solo-run is
~0,3 level waard (~5-9 min grinden); het is dus leuk extra, geen vervanging voor quests. Kerkercode niet aangeraakt (w-grot).

### Aannames

- Solo, geen bonussen: rust-xp (U-HUIZEN, tot 2x), eten (gevechtXp), bond (1,5x), reeks (tot 1,2x), groep. Met bonussen gaat alles sneller.
- Grinden: 2 kills/min (Z3Xp-ontwerp) in de beste mobzone van de zone zelf met minLv ≤ level+2; mob-xp z3-z6 = stap(lv)/55.
- Bazenlevels zijn geschat uit de registries (z3-bazen zijn in Z3Xp gemaximeerd op lv 50); de optionele wereldbazen van z5/z6 tellen 1x mee in B/C.
- Zijquests worden gedaan zodra ze openstaan en je level het toelaat; één klasse-quest per zone telt mee bij B.
