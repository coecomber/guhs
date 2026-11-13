# RELEASE_SITE: guhs.nl/vadsrijk at the 1.10.0 release (only the tutorial + zone 1 open)

The public site publishes only what the server has open. The build option is `--open-zones N`. Its **default is 1** in
`tools/make_vadsrijk_site.py`, `tools/make_wiki_site.py --vadsrijk` and `tools/patchnote_discord.py`.
- Zones beyond N are **not published at all**: no pages, no data or JSON, no map tiles, no renders or icons, no patch-note
  lines and no search entries. They show up only as one "Binnenkort: nieuwe zones" card.
- The zone switch then shows just "tutorial + zone 1".
- `--open-zones 6` builds the full site.

Nothing below has been done on main yet. Every step on main waits for the user's release OK.

## 0. Before the release (in the tree you release from)
1. Get the site tooling of `110-u-site` in (it is merged by integration like any slice).
   - The commits are f3eec597d, 172adf41c and later.
   - Shared files: `tools/make_wiki_site.py`, `tools/wiki_site/landing.py`, `tools/patchnote_discord.py`, `docs/patchnotes/*`.
2. Regenerate the inputs after the last world/model change, then commit `docs/wiki/vadsrijk_*`:
   ```
   python tools/regen_vadsrijk_site.py           # renders + vanilla icons + map tiles (+ a preview build, open-zones 1)
   ```
   Add `--dump` for a fresh runtime dump in a tree where gradle is allowed (never v-int/w-int).
3. Patch notes `docs/patchnotes/1.10.0.md`:
   - set `concept: false` and `datum: 2026-11-xx`;
   - write later-zone lines inside `:::zone N` ... `:::` blocks, and "more is coming" lines inside `:::tot 6` ... `:::`
     (see `docs/patchnotes/LEESMIJ.md`). The current text already does this.
4. Local release check. Both must report 0 leaks and 0 broken links:
   ```
   python tools/wiki_site/vadsrijk/zone1check.py --release --open-zones 1
   python tools/make_wiki_site.py --vadsrijk --open-zones 1
   python tools/wiki_site/vadsrijk/zone1check.py --release --open-zones 1 --dir docs/site/vadsrijk
   ```
   Also run `zone1check.py --release --open-zones 1 --dir docs/site/wiki`. The main wiki filters its own Vadsrijk pages with
   the same `--open-zones` (poort.filter_wiki). Its remaining hits are older Guhs words that also exist as later-zone names,
   such as Sneeuwmuts, Pluisje and Sterretje. Those were already on the live wiki before 1.10.
   The first command builds into `site_build/vadsrijk-release`. The release check greps EVERY published file for zone 2-6
   names: html (attributes and the English half too), js, json, css, xml and txt, plus every file and picture name.
5. Look at the hidden preview of exactly this build: https://guhs.nl/vadsrijk-preview-zone1/

## 1. The release (with the user's OK, after server + pack are live)
1. Server: phase B of `LIVE_DATA_PLAN.md`: `aan=true`, `map=/var/www/guhs-live`.
   - live.guhs.nl itself is phase A, which is still open.
   - Set the mod's zone gate to zone 1, matching the site.
2. On main, apply the prepared workflow change:
   ```
   git apply RELEASE_SITE_pages.patch        # .github/workflows/pages.yml
   git commit -m "pages: publish guhs.nl/vadsrijk (open zones: 1)" && git push
   ```
   The patch does four things:
   - the build step gets `--vadsrijk --live-url https://live.guhs.nl/ --open-zones 1`;
   - `docs/patchnotes/**` joins the trigger paths;
   - the existence check also wants `vadsrijk/index.html` and `vadsrijk/kaart.html`;
   - a new step runs `zone1check.py --release --open-zones 1 --dir docs/site/vadsrijk`, so a leak fails the deploy
     before anything goes online.

   With `--vadsrijk`, the landing page also gets one line, "Nieuw: Het Vadsrijk", and `sitemap.xml` gets `/vadsrijk/`.
3. Check the deploy (Actions -> "Wiki (GitHub Pages)" is green), then:
   ```
   for u in vadsrijk/ vadsrijk/kaart.html vadsrijk/zones/zone1.html vadsrijk/patchnotes/1-10-0.html; do curl -s -o /dev/null -w "%{http_code} $u\n" https://guhs.nl/$u; done   # all 200
   curl -s -o /dev/null -w "%{http_code}\n" https://guhs.nl/vadsrijk/zones/zone2.html   # 404 (not published)
   curl -s https://guhs.nl/vadsrijk/index.html | grep -c noindex                         # 0 (the real site is indexable)
   curl -s https://guhs.nl/sitemap.xml | grep vadsrijk
   ```
4. Remove the previews from gh-pages in one commit:
   - delete `vadsrijk-preview/`, `vadsrijk-preview-zone1/` and `.github/workflows/vadsrijk-preview.yml`;
   - then run "Wiki (GitHub Pages)" on main once more (workflow_dispatch). Main's deploy already replaces the whole site
     without the previews.
5. Discord: `python tools/patchnote_discord.py 1.10.0` (default `--open-zones 1`) prints the zone-1 text; add `--post` to
   send it. Ask the user which screenshots first (memory rule).

## 2. Later: opening zone 2 (and on)
- Open the zone on the server (the mod's zone gate).
- In `.github/workflows/pages.yml`, change `--open-zones 1` to `2` in **both** places (the build and the release check),
  then push. That is a rebuild + publish; nothing else changes.
- Discord for that update: `patchnote_discord.py <versie> --open-zones 2`.
- Before that, run the check locally once: `zone1check.py --release --open-zones 2`.
  - It currently reports one false hit: "Spiegelzaal". That is the zone-6 raid floor's name, and the zone-2 quest
    "Spiegelzucht" names its own Spiegelzaal.
  - Allow it in `poort.OOK_IN_ZONE1` (or rename one of them) before you publish zone 2.

## Rollback
- **Site:** revert the pages.yml commit on main and push. The site comes back without /vadsrijk/.
- **Previews:** they live only on gh-pages. Push any commit there to bring them back, or delete them as in step 1.4.
