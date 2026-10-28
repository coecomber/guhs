"""
1.8.0 Guhdromenland, ronde U2 (design: MineScapeStory/ontwerp-guhs180-dromen.md; Java: feature/slaapkleding): sleepy clothes.

  slaapmasker        EYES   racebril bones      lavender mask, embroidered closed guh eyes, little stars   droomwinkel
  nachtjaponnetje    BODY   suit bones          a long nightie with little moons and a lace hem             droomwinkel
  maanmutsje         HEAD   slaapmutsje bones   night blue cap with a moon, the pompom is a star            droomwinkel
  oorwolkjes         OREN   ear warmer bones    two fluffy clouds round the guh ears                        droomwinkel
  blub_sloffen       FEET   voet_laag + flap    Blub slippers: light blue, the three stripes, flat ears     droomwinkel
  knuffeltje         BACK   backpack bones      a plush Blub riding on the back                             Snoes' thank-you

Every bone of a piece samples ONE 8x8 swatch of the 128x128 clothes uv (all faces); the textures are 512x512, so a
swatch is 32x32 pixels at (u*4, v*4). This module reads the swatches of each piece's bones from guh.geo.json and paints
them on an empty texture (nothing else of the texture is drawn).

The player's droompyjama (droompyjama_muts/_jasje/_broek/_sloffen): the vanilla leather armour layout (from the 26.1.2
client jar), recoloured lavender with stars and moons; equipment asset guhs:droompyjama; recipes from wool, droomzand and
maanbloemetjes.

From the project root:  PYTHONPATH=tools python tools/features/slaapkleding.py
English: tools/lang/en/c216_slaapkleding.json (the FTB quests: c216_slaapkleding_ftb.json, the quests are in snoesverhaal.py).
"""
import glob
import io
import json
import os
import zipfile

import numpy as np
from PIL import Image

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
EN_OVERLAY = os.path.join("tools", "lang", "en", "c216_slaapkleding.json")

KLEDING = {  # id -> (bone prefixes, Dutch name, English name)
    "slaapmasker": (["outfit_racebril"], "Slaapmasker", "Sleep Mask"),
    "nachtjaponnetje": (["outfit_suit"], "Nachtjaponnetje", "Little Nightie"),
    "maanmutsje": (["outfit_slaapmutsje"], "Maanmutsje", "Moon Nightcap"),
    "oorwolkjes": (["outfit_oren_warmer"], "Oorwolkjes", "Ear Clouds"),
    "blub_sloffen": (["outfit_voet_laag", "outfit_voet_flap"], "Blub-sloffen", "Blub Slippers"),
    "knuffeltje": (["outfit_backpack"], "Knuffeltje", "Little Plushie"),
}
PYJAMA = {"muts": ("Droompyjama-muts", "Dream Pyjama Cap"), "jasje": ("Droompyjama-jasje", "Dream Pyjama Top"),
          "broek": ("Droompyjama-broek", "Dream Pyjama Bottoms"), "sloffen": ("Droompyjama-sloffen", "Dream Pyjama Slippers")}

LAVENDEL, LAVENDEL_D = (196, 170, 236), (150, 120, 200)
PAARS_D, GEEL, GEEL_L, WIT = (70, 44, 104), (255, 222, 110), (255, 244, 190), (250, 248, 252)
NACHT, NACHT_L = (44, 52, 112), (70, 80, 150)
BLUB, BLUB_L, BLUB_STREEP, ROZE = (138, 201, 247), (190, 228, 252), (112, 67, 80), (250, 170, 205)
rng = np.random.default_rng(1802)

TEKSTEN = {}  # key -> (nl, en)


def t(key, nl, en):
    TEKSTEN[key] = (nl, en)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# =====================================================================================================================
# swatches (32 x 32 RGBA)
# =====================================================================================================================
def stof(kleur, ruis=8):
    a = np.zeros((32, 32, 4), np.int32)
    a[..., :3] = np.array(kleur) + rng.integers(-ruis, ruis + 1, (32, 32, 1))
    a[..., 3] = 255
    return a


