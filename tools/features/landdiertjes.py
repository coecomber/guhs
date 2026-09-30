"""
3.0 (Guhverhalen), slice landdiertjes: the little land critters of the Guhmensie (DESIGN_30 §6) and Sjokkel (§3).
Java: feature/landdiertjes (LanddiertjesFeature, Landdiertje + the four critters, KnabbelvoorraadjeBlock, ShucklePlekjeBlock,
PolijstenKlus).

  build(h)   the models/animations/textures (landdiertjes_modellen), item icons and block textures, the blocks' models and loot,
             the recipe (steentjespad), sounds, the spawns (biome modifiers landdiertjes_*), the Guhdex pages, the texts (Dutch
             in both files), the advancements of the Diertjes tab (diertjes/landdiertjes_*), the game test rooms
             (landdiertjes_test_*), and a self-check
  ftb(fq)    the quests of the section "Egeltjes, konijntjes, eekhoorntjes & Sjokkel" (chapter guhs_diertjes; nothing locked)

The critters: pluisegeltje (Vadswoud; rolls up; sweet berries), guh_konijntje (Guhweides + white ones in the
Sneeuwguhtoendra; lop ears; carrots/kaasknabbels), pluiseekhoorntje (Vadswoud, Roze pluisjes, Guhvelden: the guhbloesem
trees; knabbel stashes; your shoulder; kaasknabbels) and Sjokkel (NOT a guh: the kloon-eiland via the shuckle-plekjes, rarely
the Gatenkaasgrotten; hides in its shell; berries -> bessensapje; chore "stenen polijsten"). All tameable, pick-up-able
(<id>_item), Guhhuisje residents. No clothes (CONTRACT_30 §5.4).
"""
import os

from PIL import Image, ImageDraw

from features import landdiertjes_modellen as modellen
from features import verhaal

BONES = {}
CLOTHES = []
FTB_PORTRAIT = "geo:shuckle:shuckle"

DIEREN = ["pluisegeltje", "guh_konijntje", "pluiseekhoorntje", "shuckle"]
KORT = {"pluisegeltje": "egeltje", "guh_konijntje": "konijntje", "pluiseekhoorntje": "eekhoorntje", "shuckle": "shuckle"}

# =====================================================================================================================
# texts (Dutch in both files)
# =====================================================================================================================
DEX = {  # page -> (name, rarity, info)
    "pluisegeltje": ("Pluisegeltje", "Gewoon in het Vadswoud",
                     "Een klein egeltje met zachte, pluizige stekeltjes: ze prikken nooit, het zijn eigenlijk knuffelstekeltjes. Als het "
                     "schrikt rolt het zich op tot een pluizig bolletje (sluip, dan blijft het rustig). Het is dol op zoete bessen en "
                     "loopt dwars door bessenstruiken zonder au. Tem het met zoete bessen!"),
    "guh_konijntje": ("Guh-konijntje", "Gewoon in de Guhweides (en wit in de Sneeuwguhtoendra)",
                      "Een rond konijntje met lange hangoortjes, een wiebelsnoetje en een pomponstaartje. Er zijn roze, witte, choco en "
                      "grijze. Het huppelt overal naartoe en rent weg als je te hard aan komt stampen. Tem het met een wortel of een "
                      "kaasknabbel. Getemd doet het een blije binky: hophop!"),
    "pluiseekhoorntje": ("Pluiseekhoorntje", "In de guhbloesembomen en het Vadswoud",
                         "Een pluizig eekhoorntje met een enorme krulstaart en pluimpjes op zijn oortjes. Het verstopt overal "
                         "voorraadjes kaasknabbels in de grond (zoek de hoopjes aarde!). Tem het met kaasknabbels: dan zit het op je "
                         "schouder en graaft het af en toe een voorraadje voor je op."),
    "shuckle": ("Sjokkel", "Zeldzaam: rond het kloon-eiland, en heel af en toe diep in de Gatenkaasgrotten",
                "Geen guh! Een rode schelp vol kaasgaatjes, een geel kopje met kraaloogjes en gele pootjes die uit de gaatjes steken. "
                "Heel, heel traag, en heel verlegen: kom je te dichtbij, dan kruipt hij in zijn schelp. Sluip, en geef hem zoete "
                "bessen. Getemd maakt hij van bessen een heerlijk bessensapje, en in een guhhuisje polijst hij stenen."),
}

