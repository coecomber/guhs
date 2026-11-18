"""
1.10.9 "Guhwinter", slice spellen: the games of the winter plaza and the pepernoten shop. Java: feature/guhwinter/spellen.

  build(h)   the plaza blocks (arcade cabinet, highscore board, the shop's stall, the oliebollen stall, the skating lap's start
             sign, the hidden pepernoot bag) and the shop's goods (warme chocomel, chocoladeletters A-Z, winter skates, a kerstmuts
             and a Sint-mijter for on your head, kerstlampjes): textures, models, blockstates, item definitions, loot, the melt
             tags guhs:guhwinter_smelt / guhs:guhwinter_smelt_blok, and the texts (Dutch in nl_nl, English in
             tools/lang/en/c509_guhwinter_spellen.json, written here too).

Standalone (from the project root): PYTHONPATH=tools python tools/features/guhwinter_spellen.py
"""
import json
import os

from PIL import Image, ImageDraw

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")
EN_FILE = os.path.join("tools", "lang", "en", "c509_guhwinter_spellen.json")
T = "gui.guhs.guhwinter_spellen."

# ---------------------------------------------------------------------------------------------------------------------
# texts: key -> (Dutch, English)
# ---------------------------------------------------------------------------------------------------------------------
TEKSTEN = {
    # blocks and items
    "block.guhs.guhwinter_arcadekast": ("Pepernotenrace-kast", "Pepernoten Race Cabinet"),
    "block.guhs.guhwinter_highscorebord": ("Winterhighscorebord", "Winter Highscore Board"),
    "block.guhs.guhwinter_winkelkraam": ("Pepernotenwinkeltje", "Pepernoten Shop Stall"),
    "block.guhs.guhwinter_bakkraam": ("Oliebollenbakkraam", "Oliebollen Baking Stall"),
    "block.guhs.guhwinter_schaatsbord": ("Startbordje schaatsrondje", "Skating Lap Start Sign"),
    "block.guhs.guhwinter_pepernootzakje": ("Verstopt pepernotenzakje", "Hidden Pepernoten Bag"),
    "block.guhs.guhwinter_kerstlampjes": ("Kerstlampjes", "Christmas Lights"),
    "block.guhs.guhwinter_kerstlampjes.lore": ("Een snoer twinkelende lampjes voor aan de muur. Smelt op 3 januari, net als de sneeuw.",
                                               "A string of twinkling lights for your wall. Melts on 3 January, like the snow."),
    "item.guhs.guhwinter_chocomel": ("Warme chocomel", "Hot Chocomel"),
    "item.guhs.guhwinter_chocomel.lore": ("Met een toef slagroom. Warm van binnen: even sneller, en een beetje beter.",
                                          "With a dollop of whipped cream. Warm inside: a little faster for a while, and a bit better."),
    "item.guhs.guhwinter_chocoladeletter": ("Chocoladeletter", "Chocolate Letter"),
    "item.guhs.guhwinter_chocoladeletter.met": ("Chocoladeletter %s", "Chocolate Letter %s"),
    "item.guhs.guhwinter_chocoladeletter.lore": ("Melkchocolade in de vorm van je beginletter. Eerst de pootjes eraf, njeg.",
                                                 "Milk chocolate in the shape of your first letter. Bite the little feet off first, njeg."),
    "item.guhs.guhwinter_schaatsen": ("Winterschaatsen", "Winter Skates"),
    "item.guhs.guhwinter_schaatsen.lore": ("Aan je voeten of in je hand: op het ijs glij je een stuk sneller.",
                                           "On your feet or in your hand: on the ice you glide a lot faster."),
    "item.guhs.guhwinter_schaatsen.leen": ("Geleend voor één schaatsrondje", "Borrowed for one skating lap"),
    "item.guhs.guhwinter_kerstmuts": ("Kerstmuts", "Christmas Hat"),
    "item.guhs.guhwinter_mijter": ("Sint-mijter", "Sint's Mitre"),
    # general
    T + "geen_pepernoot": ("(De pepernoten zelf komen er nog aan: kern is er nog niet.)", "(The pepernoten themselves are still on their way: kern isn't here yet.)"),
    T + "voorbij": ("Het winterfeest is voorbij. Tot het volgende feest, vahoeg!", "The winter party is over. See you at the next party, vahoeg!"),
    T + "bezig": ("Je bent nog met een ander spelletje bezig, vadsje.", "You're still busy with another game, vadsje."),
    # Pepernotenrace
    T + "race.eerste": ("Je allereerste Pepernotenrace! %s pepernoten extra, njeg!", "Your very first Pepernoten Race! %s extra pepernoten, njeg!"),
    T + "race.noten": ("Score %s: je krijgt %s pepernoten!", "Score %s: you get %s pepernoten!"),
    T + "race.plaats": ("Plaats %s op het highscorebord! VAHOEG!", "Place %s on the highscore board! VAHOEG!"),
    T + "race.scherm.naam": ("Pepernotenrace", "Pepernoten Race"),
    T + "race.scherm.start": ("Spatie: start", "Space: start"),
    T + "race.scherm.uitleg": ("Pak de pepernoten!", "Grab the pepernoten!"),
    T + "race.scherm.af": ("Plof!", "Plop!"),
    T + "race.scherm.record": ("Record!", "Record!"),
    T + "race.scherm.noten": ("Pepernoten", "Pepernoten"),
    T + "race.scherm.ongeldig": ("Telt niet", "Doesn't count"),
    T + "race.toetsen": ("A/D of pijltjes: van baan wisselen · W of spatie: springen · S: bukken · Esc: stoppen",
                         "A/D or arrows: change lanes · W or space: jump · S: duck · Esc: stop"),
    T + "race.tellen": ("De kast telt je pepernoten...", "The cabinet counts your pepernoten..."),
    T + "race.geen_antwoord": ("De kast antwoordt niet. Probeer het straks nog eens.", "The cabinet doesn't answer. Try again in a bit."),
    T + "race.uit.ongeldig": ("Die run ging sneller dan de klok. Hij telt niet, vadsje.", "That run went faster than the clock. It doesn't count, vadsje."),
    T + "race.uit.plaats": ("Plaats %s in de top 10! Spatie: nog een keer.", "Place %s in the top 10! Space: once more."),
    T + "race.uit.gewoon": ("Goed gerend! Spatie: nog een keer.", "Well run! Space: once more."),
    T + "race.top10": ("Top 10", "Top 10"),
    T + "race.jouw_best": ("Jouw beste: %s", "Your best: %s"),
    T + "race.tip": ("Elke 10 pepernoten op de daken = 1 echte pepernoot, tot %s per run.",
                     "Every 10 pepernoten on the roofs = 1 real pepernoot, up to %s a run."),
    T + "race.tip_eerste": ("Je eerste run geeft %s pepernoten extra!", "Your first run gives %s extra pepernoten!"),
    # the boards
    T + "bord.kop.race": ("Pepernotenrace · Top 10", "Pepernoten Race · Top 10"),
    T + "bord.kop.schaats": ("Schaatsrondje · Top 10", "Skating Lap · Top 10"),
    T + "bord.leeg": ("Nog niemand! Word jij de eerste?", "Nobody yet! Will you be the first?"),
    T + "bord.klik": ("Klik: jouw eigen beste", "Click: your own best"),
    T + "bord.eigen.race": ("Jouw beste Pepernotenrace: %s punten", "Your best Pepernoten Race: %s points"),
    T + "bord.eigen.schaats": ("Jouw snelste schaatsrondje: %s", "Your fastest skating lap: %s"),
    T + "bord.eigen.geen.race": ("Je hebt de Pepernotenrace nog niet gespeeld. De kast staat hiernaast!",
                                 "You haven't played the Pepernoten Race yet. The cabinet is right next to it!"),
    T + "bord.eigen.geen.schaats": ("Je hebt nog geen schaatsrondje gereden. Het startbordje staat bij de vijver!",
                                    "You haven't skated a lap yet. The start sign is by the pond!"),
    # schaatsrondje
    T + "schaats.uitleg": ("Schaatsrondje! Glij door de %s poortjes (de fonkelende is de volgende) en terug door het eerste. Klik het bordje weer om te stoppen.",
                           "Skating lap! Glide through the %s little gates (the sparkling one is next) and back through the first. Click the sign again to stop."),
    T + "schaats.leen": ("Je leent een paar winterschaatsen voor dit rondje.", "You borrow a pair of winter skates for this lap."),
    T + "schaats.vol": ("Je zakken zitten te vol voor een paar schaatsen.", "Your pockets are too full for a pair of skates."),
    T + "schaats.go": ("GLIJ!", "GLIDE!"),
    T + "schaats.stand": ("Poortje %s/%s · %s", "Gate %s/%s · %s"),
    T + "schaats.finish": ("Finish!", "Finish!"),
    T + "schaats.tijd": ("Je rondje: %s", "Your lap: %s"),
    T + "schaats.nieuw_beste": ("Nieuw eigen record: %s! Vahoeg!", "New personal best: %s! Vahoeg!"),
    T + "schaats.noten": ("Je krijgt %s pepernoten!", "You get %s pepernoten!"),
    T + "schaats.gestopt": ("Schaatsrondje gestopt.", "Skating lap stopped."),
    T + "schaats.te_ver": ("Je bent van de vijver af gegleden: rondje gestopt.", "You slid off the pond: lap stopped."),
    T + "schaats.te_lang": ("Zo lang duurt een rondje niet, vadsje: rondje gestopt.", "A lap doesn't take that long, vadsje: lap stopped."),
    # hidden pepernoten
    T + "zakje.al": ("Dit zakje had je al gevonden.", "You already found this little bag."),
    T + "zakje.gevonden": ("Een verstopt zakje! %s pepernoten. (%s van de %s gevonden)", "A hidden bag! %s pepernoten. (%s of %s found)"),
    # oliebollen
    T + "olie.titel": ("Oliebollen bakken", "Baking Oliebollen"),
    T + "olie.titel_airfryer": ("Oliebollen uit Lins airfryer", "Oliebollen from Lin's Airfryer"),
    T + "olie.uitleg": ("Spatie: schep deeg in de pan · klik (of 1-4) als de onderkant goudbruin is: omdraaien, en nog eens: eruit!",
                        "Space: scoop dough into the pan · click (or 1-4) when the underside is golden: turn it, and again: take it out!"),
    T + "olie.uitleg_airfryer": ("Spatie: deeg in het mandje · klik (of 1-4) als de onderkant goudbruin is: omdraaien, en nog eens: eruit! Geen druppel olie, zegt Lin.",
                                 "Space: dough into the basket · click (or 1-4) when the underside is golden: turn it, and again: take it out! Not a drop of oil, says Lin."),
    T + "olie.stand": ("Score %s · %s bollen", "Score %s · %s balls"),
    T + "olie.klaar_knop": ("Smakelijk!", "Enjoy!"),
    T + "olie.einde.pan.0": ("%s punten. Oei... meer vads dan bol. Volgend jaar beter!", "%s points. Oops... more vads than ball. Better luck next year!"),
    T + "olie.einde.pan.20": ("%s punten. Nou, ze zijn... rond. Ongeveer. Poedersuiker maakt alles goed.",
                              "%s points. Well, they're... round. Roughly. Powdered sugar fixes everything."),
    T + "olie.einde.pan.50": ("%s punten. Lekkere oliebollen! Njeg, nog eentje dan.", "%s points. Tasty oliebollen! Njeg, just one more then."),
    T + "olie.einde.pan.80": ("%s punten. VAHOEG! Bijna zo goed als die van oma Marrie!", "%s points. VAHOEG! Almost as good as Grandma Marrie's!"),
    T + "olie.einde.pan.100": ("%s punten. Perfecte oliebollen. Oma Marrie pinkt een traantje weg.",
                               "%s points. Perfect oliebollen. Grandma Marrie wipes away a little tear."),
    T + "olie.einde.airfryer.0": ("%s punten. Lin: 'Hm. Misschien stond hij op ontdooien.'", "%s points. Lin: 'Hm. Maybe it was set to defrost.'"),
    T + "olie.einde.airfryer.20": ("%s punten. Lin: 'Zie je? Geen druppel olie. ...wel een beetje bleek.'",
                                   "%s points. Lin: 'See? Not a drop of oil. ...a bit pale, though.'"),
    T + "olie.einde.airfryer.50": ("%s punten. Lin: 'Lekker luchtig, en geen churro-incident in zicht!'",
                                   "%s points. Lin: 'Nice and fluffy, and no churro incident in sight!'"),
    T + "olie.einde.airfryer.80": ("%s punten. Lin: 'Ik zei het toch. De airfryer wint ALTIJD.'", "%s points. Lin: 'Told you. The airfryer ALWAYS wins.'"),
    T + "olie.einde.airfryer.100": ("%s punten. Lin: 'De lekkerste oliebollen van de Guhmension. Uit een airfryer. Ik zeg niks. ...VAHOEG!'",
                                    "%s points. Lin: 'The tastiest oliebollen in the Guhmension. From an airfryer. I'm not saying anything. ...VAHOEG!'"),
    T + "olie.nog_niet": ("De bakkraam gaat pas open met Oud & Nieuw (vanaf 26 december). Het deeg moet nog rijzen!",
                          "The baking stall only opens for New Year's Eve (from 26 December). The dough still has to rise!"),
    T + "olie.kraam": ("Score %s: je krijgt %s oliebollen mee. Smakelijk!", "Score %s: you get %s oliebollen to take along. Enjoy!"),
    T + "olie.kraam_niets": ("Score %s: die waren niet om aan te zien... de kraam-guh eet ze zelf wel op, njeg.",
                             "Score %s: those weren't pretty... the stall guh will eat them himself, njeg."),
    T + "olie.noten": ("Je eerste goede baksel! %s pepernoten.", "Your first good batch! %s pepernoten."),
    # the shop
    T + "winkel.titel": ("Het Pepernotenwinkeltje", "The Pepernoten Shop"),
    T + "winkel.koop": ("Koop", "Buy"),
    T + "winkel.doei": ("Doei!", "Bye!"),
    T + "winkel.te_weinig": ("Je hebt %s pepernoten nodig, vadsje.", "You need %s pepernoten, vadsje."),
    T + "winkel.sneeuwballen": ("8 sneeuwballen", "8 snowballs"),
    T + "winkel.sneeuwballen.uitleg": ("Voor een lief sneeuwballengevecht.", "For a friendly snowball fight."),
    T + "winkel.chocomel": ("Warme chocomel", "Hot chocomel"),
    T + "winkel.chocomel.uitleg": ("Warm van binnen: even sneller.", "Warm inside: faster for a bit."),
    T + "winkel.chocoladeletter": ("Chocoladeletter", "Chocolate letter"),
    T + "winkel.chocoladeletter.uitleg": ("Met jouw eigen beginletter!", "With your very own first letter!"),
    T + "winkel.vuurwerk": ("3 vuurwerkpijlen", "3 firework rockets"),
    T + "winkel.vuurwerk.uitleg": ("Roze en goud, met een knal.", "Pink and gold, with a bang."),
    T + "winkel.schaatsen": ("Winterschaatsen", "Winter skates"),
    T + "winkel.schaatsen.uitleg": ("Sneller over het ijs. Tot 3 januari.", "Faster over the ice. Until 3 January."),
    T + "winkel.kerstmuts": ("Kerstmuts", "Christmas hat"),
    T + "winkel.kerstmuts.uitleg": ("Voor op je hoofd. Smelt op 3 januari.", "For on your head. Melts on 3 January."),
    T + "winkel.mijter": ("Sint-mijter", "Sint's mitre"),
    T + "winkel.mijter.uitleg": ("Net als de Sint! Smelt op 3 januari.", "Just like the Sint! Melts on 3 January."),
    T + "winkel.kerstlampjes": ("4 kerstlampjes", "4 Christmas lights"),
    T + "winkel.kerstlampjes.uitleg": ("Voor aan de muur. Smelt op 3 januari.", "For your walls. Melts on 3 January."),
}

