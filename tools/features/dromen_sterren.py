"""1.8.0 Guhdromenland, eiland "sterren": het Sterrenkussenveld (BOUWPLAN_180.md deel E, ontwerp r03_sterrenkussenveld).

Maakt (standalone, vanuit de guhs-map: PYTHONPATH=tools python tools/features/dromen_sterren.py):
  - de stuiterkussens (guhs:stuiterkussen_<kleur>, 8 kleuren; textuur = het knuffelblok van die kleur met een knoopje);
  - de drie uiterlijkjes van de vallende ster (items droomsterretje, _gloed, _vaag: alleen voor de renderer);
  - de eiland-template data/guhs/structure/dromen/eiland_sterren.nbt + data/guhs/dromen/eiland/sterren.json
    (formaat: DroomEilanden; extra sleutel "ster" = het midden van de grote ster, relatief aan het midden);
  - teksten (nl_nl + en_us, Engels ook in tools/lang/en/c212_sterren.json), recept en loot van de stuiterkussens.
Het eiland volgt de render r03: quilt-veld, stuiterspiraal van reuzenkussens naar de grote ster van Snoes, dekbedheuvel,
kussengevecht-kringetje (met echte kussengevecht-guhs), sterrenhemeltent, sterrenkijker, droomsteiger aan de zuidkant.
"""
import json, math, os, sys

import numpy as np
from PIL import Image

sys.path.insert(0, "tools")
import make_structures as ms                                   # noqa: E402

KLEUREN = ["boter", "vanille", "roze", "hemelsblauw", "lavendel", "perzik", "aqua", "wolkje"]
STER_LOOKS = ["droomsterretje", "droomsterretje_gloed", "droomsterretje_vaag"]
VERSIE = 2   # 2 = playtest fixes: opstapkussen at the first pillow, no kussengevecht-guh inside a pillow

