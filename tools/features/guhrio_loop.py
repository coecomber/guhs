"""
Super Guhrio (bbq2) - de loper: can every level of the castle be finished, and can every big vadsmunt be taken, with a
jump that is a little WEAKER than the one the game promises? A walk without the game, on the very cells the lane builder
puts into the castle.

  Fysica   the jump as the player's own game does it, tick by tick: vanilla's walking and jumping sums (LivingEntity.aiStep
           / travelInAir / jumpFromGround) with the numbers of a level (GuhrioSpel: jump strength, gravity, speed) and the
           level's own air steering and held / cut / heavy / flutter jump (BaanSprong.java: the numbers are READ from the
           Java files, so they cannot drift apart). marge < 1 makes the jump weaker: the highest jump reaches marge times as
           high and the longest walking jump marge times as far.
  Level    the cells of one lane builder as a grid: what is solid, what is a deck you only land on from above (moving
           platforms and falling blocks: every cell their deck sweeps), what is hidden until bumped, and the pieces.
  Loper    a player: from where he stands he tries a few hundred ways of pressing the keys (walk, a held or a cut jump,
           steering late or early, braking) against the real grid, and so finds everywhere he can come to stand. He uses
           pipes and doors, bumps ?-blocks, bricks and hidden blocks, stands on / bumps / throws a knabbel at / kicks a
           shell into switches (a timed one: only as far as its 8 seconds reach), finds the egg, hatches Guhshi, rides him
           (the flutter jump) and leaves him at a hitching post, and plans ONE run per level that takes the three big
           vadsmunten and ends at the flagpole. What he does not know: creatures in his way (he never dodges, he only uses a
           Guhmba to get small again), the turning of a grill spit, running (he walks), and the timing of a platform (he
           waits for it wherever its deck can be).

controleer(h, banen) is called by guhrio.py after the castle is built. It walks the six levels with MARGE (0.9) and writes
data/guhs/guhrio_route/<level>.json, which the game test GuhrioRouteGameTests walks in the real castle (and sprong.json: the
jump's numbers, which that test measures with the game's own movement code):

  stappen     the run with the weaker jump, step by step: {"t": what, "naar": [lane, s, y] where the player then stands,
              "kracht": 0 / 1 / 2 (nothing, Superknabbel, Vuurpeper), "guhshi": riding him}. t = loop (a step), sprong (a try:
              "p" = [d, v0, W, H, a, L, rem] of Fysica.patronen; "langs" = the cells of pieces it passes; "raak" / "cel" /
              "punt" = the big vadsmunt it touches and where), lift, pijp, deur, bots, stap, knabbel, schild, krimp (a
              creature touches you), ei, uit (hatching), raak (a vadsmunt you stand in), mast, opnieuw (a new run)
  krap        [{"wat", "stappen"}]: what is out of reach with the weaker jump, each with a run of its own with the whole jump
  mist        what is out of reach with any jump (the build stops)
  te_voet / te_voet_krap   world 3 without Guhshi, with the whole / the weaker jump: what is out of reach

The build also stops when a flagpole of world 3 can't be reached without Guhshi with the whole jump (he runs off at every
touch). What needs the whole jump is printed: there the level has less than 10 % to spare. A walk of the six levels takes a
few minutes, so its result is kept in build/guhrio_loop/ until a level, the jump's numbers or this file change.

By hand (from the worktree root):   python tools/features/guhrio_loop.py [1-1 ...] [--marge 0.9] [--kaart] [--stappen]
                                    [--te-voet] (world 3 without Guhshi)  [--groot] (the player comes in big)
"""
import heapq
import json
import os
import re
import struct
import sys

import numpy as np

if __name__ == "__main__":
    sys.path.insert(0, "tools")

from features import guhrio_baan as gb  # noqa: E402

MARGE = 0.9
CACHE = os.path.join("build", "guhrio_loop")              # (not in git: a walk of the six levels takes a minute)
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhrio")
EISLOT, BROEDPLEK, NEST, WARPPIJP = "guhs:guhriow2_eislot", "guhs:guhriow2_broedplek", "guhs:guhriow2_nest", "guhs:guhriow2_warppijp"
PAAL, STRUIK = "guhs:guhriow3_parkeerpaal", "guhs:guhriow3_peperstruik"
# what a body goes through (everything else in the lane is a full block; a hidden block and a brick decide per run)
OPEN = {gb.AIR, gb.MUNT, gb.VADSMUNT, gb.VLAG, gb.MAST, gb.DEUR, gb.GUHMBA, gb.SCHILD_MIKA, gb.PLOF_MIKA, gb.HAPBLOEM, gb.PLATFORM, gb.VALBLOK,
        gb.GUHSHI_EI, gb.GUHSHI, gb.START, "guhs:guhriow1_tip", "guhs:guhriow1_geheim", "guhs:guhriow1_bloem", BROEDPLEK,
        "guhs:guhriow3_baas_plek", "guhs:guhriow3_hendel", PAAL, STRUIK}
KLEIN, GROOT, VUUR = 0, 1, 2
# pieces that do something to whoever is in their cell (GuhrioStuk.binnen), also in the middle of a jump
DOET = (gb.GUHSHI, gb.GUHSHI_EI, BROEDPLEK, PAAL, STRUIK)


def f32(x):
    """The value a Java float holds (vanilla multiplies with float constants)."""
    return struct.unpack("f", struct.pack("f", x))[0]


def _getal(tekst, naam):
    m = re.search(r"\b" + naam + r"\s*=\s*(-?[0-9]+(?:\.[0-9]+)?)", tekst)
    if not m:
        raise SystemExit(f"guhrio_loop: the number {naam} is not in the Java source it is read from")
    return float(m.group(1))


