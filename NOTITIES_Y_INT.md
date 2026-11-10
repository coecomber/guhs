# NOTITIES Y-INT (round Y integration, branch 110-y-int from 65cd32467)

## 1. Worktree and merges
- Worktree `guhs_vadsrijk/y-int`, run/ copied from x-int without saves/, logs/, screenshots/ and command_history; kept
  `run/defaultconfigs/guhs-vadsrijk-zones.toml` (openZones 6).
- Merged `--no-ff`, in order: y-cosmetica 077eaa8d1, y-vitrine 7f9f4c2a8, y-scheiding 6e03755dd, y-ftb 6b4bd1507.
  - Conflicts only in `tools/lang/chunks.json`, `nl_nl.json`, `en_us.json`, `VadsrijkFeature.java` (three slices each added one
    register line after ZonePoortRegistry) and `tools/features/__init__.py` (vitrine + ftb generator after x_zone1).
  - JSON: a 3-way merge script (my scratchpad `jsonmerge.py`): chunks by chunk id, lang files key by key; a key removed by a slice
    stays removed (y-ftb dropped the old FTB chapter's keys); no key was changed on both sides. Java/py: both lines kept.
    `guhs.mixins.json` merged cleanly (FtbClaimMixin, FtbKeuzeClaimMixin, InventoryScheidingMixin).
  - The first merge commit (cosmetica) has no Co-Authored-By line (slip; the others have it).
- y-ftb's rekruut reward `guhs:vr_vitrine` now resolves (the item exists after the vitrine merge).

## 2. The two user decisions (all marked Y-INT)
### a) Vadsmunten only buy Vadsrijk deco and clothing for home
- New `scheiding/ThuisWaren.mag(stack)`: the one list of what may reach the home inventory through Mollie or the Post:
  every `GuhClothingItem` (Vadsrijk outfits, the ambacht hats/apron, heart glasses), the vr_eco deco blocks (`EcoSpul.DECO`:
  vaandel, klodderreus-knuffel, opperzwambeeld, mini-koning), the guh-kite keepsake `vr_amb_vlieger`, and the Guhs furniture
  Mollie sells (guh_stoel, guh_tafel, guh_bank, every zitzak/kussen colour, vlaggetjes).
- **Mollie** (`huizen/Menus.WAREN`), **removed**: Guh-taart (food), Roze guhbloem x4 and Guhoortjes x4 (flowers: suspicious stew /
  dye), lantern x4, pink carpet x8, pink bed, flower pot x4, cherry planks x32, pink stained glass pane x16, bookshelf x4.
  **Stays** (7): guh_stoel 40, guh_tafel 60, guh_bank 80, roze zitzak 50, magenta kussen 25, vlaggetjes 30, Vadsrijk-vaandel 300.
  Two pages (PER_PAGINA 6).
  - Decision made alone: the Guhs furniture stays. It is pure house decoration (the Knuffelkwartier house is where it goes) and the
    user's words named the vanilla building blocks; flowers and cake went because they have a survival use.
- **The Post** (`economie/Post.stuur`): I traced every caller. Everything it is fed today is already deco or clothing:
  Knutselwinkel "post" goods (vaandel, vlieger, klodderknuffel), Werkplaats post recipes + ambacht keepsakes (`Recepten.POST`:
  kokschort, vissershoedje, tuinhoedje, imkershoed, mijnhelm = Guh clothes; vaandel, klodderknuffel, opperzwambeeld, mini_koning =
  vr_eco deco; vlieger = keepsake), the market's `Cosmetica.KLEDING` (5 vr_item Guh clothes) + `EcoSpul.DECO`, the Knuffelmeter's
  heart glasses. So **nothing was removed** from the Post; the "workshop goods" in the scheiding report were these deco/clothing
  items. `Post.stuur` now refuses anything `ThuisWaren` does not allow: it stays on the Vadsrijk side (`EcoUtil.geefOfBewaar`) with
  a warning in the log. Items already waiting in the Post are still delivered (players keep what they bought).
- Test `VadsrijkYIntGameTests.molliePostAlleenDeco`: every Mollie ware passes, 7 wares, the 9 removed things and a diamond fail
  and are not sold, every Post source passes, cherry planks sent to the Post stay with the player, the vaandel goes by Post.

### b) FTB Essentials work blocks refused inside
- FTB Essentials 26.1.2.4 (the pack's version) registers `anvil`, `crafting`, `smithing`, `stonecutter` (checked in the jar:
  `VirtualInventoryCommand`; no grindstone/loom/cartography). Added to `Grens.GEWEIGERD` with the existing friendly message
  ("/x werkt niet in het Vadsrijk. Naar huis? /guhs vadsrijk"), plus defensive `craft`, `workbench`, `grindstone`, `loom`,
  `cartography` for a later FTB version. `Grens.WERKBANKEN` lists them.
- Test `werkbankCommandosBinnen`: decision function for all four (+ `ftbessentials <x>`), and end to end through the real command
  dispatcher with a stand-in `/stonecutter` (FTB Essentials is not on the gametest server): works at home, refused after the
  real inventory swap, works again at home.

## 3. Small open points
- **blokuitleg.py**: `vr_vitrine` + the 14 `vadsrijk_trofee_*` (NL + EN, no zone names; the trofeeën are not interactive, so
  they are extra, not required by `check_uitleg`).
- **GUHS_OPEN_ZONES**: only `tools/wiki_site/gamedata.py` reads it (the FTB quests of closed zones stay off /wiki/). The
  `--open-zones` option lives in u-site's `tools/make_wiki_site.py`: that now SETS `GUHS_OPEN_ZONES` from `--open-zones` (and
  warns if the environment said something else), so they can't differ. The Vadsrijk site does not read FTB data. Documented in
  RELEASE_CHECKLIST.
