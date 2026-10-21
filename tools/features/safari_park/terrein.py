"""
Plattegrond C (the river loop) as real terrain. Every number in park coordinates (0..512) comes from
safari-ontwerp/_bron/plattegrond.py variant C (the approved map); kit.PARK_OFFSET turns them into world coordinates.

Order (bouw): zones + contour -> heights -> water -> ground columns -> fences + gates -> route -> entreedorp plaza ->
vegetation -> decor ring. Every step keeps the escape rule: no ledge, rock or tree within 2-3 blocks of a fence (the
strip along every fence is flattened to the fence's own height).
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt, gaussian_filter, binary_dilation, binary_erosion

from . import kit
from .kit import W, WATER_Y, PARK_OFFSET, state, ruis

ZONES = ["entree", "savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie"]

# --- plattegrond C (park coordinates) ---------------------------------------------------------------------------------
SEEDS = {
    "entree": [(80, 452)],
    "savanne": [(210, 440), (330, 455), (455, 425)],
    "delta": [(445, 300)], "regenwoud": [(330, 300), (445, 175)],
    "bamboe": [(445, 55)], "oertijd": [(310, 70)], "ijskust": [(175, 45), (55, 75)],
    "europa": [(85, 215), (190, 205)], "australie": [(70, 335), (190, 330)],
}
RIVIER = [(522, 30), (450, 110), (380, 140), (300, 130), (220, 140), (140, 120), (90, 170), (120, 250), (220, 260),
          (310, 230), (390, 250), (420, 330), (380, 380), (280, 380), (200, 400), (130, 390), (100, 420)]
MEREN = [((95, 425), 22, 3), ((370, 395), 18, 3), ((75, 60), 40, 9)]      # (centre, radius, depth): last = ijskust sea
ROUTE = ["entree", "savanne", "delta", "regenwoud", "bamboe", "oertijd", "ijskust", "europa", "australie", "entree"]
QUOKKA_MEER = ((215, 300), 13, 2)     # a pond with the quokka island (zone module builds the island details)

# per zone: base height, amplitude, noise scale (sigma), surface style
ZONE_H = {
    "entree": (64, 0.4, 20), "savanne": (64, 1.6, 22), "delta": (62.7, 1.1, 10), "regenwoud": (67, 4.5, 14),
    "bamboe": (72, 13, 18), "ijskust": (67, 6, 16), "oertijd": (69, 9, 20), "europa": (66, 3.5, 16),
    "australie": (64, 1.4, 22),
}


def P(x, z):
    return x + PARK_OFFSET, z + PARK_OFFSET


# ======================================================================================================================
def zones(park):
    rng = np.random.default_rng(kit.SEED + 1)
    zz, xx = np.mgrid[0:W, 0:W].astype(float)          # [z, x]
    xx -= PARK_OFFSET
    zz -= PARK_OFFSET
    wx = ruis(rng, 18) * 14 + ruis(rng, 5) * 3
    wz = ruis(rng, 18) * 14 + ruis(rng, 5) * 3
    best = np.full((W, W), 1e18)
    zone = np.zeros((W, W), int)
    for zi, k in enumerate(ZONES):
        for (sx, sz) in SEEDS[k]:
            d = (xx + wx - sx) ** 2 + (zz + wz - sz) ** 2
            if k == "entree":
                d = d * 1.6
            m = d < best
            best[m] = d[m]
            zone[m] = zi
    N = 512
    d_in = np.maximum(np.maximum(-xx, xx - N + 1), np.maximum(-zz, zz - N + 1))
    binnen = (d_in + ruis(rng, 30) * 14 + ruis(rng, 9) * 3 + 6) < 0
    # keep only the biggest connected piece inside, fill holes
    from scipy.ndimage import label, binary_fill_holes
    lab, n = label(binnen)
    if n > 1:
        sizes = np.bincount(lab.ravel())
        sizes[0] = 0
        binnen = lab == sizes.argmax()
    binnen = binary_fill_holes(binnen)
    # arrays are [z, x] here; the park keeps [x, z]
    park.zone = np.where(binnen, zone, -1).T.copy()
    park.zone_vol = zone.T.copy()              # zone also outside (decor colours / surfaces)
    park.binnen = binnen.T.copy()


def _lijn_afstand(punten, n=W):
    """Distance (in blocks) from every [x, z] to a polyline in world coords."""
    from PIL import Image, ImageDraw
    img = Image.new("L", (n, n), 0)
    dr = ImageDraw.Draw(img)
    dr.line(punten, fill=255, width=1)
    m = np.array(img).T > 0          # [x, z]
    return distance_transform_edt(~m)


def hoogtes(park):
    rng = np.random.default_rng(kit.SEED + 2)
    zv = park.zone_vol
    base = np.zeros((W, W))
    amp = np.zeros((W, W))
    h = np.zeros((W, W))
    for zi, k in enumerate(ZONES):
        b, a, s = ZONE_H[k]
        base[zv == zi] = b
        amp[zv == zi] = a
    base = gaussian_filter(base, 10)
    amp = gaussian_filter(amp, 12)
    n1 = ruis(rng, 22) * 0.6 + ruis(rng, 9) * 0.3 + ruis(rng, 3) * 0.1
    rid = 1 - np.abs(ruis(rng, 26))                  # ridges for the mountains
    h = base + amp * n1
    bam = gaussian_filter((zv == ZONES.index("bamboe")).astype(float), 8)
    oer = gaussian_filter((zv == ZONES.index("oertijd")).astype(float), 8)
    h += bam * np.clip(rid, 0, None) * 14 + oer * np.clip(rid, 0, None) * 8
    # rainforest ravines
    reg = gaussian_filter((zv == ZONES.index("regenwoud")).astype(float), 6)
    rav = np.abs(ruis(rng, 12))
    h -= reg * np.clip(0.25 - rav, 0, None) * 30
    # decor ring: rising to the horizon
    buiten_d = distance_transform_edt(~park.binnen)
    h = np.where(park.binnen, h, h + np.clip(buiten_d - 4, 0, None) * 0.32 + ruis(rng, 10) * 4 * (buiten_d > 6))
    park.h_ruw = h
    # water: river, lakes, sea
    rv = [P(x, z) for x, z in RIVIER]
    d_riv = _lijn_afstand(rv)
    w_riv = 4.2 + ruis(rng, 30) * 0.9
    water = d_riv < w_riv
    diepte = np.where(water, 2 + 2.2 * (1 - d_riv / np.maximum(w_riv, 1)), 0)
    xx, zz = np.mgrid[0:W, 0:W].astype(float)
    park.zee = np.zeros((W, W), bool)
    for mi, ((cx, cz), r, dp) in enumerate(MEREN + [QUOKKA_MEER]):
        wx, wz = P(cx, cz)
        d = np.hypot(xx - wx, zz - wz) + ruis(rng, 6) * 1.5
        meer = d < r
        if mi == 2:
            park.zee = meer.copy()            # the ijskust sea (the route runs along its shore, see route())
        water |= meer
        diepte = np.maximum(diepte, np.where(meer, 1.5 + dp * np.clip(1 - d / r, 0, 1) ** 0.7, 0))
        d_riv = np.minimum(d_riv, np.maximum(d - r + w_riv.mean(), 0))
    # banks slope down to the water
    oever = np.clip(d_riv - w_riv, 0, None)
    h = np.where(water, WATER_Y - diepte, np.minimum(h, WATER_Y + 0.6 + oever * 0.55))
    # delta: shallow pools where the ground dips below the water line
    delta = park.zone_vol == ZONES.index("delta")
    poel = delta & (h < WATER_Y + 0.15) & ~water
    water |= poel
    h = np.where(poel, WATER_Y - 1, h)
    # the quokka island in the middle of its pond
    qx, qz = P(*QUOKKA_MEER[0])
    eil = np.hypot(xx - qx, zz - qz) < 4.5
    water &= ~eil
    h = np.where(eil, WATER_Y + 1.6, h)
    water &= np.round(h) < WATER_Y + 1
    # round 3: THE banks. Land next to water never lies below the water line (else the water runs out in the game and
    # the zones had to "raise every dry column", which made flat plates with buried trees): within 2 blocks of any
    # water the ground is at least the water line (a flush beach where animals drink), further away it may sink again
    # gently (0.8 per block), so a ravine or a low meadow stays a natural slope instead of a plate.
    d_w = distance_transform_edt(~water)
    laag = WATER_Y - np.clip(d_w - 2.0, 0, None) * 0.8
    h = np.where(water, h, np.maximum(h, laag))
    park.water = water
    park.hoogte = np.clip(np.round(h), 4, kit.H - 30).astype(int)
    park.hoogte[park.water] = np.minimum(park.hoogte[park.water], WATER_Y - 1)


def _hek_lijnen(park):
    """Inner fences (zone borders inside the park, not over water) and the outer fence (park edge), 4-connected."""
    z = park.zone
    b = park.binnen
    grens = np.zeros((W, W), bool)
    for dx, dz in ((1, 0), (0, 1)):
        a = z[:W - dx, :W - dz]
        c = z[dx:, dz:]
        verschil = (a != c) & (a >= 0) & (c >= 0)
        # put the fence on the side with the lower zone index (one line, not two)
        kant_a = verschil & (a < c)
        kant_c = verschil & (c < a)
        grens[:W - dx, :W - dz] |= kant_a
        grens[dx:, dz:] |= kant_c
    buiten = b & ~binary_erosion(b)          # outermost ring inside the park
    lijn = (grens | buiten)
    # 4-connect diagonal steps
    extra = np.zeros_like(lijn)
    for dx, dz in ((1, 1), (1, -1)):
        a = lijn[1:-1, 1:-1]
        diag = np.roll(np.roll(lijn, -dx, 0), -dz, 1)[1:-1, 1:-1]
        ortho1 = np.roll(lijn, -dx, 0)[1:-1, 1:-1]
        ortho2 = np.roll(lijn, -dz, 1)[1:-1, 1:-1]
        gat = a & diag & ~ortho1 & ~ortho2
        xs, zs = np.nonzero(gat)
        for x, zz in zip(xs + 1, zs + 1):
            extra[x + dx, zz] = True
    lijn |= extra
    park.hek_buiten = buiten | (extra & ~binary_erosion(b, iterations=2))
    park.hek = lijn


def egaliseer_hekstrook(park):
    """Escape rule: flatten 2 blocks on both sides of every fence to the fence's height; under water keep the bed."""
    lijn = park.hek & ~park.water
    d, (ix, iz) = distance_transform_edt(~lijn, return_indices=True)
    strook = (d <= 2.5) & park.binnen & ~park.water
    park.hoogte = np.where(strook, park.hoogte[ix, iz], park.hoogte)
    park.hek_afstand = d