# ---------------------------------------------------------------------------------------------------------------------
# painting
# ---------------------------------------------------------------------------------------------------------------------
ROOD, ROOD_D, WIT, GOUD, GOUD_D = (214, 52, 72), (150, 32, 52), (255, 250, 244), (255, 211, 78), (176, 138, 42)
HOUT, HOUT_D, SNEEUW, SNEEUW_S = (176, 122, 68), (122, 82, 50), (244, 248, 255), (201, 216, 240)
CHOCO, CHOCO_L = (90, 52, 32), (130, 80, 50)
NOOT, NOOT_D, NOOT_L = (201, 138, 75), (142, 90, 43), (232, 178, 122)


def leeg(b=16, h=16):
    return Image.new("RGBA", (b, h), (0, 0, 0, 0))


def vol(kleur, b=16, h=16):
    return Image.new("RGBA", (b, h), kleur + (255,))


def px(img, x, y, c):
    img.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def rect(img, x0, y0, x1, y1, c):
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], fill=c + (255,))


def ruis(img, kleur, var, seed):
    import random
    rng = random.Random(seed)
    for y in range(img.height):
        for x in range(img.width):
            v = rng.randint(-var, var)
            px(img, x, y, tuple(max(0, min(255, k + v)) for k in kleur))
    return img


