"""
biomes3 slice "blokken-wolk" (Java: feature/bio/blokkenwolk; English: tools/lang/en/c83_bio_blokken_wolk.json; contract: CONTRACT_BIO.md).

What the Wolkenweide and the lake are made of:

  - wolkenblok_wit / wolkenblok_roze, each with _plaat and _trap: cloud. Solid cubes (they cull like stone), but drawn
    WITHOUT the game's side shading ("shade": false): the light and dark is painted into the textures (a bright top, soft
    sides, a lilac belly), so a bank of them reads as one soft mass instead of a pile of cubes. The textures have no
    border and no weave, tile on all sides, and the full block picks one of three pictures by its spot.
  - regenboogblok with _plaat and _trap: translucent. The sides carry the colours in layers (red on top), the top and the
    underside carry them as stripes along the block's axis, all fixed to the world (the models are written per facing,
    never turned by the blockstate), so the stripes run on over every step of a bridge. The property "strook" spreads one
    rainbow over three pieces side by side (RegenboogBlokken.java).
  - wolkenbank, wolkenbed (two halves), wolkenlamp; drijvende_bloesemblaadjes (three densities, several pictures and
    turns each, picked by the spot); the item wolkenpluis.
  - particles: wolkenstroom_pluis (the lift columns: puffs and little arrows), waterval_schuim and waterval_nevel.
  - sounds.json for wolkenblok.stap / .plaats / .breek and waterval.ruis (the OGGs: bio_blokken_wolk_geluid.py).
  - loot (everything drops itself), recipes, tags, Dutch names.

Everything random comes from lib.rng(name): the same bytes on every run, wherever this module stands in FEATURES.
"""
import json
import os
import zipfile

import numpy as np
from PIL import Image

from features import bio_lib as lib

KLEUREN = ("wit", "roze")
WOLK = [f"wolkenblok_{k}{d}" for k in KLEUREN for d in ("", "_plaat", "_trap")]
REGENBOOG = ["regenboogblok", "regenboogblok_plaat", "regenboogblok_trap"]
MEUBELS = ["wolkenbank", "wolkenbed", "wolkenlamp"]
BLAADJES = "drijvende_bloesemblaadjes"
BLOKKEN = WOLK + REGENBOOG + MEUBELS + [BLAADJES]
STROKEN = ("heel", "a", "b", "c")
RICHTINGEN = ("north", "east", "south", "west")
# petals per picture, for each density (the blockstate picks a picture and a turn by the spot)
BLAADJES_PER_BEELD = {1: (2, 3, 4, 4, 5, 6), 2: (7, 8, 9, 10), 3: (12, 13, 15)}

SOUNDS = {
    "wolkenblok.stap": (["stap1", "stap2", "stap3", "stap4"], "Wolk ploft zacht"),
    "wolkenblok.plaats": (["plaats1", "plaats2"], "Wolk neergezet"),
    "wolkenblok.breek": (["breek1", "breek2"], "Wolk verwaait"),
    "waterval.ruis": (["ruis"], "Waterval ruist"),
}

TEKSTEN = {
    "block.guhs.wolkenblok_wit": "Wit wolkenblok",
    "block.guhs.wolkenblok_wit_plaat": "Witte wolkenplaat",
    "block.guhs.wolkenblok_wit_trap": "Witte wolkentrap",
    "block.guhs.wolkenblok_roze": "Roze wolkenblok",
    "block.guhs.wolkenblok_roze_plaat": "Roze wolkenplaat",
    "block.guhs.wolkenblok_roze_trap": "Roze wolkentrap",
    "block.guhs.regenboogblok": "Regenboogblok",
    "block.guhs.regenboogblok_plaat": "Regenboogplaat",
    "block.guhs.regenboogblok_trap": "Regenboogtrap",
    "block.guhs.wolkenbank": "Wolkenbank",
    "block.guhs.wolkenbed": "Wolkenbed",
    "block.guhs.wolkenbed.hier_niet": "Hier kun je niet slapen, njeg! Zelfs niet op een wolk.",
    "block.guhs.wolkenlamp": "Wolkenlamp",
    "block.guhs.drijvende_bloesemblaadjes": "Drijvende bloesemblaadjes",
    "item.guhs.wolkenpluis": "Wolkenpluis",
}


def _jar():
    return zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))


# =====================================================================================================================
# textures
# =====================================================================================================================
def _veld(naam, grofheid=2.2, n=16):
    """A soft field of n x n values 0..1 that tiles on all four sides (random noise, blurred round the edges)."""
    f = lib.rng(naam).random((n, n))
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


# cloud: (lightest, shadow) per face. Painted light: the models are drawn unshaded.
WOLK_TINT = {
    "wit": {"boven": ((255, 255, 255), (240, 245, 255)), "zij": ((244, 247, 255), (221, 229, 251)), "onder": ((228, 231, 250), (206, 212, 242))},
    "roze": {"boven": ((255, 236, 245), (255, 216, 234)), "zij": ((255, 222, 237), (246, 195, 222)), "onder": ((242, 196, 222), (226, 174, 210))},
}
# furniture is small: it needs more light and dark to keep its shape (also unshaded, so it matches the cloud it stands on):
# top, the two long sides, the two other sides, underside
MEUBEL_TINT = {
    "wit": {"boven": ((255, 255, 255), (240, 245, 255)), "zij": ((236, 240, 253), (217, 225, 248)), "kant": ((221, 227, 248), (203, 213, 242)),
            "onder": ((203, 209, 238), (189, 197, 230))},
    "roze": {"boven": ((255, 228, 240), (255, 206, 226)), "zij": ((250, 200, 222), (240, 180, 208)), "kant": ((240, 184, 210), (228, 166, 196)),
             "onder": ((224, 164, 196), (210, 150, 184))},
}
WOLK_BEELDEN = {"boven": 2, "zij": 3, "onder": 2}


