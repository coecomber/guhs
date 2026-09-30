"""
De Guh-Sterrenwacht (2.8.0 "Knuffeldal", slice "buiten"; see guhs_work28/KNUFFEL_CONTRACT.md): a big observatory high on
the Guhpieken and the Vadskliffen with a guh-head dome and a huge telescope, where Professor Sterretje lives.

  - the loose structure guh_sterrenwacht (sterrenwacht_bouw.py: 48 x 64 x 48 on deep legs, geometry self-check)
  - blocks guh_telescoop (connect the stars into a guh constellation) and sterrenlantaarn (the Knusfeest's
    feest-item of the task "sterrenlantaarns"), the item wensster (make a wish, or pay the Professor)
  - Professor Sterretje (STERRENKIJKERGUH): his own model (a tall starry hat, a spyglass on a cord) and texture
  - the clothes sterrenkijkersmuts (outfit_tall_hat) and sterrencape (outfit_cape)
  - the Knus section "sterrenwacht" (sterrenatlas: 12 + 3 constellations), advancements, FTB row y = 92, lang
"""
import math
import random

import numpy as np
from PIL import Image

from features import sterrenwacht_bouw as bouw
from features import sterrenwacht_hulp as hulp

NAME = bouw.NAME
CLOTHES = ["sterrenkijkersmuts", "sterrencape"]
BONES = {}
FTB_Y = 92
NACHT = (38, 34, 92)
NACHT_DONKER = (22, 18, 58)
GOUD = (250, 206, 80)

STERRENBEELDEN = [
    ("grote_knabbel", "De Grote Knabbel", "Een reuzenkaasknabbel met een gaatje. De bekendste guh-sterren van de hele hemel: wie hem vindt, "
                                          "wordt altijd een beetje vahoeger. Njeg!"),
    ("kleine_vads", "De Kleine Vads", "Een klein guhkopje met twee oortjes. Babyguhs zoeken hem als eerste, want hij lijkt op hen."),
    ("guhoor", "Het Guhoor", "Eén groot, rond guhoor. Opa Guh zegt dat het luistert of er Mika's aan komen sluipen."),
    ("vluchtende_mika", "De Vluchtende Mika", "Een Mika die er met een zak gestolen kaasknabbels vandoor gaat. Daarom zijn de guhs niet vahoeg "
                                              "genoeg! Hij rent al duizend jaar en komt nooit ergens."),
    ("kaasschaaf", "De Kaasschaaf", "Een handvat en een schaafje: de sterren van het kaasschaven. Handig voor extra dunne knabbels."),
    ("slapende_guh", "De Slapende Guh", "Een guh die ligt te slapen, met een Z erboven. Zzz... vads."),
    ("frituurpannetje", "Het Frituurpannetje", "Een rond pannetje met een lange steel. Daar worden de sterren in gefrituurd, zeggen ze."),
    ("vahoege_buik", "De Vahoege Buik", "Een grote, ronde, VAHOEGE buik. Zo moet een guh eruitzien na een goed feest!"),
    ("guhstaart", "De Guhstaart", "Een krulstaartje van sterren. Kwispelt een beetje als je lang genoeg kijkt."),
    ("knuffelhart", "Het Knuffelhart", "Een hart voor iedereen die je knuffelt. Het straalt het felst in het Knuffeldal."),
    ("luchtballon", "De Luchtballon", "Een luchtballon met een mandje: het lievelingssterrenbeeld van Kapitein Wolkje."),
    ("kampvuur", "Het Kampvuur", "Een vlammetje op twee blokjes hout: het lievelingssterrenbeeld van Opa Guh."),
    ("gouden_guh", "De Gouden Guh", "ZELDZAAM (alleen tijdens een sterrenregen): een heel guhgezicht van gouden sterren. Wie hem ziet, "
                                    "mag drie keer wensen, zegt Professor Sterretje. Of één keer, heel hard."),
    ("vallende_knabbel", "De Vallende Knabbel", "ZELDZAAM (alleen tijdens een sterrenregen): een kaasknabbel met een lange staart, "
                                                "op weg naar een hongerige guh."),
    ("guhkroon", "De Guhkroon", "ZELDZAAM (alleen tijdens een sterrenregen): de kroon van de Koningguh, met vijf punten."),
]


