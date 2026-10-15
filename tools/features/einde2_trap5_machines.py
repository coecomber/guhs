"""
einde2 phase C (CONTRACT_E2_B 7.12, DESIGN 6), slice trap5_machines: tech tier 5. The Werktuigguh's workshop and his
questline of projects, the machines and the two new sources, all on main's vadskracht.
Java: nl.juiced.guhs.feature.einde2.trap5 (E2Trap5Feature).

  lang      block.guhs.<machine> (+ .lore and the messages), item.guhs.einde2_trap5_machines_*, entity.guhs.bestelguh,
            gui.guhs.einde2_trap5_machines.*, quest.guhs.einde2_trap5_machines.werktuigguh.*, gui.guhs.verhalen.werktuigguh.*,
            scene.guhs.einde2_trap5_machines_werkplaats.*, gui.guhs.vadskracht.soort.enderguhrad|komeetkern(.enkel),
            advancements.guhs.guheinde.einde2_trap5_machines_*            (texts: einde2_trap5_machines_tekst.py)
  assets    the twelve machine blocks (vadskracht.machine: the guh face, the ears, a front of their own), the Luchtblazer's
            "omhoog" states, the Enderguhrad's plinth and its stand + ring (children of the Guhrad's models in ender colours),
            the Bouwplan and the diploma
  data      recipes (every machine needs the Bouwplan: it stays in the grid), loot tables, tags (vadskracht, pickaxe), the
            Bestelguh's (empty) loot table, advancements quest/werktuigguh_stap_<i> (the line), quest/einde2_trap5_machines_*
            (hidden, the FTB tasks) and guheinde/einde2_trap5_machines_* (visible)
  FTB       a section of its own in the chapter guhs_techniek ("Trap 5: de werkplaats van de Werktuigguh", 11 quests)

Chunk c100_e2_trap5_machines; English: tools/lang/en/c100_e2_trap5_machines.json; wiki texts: einde2_trap5_machines_wiki.py.
"""
import os

import numpy as np
from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import vadskracht, verhaal_motor
from features import einde2_trap5_machines_tekst as tekst

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("einde2_trap5_machines", tekst.SECTIE, "item:guhs:block/vadszender_voor_werkt", None)]

# the two sources of tier 5 (Java: BronSoort.ENDERGUHRAD, KOMEETKERN; features/vadskracht.py leaves their names to this module)
K = "gui.guhs.vadskracht.soort."
BRONNEN = {"enderguhrad": ("Enderguhraden", "Enderguhrad"), "komeetkern": ("Komeetkernen", "Komeetkern")}

BOUWPLAN = "guhs:einde2_trap5_machines_bouwplan"

# machine colours (basis, accent)
KLEUREN = {
    "vadszender": ((92, 176, 214), (255, 214, 92)),
    "vadsontvanger": ((112, 196, 224), (255, 180, 214)),
    "guhterminal": ((226, 140, 186), (70, 52, 96)),
    "bestelbalie": ((196, 150, 104), (250, 236, 210)),
    "knabbelpoort_buis": ((132, 92, 196), (220, 140, 255)),
    "thuispoort": ((96, 62, 150), (196, 150, 255)),
    "luchtblazer": ((206, 214, 226), (150, 196, 230)),
    "waterdrager": ((80, 132, 214), (170, 220, 255)),
    "korstduwer": ((150, 110, 76), (226, 196, 120)),
    "vuurkorf": ((196, 72, 48), (255, 190, 64)),
    "komeetkern": ((40, 36, 84), (255, 214, 120)),
}
ENDER = ((150, 60, 170), (255, 200, 90))


def build(h):
    texts(h)
    machines(h)
    enderguhrad(h)
    items(h)
    recipes(h)
    data(h)
    selfcheck(h)


