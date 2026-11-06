"""
1.10.0 "Het Vadsrijk", zone 4 "De Lampionkust", T-Z4BOOT: the player's own guh sailboat (Java: feature/vadsrijk/zone4/boot).

  * the GeckoLib model vr_eco_zeilbootje (renders-lampionkust/05_zeilbootje.png, _bron/bouwsels.guh_zeilbootje): a round pink
    hull with a guh face on the bow (+ little ears), a fore-and-aft sail with guh ears on top and a guh face, a pennant on the
    mast top, a figurehead, a lantern on a pole at the stern (glows: _glowmask) and a tiny guh sailor sitting up front;
    the variants: one bone per pennant (wimpel_*) and figurehead (boeg_*) - the renderer shows the chosen one - and one texture per
    sail colour (vr_eco_zeilbootje_<kleur>.png);
  * animations idle (bobbing), varen (rocking, pennant flutters), hard (wind behind: bow up, sail full);
  * the icon vr_item_zeilbootje ("Bootje in een fles");
  * every text (Dutch; en_us gets English).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z4boot.py
"""
import json
import os

import numpy as np
from PIL import Image

try:
    import vadsrijk_wezens as W
    from vadsrijk_economie import Doek
except ImportError:  # (inside make_resources: the package import)
    from features import vadsrijk_wezens as W
    from features.vadsrijk_economie import Doek

ITEM_TEX = "vadsrijk/zone4/boot"
kf, anim = W.kf, W.anim

ZEILEN = {   # sail colour, its darker edge (the variants of BootVariant ZEIL_*)
    "roze": ((255, 140, 186), (255, 246, 250)),
    "mint": ((140, 228, 192), (246, 255, 250)),
    "lavendel": ((192, 164, 242), (250, 246, 255)),
    "zeeblauw": ((124, 196, 244), (246, 252, 255)),
    "honing": ((255, 204, 102), (255, 250, 236)),
    "sterrennacht": ((58, 52, 120), (250, 226, 120)),
}


