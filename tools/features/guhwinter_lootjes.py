"""
Guhs 1.10.10 "Guhwinter", slice LOOTJES (Java: feature/guhwinter/lootjes): Het Grote Boek, the draw, the 8 creative plots,
the Surprise (a captured plot you can place), the Zak van Sinterklaas.

  - textures: block/guhwinter_grote_boek_pagina + _kaft (the open book on the lectern), item/guhwinter_surprise (a wrapped
    present), item/guhwinter_zak (the sack)
  - models + blockstates: the lectern with the book (vanilla lectern shape + a book on top), the plot sign (vanilla spruce
    sign look: the sign renderer draws it), the invisible Surprise blocks (particle only)
  - item models + 26.1 item definitions
  - tags: guhs:guhwinter_lootje_verboden (what may not stand in a plot), guhs:guhwinter_lootje_mag (empty: modded decoration
    with a block entity that may anyway), minecraft:wither_immune / dragon_immune += the Surprise blocks
  - lang: Dutch into nl_nl, English into the overlay tools/lang/en/c510_guhwinter_lootjes.json (+ its chunk)
  - the blokuitleg of Het Grote Boek, the plot sign and the Surprise (tools/wiki_site/blokuitleg.py, between markers)

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/guhwinter_lootjes.py
"""
import json
import os
import re

from PIL import Image

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data")
TEX = os.path.join(A, "textures")
K = "guhwinter.lootjes."
S = K + "surprise."
CHUNK = "c510_guhwinter_lootjes"

