"""
bbq2 (oude-scenes, DESIGN_VERHALENPAD B) - one camera scene for each older story: Balto, Mewtwo, Lilo & Stitch, the
Hemelkapelletje, the Grillguh and the Timmerguh. Java: feature/oudescenes.

  oude_scenes_scene.py   the six scripts, written once, in the coordinates of the stories' own templates
  oude_scenes_java.py    writes feature/oudescenes/Scenes.java from them
  oude_scenes_beeld.py   reads a template back from its .nbt (what the check asks) and draws frames of the scenes
  oude_scenes_wiki.py    the wiki notes

build(h) writes: the texts of the scenes (scene.guhs.oudescenes_<kort>.*: the title on the Guhdex's replay row, the lines, the
speakers), the picture behind each scene's picture-book replay (textures/gui/verhaal/kaart_oudescenes_<kort>.png: a little
parchment drawing of our own) and the empty test room of the game tests (oudescenes_test_kamer).

selfcheck(h): Scenes.java is what oude_scenes_java would write; every text exists; and scene_check: every camera of every scene
hangs in free air in the story's REAL template (the .nbt) and sees what it looks at, every actor stands on something. Reading
the big templates takes minutes, so a scene is only checked when its script or its template changed: what was checked is kept
in oude_scenes_check.json (a fingerprint of the script and of the template's content per scene).

  python tools/features/oude_scenes.py check     checks every scene now, whatever the record says (from the worktree root)
"""
import gzip
import hashlib
import json
import os
import sys

if __name__ == "__main__":
    sys.path.insert(0, "tools")          # (run as a tool from the worktree root)

from features import oude_scenes_java as java
from features import oude_scenes_scene as scene
from features import verhaal_motor

STRUCTURE = os.path.join("src", "main", "resources", "data", "guhs", "structure")
CHECK = os.path.join("tools", "features", "oude_scenes_check.json")
TESTKAMER = "oudescenes_test_kamer"
MIN_TICKS, MAX_TICKS = 300, 500          # 15 to 25 seconds (DESIGN_VERHALENPAD B)