# ======================================================================================================================
OPP = {   # zone: (top choices with weights, sub, bed)
    "entree": ([("grass_block", 1.0)], "dirt", "sand"),
    "savanne": ([("grass_block", 0.55), ("coarse_dirt", 0.25), ("sand", 0.12), ("rooted_dirt", 0.08)], "dirt", "sand"),
    "delta": ([("mud", 0.45), ("grass_block", 0.35), ("muddy_mangrove_roots", 0.1), ("clay", 0.1)], "mud", "mud"),
    "regenwoud": ([("grass_block", 0.55), ("podzol", 0.25), ("moss_block", 0.2)], "dirt", "gravel"),
    "bamboe": ([("grass_block", 0.65), ("podzol", 0.2), ("moss_block", 0.15)], "dirt", "gravel"),
    "ijskust": ([("snow_block", 0.8), ("packed_ice", 0.12), ("powder_snow_off", 0.0), ("gravel", 0.08)], "stone", "gravel"),
    "oertijd": ([("snow_block", 0.6), ("gravel", 0.15), ("stone", 0.15), ("packed_ice", 0.1)], "stone", "gravel"),
    "europa": ([("grass_block", 0.75), ("podzol", 0.12), ("moss_block", 0.13)], "dirt", "gravel"),
    "australie": ([("red_sand", 0.55), ("coarse_dirt", 0.2), ("terracotta", 0.1), ("orange_terracotta", 0.15)],
                  "red_sandstone", "sand"),
}


