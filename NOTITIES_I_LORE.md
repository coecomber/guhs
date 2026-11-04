# I-LORE: the 15 Keizer references in zones 1 and 2 (WERELDVERHAAL §10)

Branch `110-i-lore` (from ab6d952ac). All 15 were approved by the user. Kept subtle: only the boormeester says "Keizer", once.

| # | What | Where it landed |
|---|---|---|
| 1 | Oma Truus, 4th idle line ("tweede maan... staren") | `vadsrijk_npc.py` NPCS `oma_truus` -> `vadsrijk.npc.praat.oma_truus.4` |
| 2 | Grootmeester: "kruimeldieven, geen drakentemmers... Wie eet er zoveel?" | `vadsrijk_npc.py` `hq12_schaduw` klaar, new line 2 (before "Waar is de draak nu?") |
| 3 | Uitkijkpost note "Aan de Pink: deur zit op slot. Miauw. A." | `vadsrijk_npc.py` `AANWIJZINGEN["uitkijkpost"]` |
| 4 | Mika-speldje tooltip (handje met pinkje) | `vadsrijk_npc.py` `QUEST_ITEM_LORE` -> `item.guhs.vr_npc_mika_speldje.lore`; `NpcRegistry.metLore` puts an ItemLore on the item |
| 5 | Professor Pluis: pootjes lopen allemaal dezelfde kant op | `vadsrijk_npc.py` `z08_pluis` klaar, extra last line |
| 6 | Akim, 4th idle line ("baas... die noemen we liever niet") | `vadsrijk_npc.py` NPCS `akim_kaasguh` -> `.praat.akim_kaasguh.4` |
| 7 | Weerguh Wolkje: grijzigheid uit een richting waar de draak nooit vliegt | `vadsrijk_npc.py` `z15_lichtjes` klaar, extra last line |
| 8 | Drakensplinter tooltip (nagelknipsel) | `vadsrijk_z2_npc.py` `QUEST_ITEM_LORE` -> `item.guhs.vr_z2_npc_drakensplinter.lore`; `Z2NpcRegistry` via `NpcRegistry.metLore` |
| 9 | Poollicht Pien, 4th idle line (sterrenbeeld met lange armen) | `vadsrijk_z2_npc.py` NPCS `z2_poollicht` -> `.praat.z2_poollicht.4` |
| 10 | Kristalijn: plaatje van een kat, bladzijde gescheurd | `vadsrijk_z2_npc.py` `z2_hq12_hartsplinter` klaar, new line 2 (after "oudste boeken") |
| 11 | Boormeester slip: "De Keizer wacht niet gr... VERGEET DAT IK DAT ZEI!" | `vadsrijk_z2_bazen.py` `vadsrijk.z2bazen.bm.cliff2`; `Boormeester.java` sends it right after `bm.cliff` on the mine-cart flight |
| 12 | Opa Sneeuwbal, 4th idle line (magere guh, winter van '32) | `vadsrijk_z2_npc.py` NPCS `z2_opa_sneeuwbal` -> `.praat.z2_opa_sneeuwbal.4` |
| 13 | "Gestempeld: EIGENDOM VAN NOOITGENOEG" on every Mika-buitset piece | `vadsrijk_z2_wezens.py` `vadsrijk.z2wezens.set.mika.stempel`; `Z2WezensClient.setRegels` adds it under the set lines (dark grey italic) |
| 14 | z2 flashback: 1 s thin grey line with fingers on the far horizon | `WereldLucht` section 6 (`vingers()`): while scene `npc:z2_flashback` is at tick 228-248, drawn at the azimuth of the scene's last camera keyframe |
| 15 | Zone-1 cliffhanger: last shot, the second moon blinks once | `Verhaal.cliffhanger`: scene 260 -> 312 ticks, after the title the camera tilts up 32 deg (`MAAN_HOOGTE`); `WereldLucht` hides the orbiting second moon then and draws it in the look direction from tick 262, one blink (vertical squash) at 292-298 |

## Mechanics added
- **4th idle lines:** `NpcDef.VIER_REGELS` + `NpcDef.gedagKey(id, random)` (used by `QuestMotor.rol` and `Z2Haken`). The generators have
  a mirrored `VIER_REGELS` set and assert 3 or 4 lines. Keep both in step.
- **Quest item lore:** `NpcRegistry.metLore(props, id, prefix, ids...)` adds `DataComponents.LORE` (grey italic, translatable
  `item.guhs.<prefix><id>.lore`). Generators: `QUEST_ITEM_LORE`.
- **`SceneSpeler.huidig()` / `tijd()`** (kern slice, 2 tiny public getters) so the sky can add a moment to a shot.

## Zone-2 cliffhanger now points EAST
- Texts: `z2_hq12_hartsplinter` "Kijk eens naar het oosten", `vadsrijk.z2npc.scene.einde.onder` "Ver in het oosten".
- Camera: `Z2NpcPlekken.CLIFFHANGER` mirrored in x (target x +900/+1000 instead of -900/-1000).
- Also `z2_s10_poollicht` (west -> east), but that side quest is currently dropped by `ZIJ_HOUDEN`, so no lang change.
- **For the integrator:** the zone-3 npc slice does the same; take whichever, they should be identical in intent. NOT changed here:
  the giant-tree landmark azimuth in `WereldLucht.VERTE_PLEK` (still 255 = west; WERELDVERHAAL §12 q1 suggests ~95) and the zone-3
  box itself: that belongs to the zone-3/world slices.

## Not done / open
- No client check of #14/#15 (visual, 1 s each): the user judges looks. Tunables are constants at the top of `WereldLucht`
  (OOG_VAN, OOG_KNIPPER, VINGERS_VAN, sizes in `vingers()`), and `Verhaal.MAAN_HOOGTE`.
- Lang was regenerated (some keys moved position in the json, same content). The wiki generators were not re-run.
