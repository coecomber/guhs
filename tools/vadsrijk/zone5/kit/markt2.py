"""De Marktlaan van Hoogvads (ronde 2): veel soorten kraampjes, luifels, koopwaar en kleuren + de Grote Soepkeuken.
Wereldcoördinaten (spel-y). Alles via bouwhulp/palet."""
import math
import numpy as np
import palet
from palet import P, trap, plaat
import bouwhulp as BH
import figuren as FG

WOL = ["pink", "white", "light_blue", "yellow", "lime", "magenta", "orange", "purple", "cyan", "red"]
WAAR = {
    "fruit": ["minecraft:melon", "minecraft:pumpkin", "minecraft:red_mushroom_block[down=true,east=true,north=true,"
              "south=true,up=true,west=true]", "minecraft:hay_block[axis=y]"],
    "taart": ["minecraft:cake[bites=0]", "minecraft:cake[bites=2]", "minecraft:honey_block", "guhs:gouden_kaasje"],
    "stof": ["minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:white_wool",
             "minecraft:magenta_wool", "minecraft:lime_wool"],
    "bloem": ["minecraft:potted_pink_tulip", "minecraft:potted_allium", "minecraft:potted_cornflower",
              "minecraft:potted_azure_bluet", "minecraft:potted_red_tulip", "minecraft:potted_oxeye_daisy"],
    "vat": ["minecraft:barrel[facing=up,open=false]", "minecraft:barrel[facing=up,open=true]",
            "minecraft:composter[level=8]"],
    "licht": ["minecraft:lantern[hanging=false,waterlogged=false]", "guhs:lampion_roze", "guhs:lampion_geel",
              "guhs:lampion_mint"],
}


def _w(kleur):
    return f"minecraft:{kleur}_wool"


def _waar(W, x, y, z, soort, rng):
    lijst = WAAR[soort]
    b = lijst[rng.randint(len(lijst))]
    if palet.MODUS == "vanilla" and b.startswith("guhs:"):
        b = "minecraft:yellow_concrete"
    BH.zet(W, x, y, z, b)


def toonbank(W, x1, x2, y, z, rng, soort):
    for x in range(x1, x2 + 1):
        BH.zet(W, x, y, z, "minecraft:stripped_cherry_wood[axis=x]")
        if rng.rand() < 0.8:
            _waar(W, x, y + 1, z, soort, rng)


def kraam_luifel(W, x, y, z, rng):
    """Klassiek: 5x3, gestreepte schuine luifel (trappen-rand), toonbank vooraan."""
    k1, k2 = rng.choice(WOL, 2, replace=False)
    for (dx, dz) in ((0, 0), (4, 0), (0, 2), (4, 2)):
        BH.doos(W, x + dx, y, z + dz, x + dx, y + 3, z + dz, "minecraft:pale_oak_fence")
    for dx in range(-1, 6):
        for dz in range(-1, 4):
            BH.zet(W, x + dx, y + 4 + (1 if dz >= 2 else 0), z + dz, _w(k1 if dx % 2 else k2))
    for dx in range(-1, 6):
        BH.zet(W, x + dx, y + 3, z - 2, f"minecraft:{k1 if dx % 2 else k2}_carpet")
    toonbank(W, x, x + 4, y, z, rng, rng.choice(["fruit", "taart", "stof", "bloem"]))
    BH.zet(W, x + 2, y + 3, z + 1, "minecraft:lantern[hanging=true,waterlogged=false]")


def kraam_tent(W, x, y, z, rng):
    """Puntige tent (zadeldak van wol) met wimpel en vaatjes."""
    k = rng.choice(WOL)
    for (dx, dz) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        BH.doos(W, x + dx, y, z + dz, x + dx, y + 2, z + dz, "minecraft:stripped_spruce_log[axis=y]")
    for i in range(3):
        for dz in range(-1, 6):
            BH.zet(W, x - 1 + i, y + 3 + i, z + dz, _w(k) if i % 2 == 0 else "minecraft:white_wool")
            BH.zet(W, x + 5 - i, y + 3 + i, z + dz, _w(k) if i % 2 == 0 else "minecraft:white_wool")
    for dz in range(-1, 6):
        BH.zet(W, x + 2, y + 6, z + dz, _w(k))
    BH.doos(W, x + 2, y + 7, z + 2, x + 2, y + 9, z + 2, "minecraft:pale_oak_fence")
    BH.zet(W, x + 2, y + 10, z + 2, "goud")
    for dz in (1, 3):
        _waar(W, x + 1, y, z + dz, "vat", rng)
        _waar(W, x + 3, y, z + dz, "stof", rng)


