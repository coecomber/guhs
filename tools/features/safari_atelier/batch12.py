"""
Safari-dieren batch 12 (testronde 4, ronde 52-53: Het Kriebelhuis, de Kriebelzaal): mier, springspin, wandelende tak,
mestkever, slak, vogelspin en hommel. Zelfde stijl, werkwijze en bestandsformaat als batch 10/11 (atelier/dieren/blad
alleen geimporteerd, niet aangepast): kleine, schattige kriebelbeestjes met het guh-gezicht waar het past.

    python tools/features/safari_atelier/batch12.py [soort ...] [--geen-vellen]

Per soort in build/safari_atelier/<soort>/: <soort>.geo.json/.png, <soort>_baby.* (een kleinere kopie: insecten hebben
eigenlijk geen 'jong'), <soort>.animation.json (idle, walk, blij, eet + per soort: mier draag, springspin hop, wandelende
tak wieg, mestkever rol, slak kruip, vogelspin zwaai, hommel vlieg), <soort>.bbmodel en <soort>_voorstel.png. Daarna de
slaapoogjes (<naam>_slaap.png), de assets in src/main/resources en batch12_overzicht.png. NIET in soorten.json.

Ook de gedeelde hulpjes van batch 12-14 (zes/acht pootjes, de vellen en 'alles') staan hier; batch13/14 importeren ze.
"""
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht       # noqa: E402
from dieren import baby                                                 # noqa: E402

UIT = os.path.abspath(os.path.join(HIER, "..", "..", "..", "build", "safari_atelier"))
SOORTEN = ["mier", "springspin", "wandelende_tak", "mestkever", "slak", "vogelspin", "hommel"]
NAMEN = {"mier": "Mier", "springspin": "Springspinnetje", "wandelende_tak": "Wandelende tak", "mestkever": "Mestkevertje",
         "slak": "Slakje", "vogelspin": "Vogelspinnetje", "hommel": "Hommel"}
kf = at.kf
S = at.S
LEEG = at.transparent


# =====================================================================================================================
# gedeelde hulpjes (ook voor batch 13/14)
# =====================================================================================================================
def verborgen(*botten):
    return {b: {"scale": kf((0, [0, 0, 0]))} for b in botten}


def extra(anims, naam, lengte, botten, loop=True):
    anims["animations"][naam] = {"loop": loop, "animation_length": lengte, "bones": botten}


def verberg_in_standaard(anims, *botten):
    for a in ("idle", "walk", "blij", "eet"):
        anims["animations"][a]["bones"].update(verborgen(*botten))


def pootjes(atlas, parent, paren, y, lengte, dik, paint, spreid=55, naam="leg"):
    """Insectenpootjes: per (z, binnen-x) een paar <naam>_l<i>/<naam>_r<i>, schuin naar buiten en omlaag."""
    bones, namen = [], []
    for i, (z, x) in enumerate(paren):
        for side, sx in (("l", 1), ("r", -1)):
            o = [x, y - dik / 2, z - dik / 2] if sx > 0 else [-x - lengte, y - dik / 2, z - dik / 2]
            n = f"{naam}_{side}{i}"
            bones.append(bone(n, parent, [sx * x, y, z], [cube(atlas, o, [lengte, dik, dik], paint)],
                              rotation=[0, 0, sx * spreid]))
            namen.append(n)
    return bones, namen


def pootjes_walk(namen, L=0.5, a=22):
    """Driepootsgang: om en om naar voren en achteren draaien (om y)."""
    out = {}
    for n in namen:
        i = int(n[-1])
        fase = (i + (0 if n.split("_")[-1][0] == "l" else 1)) % 2
        s = a if fase else -a
        out[n] = {"rotation": kf((0, [0, s, 0]), (L / 2, [0, -s, 0]), (L, [0, s, 0]))}
    return out


