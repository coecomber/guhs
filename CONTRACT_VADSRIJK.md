# CONTRACT_VADSRIJK — Guhs 1.10.0 "Het Vadsrijk", zone 1

Shared API between the slices. Branch every slice from the commit that adds this file (on `110-vadsrijk-poc`).
Design source: `guhs1100-avontuur/GUHS1100_AVONTUUR.md` (§9 wins over §1-8). All in-game text is Dutch; en_us gets the same Dutch text.
MC 26.1.2 / NeoForge 26.1.2.112 / GeckoLib 5.5.2 (note: `Identifier`, `ValueInput/ValueOutput`, `TeleportTransition`, render states).

## 0. Ground rules

- **Own your package, never edit another slice's files.** The kern slice owns `feature/vadsrijk/kern/**`, `VadsrijkFeature`,
  `client/VadsrijkClient` and this contract. Shared repo files (`Features.java`, `FeaturesClient.java`, `tools/features/__init__.py`,
  `build.gradle`, lang json merges) are already wired for every slice by the kern commit; the lead/integration merges anything else.
- Each slice has **one registry class** (already present as a stub, see §1) that `VadsrijkFeature` calls. Put your
  DeferredRegisters, event listeners, payloads and commands behind it.
- Each slice has **one resource generator** `tools/features/vadsrijk_<slice>.py` (stub present, already in `FEATURES`).
  It defines `build(h)` and a standalone `_direct()`; run it from the project root:
  `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_<slice>.py`. Never run the full `make_resources.py` in a slice
  (the lead does that once at integration). Generated files are committed.
- Need something from kern that is missing? Do not patch kern: write it in your slice behind a small adapter and note it in
  your slice's NOTITIES file (`NOTITIES_<SLICE>.md`); kern/lead lifts it later.
- Guhs are always passive and never hurt. Only Mika's may be villains (zone 1: the Mika-spion).
- Gametests: batch `vadsrijk_<slice>`, class name `Vadsrijk<Slice>GameTests` so `-Pgt=Vadsrijk<Slice>` runs only yours.
  Gradle only via `gradle_slot.sh` with `GUHS_SLOTS=2`. Never `gradlew clean`.

## 1. Packages, registry classes, id prefixes

| slice | package (`nl.juiced.guhs.feature.vadsrijk.`) | registry class (server / client) | registry id prefix | lang prefix | generator |
|---|---|---|---|---|---|
| kern | `kern`, `kern.*` | `kern.KernRegistry` / `kern.client.KernClient` | `vr_` (kern ids listed in §11) | `vadsrijk.kern.` | `vadsrijk_kern.py` |
| klassen | `klassen` | `klassen.KlassenRegistry` / `klassen.client.KlassenClient` | `vr_kl_` | `vadsrijk.klassen.` | `vadsrijk_klassen.py` |
| wezens | `wezens` | `wezens.WezensRegistry` / `wezens.client.WezensClient` | `vr_mob_` | `vadsrijk.wezens.` | `vadsrijk_wezens.py` |
| bazen | `bazen` | `bazen.BazenRegistry` / `bazen.client.BazenClient` | `vr_baas_` | `vadsrijk.bazen.` | `vadsrijk_bazen.py` |
| wereld | `wereld` | `wereld.WereldRegistry` / `wereld.client.WereldClient` | `vr_w_` | `vadsrijk.wereld.` | `vadsrijk_wereld.py` |
| npc | `npc` | `npc.NpcRegistry` / `npc.client.NpcClient` | `vr_npc_` | `vadsrijk.npc.` | `vadsrijk_npc.py` |
| economie | `economie` | `economie.EconomieRegistry` / `economie.client.EconomieClient` | `vr_eco_` (items: `vr_item_`) | `vadsrijk.economie.` | `vadsrijk_economie.py` |

