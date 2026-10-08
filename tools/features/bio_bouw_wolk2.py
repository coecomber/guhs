"""
biomes3 slice "bouw-wolk2" (Java: feature/bio/bouwwolk2; English: tools/lang/en/c89_bio_bouw_wolk2.json; contract: CONTRACT_BIO.md).

Three findable structures of the Wolkenweide (Superkompas tab "wonderen"), all guhs:bio_plek kind "lucht":

  - regenboogbrug: a rainbow you walk over, from a floating island to a bank of cloud; at its end the pot of kaasknabbels
    (block regenboogbrug_pot: a handful a day per player, and a few rainbow blocks).
  - wolkenkasteeltje: a half-ruined castle of cloud with the sleeping giant guh (entity reuzenguh: the guh's own model
    without its clothes and variant bones, five times as big) and his hoard (block wolkenkasteeltje_schat: one gouden
    knabbelkruimel a day per player, wolkenkasteeltje_kruimel: a trophy you can put down, and a rare snack).
  - bliksemsmidse: a dark thundercloud (bliksemsmidse_wolk with slab and stairs) with the smid-guh (NPC kind SMIDGUH),
    who trades wolkenpluis for cloud furniture; his own pieces: bliksemsmidse_wolkentafel, bliksemsmidse_wolkenplank and
    bliksemsmidse_onweerswolkje (a little cloud that rains on what stands under it).

The templates: bio_bouw_wolk2_bouw.py. The OGGs: bio_bouw_wolk2_geluid.py (run by hand, committed).
Everything random comes from lib.rng(name).
"""
import json
import os
import zipfile

import numpy as np
from PIL import Image

from features import bio_lib as lib
from features import bio_bouw_wolk2_bouw as bouw
from features import bio_wereld_plek as bio_plek
from features import sterrenwacht_hulp as hulp

DONKER = "bliksemsmidse_wolk"
BLOKKEN = [DONKER, DONKER + "_plaat", DONKER + "_trap", "bliksemsmidse_wolkentafel", "bliksemsmidse_wolkenplank", "bliksemsmidse_onweerswolkje",
           "regenboogbrug_pot", "wolkenkasteeltje_schat", "wolkenkasteeltje_kruimel"]
STRUCTUREN = list(bouw.STRUCTUREN)
QUEST = ["regenboogbrug_gevonden", "wolkenkasteeltje_gevonden", "bliksemsmidse_gevonden", "regenboogbrug_pot", "wolkenkasteeltje_langs_reus",
         "reuzenguh_weggeblazen", "smidguh_eerste_ruil"]
SOUNDS = {
    "reuzenguh.snurk": (["snurk1", "snurk2", "snurk3"], "Reus snurkt"),
    "reuzenguh.grom": (["grom1", "grom2"], "Reus gromt in zijn slaap"),
    "reuzenguh.nies": (["nies1", "nies2"], "Reus niest"),
    "bliksemsmidse.rommel": (["rommel1", "rommel2", "rommel3"], "Onweerswolk rommelt zachtjes"),
    "smidguh.hamer": (["hamer1", "hamer2"], "Smid-guh hamert"),
}
GELUID_MAP = "bio_bouw_wolk2"

