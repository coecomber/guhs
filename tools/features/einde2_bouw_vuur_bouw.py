"""
einde2, slice bouw_vuur: the build of the Vuurnatie-hoofdstad (DESIGN 5, chapter 4), one guhs:leegte_bouw of 256 x 118 x 256.

A floating slab of scorched kaaskorst. In the middle the big crater with the capital in it; around it, clockwise from the
south: the harbour (where you arrive), Het Kokend Rotsblok (a prison on an island in a lake of boiling kaassaus, in a crater
of its own), the camp of the Witte Lotus, the bite in the rim where the Leeuwschildpad-guh surfaces (an overlay), the trail
along the north rim, the duel arena (rock pillars in a crater lake), the ruin of the Zonnekrijgers with its long stair and
the two dragon caves, and Sintelstrand with the beach house and the open-air theatre.

    einde2_bouw_vuur_grond.py    the ground (a height field turned into a hollow crust)
    einde2_bouw_vuur_bouw.py     this file: the layout, the builder with its helpers, the landscape, the self-check
    einde2_bouw_vuur_stad.py     inside the big crater: gate, city, school, dance cave, palace, pond, bunker
    einde2_bouw_vuur_buiten.py   everything around it

Nothing here hurts: what looks like lava is guhs:einde2_bouw_vuur_lauwe_lava (a solid, lukewarm block), what looks like
fire is guhs:einde2_bouw_vuur_lauwe_vlam, and the lake boils with kaassaus. No story logic and no characters: every spot a
later phase needs is a marker (build contract 6.7).
"""
from collections import deque

import numpy as np

import features.einde2_kit as kit
from features import einde2_bouw_vuur_grond as gr
from features import guheinde_bouw as gb
from features.einde2_bouw_vuur_grond import F, G, W, D, H  # noqa: F401 (G: for the modules that build on this one)

NAAM = "vuurnatie_hoofdstad"
SEED = 21401250
Y_WERELD = 134                      # the world y of template y 0: the build fills the whole high band but two layers
PREFIX = "sign.guhs.einde2_bouw_vuur"

LAVA = "guhs:einde2_bouw_vuur_lauwe_lava"
VLAM = "guhs:einde2_bouw_vuur_lauwe_vlam"
EMBLEEM = "guhs:einde2_bouw_vuur_embleem"
PORTRET = "guhs:einde2_bouw_vuur_portret_mikai"
ZONNESTEEN = "guhs:einde2_bouw_vuur_zonnesteen"
LOTUS = "guhs:einde2_bouw_vuur_lotustegel"
LICHT = {LAVA: 15, VLAM: 15, "guhs:gloeikool": 15, "guhs:rookgat": 5, "guhs:smeulkooltjes": 7, "guhs:uienlicht": 15,
         "guhs:disco_bal": 15, "guhs:disco_dansvloer": 10}

# --- the layout (template coordinates) ---
C = (128, 96)                       # the big crater: the capital
RV, RK, RF, KAM = 50, 61, 79, 78    # its floor radius, crest radius, foot radius and crest height
K = (198, 194)                      # Het Kokend Rotsblok
KRV, KRK, KRF, K_KAM = 34, 40, 50, 60
MEER, MEER_VLOER = 49, 45           # the level of the boiling lake and its floor
EILAND_R = 15
A = (56, 46)                        # the duel arena
AR, A_WATER, A_VLOER = 25, 43, 39
HAP = (236, 18, 46)                 # the bite in the rim where the Leeuwschildpad-guh surfaces
TERRAS = (34, 108, 78)              # the round terrace of the Drakendans (x, z, y)
TRAP_Z = (150, 116)                 # the long stair runs along x = 34 from z 150 (the ground) up to z 116
TRAP_NOORD = (65, 99)               # the stair from the arena shore (z 65) up to the terrace (z 99)
STATION = (159, 194, 60)            # the upper station of the gondola, on the rim of Het Kokend Rotsblok
STEIGER = (170, 211)                # the little dock at the foot of the inner wall
KADE = (228, 66)                    # the quay where the Leeuwschildpad-guh lays its chin
LAAG = (206, 30, 6)                 # the origin of the overlay laag_leeuwschildpad
PILAREN = [                         # the rock pillars of the arena: (dx, dz from A, radius, top y)
    (-2, 15, 2.6, 47), (-9, 9, 2.2, 49), (-13, 2, 2.6, 52), (-15, -7, 2.0, 55), (-8, -13, 2.2, 58), (0, -17, 2.0, 61),
    (8, -13, 2.2, 63), (14, -6, 2.6, 57), (15, 3, 2.0, 60), (10, 11, 2.2, 53), (4, 7, 1.8, 50), (0, 0, 4.2, 54), (-6, -3, 1.8, 51)]