# =====================================================================================================================
# texts: key -> (Dutch, English)
# =====================================================================================================================
TEKSTEN = {
    "block.guhs.guhwinter_grote_boek": ("Het Grote Boek", "The Big Book"),
    "item.guhs.guhwinter_grote_boek": ("Het Grote Boek", "The Big Book"),
    "block.guhs.guhwinter_bouwbord": ("Bordje van een bouwplekje", "Build Plot Sign"),
    "item.guhs.guhwinter_bouwbord": ("Bordje van een bouwplekje", "Build Plot Sign"),
    "block.guhs.guhwinter_surprise": ("Surprise", "Surprise"),
    "block.guhs.guhwinter_surprise_deel": ("Stukje surprise", "Piece of a Surprise"),
    "item.guhs.guhwinter_surprise": ("Surprise", "Surprise"),
    "item.guhs.guhwinter_surprise.van": ("Surprise van %s", "Surprise from %s"),
    "item.guhs.guhwinter_surprise.voor": ("Voor %s", "For %s"),
    "item.guhs.guhwinter_surprise.niks": ("Een lege verpakking. Hier zit (nog) niks in, njeg.", "Empty wrapping. Nothing in here (yet), nyeg."),
    "item.guhs.guhwinter_surprise.leeg": ("Oei... dit pakje is leeg. Maar het is wel met heel veel liefde ingepakt!",
                                          "Oops... this present is empty. But it was wrapped with lots and lots of love!"),
    "item.guhs.guhwinter_surprise.blokken": ("%s blokjes, 7 bij 7 bij 7", "%s blocks, 7 by 7 by 7"),
    "item.guhs.guhwinter_surprise.lore": ("Vasthouden = voorbeeld. Rechtsklik zet hem neer; R of shift+scroll draait hem. "
                                          "Shift+rechtsklik op je surprise pakt hem weer op.",
                                          "Hold it to see where it goes. Right-click puts it down; R or shift+scroll turns it. "
                                          "Shift+right-click your surprise to pick it up again."),
    "item.guhs.guhwinter_zak": ("Zak van Sinterklaas", "Sinterklaas' Sack"),
    "item.guhs.guhwinter_zak.lore": ("Rechtsklik om hem open te maken. Wat niet in je tas past, blijft er netjes in zitten.",
                                     "Right-click to open it. Whatever doesn't fit in your bag stays safely inside."),
    "key.guhs.guhwinter_surprise_draai": ("Surprise draaien", "Turn Surprise"),
    # --- Het Grote Boek ---
    K + "boek.titel": ("~ Het Grote Boek van Sinterklaas ~", "~ The Big Book of Sinterklaas ~"),
    K + "boek.teller": ("%s  %s van de %s plekjes", "%s  %s of %s places"),
    K + "boek.namen": ("Al in het boek: %s", "Already in the book: %s"),
    K + "boek.sint": ("De Sint: \"Wie wil er meedoen met lootjes trekken? Schrijf je naam in mijn boek, lieve guh!\"",
                      "The Sint: \"Who wants to join the drawing of names? Write your name in my book, dear guh!\""),
    K + "boek.jij_erin": ("Jouw naam staat erin! Vahoeg!", "Your name is in it! Wahoog!"),
    K + "boek.vol": ("Het Grote Boek is vol! Alle acht plekjes zijn bezet.", "The Big Book is full! All eight places are taken."),
    K + "boek.knop.in": ("[Schrijf mijn naam in het boek]", "[Write my name in the book]"),
    K + "boek.knop.in.hover": ("Doe mee met lootjes trekken", "Join the drawing of names"),
    K + "boek.knop.uit": ("[Toch maar niet]", "[Never mind]"),
    K + "boek.knop.uit.hover": ("Je naam weer uit het boek halen", "Take your name out of the book again"),
    K + "boek.wanneer": ("Inschrijven kan t/m 30 november 23:59. Op 1 december trekt de Sint de lootjes, en dan mag je bouwen en tekenen "
                         "tot 5 december 18:00. Pakjesavond: 5 december 19:00!",
                         "You can sign up through 30 November 23:59. On 1 December the Sint draws the names, then you build and draw until "
                         "5 December 18:00. Pakjesavond: 5 December 19:00!"),
    K + "boek.trekken_straks": ("Het boek is dicht. Om middernacht trekt de Sint de lootjes... spannend, njeg!",
                                "The book is closed. At midnight the Sint draws the names... exciting, nyeg!"),
    K + "boek.dicht": ("Het boek is dicht voor dit jaar. Volgend jaar ben je er vast bij!",
                       "The book is closed for this year. Next year you'll surely join!"),
    K + "boek.mislukt": ("Er stonden niet genoeg namen in het boek om te trekken. De Sint heeft iedereen een briefje gestuurd.",
                         "There weren't enough names in the book to draw. The Sint sent everyone a little letter."),
    K + "boek.niet_mee": ("De Sint heeft getrokken! Jij deed dit jaar niet mee, maar kijk gerust naar de bouwplekjes in het Surprisestraatje.",
                          "The Sint has drawn the names! You didn't join this year, but do have a look at the plots in Surprise Lane."),
    K + "boek.jouw_lootje": ("Ssst... jij hebt %s getrokken! Jouw bouwplekje is nummer %s in het Surprisestraatje.",
                             "Shh... you drew %s! Your build plot is number %s in Surprise Lane."),
    K + "boek.knop.teken": ("[Maak een tekening]", "[Make a drawing]"),
    K + "boek.knop.teken.hover": ("Een tekening voor je lootje (64 bij 64)", "A drawing for the name you drew (64 by 64)"),
    K + "boek.knop.teken_verder": ("[Verder met je tekening]", "[Carry on with your drawing]"),
    K + "boek.knop.teken_verder.hover": ("Je tekening aanpassen", "Change your drawing"),
    K + "boek.tot": ("Bouwen en tekenen kan tot 5 december 18:00. Wat er dan staat, krijgt je lootje!",
                     "You can build and draw until 5 December 18:00. Whatever is there by then goes to your name!"),
    K + "boek.wachten": ("Alles is ingepakt! Nog even wachten op pakjesavond, 5 december 19:00...",
                         "Everything is wrapped! Just a little wait for pakjesavond, 5 December 19:00..."),
    K + "boek.klaar": ("Alle surprises zijn uitgedeeld. Wat een feest! Vahoeg!", "All the surprises have been handed out. What a party! Wahoog!"),
    K + "in.dicht": ("Het Grote Boek is nu dicht: inschrijven kan niet meer.", "The Big Book is closed now: you can't sign up any more."),
    K + "in.al": ("Je naam staat al in het boek, gekke guh!", "Your name is already in the book, silly guh!"),
    K + "in.gelukt": ("Je naam staat in Het Grote Boek! (%s van de %s) Op 1 december krijg je een briefje van de Sint.",
                      "Your name is in the Big Book! (%s of %s) On 1 December you'll get a letter from the Sint."),
    K + "uit.te_laat": ("Uitschrijven kan niet meer: de lootjes zijn al (bijna) getrokken.", "You can't sign out any more: the names are (almost) drawn."),
    K + "uit.niet": ("Je naam stond niet in het boek.", "Your name wasn't in the book."),
    K + "uit.gelukt": ("Je naam is weer uit het boek. Jammer! Je mag je t/m 30 november opnieuw inschrijven.",
                       "Your name is out of the book again. Aww! You can sign up again through 30 November."),
    # --- drawing ---
    K + "teken.voor": ("Je tekent voor %s. Ssst!", "You're drawing for %s. Shh!"),
    K + "teken.geen_lootje": ("Je hebt geen lootje getrokken, dus je hoeft niks te tekenen.", "You didn't draw a name, so there's nothing to draw."),
    K + "teken.dicht": ("Te laat! Na 5 december 18:00 is alles ingepakt.", "Too late! After 5 December 18:00 everything is wrapped."),
    K + "teken.bewaard": ("Je tekening voor %s is bewaard! Je mag hem tot 5 december 18:00 nog aanpassen.",
                          "Your drawing for %s is saved! You can still change it until 5 December 18:00."),
    K + "teken.bijgewerkt": ("Je tekening voor %s is bijgewerkt. Njeg, nog mooier!", "Your drawing for %s is updated. Nyeg, even prettier!"),
    # --- building ---
    K + "bouwen.in": ("Welkom op je bouwplekje! Bouw iets moois voor %s. Je spullen liggen veilig bij de Sint tot je weer naar buiten loopt.",
                      "Welcome to your build plot! Make something lovely for %s. Your things are safe with the Sint until you walk out again."),
    K + "bouwen.regels": ("Alles wat mooi is mag (ook diamant!). Geen kisten, machines, redstone of spawners. Je kunt alleen in je eigen "
                          "plekje bouwen en vliegen.",
                          "Anything pretty goes (diamond too!). No chests, machines, redstone or spawners. You can only build and fly "
                          "inside your own plot."),
    K + "bouwen.uit": ("Je bent je bouwplekje uit gelopen: je eigen spullen zijn terug!", "You walked out of your build plot: your own things are back!"),
    K + "bouwen.tijd_om": ("Bouwen is voorbij: je eigen spullen zijn terug!", "Building is over: your own things are back!"),
    K + "bouwen.grens": ("Tot hier! Je bouwt alleen op je eigen plekje.", "This far! You only build on your own plot."),
    K + "bouwen.niet_jouw": ("Dit is het bouwplekje van een ander. Kijken mag, naar binnen niet!",
                             "This is someone else's build plot. Looking is fine, going in is not!"),
    K + "bouwen.vrij": ("Dit bouwplekje is (nog) van niemand.", "This build plot belongs to no one (yet)."),
    K + "bouwen.dicht": ("Bouwen kan nu niet: tussen 1 december en 5 december 18:00 is het open.",
                         "You can't build now: it's open between 1 December and 5 December 18:00."),
    K + "bouwen.modus": ("Kom in survival of adventure je bouwplekje in, dan krijg je creative.",
                         "Walk into your build plot in survival or adventure to get creative."),
    K + "bouwen.mag_niet": ("%s mag niet op je bouwplekje (geen kisten, machines, redstone of spawners).",
                            "%s isn't allowed on your build plot (no chests, machines, redstone or spawners)."),
    K + "bouwen.buiten_veld": ("Je bouwt alleen binnen je eigen bouwplekje.", "You only build inside your own build plot."),
    K + "bouwen.item": ("Dat kun je hier niet gebruiken: alleen blokken en versiergereedschap.", "You can't use that here: only blocks and decorating tools."),
    K + "bouwen.geen_kist": ("Kisten en machines openen kan niet op je bouwplekje.", "You can't open chests or machines on your build plot."),
    # --- the plot sign ---
    K + "bord.nummer": ("Bouwplekje %s", "Build plot %s"),
    K + "bord.gemaakt": ("Gemaakt door", "Made by"),
    K + "bord.vrij": ("nog vrij", "still free"),
    K + "bord.info": ("Bouwplekje %s: gemaakt door %s", "Build plot %s: made by %s"),
    K + "bord.info_vrij": ("Bouwplekje %s: nog van niemand", "Build plot %s: no one's yet"),
    # --- the Sint's letters ---
    K + "brief.kop": ("✉ Een brief van de Sint!", "✉ A letter from the Sint!"),
    K + "brief.aanhef": ("Lieve %s,", "Dear %s,"),
    K + "brief.trekking.1": ("de Pakjesguhs hebben de hele nacht geschud, en jij hebt het lootje getrokken van... %s!",
                             "the Pakjesguhs shook the hat all night, and you drew the name of... %s!"),
    K + "brief.trekking.2": ("Bouw iets moois voor hen op bouwplekje %s in het Surprisestraatje (daar krijg je creative), en maak een tekening.",
                             "Build something lovely for them on plot %s in Surprise Lane (you get creative there), and make a drawing."),
    K + "brief.trekking.3": ("Je hebt tot 5 december 18:00. En ssst... het is een geheimpje! Njeg.",
                             "You have until 5 December 18:00. And shh... it's a secret! Nyeg."),
    K + "brief.groet": ("Liefs, de Sint", "Love, the Sint"),
    K + "brief.geen.1": ("dit jaar stonden er niet genoeg namen in mijn Grote Boek om lootjes te trekken. Oei!",
                         "this year there weren't enough names in my Big Book to draw lots. Oops!"),
    K + "brief.geen.2": ("Daarom krijg je van mij %s pepernoten, omdat je zo lief was om je in te schrijven. Volgend jaar beter, vahoeg!",
                         "So here are %s pepernoten from me, because you were so sweet to sign up. Better luck next year, wahoog!"),
    # --- pakjesavond ---
    K + "zak.bonk": ("BONK BONK BONK! Er wordt op de deur geklopt...", "BONK BONK BONK! Someone is knocking on the door..."),
    K + "zak.voor_de_deur": ("...er staat een Zak van Sinterklaas voor de deur!", "...there's a Sinterklaas' Sack on the doorstep!"),
    K + "zak.gekregen": ("De Zak van Sinterklaas zit in je tas: een surprise van %s! Rechtsklik om hem open te maken.",
                         "Sinterklaas' Sack is in your bag: a surprise from %s! Right-click to open it."),
    K + "zak.tas_vol": ("De Zak van Sinterklaas wacht op je, maar je tas zit vol! Maak een plekje vrij, dan komt hij zo.",
                        "Sinterklaas' Sack is waiting for you, but your bag is full! Free up a spot and it'll come soon."),
    K + "zak.voor": ("Voor %s", "For %s"),
    K + "zak.van": ("Van %s", "From %s"),
    K + "zak.leeg": ("Briefje: \"Je surprise is leeg gebleven... maar hij is wel met liefde ingepakt!\"",
                     "Note: \"Your surprise stayed empty... but it was wrapped with love!\""),
    K + "zak.geen_tekening": ("Briefje: \"Er zit dit keer geen tekening bij. Njeg!\"", "Note: \"No drawing this time. Nyeg!\""),
    K + "zak.open": ("Alles uit de zak zit nu in je tas. Vahoeg!", "Everything from the sack is in your bag now. Wahoog!"),
    K + "zak.vol": ("Je tas is vol: de rest blijft in de zak.", "Your bag is full: the rest stays in the sack."),
    # --- the Surprise ---
    S + "hint": ("Rechtsklik: neerzetten  |  R of shift+scroll: draaien", "Right-click: put down  |  R or shift+scroll: turn"),
    S + "hint_kijk": ("Kijk naar de grond waar je surprise moet komen.", "Look at the ground where your surprise should go."),
    S + "leeg": ("Dit pakje is leeg: er valt niks neer te zetten. Je mag het lekker houden!",
                 "This present is empty: nothing to put down. You can keep it though!"),
    S + "niet_nu": ("Dat kan nu even niet.", "You can't do that right now."),
    S + "te_ver": ("Te ver weg! Kom wat dichterbij.", "Too far away! Come a bit closer."),
    S + "geplaatst": ("De surprise van %s staat! Shift+rechtsklik pakt hem weer op.", "The surprise from %s is up! Shift+right-click picks it up again."),
    S + "fout.buiten": ("Hier past hij niet: te hoog of te laag.", "It doesn't fit here: too high or too low."),
    S + "fout.laden": ("Hier is de wereld nog niet geladen.", "The world isn't loaded here yet."),
    S + "fout.plein": ("Niet op het Winterplein! Zet hem thuis neer.", "Not on the Winter Square! Put it down at home."),
    S + "fout.in_de_weg": ("Er staat iets in de weg: %s. Hij heeft een vrij plekje van 7 bij 7 nodig.",
                           "Something is in the way: %s. It needs a free 7 by 7 spot."),
    S + "fout.beschermd": ("Hier mag je niet bouwen.", "You can't build here."),
    S + "fout.iemand": ("%s staat in de weg!", "%s is in the way!"),
    S + "niet_van_jou": ("Deze surprise is van iemand anders: alleen wie hem neerzette kan hem oppakken.",
                         "This surprise belongs to someone else: only whoever put it down can pick it up."),
    S + "tas_vol": ("Je tas zit vol! Maak een plekje vrij om je surprise op te pakken.", "Your bag is full! Free up a spot to pick up your surprise."),
    S + "opgepakt": ("De surprise van %s zit weer in je tas.", "The surprise from %s is back in your bag."),
    S + "info": ("Een surprise van %s", "A surprise from %s"),
    S + "info.eigen": ("Jouw surprise van %s. Shift+rechtsklik pakt hem op.", "Your surprise from %s. Shift+right-click picks it up."),
    S + "niet_slopen": ("Een surprise sloop je niet! Shift+rechtsklik pakt hem in zijn geheel op.",
                        "You don't break a surprise! Shift+right-click picks the whole thing up."),
}

