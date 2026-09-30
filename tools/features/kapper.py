"""
De Pluiskapper "Knip & Vads" (2.8, plein slot kapper of the Knuffeldal town; nl.juiced.guhs.feature.kapper): Kapper
Krulletje (KAPPERGUH) and his salon, the hairstyles (GuhClothes.Slot.HAAR: 8 kapsels, bones outfit_haar_<stijl>*, the high
part "_kruin" hides under a hat) and the 8 hair dyes, the kappersshow minigame (krulmunt, highscore "kapper"), the Knus
collection "kapsels", the feestkapselset (Knusfeest task FEESTKAPSELS), the kappersstoel and the haarwasbak.

This module makes: the hair bones + textures (make_guh_variants hooks BONES / clothes), the icons (make_clothes_icons
hook icons), Krulletje's own model and texture, the blocks, items, particles, sounds, tags, recipes, advancements, lang,
the salon template (kapper_salon.py) and the GameTest room, and the FTB quests (row y = 87.5).
"""
import json
import os

import numpy as np
from PIL import Image

from features import kapper_salon as salon
from features import kapper_tex as tex

STIJLEN = tex.STIJLEN
VERVEN = list(tex.VERVEN)
CLOTHES = [f"kapsel_{s}" for s in STIJLEN] + ["kapperscape"]
FTB_Y = 87.5

# =====================================================================================================================
# the hair bones (make_guh_variants format: name: (parent, pivot, swatch, [(origin, size, inflate), ...]))
# the guh's head: x -8..8, top at y 13 (the middle part at y 15: x -5.5..5.5, z -10..-2), the face at z -12 (eyes y 7..12.5),
# the ears at x +-4.5..11.5, y 10..17, z -6.25..-4.75. Everything above the head's top in the middle is "_kruin".
# =====================================================================================================================
H = [0, 6, -2]
I = 0.05
CAP = [([-6.9, 12.9, -11.9], [13.8, 2.0, 11.6], I)]                    # the hair round the top of the head
TOP = [([-5.8, 14.8, -10.3], [11.6, 1.1, 8.6], I)]                     # on the middle of the top (under a hat: gone)
BACK = [([-6.8, 9.0, -0.5], [13.6, 5.0, 0.9], I)]


def mirror(cubes):
    """The same cubes on the other side (x -> -x)."""
    return [([round(-(o[0] + s[0]), 2), o[1], o[2]], s, i) for o, s, i in cubes]


def both(cubes):
    return cubes + mirror(cubes)


KRULLEN_ZIJ = [([7.0, y, z], [2.2, 2.2, 2.2], I) for y in (10.0, 12.0) for z in (-11.2, -8.8, -4.0, -1.8)]
STRIKJES_STAART = [([7.0, 9.6, -3.8], [2.4, 2.4, 2.4], I), ([7.4, 7.2, -3.6], [2.2, 2.4, 2.2], I), ([7.8, 5.0, -3.4], [2.0, 2.2, 2.0], I),
                   ([8.0, 3.2, -3.2], [1.8, 1.8, 1.8], I)]
STRIKJES_STRIK = [([7.2, 11.2, -4.4], [1.4, 1.8, 1.6], I), ([7.2, 11.2, -1.6], [1.4, 1.8, 1.6], I), ([7.0, 11.5, -2.8], [1.6, 1.2, 1.2], I)]
VLECHT = [([7.3 + (0.25 if i % 2 else 0), 10.8 - 2.0 * i, -2.3 + (0.25 if i % 2 else 0)], [2.2, 2.0, 2.2], I) for i in range(5)] + \
         [([6.6, 12.0, -2.5], [1.6, 1.6, 2.4], I)]
VLECHT_STRIK = [([7.1, 1.8, -2.6], [2.6, 1.0, 2.6], I)]
HANENKAM = [([-1.1, 14.6, z], [2.2, hgt, 2.4], I) for z, hgt in ((-12.4, 3.2), (-10.0, 4.4), (-7.6, 5.0), (-5.2, 4.4), (-2.8, 3.4))] + \
           [([-0.6, 14.6 + hgt, z + 0.5], [1.2, 0.8, 1.4], I) for z, hgt in ((-12.4, 3.2), (-10.0, 4.4), (-7.6, 5.0), (-5.2, 4.4), (-2.8, 3.4))]

