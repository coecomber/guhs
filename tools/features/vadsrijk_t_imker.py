"""
1.10 "Het Vadsrijk", zone 3 "De Schemerwortel", night round T, slice T-IMKER: the secret boss De Mika-imker (Mika de Imker)
in the raatzaal under the Raattros. Java: feature/vadsrijk/imker.

Writes (textures in textures/vadsrijk/z3imker/, GeckoLib models + animations under geckolib/*/entity/, Blockbench copies in
blockbench/vadsrijk/z3/):
  vr_z3_imker_mika      the mika.geo.json bones + beekeeper's hat/veil, suit, spy badge, roker (smoker) + honingblik (honey can)
                        (idle, walk, slag, worp, puf, sprong, giet)
  vr_z3_imker_korf      the Koninginnekorf (straw skep, opening at the front, the queen on top) (idle, walk, klop) + korf_grauw1/2
  vr_z3_imker_rookpot   a smoke pot (idle, walk, pomp)
  vr_z3_imker_zoempje   the pet Zoempje (a golden bee) + vr_huisdier_zoempje.animation.json (idle, hop, sprong)
  item vr_z3_imker_trofee (icon + model)
and the texts (nl + en_us).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_imker.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

try:
    from features.vadsrijk_bazen import Geo, vlak, disc, lijn, kf, bbmodel
    from features import vadsrijk_z3_bazen_modellen as M
except ImportError:  # standalone
    from vadsrijk_bazen import Geo, vlak, disc, lijn, kf, bbmodel
    import vadsrijk_z3_bazen_modellen as M

TEX = "textures/vadsrijk/z3imker"
HONING = (240, 184, 64)
STRO = (214, 172, 92)
STRO_DONKER = (160, 118, 56)
GRAUW = (138, 138, 150)
PAK = (240, 236, 222)
ZWART = (40, 32, 28)


# =====================================================================================================================
# the boss
# =====================================================================================================================
def mika_imker(a_dir):
    def tint(a):
        return M._tint(a, PAK, 0.62)

    def gez(v, W, H):   # a little smudge of grey honey on the cheek, sly eyebrows
        disc(v, W * 0.78, H * 0.7, W * 0.07, GRAUW)
        lijn(v, W * 0.2, H * 0.22, W * 0.4, H * 0.28, (70, 60, 60), dik=1.6)
        lijn(v, W * 0.8, H * 0.22, W * 0.6, H * 0.28, (70, 60, 60), dik=1.6)
    g = M.mika_geo("mika_imker", a_dir, tint, gez)
    rand = vlak(STRO, 8, 701, extra=lambda a, W, H, f, r: [lijn(a, 0, y, W, y, STRO_DONKER, dik=0.8) for y in np.arange(1.5, H, 3)])
    band = vlak((60, 50, 44), 4, 702)

    def gaas(W, H, f):   # the veil: a fine mesh (cutout holes)
        a = np.zeros((H, W, 4), np.uint8)
        a[..., :3] = (40, 40, 44)
        for y in range(H):
            for x in range(W):
                if x % 3 == 0 or y % 3 == 0:
                    a[y, x, 3] = 255
        return a
    pak = vlak(PAK, 6, 703, extra=lambda a, W, H, f, r: (lijn(a, W / 2, 0, W / 2, H, (196, 190, 176), dik=1.0) if f in ("north", "up") else None,
                                                         [disc(a, r.uniform(0, W), r.uniform(0, H), 1.2, GRAUW, alpha=0.6) for _ in range(W * H // 90)]))
    speld = vlak(HONING, 4, 704, extra=lambda a, W, H, f, r: disc(a, W / 2, H * 0.6, W * 0.18, (60, 50, 40)))
    blik = vlak((150, 156, 168), 6, 705, verloop=(110, 114, 128))
    grauwe = vlak(GRAUW, 6, 706)
    leer = vlak((120, 80, 52), 6, 707)
    # hat: a wide straw brim, a crown with a band, the veil all round
    g.cube("head", (-10, 14, -15), (20, 1, 17), rand)
    g.cube("head", (-6, 15, -11), (12, 4, 9), rand)
    g.cube("head", (-6.2, 15, -11.2), (12.4, 1.2, 9.4), band)
    g.cube("head", (-9.5, 6, -14.6), (19, 8, 0.4), gaas)
    g.cube("head", (-9.6, 6, -14.6), (0.4, 8, 15), gaas)
    g.cube("head", (9.2, 6, -14.6), (0.4, 8, 15), gaas)
    # the suit over the body + the Keizer's badge (a crown with a cheese hole) on the chest
    g.cube("body", (-7, 0.5, -2.5), (14, 9.5, 14), pak, inflate=0.25)
    g.cube("body", (-1.5, 4, -3.4), (3, 3, 0.8), speld)
    # the smoker (right front paw): can + bellows + spout
    g.bone("roker", "leg_front_right", (-4, 2, -12))
    g.cube("roker", (-6.5, 2, -19), (5, 7, 5), blik)
    g.cube("roker", (-5.5, 9, -18), (3, 2, 3), blik)
    g.cube("roker", (-4.5, 11, -17), (1, 2, 1), blik)
    g.cube("roker", (-7.5, 3, -15), (1, 5, 4), leer)
    # the honey can (left front paw; shows while he pours)
    g.bone("honingblik", "leg_front_left", (4, 2, -12))
    g.cube("honingblik", (1.5, 2, -19), (5, 6, 5), blik)
    g.cube("honingblik", (3.5, 5, -22), (1.2, 1.2, 4), blik)
    g.cube("honingblik", (2, 8, -18.5), (4, 0.6, 4), grauwe)
    anims = M.mika_anims({
        "puf": kf({"leg_front_right": {"rotation": {0: [0, 0, 0], 0.15: [-70, 0, 0], 0.5: [-70, 0, 0], 0.7: [0, 0, 0]}},
                   "roker": {"scale": {0: [1, 1, 1], 0.2: [1.15, 0.85, 1.15], 0.35: [0.95, 1.1, 0.95], 0.5: [1, 1, 1]}},
                   "head": {"rotation": {0: [0, 0, 0], 0.2: [-8, 0, 0], 0.7: [0, 0, 0]}}}, loop=False),
        "giet": kf({"leg_front_left": {"rotation": {0: [-60, 0, -10], 1.0: [-70, 0, -10], 2.0: [-60, 0, -10]}},
                    "head": {"rotation": {0: [10, -10, 0], 1.0: [12, -10, 0], 2.0: [10, -10, 0]}}}),
    })
    return g, anims


# =====================================================================================================================
# the room's objects
# =====================================================================================================================
def _stro(grauw):
    basis = tuple(int(c * (1 - grauw) + g * grauw) for c, g in zip(STRO, GRAUW))
    donker = tuple(int(c * (1 - grauw) + g * 0.7 * grauw) for c, g in zip(STRO_DONKER, GRAUW))

    def extra(a, W, H, f, r):
        for y in np.arange(1.5, H, 3):
            lijn(a, 0, y, W, y + r.uniform(-0.5, 0.5), donker, dik=1.0)
        if grauw > 0:
            for _ in range(int(W * H // 40 * grauw)):
                disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(0.8, 2.0), (110, 110, 122), alpha=0.6)
    return vlak(basis, 8, 711, extra=extra)


def korf(grauw=0.0):
    g = Geo("korf", 128, 128, 2)
    stro = _stro(grauw)

    def opening(W, H, f):   # the front face of the bottom ring: the dark flight hole
        a = stro(W, H, f)
        if f == "north":
            for y in range(int(H * 0.25), int(H * 0.8)):
                for x in range(int(W * 0.38), int(W * 0.62)):
                    a[y, x, :3] = (30, 22, 16)
        return a
    plank = vlak((150, 104, 64), 6, 712)
    kon_lijf = vlak(tuple(int(c * (1 - grauw) + g_ * grauw) for c, g_ in zip(HONING, GRAUW)), 6, 713,
                    extra=lambda a, W, H, f, r: [lijn(a, 0, y, W, y, ZWART, dik=1.0) for y in np.arange(2, H, 4)])
    kroon = vlak((250, 210, 80), 4, 714)
    vleugel = vlak((230, 240, 255), 4, 715)
    g.bone("root")
    g.bone("korf", "root", (0, 0, 0))
    g.cube("korf", (-9, 0, -9), (18, 4, 18), stro, faces={"north": opening})
    g.cube("korf", (-8, 4, -8), (16, 4, 16), stro)
    g.cube("korf", (-6.5, 8, -6.5), (13, 4, 13), stro)
    g.cube("korf", (-4.5, 12, -4.5), (9, 3, 9), stro)
    g.cube("korf", (-2, 15, -2), (4, 2, 4), stro)
    g.cube("korf", (-3, 0, -12), (6, 1, 3), plank)   # the landing board in front of the hole
    g.bone("koningin", "korf", (0, 17, 0))   # the queen sits on top
    g.cube("koningin", (-1.5, 17, -2.5), (3, 3, 5), kon_lijf)
    g.cube("koningin", (-1, 20, -2), (2, 1, 2), kroon)
    g.bone("vleugels", "koningin", (0, 20, 0))
    g.cube("vleugels", (-4, 20, -1), (3, 0.2, 3), vleugel)
    g.cube("vleugels", (1, 20, -1), (3, 0.2, 3), vleugel)
    zoem = {"vleugels": {"rotation": {0: [0, 0, 8], 0.1: [0, 0, -8], 0.2: [0, 0, 8]}},
            "koningin": {"position": {0: [0, 0, 0], 1.0: [0, 0.4, 0], 2.0: [0, 0, 0]}}}
    an = {"animations": {
        "idle": kf(zoem, lengte=2.0), "walk": kf(zoem, lengte=2.0),
        "klop": kf({"korf": {"scale": {0: [1, 1, 1], 0.1: [1.08, 0.94, 1.08], 0.25: [0.97, 1.04, 0.97], 0.4: [1, 1, 1]}}}, loop=False),
    }}
    return g, an


def rookpot():
    g = Geo("rookpot", 64, 64, 4)
    blik = vlak((150, 156, 168), 6, 721, verloop=(100, 104, 118), extra=M.schimmel)
    leer = vlak((120, 80, 52), 6, 722)
    rood = vlak((200, 80, 50), 6, 723)
    g.bone("root")
    g.bone("pot", "root", (0, 0, 0))
    g.cube("pot", (-3, 0, -3), (6, 7, 6), blik)
    g.cube("pot", (-3.5, 0, -3.5), (7, 1, 7), rood)
    g.bone("deksel", "pot", (0, 7, 0))
    g.cube("deksel", (-2.5, 7, -2.5), (5, 2, 5), blik)
    g.cube("deksel", (-0.75, 9, -0.75), (1.5, 2.5, 1.5), blik)
    g.cube("pot", (3, 1.5, -2), (1.2, 5, 4), leer)   # the bellows
    an = {"animations": {
        "idle": kf({"deksel": {"position": {0: [0, 0, 0], 2.0: [0, 0, 0]}}}), "walk": kf({"deksel": {"position": {0: [0, 0, 0], 2.0: [0, 0, 0]}}}),
        "pomp": kf({"deksel": {"position": {0: [0, 0, 0], 0.1: [0, 1.5, 0], 0.3: [0, 0, 0], 0.4: [0, 1, 0], 0.6: [0, 0, 0]}},
                    "pot": {"scale": {0: [1, 1, 1], 0.1: [1.1, 0.9, 1.1], 0.3: [1, 1, 1]}}}, loop=False),
    }}
    return g, an


# =====================================================================================================================
# the pet: Zoempje
# =====================================================================================================================
def zoempje():
    g = Geo("zoempje", 64, 64, 4)
    lijf = vlak(HONING, 6, 731, extra=lambda a, W, H, f, r: [lijn(a, x, 0, x, H, ZWART, dik=1.4) for x in np.arange(3, W, 5)]
                if f in ("east", "west", "up", "down") else None)
    kop = vlak((70, 54, 40), 6, 732)

    def gezicht(W, H, f):
        a = kop(W, H, f)
        if f == "north":
            for x in (W * 0.3, W * 0.7):
                disc(a, x, H * 0.45, W * 0.13, (20, 14, 16))
                disc(a, x - W * 0.04, H * 0.4, W * 0.05, (255, 255, 255))
            disc(a, W * 0.18, H * 0.72, W * 0.08, (255, 130, 170), alpha=0.7)
            disc(a, W * 0.82, H * 0.72, W * 0.08, (255, 130, 170), alpha=0.7)
        return a
    vleugel = vlak((228, 240, 255), 3, 733)
    voelspriet = vlak(ZWART, 2, 734)
    g.bone("root")
    g.bone("body", "root", (0, 5, 0))
    g.cube("body", (-2.5, 3, -2.5), (5, 5, 6.5), lijf)
    g.cube("body", (-0.5, 4.5, 4), (1, 1, 1.5), voelspriet)   # the (blunt) sting
    g.bone("head", "body", (0, 5.5, -2.5))
    g.cube("head", (-2.25, 3.25, -5.5), (4.5, 4.5, 3), kop, faces={"north": gezicht})
    g.cube("head", (-1.5, 7.75, -5), (0.6, 2, 0.6), voelspriet, rotation=(-20, 0, -15))
    g.cube("head", (0.9, 7.75, -5), (0.6, 2, 0.6), voelspriet, rotation=(-20, 0, 15))
    g.bone("vleugel_l", "body", (1, 8, 0))
    g.cube("vleugel_l", (1, 8, -1.5), (4.5, 0.2, 3.5), vleugel)
    g.bone("vleugel_r", "body", (-1, 8, 0))
    g.cube("vleugel_r", (-5.5, 8, -1.5), (4.5, 0.2, 3.5), vleugel)
    flap = {"vleugel_l": {"rotation": {0: [0, 0, 20], 0.05: [0, 0, -25], 0.1: [0, 0, 20]}},
            "vleugel_r": {"rotation": {0: [0, 0, -20], 0.05: [0, 0, 25], 0.1: [0, 0, -20]}}}
    an = {"animations": {
        "idle": kf({**flap, "root": {"position": {0: [0, 4, 0], 0.5: [0, 4.6, 0], 1.0: [0, 4, 0]}}}, lengte=1.0),
        "hop": kf({**flap, "root": {"position": {0: [0, 4, 0], 0.15: [0, 5, 0], 0.3: [0, 4, 0]}, "rotation": {0: [8, 0, 0], 0.3: [8, 0, 0]}}},
                  lengte=0.3),
        "sprong": kf({**flap, "root": {"position": {0: [0, 4, 0], 0.3: [0, 9, 0], 0.6: [0, 4, 0]}, "rotation": {0: [0, 0, 0], 0.3: [0, 360, 0], 0.6: [0, 360, 0]}}},
                     loop=False, lengte=0.6),
    }}
    return g, an


# =====================================================================================================================
# the trophy icon
# =====================================================================================================================
def trofee_icoon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    O = (40, 30, 46, 255)
    d.rectangle([4, 12, 11, 14], fill=(200, 160, 80, 255), outline=O)
    d.rectangle([6, 10, 9, 11], fill=(220, 180, 90, 255))
    d.ellipse([3, 4, 12, 10], fill=PAK + (255,), outline=O)            # the mika head in his suit
    d.point([(6, 7), (9, 7)], fill=(255, 60, 60, 255))
    d.rectangle([1, 3, 14, 4], fill=STRO + (255,), outline=O)           # the straw hat
    d.rectangle([5, 1, 10, 3], fill=STRO + (255,), outline=O)
    d.point([(13, 9), (14, 8)], fill=HONING + (255,))                   # a bee
    return im


# =====================================================================================================================
# texts (nl, en)
# =====================================================================================================================
Z = "vadsrijk.z3imker."


def teksten():
    t = {
        # names
        "entity.guhs.vr_z3_imker_mika": ("Mika de Imker", "Mika the Beekeeper"),
        "entity.guhs.vr_z3_imker_korf": ("Koninginnekorf (rechtsklik = draaien)", "Queen's hive (right-click = turn)"),
        "entity.guhs.vr_z3_imker_rookpot": ("Rookpotje (rechtsklik)", "Smoke pot (right-click)"),
        "item.guhs.vr_z3_imker_trofee": ("Trofee: Mika de Imker", "Trophy: Mika the Beekeeper"),
        Z + "naam": ("Mika de Imker", "Mika the Beekeeper"),
        Z + "titel": ("MIKA DE IMKER", "MIKA THE BEEKEEPER"),
        Z + "titel.onder": ("Spion van de Keizer. Imker voor spek en bonen.", "The Emperor's spy. Beekeeper in name only."),
        "vadsrijk.z3bazen.imker": ("MIKA DE IMKER", "MIKA THE BEEKEEPER"),
        "vadsrijk.z3bazen.imker.onder": ("Spion van de Keizer. Imker voor spek en bonen.", "The Emperor's spy. Beekeeper in name only."),
        # the tip at the intro (Z3Baas: vadsrijk.z3bazen.tip.<id>)
        "vadsrijk.z3bazen.tip.imker.titel": ("Mika de Imker", "Mika the Beekeeper"),
        "vadsrijk.z3bazen.tip.imker": ("Rood = weg daar. Een zwerm achter je aan? Rechtsklik een rookpotje en loop door de rook. Giet hij honing in de korf? "
                                       "Draai de korf (rechtsklik) met zijn gat naar hem toe!",
                                       "Red = move. A swarm after you? Right-click a smoke pot and walk through the smoke. Is he pouring honey into the "
                                       "hive? Turn the hive (right-click) so its hole points at him!"),
        Z + "tip.rook.titel": ("Rookpotjes", "Smoke pots"),
        Z + "tip.rook": ("Rechtsklik een rookpotje: 8 tellen een rookwolk. Een boze zwerm die door de rook vliegt, valt in slaap. In de rook steekt geen bij je.",
                         "Right-click a smoke pot: a smoke cloud for 8 seconds. An angry swarm that flies through the smoke falls asleep. No bee stings you in the smoke."),
        Z + "tip.korf.titel": ("De Koninginnekorf", "The Queen's hive"),
        Z + "tip.korf": ("Rechtsklik de korf om zijn gat een zesde slag te draaien (shift + rechtsklik = terug). Elke 6 tellen spuit de korf een stroom bijen "
                         "langs de rode baan. Wijst die baan naar de Mika, dan steken de bijen HEM.",
                         "Right-click the hive to turn its hole a sixth of a turn (shift + right-click = back). Every 6 seconds the hive shoots a stream of "
                         "bees along the red lane. If that lane points at the Mika, the bees sting HIM."),
        # phases (title cards + kern's phase line)
        Z + "fase2": ("De grauwe koningin!", "The grey queen!"),
        Z + "fase2.onder": ("Hij giet grauwe honing in de korf. Draai de korf naar hem toe!", "He pours grey honey into the hive. Turn the hive at him!"),
        Z + "fase3": ("De grote rookpot!", "The big smoker!"),
        Z + "fase3.onder": ("Alles of niks. Schuil bij de koningin!", "All or nothing. Shelter by the queen!"),
        Z + "grauw": ("De koningin wordt grauw!", "The queen turns grey!"),
        Z + "grauw.onder": ("Draai de korf naar de Mika, voor het weer gebeurt!", "Turn the hive at the Mika before it happens again!"),
        "vadsrijk.kern.beleving.fase.vr_z3_imker_mika.2": ("Fase 2: De grauwe koningin!", "Phase 2: The grey queen!"),
        "vadsrijk.kern.beleving.fase.vr_z3_imker_mika.3": ("Fase 3: De grote rookpot!", "Phase 3: The big smoker!"),
        # overlays
        Z + "immuun": ("Hij giet en is nergens te raken! Draai de korf zodat de bijen hem steken.", "He is pouring and can't be hit! Turn the hive so the bees sting him."),
        Z + "giet": ("Hij klimt op een stok en giet grauwe honing in de korf...", "He climbs onto a perch and pours grey honey into the hive..."),
        Z + "gestoken": ("Raak! De bijen steken de Mika. Nu kun je hem raken!", "Hit! The bees sting the Mika. Now you can hit him!"),
        Z + "korf.gedraaid": ("De korf draait. Het gat wijst nu een andere kant op.", "The hive turns. Its hole points another way now."),
        Z + "pot.rook": ("Pfff... een dikke rookwolk. De bijtjes worden er suf van.", "Pfff... a thick smoke cloud. It makes the bees drowsy."),
        Z + "pot.laden": ("Het rookpotje smeult nog na: nog %s tellen.", "The smoke pot is still smouldering: %s more seconds."),
        Z + "puf": ("Hij zet zijn roker op je! Stap opzij!", "He aims his smoker at you! Step aside!"),
        Z + "zwerm": ("Een boze zwerm zoemt achter %s aan! Lok hem door de rook!", "An angry swarm buzzes after %s! Lead it through the smoke!"),
        Z + "zwerm.suf": ("De zwerm wordt suf in de rook en valt in slaap.", "The swarm gets drowsy in the smoke and falls asleep."),
        Z + "bom": ("Hij gooit de grote kaasrookbom! Schuil met z'n allen in de gele cirkel bij de korf!",
                    "He throws the big cheese-smoke bomb! Everyone shelter in the yellow circle by the hive!"),
        Z + "bom.ok": ("De koningin spreidt haar vleugels: samen houden jullie het.", "The queen spreads her wings: together you hold."),
        Z + "bom.mis": ("Te weinig bij de korf: de kaasrook raakt iedereen!", "Too few by the hive: the cheese smoke hits everyone!"),
        Z + "weer_weg": ("De Mika-imker is weer gevlucht. De koningin zoemt tevreden.", "The Mika beekeeper fled again. The queen hums contentedly."),
        # speech (chat)
        Z + "zeg.weer": ("Jij weer?! Ik was net... aan het proeven. Voor het rapport.", "You again?! I was just... tasting. For the report."),
        Z + "zeg.fase2": ("Even bijvullen! Een scheutje rijpe kaas-honing voor Hare Majesteit.", "Time for a top-up! A splash of ripe cheese honey for Her Majesty."),
        Z + "zeg.fase3": ("Genoeg! De grote rookpot! Iedereen suf, behalve ik!", "Enough! The big smoker! Everybody drowsy, except me!"),
        Z + "zeg.au": ("AU! Niet mij! IK ben de imker!", "OW! Not me! I'M the beekeeper!"),
        Z + "zeg.vlucht": ("Dit komt in mijn rapport! Met drie uitroeptekens!", "This goes in my report! With three exclamation marks!"),
        # the intro (from the hatch)
        Z + "intro.0": ("In de raatzaal staat een Mika in een veel te groot imkerpak, met een rookpotje in de ene poot en een blik in de andere. Op zijn "
                        "pak glimt een speldje: een kroontje met een kaasgaatje. Het teken van de Keizer.",
                        "In the comb hall stands a Mika in a far too big beekeeper's suit, a smoker in one paw and a can in the other. A pin "
                        "gleams on his suit: a little crown with a cheese hole. The Emperor's sign."),
        Z + "intro.1": ("\"Niks aan de hand! Gewoon... honing proeven. Kwaliteitscontrole. Voor het rapport.\"",
                        "\"Nothing going on! Just... tasting honey. Quality control. For the report.\""),
        Z + "intro.2": ("\"Grauwe honing? Die is gewoon RIJP. Zoals kaas. Als de koningin grauw is, wordt alle honing van Honingtak rijp. De Keizer "
                        "houdt van rijp.\"",
                        "\"Grey honey? It's just RIPE. Like cheese. When the queen is grey, all of Honingtak's honey gets ripe. The Emperor "
                        "likes ripe.\""),
        Z + "intro.3": ("Achter hem zit de bijenkoningin in haar korf, grauw en stil. Het gezoem in de zaal klinkt opeens heel boos.",
                        "Behind him the bee queen sits in her hive, grey and silent. The buzzing in the hall suddenly sounds very angry."),
        Z + "intro.4": ("\"Jij hebt mijn sporen gevonden? Alle vijf? Dan kom jij ook in het rapport. Onder 'lastig'.\"",
                        "\"You found my clues? All five? Then you go in the report too. Under 'troublesome'.\""),
        Z + "intro.5": ("\"Rookpotjes aan! Bijtjes, AANVAL! ...Bijtjes? Hallo? Ik ben de imker!\"",
                        "\"Smokers on! Bees, ATTACK! ...Bees? Hello? I'm the beekeeper!\""),
        # after the fight
        Z + "na.0": ("De Mika-imker vlucht in een wolk kaasrook het luik uit. Zijn notitieboekje laat hij vallen.",
                     "The Mika beekeeper flees up the hatch in a cloud of cheese smoke. He drops his notebook."),
        Z + "na.1": ("\"Rapport aan de Keizer. Honingtak: grauw maken. Daarna: de Boom. Daarna: lunch.\"",
                     "\"Report to the Emperor. Honingtak: make grey. Then: the Tree. Then: lunch.\""),
        Z + "na.2": ("De rest van het boekje is een tekening van een kaas. Een heel mooie kaas, dat moet gezegd.",
                     "The rest of the notebook is a drawing of a cheese. A very nice cheese, it must be said."),
        Z + "na.3": ("In de korf schudt de koningin het grauw van haar vleugels. Ze is weer goud. En dan... zingt ze. De hele raatzaal zoemt mee.",
                     "In the hive the queen shakes the grey off her wings. She's gold again. And then... she sings. The whole comb hall hums along."),
        Z + "na.4": ("Een klein goudgeel bijtje landt op je neus en wil niet meer weg. Zoempje heeft jou uitgekozen.",
                     "A little golden bee lands on your nose and won't leave. Zoempje has picked you."),
        Z + "na.5": ("Breng het goede nieuws naar de Raatwever.", "Take the good news to the Comb Weaver."),
        # the pet
        "vadsrijk.huisdier.zoempje.naam": ("Zoempje", "Zoempje"),
        "vadsrijk.huisdier.zoempje.tekst": ("Een goudgeel bijtje van de bijenkoningin. Steekt nooit, zoemt altijd.",
                                            "A golden bee from the bee queen. Never stings, always hums."),
        "vadsrijk.huisdier.zoempje.bron": ("Versla Mika de Imker in de geheime raatzaal", "Beat Mika the Beekeeper in the secret comb hall"),
    }
    return t


# =====================================================================================================================
# writing everything
# =====================================================================================================================
def alles(w, a_dir, png):
    tex = os.path.join(a_dir, *TEX.split("/"))
    geo_dir = os.path.join(a_dir, "geckolib", "models", "entity")
    anim_dir = os.path.join(a_dir, "geckolib", "animations", "entity")
    bb = {}

    def model(id_, g, an, vb, textuur):
        w(os.path.join(geo_dir, id_ + ".geo.json"), g.json(vb))
        if an is not None:
            w(os.path.join(anim_dir, id_ + ".animation.json"), {"format_version": "1.8.0", **an})
        png(os.path.join(tex, textuur + ".png"), g.png())
        bb[id_] = (g.json(), g.png(), an)

    g, an = mika_imker(a_dir)
    model("vr_z3_imker_mika", g, an, (4, 4, 1), "mika_imker")
    g, an = korf(0.0)
    model("vr_z3_imker_korf", g, an, (3, 3, 1), "korf")
    for i, f in ((1, 0.5), (2, 1.0)):
        png(os.path.join(tex, f"korf_grauw{i}.png"), korf(f)[0].png())
    g, an = rookpot()
    model("vr_z3_imker_rookpot", g, an, (2, 2, 0.5), "rookpot")
    g, an = zoempje()
    model("vr_z3_imker_zoempje", g, None, (2, 2, 0.5), "zoempje")
    w(os.path.join(anim_dir, "vr_huisdier_zoempje.animation.json"), {"format_version": "1.8.0", **an})
    # the trophy
    item = "vr_z3_imker_trofee"
    png(os.path.join(a_dir, "textures", "item", item + ".png"), trofee_icoon())
    w(os.path.join(a_dir, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
    w(os.path.join(a_dir, "models", "item", item + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + item}})
    return bb


def blockbench(bb):
    d = os.path.join("blockbench", "vadsrijk", "z3")
    os.makedirs(d, exist_ok=True)
    for naam, (geo, im, an) in bb.items():
        with open(os.path.join(d, naam + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(bbmodel(geo, im, an or {"animations": {}}, naam), f)


def build(h):
    bb = alles(h.w, h.A, M._png)
    blockbench(bb)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    bb = alles(M._schrijf, a, M._png)
    blockbench(bb)
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        M._schrijf(p, data)
    print(f"vadsrijk_t_imker: {len(bb)} models, {len(t)} texts")


if __name__ == "__main__":
    _direct()
