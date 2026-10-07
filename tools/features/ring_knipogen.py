"""
bbq2 (ring-knipogen): the seven "knipogen" of the Knabbelring and Super Guhrio: winks at the older stories, each a
mini-cutscene of five to ten seconds that a player sees once (Java: feature/ringknipoog; the scenes are in Knipogen.java).

  build(h)   the Dutch texts of the seven scenes (their titles are the "Opnieuw bekijken" buttons in the Guhdex), the two
             lines about Sjokkel on the bridge of the mine, the test room, and the self-check.

No FTB quests and no Guhdex page on purpose: a wink is a surprise, not a task. The self-check reads Knipogen.java and
proves what nobody has seen in the game yet, as far as the templates can prove it:
  - every scene lasts five to ten seconds, says only lines that have a text and has a text for every speaker;
  - no actor is put inside a block, no camera stands (or glides) inside or against a block of the building it plays in,
    and nothing of that building stands between a camera and what it looks at (the scene's frame is the anchor of the
    chapter scene it follows: read from the chapter's own files, so a template that moves is noticed here);
  - the ?-block with the medicine chest is where Knipogen.KISTJE_S says, and Sjokkel's line lies on the bridge head;
  - the four spots where a chapter plays a wink are still there (a merge that loses one fails the build).
"""
import json
import math
import os
import re

from features import verhaal_motor

PKG = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature")
T = "quest.guhs.ringknipoog."

# id -> (title, lines {key: Dutch}, speakers {actor: name})
SCENES = {
    "ringknipoog_baltoguh": (
        "Een verkeerde afslag",
        {"afslag": "Hmmm. Volgens mij ben ik een verkeerde afslag afgevadst... Hier is geen medicijnenkistje..."},
        {"balto": "Baltoguh"}),
    "ringknipoog_kistje": (
        "Het medicijnenkistje",
        {"zocht": "Daar zocht iemand naar, njeg."},
        {"padguh": "Pad-guh"}),
    "ringknipoog_spiegel": (
        "Nog één ding in de spiegel",
        {"giechel": "Hihihi! Njeg!",
         "visioen": "De spiegel toont nog één ding: Mewtwo-guh, aan een dubbele portie."},
        {"mew": "Mieuwguh"}),
    "ringknipoog_stitch": (
        "Wie laat dat ding binnen?",
        {"aloha": "Aloha, njeg!",
         "wie": "WIE laat dat ding steeds binnen?!"},
        {"stitch": "626-guh", "sausuman": "Sausuman"}),
    "ringknipoog_boris": (
        "Dat is mijn moment",
        {"berg": "Een normale guh kan deze berg niet op... maar heel misschien...",
         "moment": "Hé, dat is mijn moment."},
        {"boris": "Boris", "sam": "Sam-guh"}),
    "ringknipoog_sjokkel": (
        "Sjokkel is er ook",
        {"ver": "En daar... komt nog iemand aan. Heel. Langzaam.",
         "sjokkel": "Het is Sjokkel! Hij is al onderweg sinds de brug in Knabbelmoria.",
         "toetje": "Precies op tijd voor het toetje, njeg!"},
        {"sam": "Sam-guh"}),
    "ringknipoog_kloon": (
        "Twee ringen",
        {"twee": "Twee ringen is twee keer zo lekker!",
         "nee": "Nee."},
        {"kloon": "Professor Knabbelkloon", "guhdalf": "Guhdalf"}),
}

TEKST = {
    # the action bar, once, for a player in the great hall of the mine who comes near the bridge head
    T + "sjokkel.brug": "Kijk nou: daar sjokt Sjokkel de brug op. Heel. Langzaam.",
    # a click on him there
    T + "sjokkel.klik": "Sjokkel is onderweg naar een feest. Stoor hem maar niet, njeg.",
}

# The four spots where a chapter plays a wink (file under feature/, the text that has to be in it).
HAKEN = [
    ("ringh2/Guhvendel.java", "Knipogen.speel(p, RingH2Scenes.RAAD, Knipogen.BALTOGUH, o.anker(), o.draai(), Guhvendel::naRaad)"),
    ("ringh4/Spiegel.java", "Knipogen.speel(p, SCENE, Knipogen.SPIEGEL, pos, draai, s -> {"),
    ("ringh6/Thuis.java", "Knipogen.speel(p, Finale.FEEST, Knipogen.SJOKKEL, anker, Rotation.NONE, Thuis::klaar)"),
    ("ringh6/Klim.java", 'Knipogen.boris(p, berg.wereld("richel_3"), berg.draai(), q -> draag(q, berg))'),
]