# =====================================================================================================================
# the model (model px; the bow points to -z like every GeckoLib mob; y 0 = the boat's feet, the water at ~6.4)
# =====================================================================================================================
def model_boot():
    m = W.Model("eco_zeilbootje", px=2, breedte=192, bounds=(6, 5, 1.5))
    m.bone("root", None, (0, 4, 0))
    # ---- the hull: a round tub, narrower at the bow, a guh face on the bow block -------------------------------------------
    m.bone("romp", "root", (0, 4, 0))
    m.cube("romp", "romp", (-12, 0, -28), (24, 2, 58))                # bottom
    m.cube("romp", "romp", (-14, 1, -26), (2, 11, 54))                # sides
    m.spiegel("romp", "romp", (-14, 1, -26), (2, 11, 54))
    m.cube("romp", "romp", (-12, 1, 28), (24, 11, 3))                 # stern
    m.cube("romp", "romp", (-11, 1, -32), (22, 11, 6))                # bow, wide part
    m.cube("romp", "boegkop", (-7, 1.5, -38), (14, 11, 6), gezicht={"ey": 0.42, "schaal": 0.9})   # the guh face on the bow
    m.cube("romp", "dek", (-12, 2, -26), (24, 1, 54))                 # deck planks
    m.cube("romp", "rand", (-14.5, 11.5, -26), (3, 1.5, 54))          # the white rail
    m.spiegel("romp", "rand", (-14.5, 11.5, -26), (3, 1.5, 54))
    m.cube("romp", "rand", (-12, 11.5, 28), (24, 1.5, 3.5))
    m.cube("romp", "rand", (-11, 11.5, -32.5), (22, 1.5, 6))
    m.cube("romp", "streep", (-14.2, 6, -25), (0.5, 2, 52))           # a pink stripe along the hull
    m.spiegel("romp", "streep", (-14.2, 6, -25), (0.5, 2, 52))
    m.cube("romp", "bank", (-12, 3, 16), (24, 3, 9))                  # the bench where you sit
    m.cube("romp", "bank", (-12, 3, -20), (24, 2, 6))                 # the sailor's bench
    for sgn in (-1, 1):                                                # ears on the bow face
        if sgn < 0:
            m.cube("romp", "boegkop", (-7, 12.5, -37), (3.5, 3, 2))
            m.cube("romp", "oor", (-6.5, 13, -37.4), (2.5, 2, 0.5))
        else:
            m.spiegel("romp", "boegkop", (-7, 12.5, -37), (3.5, 3, 2))
            m.spiegel("romp", "oor", (-6.5, 13, -37.4), (2.5, 2, 0.5))
    m.cube("romp", "roer", (-1, 2, 31), (2, 9, 3))                    # rudder + tiller
    m.cube("romp", "roer", (-0.75, 11, 22), (1.5, 1.5, 11))
    # ---- mast, boom and the sail (fore-and-aft, along the hull) ---------------------------------------------------------------
    m.bone("tuig", "root", (0, 12, -10))
    m.cube("tuig", "mast", (-1.5, 2, -11.5), (3, 56, 3))
    m.cube("tuig", "mast", (-1, 14, -9), (2, 2, 38))                   # the boom
    m.bone("zeil", "tuig", (0, 16, -9))
    m.cube("zeil", "zeilgezicht", (-0.5, 16, -9), (1, 18, 36))          # lower sail: the guh face
    m.cube("zeil", "zeil", (-0.5, 34, -9), (1, 12, 28))
    m.cube("zeil", "zeil", (-0.5, 46, -9), (1, 7, 19))
    m.cube("zeil", "zeilrand", (-0.6, 16, 26.5), (1.2, 18, 1))           # the trailing edge
    m.cube("zeil", "zeil", (-0.5, 53, -8), (1, 5, 6))                    # guh ears on top of the sail
    m.cube("zeil", "oor", (-0.6, 53.5, -7), (1.2, 3, 4))
    m.cube("zeil", "zeil", (-0.5, 53, 3), (1, 5, 6))
    m.cube("zeil", "oor", (-0.6, 53.5, 4), (1.2, 3, 4))
    m.cube("tuig", "touw", (-0.25, 12, -36), (0.5, 0.5, 0.5))           # (anchor point of the forestay, tiny)
    # ---- the pennant on the mast top (one bone per variant) -------------------------------------------------------------------
    m.bone("wimpel", "tuig", (0, 56, -10))
    m.bone("wimpel_hartje", "wimpel", (0, 56, -10))
    m.cube("wimpel_hartje", "w_hartje", (-0.25, 53, -8.5), (0.5, 4, 7))
    m.cube("wimpel_hartje", "w_hartje2", (-0.3, 53.5, -1.5), (0.6, 3, 3))
    m.bone("wimpel_ster", "wimpel", (0, 56, -10))
    m.cube("wimpel_ster", "w_ster", (-0.25, 53, -8.5), (0.5, 4, 6))
    m.cube("wimpel_ster", "w_ster", (-0.25, 54, -2.5), (0.5, 2, 3))
    m.cube("wimpel_ster", "w_ster2", (-0.3, 54.2, -6.5), (0.6, 1.6, 1.6))
    m.bone("wimpel_streepjes", "wimpel", (0, 56, -10))
    m.cube("wimpel_streepjes", "w_streep", (-0.25, 53, -8.5), (0.5, 4, 10))
    m.bone("wimpel_vadsrijk", "wimpel", (0, 56, -10))
    m.cube("wimpel_vadsrijk", "w_vads", (-0.25, 52, -8.5), (0.5, 5, 8))
    m.cube("wimpel_vadsrijk", "w_vads2", (-0.3, 53.5, -6.5), (0.6, 2, 2))
    # ---- the figurehead (one bone per variant) on the bow tip -----------------------------------------------------------------
    m.bone("boeg", "romp", (0, 10, -38))
    m.bone("boeg_guhkopje", "boeg", (0, 10, -38))
    m.cube("boeg_guhkopje", "b_guh", (-2.5, 8, -43), (5, 5, 5), gezicht={"ey": 0.42, "schaal": 1.0})
    m.cube("boeg_guhkopje", "b_guh", (-2.5, 13, -41), (1.5, 2, 1))
    m.cube("boeg_guhkopje", "b_guh", (1, 13, -41), (1.5, 2, 1))
    m.bone("boeg_dolfijntje", "boeg", (0, 10, -38))
    m.cube("boeg_dolfijntje", "b_dolfijn", (-2, 8, -46), (4, 4, 8), gezicht={"ey": 0.45, "schaal": 0.9})
    m.cube("boeg_dolfijntje", "b_dolfijn", (-1, 8.5, -48), (2, 2, 2))
    m.cube("boeg_dolfijntje", "b_dolfijn", (-0.5, 12, -42), (1, 3, 2))
    m.bone("boeg_sterretje", "boeg", (0, 10, -38))
    m.cube("boeg_sterretje", "b_ster", (-3, 8, -40), (6, 6, 1.5), gezicht={"ey": 0.45, "schaal": 0.9})
    m.cube("boeg_sterretje", "b_ster", (-1, 14, -40), (2, 2, 1.5))
    m.cube("boeg_sterretje", "b_ster", (-5, 10, -40), (2, 2, 1.5))
    m.cube("boeg_sterretje", "b_ster", (3, 10, -40), (2, 2, 1.5))
    m.bone("boeg_draakje", "boeg", (0, 10, -38))
    m.cube("boeg_draakje", "b_draak", (-3, 8, -44), (6, 5, 6), gezicht={"ey": 0.45, "schaal": 1.0})
    m.cube("boeg_draakje", "b_hoorn", (-2.5, 13, -40), (1, 2.5, 1))
    m.cube("boeg_draakje", "b_hoorn", (1.5, 13, -40), (1, 2.5, 1))
    m.cube("boeg_draakje", "b_draak", (-1.5, 8, -46), (3, 3, 2))
    # ---- the lantern at the stern (right corner, hanging over the water) ------------------------------------------------------
    m.bone("paal", "romp", (11, 12, 29))
    m.cube("paal", "paal", (10.5, 12, 28.5), (1.5, 20, 1.5))
    m.cube("paal", "paal", (10.5, 31, 28.5), (7, 1, 1.5))
    m.bone("lantaarn", "paal", (16, 31, 29))
    m.cube("lantaarn", "paal", (15.5, 28, 29), (1, 3, 0.5))
    m.cube("lantaarn", "lampion", (14, 23, 27.5), (4, 5, 4))
    m.cube("lantaarn", "lampdak", (13.5, 28, 27), (5, 1, 5))
    m.cube("lantaarn", "lampdak", (13.5, 22.5, 27), (5, 0.5, 5))
    # ---- the tiny guh sailor up front -------------------------------------------------------------------------------------------
    m.bone("matroos", "romp", (0, 5, -17))
    m.cube("matroos", "m_lijf", (-3, 5, -19.5), (6, 5, 5))
    m.bone("matroos_hoofd", "matroos", (0, 10, -17))
    m.cube("matroos_hoofd", "m_hoofd", (-3.5, 10, -20.5), (7, 6, 6), gezicht={"ey": 0.42, "schaal": 1.0})
    m.cube("matroos_hoofd", "m_hoofd", (-3.5, 16, -18), (2, 2.5, 1.5))
    m.cube("matroos_hoofd", "m_hoofd", (1.5, 16, -18), (2, 2.5, 1.5))
    m.cube("matroos_hoofd", "m_pet", (-3, 16, -19.5), (6, 1.5, 4.5))
    m.cube("matroos_hoofd", "m_pet2", (-3.2, 15.5, -19.8), (6.4, 0.6, 5))
    return m


