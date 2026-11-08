# NOTITIES W-ENGELS: English for Het Vadsrijk (1.10)

Branch `110-w-engels` from 0f00119f2. No Dutch text was changed. (nl_nl.json is untouched; the only "Dutch-side" change is that one riddle now also accepts an English answer, see below.)

## Result in numbers
- **12,710 keys translated:**
  - 12,577 keys that had no English;
  - 133 FTB keys added by a fresh `make_ftbquests` run (the zone-6 chapter, class-choice quests, 3 keys whose Dutch changed).
- **The 2,826 keys with quick English from earlier slices:** that English was written by the feature generators straight into en_us.json and was NOT in the overlay, so the next `apply_en.py` or `make_resources.py` would have turned it back into Dutch. All of it is now in the overlay, reviewed and brought in line with the glossary.
- **The 688 keys in the round-U overlay files** (c300_vr_t_karakter, c300_vr_u_gilden, c300_vr_u_specs, c310_vr_u_opslagvip, c310_vr_u_tabs): reviewed and edited in place (275 changed, e.g. "Cuddle Bond" -> Snuggle Squad, "the Vadsrijk" -> the Chonkdom).
- **`python tools/vadsrijk/engelscheck.py`:** 13,343 keys in scope, all categories 0, **OK**.
- **`tools/lang/check_en.py`:** all c4xx_vr and c300/c310_vr chunks have 0 todo and 0 errors (except one deliberate command word, see the allowlist).
  - The repo-wide FAIL (about 7,000 errors) was there before this slice: older chunks, FTB &-codes, misfiled keys.