HAVEN_WATER, HAVEN_VLOER = 43, 39

KOOL, STENEN, IJZER, GLOEI = gr.KOOL, gr.STENEN, gr.IJZER, gr.GLOEI
STENEN_TRAP, STENEN_PLAAT, STENEN_MUUR = STENEN + "_trap", STENEN + "_plaat", STENEN + "_muur"
BARST, BEITEL = "guhs:gebarsten_houtskoolsteen_stenen", "guhs:gebeitelde_houtskoolsteen_stenen"
GLAD = "guhs:gepolijst_roosterijzer"
ROOD, ROOD_TRAP, ROOD_PLAAT, ROOD_MUUR = "red_nether_bricks", "red_nether_brick_stairs", "red_nether_brick_slab", "red_nether_brick_wall"
GOUD = "gold_block"
ZWART, ZWART_TRAP, ZWART_PLAAT, ZWART_MUUR = "polished_blackstone_bricks", "polished_blackstone_brick_stairs", "polished_blackstone_brick_slab", \
    "polished_blackstone_brick_wall"
HOUT, HOUT_TRAP, HOUT_PLAAT, HOUT_HEK, BALK = "mangrove_planks", "mangrove_stairs", "mangrove_slab", "mangrove_fence", "stripped_mangrove_log"
MUUR = "cut_red_sandstone"
SAUS = "guhs:kaas_saus"

FACINGS = ("north", "east", "south", "west")
LINKS = {"north": "west", "west": "south", "south": "east", "east": "north"}       # (your left hand when you look that way)
RECHTS = {v: k for k, v in LINKS.items()}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}


