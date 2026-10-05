"""
Vadskracht (bbq2, foundation F1; the Java side is feature/vadskracht).

The power of the guh machines: sources (the Guhrad...) give whole VK per second, Guhdraad joins everything into a net, and
when the machines ask more than there is the WHOLE net stands still. There is no meter block: looking at any block of the tag
guhs:vadskracht shows the state of its net under the crosshair.

This module is also the helper library of every tech slice (`from features import vadskracht`):
  machine(h, name, basis, accent, voor=None, extra_tags=(), item=True)
        a one-block guh machine: the textures block/<name>_{voor_slaapt,voor_werkt,voor_vol,zij,boven}.png (the standard guh
        face + ears on your colours; voor(img, snoet) paints your own details on top), the block models <name>_{slaapt,werkt,vol},
        the blockstate facing x snoet, the item model, self_drop, the tags guhs:vadskracht + mineable/pickaxe (+ extra_tags)
  batterij(h, name, basis, accent, extra_tags=(), item=True)
        a battery (Java: BatterijBlock): five fronts (lading 0..4: asleep when empty, surprised when full, a gauge), the
        blockstate facing x lading, item model, self_drop, tags
  snoet(img, x, y, staat, schaal=1)   paints the standard face (12 x 4 pixels) on any texture; staat "slaapt" | "werkt" | "vol"
  oren(tex)                           the model elements of the two ears (on top of a full block, at the front)
  toon(h, *ids)                       adds blocks to #guhs:vadskracht (the hover readout works on them)
  getal(naam)                         a number of VadsGetallen.java (the one place where they live), e.g. getal("GUH_OVEN")
  bron_max()                          {kind of source: how many count per net} from BronSoort.java

build(h) makes what the foundation itself needs: the tag with the Guhrad, its parts, the Guhdraad and the Guhoven, the fluid
tag guhs:techniek_sauzen, the texts of the readout, the Guhrad table data/guhs/vadskracht/guhrad.json, the invisible part
block machine_deel, and the test blocks (vadskracht_testbron, _testbatterij, _testmachine, _testmachine_groot) with the test
template vadskracht_test_kamer.
"""
import os
import re

import numpy as np
from PIL import Image

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
STATEN = ("slaapt", "werkt", "vol")

OOG = (58, 28, 60)
WIT = (255, 255, 255)
BLOS = (255, 130, 172)
NEUS = (226, 98, 150)
OOR = (255, 186, 214)
OOR_BINNEN = (255, 140, 182)

# what a guh gives in a Guhrad, per variant: (an ordinary day, a happy guh). The standard comes from VadsGetallen.java.
# DESIGN: the story guhs give 20-25 (the Baltoguh runs harder, Guhtwo lets the wheel float, the 626-guh counts double).
GUHRAD_VARIANTEN = {
    "baltoguh": (20, 25),
    "mewtwo": (25, 25),
    "stitch626": (20, 25),
}


# =====================================================================================================================
# numbers
# =====================================================================================================================
_GETALLEN = None