# =====================================================================================================================
# the jump
# =====================================================================================================================
class Fysica:
    """The movement of a player in a level, and the ways of pressing the keys the walker tries."""
    T = 110                       # ticks a try may take
    PX, PY = 16, 8                # the padding of a grid (columns on both sides, rows below)

    def __init__(self, marge=1.0):
        sprong = open(os.path.join(JAVA, "BaanSprong.java"), encoding="utf-8").read()
        spel = open(os.path.join(JAVA, "GuhrioSpel.java"), encoding="utf-8").read()
        self.marge = marge
        self.LUCHT_STUUR, self.LUCHT_LOOP = _getal(sprong, "LUCHT_STUUR"), _getal(sprong, "LUCHT_LOOP")
        self.STIJG, self.HOP, self.VAL, self.VAL_MAX = (_getal(sprong, "STIJG_LICHTER"), _getal(sprong, "HOP_REST"), _getal(sprong, "VAL_ERBIJ"),
                                                         _getal(sprong, "VAL_MAX"))
        self.FLADDER_TICKS, self.FLADDER, self.EIGEN = int(_getal(sprong, "FLADDER_TICKS")), _getal(sprong, "FLADDER"), _getal(sprong, "EIGEN_SPRONG")
        self.G = 0.08 + _getal(spel, "ZWAARTE_ERBIJ")
        self.KRACHT_VOL = f32(f32(0.42) + _getal(spel, "SPRONG_ERBIJ"))
        self.SNEL = f32(f32(0.1) * (1 + _getal(spel, "SNEL_ERBIJ")))
        self.SCHAAL = 1 + _getal(spel, "GROOT_ERBIJ")
        self.SCHAKEL_TICKS, self.PIJP_TICKS = int(_getal(spel, "SCHAKEL_TICKS")), int(_getal(spel, "PIJP_TICKS"))
        self.VLIEG = f32(0.02)
        self.F098, self.GROND_W, self.LUCHT_W = f32(0.98), f32(f32(0.6) * f32(0.91)), f32(0.91)
        self.BREED, self.HOOG = f32(0.6), f32(1.8)
        self.kracht, self.hfac = self.KRACHT_VOL, 1.0
        self._patronen = {}
        vol_hoog, vol_ver = self._vlak(99, 1, True)[0], self._vlak(99, 1, True)[1]
        if marge < 1.0:
            lo, hi = 0.2, self.KRACHT_VOL
            for _ in range(60):
                self.kracht = (lo + hi) / 2
                if self._vlak(99, 0, False)[0] < marge * vol_hoog:
                    lo = self.kracht
                else:
                    hi = self.kracht
            self.kracht = hi
            lo, hi = 0.3, 1.0
            for _ in range(60):
                self.hfac = (lo + hi) / 2
                if self._vlak(99, 1, True)[1] < marge * vol_ver:
                    lo = self.hfac
                else:
                    hi = self.hfac
            self.hfac = hi
        self.hoog, self.ver = self._vlak(99, 0, False)[0], self._vlak(99, 1, True)[1]
        self.tik = self._vlak(2, 0, False)[0]
        self.loop_snel = self.F098 * self.SNEL * self.hfac / (1 - self.GROND_W)      # blocks a tick, walking
        self.loop_rest = self.loop_snel * self.GROND_W                                # what is left of it between two ticks

    def maat(self, groot):
        """(half width, height) of the player's box."""
        k = self.SCHAAL if groot else 1.0
        return self.BREED * k / 2, self.HOOG * k

    def _vlak(self, houd, teken, aanloop, guhshi=False):
        """One jump on flat ground: (how high, how far, ticks in the air)."""
        g, w98 = self.G, self.F098
        x, y, vy = 0.0, 0.0, -g * w98
        vx = self.F098 * self.SNEL * self.hfac / (1 - self.GROND_W) * self.GROND_W if aanloop else 0.0
        grond, sprong, fladder, top = True, False, 0, 0.0
        for t in range(200):
            spatie = t < houd
            voor = vy
            if not grond:
                if teken:
                    cap = self.LUCHT_LOOP * self.hfac
                    if vx * teken < cap:
                        vx = min(cap, vx + self.LUCHT_STUUR * self.hfac) if teken > 0 else max(-cap, vx - self.LUCHT_STUUR * self.hfac)
                if vy > 0:
                    if sprong and spatie:
                        vy += self.STIJG
                    elif sprong:
                        vy, sprong = vy * self.HOP, False
                else:
                    sprong = False
                    if guhshi and spatie and fladder < self.FLADDER_TICKS:
                        fladder, vy = fladder + 1, self.FLADDER
                    else:
                        vy = max(self.VAL_MAX, vy - self.VAL)
            else:
                sprong, fladder = False, 0
            if vx * vx < 9.0e-6:
                vx = 0.0
            if abs(vy) < 0.003:
                vy = 0.0
            if spatie and grond:
                vy = max(self.kracht, vy)
            wrijf = self.GROND_W if grond else self.LUCHT_W
            if teken:
                vx += teken * w98 * (self.SNEL if grond else self.VLIEG) * self.hfac
            x += vx
            y += vy
            grond = False
            if y <= 0:
                grond, y, vy = vy < 0, 0.0, 0.0
            vy = (vy - g) * w98
            vx *= wrijf
            if not grond and voor <= 0 and vy > self.EIGEN and spatie:
                sprong = True
            top = max(top, y)
            if grond and t > 1:
                return top, x, t
        return top, x, 200

    # --- the ways of pressing the keys ----------------------------------------------------------------------------------
    def patronen(self, guhshi):
        """
        Every try: d the way (+1 further / -1 back), v0 a walking start (1) or from rest (0), W ticks of walking before the
        jump, H ticks the jump key is held (0: no jump, 99: until you land - on Guhshi that flutters), a ticks after the jump
        before you steer, L ticks of steering (99: until you land), rem: brake in the air afterwards.
        """
        if guhshi in self._patronen:
            return self._patronen[guhshi]
        rijen = []
        for d in (1, -1):
            for v0 in (0, 1):
                for L in (3, 6, 10, 15, 99):                    # just walking (and walking off an edge)
                    rijen.append((d, v0, 0, 0, 0, L, 0))
            for H in (2, 3, 5, 8, 99):                           # a jump from where you stand: up first, then along
                for a in (0, 4, 8, 12):
                    for L in (4, 8, 14, 99):
                        for rem in (0, 1):
                            rijen.append((d, 0, 0, H, a, L, rem))
            for v0, W in ((0, 1), (0, 2), (0, 3), (1, 0), (1, 1), (1, 2), (1, 3)):   # a jump with a few steps of run-up
                for H in (2, 4, 7, 99):
                    for L in (4, 8, 14, 99):
                        for rem in (0, 1):
                            rijen.append((d, v0, W, H, 0, L, rem))
        a = np.array(rijen, dtype=np.int64)
        p = dict(d=a[:, 0], v0=a[:, 1], W=a[:, 2], H=a[:, 3], a=a[:, 4], L=a[:, 5], rem=a[:, 6].astype(bool), rijen=rijen)
        self._patronen[guhshi] = p
        return p

    def spring(self, R, s0, y0, groot, guhshi, doelen, onder, kies=None, spoor=False, langs=()):
        """
        Every try from standing in cell (s0, y0) of grid R. doelen: cells (s, y) of big vadsmunten to watch for; onder: the
        row you fall out under; langs: cells of pieces that do something to whoever is in them. Returns (x, y, status, ticks,
        raak, door): status 0 = came to rest, 1 = fell out, 2 = took too long; raak[n][k]: try n touched doel k (as the
        player's own game sees it); door[n][j]: the tick at which try n was first in cell langs[j] the way the server sees
        it (the column of your middle, the rows of your body), 9999 = never. kies: only this try (spoor: also its path, a
        list of (x, y) per tick).
        """
        P = self.patronen(guhshi)
        if kies is None:
            d, v0, W, H, a, L, rem = P["d"], P["v0"], P["W"], P["H"], P["a"], P["L"], P["rem"]
        else:
            d, v0, W, H, a, L, rem = (np.array([v]) for v in kies)
            rem = rem.astype(bool)
        n = len(d)
        w, h = self.maat(groot)
        wv = min(w, 0.29)                                         # (the head finds a hidden block over its middle or 0.29 beside it)
        PX, PY = self.PX, self.PY
        vast, dek, verb = R.vast, R.dek, R.verb
        steun = vast | dek
        stuit = vast | verb
        kmax, rmax = vast.shape[0] - 1, vast.shape[1] - 1

        def kolom(v):
            return np.clip(np.floor(v).astype(np.int64) + PX, 0, kmax)

        def rij(v):
            return np.clip(v.astype(np.int64) + PY, 0, rmax)

        x = np.full(n, s0 + 0.5)
        y = np.full(n, float(y0))
        vx = v0 * d * self.loop_rest
        vy = np.full(n, -self.G * self.F098)
        grond = np.ones(n, bool)
        sprong = np.zeros(n, bool)
        fladder = np.zeros(n, np.int64)
        gevlogen = np.zeros(n, bool)
        klaar = np.zeros(n, bool)
        status = np.full(n, 2)
        ticks = np.zeros(n, np.int64)
        raak = np.zeros((n, len(doelen)), bool)
        door = np.full((n, len(langs)), 9999, np.int64)
        einde = W + np.maximum(np.where(H < 99, H, 0), a + np.where(L < 99, L, 0)) + 2
        pad = []
        eps = 1e-9
        for t in range(self.T):
            act = ~klaar
            if not act.any():
                break
            geland = gevlogen & grond
            stuurt = (t >= W + a) & (t < W + a + L)
            teken = np.where((t < W) | stuurt, d, 0)
            remt = rem & (t >= W + a + L) & ~grond & (np.abs(vx) > 0.04)
            teken = np.where(remt, -np.sign(vx).astype(np.int64), teken)
            teken = np.where(geland, 0, teken)
            spatie = (t >= W) & (t < W + H) & ~geland
            # --- BaanBesturing.voor: steering in the air, the held / cut / heavy / flutter jump
            voor = vy.copy()
            lucht = ~grond
            cap = self.LUCHT_LOOP * self.hfac
            st = lucht & (teken != 0) & (vx * teken < cap)
            vx = np.where(st, np.where(teken > 0, np.minimum(cap, vx + self.LUCHT_STUUR * self.hfac),
                                       np.maximum(-cap, vx - self.LUCHT_STUUR * self.hfac)), vx)
            sprong = np.where(grond, False, sprong)
            fladder = np.where(grond, 0, fladder)
            stijgt = lucht & (vy > 0)
            daalt = lucht & ~(vy > 0)
            licht = stijgt & sprong & spatie
            hop = stijgt & sprong & ~spatie
            fl = daalt & spatie & (fladder < self.FLADDER_TICKS) if guhshi else np.zeros(n, bool)
            vy = np.where(licht, vy + self.STIJG, vy)
            vy = np.where(hop, vy * self.HOP, vy)
            vy = np.where(fl, self.FLADDER, vy)
            vy = np.where(daalt & ~fl, np.maximum(self.VAL_MAX, vy - self.VAL), vy)
            fladder = fladder + fl
            sprong = sprong & ~hop & ~daalt
            # --- LivingEntity.aiStep: tiny speeds are nothing, the jump, the push of your legs
            vx = np.where(vx * vx < 9.0e-6, 0.0, vx)
            vy = np.where(np.abs(vy) < 0.003, 0.0, vy)
            vy = np.where(spatie & grond, np.maximum(self.kracht, vy), vy)
            wrijf = np.where(grond, self.GROND_W, self.LUCHT_W)
            vx = vx + teken * self.F098 * np.where(grond, self.SNEL, self.VLIEG) * self.hfac
            # --- Entity.move: up or down first ...
            yn = y + vy
            c0, c1 = kolom(x - w + eps), kolom(x + w - eps)
            k1 = np.floor(y + eps)
            raakt_grond = np.zeros(n, bool)
            for kk in (k1, k1 - 1):
                kan = (vy < 0) & ~raakt_grond & (yn < kk)
                r = rij(kk - 1)
                op = kan & (steun[c0, r] | steun[c1, r])
                yn = np.where(op, kk, yn)
                raakt_grond |= op
            kop = y + h
            kk = np.ceil(kop - eps)
            r = rij(kk)
            v0c, v1c = kolom(x - wv), kolom(x + wv)
            bots = (vy > 0) & (yn + h > kk) & (vast[c0, r] | vast[c1, r] | verb[v0c, r] | verb[v1c, r])
            yn = np.where(bots, kk - h, yn)
            vy = np.where(raakt_grond | bots, 0.0, vy)
            # --- ... then along the lane
            xn = x + vx
            r0 = np.floor(yn + eps)
            rt = np.floor(yn + h - eps)
            blok_r = np.zeros(n, bool)
            blok_l = np.zeros(n, bool)
            kr = np.ceil(x + w - eps)
            kl = np.floor(x - w + eps)
            cr, cl = np.clip(kr.astype(np.int64) + PX, 0, kmax), np.clip(kl.astype(np.int64) - 1 + PX, 0, kmax)
            for i in range(4):
                rr = r0 + i
                in_lijf = rr <= rt
                ri = rij(rr)
                blok_r |= in_lijf & vast[cr, ri]
                blok_l |= in_lijf & vast[cl, ri]
            tegen_r = (vx > 0) & (xn + w > kr) & blok_r
            tegen_l = (vx < 0) & (xn - w < kl) & blok_l
            xn = np.where(tegen_r, kr - w, xn)
            xn = np.where(tegen_l, kl + w, xn)
            vx = np.where(tegen_r | tegen_l, 0.0, vx)
            # --- travelInAir: gravity and friction; BaanBesturing.na: was that a jump of your own
            vy = (vy - self.G) * self.F098
            vx = vx * wrijf
            sprong = sprong | (~raakt_grond & (voor <= 0) & (vy > self.EIGEN) & spatie)
            x = np.where(act, xn, x)
            y = np.where(act, yn, y)
            grond = np.where(act, raakt_grond, grond)
            gevlogen |= act & ~raakt_grond
            ticks = np.where(act, t + 1, ticks)
            for k, (ds, dy) in enumerate(doelen):
                raak[:, k] |= act & (x + w > ds + 0.2) & (x - w < ds + 0.8) & (y + h > dy + 0.1) & (y < dy + 0.9)
            if langs:
                midden = np.floor(x)
                for j, (ls, ly) in enumerate(langs):
                    in_cel = act & (midden == ls) & (np.floor(y + 0.01) <= ly) & (np.floor(y + h - 0.01) >= ly)
                    door[:, j] = np.where(in_cel & (door[:, j] == 9999), t, door[:, j])
            if spoor:
                pad.append((float(x[0]), float(y[0])))
            uit = act & (y < onder)
            rust = act & ~uit & grond & (np.abs(vx) < 0.003) & (gevlogen | (t >= einde))
            status = np.where(uit, 1, np.where(rust, 0, status))
            klaar |= uit | rust
        if spoor:
            return x, y, status, ticks, raak, door, pad
        return x, y, status, ticks, raak, door


