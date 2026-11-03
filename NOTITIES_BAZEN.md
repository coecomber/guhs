# NOTITIES_BAZEN — slice "bazen" of Guhs 1.10.0 "Het Vadsrijk"

Branch `110-vr-bazen`, worktree `guhs_vadsrijk/w-bazen`. Package `feature/vadsrijk/bazen`, ids `vr_baas_*`, lang `vadsrijk.bazen.*`,
generator `tools/features/vadsrijk_bazen.py` (writes models/textures/icons/blocks/lang + `blockbench/vadsrijk/*.bbmodel`).

## What is in
- **Koning Klodder** (`koning/KoningKlodder`, arena `bazen:koning_klodder`, lv 8, respawn 20 min, HP scales via kern Arenas).
  Jumps (red shadow), shock wave; at 60 % splits into 3 Kroonklodders (`koning/ElementKlodder`, Roze/Water(blauw)/Vuur(geel)).
  Rule in `koning/SplitRegel`: after the first is cheered up, 12 s (240 ticks) for the other two; else survivors melt back, he heals
  8 % per survivor and splits again 20 s later; no first kill within 60 s = fail too. Success = he re-forms at 35 % = fase 3.
  Fase 3: Hemelsprong every ~22 s (whole arena dark-dust shadow + 3 pink safe circles; 55 % of max Vads as true damage outside).
- **De Opperzwam** (`opperzwam/Opperzwam`, arena `bazen:opperzwam`, lv 19, respawn 5 min). Fase 1 roots (pink circles under each
  player, Plakkerig) + spore clouds (one pink safe quadrant; Gesporen). Fase 2 (66 %): dark: Darkness effect + client fog
  (`BazenClient.onFog`), he renders as eyes only (`opperzwam_donker.png` is transparent except the eyes + glow layer), nameplate hidden,
  takes 25 % damage, hunts with pounces. Light the 4 Lichtzwammen (centre ±13) → stunned 5 s + 8 % damage. Fase 3 (33 %): tilt scene
  (rocking camera, 4 s), scale attribute → 2.1, ceiling (radius 11, y +12..+20) breaks open ring by ring.
  **Reset**: every changed block is in `Herstel` (SavedData `guhs:vr_bazen_herstel` in the Vadsrijk level), restored 10 s after the
  fight, when he is removed by a reset/kill, on every new spawn, and at every server start.
- **Zwamtempel** (`opperzwam/Zwamtempel`): door ceremony (scene + title "DE ZWAMTEMPEL", min level 16 = `MAG_NAAR_BINNEN`),
  Lichtzwamkamer (Simon-says order of 5), Hendelkamer (4 levers, mirrored hint mushrooms, pressure plate checks), Ridderzaal
  (3 waves, last with a Sporenpriester elite). Rooms reset 10 min after solved when empty. Gates = own `vr_baas_zwampoort` blocks
  placed only in air.
- **De Mika-spion** (`mika/MikaSpion`, lv 15): stands disguised as "Akim de Kaasguh" at the market (mika model, pink texture with
  guh eyes, glasses and a fake snor). `MikaSpion.ontmasker(ServerPlayer)` = the npc slice's entry point (returns false if he's not
  there). Reveal scene (8 s, disguise poofs at 1.5 s, he arcs onto cap 1), then the rooftop fight on 3 caps (fases at 70/40 %:
  he hops caps; after 3 s the "sporenkatapult" of the guhs puts the fighters on the new cap), smoke-bomb blinks, ontvadsings-poeder
  (grey circles → Gesporen), 3 grey kloddertjes per 20 s (kern `vr_proefkloddertje`, max 6), at 15 %: balloon (`mika/SpionBallon`,
  6 % of his HP, 20 s to pop). Popped → caught in a knuffelnet → chests on the cap + cliffhanger (chat + title card).
  Escaped / abandoned 60 s → back at the stall after 2 min. After a capture he's back in disguise after 10 min (shared world).
  Not saved to disk: after a restart Akim simply stands at his stall again.
