# NOTITIES_U_OPSLAGVIP — round U, slice "opslagvip" (branch 110-u-opslagvip, from 110-v-int 9607dde82)

The request: only the FIRST bought storage expansion can be bought now (knapzak + bank), the rest shows "Binnenkort!", with a
configurable exponential price curve; plus the "WORD GUH VIP++" parody overlay the first time an account tries to buy one.
Package `feature/vadsrijk/opslag` (t-opslag's own package), generator `tools/features/vadsrijk_u_opslagvip.py`.

## What was built
- **`OpslagBalans`**: section `[opslag]` in `config/guhs-vadsrijk-balans.toml` (through klassen's `KlasseBalans`, live reload):
  - `max_koopbaar` = 1;
  - `knapzak_basis` = 2500 and `knapzak_factor` = 4;
  - `bank_basis` = 1000 and `bank_factor` = 4.
  - Price of the n-th bought expansion (n = 0 first) = basis x factor^n. The factor 4 is a PLACEHOLDER; the user picks the numbers.
  - `/guhs vadsrijk opslag prijzen [sleutel waarde]` shows the curve or sets a value (also written to the file). The generic
    `/guhs vadsrijk balans` command can't take 2500, because its max is 100.
- **Knapzak:**
  - The bought count `Zak.gekochtN` replaces the old boolean. `gekochtAantal()` = max(gekochtN, gekocht?1:0), so old saves count
    as 1, and the boolean is still written.
  - `koopbaar(p)`, `prijs(p)` and `binnenkortVanaf(p)` (= open + pending story sources + purchases still allowed).
  - `Knapzak.PRIJS` is gone.
- **Bank:** the fixed `PRIJZEN` array became the curve. `koopbaar/prijs/binnenkortVanaf` work the same way. Players who already
  bought more tabs (up to 4) keep them.
- **Screen info:** `prijs`, plus `koop_prijs` (the price in the tooltip line, also when nothing is for sale), `binnenkort`, `vip`,
  and `vip_nu` (dev only).
- **`OpslagScherm`:** a tab at index >= `binnenkort` that is not buyable is a darker grey flap with a little padlock. Its
  tooltip says "Binnenkort!" plus an explanation.
- **The joke:**
  - `VipGrap` (server: the flag `OpslagData.vipGrapGezien` in the account-wide attachment, saved as `vip_grap`) and
    `client/VipGrapOverlay`. The overlay is drawn ON TOP of the storage screen with no screen switch, so the server menu stays
    open and "Kopen" is a normal `KNOP_KOOP`.
  - First click on a "+" flap while `vip` is false starts the overlay. The client sends `OpslagMenu.KNOP_VIP_GEZIEN` (43) right
    away, so the flag is set even if the game closes mid-joke. After that the click works as the normal two-click confirmation.
  - The overlay: flashing/pulsing title, struck-through price, gold border with blinking bulbs, confetti and sparkles, badges,
    a countdown 00:09 to 00:00, a pulsing KOOP NU!!! button with a tilted 97% sticker, a bobbing red arrow, a guilt-trip "nee"
    link, a fake buyer counter and a disclaimer line.
  - Jingle = vanilla note-block bell + chime arpeggio, then a "ka-ching" (xp orb + bell + `guhpixel.muntje`). Buttons shake
    the overlay (bass + villager-no sound, "Er gebeurt niks. Dat is de grap.").
  - After 10 s (or Esc) a dialogue in the RijkPraatScherm style: pink frame, name plate "Winkelguh Snoes", a guh portrait with
    a red bow tie and straw hat, typing text with the coin price. "Kopen" (1/Enter) / "Nee, dank je" (2/Esc).
- Dev commands, under `/guhs vadsrijk opslag`:
  - `vipgrap`: reset + open the knapzak with the joke at once;
  - `vipgrap reset`;
  - `vipgrap stand`;
  - `prijzen [sleutel waarde]`;
  - `knapzak gekocht <n>`.
- Gametests `VadsrijkUOpslagVipGameTests` (batch `vadsrijk_u_opslagvip`): `alleenEersteKoopbaar`, `vipVlagEenKeerPerAccount`.

## Decisions I made myself
- **Free/story unlocks unchanged:** knapzak tab 1 + zone1 + zone2 + `ontgrendel(...)` sources. No conflict.
  - With max_koopbaar 1, a new character has: 1 open, 2 story "?", 1 "+", and tab 5 "Binnenkort!".
  - The `ontgrendel` "later reward" sources don't exist yet, so they fall under "Binnenkort!" too. When a quest gives one, the
    tab just opens (`ontgrendel` still works past the binnenkort line; tabs stay capped at MAX_TABS = 5).
- One `max_koopbaar` for both storages (as asked: `opslag.max_koopbaar = 1`), counted per storage (knapzak per character,
  bank per account).
- The joke shows for knapzak OR bank, whichever is tried first: one flag per account.
- The guilt-trip "nee" link and the KOOP NU button both do nothing. Only Esc skips the joke (so nobody is ever stuck 10 s).
- Emoji (⏰, 🔥) are in the lang text. Modern MC renders them with its unifont glyphs; if they show as boxes, swap them in the
  generator.
- Bond vault tabs (u-gilden, guild levels) not touched, as asked. No other purchasable storage expansions exist in the Vadsrijk:
  the materiaaltas is unlimited and the AmbachtTas has no purchase.

## Shared-file changes (add-only, marked `// U-OPSLAGVIP`)
- `klassen/KlasseBalans.java`: constant `OPSLAG` plus the section in `standaard/secties/sleutels/pasToe` and 2 comment lines
  in the file header.
- `tools/features/__init__.py`: +1 FEATURES entry (after `vadsrijk_u_z6raid`).
- `lang nl_nl/en_us`: +22 keys (`vadsrijk.opslag.tab.binnenkort*`, `vadsrijk.opslag.vip.*`), plus
  `tools/lang/en/c310_vr_u_opslagvip.json`.
- t-opslag's own files (Knapzak, Bank, OpslagData, OpslagMenu, OpslagCommando, client/OpslagScherm) changed in place.

## Tests
- `runGameTestServer -Pgt=VadsrijkUOpslagVip,VadsrijkTOpslag`: 8/8 pass (2 new + the 6 existing T-OPSLAG tests).

## Open points
- Not seen in a client: the overlay layout at GUI scale 2-4, inside the Vadsrijk-menu frame (knapzak, `Geschaald`) vs the bank
  screen. Also whether U-TABS' menu Pre-events catch clicks on the top band before the overlay does.
- The real price factors: the user decides (`/guhs vadsrijk opslag prijzen ...` or the toml).