# =====================================================================================================================
# the check against the templates
# =====================================================================================================================
def scene_check(w, s):
    """The problems of scene s in its template w (oude_scenes_beeld.Wereld): a camera in a wall or looking into one, an actor
    in the air or in a block, a speaker without a name, something after the end, a scene that is too short or too long."""
    problems = []
    for t in sorted(set(c[0] for c in s.camera) | set(range(0, s.duur + 1, 5))):
        pos, doel = s.camera_op(t)
        if not w.vrij(pos):
            problems.append(f"{s.id}: the camera of t={t} is in {w.blok(pos)} at {tuple(round(v, 2) for v in pos)}")
            continue
        # the first half of the line of sight is free (glass and thin things don't count)
        for i in range(1, 16):
            f = i / 16.0 * 0.5
            p = tuple(pos[k] + (doel[k] - pos[k]) * f for k in range(3))
            c = tuple(int(v // 1) for v in p)
            if w.binnen(p) and w.vol[c] and w.lo[c] <= p[1] - c[1] < w.hi[c]:
                problems.append(f"{s.id}: the camera of t={t} looks into {w.blok(p)} at {c}")
                break
    for a in s.acteurs:
        staan = [(a.start, 0)] + [(naar, t1) for naam, t0, t1, naar in s.lopen if naam == a.naam]
        for p, t in staan:
            if p[1] < s.anker[1] - scene.WEG / 2 or a.naam in s.vliegt:
                continue                   # (waiting out of sight, or a flyer)
            if not w.vrij((p[0], p[1] + 0.2, p[2])) or not w.vrij((p[0], p[1] + 0.9 * a.maat, p[2])):
                problems.append(f"{s.id}: {a.naam} stands in {w.blok((p[0], p[1] + 0.2, p[2]))} at {p} (t={t})")
            elif w.top(p[0], p[2], p[1] + 0.3) == w.top(p[0], p[2], p[1] + 0.3) and not (p[1] - 0.6 <= w.top(p[0], p[2], p[1] + 0.3) <= p[1] + 0.13):
                problems.append(f"{s.id}: {a.naam} does not stand on anything at {p} (t={t}): the floor is {w.top(p[0], p[2], p[1] + 0.3)}")
    wie = {a.naam for a in s.acteurs}
    for t, spreker, key, ticks in s.zinnen:
        if spreker and spreker != "speler" and spreker not in s.namen:
            problems.append(f"{s.id}: {spreker} speaks but has no name")
        if t + ticks > s.duur:
            problems.append(f"{s.id}: the line {key} runs past the end")
    for naam in [l[0] for l in s.lopen] + [k[0] for k in s.kijken] + [a[0] for a in s.animaties]:
        if naam not in wie:
            problems.append(f"{s.id}: nobody is called {naam}")
    laatste = max([c[0] for c in s.camera] + [g[0] for g in s.geluiden] + [st[1] for st in s.stromen] + [0])
    if laatste > s.duur:
        problems.append(f"{s.id}: something happens after its end ({laatste} > {s.duur})")
    if not MIN_TICKS <= s.duur <= MAX_TICKS:
        problems.append(f"{s.id}: {s.duur / 20.0} seconds (a scene of an old story is 15 to 25 seconds)")
    for t, veld, volume, pitch in s.geluiden:
        if veld not in java.GELUID:
            problems.append(f"{s.id}: no sound {veld}")
    if not all(w.binnen((s.anker[0] + 0.5, s.anker[1] + 0.5, s.anker[2] + 0.5)) for _ in (0,)):
        problems.append(f"{s.id}: the anchor {s.anker} is not in the template")
    return problems


def _vingerafdruk(s):
    """(script, template): what the check of scene s depends on."""
    script = json.dumps([s.template, s.grond, s.anker, s.duur, sorted(s.camera, key=str), [(a.naam, a.start, a.maat) for a in s.acteurs],
                         sorted(s.lopen, key=str), sorted(s.vliegt), sorted(s.zinnen), sorted(s.namen), sorted(s.geluiden, key=str),
                         sorted(st[1] for st in s.stromen)], sort_keys=True, default=str)
    with gzip.open(os.path.join(STRUCTURE, s.template + ".nbt")) as f:
        template = hashlib.sha1(f.read()).hexdigest()
    return {"script": hashlib.sha1(script.encode("utf-8")).hexdigest(), "template": template}


def check_alles(alles=False):
    """Checks every scene whose script or template changed since it was last checked (alles: every scene). Returns the
    problems; what passed is written to oude_scenes_check.json."""
    from features import oude_scenes_beeld as beeld
    bekend = json.load(open(CHECK, encoding="utf-8")) if os.path.exists(CHECK) else {}
    problems, nu = [], {}
    for s in scene.alle():
        if not os.path.exists(os.path.join(STRUCTURE, s.template + ".nbt")):
            problems.append(f"{s.id}: no template {s.template}")
            continue
        vinger = _vingerafdruk(s)
        if alles or bekend.get(s.id) != vinger:
            p = scene_check(beeld.Wereld(s.template, grond=s.grond), s)
            problems += p
            if p:
                continue
        nu[s.id] = vinger
    if nu != bekend:
        with open(CHECK, "w", encoding="utf-8", newline="\n") as f:
            json.dump(nu, f, indent=1, sort_keys=True)
            f.write("\n")
    return problems


# =====================================================================================================================
# the pictures behind the picture-book replay (256 x 160, parchment, our own drawings)
# =====================================================================================================================
INKT = (74, 50, 32, 255)


def _guhkop(k, x, y, r, kleur, ogen=(40, 60, 90, 255)):
    """A little guh head: round, two ears, two eyes."""
    for dx in (-r * 0.6, r * 0.6):
        k.d.ellipse((x + dx - r * 0.34, y - r * 1.5, x + dx + r * 0.34, y - r * 0.5), fill=kleur, outline=INKT)
    k.d.ellipse((x - r, y - r, x + r, y + r), fill=kleur, outline=INKT)
    for dx in (-r * 0.4, r * 0.4):
        k.d.ellipse((x + dx - 1, y - 2, x + dx + 1, y + 1), fill=ogen)


def _sterren(k, n, y_max, kleur=(120, 96, 60, 255)):
    for _ in range(n):
        x, y = k.rng.randint(8, 247), k.rng.randint(8, y_max)
        k.d.point((x, y), fill=kleur)
        if k.rng.random() < 0.3:
            k.d.line((x - 1, y, x + 1, y), fill=kleur)
            k.d.line((x, y - 1, x, y + 1), fill=kleur)


def kaart_balto(k):
    k.land([(0, 160), (0, 118), (60, 96), (128, 104), (196, 92), (256, 110), (256, 160)], kleur=(232, 236, 238, 255))
    k.berg(40, 100, 30, (214, 220, 226, 255)).berg(216, 96, 36, (214, 220, 226, 255)).berg(182, 98, 20, (220, 226, 232, 255))
    # the Wolvenrots with the white wolf-guh on it
    k.d.polygon([(104, 104), (112, 70), (140, 62), (152, 104)], fill=(170, 170, 176, 255), outline=INKT)
    _guhkop(k, 128, 50, 9, (250, 250, 252, 255))
    for a in range(0, 360, 45):
        import math
        k.d.line((128 + 14 * math.cos(math.radians(a)), 50 + 14 * math.sin(math.radians(a)),
                  128 + 18 * math.cos(math.radians(a)), 50 + 18 * math.sin(math.radians(a))), fill=(200, 170, 90, 255))
    _guhkop(k, 92, 122, 7, (150, 140, 130, 255))
    k.pad([(10, 140), (60, 132), (120, 138), (190, 130), (248, 136)])
    for _ in range(90):
        x, y = k.rng.randint(4, 251), k.rng.randint(6, 154)
        k.d.point((x, y), fill=(255, 255, 255, 255))
        k.d.point((x + 1, y), fill=(200, 208, 216, 255))


def kaart_mewtwo(k):
    k.water([(0, 160), (0, 128), (256, 128), (256, 160)])
    k.land([(30, 130), (50, 108), (206, 108), (226, 130)], kleur=(170, 200, 150, 255))
    # the dome and the tank in it
    k.d.pieslice((78, 40, 178, 176), 180, 360, fill=(214, 232, 236, 255), outline=INKT)
    for x in (103, 128, 153):
        k.d.line((x, 108, 128, 40), fill=INKT)
    k.d.rounded_rectangle((116, 70, 140, 108), 4, fill=(240, 150, 200, 255), outline=INKT)
    k.d.line((122, 74, 128, 84, 124, 90, 132, 100), fill=INKT)                                   # the crack
    for dx in (-4, 4):
        k.d.ellipse((128 + dx - 2, 86, 128 + dx + 2, 90), fill=(150, 70, 220, 255))
    k.toren(60, 108, 34)
    # lightning
    k.d.line((196, 8, 186, 30, 198, 32, 184, 60), fill=(220, 170, 40, 255), width=2)
    k.d.line((40, 6, 34, 22, 42, 24, 32, 44), fill=(220, 170, 40, 255), width=2)
    for _ in range(60):
        x, y = k.rng.randint(4, 250), k.rng.randint(6, 100)
        k.d.line((x, y, x - 1, y + 4), fill=(110, 130, 160, 255))


def kaart_ohana(k):
    k.water([(0, 160), (0, 120), (90, 126), (256, 112), (256, 160)])
    k.land([(60, 160), (70, 112), (256, 96), (256, 160)], kleur=(232, 214, 160, 255))
    # the stilt house, a palm, the fire and the two of them
    k.d.rectangle((176, 70, 226, 98), fill=(200, 170, 110, 255), outline=INKT)
    k.d.polygon([(168, 70), (201, 44), (234, 70)], fill=(214, 190, 96, 255), outline=INKT)
    for x in (180, 201, 222):
        k.d.line((x, 98, x, 112), fill=INKT, width=2)
    k.d.line((92, 118, 98, 76), fill=INKT, width=2)
    for dx, dy in ((-16, 4), (-10, -8), (4, -12), (16, -4), (14, 8)):
        k.d.line((98, 76, 98 + dx, 76 + dy), fill=(84, 130, 70, 255), width=2)
    k.d.polygon([(132, 132), (138, 116), (144, 132)], fill=(232, 130, 50, 255), outline=INKT)
    _guhkop(k, 156, 126, 7, (110, 150, 230, 255))
    _guhkop(k, 172, 126, 6, (250, 130, 130, 255))
    k.d.rectangle((146, 134, 152, 137), fill=(170, 110, 60, 255), outline=INKT)                # the picture book
    _sterren(k, 46, 60)
    k.d.ellipse((24, 14, 44, 34), fill=(244, 236, 200, 255), outline=INKT)                       # the moon


def kaart_hemel(k):
    for x, y, r in ((40, 122, 20), (70, 128, 24), (190, 124, 24), (220, 118, 18), (128, 134, 30)):
        k.d.ellipse((x - r, y - r // 2, x + r, y + r // 2), fill=(244, 240, 244, 255), outline=INKT)
    # the chapel on its cloud
    k.d.rectangle((104, 78, 152, 122), fill=(240, 232, 226, 255), outline=INKT)
    k.d.pieslice((100, 50, 156, 106), 180, 360, fill=(240, 170, 200, 255), outline=INKT)
    k.d.line((128, 50, 128, 38), fill=INKT)
    # the beam and the heart
    k.d.polygon([(118, 0), (138, 0), (134, 96), (122, 96)], fill=(250, 236, 170, 255))
    k.d.polygon([(128, 110), (118, 98), (122, 92), (128, 96), (134, 92), (138, 98)], fill=(240, 110, 160, 255), outline=INKT)
    # the clouds that part
    for x, y, r in ((60, 22, 26), (30, 30, 18), (196, 22, 26), (228, 30, 18)):
        k.d.ellipse((x - r, y - r // 2, x + r, y + r // 2), fill=(236, 230, 236, 255), outline=INKT)
    _guhkop(k, 168, 112, 6, (250, 250, 252, 255))


def kaart_grill(k):
    k.land([(0, 160), (0, 126), (256, 120), (256, 160)], kleur=(150, 132, 122, 255))
    # the frame with the fire in it
    k.d.rectangle((100, 50, 156, 124), fill=(56, 48, 48, 255), outline=INKT)
    k.d.rectangle((112, 62, 144, 112), fill=(244, 150, 50, 255), outline=INKT)
    for x in (118, 128, 138):
        k.d.polygon([(x - 5, 112), (x, 84 + (x % 3) * 4), (x + 5, 112)], fill=(250, 210, 90, 255))
    # the Grillguh with his hat
    _guhkop(k, 190, 114, 8, (250, 240, 230, 255))
    k.d.rectangle((184, 88, 196, 102), fill=(252, 252, 252, 255), outline=INKT)
    # far away: a charred statue, and on it somebody with a pointed hat
    k.d.rectangle((26, 78, 46, 124), fill=(40, 36, 40, 255), outline=INKT)
    _guhkop(k, 36, 70, 4, (176, 178, 190, 255))
    k.d.polygon([(30, 64), (36, 50), (42, 64)], fill=(120, 124, 140, 255), outline=INKT)
    for _ in range(14):
        x, y = k.rng.randint(104, 152), k.rng.randint(14, 44)
        k.d.ellipse((x - 2, y - 1, x + 2, y + 1), outline=(120, 110, 104, 255))


def kaart_timmer(k):
    k.land([(0, 160), (0, 124), (256, 124), (256, 160)], kleur=(190, 222, 170, 255))
    k.pad([(128, 124), (128, 152), (250, 152)])
    # the huisje: a big guh head with a door for a mouth, and the flag on top
    for dx in (-22, 22):
        k.d.ellipse((128 + dx - 10, 30, 128 + dx + 10, 62), fill=(240, 160, 200, 255), outline=INKT)
    k.d.pieslice((88, 40, 168, 120), 180, 360, fill=(240, 170, 205, 255), outline=INKT)
    k.d.rectangle((88, 80, 168, 124), fill=(242, 222, 228, 255), outline=INKT)
    for dx in (-18, 18):
        k.d.rectangle((128 + dx - 6, 88, 128 + dx + 6, 100), fill=(60, 70, 90, 255), outline=INKT)
    k.d.rectangle((122, 106, 134, 124), fill=(200, 130, 120, 255), outline=INKT)
    k.d.line((128, 40, 128, 18), fill=INKT, width=2)
    k.d.polygon([(128, 18), (146, 23), (128, 28)], fill=(240, 110, 170, 255), outline=INKT)
    # the first little resident on its way in, the Timmerguh watching
    _guhkop(k, 128, 140, 5, (255, 170, 200, 255))
    _guhkop(k, 190, 134, 8, (250, 210, 120, 255))
    k.boom(40, 124).boom(56, 120).boom(224, 122)


KAARTEN = {"balto": kaart_balto, "mewtwo": kaart_mewtwo, "ohana": kaart_ohana, "hemel": kaart_hemel, "grill": kaart_grill, "timmer": kaart_timmer}


def kaarten(h):
    for i, s in enumerate(scene.alle()):
        k = verhaal_motor.Kaart(seed=7100 + i)
        KAARTEN[s.kort](k)
        h.save(k.img, "gui", "verhaal", f"kaart_{s.id}.png")


# =====================================================================================================================
def texts(h):
    for s in scene.alle():
        verhaal_motor.scene(h, s.id, s.titel, s.teksten, s.namen)


def test_kamer(h):
    """The empty room of the game tests: a floor, air above it (every camera of every scene is free in it)."""
    t = h.Structure((15, 12, 15))
    t.fill(0, 0, 0, 14, 0, 14, "minecraft:smooth_stone")
    t.save(TESTKAMER)


def selfcheck(h):
    problems = java.check() + check_alles()
    for s in scene.alle():
        for key in ["titel"] + list(s.teksten) + [f"naam.{n}" for n in s.namen]:
            if f"scene.guhs.{s.id}.{key}" not in h.NL:
                problems.append(f"lang scene.guhs.{s.id}.{key}")
        if not os.path.exists(os.path.join(h.TEX, "gui", "verhaal", f"kaart_{s.id}.png")):
            problems.append(f"picture kaart_{s.id}.png")
    # the one line an old story already had: the ohana line, word for word
    if h.NL.get("scene.guhs.oudescenes_ohana.citaat") != h.NL.get("gui.guhs.guhwaii.ohana.citaat"):
        problems.append("the ohana line of the scene is not the line of the story")
    if not os.path.exists(f"{h.D}/structure/{TESTKAMER}.nbt"):
        problems.append(f"template {TESTKAMER}")
    if problems:
        raise SystemExit("oude_scenes: not right:\n  " + "\n  ".join(problems[:40]))


def build(h):
    texts(h)
    kaarten(h)
    test_kamer(h)
    selfcheck(h)


if __name__ == "__main__":
    fout = check_alles(alles=True)
    print("\n".join(fout) if fout else "oude_scenes: every camera hangs free, every actor stands")
    sys.exit(1 if fout else 0)