def palet(zeil, zeilrand):
    return {
        "romp": ((236, 150, 150), "strepen", (220, 128, 132)),
        "boegkop": ((255, 182, 206), None, None),
        "oor": ((255, 130, 176), None, None),
        "dek": ((226, 176, 132), "strepen", (206, 156, 112)),
        "rand": ((255, 246, 250), None, None),
        "streep": ((255, 96, 160), None, None),
        "bank": ((196, 136, 104), None, None),
        "roer": ((176, 120, 92), None, None),
        "mast": ((214, 160, 120), "veer", (190, 136, 100)),
        "touw": ((240, 240, 240), None, None),
        "zeil": (zeil, None, None),
        "zeilgezicht": (zeil, None, None),
        "zeilrand": (zeilrand, None, None),
        "w_hartje": ((255, 96, 150), None, None),
        "w_hartje2": ((255, 220, 236), None, None),
        "w_ster": ((255, 216, 74), None, None),
        "w_ster2": ((255, 255, 220), None, None),
        "w_streep": ((106, 184, 240), "strepen", (255, 255, 255)),
        "w_vads": ((138, 74, 208), None, None),
        "w_vads2": ((255, 214, 90), None, None),
        "b_guh": ((255, 192, 216), None, None),
        "b_dolfijn": ((138, 180, 232), None, None),
        "b_ster": ((255, 208, 64), None, None),
        "b_draak": ((176, 112, 224), None, None),
        "b_hoorn": ((255, 236, 200), None, None),
        "paal": ((92, 70, 72), None, None),
        "lampion": ((255, 200, 96), None, None),
        "lampdak": ((220, 70, 90), None, None),
        "m_lijf": ((255, 255, 255), "strepen", (110, 160, 230)),
        "m_hoofd": ((255, 200, 220), None, None),
        "m_pet": ((255, 255, 255), None, None),
        "m_pet2": ((60, 90, 170), None, None),
    }