def maak_soort(d, soort, g, a, anims, babyk=(0.6, 0.8), baby_geo=None, baby_atlas=None):
    img = at.schrijf(d, soort, g, a, anims)
    if baby_geo is not None:
        at.schrijf(d, soort + "_baby", baby_geo, baby_atlas or a)
    elif babyk:
        at.schrijf(d, soort + "_baby", baby(g, *babyk), a)
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)
    return img


def vellen_gedeeld(soorten, alle, namen, batch, titel, uit, overzicht=True):
    import blad as bl
    from PIL import Image, ImageDraw
    bl.UIT = uit
    bl.NAMEN.update(namen)

    def heeft_baby(s):
        return os.path.exists(os.path.join(uit, s, f"{s}_baby.geo.json"))

    def vel(soort):
        varianten = [("", "volwassen")] + ([("baby", "jong")] if heeft_baby(soort) else [])
        guh = bl.guh_quads()
        maxh, maxw = bl.hoogte(guh)
        qs = {}
        for v, _ in varianten:
            qs[v] = bl.dier_quads(soort, v)
            h, w = bl.hoogte(qs[v])
            maxh, maxw = max(maxh, h), max(maxw, w)
        CW, CH, GW_ = 360, 380, 560
        ppb = min(260, (CH - 40) / maxh, (CW - 30) / (maxw * 1.05))
        W, H = 200 + CW * 3 + GW_, 90 + CH * len(varianten) + 40
        img = Image.new("RGBA", (W, H), bl.BG)
        d = ImageDraw.Draw(img)
        d.text((20, 18), f"{namen[soort]}  (safari-dieren, batch {batch}: {titel})", font=bl.font(30, True), fill=(40, 30, 40))
        d.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Animaties volgen in de game.",
               font=bl.font(16), fill=(80, 70, 80))
        for c, (lab, _, _) in enumerate(bl.VIEWS + [("naast een guh", 0, 0)]):
            d.text((200 + c * CW + 10, 88), lab, font=bl.font(16, True), fill=(90, 80, 90))
        combs, ppb_g = {}, ppb * 0.8
        for v, _ in varianten:
            dier = bl.dier_quads(soort, v)
            combs[v] = bl.naast(dier, bl.guh_quads(), dx=-(bl.xbreed(dier) / 2 + bl.xbreed(guh) / 2 + 0.3))
            sx, sy = bl.span(combs[v], 35, -18)
            ppb_g = min(ppb_g, (GW_ - 30) / sx, (CH - 50) / sy)
        for r, (v, lab) in enumerate(varianten):
            y = 110 + r * CH
            d.text((20, y + CH / 2 - 12), lab, font=bl.font(20, True), fill=(50, 40, 50))
            for c, (_, yaw, pitch) in enumerate(bl.VIEWS):
                img.alpha_composite(bl.render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20), (200 + c * CW, y))
            img.alpha_composite(bl.render_vast(combs[v], 35, -18, ppb_g, GW_, CH - 20), (200 + 3 * CW, y))
        pad = os.path.join(uit, soort, f"{soort}_voorstel.png")
        img.convert("RGB").save(pad)
        print(pad)

    for s in soorten:
        vel(s)
    if not overzicht:
        return
    import wiki_renders as wr
    items = [("guh", bl.guh_quads(), None)] + [(s, bl.dier_quads(s), bl.dier_quads(s, "baby") if heeft_baby(s) else None)
                                              for s in alle]
    quads, x = [], 0.0
    for naam, q, qb in items:
        h, w = bl.hoogte(q)
        w = max(w, 0.5)
        quads += wr.offset_quads(q, dx=(x + w / 2))
        if qb is not None:
            quads += wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.5))
        x += w + 0.6
    ppb = 170
    W, H = int(x * ppb * 0.95) + 300, 640
    t = bl.render_vast(quads, 18, -16, ppb, W, H - 100)
    img = Image.new("RGBA", (W, H), bl.BG)
    img.alpha_composite(t, (0, 90))
    d = ImageDraw.Draw(img)
    d.text((20, 18), f"Safari-dieren batch {batch} ({titel}): naast een guh (volwassen achter, jong ervoor)",
           font=bl.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), " · ".join(namen[s] for s in alle) + " (van links naar rechts, na de guh)", font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(uit, f"batch{batch}_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


def alles(module, soorten, met_vellen=True):
    """Modellen (wakker + slaapoogjes), de assets in src/main/resources (zonder soorten.json) en de vellen."""
    cwd = os.getcwd()
    os.makedirs(module.UIT, exist_ok=True)
    for s in soorten:
        module.maak(s)
        os.chdir(cwd)
    sys.path.insert(0, os.path.abspath(os.path.join(HIER, "..")))
    import safari_dieren as sd
    wakker = module.UIT
    at.SLAAP = True
    try:
        module.UIT = sd.STAGING_SLAAP
        os.makedirs(module.UIT, exist_ok=True)
        for s in soorten:
            module.maak(s)
            os.chdir(cwd)
    finally:
        at.SLAAP = False
        module.UIT = wakker
    sd.kopieer(soorten)
    sd.extra_animaties(soorten)
    # a glowing species also needs its glow mask next to the sleep texture (else the sleeping animal draws the missing
    # texture, the glimworm bug): <naam>_slaap_glowmask.png = <naam>_glowmask.png (the glowing parts have no eyes)
    for s in soorten:
        for f in os.listdir(sd.TEX_DIR):
            if f.startswith(s) and f.endswith("_glowmask.png") and "_slaap" not in f:
                naam = f[:-len("_glowmask.png")]
                if os.path.exists(os.path.join(sd.TEX_DIR, naam + "_slaap.png")):
                    import shutil
                    shutil.copyfile(os.path.join(sd.TEX_DIR, f), os.path.join(sd.TEX_DIR, naam + "_slaap_glowmask.png"))
    if met_vellen:
        module.vellen(soorten)
        os.chdir(cwd)


# =====================================================================================================================
# mier: grote kop met gezichtje en sprieten, borststukje, rond achterlijf, zes pootjes; "draag" met een blaadje
# =====================================================================================================================
MI = (190, 82, 62)
MI_D = (130, 50, 40)
MI_L = (226, 132, 104)
BLAD = (110, 186, 88)


def mier(atlas):
    glans = plain(MI, 8, lambda W, H, rng, a: disc(a, W * 0.32, H * 0.3, W * 0.16, MI_L, 0.7))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.2, 0], [cube(atlas, [-1.1, 1.4, -1.4], [2.2, 1.8, 2.6], glans)])]
    bones.append(bone("achterlijf", "body", [0, 2.6, 1.2], [cube(atlas, [-1.8, 1.4, 1.1], [3.6, 3.2, 4], glans)],
                      rotation=[-12, 0, 0]))
    bones.append(bone("head", "body", [0, 2.8, -1.4], [
        cube(atlas, [-2.1, 1.6, -4.8], [4.2, 3.8, 3.4], glans, {"north": gezicht(MI, eye_y=0.46, eye_dx=0.25, eye_r=0.2)}),
        cube(atlas, [-0.9, 1.4, -5.1], [0.6, 0.6, 0.5], plain(MI_D, 4)),                 # kaakjes
        cube(atlas, [0.3, 1.4, -5.1], [0.6, 0.6, 0.5], plain(MI_D, 4))]))
    for side, sx in (("l", 1), ("r", -1)):
        o = [0.6, 5.3, -4.0] if sx > 0 else [-0.6 - 0.4, 5.3, -4.0]
        bones.append(bone(f"spriet_{side}", "head", [sx * 0.8, 5.3, -3.8],
                          [cube(atlas, o, [0.4, 2.6, 0.4], plain(MI_D, 4)), cube(atlas, [o[0] - 0.1, 7.7, o[2] - 0.6], [0.6, 0.6, 1], plain(MI_D, 4))],
                          rotation=[-25, 0, -sx * 18]))
    pb, pn = pootjes(atlas, "body", [(-1.0, 1.0), (0.2, 1.0), (1.2, 1.0)], 2.0, 3.0, 0.5, plain(MI_D, 4), spreid=40)
    bones += pb
    # het blaadje dat hij sjouwt (alleen in "draag"), boven het hoofd
    bones.append(bone("blaadje", "head", [0, 6, -2.4], [cube(atlas, [-2.6, 6.0, -5.2], [5.2, 0.3, 4.4], plain(BLAD, 10)),
                                                        cube(atlas, [-0.2, 4.6, -3.2], [0.4, 1.6, 0.4], plain((80, 140, 70), 4))]))
    return geo("mier", bones, atlas, 0.6, 0.6), pn


