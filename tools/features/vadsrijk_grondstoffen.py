"""
1.10.4 "Grondstoffen zie je nu echt": the real, shared gathering blocks of Het Vadsrijk (Java: feature/vadsrijk/ambacht/GrondstofBlok
+ Grondstoffen). One block per spot kind (vr_grondstof_erts/hout/kruid/vis/duik; hives stay drawn stands), properties tier 1..6 (1.10.4: T6 =
zone 6, Het Zijdeland, its own look), vol (full / empty) and waterlogged (fishing + dive spots; not in the variant keys).

Looks per tier (full -> empty):
  - erts (an ore vein in the floor rock, the mod's knuffel ores): T1 ijzererts (Pluisijzer), T2 diamanterts (Kandijzilver),
    T3 kopererts (Amberkoper), T4 gouderts (Lampionbrons), T5 a white-blue recolour (Hemelzilver), T6 Sterrengoud: rose sandstone
    with gold flecks and white star glints; empty = the same rock with the ore picked out (dark dimples);
  - hout (an upright chopping stump beside the tree): T1 zwamsteel, T2 spruce, T3 dark oak, T4 bamboo, T5 pale oak, T6 jungle wood
    (the date palm); empty = a low cut stump with the stripped rings on top;
  - kruid (a bush on the grass): T1 flowering azalea -> azalea, T2 sweet berries -> bare berry bush, T3 firefly bush (glows) -> dead
    bush, T4 torchflower -> its sprout, T5 lily of the valley -> dead bush, T6 orange tulip (saffron) -> short dry grass;
  - vis (a pink bobber in ripples on the water) -> calm water (nothing);
  - duik (brain coral on the sea floor) -> dead brain coral.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_grondstoffen.py
"""
import json
import os

from PIL import Image

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
TEX = os.path.join(A, "textures", "block", "vr_grondstof")
T = "guhs:block/vr_grondstof/"

ERTS = {1: "ijzererts_knuffel", 2: "diamanterts_knuffel", 3: "kopererts_knuffel", 4: "gouderts_knuffel", 5: None, 6: None}
TIERS = 6
HOUT = {  # tier: (side, top (full), top (empty: the cut))
    1: ("minecraft:block/mushroom_stem", "minecraft:block/mushroom_block_inside", "minecraft:block/mushroom_block_inside"),
    2: ("minecraft:block/spruce_log", "minecraft:block/spruce_log_top", "minecraft:block/stripped_spruce_log_top"),
    3: ("minecraft:block/dark_oak_log", "minecraft:block/dark_oak_log_top", "minecraft:block/stripped_dark_oak_log_top"),
    4: ("minecraft:block/bamboo_block", "minecraft:block/bamboo_block_top", "minecraft:block/stripped_bamboo_block_top"),
    5: ("minecraft:block/pale_oak_log", "minecraft:block/pale_oak_log_top", "minecraft:block/stripped_pale_oak_log_top"),
    6: ("minecraft:block/jungle_log", "minecraft:block/jungle_log_top", "minecraft:block/stripped_jungle_log_top"),
}
KRUID = {  # tier: (full vanilla model, empty vanilla model)
    1: ("minecraft:block/flowering_azalea", "minecraft:block/azalea"),
    2: ("minecraft:block/sweet_berry_bush_stage3", "minecraft:block/sweet_berry_bush_stage1"),
    3: ("minecraft:block/firefly_bush", "minecraft:block/dead_bush"),
    4: ("minecraft:block/torchflower", "minecraft:block/torchflower_crop_stage1"),
    5: ("minecraft:block/lily_of_the_valley", "minecraft:block/dead_bush"),
    6: ("minecraft:block/orange_tulip", "minecraft:block/short_dry_grass"),
}
SOORTEN = ("erts", "hout", "kruid", "vis", "duik")

TEKSTEN = {}
EN = {}


def t(k, nl, en):
    TEKSTEN[k] = nl
    EN[k] = en


t("block.guhs.vr_grondstof_erts", "Ertsader", "Ore vein")
t("block.guhs.vr_grondstof_hout", "Hakstronk", "Chopping stump")
t("block.guhs.vr_grondstof_kruid", "Kruidenstruik", "Herb bush")
t("block.guhs.vr_grondstof_vis", "Visplek", "Fishing spot")
t("block.guhs.vr_grondstof_duik", "Koraalplek", "Coral spot")
t("vadsrijk.ambacht.hud.leeg", "%s · leeg, komt zo weer terug", "%s · empty, back soon")


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _png(naam):
    return Image.open(os.path.join(A, "textures", "block", f"{naam}.png")).convert("RGBA")