class Vuur(kit.Bouw):
    """The build in the making: kit.Bouw with the things this capital is made of."""

    def __init__(self, h):
        super().__init__(h, NAAM, (W, H, D), SEED, prefix=PREFIX)
        self.grond = None
        self.kabel = set()               # the gondola line and its cabins: they hang, on purpose
        self.geen_licht = set()          # (x, z) columns where the light pass must not touch the floor (a marker stands there)
        self.dun |= {VLAM, "guhs:smeulkooltjes", "guhs:rookgat", "guhs:vlaggetjes", "guhs:theepotje", "minecraft:flower_pot"}

    def is_dun(self, n):
        # (the kit's list of thin things has "minecraft:light", the invisible light block; as a part of a name that also
        # catches every light_blue_* and light_gray_* block: those are as solid as their colour says)
        if n.startswith("minecraft:light_"):
            return "carpet" in n
        return super().is_dun(n)

    # --- small things -------------------------------------------------------------------------------------------------------
    def kies(self, tabel):
        return gb.pick(self.rng, tabel)

    def naam(self, x, y, z):
        bl = self.s.blocks.get((x, y, z))
        return bl[0] if bl else None

    def lucht(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, gb.AIR)

    def mix(self, x0, y0, z0, x1, y1, z1, tabel):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, self.kies(tabel))

    def trap(self, x, y, z, name, facing, half="bottom", shape="straight"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"})

    def plaat(self, x, y, z, name, t="bottom"):
        self.set(x, y, z, name, gb.slab(t))

    def lantaarn(self, x, y, z, hangend=True):
        self.set(x, y, z, "lantern", gb.lamp(hangend))

    def ketting(self, x, y0, y1, z):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            self.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})

    def hanglamp(self, x, y_plafond, z, lang=1):
        """A lantern on `lang` links of chain under the ceiling block at y_plafond."""
        for k in range(1, lang):
            self.set(x, y_plafond - k, z, "chain", {"axis": "y", "waterlogged": "false"})
        self.lantaarn(x, y_plafond - lang, z, True)

    def vlam(self, x, y, z):
        self.set(x, y, z, VLAM)

    def vuurpot(self, x, y, z, hoog=1, paal=ZWART_MUUR):
        """A brazier: a post of `hoog` wall blocks, a bowl of glowing coal on it and a lukewarm flame in the bowl."""
        for k in range(hoog):
            self.set(x, y + k, z, paal)
        self.set(x, y + hoog, z, GLOEI)
        self.vlam(x, y + hoog + 1, z)

    def deur(self, x, y, z, facing, hout="mangrove", scharnier="left", open_=False):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, f"{hout}_door", {"facing": facing, "half": half, "hinge": scharnier, "open": "true" if open_ else "false",
                                                    "powered": "false"})

    def luik(self, x, y, z, facing, hout="mangrove", half="bottom", open_=False):
        self.set(x, y, z, f"{hout}_trapdoor", {"facing": facing, "half": half, "open": "true" if open_ else "false", "powered": "false",
                                               "waterlogged": "false"})

    def slaapzak(self, x, y, z, facing):
        """Where somebody sleeps: a guh sleeping bag, its hood towards `facing`. NEVER a bed: in the Guheinde a bed explodes
        when you click it (the dimension's bed rule), a sleeping bag only says that you cannot sleep here."""
        self.set(x, y, z, "guhs:guh_slaapzak", {"facing": facing, "occupied": "false"})

    def knop(self, x, y, z, facing, face="wall", soort="polished_blackstone_button"):
        self.set(x, y, z, soort, {"face": face, "facing": facing, "powered": "false"})

    def hendel(self, x, y, z, facing, face="floor"):
        self.set(x, y, z, "lever", {"face": face, "facing": facing, "powered": "false"})

    def tapijt(self, x0, y, z0, x1, z1, kleur="red"):
        self.fill(x0, y, z0, x1, y, z1, f"{kleur}_carpet")

    def pot(self, x, y, z, plant="guhs:potted_knabbelroos"):
        self.set(x, y, z, plant)

    def kist(self, x, y, z, facing, tabel):
        self.chest(x, y, z, facing, f"guhs:chests/einde2_bouw_vuur_{tabel}")

    def ton(self, x, y, z):
        self.set(x, y, z, "barrel", {"facing": "up", "open": "false"})

    def stoel(self, x, y, z, facing, name=HOUT_TRAP):
        """A seat: a stair whose back is at the far side of `facing` (you sit looking towards facing)."""
        self.trap(x, y, z, name, gb.OPP[facing])

    def bank_rij(self, x0, z0, x1, z1, y, facing, name=HOUT_TRAP):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.stoel(x, y, z, facing, name)

    def boekenkast(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "bookshelf")

    # --- roofs --------------------------------------------------------------------------------------------------------------
    def dakring(self, x0, z0, x1, z1, y, trap, half="bottom"):
        """One ring of stairs around the rectangle (x0..x1, z0..z1) at y, rising inwards, with the four corner shapes."""
        if x1 - x0 < 1 or z1 - z0 < 1:
            return
        for x in range(x0 + 1, x1):
            self.trap(x, y, z0, trap, "south", half)
            self.trap(x, y, z1, trap, "north", half)
        for z in range(z0 + 1, z1):
            self.trap(x0, y, z, trap, "east", half)
            self.trap(x1, y, z, trap, "west", half)
        self.trap(x0, y, z0, trap, "south", half, "outer_left")
        self.trap(x1, y, z0, trap, "south", half, "outer_right")
        self.trap(x0, y, z1, trap, "north", half, "outer_right")
        self.trap(x1, y, z1, trap, "north", half, "outer_left")

    def dak(self, x0, z0, x1, z1, y, trap=ROOD_TRAP, plaat=ROOD_PLAAT, vol=ROOD, knop=GOUD, zwier=True, max_lagen=99, plafond=None):
        """A hipped Vuurnatie roof over the rectangle (the eaves ring lies at y, one block outside the walls it covers):
        rings of stairs stepping in, a flat top of slabs when the rings are done or max_lagen is reached, upturned golden
        corners (zwier) and, with plafond, a wooden ceiling under it. Returns the y of the top."""
        lagen = min((min(x1 - x0, z1 - z0) + 1) // 2, max_lagen)
        if plafond:
            self.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, plafond)
        for k in range(lagen):
            self.dakring(x0 + k, z0 + k, x1 - k, z1 - k, y + k, trap)
            if k:
                for x in range(x0 + k, x1 - k + 1):
                    for z in range(z0 + k, z1 - k + 1):
                        if (x in (x0 + k, x1 - k) or z in (z0 + k, z1 - k)) and self.naam(x, y + k - 1, z) in (None, gb.AIR):
                            self.set(x, y + k - 1, z, vol)
        k = lagen
        if x1 - x0 - 2 * k >= 0 and z1 - z0 - 2 * k >= 0:
            for x in range(x0 + k, x1 - k + 1):
                for z in range(z0 + k, z1 - k + 1):
                    self.plaat(x, y + k, z, plaat)
                    if k and self.naam(x, y + k - 1, z) in (None, gb.AIR) and (x in (x0 + k, x1 - k) or z in (z0 + k, z1 - k)):
                        self.set(x, y + k - 1, z, vol)
            # (the ridge: a row of full blocks with golden ends, when the top is a strip)
            if min(x1 - x0, z1 - z0) - 2 * k <= 2:
                langs_x = (x1 - x0) >= (z1 - z0)
                if langs_x:
                    zm = (z0 + z1) // 2
                    for x in range(x0 + k, x1 - k + 1):
                        self.set(x, y + k, zm, knop if x in (x0 + k, x1 - k) else vol)
                else:
                    xm = (x0 + x1) // 2
                    for z in range(z0 + k, z1 - k + 1):
                        self.set(xm, y + k, z, knop if z in (z0 + k, z1 - k) else vol)
        if zwier:
            for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
                self.set(x, y, z, knop)
                self.plaat(x, y + 1, z, plaat)
        return y + k

    def pagode(self, x0, z0, x1, z1, y, verdiepingen, hoog=4, wand=ROOD, **dak):
        """Tiers on top of each other, each one block narrower on every side, each with its own eaves. Returns the top y."""
        for v in range(verdiepingen):
            ax0, az0, ax1, az1 = x0 + v * 2, z0 + v * 2, x1 - v * 2, z1 - v * 2
            last = v == verdiepingen - 1
            self.dak(ax0, az0, ax1, az1, y, max_lagen=99 if last else 2, **dak)
            if last:
                break
            self.fill(ax0 + 2, y + 1, az0 + 2, ax1 - 2, y + hoog, az1 - 2, wand)
            y += hoog
        return y

    # --- lighting -------------------------------------------------------------------------------------------------------------
    def verlicht(self, start, min_licht=5, vloer_natuur=GLOEI, vloer_bouw=LAVA):
        """Every walkable spot gets light: where a spot is too dark, a glowing coal is set into natural ground and a tile of
        lukewarm lava into a built floor (never under a marker, never in a stair, a slab or a carpet). The light map is kept
        up to date while lights are added, so no more are set than needed. Returns how many were set."""
        natuur = set()
        for tabel in (gr.OPPERVLAK[gr.ROTS], gr.OPPERVLAK[gr.ZAND], gr.OPPERVLAK[gr.BODEM], gr.OPPERVLAK[gr.KORST], gr.OPPERVLAK[gr.KLIF],
                      gr.OPPERVLAK[gr.GLOED], gr.ONDER):
            natuur |= {gb.mc(n) for n, _ in tabel}
        geen = set(self.geen_licht)
        for e in self.plekken.values():
            if "pos" in e and e["soort"] in ("npc", "prop", "wezen"):
                geen.add((e["pos"][0], e["pos"][2]))
        blocks = self.s.blocks
        reach = kit.Loper(self).bfs(tuple(start))
        light = self.licht()

        def dicht(c):
            bl = blocks.get(c)
            if bl is None:
                return False
            n = bl[0]
            return n != gb.AIR and n not in kit.WATER and not self.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in kit.LICHT

        def gloei(c, niveau):
            light[c] = niveau
            todo = deque([c])
            while todo:
                x, y, z = todo.popleft()
                l = light[(x, y, z)] - 1
                if l <= 0:
                    continue
                for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                    if self.inside(*n) and light.get(n, 0) < l and not dicht(n):
                        light[n] = l
                        todo.append(n)

        def vloer_van(c):
            """The floor block under a walkable cell that may become a light: (its position, the block to set), or None."""
            x, y, z = c
            fy = y - 1
            while fy > 0 and (blocks.get((x, fy, z)) or (None,))[0] in kit.WATER:
                fy -= 1
            vloer = blocks.get((x, fy, z))
            if vloer is None or (x, z) in geen or vloer[2] is not None:
                return None
            boven = (blocks.get((x, fy + 1, z)) or (gb.AIR,))[0]
            if boven != gb.AIR and boven not in kit.WATER:
                return None
            n = vloer[0]
            if n in natuur:
                return (x, fy, z), vloer_natuur
            vast = ("_trap", "_stairs", "_plaat", "_slab", "_muur", "_wall", "door", "chest", "_bed", "glass", "wool", "dansvloer")
            if n in kit.LICHT or not self.solid_at((x, fy, z)) or any(t in n for t in vast):
                return None
            return (x, fy, z), vloer_bouw

        gezet = 0
        for c in sorted(reach):
            if light.get(c, 0) >= min_licht:
                continue
            x, y, z = c
            # a little ahead of the first dark cell lights more of what is still dark; else the cell itself, else next to it
            for d in ((4, 4), (4, 0), (0, 4), (0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2), (1, 1), (-1, -1), (1, -1), (-1, 1)):
                k = (x + d[0], y, z + d[1])
                if k in reach and light.get(k, 0) < min_licht + 3:
                    plek = vloer_van(k)
                    if plek:
                        self.set(*plek[0], plek[1])
                        gloei(plek[0], kit.LICHT[gb.mc(plek[1])])
                        gezet += 1
                        break
        # what is still dark has no floor of its own that can glow (a stair, a spot under a smoke hole): the nearest
        # walkable cell that has one, within five blocks
        for c in sorted(reach):
            if light.get(c, 0) >= min_licht - 1:
                continue
            x, y, z = c
            buurt = sorted((abs(dx) + abs(dy) + abs(dz), dx, dy, dz) for dx in range(-5, 6) for dy in (-2, -1, 0, 1, 2) for dz in range(-5, 6))
            for _, dx, dy, dz in buurt:
                k = (x + dx, y + dy, z + dz)
                plek = vloer_van(k) if k in reach else None
                if plek:
                    self.set(*plek[0], plek[1])
                    gloei(plek[0], kit.LICHT[gb.mc(plek[1])])
                    gezet += 1
                    break
        return gezet


