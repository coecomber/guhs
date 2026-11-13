# NOTITIES_X_SITE: the public wiki with only the tutorial + zone 1 (round X, 2026-11-09)

Worktree `guhs_vadsrijk/u-site`, branch `110-u-site`, as the coordinator asked. It is not a new x-site tree.
- 110-w-int b9ef85f4c was merged in (d76d0e62f). That merge changed only docs and tests; the site sources were already identical.
- No gradle, no client.

## 1. `--open-zones N` (default 1)
- **Where:** `tools/make_vadsrijk_site.py --open-zones N`, `tools/make_wiki_site.py --vadsrijk --open-zones N`,
  `tools/regen_vadsrijk_site.py --open-zones N` and `tools/patchnote_discord.py --open-zones N`. The default everywhere is
  1, the same as the planned server zone gate. `6` gives the full site. Raising N later = rebuild + publish.
- **New module `tools/wiki_site/vadsrijk/poort.py`:**
  - `filter_data`: only zones <= N go to the pages and the JSON. That covers areas, NPCs, quests, creatures, items, bosses,
    secrets (list + counts), lore, guide topics, routes, puzzles, sets, crystals, pets and pet powers (by the names in their
    text), crafts (recipes + places), wardrobe, the raid and the lang keys in `vertaal.js`.
    - Nested references to removed things are cleaned too (item sources, boss loot, ...).
    - Zone-1 NPC lines that name a later place ("Ooit vis ik in de Lampionkust.") are left out.
    - Before zone 3 there are no mounts at all (the first one, the Pluimguh, is chosen in zone 3). The mounts table and
      "Aankleden" are left out then.
  - The Bouwer keeps the FULL data for its look-ups. A link to a later thing therefore still comes out wrapped in
    `<span data-zone=M>`, and `strip_html` then removes every element with data-zone > N from every page. So everything
    the spoiler switch would hide is physically gone.
  - `tegels`: only the map tiles that touch zones <= N are copied. Later-zone parts of a tile become transparent, and the
    map frame shrinks to zone 1: 82 tiles instead of 1308.
  - `ruim_beelden`: removes every picture no file references any more (121 at N=1: renders, icons, kop_zone2-6, story
    pictures).
  - `teaser`: one "Binnenkort: nieuwe zones" card (NL/EN) on the home page and on zones/index.
- **Pages:**
  - The zone switch option 1 is now "tutorial + zone 1" (also in the full site). With one option, the spoiler note card
    does not pop up.
  - The home page intro without "six zones / a raid / an airship" when N < 6.
  - The raid pages and the lore world map (it names every later land) are left out when N < 6.
  - The puzzle heading on the secrets page is "Puzzels" (no "sterrenbakens") outside zone 6. This applies to the full site too.
- **Patch notes:**
  - New blocks: `:::zone N` ... `:::` (only when zone N is open) and `:::tot N` ... `:::` (only while it is not), plus a
    `samenvatting_open:` front-matter field.
  - `docs/patchnotes/1.10.0.md` is rewritten with these. At N=1 it says "De eerste zone: De Knuffelzwammen (level 1 tot 20)
    ... Meer zones volgen later!". At N=6 it is the old full text.
  - The Discord text follows the same blocks.
- **Data fix in `bron.py`:** the mining caves (`grot_*`) all said zone 1. They now take the zone of the area around them:
  Kristalgrot = 2, Wortelgrot = 3, Koraalgrot = 4, Wolkengrotten = 5, Zwamgrot = 1. Before this, the zone-1 page listed
  four later caves.
- **Tagging:** `Bouwer.tekst_zone` now uses `poort.zone_namen`, which also knows names without their article
  ("Lampionkust", "Kristalgrot").

## 2. Release check: `zone1check.py --release [--open-zones N] [--dir <site>]`
- Without `--dir` it builds into `site_build/vadsrijk-release` with the draft patch notes on, so those are checked too.
- It takes the names of every thing beyond zone N from the FULL Bron data, not from the published JSON. That covers zones
  NL+EN, areas, bosses, creatures, items, NPCs, quests, lore, secrets, map points, puzzles, zN_ guide topics, sets, and the
  raid with its floors, bosses and titles, plus the article-less forms.
- It greps EVERY published file raw: html (attributes and the English half too), js, json, css, xml and txt. It also checks
  every file and picture name against the slugs and ids of later things.
