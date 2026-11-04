"""
1.10 "Het Vadsrijk", zone 2 "De Splinterpieken", the bazen slice (Z2_BRIEF.md). Java: feature/vadsrijk/zone2/bazen.

Writes everything the three zone-2 bosses need (with the zone-1 bazen generator's helpers: Geo, painters, bbmodel):
  * GeckoLib models + animations (+ Blockbench copies in blockbench/vadsrijk/z2/):
      vr_z2_baas_galmwolf      root, body, head, kaak, oor_l, oor_r, pootvl, pootvr, pootal, pootar, staart, kristallen
                               (anims idle, walk, galm, sprong, adem)
      vr_z2_baas_spiegelzucht  root, body, rok, torso, head, kroon, arm_l, arm_r, spiegeltje (anims idle, walk, spiegel, worp, straal)
                               (her mirror images use the same model + texture)
      vr_z2_baas_boormeester   root, body, rups_l, rups_r, cabine, schoorsteen, arm (anims idle, walk, stoot, stoom, smelt)
      vr_z2_baas_boorkop       root, kop (anim draai) + 3 colours x (normal, overheated)
      vr_z2_baas_welp          root, body, head, staart, pootvl.. (anims idle, walk)
    The Mika-mijnwerkers use the existing mika.geo.json with a new texture.
  * textures in textures/vadsrijk/z2bazen/, block models/states/textures (ijspilaar, spiegel, lichtbron, kristalslot,
    kristalpoort), item icons (8 named items + 3 trophies), lang (nl = en).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_bazen.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

try:
    from features.vadsrijk_bazen import Geo, vlak, disc, lijn, kf, bbmodel, guh_gezicht, _icoon, _rng
except ImportError:  # standalone
    from vadsrijk_bazen import Geo, vlak, disc, lijn, kf, bbmodel, guh_gezicht, _icoon, _rng

TEX = "textures/vadsrijk/z2bazen"
Z = "vadsrijk.z2bazen."

# =====================================================================================================================
# texts (nl; en_us gets the same Dutch)
# =====================================================================================================================
TEKSTEN = {
    "entity.guhs.vr_z2_baas_galmwolf": "De Galmwolf",
    "entity.guhs.vr_z2_baas_spiegelzucht": "Vrouwe Spiegelzucht",
    "entity.guhs.vr_z2_baas_spiegelbeeld": "Vrouwe Spiegelzucht",
    "entity.guhs.vr_z2_baas_boormeester": "De Mika-boormeester",
    "entity.guhs.vr_z2_baas_boorkop": "Boorkop",
    "entity.guhs.vr_z2_baas_welp": "Sneeuwknabbelaar-welp",
    "entity.guhs.vr_z2_baas_mijnwerker": "Mika-mijnwerker",
    "block.guhs.vr_z2_baas_ijspilaar": "Bevroren pilaar",
    "block.guhs.vr_z2_baas_spiegel": "Spiegel",
    "block.guhs.vr_z2_baas_lichtbron": "Kristaloog",
    "block.guhs.vr_z2_baas_kristalslot": "Kristalslot",
    "block.guhs.vr_z2_baas_kristalpoort": "Kristalpoort",
    # titles
    Z + "galmwolf": "DE GALMWOLF",
    Z + "galmwolf.onder": "Wiens huil de bergen laat barsten",
    Z + "spiegelzucht": "VROUWE SPIEGELZUCHT",
    Z + "spiegelzucht.onder": "Vrouwe van de Spiegelburcht",
    Z + "boormeester": "DE MIKA-BOORMEESTER",
    Z + "boormeester.onder": "Boort alles kapot. Ook de gezelligheid.",
    Z + "te_ver": "Je staat buiten het gevecht: van daar raak je hem niet!",
    # Galmwolf
    Z + "gw.galm": "De Galmwolf zet een KRISTALGALM in! Verstop je achter een bevroren pilaar, of onderbreek hem!",
    Z + "gw.beschut": "De pilaar vangt de galm op... en barst.",
    Z + "gw.onderbroken": "Onderbroken! De Galmwolf verslikt zich in zijn eigen huil.",
    Z + "gw.roep": "De Galmwolf roept het ijs: nieuwe pilaren schieten omhoog bij de paarse cirkels!",
    Z + "gw.fase2": "HET IJS ANTWOORDT",
    Z + "gw.fase2.onder": "Hij roept nieuwe pilaren en zijn welpen",
    Z + "gw.fase3": "SNEEUWJACHT",
    Z + "gw.fase3.onder": "Een sneeuwstorm. Luister naar de galm!",
    Z + "gw.verslagen": "De Galmwolf spat uiteen in grauwe scherven die wegsmelten in de sneeuw.",
    # Spiegelzucht
    Z + "sz.split": "Vrouwe Spiegelzucht splitst in spiegelbeelden! Alleen zij heeft GEEN schaduw.",
    Z + "sz.mis": "Mis! Dat was een spiegelbeeld... het slaat terug.",
    Z + "sz.gevonden": "Gevonden! De spiegelbeelden barsten.",
    Z + "sz.straal": "Een kristaloog laadt een spiegelstraal! Draai de spiegels (rechtsklik) zodat hij HAAR raakt. Uit de paarse lijn!",
    Z + "sz.verblind": "De straal kaatst recht in haar gezicht! Vrouwe Spiegelzucht is verblind!",
    Z + "sz.regen": "Scherfregen op %s! Ga SAMEN in de gele cirkel staan.",
    Z + "sz.fase2": "DE LICHTVAL DRAAIT",
    Z + "sz.fase2.onder": "Alleen wat in het licht staat, heeft een schaduw",
    Z + "sz.fase3": "DE ZAAL VERSPLINTERT",
    Z + "sz.fase3.onder": "Pas op voor vallende scherven",
    Z + "sz.verslagen": "Vrouwe Spiegelzucht breekt in duizend grauwe scherven. Het licht in de zaal wordt warm en roze.",
    # Boormeester
    Z + "bm.naam": "De Mika-boormeester",
    Z + "bm.slot": "De boormachine is op slot. Er zit een rode knop met een kaasje erop... je weet nog niet hoe hij aangaat.",
    Z + "bm.koud": "Het pantser is ijskoud en dicht. De machine slaapt.",
    Z + "bm.kop.rood": "Rode boorkop",
    Z + "bm.kop.geel": "Gele boorkop",
    Z + "bm.kop.blauw": "Blauwe boorkop",
    Z + "bm.kop.heet": "OVERVERHIT",
    Z + "bm.goed": "%s oververhit! (%s van 3)",
    Z + "bm.fout": "VERKEERDE VOLGORDE! Stoom! Alle boorkoppen koelen af.",
    Z + "bm.afgekoeld": "De boorkoppen zijn afgekoeld. Het paneel kiest een nieuwe volgorde...",
    Z + "bm.smelt": "KERNSMELTING",
    Z + "bm.smelt.onder": "Zijn pantser smelt: sla nu!",
    Z + "bm.mijnwerkers": "Mika-mijnwerkers klimmen uit de schacht!",
    Z + "bm.steenregen": "Steenregen! Weg uit de rode cirkels!",
    Z + "bm.fase2": "VOLLE KRACHT VOORUIT",
    Z + "bm.fase2.onder": "Meer mijnwerkers, snellere boren",
    Z + "bm.fase3": "OVERDRUK",
    Z + "bm.fase3.onder": "De machine kraakt in al haar voegen",
    Z + "bm.cliff": "Pfff! Dit is nog niet voorbij! In het fort wacht de Opperboor... en die boort door ALLES heen! Njeheheh!",
    Z + "bm.cliff_titel": "WORDT VERVOLGD...",
    Z + "bm.cliff_onder": "De Mika-boormeester rijdt in een rookwolk terug naar het fort",
    # Spiegelburcht
    Z + "burcht": "DE SPIEGELBURCHT",
    Z + "burcht.onder": "Waar het licht verdwaalt",
    Z + "burcht.open": "De kristalsloten gloeien... de kristalpoort lost op in licht!",
    Z + "burcht.gang": "De Splinterzaal! Ontwijk de rode scherfbanen en versla alle golven.",
    Z + "burcht.golf": "Golf %s van %s!",
    Z + "burcht.gang_klaar": "De Splinterzaal is stil. De poort naar de grote zaal gaat open...",
    # tips (title + text)
    Z + "tip.galmwolf.titel": "De Galmwolf",
    Z + "tip.galmwolf": "Paars over de hele arena = Kristalgalm: ga ACHTER een bevroren pilaar staan (of sla hem hard genoeg om te onderbreken). Elke pilaar houdt maar twee galmen.",
    Z + "tip.spiegelzucht.titel": "Vrouwe Spiegelzucht",
    Z + "tip.spiegelzucht": "Spiegelbeelden: alleen zij heeft GEEN schaduw. Spiegelstraal: draai de spiegels zodat de paarse straal haar raakt. Geel = samen staan!",
    Z + "tip.boormeester.titel": "De Mika-boormeester",
    Z + "tip.boormeester": "Sla de boorkoppen tot ze oververhit raken, in de volgorde van het paneel. Verkeerde volgorde = stoom!",
    Z + "tip.boormeester_paneel.titel": "Het controlepaneel",
    Z + "tip.boormeester_paneel": "Kijk en luister: het paneel laat de boorkoppen één voor één oplichten. Laag = rood, midden = geel, hoog = blauw.",
    Z + "tip.boormeester_pantser.titel": "Dik pantser",
    Z + "tip.boormeester_pantser": "Op de machine zelf doe je weinig. Laat de boorkoppen in de goede volgorde oververhitten: dan smelt zijn pantser.",
    Z + "tip.spiegel.titel": "Spiegel",
    Z + "tip.spiegel": "Rechtsklik draait de spiegel: / , \\ of plat (het licht gaat er dan rechtdoor).",
    Z + "tip.splinterzaal.titel": "De Splinterzaal",
    Z + "tip.splinterzaal": "Rode banen = scherven die over de gang vliegen. Stap eruit voor de baan vol is!",
    # items
    "item.guhs.vr_z2_baas_galmklauw": "De Galmklauw",
    "item.guhs.vr_z2_baas_ijspelsmantel": "IJspelsmantel",
    "item.guhs.vr_z2_baas_spiegelstaf": "Spiegelstaf van Zucht",
    "item.guhs.vr_z2_baas_schaduwloze": "De Schaduwloze",
    "item.guhs.vr_z2_baas_spiegelkroon": "Spiegelkroontje van Zucht",
    "item.guhs.vr_z2_baas_boorhamer": "Oververhitte Boorhamer",
    "item.guhs.vr_z2_baas_stoomlaarzen": "Stoomlaarzen",
    "item.guhs.vr_z2_baas_boorbril": "Bril van de Boormeester",
    "item.guhs.vr_z2_baas_trofee_galmwolf": "Trofee: De Galmwolf",
    "item.guhs.vr_z2_baas_trofee_spiegelzucht": "Trofee: Vrouwe Spiegelzucht",
    "item.guhs.vr_z2_baas_trofee_boormeester": "Trofee: De Mika-boormeester",
    "vadsrijk.loot.z2baas_galmklauw": "De Galmklauw",
    "vadsrijk.loot.z2baas_ijspelsmantel": "IJspelsmantel",
    "vadsrijk.loot.z2baas_spiegelstaf": "Spiegelstaf van Zucht",
    "vadsrijk.loot.z2baas_schaduwloze": "De Schaduwloze",
    "vadsrijk.loot.z2baas_spiegelkroon": "Spiegelkroontje van Zucht",
    "vadsrijk.loot.z2baas_boorhamer": "Oververhitte Boorhamer",
    "vadsrijk.loot.z2baas_stoomlaarzen": "Stoomlaarzen",
    "vadsrijk.loot.z2baas_boorbril": "Bril van de Boormeester",
    "vadsrijk.loot.z2baas_galmklauw.flavour.0": "\"Wie hiermee slaat, hoort de bergen terugzingen.\"",
    "vadsrijk.loot.z2baas_ijspelsmantel.flavour.0": "\"Warm vanbinnen, ijskoud voor wie je aanraakt.\"",
    "vadsrijk.loot.z2baas_spiegelstaf.flavour.0": "\"Kijk niet te lang in de bol. Hij kijkt terug.\"",
    "vadsrijk.loot.z2baas_schaduwloze.flavour.0": "\"Het eerste schot zie je nooit aankomen.\"",
    "vadsrijk.loot.z2baas_spiegelkroon.flavour.0": "\"Wie jou slaat, slaat zichzelf. Meestal.\"",
    "vadsrijk.loot.z2baas_boorhamer.flavour.0": "\"Nog steeds warm. Niet in je broekzak stoppen.\"",
    "vadsrijk.loot.z2baas_stoomlaarzen.flavour.0": "\"Pssssjjjt! En weg ben je.\"",
    "vadsrijk.loot.z2baas_boorbril.flavour.0": "\"Met deze bril zie je precies waar het pijn doet.\"",
    "vadsrijk.bazen.effect.z2baas_galmklauw": "elke 5e klap galmt na: 40 % schade op alles binnen 3 blokken",
    "vadsrijk.bazen.effect.z2baas_ijspelsmantel": "15 % kans als je geraakt wordt: de aanvaller bevriest 2 s",
    "vadsrijk.bazen.effect.z2baas_spiegelstaf": "kritieke treffers kaatsen door naar een tweede vijand (30 %)",
    "vadsrijk.bazen.effect.z2baas_schaduwloze": "+25 % schade op een wezen dat nog vol vads is",
    "vadsrijk.bazen.effect.z2baas_spiegelkroon": "10 % kans: een klap kaatst half terug en jij voelt niks (20 s afkoelen)",
    "vadsrijk.bazen.effect.z2baas_boorhamer": "elke 3e klap zet het doelwit 3 s in brand",
    "vadsrijk.bazen.effect.z2baas_stoomlaarzen": "onder 30 % vads: een stoomstoot duwt vijanden weg + Snelpootjes (30 s afkoelen)",
    "vadsrijk.bazen.effect.z2baas_boorbril": "12 % kans bij een treffer: 4 s Vahoeg (20 s afkoelen)",
}

# =====================================================================================================================
# the models
# =====================================================================================================================
IJS = (196, 228, 255)
IJS_DONKER = (120, 168, 228)
KRISTAL_OOG = (150, 245, 255)


def facetten(a, W, H, f, r):
    """Crystal facets: light and dark diagonal streaks + a few sparkles."""
    if f == "down":
        return
    for _ in range(max(2, W * H // 300)):
        x0, y0 = r.uniform(0, W), r.uniform(0, H)
        lijn(a, x0, y0, x0 + r.uniform(-W * 0.3, W * 0.3), y0 + r.uniform(-H * 0.3, H * 0.3),
             (255, 255, 255) if r.random() < 0.5 else (140, 160, 210), dik=1.2)
    for _ in range(max(1, W * H // 500)):
        disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(0.8, 1.6), (255, 255, 255))


def wolf_ogen(W, H, f, glow=False, basis=IJS):
    a = np.zeros((H, W, 4), np.float32)
    if not glow:
        a[..., :3] = basis
        a[..., :3] += _rng(71).normal(0, 6, (H, W, 1))
        a[..., 3] = 255
    for x in (W * 0.28, W * 0.72):
        disc(a, x, H * 0.36, W * 0.11, KRISTAL_OOG, ry=H * 0.09)
        disc(a, x, H * 0.36, W * 0.05, (20, 60, 90) if not glow else KRISTAL_OOG)
    if not glow:  # an angry brow
        lijn(a, W * 0.14, H * 0.2, W * 0.4, H * 0.28, (60, 80, 130), dik=max(1.5, W * 0.04))
        lijn(a, W * 0.86, H * 0.2, W * 0.6, H * 0.28, (60, 80, 130), dik=max(1.5, W * 0.04))
    return np.clip(a, 0, 255).astype(np.uint8)


def galmwolf(modus="gewoon", s=1.0, naam="galmwolf"):
    """modus gewoon | glow (only eyes + crystals glow). s scales the whole model (the pup is s=0.5)."""
    g = Geo(naam, 256, 256, 2) if s >= 1 else Geo(naam, 128, 128, 2)
    leeg = modus == "glow"

    def w(p):
        return (lambda W, H, f: np.zeros((H, W, 4), np.uint8)) if leeg else p
    vacht = w(vlak(IJS, 10, 81, extra=facetten, verloop=IJS_DONKER))
    buik = w(vlak((230, 242, 255), 8, 82, verloop=(190, 214, 245)))
    kristal_kleur = (170, 230, 255)

    def kristal(W, H, f):
        a = vlak(kristal_kleur, 12, 83, extra=facetten, verloop=(90, 170, 240))(W, H, f)
        if leeg:
            a[..., :3] = np.clip(a[..., :3].astype(np.int32) + 40, 0, 255)
        return a
    gezicht = (lambda W, H, f: wolf_ogen(W, H, f, glow=True)) if leeg else (lambda W, H, f: wolf_ogen(W, H, f))

    def k(v):
        return [x * s for x in v]
    g.bone("root")
    g.bone("body", "root", k((0, 12, 0)))
    g.cube("body", k((-7, 10, -12)), k((14, 12, 24)), vacht, faces={"down": buik})
    g.bone("kristallen", "body", k((0, 22, 0)))
    for i, z in enumerate((-9, -4, 1, 6)):
        h = (7, 9, 8, 5)[i]
        g.cube("kristallen", k((-1.5, 21, z)), k((3, h, 3)), kristal, rotation=[(-15, 0, 10), (-5, 0, -12), (10, 0, 8), (20, 0, -6)][i])
    g.cube("kristallen", k((-5.5, 20, -6)), k((2.5, 5, 2.5)), kristal, rotation=(0, 0, 30))
    g.cube("kristallen", k((3, 20, -2)), k((2.5, 6, 2.5)), kristal, rotation=(0, 0, -30))
    g.bone("head", "body", k((0, 18, -12)))
    g.cube("head", k((-6, 13, -23)), k((12, 11, 11)), vacht, faces={"north": gezicht})
    g.bone("kaak", "head", k((0, 15, -23)))
    g.cube("kaak", k((-3.5, 13, -29)), k((7, 4, 6)), vacht)
    g.cube("kaak", k((-3.5, 17, -28.5)), k((7, 3, 5.5)), vacht)
    g.cube("kaak", k((-1, 18.5, -29.4)), k((2, 1.5, 1)), w(vlak((40, 50, 80), 4, 84)))
    for naam_oor, x in (("oor_l", 3), ("oor_r", -6)):
        g.bone(naam_oor, "head", k((x + 1.5, 24, -17)))
        g.cube(naam_oor, k((x, 24, -18)), k((3, 5, 2)), kristal)
    poten = (("pootvl", 4, -9), ("pootvr", -8, -9), ("pootal", 4, 6), ("pootar", -8, 6))
    for n, x, z in poten:
        g.bone(n, "body", k((x + 2, 12, z + 2)))
        g.cube(n, k((x, 0, z)), k((4, 12, 4)), vacht)
    g.bone("staart", "body", k((0, 20, 12)))
    g.cube("staart", k((-2, 17, 12)), k((4, 4, 10)), kristal, rotation=(25, 0, 0), pivot=k((0, 20, 12)))
    anims = {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 1.0: [1.02, 1.03, 1.0], 2.0: [1, 1, 1]}},
                    "staart": {"rotation": {0: [0, -10, 0], 1.0: [0, 10, 0], 2.0: [0, -10, 0]}},
                    "head": {"rotation": {0: [0, 0, 0], 1.0: [4, 0, 0], 2.0: [0, 0, 0]}}}),
        "walk": kf({"pootvl": {"rotation": {0: [-30, 0, 0], 0.3: [30, 0, 0], 0.6: [-30, 0, 0]}},
                    "pootar": {"rotation": {0: [-30, 0, 0], 0.3: [30, 0, 0], 0.6: [-30, 0, 0]}},
                    "pootvr": {"rotation": {0: [30, 0, 0], 0.3: [-30, 0, 0], 0.6: [30, 0, 0]}},
                    "pootal": {"rotation": {0: [30, 0, 0], 0.3: [-30, 0, 0], 0.6: [30, 0, 0]}},
                    "body": {"position": {0: [0, 0, 0], 0.15: [0, 1, 0], 0.3: [0, 0, 0], 0.45: [0, 1, 0], 0.6: [0, 0, 0]}},
                    "staart": {"rotation": {0: [10, 0, 0], 0.3: [-5, 0, 0], 0.6: [10, 0, 0]}}}),
        "galm": kf({"head": {"rotation": {0: [0, 0, 0], 0.4: [-50, 0, 0], 2.6: [-55, 0, 0], 3.0: [0, 0, 0]}},
                    "kaak": {"rotation": {0: [0, 0, 0], 0.4: [30, 0, 0], 2.6: [35, 0, 0], 3.0: [0, 0, 0]}},
                    "body": {"rotation": {0: [0, 0, 0], 0.4: [-12, 0, 0], 2.6: [-12, 0, 0], 3.0: [0, 0, 0]}},
                    "kristallen": {"scale": {0: [1, 1, 1], 1.4: [1.15, 1.3, 1.15], 2.8: [1.25, 1.45, 1.25], 3.0: [1, 1, 1]}}}, loop=False),
        "sprong": kf({"body": {"position": {0: [0, 0, 0], 0.5: [0, -3, 2], 0.9: [0, 2, -2], 1.4: [0, 0, 0]},
                               "rotation": {0: [0, 0, 0], 0.5: [8, 0, 0], 0.9: [-15, 0, 0], 1.4: [0, 0, 0]}},
                      "pootvl": {"rotation": {0.5: [40, 0, 0], 0.9: [-60, 0, 0], 1.4: [0, 0, 0]}},
                      "pootvr": {"rotation": {0.5: [40, 0, 0], 0.9: [-60, 0, 0], 1.4: [0, 0, 0]}}}, loop=False),
        "adem": kf({"head": {"rotation": {0: [0, 0, 0], 0.4: [15, 0, 0], 1.2: [10, 0, 0], 1.5: [0, 0, 0]}},
                    "kaak": {"rotation": {0: [0, 0, 0], 0.4: [40, 0, 0], 1.2: [40, 0, 0], 1.5: [0, 0, 0]}}}, loop=False),
    }}
    return g, anims


SZ_KRISTAL = (206, 206, 222)
SZ_SPIEGEL = (232, 238, 250)
SZ_OOG = (205, 150, 255)


def spiegelzucht(modus="gewoon"):
    g = Geo("spiegelzucht", 256, 256, 2)
    leeg = modus == "glow"

    def w(p):
        return (lambda W, H, f: np.zeros((H, W, 4), np.uint8)) if leeg else p

    def glans_spiegel(a, W, H, f, r):
        for i in range(3):
            lijn(a, W * (0.1 + i * 0.3), H * 0.05, W * (0.3 + i * 0.3), H * 0.95, (255, 255, 255), dik=max(1, W * 0.04))
    jurk = w(vlak((178, 176, 198), 8, 91, extra=facetten, verloop=(110, 108, 136)))
    spiegel = w(vlak(SZ_SPIEGEL, 6, 92, extra=glans_spiegel, verloop=(170, 186, 214)))
    huid = vlak((226, 224, 236), 5, 93)

    def gezicht(W, H, f):
        a = np.zeros((H, W, 4), np.float32)
        if not leeg:
            a[..., :3] = (226, 224, 236)
            a[..., :3] += _rng(94).normal(0, 3, (H, W, 1))
            a[..., 3] = 255
        for x in (W * 0.32, W * 0.68):
            disc(a, x, H * 0.45, W * 0.1, SZ_OOG, ry=H * 0.07)
            if not leeg:
                disc(a, x - W * 0.03, H * 0.43, W * 0.03, (255, 255, 255))
                lijn(a, x - W * 0.12, H * 0.32, x + W * 0.1, H * 0.36 if x < W / 2 else H * 0.3, (90, 80, 120), dik=1.2)
                disc(a, x, H * 0.62, W * 0.035, (170, 190, 255), ry=H * 0.06, alpha=0.8)  # a crystal tear
        if not leeg:
            lijn(a, W * 0.42, H * 0.78, W * 0.58, H * 0.76, (120, 90, 130), dik=1.2)
        return np.clip(a, 0, 255).astype(np.uint8)
    kroon = vlak((236, 220, 255), 10, 95, extra=facetten) if not leeg else vlak((210, 170, 255), 6, 95)
    g.bone("root")
    g.bone("body", "root", (0, 0, 0))
    g.bone("rok", "body", (0, 14, 0))
    g.cube("rok", (-7, 0, -7), (14, 6, 14), jurk)
    g.cube("rok", (-5.5, 6, -5.5), (11, 8, 11), jurk)
    for x, z in ((-7.5, -2), (6.5, -2), (-2, -7.5), (-2, 6.5)):
        g.cube("rok", (x, 1, z), (1, 9, 4), spiegel)  # mirror panels on the skirt
    g.bone("torso", "body", (0, 14, 0))
    g.cube("torso", (-4, 14, -3), (8, 10, 6), jurk)
    g.cube("torso", (-2.5, 17, -3.4), (5, 5, 0.5), spiegel)
    g.bone("head", "torso", (0, 24, 0))
    g.cube("head", (-3.5, 24, -3.5), (7, 8, 7), w(huid), faces={"north": gezicht})
    g.cube("head", (-4, 28, -4), (8, 5, 8), w(vlak((200, 200, 226), 6, 96, verloop=(150, 150, 190))), faces={"north": lambda W, H, f: np.zeros((H, W, 4), np.uint8)})
    g.bone("kroon", "head", (0, 33, 0))
    for i, (x, z, h) in enumerate(((-3, -3, 5), (2, -3, 4), (-0.5, -3.5, 7), (-3, 2, 4), (2, 2, 5))):
        g.cube("kroon", (x, 32, z), (1.5, h, 1.5), kroon, rotation=(0, 0, (-12, 10, 0, -8, 14)[i]))
    g.bone("arm_l", "torso", (5, 23, 0))
    g.cube("arm_l", (4, 13, -1), (2, 10, 2), w(huid))
    g.bone("arm_r", "torso", (-5, 23, 0))
    g.cube("arm_r", (-6, 13, -1), (2, 10, 2), w(huid))
    g.bone("spiegeltje", "arm_l", (5, 13, 0))
    g.cube("spiegeltje", (4.5, 9, -3), (1, 5, 6), spiegel)
    g.cube("spiegeltje", (4.6, 12, -0.5), (0.8, 2, 1), w(vlak((150, 140, 170), 4, 97)))
    anims = {"animations": {
        "idle": kf({"body": {"position": {0: [0, 0, 0], 1.5: [0, 1, 0], 3.0: [0, 0, 0]}},
                    "arm_l": {"rotation": {0: [0, 0, -5], 1.5: [0, 0, -10], 3.0: [0, 0, -5]}},
                    "arm_r": {"rotation": {0: [0, 0, 5], 1.5: [0, 0, 10], 3.0: [0, 0, 5]}},
                    "kroon": {"rotation": {0: [0, 0, 0], 1.5: [0, 8, 0], 3.0: [0, 0, 0]}}}),
        "walk": kf({"body": {"position": {0: [0, 0, 0], 0.5: [0, 1.5, 0], 1.0: [0, 0, 0]}},
                    "rok": {"rotation": {0: [0, -6, 0], 0.5: [0, 6, 0], 1.0: [0, -6, 0]}}}),
        "spiegel": kf({"arm_l": {"rotation": {0: [0, 0, 0], 0.3: [0, 0, -90], 1.0: [0, 0, -90], 1.3: [0, 0, 0]}},
                       "arm_r": {"rotation": {0: [0, 0, 0], 0.3: [0, 0, 90], 1.0: [0, 0, 90], 1.3: [0, 0, 0]}},
                       "body": {"rotation": {0: [0, 0, 0], 1.3: [0, 360, 0]}}}, loop=False),
        "worp": kf({"arm_l": {"rotation": {0: [0, 0, 0], 0.25: [-150, 0, 0], 0.45: [-40, 0, 0], 0.8: [0, 0, 0]}}}, loop=False),
        "straal": kf({"arm_l": {"rotation": {0: [0, 0, 0], 0.4: [-170, 0, -10], 3.2: [-170, 0, -10], 3.5: [0, 0, 0]}},
                      "arm_r": {"rotation": {0: [0, 0, 0], 0.4: [-170, 0, 10], 3.2: [-170, 0, 10], 3.5: [0, 0, 0]}},
                      "body": {"position": {0: [0, 0, 0], 0.4: [0, 3, 0], 3.2: [0, 3, 0], 3.5: [0, 0, 0]}}}, loop=False),
    }}
    return g, anims


IJZER = (92, 92, 104)
MIKA_ROOD = (200, 40, 56)


def boormeester():
    g = Geo("boormeester", 256, 256, 2)

    def klinknagels(a, W, H, f, r):
        for x in range(3, W, 8):
            for y in (3, H - 4):
                disc(a, x, y, 1.2, (150, 150, 160))
        for _ in range(W * H // 400):  # rust and soot
            disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(1, 3), (110, 70, 40), alpha=0.4)
    plaat = vlak(IJZER, 10, 101, extra=klinknagels, verloop=(64, 62, 72))

    def strepen(a, W, H, f, r):
        if f in ("north", "south", "east", "west"):
            for i in range(0, W + H, 10):
                lijn(a, i, 0, i - H, H, (30, 26, 30), dik=3)
    waarschuwing = vlak((240, 200, 40), 6, 102, extra=strepen)

    def rups(a, W, H, f, r):
        for y in range(0, H, 5):
            a[y:y + 2, :, :3] = (40, 40, 44)
    rupsband = vlak((70, 70, 76), 6, 103, extra=rups)

    def raam(W, H, f):
        a = vlak((150, 200, 230), 6, 104)(W, H, f).astype(np.float32)
        # the Mika driver behind the glass: dark round head, red eyes, a little pilot cap
        disc(a, W * 0.5, H * 0.62, W * 0.26, (230, 170, 190))
        disc(a, W * 0.4, H * 0.58, W * 0.06, MIKA_ROOD)
        disc(a, W * 0.6, H * 0.58, W * 0.06, MIKA_ROOD)
        a[int(H * 0.3):int(H * 0.42), int(W * 0.26):int(W * 0.74), :3] = (60, 50, 40)
        lijn(a, W * 0.4, H * 0.75, W * 0.6, H * 0.75, (90, 40, 50), dik=1.5)
        lijn(a, 0, 0, W * 0.3, H * 0.3, (230, 245, 255), dik=1.5)
        return np.clip(a, 0, 255).astype(np.uint8)
    g.bone("root")
    g.bone("body", "root")
    g.cube("body", (-11, 5, -14), (22, 10, 28), plaat)
    g.cube("body", (-11.2, 13, -14.2), (22.4, 2, 28.4), waarschuwing)
    for n, x in (("rups_l", 11), ("rups_r", -15)):
        g.bone(n, "body", (x + 2, 3, 0))
        g.cube(n, (x, 0, -15), (4, 7, 30), rupsband)
    g.bone("cabine", "body", (0, 15, 2))
    g.cube("cabine", (-6, 15, -3), (12, 10, 11), plaat, faces={"north": raam})
    g.cube("cabine", (-7, 25, -4), (14, 2, 13), vlak(MIKA_ROOD, 8, 105))
    g.bone("schoorsteen", "cabine", (4, 27, 6))
    g.cube("schoorsteen", (2.5, 25, 5), (3, 9, 3), vlak((50, 48, 56), 6, 106))
    g.bone("arm", "body", (0, 10, -14))
    g.cube("arm", (-10, 7, -19), (20, 6, 5), plaat)
    g.cube("arm", (-9, 8, -22), (4, 4, 4), plaat)
    g.cube("arm", (5, 8, -22), (4, 4, 4), plaat)
    g.cube("arm", (-2, 7, -24), (4, 4, 6), plaat)
    anims = {"animations": {
        "idle": kf({"body": {"position": {0: [0, 0, 0], 0.1: [0, 0.3, 0], 0.2: [0, 0, 0]}},
                    "schoorsteen": {"scale": {0: [1, 1, 1], 0.6: [1.15, 0.9, 1.15], 1.2: [1, 1, 1]}}}),
        "walk": kf({"body": {"position": {0: [0, 0, 0], 0.15: [0, 0.6, 0], 0.3: [0, 0, 0]}},
                    "cabine": {"rotation": {0: [0, 0, -2], 0.3: [0, 0, 2], 0.6: [0, 0, -2]}}}),
        "stoot": kf({"body": {"rotation": {0: [0, 0, 0], 0.5: [-6, 0, 0], 1.0: [10, 0, 0], 1.5: [0, 0, 0]}},
                     "arm": {"position": {0: [0, 0, 0], 0.5: [0, 0, 3], 1.0: [0, 0, -5], 1.5: [0, 0, 0]}}}, loop=False),
        "stoom": kf({"body": {"rotation": {0: [0, 0, 0], 0.1: [0, 0, 3], 0.2: [0, 0, -3], 0.3: [0, 0, 2], 0.4: [0, 0, 0]}},
                     "schoorsteen": {"scale": {0: [1, 1, 1], 0.2: [1.5, 1.4, 1.5], 0.6: [1, 1, 1]}}}, loop=False),
        "smelt": kf({"cabine": {"rotation": {0: [0, 0, 0], 0.5: [0, 0, 12], 7.5: [0, 0, 12], 8.0: [0, 0, 0]}},
                     "body": {"scale": {0: [1, 1, 1], 0.5: [1.05, 0.92, 1.05], 7.5: [1.05, 0.92, 1.05], 8.0: [1, 1, 1]}}}, loop=False),
    }}
    return g, anims


BOOR_KLEUREN = {"rood": (220, 56, 48), "geel": (240, 200, 50), "blauw": (60, 150, 230)}


def boorkop(kleur, heet=False):
    g = Geo("boorkop", 64, 64, 4)
    basis = BOOR_KLEUREN[kleur]
    if heet:
        basis = tuple(min(255, int(c * 0.5 + v)) for c, v in zip(basis, (140, 80, 20)))

    def spiraal(a, W, H, f, r):
        for i in range(-H, W, 5):
            lijn(a, i, H, i + H, 0, (40, 36, 40) if not heet else (255, 240, 160), dik=1.2)
    staal = vlak(basis, 8, 111, extra=spiraal, verloop=tuple(int(c * 0.7) for c in basis))
    g.bone("root")
    g.bone("kop", "root", (0, 8, 0))
    g.cube("kop", (-5, 3, -2), (10, 10, 4), vlak((80, 80, 92), 6, 112))
    g.cube("kop", (-4, 4, -6), (8, 8, 4), staal)
    g.cube("kop", (-3, 5, -10), (6, 6, 4), staal)
    g.cube("kop", (-2, 6, -13), (4, 4, 3), staal)
    g.cube("kop", (-1, 7, -15), (2, 2, 2), vlak((220, 220, 230), 4, 113))
    g.cube("kop", (-1.5, 6.5, 2), (3, 3, 6), vlak((70, 70, 80), 6, 114))
    anims = {"animations": {
        "draai": kf({"kop": {"rotation": {0: [0, 0, 0], 0.5: [0, 0, 180], 1.0: [0, 0, 360]}}}),
    }}
    return g, anims


# =====================================================================================================================
# the Mika-mijnwerker texture (on the existing mika.geo.json): dusty overalls, the face kept
# =====================================================================================================================
def mijnwerker_textuur(a_dir):
    bron = Image.open(os.path.join(a_dir, "textures", "entity", "mika.png")).convert("RGBA")
    k = bron.width // 128
    fx0, fy0, fx1, fy1 = 11 * k, 49 * k, 25 * k, 60 * k
    a = np.asarray(bron, np.float32).copy()
    gezicht = a[fy0:fy1, fx0:fx1].copy()
    a[..., :3] = a[..., :3] * 0.55 + np.array([120, 96, 60]) * 0.45   # dusty brown work clothes
    a[fy0:fy1, fx0:fx1] = gezicht
    vak = a[fy0:fy1, fx0:fx1]
    W, H = fx1 - fx0, fy1 - fy0
    vak[:max(1, int(H * 0.16)), :, :3] = (240, 196, 40)   # a yellow helmet rim
    disc(vak, W * 0.5, H * 0.08, max(1.5, W * 0.06), (255, 250, 200))   # the lamp
    for _ in range(12):  # soot on the cheeks
        disc(vak, _rng(121).uniform(0, W), _rng(122 + _).uniform(H * 0.6, H), 0.8, (70, 60, 50), alpha=0.5)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# =====================================================================================================================
# item icons and block textures
# =====================================================================================================================
def _iconen():
    O = (40, 30, 50, 255)
    ic = {}
    ic["vr_z2_baas_galmklauw"] = _icoon(lambda d: (d.line([(3, 13), (6, 10)], fill=(120, 90, 60, 255), width=2),
                                                   d.polygon([(5, 11), (13, 2), (14, 4), (7, 12)], fill=IJS + (255,), outline=O),
                                                   d.point([(10, 5), (12, 4)], fill=(255, 255, 255, 255)), d.line([(4, 9), (8, 13)], fill=(150, 200, 255, 255))))
    ic["vr_z2_baas_ijspelsmantel"] = _icoon(lambda d: (d.rectangle([3, 3, 12, 13], fill=(200, 226, 250, 255), outline=O),
                                                       d.rectangle([6, 3, 9, 5], fill=(0, 0, 0, 0)), d.line([(3, 12), (12, 12)], fill=(255, 255, 255, 255)),
                                                       d.point([(5, 8), (10, 7), (8, 10)], fill=(130, 180, 240, 255))))
    ic["vr_z2_baas_spiegelstaf"] = _icoon(lambda d: (d.line([(3, 14), (9, 8)], fill=(170, 160, 190, 255), width=2),
                                                     d.ellipse([7, 1, 15, 9], fill=SZ_SPIEGEL + (255,), outline=O),
                                                     d.line([(9, 3), (12, 6)], fill=(255, 255, 255, 255)), d.point([(13, 3)], fill=SZ_OOG + (255,))))
    ic["vr_z2_baas_schaduwloze"] = _icoon(lambda d: (d.line([(2, 13), (13, 2)], fill=(150, 140, 170, 255), width=2),
                                                     d.arc([3, 3, 15, 15], 180, 270, fill=(220, 220, 236, 255), width=2),
                                                     d.line([(6, 2), (14, 10)], fill=(240, 240, 255, 255)), d.point([(13, 2)], fill=(200, 255, 200, 255))))
    ic["vr_z2_baas_spiegelkroon"] = _icoon(lambda d: (d.polygon([(2, 12), (3, 5), (5, 9), (8, 2), (11, 9), (13, 5), (14, 12)], fill=(226, 220, 250, 255), outline=O),
                                                      d.ellipse([6, 8, 10, 12], fill=SZ_OOG + (255,)), d.point([(4, 10), (12, 10)], fill=(255, 255, 255, 255))))
    ic["vr_z2_baas_boorhamer"] = _icoon(lambda d: (d.line([(3, 14), (9, 8)], fill=(110, 80, 50, 255), width=2),
                                                   d.rectangle([7, 2, 14, 8], fill=IJZER + (255,), outline=O),
                                                   d.polygon([(14, 4), (16, 5), (14, 6)], fill=(255, 140, 40, 255)), d.point([(9, 4), (11, 6)], fill=(255, 120, 40, 255))))
    ic["vr_z2_baas_stoomlaarzen"] = _icoon(lambda d: (d.rectangle([3, 7, 6, 13], fill=(90, 80, 70, 255), outline=O),
                                                      d.rectangle([9, 7, 12, 13], fill=(90, 80, 70, 255), outline=O),
                                                      d.ellipse([1, 1, 8, 6], fill=(235, 235, 240, 210)), d.ellipse([8, 0, 15, 5], fill=(245, 245, 250, 210))))
    ic["vr_z2_baas_boorbril"] = _icoon(lambda d: (d.ellipse([1, 4, 7, 10], fill=(150, 210, 240, 255), outline=O),
                                                  d.ellipse([9, 4, 15, 10], fill=(150, 210, 240, 255), outline=O),
                                                  d.line([(7, 7), (9, 7)], fill=O), d.line([(0, 6), (1, 6)], fill=(80, 60, 40, 255)),
                                                  d.point([(3, 6), (11, 6)], fill=(255, 255, 255, 255))))

    def trofee(top):
        def t(d):
            d.rectangle([4, 12, 11, 14], fill=(200, 160, 80, 255), outline=O)
            d.rectangle([6, 10, 9, 11], fill=(220, 180, 90, 255))
            top(d)
        return _icoon(t)
    ic["vr_z2_baas_trofee_galmwolf"] = trofee(lambda d: (d.polygon([(3, 9), (5, 2), (7, 5), (9, 5), (11, 2), (13, 9), (8, 10)], fill=IJS + (255,), outline=O),
                                                         d.point([(6, 6), (10, 6)], fill=(60, 200, 240, 255))))
    ic["vr_z2_baas_trofee_spiegelzucht"] = trofee(lambda d: (d.ellipse([4, 1, 11, 9], fill=SZ_SPIEGEL + (255,), outline=O),
                                                             d.line([(6, 3), (9, 6)], fill=(255, 255, 255, 255))))
    ic["vr_z2_baas_trofee_boormeester"] = trofee(lambda d: (d.polygon([(3, 5), (12, 3), (12, 8), (3, 7)], fill=(240, 200, 50, 255), outline=O),
                                                            d.polygon([(12, 3), (15, 5), (12, 8)], fill=(200, 200, 210, 255))))
    return ic


def _blokken():
    def vul(kleur, seed, extra=None, verloop=None):
        return Image.fromarray(vlak(kleur, 12, seed, extra=extra, verloop=verloop)(16, 16, "north"), "RGBA")

    def barsten(a, W, H, f, r):
        lijn(a, 2, 1, 7, 8, (250, 255, 255), dik=1)
        lijn(a, 7, 8, 5, 15, (250, 255, 255), dik=1)
        lijn(a, 7, 8, 14, 11, (250, 255, 255), dik=1)
        lijn(a, 10, 0, 12, 5, (90, 140, 200), dik=1)

    def ijs(a, W, H, f, r):
        for _ in range(4):
            x = r.uniform(0, 16)
            lijn(a, x, 0, x + r.uniform(-4, 4), 16, (225, 245, 255), dik=1)

    def facet(a, W, H, f, r):
        lijn(a, 0, 8, 8, 0, (255, 255, 255), dik=1)
        lijn(a, 8, 16, 16, 8, (150, 140, 180), dik=1)

    def oog(a, W, H, f, r):
        disc(a, 8, 8, 5, (255, 250, 230))
        disc(a, 8, 8, 2.5, (200, 160, 255))
        disc(a, 7, 7, 1, (255, 255, 255))

    def spiegelglans(a, W, H, f, r):
        lijn(a, 2, 14, 14, 2, (255, 255, 255), dik=1.5)
        lijn(a, 6, 15, 15, 6, (255, 255, 255), dik=0.8)
    return {
        "ijspilaar": vul((176, 214, 250), 131, extra=ijs, verloop=(130, 180, 236)),
        "ijspilaar_barst": vul((176, 214, 250), 131, extra=lambda a, W, H, f, r: (ijs(a, W, H, f, r), barsten(a, W, H, f, r)), verloop=(130, 180, 236)),
        "spiegel": vul(SZ_SPIEGEL, 132, extra=spiegelglans, verloop=(176, 190, 220)),
        "spiegel_rand": vul((150, 146, 170), 133),
        "lichtbron": vul((200, 198, 214), 134, extra=facet),
        "lichtbron_oog": vul((200, 198, 214), 134, extra=oog),
        "kristalslot": vul((120, 110, 150), 135, extra=facet),
        "kristalslot_aan": vul((236, 180, 255), 136, extra=lambda a, W, H, f, r: (facet(a, W, H, f, r), disc(a, 8, 8, 4, (255, 240, 255), alpha=0.6))),
        "kristalpoort": vul((168, 150, 206), 137, extra=facet, verloop=(120, 104, 170)),
    }


def _spiegel_model(stand):
    """stand 0 "/", 1 "\\" (seen from above, north up), 2 flat. The pane is a staircase of small cubes (no rotations: exact)."""
    els = [{"from": [2, 0, 2], "to": [14, 2, 14], "faces": {f: {"texture": "#rand"} for f in ("north", "south", "east", "west", "up", "down")}}]
    if stand == 2:
        els.append({"from": [3, 2, 3], "to": [13, 3, 13], "faces": {"up": {"texture": "#spiegel"}, **{f: {"texture": "#rand"} for f in
                                                                                                    ("north", "south", "east", "west")}}})
    else:
        for i in range(8):
            x = 2 * i
            z = 14 - 2 * i if stand == 0 else 2 * i
            els.append({"from": [x, 2, z], "to": [x + 2, 15, z + 2],
                        "faces": {f: {"texture": "#spiegel"} for f in ("north", "south", "east", "west", "up")}})
    return {"parent": "minecraft:block/block", "textures": {"particle": "guhs:block/vr_z2_baas_spiegel", "spiegel": "guhs:block/vr_z2_baas_spiegel",
                                                             "rand": "guhs:block/vr_z2_baas_spiegel_rand"}, "elements": els}


# =====================================================================================================================
ITEMS_WAPEN = ["vr_z2_baas_galmklauw", "vr_z2_baas_spiegelstaf", "vr_z2_baas_schaduwloze", "vr_z2_baas_boorhamer"]


def _alles(w, a_dir, png):
    tex = os.path.join(a_dir, *TEX.split("/"))
    geo_dir = os.path.join(a_dir, "geckolib", "models", "entity")
    anim_dir = os.path.join(a_dir, "geckolib", "animations", "entity")
    bb = {}

    def model(id_, g, an, vb, textuur):
        w(os.path.join(geo_dir, id_ + ".geo.json"), g.json(vb))
        w(os.path.join(anim_dir, id_ + ".animation.json"), {"format_version": "1.8.0", **an})
        png(os.path.join(tex, textuur + ".png"), g.png())
        bb[id_] = (g.json(), g.png(), an)
    g, an = galmwolf()
    model("vr_z2_baas_galmwolf", g, an, (5, 5, 2), "galmwolf")
    png(os.path.join(tex, "galmwolf_glowmask.png"), galmwolf("glow")[0].png())
    g, an = galmwolf(s=0.5, naam="welp")
    model("vr_z2_baas_welp", g, an, (2, 2, 0.5), "welp")
    png(os.path.join(tex, "welp_glowmask.png"), galmwolf("glow", s=0.5, naam="welp")[0].png())
    g, an = spiegelzucht()
    model("vr_z2_baas_spiegelzucht", g, an, (3, 4, 1.5), "spiegelzucht")
    png(os.path.join(tex, "spiegelzucht_glowmask.png"), spiegelzucht("glow")[0].png())
    g, an = boormeester()
    model("vr_z2_baas_boormeester", g, an, (6, 5, 2), "boormeester")
    for kleur in BOOR_KLEUREN:
        for heet in (False, True):
            g, an = boorkop(kleur, heet)
            png(os.path.join(tex, f"boorkop_{kleur}{'_heet' if heet else ''}.png"), g.png())
            if kleur == "rood" and not heet:
                w(os.path.join(geo_dir, "vr_z2_baas_boorkop.geo.json"), g.json((2, 2, 0.5)))
                w(os.path.join(anim_dir, "vr_z2_baas_boorkop.animation.json"), {"format_version": "1.8.0", **an})
                bb["vr_z2_baas_boorkop"] = (g.json(), g.png(), an)
    png(os.path.join(tex, "mijnwerker.png"), mijnwerker_textuur(a_dir))
    # items
    for item, im in _iconen().items():
        png(os.path.join(a_dir, "textures", "item", item + ".png"), im)
        w(os.path.join(a_dir, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a_dir, "models", "item", item + ".json"),
          {"parent": "minecraft:item/handheld" if item in ITEMS_WAPEN else "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + item}})
    # blocks
    for naam, im in _blokken().items():
        png(os.path.join(a_dir, "textures", "block", "vr_z2_baas_" + naam + ".png"), im)
    mb = os.path.join(a_dir, "models", "block")
    bs = os.path.join(a_dir, "blockstates")
    for b in (0, 1):
        t = "guhs:block/vr_z2_baas_ijspilaar" + ("_barst" if b else "")
        w(os.path.join(mb, f"vr_z2_baas_ijspilaar_{b}.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": t}})
    w(os.path.join(bs, "vr_z2_baas_ijspilaar.json"), {"variants": {f"barst={b}": {"model": f"guhs:block/vr_z2_baas_ijspilaar_{b}"} for b in (0, 1)}})
    for s in (0, 1, 2):
        w(os.path.join(mb, f"vr_z2_baas_spiegel_{s}.json"), _spiegel_model(s))
    w(os.path.join(bs, "vr_z2_baas_spiegel.json"), {"variants": {f"stand={s}": {"model": f"guhs:block/vr_z2_baas_spiegel_{s}"} for s in (0, 1, 2)}})
    w(os.path.join(mb, "vr_z2_baas_lichtbron.json"), {"parent": "minecraft:block/orientable", "textures": {
        "top": "guhs:block/vr_z2_baas_lichtbron", "side": "guhs:block/vr_z2_baas_lichtbron", "front": "guhs:block/vr_z2_baas_lichtbron_oog"}})
    w(os.path.join(bs, "vr_z2_baas_lichtbron.json"), {"variants": {
        f"facing={f}": {"model": "guhs:block/vr_z2_baas_lichtbron", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    for aan in (False, True):
        t = "guhs:block/vr_z2_baas_kristalslot" + ("_aan" if aan else "")
        w(os.path.join(mb, f"vr_z2_baas_kristalslot_{'aan' if aan else 'uit'}.json"), {
            "parent": "minecraft:block/block", "textures": {"particle": t, "k": t},
            "elements": [{"from": [2, 0, 2], "to": [14, 12, 14], "faces": {f: {"texture": "#k"} for f in ("north", "south", "east", "west", "up", "down")}},
                         {"from": [5, 12, 5], "to": [11, 15, 11], "faces": {f: {"texture": "#k"} for f in ("north", "south", "east", "west", "up")}}]})
    w(os.path.join(bs, "vr_z2_baas_kristalslot.json"), {"variants": {
        "lit=false": {"model": "guhs:block/vr_z2_baas_kristalslot_uit"}, "lit=true": {"model": "guhs:block/vr_z2_baas_kristalslot_aan"}}})
    w(os.path.join(mb, "vr_z2_baas_kristalpoort.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/vr_z2_baas_kristalpoort"}})
    w(os.path.join(bs, "vr_z2_baas_kristalpoort.json"), {"variants": {"": {"model": "guhs:block/vr_z2_baas_kristalpoort"}}})
    return bb


def _blockbench(bb):
    d = os.path.join("blockbench", "vadsrijk", "z2")
    os.makedirs(d, exist_ok=True)
    for naam, (geo, im, an) in bb.items():
        with open(os.path.join(d, naam + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(bbmodel(geo, im, an, naam), f)


def _png(pad, im):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    im.save(pad)


def build(h):
    bb = _alles(h.w, h.A, _png)
    _blockbench(bb)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources")
    bb = _alles(_schrijf, os.path.join(a, "assets", "guhs"), _png)
    _blockbench(bb)
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_z2_bazen: models + textures + {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
