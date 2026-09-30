"""
Het Knuffelbad (2.8) - the three water slides: their paths, the ride physics and how their blocks are built.

A slide is a path of the ride (a "turtle" that goes straight, turns, spirals down a funnel, runs through a tube or flies
through the air), sampled every STAP blocks. Every sample says where the running surface is under the middle of the
ride (p), which way the ride goes (t), which way is right (b, always horizontal), and the cross-section of the slide
there (the profile):
  goot      an open chute: the surface is h(d) = k*d^2 + j*d above the middle (d = sideways, in blocks)
  trechter  a funnel: the cone wall (h(d) = j*d, d outwards is up the wall); the funnel itself is a height field
  buis      a closed tube of radius R: the ride goes round the tube (lateral = angle)
  lucht     in the air (the drain of a funnel, a launch into the pool): no surface
  water     floating on the pool at the end
The same path is written to assets/guhs/knuffelbad/<id>.json for the mod (GlijPad.java, local coordinates relative to
the slide's start block facing north) and used here to build the slide's blocks, so the ride always follows them.

The ride's speed (both here and in GlijPad.java, per tick): a = -G*dy/ds + (v_pref - v)*RELAX on a surface, a = -G*dy/ds
in the air (a ballistic arc then has exactly the right speed), v kept between V_MIN and V_MAX.
"""
import json
import math

import numpy as np

DS = 0.05          # integration step of the turtle (blocks of path)
STAP = 0.5         # exported sample spacing (blocks of path)
G_PHYS = 0.04      # gravity along the path (blocks/tick^2)
RELAX = 0.04       # how fast the speed goes to the section's preferred speed (per tick)
V_MIN, V_MAX = 0.15, 1.3
RING = 0.28        # the ring's middle above the running surface

PROFIELEN = {"goot": 0, "trechter": 1, "buis": 2, "lucht": 3, "water": 4}
OVERGANG = 2.5     # blocks after a change of profile in which the ride glides from the old cross-section into the new one
RECHTOP = 1.5      # blocks of a flight in which the rider turns upright (the normal goes to straight up)


def _glad(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)
FX = {"sterren": 1, "spetters": 2, "draai": 4, "plons": 8, "donker": 16, "lichtring": 32, "val": 64, "schuim": 128, "start": 256}


