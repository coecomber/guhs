# NOTITIES_FIX_L (agent L-FIX, branch 110-l-fix from 05196f3cc = 110-j-int)

Generator sources only; tiles / WereldPlekken / POI / AUDIT / veilige_plekken NOT committed (integration regenerates).

## 1. Doors (stal + audit `deur`)
- `bouwsels.stal`: the ground there is y 64 (K.h), the stal was built on g = 63, so the meadow knuffelgras stood in front of
  the door. Now `g = max(63, K.h(door + 2))` (= 64), the yard is carved flat (cave_air g+1..g+4) and a 3-wide knuffelklinkers
  path runs 9 blocks east from the door. `stal.binnen` is back on the network (audit POI 20 -> 19).
- `zwamzoet.hoedhuisjes`: on the small `hoeko` cap the blossom-tree trunk in the middle stood right in front of a house door
  (105,118,-88). The middle tree is skipped when a door is within 2 blocks of the cap centre.
- `bouwsels.haven`: the four Haven houses get a cherry slab step in front of the door where the ground is lower than the sill
  (house (100,540): a 1-block step up into the door without a stair).
- Audit after regen: **deur 2 -> 0**, nothing new. Extra door check over all zone-2 tiles (front + back free 2 high + floor):
  35 doors, 0 problems; zone 1: 45 doors, 0 problems.

## 2. Pier fences
- De Haven (`bouwsels.haven`): the quay railing (fence at x = oever) ran straight across the pier start (z 468..472): cleared.
  The guh flag pole stood ON the pier deck at (31,468): moved to the end post (30,473).
- Zwamzoet bay (`stad_wereld.haven`): the quay railing from `terrein()` crossed the start of both jetties (scene px 84/160 =
  world x -44 / 32): cleared over the jetty width.
- Checked: Knuffelkamp south jetty and the zone-2 Kandijkom airship pier only have railings along the edges (no change).
- Not changed: the little boats' masts (a fence column in the middle of each boat). The boats are not connected to a jetty
  (no bridge exists), so nothing walkable is blocked; if the user means those, give them a gangway + side mast.

## 3. Town sound
- `vadsrijk_stadsmuziek_geluid.py`: the three beds are new (no wind noise at all any more), seamless (wrap-around placement +
  circular reverb), much quieter (rms -33..-35 dB, was -27..-29):
  - `bed_briesje` (Zwamzoet, Knuffelkamp): gentle birdsong phrases + soft distant pentatonic chimes + a faint warm hum;
  - `bed_golfjes` (De Haven): separate soft water laps at the jetty + little droplets + a rare distant chime;
  - `bed_haardvuur` (Kandijkom): a warm low hearth glow with crackles and log pops + soft sleigh-bell jingles.
  Event/file names kept (no code/sounds.json/test change needed). Only the bed OGGs were rewritten (tune code unchanged).
- `StadsMuziekSpeler`: MUZIEK_VOLUME 0.75 -> 0.85, BED_VOLUME 0.55 -> 0.4 (tunes + sweet details lead).
- Not listened to by me: the user judges the sound in the dev client.

## 4. Verkenningsboek for zone 3
- Already generic: `Voltooiing.registreer(new ZoneBoek(...))` + the public helpers in `ZoneBoeken` (quests(zone, soort) via the
  "z<N>_" id prefix, uitkijken, kampvuren, geheimen, baas, huisdieren(zone) via "guhs:vr_z<N>_", rijdieren, gebieden).
- Zone 2's page counts: main + side quests (z2_), lookouts, campfires, clues + treasures, knuffels, 3 bosses (galmwolf,
  spiegelzucht, secret boormeester), pets (vr_z2_), all 6 areas (z2_ring = fallback, like zone 1's ring); mounts = "later",
  not counted. Nothing missing. Zone 3: add `ZONE3` + `zone3()` in ZoneBoeken (or its own class) and register it; areas
  need `vadsrijk.kern.gebied.z3_*` names.

## Tests
- Regen zone 1 + 2 (`--zone alle --geen-cache`), audit, door check, fence check: see above.
- Gametest `-Pgt=StadsMuziek`: 2/2 passed.
