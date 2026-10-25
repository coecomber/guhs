"""
1.7.0 playtest: WORKING vadskracht in the underground buildings (feature/vadskracht: the Guhrad is the source, Guhdraad the
net, machines the consumers; feature/techsaus: the Sauspomp, Sausslang and Sausvat). Helpers for the template scripts
(onderwol_structuren2.py: the Kaasmakerij's pomphuis, onderwol_structuren3.py: the Wolclub's krachtcentrale,
onderwol_stadje.py: the Guhrad-centrale), on a Bouw (features/onderwol_bouw.py):

    guhrad(b, x, y, z, facing)      a Guhrad (3 x 3) with a happy guh running in it; the wheel is "Vast" (a building's guh: it
                                    has no owner, nobody takes it out, a broken wheel lets it hop out as a wild guh)
    machine(b, x, y, z, naam, ...)  a guh machine, asleep (the net wakes it), optionally with items in its slots
    oven(b, x, y, z, facing, ...)   a Guhoven, optionally with something to bake
    draad(b, x, y, z, lamp=True)    a piece of Guhdraad, with a redstone lamp under it (the wire powers the block it lies
                                    on while its net runs: the lamp burns on the guh's power)
    slang(b, x, y, z)               a piece of Sausslang
    verbind(b)                      LAST: every Guhdraad and Sausslang of the template gets the block state of what is
                                    around it (a structure placed in a new chunk gets no block updates: the hose nets
                                    read the hose's arms, so they must be right in the template)

The Java side checks it in game tests (feature/onderwol/bouw/KrachtProef: the template placed, also turned).
"""
import json
import os

import make_structures as ms

HIER = os.path.dirname(os.path.abspath(__file__))
TAG = os.path.join(HIER, "..", "..", "src", "main", "resources", "data", "guhs", "tags", "block", "vadskracht.json")
# what Guhdraad bends towards (the block tag guhs:vadskracht), and what a Sausslang joins (everything with a fluid tank)
with open(TAG, encoding="utf-8") as f:
    VADS = set(json.load(f)["values"])
