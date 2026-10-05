"""
De klusjes rond het Guhhuisje (2.10 "Lieve vadsjes van elkaar", slice klusjes; see guhs_work210/CONTRACT_210.md par. 5.4).

Ten chores the residents of a Guhhuisje do in its home base (the Java side is feature/klusjes): opgraven, farmen,
opruimen, dieren, bakken, vissen, waken, plukken, lampjes, oppas. This module makes:
  - block klusjes_guhlampje (a little standing lamp with guh ears and a glowing snoet-face; lit / unlit, 4 facings)
  - item klusjes_schelpje (a shiny pink shell: fished up, sometimes dug up; hold it to your ear)
  - loot tables gameplay/klusjes_opgraven and gameplay/klusjes_vissen; tags klusjes/lampjes, klusjes/graafgrond,
    klusjes/zeldzaam; recipes; particles klusjes_sterretje, klusjes_uitroep; sounds klusjes.*
  - the chore names + tips (polished), messages, first times + wist-je-datjes of every chore, advancements
    (tab lieve_vadsjes: klusjes_<id>, klusjes_alle, klusjes_honderd, klusjes_bank, klusjes_zeldzaam)
  - the game test room klusjes_test_tuin and the FTB quests of the section "Klusjes rond het huisje"
"""
import os

import numpy as np
from PIL import Image, ImageDraw

IDS = ["opgraven", "farmen", "opruimen", "dieren", "bakken", "vissen", "waken", "plukken", "lampjes", "oppas"]
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# =====================================================================================================================
# textures
# =====================================================================================================================
ROZE = (246, 168, 200)
ROZE_DONKER = (214, 120, 162)
LICHT = (255, 222, 236)
HOUT = (150, 82, 96)


