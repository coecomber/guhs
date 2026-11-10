# NOTITIES Y-FTB (ronde Y, 2026-11-10): het compacte FTB-hoofdstuk "Het Vadsrijk"

Branch `110-y-ftb` vanaf 65cd32467 (110-x-int). Brief: `Y_BRIEF.md` + de opdracht van de slice.

## Wat er nu is

### Het hoofdstuk (14 quests + 5 poortquests)
Generator: `tools/features/vadsrijk_y_ftb.py` (`ftb()`, `FTB_SECTIES`, `FTB_DEPS`, `FTB_VELDEN`, `wiki_verborgen()`). Drie
secties met neutrale koppen (geen zonenamen op de plaatjes):

| sectie | quest (key) | taak (advancement `guhs:quest/...`) | wanneer | beloning |
|---|---|---|---|---|
| De Knuffelorde | Word rekruut van De Knuffelorde (`vr_ftb_rekruut`) | `vr_ftb_rekruut` | `hq05_klodderreus` ingeleverd (einde opleiding Knuffelkamp) | Rekruutvaantje + **Vadsrijk-vitrine** (`guhs:vr_vitrine`, 1x) |
| De Knuffelorde | Zone N is open! (`vr_ftb_poort2..6`, onzichtbaar tot ze af zijn) | `vr_ftb_poortN` | server heeft zone N open (`openZones` >= N) | geen |
| Het avontuur | Zone 1 uitgespeeld | `vr_ftb_zone1` | `hq12_schaduw` | Knuffelzwamlampje (licht 12) |
| Het avontuur | Zone 2 uitgespeeld | `vr_ftb_zone2` | `z2_hq12_hartsplinter` | Splinternaaldjes (licht 4) |
| Het avontuur | Zone 3 uitgespeeld | `vr_ftb_zone3` | `z3_hq15_hoogvads` | Schemerwortellantaarn (licht 13) |
| Het avontuur | Zone 4 uitgespeeld | `vr_ftb_zone4` | `z4_hq07_opstijgpier` | Lampionvliegertje (licht 13) |
| Het avontuur | Zone 5 uitgespeeld | `vr_ftb_zone5` | `z5_hq10_stormrand` | Hoogvads-roosvenstertje (licht 6) |
| Het avontuur | Zone 6 uitgespeeld | `vr_ftb_zone6` | `z6_hq10_moederoase` | Sterrentorentje (licht 10) |
| Het avontuur | De Sterrendovende Toren verslagen | `vr_ftb_raid` | eerste raid-clear (Z6RaidHaken.OPGEVROLIJKT, of de "clear"-vlag van de raid) | Sterrendovende trofee |
| Geheimpjes | Alle geheimpjes van zone N (N=1..6) | `vr_ftb_geheimenN` | alle "Geheimen" + "Verloren knuffels" + "Puzzels en geheimpjes" van de Verkenningsboek-pagina van zone N | Geheimpjesdoosje van zone N |

- **Vitrine (verzoek coördinator):** de rekruut-quest geeft naast het Rekruutvaantje ook `guhs:vr_vitrine` (1x), het
  trofeekastje van slice y-vitrine (110-y-vitrine 7f9f4c2a8). Dat item bestaat NIET op deze branch: in de FTB-data staat het al
  (`VITRINE` in vadsrijk_y_ftb.py); pas na de merge met y-vitrine is het een echt item. Integratie: na de merge in de client checken.
- Geen kaasknabbels, geen xp, geen spullen: elke beloning is precies een deco-blok. Ook het "Hoe kom je hier?" van dit hoofdstuk
  geeft niets meer (`intro_beloning=()`).
- Het oude hoofdstuk (204 quests) is weg: de `ftb()`-functies en `FTB_*`-tabellen van `vadsrijk_npc.py`, `vadsrijk_z2_npc.py`,
  `vadsrijk_z3_npc.py`, `vadsrijk_t_z4verhaal.py`, `vadsrijk_t_z5verhaal.py`, `vadsrijk_u_z6verhaal.py` zijn verwijderd (een regel
  `# Y-FTB` in hun plaats). Hun verborgen advancements `vr_<quest>` blijven (NpcFtb geeft ze nog; schaadt niet).