- u-site also: `poort.filter_wiki` drops `vadsrijk_trofee_zoneN/geheimN` for N > open and `vadsrijk_trofee_raid` below 6 (their
  names carry zone names: Schemerwortellantaarn, Hoogvads-roosvenstertje...).

## 4. Generators and checks
- Standalone, to confirm the merged lang: `vadsrijk_y_vitrine`, `vadsrijk_y_ftb`, `vadsrijk_t_kleding`. nl_nl unchanged; the png
  byte drift and t_kleding's own (older) English in en_us were reverted (apply_en owns en_us).
- `make_ftbquests.py` (1.21.1 client jar copied from x-int to `build/moddev/artifacts/`) + `apply_en.py`: no change (only CRLF
  noise, reverted). 26 chapters, 1758 quests, 3865 FTB keys, 0 still Dutch.
- `engelscheck --alles`: GEEN ENGELS 0, NL GEWIJZIGD 0, FORMAAT 0, VEROUDERD 0, EN_US OUD 0 (GELIJK 902 / NEDERLANDS? 369 = the
  old unreviewed ones). Three new vitrine format keys ("%s · level %s", "%s (%s) · level %s", " ✔ Zone %s: %s") allowlisted.
  `--merk --alles`: 49,779 keys. Default scope: OK.
- `taalcheck`: 2,982 keys, 0 missing.
- compileJava OK.
- No map changes this round: `git diff 65cd32467` touches no tiles, gpak, *WereldPlekken.java or POI_*.md. No regen.

## 5. Tests
- Focused after the merges + decisions: `-Pgt=VadsrijkYInt,VadsrijkScheiding,VadsrijkYCosmetica,VadsrijkVitrine,VadsrijkFtb,ftbquests,
  VadsrijkUHuizen,VadsrijkEconomie`: **51/51** (`y-int-gt1.log`).