class Pad:
    """A slide path, built like a turtle. Angles: psi = heading (0 = north/-z, 90 = east), theta = pitch (negative = down)."""

    def __init__(self, naam, x, y, z, richting):
        self.naam = naam
        self.x, self.y, self.z = float(x), float(y), float(z)
        self.psi = math.radians(richting)
        self.theta = 0.0
        self.rows = []
        self.state = dict(prof="goot", k=0.22, j=0.0, dmax=1.5, R=2.0, vpref=0.42, fx=0)
        self.trechters = []        # funnels (for the voxel builder)
        self.helices = []          # circle centres of the helix parts (for the towers)
        self.eendjes = []          # duck spots (s, lateral -1..1)
        self.merk = {}             # named spots: s
        self.uitstap = None        # where the rider gets off (x, y, z, yaw)
        self._emit()

    # --- the turtle ---------------------------------------------------------------------------------------------------
    def s(self):
        return (len(self.rows) - 1) * DS

    def _emit(self):
        st = self.state
        self.rows.append([self.x, self.y, self.z, self.psi, self.theta, PROFIELEN[st["prof"]], st["k"], st["j"], st["dmax"], st["R"],
                          st["vpref"], st["fx"]])

    def _step(self, dpsi=0.0, dtheta=0.0):
        c = math.cos(self.theta)
        self.x += DS * c * math.sin(self.psi)
        self.z += -DS * c * math.cos(self.psi)
        self.y += DS * math.sin(self.theta)
        self.psi += dpsi
        self.theta += dtheta
        self._emit()

    def zet(self, **st):
        """Changes the profile from here on (prof, k, j, dmax, R, vpref, fx)."""
        self.state.update(st)
        return self

    def fx(self, *namen):
        f = 0
        for n in namen:
            f |= FX[n]
        self.state["fx"] = f
        return self

    def recht(self, lengte, helling=None, **st):
        """Straight on for `lengte` blocks; the pitch goes smoothly (in angle) to `helling` degrees by the end."""
        self.state.update(st)
        n = max(1, int(round(lengte / DS)))
        t0 = self.theta
        t1 = t0 if helling is None else math.radians(helling)
        for i in range(n):
            f0, f1 = _ease(i / n), _ease((i + 1) / n)
            self._step(0.0, (t1 - t0) * (f1 - f0))
        return self

    def bocht(self, hoek, straal, helling=None, inloop=3.0, **st):
        """A horizontal turn of `hoek` degrees (positive = right) with radius `straal`; the curvature eases in and out over
        `inloop` blocks, the pitch goes smoothly to `helling`. A turn of more than 360 degrees is a helix."""
        self.state.update(st)
        a = math.radians(abs(hoek))
        kappa = 1.0 / straal
        e = min(inloop, a * straal * 0.45)
        hold = a * straal - e            # (ramps of length e at both ends turn e*kappa/2 each)
        total = hold + e
        n = max(1, int(round(total / DS)))
        sign = 1 if hoek >= 0 else -1
        t0 = self.theta
        t1 = t0 if helling is None else math.radians(helling)
        turned = 0.0
        for i in range(n):
            sm = (i + 0.5) * DS
            if sm < e:
                k = kappa * sm / e
            elif sm > total - e:
                k = kappa * max(0.0, (total - sm) / e)
            else:
                k = kappa
            f0, f1 = _ease(i / n), _ease((i + 1) / n)
            turned += k * DS
            self._step(sign * k * DS, (t1 - t0) * (f1 - f0))
        # (fix the rounding of the heading: exactly `hoek` degrees turned)
        self.psi += sign * (a - turned)
        return self

    def midden_van_bocht(self, hoek, straal):
        """The centre of the circle a turn would go round, from here (for placing towers inside helices)."""
        side = 1 if hoek >= 0 else -1
        return (self.x + side * straal * math.cos(self.psi), self.z + side * straal * math.sin(self.psi))

    def trechter(self, rondjes, r_rand, r_gat, kegel=0.75, rechtsom=True, afvoer=5.0, dmax=1.4, vpref=0.52, fx=("draai", "spetters"),
                 tot_y=None, kleuren=("roze", "wit")):
        """A funnel: from here (on the rim, going round it) spiral down the cone `rondjes` times to the drain hole, then drop
        `afvoer` blocks through the hole (spiralling on). The funnel: centre, rim radius r_rand, hole radius r_gat, the cone
        rises `kegel` blocks per block outwards. The funnel's shape is kept for the builder."""
        side = 1 if rechtsom else -1
        cx = self.x + side * r_rand * math.cos(self.psi)
        cz = self.z + side * r_rand * math.sin(self.psi)
        y_rand = self.y
        r_eind = r_gat + 1.2
        y_bodem = y_rand - kegel * (r_rand - r_gat)
        if tot_y is not None:       # (drop through the hole until this height: e.g. into the foam bath below)
            afvoer = y_rand - kegel * (r_rand - r_eind) - tot_y
        trechter = dict(cx=cx, cz=cz, r_rand=r_rand, r_gat=r_gat, kegel=kegel, y_rand=y_rand, y_bodem=y_bodem, rechtsom=rechtsom,
                        s_in=self.s(), in_hoek=math.atan2(self.z - cz, self.x - cx), kleuren=kleuren)
        # the spiral: angle phi around the centre, radius r(phi) shrinking linearly
        phi0 = math.atan2(self.z - cz, self.x - cx)
        total = 2 * math.pi * rondjes
        f = 0
        for n in fx:
            f |= FX[n]
        j = -side * kegel     # the wall rises outwards: away from the centre, which is on the right when going round clockwise
        self.state.update(prof="trechter", k=0.0, j=j, dmax=dmax, vpref=vpref, fx=f)
        phi = 0.0
        dmax_gat = 0.9        # (near the hole the funnel narrows: the ride's room sideways too, down to 0.4 over the lip)
        while phi < total:
            eind = _ease(min(1.0, max(0.0, (phi - (total - 1.6)) / 1.6)))
            self.state["dmax"] = (0.4 + (dmax - 0.4) * _ease(min(1.0, phi / 2.2))) * (1 - eind) + dmax_gat * eind
            r = r_rand - (r_rand - r_eind) * phi / total
            # arc length step DS: dphi = DS / sqrt(r^2 + (dr/dphi)^2 + (dy/dphi)^2)
            drdphi = -(r_rand - r_eind) / total
            dydphi = kegel * drdphi
            dphi = DS / math.sqrt(r * r + drdphi * drdphi + dydphi * dydphi)
            phi += dphi
            r = r_rand - (r_rand - r_eind) * min(phi, total) / total
            ang = phi0 + side * phi
            nx, nz = cx + r * math.cos(ang), cz + r * math.sin(ang)
            ny = y_rand - kegel * (r_rand - r)
            self._goto(nx, ny, nz)
        # over the lip: the last bit of the spiral quickly in to the middle of the hole (sliding down the last of the cone)
        r_val = max(0.35, r_gat * 0.35)
        y_lip = self.y
        phi_d = 0.0
        swirl = 1.4
        while phi_d < swirl:
            dphi = DS / max(self._r(cx, cz), 0.5)
            phi_d += dphi
            f = _ease(phi_d / swirl)
            r = r_eind + (r_val - r_eind) * f
            if self.state["prof"] != "lucht":
                self.state["dmax"] = dmax_gat + (0.4 - dmax_gat) * _ease(min(1.0, phi_d / (0.6 * swirl)))
            y = y_lip - kegel * (r_eind - max(r, r_gat)) - max(0.0, r_gat - r) * 0.9
            if r < r_gat - 0.1 and self.state["prof"] != "lucht":
                self.state.update(prof="lucht", j=0.0, k=0.0, dmax=0.4, fx=FX["val"] | FX["draai"])
            ang = phi0 + side * (total + phi_d)
            self._goto(cx + r * math.cos(ang), y, cz + r * math.sin(ang))
        # the drop through the hole: spiralling on at a small radius, falling faster and faster
        self.state.update(prof="lucht", j=0.0, k=0.0, dmax=0.4, vpref=vpref, fx=FX["val"] | FX["draai"])
        y_top = self.y
        eind_y = y_lip - afvoer
        while self.y > eind_y:
            dphi = DS * 0.45 / r_val
            phi_d += dphi
            ang = phi0 + side * (total + phi_d)
            self._goto(cx + r_val * math.cos(ang), self.y - DS * 0.9, cz + r_val * math.sin(ang))
        trechter["s_uit"] = self.s()
        trechter["y_uit"] = self.y
        trechter["y_val"] = y_top
        self.trechters.append(trechter)
        return trechter

    def _r(self, cx, cz):
        return math.hypot(self.x - cx, self.z - cz)

    def _goto(self, nx, ny, nz):
        """Moves the turtle straight to a point (a small step), keeping its angles up to date."""
        dx, dy, dz = nx - self.x, ny - self.y, nz - self.z
        h = math.hypot(dx, dz)
        if h > 1e-9:
            self.psi = math.atan2(dx, -dz)
        self.theta = math.atan2(dy, h)
        self.x, self.y, self.z = nx, ny, nz
        self._emit()

    def vlucht(self, y_water, v_start):
        """A ballistic flight from here (the current pitch) with launch speed v_start until the water at y_water."""
        self.state.update(prof="lucht", k=0.0, j=0.0, dmax=0.5, fx=FX["val"])
        vh = v_start * math.cos(self.theta)
        vy = v_start * math.sin(self.theta)
        x0, y0, z0 = self.x, self.y, self.z
        sx, sz = math.sin(self.psi), -math.cos(self.psi)
        h = 0.0
        while True:
            h += DS * 0.7
            t = h / vh
            y = y0 + vy * t - 0.5 * G_PHYS * t * t
            if y <= y_water:
                # the last bit exactly down to the water (where the arc crosses it)
                a, b, c = -0.5 * G_PHYS / (vh * vh), vy / vh, y0 - y_water
                hw = (-b - math.sqrt(max(0.0, b * b - 4 * a * c))) / (2 * a)
                self._goto(x0 + sx * hw, y_water, z0 + sz * hw)
                break
            self._goto(x0 + sx * h, y, z0 + sz * h)
        return self

    def drijven(self, lengte, vpref=0.05, **st):
        """Floating on the pool at the end: straight on, slowing down."""
        self.state.update(prof="water", k=0.0, j=0.0, dmax=0.8, vpref=vpref, **st)
        self.theta = 0.0
        n = int(round(lengte / DS))
        for _ in range(n):
            self._step()
        return self

    def eendje(self, lateraal, voor=0.0):
        """A duck spot here (or `voor` blocks further on), lateral -1 (left) .. 1 (right)."""
        self.eendjes.append((self.s() + voor, lateraal))
        return self

    def rij(self, lengte, n, lateralen):
        """n duck spots spread over the next `lengte` blocks (to be built after them), with these laterals (cycled)."""
        for i in range(n):
            self.eendjes.append((self.s() + (i + 0.5) * lengte / n, lateralen[i % len(lateralen)]))
        return self