LANG = {
    # blocks and items
    "block.guhs.shuckle_plekje": "Sjokkel-plekje",
    "block.guhs.landdiertjes_knabbelvoorraadje": "Knabbelvoorraadje",
    "block.guhs.landdiertjes_steentjespad": "Guhsteentjespad",
    "item.guhs.landdiertjes_guhsteentje": "Gepolijst guhsteentje",
    "item.guhs.landdiertjes_guhsteentje.lore": "Glad en glimmend gepoetst door een Sjokkel. Vier ervan maken een guhsteentjespad.",
    "item.guhs.landdiertjes_bessensapje": "Sjokkel-bessensapje",
    "item.guhs.landdiertjes_bessensapje.lore": "Zoete bessen, heel geduldig tot sapje gemaakt in de schelp van een Sjokkel. Slurp! Njeg.",
    "item.guhs.pluisegeltje_item": "Pluisegeltje",
    "item.guhs.pluisegeltje_item.tooltip": "Een opgepakt pluisegeltje (met naam en al). Rechtsklik op een blok: het snuffelt er weer vanaf.",
    "item.guhs.guh_konijntje_item": "Guh-konijntje",
    "item.guhs.guh_konijntje_item.tooltip": "Een opgepakt guh-konijntje (met naam en al). Rechtsklik op een blok: hop, eraf!",
    "item.guhs.pluiseekhoorntje_item": "Pluiseekhoorntje",
    "item.guhs.pluiseekhoorntje_item.tooltip": "Rechtsklik op een blok: het eekhoorntje springt eraf. Rechtsklik in de lucht: op je schouder!",
    "item.guhs.shuckle_item": "Sjokkel",
    "item.guhs.shuckle_item.tooltip": "Een opgepakte Sjokkel (met naam en al). Hij zit lekker in zijn schelp. Rechtsklik op een blok: heel traag eraf.",
    "item.guhs.pluisegeltje_spawn_egg": "Pluisegeltje-spawnei",
    "item.guhs.guh_konijntje_spawn_egg": "Guh-konijntje-spawnei",
    "item.guhs.pluiseekhoorntje_spawn_egg": "Pluiseekhoorntje-spawnei",
    "item.guhs.shuckle_spawn_egg": "Sjokkel-spawnei",
    # the little menu (the piep-maatje menu) and picking up
    "gui.guhs.piep.menu.sub.pluisegeltje": "Pluisegeltje: zachte stekeltjes, heel veel knuffel",
    "gui.guhs.piep.menu.sub.guh_konijntje": "Guh-konijntje: hangoortjes en hophop",
    "gui.guhs.piep.menu.sub.pluiseekhoorntje": "Pluiseekhoorntje: verstopt knabbels en zit op je schouder",
    "gui.guhs.piep.menu.sub.shuckle": "Sjokkel: geen guh, wel heel lief (en heel traag)",
    "gui.guhs.piep.menu.speciaal.pluisegeltje": "Rol eens!",
    "gui.guhs.piep.menu.speciaal.pluisegeltje.tooltip": "Je egeltje rolt zich op tot een pluizig bolletje en rolt een rondje om je heen. Wiee!",
    "gui.guhs.piep.menu.speciaal.guh_konijntje": "Hophop!",
    "gui.guhs.piep.menu.speciaal.guh_konijntje.tooltip": "Een blije binky! En jij krijgt even konijnensprongetjes (hoger springen).",
    "gui.guhs.piep.menu.speciaal.pluiseekhoorntje": "Op mijn schouder!",
    "gui.guhs.piep.menu.speciaal.pluiseekhoorntje.tooltip": "Het eekhoorntje klimt op je schouder. Sluip + rechtsklik op een blok (lege hand): eraf.",
    "gui.guhs.piep.menu.speciaal.shuckle": "Bessensapje!",
    "gui.guhs.piep.menu.speciaal.shuckle.tooltip": "Met 3 zoete bessen in zijn buikje kruipt Sjokkel in zijn schelp en maakt er een bessensapje van.",
    "gui.guhs.piep.opgepakt.pluisegeltje": "Je pakt %s op. Heel voorzichtig... maar het prikt helemaal niet. Pluis!",
    "gui.guhs.piep.opgepakt.guh_konijntje": "Je pakt %s op. Zijn hangoortjes flappen vrolijk. Hop!",
    "gui.guhs.piep.opgepakt.pluiseekhoorntje": "Je pakt %s op. Zijn staart kriebelt aan je neus!",
    "gui.guhs.piep.opgepakt.shuckle": "Je pakt %s op. Hij trekt zijn kopje in: tink!",
    "gui.guhs.piep.neergezet.pluisegeltje": "%s snuffelt weer rond. Snuf snuf!",
    "gui.guhs.piep.neergezet.guh_konijntje": "%s huppelt weer rond. Hophop!",
    "gui.guhs.piep.neergezet.pluiseekhoorntje": "%s springt eraf en kijkt meteen of er ergens knabbels liggen.",
    "gui.guhs.piep.neergezet.shuckle": "%s steekt heel voorzichtig zijn kopje weer naar buiten. Hallo!",
    # what they do
    "gui.guhs.landdiertjes.getemd.pluisegeltje": "%s rolt zich uit en snuffelt aan je hand. Jullie zijn vriendjes! Pluis!",
    "gui.guhs.landdiertjes.getemd.guh_konijntje": "%s doet een blije binky. Jullie zijn vriendjes! Hophop!",
    "gui.guhs.landdiertjes.getemd.pluiseekhoorntje": "%s propt de knabbel in zijn wangetjes en kijkt je heel lief aan. Vriendjes!",
    "gui.guhs.landdiertjes.getemd.shuckle": "%s komt helemaal uit zijn schelp en knippert met zijn kraaloogjes. Vriendjes! (heel langzaam)",
    "gui.guhs.landdiertjes.egeltje_opgerold": "Het egeltje is opgerold... Wacht even heel stil (of sluip), dan komt het weer tevoorschijn.",
    "gui.guhs.landdiertjes.egeltje_rondje": "%s rolt een rondje om je heen. Wiee! Pluis!",
    "gui.guhs.landdiertjes.hophop": "%s doet een blije binky! Jij krijgt konijnensprongetjes. Hophop!",
    "gui.guhs.landdiertjes.op_schouder": "%s klimt op je schouder. (Sluip + rechtsklik op een blok: eraf.)",
    "gui.guhs.landdiertjes.schouder_vol": "Er zit al iemand op je schouder, njeg.",
    "gui.guhs.landdiertjes.voorraadje_gevonden": "Een knabbelvoorraadje! Je graaft %s kaasknabbels op. VAHOEG!",
    "gui.guhs.landdiertjes.voorraadje_cadeau": "%s heeft een voorraadje voor je opgegraven! Kijk eens in dat hoopje aarde.",
    "gui.guhs.landdiertjes.shuckle_in_schelp": "Sjokkel zit in zijn schelp... Sluip dichterbij en wacht even, dan komt hij eruit.",
    "gui.guhs.landdiertjes.shuckle_bessen": "%s bewaart de bessen in zijn schelp (%s). Straks wordt het sapje!",
    "gui.guhs.landdiertjes.shuckle_sapje": "%s kruipt in zijn schelp en... blub blub... het wordt een bessensapje!",
    "gui.guhs.landdiertjes.shuckle_geen_bessen": "%s heeft eerst %s zoete bessen nodig voor een sapje. Njeg.",
    # the chore
    "gui.guhs.klus.polijsten": "Stenen polijsten",
    "gui.guhs.klus.polijsten.tip": "Alleen Sjokkel: keien in de kist worden gladde steen en glimmende guhsteentjes",
    "gui.guhs.dagboek.eerste.klusjes_polijsten": "Stenen gepoetst",
    "gui.guhs.dagboek.eerste.klusjes_polijsten.tekst": "Ik heb stenen gepoetst. Heel langzaam. Heel glad. Heel mooi.",
    "gui.guhs.wistjedat.klusjes.polijsten": "Wist je dat de stenen bij %s nu glimmen? Ik heb ze gepoetst met mijn pootjes. Tink tink.",
    # sounds
    "subtitles.guhs.landdiertjes.egeltje": "Egeltje snuffelt",
    "subtitles.guhs.landdiertjes.egeltje.rol": "Egeltje rolt zich op",
    "subtitles.guhs.landdiertjes.konijntje": "Konijntje piept",
    "subtitles.guhs.landdiertjes.eekhoorntje": "Eekhoorntje kwettert",
    "subtitles.guhs.landdiertjes.shuckle": "Sjokkel mompelt",
    "subtitles.guhs.landdiertjes.shuckle.dicht": "Sjokkel kruipt in zijn schelp",
    "subtitles.guhs.landdiertjes.shuckle.open": "Sjokkel kijkt naar buiten",
    "subtitles.guhs.landdiertjes.shuckle.tik": "Tink! (tegen de schelp)",
    "subtitles.guhs.landdiertjes.polijsten": "Sjokkel polijst een steen",
}