def pepernoot(img, x, y):
    for dx, dy, c in ((1, 0, NOOT_L), (2, 0, NOOT), (3, 0, NOOT), (0, 1, NOOT_L), (1, 1, NOOT), (2, 1, NOOT), (3, 1, NOOT_D), (4, 1, NOOT),
                      (0, 2, NOOT), (1, 2, NOOT_D), (2, 2, NOOT), (3, 2, NOOT), (4, 2, NOOT), (0, 3, NOOT), (1, 3, NOOT), (2, 3, NOOT), (3, 3, NOOT_D),
                      (4, 3, NOOT), (1, 4, NOOT), (2, 4, NOOT_D), (3, 4, NOOT)):
        px(img, x + dx, y + dy, c)


def sneeuw():
    return ruis(vol(SNEEUW), SNEEUW, 6, 11)


def hout():
    img = ruis(vol(HOUT), HOUT, 8, 12)
    d = ImageDraw.Draw(img)
    for y in (3, 7, 11, 15):
        d.line([0, y, 15, y], fill=HOUT_D + (255,))
    return img


def kast_zij():
    img = ruis(vol(ROOD), ROOD, 7, 13)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=ROOD_D + (255,))
    for x in (5, 10):
        d.line([x, 1, x, 14], fill=ROOD_D + (255,))
    for x in range(1, 15, 3):     # a garland of little lights along the top
        px(img, x, 1, ((255, 211, 78), (120, 220, 140), (130, 170, 255), (255, 150, 200))[x % 4])
    return img


