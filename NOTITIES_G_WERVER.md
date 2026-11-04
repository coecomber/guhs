# NOTITIES_G_WERVER: Vadsrijk fix round 2, agent g-werver

Branch `110-g-werver`, worktree `guhs_vadsrijk/g-werver`, from fba766b79, with `110-g-ux` and `110-g-mounts` merged in
(their only conflict: the end of nl_nl/en_us.json, both sides kept).

## 1. Werver Wimpel, the recruiter at the Guhmension portal temple
- `npc/Werver.java`. NPC id `werver`, name "Werver Wimpel": a RijkGuh (snow guh, knight helmet + armour + royal cape, size 1.05).
  He is NOT in `Npcs.ALLE` (that keeper only runs in the Vadsrijk). He has his own keeper instead.
- **Lazy spawn:** every 2 s, for each player outside the Vadsrijk, the code looks for a `guhs:vr_w_portaaltempel` start at the
  player's position and at ±40 blocks (loaded chunks only, `structureManager().getStructureAt`). The temple's portal is found
  by scanning its bounding box for `PortaalBlok` once per temple, and the result is cached. Wimpel then stands 4 blocks in
  front of the portal, on the side towards the box's middle (where the stairs are), 3 blocks to the side, looking outward,
  via `Npcs.standplek`. Like every RijkGuh he is never saved, never hurt and never pushed, so he comes back on the next
  approach after a chunk unload. Old ruins on the live server get him too, and no structure/template change was needed.
- **Gate** (`Werver.magDoor`, called first in `Portalen.aanraking`): a player who has not talked to him gets the overlay
  `vadsrijk.npc.werver.eerst_praten`, Wimpel waves, and the portal stays asleep. The gate applies to a SLEEPING portal, and
  to an awake portal that lies inside a temple's pieces (woken by someone else). A self-built portal is never gated.
  Players who were already inside before this update (`ONTWAAKT` / `BINNEN_GEWEEST`) count as having talked.
- **Talk:** `QuestMotor.KLIK_VOOR` hook. Eight click-to-continue pages (`vadsrijk.npc.werver.gesprek.0..7`, speakers in
  `Werver.GESPREK`): he recruits you against the Vadsloosheid, makes the Wynn joke (moved here from Sergeant Pluizebol, whose
  hq01 offer is back to 4 simple pages), and gives the view distance advice (32 chunks, at least 20; the hand-built Vadsrijk
  without heavy worldgen can take a higher distance than the Guhmension). After reading, the action `werver_klaar` sets the
  `gezien` flag `vr_npc_werver_gesproken`. Talking again gives one short page (`werver.weer`).
- **View distance also shows as:**
  - the first-time tip `vadsrijk.npc.tip.weergave`, at the end of the opening scene, or on the next arrival for players who
    already did the opening;
  - the Avonturiersgids page `weergave` (third in `QuestlogScherm.GIDS`).
- `QuestMotor.open` is package-private now (it was private).
- Dev commands:
  - `/guhs vadsrijk npc werver` puts a Wimpel at your feet in any dimension;
  - `/guhs vadsrijk npc werver vergeet` forgets the talk;
  - `/guhs vadsrijk wereld vergeetportaal` now also forgets the talk.

## 2. Mount return quests (`npc/RijdierQuests.java`)
- **Gate:** a `na` entry `@rijdier` (`RijdierQuests.POORT`) in `Questlog.beschikbaar` means `Rijdieren.heeftEen(p)`. The
  generators accept it (zone 1: `RIJDIER_POORT`).
