"""
bbq2 (ring-h3) - pictures of the mine, for whoever builds it (not part of the build; needs tools/ on the path).

  snede(b, ...)        isometric cut-aways: the rock on the cave floor, the middle level, the deep level
  Kijker(b)            a little ray caster that stands INSIDE the mine: block light as the game spreads it (so a dark hall
                       is dark), a camera with a position and a point to look at, and optionally the Barbecuerog's model,
                       guh-sized markers for characters and the scene's own extra lights. This is how the halls and the frames
                       of the bridge scene are looked at without a game client.
  alles(b, out)        every picture (ring_h3_bouw.preview)
"""
import json
import math
import os
import tempfile

import numpy as np
from PIL import Image

from features import ring_h3_bouw as B

LICHT = {B.LANTAARN: 15, B.ZIELLAMP: 10, B.GLOEIKOOL: 15, B.UIENLICHT: 15, B.ROOKGAT: 5, B.SMEUL: 7, B.VUUR: 12, B.RUNE: 6}
DUN = (B.KETTING, B.HEK, B.TRALIES, B.MUUR, B.SMEUL, B.HENDEL, "minecraft:spruce_wall_sign", "minecraft:spruce_sign")
KLEUR = {B.VUUR: (255, 150, 50), B.LANTAARN: (255, 214, 120), B.ZIELLAMP: (120, 230, 240), B.RUNE: (150, 200, 255), B.BROKKEL: (150, 132, 124),
         B.HENDEL: (190, 150, 60), B.GLOEIKOOL: (255, 140, 40), B.ROOKGAT: (120, 70, 50), B.TRALIES: (80, 80, 90), B.KETTING: (70, 74, 90),
         B.HEK: (57, 47, 47), "minecraft:spruce_wall_sign": (126, 94, 56), "minecraft:spruce_sign": (126, 94, 56), "minecraft:barrel": (130, 96, 56)}


def kleur(name):
    import wiki_renders as wr
    if name in KLEUR:
        return KLEUR[name]
    c = wr.block_colour(name)
    return c if c else (200, 0, 200)


# =====================================================================================================================
# cut-aways
# =====================================================================================================================
def snede(b, y_van, y_tot, px=9, doos=None, draai=0):
    """An isometric picture of the blocks with y_van <= y <= y_tot (air left out): what you see when the rock above is lifted off."""
    import make_structures as ms
    import wiki_renders as wr
    for name, c in KLEUR.items():
        wr.SPECIAL_COLOURS[name] = c
    wr.block_colour.cache_clear()
    x0, z0, x1, z1 = doos or (0, 0, B.MAAT[0] - 1, B.MAAT[2] - 1)
    s = ms.Structure((x1 - x0 + 1, y_tot - y_van + 1, z1 - z0 + 1))
    for (x, y, z), v in b.s.blocks.items():
        if y_van <= y <= y_tot and x0 <= x <= x1 and z0 <= z <= z1 and v[0] != B.AIR:
            s.blocks[(x - x0, y - y_van, z - z0)] = v
    if draai:
        s = wr.turned(s, draai)
    return wr.render_structure(s, {}, px=px, max_size=2000)


