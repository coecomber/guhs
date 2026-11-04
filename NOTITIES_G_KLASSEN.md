# NOTITIES_G_KLASSEN (fixronde 2, branch 110-g-klassen, vanaf fba766b79)

Doel: het klassensysteem volgens de tweede playtest. Je bent altijd alle 3 de klassen; de klasse is het wapen in je hand; twee
niveaus (generiek + klasse); balansconfig per klasse.

## Wat er veranderd is
**Kern (API-wijzigingen, andere slices letten op):**
- `SpelerData`: `generiekXp` (opgeslagen als `gxp`), `level()` = **generiek** (was: de huidige klasse), `klasseLevel()`,
  `level(klasse)` = klasse. `klasse` = de ACTIEVE klasse (laatst mee aangevallen). Migratie in `deserialize`: geen `gxp` ->
  `migreerGeneriek` = de **hoogste** klasse-xp (niet de som: twee klassen op 15 mogen geen generiek 22 geven), gecapt; vlag
  `gemigreerd` (transient).
- `Levels`: generieke curve (`xpVoorGeneriek`, `totaalVoorGeneriek`, `generiekVan`, `restXpGeneriek`, `MAX_GENERIEK` 35) en
  schalen `SCHAAL_GENERIEK` / `SCHAAL_KLASSE` (uit de balansconfig).
- `Spelers`:
  - `level(p)` = generiek; `level(p, klasse)` / `klasseLevel(p)` = klasse.
  - Wapens: `wapenKlasse(BasisType)` (eigen klasse, anders op soort), `wapenInHand`, `magWapen` (klasseniveau van het wapen),
    `wapen(p)` = een klassewapen van ELKE klasse dat je mag gebruiken (was: alleen van de huidige klasse), `actieveWapenKlasse`.
  - `voorAanval(p)`: elke aanval vraagt dit eerst (VadsrijkMob.hurtServer, Basisaanval, Vaardigheden.gebruik). Niet-gehaald wapen:
    rode melding + false. Wapen van een andere klasse: `wisselKlasse` (Vads-% blijft, R/V/G op volle cooldown, ult 0, sprong niet).
  - `stats`: Vads + Verdediging uit `Klasse.basis(generiek)`, de rest uit `basis(klasseniveau)`; harnas/sieraden eisen generiek;
    nieuwe haak `STATS_NA` (voor de statussen). `maxEnergie` en energie-regen per klasseniveau.
  - Xp: `geefXp(p, n, bron)` -> alles naar generiek EN naar `xpKlasse(p)`; `geefXp(p, n, bron, klasse)`. Bij een verslagen mob
    (`metKlasseBijdrage`, gezet door `VadsrijkMob.verslagen` rond `beloon`) krijgt de klasse met de meeste schade op die mob de
    klasse-xp (`VadsrijkMob.klasseBijdrage`, per speler per actieve klasse). Partyleden die niet raakten: hun actieve klasse.
  - `zetLevel` = generiek + actieve klasse (oude tests/dev blijven werken); `zetGeneriek`, `zetKlasseLevel`.
  - `Haken.LEVEL_OP` krijgt nu `"generiek:N"` en/of `"<klasse>:N"` (was altijd `"<klasse>:N"`).
  - `KLASSE_GEWISSELD` haak.
- `Vaardigheden.gebruik`: eerst `voorAanval`, zonder (bruikbaar) klassewapen: "geen_wapen".
- `KernPayloads.Hud`: extra velden achteraan (`generiek`, `gXpIn`, `gXpNodig`, `grijs`, `klasseLv`); `level/xpIn/xpNodig` =
  klasse. `KernPayloads.laatsteHud` (client) voor tooltips. Party-HUD gebruikt `Spelers.level` = generiek.
- `RijkHud`: alleen de niveauregels (Generiek nv X / <titel> nv Y, grijs zonder wapen) en de xp-balk (goud generiek, roze lijn klasse).
- `RijkItem` tooltip: "Vereist: <Klasse> nv X" (wapen) / "Vereist: Generiek nv X" (rest), groen/rood naar de laatste HUD.

**Klassen:**
- `KlasseBalans` + `config/guhs-vadsrijk-balans.toml` (zelf geparst, eerste keer geschreven, mtime-check elke 5 s): per klasse
  schade/skillschade/vads/verdediging/cooldowns/energie/melee_reductie/vads_per_klasselevel, `[algemeen]` xp-schalen. Haken:
  `Spelers.STATS_NA`, `Schade.VOOR_KLAP` (aanvaller x schade (x skillschade als skillId niet "basis*"/"kettingbolt"); verdediger
  met melee-wapen x (1 - melee_reductie)), `COOLDOWN_MOD`, `ENERGIE_MOD`. Commando `/guhs vadsrijk balans toon|herlaad|<k> <s> <w>`.
  Knuffelridder standaard: melee_reductie 0.20, vads_per_klasselevel 4.