# =====================================================================================================================
# texts
# =====================================================================================================================
def texts(h):
    for id, (meer, een) in BRONNEN.items():
        kit.lang(h, K + id, meer)
        kit.lang(h, K + id + ".enkel", een)
    for id, (naam, lore) in tekst.BLOKKEN.items():
        kit.lang(h, f"block.guhs.{id}", naam)
        kit.lang(h, f"block.guhs.{id}.lore", lore)
    for id, berichten in tekst.BERICHTEN.items():
        for k, v in berichten.items():
            kit.lang(h, f"block.guhs.{id}.{k}", v)
    for id, (naam, lore) in tekst.ITEMS.items():
        kit.lang(h, f"item.guhs.{id}", naam)
        kit.lang(h, f"item.guhs.{id}.lore", lore)
    for k, v in tekst.ENTITY.items():
        kit.lang(h, k, v)
    for k, v in tekst.GUI.items():
        kit.lang(h, tekst.P + k, v)
    for k, v in tekst.WERKTUIGGUH.items():
        kit.lang(h, tekst.Q + k, v)
    titel, regels, namen = tekst.SCENE_TEKST
    verhaal_motor.scene(h, tekst.SCENE, titel, regels, namen)
    verhaal_motor.verhaallijn(h, tekst.LIJN, tekst.LIJN_NAAM, tekst.LIJN_UITLEG, tekst.STAPPEN, klaar=tekst.KLAAR)


# =====================================================================================================================
# the machines (vadskracht.machine: the standard guh machine with a front of its own)
# =====================================================================================================================
def _px(img, pts, kleur):
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), tuple(kleur[:3]) + (255,))


def _donker(c, f=0.6):
    return tuple(int(v * f) for v in c[:3])


def _licht(c, f=1.25):
    return tuple(min(255, int(v * f)) for v in c[:3])


def _voor_zender(accent):
    def verf(img, staat):
        # a little antenna on top of the face, with waves going out when it sends
        _px(img, [(7, 0), (8, 0), (7, 1), (8, 1), (7, 2), (8, 2)], (90, 90, 110))
        _px(img, [(7, 0), (8, 0)], accent)
        if staat != "slaapt":
            _px(img, [(5, 1), (4, 2), (10, 1), (11, 2), (3, 0), (12, 0)], accent)
        _hatch(img, staat, accent, "golf")
    return verf


def _voor_ontvanger(accent):
    def verf(img, staat):
        # a dish: a little bowl above the face
        _px(img, [(5, 1), (6, 2), (7, 2), (8, 2), (9, 2), (10, 1)], (210, 210, 230))
        _px(img, [(7, 0), (8, 0)], accent)
        _hatch(img, staat, accent, "golf_in")
    return verf