def kraam_kar(W, x, y, z, rng):
    """Handkar met wielen, twee lange handvatten en een parasolletje."""
    BH.doos(W, x, y + 1, z, x + 3, y + 1, z + 1, "minecraft:spruce_planks")
    for (dx, dz) in ((0, -1), (3, -1), (0, 2), (3, 2)):
        BH.zet(W, x + dx, y, z + dz, "minecraft:dark_oak_log[axis=z]")
    BH.zet(W, x + 4, y + 1, z, "minecraft:pale_oak_fence")
    BH.zet(W, x + 4, y + 1, z + 1, "minecraft:pale_oak_fence")
    for dx in range(4):
        _waar(W, x + dx, y + 2, z, rng.choice(["fruit", "bloem"]), rng)
        _waar(W, x + dx, y + 2, z + 1, rng.choice(["fruit", "taart"]), rng)
    parasol(W, x + 1, y + 2, z + 1, rng, h=3, r=2)


def parasol(W, x, y, z, rng, h=4, r=3):
    k1, k2 = rng.choice(WOL, 2, replace=False)
    BH.doos(W, x, y, z, x, y + h, z, "minecraft:pale_oak_fence")
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d <= r + 0.3:
                a = math.atan2(dz, dx)
                kl = k1 if int((a + math.pi) / (math.pi / 4)) % 2 else k2
                BH.zet(W, x + dx, y + h + (1 if d < r * 0.5 else 0), z + dz, f"minecraft:{kl}_carpet" if d > r * 0.5
                       else _w(kl))
    BH.zet(W, x, y + h + 2, z, "goud")


def kraam_bloemen(W, x, y, z, rng):
    """Bloemenstal: getrapte rekjes vol potjes, met een boog van bladeren erboven."""
    for k in range(3):
        BH.doos(W, x, y + k, z + k, x + 4, y + k, z + k, plaat("wit_plaat", "top") if palet.MODUS != "x" else None)
        for dx in range(5):
            if rng.rand() < 0.85:
                _waar(W, x + dx, y + k + 1, z + k, "bloem", rng)
    for dx in range(-1, 6):
        h = int(4 + 1.5 * math.sin(math.pi * (dx + 1) / 6))
        BH.zet(W, x + dx, y + h + 1, z + 1, "minecraft:flowering_azalea_leaves")
    BH.doos(W, x - 1, y, z + 1, x - 1, y + 4, z + 1, "minecraft:flowering_azalea_leaves")
    BH.doos(W, x + 5, y, z + 1, x + 5, y + 4, z + 1, "minecraft:flowering_azalea_leaves")


def kraam_bakker(W, x, y, z, rng):
    """Houten kraam met puntdakje (dakpannen), uithangbord en taarten."""
    BH.doos(W, x, y, z, x + 4, y, z + 3, "minecraft:spruce_planks")
    BH.doos(W, x, y + 1, z + 3, x + 4, y + 3, z + 3, "minecraft:spruce_planks")
    for dx in (0, 4):
        BH.doos(W, x + dx, y + 1, z, x + dx, y + 3, z + 2, "minecraft:stripped_spruce_log[axis=y]")
    BH.zadeldak(W, x, z, x + 4, z + 3, y + 4, nok="z", dak="dak_roze", overstek=1)
    toonbank(W, x + 1, x + 3, y + 1, z, rng, "taart")
    BH.zet(W, x + 2, y + 3, z - 1, "minecraft:cherry_wall_hanging_sign[facing=north,waterlogged=false]")


