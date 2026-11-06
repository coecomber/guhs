"""Builds the zone-2 map "De Splinterpieken" as 64x320x64 structure tiles (same mechanism as zone 1, CONTRACT §10).

Called from tools/vadsrijk/maak_kaart.py (``--zone 2`` = only zone 2, default = both). Standalone from the project root:
    python tools/vadsrijk/maak_kaart.py --zone 2 [--alleen tx,tz;...]
Writes src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt for tx -11..10, tz -48..-28 (never touches the
zone-1 tiles), tools/vadsrijk/_cache/plekken_z2.json + kaart_z2.png, Z2WereldPlekken.java and POI_Z2.md.
"""
import json
import os
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
CACHE = os.path.join(HIER, "..", "_cache")
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone2", "wereld", "Z2WereldPlekken.java")

import plan as PL  # noqa: E402
sys.path.insert(0, os.path.join(HIER, ".."))
import indeling as IND  # noqa: E402  (T-WERELD: the zone-2 offset)
sys.path.pop(0)
DX, DZ = IND.Z2_DX, IND.Z2_DZ
DTX, DTZ = DX // 64, DZ // 64


def wereld_tegel(tx, tz):
    """local tile -> world tile index."""
    return tx + DTX, tz + DTZ


def gebieden_w():
    return [(g[0], g[1] + DX, g[2] + DZ, g[3] + DX, g[4] + DZ) + tuple(g[5:]) for g in PL.GEBIEDEN]