TEKSTEN = {
    "entity.guhs.droomsterretje": ("Vallend sterretje", "Falling little star"),
    "item.guhs.droomsterretje": ("Vallend sterretje", "Falling little star"),
    "item.guhs.droomsterretje_gloed": ("Gloeiend sterretje", "Glowing little star"),
    "item.guhs.droomsterretje_vaag": ("Vervagend sterretje", "Fading little star"),
    "gui.guhs.sterren.gevangen": ("Gevangen! Een sterretje vol droomzand, vahoeg!", "Caught! A little star full of dream sand, vahoeg!"),
    "gui.guhs.sterren.weg": ("Njeg... dat sterretje is al vervaagd.", "Njeg... that little star already faded."),
    "gui.guhs.sterren.grote_ster.titel": ("De ster van Snoes!", "Snoes's star!"),
    "gui.guhs.sterren.grote_ster.sub": ("Een wens: een eigen plekje.", "A wish: a little place of his own."),
    "gui.guhs.sterren.grote_ster.chat": ("Je vangt de grote vallende ster. Hij fluistert een wens: 'een eigen plekje om te slapen'. Dat is een stukje van Snoes' droom!",
                                         "You catch the big falling star. It whispers a wish: 'a little place of my own to sleep'. That is a piece of Snoes's dream!"),
    "gui.guhs.sterren.grote_ster.al": ("De ster glinstert naar je. Snoes' wens heb je al, vads!", "The star twinkles at you. You already have Snoes's wish, vads!"),
    "sign.guhs.sterren.welkom1": ("Welkom op het", "Welcome to the"),
    "sign.guhs.sterren.welkom2": ("Sterrenkussenveld!", "Star Pillow Field!"),
    "sign.guhs.sterren.welkom3": ("Stuiteren op eigen", "Bounce at your own"),
    "sign.guhs.sterren.welkom4": ("vads-risico", "vads risk"),
    "sign.guhs.sterren.spiraal1": ("Stuiterkussens:", "Bouncy pillows:"),
    "sign.guhs.sterren.spiraal2": ("hoger = vahoeger!", "higher = vahoeger!"),
    "sign.guhs.sterren.spiraal3": ("Sluipen = blijven", "Sneak = stay put"),
    "sign.guhs.sterren.spiraal4": ("staan, njeg", "standing, njeg"),
    "sign.guhs.sterren.gevecht1": ("Kussengevecht!", "Pillow fight!"),
    "sign.guhs.sterren.gevecht2": ("Alleen zachte", "Only soft"),
    "sign.guhs.sterren.gevecht3": ("vads, njeg!", "vads, njeg!"),
    "sign.guhs.sterren.tent1": ("Sst! Sterrentent", "Shh! Star tent"),
    "sign.guhs.sterren.tent2": ("Hier dromen guhs", "Here guhs dream"),
    "sign.guhs.sterren.tent3": ("over nog meer", "about even more"),
    "sign.guhs.sterren.tent4": ("dromen", "dreams"),
    "sign.guhs.sterren.kijker1": ("Sterrenkijker", "Star gazer"),
    "sign.guhs.sterren.kijker2": ("Wens iets", "Wish for something"),
    "sign.guhs.sterren.kijker3": ("vahoegs!", "vahoeg!"),
    "sign.guhs.sterren.dekbed1": ("Dekbedheuvel", "Duvet hill"),
    "sign.guhs.sterren.dekbed2": ("Even liggen", "Lie down a bit"),
    "sign.guhs.sterren.dekbed3": ("mag altijd", "always allowed"),
    "sign.guhs.sterren.vang1": ("Vallende sterretjes?", "Falling little stars?"),
    "sign.guhs.sterren.vang2": ("Vang ze voor ze", "Catch them before"),
    "sign.guhs.sterren.vang3": ("vervagen: droomzand!", "they fade: dream sand!"),
}
for k in KLEUREN:
    TEKSTEN[f"block.guhs.stuiterkussen_{k}"] = (f"Stuiterkussen ({k})", f"Bouncy pillow ({ {'boter': 'butter', 'vanille': 'vanilla', 'roze': 'pink', 'hemelsblauw': 'sky blue', 'lavendel': 'lavender', 'perzik': 'peach', 'aqua': 'aqua', 'wolkje': 'cloud'}[k] })")
    TEKSTEN[f"block.guhs.stuiterkussen_{k}.lore"] = ("Boing! Spring erop en je stuitert hoog de lucht in. Sluip om gewoon te blijven staan.",
                                                     "Boing! Jump on it and you bounce high into the air. Sneak to just stand on it.")


# ------------------------------------------------------------------------------------------------------- textures
def kussen_textuur(h, kleur):
    src = os.path.join(h.TEX, "block", "bouwhulp", f"knuffelblok_{kleur}.png")
    im = Image.open(src).convert("RGBA").resize((16, 16), Image.NEAREST)
    a = np.array(im, np.int16)
    # zachte rand (iets donkerder) en een knoopje met kruissteek in het midden: een kussen, geen blok
    for i in range(16):
        for j in (0, 15):
            a[i, j, :3] = (a[i, j, :3] * 0.86).astype(np.int16)
            a[j, i, :3] = (a[j, i, :3] * 0.86).astype(np.int16)
    for (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)):
        a[y, x, :3] = (a[y, x, :3] * 0.7).astype(np.int16)
    for (x, y) in ((6, 6), (9, 9), (6, 9), (9, 6)):
        a[y, x, :3] = np.minimum(a[y, x, :3] + 40, 255)
    h.save(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), "block", f"stuiterkussen_{kleur}.png")


def ster_textuur(h, naam, alpha, glans):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 8.0
            a = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            rand = 3.2 + 4.0 * (0.5 + 0.5 * math.cos(5 * (a + math.pi / 2))) ** 3
            if r <= rand:
                rand_px = r > rand - 1.1
                c = (255, 214, 92) if rand_px else (255, 240, 150)
                c = tuple(min(255, int(v + glans)) for v in c)
                px[x, y] = c + (alpha,)
    # gezichtje: oogjes, blosjes, mondje
    for (x, y, c) in ((6, 8, (60, 40, 50)), (9, 8, (60, 40, 50)), (5, 10, (255, 150, 170)), (10, 10, (255, 150, 170)),
                      (7, 10, (120, 70, 80)), (8, 10, (120, 70, 80))):
        px[x, y] = c + (alpha,)
    h.save(im, "item", f"{naam}.png")


