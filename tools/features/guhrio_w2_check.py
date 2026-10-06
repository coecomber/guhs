"""
Super Guhrio, world 2 (bbq2 guhrio-w2) - can the levels be played? A check without the game, run by guhrio_w2.py right after
a level is built (it stops the generators when a level cannot be finished).

The lane of a level is a grid of cells (s along the lane, y up). The check walks it like a careful player would, with jump
numbers well INSIDE what the engine promises (reports/slice_guhrio-engine.md 1.3: a held jump rises 3.3 blocks and covers
4.4 walking; the prototype measured 3.18 / 3.7), so a level that passes is still playable when the real feel turns out a
little heavier:

  a step up            at most 2 rows (the engine: 3)
  a jump               lands at most 4 cells away on the same height or lower (a gap of 3), 3 cells one row up, 2 cells
                       two rows up; its path (up, over at the top, down) must be free for the whole body
  a moving platform    every cell its deck sweeps is a place to stand (you wait for it); a falling block too
  a pipe / a door      takes you to its partner, as the engine pairs them
  a switch             counts as hit when you can stand on it, bump it from below, or kick a Schild-Mika's shell into it;
                       its blocks then are what they are with the channel on (the levels only switch things ON)
  the egg lock         its channel is on once the egg can be reached

It then demands: the flagpole, every flag, every coin, the three big vadsmunten, the egg / nest / Guhshi and every warp
pipe can be reached; no flag is more than MAX_VLAG cells from the one before it; nobody gets stuck (from every place you
can stand, the flagpole can still be reached); a player made big by a Superknabbel (three rows of body) can finish the
level too; every walker has a pen.

Guhshi's flutter jump, running jumps and hidden blocks are never needed: nothing here knows them.
"""
from collections import deque

from features import guhrio_baan as gb

EISLOT, BROEDPLEK, NEST, WARPPIJP = "guhs:guhriow2_eislot", "guhs:guhriow2_broedplek", "guhs:guhriow2_nest", "guhs:guhriow2_warppijp"
# blocks you walk through
OPEN = {gb.AIR, gb.MUNT, gb.VADSMUNT, gb.VLAG, gb.MAST, gb.DEUR, gb.GUHMBA, gb.SCHILD_MIKA, gb.PLOF_MIKA, gb.HAPBLOEM, gb.PLATFORM, gb.VALBLOK,
        gb.GUHSHI_EI, gb.GUHSHI, gb.ONZICHTBAAR, gb.START, BROEDPLEK}
MAX_VLAG = 36
STAP_OP = 2
VER = {2: 2, 1: 3}              # rows up -> cells far; level or down: 4