HAAR = {
    "krullen": (CAP + [([x, 12.2, -12.9], [2.4, 2.2, 2.0], I) for x in (-6.0, -3.6, -1.2, 1.2, 3.6)] + both(KRULLEN_ZIJ)
                + [([x, y, -0.6], [2.4, 2.2, 2.2], I) for x in (-6.0, -3.6, -1.2, 1.2, 3.6) for y in (8.2, 10.4, 12.4)],
                TOP + [([x, 14.9, z], [2.5, 2.0, 2.4], I) for x in (-5.0, -2.5, 0.0, 2.5) for z in (-10.2, -7.8, -5.4, -3.0)]
                + [([x, 16.6, z], [2.4, 1.8, 2.4], I) for x in (-3.6, -1.2, 1.2) for z in (-8.6, -6.2, -3.8)]),
    "kuifje": (CAP + both([([6.9, 9.6, -11.4], [0.9, 3.4, 4.4], I)]) + BACK,
               TOP + [([-2.6, 14.8, -11.6], [5.2, 1.8, 4.2], I), ([-2.3, 16.3, -12.5], [4.6, 1.7, 3.8], I), ([-1.9, 17.7, -13.2], [3.8, 1.4, 3.2], I),
                      ([-1.4, 18.8, -13.6], [2.8, 1.0, 2.6], I), ([-1.0, 19.4, -12.4], [2.0, 0.8, 1.6], I)]),
    "knotjes": (CAP + [([-6.2, 12.5, -12.5], [12.4, 1.9, 1.6], I)] + BACK,
                TOP + both([([1.2, 15.4, -9.6], [4.0, 3.4, 4.0], I), ([1.8, 18.6, -9.0], [2.8, 1.2, 2.8], I)])),
    "strikjes": (CAP + [([x, 12.0, -12.7], [2.2, 2.6, 1.4], I) for x in (-5.5, -3.3, -1.1, 1.1, 3.3)] + both(STRIKJES_STAART) + BACK, TOP),
    "pluisbol": (CAP + both([([6.4, 7.8, -11.8], [3.2, 5.4, 5.0], I), ([6.4, 7.8, -4.4], [3.2, 5.8, 4.6], I)]) + [([-7.2, 6.8, -0.6], [14.4, 7.6, 2.4], I)],
                 [([-6.8, 14.4, -11.8], [13.6, 4.8, 12.0], I), ([-5.6, 19.2, -10.6], [11.2, 1.8, 9.6], I), ([-3.8, 21.0, -8.6], [7.6, 0.8, 5.6], I)]),
    "vlechtjes": (CAP + [([-6.4, 12.4, -12.6], [7.6, 2.0, 1.5], I), ([1.6, 12.8, -12.6], [4.8, 1.6, 1.5], I)] + BACK + both(VLECHT), TOP),
    "hanenkam": ([([-6.9, 12.9, -11.9], [13.8, 1.0, 11.6], I), ([-6.8, 10.0, -0.4], [13.6, 3.9, 0.6], I), ([-1.3, 12.9, -12.6], [2.6, 2.0, 12.4], I)],
                 HANENKAM),
    "matje": (CAP + [([-6.2, 12.6, -12.4], [12.4, 1.5, 1.2], I), ([-7.0, 6.0, -0.6], [14.0, 8.0, 1.8], I)]
              + both([([4.8, 5.2, 0.4], [3.0, 1.4, 1.8], I), ([6.8, 5.0, -3.6], [1.2, 7.8, 3.4], I)]), TOP),
}
BONES = {}
for _s, (_base, _kruin) in HAAR.items():
    BONES[f"outfit_haar_{_s}"] = ("head", H, "haar", _base)
    BONES[f"outfit_haar_{_s}_kruin"] = ("head", H, "haar", _kruin)
BONES["outfit_haar_strikjes_strik"] = ("head", H, "haar_strik", both(STRIKJES_STRIK))
BONES["outfit_haar_vlechtjes_strik"] = ("head", H, "haar_strik", both(VLECHT_STRIK))


def clothes(rng, v):
    out = {}
    for s in STIJLEN:
        painters = {"haar": (lambda st=s: tex.haar(rng, st))}
        if s in ("strikjes", "vlechtjes"):
            painters["haar_strik"] = lambda: tex.strik(rng)
        out[f"kapsel_{s}"] = painters
    out["kapperscape"] = {"cape": lambda: tex.cape(rng)}
    return out


def icons(ic):
    out = {f"kapsel_{s}": tex.kapsel_icon(s) for s in STIJLEN}
    out["kapperscape"] = tex.cape_icon()
    return out


# =====================================================================================================================
# Kapper Krulletje: the sitting guh, lavender, with a big pink curly perm, a comb in it and scissors in his paw
# =====================================================================================================================
UV, SW = 128, 8


def _used(geo):
    used = np.zeros((UV, UV), bool)
    for bone in geo["bones"]:
        for cube in bone.get("cubes", []):
            uv = cube.get("uv")
            if isinstance(uv, dict):
                for face in uv.values():
                    (u, v), (w, hh) = face["uv"], face["uv_size"]
                    used[int(min(v, v + hh)):int(np.ceil(max(v, v + hh))), int(min(u, u + w)):int(np.ceil(max(u, u + w)))] = True
            elif isinstance(uv, list):
                u, v = uv
                sx, sy, sz = cube["size"]
                used[int(v):int(np.ceil(v + sz + sy)), int(u):int(np.ceil(u + 2 * (sx + sz)))] = True
    return used


def _swatches(geo, names):
    used = _used(geo)
    out = {}
    for name in names:
        for y in range(0, UV - SW + 1, SW):
            for x in range(0, UV - SW + 1, SW):
                if name not in out and not used[y:y + SW, x:x + SW].any():
                    used[y:y + SW, x:x + SW] = True
                    out[name] = (x, y)
        if name not in out:
            raise SystemExit(f"kapper: no free texture space for {name}")
    return out


def _cube(origin, size, swatch, inflate=0.0):
    face = {"uv": list(swatch), "uv_size": [SW, SW]}
    c = {"origin": origin, "size": size, "uv": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}
    if inflate:
        c["inflate"] = inflate
    return c