- The old visible-text mode (`zone1check.py <site>`) is unchanged.
- **Results:**
  - **open-zones 1: 379 files (217 pages, 73 pictures), 1123 names + 1710 slugs -> 0 text leaks, 0 file-name leaks;
    0 broken links.**
  - Full build (open-zones 6): 939 pages, 0 broken, old zone1check 0 leaks on 937 pages.
  - open-zones 2: one false hit, "Spiegelzaal" (the zone-6 raid floor vs a zone-2 quest's own spiegelzaal). See
    RELEASE_SITE.md section 2.
- A broader grep (`z[2-6]_`, `zone[2-6]`, "raid") of the zone-1 build finds only generic words. Examples: "Imkeren (zone 3)",
  "zones 1-3" in the story part header, and "on zone 4's water: your sailboat" in the key table. These are not names, so I
  left them.

## 2b. The main wiki (/wiki/) leaked too: now gated
The main Guhs wiki is built from the mod's lang + assets, so with 1.10 on main it would make its own pages for every vr_
item/creature/block and every Vadsrijk biome: 412 files with zone 2-6 names, including the search index.
- `make_wiki_site.py` now calls `poort.filter_wiki(site, root, --open-zones)` before rendering.
- **What it drops:**
  - pages with vr_/vadsrijk in the id whose zone is > N. The zone comes from vr_zN in the id, a later name in the title or
    text, a later id/slug in the id, or the recipe tier for crafting things;
  - pages whose id names a later area (the Hoogvads banner blocks).
- **What it cleans:** links to dropped pages are removed from the pages that stay (e.g. the biome list on the Guhmension
  page).
- The index pages, search index and sitemap follow automatically.
- The dry run drops 495 of 4335 pages. The only non-vr ones are `blokken/hoogvads_vaandel_boven` and `_onder`.
- **Full build result** (`make_wiki_site.py --vadsrijk`, defaults):
  - 549 wiki pages of later zones left out; 0 broken links over the whole site (138k links).
  - `/vadsrijk/` itself: 0 leaks. The landing page has the "Nieuw: Het Vadsrijk" line and the sitemap has `/vadsrijk/`.
  - `zone1check --release --dir docs/site/wiki`: from 412 files down to 33 files + 8 file names. Every one of those is an
    OLDER Guhs word that is a namesake of a later-zone thing. Examples: Sneeuwmuts/Wantje (Balto clothes), Sterretje,
    Pluisje, Zandkasteeltje (Reisbureau), Bevroren (deco), Glimmertje (item), Schildpadje, Sneeuwbal, "De Grote Knuffel" and
    "Speuren" (ftb-erfenis).
  - I checked them with curl: they are all on today's live guhs.nl/wiki already. They are not leaks, so the CI step only
    checks `docs/site/vadsrijk`.

## 3. The real publish path (NOT applied: waits for the release OK)
- `RELEASE_SITE.md`: the exact steps (regen, patch notes, local check, server, pages.yml, verification, removing the
  previews, Discord, opening zone 2 later, rollback).
- `RELEASE_SITE_pages.patch`: a `git apply`-ready diff for `.github/workflows/pages.yml`. It adds
  `--vadsrijk --live-url https://live.guhs.nl/ --open-zones 1`, the patchnotes path, the existence check for vadsrijk/, and
  a CI step that runs the release check (a leak fails the deploy). It is checked with `git apply --check`. pages.yml itself
  is unchanged, on this branch and on main.
- `tools/wiki_site/landing.py`: with `--vadsrijk` only, the landing page gets one small line ("Nieuw: Het Vadsrijk") and
  sitemap.xml gets `/vadsrijk/`. Without the flag (today's main build) nothing changes.

## 4. Previews (gh-pages, branch u-site-ghpages in the scratchpad worktree)
- gh-pages **0fdf94769**:
  - `vadsrijk-preview/` = the full build (--open-zones 6);
  - new `vadsrijk-preview-zone1/` = the public build (--open-zones 1). It is noindex with the banner "Dit is de publieke
    versie: alleen de tutorial en zone 1 staan erop.";
  - `.github/workflows/vadsrijk-preview.yml` copies both folders and refuses to deploy either one without noindex.
- main was not touched.

## Shared-file changes (all marked X-SITE)
- `tools/make_wiki_site.py`: the `--open-zones` option (default 1), passed to the Vadsrijk build, and `vadsrijk=` for Landing.
- `tools/wiki_site/landing.py`: an optional `vadsrijk` link + sitemap entry (off unless --vadsrijk).
- `tools/patchnote_discord.py`: `--open-zones` (default 1).
- `docs/patchnotes/1.10.0.md` and `LEESMIJ.md`: the zone blocks.
- Everything else is in `tools/wiki_site/vadsrijk/` (+ `tools/make_vadsrijk_site.py`, `tools/regen_vadsrijk_site.py`).

## Open
- live.guhs.nl (LIVE_DATA_PLAN phase A) is still not set up (no SSH/Cloudflare access in these sessions).
- The mod's zone gate setting must say the same N as `--open-zones` in pages.yml. They are two separate switches; keep them
  together.
- Before opening zone 2: allow or rename "Spiegelzaal" (see above).
- I did not run a client or gametests. The site is static and built from the committed data (runtime dump from round W).
