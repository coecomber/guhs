"""
Guhs 1.9 "Een opknapper!", slice KOPPELING (Java: feature/erfenis/koppeling): what connects the village to your real base.

  - the Erfhekje (guhs:erfhekje, two blocks high: pink posts with a heart, the knuffelsteen face on the beam, roze-hooibaal ears,
    a pink tulip on top, and a soft glowing animated doorway; render_type translucent, light_emission on the doorway)
  - the Leverkist (guhs:leverkist: a little treasure chest with a pink lid, gold trim, a letter slot and a mailbox flag that
    goes up while there is post: blockstate post=true|false)
  - the Dorpszender (guhs:dorpszender: a guh machine via vadskracht.machine, pink + gold, antenna and a little house on its hatch)
  - items: makelaarsbrief, glasscherf (smelts to glass), guhldenzakje_klein / guhldenzakje / guhldenzakje_groot, decobon
  - recipes (dorpszender, leverkist, glasscherf -> glass), loot tables, tags, lang (Dutch in nl_nl AND en_us, house rule),
    the blokuitleg of the three blocks (tools/wiki_site/blokuitleg.py, between markers).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/erfenis_koppeling.py
"""
import json
import math
import os
import random
import re
import sys

from PIL import Image, ImageDraw

from features import vadskracht

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
TEX = os.path.join(A, "textures")
K = "erfenis.koppeling."