# =====================================================================================================================
# guh clothes (make_guh_variants.py / make_clothes_icons.py): the tall hat and the cape bones already exist
# =====================================================================================================================
def clothes(rng, v):
    def muts():
        a = v.stars(NACHT, (255, 236, 130), rng, n=18)
        a[26:32, :] = GOUD                              # a golden band round the rim
        return np.clip(a, 0, 255)

    def cape():
        a = v.stars(NACHT_DONKER, (255, 240, 170), rng, n=22)
        a[:3, :] = GOUD
        return np.clip(a, 0, 255)
    return {"sterrenkijkersmuts": {"tall_hat": muts}, "sterrencape": {"cape": cape}}


def icons(ic):
    return {"sterrenkijkersmuts": ic.shaped("tall_hat", (20, 16, 50), (38, 34, 92), (255, 230, 120)),
            "sterrencape": ic.shaped("cape", (14, 12, 40), (38, 34, 92), (250, 206, 80))}


# =====================================================================================================================
# textures
# =====================================================================================================================
def ster(img, cx, cy, r, colour, glow=None):
    """A little five-pointed star (radius r) on an image."""
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            a = math.atan2(dy, dx)
            d = math.hypot(dx, dy)
            lim = r * (0.55 + 0.45 * abs(math.cos(2.5 * (a + math.pi / 2))))
            if d <= lim:
                px[x, y] = colour + (255,)
            elif glow and d <= lim + 1.0 and px[x, y][3] == 0:
                px[x, y] = glow + (160,)


def textures(h):
    rng = random.Random(28602)
    save = h.save
    buis = hulp.noisy((120, 190, 236), 8, rng)
    px = buis.load()
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = (255, 250, 210, 255)
    for x in range(16):
        px[x, 12] = GOUD + (255,)
        px[x, 13] = (220, 170, 60, 255)
    save(buis, "block", "guh_telescoop_buis.png")
    save(h.ramp(h.vanilla("block/gold_block"), (170, 110, 40), (255, 226, 120)), "block", "guh_telescoop_koper.png")
    save(h.ramp(h.vanilla("block/spruce_planks"), (90, 60, 40), (190, 140, 100)), "block", "guh_telescoop_hout.png")
    oor = hulp.noisy((246, 150, 196), 6, rng)
    for x in range(4, 12):
        for y in range(4, 12):
            if math.dist((x + 0.5, y + 0.5), (8, 8)) < 3.6:
                oor.putpixel((x, y), (214, 90, 170, 255))
    save(oor, "block", "guh_telescoop_oor.png")
    lens = hulp.noisy((150, 214, 250), 6, rng)
    lens.putpixel((5, 5), (255, 255, 255, 255))
    lens.putpixel((6, 5), (255, 255, 255, 255))
    lens.putpixel((5, 6), (255, 255, 255, 255))
    save(lens, "block", "guh_telescoop_lens.png")
    glas = hulp.noisy(NACHT, 6, rng)
    ster(glas, 8, 8.3, 5.2, (255, 232, 110), glow=(255, 200, 120))
    for (x, y) in ((2, 3), (13, 2), (3, 13), (13, 12)):
        glas.putpixel((x, y), (255, 250, 220, 255))
    save(glas, "block", "sterrenlantaarn_glas.png")
    save(h.ramp(h.vanilla("block/gold_block"), (190, 130, 40), (255, 236, 140)), "block", "sterrenlantaarn_rand.png")
    # the wensster: a golden star with a little guh face and a pink sparkle
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ster(icon, 8, 8.4, 7.4, (255, 214, 80), glow=(255, 160, 210))
    ipx = icon.load()
    for (x, y) in ((6, 7), (10, 7)):
        ipx[x, y] = (60, 40, 70, 255)
        ipx[x, y + 1] = (60, 40, 70, 255)
    for (x, y) in ((5, 10), (11, 10)):
        ipx[x, y] = (255, 140, 180, 255)
    ipx[8, 10] = (200, 90, 120, 255)
    for (x, y) in ((7, 4), (8, 3)):
        ipx[x, y] = (255, 250, 220, 255)
    save(icon, "item", "wensster.png")
    # the sparkle particle: a four-pointed twinkle, three sizes
    frames = []
    for i in range(3):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        p = img.load()
        r = 1 + i
        for k in range(-r, r + 1):
            for (x, y) in ((4 + k, 4), (4, 4 + k)):
                if 0 <= x < 8 and 0 <= y < 8:
                    p[x, y] = (255, 246, 190, 255) if abs(k) < r else (255, 190, 230, 200)
        p[4, 4] = (255, 255, 255, 255)
        frames.append(img)
    hulp.particle_frames(h, "wensster", frames)