def krulletje(h):
    geo_file = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh_sitting.geo.json"), encoding="utf-8"))
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_kapperguh"
    geo["description"]["visible_bounds_height"] = 2.5
    sw = _swatches(geo, ["krul", "kam", "schaar", "greep"])
    # the curly perm: curls all over the top of the head (the head: x -8..8, top y 26, front z -7, ears at y 23-30)
    curls = [_cube([x, 25.4, z], [2.6, 2.4, 2.6], sw["krul"], 0.05) for x in (-6.4, -3.8, -1.3, 1.2, 3.8) for z in (-6.8, -4.2, -1.6, 1.0, 3.6)]
    curls += [_cube([x, 27.6, z], [2.6, 2.2, 2.6], sw["krul"], 0.05) for x in (-3.8, -1.3, 1.2) for z in (-4.2, -1.6, 1.0)]
    curls += [_cube([-1.3, 29.6, -1.6], [2.6, 1.8, 2.6], sw["krul"], 0.05)]
    curls += [_cube([x, 23.4, -8.2], [2.4, 2.4, 2.0], sw["krul"], 0.05) for x in (-6.0, -3.6, -1.2, 1.2, 3.6)]
    curls += [_cube([sx, y, z], [2.2, 2.2, 2.2], sw["krul"], 0.05) for sx in (-9.0, 6.8) for y in (19.6, 21.8) for z in (-6.0, 2.2)]
    geo["bones"].append({"name": "kapper_krullen", "parent": "head", "pivot": [0, 26, 0], "cubes": curls})
    geo["bones"].append({"name": "kapper_kam", "parent": "head", "pivot": [4, 29, 2], "cubes": [
        _cube([2.4, 30.2, 1.2], [4.6, 0.8, 0.6], sw["kam"]), *[_cube([2.6 + 0.9 * i, 28.4, 1.2], [0.4, 1.8, 0.6], sw["kam"]) for i in range(5)]]})
    # the scissors in his right paw (the paw: x -4.5..-1.5, y 7..10.5, z -6.5..-3.5): rings at the paw, blades up
    geo["bones"].append({"name": "kapper_schaar", "parent": "arm_right", "pivot": [-3, 10, -6.8], "cubes": [
        _cube([-4.6, 8.4, -7.3], [1.4, 1.4, 0.5], sw["greep"]), _cube([-2.8, 8.4, -7.3], [1.4, 1.4, 0.5], sw["greep"])]})
    geo["bones"].append({"name": "kapper_schaar_blad_a", "parent": "kapper_schaar", "pivot": [-3.0, 9.8, -7.1], "cubes": [
        _cube([-3.5, 9.8, -7.4], [0.7, 5.2, 0.4], sw["schaar"])]})
    geo["bones"].append({"name": "kapper_schaar_blad_b", "parent": "kapper_schaar", "pivot": [-3.0, 9.8, -7.1], "cubes": [
        _cube([-2.8, 9.8, -7.0], [0.7, 5.2, 0.4], sw["schaar"])]})
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "guh_npc_kapperguh.geo.json"), geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.76, sat=0.5, val=1.0, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2841)

    def paint(swatch, block):
        x, y = swatch
        a[y * 4:(y + SW) * 4, x * 4:(x + SW) * 4, :3] = np.clip(block, 0, 255).astype(np.uint8)
        a[y * 4:(y + SW) * 4, x * 4:(x + SW) * 4, 3] = 255
    krul = tex.haar(rng, "krullen") * (np.array((255, 150, 205)) / 255.0)
    paint(sw["krul"], krul)
    paint(sw["kam"], np.full((32, 32, 3), 250.0) - rng.normal(0, 3, (32, 32, 1)))
    blad = np.zeros((32, 32, 3), np.float32)
    blad[:] = (214, 220, 232)
    blad[:, :8] = (250, 252, 255)
    paint(sw["schaar"], blad)
    paint(sw["greep"], np.full((32, 32, 3), (230, 80, 150), np.float32))
    h.save(Image.fromarray(a), "entity", "npc_kapperguh.png")