# =====================================================================================================================
# the ray caster
# =====================================================================================================================
class Kijker:
    def __init__(self, b):
        X, Y, Z = B.MAAT
        self.maat = (X, Y, Z)
        self.vol = np.zeros((X, Y, Z), bool)
        self.rgb = np.zeros((X, Y, Z, 3), np.float32)
        self.gloeit = np.zeros((X, Y, Z), bool)
        bron = np.zeros((X, Y, Z), np.int8)
        self.dicht = np.ones((X, Y, Z), bool)        # what light does not pass (everything that isn't set counts as rock)
        for (x, y, z), (name, props, _) in b.s.blocks.items():
            if name == B.AIR:
                self.dicht[x, y, z] = False
                continue
            if name in LICHT:
                bron[x, y, z] = LICHT[name]
            if name in DUN or name in (B.LANTAARN, B.ZIELLAMP):
                self.dicht[x, y, z] = False
                continue
            self.vol[x, y, z] = True
            self.rgb[x, y, z] = kleur(name)
            self.gloeit[x, y, z] = name in (B.LANTAARN, B.ZIELLAMP, B.GLOEIKOOL, B.UIENLICHT, B.VUUR) or (name == B.RUNE and props.get("teken") == str(B.NJEG))
        # rock that the template doesn't set
        leeg = np.ones((X, Y, Z), bool)
        for (x, y, z) in b.s.blocks:
            leeg[x, y, z] = False
        leeg[:, B.G + 1:, :] = False                 # (above the cave floor the template leaves the open cave alone)
        self.vol |= leeg
        self.rgb[leeg] = kleur(B.HOUTSKOOL)
        self.dicht[:, B.G + 1:, :] &= self.vol[:, B.G + 1:, :]
        self.bron = bron
        self.licht = self._verspreid(bron)

    def _verspreid(self, bron):
        """Block light as the game does it: 15 at a source, one less per step through anything that isn't solid."""
        licht = bron.astype(np.int8).copy()
        for _ in range(15):
            buur = licht.copy()
            for as_ in range(3):
                for stap in (1, -1):
                    r = np.roll(licht, stap, as_)
                    # (np.roll wraps: the outer layers are rock, so nothing leaks round)
                    buur = np.maximum(buur, r - 1)
            buur[self.dicht & (bron == 0)] = 0
            licht = np.maximum(licht, buur)
        return licht

    def met_licht(self, lampen):
        """A copy of the light with extra sources (x, y, z, level): the fire the Barbecuerog carries with him."""
        bron = self.bron.copy()
        for x, y, z, n in lampen:
            if 0 <= x < self.maat[0] and 0 <= y < self.maat[1] and 0 <= z < self.maat[2]:
                bron[int(x), int(y), int(z)] = n
        return self._verspreid(bron)

    def teken(self, oog, kijk, breed=480, hoog=270, fov=70.0, licht=None, ver=110.0, omgeving=0.1, werk=False):
        """(image as float array h x w x 3, depth h x w) seen from `oog` looking at `kijk`."""
        licht = self.licht if licht is None else licht
        oog, kijk = np.array(oog, np.float32), np.array(kijk, np.float32)
        f = kijk - oog
        f /= np.linalg.norm(f)
        r = np.cross(f, np.array([0, 1, 0], np.float32))
        r /= max(1e-6, np.linalg.norm(r))
        u = np.cross(r, f)
        tan = math.tan(math.radians(fov) / 2)
        xs = (np.arange(breed) + 0.5) / breed * 2 - 1
        ys = 1 - (np.arange(hoog) + 0.5) / hoog * 2
        gx, gy = np.meshgrid(xs * tan * breed / hoog, ys * tan)
        d = f[None, None, :] + gx[..., None] * r[None, None, :] + gy[..., None] * u[None, None, :]
        d /= np.linalg.norm(d, axis=-1, keepdims=True)
        d = d.reshape(-1, 3)
        n = d.shape[0]
        beeld = np.zeros((n, 3), np.float32)
        diepte = np.full(n, ver, np.float32)
        bezig = np.arange(n)
        X, Y, Z = self.maat
        stap = 0.2
        vorige = np.floor(np.repeat(oog[None, :], n, 0)).astype(np.int32)
        t = 0.0
        while t < ver and len(bezig):
            t += stap
            p = oog[None, :] + d[bezig] * t
            c = np.floor(p).astype(np.int32)
            binnen = (c[:, 0] >= 0) & (c[:, 0] < X) & (c[:, 1] >= 0) & (c[:, 1] < Y) & (c[:, 2] >= 0) & (c[:, 2] < Z)
            cc = np.clip(c, 0, [X - 1, Y - 1, Z - 1])
            raak = binnen & self.vol[cc[:, 0], cc[:, 1], cc[:, 2]]
            buiten_raak = ~binnen
            if raak.any():
                idx = bezig[raak]
                cel = cc[raak]
                vc = np.clip(vorige[idx], 0, [X - 1, Y - 1, Z - 1])
                verschil = cel - vorige[idx]
                # the face that was hit: the axis along which the cell changed
                vlak = np.where(verschil[:, 1] != 0, np.where(verschil[:, 1] < 0, 1.0, 0.5), np.where(verschil[:, 0] != 0, 0.8, 0.66))
                l = licht[vc[:, 0], vc[:, 1], vc[:, 2]].astype(np.float32) / 15.0
                # the game's own curve (LightTexture): level -> brightness, the dimension's ambient light, the default gamma
                helder = l / (4 - 3 * l)
                helder = omgeving + (1 - omgeving) * helder
                helder = 0.5 * helder + 0.5 * (1 - (1 - helder) ** 4)
                if werk:
                    helder = (0.55 + 0.45 * helder) * 2.2      # a builder's lamp: everything can be seen, light or not
                kleur_ = self.rgb[cel[:, 0], cel[:, 1], cel[:, 2]]
                gloei = self.gloeit[cel[:, 0], cel[:, 1], cel[:, 2]]
                warm = np.stack([np.ones_like(l), 0.86 + 0.14 * (1 - l), 0.70 + 0.3 * (1 - l)], -1)
                uit = kleur_ * (helder * vlak)[:, None] * warm
                uit = np.where(gloei[:, None], kleur_ * 1.0, uit)
                mist = np.clip(1 - t / ver, 0, 1) ** 0.6
                beeld[idx] = uit * mist
                diepte[idx] = t
            vorige[bezig] = c
            bezig = bezig[~(raak | buiten_raak)]
        return beeld.reshape(hoog, breed, 3), diepte.reshape(hoog, breed), (oog, f, r, u, tan, breed, hoog)

    @staticmethod
    def punten(beeld, diepte, cam, pts, rgb, straal=0.0):
        """Draws 3D points (n x 3) with colours (n x 3) into the picture, depth-tested (nearest wins)."""
        oog, f, r, u, tan, breed, hoog = cam
        v = pts - oog[None, :]
        zf = v @ f
        ok = zf > 0.2
        xs = (v @ r) / np.maximum(zf, 1e-3) / (tan * breed / hoog)
        ys = (v @ u) / np.maximum(zf, 1e-3) / tan
        px = ((xs + 1) / 2 * breed).astype(np.int32)
        py = ((1 - ys) / 2 * hoog).astype(np.int32)
        afstand = np.linalg.norm(v, axis=1)
        ok &= (px >= 0) & (px < breed) & (py >= 0) & (py < hoog)
        px, py, afstand, rgb = px[ok], py[ok], afstand[ok], rgb[ok]
        ok2 = afstand < diepte[py, px]
        px, py, afstand, rgb = px[ok2], py[ok2], afstand[ok2], rgb[ok2]
        volgorde = np.argsort(-afstand)
        px, py, afstand, rgb = px[volgorde], py[volgorde], afstand[volgorde], rgb[volgorde]
        beeld[py, px] = rgb
        diepte[py, px] = afstand

    def model(self, beeld, diepte, cam, geo_file, tex, glow, plek, yaw, helder=0.5):
        """The Barbecuerog (or any geo) standing at `plek` (feet), facing `yaw` (Minecraft: 0 = south, 90 = west)."""
        import wiki_renders as wr
        with tempfile.NamedTemporaryFile("w", suffix=".geo.json", delete=False) as fh:
            json.dump(geo_file, fh)
            pad = fh.name
        quads = wr.geo_quads(pad, tex)
        os.unlink(pad)
        oog = cam[0]
        # model space: front = -z; yaw 0 looks south (+z): turn by 180 + yaw around y
        a = math.radians(180 - yaw)
        R = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]], np.float32)
        k = glow[..., 3:4].astype(np.float32) / 255.0
        alle_p, alle_c = [], []
        for q in quads:
            o, qu, qv = R @ q.origin + plek, R @ q.u, R @ q.v
            midden = o + (qu + qv) / 2
            afst = max(1.0, float(np.linalg.norm(midden - oog)))
            dicht = cam[5] / (2 * cam[4] * cam[5] / cam[6]) / afst          # pixels per block at that distance
            nu, nv = max(2, int(np.linalg.norm(qu) * dicht * 1.5) + 1), max(2, int(np.linalg.norm(qv) * dicht * 1.5) + 1)
            A, Bv = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
            P = o[None, None, :] + A[..., None] * qu[None, None, :] + Bv[..., None] * qv[None, None, :]
            th, tw = tex.shape[:2]
            u0, v0, u1, v1 = q.uv
            tu = np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)
            tv = np.clip((v0 + Bv * (v1 - v0)) * th, 0, th - 1).astype(int)
            rgba = tex[tv, tu].astype(np.float32)
            g = glow[tv, tu].astype(np.float32)
            kk = g[..., 3:4] / 255.0
            nrm = R @ q.normal
            schaduw = 0.6 + 0.4 * max(0.0, float(nrm @ np.array([0.2, 0.9, -0.3], np.float32)))
            c = rgba[..., :3] * helder * schaduw * (1 - kk) + np.clip(g[..., :3] * 1.15, 0, 255) * kk
            m = rgba[..., 3] > 20
            alle_p.append(P[m])
            alle_c.append(c[m])
        self.punten(beeld, diepte, cam, np.concatenate(alle_p), np.concatenate(alle_c))

    def figuur(self, beeld, diepte, cam, plek, rgb=(255, 150, 200), hoog=1.0, breed=0.7):
        """A guh-sized marker (a blob with ears) at a spot: a character of the cast, or the player (taller)."""
        n = 900
        rng = np.random.default_rng(1)
        p = rng.random((n, 3)) * np.array([breed, hoog, breed]) + np.array(plek) - np.array([breed / 2, 0, breed / 2])
        oor = rng.random((120, 3)) * np.array([breed * 1.2, 0.25, 0.2]) + np.array(plek) + np.array([-breed * 0.6, hoog, -0.1])
        pts = np.concatenate([p, oor]).astype(np.float32)
        c = np.repeat(np.array(rgb, np.float32)[None, :], len(pts), 0) * (0.7 + 0.3 * rng.random((len(pts), 1)))
        self.punten(beeld, diepte, cam, pts, c)