def _erts_masker(im):
    """The ore pixels of a knuffel ore: clearly different from the plain knuffelsteen it was painted on."""
    basis = _png("knuffelsteen")
    m = set()
    for y in range(16):
        for x in range(16):
            p, q = im.getpixel((x, y)), basis.getpixel((x, y))
            if sum((p[i] - q[i]) ** 2 for i in range(3)) ** 0.5 > 35:
                m.add((x, y))
    return m


def rozensteen(im):
    """The plain knuffelsteen tinted to the rose sandstone of the Zijdeland (zone 6): warm pink, the same grain."""
    uit = im.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = im.getpixel((x, y))
            v = (r + g + b) / 3 / 255
            uit.putpixel((x, y), (min(255, int(150 + 100 * v)), min(255, int(95 + 95 * v)), min(255, int(100 + 90 * v)), a))
    return uit


def leeg_erts(im, basis=None):
    """The same rock with the ore picked out: every ore pixel becomes a shadowed dimple in the plain knuffelsteen."""
    basis = basis or _png("knuffelsteen")
    uit = basis.copy()
    m = _erts_masker(im)
    for (x, y) in m:
        r, g, b, _a = basis.getpixel((x, y))
        rand = (x, y - 1) not in m   # (the upper edge of a dimple catches a little light)
        f = 0.8 if rand else 0.6
        uit.putpixel((x, y), (int(r * f), int(g * f * 0.95), int(b * f), 255))
    return uit


def hemelzilver(im):
    """T5 Hemelzilver: the iron vein recoloured sky blue and silver with white glints."""
    uit = im.copy()
    for (x, y) in _erts_masker(im):
        px = im.getpixel((x, y))
        v = (px[0] + px[1] + px[2]) / 3 / 255
        glans = (x * 7 + y * 3) % 9 == 0
        c = (255, 255, 255) if glans else (int(90 + 110 * v), int(140 + 90 * v), int(215 + 40 * v))
        uit.putpixel((x, y), (*[min(255, k) for k in c], 255))
    return uit


def sterrengoud(im):
    """T6 Sterrengoud: the iron vein as warm gold flecks with white star glints, in rose sandstone (zone 6)."""
    m = _erts_masker(im)
    uit = rozensteen(_png("knuffelsteen"))
    for (x, y) in m:
        px = im.getpixel((x, y))
        v = (px[0] + px[1] + px[2]) / 3 / 255
        ster = (x * 5 + y * 7) % 11 == 0
        c = (255, 255, 240) if ster else (int(215 + 40 * v), int(160 + 70 * v), int(40 + 50 * v))
        uit.putpixel((x, y), (*[min(255, k) for k in c], 255))
    return uit


def rimpels():
    """The fishing spot: two pale ripple rings on transparent (cutout)."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 6.6 <= d <= 7.4:
                im.putpixel((x, y), (215, 240, 255, 255))
            elif 3.6 <= d <= 4.3 and (x + y) % 2 == 0:
                im.putpixel((x, y), (235, 250, 255, 255))
    return im


def dobber():
    """The bobber: top half pink, a white band, bottom half white (uv rows 0-7 pink, 8-15 white)."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), (255, 105, 160, 255) if y < 7 else (255, 255, 255, 255) if y > 8 else (255, 210, 225, 255))
    return im


def _alle_vlakken(tex, uv=None):
    return {f: {"texture": tex, **({"uv": uv} if uv else {})} for f in ("north", "south", "west", "east")}


def stronk(side, top, hoogte):
    """An upright stump of 14x14, {hoogte} px high: bark around, rings on top."""
    zij_uv = [1, 16 - hoogte, 15, 16]
    vlakken = _alle_vlakken("#side", zij_uv)
    vlakken["up"] = {"texture": "#top", "uv": [1, 1, 15, 15]}
    vlakken["down"] = {"texture": "#top", "uv": [1, 1, 15, 15], "cullface": "down"}
    return {"parent": "minecraft:block/block", "textures": {"particle": side, "side": side, "top": top},
            "elements": [{"from": [1, 0, 1], "to": [15, hoogte, 15], "faces": vlakken}]}