def grond(park):
    rng = np.random.default_rng(kit.SEED + 3)
    h = park.hoogte
    zv = park.zone_vol
    # every column down to (lowest neighbour - 2) so cliffs have no holes, at least 4 deep
    buren = h.copy()
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        buren = np.minimum(buren, np.roll(np.roll(h, dx, 0), dz, 1))
    vloer = np.minimum(h - 4, buren - 2)
    vloer = np.maximum(vloer, 1)
    keuze = rng.random((W, W))
    top = np.zeros((W, W), np.uint16)
    sub = np.zeros((W, W), np.uint16)
    bed = np.zeros((W, W), np.uint16)
    for zi, k in enumerate(ZONES):
        m = zv == zi
        tops, s, bd = OPP[k]
        acc = 0.0
        vlek = gaussian_filter(rng.random((W, W)), 2.5)
        vlek = (vlek - vlek.min()) / (vlek.max() - vlek.min() + 1e-9)
        for naam, w in tops:
            if w <= 0:
                continue
            sel = m & (vlek >= acc) & (vlek < acc + w + (1e-6 if acc + w >= 0.999 else 0))
            if acc + w >= 0.999:
                sel = m & (vlek >= acc)
            top[sel] = park.id(_blok(naam))
            acc += w
        sub[m] = park.id(_blok(s))
        bed[m] = park.id(_blok(bd))
    # steep slopes show stone (mountains)
    steil = np.zeros((W, W), bool)
    for dx, dz in ((1, 0), (0, 1)):
        steil |= np.abs(h - np.roll(np.roll(h, dx, 0), dz, 1)) >= 3
    sneeuw_zone = np.isin(zv, [ZONES.index("ijskust"), ZONES.index("oertijd")])
    top = np.where(steil & ~sneeuw_zone & (h > 70), park.id("stone"), top)
    top = np.where(steil & sneeuw_zone, park.id("packed_ice"), top)
    top = np.where(park.water, bed, top)
    stone = park.id("stone")
    for y in range(int(vloer.min()), int(h.max()) + 1):
        m = (y >= vloer) & (y <= h)
        if not m.any():
            continue
        laag = np.where(y == h, top, np.where(y >= h - 2, sub, stone))
        park.v[:, y, :][m] = laag[m]
    # water up to the water line
    wid = park.id("water", level=0)
    for y in range(int(h[park.water].min()) + 1 if park.water.any() else WATER_Y, WATER_Y + 1):
        m = park.water & (y > h)
        park.v[:, y, :][m] = wid
    park.vloer = vloer


def _blok(naam):
    if naam == "grass_block":
        return state("grass_block", snowy="false")
    if naam == "podzol":
        return state("podzol", snowy="false")
    if naam == "muddy_mangrove_roots":
        return state("muddy_mangrove_roots", axis="y")
    return state(naam)


# ======================================================================================================================
def hekken(park):
    """The same safari fence everywhere: dark oak fence + iron bars (mesh) with dark oak log posts every 4 blocks.
    Outer fence one higher. Over water (river crossing the park edge) the mesh goes down to the bed."""
    h = park.hoogte
    xs, zs = np.nonzero(park.hek)
    post = state("dark_oak_log", axis="y")
    for x, z in zip(xs, zs):
        buiten = park.hek_buiten[x, z]
        if park.water[x, z]:
            if not buiten:
                continue                      # inner borders across water: the river is the border there
            for y in range(h[x, z] + 1, WATER_Y + 4):
                park.set(x, y, z, "minecraft:iron_bars")
            if (x + z) % 4 == 0:
                for y in range(h[x, z] + 1, WATER_Y + 5):
                    park.set(x, y, z, post)
            continue
        g = h[x, z]
        hoog = 4 if buiten else 3
        if (x * 7 + z * 3) % 4 == 0:
            for y in range(g + 1, g + hoog + 2):
                park.set(x, y, z, post)
        else:
            park.set(x, g + 1, z, "minecraft:dark_oak_fence")
            for y in range(g + 2, g + hoog + 1):
                park.set(x, y, z, "minecraft:iron_bars")


