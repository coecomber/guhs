"""
1.6.0 Op Safari! slice S4 (quests & extra's): the resources of feature/safari/quests (SAFARI_CONTRACT.md "Stap 2 API";
design ../../ontwerp-safaripark-en-bouwen.md 1.7, rounds 10-11, 15, 26, 31; English: tools/lang/en/c165_safari_quests.json).

  - De vermomde guh: three disguise suits (guh clothes zebrapakje / pandapakje / pinguinpakje; the zebrapakje is the reward)
  - De te vahe olifant: the olifantenloopband (deco block, also the quest's treadmill in the park) and the photo frame
    with the slim elephant
  - de geheimpjes: the trophy for finding all eight (where they are: safari_park/zone_zz_s4.py writes geheimpjes.json)
  - the full dex (46/46): the golden safari camera, the golden animal statue, the voucher for the Gouden dikdik's board
  - the pootafdruk (footprints on the trails to the hiders; they glow through a spyglass / verrekijker)
  - the titles Speurneus, Beverbaas, Safarikoning; the hidden quest advancements; the FTB chapter guhs_safari
"""
import numpy as np
from PIL import Image

from features import guhpixel_lib as px

NAME = "safari_quests"
CLOTHES = ["zebrapakje", "pandapakje", "pinguinpakje"]
BONES = {}
FTB_CHAPTER = "guhs_safari"
FTB_SECTION = "Op safari"

DECO = ["olifantenloopband", "fotolijstje_slanke_olifant", "geheimpjes_trofee", "gouden_dierenbeeld"]
ITEMS = ["gouden_safaricamera", "bordbon_gouden_dikdik"]
ADVANCEMENTS = ["safari_eerste_foto", "safari_drie_sterren", "safari_dex_10", "safari_dex_25", "safari_dex_vol",
                "safari_zone_vol", "safari_spoorzoeker", "safari_geheimpje", "safari_geheimpjes_alle",
                "safari_vermomde_guh", "safari_vahe_olifant", "safari_aaihoek"]

