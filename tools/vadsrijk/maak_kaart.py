"""Builds the whole Vadsrijk zone-1 map as 64x320x64 structure tiles (CONTRACT_VADSRIJK §10).

Run from the project root:  python tools/vadsrijk/maak_kaart.py [--alleen tx,tz ...] [--geen-cache] [--kaartbeeld]
Output: src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt, tools/vadsrijk/_cache/kaart_bovenaanzicht.png,
tools/vadsrijk/_cache/plekken.json (all POIs, mirrored in WereldPlekken.java / POI_VADSRIJK.md).
Pipeline:
  1. kamp/kamp_wereld.py and stad/stad_wereld.py build Knuffelkamp and Zwamzoet (render generators) -> _cache/*.npz
     (separate processes: the two kit folders share module names);
  2. wereld/kaart.py: the 2D plan (heights, areas, paths, scars, Kloof, sea);
  3. wereld/bouwsels.py: the hand-made set pieces (Zwamtempel + dungeon, De Haven, arena, slime hill, needles, fossil
     bridge, turtle island, stal, caves + crystal chamber, giant mushrooms, campfires, decoration);
  4. wereld/tegels.py: terrain columns + stamps + points -> hollowed -> nbt.
"""
import argparse
import json
import os
import subprocess
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "wereld"))
sys.path.insert(0, os.path.join(HIER, "stad"))
CACHE = os.path.join(HIER, "_cache")
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")

import kaart as KA  # noqa: E402
import tegels as TG  # noqa: E402


def scene(naam, map_, script, opnieuw):
    npz = os.path.join(CACHE, naam + ".npz")
    if opnieuw or not os.path.exists(npz):
        t = time.time()
        subprocess.run([sys.executable, script, npz], cwd=os.path.join(HIER, map_), check=True)
        print(f"  {naam}: {time.time() - t:.0f} s")
    d = np.load(npz)
    with open(npz.replace(".npz", ".json"), encoding="utf-8") as f:
        info = json.load(f)
    return TG.Stempel(naam, d["v"], [str(s) for s in d["pal"]], d["off"]), info, d


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None, help="tx,tz;tx,tz;...")
    ap.add_argument("--geen-cache", action="store_true")
    ap.add_argument("--zonder-bouwsels", action="store_true")
    a = ap.parse_args()
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(UIT, exist_ok=True)
    t0 = time.time()
    kamp, kamp_poi, kamp_d = scene("kamp", "kamp", "kamp_wereld.py", a.geen_cache)
    stad, stad_info, _ = scene("stad", "stad", "stad_wereld.py", a.geen_cache)
    tempel = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "wereld", "portaaltempel.nbt")
    if a.geen_cache or not os.path.exists(tempel):
        os.makedirs(os.path.dirname(tempel), exist_ok=True)
        subprocess.run([sys.executable, "portaaltempel_wereld.py", tempel], cwd=os.path.join(HIER, "kamp"), check=True)
    print(f"scenes klaar ({time.time() - t0:.0f} s)")
    K = KA.Kaart()
    print(f"kaart klaar ({time.time() - t0:.0f} s)")
    W = TG.Wereld(K)
    W.stempel(kamp)
    W.stempel(stad)
    plekken = {"kamp": kamp_poi, "stad": stad_info}
    if not a.zonder_bouwsels:
        import bouwsels
        plekken.update(bouwsels.bouw_alles(W, K, kamp_d))
        bouwsels.kamp_arena(plekken["wereld"], kamp_poi)
        for d in deuren(plekken).values():
            bouwsels.deurkopie(W, d)
        java_plekken(plekken)
    print(f"bouwsels klaar ({time.time() - t0:.0f} s), {len(W.stempels)} stempels")
    mis = __import__("blokken").controleer(W.palet)
    if mis:
        print("ONBEKENDE BLOKKEN:", mis)
    lut = W.opaak_lut()
    tegels = [(tx, tz) for tx in range(KA.TX0, KA.TX1 + 1) for tz in range(KA.TZ0, KA.TZ1 + 1)]
    if a.alleen:
        tegels = [tuple(int(v) for v in s.split(",")) for s in a.alleen.split(";")]
    totaal, grootte = 0, 0
    bovenaan = {}
    for n, (tx, tz) in enumerate(tegels):
        vol = W.tegel(tx, tz)
        vol = W.hol(vol, lut)
        pad = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
        nb = W.schrijf(vol, pad, plekken_nbt(plekken, tx, tz))
        totaal += nb
        grootte += os.path.getsize(pad)
        # top view: highest block per column
        heeft = vol > 0
        hy = np.where(heeft.any(1), 319 - np.argmax(heeft[:, ::-1, :], axis=1), -1)
        hid = np.take_along_axis(vol, np.clip(hy, 0, 319)[:, None, :], axis=1)[:, 0, :]
        bovenaan[(tx, tz)] = (hy.astype(np.int16), hid)
        if n % 40 == 0:
            print(f"  tegel {n}/{len(tegels)} ({tx},{tz}): {nb} blokken; totaal {grootte / 1e6:.1f} MB ({time.time() - t0:.0f} s)",
                  flush=True)
    print(f"{len(tegels)} tegels, {totaal} blokken, {grootte / 1e6:.1f} MB, palet {len(W.palet.lijst)} ({time.time() - t0:.0f} s)")
    with open(os.path.join(CACHE, "plekken.json"), "w", encoding="utf-8") as f:
        json.dump(plekken, f, indent=1)
    if not a.alleen:
        import kaartbeeld
        kaartbeeld.teken(W, bovenaan, os.path.join(CACHE, "kaart_bovenaanzicht.png"))


