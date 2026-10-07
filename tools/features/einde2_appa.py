"""
einde2 (the Guheinde expansion), slice appa: the Appa-guh and the Momo-guh (DESIGN "Wat je overhoudt").
Java: nl.juiced.guhs.feature.einde2.appa (E2AppaFeature, AppaGuhEntity + Appa, MomoGuhEntity + Momo, AppaFluitjeItem).

  build(h)   the models/animations/textures of both (einde2_appa_modellen), the Appa-fluitje (icon, model, recipe), the two
             creative spawn eggs, the sounds, the Guhdex pages, the texts (Dutch in both files), the game test rooms
             (einde2_appa_test_*), and a self-check
No ftb(fq): nothing in phase A gives an Appa or a Momo in survival; the story does (phase B), and its quests come with it.

The Appa-guh: a big six-legged flying bison with a guh face, one per player, never lost (the fluitje always brings him, with
his saddle chest and his name). His owner sits on his head; three guests (players, or the owner's own guhs) and the team of
the story ride in the saddle basket. The Momo-guh: a winged lemur on your right shoulder that picks things up and now and
then borrows a knabbel (and gives it back).
"""
import os

from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import einde2_appa_modellen as modellen

DIEREN = ["appa_guh", "momo_guh"]

# =====================================================================================================================
# texts (Dutch in both files)
# =====================================================================================================================
DEX = {  # page -> (name, rarity, info)
    "appa_guh": ("Appa-guh", "Eén per mensje, en hij kiest zelf",
                 "Een luchtbizon met een guh-snoet: zes poten, een pijl van zijn voorhoofd tot het puntje van zijn platte staart, "
                 "en twee zachte ronde guh-oren boven zijn hoorns. Zo groot als een schuur, zo lief als een guh. Klik om op te "
                 "stappen; zeg 'jip jip' (spring) en hij stijgt op, sluip en hij zakt. Hij vliegt langzamer dan Guhvleugels, maar "
                 "wordt nooit moe en hij doet het overal. In zijn zadelmand passen drie vrienden: andere mensjes, of je eigen guhs "
                 "(klik met een guh op je arm of aan de lijn). Sluip + klik: de zadelkist. Een aai tegen zijn flank en je guhs "
                 "rollen er weer uit. Kwijt? Blaas op je Appa-fluitje. Hij komt altijd, njeg."),
    "momo_guh": ("Momo-guh", "Eén per mensje: hij kiest de schouder met de meeste knabbels",
                 "Een vliegende lemur met reusachtige ronde oren, een ringelstaart en grote groene guh-ogen. Hij woont op je "
                 "rechterschouder en raapt alles op wat binnen zes blokken ligt: hup, in je tas. Wat je zelf weggooit laat hij "
                 "netjes liggen. Af en toe 'leent' hij een kaasknabbel uit je tas. Die krijg je terug. Echt. Sluip + klik met een "
                 "lege hand op een blok en hij blijft daar thuis zitten; sluip + klik op hem (of sluip en blaas op je "
                 "Appa-fluitje) en hij fladdert weer naar je schouder."),
}

