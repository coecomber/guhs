# NOTITIES X-ZONE1 (ronde X, 2026-11-09)

Branch `110-x-zone1` vanaf b9ef85f4c (110-w-int). Vraag: de publieke server gaat live met ALLEEN zone 1 (+ Knuffelkamp) open;
het schip naar zone 2 is tijdelijk uit. Bouw die poort goed, maak zone 1 af als losse release en trek de xp-keten recht.

## 1. De zone-poort (nieuw pakket `feature/vadsrijk/zonepoort`)
- **Instelling:** `ZonePoortConfig` = SERVER-config `guhs-vadsrijk-zones.toml`, sleutel `openZones` (1..6, standaard 6).
  `/guhs vadsrijk zones` (status) en `/guhs vadsrijk zones open <n>` (op): meteen actief, `set()+save()` in de toml.
  Alles leest `ZonePoort.open()` live; omhoog zetten heeft geen migratie nodig (quest-staten worden nooit aangeraakt).
- **Vrij:** ops (permissie 2), creative en spectator (`ZonePoort.vrij`). Tests: `ZonePoort.TEST_STRENG` (de mock-speler is creative).
- **Wat dicht gaat (zone > open), telkens met "binnenkort"-tekst:**
  - luchtschiplijnen naar een dichte zone (`Poorten.dicht`: bestemming = de haven aan de overkant; terug naar een lagere zone mag
    altijd). Pier, bord, schip-klik en loopplank gebruiken allemaal `Poorten.dicht`, de uitleg/actiebalk tonen de poorttekst;
  - reizen via NPC's, bellen, reiskaart en bond-teleports (`QuestMotor.reisSlot`; de reiskaart toont de plek grijs "Binnenkort!");
  - quests van een dichte zone worden nooit aangeboden (`Questlog.beschikbaar`), dus ook geen quest-acties/vluchten erheen;
  - dungeons van een dichte zone weigeren solo, groep, wachtrij en klaar-check (`DungeonRij.weiger`; ook de Party-tab-wachtrij);
  - de raid (zone 6) en de droom (zone 5) liggen in hun zone: daar kom je niet meer, en het vangnet vangt alles wat toch lukt;
  - Questlopen is al alleen voor ops (die mogen erdoor).
- **Einde zone 1:** hq12 "Een grauwe schaduw" gaat gewoon af (cliffhanger, `zone1_klaar`). De tracker (`QuestStap.hoofdlijn`)
  geeft dan i.p.v. z2_hq01 een stap "Wordt vervolgd / Het schip naar De Splinterpieken vaart binnenkort / Houd Discord in de
  gaten!" (geen straal). De gever van z2_hq01 (de luchtschipper, De Haven) zegt "Het schip naar De Splinterpieken vaart
  binnenkort! Houd Discord in de gaten." met zijn reiskeuzes eronder (`QuestMotor.klik` stap 5c, `ZonePoort.binnenkortRegel`).
- **Vangnet (`ZonePoort.tik`, elke 2 ticks, alleen als niet alles open is):** zone van je plek = de tegelvakken van
  `VadsrijkZones` (zone 4/5 op hoogte) of anders de vrije gebieden van `Barrieres`. In een dichte zone: binnen 12 blokken van je
  laatste open plek (onthouden elke 10 ticks als je op de grond/in water staat) word je teruggeduwd (max 3 s); verder weg, op
  een elytra, op een rijdier of zonder open plek word je teruggezet (van je rijdier af, elytra uit, geen valschade); zonder
  open plek naar De Haven (`WereldReizen.veiligeLanding(HAVEN_LANDING)`). Actiebalk + chat (max 1x per 30 s). Scènes vrij.
  Enderparel/elytra kunnen er dus niet langs: ze landen hooguit even binnen en staan 2 ticks later terug. De bestaande
  `Barrieres` (water/wind tussen de zones) blijft er gewoon onder liggen.
- **Zachte cap (`ZonePoort.knip`, gekozen):** poort op N < 6 => generiek en klasse-level stoppen op het startlevel van zone N+1
  (20/30/50/56/60). Het teveel gaat in een spaarpot (max 3 levels xp, in `SpelerData.extra("zonepoort")`, dus per karakter
  en mee bewaard zonder save-wijziging). Zonder cap betaalt de pot uit als +100 % op elke xp-winst tot hij leeg is: geen sprong
  ineens, geen misbruik (pot is begrensd). HUD-balk vol op de cap (`KernPayloads.stuurHud`), Karakter-menu "Hoogste level voor
  nu ... Gespaarde xp: N" (`MenuServer.karakter` + `KarakterPaneel`). Eén chatregel per plafond.
  Waarom gespaard i.p.v. "laat maar lopen": met poort 1 zouden hoofd+zij-spelers anders op ~26-28 zone 2 (band 20-30) in lopen.

## 2. XP-balans (zie XP_BALANS.md voor de tabellen)
- Alleen quest-xp, per generator een blok `X_ZONE1_XP` (id -> xp) dat `quests_json()` toepast; de 6 quest-JSON's zijn
  opnieuw geschreven met alleen die xp-velden (diff gecontroleerd: 134 regels, alleen `"xp"`). Lang niet geraakt door die run.
- Zone 1 alleen-hoofd: 0 min grinden (5 min zonder extra kills), was 71-91. Hoofd+zij eindigt op ~26-28: met poort 1 dus
  netjes op het plafond 20, het teveel gespaard.
- Zone 2 omlaag (hoofd 20 -> ~32 i.p.v. 41), zone 3 omhoog (30 -> 50 nu echt in zone 3), zones 4-6 opnieuw verdeeld,
  zijquests overal klein (~0,05-0,1 level; zone 1 x0,35).
