"""
einde2, slice geestmodellen - its two GuhNpcEntity kinds (CONTRACT_E2 11.2), both on the sitting guh ("zit"), drawn with
cast_team's toolkit (einde2_cast_team_kit, imported read-only):

  theegeest_guh   the Theegeest of the Geestenwereld's tea pavilion (DESIGN_EINDE2 5): a pale blue spirit guh, see-through
                  and softly glowing, a knitted tea cosy on his head, a jade tea kettle in his right paw, a cup on his left,
                  steam that always rises, a wisp for a tail end
  bestelguh       the Bestelguh of tech tier 5 (DESIGN_EINDE2 6): a cheerful guh with a striped apron, a paper cap, a pencil
                  behind his ear, an order pad, and the receipt he shows ("bonnetje") that unrolls

KINDS: kind -> (doorzichtig, gloed, [(animation besides idle, ticks; 0 = a pose that lasts)]) - the same table as the Java
enum GeestKinds (check() compares). Files: guh_npc_<kind>.geo.json, npc_<kind>.png (+ _glowmask), and, each kind being its
own character, guh_npc_<kind>.animation.json (animation.guh_sitting.<name>).
"""
import json
import os
import re

from features import einde2_cast_team_kit as ck
from features.einde2_cast_team_anim import _praat_zit
from features.einde2_cast_team_figuren import gewaad, mouwen
from features.einde2_cast_team_kit import T, anim, rand, strepen, weef

P = "animation.guh_sitting."
KINDS = {
    "theegeest_guh": (True, True, [("praat", 24), ("schenk", 60), ("slurp", 48)]),
    "bestelguh": (False, False, [("praat", 24), ("bonnetje", 48), ("ren", 0)]),
}
# the poses that last (a loop)
HOUDINGEN = {("bestelguh", "ren")}


# =====================================================================================================================
# the looks
# =====================================================================================================================
def theegeest(h, kind="theegeest_guh"):
    f = ck.Figuur(h, kind, "zit", (0.55, 0.32, 1.10))
    f.kleur("muts", (244, 214, 120), 5, strepen((236, 150, 170), 8, 3))
    f.kleur("pompon", (252, 252, 255), 4, gloed=True)
    f.kleur("ketel", (150, 196, 170), 6)
    f.kleur("ketel_donker", (96, 140, 120), 6)
    f.kleur("porselein", (248, 248, 252), 3, rand((100, 140, 220), 4))
    f.kleur("thee", (196, 140, 70), 4)
    f.kleur("stoom", (236, 244, 255), 3, gloed=True)
    f.kleur("sliert", (190, 230, 255), 4, gloed=True)
    f.bot("theegeest_muts", "head", (0, 25, 0), [                               # a knitted tea cosy, with a pompom
        f.k((-5.6, 23.4, -5.6), (11.2, 3.8, 11.2), "muts"),
        f.k((-4.4, 27.2, -4.4), (8.8, 1.2, 8.8), "muts"),
        f.k((-1.3, 28.4, -1.3), (2.6, 2.6, 2.6), "pompon")])
    f.bot("theegeest_ketel", "arm_right", (-3, 9, -5), [                         # the jade kettle, held by its handle
        f.k((-7.5, 3.6, -10.2), (5.0, 4.4, 5.0), "ketel"),
        f.k((-6.7, 8.0, -9.4), (3.4, 0.8, 3.4), "ketel_donker"),
        f.k((-5.5, 8.8, -8.2), (1.0, 0.8, 1.0), "ketel_donker"),
        f.k((-9.6, 5.6, -8.4), (2.2, 1.2, 1.2), "ketel"),
        f.k((-5.5, 9.8, -10.0), (1.0, 0.8, 4.6), "ketel_donker"),
        f.k((-5.5, 8.0, -10.0), (1.0, 1.8, 0.8), "ketel_donker"),
        f.k((-5.5, 8.0, -6.2), (1.0, 1.8, 0.8), "ketel_donker")])
    f.bot("theegeest_stoom", "theegeest_ketel", (-10, 7, -8), [
        f.k((-10.2, 7.4, -8.4), (1.2, 1.2, 1.2), "stoom"), f.k((-10.6, 9.0, -8.2), (0.9, 0.9, 0.9), "stoom")])
    f.bot("theegeest_kopje", "arm_left", (3, 10, -5.5), [
        f.k((1.9, 10.4, -7.0), (2.8, 2.0, 2.8), "porselein"),
        f.k((2.2, 12.3, -6.7), (2.2, 0.2, 2.2), "thee")])
    f.bot("theegeest_stoom_kop", "theegeest_kopje", (3.3, 13, -5.6), [
        f.k((2.8, 12.8, -6.1), (1.0, 1.0, 1.0), "stoom"), f.k((3.2, 14.2, -5.9), (0.8, 0.8, 0.8), "stoom")])
    f.bot("theegeest_sliert", "tail", (0, 1, 11), [                              # a spirit's wisp at the tail end
        f.k((-1.0, 0.2, 11.2), (2.0, 1.6, 2.4), "sliert"),
        f.k((-0.8, 1.2, 13.0), (1.6, 1.6, 1.6), "sliert"),
        f.k((-0.6, 2.4, 14.0), (1.2, 1.4, 1.2), "sliert")])
    f.doorzichtig(172)
    f.bewaar(gloed=True)


