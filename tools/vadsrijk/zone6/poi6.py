"""Zone 6 POIs: snap every planned/recorded spot onto the real blocks of the local world and write Z6WereldPlekken.java +
POI_Z6.md + tools/vadsrijk/_cache/plekken_z6.json (all GENERATED: do not commit them). The kit works in game coordinates,
so nothing is turned or shifted here.
"""
import json
import math
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
CACHE = os.path.join(HIER, "..", "_cache")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone6", "wereld", "Z6WereldPlekken.java")
MD = os.path.join(ROOT, "POI_Z6.md")
sys.path.insert(0, os.path.join(HIER, ".."))

import plan6 as PL  # noqa: E402


class Klassen:
    """Movement class per kit palette entry (the loopcheck model): stand = floor below, 2 passable cells."""

    def __init__(self, W):
        import loopcheck as LC
        self.LC = LC
        k = []
        for s in W.pal:
            naam, _, rest = s.partition("[")
            props = dict(kv.split("=", 1) for kv in rest.rstrip("]").split(",") if "=" in kv) if rest else {}
            if naam in ("minecraft:air", "minecraft:cave_air"):
                k.append(LC.PASS)
            else:
                k.append(LC.klasse(naam, props))
        self.k = np.array(k, np.int8)
        self.water = np.array([s.startswith("minecraft:water") for s in W.pal], bool)
        self.W = W

    def staan(self, x, y, z):
        W, LC = self.W, self.LC
        i, j, k = W.vx(x), W.vy(y), W.vz(z)
        X, Y, Z = W.v.shape
        if not (0 <= i < X and 1 <= j < Y - 2 and 0 <= k < Z):
            return False
        v = W.v[i, j - 1:j + 2, k]
        c = self.k[v]
        return c[0] in (LC.SOLID, LC.HALF, LC.STAIR, LC.DIRT_PATH) and c[1] == LC.PASS and c[2] == LC.PASS and \
            not self.water[v[1]] and not self.water[v[0]]

    def top(self, x, z, ymax=None):
        W = self.W
        i, k = W.vx(x), W.vz(z)
        X, Y, Z = W.v.shape
        if not (0 <= i < X and 0 <= k < Z):
            return None
        hi = Y - 2 if ymax is None else min(Y - 2, W.vy(ymax))
        col = W.v[i, :, k]
        for j in range(hi, 0, -1):
            if col[j - 1] != 0 and self.staan(x, W.b[1] + j, z):
                return int(W.b[1] + j)
        return None


_OFF = {}


def _offsets(r, dy):
    if (r, dy) not in _OFF:
        o = [(dx, ddy, dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1) for ddy in range(-dy, dy + 1)]
        o.sort(key=lambda t: (max(abs(t[0]), abs(t[2])) + 1.5 * abs(t[1]), abs(t[0]) + abs(t[2])))
        _OFF[(r, dy)] = o
    return _OFF[(r, dy)]


def snap(K, x, y, z, r=6, dy=4):
    if y is None:
        for d in range(0, r + 1):
            for dx in range(-d, d + 1):
                for dz in range(-d, d + 1):
                    if max(abs(dx), abs(dz)) != d:
                        continue
                    t = K.top(x + dx, z + dz, ymax=200)
                    if t is not None:
                        return (x + dx, t, z + dz)
        return None
    for (dx, ddy, dz) in _offsets(r, dy):
        if K.staan(x + dx, y + ddy, z + dz):
            return (x + dx, y + ddy, z + dz)
    return None


def verzamel(W, info, snap_=True):
    """id -> [x, y, z] (game). Recorded spots (W.plekken) win over the plan. Also fills info['niet_gesnapt']."""
    K = Klassen(W)
    plan = dict(PL.plekken())
    plan.update(info.get("plekken", {}))
    alle, mis = {}, []
    for pid, (x, y, z) in sorted(plan.items()):
        p = (x, y, z)
        if snap_ and not pid.startswith(PL.GEEN_SNAP):
            q = snap(K, x, y, z)
            if q is None:
                mis.append(pid)
                if y is None:
                    y = K.top(x, z) or 70
                q = (x, y, z)
            p = q
        elif y is None:
            p = (x, K.top(x, z) or 70, z)
        alle[pid] = [int(v) for v in p]
    for a_, b_ in PL.ALIAS.items():
        if b_ in alle:
            alle[a_] = list(alle[b_])
    info["niet_gesnapt"] = mis
    # townsfolk: the guh figures of the kit (render figures = in-game props), riders are figures too
    x1, y1, z1 = W.b[:3]
    bew = []
    for (x, y, z, yaw, skin, zit) in getattr(W, "guhs", []):
        bew.append((x / W.k + x1, y / W.k + y1, z / W.k + z1, float(yaw) % 360, skin or "guh", 1 if zit else 0))
    info["bewoners"] = bew
    info["rijdieren"] = list(getattr(W, "rijdieren", []))
    with open(os.path.join(CACHE, "plekken_z6.json"), "w", encoding="utf-8") as fh:
        json.dump(alle, fh, indent=1)
    return alle


