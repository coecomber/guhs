# XP-balans Het Vadsrijk

Gemaakt met `python tools/vadsrijk/xpbalans.py` (opties: `--kpm`, `--onderweg`, `--zone N`, `--schaal z:hoofd:zij` voor wat-als).
Bron: de quest-JSON's (`data/guhs/vadsrijk/npc/quests*.json`) + de xp-formules uit de Java-code (Levels-curve, mob-xp per zone,
bazen 0,35 level, kerker-run = 3+4 mobs + elite + grotbaas 0,15 level). Het **generieke** level telt voor quest-poorten.
A = alleen hoofdquests, B = hoofd + zij + 1 klasse-quest + eerste keer elke baas, C = B + 1 kerker-run. "Onderweg" = extra
gewone kills per quest (normaal spelen; 0 = alleen wat de quests zelf vragen).

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