TEXTS = {
    # the fluitje and the spawn eggs
    "item.guhs.appa_fluitje": "Appa-fluitje",
    "item.guhs.appa_fluitje.lore": "Je hoort er niets van. Eén heel grote, heel pluizige vriend wel.",
    "item.guhs.appa_fluitje.tooltip": "Blaas: je Appa-guh komt naar je toe, waar hij ook is.",
    "item.guhs.appa_fluitje.tooltip.momo": "Sluip + blaas: je Momo-guh komt terug op je schouder.",
    # the Appa-guh
    "gui.guhs.einde2_appa.jip_jip": "Jip jip!",
    "gui.guhs.einde2_appa.opstappen": "Je zit op de kop van %s. Spring = jip jip, sluip = omlaag.",
    "gui.guhs.einde2_appa.instappen": "Je klimt in de zadelmand van %s. %s stuurt. Hou je vast, njeg!",
    "gui.guhs.einde2_appa.vol": "De zadelmand zit vol. Er past zelfs geen magere guh meer bij.",
    "gui.guhs.einde2_appa.guh_erop": "%s wipt in de zadelmand. Vahoeg!",
    "gui.guhs.einde2_appa.guhs_eraf": "Hup, eraf! Je guhs rollen uit de zadelmand.",
    "gui.guhs.einde2_appa.blijf_zitten": "Njeg! Van een vliegende Appa-guh spring je niet af. Eerst landen.",
    "gui.guhs.einde2_appa.kist": "Zadelkist van %s",
    "gui.guhs.einde2_appa.kist_niet": "Dit is de zadelkist van %s. De Appa-guh kijkt je aan tot je je hand weghaalt.",
    "gui.guhs.einde2_appa.wild": "Een Appa-guh zonder mensje. Hij snuffelt aan je, maar hij wacht op iemand anders, njeg.",
    "gui.guhs.einde2_appa.iemand": "iemand anders",
    "gui.guhs.einde2_appa.wijst": "%s wijst de weg: %s  nog %s blokken (laat los en hij vliegt zelf)",
    "gui.guhs.einde2_appa.vliegt_zelf": "%s vliegt zelf. Leun achterover en vads. Nog %s blokken.",
    "gui.guhs.einde2_appa.aangekomen": "%s landt met een plof. Jullie zijn er, njeg!",
    # the fluitje's lines
    "gui.guhs.einde2_appa.fluitje.geen": "Je blaast. Niemand hoort iets, jij ook niet. Je hebt nog geen Appa-guh, njeg.",
    "gui.guhs.einde2_appa.fluitje.weg": "Je blaast en blaast... maar je Appa-guh komt niet. Iemand houdt hem vast. Njeg.",
    "gui.guhs.einde2_appa.fluitje.komt": "%s hoort je fluitje en komt aangevadst. Jip jip!",
    "gui.guhs.einde2_appa.fluitje.zit_al": "Je zit er al op. Hij draait één oor naar je toe.",
    "gui.guhs.einde2_appa.fluitje.momo_geen": "Je fluit heel hoog. Er komt geen Momo-guh: je hebt er geen, njeg.",
    "gui.guhs.einde2_appa.fluitje.momo_al": "%s zit al op je schouder en kijkt je vragend aan.",
    "gui.guhs.einde2_appa.fluitje.momo_komt": "%s fladdert naar je toe. Hij heeft niets gejat. Zegt hij.",
    # the Momo-guh
    "gui.guhs.einde2_appa.momo.schouder": "%s springt op je rechterschouder. Zijn staart kriebelt in je nek.",
    "gui.guhs.einde2_appa.momo.thuis": "%s blijft hier thuis zitten en belooft nergens aan te komen. (Sluip + klik, of sluip + fluitje: weer mee.)",
    "gui.guhs.einde2_appa.momo.aai": "%s spint. Of het is zijn maag.",
    "gui.guhs.einde2_appa.momo.niet_van_jou": "Dit is de Momo-guh van %s. Hou je knabbels goed vast.",
    "gui.guhs.einde2_appa.momo.wild": "Een Momo-guh zonder mensje. Hij loert naar je tas.",
    "gui.guhs.einde2_appa.momo.vol": "%s wijst naar de grond en dan naar je tas. Die zit vol, njeg.",
    "gui.guhs.einde2_appa.momo.jat.1": "%s graait een kaasknabbel uit je tas en kijkt alsof hij van niets weet.",
    "gui.guhs.einde2_appa.momo.jat.2": "Hé! %s zit op je schouder te smakken. Dat is JOUW kaasknabbel.",
    "gui.guhs.einde2_appa.momo.jat.3": "%s heeft een kaasknabbel 'geleend'. Geleend, njeg.",
    "gui.guhs.einde2_appa.momo.jat.4": "Er ritselt iets in je tas. %s fluit heel onschuldig een liedje.",
    "gui.guhs.einde2_appa.momo.jat.5": "%s houdt een kaasknabbel omhoog: kijk eens wat ik gevonden heb! In jouw tas.",
    "gui.guhs.einde2_appa.momo.terug": "...en hij geeft hem terug, njeg.",
    # op commands
    "gui.guhs.einde2_appa.commando.gegeven": "%s heeft nu een Appa-guh (en een fluitje).",
    "gui.guhs.einde2_appa.commando.momo": "%s heeft nu een Momo-guh.",
    "gui.guhs.einde2_appa.commando.geroepen": "De Appa-guh van %s komt eraan.",
    "gui.guhs.einde2_appa.commando.weg": "De Appa-guh van %s wordt weggehouden.",
    "gui.guhs.einde2_appa.commando.niet_weg": "De Appa-guh van %s mag weer komen.",
    "gui.guhs.einde2_appa.commando.vlieg": "De Appa-guh vliegt %s erheen.",
    "gui.guhs.einde2_appa.commando.gestopt": "De vlucht van %s is afgebroken.",
    "gui.guhs.einde2_appa.commando.thuis": "De Momo-guh van %s blijft thuis.",
    "gui.guhs.einde2_appa.commando.mee": "De Momo-guh van %s gaat weer mee.",
    "gui.guhs.einde2_appa.commando.jat": "De Momo-guh van %s leent een kaasknabbel.",
    "gui.guhs.einde2_appa.commando.mislukt": "Dat lukt niet bij %s (geen Appa-guh of Momo-guh, of niets te doen).",
    # sounds
    "subtitles.guhs.einde2_appa.jip_jip": "Jip jip!",
    "subtitles.guhs.einde2_appa.brul": "Appa-guh bromt tevreden",
    "subtitles.guhs.einde2_appa.momo": "Momo-guh kwettert",
    "subtitles.guhs.einde2_appa.fluitje": "Fluitje (heel hoog)",
}