def _bp(v):
    return f"new BlockPos({v[0]}, {v[1]}, {v[2]})"


def _pak(lijst, rij):
    s = ";".join(rij(e) for e in lijst)
    return [s[i:i + 3000] for i in range(0, len(s), 3000)] or [""]


def schrijf(alle, info, W):
    bew = info.get("bewoners", [])
    skins = sorted({b_[4] for b_ in bew} | {r[7] for r in info.get("rijdieren", [])} | {"guh"})
    bpak = _pak(bew, lambda e: f"{int(round(e[0] * 2))},{int(round(e[1] * 2))},{int(round(e[2] * 2))},{int(e[3]) // 2 % 180},"
                               f"{skins.index(e[4])},{e[5]}")
    soorten = ["bultguh", "tapijt", "pluimguh"]
    rij = info.get("rijdieren", [])
    rpak = _pak(rij, lambda e: f"{soorten.index(e[0])},{int(round(e[1] * 2))},{int(round(e[2] * 2))},{int(round(e[3] * 2))},"
                               f"{int((e[4] + 180) % 360) // 2},{e[5]},{1 if e[6] else 0},{skins.index(e[7])}")
    kampvuren = sorted(k for k in alle if k.startswith("kampvuur_"))
    uitkijk = [alle[k] for k in PL.UITKIJKEN if k in alle]
    regels = [f'            Map.entry("{k}", {_bp(alle[k])})' for k in sorted(alle)]
    geb = [f'            new Gebied("{g[0]}", {g[1]}, {g[2]}, {g[3]}, {g[4]}, "{g[5]}", {g[7][0]}, {g[7][1]}, {str(g[8]).lower()})'
           for g in PL.GEBIEDEN]
    huizen = [f'            new Huis("{h["id"]}", "{h["rol"]}", {_bp(h["deur"])}, {_bp(h["binnen"])})' for h in info.get("huizen", [])]
    nl = chr(10)
    src = f"""package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * GENERATED by tools/vadsrijk/zone6/poi6.py (via maak_kaart.py --zone 6) from the zone-6 map (do not edit by hand; the same list is
 * POI_Z6.md). Every position in zone 6 "Het Zijdeland" (zone box x 3280..4180, z -1060..-460; tiles tx {PL.TX0}..{PL.TX1},
 * tz {PL.TZ0}..{PL.TZ1}; real game coordinates). {{@link #plek}} gives any of them by id ({{@code /guhs vadsrijk z6wereld plek <id>}}
 * teleports there). Stand spots are snapped onto the real blocks.
 */
public final class Z6WereldPlekken {{
    public static final int TEGEL_X0 = {PL.TX0}, TEGEL_X1 = {PL.TX1}, TEGEL_Z0 = {PL.TZ0}, TEGEL_Z1 = {PL.TZ1};
    /** The zone box (the tiles; never visible). */
    public static final int MIN_X = {PL.TX0 * 64}, MAX_X = {PL.TX1 * 64 + 63}, MIN_Z = {PL.TZ0 * 64}, MAX_Z = {PL.TZ1 * 64 + 63};
    public static final int MIN_LEVEL = {PL.LEVELS[0]}, MAX_LEVEL = {PL.LEVELS[1]};

    /** An area: box (x/z), biome id ("vadsrijk_" + biome), level band, town (safe)? First match wins; the last one is the zone. */
    public record Gebied(String id, int x1, int z1, int x2, int z2, String biome, int minLevel, int maxLevel, boolean stad) {{
        public boolean bevat(double x, double z) {{
            return x >= x1 && x < x2 + 1 && z >= z1 && z < z2 + 1;
        }}

        public String naamKey() {{
            return "vadsrijk.kern.gebied." + id;
        }}
    }}

    /** A house/shop with a role (herberg, bank, winkel_..., woning, stal, ...): its door and a spot inside. */
    public record Huis(String id, String rol, BlockPos deur, BlockPos binnen) {{
    }}

    /** A townsfolk figure (a passive guh prop): position, yaw, skin texture, 0 standing / 1 sitting. */
    public record Bewoner(double x, double y, double z, float yaw, String skin, int soort) {{
    }}

    /** A mount figure at an NPC/stall/structure: soort bultguh | tapijt | pluimguh, feet position, Minecraft yaw, variant
     *  (colour / carpet pattern), with a rider? (the rider is also a Bewoner, sitting), the rider's skin. */
    public record Rijdier(String soort, double x, double y, double z, float yaw, String variant, boolean ruiter, String skin) {{
    }}

    public static final List<Gebied> GEBIEDEN = List.of(
{(',' + nl).join(geb)});

    public static final Map<String, BlockPos> ALLE = Map.ofEntries(
{(',' + nl).join(regels)});

    public static final List<Huis> HUIZEN = List.of(
{(',' + nl).join(huizen)});

    /** Viewpoints. */
    public static final List<BlockPos> UITKIJKEN = List.of({", ".join(_bp(u) for u in uitkijk)});

    public static final List<String> KAMPVUREN = List.of({", ".join('"' + k + '"' for k in kampvuren)});

    private static final String[] SKINS = {{{", ".join('"' + s + '"' for s in skins)}}};
    private static final String[] SOORTEN = {{"bultguh", "tapijt", "pluimguh"}};
    private static final String[] BEWONERS_PAK = {{
{(',' + nl).join('            "' + d + '"' for d in bpak)}}};
    private static final String[] RIJDIEREN_PAK = {{
{(',' + nl).join('            "' + d + '"' for d in rpak)}}};
    /** The townsfolk of the map ({len(bew)} figures). */
    public static final List<Bewoner> BEWONERS;
    /** The mounts of the map ({len(rij)}: bultguhs, flying carpets, Pluimguhs). */
    public static final List<Rijdier> RIJDIEREN;

    static {{
        List<Bewoner> l = new ArrayList<>();
        String pak = String.join("", BEWONERS_PAK);
        if (!pak.isEmpty()) {{
            for (String r : pak.split(";")) {{
                String[] f = r.split(",");
                l.add(new Bewoner(Integer.parseInt(f[0]) / 2.0, Integer.parseInt(f[1]) / 2.0, Integer.parseInt(f[2]) / 2.0,
                        Integer.parseInt(f[3]) * 2f, SKINS[Integer.parseInt(f[4])], Integer.parseInt(f[5])));
            }}
        }}
        BEWONERS = Collections.unmodifiableList(l);
        List<Rijdier> m = new ArrayList<>();
        String rp = String.join("", RIJDIEREN_PAK);
        if (!rp.isEmpty()) {{
            for (String r : rp.split(";")) {{
                String[] f = r.split(",");
                m.add(new Rijdier(SOORTEN[Integer.parseInt(f[0])], Integer.parseInt(f[1]) / 2.0, Integer.parseInt(f[2]) / 2.0,
                        Integer.parseInt(f[3]) / 2.0, Integer.parseInt(f[4]) * 2f, f[5], f[6].equals("1"), SKINS[Integer.parseInt(f[7])]));
            }}
        }}
        RIJDIEREN = Collections.unmodifiableList(m);
    }}

    private Z6WereldPlekken() {{
    }}

    @Nullable
    public static BlockPos plek(String id) {{
        return ALLE.get(id);
    }}

    /** Is this point in zone 6 (the tile box)? */
    public static boolean inZone6(double x, double z) {{
        return x >= MIN_X && x < MAX_X + 1 && z >= MIN_Z && z < MAX_Z + 1;
    }}

    /** The area at this point (null outside zone 6). */
    @Nullable
    public static Gebied gebied(double x, double z) {{
        if (!inZone6(x, z)) {{
            return null;
        }}
        for (Gebied g : GEBIEDEN) {{
            if (g.bevat(x, z)) {{
                return g;
            }}
        }}
        return null;
    }}
}}
"""
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)
    md(alle, info)