def getal(naam):
    """A number of feature/vadskracht/VadsGetallen.java (so the texts and the data never disagree with the code)."""
    global _GETALLEN
    if _GETALLEN is None:
        src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadskracht", "VadsGetallen.java"),
                   encoding="utf-8").read()
        _GETALLEN = {m.group(1): int(m.group(2).replace("_", "")) for m in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*=\s*([0-9][0-9_]*)", src)}
    if naam not in _GETALLEN:
        raise SystemExit(f"vadskracht: VadsGetallen heeft geen {naam}")
    return _GETALLEN[naam]


def bron_max():
    """{kind id: how many count per net} from BronSoort.java, e.g. {"guhrad": 4, ...}."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadskracht", "BronSoort.java"), encoding="utf-8").read()
    lijst = src[src.index("public enum BronSoort"):]
    lijst = lijst[:lijst.index(";")]
    return {m.group(1).lower(): int(m.group(2)) for m in re.finditer(r"([A-Z][A-Z_]*)[(]([0-9]+)[)]", lijst)}


# =====================================================================================================================
# the face, the ears
# =====================================================================================================================
def snoet(img, x, y, staat, schaal=1):
    """
    Paints the standard guh face on a texture, its top-left corner at (x, y); it takes 12 x 4 pixels (times schaal):
      slaapt: closed eyes (two little dashes) and a sleepy snoet          (no vadskracht)
      werkt:  open eyes with a shine, blushing cheeks and a snoet         (has vadskracht)
      vol:    open eyes, raised brows and a round "o" mouth               (full: its output is blocked)
    """
    if staat not in STATEN:
        raise ValueError(f"snoet: staat {staat!r} is niet een van {STATEN}")

    def px(dx, dy, kleur):
        for sx in range(schaal):
            for sy in range(schaal):
                xx, yy = x + dx * schaal + sx, y + dy * schaal + sy
                if 0 <= xx < img.width and 0 <= yy < img.height:
                    img.putpixel((xx, yy), tuple(kleur[:3]) + (255,))

    for ex in (2, 8):
        if staat == "slaapt":
            px(ex, 1, OOG)
            px(ex + 1, 1, OOG)
        else:
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                px(ex + dx, dy, OOG)
            px(ex, 0, WIT)
    if staat == "slaapt":
        px(5, 2, NEUS)
        px(6, 2, NEUS)
    elif staat == "werkt":
        for dx in (0, 1, 10, 11):
            px(dx, 2, BLOS)
        px(5, 2, NEUS)
        px(6, 2, NEUS)
    else:
        for dx, dy in ((5, 2), (6, 2), (5, 3), (6, 3)):
            px(dx, dy, OOG)
    return img


def oren(tex):
    """
    The two ears of a guh machine as model elements: 4 wide, 3 high, 2 deep, on top of a full block at its front (north)
    edge. tex is a texture variable ("#boven") whose pixels x 2..5 and 10..13, y 1..2 have the ear colours (machine() and
    batterij() paint them into the top texture, right where the ears stand).
    """
    out = []
    for x0 in (2, 10):
        uv = [x0, 1, x0 + 4, 3]
        out.append({"from": [x0, 16, 1], "to": [x0 + 4, 19, 3],
                    "faces": {f: {"texture": tex, "uv": uv} for f in ("up", "north", "south", "west", "east")}})
    return out


def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _tint(kleur, f):
    return tuple(int(max(0, min(255, c * f))) for c in kleur[:3])


def _zaad(name):
    return sum((i + 1) * ord(ch) for i, ch in enumerate(name)) % 100000


def _rand(a, kleur):
    a[0, :, :3] = kleur
    a[-1, :, :3] = kleur
    a[:, 0, :3] = kleur
    a[:, -1, :3] = kleur


def _voorkant(name, basis, accent, staat, luik=True):
    """The front of a machine: its colour with a dark frame, the face, and (luik) a hatch in the accent colour below it."""
    a = _ruis(basis, 5, _zaad(name) + 1)
    _rand(a, _tint(basis, 0.72))
    if luik:
        a[9:14, 4:12, :3] = _tint(accent, 0.55 if staat == "slaapt" else 1.0)
        for x, y in ((4, 9), (11, 9), (4, 13), (11, 13)):
            a[y, x, :3] = basis[:3]
        a[8, 5:11, :3] = _tint(basis, 0.72)
        a[14, 5:11, :3] = _tint(basis, 0.72)
    img = Image.fromarray(a)
    snoet(img, 2, 4, staat)
    return img


def _zijkant(name, basis, accent):
    a = _ruis(basis, 6, _zaad(name) + 2)
    _rand(a, _tint(basis, 0.72))
    a[11:13, 1:15, :3] = accent[:3]                       # a band in the accent colour
    for x, y in ((2, 2), (13, 2), (2, 8), (13, 8)):       # rivets
        a[y, x, :3] = _tint(basis, 1.18)
    return Image.fromarray(a)


def _bovenkant(name, basis):
    a = _ruis(_tint(basis, 1.1), 4, _zaad(name) + 3)
    _rand(a, _tint(basis, 0.72))
    for ox in (2, 10):                                    # the ear colours, under the ear elements (see oren)
        a[1:3, ox:ox + 4, :3] = OOR
        a[1:3, ox + 1:ox + 3, :3] = OOR_BINNEN
    return Image.fromarray(a)


def _kubus(voor, zij, boven):
    """A full block with its front on the north face, plus the two ears."""
    f = lambda t, cull: {"texture": t, "cullface": cull}
    return {"parent": "minecraft:block/block",
            "textures": {"particle": zij, "voor": voor, "zij": zij, "boven": boven},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                          "faces": {"north": f("#voor", "north"), "south": f("#zij", "south"), "west": f("#zij", "west"),
                                    "east": f("#zij", "east"), "up": f("#boven", "up"), "down": f("#zij", "down")}},
                         *oren("#boven")]}


def toon(h, *ids):
    """Adds blocks ("guhs:x" or "x") to the block tag guhs:vadskracht: the hover readout works on them."""
    h.add_tag("guhs/tags/block/vadskracht", [i if ":" in i else f"guhs:{i}" for i in ids])


def machine(h, name, basis, accent, voor=None, extra_tags=(), item=True):
    """
    Everything a one-block guh machine needs to be seen (Java: a MachineBlock, properties facing + snoet).
    basis / accent: (r, g, b); voor(img, snoet): paints your own details on the 16 x 16 front after the face is on it.
    item=False: no item model and no loot table (a block without an item).
    """
    t = lambda k: f"guhs:block/{name}_{k}"
    for staat in STATEN:
        img = _voorkant(name, basis, accent, staat)
        if voor:
            voor(img, staat)
        h.save(img, "block", f"{name}_voor_{staat}.png")
        h.w(f"{h.A}/models/block/{name}_{staat}.json", _kubus(t(f"voor_{staat}"), t("zij"), t("boven")))
    h.save(_zijkant(name, basis, accent), "block", f"{name}_zij.png")
    h.save(_bovenkant(name, basis), "block", f"{name}_boven.png")
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": f"guhs:block/{name}_{staat}", **({"y": r} if r else {})}
        for f, r in ROT.items() for staat in STATEN}})
    if item:
        h.w(f"{h.A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_werkt"})
        h.self_drop(name)
    toon(h, name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{name}"])
    for tag in extra_tags:
        h.add_tag(tag, [f"guhs:{name}"])


def batterij(h, name, basis, accent, extra_tags=(), item=True):
    """
    Everything a battery needs to be seen (Java: a BatterijBlock, properties facing + lading 0..4): the front shows the
    face (asleep when empty, surprised when full) and a gauge of four bars in the accent colour.
    """
    t = lambda k: f"guhs:block/{name}_{k}"
    for lading in range(5):
        staat = "slaapt" if lading == 0 else "vol" if lading == 4 else "werkt"
        img = _voorkant(name, basis, accent, staat, luik=False)
        a = np.asarray(img).copy()
        a[9:14, 2:15, :3] = _tint(basis, 0.5)             # the gauge: four bars
        for i in range(4):
            a[10:13, 3 + i * 3:5 + i * 3, :3] = accent[:3] if i < lading else _tint(basis, 0.7)
        h.save(Image.fromarray(a), "block", f"{name}_voor_{lading}.png")
        h.w(f"{h.A}/models/block/{name}_{lading}.json", _kubus(t(f"voor_{lading}"), t("zij"), t("boven")))
    h.save(_zijkant(name, basis, accent), "block", f"{name}_zij.png")
    h.save(_bovenkant(name, basis), "block", f"{name}_boven.png")
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {
        f"facing={f},lading={lading}": {"model": f"guhs:block/{name}_{lading}", **({"y": r} if r else {})}
        for f, r in ROT.items() for lading in range(5)}})
    if item:
        h.w(f"{h.A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_2"})
        h.self_drop(name)
    toon(h, name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{name}"])
    for tag in extra_tags:
        h.add_tag(tag, [f"guhs:{name}"])


# =====================================================================================================================
# what the foundation itself needs
# =====================================================================================================================
ROZE = (232, 150, 190)
PAARS = (150, 110, 205)
GEEL = (255, 214, 92)


def blocks(h):
    # the blocks that exist already
    toon(h, "guh_wheel", "guh_wheel_part", "guh_wire", "guh_oven", "machine_deel")
    # the invisible part of machines bigger than one block (like guh_wheel_part: only a particle texture)
    h.save(_zijkant("machine_deel", ROZE, PAARS), "block", "machine_deel.png")
    h.w(f"{h.A}/models/block/machine_deel.json", {"textures": {"particle": "guhs:block/machine_deel"}})
    h.w(f"{h.A}/blockstates/machine_deel.json", {"multipart": [{"apply": {"model": "guhs:block/machine_deel"}}]})
    # the test blocks (no items): a source, a battery, a machine of one block and one of 2 x 2
    a = _ruis(GEEL, 6, 9101)
    _rand(a, _tint(GEEL, 0.7))
    img = Image.fromarray(a)
    snoet(img, 2, 5, "werkt")
    h.save(img, "block", "vadskracht_testbron.png")
    h.w(f"{h.A}/models/block/vadskracht_testbron.json", {"parent": "minecraft:block/cube_all",
                                                         "textures": {"all": "guhs:block/vadskracht_testbron"}})
    h.w(f"{h.A}/blockstates/vadskracht_testbron.json", {"variants": {"": {"model": "guhs:block/vadskracht_testbron"}}})
    toon(h, "vadskracht_testbron")
    batterij(h, "vadskracht_testbatterij", (120, 200, 215), GEEL, item=False)
    machine(h, "vadskracht_testmachine", ROZE, PAARS, item=False)
    h.w(f"{h.A}/blockstates/vadskracht_testmachine_groot.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": f"guhs:block/vadskracht_testmachine_{staat}", **({"y": r} if r else {})}
        for f, r in ROT.items() for staat in STATEN}})
    toon(h, "vadskracht_testmachine_groot")
    # the fluids of the Guh-technologie (Java: Sauzen.TECHNIEK). minecraft:milk is NeoForge's milk fluid, switched on by
    # VadskrachtFeature; "required": false so the tag also loads where that fluid does not exist (tools/check_datapack26.py)
    h.w(f"{h.D}/tags/fluid/techniek_sauzen.json", {"replace": False, "values": [
        "guhs:kaas_saus", "guhs:kaasfrituursaus", "minecraft:water", {"id": "minecraft:milk", "required": False}]})


def guhrad_tabel(h):
    """data/guhs/vadskracht/guhrad.json: what a guh gives in a Guhrad (Java: GuhradKracht)."""
    h.w(f"{h.D}/vadskracht/guhrad.json", {
        "standaard": {"kracht": getal("GUHRAD"), "blij": getal("GUHRAD_BLIJ")},
        "varianten": {v: {"kracht": k, "blij": b} for v, (k, b) in sorted(GUHRAD_VARIANTEN.items())}})


def test_templates(h):
    """vadskracht_test_kamer: 9 x 6 x 11 with a stone floor (room for five Guhraden next to each other)."""
    s = h.Structure((9, 6, 11))
    s.fill(0, 0, 0, 8, 0, 10, "minecraft:stone")
    s.save("vadskracht_test_kamer")


# =====================================================================================================================
# texts
# =====================================================================================================================
K = "gui.guhs.vadskracht."
TEXTS = {
    # the hover readout
    K + "gebruik": "Deze opstelling gebruikt %s/%s vadskracht",
    K + "stil.te_zwaar": "Te zwaar: er is %s vadskracht te weinig, dus alles staat stil. Njeg!",
    K + "stil.geen_bron": "Geen vadskracht: er rent nergens een guh. Zet een tamme guh in een Guhrad!",
    K + "stil.te_groot": "Te groot: meer dan %s blokken aan elkaar, daar wordt een guh duizelig van. Alles staat stil.",
    K + "op_batterij": "De batterij past %s vadskracht bij: nog %s",
    K + "buffer": "In de batterijen: %s/%s vadskracht",
    K + "geeft": "Dit geeft %s vadskracht",
    K + "verbruikt": "Dit gebruikt %s vadskracht",
    K + "staat_uit": "Dit staat uit",
    K + "batterij": "Opgeslagen: %s/%s vadskracht",
    K + "telt_niet_mee": "Telt niet mee: hooguit %s %s per opstelling",
    K + "tijd.s": "%s tellen",
    K + "tijd.min": "%s minuten",
    K + "tijd.uur": "%s uur",
    # the kinds of sources (plural, for "hooguit 4 Guhraden per opstelling")
    K + "soort.guhrad": "Guhraden",
    K + "soort.knuffelgenerator": "Knuffelgeneratoren",
    K + "soort.disco_dynamo": "Disco-dynamo's",
    K + "soort.blubkacheltje": "Blubkacheltjes",
    K + "soort.gloeisterkern": "Gloeisterkernen",
    K + "soort.guhrad.enkel": "Guhrad",
    K + "soort.knuffelgenerator.enkel": "Knuffelgenerator",
    K + "soort.disco_dynamo.enkel": "Disco-dynamo",
    K + "soort.blubkacheltje.enkel": "Blubkacheltje",
    K + "soort.gloeisterkern.enkel": "Gloeisterkern",
    # the Guhrad's own lines
    K + "guhrad.leeg": "Er rent geen guh in dit rad",
    K + "guhrad.blij": "Een blij guhtje: het rent extra hard, njeg!",
    # the Guhrad and the Guhdraad in your inventory
    "block.guhs.guh_wheel.lore": "Zet er een tamme guh in: die rent vadskracht bij elkaar voor je guhmachines. Een blij guhtje rent "
                                 "extra hard, en moe wordt het nooit.",
    "block.guhs.guh_wire.lore": "Brengt vadskracht van je Guhrad naar je guhmachines. Alles wat eraan vastzit is samen één opstelling: "
                                "kijk ernaar en je leest hoeveel vadskracht die gebruikt.",
    # blocks without an item
    "block.guhs.machine_deel": "Guhmachine",
    "block.guhs.vadskracht_testbron": "Proefbron",
    "block.guhs.vadskracht_testbatterij": "Proefbatterij",
    "block.guhs.vadskracht_testmachine": "Proefmachientje",
    "block.guhs.vadskracht_testmachine_groot": "Groot proefmachientje",
    # /guhs vadskracht
    K + "commando.niets": "Hier is geen vadskracht-opstelling",
    K + "commando.net": "Status %s: %s blokken, %s knopen (%s opstellingen in deze wereld)",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def selfcheck(h):
    A, D = h.A, h.D
    paden = [f"{A}/blockstates/{b}.json" for b in ("machine_deel", "vadskracht_testbron", "vadskracht_testbatterij",
                                                    "vadskracht_testmachine", "vadskracht_testmachine_groot")]
    paden += [f"{A}/models/block/vadskracht_testmachine_{s}.json" for s in STATEN]
    paden += [f"{A}/models/block/vadskracht_testbatterij_{i}.json" for i in range(5)]
    paden += [f"{D}/vadskracht/guhrad.json", f"{D}/tags/block/vadskracht.json", f"{D}/tags/fluid/techniek_sauzen.json",
              f"{D}/structure/vadskracht_test_kamer.nbt"]
    missing = [p for p in paden if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    for naam in ("GUHRAD", "GUHRAD_BLIJ", "GUH_OVEN", "BATTERIJ", "MAX_NET"):
        getal(naam)
    missing += [K + "soort." + soort + e for soort in bron_max() for e in ("", ".enkel") if K + "soort." + soort + e not in TEXTS]
    if missing:
        raise SystemExit(f"vadskracht: missing {missing}")


def build(h):
    blocks(h)
    guhrad_tabel(h)
    test_templates(h)
    texts(h)
    selfcheck(h)