# =====================================================================================================================
# blocks and items
# =====================================================================================================================
def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    el = hulp.el
    rot = {"origin": [8, 12, 8], "axis": "x", "angle": 22.5}
    tele = {"particle": "guhs:block/guh_telescoop_buis", "buis": "guhs:block/guh_telescoop_buis", "koper": "guhs:block/guh_telescoop_koper",
            "hout": "guhs:block/guh_telescoop_hout", "oor": "guhs:block/guh_telescoop_oor", "lens": "guhs:block/guh_telescoop_lens"}
    w(f"{A}/models/block/guh_telescoop.json", {"parent": "minecraft:block/block", "textures": tele, "elements": [
        el([3, 0, 3], [4.5, 9, 4.5], "#hout"), el([11.5, 0, 3], [13, 9, 4.5], "#hout"), el([7.25, 0, 12], [8.75, 9, 13.5], "#hout"),
        el([5, 8, 5], [11, 10, 11], "#koper"),
        el([6, 10, 2], [10, 14, 14], "#buis", rot=rot),
        el([5.5, 9.5, 1], [10.5, 14.5, 2.5], "#koper", rot=rot),
        el([6.5, 10.5, 0.8], [9.5, 13.5, 1], "#lens", faces=("north",), rot=rot),
        el([5.8, 14, 3], [7.4, 16.2, 4.2], "#oor", rot=rot), el([8.6, 14, 3], [10.2, 16.2, 4.2], "#oor", rot=rot),
        el([7, 10.8, 14], [9, 12.8, 16], "#koper", rot=rot)]})
    w(f"{A}/blockstates/guh_telescoop.json", {"variants": hulp.facing_variants("guhs:block/guh_telescoop")})
    w(f"{A}/models/item/guh_telescoop.json", {"parent": "guhs:block/guh_telescoop"})
    lt = {"particle": "guhs:block/sterrenlantaarn_glas", "glas": "guhs:block/sterrenlantaarn_glas", "rand": "guhs:block/sterrenlantaarn_rand",
          "oor": "guhs:block/guh_telescoop_oor"}
    w(f"{A}/models/block/sterrenlantaarn.json", {"parent": "minecraft:block/block", "textures": lt, "elements": [
        el([4, 0, 4], [12, 1, 12], "#rand"), el([5, 1, 5], [11, 8, 11], "#glas"), el([4, 8, 4], [12, 9, 12], "#rand"),
        el([6, 9, 6], [10, 10, 10], "#rand"), el([7.5, 10, 7.5], [8.5, 11.5, 8.5], "#rand"),
        el([4.5, 9, 7], [6.5, 11.5, 9], "#oor"), el([9.5, 9, 7], [11.5, 11.5, 9], "#oor")]})
    w(f"{A}/blockstates/sterrenlantaarn.json", {"variants": {"": {"model": "guhs:block/sterrenlantaarn"}}})
    w(f"{A}/models/item/sterrenlantaarn.json", {"parent": "guhs:block/sterrenlantaarn"})
    h.item_model("wensster")
    for b in ("guh_telescoop", "sterrenlantaarn"):
        h.self_drop(b)
    h.shapeless("sterrenlantaarn", ["minecraft:lantern", "guhs:wensster"], "guhs:sterrenlantaarn", 2)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guh_telescoop", "guhs:sterrenlantaarn"])
    h.add_tag("guhs/tags/item/knus/sterrenlantaarns", ["guhs:sterrenlantaarn"])
    # the chest in the observatory
    w(f"{D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:wensster", "functions": h.count_fn(1, 3)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:sterrenlantaarn", "functions": h.count_fn(1, 3)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": h.count_fn(6, 14)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 3, "functions": h.count_fn(1, 4)},
            {"type": "minecraft:item", "name": "minecraft:spyglass", "weight": 1},
            {"type": "minecraft:item", "name": "minecraft:glowstone_dust", "weight": 3, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 1}]}]})


