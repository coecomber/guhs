"""Zone 5 POIs: snap every planned spot onto the real blocks of the local world, turn it into game coordinates, and write
Z5WereldPlekken.java + POI_Z5.md + tools/vadsrijk/_cache/plekken_z5.json (all GENERATED: do not commit them).
"""
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
CACHE = os.path.join(HIER, "..", "_cache")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone5", "wereld", "Z5WereldPlekken.java")
MD = os.path.join(ROOT, "POI_Z5.md")
sys.path.insert(0, os.path.join(HIER, ".."))

import plan5 as PL  # noqa: E402

# spots that are blocks on purpose (not stand spots): never snapped
GEEN_SNAP = ("haven_5_4_luchtschip", "haven_5_4_mast")


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
        """Highest stand y in this column (or None)."""
        W = self.W
        i, k = W.vx(x), W.vz(z)
        X, Y, Z = W.v.shape
        if not (0 <= i < X and 0 <= k < Z):
            return None
        col = W.v[i, :, k]
        hi = Y - 2 if ymax is None else min(Y - 2, W.vy(ymax))
        for j in range(hi, 0, -1):
            if col[j - 1] != 0 and self.staan(x, W.b[1] + j, z):
                return int(W.b[1] + j)
        return None


_OFF = {}


def _OFFSETS(r, dy):
    """Candidate offsets, cheapest first: horizontal distance + 1.5 x height difference (a spot on the floor next to the
    planned one beats the roof of a market stall right above it)."""
    if (r, dy) not in _OFF:
        o = [(dx, ddy, dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1) for ddy in range(-dy, dy + 1)]
        o.sort(key=lambda t: (max(abs(t[0]), abs(t[2])) + 1.5 * abs(t[1]), abs(t[0]) + abs(t[2])))
        _OFF[(r, dy)] = o
    return _OFF[(r, dy)]


def snap(K, x, y, z, r=6, dy=4):
    if y is None:
        best = None
        for d in range(0, r + 1):
            for dx in range(-d, d + 1):
                for dz in range(-d, d + 1):
                    if max(abs(dx), abs(dz)) != d:
                        continue
                    t = K.top(x + dx, z + dz)
                    if t is not None:
                        return (x + dx, t, z + dz)
        return best
    for (dx, ddy, dz) in _OFFSETS(r, dy):
        if K.staan(x + dx, y + ddy, z + dz):
            return (x + dx, y + ddy, z + dz)
    return None


def verzamel(W, S, info, snap_=True):
    """id -> [gx, gy, gz] (game). Also fills info['niet_gesnapt']."""
    K = Klassen(W)
    plan = dict(PL.plekken())
    plan.update(info.get("plekken", {}))
    alle, mis = {}, []
    for pid, (x, y, z) in plan.items():
        p = (x, y, z)
        if snap_ and not pid.startswith(GEEN_SNAP):
            q = snap(K, x, y, z)
            if q is None:
                mis.append(pid)
                if y is None:
                    y = K.top(x, z) or 240
                q = (x, y, z)
            p = q
        elif y is None:
            p = (x, K.top(x, z) or 240, z)
        alle[pid] = list(PL.spel(*p))
    # t-z5wezens' rooms inside the whale: their placeholder boxes, lowered with the whale
    for kid, (a, b, c, d, e, f) in PL.WEZENS_KAMERS.items():
        alle[kid + "_min"] = [a, b + PL.DY, c]
        alle[kid + "_max"] = [d, e + PL.DY, f]
    info["niet_gesnapt"] = mis
    with open(os.path.join(CACHE, "plekken_z5.json"), "w", encoding="utf-8") as fh:
        json.dump(alle, fh, indent=1)
    return alle


def _bp(v):
    return f"new BlockPos({v[0]}, {v[1]}, {v[2]})"