# =====================================================================================================================
# blocks, items, tags, sounds
# =====================================================================================================================
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def el(frm, to, texture, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": frm, "to": to, "faces": {f: {"texture": texture} for f in faces}}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # the kappersstoel (facing north: you sit looking north; the backrest at the south side)
    w(f"{A}/models/block/kappersstoel.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/kappersstoel_kussen", "kussen": "guhs:block/kappersstoel_kussen", "chroom": "guhs:block/kappersstoel_chroom"},
        "elements": [el([3, 0, 3], [13, 1, 13], "#chroom"), el([6, 1, 6], [10, 4, 10], "#chroom"), el([2, 4, 2], [14, 8, 14], "#kussen"),
                     el([2, 8, 11], [14, 19, 14], "#kussen"), el([5, 19, 11.5], [11, 22, 13.5], "#kussen"),
                     el([1, 8, 3], [3, 11, 11], "#chroom"), el([13, 8, 3], [15, 11, 11], "#chroom"), el([4, 1, 0], [12, 2, 3], "#chroom")]})
    # the haarwasbak (facing north: you stand at the north side, the golden tap at the south side)
    w(f"{A}/models/block/haarwasbak.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/haarwasbak_porselein", "bak": "guhs:block/haarwasbak_porselein", "schuim": "guhs:block/haarwasbak_schuim",
        "kraan": "guhs:block/haarwasbak_kraan"},
        "elements": [el([3, 0, 3], [13, 1, 13], "#bak"), el([5, 1, 5], [11, 9, 11], "#bak"), el([1, 9, 1], [15, 10, 15], "#bak"),
                     el([1, 10, 1], [15, 15, 3], "#bak"), el([1, 10, 13], [15, 15, 15], "#bak"), el([1, 10, 3], [3, 15, 13], "#bak"),
                     el([13, 10, 3], [15, 15, 13], "#bak"), el([3, 10, 3], [13, 13, 13], "#schuim", ("up",)),
                     el([7, 15, 12], [9, 20, 14], "#kraan"), el([7, 18, 9], [9, 20, 12], "#kraan")]})
    for name in ("kappersstoel", "haarwasbak"):
        w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{name}", **({"y": y} if y else {})}
                                                         for f, y in FACING_Y.items()}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
        h.self_drop(name)
    h.shaped("kappersstoel", [" P ", "PPP", " I "], {"P": "minecraft:pink_wool", "I": "minecraft:iron_ingot"}, "guhs:kappersstoel", 1)
    h.shaped("haarwasbak", ["Q Q", "QBQ", " I "], {"Q": "minecraft:quartz", "B": "minecraft:bucket", "I": "minecraft:gold_ingot"}, "guhs:haarwasbak", 1)
    for item in ["krulmunt", "feestkapselset", "kappersschaar"] + [f"haarverf_{v}" for v in VERVEN]:
        h.item_model(item, parent="minecraft:item/handheld" if item == "kappersschaar" else "minecraft:item/generated")
    # hair dye: a glass bottle, a dye and a kaasknabbel (the rainbow one only from Krulletje)
    dyes = {"roze": "pink_dye", "mint": "lime_dye", "citroen": "yellow_dye", "lavendel": "purple_dye", "hemelsblauw": "light_blue_dye",
            "perzik": "orange_dye", "zilver": "light_gray_dye"}
    for v, dye in dyes.items():
        h.shapeless(f"haarverf_{v}", ["minecraft:glass_bottle", f"minecraft:{dye}", "guhs:kaas_knabbels"], f"guhs:haarverf_{v}", 1)
    add = h.add_tag
    add("guhs/tags/item/loaned", ["guhs:kappersschaar"])
    add("guhs/tags/item/knus/grijptickets", ["guhs:krulmunt"])
    add("guhs/tags/item/knus/feestkapsels", ["guhs:feestkapselset"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:kappersstoel", "guhs:haarwasbak"])


SOUNDS = {
    "kapper.knip": [{"name": "minecraft:entity.sheep.shear", "type": "event", "pitch": 1.5, "volume": 0.9}],
    "kapper.fohn": [{"name": "minecraft:entity.breeze.whirl", "type": "event", "pitch": 1.4, "volume": 0.7}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements
# =====================================================================================================================
QUEST = ["kapper_krulletje", "kapper_eerste_klant", "kapper_klanten", "kapper_show", "kapper_eigen_guh", "kapper_haarverf",
         "kapper_alle_kapsels", "kapper_eerste_show", "seen_kapperguh"]


def advancements(h):
    D = h.D
    for name in QUEST:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    h.w(f"{D}/advancement/quest/kapper_cape.json", {"criteria": {"done": {"trigger": "minecraft:inventory_changed",
                                                                          "conditions": {"items": [{"items": "guhs:kapperscape"}]}}}})
    impossible = {"done": {"trigger": "minecraft:impossible"}}
    for name, parent, icon, frame, title, desc in [
        ("kapper_eerste_kapsel", "stadje", "guhs:kapsel_krullen", "task", "Knip knip!",
         "Geef een guh een nieuw kapsel bij Knip & Vads (of met een kapsel op je eigen tamme guh)"),
        ("kapper_show", "kapper_eerste_kapsel", "guhs:kappersschaar", "goal", "Vahoege kappersshow",
         "Haal minstens 120 punten in een kappersshow van Kapper Krulletje"),
        ("kapper_alle_kapsels", "kapper_show", "guhs:haarverf_regenboog", "challenge", "Kapselcollectie compleet",
         "Vul de hele kapselcollectie: alle 8 kapsels en alle 8 haarverfjes"),
    ]:
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": impossible})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


# =====================================================================================================================
# texts (Dutch in both languages)
# =====================================================================================================================
KAPSEL_NAMEN = {"krullen": "Krullen", "kuifje": "Kuifje", "knotjes": "Knotjes", "strikjes": "Strikjes", "pluisbol": "Pluisbol",
                "vlechtjes": "Vlechtjes", "hanenkam": "Hanenkam", "matje": "Matje"}
KAPSEL_INFO = {
    "krullen": "Een bos vadsige krulletjes, overal. Niet te kammen, wel te knuffelen.",
    "kuifje": "Een stoer kuifje dat vooruit wijst: daar liggen de kaasknabbels!",
    "knotjes": "Twee knotjes bovenop, als twee kaasbolletjes. Superschattig.",
    "strikjes": "Twee staartjes met witte strikjes. Voor als je guh er extra lief uit wil zien.",
    "pluisbol": "Een enorme pluisbol: de oren steken er gewoon doorheen. VAHOEG!",
    "vlechtjes": "Twee lange vlechtjes met een strikje aan het eind. Bijna tot op de pootjes!",
    "hanenkam": "Een hanenkam van pluis: stoer van voren, lief van achteren.",
    "matje": "Van voren netjes, van achteren feest. Het beroemde guhmatje.",
}
VERF_NAMEN = {"roze": "Roze", "mint": "Mint", "citroen": "Citroen", "lavendel": "Lavendel", "hemelsblauw": "Hemelsblauw",
              "perzik": "Perzik", "zilver": "Zilver", "regenboog": "Regenboog"}


def texts(h):
    L = []
    for s in STIJLEN:
        L += [(f"item.guhs.kapsel_{s}", f"Kapsel: {KAPSEL_NAMEN[s]}"), (f"gui.guhs.kapper.kapsel.{s}", KAPSEL_NAMEN[s]),
              (f"gui.guhs.knus.kapsels.kapsel_{s}", f"Kapsel: {KAPSEL_NAMEN[s]}"), (f"gui.guhs.knus.kapsels.kapsel_{s}.info", KAPSEL_INFO[s])]
    for v in VERVEN:
        L += [(f"item.guhs.haarverf_{v}", f"Haarverf ({VERF_NAMEN[v].lower()})"), (f"gui.guhs.kapper.verf.{v}", VERF_NAMEN[v]),
              (f"gui.guhs.knus.kapsels.haarverf_{v}", f"Haarverf: {VERF_NAMEN[v].lower()}")]
    L += [(f"gui.guhs.knus.kapsels.haarverf_regenboog.info", "Regenbooghaar blijft van kleur veranderen, net als een regenboogguh. Alleen bij Krulletje!")]
    L += [
        ("entity.guhs.guh_npc.kapperguh", "Kapper Krulletje"),
        ("entity.guhs.kapper_klant", "Klant"),
        ("gui.guhs.guhdex.rarity.kapperguh", "Zeldzaamheid: uniek (Knip & Vads, in elk Knuffeldal-stadje)"),
        ("gui.guhs.guhdex.info.kapperguh", "Kapper Krulletje knipt, krult en föhnt elke guh vahoeg mooi. Zijn eigen krullen zet hij elke ochtend "
         "met een kaasknabbel in de krulspelden. Een guh is nooit te vads voor een nieuw kapsel, zegt hij altijd: alleen niet vahoeg genoeg."),
        ("item.guhs.krulmunt", "Krulmunt"),
        ("item.guhs.feestkapselset", "Feestkapselset"),
        ("item.guhs.feestkapselset.lore", "Speldjes, strikjes en glitterspray voor de feestkapsels. Breng het naar Burgemeester Vadsema!"),
        ("item.guhs.kappersschaar", "Kappersschaar"),
        ("item.guhs.kappersschaar.lore", "Geleend van Kapper Krulletje. Rechtsklik een klant om te knippen."),
        ("item.guhs.kappersschaar.lore2", "Shift + rechtsklik je eigen tamme guh: alle haren eraf (njeg!)"),
        ("item.guhs.haarverf.lore", "Rechtsklik je eigen tamme guh met een kapsel: zijn haar krijgt deze kleur"),
        ("item.guhs.kapsel.lore", "Een blijvend kapsel: rechtsklik je eigen tamme guh (het oude kapsel is dan weg)"),
        ("item.guhs.kapperscape", "Kapperscape"),
        ("block.guhs.kappersstoel", "Kappersstoel"),
        ("block.guhs.haarwasbak", "Haarwasbak"),
        ("subtitles.guhs.kapper.knip", "Schaar knipt"),
        ("subtitles.guhs.kapper.fohn", "Föhn blaast"),
        ("gui.guhs.knus.slot.kapper", "Knip & Vads"),
        ("gui.guhs.knus.onderdeel.kapper", "Knip & Vads"),
        ("gui.guhs.knus.verzameling.kapsels", "Kapselcollectie"),
        ("gui.guhs.knus.mijlpaal.kapper_eerste_klant", "Eerste tevreden klant"),
        ("gui.guhs.knus.mijlpaal.kapper_klanten", "30 tevreden klanten"),
        ("gui.guhs.knus.mijlpaal.kapper_perfect", "10 perfecte kapsels"),
        ("gui.guhs.knus.mijlpaal.kapper_show", "120 punten in een kappersshow"),
        ("gui.guhs.knus.mijlpaal.kapper_eigen_guh", "Je eigen guh naar de kapper"),
        ("gui.guhs.knus.mijlpaal.kapper_collectie", "Kapselcollectie compleet"),
        ("gui.guhs.scorebord.kapper", "Top 3 vahoegste kappers", ),
        ("gui.guhs.scorebord.kapper.punten", "meeste punten in een kappersshow"),
        # own guhs
        ("gui.guhs.kapper.alleen_eigen", "Njeg! Alleen je eigen tamme guh mag naar de kapper."),
        ("gui.guhs.kapper.nieuw_kapsel", "Knip knip! %s heeft nu: %s. VAHOEG!"),
        ("gui.guhs.kapper.eerst_kapsel", "Njeg, eerst een kapsel! Kale plukjes kun je niet verven."),
        ("gui.guhs.kapper.geverfd", "%s heeft nu %s haar. Wat een plaatje!"),
        ("gui.guhs.kapper.al_kaal", "Njeg, er valt niks meer te knippen."),
        ("gui.guhs.kapper.kaal", "Knip knip knip... %s is weer lekker naturel. Njeg!"),
        ("gui.guhs.kapper.geen_kapsel", "%s heeft nog geen kapsel. Koop er een bij Kapper Krulletje!"),
        ("gui.guhs.kapper.huidig", "%s: %s, %s"),
        ("gui.guhs.kapper.verf.naturel", "Naturel"),
        # Krulletje talks
        ("quest.guhs.kapper.hoi.0", "Welkom bij Knip & Vads! Een guh is nooit te vads voor een nieuw kapsel... alleen soms niet vahoeg genoeg. Zin in een kappersshow?"),
        ("quest.guhs.kapper.hoi.1", "Krul krul! Mijn klanten wachten al. Help je me knippen? De Mika's hebben mijn kaasknabbels gepikt, dus ik kan elke hulp gebruiken!"),
        ("quest.guhs.kapper.hoi.2", "Njeg, wat een pluis vandaag! Pak de schaar, dan maken we er iets vahoegs van."),
        ("quest.guhs.kapper.bezig", "Je bent nog bezig! Je klant zit te wachten, schiet op met die schaar!"),
        ("quest.guhs.kapper.druk", "Njeg, de salon is bezet! Kijk even mee, straks ben jij aan de beurt."),
        ("quest.guhs.kapper.schaar", "Hier, een kappersschaar. Die mag je lenen. Niet in je eigen oren knippen, guh!"),
        ("quest.guhs.kapper.geen_stoel", "Njeg... waar is mijn kappersstoel gebleven? Zonder stoel geen show."),
        ("quest.guhs.kapper.niet_jouw_klant", "Dit is niet jouw klant! Njeg, niet voorkruipen."),
        ("quest.guhs.kapper.uitleg", "De kappersshow begint! %s klanten komen een voor een in de stoel zitten. Kijk goed naar de foto: dat kapsel en die kleur "
         "willen ze. Eerst 3x wassen, dan knippen, dan verven, en föhnen maakt het af. Hoe sneller, hoe meer punten, en perfecte kapsels achter elkaar geven een combo!"),
        ("quest.guhs.kapper.uitleg_feest", "De feestkappersshow! %s feestgasten willen een feestkapsel voor het Grote Knusfeest. Haal genoeg punten, "
         "dan krijg je de feestkapselset voor Burgemeester Vadsema!"),
        ("quest.guhs.kapper.weggelopen", "Je liep de salon uit: de show is voorbij!"),
        ("quest.guhs.kapper.klaar", "De show is klaar! %s punten, %s perfecte kapsels bij %s klanten: %s krulmunt(en)."),
        ("quest.guhs.kapper.record", "NIEUW RECORD: %s punten! (was %s)"),
        ("quest.guhs.kapper.record_eerste", "Je eerste record: %s punten! Kom jij in de top 3 boven de showstoel? VAHOEG!"),
        ("quest.guhs.kapper.best", "Jouw record: %s punten."),
        ("quest.guhs.kapper.eerste", "Je eerste kappersshow! Een welkomstcadeautje: 4 krulmunten. Njeg, wat knip jij mooi!"),
        ("quest.guhs.kapper.feest_gelukt", "VAHOEG! Wat een feestkapsels! Hier is de feestkapselset. Breng hem snel naar Burgemeester Vadsema!"),
        ("quest.guhs.kapper.feest_bijna", "Njeg, bijna! Voor de feestkapselset heb je minstens %s punten nodig. Nog een keer?"),
        ("quest.guhs.kapper.title.show", "Kappersshow!"),
        ("quest.guhs.kapper.title.feest", "Feestkappersshow!"),
        ("quest.guhs.kapper.title.show.sub", "wassen, knippen, verven, föhnen"),
        ("quest.guhs.kapper.title.vahoeg", "VAHOEG!"),
        ("quest.guhs.kapper.title.njeg", "Njeg..."),
        ("quest.guhs.kapper.title.te_laat", "Te laat!"),
        ("quest.guhs.kapper.uitslag.perfect", "Perfect kapsel! +%s punten"),
        ("quest.guhs.kapper.uitslag.combo", "Perfect! +%s punten, combo x%s!"),
        ("quest.guhs.kapper.uitslag.kleur_fout", "Goed kapsel, verkeerde kleur. +%s punten"),
        ("quest.guhs.kapper.uitslag.kapsel_fout", "Goede kleur, verkeerd kapsel. +%s punten"),
        ("quest.guhs.kapper.uitslag.njeg", "Njeg... niks goed, maar de klant zegt toch lief dankjewel. +%s punt"),
        ("quest.guhs.kapper.uitslag.te_laat", "De klant moest nog naar de Knabbelbakkerij... maar zwaait lief gedag."),
        ("quest.guhs.kapper.bar.tingeling", "Tingeling! Klant %s van %s komt binnen"),
        ("quest.guhs.kapper.bar.klant", "Klant %s/%s  -  nog %s s  -  %s punten"),
        ("quest.guhs.kapper.bar.foto_weg", "De foto is weggelegd! Weet je het nog?"),
        ("quest.guhs.kapper.bar.eerst_wassen", "Njeg, eerst wassen! (3x schuim)"),
        ("quest.guhs.kapper.bar.eerst_knippen", "Njeg, eerst knippen!"),
        # the talking screen
        ("gui.guhs.kapper.ondertitel", "Knip & Vads"),
        ("gui.guhs.kapper.uitleg", "Klanten komen een voor een in de stoel zitten, met een foto van het kapsel dat ze willen. Wassen, knippen, "
         "verven, föhnen: hoe sneller en preciezer, hoe meer punten en krulmunten!"),
        ("gui.guhs.kapper.bezet", "%s geeft nu een kappersshow. Er kan er maar een tegelijk: kijk mee en wacht op je beurt!"),
        ("gui.guhs.kapper.start", "Kappersshow!"),
        ("gui.guhs.kapper.start.tooltip", "8 klanten, elk met een eigen wens. Tijdens de show krijg je geen honger of schade."),
        ("gui.guhs.kapper.feest", "Feestkappersshow!"),
        ("gui.guhs.kapper.feest.tooltip", "Voor het Grote Knusfeest: haal minstens %s punten en je krijgt de feestkapselset"),
        ("gui.guhs.kapper.shop", "Winkeltje"),
        ("gui.guhs.kapper.shop.tooltip", "Kapsels en haarverf voor je eigen guh, de kapperscape en salonmeubels, voor krulmunten"),
        ("gui.guhs.kapper.best", "Jouw record: %s punten"),
        ("gui.guhs.kapper.geen_best", "Nog geen record: tijd voor een show!"),
        ("gui.guhs.kapper.munten", "Krulmunten op zak: %s (1 per 20 punten +1)"),
        ("gui.guhs.kapper.eerste", "Bij je eerste show krijg je een welkomstcadeautje!"),
        # the knip screen
        ("gui.guhs.kapper.knip.titel", "Knippen"),
        ("gui.guhs.kapper.knip.kop", "Knip & Vads - klant %s/%s"),
        ("gui.guhs.kapper.knip.kop_feest", "Feestkapsels - gast %s/%s"),
        ("gui.guhs.kapper.knip.score", "%s punten - combo %s"),
        ("gui.guhs.kapper.knip.foto", "Zo wil ik het!"),
        ("gui.guhs.kapper.knip.foto_weg", "foto weggelegd"),
        ("gui.guhs.kapper.knip.klant", "De klant"),
        ("gui.guhs.kapper.knip.stap_was", "1. Wassen"),
        ("gui.guhs.kapper.knip.stap_knip", "2. Knippen"),
        ("gui.guhs.kapper.knip.stap_verf", "3. Verven (N = naturel)"),
        ("gui.guhs.kapper.knip.wassen", "Schuim! (%s/%s)"),
        ("gui.guhs.kapper.knip.wassen.tooltip", "Drie keer schuimen, dan is het haar schoon genoeg om te knippen"),
        ("gui.guhs.kapper.knip.fohn", "4. Föhnen!"),
        ("gui.guhs.kapper.knip.fohn.tooltip", "Klaar? De föhn maakt het af en de klant gaat tevreden weg. Kijk eerst nog even naar de foto!"),
        ("gui.guhs.kapper.knip.kijk", "Nog eens kijken (-%s)"),
        ("gui.guhs.kapper.knip.stop", "Stoppen"),
        ("gui.guhs.kapper.knip.stop.tooltip", "De show stopt; je krijgt de krulmunten voor je punten"),
        ("gui.guhs.kapper.knip.sluit", "Even weg"),
        ("gui.guhs.kapper.knip.sluit.tooltip", "Het scherm gaat dicht, de show gaat door! Rechtsklik de klant om terug te komen"),
        ("gui.guhs.kapper.knip.aftellen", "Klaar voor de start... de eerste klant komt zo binnen!"),
        ("gui.guhs.kapper.knip.volgende", "Tingeling... daar komt de volgende klant!"),
        ("gui.guhs.kapper.knip.hint", "wassen - knippen - verven - föhnen"),
        # structure
        ("structure.guhs.knuffeldal_stadje.kapper", "Knip & Vads"),
    ]
    for key, text in L:
        h.lang(key, text, text)


# =====================================================================================================================
# the GameTest room: a little salon floor with Krulletje, the showstoel, a haarwasbak and a second chair further away
# =====================================================================================================================
def test_templates(h):
    mc = h.mc
    s = h.Structure((13, 6, 13))
    for x in range(13):
        for z in range(13):
            s.set(x, 0, z, "guhs:knuffelklinkers" if (x + z) % 2 else mc("white_concrete"))
    s.set(6, 1, 6, "guhs:kappersstoel", {"facing": "south"})
    s.set(11, 1, 1, "guhs:kappersstoel", {"facing": "west"})
    s.set(9, 1, 6, "guhs:haarwasbak", {"facing": "west"})
    s.entity(4.5, 1.0, 6.5, {"id": "guhs:guh_npc", "Kind": "kapperguh", "PersistenceRequired": h.ms.Byte(1), "Rotation": h.ms.floats(270.0, 0.0)})
    s.save("kapper_test_salon")


def selfcheck_assets(h):
    A = h.A
    missing = []
    for b in ("kappersstoel", "haarwasbak"):
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
    for i in ["krulmunt", "feestkapselset", "kappersschaar", "kappersstoel", "haarwasbak"] + [f"haarverf_{v}" for v in VERVEN]:
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
    for t in ["item/krulmunt", "item/feestkapselset", "item/kappersschaar", "entity/npc_kapperguh"] + [f"item/haarverf_{v}" for v in VERVEN]:
        if not os.path.exists(os.path.join(h.TEX, *t.split("/")) + ".png"):
            missing.append(f"texture {t}")
    if missing:
        raise SystemExit(f"kapper assets missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    krulletje(h)
    sounds(h)
    advancements(h)
    texts(h)
    b = salon.build(h)
    test_templates(h)
    selfcheck_assets(h)
    print(f"kapper: salon {len(b.s.blocks)} blocks, {b.walkable} walkable, {b.gezichten} + {b.grote_gezichten} guh faces")


# =====================================================================================================================
# FTB quests (row y = 87.5), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("kapper_krulletje", "Knip & Vads", "Aan het plein van het Knuffeldal-stadje zit een kapsalon met een enorme roze krullenbol als dak: "
      "&dKnip & Vads&r. Praat met &dKapper Krulletje&r: hij leent je een kappersschaar.",
      "guhs:kappersschaar", [fq.adv("kapper_krulletje")], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, xp=100)
    q("kapper_eerste_klant", "Knip knip!", "Geef een klant in de kappersshow het goede kapsel. Eerst 3x wassen, dan knippen, verven en föhnen!",
      "guhs:kapsel_krullen", [fq.adv("kapper_eerste_klant")], rewards=(("guhs:krulmunt", 2),), x=-6.5, y=y, xp=100)
    q("kapper_eerste_show", "De show moet door", "Maak een hele kappersshow af (of stop wanneer je wil). Je krijgt krulmunten voor je punten.",
      "guhs:krulmunt", [fq.adv("kapper_eerste_show")], rewards=(("guhs:krulmunt", 3),), x=-5, y=y, xp=150)
    q("kapper_show", "Vahoege kappersshow", "Haal minstens &e120 punten&r in een kappersshow. Snel en precies knippen, en perfecte kapsels "
      "achter elkaar geven een combo! De beste kappers staan in de &etop 3&r boven de showstoel.",
      "guhs:kappersschaar", [fq.adv("kapper_show")], rewards=(("guhs:krulmunt", 6),), x=-3.5, y=y, xp=300, shape="hexagon")
    q("kapper_eigen_guh", "Mijn guh naar de kapper", "Koop een kapsel bij Kapper Krulletje (of win er een) en rechtsklik je eigen tamme guh: "
      "een blijvend nieuw kapsel! Met een kapsel op kun je hem ook verven.",
      "guhs:kapsel_strikjes", [fq.adv("kapper_eigen_guh")], rewards=(("guhs:haarverf_roze", 1),), x=-2, y=y, xp=150)
    q("kapper_haarverf", "Een kleurtje", "Verf het haar van je tamme guh met haarverf (maak het met een glazen flesje, een kleurstof en een kaasknabbel).",
      "guhs:haarverf_mint", [fq.adv("kapper_haarverf")], rewards=(("guhs:haarverf_lavendel", 1),), x=-0.5, y=y, xp=100)
    q("kapper_cape", "Kapperscape", "Koop de kapperscape bij Kapper Krulletje (voor krulmunten). Kopen is genoeg; aantrekken mag, je guh wordt er extra vadsig chique van.",
      "guhs:kapperscape", [fq.adv("kapper_cape")], rewards=(("guhs:kaas_knabbels", 16),), x=1, y=y, xp=100)
    q("kapper_collectie", "Kapselcollectie", "Vul de hele kapselcollectie in de Guhdex (Knus): alle 8 kapsels en alle 8 haarverfjes. "
      "Ze tellen als je ze je eigen guh geeft, of als je een klant perfect knipt.",
      "guhs:haarverf_regenboog", [fq.adv("kapper_alle_kapsels")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=2.5, y=y, xp=500, shape="gear")
    q("kapper_guhdex", "Krullenkop", "Zet Kapper Krulletje in je Guhdex.", "guhs:guhdex", [fq.adv("seen_kapperguh")],
      rewards=(("guhs:kaas_knabbels", 8),), x=4, y=y, shape="rsquare", xp=100)
    q("kapper_klanten", "30 tevreden klanten", "Knip &e30 klanten&r tevreden in de kappersshows van Kapper Krulletje. "
      "Elke guh die met een vahoeg kapsel de salon uit waggelt, telt mee. Zo vads, zo fris!",
      "guhs:haarverf_regenboog", [fq.adv("kapper_klanten")], rewards=(("guhs:krulmunt", 8),), x=5.5, y=y, xp=300, shape="hexagon")
