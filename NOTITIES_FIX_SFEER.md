# NOTITIES_FIX_SFEER — Vadsrijk fix round 1, agent SFEER (branch 110-fix-sfeer)

Ronde 14 "Sfeer" + VOORSTEL_EPISCHER ideas 1, 5, 10, 11, 12, 14. All new code is in `feature/vadsrijk/wereld/sfeer` (server)
and `feature/vadsrijk/wereld/sfeer/client`. No map generator, no wereld travel code, no wereld client touched.

## Hook edits outside the package (one line each, for the integrator)
- `VadsrijkFeature.register/payloads`: `wereld.sfeer.SfeerRegistry.register(modBus)` / `.payloads(registrar)` (after wereld).
- `client/VadsrijkClient.init`: `wereld.sfeer.client.SfeerClient.init(modBus)` (after WereldClient).
The sky layer is its own `RenderLevelStageEvent.AfterSky` listener, so `WereldClient`/`WereldLucht` are unchanged.

## What is built
| what | where | how |
|---|---|---|
| Fog colour/density per area, ~20-block soft borders | `client/GebiedMist` | kern `Gebieden` boxes (smoothstep over 10 blocks each side of the edge) + the Kloof ravine polyline (from `tools/vadsrijk/wereld/kaart.py` KLOOF, fades 18..38 blocks). Stad/kamp/haven warm pink-gold, weide golden, woud green-gold + denser, druipgrotten blue-turquoise, kloof cool grey, zwamtempel purple. Colour scaled by daylight; `ComputeFogColor` + `RenderFog` (far plane x0.5..0.62 in woud/grotten/kloof/tempel). |
| God rays Sporenwoud | `client/SfeerWereld#stralen` | additive slanted shafts at `WereldPlekken.LICHTSTRALEN` (on top of wereld's falling motes), lean with the sun, by day. |
| Dragon pass (idea 1) | `Drakenpas` (server) + `client/SfeerDeeltjes`, `SfeerWereld#schaduw` | every 30-45 min (random), prefers a player in Zwamzoet (fixed low path between the southern caps over the plein), else a straight skim ~30 above the ground over a random player's spot. Skipped (retry each minute) when any player within 200 of the path fought in the last 15 s (kern `Schade.NA_KLAP`), is inside a kern `Arena`, or has a targeting boss within 40; or players under the surface; or a `DraakVlucht` is already flying (opening scene). Uses `DraakVlucht.vlieg(level, pad, 260, true)`. Server: NPC guhs (`RijkGuh`) look up at it + one fright hop + squeak; klodders (`Slijmwezen` without target, not bosses) hop away. Client (packet `DraakPas`): terrain-following shadow under every low `Vahoegnir`, whoosh + wind particles along its heading, lanterns/froglights within 20 blocks sway (flame/end-rod drift), small flowers within 22 blocks turn grey (client-side swap to closed eyeblossom) for 10 s. |
| Skyline light wave (idea 5) | `client/Lichten` | **client glow layer only** (allowed fallback): the client scans loaded chunks of Zwamzoet + Sporenwoud for light blocks (emission >= 10, palette `maybeHas` first, one light per 3x3x3 cell, <= 900 nearest), delay = distance from the plein + height. At tod 12500 the horn (goat horn "call", ambient) sounds, lights flash on with a sparkle in a ~20 s wave up the caps, the ordetempel beacon (halo + beam on top of `hoed_tempel`) last with a second horn. Halo stays all night, fades at dawn (tod 23200-23600). No real block toggling: nothing to export from the generator, no server cost. |
| Sporenstorm (idea 11, visual only) | `SfeerKlok.storm`, GebiedMist, SfeerLucht, SfeerDeeltjes, Lichten | every day tod 11800 + 3 min: fog orange-purple + denser, orange-purple sky bowl, thick spores (crimson/warped/blossom) + glow motes, the glowing mushrooms (light blocks in the woud) get pulsing pink halos. No xp bonus (optional, skipped). |
| Pink showers + rainbow (idea 14) | `SfeerKlok.bui/regenboog`, SfeerWereld#regen, SfeerLucht#regenboog, SfeerRegistry | on 2 of 3 days, once by day (hashed start): 1 min pink rain streaks + pink splashes + soft rain sound + pink fog/sky, slimes on the Klodderweide hop happily (server, splash + hearts), then a 7-band rainbow opposite the sun for ~70 s. |
| Aurora (idea 12) | SfeerLucht#noorderlicht | at night 3 faint waving pink ribbons above the Zwamtempel mountain (direction + elevation from the real position). |
| Light pillar after the Opperzwam | SfeerLucht#zuil, `SfeerRegistry.vlag` | flag = kern kill key `baaskill:guhs:vr_baas_opperzwam` in `SpelerData.gezien` (synced by packet on login/dimension change/every 2 s when it changes); the ribbons turn bright pink and a pillar rises from the dome (y 195) for that player only. |
| Config | `client/SfeerConfig` | `config/guhs-vadsrijk-sfeer.toml`, `sfeer = VOL / ZACHT / UIT` (ZACHT: weaker fog, fewer particles, <= 350 halos; UIT: nothing). |

Schedules are deterministic from the day clock (`SfeerKlok`), so all clients agree without packets.

## Dev commands (`/guhs vadsrijk sfeer ...`, op)
`draak [stad|hier]` (a pass now, ignores the fight check), `golf`, `storm`, `bui`, `noorderlicht` (force for you; `bui` also
makes the server's slimes hop), `stop`, `zuil aan|uit` (adds/removes the Opperzwam kill key: dev only, it also affects the
first-kill reward), `status` (next dragon in s, clock values).

## Tests
- AutoCheck script `tools/autocheck/vadsrijk_sfeer.txt` (see the result below).

## Open / for the integrator
- Lights are a glow layer: in daylight the light blocks shine anyway (as before). If the user wants real dark lamps by day, the
  generator could place `minecraft:redstone_lamp`/copper bulbs in the city and export their positions; `Lichten` would then
  toggle them server-side per ring (not done: cost + generator ownership).
- NPC "duck" is a look-up + hop (GuhEntity has no crouch animation).
- Sporenstorm xp bonus not done (optional).