# =====================================================================================================================
# a level as a grid
# =====================================================================================================================
class Rooster:
    """The grid of one level for one state of its switches, hidden blocks and bricks (padded: see Fysica.PX / PY)."""

    def __init__(self, level, gevonden, gebroken, kanalen):
        L, H, PX, PY = level.L, level.H, Fysica.PX, Fysica.PY
        self.vast = np.zeros((L + 2 * PX, H + PY + 8), bool)
        self.verb = np.zeros_like(self.vast)
        self.vast[:PX, :] = True
        self.vast[L + PX:, :] = True
        self.vast[:, H + PY:] = True
        for (s, y), (naam, props) in level.cel.items():
            if naam == gb.ONZICHTBAAR:
                if (s, y) in gevonden:
                    self.vast[s + PX, y + PY] = True
                else:
                    self.verb[s + PX, y + PY] = True
            elif naam == gb.STEEN:
                self.vast[s + PX, y + PY] = (s, y) not in gebroken
            elif naam == gb.SCHAKELBLOK:
                self.vast[s + PX, y + PY] = (int(props["kanaal"]) in kanalen) == (props["aan"] == "true")
            elif naam not in OPEN:
                self.vast[s + PX, y + PY] = True
        self.dek = level.dek_rooster
        self.L, self.H = L, H

    def is_vast(self, s, y):
        return bool(self.vast[s + Fysica.PX, y + Fysica.PY]) if -Fysica.PX <= s < self.L + Fysica.PX and -Fysica.PY <= y < self.H + 8 else True

    def vrij(self, s, y, lijf):
        return all(not self.is_vast(s, y + k) for k in range(lijf))

    def steun(self, s, y):
        """Something to stand on under cell (s, y)?"""
        return self.is_vast(s, y - 1) or bool(self.dek[s + Fysica.PX, y - 1 + Fysica.PY])

    def staat(self, s, y, lijf):
        return 0 <= s < self.L and y >= 1 and self.vrij(s, y, lijf) and self.steun(s, y)

    def venster(self, s, y, bereik):
        a, b = s + Fysica.PX - bereik, s + Fysica.PX + bereik + 1
        return self.vast[a:b].tobytes() + self.verb[a:b].tobytes() + self.dek[a:b].tobytes()