def schilder(m, kleur, rand):
    img = W.schilder(m, palet(kleur, rand), "blij", 4711, False)
    a = np.array(img)
    S = m.px
    for reg in m.regios:
        if reg["rol"] != "zeilgezicht" or reg["f"] not in ("east", "west"):
            continue
        x, y, w, h = reg["r"]
        blok = a[y * S:(y + h) * S, x * S:(x + w) * S]
        W.gezicht(blok, "blij", 0.5, 0.9)               # the guh face on both sides of the sail
        blok[-max(1, S):, :, :3] = rand                 # a white hem at the bottom
    for reg in m.regios:                                 # a lantern with a warm window pattern
        if reg["rol"] == "lampion" and reg["f"] in ("north", "south", "east", "west"):
            x, y, w, h = reg["r"]
            blok = a[y * S:(y + h) * S, x * S:(x + w) * S]
            blok[:, ::3, :3] = (255, 236, 160)
    return Image.fromarray(a)


def anims_boot():
    def schommel(rol, stamp, lengte):
        h = lengte / 2
        q = lengte / 4
        return {"rotation": kf([(0, (stamp, 0, rol)), (q, (0, 0, 0)), (h, (-stamp, 0, -rol)), (3 * q, (0, 0, 0)), (lengte, (stamp, 0, rol))])}

    def wapper(hoek, lengte):
        return {"rotation": kf([(0, (0, hoek, 0)), (lengte / 2, (0, -hoek, 0)), (lengte, (0, hoek, 0))])}

    def lamp(hoek, lengte):
        return {"rotation": kf([(0, (0, 0, hoek)), (lengte / 2, (0, 0, -hoek)), (lengte, (0, 0, hoek))])}

    idle = {"root": schommel(2.0, 1.0, 3.2),
            "wimpel": wapper(8, 1.6),
            "lantaarn": lamp(6, 3.2),
            "zeil": wapper(2, 3.2),
            "matroos_hoofd": {"rotation": kf([(0, (0, 0, 0)), (1.6, (-4, 12, 0)), (3.2, (0, 0, 0))])}}
    varen = {"root": schommel(3.0, 2.0, 2.0),
             "wimpel": wapper(18, 0.5),
             "lantaarn": lamp(10, 1.0),
             "zeil": {"rotation": kf([(0, (0, 4, 0)), (1.0, (0, 6, 0)), (2.0, (0, 4, 0))])},
             "matroos_hoofd": {"rotation": kf([(0, (-6, 0, 0)), (1.0, (-2, 0, 4)), (2.0, (-6, 0, 0))])}}
    hard = {"root": {"rotation": kf([(0, (-3, 0, 3)), (0.6, (-4, 0, -2)), (1.2, (-3, 0, 3))])},
            "wimpel": wapper(30, 0.3),
            "lantaarn": lamp(14, 0.6),
            "zeil": {"rotation": kf([(0, (0, 8, 0)), (0.6, (0, 10, 0)), (1.2, (0, 8, 0))])},
            "matroos_hoofd": {"rotation": kf([(0, (-10, 0, 0)), (0.6, (-8, 0, 0)), (1.2, (-10, 0, 0))])}}
    return {"idle": anim(3.2, idle), "varen": anim(2.0, varen), "hard": anim(1.2, hard)}


