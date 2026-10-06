"""
bbq2 (sausdieren): the Sausloper and its stable, the Sausblubje, blubroom and its Guhdrankje. Java: feature/sausdieren.

  - de Sausloper (entity guhs:sausloper, Guhdex page SAUSLOPER): model/animations/textures in sausdieren_modellen.py
  - het Sausblubje (entity guhs:sausblubje, page SAUSBLUBJE), blubroom, het Sausblubje in een potje, het Stuiterdrankje
    (Brouwsel BLUBROOM, effect guhs:sausdieren_stuiter), pindasaus aan een stok
  - de Sausloper-stal (structure guhs:sausloper_stal: sausdieren_bouw.py; a "grot" of wereld.bbq_structuur with its
    guaranteed copy in sector 6) with the Verzorger-guh and his questline "sausloper" (5 steps, per player)
  - spawns in the five biomes of the Guhbarbecuether, loot, the recipe of the stick, tags, sounds, advancements, FTB quests

Fixed ids of CONTRACT_130 7 that this module owns: blubroom, sausblubje_potje (items), sausblubje, sausloper (entities).
The wiki entries are in sausdieren_wiki.py.
"""
import json
import os
import re

from PIL import Image

from . import bbq2
from . import sausdieren_bouw as bouw
from . import sausdieren_modellen as modellen
from . import spiesburcht_tex
from . import verhaal_motor
from . import wereld

NAME = "sausdieren"
SALT = 21301201
STRUCTUUR = "sausloper_stal"

FTB_SECTIES = [("sausdieren_stal", "De Sausloper-stal", "npc:verzorgerguh",
                ["sausdieren_stal", "sausdieren_sausloper_1", "sausdieren_sausloper_2", "sausdieren_sausloper_3", "sausdieren_sausloper_4",
                 "sausdieren_sausloper_5", "sausdieren_eigen_loper", "sausdieren_loper_dex", "sausdieren_snel"]),
               ("sausdieren_blubjes", "Sausblubjes en blubroom", "item:guhs:blubroom", None)]

# --- spawns: creature entries added to the biomes of the Guhbarbecuether (type, weight, min, max) -------------------------
SPAWNS = {
    "houtskoolvlakte": [("guhs:sausloper", 8, 1, 2), ("guhs:sausblubje", 6, 1, 2)],
    "asdal": [("guhs:sausloper", 5, 1, 1), ("guhs:sausblubje", 3, 1, 1)],
    "satebos": [("guhs:sausloper", 6, 1, 2), ("guhs:sausblubje", 3, 1, 2)],
    "worstenwoud": [("guhs:sausloper", 6, 1, 2), ("guhs:sausblubje", 3, 1, 2)],
    "rookdelta": [("guhs:sausloper", 10, 1, 2), ("guhs:sausblubje", 12, 1, 3)],
}