- `KlasseBalie`: geen klassewissel meer. `kies` = startwapen kiezen (zet ook de eerste actieve klasse). Balie: tabs, respec per
  bekeken klasse, ceremonie per bekeken klasse, startwapens van de andere klassen (eerste gratis, daarna `WAPEN_PRIJS` 30 brons).
  API voor trainers/winkel: `KlasseBalie.geefStartwapen(p, klasse, prijs)`. `wissel` bestaat nog als dev/test-helper.
- `Advancement.kanDoen(p, klasse)` / `doe(p, klasse)` op klasseniveau; de oude 1-arg-versies = actieve klasse.
- `Boom.koop(p, klasse, id)`; `BoomScherm` met klasse-tabs (boven het paneel), balie-kolom met wapenknoppen.
- Lang: via `tools/features/vadsrijk_kern.py` en `vadsrijk_klassen.py` (tekst-dicts aangepast) + exact die sleutels in de lang-json gezet.

**Buiten mijn pakket (opdracht: "grep Spelers.level( en zet om"):**
- `npc/Questlog`: klassequests (job-advancement) op het KLASSEniveau van hun klasse en voor elke klasse beschikbaar (geen
  "huidige klasse"-check meer); LEVEL-doelen idem; `lvVoor(p, q)`. `npc/QuestStap` gebruikt `lvVoor`.
- `npc/NpcRollen` "advancement": doet de advancement van de klasse uit het argument ("klasse:stap").
- `zone2/systemen/Advancement3`: lv-30-melding op het klasseniveau ("<klasse>:30").
- Ongewijzigd want al goed met `Spelers.level` = generiek: Zwamtempel (generiek 16 + quest), Z2Verhaal/Z2Reizen (generiek 20),
  bazen/z2bazen `gedragen` (harnas = generiek), Kristallen (harnas generiek), loot-niveaus (generiek), BelevingCommando.

## Tests
`-Pgt=VadsrijkKlassen`: nieuw `klasseWisseltBijAanval`, `xpGaatNaarGeneriekEnKlasse`, `wapenVereistKlasseLevel`,
`balansConfigWerkt`, `opslagMigratie` (vervangen `klasseWisselenHoudtLevels`).

## Open / voor integratie
- Trainers / startwinkel (npc/economie): kunnen `KlasseBalie.geefStartwapen(p, klasse, prijs)` aanroepen.
- g-balans past `Schade.verdediging` aan; ik heb Schade.java niet aangeraakt (alles via VOOR_KLAP / STATS_NA).
- g-ui werkt aan RijkHud (skill-iconen) en tooltips: mijn RijkHud-wijziging is alleen het niveau-blok en de xp-balk.
- Het luchtschip/tempel blijven op hun eigen getallen (20 / 16 + quest); de opdracht noemde ook "lv 18" voor de tempel: niet
  veranderd (BazenPlekken.TEMPEL_MIN_LEVEL = 16 + quest is wat er nu staat).

## Resultaat (stand bij de commit)
- Gametests: `-Pgt=VadsrijkKlassen` 11/11 (incl. 5 nieuwe); `-Pgt=VadsrijkZ2Systemen,VadsrijkKern,VadsrijkNpc,VadsrijkBazenGevecht` 21/21.
- AutoCheck `tools/autocheck/vadsrijk_gklassen.txt` (run/screenshots/autocheck/gk_*.png, niet gecommit): HUD "Generiek nv 12 /
  Knuffelridder nv 9"; boog vasthouden = nog ridder; schieten = "Nu Pluisjager!", R/V/G op 6/8/7 s; terug naar zwaard midden in
  het gevecht; lege hand = grijze klasseregel + melding; lv-14-boog: rode melding + rode "Vereist: Pluisjager nv 14" tooltip;
  boomtabs; balie met wapenknoppen; balans toon/zet. (De tooltip-sectie draaide ik nog één keer kort, in survival: in creatief
  opent de inventaris het creatieve scherm; het script staat nu goed.)
- AutoCheck-tool: nieuw commando `gui inventaris [hotbarslot]` (survival-inventaris, muis op dat vakje) in `dev/AutoCheck.java`.