UITLEG = {
    "guhwinter_grote_boek": (
        "Het Grote Boek van Sinterklaas ligt op een lessenaar naast zijn stoel op het Winterplein (Guhwinter, de Sinterklaastijd). "
        "Rechtsklik erop en de bladzijde staat in je chat: hoeveel namen er al in staan, en een knop om je in te schrijven voor "
        "lootjes trekken (of toch maar niet).\n\nInschrijven kan tot en met 30 november 23:59, met maximaal 8 namen: wie het eerst "
        "komt, het eerst maalt. Op 1 december trekt de Sint en krijg je een brief met het lootje dat jij hebt (nooit jezelf). Daarna "
        "staat in het boek ook je bouwplekje en een knop voor je tekening.",
        "The Big Book of Sinterklaas lies on a lectern next to his chair on the Winter Square (Guhwinter, the Sinterklaas time). "
        "Right-click it and the page shows in your chat: how many names are in it, and a button to sign up for the drawing of names "
        "(or never mind).\n\nYou can sign up through 30 November 23:59, with at most 8 names: first come, first served. On 1 December "
        "the Sint draws and you get a letter with the name you drew (never yourself). After that the book also shows your build "
        "plot and a button for your drawing."),
    "guhwinter_bouwbord": (
        "Het naambordje van een bouwplekje in het Surprisestraatje op het Winterplein. Er staat op wie er bouwt (\"Gemaakt door\"), "
        "maar nooit voor wie: dat blijft een geheimpje tot pakjesavond.\n\nVan 1 december tot 5 december 18:00 bouw je op je eigen "
        "plekje in creative: alles wat mooi is mag, maar geen kisten, machines, redstone of spawners. Je eigen spullen liggen zolang "
        "veilig bewaard en komen terug zodra je het plekje uit loopt.\n\n"
        "Rechtsklik op het bordje van je eigen plekje opent je tekening voor je lootje. Bij een ander bordje zie je wie daar bouwt.",
        "The name sign of a build plot in Surprise Lane on the Winter Square. It says who builds there (\"Made by\"), but never for "
        "whom: that stays a secret until pakjesavond.\n\nFrom 1 December until 5 December 18:00 you build on your own plot in "
        "creative: anything pretty goes, but no chests, machines, redstone or spawners. Your own things are kept safe meanwhile and "
        "come back as soon as you walk out of the plot.\n\n"
        "Right-click the sign of your own plot to open the drawing for your name. On someone else's sign you see who builds there."),
    "guhwinter_surprise": (
        "Je lootjes-cadeau van Guhwinter: het bouwwerk van 7 bij 7 bij 7 dat iemand op 5 december om 18:00 op zijn bouwplekje had "
        "staan, ingepakt als één geheel. Je krijgt het op pakjesavond in de Zak van Sinterklaas. Het smelt nooit.\n\n"
        "Hou hem vast en je ziet een doorzichtig voorbeeld waar hij komt (roze = past, rood = er staat iets in de weg). Draai hem met "
        "R of shift+scroll, en rechtsklik om hem neer te zetten. Hij heeft een vrij plekje nodig en kan niet op het Winterplein of in "
        "een beschermd gebied.\n\n"
        "Het is één object: je loopt erop en botst ertegen, maar niks erin werkt (deuren, knoppen en bedden zijn versiering) en je "
        "kunt er geen blokjes uit slopen. Shift+rechtsklik door wie hem neerzette maakt er weer een item van, om hem ergens anders "
        "neer te zetten.",
        "Your Guhwinter name-drawing present: the 7 by 7 by 7 build that someone had on their plot at 18:00 on 5 December, wrapped "
        "up as one piece. You get it on pakjesavond in Sinterklaas' Sack. It never melts.\n\n"
        "Hold it to see a see-through preview of where it goes (pink = it fits, red = something is in the way). Turn it with R or "
        "shift+scroll, and right-click to put it down. It needs a free spot and can't go on the Winter Square or in a protected "
        "area.\n\n"
        "It is one object: you walk on it and bump into it, but nothing inside works (doors, buttons and beds are decoration) and "
        "you can't break blocks out of it. Shift+right-click by whoever put it down turns it back into an item, to put it "
        "somewhere else."),
}