class Level:
    """The cells of a lane builder, and what the walker needs to know of its pieces."""

    def __init__(self, baan):
        self.baan, self.wereld, self.L, self.H = baan, baan.wereld, baan.L, baan.H
        self.cel = {(s, y): (v[0], v[1] or {}) for (s, y, d), v in baan.cellen.items() if d == 0}
        hoofd = baan._hoofd
        self.banen = [dict(id="hoofd", s0=0, s1=baan.L - 1, onder=hoofd["onder"], boven=hoofd["boven"])] + [dict(b) for b in baan._bij]
        self.start = tuple(baan._start)
        PX, PY = Fysica.PX, Fysica.PY
        self.dek_rooster = np.zeros((self.L + 2 * PX, self.H + PY + 8), bool)
        self.liften = []                                         # the cells you stand in on a platform that goes up and down
        for (s, y), (naam, props) in self.cel.items():
            if naam == gb.PLATFORM:
                breed, afstand = int(props["breed"]), int(props["afstand"])
                if props["as"] == "langs":
                    cellen = [(c, y) for c in range(s, s + afstand + breed)]
                else:
                    cellen = [(c, y + k) for c in range(s, s + breed) for k in range(afstand + 1)]
                    self.liften.append([(c, r + 1) for c, r in cellen])
            elif naam == gb.VALBLOK:
                cellen = [(c, y) for c in range(s, s + int(props["breed"]))]
            else:
                continue
            for c, r in cellen:
                if 0 <= c < self.L and 0 <= r < self.H:
                    self.dek_rooster[c + PX, r + PY] = True
        self.soort = {}
        for cel, (naam, _p) in self.cel.items():
            self.soort.setdefault(naam, []).append(cel)
        for lijst in self.soort.values():
            lijst.sort()
        self.vads = {int(self.cel[c][1]["nummer"]): c for c in self.soort.get(gb.VADSMUNT, [])}
        self.broed = bool(self.soort.get(BROEDPLEK))
        self.monden = sorted((c for c in self.soort.get(gb.PIJP, []) if self.cel[c][1].get("boven") != "true"), key=self.volgorde)
        self.deuren = sorted((c for c in self.soort.get(gb.DEUR, []) if self.cel[c][1].get("half") == "lower"), key=self.volgorde)
        self._roosters = {}
        basis = self.rooster(frozenset(), frozenset(), frozenset())
        # where a walker lives: its row, from wall to wall (or ledge)
        self.hokken = []
        for naam in (gb.GUHMBA, gb.SCHILD_MIKA):
            for (s, y) in self.soort.get(naam, []):
                a = b = s
                while a - 1 >= 0 and not basis.is_vast(a - 1, y) and basis.is_vast(a - 1, y - 1):
                    a -= 1
                while b + 1 < self.L and not basis.is_vast(b + 1, y) and basis.is_vast(b + 1, y - 1):
                    b += 1
                self.hokken.append(dict(soort=naam, plek=(s, y), y=y, a=a, b=b))

    def rooster(self, gevonden, gebroken, kanalen):
        sleutel = (gevonden, gebroken, kanalen)
        if sleutel not in self._roosters:
            self._roosters[sleutel] = Rooster(self, gevonden, gebroken, kanalen)
        return self._roosters[sleutel]

    def baan_van(self, s, y):
        for i, b in enumerate(self.banen):
            if b["s0"] <= s <= b["s1"] and b["onder"] <= y <= b["boven"]:
                return i
        return -1

    def volgorde(self, cel):
        """The order in which the engine finds the pieces of a level: lane by lane, along the lane, from its bottom row up."""
        i = self.baan_van(*cel)
        return (i if i >= 0 else len(self.banen), cel[0], cel[1])

    def andere(self, lijst, cel):
        """The partner of a pipe mouth / a door: the next one of its channel, as GuhrioBlocks.PijpBlok.andere finds it."""
        kanaal = self.cel[cel][1]["kanaal"]
        zelfde = [c for c in lijst if self.cel[c][1]["kanaal"] == kanaal]
        return zelfde[(zelfde.index(cel) + 1) % len(zelfde)] if len(zelfde) > 1 else None

    def naam(self, s, y):
        return self.cel.get((s, y), (gb.AIR, {}))[0]


# =====================================================================================================================
# the walker
# =====================================================================================================================
class Toestand:
    """What a run has changed so far (per player, per run), and what the player carries from earlier levels."""
    __slots__ = ("gevonden", "gebroken", "leeg", "kanalen", "ei", "uit")

    def __init__(self, gevonden=frozenset(), gebroken=frozenset(), leeg=frozenset(), kanalen=frozenset(), ei=False, uit=False):
        self.gevonden, self.gebroken, self.leeg, self.kanalen, self.ei, self.uit = gevonden, gebroken, leeg, kanalen, ei, uit

    def met(self, **kw):
        t = Toestand(self.gevonden, self.gebroken, self.leeg, self.kanalen, self.ei, self.uit)
        for k, v in kw.items():
            setattr(t, k, v)
        return t

    def sleutel(self):
        return (self.gevonden, self.gebroken, self.leeg, self.kanalen, self.ei, self.uit)