def _ease(t):
    """Smoothstep: 0..1 with zero slope at both ends (for the pitch changes)."""
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


# =====================================================================================================================
# sampling, frames and physics
# =====================================================================================================================
class Samples:
    """The path resampled every STAP blocks: arrays p, t, b, u (n x 3) and the profile columns."""

    def __init__(self, pad):
        rows = np.array(pad.rows, float)
        pos = rows[:, 0:3]
        seg = np.linalg.norm(np.diff(pos, axis=0), axis=1)
        acc = np.concatenate([[0.0], np.cumsum(seg)])
        self.lengte = float(acc[-1])
        n = int(math.floor(self.lengte / STAP)) + 1
        s = np.arange(n) * STAP
        self.s = s
        self.p = np.stack([np.interp(s, acc, pos[:, i]) for i in range(3)], axis=1)
        # the profile columns: nearest row (they only change at segment borders)
        idx = np.clip(np.searchsorted(acc, s), 0, len(rows) - 1)
        self.prof = rows[idx, 5].astype(int)
        self.k = rows[idx, 6]
        self.j = rows[idx, 7]
        self.dmax = rows[idx, 8]
        self.R = rows[idx, 9]
        self.vpref = rows[idx, 10]
        self.fx = rows[idx, 11].astype(int)
        # tangents (central differences), right vectors from the heading (always horizontal), up = b x t
        t = np.gradient(self.p, axis=0)
        t /= np.linalg.norm(t, axis=1, keepdims=True)
        self.t = t
        psi = np.array([math.atan2(tt[0], -tt[2]) if math.hypot(tt[0], tt[2]) > 1e-4 else float("nan") for tt in t])
        # (straight down: keep the heading from before)
        for i in range(len(psi)):
            if math.isnan(psi[i]):
                psi[i] = psi[i - 1] if i > 0 else 0.0
        self.b = np.stack([np.cos(psi), np.zeros_like(psi), np.sin(psi)], axis=1)
        self.u = np.cross(self.b, self.t)
        self.u /= np.linalg.norm(self.u, axis=1, keepdims=True)
        # the first sample of each sample's section (same profile): the cross-sections are blended after a change
        self.sectie = np.zeros(n, int)
        for i in range(1, n):
            self.sectie[i] = self.sectie[i - 1] if self.prof[i] == self.prof[i - 1] else i

    def _doorsnede(self, q, i, lat, in_sectie):
        """The surface point and normal in the frame of sample i with the cross-section of sample q."""
        p, b, u = self.p[i], self.b[i], self.u[i]
        if self.prof[q] == PROFIELEN["buis"]:
            R = self.R[q]
            phi = lat * self.dmax[q]            # (in a tube dmax is the largest angle round the tube, in radians)
            c = p + u * R
            pos = c + R * (math.sin(phi) * b - math.cos(phi) * u)
            normal = (c - pos) / R
            return pos, normal
        d = lat * self.dmax[q]
        k, j = self.k[q], self.j[q]
        pos = p + b * d + u * (k * d * d + j * d)
        normal = u - b * (2 * k * d + j)
        normal = normal / np.linalg.norm(normal)
        if self.prof[q] == PROFIELEN["lucht"]:      # flying: the rider turns upright
            w = _glad(in_sectie / RECHTOP)
            normal = normal * (1 - w) + np.array([0.0, 1.0, 0.0]) * w
            normal = normal / np.linalg.norm(normal)
        return pos, normal

    def punt(self, i, lat):
        """The running surface under the ride and its normal at sample i, lateral lat (-1..1). Like GlijPad.stand: just
        after a change of profile the old cross-section glides into the new one (no jumps for a rider off the middle)."""
        i0 = int(self.sectie[i])
        in_sectie = (i - i0 + 0.5) * STAP
        pos, normal = self._doorsnede(i, i, lat, in_sectie)
        if i0 > 0 and in_sectie < OVERGANG:
            w = _glad(in_sectie / OVERGANG)
            pos_o, n_o = self._doorsnede(i0 - 1, i, lat, 1e9)
            pos = pos_o * (1 - w) + pos * w
            normal = n_o * (1 - w) + normal * w
            normal = normal / np.linalg.norm(normal)
        return pos, normal

    def rit(self):
        """The ride per tick (like GlijPad.java): s per tick, speed per tick."""
        dyds = np.gradient(self.p[:, 1]) / STAP
        s, v = 0.0, 0.25
        ss, vs = [0.0], [v]
        while s < self.lengte - 1e-6 and len(ss) < 20000:
            i = min(len(self.s) - 1, int(s / STAP))
            f = s / STAP - i
            i2 = min(len(self.s) - 1, i + 1)
            slope = dyds[i] * (1 - f) + dyds[i2] * f
            if self.prof[i] == PROFIELEN["lucht"]:
                a = -G_PHYS * slope
            else:
                a = -G_PHYS * slope + (self.vpref[i] - v) * RELAX
            v = min(V_MAX, v + a)
            if self.prof[i] != PROFIELEN["lucht"]:
                v = max(V_MIN, v)
            v = max(0.02, v)
            s = min(self.lengte, s + v)
            ss.append(s)
            vs.append(v)
        return np.array(ss), np.array(vs)