TEKSTEN = {
    # --- blocks and items ---
    "block.guhs.bliksemsmidse_wolk": "Onweerswolk",
    "block.guhs.bliksemsmidse_wolk_plaat": "Onweerswolkplaat",
    "block.guhs.bliksemsmidse_wolk_trap": "Onweerswolktrap",
    "block.guhs.bliksemsmidse_wolkentafel": "Wolkentafel",
    "block.guhs.bliksemsmidse_wolkenplank": "Wolkenplank",
    "block.guhs.bliksemsmidse_onweerswolkje": "Onweerswolkje",
    "block.guhs.bliksemsmidse_onweerswolkje.lore": "Regent zachtjes op wat eronder staat. Bliksemt nooit echt, njeg.",
    "block.guhs.regenboogbrug_pot": "Pot kaasknabbels",
    "block.guhs.regenboogbrug_pot.lore": "Staat aan het einde van de regenboog. Elke dag weer vol.",
    "block.guhs.wolkenkasteeltje_schat": "Schat van de reus",
    "block.guhs.wolkenkasteeltje_schat.lore": "Een berg gouden kruimels. De reus telt ze. Zeggen ze.",
    "block.guhs.wolkenkasteeltje_kruimel": "Gouden knabbelkruimel",
    "block.guhs.wolkenkasteeltje_kruimel.lore": "Uit de schat van de slapende reus. Te mooi om op te eten. Bijna, njeg.",
    "entity.guhs.reuzenguh": "Reuzenguh",
    "entity.guhs.guh_npc.smidguh": "Smid-guh",
    # --- the structures in the Superkompas ---
    "structure.guhs.regenboogbrug": "Regenboogbrug",
    "structure.guhs.regenboogbrug.tooltip": "Een regenboog waar je overheen loopt. Aan het einde staat een pot.",
    "structure.guhs.wolkenkasteeltje": "Wolkenkasteeltje",
    "structure.guhs.wolkenkasteeltje.tooltip": "Een kasteeltje op de hoogste wolk. Er snurkt iets heel groots.",
    "structure.guhs.bliksemsmidse": "Bliksemsmidse",
    "structure.guhs.bliksemsmidse.tooltip": "Een donker wolkje dat rommelt. De smid-guh perst er wolken tot blokken.",
    # --- the pot ---
    "gui.guhs.regenboogbrug.pot.pak": "De pot aan het einde van de regenboog! Een handje kaasknabbels en een brokje regenboog voor jou. Vahoeg!",
    "gui.guhs.regenboogbrug.pot.leeg": "De pot is leeg voor vandaag. Morgen heeft de regenboog hem weer gevuld, njeg.",
    "gui.guhs.regenboogbrug.terug": "De regenboog is weer aangegroeid.",
    # --- the giant ---
    "gui.guhs.reuzenguh.slaapt.0": "Zzz... knabbel... zzz... nog vijf minuutjes, njeg...",
    "gui.guhs.reuzenguh.slaapt.1": "Zzz... wie zit er... aan mijn kruimels... zzz...",
    "gui.guhs.reuzenguh.slaapt.2": "Snurrrk... vads... snurrrk...",
    "gui.guhs.reuzenguh.loer": "...njeg?",
    "gui.guhs.reuzenguh.loer.tip": "De reus doet één oog open. Sssst! Sluipen, niet rennen of springen.",
    "gui.guhs.reuzenguh.weg.0": "HA... HA... HATSJOE-GUH! Je waait zo het kasteel uit. De reus draait zich om en snurkt verder.",
    "gui.guhs.reuzenguh.weg.1": "De reus gaapt een storm: VAHOEEEG. Daar lig je, buiten op een wolk. Zacht geland, njeg.",
    "gui.guhs.reuzenguh.weg.2": "Eén nies en je bent buiten. Hij heeft het niet eens gemerkt.",
    "gui.guhs.reuzenguh.weg.3": "Proest! Je vliegt als een pluisje naar buiten. Volgende keer op je tenen, vads?",
    "gui.guhs.wolkenkasteeltje.schat.pak": "Je pakt één gouden knabbelkruimel uit de schat van de reus. Hij snurkt gewoon door. Vahoeg!",
    "gui.guhs.wolkenkasteeltje.schat.leeg": "Eén kruimel per dag is genoeg. De reus telt ze, zeggen ze. Kom morgen terug, njeg.",
    # --- the smid-guh ---
    "gui.guhs.smidguh.hallo.0": "Tik. Tik. Wolk erin. Blok eruit. Njeg.",
    "gui.guhs.smidguh.hallo.1": "Pluis geven. Ik persen. Jij bouwen. Vads.",
    "gui.guhs.smidguh.hallo.2": "Heet hier. Bliksem doet het werk. Ik de rest. Tik.",
    "gui.guhs.smidguh.hallo.3": "Niet schrikken. Flits is lief. Rommel is buik. Njeg.",
    "gui.guhs.smidguh.geen_pluis": "Geen pluis? Schaapje knippen. Dan terugkomen. Tik.",
    "gui.guhs.smidguh.geruild": "Goed geperst. Stevig blok. Vahoeg. Tik.",
}


def _jar():
    return zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))


# =====================================================================================================================
# textures
# =====================================================================================================================
def _veld(naam, grofheid=2.2, n=16):
    """A soft field of n x n values 0..1 that tiles on all four sides."""
    f = lib.rng("bio_bouw_wolk2/" + naam).random((n, n))
    k = np.fft.fftfreq(n) * n
    g = np.exp(-(k[:, None] ** 2 + k[None, :] ** 2) / (2 * grofheid ** 2))
    v = np.real(np.fft.ifft2(np.fft.fft2(f) * g))
    return (v - v.min()) / (v.max() - v.min() + 1e-9)


def _meng(a, b, t):
    return np.asarray(a, np.float32)[None, None, :] * (1 - t[..., None]) + np.asarray(b, np.float32)[None, None, :] * t[..., None]


def _beeld(rgb, alpha=255):
    a = np.clip(np.rint(rgb), 0, 255).astype(np.uint8)
    al = np.full(a.shape[:2] + (1,), alpha, np.uint8) if np.isscalar(alpha) else np.clip(np.rint(alpha), 0, 255).astype(np.uint8)[..., None]
    return Image.fromarray(np.concatenate([a, al], axis=2), "RGBA")


# the thundercloud: (lightest, shadow) per face, painted light like the wolkenblok (its models are drawn unshaded)
DONKER_TINT = {"boven": ((158, 162, 186), (134, 138, 166)), "zij": ((132, 136, 166), (104, 108, 142)), "onder": ((96, 98, 134), (76, 78, 114))}
DONKER_BEELDEN = {"boven": 2, "zij": 3, "onder": 2}