SAUS = {"guhs:sauspomp", "guhs:sausvat", "guhs:brouwautomaat", "guhs:frituurautomaat", "guhs:grillkoolpers", "minecraft:cauldron"}
DRAAD, SLANG, LAMP = "guhs:guh_wire", "guhs:sausslang", "minecraft:redstone_lamp"
RICHTING = {"north": (0, 0, -1), "east": (1, 0, 0), "south": (0, 0, 1), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
# GuhWheelPartBlock.along(facing) = facing.getClockWise()
LANGS = {"north": (1, 0), "east": (0, 1), "south": (-1, 0), "west": (0, -1)}
# full blocks a wire may climb onto (anything else: the wire stays flat)
VOL = {LAMP, "minecraft:spruce_planks", "guhs:gladde_knuffelsteen", "guhs:viltsteen_tegels"}


def wiel_blokken(x, y, z, facing):
    """{(x, y, z): (side, height)} of the 9 blocks of a Guhrad whose wheel block (bottom middle) is at (x, y, z)."""
    dx, dz = LANGS[facing]
    return {(x + dx * (s - 1), y + h, z + dz * (s - 1)): (s, h) for s in range(3) for h in range(3)}


def guhrad(b, x, y, z, facing, blij=True, variant=None):
    """A Guhrad with a running guh (happy: 15 VK, else 10) facing this way (its front: where you see the guh run).
    Every spot of the 3 x 3 must be open (air); returns its 9 spots."""
    blokken = wiel_blokken(x, y, z, facing)
    for p in blokken:
        assert b.get(*p) in (None, "minecraft:air"), f"{b.naam}: the Guhrad at {(x, y, z)} needs {p} open, not {b.get(*p)}"
    guh = ms.guh_nbt(1.0)
    if variant:
        guh["Variant"] = variant
    b.zet(x, y, z, f"guh_wheel[facing={facing},running=true]",
          {"id": "guhs:guh_wheel", "HasGuh": ms.Byte(1), "Blij": ms.Byte(1 if blij else 0), "Vast": ms.Byte(1), "Guh": guh})
    for (p, (s, h)) in blokken.items():
        if p != (x, y, z):
            b.zet(*p, f"guh_wheel_part[facing={facing},side={s},height={h}]")
    return list(blokken)


def _items(stapels, slots):
    """A machine's slots (MachineBlockEntity "Vakken": a list as long as the machine has slots, empty = {})."""
    out = [{} for _ in range(slots)]
    for (slot, item, n) in stapels:
        out[slot] = {"id": item, "count": n}
    return ms.compounds(out)


def machine(b, x, y, z, naam, facing, slots=0, stapels=()):
    """A guh machine (guhs:<naam>: vadsmolen, sauspomp...) asleep; stapels = [(slot, item id, count)] in its slots."""
    nbt = {"id": f"guhs:{naam}"}
    if stapels:
        nbt["Vakken"] = {"stacks": _items(stapels, slots)}
    b.zet(x, y, z, f"{naam}[facing={facing},snoet=slaapt]", nbt)


def oven(b, x, y, z, facing, bak=None, n=0):
    """A Guhoven (asleep, cold); bak = an item id it bakes n of (its input slot)."""
    nbt = {"id": "guhs:guh_oven"}
    if bak:
        nbt["Items"] = ms.compounds([{"Slot": ms.Byte(0), "id": bak, "count": n}])
    b.zet(x, y, z, f"guh_oven[facing={facing},lit=false,snoet=slaapt]", nbt)


def draad(b, x, y, z, lamp=True):
    """Guhdraad at (x, y, z) (it needs a full block under it); lamp: that block is a redstone lamp (it burns on vadskracht)."""
    if lamp and b.get(x, y - 1, z) != "minecraft:jigsaw":   # (never over a building's anchor or shaft marker)
        b.zet(x, y - 1, z, f"{LAMP}[lit=false]")
    onder = b.get(x, y - 1, z)
    assert onder not in (None, "minecraft:air"), f"{b.naam}: Guhdraad at {(x, y, z)} needs a block under it"
    b.zet(x, y, z, DRAAD)


def slang(b, x, y, z):
    b.zet(x, y, z, SLANG)


def _naam(b, p):
    blok = b.blocks.get(p)
    return blok[0] if blok else None


def verbind(b):
    """The block states of every Guhdraad (dust arms, like GuhWireBlock.connectionState) and Sausslang (an arm per joined
    side, like SausslangBlock.verbindt) from what stands around it in the template."""
    for p, (naam, props, nbt) in list(b.blocks.items()):
        x, y, z = p
        if naam == SLANG:
            arms = {}
            for kant, (dx, dy, dz) in RICHTING.items():
                buur = _naam(b, (x + dx, y + dy, z + dz))
                arms[kant] = "true" if buur == SLANG or buur in SAUS else "false"
            b.blocks[p] = (naam, arms, nbt)
        elif naam == DRAAD:
            boven_vrij = _naam(b, (x, y + 1, z)) in (None, "minecraft:air")
            arms = {}
            for kant in ("north", "east", "south", "west"):
                dx, _, dz = RICHTING[kant]
                buur = (x + dx, y, z + dz)
                n = _naam(b, buur)
                if boven_vrij and n in VOL and _naam(b, (x + dx, y + 1, z + dz)) == DRAAD:
                    arms[kant] = "up"
                elif n == DRAAD or n in VADS:
                    arms[kant] = "side"
                elif n in (None, "minecraft:air") and _naam(b, (x + dx, y - 1, z + dz)) == DRAAD:
                    arms[kant] = "side"
                else:
                    arms[kant] = "none"
            verbonden = [k for k, v in arms.items() if v != "none"]
            if not verbonden:
                arms = {k: "side" for k in arms}
            elif not ({"east", "west"} & set(verbonden)):
                arms["north"] = arms["north"] if arms["north"] != "none" else "side"
                arms["south"] = arms["south"] if arms["south"] != "none" else "side"
            elif not ({"north", "south"} & set(verbonden)):
                arms["east"] = arms["east"] if arms["east"] != "none" else "side"
                arms["west"] = arms["west"] if arms["west"] != "none" else "side"
            b.blocks[p] = (naam, arms, nbt)   # (powered: false, the default; the net lights it)
