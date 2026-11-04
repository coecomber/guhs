# NOTITIES_Z3_NPC — Vadsrijk zone 3 "De Schemerwortel", slice npc

Branch `110-z3-npc`, worktree `guhs_vadsrijk/z3-npc`, package `feature/vadsrijk/zone3/npc`, generator
`tools/features/vadsrijk_z3_npc.py` (in FEATURES), wiki `tools/features/vadsrijk_z3_npc_wiki.py` (not a feature, like zone 1/2's).
**Merged in:** `110-i-voltooiing` (which contains `110-i-ui`), for the Verkenningsboek API.

## Approach: zone 1's quest SYSTEM, zone-3 DATA (as zone 2)
- quests: `data/guhs/vadsrijk/npc/quests_z3.json` (`Quests.BESTAND_Z3`, read after quests_z2.json; ids `z3_*`), texts under the
  shared keys `vadsrijk.npc.q|naam|praat|plek|gids|tip.z3_*`, zone-3-own texts `vadsrijk.z3npc.*`.
- every objective has an explicit `plek` (npc:/poi:/gebied:/baas:) + a where text and says WHO + WHERE + WHAT (generator asserts,
  gametest checks). The tracker hop between zone 3 and the rest goes through `QuestStap.ROUTES` (Z3Haken.route: to Kapitein Zoemzeil
  at Honingtak's mast, or to Poolster/Zeppie).
- NPCs via `Npcs.voegToe`: **49** (Honingtak 28: kapitein, Raatje, Wasja, Veertje, café, Jagersguh Raap (premiebord), smid,
  juwelier, winkel, wapens, kleermaker, markt, stalguh, liftguh + 14 citizens; out in the zone 9: Kruinwachter Blad, Lusje +
  herbergier (Hangnest), Oude Knoest, Mosvoet 3, Wortelwacht Gloei, de Oude Nestwachter; Honingwacht 12 at the campfires/plaza).
  Clothes: striped jumpers + BZG_STROHOED/ZONNEHOEDJE/TUINSCHORTJE/BLOESEMKRANSJE (no bee skin exists).

### Small, additive changes outside zone3/npc (check at integration)
| file | change |
|---|---|
| `npc/Quests` | `BESTAND_Z3` read after zone 2 |
| `npc/QuestMotor` | the airship case also for role `luchtschip_z3` (one label) |
| `npc/client/QuestlogScherm` | 6 Gids ids `z3_*` appended |
| `VadsrijkFeature` | one line `zone3.npc.Z3NpcRegistry.register` |
| `tools/features/__init__.py` | `vadsrijk_z3_npc` appended to FEATURES |
| `tools/features/vadsrijk_npc.py` | `_direct` keeps all `z[2-9]_` keys/advancements (regex) |
| **zone 2 cliffhanger (brief item 6)** | `Z2NpcPlekken.CLIFFHANGER` looks EAST (+900/+1000 x), `vadsrijk_z2_npc.py`: "Kijk eens naar het oosten" + "Ver in het oosten, achter de wolken, staat een boom zo groot als een berg."; Z2Verhaal javadoc |

**Travel is z3-wereld's** (their branch already edits QuestMotor REIZEN/reisSlot/vliegNaar, Reiskaart and WereldLucht with
`Z3Reizen`): I reverted my own versions of those edits to avoid conflicts. Our side: action `z3_vlieg` (QuestMotor.ACTIES, called by
their `vliegNaar("schemerwortel")`) starts z3_hq01 when available, then calls `Z3Reizen.naarHoningtak` by reflection (gate + landing
+ their opening), and only when Z3Reizen is absent our own flight/opening. Request: add `schemerwortel` to `luchtschipper` (De
Haven) too — POI_VERZOEKEN_Z3.md.

## Coordinates
`Z3NpcPlekken` = POI_Z3 v1 (z3-wereld, MIRRORED layout: Honingtak on the west branch x 1634..2014, riding branches + Hangnest +
werf in the east, Mosvoet SW, schimmelboerderij NE), every spot `w("<POI_Z3 id>", fallback)` looked up in
`Z3WereldPlekken.plek(id)` by reflection. Requests in `../POI_VERZOEKEN_Z3.md` (only `kuiken_1..6`). Texts use these directions
(the riding branches are "de lange tak / noordoosttak / zuidoosttak" in my texts, never "westtak").

## Content
- **Main quest (15, ZONE3_PLAN §7 ✎):** hq01 Poolster (Kandijkom) -> fly -> Raatje; hq02 Wasja in the Nestkapel, BAM (zeppelin over
  the market, 8 Bladrovers; pamphlet "Operatie Rijpe Kaas ... Graaf Piepvel", Wasja "Mika's met titels?"); **hq03 De Pluimstal:
  Veertje's offer has the chocobo joke verbatim, `actie_aanname z3_starter` -> `Rijdieren.kiesStarter(p)`; steps: pick (STARTER_GEKOZEN
  -> meld rijdier:starter), mount (on a RijdierEntity), ride to the zweefplatform, glide down (riding 8+ below the platform within 90),
  title "Je eerste rit!"**; hq04 grauwe vlekken + Mika-bevel; hq05 the old nest: flashback scene (pink dragon lays the egg), splinter
  2/5; hq06 Raatje + Piepje (bald mouse in the Raattrossen, quest item "in je tas"); hq07 Hangnest Dauwdrup (Lusje, 3 Kaasschutter-
  posten, kills); hq08 Kaaszeppelinwerf (Taleggio, Brokkel, Roquefort via meld; kajuit = Boomvolkkaart, splinter 3/5, Bleu's letter,
  the first Grauwe brief); hq09 Stamspiraal -> Oude Knoest, Honinglift opens; hq10 7 lantern rings (cages open, mice run; Nestlantaarn
  at ring 4), Graaf Piepvel (meld), **flash F1** (the long grey arm grabs the Count; fingerprint POI), splinter 4/5 = the cape pin;
  hq11 Wasja ("Een keizer.") -> Mosvoet, 2 kettles; hq12 Wortelduister: Wortelpoort, Hartslag-zaal (second heartbeat), 4 cave
  lanterns; hq13 Hart van de Boom: Wortelwurger, Nestwachter (opgevrolijkt -> splinter 5/5), Baron Bleu, the egg scene; hand-in at
  the Nestwachter -> hq14 starts + the **escape ride** scene up the Stamspiraal -> at the nest `NpcRollen z3_vahoegnir` (z3-bazen
  starts the deelbaas; without it the story goes on) -> meld z3_vahoegnir:opgevrolijkt -> **finale** (splinters merge, black fluff
  blows off, egg hatches, sneeze, giggle, **Vahoegje** pet, the pink beam east to Hoogvads, he flies off) -> bloom title +
  **`zone3_klaar`**; hq15 Raatje goodbye -> Wasja -> **cliffhanger**: balcony view east, then **flash F2** (throne room, Krabbel's
  report, five fingers tapping, "Dan... reik... ik... verder.", "Wordt vervolgd... Ver in het oosten strekt iets zijn armen.").