# =====================================================================================================================
# the landscape
# =====================================================================================================================
def landschap(b):
    """The slab with its two craters, the ridge of the Zonnekrijgers, the pads and paths, and the basins."""
    from features import einde2_bouw_vuur_buiten as buiten
    from features import einde2_bouw_vuur_stad as stad
    g = gr.Grond(SEED + 1)
    b.grond = g
    X, Z = g.X, g.Z
    g.plaat(happen=[HAP])
    # the capital's crater and Het Kokend Rotsblok
    g.krater(C[0], C[1], RV, RK, RF, F, KAM, soort_binnen=gr.PLEIN)
    g.krater(K[0], K[1], KRV, KRK, KRF, MEER_VLOER, K_KAM, soort_binnen=gr.BODEM, golf=1.5)
    # the ridge of the Zonnekrijgers: two peaks and the saddle that carries the terrace
    tx, tz, ty = TERRAS
    g.berg(tx - 18, tz, 25, 107, 0.9)
    g.berg(tx + 18, tz, 25, 105, 0.9)
    zadel = (X >= tx - 18) & (X <= tx + 18) & g.land
    hz = np.rint(ty - 2.0 * np.maximum(np.abs(Z - tz) - 9, 0)).astype(np.int32)
    g.soort = np.where(zadel & (hz > g.top), gr.KLIF, g.soort)
    g.top = np.where(zadel & (hz > g.top), hz, g.top)
    # the tunnel of the crater gate ends where the flank is low enough: from there the road lies in the open
    b.mond = next(z for z in range(C[1] + RV + 8, C[1] + RF + 6) if g.top[128, z] <= F + 8)
    g.massief(120, C[1] + RV - 8, 136, b.mond + 1, F - 3)
    # what is designed: pads, paths, stairs
    stad.paleis_grond(g)
    buiten.grond(g, b.mond)
    # the basins (they leave what was designed alone) and what stands in them
    g.bekken(np.hypot(X - A[0], Z - A[1]) < AR + gr.ruis(g.rng, 8) * 1.5, A_VLOER, A_WATER)
    for dx, dz, r, y in PILAREN:
        g.zuil(A[0] + dx, A[1] + dz, r, y)
    g.bekken(np.hypot(X - K[0], Z - K[1]) < KRV - 1.0, MEER_VLOER, MEER, vloeistof=SAUS)
    g.zuil(K[0], K[1], EILAND_R + 0.3, MEER, gr.PLEIN)
    g.zuil(STEIGER[0], STEIGER[1], 2.3, MEER, gr.WEG)
    g.bekken((Z >= 216) & (((X - 126) / 29.0) ** 2 + ((Z - 216) / 25.0) ** 2 < 1.0 + gr.ruis(g.rng, 9) * 0.08), HAVEN_VLOER, HAVEN_WATER)
    g.bekken((X >= 22) & (X <= 88) & (Z > 229 + gr.ruis(g.rng, 12) * 2.5 - np.maximum(60 - X, 0) * 0.1), HAVEN_VLOER + 1, HAVEN_WATER)
    g.talud()
    stad.na_talud(g)
    buiten.na_talud(g)
    g.oever()
    return g


