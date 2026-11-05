# NOTITIES_S_SITE: guhs.nl/vadsrijk, built locally (branch 110-s-site, from f8cd85d3b)

Nothing is published, pushed or deployed. `pages.yml` is unchanged: the section is only built with an explicit flag.
Input: `guhs1100-avontuur/VOORSTEL_WEBSITE.md` + the user's decisions W1/W2.

## Look at it
```
python tools/make_vadsrijk_site.py                 # ~30 s -> site_build/vadsrijk (sample live data + draft patch notes)
python -m http.server 8000 -d site_build           # open http://localhost:8000/vadsrijk/
```
(Opening `site_build/vadsrijk/index.html` straight from disk works for the wiki pages and the map, but the live parts
(status, highscores, profiles, timers) need http: browsers block `fetch` on file://.)
Screenshots: `notities_s_site/*.webp` (start, map overview + zone 1 + zone 3, guide, zone page, quest, boss, creatures, items,
item, rarities/stars/crystals, highscores, profile, live, patch note, English start page).

## What is there
| Part | Where | Notes |
|---|---|---|
| Build script | `tools/make_vadsrijk_site.py` | standalone; also `tools/make_wiki_site.py --vadsrijk [--live-url ...]` builds it into `docs/site/vadsrijk/` with the real settings (no drafts, no sample data). Link check: 0 broken (12.4k links, 1.85k pictures). |
| Data export (offline) | `tools/wiki_site/vadsrijk/bron.py` | quests*.json + lang + POI_*.md + `*_npc_wiki.py` prose + named boss loot (`new Benoemd(...)` in the *Loot.java) + the runtime dump. Writes `vadsrijk/data/vadsrijk.json` (everything), `data/kaart.js`, `data/vertaal.js`. Allowlist for map points; **secrets only as a count per area** (never a position, also not in the JSON). |
| Data export (mod) | `feature/vadsrijk/web/WikiExport.java` | dumps the runtime registries: areas (+layers), 588 spawners (type, levels, element, elite), 14 arenas (respawn), 102 base types (stats at lv 1/10/20/30/40/50), 104 loot tables, rarities, classes, pets, mounts, NPC/POI positions, sets, crystals, star table. `runGameTestServer -Pgt=VadsrijkWeb` writes `run/vadsrijk_runtime.json`; copy it to `docs/wiki/vadsrijk_runtime.json` (committed). In game: `/guhs vadsrijk web wikiexport` (op). Without the file the site still builds (fewer numbers; the build says so). |
| Map tiles | `tools/wiki_site/vadsrijk/kaart.py` -> `docs/wiki/vadsrijk_kaart/` (committed, 153 tiles, 1.2 MB) | technical/satellite top-down of the real .nbt tiles (texture colours from the mod's assets + the MC 26.1.2 jar, hill shading, water by depth, edge darkening), Leaflet CRS.Simple, zoom -4..0 native, +3 pixelated. Cache in `tools/vadsrijk/_cache/site_kaart_<hash>.npz`. Re-run after a map regen (16 s). |
| Live export (mod) | `feature/vadsrijk/web/WebExport.java`, `WebStats.java`, `WebRegistry.java` | see below. **Off by default.** |
| Sample live JSON | `tools/wiki_site/vadsrijk/voorbeeld.py` | made-up players, real arena ids/classes/zones/base types; only in preview builds (`live-voorbeeld/`). |
| Pages | `tools/wiki_site/vadsrijk/pages.py`, `stijl.py` | 460 pages, NL + EN in every page (the game texts are Dutch in both, like the game's en_us), the wiki's own site.css + `vadsrijk.css`. |
| Patch notes | `docs/patchnotes/<versie>.md` + `tools/wiki_site/vadsrijk/patchnotes.py` + `tools/patchnote_discord.py` | ONE source: the site renders it (more pictures, `:::spoiler` blocks), the Discord script prints/posts the same text without spoiler blocks and max 4 pictures. Webhook only from `GUHS_DISCORD_WEBHOOK`; refuses drafts. `1.10.0.md` is a draft (`concept: true`): only in previews. |

### Pages
Start (hero, live status tiles, zone cards) · Kaart (Leaflet: zone/area borders with level ranges, towns, campfires, viewpoints,
airships/portals/lifts/gondola, dungeon entrances, bosses with live respawn, quest givers (off by default), airship lines, the
live caravan; search; share links `kaart.html?plek=<id>` and `kaart.html?x=..&z=..`; zoom-dependent markers; secret counts as
area tooltips) · Gids (first hour, keys, "where do I go" table by level, element triangle, all in-game Gids topics) · Zones (3)
· Quests (90; walkthrough steps with map links + dialogue, both behind spoiler buttons; secret quests fully hidden) ·
Personages (176) · Wezens (51, filterable; tips, levels, elements, areas, elite %, loot table with % per roll) · Bazen (16;
mechanics, respawn, named loot with drop %, pet, Vahoeg mode; Vahoegnir + Mika-spion as spoilers) · Items (102, filterable by
slot/class/zone; icons from the mod's textures; stats per level; set; drop sources) · Systemen (sets, rarities + stars + crystals,
classes with stats per level, mounts + pets) · Verhaal (the spoiler-free prose of the npc wiki helpers + the travel map) ·
Highscores (7 lists) · Speler (profile from `speler.html#Naam`) · Live (status, players, event of the day, daily reset, week
seal, contracts, caravan, boss timers) · Patch notes · Privacy.

### Spoilers (W1)
- "Toon tot zone X" switch in the header (remembered in localStorage, default 3 = everything released): pages/rows/markers of
  later zones are hidden (`data-zone` + CSS; the map hides those layers too).
- Spoiler buttons: dialogue, walkthrough steps, the "busy" hint, secret quests, story bosses, how to get a pet.
- Secrets are never on the map, only "er liggen hier N geheimen" (area tooltip on the map, a column on the zone pages).

## WebExport (live.guhs.nl)
- Config `config/guhs-vadsrijk-web.properties` (written on first start): `aan=false`, `map=web/live` (relative to the server dir or
  absolute, e.g. `/opt/guhs/web/live`), `status_s=60`, `wereld_s=30`, `hiscores_s=300`, `spelers_s=600`.
- Files: `status.json` (online, count, names, version, uptime), `wereld.json` (boss timers from `ArenaData`, the 06:00 reset,
  the Vahoeg week seal, today's contracts, the caravan phase/stop/x,z), `hiscores/<generiek|knuffelridder|pluisjager|vadsmagier|
  opgevrolijkt|bazen|verkenning>.json`, `spelers/<naam>.json` + `spelers/index.json`. On stop: a last `status.json` with
  `online=false` (the site also shows offline when the time stamp is older than 3 min).
- Data is copied on the server thread, JSON written on one daemon thread, each file atomically (`.tmp` + move).
- `WebStats` (SavedData `vadsrijk_web`) keeps the allowlisted numbers per player (updated on login/logout, every `spelers_s`,
  boss kills via `Haken.VERSLAGEN`), so offline players stay on the lists.
- Privacy (W1 "alles openbaar, nooit IP of live posities"): `WebExport.snapshot` is the only place player data is read; a
  gametest asserts no position/IP/coins/area and a date-only "last seen". Names are filtered to `[A-Za-z0-9_]{1,16}`.
- Dev: `/guhs vadsrijk web nu` writes everything once (also with `aan=false`).
- Server side still to do (needs the user's OK): Caddy site `live.guhs.nl` serving `/opt/guhs/web/live` with
  `Access-Control-Allow-Origin: https://guhs.nl` and `Cache-Control: max-age=30`, Cloudflare DNS `live` (proxied, orange), then
  `aan=true` + `map=/opt/guhs/web/live` on the server.

## Tests
- `runGameTestServer -Pgt=VadsrijkWeb` (slot script, GUHS_SLOTS=3): **all 3 required tests passed** (wikiExport, allowlist + the
  status/world builders + atomic write).
- Site: link check 0 broken; `node --check` on the scripts; pages looked at in headless Edge (screenshots above).
- Full site: `python tools/make_wiki_site.py --vadsrijk` -> docs/site (git-ignored): 0 broken over the whole site (144k links);
  its one note (vadskracht_wiki.py KeyError) is older than this branch.

## Open choices / for the integration
- **Map tiles vs. layout:** the committed `.nbt` tiles (from k-int) still have zone 3 at its OLD place (trunk 2300,0), while the
  code/POIs of this branch have it at (1404,-576). I regenerated zone 3 locally (`maak_kaart.py --zone 3`) to render the site
  map, then restored the tiles (not committed, per G_BRIEF). So `docs/wiki/vadsrijk_kaart/` matches the code. After the
  integration's regen (and after any wereldindeling move): `python tools/wiki_site/vadsrijk/kaart.py`, the gametest for the
  runtime dump, then the site build.
- **Publishing (needs OK):** add `--vadsrijk --live-url https://live.guhs.nl/` to the build step of `.github/workflows/pages.yml`
  (+ `docs/wiki/vadsrijk_*/**` and `docs/patchnotes/**` to its paths), a link from the landing page / wiki to `/vadsrijk/`,
  and the Caddy/DNS part above. Leaflet and the skin heads load from unpkg.com / mc-heads.net.
- Spoiler switch default = zone 3 (everything released). Could default to zone 1 for new visitors.
- Event of the day = a fixed week rhythm in `pages.py` (EVENTS); no server-side event system exists yet.
- Highscore "rijkdom" deliberately absent; market prices not built (W1).
- Mob/boss pictures: none yet (the wiki's render pipeline has no Vadsrijk renders); items use their in-game icons.
- Zone 3's layered areas are concentric circles on the map; labels are spread round the trunk but still busy at zoom 0.
- `knuffelridder` has no `vadsrijk.klassen.klasse.*` key (the site falls back on `vadsrijk.kern.klasse.knuffelridder`).

## Resume
Everything is regenerated by: `kaart.py` (map) -> gametest `VadsrijkWeb` + copy `run/vadsrijk_runtime.json` -> `make_vadsrijk_site.py`.
