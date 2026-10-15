"""
biomes3 slice "bouw-dal" (Java: feature/bio/bouwdal; English: tools/lang/en/c86_bio_bouw_dal.json; contract: CONTRACT_BIO.md).

The Klaterdal mini structures and het weebhuisje:
  bio_bouw_dal_bouw.py     the templates and the structures (guhs:bio_plek)
  bio_bouw_dal_blokken.py  the verzamelreeks (japan_*), the fixed things of the house (weeb_*), the four foods
  bio_bouw_dal_tekst.py    every text (the scene and its variants, the departure, the homecoming)
  here                     the two friends' looks (Evivads, Nielsvads) and the thee-guh, the four outfits, the proofs
The four outfits use bones the guh already has (outfit_suit, outfit_spelen_zweetband, outfit_oren_balto, outfit_bowtie):
no new bone, no new place on the guh's texture sheet.
"""
import os
import re

import numpy as np
from PIL import Image

from features import bio_bouw_dal_blokken as blokken
from features import bio_bouw_dal_bouw as bouw
from features import bio_bouw_dal_tekst as tekst
from features import bio_lib as lib
from features import sterrenwacht_hulp as hulp

CLOTHES = ["japan_kimono", "japan_hachimaki", "japan_kattenoortjes", "japan_strikje"]
ADVANCEMENTS = ["weeb_ontmoet", "weeb_eerste_reis", "weeb_cadeau", "japan_reeks_compleet", "japan_outfits_compleet", *blokken.REEKS]
NPC_MODELLEN = ["weeb_evivads", "weeb_nielsvads"]
BLOND, BLOND_LICHT, BRUIN, BRUIN_LICHT = (244, 210, 112), (252, 232, 160), (176, 128, 84), (200, 154, 108)


# =====================================================================================================================
# the outfits
# =====================================================================================================================
def clothes(rng, v):
    # (rng depends on this module's place in FEATURES: not used)
    def kimono():
        r = lib.rng("japan_kimono")
        a = v.fabric((226, 70, 110), r, 8)
        for (x, y) in ((4, 4), (20, 6), (12, 12), (26, 16), (6, 22), (18, 26), (28, 28), (2, 14)):      # little blossoms
            a[y:y + 2, x:x + 2] = (255, 236, 244)
            a[y - 1, x] = a[y + 2, x + 1] = a[y, x - 1] = a[y + 1, x + 2 if x + 2 < 32 else x] = (255, 190, 214)
        a[14:19, :] = v.fabric((244, 198, 70), r, 5)[14:19, :]                                           # the obi
        a[16, :] = (214, 150, 40)
        return np.clip(a, 0, 255)

    def hachimaki():
        r = lib.rng("japan_hachimaki")
        a = v.fabric((250, 248, 244), r, 4)
        for y in range(32):
            for x in range(32):
                if ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5 < 6:
                    a[y, x] = (220, 50, 60)
        return np.clip(a, 0, 255)

    def oortjes():
        r = lib.rng("japan_kattenoortjes")
        a = v.fabric((250, 246, 240), r, 5)
        for y in range(8, 30):
            half = (y - 8) * 0.5
            for x in range(32):
                if abs(x - 15.5) <= half and y < 28:
                    a[y, x] = (255, 170, 196)
        return np.clip(a, 0, 255)

    def strikje():
        r = lib.rng("japan_strikje")
        return np.clip(v.dots((214, 52, 62), (255, 250, 250), r, every=8, size=2), 0, 255)

    return {"japan_kimono": {"suit": kimono}, "japan_hachimaki": {"spelen_zweetband": hachimaki},
            "japan_kattenoortjes": {"balto_wolfsoor": oortjes}, "japan_strikje": {"bowtie": strikje}}