def wolk_textures(h):
    for kleur in KLEUREN:
        for vlak, n in WOLK_BEELDEN.items():
            licht, schaduw = WOLK_TINT[kleur][vlak]
            for i in range(n):
                # big soft billows, and a breath of finer fluff on top; no pixel noise, no border
                v = 0.8 * _veld(f"wolk_{vlak}_{i}", 2.0) + 0.2 * _veld(f"wolk_{vlak}_{i}_fijn", 4.5)
                v = np.clip((v - 0.25) / 0.6, 0, 1) ** 1.3
                h.save(_beeld(_meng(licht, schaduw, v)), "block", f"wolkenblok_{kleur}_{vlak}_{i}.png")
        for vlak, (licht, schaduw) in MEUBEL_TINT[kleur].items():
            v = np.clip((_veld(f"wolkenmeubel_{vlak}", 2.0) - 0.25) / 0.6, 0, 1) ** 1.3
            h.save(_beeld(_meng(licht, schaduw, v)), "block", f"wolkenblok_{kleur}_meubel_{vlak}.png")


SPECTRUM = [(255, 98, 118), (255, 166, 88), (255, 230, 104), (126, 226, 136), (96, 200, 252), (120, 142, 248), (190, 128, 242)]
REGENBOOG_ALPHA = 214


def _spectrum(t):
    """The seven colours over t = 0..1: soft bands (a flat middle, a short blend into the next)."""
    pos = np.clip(t, 0, 1) * 7 - 0.5
    i = np.clip(np.floor(pos), 0, 5).astype(int)
    f = np.clip(pos - i, 0, 1)
    s = np.clip((f - 0.25) / 0.5, 0, 1)
    s = s * s * (3 - 2 * s)
    c = np.asarray(SPECTRUM, np.float32)
    return c[i] * (1 - s[..., None]) + c[i + 1] * s[..., None]


def _glans(naam, rgb):
    """A soft sheen over a rainbow texture and a few glints."""
    v = _veld(naam, 2.5)
    out = rgb + (v[..., None] - 0.5) * 14
    alpha = np.full(rgb.shape[:2], REGENBOOG_ALPHA, np.float32) + (v - 0.5) * 24
    r = lib.rng(naam + "_glint")
    for _ in range(3):
        x, y = r.integers(0, 16, 2)
        out[y, x] = out[y, x] * 0.4 + 255 * 0.6
        alpha[y, x] = 240
    return out, alpha


def regenboog_textures(h):
    rij = (np.arange(16) + 0.5)
    # the sides: layers, red on top
    zij = np.repeat(_spectrum(rij / 16)[:, None, :], 16, axis=1)
    h.save(_beeld(*_glans("regenboog_zij", zij)), "block", "regenboogblok_zij.png")
    # the top: stripes. _x: stripes along x (the colours go down the picture = to the south, red in the north);
    # _z: stripes along z (the colours go to the east, red in the west). a/b/c: a third of the colours each.
    for strook, (van, breed) in {"heel": (0, 16), "a": (0, 48), "b": (16, 48), "c": (32, 48)}.items():
        kleuren = _spectrum((van + rij) / breed)
        x = np.repeat(kleuren[:, None, :], 16, axis=1)
        rgb, alpha = _glans(f"regenboog_boven_{strook}", x)
        h.save(_beeld(rgb, alpha), "block", f"regenboogblok_boven_{strook}_x.png")
        h.save(_beeld(np.transpose(rgb, (1, 0, 2)), alpha.T), "block", f"regenboogblok_boven_{strook}_z.png")


BLAD_KLEUR = [((250, 172, 206), (255, 212, 230)), ((244, 150, 194), (255, 196, 222)), ((255, 190, 216), (255, 228, 240)),
              ((236, 132, 182), (250, 180, 212))]
BLAD_VORM = [[(0, 0), (1, 0)], [(0, 0), (0, 1)], [(0, 0), (1, 0), (0, 1)], [(0, 0), (1, 1), (1, 0)], [(0, 0), (1, 0), (2, 0), (1, 1)],
             [(0, 1), (1, 0), (1, 1)], [(0, 0), (1, 0), (0, 1), (1, 1)], [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)], [(0, 0), (1, 0), (1, 1), (2, 1)]]