def bestelguh(h, kind="bestelguh"):
    f = ck.Figuur(h, kind, "zit", (0.97, 0.82, 1.0))
    hemd, rood = (246, 238, 214), (214, 60, 56)
    gewaad(f, [(0, 14, hemd)])
    mouwen(f, hemd, rood)
    f.kleur("schort", rood, 5, strepen((250, 246, 240), 8, 3, staand=True))
    f.kleur("band", (170, 40, 40), 5)
    f.kleur("muts", (250, 250, 248), 4, rand(rood, 3))
    f.kleur("bon", (252, 250, 244), 2, strepen((150, 150, 164), 5, 1))
    f.kleur("potlood", (246, 204, 60), 4)
    f.kleur("punt", (50, 44, 40), 3)
    f.bot("bestelguh_schort", "body", (0, 8, -4), [
        f.k((-4.4, 2.4, -4.9), (8.8, 7.4, 1.0), "schort"),
        f.k((-5.3, 9.4, -4.4), (10.6, 0.9, 8.8), "band"),
        f.k((-1.4, 3.6, -5.2), (2.8, 2.2, 0.5), "band")])                        # the pocket
    f.bot("bestelguh_muts", "head", (0, 25, 0), [
        f.k((-4.4, 24.6, -4.6), (8.8, 2.6, 8.8), "muts"),
        f.k((-3.6, 27.2, -3.2), (7.2, 0.8, 6.4), "muts")])
    f.bot("bestelguh_potlood", "head", (7.4, 24, -2), [
        f.k((7.0, 23.6, -5.2), (0.8, 0.8, 6.4), "potlood"),
        f.k((7.1, 23.7, -6.0), (0.6, 0.6, 0.8), "punt")])
    f.bot("bestelguh_blok", "arm_right", (-3, 9, -7), [                          # the order pad
        f.k((-4.6, 8.0, -8.2), (3.0, 0.6, 2.2), "bon"),
        f.k((-4.6, 8.6, -8.2), (3.0, 0.25, 2.2), "band")])
    f.bot("bestelguh_bonnetje", "arm_left", (3.1, 10.4, -7.3), [                  # the receipt: it unrolls downwards
        f.k((1.8, 4.6, -7.5), (2.6, 5.8, 0.3), "bon")])
    f.bewaar()


TEKEN = {"theegeest_guh": theegeest, "bestelguh": bestelguh}


