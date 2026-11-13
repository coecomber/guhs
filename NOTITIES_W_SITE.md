# NOTITIES_W_SITE: guhs.nl/vadsrijk without sample data, built from the real v-int data (round W, 2026-11-08)

Worktree `guhs_vadsrijk/u-site`, branch `110-u-site`. It was **fast-forwarded to 110-v-int 0f00119f2** (u-site was an ancestor:
v-int had already merged u-site round 3). So the site is built from exactly v-int's sources. v-int itself was only read, never
built in, never written to.

## 1. Fake data removed
- `tools/wiki_site/vadsrijk/voorbeeld.py` deleted. It made `live-voorbeeld/` (made-up players Lieke/Juiced/PluisPiet/...,
  positions, hiscores, profiles, status, world). The build no longer writes `live-voorbeeld/`.
- `make_vadsrijk_site.py`: `LIVE_URL = "https://live.guhs.nl/"` is now the default for every build (preview and real). The
  `data-voorbeeld` attribute and the "Voorbeelddata (voorbeeld, geen echte spelers)" badges are gone (home, live, highscores,
  profile).
- The "Event van vandaag" (a week rhythm the site made up, not game data) is removed from the live page and from vertaal.js. The
  live page tile now says "Baastimers, de karavaan, het weekzegel".
- Preview banner: "Voorbeeld van Guhs 1.10.0 "Het Vadsrijk": nog niet uit. De live-delen (server, highscores, spelers op de kaart)
  starten zodra de server de update draait."
- Privacy page and the live page footer said "never a position": they now explain the live map + `/guhs vadsrijk kaart verberg`.
- Grep of the build for lorem/ipsum/voorbeelddata/sample data/TODO/FIXME and the made-up names: only real game text is left
  (Breister Wolwinter, "pluimpjes" are in-game names).