Other ids: payloads `guhs:vr_<slice>_<name>`; sounds `vr.<slice>.<name>`; structures `data/guhs/structure/vadsrijk/<slice>/...`;
textures `textures/vadsrijk/<slice>/...`; commands `/guhs vadsrijk <slice-word> ...` (brigadier merges the `guhs vadsrijk` nodes,
so every slice registers its own subtree from its registry class: `event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("<word>")...))`). Hint keys `vadsrijk.<slice>.tip.<id>` (+ `.titel`). Gear flavour lines: `vadsrijk.loot.<basetype>.flavour.<n>`. SpelerData extra-tags: the slice name (§3).

The 2 placeholder test mobs of kern (`vr_proefklodder`, `vr_proefkloddertje`) exist only for kern tests and the reference boss;
the real zone-1 mobs (Klodder, Bubbelklodder, ...) are the wezens slice.

## 2. Dimension and coordinate plan

- Dimension `guhs:vadsrijk` (type `guhs:vadsrijk`, biome `guhs:vadsrijk`), min_y 0, height 320, sea/water level 62,
  **ground level ~64**. Day/night cycle on, pink-gold fog/sky. No natural mob spawning (spawning is code: `kern.mob.Spawners`).
- Adventure rules in the dimension (kern enforces): no block breaking/placing for non-creative players; right-click works
  (doors, levers, campfires); ender chests and shulker boxes are blocked (no smuggling between the worlds).
- `kern.Gebieden` holds the area boxes (x/z, inclusive). Zone 1 = -600..600 x -600..600.

| area id | name | box (minX, minZ) .. (maxX, maxZ) | levels |
|---|---|---|---|
| `zwamzoet` | Zwamzoet (city) | (-160,-160) .. (160,160) | town |
| `haven` | De Haven (airship dock) | (-120,400) .. (120,600) | town |
| `klodderweide` | Klodderweide | (160,-300) .. (600,400) | 1-6 |
| `sporenwoud` | Sporenwoud | (-600,-160) .. (-160,400) | 6-13 |
| `druipgrotten` | Druipgrotten | (-600,-600) .. (-160,-160) | 12-18 |
| `zwamtempel` | Zwamtempel mountain | (160,-600) .. (600,-300) | 18-20 |
| `ring` | the land between (paths, scars) | rest of -600..600 | - |
| `knuffelkamp` | Knuffelkamp island | (-128,900) .. (128,1150) | 1-3 |
| `proeftuin` | kern dev/test area (never in the map) | (3000,3000) .. (3192,3192) | dev |

Arrival (first entry) = Knuffelkamp dock, `Gebieden.AANKOMST` = (0, 65, 1130).

## 3. Player data API (`kern.SpelerData`, `kern.Spelers`)

`SpelerData` is a NeoForge **attachment on the player** (`KernRegistry.SPELER`), serialized with player.dat and copied on death.
That makes the inventory stash atomic with the player's own inventory (both live in the same file).

- `klasse` (current class id), `Map<String, Integer> xp` per class (FF14 model; level derived), `vads`/`energie`/`ultLading`
  (runtime, clamped by stats), `munten` (Vadsmunten in brons: 100 brons = 1 zilver, 100 zilver = 1 goud), `checkpoint`,
  `duizeligTot`, `Set<String> tips` (first-time hints seen), `Set<String> gezien` (cutscenes/titles seen),
  `Map<String, Set<String>> boom` (skill-tree nodes per class), `skillpunten` derived, `long respecKlaar`.
- **`CompoundTag extra(String slice)`**: free per-slice storage (quests = `extra("npc")`, mounts = `extra("economie")`...). Call
  `Spelers.opgeslagen(p)` after changing it.
- `Spelers`: `data(p)`, `level(p)` / `level(p, klasse)`, `stats(p)` (computed: class base + level growth + gear + skill tree + status),
  `geefXp(p, n, bron)` (this player only), `geefXpGedeeld(p, n, bron)` (the party within 48 blocks each get n; mobs use this),
  `zetLevel(p, n)`, `geefMunten(p, n)`, `neemMunten(p, n)`, `sync(p)` (HUD).
