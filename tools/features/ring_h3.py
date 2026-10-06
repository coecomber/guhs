"""
bbq2 (ring-h3): chapter 3 of the Knabbelring: De Mijnen van Knabbelmoria (DESIGN_130 4). Java: feature/ringh3.

  ring_h3_bouw.py      the mine, block by block (structure guhs:knabbelmoria: one template, sunk into the rock), its self-check
  ring_h3_modellen.py  the Barbecuerog: model, textures, animations
  ring_h3_scene.py     the two camera scenes (the well, the bridge), written once, in template coordinates
  ring_h3_java.py      writes feature/ringh3/Plekken.java and Scenes.java from the two above (a dev tool; the build only compares)
  ring_h3_beeld.py     pictures for whoever builds: cut-aways, a ray caster that stands in the halls, frames of the scenes
  ring_h3_tekst.py     every Dutch text
  ring_h3_wiki.py      the wiki entries (CONTRACT_130 2.4; not built here)
  here                 the three blocks (Brokkelsteen, Dwergen-hefboom, Runensteen), the sounds, the questline, the narrator card,
                       the scenes' texts, the advancements, the structure set, the FTB section

Fixed id of CONTRACT_130 7 that this module owns: barbecuerog (entity).
"""
import json
import os

import numpy as np
from PIL import Image, ImageDraw

from features import bbq2
from features import ring_h3_bouw as bouw
from features import ring_h3_java as java
from features import ring_h3_modellen as modellen
from features import ring_h3_scene as scene
from features import ring_h3_tekst as tekst
from features import verhaal_motor
from features import wereld

FTB_LINEAIR = True
FTB_SECTIES = [("ring_h3", "De Mijnen van Knabbelmoria", "npc:gimguh", None)]

STRUCTUUR = bouw.NAAM
SALT = 21301801
VOORRANG = 290                # (CONTRACT_130 3: guhvendel 300 .. frituurberg 260, in chapter order)
VORIGE = "guhvendel"          # the story chain: this structure stands 250-500 blocks from the one of chapter 2
# until ring-h2 is merged there is no guhvendel to stand near: then the mine takes a slice of the ring round 0,0 of its own
# (sectors 0-12 are taken, CONTRACT_130 3); with ring-h2 in the tree this is never used
NOOD_SECTOR = 13
TYPE = "guhs:ringh3_mijn"

# =====================================================================================================================
# the three blocks
# =====================================================================================================================
# the eight rune signs, 16 x 16 ('#' = carved)
TEKENS = {
    "kaas": ["................", "................", ".........##.....", ".......##..#....", ".....##.....#...", "...##........#..", "..#############.",
             "..#..#......#.#.", "..#.......#...#.", "..#...##......#.", "..#...##...#..#.", "..#...........#.", "..#############.", "................",
             "................", "................"],
    "worst": ["................", "................", "..##........##..", ".#..#......#..#.", ".#...#....#...#.", "..#...####...#..", "..#..........#..",
              "..#..........#..", "...#........#...", "...#........#...", "....##....##....", "......####......", "................", "................",
              "................", "................"],
    "saus": ["................", ".......##.......", ".......##.......", "......#..#......", "......#..#......", ".....#....#.....", "....#......#....",
             "...#........#...", "...#........#...", "...#.#......#...", "...#..#.....#...", "....#......#....", ".....######.....", "................",
             "................", "................"],
    "knabbel": ["................", "...##########...", "...#........#...", "...#.##.....#...", "...#.##...###...", "...#.....##.....", "...#.....#......",
                "...#.....##.....", "...#.......##...", "...#..##....#...", "...#..##....#...", "...#........#...", "...##########...", "................",
                "................", "................"],
    "bot": ["................", "..##............", ".#..#...........", ".#...#..........", "..#...#.........", "...##..#........", ".....#..#.......",
            "......#..#......", ".......#..##....", "........#...#...", ".........#...#..", "..........#..#..", "...........##...", "................",
            "................", "................"],
    "vlam": ["................", ".......#........", "......##........", "......#.#.......", ".....#..#..#....", ".....#...###....", "....#......#....",
             "...#...#....#...", "...#..#.#...#...", "...#..#.#...#...", "....#..#...#....", ".....######.....", "................", "................",
             "................", "................"],
    "njeg": ["................", ".#..#.###.#.#.#.", ".##.#...#.#.#.#.", ".#.##...#.##..#.", ".#..#.#.#.#.#...", ".#..#..#..#.#.#.", "................",
             "...##########...", "................", ".###.###.##.#.#.", ".#...#...#..#.#.", ".##..###.#.###..", ".#...#.#.#..#.#.", ".###.###.##.#.#.",
             "................", "................"],
    "trommel": ["................", "................", "...##########...", "..#..........#..", "..#.########.#..", "..##........##..", "..#.#......#.#..",
                "..#..#....#..#..", "..#...#..#...#..", "..#....##....#..", "..#...#..#...#..", "..##........##..", "...##########...", "................",
                "................", "................"],
}
TEKEN_NAMEN = ("kaas", "worst", "saus", "knabbel", "bot", "vlam", "njeg", "trommel")       # block state teken = index (Raadsels.TEKEN_NAMEN)