SOUNDS = {
    "einde2_appa.jip_jip": [{"name": "minecraft:entity.camel.dash", "type": "event", "pitch": 0.7},
                            {"name": "minecraft:entity.polar_bear.ambient", "type": "event", "pitch": 0.75, "volume": 0.8}],
    "einde2_appa.brul": [{"name": "minecraft:entity.polar_bear.ambient", "type": "event", "pitch": 0.55},
                         {"name": "minecraft:entity.camel.ambient", "type": "event", "pitch": 0.6},
                         {"name": "minecraft:entity.panda.ambient", "type": "event", "pitch": 0.6}],
    "einde2_appa.momo": [{"name": "minecraft:entity.parrot.ambient", "type": "event", "pitch": 1.7},
                         {"name": "minecraft:entity.fox.ambient", "type": "event", "pitch": 1.9, "volume": 0.7},
                         {"name": "minecraft:entity.bat.ambient", "type": "event", "pitch": 1.2, "volume": 0.6}],
    "einde2_appa.fluitje": [{"name": "minecraft:block.note_block.flute", "type": "event", "pitch": 2.0},
                            {"name": "minecraft:entity.allay.ambient_without_item", "type": "event", "pitch": 1.8, "volume": 0.6}],
}


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)
    for page, (name, rarity, info) in DEX.items():
        kit.pagina(h, page, name, rarity, info)