# =====================================================================================================================
# the icon: a little boat in a bottle
# =====================================================================================================================
def fles():
    d = Doek()
    glas, rand = (200, 236, 255), (150, 200, 236)
    d.rect(2, 6, 13, 13, glas)                   # the bottle lying down
    d.rect(3, 5, 12, 5, glas)
    d.rect(3, 14, 12, 14, glas)
    d.rect(13, 8, 15, 11, rand)                  # neck + cork
    d.rect(15, 8, 15, 11, (190, 140, 96))
    d.rect(3, 11, 12, 13, (124, 196, 244))       # water
    d.rect(5, 10, 10, 11, (236, 150, 150))       # hull
    d.px(4, 10, (236, 150, 150))
    d.px(10, 9, (255, 182, 206))
    d.rect(7, 4, 7, 9, (214, 160, 120))          # mast
    d.rect(8, 5, 10, 9, (255, 140, 186))         # sail with ears
    d.px(8, 4, (255, 140, 186))
    d.px(10, 4, (255, 140, 186))
    d.px(9, 7, (40, 26, 40))
    d.px(4, 6, (255, 255, 255))                  # glint on the glass
    d.px(4, 7, (255, 255, 255))
    d.schaduw()
    return d.rand()


# =====================================================================================================================
# texts (nl, en)
# =====================================================================================================================
Z = "vadsrijk.z4boot."
STEIGERS = {"lampionhaven": "Lampionhaven", "knuffeltij": "Knuffeltij-strand", "bloemeneiland": "Bloemeneiland", "strandfeest": "Strandfeesteiland",
            "koraalbaai": "Koraalbaai", "vliegereiland": "Vliegereiland", "vuurtoren": "Vuurtoreneiland", "mysterie": "Mysterie-eiland",
            "opstijgpier": "Opstijgpier"}
VARIANTEN = {
    "zeil_roze": ("Roze zeil", "Pink sail"), "zeil_mint": ("Muntgroen zeil", "Mint sail"), "zeil_lavendel": ("Lavendel zeil", "Lavender sail"),
    "zeil_zeeblauw": ("Zeeblauw zeil", "Sea-blue sail"), "zeil_honing": ("Honingzeil", "Honey sail"),
    "zeil_sterrennacht": ("Sterrennachtzeil", "Starry-night sail"),
    "wimpel_hartje": ("Hartjeswimpel", "Heart pennant"), "wimpel_ster": ("Sterrenwimpel", "Star pennant"),
    "wimpel_streepjes": ("Streepjeswimpel", "Striped pennant"), "wimpel_vadsrijk": ("Vadsrijkwimpel", "Vadsrijk pennant"),
    "boeg_guhkopje": ("Guhkopje", "Little guh head"), "boeg_dolfijntje": ("Dolfijntje", "Little dolphin"),
    "boeg_sterretje": ("Sterretje", "Little star"), "boeg_draakje": ("Draakje", "Little dragon"),
}


