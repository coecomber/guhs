# NOTITIES U-LUCHTSCHIP (round U)

Branch `110-u-luchtschip` from eaee06d08 (110-t-int). Feedback: FEEDBACK_RONDE4 "Luchtschip" (click the whole entity, walk freely on the deck, a clear "not yet" message) + the brief's item 4 (show whether YOU may board).

## What is built

All code is in `feature/vadsrijk/luchtschip/` (on top of T-LUCHTSCHIP); the texts come from the generator `tools/features/vadsrijk_u_luchtschip.py` (15 keys, nl + real en).

| File | What |
|---|---|
| `SchipDeel` (new) | A NeoForge `PartEntity` click box. The ship has 12 of them (`LuchtschipEntity.DELEN`): 4 along the hull (incl. the railing height), the bow, the cabin, 3 along the balloon, the balloon nose with the face, the tail fins, and the ears. They are moved in `volg()` on both sides. They are only pickable: no collision, no push, no damage, arrows pass (`canBeHitByProjectile false`), not saved, not sent. A click goes to the ship's `interact`. |
| `LuchtschipEntity` | Multipart (`isMultipartEntity`, `getParts`, part ids follow the ship id like the dragon's: MC-158205). The ship's own small box is no longer pickable. On board, a click on the ship returns PASS, so eating and items work on the deck. **Deck:** `dek` = the place per passenger (ship frame), `klem` = the railing clamp (x ±2.95, z -4.2..8.6: in front of the cabin, behind the bow railing), `dekPlek` (falls back to the old 16 `PLEKKEN`), `vrijePlek` (start place), `zetDek`. Client: `dekVolgen` walks the other passengers softly to their synced places, plus the walk animation. |
| `LuchtschipDek` (new) | The "virtual deck": payloads `Stap` (C->S: my place) and `Stand` (S->C: the places of all passengers; `eigen` = a correction). Server check: on the deck, at most 0.45/tick (+0.1) since the last step, else the step is cut short and the client corrected. The walk animation for riders: LivingEntity stops it every tick for passengers, so it is kept per entity and written back in `positionRider` (reflection on `WalkAnimationState` speedOld/speed/position, mojmap names; on failure: no leg swing, walking still works). |
| `client/DekLopen` (new) | ClientTickEvent.Pre: WASD (`input.getMoveVector`) + sprint, turned from the player's yaw into the ship frame, clamped, predicted locally (no lag), sent only when you move. |
| `Poorten` (new) | The WHY per line gate: the quest after which the line opens, the story check and the minimum level. `dicht` (the SLOT result, creative/test open), `uitleg` (chat: "Lijn A is nog dicht voor jou. Volg eerst de hoofdquest: na "Een grauwe schaduw" kun je hier vliegen. ... Je bent nu bij: "...". En je moet level 20 zijn (jij: level 3)."), `kort` (for the action bar), `meld` (chat at most every 3 s + action bar). |
| `Luchtschepen` | `instappen` uses `Poorten` (message instead of the old bare key). The pier action bar is green "✔ Jij mag mee" or "✖ nog dicht voor jou: ...". The board has a 4th line ("Mee na de hoofdquest "X", vanaf level N" / "Terugvliegen mag altijd"). Also `bordKlik`, `pierKlik` (RightClickBlock within 7 blocks of a pier POI: explanation, not cancelled), `opAanval` (AttackEntityEvent: a left-click on the ship = board, always cancelled; on the board = board click), and the deck sync every 2 ticks (all every second for new onlookers). |
| `VertrekbordEntity` | Pickable + interact (board click). The box is 2.4 x 0.32 (`LuchtschipRegistry`), and `nameTagOffset(-0.4)` + spawning 0.6 higher (`ONDER`) keep the text in the same place. It remembers its harbour. |
| `LuchtschipRegistry` | Registers the 2 deck payloads and the listeners (attack, pier click, logout cleanup). The board's size changed. |

## Decisions I made myself
- **Clicking: multipart click boxes, not one big box.** An axis-aligned box over the whole ship would be huge when the ship flies at an angle, and it would steal clicks around the pier. 12 square boxes along the axis follow the model closely; at the piers the ships lie N/S/E/W, so the boxes fit exactly there. The space between the deck and the balloon (only ropes) is NOT clickable, on purpose: you can still click through it to the pier.
- **Walking: a "virtual deck", not a collision deck.** Passengers stay passengers. Their place on the deck is their own (client-predicted, server-checked, synced to the others). Why:
  - a moving collision deck (shulker-like boxes) in 26.1 means the player's own client movement fights the ship's movement (rubber-banding, server "moved wrongly");
  - you can fall through at 8 bl/s;
  - the server's "flying" kick fires after 4 s standing on an entity in the air (noBlocksAround only looks at blocks).
  
  With the virtual deck, all rules stay as they were: you cannot fall or jump off (railing clamp; jumping does nothing for passengers), logout mid-flight lands at the destination, sneak only at the pier, no damage, the barrier tag `guhs_luchtschip` stays on (still a passenger).
- **Walking speed** 0.2 bl/tick, sprint 0.28 (as on the ground). No collision between passengers or with the posts (the 4 ropes stand right at the railing).
- **Lines C and D now use the captains' gates** (`Z4Verhaal.slot` = zone3_klaar + level 50, `Z5Verhaal.slot` = zone 4 done + level 55) instead of only the story flag. This keeps the airship consistent with the travel map and makes the level part of the message.
- **Gate quest per line** (Poorten.POORTEN): 0 = hq05_klodderreus; A = hq12_schaduw (cliffhanger zone 1) + lv 20; B = z2_hq12_hartsplinter + lv 30; C = z3_hq14_vahoegnir (the zone-3 bloom) + lv 50; D = z4_hq07_opstijgpier + lv 55. The open/closed decision itself is still `Luchtschepen.SLOT` (other slices may replace it); Poorten only explains it.
- **The board stays one shared name tag** (the same for everybody): its 4th line says WHO may fly; whether YOU may is in your own action bar and in the click answer.
- The pier click explains but never cancels the click (building at the pier keeps working), at most once every 3 s in the chat.

## Shared-file changes
- `tools/features/__init__.py`: +`vadsrijk_u_luchtschip` in FEATURES (add-only, marked).
- `lang/nl_nl.json`, `lang/en_us.json`: +15 keys (generator).
- Nothing else outside `feature/vadsrijk/luchtschip/`.

## Tests
`runGameTestServer -Pgt=VadsrijkLuchtschip`: the 5 T tests + 3 new ones:
- `helemaalKlikbaar`: 8 model points (bow deck, hull underside, cabin roof, balloon side/middle/nose/tail, ears) are each inside a registered click box (`getEntityOrPart`); the parts do not collide, take no arrows or damage; a right-click on the balloon boards; on board the click PASSes; a left-click (AttackEntityEvent) on the hull boards a second player and is cancelled; two passengers start on different places.
- `dekLopen`: steps move you (the server places you in the ship frame); the railing clamp; a 6-block jump is cut short; a step for another ship is ignored; walking in flight; still no getting off; the barrier tag stays.
- `dichtUitleg`: a new player: all 5 lines closed one way and open the other way; the explanation names the right quest per line; the short reason; the board's 4th line on both sides; creative is open.

## Resume point / status
DONE and committed on `110-u-luchtschip`. `runGameTestServer -Pgt=VadsrijkLuchtschip`: all 9 pass (8 own + 1 vanilla). Not tried in a client (the user tests from the SPIEK). Things to look at first in the client:
- walking smoothness while the ship turns (the yaw follow code from T is unchanged);
- the walk animation of remote passengers;
- the board text position (`ONDER` / nameTagOffset);
- whether the click boxes catch clicks meant for the pier (the stern lies close to the pier).

## Open points
- The cabin is solid (no door, the deck stops in front of it); the deck strips beside the cabin are not walkable.
- The ropes/posts at the railing corners have no collision (you walk through them).
- Remote passengers lag ~2-3 ticks behind their own client (sync every 2 ticks + soft follow); fine for looking at each other, by design.