# =====================================================================================================================
# the animations
# =====================================================================================================================
def _theegeest():
    schenk = (3.0, {
        "arm_right": {"rotation": T((0, 0, 0, 0), (0.6, -45, 20, 0), (1.0, -45, 20, -35), (2.2, -45, 20, -35), (2.6, -45, 20, 0), (3.0, 0, 0, 0))},
        "arm_left": {"rotation": T((0, 0, 0, 0), (0.6, -35, -15, 0), (2.4, -35, -15, 0), (3.0, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.6, 12, -8, 0), (2.4, 12, -8, 0), (3.0, 0, 0, 0))}})
    slurp = (2.4, {
        "arm_left": {"rotation": T((0, 0, 0, 0), (0.5, -75, 25, 0), (1.6, -75, 25, 0), (2.1, 0, 0, 0), (2.4, 0, 0, 0))},
        "head": {"rotation": T((0, 0, 0, 0), (0.5, -12, 10, 0), (1.6, -12, 10, 0), (2.1, 0, 0, 4), (2.4, 0, 0, 0))},
        "body": {"position": T((0, 0, 0, 0), (1.7, 0, 0, 0), (1.85, 0, 0.8, 0), (2.0, 0, 0, 0), (2.15, 0, 0.6, 0), (2.4, 0, 0, 0))},
        "ear_left": {"rotation": T((0, 0, 0, 0), (1.7, 0, 0, 0), (1.9, 0, 0, -14), (2.4, 0, 0, 0))},
        "ear_right": {"rotation": T((0, 0, 0, 0), (1.7, 0, 0, 0), (1.9, 0, 0, 14), (2.4, 0, 0, 0))}})
    return {"schenk": schenk, "slurp": slurp}


def _stoom(bot, fase):
    stappen = [i * 0.5 for i in range(25)]
    return {bot: {"position": T(*[(t, 0, ((t + fase) % 2.0) * 1.4, 0) for t in stappen]),
                  "scale": T(*[(t, 1.0 - ((t + fase) % 2.0) * 0.35) for t in stappen])}}


def _bestelguh():
    bonnetje = (2.4, {
        "arm_left": {"rotation": T((0, 0, 0, 0), (0.4, -70, -10, 0), (2.0, -70, -10, 0), (2.4, 0, 0, 0))},
        "bestelguh_bonnetje": {"rotation": T((0, 0, 0, 0), (0.4, 70, 0, 0), (2.0, 70, 0, 0), (2.4, 0, 0, 0)),
                               "scale": T((0, 1, 1, 1), (0.4, 1, 1, 1), (1.0, 1, 2.6, 1), (2.0, 1, 2.6, 1), (2.4, 1, 1, 1))},
        "head": {"rotation": T((0, 0, 0, 0), (0.5, -8, -12, 0), (1.2, 4, -12, 0), (1.6, -6, -12, 0), (2.4, 0, 0, 0))}})
    ren = (0.5, {
        "body": {"position": T((0, 0, 0, 0), (0.125, 0, 1.0, 0), (0.25, 0, 0, 0), (0.375, 0, 1.0, 0), (0.5, 0, 0, 0))},
        "arm_left": {"rotation": T((0, -40, 0, 0), (0.25, 30, 0, 0), (0.5, -40, 0, 0))},
        "arm_right": {"rotation": T((0, 30, 0, 0), (0.25, -40, 0, 0), (0.5, 30, 0, 0))},
        "foot_left": {"rotation": T((0, -25, 0, 0), (0.25, 20, 0, 0), (0.5, -25, 0, 0))},
        "foot_right": {"rotation": T((0, 20, 0, 0), (0.25, -25, 0, 0), (0.5, 20, 0, 0))},
        "ear_left": {"rotation": T((0, -20, 0, -10), (0.25, -10, 0, -4), (0.5, -20, 0, -10))},
        "ear_right": {"rotation": T((0, -20, 0, 10), (0.25, -10, 0, 4), (0.5, -20, 0, 10))}})
    return {"bonnetje": bonnetje, "ren": ren}


MAKERS = {"theegeest_guh": _theegeest, "bestelguh": _bestelguh}
ALTIJD = {"theegeest_guh": {**_stoom("theegeest_stoom", 0.0), **_stoom("theegeest_stoom_kop", 1.0),
                            "theegeest_sliert": {"rotation": T(*[(float(t), 0, 12 if t % 2 else -12, 0) for t in range(13)])}}}