# what the build must have (build contract 6.7; the standard and story markers are added by the check itself)
VERWACHT = [
    "havenpoort", "poortwacht_1", "poortwacht_2", ("bewoner", 8),
    "school", "juf", "schoolbank_speler", ("leerling", 5), "portret_mikai",
    "dansgrot", "dansvloer", "dans_speler", ("muzikant", 3),
    "paleis", "troonzaal", "troon", "vlamgordijn", "mikai", "paleisvijver", ("eendguh", 4), "brood",
    "invasie_strand", "bunker_ingang", "bunker", "bunker_troon", "azula_bunker", "zuka_vader",
    "zonnekrijger_ruine", "trap_onder", "trap_boven", "zonnekrijger", "eeuwige_vlam", "drakengrot_rood", "drakengrot_blauw", "draak_rood",
    "draak_blauw", "drakendans", "drakendans_speler", "drakendans_zuka",
    "kokend_rotsblok", "kokend_meer", "gondel_onder", "gondel_boven", "gondel_route", "gevangenis_plein", "inbraak_route", "suki", "cel_suki",
    ("gevangene", 5), "directeur", "koelcel", "ty_lee", "azula_gondel",
    "sintelstrand", "strandhuis", "theater", "toneel", "publiek_speler", ("acteur", 5),
    "lotus_kamp", "lotus_tent", "miroh_tent", "verzoening_zuka", "bumi", "pakguh_lotus", "piandao",
    "leeuwschildpad_anker", "leeuwschildpad_rug", "leeuwschildpad_kop", "leeuwschildpad", "duel_arena", ("pilaar", 12), "duel_speler", "duel_mikai", "ronde_lucht",
    "ronde_water", "ronde_aarde", "ronde_vuur", "komeet_zicht", "komeetscherf", "kroning", "kroning_zuka",
    "knipoog_sjokkel", "knipoog_sjokkel_finish", "knipoog_sjokkel_camera", "knipoog_stitch_paneel", "knipoog_stitch_bewaker",
    ("knipoog_stitch_celdeur", 3), "knipoog_stitch_camera",
    # phase B (h4_vuur, CONTRACT_E2_B 4.1): the cell under the palace, the square of the Agni Kai, the route of the fleet
    "miroh_cel", "agni_kai_plein", "agni_kai_midden", "luchtvloot_route",
]


