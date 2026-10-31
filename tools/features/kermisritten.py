"""
1.8.1: the turning rides of the Guhkermis (Knabbelbooster, Zweefmolen, Reuzenrad, Guhtwister). Java: feature/kermisritten.

  build(h)    the four controller blocks (states, a plain cube model, item models), the moving parts as baked block models in
              models/block/kermisrit/ (see Model; the scale of each part is also in KermisritRenderer), the advancements and the
              texts (Dutch in nl_nl, English in tools/lang/en/c220_kermisritten.json) plus the blokuitleg entries
  ftb(fq)     a section "De kermisritten" in guhs_minigames (English: tools/lang/en/c220_kermisritten_ftb.json)

The shapes follow kermis-ontwerp/_bron/kermiskit_modules.py. Standalone: PYTHONPATH=tools python tools/features/kermisritten.py
"""
import json
import math
import os
import sys

RITTEN = ["booster", "zweefmolen", "reuzenrad", "twister"]
RAD_KLEUREN = ["aqua", "orchidee", "boter", "pistache", "perzik", "korenbloem", "framboos", "lavendel"]
ZWEEF_KLEUREN = ["aqua", "orchidee", "boter"]
TWIST_ARM = ["framboos", "aqua", "pistache"]
TWIST_KAR = ["boter", "perzik", "orchidee"]
G = "gui.guhs.kermisrit."

NAMEN = {"booster": ("Knabbelbooster", "Nibble Booster"), "zweefmolen": ("Zweefmolen", "Swing Carousel"),
         "reuzenrad": ("Reuzenrad", "Ferris Wheel"), "twister": ("Guhtwister", "Guh Twister")}
TEKSTEN = {
    G + "loket": ("%s: klik om in te stappen", "%s: click to get on"),
    G + "instappen": ("Instappen! Over %2$s seconden gaat de %1$s los. VAHOEG!", "All aboard! The %1$s starts in %2$s seconds. VAHOEG!"),
    G + "bezig": ("Njeg, de rit is bezig! Even wachten tot hij stilstaat.", "Njeg, the ride is running! Wait until it stops."),
    G + "vol": ("Helemaal vol! Volgende rit dan maar.", "Completely full! Next ride, then."),
    G + "blijf_zitten": ("Njeg! Blijf lekker zitten tot de rit stopt.", "Njeg! Stay seated until the ride stops."),
    G + "klaar": ("WIEEEE! Wat een rit! +1 kermisbon", "WHEEEE! What a ride! +1 kermisbon"),
    "entity.guhs.kermisrit_stoel": ("Kermisstoeltje", "Fair Seat"),
}
for rit, (nl, en) in NAMEN.items():
    TEKSTEN[f"block.guhs.kermisrit_{rit}"] = (nl, en)

ADVIES = {
    "booster": ("Ik zie mijn huis!", "Rij de Knabbelbooster op de Guhkermis", "I Can See My House!", "Ride the Nibble Booster at the Guh Fair"),
    "zweefmolen": ("Zweven als een guh", "Rij de Zweefmolen op de Guhkermis", "Floating Like a Guh", "Ride the Swing Carousel at the Guh Fair"),
    "reuzenrad": ("Uitzicht vol vads", "Rij het Reuzenrad op de Guhkermis", "A View Full of Vads", "Ride the Ferris Wheel at the Guh Fair"),
    "twister": ("Duizelguh", "Rij de Guhtwister op de Guhkermis", "Dizzy Guh", "Ride the Guh Twister at the Guh Fair"),
}
for rit, (t, b, _, _) in ADVIES.items():
    TEKSTEN[f"advancements.guhs.guhmension.kermisrit_{rit}.title"] = (t, ADVIES[rit][2])
    TEKSTEN[f"advancements.guhs.guhmension.kermisrit_{rit}.description"] = (b, ADVIES[rit][3])