def _steen(h, naam):
    return np.asarray(Image.open(os.path.join(h.TEX, "block", naam)).convert("RGBA")).astype(np.float32)


def textures(h):
    rng = np.random.default_rng(21301855)
    # the rune stones: a dressed, darker face of chiselled stone with a rim; the sign carved deep and filled with cheese gold
    # (the njeg rune: with pale blue light, like the letters on the gate)
    basis = _steen(h, "houtskoolsteen_stenen.png")
    vlak = basis.copy()
    vlak[..., :3] = vlak[..., :3].mean(axis=(0, 1), keepdims=True) * 0.82 + (rng.random((16, 16, 1)) - 0.5) * 12
    vlak[0, :, :3] *= 1.25
    vlak[:, 0, :3] *= 1.18
    vlak[15, :, :3] *= 0.62
    vlak[:, 15, :3] *= 0.7
    for naam in TEKEN_NAMEN:
        a = vlak.copy()
        licht, donker = ((176, 226, 255), (60, 110, 170)) if naam == "njeg" else ((232, 186, 84), (112, 76, 28))
        rijen = TEKENS[naam]
        for y in range(16):
            for x in range(16):
                if rijen[y][x] == "#":
                    a[y, x, :3] = licht
                    if y + 1 < 16 and rijen[y + 1][x] != "#":
                        a[y + 1, x, :3] = a[y + 1, x, :3] * 0.45 + np.array(donker) * 0.25      # the shadow of the groove
        h.save(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)), "block", f"ringh3_rune_{naam}.png")
    # Brokkelsteen: paler than the stone round it (so the path can be read in the dark), cracked; cracking: glowing splits
    for naam, barsten, gloed in (("ringh3_brokkelsteen", 5, False), ("ringh3_brokkelsteen_barst", 11, True)):
        a = basis.copy()
        a[..., :3] = np.clip(a[..., :3] * 1.5 + 26, 0, 255)
        for i in range(barsten):
            x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
            for stap in range(int(rng.integers(4, 9))):
                a[y % 16, x % 16, :3] = (255, 150, 40) if gloed and stap % 3 else (28, 22, 22)
                x += int(rng.integers(-1, 2))
                y += 1 if rng.random() < 0.7 else 0
        h.save(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)), "block", f"{naam}.png")
    # the lever: a knob of cheese gold
    knop = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(knop)
    d.rectangle((0, 0, 15, 15), fill=(214, 164, 62, 255))
    d.rectangle((0, 0, 15, 2), fill=(250, 214, 110, 255))
    d.rectangle((0, 13, 15, 15), fill=(150, 104, 30, 255))
    for x in range(2, 16, 4):
        d.line((x, 3, x, 12), fill=(184, 134, 44, 255))
    h.save(knop, "block", "ringh3_hendel_knop.png")