SOUNDS = {
    "landdiertjes.egeltje": [{"name": "minecraft:entity.fox.sniff", "type": "event", "pitch": 1.6},
                             {"name": "minecraft:entity.rabbit.ambient", "type": "event", "pitch": 1.3, "volume": 0.6}],
    "landdiertjes.egeltje.rol": [{"name": "minecraft:block.wool.place", "type": "event", "pitch": 1.4},
                                 {"name": "minecraft:entity.armadillo.roll", "type": "event", "pitch": 1.3}],
    "landdiertjes.konijntje": [{"name": "minecraft:entity.rabbit.ambient", "type": "event", "pitch": 1.25}],
    "landdiertjes.eekhoorntje": [{"name": "minecraft:entity.fox.ambient", "type": "event", "pitch": 1.8},
                                 {"name": "minecraft:entity.parrot.ambient", "type": "event", "pitch": 1.9, "volume": 0.5}],
    "landdiertjes.shuckle": [{"name": "minecraft:entity.turtle.ambient_land", "type": "event", "pitch": 1.3},
                             {"name": "minecraft:entity.frog.ambient", "type": "event", "pitch": 1.5, "volume": 0.5}],
    "landdiertjes.shuckle.dicht": [{"name": "minecraft:entity.shulker.close", "type": "event", "pitch": 1.5, "volume": 0.6}],
    "landdiertjes.shuckle.open": [{"name": "minecraft:entity.shulker.open", "type": "event", "pitch": 1.5, "volume": 0.6}],
    "landdiertjes.shuckle.tik": [{"name": "minecraft:block.amethyst_block.hit", "type": "event", "pitch": 1.6}],
    "landdiertjes.polijsten": [{"name": "minecraft:block.grindstone.use", "type": "event", "pitch": 1.6, "volume": 0.4},
                               {"name": "minecraft:block.amethyst_cluster.hit", "type": "event", "pitch": 1.4, "volume": 0.5}],
}