def mier_anims(pn):
    an = at.animaties(extra_idle={"spriet_l": {"rotation": kf((0, [-25, 0, -18]), (1.5, [-10, 0, -30]), (3.0, [-25, 0, -18]))},
                                  "spriet_r": {"rotation": kf((0, [-25, 0, 18]), (1.0, [-35, 0, 8]), (3.0, [-25, 0, 18]))}},
                      extra_walk=pootjes_walk(pn, 0.4))
    an["animations"]["walk"]["animation_length"] = 0.4
    verberg_in_standaard(an, "blaadje")
    w = json.loads(json.dumps(an["animations"]["walk"]["bones"]))
    w.pop("blaadje", None)
    w["head"] = {"rotation": kf((0, [-8, 0, 0]))}
    w["blaadje"] = {"rotation": kf((0, [0, 0, -4]), (0.2, [0, 0, 4]), (0.4, [0, 0, -4]))}
    extra(an, "draag", 0.4, w)
    return an


# =====================================================================================================================
# springspinnetje: rond donzig lijfje, een groot plat gezicht met GIGANTISCHE oogjes, acht korte pootjes; hupjes
# =====================================================================================================================
SS = (92, 86, 98)
SS_L = (238, 232, 226)
SS_O = (246, 160, 70)


def springspin(atlas):
    def wit_vlekje(W, H, rng, a):
        disc(a, W * 0.5, H * 0.5, W * 0.18, SS_L, 0.9)
    vacht = at.fluffy(SS, 12)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.6, 0], [cube(atlas, [-2.4, 1.6, 0.2], [4.8, 3.6, 4.6], vacht, {"up": at.met(vacht, wit_vlekje)})])]
    # de kop: groot, plat gezicht met de reuzenoogjes en twee kleine oogjes erboven, oranje 'wenkbrauwtjes'
    def wenkbrauw(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W / 2 + sx * W * 0.25, H * 0.16, W * 0.14, SS_O, 1.0, ry=H * 0.06)
            disc(a, W / 2 + sx * W * 0.1, H * 0.1, W * 0.05, (30, 26, 40), 1.0)
    kop = gezicht(SS, eye_y=0.5, eye_dx=0.24, eye_r=0.22, spots=wenkbrauw)
    bones.append(bone("head", "body", [0, 3.4, 0.2], [cube(atlas, [-2.8, 1.4, -3.4], [5.6, 5, 3.6], vacht, {"north": kop})]))
    pb, pn = pootjes(atlas, "body", [(-1.8, 2.2), (-0.6, 2.4), (0.8, 2.4), (2.4, 2.2)], 2.6, 2.6, 0.7, at.fluffy(SS, 10), spreid=45)
    bones += pb
    return geo("springspin", bones, atlas, 0.6, 0.5), pn