# =====================================================================================================================
# export (local coordinates: relative to the start block's bottom centre, the start block facing north)
# =====================================================================================================================
FACINGS = {"north": 0, "east": 1, "south": 2, "west": 3}


def naar_lokaal(v, facing, punt=True, oorsprong=(0, 0, 0)):
    """Template -> local (the start block faces north): the inverse of GlijPad.rotate."""
    x, y, z = v[0] - (oorsprong[0] if punt else 0), v[1] - (oorsprong[1] if punt else 0), v[2] - (oorsprong[2] if punt else 0)
    # rotate(local, facing): NORTH (x, y, z); EAST (-z, y, x); SOUTH (-x, y, -z); WEST (z, y, -x)
    if facing == "east":        # world = (-lz, ly, lx) -> lx = wz, lz = -wx
        return (z, y, -x)
    if facing == "south":
        return (-x, y, -z)
    if facing == "west":        # world = (lz, ly, -lx) -> lx = -wz, lz = wx
        return (-z, y, x)
    return (x, y, z)


def exporteer(sm, pad, start_blok, facing, pad_json):
    """Writes the path JSON for the mod. start_blok: the glijbaan_start block (template coords); facing: its facing."""
    o = (start_blok[0] + 0.5, start_blok[1], start_blok[2] + 0.5)

    def lok(v, punt=True):
        return naar_lokaal(v, facing, punt, o)

    r3 = lambda a: [round(float(x), 3) for x in a]
    out = {"id": pad.naam, "stap": STAP, "lengte": round(sm.lengte, 3),
           "g": G_PHYS, "relax": RELAX, "v_min": V_MIN, "v_max": V_MAX, "ring": RING,
           "p": [], "t": [], "b": [], "prof": sm.prof.tolist(), "k": r3(sm.k), "j": [], "dmax": r3(sm.dmax), "R": r3(sm.R),
           "vpref": r3(sm.vpref), "fx": sm.fx.tolist()}
    for i in range(len(sm.s)):
        out["p"] += r3(lok(sm.p[i]))
        out["t"] += r3(lok(sm.t[i], False))
        out["b"] += r3(lok(sm.b[i], False))
    # j: its sign is about "right" which turns with the frame, so it stays as it is
    out["j"] = r3(sm.j)
    out["eendjes"] = [[round(s, 2), round(l, 3)] for s, l in sorted(pad.eendjes) if 2 < s < sm.lengte - 2]
    ux, uy, uz, yaw = pad.uitstap
    lx, ly, lz = lok((ux, uy, uz))
    # yaw turns with the facing too (minecraft yaw: 0 = south, 90 = west, 180 = north, 270 = east)
    turn = {"north": 0, "east": -90, "south": 180, "west": 90}[facing]
    out["uitstap"] = [round(lx, 3), round(ly, 3), round(lz, 3), round((yaw + turn) % 360, 1)]
    out["merk"] = {k: round(v, 2) for k, v in pad.merk.items()}
    with open(pad_json, "w", encoding="utf-8") as f:
        json.dump(out, f, separators=(",", ":"))
    return out