TEKSTEN = {
    "gui.guhs.safari.bordbon.ingewisseld": "Bordbon ingewisseld: het verblijfsbord van de Gouden dikdik! Glinster glinster, njeg.",
    # blocks and items
    "block.guhs.olifantenloopband": "Olifantenloopband",
    "block.guhs.olifantenloopband.lore": "Rennen, Dikkie, rennen! (Hij is niet te vads. Wel een beetje te vah.)",
    "block.guhs.fotolijstje_slanke_olifant": "Fotolijstje: de slanke olifant",
    "block.guhs.fotolijstje_slanke_olifant.lore": "Dikkie, na zijn dieetje. Hij past weer door zijn poortje!",
    "block.guhs.geheimpjes_trofee": "Geheimpjestrofee",
    "block.guhs.geheimpjes_trofee.lore": "Alle acht de geheimpjes van Safaripark Vadse Bergen gevonden. Speurneus!",
    "block.guhs.gouden_dierenbeeld": "Gouden dierenbeeld",
    "block.guhs.gouden_dierenbeeld.lore": "Voor wie elk dier in het safaripark op de foto zette. Safarikoning!",
    "block.guhs.pootafdruk": "Pootafdruk",
    "item.guhs.gouden_safaricamera": "Gouden safaricamera",
    "item.guhs.gouden_safaricamera.lore": "Alle 46 dieren op de foto. Je foto's krijgen gouden lijstjes. Vahoeg!",
    "item.guhs.bordbon_gouden_dikdik": "Bon: verblijfsbord Gouden dikdik",
    "item.guhs.bordbon_gouden_dikdik.lore": "Wissel deze bon in de giftshop in voor het verblijfsbord van de Gouden dikdik.",
    "item.guhs.zebrapakje": "Zebrapakje", "item.guhs.pandapakje": "Pandapakje", "item.guhs.pinguinpakje": "Pinguïnpakje",
    "gui.guhs.kledingbron.safari_quests": "De vermomde guh (Safaripark Vadse Bergen)",
    "gui.guhs.kleding.prijs.hint.vind_de_vermomde_guh": "Vind de vermomde guh in het safaripark (drie keer!)",
    # the titles
    "gui.guhs.titels.naam.speurneus": "Speurneus",
    "gui.guhs.titels.hint.speurneus": "Vind alle geheimpjes in Safaripark Vadse Bergen.",
    "gui.guhs.titels.naam.beverbaas": "Beverbaas",
    "gui.guhs.titels.hint.beverbaas": "Laat je bevers vijf dammen bouwen.",
    "gui.guhs.titels.naam.safarikoning": "Safarikoning",
    "gui.guhs.titels.hint.safarikoning": "Zet elk dier in Safaripark Vadse Bergen op de foto.",
    # the geheimpjes
    "gui.guhs.safari.geheimpje.gevonden": "Geheimpje gevonden: %s! (%s van de %s)",
    "gui.guhs.safari.geheimpje.al": "Dit geheimpje had je al gevonden: %s.",
    "gui.guhs.safari.geheimpje.alle": "Alle geheimpjes gevonden! Je bent een echte Speurneus, njeg!",
    "gui.guhs.safari.geheimpje.savanne": "een verstopte kist in het hoge gras",
    "gui.guhs.safari.geheimpje.delta": "de verloren zonnebril van de krokodil",
    "gui.guhs.safari.geheimpje.regenwoud": "de oude pot in de tempelruïne",
    "gui.guhs.safari.geheimpje.bamboe": "de oude theepot achter de pagode",
    "gui.guhs.safari.geheimpje.ijskust": "de ton in het ruim van het scheepswrak",
    "gui.guhs.safari.geheimpje.oertijd": "het fossiel in de ijsgrot",
    "gui.guhs.safari.geheimpje.europa": "de oude rangerkist in de holle eik",
    "gui.guhs.safari.geheimpje.australie": "de verloren ansichtkaart van het postkantoortje",
    # the rewards
    "gui.guhs.safari.beloning.item": "Je krijgt: %s!",
    "gui.guhs.safari.dexvol": "Elk dier in Safaripark Vadse Bergen staat op de foto! Je bent de Safarikoning. VAHOEG!",
    "gui.guhs.safari.beverdam": "Weer een beverdam af! (%s)",
    # de vermomde guh
    "gui.guhs.safari.vermomd.naam": "Streepje",
    "gui.guhs.safari.vermomd.ranger": "Er is een guh het park binnengeglipt, verkleed als zebra! Hij denkt dat niemand het ziet. "
                                      "Kun jij hem vinden? Hij hangt vast rond bij de zebra's op de savanne.",
    "gui.guhs.safari.vermomd.ranger_bezig": "Heb je die verklede guh al gevonden? Hij verstopt zich steeds bij andere dieren.",
    "gui.guhs.safari.vermomd.ranger_klaar": "Streepje woont nu bij jou? Mooi zo. Zorg goed voor hem, njeg!",
    "gui.guhs.safari.vermomd.1": "Njeg! Ik ben echt een zebra, hoor. Kijk maar naar mijn streepjes! ...Oké, je hebt me. "
                                 "Maar je vangt me nooit! *rent weg naar de bamboe*",
    "gui.guhs.safari.vermomd.2": "Ik ben een reuzenpanda. Ik eet bamboe. Nom nom. ...Hè, jij weer?! "
                                 "*waggelt weg naar het ijs*",
    "gui.guhs.safari.vermomd.3": "Pinguïns hebben het hier echt koud, zeg. Brrr. ...Oké oké, je hebt gewonnen. "
                                 "Mag ik met jou mee naar huis? Ik heb mijn zebrapakje nog!",
    "gui.guhs.safari.vermomd.klaar": "Streepje is nu jouw guh! Hij heeft zijn zelfgemaakte zebrapakje aan. +%s Safarimunten",
    "gui.guhs.safari.vermomd.niet_nu": "Psst! Ik ben gewoon een %s, hoor. Niks aan de hand. Njeg.",
    "gui.guhs.safari.vermomd.zebra": "zebra", "gui.guhs.safari.vermomd.panda": "panda", "gui.guhs.safari.vermomd.pinguin": "pinguïn",
    # de te vahe olifant
    "gui.guhs.safari.olifant.naam": "Dikkie",
    "gui.guhs.safari.olifant.start": "Pfff... Bezoekers gaven me stiekem knabbeltjes, en nu pas ik niet meer door mijn "
                                     "poortje. Je kunt niet te vads zijn, hoor... maar ik ben wel een beetje te VAH. Help je me met een dieetje? "
                                     "Ik wil graag een appel, een wortel en een stukje meloen.",
    "gui.guhs.safari.olifant.dieet": "Dikkie wil nog: %s.",
    "gui.guhs.safari.olifant.hap": "Mmm, gezond! *knabbel knabbel*",
    "gui.guhs.safari.olifant.loopband": "Dat was lekker gezond! Nu nog even rennen op de olifantenloopband naast mijn stal. "
                                        "Klik een paar keer op de loopband om me aan te moedigen!",
    "gui.guhs.safari.olifant.ren": "Rennen, Dikkie, rennen! (%s/%s)",
    "gui.guhs.safari.olifant.even": "Dikkie hijgt even uit. Nog heel even wachten...",
    "gui.guhs.safari.olifant.klaar": "Dikkie past weer door zijn poortje! Hij is zó blij. +%s Safarimunten",
    "gui.guhs.safari.olifant.na": "Kijk mij eens slank zijn! Nou ja... een beetje. Dankjewel!",
    "gui.guhs.safari.olifant.appel": "een appel", "gui.guhs.safari.olifant.wortel": "een wortel",
    "gui.guhs.safari.olifant.meloen": "een stukje meloen",
    "gui.guhs.safari.olifant.bord": "Hier woont Dikkie",
    # commands
    "commands.guhs.safari.quest": "Quest %s staat nu op: %s",
    "commands.guhs.safari.quests": "Vermomde guh: %s · Te vahe olifant: %s · Geheimpjes: %s/%s · Beverdammen: %s",
    "gui.guhs.safari.dialoog.park": "Safaripark Vadse Bergen",
    "gui.guhs.safari.dialoog.hoofdranger": "Hoofdranger",
    "gui.guhs.safari.dialoog.verkleed": "Verklede guh",
    "gui.guhs.safari.dialoog.top": "Top!",
    "gui.guhs.safari.vermomd.ranger_2": "Hij verstopt zich steeds bij andere dieren, dus hou je ogen open. Rechtsboven in je scherm zie je waar je moet zoeken. Succes, speurneus!",
    "gui.guhs.safari.tracker.titel": "Safari-quests",
    "gui.guhs.safari.tracker.vermomd": "Vermomde guh (%s/%s): zoek %s",
    "gui.guhs.safari.tracker.waar.zebra": "bij de zebra's op de savanne",
    "gui.guhs.safari.tracker.waar.reuzenpanda": "bij de reuzenpanda's in de bamboe",
    "gui.guhs.safari.tracker.waar.pinguin": "bij de pinguïns op de IJskust",
    "gui.guhs.safari.tracker.dieet": "Dikkie wil nog: %s",
    "gui.guhs.safari.tracker.loopband": "Dikkie op de loopband: %s/%s rondjes",
    "gui.guhs.safari.tracker.geheimpjes": "Geheimpjes: %s van de %s",
    "gui.guhs.safari.tracker.klaar": "Klaar! Vahoeg!",
    "gui.guhs.safari.tracker.naam.vermomd": "De vermomde guh",
    "gui.guhs.safari.tracker.naam.olifant": "De te vahe olifant",
    "gui.guhs.safari.tracker.naam.geheimpjes": "De geheimpjes",
    "gui.guhs.safari.tracker.start.vermomd": "Nog niet begonnen. Praat met de hoofdranger in het entreedorp.",
    "gui.guhs.safari.tracker.start.olifant": "Nog niet begonnen. Ga eens langs bij Dikkie in zijn stal op de savanne.",
    "gui.guhs.safari.dex.weergave.quests": "Quests",
    "gui.guhs.safari.dex.quests.kop": "Safari-quests",
    "gui.guhs.safari.dex.quests.uitleg": "Wat je nu doet, zie je ook rechtsboven in je scherm als je in het park bent.",
    "subtitles.guhs.safari.parade": "Pinguïnparade speelt",
}