def texturen(h):
    for vlak, n in DONKER_BEELDEN.items():
        licht, schaduw = DONKER_TINT[vlak]
        for i in range(n):
            v = 0.8 * _veld(f"donker_{vlak}_{i}", 2.0) + 0.2 * _veld(f"donker_{vlak}_{i}_fijn", 4.5)
            v = np.clip((v - 0.25) / 0.6, 0, 1) ** 1.3
            h.save(_beeld(_meng(licht, schaduw, v)), "block", f"{DONKER}_{vlak}_{i}.png")
    # the pot: dark iron with a soft sheen; the knabbels in it; gold for the hoard and the crumb
    v = _veld("pot", 2.6)
    h.save(_beeld(_meng((74, 70, 92), (40, 38, 56), v)), "block", "regenboogbrug_pot_ijzer.png")
    kaas = _meng((255, 214, 92), (240, 160, 48), np.clip(_veld("kaas", 4.0) * 1.2 - 0.1, 0, 1))
    r = lib.rng("bio_bouw_wolk2/kaasgaatjes")
    for _ in range(9):
        x, y = int(r.integers(1, 15)), int(r.integers(1, 15))
        kaas[y, x] = (214, 128, 30)
    h.save(_beeld(kaas), "block", "regenboogbrug_pot_kaas.png")
    goud = _meng((255, 236, 130), (232, 170, 40), np.clip(_veld("goud", 3.4) * 1.3 - 0.15, 0, 1))
    for _ in range(7):
        x, y = int(r.integers(0, 16)), int(r.integers(0, 16))
        goud[y, x] = (255, 252, 214)
    h.save(_beeld(goud), "block", "wolkenkasteeltje_goud.png")
    # rain for the onweerswolkje's underside: a few drops on a see-through sheet
    regen = np.zeros((16, 16, 4), np.uint8)
    for _ in range(11):
        x, y = int(r.integers(0, 16)), int(r.integers(0, 13))
        for d in range(3):
            regen[(y + d) % 16, x] = (150, 196, 255, 200 - 50 * d)
    h.save(Image.fromarray(regen, "RGBA"), "block", "bliksemsmidse_regen.png")
    geel = _meng((255, 246, 150), (255, 226, 90), _veld("bliksem", 3.0))
    h.save(_beeld(geel), "block", "bliksemsmidse_bliksem.png")


def deeltjes(h):
    """reuzenguh_zzz: a "z" in three sizes; bliksemsmidse_flits: two small soft flashes."""
    def letter(n, rijen, kleur=(236, 244, 255)):
        img = Image.new("RGBA", (n, n))
        for y, rij in enumerate(rijen):
            for x, c in enumerate(rij):
                if c == "#":
                    img.putpixel((x, y), kleur + (255,))
                elif c == "o":
                    img.putpixel((x, y), (120, 140, 200, 255))
        return img
    z_klein = ["........", ".ooooo..", ".o###o..", ".oo#oo..", ".o#ooo..", ".o###o..", ".ooooo..", "........"]
    z_mid = ["oooooooo", "o######o", "ooooo##o", "..oo##oo", ".oo##oo.", "oo##oooo", "o######o", "oooooooo"]
    z_groot = ["oooooooooooo....", "o##########o....", "o##########o....", "ooooooo###oo....", "....oo###oo.....", "...oo###oo......",
               "..oo###oo.......", ".oo###oo........", "oo###ooooooo....", "o##########o....", "o##########o....", "oooooooooooo....",
               "................", "................", "................", "................"]
    hulp.particle_frames(h, "reuzenguh_zzz", [letter(8, z_klein), letter(8, z_mid), letter(16, z_groot)])
    beelden = []
    for i in range(2):
        yy, xx = np.mgrid[0:16, 0:16]
        d = np.hypot(xx - 7.5, yy - 7.5)
        gloed = np.clip(1 - d / 7.5, 0, 1) ** 1.6
        # a little zigzag through the glow
        zig = np.zeros((16, 16))
        x = 7 + i
        for y in range(2, 14):
            if y in (5, 9, 11):
                x += 2 if (y + i) % 2 else -2
            zig[y, max(0, min(15, x))] = 1
            zig[y, max(0, min(15, x + 1))] = 0.7
        al = np.clip(gloed * 150 + zig * 255, 0, 255)
        rgb = _meng((255, 252, 220), (255, 240, 150), np.clip(d / 8, 0, 1))
        beelden.append(_beeld(rgb, al))
    hulp.particle_frames(h, "bliksemsmidse_flits", beelden)


# =====================================================================================================================
# block models
# =====================================================================================================================
KUBUS = [{"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "faces": {
    "down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#top", "cullface": "up"},
    **{f: {"texture": "#side", "cullface": f} for f in ("north", "east", "south", "west")}}}]