# ------------------------------------------------------------------------------------------------------- het eiland
class Eiland:
    """Een kleine voxel-scene (zoals droomkit.Scene) die als structuur-template wordt opgeslagen."""

    def __init__(self, X, Y, Z):
        self.X, self.Y, self.Z = X, Y, Z
        self.b = {}
        self.ent = []

    def set(self, x, y, z, naam, props=None, nbt=None, keep=False):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if not (0 <= x < self.X and 0 <= y < self.Y and 0 <= z < self.Z):
            return
        if keep and (x, y, z) in self.b:
            return
        if ":" not in naam:
            naam = "guhs:" + naam
        self.b[(x, y, z)] = (naam, props or {}, nbt)

    def lucht(self, x, y, z):
        return (int(x), int(y), int(z)) not in self.b

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, naam, hollow=0.0, ymin=None, props=None):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                if ymin is not None and y < ymin:
                    continue
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d > 1:
                        continue
                    if hollow and ((x - cx) / (rx - hollow)) ** 2 + ((y - cy) / (ry - hollow)) ** 2 + ((z - cz) / (rz - hollow)) ** 2 <= 1:
                        continue
                    self.set(x, y, z, naam, props)

    def cylinder(self, cx, cz, r, y1, y2, naam, hollow=0.0):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r and not (hollow and d <= r - hollow):
                    for y in range(y1, y2 + 1):
                        self.set(x, y, z, naam)

    def bord(self, x, y, z, facing, keys, hout="cherry"):
        msgs = [json.dumps({"translate": f"sign.guhs.sterren.{k}"}) for k in keys]
        msgs += [json.dumps("")] * (4 - len(msgs))
        text = {"messages": ms.NbtList(8, msgs), "color": "black", "has_glowing_text": ms.Byte(1)}
        leeg = {"messages": ms.NbtList(8, [json.dumps("")] * 4), "color": "black", "has_glowing_text": ms.Byte(0)}
        rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
        self.set(x, y, z, f"minecraft:{hout}_sign", {"rotation": rot, "waterlogged": "false"},
                 {"id": "minecraft:sign", "is_waxed": ms.Byte(1), "front_text": text, "back_text": leeg})

    def entity(self, x, y, z, nbt):
        self.ent.append((x, y, z, nbt))


