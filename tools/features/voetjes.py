"""
1.7.0 "Onder de Wol", ronde 26 (design: MineScapeStory/ontwerp-guhs170-mijnen.md; Java: feature/voetjes): the FEET slot of
the guh's wardrobe (GuhClothes.Slot.FEET) and its six pieces:

  pluisslofjes          pink fluffy slippers with a pompom              the kleermaker (emeralds)
  regenlaarsjes         yellow rain boots (with the regenjas/-hoed)      the kleermaker (emeralds)
  golfschoentjes        white golf shoes with a pink fringed flap        the Golfguh (golfballetjes; his shop: feature/golf)
  mijnwerkerslaarsjes   brown miner's boots with a steel toe cap         rare in the chests of the Verlaten kaasmijnschacht
  kaassauslopers        cheese-yellow platform shoes: walk on kaassaus   rare in the chests of the Mika-grillpaleis + Spiesburcht
  lavalopers            obsidian platform boots with glowing cracks      the kleermaker's upgrade (+4 magma cream, +1 vahoege vads)

The bones (outfit_voet_<part>_<paw>, one per paw, on the four leg bones so they walk along) are shared by the pieces:
laag (a low shell), hoog (a boot with a cuff), zool (a thin sole), plateau (a thick platform), pompon, neus (a toe cap) and
flap (the golf fringe). They sample three swatches of this module's own pool (voet_schoen, voet_zool, voet_extra); every
piece paints them in its own clothes texture.

make_guh_variants.py picks up BONES and clothes(), make_clothes_icons.py icons(), make_resources build(h). On its own (the
way the 1.5+ slices run, a full make_resources is not safe on this tree), from the project root:

    python tools/features/voetjes.py

It re-adds every feature's bones to guh.geo.json (deterministic: the same result as make_guh_variants), paints only the
six clothes textures and icons of this module, writes the item models + 26.1 item definitions, patches the four loot
tables (idempotent) and merges its texts into nl_nl.json / en_us.json. English: tools/lang/en/c192_voetjes.json.
"""
import json
import os

import numpy as np

CLOTHES = ["pluisslofjes", "regenlaarsjes", "golfschoentjes", "mijnwerkerslaarsjes", "kaassauslopers", "lavalopers"]

# the four paws: (leg bone, its pivot, the paw cube's origin x/z); the paw is 3.5 x 2.5 x 4.5, its toes at the low z
_POTEN = {"al": ("leg_back_left", [6, 1, 9], 4.5, 7), "ar": ("leg_back_right", [-6, 1, 9], -8, 7),
          "vl": ("leg_front_left", [2, 1, 0], 4.25, -2.5), "vr": ("leg_front_right", [-6, 1, 0], -7.75, -2.5)}


def _delen(x, z):
    """The parts on one paw: name -> (swatch, [(origin, size, inflate), ...])."""
    return {
        "laag": ("voet_schoen", [([x, 0, z], [3.5, 1.6, 4.5], 0.45)]),
        "hoog": ("voet_schoen", [([x, 0, z], [3.5, 2.5, 4.5], 0.45), ([x - 0.1, 2.3, z + 0.6], [3.7, 1.3, 3.3], 0.5)]),
        "zool": ("voet_zool", [([x, 0, z], [3.5, 0.5, 4.5], 0.65)]),
        "plateau": ("voet_zool", [([x, 0, z], [3.5, 0.9, 4.5], 0.85)]),
        "pompon": ("voet_extra", [([x + 0.9, 1.6, z - 0.7], [1.7, 1.4, 1.5], 0.15)]),
        "neus": ("voet_extra", [([x, 0, z], [3.5, 1.5, 1.4], 0.6)]),
        "flap": ("voet_extra", [([x + 0.4, 1.9, z - 0.3], [2.7, 0.4, 1.6], 0.2)]),
    }


BONES = {}
for _p, (_poot, _pivot, _x, _z) in _POTEN.items():
    for _deel, (_swatch, _cubes) in _delen(_x, _z).items():
        BONES[f"outfit_voet_{_deel}_{_p}"] = (_poot, _pivot, _swatch, _cubes)