- **Loot** (`BazenLoot`, effects `BazenEffecten`, all chest loot **gebonden**). First kill per player (SpelerData `extra("bazen")`
  `eerste_<baas>`) guarantees one named Legendary + the trophy; trophy otherwise 25 %.
  | item | boss | rarity / chance | effect |
  |---|---|---|---|
  | `vr_baas_klodderknots` Koninklijke Klodderknots (knots, ridder, Water) | KK | Legendarisch 8 % | every 5th hit a pink mini-slijmpje (`vr_baas_hulpslijmpje`) fights along 8 s |
  | `vr_baas_pluisboog` Plakkerige Pluisboog (boog, jager) | KK | Leg. 8 % | hits make the target Plakkerig 2 s |
  | `vr_baas_klodderkroontje` Het Klodderkroontje (head) | KK | **Guhddelijk** 2 % | crown sparkles above your head + every 30 s 5 s Vahoeg for you and party within 8 |
  | `vr_baas_sporenstaf` Sporenstaf (staf, magier, Natuur) | OZ | Leg. 10 % | crits leave a spore cloud (Gesporen around the target) |
  | `vr_baas_wortelwambuis` (chest) | OZ | Leg. 10 % | 15 % when hit: roots stun the attacker 1 s |
  | `vr_baas_lichtzwamhelm` (head) | OZ | Leg. 10 % | immune to darkness/blindness and the Opperzwam fog |
  | `vr_baas_rookbomlaarzen` (feet) | Mika | Leg. 15 % | below 30 % Vads: poof 6 blocks away + Snelpootjes (30 s cooldown) |
  | `vr_baas_neppe_snor` De Neppe Snor van de Spion (head) | Mika | **Guhddelijk**, 100 % first kill, then 3 % | 12 % when hit: enemies lose you, Snelpootjes (15 s cd) |
  Random gear in the chest comes from `economie:baas_<baas>` → `economie:zone1` → `kern:klodderreus` (first that exists).
- **Trophies** for the economie slice (deco): `guhs:vr_baas_trofee_koning_klodder`, `guhs:vr_baas_trofee_opperzwam`,
  `guhs:vr_baas_trofee_mika_spion` (plain items, stack 16, NOT gebonden so they can go home).
- **Music hooks**: sound events `guhs:vr.bazen.muziek.koning_klodder|opperzwam|mika_spion` registered, no sounds.json/ogg.
  `BazenMuziek.AAN = false` → nothing plays until audio exists (start on intro/reveal, stop at defeat).

## Kern adapters / things integration must lift
- `BazenBaas.beloon`: kern's `VadsrijkBaas.beloon` always rolls its own table; we hide the table during `super.beloon` (xp + arena
  notify stay kern's) and place our own chests. Cleaner: a kern hook `protected LootTabel.Resultaat kistInhoud(ServerPlayer)`.
- The Mika fight is NOT a registered kern Arena (he must only appear via `ontmasker`); he sends `KernPayloads.BaasBalk` himself.
  Kern could add `Arenas.registreerHandmatig` (no auto spawn).
- `Adapters.spawn`: wezens mob ids guessed as `guhs:vr_mob_tempelzwamridder` and `guhs:vr_mob_sporenpriester`; missing → kern
  `vr_proefklodder` (Natuur, lv 18-19). Check the real ids.
- `Adapters.deurOpen/deurDicht`: reflection on `wereld.WereldDeuren.open/sluit(ServerLevel, String)` with id `"zwamtempel"`;
  missing → our own sliding fallback with `Herstel`.
- Weapon kinds for the named weapons: `knots` (ridder), `boog` (jager), `staf` (magier) — must match the klassen slice's `wapenSoorten`.
- Kern's dev command `/guhs vadsrijk baas reset <arena>` works for our arenas (`bazen:koning_klodder`, `bazen:opperzwam`).
- Kern already counts tank (NA_KLAP) and heal (NA_HEAL in a registered arena) contribution; heal during the Mika fight does not count.

## Coordinates
All in `BazenPlekken.java`; requests in `guhs_vadsrijk/POI_VERZOEKEN.md` section "bazen". The Grauwe Klodderreus stays kern's
(proeftuin arena); if it moves to the Knuffelkamp pond that is a kern/wereld POI change only — bazen does not touch it.

## Open choices for the user
- Balance numbers are first guesses (HP: KK 2000+200·lv, OZ 4000+250·lv, Mika 2400+150·lv; split window 12 s; Hemelsprong 55 %).
- The "tilting hall" is a rocking camera + growth + ceiling; the floor itself does not tilt (blocks stay).
- Mika fight: the players are teleported cap to cap by the "sporenkatapult" instead of having to climb/bridge after him.
- Mika comes back 10 min after a capture (so everyone can fight him); story-wise "he escaped from the Knuffelorde cell".
- Unmasking by right-click is OFF (`MikaSpion.MAG_ONTMASKEREN`); the npc quest decides. Dev: `/guhs vadsrijk bazen mika ontmasker`.
- Temple entry needs level 16 (`Zwamtempel.MAG_NAAR_BINNEN`, npc may replace with quest progress).
- Item art and boss models are generated placeholders-plus (Blockbench sources in `blockbench/vadsrijk/`).

## Resume point
Done: everything above compiles; gametests `-Pgt=VadsrijkBazen` (split rule, KK split/heal on the entity, Opperzwam phases +
restore, contribution → chest + bound loot): **4/4 passed** (2026-11-03). No dev-client/AutoCheck run was done (slot time):
the GeckoLib models/textures are only verified by the generator, not rendered in a client yet — integration's AutoCheck should look.
See TESTEN_BAZEN.md for the Dutch test guide.