def blaadjes_texture(naam, aantal):
    """`aantal` petals strewn over a 16 x 16 picture: little pink shapes with a light spot, a pixel of water between them."""
    r = lib.rng(naam)
    a = np.zeros((16, 16, 4), np.uint8)
    bezet = np.zeros((16, 16), bool)
    gezet = 0
    for _ in range(400):
        if gezet == aantal:
            break
        vorm = BLAD_VORM[r.integers(0, len(BLAD_VORM))]
        x0, y0 = r.integers(0, 16, 2)
        cellen = [(x0 + dx, y0 + dy) for dx, dy in vorm]
        if any(x > 15 or y > 15 for x, y in cellen):
            continue
        # a pixel of water between two petals, so they stay loose petals and not a pink blob
        if any(bezet[max(0, y - 1):y + 2, max(0, x - 1):x + 2].any() for x, y in cellen):
            continue
        donker, licht = BLAD_KLEUR[r.integers(0, len(BLAD_KLEUR))]
        lichte = r.integers(0, len(cellen))
        for i, (x, y) in enumerate(cellen):
            a[y, x] = (licht if i == lichte else donker) + (255,)
            bezet[y, x] = True
        gezet += 1
    if gezet != aantal:
        raise SystemExit(f"bio_blokken_wolk: no room for {aantal} petals in {naam}")
    return Image.fromarray(a, "RGBA")


def _rond(n, straal, zacht, kleur=(255, 255, 255), alpha=255, midden=None):
    """A round soft blob on an n x n picture."""
    cx, cy = midden or ((n - 1) / 2, (n - 1) / 2)
    yy, xx = np.mgrid[0:n, 0:n]
    d = np.hypot(xx - cx, yy - cy)
    al = np.clip((straal - d) / max(zacht, 1e-6), 0, 1) * alpha
    rgb = np.broadcast_to(np.asarray(kleur, np.float32), (n, n, 3))
    return rgb, al


def deeltjes_textures(h):
    A = h.A
    # the puff of the wolkenstroom: white (the particle tints it), three shapes
    for i, (straal, midden) in enumerate(((3.6, None), (3.2, (3.2, 3.8)), (3.0, (4.0, 3.0)))):
        rgb, al = _rond(8, straal, 2.2, midden=midden)
        rgb2, al2 = _rond(8, 2.0, 1.6, midden=(5.2 - i, 2.6 + i * 0.8))
        h.save(_beeld(rgb, np.maximum(al, al2 * 0.9)), "particle", f"wolkenstroom_pluis_{i}.png")
    # the arrow: two chevrons, a light middle with a darker rim so it shows against cloud and against sky
    op = ["...ee...", "..eIIe..", ".eIeeIe.", "eIe..eIe", "...ee...", "..eIIe..", ".eIeeIe.", "eIe..eIe"]
    for i, (rijen, licht, rand) in enumerate(((op, (255, 255, 255, 255), (96, 168, 240, 255)), (op[::-1], (255, 240, 248, 255), (236, 96, 168, 255)))):
        h.save(h.grid(rijen, {"I": licht, "e": rand, ".": (0, 0, 0, 0)}), "particle", f"wolkenstroom_pijl_{i}.png")
    # one particle, five pictures, in the order client/Deeltjes.java counts on: the three puffs, the arrow up, the arrow down
    h.w(f"{A}/particles/wolkenstroom_pluis.json", {"textures": [f"guhs:wolkenstroom_pluis_{i}" for i in range(3)]
                                                   + ["guhs:wolkenstroom_pijl_0", "guhs:wolkenstroom_pijl_1"]})
    # foam: small white flecks
    for i, (straal, midden) in enumerate(((1.6, None), (2.1, (3.4, 3.6)), (1.2, (3.0, 4.0)))):
        rgb, al = _rond(8, straal, 1.0, midden=midden)
        h.save(_beeld(rgb, al), "particle", f"waterval_schuim_{i}.png")
    h.w(f"{A}/particles/waterval_schuim.json", {"textures": [f"guhs:waterval_schuim_{i}" for i in range(3)]})
    # mist: a big ragged veil (the particle keeps it faint)
    for i in range(2):
        rgb, al = _rond(16, 7.4, 6.0, kleur=(244, 250, 255))
        al = al * (0.55 + 0.45 * _veld(f"nevel_{i}", 2.2))
        h.save(_beeld(rgb, al), "particle", f"waterval_nevel_{i}.png")
    h.w(f"{A}/particles/waterval_nevel.json", {"textures": [f"guhs:waterval_nevel_{i}" for i in range(2)]})


def meubel_textures(h):
    # the lamp: a cloud lit from inside, warm in the middle of every face
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.clip(np.hypot(xx - 7.5, yy - 7.5) / 9.5, 0, 1)
    v = np.clip(d * 0.85 + (_veld("wolkenlamp", 2.4) - 0.5) * 0.3, 0, 1)
    h.save(_beeld(_meng((255, 232, 164), (255, 252, 238), v)), "block", "wolkenlamp.png")
    # wolkenpluis: a tuft of fluff
    pal = dict(h.ITEM_PAL)
    pal.update({"W": (255, 255, 255, 255), "c": (214, 228, 252, 255), "C": (178, 200, 240, 255), "o": (120, 140, 190, 255),
                "p": (255, 214, 232, 255)})
    h.save(h.grid(["................", "................", ".....ooo........", "....oWWWo.ooo...", "...oWWWWWoWWWo..", "..oWWWWWWWWWWWo.",
                   "..oWWWWcWWWWWWo.", ".oWWWWWWWWWcWWWo", ".oWWcWWWWWWWWWCo", ".oWWWWWWpWWWWcCo", "..oWWWWWWWWWcCo.",
                   "..ocWWWcWWWcCCo.", "...oCccCcccCCo..", "....ooCCCCooo...", "......oooo......", "................"], pal),
           "item", "wolkenpluis.png")
    h.item_model("wolkenpluis")