# Scene positions that are inside a block on purpose (an actor that waits out of sight), per scene method.
VERBORGEN = {
    "kistjeScene": {(0.5, 0.25, 0.5), (2.4, -4.9, 0.5)},        # the chest in its ?-block, Pad-guh under the ground
    "spiegel": {(0.5, -0.95, 0.5)},                              # the vision in the mirror's foot
}

# Blocks a camera sees through and an actor stands in: thin things and plants.
DUN = ("air", "carpet", "sign", "lantern", "chain", "button", "guh_wire", "torch", "bloem", "flower", "tulip", "daisy", "bluet", "lily_of_the_valley",
       "scheutjes", "zwammetje", "roos", "banner", "lightning_rod", "rookgat", "kooislot", "elfentouw_haak", "guhrio_munt", "guhrio_vlag",
       "guhriow1_lucht", "guhriow1_tip")


def texts(h):
    for scene_id, (titel, regels, namen) in SCENES.items():
        verhaal_motor.scene(h, scene_id, titel, regels, namen=namen)
    for key, text in TEKST.items():
        h.lang(key, text, text)


def kamer(h):
    """The game test room: a bare floor of 25 x 25 (the tests lay their own frames of the chapters over it)."""
    t = h.Structure((25, 9, 25))
    for x in range(25):
        for z in range(25):
            t.set(x, 0, z, "minecraft:smooth_quartz")
    t.save("ringknipoog_test_kamer")


# =====================================================================================================================
# the self-check
# =====================================================================================================================
def _template(h, naam):
    """The blocks {(x, y, z): name} of a generated template: one .nbt, or a build in tiles of 32 (<naam>/stuk_i_j)."""
    basis = os.path.join(h.D, "structure")
    blocks = {}
    map_ = os.path.join(basis, naam)
    if os.path.isdir(map_):
        bestanden = []
        for f in sorted(os.listdir(map_)):
            m = re.fullmatch(r"stuk_(\d+)_(\d+)\.nbt", f)
            if m:
                bestanden.append((os.path.join(map_, f), int(m.group(1)) * 32, int(m.group(2)) * 32))
    else:
        bestanden = [(os.path.join(basis, naam + ".nbt"), 0, 0)]
    for pad, ox, oz in bestanden:
        root = h.read_nbt(pad)
        palette = root["palette"] if "palette" in root else root["palettes"][0]
        for b in root["blocks"]:
            x, y, z = b["pos"]
            blocks[(x + ox, y, z + oz)] = palette[b["state"]]["Name"]
    return blocks


def _ankers(h):
    """Scene method -> (template, anchor block): every anchor is read from the file of the chapter it belongs to."""
    def java(pad, patroon):
        tekst = open(os.path.join(PKG, pad), encoding="utf-8").read()
        m = re.search(patroon, tekst)
        if not m:
            raise SystemExit(f"ring_knipogen: {pad} has no {patroon}")
        return tuple(int(v) for v in m.groups())
    plekken = json.load(open(os.path.join(h.D, "ringh4", "plekken.json"), encoding="utf-8"))["plekken"]
    berg = json.load(open(os.path.join(h.D, "ringh6", "berg.json"), encoding="utf-8"))["plekken"]
    s = java(os.path.join("ringknipoog", "Knipogen.java"), r"KISTJE_S = (\d+);")[0]
    return {
        "baltoguh": ("guhvendel", java(os.path.join("ringh2", "Guhvendel.java"), r"KRING = new BlockPos\((\d+), (\d+), (\d+)\)")),
        # the level 1-1 as its test template has it: cell (s, row) at (1 + s, 2 + row, 1), the ?-block in row 6, the camera's side +z
        "kistjeScene": ("guhriow1_test_1_1", (1 + s, 2 + 6, 1)),
        "spiegel": ("guhladriel_boomstad", tuple(int(v) for v in plekken["spiegel"])),
        "stitchScene": ("sausuman_toren", java(os.path.join("ringsausuman", "Toren.java"), r"BAKKER = new BlockPos\((\d+), (\d+), (\d+)\)")),
        "boris": ("frituurberg", tuple(int(v) for v in berg["richel_3"])),
        "sjokkel": (None, None),                                  # open ground at home: nothing to check against
        "kloonScene": ("knabbelmoria", java(os.path.join("ringh3", "Plekken.java"), r"POORT_BUITEN = new BlockPos\((\d+), (\d+), (\d+)\)")),
    }