# =====================================================================================================================
# the blocks of a slide (written into a make_structures.Structure)
# =====================================================================================================================
GOOT = "guhs:knuffelbad_glijgoot"
TRECHTERTEGEL = "guhs:trechtertegel"
GLIMTEGEL = "guhs:glimtegel"
ROMP = "guhs:knuffelsteen"          # the hull under a chute
GOOT_BREED = 2.3                     # half the width of an open chute's running surface
BUIS_DIK = 1.55                      # the tube's wall


def goot_blok(lagen, kleur):
    return GOOT, {"lagen": str(lagen), "kleur": kleur}


def zet_kolom(s, x, z, ys, kleur, dikte=1.6, romp=ROMP, eigen=None):
    """A column of running surface: glijgoot up to world height ys (a partial top layer, 1/8 block steps), `dikte`
    blocks deep, one block of hull under it. `eigen` collects the cells (they belong to the slide). Returns the top cell."""
    top = math.floor(ys - 1e-6)
    n = int(round((ys - top) * 8))
    if n <= 0:
        top -= 1
        n = 8
    cells = [(x, top, z)]
    s.set(x, top, z, *goot_blok(n, kleur))
    y = top - 1
    while y >= math.floor(ys - dikte):
        s.set(x, y, z, *goot_blok(8, kleur))
        cells.append((x, y, z))
        y -= 1
    if romp:
        s.set(x, y, z, romp)
        cells.append((x, y, z))
    if eigen is not None:
        eigen.update(cells)
    return top