class Loper:
    """A player in one level. A place is (lane, s, y, kracht, guhshi): standing in cell (s, y) of that lane."""
    LOOP, PIJP, DEUR, LIFT = 4, 34, 2, 60                        # ticks a step, a pipe, a door, a ride cost

    def __init__(self, level, fys, ei=False):
        self.level, self.fys, self.ei0 = level, fys, ei
        self._memo = {}
        self.sprongen = 0

    def rooster(self, W):
        return self.level.rooster(W.gevonden, W.gebroken, W.kanalen)

    def rooster_bij(self, plek, stap):
        """The grid a step of a run was planned on (kept with the step by zoek)."""
        return stap.pop("_R", None) or self.rooster(Toestand())

    @staticmethod
    def lijf(kracht):
        return 3 if kracht != KLEIN else 2

    # --- coming to stand somewhere -----------------------------------------------------------------------------------
    def kom(self, lane, s, y, kracht, guhshi, W):
        """What the pieces in the cells of your body do when you come to stand in (s, y): the place you then are, and the
        thing that changes the run there (the egg, hatching), if any."""
        wat = None
        for k in range(self.lijf(kracht)):
            kracht, guhshi, ding = self.in_stuk((s, y + k), kracht, guhshi, W)
            wat = wat or ding
        return (lane, s, y, kracht, guhshi), wat

    def in_stuk(self, cel, kracht, guhshi, W):
        """What the piece in this cell does to whoever is in it: (kracht, guhshi, the thing that changes the run or None)."""
        lv = self.level
        naam = lv.naam(*cel)
        if naam == STRUIK:
            return VUUR, guhshi, None
        if naam == PAAL:
            return kracht, False, None
        if naam == gb.GUHSHI and not guhshi and W.ei:
            # (a level with a hatching spot: nobody gets to its Guhshi spots without having walked through that spot)
            return kracht, True, None
        if naam == gb.GUHSHI_EI and not W.ei:
            return kracht, guhshi, "ei"
        if naam == BROEDPLEK and W.ei and not W.uit:
            return kracht, guhshi, "uit"
        return kracht, guhshi, None

    def sprong(self, plek, W, R):
        """
        Everywhere the tries end from this place: {(ds, y, pieces passed): (worth, ticks, try)} and
        {vadsmunt: [(worth, ticks, try, landing or None, pieces passed)]}. Pieces passed: the cells of DOET the body was in
        on the way, in order (a try that hops over a hitching post is another one than a try that walks through it).
        """
        lane, s, y, kracht, guhshi = plek
        lv, fys = self.level, self.fys
        groot = kracht != KLEIN
        b = lv.banen[lane]
        doelen = [lv.vads[k] for k in sorted(lv.vads)]
        bereik = 15 if guhshi else 8
        langs = [c for naam in DOET for c in lv.soort.get(naam, []) if abs(c[0] - s) <= bereik + 1]
        sleutel = (R.venster(s, y, bereik), y, groot, guhshi, b["onder"], b["boven"],
                   tuple((ds - s, dy) for ds, dy in doelen if abs(ds - s) <= bereik + 1), tuple((c[0] - s, c[1]) for c in langs))
        memo = self._memo.get(sleutel)
        if memo is None:
            self.sprongen += 1
            x, yy, status, ticks, raak, door = fys.spring(R, s, y, groot, guhshi, doelen, b["onder"], langs=langs)
            lijf = self.lijf(kracht)
            w = fys.maat(groot)[0]
            land, munt = {}, {}
            for n in np.nonzero(status < 2)[0]:
                n = int(n)
                reeks = tuple((langs[j][0] - s, langs[j][1]) for j in sorted(np.nonzero(door[n] < 9999)[0], key=lambda j: (door[n][j], j)))
                plek2 = None
                if status[n] == 0:
                    r = int(round(float(yy[n])))
                    c = int(np.floor(x[n]))
                    for kand in (c, int(np.floor(x[n] - w + 1e-9)), int(np.floor(x[n] + w - 1e-9))):
                        if R.staat(kand, r, lijf) and r - 1 <= b["boven"]:
                            plek2 = (kand - s, r)
                            break
                    if plek2 is not None:
                        # (a try that ends well inside its cell is worth more than a quick one that ends on the edge)
                        rand = min(float(x[n]) - np.floor(x[n]), np.ceil(x[n]) - float(x[n])) if plek2[0] + s == c else 0.0
                        waarde = int(ticks[n]) + (0 if rand >= 0.2 else 6)
                        k2 = (plek2[0], plek2[1], reeks)
                        if k2 not in land or waarde < land[k2][0]:
                            land[k2] = (waarde, int(ticks[n]), n)
                for k in np.nonzero(raak[n])[0]:
                    k = sorted(lv.vads)[int(k)]
                    waarde = int(ticks[n]) + (0 if plek2 is not None else 1000)
                    munt.setdefault(k, {})
                    k2 = (plek2, reeks)
                    if k2 not in munt[k] or waarde < munt[k][k2][0]:
                        munt[k][k2] = (waarde, int(ticks[n]), n)
            memo = (land, {k: sorted(((v[0], v[1], v[2], k2[0], k2[1]) for k2, v in d.items()), key=lambda r: (r[0], r[1], r[2]))
                           for k, d in munt.items()})
            self._memo[sleutel] = memo
        return memo

    def onderweg(self, s, reeks, kracht, guhshi, W):
        """The pieces a jump passes: (kracht, guhshi) afterwards, or None when one of them stops the run (the egg, the
        hatching spot: there you must come to stand, the level waits for you)."""
        for ds, dy in reeks:
            kracht, guhshi, wat = self.in_stuk((s + ds, dy), kracht, guhshi, W)
            if wat:
                return None
        return kracht, guhshi

    def stappen(self, plek, W, R):
        """Every move from this place: [(place, ticks, step, thing that changes the run there)] and the vadsmunten a jump
        from here touches: {nummer: (ticks, step, place or None)}."""
        lane, s, y, kracht, guhshi = plek
        lv = self.level
        lijf = self.lijf(kracht)
        uit, munten = [], {}

        def naar(l2, s2, y2, ticks, stap, kracht=kracht, guhshi=guhshi):
            if l2 < 0 or not R.staat(s2, y2, self.lijf(kracht)):
                return
            p2, wat = self.kom(l2, s2, y2, kracht, guhshi, W)
            uit.append((p2, ticks, stap, wat))

        # a step beside you
        for d in (-1, 1):
            if R.staat(s + d, y, lijf):
                naar(lane, s + d, y, self.LOOP, dict(t="loop"))
        # a walk off an edge, a jump
        land, munt = self.sprong(plek, W, R)
        rijen = self.fys.patronen(guhshi)["rijen"]
        for (ds, y2, reeks), (_waarde, ticks, n) in land.items():
            if (ds, y2) == (0, y) or (abs(ds) == 1 and y2 == y and not reeks):
                continue
            na = self.onderweg(s, reeks, kracht, guhshi, W)
            if na is None:
                continue
            stap = dict(t="sprong", p=list(rijen[n]))
            if reeks:
                stap["langs"] = [[s + a, b] for a, b in reeks]
            naar(lane, s + ds, y2, ticks, stap, na[0], na[1])
        for k, lijst in munt.items():
            for _waarde, ticks, n, plek2, reeks in lijst:
                na = self.onderweg(s, reeks, kracht, guhshi, W)
                if na is None:
                    continue
                p2 = None
                if plek2 is not None:
                    if not R.staat(s + plek2[0], plek2[1], self.lijf(na[0])):
                        continue
                    p2, wat = self.kom(lane, s + plek2[0], plek2[1], na[0], na[1], W)
                    if wat:
                        continue                                 # (lands in the egg or the hatching spot: walk there instead)
                stap = dict(t="sprong", p=list(rijen[n]), raak=k, _R=R)
                if reeks:
                    stap["langs"] = [[s + a, b] for a, b in reeks]
                munten[k] = (ticks, stap, p2)
                break
        # a platform that goes up and down: every height it comes to
        for lift in lv.liften:
            if (s, y) in lift:
                for (c, r) in lift:
                    if (c, r) != (s, y) and c == s:
                        naar(lane, c, r, self.LIFT, dict(t="lift"))
        # a pipe under your feet or in front of your nose
        onder = lv.cel.get((s, y - 1))
        if onder and onder[0] == gb.PIJP and onder[1].get("facing") == "up" and onder[1].get("ingang") == "true":
            self._pijp((s, y - 1), plek, W, R, uit)
        for d, kijkt in ((1, lv.baan.kant("terug")), (-1, lv.baan.kant("verder"))):
            mond = lv.cel.get((s + d, y))
            if mond and mond[0] == gb.PIJP and mond[1].get("facing") == kijkt and mond[1].get("ingang") == "true" and mond[1].get("boven") != "true":
                self._pijp((s + d, y), plek, W, R, uit)
        # a door you stand in
        hier = lv.cel.get((s, y))
        if hier and hier[0] == gb.DEUR and hier[1].get("half") == "lower":
            ander = lv.andere(lv.deuren, (s, y))
            if ander is not None:
                naar(lv.baan_van(*ander), ander[0], ander[1], self.DEUR, dict(t="deur", cel=[s, y]))
        return uit, munten

    def _pijp(self, mond, plek, W, R, uit):
        lv = self.level
        _lane, _s, _y, kracht, guhshi = plek
        lijf = self.lijf(kracht)
        ander = lv.andere(lv.monden, mond)
        if ander is None:
            return
        kijkt = lv.cel[ander][1]["facing"]
        if kijkt == "up":
            s2, y2 = ander[0], ander[1] + 1
        elif kijkt == "down":
            s2, y2 = ander[0], ander[1] - 2
            while y2 > 0 and not R.steun(s2, y2):
                y2 -= 1
        else:
            s2, y2 = ander[0] + (1 if kijkt == lv.baan.kant("verder") else -1), ander[1]
        l2 = lv.baan_van(s2, y2)
        if l2 >= 0 and R.staat(s2, y2, lijf):
            p2, wat = self.kom(l2, s2, y2, kracht, guhshi, W)
            uit.append((p2, self.PIJP, dict(t="pijp", mond=list(mond), uit=list(ander)), wat))

    # --- everywhere you can get to ----------------------------------------------------------------------------------------
    def bereik(self, plek, W, tijd=None):
        """Dijkstra over places: {place: (ticks, place before, step)}, the things that change the run {place: what}, and the
        vadsmunten a jump touches {nummer: (ticks, from, step, landing or None)}. tijd: only what is within so many ticks."""
        R = self.rooster(W)
        beste = {plek: (0, None, None)}
        dingen, munten = {}, {}
        rij = [(0, plek)]
        while rij:
            t, p = heapq.heappop(rij)
            if beste[p][0] < t:
                continue
            if p in dingen and p != plek:
                continue                                         # (what happens here changes the run: not walked past)
            buren, munt = self.stappen(p, W, R)
            for k, (ticks, stap, p2) in munt.items():
                if tijd is not None and t + ticks > tijd:
                    continue
                if k not in munten or (munten[k][3] is None and p2 is not None) or (t + ticks < munten[k][0] and (p2 is None) == (munten[k][3] is None)):
                    munten[k] = (t + ticks, p, stap, p2)
            for p2, ticks, stap, wat in buren:
                t2 = t + ticks
                if tijd is not None and t2 > tijd:
                    continue
                if p2 not in beste or t2 < beste[p2][0]:
                    beste[p2] = (t2, p, stap)
                    if wat:
                        dingen[p2] = wat
                    heapq.heappush(rij, (t2, p2))
        return beste, dingen, munten

    def pad(self, beste, plek):
        """The steps from the start of a bereik to this place: [(step, place)]."""
        uit = []
        while beste[plek][1] is not None:
            _t, voor, stap = beste[plek]
            uit.append((stap, plek))
            plek = voor
        return uit[::-1]

    # --- what changes a run ---------------------------------------------------------------------------------------------
    def _boven(self, s, y, kracht, R):
        """The first thing over your head and how far you must rise to bump it: (cell, rise) or None."""
        h = self.fys.maat(kracht != KLEIN)[1]
        k = int(np.ceil(y + h - 1e-6))
        while k < self.level.H:
            if R.is_vast(s, k) or self.level.naam(s, k) == gb.ONZICHTBAAR:
                return (s, k), k - (y + h)
            k += 1
        return None

    def _knabbel(self, s, y, kracht, d, R):
        """A knabbel thrown from (s, y) the way d: the switch it flies into, or None (KnabbelEntity's own sums)."""
        h = self.fys.maat(kracht != KLEIN)[1]
        x, yy, vy, stuiters = s + 0.5 + 0.5 * d, y + h * 0.55, -0.12, 0
        for _ in range(50):
            vy -= 0.06
            ny = yy + vy
            grond = False
            if vy < 0 and (R.is_vast(int(np.floor(x - 0.2)), int(np.floor(ny))) or R.is_vast(int(np.floor(x + 0.2 - 1e-9)), int(np.floor(ny)))):
                ny, grond = float(np.floor(ny)) + 1.0, True
            elif vy > 0 and (R.is_vast(int(np.floor(x - 0.2)), int(np.floor(ny + 0.4))) or R.is_vast(int(np.floor(x + 0.2 - 1e-9)), int(np.floor(ny + 0.4)))):
                ny, vy = float(np.floor(ny + 0.4)) - 0.4, -0.1
            yy = ny
            nx = x + 0.5 * d
            rand = nx + 0.2 * d
            if any(R.is_vast(int(np.floor(rand)), r) for r in {int(np.floor(yy + 1e-9)), int(np.floor(yy + 0.4 - 1e-9))}):
                tegen = (int(np.floor(x + 0.2 * d + 0.5 * d)), int(np.floor(yy + 0.2)))
                tegen = (int(np.floor(rand)), int(np.floor(yy + 0.2)))
                return tegen if self.level.naam(*tegen) == gb.SCHAKELAAR else None
            x = nx
            if grond:
                stuiters += 1
                if stuiters > 4:
                    return None
                vy = 0.22
            if yy < 0 or not 0 <= x < self.level.L:
                return None
        return None

    def dingen(self, beste, W, doel):
        """Everything within reach that changes the run or the player: [(how far from doel, place, what)]. what is a dict:
        t = bots / stap / knabbel / schild / krimp, and what comes of it."""
        lv, fys = self.level, self.fys
        R = self.rooster(W)
        uit, gezien = [], {}

        def meld(plek, wat, blok):
            sleutel = (wat["t"], tuple(wat.get("blok", ())))
            ver = abs(blok[0] - doel[0]) + (0 if lv.baan_van(*blok) == lv.baan_van(*doel) else 40)
            rij = (ver, beste[plek][0], plek, wat)
            if sleutel in gezien:
                # (the first place it can be done from; a knabbel: the place nearest the switch, where least is in its way)
                oud = uit[gezien[sleutel]]
                if wat["t"] != "knabbel" or abs(plek[1] - blok[0]) >= abs(oud[2][1] - blok[0]):
                    return
                uit[gezien[sleutel]] = rij
                return
            gezien[sleutel] = len(uit)
            uit.append(rij)

        def schakel(plek, hoe, cel, extra=None):
            props = lv.cel[cel][1]
            k = int(props["kanaal"])
            if props["soort"] == "aan" and k not in W.kanalen:
                meld(plek, dict(t=hoe, blok=list(cel), kanaal=k, **(extra or {})), cel)
            elif props["soort"] == "tijd":
                meld(plek, dict(t=hoe, blok=list(cel), kanaal=k, tijd=True, **(extra or {})), cel)
            elif props["soort"] not in ("aan", "tijd"):
                raise SystemExit(f"guhrio_loop: level {lv.wereld} has a switch of kind {props['soort']} at {cel}: the walker only knows aan and tijd")

        for plek in sorted(beste, key=lambda p: beste[p][0]):
            lane, s, y, kracht, guhshi = plek
            # over your head: a ?-block with something in it, a hidden block, a brick (when you are big), a switch
            boven = self._boven(s, y, kracht, R)
            if boven is not None and -1e-3 <= boven[1] < fys.hoog - 0.08:
                cel = boven[0]
                naam, props = lv.cel.get(cel, (gb.AIR, {}))
                inhoud = props.get("inhoud", "munt")
                krijgt = kracht
                if inhoud == "vuurpeper" or (inhoud == "superknabbel" and kracht == KLEIN):
                    krijgt = VUUR if inhoud == "vuurpeper" else GROOT
                if naam == gb.ONZICHTBAAR and cel not in W.gevonden:
                    meld(plek, dict(t="bots", blok=list(cel), wat="verborgen", kracht=krijgt), cel)
                elif naam == gb.VRAAG and cel not in W.leeg and inhoud != "munt" and krijgt != kracht:
                    meld(plek, dict(t="bots", blok=list(cel), wat="vraag", kracht=krijgt), cel)
                elif naam == gb.STEEN and kracht != KLEIN and cel not in W.gebroken and max(abs(cel[0] - doel[0]), abs(cel[1] - doel[1])) <= 2:
                    meld(plek, dict(t="bots", blok=list(cel), wat="steen", kracht=kracht), cel)
                elif naam == gb.SCHAKELAAR:
                    schakel(plek, "bots", cel)
            if lv.naam(s, y - 1) == gb.SCHAKELAAR:
                schakel(plek, "stap", (s, y - 1))
            if kracht == VUUR and not guhshi:
                for d in (1, -1):
                    cel = self._knabbel(s, y, kracht, d, R)
                    if cel is not None:
                        schakel(plek, "knabbel", cel, dict(teken=d))
            for hok in lv.hokken:
                if hok["y"] != y or not hok["a"] <= s <= hok["b"]:
                    continue
                if kracht != KLEIN or guhshi:
                    meld(plek, dict(t="krimp", blok=list(hok["plek"])), hok["plek"])
                if hok["soort"] == gb.SCHILD_MIKA and hok["b"] > hok["a"]:
                    # his shell, kicked away from you, slides to the end of his pen: into a switch there (or one over it)?
                    for d, eind in ((1, hok["b"] + 1), (-1, hok["a"] - 1)):
                        for cel in ((eind, y), (eind, y + 1)):
                            if lv.naam(*cel) == gb.SCHAKELAAR and (s < hok["b"] if d > 0 else s > hok["a"]):
                                schakel(plek, "schild", cel, dict(teken=d, mika=list(hok["plek"])))
        uit.sort(key=lambda e: (e[0], e[1]))
        return uit

    def doe(self, plek, wat, W):
        """The run and the player after this thing is done at this place: (place, W)."""
        lane, s, y, kracht, guhshi = plek
        t = wat["t"]
        if t == "krimp":
            return ((lane, s, y, kracht, False) if guhshi else (lane, s, y, KLEIN, False)), W
        if t == "bots" and wat["wat"] in ("verborgen", "vraag", "steen"):
            cel = tuple(wat["blok"])
            if wat["wat"] == "verborgen":
                W = W.met(gevonden=W.gevonden | {cel})
            elif wat["wat"] == "vraag":
                W = W.met(leeg=W.leeg | {cel})
            else:
                W = W.met(gebroken=W.gebroken | {cel})
            return (lane, s, y, wat["kracht"], guhshi), W
        if "kanaal" in wat and not wat.get("tijd"):
            return plek, W.met(kanalen=W.kanalen | {wat["kanaal"]})
        raise SystemExit(f"guhrio_loop: do not know what to do with {wat}")

    # --- a run --------------------------------------------------------------------------------------------------------------
    def zoek(self, plek, W, doel, diepte, gezien, heeft, nood=False):
        """
        The steps that bring the player from this place to doel: ("vads", nummer) or ("mast",). Returns
        (steps, place, W, vadsmunten touched on the way) or None. A step is (dict, place after it). nood: a vadsmunt that
        can only be touched on the way down into the sauce counts too (place None: you are back at your flag).
        """
        lv = self.level
        beste, dingen, munten = self.bereik(plek, W)
        # is it within reach as things are?
        if doel[0] == "vads":
            k = doel[1]
            cel = lv.vads[k]
            staand = [p for p in beste if p[1] == cel[0] and p[2] <= cel[1] < p[2] + self.lijf(p[3])]
            if staand:
                p = min(staand, key=lambda q: beste[q][0])
                return self.pad(beste, p) + [(dict(t="raak", cel=list(cel), n=k), p)], p, W, {k}
            if k in munten and munten[k][3] is not None:
                _t, van, stap, p2 = munten[k]
                return self.pad(beste, van) + [(stap, p2)], p2, W, {k}
            doelcel = cel
        else:
            masten = set(lv.soort.get(gb.MAST, []))
            bij = [p for p in beste if any((p[1], p[2] + i) in masten for i in range(self.lijf(p[3])))]
            if bij:
                p = min(bij, key=lambda q: beste[q][0])
                return self.pad(beste, p) + [(dict(t="mast"), p)], p, W, set()
            doelcel = min(masten)
        if diepte <= 0:
            return None
        # the things that change the run where you come to stand (the egg, hatching): always first
        kandidaten = [(0, beste[p][0], p, dict(t=wat)) for p, wat in dingen.items()]
        kandidaten += self.dingen(beste, W, doelcel)
        # a vadsmunt you can only touch on your way down into the sauce: better than nothing, but last
        laatste = None
        if nood and doel[0] == "vads" and doel[1] in munten:
            _t, van, stap, _geen = munten[doel[1]]
            laatste = (self.pad(beste, van) + [(stap, None)], None, W, {doel[1]})
        for _ver, _t, p, wat in kandidaten[:12]:
            pad = self.pad(beste, p)
            if wat["t"] in ("ei", "uit"):
                W2 = W.met(ei=True, kanalen=W.kanalen | self.eikanalen()) if wat["t"] == "ei" else W.met(uit=True)
                p2 = self.kom(p[0], p[1], p[2], p[3], p[4], W2)[0]
                stappen = pad + [(dict(t=wat["t"]), p2)]
            elif wat.get("tijd"):
                gevonden = self.tijdbrug(p, wat, W, doel, heeft)
                if gevonden is None:
                    continue
                extra, p2, raak = gevonden
                stappen = pad + [(dict(wat), p)] + extra
                W2 = W
                if raak:
                    return stappen, p2, W2, raak
            else:
                p2, W2 = self.doe(p, wat, W)
                stappen = pad + [(dict(wat), p2)]
            sleutel = (W2.sleutel(), p2[3:] if p2 else None, p2[0] if p2 else None, wat.get("tijd") and tuple(wat["blok"]))
            if p2 is None or sleutel in gezien:
                continue
            gezien.add(sleutel)
            verder = self.zoek(p2, W2, doel, diepte - 1, gezien, heeft, nood)
            if verder is not None:
                return stappen + verder[0], verder[1], verder[2], verder[3]
        return laatste

    def eikanalen(self):
        return frozenset(int(self.level.cel[c][1]["kanaal"]) for c in self.level.soort.get(EISLOT, []))

    def tijdbrug(self, plek, wat, W, doel, heeft):
        """
        A timed switch is hit at plek: what its 8 seconds (times the margin) bring. Returns (steps, place where you stand when
        it is over, vadsmunten touched) for the doel's vadsmunt when it can be touched in that time, else for the nearest new
        ground beyond; None when it brings nothing new.
        """
        budget = int(self.fys.SCHAKEL_TICKS * self.fys.marge) - 12
        W2 = W.met(kanalen=W.kanalen | {wat["kanaal"]})
        R0 = self.rooster(W)
        gewoon, _d, _m = self.bereik(plek, W)
        beste, _dingen, munten = self.bereik(plek, W2, tijd=budget)

        def blijft(p):
            return R0.staat(p[1], p[2], self.lijf(p[3]))

        if doel[0] == "vads" and doel[1] in munten:
            t, van, stap, p2 = munten[doel[1]]
            if p2 is not None and blijft(p2):
                return self.pad(beste, van) + [(stap, p2)], p2, {doel[1]}
            if p2 is not None:
                # get off the bridge before it is gone
                na, _d2, _m2 = self.bereik(p2, W2, tijd=budget - t)
                vast = [q for q in na if blijft(q)]
                if vast:
                    q = min(vast, key=lambda v: na[v][0])
                    return self.pad(beste, van) + [(stap, p2)] + self.pad(na, q), q, {doel[1]}
        nieuw = [p for p in beste if p not in gewoon and blijft(p)]
        if not nieuw:
            return None
        q = min(nieuw, key=lambda v: beste[v][0])
        return self.pad(beste, q), q, set()

    def run(self, vads=None, mast=True, groot=False):
        """
        One run of the level: the big vadsmunten (vads: which; None = all three) and the flagpole (mast). Returns
        dict(stappen=[...], mist=[...]): the steps and what could not be reached. A step is (dict, place after it). The run
        starts again (a new run of the level: everything the level remembers of you is as new) when the next thing can't be
        reached from where the player is. groot: the player comes in big (somebody who kept a Superknabbel... a check).
        """
        lv = self.level

        def vers(W):
            return Toestand(ei=W.ei, uit=W.uit, kanalen=self.eikanalen() if W.ei else frozenset())

        W = vers(Toestand(ei=self.ei0, uit=self.ei0))
        begin, _wat = self.kom(0, lv.start[0], lv.start[1], GROOT if groot else KLEIN, False, W)
        plek, heeft, stappen, mist = begin, set(), [], []
        doelen = [("vads", k) for k in (sorted(lv.vads) if vads is None else vads)] + ([("mast",)] if mast else [])
        while doelen:
            # the vadsmunten first (any of them), the flagpole ends the run
            kandidaten = [d for d in doelen if d[0] == "vads"] or doelen
            gevonden = None
            for nood in (False, True):
                for doel in kandidaten:
                    gevonden = self.zoek(plek, W, doel, 6, set(), heeft, nood)
                    if gevonden is not None:
                        break
                if gevonden is not None or plek != begin:
                    break                                        # (a new run first, before a last resort)
            if gevonden is None and plek != begin:
                W, plek = vers(W), begin                         # a new run of the level
                stappen.append((dict(t="opnieuw"), plek))
                continue
            if gevonden is None:
                # not even from the start of a new run: out of reach
                mist += [f"vadsmunt {d[1]} at {lv.vads[d[1]]}" if d[0] == "vads" else "the flagpole" for d in kandidaten]
                doelen = [d for d in doelen if d not in kandidaten]
                continue
            pad, plek2, W, raak = gevonden
            stappen += pad
            heeft |= raak
            doelen = [d for d in doelen if not (d[0] == "vads" and d[1] in heeft) and not (d[0] == "mast" and doel[0] == "mast")]
            if plek2 is None:
                # (touched on the way down into the sauce: back at the flag, which for the walker is a new run)
                W, plek = vers(W), begin
                stappen.append((dict(t="opnieuw", gevallen=True), plek))
            else:
                plek = plek2
        return dict(stappen=stappen, mist=mist, begin=begin, ei=W.ei, uit=W.uit, heeft=heeft)


