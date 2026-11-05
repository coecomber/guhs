# NOTITIES_T_Z5WEZENS: zone 5 "Hoogvads" combat (lv 56-60)

Branch `110-t-z5wezens` (worktree `guhs_vadsrijk/t-z5wezens`, from dbf82899d + merge 110-r-int abb8a027c).
Package `feature/vadsrijk/zone5/wezens/` (+ `mob/`, `baas/`, `loot/`, `client/`), ids `guhs:vr_z5_mob_*` / `guhs:vr_z5_baas_*`,
loot tables `z5wezens:*`, lang `vadsrijk.z5wezens.*`, generator `tools/features/vadsrijk_t_z5wezens.py`
(run: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z5wezens.py`; rerun after a gradle build for the worn
armour textures, which need the minecraft jar in build/).
Cheat sheet: `SPIEK_T_Z5WEZENS.md`. Gametests: `VadsrijkZ5WezensGameTests` (`-Pgt=VadsrijkZ5Wezens`).

## The places (decision of this slice: the city is safe, combat is contained)
| place | where (placeholder) | lv | what |
|---|---|---|---|
| De Proefarena van de Wolkwacht | beside the Academie oefenplein (2760,235,-592), r 11 | 56-57 | Bezeten Oefenpop, Sparringridder (non-lethal), leader Opperpop Strobaal |
| De Buikgangen (own rooms) | inside the whale under the market: x 2704..2808, y 185..196, z -530..-508; hatch in the Soepkeuken cellar | 57-59 | saboteurs, lontleggers, pluismijten; dungeon boss Opzichter Gorgonzola (Ketelkamer) |
| De Wolkrand | the two pectoral fins (2605,176,-650 / -390) + the tail fluke (2975,228,-455); cloud lifts from the rim | 58-60 | Wolkduivels, Mistsliers, harpoeniers, leader Moeder Mist; world boss Donderbuik (2975,228,-585) |
| Het Schuilnest (own room) | inside the whale under the Luchthaven: x 2588..2624, y 206..214, z -440..-414; loose plank in the station | 58-60 | Vermomde Mika's, saboteurs, lontleggers, leader Meester-Vermommer Feta |
| De Wolkwaldroom (t-z5verhaal) | Z5VerhaalPlekken.WOLKWALDROOM (2600,200,-520) | 60 | spy finale Kootje Zilverstaart |

All coordinates: `Z5WezensPlekken` (`w("<poi id>", x, y, z)`: `zone5.wereld.Z5WereldPlekken.plek(id)` wins by reflection). They are
from the round-2 whale, NOT lowered; if t-z5wereld lowers the whale ~20, it only has to publish the ids.
The combat areas are kern `Gebieden` (not towns) put IN FRONT of the town boxes (at register + again at ServerAboutToStart, lowest
priority), so spawners work there while the rest of Hoogvads stays a town.

## What was built
- **9 creature kinds** (`mob/`): Bezeten Oefenpop (hidden as a normal dummy, spin), Sparringridder (GUH: right-click to spar; red strip
  = pillow swing, 3 bonks = you lose, pink shield = blocks, at 5 % it bows; never hurts, never defeated; only the partner's hits count;
  win = xp + `extra("z5wezens").sparren` + loot from `z5wezens:sparren`), Mika-saboteur (gas spanner circle + Plakkerig cloud),
  Mika-lontlegger (keeps distance, cheese bombs), Grauwe Pluismijt (small, Gesporen bites), Grauwe Wolkduivel (flyer, dive strip),
  Mistslier (mist circle: Blindness + Plakkerig, bolts), Mika-harpoenier (harpoon line pulls you TO him, never to the edge),
  Vermomde Mika (Wolkwacht costume, unmasks within 4, blinks behind you).
- **3 leaders** (spawners `z5wezens:leider_*`, elite, lv 59/62/62, 5 min).
- **3 bosses** (`baas/`, on zone 3's `Z3Baas` engine via `Z5Baas`):
  - **Kootje Zilverstaart** (lv 60): hatpin lunge, smoke bomb + blink; 70 % the disguise round (4 Z3Doel decoys + him in costume, silver
    glint = the tell; decoy hit = 10 % on the hitter; him hit = unmasked +50 % for 8 s; 25 s = 25 % on all); 35 % the escape (3 gas
    valves, right-click 3x each, floor 10 % until shut). Only spawns when the story asks.
  - **Opzichter Gorgonzola** (lv 59, Ketelkamer, respawn 5 min): 4 warm-air valves (Z3Doel), he shuts one every ~25 s (-15 % damage
    taken each, cold pulse 6 %/3 s), right-click 3x to reopen; 60 % 2 saboteurs; 25 % all shut + floor 15 % until 3 open.
  - **Donderbuik** (world boss lv 60, tail fluke, respawn 20 min): lightning circles, rain lane, the pull (3 pink cushions, else 35 % +
    sucked in), 60 % 3 imps + yellow stack; ends pink and drifts away (victim, cheered up).
- **Story hook (t-z5verhaal):** `Z5Bazen.start(ServerPlayer, String fase)` (static, found by their reflection list) AND a reflective
  `Z5Finale.registreer(Fase.SPION, ...)` when their class is on the classpath. "spion" → the arena reset + spawn, the player (+ party
  within 24) to the door if > 40 away; returns true. "droom" → false (their own scene plays). On the spy's defeat every contributor
  gets `Z5Finale.spionVerslagen(p)` (reflection). Entity id `guhs:vr_z5_baas_zilverstaart` as they suggested.
- **Loot** (`loot/`, `baas/Z5BazenLoot`): Wolkwachtset (helmet, cuirass, greaves, boots + t-items' gloves and cape = 6), jewellery
  (Wolkring, Walvisketting, Regendruppeloorbel + t-items' Stormarmband), 6 class weapons (2 per class), set bonuses 2/4/6 (Verdediging
  +10 & Vads +60 / Schade tegen bazen +8 % / Wolkkussen shield), 7 named boss items (6 Legendary + 1 Guhddelijk), 3 trophies,
  boss chest = `z5wezens:baas` + named; first kill = a sure named Legendary + trophy. 3 pets: Donderbuikje, Warmpje, Lokpopje.
- **Own rooms + rules** (`Z5Bouw`): the Buikgangen and Schuilnest builders, stand-in floors (Proefarena, fins, tail, arenas,
  Wolkwaldroom disc, lift pads), ALL only into air. Hatches/lifts (`Z5WezensPlekken.LUIKEN`): right-click the trapdoor / white glazed
  block = teleport + slow falling; a grey overlay hint within 3 blocks. Fall net: falling below a Wolkrand area's floor = back to its lift.
- **Dev command** `/guhs vadsrijk z5wezens spawn|alle|sparren|tp|bouw|baas|mechaniek|set|wapens|loot|plekken|spawners|opruimen`.

## Decisions made alone
- The 4 places above (the brief's list), plus the cloud LIFTS and HATCHES as teleports (no shaft through the city, no stairs I can't
  see): t-z5wereld may replace them by real stairs; then just drop the lift entries.
- Sparring = a separate GUH kind with pillow bonks counted, not damage (guhs never hurt).
- The Grauwe Droom (part-2 deelbaas) is NOT built here; the brief listed only the spy. The story's fallback scene covers it.
- New slots/stats of t-items are looked up BY NAME (`Z5Loot.slot/stat`): without t-items the gloves/cape/armband pieces and the
  Bazenschade rolls are simply absent; merged they appear (6-piece set). My set counts virtual slots itself (`Z5Sets.stukken`).
- No VFX kit yet (t-vfx had no NOTITIES/commit when I checked): everything uses kern `Telegraaf` (cirkel/lijn).
- Zone-5 levels > Levels.MAX (50 in this tree): xp uses the curve clamped to MAX-1 (like z3-systemen does).
- Boss tips: Z2Baas/Z3Baas show `vadsrijk.z2bazen.tip.<id>` and `vadsrijk.z3bazen.tip.<id>`; my generator fills those keys for my 3 ids.

## Shared-file changes (one line each, marked T-Z5WEZENS)
- `VadsrijkFeature.register`: `Z5WezensRegistry.register(modBus)`.
- `client/VadsrijkClient.init`: `Z5WezensClient.init(modBus)`.
- `tools/features/__init__.py`: `vadsrijk_t_z5wezens` in FEATURES.
- Lang nl_nl/en_us: generated keys only (vadsrijk.z5wezens.*, entity/item names, vadsrijk.loot.*, area names, pet texts, the boss tip keys).
- No kern/registry edits. Reused (read-only) classes: zone 3 `Z3Baas`, `Z3Doel` (own entity types `vr_z5_baas_klep`, `vr_z5_baas_lokpop`).

## For t-z5wereld (POI ids)
See the table in the first version of this file (commit 92870f420) and `/guhs vadsrijk z5wezens plekken`. Volumes to keep free inside
the whale: Buikgangen x 2704..2808 y 184..196 z -530..-508, Schuilnest x 2588..2624 y 206..214 z -440..-414. Wanted ids:
z5w_buik_luik / _uit, z5w_schuilnest_luik / _uit, z5w_proefarena, z5w_lift_{zuidvin,noordvin,staart}_{boven,onder}, z5w_zuidvin,
z5w_noordvin, z5w_staartvlakte_noord, z5w_donderbuik_arena / _deur, wolkwaldroom_arena + z5w_wolkwaldroom_deur,
z5w_leider_{strobaal,mistmoeder,meestervermommer}.

## Open points
- Looks of all models are generated and unseen in a client (the user judges in the dev client).
- Balance (HP, fractions, cooldowns) first guesses.
- Grauwe Droom fight not built; the Wolkwaldroom room itself is t-z5verhaal/t-z5wereld's (I only lay a stand-in disc).
- Gloves/cape/armband items only exist after t-items merges (by design).
- No AutoCheck client run.

## Resume point
See `git log -1` and the test results below.