def animaties(kind):
    zetten = MAKERS[kind]()
    out = {}
    for naam, _ in KINDS[kind][2][1:]:
        lengte, botten = zetten[naam]
        out[naam] = anim(lengte, botten, lus=(kind, naam) in HOUDINGEN)
    eerste = out[KINDS[kind][2][1][0]]
    idle = weef(12.0, "zit", eerste, 5.0, extra=ALTIJD.get(kind))
    if (kind, KINDS[kind][2][1][0]) in HOUDINGEN:
        idle = weef(12.0, "zit", anim(1.0, {}), 5.0, extra=ALTIJD.get(kind))
    return {"idle": idle, "praat": anim(1.2, _praat_zit()), **out}


def bestand(kind):
    return {"format_version": "1.8.0", "animations": {P + naam: a for naam, a in animaties(kind).items()}}


def build(h):
    for kind, teken in TEKEN.items():
        teken(h, kind)
        h.w(os.path.join(h.A, "geckolib", "animations", "entity", f"guh_npc_{kind}.animation.json"), bestand(kind))


def check(h):
    fout = []
    for kind, (door, gloed, anims) in KINDS.items():
        geo_pad = os.path.join(h.A, "geckolib", "models", "entity", f"guh_npc_{kind}.geo.json")
        an_pad = os.path.join(h.A, "geckolib", "animations", "entity", f"guh_npc_{kind}.animation.json")
        tex = os.path.join(h.TEX, "entity", f"npc_{kind}.png")
        ontbreekt = [p for p in (geo_pad, an_pad, tex) if not os.path.exists(p)]
        fout += [f"{kind}: {p} is missing" for p in ontbreekt]
        if gloed != os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}_glowmask.png")):
            fout.append(f"{kind}: its glowmask is {'missing' if gloed else 'not expected'}")
        if ontbreekt:
            continue
        geo = json.load(open(geo_pad, encoding="utf-8"))["minecraft:geometry"][0]
        botten = {b["name"] for b in geo["bones"]}
        if geo["description"]["identifier"] != f"geometry.guh_npc_{kind}":
            fout.append(f"{kind}: identifier {geo['description']['identifier']}")
        for b in ck.BASISBOTTEN["zit"]:
            if b not in botten:
                fout.append(f"{kind}: base bone {b} is gone")
        for b in botten - set(ck.BASISBOTTEN["zit"]):
            if not b.startswith(kind.split("_")[0] + "_"):
                fout.append(f"{kind}: extra bone {b} is not named {kind.split('_')[0]}_<ding>")
        bestandje = json.load(open(an_pad, encoding="utf-8"))["animations"]
        for naam in ["idle"] + [n for n, _ in anims]:
            if P + naam not in bestandje:
                fout.append(f"{kind}: no animation {naam}")
        for naam, a in bestandje.items():
            for b in a.get("bones", {}):
                if b not in botten:
                    fout.append(f"{kind}: {naam} moves a missing bone {b}")
        for naam, t in anims:
            lengte = bestandje.get(P + naam, {}).get("animation_length", 0)
            if t and abs(lengte * 20 - t) > 1:
                fout.append(f"{kind}: {naam} lasts {lengte} s, the table says {t} ticks")
            if (t == 0) != bool(bestandje.get(P + naam, {}).get("loop")):
                fout.append(f"{kind}: {naam} must loop exactly when it is a pose")
    pad = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "geestwezens", "GeestKinds.java")
    java = open(pad, encoding="utf-8").read() if os.path.exists(pad) else ""
    for kind, (door, gloed, anims) in KINDS.items():
        wil = f'GuhNpcEntity.Kind.{kind.upper()}, {str(door).lower()}, {str(gloed).lower()}, "' + ",".join(f"{n}:{t}" for n, t in anims) + '"'
        m = re.search(r"^\s*" + kind.upper() + r"\((.*?)\)[,;]\s*$", java, re.M)
        if not m or " ".join(m.group(1).split()) != wil:
            fout.append(f"GeestKinds.{kind.upper()} does not say ({wil})")
    return fout