# =====================================================================================================================
# texts (Dutch; en_us gets the same Dutch)
# =====================================================================================================================
TEKSTEN = {
    "block.guhs.erfhekje": "Erfhekje",
    "block.guhs.leverkist": "Leverkist",
    "block.guhs.leverkist.lore": "Post uit je erfenis komt hier vanzelf in. Het vlaggetje gaat omhoog als er iets in zit.",
    "block.guhs.dorpszender": "Dorpszender",
    "block.guhs.dorpszender.lore": "Zendt vadskracht van je net naar de Vadsmast in je dorp. Een kwart waait onderweg weg, njeg.",
    "item.guhs.makelaarsbrief": "Brief van makelaar Guhrrit",
    "item.guhs.makelaarsbrief.lore": "Een dikke envelop die een beetje naar kaas ruikt. Rechtsklik om te lezen.",
    "item.guhs.glasscherf": "Glasscherven",
    "item.guhs.glasscherf.lore": "Uit de Glasblazerij van je dorp. Smelt ze thuis in een oven tot glas.",
    "item.guhs.guhldenzakje_klein": "Klein Guhldenzakje",
    "item.guhs.guhldenzakje": "Guhldenzakje",
    "item.guhs.guhldenzakje_groot": "Dikke Guhldenzak",
    "item.guhs.guhldenzakje.lore": "%s Guhlden voor je dorp. Rechtsklik: in de Hoeve. Sluipen: het hele stapeltje.",
    "item.guhs.decobon": "Decobon",
    "item.guhs.decobon.lore": "Goed voor één decostukje naar keuze in de decowinkel van je dorp. Rechtsklik om in te leveren.",
    # the letter
    K + "brief.titel": "Brief van makelaar Guhrrit",
    K + "brief.kop": "Makelaardij Guhrrit & Zn.",
    K + "brief.kop2": "Sinds gisteren. Erkend door niemand.",
    K + "brief.aanhef": "Beste %s,",
    K + "brief.tekst": "Gefeliciteerd, njeg! U bent uitverkoren (door mij, met een dobbelsteen) om een prachtig stukje grond over te "
                       "nemen: een oude boerderij aan de rand van de Guhmension. Ruim perceel, veel groen (heel veel groen), een "
                       "sfeervol schuurtje. In de volksmond heet zoiets een opknapper. Het bouwkundig rapport is kort maar eerlijk: vads.\n\n"
                       "Vraagprijs: één (1) kaasknabbel. Nee, dat is geen typfout. Betaal de knabbel en u krijgt het Erfhekje: "
                       "zet het in uw eigen basis en stap zo uw erfje op. Vahoeg!",
    K + "brief.groet": "Hoogachtend, makelaar Guhrrit",
    K + "brief.stempel_rapport": "BOUWKUNDIG RAPPORT: VADS",
    K + "brief.stempel_verkocht": "VERKOCHT! (voor 1 knabbel)",
    K + "brief.knop_koop": "Kopen voor 1 kaasknabbel",
    K + "brief.knop_extra": "Extra hekje: 1 knabbel",
    K + "brief.knop_later": "Nog even niet",
    K + "brief.knabbels": "Jij hebt %s kaasknabbels",
    K + "brief.geen_knabbel": "Je hebt geen kaasknabbel bij je, njeg! Guhrrit doet het echt niet voor minder.",
    K + "brief.kwijt": "Je hebt de brief van Guhrrit niet meer bij je. Waar is die nou weer gebleven?",
    K + "brief.gekocht": "Verkocht! Guhrrit stopt de kaasknabbel tevreden in zijn binnenzak. Je krijgt het Erfhekje en een Leverkist. "
                         "Zet het hekje in je basis en stap erdoor naar je erfenis. Vahoeg!",
    K + "brief.extra": "Nog een Erfhekje, alsjeblieft. Guhrrit knipoogt: \"Voor u altijd, njeg.\"",
    K + "brief.bezorgd": "Post voor %s! Een dikke envelop van makelaar Guhrrit. Hij ruikt een beetje naar kaas, njeg.",
    K + "postguh": "Postguh",
    # the hekje
    K + "hekje.op_slot": "Dit hekje zit op slot. Eerst de brief van makelaar Guhrrit, njeg!",
    K + "hekje.dicht": "Het hekje piept, maar er gebeurt niks. Probeer het zo nog eens.",
    # the Leverkist
    K + "leverkist.post": "Post uit je erfenis: %s spulletjes in je Leverkist!",
    K + "leverkist.post_vol": "Post uit je erfenis: %s spulletjes in je Leverkist. De rest wacht tot er weer plek is.",
    K + "leverkist.van_jou": "Deze Leverkist is nu van jou: post uit je erfenis komt hierin.",
    # the Dorpszender
    K + "dorpszender.stand": "Dorpszender: ik zend %s VK. In je dorp komt %s VK aan (een kwart waait onderweg weg, njeg).",
    # the rewards
    K + "zakje.geen_erfenis": "Je hebt nog geen erfenis! Bewaar dit maar, dat komt goed.",
    K + "zakje.gestort": "+%s Guhlden in je Hoeve. Rinkel rinkel!",
    K + "decobon.ingewisseld": "Decobon ingeleverd! Je hebt nu %s Decobon(nen) voor de decowinkel in je dorp.",
    # dev commands
    K + "cmd.brief_status": "Brief: staat %s (0 = wacht, 1 = bezorgd, 2 = gekocht), klok %s van %s minuten.",
    K + "cmd.brief_reset": "De brief wacht weer (klok op 0).",
    K + "cmd.brief_klok": "Briefklok op %s minuten.",
    K + "cmd.leverpost": "%s pakketjes verstuurd. Nog %s spulletjes wachten op een Leverkist.",
    K + "cmd.leverpost_info": "Leverkisten: %s. Wachtende spulletjes: %s.",
    K + "cmd.dorpszender": "Je Dorpszenders sturen %s VK naar je dorp (na het verlies).",
    K + "cmd.decobon": "Je hebt %s Decobon(nen).",
}