def schrijf(alle, info, W):
    geb = PL.gebieden()
    bew = info.get("bewoners", [])
    # bewoners packed: x*2, y*2, z*2, yaw/2, skin index, soort (0 guh, 1 zit, 2 ridder, 3 npc)
    skins = sorted({b[4] for b in bew})
    pak = []
    for (gx, gy, gz, yaw, skin, soort) in bew:
        pak.append(f"{int(round(gx * 2))},{int(round(gy * 2))},{int(round(gz * 2))},{int(yaw) // 2 % 180},{skins.index(skin)},{soort}")
    pakstr = ";".join(pak)
    delen = [pakstr[i:i + 3000] for i in range(0, len(pakstr), 3000)] or [""]
    kampvuren = sorted(k for k in alle if k.startswith("kampvuur_"))
    uitkijk = info.get("uitkijken", [])
    vangnet = [alle[k] for k in info.get("vangnet_ids", []) if k in alle]
    regels = []
    for k in sorted(alle):
        regels.append(f'            Map.entry("{k}", {_bp(alle[k])})')
    wijken = []
    for g in geb:
        wijken.append(f'            new Wijk("{g["id"]}", {g["x1"]}, {g["z1"]}, {g["x2"]}, {g["z2"]}, "{g["biome"]}")')
    huizen = []
    for h in info.get("huizen", []):
        huizen.append(f'            new Huis("{h["id"]}", "{h["rol"]}", {_bp(h["deur"])}, {_bp(h["binnen"])})')
    src = f"""package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * GENERATED by tools/vadsrijk/zone5/poi5.py (via maak_kaart.py --zone 5) from the zone-5 map (do not edit by hand; the same list is
 * POI_Z5.md). Every position in zone 5 "Hoogvads", the capital on the back of Moeder Wolkwal (whale x 2440..3015, axis z -520,
 * belly y ~134, city y ~214..236, head WEST over the Opstijgpier; local kit -> game: gx = {PL.GX0} - x, gy = y {PL.DY:+d}, gz = {PL.GZ0} - z).
 * {{@link #plek}} gives any of them by id ({{@code /guhs vadsrijk z5wereld plek <id>}} teleports there). Stand spots are snapped onto the
 * real blocks. Zone 5 lies ABOVE zone 4 (same x/z): everything here is from y {PL.ZONE_MIN_Y} up ({{@link #inZone5}}).
 */
public final class Z5WereldPlekken {{
    public static final int TEGEL_X0 = {PL.TX0}, TEGEL_X1 = {PL.TX1}, TEGEL_Z0 = {PL.TZ0}, TEGEL_Z1 = {PL.TZ1};
    /** The whale's box (x/z, with the fins and the tail) and the lowest y of zone 5 (zone 4 below). */
    public static final int MIN_X = {geb[-1]["x1"]}, MAX_X = {geb[-1]["x2"]}, MIN_Z = {geb[-1]["z1"]}, MAX_Z = {geb[-1]["z2"]}, MIN_Y = {PL.ZONE_MIN_Y};
    public static final int MIN_LEVEL = {PL.LEVELS[0]}, MAX_LEVEL = {PL.LEVELS[1]};

    /** A district: game box (x/z, from MIN_Y up) and its biome id ("vadsrijk_" + biome). First match wins; the last two are the whole
     *  city and the whale. */
    public record Wijk(String id, int x1, int z1, int x2, int z2, String biome) {{
        public boolean bevat(double x, double z) {{
            return x >= x1 && x < x2 + 1 && z >= z1 && z < z2 + 1;
        }}

        public String naamKey() {{
            return "vadsrijk.kern.gebied." + id;
        }}
    }}

    /** A house of the city with a role (herberg, bank, winkel, woning, ...): where its door is and a spot inside. */
    public record Huis(String id, String rol, BlockPos deur, BlockPos binnen) {{
    }}

    /** A townsfolk figure (a passive guh prop, Z5Bewoners): position, yaw, skin texture, kind (0 walking/standing, 1 sitting,
     *  2 knight of the Wolkwacht, 3 named-looking townsfolk). */
    public record Bewoner(double x, double y, double z, float yaw, String skin, int soort) {{
    }}

    public static final List<Wijk> WIJKEN = List.of(
{(',' + chr(10)).join(wijken)});

    public static final Map<String, BlockPos> ALLE = Map.ofEntries(
{(',' + chr(10)).join(regels)});

    public static final List<Huis> HUIZEN = List.of(
{(',' + chr(10)).join(huizen) if huizen else ''});

    /** Viewpoints (a panorama when you first stand there). */
    public static final List<BlockPos> UITKIJKEN = List.of({", ".join(_bp(u) for u in uitkijk)});

    /** Safe spots on the whale (the cloud that catches a falling player puts them back on the nearest one). */
    public static final List<BlockPos> VANGNET = List.of({", ".join(_bp(u) for u in vangnet)});

    public static final List<String> KAMPVUREN = List.of({", ".join('"' + k + '"' for k in kampvuren)});

    private static final String[] SKINS = {{{", ".join('"' + s + '"' for s in skins)}}};
    private static final String[] BEWONERS_PAK = {{
{(',' + chr(10)).join('            "' + d + '"' for d in delen)}}};
    /** The townsfolk of the concept renders (figuren.json + the knights/NPCs of the kit), {len(bew)} figures. */
    public static final List<Bewoner> BEWONERS;

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
    }}

    private Z5WereldPlekken() {{
    }}

    @Nullable
    public static BlockPos plek(String id) {{
        return ALLE.get(id);
    }}

    /** Is this point in zone 5 (on/around the whale, above zone 4)? */
    public static boolean inZone5(double x, double y, double z) {{
        return y >= MIN_Y && x >= MIN_X && x < MAX_X + 1 && z >= MIN_Z && z < MAX_Z + 1;
    }}

    /** The district at this point (null outside zone 5). */
    @Nullable
    public static Wijk wijk(double x, double y, double z) {{
        if (!inZone5(x, y, z)) {{
            return null;
        }}
        for (Wijk w : WIJKEN) {{
            if (w.bevat(x, z)) {{
                return w;
            }}
        }}
        return null;
    }}
}}
"""
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)
    md(alle, info, geb)