- **Tracker:** `NpcVolg.bouw` adds the first return quest that is active or available (`RijdierQuests.trackerQuest`).
- **Reward:** the action `rijdier_geef:<soort>` calls `Rijdieren.geef(p, soort)`.
- **"Het Knuffelhert"** (`rijdier_knuffelhert`, zone 1, soort rijdier, giver and hand-in Boswachter Mos, lv 10, after hq06 +
  owning a mount):
  - three lanterns: GEBIED poi `hert_lantaarn_1..3` = WereldPlekken `lichtstralen.3 / .1 / .2`;
  - then the glade: MELD `hert:open_plek`, plek `poi:hert_open_plek` = `lichtstralen.4`.
  - Moments: each lantern lights up (END_ROD + flame, a chime, "Lantaarn aangestoken! (n/3)"). After the third, narrator pages
    and a glowing trail of hoof prints that only you see, from you towards the glade. At the glade the deer appears in
    narrator pages, with hearts. Mos gives the Knuffelhert.
  - Zone-1 objectives can carry a `plek` now (D(..., plek=...)); before this, only zone 2 could.
- **"De verdwaalde Guhwolf"** (`z2_rijdier_guhwolf`, zone 2, giver and hand-in Breister Wolwinter in Kandijkom, lv 22, after
  z2_hq02 + owning a mount):
  - GEBIED `poi:z2_wolfje` (`Z2NpcPlekken`: `w("kloof_trap_2", -109, 65, -2766)`, the foot of the 2nd stairway on the ravine
    floor; a POI id, so the g-z2 rebuild only has to keep `kloof_trap_2`). Finding the pup plays 3 narrator pages with
    snowflakes and a whine.
  - OPVROLIJK 5 in `gebied:z2_splinterkloof`.
  - The hand-in ("...three scarves later... he grew up!") gives the Guhwolf.
- All the moment texts (`vadsrijk.npc.rijdierquest.*`) are in vadsrijk_npc.py OVERIG, because zone 1's `_direct` would delete
  them otherwise.
- FTB: the quests are in the generators' sections:
  - Knuffelhert under "Klassen en rijdieren";
  - Guhwolf under "Kandijkom helpen" (soort rijdier now included, saddle icon).
  - `tools/make_ftbquests.py` was run, and only the guhs_vadsrijk chapter + `tools/lang/source_ftb.json` are committed.
    **CHAPTER_VERSION was NOT bumped (58):** integration bumps it once.

## Tests
- `VadsrijkWerverGameTests` (`-Pgt=VadsrijkWerver`):
  - `portaalWachtOpWerver`: sleeping portal + no talk = no wake; a self-built one is open; Wimpel spawns in the test level
    (not the Vadsrijk); the click + reading sets the flag; the spot math.
  - `rijdierQuestsNaEenRijdier`: no mount = neither quest; with a mount both are available + tracked with a target; the
    reward actions give Knuffelhert + Guhwolf.
- Results: see the resume point.
- Run `-Pgt=VadsrijkWerver,VadsrijkNpc`: 8/9 passed. BOTH of my tests passed. The 1 failure is g-ux's `reisKeuzes`, and it is flaky: it depends on where the test is placed. The test sat at z = -13,129,832, so `QuestStap.zone2(p.getZ())` was true, `Reiskaart.hier` = kandijkom, and Kandijkom is HIER instead of OPEN, so there is no flight. That is unrelated to this branch. Fix for the npc owner: make the test position the mock player inside zone 1 (or compare against HIER too).

## Open
- The structure template itself is unchanged (lazy spawn covers new and old temples). If the user wants him in the template
  as well, that is a template/wereld change.
- The deer and the pup are narrator pages, not entities that walk. A walking deer (the mount entity without a rider) would be
  a follow-up.
- `kloof_trap_2` must keep existing after the zone-2 redesign (otherwise the fallback coordinates are used).
- en_us = the same Dutch (rule).

## Resume point
- Everything is committed on 110-g-werver. AutoCheck (Wimpel in a Guhmension test ruin) was not run (optional). To check by hand: `/locate structure guhs:vr_w_portaaltempel` in the Guhmension, walk up and see Wimpel by the portal; the portal says 'praat eerst met Werver Wimpel'; the talk, then the portal wakes.
- Wolf sound: `SoundEvents.WOLF_WHINE` does not exist in 26.1 (wolf sounds are variant-based), so FOX_SNIFF is used.
