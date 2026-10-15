"""
einde2 phase B (CONTRACT_E2_B 7.5), slice h4_vuur: chapter 4 and the finale of the Guhvatar story: the capital of the
Vuurnatie in disguise, the school, the secret dance party, the turtle-ducks, the Dag van de Zwarte Zon, the cell under the
palace, the camp and the making-up, Het Kokend Rotsblok, the Drakendans, the Sintelstrand theatre, the White Lotus tent;
then the Top der Voorgangers, the Leeuwschildpad-guh, the airship fleet, the Agni Kai, the duel, the coronation and the
credits. Java: nl.juiced.guhs.feature.einde2.h4vuur (E2H4VuurFeature). Chunk c95_e2_h4_vuur.

  texts       einde2_h4_vuur_tekst.py: the two questlines, the narrator card, the 18 cutscenes, the chat of the minigames,
              the talks, the names (Dutch; English in tools/lang/en/c95_e2_h4_vuur.json)
  here        the questline texts and their advancements (verhaal_motor), the narrator card and its drawn map, the scene
              texts, the Guhvatar statue (block guhs:guhvatar_beeld: the plinth model, its textures, a loot table that keeps
              its owner), the airship (geckolib model + texture), the disguise and the glowing eyes (two 64 x 64 textures in
              the player-skin layout, client.LookLaag), the two guh clothes (hooks clothes/icons and the bone of the scar),
              the visible advancement "De Guhvatar", and the FTB sections "De Vuurnatie" and "De komeet van Mikai"

The build of the capital is einde2_bouw_vuur (this slice owns it in phase B): it got the markers miroh_cel, agni_kai_plein,
agni_kai_midden and luchtvloot_route for this story (CONTRACT_E2_B 4.1).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import einde2_h4_vuur_tekst as tekst
from features import verhaal_motor

FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
FTB_SECTIES = [
    ("einde2_h4_vuur", "De Vuurnatie", "npc:zuka_prins", [q[0] for q in tekst.FTB_VUUR]),
    ("einde2_h4_vuur_finale", "De komeet van Mikai", "npc:zuka_vuurheer", [q[0] for q in tekst.FTB_FINALE]),
]

VUUR, FINALE = "guhvatar_vuur", "guhvatar_finale"
SCENE = "einde2_h4_vuur_"
KAART = "einde2_h4_vuur"
BEELD = "guhvatar_beeld"
SCHIP = "einde2_h4_vuur_luchtschip"

# the scar of Zuka on a guh: a bone of its own over one eye, painted with the (existing) eyepatch swatch
BONES = {
    "outfit_zuka_litteken": ("head", [0, 6, -2], "eyepatch", [([-7.6, 6.4, -12.66], [7.0, 7.0, 0.3], 0)]),
}

ROOD, ROOD_DONKER, GOUD, ZWART = (178, 34, 30), (112, 18, 20), (236, 184, 54), (34, 26, 28)


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for key, text in tekst.TEKSTEN.items():
        if not key.startswith("advancements."):
            kit.lang(h, key, text)
    verhaal_motor.verhaallijn(h, VUUR, tekst.VUUR_NAAM, tekst.VUUR_UITLEG, tekst.VUUR_STAPPEN, klaar=tekst.VUUR_KLAAR,
                              extra=tekst.VUUR_EXTRA, kort=tekst.VUUR_KORT)
    verhaal_motor.verhaallijn(h, FINALE, tekst.FINALE_NAAM, tekst.FINALE_UITLEG, tekst.FINALE_STAPPEN, klaar=tekst.FINALE_KLAAR,
                              extra=tekst.FINALE_EXTRA, kort=tekst.FINALE_KORT)
    verhaal_motor.vertelkaart(h, KAART, tekst.KAART_TITEL, tekst.KAART_REGELS, kaart)
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, SCENE + naam, titel, regels, namen)
    verhaal_motor.sluier(h, "vuurnatie_hoofdstad")
    a = "advancements.guhs.guheinde.einde2_h4_vuur_guhvatar."
    kit.zichtbaar(h, "einde2_h4_vuur_guhvatar", "guheinde_winst", "guhs:guhvatar_beeld", "challenge", tekst.TEKSTEN[a + "title"],
                  tekst.TEKSTEN[a + "description"])


def kaart(k):
    """The drawn map of the narrator card: the capital in its crater, the harbour, the prison in its own crater, the ruin of
    the Zonnekrijgers, Sintelstrand, the camp of the Witte Lotus and the arena of pillars."""
    k.water([(0, 120), (60, 112), (120, 124), (190, 116), (256, 126), (256, 160), (0, 160)], kleur=(150, 160, 190, 255))
    k.land([(10, 30), (70, 14), (150, 10), (226, 24), (248, 70), (240, 112), (190, 122), (120, 118), (60, 120), (14, 98)],
           kleur=(150, 116, 96, 255))
    k.vulkaan(128, 66, hoog=30)
    k.toren(128, 52, hoog=16, kleur=(150, 40, 40, 255))
    k.huisje(110, 74, kleur=(200, 60, 50, 255))
    k.huisje(146, 74, kleur=(200, 60, 50, 255))
    k.vulkaan(196, 92, hoog=18)
    k.toren(196, 86, hoog=10, kleur=(80, 70, 80, 255))
    k.berg(40, 62, hoog=22)
    k.berg(58, 60, hoog=18)
    k.huisje(36, 98, kleur=(240, 200, 120, 255))
    k.huisje(224, 64, kleur=(244, 244, 236, 255))
    for x, y in ((48, 34), (56, 30), (62, 36), (52, 40)):
        k.toren(x, y, hoog=8, kleur=(110, 96, 90, 255))
    k.pad([(116, 112), (124, 96), (128, 80), (128, 70)])
    k.pad([(128, 80), (160, 92), (192, 96)])
    k.pad([(124, 96), (70, 86), (44, 72)])
    k.pad([(124, 100), (60, 104), (40, 100)])
    k.pad([(140, 88), (200, 72), (222, 66)])
    k.kruis(128, 70)
    k.kompasroos()


# =====================================================================================================================
# the Guhvatar statue: the plinth (the figure is drawn by the client), its textures and its loot
# =====================================================================================================================
def _ruis(w, h_, kleur, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((h_, w, 4), np.float32)
    a[..., :3] = kleur
    a[..., :3] += rng.normal(0, var, (h_, w, 1))
    a[..., 3] = 255
    return a


def beeld(h):
    A = h.A
    # the plinth's textures: white marble with a golden rim; the front has a small pair of guh ears carved in gold
    top = _ruis(16, 16, (236, 232, 226), 5, kit.seed("h4/beeld/top"))
    top[0, :, :3] = top[-1, :, :3] = top[:, 0, :3] = top[:, -1, :3] = GOUD
    zij = _ruis(16, 16, (228, 224, 218), 5, kit.seed("h4/beeld/zij"))
    zij[0:2, :, :3] = GOUD
    zij[-2:, :, :3] = (150, 130, 110)
    voor = zij.copy()
    for cx in (5.5, 10.5):                                            # the guh ears
        for y in range(5, 11):
            for x in range(16):
                if (x - cx) ** 2 + (y - 8) ** 2 <= 4.2:
                    voor[y, x, :3] = GOUD
    voor[9:12, 6:10, :3] = GOUD
    for naam, a in (("guhvatar_beeld_top", top), ("guhvatar_beeld_zij", zij), ("guhvatar_beeld_voor", voor)):
        h.save(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)), "block", f"{naam}.png")
    t = {"top": "guhs:block/guhvatar_beeld_top", "zij": "guhs:block/guhvatar_beeld_zij", "voor": "guhs:block/guhvatar_beeld_voor",
         "particle": "guhs:block/guhvatar_beeld_zij"}

    def doos(van, tot, voorkant):
        def vlak(tex, uv):
            return {"texture": tex, "uv": uv}
        x0, y0, z0 = van
        x1, y1, z1 = tot
        return {"from": van, "to": tot, "faces": {
            "up": vlak("#top", [x0, z0, x1, z1]), "down": vlak("#zij", [x0, z0, x1, z1]),
            "north": vlak("#voor" if voorkant else "#zij", [16 - x1, 16 - y1, 16 - x0, 16 - y0]),
            "south": vlak("#zij", [x0, 16 - y1, x1, 16 - y0]), "west": vlak("#zij", [z0, 16 - y1, z1, 16 - y0]),
            "east": vlak("#zij", [16 - z1, 16 - y1, 16 - z0, 16 - y0])}}
    h.w(f"{A}/models/block/{BEELD}.json", {"parent": "minecraft:block/block", "textures": t,
                                          "elements": [doos([0, 0, 0], [16, 2, 16], True), doos([1, 2, 1], [15, 5, 15], True)]})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{A}/blockstates/{BEELD}.json", {"variants": {f"facing={f},spiegel={s}": ({"model": f"guhs:block/{BEELD}", "y": r} if r else
                                                                                   {"model": f"guhs:block/{BEELD}"})
                                                       for f, r in rot.items() for s in ("false", "true")}})
    h.w(f"{A}/models/item/{BEELD}.json", {"parent": f"guhs:block/{BEELD}"})
    # the statue keeps whose it is when it is broken (the minecraft:profile of its block entity)
    h.w(f"{h.D}/loot_table/blocks/{BEELD}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}],
        "entries": [{"type": "minecraft:item", "name": f"guhs:{BEELD}", "functions": [
            {"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:profile"]}]}]}]})
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{BEELD}"])


# =====================================================================================================================
# the disguise and the glowing eyes: 64 x 64 in the layout of a player skin
# =====================================================================================================================
def _vlak(a, x0, y0, x1, y1, kleur, var=10, seed=0):
    rng = np.random.default_rng(seed)
    hgt, wdt = y1 - y0, x1 - x0
    blok = np.zeros((hgt, wdt, 4), np.float32)
    blok[..., :3] = kleur
    blok[..., :3] += rng.normal(0, var, (hgt, wdt, 1))
    blok[..., 3] = 255
    a[y0:y1, x0:x1] = np.clip(blok, 0, 255)


# (u, v, w, h, d) of each box of the player model: its six faces sit in the usual net
DELEN = {
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "right_arm": (40, 16, 4, 12, 4), "right_sleeve": (40, 32, 4, 12, 4),
    "left_arm": (32, 48, 4, 12, 4), "left_sleeve": (48, 48, 4, 12, 4),
    "right_leg": (0, 16, 4, 12, 4), "right_pants": (0, 32, 4, 12, 4),
    "left_leg": (16, 48, 4, 12, 4), "left_pants": (0, 48, 4, 12, 4),
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
}


def _zijden(deel):
    """The side faces (around the box, top to bottom) of a part: [(x0, y0, x1, y1), ...] and its top and bottom."""
    u, v, w, hgt, d = DELEN[deel]
    rondom = [(u, v + d, u + d, v + d + hgt), (u + d, v + d, u + d + w, v + d + hgt), (u + d + w, v + d, u + 2 * d + w, v + d + hgt),
              (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + hgt)]
    return rondom, (u + d, v, u + d + w, v + d), (u + d + w, v, u + d + 2 * w, v + d)


def vermomming():
    a = np.zeros((64, 64, 4), np.float32)
    s = kit.seed("h4/vermomming")
    # the tunic: body, a short sleeve on each arm (the hands stay the player's), a golden collar and a black sash
    for deel in ("body", "jacket"):
        rondom, boven, onder = _zijden(deel)
        for i, (x0, y0, x1, y1) in enumerate(rondom):
            _vlak(a, x0, y0, x1, y1, ROOD if deel == "body" else ROOD_DONKER, 9, s + i)
            if deel == "jacket":
                a[y0:y1, x0:x1, 3] = 0                                         # the jacket layer: only its trims
                a[y0:y0 + 1, x0:x1] = (*GOUD, 255)                               # the collar
                a[y0 + 8:y0 + 10, x0:x1] = (*ZWART, 255)                         # the sash
            else:
                a[y0 + 8:y0 + 10, x0:x1, :3] = ZWART
                a[y1 - 1:y1, x0:x1, :3] = GOUD                                   # the hem
        _vlak(a, *boven, ROOD, 6, s + 9)
        if deel == "jacket":
            a[boven[1]:boven[3], boven[0]:boven[2], 3] = 0
    # the golden V of the collar on the front of the tunic, and the buckle of the sash
    fx0, fy0 = 20, 20
    for k in range(4):
        a[fy0 + k, fx0 + 3 - k, :3] = GOUD
        a[fy0 + k, fx0 + 4 + k, :3] = GOUD
    a[fy0 + 8:fy0 + 10, fx0 + 3:fx0 + 5, :3] = GOUD
    for deel in ("right_arm", "left_arm"):
        rondom, boven, onder = _zijden(deel)
        for i, (x0, y0, x1, y1) in enumerate(rondom):
            _vlak(a, x0, y0, x1, y0 + 7, ROOD, 9, s + 20 + i)
            a[y0 + 6:y0 + 7, x0:x1, :3] = GOUD                                    # the cuff
        _vlak(a, *boven, ROOD, 6, s + 29)
    # dark trousers and boots
    for deel in ("right_leg", "left_leg"):
        rondom, boven, onder = _zijden(deel)
        for i, (x0, y0, x1, y1) in enumerate(rondom):
            _vlak(a, x0, y0, x1, y1, (58, 40, 40), 7, s + 40 + i)
            a[y1 - 4:y1, x0:x1, :3] = ZWART                                       # the boots
            a[y1 - 4:y1 - 3, x0:x1, :3] = (90, 70, 40)
        _vlak(a, *boven, (58, 40, 40), 5, s + 49)
        _vlak(a, *onder, ZWART, 3, s + 50)
    # the head: only the hat layer: a thin red headband (it hides the ears, more or less) and a topknot with a golden clip
    rondom, boven, onder = _zijden("hat")
    for x0, y0, x1, y1 in rondom:
        a[y0 + 1:y0 + 2, x0:x1] = (*ROOD, 255)
        a[y0 + 2:y0 + 3, x0:x1] = (*ROOD_DONKER, 255)
    bx0, by0 = boven[0], boven[1]
    a[by0 + 3:by0 + 6, bx0 + 3:bx0 + 5] = (*ZWART, 255)
    a[by0 + 4, bx0 + 3:bx0 + 5] = (*GOUD, 255)
    return np.clip(a, 0, 255).astype(np.uint8)


def ogen():
    """The glowing eyes of the Guhvatar state on the head's front face (a lit row where most skins have their eyes)."""
    a = np.zeros((64, 64, 4), np.uint8)
    for x in (9, 10, 13, 14):
        a[12, x] = (225, 246, 255, 255)
    for x in (9, 10, 13, 14):
        a[11, x] = (150, 220, 255, 140)
        a[13, x] = (150, 220, 255, 110)
    return a