def springspin_anims(pn):
    an = at.animaties(extra_idle={"head": {"rotation": kf((0, [0, 0, 0]), (0.8, [0, 0, 0]), (1.0, [0, 25, 0]), (1.8, [0, 25, 0]),
                                                          (2.0, [0, -20, 0]), (2.8, [0, -20, 0]), (3.0, [0, 0, 0]))}})
    L = 0.5
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": {
        "body": {"position": kf((0, [0, 0, 0]), (L / 2, [0, 2.4, 0]), (L, [0, 0, 0]))}}}
    extra(an, "hop", L, an["animations"]["walk"]["bones"])
    return an


# =====================================================================================================================
# wandelende tak: lang dun takkenlijfje, piepklein kopje met gezichtje, lange dunne pootjes; wiegt heen en weer
# =====================================================================================================================
WT = (150, 132, 86)
WT_D = (110, 94, 58)


def wandelende_tak(atlas):
    def knoesten(W, H, rng, a):
        for x in range(0, W, 3 * S):
            a[:, x:x + S // 2, :3] = np.array(WT_D)
    stok = plain(WT, 8, knoesten)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4.4, 0], [cube(atlas, [-0.6, 3.8, -4], [1.2, 1.2, 13], stok)])]
    bones.append(bone("head", "body", [0, 4.6, -4], [cube(atlas, [-1.2, 3.6, -6.4], [2.4, 2.4, 2.4], plain(WT, 6),
                                                          {"north": gezicht(WT, eye_y=0.44, eye_dx=0.25, eye_r=0.19)}),
                                                     cube(atlas, [-0.9, 5.8, -9.4], [0.3, 0.3, 3.4], plain(WT_D, 4)),
                                                     cube(atlas, [0.6, 5.8, -9.4], [0.3, 0.3, 3.4], plain(WT_D, 4))]))
    bones.append(bone("staart", "body", [0, 4.4, 9], [cube(atlas, [-0.4, 4.0, 9], [0.8, 0.8, 3.4], stok)], rotation=[12, 0, 0]))
    pb, pn = pootjes(atlas, "body", [(-3.2, 0.6), (1.0, 0.6), (5.4, 0.6)], 4.4, 6.0, 0.45, plain(WT_D, 6), spreid=48)
    bones += pb
    return geo("wandelende_tak", bones, atlas, 0.9, 0.5), pn