- `CHAPTER_VERSION` 63 -> 64, zodat werelden die het oude hoofdstuk al hadden (dev/test) het nieuwe krijgen. Live had 1.10 nooit.

### Elk karakter, elke modus
De taken zijn advancements van de Minecraft-speler, niet van het karakter. Welk karakter (Normaal/Knuffelmodus/Eénvads) of welke
gamemode het ook doet: één keer is genoeg en het blijft staan (ook een Eénvads-ridder met pensioen). `VrFtb.controleer` kijkt bij
elke inlevering (NpcApi.QUEST_KLAAR), bij de raid-clear, bij inloggen en elke 10 s naar het huidige karakter; wat al gedaan was (oude
saves, dev-commando's zonder hook) wordt zo ook afgevinkt.

### Zonepoort (keuze: verborgen, niet "binnenkort")
FTB Quests kan niet dynamisch op een server-config verbergen, maar wel op afhankelijkheden. Daarom:
- per zone 2..6 een kleine poortquest "Zone N is open!" met `invisible: true` (onzichtbaar tot hij af is); de taak `vr_ftb_poortN`
  geeft de Java aan iedereen zodra `ZonePoort.open() >= N` (login + elke 10 s; `/guhs vadsrijk zones open N` werkt dus meteen);
- "Zone N uitgespeeld", "Alle geheimpjes van zone N" (N>=2) en de raid (achter poort 6) hangen erachter met
  `hide_until_deps_complete: true` en `hide_dependency_lines: true`.
- Gevolg: bij gate 1 ziet een speler alleen rekruut, zone 1 en geheimpjes 1 (geen titels, geen iconen van later). Gaat een zone open,
  dan verschijnen "Zone N is open!" (afgevinkt) en de twee quests van die zone. Een verlaagde gate neemt niets terug.
- Wiki: `tools/wiki_site/gamedata.py` laat de quests van dichte zones weg (`vadsrijk_y_ftb.wiki_verborgen(GUHS_OPEN_ZONES)`,
  standaard 1). **Integratie/u-site**: zet `GUHS_OPEN_ZONES` gelijk aan de `--open-zones` van de wiki-build als die verhoogd wordt.
- De wiki gebruikte de zonekoppen van het oude hoofdstuk (`kop_vr_zwamzoet.png` enz.); die staan nu in
  `tools/wiki_site/vadsrijk/koppen/` en `tools/wiki_site/vadsrijk/pages.py` leest ze daar.

### Claimregel: nooit in het Vadsrijk
- `feature/vadsrijk/ftb/FtbClaimRegel` (geen FTB-types) + `mixin/FtbClaimMixin` en `mixin/FtbKeuzeClaimMixin` (@Pseudo, require 0).
- Met javap tegen ftb-quests-neoforge 26.1.2.8 nagelopen: elke claim eindigt in
  `TeamData.claimReward(ServerPlayer, Reward, boolean, long)`: claimknop (ClaimRewardMessage), "claim all" (ClaimAllRewardsMessage),
  auto-claim (TeamData.checkAutoCompletion), team-beloningen (per online lid, elk met zijn eigen dimensie) en change-progress.
  De enige uitzondering, een keuzebeloning (ClaimChoiceRewardMessage, lambda$handle$1), heeft een eigen mixin.
- In het Vadsrijk (`guhs:vadsrijk`): geweigerd vóór er iets gemarkeerd is, dus de beloning blijft gewoon claimbaar. Bericht (goud,
  max 1 per 2 s): "Beloningen claim je in je eigen wereld, niet in het Vadsrijk." Overal anders mag het, ook in het Knuffelkwartier
  (daar heb je je thuis-inventaris).
- Geldt voor ALLE FTB-beloningen van alle hoofdstukken (in het Vadsrijk draag je de Vadsrijk-tas; niets van buiten mag daarin).
- Auto-claim-beloningen die in het Vadsrijk vrijkomen: geweigerd, daarna gewoon met de knop te claimen.
- **Niet getest met FTB zelf**: in gametests is FTB Quests niet geladen. Getest is de beslissing (`weiger`). Integratie: in de
  client met FTB kijken of de claimknop in het Vadsrijk het bericht geeft en thuis werkt (zie SPIEK). Faalt de mixin stil (andere
  FTB-versie), dan mag claimen overal weer: in de log staat dan geen fout (require 0), dus dit echt in de client checken.

### De deco-blokken (14)
- Ids `guhs:vadsrijk_trofee_{rekruut,zone1..6,raid,geheim1..6}`: NIET `vr_` (een `vr_`-item telt als Vadsrijk-spul).
- `TrofeeBlock` (HorizontalDirectionalBlock: kijkt naar wie hem neerzet, sommige geven licht, niets te klikken, sterkte 0,5).
  Neerzetten in `guhs:vadsrijk` kan niet (getStateForPlacement = null + actiebalk "Dit pronkstuk is voor je eigen wereld...").
- Modellen: JSON-elementen met alleen vanilla-texturen (geen nieuwe plaatjes); roosvenster = translucent (gekleurd glas). Item =
  blokmodel. Tooltip (`BaltoBlocks.LoreBlock`): `block.guhs.<id>.lore` = "Beloning: Zone N van Het Vadsrijk uitgespeeld" enz.
- In de Guhs-creative-tab (`Features.creative`). Loot table: laat zichzelf vallen.
- Uiterlijk niet in een client bekeken (geen clientruns deze ronde): graag even in de dev-client (`/guhs vadsrijk ftb trofeeen`).

## Gedeelde bestanden (allemaal gemarkeerd `Y-FTB`)
- `tools/make_ftbquests.py`: CHAPTER_VERSION 64; intro guhs_vadsrijk (tekst + `intro_beloning=()`); `eigen_veld()` voor
  `FTB_DEPS` (in `gates`) en `FTB_VELDEN` (extra velden per quest). Alle andere hoofdstukken: alleen `guhs_chapter_version` 63->64
  en `index.txt` `version 64` (gediffd: verder byte-gelijk).
- `tools/features/__init__.py`: `vadsrijk_y_ftb` vóór `vadsrijk_t_vfx`.
- 6 zone-generators: `ftb()` + `FTB_*` weg (zie boven).
- `tools/wiki_site/gamedata.py` (dichte zones weg), `tools/wiki_site/vadsrijk/pages.py` (koppen uit `koppen/`).
- `src/main/java/.../feature/vadsrijk/VadsrijkFeature.java` (1 regel register), `feature/Features.java` (1 regel creative),
  `src/main/resources/guhs.mixins.json` (2 mixins), `gametest/GuhGameTests.java` (guhs_vadsrijk heeft nu 11 locks).
- `tools/lang/chunks.json` (c450_vr_y_ftb), `tools/lang/en/c414_vr_ftb.json` (629 oude sleutels weg, intro + 3 sectiekoppen Engels),
  nieuw `tools/lang/en/c450_vr_y_ftb.json`; `tools/vadsrijk/engelscheck_allow.json` ("Zone N is open!" = Engels), `engels_bron.json`
  (`--merk`).
- Gegenereerd: `src/main/resources/ftbquests/**` (alle hoofdstukken: versieregel), de plaatjes van guhs_vadsrijk (oude koppen weg,
  3 nieuwe), lang nl_nl/en_us, advancements `quest/vr_ftb_*`, modellen/blockstates/items/loot van de 14 blokken.

## Checks
- `python tools/make_ftbquests.py` (met de 1.21.1-client-jar in `build/moddev/artifacts/`), `python tools/lang/apply_en.py`,
  `engelscheck.py`: alles 0 / OK; `taalcheck.py`: 2.958 sleutels, 0 ontbreken.
- compileJava OK. Gametests: zie het eindrapport (`-Pgt=VadsrijkFtb,ftbquests`).

## Open punten
- In de client checken (integratie): claimknop/claim all in het Vadsrijk geweigerd + bericht, thuis wel; hoofdstuk bij gate 1 toont
  alleen rekruut/zone 1/geheimpjes 1; `zones open 2` laat zone 2 verschijnen; de blokken zien er goed uit.
- "Alle geheimpjes" telt Geheimen + Verloren knuffels + Puzzels; zones 4-6 hebben extra pagina-categorieën (flessenpost,
  uitzichten, uitkijk, kampvuren, grotten) die NIET meetellen. Wil de gebruiker die erbij, dan `VrFtb.GEHEIM_CATS` uitbreiden.
- De rekruut-quest vinkt af bij het einde van de opleiding (hq05_klodderreus), niet bij hq01 "Welkom, rekruut!".