def modellen():
    mod, bs = {}, {}
    for tier in range(1, TIERS + 1):
        # ore: cube_all
        vol = f"guhs:block/{ERTS[tier]}" if ERTS[tier] else f"{T}erts_t{tier}"
        mod[f"vr_grondstof_erts_t{tier}_vol"] = {"parent": "minecraft:block/cube_all", "textures": {"all": vol}}
        mod[f"vr_grondstof_erts_t{tier}_leeg"] = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{T}erts_t{tier}_leeg"}}
        side, top, snee = HOUT[tier]
        mod[f"vr_grondstof_hout_t{tier}_vol"] = stronk(side, top, 16)
        mod[f"vr_grondstof_hout_t{tier}_leeg"] = stronk(side, snee, 5)
    # fishing: ripples just above the water surface (14.2/16) + a bobbing pink float
    mod["vr_grondstof_vis_vol"] = {
        "ambientocclusion": False,
        "textures": {"particle": f"{T}dobber", "rimpel": f"{T}rimpel", "dobber": f"{T}dobber"},
        "elements": [
            {"from": [0, 14.6, 0], "to": [16, 14.6, 16], "shade": False,
             "faces": {"up": {"texture": "#rimpel", "uv": [0, 0, 16, 16]}, "down": {"texture": "#rimpel", "uv": [0, 16, 16, 0]}}},
            {"from": [7, 13.5, 7], "to": [9, 15.5, 9],
             "faces": {**_alle_vlakken("#dobber", [0, 9, 2, 15]), "down": {"texture": "#dobber", "uv": [0, 10, 2, 12]}}},
            {"from": [6.5, 15.5, 6.5], "to": [9.5, 17.5, 9.5],
             "faces": {**_alle_vlakken("#dobber", [0, 0, 3, 7]), "up": {"texture": "#dobber", "uv": [0, 0, 3, 3]},
                       "down": {"texture": "#dobber", "uv": [0, 4, 3, 7]}}},
            {"from": [7.5, 17.5, 7.5], "to": [8.5, 19, 8.5],
             "faces": {**_alle_vlakken("#dobber", [0, 0, 1, 2]), "up": {"texture": "#dobber", "uv": [0, 0, 1, 1]}}},
        ]}
    mod["vr_grondstof_vis_leeg"] = {"textures": {"particle": f"{T}rimpel"}, "elements": []}
    for s in SOORTEN:
        v = {}
        for tier in range(1, 7):
            tt = min(TIERS, tier)
            for vol in (True, False):
                w = "vol" if vol else "leeg"
                if s == "kruid":   # (the vanilla plant models themselves; 26.1 picks the cutout layer from the texture)
                    model = KRUID[tt][0 if vol else 1]
                elif s == "duik":
                    model = "minecraft:block/brain_coral" if vol else "minecraft:block/dead_brain_coral"
                elif s == "vis":
                    model = f"guhs:block/vr_grondstof_vis_{w}"
                else:
                    model = f"guhs:block/vr_grondstof_{s}_t{tt}_{w}"
                v[f"tier={tier},vol={'true' if vol else 'false'}"] = {"model": model}
        bs[f"vr_grondstof_{s}"] = {"variants": v}
    return mod, bs


def texturen():
    tex = {}
    for tier, naam in ERTS.items():
        if naam:
            bron = _png(naam)
        else:
            bron = (sterrengoud if tier == 6 else hemelzilver)(_png("ijzererts_knuffel"))
            tex[f"erts_t{tier}"] = bron
        # (T6: the vein mask comes from the iron ore it was painted on; the empty rock is rose sandstone)
        tex[f"erts_t{tier}_leeg"] = leeg_erts(_png("ijzererts_knuffel"), rozensteen(_png("knuffelsteen"))) if tier == 6 else leeg_erts(bron)
    tex["rimpel"] = rimpels()
    tex["dobber"] = dobber()
    return tex


def _assets():
    for naam, im in texturen().items():
        os.makedirs(TEX, exist_ok=True)
        im.save(os.path.join(TEX, f"{naam}.png"))
    mod, bs = modellen()
    for naam, m in mod.items():
        _schrijf(os.path.join(A, "models", "block", f"{naam}.json"), m)
    for naam, b in bs.items():
        _schrijf(os.path.join(A, "blockstates", f"{naam}.json"), b)
    return len(mod), len(bs)


def build(h):
    _assets()
    for k, nl in TEKSTEN.items():
        h.lang(k, EN[k], nl)   # (make_v2.lang(key, en, nl))


def _direct():
    n_mod, n_bs = _assets()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN if taal == "nl_nl" else EN)
        _schrijf(p, data)
    print(f"vadsrijk_grondstoffen: {n_mod} models, {n_bs} blockstates, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