class Claims:
    """Height-field claims per column: several surfaces can share a column (a chute under a funnel, the laps of a
    helix), claims closer than `apart` blocks in height are the same surface (the sample nearest to the column wins)."""

    def __init__(self, apart=2.5):
        self.cols = {}
        self.apart = apart

    def claim(self, x, z, along, ys, *extra):
        lst = self.cols.setdefault((x, z), [])
        for k, old in enumerate(lst):
            if abs(old[1] - ys) < self.apart:
                if along < old[0]:
                    lst[k] = (along, ys) + extra
                return
        lst.append((along, ys) + extra)

    def items(self):
        for (x, z), lst in self.cols.items():
            for c in lst:
                yield x, z, c


def _horizontaal(t):
    h = math.hypot(t[0], t[2])
    return (t[0] / h, t[2] / h) if h > 1e-6 else (0.0, -1.0)


def voxel_goot(s, sm, kleur, eigen, lucht, binnen_trechter=None, wand=TRECHTERTEGEL, clear=2.8, verboden=frozenset()):
    """The open chute parts of a slide: a U-shaped running surface (a height field per column), low walls and air above.
    binnen_trechter(x, z) -> True for columns that belong to a funnel (those are left to the funnel). Walls never go into
    `verboden` (the room of the rides)."""
    claims = Claims()
    for i in range(len(sm.s)):
        if sm.prof[i] != PROFIELEN["goot"]:
            continue
        p, t, b = sm.p[i], sm.t[i], sm.b[i]
        th = _horizontaal(t)
        hlen = math.hypot(t[0], t[2])
        slope = t[1] / hlen if hlen > 1e-3 else -20.0
        k, j = sm.k[i], sm.j[i]
        for x in range(int(math.floor(p[0] - 5)), int(math.floor(p[0] + 6))):
            for z in range(int(math.floor(p[2] - 5)), int(math.floor(p[2] + 6))):
                dx, dz = x + 0.5 - p[0], z + 0.5 - p[2]
                along = dx * th[0] + dz * th[1]
                if abs(along) > STAP * 0.62:
                    continue
                d = dx * b[0] + dz * b[2]
                if abs(d) > GOOT_BREED + 1.05:
                    continue
                y0 = p[1] + along * slope
                if binnen_trechter is not None and binnen_trechter(x + 0.5, z + 0.5, y0):
                    continue
                if abs(d) <= GOOT_BREED:
                    ys = y0 + k * d * d + j * d
                    kind = "vloer"
                else:
                    dd = math.copysign(GOOT_BREED, d)
                    ys = y0 + k * dd * dd + j * dd
                    kind = "wand"
                claims.claim(x, z, abs(along), ys, kind, y0)
    for x, z, (_a, ys, kind, y0) in claims.items():
        if kind == "vloer":
            top = zet_kolom(s, x, z, ys, kleur, eigen=eigen)
            for y in range(top + 1, int(math.floor(y0 + clear)) + 1):
                lucht.add((x, y, z))
        else:
            for y in range(int(math.floor(ys - 1.6)), math.floor(ys + 0.45) + 1):
                if (x, y, z) in verboden:
                    continue
                s.set(x, y, z, wand)
                eigen.add((x, y, z))
    return claims