UITLEG = {
    "erfhekje": (
        "Het poortje naar je eigen erfenis. Je krijgt het van makelaar Guhrrit: zijn brief komt een half uurtje nadat je voor het "
        "eerst in de Guhmension was, en voor één kaasknabbel is de opknapper van jou.\n\nZet het hekje in je basis en loop erdoor "
        "(of rechtsklik erop): je staat meteen op je erfje. Ga je terug, dan kom je weer uit voor het hekje dat je gebruikte. Je mag "
        "er meer neerzetten; het laatste hekje dat je gebruikte telt. Een extra hekje koop je met de brief, ook voor één kaasknabbel.",
        "The little gate to your own inheritance. Makelaar Guhrrit gives it to you: his letter arrives half an hour after your first "
        "visit to the Guhmension, and for one cheese nibble the fixer-upper is yours.\n\nPut the gate in your base and walk through "
        "it (or right-click it): you are on your plot at once. When you go back, you come out in front of the gate you used. You may "
        "place more; the last one you used counts. An extra gate costs one cheese nibble, through the letter."),
    "leverkist": (
        "De brievenbus-schatkist voor je basis. Wat je in je dorp op \"Naar huis sturen\" zet, komt hier vanzelf in, ook als je er "
        "niet bent: zodra de kist geladen is. Het vlaggetje gaat omhoog als er post in zit.\n\nRechtsklik om hem te openen, of laat "
        "een trechter of buis hem leeghalen. Heb je er meer, dan vullen ze zich op volgorde: de eerste die je neerzette eerst. Past "
        "het niet, dan wacht de rest netjes tot er weer plek is. Je eerste Leverkist krijg je bij de opknapper.",
        "The mailbox treasure chest for your base. What you set to \"Send home\" in your village arrives here by itself, even when "
        "you are away: as soon as the chest is loaded. The little flag goes up while there is post inside.\n\nRight-click to open "
        "it, or let a hopper or pipe empty it. With more chests they fill in order: the first one you placed first. What does not "
        "fit waits until there is room again. Your first Leverkist comes with the fixer-upper."),
    "dorpszender": (
        "Een Vadszender met een dorp-upgrade (Vadszender, 4 guhkristallen en een blok kaasknabbels). Zet hem op je vadskracht-net: "
        "hij zendt vadskracht naar de Vadsmast in je dorp, waar de hoogste werkplaatsen en de Mammoetwei op draaien.\n\nRechtsklik "
        "zet hem hoger, sluipen + rechtsklik lager (10 tot 200 VK). Een kwart waait onderweg weg: 100 VK zenden is 75 VK in je dorp. "
        "Meer Dorpszenders tellen bij elkaar op. Heeft je net te weinig, dan staat alles op dat net stil, ook de zender.",
        "A Vadszender with a village upgrade (Vadszender, 4 guh crystals and a block of cheese nibbles). Put it on your vadskracht "
        "net: it sends power to the Vadsmast in your village, which runs the top workshops and the Mammoth Meadow.\n\nRight-click "
        "turns it up, sneak + right-click down (10 to 200 VK). A quarter blows away on the way: 100 VK sent is 75 VK in your "
        "village. More senders add up. If your net has too little, everything on it stops, the sender too."),
}

BLOKUITLEG = os.path.join("tools", "wiki_site", "blokuitleg.py")
_MARK = "# <erfenis_koppeling> (1.9, tools/features/erfenis_koppeling.py: rewritten on every run)"


def blokuitleg():
    with open(BLOKUITLEG, encoding="utf-8") as f:
        src = f.read()
    src = re.sub(r"\n" + re.escape(_MARK) + r".*?# </erfenis_koppeling>\n", "\n", src, flags=re.S)
    regels = [_MARK] + [f"UITLEG[{k!r}] = dict(nl={nl!r}, en={en!r})" for k, (nl, en) in UITLEG.items()] + ["# </erfenis_koppeling>"]
    if not src.endswith("\n"):
        src += "\n"
    with open(BLOKUITLEG, "w", encoding="utf-8", newline="\n") as f:
        f.write(src + "\n".join(regels) + "\n")


# =====================================================================================================================
# helpers
# =====================================================================================================================
def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def _tex(name):
    return Image.open(os.path.join(TEX, "block", name)).convert("RGBA")


def _px(img, pts, kleur):
    for x, y in pts:
        img.putpixel((x, y), tuple(kleur[:3]) + ((kleur[3],) if len(kleur) > 3 else (255,)))


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


