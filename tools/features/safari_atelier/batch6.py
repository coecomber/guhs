"""
Safari-voorwerk, batch 6 (Europees Woud & Beekdal): vuursalamander, egel, otter, das, edelhert.
Zelfde stijl, werkwijze en bestandsformaat als batch 1 (dieren.py); hergebruikt atelier/dieren/blad zonder ze aan te passen.

    python safari-ontwerp/dieren/_bron/batch6.py [soort ...] [overzicht]     (geen argumenten: alle vijf + vellen + overzicht)

Per soort in safari-ontwerp/dieren/<soort>/: <soort>.geo.json/.png (vrouwtje, of beide), <soort>_man.* (alleen edelhert: gewei),
<soort>_baby.* (jong; het edelhertkalf heeft eigen witte stipjes), <soort>.animation.json (idle, walk, blij, eet; egel ook
opgerold, otter ook drijf), <soort>.bbmodel en <soort>_voorstel.png. Plus dieren/batch6_overzicht.png naast een guh.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht, neusje, vorm_oor, transparent  # noqa: E402
from dieren import baby, paar, vier_poten, ZIJ_LEEG, POTEN4, NEUS      # noqa: E402
from features.landdiertjes_modellen import stekels                     # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
SOORTEN = ["vuursalamander", "egel", "otter", "das", "edelhert"]
NAMEN = {"vuursalamander": "Vuursalamander", "egel": "Egel", "otter": "Otter", "das": "Das", "edelhert": "Edelhert"}


def vlekjes(kleur, n_per=60, rmin=0.08, rmax=0.16):
    """Losse ronde vlekken (vuursalamander: gele vlekken op zwart; kalf: witte stipjes)."""
    def f(W, H, rng, a):
        for _ in range(max(2, W * H // (n_per * at.S * at.S // 4))):
            r = min(W, H) * rng.uniform(rmin, rmax) + 1.5
            disc(a, rng.uniform(0, W), rng.uniform(0, H), r, kleur, 1.0, ry=r * rng.uniform(0.7, 1.2))
    return f


# =====================================================================================================================
# vuursalamander
# =====================================================================================================================
VS = (32, 30, 36)
VS_GEEL = (255, 204, 40)
VS_BUIK = (60, 54, 60)


def vuursalamander(atlas):
    zwart = plain(VS, 8, vlekjes(VS_GEEL, 30, 0.12, 0.22))
    effen = plain(VS, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.5, 0], [cube(atlas, [-3, 1.2, -5], [6, 3.6, 10], zwart, {"down": plain(VS_BUIK, 8)})])]
    poot = plain(VS, 6, vlekjes(VS_GEEL, 12, 0.2, 0.3))
    for voor, z in (("front", -4.5), ("back", 3)):
        for side, sx in (("left", 1), ("right", -1)):
            o = [3, 0, z] if sx > 0 else [-4.6, 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + 0.8, 1.8, o[2] + 0.8],
                              [cube(atlas, o, [1.6, 1.8, 1.6], poot)], rotation=[0, 0, -12 * sx]))
    gele_wangen = lambda W, H, rng, a: [disc(a, W * (0.5 + s * 0.42), H * 0.18, W * 0.1, VS_GEEL, 1.0, ry=H * 0.12) for s in (-1, 1)]  # noqa: E731
    bones.append(bone("head", "body", [0, 3, -5], [
        cube(atlas, [-3.5, 1, -10.5], [7, 5, 5.5], zwart, {"north": gezicht(VS, eye_y=0.42, eye_r=0.18, spots=gele_wangen),
                                                          "up": plain(VS, 8, vlekjes(VS_GEEL, 20, 0.18, 0.28))})]))
    bones.append(bone("tail", "body", [0, 3, 5], [cube(atlas, [-1.6, 1.3, 5], [3.2, 2.6, 6], zwart)]))
    bones.append(bone("tail_tip", "tail", [0, 2.6, 11], [cube(atlas, [-1, 1.5, 11], [2, 1.8, 5], zwart, {"south": effen})]))
    return geo("vuursalamander", bones, atlas, 0.8, 0.5)


def vuursalamander_anims():
    kronkel = {"tail_tip": {"rotation": at.kf((0, [0, 0, 0]), (1.0, [0, 15, 0]), (2.0, [0, -15, 0]), (3.0, [0, 0, 0]))}}
    walk = {"body": {"rotation": at.kf((0, [0, 8, 0]), (0.4, [0, -8, 0]), (0.8, [0, 8, 0]))},
            "tail_tip": {"rotation": at.kf((0, [0, -14, 0]), (0.4, [0, 14, 0]), (0.8, [0, -14, 0]))}}
    return at.animaties(legs4=POTEN4, staart="tail", extra_idle=kronkel, extra_walk=walk)


# =====================================================================================================================
# egel (echte bruine egel, guh-ified; NIET het roze pluisegeltje)
# =====================================================================================================================
EG_STEKEL = (128, 92, 62)
EG_TIP = (238, 222, 196)
EG_DONKER = (70, 50, 36)
EG_GEZICHT = (226, 196, 158)
EG_BUIK = (206, 172, 134)


def egel(atlas):
    rug = stekels(EG_STEKEL, EG_TIP, EG_DONKER, rij=7.0)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [
                 cube(atlas, [-6, 1.5, -5.5], [12, 9, 12], rug, {"down": plain(EG_BUIK, 8), "north": plain(EG_BUIK, 8)}),
                 cube(atlas, [-5, 10.5, -4.5], [10, 1.5, 10], rug, {"down": transparent})])]       # bolle ruglijn
    bones += vier_poten(atlas, "body", 2, -4.5, 3.5, [2.5, 1.8, 2.5], plain(EG_DONKER, 6), 1.8)
    bones.append(bone("head", "body", [0, 5.5, -5.5], [
        cube(atlas, [-4.5, 1.8, -11], [9, 7.5, 5.5], plain(EG_GEZICHT, 8),
             {"north": gezicht(EG_GEZICHT, eye_y=0.4, eye_r=0.17), "up": rug}),
        cube(atlas, [-1.6, 2.2, -13], [3.2, 2.6, 2], plain(EG_GEZICHT, 6), {"north": neusje(EG_GEZICHT, NEUS, mond=False)})]))
    oor = vorm_oor(EG_GEZICHT, (214, 150, 140))
    bones += paar(atlas, "ear", "head", [4, 8.8, -8], [3.4, 8.2, -8.5], [1.8, 1.8, 0.8], plain(EG_GEZICHT, 6),
                  dict(ZIJ_LEEG, north=oor, south=oor))
    return geo("egel", bones, atlas, 1.0, 0.9)


def egel_anims():
    a = at.animaties(legs4=POTEN4, oren=True, waggel=True)
    # opgerold: kop en pootjes naar binnen, lijf een bolletje (blijft zo staan zolang de animatie loopt)
    op = {"head": {"rotation": at.kf((0, [55, 0, 0]), (2.0, [55, 0, 0])), "position": at.kf((0, [0, -2, 3.5]), (2.0, [0, -2, 3.5]))},
          "body": {"rotation": at.kf((0, [-12, 0, 0]), (1.0, [-12, 0, 2]), (2.0, [-12, 0, 0])),
                   "scale": at.kf((0, [1.05, 0.95, 1.0]), (1.0, [1.06, 0.97, 1.0]), (2.0, [1.05, 0.95, 1.0]))}}
    for p in POTEN4:
        op[p] = {"scale": at.kf((0, [0.01, 0.01, 0.01]), (2.0, [0.01, 0.01, 0.01]))}
    for e in ("ear_left", "ear_right"):
        op[e] = {"scale": at.kf((0, [0.01, 0.01, 0.01]), (2.0, [0.01, 0.01, 0.01]))}
    a["animations"]["opgerold"] = {"loop": True, "animation_length": 2.0, "bones": op}
    return a


# =====================================================================================================================
# otter
# =====================================================================================================================
OT = (124, 86, 58)
OT_L = (226, 196, 160)
OT_D = (84, 58, 40)


def otter(atlas):
    vacht = at.fluffy(OT, 10)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [cube(atlas, [-3.5, 2, -7], [7, 6, 15], vacht,
                                                   {"north": at.buik(OT, OT_L, cy=0.6), "down": plain(OT_L, 8)})])]
    bones += vier_poten(atlas, "body", 1.2, -6, 5, [2.3, 2.5, 2.3], plain(OT_D, 6), 2.5)
    lichte_kin = lambda W, H, rng, a: disc(a, W * 0.5, H * 0.98, W * 0.42, OT_L, 1.0, ry=H * 0.3)  # noqa: E731
    bones.append(bone("head", "body", [0, 6.5, -7], [
        cube(atlas, [-4, 3.5, -13], [8, 7, 6.5], vacht, {"north": gezicht(OT, eye_y=0.38, eye_r=0.16, spots=lichte_kin)}),
        cube(atlas, [-2.5, 3.8, -14.6], [5, 3, 1.6], plain(OT_L, 6), {"north": neusje(OT_L, NEUS)})]))
    oor = vorm_oor(OT, OT_D)
    bones += paar(atlas, "ear", "head", [4, 10, -9.5], [3.3, 9.3, -10], [1.6, 1.6, 0.8], plain(OT, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 4, 8], [cube(atlas, [-2, 2.5, 8], [4, 3, 8], vacht)], rotation=[8, 0, 0]))
    bones.append(bone("tail_tip", "tail", [0, 3.5, 16], [cube(atlas, [-1.2, 2.8, 16], [2.4, 2, 5], plain(OT_D, 8))]))
    return geo("otter", bones, atlas, 1.2, 0.8)


def otter_anims():
    a = at.animaties(legs4=POTEN4, oren=True, staart="tail")
    # drijven: op de rug, met een steentje-poot op de buik (schommelt zachtjes)
    a["animations"]["drijf"] = {"loop": True, "animation_length": 3.0, "bones": {
        "root": {"rotation": at.kf((0, [0, 0, 180]), (1.5, [0, 0, 186]), (3.0, [0, 0, 180])),
                 "position": at.kf((0, [0, 8, 0]), (1.5, [0, 8.4, 0]), (3.0, [0, 8, 0]))},
        "leg_front_left": {"rotation": at.kf((0, [-70, 0, 20]), (3.0, [-70, 0, 20]))},
        "leg_front_right": {"rotation": at.kf((0, [-70, 0, -20]), (3.0, [-70, 0, -20]))}}}
    return a


# =====================================================================================================================
# das
# =====================================================================================================================
DS = (150, 148, 152)
DS_D = (40, 38, 44)
DS_W = (246, 244, 240)


def das(atlas):
    grijs = at.fluffy(DS, 12)
    zwart = plain(DS_D, 6)

    def strepen(W, H, rng, a):
        for sx in (-1, 1):                                   # twee zwarte strepen van de neus over de ogen naar achter
            x0 = W / 2 + sx * W * 0.26
            for y in range(H):
                for x in range(W):
                    if abs(x + 0.5 - x0) < W * 0.13:
                        a[y, x, :3] = DS_D
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [cube(atlas, [-5.5, 2.5, -7], [11, 7.5, 14], grijs, {"down": zwart})])]
    bones += vier_poten(atlas, "body", 2.2, -6, 4, [3, 3, 3], zwart, 3)
    kop_boven = plain(DS_W, 6, lambda W, H, rng, a: strepen(W, H, rng, a))
    bones.append(bone("head", "body", [0, 6, -7], [
        cube(atlas, [-4.5, 2.8, -13], [9, 7, 6.5], plain(DS_W, 6),
             {"north": gezicht(DS_W, eye_y=0.44, eye_r=0.16, spots=strepen), "up": kop_boven,
              "east": plain(DS_D, 6), "west": plain(DS_D, 6)}),
        cube(atlas, [-1.8, 3, -14.8], [3.6, 3, 1.8], plain(DS_W, 6), {"north": neusje(DS_W, NEUS, mond=False)})]))
    oor = vorm_oor(DS_D, DS_D, rand=DS_W)
    bones += paar(atlas, "ear", "head", [4.2, 10, -8.5], [3.4, 9.2, -9], [1.8, 1.6, 0.8], zwart, dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 8, 7], [cube(atlas, [-1.5, 6, 7], [3, 2.5, 3], grijs)], rotation=[-15, 0, 0]))
    return geo("das", bones, atlas, 1.2, 0.9)


def das_anims():
    snuffel = {"head": {"rotation": at.kf((0, [10, 0, 0]), (0.5, [14, 6, 0]), (1.0, [10, -6, 0]), (1.5, [14, 0, 0]),
                                          (2.0, [0, 0, 0]), (3.0, [0, 0, 0]))}}
    return at.animaties(legs4=POTEN4, oren=True, staart="tail", extra_idle=snuffel)


# =====================================================================================================================
# edelhert (+ kalf met witte stipjes)
# =====================================================================================================================
EH = (168, 102, 62)
EH_L = (240, 214, 176)
EH_D = (96, 60, 38)
GEWEI = (226, 206, 168)
HOEF = (60, 44, 34)


def edelhert(atlas, man=False, kalf=False):
    stip = vlekjes((252, 242, 224), 40, 0.05, 0.09) if kalf else None
    vacht = plain(EH, 8, stip)
    licht = plain(EH_L, 6)
    spiegel = plain(EH, 8, lambda W, H, rng, a: disc(a, W / 2, H * 0.45, W * 0.38, EH_L, 1.0, ry=H * 0.4))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 15, 0], [cube(atlas, [-4.5, 13, -8], [9, 8, 16], vacht, {"down": licht, "south": spiegel})])]
    bones += vier_poten(atlas, "body", 1.4, -7, 5, [2.4, 13, 2.4], plain(EH_D, 6), 13, voet=(1.4, plain(HOEF, 4)))
    bones.append(bone("hals", "body", [0, 19, -6.5], [cube(atlas, [-2.5, 18, -9.5], [5, 9, 4], vacht,
                                                           {"north": plain(EH_L if kalf else EH_D, 6)})], rotation=[12, 0, 0]))
    bones.append(bone("head", "hals", [0, 26, -8], [
        cube(atlas, [-3.75, 25, -14], [7.5, 6.5, 7], plain(EH, 8), {"north": gezicht(EH, eye_y=0.36, eye_r=0.16)}),
        cube(atlas, [-2, 25.2, -16.2], [4, 2.6, 2.2], licht, {"north": neusje(EH_L, NEUS)})]))
    oor = vorm_oor(EH, (240, 176, 180), punt=0.5)
    bones += paar(atlas, "ear", "head", [3.6, 30, -10], [3.6, 29, -11], [3.5, 1, 2.6], plain(EH, 6), {"up": oor}, rotation=[0, 0, -20])
    bones.append(bone("tail", "body", [0, 20.5, 8], [cube(atlas, [-1, 18, 8], [2, 3, 1.2], licht)]))
    if man:
        g = plain(GEWEI, 6)
        for side, sx in (("left", 1), ("right", -1)):
            def xo(x, w):
                return x if sx > 0 else -x - w
            bones.append(bone(f"gewei_{side}", "head", [sx * 2.2, 31.5, -10], [
                cube(atlas, [xo(1.6, 1.2), 31.5, -10.6], [1.2, 6, 1.2], g),                # hoofdstang
                cube(atlas, [xo(1.6, 1.2), 33, -12.8], [1.2, 1, 2.4], g),                  # oogspruit naar voren
                cube(atlas, [xo(1.6, 1.2), 35.5, -12.2], [1.2, 1, 2], g),
                cube(atlas, [xo(2.4, 2.4), 37.2, -10.6], [2.4, 1, 1.2], g),                # kroontje
                cube(atlas, [xo(1.6, 1.2), 37.2, -11.8], [1.2, 2, 1.2], g)], rotation=[-10, 0, 22 * sx]))
    naam = "edelhert_man" if man else ("edelhert_kalf" if kalf else "edelhert")
    return geo(naam, bones, atlas, 1.4, 2.6)


def edelhert_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail",
                        extra_idle={"hals": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [3, 0, 2]), (3.0, [0, 0, 0]))}})


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "vuursalamander":
        a = Atlas(610); g = vuursalamander(a); anims = vuursalamander_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.82), a)
    elif soort == "egel":
        a = Atlas(620); g = egel(a); anims = egel_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "otter":
        a = Atlas(630); g = otter(a); anims = otter_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "das":
        a = Atlas(640); g = das(a); anims = das_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "edelhert":
        a = Atlas(650, 128); g = edelhert(a); anims = edelhert_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(651, 128); at.schrijf(d, soort + "_man", edelhert(am, man=True), am)
        ak = Atlas(652, 128); kalf = baby(edelhert(ak, kalf=True), 0.56, 0.8)
        kalf["minecraft:geometry"][0]["description"]["identifier"] = "geometry.edelhert_baby"
        at.schrijf(d, soort + "_baby", kalf, ak)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


# =====================================================================================================================
# vellen (de vel/overzicht-functies van blad.py, met de batch-6-namen en -titels)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    import blad as bl
    from PIL import Image, ImageDraw
    bl.NAMEN.update(NAMEN)

    def vel(soort):
        varianten = [("", "volwassen" + (" (vrouwtje)" if os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json")) else ""))]
        if os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json")):
            varianten.append(("man", "mannetje"))
        varianten.append(("baby", "jong"))
        guh = bl.guh_quads()
        maxh, maxw = bl.hoogte(guh)
        qs = {}
        for v, _ in varianten:
            qs[v] = bl.dier_quads(soort, v)
            h, w = bl.hoogte(qs[v])
            maxh, maxw = max(maxh, h), max(maxw, w)
        CW, CH, GW = 360, 380, 560
        ppb = min(170, (CH - 40) / maxh, (CW - 30) / (maxw * 1.05))
        W, H = 200 + CW * 3 + GW, 90 + CH * len(varianten) + 40
        img = Image.new("RGBA", (W, H), bl.BG)
        d = ImageDraw.Draw(img)
        d.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 6: Europees Woud)", font=bl.font(30, True), fill=(40, 30, 40))
        d.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Animaties volgen in de game.",
               font=bl.font(16), fill=(80, 70, 80))
        for c, (lab, _, _) in enumerate(bl.VIEWS + [("naast een guh", 0, 0)]):
            d.text((200 + c * CW + 10, 88), lab, font=bl.font(16, True), fill=(90, 80, 90))
        combs, ppb_g = {}, ppb * 0.8
        for v, _ in varianten:
            dier = bl.dier_quads(soort, v)
            combs[v] = bl.naast(dier, bl.guh_quads(), dx=-(bl.xbreed(dier) / 2 + bl.xbreed(guh) / 2 + 0.3))
            sx, sy = bl.span(combs[v], 35, -18)
            ppb_g = min(ppb_g, (GW - 30) / sx, (CH - 50) / sy)
        for r, (v, lab) in enumerate(varianten):
            y = 110 + r * CH
            d.text((20, y + CH / 2 - 12), lab, font=bl.font(20, True), fill=(50, 40, 50))
            for c, (_, yaw, pitch) in enumerate(bl.VIEWS):
                img.alpha_composite(bl.render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20), (200 + c * CW, y))
            img.alpha_composite(bl.render_vast(combs[v], 35, -18, ppb_g, GW, CH - 20), (200 + 3 * CW, y))
        pad = os.path.join(UIT, soort, f"{soort}_voorstel.png")
        img.convert("RGB").save(pad)
        print(pad)

    for s in soorten:
        vel(s)
    if not overzicht:
        return
    import wiki_renders as wr
    items = [("guh", bl.guh_quads(), None)] + [(s, bl.dier_quads(s), bl.dier_quads(s, "baby")) for s in SOORTEN]
    quads, x = [], 0.0
    for naam, q, qb in items:
        h, w = bl.hoogte(q)
        quads += wr.offset_quads(q, dx=(x + w / 2))
        if qb is not None:
            quads += wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.6))
        x += w + 0.7
    ppb = 120
    W, H = int(x * ppb * 0.95) + 220, 640
    t = bl.render_vast(quads, 18, -16, ppb, W, H - 100)
    img = Image.new("RGBA", (W, H), bl.BG)
    img.alpha_composite(t, (0, 90))
    d = ImageDraw.Draw(img)
    d.text((20, 18), "Safari-dieren batch 6 (Europees Woud): naast een guh (volwassen achter, jong ervoor)", font=bl.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), "Vuursalamander · Egel · Otter · Das · Edelhert (van links naar rechts, na de guh; het edelhert-mannetje staat op zijn eigen vel)",
           font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch6_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    args = sys.argv[1:]
    soorten = [a for a in args if a != "overzicht"] or SOORTEN
    for s in soorten:
        maak(s)
    vellen(soorten, overzicht=(not args or "overzicht" in args))