def teksten():
    t = {
        "entity.guhs.vr_eco_zeilbootje": ("Zeilbootje", "Little sailboat"),
        "item.guhs.vr_item_zeilbootje": ("Bootje in een fles", "Boat in a bottle"),
        Z + "fles.uitleg": ("Rechtsklik: het guh-zeilbootje is van jou! Roep het met je rijdier-toets in het water van de Lampionkust.",
                            "Right-click: the guh sailboat is yours! Call it with your mount key in the water of the Lampion Coast."),
        "vadsrijk.economie.rijdier.zeilbootje": ("Zeilbootje", "Little sailboat"),
        "vadsrijk.economie.rijdier.eigen.zeilbootje": ("Je eigen guh-zeilbootje voor de Lampionkust. W = zeil hijsen, A/D = sturen, Shift bij een steiger = aanmeren.",
                                                       "Your own guh sailboat for the Lampion Coast. W = hoist the sail, A/D = steer, Shift at a dock = moor."),
        "vadsrijk.economie.stal.nog_niet.zeilbootje": ("Krijg je vroeg in het verhaal van de Lampionkust.", "You get it early in the story of the Lampion Coast."),
        Z + "alleen_water": ("Je zeilbootje vaart alleen op het water (en je hebt nog geen landrijdier).",
                             "Your sailboat only sails on water (and you have no land mount yet)."),
        Z + "geen_plek": ("Hier is geen plek voor je bootje: zoek open water (3x3, met lucht erboven).",
                          "No room for your boat here: find open water (3x3, with air above)."),
        Z + "stroming": ("De Kringstroom duwt je bootje terug naar de eilanden!", "The Ring Current pushes your boat back to the islands!"),
        Z + "aanleggen": ("Steiger %s: druk op Shift om aan te meren.", "Dock %s: press Shift to moor."),
        Z + "aangemeerd": ("Aangemeerd bij %s.", "Moored at %s."),
        Z + "terug_steiger": ("Je logde uit op zee. Je bootje ligt weer veilig in de haven: je staat op de steiger van %s.",
                              "You logged out at sea. Your boat is safe in the harbour again: you are on the dock of %s."),
        Z + "geen_vreemde_boot": ("Andere bootjes drijven hier niet: in het Vadsrijk vaar je met je eigen guh-zeilbootje.",
                                  "Other boats don't float here: in the Vadsrijk you sail your own guh sailboat."),
        Z + "nieuw_variant": ("Nieuw voor je zeilbootje: %s! Kies het in het Zeilbootje-scherm.", "New for your sailboat: %s! Choose it in the Sailboat screen."),
        Z + "tab": ("Zeilbootje", "Sailboat"),
        Z + "scherm.titel": ("Je zeilbootje", "Your sailboat"),
        Z + "scherm.snelheid": ("Snelheid op zee: %s%%", "Speed at sea: %s%%"),
        Z + "scherm.steiger": ("Laatste steiger: %s", "Last dock: %s"),
        Z + "scherm.nog_niet": ("Je krijgt je eigen zeilbootje vroeg in het verhaal van De Lampionkust.",
                                "You get your own sailboat early in the story of the Lampion Coast."),
        Z + "scherm.uitleg": ("%s in het water of op een steiger = je bootje. W = zeil hijsen, S = remmen, A/D = sturen. Met de wind mee ga je sneller. Shift bij een steiger = aanmeren.",
                              "%s in the water or on a dock = your boat. W = hoist the sail, S = brake, A/D = steer. With the wind behind you you go faster. Shift at a dock = moor."),
        Z + "scherm.kies": ("Dit wil ik!", "I want this!"),
        Z + "scherm.gekozen": ("Je bootje is aangekleed!", "Your boat is dressed up!"),
        Z + "scherm.gekocht": ("Gekocht en meteen gekozen!", "Bought and chosen right away!"),
        Z + "scherm.al": ("Die heb je al.", "You already have that."),
        Z + "scherm.geen_boot": ("Eerst je eigen zeilbootje.", "First your own sailboat."),
        Z + "as.zeil": ("Zeil", "Sail"),
        Z + "as.wimpel": ("Wimpel", "Pennant"),
        Z + "as.boeg": ("Boegbeeld", "Figurehead"),
        Z + "bron.start": ("Heb je meteen.", "You have it from the start."),
        Z + "bron.winkel": ("Te koop voor %s.", "For sale for %s."),
        Z + "bron.beloning": ("Een beloning ergens in de Lampionkust...", "A reward somewhere on the Lampion Coast..."),
        Z + "wind": ("Wind", "Wind"),
        Z + "wind.mee": ("mee!", "behind!"),
        Z + "wind.zij": ("zijwind", "side wind"),
        Z + "wind.tegen": ("tegenwind", "headwind"),
        Z + "wind.stroom": ("Kringstroom!", "Ring Current!"),
        Z + "tip.boot.titel": ("Je eigen zeilbootje!", "Your own sailboat!"),
        Z + "tip.boot": ("Ga in het water of op een steiger staan en druk op je rijdier-toets (X): daar is je bootje. Met N (tab Zeilbootje) kleed je het aan.",
                         "Stand in the water or on a dock and press your mount key (X): there is your boat. With N (tab Sailboat) you can dress it up."),
        Z + "tip.varen.titel": ("Varen!", "Sailing!"),
        Z + "tip.varen": ("W = zeil hijsen (je gaat steeds harder), S = remmen, A/D = sturen. De wind draait: met de wind mee ga je sneller. Bij een steiger: Shift = aanmeren. Aan de rand duwt de Kringstroom je terug.",
                          "W = hoist the sail (you build up speed), S = brake, A/D = steer. The wind turns: with it behind you you go faster. At a dock: Shift = moor. At the edge the Ring Current pushes you back."),
    }
    for k, naam in STEIGERS.items():
        t[Z + "steiger." + k] = (naam, naam)
    for k, (nl, en) in VARIANTEN.items():
        t[Z + "variant." + k] = (nl, en)
    return t


