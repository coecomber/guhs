# NOTITIES_U_SITE: guhs.nl/vadsrijk with zones 1-5 + a hidden live preview (branch 110-u-site, from eaee06d08)

The user gave a full GO (2026-11-06) to test the website as realistically as possible, also on guhs.nl under its own sub-URL.

## Live preview (hidden, noindex)
- **URL:** https://guhs.nl/vadsrijk-preview/ (map: https://guhs.nl/vadsrijk-preview/kaart.html)
- Not linked from anywhere on guhs.nl. Every page has `<meta name="robots" content="noindex, nofollow">` and a yellow banner
  "Voorbeeld van Guhs 1.10.0: nog niet uit ... voorbeelddata". Server/players/highscores/live/profiles read the sample JSON in
  `vadsrijk-preview/live-voorbeeld/` (made-up players), labelled "Voorbeelddata (voorbeeld, geen echte spelers)".
- The game server was NOT touched (no WebExport, no Caddy/DNS for live.guhs.nl).

### How it is published (least invasive; main untouched)
- guhs.nl = ONE GitHub Pages site of coecomber/guhs (source: GitHub Actions), deployed by main's `.github/workflows/pages.yml`.
  A Pages deploy always replaces the whole site, and the `github-pages` environment only allows deploys from **main and gh-pages**.
- So: I added ONE commit on top of the old, unused branch **gh-pages** (fast-forward, no force push; its old 2026-09-30 wiki
  preview files are left as they were and are not served). That commit adds:
  - `.github/workflows/vadsrijk-preview.yml`: on a push to gh-pages it checks out **main**, builds the normal site with the exact
    command + checks of main's pages.yml, copies `vadsrijk-preview/` into it and deploys. So the live site = main's site + the
    preview folder;
  - `vadsrijk-preview/`: the static build (`python tools/make_vadsrijk_site.py` on this branch: preview mode = sample live data,
    draft patch notes, noindex, banner).
- Nothing pushed to main, no 110-* branch pushed, no mod release touched, no Pages/repo settings changed.

### Lifetime + rollback
- **The next deploy from main** (a push to main touching docs/tools/lang, or a manual run of "Wiki (GitHub Pages)") rebuilds guhs.nl
  WITHOUT the preview: /vadsrijk-preview/ then gives 404. That is also the quickest rollback: Actions -> "Wiki (GitHub Pages)" ->
  Run workflow (branch main).
- **Bring it back / update it:** rebuild here, copy `site_build/vadsrijk/` over `vadsrijk-preview/` in a gh-pages checkout, commit,
  push gh-pages (the workflow only runs on changes in `vadsrijk-preview/**` or the workflow file).
- **Remove for good:** on gh-pages `git rm -r vadsrijk-preview .github/workflows/vadsrijk-preview.yml`, commit, push (no run, the
  workflow file is gone), then run "Wiki (GitHub Pages)" on main once. Or reset gh-pages to the old commit 5fcef7dd7.

