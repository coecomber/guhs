# NOTITIES_I_MOUNTS (Vadsrijk fix round 3, agent I-MOUNTS, branch 110-i-mounts)

The user: mounts work very well. This round: two mounts out, new speeds/jumps, a 2-block step.

## What changed
- **Hoedhupper + Klodderros removed completely** (enum values, entity types, saddle items, geo/animation/textures, item models,
  icons, texts, the "ren" add-on in vadsrijk_rijdieren.py, the generator functions in vadsrijk_economie.py, Koning Klodder's
  Klodderzadel drop in `BazenLoot` (his rare mount loot stays the sterrennacht Pluimguh egg), autocheck scripts).
  - **Old saves:** `rijdieren.hoedhupper/klodderros` in `SpelerData.extra("economie")` and `actief = "hoedhupper"` are ignored
    (every lookup goes through `RijdierSoort.values()/van`); a player who only had one of them has no mount until the next one.
    Leftover saddle items vanish on load (unknown registry id); whistled mounts are never saved, so no entity problem.
- **The lv-8 quest** (`rijdier_hoedhupper`, Stalmeester Hoefje): **choice: it now gives the Wolkenschaapje** ("Een eigen
  Wolkenschaapje"). Kept: the quest id (saved progress + advancement `guhs:quest/vr_rijdier_hoedhupper` + FTB quest), the POI id
  `hoedhupperwei` (map) - in texts the meadow is now the "Schaapjeswei". Reason: zone 1 keeps a first mount at lv 8, which is also
  the gate of the Knuffelhert return quest (lv 10, "owns any mount"). If you already bought the Wolkenschaapje at the stal, the
  quest pays its price back (`Rijdieren.questBeloning`, npc role "rijdier"; text `vadsrijk.economie.rijdier.al_schaapje`).
  `Rijdieren.geefEerste` = the Wolkenschaapje now. The Wolkenschaapje stays for sale at the stal (12z).
- **Speeds** (`RijdierSoort`, 0.1 = player walking speed, level 1): Pluimguh 0.35 (350 %), Knuffelhert 0.32 (320 %), Guhwolf 0.37
  (370 %), Wolkenschaapje 0.30 (300 %), Slakkie 0.085 (85 % = half of its old 170 %).
  **Jumps** (upward velocity -> height): Knuffelhert 0.92 (~5.1 blocks, highest; was 0.88), Pluimguh 0.72 (~3.3), Wolkenschaapje
  0.62 (~2.5 + double jump), Guhwolf 0.60 (~2.4, smaller; was 0.74), Slakkie 0.45 (~1.4). Faster = lower jump.
  **Levelling** scaled down: speed +1.5 % per level (max +13.5 % at lv 10; was +3 %/+27 %), jump +0.01 per level (~+0.6 blocks
  at max; was +0.025).
- **Balans config:** section `[rijdieren]` in `config/guhs-vadsrijk-balans.toml` (KlasseBalans, live reload, also
  `/guhs vadsrijk balans rijdieren <sleutel> <x>`): `snelheid` (x, 1.0), `sprong` (x, 1.0), `groei_snelheid` (0.015),
  `groei_sprong` (0.01). Pushed to `RijdierSoort.Balans`. The mount's effective speed/jump are synced to the client via entity
  data (`RijdierEntity.DATA_SNELHEID/DATA_SPRONG`), because the riding client moves the mount.
- **Step height 2.0** (`RijdierSoort.STAP`, attribute STEP_HEIGHT on every mount): walks up a 2-block ledge, stairs and slabs
  without jumping; 2.5 blocks (2 blocks + a slab) or more still needs a jump. Side effect: mounts also step over fences/walls
  (1.5 high) - tell the user if that's unwanted.
- **Mount screen:** jump text now uses the real height (`RijdierSoort.springHoogte`), speed bar scale 0.45. Trait texts updated
  (Guhwolf = fastest/low jump, Knuffelhert = highest jump, Pluimguh no longer "the fastest").
- Docs: TESTEN_VADSRIJK.md, SPIEKBRIEF_RONDE2.md, TESTEN_VADSRIJK_ZONE1.md.

## Outside my slice (small, needed)
- `klassen/KlasseBalans.java` + `KlassenRegistry.java` (the `[rijdieren]` section + command suggestions).
- `bazen/BazenLoot.java` (Klodderzadel drop removed).
- `npc/NpcRollen.java` (role "rijdier" -> `Rijdieren.questBeloning`), `npc/QuestMotor.java` / `NpcApi.java` (default arg/javadoc),
  `tools/features/vadsrijk_npc.py` + generated `quests.json` + lang + FTB lang (quest texts).

## Tests
- Gametests `VadsrijkRijdieren` (new: `rooster` = roster of 5, old saves ignored, items/entities gone, quest reward/refund;
  `snelheden` = the % values, jump order, max-level growth, the balans knob; `stapHoogte` = attribute 2.0, walks up a 2-block
  ledge, not a 2.5 wall) + `VadsrijkEconomie` (rijdierXp on the Wolkenschaapje).
- Results: see the end of this file.

## Test results (2026-11-04)
- `runGameTestServer -Pgt=VadsrijkRijdieren,VadsrijkEconomie,VadsrijkWerver`: all 13 passed (incl. rooster, snelheden, stapHoogte;
  Werver = the return-quest gate still works with the new first mount).
- Note for gametests: the `empty` structure has a barrier ceiling at relative y 4 (stapHoogte clears it).
- No AutoCheck client run this round (logic/text changes only; the user judges the feel in the dev client).

## Resume point
Done and committed on 110-i-mounts. Open: user feedback on the numbers (all tunable live in `[rijdieren]`), and whether mounts
stepping over fences (1.5 high) is OK.