def plekken_nbt(plekken, tx, tz):
    return None


def deuren(plekken):
    d = {"ordetempel": plekken["stad"]["deuren"]["ordetempel"]}
    d["zwamtempel"] = plekken["wereld"]["deur_zwamtempel"]
    return d


def plat(prefix, o, uit):
    """All [x,y,z] points in the plekken tree as flat ids (kamp.x, stad.poi.x -> x, wereld.x -> x)."""
    if isinstance(o, list) and len(o) == 3 and all(isinstance(v, int) for v in o):
        uit[prefix] = o
    elif isinstance(o, dict):
        for k, v in o.items():
            plat(f"{prefix}.{k}" if prefix else k, v, uit)
    elif isinstance(o, list):
        for i, v in enumerate(o):
            plat(f"{prefix}.{i + 1}", v, uit)


JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "wereld", "WereldPlekken.java")


def java_plekken(plekken):
    alle = {}
    plat("", {"kamp": plekken["kamp"], "stad": plekken["stad"]["poi"], "gebouw": plekken["stad"]["gebouwen"],
              "wereld": plekken["wereld"]}, alle)
    kort = {}
    for k, v in alle.items():
        for pre in ("kamp.", "stad.", "wereld."):
            if k.startswith(pre):
                k = k[len(pre):]
        kort[k] = v

    def bp(v):
        return f"new BlockPos({v[0]}, {v[1]}, {v[2]})"
    W_ = plekken["wereld"]
    regels = []
    for id_, d in deuren(plekken).items():
        regels.append(f'            new Deur("{id_}", {bp(d["min"])}, {bp(d["max"])}, {bp(d["voor"])}, {d["kopie_y"]}, {str(d.get("as_", "x") == "x").lower()})')
    uit = [k for k in W_ if k.startswith("uitkijk_")]
    kv = [k for k in kort if "kampvuur" in k and not k.endswith(("_trap",))]
    src = f"""package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;

/**
 * GENERATED by tools/vadsrijk/maak_kaart.py from the map generator (do not edit by hand; the same list is POI_VADSRIJK.md).
 * Every position in zone 1 of Het Vadsrijk: campfires, arenas, NPC spots, shops, doors, secrets. {{@link #plek}} gives any of
 * them by id (the ids of {{@link #ALLE}}; {{@code /guhs vadsrijk wereld plek <id>}} teleports there).
 */
public final class WereldPlekken {{
    public static final int TEGEL_X0 = {KA.TX0}, TEGEL_X1 = {KA.TX1}, TEGEL_Z0 = {KA.TZ0}, TEGEL_Z1 = {KA.TZ1};
    /** First arrival (kern Gebieden.AANKOMST): the low jetty on the south coast of Knuffelkamp. */
    public static final BlockPos AANKOMST = {bp(W_["aankomst"])};
    public static final BlockPos KAMP_LANDING = {bp(plekken["kamp"]["steiger_eind"])}, HAVEN_LANDING = {bp(W_["luchtschip_haven_kapitein"])};
    public static final BlockPos KAMP_BEL = {bp(plekken["kamp"]["luchtschip_bel"])}, HAVEN_BEL = {bp(W_["luchtschip_haven_bel"])};

    /** A moving temple door: the box of its blocks, where you stand in front of it, the y of its hidden copy, wings along x? */
    public record Deur(String id, BlockPos min, BlockPos max, BlockPos voor, int kopieY, boolean langsX) {{
    }}

    public record Uitkijk(String id, BlockPos top) {{
    }}

    public static final List<Deur> DEUREN = List.of(
{(","+chr(10)).join(regels)});

    public static final List<Uitkijk> UITKIJKEN = List.of(
{(","+chr(10)).join(f'            new Uitkijk("{k[8:]}", {bp(W_[k])})' for k in uit)});

    /** The light beams of the Sporenwoud (the client draws them). */
    public static final List<BlockPos> LICHTSTRALEN = List.of(
{(","+chr(10)).join("            " + bp(v) for v in W_["lichtstralen"])});

    public static final List<BlockPos> KAMPVUREN = List.of(
{(","+chr(10)).join("            " + bp(kort[k]) for k in sorted(kv))});

    public static final Map<String, BlockPos> ALLE = Map.ofEntries(
{(","+chr(10)).join(f'            Map.entry("{k}", {bp(v)})' for k, v in sorted(kort.items()))});

    private WereldPlekken() {{
    }}

    public static BlockPos plek(String id) {{
        return ALLE.get(id);
    }}
}}
"""
    with open(JAVA, "w", encoding="utf-8", newline=chr(10)) as f:
        f.write(src)
    with open(os.path.join(CACHE, "plekken_plat.json"), "w", encoding="utf-8") as f:
        json.dump(kort, f, indent=1)
    print("WereldPlekken.java:", len(kort), "plekken")
    poi_md(kort, plekken)