SOUNDS = {
    "sausdieren.sausloper": [{"name": "minecraft:entity.strider.ambient", "type": "event", "pitch": 1.45, "volume": 0.7},
                             {"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 0.85, "volume": 0.6}],
    "sausdieren.sausloper.blij": [{"name": "minecraft:entity.strider.happy", "type": "event", "pitch": 1.5},
                                  {"name": "guhs:entity.guh.happy", "type": "event", "pitch": 0.9, "volume": 0.7}],
    "sausdieren.sausloper.bibber": [{"name": "minecraft:entity.strider.retreat", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "sausdieren.sausloper.stap": [{"name": "minecraft:entity.strider.step_lava", "type": "event", "pitch": 1.2, "volume": 0.6}],
    "sausdieren.sausloper.smak": [{"name": "minecraft:entity.strider.eat", "type": "event", "pitch": 1.3}],
    "sausdieren.sausblubje.plof": [{"name": "minecraft:entity.magma_cube.squish_small", "type": "event", "pitch": 1.3},
                                   {"name": "minecraft:block.honey_block.fall", "type": "event", "pitch": 1.2}],
    "sausdieren.sausblubje.blub": [{"name": "minecraft:block.bubble_column.bubble_pop", "type": "event", "pitch": 0.8},
                                   {"name": "minecraft:block.lava.pop", "type": "event", "pitch": 1.5}],
    "sausdieren.sausblubje.splits": [{"name": "minecraft:entity.magma_cube.squish", "type": "event", "pitch": 1.4},
                                     {"name": "minecraft:block.honey_block.break", "type": "event", "pitch": 1.3}],
    "sausdieren.potje": [{"name": "minecraft:item.bottle.fill", "type": "event", "pitch": 0.8}],
    "sausdieren.poortje": [{"name": "minecraft:block.note_block.chime", "type": "event"}],
    "sausdieren.stuiter": [{"name": "minecraft:block.slime_block.fall", "type": "event", "pitch": 1.2},
                           {"name": "minecraft:entity.magma_cube.jump", "type": "event", "pitch": 1.4, "volume": 0.6}],
}
SUBTITLES = {
    "sausdieren.sausloper": "Sausloper snuffelt", "sausdieren.sausloper.blij": "Sausloper is blij",
    "sausdieren.sausloper.bibber": "Sausloper bibbert", "sausdieren.sausloper.stap": "Sausloper stapt door de saus",
    "sausdieren.sausloper.smak": "Sausloper smakt", "sausdieren.sausblubje.plof": "Sausblubje ploft neer",
    "sausdieren.sausblubje.blub": "Sausblubje blubt", "sausdieren.sausblubje.splits": "Sausblubje splitst",
    "sausdieren.potje": "Sausblubje hupt een potje in of uit", "sausdieren.poortje": "Poortje gehaald",
    "sausdieren.stuiter": "Boing",
}

# --- Dutch texts ------------------------------------------------------------------------------------------------------------
V = "quest.guhs.sausdieren.verzorger."
LANG = {
    "item.guhs.blubroom": "Blubroom",
    "item.guhs.blubroom.lore": "Een warme klodder van een blij Sausblubje. Voor in de Guhbrouwketel, en voor sauzige uitvindingen",
    "item.guhs.sausblubje_potje": "Sausblubje in een potje",
    "item.guhs.sausblubje_potje.lore": "Het blubt tevreden tegen het glas. Njeg",
    "item.guhs.sausblubje_potje.tooltip": "Rechtsklik op een blok om het weer vrij te laten",
    "item.guhs.sausdieren_pindasaus_stok": "Pindasaus aan een stok",
    "item.guhs.sausdieren_pindasaus_stok.lore": "Een Sausloper loopt er vahoeg achteraan. Rechtsklik tijdens het rijden voor een sprintje",
    "item.guhs.sausdieren_stuiterdrankje": "Stuiterdrankje",
    "item.guhs.guhdrankje.blubroom.lore": "Kaasbouillon met blubroom. Je valt niet meer, je stuitert. Boing, njeg!",
    "item.guhs.sausloper_spawn_egg": "Sausloper-spawnei",
    "item.guhs.sausblubje_spawn_egg": "Sausblubje-spawnei",
    "effect.guhs.sausdieren_stuiter": "Stuiterblub",
    "quest.guhs.guhbrouwketel.brouwsel.blubroom": "Stuiterdrankje",
    # the Sausloper
    "gui.guhs.sausdieren.blijft": "%s blijft hier braaf wachten",
    "gui.guhs.sausdieren.loopt": "%s mag weer rondstappen",
    "gui.guhs.sausdieren.leen_ander": "Deze Sausloper wacht op iemand anders. Vraag de Verzorger-guh om je eigen proefrit",
    "gui.guhs.sausdieren.stok_nodig": "Houd pindasaus aan een stok vast: hij loopt waar jij heen kijkt",
    "gui.guhs.sausdieren.aai_bewoner": "%s snuffelt aan je hand. Heb je pindascheutjes bij je?",
    "gui.guhs.sausdieren.aai_leen": "%s staat klaar voor je proefrit. Stap maar op!",
    "gui.guhs.sausdieren.aai_tam": "%s kwispelt met zijn krulstaartje. Vahoeg!",
    "gui.guhs.sausdieren.aai_zadel": "%s is van jou. Leg er een zadel op, dan kun je rijden",
    "gui.guhs.sausdieren.aai_wild": "%s kijkt je verlegen aan. Hij lust wel pindascheutjes",
    "gui.guhs.sausdieren.aai_onbekend": "%s kent je nog niet. De Verzorger-guh in de Sausloper-stal leert je hoe het moet",
    "gui.guhs.sausdieren.eerst_stal": "%s snuffelt, maar durft niet. Leer eerst bij de Verzorger-guh in de Sausloper-stal hoe je vrienden wordt",
    "gui.guhs.sausdieren.getemd": "%s is nu je eigen Sausloper! Leg er een zadel op",
    "gui.guhs.sausdieren.niet_slaan": "Njeg! Een Sausloper sla je niet, die aai je",
    "gui.guhs.sausdieren.njam": "Njam! De Sausloper smakt tevreden",
    # the Sausblubje
    "gui.guhs.sausdieren.te_groot_voor_potje": "%s past niet in je flesje. Alleen een klein blubje past erin",
    "gui.guhs.sausdieren.gegroeid": "%s is van al die knabbels een maatje gegroeid. Blub!",
    "gui.guhs.sausdieren.nog_voer": "Blub! Nog %s knabbel(s) en het groeit een maatje",
    "gui.guhs.sausdieren.knuffel_klein": "%s blubt van blijdschap. Kleiner dan dit wordt het niet",
    "gui.guhs.sausdieren.gesplitst": "Blub blub! %s splitst van blijdschap en laat blubroom achter",
    "gui.guhs.sausdieren.niet_slaan_blubje": "Njeg! Een Sausblubje sla je niet, dat knuffel je",
    # the test lap
    "gui.guhs.sausdieren.poortje": "Poortje %s van %s: volg de sterretjes",
    "gui.guhs.sausdieren.stap_op": "Stap op je Sausloper (rechtsklik) voor poortje %s van %s",
    "gui.guhs.sausdieren.te_ver": "Ho ho, zo ver mag hij niet van stal. Terug naar de start!",
    "gui.guhs.sausdieren.rondje_klaar": "Je proefrondje zit erop in %s seconden. Vahoeg!",
    "gui.guhs.sausdieren.rondje_record": "Een rondje in %s seconden: je snelste tot nu toe. Njeg!",
    "gui.guhs.sausdieren.rondje_tijd": "Een rondje in %s seconden (je snelste: %s)",
    "gui.guhs.sausdieren.optie.graag": "Dat wil ik leren!",
    "gui.guhs.sausdieren.optie.opstappen": "Ik ben er klaar voor!",
    "gui.guhs.sausdieren.optie.rondje": "Nog een rondje!",
    "gui.guhs.sausdieren.optie.uitleg": "Hoe tem ik er zelf een?",
    # the Verzorger-guh
    V + "hallo": "Njeg! Welkom in de Sausloper-stal. Mijn lopers stappen de hele dag door de frituursaus, want op het droge krijgen ze "
                 "koude pootjes. Wil je leren hoe je er vrienden mee wordt? Dan mag je er straks zelf een temmen.",
    V + "lok": "Hier, pindasaus aan een stok. Daar lopen ze vahoeg achteraan. Lok er eens eentje uit de sausbak naar mij toe!",
    V + "stok_kwijt": "Stok kwijt? Opgelikt zeker. Hier heb je een nieuwe, njeg.",
    V + "gelokt": "Kijk hem eens stappen! En zie je hem bibberen? Dat is de kou. Kom, we maken hem warm vanbinnen.",
    V + "voer": "Vriendschap gaat door de maag. Hier zijn drie pindascheutjes: voer ze aan een Sausloper van de stal.",
    V + "voer_nog": "Nog %s pindascheutje(s) en hij vertrouwt je. Voer ze aan een Sausloper hier in de stal, njeg.",
    V + "voer_kwijt": "Scheutjes op? Zelf opgesmikkeld zeker, njeg. Hier heb je er nog %s. Maar deze zijn echt voor de Sausloper!",
    V + "proefrit": "Hij vertrouwt je! Tijd voor een proefrondje door de sausbak: door de vier poortjes, eindigen bij de geblokte "
                    "finish. Houd je stok goed vast, hij loopt waar jij heen kijkt.",
    V + "start": "Fiet-fiew! Daar is je Sausloper, bij de steiger. Stap op en volg de sterretjes. Geen haast, hij kan niet omvallen.",
    V + "start_opnieuw": "Fiet-fiew! Daar staat hij weer. Ik klok je rondje, njeg.",
    V + "klaar": "Wat een rit! Je bent nu een echte Sausloper-vriend. Hier is een zadel, en wat pindascheutjes voor onderweg. De wilde "
                 "lopers op de sauszee durven nu ook naar je toe.",
    V + "na": "Njeg, daar is mijn beste leerling! Zin in een rondje door de sausbak? Ik klok je tijd.",
    V + "uitleg": "Zoek een wilde Sausloper op de frituursauszee en voer hem pindascheutjes tot hij hartjes geeft. Zadel erop, stok in je "
                  "hand en rijden maar. Sluip en klik om hem te laten wachten. En laat hem niet te lang op het droge staan!",
    "quest.guhs.sausdieren.hint.lok": "lok een Sausloper met de pindasaus aan een stok naar de Verzorger-guh",
    "quest.guhs.sausdieren.hint.gelokt": "vraag de Verzorger-guh wat een Sausloper lust",
    "quest.guhs.sausdieren.hint.voer": "voer een Sausloper van de stal drie pindascheutjes",
    "quest.guhs.sausdieren.hint.nog_voer": "nog een pindascheutje voor de Sausloper",
    "quest.guhs.sausdieren.hint.vertrouwt": "vraag de Verzorger-guh om je proefrit",
    "quest.guhs.sausdieren.hint.proefrit": "stap op bij de steiger, houd de stok vast en rijd door de vier poortjes",
    "quest.guhs.sausdieren.hint.terug": "haal je zadel op bij de Verzorger-guh",
    "quest.guhs.sausdieren.hint.klaar": "tem een wilde Sausloper op de frituursauszee met pindascheutjes",
}

PAGINAS = {
    "sausloper": ("Sausloper", "Ongewoon (de frituursauszee van de Guhbarbecuether)",
                  "Een lieve guh op heel lange poten, met een kuif van frietjes. Hij stapt over de kaasfrituursaus alsof het een stoep is, "
                  "maar op het droge bibbert hij van de kou. Na de les van de Verzorger-guh in de Sausloper-stal tem je er zelf een met "
                  "pindascheutjes. Zadel erop, pindasaus aan een stok in je hand, en hij loopt waar jij heen kijkt."),
    "sausblubje": ("Sausblubje", "Gewoon (stuitert rond in de Guhbarbecuether, vooral in de Rookdelta)",
                   "Een stuiterend blubje saus met een korstje. Geef het een knuffel of een kaasknabbel en het splitst van blijdschap in "
                   "twee kleinere blubjes. Daarbij laat het blubroom achter. Een klein blubje groeit van drie knabbels weer een maatje, "
                   "en past precies in een glazen flesje."),
}

ADV = [  # name, parent, icon, frame, title, description
    ("sausdieren_stal", "binnen", "minecraft:saddle", "goal", "Sausloper-vriend",
     "Leer bij de Verzorger-guh in de Sausloper-stal lokken, voeren en rijden"),
    ("sausdieren_eigen_loper", "sausdieren_stal", "guhs:sausdieren_pindasaus_stok", "task", "Lange poten, klein hartje",
     "Tem een wilde Sausloper op de frituursauszee"),
    ("sausdieren_snel", "sausdieren_stal", "guhs:pindascheutjes", "challenge", "Sausracer",
     "Rijd het rondje door de sausbak in dertig seconden of minder"),
    ("sausdieren_blubroom", "binnen", "guhs:blubroom", "task", "Blub blub",
     "Knuffel of voer een Sausblubje tot het splitst en blubroom achterlaat"),
]
VERBORGEN = ["sausdieren_gereden", "sausdieren_geknuffeld", "sausdieren_gesplitst", "sausdieren_potje", "sausdieren_proefrit"]

STAPPEN = [  # (stapnaam, nu, waar): the questline "sausloper" in the Guhdex
    ("Praat met de Verzorger-guh", "Praat met de Verzorger-guh. Hij leert je alles over Sauslopers.", "De Sausloper-stal in de Guhbarbecuether"),
    ("Lok een Sausloper", "Houd de pindasaus aan een stok vast en lok een Sausloper naar de Verzorger-guh.", "De sausbak van de Sausloper-stal"),
    ("Win zijn vertrouwen", "Voer een Sausloper van de stal drie pindascheutjes.", "De sausbak van de Sausloper-stal"),
    ("Rijd een proefrondje", "Vraag de Verzorger-guh om je proefrit, stap op bij de steiger en rijd door de vier poortjes.",
     "De sausbak van de Sausloper-stal"),
    ("Haal je zadel op", "Ga terug naar de Verzorger-guh. Hij heeft een zadel voor je.", "De Sausloper-stal in de Guhbarbecuether"),
]


# =====================================================================================================================
# textures and item models
# =====================================================================================================================
BLUBROOM = ["................", "................", "......oooo......", "....ooLLLLoo....", "...oLLWWLLCCo...", "...oLWWLLCCCo...",
            "..oLLLLLCCcCCo..", "..oLLLCCCCccCo..", "..oCLCCCccCCCo..", "..oCCCCcCCCcdo..", "...oCCcCCCddo...", "...odCCCdddoo...",
            "....oodddooo....", "......oooo......", "................", "................"]
POTJE = ["................", ".....kkkkkk.....", ".....kKKKKk.....", "....gggggggg....", "....gb....bg....", "...gb......bg...",
         "...g..dddd..g...", "...g.doooodbg...", "...g.dEwCEwdg...", "...g.dEECEEdg...", "...g.dCCmCCdg...", "...g.dCCCCCdg...",
         "...gbdddddddg...", "....gggggggg....", "................", "................"]
PAL = {"o": (150, 78, 26, 255), "L": (255, 222, 150, 255), "W": (255, 250, 232, 255), "C": (250, 190, 96, 255), "c": (236, 160, 60, 255),
       "d": (214, 130, 44, 255), "k": (120, 80, 44, 255), "K": (176, 126, 76, 255), "g": (196, 220, 236, 255), "b": (232, 242, 250, 150),
       "E": (34, 22, 44, 255), "w": (255, 255, 255, 255), "m": (214, 96, 150, 255)}
STOK_KLODDER = {(9, 10): "h", (10, 10): "n", (12, 10): "n", (9, 11): "H", (10, 11): "h", (11, 11): "n", (12, 11): "s",
                (9, 12): "h", (10, 12): "n", (11, 12): "n", (12, 12): "D", (9, 13): "n", (10, 13): "h", (11, 13): "n", (12, 13): "D",
                (13, 13): "D", (10, 14): "D", (11, 14): "n", (12, 14): "D", (13, 14): None, (11, 15): "D"}
STOK_PAL = {"H": (244, 196, 120, 255), "h": (222, 158, 76, 255), "n": (186, 116, 44, 255), "D": (122, 72, 24, 255), "s": (73, 55, 44, 255)}


def spawn_ei(h, basis, vlekken):
    def tint(img, rgb):
        r, g, b = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
        out = img.copy()
        px = out.load()
        for y in range(out.height):
            for x in range(out.width):
                pr, pg, pb, pa = px[x, y]
                px[x, y] = (pr * r // 255, pg * g // 255, pb * b // 255, pa)
        return out
    return Image.alpha_composite(tint(h.vanilla("item/spawn_egg"), basis), tint(h.vanilla("item/spawn_egg_overlay"), vlekken))


def textures(h):
    h.save(spiesburcht_tex.icon(BLUBROOM, PAL), "item", "blubroom.png")
    h.item_model("blubroom")
    h.save(spiesburcht_tex.icon(POTJE, PAL), "item", "sausblubje_potje.png")
    h.item_model("sausblubje_potje")
    stok = h.vanilla("item/warped_fungus_on_a_stick").copy()
    px = stok.load()
    for (x, y), c in STOK_KLODDER.items():
        px[x, y] = STOK_PAL[c] if c else (0, 0, 0, 0)
    h.save(stok, "item", "sausdieren_pindasaus_stok.png")
    h.item_model("sausdieren_pindasaus_stok", parent="minecraft:item/handheld_rod")
    h.save(spiesburcht_tex.drankje(spiesburcht_tex.BROUWSELS["blubroom"]), "item", "sausdieren_stuiterdrankje.png")
    h.item_model("sausdieren_stuiterdrankje")
    # the spawn eggs (their own textures, so mc26.spawn_eggs leaves them alone)
    for ei, basis, vlekken in (("sausloper_spawn_egg", 0xF4966A, 0xFAD25A), ("sausblubje_spawn_egg", 0xF2A02E, 0x9A521E)):
        h.save(spawn_ei(h, basis, vlekken), "item", f"{ei}.png")
        h.item_model(ei)
    # the effect icon: a blob of sauce bouncing off the ground
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    ip = icon.load()
    for x in range(18):
        for y in range(18):
            if ((x - 8.5) / 5.0) ** 2 + ((y - 7.5) / 4.6) ** 2 <= 1:
                ip[x, y] = (244, 170, 60, 255) if y > 6 else (255, 208, 110, 255)
    for (x, y) in ((6, 6), (7, 5)):
        ip[x, y] = (255, 246, 214, 255)
    for (x, y) in ((6, 8), (11, 8)):
        ip[x, y] = (40, 24, 44, 255)
    for x in range(3, 15):
        ip[x, 16] = (120, 70, 30, 255)
    for (x, y) in ((2, 13), (3, 12), (15, 13), (14, 12), (8, 14), (9, 14)):
        ip[x, y] = (255, 232, 150, 255)
    h.save(icon, "mob_effect", "sausdieren_stuiter.png")
    # the Verzorger-guh: a caramel sitting guh (his name here; his straw hat, neckerchief and apron: sausdieren_modellen)
    bbq2.stub_npc(h, "verzorgerguh", "Verzorger-guh", 0.085, sat=2.6, val=0.96)
    modellen.verzorger(h)


# =====================================================================================================================
# data: loot, recipe, tags, spawns, sounds
# =====================================================================================================================
def data(h):
    D = h.D
    # sweet creatures: nothing to gain from hurting one (they can't be hurt anyway)
    for e in ("sausloper", "sausblubje"):
        h.w(f"{D}/loot_table/entities/{e}.json", {"type": "minecraft:entity", "pools": []})
    # the feed room's chest: a few handfuls of what a stable has lying around
    h.w(f"{D}/loot_table/chests/sausdieren_stal.json", {"type": "minecraft:chest", "pools": [{
        "rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            {"type": "minecraft:item", "name": item, "weight": wgt, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
            for item, wgt, lo, hi in (("guhs:pindascheutjes", 12, 2, 5), ("guhs:kaas_knabbels", 10, 3, 8), ("minecraft:wheat", 8, 2, 4),
                                      ("minecraft:glass_bottle", 6, 1, 2), ("guhs:pindasausplasje", 6, 1, 2), ("minecraft:lead", 4, 1, 1),
                                      ("minecraft:fishing_rod", 2, 1, 1), ("guhs:blubroom", 2, 1, 1))]}]})
    h.shapeless("sausdieren_pindasaus_stok", ["minecraft:fishing_rod", "guhs:pindasausplasje"], "guhs:sausdieren_pindasaus_stok")
    h.add_tag("guhs/tags/item/brouwsel/blubroom", ["guhs:blubroom"])
    h.add_tag("guhs/tags/item/sausdieren/sausloper_voer", ["guhs:pindascheutjes", "guhs:pindasausplasje"])
    h.add_tag("guhs/tags/item/sausdieren/sausblubje_voer", ["guhs:kaas_knabbels", "guhs:gefrituurde_kaasknabbels"])
    h.add_tag("guhs/tags/block/sausdieren/warm", ["guhs:kaasfrituursaus", "minecraft:lava", "guhs:gloeikool", "guhs:smeulkooltjes",
                                                  "minecraft:magma_block", "minecraft:campfire", "minecraft:soul_campfire", "minecraft:fire"])
    h.add_tag("minecraft/tags/entity_type/fall_damage_immune", ["guhs:sausblubje"])
    # (the vanilla saddle only goes on entity types of this tag)
    h.add_tag("minecraft/tags/entity_type/can_equip_saddle", ["guhs:sausloper"])
    # they spawn as creatures in every biome of the Guhbarbecuether (the Sausloper only where there is sauce: its spawn rule)
    for biome, onze in SPAWNS.items():
        def add(d, onze=onze):
            types = {t for t, _, _, _ in onze}
            oud = d.setdefault("spawners", {}).get("creature", [])
            d["spawners"]["creature"] = [e for e in oud if e["type"] not in types] + \
                [{"type": t, "weight": wgt, "minCount": lo, "maxCount": hi} for t, wgt, lo, hi in onze]
        h.patch_json(f"{D}/worldgen/biome/{biome}.json", add)

    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the stable
# =====================================================================================================================
def stal(h):
    s, problems = bouw.bouw(h)
    if problems:
        raise SystemExit("sausdieren: the Sausloper-stal is not right:\n  " + "\n  ".join(problems))
    s.save(STRUCTUUR)
    wereld.bbq_structuur(h, STRUCTUUR, soort="grot", titel="Sausloper-stal",
                         tooltip="De stal van de Sauslopers, met de Verzorger-guh en een proefrit door de sausbak (Guhbarbecuether)",
                         biomes=wereld.BBQ, salt=SALT, templates=[(STRUCTUUR, 1)], spacing=32, separation=12,
                         gegarandeerd=dict(sector=6, min=250, max=900), grootte=30, vlak=8, hoogte=12)

    # the pool says where the ground really is (sausdieren_bouw.midden): the yard lands on the cave floor and the terrain is
    # smoothed towards it, instead of towards the bottom of the template's foundation
    def grond(pool):
        for e in pool["elements"]:
            el = {"element_type": "guhs:grond_single_pool_element"}
            el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
            el["ground_level_delta"] = bouw.G + 1
            e["element"] = el
    h.patch_json(f"{h.D}/worldgen/template_pool/{STRUCTUUR}/start.json", grond)
    # the game test room: a floor of houtskoolsteen with a walled tub of sauce (8 x 8) on it and room to stand next to it
    t = h.Structure((24, 8, 16))
    for x in range(24):
        for z in range(16):
            t.set(x, 0, z, "guhs:houtskoolsteen")
    for x in range(2, 12):
        for z in range(3, 13):
            rand = x in (2, 11) or z in (3, 12)
            t.set(x, 1, z, "guhs:houtskoolsteen_stenen" if rand else "guhs:kaasfrituursaus", None if rand else {"level": "0"})
    t.save("sausdieren_test_kamer")
    # and a bare floor: the lap of a Verzorger-guh that sits in no stable is laid out around him
    t = h.Structure((26, 6, 18))
    for x in range(26):
        for z in range(18):
            t.set(x, 0, z, "guhs:houtskoolsteen")
    t.save("sausdieren_test_plein")


# =====================================================================================================================
# texts, advancements, the questline
# =====================================================================================================================
def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for event, text in SUBTITLES.items():
        h.lang(f"subtitles.guhs.{event}", text, text)
    for id, (naam, zeldzaamheid, info) in PAGINAS.items():
        bbq2.pagina(h, id, naam, zeldzaamheid, info)
    for name in VERBORGEN:
        bbq2.verborgen(h, name)
    for name, parent, icon, frame, titel, tekst in ADV:
        bbq2.zichtbaar(h, "barbecuether", name, parent, icon, frame, titel, tekst)
    verhaal_motor.verhaallijn(
        h, "sausloper", "De Sausloper-stal",
        "De Verzorger-guh leert je lokken, voeren en rijden. Daarna mag je zelf een wilde Sausloper temmen.", STAPPEN,
        klaar=("Je bent een Sausloper-vriend. Tem een wilde Sausloper op de frituursauszee, of rijd nog een rondje op tijd.",
               "De frituursauszee van de Guhbarbecuether"),
        kort={"0": "Praat met de Verzorger-guh", "1": "Lok een Sausloper naar de Verzorger-guh", "2": "Voer een Sausloper drie pindascheutjes",
              "3": "Rijd het proefrondje door de sausbak", "4": "Haal je zadel op bij de Verzorger-guh"})


# =====================================================================================================================
# self-check
# =====================================================================================================================
def java_plekken(h):
    """The template coordinates in feature/sausdieren/Stal.java: {name: [(x, y, z), ...]}."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "sausdieren", "Stal.java"), encoding="utf-8").read()
    uit = {}
    for m in re.finditer(r"public static final [\w<>]+ (\w+) = (.*?);", src, re.S):
        punten = [tuple(int(v) for v in p) for p in re.findall(r"new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", m.group(2))]
        if punten:
            uit[m.group(1)] = punten
    g = re.search(r"public static final int G = (\d+);", src)
    yaw = re.search(r"public static final float NPC_YAW = (-?[\d.]+)f;", src)
    return uit, int(g.group(1)) if g else None, float(yaw.group(1)) if yaw else None


def selfcheck(h):
    problems = modellen.check(h)
    # the geometry Java uses is the geometry of the template
    java, g, yaw = java_plekken(h)
    for naam, wat in bouw.PLEKKEN.items():
        py = [tuple(wat)] if isinstance(wat, tuple) else [tuple(p) for p in wat]
        if java.get(naam) != py:
            problems.append(f"Stal.java {naam} = {java.get(naam)}, the template has {py}")
    if g != bouw.G:
        problems.append(f"Stal.java G = {g}, the template has {bouw.G}")
    if yaw != bouw.NPC_YAW:
        problems.append(f"Stal.java NPC_YAW = {yaw}, the template has {bouw.NPC_YAW}")
    for key in list(LANG) + [f"subtitles.guhs.{e}" for e in SOUNDS] + ["structure.guhs.sausloper_stal", "entity.guhs.guh_npc.verzorgerguh"]:
        if key not in h.NL:
            problems.append(f"missing lang {key}")
    for key, text in LANG.items():
        if "hamster" in text.lower():
            problems.append(f"lore: {key}")
    for p in ([f"{h.A}/geckolib/models/entity/{e}.geo.json" for e in ("sausloper", "sausblubje", modellen.VERZORGER)]
              + [os.path.join(h.TEX, "entity", f"{t}.png") for t in ("sausloper", "sausloper_koud", "sausblubje", "sausblubje_glowmask", "npc_verzorgerguh")]
              + [os.path.join(h.TEX, "item", f"{t}.png") for t in ("blubroom", "sausblubje_potje", "sausdieren_pindasaus_stok", "sausdieren_stuiterdrankje",
                                                                   "sausloper_spawn_egg", "sausblubje_spawn_egg")]
              + [os.path.join(h.TEX, "mob_effect", "sausdieren_stuiter.png"), f"{h.D}/structure/{STRUCTUUR}.nbt", f"{h.D}/structure/sausdieren_test_kamer.nbt",
                 f"{h.D}/worldgen/structure/{STRUCTUUR}.json", f"{h.D}/worldgen/structure_set/{STRUCTUUR}_gegarandeerd.json",
                 f"{h.D}/advancement/quest/sausloper_stap_5.json"]):
        if not os.path.exists(p):
            problems.append(f"missing file {p}")
    sounds = json.load(open(f"{h.A}/sounds.json", encoding="utf-8"))
    for e in SOUNDS:
        if e not in sounds:
            problems.append(f"sounds.json misses {e}")
    for biome, onze in SPAWNS.items():
        d = json.load(open(f"{h.D}/worldgen/biome/{biome}.json", encoding="utf-8"))
        types = [e["type"] for e in d["spawners"].get("creature", [])]
        for t, _, _, _ in onze:
            if types.count(t) != 1:
                problems.append(f"biome {biome}: {t} is in the creature spawners {types.count(t)} times")
    if problems:
        raise SystemExit("sausdieren self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    textures(h)
    data(h)
    stal(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_barbecuether; nothing locked)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("sausdieren_stal", "De Sausloper-stal",
      "Ergens langs de frituursauszee staat een stal met een koperen dak en twee roze guh-oren. Daar woont de &6Verzorger-guh&r met zijn "
      "&6Sauslopers&r. Je &dSuperkompas&r wijst de weg (tab Barbecue).",
      "guhs:sausdieren_pindasaus_stok", [fq.structure(STRUCTUUR)], deps=["bbq_aan"], shape="hexagon")
    wereld.ftb_questlijn(fq, "sausdieren", "sausloper", [
        ("Pindasaus aan een stok", "Praat met de &6Verzorger-guh&r. Hij geeft je &6pindasaus aan een stok&r: daar loopt elke Sausloper "
                                   "vahoeg achteraan.", "guhs:sausdieren_pindasaus_stok"),
        ("Kom maar, langpoot!", "Houd de stok vast en lok een Sausloper uit de sausbak naar de Verzorger-guh. Zie je hem bibberen op het "
                                "droge? Koude pootjes, njeg.", "guhs:pindasausplasje"),
        ("Vriendschap gaat door de maag", "Voer een Sausloper van de stal drie &6pindascheutjes&r. De Verzorger-guh geeft je de eerste "
                                          "drie. Daarna vertrouwt hij je.", "guhs:pindascheutjes"),
        ("Het proefrondje", "Vraag om je proefrit. Er komt een gezadelde Sausloper naar de steiger, speciaal voor jou. Stap op, houd de "
                            "stok vast en rijd door de vier poortjes. Hij loopt waar jij heen kijkt.", "minecraft:saddle"),
        ("Sausloper-vriend", "Haal je &6zadel&r op bij de Verzorger-guh. Vanaf nu durven de wilde Sauslopers op de sauszee ook naar je toe.",
         "minecraft:saddle"),
    ], na=["sausdieren_stal"], eind=(("guhs:pindascheutjes", 4), ("guhs:kaas_knabbels", 8)))
    q("sausdieren_eigen_loper", "Lange poten, klein hartje",
      "Zoek een wilde &6Sausloper&r op de frituursauszee en voer hem pindascheutjes tot hij hartjes geeft. Zadel erop, stok in je hand: "
      "nu steek je de sauszee zo over. Sluip en klik om hem te laten wachten.",
      "guhs:sausloper_spawn_egg", [adv("guhs:barbecuether/sausdieren_eigen_loper")], rewards=(("guhs:pindascheutjes", 6),),
      deps=["sausdieren_sausloper_5"], xp=100)
    q("sausdieren_loper_dex", "Wie stapt daar door de saus?",
      "Kijk een &6Sausloper&r van dichtbij aan: dan staat hij in je &dGuhdex&r.", "guhs:sausloper_spawn_egg", [adv("seen_sausloper")],
      deps=["sausdieren_stal"])
    q("sausdieren_snel", "Sausracer",
      "Na de les klokt de Verzorger-guh elk rondje. Haal de finish in &6dertig seconden&r of minder. Tip: rechtsklik met de stok voor "
      "een sprintje.", "guhs:pindascheutjes", [adv("guhs:barbecuether/sausdieren_snel")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=["sausdieren_sausloper_5"], shape="octagon", xp=150)
    # --- the Sausblubjes
    q("sausdieren_blubje_dex", "Blub?",
      "In de Guhbarbecuether stuiteren &6Sausblubjes&r rond: blubjes saus met een korstje en een snoet. Kijk er een van dichtbij aan "
      "voor je &dGuhdex&r. Ze doen niks, ze blubben alleen.", "guhs:sausblubje_spawn_egg", [adv("seen_sausblubje")], deps=["bbq_aan"],
      shape="hexagon")
    q("sausdieren_knuffel", "Een knuffel te veel",
      "Geef een groot Sausblubje een knuffel (rechtsklik met een lege hand) of een kaasknabbel. Het splitst van blijdschap in twee "
      "kleinere blubjes.", "guhs:kaas_knabbels", [adv("sausdieren_gesplitst")], deps=["sausdieren_blubje_dex"])
    q("sausdieren_blubroom", "Blubroom",
      "Elke keer dat een Sausblubje splitst blijft er een klodder &6blubroom&r liggen. Een klein blubje groeit van drie knabbels weer "
      "een maatje, dus een hokje blubjes blijft blubben.", "guhs:blubroom", [item("guhs:blubroom", 3)], deps=["sausdieren_knuffel"])
    q("sausdieren_potje", "Blubje to go",
      "Een &6klein&r Sausblubje past precies in een glazen flesje (rechtsklik). Zo neem je het mee naar huis. De uitvinder-guh weet "
      "er vast ook raad mee...", "guhs:sausblubje_potje", [item("guhs:sausblubje_potje")], deps=["sausdieren_knuffel"])
    q("sausdieren_stuiterdrankje", "Boing!",
      "Roer blubroom door een pan kaasbouillon in de &6Guhbrouwketel&r en tap een &6Stuiterdrankje&r. Drie minuten lang val je niet "
      "meer: je stuitert. Sluipen is landen.", "guhs:sausdieren_stuiterdrankje", [item("guhs:sausdieren_stuiterdrankje")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["sausdieren_blubroom"], shape="gear", xp=100)