## 2. Empty states (assets/vadsrijk.js, from stijl.py)
- `VR.haal`: 8 s timeout (AbortController). `{"nog_niet_live":true}` counts as "not live".
- Server tiles: "Nog niet live" + "Live zodra de server de update draait".
- Highscores: "Nog geen spelers in het Vadsrijk. Live zodra de server de update draait." (or just "Nog geen spelers in het
  Vadsrijk" for an empty list).
- Profile index: the same texts. Live page boss timers + caravan: "Live zodra de server de update draait".
- Map: "Nog geen spelers in het Vadsrijk" for an empty `posities.json`, "Live zodra ..." when unreachable. It polls every 10 s,
  but after 2 failures only every 60 s. A `posities.json` older than 2 min (server stopped) = nobody on the map.

## 3. Real data from v-int
- Built from 0f00119f2's sources: 939 pages; 156 quests, 359 characters, 84 creatures, 27 bosses + the raid, 274 items, 134 map
  points, every secret of zones 1-6; 0 broken links (26.5k links + 4.5k pictures).
- **Item icons: 274 / 274.** The 8 without one (zone-1 starter gear: knuffelharnas, pluishelm, knuffelketting, pluisoorbel,
  roze_ring, knuffelknots, kussenzwaard, pluishamer) borrow vanilla textures (`minecraft:item/golden_chestplate`, mace, ...). New
  `tools/wiki_site/vadsrijk/vanilla_iconen.py` copies every vanilla texture a `vr_*` item model borrows (48 tiny PNGs) from the
  26.1.2 client jar into `docs/wiki/vadsrijk_vanilla/` (committed, so CI needs no jar). `pages._item_textuur` uses that folder for
  `minecraft:` textures.
- Renders: `renders.py` re-run on the v-int tree (117 renders; 7 webp changed slightly after u-krimp's PNG pass).
- Map tiles: v-int's committed tiles from the final zone 1-6 regen (045387885).
- Runtime dump: regenerated in u-site with `runGameTestServer -Pgt=VadsrijkWeb` (see the commit) and copied to
  `docs/wiki/vadsrijk_runtime.json`.

## 4. One regen command (for integration at the release)
```
python tools/regen_vadsrijk_site.py --dump      # in a tree you may run gradle in (NOT v-int; it refuses there)
python tools/regen_vadsrijk_site.py             # without the gradle dump (copy docs/wiki/vadsrijk_runtime.json yourself)
```
Steps: runtime dump (gametest VadsrijkWeb via gradle_slot.sh) -> renders (hash cache; `--alles` = everything) -> vanilla icons ->
kaart.py -> make_vadsrijk_site.py. Round W's blob/slime model revert therefore only needs this command: renders.py notices the
changed geo/texture hashes. Then commit `docs/wiki/vadsrijk_*`. `--echt --out docs/site/vadsrijk` builds the real site.

## 5. Live positions (real path)
- Mod: WebExport + WebPosities are already in v-int (default `aan=false`, so it is harmless until switched on).
- Site: reads `https://live.guhs.nl/`.
- Server + DNS: **NOT done.** The coordinator relayed the user's OK to set up the endpoint. My first read-only SSH command was
  refused by the session's permission system ("Production Reads"). A search for a local Cloudflare token was refused too
  ("Credential Exploration"). Per the instructions I stopped there and did not work around it.
  `LIVE_DATA_PLAN.md` has every command: DNS records, folder + empty dataset, Caddy block (Caddy already runs for map.guhs.nl, so
  Caddy rather than nginx), checks, rollback, and phase B (`aan=true`, `map=/var/www/guhs-live`) for the 1.10.0 rollout.

## 6. Preview
- Republished on gh-pages (scratch worktree `<scratchpad>/ghpages`, branch u-site-ghpages -> origin/gh-pages):
  `vadsrijk-preview/` replaced with the clean build: **gh-pages 85aba1b31** (fast-forward on 9fc738168). The deploy is live: 200 on
  /, kaart, live, highscores, speler, privacy, bazen, quests, items, wezens, geheimen, raid, zone6, verhaal, data/*.json/js;
  `live-voorbeeld/posities.json` is 404 now. A grep of the published HTML/JSON/JS for voorbeelddata/sample data/live-voorbeeld/
  the made-up names/VR_EVENTS/lorem finds nothing. `data-live="https://live.guhs.nl/"`, noindex is still there.
  guhs.nl/ and /wiki/ are 200 and unchanged. main and the normal site were not touched.
- Rollback: unchanged (Actions -> "Wiki (GitHub Pages)" -> run on main).

## Shared-file changes
None in mod code. Only the site tooling (`tools/wiki_site/vadsrijk/*`, `tools/make_vadsrijk_site.py`, new
`tools/regen_vadsrijk_site.py`) and generated docs (`docs/wiki/vadsrijk_renders/`, `docs/wiki/vadsrijk_vanilla/`,
`docs/wiki/vadsrijk_runtime.json`).

## Open
- live.guhs.nl server + DNS (phase A of LIVE_DATA_PLAN.md): needs someone with SSH/Cloudflare access, or a permission rule.
- At release: phase B (mod `aan=true`) + phase C (pages.yml `--vadsrijk`, remove the preview from gh-pages).
- After round W's blob revert: `python tools/regen_vadsrijk_site.py` in the integration tree.

## 7. Spoiler switch always starts at zone 1 (user request, later in round W)
- `stijl.py` JS: every page load does `setZone('1')`. The choice is never stored, and the old `guhs-vr-zonetot` localStorage key
  is removed. The HTML itself also says `data-zonetot="1"`, so nothing beyond zone 1 flashes before the script runs.
- A note on load (fixed card at the bottom, NL + EN): "Alle informatie toont alleen de tutorial en de eerste zone. Spoilers of meer
  informatie? Kies rechtsboven bij Toon tot tot welke zone je alles wilt zien." It has an OK button and a "Niet meer tonen" box
  (localStorage `guhs-vr-spoileruitleg-weg`). That box only hides the NOTE next time; the switch still starts at zone 1. Changing
  the switch also closes the note.
- Content gating:
  - `pages.py` link helpers (`l_npc`, `l_quest`, `l_item`, `l_wezen`, `l_gebied`) wrap a link to anything of zone > 1 in
    `<span data-zone=N>`.
  - Item "Waar te krijgen" is a comma list (`ul.komma`) sorted by zone.
  - The zone filter `<option>`s carry their zone.
  - Rows of the recipe/wardrobe/mount/pet/pet-power/coat tables get `data-zone` = the highest zone named in their text
    (`Bouwer.tekst_zone`).
  - The guide topic "rijdieren" is zone 3. The story page's world-map caption and its part II/"verder" nav links are gated.
  - The ambachten workshop sentence is split, so the town list is gated.
  - NPCs named after a later town (bond_bord_rozenkloof, kk_poort_saffraanpoort, ...) get that town's zone (`bron._npcs`).
- The map (kaart.js) already filtered markers, layers, the search and routes with `VR.zone()`, so it now starts at zone 1 too.
- New check `python tools/wiki_site/vadsrijk/zone1check.py [site_build/vadsrijk]`: it works out the visible text at zone 1 and
  looks for names of zone 2-6 things. Result: **0 leaks on 937 pages**.
  - Allowed on purpose: "Vahoegnir" (the zone-1 story premise in zone-1 quests and lore), "De Schemerwortel" (Stalmeester Hoefje's
    own zone-1 line), and "Bolletje" (a name clash with a zone-1 knuffel).
  - Skipped: the 1.10.0 patch notes, which summarise the whole update on purpose.
  - Every page whose subject is beyond zone 1 shows only "Verborgen door je spoilerinstelling."
- Checked in headless Edge: bazen/index at zone 1 = only De Knuffelzwammen + hidden notes + the note card.

## 8. Regen from 110-w-int (w-int agent, 2026-11-09)
- 110-w-int merged into 110-u-site (2f0d39d9d; renders index kept, the regen rewrote it). No gradle in u-site: the runtime dump
  is w-int's own `run/vadsrijk_runtime.json` from its full gametest run (VadsrijkWeb), copied here (read-only on w-int).
- `python tools/regen_vadsrijk_site.py`: renders 117 (the round-W slime/Klodderreus renders came in with w-modellen; re-render
  identical, index hashes updated), vanilla icons, map tiles from the round-W regen, site: 939 pages, 0 broken links;
  zone1check 0 leaks on 937 pages; no sample data. Committed 907366dce.
- Preview republished: gh-pages aa19bf3e8 (fast-forward on 932bbf7b5, scratch worktree `<scratchpad>/ghpages`). /vadsrijk-preview/
  answers 200 with noindex. main untouched.