## Where the English lives (convention: the overlay is the ONE source)
- `tools/lang/en/c401_vr_npc_quests.json` ... `c414_vr_ftb.json`: 14 files, split by key family. Each is registered in `tools/lang/chunks.json`; c413 lists 168 single keys outside vadsrijk.* by full key. The round-U files c300/c310 are now registered per key too.
- en_us.json = nl_nl + the overlay, built by `python tools/lang/apply_en.py` (`--no-ftb` = only the mod's en_us).
- Name glossary (binding): `tools/lang/GLOSSARY_VADSRIJK.md`, with the machine copy `tools/lang/glossary_vadsrijk.json` (about 1,900 names). It adds to `tools/lang/GLOSSARY.md` (1.2.0).

## The check for integration (task 3)
```
python tools/vadsrijk/engelscheck.py                  # report; exit 1 if anything needs work
python tools/vadsrijk/engelscheck.py --lijst          # every key, not just 25 per category
python tools/vadsrijk/engelscheck.py --todo todo.json # the keys that need (new) English, for a translator
python tools/vadsrijk/engelscheck.py --merk           # after translating: remember the current Dutch (engels_bron.json)
```
Categories:
- **GEEN ENGELS:** not in the overlay.
- **NL GEWIJZIGD:** the Dutch changed after translating. It is tracked by a hash in `tools/vadsrijk/engels_bron.json`.
- **GELIJK AAN NL:** the same as the Dutch. The allowlist is `tools/vadsrijk/engelscheck_allow.json`.
- **NEDERLANDS?:** check_en's Dutch-word list plus Vadsrijk names and words. Command text after `/guhs` and `/bond` is skipped.
- **FORMAAT:** %s, §, &, \n or {0} differ.
- **VEROUDERD:** overlay keys whose source is gone.
- **EN_US OUD:** run apply_en.

Scope = keys with vadsrijk / .vr_ / ftb.guhs_vadsrijk, plus every key that was not in v1.9.7's nl_nl (git tag).

### Integration recipe after merging the other W slices
1. Merge. **On a conflict in `en_us.json`, take either side.** It is generated and step 3 rebuilds it. (`source_ftb.json` is the same, generated.)
2. Run the generators the slices need (vadsrijk_*.py write Dutch into en_us; that is fine), then `python tools/make_ftbquests.py`.
   - The art step of make_ftbquests needs the vanilla resources jar at `build/moddev/artifacts/neoforge-21.1.251-client-extra-aka-minecraft-resources.jar`. That jar does not exist on 26.1. Copy `%APPDATA%/PrismLauncher/libraries/com/mojang/minecraft/1.21.1/minecraft-1.21.1-client.jar` to that path (build/ is gitignored); only the font is read.
   - I did NOT commit the FTB outputs (chapters/lang json5, header pictures). v-int's FTB chapter was stale (no zone 6), and the brief says generated outputs are regenerated at integration. Only `tools/lang/source_ftb.json` is committed. So the English FTB book appears after integration's make_ftbquests run.
3. `python tools/vadsrijk/engelscheck.py --todo todo.json`. Translate the keys in todo.json (follow GLOSSARY_VADSRIJK.md and the style in GLOSSARY.md), and put them in `tools/lang/en/c415_vr_aanvulling.json`.
   - A key must be in exactly ONE overlay file, or apply_en crashes. For an NL GEWIJZIGD key, edit it where it already is.
   - Register the new file in chunks.json, or accept "belongs in" errors in check_en. engelscheck does not care.
4. `python tools/lang/apply_en.py` (rebuilds en_us + the FTB texts), then `engelscheck.py` must say OK, then `engelscheck.py --merk`.

Expected top-ups from round W: area labels, the cave-dungeon GUI, the Questlopen buttons (other slices are still changing these).

## Conventions used
- **Tone:** the 1.2.0 style (GLOSSARY.md section 1): playful, warm, punny, American spelling, Title Case for names, puns translated as puns.
- **Names are translated** (the 1.2.0 user decision: "translate everything, places and characters too"). Never changed: guh/guhs, Guh!, Mika, Guhdex, Guhmension.
- **Key forms:**
  - Het Vadsrijk = **The Chonkdom**
  - Vads (the bar) = **Chonk**; vadsig = chonky; Vadsmunten = **Chonk Coins**
  - de Vadsloosheid = **the Chonklessness**; Uitgevadsten (the refugees) = **the Unchonked**; ontvadst (gray creatures) = **dechonked**
  - De Knuffelorde = **the Snuggle Order**; Knuffelkamp = **Camp Snuggle**; Knuffelbond = **Snuggle Squad**
  - Classes: Knuffelridder / Pluisjager / Vadsmagiër = **Snuggle Knight / Fluff Hunter / Chonk Mage**
  - ridder(s) = knight(s); the title "Ridder X" = "Sir X"
  - Vahoegnir = **Wahoognir**
  - Zones and towns: Zwamzoet = Shroomsbury, Kandijkom = Sugarbowl, Honingtak = Honeybough, Lampionkust = Lantern Coast, Hoogvads = Highchonk, Zijdeland = the Silklands, Saffraanpoort = Saffron Gate
  - The Emperor's hand ranks: Knokkel = Knuckle, Kootje = Pinkie, de Pink = the Little Finger, de Ringvinger = the Ring Finger
  - verpluisd = frayed; Guhddelijk (top rarity) = Guhvine
  - The full list: GLOSSARY_VADSRIJK.md.
- **Formatting:** placeholders, § and & codes, \n, symbols and leading/trailing spaces are kept exactly. The validator checked every key.
- **Command text** (`/guhs vadsrijk ...`, `<naam>`) stays as typed.
- **Price codes** "2z 50b" stay Dutch letters. `MarktScherm.leesPrijs` only parses g/z/b.

## Allowlist (`tools/vadsrijk/engelscheck_allow.json`)
- **74 keys** whose English is the same as the Dutch. Examples: Water, Elite, Ring, Cape, Aura, Blink, Quests, Info, Tip/Tap (twins), Splinterling, Guhwolf, Baron Bleu, Mika, Duet, Wind, tier N, Level %s, lv %s, %s / %s xp, Zone %s, Contract, Villa, Mint, Confetti, Wipe!, Emotes, Party, Tips, Stop, offline, "Plink.", "Tink!", HRRROOOAAAR!, 50 EURO, Level: minimum.
- **1 key** with a Dutch command argument on purpose: `vadsrijk.tmodels.cmd.onbekend` ("alles" is the argument you type).
- **A value list:** Guhdex, Mika, Level %s, Stats, Perks, Quests and so on.

## Shared-file changes (all minimal)
- `tools/features/vadsrijk_t_puzzels.py` + `src/main/resources/data/guhs/vadsrijk_puzzels/z1_fluistersteen.json`: the z1 whisper-stone riddle (answer "vads") also accepts **chonk / the chonk / chonky**, because the English hint now points to CHONK (`// W-ENGELS` comment in the generator). z2 (ice) and z3 (bee) already accepted the English answer.
- `tools/lang/chunks.json`: 14 new chunks, plus the c300/c310 files registered per key.
- `tools/lang/en/c300_*.json`, `c310_*.json`: values edited in place, no keys added or removed.
- `tools/lang/source_ftb.json`: regenerated by make_ftbquests (the current Dutch FTB source, including zone 6).
- `src/main/resources/assets/guhs/lang/en_us.json`: rebuilt by apply_en.py.

## Tests
- `runGameTestServer -Pgt=TaalsleutelsGameTests,TaalGameTests`: see the final report for the result.
- I did not look at anything in a client (user rule: English is only checked for completeness).

## Open points / doubts
- **Not done: older keys outside the Vadsrijk/1.10 scope** are still Dutch in en_us: about 1,955 keys with no English (about 1,480 mod keys, mostly 1.9.x Erfenis/Sherlock, plus FTB erfenis keys).
  - `python tools/vadsrijk/engelscheck.py --alles` lists them; `check_en.py --complete` shows them too.
- **Not done: Java `Component.literal` with Dutch** in the vadsrijk package. These are all op/dev commands (tp, reset, debug output, `/guhs vadsrijk gilde ...` with permission level 2), so they were left alone. Normal players see none of them.
- **Translator choices someone may want to look at:**
  - Bobbel = Lumpy (Granny Date's humpguh) and Bobbeltje = Lumpkin (her baby).
  - Uitkijkpunten = "Lookouts" (the spires you climb) and Uitzichtpunten = "Viewpoints", to keep the two Exploration Book categories apart.
  - Raddie = Wheelie (a guess).
  - Jachtbord = Hunt Board and premiebord = Bounty Board, kept apart.
- **Initials in the zone-5 mystery** now follow the English names (Z.P. -> S.P. for Softpelt, Z. -> S. for Silvertail). The clue logic is text only, so nothing breaks.
- **Source typos seen and translated correctly** (the Dutch is not changed): "ten zuiden van in Kandijkom", "het splinter", "in het de Kristalkapel", "ru?ne".
