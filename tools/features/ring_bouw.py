"""
bbq2 (ring-kern) - the rest points of the trip (structure guhs:ring_rustpunt, a "grot" of wereld.bbq_structuur: one of three
little camps on a cave floor of the Guhbarbecuether, scattered all over it) and the test template of RingGameTests.

Every camp (13 x 7 x 13, the ground layer is layer 0 = the top block of the cave floor, the centre jigsaw under the fire):
  - the Rustvuurtje (guhs:ring_rustvuur) in the middle of a ring of cracked stones in the floor: walking up to it makes it
    your rest point and Sam-guh cooks (feature/ring/Rustpunt);
  - log benches around the fire, sleeping bags (a cushion and two carpets: a bed would not work down here), a signpost,
    a lantern, a little pile of provisions;
  - and what makes the three different: 0 "afdakje" a lean-to of saté logs with two sleeping bags under it; 1 "worstboom" a
    dead worst tree with a lantern hanging from a branch and the sign nailed to its trunk; 2 "rots" a boulder with a small
    elven tent beside it.

  bouw(h, variant) -> (Structure, problems)        check: the fire, the sign, the jigsaw, nothing floats, the fire is reachable
  preview(out)     pictures of the three camps (python tools/features/ring_bouw.py <out>)
"""
import math
import os
import random
import sys

MIDDEN = "guhs:ring_rustpunt_midden"
SIZE = (13, 7, 13)
C = 6
VARIANTEN = ("afdakje", "worstboom", "rots")

HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
HEK = "guhs:houtskoolsteen_stenen_hek"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
AS_AARDE = "guhs:as_aarde"
AS = "guhs:as_blok"
SATE = "guhs:sate_stam"
WORST = "guhs:worst_stam"
VUUR = "guhs:ring_rustvuur"
AIR = "minecraft:air"


def _fence():
    return {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}