def naar_png(beeld, pad, schaal=2):
    a = np.clip(beeld, 0, 255).astype(np.uint8)
    img = Image.fromarray(a)
    if schaal != 1:
        img = img.resize((img.width * schaal, img.height * schaal), Image.NEAREST)
    img.save(pad)


# the standing views of the mine: name -> (eye, look at)
BLIKKEN = {
    "plein_west": ((24.5, 34.5, 66.5), (37, 34, 62)),
    "poort": ((31.5, 32.6, 62.5), (37, 34.5, 62)),
    "westhal": ((44.5, 33.5, 64.5), (40, 32.5, 59)),
    "hefboomhal": ((49.5, 24.6, 40.5), (38, 22.5, 29)),
    "hefbomen": ((44.5, 22.7, 33.5), (41, 22.9, 27)),
    "putkamer": ((27.5, 24.6, 41.5), (18, 22, 31)),
    "runen": ((19.5, 22.7, 35.5), (13, 22.4, 35.5)),
    "zuilenhal": ((44.5, 14.7, 14.5), (18, 16, 14.5)),
    "zuilenhal_in": ((21.5, 13.7, 24.5), (40, 16, 12)),
    "kloof": ((44.5, 15.2, 14.5), (70, 10, 14)),
    "kloof_boven": ((50.5, 20.5, 5.5), (62, 9, 15)),
    "brug": ((63.5, 13.7, 14.5), (80, 13, 14.5)),
    "brug_terug": ((79.5, 13.7, 14.5), (60, 14, 14.5)),
    "oostplein": ((73.5, 34.5, 58.5), (59, 34, 62)),
}


def alles(b, out, alleen=()):
    wil = lambda naam: not alleen or naam in alleen
    if wil("snede"):
        snede(b, B.G - 1, B.MAAT[1] - 1).save(os.path.join(out, "snede_boven.png"))
        snede(b, B.MIDDEL - 1, B.MIDDEL + 3).save(os.path.join(out, "snede_midden.png"))
        snede(b, 0, B.DIEP + 5).save(os.path.join(out, "snede_diep.png"))
        snede(b, B.G - 1, B.MAAT[1] - 1, draai=2).save(os.path.join(out, "snede_boven_oost.png"))
    k = None
    for naam, (oog, kijk) in BLIKKEN.items():
        if not wil(naam) and not wil("blik") and not wil("werk"):
            continue
        k = k or Kijker(b)
        for werk in (False, True):
            if werk and not (wil("werk") or wil(naam)):
                continue
            beeld, diepte, cam = k.teken(oog, kijk, werk=werk)
            for kind, id, x, y, z, yaw, plek, van, tot in B.CAST:
                k.figuur(beeld, diepte, cam, (x + 0.5, y, z + 0.5))
            naar_png(beeld, os.path.join(out, f"{'werk' if werk else 'blik'}_{naam}.png"))
