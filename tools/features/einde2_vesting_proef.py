"""
einde2 (the Guheinde expansion), slice vesting: whole Mika-vestingen put together the way the game does it, to check them
before the game ever sees them (not a feature itself: einde2_vesting calls it).

stel_samen() follows net.minecraft JigsawPlacement step by step: the start piece turned at random, its jigsaws shuffled and
ordered by selection priority, for each the pieces of the pool (then of its fallback) shuffled, every rotation, every
jigsaw of the candidate that fits; a child lies wholly outside every other box and inside max_distance_from_center; a
child's own jigsaws wait in a queue ordered by placement priority; at the last depth only the fallback is asked. The dice
are python's, not the game's: it is the same kind of vesting, not the same one.

controleer() builds many of them and asks of each: is everything there that a vesting always has (the keep up to its roof,
the cellenblok, the steiger and ONE ship), does it stay under the height limit and inside its reach, does nothing float,
can a player walk from the gate into every piece and to every marker and chest, is every floor lit. hoogste() gives the
worst case of the pools without dice. beeld() renders a few (from the worktree root:
PYTHONPATH=tools python -m features.einde2_vesting_proef <folder>; nothing is written but the pictures).
"""
import heapq
import random

import features.einde2_kit as kit
from features import einde2_vesting_bouw as vb
from features import guheinde_bouw as gb