def _hatch(img, staat, accent, soort):
    """Paints a sign on the hatch (rows 9..13, columns 4..11)."""
    aan = staat != "slaapt"
    if soort not in ("scherm", "bon", "boog"):
        # a dark window in the hatch, the sign light on it (lit when the machine works)
        for x in range(4, 12):
            for y in range(9, 14):
                img.putpixel((x, y), tuple(_donker(accent, 0.42 if aan else 0.3)) + (255,))
    c = _licht(accent, 1.35) if aan else _donker(accent, 0.75)
    if soort == "golf":
        _px(img, [(5, 11), (6, 10), (7, 11), (8, 12), (9, 11), (10, 10)], c)
    elif soort == "golf_in":
        _px(img, [(5, 10), (6, 11), (7, 12), (8, 12), (9, 11), (10, 10)], c)
    elif soort == "scherm":
        for x in range(4, 12):
            for y in range(9, 14):
                img.putpixel((x, y), (40, 30, 60, 255) if not aan else (60, 44, 92, 255))
        if aan:
            _px(img, [(5, 10), (7, 10), (9, 10), (5, 12), (7, 12), (9, 12), (10, 12)], (255, 214, 232))
    elif soort == "bon":
        for x in range(6, 10):
            for y in range(9, 14):
                img.putpixel((x, y), (250, 246, 236, 255))
        _px(img, [(7, 10), (8, 10), (7, 12)], (120, 100, 90))
    elif soort == "portaal":
        _px(img, [(6, 10), (7, 9), (8, 9), (9, 10), (9, 11), (8, 12), (7, 12), (6, 11)], c)
        _px(img, [(7, 10), (8, 11)], _licht(c, 1.3))
    elif soort == "boog":
        for y in range(9, 14):
            _px(img, [(5, y), (10, y)], c)
        _px(img, [(6, 8), (7, 8), (8, 8), (9, 8)], c)
        for x in range(6, 10):
            for y in range(9, 14):
                img.putpixel((x, y), tuple(_licht(c, 1.2) if aan else (50, 30, 70)) + (255,))
    elif soort == "waaier":
        _px(img, [(7, 11), (8, 11), (6, 10), (5, 9), (9, 12), (10, 13), (9, 10), (10, 9), (6, 12), (5, 13)], c)
    elif soort == "druppel":
        _px(img, [(7, 9), (8, 9), (6, 10), (7, 10), (8, 10), (9, 10), (6, 11), (7, 11), (8, 11), (9, 11), (7, 12), (8, 12)], c)
        _px(img, [(7, 10)], (240, 250, 255))
    elif soort == "pijl":
        _px(img, [(5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (8, 10), (8, 12), (9, 10), (9, 12), (10, 11)], c)
    elif soort == "vlam":
        if aan:
            _px(img, [(7, 9), (6, 10), (7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12)],
                (255, 140, 40))
            _px(img, [(7, 11), (8, 11), (7, 12), (8, 12)], (255, 230, 120))
        else:
            _px(img, [(6, 12), (7, 12), (8, 12), (9, 12)], (90, 60, 50))
    elif soort == "ster":
        _px(img, [(7, 9), (7, 10), (5, 11), (6, 11), (8, 11), (9, 11), (7, 12), (7, 13)], (255, 240, 180))
        _px(img, [(7, 11)], (255, 255, 255))
        _px(img, [(4, 9), (10, 13), (11, 9)], (255, 214, 120))


def _voor(soort, accent):
    def verf(img, staat):
        _hatch(img, staat, accent, soort)
    return verf


FRONTEN = {
    "vadszender": lambda a: _voor_zender(a),
    "vadsontvanger": lambda a: _voor_ontvanger(a),
    "guhterminal": lambda a: _voor("scherm", a),
    "bestelbalie": lambda a: _voor("bon", a),
    "knabbelpoort_buis": lambda a: _voor("portaal", a),
    "thuispoort": lambda a: _voor("boog", a),
    "luchtblazer": lambda a: _voor("waaier", a),
    "waterdrager": lambda a: _voor("druppel", a),
    "korstduwer": lambda a: _voor("pijl", a),
    "vuurkorf": lambda a: _voor("vlam", a),
    "komeetkern": lambda a: _voor("ster", a),
}


def machines(h):
    for id, (basis, accent) in KLEUREN.items():
        vadskracht.machine(h, id, basis, accent, voor=FRONTEN[id](accent))
    # the Luchtblazer: also "omhoog" (placed while looking down: it blows up, its front faces the sky)
    name = "luchtblazer"
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {
        f"facing={f},omhoog={o},snoet={staat}": {"model": f"guhs:block/{name}_{staat}", **({"x": 270} if o == "true" else {}),
                                                **({"y": r} if r else {})}
        for f, r in vadskracht.ROT.items() for o in ("false", "true") for staat in vadskracht.STATEN}})