# colours
ROZE, ROZE_LICHT = (250, 150, 200), (255, 206, 230)
GEEL, GEEL_DONKER = (250, 210, 40), (196, 140, 24)          # the regenjas / regenhoed yellow
WIT, GOLF_ROZE, GOLF_DONKER = (246, 244, 246), (246, 110, 172), (150, 50, 105)
LEER, LEER_DONKER, STAAL, ZOOL = (132, 86, 50), (84, 54, 32), (168, 172, 184), (52, 44, 40)
KAAS, KAAS_GAT, KAAS_ZOOL = (252, 206, 70), (214, 160, 40), (236, 128, 36)
OBSIDIAAN, GLOED, GLOED_LICHT = (44, 26, 58), (255, 120, 20), (255, 210, 90)


def clothes(rng, v):
    def pluis(kleur, licht):
        a = v.fabric(kleur, rng, 10)
        px = a.shape[0]
        for _ in range(70):                      # fluffy strands
            x, y = rng.integers(0, px - 1), rng.integers(0, px - 3)
            a[y:y + 3, x] = licht
        return a

    def glans(kleur, donker):
        """Shiny rubber: smooth, a light streak, a dark rim at the bottom."""
        a = v.fabric(kleur, rng, 4)
        a[5:8, :] = np.clip(np.array(kleur, np.float32) + 40, 0, 255)
        a[28:32, :] = donker
        return a

    def golfschoen():
        a = v.fabric(WIT, rng, 4)
        for x in range(2, 32, 6):                # little perforation holes
            a[10:12, x:x + 2] = (210, 206, 212)
        a[28:32, :] = GOLF_ROZE
        return a

    def golfzool():
        a = v.fabric(GOLF_DONKER, rng, 6)
        for x in range(1, 32, 5):                # the spikes
            a[26:30, x:x + 2] = (230, 230, 236)
        return a

    def franje():
        a = v.fabric(GOLF_ROZE, rng, 6)
        for x in range(0, 32, 3):                # the fringe
            a[20:32, x] = GOLF_DONKER
        return a

    def leer():
        a = v.fabric(LEER, rng, 12)
        a[0:4, :] = LEER_DONKER                  # the cuff
        for y in range(8, 24, 5):                # laces
            a[y:y + 1, 12:20] = (232, 214, 170)
        return a

    def staal():
        a = v.fabric(STAAL, rng, 6)
        a[:3, :] = (210, 214, 224)
        return a

    def kaas():
        a = v.fabric(KAAS, rng, 8)
        for _ in range(9):                       # the holes
            x, y, r = rng.integers(3, 29), rng.integers(3, 29), rng.integers(2, 4)
            yy, xx = np.ogrid[:32, :32]
            a[(yy - y) ** 2 + (xx - x) ** 2 <= r * r] = KAAS_GAT
        return a

    def zool(kleur, ruis=8):
        a = v.fabric(kleur, rng, ruis)
        for y in range(4, 32, 6):
            a[y, :] = a[y, :] * 0.8                # the tread
        return a

    def obsidiaan(gloed_onder=False):
        a = v.fabric(OBSIDIAAN, rng, 10)
        x = int(rng.integers(4, 10))
        for y in range(32):                      # a glowing crack zigzagging down
            x = int(np.clip(x + rng.integers(-1, 2), 1, 30))
            a[y, x:x + 2] = GLOED
        x = int(rng.integers(20, 28))
        for y in range(32):
            x = int(np.clip(x + rng.integers(-1, 2), 1, 30))
            a[y, x:x + 1] = GLOED_LICHT
        if gloed_onder:
            a[26:32, :] = GLOED
        return a

    return {
        "pluisslofjes": {"voet_schoen": lambda: pluis(ROZE, ROZE_LICHT), "voet_extra": lambda: pluis(ROZE_LICHT, (255, 236, 246))},
        "regenlaarsjes": {"voet_schoen": lambda: glans(GEEL, GEEL_DONKER), "voet_zool": lambda: zool(GEEL_DONKER, 5)},
        "golfschoentjes": {"voet_schoen": golfschoen, "voet_zool": golfzool, "voet_extra": franje},
        "mijnwerkerslaarsjes": {"voet_schoen": leer, "voet_zool": lambda: zool(ZOOL), "voet_extra": staal},
        "kaassauslopers": {"voet_schoen": kaas, "voet_zool": lambda: zool(KAAS_ZOOL)},
        "lavalopers": {"voet_schoen": obsidiaan, "voet_zool": lambda: obsidiaan(True)},
    }