def _el(van, tot, tex, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": van, "to": tot, "faces": {f: {"texture": tex} for f in faces}}


def blocks_and_items(h):
    A = h.A
    # --- the rune stone: eight signs ---
    for naam in TEKEN_NAMEN:
        h.w(f"{A}/models/block/ringh3_rune_{naam}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/ringh3_rune_{naam}"}})
    h.w(f"{A}/blockstates/ringh3_rune.json", {"variants": {f"teken={i}": {"model": f"guhs:block/ringh3_rune_{naam}"} for i, naam in enumerate(TEKEN_NAMEN)}})
    h.w(f"{A}/models/item/ringh3_rune.json", {"parent": "guhs:block/ringh3_rune_njeg"})
    h.self_drop("ringh3_rune")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:ringh3_rune"])
    # --- Brokkelsteen: whole, cracking, gone (nothing to draw) ---
    h.w(f"{A}/models/block/ringh3_brokkelsteen.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/ringh3_brokkelsteen"}})
    h.w(f"{A}/models/block/ringh3_brokkelsteen_barst.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/ringh3_brokkelsteen_barst"}})
    h.w(f"{A}/models/block/ringh3_brokkelsteen_weg.json", {"textures": {"particle": "guhs:block/ringh3_brokkelsteen"}, "elements": []})
    h.w(f"{A}/blockstates/ringh3_brokkelsteen.json", {"variants": {
        "staat=0": {"model": "guhs:block/ringh3_brokkelsteen"}, "staat=1": {"model": "guhs:block/ringh3_brokkelsteen_barst"},
        "staat=2": {"model": "guhs:block/ringh3_brokkelsteen_weg"}}})
    h.w(f"{A}/models/item/ringh3_brokkelsteen.json", {"parent": "guhs:block/ringh3_brokkelsteen"})
    # --- the lever: a plate on the wall (the back = north side of the block), an arm that points up, or down when pulled ---
    ij, steen, knop = "#ijzer", "#steen", "#knop"
    plaat = [_el([4, 2, 0], [12, 14, 1.5], steen), _el([5, 3, 1.5], [11, 13, 2.5], ij), _el([6.5, 7, 2.5], [9.5, 9, 4.5], ij)]
    op = plaat + [_el([7.25, 8, 3], [8.75, 13.5, 4.5], ij), _el([7.25, 12, 4.5], [8.75, 13.5, 8], ij), _el([6.5, 11.25, 8], [9.5, 14.25, 10], knop)]
    neer = plaat + [_el([7.25, 2.5, 3], [8.75, 8, 4.5], ij), _el([7.25, 2.5, 4.5], [8.75, 4, 8], ij), _el([6.5, 1.75, 8], [9.5, 4.75, 10], knop)]
    tex = {"particle": "guhs:block/roosterijzer", "ijzer": "guhs:block/roosterijzer", "steen": "guhs:block/gebeitelde_houtskoolsteen_stenen",
           "knop": "guhs:block/ringh3_hendel_knop"}
    scherm = {"gui": {"rotation": [30, 135, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
              "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}, "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]},
              "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
              "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [0.5, 0.5, 0.5]}}
    h.w(f"{A}/models/block/ringh3_hendel.json", {"parent": "minecraft:block/block", "textures": tex, "elements": op, "display": scherm})
    h.w(f"{A}/models/block/ringh3_hendel_om.json", {"parent": "minecraft:block/block", "textures": tex, "elements": neer})
    draai = {"south": 0, "west": 90, "north": 180, "east": 270}
    varianten = {}
    for facing, y in draai.items():
        for nr in range(4):
            for om in ("false", "true"):
                v = {"model": "guhs:block/ringh3_hendel" + ("_om" if om == "true" else "")}
                if y:
                    v["y"] = y
                varianten[f"facing={facing},nr={nr},om={om}"] = v
    h.w(f"{A}/blockstates/ringh3_hendel.json", {"variants": varianten})
    h.w(f"{A}/models/item/ringh3_hendel.json", {"parent": "guhs:block/ringh3_hendel"})


def texts(h):
    for key, text in tekst.TEKSTEN.items():
        h.lang(key, text, text)
    verhaal_motor.verhaallijn(h, "ring_h3", tekst.NAAM, tekst.UITLEG, stappen=tekst.STAPPEN, klaar=tekst.KLAAR, kort=tekst.KORT)
    verhaal_motor.vertelkaart(h, "ring_h3", tekst.KAART_TITEL, tekst.KAART_REGELS, kaart())
    for s in scene.alle():
        verhaal_motor.scene(h, s.id, s.titel, s.teksten, namen=s.namen)


def kaart():
    """The narrator card's map: the black plain, the mountains nobody gets over, and under them the gate with its arch of light."""
    k = verhaal_motor.Kaart(seed=2130180)
    k.land([(6, 150), (8, 96), (28, 62), (70, 40), (130, 28), (196, 34), (244, 60), (250, 120), (232, 152), (120, 156)], (150, 132, 120, 255))
    k.bos(14, 100, 30, 26, 8, (150, 96, 70, 255))                     # the Worstenwoud behind them
    k.huisje(30, 118, (236, 226, 200, 255))                          # Guhvendel
    for x, y, hoog in ((112, 62, 26), (134, 54, 34), (158, 60, 30), (182, 66, 22), (96, 74, 16), (204, 78, 14)):
        k.berg(x, y, hoog, (86, 78, 84, 255))
    k.pad([(30, 118), (58, 112), (84, 104), (112, 98), (136, 92)])
    # the gate: a dark arch in the foot of the mountains, runes of light over it
    k.d.rectangle((131, 82, 143, 96), fill=(30, 24, 30, 255), outline=(60, 50, 56, 255))
    k.d.arc((128, 72, 146, 92), 180, 360, fill=(120, 200, 255, 255), width=2)
    for x in (132, 137, 142):
        k.d.point((x, 78), fill=(220, 240, 255, 255))
    # what waits underneath: a red glow in the deep
    k.d.ellipse((166, 100, 190, 112), fill=(200, 80, 30, 255), outline=(90, 30, 20, 255))
    k.d.ellipse((172, 103, 184, 109), fill=(250, 180, 60, 255))
    k.d.line((143, 94, 166, 104), fill=(74, 50, 32, 255), width=1)
    k.d.line((190, 104, 222, 92), fill=(74, 50, 32, 255), width=1)
    k.kruis(224, 90)
    k.tekst(104, 118, "Knabbelmoria")
    k.tekst(12, 140, "Houtskoolvlakte")
    k.kompasroos(232, 136)
    return k


def sounds(h):
    def patch(d):
        for event, entries in tekst.SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    for name, parent, icon, frame, titel, text, verborgen in tekst.ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, text, hidden=verborgen)
        bbq2.verborgen(h, name)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)