- Hoofd+zij: cap 70 nu bij z6_hq06/hq07 (2/0 kills onderweg) i.p.v. al in zone 5. **Niet gehaald:** "elke zone eindigt rond
  het startlevel van de volgende" voor hoofd+zij lukt niet helemaal: B loopt in zone 1 ~5 en in zones 4-5 ~2-4 levels voor door
  de kills/bazen van zijcontent (zelfs met 0 xp voor zijquests). Verder omlaag vraagt minder mob-/baas-xp: niet gedaan (open punt).
- `tools/vadsrijk/xpbalans.py`: + `cap_bij` (na welke quest 70 valt) en de samenvatting "keten bij 0/2/5 kills onderweg".
- Levelpoorten, curve, cap, mob-xp, bazen, kerkers: niet aangeraakt. De questrobot leest geen xp; `QlStappen` (Questlopen)
  gebruikt alleen de `lv`-poorten: blijft geldig.

## 3. Zwamgrot met alleen zone 1
- De Zwamgrot is zone 1: geen enkele poortregel raakt hem. Gametest `VadsrijkZwamgrotPoortGameTests` draait wachtrij +
  nep-spelers vullen + klaar-check + naar binnen + cooldown + solo met poort 1 streng. Loot/kist: ongewijzigd (bestaande
  `VadsrijkGrotKerker`-tests). Let op: kerker-xp valt onder het plafond (lv 20) zoals alle xp.

## Gedeelde bestanden (allemaal gemarkeerd `X-ZONE1`)
- `VadsrijkFeature.java` (+1 regel register), `luchtschip/Poorten.java` (dicht/uitleg/kort), `npc/QuestMotor.java` (reisSlot +
  klik stap 5c), `npc/Reiskaart.java` (plek), `npc/Questlog.java` (beschikbaar), `npc/QuestStap.java` (hoofdlijn),
  `dungeon/DungeonRij.java` (weiger), `kern/Spelers.java` (geefXp: knip generiek + klasse), `kern/KernPayloads.java` (HUD-balk),
  `menu/MenuServer.java` + `menu/client/KarakterPaneel.java` (max-tooltip).
- Generators: `tools/features/vadsrijk_npc.py`, `vadsrijk_z2_npc.py`, `vadsrijk_z3_npc.py`, `vadsrijk_t_z4verhaal.py`,
  `vadsrijk_t_z5verhaal.py`, `vadsrijk_u_z6verhaal.py` (blok `X_ZONE1_XP` + 3 regels in `quests_json`); de 6 `quests*.json`.
  Let op integratie: W-BALANS' `Z4_HOOFD/Z6_HOOFD` blijven bestaan maar mijn blok overschrijft hun hoofd/zij-xp.
- `tools/features/__init__.py` (+`vadsrijk_x_zone1`), lang nl/en (+27 sleutels `vadsrijk.zonepoort.*`), `tools/lang/chunks.json`
  (+chunk `c417_vr_zonepoort`), `tools/vadsrijk/engels_bron.json` (`--merk`), `tools/vadsrijk/xpbalans.py`, `XP_BALANS.md`.
- Nieuw: `zonepoort/ZonePoort*.java`, `zonepoort/VadsrijkZonePoortGameTests.java`, `grotkerker/VadsrijkZwamgrotPoortGameTests.java`,
  `tools/features/vadsrijk_x_zone1.py`, `tools/lang/en/c417_vr_zonepoort.json`.
- Geen kaart/tiles/POI's geraakt (geen regen nodig).

## Tests
- compileJava OK.
- `runGameTestServer -Pgt=VadsrijkZonePoort,VadsrijkZwamgrotPoort,VadsrijkLuchtschip,VadsrijkSchepen,VadsrijkNpc,VadsrijkDungeon,
  VadsrijkDungeonNep,VadsrijkMenu,VadsrijkKern,VadsrijkBalans,VadsrijkParty,VadsrijkQuestLopen,Taalsleutels`: **59/59** (log
  `../x-zone1-gt2.log`). Nieuw: schipEnReisDicht, eindeZone1, dungeonsDicht, vangnet, zachtPlafond, robotZone1Poort1,
  robotAlleZonesPoort6, zwamgrotMetAlleenZone1.
- Robot poort 1: zone-1 hoofd 13/13, overige 26/26 (de 2 rijdier-terugkeerquests wachten bewust op je eerste rijdier = de
  zone-3-starter: met poort 1 dus niet te doen, de NPC toont de bestaande teaser). Robot poort 6: hoofd z1-z6 13/12/15/7/10/10, 0 vast.
- Python: taalcheck 0 ontbrekend, engelscheck OK (+`--merk`). Kaartcheckers (bereikcheck, schepen_check, kijkcheck, verbindcheck)
  niet gedraaid: geen kaart/tiles/POI geraakt.

## Open punten
- Rijdier-terugkeerquests van zone 1 (hoedhupper, knuffelhert) zijn met poort 1 niet te halen (eerste rijdier = zone 3).
- Wiki/website: de site-generator (`web/`, u-site) toont nog alle zones; "binnenkort" voor zone 2+ op de wiki is niet gedaan
  (de site is een aparte pijplijn; bij publiceren met poort 1 de zone 2-6-pagina's verbergen of een banner zetten).
- De HUD heeft geen wereldkaart met zones; de enige kaart (de reiskaart) toont dichte zones grijs "Binnenkort!".
- Hoofd+zij loopt nog voor (zie 2). Knoppen als de gebruiker het strakker wil: zijcontent-kills/optionele wereldbazen minder xp.
- Niet in een client gezien (geen client-runs deze ronde): de "Wordt vervolgd"-regel in de tracker, de menu-tooltip.