UITLEG = {
    "kermisrit_booster": ("Het hart van de Knabbelbooster: het blok zit verstopt onder de attractie.\n\n- Klik op het kaartjespaaltje om in te "
                          "stappen.\n- Na 8 seconden gaat de rit los: 40 seconden over de kop.\n- Je stapt bij de poort uit en krijgt een kermisbon.",
                          "The heart of the Nibble Booster: the block is hidden under the ride.\n\n- Click the ticket post to get on.\n- After 8 "
                          "seconds the ride starts: 40 seconds head over heels.\n- You get off at the gate and receive a kermisbon."),
    "kermisrit_zweefmolen": ("Het hart van de Zweefmolen: het blok zit verstopt onder de attractie.\n\n- Klik op een stoeltje of het "
                             "kaartjespaaltje om in te stappen.\n- Na 8 seconden zweef je 35 seconden in het rond.\n- Je stapt bij de poort uit en "
                             "krijgt een kermisbon.",
                             "The heart of the Swing Carousel: the block is hidden under the ride.\n\n- Click a seat or the ticket post to get on."
                             "\n- After 8 seconds you float around for 35 seconds.\n- You get off at the gate and receive a kermisbon."),
    "kermisrit_reuzenrad": ("Het hart van het Reuzenrad: het blok zit verstopt onder de attractie.\n\n- Klik op het kaartjespaaltje om in te "
                            "stappen.\n- Na 8 seconden draait het rad een minuut lang rustig rond.\n- Je stapt bij de poort uit en krijgt een "
                            "kermisbon.",
                            "The heart of the Ferris Wheel: the block is hidden under the ride.\n\n- Click the ticket post to get on.\n- After 8 "
                            "seconds the wheel turns calmly for a minute.\n- You get off at the gate and receive a kermisbon."),
    "kermisrit_twister": ("Het hart van de Guhtwister: het blok zit verstopt onder de attractie.\n\n- Klik op een karretje of het "
                          "kaartjespaaltje om in te stappen.\n- Na 8 seconden tol je 40 seconden rond.\n- Je stapt bij de poort uit en krijgt een "
                          "kermisbon.",
                          "The heart of the Guh Twister: the block is hidden under the ride.\n\n- Click a car or the ticket post to get on.\n- "
                          "After 8 seconds you spin for 40 seconds.\n- You get off at the gate and receive a kermisbon."),
}

FTB_CHAPTER = "guhs_minigames"
FTB_SECTIES = [("kermisritten", "De kermisritten", "item:guhs:kermisbon", None)]
FTB_EN = {
    "kermisrit_reuzenrad": ("Rustig rondje", "Stap in het &dReuzenrad&r op de Guhkermis en bekijk de Guhmensie van bovenaf.",
                            "A Calm Round", "Get on the &dFerris Wheel&r at the Guh Fair and see the Guhmension from above."),
    "kermisrit_zweefmolen": ("Vliegende guh", "Rij de &dZweefmolen&r op de Guhkermis.", "Flying Guh", "Ride the &dSwing Carousel&r at the Guh Fair."),
    "kermisrit_twister": ("Duizelguh", "Tol rond in de &dGuhtwister&r op de Guhkermis.", "Dizzy Guh", "Spin around in the &dGuh Twister&r at the Guh Fair."),
    "kermisrit_booster": ("Ik zie mijn huis!", "Durf jij in de &dKnabbelbooster&r, het hoogste punt van de Guhkermis?", "I Can See My House!",
                          "Dare you ride the &dNibble Booster&r, the highest point of the Guh Fair?"),
}

TEX = {"goud": "minecraft:block/gold_block", "ijzer": "minecraft:block/iron_block", "berk": "minecraft:block/birch_planks",
       "tralie": "minecraft:block/iron_bars"}


def tex(naam):
    return TEX.get(naam, f"guhs:block/bouwhulp/pleister_{naam}")


class Model:
    """A moving part in BLOCK coordinates around its pivot (voxel centres on whole numbers). Exposed faces only, so it stays
    light. Written at 16/schaal units per block: block models may only reach -16..32 around their middle."""

    def __init__(self):
        self.voxels = {}
        self.boxes = []

    def vox(self, x, y, z, t):
        self.voxels[(round(x), round(y), round(z))] = t

    def box(self, a, b, t):
        self.boxes.append((a, b, t))

    def json(self, schaal):
        u = 16 / schaal
        namen = sorted({t for t in self.voxels.values()} | {b[2] for b in self.boxes})
        ref = {t: f"t{i}" for i, t in enumerate(namen)}

        def punt(p):
            q = [round(8 + c * u, 4) for c in p]
            assert all(-16 <= c <= 32 for c in q), (p, q, schaal)
            return q

        rich = {"east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0), "south": (0, 0, 1), "north": (0, 0, -1)}
        els = []
        for (x, y, z), t in sorted(self.voxels.items()):
            faces = {f: {"uv": [0, 0, 16, 16], "texture": "#" + ref[t]} for f, d in rich.items()
                     if (x + d[0], y + d[1], z + d[2]) not in self.voxels}
            if faces:
                els.append({"from": punt((x - .5, y - .5, z - .5)), "to": punt((x + .5, y + .5, z + .5)), "faces": faces})
        for a, b, t in self.boxes:
            els.append({"from": punt(a), "to": punt(b), "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#" + ref[t]} for f in rich}})
        tx = {ref[t]: tex(t) for t in namen}
        tx["particle"] = tex(namen[0])
        return {"parent": "minecraft:block/block", "textures": tx, "elements": els}