RICHTINGEN = ["north", "east", "south", "west"]      # a quarter turn clockwise is one step on
STAP = {"north": (0, 0, -1), "east": (1, 0, 0), "south": (0, 0, 1), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
PROEVEN = 60                                          # how many vestingen controleer() builds


def draai(p, r):
    """StructureTemplate.transform without a pivot: r quarter turns clockwise."""
    x, y, z = p
    return ((x, y, z), (-z, y, x), (-x, y, -z), (z, y, -x))[r % 4]


def draai_richting(d, r):
    return d if d in ("up", "down") else RICHTINGEN[(RICHTINGEN.index(d) + r) % 4]


def doos(b, pos, r):
    a, c = draai((0, 0, 0), r), draai((b.W - 1, b.H - 1, b.D - 1), r)
    return tuple(pos[i] + min(a[i], c[i]) for i in range(3)) + tuple(pos[i] + max(a[i], c[i]) for i in range(3))


def snijdt(p, q):
    return all(p[i] <= q[i + 3] and q[i] <= p[i + 3] for i in range(3))


def stel_samen(stukken, seed, diepte=vb.DIEPTE, afstand=vb.MAX_AFSTAND):
    """One vesting: a list of placed pieces {naam, rot, pos, doos, diepte}; the anchor of the kern lies at 0, 0, 0."""
    rng = random.Random(seed)

    def geschud(lijst):
        lijst = list(lijst)
        rng.shuffle(lijst)
        return lijst

    def uit_pool(pool):
        return geschud([s for s, w in vb.POOLS[pool][0] for _ in range(w)])

    kern = stukken["kern"]
    rot = rng.randrange(4)
    ax, ay, az, _, _ = kern.anchor_at
    d = draai((ax, ay, az), rot)
    pos = (-d[0], -d[1], -d[2])
    eerste = {"naam": "kern", "rot": rot, "pos": pos, "doos": doos(kern, pos, rot), "diepte": 0}
    cx, cz = (eerste["doos"][0] + eerste["doos"][3]) // 2, (eerste["doos"][2] + eerste["doos"][5]) // 2
    grens = (cx - afstand, -afstand, cz - afstand, cx + afstand, afstand, cz + afstand)
    geplaatst = [eerste]
    rij, teller = [], 0

    def kinderen(stuk):
        nonlocal teller
        b = stukken[stuk["naam"]]
        jigs = sorted(geschud(b.jigs), key=lambda j: -j["sel"])
        for j in jigs:
            if not j["pool"]:
                continue
            bron = tuple(stuk["pos"][i] + draai(j["pos"], stuk["rot"])[i] for i in range(3))
            kant = draai_richting(j["richting"], stuk["rot"])
            doel = tuple(bron[i] + STAP[kant][i] for i in range(3))
            kandidaten = uit_pool(j["pool"]) if stuk["diepte"] != diepte else []
            terugval = vb.POOLS[j["pool"]][1]
            if terugval:
                kandidaten += uit_pool(terugval)
            klaar = False
            for naam in kandidaten:
                kb = stukken[naam]
                for r2 in geschud(range(4)):
                    for kj in sorted(geschud(kb.jigs), key=lambda q: -q["sel"]):
                        if kj["naam"] != j["doel"] or draai_richting(kj["richting"], r2) != TEGEN[kant]:
                            continue
                        if kant in ("up", "down") and r2 != stuk["rot"]:        # (joint "aligned": both tops point the same way)
                            continue
                        kd = draai(kj["pos"], r2)
                        kpos = tuple(doel[i] - kd[i] for i in range(3))
                        box = doos(kb, kpos, r2)
                        if any(box[i] < grens[i] or box[i + 3] > grens[i + 3] for i in range(3)) or any(snijdt(box, g["doos"]) for g in geplaatst):
                            continue
                        kind = {"naam": naam, "rot": r2, "pos": kpos, "doos": box, "diepte": stuk["diepte"] + 1, "aan": stuk["naam"]}
                        geplaatst.append(kind)
                        if kind["diepte"] <= diepte:
                            teller += 1
                            heapq.heappush(rij, (-j["plc"], teller, len(geplaatst) - 1))
                        klaar = True
                        break
                    if klaar:
                        break
                if klaar:
                    break

    kinderen(eerste)
    while rij:
        _, _, i = heapq.heappop(rij)
        kinderen(geplaatst[i])
    return geplaatst


def wereld(h, stukken, geplaatst):
    """The blocks of a placed vesting as one build (a kit.Bouw; jigsaw blocks are what they turn into) -> (bouw, shift):
    world position + shift = position in the build."""
    lo = [min(g["doos"][i] for g in geplaatst) for i in range(3)]
    hi = [max(g["doos"][i + 3] for g in geplaatst) for i in range(3)]
    b = kit.Bouw(h, f"{vb.NS}/_proef", tuple(hi[i] - lo[i] + 1 + (2 if i == 1 else 0) for i in range(3)), 0)
    shift = (-lo[0], -lo[1], -lo[2])
    for g in geplaatst:
        s = stukken[g["naam"]]
        finals = {j["pos"]: j["final"] for j in s.jigs}
        if g["naam"] == "kern":
            finals[s.anchor_at[:3]] = s.anchor_at[4]
        for c, (naam, props, _) in s.s.blocks.items():
            if naam == "minecraft:jigsaw":
                naam, props = finals[c].split("[")[0], {}
            w = draai(c, g["rot"])
            b.s.blocks[tuple(g["pos"][i] + w[i] + shift[i] for i in range(3))] = (naam, props, None)
        for (x, y, z, soort, nbt) in s.ents:
            w = draai((x - 0.5, y, z - 0.5), g["rot"])
            b.ents.append((g["pos"][0] + w[0] + 0.5 + shift[0], g["pos"][1] + w[1] + shift[1], g["pos"][2] + w[2] + 0.5 + shift[2], soort, nbt))
    return b, shift


def in_wereld(g, shift, c):
    w = draai(c, g["rot"])
    return tuple(g["pos"][i] + w[i] + shift[i] for i in range(3))


def bekijk(h, stukken, geplaatst):
    """One placed vesting -> (problems, numbers)."""
    p = []
    namen = [g["naam"] for g in geplaatst]
    for s in vb.ALTIJD:
        if namen.count(s) != 1:
            p.append(f"{s} zit er {namen.count(s)} keer in (altijd precies een)")
    schepen = [n for n in namen if n in vb.SCHEPEN]
    if len(schepen) != 1:
        p.append(f"{len(schepen)} vetschepen (altijd precies een)")
    if sum(n in ("slaapzaal", "depot") for n in namen) != 1:
        p.append("de tweede verdieping van de burcht ontbreekt")
    top = max(g["doos"][4] for g in geplaatst)
    if top > vb.MAX_BOVEN - 1:
        p.append(f"te hoog: {top} boven de vloer van het begin (hooguit {vb.MAX_BOVEN - 1})")
    ver = max(max(abs(g["doos"][i]), abs(g["doos"][i + 3])) for g in geplaatst for i in (0, 2))
    b, shift = wereld(h, stukken, geplaatst)
    kern = geplaatst[0]
    start = in_wereld(kern, shift, stukken["kern"].begin)
    lop = kit.Loper(b)
    if not lop.standable(start):
        return p + ["voor de poort kun je niet staan"], {}
    reach = lop.bfs(start)
    for g in geplaatst:
        s = stukken[g["naam"]]
        if s.begin and in_wereld(g, shift, s.begin) not in reach:
            p.append(f"{g['naam']} (aan {g.get('aan')}): daar kom je niet vanaf de poort")
        for id, e in s.plekken.items():
            if "tot" in e:
                (x0, y0, z0), (x1, y1, z1) = e["pos"], e["tot"]
                if in_wereld(g, shift, ((x0 + x1) // 2, y0, (z0 + z1) // 2)) not in reach and \
                        not any(in_wereld(g, shift, (x, y0, z)) in reach for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)):
                    p.append(f"zone {id}: daar kom je niet")
            else:
                c = in_wereld(g, shift, tuple(e["pos"]))
                if c not in reach and not gb.near(reach, c, dy=(-1, 0, 1)):
                    p.append(f"plek {id} in {g['naam']}: daar kom je niet")
        for c, bl in s.s.blocks.items():
            if bl[0] == "minecraft:chest" and not gb.near(reach, in_wereld(g, shift, c), dy=(0, -1, 1)):
                p.append(f"kist in {g['naam']} op {c}: daar kom je niet bij")
    onder = (start[0], start[1] - 1, start[2])
    los = {c for c in gb.floating(b, lambda c: c == onder) if not b.is_dun(b.s.blocks[c][0]) and "sign" not in b.s.blocks[c][0]}
    if los:
        p.append(f"{len(los)} zwevende blokken, bv. {sorted(los)[:4]}")
    licht = b.licht()
    donker = [c for c in reach if licht.get(c, 0) < 4]
    if donker:
        p.append(f"{len(donker)} van {len(reach)} beloopbare plekken zijn te donker, bv. {sorted(donker)[:4]}")
    return p, {"stukken": len(geplaatst), "top": top, "ver": ver, "beloopbaar": len(reach), "schip": schepen[0] if schepen else None,
               "torens": sum(n.startswith("toren_") for n in namen), "cellen": 6 + 2 * namen.count("toren_kerker"),
               "schuilguhs": sum(1 for e in b.ents if e[3] == "schuilguh"), "mikas": sum(1 for e in b.ents if e[3] == "mika")}


def hoogste(stukken):
    """The worst case of the pools, without dice: the highest and the lowest block any chain of pieces can reach, counted
    from the floor of the start -> (top, bottom)."""
    gezien = {}

    def af(naam, y, diepte):
        key = (naam, y, min(diepte, vb.DIEPTE + 1))
        if key in gezien:
            return gezien[key]
        b = stukken[naam]
        top, bodem = y + b.H - 1, y
        gezien[key] = (top, bodem)
        if diepte <= vb.DIEPTE:
            for j in b.jigs:
                pool = j["pool"]
                eerste = True
                while pool:
                    if not (eerste and diepte == vb.DIEPTE):          # (at the last depth only the fallback is asked)
                        for s, _ in vb.POOLS[pool][0]:
                            for kj in stukken[s].jigs:
                                if kj["naam"] == j["doel"]:
                                    t2, b2 = af(s, y + j["pos"][1] + STAP[j["richting"]][1] - kj["pos"][1], diepte + 1)
                                    top, bodem = max(top, t2), min(bodem, b2)
                    pool, eerste = vb.POOLS[pool][1], False
        gezien[key] = (top, bodem)
        return gezien[key]

    return af("kern", 0, 0)


def controleer(h, stukken, proeven=PROEVEN):
    """Builds `proeven` vestingen -> (problems, a summary line)."""
    problemen = []
    top, bodem = hoogste(stukken)
    if top > vb.MAX_BOVEN - 1:
        problemen.append(f"de pools laten een vesting toe van {top} hoog (hooguit {vb.MAX_BOVEN - 1} boven de vloer van het begin)")
    cijfers = []
    for seed in range(proeven):
        geplaatst = stel_samen(stukken, 214007500 + seed)
        p, n = bekijk(h, stukken, geplaatst)
        problemen += [f"proef {seed}: {x}" for x in p[:6]]
        if n:
            cijfers.append(n)
    if not cijfers:
        return problemen, "geen enkele proef lukte"
    gezien = {g for seed in range(proeven) for g in (x["naam"] for x in stel_samen(stukken, 214007500 + seed))}
    nooit = sorted(set(stukken) - gezien)
    if nooit:
        problemen.append(f"deze stukken kwamen in geen enkele proef voor: {nooit}")
    lo, hi, gem = (lambda k: min(c[k] for c in cijfers)), (lambda k: max(c[k] for c in cijfers)), (lambda k: sum(c[k] for c in cijfers) / len(cijfers))
    regel = (f"{len(cijfers)} proefvestingen: {lo('stukken')}-{hi('stukken')} stukken (gem. {gem('stukken'):.1f}), {lo('torens')}-{hi('torens')} zijtorens, "
             f"top {lo('top')}-{hi('top')} (pools: hooguit {top}, diepste {bodem}), reikwijdte tot {hi('ver')}, {lo('cellen')}-{hi('cellen')} cellen, "
             f"{lo('schuilguhs')}-{hi('schuilguhs')} Schuilguhs, {lo('mikas')}-{hi('mikas')} Mika's; schepen "
             + ", ".join(f"{s[6:]} {sum(c['schip'] == s for c in cijfers)}" for s in vb.SCHEPEN))
    return problemen, regel


def beeld(h, stukken, seed, pad, px=7):
    """Renders one proefvesting from two sides (tools/wiki_renders.py draws; it is only read here)."""
    import wiki_renders as wr
    geplaatst = stel_samen(stukken, seed)
    b, _ = wereld(h, stukken, geplaatst)
    s = h.Structure((b.W, b.H, b.D))
    s.blocks = dict(b.s.blocks)
    for k, turns in (("a", 0), ("b", 2)):
        wr.render_structure(wr.turned(s, turns), {}, px=px, max_size=1800).save(f"{pad}_{k}.png")
    return geplaatst


if __name__ == "__main__":
    import os
    import sys
    import types
    import make_v2
    uit = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(uit, exist_ok=True)
    hh = types.SimpleNamespace(**vars(make_v2))
    alle, fouten = vb.maak(hh)
    fouten2, regel = controleer(hh, alle)
    print(regel)
    for f in fouten + fouten2:
        print("  ", f)
    for zaad in (1, 2, 3, 4):
        wie = beeld(hh, alle, 214007500 + zaad, os.path.join(uit, f"vesting_{zaad}"))
        print(zaad, [g["naam"] for g in wie])
