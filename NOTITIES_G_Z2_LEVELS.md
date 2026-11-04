# NOTITIES_G_Z2_LEVELS: zone 2 = lv 20-30, fewer side quests (g-z2, coordinator request 2, 2026-11-04)

## Levels (zone 2 becomes lv 20-30; zone 3 = lv 30-50 is another team's)
| where | old | new |
|---|---|---|
| plan.py GEBIEDEN (parent) | galm 20-25, meer 25-30, kloof 30-34, burcht 33-35, town/ring 20-35 | 20-23, 23-26, 26-29, 28-30, 20-30 |
| wezens `Z2WezensPlekken` ZONE / GALMHELLINGEN / KNUFFELMEER / SPLINTERKLOOF levels | 20-35 / 20-25 / 25-30 / 30-34 | 20-30 / 20-23 / 23-26 / 26-29 (boxes untouched: parent rewrites them) |
| wezens `Z2WezensSpawners` kloof-floor spawners | 31-33 | 27-29 |
| wezens `Z2WezensSpawners` Spiegelburcht room spawners | 33-35 | 28-30 |
| bazen Galmwolf (`Z2BazenRegistry` arena spawn) | 24 | 23 |
| bazen Vrouwe Spiegelzucht (`Z2BazenRegistry` arena spawn) | 33 | 30 |
| bazen Mika-boormeester (`Boormeester.java`) | 32 | 28 |
| bazen Spiegelgang gauntlet mobs (`Spiegelburcht.java` stelIn) | 31 | 29 |
| bazen dev `loot`/`items` commands | 32 | 30 |
| bazen gametests (Spiegelzucht/Boormeester stelIn, kist/maak level) | 33/32 | 30/28 |
| quests (vadsrijk_z2_npc.py, min level to accept) hq06..hq12 | 25,26,28,30,31,33,33 | 24,25,26,27,28,29,30 |
| quest z2_geheim_boormeester | 30 | 28 |
| `npc/QuestStap.levelPlek` (where to level to n) | galm <=25, meer <=30 | galm <=23, meer <=26, else kloof |
| texts: Gids page, wiki page, `vadsrijk.wereld.z2.luchtschip.onder`, `vadsrijk.wereld.deur.onder.spiegelburcht` | lv 20-35 / 33-35 | lv 20-30 / 28-30 |

- Gear: item level = the level of the mob/boss that drops it (Rollen.rol(level)), so the zone-2 gear/weapon bands follow the mob levels
  above automatically (boss named items now roll at 23 / 28 / 30). Only the comments in Z2Wapens/Z2Loot/mob classes said "20-35".
- Boss HP/damage scale with level (basisVads/basisSchade), so the bosses get a bit weaker with the lower level: intended (players
  arrive lower too).
- **Not changed (open choice):** kern `Levels.MAX = 35` (zone-2 systemen test asserts it; zone 3 will raise the cap to 50 anyway),
  skill-tree rows open at lv 20/26 (inside 20-30), the job advancement stays at lv 30 (quest objective "level 30").
- Parent: `bouw2.py` MOBZONES (levels 20-35 there) are yours to rescale with the new boxes.

## Quests: main line + 5 short side quests
- Kept: `z2_s03_knuffels` (10 plushies under the ice), `z2_s04_schaatsen` (skating lesson), `z2_s05_echokoor` (3 singing needles,
  Galm), `z2_s13_ijsbloemen` (ice flowers for Mallow's chocomelk), `z2_s23_gondel` ("Naar de top!").
- Removed (18): s01 wanten, s02 kandijkoekjes, s06 post, s07 sneeuwpop, s08 kas, s09 winterslaap, s10 poollicht, s11 tandwielen,
  s12 kandijfeest, s14-16 premies, s17 model, s18 lawine, s19 ijsvis, s20 bruggen, s21 truien, s22 edelsteen.
  Kept the class job advancement (z2_adv30_*), the secret z2_geheim_boormeester, all SCHAT/AANWIJZING/knuffel secrets.
- How: `tools/features/vadsrijk_z2_npc.py`: `ZIJ_HOUDEN` + a filter on QUESTS after the side-quest definitions (the 18 definitions
  stay in the file for reuse but are not written); the quests_json assert now wants exactly the kept side quests.
  Regenerated standalone (`PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_npc.py`): quests_z2.json (21 quests:
  12 main + 5 side + 3 class + 1 secret), lang nl/en (their texts dropped), 18 advancements `vr_z2_s*` deleted.
- FTB: `tools/make_ftbquests.py` CHAPTER_VERSION 58 -> 59 (so packs get the shrunk chapter), regenerated: guhs_vadsrijk chapter +
  lang lose the 18 quests; every other chapter only its version stamp.
- Wiki text (`vadsrijk_z2_npc_wiki.py` "Zijquests") now names the five.
- `Z2NpcGameTests`: side-quest count check `>= 20` -> `>= 5`.
- The citizen NPCs of removed quests (Opa Sneeuwbal, Bakker, Postguh, ...) stay as town life with their idle lines. Their quest
  items stay registered (no drops without the quest).

## Verify at build time
- compileJava; gametests `-Pgt=Z2Npc,VadsrijkZ2Bazen,VadsrijkZ2Wezens,VadsrijkZ2Systemen` (Bazen tests use the new stelIn levels).
- `vadsrijk_z2_wereld.py` was re-run for its 2 lang strings (it also rewrites its sky textures identically).