class Kamp:
    def __init__(self, h, variant):
        self.h = h
        self.variant = variant
        self.s = h.Structure(SIZE)
        self.rng = random.Random(21301500 + variant)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def grond(self):
        """A frayed round plate of charred ground, open air above it, a ring of cracked stones around the fire."""
        for x in range(SIZE[0]):
            for z in range(SIZE[2]):
                d = math.hypot(x - C, z - C)
                if d > 6.3 + self.rng.uniform(-0.5, 0.2):
                    continue
                top = self.rng.choice([HOUTSKOOL] * 3 + [AS_AARDE] * 4 + [AS])
                if 1.6 < d < 2.9:
                    top = GEBARSTEN if self.rng.random() < 0.45 else STENEN
                elif d <= 1.6:
                    top = STENEN
                self.set(x, 0, z, top)
                for y in range(1, SIZE[1]):
                    self.set(x, y, z, AIR)

    def vuur(self):
        self.set(C, 1, C, VUUR)

    def bank(self, x, z, langs_x, lengte=3):
        for i in range(lengte):
            self.set(x + (i if langs_x else 0), 1, z + (0 if langs_x else i), SATE, {"axis": "x" if langs_x else "z"})

    def slaapzak(self, x, z, kleur, dx=0, dz=1):
        """A cushion and two carpets in a row (from the cushion in direction dx, dz)."""
        kijkt = {(0, 1): "south", (0, -1): "north", (1, 0): "east", (-1, 0): "west"}[(dx, dz)]
        self.set(x, 1, z, f"guhs:{kleur}_kussen", {"facing": kijkt})
        for i in (1, 2):
            self.set(x + dx * i, 1, z + dz * i, f"minecraft:{kleur}_carpet")

    def lantaarn(self, x, z, hoog=2):
        for y in range(1, 1 + hoog):
            self.set(x, y, z, HEK, _fence())
        self.set(x, 1 + hoog, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})

    def bord(self, x, z, rotatie, regels):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages("sign.guhs.ring", ["", "", "", ""])
        self.set(x, 1, z, "minecraft:spruce_sign", {"rotation": str(rotatie), "waterlogged": "false"}, {
            "id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.ring", regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}})

    def wandbord(self, x, y, z, facing, regels):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages("sign.guhs.ring", ["", "", "", ""])
        self.set(x, y, z, "minecraft:spruce_wall_sign", {"facing": facing, "waterlogged": "false"}, {
            "id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.ring", regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}})

    def proviand(self, x, z):
        self.set(x, 1, z, "guhs:block_of_kaasknabbels")
        self.set(x + 1, 1, z, "guhs:sate_vlees")
        self.set(x, 2, z, "minecraft:brown_carpet")

    def midden(self):
        floor = self.s.blocks.get((C, 0, C))
        self.s.set(C, 0, C, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": floor[0] if floor else STENEN, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    # --- the three camps ---------------------------------------------------------------------------------------------------
    def afdakje(self, borden):
        # a lean-to against the north side: four saté posts, a roof of slabs that slopes to the back, two sleeping bags under it
        for x in (3, 9):
            for y in (1, 2, 3):
                self.set(x, y, 3, SATE, {"axis": "y"})
            for y in (1, 2):
                self.set(x, y, 1, SATE, {"axis": "y"})
        for x in range(2, 11):
            self.set(x, 4, 3, PLAAT, {"type": "bottom", "waterlogged": "false"})
            self.set(x, 3, 2, PLAAT, {"type": "top", "waterlogged": "false"})
            self.set(x, 3, 1, PLAAT, {"type": "bottom", "waterlogged": "false"})
        self.slaapzak(5, 1, "green")
        self.slaapzak(7, 1, "red")
        self.bank(5, 9, True)
        self.bank(2, 5, False)
        self.lantaarn(10, 8)
        self.bord(9, 5, 10, borden)
        self.proviand(10, 4)

    def worstboom(self, borden):
        # a dead worst tree in the north-east: a crooked trunk, three bare branches, a lantern on a chain over the camp
        for y in range(1, 5):
            self.set(9, y, 3, WORST, {"axis": "y"})
        self.set(9, 5, 3, WORST, {"axis": "y"})
        self.set(8, 4, 3, WORST, {"axis": "x"})
        self.set(7, 4, 3, WORST, {"axis": "x"})
        self.set(9, 4, 4, WORST, {"axis": "z"})
        self.set(9, 4, 5, WORST, {"axis": "z"})
        self.set(10, 5, 3, WORST, {"axis": "x"})
        self.set(7, 3, 3, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        self.set(7, 2, 3, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
        self.wandbord(9, 2, 4, "south", borden)
        self.slaapzak(3, 3, "yellow", dx=1, dz=0)
        self.slaapzak(2, 8, "green", dx=1, dz=0)
        self.bank(5, 9, True)
        self.bank(9, 6, False, 2)
        self.proviand(10, 8)

    def rots(self, borden):
        # a boulder in the north-west and a little elven tent (a ridge of wool over one sleeping bag) in the east
        for (x, y, z) in ((2, 1, 2), (3, 1, 2), (2, 1, 3), (3, 1, 3), (4, 1, 3), (3, 1, 4), (2, 2, 2), (3, 2, 2), (3, 2, 3), (2, 2, 3), (3, 3, 2), (4, 1, 2)):
            self.set(x, y, z, HOUTSKOOL if self.rng.random() < 0.7 else STENEN)
        self.set(4, 2, 3, PLAAT, {"type": "bottom", "waterlogged": "false"})
        self.set(4, 2, 2, "guhs:uienlicht")
        for x in range(8, 11):
            self.set(x, 1, 2, "minecraft:green_wool")
            self.set(x, 1, 4, "minecraft:green_wool")
            self.set(x, 2, 3, "minecraft:lime_wool")
        self.set(11, 1, 3, "minecraft:green_wool")
        self.slaapzak(10, 3, "white", dx=-1, dz=0)
        self.bank(5, 9, True)
        self.bank(2, 6, False, 2)
        self.lantaarn(10, 8)
        self.bord(8, 6, 12, borden)
        self.proviand(9, 9)


def bouw(h, variant):
    """The template of camp `variant` (not saved) and the problems of its self-check."""
    from features import ring_tekst
    k = Kamp(h, variant)
    k.grond()
    k.vuur()
    getattr(k, VARIANTEN[variant])(ring_tekst.BORDEN[variant] if variant else ring_tekst.BORD)
    k.midden()
    return k.s, check(k.s)


def check(s):
    problems = []
    blocks = s.blocks
    if s.get(C, 1, C) != VUUR:
        problems.append("no Rustvuurtje in the middle")
    jigsaws = [c for c, b in blocks.items() if b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == MIDDEN]
    if jigsaws != [(C, 0, C)]:
        problems.append(f"the centre jigsaw belongs under the fire, exactly once: {jigsaws}")
    if not any(b[0].endswith("sign") for b in blocks.values()):
        problems.append("no signpost")
    if not any(b[0].endswith("_kussen") for b in blocks.values()):
        problems.append("no sleeping bag")
    hangt = ("minecraft:lantern", "minecraft:chain", "minecraft:spruce_wall_sign")
    for (x, y, z), b in blocks.items():
        if b[0] in (AIR, "minecraft:jigsaw") or y == 0:
            continue
        buren = [(x, y - 1, z), (x, y + 1, z), (x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1)]
        steun = buren[:1] if b[0] not in hangt and not b[0].endswith(("_stam", "_plaat", "_wool")) else buren
        if not any(blocks.get(c, (AIR,))[0] not in (AIR,) for c in steun):
            problems.append(f"{b[0]} floats at {(x, y, z)}")
    # every side of the fire can be walked to from the edge of the plate (two blocks of air, ground under it)
    vrij = {(x, z) for (x, y, z), b in blocks.items() if y == 0 and b[0] != AIR
            and blocks.get((x, 1, z), (AIR,))[0] in (AIR,) and blocks.get((x, 2, z), (AIR,))[0] == AIR}
    lopend = set()
    for x, z in vrij:
        if (x, z) not in lopend and ((x, z) in ((C + 1, C), (C - 1, C), (C, C + 1), (C, C - 1))):
            stapel = [(x, z)]
            while stapel:
                p = stapel.pop()
                if p in lopend or p not in vrij:
                    continue
                lopend.add(p)
                stapel += [(p[0] + 1, p[1]), (p[0] - 1, p[1]), (p[0], p[1] + 1), (p[0], p[1] - 1)]
    if not any(math.hypot(x - C, z - C) > 5.2 for x, z in lopend):
        problems.append("the fire can't be reached from outside the camp")
    return problems


def test_template(h):
    """RingGameTests: a room with a floor, a Rustvuurtje, a hook on a pillar, and room to walk and ride."""
    s = h.Structure((21, 9, 21))
    for x in range(21):
        for z in range(21):
            s.set(x, 0, z, HOUTSKOOL)
    s.set(4, 1, 4, VUUR)
    for y in range(1, 6):
        s.set(16, y, 4, STENEN)
    s.set(16, 5, 5, "guhs:elfentouw_haak", {"facing": "south"})
    return s


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What bouw() needs of the make_v2 namespace, for the preview."""
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats


def gedraaid(s, kwart):
    import make_structures as ms
    W, H, D = s.size
    for _ in range(kwart % 4):
        t = ms.Structure((D, H, W))
        for (x, y, z), b in s.blocks.items():
            t.blocks[(D - 1 - z, y, x)] = b
        s, (W, H, D) = t, t.size
    return s


def preview(out):
    import wiki_renders as wr
    wr.SPECIAL_COLOURS[VUUR] = (255, 150, 50)
    wr.SPECIAL_COLOURS["minecraft:lantern"] = (255, 214, 120)
    wr.SPECIAL_COLOURS["minecraft:chain"] = (70, 74, 90)
    wr.SPECIAL_COLOURS[HEK] = (57, 47, 47)
    wr.SPECIAL_COLOURS["minecraft:spruce_sign"] = (126, 94, 56)
    wr.SPECIAL_COLOURS["minecraft:spruce_wall_sign"] = (126, 94, 56)
    for kleur, rgb in (("green", (90, 130, 60)), ("red", (170, 50, 50)), ("yellow", (240, 200, 60)), ("white", (236, 236, 236))):
        wr.SPECIAL_COLOURS[f"guhs:{kleur}_kussen"] = rgb
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    for v, naam in enumerate(VARIANTEN):
        s, problems = bouw(_H(), v)
        for p in problems:
            print("PROBLEM:", naam, p)
        for kwart in (0, 2):
            wr.render_structure(gedraaid(s, kwart), {}, px=26, max_size=1200).save(os.path.join(out, f"ring_rustpunt_{naam}_{kwart}.png"))
    print("camps:", SIZE)


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