# =====================================================================================================================
# models and blockstates
# =====================================================================================================================
def _onbeschaduwd(elements):
    """Model elements drawn without the game's side shading (cloud)."""
    return [dict(e, shade=False) for e in elements]


KUBUS = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
    "down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#top", "cullface": "up"},
    **{f: {"texture": "#side", "cullface": f} for f in RICHTINGEN}}}]


def wolk_modellen(h, z):
    A, w = h.A, h.w
    sjabloon = {n: json.loads(z.read(f"assets/minecraft/models/block/{n}.json")) for n in ("slab", "slab_top", "stairs", "inner_stairs", "outer_stairs")}
    for kleur in KLEUREN:
        naam = f"wolkenblok_{kleur}"

        def tex(i):
            return {"top": f"guhs:block/{naam}_boven_{i % 2}", "side": f"guhs:block/{naam}_zij_{i}", "bottom": f"guhs:block/{naam}_onder_{i % 2}",
                    "particle": f"guhs:block/{naam}_zij_{i}"}
        # the block: three pictures and a quarter turn, by the spot
        for i in range(3):
            w(f"{A}/models/block/{naam}_{i}.json", {"parent": "minecraft:block/block", "textures": tex(i), "elements": _onbeschaduwd(KUBUS)})
        w(f"{A}/blockstates/{naam}.json", {"variants": {"": [{"model": f"guhs:block/{naam}_{i}", **({"y": y} if y else {})}
                                                              for i in range(3) for y in (0, 90)]}})
        w(f"{A}/models/item/{naam}.json", {"parent": f"guhs:block/{naam}_0"})
        # slab and stairs: vanilla's shapes, unshaded
        for ours, van in ((f"{naam}_plaat", "slab"), (f"{naam}_plaat_top", "slab_top"), (f"{naam}_trap", "stairs"),
                          (f"{naam}_trap_inner", "inner_stairs"), (f"{naam}_trap_outer", "outer_stairs")):
            model = {"parent": "minecraft:block/block", "textures": tex(0), "elements": _onbeschaduwd(sjabloon[van]["elements"])}
            if "display" in sjabloon[van]:
                model["display"] = sjabloon[van]["display"]
            w(f"{A}/models/block/{ours}.json", model)
        state = json.dumps(json.loads(z.read("assets/minecraft/blockstates/oak_slab.json")))
        state = state.replace("minecraft:block/oak_slab_top", f"guhs:block/{naam}_plaat_top").replace("minecraft:block/oak_slab", f"guhs:block/{naam}_plaat")
        state = state.replace("minecraft:block/oak_planks", f"guhs:block/{naam}_0")
        w(f"{A}/blockstates/{naam}_plaat.json", json.loads(state))
        state = json.dumps(json.loads(z.read("assets/minecraft/blockstates/oak_stairs.json")))
        state = state.replace("minecraft:block/oak_stairs", f"guhs:block/{naam}_trap")
        w(f"{A}/blockstates/{naam}_trap.json", json.loads(state))
        w(f"{A}/models/item/{naam}_plaat.json", {"parent": f"guhs:block/{naam}_plaat"})
        w(f"{A}/models/item/{naam}_trap.json", {"parent": f"guhs:block/{naam}_trap"})


# --- the rainbow: models written in world coordinates, one per state --------------------------------------------------
HOEKEN = ["nw", "ne", "se", "sw"]                       # clockwise seen from above
HOEK_CEL = {"nw": (0, 0), "ne": (1, 0), "se": (1, 1), "sw": (0, 1)}      # (x, z) of the quarter


def trap_cellen(facing, half, shape):
    """
    The eight half-block cells (x, y, z in 0/1) a stair fills: StairBlock's own shapes. Facing north the high part is the
    two north quarters; an outer corner is one quarter, an inner corner three; "left" turns it a quarter back.
    """
    draai = RICHTINGEN.index(facing)
    if shape == "straight":
        hoeken, draai = ["nw", "ne"], draai
    elif shape == "outer_left":
        hoeken, draai = ["nw"], draai
    elif shape == "outer_right":
        hoeken, draai = ["nw"], draai + 1
    elif shape == "inner_right":
        hoeken, draai = ["nw", "ne", "se"], draai
    else:
        hoeken, draai = ["nw", "ne", "se"], draai - 1
    plaat, stap = (0, 1) if half == "bottom" else (1, 0)
    cellen = {(x, plaat, z) for x in (0, 1) for z in (0, 1)}
    for hoek in hoeken:
        x, z = HOEK_CEL[HOEKEN[(HOEKEN.index(hoek) + draai) % 4]]
        cellen.add((x, stap, z))
    return cellen