# =====================================================================================================================
# Professor Sterretje: the sitting guh in night blue, a tall starry hat, a little spyglass on a cord
# =====================================================================================================================
def npc(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_sterrenkijkerguh")
    sw = hulp.swatches(geo, ["hoed", "hoedrand", "ster", "kijker", "lens", "snoer"])
    c = hulp.cube
    geo["bones"].append({"name": "sterretje_hoed", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.0, 25.8, -7.0], [12, 0.8, 11], sw["hoedrand"]),
        c([-4.0, 26.6, -5.0], [8, 3.4, 7], sw["hoed"]),
        c([-3.0, 30.0, -4.2], [6, 3.0, 5.4], sw["hoed"]),
        c([-2.0, 33.0, -3.4], [4, 2.4, 3.8], sw["hoed"])]})
    geo["bones"].append({"name": "sterretje_hoedpunt", "parent": "sterretje_hoed", "pivot": [0, 35.4, -1.5], "cubes": [
        c([-1.0, 35.4, -2.5], [2, 2.2, 2], sw["hoed"]),
        c([-1.5, 37.6, -2.0], [3, 3, 1], sw["ster"], inflate=0.1)]})
    geo["bones"].append({"name": "sterretje_kijker", "parent": "body", "pivot": [0, 12, -4.2], "cubes": [
        c([-3.2, 11.4, -4.8], [6.4, 0.5, 0.5], sw["snoer"]),
        c([-0.9, 7.0, -5.8], [1.8, 4.4, 1.8], sw["kijker"]),
        c([-1.2, 6.2, -6.1], [2.4, 1.0, 2.4], sw["kijker"]),
        c([-0.7, 6.0, -5.6], [1.4, 0.3, 1.4], sw["lens"])]})
    hulp.save_geo(h, "guh_npc_sterrenkijkerguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.68, sat=1.9, val=0.86)
    rng = np.random.default_rng(28603)

    def sterretjes(block):
        for _ in range(14):
            x, y = rng.integers(1, 30, 2)
            block[y:y + 2, x:x + 2, :3] = (255, 236, 130)
    hulp.paint_swatch(a, sw["hoed"], NACHT, rng, 6, sterretjes)
    hulp.paint_swatch(a, sw["hoedrand"], NACHT_DONKER, rng, 6, sterretjes)
    hulp.paint_swatch(a, sw["ster"], (255, 220, 90), rng, 8)
    hulp.paint_swatch(a, sw["kijker"], (206, 150, 60), rng, 10)
    hulp.paint_swatch(a, sw["lens"], (170, 225, 255), rng, 6)
    hulp.paint_swatch(a, sw["snoer"], (120, 60, 100), rng, 6)
    # a few tiny stars in his fur
    fur = np.argwhere((a[..., 3] > 0) & (a[..., 2].astype(int) > a[..., 0].astype(int) + 30))
    srng = random.Random(28604)
    for _ in range(120):
        y, x = fur[srng.randrange(len(fur))]
        a[y:y + 2, x:x + 2, :3] = (255, 240, 170)
    h.save(Image.fromarray(a), "entity", "npc_sterrenkijkerguh.png")


# =====================================================================================================================
# sounds, advancements, lang
# =====================================================================================================================
SOUNDS = {
    "sterrenwacht.ster_klik": [{"name": "minecraft:block.amethyst_block.chime", "type": "event", "volume": 0.9}],
    "sterrenwacht.sterrenbeeld": [{"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.2},
                                  {"name": "minecraft:block.amethyst_block.resonate", "type": "event"}],
}
QUEST_ADVANCEMENTS = ["seen_sterrenkijkerguh", "sterrenwacht_sterretje", "sterrenwacht_sterrenbeeld", "sterrenwacht_zes",
                      "sterrenwacht_zeldzaam", "sterrenwacht_atlas_vol", "sterrenwacht_wens"]


def advancements(h):
    hulp.quest_advancements(h, QUEST_ADVANCEMENTS)
    adv = hulp.display_advancement
    adv(h, "sterrenwacht_gevonden", "root", "guhs:guh_telescoop", "task", hulp.in_structure(NAME),
        "Hoog in de sterren", "Vind de Guh-Sterrenwacht, hoog op de Guhpieken of de Vadskliffen")
    adv(h, "sterrenwacht_sterrenbeeld", "sterrenwacht_gevonden", "guhs:wensster", "goal", hulp.IMPOSSIBLE,
        "Verbind de sterretjes", "Kijk 's nachts door een guh-telescoop en verbind je eerste guh-sterrenbeeld")
    adv(h, "sterrenwacht_zeldzaam", "sterrenwacht_sterrenbeeld", "minecraft:nether_star", "goal",
        hulp.IMPOSSIBLE, "Een vallende knabbel!", "Vind een zeldzaam sterrenbeeld tijdens een sterrenregen")
    adv(h, "sterrenwacht_atlas_vol", "sterrenwacht_sterrenbeeld", "guhs:sterrenlantaarn", "challenge", hulp.IMPOSSIBLE,
        "De hele sterrenatlas", "Vind alle 15 guh-sterrenbeelden, ook de drie van de sterrenregen. VAHOEG, sterrenkijker!")


LANG = {
    "block.guhs.guh_telescoop": "Guh-telescoop",
    "block.guhs.guh_telescoop.lore": "'s Nachts: rechtsklik en verbind de sterren tot een guh-sterrenbeeld",
    "block.guhs.sterrenlantaarn": "Sterrenlantaarn",
    "block.guhs.sterrenlantaarn.lore": "Een lantaarntje met een ster erin. Voor het Grote Knusfeest!",
    "item.guhs.wensster": "Wensster",
    "item.guhs.wensster.lore": "Rechtsklik en doe een wens! (Of betaal Professor Sterretje ermee)",
    "item.guhs.sterrenkijkersmuts": "Sterrenkijkersmuts", "item.guhs.sterrencape": "Sterrencape",
    "entity.guhs.guh_npc.sterrenkijkerguh": "Professor Sterretje",
    "structure.guhs.guh_sterrenwacht": "Guh-Sterrenwacht",
    "structure.guhs.guh_sterrenwacht.tooltip": "De sterrenwacht van Professor Sterretje, hoog op de bergen: verbind 's nachts de sterren tot guh-sterrenbeelden",
    "gui.guhs.guhdex.rarity.sterrenkijkerguh": "Zeldzaamheid: Zeldzaam (Guh-Sterrenwacht, Guhpieken en Vadskliffen)",
    "gui.guhs.guhdex.info.sterrenkijkerguh": "Kijkt elke nacht door zijn reuzentelescoop naar de guh-sterrenbeelden. De Grote Knabbel is zijn "
                                             "lievelings. Verkoopt sterrenlantaarns en zijn sterrenkijkerspakje voor wenssterren.",
    "subtitles.guhs.sterrenwacht.ster_klik": "Sterretje tingelt",
    "subtitles.guhs.sterrenwacht.sterrenbeeld": "Sterrenbeeld gevonden!",
    "gui.guhs.sterrenwacht.beschermd": "Njeg! De Guh-Sterrenwacht is van Professor Sterretje. Hier niks slopen of bouwen!",
    "gui.guhs.sterrenwacht.bezig": "Njeg, je bent nog met iets anders bezig. Eerst dat afmaken!",
    "gui.guhs.sterrenwacht.overdag": "Overdag zie je alleen de zon, njeg. Kom terug als het donker is!",
    "gui.guhs.sterrenwacht.te_laat": "Njeg, de sterren zijn alweer verder gedraaid: je keek te lang. Kijk nog eens door de telescoop!",
    "gui.guhs.sterrenwacht.klopt_niet": "Hmm, die lijntjes kloppen niet met de hemel van Professor Sterretje. Kijk nog eens door de telescoop, njeg!",
    "gui.guhs.sterrenwacht.moe": "Je oogjes zijn moe van al dat sterrenkijken (%s per nacht). Morgennacht weer! Zzz...",
    "gui.guhs.sterrenwacht.telescoop": "Guh-telescoop",
    "gui.guhs.sterrenwacht.zoek": "Zoek aan de hemel:",
    "gui.guhs.sterrenwacht.nieuw": "Nieuw voor je atlas!",
    "gui.guhs.sterrenwacht.lijnen": "Lijntjes: %s/%s",
    "gui.guhs.sterrenwacht.fouten": "Njeg-lijntjes: %s",
    "gui.guhs.sterrenwacht.vannacht": "Vannacht: %s/%s",
    "gui.guhs.sterrenwacht.vahoeg": "VAHOEG! Gevonden!",
    "gui.guhs.sterrenwacht.hint": "Klik een ster en dan de volgende om een lijntje te trekken",
    "gui.guhs.sterrenwacht.opnieuw": "Opnieuw",
    "gui.guhs.sterrenwacht.stoppen": "Stoppen",
    "gui.guhs.sterrenwacht.wens.knabbels": "Je wens komt uit: een handvol kaasknabbels! Mjam, VAHOEG!",
    "gui.guhs.sterrenwacht.wens.gefrituurd": "Je wens komt uit: gefrituurde kaasknabbels! Extra knapperig, njeg!",
    "gui.guhs.sterrenwacht.wens.ballon": "Je wens komt uit: guh-ballonnen! Feest!",
    "gui.guhs.sterrenwacht.wens.lantaarn": "Je wens komt uit: sterrenlantaarns, zomaar uit de lucht!",
    "gui.guhs.sterrenwacht.wens.geluk": "Je wens komt uit: je voelt je licht, gelukkig en supervahoeg!",
    "gui.guhs.sterrenwacht.wens.vahoeg": "WOW, een superwens: VAHOEGE vadsstaven! Dat gebeurt maar heel af en toe...",
    "quest.guhs.sterrenwacht.hallo": "O! Bezoek! Welkom in de Guh-Sterrenwacht, ik ben Professor Sterretje. Ik kijk elke nacht naar de guh-sterrenbeelden: "
                                     "De Grote Knabbel, De Kleine Vads, het Guhoor... Kom vannacht terug en kijk door een telescoop. "
                                     "Verbind de sterren, dan krijg je wenssterren. VAHOEG!",
    "quest.guhs.sterrenwacht.knusfeest": "Sterrenlantaarns voor het Knusfeest? Njeg, natuurlijk! Verbind vannacht een sterrenbeeld in een telescoop, "
                                         "dan geef ik je er %s. Dan wordt het feest net zo mooi als de hemel!",
    "quest.guhs.sterrenwacht.regen": "EEN STERRENREGEN! Snel, naar de telescoop! Nu kun je de zeldzame sterrenbeelden zien: De Gouden Guh, "
                                     "De Vallende Knabbel en De Guhkroon!",
    "quest.guhs.sterrenwacht.nacht": "Het is donker, de sterren zijn er! Kijk door een telescoop en verbind ze. Ik hou je atlas bij, njeg.",
    "quest.guhs.sterrenwacht.nacht_moe": "Je hebt vannacht al genoeg sterren gekeken. Ga maar lekker slapen, morgennacht staan ze er weer.",
    "quest.guhs.sterrenwacht.tip0": "Wist je dat er twaalf guh-sterrenbeelden zijn? En tijdens een sterrenregen nog drie heel zeldzame!",
    "quest.guhs.sterrenwacht.tip1": "Een wensster kun je in je hand houden en dan een wens doen. Rechtsklik maar. Wat je wenst? Meestal kaasknabbels, njeg.",
    "quest.guhs.sterrenwacht.tip2": "De Vluchtende Mika: een Mika met een zak gestolen kaasknabbels. Daarom zijn de guhs nooit vahoeg genoeg! Hij rent en rent...",
    "quest.guhs.sterrenwacht.tip3": "Mijn koepel is een guhkop, zie je de oren? Zo hoor ik de sterretjes tingelen.",
    "quest.guhs.sterrenwacht.tip4": "Voor wenssterren heb ik mijn sterrenkijkersmuts en sterrencape in guhmaat. Staat jouw guh vast heel sterrig!",
    "quest.guhs.sterrenwacht.tip5": "Een eigen guh-telescoop? Die heb ik voor acht wenssterren. Dan kun je thuis sterrenkijken!",
    "quest.guhs.sterrenwacht.tip6": "Overdag slaap ik een beetje. Sterrenkijkers zijn 's nachts vahoeg en overdag vads. Njeg.",
    "quest.guhs.sterrenwacht.gevonden_nieuw": "VAHOEG! Je hebt %s gevonden! Die schrijf ik in je sterrenatlas. Hier, %s wenssterren!",
    "quest.guhs.sterrenwacht.gevonden_weer": "Ah, %s! Die kende je al, maar het blijft mooi. Hier, %s wenssterren.",
    "quest.guhs.sterrenwacht.lantaarns": "En voor het Knusfeest: %s sterrenlantaarns! Breng ze maar naar de burgemeester.",
    # the Knus tab
    "gui.guhs.knus.verzameling.sterrenatlas": "Sterrenatlas",
    "gui.guhs.knus.mijlpaal.sterrenwacht_gevonden": "Praat met Professor Sterretje",
    "gui.guhs.knus.mijlpaal.sterrenwacht_eerste": "Je eerste sterrenbeeld",
    "gui.guhs.knus.mijlpaal.sterrenwacht_zes": "6 sterrenbeelden in je atlas",
    "gui.guhs.knus.mijlpaal.sterrenwacht_twaalf": "12 sterrenbeelden in je atlas",
    "gui.guhs.knus.mijlpaal.sterrenwacht_zeldzaam": "Een zeldzaam sterrenbeeld (sterrenregen)",
    "gui.guhs.knus.mijlpaal.sterrenwacht_wensen": "10 wensen gedaan",
    "gui.guhs.knus.mijlpaal.sterrenwacht_atlas_vol": "De hele sterrenatlas (15)",
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for sid, naam, info in STERRENBEELDEN:
        h.lang(f"gui.guhs.knus.sterrenatlas.{sid}", naam, naam)
        h.lang(f"gui.guhs.knus.sterrenatlas.{sid}.info", info, info)


def selfcheck(h):
    import os
    missing = []
    for b in ("guh_telescoop", "sterrenlantaarn"):
        if not os.path.exists(f"{h.A}/blockstates/{b}.json") or f"block.guhs.{b}" not in h.NL:
            missing.append(b)
    for i in ("wensster",):
        if not os.path.exists(f"{h.A}/textures/item/{i}.png") or f"item.guhs.{i}" not in h.NL:
            missing.append(i)
    if len(STERRENBEELDEN) != 15:
        missing.append("15 constellations")
    if missing:
        raise SystemExit(f"sterrenwacht assets missing: {missing}")


# the sterrenwacht stands HIGH on the mountains: its start (the terrace floor) never goes below this height, so it is not
# built down in a valley between the peaks (guhs:sterrenwacht_hoog, HogeJigsawStructure.java, is the inner jigsaw of the
# shared guhs:flat_jigsaw: the flatness check and BouwRuimte stay the same)
# (2.8 merge: was 90 - with the flatness check that left ~2 sterrenwachten in 16 x 16 km, almost unfindable; at 70 it
# still stands on the mountain biomes above the lowlands, about as often as the other 2.8 buildings)
MIN_START_Y = 70


def hoog(h):
    import json
    path = f"{h.D}/worldgen/structure/{NAME}.json"
    s = json.load(open(path, encoding="utf-8"))
    s["jigsaw"]["type"] = "guhs:sterrenwacht_hoog"
    s["jigsaw"]["min_start_y"] = MIN_START_Y
    h.w(path, s)


def build(h):
    textures(h)
    blocks_and_items(h)
    npc(h)
    hulp.sounds(h, SOUNDS)
    advancements(h)
    texts(h)
    # the observatory: on the Guhpieken and the Vadskliffen, anchored in the middle of its star-map floor
    s, info = bouw.build(h)
    n = bouw.check(s, info)
    h.TEMPLATE_SIZES[NAME] = 24
    h.FLATNESS[NAME] = 24
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, ["guh_peaks", "vads_cliffs"], spacing=18, separation=6, salt=20280601, start_y=-bouw.G, reach=60, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none})
    hoog(h)
    s.save(NAME)
    selfcheck(h)
    print(f"sterrenwacht: geometry check ok ({n} walkable spots, {len(s.blocks)} blocks, {info['gezichten']} guh faces)")


# =====================================================================================================================
# FTB quests (row y = 92), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("sterrenwacht_gevonden", "De Guh-Sterrenwacht", "Hoog op de &dGuhpieken&r en de &dVadskliffen&r staat een sterrenwacht met een koepel die "
      "eruitziet als een guhkop, en een reuzentelescoop. Het superkompas (categorie Knus) wijst de weg.",
      "guhs:guh_telescoop", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, shape="circle", xp=150)
    q("sterrenwacht_sterretje", "Professor Sterretje", "Praat met &dProfessor Sterretje&r in de sterrenwacht. Hij weet alles van de guh-sterrenbeelden.",
      "guhs:wensster", [fq.adv("sterrenwacht_sterretje")], rewards=(("guhs:kaas_knabbels", 8),), x=-6.5, y=y, xp=100)
    q("sterrenwacht_eerste", "Verbind de sterretjes", "Kijk 's nachts door een &dguh-telescoop&r. Ergens tussen de sterren zit een guh-sterrenbeeld: "
      "klik een ster en dan de volgende om de lijntjes te trekken. Gevonden? Wenssterren!",
      "guhs:guh_telescoop", [fq.adv("sterrenwacht_sterrenbeeld")], rewards=(("guhs:wensster", 1),), x=-5, y=y, xp=150)
    q("sterrenwacht_zes", "Een halve atlas", "Zet zes guh-sterrenbeelden in je sterrenatlas (Guhdex, tab Knus). Je vindt er maximaal drie per nacht.",
      "guhs:sterrenlantaarn", [fq.adv("sterrenwacht_zes")], rewards=(("guhs:sterrenlantaarn", 2),), x=-3.5, y=y, xp=200)
    q("sterrenwacht_zeldzaam", "Sterrenregen!", "Tijdens een &bsterrenregen&r zie je drie zeldzame sterrenbeelden: De Gouden Guh, De Vallende Knabbel "
      "en De Guhkroon. Vind er een!", "minecraft:nether_star", [fq.adv("sterrenwacht_zeldzaam")], rewards=(("guhs:wensster", 2),), x=-2, y=y, xp=250)
    q("sterrenwacht_atlas", "De hele sterrenatlas", "Vind alle vijftien guh-sterrenbeelden. VAHOEG, echte sterrenkijker!",
      "guhs:guh_telescoop", [fq.adv("sterrenwacht_atlas_vol")], rewards=(("guhs:vahoege_vads_ingot", 2),), x=-0.5, y=y, shape="gear", xp=500)
    q("sterrenwacht_wens", "Doe een wens", "Houd een &dwensster&r vast en rechtsklik: je wens komt uit! (Meestal kaasknabbels, njeg.)",
      "guhs:wensster", [fq.adv("sterrenwacht_wens")], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=y, xp=100)
    q("sterrenwacht_pakje", "Sterrenkijkerspakje", "Koop de sterrenkijkersmuts en de sterrencape bij Professor Sterretje (voor wenssterren) "
      "en trek ze je guh aan.", "guhs:sterrenkijkersmuts", [fq.item("guhs:sterrenkijkersmuts"), fq.item("guhs:sterrencape")],
      rewards=(("guhs:kaas_knabbels", 12),), x=2.5, y=y, xp=150)
    q("sterrenwacht_guhdex", "Een sterrig gezicht", "Zet Professor Sterretje in je Guhdex (kom dichtbij genoeg).",
      "guhs:guhdex", [fq.adv("seen_sterrenkijkerguh")], rewards=(("guhs:kaas_knabbels", 8),), x=4, y=y, shape="rsquare", xp=100)