def kast_onder():
    img = kast_zij()
    rect(img, 1, 0, 14, 6, (52, 40, 72))         # the control panel
    rect(img, 3, 1, 4, 3, (60, 60, 70))
    rect(img, 2, 0, 5, 1, (230, 60, 80))         # the joystick's ball
    for x, c in ((8, (255, 211, 78)), (11, (255, 150, 200))):
        rect(img, x, 2, x + 1, 3, c)
    rect(img, 6, 9, 9, 13, (40, 30, 30))         # the coin slot
    rect(img, 7, 10, 8, 12, GOUD)
    pepernoot(img, 10, 9)
    return img


def kast_boven():
    img = kast_zij()
    rect(img, 1, 0, 14, 4, GOUD)                  # the marquee with three pepernoten
    for x in (1, 6, 11):
        pepernoot(img, x, 0)
    rect(img, 1, 5, 14, 15, (20, 16, 28))         # the screen: three snow paths on a red roof, a pink guh
    rect(img, 2, 6, 13, 14, (180, 85, 63))
    for x in (3, 7, 11):
        rect(img, x, 6, x + 1, 14, SNEEUW)
    rect(img, 7, 11, 8, 13, (255, 158, 196))
    px(img, 7, 10, (224, 56, 76))
    px(img, 4, 8, NOOT)
    px(img, 12, 7, NOOT)
    return img


def bord(soort):
    img = vol((40, 72, 58))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=GOUD + (255,))
    d.rectangle([1, 1, 14, 14], outline=GOUD_D + (255,))
    if soort == "race":
        pepernoot(img, 2, 2)
    else:                           # a little skate
        d.line([2, 6, 7, 6], fill=(220, 230, 240, 255))
        rect(img, 3, 3, 5, 5, (120, 180, 255))
    for y in (9, 11, 13):
        d.line([3, y, 12 - (y % 3) * 2, y], fill=(220, 236, 220, 255))
    for x, y in ((9, 3), (11, 5), (13, 3)):
        px(img, x, y, GOUD)
    return img