def voxel_trechter(s, tr, eigen, lucht, gezicht=True, verboden=frozenset(), goot=frozenset()):
    """A funnel: a cone of running surface (a guh face looking up with its mouth - the drain - wide open: eyes with a
    shine, blushing cheeks, a two-coloured spiral), a raised rim with a gap where the chute comes in, two round ears."""
    cx, cz, R1, R0, kg_, yB, yR = tr["cx"], tr["cz"], tr["r_rand"], tr["r_gat"], tr["kegel"], tr["y_bodem"], tr["y_rand"]
    kleur_a, kleur_b = tr["kleuren"]
    in_hoek = tr["in_hoek"]
    # the face looks away from where the chute comes in: its "up" (eyes, ears) is opposite the entry
    up = in_hoek + math.pi
    ogen = [(cx + 0.55 * R1 * math.cos(up + a), cz + 0.55 * R1 * math.sin(up + a)) for a in (-0.5, 0.5)]
    wangen = [(cx + 0.62 * R1 * math.cos(up + a), cz + 0.62 * R1 * math.sin(up + a)) for a in (-1.25, 1.25)]
    for x in range(int(math.floor(cx - R1 - 3)), int(math.floor(cx + R1 + 3)) + 1):
        for z in range(int(math.floor(cz - R1 - 3)), int(math.floor(cz + R1 + 3)) + 1):
            px, pz = x + 0.5, z + 0.5
            r = math.hypot(px - cx, pz - cz)
            ang = math.atan2(pz - cz, px - cx)
            if r < R0 - 0.15:
                for y in range(int(math.floor(yB - 3)), int(math.floor(yR + 3)) + 1):
                    lucht.add((x, y, z))
                continue
            if r <= R1 + 0.6:
                ys = yB + kg_ * (max(r, R0) - R0)
                arm = (ang / math.pi + r / 3.2) % 1.0
                kleur = kleur_a if arm < 0.5 else kleur_b
                if gezicht:
                    for (ex, ez) in ogen:
                        de = math.hypot(px - ex, pz - ez)
                        if de < 1.35:
                            kleur = "donker" if de > 0.5 else "wit"
                    for (wx, wz) in wangen:
                        if math.hypot(px - wx, pz - wz) < 1.2:
                            kleur = "blos"
                top = zet_kolom(s, x, z, ys, kleur, romp=TRECHTERTEGEL, eigen=eigen)
                for y in range(top + 1, int(math.floor(yR + 3.5)) + 1):
                    lucht.add((x, y, z))
                continue
            if r <= R1 + 1.8:
                gap = abs(math.atan2(math.sin(ang - in_hoek), math.cos(ang - in_hoek)))
                if gap * R1 < 2.9 or (x, z) in goot:          # (where the chute comes in: no rim)
                    continue
                for y in range(int(math.floor(yR - 2.2)), int(math.floor(yR + 1.2)) + 1):
                    if (x, y, z) in verboden:
                        continue
                    s.set(x, y, z, TRECHTERTEGEL)
                    eigen.add((x, y, z))
    # two round guh ears standing on the rim, left and right of "up"
    rand_top = int(math.floor(yR + 1.2))
    for kant in (-1, 1):
        for a in (0.62, 0.75, 0.5, 0.9, 0.4):
            if _oor(s, cx, cz, R1, up + kant * a, rand_top, eigen, verboden, goot):
                break


def _oor(s, cx, cz, R1, hoek, rand_top, eigen, verboden, goot):
    """A round guh ear (a darker inside) standing on the funnel's rim wall (radius R1 + 0.6 .. R1 + 1.8) at this angle.
    False - and nothing placed - when it would be in the way of a ride."""
    ox, oz = cx + (R1 + 1.2) * math.cos(hoek), cz + (R1 + 1.2) * math.sin(hoek)
    tx, tz = -math.sin(hoek), math.cos(hoek)        # along the rim
    oor = {}
    for uu in np.arange(-3.2, 3.21, 0.25):
        for vv in np.arange(0, 6.01, 0.25):
            d = math.hypot(uu, vv - 3.0)
            if d > 3.1:
                continue
            blk = "minecraft:magenta_wool" if d < 1.6 else "guhs:pluisdak"
            oor[(int(math.floor(ox + tx * uu)), rand_top + 1 + int(math.floor(vv)), int(math.floor(oz + tz * uu)))] = blk
    if any(c in verboden or (c[0], c[2]) in goot for c in oor):
        return False
    for c, blk in oor.items():
        s.set(*c, blk)
        eigen.add(c)
    return True