# =====================================================================================================================
# writing
# =====================================================================================================================
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(pad, img):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)


def assets(a, w=_schrijf_json):
    m = model_boot()
    w(os.path.join(a, "geckolib", "models", "entity", "vr_eco_zeilbootje.geo.json"), m.geo())
    w(os.path.join(a, "geckolib", "animations", "entity", "vr_eco_zeilbootje.animation.json"), W.anim_bestand(anims_boot()))
    tex = os.path.join(a, "textures", "entity")
    for naam, (kleur, rand) in ZEILEN.items():
        _png(os.path.join(tex, f"vr_eco_zeilbootje_{naam}.png"), schilder(m, kleur, rand))
    _png(os.path.join(tex, "vr_eco_zeilbootje_glowmask.png"), W.gloed(m, {"lampion": (255, 214, 120)}, 4711))
    naam = "vr_item_zeilbootje"
    fles().png(os.path.join(a, "textures", "item", ITEM_TEX, f"{naam}.png"))
    w(os.path.join(a, "models", "item", f"{naam}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}})
    w(os.path.join(a, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})
    return m


def build(h):
    assets(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    m = assets(a)
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf_json(p, data)
    print(f"vadsrijk_t_z4boot: model ({m.W * m.px}x{m.hoogte() * m.px} texture), {len(ZEILEN)} sails, {len(t)} texts")


if __name__ == "__main__":
    _direct()
