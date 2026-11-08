# HERVAT_U_SITE (pause 2026-11-06)

## Done (all committed on 110-u-site; details in NOTITIES_U_SITE.md)
- Site reads zones 4 + 5 and the new systems (ambachten, uitrusting 2.0, karakters, kledingkast, puzzels); 0 broken links.
- 110-v-int merged in.
- Round-2 wiki requests, ALL built and committed:
  - boss drops with their chances (mount 10% per player per win, sure after 15);
  - renders of every creature + boss (`tools/wiki_site/vadsrijk/renders.py` -> committed `docs/wiki/vadsrijk_renders/`);
  - bosses + quests: zones collapsed by default;
  - creature/item tables with pagination, sorting, filters + search (items: rarity filter too);
  - story page overhaul (`tools/wiki_site/vadsrijk/verhaal.py` + `docs/wiki/vadsrijk_verhaal/`);
  - live player positions:
    - mod: `posities.json`, opt-out `/guhs vadsrijk kaart verberg|toon`, privacy gametest; VadsrijkWeb 4/4 green;
    - site: a "Spelers tonen" layer + sample data.
- Local build: 744 pages, 0 broken links (20,221 links + 3,442 pictures).

## Published on guhs.nl
- **URL:** https://guhs.nl/vadsrijk-preview/ (hidden: noindex, not linked, a yellow "voorbeelddata" banner).
- **How:** commits on branch **gh-pages** (fast-forward on top of the old 5fcef7dd7):
  - `.github/workflows/vadsrijk-preview.yml` builds main's site exactly like main's pages.yml, adds `vadsrijk-preview/` and deploys;
  - main, the mod release and the server were not touched.
- **gh-pages commits:**
  - 235635bd4 = round 1: LIVE and verified (200 on /, /kaart.html, zone pages, tiles, sample JSON; root and /wiki/ unchanged).
  - 4f9a30412 = round 2: pushed, but its deploy was NOT yet confirmed at the pause. At that moment /vadsrijk-preview/img/renders/... was still 404. The GitHub API was rate-limited, so I could not see the run.
- **Rollback:**
  - Quick: GitHub -> Actions -> "Wiki (GitHub Pages)" -> Run workflow on main. The preview is gone (404) and the normal site stays the same. Any push to main does this too.
  - For good: on gh-pages, `git rm -r vadsrijk-preview .github/workflows/vadsrijk-preview.yml`, commit, push. Then run "Wiki (GitHub Pages)" on main once.

## Left / next steps
1. Check the round-2 deploy:
   - `curl -I https://guhs.nl/vadsrijk-preview/img/renders/baas_zeeslurf.webp` and `.../live-voorbeeld/posities.json` should be 200.
   - If 404: look at the "Vadsrijk preview (GitHub Pages)" run in Actions.
     - If it failed: re-run it.
     - If it was overwritten by a main deploy: push an empty commit to gh-pages.
2. Then report to the lead: URLs, head commit, rollback (as above).
3. Open points (not blocking):
   - the server side of live data (`aan=true`, live.guhs.nl via Caddy/DNS) needs the user's OK;
   - no raid exists yet (hook `WebPosities.VERBORGEN_PLEKKEN`);
   - zone 6 has no site data;
   - no render for Mika-spion, Kloddertje or Honingbij.
4. To update the preview:
   - `python tools/wiki_site/vadsrijk/renders.py` (only when models change), then `python tools/make_vadsrijk_site.py`;
   - copy `site_build/vadsrijk/` over `vadsrijk-preview/` in a gh-pages checkout. The scratch worktree is `<scratchpad>/ghpages` (branch u-site-ghpages); otherwise `git worktree add -b x <dir> origin/gh-pages`;
   - commit and push to gh-pages.