SPAWNS = {  # biome modifier -> (biomes, [(entity, weight, min, max)])
    "landdiertjes_egeltjes": (["guhs:vadswoud"], [("guhs:pluisegeltje", 12, 1, 3)]),
    "landdiertjes_konijntjes": (["guhs:guh_meadows", "guhs:sneeuwguhtoendra"], [("guhs:guh_konijntje", 10, 2, 3)]),
    "landdiertjes_eekhoorntjes": (["guhs:vadswoud", "guhs:pink_puffs", "guhs:guh_fields"], [("guhs:pluiseekhoorntje", 10, 1, 3)]),
    "landdiertjes_shuckle": (["guhs:gatenkaasgrotten"], [("guhs:shuckle", 2, 1, 1)]),
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for page, (name, rarity, info) in DEX.items():
        h.lang(f"entity.guhs.{page}", name, name)
        h.lang(f"gui.guhs.guhdex.rarity.{page}", "Zeldzaamheid: " + rarity, "Zeldzaamheid: " + rarity)
        h.lang(f"gui.guhs.guhdex.info.{page}", info, info)
        h.w(f"{h.D}/advancement/quest/seen_{page}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
# item icons and block textures (16x16, drawn)
# =====================================================================================================================
def _img():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def icon_egeltje():
    img, d = _img()
    d.ellipse([3, 4, 15, 14], fill=(138, 88, 82, 255))                       # the spiky back (seen from the side, facing left)
    for (x, y) in ((5, 4), (8, 3), (11, 4), (13, 6), (14, 9), (4, 6), (7, 5), (10, 5), (12, 8)):
        d.line([x, y + 2, x + 1, y], fill=(255, 232, 224, 255))
    d.ellipse([1, 8, 8, 14], fill=(252, 232, 214, 255))                      # the cream face
    d.rectangle([0, 11, 1, 12], fill=(120, 56, 72, 255))                     # the nose
    d.point((4, 10), fill=(20, 20, 34, 255))
    d.point((4, 9), fill=(255, 255, 255, 255))
    d.point((5, 12), fill=(255, 150, 186, 255))
    d.rectangle([5, 14, 6, 15], fill=(246, 208, 192, 255))
    d.rectangle([11, 14, 12, 15], fill=(246, 208, 192, 255))
    return img


def icon_konijntje():
    img, d = _img()
    fur, light = (250, 196, 214, 255), (255, 238, 244, 255)
    d.ellipse([3, 3, 13, 13], fill=fur)                                      # the round head
    d.rounded_rectangle([1, 5, 4, 14], 2, fill=fur, outline=(226, 150, 180, 255))    # the lop ears
    d.rounded_rectangle([12, 5, 15, 14], 2, fill=fur, outline=(226, 150, 180, 255))
    for x in (5, 9):                                                         # big guh eyes
        d.rectangle([x, 6, x + 2, 9], fill=(22, 18, 40, 255))
        d.point((x + 1, 9), fill=(60, 170, 220, 255))
        d.point((x + 2, 6), fill=(255, 255, 255, 255))
    d.rectangle([7, 10, 8, 11], fill=light)
    d.point((7, 10), fill=(236, 110, 150, 255))
    d.point((8, 10), fill=(236, 110, 150, 255))
    d.point((5, 11), fill=(255, 150, 186, 255))
    d.point((10, 11), fill=(255, 150, 186, 255))
    return img


def icon_eekhoorntje():
    img, d = _img()
    d.ellipse([7, 1, 15, 12], fill=(238, 156, 98, 255), outline=(192, 104, 58, 255))   # the big curled tail
    d.ellipse([9, 3, 13, 8], fill=(255, 232, 204, 255))
    d.ellipse([2, 7, 10, 15], fill=(226, 136, 82, 255))                      # the body
    d.ellipse([1, 3, 8, 10], fill=(226, 136, 82, 255))                       # the head
    d.rectangle([2, 1, 3, 3], fill=(192, 104, 58, 255))                      # ear tufts
    d.rectangle([6, 1, 7, 3], fill=(192, 104, 58, 255))
    d.ellipse([4, 10, 8, 14], fill=(255, 232, 204, 255))                     # the belly
    d.point((3, 6), fill=(20, 20, 34, 255))
    d.point((6, 6), fill=(20, 20, 34, 255))
    d.point((3, 5), fill=(255, 255, 255, 255))
    d.point((6, 5), fill=(255, 255, 255, 255))
    d.point((4, 8), fill=(150, 70, 60, 255))
    d.point((2, 8), fill=(255, 170, 170, 255))
    d.point((7, 8), fill=(255, 170, 170, 255))
    return img


def icon_shuckle():
    img, d = _img()
    d.rounded_rectangle([3, 3, 15, 13], 3, fill=(214, 46, 54, 255), outline=(164, 28, 38, 255))   # the red shell
    for (x, y) in ((6, 5), (10, 5), (13, 8), (8, 9), (11, 11), (5, 10)):          # cheese holes
        d.rectangle([x, y, x + 1, y + 1], fill=(252, 226, 126, 255))
        d.point((x + 1, y + 1), fill=(196, 132, 40, 255))
    d.rounded_rectangle([0, 7, 5, 12], 2, fill=(250, 212, 70, 255))            # the yellow head
    d.point((1, 9), fill=(18, 14, 20, 255))
    d.point((3, 9), fill=(18, 14, 20, 255))
    for x in (5, 9, 13):                                                     # little yellow feet
        d.rectangle([x, 13, x + 1, 15], fill=(250, 212, 70, 255))
    return img


def icon_sapje():
    img, d = _img()
    d.rectangle([6, 1, 9, 3], fill=(250, 212, 70, 255))                      # a little yellow shell-cork
    d.rectangle([7, 3, 8, 5], fill=(220, 236, 244, 255))                     # the neck
    d.ellipse([3, 5, 12, 15], fill=(220, 236, 244, 255), outline=(150, 176, 196, 255))   # the glass
    d.ellipse([4, 8, 11, 14], fill=(196, 40, 88, 255))                       # berry juice
    d.ellipse([5, 8, 8, 10], fill=(236, 96, 136, 255))
    d.point((4, 7), fill=(255, 255, 255, 255))
    d.point((10, 10), fill=(255, 190, 210, 255))
    return img


def icon_steentje():
    img, d = _img()
    d.ellipse([2, 5, 13, 13], fill=(206, 196, 204, 255), outline=(150, 138, 150, 255))   # a smooth pale pebble
    d.ellipse([4, 6, 9, 9], fill=(236, 230, 238, 255))
    d.point((5, 7), fill=(255, 255, 255, 255))
    d.point((10, 10), fill=(236, 170, 196, 255))                             # a tiny pink speck
    d.point((11, 4), fill=(255, 255, 255, 255))                              # a sparkle
    d.point((12, 3), fill=(255, 250, 220, 255))
    d.point((13, 4), fill=(255, 250, 220, 255))
    return img


def tex_steentjespad():
    """A mosaic of small polished pebbles (grey, cream, soft pink) in a bed of darker grit."""
    import random
    rng = random.Random(2030155)
    img = Image.new("RGBA", (16, 16), (122, 112, 110, 255))
    d = ImageDraw.Draw(img)
    for y in range(16):
        for x in range(16):
            v = rng.randint(-10, 10)
            img.putpixel((x, y), (122 + v, 112 + v, 110 + v, 255))
    kleuren = [(206, 196, 204), (230, 222, 206), (236, 190, 206), (190, 190, 198), (220, 210, 220)]
    for (x, y, w, hh) in ((0, 0, 4, 3), (5, 1, 4, 3), (10, 0, 5, 3), (1, 4, 3, 4), (5, 5, 5, 3), (11, 4, 4, 4), (0, 9, 4, 3),
                          (5, 9, 3, 3), (9, 9, 4, 3), (13, 9, 3, 3), (1, 13, 4, 3), (6, 13, 4, 3), (11, 13, 4, 3)):
        c = kleuren[rng.randrange(len(kleuren))]
        d.ellipse([x, y, x + w - 1, y + hh - 1], fill=c + (255,))
        d.point((x + 1, y), fill=tuple(min(255, v + 30) for v in c) + (255,))
    return img


def textures(h):
    h.save(icon_egeltje(), "item", "pluisegeltje_item.png")
    h.save(icon_konijntje(), "item", "guh_konijntje_item.png")
    h.save(icon_eekhoorntje(), "item", "pluiseekhoorntje_item.png")
    h.save(icon_shuckle(), "item", "shuckle_item.png")
    h.save(icon_sapje(), "item", "landdiertjes_bessensapje.png")
    h.save(icon_steentje(), "item", "landdiertjes_guhsteentje.png")
    h.save(tex_steentjespad(), "block", "landdiertjes_steentjespad.png")


# =====================================================================================================================
# blocks, items, loot, recipe, sounds, spawns
# =====================================================================================================================
def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex, **({"uv": uv} if uv else {})} for f in faces}}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    for iid in ("pluisegeltje_item", "guh_konijntje_item", "pluiseekhoorntje_item", "shuckle_item", "landdiertjes_bessensapje",
                "landdiertjes_guhsteentje"):
        h.item_model(iid)
    for e in DIEREN:
        w(f"{A}/models/item/{e}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    # the shuckle-plekje: invisible (the particle texture only)
    w(f"{A}/blockstates/shuckle_plekje.json", {"variants": {"": {"model": "guhs:block/shuckle_plekje"}}})
    w(f"{A}/models/block/shuckle_plekje.json", {"textures": {"particle": "minecraft:block/stone"}})
    # the knabbelvoorraadje: a mound of earth with a few leaves and a knabbel peeking out (bigger with 5+ knabbels)
    tex = {"aarde": "minecraft:block/rooted_dirt", "grof": "minecraft:block/coarse_dirt", "blad": "minecraft:block/azalea_leaves",
           "knabbel": "guhs:block/block_of_kaasknabbels", "particle": "minecraft:block/rooted_dirt"}
    klein = [_el([3, 0, 3], [13, 2, 13], "#aarde"), _el([5, 2, 5], [11, 3.5, 11], "#grof"),
             _el([9, 2.5, 6], [11.5, 5, 7.5], "#knabbel"), _el([4, 2, 9], [7, 2.6, 12], "#blad", ("up", "down", "north", "south", "east", "west"))]
    groot = [_el([2, 0, 2], [14, 2.5, 14], "#aarde"), _el([4, 2.5, 4], [12, 4.5, 12], "#grof"), _el([6, 4.5, 6], [10, 5.5, 10], "#aarde"),
             _el([9, 3.5, 5], [11.5, 6.5, 6.5], "#knabbel"), _el([5, 4, 9], [7.5, 6.5, 10.5], "#knabbel"),
             _el([3, 2.5, 10], [6.5, 3.1, 13], "#blad"), _el([10, 2.5, 10], [13, 3.1, 12.5], "#blad")]
    w(f"{A}/models/block/landdiertjes_knabbelvoorraadje.json", {"parent": "minecraft:block/block", "textures": tex, "elements": klein,
                                                                "render_type": "minecraft:cutout"})
    w(f"{A}/models/block/landdiertjes_knabbelvoorraadje_groot.json", {"parent": "minecraft:block/block", "textures": tex, "elements": groot,
                                                                      "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/landdiertjes_knabbelvoorraadje.json", {"variants": {
        f"knabbels={n}": {"model": "guhs:block/landdiertjes_knabbelvoorraadje" + ("_groot" if n >= 5 else "")} for n in range(1, 9)}})
    w(f"{D}/loot_table/blocks/landdiertjes_knabbelvoorraadje.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": [
            {"function": "minecraft:set_count", "count": n, "add": False, "conditions": [{
                "condition": "minecraft:block_state_property", "block": "guhs:landdiertjes_knabbelvoorraadje",
                "properties": {"knabbels": str(n)}}]} for n in range(1, 9)]}]}]})
    # the guhsteentjespad: a carpet of polished pebbles
    w(f"{A}/models/block/landdiertjes_steentjespad.json", {"parent": "minecraft:block/carpet",
                                                           "textures": {"wool": "guhs:block/landdiertjes_steentjespad"}})
    w(f"{A}/blockstates/landdiertjes_steentjespad.json", {"variants": {"": {"model": "guhs:block/landdiertjes_steentjespad"}}})
    w(f"{A}/models/item/landdiertjes_steentjespad.json", {"parent": "guhs:block/landdiertjes_steentjespad"})
    h.self_drop("landdiertjes_steentjespad")
    h.shaped("landdiertjes_steentjespad", ["SS", "SS"], {"S": "guhs:landdiertjes_guhsteentje"}, "guhs:landdiertjes_steentjespad", 3)
    # the critters drop nothing (they are lief, and nobody should want to hurt one)
    for e in DIEREN:
        w(f"{D}/loot_table/entities/{e}.json", {"type": "minecraft:entity", "pools": []})
    # spawns
    for name, (biomes, spawners) in SPAWNS.items():
        w(f"{D}/neoforge/biome_modifier/{name}.json", {
            "type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": t, "weight": wt, "minCount": lo, "maxCount": hi} for t, wt, lo, hi in spawners]})


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements: the Diertjes tab
# =====================================================================================================================
def _tam(e):
    return {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                             "predicate": {"type": f"guhs:{e}"}}]}}