# --- the item icons (16x16, toes to the left) --------------------------------------------------------------------------
SLOF = ["......pp........", ".....pPPp.......", "...aapPPpaa.....", "..abbbppbbbaa...", ".abbbbbbbbbbba..",
        ".abbbbbbbbbbbba.", ".acccccccccccca.", "..aaaaaaaaaaaa.."]
LAARS = [".......aaaaa....", ".......abbba....", ".......abbba....", ".......abbba....", ".......acbca....",
         "...aaaaabbba....", "..abbbbbbbbba...", ".abbbbbbbbbbba..", ".abbbbbbbbbbba..", ".addddddddddda..",
         "..aaaaaaaaaaa..."]
GOLF = [".....aaaaa......", "....affffa......", "..aaafffffaa....", ".abbbffffbbba...", ".abbbbbbbbbbba..",
        ".abbbbbbbbbbbba.", ".adddddddddddda.", "..s..s..s..s..s."]
MIJN = [".......aaaaa....", ".......abbba....", ".......aebea....", ".......abbba....", "...aaaaaebea....",
        "..annnbbbbbba...", ".annnnbbbbbba...", ".annnnbbbbbbba..", ".addddddddddda..", "..aaaaaaaaaaa..."]
KAASLOPER = ["....aaaaaa......", "..aabbbbbbaa....", ".abbhbbbbhbba...", ".abbbbbhbbbbba..", ".addddddddddda..",
             ".adddddddddddda.", ".adddddddddddda.", "..aaaaaaaaaaaa.."]
LAVALOPER = [".......aaaaa....", ".......abgba....", ".......abbba....", ".......agbba....", "...aaaaabbba....",
             "..abbgbbbbgba...", ".abbbbbgbbbba...", ".addddddddddda..", ".adgdddddgdddda.", ".adddddddddddda.",
             "..aaaaaaaaaaaa.."]


def icons(ic):
    return {
        "pluisslofjes": ic.icon(ic.pad(SLOF), {"a": (190, 80, 140), "b": ROZE, "c": (220, 110, 170), "p": ROZE_LICHT, "P": (255, 240, 248)}),
        "regenlaarsjes": ic.icon(ic.pad(LAARS), {"a": (150, 104, 16), "b": GEEL, "c": (255, 240, 140), "d": GEEL_DONKER}),
        "golfschoentjes": ic.icon(ic.pad(GOLF), {"a": (120, 110, 124), "b": WIT, "f": GOLF_ROZE, "d": GOLF_DONKER, "s": (200, 200, 210)}),
        "mijnwerkerslaarsjes": ic.icon(ic.pad(MIJN), {"a": (54, 34, 20), "b": LEER, "e": (232, 214, 170), "n": STAAL, "d": ZOOL}),
        "kaassauslopers": ic.icon(ic.pad(KAASLOPER), {"a": (150, 100, 20), "b": KAAS, "h": KAAS_GAT, "d": KAAS_ZOOL}),
        "lavalopers": ic.icon(ic.pad(LAVALOPER), {"a": (20, 10, 28), "b": OBSIDIAAN, "g": GLOED, "d": (70, 40, 84)}),
    }