def bouw_eiland():
    X, Y, Z = 104, 110, 104
    C = 52
    T = 30
    S = Eiland(X, Y, Z)
    rng = np.random.default_rng(3)

    # --- het eiland: rafelige rand, druppelvormige onderkant (droomkit.eiland), bovenkant knuffelgras
    r0, diepte, fase = 33, 21, np.random.default_rng(33).random(4) * 2 * math.pi

    def rand(a):
        return r0 * (1 + 0.18 * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.25 * math.sin(8 * a + fase[2])))

    def binnen(x, z):
        return math.hypot(x - C, z - C) <= rand(math.atan2(z - C, x - C))

    rr = np.random.default_rng(34)
    for x in range(X):
        for z in range(Z):
            d = math.hypot(x - C, z - C) / rand(math.atan2(z - C, x - C))
            if d > 1:
                continue
            h = int(diepte * (1 - d) ** 0.7 + rr.random() * 1.5) + 1
            for k in range(h + 1):
                b = "knuffelgras" if k == 0 else "pluiswolblok" if k <= 2 else "droomzandblok" if k >= h - 1 and d < 0.3 else "knuffelsteen"
                S.set(x, T - k, z, b)
            if d > 0.82 and rr.random() < 0.12:
                n = int(rr.integers(2, 5))
                for k in range(1, n):
                    S.set(x, T - h - k, z, "wolkenweide_rank", {"tip": "true" if k == n - 1 else "false"}, keep=True)

    # quilt: lapjes van 5x5 boter/vanille met roze steekjes
    for x in range(X):
        for z in range(Z):
            if binnen(x, z):
                if x % 5 == 0 and z % 5 == 0:
                    S.set(x, T, z, "knuffelblok_roze")
                else:
                    S.set(x, T, z, "knuffelblok_boter" if ((x // 5) + (z // 5)) % 2 else "knuffelblok_vanille")

    def kussen(cx, cy, cz, rx, kleur, gezicht="south"):
        """Reuzenkussen van stuiterkussen-blokken (2 dik + ingesprongen bovenlaag), knoop, kwastjes en een gezichtje."""
        cx, cy, cz, rx = int(round(cx)), int(round(cy)), int(round(cz)), int(round(rx))
        blok = f"stuiterkussen_{kleur}"
        for x in range(cx - rx, cx + rx + 1):
            for z in range(cz - rx, cz + rx + 1):
                if not (abs(x - cx) == rx and abs(z - cz) == rx):
                    S.set(x, cy, z, blok)
                    S.set(x, cy - 1, z, blok)
                if abs(x - cx) < rx and abs(z - cz) < rx:
                    S.set(x, cy + 1, z, blok)
        for sx in (-1, 1):
            for sz in (-1, 1):
                S.set(cx + sx * rx, cy, cz + sz * rx, "lampion_geel", {"hanging": "false"})
        dx, dz = {"south": (0, rx), "north": (0, -rx), "east": (rx, 0), "west": (-rx, 0)}[gezicht]
        S.set(cx + dx, cy, cz + dz, "knuffelsteen_gezicht", {"facing": gezicht, "stemming": str(int(rng.integers(0, 4)))})
        return cy + 1

    # --- pad van de steiger (zuid) naar het midden, rond pleintje
    for k in np.arange(0, 34, 0.5):
        for wdt in (-1.5, -0.5, 0.5, 1.5):
            x, z = int(round(C + wdt)), int(round(C + 33 - k))
            if binnen(x, z):
                S.set(x, T, z, "pleister_boter" if abs(wdt) < 1 else "maansteen_tegels")
    for x in range(C - 9, C + 10):
        for z in range(C - 9, C + 10):
            r = math.hypot(x - C, z - C)
            if r <= 8.5:
                S.set(x, T, z, "tegel_hartjes" if r > 7.5 else "pleister_vanille" if (x + z) % 2 else "pleister_boter")
    S.set(C, T, C, "tegel_guhgezicht")

    # droomsteiger + zeepbel aan de zuidkant (richting het plein)
    for k in np.arange(28, 43, 0.5):
        for wdt in (-1, 0, 1):
            x, z = C + wdt, int(round(C + k))
            if not binnen(x, z):
                S.set(x, T, z, "wolkenblok_wit_plaat" if wdt == 0 else "wolkenblok_roze_plaat", {"type": "top", "waterlogged": "false"}, keep=True)
    S.ellipsoid(C, T + 4, C + 46, 2.6, 2.6, 2.6, "zeepbel", hollow=1)
    S.bord(C - 3, T + 1, C + 26, "south", ["welkom1", "welkom2", "welkom3", "welkom4"])
    S.bord(C + 3, T + 1, C + 26, "south", ["vang1", "vang2", "vang3"])
    for z in range(C + 12, C + 30, 5):
        for wdt in (-3, 3):
            if binnen(C + wdt, z):
                S.set(C + wdt, T + 1, z, "guh_lantaarnpaal", {"half": "lower"})
                S.set(C + wdt, T + 2, z, "guh_lantaarnpaal", {"half": "upper"})

    # --- de stuiterspiraal: kussens steeds hoger naar de grote ster
    SPX, SPZ = C, C - 2
    spiraal = []
    for i in range(11):
        a = math.radians(150 + i * 47)
        rr_ = 15 - i * 0.7
        x, z = SPX + rr_ * math.cos(a), SPZ + rr_ * math.sin(a)
        y = T + 3 + i * 3
        top = kussen(x, y, z, 4.4 - i * 0.15, KLEUREN[i % len(KLEUREN)], ["south", "east", "west", "south"][i % 4])
        spiraal.append((int(round(x)), top, int(round(z))))
        if i:
            px, py, pz = spiraal[i - 1]
            for t in (0.35, 0.65):
                S.set(px + (round(x) - px) * t, py + 2 + (top - py) * t, pz + (round(z) - pz) * t, "zeepbel", keep=True)
    S.bord(spiraal[0][0] + 6, T + 1, spiraal[0][2] + 2, "south", ["spiraal1", "spiraal2", "spiraal3", "spiraal4"])
    # (playtest fix) an opstapkussen on the ground next to the first pillow: the first pillow's top was 3-4 blocks up, out of
    # reach in survival; a low bouncy pad bounces you (about 5 blocks) up onto it
    ox, oz = spiraal[0][0] + 7, spiraal[0][2] - 2
    for x in range(ox - 1, ox + 2):
        for z in range(oz - 1, oz + 2):
            S.set(x, T + 1, z, "stuiterkussen_roze")
    S.set(ox + 2, T + 1, oz + 1, "knuffelsteen_gezicht", {"facing": "east", "stemming": "1"})

    # --- de grote ster van Snoes, boven de spiraal (goud + glowstone, guhgezichtje voor en achter)
    SX, SY, SZ = SPX + 1, spiraal[-1][1] + 7, SPZ - 1
    for x in range(-7, 8):
        for y in range(-7, 8):
            a = math.atan2(y, x)
            r = math.hypot(x, y)
            rnd = 3 + 4.2 * (0.5 + 0.5 * math.cos(5 * (a - math.pi / 2))) ** 3
            if r <= rnd:
                b = "minecraft:glowstone" if r > rnd - 1.2 else "minecraft:gold_block"
                S.set(SX + x, SY + y, SZ, b)
                S.set(SX + x, SY + y, SZ + 1, b)
    S.set(SX, SY, SZ - 1, "knuffelsteen_gezicht", {"facing": "north", "stemming": "1"})
    S.set(SX, SY, SZ + 2, "knuffelsteen_gezicht", {"facing": "south", "stemming": "1"})
    for k in range(4, 17):                                     # staart: schuin omhoog naar het noordoosten
        for wdt in range(-2, 3):
            if rng.random() < 0.8 - k * 0.03 - abs(wdt) * 0.15:
                keuze = rng.choice(["glowstone", "zeepbel", "glowstone", "end_rod"])
                naam, props = {"glowstone": ("minecraft:glowstone", None), "zeepbel": ("zeepbel", None),
                               "end_rod": ("minecraft:end_rod", {"facing": "up"})}[keuze]
                S.set(SX + 5 + k * 0.9, SY + 4 + k * 0.75 + wdt * 0.5, SZ - k * 0.5 + wdt, naam, props, keep=True)
    ster = (SX + 0.5, SY + 0.5, SZ + 1.0)

    # --- dekbedheuvel (oosten): quiltpatroon, wolkenbedden aan de rand
    DX, DZ = C + 19, C + 5
    for x in range(DX - 9, DX + 10):
        for z in range(DZ - 7, DZ + 8):
            d = ((x - DX) / 9.5) ** 2 + ((z - DZ) / 7.5) ** 2
            if d <= 1 and binnen(x, z):
                hh = int(3.5 * (1 - d) ** 0.6) + 1
                for y in range(T + 1, T + hh + 1):
                    S.set(x, y, z, ["knuffelblok_roze", "knuffelblok_hemelsblauw", "knuffelblok_lavendel"][((x // 3) + (z // 3)) % 3])
                if x % 3 == 0 and z % 3 == 0:
                    S.set(x, T + hh, z, "knuffelblok_wolkje")
    for z in range(DZ - 5, DZ + 6, 3):
        if binnen(DX + 12, z):
            S.set(DX + 11, T + 1, z, "wolkenbed", {"facing": "west", "part": "head"})
            S.set(DX + 12, T + 1, z, "wolkenbed", {"facing": "west", "part": "foot"})
    S.bord(DX - 9, T + 1, DZ + 8, "south", ["dekbed1", "dekbed2", "dekbed3"])

    # --- kussengevecht-kringetje (westen) met echte kussengevecht-guhs
    KX, KZ = C - 20, C + 8
    S.cylinder(KX, KZ, 7, T, T, "pleister_vanille")
    S.cylinder(KX, KZ, 7, T, T, "tegel_hartjes", hollow=1)
    for a in range(0, 360, 12):
        x, z = KX + 7.6 * math.cos(math.radians(a)), KZ + 7.6 * math.sin(math.radians(a))
        if a % 90 != 0 and binnen(x, z):
            S.set(x, T + 1, z, "guh_hekje_roze", keep=True)
    for (ox, oz, yaw) in ((-2.5, 0, 270.0), (1.0, 0, 90.0), (0, -2.5, 0.0), (0, 2.5, 180.0)):   # (playtest fix: the east one stood in the first pillow)
        S.entity(KX + ox + 0.5, T + 1, KZ + oz + 0.5,
                 {"id": "guhs:guh_npc", "Kind": "kussengevecht_guh", "PersistenceRequired": ms.Byte(1),
                  "Rotation": ms.floats(yaw, 0.0)})
    kleine = ["yellow_kussen", "white_kussen", "pink_kussen", "light_blue_kussen", "purple_kussen", "orange_kussen"]
    for _ in range(10):
        a, r = rng.random() * 2 * math.pi, 3 + rng.random() * 3.5
        S.set(KX + r * math.cos(a), T + 1, KZ + r * math.sin(a), kleine[rng.integers(0, len(kleine))],
              {"facing": str(rng.choice(["north", "east", "south", "west"]))}, keep=True)
    S.bord(KX + 7, T + 1, KZ + 6, "south", ["gevecht1", "gevecht2", "gevecht3"])

    # --- sterrenhemeltent (noordwesten): koepel van sterrenplafond op paaltjes, nestjes
    TX, TZ = C - 16, C - 16
    S.cylinder(TX, TZ, 6.5, T, T, "pleister_lavendel")
    S.cylinder(TX, TZ, 6.5, T, T, "tegel_hartjes", hollow=1)
    S.ellipsoid(TX, T + 4, TZ, 7.5, 4.5, 7.5, "sterrenplafond", hollow=1, ymin=T + 5)
    for a in range(0, 360, 60):
        x, z = TX + 6.5 * math.cos(math.radians(a)), TZ + 6.5 * math.sin(math.radians(a))
        for y in range(T + 1, T + 5):
            S.set(round(x), y, round(z), "pleister_boter")
        S.set(round(x), T + 5, round(z), "lampion_geel", {"hanging": "false"}, keep=True)
    S.set(TX, T + 8, TZ, "minecraft:glowstone")
    for (ox, oz) in ((-3, -2), (0, -4), (3, -2)):
        S.set(TX + ox, T + 1, TZ + oz, "guhnestje")
    S.set(TX + 4, T + 1, TZ + 2, "maanlampje_maan", {"facing": "south", "lit": "true"})
    S.set(TX - 4, T + 1, TZ + 2, "knuffel_starry", {"facing": "east"})
    for a in range(0, 360, 45):
        x, z = TX + 9 * math.cos(math.radians(a)), TZ + 9 * math.sin(math.radians(a))
        if binnen(x, z):
            S.set(x, T + 1, z, "sterrenlantaarn")
    S.bord(TX + 7, T + 1, TZ + 4, "south", ["tent1", "tent2", "tent3", "tent4"])

    # --- sterrenkijker (noordoosten)
    OX, OZ = C + 17, C - 17
    S.cylinder(OX, OZ, 3.5, T + 1, T + 2, "maansteen")
    S.cylinder(OX, OZ, 3.5, T + 3, T + 3, "maansteen_tegels")
    S.set(OX, T + 4, OZ, "guh_telescoop", {"facing": "north"})
    S.bord(OX + 4, T + 1, OZ + 3, "south", ["kijker1", "kijker2", "kijker3"])

    # --- Blub-guhs (in de droom altijd zichtbaar): een paar losse, rustig blijvende Blubs
    for (x, z, soort, meter) in ((C + 4.5, C + 5.5, "blauw", 30), (C - 6.5, C + 20.5, "weide", 45), (C + 9.5, C + 18.5, "schild", 20),
                                 (C - 26.5, C - 4.5, "blauw", 60), (OX - 1.5, OZ + 2.5, "blauw", 10)):
        y = T + 4 if (x, z) == (OX - 1.5, OZ + 2.5) else T + 1
        S.entity(x, y, z, {"id": "guhs:blubguh", "Meter": meter, "BlubSoort": soort, "Tint": 0, "Grootte": ms.Float(1.0),
                           "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(float(rng.integers(0, 360)), 0.0)})

    # --- maanbloemetjes en pluisgras
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and math.hypot(x - C, z - C) > 10 and S.lucht(x, T + 1, z) and not S.lucht(x, T, z) and rng.random() < 0.08:
                if rng.random() < 0.6:
                    S.set(x, T + 1, z, "maanbloemetje", {"open": "true"})
                else:
                    S.set(x, T + 1, z, "pluisgras")

    # --- uitsnijden: alleen de bezette hoogte (de template mag max 80 hoog zijn)
    ys = [p[1] for p in S.b]
    y0 = min(ys)
    hoogte = max(ys) - y0 + 1
    if hoogte > 80:
        y1 = y0 + 79
        S.b = {p: v for p, v in S.b.items() if p[1] <= y1}
        hoogte = 80
    st = ms.Structure((X, hoogte, Z))
    for (x, y, z), (naam, props, nbt) in S.b.items():
        st.blocks[(x, y - y0, z)] = (naam, props, nbt)
    for (x, y, z, nbt) in S.ent:
        st.entity(x, y - y0, z, nbt)
    midden = (C, T - y0, C)
    return st, midden, ster, y0


def build(h):
    A, D = h.A, h.D
    for k in KLEUREN:
        kussen_textuur(h, k)
        h.w(f"{A}/models/block/stuiterkussen_{k}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/stuiterkussen_{k}"}})
        h.w(f"{A}/blockstates/stuiterkussen_{k}.json", {"variants": {"": {"model": f"guhs:block/stuiterkussen_{k}"}}})
        h.w(f"{A}/models/item/stuiterkussen_{k}.json", {"parent": f"guhs:block/stuiterkussen_{k}"})
        h.self_drop(f"stuiterkussen_{k}")
        h.shapeless(f"stuiterkussen_{k}", [f"guhs:knuffelblok_{k}", "minecraft:slime_ball"], f"guhs:stuiterkussen_{k}", 1)
    for naam, alpha, glans in ((STER_LOOKS[0], 255, 0), (STER_LOOKS[1], 235, 25), (STER_LOOKS[2], 110, 40)):
        ster_textuur(h, naam, alpha, glans)
        h.item_model(naam)
    h.add_tag("minecraft/tags/block/mineable/hoe", [f"guhs:stuiterkussen_{k}" for k in KLEUREN])
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)

    st, midden, ster, y0 = bouw_eiland()
    ms.OUT = os.path.join("src", "main", "resources", "data", "guhs", "structure")
    st.save("dromen/eiland_sterren")
    rel = lambda p: [round(p[0] - midden[0], 2), round(p[1] - y0 - midden[1], 2), round(p[2] - midden[2], 2)]
    C, T = midden[0], midden[1]
    opzet = {"versie": VERSIE, "template": "guhs:dromen/eiland_sterren", "midden": list(midden),
             "aankomst": {"plek": [0.5, 1, 22.5], "yaw": 180},
             "steiger": [0.5, 1, 38.5],
             "ster": rel(ster)}
    h.w(f"{D}/dromen/eiland/sterren.json", opzet)
    selfcheck(h, st)


def selfcheck(h, st):
    problems = []
    blokken = {v[0] for v in st.blocks.values()}
    for b in sorted(blokken):
        ns, naam = b.split(":")
        if ns == "guhs" and not os.path.exists(f"{h.A}/blockstates/{naam}.json"):
            problems.append(f"unknown block {b}")
    if st.size[0] > 112 or st.size[1] > 80 or st.size[2] > 112:
        problems.append(f"template too big {st.size}")
    if problems:
        raise SystemExit("dromen_sterren self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c212_sterren.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in [f"stuiterkussen_{k}" for k in KLEUREN] + STER_LOOKS:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"sterren: {len(KLEUREN)} stuiterkussens, vallende ster, eiland, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