HART = [(1, 0), (3, 0), (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (1, 2), (2, 2), (3, 2), (2, 3)]


def _hart(img, x0, y0, kleur):
    _px(img, [(x0 + x, y0 + y) for x, y in HART], kleur)


def _face(uv, cull=None, rot=None, tint=None, tex="#t"):
    f = {"texture": tex, "uv": uv}
    if cull:
        f["cullface"] = cull
    if rot:
        f["rotation"] = rot
    return f


def _box(frm, to, tex, uvs=None, faces=("north", "south", "west", "east", "up", "down"), emis=None, rotation=None):
    """An element; uvs: {face: [u0, v0, u1, v1]} (default: the matching part of a 16x16 texture)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "west": [z0, 16 - y1, z1, 16 - y0], "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
            "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for f in faces:
        uv = (uvs or {}).get(f, auto[f])
        uv = [max(0, min(16, v)) for v in uv]
        el["faces"][f] = {"texture": tex, "uv": uv}
    if emis is not None:
        el["light_emission"] = emis
    if rotation:
        el["rotation"] = rotation
    return el


# =====================================================================================================================
# the Erfhekje
# =====================================================================================================================
def erfhekje_texturen():
    # the posts: roze knuffelsteen with a little heart in the middle
    paal = _tex("roze_knuffelsteen.png")
    _hart(paal, 6, 6, (255, 246, 250))
    _px(paal, [(7, 7)], (255, 205, 225))
    _save(paal, "block", "erfhekje_paal.png")
    # the doorway: 8 frames of a soft pink-lilac swirl with sparkles
    frames = 8
    img = Image.new("RGBA", (16, 16 * frames))
    rng = random.Random(1909)
    vonken = [(rng.random() * 16, rng.random() * 16, rng.random()) for _ in range(7)]
    roze, lila, diep = (255, 168, 214), (206, 160, 255), (168, 96, 220)
    for f in range(frames):
        fase = f / frames * 2 * math.pi
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy)
                hoek = math.atan2(dy, dx)
                golf = 0.5 + 0.5 * math.sin(hoek * 2 + r * 0.7 - fase)
                kleur = _mix(_mix(diep, lila, min(1, r / 9)), roze, golf * 0.75)
                a = 205 + int(30 * golf)
                img.putpixel((x, f * 16 + y), kleur + (a,))
        for vx, vy, vf in vonken:
            yy = int((vy - f * 2 + 64) % 16)
            xx = int(vx)
            if (f + int(vf * 8)) % 3 != 0:
                img.putpixel((xx, f * 16 + yy), (255, 250, 255, 250))
    _save(img, "block", "erfhekje_poort.png")
    with open(os.path.join(TEX, "block", "erfhekje_poort.png.mcmeta"), "w", encoding="utf-8", newline="\n") as fh:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, fh, indent=2)
        fh.write("\n")


def erfhekje_modellen():
    # (26.1: no render_type; the doorway's sprite is forced translucent, the tulip's cutout comes from its texture)
    t = {"particle": "guhs:block/erfhekje_paal", "paal": "guhs:block/erfhekje_paal",
         "poort": {"sprite": "guhs:block/erfhekje_poort", "force_translucent": True},
         "gezicht": "guhs:block/knuffelsteen_gezicht_0", "oor": "guhs:block/roze_hooibaal_side", "oor_top": "guhs:block/roze_hooibaal_top",
         "bloem": "minecraft:block/pink_tulip"}
    onder = [
        _box([0, 0, 5], [3, 16, 11], "#paal"),
        _box([13, 0, 5], [16, 16, 11], "#paal"),
        _box([3, 0, 7.5], [13, 16, 8.5], "#poort", faces=("north", "south"), emis=15,
             uvs={"north": [3, 0, 13, 16], "south": [3, 0, 13, 16]}),
        _box([3, 0, 5.5], [13, 0.5, 10.5], "#paal", faces=("up",)),   # (a little threshold)
    ]
    boven = [
        _box([0, 0, 5], [3, 14, 11], "#paal"),
        _box([13, 0, 5], [16, 14, 11], "#paal"),
        _box([3, 0, 7.5], [13, 14, 8.5], "#poort", faces=("north", "south"), emis=15,
             uvs={"north": [3, 0, 13, 14], "south": [3, 0, 13, 14]}),
        # the beam with the face, on both sides
        {"from": [-1, 13, 4], "to": [17, 25, 12], "faces": {
            "north": {"texture": "#gezicht", "uv": [0, 2, 16, 14]}, "south": {"texture": "#gezicht", "uv": [0, 2, 16, 14]},
            "west": {"texture": "#paal", "uv": [4, 2, 12, 14]}, "east": {"texture": "#paal", "uv": [4, 2, 12, 14]},
            "up": {"texture": "#paal", "uv": [0, 4, 16, 12]}, "down": {"texture": "#paal", "uv": [0, 4, 16, 12]}}},
        # the ears (roze hooibaal)
        _box([0, 25, 6], [4, 29, 10], "#oor", uvs={"up": [0, 0, 4, 4]}),
        _box([12, 25, 6], [16, 29, 10], "#oor", uvs={"up": [0, 0, 4, 4]}),
        # a pink tulip on top (two crossed planes)
        {"from": [4.5, 25, 8], "to": [11.5, 32, 8], "rotation": {"origin": [8, 25, 8], "axis": "y", "angle": 45, "rescale": True},
         "shade": False, "faces": {"north": {"texture": "#bloem", "uv": [0, 0, 16, 16]}, "south": {"texture": "#bloem", "uv": [0, 0, 16, 16]}}},
        {"from": [8, 25, 4.5], "to": [8, 32, 11.5], "rotation": {"origin": [8, 25, 8], "axis": "y", "angle": 45, "rescale": True},
         "shade": False, "faces": {"west": {"texture": "#bloem", "uv": [0, 0, 16, 16]}, "east": {"texture": "#bloem", "uv": [0, 0, 16, 16]}}},
    ]
    for el in boven[4:6]:
        el["faces"]["up"]["texture"] = "#oor_top"
    for naam, els in (("erfhekje_onder", onder), ("erfhekje_boven", boven)):
        _w(os.path.join(A, "models", "block", naam + ".json"), {"parent": "minecraft:block/block", "textures": t, "elements": els})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    _w(os.path.join(A, "blockstates", "erfhekje.json"), {"variants": {
        f"facing={f},half={h}": {"model": f"guhs:block/erfhekje_{'onder' if h == 'lower' else 'boven'}", **({"y": r} if r else {})}
        for f, r in rot.items() for h in ("lower", "upper")}})
    # the item: a flat icon
    icoon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(icoon)
    roze, donker, gezicht = (240, 150, 190, 255), (196, 100, 150, 255), (255, 226, 232, 255)
    d.rectangle((4, 7, 11, 15), fill=(214, 150, 250, 255))                # the doorway
    for y in range(7, 16):
        for x in range(4, 12):
            if (x * 3 + y * 5) % 7 == 0:
                icoon.putpixel((x, y), (255, 190, 226, 255))
    d.rectangle((1, 6, 3, 15), fill=roze)
    d.rectangle((12, 6, 14, 15), fill=roze)
    d.line((1, 15, 3, 15), fill=donker)
    d.line((12, 15, 14, 15), fill=donker)
    d.rectangle((0, 2, 15, 6), fill=gezicht, outline=donker)              # the beam with the face
    _px(icoon, [(5, 4), (10, 4)], (40, 28, 50))
    _px(icoon, [(3, 5), (12, 5)], (255, 140, 170))
    _px(icoon, [(7, 5), (8, 5)], (200, 90, 130))
    _px(icoon, [(1, 0), (2, 0), (1, 1), (2, 1), (13, 0), (14, 0), (13, 1), (14, 1)], (228, 128, 160))   # ears
    _px(icoon, [(7, 0), (8, 0), (7, 1)], (255, 120, 180))                  # the tulip
    _px(icoon, [(2, 9), (13, 9)], (255, 246, 250))                         # hearts on the posts
    _save(icoon, "item", "erfhekje.png")
    _w(os.path.join(A, "models", "item", "erfhekje.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/erfhekje"}})


# =====================================================================================================================
# the Leverkist
# =====================================================================================================================
HOUT, HOUT_D, HOUT_L = (156, 104, 64), (112, 72, 44), (182, 128, 82)
LID, LID_D, LID_L = (240, 150, 190), (206, 110, 156), (255, 192, 220)
GOUD, GOUD_D = (255, 210, 90), (204, 150, 50)


def _kist_zij(voor=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(77 if voor else 78)
    for y in range(16):
        for x in range(16):
            if y < 3:
                continue
            if y <= 6:
                c = LID if y > 3 else LID_L
                c = _mix(c, LID_D, rng.random() * 0.15)
            elif y == 7:
                c = GOUD
            else:
                c = HOUT_D if y in (11, 15) else _mix(HOUT, HOUT_L, rng.random() * 0.35)
                if x in (1, 14):
                    c = HOUT_D
            img.putpixel((x, y), c + (255,))
    for y in range(3, 16):
        _px(img, [(1, y), (14, y)], GOUD_D if y <= 7 else HOUT_D)
    if voor:
        # the letter slot in the lid, a little gold latch and a heart
        _px(img, [(x, 5) for x in range(5, 11)], (70, 36, 56))
        _px(img, [(7, 7), (8, 7), (7, 8), (8, 8), (6, 8), (9, 8), (7, 9), (8, 9)], GOUD)
        _px(img, [(7, 8), (8, 8)], (120, 80, 30))
        _hart(img, 6, 11, (255, 170, 205))
    return img


def leverkist_texturen():
    _save(_kist_zij(False), "block", "leverkist_zij.png")
    _save(_kist_zij(True), "block", "leverkist_voor.png")
    top = Image.new("RGBA", (16, 16))
    rng = random.Random(79)
    for y in range(16):
        for x in range(16):
            c = _mix(LID, LID_D, rng.random() * 0.15)
            if x in (1, 14) or y in (1, 14):
                c = GOUD
            top.putpixel((x, y), c + (255,))
    _hart(top, 6, 6, (255, 236, 244))
    _save(top, "block", "leverkist_boven.png")
    onder = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            onder.putpixel((x, y), (HOUT_D if y % 4 == 0 else HOUT) + (255,))
    _save(onder, "block", "leverkist_onder.png")
    vlag = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            vlag.putpixel((x, y), (250, 248, 244, 255) if x < 2 else (236, 98, 156, 255))
    _hart(vlag, 6, 6, (255, 255, 255))
    _save(vlag, "block", "leverkist_vlag.png")


def leverkist_modellen():
    t = {"particle": "guhs:block/leverkist_zij", "zij": "guhs:block/leverkist_zij", "voor": "guhs:block/leverkist_voor",
         "boven": "guhs:block/leverkist_boven", "onder": "guhs:block/leverkist_onder", "vlag": "guhs:block/leverkist_vlag"}

    def kist():
        return {"from": [1, 0, 1], "to": [15, 13, 15], "faces": {
            "north": {"texture": "#voor", "uv": [1, 3, 15, 16]}, "south": {"texture": "#zij", "uv": [1, 3, 15, 16]},
            "west": {"texture": "#zij", "uv": [1, 3, 15, 16]}, "east": {"texture": "#zij", "uv": [1, 3, 15, 16]},
            "up": {"texture": "#boven", "uv": [1, 1, 15, 15]}, "down": {"texture": "#onder", "uv": [1, 1, 15, 15], "cullface": "down"}}}

    paal = {"from": [12.5, 13, 12.5], "to": [13.5, 23, 13.5], "faces": {
        f: {"texture": "#vlag", "uv": [0, 0, 1, 10]} for f in ("north", "south", "west", "east")} | {
        "up": {"texture": "#vlag", "uv": [0, 0, 1, 1]}}}

    def vlag(y0):
        return {"from": [8, y0, 12.75], "to": [12.5, y0 + 3.5, 13.25], "faces": {
            "north": {"texture": "#vlag", "uv": [2, 4, 12, 12]}, "south": {"texture": "#vlag", "uv": [12, 4, 2, 12]},
            "west": {"texture": "#vlag", "uv": [2, 4, 3, 12]}, "up": {"texture": "#vlag", "uv": [2, 4, 12, 5]},
            "down": {"texture": "#vlag", "uv": [2, 4, 12, 5]}}}

    for naam, y0 in (("leverkist", 14), ("leverkist_post", 19.5)):
        _w(os.path.join(A, "models", "block", naam + ".json"), {"parent": "minecraft:block/block", "textures": t,
                                                               "elements": [kist(), paal, vlag(y0)]})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    _w(os.path.join(A, "blockstates", "leverkist.json"), {"variants": {
        f"facing={f},post={p}": {"model": f"guhs:block/{'leverkist_post' if p == 'true' else 'leverkist'}", **({"y": r} if r else {})}
        for f, r in rot.items() for p in ("false", "true")}})
    _w(os.path.join(A, "models", "item", "leverkist.json"), {"parent": "guhs:block/leverkist_post"})


# =====================================================================================================================
# the Dorpszender (a guh machine: pink and gold, the zender's antenna, a little house on the hatch)
# =====================================================================================================================
ZENDER_BASIS, ZENDER_ACCENT = (236, 150, 186), (255, 210, 90)


def _voor_dorpszender(img, staat):
    aan = staat != "slaapt"
    _px(img, [(7, 0), (8, 0), (7, 1), (8, 1), (7, 2), (8, 2)], (110, 90, 110))
    _px(img, [(7, 0), (8, 0)], ZENDER_ACCENT)
    if aan:
        _px(img, [(5, 1), (4, 2), (10, 1), (11, 2), (3, 0), (12, 0)], ZENDER_ACCENT)
    for x in range(4, 12):
        for y in range(9, 14):
            img.putpixel((x, y), ((120, 70, 90) if aan else (60, 36, 50)) + (255,))
    dak = (255, 120, 170) if aan else (150, 80, 110)
    muur = (255, 240, 220) if aan else (130, 110, 110)
    _px(img, [(7, 9), (8, 9), (6, 10), (7, 10), (8, 10), (9, 10), (5, 11), (10, 11)], dak)
    _px(img, [(6, 11), (7, 11), (8, 11), (9, 11), (6, 12), (9, 12), (6, 13), (7, 13), (8, 13), (9, 13)], muur)
    _px(img, [(7, 12), (8, 12)], (150, 96, 60) if aan else (70, 50, 40))
    if aan:
        _px(img, [(11, 9)], (255, 250, 200))


def dorpszender(h):
    vadskracht.machine(h, "dorpszender", ZENDER_BASIS, ZENDER_ACCENT, voor=_voor_dorpszender)


# =====================================================================================================================
# items
# =====================================================================================================================
def _brief():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 4, 14, 12), fill=(252, 240, 214, 255), outline=(176, 140, 96, 255))
    d.line((1, 4, 7, 9), fill=(196, 160, 116, 255))
    d.line((14, 4, 8, 9), fill=(196, 160, 116, 255))
    d.ellipse((6, 8, 9, 11), fill=(222, 70, 130, 255))   # the wax seal
    img.putpixel((7, 9), (255, 160, 200, 255))
    _px(img, [(11, 6), (12, 6), (11, 7), (12, 7)], (240, 150, 190))   # a little stamp
    return img


def _scherf():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    stukken = [
        [(3, 4), (7, 2), (8, 7), (4, 9)],
        [(9, 5), (13, 4), (12, 10), (9, 9)],
        [(4, 11), (8, 10), (10, 14), (5, 14)],
    ]
    d = ImageDraw.Draw(img)
    for i, st in enumerate(stukken):
        d.polygon(st, fill=(186, 226, 240, 190) if i != 1 else (210, 238, 248, 200), outline=(120, 176, 204, 255))
    _px(img, [(5, 4), (6, 4), (11, 6), (6, 12)], (255, 255, 255))
    return img


def _zakje(grootte):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    stof, stof_d = (226, 150, 120), (176, 104, 82)
    if grootte == "klein":
        d.ellipse((4, 7, 11, 14), fill=stof + (255,), outline=stof_d + (255,))
        d.rectangle((6, 5, 9, 7), fill=stof + (255,))
        d.line((5, 7, 10, 7), fill=(240, 120, 170, 255))
        munt = [(7, 10), (8, 10), (7, 11), (8, 11)]
    elif grootte == "midden":
        d.ellipse((2, 5, 13, 15), fill=stof + (255,), outline=stof_d + (255,))
        d.rectangle((5, 2, 10, 5), fill=stof + (255,), outline=stof_d + (255,))
        d.line((4, 5, 11, 5), fill=(240, 120, 170, 255))
        munt = [(6, 9), (7, 9), (8, 9), (9, 9), (6, 10), (9, 10), (6, 11), (7, 11), (8, 11), (9, 11)]
    else:
        stof, stof_d = (236, 170, 90), (180, 120, 50)
        d.ellipse((1, 3, 14, 15), fill=stof + (255,), outline=stof_d + (255,))
        d.rectangle((5, 0, 10, 3), fill=stof + (255,), outline=stof_d + (255,))
        d.line((3, 3, 12, 3), fill=(240, 120, 170, 255))
        munt = [(5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (5, 9), (10, 9), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10),
                (5, 11), (10, 11), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12)]
        _px(img, [(0, 13), (1, 14), (14, 14), (15, 13)], GOUD)   # coins spilling out
    _px(img, munt, GOUD)
    return img


def _decobon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 4, 14, 11), fill=(255, 214, 230, 255), outline=(220, 110, 160, 255))
    for y in range(5, 11, 2):
        img.putpixel((5, y), (220, 110, 160, 255))   # the perforation
    _px(img, [(9, 6), (11, 6), (10, 7), (9, 8), (11, 8)], (255, 120, 170))   # a little flower
    _px(img, [(10, 7)], GOUD)
    _px(img, [(10, 9), (10, 10)], (90, 170, 90))
    return img


ITEMS = {"makelaarsbrief": _brief, "glasscherf": _scherf, "guhldenzakje_klein": lambda: _zakje("klein"),
         "guhldenzakje": lambda: _zakje("midden"), "guhldenzakje_groot": lambda: _zakje("groot"), "decobon": _decobon}


def items():
    for n, maak in ITEMS.items():
        _save(maak(), "item", n + ".png")
        _w(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{n}"}})
    for n in list(ITEMS) + ["erfhekje", "leverkist", "dorpszender"]:
        _w(os.path.join(A, "items", n + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}})


# =====================================================================================================================
# data: recipes, loot, tags
# =====================================================================================================================
def data(add_tag):
    D = os.path.join(R, "data", "guhs")
    _w(os.path.join(D, "recipe", "dorpszender.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["guhs:vadszender", "guhs:guh_kristal", "guhs:guh_kristal", "guhs:guh_kristal", "guhs:guh_kristal",
                        "guhs:block_of_kaasknabbels"],
        "result": {"id": "guhs:dorpszender", "count": 1}})
    _w(os.path.join(D, "recipe", "leverkist.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" K ", "WCW", "PPP"],
        "key": {"K": "guhs:kaas_knabbels", "W": "minecraft:pink_wool", "C": "minecraft:chest", "P": "#minecraft:planks"},
        "result": {"id": "guhs:leverkist", "count": 1}})
    _w(os.path.join(D, "recipe", "glasscherf_glas.json"), {
        "type": "minecraft:smelting", "category": "blocks", "ingredient": "guhs:glasscherf", "result": {"id": "minecraft:glass"},
        "experience": 0.1, "cookingtime": 200})
    _w(os.path.join(D, "loot_table", "blocks", "erfhekje.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:erfhekje"}],
        "conditions": [{"condition": "minecraft:survives_explosion"},
                       {"condition": "minecraft:block_state_property", "block": "guhs:erfhekje", "properties": {"half": "lower"}}]}]})
    _w(os.path.join(D, "loot_table", "blocks", "leverkist.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:leverkist"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    add_tag("minecraft/tags/block/mineable/axe", ["guhs:erfhekje", "guhs:leverkist"])


def _add_tag(path, values):
    full = os.path.join(R, "data", path + ".json")
    d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    _w(full, d)


# =====================================================================================================================
# entry points
# =====================================================================================================================
def build(h):
    erfhekje_texturen()
    erfhekje_modellen()
    leverkist_texturen()
    leverkist_modellen()
    dorpszender(h)
    items()
    data(h.add_tag)
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)
    blokuitleg()


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    erfhekje_texturen()
    erfhekje_modellen()
    leverkist_texturen()
    leverkist_modellen()
    dorpszender(h)
    items()
    data(_add_tag)
    blokuitleg()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(TEKSTEN)
        _w(p, d)
    print(f"erfenis_koppeling: 3 blocks, {len(ITEMS)} items, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