def icons(ic):
    kimono = ["....aa....aa....", "...abba..abba...", "..abbbbaabbbba..", ".abbbbwbbwbbbba.", ".abbbbbwwbbbbba.", ".aabbbbwbbbbbaa.",
              "...ayyyyyyyya...", "...ayyyyyyyya...", "...abbbbbbbba...", "...abwbbbbwba...", "...abbbbbbbba...", "...aaaaaaaaaa..."]
    band = ["................", "..aaaaaaaaaaaa..", ".awwwwwrrwwwwwa.", ".awwwwrrrrwwwwa.", ".awwwwwrrwwwwwa.", "..aaaaaaaaaaaa..",
            "...........awwa.", "..........awwa..", "..........aaa..."]
    oren = ["..a..........a..", ".awa........awa.", ".awpa......apwa.", "awppwa....awppwa", "awppwa....awppwa", "awwwwwaaaaawwwwa",
            ".aaaa.bbbbb.aaaa", "......bbbbb....."]
    strik = ["................", ".aaa........aaa.", "abbba......abbba", "abwbbaa..aabbwba", "abbbbbbaabbbbbba", "abbwbbbccbbbwbba",
             "abbbbbbaabbbbbba", "abwbbaa..aabbwba", "abbba......abbba", ".aaa........aaa."]
    return {
        "japan_kimono": ic.icon(ic.pad(kimono), {"a": (150, 34, 70), "b": (226, 70, 110), "w": (255, 236, 244), "y": (244, 198, 70)}),
        "japan_hachimaki": ic.icon(ic.pad(band), {"a": (170, 166, 160), "w": (250, 248, 244), "r": (220, 50, 60)}),
        "japan_kattenoortjes": ic.icon(ic.pad(oren), {"a": (170, 150, 150), "w": (250, 246, 240), "p": (255, 170, 196), "b": (60, 50, 60)}),
        "japan_strikje": ic.icon(ic.pad(strik), {"a": (140, 26, 36), "b": (214, 52, 62), "w": (255, 250, 250), "c": (244, 198, 70)}),
    }


# =====================================================================================================================
# the two friends and the thee-guh
# =====================================================================================================================
def _lokken(licht):
    def teken(block):
        for x in range(0, 32, 5):
            block[:, x, :3] = licht
    return teken


def _bril_rand(block):
    block[..., :3] = (44, 36, 46)


def _vlag(block):
    block[..., :3] = (250, 250, 250)
    for y in range(32):
        for x in range(32):
            if ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5 < 7:
                block[y, x, :3] = (220, 50, 60)


def _kraag(block):
    block[..., :3] = (236, 96, 140)
    block[0:6, :, :3] = (255, 244, 248)
    block[26:, :, :3] = (255, 244, 248)