# =====================================================================================================================
# the fluitje's icon (16x16, drawn): a little bison-shaped whistle on a cord
# =====================================================================================================================
def icon_fluitje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cream, shade, brown, dark = (248, 240, 224, 255), (214, 198, 172, 255), (146, 100, 72, 255), (96, 64, 46, 255)
    d.line([1, 2, 5, 6], fill=(232, 138, 52, 255))                               # the orange cord
    d.line([1, 3, 4, 6], fill=(186, 86, 40, 255))
    d.rounded_rectangle([4, 6, 13, 12], 2, fill=cream, outline=shade)            # the body of the whistle (Appa's body)
    d.rectangle([12, 8, 15, 10], fill=cream, outline=shade)                      # the mouthpiece (his flat tail)
    d.line([7, 6, 13, 6], fill=brown)                                            # the arrow down his back
    d.line([13, 9, 15, 9], fill=brown)
    d.point((6, 7), fill=brown)
    d.rectangle([9, 8, 10, 9], fill=dark)                                        # the sound hole
    d.point((5, 9), fill=(22, 18, 40, 255))                                      # an eye, a highlight, a blush
    d.point((5, 8), fill=(255, 255, 255, 255))
    d.point((6, 10), fill=(255, 150, 186, 255))
    d.rectangle([3, 5, 4, 6], fill=(188, 152, 208, 255))                         # a round lilac guh ear
    for x in (5, 8, 11):                                                         # three of his six little legs
        d.rectangle([x, 13, x + 1, 14], fill=shade)
    d.point((14, 4), fill=(255, 255, 255, 255))                                  # a note nobody hears
    d.point((15, 5), fill=(210, 236, 255, 255))
    return img


def textures(h):
    h.save(icon_fluitje(), "item", "appa_fluitje.png")


def blocks_and_items(h):
    h.item_model("appa_fluitje")
    kit.spawn_ei(h, "appa_guh", "Appa-guh-spawnei", 0xF8F0E0, 0x926448)
    kit.spawn_ei(h, "momo_guh", "Momo-guh-spawnei", 0xF6F2EC, 0x60525A)
    # craftable, so the way to your Appa is never lost for good (it does nothing for who has no Appa)
    h.shaped("appa_fluitje", [" B", "BK"], {"B": "minecraft:bamboo", "K": "guhs:kaas_knabbels"}, "guhs:appa_fluitje", 1)
    # nothing ever drops from them (nothing hurts them)
    for e in DIEREN:
        h.w(f"{h.D}/loot_table/entities/{e}.json", {"type": "minecraft:entity", "pools": []})


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# game test rooms
# =====================================================================================================================
def test_templates(h):
    # a meadow: seats, the chest, the whistle, Momo
    t = h.Structure((16, 9, 16))
    for x in range(16):
        for z in range(16):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("einde2_appa_test_wei")
    # a long runway with a wall across it: the flight by himself has to go over
    t = h.Structure((14, 24, 44))
    for x in range(14):
        for z in range(44):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    for x in range(14):
        for y in range(1, 6):
            t.set(x, y, 22, "minecraft:stone_bricks")
    t.save("einde2_appa_test_baan")


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    problems = modellen.check(h)
    for key, text in list(TEXTS.items()) + [(k, " ".join(v)) for k, v in DEX.items()]:
        low = text.lower()
        if "te vads" in low or "hamster" in low or "laatste" in low or "sterkste" in low:
            problems.append(f"lore: {key}")
    for e in DIEREN:
        for key in (f"entity.guhs.{e}", f"item.guhs.{e}_spawn_egg", f"gui.guhs.guhdex.info.{e}", f"gui.guhs.guhdex.rarity.{e}"):
            if key not in h.NL:
                problems.append(f"missing lang {key}")
        for p in (f"{h.A}/geckolib/models/entity/{e}.geo.json", f"{h.A}/geckolib/animations/entity/{e}.animation.json",
                  os.path.join(h.TEX, "entity", f"{e}.png"), f"{h.A}/models/item/{e}_spawn_egg.json",
                  f"{h.D}/advancement/quest/seen_{e}.json", f"{h.D}/loot_table/entities/{e}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for n in range(1, 6):                                            # = Momo.JAT_ZINNEN
        if f"gui.guhs.einde2_appa.momo.jat.{n}" not in h.NL:
            problems.append(f"missing knabbel line {n}")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in h.NL:
            problems.append(f"missing subtitle {event}")
    if problems:
        raise SystemExit("einde2_appa self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    textures(h)
    blocks_and_items(h)
    sounds(h)
    texts(h)
    test_templates(h)
    selfcheck(h)