def loket():
    m = Model()
    m.vox(0, 0, 0, "framboos")
    m.box((-.15, .5, -.15), (.15, 1.2, .15), "vanille")
    m.box((-.5, 1.2, -.1), (.5, 1.7, .1), "boter")
    return m


def booster_arm():
    m = Model()
    for s in range(-18, 19):
        k = "goud" if abs(s) % 6 == 0 else "boter"
        m.vox(s, 0, 0, k)
        for w in (-1, 1):
            m.vox(s, w, 0, "goud" if abs(s) % 6 == 0 else "framboos")
    for teken in (-1, 1):
        for z in range(-3, 4):          # the fork that holds a cage on both sides
            m.vox(teken * 18, 0, z, "goud")
        for x in range(18, 21):
            m.vox(teken * x, 0, -3, "framboos")
            m.vox(teken * x, 0, 3, "framboos")
    return m


def booster_gondel(kleur):
    m = Model()
    for x in range(-2, 3):
        for z in range(-2, 3):
            m.vox(x, -3, z, kleur)
            m.box((x - .5, 2.5, z - .5), (x + .5, 3, z + .5), kleur)
    for x in (-2, 2):
        for z in (-2, 2):
            m.box((x - .15, -2.5, z - .15), (x + .15, 2.5, z + .15), "ijzer")
    return m


def rad_wiel():
    m = Model()
    rr = 13
    for zz in (-2, 2):
        for i in range(720):
            a = i * math.pi / 360
            m.vox(rr * math.cos(a), rr * math.sin(a), zz, "framboos")
        for i in range(8):
            a = i * math.pi / 4 + math.pi / 8
            for r in range(1, rr):
                m.vox(r * math.cos(a), r * math.sin(a), zz, "vanille")
    return m


def rad_cabine(kleur):
    m = Model()
    for x in (-1, 0, 1):
        for z in (-1, 0, 1):
            m.vox(x, -3, z, kleur)
            m.box((x - .5, 0, z - .5), (x + .5, .5, z + .5), kleur)
    for x in (-1, 1):
        for z in (-1, 1):
            m.box((x - .13, -2.5, z - .13), (x + .13, 0, z + .13), "berk")
    return m


def zweef_stoel(kleur):
    m = Model()
    for z in (-.35, .35):
        m.box((-.05, -6, z - .05), (.05, 0, z + .05), "ijzer")
    m.box((-.5, -6.5, -.5), (.5, -6, .5), kleur)
    m.box((-.5, -6, -.5), (-.35, -5.3, .5), kleur)
    return m


def twister_ster():
    m = Model()
    for i, kleur in enumerate(TWIST_ARM):
        a = i * 2 * math.pi / 3
        r = 1.0
        while r < 7.6:
            m.vox(r * math.cos(a), 6 - round(r / 4), r * math.sin(a), kleur)
            r += .4
        hx, hz = 7.5 * math.cos(a), 7.5 * math.sin(a)
        for y in (3, 4):
            m.vox(hx, y, hz, "goud")
    return m


def twister_kruis(kleur):
    m = Model()
    for j in range(3):
        b = j * 2 * math.pi / 3
        r = .5
        while r < 3.4:
            m.vox(r * math.cos(b), 3, r * math.sin(b), "ijzer")
            r += .4
        kx, kz = 3.4 * math.cos(b), 3.4 * math.sin(b)
        m.box((kx - 1, .5, kz - 1), (kx + 1, 1, kz + 1), kleur)
        m.box((kx - 1, 1, kz - 1), (kx + 1, 1.6, kz - .7), kleur)
        m.box((kx - .1, 1.6, kz - .1), (kx + .1, 3, kz + .1), "ijzer")
    return m