# =====================================================================================================================
# the Enderguhrad: a low plinth with the face (the block), and the Guhrad's stand and ring in ender colours (drawn big)
# =====================================================================================================================
def _ruis(kleur, seed, var=8):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, var, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def enderguhrad(h):
    basis, accent = ENDER
    name = "enderguhrad"
    for staat in vadskracht.STATEN:
        a = _ruis(_donker(basis, 0.8), kit.seed(f"{name}_voor"), 6)
        a[0, :, :3] = _donker(basis, 0.5)
        a[-1, :, :3] = _donker(basis, 0.5)
        img = Image.fromarray(a)
        vadskracht.snoet(img, 2, 10, staat)
        h.save(img, "block", f"{name}_voor_{staat}.png")
        t = lambda k: f"guhs:block/{name}_{k}"
        h.w(f"{h.A}/models/block/{name}_{staat}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": t("zij"), "voor": t(f"voor_{staat}"), "zij": t("zij"), "boven": t("boven")},
            "elements": [{"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
                "north": {"texture": "#voor", "uv": [0, 10, 16, 16], "cullface": "north"},
                "south": {"texture": "#zij", "uv": [0, 10, 16, 16], "cullface": "south"},
                "west": {"texture": "#zij", "uv": [0, 10, 16, 16], "cullface": "west"},
                "east": {"texture": "#zij", "uv": [0, 10, 16, 16], "cullface": "east"},
                "up": {"texture": "#boven"}, "down": {"texture": "#zij", "cullface": "down"}}}]})
    zij = _ruis(_donker(basis, 0.7), kit.seed(f"{name}_zij"), 6)
    zij[12:14, :, :3] = accent
    h.save(Image.fromarray(zij), "block", f"{name}_zij.png")
    boven = _ruis(basis, kit.seed(f"{name}_boven"), 6)
    for x in range(16):
        if x % 4 == 1:
            boven[7:9, x, :3] = accent
    h.save(Image.fromarray(boven), "block", f"{name}_boven.png")
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {
        f"facing={f},snoet={staat}": {"model": f"guhs:block/{name}_{staat}", **({"y": r} if r else {})}
        for f, r in vadskracht.ROT.items() for staat in vadskracht.STATEN}})
    # the wheel: the Guhrad's ring and stand, recoloured (children of guhs:block/guh_wheel_ring / _stand)
    ring = _ruis((88, 40, 120), kit.seed("enderguhrad_ring"), 10)
    for x in range(16):
        ring[x % 16, x, :3] = (255, 140, 214)
        ring[(x + 8) % 16, x, :3] = accent
    h.save(Image.fromarray(ring), "block", "enderguhrad_ring.png")
    stand = _ruis((54, 30, 76), kit.seed("enderguhrad_stand"), 8)
    stand[::5, :, :3] = accent
    h.save(Image.fromarray(stand), "block", "enderguhrad_stand.png")
    h.w(f"{h.A}/models/block/enderguhrad_ring.json", {"parent": "guhs:block/guh_wheel_ring", "textures": {
        "particle": "guhs:block/enderguhrad_ring", "coral": "guhs:block/enderguhrad_ring", "stand": "guhs:block/enderguhrad_stand"}})
    h.w(f"{h.A}/models/block/enderguhrad_stand.json", {"parent": "guhs:block/guh_wheel_stand", "textures": {
        "particle": "guhs:block/enderguhrad_stand", "stand": "guhs:block/enderguhrad_stand"}})
    h.w(f"{h.A}/models/item/{name}.json", {"parent": "guhs:block/enderguhrad_ring"})
    h.self_drop(name)
    vadskracht.toon(h, name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{name}"])


# =====================================================================================================================
# items: the Bouwplan and the diploma
# =====================================================================================================================
def items(h):
    plan = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(plan)
    d.rectangle((2, 2, 13, 13), fill=(66, 112, 190, 255))
    d.rectangle((2, 2, 13, 13), outline=(220, 236, 255, 255))
    for x in (4, 7, 10):
        d.line((x, 4, x, 11), fill=(140, 180, 240, 255))
    d.ellipse((5, 5, 10, 10), outline=(255, 255, 255, 255))
    d.line((7, 7, 9, 5), fill=(255, 214, 92, 255))
    plan.putpixel((12, 3), (255, 140, 172, 255))
    plan.putpixel((3, 12), (255, 140, 172, 255))
    h.save(plan, "item", "einde2_trap5_machines_bouwplan.png")
    dip = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(dip)
    d.rectangle((1, 3, 14, 12), fill=(248, 238, 214, 255), outline=(176, 140, 90, 255))
    for y in (5, 7, 9):
        d.line((3, y, 10, y), fill=(150, 130, 110, 255))
    d.ellipse((10, 8, 14, 12), fill=(210, 50, 70, 255))
    dip.putpixel((12, 10), (255, 214, 92, 255))
    h.save(dip, "item", "einde2_trap5_machines_diploma.png")
    h.item_model("einde2_trap5_machines_bouwplan")
    h.item_model("einde2_trap5_machines_diploma")


# =====================================================================================================================
# recipes: every one needs the Bouwplan (it stays in the grid) and the materials of the Guheinde
# =====================================================================================================================
RECEPTEN = {
    # id: (pattern, extra keys, count)   X = the Bouwplan, K = kristalkaas, S = doosschil, F = flitskruimel
    "vadszender": ([" K ", "SXS", "SFS"], {}, 1),
    "vadsontvanger": ([" K ", "SXS", "S S"], {}, 1),
    "guhterminal": (["SGS", "KXK", "SFS"], {"G": "minecraft:glass_pane"}, 1),
    "bestelbalie": (["PPP", "KXK", "SFS"], {"P": "#minecraft:planks"}, 1),
    "knabbelpoort_buis": ([" C ", "BXB", " C "], {"C": "guhs:knabbelkoorvrucht", "B": "guhs:knabbelbuis"}, 2),
    "thuispoort": (["CEC", "SXS", "SFS"], {"C": "guhs:gepofte_knabbelkoorvrucht", "E": "minecraft:ender_pearl"}, 1),
    "enderguhrad": (["CKC", "KWK", "SXS"], {"C": "guhs:gepofte_knabbelkoorvrucht", "W": "guhs:guh_wheel"}, 1),
    "luchtblazer": (["SPS", "FXF", "SSS"], {"P": "minecraft:feather"}, 1),
    "waterdrager": (["SBS", "KXK", "SSS"], {"B": "minecraft:bucket"}, 1),
    "korstduwer": (["SPS", "KXK", "SSS"], {"P": "minecraft:piston"}, 1),
    "vuurkorf": (["SBS", "KXK", "SFS"], {"B": "minecraft:campfire"}, 1),
}


def recipes(h):
    for id, (patroon, extra, n) in RECEPTEN.items():
        sleutel = {"X": BOUWPLAN, "K": "guhs:kristalkaas", "S": "guhs:doosschil", "F": "guhs:flitskruimel", **extra}
        used = {c for regel in patroon for c in regel if c != " "}
        h.shaped(id, patroon, {k: v for k, v in sleutel.items() if k in used}, f"guhs:{id}", n)


# =====================================================================================================================
# data: the Bestelguh's loot table, the advancements
# =====================================================================================================================
def data(h):
    h.w(f"{h.D}/loot_table/entities/bestelguh.json", {"type": "minecraft:entity", "pools": []})
    for naam in tekst.VERBORGEN:
        kit.verborgen(h, naam)
    for naam, ouder, icoon, frame, titel, tekst_ in tekst.ZICHTBAAR:
        kit.zichtbaar(h, naam, ouder, icoon, frame, titel, tekst_)


# =====================================================================================================================
# FTB: a section of its own in the chapter Guh-technologie (guhs_techniek)
# =====================================================================================================================
def ftb(fq):
    plek = {"werkplaats": (0, 0), "zender": (2, -1), "terminal": (4, -1), "bestel": (6, -1), "poortbuis": (8, -1), "thuispoort": (10, -1),
            "elementen": (2, 1), "enderguhrad": (4, 1), "komeet": (6, 1), "bronnen": (8, 1), "klaar": (12, 0)}
    na = {"zender": "werkplaats", "terminal": "zender", "bestel": "terminal", "poortbuis": "bestel", "thuispoort": "poortbuis",
          "elementen": "werkplaats", "enderguhrad": "elementen", "komeet": "elementen", "bronnen": "komeet", "klaar": "thuispoort"}
    groot = {"werkplaats", "komeet", "klaar"}
    for key, titel, tekst_, icoon, (soort, arg) in tekst.FTB:
        x, y = plek[key]
        deps = [f"einde2_trap5_machines_{na[key]}"] if key in na else []
        fq.q(f"einde2_trap5_machines_{key}", titel, tekst_, icoon, [fq.adv(arg)],
             rewards=(("guhs:kaas_knabbels", 32 if key in groot else 16),), deps=deps, x=x, y=y,
             shape="gear" if key in groot else None, xp=200 if key in groot else 100)


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    problems = []
    for id in list(KLEUREN) + ["enderguhrad"]:
        for p in (f"{h.A}/blockstates/{id}.json", f"{h.A}/models/item/{id}.json", f"{h.D}/loot_table/blocks/{id}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
        if f"block.guhs.{id}" not in h.NL or f"block.guhs.{id}.lore" not in h.NL:
            problems.append(f"no name or lore for {id}")
        if id != "komeetkern" and not os.path.exists(f"{h.D}/recipe/{id}.json"):
            problems.append(f"no recipe for {id}")
    for i in range(1, len(tekst.STAPPEN) + 1):
        if not os.path.exists(f"{h.D}/advancement/quest/{tekst.LIJN}_stap_{i}.json"):
            problems.append(f"no advancement {tekst.LIJN}_stap_{i}")
    if len(tekst.STAPPEN) != 9:
        problems.append(f"{len(tekst.STAPPEN)} steps, E2Trap5.STAPPEN is 9")
    for _, _, _, _, (soort, arg) in tekst.FTB:
        if soort == "adv" and not os.path.exists(f"{h.D}/advancement/quest/{arg}.json"):
            problems.append(f"FTB task without advancement: {arg}")
    for id in BRONNEN:
        if K + id not in h.NL:
            problems.append(f"no name for the source {id}")
    for zin in tekst.SCENE_TEKST[1]:
        if f"scene.guhs.{tekst.SCENE}.{zin}" not in h.NL:
            problems.append(f"no scene line {zin}")
    for p in tekst.PROJECTEN:
        if tekst.P + "project." + p not in h.NL:
            problems.append(f"no project message {p}")
    tekst.getal("KOMEETKERN")
    if problems:
        raise SystemExit("einde2_trap5_machines self-check: " + "; ".join(problems))
