# NOTITIES_G_UX: Vadsrijk fix round 2 (second playtest), agent g-ux

Branch `110-g-ux`, worktree `guhs_vadsrijk/g-ux`, from fba766b79. Owner of: the npc package (dialogue, quests, tracker), the
zone-2 npc package, small fixes in the bazen temple-door gate.

## What changed

### 1. Airship NPCs: real travel choices (QuestMotor)
- `QuestMotor.REIZEN`: kapitein (Knuffelkamp) -> Zwamzoet, Kandijkom; luchtschipper (De Haven, Stuurguh Zeppie) -> Knuffelkamp,
  Kandijkom; z2_kapitein (Kandijkom, Kapitein Poolster) -> Zwamzoet (De Haven), Knuffelkamp.
- Kandijkom only appears once zone 1 is done; a locked line is still shown as "Naar X (nog op slot)" and choosing it makes the
  captain say why (`vadsrijk.npc.reis.slot.*`: hq05 not done / zone 1 not done / below lv 20).
- Choosing one flies through the same code as the bells: `Verhaal.vlieg` (-> `WereldReizen` binding) and the `z2_vlieg` action
  (-> `Z2Reizen` binding). Kandijkom also accepts z2_hq01 when available (as before).
- The choices appear in the role dialogue AND in the "how is it going" dialogue (the playtest bug: during hq06 the captain only
  said "kies 'Naar Zwamzoet'" with Verder/Doei), in talk-objective pages and after a hand-in without a story action.
- Every captain line mentions the bell (`vadsrijk.npc.reis.kies.<npc>`); hq06's "bezig" text too.
- Removed: Z2Haken's EXTRA "naar_kandijkom" option on the pilot and its z2_hq01 special case (both covered now).
- Other promised options checked: Zuster Zachtjes ("klik nog eens op mij als je de keuze wilt zien") now really opens the class
  choice while hq02 runs; shop/service NPCs (juwelier "klik op mij met een kristal", smid, ...) keep their service choice in the
  "how is it going" dialogue. Opa Sterrenstof's quiz and the Grootmeester's "praat nog eens met mij" already worked.