def npcs(h):
    """Evivads: a tall girl guh with long blond hair, a pink bow and the collar of a little haori. Nielsvads: a little
    taller (GuhNpcEntity.Kind: 1.12 and 1.2), short light-brown hair, glasses, a traveller's rugzakje with a Japanese
    flag on it. Hair and glasses are bones on the head, so they turn with it."""
    c = hulp.cube
    # --- Evivads ---
    rng = lib.rng("npc_weeb_evivads")
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_weeb_evivads")
    sw = hulp.swatches(geo, ["haar", "strik", "kraag"])
    geo["bones"].append({"name": "evi_haar", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-7.3, 23.6, -7.4], [14.6, 2.2, 13.9], sw["haar"]), c([-5.8, 25.4, -5.3], [11.6, 1.2, 9.6], sw["haar"]),      # the top
        c([-7.9, 10.5, -5.0], [1.1, 13.6, 10.4], sw["haar"]), c([6.8, 10.5, -5.0], [1.1, 13.6, 10.4], sw["haar"]),     # long sides
        c([-7.2, 8.5, 5.6], [14.4, 16.4, 1.3], sw["haar"]),                                                              # long down the back
        c([-7.2, 22.4, -7.6], [6.2, 1.6, 0.6], sw["haar"]), c([-1.0, 22.9, -7.6], [8.2, 1.1, 0.6], sw["haar"]),        # the fringe, swept
        c([-7.6, 17.0, -7.5], [1.2, 6.6, 2.6], sw["haar"]), c([6.4, 17.0, -7.5], [1.2, 6.6, 2.6], sw["haar"])]})       # locks beside the face
    geo["bones"].append({"name": "evi_strik", "parent": "head", "pivot": [3, 26, -4], "cubes": [
        c([1.6, 25.6, -5.4], [2.2, 2.4, 1.0], sw["strik"]), c([4.6, 25.6, -5.4], [2.2, 2.4, 1.0], sw["strik"]),
        c([3.6, 26.1, -5.6], [1.2, 1.4, 1.4], sw["strik"])]})
    geo["bones"].append({"name": "evi_kraag", "parent": "body", "pivot": [0, 12, 0], "cubes": [
        c([-5.4, 10.4, -4.5], [10.8, 2.4, 9.0], sw["kraag"])]})
    hulp.save_geo(h, "guh_npc_weeb_evivads.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.93, sat=0.8, val=1.0)
    hulp.paint_swatch(a, sw["haar"], BLOND, rng, 8, _lokken(BLOND_LICHT))
    hulp.paint_swatch(a, sw["strik"], (246, 120, 170), rng, 5)
    hulp.paint_swatch(a, sw["kraag"], (236, 96, 140), rng, 4, _kraag)
    h.save(Image.fromarray(a), "entity", "npc_weeb_evivads.png")
    # --- Nielsvads ---
    rng = lib.rng("npc_weeb_nielsvads")
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_weeb_nielsvads")
    sw = hulp.swatches(geo, ["haar", "bril", "rugzak", "vlag", "riem"])
    geo["bones"].append({"name": "niels_haar", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-7.3, 23.6, -7.4], [14.6, 2.2, 13.9], sw["haar"]), c([-5.8, 25.4, -5.3], [11.6, 1.2, 9.6], sw["haar"]),
        c([-7.7, 19.6, -4.6], [0.9, 4.2, 9.6], sw["haar"]), c([6.8, 19.6, -4.6], [0.9, 4.2, 9.6], sw["haar"]),         # short sides
        c([-6.8, 17.6, 5.6], [13.6, 6.4, 1.0], sw["haar"]),                                                              # short at the back
        c([-6.6, 22.8, -7.6], [13.2, 1.2, 0.6], sw["haar"]), c([-2.0, 26.4, -4.5], [4.0, 1.0, 4.0], sw["haar"])]})     # a short fringe, a tuft
    bril = []
    for x in (-6.0, 1.2):                                               # two round-ish frames: only the rims, the eyes show
        bril += [c([x, 20.9, -8.1], [4.8, 0.6, 0.5], sw["bril"]), c([x, 17.6, -8.1], [4.8, 0.6, 0.5], sw["bril"]),
                 c([x, 17.6, -8.1], [0.6, 3.9, 0.5], sw["bril"]), c([x + 4.2, 17.6, -8.1], [0.6, 3.9, 0.5], sw["bril"])]
    bril += [c([-1.2, 19.6, -8.1], [2.4, 0.6, 0.5], sw["bril"]),        # the bridge and the two temples
             c([-7.7, 19.6, -8.0], [0.5, 0.7, 8.0], sw["bril"]), c([7.2, 19.6, -8.0], [0.5, 0.7, 8.0], sw["bril"])]
    geo["bones"].append({"name": "niels_bril", "parent": "head", "pivot": [0, 19, -7], "cubes": bril})
    geo["bones"].append({"name": "niels_rugzak", "parent": "body", "pivot": [0, 7, 4], "cubes": [
        c([-3.8, 3.4, 3.4], [7.6, 7.6, 3.4], sw["rugzak"]), c([-2.6, 5.0, 6.8], [5.2, 4.0, 0.3], sw["vlag"]),
        c([-3.6, 9.6, -4.3], [1.3, 1.6, 8.0], sw["riem"]), c([2.3, 9.6, -4.3], [1.3, 1.6, 8.0], sw["riem"])]})
    hulp.save_geo(h, "guh_npc_weeb_nielsvads.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.56, sat=0.55, val=1.0)
    hulp.paint_swatch(a, sw["haar"], BRUIN, rng, 8, _lokken(BRUIN_LICHT))
    hulp.paint_swatch(a, sw["bril"], (44, 36, 46), rng, 2, _bril_rand)
    hulp.paint_swatch(a, sw["rugzak"], (70, 110, 170), rng, 6)
    hulp.paint_swatch(a, sw["vlag"], (250, 250, 250), rng, 2, _vlag)
    hulp.paint_swatch(a, sw["riem"], (50, 80, 130), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_weeb_nielsvads.png")
    # --- the thee-guh of the tea house: a plain soft green guh ---
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.36, sat=0.6, val=1.0, only=h.pinkish).convert("RGBA"), "entity", "npc_dal_theeguh.png")