# =====================================================================================================================
# the six levels of the castle
# =====================================================================================================================
def als_stappen(loper, run):
    """The steps of a run as the route file has them: what the game test does, and where the player then stands."""
    lv, fys = loper.level, loper.fys
    uit = []
    voor = run["begin"]
    for stap, plek in run["stappen"]:
        d = dict(stap)
        if d["t"] == "sprong" and "raak" in d and voor is not None:
            # where the jump touches the vadsmunt (the player's own game reports it from there)
            W = Toestand()
            R = loper.rooster_bij(voor, d)
            b = lv.banen[voor[0]]
            cel = lv.vads[d["raak"]]
            _x, _y, _st, _t, _raak, _door, pad = fys.spring(R, voor[1], voor[2], voor[3] != KLEIN, voor[4], [cel], b["onder"], kies=d["p"], spoor=True)
            w, h = fys.maat(voor[3] != KLEIN)
            punt = next(((x, y) for x, y in pad if x + w > cel[0] + 0.2 and x - w < cel[0] + 0.8 and y + h > cel[1] + 0.1 and y < cel[1] + 0.9), None)
            d["cel"] = list(cel)
            if punt is not None:
                d["punt"] = [round(punt[0], 3), round(punt[1], 3)]
            del W
        if plek is not None:
            d["naar"] = [plek[0], plek[1], plek[2]]
            d["kracht"], d["guhshi"] = plek[3], bool(plek[4])
        uit.append(d)
        voor = plek if plek is not None else run["begin"]
    return uit


