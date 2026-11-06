# NOTITIES_U_HUD — round U, slice "hud" (branch 110-u-hud, from eaee06d08)

Party frames, emotes and the HUD editor (FEEDBACK_RONDE4: "party-frames + emotes, en een HUD-bewerker").
Package `nl.juiced.guhs.feature.vadsrijk.hud` (+ `.client`), mixin `mixin/client/UHudEmotePoseMixin`, generator
`tools/features/vadsrijk_u_hud.py`. Test steps: SPIEK_U_HUD.md.

## API for other slices
- **New HUD parts are editable by themselves**: draw through `RijkHud.voegToe(id, ...)` and call `RijkHud.plek("<spot>", w, h)` at
  the start of drawing. A new plek id (add a `case` in `RijkHud.standaardPlek`) is registered in the editor the first time it is
  asked; its lang key is `vadsrijk.uhud.deel.<spot>` (falls back to the id). One element may use several spots (OpslagHud's
  tekenGiet: gietbalk then baasgiet): each plek() call switches the transform.
- A slice with its OWN GUI layer: `HudIndeling.registreerLaag(layerId, partId, naamKey, (w, h) -> {x, y, breed, hoog})` (client) —
  the whole layer is moved/scaled (RenderGuiLayerEvent Pre/Post, LOWEST/HIGHEST priority).
- Party spec line: `PartyFrames.SPEC_BRON.add(p -> langKeyOrNull)` (server; **u-specs**: add your spec name here). Default =
  kern's `Spelers.titelKey` (class title / job advancement title).
- Party target: `PartyFrames.gekozen(ServerPlayer)` = the member this player clicked last (for heal/buff skills that want a
  target); a click also calls T-OPSLAG's `HudBron.zetDoel`.
- Emotes: `Emotes.speel(p, Emote.X, melden)`, `Emotes.stop(p)`, `Emotes.actief(p)`; client pose `EmotePose.speelt(entityId)`.

## What was built
- **Party frames** (`PartyFrames` server, `PartyHud` client): every 10 ticks each party member in the Vadsrijk gets the OTHER
  members (cheap payload `vr_u_hud_party`: name, class id, spec key, level, Vads/max, up to 6 status ordinals, dimension + x/y/z,
  state ok/fainted/offline, leader). Not sent without a party (one empty message when the party ends). Offline members stay in
  the party (kern keeps their UUID) and show grey with their last-known name. "Flauwgevallen!" for 5 s after kern's
  Flauwvallen (via `Haken.FLAUW`). Distance + turning arrow from 24 blocks (uses the loaded entity when there, else the sent spot),
  "elders" in another world. Kern's simple party list is switched off while the frames have data (`RijkHud.EIGEN_PARTY`).
  Click-to-target: click a frame with the chat open (gold edge, server `PartyFrames.gekozen`).
- **Emotes** (`Emote`, `Emotes` server, `EmotePose` + `EmoteWiel` client): wave, hug, dance, cheer, sit, sleep, laugh, point,
  heart. Pose system = a mixin at the TAIL of `HumanoidModel.setupAnim` (so armour models on players follow too) sets
  arm/head/leg rotations (+ whole-body offset/rotation for sit/sleep) from the render state (`EmotePose.STAAT`, filled by an
  AvatarRenderStateModifier), eased in/out. Server plays sound + particles, syncs to the player + everyone tracking
  (`sendToPlayersTrackingEntityAndSelf`), late viewers get running emotes (`PlayerEvent.StartTracking`). Loop emotes stop when
  you move (server and client). Hug: the nearest player within 3.5 blocks hugs along (+ hearts + action-bar line).
  `/emote <naam>|stop` for everyone; wheel on key `;`.
- **HUD editor** (`HudIndeling` + `HudBewerker`): every part is drawn under a pose transform (move dx/dy + scale 0.5-2 around its
  default box). Parts: kern's balken/xp/skills/info/baasbalk/tip (wrapped in RijkHud), every plek spot (party, buffs,
  doelframe, gietbalk, baasgiet, drankjesriem), and whole layers (questvolger, drankjesriem layer, vfx boss cast bar,
  munten-pop, rijdier slot, zeilboot wind). Snap to screen edges/middle, other parts, 4 px grid (Shift = free), scroll = scale,
  right click/R = reset part, "Alles terug" (click twice), saved in `config/guhs-vadsrijk-hud.json`. Moved parts are clamped to
  the screen (GUI scale changes stay safe). Opens from the pause menu + options screen (button top left, only in the Vadsrijk),
  the client command `/hudbewerker`, and the dev command `/guhs vadsrijk hud bewerker`. `RijkHud.skillVak` (skill hover
  tooltips in the inventory) follows the moved skills.

## Decisions I made myself
- Key **`;`** (semicolon) for the emote wheel (hold; a quick tap keeps it open). Free in the final key table.
- Emotes work everywhere, not only in the Vadsrijk (harmless fun; the HUD parts are Vadsrijk-only). Not while riding, flying,
  swimming, gliding or asleep in a bed. Anti-spam 10 ticks.
- Party frames show only the OTHER members (you are the main HUD), max 4 frames; default spot left-middle (kern's old place).
- Party target is a party-level concept; T-OPSLAG's target frame only draws Vadsrijk mobs, so a clicked player shows as the gold
  frame edge, not in the target frame.
- Editor saves offsets relative to each part's default spot (not absolute), so a different GUI scale/window keeps the layout sane.
- No "hide part" toggle (not asked; would need every slice's draw code to skip).
- The ambacht HUD layer (gathering text in the screen centre + bottom-left food lines) is NOT editable: it is two unrelated spots.

## Shared-file changes (all marked U-HUD, add-only)
- `kern/client/RijkHud`: `Plaatser` interface + `PLAATSER` + `EIGEN_PARTY`; `plek` → `standaardPlek` + layout; `case "party"`;
  `deel(...)`/`deelUit(...)` around kern's own parts; `extraBegin/extraEind` around each voegToe element; `skillVak` through the layout.
- `opslag/HudBron.zetDoel`: now public.
- `VadsrijkFeature` (+2), `client/VadsrijkClient` (+1), `guhs.mixins.json` (+1 client mixin), `tools/features/__init__.py` (+1).
- lang nl_nl/en_us: +47 keys `vadsrijk.uhud.*`, `key.guhs.vr_emotes`.

## Tests
- `runGameTestServer -Pgt=VadsrijkUHud` (batch vadsrijk_u_hud, all pass): `partyFramesSync` (frames content, leader, buffs, offline member,
  payload round trip, fake party, click-to-target, leaving), `emoteSync` (loop stops on moving, anti-spam, hug reaches the
  nearby player only, payload round trip). Mixin target descriptor checked with javap.
- No client run by me (no AutoCheck): poses, the wheel and the editor are NOT visually checked. Things to look at first:
  sleep pose (root rotation + offset numbers), sit height, the arrow direction, the editor at GUI scale 4.

## Open points / resume
- Visual tuning of the poses (numbers in `EmotePose.pas`) after the user looks in the dev client.
- Cape/elytra layers do not follow sit/sleep (they use their own model).
- Kern's own `Duizelig/Bevroren` centre lines are not a separate editable part (they sit with nothing; small).