def structuur(h):
    b = bouw.bouw(h)
    problems = bouw.check(b) + scene_check(b)
    if problems:
        raise SystemExit("ring_h3: the mine is not right:\n  " + "\n  ".join(problems[:40]))
    b.s.save(STRUCTUUR)
    for naam in bouw.DEUREN:
        bouw.deur_template(h, b, naam).save(f"ringh3_deur_{naam}")
    bouw.test_template(h).save("ringh3_test_kamer")
    titel, tooltip = tekst.STRUCTUUR
    # exactly one copy per world, a walk away from Guhvendel (the story chain, CONTRACT_130 3); no random spread
    if VORIGE in wereld.STRUCTUREN:
        gegarandeerd = dict(rond=VORIGE, min=250, max=500)
    else:
        gegarandeerd = dict(sector=NOOD_SECTOR, min=250, max=700)
    wereld.bbq_structuur(h, STRUCTUUR, soort="grot", titel=titel, tooltip=tooltip, biomes=["houtskoolvlakte"], salt=SALT,
                         templates=[(STRUCTUUR, 1)], gegarandeerd=gegarandeerd, voorrang=VOORRANG, kompas=None, grootte=28, vlak=8, hoogte=12)

    # the pool says where the ground is (the cave floor's top block is template y = G: everything under it is the mine), and
    # nothing spawns in the halls: no monsters, no Sauslopers, nothing that isn't ours
    def grond(pool):
        for e in pool["elements"]:
            el = {"element_type": "guhs:grond_single_pool_element"}
            el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
            el["ground_level_delta"] = bouw.G + 1
            e["element"] = el
    h.patch_json(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", grond)

    def leeg(s):
        s["spawn_overrides"] = {soort: {"bounding_box": "piece", "spawns": []} for soort in ("monster", "creature", "ambient")}
        # the mine's own structure type (feature/ringh3/MijnStructure): a barbecueput that stands on the LOWEST cave floor of its
        # column, so the 30 blocks under it are rock and sauce sea, not open cave
        s["type"] = TYPE
    h.patch_json(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", leeg)
    h.RUIMTE_HOOKS[TYPE] = lambda s, jigsaw_reach: jigsaw_reach(s, True)      # (make_v2.bouwruimte: keep_clear, like a barbecueput)
    verhaal_motor.sluier(h, STRUCTUUR)


def scene_check(b):
    """Every camera of the two scenes hangs in the air inside the mine and sees what it looks at; every actor stands on something
    (the two that fall excepted)."""
    problems = []
    blocks = b.s.blocks

    def lucht(p):
        q = (int(p[0] // 1), int(p[1] // 1), int(p[2] // 1))
        return q in blocks and blocks[q][0] == bouw.AIR
    for s in scene.alle():
        for t, pos, kijk, volgt, knip in s.camera:
            if not lucht(pos):
                problems.append(f"{s.id}: the camera of t={t} is in the rock at {pos}")
                continue
            doel = kijk if kijk else tuple(v + (1.0 if i == 1 else 0.0) for i, v in enumerate(s.plek(volgt, t)))
            for i in range(1, 12):
                f = i / 12.0 * 0.5        # (the first half of the line of sight must be free)
                p = tuple(pos[k] + (doel[k] - pos[k]) * f for k in range(3))
                if not lucht(p) and p[1] > 2:
                    q = (int(p[0] // 1), int(p[1] // 1), int(p[2] // 1))
                    if blocks.get(q, ("rock",))[0] not in (bouw.KETTING, bouw.LANTAARN, bouw.ZIELLAMP, bouw.HEK, bouw.TRALIES, bouw.SMEUL):
                        problems.append(f"{s.id}: the camera of t={t} looks into {blocks.get(q, ('rock',))[0]} at {q}")
                        break
        staan = [(naam, start, 0) for naam, soort, arg, start, yaw in s.acteurs] + [(a, naar, t1) for a, t0, t1, naar in s.lopen]
        for naam, p, t in staan:
            if p[1] < bouw.DIEP - 0.5:
                continue                   # (falling into the chasm)
            if not lucht(p) or lucht((p[0], p[1] - 0.6, p[2])):
                problems.append(f"{s.id}: {naam} does not stand at {p} (t={t})")
        for t, spreker, key, ticks in s.zinnen:
            if spreker and spreker not in [a[0] for a in s.acteurs]:
                problems.append(f"{s.id}: nobody is called {spreker}")
        if max([c[0] for c in s.camera] + [z[0] + z[3] for z in s.zinnen]) > s.duur:
            problems.append(f"{s.id}: something happens after its end ({s.duur})")
    return problems


def selfcheck(h):
    problems = modellen.check(h) + java.check()
    for key in ("entity.guhs.barbecuerog", "block.guhs.ringh3_rune", "gui.guhs.verhalen.ring_h3.naam", "gui.guhs.verhalen.ring_h3.kort.6",
                "gui.guhs.verhaal.kaart.ring_h3.regel.3", "scene.guhs.ringh3_brug.you_4", "scene.guhs.ringh3_emmer.titel",
                f"structure.guhs.{STRUCTUUR}", "quest.guhs.ring.terug.barbecuerog", "advancements.guhs.knabbelring.ring_h3_brug.title"):
        if key not in h.NL:
            problems.append(f"lang {key}")
    # the one line that is English in both languages
    if h.NL.get("scene.guhs.ringh3_brug.you_4") != scene.ZIN_YOU or scene.ZIN_YOU != "YOU.. SHALL.. NOT.. VADS!":
        problems.append("Guhdalf's line on the bridge must be exactly YOU.. SHALL.. NOT.. VADS!")
    for name in [STRUCTUUR, "ringh3_test_kamer"] + [f"ringh3_deur_{n}" for n in bouw.DEUREN]:
        if not os.path.exists(f"{h.D}/structure/{name}.nbt"):
            problems.append(f"template {name}")
    pool = json.load(open(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", encoding="utf-8"))
    if any(e["element"].get("ground_level_delta") != bouw.G + 1 for e in pool["elements"]):
        problems.append("the start pool has no ground_level_delta")
    if json.load(open(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", encoding="utf-8"))["type"] != TYPE:
        problems.append(f"the structure is not a {TYPE}")
    if not os.path.exists(f"{h.D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json") or os.path.exists(f"{h.D}/worldgen/structure_set/{STRUCTUUR}.json"):
        problems.append("the mine must have a guaranteed copy and no random spread")
    sounds_json = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for e in tekst.SOUNDS:
        if e not in sounds_json:
            problems.append(f"sounds.json misses {e}")
    # what Java says about the texts of this module (the keys it sends)
    bron = ""
    for naam in ("Raadsels.java", "Rollen.java", "MijnEvents.java", "Brug.java", "Achtervolging.java", "BarbecuerogEntity.java"):
        bron += open(os.path.join(java.JAVA, naam), encoding="utf-8").read()
    import re
    for key in sorted(set(re.findall(r'"((?:quest|gui)\.guhs\.ringh3\.[a-z0-9_.]+)"', bron))):
        if key.endswith("."):
            if not any(k.startswith(key) for k in h.NL):
                problems.append(f"no text starts with {key}")
        elif key not in h.NL:
            problems.append(f"lang {key} (used in Java)")
    if problems:
        raise SystemExit("ring_h3 self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    textures(h)
    blocks_and_items(h)
    modellen.build(h)
    texts(h)
    sounds(h)
    advancements(h)
    structuur(h)
    selfcheck(h)


# =====================================================================================================================
# FTB: the section "De Mijnen van Knabbelmoria" of the chapter guhs_knabbelring
# =====================================================================================================================
def ftb(fq):
    """The chapter is one chain in quest order (FTB_LINEAIR). So: the six quests of the way through FIRST (each comes after the
    one before by itself, the first after the last quest of ring_h2), then the side quests (each names what it hangs on: a
    branch that locks nothing), and the way out LAST (it names the bridge): the first quest of ring_h4 comes after it."""
    q, adv = fq.q, fq.adv
    q("ring_h3_reis", "Eronderdoor",
      "Over de berg kan niet, eromheen duurt te lang. Loop met Sam-guh naar de &6Mijnen van Knabbelmoria&r: het &dSuperkompas&r (Mijn verhaal) wijst "
      "de weg, een flink eind lopen van Guhvendel. Tot je er bent zie je alleen Guhdalfs rook.",
      "guhs:houtskoolsteen_stenen", [adv("ring_h3_stap_1")], shape="gear")
    q("ring_h3_poort", "Zeg njeg en treed binnen",
      "De poort zit potdicht en Guhdalf weet het ook niet meer. Boven de deur gloeit een spreuk. Lees hem &6heel letterlijk&r en typ het woord in de "
      "chat. (Of klik op de gloeiende runen en kies het goede antwoord.)",
      "guhs:ringh3_rune", [adv("ring_h3_stap_2")])
    q("ring_h3_hefbomen", "De Hal van de Hefbomen",
      "Vier hefbomen, elk onder een teken. Op de steen in het midden staat een rijmpje over hoe een dwerg-guh eet: trek de hefbomen in &6die volgorde&r "
      "over. Een foute hefboom en alles springt terug. Iedereen lost dit raadsel zelf op.",
      "guhs:ringh3_hendel", [adv("ring_h3_stap_3")])
    q("ring_h3_put", "Dwaas van een Pippguh",
      "Door het valhek ligt een oude wachtkamer met een put. Pippguh ziet een emmertje op de rand staan. &cNiemand komt aan dat emmertje.&r Njeg.",
      "minecraft:bucket", [adv("ring_h3_stap_4")])
    q("ring_h3_gang", "Gimguhs geheime doorgang",
      "De gang is ingestort en de trommels komen dichterbij. Praat met &dGimguh&r: er zit een dwergendeur in de muur. Klop &6drie keer&r op de rune van "
      "wat Durguh het lekkerst vond. Zijn tombe staat in dezelfde kamer...",
      "minecraft:lantern", [adv("ring_h3_stap_5")])
    q("ring_h3_brug", "YOU.. SHALL.. NOT.. VADS!",
      "In de Zuilenhal wordt iets wakker: de &cBarbecuerog&r, een demon van houtskool en vuur. Hij doet je niks (hij zet je hooguit terug bij je "
      "rustvuurtje), maar &6ren&r: over het &6Brokkelpad&r, dat onder je pootjes wegvalt, en de smalle &6Brug van Knabbel-dûm&r. Aan de overkant... kijk maar.",
      "minecraft:blaze_rod", [adv("ring_h3_stap_6")], rewards=(("guhs:kaas_knabbels", 12),), shape="gear", xp=100)
    q("ring_h3_njeg", "Gewoon zeggen", "Je zei het woord hardop in de chat, zoals het er staat. Guhdalf deed er drie spreuken over.",
      "minecraft:writable_book", [adv("ring_h3_njeg")], rewards=(("guhs:kaas_knabbels", 4),), deps=["ring_h3_poort"])
    q("ring_h3_val", "Even de diepte in", "Het Brokkelpad houdt het precies lang genoeg uit. Meestal. Val je, dan sta je zo weer bij je rustvuurtje: "
      "je verliest niks.", "guhs:gloeikool", [adv("ring_h3_gevallen")], rewards=(("guhs:kaas_knabbels", 4),), deps=["ring_h3_gang"])
    q("ring_h3_zweep", "Aan de worst geregen", "De zweep van de Barbecuerog is een rij aan elkaar geknoopte braadworstjes. Dat merk je als hij je "
      "ermee pakt. Je ruikt daarna een uur naar barbecue.", "minecraft:lead", [adv("ring_h3_gepakt")], rewards=(("guhs:kaas_knabbels", 4),),
      deps=["ring_h3_gang"])
    q("ring_h3_buiten", "Naar buiten",
      "Guhdalf is gevallen. Klim de &6lange trap&r op naar de oostpoort en praat buiten met &dAraguh&r. Gimguh hakte nog vier &6runenstenen&r voor je uit de "
      "muur: klop erop voor een ander teken. De scènes kun je in de &dGuhdex&r opnieuw bekijken.",
      "guhs:ringh3_rune", [adv("ring_h3_stap_7")], rewards=(("guhs:kaas_knabbels", 16),), deps=["ring_h3_brug"], shape="gear", xp=150)