def zet(a, pts, kleur):
    for x, y in pts:
        if 0 <= x < 32 and 0 <= y < 32:
            a[y, x, :3] = kleur
            a[y, x, 3] = 255


def ster(a, cx, cy, kleur=GEEL):
    zet(a, [(cx, cy), (cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)], kleur)


def maantje(a, cx, cy, kleur=GEEL):
    """A little crescent (5 x 5)."""
    for y in range(-2, 3):
        for x in range(-2, 3):
            if x * x + y * y <= 5 and (x - 1) ** 2 + (y + 1) ** 2 > 3:
                zet(a, [(cx + x, cy + y)], kleur)


def sw_masker():
    a = stof(LAVENDEL, 6)
    a[0:2, :, :3] = LAVENDEL_D
    a[30:32, :, :3] = LAVENDEL_D
    # an embroidered closed guh eye: a downward arc with three little lashes
    for x in range(7, 25):
        y = int(round(15 + 4 * (1 - ((x - 16) / 9.0) ** 2)))
        zet(a, [(x, y), (x, y + 1)], PAARS_D)
    for x in (10, 16, 22):
        zet(a, [(x, 21), (x, 22), (x + (1 if x > 16 else -1 if x < 16 else 0), 23)], PAARS_D)
    zet(a, [(6, 22), (7, 23), (25, 22), (24, 23)], ROZE)            # little blushes
    for cx, cy in ((5, 6), (26, 5), (16, 6), (27, 26), (4, 27)):
        ster(a, cx, cy)
    return a


def sw_masker_band():
    a = stof(LAVENDEL_D, 5)
    for x in range(2, 32, 6):
        zet(a, [(x, 15), (x, 16)], GEEL_L)
    return a


def sw_japon():
    a = stof((226, 200, 244), 6)
    for cx, cy in ((6, 6), (20, 4), (13, 13), (27, 12), (5, 18), (22, 20)):
        maantje(a, cx, cy, GEEL)
    for x in range(32):                                              # the lace hem: scallops and little holes
        for y in range(26, 32):
            if (y - 26) < 3 + abs(((x % 6) - 3)) * 0.6:
                a[y, x, :3] = WIT
        if x % 3 == 1:
            zet(a, [(x, 28)], (226, 214, 236))
    return a


def sw_mutsje():
    a = stof(NACHT, 6)
    for cx, cy in ((4, 4), (27, 6), (9, 25), (24, 27), (15, 3)):
        zet(a, [(cx, cy)], GEEL_L)
    for y in range(-6, 7):                                           # a big crescent moon in the middle
        for x in range(-6, 7):
            if x * x + y * y <= 36 and (x - 3) ** 2 + (y + 2) ** 2 > 22:
                zet(a, [(16 + x, 16 + y)], GEEL)
    a[29:32, :, :3] = NACHT_L
    return a


def sw_ster():
    a = stof(GEEL, 6)
    for y in range(32):
        for x in range(32):
            if abs(x - 16) + abs(y - 16) < 7:
                a[y, x, :3] = GEEL_L
    return a


def sw_wolkje():
    a = stof(WIT, 4)
    for _ in range(26):                                              # soft blue shadows in the fluff
        x, y = rng.integers(0, 32), rng.integers(16, 32)
        a[y:y + 2, x:x + 3, :3] = (214, 228, 248)
    for _ in range(18):
        x, y = rng.integers(0, 32), rng.integers(0, 14)
        a[y:y + 2, x:x + 2, :3] = (255, 255, 255)
    return a


def sw_blub(gezicht=True):
    a = stof(BLUB, 5)
    a[0:3, :, :3] = BLUB_L
    a[:, 0:2, :3] = BLUB_L
    if gezicht:                                                      # the Blub face: two eyes and a mouth (three stripes)
        a[12:16, 7:12, :3] = BLUB_STREEP
        a[12:16, 20:25, :3] = BLUB_STREEP
        a[19:22, 13:19, :3] = BLUB_STREEP
        a[18:20, 5:8, :3] = ROZE
        a[18:20, 24:27, :3] = ROZE
    return a