# ======================================================================================================================
def _catmull(punten, n=40):
    uit = []
    for i in range(len(punten) - 1):
        p0 = punten[max(i - 1, 0)]
        p1, p2 = punten[i], punten[i + 1]
        p3 = punten[min(i + 2, len(punten) - 1)]
        for t in np.linspace(0, 1, n, endpoint=False):
            t2, t3 = t * t, t * t * t
            uit.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2
                                   + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in range(2)))
    uit.append(punten[-1])
    return uit


BRUG_Y = kit.BRUG_Y        # bridge deck block (y 63, 64 free above the water)


def _route_richting(park, x, z):
    """Direction (dx, dz) of the route line nearest to x,z."""
    lijn = getattr(park, "route_lijn", None)
    if not lijn:
        return (0, 1)
    i = min(range(len(lijn)), key=lambda k: (lijn[k][0] - x) ** 2 + (lijn[k][1] - z) ** 2)
    a, b = lijn[max(i - 2, 0)], lijn[min(i + 2, len(lijn) - 1)]
    return (b[0] - a[0], b[1] - a[1])


def _zone_stijl(park, x, z):
    zi = int(park.zone_vol[int(x), int(z)])
    return ZONES[zi] if zi >= 0 else "hout"


def route(park):
    """Footpath = truck route (3 wide), through the zone centres in plattegrond C's order (round 3):
    - one height PROFILE along the line: the deck of a bridge is fixed at kit.BRUG_Y, the land follows the ground but
      never drops more than 1 block per block (half a block per block within 8 blocks of a bridge: a ramp), and the
      profile only ever raises the ground (an embankment), it never cuts into a slope;
    - every cell of the 3-wide path takes the height of its nearest line point, so the path has no steps sideways;
    - path over water = a bridge from kit.brug_cellen (one design per zone), gates where the path crosses a fence."""
    zcent = {}
    for zi, k in enumerate(ZONES):
        xs, zs = np.nonzero(park.zone == zi)
        zcent[k] = (xs.mean(), zs.mean())
    pts = [zcent[k] for k in ROUTE]
    ruw = _catmull(pts, 60)
    lijn = []
    for (ax, az), (bx, bz) in zip(ruw, ruw[1:]):          # dense: a point every half block, no gaps in the path
        n = max(1, int(math.hypot(bx - ax, bz - az) * 2))
        for i in range(n):
            t = i / n
            lijn.append((ax + (bx - ax) * t, az + (bz - az) * t))
    lijn.append(ruw[-1])
    park.route_lijn = lijn
    n = len(lijn)
    cel = [(int(round(x)), int(round(z))) for x, z in lijn]
    h = park.hoogte
    # the 3-wide path: every cell takes its nearest line point
    pad = np.zeros((W, W), bool)
    doel = {}
    for i, ((fx, fz), (cx, cz)) in enumerate(zip(lijn, cel)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                xi, zi = cx + dx, cz + dz
                if not (0 <= xi < W and 0 <= zi < W) or not park.binnen[xi, zi]:
                    continue
                d = (xi - fx) ** 2 + (zi - fz) ** 2
                if (xi, zi) not in doel or d < doel[(xi, zi)][0]:
                    doel[(xi, zi)] = (d, i)
                pad[xi, zi] = True
    park.pad = pad
    # where the route runs along the ijskust sea shore (not across a river): the sea under and beside the path becomes
    # real shore (stone, snow, the path on top) - a coast path, not a long bridge in the sea (round 3: this replaces the
    # zone's own 'kust_pad' fix, which tore the bridge down)
    zee = getattr(park, "zee", None)
    if zee is not None:
        kruis = pad & park.water & zee
        if kruis.any():
            strook = binary_dilation(kruis, iterations=3) & park.water & zee
            steen = park.id("stone")
            for x, z in zip(*np.nonzero(strook)):
                for y in range(int(h[x, z]) + 1, WATER_Y):
                    park.v[x, y, z] = steen
                park.set(x, WATER_Y, z, "snow_block")
                for y in range(WATER_Y + 1, WATER_Y + 5):
                    park.v[x, y, z] = 0
                park.water[x, z] = False
                h[x, z] = WATER_Y
    # --- the profile (in blocks: the y of the top block you walk on) ---
    nat = np.array([bool(park.water[c]) for c in cel])
    t = np.array([float(BRUG_Y) if nat[i] else float(h[c]) for i, c in enumerate(cel)])
    # distance (in samples) to the nearest bridge sample: ramps near bridges are gentler
    idx = np.nonzero(nat)[0]
    if len(idx):
        d_brug = np.min(np.abs(np.arange(n)[:, None] - idx[None, :]), axis=1) / 2.0
    else:
        d_brug = np.full(n, 1e9)
    stap = np.where(d_brug <= 8, 0.25, 0.5)                 # max drop per sample (samples are half a block apart)
    for _ in range(3):
        for i in range(1, n):                               # raise only: no drop bigger than stap
            t[i] = max(t[i], t[i - 1] - stap[i])
        for i in range(n - 2, -1, -1):
            t[i] = max(t[i], t[i + 1] - stap[i])
        t[nat] = BRUG_Y
    # quantize: half blocks near a bridge (slabs), whole blocks elsewhere
    t = np.where(d_brug <= 8, np.ceil(t * 2) / 2, np.ceil(t))
    park.route_profiel = t
    padblok = park.id("dirt_path")
    dek = []
    for (x, z), (_, i) in doel.items():
        if park.water[x, z]:
            dek.append((x, z))
            continue
        ty = float(t[i])
        g = int(h[x, z])
        vol = int(math.floor(ty))
        half = ty - vol >= 0.5
        if d_brug[i] <= 8 and (ty > g or half):
            plank, slab, _, _ = kit._stijl(_zone_stijl(park, x, z))
            for y in range(g + 1, vol + 1):
                park.set(x, y, z, plank)
            if vol <= g:
                park.v[x, g, z] = park.id(plank) if half else padblok
            if half:
                park.set(x, vol + 1, z, slab, type="bottom", waterlogged="false")
            boven = vol + 1 + (1 if half else 0)
            h[x, z] = max(vol, g)
        else:
            sub = park.id("dirt")
            for y in range(g + 1, vol):
                park.v[x, y, z] = sub
            park.v[x, max(vol, g), z] = padblok
            boven = max(vol, g) + 1
            h[x, z] = max(vol, g)
        for y in range(boven, min(boven + 4, kit.H)):         # 4 free above the path (the truck)
            park.v[x, y, z] = 0
    # bridges: one per connected stretch of path over water, in its zone's style
    from scipy.ndimage import label as _label
    dekmask = np.zeros((W, W), bool)
    for c in dek:
        dekmask[c] = True
    lab, nb = _label(dekmask, structure=np.ones((3, 3), bool))
    park.bruggen = []
    for k in range(1, nb + 1):
        bx, bz = np.nonzero(lab == k)
        stijl = _zone_stijl(park, int(bx.mean()), int(bz.mean()))
        kit.brug_cellen(park, list(zip(bx.tolist(), bz.tolist())), stijl)
    # the embankments: the ground beside a raised path slopes down to the land (no wall, no hole beside the path)
    _bermen(park)
    # gates where the path crosses a fence: the fence cells on the path become gates, the mesh above them goes
    for x, z in zip(*np.nonzero(pad & park.hek & ~park.water)):
        g = int(park.hoogte[x, z])                  # the path surface (not the top of a fence post standing here)
        for y in range(g + 1, g + 7):
            park.v[x, y, z] = 0
        lijn_x = (x > 0 and park.hek[x - 1, z]) or (x < W - 1 and park.hek[x + 1, z])
        lijn_z = (z > 0 and park.hek[x, z - 1]) or (z < W - 1 and park.hek[x, z + 1])
        if lijn_x and not lijn_z:
            facing = "north"
        elif lijn_z and not lijn_x:
            facing = "east"
        else:
            rdx, rdz = _route_richting(park, x, z)
            facing = "north" if abs(rdz) >= abs(rdx) else "east"
        park.set(x, g + 1, z, kit.GATE, facing=facing, in_wall="false", open="false", powered="false")
        park.set(x, g + 4, z, "minecraft:dark_oak_log", axis="x" if facing == "north" else "z")


def _pad_top(park, x, z):
    """y of the highest solid block of a path column (the surface you walk on, slabs included)."""
    col = park.v[x, :, z]
    ys = np.nonzero(col)[0]
    return int(ys[-1]) if len(ys) else int(park.hoogte[x, z])


def _bermen(park, breed=3):
    """Beside a path that was raised above the ground: the cells next to it get ground up to (path - 1 per block), so
    a raised path has sloping sides (a berm), not a wall with a drop."""
    pad = park.pad
    d, (ix, iz) = distance_transform_edt(~pad, return_indices=True)
    rand = (d > 0) & (d <= breed) & ~park.water & ~park.hek & park.binnen
    h = park.hoogte
    for x, z in zip(*np.nonzero(rand)):
        px, pz = ix[x, z], iz[x, z]
        if park.water[px, pz]:
            continue
        doel = int(h[px, pz]) - int(math.ceil(d[x, z]))
        g = int(h[x, z])
        if doel <= g:
            continue
        top = park.v[x, g, z]
        for y in range(g, doel):
            park.v[x, y, z] = park.id("dirt")
        park.v[x, doel, z] = top if top else park.id("dirt")
        h[x, z] = doel
        # plants / snow standing on the old ground are buried: clear what is in the new ground's way
        for y in range(doel + 1, doel + 3):
            if park.v[x, y, z] and park.pal[park.v[x, y, z]].split("[")[0] in (
                    "minecraft:short_grass", "minecraft:snow", "minecraft:fern", "minecraft:dead_bush"):
                park.v[x, y, z] = 0


_OEVER_GROND = ("grass_block", "dirt", "coarse_dirt", "sand", "red_sand", "mud", "clay", "podzol", "moss_block",
                "snow_block", "gravel", "rooted_dirt")


def oeverlijn(park, log=print, max_meer=4000):
    """Round 3, the oeverlijn rule: every pond / lake / drinking pool (a water body smaller than max_meer columns: the
    billabong, the savanne poel, the delta pools, the brook ponds; not the river or the sea) gets soft banks: a bank
    column right next to the water whose top is 1 above the water becomes a beach at the water line, one that is 2 above
    steps down by 1 - so animals walk in and out (the kangaroos could not leave the billabong). Only plain ground; paths,
    fences and buildings stay."""
    from scipy.ndimage import label as _label
    lab, n = _label(park.water)
    if not n:
        return 0
    grootte = np.bincount(lab.ravel())
    klein = np.zeros(n + 1, bool)
    klein[1:] = grootte[1:] < max_meer
    plas = klein[lab]
    rand = binary_dilation(plas) & ~park.water & park.binnen & ~park.pad & ~park.hek
    if getattr(park, "hek_afstand", None) is not None:
        rand &= park.hek_afstand > 2.5
    h = park.hoogte
    grond = np.array([s.split("[")[0].split(":")[-1] in _OEVER_GROND for s in park.pal], bool)
    n_aan = 0
    for x, z in zip(*np.nonzero(rand)):
        g = int(h[x, z])
        if g <= WATER_Y or g > WATER_Y + 2:
            continue
        if not grond[park.v[x, g, z]] or (park.v[x, g + 1, z] and park.pal[park.v[x, g + 1, z]].split("[")[0].split(":")[-1]
                                          not in ("short_grass", "fern", "snow", "dead_bush", "tall_grass")):
            continue
        top = park.v[x, g, z]
        nieuw = g - 1
        for y in range(nieuw + 1, g + 3):
            if y < kit.H and park.v[x, y, z] and not park.pal[park.v[x, y, z]].endswith("_leaves"):
                park.v[x, y, z] = 0
        park.v[x, nieuw, z] = top
        h[x, z] = nieuw
        n_aan += 1
    if n_aan:
        log(f"  oeverlijn: {n_aan} oeverkolommen van plassen verlaagd (dieren kunnen erin en eruit)")
    return n_aan


def bruggen_nalopen(park, log=print):
    """After the zone modules (round 3): every stretch of path over water must have a bridge deck; a stretch that has
    none (a zone made new water under the path, e.g. a brook) gets one from kit.brug_cellen. Zones never build or raise
    route bridges themselves."""
    from scipy.ndimage import label as _label
    dekmask = park.pad & park.water
    lab, nb = _label(dekmask, structure=np.ones((3, 3), bool))
    nieuw = 0
    for k in range(1, nb + 1):
        bx, bz = np.nonzero(lab == k)
        heeft = sum(1 for x, z in zip(bx.tolist(), bz.tolist()) if park.v[x, BRUG_Y, z] != 0)
        if heeft >= len(bx) * 0.9:
            continue
        stijl = _zone_stijl(park, int(bx.mean()), int(bz.mean()))
        kit.brug_cellen(park, list(zip(bx.tolist(), bz.tolist())), stijl)
        nieuw += 1
    if nieuw:
        log(f"  bruggen nagelopen: {nieuw} brug(gen) gebouwd onder het pad")
    oeverlijn(park, log)
    # the path after the zone modules (a village, a brook, a coast path): no jump > 1 block, BEFORE the rides read the
    # path heights (zone_s3_rit runs after this)
    from . import controle
    n = controle.padsprongen(park, True)
    if n:
        log(f"  padsprongen na de zones: {n} (opgehoogd)")


# ======================================================================================================================
def entreedorp(park):
    """A flat placeholder plaza for the entreedorp (the lobby slice builds the real village); the entrance spawn point."""
    ezi = ZONES.index("entree")
    g = 64
    m = park.zone == ezi
    # the plaza centre: the spot in the entree zone farthest from water, fences and the zone edge
    ruimte = distance_transform_edt(m & ~park.water & ~park.hek)
    cx, cz = np.unravel_index(int(np.argmax(ruimte)), ruimte.shape)
    cx, cz = int(cx), int(cz)
    # flatten the whole entree zone to y 64 (but not the fence strip, already flat at its own height)
    vlak = m & ~park.water & (park.hek_afstand > 2.5)
    for x, z in zip(*np.nonzero(vlak)):
        oud = park.hoogte[x, z]
        if oud > g:
            for y in range(g + 1, oud + 1):
                park.v[x, y, z] = 0
        elif oud < g:
            for y in range(oud, g):
                park.set(x, y, z, "dirt")
        park.hoogte[x, z] = g
        if park.pad[x, z]:
            park.set(x, g, z, "dirt_path")
        else:
            park.set(x, g, z, "grass_block", snowy="false")
    for x in range(cx - 16, cx + 17):
        for z in range(cz - 16, cz + 17):
            d = math.hypot(x - cx, z - cz)
            if d <= 14.5 and m[x, z] and not park.water[x, z]:
                for y in range(int(park.vloer[x, z]), g):          # solid underneath (no floating plaza)
                    park.set(x, y, z, "dirt", keep=True)
                for y in range(g + 1, g + 4):
                    park.v[x, y, z] = 0
                park.set(x, g, z, "smooth_sandstone" if d < 13.5 else "cut_sandstone")
    park.set(cx, g + 1, cz, "minecraft:dark_oak_log", axis="y")       # marker in the middle (fountain later)
    # a path from the plaza to the route
    px, pz = np.nonzero(park.pad & ~park.water)
    d = np.hypot(px - cx, pz - cz)
    d[d < 15] = 1e9
    i = int(np.argmin(d))
    tx, tz = int(px[i]), int(pz[i])
    n = int(math.hypot(tx - cx, tz - cz) * 2) + 1
    for k in range(n + 1):
        fx, fz = cx + (tx - cx) * k / n, cz + (tz - cz) * k / n
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                x, z = int(round(fx)) + dx, int(round(fz)) + dz
                if math.hypot(x - cx, z - cz) <= 14.5 or park.water[x, z] or park.hek[x, z]:
                    continue
                park.pad[x, z] = True
                park.set(x, int(park.hoogte[x, z]), z, "dirt_path")
                for y in range(int(park.hoogte[x, z]) + 1, int(park.hoogte[x, z]) + 4):
                    park.v[x, y, z] = 0
    park.ingang = (cx + 0.5, g + 1, cz + 4.5, 180.0)
    park.plein = (cx, g, cz)


# ======================================================================================================================
PLANT = {
    "entree": [("bloem", 0.02), ("gras", 0.10)],
    "savanne": [("gras", 0.35), ("hoog_gras", 0.06), ("dor", 0.02)],
    "delta": [("gras", 0.18), ("riet", 0.05), ("varen", 0.03)],
    "regenwoud": [("varen", 0.25), ("grote_varen", 0.06), ("gras", 0.15), ("struik", 0.025)],
    "bamboe": [("bamboe", 0.20), ("gras", 0.12), ("bloem", 0.02), ("struik", 0.01)],
    "ijskust": [("sneeuw", 0.35)],
    "oertijd": [("sneeuw", 0.45), ("dor", 0.01)],
    "europa": [("gras", 0.30), ("varen", 0.08), ("bloem", 0.05), ("paddenstoel", 0.01), ("struik", 0.01)],
    "australie": [("dor", 0.03), ("gras", 0.10), ("struik", 0.005)],
}
BOMEN = {   # zone: (soort, kans per kolom)
    "entree": [("eik", 0.0006)],
    "savanne": [("acacia", 0.0016), ("baobab", 0.00004)],
    "delta": [("mangrove", 0.004), ("wilg", 0.0008)],
    "regenwoud": [("jungle", 0.012), ("reus", 0.0004)],
    "bamboe": [("kers", 0.0012), ("spar", 0.0008)],
    "ijskust": [("spar", 0.0003)],
    "oertijd": [("spar", 0.0012), ("dode", 0.0006)],
    "europa": [("eik", 0.006), ("berk", 0.004), ("spar", 0.001)],
    "australie": [("eucalyptus", 0.0012), ("acacia", 0.0004)],
}


def begroeiing(park):
    rng = np.random.default_rng(kit.SEED + 4)
    h = park.hoogte
    vrij = ~park.water & ~park.pad & (park.hek_afstand > 3.5)
    # keep the reserved landmark spots free of trees
    xx, zz = np.mgrid[0:W, 0:W]
    gereserveerd = np.zeros((W, W), bool)
    for naam, (rx, rz, r, _) in kit.RESERVERINGEN.items():
        gereserveerd |= np.hypot(xx - rx, zz - rz) <= r
    plein = park.plein
    gereserveerd |= np.hypot(xx - plein[0], zz - plein[2]) <= 18
    # paths: no trees within 3, no plants on them
    pad_d = distance_transform_edt(~park.pad)
    # --- trees ---
    for zi, k in enumerate(ZONES):
        m = (park.zone_vol == zi) & vrij & ~gereserveerd & (pad_d > 3)
        buiten = ~park.binnen
        for soort, kans in BOMEN[k]:
            p = np.where(buiten, kans * 0.35, kans)
            sel = m & (rng.random((W, W)) < p)
            for x, z in zip(*np.nonzero(sel)):
                if x < 6 or z < 6 or x > W - 7 or z > W - 7:
                    continue
                g = int(h[x, z])
                if park.v[x, g, z] == 0 or park.get(x, g, z).startswith(("minecraft:water", "minecraft:packed_ice")):
                    continue
                top = park.boom(soort, x, g, z, grootte=float(rng.uniform(0.6, 1.4)))
                park.bomen.append((int(x), int(top), int(z), soort))
    # --- ground plants (only in empty air above a solid top that can hold them) ---
    gras = state("short_grass")
    for zi, k in enumerate(ZONES):
        m = (park.zone_vol == zi) & vrij & ~gereserveerd & ~park.pad
        m &= rng.random((W, W)) < np.where(park.binnen, 1.0, 0.5)
        for soort, kans in PLANT[k]:
            sel = m & (rng.random((W, W)) < kans)
            xs, zs = np.nonzero(sel)
            for x, z in zip(xs, zs):
                g = int(h[x, z])
                if park.v[x, g + 1, z] != 0 or park.v[x, g, z] == 0:
                    continue
                onder = park.get(x, g, z)
                if soort == "sneeuw":
                    if not onder.startswith(("minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice")):
                        park.set(x, g + 1, z, "minecraft:snow", layers=1)     # (snow never lies on ice)
                    continue
                onder_naam = onder.split("[")[0]
                aarde = onder_naam in ("minecraft:grass_block", "minecraft:podzol", "minecraft:moss_block",
                                       "minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:dirt",
                                       "minecraft:mud")
                zand = onder_naam in ("minecraft:sand", "minecraft:red_sand")
                if not aarde and not (zand and soort in ("dor", "riet")):
                    if zand and soort == "gras":
                        park.set(x, g + 1, z, "minecraft:dead_bush")     # dry tufts on the sand instead of grass
                    continue
                if soort == "gras":
                    park.v[x, g + 1, z] = park.id(gras)
                elif soort == "hoog_gras" and park.v[x, g + 2, z] == 0:
                    park.set(x, g + 1, z, "minecraft:tall_grass", half="lower")
                    park.set(x, g + 2, z, "minecraft:tall_grass", half="upper")
                elif soort == "grote_varen" and park.v[x, g + 2, z] == 0:
                    park.set(x, g + 1, z, "minecraft:large_fern", half="lower")
                    park.set(x, g + 2, z, "minecraft:large_fern", half="upper")
                elif soort == "varen":
                    park.set(x, g + 1, z, "minecraft:fern")
                elif soort == "dor":
                    park.set(x, g + 1, z, "minecraft:dead_bush")
                elif soort == "bloem":
                    b = ["dandelion", "poppy", "cornflower", "oxeye_daisy", "allium", "azure_bluet", "pink_tulip"]
                    park.set(x, g + 1, z, "minecraft:" + b[int(rng.integers(0, len(b)))])
                elif soort == "paddenstoel":
                    park.set(x, g + 1, z, "minecraft:" + ("red_mushroom" if rng.random() < 0.5 else "brown_mushroom"))
                elif soort == "riet":
                    # sugar cane needs water next to it
                    if any(park.water[min(max(x + dx, 0), W - 1), min(max(z + dz, 0), W - 1)]
                           for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                        for y in range(g + 1, g + 1 + int(rng.integers(2, 4))):
                            park.set(x, y, z, "minecraft:sugar_cane", age=0)
                elif soort == "bamboe":
                    hh = int(rng.integers(5, 13))
                    for i in range(hh):
                        blad = "large" if i >= hh - 2 else ("small" if i >= hh - 4 else "none")
                        park.set(x, g + 1 + i, z, "minecraft:bamboe" if False else "minecraft:bamboo",
                                 age=1 if i < hh - 1 else 0, leaves=blad, stage=0)
                elif soort == "struik":
                    blad = {"regenwoud": "jungle", "bamboe": "azalea", "europa": "oak", "australie": "acacia"}.get(k, "oak")
                    park.blob(x, g + 1, z, 1.3, 1.0, 1.3, park.leaves(blad))
    # water plants: lily pads in the delta and on the lakes, seagrass in the deeper water
    for x, z in zip(*np.nonzero(park.water & (rng.random((W, W)) < 0.03))):
        k = ZONES[park.zone_vol[x, z]]
        if k in ("delta", "europa", "australie", "entree") and park.v[x, WATER_Y + 1, z] == 0 and not park.pad[x, z]:
            park.set(x, WATER_Y + 1, z, "minecraft:lily_pad")
    for x, z in zip(*np.nonzero(park.water & (rng.random((W, W)) < 0.12))):
        g = int(park.hoogte[x, z])
        if g < WATER_Y - 1 and park.get(x, g + 1, z).startswith("minecraft:water"):
            park.set(x, g + 1, z, "minecraft:seagrass")
    # ice floes on the ijskust sea
    ijs = park.water & (park.zone_vol == ZONES.index("ijskust")) & (ruis(rng, 2.5) > 1.0)
    for x, z in zip(*np.nonzero(ijs)):
        if not park.pad[x, z]:
            park.set(x, WATER_Y, z, "minecraft:ice")


def kopjes(park):
    """A few rock domes (kopjes) on the savanne and boulders in the oertijd / australie, never near a fence or path."""
    rng = np.random.default_rng(kit.SEED + 5)
    pad_d = distance_transform_edt(~park.pad)
    for k, n, blokken in (("savanne", 9, ("granite", "stone", "andesite")),
                          ("oertijd", 7, ("cobblestone", "mossy_cobblestone", "stone")),
                          ("australie", 5, ("orange_terracotta", "red_sandstone", "terracotta"))):
        zi = ZONES.index(k)
        xs, zs = np.nonzero((park.zone == zi) & ~park.water & (park.hek_afstand > 9) & (pad_d > 6))
        geplaatst = 0
        for _ in range(400):
            if geplaatst >= n or not len(xs):
                break
            i = int(rng.integers(0, len(xs)))
            x, z = int(xs[i]), int(zs[i])
            if any(math.hypot(x - rx, z - rz) < r + 6 for rx, rz, r, _ in kit.RESERVERINGEN.values()):
                continue
            r = float(rng.uniform(2.5, 5.5))
            g = int(park.hoogte[x, z])
            for dx in range(-7, 8):
                for dz in range(-7, 8):
                    for dy in range(-2, 7):
                        d = math.sqrt(dx * dx + (dy * 1.5) ** 2 + dz * dz)
                        if d <= r + rng.random() * 0.6:
                            park.set(x + dx, g + dy, z + dz, "minecraft:" + blokken[int(rng.integers(0, 3))])
            # nothing floating: fill under every rock column down to the ground
            for dx in range(-7, 8):
                for dz in range(-7, 8):
                    xx, zz = x + dx, z + dz
                    col = np.nonzero(park.v[xx, :, zz])[0]
                    if len(col):
                        for y in range(int(park.vloer[xx, zz]), int(col[-1])):
                            if park.v[xx, y, zz] == 0:
                                park.v[xx, y, zz] = park.id("minecraft:stone")
            geplaatst += 1


def bouw(park, log=print):
    import time
    t0 = time.time()
    zones(park); log(f"  zones {time.time() - t0:.0f}s")
    hoogtes(park); log(f"  hoogtes {time.time() - t0:.0f}s")
    _hek_lijnen(park)
    egaliseer_hekstrook(park)
    grond(park); log(f"  grond {time.time() - t0:.0f}s")
    hekken(park); log(f"  hekken {time.time() - t0:.0f}s")
    route(park); log(f"  route {time.time() - t0:.0f}s")
    entreedorp(park)
    kopjes(park)
    begroeiing(park); log(f"  begroeiing {time.time() - t0:.0f}s ({len(park.bomen)} bomen)")