def _hash(baan, fys):
    """What a walk depends on: the level's cells, the jump's numbers and this file."""
    import hashlib
    m = hashlib.sha1()
    m.update(open(__file__, "rb").read())
    m.update(repr((fys.KRACHT_VOL, fys.G, fys.SNEL, fys.SCHAAL, fys.LUCHT_STUUR, fys.LUCHT_LOOP, fys.STIJG, fys.HOP, fys.VAL, fys.VAL_MAX,
                   fys.FLADDER_TICKS, fys.FLADDER, fys.EIGEN, fys.SCHAKEL_TICKS, MARGE)).encode())
    m.update(repr((sorted((k, v[0], sorted((v[1] or {}).items())) for k, v in baan.cellen.items() if k[2] == 0), baan._start, baan._hoofd,
                   baan._bij, baan.kant("verder"))).encode())
    return m.hexdigest()


def loop_level(baan, vol, krap, ei):
    """
    Walks one level of the castle. With the weaker jump (krap): one run for the three big vadsmunten and the flagpole; what
    is out of reach then gets a run of its own with the whole jump (vol). Returns the route (a dict for the route file):
    stappen (the run with the weaker jump), krap ([{wat, stappen}]: what needs the whole jump), mist (out of reach with any
    jump), te_voet (world 3: the same without Guhshi, with the whole jump: what is out of reach).
    """
    loper = Loper(Level(baan), krap, ei)
    run = loper.run()
    lv = loper.level
    route = dict(level=baan.id, wereld=baan.wereld, start=list(lv.start), marge=krap.marge, banen=[b["id"] for b in lv.banen],
                 vadsmunten={str(k): list(v) for k, v in sorted(lv.vads.items())}, ei=bool(ei), stappen=als_stappen(loper, run), krap=[],
                 mist=[], te_voet=[], te_voet_krap=[])
    if run["mist"]:
        heel = Loper(Level(baan), vol, ei)
        for wat in run["mist"]:
            deel = heel.run(vads=[int(wat.split()[1])] if wat.startswith("vadsmunt") else [], mast=wat == "the flagpole")
            if deel["mist"]:
                route["mist"].append(wat)
            else:
                route["krap"].append(dict(wat=wat, stappen=als_stappen(heel, deel)))
    if ei:
        # somebody whose Guhshi ran off (he does at every touch): the level must still let him through
        route["te_voet"] = Loper(Level(baan), vol, False).run()["mist"]
        route["te_voet_krap"] = Loper(Level(baan), krap, False).run()["mist"]
    return route, run["ei"] or ei