def streep():
    img = vol(WIT)
    d = ImageDraw.Draw(img)
    for x in range(0, 16, 4):
        d.rectangle([x, 0, x + 1, 15], fill=ROOD + (255,))
    d.rectangle([0, 0, 15, 15], outline=ROOD_D + (255,))
    return img


def kraam_voor(soort):
    img = streep()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 12, 12], fill=WIT + (255,), outline=ROOD_D + (255,))
    if soort == "winkel":
        pepernoot(img, 4, 7)
        pepernoot(img, 7, 5)
        pepernoot(img, 8, 8)
    else:
        d.ellipse([5, 5, 10, 10], fill=(150, 92, 44, 255), outline=(96, 54, 24, 255))
        for p in ((6, 6), (9, 7), (7, 9)):
            px(img, p[0], p[1], WIT)
    return img


def luifel():
    img = vol(WIT)
    for y in range(0, 16, 4):
        rect(img, 0, y, 15, y + 1, ROOD)
    return img


def schaatsbord():
    img = vol((150, 200, 240))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(70, 110, 170, 255))
    rect(img, 4, 4, 8, 8, WIT)                    # the boot
    rect(img, 4, 8, 11, 9, WIT)
    d.line([3, 11, 12, 11], fill=(90, 100, 120, 255))   # the blade
    px(img, 12, 10, (90, 100, 120))
    for x in (2, 13):
        px(img, x, 2, WIT)
    return img


def zakje():
    img = ruis(vol((196, 160, 110)), (196, 160, 110), 10, 14)
    for y in range(0, 16, 2):
        for x in range((y // 2) % 2, 16, 2):
            px(img, x, y, (170, 136, 90))
    pepernoot(img, 5, 6)
    return img


def lint():
    return ruis(vol((214, 52, 72)), (214, 52, 72), 8, 15)


LAMP_KLEUREN = [(255, 80, 90), (255, 211, 78), (110, 220, 130), (110, 160, 255)]


def lampjes():
    """16 x 32: two frames (they twinkle). Rows 0-3 the four bulb colours (4 px each), row 8 the green wire."""
    img = leeg(16, 32)
    for frame in (0, 1):
        for i, c in enumerate(LAMP_KLEUREN):
            aan = (i + frame) % 2 == 0
            k = c if aan else tuple(int(v * 0.65) for v in c)
            rect(img, i * 4, frame * 16, i * 4 + 3, frame * 16 + 3, k)
            if aan:
                px(img, i * 4 + 1, frame * 16 + 1, (255, 255, 240))
        rect(img, 0, frame * 16 + 8, 15, frame * 16 + 9, (40, 90, 50))
    return img


# --- items --------------------------------------------------------------------------------------------------------------

def chocomel():
    img = leeg()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 5, 11, 14], fill=(255, 158, 196, 255), outline=(176, 56, 96, 255))
    d.rectangle([11, 7, 13, 11], outline=(176, 56, 96, 255))
    d.rectangle([4, 5, 10, 6], fill=CHOCO_L + (255,))
    for x, y in ((4, 3), (5, 2), (6, 3), (7, 2), (8, 3), (9, 3), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (4, 4)):
        px(img, x, y, WIT)
    px(img, 7, 1, (232, 56, 76))
    px(img, 6, 9, WIT)
    return img


# a 5 x 7 font for the chocolate letters (each pixel becomes 2 x 2, with a highlight)
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"], "B": ["11110", "10001", "11110", "10001", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"], "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "11110", "10000", "10000", "10000", "11111"], "F": ["11111", "10000", "11110", "10000", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10000", "10011", "10001", "10001", "01111"], "H": ["10001", "10001", "11111", "10001", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"], "J": ["00111", "00001", "00001", "00001", "10001", "10001", "01110"],
    "K": ["10001", "10010", "11100", "10100", "10010", "10001", "10001"], "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"], "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"], "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"], "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"], "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"], "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "11011", "10001"], "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"], "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
}


def letter(ch):
    img = leeg()
    rijen = FONT[ch]
    for r, rij in enumerate(rijen):
        for k, c in enumerate(rij):
            if c == "1":
                x, y = 3 + k * 2, 1 + r * 2
                rect(img, x, y, x + 1, y + 1, CHOCO)
    # a lighter top-left edge, a darker bottom-right, like a real chocolate letter
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] and (x == 0 or not img.getpixel((x - 1, y))[3] or y == 0 or not img.getpixel((x, y - 1))[3]):
                px(img, x, y, CHOCO_L)
    for y in range(15, -1, -1):
        for x in range(15, -1, -1):
            if img.getpixel((x, y))[3] and (x == 15 or not img.getpixel((x + 1, y))[3]) and img.getpixel((x, y))[:3] == CHOCO:
                px(img, x, y, (60, 34, 20))
    return img


def letter_los():
    """The plain one (no letter known): a G in golden foil."""
    img = letter("G")
    d = ImageDraw.Draw(img)
    d.line([2, 15, 13, 15], fill=GOUD + (255,))
    return img


def muts_tex():
    rood = ruis(vol((210, 40, 56)), (210, 40, 56), 9, 21)
    wit = ruis(vol((250, 250, 252)), (250, 250, 252), 6, 22)
    return rood, wit


