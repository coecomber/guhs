"""
einde2 phase B (CONTRACT_E2_B 7.4), slice h3_aarde: chapter 3 of the Guhvatar story, Ba Sing Guh (Jasmijn Draak, Appa's lost
days, Lu Ten, the fall). Java: nl.juiced.guhs.feature.einde2.h3aarde (E2H3AardeFeature and its classes).

  - the questline guhvatar_aarde (fifteen steps) with its hidden advancements, the narrator card (a drawn map of the city),
    the texts of the eleven scenes (their cameras are markers of the build: einde2_bouw_aarde_verhaal.py), every line of
    the roles (einde2_h3_aarde_tekst.py);
  - the three things you take home: the Pai Sho table, Oom Miroh's theeset, the koolman's stall (models, textures, loot);
  - the Pai Sho board of the screen (textures/gui/einde2_h3_aarde/pai_sho.png);
  - the stall the story shows one player (data/guhs/einde2/h3_aarde/koolkraam.json: whole and wrecked, from the same builder
    as the build's overlay templates);
  - the clothes: the Aarderijk hat and the belt of De Blinde Bandiet (BONES, clothes(), icons() for make_guh_variants and
    make_clothes_icons);
  - the FTB section "Hoofdstuk 3: Ba Sing Guh" (FTB_LINEAIR: one quest per step).
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import einde2_bouw_aarde_bouw as bouw
from features import einde2_h3_aarde_tekst as tekst
from features import verhaal_motor

LIJN = "guhvatar_aarde"
SLICE = "einde2_h3_aarde"
STRUCTUUR = "ba_sing_guh"
SCENE = "einde2_h3_aarde_"

FTB_CHAPTER = "guhs_guheinde"
FTB_LINEAIR = True
FTB_SECTIES = [("einde2_h3_aarde", "Hoofdstuk 3: Ba Sing Guh", "npc:toph_guh", None)]

BLOKKEN = ["pai_sho_tafel", "theeset", "einde2_h3_aarde_koolkraam"]
# the two clothes pieces (make_resources writes their item models from this list)
CLOTHES = ["aarderijk_hoed", "blinde_bandiet_riem"]

# =====================================================================================================================
# clothes (make_guh_variants: BONES + clothes(); make_clothes_icons: icons()). Existing swatch names only (the sheet is
# full, CONTRACT_E2_B 7.1): rain_hat, sash, zwemband.
# =====================================================================================================================
_H, _B = [0, 6, -2], [0, 6, 6]       # head and body pivots of the guh model
BONES = {
    # the Aarderijk hat: a wide straw hat in steps (a little pagoda) over the ears, a green band round its foot
    "outfit_aarderijk_hoed": ("head", _H, "rain_hat", [([-7.5, 15.2, -13.5], [15, 0.6, 15], 0), ([-5.5, 15.8, -11.5], [11, 0.8, 11], 0),
                                                        ([-3.5, 16.6, -9.5], [7, 0.8, 7], 0), ([-1.8, 17.4, -7.8], [3.6, 0.8, 3.6], 0),
                                                        ([-0.6, 18.2, -6.6], [1.2, 0.9, 1.2], 0)]),
    "outfit_aarderijk_hoed_band": ("head", _H, "sash", [([-5.7, 15.75, -11.7], [11.4, 0.35, 11.4], 0)]),
    # the belt of De Blinde Bandiet: a band round the belly (the sides and the back, like the swim ring) and a big golden
    # buckle on both flanks
    "outfit_bandiet_riem": ("body", _B, "zwemband", [([6.4, 4.0, -2.4], [0.7, 2.2, 13.4], 0), ([-7.1, 4.0, -2.4], [0.7, 2.2, 13.4], 0),
                                                       ([-7.1, 4.0, 10.6], [14.2, 2.2, 0.7], 0)]),
    "outfit_bandiet_gesp": ("body", _B, "sash", [([7.1, 3.4, 1.6], [0.6, 3.4, 5.0], 0), ([-7.7, 3.4, 1.6], [0.6, 3.4, 5.0], 0)]),
}
STRO, STRO_DONKER, GROEN_BAND, GOUD, LEER = (222, 196, 120), (172, 140, 70), (60, 132, 70), (240, 196, 70), (92, 60, 38)
HOED_ICON = ["................", "................", ".......kk.......", "......kssk......", ".....kssssk.....", "....kssssssk....",
             "...kggggggggk...", "..kssssssssssk..", ".ksssssssssssssk", "kkkkkkkkkkkkkkkk", "................", "................",
             "................", "................", "................", "................"]
RIEM_ICON = ["................", "................", "................", "................", "llllllllllllllll", "LlLlLlyyyyLlLlLl",
             "llllllyGGyllllll", "LlLlLlyGGyLlLlLl", "llllllyyyyllllll", "LlLlLlLlLlLlLlLl", "llllllllllllllll", "................",
             "................", "................", "................", "................"]


def clothes(rng, v):
    """The textures of the two pieces (own rng from the name: the hook's rng shifts with the module's place in FEATURES)."""
    r = kit.rng("h3_aarde/kleding")
    px = 32

    def stro():
        a = v.fabric(STRO, r, 10)
        for y in range(0, px, 3):                                 # the weave
            a[y:y + 1, :] = np.clip(a[y:y + 1, :] * 0.86, 0, 255)
        for x in range(0, px, 5):
            a[:, x:x + 1] = np.clip(a[:, x:x + 1] * 0.92, 0, 255)
        return np.clip(a, 0, 255)

    def band():
        a = v.fabric(GROEN_BAND, r, 6)
        a[14:18, :] = GOUD                                        # a thin golden thread
        return np.clip(a, 0, 255)

    def leer():
        a = v.fabric(LEER, r, 8)
        a[0:2, :] = (60, 38, 24)
        a[-2:, :] = (60, 38, 24)
        for x in range(3, px, 7):                                 # golden studs
            a[14:18, x:x + 3] = GOUD
        return np.clip(a, 0, 255)

    def gesp():
        a = v.fabric(GOUD, r, 10)
        cy, cx = 16, 16
        for y in range(px):
            for x in range(px):
                d = math.hypot(x - cx, y - cy)
                if 9 <= d <= 11:
                    a[y, x] = (176, 128, 30)                      # the round rim of the aardemunt
                if abs(x - cx) <= 3 and abs(y - cy) <= 3:
                    a[y, x] = (70, 150, 70)                       # its square hole, green: the Aarderijk
        return np.clip(a, 0, 255)
    return {
        "aarderijk_hoed": {"rain_hat": stro, "sash": band},
        "blinde_bandiet_riem": {"zwemband": leer, "sash": gesp},
    }


def icons(ic):
    return {
        "aarderijk_hoed": ic.icon(HOED_ICON, {"k": (110, 84, 40), "s": STRO, "g": GROEN_BAND}),
        "blinde_bandiet_riem": ic.icon(RIEM_ICON, {"l": LEER, "L": (70, 46, 30), "y": GOUD, "G": (70, 150, 70)}),
    }


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    verhaal_motor.verhaallijn(h, LIJN, tekst.LIJN_NAAM, tekst.LIJN_UITLEG, stappen=tekst.STAPPEN, klaar=tekst.KLAAR, extra=tekst.EXTRA,
                              kort=tekst.KORT)
    for i, naam in enumerate(tekst.DOELEN):
        kit.lang(h, f"gui.guhs.verhalen.{LIJN}.doel.{i}", naam)
    verhaal_motor.vertelkaart(h, SLICE, tekst.KAART_TITEL, tekst.KAART_REGELS, kaart())
    for naam, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, SCENE + naam, titel, regels, namen=namen)
    for key, text in tekst.QUEST.items():
        kit.lang(h, f"quest.guhs.einde2_h3_aarde.{key}", text)
    for key, text in tekst.GUI.items():
        kit.lang(h, f"gui.guhs.einde2_h3_aarde.{key}", text)
    for key, text in tekst.BLOKKEN.items():
        kit.lang(h, key, text)
    verhaal_motor.sluier(h, STRUCTUUR)


def kaart():
    """The narrator card: Ba Sing Guh from above: the round slab, three rings of walls, the lake in the east, the palace,
    the landing lobe with the camp and the little hill with its tree, the way from the gate to the tea house."""
    k = verhaal_motor.Kaart(seed=2140310)
    cx, cy = 140, 82
    k.land([(cx + int(70 * math.cos(math.radians(a))), cy + int(66 * math.sin(math.radians(a)))) for a in range(0, 360, 12)], (206, 178, 120, 255))
    k.land([(18, 70), (40, 64), (52, 72), (52, 96), (40, 102), (18, 96)], (206, 178, 120, 255))     # the landing lobe
    for r, kleur in ((64, (150, 128, 96, 255)), (44, (122, 152, 104, 255)), (26, (186, 160, 92, 255))):
        k.d.ellipse((cx - r, cy - int(r * 0.94), cx + r, cy + int(r * 0.94)), outline=k.INKT, fill=kleur)
    k.water([(186, 66), (200, 60), (208, 74), (204, 96), (190, 100), (182, 84)])                    # Meer Laogai
    k.toren(cx, cy + 6, 16, (214, 176, 70, 255))                                                    # the palace
    k.huisje(112, 100, (90, 150, 90, 255))                                                          # De Jasmijn Draak
    k.berg(30, 74, 9, (122, 152, 104, 255))                                                         # the hill
    k.boom(30, 66, (232, 150, 190, 255))                                                            # its cherry tree
    k.pad([(44, 84), (74, 82), (96, 84), (112, 96)])
    k.kruis(70, 82)
    k.huisje(36, 94, (200, 120, 80, 255))                                                           # the camp
    k.kompasroos(236, 24)
    return k


# =====================================================================================================================
# the three blocks
# =====================================================================================================================
def _vlak(kleur, var, seed, size=16):
    r = kit.rng(f"h3_aarde/{seed}")
    a = np.zeros((size, size, 4), np.uint8)
    ruis = r.integers(-var, var + 1, (size, size, 1))
    a[..., :3] = np.clip(np.array(kleur)[None, None, :] + ruis, 0, 255)
    a[..., 3] = 255
    return a


def textures(h):
    # the table: a board of red and cream with a lotus in the middle; dark wood with a golden edge
    a = _vlak((222, 206, 170), 6, "tafel_top")
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.6:
                a[y, x, :3] = (92, 52, 34)
            elif ((x < 8) == (y < 8)):
                a[y, x, :3] = (182, 66, 54) if d > 3 else (240, 230, 206)
            if 7.0 < d <= 7.6:
                a[y, x, :3] = (214, 170, 70)
    for x, y in ((7, 5), (8, 5), (5, 7), (5, 8), (10, 7), (10, 8), (7, 10), (8, 10)):
        a[y, x, :3] = (236, 150, 186)                                  # the white lotus in the middle, pink petals
    a[7:9, 7:9, :3] = (250, 214, 90)
    h.save(Image.fromarray(a), "block", "pai_sho_tafel_top.png")
    a = _vlak((84, 50, 34), 8, "tafel_zij")
    a[0:2, :, :3] = (214, 170, 70)
    h.save(Image.fromarray(a), "block", "pai_sho_tafel_zij.png")
    # the theeset: white porcelain with a jade band; a tray of bamboo
    a = _vlak((238, 238, 230), 5, "thee_porselein")
    a[6:9, :, :3] = (90, 170, 130)
    a[7, ::3, :3] = (240, 220, 120)
    h.save(Image.fromarray(a), "block", "theeset.png")
    a = _vlak((176, 150, 90), 8, "thee_blad")
    for x in range(0, 16, 4):
        a[:, x, :3] = (140, 116, 64)
    h.save(Image.fromarray(a), "block", "theeset_blad.png")
    # the stall: planks and a striped awning
    a = _vlak((150, 112, 70), 10, "kraam_hout")
    for y in range(0, 16, 4):
        a[y, :, :3] = (110, 80, 50)
    h.save(Image.fromarray(a), "block", "einde2_h3_aarde_koolkraam.png")
    a = _vlak((120, 190, 80), 6, "kraam_doek")
    for x in range(16):
        if (x // 4) % 2:
            a[:, x, :3] = (232, 236, 220)
    h.save(Image.fromarray(a), "block", "einde2_h3_aarde_koolkraam_doek.png")


def el(van, tot, tex, faces=None, uv_top=None):
    """A model element with the same texture on every face (uv from its own size: no stretching)."""
    (x0, y0, z0), (x1, y1, z1) = van, tot
    fs = {}
    for f in faces or ("north", "south", "east", "west", "up", "down"):
        if f in ("north", "south"):
            uv = [x0, 16 - y1, x1, 16 - y0]
        elif f in ("east", "west"):
            uv = [z0, 16 - y1, z1, 16 - y0]
        else:
            uv = uv_top or [x0, z0, x1, z1]
        fs[f] = {"uv": [max(0, min(16, q)) for q in uv], "texture": tex}
    return {"from": list(van), "to": list(tot), "faces": fs}


def blok_model(h, name, elements, textures_, particle):
    h.w(f"{h.A}/models/block/{name}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                            "textures": dict(textures_, particle=particle), "elements": elements})
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{name}", "y": y}
                                                        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    h.w(f"{h.A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})


def blocks_and_items(h):
    t = lambda n: f"guhs:block/{n}"  # noqa: E731
    # the Pai Sho table: a thick round-ish top on four legs, two tiles on the board
    tafel = [el((0, 9, 0), (16, 12, 16), "#zij")]
    tafel[0]["faces"]["up"] = {"uv": [0, 0, 16, 16], "texture": "#top"}
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        tafel.append(el((x, 0, z), (x + 2, 9, z + 2), "#zij"))
    tafel.append(el((4, 12, 4), (6, 12.6, 6), "#tegel_wit"))
    tafel.append(el((10, 12, 9), (12, 12.6, 11), "#tegel_rood"))
    blok_model(h, "pai_sho_tafel", tafel, {"top": t("pai_sho_tafel_top"), "zij": t("pai_sho_tafel_zij"), "tegel_wit": "minecraft:block/white_terracotta",
                                           "tegel_rood": "minecraft:block/red_terracotta"}, t("pai_sho_tafel_zij"))
    # the theeset: a tray, the pot with its lid and spout, four cups
    thee = [el((2, 0, 2), (14, 1, 14), "#blad"),
            el((5, 1, 5), (11, 6, 11), "#porselein"), el((6, 6, 6), (10, 7, 10), "#porselein"), el((7.5, 7, 7.5), (8.5, 8, 8.5), "#jade"),
            el((11, 2.5, 7.5), (13, 4, 8.5), "#porselein"), el((3, 3, 7.5), (5, 5, 8.5), "#jade")]
    for x, z in ((2.5, 2.5), (11.5, 2.5), (2.5, 11.5), (11.5, 11.5)):
        thee.append(el((x, 1, z), (x + 2, 3, z + 2), "#porselein"))
    blok_model(h, "theeset", thee, {"blad": t("theeset_blad"), "porselein": t("theeset"), "jade": "minecraft:block/prismarine_bricks"}, t("theeset"))
    # the koolman's stall: a counter, three cabbages, two posts and a striped awning
    kraam = [el((0, 0, 3), (16, 8, 13), "#hout"),
             el((1, 8, 4), (5, 11, 8), "#kool"), el((6, 8, 5), (10, 11.5, 9), "#kool"), el((11, 8, 4), (15, 11, 8), "#kool"),
             el((0, 8, 10), (1.5, 15, 11.5), "#hout"), el((14.5, 8, 10), (16, 15, 11.5), "#hout"),
             el((-1, 15, 1), (17, 16, 12), "#doek")]
    blok_model(h, "einde2_h3_aarde_koolkraam", kraam, {"hout": t("einde2_h3_aarde_koolkraam"), "kool": "guhs:block/einde2_bouw_aarde_kool",
                                                       "doek": t("einde2_h3_aarde_koolkraam_doek")}, t("einde2_h3_aarde_koolkraam"))
    for name in BLOKKEN:
        h.self_drop(name)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:pai_sho_tafel", "guhs:einde2_h3_aarde_koolkraam"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:theeset"])


# =====================================================================================================================
# the Pai Sho board of the screen
# =====================================================================================================================
def pai_sho_gui(h):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = kit.rng("h3_aarde/pai_sho_gui")
    # the board: dark wood, the round field of the White Lotus in four quarters, the 5 x 5 squares
    hout = np.zeros((176, 176, 4), np.uint8)
    hout[..., :3] = np.clip(np.array((96, 56, 36)) + r.integers(-10, 11, (176, 176, 1)), 0, 255)
    for y in range(0, 176, 6):
        hout[y, :, :3] = np.clip(hout[y, :, :3].astype(int) - 18, 0, 255)
    hout[..., 3] = 255
    img.paste(Image.fromarray(hout), (0, 0))
    d.ellipse((10, 10, 166, 166), fill=(236, 222, 190, 255), outline=(214, 170, 70, 255), width=3)
    for (x0, y0, x1, y1) in ((18, 18, 88, 88), (88, 88, 158, 158)):
        d.pieslice((10, 10, 166, 166), 180 if x0 == 18 else 0, 270 if x0 == 18 else 90, fill=(190, 72, 58, 255))
    d.ellipse((10, 10, 166, 166), outline=(214, 170, 70, 255), width=3)
    for i in range(6):
        p = 18 + i * 28
        d.line((p, 18, p, 158), fill=(70, 40, 26, 255), width=2)
        d.line((18, p, 158, p), fill=(70, 40, 26, 255), width=2)
    d.ellipse((80, 80, 96, 96), outline=(214, 170, 70, 255), width=2)                    # the garden in the middle
    # the tiles (28 x 28 at x 176): the white lotus (with two tiny guh ears on its rim), the red lotus, the glow of a win
    def tegel(y0, schijf, rand, blad, hart, oortjes):
        d.ellipse((179, y0 + 3, 201, y0 + 25), fill=schijf, outline=rand, width=2)
        if oortjes:
            d.polygon([(183, y0 + 7), (185, y0 + 1), (189, y0 + 4)], fill=rand)
            d.polygon([(197, y0 + 7), (195, y0 + 1), (191, y0 + 4)], fill=rand)
        for a in range(0, 360, 72):
            x = 190 + 5 * math.cos(math.radians(a - 90))
            y = y0 + 14 + 5 * math.sin(math.radians(a - 90))
            d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=blad)
        d.ellipse((187, y0 + 11, 193, y0 + 17), fill=hart)
    tegel(0, (246, 240, 226, 255), (150, 120, 90, 255), (240, 150, 186, 255), (250, 214, 90, 255), True)
    tegel(28, (186, 50, 46, 255), (110, 30, 26, 255), (246, 196, 80, 255), (250, 240, 210, 255), False)
    d.ellipse((177, 57, 203, 83), outline=(255, 214, 90, 200), width=3)
    h.save(img, "gui", "einde2_h3_aarde", "pai_sho.png")


# =====================================================================================================================
# the stall the story shows one player (Java: H3Schijn)
# =====================================================================================================================
def koolkraam(h):
    out = {}
    for naam, heel in (("heel", True), ("kapot", False)):
        cellen = []

        def zet(x, y, z, n, p):
            state = {"Name": n if ":" in n else "minecraft:" + n}
            if p:
                state["Properties"] = {k: str(v) for k, v in p.items()}
            cellen.append({"x": x, "y": y, "z": z, "state": state})
        bouw.kraam(zet, 0, 0, 0, heel=heel)
        out[naam] = sorted(cellen, key=lambda c: (c["y"], c["x"], c["z"]))
    h.w(f"{h.D}/einde2/h3_aarde/koolkraam.json", out)


# =====================================================================================================================
# FTB: one quest per step (FTB_LINEAIR: a chain in this order)
# =====================================================================================================================
FTB = [
    ("aankomst", "Ba Sing Guh", "Na de Waterstam vliegt Appa-guh je naar het Aarderijk. Land op de landingslob voor de reusachtige buitenmuur van "
                                "&6Ba Sing Guh&r. Het verhaal begint vanzelf.", "guhs:einde2_bouw_aarde_stadssteen", 1),
    ("loket", "Paspoort, alstublieft", "Niemand komt binnen zonder stempel. Praat met de &dstrenge loketguh&r in het poortgebouw en beantwoord zijn "
                                       "vragen. Elk antwoord is goed. Echt.", "minecraft:paper", 2),
    ("aardrommel", "De Aardrommel", "In de onderring wordt gevochten. Niemand raakt gewond, alleen een beetje gekiezeld. Ga naar de arena in het "
                                    "zuidwesten.", "guhs:einde2_bouw_aarde_aardemunt", 3),
    ("blinddoek", "Zien met je voeten", "&dToph-guh&r, De Blinde Bandiet, leert je aardesturen: met je ogen dicht. Vind in haar oefenbaan de zes "
                                        "aardemunten. Bukken is stampen.", "minecraft:sculk_sensor", 4),
    ("monorail", "De monorail", "Neem de monorail van de buitenmuur naar de middenring. Elke sprong is een duw met aardesturen.",
     "guhs:einde2_bouw_aarde_spoor", 5),
    ("theehuis", "De Jasmijn Draak", "Ergens in de middenring ruikt het naar jasmijn. Zoek het theehuis van &dOom Miroh&r. De ober is... chagrijnig.",
     "guhs:theepotje", 6),
    ("thee", "Geduld in een kopje", "Zet thee bij de grote ketel: water, blaadjes, schenken. Telkens precies op tijd klikken.", "guhs:theeset", 7),
    ("pai_sho", "Een potje Pai Sho", "Om de beurt een tegel. Wie eerst een lotus legt, wint. Of je wint of niet: Oom Miroh heeft iets te zeggen.",
     "guhs:pai_sho_tafel", 8),
    ("joo_dee", "Geen knabbeltekort", "&dJoo Dee-guh&r laat je de stad zien. Ze glimlacht heel veel.", "minecraft:written_book", 9),
    ("kamp", "Appa's verloren dagen", "Ga terug naar het kamp. Iets klopt er niet.", "guhs:appa_fluitje", 10),
    ("koning", "De Aardekoning", "Misschien weet de &dAardekoning-guh&r iets. Of zijn beer Bosco.", "minecraft:honeycomb", 11),
    ("laogai", "Meer Laogai", "Sluip de basis onder het meer binnen. Gezien worden = terug naar de hal, verder niets. Bukken helpt.",
     "minecraft:iron_bars", 12),
    ("lu_ten", "Een lied voor Lu Ten", "Oom Miroh is niet in zijn theehuis. Zoek de heuvel met de kersenboom.", "minecraft:cherry_sapling", 13),
    ("val", "De val van Ba Sing Guh", "De Dai Li hebben Katara-guh meegenomen naar de kristalcatacomben onder het paleis.",
     "guhs:einde2_bouw_aarde_groen_kristal", 14),
    ("toph", "Een nieuw teamlid", "Bij het kamp wacht iemand met haar voeten in het gras. Daarna: op naar de Vuurnatie.", "minecraft:mud_bricks", 15),
]


def ftb(fq):
    q, adv = fq.q, fq.adv
    for i, (naam, titel, tekst_, icon, stap) in enumerate(FTB):
        q(f"einde2_h3_aarde_{naam}", titel, tekst_, icon, [adv(f"{LIJN}_stap_{stap}")], shape="gear" if i in (0, len(FTB) - 1) else None)


# =====================================================================================================================
def selfcheck(h):
    problems = []
    for key in list(tekst.QUEST):
        if f"quest.guhs.einde2_h3_aarde.{key}" not in h.NL:
            problems.append(f"no text quest.guhs.einde2_h3_aarde.{key}")
    for name in BLOKKEN:
        for path in (f"{h.A}/blockstates/{name}.json", f"{h.A}/models/block/{name}.json", f"{h.A}/models/item/{name}.json",
                     f"{h.D}/loot_table/blocks/{name}.json"):
            if not os.path.exists(path):
                problems.append(path)
        if f"block.guhs.{name}" not in h.NL or f"block.guhs.{name}.lore" not in h.NL:
            problems.append(f"no name or lore for {name}")
    for path in (f"{h.TEX}/gui/einde2_h3_aarde/pai_sho.png", f"{h.D}/einde2/h3_aarde/koolkraam.json", f"{h.TEX}/gui/verhaal/kaart_{SLICE}.png"):
        if not os.path.exists(path):
            problems.append(path)
    kraam = json.load(open(f"{h.D}/einde2/h3_aarde/koolkraam.json", encoding="utf-8"))
    if not kraam.get("heel") or not kraam.get("kapot") or any(not (0 <= c["x"] < 5 and 0 <= c["y"] < 4 and 0 <= c["z"] < 4)
                                                             for c in kraam["heel"] + kraam["kapot"]):
        problems.append("koolkraam.json: two lists of blocks inside 5 x 4 x 4")
    # every scene line the Java scripts name is here (the keys of H3Scenes)
    for naam, (_, regels, _n) in tekst.SCENES.items():
        if f"scene.guhs.{SCENE}{naam}.titel" not in h.NL:
            problems.append(f"no title of scene {naam}")
    if len(tekst.STAPPEN) != 15 or len(tekst.DOELEN) != 15 or len(FTB) != 15:
        problems.append("fifteen steps, fifteen goals, fifteen quests")
    if problems:
        raise SystemExit("einde2_h3_aarde self-check:\n  " + "\n  ".join(problems[:40]))


def build(h):
    textures(h)
    blocks_and_items(h)
    pai_sho_gui(h)
    koolkraam(h)
    texts(h)
    selfcheck(h)
