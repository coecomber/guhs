# NOTITIES_FIX_LEVEN — fix round 1 "leven" (more life in zone 1)

Branch `110-fix-leven`, worktree `guhs_vadsrijk/f-leven`. Owner: the wezens package + kern `mob/Spawners` (+ a small
`plofDuur()` hook in kern `mob/VadsrijkMob`, asked for by the coordinator).

## Why the density was low (diagnosis)
1. **Fill-up was one mob per respawn time**: a spawner spawned 1 mob at once, then +1 every 30-45 s. A spawner with max 4
   took ~2 minutes to fill, and every beaten mob refilled one by one. Players walking through saw 1 per spawner.
2. **Few spawners, far apart**: 7 Klodder spots over a 440x700 area; a player is only near 1-2 of them at a time
   (activation was a 3D 64-block check from the centre).
3. **Mushroom caps**: open-air spawning used the heightmap: in the Sporenwoud (~200 giant mushrooms, 25-60 high) many mobs
   spawned ON the caps, out of sight.
4. Cave spawners: only 4+3+2+1 spots at the knots; nothing in the tunnels.

## What changed
### kern `mob/Spawners` (rewritten tick, same API)
- Awake = a player within **80** blocks (flat; caves inside their y-range; surface spawners only for players above y 58).
- **Fresh fill**: a spawner that rested > 30 s fills up at once when a player arrives. Afterwards every missing mob has
  its own respawn timer (a queue): 3 beaten mobs come back together after the respawn time.
- **Despawn far**: mobs of a resting spawner with no player within **112** blocks vanish (not while fighting). Over-cap mobs
  (chunk reloads) are trimmed.
- **Caps**: per spawner (max) and per kern area (`GEBIED_MAX`: klodderweide 80, sporenwoud 90, druipgrotten 80,
  zwamtempel 30, ring 30).
- **Where**: 12 tries per spawn; never in a block (`noCollision`), in fluid, in the air; open air = the first good ground
  from the top (`slechteVloer`: mushroom caps/stems, leaves, logs, planks/slabs/stairs/roofs, fences, glass are no ground,
  so under a giant mushroom it is the forest floor). Never within **12** of a campfire (`VEILIGE_PLEKKEN`), a guh/NPC or the
  Kampklodder (`VERBODEN`), in a town area, or within 12 of a player.
- Live numbers in `laatstActief/laatstLevend/LAATST_PER_GEBIED`; `/guhs vadsrijk wezens spawners` prints them (+ the plan and
  the entity count of the dimension) and logs them as `[leven] ...`.

### wezens: the plan (`WezensSpawners` + `WezensPlekken`)
Built at startup from the real map (copies of kaart.py PADEN/VIJVERS/ARENA/SLAAPHEUVEL/GROT_INGANGEN/berg ramp and
bouwsels.py GROT_KNOPEN + the tunnel wiggle; keep in step if the map layout changes):
- along every path every 22-34 blocks, in turn: a group ON the path (1-2), a group beside it (2-3, 7-13 blocks out), and a
  **patrolling pair** walking 12 blocks up and down the path (turns every 15 s); respawn 10-20 s (patrols 15 s);
- **camps** of 3-6 (two kinds together) ≥ 22 from paths, ≥ 45 apart: Klodderweide 6, Sporenwoud 8, Druipgrotten surface 4;
  respawn 25-30 s;
- Klodderweide pond rims: bubbelklodders; 2 Moederklodders (2 min);
- caves: a group every 16 blocks along all 7 tunnels (mostly **Druipslijm, which hang from the tunnel ceilings**; ceiling
  search now up to 12 high so knots work too), a camp + elite in each of the 4 knots, the crystal chamber;
- **elites** (3 min) at landmarks: the 6 light beams, the waterfall, Zwamnaald, Klodderkijk, Slijmnaald, Hoedhupperwei,
  the north pond, the 4 cave knots, the 3 cave mouths, the fossil-bridge end and the first hairpin of the mountain path;
- the mountain path: a knight pair / priest every 26 blocks on the ramp (cave-mode floor search at the ramp height), the
  voorhal as before.
- Level ranges per kind unchanged (per area). Elite chance 6 %.
- Spawner ids are `wezens:<groep>#<soort>`; old saved mobs with ids of the previous plan are removed on load.

### behaviour (`RijkWezen`, `Slijmwezen`, Mosgeest)
- **Aggro range per kind** (own target goal `RijkWezen.Aggro`, line of sight, survival players): Klodder 9, Bubbel 12,
  Stuiter 10, Moeder 10, Druipslijm 8, Kristal 9, Hoedjesbol 13, Puffer 9, Wachter 8, Glimzwam 0, Ridder 12, Priester 15,
  Mosgeest 14, Knaag 14.
- **Leash**: more than 30 blocks past its camp radius = it gives up, heals fully, walks home (8 s ignoring players that do
  not hit it). Idle mobs outside their camp walk back; patrols walk their route; slimes hop there, the Mosgeest floats there.
  A mob > 50 past its camp with no target (lost) vanishes. Follow range ≥ 34 so the leash, not vanilla, ends a chase.
- **Social aggro**: when one gets a target, the idle members of its group within 12 join (no chains).
- Mosgeest: floats over the ground under it (not on top of mushroom caps any more).
- **Cheered-up creatures stay ~10 s** (`plofDuur` 200 ticks; kern default stays 30 for bosses/proefklodder): first they
  hop away, then hop around happily with an occasional heart, then vanish.

### ambient life (no fights, no loot, no Vadsrijk xp)
- server `wezens/Sfeer`: around each player, one kind topped up every 2 s (max within 28 blocks), tagged `guhs_vr_sfeer`,
  removed when no player is within 64: Klodderweide knabbelvlindertjes (day), kikkerguhs + guh-visjes at the ponds;
  Sporenwoud kaasmotten (spore moths), glimguhtjes (night); Druipgrotten caves bats + grotglimwormen.
- client `wezens/client/SfeerClient` (particles only): Sporenwoud fireflies (more at night) + floating spores; caves drips
  from the ceiling + glow dust, white ash on the rock hills; Klodderweide drifting petals, fireflies at night and **fish
  splashes** on the ponds.
- Not done (no fitting existing critter, would need new models): mushroom snails, little birds that fly up when you run.
  Kikkerguhs are vanilla Animals: if a player kills one it drops vanilla xp orbs (no Vadsrijk xp).

## Numbers
(see "Test results" below; filled in after the runs)

## Open / for the integrator
- `f-verticaal` (hat paths in the Sporenwoud with an elite nest, the Druipschacht): NOTITIES_FIX_VERTICAAL.md did not exist
  when I finished; once its WereldPlekken ids are known, add spawners in `WezensSpawners.sporenwoud()/druipgrotten()` with
  `zet(..., grot=true, minY, maxY)` around their stand y (a narrow y-range so the hat layer and the ground do not mix).
- `f-sfeer` exists too: if it also adds ambient critters/particles, merge so the two do not double up (mine: `Sfeer`,
  `SfeerClient`, tag `guhs_vr_sfeer`).
- Map layout copies in `WezensPlekken` (paths, ponds, knots) must follow kaart.py/bouwsels.py changes.
