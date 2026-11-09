# NOTITIES X-ENGELS: English for the older keys (1.9.x and earlier)

Branch `110-x-engels` from b9ef85f4c. No Dutch text was changed (nl_nl.json is untouched).

## Result in numbers
- **1,955 keys that had no English now have it.** They are in 6 new overlay chunks, each registered in `tools/lang/chunks.json` (every key as its own full-key prefix, the same way c413 does it):

  | chunk | keys | content |
  |---|---|---|
  | c420_erfenis_a | 392 | Erfenis: buildings + explanations, deco, deco shop, postcards, commands, goals, errors |
  | c421_erfenis_b | 521 | Erfenis: world map, orders, news, letters, goods, recipes, UI, finds, stats |
  | c422_sherlock_a | 321 | Sherlock Guh: courtroom scenes, titles, clue cards, cases blikje/eieren/kelder (part) |
  | c423_sherlock_b | 220 | Sherlock Guh: cases kelder..wol, licht, paaltjes, voorbereiding, rechtszaak |
  | c424_ftb_oud | 221 | FTB chapter "Een opknapper!" (guhs_erfenis) |
  | c425_oud_registry | 280 | 1.7 mining blocks/ores/biomes, enchantments, Vahoegiet gear, Mijn-guhhuisje GUI, Blub-guh menu, ring flight, titles, signs, emotes |

- **77 stale English values fixed:** these older keys had English whose Dutch had been rewritten.
  - 75 FTB "Guh-technologie" descriptions: tools/ftb_techniek.py rewrote the Dutch, so the old English had other &-codes.
  - 2 sign lines had empty English.
  - All 77 were retranslated in place in c43, c100, c101, c205, c217_*, c172 and c194.
- **16 stale keys removed:** `ftb.guhs_techniek.section.*` keys that no generator makes any more.
- **The checks:**
  - `python tools/vadsrijk/engelscheck.py --alles` reports GEEN ENGELS 0, NL GEWIJZIGD 0, FORMAAT 0, VEROUDERD 0, EN_US OUD 0.
  - `python tools/vadsrijk/engelscheck.py` (the Vadsrijk scope) reports **OK**.
  - `python tools/lang/check_en.py` reports "0 keys without English" (it was 1,955). Its FAIL comes from about 6,900 old "belongs in"/marker errors that were already there.
- **engels_bron.json** is now marked with `--merk --alles` (50,140 keys), so a later change to an old key's Dutch shows up as NL GEWIJZIGD too.

## What --alles still lists (pre-existing, not missing English)
- **GELIJK AAN NL 901:** older chunks whose English equals the Dutch on purpose. Examples: Guhs, Stop, Start, Bonus, Medium, Guhlock Holmes, HOLE-IN-ONE!, Model %s.
  - The default (Vadsrijk) check uses an allowlist for these. Adding all 901 old keys to the allowlist without checking each one was out of scope.
  - My own 26 identical values are justified and added to `tools/vadsrijk/engelscheck_allow.json`: Sherlock Guh, Morris-Mika, Postguh, Toverguh, Bank Guh, Nether, The End, Fruit, Water, Deco, Max, "Ring %s", "(level %s)", "Deco (%s)", "Guhs %s/%s".
- **NEDERLANDS? 369:** older English that contains a marker word. Most are false positives or deliberate 1.6 names, e.g. "Safaripark Vadse Bergen", or "Vahoeg!"/"njeg" written in older English before the wahoog/nyeg convention. None are in c420-c425.

## Conventions (binding for these chunks; the agents' glossary is in the commit message history and below)
- Style as in GLOSSARY.md section 1 and NOTITIES_W_ENGELS: playful and punny, American spelling, names translated, njeg = nyeg, vahoeg = wahoog.
- **Erfenis:**
  - De Erfenis = The Inheritance; Erfhekje = Inheritance Gate; "Een opknapper!" = "A Fixer-Upper!"; "Bouwkundig rapport: vads" = "Structural report: chonky".
  - De Hoeve = the Homestead (Hoeve-level = Homestead level).
  - Guhlden = Guhlders, with the symbol Ƒ kept. Knusheid = Coziness. plukje = clump. kavel = plot.
  - Makelaar Guhrrit = Realtor Guhrrit. Leverkist = Delivery Chest. Decobon = Deco Voucher. Bouwkeet = Site Shed.
  - Bouwmateriaal-loket = Building Supplies Counter. Guhpension = Guh Guesthouse. Vadsmast = Chonk Mast. Dorpszender = Village Transmitter.
  - VK in CP strings = chonk power.