def mijter_tex():
    wit = ruis(vol((250, 248, 240)), (250, 248, 240), 5, 23)
    rood = ruis(vol((200, 30, 44)), (200, 30, 44), 8, 24)
    goud = ruis(vol(GOUD), GOUD, 12, 25)
    return wit, rood, goud


def icoon_kast():
    img = leeg()
    rect(img, 4, 1, 11, 15, ROOD)
    rect(img, 4, 1, 11, 2, GOUD)
    rect(img, 5, 3, 10, 8, (20, 16, 28))
    rect(img, 6, 4, 9, 7, (180, 85, 63))
    px(img, 7, 6, (255, 158, 196))
    rect(img, 3, 9, 12, 10, (52, 40, 72))
    px(img, 5, 9, (230, 60, 80))
    px(img, 9, 9, GOUD)
    rect(img, 4, 0, 11, 0, SNEEUW)
    return img


def icoon_lampjes():
    img = leeg()
    d = ImageDraw.Draw(img)
    d.line([0, 4, 4, 7, 8, 5, 12, 8, 15, 5], fill=(40, 90, 50, 255))
    for i, (x, y) in enumerate(((1, 6), (5, 9), (9, 7), (13, 10))):
        rect(img, x, y, x + 1, y + 2, LAMP_KLEUREN[i])
        px(img, x, y, (255, 255, 240))
    return img


# ---------------------------------------------------------------------------------------------------------------------
# models
# ---------------------------------------------------------------------------------------------------------------------

def el(frm, to, faces, rot=None):
    """faces: {side: texture} or one texture for all six."""
    if isinstance(faces, str):
        faces = {f: faces for f in ("north", "south", "east", "west", "up", "down")}
    e = {"from": frm, "to": to, "faces": {f: {"texture": "#" + t} for f, t in faces.items()}}
    if rot:
        e["rotation"] = rot
    return e


def kanten(voor, zij, boven=None, onder=None):
    return {"north": voor, "south": zij, "east": zij, "west": zij, "up": boven or zij, "down": onder or zij}