- **Full suite = the WHOLE mod, no filter: 2,778 tests, 11 red** (`y-int-full.log`; x-int's "full suite" was its 29-prefix
  Vadsrijk filter, 502 tests). Re-run alone (`y-int-alone1.log`, 58 tests):
  - round Y's own: `draaiAlleGuhsBlokkenDraaienMee` (vr_vitrine had no rotate/mirror). Fixed, and the two older Vadsrijk blocks the
    same test named (vr_w_portaal axis, vr_z2_baas_lichtbron facing): `Draai,VadsrijkZ2Bazen,VadsrijkWereld,VadsrijkYInt` 15/15.
  - order-dependent, pass alone: spiegelzucht (Z2Bazen 6/6), piepvel (Z3Bazen + Ruiters 8/8), verhaalWolkjesEnTerug.
  - still red alone, outside the Vadsrijk and outside every file this round touched (checked with `git diff 65cd32467 --name-only`):
    Gids kussengevecht x2, Vadsschaar weide_roze_gras, WereldBescherming, TechquestGebouw (both Bescherming), Titels hint
    vr_karavaanvriend, UpstreamSamen SNOES Guhdex page. Never in x-int's filter, so their start is unknown. NOT fixed (open point).
- Quest robot (in the full suite): zone 1-6 41/19/24/23/24/25 gehaald, 0 vast; gate 6 main 13/12/15/7/10/10 (all); gate 1 main
  13/13, other 26/26.
- After the final grenscheck/Grens change: `VadsrijkYInt,VadsrijkScheiding,VadsrijkVitrine,DraaiGameTests` green except the two
  old Draai blocks, which were then fixed (15/15 above).

## 6. AutoCheck: the real border (`tools/autocheck/vadsrijk_y_int.txt`)
- New dev command `/guhs grenscheck start|in|binnen|uit|thuis|vitrine|rapport` (op; `scheiding/GrensCheck` + `GrensCheckFtb`).
  It makes the player count as a non-op for the border rules (`Scheiding.NOOIT_VRIJ`), goes through the REAL dimension change
  and logs every check as "GRENSCHECK PASS|FAIL|SKIP" (log, chat, `run/grenscheck.txt`).
- **Result (gate 1, fresh world): 42 PASS, 0 FAIL, 0 SKIP.** Evidence copied to `guhs_vadsrijk/y-int-autocheck-bewijs/`.
- Lessons on the way (runs 1-3):
  - **Nested commands are queued**: a command started from inside our command only runs after it. The first run's "refused inside"
    checks were trivially true; the positive controls at home exposed it. The command checks now run in a TickTask
    (`GrensCheck.later`).
  - **FTB Essentials 26.1.2.4** has `/enderchest` (no `/ec`) and the work blocks only as `/open anvil|crafting|smithing|stonecutter`
    (gamemaster only). `open` is now in `Grens.GEWEIGERD` (+ test).
  - FTB Essentials as a jar in run/mods broke the GAMETEST server (no ftblibrary there); it is a dev-only `localRuntime` now.
  - **NeoForge 26 reads server configs from `<instance>/config/`** (world `serverconfig/` may override); `defaultconfigs/` is not
    used. The gate for the run was set in `run/config/guhs-vadsrijk-zones.toml` (1) and put back to 6. RELEASE_CHECKLIST step 5
    and SPIEKBRIEF X2.1 corrected. `run/defaultconfigs/` kept at 6 as the brief asked, but it has no effect.
  - The first emerald check ran inside, when the home chunk was unloaded; it is now checked back home.
  - Placement found a spot on top of snow layers, and the block went into the layer: the floor search now treats replaceable
    blocks as room.
  - `close` is not a script verb (`gui close`); the Garderobe needs `gui keep`.
- Screens seen: the vitrine at gate 1 (① ? ? ? ? ?, no raid line, Knuffelhert statue), the FTB book at gate 1 (3 sections; only
  rekruut, zone 1, geheimpjes 1), the Garderobe at home (title, 8 tabs; the "alleen voor de looks" line is cut off at the right:
  left for the user to judge).