## Content: zones 4 + 5 and the new systems (tools/wiki_site/vadsrijk/)
- `bron.py`: ZONES + zone4 (De Lampionkust, lv 50-56, hub z4_lampionhaven, envelope z4_zee) and zone5 (Hoogvads, lv 56-60, hub
  z5_hoogvads, envelope z5_moeder_wolkwal); quests_z4/z5.json; POI_Z4/Z5.md (campfires, viewpoints, airship captains, Wrak +
  Buikgangen entrances, wolk-lifts; secrets only counted); z4_/z5_ NPCs; z4/z5 creatures (+ tips + loot tables); bosses Zeeslurf,
  Vliegervanger, Gorgonzola, Donderbuik, Zilverstaart (story spoiler), Grauwe Droom (story spoiler) with their tip cards and pets (from
  the runtime dump's huisdieren.baas); item zones for z4baas_/z5baas_/z5droom_/mw_; airship lines C (Honingtak -> Lampionhaven) and D
  (Opstijgpier -> Hoogvads); zone-5 district names from POI_Z5.md's table (+ a small fallback dict); an area lookup now takes the
  SMALLEST matching area (zone 5's rooms lie inside the districts).
- New data: `ambachten` (60 recipes parsed from ambacht/Recepten.java incl. its loops, gathering spot groups per zone),
  `puzzels` (data/guhs/vadsrijk_puzzels/*.json: name, kind, reward; never solutions, geheimpjes only counted), `kleding`
  (every Kledingkast item + how to get it, behind a spoiler button), `rij_uiterlijk` (coats/patterns/tack), `krachten` (pet powers).
- `systemen2.py` (new): pages Ambachten, Uitrusting 2.0, Karakters en spelmodi, Kledingkast, Puzzels en geheimpjes (player prose
  from the SPIEK_T_* sheets, NL + EN). The mounts page got dressing-up + pet powers; the guide's key table got P/K/Y/I/O/Alt+1-3.
- `pages.py`/`stijl.py`: zones 4/5 everywhere (badges, colours, spoiler switch up to zone 5 with generated CSS, zone header images
  kop_vr_z4_kust/kop_vr_z5_stad, icons), zone envelopes not drawn as areas on the map, `noindex` + banner for previews, sets with a
  6-piece bonus (Wolkwacht), version shown = newest patch note in a preview (1.10.0).
- `make_vadsrijk_site.py`: `bouw(..., noindex=None)` (preview = noindex by default; `--echt` = no banner, indexable).
- `docs/patchnotes/1.10.0.md` (draft): five zones + the new systems + 2 more pictures.
- **Shared-file change (mod):** `feature/vadsrijk/web/WikiExport.java` sets(): + schelpen, lampion (Z4Loot) and wolkwacht (Z5Loot),
  marked `// U-SITE`. Runtime dump regenerated (`runGameTestServer -Pgt=VadsrijkWeb`) and copied to `docs/wiki/vadsrijk_runtime.json`.
- Map tiles regenerated with `kaart.py` (all 5 zones, from this tree's committed .nbt tiles): identical tiles, only kaart.json's
  timestamp changed.

## Round 2 (lead's new requirements, same day)
- **Merged 110-v-int** into 110-u-site (latest models + zone code; zone 6 has no site data yet).
- **Renders:** `tools/wiki_site/vadsrijk/renders.py` renders every creature + boss (GeckoLib model + texture + glow, idle pose, 3/4)
  with t-models' `tools/vadsrijk/model_voorbeeld.py` into `docs/wiki/vadsrijk_renders/<wezen_|baas_><id>.webp` (committed, ~0.8 MB,
  89 renders; hash cache in index.json, so a re-run only redoes what changed; `--alles` = everything). Needs the render pipeline
  outside the repo (renders-mounts/_bron + base-ontwerpen/_bron), so CI only copies the committed files. No render: Kloddertje,
  Honingbij (no own model), Mika-spion (player-like NPC model).
- **Bosses:** a render on every boss page and a preview per boss card; the index has every zone COLLAPSED (`<details>`, open per zone).
  "Alle buit": the boss chest table (rolls, luck, coins, % per item, where the dump has the table: Koning Klodder, Opperzwam,
  Mika-spion, zone-5 bosses), named items with drop % + fixed rarity, and an "Ook" table: mounts (10% per player per win, sure after
  15 = the user's pity decision), mount coats/patterns (first win), pet, trophy, boss core 15% per helper (zones 1-3), Groeipluis,
  Gouden Veren + Schemervleugels 3%/8% in Vahoeg form. Parsed from rijdier/Uiterlijk.java + Aankleden.java (bron.py `_baas_extra`).
- **Creatures:** render on the detail page; the index table has thumbnails, sorting, zone filter, search and pagination (20/page).
- **Items:** + a Zeldzaamheid column/filter (fixed rarity of named items, else "willekeurig"), pagination (25/page). Pagination is
  generic: `<table data-pagina="N">` (respects filters and the spoiler switch).
- **Quests:** every zone collapsed (open per zone).
- **Story page:** `tools/wiki_site/vadsrijk/verhaal.py` (new; replaces the wall of text + old travel map): world map with the new
  layout (zone 6 = Het Zijdeland; regenerated from guhs1100-avontuur/wereldkaart/_bron with a copy in the scratchpad, original untouched),
  parts I-VI, Vads + the 5 steps of grauwheid, a timeline, character cards (Vahoegnir, Piepvel, Moeder Wolkwal, Zachtmoed, de Grauwe
  Keizer-Mika, Ringeling, Moeder Bron) + the Vingers table behind spoilers, chapters per zone with concept renders. Pictures in
  `docs/wiki/vadsrijk_verhaal/` (16 webp, 0.9 MB). Zone-6+ content is gated at data-zone 5. Keizer card gated at zone 5 (my reading).
- **Live positions (user decision, replaces "never live positions"):**
  - Mod (`web/WebPosities.java` new; WebExport/WebStats/WebRegistry/VadsrijkWebGameTests add-only, `// U-SITE`; lang via
    `tools/features/vadsrijk_u_site.py` + one FEATURES line in `tools/features/__init__.py`): `posities.json` every `posities_s`
    (default 10) only when `aan=true` (or once with `/guhs vadsrijk web nu`); format
    `{"tijd": ms, "interval_s": 10, "spelers": [{naam,x,z,klasse,level,zone,gebied}], "status": [{naam,waar}], "schepen": [{id,lijn,x,z,richting,naar}]}`.
    Opt-out `/guhs vadsrijk kaart verberg` / `kaart toon` (any player; stored per account in WebStats). Hidden players appear nowhere.
    Dungeons/arenas (the wings' no-glide list + boss arenas), Knuffelkwartier houses, Bondenvallei = status row only ("in een kerker" ...),
    also as `waar` in the profile. No raid exists yet (hook `WebPosities.VERBORGEN_PLEKKEN`). Gametest `posities`: VadsrijkWeb 4/4 green.
  - Site: map layer "Spelers tonen (live)" (on by default, Leaflet layer toggle), skin head + name + class icon + level, markers glide
    to the new spot, airships as floating icons, note "Geen spelers online" / "Spelers op de kaart: N (+M in een kerker of huisje)",
    profile shows "Nu: ...". Preview = sample `live-voorbeeld/posities.json` (static, so nothing moves there).
  - Still needed on the server (not done, no OK asked): `aan=true` + live.guhs.nl (see s-site's notes).

## Numbers (local build)
Round 2 (after the v-int merge): 744 pages; 131 quests, 286 characters, 70 creatures, 22 bosses, 203 items, 115 map points;
link check: 0 broken (20,221 links + 3,442 pictures); 9.6 MB.

## Open
- Item icons: 8 items without an icon (mostly mw_* / new slots without their own item model).
- Zone 4/5 have no in-game "Gids" topics or npc_wiki lore files, so the Verhaal page has nothing for them yet.
- Puzzles exist only in zones 1-3 (t-puzzels).
- Gathering spot names can mention a place that is itself a secret in-game ("de geheime IJsgrot"); harmless, but could be filtered.
- At release: drop `vadsrijk-preview/` + its workflow from gh-pages and follow s-site's plan (pages.yml `--vadsrijk --live-url`).