# --- texts ---------------------------------------------------------------------------------------------------------------
TEKSTEN = {   # key: (nl, en)
    "gui.guhs.menu.clothes.feet": ("Voetjes", "Feet"),
    "item.guhs.pluisslofjes": ("Pluisslofjes", "Fluffy Slippers"),
    "item.guhs.regenlaarsjes": ("Guhregenlaarsjes", "Guh Rain Boots"),
    "item.guhs.golfschoentjes": ("Golfschoentjes", "Golf Shoes"),
    "item.guhs.mijnwerkerslaarsjes": ("Mijnwerkerslaarsjes", "Miner's Boots"),
    "item.guhs.kaassauslopers": ("Kaassauslopers", "Cheese Sauce Striders"),
    "item.guhs.lavalopers": ("Lavalopers", "Lava Striders"),
    "item.guhs.kaassauslopers.uitleg": ("Je guh loopt ermee over alle kaassaus, ook de hete kaasfrituursaus. Niet over echte lava, vads!",
                                        "Your guh walks on top of every cheese sauce with these, the hot frying sauce too. Not on real lava, vads!"),
    "item.guhs.lavalopers.uitleg": ("Je guh loopt ermee over echte lava én over alle kaassaus. Vahoeg!",
                                    "Your guh walks on top of real lava and every cheese sauce with these. Wahoog!"),
    "gui.guhs.kledingbron.loot_barbecuether": ("Kisten van de Mika's in de Barbecuether", "Chests of the Mikas in the Barbecuether"),
    "gui.guhs.kledingbron.loot_mijnschacht": ("Verlaten kaasmijnschacht", "Old Cheese Mineshaft"),
    "gui.guhs.kleding.prijs.hint.lavalopers_opwaarderen": (
        "opwaarderen bij de kleermaker: kaassauslopers + 4 magmacrème + 1 vahoege vads (rechtsklik hem met magmacrème)",
        "upgrade at the tailor: cheese sauce striders + 4 magma cream + 1 wahoog chonk (right-click him with magma cream)"),
    "gui.guhs.kleding.prijs.hint.kisten_in_de_kaasmijnschacht": ("zeldzaam in de kisten van de verlaten kaasmijnschacht",
                                                               "rare in the chests of the Old Cheese Mineshaft"),
    "gui.guhs.kleding.prijs.hint.kisten_van_de_barbecuether_mikas": ("zeldzaam in de kisten van het Mika-grillpaleis en de Spiesburcht",
                                                                    "rare in the chests of the Mika Grill Palace and the Skewer Keep"),
    "gui.guhs.voetjes.mist": ("De kleermaker kan je kaassauslopers opwaarderen tot lavalopers, maar hij mist nog: %s.",
                              "The tailor can upgrade your cheese sauce striders to lava striders, but he still needs: %s."),
    "gui.guhs.voetjes.mist.lopers": ("kaassauslopers (in je tas, of al ontgrendeld)", "cheese sauce striders (in your bag, or unlocked)"),
    "gui.guhs.voetjes.mist.magma": ("%s magmacrème", "%s magma cream"),
    "gui.guhs.voetjes.mist.vads": ("%s vahoege vads", "%s wahoog chonk"),
    "gui.guhs.voetjes.en": ("%s en %s", "%s and %s"),
    "gui.guhs.voetjes.al": ("Je hebt de lavalopers al, vads! Gebruik ze in de kledingkast van je guh.",
                            "You already have the lava striders, vads! Use them in your guh's wardrobe."),
    "gui.guhs.voetjes.opgewaardeerd": ("De kleermaker smelt de vahoege vads en de magmacrème in je kaassauslopers... Lavalopers! "
                                       "Nu loopt je guh zelfs over echte lava, njeg!",
                                       "The tailor melts the wahoog chonk and the magma cream into your cheese sauce striders... "
                                       "Lava striders! Now your guh even walks on real lava, njeg!"),
}

# --- loot: rare extra pools (one per table; found again by the item it gives, so re-running replaces it) --------------------
LOOT = [("grillpaleis_schat", "kaassauslopers", 5), ("grillpaleis_voorraad", "kaassauslopers", 9),
        ("spiesburcht", "kaassauslopers", 7), ("gatenkaas_mijnschacht", "mijnwerkerslaarsjes", 4)]


def loot(h):
    for table, item, leeg in LOOT:
        path = os.path.join(h.D, "loot_table", "chests", f"{table}.json")
        if not os.path.exists(path):
            print(f"voetjes: loot table {table} not found, skipped")
            continue

        def patch(d, item=item, leeg=leeg):
            naam = f"guhs:{item}"
            d["pools"] = [p for p in d["pools"] if not any(e.get("name") == naam for e in p.get("entries", []))]
            d["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": naam, "weight": 1},
                                                      {"type": "minecraft:empty", "weight": leeg}]})
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        patch(d)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")


def build(h):
    """(make_resources) texts, item models, loot."""
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    for c in CLOTHES:
        h.item_model(c)
    loot(h)