HOOFD_DISPLAY = {
    "head": {"translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [25, 200, 0], "translation": [0, -5, 0], "scale": [0.55, 0.55, 0.55]},
    "ground": {"translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, -5, 0], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, -1, 0], "scale": [0.35, 0.35, 0.35]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.35, 0.35, 0.35]},
}


def kerstmuts_model():
    """Model space: the player's head is 1.6..14.4 on every axis (head top at y 14.4); the hat sits round the top."""
    els = [
        el([1.1, 11.6, 1.1], [14.9, 15.2, 14.9], "w"),                     # the white fur band round the forehead
        el([1.8, 15.2, 1.8], [14.2, 18.4, 14.2], "r"),
        el([2.8, 18.4, 3.0], [13.2, 21.4, 13.2], "r"),
        el([4.0, 21.4, 4.6], [12.0, 24.0, 12.6], "r"),
        el([5.4, 23.6, 6.8], [11.0, 26.0, 11.8], "r", {"angle": -22.5, "axis": "x", "origin": [8, 24, 9]}),
        el([6.4, 24.6, 10.2], [10.0, 27.0, 14.6], "r", {"angle": -45, "axis": "x", "origin": [8, 25, 11]}),
        el([6.6, 22.0, 14.2], [9.6, 25.0, 17.2], "w"),                     # the pompom, flopped to the back
    ]
    return {"textures": {"r": "guhs:item/guhwinter_muts_rood", "w": "guhs:item/guhwinter_muts_wit", "particle": "guhs:item/guhwinter_muts_rood"},
            "elements": els, "display": HOOFD_DISPLAY}


def mijter_model():
    els = [
        el([1.2, 11.6, 1.2], [14.8, 14.8, 14.8], "g"),                     # the golden band
        el([2.0, 14.8, 3.0], [14.0, 19.0, 13.0], "w"),
        el([3.0, 19.0, 4.0], [13.0, 23.0, 12.0], "w"),
        el([4.4, 23.0, 5.0], [11.6, 26.4, 11.0], "w"),
        el([6.0, 26.4, 6.0], [10.0, 28.6, 10.0], "w"),
        el([7.0, 28.6, 7.0], [9.0, 29.6, 9.0], "g"),                       # the little top
        el([3.0, 14.8, 2.7], [13.0, 22.0, 3.0], "r"),                      # the red front with its golden cross
        el([7.2, 15.6, 2.4], [8.8, 24.6, 2.7], "g"),
        el([4.6, 19.4, 2.4], [11.4, 21.0, 2.7], "g"),
        el([3.0, 14.8, 13.0], [13.0, 22.0, 13.3], "r"),                    # the red back
    ]
    return {"textures": {"w": "guhs:item/guhwinter_mijter_wit", "r": "guhs:item/guhwinter_mijter_rood", "g": "guhs:item/guhwinter_mijter_goud",
                         "particle": "guhs:item/guhwinter_mijter_wit"}, "elements": els, "display": HOOFD_DISPLAY}


ITEMS = ("guhwinter_arcadekast", "guhwinter_highscorebord", "guhwinter_winkelkraam", "guhwinter_bakkraam", "guhwinter_schaatsbord",
         "guhwinter_pepernootzakje", "guhwinter_kerstlampjes", "guhwinter_chocomel", "guhwinter_schaatsen", "guhwinter_kerstmuts", "guhwinter_mijter")
# the block items' names (mc26.lang_block_items copies them in a full run; here for the standalone run)
for _k in [k for k in TEKSTEN if k.startswith("block.guhs.") and k.count(".") == 2]:
    TEKSTEN["item.guhs." + _k[len("block.guhs."):]] = TEKSTEN[_k]


def build(h):
    w = h.w
    # --- textures ---
    for naam, img in (("guhwinter_sneeuw", sneeuw()), ("guhwinter_hout", hout()), ("guhwinter_kast_zij", kast_zij()),
                      ("guhwinter_kast_onder", kast_onder()), ("guhwinter_kast_boven", kast_boven()), ("guhwinter_bord_race", bord("race")),
                      ("guhwinter_bord_schaats", bord("schaats")), ("guhwinter_kraam_zij", streep()), ("guhwinter_winkelkraam_voor", kraam_voor("winkel")),
                      ("guhwinter_bakkraam_voor", kraam_voor("bak")), ("guhwinter_luifel", luifel()), ("guhwinter_schaatsbord", schaatsbord()),
                      ("guhwinter_zakje", zakje()), ("guhwinter_lint", lint()), ("guhwinter_kerstlampjes", lampjes())):
        h.save(img, "block", naam + ".png")
    w(os.path.join(A, "textures", "block", "guhwinter_kerstlampjes.png.mcmeta"), {"animation": {"frametime": 14, "interpolate": False}})
    rood, wit = muts_tex()
    h.save(rood, "item", "guhwinter_muts_rood.png")
    h.save(wit, "item", "guhwinter_muts_wit.png")
    mw, mr, mg = mijter_tex()
    h.save(mw, "item", "guhwinter_mijter_wit.png")
    h.save(mr, "item", "guhwinter_mijter_rood.png")
    h.save(mg, "item", "guhwinter_mijter_goud.png")
    h.save(chocomel(), "item", "guhwinter_chocomel.png")
    h.save(letter_los(), "item", "guhwinter_chocoladeletter.png")
    for ch in FONT:
        h.save(letter(ch), "item", f"guhwinter_chocoladeletter_{ch.lower()}.png")
    h.save(icoon_kast(), "item", "guhwinter_arcadekast.png")
    h.save(icoon_lampjes(), "item", "guhwinter_kerstlampjes.png")
    skate = Image.open(os.path.join(A, "textures", "item", "guh_schaatsen.png")).convert("RGBA")
    h.save(h.rehue(skate, 0.55, 0.9, 1.05), "item", "guhwinter_schaatsen.png")       # the elftocht skates, icy blue

    bt = lambda n: f"guhs:block/{n}"       # noqa: E731
    # --- the arcade cabinet (two halves) ---
    tk = {"z": bt("guhwinter_kast_zij"), "o": bt("guhwinter_kast_onder"), "b": bt("guhwinter_kast_boven"), "s": bt("guhwinter_sneeuw"),
          "particle": bt("guhwinter_kast_zij")}
    w(os.path.join(A, "models", "block", "guhwinter_arcadekast_onder.json"), {"parent": "minecraft:block/block", "textures": tk, "elements": [
        el([1, 0, 2], [15, 16, 15], kanten("o", "z")),
        el([1, 9, 0], [15, 11, 2], kanten("z", "z", "o")),
    ]})
    w(os.path.join(A, "models", "block", "guhwinter_arcadekast_boven.json"), {"parent": "minecraft:block/block", "textures": tk, "elements": [
        el([1, 0, 2], [15, 15, 15], kanten("b", "z")),
        el([0.5, 15, 1.5], [15.5, 16.5, 15.5], "s"),
    ]})
    states = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for half, m in (("lower", "onder"), ("upper", "boven")):
            states[f"facing={f},half={half}"] = {"model": f"guhs:block/guhwinter_arcadekast_{m}", **({"y": y} if y else {})}
    w(os.path.join(A, "blockstates", "guhwinter_arcadekast.json"), {"variants": states})
    h.item_model("guhwinter_arcadekast")

    # --- the highscore board (race / schaats) ---
    for soort in ("race", "schaats"):
        w(os.path.join(A, "models", "block", f"guhwinter_highscorebord_{soort}.json"), {"parent": "minecraft:block/block", "textures": {
            "v": bt(f"guhwinter_bord_{soort}"), "h": bt("guhwinter_hout"), "s": bt("guhwinter_sneeuw"), "particle": bt("guhwinter_hout")}, "elements": [
            el([2, 0, 7], [4, 8, 9], "h"), el([12, 0, 7], [14, 8, 9], "h"),
            el([1, 6, 6.5], [15, 16, 9.5], kanten("v", "h")),
            el([1, 16, 6.5], [15, 17, 9.5], "s"),
        ]})
    states = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for soort in ("race", "schaats"):
            states[f"facing={f},spel={soort}"] = {"model": f"guhs:block/guhwinter_highscorebord_{soort}", **({"y": y} if y else {})}
    w(os.path.join(A, "blockstates", "guhwinter_highscorebord.json"), {"variants": states})
    w(os.path.join(A, "models", "item", "guhwinter_highscorebord.json"), {"parent": "guhs:block/guhwinter_highscorebord_race"})

    # --- the stalls ---
    for naam in ("guhwinter_winkelkraam", "guhwinter_bakkraam"):
        h.furniture_model(naam, [
            el([0, 0, 1], [16, 10, 16], kanten("v", "z", "b", "b")),
            el([0, 10, 0], [16, 11, 16], "b"),
            el([0.5, 11, 14], [2, 15, 15.5], "b"), el([14, 11, 14], [15.5, 15, 15.5], "b"),
            el([0, 15, 7], [16, 16, 16], "l"),
            el([0, 16, 8], [16, 16.8, 16], "s"),
        ], {"v": bt(f"{naam}_voor"), "z": bt("guhwinter_kraam_zij"), "b": bt("guhwinter_hout"), "l": bt("guhwinter_luifel"),
            "s": bt("guhwinter_sneeuw"), "particle": bt("guhwinter_kraam_zij")})

    # --- the start sign ---
    h.furniture_model("guhwinter_schaatsbord", [
        el([7, 0, 7], [9, 9, 9], "h"),
        el([2, 8, 7.5], [14, 15, 8.5], kanten("v", "h")),
        el([2, 15, 7.5], [14, 15.8, 8.5], "s"),
    ], {"v": bt("guhwinter_schaatsbord"), "h": bt("guhwinter_hout"), "s": bt("guhwinter_sneeuw"), "particle": bt("guhwinter_schaatsbord")})

    # --- the hidden bag ---
    h.furniture_model("guhwinter_pepernootzakje", [
        el([5, 0, 5], [11, 5, 11], "z"),
        el([6, 5, 6], [10, 6, 10], "z"),
        el([6.5, 6, 6.5], [9.5, 6.8, 9.5], "l"),
        el([7, 6.8, 7], [9, 8, 9], "z"),
    ], {"z": bt("guhwinter_zakje"), "l": bt("guhwinter_lint"), "particle": bt("guhwinter_zakje")})

    # --- kerstlampjes (FACING away from the wall: the wire runs along the wall at the back) ---
    lamp_els = [{"from": [0, 14, 14.6], "to": [16, 14.6, 15.2], "faces": {f: {"texture": "#l", "uv": [0, 8, 16, 9]} for f in
                                                                              ("north", "south", "up", "down", "east", "west")}}]
    for i, (x, y) in enumerate(((1.5, 12), (5.5, 11.5), (9.5, 12.5), (13.0, 11))):
        uv = [i * 4, 0, i * 4 + 4, 4]
        lamp_els.append({"from": [x, y, 13.6], "to": [x + 1.6, y + 2.4, 15.2],
                         "faces": {f: {"texture": "#l", "uv": uv} for f in ("north", "south", "up", "down", "east", "west")}})
    w(os.path.join(A, "models", "block", "guhwinter_kerstlampjes.json"), {"parent": "minecraft:block/block",
                                                                           "ambientocclusion": False,
                                                                           "textures": {"l": bt("guhwinter_kerstlampjes"), "particle": bt("guhwinter_kerstlampjes")},
                                                                           "elements": lamp_els})
    w(os.path.join(A, "blockstates", "guhwinter_kerstlampjes.json"), {"variants": h.facing_states("guhwinter_kerstlampjes")})
    h.item_model("guhwinter_kerstlampjes")
    h.self_drop("guhwinter_kerstlampjes")

    # --- items ---
    h.item_model("guhwinter_chocomel")
    h.item_model("guhwinter_schaatsen", parent="minecraft:item/handheld")
    h.item_model("guhwinter_chocoladeletter")
    for ch in FONT:
        h.item_model(f"guhwinter_chocoladeletter_{ch.lower()}")
    w(os.path.join(A, "items", "guhwinter_chocoladeletter.json"), {"model": {
        "type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
        "cases": [{"when": ch, "model": {"type": "minecraft:model", "model": f"guhs:item/guhwinter_chocoladeletter_{ch.lower()}"}} for ch in FONT],
        "fallback": {"type": "minecraft:model", "model": "guhs:item/guhwinter_chocoladeletter"}}})
    w(os.path.join(A, "models", "item", "guhwinter_kerstmuts.json"), kerstmuts_model())
    w(os.path.join(A, "models", "item", "guhwinter_mijter.json"), mijter_model())

    # --- tags: what melts on 3 January (kern's mechanism) ---
    h.add_tag("guhs/tags/item/guhwinter_smelt", ["guhs:guhwinter_kerstmuts", "guhs:guhwinter_mijter", "guhs:guhwinter_kerstlampjes", "guhs:guhwinter_schaatsen"])
    h.add_tag("guhs/tags/block/guhwinter_smelt_blok", ["guhs:guhwinter_kerstlampjes"])

    # --- item definitions (26.1; mc26.item_definitions makes the same ones in a full run) ---
    for naam in ITEMS:
        w(os.path.join(A, "items", naam + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})

    # --- texts ---
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    schrijf_en()


def schrijf_en():
    os.makedirs(os.path.dirname(EN_FILE), exist_ok=True)
    with open(EN_FILE, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in sorted(TEKSTEN.items())}, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    import sys
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"guhwinter_spellen: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