ADV = [  # name, parent, icon, frame, title, description, criteria (None = granted by code)
    ("landdiertjes_geaaid", "root", "minecraft:sweet_berries", "task", "Zachtjes aaien",
     "Aai een egeltje, konijntje, eekhoorntje of Sjokkel met een lege hand", None),
    ("landdiertjes_egeltje", "landdiertjes_geaaid", "guhs:pluisegeltje_spawn_egg", "task", "Pluis!",
     "Zet een pluisegeltje in je Guhdex (in het Vadswoud)", None),
    ("landdiertjes_egeltje_tam", "landdiertjes_egeltje", "minecraft:sweet_berries", "task", "Knuffelstekeltjes",
     "Tem een pluisegeltje met zoete bessen (sluip, anders rolt het zich op!)", {"done": _tam("pluisegeltje")}),
    ("landdiertjes_rondje", "landdiertjes_egeltje_tam", "guhs:pluisegeltje_item", "task", "Wiee, een rondje!",
     "Laat je egeltje een rondje om je heen rollen (Rol eens!)", None),
    ("landdiertjes_konijntje", "landdiertjes_geaaid", "guhs:guh_konijntje_spawn_egg", "task", "Hangoortjes",
     "Zet een guh-konijntje in je Guhdex (in de Guhweides)", None),
    ("landdiertjes_konijntje_tam", "landdiertjes_konijntje", "minecraft:carrot", "task", "Een eigen konijntje",
     "Tem een guh-konijntje met een wortel of een kaasknabbel", {"done": _tam("guh_konijntje")}),
    ("landdiertjes_hophop", "landdiertjes_konijntje_tam", "minecraft:rabbit_foot", "task", "Hophop!",
     "Doe een binky met je konijntje en krijg konijnensprongetjes", None),
    ("landdiertjes_eekhoorntje", "landdiertjes_geaaid", "guhs:pluiseekhoorntje_spawn_egg", "task", "Wat een staart!",
     "Zet een pluiseekhoorntje in je Guhdex (in de guhbloesembomen of het Vadswoud)", None),
    ("landdiertjes_eekhoorntje_tam", "landdiertjes_eekhoorntje", "guhs:kaas_knabbels", "task", "Knabbelvriendje",
     "Tem een pluiseekhoorntje met kaasknabbels", {"done": _tam("pluiseekhoorntje")}),
    ("landdiertjes_schouder", "landdiertjes_eekhoorntje_tam", "guhs:pluiseekhoorntje_item", "task", "Op mijn schouder!",
     "Laat je eekhoorntje op je schouder zitten", None),
    ("landdiertjes_voorraadje", "landdiertjes_eekhoorntje", "minecraft:rooted_dirt", "task", "Een geheim voorraadje",
     "Graaf een knabbelvoorraadje van een eekhoorntje op", None),
    ("landdiertjes_shuckle", "landdiertjes_geaaid", "guhs:shuckle_spawn_egg", "task", "Geen guh... toch?",
     "Zet Sjokkel in je Guhdex (rond het kloon-eiland, of diep in de Gatenkaasgrotten)", None),
    ("landdiertjes_shuckle_tam", "landdiertjes_shuckle", "minecraft:glow_berries", "goal", "Heel, heel langzaam vriendjes",
     "Tem een Sjokkel (sluip dichterbij en geef hem bessen)", {"done": _tam("shuckle")}),
    ("landdiertjes_sapje", "landdiertjes_shuckle_tam", "guhs:landdiertjes_bessensapje", "task", "Slurp!",
     "Drink een bessensapje dat je Sjokkel gemaakt heeft", None),
    ("landdiertjes_polijsten", "landdiertjes_shuckle_tam", "guhs:landdiertjes_guhsteentje", "task", "Tink tink, glimmend!",
     "Laat een Sjokkel in een guhhuisje stenen polijsten", None),
    ("landdiertjes_huisje", "landdiertjes_geaaid", "guhs:guhhuisje_klein", "task", "Ook een huisje!",
     "Laat een egeltje, konijntje, eekhoorntje of Sjokkel in een guhhuisje wonen", None),
    ("landdiertjes_alle", "landdiertjes_huisje", "minecraft:cake", "challenge", "Een huis vol diertjes",
     "Tem een pluisegeltje, een guh-konijntje, een pluiseekhoorntje en een Sjokkel",
     {e: _tam(e) for e in DIEREN}),
]