- **Side (5, light):** Veertjes zoek (6 Pluimkuikens), Opa Bast to the top (ESCORTE), Thee in het moeras, Knoest z'n vrienden (4
  lights), Lusje's kaartjes (5 lookouts). **Job advancement:** `z3_adv50_<klasse>` at class lv 50 (after hq13), action
  `advancement:<k>:4` (titles Nestwachter / Kruinvalk / Schemerwijze, `vadsrijk.npc.adv.<k>.4`). **Secret:** z3_geheim_imker (5 clues
  -> `NpcRollen z3_imker` / reflection `zone3.bazen.MikaImker#ontgrendel`, then meld z3_imker:verslagen).
- **Secrets:** 10 knuffels, 5 clues, 5 treasures, 8 Muurverhaal reliefs (all 8 = the flashback text + adv `vr_z3_reliefs_alle`),
  the Grauwe brief (adv `vr_z3_grauwe_brief`, map `z3_brieven`), the fingerprint after hq10.
- **Pet Vahoegje:** `Huisdieren.registreer(...)` in Z3NpcRegistry (model = the dragon `vadsrijk/wezens/draak`, scale 0.16, pink
  recolour `textures/vadsrijk/zone3/vahoegje.png` + `vr_huisdier_vahoegje.animation.json` idle/hop/sprong, generated); given in the
  finale (`Huisdieren.geef`). Boss pets (Kruinroofvogel, Piepje) are z3-bazen's.
- **Verkenningsboek:** `Voltooiing.registreer(ZoneBoek "zone3", title vr_kenner_van_de_schemerwortel, 60000 brons)`: main, side,
  secrets (clues + treasures + reliefs), knuffels, bosses (Roquefort, Kruinroofvogel, Piepvel, Bleu, Vahoegnir, Mika-imker(secret);
  entity ids guessed `guhs:vr_z3_baas_<naam>` + quest fallback), pets (zone 3), Pluimguh, areas (names = z3-wereld's
  `vadsrijk.kern.gebied.z3_*`). Not yet: lookouts/campfires (need `Z3WereldPlekken.UITKIJKEN/KAMPVUREN` typed lists: add at integration
  with `ZoneBoeken.uitkijken/kampvuren`).
- **Bounty board:** Jagersguh Raap (Jagershut, role `premiebord`): binding wins, else reflection `zone3.systemen.Premiebord#open(p)`,
  else a "nog leeg" line. Logic/rewards = z3-systemen.