SECTIES = [
    ("Arrival, travel, ways home", ("aankomst", "steiger", "luchtschip", "terugportaal", "lift_")),
    ("Campfires (unlit `minecraft:campfire[lit=false]`, kern checkpoints) + ridder-guh spots", ("kampvuur", "ridder_")),
    ("Arenas (centre / straal / deur = stand outside the door / bord = respawn board)", ("arena_",)),
    ("Zwamtempel (dungeon)", ("deur_zwamtempel", "zwamtempel_", "puzzel", "geheim_tempelkamer")),
    ("Zwamzoet: ordetempel, plein, questbord, hats (cap tops)", ("ordetempel", "deur_ordetempel", "plein", "questbord", "guhpoort", "hoed_")),
    ("Zwamzoet buildings (deur = stand outside the door, binnen = interior centre, doos = bounding box)", ("gebouw.",)),
    ("Knuffelkamp", ("ordetent", "training", "tempeltje", "vijver", "vuurtoren")),
    ("De Haven + Stal", ("haven_", "stal.")),
    ("Viewpoints (top platform, panorama on first visit)", ("uitkijk_",)),
    ("Mika-spion clues + secrets", ("mika_", "geheim_")),
    ("Landmarks", ("slaapheuvel", "fossielbrug", "schildpad", "kristalkamer", "grot_", "lichtstralen", "waterval")),
]


def poi_md(kort, plekken):
    gedaan = set()
    r = ["# POI_VADSRIJK - coordinates of zone 1 (wereld slice)", "",
         "**v1, GENERATED** by `tools/vadsrijk/maak_kaart.py` from the real map (do not edit by hand; requests go to "
         "`../POI_VERZOEKEN.md`). Java: `nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken` (`WereldPlekken.plek(\"id\")`, ids below). "
         "Dev: `/guhs vadsrijk wereld plek <id>` teleports there.", "",
         "All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless a key says "
         "otherwise (`doos` = bounding box corners, door `min/max` = the door's own blocks). Sea level 62; mainland ground ~64-70; "
         "Zwamzoet ground stand y 64; Knuffelkamp plateau stand y 87.", ""]
    for titel, pres in SECTIES:
        regels = [(k, v) for k, v in sorted(kort.items()) if k not in gedaan and any(k.startswith(p) for p in pres)]
        if not regels:
            continue
        r += [f"## {titel}", "", "| id | x, y, z |", "|---|---|"]
        for k, v in regels:
            r.append(f"| `{k}` | {v[0]}, {v[1]}, {v[2]} |")
            gedaan.add(k)
        r.append("")
    rest = [(k, v) for k, v in sorted(kort.items()) if k not in gedaan]
    if rest:
        r += ["## Other", "", "| id | x, y, z |", "|---|---|"] + [f"| `{k}` | {v[0]}, {v[1]}, {v[2]} |" for k, v in rest] + [""]
    r += VASTE_TEKST
    with open(os.path.join(ROOT, "POI_VADSRIJK.md"), "w", encoding="utf-8", newline=chr(10)) as f:
        f.write(chr(10).join(r))


VASTE_TEKST = open(os.path.join(HIER, "poi_vast.md"), encoding="utf-8").read().split(chr(10)) if os.path.exists(os.path.join(HIER, "poi_vast.md")) else []


if __name__ == "__main__":
    main()
