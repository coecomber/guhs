# NOTITIES_SITE_LIVE: guhs.nl/vadsrijk/ is live before the mod (2026-11-09)

The user said: "wel vast website 'echt' updaten zover mogelijk voor het vadsrijk".
- The mod itself is NOT released. No 1.10 mod code went to main: no src/, no gradle.properties, no tag, no jar, no pack and no server change.
- Only the website changed.

## Why it uses a prebuilt folder (not the planned `--vadsrijk` build on main)
`tools/make_vadsrijk_site.py` reads the 1.10 mod source: Java under `src/main/java/.../vadsrijk`, the lang files, POI_*.md and the quest JSON. That source is not on main, and it must not go there yet. Copying it into main would amount to merging the mod.

So the Vadsrijk section is built on `110-u-site` and committed to main as a **static folder**, `docs/vadsrijk-site/`. Main then:
- publishes only what the zone-1 site shows;
- carries no zone 2-6 data, renders or map tiles. The raw `docs/wiki/vadsrijk_*` inputs would have carried them.

**Build command, on 110-u-site:**
```
python tools/make_vadsrijk_site.py --echt --komt-eraan 1.10.0 --open-zones 1 --out <tmp>/vadsrijk
python tools/wiki_site/vadsrijk/zone1check.py --release --open-zones 1 --dir <tmp>/vadsrijk     # must be 0 / 0
```
- `--komt-eraan 1.10.0` is new (commit a29d492aa on 110-u-site). It does three things:
  - shows the banner "Het Vadsrijk komt eraan in Guhs 1.10.0. Spelen kan nog niet: de live-delen ... zijn live zodra de server de update draait.";
  - sets the header badge to "v1.10.0 (binnenkort)";
  - keeps the patch notes unpublished. The patch notes index shows one "Guhs 1.10.0: binnenkort" card and there is no 1-10-0.html.
- The pages are indexable (no noindex).

## What changed on main (one site-only commit)
- `docs/vadsrijk-site/`: the prebuilt section (376 files, about 3.6 MB).
- `tools/make_wiki_site.py`: if `docs/vadsrijk-site/index.html` exists, it copies the folder to `<landing>/vadsrijk` and passes `vadsrijk=True` to the landing page.
  - The copy happens inside the normal build, so **every** site build gets /vadsrijk/: main's `pages.yml`, and also the gh-pages preview workflow, which runs that same main command.
  - A later push to main does not wipe it.
- `tools/wiki_site/landing.py`: with vadsrijk, the landing page gets one line, "Binnenkort: Het Vadsrijk, de RPG-dimensie (komt eraan in Guhs 1.10.0)", and `sitemap.xml` gets `/vadsrijk/`.
- `.github/workflows/pages.yml`:
  - adds the trigger path `docs/vadsrijk-site/**`;
  - with the folder present, checks for vadsrijk/index.html, kaart.html and zones/zone1.html;
  - fails if there is any noindex, any zones page beyond zone 1, or no sitemap entry.
- /wiki/ is unchanged. Its own build path is untouched, so no 1.10 content reaches it.

## gh-pages (previews)
`vadsrijk-preview.yml` now fails if main has `docs/vadsrijk-site` but the build lacks `docs/site/vadsrijk`. Its comment explains the new setup.
- Main's deploy drops the two previews: a Pages deploy replaces the whole site.
- The commit on gh-pages that changed that workflow redeployed the site with both previews AND /vadsrijk/.

## Other workflows on that push
- `release.yml` runs only on tags `v*`, so it did not run.
- `build.yml` runs on main pushes outside docs/ and *.md, so the `tools/` change ran it. It only compiles main's own 1.9.7 jar as a CI artifact: no release, no upload.
- The pack (packwiz) is not in this repo's workflows.

## Rollback
1. On main: `git revert <the site commit>` and push.
   - pages.yml triggers on its own file, on tools/ and on docs/vadsrijk-site, so the "Wiki (GitHub Pages)" run starts by itself.
   - If it doesn't, start it from Actions -> "Wiki (GitHub Pages)" -> Run workflow on main.
   - The site comes back without /vadsrijk/, the landing line and the sitemap entry. The previews drop out too, because main's deploy replaces everything.
2. To bring the previews back: push any commit to gh-pages that touches `.github/workflows/vadsrijk-preview.yml` or a preview folder.
   - The preview workflow's vadsrijk check only runs when main has docs/vadsrijk-site, so it works after the revert too.

## Updating /vadsrijk/ before the release (new data)
Rebuild on 110-u-site with the command above and run zone1check. Then replace `docs/vadsrijk-site/` on main with the new output (delete first, so removed files go too) and push. The deploy follows.

## At the mod release (1.10.0)
- When the 1.10 tree is merged to main: delete `docs/vadsrijk-site/` and resolve `make_wiki_site.py`/`landing.py` in favour of the 110 version (`--vadsrijk`, built from source).
  - Expect a small merge conflict at the Landing(...) call and the landing line.
  - Then apply `RELEASE_SITE_pages.patch`. The pages.yml lines added here (trigger path + the `if [ -f docs/vadsrijk-site ...` block) can go, or stay harmless; the patch context may need a manual fix.
- The banner goes away by itself: the release build uses `--echt` without `--komt-eraan`.
- Publish the patch notes: `docs/patchnotes/1.10.0.md` gets `concept: false` + a date.
- The landing line becomes 110's "Nieuw: Het Vadsrijk ...".
- Remove the previews (RELEASE_SITE.md step 1.4).