- **Guides:** Gids z3_schemerwortel/pluimguh/lantaarn/premiebord/reizen/titel; tips z3_monsters/z3_lantaarn/z3_zweef; FTB sections
  (chapter guhs_vadsrijk); wiki pages in vadsrijk_z3_npc_wiki.py (integration: add to the docs step).

## Contract for the other zone-3 slices (Z3NpcApi javadoc)
- z3-bazen: `NpcApi.meld(p, "z3_taleggio|z3_brokkel|z3_roquefort|z3_piepvel|z3_wortelwurger|z3_bleu|z3_imker", "verslagen")`,
  `meld(p, "z3_nestwachter", "opgevrolijkt")`, `meld(p, "z3_vahoegnir", "opgevrolijkt")`; we also catch kills of entity ids
  `vr_z3_*<naam>*` ourselves. Bind `z3_vahoegnir` (start the deelbaas at the nest for this player/party).
- z3-wezens: bind `z3_bladrovers` (arg "x,y,z,n") for the BAM; fallback = zone 1 klodders.
- z3-systemen: **klassen Advancement needs a 4th step** (`MAX_STAP` 3 -> 4, `nodigLevel(3)` = 50, titles
  `vadsrijk.klassen.titel.<k>.4`); until then the action falls back to our title card. Bounty board `Premiebord#open`.
- API: `Z3NpcApi.zone3Klaar/ZONE3_KLAAR`, `nestlantaarn(p)` (soft light rule), `lantaarnsAan(p)`, `honinglift(p)`.

## Decisions made alone (open for the user)
- hq01 starts at Kapitein Poolster (Kandijkom; zone 2 ends there). All names (Zoemzeil, Raatje, Wasja, Veertje, Zoet, Raap,
  Hamerhoning, Glinsterhoning, Lusje, Knoest, Wortelien, Gloei, Kruinwachter Blad, ...).
- The escape ride (hq14) is a camera scene (no free control/fail reset: `RitScene` is a kern TODO in ZONE3_PLAN §9.6).
- The first glide counts by height (riding, 8+ blocks below the zweefplatform, within 90 blocks), not by a landing spot.
- Both kettles are at the Mika-schimmelboerderij (POI_Z3 has 4 kettles there, none in the Honingmoeras).
- Titles "Muizenbevrijder/Drakenvriend" are not granted here (z3-bazen loot per plan §5); Wasja says "Drakenvriend".
- Not done: WERELDVERHAAL §10 small references in zone 1/2 (only the zone-2 cliffhanger direction).

## z3-systemen wiring (merged 110-z3-systemen ca18781a8, commit 697156575)
- Jagersguh Raap: role `premiebord` -> `Premiebord.open(p, PREMIES)`; extra choices Jachtbord (`JACHT`) and penningwinkel (`WINKEL`).
- Juweliersguh Glinsterhoning: role `juwelier` (zone 2's binding -> `zone2.systemen.Juwelier.open(p)`, handles zone-3 gems).
- Winkelguh Bijenwas: role `honingkraam` -> `Winkels.open(p, Z3SystemenRegistry.HONINGKRAAM)`.
- Stalmeester Veertje: extra choice "25 Gouden Veren ruilen" (visible when `GoudenVeren.kanRuilen`) -> `GoudenVeren.ruil(p)`.
- Ordemeester Wasja: extra choice "vierde titel" (when `Advancement.kanDoen`) -> `Advancement.doe(p)`; the class quests
  `z3_adv50_<k>` end with `advancement:<k>:4` (MAX_STAP is 4 now).
- Quest xp = `Z3Xp` shares, computed in the generator (`z3xp(lv, hoofd)`: main/class/secret 45 %, side 18 % of the level's xp).
- Areas: `Gebieden.op(x, y, z)` (hq01 "in Honingtak"); our flat fallback boxes only register when an id is missing (Z3Lagen
  registers them all now).

## Resume point / tests (2026-11-04)
- Head: see `git log` on 110-z3-npc. Compiles; `runGameTestServer -Pgt=Z3NpcGameTests`: **2/2 passed** (elkeStapWieEnWaar: every
  z3 step has a target + where text, every NPC name/where/spot in zone 3, Vahoegje registered; hoofdlijnViaApi: the 15 main steps via
  the real hooks (starter Pluimguh via `Z3Haken.starter` + `Rijdieren.geefStarter`, scenes run to their end), hand-ins by clicking
  the NPCs, up to `zone3_klaar` + Vahoegje + all splinters).
- No AutoCheck run (memory; the real zone-3 map lives in z3-wereld). Worth checking in the dev client after integration: NPC spots on
  the real map, the scenes' camera paths (opening fallback, BAM, nest, F1, ride, finale, F2), the starter screen from Veertje.
- Lang files were regenerated after the merge (npc, z2_npc, z3_npc, voltooiing, z3_systemen generators); integration regenerates
  anyway.