def md(alle, info):
    L = ["# POI_Z6 - coordinates of zone 6 \"Het Zijdeland\" (u-z6wereld slice)", "",
         "**GENERATED** by `tools/vadsrijk/zone6/poi6.py` (via `python tools/vadsrijk/maak_kaart.py --zone 6`). Do not edit by hand. "
         "Java: `nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken` (`plek(\"id\")`, `GEBIEDEN` / `gebied(x, z)`, `inZone6`, "
         "`HUIZEN`, `BEWONERS`, `RIJDIEREN`, `KAMPVUREN`, `UITKIJKEN`). Dev: `/guhs vadsrijk z6wereld plek <id>` teleports there.", "",
         "**Use ids, never numbers** (`Z6WereldPlekken.plek(id)`): a regen moves the spots with the map.", "",
         f"**Layout.** Zone box x 3280..4180, z -1060..-460 (tiles tx {PL.TX0}..{PL.TX1}, tz {PL.TZ0}..{PL.TZ1}, real game coordinates, "
         "the tiles have a coast margin: the land runs into the shared open sea at the north/south/east tile edges). West: the sea cliffs "
         "(De Roze Kliffen, coast x ~3292) and the sandstone plateau (y ~120) with the Rozenkloof canyon (the arrival city, floor y ~68-74, "
         "line E airport on the north rim) and the Palmenkloven (the river canyon). Middle: the Saffraanrivier through Saffraanpoort "
         "(street y 71). South: De Roze Duinen + De Zoutvlakte (the Zingende Zoutgrot below it, floor y 49). North-east: Het Ruinedal "
         "(mesas, the dungeon entrance). East: De Moederoase and De Sterrendovende Toren (raid exterior; the gate is sealed, "
         "`toren_raid_ingang` = in front of it).", "",
         "**Notes for the other slices.** The five star beacons are DARK in the tiles: the star of each is centred on `<baken>_ster` "
         "(r ~3.6, light blocks replace the grey ones when you light it). `zoutgrot_kristalwand` = the centre of the 3x4 crystal wall "
         "(x-1..x+1, y-1..y+2) the cave puzzle opens. `kerker_poort` = in front of the sealed star gate (the instance teleport). "
         "All positions are **stand positions** (the air block the feet are in) in `guhs:vadsrijk`, except `*_ster` and "
         "`haven_6_5_luchtschip` / `haven_6_5_mast`.", "",
         "## Areas (`Z6WereldPlekken.GEBIEDEN`, first match wins; biome `guhs:vadsrijk_<biome>`)", "",
         "| id | x1, z1 .. x2, z2 | biome | name | lv | town |", "|---|---|---|---|---|---|"]
    for g in PL.GEBIEDEN:
        L.append(f"| `{g[0]}` | {g[1]}, {g[2]} .. {g[3]}, {g[4]} | {g[5]} | {g[6]} | {g[7][0]}-{g[7][1]} | {'yes' if g[8] else 'no'} |")
    L += ["", "## All spots", "", "| id | x, y, z | area |", "|---|---|---|"]
    for k in sorted(alle):
        v = alle[k]
        g = PL.gebied(v[0], v[2])
        L.append(f"| `{k}` | {v[0]}, {v[1]}, {v[2]} | {g[0] if g else '-'} |")
    if info.get("huizen"):
        L += ["", "## Houses with a role (`HUIZEN`)", "", "| id | role | door | inside |", "|---|---|---|---|"]
        for h in info["huizen"]:
            L.append(f"| `{h['id']}` | {h['rol']} | {', '.join(map(str, h['deur']))} | {', '.join(map(str, h['binnen']))} |")
    rij = info.get("rijdieren", [])
    tel = ", ".join(f"{s} {sum(1 for r in rij if r[0] == s)}" for s in ("bultguh", "tapijt", "pluimguh"))
    L += ["", f"Townsfolk figures: {len(info.get('bewoners', []))}; mount figures: {len(rij)} ({tel})."]
    if info.get("niet_gesnapt"):
        L += ["", "Not snapped (no stand spot within 6 blocks; planned spot kept): " + ", ".join(f"`{k}`" for k in info["niet_gesnapt"])]
    with open(MD, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(L) + "\n")