def controleer(h, banen):
    """
    (guhrio.py) walks the six levels of the castle and writes their routes (data/guhs/guhrio_route/). Stops the build when a
    flagpole or a big vadsmunt is out of reach with the whole jump, or (world 3) a flagpole without Guhshi. What only the
    whole jump reaches is printed: there the level has less than 10 % to spare.
    """
    vol, krap = Fysica(1.0), Fysica(MARGE)
    h.w(os.path.join(h.D, "guhrio_route", "sprong.json"), dict(
        marge=MARGE, hoog=round(vol.hoog, 4), ver=round(vol.ver, 4), tik=round(vol.tik, 4), hoog_marge=round(krap.hoog, 4),
        ver_marge=round(krap.ver, 4), kracht=round(vol.kracht, 6), kracht_marge=round(krap.kracht, 6), snel_marge=round(krap.hfac, 6),
        loop=round(vol.loop_snel, 5)))
    os.makedirs(CACHE, exist_ok=True)
    fouten, ei = [], False
    for wereld, baan in banen.items():
        if wereld == "duel":
            continue
        sleutel = _hash(baan, vol) + ("-ei" if ei else "")
        pad = os.path.join(CACHE, f"{baan.id}.json")
        oud = json.load(open(pad, encoding="utf-8")) if os.path.exists(pad) else {}
        if oud.get("sleutel") == sleutel:
            route, ei2 = oud["route"], oud["ei"]
        else:
            route, ei2 = loop_level(baan, vol, krap, ei)
            json.dump(dict(sleutel=sleutel, route=route, ei=ei2), open(pad, "w", encoding="utf-8"))
        h.w(os.path.join(h.D, "guhrio_route", f"{baan.id}.json"), route)
        print(f"guhrio_loop level {wereld}: {len(route['stappen'])} steps with the jump at {int(MARGE * 100)} %"
              + (f"; ONLY with the whole jump: {[k['wat'] for k in route['krap']]}" if route["krap"] else "")
              + (f"; without Guhshi the whole jump is needed for {route['te_voet_krap']}" if route["te_voet_krap"] else ""))
        if route["mist"]:
            fouten.append(f"level {wereld}: out of reach even with the whole jump: {route['mist']}")
        if "the flagpole" in route["te_voet"]:
            fouten.append(f"level {wereld}: the flagpole is out of reach without Guhshi (he runs off at every touch)")
        ei = ei2
    if fouten:
        raise SystemExit("guhrio_loop: " + "; ".join(fouten))


# =====================================================================================================================
# by hand
# =====================================================================================================================
def _banen():
    """The six lane builders as the castle makes them (without building the castle)."""
    import make_structures as ms
    from features import guhrio_kasteel as gk

    class H:
        Byte, floats = staticmethod(ms.Byte), staticmethod(ms.floats)
        D = "src/main/resources/data/guhs"

        def lang(self, *a):
            pass

        def w(self, *a):
            pass

    H.ms = ms
    eigen = {}
    for fn in gk._slot("LEVELS"):
        eigen.update(fn)
    uit = {}
    for nr, (wereld, thema, kant) in enumerate(gk.LEVELS):
        v = gk.VLEUGEL[kant]
        oorsprong, richting = ((v["baan"], gk.Y0[thema], gk.LZ0), (0, 1)) if kant == "west" else ((v["baan"], gk.Y0[thema], gk.LZ1), (0, -1))
        baan = gb.Baanbouwer(H(), gk.level_id(wereld), wereld, nr, thema, gk.LEVEL_L, gk.SLOT_H, oorsprong, richting)
        eigen[wereld](baan)
        uit[wereld] = baan
    return uit


def kaart(loper, run):
    """The level as text with everywhere the run came to stand."""
    lv = loper.level
    R = loper.rooster(Toestand())
    stond = {(p[1], p[2]) for _s, p in run["stappen"] if p is not None}
    regels = []
    for y in range(lv.H - 1, -1, -1):
        r = ""
        for s in range(lv.L):
            naam = lv.naam(s, y)
            if (s, y) in stond:
                ch = "o"
            elif naam == gb.VADSMUNT:
                ch = "V"
            elif naam == gb.MAST:
                ch = "|"
            elif naam == gb.ONZICHTBAAR:
                ch = "?"
            elif naam == gb.SCHAKELBLOK:
                ch = "%"
            elif naam == gb.STEEN:
                ch = "x"
            elif R.is_vast(s, y):
                ch = "#"
            elif R.dek[s + Fysica.PX, y + Fysica.PY]:
                ch = "="
            else:
                ch = {gb.MUNT: ".", gb.VLAG: "f", gb.DEUR: "D", gb.GUHSHI: "G", gb.GUHSHI_EI: "e", PAAL: "p", STRUIK: "s", gb.GUHMBA: "g",
                      gb.SCHILD_MIKA: "m"}.get(naam, " ")
            r += ch
        regels.append(f"{y:2d} {r}")
    regels.append("   " + "".join(str(s // 10 % 10) for s in range(lv.L)))
    regels.append("   " + "".join(str(s % 10) for s in range(lv.L)))
    return "\n".join(regels)


if __name__ == "__main__":
    import time
    marge = float(sys.argv[sys.argv.index("--marge") + 1]) if "--marge" in sys.argv else MARGE
    args = [a for a in sys.argv[1:] if not a.startswith("--") and a != (sys.argv[sys.argv.index("--marge") + 1] if "--marge" in sys.argv else None)]
    fys = Fysica(marge)
    print(f"jump at {int(marge * 100)} %: rises {fys.hoog:.3f} (a tap of two ticks {fys.tik:.3f}), {fys.ver:.3f} far walking; "
          f"jump power {fys.kracht:.4f}, speeds x {fys.hfac:.4f}")
    for wereld, baan in _banen().items():
        if args and wereld not in args:
            continue
        ei = wereld.startswith("3") and "--te-voet" not in sys.argv
        t0 = time.time()
        loper = Loper(Level(baan), fys, ei)
        run = loper.run(groot="--groot" in sys.argv)
        print(f"level {wereld}{' (with Guhshi)' if ei else ''}: {len(run['stappen'])} steps, {loper.sprongen} places jumped from, "
              f"{time.time() - t0:.1f} s" + (f"  OUT OF REACH: {run['mist']}" if run["mist"] else ""))
        if "--stappen" in sys.argv:
            for stap in als_stappen(loper, run):
                if stap["t"] != "loop" or "--alles" in sys.argv:
                    print("   ", stap)
        if "--kaart" in sys.argv:
            print(kaart(loper, run))
