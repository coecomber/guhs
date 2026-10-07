"""
bbq2 (oude-scenes) - pictures of the six old-story scenes, for whoever stages them (not part of the build; needs tools/ on
the path and the generated templates in data/guhs/structure).

  Wereld(sid)            a story's real template read back from its .nbt as a grid: what is solid, what colour, which part of
                         a cell is filled (slabs, snow layers, carpets), and where the template says nothing (the terrain the
                         structure was set into: solid up to the scene's `grond`, open air above it)
  Wereld.vrij(p)         is this point free air? (what oude_scenes.scene_check asks for every camera and every actor)
  Wereld.kaart(...)      a top-down map of a part of the template with a coordinate grid: for finding the numbers
  Wereld.teken(...)      a little ray caster: the template seen from a camera position looking at a point
  scene_frames(...)      the frames of a scene at given ticks: the camera of that moment, every actor where the script has it
                         (a guh-sized or player-sized figure in its own colour), the bars, and the scene's own weather

  python tools/features/oude_scenes_beeld.py <out dir> [scene id ...]      every scene (or the ones named), a frame per 2 seconds
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

AIR = "minecraft:air"
# blocks a camera may hang in and a ray goes through: plants, lamps, signs, rails, the thin things
DUN = ("torch", "lantern", "sign", "banner", "flower", "tulip", "allium", "orchid", "bluet", "daisy", "poppy", "dandelion", "cornflower",
       "sapling", "short_grass", "tall_grass", "fern", "vine", "ladder", "rail", "button", "pressure_plate", "lever", "chain", "candle",
       "pluisgras", "sprietjes", "bloempje", "hibiscus", "jigsaw", "structure_void", "light", "cobweb", "tripwire", "string", "redstone_wire",
       "dead_bush", "sugar_cane", "bamboo_sapling", "kelp", "seagrass", "coral_fan", "scaffolding", "item_frame", "flower_pot", "potted_",
       "campfire", "end_rod", "lightning_rod", "amethyst_cluster", "amethyst_bud", "pointed_dripstone", "sterretje", "dakplek",
       "notitieplek", "shuckle_plekje", "rommel", "portaal", "fire", "smeulkooltjes", "pindasausplasje", "plasje", "snoer", "slinger",
       "vlaggetjes", "guhlampje", "bars", "tralies", "pane", "fence", "muur", "wall", "wolkenstroom")
GLAS = ("glass", "ice")
TANK = ("kloontank", "tankwand")                     # the kloontank: pink liquid behind glass, drawn as a pink body
LAAG = ("carpet", "snow", "tapijt")                  # a thin layer on the floor
LICHTGEVEND = ("lantern", "glowstone", "froglight", "sea_lantern", "shroomlight", "torch", "campfire", "gloeikool", "knuffelhart",
               "portaal", "end_rod", "lamp")
KLEUR = {"minecraft:snow": (244, 250, 252), "minecraft:snow_block": (240, 247, 250), "minecraft:powder_snow": (246, 250, 252),
         "minecraft:water": (64, 110, 200), "guhs:kaas_saus": (242, 165, 22), "minecraft:grass_block": (112, 168, 72),
         "guhs:knuffelgras": (166, 214, 150), "minecraft:glass": (210, 236, 240), "minecraft:pink_stained_glass": (240, 170, 210),
         "guhs:knuffelhart": (246, 120, 180), "guhs:barbecuether_portaal": (255, 150, 40),
         "minecraft:ice": (150, 190, 250), "minecraft:packed_ice": (140, 180, 245), "minecraft:blue_ice": (120, 170, 250),
         "guhs:kaasknabbel_stone": (236, 196, 96), "minecraft:smooth_quartz": (236, 230, 222), "minecraft:smooth_quartz_slab": (236, 230, 222),
         "minecraft:quartz_stairs": (236, 230, 222), "minecraft:smooth_sandstone": (224, 214, 170), "minecraft:purpur_slab": (170, 126, 170),
         "minecraft:purpur_stairs": (170, 126, 170), "minecraft:stone_brick_stairs": (122, 122, 122), "minecraft:water_cauldron": (70, 70, 80),
         "guhs:guh_bank": (238, 150, 190), "guhs:guh_stoel": (238, 150, 190), "guhs:guh_tafel": (200, 150, 110),
         "guhs:knuffelsteen_gezicht": (232, 206, 214), "guhs:seizoensbloembak": (150, 110, 80), "guhs:kokosnoot": (120, 84, 50),
         "guhs:schilly_eitjes": (200, 220, 240), "guhs:houtskoolsteen_stenen_hek": (60, 56, 56), "guhs:sterrenlantaarn": (255, 236, 150),
         "guhs:mewtwo_knabbelschaal": (210, 170, 90), "guhs:mewtwo_onderdelenkist": (130, 96, 56), "guhs:mewtwo_computer": (90, 100, 120),
         "guhs:mewtwo_reageerbuisjes": (200, 230, 240), "guhs:guhnestje": (220, 190, 150), "guhs:guh_bloempot": (170, 110, 80),
         "guhs:guh_wiegje": (240, 200, 210)}


def kleur(name):
    import wiki_renders as wr
    if name in KLEUR:
        return KLEUR[name]
    c = wr.block_colour(name)
    return c if c else (200, 0, 200)


def _is(name, woorden):
    kort = name.split(":")[1]
    return any(w in kort for w in woorden)


class Wereld:
    def __init__(self, sid, grond=None, lucht=(140, 190, 240), blokken=None, maat=None, als=None):
        """sid: the template (data/guhs/structure/<sid>.nbt); grond: cells the template leaves alone count as ground up to
        and including this template y and as open air above it (None: all of them are air); als: {part of a block name:
        colour} for blocks that are drawn as a solid block of that colour in this scene (the roof tiles that are laid by
        then, the portal that burns by then) while the check still sees what the template has."""
        import wiki_renders as wr
        self.grond = grond
        self._cache = None
        map_ = os.environ.get("OUDE_SCENES_CACHE")
        if blokken is None and map_ and not als:
            self._cache = os.path.join(map_, sid.replace("/", "_") + f"_{grond}.npz")
            if os.path.exists(self._cache) and os.path.getmtime(self._cache) > os.path.getmtime(
                    os.path.join("src", "main", "resources", "data", "guhs", "structure", sid + ".nbt")):
                d = np.load(self._cache, allow_pickle=True)
                self.sid, self.maat, self.lucht = sid, tuple(int(v) for v in d["maat"]), np.array(lucht, np.float32)
                for k in ("vol", "dicht", "glas", "gloeit", "rgb", "lo", "hi", "gezet"):
                    setattr(self, k, d[k])
                self.blokken, self.entities = d["blokken"].item(), list(d["entities"])
                return
        if blokken is None:
            s = wr.load_structure(sid)
            if s is None:
                raise SystemExit(f"oude_scenes_beeld: no template {sid} (run the generators first)")
            blokken, maat = s.blocks, s.size
            self.entities = s.entities
        else:
            self.entities = []
        self.sid = sid
        X, Y, Z = maat
        self.maat = (X, Y, Z)
        self.blokken = blokken
        self.lucht = np.array(lucht, np.float32)
        self.vol = np.zeros((X, Y, Z), bool)            # a ray stops here
        self.dicht = np.zeros((X, Y, Z), bool)          # a camera or an actor can't be here
        self.glas = np.zeros((X, Y, Z), bool)
        self.gloeit = np.zeros((X, Y, Z), bool)
        self.rgb = np.zeros((X, Y, Z, 3), np.float32)
        self.lo = np.zeros((X, Y, Z), np.float32)       # the filled part of the cell (0..1 of its height)
        self.hi = np.ones((X, Y, Z), np.float32)
        gezet = np.zeros((X, Y, Z), bool)
        for (x, y, z), (name, props, _) in blokken.items():
            gezet[x, y, z] = True
            if name == AIR or name.endswith(":cave_air") or name.endswith(":void_air"):
                continue
            anders = next((k for w_, k in (als or {}).items() if w_ in name), None)
            if anders is not None:
                self.vol[x, y, z] = True
                self.rgb[x, y, z] = anders
                continue
            if _is(name, TANK):
                self.vol[x, y, z] = True
                self.dicht[x, y, z] = True
                self.gloeit[x, y, z] = True
                self.rgb[x, y, z] = (236, 130, 190)
                continue
            if _is(name, GLAS):
                self.glas[x, y, z] = True
                self.dicht[x, y, z] = True
                self.rgb[x, y, z] = kleur(name)
                continue
            if _is(name, LAAG) and not name.endswith("snow_block") and "powder" not in name:
                lagen = int((props or {}).get("layers", 1))
                self.vol[x, y, z] = True
                self.hi[x, y, z] = max(0.0625, lagen / 8.0) if "snow" in name else 0.0625
                self.dicht[x, y, z] = lagen >= 5
                self.rgb[x, y, z] = kleur(name)
                continue
            if _is(name, DUN):
                if _is(name, ("fence", "muur", "wall", "pane", "bars", "tralies")):
                    self.dicht[x, y, z] = True       # (thin, but nobody stands in a fence)
                continue
            self.vol[x, y, z] = True
            self.dicht[x, y, z] = True
            self.rgb[x, y, z] = kleur(name)
            self.gloeit[x, y, z] = _is(name, LICHTGEVEND)
            soort = (props or {}).get("type")
            if name.endswith("_slab") or name.endswith("_plaat"):
                if soort == "bottom":
                    self.hi[x, y, z] = 0.5
                elif soort == "top":
                    self.lo[x, y, z] = 0.5
        if grond is not None:
            leeg = ~gezet
            leeg[:, grond + 1:, :] = False
            self.vol |= leeg
            self.dicht |= leeg
            self.rgb[leeg] = (150, 140, 120)
        self.gezet = gezet
        if self._cache:
            np.savez_compressed(self._cache, maat=np.array(self.maat), vol=self.vol, dicht=self.dicht, glas=self.glas, gloeit=self.gloeit,
                                rgb=self.rgb, lo=self.lo, hi=self.hi, gezet=self.gezet, blokken=np.array(self.blokken, dtype=object),
                                entities=np.array(self.entities, dtype=object))

    # --- questions ------------------------------------------------------------------------------------------------------------
    def binnen(self, p):
        return all(0 <= int(math.floor(p[i])) < self.maat[i] for i in range(3))

    def vrij(self, p):
        """Is this point free (air, a plant, a lamp)? Outside the template: free above its ground, not below."""
        c = tuple(int(math.floor(v)) for v in p)
        if not self.binnen(p):
            return True
        if not self.dicht[c]:
            return True
        f = p[1] - c[1]
        return not (self.lo[c] <= f < self.hi[c]) and not self.glas[c]

    def blok(self, p):
        c = tuple(int(math.floor(v)) for v in p)
        return self.blokken.get(c, ("?",))[0]

    def top(self, x, z, van=None):
        """The y of the top of the highest solid cell in this column at or below `van` (the floor an actor stands on)."""
        x, z = int(math.floor(x)), int(math.floor(z))
        if not (0 <= x < self.maat[0] and 0 <= z < self.maat[2]):
            # (outside the template: the land it stands in, at the height of its own ground)
            return float(self.grond + 1) if self.grond is not None else float("nan")
        y = int(min(self.maat[1] - 1, self.maat[1] - 1 if van is None else math.floor(van)))
        while y >= 0:
            if self.vol[x, y, z]:
                return y + float(self.hi[x, y, z])
            y -= 1
        return 0.0

    def zoek(self, woord):
        """Every block whose name contains `woord`: [(x, y, z, name, props)]."""
        return sorted((x, y, z, n, p) for (x, y, z), (n, p, _) in self.blokken.items() if woord in n)

    # --- a map ----------------------------------------------------------------------------------------------------------------
    def kaart(self, x0, z0, x1, z1, px=12, y_max=None, raster=5):
        """Top-down: every column's highest block at or below y_max in its colour, shaded by height, with its height written
        in it (px >= 12) and a grid line + coordinate every `raster` blocks."""
        y_max = self.maat[1] - 1 if y_max is None else y_max
        img = Image.new("RGB", ((x1 - x0 + 1) * px + 30, (z1 - z0 + 1) * px + 14), (20, 20, 24))
        d = ImageDraw.Draw(img)
        hoogten = []
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                y = y_max
                while y >= 0 and not (self.vol[x, y, z] or self.glas[x, y, z]):
                    y -= 1
                hoogten.append((x, z, y))
        ys = [y for _, _, y in hoogten if y >= 0]
        lo, hi = (min(ys), max(ys)) if ys else (0, 1)
        for x, z, y in hoogten:
            if y < 0:
                continue
            c = self.rgb[x, y, z] * (0.55 + 0.45 * (y - lo) / max(1, hi - lo))
            X, Zp = 30 + (x - x0) * px, 14 + (z - z0) * px
            d.rectangle((X, Zp, X + px - 1, Zp + px - 1), fill=tuple(int(v) for v in c))
            if px >= 12:
                d.text((X + 1, Zp), str(y), fill=(0, 0, 0))
        for x in range(x0, x1 + 1):
            if x % raster == 0:
                X = 30 + (x - x0) * px
                d.line((X, 14, X, img.height), fill=(255, 255, 255))
                d.text((X + 1, 1), str(x), fill=(255, 255, 0))
        for z in range(z0, z1 + 1):
            if z % raster == 0:
                Zp = 14 + (z - z0) * px
                d.line((30, Zp, img.width, Zp), fill=(255, 255, 255))
                d.text((1, Zp + 1), str(z), fill=(255, 255, 0))
        return img

    # --- the ray caster ---------------------------------------------------------------------------------------------------------
    def teken(self, oog, kijk, breed=480, hoog=270, fov=70.0, ver=96.0, nacht=0.0, mist=None, zon=(0.35, 0.85, -0.4)):
        """(picture h x w x 3 float, depth h x w, camera) from `oog` looking at `kijk`. nacht 0..1 darkens (lamps keep
        glowing); mist = (colour, distance at which nothing is left)."""
        oog, kijk = np.array(oog, np.float32), np.array(kijk, np.float32)
        f = kijk - oog
        f /= max(1e-6, np.linalg.norm(f))
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
        hemel = self.lucht * (1 - 0.86 * nacht)
        beeld = np.repeat(hemel[None, :], n, 0).astype(np.float32)
        # a soft gradient in the sky
        beeld *= (0.82 + 0.18 * np.clip(d[:, 1:2] * 2 + 0.5, 0, 1))
        diepte = np.full(n, ver, np.float32)
        tint = np.ones((n, 3), np.float32)
        bezig = np.arange(n)
        X, Y, Z = self.maat
        zon = np.array(zon, np.float32)
        zon /= np.linalg.norm(zon)
        stap = 0.12
        vorige = np.floor(np.repeat(oog[None, :], n, 0)).astype(np.int32)
        t = 0.0
        while t < ver and len(bezig):
            t += stap
            p = oog[None, :] + d[bezig] * t
            c = np.floor(p).astype(np.int32)
            binnen = (c[:, 0] >= 0) & (c[:, 0] < X) & (c[:, 1] >= 0) & (c[:, 1] < Y) & (c[:, 2] >= 0) & (c[:, 2] < Z)
            cc = np.clip(c, 0, [X - 1, Y - 1, Z - 1])
            fy = p[:, 1] - c[:, 1]
            cel = (cc[:, 0], cc[:, 1], cc[:, 2])
            raak = binnen & self.vol[cel] & (fy >= self.lo[cel]) & (fy < self.hi[cel])
            # glass tints what lies behind it, once per pane
            door_glas = binnen & self.glas[cel] & np.any(c != vorige[bezig], axis=1)
            if door_glas.any():
                gi = bezig[door_glas]
                tint[gi] = tint[gi] * 0.82 + self.rgb[cel][door_glas] / 255.0 * 0.12
            if raak.any():
                idx = bezig[raak]
                rc = cc[raak]
                verschil = rc - vorige[idx]
                normaal = np.zeros((len(idx), 3), np.float32)
                boven = verschil[:, 1] != 0
                normaal[boven, 1] = -np.sign(verschil[boven, 1])
                zij_x = ~boven & (verschil[:, 0] != 0)
                normaal[zij_x, 0] = -np.sign(verschil[zij_x, 0])
                zij_z = ~boven & ~zij_x
                normaal[zij_z, 2] = -np.sign(verschil[zij_z, 2])
                # (a slab or a snow layer hit from the side inside its own cell: its top)
                zelfde = np.all(verschil == 0, axis=1)
                normaal[zelfde] = (0, 1, 0)
                licht = 0.52 + 0.48 * np.clip(normaal @ zon, 0, 1)
                licht = licht * (1 - 0.8 * nacht)
                kleur_ = self.rgb[rc[:, 0], rc[:, 1], rc[:, 2]]
                gloei = self.gloeit[rc[:, 0], rc[:, 1], rc[:, 2]]
                uit = kleur_ * licht[:, None]
                uit = np.where(gloei[:, None], kleur_, uit)
                beeld[idx] = uit * tint[idx]
                diepte[idx] = t
            vorige[bezig] = c
            onder = p[:, 1] < -2
            bezig = bezig[~(raak | onder)]
        if mist is not None:
            mk, mv = mist
            a = np.clip(diepte / mv, 0, 1)[:, None] ** 0.8
            beeld = beeld * (1 - a) + np.array(mk, np.float32)[None, :] * a
        return beeld.reshape(hoog, breed, 3), diepte.reshape(hoog, breed), (oog, f, r, u, tan, breed, hoog)

    @staticmethod
    def punten(beeld, diepte, cam, pts, rgb):
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

    def doos(self, beeld, diepte, cam, lo, hi, rgb, yaw=0.0, om=None, licht=1.0):
        """A solid box (lo..hi, template blocks), turned `yaw` degrees (Minecraft) round the vertical through `om`."""
        lo, hi = np.array(lo, np.float32), np.array(hi, np.float32)
        om = np.array(om if om is not None else (lo + hi) / 2, np.float32)
        a = math.radians(-yaw)
        R = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]], np.float32)
        oog = cam[0]
        dicht = cam[5] / (2 * cam[4] * cam[5] / cam[6]) / max(0.6, float(np.linalg.norm((lo + hi) / 2 - oog)))
        alle_p, alle_c = [], []
        for as_ in range(3):
            a2, b2 = [i for i in range(3) if i != as_]
            for kant, helder in ((lo[as_], 0.62 if as_ != 1 else 0.5), (hi[as_], 0.82 if as_ != 1 else 1.0)):
                nu = max(2, int((hi[a2] - lo[a2]) * dicht * 1.6) + 1)
                nv = max(2, int((hi[b2] - lo[b2]) * dicht * 1.6) + 1)
                A, Bv = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
                P = np.zeros(A.shape + (3,), np.float32)
                P[..., as_] = kant
                P[..., a2] = lo[a2] + A * (hi[a2] - lo[a2])
                P[..., b2] = lo[b2] + Bv * (hi[b2] - lo[b2])
                P = (P.reshape(-1, 3) - om) @ R.T + om
                alle_p.append(P)
                alle_c.append(np.repeat(np.array(rgb, np.float32)[None, :] * helder * licht, len(P), 0))
        self.punten(beeld, diepte, cam, np.concatenate(alle_p), np.concatenate(alle_c))

    def figuur(self, beeld, diepte, cam, plek, rgb=(255, 150, 200), maat=1.0, yaw=0.0, soort="guh", licht=1.0, oog=(30, 60, 90)):
        """A stand-in for a character at a spot (feet): a guh (body, head, two ears, eyes; maat = its scale), "speler" (a player:
        two blocks tall) or "boek" (a little open book on the floor)."""
        x, y, z = plek
        om = (x, y, z)

        def d(lo, hi, k):
            self.doos(beeld, diepte, cam, (x + lo[0], y + lo[1], z + lo[2]), (x + hi[0], y + hi[1], z + hi[2]), k, yaw=yaw, om=om, licht=licht)
        if soort == "speler":
            d((-0.25, 0, -0.13), (0.25, 0.75, 0.13), (60, 70, 150))
            d((-0.25, 0.75, -0.13), (0.25, 1.45, 0.13), rgb)
            d((-0.25, 1.45, -0.25), (0.25, 1.95, 0.25), (190, 150, 120))
            d((-0.16, 1.66, 0.25), (-0.06, 1.76, 0.27), (30, 30, 40))
            d((0.06, 1.66, 0.25), (0.16, 1.76, 0.27), (30, 30, 40))
            return
        if soort == "boek":
            d((-0.3, 0, -0.2), (0.3, 0.08, 0.2), rgb)
            return
        k = maat
        d((-0.3 * k, 0, -0.28 * k), (0.3 * k, 0.55 * k, 0.28 * k), tuple(c * 0.9 for c in rgb))
        d((-0.38 * k, 0.5 * k, -0.34 * k), (0.38 * k, 1.05 * k, 0.34 * k), rgb)
        d((-0.24 * k, 0.72 * k, 0.34 * k), (-0.08 * k, 0.9 * k, 0.36 * k), oog)
        d((0.08 * k, 0.72 * k, 0.34 * k), (0.24 * k, 0.9 * k, 0.36 * k), oog)
        d((-0.36 * k, 1.05 * k, -0.06 * k), (-0.14 * k, 1.3 * k, 0.06 * k), rgb)
        d((0.14 * k, 1.05 * k, -0.06 * k), (0.36 * k, 1.3 * k, 0.06 * k), rgb)


def naar_png(beeld, pad, schaal=2):
    a = np.clip(beeld, 0, 255).astype(np.uint8)
    img = Image.fromarray(a)
    if schaal != 1:
        img = img.resize((img.width * schaal, img.height * schaal), Image.NEAREST)
    img.save(pad)
    return img


# =====================================================================================================================
# frames of the scenes
# =====================================================================================================================
def _yaw_op(s, naam, t):
    """Where an actor looks at tick t (Minecraft yaw): along its walk while it walks, else at its last `kijk`, else its start yaw."""
    def yaw(van, naar):
        return math.degrees(math.atan2(-(naar[0] - van[0]), naar[2] - van[2]))
    uit = next(a.yaw for a in s.acteurs if a.naam == naam)
    hier = s.plek(naam, t)
    laatste = -10 ** 9
    for a, t0, t1, naar in sorted((l for l in s.lopen if l[0] == naam), key=lambda l: l[1]):
        if t0 <= t:
            van = s.plek(naam, t0)
            if abs(naar[0] - van[0]) + abs(naar[2] - van[2]) > 1e-3:
                uit, laatste = yaw(van, naar), t0
    for a, t0, naar in sorted((x for x in s.kijken if x[0] == naam), key=lambda x: x[1]):
        if t0 <= t and t0 >= laatste and not s.loopt(naam, t) and abs(naar[0] - hier[0]) + abs(naar[2] - hier[2]) > 1e-3:
            uit = yaw(hier, naar)
    return uit


def scene_frames(w, out, s, tijden, breed=480, hoog=270, werk=False):
    """Draws scene `s` at these ticks in its template `w` (werk: a builder's lamp: the night is only half as dark). Returns
    the file names."""
    namen = []
    rng = np.random.default_rng(7)
    for t in tijden:
        oog, kijk = s.camera_op(t)
        weer = s.weer_op(t)
        if werk:
            weer["nacht"] *= 0.45
        mist = None
        if weer.get("mist", 0) > 0.01:
            # the fog of the storm: nothing left at `zicht` blocks
            mist = ((230, 237, 250), weer["zicht"])
        beeld, diepte, cam = w.teken(oog, kijk, breed=breed, hoog=hoog, nacht=weer.get("nacht", 0.0), mist=mist)
        donker = 1 - 0.8 * weer.get("nacht", 0.0)
        for a in s.acteurs:
            if not s.zichtbaar(a.naam, t):
                continue
            p = s.plek(a.naam, t)
            w.figuur(beeld, diepte, cam, p, rgb=a.kleur, maat=a.maat, yaw=_yaw_op(s, a.naam, t),
                     soort="speler" if a.soort == "speler" else a.vorm, licht=donker if not a.gloeit else 1.0)
        if mist is not None:
            # the figures are in the fog too
            mk, mv = mist
            a_ = np.clip(diepte / mv, 0, 1)[..., None] ** 0.8
            beeld = beeld * (1 - a_) + np.array(mk, np.float32)[None, None, :] * a_
        if weer.get("sneeuw", 0) > 0.01:
            n = int(1400 * weer["sneeuw"])
            xs, ys = rng.integers(0, breed, n), rng.integers(0, hoog, n)
            beeld[ys, xs] = 255
        if weer.get("regen", 0) > 0.01:
            n = int(500 * weer["regen"])
            xs, ys = rng.integers(0, breed, n), rng.integers(0, hoog - 6, n)
            for k in range(6):
                beeld[ys + k, xs] = beeld[ys + k, xs] * 0.5 + np.array([150, 170, 220], np.float32) * 0.5
        if weer.get("flits", 0) > 0.01:
            beeld = beeld * (1 - weer["flits"]) + 255 * weer["flits"]
        zwart = s.zwart_op(t)
        if zwart > 0:
            beeld = beeld * (1 - zwart)
        balk = int(hoog * 0.13)
        beeld[:balk] = 0
        beeld[-balk:] = 0
        naam = f"{s.id}_{t:04d}.png"
        img = naar_png(beeld, os.path.join(out, naam), schaal=2)
        zin = s.zin_op(t)
        if zin:
            d = ImageDraw.Draw(img)
            d.text((12, img.height - 22), zin[:150], fill=(255, 255, 255))
            img.save(os.path.join(out, naam))
        namen.append(naam)
    return namen


def blad(out, namen, pad, kolommen=4):
    """A contact sheet of frames."""
    plaatjes = [Image.open(os.path.join(out, n)) for n in namen]
    if not plaatjes:
        return
    bw, bh = plaatjes[0].size
    bw, bh = bw // 2, bh // 2
    rijen = (len(plaatjes) + kolommen - 1) // kolommen
    vel = Image.new("RGB", (kolommen * bw, rijen * bh), (0, 0, 0))
    for i, p in enumerate(plaatjes):
        vel.paste(p.resize((bw, bh), Image.LANCZOS), ((i % kolommen) * bw, (i // kolommen) * bh))
    vel.save(pad)


def main(out, alleen=()):
    from features import oude_scenes_scene as S
    os.makedirs(out, exist_ok=True)
    werk = "werk" in alleen
    alleen = [a for a in alleen if a != "werk"]
    for s in S.alle():
        if alleen and s.id not in alleen and s.kort not in alleen:
            continue
        w = Wereld(s.template, grond=s.grond, lucht=s.lucht, als=s.als)
        tijden = list(range(10, s.duur, 40))
        namen = scene_frames(w, out, s, tijden, werk=werk)
        blad(out, namen, os.path.join(out, f"blad_{s.kort}.png"))
        print("drew", s.id, len(namen), "frames")


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    main(sys.argv[1], sys.argv[2:])