# the FTB quests: (key, title, description, icon, advancement, x, shape)
FTB = [
    ("safari_eerste_foto", "Klik!", "Maak in Safaripark Vadse Bergen je eerste foto van een dier. Hij komt meteen in je Safari-dex.",
     "minecraft:spyglass", "safari_eerste_foto", -6.0, "circle"),
    ("safari_drie_sterren", "Drie sterren!", "Maak een foto van drie sterren: dichtbij, mooi in het midden, en het liefst terwijl het dier iets "
     "grappigs doet (slapen, eten, spelen...).", "minecraft:nether_star", "safari_drie_sterren", -4.5, None),
    ("safari_dex_10", "Tien dieren", "Zet tien verschillende dieren op de foto.", "minecraft:spyglass", "safari_dex_10", -3.0, None),
    ("safari_dex_25", "Vijfentwintig dieren", "Zet vijfentwintig verschillende dieren op de foto. Je bent al een halve ranger!",
     "minecraft:spyglass", "safari_dex_25", -1.5, None),
    ("safari_zone_vol", "Een hele zone", "Fotografeer alle dieren van één zone. Haal daarna je zonebadge op bij de expert-guh in het "
     "expertenpaviljoen.", "minecraft:map", "safari_zone_vol", 0.0, None),
    ("safari_spoorzoeker", "Spoorzoeker", "Sommige dieren verstoppen zich heel goed. Volg de pootafdrukjes (door een verrekijker lichten ze op) "
     "en fotografeer een verstopper, zoals de okapi, de das of de bever.", "guhs:gouden_safaricamera", "safari_spoorzoeker", 1.5, None),
    ("safari_dex_vol", "Safarikoning", "Zet elk dier in het park op de foto: alle 46! Je krijgt de gouden safaricamera, het gouden "
     "dierenbeeld en de bon voor het verblijfsbord van de Gouden dikdik.", "guhs:gouden_safaricamera", "safari_dex_vol", 3.0, "gear"),
    ("safari_geheimpje", "Psst, een geheimpje", "In elke zone van het park ligt een geheimpje verstopt. Vind er eentje!",
     "minecraft:chest", "safari_geheimpje", -6.0, "circle"),
    ("safari_geheimpjes_alle", "Speurneus", "Vind alle acht de geheimpjes. Dan krijg je de geheimpjestrofee en de titel Speurneus.",
     "guhs:geheimpjes_trofee", "safari_geheimpjes_alle", -4.5, "gear"),
    ("safari_vermomde_guh", "Een zebra met oortjes?", "Volgens de hoofdranger loopt er een guh in een zelfgemaakt zebrapakje door het park. "
     "Vind hem, drie keer! Hij gaat daarna met je mee naar huis.", "guhs:zebrapakje", "safari_vermomde_guh", -3.0, None),
    ("safari_vahe_olifant", "De te vahe olifant", "Dikkie de olifant past niet meer door zijn poortje. Help hem met een gezond dieetje en "
     "een rondje op de olifantenloopband.", "guhs:olifantenloopband", "safari_vahe_olifant", -1.5, None),
    ("safari_aaihoek", "Aaien maar!", "Doe een klusje in de aaihoek van het entreedorp: voeren, borstelen of de alpaca uitlaten.",
     "minecraft:wheat", "safari_aaihoek", 0.0, None),
]
FTB_Y = 0.0