VLAKKEN = {"down": (1, -1), "up": (1, 1), "north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1)}


def cel_elementen(cellen):
    """
    Flat elements (one face each) for every outside face of a shape of half-block cells, neighbouring faces joined into
    one. Only what you can see from outside: a see-through block must not show faces inside itself. The up and down faces
    get the same picture seen from above (the stripes of the top and the underside lie over each other); the sides use the
    game's own mapping, which keeps a layered picture level. Unshaded, like cloud: a rainbow is as bright below as on top.
    """
    elements = []
    for vlak, (as_, kant) in VLAKKEN.items():
        andere = [i for i in range(3) if i != as_]
        for laag in (0, 1):
            vrij = set()
            for c in cellen:
                if c[as_] != laag:
                    continue
                buur = list(c)
                buur[as_] += kant
                if tuple(buur) not in cellen:
                    vrij.add((c[andere[0]], c[andere[1]]))
            # join: the whole layer, else pairs, else single cells
            stukken = []
            if len(vrij) == 4:
                stukken.append((0, 0, 2, 2))
            else:
                over = set(vrij)
                for (a, b) in sorted(vrij):
                    if (a, b) not in over:
                        continue
                    if (a + 1, b) in over:
                        stukken.append((a, b, a + 2, b + 1))
                        over -= {(a, b), (a + 1, b)}
                    elif (a, b + 1) in over:
                        stukken.append((a, b, a + 1, b + 2))
                        over -= {(a, b), (a, b + 1)}
                    else:
                        stukken.append((a, b, a + 1, b + 1))
                        over.discard((a, b))
            vast = (laag + (1 if kant > 0 else 0)) * 8
            for (a0, b0, a1, b1) in stukken:
                frm, to = [0, 0, 0], [0, 0, 0]
                frm[as_] = to[as_] = vast
                frm[andere[0]], to[andere[0]] = a0 * 8, a1 * 8
                frm[andere[1]], to[andere[1]] = b0 * 8, b1 * 8
                face = {"texture": "#boven" if vlak in ("up", "down") else "#zij"}
                if vlak == "down":
                    face["uv"] = [frm[0], to[2], to[0], frm[2]]
                if vast in (0, 16):
                    face["cullface"] = vlak
                elements.append({"from": frm, "to": to, "shade": False, "faces": {vlak: face}})
    return elements


def regenboog_modellen(h, z):
    A, w = h.A, h.w
    stairs_display = json.loads(z.read("assets/minecraft/models/block/stairs.json"))["display"]

    def model(naam, cellen, strook, as_, display=None):
        d = {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
             "textures": {"boven": f"guhs:block/regenboogblok_boven_{strook}_{as_}", "zij": "guhs:block/regenboogblok_zij",
                          "particle": "guhs:block/regenboogblok_zij"},
             "elements": cel_elementen(cellen)}
        if display:
            d["display"] = display
        w(f"{A}/models/block/{naam}.json", d)
        return {"model": f"guhs:block/{naam}"}

    alles = {(x, y, z_) for x in (0, 1) for y in (0, 1) for z_ in (0, 1)}
    lagen = {"bottom": {c for c in alles if c[1] == 0}, "top": {c for c in alles if c[1] == 1}, "double": alles}
    blok, plaat, trap = {}, {}, {}
    for strook in STROKEN:
        for as_ in ("x", "z"):
            blok[f"axis={as_},strook={strook}"] = model(f"regenboogblok_{strook}_{as_}", alles, strook, as_)
            for soort, cellen in lagen.items():
                plaat[f"axis={as_},strook={strook},type={soort}"] = model(f"regenboogblok_plaat_{soort}_{strook}_{as_}", cellen, strook, as_)
        for facing in RICHTINGEN:
            as_ = "x" if facing in ("east", "west") else "z"
            for half in ("bottom", "top"):
                for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                    trap[f"facing={facing},half={half},shape={shape},strook={strook}"] = model(
                        f"regenboogblok_trap_{facing}_{half}_{shape}_{strook}", trap_cellen(facing, half, shape), strook, as_,
                        stairs_display if (facing, half, shape, strook) == ("east", "bottom", "straight", "heel") else None)
    w(f"{A}/blockstates/regenboogblok.json", {"variants": blok})
    w(f"{A}/blockstates/regenboogblok_plaat.json", {"variants": plaat})
    w(f"{A}/blockstates/regenboogblok_trap.json", {"variants": trap})
    w(f"{A}/models/item/regenboogblok.json", {"parent": "guhs:block/regenboogblok_heel_x"})
    w(f"{A}/models/item/regenboogblok_plaat.json", {"parent": "guhs:block/regenboogblok_plaat_bottom_heel_x"})
    w(f"{A}/models/item/regenboogblok_trap.json", {"parent": "guhs:block/regenboogblok_trap_east_bottom_straight_heel"})


# --- cloud furniture (models facing north: the back of the bench and the head of the bed are in the north... see below) ---
def meubel_modellen(h):
    A, w = h.A, h.w
    W, R = "wit", "roze"
    tex = {f"{k}_{vlak}": f"guhs:block/wolkenblok_{k}_meubel_{vlak}" for k in KLEUREN for vlak in ("boven", "zij", "kant", "onder")}
    tex["particle"] = "guhs:block/wolkenblok_wit_zij_0"

    def el(frm, to, kleur):
        """A lump of cloud: unshaded like the wolkenblok, its light and dark come from a picture per side."""
        return {"from": frm, "to": to, "shade": False, "faces": {
            "up": {"texture": f"#{kleur}_boven"}, "down": {"texture": f"#{kleur}_onder"},
            "north": {"texture": f"#{kleur}_zij"}, "south": {"texture": f"#{kleur}_zij"},
            "west": {"texture": f"#{kleur}_kant"}, "east": {"texture": f"#{kleur}_kant"}}}
    # the bench (the same sizes as BlokkenWolkSlice.WOLKENBANK's boxes; the back is at z 11..15, as the guh-bank's)
    bank = [el([2, 0, 3], [14, 2, 13], W), el([0, 2, 1], [16, 8, 15], W),
            el([1, 8, 2], [7.5, 9.5, 11], R), el([8.5, 8, 2], [15, 9.5, 11], R),
            el([0, 8, 11], [16, 15, 15], W), el([1, 15, 11], [6, 17, 15], W), el([5.5, 15, 11.5], [10.5, 18, 14.5], W),
            el([10, 15, 11], [15, 17, 15], W),
            el([0, 8, 1], [2, 12, 11], W), el([14, 8, 1], [16, 12, 11], W), el([0, 12, 3], [2, 13, 9], W), el([14, 12, 3], [16, 13, 9], W)]
    w(f"{A}/models/block/wolkenbank.json", {"parent": "minecraft:block/block", "textures": tex, "elements": bank})
    w(f"{A}/blockstates/wolkenbank.json", {"variants": h.facing_states("wolkenbank")})
    w(f"{A}/models/item/wolkenbank.json", {"parent": "guhs:block/wolkenbank"})

    # the bed, facing north = the head half lies north of the foot half; the pillow is at the far (north) end
    hoofd = [el([1, 0, 1], [15, 2, 16], W), el([0, 2, 0], [16, 7, 16], W),
             el([0, 7, 0], [16, 11, 2], W), el([1.5, 11, 0], [7, 13, 2], W), el([9, 11, 0], [14.5, 13, 2], W),
             el([3, 7, 3], [13, 9, 9], R), el([0, 7, 12], [16, 8.5, 16], R)]
    voet = [el([1, 0, 0], [15, 2, 15], W), el([0, 2, 0], [16, 7, 16], W),
            el([0, 7, 0], [16, 8.5, 13], R), el([0, 7, 13], [16, 9, 16], W)]
    w(f"{A}/models/block/wolkenbed_hoofd.json", {"parent": "minecraft:block/block", "textures": tex, "elements": hoofd})
    w(f"{A}/models/block/wolkenbed_voet.json", {"parent": "minecraft:block/block", "textures": tex, "elements": voet})
    w(f"{A}/blockstates/wolkenbed.json", {"variants": {
        f"facing={f},part={part}": {"model": f"guhs:block/wolkenbed_{nl}", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for part, nl in (("head", "hoofd"), ("foot", "voet"))}})

    def schuif(elements, dz):
        return [dict(e, **{"from": [e["from"][0], e["from"][1], e["from"][2] + dz], "to": [e["to"][0], e["to"][1], e["to"][2] + dz]})
                for e in elements]
    # in the hand: the whole bed, both halves
    w(f"{A}/models/item/wolkenbed.json", {"parent": "minecraft:block/block", "textures": tex, "elements": schuif(hoofd, -8) + schuif(voet, 8),
                                          "display": {"gui": {"rotation": [30, 160, 0], "translation": [2, 3, 0], "scale": [0.5, 0.5, 0.5]},
                                                      "fixed": {"rotation": [270, 0, 0], "translation": [0, 0, -2], "scale": [0.5, 0.5, 0.5]},
                                                      "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                                                      "thirdperson_righthand": {"rotation": [30, 160, 0], "translation": [0, 3, -2],
                                                                                "scale": [0.23, 0.23, 0.23]},
                                                      "firstperson_righthand": {"rotation": [30, 160, 0], "translation": [0, 3, 0],
                                                                                "scale": [0.375, 0.375, 0.375]}}})

    # the lamp: a little floating cloud, glowing (its faces are lit from inside: light_emission, and unshaded)
    L = "#lamp"
    el = h.el
    lamp = [el([3, 4, 3], [13, 10, 13], L), el([5, 10, 4], [11, 12.5, 10], L), el([6, 10, 8], [12, 12, 12], L),
            el([2, 5, 5], [3, 9, 11], L), el([13, 5, 5], [14, 9, 11], L), el([5, 5, 2], [11, 9, 3], L), el([5, 5, 13], [11, 9, 14], L),
            el([5, 3, 5], [11, 4, 11], L)]
    lamp = [dict(e, shade=False, light_emission=13) for e in lamp]
    w(f"{A}/models/block/wolkenlamp.json", {"parent": "minecraft:block/block", "textures": {"lamp": "guhs:block/wolkenlamp",
                                                                                           "particle": "guhs:block/wolkenlamp"}, "elements": lamp})
    w(f"{A}/blockstates/wolkenlamp.json", {"variants": {"": {"model": "guhs:block/wolkenlamp"}}})
    w(f"{A}/models/item/wolkenlamp.json", {"parent": "guhs:block/wolkenlamp"})


def blaadjes_modellen(h):
    A, w = h.A, h.w
    varianten = {}
    for dichtheid, aantallen in BLAADJES_PER_BEELD.items():
        lijst = []
        for i, aantal in enumerate(aantallen):
            naam = f"{BLAADJES}_{dichtheid}_{i}"
            h.save(blaadjes_texture(naam, aantal), "block", f"{naam}.png")
            # a film just above the water of the block below (the water's own surface is 1.8 pixels under this block)
            w(f"{A}/models/block/{naam}.json", {"render_type": "minecraft:cutout", "ambientocclusion": False,
                                                "textures": {"blad": f"guhs:block/{naam}", "particle": f"guhs:block/{naam}"},
                                                "elements": [{"from": [0, -1, 0], "to": [16, -1, 16], "shade": False, "faces": {
                                                    "up": {"uv": [0, 0, 16, 16], "texture": "#blad"},
                                                    "down": {"uv": [0, 16, 16, 0], "texture": "#blad"}}}]})
            lijst += [{"model": f"guhs:block/{naam}", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
        varianten[f"dichtheid={dichtheid}"] = lijst
    w(f"{A}/blockstates/{BLAADJES}.json", {"variants": varianten})
    h.item_model(BLAADJES, tex=f"guhs:block/{BLAADJES}_3_0")


# =====================================================================================================================
# loot, recipes, tags, sounds, texts
# =====================================================================================================================
def loot(h):
    D, w = h.D, h.w
    for b in ("wolkenblok_wit", "wolkenblok_wit_trap", "wolkenblok_roze", "wolkenblok_roze_trap", "regenboogblok", "regenboogblok_trap",
              "wolkenbank", "wolkenlamp"):
        h.self_drop(b)
    for b in ("wolkenblok_wit_plaat", "wolkenblok_roze_plaat", "regenboogblok_plaat"):          # (a double slab: two)
        w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
            "type": "minecraft:item", "name": f"guhs:{b}", "functions": [
                {"function": "minecraft:set_count", "add": False, "count": 2, "conditions": [{
                    "condition": "minecraft:block_state_property", "block": f"guhs:{b}", "properties": {"type": "double"}}]},
                {"function": "minecraft:explosion_decay"}]}]}]})
    # the bed: one bed for its two halves (the head half drops it, as vanilla's)
    w(f"{D}/loot_table/blocks/wolkenbed.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}], "entries": [{
            "type": "minecraft:item", "name": "guhs:wolkenbed", "conditions": [{
                "condition": "minecraft:block_state_property", "block": "guhs:wolkenbed", "properties": {"part": "head"}}]}]}]})
    # petals: as many as the patch is thick
    w(f"{D}/loot_table/blocks/{BLAADJES}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:item", "name": f"guhs:{BLAADJES}", "functions": [
            {"function": "minecraft:set_count", "add": False, "count": n, "conditions": [{
                "condition": "minecraft:block_state_property", "block": f"guhs:{BLAADJES}", "properties": {"dichtheid": str(n)}}]}
            for n in BLAADJES_PER_BEELD]}]}]})