def wandelende_tak_anims(pn):
    wieg = {"body": {"position": kf((0, [-0.6, 0, 0]), (1.5, [0.6, 0, 0]), (3.0, [-0.6, 0, 0])),
                     "rotation": kf((0, [0, 0, -4]), (1.5, [0, 0, 4]), (3.0, [0, 0, -4]))}}
    an = at.animaties(extra_idle=wieg, extra_walk=pootjes_walk(pn, 1.4, 16))
    an["animations"]["walk"]["animation_length"] = 1.4
    an["animations"]["walk"]["bones"]["body"] = {"position": kf((0, [-0.6, 0, 0]), (0.7, [0.6, 0, 0]), (1.4, [-0.6, 0, 0]))}
    extra(an, "wieg", 3.0, wieg)
    return an


# =====================================================================================================================
# mestkevertje: glimmend bol schildje, klein kopje met gezichtje en een hoorntje, zes pootjes; "rol": een balletje rollen
# =====================================================================================================================
MK = (64, 92, 150)
MK_L = (128, 176, 226)
MK_D = (38, 50, 86)
BAL = (126, 92, 58)


def mestkever(atlas):
    def glans(W, H, rng, a):
        disc(a, W * 0.3, H * 0.28, W * 0.14, MK_L, 0.85)
        a[:, W // 2 - S // 4:W // 2 + S // 4, :3] = np.array(MK_D)       # de naad tussen de dekschildjes
    schild = plain(MK, 8, glans)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.4, 0], [cube(atlas, [-2.4, 1.4, -2], [4.8, 3, 5], schild)])]
    bones.append(bone("head", "body", [0, 2.6, -2], [cube(atlas, [-1.8, 1.2, -4.6], [3.6, 2.8, 2.8], plain(MK_D, 6),
                                                          {"north": gezicht(MK_D, eye_y=0.46, eye_dx=0.25, eye_r=0.21)}),
                                                     cube(atlas, [-0.4, 3.8, -4.4], [0.8, 1.6, 0.8], plain(MK_D, 4))]))
    pb, pn = pootjes(atlas, "body", [(-1.2, 1.8), (0.4, 1.8), (2.0, 1.8)], 2.2, 2.6, 0.55, plain(MK_D, 4), spreid=40)
    bones += pb
    # het balletje (achter hem; alleen zichtbaar in "rol")
    bones.append(bone("bal", "root", [0, 3, 6], [cube(atlas, [-2.6, 0.4, 3.4], [5.2, 5.2, 5.2], plain(BAL, 14))]))
    return geo("mestkever", bones, atlas, 0.6, 0.45), pn