# =====================================================================================================================
# guh clothes (make_guh_variants.py / make_clothes_icons.py): the three disguise suits, on the body bone outfit_suit
# =====================================================================================================================
def clothes(rng, v):
    def zebra():
        a = v.fabric((246, 246, 240), rng, 8)
        n = a.shape[0]
        for y in range(n):
            for x in range(n):
                if ((x + int(3 * np.sin(y / 3.0))) // 3) % 2 == 0:
                    a[y, x] = (34, 30, 36)
        a[n // 2 - 2:n // 2 + 2, n // 2 - 3:n // 2 + 3] = (238, 120, 170)       # a little pink heart-ish patch: homemade!
        return a

    def panda():
        a = v.fabric((246, 246, 244), rng, 6)
        n = a.shape[0]
        a[:, : n // 4] = (36, 34, 40)
        a[:, 3 * n // 4:] = (36, 34, 40)
        a[n // 3:n // 3 + 3, n // 2 - 2:n // 2 + 2] = (90, 160, 80)              # a bamboo leaf on the belly
        return a

    def pinguin():
        a = v.fabric((40, 42, 52), rng, 8)
        n = a.shape[0]
        a[:, n // 4:3 * n // 4] = (248, 248, 246)
        a[:4, n // 4:3 * n // 4] = (246, 190, 70)                                   # the yellow neck patch
        return a

    return {"zebrapakje": {"suit": zebra}, "pandapakje": {"suit": panda}, "pinguinpakje": {"suit": pinguin}}


def icons(ic):
    return {"zebrapakje": ic.shirt((246, 246, 240), (34, 30, 36), (34, 30, 36), "stripes"),
            "pandapakje": ic.shirt((246, 246, 244), (36, 34, 40), (36, 34, 40), "buttons"),
            "pinguinpakje": ic.shirt((40, 42, 52), (20, 20, 26), (248, 248, 246), "buttons")}


# =====================================================================================================================
# textures
# =====================================================================================================================
def _img(rows, kleuren):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c != ".":
                kl = kleuren[c]
                img.putpixel((x, y), tuple(kl) + ((255,) if len(kl) == 3 else ()))
    return img


def _vlak(kleur, ruis=10, seed=1):
    r = np.random.default_rng(seed)
    a = np.clip(np.array(kleur, np.float32) + r.normal(0, ruis / 2, (16, 16, 1)), 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([a, np.full((16, 16), 255, np.uint8)]), "RGBA")


def textures(h):
    goud = {"a": (250, 214, 90), "b": (220, 170, 40), "c": (170, 120, 20), "d": (255, 244, 180)}
    h.save(_vlak((60, 62, 70), 12, 2), "block", "olifantenloopband_band.png")
    h.save(_vlak((150, 156, 166), 8, 3), "block", "olifantenloopband_voet.png")
    h.save(_vlak((240, 120, 170), 8, 4), "block", "olifantenloopband_knop.png")
    h.save(_vlak((236, 200, 120), 10, 5), "block", "fotolijstje_lijst.png")
    foto = Image.new("RGBA", (16, 16), (196, 230, 250, 255))       # a sky-blue photo with the slim elephant on the grass
    foto.alpha_composite(_img([
        "................", ".......bbb......", "......bbbbb.....", ".....bbbbbbbb...", "....bbbcbbbbbb..", "....bbbbbbbbbb..",
        "...bbbbbbbbbbb..", "..bb.bbbbbbbbb..", "..b..bbbbbbbbb..", "..b...bb..bb....", "......bb..bb....",
        "gggggggggggggggg", "gggggggggggggggg", "ggggdddddddddggg", "gggggggggggggggg", "gggggggggggggggg"],
        {"b": (150, 158, 172), "c": (30, 30, 40), "d": (240, 120, 170), "g": (120, 190, 90)}))
    h.save(foto, "block", "fotolijstje_foto.png")
    h.save(_vlak((250, 214, 90), 14, 6), "block", "gouden_beeld.png")
    h.save(_vlak((120, 74, 40), 8, 7), "block", "trofee_voet.png")
    h.save(_img([
        "abababababababab", "bcbcbcbcbcbcbcbc", "abababababababab", "bcbcbcbcbcbcbcbc", "abababababababab", "bcbcbcbcbcbcbcbc",
        "abababababababab", "bcbcbcbcbcbcbcbc", "abababababababab", "bcbcbcbcbcbcbcbc", "abababababababab", "bcbcbcbcbcbcbcbc",
        "abababababababab", "bcbcbcbcbcbcbcbc", "abababababababab", "bcbcbcbcbcbcbcbc"], goud), "block", "trofee_goud.png")
    h.save(_img([
        "................", "...aa...........", "..aaaa..........", "..aaaa..a.a.....", "...aa...........", "..........aa....",
        ".........aaaa...", ".........aaaa...", "...aa.....aa.a..", "..aaaa..........", "..aaaa..........",
        "...aa....aa.....", ".........aaaa...", ".....a...aaaa...", "..........aa....", "................"],
        {"a": (70, 50, 36, 170)}), "block", "pootafdruk.png")
    # items
    h.save(_img([
        "................", "................", "....aa..........", "...abba.........", "aaaaaaaaaaaaaaa.", "abbbbbbbbbbbbba.",
        "abbbccccccbbbba.", "abbcddddddcbbba.", "abbcdeeeedcbdba.", "abbcdeeeedcbbba.", "abbcddddddcbbba.",
        "abbbccccccbbbba.", "abbbbbbbbbbbbba.", "aaaaaaaaaaaaaaa.", "................", "................"],
        {"a": (170, 120, 20), "b": (250, 214, 90), "c": (220, 170, 40), "d": (60, 60, 70), "e": (120, 190, 240)}),
        "item", "gouden_safaricamera.png")
    h.save(_img([
        "................", "................", ".aaaaaaaaaaaaaa.", ".abbbbbbbbbbbba.", ".abccccbbbbbbba.", ".abcddcbbeeebba.",
        ".abccccbbbbbbba.", ".abbbbbbbeeeeba.", ".abffffffffffba.", ".abbbbbbbbbbbba.", ".aaaaaaaaaaaaaa.", "................",
        "................", "................", "................", "................"],
        {"a": (170, 120, 20), "b": (255, 244, 210), "c": (250, 214, 90), "d": (150, 100, 40), "e": (120, 100, 80),
         "f": (240, 120, 170)}), "item", "bordbon_gouden_dikdik.png")


# =====================================================================================================================
# blocks (DecoBlock models, facing north) and items
# =====================================================================================================================
def _el(f, t, tex, faces=("north", "south", "east", "west", "up", "down")):
    return {"from": f, "to": t, "faces": {k: {"texture": tex} for k in faces}}


def blocks(h):
    px.deco(h, "olifantenloopband", [
        _el([0, 0, 1], [16, 2, 15], "#voet"),
        _el([1, 2, 2], [15, 3, 14], "#band"),
        _el([0, 2, 1], [1, 4, 15], "#voet"), _el([15, 2, 1], [16, 4, 15], "#voet"),
        _el([1, 3, 1], [2, 14, 2], "#voet"), _el([14, 3, 1], [15, 14, 2], "#voet"),
        _el([1, 12, 0], [15, 14, 3], "#voet"),
        _el([6, 13, 0], [10, 15, 2], "#knop"),
    ], {"voet": "guhs:block/olifantenloopband_voet", "band": "guhs:block/olifantenloopband_band",
        "knop": "guhs:block/olifantenloopband_knop"}, TEKSTEN["block.guhs.olifantenloopband"],
        TEKSTEN["block.guhs.olifantenloopband.lore"])
    px.deco(h, "fotolijstje_slanke_olifant", [
        _el([2, 0, 9], [14, 1, 12], "#lijst"),
        _el([2, 1, 9], [14, 13, 10], "#lijst"),
        {"from": [3, 2, 8.9], "to": [13, 12, 9], "faces": {"north": {"texture": "#foto", "uv": [0, 0, 16, 16]}}},
        _el([7, 0, 10], [9, 8, 14], "#lijst"),
    ], {"lijst": "guhs:block/fotolijstje_lijst", "foto": "guhs:block/fotolijstje_foto"},
        TEKSTEN["block.guhs.fotolijstje_slanke_olifant"], TEKSTEN["block.guhs.fotolijstje_slanke_olifant.lore"])
    px.deco(h, "geheimpjes_trofee", [
        _el([3, 0, 3], [13, 3, 13], "#voet"),
        _el([6, 3, 6], [10, 7, 10], "#goud"),
        _el([3, 7, 3], [13, 14, 13], "#goud"),
        _el([1, 9, 7], [3, 13, 9], "#goud"), _el([13, 9, 7], [15, 13, 9], "#goud"),
    ], {"voet": "guhs:block/trofee_voet", "goud": "guhs:block/trofee_goud"},
        TEKSTEN["block.guhs.geheimpjes_trofee"], TEKSTEN["block.guhs.geheimpjes_trofee.lore"])
    # a chunky golden elephant on a plinth (body, head, ears, trunk, four legs)
    px.deco(h, "gouden_dierenbeeld", [
        _el([2, 0, 2], [14, 2, 14], "#voet"),
        _el([4, 2, 5], [6, 6, 7], "#goud"), _el([10, 2, 5], [12, 6, 7], "#goud"),
        _el([4, 2, 10], [6, 6, 12], "#goud"), _el([10, 2, 10], [12, 6, 12], "#goud"),
        _el([3, 6, 5], [13, 12, 13], "#goud"),
        _el([5, 8, 1], [11, 14, 5], "#goud"),
        _el([2, 9, 2], [5, 14, 4], "#goud"), _el([11, 9, 2], [14, 14, 4], "#goud"),
        _el([7, 4, 0], [9, 9, 2], "#goud"),
    ], {"voet": "guhs:block/trofee_voet", "goud": "guhs:block/gouden_beeld"},
        TEKSTEN["block.guhs.gouden_dierenbeeld"], TEKSTEN["block.guhs.gouden_dierenbeeld.lore"])
    # the pootafdruk: a flat 1/16 layer with the prints (cutout), turned with the trail (facing); no item, no drop
    h.w(f"{h.A}/models/block/pootafdruk.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                "textures": {"particle": "guhs:block/pootafdruk", "p": "guhs:block/pootafdruk"},
                                                "elements": [{"from": [0, 0, 0], "to": [16, 0.25, 16],
                                                              "faces": {"up": {"texture": "#p"}}}]})
    h.w(f"{h.A}/blockstates/pootafdruk.json", {"variants": h.facing_states("pootafdruk")})
    for item in ITEMS:
        h.item_model(item)


def lang(h):
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def advancements(h):
    for name in ADVANCEMENTS:
        px.quest_adv(h, name)


def sounds(h):
    """Test round 4: the four phrases of the pinguinparade tune (OGGs by safari_parade_geluid.py)."""
    def patch(d):
        for i in range(1, 5):
            d[f"safari.parade.{i}"] = {"sounds": [{"name": f"guhs:safari/parade/parade{i}"}], "subtitle": "subtitles.guhs.safari.parade"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def build(h):
    textures(h)
    blocks(h)
    lang(h)
    advancements(h)
    sounds(h)


# =====================================================================================================================
# FTB quests: the chapter guhs_safari (make_ftbquests.py CHAPTERS), no locks
# =====================================================================================================================
def ftb(fq):
    for i, (key, titel, tekst, icoon, adv, x, vorm) in enumerate(FTB):
        y = FTB_Y if i < 7 else FTB_Y + 2.0
        fq.q(key, titel, tekst, icoon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 8),), x=x, y=y, shape=vorm,
             xp=250 if vorm == "gear" else 100)