GETAL = r"(-?[\d.]+)"
VEC = rf"new Vec3\({GETAL}, {GETAL}, {GETAL}\)"


def _methodes(java):
    """Scene method name -> its text (from 'private static Cutscene <name>()' to its registreer())."""
    uit = {}
    for m in re.finditer(r"private static Cutscene (\w+)\(\) \{", java):
        eind = java.index(".registreer();", m.end())
        uit[m.group(1)] = java[m.end():eind]
    return uit


def _vec(tekst, namen):
    """A Vec3 argument: a literal, or a local constant of the scene."""
    m = re.fullmatch(VEC, tekst.strip())
    if m:
        return tuple(float(v) for v in m.groups())
    return namen.get(tekst.strip())


def geometrie(h, java):
    problems = []
    ankers = _ankers(h)
    methodes = _methodes(java)
    for naam in ankers:
        if naam not in methodes:
            problems.append(f"Knipogen.java has no scene method {naam}()")
    for naam, tekst in methodes.items():
        if naam not in ankers:
            problems.append(f"scene method {naam}() has no anchor in ring_knipogen.py")
            continue
        template, anker = ankers[naam]
        if template is None:
            continue
        blocks = _template(h, template)
        if not blocks:
            problems.append(f"{naam}: the template {template} is empty")
            continue
        ax, ay, az = anker
        verborgen = VERBORGEN.get(naam, set())
        # (a level's lane is one block deep and its camera's room, in front of it, is air by the lane builder's own rule:
        #  the test template of level 1-1 only holds the lane and one cell of that room)
        alleen_baan = template.startswith("guhriow1_test")

        def blok(x, y, z):
            return blocks.get((ax + math.floor(x), ay + math.floor(y), az + math.floor(z)))

        def dicht(x, y, z, camera=False, blik=False):
            """Something fills this point. camera: what the template does not say counts as filled too; blik: for a line
            of sight (a fence, bars or a pane do not hide what is behind them)."""
            n = blok(x, y, z)
            if alleen_baan and (math.floor(z) != 0 or n is None):
                return False
            if n is None:
                return camera or blik
            kort = n.split(":")[1]
            if any(d in kort for d in DUN):
                return False
            if kort.endswith("_slab") or "kussen" in kort:
                return (y % 1.0) < 0.5
            if "spiegel" in kort:
                return (y % 1.0) < 0.69
            return not (blik and any(d in kort for d in ("_hek", "fence", "tralies", "_pane")))
        namen = {m.group(1): tuple(float(v) for v in m.group(2, 3, 4)) for m in re.finditer(rf"(\w+) = {VEC}", tekst)}
        # 1. the actors: where they start, where they walk to, and the way between (an actor that waits out of sight inside a
        #    block comes out of it in a straight line: that stretch is not looked at)
        arg = rf"({VEC}|\w+)"
        var = dict(re.findall(r'(\w+) = "(\w+)"', tekst))

        def wie_is(naam_):
            naam_ = naam_.strip('"')
            return "speler" if naam_.endswith("SPELER") else var.get(naam_, naam_)
        start, lopen = {}, []
        for m in re.finditer(rf"\.speler\({arg},", tekst):
            start["speler"] = m.group(1)
        for m in re.finditer(rf"\.(?:npc|guh|acteur)\((\w+|\"\w+\"), [^,]+(?:\(\) -> [\w.]+)?, {arg},", tekst):
            start[wie_is(m.group(1))] = m.group(2)
        for m in re.finditer(rf"\.loop\(([\w.]+|\"\w+\"), (\d+), \d+, {arg}\)", tekst):
            lopen.append((int(m.group(2)), wie_is(m.group(1)), m.group(3)))
        if len(start) < 3 or len(lopen) != tekst.count(".loop("):
            problems.append(f"{naam}: {len(start)} actors and {len(lopen)} of {tekst.count('.loop(')} walks were read")
        waar = {}
        for wie, p in start.items():
            pos = _vec(p, namen)
            if pos is None:
                problems.append(f"{naam}: the position {p} of {wie} is not a Vec3 the self-check can read")
                continue
            waar[wie] = pos
            if pos not in verborgen and dicht(pos[0], pos[1] + 0.05, pos[2]):
                problems.append(f"{naam}: {wie} starts at {pos}, inside {blok(pos[0], pos[1] + 0.05, pos[2])}")
        for _, wie, p in sorted(lopen):
            pos = _vec(p, namen)
            if pos is None or wie not in waar:
                problems.append(f"{naam}: the walk of {wie} to {p} can not be read")
                continue
            van, waar[wie] = waar[wie], pos
            if van in verborgen or pos in verborgen:
                continue
            for n in range(1, 13):
                q = tuple(van[k] + (pos[k] - van[k]) * n / 12.0 for k in range(3))
                if dicht(q[0], q[1] + 0.05, q[2]):
                    problems.append(f"{naam}: {wie} walks from {van} to {pos} through {blok(q[0], q[1] + 0.05, q[2])} at {tuple(round(v, 2) for v in q)}")
                    break
        # 2. the cameras
        punten = []
        for m in re.finditer(rf"\.camera(Knip)?\((\d+), {VEC}, {VEC}\)", tekst):
            punten.append((int(m.group(2)), tuple(float(v) for v in m.group(3, 4, 5)), tuple(float(v) for v in m.group(6, 7, 8)), bool(m.group(1))))
        if "cameraVolgt" in tekst or "cameraKnipVolgt" in tekst:
            problems.append(f"{naam}: a camera that follows an actor: the self-check only reads cameras that look at a point")
        punten.sort()
        if len(punten) < 2:
            problems.append(f"{naam}: only {len(punten)} camera points were read")
        for i, (tijd, pos, kijk, knip) in enumerate(punten):
            stappen = [pos]
            if i + 1 < len(punten) and not punten[i + 1][3]:
                volgende = punten[i + 1][1]
                stappen += [tuple(pos[k] + (volgende[k] - pos[k]) * n / 12.0 for k in range(3)) for n in range(1, 12)]
            for (x, y, z) in stappen:
                raak = [blok(x + dx, y + dy, z + dz) or "what the template does not say" for dx, dz in ((0, 0), (0.3, 0), (-0.3, 0), (0, 0.3), (0, -0.3))
                        for dy in (0, -0.3) if dicht(x + dx, y + dy, z + dz, camera=True)]
                if raak:
                    problems.append(f"{naam}: the camera of tick {tijd} is in or against {raak[0]} at {tuple(round(v, 2) for v in (x, y, z))}")
                    break
            d = [kijk[k] - pos[k] for k in range(3)]
            lang = sum(v * v for v in d) ** 0.5
            n = max(2, int(lang / 0.2))
            for j in range(1, n):
                f = j / n
                if lang * (1 - f) < 1.0:
                    break                                         # (the last block: what it looks at)
                q = tuple(pos[k] + d[k] * f for k in range(3))
                if dicht(*q, blik=True):
                    problems.append(f"{naam}: the camera of tick {tijd} looks at {kijk} through {blok(*q) or 'what the template does not say'} "
                                    f"at {tuple(round(v, 2) for v in q)}")
                    break
    return problems