def recipes(h):
    P, W, R = "guhs:wolkenpluis", "guhs:wolkenblok_wit", "guhs:wolkenblok_roze"
    # fluff into cloud; white <-> pink with dye
    h.shaped("wolkenblok_wit", ["PP", "PP"], {"P": P}, W, 4)
    h.shapeless("wolkenblok_roze", [P, P, P, P, "minecraft:pink_dye"], R, 4)
    h.shaped("wolkenblok_roze_verven", ["WWW", "WDW", "WWW"], {"W": W, "D": "minecraft:pink_dye"}, R, 8)
    h.shaped("wolkenblok_wit_bleken", ["RRR", "RDR", "RRR"], {"R": R, "D": "minecraft:white_dye"}, W, 8)
    for blok in ("wolkenblok_wit", "wolkenblok_roze", "regenboogblok"):
        h.shaped(f"{blok}_plaat", ["BBB"], {"B": f"guhs:{blok}"}, f"guhs:{blok}_plaat", 6)
        h.shaped(f"{blok}_trap", ["B  ", "BB ", "BBB"], {"B": f"guhs:{blok}"}, f"guhs:{blok}_trap", 4)
    # fluff and the three primary colours pressed into rainbow
    h.shaped("regenboogblok", ["RYB", "PPP", "PPP"], {"R": "minecraft:red_dye", "Y": "minecraft:yellow_dye", "B": "minecraft:blue_dye", "P": P},
             "guhs:regenboogblok", 6)
    # furniture
    h.shaped("wolkenbank", ["  W", "WWW", "PPP"], {"W": W, "P": P}, "guhs:wolkenbank", 1)
    h.shaped("wolkenbed", ["RRR", "WWW"], {"R": R, "W": W}, "guhs:wolkenbed", 1)
    h.shaped("wolkenlamp", [" P ", "PKP", " P "], {"P": P, "K": "guhs:guh_kristal"}, "guhs:wolkenlamp", 2)
    # petals: shaken out of blossom leaves (or vanilla's pink petals)
    h.shapeless(BLAADJES, ["guhs:guhbloesem_leaves"], f"guhs:{BLAADJES}", 4)
    h.shapeless(f"{BLAADJES}_uit_roze_blaadjes", ["minecraft:pink_petals"], f"guhs:{BLAADJES}", 2)