def donker_modellen(h, z):
    A, w = h.A, h.w
    sjabloon = {n: json.loads(z.read(f"assets/minecraft/models/block/{n}.json")) for n in ("slab", "slab_top", "stairs", "inner_stairs", "outer_stairs")}

    def tex(i):
        return {"top": f"guhs:block/{DONKER}_boven_{i % 2}", "side": f"guhs:block/{DONKER}_zij_{i}", "bottom": f"guhs:block/{DONKER}_onder_{i % 2}",
                "particle": f"guhs:block/{DONKER}_zij_{i}"}
    for i in range(3):
        w(f"{A}/models/block/{DONKER}_{i}.json", {"parent": "minecraft:block/block", "textures": tex(i), "elements": KUBUS})
    w(f"{A}/blockstates/{DONKER}.json", {"variants": {"": [{"model": f"guhs:block/{DONKER}_{i}", **({"y": y} if y else {})}
                                                             for i in range(3) for y in (0, 90)]}})
    w(f"{A}/models/item/{DONKER}.json", {"parent": f"guhs:block/{DONKER}_0"})
    for ours, van in ((f"{DONKER}_plaat", "slab"), (f"{DONKER}_plaat_top", "slab_top"), (f"{DONKER}_trap", "stairs"),
                      (f"{DONKER}_trap_inner", "inner_stairs"), (f"{DONKER}_trap_outer", "outer_stairs")):
        model = {"parent": "minecraft:block/block", "textures": tex(0), "elements": [dict(e, shade=False) for e in sjabloon[van]["elements"]]}
        if "display" in sjabloon[van]:
            model["display"] = sjabloon[van]["display"]
        w(f"{A}/models/block/{ours}.json", model)
    state = json.dumps(json.loads(z.read("assets/minecraft/blockstates/oak_slab.json")))
    state = state.replace("minecraft:block/oak_slab_top", f"guhs:block/{DONKER}_plaat_top").replace("minecraft:block/oak_slab", f"guhs:block/{DONKER}_plaat")
    state = state.replace("minecraft:block/oak_planks", f"guhs:block/{DONKER}_0")
    w(f"{A}/blockstates/{DONKER}_plaat.json", json.loads(state))
    state = json.dumps(json.loads(z.read("assets/minecraft/blockstates/oak_stairs.json")))
    state = state.replace("minecraft:block/oak_stairs", f"guhs:block/{DONKER}_trap")
    w(f"{A}/blockstates/{DONKER}_trap.json", json.loads(state))
    w(f"{A}/models/item/{DONKER}_plaat.json", {"parent": f"guhs:block/{DONKER}_plaat"})
    w(f"{A}/models/item/{DONKER}_trap.json", {"parent": f"guhs:block/{DONKER}_trap"})


# shapes (facing north), the same boxes as the Java side's VoxelShapes (BouwWolk2Slice)
VORMEN = {
    "bliksemsmidse_wolkentafel": [[1, 11, 1, 15, 14, 15], [5, 0, 5, 11, 11, 11]],
    "bliksemsmidse_wolkenplank": [[0, 3, 9, 16, 6, 16], [0, 11, 9, 16, 14, 16]],
    "bliksemsmidse_onweerswolkje": [[3, 8, 3, 13, 14, 13]],
    "regenboogbrug_pot": [[2, 0, 2, 14, 12, 14]],
    "wolkenkasteeltje_schat": [[0, 0, 0, 16, 9, 16]],
    "wolkenkasteeltje_kruimel": [[5, 0, 5, 11, 5, 11]],
}


