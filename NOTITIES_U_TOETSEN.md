# NOTITIES_U_TOETSEN (round U, branch 110-u-toetsen, from eaee06d08)

## What it does
In the Vadsrijk the pause menu (Esc) shows every Vadsrijk key on the RIGHT side in a pink guh panel (semi-transparent).
- Grouped: Gevecht (R V G Z C U), Menu's (K J B N P O I Y), Rijden (X), Sociaal (u-hud's emote wheel `;` once merged),
  Overig (drankjesriem Alt+1/2/3, questvolger/lichtstraal, which are unbound by default, and a future `key.guhs.vr_hudbewerker`).
- Each line = a key cap + a short Dutch description. Unbound = grey with "—". A conflict = red cap + "!" + tooltip "Botst met: X".
- Hover = tooltip (full mapping name, state, "Klik: naar Besturing"); click = the vanilla KeyBindsScreen (back = pause menu).
- `/toetsen` (client command) opens the same overview as its own screen, anywhere.

## How
- `feature/vadsrijk/toetsen/client/VadsrijkToetsen`: reads `mc.options.keyMappings` LIVE every frame, so rebinds and modifiers show at once.
  A mapping belongs in the overview when its category == `KernClient.CATEGORIE`, OR its name starts with `key.guhs.vr_`, OR it is registered.
  `VadsrijkToetsen.registreer(mappingNaam | KeyMapping, beschrijvingKey, Groep)` gives it a group/description. Unknown ones still
  show (group Overig, their own name), so keys from other branches show up after a merge with no code change.
- Conflict = same key AND same modifier AND overlapping conflict context. NOT counted: Alt+1 vs the plain hotbar 1 (NeoForge fires only the
  Alt binding), and the old "Guhs" category keys (maag G, snuffel R/V/Z/B/N, F9/F10...), which kern swallows in the dimension and which belong to other worlds.
- `toetsen/ToetsenIndeling` (pure, gametested): picks scale 1 / 0.75 / 0.5 x 1-3 columns, first without cutting text, then cutting
  descriptions (column >= 96 px). Never below scale 1 at GUI scale 1. A header always stays with its first key; columns are balanced.
- `ToetsenClient`: hooks `ScreenEvent.Init.Post` + `Render.Foreground` (before the deferred tooltip, so the hover tooltip works) +
  `MouseButtonPressed.Pre` for `PauseScreen` (only `showsPauseMenu()`, only `Vadsrijk.in(player)`). The free room = right of the
  furthest right edge of the centred widgets (measured from the screen's children, so a wider grid from another mod moves it along) + 8 px.
  No fit at all = a "Toetsen ▸" button in that strip (or top-right) that opens `ToetsenScherm`.
- Independent of u-hud (its "HUD aanpassen" button is top-left at 4,4) and u-tabs (no shared code).

## Real sizes (1920x1080 window)
- GUI scale 2: room ~350x528: full size, one column.
- GUI scale 3: room ~190x348: scale 0.75, one column.
- GUI scale 4: room ~110x258: scale 0.5, one column (2 real pixels per text pixel, still readable).

## Shared files (add-only)
- `feature/vadsrijk/client/VadsrijkClient.java`: one line `ToetsenClient.init(modBus)` (`// U-TOETSEN`).
- `tools/features/__init__.py`: `'vadsrijk_u_toetsen'` after `'vadsrijk_t_karakter'`.
- lang nl_nl/en_us: `vadsrijk.toetsen.*` (generator `tools/features/vadsrijk_u_toetsen.py`).

## Tests
- `runGameTestServer -Pgt=VadsrijkUToetsen`: `indelingPastOpElkeSchaal` (fits at GUI scale 2/3/4, header stays with its key, null when it cannot fit).
- Not checked in a client (the user tests): the look, tooltip, click to KeyBindsScreen.

## Open points
- The HUD editor has no key (u-hud: button + `/hudbewerker`). If a key `key.guhs.vr_hudbewerker` comes, it is already labelled "HUD aanpassen".
- Party has no key yet; a later party key in the Vadsrijk category lands in Overig unless it calls `registreer(..., Groep.SOCIAAL)`.
- A widget of another mod placed entirely in the right strip (not centred) is not avoided.
- Clicking opens the key screen at the top; it does not scroll to the Vadsrijk category.