def tags(h):
    add = h.add_tag
    for soort in ("block", "item"):
        add(f"minecraft/tags/{soort}/slabs", ["guhs:wolkenblok_wit_plaat", "guhs:wolkenblok_roze_plaat", "guhs:regenboogblok_plaat"])
        add(f"minecraft/tags/{soort}/stairs", ["guhs:wolkenblok_wit_trap", "guhs:wolkenblok_roze_trap", "guhs:regenboogblok_trap"])
        # ours: every piece of cloud / of rainbow (for other features: "is this cloud?")
        add(f"guhs/tags/{soort}/wolkenblok", [f"guhs:{b}" for b in WOLK])
        add(f"guhs/tags/{soort}/regenboogblok", [f"guhs:{b}" for b in REGENBOOG])
    add("minecraft/tags/block/beds", ["guhs:wolkenbed"])
    add("minecraft/tags/block/inside_step_sound_blocks", [f"guhs:{BLAADJES}"])
    add("minecraft/tags/block/sword_efficient", [f"guhs:{BLAADJES}"])
    # cloud is quick by hand and quicker with a hoe (as leaves and moss are)
    add("minecraft/tags/block/mineable/hoe", [f"guhs:{b}" for b in WOLK + REGENBOOG + MEUBELS])


def sounds(h):
    def patch(d):
        for event, (files, _sub) in SOUNDS.items():
            map_ = event.split(".")[0]
            d[event] = {"sounds": [f"guhs:{map_}/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    lib.teksten(h, {f"subtitles.guhs.{event}": sub for event, (_files, sub) in SOUNDS.items()})


# =====================================================================================================================
def build(h):
    with _jar() as z:
        wolk_textures(h)
        regenboog_textures(h)
        deeltjes_textures(h)
        meubel_textures(h)
        wolk_modellen(h, z)
        regenboog_modellen(h, z)
    meubel_modellen(h)
    blaadjes_modellen(h)
    loot(h)
    recipes(h)
    tags(h)
    sounds(h)
    lib.teksten(h, TEKSTEN)
    selfcheck(h)


def selfcheck(h):
    A, D = h.A, h.D
    mis = []
    for b in BLOKKEN:
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                mis.append(p)
        if f"block.guhs.{b}" not in h.NL:
            mis.append(f"lang block.guhs.{b}")
    # every model a blockstate of ours names exists, and every texture a model of ours names exists
    stammen = ("wolkenblok", "regenboogblok", "wolkenbank", "wolkenbed", "wolkenlamp", BLAADJES)
    for b in BLOKKEN:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            continue
        state = json.load(open(f"{A}/blockstates/{b}.json", encoding="utf-8"))
        for v in state["variants"].values():
            for m in (v if isinstance(v, list) else [v]):
                if not os.path.exists(f"{A}/models/{m['model'].split(':')[1]}.json"):
                    mis.append(f"model {m['model']} ({b})")
    for f in sorted(os.listdir(f"{A}/models/block")):
        if f.startswith(stammen):
            model = json.load(open(os.path.join(A, "models", "block", f), encoding="utf-8"))
            for t in model.get("textures", {}).values():
                t = t["sprite"] if isinstance(t, dict) else t          # (after tools/mc26.py: translucent textures are objects)
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    mis.append(f"texture {t} ({f})")
    for naam, beelden in (("wolkenstroom_pluis", 5), ("waterval_schuim", 3), ("waterval_nevel", 2)):
        p = f"{A}/particles/{naam}.json"
        if not os.path.exists(p):
            mis.append(f"particle {naam}")
            continue
        textures = json.load(open(p, encoding="utf-8"))["textures"]
        if len(textures) != beelden or any(not os.path.exists(f"{A}/textures/particle/{t.split(':')[1]}.png") for t in textures):
            mis.append(f"particle {naam}: {beelden} pictures expected, each with its file")
    for event, (files, _sub) in SOUNDS.items():
        for f in files:
            p = f"{A}/sounds/{event.split('.')[0]}/{f}.ogg"
            if not os.path.exists(p):
                mis.append(f"{p} (run: python tools/features/bio_blokken_wolk_geluid.py)")
    # the stairs' shapes: a whole slab plus two, one or three quarters, the high side where the stair faces
    for f, (x, z) in (("north", (None, 0)), ("south", (None, 1)), ("west", (0, None)), ("east", (1, None))):
        hoog = {c for c in trap_cellen(f, "bottom", "straight") if c[1] == 1}
        if len(hoog) != 2 or any((x is not None and c[0] != x) or (z is not None and c[2] != z) for c in hoog):
            mis.append(f"regenboogblok_trap: straight {f} is wrong: {sorted(hoog)}")
    for shape, n in (("outer_left", 5), ("outer_right", 5), ("straight", 6), ("inner_left", 7), ("inner_right", 7)):
        if any(len(trap_cellen(f, hf, shape)) != n for f in RICHTINGEN for hf in ("bottom", "top")):
            mis.append(f"regenboogblok_trap: {shape} should fill {n} cells")
    if mis:
        raise SystemExit("bio_blokken_wolk assets missing:\n  " + "\n  ".join(mis))