def meubel_modellen(h):
    A, w = h.A, h.w
    W = {vlak: f"guhs:block/wolkenblok_wit_meubel_{vlak}" for vlak in ("boven", "zij", "kant", "onder")}
    R = {vlak: f"guhs:block/wolkenblok_roze_meubel_{vlak}" for vlak in ("boven", "zij", "kant", "onder")}

    def wolk(frm, to, t, gloed=0):
        e = {"from": frm, "to": to, "shade": False, "faces": {
            "up": {"texture": t["boven"]}, "down": {"texture": t["onder"]}, "north": {"texture": t["zij"]}, "south": {"texture": t["zij"]},
            "west": {"texture": t["kant"]}, "east": {"texture": t["kant"]}}}
        if gloed:
            e["light_emission"] = gloed
        return e

    def blok(naam, elements, particle, extra=None):
        # biomes3 eindfix: a face names a texture VARIABLE ("#t0"), never a path: with a path in a face the game logs
        # "Missing texture references" and draws the purple-and-black block (the files were all there)
        textures, elements = {"particle": particle}, json.loads(json.dumps(elements))
        namen = {}
        for e in elements:
            for vlak in e.get("faces", {}).values():
                if not vlak["texture"].startswith("#"):
                    var = namen.setdefault(vlak["texture"], f"t{len(namen)}")
                    textures[var] = vlak["texture"]
                    vlak["texture"] = "#" + var
        model = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
        if extra:
            model.update(extra)
        w(f"{A}/models/block/{naam}.json", model)
        w(f"{A}/blockstates/{naam}.json", {"variants": h.facing_states(naam)})
        w(f"{A}/models/item/{naam}.json", {"parent": f"guhs:block/{naam}"})

    # the cloud table: a thick round top on one fat leg, a pink doily
    blok("bliksemsmidse_wolkentafel", [
        wolk([5, 0, 5], [11, 2, 11], W), wolk([6, 2, 6], [10, 11, 10], W),
        wolk([1, 11, 1], [15, 14, 15], W), wolk([0, 11.5, 3], [16, 13.5, 13], W), wolk([3, 11.5, 0], [13, 13.5, 16], W),
        wolk([4, 14, 4], [12, 14.5, 12], R)], W["zij"])
    # the cloud shelf: two boards of cloud against the wall behind it (south side), puffs as brackets
    blok("bliksemsmidse_wolkenplank", [
        wolk([0, 3, 9], [16, 6, 16], W), wolk([0, 11, 9], [16, 14, 16], W),
        wolk([1, 0.5, 13], [4, 3, 16], R), wolk([12, 0.5, 13], [15, 3, 16], R), wolk([1, 8.5, 13], [4, 11, 16], R), wolk([12, 8.5, 13], [15, 11, 16], R)],
        W["zij"])
    # the little thundercloud: a dark puff that floats, a yellow flash peeking out, a sheet of rain under it
    D = {"boven": f"guhs:block/{DONKER}_boven_0", "zij": f"guhs:block/{DONKER}_zij_0", "kant": f"guhs:block/{DONKER}_zij_1", "onder": f"guhs:block/{DONKER}_onder_0"}
    G = {v: "guhs:block/bliksemsmidse_bliksem" for v in ("boven", "zij", "kant", "onder")}
    regen = [{"from": [8, -8, 3], "to": [8, 8, 13], "shade": False, "faces": {"west": {"texture": "guhs:block/bliksemsmidse_regen"},
                                                                               "east": {"texture": "guhs:block/bliksemsmidse_regen"}}},
             {"from": [3, -8, 8], "to": [13, 8, 8], "shade": False, "faces": {"north": {"texture": "guhs:block/bliksemsmidse_regen"},
                                                                               "south": {"texture": "guhs:block/bliksemsmidse_regen"}}}]
    blok("bliksemsmidse_onweerswolkje", [
        wolk([3, 8, 4], [13, 13, 12], D), wolk([4, 9, 3], [12, 12, 13], D), wolk([5, 13, 5], [10, 15, 10], D), wolk([8, 12.5, 7], [12.5, 14.5, 11.5], D),
        wolk([6.5, 5, 7.5], [8, 8, 8.5], G, 15), wolk([8, 3, 7.5], [9.5, 6, 8.5], G, 15)] + regen,
        D["zij"], {"render_type": "minecraft:cutout"})
    # the pot: an iron cauldron on three stubby feet, heaped with knabbels
    ijzer, kaas = "guhs:block/regenboogbrug_pot_ijzer", "guhs:block/regenboogbrug_pot_kaas"
    el = h.el
    blok("regenboogbrug_pot", [
        el([3, 1, 3], [13, 10, 13], ijzer), el([2, 3, 4], [14, 9, 12], ijzer), el([4, 3, 2], [12, 9, 14], ijzer),
        el([2, 10, 2], [14, 12, 14], ijzer), el([3, 0, 3], [5, 1, 5], ijzer), el([11, 0, 3], [13, 1, 5], ijzer), el([7, 0, 11], [9, 1, 13], ijzer),
        el([3, 12, 3], [13, 13, 13], kaas), el([4, 13, 5], [11, 14.5, 12], kaas), el([6, 14.5, 6], [10, 16, 10], kaas),
        el([1, 12, 6], [3, 13.5, 9], kaas), el([12, 12, 9], [15, 13, 12], kaas)], ijzer)
    # the hoard you take from: a heap of golden crumbs
    goud = "guhs:block/wolkenkasteeltje_goud"
    blok("wolkenkasteeltje_schat", [
        el([0, 0, 0], [16, 3, 16], goud), el([1, 3, 2], [14, 6, 15], goud), el([3, 6, 4], [12, 9, 13], goud), el([5, 9, 6], [10, 11.5, 11], goud),
        el([12, 3, 0], [16, 5, 5], goud), el([0, 3, 9], [3, 5, 13], goud)], goud)
    # the crumb: a lumpy golden nugget
    blok("wolkenkasteeltje_kruimel", [
        el([5, 0, 5], [11, 3, 11], goud), el([6, 3, 6], [10.5, 5, 10], goud), el([4.5, 0, 7], [6, 2, 9.5], goud), el([10, 0, 6.5], [12, 2.5, 9], goud)],
        goud, {"display": {"gui": {"rotation": [25, 35, 0], "translation": [0, 4.5, 0], "scale": [1.6, 1.6, 1.6]},
                           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
                           "fixed": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [1.3, 1.3, 1.3]},
                           "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [0.7, 0.7, 0.7]},
                           "firstperson_righthand": {"rotation": [0, 35, 0], "translation": [0, 5, 0], "scale": [0.9, 0.9, 0.9]}}})


# =====================================================================================================================
# the giant and the smid-guh
# =====================================================================================================================
BASIS_BOTTEN = ("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")