def mestkever_anims(pn):
    an = at.animaties(extra_walk=pootjes_walk(pn, 0.5))
    an["animations"]["walk"]["animation_length"] = 0.5
    verberg_in_standaard(an, "bal")
    L = 1.2
    rol = pootjes_walk(pn, L / 2, 26)
    rol.update({"body": {"rotation": kf((0, [-24, 0, 0]))},            # kop omlaag, achterpootjes tegen de bal
                "head": {"rotation": kf((0, [20, 0, 0]))},
                "bal": {"rotation": kf((0, [0, 0, 0]), (L, [-360, 0, 0]))}})
    extra(an, "rol", L, rol)
    return an


# =====================================================================================================================
# slakje: zacht lijf, een opgekruld huisje (spiraal), twee voelsprietjes met bolletjes; heel traag kruipen
# =====================================================================================================================
SL = (236, 206, 172)
SL_D = (196, 160, 124)
HUIS = (206, 142, 96)
HUIS_D = (150, 94, 62)


def slak(atlas):
    def spiraal(W, H, rng):
        a = plain(HUIS, 8)(W, H, rng).astype(np.float32)
        cx, cy = W / 2, H / 2
        for y in range(H):
            for x in range(W):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                r = np.hypot(dx, dy) / (W / 2)
                t = np.arctan2(dy, dx) / (2 * np.pi)
                if abs(((r * 3.2 - t) % 1.0) - 0.5) < 0.12 and r < 1.0:
                    a[y, x, :3] = HUIS_D
        return np.clip(a, 0, 255).astype(np.uint8)
    huid = plain(SL, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1.2, 0], [cube(atlas, [-1.6, 0, -4.4], [3.2, 2.2, 9.6], huid, {"down": plain(SL_D, 6)})])]
    bones.append(bone("huisje", "body", [0, 2.2, 1.6], [cube(atlas, [-1.8, 2.2, -1.2], [3.6, 5.6, 5.6], plain(HUIS, 8),
                                                              {"east": spiraal, "west": spiraal})]))
    bones.append(bone("head", "body", [0, 1.6, -4.2], [cube(atlas, [-1.7, 0.2, -6.4], [3.4, 3.2, 2.4], huid,
                                                            {"north": gezicht(SL, eye_y=0.46, eye_dx=0.25, eye_r=0.2)})]))
    for side, sx in (("l", 1), ("r", -1)):
        o = [0.6, 3.4, -5.4] if sx > 0 else [-1.0, 3.4, -5.4]
        bones.append(bone(f"spriet_{side}", "head", [sx * 0.8, 3.4, -5.2],
                          [cube(atlas, o, [0.4, 2.6, 0.4], huid), cube(atlas, [o[0] - 0.2, 6.0, o[2] - 0.2], [0.8, 0.8, 0.8], plain(SL_D, 4))],
                          rotation=[-12, 0, -sx * 14]))
    return geo("slak", bones, atlas, 0.6, 0.6)


def slak_anims():
    an = at.animaties(extra_idle={"spriet_l": {"rotation": kf((0, [-12, 0, -14]), (1.5, [6, 0, -24]), (3.0, [-12, 0, -14]))},
                                  "spriet_r": {"rotation": kf((0, [-12, 0, 14]), (2.0, [-24, 0, 6]), (3.0, [-12, 0, 14]))}})
    L = 2.4
    kruip = {"body": {"scale": kf((0, [1, 1, 1]), (L / 2, [1, 0.94, 1.1]), (L, [1, 1, 1]))},
             "huisje": {"rotation": kf((0, [0, 0, -3]), (L / 2, [0, 0, 3]), (L, [0, 0, -3]))}}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": kruip}
    extra(an, "kruip", L, kruip)
    extra(an, "schuil", 3.0, {"head": {"scale": kf((0, [1, 1, 1]), (0.6, [0.2, 0.2, 0.2]), (2.4, [0.2, 0.2, 0.2]), (3.0, [1, 1, 1]))}},
          loop=False)
    return an