# =====================================================================================================================
# textures
# =====================================================================================================================


def grid(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


ROOD, ROOD_D, GOUD, PAPIER, PAPIER_D, INKT = (0xB2, 0x2A, 0x3A, 255), (0x7A, 0x18, 0x26, 255), (0xE8, 0xB8, 0x4A, 255), \
    (0xFF, 0xF8, 0xE6, 255), (0xE8, 0xDC, 0xC0, 255), (0x8A, 0x7A, 0x90, 255)


def pagina():
    """The open book seen from above: two pages, little lines of names, a red ribbon bookmark."""
    return grid([
        "rrrrrrrrrrrrrrrr",
        "rpppppppdppppppr",
        "rpiiiiipdpiiiipr",
        "rpppppppdppppppr",
        "rpiiiippdpiiiiir",
        "rpppppppdppppppr",
        "rpiiiiipdpiiippr",
        "rpppppppdppppppr",
        "rpiiipppdpiiiipr",
        "rpppppppdppppppr",
        "rpiiiiipdpiipppr",
        "rpppppppdppppppr",
        "rppppppLdppppppr",
        "rppppppLdppppppr",
        "rpppppLLdppppppr",
        "rrrrrrrrrrrrrrrr"], {"r": ROOD_D, "p": PAPIER, "d": PAPIER_D, "i": INKT, "L": ROOD})


def kaft():
    """The cover: red leather with gold corners and a gold band."""
    return grid([
        "gRRRRRRRRRRRRRRg",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "gggggggggggggggg",
        "gggggggggggggggg",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "RrrrrrrrrrrrrrrR",
        "gRRRRRRRRRRRRRRg"], {"R": ROOD_D, "r": ROOD, "g": GOUD})


def icoon_surprise():
    """A wrapped present: pink paper with white dots, a white ribbon and a bow."""
    return grid([
        "................",
        "....ww....ww....",
        "...wWWw..wWWw...",
        "...wWWwwwwWWw...",
        "....wwwWWwww....",
        ".ppppppwwpppppp.",
        ".pPpppPwwpPpppP.",
        ".ppppppwwpppppp.",
        ".kkkkkkwwkkkkkk.",
        ".kpppKpwwpKpppk.",
        ".kpKpppwwppppKk.",
        ".kppppKwwKppppk.",
        ".kpKpppwwpppKpk.",
        ".kppppppwpKpppk.",
        ".kkkkkkwwkkkkkk.",
        "................"], {
        "p": (0xF7, 0x9C, 0xC8, 255), "P": (0xFF, 0xD2, 0xE8, 255), "k": (0xC8, 0x5C, 0x96, 255), "K": (0xFF, 0xE6, 0xF2, 255),
        "w": (0xFF, 0xFF, 0xFF, 255), "W": (0xE0, 0xE6, 0xFF, 255)})


def icoon_zak():
    """The Sint's sack: red-brown jute, a golden rope, a present peeking out."""
    return grid([
        "................",
        "......pPp.......",
        ".....pwwwp......",
        "......gGg.......",
        ".....gGgGg......",
        "....rrrrrrr.....",
        "...rRrrrrrRr....",
        "..rRrrrrrrrRr...",
        "..rrrrrrrrrrr...",
        ".rRrrrrrrrrrRr..",
        ".rrrrrdrrrrrrr..",
        ".rrrrrrrrrdrrr..",
        ".rRrrdrrrrrrRr..",
        "..rrrrrrrrrrr...",
        "...rrrrrrrrr....",
        "................"], {
        "r": (0xA8, 0x34, 0x2E, 255), "R": (0xC8, 0x52, 0x46, 255), "d": (0x82, 0x24, 0x22, 255), "g": (0xD8, 0xA8, 0x40, 255),
        "G": (0xF0, 0xCC, 0x60, 255), "p": (0xF7, 0x9C, 0xC8, 255), "P": (0xFF, 0xFF, 0xFF, 255), "w": (0xFF, 0xD2, 0xE8, 255)})


# =====================================================================================================================
# models
# =====================================================================================================================
def boek_model():
    """The vanilla lectern (its own textures) with a big open book lying on the slanted top."""
    schuin = {"angle": -22.5, "axis": "x", "origin": [8, 8, 8]}
    return {
        "parent": "minecraft:block/block",
        "display": {"firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}},
        "textures": {
            "particle": "minecraft:block/lectern_sides", "bottom": "minecraft:block/oak_planks", "base": "minecraft:block/lectern_base",
            "front": "minecraft:block/lectern_front", "sides": "minecraft:block/lectern_sides", "top": "minecraft:block/lectern_top",
            "pagina": "guhs:block/guhwinter_grote_boek_pagina", "kaft": "guhs:block/guhwinter_grote_boek_kaft"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
                "north": {"uv": [0, 14, 16, 16], "texture": "#base", "cullface": "north"},
                "east": {"uv": [0, 6, 16, 8], "texture": "#base", "cullface": "east"},
                "south": {"uv": [0, 6, 16, 8], "texture": "#base", "cullface": "south"},
                "west": {"uv": [0, 6, 16, 8], "texture": "#base", "cullface": "west"},
                "up": {"uv": [0, 0, 16, 16], "rotation": 180, "texture": "#base"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"}}},
            {"from": [4, 2, 4], "to": [12, 15, 12], "faces": {
                "north": {"uv": [0, 0, 8, 13], "texture": "#front"},
                "east": {"uv": [2, 16, 15, 8], "rotation": 90, "texture": "#sides"},
                "south": {"uv": [8, 3, 16, 16], "texture": "#front"},
                "west": {"uv": [2, 8, 15, 16], "rotation": 90, "texture": "#sides"}}},
            {"from": [0.0125, 12, 3], "to": [15.9875, 16, 16], "rotation": schuin, "faces": {
                "north": {"uv": [0, 0, 16, 4], "texture": "#sides"},
                "east": {"uv": [0, 4, 13, 8], "texture": "#sides"},
                "south": {"uv": [0, 4, 16, 8], "texture": "#sides"},
                "west": {"uv": [0, 4, 13, 8], "texture": "#sides"},
                "up": {"uv": [0, 1, 16, 14], "rotation": 180, "texture": "#top"},
                "down": {"uv": [0, 0, 16, 13], "texture": "#bottom"}}},
            # the big book: a cover, and the open pages on top of it
            {"from": [0.5, 16, 3.5], "to": [15.5, 16.75, 15.5], "rotation": schuin, "faces": {
                "north": {"uv": [0, 7, 16, 8], "texture": "#kaft"},
                "east": {"uv": [0, 7, 12, 8], "texture": "#kaft"},
                "south": {"uv": [0, 7, 16, 8], "texture": "#kaft"},
                "west": {"uv": [0, 7, 12, 8], "texture": "#kaft"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#kaft"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#kaft"}}},
            {"from": [1.25, 16.75, 4.25], "to": [14.75, 17.75, 14.75], "rotation": schuin, "faces": {
                "north": {"uv": [0, 1, 16, 2], "texture": "#pagina"},
                "east": {"uv": [1, 1, 15, 2], "texture": "#pagina"},
                "south": {"uv": [0, 1, 16, 2], "texture": "#pagina"},
                "west": {"uv": [1, 1, 15, 2], "texture": "#pagina"},
                "up": {"uv": [0, 0, 16, 16], "rotation": 180, "texture": "#pagina"}}},
        ]}


# =====================================================================================================================
# writing
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


def _tag(path, values):
    """Appends to a tag file (keeps what is there; entries may be {"id", "required": false})."""
    full = os.path.join(D, *path.split("/")) + ".json"
    data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    _w(full, data)


def _opt(i):
    return {"id": i, "required": False}


VERBODEN = [
    # pistons, explosives, the "invisible" blocks
    "minecraft:piston", "minecraft:sticky_piston", "minecraft:piston_head", "minecraft:moving_piston", "minecraft:tnt",
    "minecraft:barrier", "minecraft:light", "minecraft:structure_void",
    # spawners, command and structure blocks (they have a block entity too: listed for the wiki)
    "minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault", "minecraft:command_block", "minecraft:chain_command_block",
    "minecraft:repeating_command_block", "minecraft:structure_block", "minecraft:jigsaw", _opt("minecraft:test_block"),
    _opt("minecraft:test_instance_block"),
    # working machines and blocks with an inventory
    "minecraft:hopper", "minecraft:dropper", "minecraft:dispenser", "minecraft:crafter", "minecraft:observer", "minecraft:furnace",
    "minecraft:smoker", "minecraft:blast_furnace", "minecraft:brewing_stand", "minecraft:chest", "minecraft:trapped_chest",
    "minecraft:barrel", "minecraft:ender_chest", "#minecraft:shulker_boxes", "minecraft:lectern", "minecraft:jukebox",
    "minecraft:beacon", "minecraft:conduit", "minecraft:campfire", "minecraft:soul_campfire", "minecraft:decorated_pot",
    "minecraft:chiseled_bookshelf", _opt("#minecraft:wooden_shelves"), "minecraft:beehive", "minecraft:bee_nest",
    "minecraft:enchanting_table", "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil", "minecraft:composter",
    "minecraft:respawn_anchor", "minecraft:cauldron", "minecraft:water_cauldron", "minecraft:lava_cauldron",
    "minecraft:powder_snow_cauldron", "minecraft:creaking_heart", "minecraft:suspicious_sand", "minecraft:suspicious_gravel",
    # redstone components (anything that gives a signal is refused anyway)
    "minecraft:redstone_wire", "minecraft:repeater", "minecraft:comparator", "minecraft:redstone_torch", "minecraft:redstone_wall_torch",
    "minecraft:redstone_block", "minecraft:lever", "#minecraft:buttons", "#minecraft:pressure_plates", "minecraft:tripwire_hook",
    "minecraft:daylight_detector", "minecraft:target", "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor",
    "minecraft:sculk_shrieker", "minecraft:sculk_catalyst", "minecraft:lightning_rod", _opt("#minecraft:lightning_rods"),
    "minecraft:powered_rail", "minecraft:detector_rail", "minecraft:activator_rail",
    # things that hatch, grow out of the plot, burn or teleport
    "minecraft:turtle_egg", "minecraft:frogspawn", "minecraft:sniffer_egg", _opt("minecraft:dried_ghast"),
    "minecraft:infested_stone", "minecraft:infested_cobblestone", "minecraft:infested_stone_bricks",
    "minecraft:infested_mossy_stone_bricks", "minecraft:infested_cracked_stone_bricks", "minecraft:infested_chiseled_stone_bricks",
    "minecraft:infested_deepslate", "#minecraft:saplings", "minecraft:bamboo", "minecraft:bamboo_sapling", "minecraft:chorus_flower",
    "minecraft:crimson_fungus", "minecraft:warped_fungus", "minecraft:fire", "minecraft:soul_fire", "minecraft:nether_portal",
    "minecraft:end_portal", "minecraft:end_gateway",
]


def bestanden():
    # textures
    _save(pagina(), "block", "guhwinter_grote_boek_pagina.png")
    _save(kaft(), "block", "guhwinter_grote_boek_kaft.png")
    _save(icoon_surprise(), "item", "guhwinter_surprise.png")
    _save(icoon_zak(), "item", "guhwinter_zak.png")
    # Het Grote Boek
    _w(os.path.join(A, "models", "block", "guhwinter_grote_boek.json"), boek_model())
    _w(os.path.join(A, "blockstates", "guhwinter_grote_boek.json"), {"variants": {
        f"facing={f}": {"model": "guhs:block/guhwinter_grote_boek", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    _w(os.path.join(A, "models", "item", "guhwinter_grote_boek.json"), {"parent": "guhs:block/guhwinter_grote_boek"})
    # the plot sign: a spruce sign (the vanilla sign renderer draws it)
    _w(os.path.join(A, "models", "block", "guhwinter_bouwbord.json"), {"textures": {"particle": "minecraft:block/spruce_planks"}})
    _w(os.path.join(A, "blockstates", "guhwinter_bouwbord.json"), {"variants": {"": {"model": "guhs:block/guhwinter_bouwbord"}}})
    _w(os.path.join(A, "models", "item", "guhwinter_bouwbord.json"), {"parent": "minecraft:item/generated",
                                                                       "textures": {"layer0": "minecraft:item/spruce_sign"}})
    # the Surprise blocks: invisible (the kern's renderer draws the gift)
    for naam in ("guhwinter_surprise", "guhwinter_surprise_deel"):
        _w(os.path.join(A, "models", "block", naam + ".json"), {"textures": {"particle": "minecraft:block/pink_wool"}})
        _w(os.path.join(A, "blockstates", naam + ".json"), {"variants": {"": {"model": "guhs:block/" + naam}}})
    # items
    for naam in ("guhwinter_surprise", "guhwinter_zak"):
        _w(os.path.join(A, "models", "item", naam + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + naam}})
    for naam in ("guhwinter_grote_boek", "guhwinter_bouwbord", "guhwinter_surprise", "guhwinter_zak"):
        _w(os.path.join(A, "items", naam + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + naam}})
    # loot: the book and the sign are unbreakable fixtures, but drop themselves for an op who breaks them
    for naam in ("guhwinter_grote_boek", "guhwinter_bouwbord"):
        _w(os.path.join(D, "guhs", "loot_table", "blocks", naam + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:" + naam}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # tags
    _tag("guhs/tags/block/guhwinter_lootje_verboden", VERBODEN)
    _tag("guhs/tags/block/guhwinter_lootje_mag", [])
    for t in ("minecraft/tags/block/wither_immune", "minecraft/tags/block/dragon_immune"):
        _tag(t, ["guhs:guhwinter_surprise", "guhs:guhwinter_surprise_deel", "guhs:guhwinter_grote_boek", "guhs:guhwinter_bouwbord"])


def engels():
    """The English overlay of these keys and its chunk in tools/lang/chunks.json (added once, as text: no reformatting)."""
    _w(os.path.join("tools", "lang", "en", CHUNK + ".json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    p = os.path.join("tools", "lang", "chunks.json")
    with open(p, encoding="utf-8") as f:
        src = f.read()
    if f'"id": "{CHUNK}"' in src:
        return
    prefixes = [K, "item.guhs.guhwinter_surprise", "item.guhs.guhwinter_zak", "item.guhs.guhwinter_grote_boek", "item.guhs.guhwinter_bouwbord",
                "block.guhs.guhwinter_grote_boek", "block.guhs.guhwinter_bouwbord", "block.guhs.guhwinter_surprise",
                "key.guhs.guhwinter_surprise_draai"]
    blok = ('    {\n      "id": "%s",\n      "title": "1.10.10 Guhwinter, slice lootjes: Het Grote Boek, the draw, the creative plots, '
            'the Surprise, the Zak van Sinterklaas",\n      "prefixes": [\n%s\n      ]\n    }' % (
                CHUNK, ",\n".join(f'        "{x}"' for x in prefixes)))
    i = src.rstrip().rfind("]")
    j = src.rstrip()[:i].rstrip()
    assert j.endswith("}"), "chunks.json: unexpected end"
    src = j + ",\n" + blok + "\n  ]\n}\n"
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)


BLOKUITLEG = os.path.join("tools", "wiki_site", "blokuitleg.py")
_MARK = "# <guhwinter_lootjes> (1.10.10, tools/features/guhwinter_lootjes.py: rewritten on every run)"


def blokuitleg():
    with open(BLOKUITLEG, encoding="utf-8") as f:
        src = f.read()
    src = re.sub(r"\n" + re.escape(_MARK) + r".*?# </guhwinter_lootjes>\n", "\n", src, flags=re.S)
    regels = [_MARK] + [f"UITLEG[{k!r}] = dict(nl={nl!r}, en={en!r})" for k, (nl, en) in UITLEG.items()] + ["# </guhwinter_lootjes>"]
    if not src.endswith("\n"):
        src += "\n"
    with open(BLOKUITLEG, "w", encoding="utf-8", newline="\n") as f:
        f.write(src + "\n".join(regels) + "\n")


def build(h):
    bestanden()
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    engels()
    blokuitleg()


def _direct():
    bestanden()
    engels()
    blokuitleg()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _w(p, d)
    print(f"guhwinter_lootjes: 4 blocks, 4 items, {len(TEKSTEN)} texts, {len(UITLEG)} blokuitleg")


if __name__ == "__main__":
    _direct()