def _ruis(size, basis, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _glas(aan, gezicht):
    """The lamp glass: warm and glowing when on (with a happy guh face), dim and asleep when off."""
    size = 16
    a = np.zeros((size, size, 4), np.uint8)
    for y in range(size):
        for x in range(size):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 10.6
            if aan:
                c = (255 - 10 * d, 236 - 40 * d, 214 - 30 * d)
            else:
                c = (176 - 20 * d, 150 - 20 * d, 170 - 18 * d)
            a[y, x, :3] = [max(0, min(255, int(v))) for v in c]
            a[y, x, 3] = 255
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    # the frame of the little window
    rand = (200, 110, 140, 255) if aan else (130, 84, 104, 255)
    d.rectangle([0, 0, 15, 15], outline=rand)
    if gezicht:
        oog = (58, 28, 60, 255)
        if aan:   # big shiny guh eyes, a pink snoet, blush
            for ex in (4, 10):
                d.ellipse([ex - 2, 5, ex + 1, 9], fill=oog)
                img.putpixel((ex - 1, 6), (255, 255, 255, 255))
            d.rectangle([7, 10, 8, 11], fill=(226, 98, 150, 255))
            img.putpixel((6, 12), (214, 86, 138, 255))
            img.putpixel((9, 12), (214, 86, 138, 255))
            for bx in (2, 13):
                img.putpixel((bx, 10), (255, 160, 190, 255))
                img.putpixel((bx - 1 if bx > 8 else bx + 1, 10), (255, 176, 200, 255))
        else:     # asleep: closed eyes (little arcs) and a tiny snoet
            for ex in (4, 10):
                d.line([(ex - 2, 7), (ex - 1, 8), (ex, 8), (ex + 1, 7)], fill=oog)
            d.rectangle([7, 10, 8, 11], fill=(170, 96, 130, 255))
    else:
        d.line([(3, 3), (5, 3)], fill=(255, 255, 255, 180) if aan else (200, 190, 200, 160))
    return img


def _kap():
    a = _ruis(16, ROZE, 8, 3101)
    rng = np.random.default_rng(3102)
    for _ in range(26):
        x, y = rng.integers(0, 16), rng.integers(0, 15)
        a[y, x, :3] = ROZE_DONKER
        a[y + 1, x, :3] = (236, 150, 186)
    return Image.fromarray(a)


def _oor():
    a = _ruis(16, ROZE, 5, 3103)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.ellipse([4, 3, 11, 15], fill=(255, 176, 206, 255))
    d.ellipse([5, 5, 10, 15], fill=(255, 196, 220, 255))
    return img


def _voet():
    a = _ruis(16, HOUT, 7, 3104)
    for y in (3, 8, 13):
        a[y, :, :3] = np.clip(a[y, :, :3].astype(int) - 22, 0, 255)
    return Image.fromarray(a)


def _icoon_lampje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # ears
    d.polygon([(4, 4), (5, 0), (7, 3)], fill=ROZE + (255,))
    d.polygon([(9, 3), (11, 0), (12, 4)], fill=ROZE + (255,))
    img.putpixel((5, 2), (255, 196, 220, 255))
    img.putpixel((11, 2), (255, 196, 220, 255))
    # cap
    d.rectangle([3, 3, 12, 5], fill=ROZE + (255,))
    d.line([(3, 5), (12, 5)], fill=ROZE_DONKER + (255,))
    # glowing glass with the face
    d.rectangle([4, 6, 11, 12], fill=(255, 232, 206, 255))
    d.rectangle([4, 6, 11, 12], outline=(200, 110, 140, 255))
    img.putpixel((6, 8), (58, 28, 60, 255))
    img.putpixel((9, 8), (58, 28, 60, 255))
    img.putpixel((7, 10), (226, 98, 150, 255))
    img.putpixel((8, 10), (226, 98, 150, 255))
    # foot
    d.rectangle([3, 13, 12, 14], fill=HOUT + (255,))
    d.line([(3, 14), (12, 14)], fill=(110, 58, 70, 255))
    return img


def _schelpje():
    """A scallop shell: a pink fan with white ridges and a little hinge at the bottom."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.pieslice([1, 1, 15, 17], 200, 340, fill=(250, 176, 206, 255), outline=(206, 110, 150, 255))
    for i, ang in enumerate(range(205, 340, 22)):
        import math
        r = 7.5
        x = 8 + math.cos(math.radians(ang)) * r
        y = 9 + math.sin(math.radians(ang)) * r
        d.line([(8, 11), (x, y)], fill=(255, 236, 244, 255) if i % 2 == 0 else (236, 150, 186, 255))
    d.rectangle([6, 11, 10, 13], fill=(236, 150, 186, 255))
    d.rectangle([5, 12, 11, 13], outline=(206, 110, 150, 255))
    img.putpixel((5, 4), (255, 255, 255, 255))
    img.putpixel((6, 3), (255, 255, 255, 255))
    return img


def _sterretje(variant):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    kern = (255, 246, 170, 255) if variant == 0 else (255, 220, 240, 255)
    rand = (255, 206, 80, 255) if variant == 0 else (246, 150, 196, 255)
    for x, y in ((3, 0), (4, 0), (3, 7), (4, 7), (0, 3), (0, 4), (7, 3), (7, 4)):
        img.putpixel((x, y), rand)
    for i in range(1, 7):
        img.putpixel((3, i), kern)
        img.putpixel((4, i), kern)
        img.putpixel((i, 3), kern)
        img.putpixel((i, 4), kern)
    for x, y in ((2, 2), (5, 2), (2, 5), (5, 5)):
        img.putpixel((x, y), rand)
    img.putpixel((3, 3), (255, 255, 255, 255))
    return img


def _uitroep():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    wit = (255, 255, 255, 255)
    roze = (236, 70, 140, 255)
    d.rounded_rectangle([5, 0, 10, 10], 2, fill=wit)
    d.ellipse([5, 11, 10, 15], fill=wit)
    d.rounded_rectangle([6, 1, 9, 9], 1, fill=roze)
    d.ellipse([6, 12, 9, 14], fill=roze)
    return img


def textures(h):
    h.save(_glas(True, True), "block", "klusjes_guhlampje_glas_aan.png")
    h.save(_glas(True, False), "block", "klusjes_guhlampje_glas_aan_zij.png")
    h.save(_glas(False, True), "block", "klusjes_guhlampje_glas_uit.png")
    h.save(_glas(False, False), "block", "klusjes_guhlampje_glas_uit_zij.png")
    h.save(_kap(), "block", "klusjes_guhlampje_kap.png")
    h.save(_oor(), "block", "klusjes_guhlampje_oor.png")
    h.save(_voet(), "block", "klusjes_guhlampje_voet.png")
    h.save(_icoon_lampje(), "item", "klusjes_guhlampje.png")
    h.save(_schelpje(), "item", "klusjes_schelpje.png")
    for v in (0, 1):
        h.save(_sterretje(v), "particle", f"klusjes_sterretje_{v}.png")
    h.save(_uitroep(), "particle", "klusjes_uitroep.png")


# =====================================================================================================================
# blocks, items, loot, recipes, tags
# =====================================================================================================================
def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    return {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for f in faces}}


def _lampje_model(aan):
    sfx = "aan" if aan else "uit"
    glas = {"north": {"texture": "#gezicht"}, "south": {"texture": "#glas"}, "east": {"texture": "#glas"}, "west": {"texture": "#glas"}}
    els = [
        _el([4, 0, 4], [12, 2, 12], "#voet"),
        {"from": [5, 2, 5], "to": [11, 9, 11], "faces": {**glas, "down": {"texture": "#voet"}}},
        _el([4, 9, 4], [12, 11, 12], "#kap"),
        _el([5, 11, 7], [7.5, 14, 8.5], "#oor", faces=("north", "south", "east", "west", "up"), uv=[4, 3, 11, 15]),
        _el([8.5, 11, 7], [11, 14, 8.5], "#oor", faces=("north", "south", "east", "west", "up"), uv=[4, 3, 11, 15]),
        _el([7, 11, 6], [9, 12, 8], "#kap", faces=("north", "south", "east", "west", "up")),     # a little tuft
    ]
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
            "textures": {"particle": "guhs:block/klusjes_guhlampje_kap", "voet": "guhs:block/klusjes_guhlampje_voet",
                         "kap": "guhs:block/klusjes_guhlampje_kap", "oor": "guhs:block/klusjes_guhlampje_oor",
                         "gezicht": f"guhs:block/klusjes_guhlampje_glas_{sfx}", "glas": f"guhs:block/klusjes_guhlampje_glas_{sfx}_zij"},
            "elements": els}


LOOT_OPGRAVEN = {"type": "minecraft:chest", "pools": [
    {"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 60, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
        {"type": "minecraft:item", "name": "guhs:kaasknabbelzaadjes", "weight": 12, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
        {"type": "minecraft:item", "name": "guhs:knabbelzaadjes", "weight": 10, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
        {"type": "minecraft:item", "name": "guhs:klusjes_schelpje", "weight": 5},
    ]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.06}], "entries": [
        {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 40, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
        {"type": "minecraft:item", "name": "guhs:marshmallow_knabbel", "weight": 25, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
        {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 20},
        {"type": "minecraft:item", "name": "guhs:vahoege_vads", "weight": 8},
        {"type": "minecraft:item", "name": "guhs:music_disc_ze_hangen", "weight": 2},
    ]},
]}
LOOT_VISSEN = {"type": "minecraft:chest", "pools": [
    {"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:guh_vis", "weight": 40},
        {"type": "minecraft:item", "name": "guhs:kaasvis", "weight": 18},
        {"type": "minecraft:item", "name": "guhs:vadsbaars", "weight": 12},
        {"type": "minecraft:item", "name": "guhs:guhpuffer", "weight": 6},
        {"type": "minecraft:item", "name": "guhs:njegforel", "weight": 4},
        {"type": "minecraft:item", "name": "guhs:klusjes_schelpje", "weight": 25, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
    ]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.02}], "entries": [
        {"type": "minecraft:item", "name": "guhs:gouden_guhvis"},
    ]},
]}
ZELDZAAM = ["guhs:gefrituurde_kaasknabbels", "guhs:marshmallow_knabbel", "guhs:guh_kristal", "guhs:vahoege_vads",
            "guhs:music_disc_ze_hangen", "guhs:gouden_guhvis"]
GRAAFGROND = ["minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:rooted_dirt",
              "minecraft:mycelium", "minecraft:sand", "minecraft:red_sand", "minecraft:moss_block", "guhs:kaasknabbel_dirt",
              "guhs:knuffelgras", "guhs:modderig_kaasgras", "guhs:rijpgras"]


def blocks_and_items(h):
    A, D = h.A, h.D
    for aan in (True, False):
        h.w(f"{A}/models/block/klusjes_guhlampje_{'aan' if aan else 'uit'}.json", _lampje_model(aan))
    h.w(f"{A}/blockstates/klusjes_guhlampje.json", {"variants": {
        f"facing={f},lit={l}": {"model": f"guhs:block/klusjes_guhlampje_{'aan' if l == 'true' else 'uit'}", **({"y": r} if r else {})}
        for f, r in ROT.items() for l in ("true", "false")}})
    h.item_model("klusjes_guhlampje")
    h.item_model("klusjes_schelpje")
    h.self_drop("klusjes_guhlampje")
    h.w(f"{D}/loot_table/gameplay/klusjes_opgraven.json", LOOT_OPGRAVEN)
    h.w(f"{D}/loot_table/gameplay/klusjes_vissen.json", LOOT_VISSEN)
    h.shaped("klusjes_guhlampje", ["W W", "GTG", " K "], {"W": "minecraft:pink_wool", "G": "minecraft:glass_pane", "T": "minecraft:torch",
                                                           "K": "guhs:kaas_knabbels"}, "guhs:klusjes_guhlampje", 2)
    h.shapeless("klusjes_schelpje_beendermeel", ["guhs:klusjes_schelpje"], "minecraft:bone_meal", 2)
    h.shapeless("klusjes_schelpjes_roze", ["guhs:klusjes_schelpje", "guhs:klusjes_schelpje"], "minecraft:pink_dye", 1)
    h.add_tag("guhs/tags/block/klusjes/lampjes", ["guhs:klusjes_guhlampje", "#minecraft:candles"])
    h.add_tag("guhs/tags/block/klusjes/graafgrond", GRAAFGROND)
    h.add_tag("guhs/tags/item/klusjes/zeldzaam", ZELDZAAM)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:klusjes_guhlampje"])


def particles(h):
    h.w(f"{h.A}/particles/klusjes_sterretje.json", {"textures": ["guhs:klusjes_sterretje_0", "guhs:klusjes_sterretje_1"]})
    h.w(f"{h.A}/particles/klusjes_uitroep.json", {"textures": ["guhs:klusjes_uitroep"]})


SOUNDS = {
    "klusjes.graaf": [{"name": "minecraft:item.brush.brushing.gravel", "type": "event", "pitch": 1.3, "volume": 0.7},
                      {"name": "minecraft:block.rooted_dirt.hit", "type": "event", "pitch": 1.2, "volume": 0.8}],
    "klusjes.piep": [{"name": "guhs:guh_ambient3", "pitch": 1.5}, {"name": "guhs:guh_ambient7", "pitch": 1.6},
                     {"name": "guhs:guh_ambient11", "pitch": 1.5}],
    "klusjes.klaar": [{"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.6, "volume": 0.6},
                      {"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.4, "volume": 0.8}],
    "klusjes.lampje": [{"name": "minecraft:block.lever.click", "type": "event", "pitch": 1.8, "volume": 0.5}],
    "klusjes.duw": [{"name": "minecraft:entity.slime.squish_small", "type": "event", "pitch": 1.4, "volume": 0.8},
                    {"name": "minecraft:block.wool.hit", "type": "event", "pitch": 0.8}],
    "klusjes.plons": [{"name": "minecraft:entity.fishing_bobber.splash", "type": "event", "pitch": 1.3, "volume": 0.5}],
    "klusjes.schelpje": [{"name": "minecraft:block.conduit.ambient.short", "type": "event", "pitch": 1.2, "volume": 0.9},
                         {"name": "minecraft:ambient.underwater.loop.additions", "type": "event", "pitch": 1.0, "volume": 0.8}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements, texts
# =====================================================================================================================
ADV = {  # id: (parent, icon, frame, title, description)
    "klusjes_opgraven": ("huisje_gebouwd", "minecraft:wooden_shovel", "task", "Graafguh", "Laat een bewoner van je Guhhuisje kaasknabbels opgraven"),
    "klusjes_farmen": ("huisje_gebouwd", "guhs:knabbelgraan", "task", "Boertje guh", "Laat een bewoner rijpe gewassen oogsten en opnieuw planten"),
    "klusjes_opruimen": ("huisje_gebouwd", "minecraft:chest", "task", "Opgeruimd staat netjes", "Laat een bewoner rondslingerende spulletjes opruimen"),
    "klusjes_dieren": ("huisje_gebouwd", "guhs:pluiswol", "task", "Lieve dierenoppas", "Laat een bewoner de boerderijdieren aaien en voeren, of een knabbelkorf oogsten"),
    "klusjes_bakken": ("huisje_gebouwd", "guhs:knabbeloven", "task", "Guh de bakker", "Laat een bewoner malen in een guh-molentje of bakken in een knabbeloven"),
    "klusjes_vissen": ("huisje_gebouwd", "guhs:guh_vis", "task", "Visje vangen", "Laat een bewoner guhvissen en schelpjes vangen in de vijver bij het huisje"),
    "klusjes_waken": ("huisje_gebouwd", "minecraft:bell", "task", "Waakguh", "Een bewoner piept en waarschuwt je voor een Mika of monster (en duwt Mika's zachtjes weg)"),
    "klusjes_plukken": ("huisje_gebouwd", "guhs:knabbelbessen", "task", "Bessenplukker", "Laat een bewoner bessen of bloemetjes plukken"),
    "klusjes_lampjes": ("huisje_gebouwd", "guhs:klusjes_guhlampje", "task", "Lichtjes aan", "Een bewoner doet 's avonds de lampjes aan (en 's ochtends gapend weer uit)"),
    "klusjes_oppas": ("huisje_gebouwd", "guhs:kaas_knabbels", "task", "Oppasguh", "Een bewoner verzorgt een muisje, schildpadje of een gewonde guh"),
    "klusjes_bank": ("klusjes_opruimen", "guhs:bank_guh", "task", "Alles op zijn plek", "Laat de spulletjes van klusjes in een Bank Guh sorteren"),
    "klusjes_zeldzaam": ("klusjes_opgraven", "guhs:guh_kristal", "goal", "Kijk wat ik vond!", "Een bewoner vindt iets zeldzaams bij het graven of vissen"),
    "klusjes_alle": ("klusjes_oppas", "minecraft:golden_shovel", "goal", "Klusjeskampioen", "Laat je bewoners alle tien de klusjes doen"),
    "klusjes_honderd": ("klusjes_alle", "minecraft:cake", "challenge", "Honderd keer vads geholpen", "Je bewoners deden samen honderd klusjes. VAHOEG!"),
}

KLUSSEN = {  # the chore names + the tips of the huisje screen (what it needs nearby)
    "opgraven": ("Kaasknabbels opgraven", "Graaft kaasknabbels op in gras, aarde of zand in de klus-area. Soms vindt hij iets zeldzaams, njeg! "
                 "Zet een kist naast het huisje voor de buit. (Guhs en muisjes)"),
    "farmen": ("Farmen", "Oogst rijpe gewassen en plant ze meteen opnieuw: tarwe, wortels, aardappels, bieten, kaasknabbelplantjes en "
               "guhtuintjes. Geeft dorstige tuintjes water. (Guhs)"),
    "opruimen": ("Opruimen & sorteren", "Raapt spulletjes op die rondslingeren en brengt ze naar de kist. Staat er een Bank Guh in de "
                 "klus-area? Dan wordt alles gesorteerd, ook wat er in de kist ligt. Nodig: een kist of Bank Guh. (Guhs en muisjes)"),
    "dieren": ("Dieren & bijen verzorgen", "Aait en voert guhschaapjes, knabbelkippetjes en guhkoeien, raapt wol en eitjes op en haalt "
               "eieren uit de kippennestjes. Oogst volle knabbelkorven. Leg knabbelvoer en lege flesjes (voor kaasmelk) in de kist! (Guhs)"),
    "bakken": ("Bakken & molen", "Brengt knabbelgraan uit de kist naar een guh-molentje en het meel terug, en bakt lekkers in een "
               "knabbeloven als de kist alles voor een recept heeft. Knabbelmeel = dubbel zoveel! (Guhs)"),
    "vissen": ("Vissen", "Vist guhvissen en schelpjes als er een vijvertje (3+ waterblokjes) in de klus-area is. Heel soms een Gouden "
               "Guhvis! (Guhs, Schilly en Poepschilly)"),
    "waken": ("Wachten & waarschuwen", "Piept en rent naar jou als er een Mika of monster bij het huisje komt, en duwt Mika's heel "
              "zachtjes weg. Nooit vechten, altijd lief! (Guhs; muisjes piepen alleen)"),
    "plukken": ("Bloemetjes & bessen plukken", "Plukt knabbelbessen en zoete bessen, en een bloemetje bij elke bloem (die blijft staan): alle soorten "
                "bloemen, het vaakst een roze guhbloem. Plant soms een nieuw bloemetje. (Guhs en muisjes)"),
    "lampjes": ("Lampjes aan & uit", "Doet 's avonds de guhlampjes en kaarsjes in de klus-area aan en 's ochtends weer uit, met een "
                "grote gaap. (Guhs)"),
    "oppas": ("Muisje-oppas & verzorgen", "Knuffelt en voert pieppiepmuisjes, Schilly en Poepschilly in de buurt. Een gewonde guh krijgt "
              "een snackje uit de kist (cupcake, koekje, kaasknabbels...). (Guhs)"),
}
EERSTE = {  # first time a guh does this chore: (title, what it writes in its dagboekje)
    "opgraven": ("Eerste keer gegraven", "Ik groef een gat en er zat een kaasknabbel in! De grond is eigenlijk gewoon een grote knabbeltrommel, njeg."),
    "farmen": ("Boertje guh", "Ik heb tarwe geplukt en meteen weer geplant. Dat heet boeren. Ik ben nu een boer. Met een vads."),
    "opruimen": ("Opgeruimd staat netjes", "Ik heb alles opgeruimd wat op de grond lag. Ook een steentje. Het steentje ligt nu in de kist. Graag gedaan."),
    "dieren": ("Dierenvriendje", "Ik heb een guhschaapje geaaid. Het schaapje heeft mij terug geaaid. Denk ik. Het was in ieder geval heel zacht."),
    "bakken": ("Guh de bakker", "Ik heb gebakken! Er kwam iets lekkers uit de oven en ik heb het NIET opgegeten. Bijna niet."),
    "vissen": ("Eerste visje", "Ik heb een vis gevangen met mijn pootjes. De vis was net zo verbaasd als ik, njeg."),
    "waken": ("Waakguh", "Er kwam een Mika bij ons huisje. Ik heb heel hard gepiept. Ik ben heel dapper. Een beetje. Van een afstandje."),
    "plukken": ("Plukguh", "Ik heb bessen geplukt. Er zijn er een paar in de kist gekomen. De rest zit in mijn vads. Oepsie."),
    "lampjes": ("Lampjesguh", "Ik heb de lampjes aangedaan, zodat niemand in het donker hoeft te zitten. 's Ochtends doe ik ze weer uit. Met een gaap."),
    "oppas": ("Oppasguh", "Ik heb op een muisje gepast. Het muisje heeft op mij gepast. We zijn nu allebei heel goed opgepast."),
}
WISTJEDAT = {  # gui.guhs.wistjedat.klusjes.<id>: the guh writes it the first time it does the chore (%s = the huisje)
    "opgraven": "Wist je dat er onder %s kaasknabbels in de grond zitten? Ik heb het zelf ontdekt. Met mijn eigen pootjes!",
    "farmen": "Vandaag heb ik bij %s geoogst. Ik heb ook een zaadje teruggestopt, anders wordt de grond verdrietig.",
    "opruimen": "Wist je dat %s nu het netste huisje van de hele wereld is? Dat komt door mij. En een beetje door de kist.",
    "dieren": "Wist je dat een guhkoe moeh zegt als je haar aait? Ik zeg dan njeg terug. Zo praten wij samen bij %s.",
    "bakken": "Het ruikt bij %s naar versgebakken koekjes. Ik weet niet wie dat gedaan heeft. (Ik.)",
    "vissen": "Wist je dat vissen niet van kaasknabbels houden? Ik wel. Daarom vis ik bij %s, en eet ik zelf de knabbels.",
    "waken": "Ik pas op %s. Als er een Mika komt, piep ik zo hard als ik kan. Daarna duw ik hem heel zachtjes weg. Lief, maar streng.",
    "plukken": "Wist je dat een bloemetje niet boos wordt als je er eentje van plukt? Ik heb het bij %s gevraagd. Het zei niks, dus het is goed.",
    "lampjes": "Bij %s gaan de lampjes aan als het donker wordt. Dat doe ik. Ik ben eigenlijk een beetje de zon, maar dan kleiner.",
    "oppas": "Vandaag heb ik bij %s voor iemand gezorgd. Het voelde warm in mijn vads. Zo voelt lief zijn, denk ik.",
    "zeldzaam": "Vandaag vond ik %s bij %s. VAHOEG! Ik heb er drie keer aan geroken voor ik het in de kist deed.",
}
TEXTS = {
    "block.guhs.klusjes_guhlampje": "Guhlampje",
    "block.guhs.klusjes_guhlampje.lore": "Een lampje met oortjes en een lief gezichtje. Rechtsklik: aan of uit. Bewoners van een Guhhuisje "
                                         "doen hem 's avonds aan en 's ochtends uit!",
    "item.guhs.klusjes_schelpje": "Schelpje",
    "item.guhs.klusjes_schelpje.lore": "Opgevist door een huisjesguh. Houd hem aan je oor (rechtsklik) en luister naar de Guhzee.",
    "item.guhs.klusjes_schelpje.oor.0": "Ruisss... je hoort de Guhzee. En heel zachtjes: njeg.",
    "item.guhs.klusjes_schelpje.oor.1": "Ruisss... ergens ver weg klotst een golfje kaassaus.",
    "item.guhs.klusjes_schelpje.oor.2": "Ruisss... een visje fluistert: \"die guh vist best goed, hoor\".",
    "item.guhs.klusjes_schelpje.oor.3": "Ruisss... je hoort een guh snurken. O nee, dat is je eigen guh.",
    "item.guhs.klusjes_schelpje.oor.4": "Ruisss... VAHOEG! (de zee is blij dat je luistert)",
    "item.guhs.klusjes_schelpje.oor.5": "Ruisss... het schelpje ruikt naar zout en een klein beetje naar kaasknabbels.",
    "gui.guhs.klusjes.zeldzaam": "✦ %1$s heeft iets zeldzaams gevonden bij %3$s: %2$s! VAHOEG!",
    "gui.guhs.klusjes.waken.mika": "een Mika",
    "gui.guhs.klusjes.waken.monster": "een %s",
    "gui.guhs.klusjes.waken.hier": "Piep piep! %1$s komt je waarschuwen: pas op, %2$s bij %3$s!",
    "gui.guhs.klusjes.waken.ver": "Piep piep! %1$s waarschuwt vanuit %3$s: er sluipt %2$s rond het huisje, njeg!",
    "subtitles.guhs.klusjes.graaf": "Guh graaft",
    "subtitles.guhs.klusjes.piep": "Guh piept een waarschuwing",
    "subtitles.guhs.klusjes.klaar": "Klusje klaar",
    "subtitles.guhs.klusjes.lampje": "Lampje klikt",
    "subtitles.guhs.klusjes.duw": "Guh duwt zachtjes",
    "subtitles.guhs.klusjes.plons": "Plonsje",
    "subtitles.guhs.klusjes.schelpje": "De Guhzee ruist",
}


def advancements(h):
    from features import band
    for name, (parent, icon, frame, title, desc) in ADV.items():
        band.visible(h, name, parent, icon, frame, title, desc)


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    for k, (naam, tip) in KLUSSEN.items():
        h.lang(f"gui.guhs.klus.{k}", naam, naam)
        h.lang(f"gui.guhs.klus.{k}.tip", tip, tip)
    for k, (titel, tekst) in EERSTE.items():
        h.lang(f"gui.guhs.dagboek.eerste.klusjes_{k}", titel, titel)
        h.lang(f"gui.guhs.dagboek.eerste.klusjes_{k}.tekst", tekst, tekst)
    for k, v in WISTJEDAT.items():
        h.lang(f"gui.guhs.wistjedat.klusjes.{k}", v, v)


# =====================================================================================================================
# the game test room, the self-check
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((24, 8, 24))
    for x in range(24):
        for z in range(24):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("klusjes_test_tuin")


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for p in (f"{A}/blockstates/klusjes_guhlampje.json", f"{A}/models/block/klusjes_guhlampje_aan.json",
              f"{A}/models/block/klusjes_guhlampje_uit.json", f"{A}/models/item/klusjes_guhlampje.json",
              f"{A}/models/item/klusjes_schelpje.json", f"{D}/loot_table/blocks/klusjes_guhlampje.json",
              f"{D}/loot_table/gameplay/klusjes_opgraven.json", f"{D}/loot_table/gameplay/klusjes_vissen.json",
              f"{A}/particles/klusjes_sterretje.json", f"{A}/particles/klusjes_uitroep.json"):
        if not os.path.exists(p):
            missing.append(p)
    for name in ("klusjes_guhlampje_glas_aan", "klusjes_guhlampje_glas_uit", "klusjes_guhlampje_kap", "klusjes_guhlampje_oor",
                 "klusjes_guhlampje_voet"):
        if not os.path.exists(os.path.join(h.TEX, "block", name + ".png")):
            missing.append(name)
    for k in IDS:
        for key in (f"gui.guhs.klus.{k}", f"gui.guhs.klus.{k}.tip", f"gui.guhs.dagboek.eerste.klusjes_{k}",
                    f"gui.guhs.wistjedat.klusjes.{k}", f"advancements.guhs.lieve_vadsjes.klusjes_{k}.title"):
            if key not in h.NL:
                missing.append(key)
    for i in range(6):
        if f"item.guhs.klusjes_schelpje.oor.{i}" not in h.NL:
            missing.append(f"oor.{i}")
    if missing:
        raise SystemExit(f"klusjes: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    particles(h)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (section "Klusjes rond het huisje" of the chapter guhs_band; no locking)
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv

    def a(name):
        return adv(f"guhs:lieve_vadsjes/{name}")

    q("klusjes_lampje", "Een lampje met oortjes", "Maak een paar &dguhlampjes&r (roze wol, glas, een fakkel en een kaasknabbel). Rechtsklik zet "
      "ze aan of uit, maar bewoners met het klusje &eLampjes&r doen dat 's avonds en 's ochtends vanzelf!",
      "guhs:klusjes_guhlampje", [item("guhs:klusjes_guhlampje")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=50)
    q("klusjes_opgraven", "Graafguh", "Laat een bewoner van je Guhhuisje &ekaasknabbels opgraven&r: hij graaft in gras, aarde of zand in de "
      "klus-area. Zet een kist naast het huisje, daar komt de buit in. Pieppiepmuisjes graven ook graag mee!",
      "minecraft:wooden_shovel", [a("klusjes_opgraven")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("klusjes_farmen", "Boertje guh", "Plant tarwe, wortels, kaasknabbelplantjes of guhtuintjes bij het huisje. Bewoners met het klusje &eFarmen&r "
      "oogsten alles wat rijp is en planten het meteen opnieuw. Dorstige tuintjes krijgen ook water!",
      "guhs:knabbelgraan", [a("klusjes_farmen")], rewards=(("guhs:knabbelzaadjes", 4),), xp=50)
    q("klusjes_opruimen", "Opgeruimd staat netjes", "Slingert er van alles rond bij het huisje? Het klusje &eOpruimen&r raapt het op en brengt het "
      "naar de kist. Wel een kist (of Bank Guh) neerzetten, anders weet je guh niet waar het heen moet, njeg.",
      "minecraft:chest", [a("klusjes_opruimen")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("klusjes_bank", "Alles op zijn plek", "Zet een &dBank Guh&r in de klus-area van je huisje. Alles wat je bewoners verzamelen wordt dan netjes "
      "in de Bank Guh gesorteerd, en met Opruimen ook wat er nog in de kist ligt. Geleende spulletjes blijven altijd in de kist.",
      "guhs:bank_guh", [a("klusjes_bank")], rewards=(("guhs:gefrituurde_kaasknabbels", 2),), xp=100)
    q("klusjes_dieren", "Lieve dierenoppas", "Houd guhschaapjes, knabbelkippetjes of een guhkoe bij het huisje. Het klusje &eDieren & bijen&r aait en "
      "voert ze (leg knabbelvoer in de kist), raapt wol en eitjes op, melkt de guhkoe (met lege flesjes uit de kist) en oogst volle "
      "knabbelkorven.", "guhs:pluiswol", [a("klusjes_dieren")], rewards=(("guhs:knabbelvoer", 8),), xp=50)
    q("klusjes_bakken", "Guh de bakker", "Zet een &dguh-molentje&r en een &dknabbeloven&r bij het huisje. Met knabbelgraan in de kist maalt je guh meel, "
      "en als de kist alles voor een recept heeft, bakt hij lekkers voor je. Met knabbelmeel wordt het dubbel zoveel!",
      "guhs:knabbeloven", [a("klusjes_bakken")], rewards=(("guhs:knabbelgraan", 8),), xp=100)
    q("klusjes_vissen", "Visje vangen", "Graaf een vijvertje (minstens 3 waterblokjes) in de klus-area. Bewoners met het klusje &eVissen&r vangen "
      "guhvissen en &dschelpjes&r. Schilly en Poepschilly vissen ook mee!", "guhs:guh_vis", [a("klusjes_vissen")],
      rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("klusjes_zeldzaam", "Kijk wat ik vond!", "Heel soms graaft of vist een bewoner iets &6zeldzaams&r op: een guhkristal, gefrituurde "
      "kaasknabbels, zelfs een Gouden Guhvis... Je hoort het meteen, want hij is er zelf ook heel blij mee. VAHOEG!",
      "guhs:guh_kristal", [a("klusjes_zeldzaam")], rewards=(("guhs:gefrituurde_kaasknabbels", 3),), shape="gear", xp=200)
    q("klusjes_waken", "Waakguh", "Bewoners met het klusje &eWachten & waarschuwen&r piepen als er een Mika of monster bij het huisje komt, rennen "
      "naar je toe en duwen Mika's heel zachtjes weg. Vechten doen ze nooit: guhs zijn altijd lief!",
      "minecraft:bell", [a("klusjes_waken")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("klusjes_plukken", "Bessenplukker", "Knabbelbessen, zoete bessen of bloemen bij het huisje? Het klusje &ePlukken&r plukt ze voor je (de struikjes en "
      "bloemetjes blijven staan): bij een bloem komt er een bloemetje van een willekeurige soort mee, het vaakst een roze "
      "guhbloem. Soms plant je guh zelfs een nieuw bloemetje.",
      "guhs:knabbelbessen", [a("klusjes_plukken")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("klusjes_lampjes", "Lichtjes aan", "Zet guhlampjes (of kaarsjes) rond je huisje. Als het avond wordt, doet een bewoner ze aan, en 's "
      "ochtends gapend weer uit.", "guhs:klusjes_guhlampje", [a("klusjes_lampjes")], rewards=(("guhs:marshmallow_knabbel", 2),), xp=50)
    q("klusjes_oppas", "Oppasguh", "Laat pieppiepmuisjes, Schilly of Poepschilly bij het huisje rondscharrelen: het klusje &eOppas&r knuffelt en "
      "voert ze. Een gewonde guh krijgt van een andere guh een snackje uit de kist en is weer helemaal beter.",
      "guhs:kaas_knabbels", [a("klusjes_oppas")], rewards=(("guhs:guh_cupcake", 2),), xp=50)
    q("klusjes_alle", "Klusjeskampioen", "Laat je bewoners &6alle tien&r de klusjes doen. In het huisje-scherm zie je per bewoner welke klusjes "
      "hij doet (en wat erbij nodig is). Wat een harde werkers, en wat een vads!", "minecraft:golden_shovel", [a("klusjes_alle")],
      rewards=(("guhs:vahoege_vads_ingot", 1),), shape="gear", xp=500)