# =====================================================================================================================
def build(h):
    blokken.build(h)
    npcs(h)
    lib.teksten(h, tekst.TEKSTEN)
    for naam in ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{naam}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    bouw.build(h)
    # (a test room for the game tests: a floor to stand on)
    s = h.Structure((9, 1, 9))
    for x in range(9):
        for z in range(9):
            s.set(x, 0, z, "minecraft:pink_concrete" if (x + z) % 2 else "minecraft:white_concrete")
    s.save("weeb_test_vloer")
    selfcheck(h)


def _java(naam):
    return open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio", "bouwdal", naam), encoding="utf-8").read()


def selfcheck(h):
    A, D = h.A, h.D
    mis = []
    for b in list(blokken.REEKS) + list(blokken.WEEB):
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            mis.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL:
            mis.append(f"name of {b}")
    for b in blokken.REEKS:
        for pad in (f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json", f"{D}/advancement/quest/{b}.json"):
            if not os.path.exists(pad):
                mis.append(pad)
    for e in blokken.ETEN:
        if not os.path.exists(f"{A}/textures/item/{e}.png") or f"item.guhs.{e}" not in h.NL:
            mis.append(f"food {e}")
    for c in CLOTHES:
        if f"item.guhs.{c}" not in h.NL:
            mis.append(f"name of {c}")
    for n in NPC_MODELLEN + ["dal_theeguh"]:
        if not os.path.exists(f"{A}/textures/entity/npc_{n}.png") or f"entity.guhs.guh_npc.{n}" not in h.NL:
            mis.append(f"npc {n}")
    for s in bouw.STRUCTUREN:
        for pad in (f"{D}/worldgen/structure/{s}.json", f"{D}/worldgen/structure_set/{s}.json", f"{D}/worldgen/template_pool/{s}/start.json"):
            if not os.path.exists(pad):
                mis.append(pad)
    # the Java side names the same things
    huis, cadeaus = _java("WeebHuis.java"), _java("Cadeaus.java")
    sprekers = re.search(r"SPREKERS = \{([^}]*)\}", huis).group(1).replace('"', "").replace(" ", "").split(",")
    if sprekers != [wie for wie, _ in tekst.SCENES]:
        mis.append(f"WeebHuis.SPREKERS {sprekers} differ from the scenes")
    terug = re.search(r"TERUG_SPREKERS = \{([^}]*)\}", huis).group(1).replace('"', "").replace(" ", "").split(",")
    if terug != [wie for wie, _ in tekst.TERUG]:
        mis.append(f"WeebHuis.TERUG_SPREKERS {terug} differ from the texts")
    if f"VERTREK_VARIANTEN = {len(tekst.VERTREK)}" not in huis:
        mis.append("WeebHuis.VERTREK_VARIANTEN differs from the texts")
    for groep, namen in (("REEKS", list(blokken.REEKS)), ("ETEN", list(blokken.ETEN))):
        java = re.search(groep + r" = \{([^}]*)\}", cadeaus).group(1).replace('"', "").split(",")
        if [j.strip() for j in java] != namen:
            mis.append(f"Cadeaus.{groep} differs from the generator")
    check = _java("BouwDalCheck.java")
    for naam, bestand in (("dal_torii", "dal_torii_a"), ("dal_torii_water", "dal_torii_water"), ("dal_lantaarns", "dal_lantaarns_a"),
                          ("dal_boogbrug", "dal_boogbrug_a"), ("dal_theehuisje", "dal_theehuisje"), ("dal_zenhoek", "dal_zenhoek_a"),
                          ("dal_staptreden", "dal_staptreden"), ("weebhuisje", "weebhuisje")):
        nbt = h.read_nbt(f"{h.R}/data/guhs/structure/{bestand}.nbt")
        anker = [list(blk["pos"]) for blk in nbt["blocks"] if nbt["palette"][blk["state"]]["Name"] == "minecraft:jigsaw"]
        m = re.search(r'ANKER\.put\("' + naam + r'", new int\[\]\{(\d+), (\d+), (\d+)\}\)', check)
        if not m or anker != [[int(v) for v in m.groups()]]:
            mis.append(f"BouwDalCheck.ANKER {naam} differs from the template's jigsaw {anker}")
    if tekst.SCENES[0][1][0] != "Ohayo, guh-chan! Kom binnen, schoenen uit. Kawaii huisje hè?":
        mis.append("the approved first scene was changed")
    if mis:
        raise SystemExit(f"bio_bouw_dal: {mis}")