- Levels 1..`Levels.MAX` (20); `Levels.xpVoor(level)`; level-up gives automatic stat growth + 1 skill point.
- **Inventory swap** (`kern.Inventaris`): `naarRijk(p)` / `naarThuis(p)`. Never call these yourself: the kern reconciler swaps on every
  dimension change/login (`binnen` flag vs. actual dimension). Rules: the stash is never overwritten (a non-empty stash merges,
  overflow goes into a "verloren-gevonden" list that `/guhs vadsrijk kist` hands out), cursor + crafting grid are emptied into
  the inventory first, the ender chest is blocked in the dimension.

## 4. Stats, elements, status effects, damage

- `kern.Stat` enum: VADS, KRACHT, BEHENDIGHEID, WIJSHEID, VERDEDIGING, KRITIEK, KRITIEKSCHADE, AANVALSSNELHEID, LOOPSNELHEID,
  COOLDOWNREDUCTIE, ELEMENTSCHADE, ELEMENTWEERSTAND, LEVENSROOF, HEALKRACHT. `kern.Stats` = EnumMap container (`get/plus/samen`).
- `kern.Element` enum: GEEN, ROZE, VUUR, NATUUR (= Zwam), WATER (= Slijm). `Element.factor(aanval, verdediger, ontvadst)`:
  Vuur > Natuur > Water > Vuur ×1.5, reverse ×0.7, Roze ×1.5 vs `ontvadst` targets, else ×1.
- `kern.Status` enum: BRANDEND, GESPOREN, PLAKKERIG, VERDOOFD (debuffs), VAHOEG, KNUFFELSCHILD, SNELPOOTJES (team buffs), DUIZELIG (after
  fainting). `kern.Statussen.geef(entity, status, ticks, sterkte, bron)`, `heeft`, `sterkte`, `wis`. Ticking (DoT, slow, stun) is kern.
- **Damage pipeline** `kern.Schade`: everything in the dimension goes through `Schade.doe(Klap)`; vanilla damage to players and to
  `VadsrijkMob`s is cancelled/rerouted. `Klap(bron, doel, basis, element, Soort{MELEE,AFSTAND,MAGIE,WAAR}, kanKritiek, skillId)`.
  `Schade.heal(bron, doel, n)`. Hooks: `Schade.NA_KLAP` (List<Consumer<Uitkomst>>: ult charge, lifesteal, boss contribution,
  quest counters), `Schade.NA_HEAL`. No friendly fire: players in the same party (or any two players: PvP is off) never hurt each
  other. Players' HP = `SpelerData.vads` (vanilla health stays full; hearts/hunger/armor HUD hidden in the dimension).
- **Fainting** (`kern.Flauwvallen`): Vads 0 → wake at the checkpoint (last lit campfire; else area respawn; at a boss: the arena door),
  lose nothing, 30 s DUIZELIG (-25 % stats). `Flauwvallen.CHECKPOINT_KIEZER` lets bazen override (arena door).
- Checkpoints: right-clicking an unlit vanilla campfire in the dimension lights it and sets the checkpoint (`kern.Kampvuren`).
  The wereld slice places unlit campfires; the npc slice may add a ridder-guh next to it.

## 5. Mobs (`kern.mob.VadsrijkMob`) — wezens slice builds on this

`abstract class VadsrijkMob extends Monster`: synced `vads/maxVads`, `level`, `element`, `ontvadst`, `affixen` (Set<Affix>: GLOEIEND,
SNEL, SPLITSEND, SCHILDEND; elites only), `elite`. Override points:
`basisVads(level)`, `basisSchade(level)`, `xp()`, `lootTabel()` (an id from `kern.loot.LootTabellen`), `opPlof()` (defeat FX:
default = pink particles + happy squeak, then turns pink and hops away/vanishes — no death animation), `aanval(target)` (default
melee through `Schade`). The nameplate HP bar (element letter, level, coloured bar) is kern (custom name, auto-updated).
Defeat = `VadsrijkMob.verslagen(by)`: xp to contributors, loot drops (personal, per player), `Haken.VERSLAGEN` (quests:
"opgevrolijkt" counter), then plof. Spawning: `kern.mob.Spawners.registreer(Spawner)` (area box, type, max, respawn ticks, level range).
Renderers: wezens own their renderers (GeckoLib or vanilla models).

## 6. Bosses (`kern.baas.*`) — bazen slice builds on this