def modellen():
    """Name, model, schaal (the same scales as KermisritRenderer.DELEN)."""
    uit = [("loket", loket(), 2), ("booster_arm", booster_arm(), 16), ("booster_gondel_0", booster_gondel("aqua"), 3),
           ("booster_gondel_1", booster_gondel("orchidee"), 3), ("rad_wiel", rad_wiel(), 10), ("twister_ster", twister_ster(), 8)]
    uit += [(f"rad_cabine_{k}", rad_cabine(k), 3) for k in RAD_KLEUREN]
    uit += [(f"zweef_stoel_{k}", zweef_stoel(k), 5) for k in ZWEEF_KLEUREN]
    uit += [(f"twister_kruis_{k}", twister_kruis(k), 4) for k in TWIST_KAR]
    return uit


def blokken(h):
    for r in RITTEN:
        n = f"kermisrit_{r}"
        h.w(f"{h.A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/bouwhulp/pleister_framboos"}})
        h.w(f"{h.A}/blockstates/{n}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{n}"} for f in ("north", "east", "south", "west")}})
        h.w(f"{h.A}/models/item/{n}.json", {"parent": f"guhs:block/{n}"})
    for naam, model, schaal in modellen():
        h.w(f"{h.A}/models/block/kermisrit/{naam}.json", model.json(schaal))


def advancements(h):
    for r in RITTEN:
        n = f"kermisrit_{r}"
        h.w(f"{h.D}/advancement/guhmension/{n}.json", {
            "parent": "guhs:guhmension/find_guh_kermis",
            "display": {"icon": {"id": f"guhs:{n}"}, "title": {"translate": f"advancements.guhs.guhmension.{n}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{n}.description"},
                        "frame": "goal" if r == "booster" else "task", "show_toast": True, "announce_to_chat": False, "hidden": False},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def build(h):
    blokken(h)
    advancements(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def ftb(fq):
    item = fq.item
    pics = {"kermisrit_reuzenrad": "guhs:kermisrit_reuzenrad", "kermisrit_zweefmolen": "guhs:kermisrit_zweefmolen",
            "kermisrit_twister": "guhs:kermisrit_twister", "kermisrit_booster": "guhs:kermisrit_booster"}
    for i, (key, (titel, tekst, _, _)) in enumerate(FTB_EN.items()):
        fq.q(key, titel, tekst, pics[key], [fq.adv(f"guhs:guhmension/{key}")], rewards=(("guhs:kermisbon", 2),), x=i * 2, y=0, xp=100)
    del item


def selfcheck(h):
    problems = []
    for r in RITTEN:
        if not os.path.exists(f"{h.A}/blockstates/kermisrit_{r}.json"):
            problems.append(r)
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%") != en.count("%"):
            problems.append(f"% count differs: {k}")
    if problems:
        raise SystemExit("kermisritten self-check failed:\n  " + "\n  ".join(problems))


def _blokuitleg():
    p = os.path.join("tools", "wiki_site", "blokuitleg.py")
    s = open(p, encoding="utf-8").read()
    begin, eind = "# <kermisritten>", "# </kermisritten>"
    if begin in s:
        s = s[:s.index(begin)].rstrip("\n") + "\n" + s[s.index(eind) + len(eind):].lstrip("\n")
    blok = [begin]
    for n, (nl, en) in UITLEG.items():
        blok.append(f"UITLEG[{n!r}] = dict(\n    nl={nl!r},\n    en={en!r},\n    meer=[])")
    blok.append(eind)
    s = s.rstrip("\n") + "\n\n\n" + "\n".join(blok) + "\n"
    open(p, "w", encoding="utf-8", newline="\n").write(s)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    def schrijf(pad, d):
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    os.makedirs(os.path.join("tools", "lang", "en"), exist_ok=True)
    schrijf(os.path.join("tools", "lang", "en", "c220_kermisritten.json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    ftb_en = {f"ftb.{FTB_CHAPTER}.section.kermisritten": "The Fair Rides"}
    for key, (_, _, ten, den) in FTB_EN.items():
        ftb_en[f"ftb.{FTB_CHAPTER}.q.{key}.title"] = ten
        ftb_en[f"ftb.{FTB_CHAPTER}.q.{key}.desc"] = den
    schrijf(os.path.join("tools", "lang", "en", "c220_kermisritten_ftb.json"), ftb_en)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        schrijf(p, d)
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for r in RITTEN:
        schrijf(os.path.join(a, f"kermisrit_{r}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/kermisrit_{r}"}})
    _blokuitleg()
    print(f"kermisritten: {len(RITTEN)} rides, {len(modellen())} moving parts, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