# =====================================================================================================================
# vogelspinnetje: pluizig en lief, groot achterlijf, acht dikke harige pootjes; "zwaai" met een voorpootje
# =====================================================================================================================
VS = (120, 86, 64)
VS_L = (232, 176, 120)
VS_D = (74, 52, 40)


def vogelspin(atlas):
    vacht = at.fluffy(VS, 16)
    def kniebandjes(W, H, rng, a):
        a[:, int(W * 0.45):int(W * 0.55), :3] = np.array(VS_L)
    poot = at.met(at.fluffy(VS_D, 14), kniebandjes)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3.4, 0], [cube(atlas, [-2.6, 2.4, -2.4], [5.2, 3.2, 4.4], vacht)])]
    bones.append(bone("achterlijf", "body", [0, 4, 2], [cube(atlas, [-3.2, 2.6, 1.8], [6.4, 5, 6], vacht,
                                                             {"up": at.met(vacht, lambda W, H, rng, a: disc(a, W / 2, H / 2, W * 0.24, VS_L, 0.6))})]))
    bones.append(bone("head", "body", [0, 4, -2.4], [cube(atlas, [-2.4, 2.6, -4.8], [4.8, 3.8, 2.6], vacht,
                                                          {"north": gezicht(VS, eye_y=0.44, eye_dx=0.24, eye_r=0.2)})]))
    pb, pn = pootjes(atlas, "body", [(-2.0, 2.6), (-0.8, 2.6), (0.4, 2.6), (1.6, 2.6)], 3.6, 4.6, 1.1, poot, spreid=35)
    bones += pb
    return geo("vogelspin", bones, atlas, 0.8, 0.6), pn