def kraam_kaas(W, x, y, z, rng):
    """Kaas- en honingkraam: vaten, een gele luifel met franje, kaasblokken."""
    for (dx, dz) in ((0, 0), (5, 0)):
        BH.doos(W, x + dx, y, z + dz, x + dx, y + 3, z + dz, "minecraft:stripped_birch_log[axis=y]")
    for dx in range(-1, 7):
        BH.zet(W, x + dx, y + 4, z, "minecraft:yellow_wool")
        BH.zet(W, x + dx, y + 4, z + 1, "minecraft:yellow_wool")
        BH.zet(W, x + dx, y + 3, z - 1, "minecraft:white_carpet" if dx % 2 else "minecraft:yellow_carpet")
    for dx in range(0, 6):
        _waar(W, x + dx, y, z + 1, rng.choice(["vat", "taart"]), rng)
        if dx % 2 == 0:
            _waar(W, x + dx, y + 1, z + 1, "taart", rng)


SOORTEN = [kraam_luifel, kraam_tent, kraam_kar, kraam_bloemen, kraam_bakker, kraam_kaas]


def markt(W, S, xa=-72, xb=-8, y0=238):
    rng = np.random.RandomState(4)
    # plaveisel met een patroon: banen van tegels, randen van wolkensteen, dambord rond de fontein
    for x in range(xa, xb + 1):
        for z in range(-24, 25):
            blok = P["vloer2"] if (x // 3 + z // 3) % 2 else P["steen"]
            if abs(z) <= 5:
                blok = P["steen"] if (x % 6 in (0, 1)) else P["vloer2"]
            BH.zet(W, x, y0, z, blok)
    cx = (xa + xb) // 2 - 6
    for x in range(cx - 10, cx + 11):
        for z in range(-10, 11):
            if math.hypot(x - cx, z) <= 10.5:
                BH.zet(W, x, y0, z, P["vloer"])
    # fontein met getrapte schalen en het beeld van de koningin
    for dx in range(-7, 8):
        for dz in range(-7, 8):
            dd = math.hypot(dx, dz)
            if dd <= 7.3:
                BH.zet(W, cx + dx, y0 + 1, dz, "wit_glad" if dd > 6.2 else "minecraft:water")
            if dd <= 4.2:
                BH.zet(W, cx + dx, y0 + 3, dz, "wit_glad" if dd > 3.3 else "minecraft:water")
            if dd <= 2.0:
                BH.zet(W, cx + dx, y0 + 5, dz, "goud" if dd > 1.2 else "minecraft:water")
    BH.doos(W, cx, y0 + 1, 0, cx, y0 + 4, 0, "wit_glad")
    BH.doos(W, cx - 1, y0 + 2, 0, cx + 1, y0 + 2, 0, "wit_glad")
    BH.doos(W, cx, y0 + 2, -1, cx, y0 + 2, 1, "wit_glad")
    import kathedraal as KA
    KA.beeld_guh(W, cx, y0 + 7, 0, 7, "beeld", "east")
    for k in range(10):  # waterstraaltjes
        a = k * math.pi / 5
        W.deeltjes.append((cx + 0.5 + math.cos(a) * 3, y0 + 5.5, 0.5 + math.sin(a) * 3, (180, 225, 255), 0.25, 0.4,
                           "stip"))
    # kraampjes in twee rijen aan weerszijden, gemengde soorten
    plekken = []
    for x in range(xa + 2, xb - 6, 8):
        for z in (-21, -14, 9, 16):
            if math.hypot(x + 3 - cx, z + 1) < 13:
                continue
            if z > 0 and x + 7 >= xb - 27:      # daar staat de Soepkeuken
                continue
            plekken.append((x, z))
    for i, (x, z) in enumerate(plekken):
        f = SOORTEN[(i * 7 + rng.randint(3)) % len(SOORTEN)]
        f(W, x, y0 + 1, z, rng)
    # parasols en bankjes langs de laan
    for x in range(xa + 6, xb - 2, 12):
        for z in (-7, 7):
            if abs(x - cx) > 12:
                parasol(W, x, y0 + 1, z, rng, h=3, r=2)
                BH.zet(W, x + 2, y0 + 1, z, trap("wit_trap", "north" if z > 0 else "south"))
    # lampionslingers in zigzag over de hele markt
    kl = ["guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"] if palet.MODUS != "vanilla" else \
        ["minecraft:pearlescent_froglight"]
    for x in range(xa + 4, xb, 9):
        for z in (-24, 24):
            BH.doos(W, x, y0 + 1, z, x, y0 + 11, z, "minecraft:pale_oak_fence")
            BH.zet(W, x, y0 + 12, z, "goud")
        for z in range(-23, 24):
            yy = y0 + 11 - int(4 * (1 - (z / 24.0) ** 2))
            xx = x + int(2 * math.sin(z / 5.0))
            BH.zet(W, xx, yy, z, kl[(z // 3) % len(kl)] if z % 3 == 0 else "minecraft:iron_chain[axis=z,waterlogged=false]")
    # bloembakken en bomen aan de randen
    for x in range(xa, xb + 1, 7):
        for z in (-26, 26):
            BH.zet(W, x, y0 + 1, z, "minecraft:flowering_azalea_leaves")
            BH.zet(W, x + 1, y0 + 1, z, "minecraft:azalea_leaves")
    soepkeuken(W, xb - 26, 6, xb - 2, 24, y0)
    W.label(cx, y0 + 18, 0, "de Marktlaan + fontein van de koningin")
    W.markt = dict(box=(xa, xb), y0=y0, fontein=cx)


def soepkeuken(W, xa, za, xb, zb, y0):
    """De Grote Soepkeuken: open hal met een vakwerkgevel, reuzenketel met stoom, lange tafels en een rij vluchtelingen."""
    wand = "guhs:pastelbaksteen_perzik" if palet.MODUS != "vanilla" else "minecraft:orange_terracotta"
    BH.doos(W, xa, y0, za, xb, y0, zb, "minecraft:spruce_planks")
    BH.doos(W, xa, y0 + 1, zb, xb, y0 + 12, zb, wand)
    BH.doos(W, xa, y0 + 1, za, xa, y0 + 12, zb, wand)
    BH.doos(W, xb, y0 + 1, za, xb, y0 + 12, zb, wand)
    # vakwerk op de zijgevels
    for z in range(za, zb + 1, 3):
        for x in (xa, xb):
            BH.doos(W, x, y0 + 1, z, x, y0 + 12, z, "minecraft:stripped_cherry_log[axis=y]")
    for x in range(xa, xb + 1, 4):
        BH.doos(W, x, y0 + 1, za, x, y0 + 11, za, "minecraft:stripped_cherry_log[axis=y]")
        if x + 2 <= xb:
            BH.zet(W, x + 2, y0 + 10, za, "minecraft:stripped_cherry_log[axis=y]")
    BH.doos(W, xa, y0 + 12, za, xb, y0 + 12, za, "minecraft:stripped_cherry_log[axis=x]")
    BH.zadeldak(W, xa, za, xb, zb, y0 + 13, nok="x", dak="dak_roze", overstek=1, gevel=wand)
    # dakkapellen met ronde raampjes en een dakruiter met bel
    for x in range(xa + 4, xb - 2, 8):
        BH.doos(W, x - 1, y0 + 14, za + 1, x + 1, y0 + 16, za + 2, wand)
        BH.zet(W, x, y0 + 15, za + 1, P["raam"])
        BH.zadeldak(W, x - 1, za + 1, x + 1, za + 3, y0 + 17, nok="z", dak="dak_roze", overstek=0)
    mx = (xa + xb) // 2
    my = y0 + 13 + (zb - za + 3) // 2
    BH.doos(W, mx - 1, my, (za + zb) // 2 - 1, mx + 1, my + 4, (za + zb) // 2 + 1, "wit_glad")
    BH.zet(W, mx, my + 2, (za + zb) // 2, "minecraft:bell[attachment=ceiling,facing=north,powered=false]")
    BH.spits(W, mx, (za + zb) // 2, my + 5, 2, 5, dak="dak_goud", hoeken=4, rand=None)
    # luifel met franje boven de opening
    for x in range(xa - 1, xb + 2):
        BH.zet(W, x, y0 + 9, za - 1, "minecraft:pink_wool" if x % 2 else "minecraft:white_wool")
        BH.zet(W, x, y0 + 8, za - 2, "minecraft:pink_carpet" if x % 2 else "minecraft:white_carpet")
    # bord met een soepkom (pixel)
    BH.doos(W, mx - 4, y0 + 10, za - 1, mx + 4, y0 + 11, za - 1, "minecraft:stripped_cherry_wood[axis=x]")
    BH.zet(W, mx, y0 + 11, za - 2, "minecraft:orange_concrete")
    # reuzenketel achterin (koper met gouden rand), vuur, stoomwolk
    kx, kz = mx, zb - 5
    for dy in range(0, 5):
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                dd = math.hypot(dx, dz)
                r = 3.5 + (0.7 if dy in (2, 3) else 0)
                if r - 1 < dd <= r:
                    BH.zet(W, kx + dx, y0 + 1 + dy, kz + dz, "minecraft:waxed_cut_copper" if dy < 4 else "goud")
                elif dd <= r - 1 and dy == 3:
                    BH.zet(W, kx + dx, y0 + 1 + dy, kz + dz, "minecraft:orange_concrete_powder")
    for dx in (-2, 2):
        BH.zet(W, kx + dx, y0 + 1, kz - 4, "minecraft:campfire[facing=north,lit=true,signal_fire=false,"
                                         "waterlogged=false]")
    for k in range(4):
        W.bol((kx + k * 0.6, y0 + 8 + k * 2.5, kz - k * 0.8), 1.4 + k * 0.6, "guhs:wolkenblok_wit")
    # lange tafels met banken en kommen (bloempotten als kom), kruidenrekjes aan de muur
    for z in (za + 4, za + 8):
        BH.doos(W, xa + 2, y0 + 1, z, xb - 2, y0 + 1, z, "minecraft:stripped_cherry_wood[axis=x]")
        for zz in (z - 1, z + 1):
            for x in range(xa + 2, xb - 1):
                BH.zet(W, x, y0 + 1, zz, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
        for x in range(xa + 3, xb - 2, 2):
            BH.zet(W, x, y0 + 2, z, "minecraft:flower_pot")
    for x in range(xa + 2, xb - 1, 3):
        BH.zet(W, x, y0 + 6, zb - 1, "minecraft:flowering_azalea_leaves" if x % 2 else "minecraft:hay_block[axis=x]")
    W.soep = dict(box=(xa, za, xb, zb), y0=y0, ketel=(kx, kz))
    W.label((xa + xb) / 2, y0 + 24, (za + zb) / 2, "de Grote Soepkeuken")


def markt_figuren(W):
    m = W.markt
    xa, xb = m["box"]
    y0 = m["y0"]
    cx = m["fontein"]
    FG.menigte(W, xa, -24, xb - 28, 24, y0 + 1, 150, seed=31, vrij=lambda x, z: math.hypot(x - cx, z) > 8)
    s = W.soep
    xa_, za, xb_, zb = s["box"]
    for z in (za + 3, za + 5, za + 7, za + 9):
        for x in range(xa_ + 3, xb_ - 2, 2):
            if (x + z) % 3:
                W.wguh(x + 0.5, y0 + 1, z + 0.5, 0 if z in (za + 3, za + 7) else 180,
                       skin=["guh_mager", "guh_hongerig_3", "guh_hongerig_5", "guh_mager"][(x + z) % 4])
    for k in range(16):
        W.wguh(xa_ - 2 - k * 1.3, y0 + 1, za - 2.5 + 0.4 * math.sin(k), 90,
               skin=["guh_mager", "guh_hongerig_2", "guh_hongerig_6"][k % 3])
    kx, kz = s["ketel"]
    BH.npc(W, "guh_npc_bakkerguh", kx - 5.5, y0 + 1, kz - 0.5, yaw=180)
    BH.npc(W, "guh_npc_omaguh", kx + 5.5, y0 + 1, kz - 1.5, yaw=180)
    BH.npc(W, "guh_npc_luchtnon_guh", xa_ + 4.5, y0 + 1, za - 1.5, yaw=270)