class Speelbaar:
    """One level's grid and what can be reached in it. kanalen: the switch channels that are on."""

    def __init__(self, baan):
        self.baan = baan
        self.cel = {(s, y): (v[0], v[1] or {}) for (s, y, d), v in baan.cellen.items() if d == 0}
        self.L, self.H = baan.L, baan.H
        self.dek = set()                                         # cells whose TOP a deck can be at (a place to stand above them)
        self.rit = []                                            # groups of deck cells that one platform connects
        for (s, y), (naam, props) in self.cel.items():
            if naam == gb.PLATFORM:
                breed, afstand = int(props["breed"]), int(props["afstand"])
                if props["as"] == "langs":
                    groep = [(c, y) for c in range(s, s + afstand + breed)]
                else:
                    groep = [(c, y + k) for c in range(s, s + breed) for k in range(afstand + 1)]
                self.rit.append(groep)
                self.dek.update(groep)
            elif naam == gb.VALBLOK:
                self.dek.update((c, y) for c in range(s, s + int(props["breed"])))
        self.kanalen = set()

    # --- the grid ---------------------------------------------------------------------------------------------------------
    def naam(self, s, y):
        return self.cel.get((s, y), (gb.AIR, {}))[0]

    def vast(self, s, y):
        if not (0 <= s < self.L) or y >= self.H:
            return True
        if y < 0:
            return False
        naam, props = self.cel.get((s, y), (gb.AIR, {}))
        if naam == gb.SCHAKELBLOK:
            return (int(props["kanaal"]) in self.kanalen) == (props["aan"] == "true")
        return naam not in OPEN

    def vrij(self, s, y, hoog):
        """Can a body of hoog rows be in column s with its feet in row y?"""
        return y >= 0 and all(not self.vast(s, y + k) for k in range(hoog))

    def staat(self, s, y, hoog):
        return self.vrij(s, y, hoog) and (self.vast(s, y - 1) or (s, y - 1) in self.dek)

    def val(self, s, y, hoog):
        """Dropping in column s with the feet in row y: the row you land in, or None (into the sauce / blocked)."""
        if not self.vrij(s, y, hoog):
            return None
        while y >= 0:
            if self.vast(s, y - 1) or (s, y - 1) in self.dek:
                return y
            y -= 1
        return None

    # --- what you can do from a place ----------------------------------------------------------------------------------------
    def stappen(self, s, y, hoog):
        """(place, cells the body passes) for everything a careful player can do from (s, y)."""
        uit = []
        for teken in (-1, 1):
            c = s + teken
            if self.staat(c, y, hoog):
                uit.append(((c, y), [(c, y + k) for k in range(hoog)]))
            elif self.vrij(c, y, hoog):                         # walk off the edge
                land = self.val(c, y, hoog)
                if land is not None:
                    uit.append(((c, land), [(c, r) for r in range(land, y + hoog)]))
        # jumps: up to the top row a, over, down to the landing
        for top in range(y, y + 4):
            if not all(not self.vast(s, r) for r in range(y, top + hoog)):
                break
            for teken in (-1, 1):
                pad = [(s, r) for r in range(y, top + hoog)]
                for ver in range(1, 5):
                    c = s + teken * ver
                    if not self.vrij(c, top, hoog):
                        break
                    kolom = [(c, top + k) for k in range(hoog)]
                    land = self.val(c, top, hoog)
                    if land is not None and (top > y or ver == 1):       # (a jump over a gap needs room to rise)
                        op = land - y
                        if op <= STAP_OP and ver <= VER.get(op, 4):
                            uit.append(((c, land), pad + kolom + [(c, r) for r in range(land, top + hoog)]))
                    pad = pad + kolom
        # riding a platform
        for groep in self.rit:
            if (s, y - 1) in groep:
                for (c, r) in groep:
                    if self.vrij(c, r + 1, hoog):
                        uit.append(((c, r + 1), [(c, r + 1 + k) for k in range(hoog)]))
        # a pipe under your feet, a sideways pipe in front of you, a door you stand in
        onder = self.cel.get((s, y - 1))
        if onder and onder[0] == gb.PIJP and onder[1].get("facing") == "up" and onder[1].get("ingang") == "true":
            uit += self.uit_pijp(s, y - 1, hoog)
        for teken, kijkt in ((1, self.baan.kant("terug")), (-1, self.baan.kant("verder"))):
            mond = self.cel.get((s + teken, y))
            if mond and mond[0] == gb.PIJP and mond[1].get("facing") == kijkt and mond[1].get("ingang") == "true" and mond[1].get("boven") != "true":
                uit += self.uit_pijp(s + teken, y, hoog)
        hier = self.cel.get((s, y))
        if hier and hier[0] == gb.DEUR and hier[1].get("half") == "lower":
            for (c, r), (naam, props) in self.cel.items():
                if naam == gb.DEUR and props.get("half") == "lower" and props["kanaal"] == hier[1]["kanaal"] and (c, r) != (s, y) \
                        and self.staat(c, r, hoog):
                    uit.append(((c, r), [(c, r + k) for k in range(hoog)]))
        return uit

    def uit_pijp(self, s, y, hoog):
        """Where the pipe whose mouth is at (s, y) lets you out."""
        kanaal = self.cel[(s, y)][1]["kanaal"]
        monden = sorted((k for k, v in self.cel.items() if v[0] == gb.PIJP and v[1].get("boven") != "true" and v[1]["kanaal"] == kanaal),
                        key=lambda k: self.volgorde(k))
        if len(monden) < 2:
            return []
        c, r = monden[(monden.index((s, y)) + 1) % len(monden)]
        kijkt = self.cel[(c, r)][1]["facing"]
        if kijkt == "up":
            plek = (c, r + 1) if self.vrij(c, r + 1, hoog) else None
        elif kijkt == "down":
            land = self.val(c, r - hoog, hoog)
            plek = None if land is None else (c, land)
        else:
            stap = -1 if kijkt == self.baan.kant("terug") else 1
            plek = (c + stap, r) if self.staat(c + stap, r, hoog) else None
        return [] if plek is None else [(plek, [(plek[0], plek[1] + k) for k in range(hoog)])]

    def volgorde(self, cel):
        """The engine pairs pipes in the order it finds them: lane by lane, along the lane, from the lane's bottom row up."""
        s, y = cel
        lanen = [(0, self.L - 1, self.baan._hoofd["onder"], self.baan._hoofd["boven"])] + \
                [(b["s0"], b["s1"], b["onder"], b["boven"]) for b in self.baan._bij]
        for i, (s0, s1, onder, boven) in enumerate(lanen):
            if s0 <= s <= s1 and onder <= y <= boven:
                return (i, s, y)
        return (len(lanen), s, y)

    def bereik(self, van, hoog, graaf=None):
        """Every place that can be reached from van, and every cell a body passes on the way. graaf: a dict that gets, per
        place, the places one move further and whether that move passes one of the cells in graaf["doel"]."""
        gezien, geraakt = {van}, {(van[0], van[1] + k) for k in range(hoog)}
        rij = deque([van])
        while rij:
            s, y = rij.popleft()
            for plek, pad in self.stappen(s, y, hoog):
                geraakt.update(pad)
                if graaf is not None:
                    graaf.setdefault("terug", {}).setdefault(plek, set()).add((s, y))
                    if graaf["doel"].intersection(pad):
                        graaf.setdefault("raakt", set()).add((s, y))
                if plek not in gezien:
                    gezien.add(plek)
                    rij.append(plek)
        return gezien, geraakt

    # --- switches ---------------------------------------------------------------------------------------------------------
    def schakel(self, gezien, geraakt):
        """Switch channels that can be turned on with what is reachable now. True when something changed."""
        nieuw = set()
        for (s, y), (naam, props) in self.cel.items():
            if naam == gb.SCHAKELAAR:
                k = int(props["kanaal"])
                if k in self.kanalen or props["soort"] == "uit":
                    continue
                op = (s, y + 1) in gezien
                onder = any((s, r) in gezien and all(not self.vast(s, q) for q in range(r, y)) for r in range(y - 4, y - 1))
                if op or onder or self.schild(s, y, gezien):
                    nieuw.add(k)
            elif naam == EISLOT:
                ei = [c for c, v in self.cel.items() if v[0] == gb.GUHSHI_EI]
                if int(props["kanaal"]) not in self.kanalen and any(c in geraakt for c in ei):
                    nieuw.add(int(props["kanaal"]))
        self.kanalen |= nieuw
        return bool(nieuw)

    def schild(self, s, y, gezien):
        """Can a shell be kicked into the switch at (s, y)? A Schild-Mika lives in the same row, with a free run to it."""
        for teken in (-1, 1):
            c = s + teken
            while 0 <= c < self.L and not self.vast(c, y) and (self.vast(c, y - 1) or (c, y - 1) in self.dek):
                if self.naam(c, y) == gb.SCHILD_MIKA and any((q, y) in gezien for q in range(c - 3, c + 4)):
                    return True
                c += teken
        return False

    def alles(self, van, hoog):
        while True:
            gezien, geraakt = self.bereik(van, hoog)
            if not self.schakel(gezien, geraakt):
                return gezien, geraakt

    def hok(self, s, y):
        """The cells a walker that starts at (s, y) walks: until a wall or a ledge on both sides."""
        cellen = [s]
        for teken in (-1, 1):
            c = s + teken
            while 0 <= c < self.L and not self.vast(c, y) and (self.vast(c, y - 1)):
                cellen.append(c)
                c += teken
        return min(cellen), max(cellen)