### 1b. The travel map (Reiskaart), coordinator request
- Every airship NPC opens a big illustrated travel map (`npc/Reiskaart` server, `npc/client/ReiskaartScherm`, payloads
  `NpcPayloads.Reiskaart/ReisPlek/ReisKies`). A plain click on the captain opens it directly; in a quest dialogue ("how is it
  going") the first choice is "Laat de reiskaart zien", followed by the direct destination choices.
- Art: `tools/features/vadsrijk_reiskaart_art.py` (PIL, fixed seed) -> `textures/gui/vadsrijk/reiskaart.png` 768x480: starter island
  bottom left, zone 1 mainland with mushrooms, zone 2 snowy peaks around the pink geode, zone 3 De Schemerwortel (larger than
  zone 2, dusky woods + giant tree), a small zone-4 silhouette. Icons (item badges), dotted curved routes, names, the pulsing
  "Jij bent hier" marker and the grey veil + "?" are drawn in Java. Spot fractions live in BOTH the script and `ReiskaartScherm.PLEKKEN`.
- Drag with the mouse held to pan (clamped); no zoom. Hover shows name + state; click an open place = fly (server checks again;
  a choice only counts within 3 min after the map was opened at an airship NPC).
- Unlock: Knuffelkamp always; Zwamzoet once hq05 is done or hq06 runs; Kandijkom once z2_hq01 started, or zone 1 done + lv 20.
  Others: grey + "?" + "Nog niet ontdekt ..." line (`vadsrijk.npc.reis.slot.*.kort`). `schemerwortel` and `zone4` ("???") are
  teasers ("Binnenkort..."); give them a real state in `Reiskaart.plek` when zone 3/4 exist.
- The bells were not changed (wereld's code); optional follow-up: let the bell open `Reiskaart.open(p)` too.

### 2. Unstuck (U) explained
- Meester Kussensloop (hq03 hand-in, Knuffelkamp training field): a new page about U + 3 seconds standing still.
- The opening welcome line (`vadsrijk.npc.opening.welkom`) ends with "Zit je ergens vast? Druk op U en blijf 3 tellen stilstaan."
- Avonturiersgids: new page `vastzitten` ("Vastzitten? Druk op U"), second in the list; the "begin" page mentions it too.
- NB: kern's mechanic is press U then stand still 3 s (not hold); the texts say that. Kern's own `tip.vast` was left alone.

### 3. Job advancement in the tracker / questlog
- `NpcVolg.klasseQuest(p)`: the first KLASSE quest of your current class that is active or may be started (level + earlier quests).
  The tracker shows it as its own block (light blue, "✦"), its beam is light blue, and it points at the Grootmeester (or the
  Ordemeester in zone 2) until it is accepted, then through its steps to the hand-in.
- Questlog (J): a "Klasse" row under the main story when the quest is not yet accepted (step "Praat met ... "); an active one is
  in the normal list. No pin button on that not-yet-accepted row.

### 4. Zwamtempel door (bazen Zwamtempel, small gate fix)
- `Zwamtempel.waaromDicht`: level >= 18 (`TEMPEL_ALTIJD_LEVEL`) ALWAYS opens with a right-click (or walking up), whatever the
  quests; the old rule stays (lv 16 + hq10/hq11 active or done).
- The line below the gate (gold now) says the player's level, that 18 always works, and that the story quest allows 16
  (`vadsrijk.bazen.tempel.te_laag` / `.verhaal`, args %1$s = 16, %2$s = your level, %3$s = 18). Texts also in tools/features/vadsrijk_bazen.py
  (only the two TEKSTEN lines; the lang json was patched directly, the bazen generator was NOT run: it rewrites models/textures).
- hq11_opperzwam got a first objective `meld zwamtempel:deur` "Klik op de grote deur van de Zwamtempel" (tracker target: poi
  tempelpoort). Counted by the door ceremony (everyone within 24), a click on the already-open door, and standing in room 1.
  Save compat: a player with hq11 ALREADY active gets its old progress shifted by one objective (dev saves only).
- hq10's last hand-in page and hq11's texts say "klik met rechts op de grote deur".

### 5. Puzzle-room line at the bottom (KamerTips)
- New `npc/KamerTips`: while you stand in a puzzle room, its tip is drawn at the bottom of the screen above the action bar (client
  `VolgHud.tekenKamer`, payload `NpcPayloads.KamerTip`, sent on change, checked twice a second). It never overwrites the rooms' own
  action-bar messages.
- Rooms: Zwamtempel Lichtzwamkamer / Hendelkamer / Ridderzaal (BazenPlekken K1..K3 boxes); Spiegelburcht Lichtkamer / Prismazaal
  (Z2BazenPlekken.PUZZELS) and the Spiegelgang (`Spiegelburcht.inGang`); a solved Spiegelburcht room says "Opgelost! ..." instead.
  Texts `vadsrijk.npc.kamer.*` (zone 1 in vadsrijk_npc.py, zone 2 in vadsrijk_z2_npc.py).

### 6. NPC heights (zone 2 + the shared floor search)
- **Shared code changed (note for g-kaart):** `Npcs.standplek(sl, pos)` is now used for spawning and for snapping home. If the spot
  itself is INSIDE a block (wall/ground), it first looks for a free, lit floor right next to it (ring 1..3, same height +-1)
  before searching up/down; that is how Bakker Speculaas fell into the dark pocket under his house (his spot x 95 was in the west
  wall of huis_4, with a floor gap underneath). `Npcs.vloer` (used by Verhaal.land and the tp command) is unchanged.
- Zone-2 spots checked against the tiles (all 50, script below): fixed z2_bakker (95 -> 93, outside the door, faces the street),
  z2_beeldhouwer y 107 -> 106, z2_touwtje y 150 -> 151 (was in a snow block), z2_wacht_burcht y 164 -> 163, z2_versteend_pluk
  y 85 -> 87 (was in stone). All 50 now stand on a floor at their exact y. No zone-1 spot data touched.

### 7. Zone 2's first quest: the wall (NOT fixed, the map is being redesigned)
- **Exact spot:** Kandijkom's EAST gate is closed by a 1-thick, 7-high wall of stone/calcite at **x = 128, z -2414..-2386,
  y 91..97** (the path's knuffelklinkers run on under it; beyond is a tunnel with air y 91..96). The WEST gate has the same wall
  at **x = -128, z -2404..-2396 (and wider), y 91..97**. The NORTH gate has a 4-high step at **z = -2528, x -3..3, y 91..94**.
  Only the south (dock road) is open, and that leads to the dock, not to land. Cause (tools/vadsrijk/zone2/kaart2.py): the gate
  flattening only applies inside `r < R0 + 4` (r 128) while the geode wall shell starts at r 128 (`buiten`), so the gate's road
  meets the wall. For the redesign: carve the gates through the whole shell (`self.poort & (r < R0 + GEODE_DIK + 40)`).
- Tracker: inside the geode with a target outside, the step now points at the gate on that side first
  (`Z2Haken.geodePoort`, `Z2NpcPlekken.POORT_OOST/WEST/NOORD`, text "Eerst de geode van Kandijkom uit: door de oostpoort, aan het
  eind van de straat langs de Kandijkraam"). z2_hq03's texts name the east gate. Update `POORT_*` if the redesign moves the gates.

## Tests
- Gametests (`-Pgt=VadsrijkNpc`): new `reisKeuzes` (all three captains: shown/locked/flying via a test hook `QuestMotor.VLIEG_TEST`)
  and `klasseInTracker`; the bazen `tempeldeurPoort` test got the lv 18 / lv 17 cases (`-Pgt=VadsrijkBazenGevecht`).
- AutoCheck: `tools/autocheck/vadsrijk_g_ux.txt` (dev command `/guhs vadsrijk npc praat <id>` added for it).
- Spot checker used for zone 2: scratchpad `gux/npcz2.py <worktree>` (reads the tiles with tools/vadsrijk/audit/tegellezer.py).

## Resume point / open
See the bottom of this file (filled at the end of the run).