def maak(alleen=None):
    import kaart2 as K2
    import tegels2 as T2
    import bouw2
    import bouwsels as BZ
    t0 = time.time()
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(UIT, exist_ok=True)
    K = K2.Kaart2()
    print(f"zone 2: kaart klaar ({time.time() - t0:.0f} s)")
    W = T2.Wereld2(K)
    P = bouw2.bouw_alles(W, K)
    d = P["deur_spiegelburcht"]
    BZ.deurkopie(W, d)
    print(f"zone 2: bouwsels klaar ({time.time() - t0:.0f} s), {len(W.stempels)} stempels")
    mis = __import__("blokken").controleer(W.palet)
    if mis:
        print("zone 2 ONBEKENDE BLOKKEN:", mis)
    Pw = IND.schuif(P, DX, DZ)          # T-WERELD: every POI in world coordinates
    java_plekken(Pw)
    sys.path.insert(0, os.path.join(HIER, "..", "grotten"))
    import grot as GROT                                    # U-GROTTEN: the zone-2 cave (Kristalgrot), states known before the lut
    for _s in GROT.staten(2):
        W.palet.id(_s)
    lut = W.opaak_lut()
    tegels = [(tx, tz) for tx in range(PL.UIT_TX0, PL.UIT_TX1 + 1) for tz in range(PL.UIT_TZ0, PL.UIT_TZ1 + 1)]
    if alleen:   # --alleen takes WORLD tile indices
        tegels = [tuple(int(v) for v in s.split(",")) for s in alleen.split(";")]
        tegels = [(t[0] - DTX, t[1] - DTZ) for t in tegels]
        tegels = [t for t in tegels if PL.UIT_TX0 <= t[0] <= PL.UIT_TX1 and PL.UIT_TZ0 <= t[1] <= PL.UIT_TZ1]
    else:
        # T-WERELD: the zone-2 tiles at the OLD place (world tx -11..10, tz -48..-28) go
        weg = 0
        for tx in range(PL.TX0, PL.TX1 + 1):
            for tz in range(PL.TZ0, PL.TZ1 + 1):
                if IND.van_andere_zone(tx, tz, "z2"):
                    continue
                p = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
                if os.path.exists(p):
                    os.remove(p)
                    weg += 1
        print(f"zone 2: {weg} oude tegels (oude plek) verwijderd")
    totaal, grootte = 0, 0
    beeld = np.zeros((PL.NZ, PL.NX, 3), np.uint8)
    hgt = np.zeros((PL.NZ, PL.NX), np.float32)
    kleur = None
    for n, (tx, tz) in enumerate(tegels):
        vol = W.tegel(tx, tz)
        GROT.pas_toe(vol, *wereld_tegel(tx, tz), W.palet.id, W.palet.lijst, 2)   # U-GROTTEN (world tile)
        vol = W.hol(vol, lut)
        wx, wz = wereld_tegel(tx, tz)
        pad = os.path.join(UIT, f"t_{wx}_{wz}.nbt")
        nb = W.schrijf(vol, pad)
        totaal += nb
        grootte += os.path.getsize(pad)
        if not alleen:
            if kleur is None or len(kleur) < len(W.palet.lijst):
                import kaartbeeld
                kleur = np.array([kaartbeeld.kleur_van(s.split("[")[0]) for s in W.palet.lijst], np.float32)
            heeft = vol > 0
            hy = np.where(heeft.any(1), 319 - np.argmax(heeft[:, ::-1, :], axis=1), -1)
            hid = np.take_along_axis(vol, np.clip(hy, 0, 319)[:, None, :], axis=1)[:, 0, :]
            i0, j0 = tx * 64 - PL.X0, tz * 64 - PL.Z0
            beeld[j0:j0 + 64, i0:i0 + 64] = kleur[hid].transpose(1, 0, 2)
            hgt[j0:j0 + 64, i0:i0 + 64] = hy.T
        if n % 40 == 0:
            print(f"  z2 tegel {n}/{len(tegels)} ({tx},{tz}): {nb} blokken; {grootte / 1e6:.1f} MB ({time.time() - t0:.0f} s)", flush=True)
    print(f"zone 2: {len(tegels)} tegels, {totaal} blokken, {grootte / 1e6:.1f} MB ({time.time() - t0:.0f} s)")
    with open(os.path.join(CACHE, "plekken_z2.json"), "w", encoding="utf-8") as f:
        json.dump(Pw, f, indent=1)
    if not alleen:
        from PIL import Image
        gz, gx = np.gradient(hgt)
        licht = np.clip(1 + (-gx * 0.5 - gz * 0.7) * 0.12, 0.6, 1.35)
        out = np.clip(beeld * licht[..., None], 0, 255).astype(np.uint8)
        Image.fromarray(out).save(os.path.join(CACHE, "kaart_z2.png"))
        Image.fromarray(out).resize((PL.NX // 2, PL.NZ // 2)).save(os.path.join(CACHE, "kaart_z2_klein.png"))
    return W, K, Pw


# ------------------------------------------------------------------------------------------------ POIs -> Java + POI_Z2.md
def plat(prefix, o, uit):
    if isinstance(o, list) and len(o) == 3 and all(isinstance(v, int) for v in o):
        uit[prefix] = o
    elif isinstance(o, dict):
        for k, v in o.items():
            if k in ("mobzones", "deur_spiegelburcht"):
                continue
            plat(f"{prefix}.{k}" if prefix else k, v, uit)
    elif isinstance(o, list):
        for i, v in enumerate(o):
            plat(f"{prefix}.{i + 1}", v, uit)


def java_plekken(P):
    alle = {}
    plat("", P, alle)
    alle = {k: [int(a) for a in v] for k, v in alle.items()}

    def bp(v):
        return f"new BlockPos({v[0]}, {v[1]}, {v[2]})"
    d = P["deur_spiegelburcht"]
    deur = f'            new WereldPlekken.Deur("spiegelburcht", {bp(d["min"])}, {bp(d["max"])}, {bp(d["voor"])}, {d["kopie_y"]}, true)'
    uit = [k for k in alle if k.startswith("uitkijk_")]
    kv = sorted(k for k in alle if k.startswith("kampvuur_"))
    mob = P["mobzones"]
    zones = (","+chr(10)).join(f'            new Zone("{k}", {v["min"][0]}, {v["min"][2]}, {v["max"][0]}, {v["max"][2]}, {v["level"][0]}, {v["level"][1]})'
                               for k, v in mob.items())
    paden = (","+chr(10)).join("            new int[][] {" + ", ".join("{" + str(a) + ", " + str(b) + "}" for (a, b) in pts) + "}"
                               for pts in [[(a + DX, b + DZ) for (a, b) in p_] for p_ in PL.PADEN.values()])
    gebieden = (","+chr(10)).join(f'            new Gebieden.Gebied("{g[0]}", {g[1]}, {g[2]}, {g[3]}, {g[4]}, {g[5]}, {g[6]}, {str(g[7]).lower()})'
                                  for g in gebieden_w())
    src = f"""package nl.juiced.guhs.feature.vadsrijk.zone2.wereld;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * GENERATED by tools/vadsrijk/zone2/maak2.py (via maak_kaart.py) from the zone-2 map generator (do not edit by hand; the same
 * list is POI_Z2.md). Every position in zone 2 "De Splinterpieken": town spots, arenas, the Spiegelburcht, campfires, secrets.
 * {{@link #plek}} gives any of them by id ({{@code /guhs vadsrijk z2wereld plek <id>}} teleports there).
 */
public final class Z2WereldPlekken {{
    public static final int TEGEL_X0 = {PL.UIT_TX0 + DTX}, TEGEL_X1 = {PL.UIT_TX1 + DTX}, TEGEL_Z0 = {PL.UIT_TZ0 + DTZ}, TEGEL_Z1 = {PL.UIT_TZ1 + DTZ};
    /** The playable zone box (fix round 2: compact, ~500 x 575; the rest of the tile grid is cloud sea). */
    public static final int ZONE_MIN_X = {PL.ZONE[0] + DX}, ZONE_MIN_Z = {PL.ZONE[1] + DZ}, ZONE_MAX_X = {PL.ZONE[2] + DX}, ZONE_MAX_Z = {PL.ZONE[3] + DZ};
    /** The Spiegelburcht was moved as one piece: v1 position + (BURCHT_DX, 0, BURCHT_DZ) (T-WERELD: incl. the zone-2 offset). */
    public static final int BURCHT_DX = {PL.BURCHT_DX + DX}, BURCHT_DZ = {PL.BURCHT_DZ + DZ};
    /** The paths (polylines of x, z) — creatures never spawn within ~5 blocks of them. */
    public static final List<int[][]> PADEN = List.of(
{paden});
    /** Kandijkom dock: where the airship from De Haven lands, and its bell. */
    public static final BlockPos DOK_LANDING = {bp(alle["dok_landing"])}, DOK_BEL = {bp(alle["dok_bel"])};
    /** Gondola: the two stations (stand on the platform), their bells, the cable ends. */
    public static final BlockPos GONDEL_DAL = {bp(alle["gondel_dal"])}, GONDEL_BERG = {bp(alle["gondel_berg"])};
    public static final BlockPos GONDEL_DAL_KNOP = {bp(alle["gondel_dal_knop"])}, GONDEL_BERG_KNOP = {bp(alle["gondel_berg_knop"])};
    public static final BlockPos KABEL_DAL = {bp(alle["gondel_kabel_dal"])}, KABEL_BERG = {bp(alle["gondel_kabel_berg"])};

    /** A mob zone (box, inclusive) with its level range (for the wezens slice). */
    public record Zone(String id, int minX, int minZ, int maxX, int maxZ, int minLevel, int maxLevel) {{
    }}

    /** The zone-2 areas (kern.Gebieden boxes, first match wins). */
    public static final List<Gebieden.Gebied> GEBIEDEN = List.of(
{gebieden});

    public static final List<WereldPlekken.Deur> DEUREN = List.of(
{deur});

    public static final List<WereldPlekken.Uitkijk> UITKIJKEN = List.of(
{(","+chr(10)).join(f'            new WereldPlekken.Uitkijk("{k[8:]}", {bp(alle[k])})' for k in uit)});

    public static final List<BlockPos> KAMPVUREN = List.of(
{(","+chr(10)).join("            " + bp(alle[k]) for k in kv)});

    public static final List<Zone> MOBZONES = List.of(
{zones});

    public static final Map<String, BlockPos> ALLE = Map.ofEntries(
{(","+chr(10)).join(f'            Map.entry("{k}", {bp(v)})' for k, v in sorted(alle.items()))});

    private Z2WereldPlekken() {{
    }}

    public static BlockPos plek(String id) {{
        return ALLE.get(id);
    }}
}}
"""
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8", newline=chr(10)) as f:
        f.write(src)
    print("Z2WereldPlekken.java:", len(alle), "plekken")
    poi_md(alle, P)


SECTIES = [
    ("Travel (airship dock, gondola)", ("dok_", "gondel_", "npc_gondel")),
    ("Kandijkom: plaza, NPC spots, secrets in town", ("plein", "schaatsvijver", "questbord", "sneeuwguhspar", "npc_", "kristalkapel", "cafe_",
                                                       "geheim_chocokelder")),
    ("Kandijkom buildings (deur = stand outside the door, binnen = interior centre, npc = behind the counter, doos = box)", ("gebouw.",)),
    ("Arenas and bosses", ("arena_", "galmwolf_", "spiegelzucht_", "boormeester_")),
    ("De Spiegelburcht (dungeon): door, rooms, gates (open AIR, bazen fills), puzzle spots", ("deur_", "burcht_", "poort_", "puzzel_")),
    ("Campfires (unlit, kern checkpoints) + ridder spots", ("kampvuur_", "ridder_")),
    ("Viewpoints (needle tops, panorama on first visit)", ("uitkijk_",)),
    ("De Galmhellingen", ("galm_",)),
    ("Het Bevroren Knuffelmeer (knuffel_ijs = stand on the ice above a frozen plushie)", ("meer_", "knuffel_ijs", "draak_")),
    ("De Splinterkloof (bridges: stand at each end; stairways: the foot on the ravine floor)", ("brug_", "boortoren_", "kloof_")),
    ("Secrets", ("geheim_",)),
    ("Landmarks", ("landmark_",)),
]


def poi_md(alle, P):
    r = ["# POI_Z2 - coordinates of zone 2 \"De Splinterpieken\" (z2-wereld slice)", "",
         "**v1, GENERATED** by `tools/vadsrijk/maak_kaart.py` (zone-2 part `tools/vadsrijk/zone2/`) from the real map; the x/z/y of "
         "the plan (v0) are kept. Do not edit by hand; requests go to `../POI_VERZOEKEN_Z2.md`. Java: "
         "`nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken` (`plek(\"id\")`, `GEBIEDEN`, `MOBZONES`, `KAMPVUREN`, `DEUREN`).", "",
         "All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted (`doos` = box "
         "corners, door `min/max` = the door's own blocks, `*_straal` = radius in x). Cloud sea level 62; Kandijkom floor stand y 91; "
         "Spiegelburcht floor stand y 176. Dev: `/guhs vadsrijk z2wereld plek <id>` teleports there.", "",
         "## Areas (kern.Gebieden, first match wins)", "", "| id | box (minX, minZ)..(maxX, maxZ) | levels | town |", "|---|---|---|---|"]
    for g in gebieden_w():
        r.append(f"| `{g[0]}` | ({g[1]},{g[2]})..({g[3]},{g[4]}) | {g[5]}-{g[6]} | {'yes' if g[7] else 'no'} |")
    r += ["", "## Mob zones (boxes for the wezens slice; `Z2WereldPlekken.MOBZONES`)", "", "| id | box | levels |", "|---|---|---|"]
    for k, v in P["mobzones"].items():
        r.append(f"| `{k}` | ({v['min'][0]},{v['min'][2]})..({v['max'][0]},{v['max'][2]}) | {v['level'][0]}-{v['level'][1]} |")
    d = P["deur_spiegelburcht"]
    r += ["", "## Moving door", "", f"`spiegelburcht` (WereldDeuren id): blocks {d['min']}..{d['max']} (wings slide along x), stand in front "
          f"{d['voor']}, hidden copy at y {d['kopie_y']}. `WereldDeuren.open(\"spiegelburcht\", player)` / `open(level, id)` / `sluit(level, id)`.", ""]
    gedaan = set()
    for titel, pres in SECTIES:
        regels = [(k, v) for k, v in sorted(alle.items()) if k not in gedaan and any(k.startswith(p) for p in pres)]
        if not regels:
            continue
        r += [f"## {titel}", "", "| id | x, y, z |", "|---|---|"]
        for k, v in regels:
            r.append(f"| `{k}` | {v[0]}, {v[1]}, {v[2]} |")
            gedaan.add(k)
        r.append("")
    rest = [(k, v) for k, v in sorted(alle.items()) if k not in gedaan]
    if rest:
        r += ["## Other", "", "| id | x, y, z |", "|---|---|"] + [f"| `{k}` | {v[0]}, {v[1]}, {v[2]} |" for k, v in rest] + [""]
    with open(os.path.join(ROOT, "POI_Z2.md"), "w", encoding="utf-8", newline=chr(10)) as f:
        f.write(chr(10).join(r))