# =====================================================================================================================
# the airship: a small geckolib model (drawn 2.5 times its size by the renderer) on its own atlas
# =====================================================================================================================
class _Atlas:
    def __init__(self, maat, schaal, naam):
        self.maat, self.s = maat, schaal
        self.img = np.zeros((maat * schaal, maat * schaal, 4), np.uint8)
        self.x = self.y = self.rij = 0
        self.rng = kit.rng(naam)

    def vak(self, w, hgt, verf):
        w, hgt = int(math.ceil(w)), int(math.ceil(hgt))
        if self.x + w > self.maat:
            self.x, self.y, self.rij = 0, self.y + self.rij, 0
        if self.y + hgt > self.maat:
            raise SystemExit("einde2_h4_vuur: the airship atlas is full")
        u, v = self.x, self.y
        self.x += w
        self.rij = max(self.rij, hgt)
        S = self.s
        self.img[v * S:(v + hgt) * S, u * S:(u + w) * S] = verf(w * S, hgt * S, self.rng)
        return {"uv": [u, v], "uv_size": [w, hgt]}


def _effen(kleur, var=8):
    def verf(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = kleur
        a[..., :3] += rng.normal(0, var, (H, W, 1))
        a[..., 3] = 255
        return np.clip(a, 0, 255).astype(np.uint8)
    return verf


def _romp(kleur):
    """The red hull with dark seams of plates, a row of round windows and the flame of the Vuurnatie in the middle."""
    def verf(W, H, rng):
        a = _effen(kleur, 7)(W, H, rng).astype(np.float32)
        for x in range(0, W, 12):
            a[:, x:x + 1, :3] *= 0.6
        a[H // 4:H // 4 + 1, :, :3] *= 0.7
        for x in range(6, W - 4, 10):
            yy = H // 2
            a[yy - 1:yy + 2, x:x + 3, :3] = (250, 200, 90)
        cx, cy = W // 2, int(H * 0.72)
        for y in range(H):
            for x in range(W):
                dx, dy = (x - cx) / max(1, H * 0.22), (y - cy) / max(1, H * 0.22)
                if dx * dx + dy * dy < 1 and dy > -0.9 + abs(dx) * 0.3:
                    a[y, x, :3] = GOUD
        return np.clip(a, 0, 255).astype(np.uint8)
    return verf


def _kubus(atlas, origin, size, verf, over=None):
    faces = {}
    sx, sy, sz = size
    maten = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    for f, (w, hgt) in maten.items():
        faces[f] = atlas.vak(max(w, 1), max(hgt, 1), (over or {}).get(f, verf))
    return {"origin": list(origin), "size": list(size), "uv": faces}


def luchtschip():
    at = _Atlas(128, 2, "h4/luchtschip")
    rood, donker, zwart, staal = _effen(ROOD, 8), _effen(ROOD_DONKER, 6), _effen(ZWART, 4), _effen((90, 86, 92), 6)
    romp_zij = _romp(ROOD)
    bones = [
        {"name": "schip", "pivot": [0, 0, 0], "cubes": [
            _kubus(at, [-6, 6, -24], [12, 10, 48], rood, {"east": romp_zij, "west": romp_zij}),       # the hull
            _kubus(at, [-4, 4, -20], [8, 2, 40], donker),                                              # its belly
            _kubus(at, [-2, 2, -16], [4, 2, 32], zwart),                                                # the keel
            _kubus(at, [-3, 8, -28], [6, 6, 4], staal),                                                 # the ram
            _kubus(at, [-4, 16, -6], [8, 6, 12], donker),                                               # the bridge
            _kubus(at, [-3, 22, -4], [6, 1, 8], zwart),
            _kubus(at, [-1, 10, 24], [2, 12, 8], zwart),                                                # the tail fin
            _kubus(at, [-10, 10, 24], [20, 2, 6], donker),                                              # the side fins
        ]},
        {"name": "schoorstenen", "parent": "schip", "pivot": [0, 16, 0], "cubes": [
            _kubus(at, [-3, 16, 8], [2, 7, 2], zwart), _kubus(at, [1, 16, 8], [2, 7, 2], zwart),
        ]},
        {"name": "vlaggen", "parent": "schip", "pivot": [0, 22, 0], "cubes": [
            _kubus(at, [-0.5, 23, -2], [1, 6, 1], staal), _kubus(at, [0, 26, -1], [0.2, 3, 4], _effen(GOUD, 6)),
        ]},
    ]
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{SCHIP}", "texture_width": at.maat, "texture_height": at.maat,
                        "visible_bounds_width": 6, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}
    return geo, at.img


def modellen(h):
    A = h.A
    geo, img = luchtschip()
    h.w(f"{A}/geckolib/models/entity/{SCHIP}.geo.json", geo)
    h.w(f"{A}/geckolib/animations/entity/{SCHIP}.animation.json", {"format_version": "1.8.0", "animations": {}})
    h.save(Image.fromarray(img), "entity", f"{SCHIP}.png")
    h.save(Image.fromarray(vermomming()), "entity", "einde2_h4_vuur_vermomming.png")
    h.save(Image.fromarray(ogen()), "entity", "einde2_h4_vuur_ogen.png")


# =====================================================================================================================
# the two guh clothes (kleding hooks: textures per swatch, and the icons)
# =====================================================================================================================
def clothes(rng, v):
    r = kit.rng("h4_vuur/kleding")

    def tuniek():
        a = v.fabric(ROOD, r, 10).astype(np.float32)
        a[0:4, :] = GOUD                                                  # the golden collar
        a[15:19, :] = ZWART                                               # the sash
        a[15:19, 14:18] = GOUD
        a[29:32, :] = GOUD                                                # the hem
        return np.clip(a, 0, 255)

    def litteken():
        a = np.zeros((32, 32, 4), np.float32)
        rng2 = np.random.default_rng(kit.seed("h4_vuur/litteken"))
        for y in range(32):
            for x in range(32):
                d = ((x - 15.5) / 15.0) ** 2 + ((y - 15.5) / 13.0) ** 2 + rng2.normal(0, 0.06)
                if d < 1.0:
                    rood = 1.0 - d
                    a[y, x, :3] = (206 - 60 * rood, 104 - 40 * rood, 96 - 30 * rood)
                    a[y, x, 3] = 255 if d < 0.85 else 160
        for y in range(5, 27, 6):                                         # the ridges of the old burn
            a[y:y + 1, 6:26, :3] *= 0.8
        return np.clip(a, 0, 255)
    return {"vuurnatie_vermomming": {"suit": tuniek}, "zuka_litteken": {"eyepatch": litteken}}


LITTEKEN_ICON = ["................", "................", "....aaaaaaa.....", "...abbbbbbba....", "..abbcbbbcbba...", "..abcbbcbbbba...",
                 "..abbbcbbcbba...", "..abbcbbbbcba...", "..abbbbcbbbba...", "...abbbbbbba....", "....aaaaaaa.....", "................",
                 "................", "................", "................", "................"]


def icons(ic):
    return {
        "vuurnatie_vermomming": ic.shirt(ROOD, ROOD_DONKER, GOUD, "buttons"),
        "zuka_litteken": ic.icon(LITTEKEN_ICON, {"a": (120, 50, 44), "b": (206, 112, 100), "c": (150, 64, 58)}),
    }


# =====================================================================================================================
# FTB: two sections in the Guheinde chapter, one chain in order (FTB_LINEAIR) after the chapters before
# =====================================================================================================================
def ftb(fq):
    alle = tekst.FTB_VUUR + tekst.FTB_FINALE
    for i, (key, titel, text, icon, adv) in enumerate(alle):
        laatste = i == len(alle) - 1
        eerste_finale = key == tekst.FTB_FINALE[0][0]
        fq.q(key, titel, text, icon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 32 if laatste else 8),),
             shape="gear" if i == 0 or eerste_finale or laatste else None, xp=500 if laatste else 0)