def vogelspin_anims(pn):
    an = at.animaties(extra_walk=pootjes_walk(pn, 0.9, 18))
    an["animations"]["walk"]["animation_length"] = 0.9
    extra(an, "zwaai", 2.0, {
        "leg_l0": {"rotation": kf((0, [0, 0, -35]), (0.3, [0, -30, -95]), (0.6, [0, 10, -95]), (0.9, [0, -30, -95]),
                                  (1.2, [0, 10, -95]), (1.6, [0, 0, -35]), (2.0, [0, 0, -35]))},
        "body": {"rotation": kf((0, [0, 0, 0]), (0.3, [-6, 0, 4]), (1.6, [-6, 0, 4]), (2.0, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (0.6, [0, 0, 8]), (1.2, [0, 0, -8]), (2.0, [0, 0, 0]))}}, loop=False)
    return an


# =====================================================================================================================
# hommel: kogelrond en pluizig met geel-zwarte banden, kopje met gezichtje, doorzichtige vleugeltjes; "vlieg" (zoemen)
# =====================================================================================================================
HM_G = (250, 206, 70)
HM_Z = (52, 44, 48)
HM_W = (246, 240, 232)
VLEUGEL = (226, 236, 250)


def hommel(atlas):
    def banden(W, H, rng):
        a = at.fluffy(HM_G, 14)(W, H, rng).astype(np.float32)
        for x in range(W):
            band = int(x / W * 4)
            if band in (1, 3):
                a[:, x, :3] = np.array(HM_Z) + rng.normal(0, 6, (H, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [cube(atlas, [-3, 2.6, -2.6], [6, 5.4, 7], banden,
                                                   {"north": at.fluffy(HM_G, 14), "south": at.fluffy(HM_W, 10)})])]
    bones.append(bone("head", "body", [0, 5, -2.6], [cube(atlas, [-2.4, 2.8, -5.6], [4.8, 4.2, 3], at.fluffy(HM_Z, 10),
                                                          {"north": gezicht(HM_Z, eye_y=0.46, eye_dx=0.25, eye_r=0.2)})]))
    for side, sx in (("l", 1), ("r", -1)):
        o = [0.4, 7.0, -5.0] if sx > 0 else [-0.8, 7.0, -5.0]
        bones.append(bone(f"spriet_{side}", "head", [sx * 0.6, 7, -4.6], [cube(atlas, o, [0.4, 1.8, 0.4], plain(HM_Z, 4))],
                          rotation=[-25, 0, -sx * 20]))
    vleugel = plain(VLEUGEL, 4, lambda W, H, rng, a: a.__setitem__((slice(None), slice(None), 3), 170))
    for side, sx in (("left", 1), ("right", -1)):
        o = [1.0, 8.0, -1.6] if sx > 0 else [-1.0 - 4.4, 8.0, -1.6]
        bones.append(bone(f"wing_{side}", "body", [sx * 1.0, 8.0, -0.6], [cube(atlas, o, [4.4, 0.2, 3.2], vleugel)]))
    for i, z in enumerate((-1.2, 1.4)):
        for side, sx in (("l", 1), ("r", -1)):
            o = [1.4, 0.4, z] if sx > 0 else [-2.2, 0.4, z]
            bones.append(bone(f"leg_{side}{i}", "body", [sx * 1.8, 2.8, z + 0.4], [cube(atlas, o, [0.8, 2.4, 0.8], plain(HM_Z, 4))]))
    return geo("hommel", bones, atlas, 0.7, 0.7)


def hommel_anims():
    def zoem(L, n, a):
        ks = [(L * i / n, [0, 0, (-a if i % 2 else a)]) for i in range(n + 1)]
        return {"wing_left": {"rotation": kf(*ks)},
                "wing_right": {"rotation": kf(*[(t, [0, 0, -v[2]]) for t, v in ks])}}
    idle = zoem(3.0, 30, 12)
    idle["root"] = {"position": kf((0, [0, 3, 0]), (1.5, [0, 4, 0]), (3.0, [0, 3, 0]))}     # hangt zoemend in de lucht
    an = at.animaties(extra_idle=idle)
    L = 0.8
    vlieg = zoem(L, 16, 30)
    vlieg["root"] = {"position": kf((0, [0, 4, 0]), (L / 2, [0, 5, 0]), (L, [0, 4, 0]))}
    vlieg["body"] = {"rotation": kf((0, [8, 0, 0]))}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": vlieg}
    extra(an, "vlieg", L, vlieg)
    return an


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "mier":
        a = Atlas(1210); g, pn = mier(a); maak_soort(d, soort, g, a, mier_anims(pn), (0.6, 0.8))
    elif soort == "springspin":
        a = Atlas(1220); g, pn = springspin(a); maak_soort(d, soort, g, a, springspin_anims(pn), (0.6, 0.85))
    elif soort == "wandelende_tak":
        a = Atlas(1230); g, pn = wandelende_tak(a); maak_soort(d, soort, g, a, wandelende_tak_anims(pn), (0.6, 0.8))
    elif soort == "mestkever":
        a = Atlas(1240); g, pn = mestkever(a); maak_soort(d, soort, g, a, mestkever_anims(pn), (0.6, 0.85))
    elif soort == "slak":
        a = Atlas(1250); g = slak(a); maak_soort(d, soort, g, a, slak_anims(), (0.6, 0.85))
    elif soort == "vogelspin":
        a = Atlas(1260); g, pn = vogelspin(a); maak_soort(d, soort, g, a, vogelspin_anims(pn), (0.6, 0.85))
    elif soort == "hommel":
        a = Atlas(1270); g = hommel(a); maak_soort(d, soort, g, a, hommel_anims(), (0.6, 0.85))
    else:
        raise SystemExit(f"onbekende soort {soort}")


def vellen(soorten, overzicht=True):
    vellen_gedeeld(soorten, SOORTEN, NAMEN, 12, "Kriebelhuis, Kriebelzaal", UIT, overzicht)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    alles(sys.modules[__name__], args or SOORTEN, met_vellen="--geen-vellen" not in sys.argv)