def reus(h):
    """
    The giant is the guh itself: its ten plain bones out of guh.geo.json (no clothes, no variant bones), drawn five times as
    big by the renderer, with the guh's own texture (closed eyes: guh_slaap/guh.png) and one of his own with one eye open.
    """
    A = h.A
    bron = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    g = bron["minecraft:geometry"][0]
    botten = [b for b in g["bones"] if b["name"] in BASIS_BOTTEN]
    if len(botten) != len(BASIS_BOTTEN):
        raise SystemExit("bio_bouw_wolk2: guh.geo.json misses a plain bone of the guh")
    geo = {"format_version": bron["format_version"], "minecraft:geometry": [{
        "description": dict(g["description"], identifier="geometry.reuzenguh"), "bones": botten}]}
    h.w(os.path.join(A, "geckolib", "models", "entity", "reuzenguh.geo.json"), geo)
    # one eye open: the sleeping face, with the left half of the eyes' strip from the waking face
    wakker = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA"))
    slaap = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh_slaap", "guh.png")).convert("RGBA")).copy()
    anders = np.abs(wakker.astype(int) - slaap.astype(int)).sum(2) > 0
    ys, xs = np.nonzero(anders)
    if len(xs) == 0:
        raise SystemExit("bio_bouw_wolk2: the guh's sleeping texture has no closed eyes")
    midden = (int(xs.min()) + int(xs.max()) + 1) // 2
    deel = anders & (np.arange(anders.shape[1])[None, :] < midden)
    slaap[deel] = wakker[deel]
    h.save(Image.fromarray(slaap, "RGBA"), "entity", "reuzenguh_loer.png")
    # his own few animations (the bones are the guh's; lengths in seconds)
    slapen = {"root": {"position": {"0.0": [0, -1, 0]}},
              "body": {"scale": {"0.0": [1.06, 0.86, 1.02], "1.7": [1.13, 0.97, 1.06], "2.6": [1.13, 0.97, 1.06], "4.2": [1.06, 0.86, 1.02]}},
              "head": {"rotation": {"0.0": [0, 0, 14], "1.7": [0, 0, 18], "2.6": [0, 0, 18], "4.2": [0, 0, 14]},
                       "scale": {"0.0": [1, 1, 1], "1.7": [1.03, 1.03, 1.02], "4.2": [1, 1, 1]}},
              "ear_left": {"rotation": {"0.0": [0, 0, 38], "3.0": [0, 0, 38], "3.15": [0, 0, 26], "3.3": [0, 0, 38], "4.2": [0, 0, 38]}},
              "ear_right": {"rotation": {"0.0": [0, 0, -38]}},
              "leg_front_left": {"position": {"0.0": [0, 0.6, 1]}}, "leg_front_right": {"position": {"0.0": [0, 0.6, 1]}},
              "leg_back_left": {"position": {"0.0": [0, 0.6, -1]}}, "leg_back_right": {"position": {"0.0": [0, 0.6, -1]}},
              "tail": {"rotation": {"0.0": [0, 50, 0], "2.1": [0, 44, 0], "4.2": [0, 50, 0]}}}
    loer = {"root": {"position": {"0.0": [0, -1, 0]}},
            "body": {"scale": {"0.0": [1.08, 0.9, 1.03], "1.0": [1.1, 0.93, 1.04], "2.0": [1.08, 0.9, 1.03]}},
            "head": {"rotation": {"0.0": [0, 0, 14], "0.35": [-6, 8, 4], "1.6": [-6, -6, 4], "2.0": [-6, 8, 4]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 38], "0.3": [0, 0, 4], "0.5": [0, 0, 12], "2.0": [0, 0, 12]}},
            "ear_right": {"rotation": {"0.0": [0, 0, -38], "0.3": [0, 0, -30]}},
            "leg_front_left": {"position": {"0.0": [0, 0.6, 1]}}, "leg_front_right": {"position": {"0.0": [0, 0.6, 1]}},
            "leg_back_left": {"position": {"0.0": [0, 0.6, -1]}}, "leg_back_right": {"position": {"0.0": [0, 0.6, -1]}},
            "tail": {"rotation": {"0.0": [0, 50, 0], "0.5": [0, 10, 0], "1.0": [0, 30, 0], "1.5": [0, 10, 0], "2.0": [0, 30, 0]}}}
    nies = {"root": {"position": {"0.0": [0, -1, 0], "0.7": [0, 0.5, 1.5], "0.85": [0, -1, -2], "1.5": [0, -1, 0]},
                     "rotation": {"0.0": [0, 0, 0], "0.7": [-10, 0, 0], "0.85": [9, 0, 0], "1.1": [0, 0, 3], "1.25": [0, 0, -3], "1.5": [0, 0, 0]}},
            "body": {"scale": {"0.0": [1.06, 0.9, 1.02], "0.7": [1.24, 1.2, 1.1], "0.85": [0.94, 0.84, 1.0], "1.5": [1.06, 0.88, 1.02]}},
            "head": {"rotation": {"0.0": [-6, 0, 4], "0.35": [-18, 0, 0], "0.7": [-30, 0, 0], "0.85": [22, 0, 0], "1.15": [6, 0, 8], "1.5": [0, 0, 14]},
                     "scale": {"0.0": [1, 1, 1], "0.7": [1.08, 1.14, 1.05], "0.85": [1.0, 0.94, 1.08], "1.5": [1, 1, 1]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 12], "0.7": [0, -30, 0], "0.85": [0, 30, 40], "1.5": [0, 0, 38]}},
            "ear_right": {"rotation": {"0.0": [0, 0, -30], "0.7": [0, 30, 0], "0.85": [0, -30, -40], "1.5": [0, 0, -38]}},
            "leg_front_left": {"position": {"0.0": [0, 0.6, 1]}}, "leg_front_right": {"position": {"0.0": [0, 0.6, 1]}},
            "leg_back_left": {"position": {"0.0": [0, 0.6, -1]}}, "leg_back_right": {"position": {"0.0": [0, 0.6, -1]}},
            "tail": {"rotation": {"0.0": [0, 30, 0], "0.7": [40, 0, 0], "0.85": [-20, 0, 0], "1.5": [0, 50, 0]}}}
    h.w(os.path.join(A, "geckolib", "animations", "entity", "reuzenguh.animation.json"), {"format_version": "1.8.0", "animations": {
        "slaap": {"loop": True, "animation_length": 4.2, "bones": slapen},
        "loer": {"loop": True, "animation_length": 2.0, "bones": loer},
        "nies": {"loop": "hold_on_last_frame", "animation_length": 1.5, "bones": nies}}})