# =====================================================================================================================
def build(h):
    texts(h)
    beeld(h)
    modellen(h)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        if f"scene.guhs.{SCENE}{naam}.titel" not in h.NL:
            problems.append(f"scene {naam}: no title")
    for path in (f"{h.A}/models/block/{BEELD}.json", f"{h.A}/blockstates/{BEELD}.json", f"{h.D}/loot_table/blocks/{BEELD}.json",
                 f"{h.A}/geckolib/models/entity/{SCHIP}.geo.json", os.path.join(h.TEX, "entity", f"{SCHIP}.png"),
                 os.path.join(h.TEX, "entity", "einde2_h4_vuur_vermomming.png"), os.path.join(h.TEX, "entity", "einde2_h4_vuur_ogen.png")):
        if not os.path.exists(path):
            problems.append(f"missing {path}")
    for key in ("block.guhs.guhvatar_beeld", "entity.guhs.einde2_h4_vuur_luchtschip", "entity.guhs.einde2_h4_vuur_vuurbal", "gui.guhs.titel.guhvatar",
                "item.guhs.vuurnatie_vermomming", "item.guhs.zuka_litteken", f"gui.guhs.verhalen.{VUUR}.naam", f"gui.guhs.verhalen.{FINALE}.naam"):
        if key not in h.NL:
            problems.append(f"no text {key}")
    if len(tekst.VUUR_STAPPEN) != 12 or len(tekst.FINALE_STAPPEN) != 7:
        problems.append("the step counts differ from E2H4Vuur (12 and 7)")
    if problems:
        raise SystemExit("einde2_h4_vuur self-check: " + "; ".join(problems))