def voxel_buis(s, sm, eigen, lucht, raam=None, ring_elke=12, verboden=frozenset(), streep=None, streep_elke=6):
    """The closed tube parts: a round shell of glimtegel (in the star tunnel every `ring_elke` blocks a bright ring),
    inside a floor of glowing glijgoot (a height field) and air. raam(i) -> True: windows of pink glass there."""
    shell, inside = {}, set()
    floor = Claims(apart=3.0)
    for i in range(len(sm.s)):
        if sm.prof[i] != PROFIELEN["buis"]:
            continue
        p, t, b, u, R = sm.p[i], sm.t[i], sm.b[i], sm.u[i], sm.R[i]
        c = p + u * R
        ring = int(sm.s[i]) % ring_elke == 0 and bool(sm.fx[i] & FX["lichtring"])
        window = raam(i) if raam else False
        rng_ = int(R + BUIS_DIK + 1)
        for x in range(int(math.floor(c[0] - rng_)), int(math.floor(c[0] + rng_)) + 1):
            for y in range(int(math.floor(c[1] - rng_)), int(math.floor(c[1] + rng_)) + 1):
                for z in range(int(math.floor(c[2] - rng_)), int(math.floor(c[2] + rng_)) + 1):
                    q = np.array((x + 0.5, y + 0.5, z + 0.5)) - c
                    along = float(q @ t)
                    if abs(along) > STAP * 0.9:        # (wide enough for no gaps on the outside of the helix's bends)
                        continue
                    rad = q - along * t
                    rho = float(np.linalg.norm(rad))
                    if rho < R:
                        inside.add((x, y, z))
                    elif rho < R + BUIS_DIK:
                        up_part = float(rad @ u) / max(rho, 1e-6)
                        kind = "ring" if ring else ("raam" if window and up_part > -0.2 else "wand")
                        if kind == "wand" and streep and rho > R + 1.05 and int(sm.s[i]) % streep_elke == 0:
                            kind = "streep"
                        if shell.get((x, y, z)) not in ("ring", "raam"):
                            shell[(x, y, z)] = kind
        # the floor: a height field in the bottom of the tube
        hlen = math.hypot(t[0], t[2])
        th = _horizontaal(t)
        slope = t[1] / hlen if hlen > 1e-3 else -20.0
        for x in range(int(math.floor(p[0] - 3)), int(math.floor(p[0] + 4))):
            for z in range(int(math.floor(p[2] - 3)), int(math.floor(p[2] + 4))):
                dx, dz = x + 0.5 - p[0], z + 0.5 - p[2]
                along = dx * th[0] + dz * th[1]
                if abs(along) > STAP * 0.62:
                    continue
                d = dx * b[0] + dz * b[2]
                if abs(d) > R * 0.72:
                    continue
                ys = p[1] + along * slope + R - math.sqrt(R * R - d * d)
                floor.claim(x, z, abs(along), ys)
    for (x, y, z), kind in shell.items():
        if (x, y, z) in inside or (x, y, z) in verboden or s.get(x, y, z) == GOOT:
            continue
        if kind == "streep":
            s.set(x, y, z, streep)
        elif kind == "ring":
            s.set(x, y, z, GLIMTEGEL, {"fel": "true"})
        elif kind == "raam":
            s.set(x, y, z, "minecraft:pink_stained_glass")
        else:
            s.set(x, y, z, GLIMTEGEL, {"fel": "false"})
        eigen.add((x, y, z))
    lucht.update(inside)
    for x, z, (_a, ys) in floor.items():
        zet_kolom(s, x, z, ys, "glim", dikte=0.9, romp=None, eigen=eigen)
    return inside


def vrije_ruimte(sm):
    """The room the rider needs: around the ring (every lateral) up to 1.8 blocks along its normal. Returns the cells.
    In a tube only the cells inside it (their middle nearer to the tube's axis than R): the tube's own wall is never
    room to clear (the ride goes round on its inside)."""
    cells = set()
    buis = PROFIELEN["buis"]
    for i in range(len(sm.s)):
        in_buis = sm.prof[i] == buis
        if in_buis:
            c = sm.p[i] + sm.u[i] * sm.R[i]
        for lat in (-1.0, -0.5, 0.0, 0.5, 1.0):
            pos, n = sm.punt(i, lat)
            for a in (0.45, 0.9, 1.35, 1.8):
                for side in (-0.35, 0.0, 0.35):
                    q = pos + n * a + sm.b[i] * side
                    cell = (int(math.floor(q[0])), int(math.floor(q[1])), int(math.floor(q[2])))
                    if in_buis:
                        m = np.array(cell, float) + 0.5 - c
                        along = float(m @ sm.t[i])
                        if float(np.linalg.norm(m - along * sm.t[i])) >= sm.R[i]:
                            continue
                    cells.add(cell)
    return cells