def selfcheck(h):
    problems = []
    for key in list(TEKST) + [f"scene.guhs.{i}.titel" for i in SCENES]:
        if key not in h.NL:
            problems.append(f"lang {key}")
    if not os.path.exists(os.path.join(h.D, "structure", "ringknipoog_test_kamer.nbt")):
        problems.append("the test room is missing")
    java = open(os.path.join(PKG, "ringknipoog", "Knipogen.java"), encoding="utf-8").read()
    # every scene: registered once, five to ten seconds, its lines and its speakers have a text
    ids = re.findall(r'Cutscene\.maak\("(\w+)"\)\.duur\((\d+)\)', java)
    if sorted(i for i, _ in ids) != sorted(SCENES):
        problems.append(f"Knipogen.java registers {sorted(i for i, _ in ids)}, the texts are for {sorted(SCENES)}")
    for scene_id, duur in ids:
        if not 100 <= int(duur) <= 200:
            problems.append(f"{scene_id} takes {duur} ticks: a wink is five to ten seconds")
    methodes = _methodes(java)
    for naam, tekst in methodes.items():
        m = re.search(r'Cutscene\.maak\("(\w+)"\)', tekst)
        if not m or m.group(1) not in SCENES:
            continue
        scene_id = m.group(1)
        _, regels, sprekers = SCENES[scene_id]
        lokaal = dict(re.findall(r'(\w+) = "(\w+)"', tekst))
        lokaal["speler"] = "speler"
        gezegd = set()
        for wie, key, ticks in re.findall(r'\.zeg\(\d+, ([\w".]+), "(\w+)", (\d+)\)', tekst):
            gezegd.add(key)
            acteur = wie.strip('"') if wie.startswith('"') else lokaal.get(wie, wie)
            if acteur and acteur not in sprekers:
                problems.append(f"{scene_id}: {acteur} says '{key}' and has no name")
            if key in regels and int(ticks) < 20 + len(regels[key]) * 0.55:
                problems.append(f"{scene_id}: '{key}' ({len(regels[key])} characters) stands only {ticks} ticks")
        for key in sorted(gezegd - set(regels)):
            problems.append(f"{scene_id} says '{key}', which has no text")
        for key in sorted(set(regels) - gezegd):
            problems.append(f"{scene_id}: the text '{key}' is never said")
        laatste = max([int(t) for t in re.findall(r"\.(?:zeg|animatie|geluid|deeltjes|kijk|schud|camera\w*)\((\d+),", tekst)]
                      + [int(t) for t in re.findall(r"\.loop\([\w\"]+, \d+, (\d+),", tekst)] + [0])
        duur = int(dict(ids)[scene_id])
        if laatste > duur:
            problems.append(f"{scene_id}: something happens at tick {laatste}, after its end at {duur}")
    problems += geometrie(h, java)
    # the medicine chest: a coin ?-block at KISTJE_S of the first lane of level 1-1
    s = int(re.search(r"KISTJE_S = (\d+);", java).group(1))
    lane = _template(h, "guhriow1_test_1_1")
    if lane.get((1 + s, 2 + 6, 1)) != "guhs:guhrio_vraagblok":
        problems.append(f"level 1-1 has no ?-block at s {s}, row 6: {lane.get((1 + s, 2 + 6, 1))}")
    w1 = open(os.path.join("tools", "features", "guhrio_w1_bouw.py"), encoding="utf-8").read()
    if f"baan.vraag({s}, Y + 3)\n" not in w1[w1.index("def bouw_1_1"):w1.index("def bouw_1_2")]:
        problems.append(f"guhrio_w1_bouw.bouw_1_1 has no coin ?-block at s {s}")
    # Sjokkel: his line lies on the west bridge head of the mine, with floor under it and air in it
    sjokkel = open(os.path.join(PKG, "ringknipoog", "Sjokkel.java"), encoding="utf-8").read()
    mijn = _template(h, "knabbelmoria")
    lijn = re.search(rf"VAN = {VEC}, NAAR = {VEC};", sjokkel)
    begin = re.search(r"BEGIN = new BlockPos\((\d+), (\d+), (\d+)\)", sjokkel)
    if not lijn or not begin:
        problems.append("Sjokkel.java: VAN / NAAR / BEGIN were not read")
    else:
        x0, y0, z0, x1, y1, z1 = (float(v) for v in lijn.groups())
        cellen = {(math.floor(x0 + (x1 - x0) * n / 8.0), math.floor(y0), math.floor(z0 + (z1 - z0) * n / 8.0)) for n in range(9)}
        cellen.add(tuple(int(v) for v in begin.groups()))
        for (x, y, z) in sorted(cellen):
            if mijn.get((x, y, z)) != "minecraft:air" or mijn.get((x, y + 1, z)) != "minecraft:air":
                problems.append(f"Sjokkel's line: {(x, y, z)} is not air ({mijn.get((x, y, z))})")
            if mijn.get((x, y - 1, z)) in (None, "minecraft:air"):
                problems.append(f"Sjokkel's line: no floor under {(x, y, z)}")
    # the spots where a chapter plays a wink
    for pad, haak in HAKEN:
        if haak not in open(os.path.join(PKG, *pad.split("/")), encoding="utf-8").read():
            problems.append(f"feature/{pad} no longer plays its wink: '{haak}' is gone")
    if problems:
        raise SystemExit("ring_knipogen self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    texts(h)
    kamer(h)
    selfcheck(h)
