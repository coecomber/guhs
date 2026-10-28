"""
Guhpixel slice "gezel" (1.8.0, the Gezelschapshoek; Java: feature/guhpixel/gezel): Kussengevecht (an arena game),
Wie is het (guh-editie) and Guh op een rij (board games on a screen), on three new lobby anchors in the south-east
(guhpixel_lobby_bouw.py ANKERS SPEL_KUSSEN / SPEL_WIEISHET / SPEL_OPEENRIJ; the lobby template goes to VERSIE 4).

This module makes:
  - the arena template guhs:guhpixel/kussengevecht (31 x 16 x 31): a giant bed (top at local y 8, x and z 8..22) above a
    cushion pit with walls; KussenSessie mirrors the numbers (BED_Y, BED_MIN, BED_MAX, MAAT)
  - the items kussengevecht_kussen (the pillow you hit with), opeenrij_schijfje and wieishet_kaartje (icons on the
    Highscores page): textures, models, 26.1 item definitions
  - the three NPC kinds (kussenbaas_guh, wieishet_guh, opeenrij_guh): a recoloured sitting guh each
  - the hidden advancements quest/px_kussengevecht_gewonnen, px_wieishet_gewonnen, px_opeenrij_gewonnen
  - every text (NL + EN; the English also in tools/lang/en/c216_guhpixel_gezel.json) and the FTB section "Gezelschapshoek"
  - the lobby again (guhpixel_lobby_bouw.build) with its sign texts
Standalone: PYTHONPATH=tools python tools/features/guhpixel_gezel.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

ARENA = "guhpixel/kussengevecht"
MAAT = (31, 16, 31)
BED_Y, BED_MIN, BED_MAX = 8, 8, 22
ITEMS = ["kussengevecht_kussen", "opeenrij_schijfje", "wieishet_kaartje"]
ADVS = ["px_kussengevecht_gewonnen", "px_wieishet_gewonnen", "px_opeenrij_gewonnen"]
NPCS = {"kussenbaas_guh": ("Kussenbaas", "Pillow Boss", 0.92, 0.55), "wieishet_guh": ("Spelletjesguh", "Games Guh", 0.55, 0.55),
        "opeenrij_guh": ("Rijtjesguh", "Rows Guh", 0.14, 0.7)}

# key: (nl, en)
TEKSTEN = {
    # items
    "item.guhs.kussengevecht_kussen": ("Kussengevecht-kussen", "Pillow-fight pillow"),
    "item.guhs.kussengevecht_kussen.lore": ("Linksklik op iemand op het bed: plof! Doet nooit pijn, alleen vliegen.",
                                            "Left-click someone on the bed: poof! Never hurts, only flying."),
    "item.guhs.opeenrij_schijfje": ("Guh-op-een-rij-schijfje", "Guh-in-a-row disc"),
    "item.guhs.opeenrij_schijfje.lore": ("Een roze guh aan de ene kant, een blauwe Blub aan de andere. Altijd raak.",
                                         "A pink guh on one side, a blue Blub on the other. Always a hit."),
    "item.guhs.wieishet_kaartje": ("Wie-is-het-kaartje", "Guess-who card"),
    "item.guhs.wieishet_kaartje.lore": ("Er staat een guh op. Of is het een andere guh? Njeg.", "There is a guh on it. Or is it another guh? Njeg."),
    # NPCs and their lines in the lobby
    "gui.guhs.gezel.npc.wieishet": ("Wie is het? Tegen een guh of een vriend", "Guess who? Against a guh or a friend"),
    "gui.guhs.gezel.npc.opeenrij": ("Vier op een rij! Tegen een guh of een vriend", "Four in a row! Against a guh or a friend"),
    "quest.guhs.gezel.kussenbaas.hallo": ("Pyjama aan, kussen in je pootjes en hup, op het grote bed! Wie het langst blijft staan, wint. Wie eraf valt, "
                                          "landt zacht in de kussenkuil. Spelers die vlakbij staan doen mee, en mijn guh-vriendjes vullen de rest aan. Njeg!",
                                          "Pyjamas on, pillow in your paws and up on the big bed! Whoever stays standing longest wins. Whoever falls off lands "
                                          "softly in the cushion pit. Players standing close by join in, and my guh friends fill up the rest. Njeg!"),
    "quest.guhs.gezel.kussenbaas.optie.spelen": ("Kussengevecht!", "Pillow fight!"),
    "quest.guhs.gezel.kussenbaas.optie.uitleg": ("Hoe werkt het?", "How does it work?"),
    "quest.guhs.gezel.kussenbaas.uitleg": ("Linksklik met je kussen op een ander: plof, die vliegt een stukje weg. Het doet nooit pijn. Val je van het bed, "
                                           "dan lig je in de kussenkuil en kijk je toe. De laatste op het bed wint (na twee minuten wint iedereen die er nog "
                                           "staat). Je krijgt 10 punten voor iedereen die jij eraf duwt, en 50 als je wint. Vrienden doen mee als ze binnen "
                                           "%s blokken van mij staan als jij begint.",
                                           "Left-click someone with your pillow: poof, off they fly a little. It never hurts. Fall off the bed and you lie in "
                                           "the cushion pit and watch. The last one on the bed wins (after two minutes everybody still standing wins). You get "
                                           "10 points for everyone you push off, and 50 for winning. Friends join in when they stand within %s blocks of me "
                                           "when you start."),
    # the Kussengevecht
    "gui.guhs.gezel.kussen.jasje": ("Pyjamajasje", "Pyjama jacket"),
    "gui.guhs.gezel.kussen.broek": ("Pyjamabroek", "Pyjama trousers"),
    "gui.guhs.gezel.kussen.uitleg": ("Kussengevecht! Linksklik met je kussen op een ander. Laatste op het bed wint!",
                                     "Pillow fight! Left-click someone with your pillow. Last one on the bed wins!"),
    "gui.guhs.gezel.kussen.aftellen": ("Kussens klaar... %s", "Pillows ready... %s"),
    "gui.guhs.gezel.kussen.start": ("PLOF! Kussengevecht!", "POOF! Pillow fight!"),
    "gui.guhs.gezel.kussen.af": ("%s ligt in de kussenkuil!", "%s is in the cushion pit!"),
    "gui.guhs.gezel.kussen.jij_af": ("Plof! Je ligt in de kussenkuil. Lekker zacht, njeg.", "Poof! You are in the cushion pit. Nice and soft, njeg."),
    "gui.guhs.gezel.kussen.winnaar": ("%s is de Kussenkoning!", "%s is the Pillow Champion!"),
    "gui.guhs.gezel.kussen.niemand": ("Iedereen ligt in de kuil. Gelijkspel, en een dutje.", "Everybody is in the pit. A draw, and a nap."),
    "gui.guhs.gezel.kussen.gewonnen": ("Gewonnen! %s punten. Vahoeg!", "You won! %s points. Vahoeg!"),
    "gui.guhs.gezel.kussen.punten": ("%s punten. Volgende keer harder kussen!", "%s points. Pillow harder next time!"),
    "gui.guhs.gezel.muntjes": ("+%s muntjes", "+%s muntjes"),
    # the board game screen
    "gui.guhs.gezel.titel.opeenrij": ("Guh op een rij", "Guh in a row"),
    "gui.guhs.gezel.titel.wieishet": ("Wie is het? (guh-editie)", "Guess who? (guh edition)"),
    "gui.guhs.gezel.uitleg.opeenrij": ("Laat om de beurt een schijfje in het bord vallen.", "Take turns dropping a disc into the board."),
    "gui.guhs.gezel.uitleg2.opeenrij": ("Vier op een rij wint: guhs tegen Blubs!", "Four in a row wins: guhs against Blubs!"),
    "gui.guhs.gezel.uitleg.wieishet": ("Stel ja/nee-vragen over de guh van de ander.", "Ask yes/no questions about the other one's guh."),
    "gui.guhs.gezel.uitleg2.wieishet": ("Wie het eerst de goede guh raadt, wint!", "Whoever first guesses the right guh wins!"),
    "gui.guhs.gezel.gewonnen": ("Al %s keer gewonnen", "Won %s times so far"),
    "gui.guhs.gezel.knop.bot.0": ("Tegen een guh: makkelijk", "Against a guh: easy"),
    "gui.guhs.gezel.knop.bot.1": ("Tegen een guh: normaal", "Against a guh: normal"),
    "gui.guhs.gezel.knop.bot.2": ("Tegen een guh: lastig", "Against a guh: hard"),
    "gui.guhs.gezel.knop.zoek": ("Zoek een tegenstander", "Find an opponent"),
    "gui.guhs.gezel.knop.sluit": ("Sluiten", "Close"),
    "gui.guhs.gezel.knop.annuleer": ("Toch niet", "Never mind"),
    "gui.guhs.gezel.knop.opnieuw": ("Nog een potje", "Another round"),
    "gui.guhs.gezel.knop.stop": ("Stoppen", "Stop"),
    "gui.guhs.gezel.knop.vraag": ("Vraag het!", "Ask!"),
    "gui.guhs.gezel.knop.raden": ("Ik weet het! Raden", "I know it! Guess"),
    "gui.guhs.gezel.knop.niet_raden": ("Toch niet raden", "Don't guess yet"),
    "gui.guhs.gezel.bot.0": ("Dommelguh", "Dozy guh"),
    "gui.guhs.gezel.bot.1": ("Spelguh", "Game guh"),
    "gui.guhs.gezel.bot.2": ("Slimme Guh", "Clever guh"),
    "gui.guhs.gezel.wacht": ("Even wachten op een tegenstander...", "Waiting for an opponent..."),
    "gui.guhs.gezel.wacht2": ("Vraag een vriend om ook 'Zoek een tegenstander' te kiezen.", "Ask a friend to choose 'Find an opponent' too."),
    "gui.guhs.gezel.tegen": ("Tegen: %s", "Against: %s"),
    "gui.guhs.gezel.jouw_beurt": ("Jouw beurt!", "Your turn!"),
    "gui.guhs.gezel.raad_nu": ("Klik op de guh die je raadt.", "Click the guh you guess."),
    "gui.guhs.gezel.wacht_beurt": ("%s denkt na...", "%s is thinking..."),
    "gui.guhs.gezel.ander_open": ("De ander heeft nog %s guhs", "The other one has %s guhs left"),
    "gui.guhs.gezel.jouw_guh": ("Jouw guh:", "Your guh:"),
    "gui.guhs.gezel.zijn_guh": ("De ander had: %s", "The other one had: %s"),
    "gui.guhs.gezel.uitslag.gewonnen": ("Gewonnen! Vahoeg!", "You won! Vahoeg!"),
    "gui.guhs.gezel.uitslag.verloren": ("Verloren. Njeg...", "You lost. Njeg..."),
    "gui.guhs.gezel.uitslag.gelijk": ("Gelijkspel!", "A draw!"),
    "gui.guhs.gezel.uitslag.gestopt": ("Het spel is gestopt.", "The game stopped."),
    "gui.guhs.gezel.log.begin": ("%s mag beginnen.", "%s may begin."),
    "gui.guhs.gezel.log.ja": ("%s: %s Ja!", "%s: %s Yes!"),
    "gui.guhs.gezel.log.nee": ("%s: %s Nee.", "%s: %s No."),
    "gui.guhs.gezel.log.goed": ("%s raadt %s. Goed!", "%s guesses %s. Right!"),
    "gui.guhs.gezel.log.fout": ("%s raadt %s. Fout, het was %s!", "%s guesses %s. Wrong, it was %s!"),
    "gui.guhs.gezel.log.gelijk": ("Het bord is vol: gelijkspel.", "The board is full: a draw."),
    "gui.guhs.gezel.log.winnaar": ("%s wint!", "%s wins!"),
    "gui.guhs.gezel.log.weg": ("%s is weggegaan.", "%s has left."),
    # Wie is het: the questions and their words
    "gui.guhs.gezel.vraag.kleur": ("Is hij %s?", "Is it %s?"),
    "gui.guhs.gezel.vraag.hoed": ("Draagt hij een hoed?", "Does it wear a hat?"),
    "gui.guhs.gezel.vraag.hoedsoort": ("Draagt hij een %s?", "Does it wear a %s?"),
    "gui.guhs.gezel.vraag.bril": ("Heeft hij een bril?", "Does it have glasses?"),
    "gui.guhs.gezel.vraag.strik": ("Heeft hij een strikje?", "Does it have a bow?"),
    "gui.guhs.gezel.vraag.aard": ("Is hij %s?", "Is it %s?"),
    "gui.guhs.gezel.kleur.roze": ("roze", "pink"), "gui.guhs.gezel.kleur.blauw": ("blauw", "blue"),
    "gui.guhs.gezel.kleur.geel": ("geel", "yellow"), "gui.guhs.gezel.kleur.groen": ("groen", "green"),
    "gui.guhs.gezel.kleur.paars": ("paars", "purple"), "gui.guhs.gezel.kleur.wit": ("wit", "white"),
    "gui.guhs.gezel.kleur.bruin": ("bruin", "brown"), "gui.guhs.gezel.kleur.mint": ("mintgroen", "mint green"),
    "gui.guhs.gezel.hoed.geen": ("geen hoed", "no hat"), "gui.guhs.gezel.hoed.slaapmuts": ("slaapmuts", "nightcap"),
    "gui.guhs.gezel.hoed.kroontje": ("kroontje", "little crown"), "gui.guhs.gezel.hoed.koksmuts": ("koksmuts", "chef's hat"),
    "gui.guhs.gezel.hoed.strohoed": ("strohoed", "straw hat"),
    "gui.guhs.gezel.aard.vadsig": ("vadsig", "vadsig"), "gui.guhs.gezel.aard.lief": ("lief", "sweet"),
    "gui.guhs.gezel.aard.stoer": ("stoer", "tough"), "gui.guhs.gezel.aard.slaperig": ("slaperig", "sleepy"),
    # the Highscores page
    "gui.guhs.highscores.game.kussengevecht": ("Kussengevecht", "Pillow fight"),
    "gui.guhs.highscores.game.wieishet": ("Wie is het (gewonnen)", "Guess who (wins)"),
    "gui.guhs.highscores.game.opeenrij": ("Guh op een rij (gewonnen)", "Guh in a row (wins)"),
}


# =====================================================================================================================
# the arena
# =====================================================================================================================
def arena(h):
    W, H, D = MAAT
    s = h.Structure(MAAT)
    m = "minecraft:"
    # the cushion pit: a soft floor, low walls
    for x in range(W):
        for z in range(D):
            s.set(x, 0, z, "guhs:pluiswolblok")
            rand = x in (0, W - 1) or z in (0, D - 1)
            if rand:
                for y in range(1, 6):
                    s.set(x, y, z, m + ("pink_wool" if (x + z + y) % 2 else "white_wool"))
            else:
                s.set(x, 1, z, m + ("pink_carpet" if (x // 2 + z // 2) % 2 else "white_carpet"))
    # the bed: legs, a wooden frame, a quilt top, a headboard with a guh face, two big pillows
    for (x, z) in ((BED_MIN, BED_MIN), (BED_MAX, BED_MIN), (BED_MIN, BED_MAX), (BED_MAX, BED_MAX)):
        for y in range(1, BED_Y - 1):
            s.set(x, y, z, m + "stripped_cherry_log", {"axis": "y"})
    for x in range(BED_MIN, BED_MAX + 1):
        for z in range(BED_MIN, BED_MAX + 1):
            s.set(x, BED_Y - 1, z, m + "cherry_planks")
            kleur = "pink_wool" if ((x - BED_MIN) // 3 + (z - BED_MIN) // 3) % 2 == 0 else "white_wool"
            if x in (BED_MIN, BED_MAX) or z in (BED_MIN, BED_MAX):
                kleur = "magenta_wool"
            s.set(x, BED_Y, z, m + kleur)
    for x in range(BED_MIN, BED_MAX + 1):          # the headboard (north)
        for y in range(BED_Y + 1, BED_Y + 5):
            s.set(x, y, BED_MIN - 1, m + ("cherry_planks" if y < BED_Y + 4 else "pink_wool"))
    hx = (BED_MIN + BED_MAX) // 2
    for dx in (-5, -4, 4, 5):                       # guh ears on the headboard
        s.set(hx + dx, BED_Y + 5, BED_MIN - 1, m + "pink_wool")
    for dx in (-2, 2):                              # eyes and a mouth
        s.set(hx + dx, BED_Y + 3, BED_MIN - 1, m + "black_wool")
    s.set(hx, BED_Y + 2, BED_MIN - 1, m + "magenta_wool")
    for x0 in (BED_MIN + 2, BED_MAX - 4):          # two big pillows against the headboard
        for x in range(x0, x0 + 3):
            s.set(x, BED_Y + 1, BED_MIN + 1, m + "white_wool")
    # lights on the walls of the pit
    for x in range(4, W - 2, 6):
        for z in (1, D - 2):
            s.set(x, 4, z, m + "pearlescent_froglight", {"axis": "y"})
    for z in range(4, D - 2, 6):
        for x in (1, W - 2):
            s.set(x, 4, z, m + "pearlescent_froglight", {"axis": "y"})
    s.save(ARENA)
    return s


# =====================================================================================================================
# items, NPCs, advancements
# =====================================================================================================================
def _icoon(naam):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if naam == "kussengevecht_kussen":
        d.rounded_rectangle((1, 4, 14, 12), 3, fill=(247, 198, 218, 255), outline=(214, 120, 160, 255))
        d.line((4, 8, 11, 8), fill=(255, 235, 244, 255))
        for (x, y) in ((1, 4), (14, 4), (1, 12), (14, 12)):
            d.point((x, y), fill=(214, 120, 160, 255))
    elif naam == "opeenrij_schijfje":
        d.ellipse((1, 2, 14, 15), fill=(244, 166, 200, 255), outline=(200, 100, 150, 255))
        d.rectangle((2, 1, 4, 4), fill=(229, 138, 176, 255))
        d.rectangle((11, 1, 13, 4), fill=(229, 138, 176, 255))
        d.point((5, 8), fill=(32, 16, 24, 255))
        d.point((10, 8), fill=(32, 16, 24, 255))
        d.point((7, 11), fill=(176, 64, 106, 255))
        d.point((8, 11), fill=(176, 64, 106, 255))
    else:
        d.rounded_rectangle((2, 1, 13, 15), 2, fill=(255, 246, 238, 255), outline=(150, 110, 130, 255))
        d.rectangle((4, 4, 11, 11), fill=(140, 184, 240, 255))
        d.rectangle((4, 3, 5, 4), fill=(140, 184, 240, 255))
        d.rectangle((10, 3, 11, 4), fill=(140, 184, 240, 255))
        d.point((6, 7), fill=(32, 16, 24, 255))
        d.point((9, 7), fill=(32, 16, 24, 255))
        d.text((6, 9), "?", fill=(120, 60, 90, 255))
    return img


def items(h):
    for n in ITEMS:
        h.save(_icoon(n), "item", n + ".png")
        h.w(f"{h.A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{n}"}})
        h.w(f"{h.A}/items/{n}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}})


def npcs(h):
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    for kind, (_nl, _en, hue, sat) in NPCS.items():
        img = h.recolour(src, hue=hue, sat=sat, val=1.0, only=h.pinkish).convert("RGBA")
        h.save(img, "entity", f"npc_{kind}.png")


def advancements(h):
    for a in ADVS:
        h.w(f"{h.D}/advancement/quest/{a}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def alle_teksten():
    t = dict(TEKSTEN)
    for kind, (nl, en, _h, _s) in NPCS.items():
        t[f"entity.guhs.guh_npc.{kind}"] = (nl, en)
    return t


def build(h):
    arena(h)
    items(h)
    npcs(h)
    advancements(h)
    for k, (nl, en) in alle_teksten().items():
        h.lang(k, en, nl)


# =====================================================================================================================
# FTB quests (chapter Guhpixel, section "Gezelschapshoek"; no locking, knabbel rewards)
# =====================================================================================================================
def ftb(fq):
    q, adv = fq.q, fq.adv
    q("px_kussengevecht_gewonnen", "Kussenkoning", "Bij de &dKussenbaas&r in de Gezelschapshoek van de Guhpixel-lobby begint een "
      "&dkussengevecht&r op een reuzenbed. Linksklik met je kussen: plof! Wie van het bed valt, ligt in de kussenkuil. Blijf als laatste "
      "staan. Neem vrienden mee: wie vlakbij staat doet mee.", "guhs:kussengevecht_kussen", [adv("px_kussengevecht_gewonnen")],
      rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("px_wieishet_gewonnen", "Wie is het? Jij!", "De &bSpelletjesguh&r speelt &bWie is het&r, de guh-editie. Stel ja/nee-vragen "
      "(Is hij roze? Draagt hij een slaapmuts?) en raad de guh van de ander. Tegen een guh-bot of tegen een vriend.",
      "guhs:wieishet_kaartje", [adv("px_wieishet_gewonnen")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("px_opeenrij_gewonnen", "Vier op een rij", "De &eRijtjesguh&r heeft een staand bord met guh- en Blub-schijfjes. Krijg er vier op "
      "een rij: recht, schuin of omhoog. De Slimme Guh is echt slim, njeg.", "guhs:opeenrij_schijfje", [adv("px_opeenrij_gewonnen")],
      rewards=(("guhs:kaas_knabbels", 8),), xp=50)


FTB_EN = {
    "px_kussengevecht_gewonnen": ("Pillow champion", "With the &dPillow Boss&r in the Gezelschapshoek of the Guhpixel lobby a &dpillow fight&r "
                                  "starts on a giant bed. Left-click with your pillow: poof! Whoever falls off the bed lies in the cushion pit. "
                                  "Be the last one standing. Bring friends: whoever stands close by joins in."),
    "px_wieishet_gewonnen": ("Guess who? You!", "The &bGames Guh&r plays &bGuess who&r, the guh edition. Ask yes/no questions (Is it pink? "
                             "Does it wear a nightcap?) and guess the other one's guh. Against a guh bot or against a friend."),
    "px_opeenrij_gewonnen": ("Four in a row", "The &eRows Guh&r has a standing board with guh and Blub discs. Get four in a row: straight, "
                             "diagonal or up. The Clever Guh really is clever, njeg."),
}


# the English of the Gezelschapshoek signs in the lobby (guhpixel_lobby_bouw.GEZEL)
SIGN_EN = {"~ Kussengevecht ~": "~ Pillow fight ~", "Wie blijft er": "Who stays", "op het bed?": "on the bed?", "Zachte val!": "Soft landing!",
           "~ Wie is het ~": "~ Guess who ~", "Raad de guh": "Guess the guh", "van de ander.": "of the other.",
           "Draagt hij een hoed?": "Wearing a hat?", "~ Guh op een rij ~": "~ Guh in a row ~", "Vier guhs op": "Four guhs in",
           "een rij wint.": "a row win.", "Of vier Blubs.": "Or four Blubs."}


# =====================================================================================================================
def selfcheck():
    """The Java side must use the same arena numbers."""
    problems = []
    pad = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "gezel", "KussenSessie.java")
    if os.path.exists(pad):
        src = open(pad, encoding="utf-8").read()
        for v in (f"new Vec3i({MAAT[0]}, {MAAT[1]}, {MAAT[2]})", f"BED_Y = {BED_Y}, BED_MIN = {BED_MIN}, BED_MAX = {BED_MAX}"):
            if v not in src:
                problems.append(f"KussenSessie.java does not have '{v}'")
    if problems:
        raise SystemExit("guhpixel_gezel self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    import sign_text
    from features import guhpixel_lobby_bouw as bouw
    selfcheck()
    build(h)
    s = bouw.build(h)            # the lobby with the Gezelschapshoek (VERSIE 4)
    teksten = alle_teksten()
    eigen = {regel for (_k, regels) in bouw.GEZEL.values() for regel in regels}
    for k, v in sign_text.TEXTS.items():
        if k.startswith("sign.guhs.lobby.") and k not in teksten and v in eigen:
            teksten[k] = (v, SIGN_EN.get(v, v))
    en_pad = os.path.join("tools", "lang", "en", "c216_guhpixel_gezel.json")
    en = {k: e for k, (n, e) in teksten.items()}
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    ftb_en = {"ftb.guhs_guhpixel.section.px_gezel": "Gezelschapshoek (board games and pillow fights)"}
    for q, (titel, desc) in FTB_EN.items():
        ftb_en[f"ftb.guhs_guhpixel.q.{q}.title"] = titel
        ftb_en[f"ftb.guhs_guhpixel.q.{q}.desc"] = desc
    with open(os.path.join("tools", "lang", "en", "c216_guhpixel_gezel_ftb.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ftb_en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        for k, v in teksten.items():
            d[k] = v[i]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"guhpixel_gezel: arena {MAAT}, {len(ITEMS)} items, {len(NPCS)} NPCs, lobby v{bouw.VERSIE}, {len(teksten)} texts")


if __name__ == "__main__":
    _direct()