def md(alle, info, geb):
    L = ["# POI_Z5 - coordinates of zone 5 \"Hoogvads\" (t-z5wereld slice)", "",
         "**GENERATED** by `tools/vadsrijk/zone5/poi5.py` (via `python tools/vadsrijk/maak_kaart.py --zone 5`). Do not edit by hand. "
         "Java: `nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken` (`plek(\"id\")`, `WIJKEN` / `wijk(x, y, z)`, `inZone5`, "
         "`HUIZEN`, `BEWONERS`, `VANGNET`, `KAMPVUREN`). Dev: `/guhs vadsrijk z5wereld plek <id>` teleports there.", "",
         f"**Layout.** Hoogvads lies on the back of Moeder Wolkwal, ABOVE the east end of zone 4 (same x/z): whale x 2440..3015 (head WEST at "
         f"x ~2440 over the Opstijgpier, tail fluke east to ~3015), axis z -520, ~240 wide. The whale is **lowered 20 blocks** compared to the "
         f"concept export (night round T, decision 2): belly y ~134, city y ~214..236, cathedral floor y 224, Grote Poort ~58 high, spires to "
         f"y ~316. Everything of zone 5 is from y {PL.ZONE_MIN_Y} up (`MIN_Y`); tiles tx {PL.TX0}..{PL.TX1}, tz {PL.TZ0}..{PL.TZ1} "
         f"(tx 38..39 shared with zone 4: zone 4 below y {PL.NAAD_Y}).", "",
         "**Placeholders of other slices:** every id of `Z5VerhaalPlekken` (t-z5verhaal) and `Z5WezensPlekken` (t-z5wezens) is in the list "
         "below with its real spot. t-z5wezens: your rooms inside the whale (Buikgangen, Schuilnest) and every render-y placeholder move "
         f"down {-PL.DY} with the whale: `z5w_buikgangen_min/_max`, `z5w_schuilnest_min/_max` are your boxes shifted; subtract {-PL.DY} from "
         "BUIK_Y / NEST_Y and from your area y-ranges (wolkrand/staartvlakte).", "",
         "All positions are **stand positions** (the air block a player/NPC stands in) in `guhs:vadsrijk`, unless noted "
         "(`haven_5_4_luchtschip` = the middle of the docked ship, `haven_5_4_mast` = top of the mooring mast).", "",
         "## Districts (`Z5WereldPlekken.WIJKEN`, first match wins, from y %d up; biome `guhs:vadsrijk_<biome>`)" % PL.ZONE_MIN_Y, "",
         "| id | x1, z1 .. x2, z2 | biome | name |", "|---|---|---|---|"]
    for g in geb:
        L.append(f"| `{g['id']}` | {g['x1']}, {g['z1']} .. {g['x2']}, {g['z2']} | {g['biome']} | {g['nl']} |")
    L += ["", "## All spots", "", "| id | x, y, z |", "|---|---|"]
    for k in sorted(alle):
        v = alle[k]
        L.append(f"| `{k}` | {v[0]}, {v[1]}, {v[2]} |")
    if info.get("huizen"):
        L += ["", "## Houses with a role (`HUIZEN`)", "", "| id | role | door | inside |", "|---|---|---|---|"]
        for h in info["huizen"]:
            L.append(f"| `{h['id']}` | {h['rol']} | {', '.join(map(str, h['deur']))} | {', '.join(map(str, h['binnen']))} |")
    if info.get("niet_gesnapt"):
        L += ["", "Not snapped (no stand spot within 6 blocks; planned spot kept): " + ", ".join(f"`{k}`" for k in info["niet_gesnapt"])]
    with open(MD, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(L) + "\n")