def controleer(baan, eisen):
    """
    Raises SystemExit (through baan.fout) when the level cannot be played. eisen: dict(ei=bool, warp=n) - what the level
    must also let you reach. Returns a few numbers for the build log.
    """
    sp = Speelbaar(baan)
    start = baan._start
    gezien, geraakt = sp.alles(start, 2)
    stukken = {}
    for cel, (naam, props) in sp.cel.items():
        stukken.setdefault(naam, []).append(cel)
    fouten = []

    def moet(naam, wat, cellen=None):
        mist = [c for c in (stukken.get(naam, []) if cellen is None else cellen) if c not in geraakt]
        if mist:
            fouten.append(f"{wat} at {sorted(mist)} can't be reached")

    if not any(c in geraakt for c in stukken.get(gb.MAST, [])):
        fouten.append("the flagpole can't be reached")
    moet(gb.VLAG, "the flag")
    moet(gb.MUNT, "the coin")
    moet(gb.VADSMUNT, "the big vadsmunt")
    for naam, wat in ((gb.VRAAG, "the ?-block"), (gb.STEEN, "the brick")):
        mist = [(s, y) for (s, y) in stukken.get(naam, []) if not any((s, r) in gezien and all(not sp.vast(s, q) for q in range(r, y))
                                                                      for r in range(y - 5, y - 1))]
        if naam == gb.VRAAG and mist:
            fouten.append(f"{wat} at {sorted(mist)} can't be bumped from below")
    if eisen.get("ei"):
        if not stukken.get(gb.GUHSHI_EI) or not stukken.get(NEST) or not stukken.get(BROEDPLEK) or not stukken.get(gb.GUHSHI) or not stukken.get(EISLOT):
            fouten.append("the level needs Guhshi's egg, the egg lock, the hatching spot, the nest and a Guhshi spot")
        moet(gb.GUHSHI_EI, "Guhshi's egg")
        moet(gb.GUHSHI, "Guhshi's spot")
        if not any(c in geraakt for c in stukken.get(BROEDPLEK, [])):
            fouten.append("the hatching spot can't be reached")
        # nobody reaches the nest (or the flagpole) without the egg: with the egg's cell walled up and the lock's channel
        # off, both must be out of reach
        zonder = Speelbaar(baan)
        for c in stukken.get(gb.GUHSHI_EI, []):
            zonder.cel[c] = ("minecraft:bedrock", {})
        _dicht, dicht_geraakt = zonder.bereik(start, 2)
        for wat, cellen in (("Guhshi's spot", stukken.get(gb.GUHSHI, [])), ("the flagpole", stukken.get(gb.MAST, []))):
            if any(c in dicht_geraakt for c in cellen):
                fouten.append(f"{wat} can be reached without the egg")
        # ... and nobody passes the nest without walking through the hatching spot
        zonder_plek = Speelbaar(baan)
        zonder_plek.kanalen = set(sp.kanalen)
        for c in stukken.get(BROEDPLEK, []):
            zonder_plek.cel[c] = ("minecraft:bedrock", {})
        om, om_geraakt = zonder_plek.bereik(start, 2)
        if any(c in om_geraakt for c in stukken.get(gb.GUHSHI, [])):
            fouten.append("Guhshi's spot can be reached around the hatching spot")
    warp = [c for c in stukken.get(WARPPIJP, []) if (c[0], c[1] + 1) in gezien]
    if len(warp) != eisen.get("warp", 0) or len(stukken.get(WARPPIJP, [])) != eisen.get("warp", 0):
        fouten.append(f"{eisen.get('warp', 0)} warp pipes you can stand on are wanted, found {len(warp)} of {len(stukken.get(WARPPIJP, []))}")
    for cel in stukken.get(gb.PIJP, []):
        props = sp.cel[cel][1]
        if props.get("facing") == "up" and props.get("ingang") == "true" and (cel[0], cel[1] + 1) not in gezien:
            fouten.append(f"nobody can stand on the pipe at {cel}")
    # flags: never far apart along the main lane
    hoofd = sorted(s for (s, y) in stukken.get(gb.VLAG, []) if baan._hoofd["onder"] <= y <= baan._hoofd["boven"])
    mast = min(s for (s, y) in stukken.get(gb.MAST, [(baan.L - 1, 0)]))
    punten = [start[0]] + hoofd + [mast]
    for a, b in zip(punten, punten[1:]):
        if b - a > MAX_VLAG:
            fouten.append(f"no flag between s = {a} and s = {b} ({b - a} cells: at most {MAX_VLAG})")
    # nobody gets stuck: from everywhere the flagpole can still be reached (switches stay as they are: on)
    mastcellen = set(stukken.get(gb.MAST, []))
    graaf = {"doel": mastcellen}
    sp.bereik(start, 2, graaf)
    goed = set(graaf.get("raakt", ()))
    rij = deque(goed)
    while rij:
        for voor in graaf.get("terug", {}).get(rij.popleft(), ()):
            if voor not in goed:
                goed.add(voor)
                rij.append(voor)
    vast = sorted(p for p in gezien if p not in goed and not (p[0], p[1]) in mastcellen)
    if vast:
        fouten.append(f"a player can get stuck (no way to the flagpole) at {vast[:12]}")
    # a big player (three rows of body) finishes the level too, from the start and from every flag
    groot = Speelbaar(baan)
    groot.kanalen = set(sp.kanalen)
    for van in [start] + sorted(c for c in stukken.get(gb.VLAG, []) if c in gezien):
        if not groot.staat(van[0], van[1], 3):
            fouten.append(f"a big player does not fit at {van}")
            continue
        _g, groot_geraakt = groot.bereik(van, 3)
        if not groot_geraakt & mastcellen:
            fouten.append(f"a big player can't reach the flagpole from {van}")
    # walkers need a pen
    hokken = {}
    for naam in (gb.GUHMBA, gb.SCHILD_MIKA):
        for (s, y) in stukken.get(naam, []):
            a, b = sp.hok(s, y)
            hokken[(s, y)] = (a, b)
            if b - a < 2 or b - a > 16:
                fouten.append(f"the walker at {(s, y)} has a pen of {b - a + 1} cells ({a}..{b}): 3 to 17 are wanted")
            if a <= start[0] <= b and y == start[1]:
                fouten.append(f"the walker at {(s, y)} walks over the start")
    if fouten:
        baan.fout("not playable:\n    " + "\n    ".join(fouten))
    return dict(plekken=len(gezien), munten=len(stukken.get(gb.MUNT, [])), vlaggen=len(hoofd), kanalen=sorted(sp.kanalen), hokken=hokken)