# =====================================================================================================================
# on its own: python tools/features/voetjes.py (from the project root)
# =====================================================================================================================
def _direct():
    import sys
    import types
    from PIL import Image
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def patch_json(path, fn):
        data = json.load(open(path, encoding="utf-8"))
        fn(data)
        w(path, data)

    def item_model(name, tex_=None, parent="minecraft:item/generated"):
        w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": tex_ or f"guhs:item/{name}"}})
        w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    NL, EN = {}, {}

    def lang(key, en, nl):
        EN[key], NL[key] = en, nl
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, patch_json=patch_json, item_model=item_model, lang=lang)
    build(h)

    # the bones: every feature's, exactly as make_guh_variants.add_bones() writes them (our pool is new, nothing moves)
    import make_guh_variants as v
    import make_clothes_icons as ic
    import features
    for module in features.modules():
        v.BONES.update(getattr(module, "BONES", {}))
        for bone in getattr(module, "BONES", {}):
            v.BONE_MODULE[bone] = module.__name__.split(".")[-1]
    swatches = v.add_bones()
    with open(v.GEO, encoding="utf-8") as f:   # (add_bones writes the platform's line ends; the repo keeps LF)
        geo = f.read()
    with open(v.GEO, "w", encoding="utf-8", newline="\n") as f:
        f.write(geo)
    # our clothes textures, with the rng a full make_guh_variants run gives this module
    base = Image.open(os.path.join(v.TEXTURES, "guh.png"))
    frng = np.random.default_rng(1000 + features.FEATURES.index("voetjes"))
    for name, painters in clothes(frng, v).items():
        a = np.zeros((base.height, base.width, 4), np.uint8)
        for swatch, paint in painters.items():
            u, vv = swatches[swatch]
            px = paint().astype(np.uint8)
            a[vv * 4:(vv + v.SWATCH) * 4, u * 4:(u + v.SWATCH) * 4, :3] = px[..., :3]
            a[vv * 4:(vv + v.SWATCH) * 4, u * 4:(u + v.SWATCH) * 4, 3] = 255
        Image.fromarray(a).save(os.path.join(v.TEXTURES, "guh_clothes", f"{name}.png"))
    for name, img in icons(ic).items():
        img.save(os.path.join(ic.OUT, f"{name}.png"))

    # the texts (and the English overlay of a full run)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    w(os.path.join("tools", "lang", "en", "c192_voetjes.json"), dict(sorted(EN.items())))
    print(f"voetjes: {len(CLOTHES)} pieces, {len(BONES)} bones, {len(NL)} texts, swatches "
          + ", ".join(f"{s}={swatches[s]}" for s in ("voet_schoen", "voet_zool", "voet_extra")), file=sys.stdout)


# --- FTB Quests (tools/make_ftbquests.py): a section of its own in Guhs & basis, by the wardrobe -------------------------------
FTB_CHAPTER = "guhs_basis"
FTB_SECTIES = [("voetjes", "Voetjes: slofjes, laarsjes en lopers", "item:guhs:pluisslofjes", None)]


def ftb(fq):
    q, item = fq.q, fq.item
    q("voetjes_slofjes", "Warme voetjes", "De kledingkast heeft een nieuw vakje: &dvoetjes&r! Koop pluisslofjes bij de kleermaker en doe ze je "
      "guh aan.", "guhs:pluisslofjes", [item("guhs:pluisslofjes")], xp=50)
    q("voetjes_regenlaarsjes", "Plassen springen", "Gele regenlaarsjes bij de kleermaker: precies goed bij de regenjas en de regenhoed.",
      "guhs:regenlaarsjes", [item("guhs:regenlaarsjes")], xp=50)
    q("voetjes_golfschoentjes", "Golfschoentjes", "De Golfguh verkoopt golfschoentjes, voor golfballetjes.", "guhs:golfschoentjes",
      [item("guhs:golfschoentjes")], xp=50)
    q("voetjes_mijnwerkerslaarsjes", "Stevige laarsjes", "Mijnwerkerslaarsjes zijn niet te koop: ze liggen heel soms in de kisten van "
      "de verlaten kaasmijnschacht (in de Gatenkaasgrotten).", "guhs:mijnwerkerslaarsjes", [item("guhs:mijnwerkerslaarsjes")], xp=100)
    q("voetjes_kaassauslopers", "Over de kaassaus", "Kaassauslopers liggen heel soms in de kisten van het Mika-grillpaleis en de Spiesburcht in de "
      "Guhbarbecuether. "
      "Een tamme guh die ze draagt loopt bovenop alle kaassaus alsof het vloer is, en volgt je gewoon naar de overkant.",
      "guhs:kaassauslopers", [item("guhs:kaassauslopers")], shape="hexagon", xp=200)
    q("voetjes_lavalopers", "Over de lava", "Breng de kleermaker kaassauslopers, 4 magmacrème en een vahoege vads (klik hem aan met de "
      "magmacrème): hij maakt er &6lavalopers&r van. Daarmee loopt je guh ook over echte lava.", "guhs:lavalopers",
      [item("guhs:lavalopers")], shape="hexagon", xp=300)


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