def sw_blub_oor():
    a = stof(BLUB, 5)
    a[8:24, 8:24, :3] = ROZE
    return a


SCHILDER = {  # bone name prefix -> swatch painter (the most specific prefix wins)
    "outfit_racebril_band": sw_masker_band, "outfit_racebril": sw_masker,
    "outfit_suit": sw_japon,
    "outfit_slaapmutsje_pompon": sw_ster, "outfit_slaapmutsje": sw_mutsje,
    "outfit_oren_warmer": sw_wolkje,
    "outfit_voet_flap": sw_blub_oor, "outfit_voet_laag": lambda: sw_blub(False),
    "outfit_backpack": sw_blub,
}


def kleding_textures():
    geo = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    bones = geo["minecraft:geometry"][0]["bones"]
    for naam, (prefixen, _, _) in KLEDING.items():
        img = np.zeros((512, 512, 4), np.int32)
        for b in bones:
            if not any(b["name"].startswith(p) for p in prefixen):
                continue
            schilder = SCHILDER[max((k for k in SCHILDER if b["name"].startswith(k)), key=len)]
            for c in b.get("cubes", []):
                uv = c["uv"]["north"]["uv"] if isinstance(c.get("uv"), dict) else c["uv"]
                u, v = int(uv[0]) * 4, int(uv[1]) * 4
                if img[v:v + 32, u:u + 32, 3].max() == 0:
                    img[v:v + 32, u:u + 32] = schilder()
        p = os.path.join(A, "textures", "entity", "guh_clothes", f"{naam}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(p)


# =====================================================================================================================
# item icons (16 x 16)
# =====================================================================================================================
ICONEN = {
    "slaapmasker": ["................", "................", "................", "..bbbbbbbbbbbb..", ".blllllllllllllb", "bllllllllllllllb",
                    "blpllllllllllplb", "bllppplllppplllb", "blllllllllllllll", ".bllllyllllyllb.", "..bbbbbbbbbbbb..", "................"],
    "nachtjaponnetje": ["................", ".....wwwwww.....", "....wjjjjjjw....", "...jjjjjjjjjj...", "...jjyjjjjjjj...",
                        "...jjjjjjjyjj...", "..jjjjjjjjjjjj..", "..jjjyjjjjjjjj..", "..jjjjjjjjjjjj..", ".jjjjjjjjjyjjjj.",
                        ".wwwwwwwwwwwwww.", "..w.w.w.w.w.w.w."],
    "maanmutsje": ["............yy..", "...........yYYy.", "..........nnyy..", ".........nnn....", "........nnnn....",
                   ".......nnynn....", "......nnnnnnn...", ".....nnyyynnnn..", "....nnnnyynnnnn.", "...nnnnnnnnnnnn.",
                   "..NNNNNNNNNNNNNN", "..NNNNNNNNNNNNNN"],
    "oorwolkjes": ["................", "................", "..ww........ww..", ".wwww......wwww.", "wwwsww....wwswww", "wwwwww....wwwwww",
                   ".wsww......wwsw.", "..ww........ww..", "...p........p...", "....pppppppp....", "................"],
    "blub_sloffen": ["................", "..rb.........rb.", ".bbbb.......bbbb", ".bbbb.......bbbb", "bbbbbb.....bbbbbb", "bdbdbb.....bdbdbb",
                     "bbddbb.....bbddbb", "bbbbbb.....bbbbbb", ".LLLL.......LLLL.", "................"],
    "knuffeltje": ["................", "....bb....bb....", "....bb....bb....", "...bbbbbbbbbb...", "...bLLLLLLLLb...", "...bbbbbbbbbb...",
                   "...bddbbbbddb...", "...bbbbbbbbbb...", "...bbbbddbbbb...", "...bbbbbbbbbb...", "...bb......bb...", "................"],
}
ICOON_KLEUR = {"b": BLUB, "l": LAVENDEL, "p": PAARS_D, "y": GEEL, "Y": GEEL_L, "w": WIT, "j": (226, 200, 244), "n": NACHT,
               "N": NACHT_L, "s": (214, 228, 248), "d": BLUB_STREEP, "L": BLUB_L, "r": ROZE}


def iconen():
    for naam, rijen in ICONEN.items():
        ic = np.zeros((16, 16, 4), np.uint8)
        top = (16 - len(rijen)) // 2
        for y, rij in enumerate(rijen):
            for x, ch in enumerate(rij[:16]):
                if ch in ICOON_KLEUR:
                    ic[top + y, x, :3] = ICOON_KLEUR[ch]
                    ic[top + y, x, 3] = 255
        if naam == "slaapmasker":                                     # the band blue-lavender
            ic[ic[..., 0] == LAVENDEL_D[0]] = list(LAVENDEL_D) + [255]
        Image.fromarray(ic).save(os.path.join(A, "textures", "item", f"{naam}.png"))
        item_model(naam)


def item_model(naam):
    w(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{naam}"}})
    w(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


# =====================================================================================================================
# the droompyjama (player armour)
# =====================================================================================================================
def _client_jar():
    kandidaten = sorted(glob.glob(os.path.join("build", "moddev", "artifacts", "minecraft-patched-*.jar"))) + glob.glob(
        os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "libraries", "com", "mojang", "minecraft", "*", "*-client.jar"))
    for j in kandidaten:
        try:
            z = zipfile.ZipFile(j)
            if "assets/minecraft/textures/entity/equipment/humanoid/leather.png" in z.namelist():
                return z
        except (OSError, zipfile.BadZipFile):
            pass
    return None


def _lavendel(img, sterren=True, maantjes=False):
    """A grey leather texture -> lavender pyjama fabric with little stars (and moons)."""
    a = np.asarray(img.convert("RGBA")).astype(np.float32)
    grijs = a[..., :3].mean(axis=2, keepdims=True) / 255.0
    kleur = np.array(LAVENDEL, np.float32) * (0.55 + 0.6 * grijs)
    a[..., :3] = np.clip(kleur, 0, 255)
    h, wd = a.shape[:2]
    if sterren:
        r = np.random.default_rng(18)
        for _ in range(h * wd // 22):
            x, y = r.integers(0, wd), r.integers(0, h)
            if a[y, x, 3] > 0:
                a[y, x, :3] = GEEL_L if r.random() < 0.7 else WIT
    if maantjes:
        r = np.random.default_rng(19)
        for _ in range(h * wd // 120):
            x, y = r.integers(1, wd - 1), r.integers(1, h - 1)
            if a[y, x, 3] > 0 and a[y + 1, x, 3] > 0:
                a[y, x, :3] = GEEL
                a[y + 1, x - 1, :3] = GEEL
    return Image.fromarray(a.astype(np.uint8))


def pyjama():
    z = _client_jar()
    if z is None:
        print("slaapkleding: no 26.1.2 client jar found, the droompyjama textures are left as they are")
    else:
        for laag in ("humanoid", "humanoid_leggings"):
            src = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/leather.png")))
            p = os.path.join(A, "textures", "entity", "equipment", laag, "droompyjama.png")
            os.makedirs(os.path.dirname(p), exist_ok=True)
            _lavendel(src, maantjes=(laag == "humanoid")).save(p)
        for stuk, vanilla in (("muts", "leather_helmet"), ("jasje", "leather_chestplate"), ("broek", "leather_leggings"),
                              ("sloffen", "leather_boots")):
            src = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/item/{vanilla}.png")))
            _lavendel(src).save(os.path.join(A, "textures", "item", f"droompyjama_{stuk}.png"))
    w(os.path.join(A, "equipment", "droompyjama.json"),
      {"layers": {"humanoid": [{"texture": "guhs:droompyjama"}], "humanoid_leggings": [{"texture": "guhs:droompyjama"}]}})
    for stuk in PYJAMA:
        item_model(f"droompyjama_{stuk}")
    recepten = {
        "muts": (["WWW", "WDW"], {"W": "#minecraft:wool", "D": "guhs:droomzand"}),
        "jasje": (["WDW", "WMW", "WWW"], {"W": "#minecraft:wool", "D": "guhs:droomzand", "M": "guhs:maanbloemetje"}),
        "broek": (["WMW", "WDW", "W W"], {"W": "#minecraft:wool", "D": "guhs:droomzand", "M": "guhs:maanbloemetje"}),
        "sloffen": (["M M", "WDW"], {"W": "#minecraft:wool", "D": "guhs:droomzand", "M": "guhs:maanbloemetje"}),
    }
    for stuk, (patroon, sleutel) in recepten.items():
        w(os.path.join(D, "recipe", f"droompyjama_{stuk}.json"),
          {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": patroon, "key": sleutel,
           "result": {"id": f"guhs:droompyjama_{stuk}", "count": 1}})


# =====================================================================================================================
# texts
# =====================================================================================================================
def teksten():
    for naam, (_, nl, en) in KLEDING.items():
        t(f"item.guhs.{naam}", nl, en)
    for stuk, (nl, en) in PYJAMA.items():
        t(f"item.guhs.droompyjama_{stuk}", nl, en)
    t("gui.guhs.kledingbron.droomwinkel", "Het droomwinkeltje (Droomplein)", "The dream shop (Dream Square)")
    t("gui.guhs.kledingbron.snoesverhaal", "De droom van Snoes", "Snoes' dream")
    t("gui.guhs.kleding.prijs.munt.droomzand", "%s droomzand", "%s dream sand")
    t("gui.guhs.kleding.prijs.hint.snoes_bedankt", "Snoes' bedankje, na zijn droom", "Snoes' thank-you, after his dream")
    t("gui.guhs.snoes.knuffeltje", "Snoes geeft je een klein knuffel-Blubje: \"Voor op de rug van je guh. Dan slaapt die ook nooit "
      "meer alleen. Njeg.\" (Maak het op en trek het je guh aan!)",
      "Snoes gives you a little plush Blub: \"For your guh's back. Then they never sleep alone either. Njeg.\" (Use it up and dress "
      "your guh in it!)")
    t("item.guhs.droompyjama.lore", "Zacht, warm en vol sterretjes. Beschermt tegen niks, behalve nachtmerries.",
      "Soft, warm and full of little stars. Protects against nothing, except nightmares.")
    t("item.guhs.droompyjama.set", "Droompyjama: %s/4 stukjes aan", "Dream pyjamas: %s/4 pieces on")
    t("item.guhs.droompyjama.bonus.1", "Hele set: je valt sneller in slaap (snellere droom-fade)",
      "Whole set: you fall asleep faster (quicker dream fade)")
    t("item.guhs.droompyjama.bonus.2", "Hele set: vallende droomsterretjes geven vaker extra droomzand",
      "Whole set: falling dream stars give extra dream sand more often")
    t("item.guhs.droompyjama.bonus.3", "Hele set: in bed vraagt je droommaatje sneller of jullie gaan dromen",
      "Whole set: in bed your dream buddy asks sooner whether you're going to dream")


def opslaan():
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        d = json.load(open(p, encoding="utf-8"))
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        w(p, d)
    w(EN_OVERLAY, {k: en for k, (nl, en) in sorted(TEKSTEN.items())})


def build(h=None):
    kleding_textures()
    iconen()
    pyjama()
    teksten()
    if h is not None and hasattr(h, "lang"):          # (a full make_resources run: the texts through its lang hook)
        for k, (nl, en) in TEKSTEN.items():
            h.lang(k, en, nl)


if __name__ == "__main__":
    build()
    opslaan()
    print(f"slaapkleding: {len(KLEDING)} guh clothes, droompyjama (4), {len(TEKSTEN)} texts")