## 7. Site (u-site)
- u-site: commit "GUHS_OPEN_ZONES follows --open-zones + filter_wiki for pronkstukken", then merged 110-y-int (twice: also the
  vitrine model and the patch-note fix). One conflict: `docs/patchnotes/1.10.0.md` (kept u-site's `:::zone 3` block + y-int's
  new paths). The patch notes pointed at FTB header pngs that y-ftb moved to `tools/wiki_site/vadsrijk/koppen/`; fixed in y-int.
- Runtime dump from y-int's full suite (`run/vadsrijk_runtime.json`), renders (0 new), vanilla icons, map (tiles unchanged, only
  the timestamp). Committed `docs/wiki/vadsrijk_runtime.json` + `kaart.json`.
- Builds: full 946 pages, 0 broken; zone 1: 218 pages / 380 files, 0 broken, **zone1check 0 leaks**. Main wiki (/wiki/,
  --open-zones 1, scratch): 0 broken, 209/209 interactive blocks with a "Zo werkt het", only rekruut/zone1/geheim1 pronkstuk pages;
  zone1check 33 files + 8 names = the known older namesakes (same as x-site).
- gh-pages **3c0d41d8d**: `vadsrijk-preview/` + `vadsrijk-preview-zone1/` replaced (fast-forward on 5376fee7b). main and the live
  `docs/vadsrijk-site/` were not touched. At the release the prebuilt `docs/vadsrijk-site/` on main must be rebuilt from u-site
  (NOTITIES_SITE_LIVE.md).
- u-site has an uncommitted `LIVE_DATA_PLAN.md` change that is not mine: left alone.

## Shared-file changes (Y-INT)
- `feature/vadsrijk/huizen/Menus.java`: WAREN trimmed, `mollieWaren()` accessor.
- `feature/vadsrijk/economie/Post.java`: the ThuisWaren guard in `stuur`.
- `feature/vadsrijk/scheiding/Grens.java`: GEWEIGERD + WERKBANKEN; `Scheiding.java`: one listener line (grenscheck).
- New: `scheiding/ThuisWaren.java`, `VadsrijkYIntGameTests.java`, `GrensCheck.java`, `GrensCheckFtb.java` (dev command).
- `dev/AutoCheck.java`: `gui ftbquests <prefix> <chapter prefix>` (optional chapter filter).
- `build.gradle` + `gradle.properties`: FTB Essentials 26.1.2.4 as a dev-only `localRuntime` (not in gametest runs, never in the
  jar), so `/ec /home /anvil` exist in the dev client like on the server. (A jar in run/mods broke the gametest server: no
  ftblibrary there.)
- `tools/wiki_site/blokuitleg.py`, `tools/vadsrijk/engelscheck_allow.json`, `engels_bron.json`, `tools/autocheck/vadsrijk_y_int.txt`.
- `feature/vadsrijk/vitrine/VitrineBlock.java` (rotate/mirror), `tools/features/vadsrijk_y_vitrine.py` (models/item/vr_vitrine.json:
  the wiki only lists blocks with a models/item file), `wereld/PortaalBlok.java` + `zone2/bazen/spiegel/KristalBlokken.java` (rotate).
- `docs/patchnotes/1.10.0.md`: header image paths.

## Hand-off
- run/: saves, logs, screenshots, grenscheck.txt, gametestserver and command_history removed; `run/config/guhs-vadsrijk-zones.toml`
  = 6 (the one that counts), `run/defaultconfigs` = 6.
- Docs (Dutch): SPIEKBRIEF.md (ch. 0 Questlopen, ch. 1 Ronde Y, then X1/X2, W1/W2, B), TESTEN_RONDE_Y.md, EINDRAPPORT_RONDE_Y.md,
  RELEASE_CHECKLIST.md (from x-int's: source y-int, GUHS_OPEN_ZONES, config/ path, round-Y post-deploy checks).