def maak(h, opslaan=True, controle=True):
    """Builds the capital. With controle the self-check runs (SystemExit on a problem); with opslaan the tiles and the
    overlay are saved. Returns the build."""
    # (the kit's light table does not know the glowing blocks of the Barbecuether, nor ours: they count while this build is
    # made and checked, and the table is put back as it was for the modules that come after this one)
    kit_licht = dict(kit.LICHT)
    kit.LICHT.update({gb.mc(k): v for k, v in LICHT.items()})
    try:
        return _maak(h, opslaan, controle)
    finally:
        kit.LICHT.clear()
        kit.LICHT.update(kit_licht)


def _maak(h, opslaan, controle):
    from features import einde2_bouw_vuur_buiten as buiten
    from features import einde2_bouw_vuur_stad as stad
    b = Vuur(h)
    g = landschap(b)
    fouten = g.bekken_fouten()
    g.zet(b, b.kies)
    stad.poort(b, b.mond)
    stad.stad(b)
    stad.school(b)
    stad.dansgrot(b)
    stad.paleis(b)
    stad.vijver(b)
    stad.bunker(b)
    buiten.haven(b)
    buiten.kokend_rotsblok(b)
    buiten.zonnekrijgers(b)
    buiten.arena(b)
    buiten.sintelstrand(b)
    buiten.lotus_kamp(b)
    buiten.kade(b)
    buiten.natuur(b)
    laag = b.laag_structuur = buiten.leeuwschildpad(b)
    b.connect()
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    b.lichtjes = b.verlicht(start)
    if controle:
        fouten += b.check(start, VERWACHT, min_licht=4, zwevend_ok=lambda c: c in b.kabel)
        fouten += buiten.eigen_controle(b)
        if fouten:
            raise SystemExit(f"{NAAM}: self-check found {len(fouten)} problem(s):\n  " + "\n  ".join(fouten[:60]))
        print(f"{NAAM}: self-check ok {b.stats}, {b.lichtjes} lichtjes gezet")
    # the Leeuwschildpad-guh lays its chin ON the quay: where a cell of the overlay is a block of the build, the build keeps
    # its block (else taking the overlay away again would leave a hole in the quay)
    for (x, y, z) in list(laag.blocks):
        if b.s.blocks.get((LAAG[0] + x, LAAG[1] + y, LAAG[2] + z), (gb.AIR,))[0] != gb.AIR:
            del laag.blocks[(x, y, z)]
    if opslaan:
        b.laag("leeuwschildpad", laag, *LAAG)
        b.save_tiles()
    return b