- **People:** Opa Knabbel = Grandpa Nibble, Oma Breiguh = Granny Knitguh, Pim = Pete (as in GLOSSARY_VADSRIJK), Dokter Vadson = Doctor Chonkson, Edelachtbare Knabbelhamer = Your Honor Nibblegavel, Oehoe = Hoo-hoo, Hendrik de Haan = Henry the Rooster, Boerin Bonnie = Farmer Bonnie, Bouwer Bram = Builder Bob, Molenaar Meelmuts = Miller Flourcap, Koos = Cooper (The Chonky Cow).
- **Sherlock:**
  - Het Speurboekje = the Sniffer's Notebook; speurneus = Sniffer (as in the existing overlay).
  - BEZWAAR, NJEG! = OBJECTION, NYEG!; De Grote Knabbelzaak = The Great Nibble Case; Knabbelparking(je) = (Little) Nibble Parking.
  - The 10 cases: The Vanished Eggs, The Trail of Crumbs, The Ghost in the Shed, The Vanished Wool, Grandpa Nibble's Cellar, The Stolen Cowbell, The Moved Posts, Light in the Cellar, The Tin Under the Little Tree, The Preparation.
  - Clue logic: the old box marked "O.K." (Opa Knabbel) is now "G.N." (Grandpa Nibble). The check is text only.
- **Mining:** Viltsteen = Feltstone, Knuffelsteen = Snugglestone, Guhkristalsteen = Guh Crystal Stone. Ores are named "<stone> <ore> Ore". Vahoegiet = Wahoogite.
- Placeholders, &/§ codes, \n and leading/trailing spaces were checked per key by a validator: 0 problems.

## Allowlist
- `tools/vadsrijk/engelscheck_allow.json`: 26 keys added (listed above). Nothing else.

## Shared-file changes
- `tools/lang/chunks.json`: 6 chunks added (c420-c425).
- `tools/lang/en/c43_bbq2_ftb_techniek.json`, `c100_e2_trap5_machines.json`, `c101_e2_trap5_ballon.json`, `c205_techbeurs.json`, `c217_{vadsbasis,kaaskas_ftb,fabriek_ftb,koepel_ftb,centrale_ftb,raket_ftb,fossiel_ftb,hamsterbuis}.json`, `c172_onderwol_structuren1.json`, `c194_grotspellen.json`: values retranslated, and 16 stale keys removed.
- `tools/features/raket.py` (marked `# X-ENGELS`): it no longer writes `c217_raket_ftb.json` during make_ftbquests.
  - Why: tools/ftb_techniek.py overrides those quests' Dutch, so raket.py's built-in English was stale. Every make_ftbquests/apply_en run turned 4 keys back into FORMAAT errors and re-added a dead section key. That file is now kept by hand like the rest of the overlay.
- `tools/vadsrijk/engelscheck_allow.json`, `tools/vadsrijk/engels_bron.json`.
- `src/main/resources/assets/guhs/lang/en_us.json`: rebuilt with `python tools/lang/apply_en.py --no-ftb`. It has 0 keys still Dutch.

## For integration
- On a conflict in en_us.json, take either side, then run `python tools/lang/apply_en.py`.
  - The full run (with FTB) needs the vanilla resources jar for the art step; see NOTITIES_W_ENGELS. Here it crashed in that art step, so I used `--no-ftb`.
  - The FTB English (c424) shows up after integration's make_ftbquests/apply_en run.
- Then `engelscheck.py --alles` should still say GEEN ENGELS 0. New Dutch from other X slices shows up as GEEN ENGELS or NL GEWIJZIGD: translate those keys, then run `--merk --alles`.

## Tests
- `runGameTestServer -Pgt=TaalGameTests,TaalsleutelsGameTests`: **All 11 required tests passed** (log: guhs_vadsrijk/x-engels-gt.log).
- I did not look at English in a client (user rule).

## Open points
- The 901 GELIJK and 369 NEDERLANDS? findings in the older chunks are unreviewed (see above).
- Rookguh: the older overlay has both "Smoke Guh" and "Smokeguh". The new chunks use "Smokeguh".
- Kleurmolen: the older overlay has "Colour Mill" (British); c424 uses "Color Mill".
