# Q-DIALOOG: portraits in the talking screen (PraatScherm)

## Cause
The only talking screen is `knuffeldal/client/PraatScherm` (used by verhaal.Praat, Vadsrijk QuestMotor, zone 2/3, Erfenis...).
It drew the portrait only when `level.getEntity(Spreker)` found a LivingEntity. Empty frame when:
- the player's lines ("jij" in QuestMotor.regels): sent as `Regel(null, ...)` -> Spreker -1;
- another NPC named on a page but further than 16 blocks / not loaded / not tracked -> null;
- the narrator and other speaker-less pages (Z3Haken, Z3Koppelingen, RijdierQuests, signs...).
The render call itself (26.1 `extractEntityInInventoryFollowsMouse`) was fine.

## Fix
- `Praat` (server): each page gets `Speler:true` for the reader's own lines (speaker id stays -1, so answers/closing
  behave as before) or a `Portret` tag (entity type + guh variant/clothes/sitting, or GuhNpcEntity kind).
  Pages without an entity ask `Praat.portretBron` resolvers by name key; QuestMotor registers one for
  `vadsrijk.npc.naam.<id>` -> NpcDef (all zones' NPCs go through Npcs).
- QuestMotor "jij" lines pass the player as speaker.
- `PraatScherm`: portrait = local player / the real entity / a cached client-side stand-in built from `Portret`
  (ticked for idle animation) / a writable book for the narrator.
  Framing: guh close (head+body), tall upright entities chest-up, others whole; story GuhNpcEntity keep the old framing.
  Look-at-mouse clamped plus a gentle sway.
- Extra: `top` clamped >= 2 (big GUI scale); `Praat.sluit` sends `Van` = sleutel and the client only closes
  that conversation; `SceneSpeler.start` closes a PraatScherm via `onClose()` (the server hears -1).
- Dev aid: with F3+H (advanced tooltips) a gray line under the screen shows sleutel | text key | speaker id | portrait source.

## Open
- Mika de Imker (Z3Koppelingen imker scene, name key `vadsrijk.z3npc.imker.naam`) has no resolver -> book.
  Add a `Praat.portretBron` for it if it has its own entity type.

## Test
- compileJava OK. No AutoCheck run: only ~5.5 GB RAM free while the user's client runs (too tight for gradle + a 2nd client).
- Ready to run: `-PautocheckScript=tools/autocheck/vadsrijk_q_dialoog.txt` (dev command `/guhs vadsrijk npc portrettest`:
  player line, zone-1 stand-in Oma Truus, zone-3 stand-in z3_imker_1, narrator; then a live NPC from the row).
- By hand: F3+H in the talking screen shows the sleutel/text/speaker/portrait source line.