`abstract class VadsrijkBaas extends VadsrijkMob`: `fases()` (HP thresholds), `opFase(int)`, `titelKey()`/`ondertitelKey()`,
`intro(ServerPlayer, boolean eersteKeer)` (default: `BaasIntro`), HP scaling `schaalVads(int spelers)`, `Arena arena()`.
- `kern.baas.Arena` record (id, centre, radius, door, respawnSeconds, bord pos) + `kern.baas.Arenas.registreer(Arena, Supplier<VadsrijkBaas>)`:
  spawns the boss, scales HP by players inside, plays the intro on entering (first time: 4-6 s non-click camera swoop + signature move +
  big title card; later: 1.5 s title card), shows a countdown hologram (TextDisplay) after defeat.
- `kern.baas.Bijdrage`: damage/heal/tank per player; > 5 % → personal loot chest (`kern.baas.BaasKist`: one per player, only
  the owner can open it, rolls from the boss's loot table, vanishes after 2 min).
- `kern.baas.Telegraaf`: ground telegraphs (red/pink circle, line, cone) as particles + `Telegraaf.cirkel(level, centre, radius, ticks, kleur)`.
- The **Grauwe Klodderreus** (`kern.baas.klodderreus`) is the kern reference boss; the bazen slice does Koning Klodder, de Opperzwam,
  de Mika-spion (and may move/rework the Klodderreus into Knuffelkamp).

## 7. Classes and skills (`kern.klasse.*`) — klassen slice builds on this

- `kern.klasse.Klasse` (id, naamKey, `Stats basis(level)`, `wapenSoorten`, `Vaardigheid[4]` = R, V, G, Z(ult), `List<BoomKnoop> boom`),
  `kern.klasse.Klassen.registreer(Klasse)`, `Klassen.van(id)`.
- `kern.klasse.Vaardigheid` interface: `id()`, `slot()`, `energie()`, `cooldownTicks()`, `boolean ult()`, `boolean gebruik(ServerPlayer, SkillContext)`.
  Kern handles keys (rebindable, category "Vadsrijk", only active in the dimension; R/V/G/Z defaults), energy, cooldowns (with
  COOLDOWNREDUCTIE), ult charge (from damage dealt), HUD slots and the "first skill" hint.
- `kern.klasse.BoomKnoop` record (id, klasse, kosten, vereist, rij/kolom, `StatBonus`/variant flag). Respec and job advancement
  (lv 10/20 ceremony) are klassen-slice logic; kern exposes `SpelerData.boom`, `Spelers.skillpunten(p, klasse)`, `respecKlaar`.
- Kern ships the **Knuffelridder** (`klassen/ridder` package is kern-owned until integration; the klassen slice adds `klassen/jager`,
  `klassen/magier`, `klassen/boom` and the class-change at the ordetempel).
- Basic attack = left click with a class weapon (`kern.loot` weapon); without one: a weak punch.

## 8. Items and loot (`kern.loot.*`) — economie slice builds on this

- `kern.loot.Zeldzaamheid` enum: GEWOON (grey), ONGEWOON (green), ZELDZAAM (blue), EPISCH (purple), LEGENDARISCH (orange),
  GUHDDELIJK (pink: glitter + light beam + own loot sound).
- `kern.loot.BasisType` (id, Slot{WAPEN,HOOFD,BORST,BENEN,VOETEN}, klasse or null, element, `Stats basis(level)`, flavour keys) +
  `kern.loot.BasisTypes.registreer`. One registered Item per base type (`kern.loot.RijkItem` / `RijkHarnas`), rolls in the data component
  `guhs:vr_uitrusting` (`kern.loot.Uitrusting` record: type, zeldzaamheid, level, bonussen, element, flavour, sterren, verpluisd, gebonden, seed).
- `kern.loot.Rollen.rol(type, level, zeldzaamheid, random)`; bonus pool `kern.loot.Bonussen` (1-5 bonuses by rarity, ranges by level).
- `kern.loot.LootTabellen.registreer(id, tabel)`; drops pop with a rarity-coloured beam (`kern.loot.LootBundel`).
- Gear counts only inside the dimension (armour in the normal armour slots; vanilla armour attributes are ignored there).
- **Economie hooks**: `Uitrusting.metSterren/metVerpluisd/metBonussen` (immutable updates), `Rollen.STER_BONUS` (stat bonus per star,
  economie sets it), `Uitrusting.gebonden` (no trade/market). Vadsmunten via `Spelers.geefMunten/neemMunten`.

## 9. NPCs, dialogue, quests, cutscenes, hints

- Quest dialogue = the existing `feature/verhaal` + knuffeldal `PraatScherm` tech (click to continue).
- Non-click camera scenes in the dimension: `kern.scene.RijkScene` (keyframes in world coords, optional title card) played with
  `RijkScenes.speel(ServerPlayer, RijkScene)`; title cards alone: `RijkScenes.titel(p, titelKey, ondertitelKey, ticks)`.
- Quest log data lives in `SpelerData.extra("npc")`; the "!" marker above quest givers and the questlog screen are npc-slice.
  Kern hooks for quest progress: `Haken.VERSLAGEN`, `Haken.LEVEL_OP`, `Haken.GEBIED_BETREDEN`, `Haken.LOOT`, `Haken.KAMPVUUR`.
- First-time hints: `kern.Tips.toon(ServerPlayer, String key)` (shown once per player; texts `vadsrijk.<slice>.tip.<id>`).

## 10. The map: how the hand-built world gets in (wereld slice)

**One mechanism: per-chunk tiles at world generation.**
- Python generators (wereld slice, render pipeline) write the map as **structure tiles of 64×320×64** blocks:
  `data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt`, tile `(tx, tz)` covering x = tx·64 .. tx·64+63 (floorDiv), full height y 0..319.
- The dimension uses a flat generator with `features: true`; the biome `guhs:vadsrijk` has one feature
  `guhs:vr_kaart` (`kern.wereld.KaartFeature`). For every generated chunk it loads the tile that contains it and places only the
  part inside that chunk (StructureTemplate with a chunk bounding box). No tile → `kern.wereld.Ondergrond` (empty air / void,
  except the `proeftuin`, which kern stamps by code for its own tests).
- Deterministic (same files → same world), server-safe (only new chunks, normal worldgen threads, no ticking), re-runnable
  (delete the dimension's region files → regenerated identically). Entities in tiles are NOT placed (worldgen); NPCs/mobs/spawners are
  code (npc/wezens), keyed by area.
- Jigsaw/instance-free. Moving parts (temple doors) = code (`wereld`).
- Stub: `kern.wereld.KaartFeature` already places tiles if present; the wereld slice only adds tiles (+ its generator scripts in
  `tools/vadsrijk/`) and any extra hooks in `wereld.WereldRegistry`.

## 11. Kern-owned ids
Dimension/type/biome `vadsrijk`; feature `vr_kaart`; attachment `vr_speler`; component `vr_uitrusting`; items `vr_kussenzwaard`, `vr_knuffelknots`,
`vr_pluishamer`, `vr_pluishelm`, `vr_knuffelharnas`; entities `vr_proefklodder`, `vr_proefkloddertje`, `vr_klodderreus`, `vr_kussen`;
block `vr_baaskist`; payloads `vr_kern_*`; keys `key.guhs.vr_r|v|g|z`, category `key.category.guhs.vadsrijk` (in the dimension kern swallows
the other Guhs keys on the same key, e.g. the maag key G and the snuffel keys R/V/Z).

## 12. Status of this commit
Everything above exists and compiles. Implemented for real (kern): attachment + inventory swap, stats/elements/statuses,
damage pipeline, fainting + campfires, xp/levels, loot rolls/items/tooltips/drops with beams, mob base + spawners, boss base +
arenas + chests + telegraphs, party, commands, the Knuffelridder, the Grauwe Klodderreus, the map feature + proeftuin.
Still being filled in by kern after this commit (same signatures): the client HUD (`kern.client.RijkHud`), the scene/title player
(`kern.client.SceneSpeler`), gametests. Slice registry/client classes and `tools/features/vadsrijk_<slice>.py` are empty stubs.