def smid(h):
    """The smid-guh: a slate-grey sitting guh with soot on his nose, a leather apron and a little hammer."""
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_smidguh")
    sw = hulp.swatches(geo, ["schort", "riem", "hamerkop", "steel"])
    c = hulp.cube
    geo["bones"].append({"name": "smid_schort", "parent": "body", "pivot": [0, 6, 0], "cubes": [
        c([-4.6, 1.2, -4.5], [9.2, 8.6, 1.0], sw["schort"], inflate=0.05),
        c([-5.3, 8.6, -4.6], [10.6, 1.1, 9.4], sw["riem"]),
        c([-2.0, 9.6, -4.5], [4.0, 2.6, 0.9], sw["schort"])]})
    geo["bones"].append({"name": "smid_hamer", "parent": "arm_right", "pivot": [-3, 8.5, -6], "cubes": [
        c([-3.5, 8.0, -9.4], [1.0, 1.0, 5.0], sw["steel"]),
        c([-4.6, 7.2, -11.2], [3.2, 2.6, 2.2], sw["hamerkop"])]})
    hulp.save_geo(h, "guh_npc_smidguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.63, sat=0.38, val=0.78)
    rng = np.random.default_rng(215009)
    hulp.paint_swatch(a, sw["schort"], (132, 84, 52), rng, 8)
    hulp.paint_swatch(a, sw["riem"], (74, 48, 34), rng, 5)
    hulp.paint_swatch(a, sw["hamerkop"], (92, 96, 112), rng, 6)
    hulp.paint_swatch(a, sw["steel"], (150, 112, 70), rng, 5)
    # soot: a smudge on the nose and a few specks on the cheeks (the face is the north side of the head's outer cube: uv 110,32 14 x 11)
    x0, y0 = 110 * 4, 32 * 4
    roet = np.random.default_rng(215010)
    for _ in range(170):
        dx, dy = roet.normal(0, 5.2), roet.normal(0, 3.4)
        x, y = int(x0 + 28 + dx), int(y0 + 29 + dy)
        if x0 <= x < x0 + 56 and y0 <= y < y0 + 44:
            a[y, x, :3] = (a[y, x, :3].astype(np.float32) * 0.35 + np.array((34, 30, 36)) * 0.65).astype(np.uint8)
    for (cx, cy) in ((9, 30), (47, 27), (40, 36)):
        for _ in range(16):
            x, y = int(x0 + cx + roet.normal(0, 1.6)), int(y0 + cy + roet.normal(0, 1.2))
            if x0 <= x < x0 + 56 and y0 <= y < y0 + 44:
                a[y, x, :3] = (a[y, x, :3].astype(np.float32) * 0.5 + np.array((40, 36, 42)) * 0.5).astype(np.uint8)
    h.save(Image.fromarray(a), "entity", "npc_smidguh.png")


# =====================================================================================================================
# loot, recipes, tags, sounds, advancements, structures
# =====================================================================================================================
def data(h):
    D, w = h.D, h.w
    for b in (DONKER, DONKER + "_trap", "bliksemsmidse_wolkentafel", "bliksemsmidse_wolkenplank", "bliksemsmidse_onweerswolkje", "wolkenkasteeltje_kruimel"):
        h.self_drop(b)
    w(f"{D}/loot_table/blocks/{DONKER}_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:item", "name": f"guhs:{DONKER}_plaat", "functions": [
            {"function": "minecraft:set_count", "add": False, "count": 2, "conditions": [{
                "condition": "minecraft:block_state_property", "block": f"guhs:{DONKER}_plaat", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    # the pot and the hoard are part of their place: they cannot be broken, so they drop nothing
    for b in ("regenboogbrug_pot", "wolkenkasteeltje_schat"):
        w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": []})
    # the thundercloud is pressed by the smid-guh; its slab and stairs you make yourself
    h.shaped(f"{DONKER}_plaat", ["BBB"], {"B": f"guhs:{DONKER}"}, f"guhs:{DONKER}_plaat", 6)
    h.shaped(f"{DONKER}_trap", ["B  ", "BB ", "BBB"], {"B": f"guhs:{DONKER}"}, f"guhs:{DONKER}_trap", 4)
    h.shaped(DONKER, ["WWW", "WKW", "WWW"], {"W": "guhs:wolkenblok_wit", "K": "minecraft:gray_dye"}, f"guhs:{DONKER}", 8)
    for soort in ("block", "item"):
        h.add_tag(f"minecraft/tags/{soort}/slabs", [f"guhs:{DONKER}_plaat"])
        h.add_tag(f"minecraft/tags/{soort}/stairs", [f"guhs:{DONKER}_trap"])
    h.add_tag("minecraft/tags/block/mineable/hoe", [f"guhs:{b}" for b in BLOKKEN[:6]])
    for name in QUEST:
        w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})

    def patch(d):
        for event, (files, _sub) in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:{GELUID_MAP}/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    lib.teksten(h, {f"subtitles.guhs.{event}": sub for event, (_files, sub) in SOUNDS.items()})


def structuren(h):
    for naam, s in bouw.STRUCTUREN.items():
        m = bouw.MATEN[naam]
        # reach: the furthest corner of the template from its anchor
        h.structure(naam, ["wolkenweide"], spacing=s["spacing"], separation=s["separation"], salt=s["salt"], reach=s["ruimte"] + 8,
                    centre=f"guhs:{naam}_midden")
        bio_plek.plek(h, naam, "lucht", hoogte=s["hoogte"], ruimte=s["ruimte"])
        assert m["anker"][1] == s["hoogte"], naam


def test_templates(h):
    # the hall of a giant without its walls: a floor of cloud 17 x 26 (things on the floor stand on helper y 2) and his bed,
    # one high, x 3..13, z 6..12: the giant lies at template (8.5, 2, 9.5) = helper (8.5, 3, 9.5), his head west; with the
    # castle not turned his hall is x 1.5..16.5, z 0.5..17.5 and the landing in front of the gate is (9, 2, 23)
    t = h.Structure((17, 12, 26))
    for x in range(17):
        for z in range(26):
            t.set(x, 0, z, "guhs:wolkenblok_wit")
    for x in range(3, 14):
        for z in range(6, 13):
            t.set(x, 1, z, "guhs:wolkenblok_roze")
    t.save("reuzenguh_test_hal")


def build(h):
    with _jar() as z:
        texturen(h)
        donker_modellen(h, z)
    deeltjes(h)
    meubel_modellen(h)
    reus(h)
    smid(h)
    data(h)
    lib.teksten(h, TEKSTEN)
    templates = bouw.build(h)
    structuren(h)
    test_templates(h)
    selfcheck(h, templates)


def selfcheck(h, templates=None):
    A, D = h.A, h.D
    mis = []
    for b in BLOKKEN:
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                mis.append(p)
        if f"block.guhs.{b}" not in h.NL:
            mis.append(f"lang block.guhs.{b}")
    for b in BLOKKEN:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            continue
        state = json.load(open(f"{A}/blockstates/{b}.json", encoding="utf-8"))
        for v in state["variants"].values():
            for m in (v if isinstance(v, list) else [v]):
                pad = f"{A}/models/{m['model'].split(':')[1]}.json"
                if not os.path.exists(pad):
                    mis.append(f"model {m['model']} ({b})")
                    continue
                for t in json.load(open(pad, encoding="utf-8")).get("textures", {}).values():
                    t = t["sprite"] if isinstance(t, dict) else t
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        mis.append(f"texture {t} ({b})")
    for naam in STRUCTUREN:
        for p in (f"{D}/structure/{naam}.nbt", f"{D}/worldgen/structure/{naam}.json", f"{D}/worldgen/structure_set/{naam}.json"):
            if not os.path.exists(p):
                mis.append(p)
        for k in (f"structure.guhs.{naam}", f"structure.guhs.{naam}.tooltip"):
            if k not in h.NL:
                mis.append(f"lang {k}")
        p = f"{D}/worldgen/structure/{naam}.json"
        if os.path.exists(p):
            s = json.load(open(p, encoding="utf-8"))
            if s.get("type") != "guhs:bio_plek" or s.get("plek") != "lucht":
                mis.append(f"{naam} is not a guhs:bio_plek of kind lucht")
        p = f"{D}/worldgen/structure_set/{naam}.json"
        # biomes3 fix-plaatsing: a lucht structure is one per region now (only the buildings that come keep air free); the
        # old rule "spacing 8 or more" holds for a lucht set that is NOT per_regio
        ps = f"{D}/worldgen/structure/{naam}.json"
        if os.path.exists(p) and os.path.exists(ps) and "per_regio" not in json.load(open(ps, encoding="utf-8")) \
                and json.load(open(p, encoding="utf-8"))["placement"]["spacing"] < 8:
            mis.append(f"{naam}: a lucht set that is not per_regio must keep spacing 8 or more")
    for event, (files, _sub) in SOUNDS.items():
        for f in files:
            p = f"{A}/sounds/{GELUID_MAP}/{f}.ogg"
            if not os.path.exists(p):
                mis.append(f"{p} (run: python tools/features/bio_bouw_wolk2_geluid.py)")
    for p in (f"{A}/geckolib/models/entity/reuzenguh.geo.json", f"{A}/geckolib/animations/entity/reuzenguh.animation.json",
              f"{A}/geckolib/models/entity/guh_npc_smidguh.geo.json", f"{A}/textures/entity/npc_smidguh.png", f"{A}/textures/entity/reuzenguh_loer.png",
              f"{A}/particles/reuzenguh_zzz.json", f"{A}/particles/bliksemsmidse_flits.json"):
        if not os.path.exists(p):
            mis.append(p)
    for key in TEKSTEN:
        if key not in h.NL:
            mis.append(f"lang {key}")
    if mis:
        raise SystemExit("bio_bouw_wolk2 self-check failed:\n  " + "\n  ".join(mis))