def advancements(h):
    for name, parent, icon, frame, title, desc, criteria in ADV:
        verhaal.zichtbaar(h, "diertjes", name, parent, icon, frame, title, desc, criteria=criteria)


# =====================================================================================================================
# game test rooms
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((24, 8, 24))
    for x in range(24):
        for z in range(24):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("landdiertjes_test_tuin")
    t = h.Structure((10, 6, 10))
    for x in range(10):
        for z in range(10):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("landdiertjes_test_wei")


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    problems = modellen.check(h)
    for key, text in list(LANG.items()) + [(k, " ".join(v)) for k, v in DEX.items()]:
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    for e in DIEREN:
        for key in (f"entity.guhs.{e}", f"item.guhs.{e}_item", f"item.guhs.{e}_item.tooltip", f"item.guhs.{e}_spawn_egg",
                    f"gui.guhs.piep.menu.sub.{e}", f"gui.guhs.piep.menu.speciaal.{e}", f"gui.guhs.piep.menu.speciaal.{e}.tooltip",
                    f"gui.guhs.piep.opgepakt.{e}", f"gui.guhs.piep.neergezet.{e}", f"gui.guhs.landdiertjes.getemd.{e}",
                    f"gui.guhs.guhdex.info.{e}"):
            if key not in h.NL:
                problems.append(f"missing lang {key}")
        for p in (f"{h.A}/geckolib/models/entity/{e}.geo.json", f"{h.A}/geckolib/animations/entity/{e}.animation.json",
                  f"{h.A}/models/item/{e}_item.json", f"{h.D}/advancement/quest/seen_{e}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for kleur in modellen.KONIJN_KLEUREN:
        if not os.path.exists(os.path.join(h.TEX, "entity", f"guh_konijntje_{kleur}.png")):
            problems.append(f"missing guh_konijntje_{kleur}.png")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in h.NL:
            problems.append(f"missing subtitle {event}")
    if problems:
        raise SystemExit("landdiertjes self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    textures(h)
    blocks_and_items(h)
    sounds(h)
    texts(h)
    advancements(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: "Egeltjes, konijntjes, eekhoorntjes & Sjokkel" (chapter guhs_diertjes; nothing locked)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("landdiertjes_egeltje", "Pluisegeltjes", "In het &2Vadswoud&r scharrelen &dpluisegeltjes&r: egeltjes met zachte knuffelstekeltjes. "
      "Loop er niet op af, want dan rollen ze zich op! Sluip dichterbij (binnen 3 blokjes) zodat het in je Guhdex komt.",
      "guhs:pluisegeltje_spawn_egg", [adv("guhs:quest/seen_pluisegeltje")], rewards=(("minecraft:sweet_berries", 8),), shape="gear", xp=50)
    q("landdiertjes_egeltje_tam", "Knuffelstekeltjes", "Sluip naar een pluisegeltje en geef het &czoete bessen&r. Soms eet het uit je hand "
      "en is het van jou! Getemd: rechtsklik met een lege hand voor zijn menuutje, en probeer &dRol eens!&r.",
      "minecraft:sweet_berries", [adv("guhs:diertjes/landdiertjes_egeltje_tam")], rewards=(("guhs:kaas_knabbels", 12),), xp=100)
    q("landdiertjes_konijntje", "Guh-konijntjes", "In de &aGuhweides&r huppelen &dguh-konijntjes&r met lange hangoortjes: roze, wit, "
      "choco en grijs (en in de Sneeuwguhtoendra allemaal wit!). Kom er dichtbij voor je Guhdex.",
      "guhs:guh_konijntje_spawn_egg", [adv("guhs:quest/seen_guh_konijntje")], rewards=(("minecraft:carrot", 8),), xp=50)
    q("landdiertjes_konijntje_tam", "Hophop!", "Tem een guh-konijntje met een &6wortel&r (of een kaasknabbel). Getemd kan het een blije "
      "&dbinky&r doen: dan krijg jij ook even konijnensprongetjes!", "minecraft:carrot",
      [adv("guhs:diertjes/landdiertjes_konijntje_tam")], rewards=(("guhs:kaas_knabbels", 12),), xp=100)
    q("landdiertjes_eekhoorntje", "Pluiseekhoorntjes", "In de &dguhbloesembomen&r en het &2Vadswoud&r wonen &6pluiseekhoorntjes&r met een "
      "reusachtige krulstaart. Ze klimmen tegen boomstammen op! Kom er dichtbij voor je Guhdex.",
      "guhs:pluiseekhoorntje_spawn_egg", [adv("guhs:quest/seen_pluiseekhoorntje")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("landdiertjes_eekhoorntje_tam", "Knabbelvriendje", "Tem een pluiseekhoorntje met &6kaasknabbels&r. Wat het in zijn wangetjes had, "
      "krijg jij cadeau!", "guhs:kaas_knabbels", [adv("guhs:diertjes/landdiertjes_eekhoorntje_tam")],
      rewards=(("guhs:kaas_knabbels", 12),), xp=100)
    q("landdiertjes_schouder", "Op mijn schouder!", "Kies &dOp mijn schouder!&r in het menuutje van je eekhoorntje (of pak het op en "
      "rechtsklik ermee in de lucht): het zit op je schouder! Sluip + rechtsklik op een blok met een lege hand, en het springt eraf.",
      "guhs:pluiseekhoorntje_item", [adv("guhs:diertjes/landdiertjes_schouder")], rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("landdiertjes_voorraadje", "Een geheim voorraadje", "Eekhoorntjes verstoppen kaasknabbels in &6hoopjes aarde&r (met een knabbel die "
      "eruit piept). Vind er een en rechtsklik om hem op te graven. Een tam eekhoorntje graaft er af en toe zelf een voor je op!",
      "minecraft:rooted_dirt", [adv("guhs:diertjes/landdiertjes_voorraadje")], rewards=(("guhs:gefrituurde_kaasknabbels", 2),), xp=100)
    q("landdiertjes_shuckle", "Sjokkel?!", "Geen guh! Een &crode schelp met kaasgaatjes&r en een &egeel kopje&r. Sjokkel woont rond de "
      "rotsen van het &dkloon-eiland&r in de Guhzee, en heel af en toe diep in de &6Gatenkaasgrotten&r. Hij is heel verlegen: sluip!",
      "guhs:shuckle_spawn_egg", [adv("guhs:quest/seen_shuckle")], rewards=(("minecraft:sweet_berries", 8),), shape="circle", xp=100)
    q("landdiertjes_shuckle_tam", "Heel, heel langzaam vriendjes", "Sluip naar Sjokkel (anders kruipt hij in zijn schelp) en geef hem "
      "&czoete bessen&r. Het duurt even... hij is nu eenmaal heel traag. Getemd volgt hij je, op zijn eigen tempo.",
      "minecraft:glow_berries", [adv("guhs:diertjes/landdiertjes_shuckle_tam")], rewards=(("guhs:kaas_knabbels", 16),), xp=150)
    q("landdiertjes_sapje", "Sjokkel-bessensapje", "Geef je tamme Sjokkel zoete bessen: hij bewaart ze in zijn schelp. Met 3 bessen maakt "
      "hij er vanzelf een &dbessensapje&r van (of kies &dBessensapje!&r in zijn menuutje). Proost, njeg!",
      "guhs:landdiertjes_bessensapje", [item("guhs:landdiertjes_bessensapje")], rewards=(("minecraft:sweet_berries", 12),), xp=100)
    q("landdiertjes_polijsten", "Stenen polijsten", "Laat je Sjokkel in een &dguhhuisje&r wonen en leg &7keien&r in de kist ernaast. Zijn "
      "klusje: heel geduldig stenen poetsen tot &fgladde steen&r en glimmende &dguhsteentjes&r (vier daarvan maken een guhsteentjespad).",
      "guhs:landdiertjes_guhsteentje", [adv("guhs:diertjes/landdiertjes_polijsten")], rewards=(("minecraft:cobblestone", 32),), xp=150)
    q("landdiertjes_huisje", "Ook een huisje!", "Egeltjes, konijntjes, eekhoorntjes en Sjokkel kunnen ook in een &dguhhuisje&r wonen: "
      "gebruik je opgepakte diertje op het huisje, of kies het in het huisjesscherm.", "guhs:guhhuisje_klein",
      [adv("guhs:diertjes/landdiertjes_huisje")], rewards=(("guhs:kaas_knabbels", 12),), xp=100)
    q("landdiertjes_alle", "Een huis vol diertjes", "Tem ze allemaal: een pluisegeltje, een guh-konijntje, een pluiseekhoorntje en een "
      "Sjokkel. VAHOEG!", "minecraft:cake", [adv("guhs:diertjes/landdiertjes_alle")],
      rewards=(("guhs:gouden_kaasknabbel", 1), ("guhs:kaas_knabbels", 24)), shape="gear", xp=300)
