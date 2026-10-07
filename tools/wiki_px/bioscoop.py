"""
The wiki part of the guhpixel slice "bioscoop": the Guhbioscoop (projector, screen, seats, popcornmachine) and its nine films.

body(w) returns the HTML of this slice's part, built with the helpers of tools/make_wiki.py; every text is written twice,
(English, Dutch); the English is hand-written here and follows tools/lang/GLOSSARY.md and tools/lang/glossary_px/bioscoop.md.
renders(r) adds the pictures docs/wiki/img/guhbioscoop_*.png: the four blocks and one still of every film (drawn by the
Python preview of the film generator, the same rules as the game).
"""
import os

FILMS = [
    ("skyblok", "Skyblok: The Movie", "Skyblok: De Film", "Finish Skyblok in the Guhpixel lobby", "Speel Skyblok uit in de Guhpixel-lobby", 480),
    ("bedwars", "Bedwars: The Movie", "Bedwars: De Film", "Finish Bedwars in the Guhpixel lobby", "Speel Bedwars uit in de Guhpixel-lobby", 600),
    ("vadsnite", "Chonknite: The Movie", "Vadsnite: De Film", "Finish Chonknite in the Guhpixel lobby", "Speel Vadsnite uit in de Guhpixel-lobby", 420),
    ("guhmon", "Guhmon: The Big Battle", "Guhmon: Het Grote Gevecht", "Win the Guhmon Battle in the Guhpixel lobby",
     "Win het Guhmon-gevecht in de Guhpixel-lobby", 380),
    ("bzg", "Farmer Wants a Guh", "Boer zoekt Guh", "Finish Farmer Wants a Guh in the Guhpixel lobby", "Speel Boer zoekt Guh uit in de Guhpixel-lobby", 330),
    ("among", "Among Guhs: The Movie", "Among Guhs: De Film", "Finish the Among Guhs parody round with Captain Guh",
     "Speel de Among Guhs-parodieronde uit bij de Kapitein-guh", 200),
    ("balto", "Baltoguh: The Movie", "Baltoguh: De Film", "Become the Hero of Nomguh (the story of Baltoguh)",
     "Word de Held van Nomguh (het verhaal van Baltoguh)", 700),
    ("mewtwo", "Guhtwo: The Movie", "Guhtwo: De Film", "Become a Friend of Guhtwo (the story of Clone Island)",
     "Word Vriend van Guhtwo (het verhaal van het kloon-eiland)", 580),
    ("stitch626", "626: Ohana on Guhwai'i", "626: Ohana op Guhwai'i", "Become an Ohana Guh (the story of 626-guh)",
     "Word Ohana-guh (het verhaal van 626-guh)", 760),
]


def body(w):
    uit = [w.h3("The Guh Cinema", "De Guhbioscoop")]
    uit.append(w.p(
        "Your own movie theater for at home. Buy a <b>Guh Cinema Set</b> from the Shopkeeper Guh in the Guhpixel lobby: one projector, "
        "28 pieces of Cinema Screen and four Cinema Seats. Build a screen of Cinema Screen (any full rectangle up to 7 wide and 4 high: "
        "the bigger, the bigger the picture), put the projector within 16 blocks in front of it with the lens toward the white side, "
        "right-click the projector and pick one of your movies.",
        "Je eigen bioscoop voor thuis. Koop bij de Verkoper-guh in de Guhpixel-lobby een <b>Guhbioscoop-set</b>: één projector, 28 stukken "
        "Bioscoopdoek en vier Bioscoopstoeltjes. Bouw een scherm van Bioscoopdoek (elke volle rechthoek tot 7 breed en 4 hoog: hoe groter, "
        "hoe groter het beeld), zet de projector binnen 16 blokken ervoor met de lens naar de witte kant, rechtsklik op de projector en kies "
        "een van je films."))
    uit.append(w.entry(w.img("guhbioscoop_projector", "Guh Cinema Projector"), "Guh Cinema Projector", "Guhbioscoop-projector", w.p(
        "Shows the list of movies: yours have a Play button, the others say how to get them. Everybody near the screen sees and hears the "
        "same movie at the same moment. Whoever is within 24 blocks when it ends has watched it.",
        "Laat de lijst met films zien: die van jou hebben een knop Speel, bij de andere staat hoe je ze krijgt. Iedereen bij het scherm ziet "
        "en hoort dezelfde film op hetzelfde moment. Wie binnen 24 blokken is als hij afloopt, heeft hem gezien.")))
    uit.append(w.entry(w.img("guhbioscoop_stoeltje", "Cinema Seat"), "Cinema Seat", "Bioscoopstoeltje", w.p(
        "Right-click to sit down. When a movie starts, your tamed guhs nearby (within 16 blocks of the projector, with wandering on) pick a "
        "free seat within 12 blocks themselves. They laugh, jump at the scary bits and usually doze off before the end. Afterwards they "
        "clap, get a little heart and write about the movie in their diary.",
        "Rechtsklik om te gaan zitten. Begint er een film, dan zoeken je getemde guhs in de buurt (binnen 16 blokken van de projector, met "
        "rondvadsen aan) zelf een vrij stoeltje binnen 12 blokken. Ze lachen, schrikken bij de enge stukjes en dommelen meestal voor het "
        "einde in. Daarna klappen ze, krijgen ze een hartje en schrijven ze over de film in hun dagboek.")))
    uit.append(w.entry(w.img("guhbioscoop_popcornmachine", "Popcorn Machine"), "Popcorn Machine", "Popcornmachine", w.p(
        "With one within 12 blocks of the projector every guh in the audience holds a tub of popcorn during the movie (and throws it in the "
        "air when startled). Right-click it for a Tub of Popcorn of your own, once a minute.",
        "Staat er eentje binnen 12 blokken van de projector, dan heeft elke guh in de zaal een bakje popcorn bij de film (en gooit het in de "
        "lucht als hij schrikt). Rechtsklik erop voor je eigen Bakje popcorn, één keer per minuut.")))
    uit.append(w.h3("In the Guhpixel shop", "In de Guhpixel-winkel"))
    uit.append(w.table([("Offer", "Aanbod"), ("Coins", "Muntjes"), ("What you get", "Wat je krijgt")], [
        [("Guh Cinema Set", "Guhbioscoop-set"), "250", ("1 projector, 28 Cinema Screen, 4 Cinema Seats (a second set: 125)",
                                                         "1 projector, 28 Bioscoopdoek, 4 Bioscoopstoeltjes (een tweede set: 125)")],
        [("Cinema Seat", "Bioscoopstoeltje"), "15", ("one extra seat", "één extra stoeltje")],
        [("Popcorn Machine", "Popcornmachine"), "60", ("popcorn for the whole audience", "popcorn voor de hele zaal")],
        [("Cinema Screen (4 pieces)", "Bioscoopdoek (4 stuks)"), "8", ("four extra pieces of screen", "vier extra stukken doek")],
    ]))
    uit.append(w.h3("The nine movies", "De negen films"))
    uit.append(w.p(
        "Movies are per player: the list on the projector shows YOUR movies. Each lasts under a minute, has Dutch and English subtitles, "
        "and ends the way everything ends in the Guhmension.",
        "Films zijn per speler: de lijst op de projector laat JOUW films zien. Elke film duurt nog geen minuut, heeft Nederlandse en "
        "Engelse ondertitels en loopt af zoals alles in de Guhmensie afloopt."))
    uit.append('<div class="gallery">' + "".join(w.img(f"guhbioscoop_film_{fid}", en) for fid, en, *_ in FILMS) + "</div>")
    uit.append(w.table([("Movie", "Film"), ("How to get it", "Zo krijg je hem")],
                       [[(en, nl), (hoe_en, hoe_nl)] for _, en, nl, hoe_en, hoe_nl, _ in FILMS]))
    return "".join(uit)


def renders(r):
    import sys
    from PIL import Image
    if "tools" not in sys.path:
        sys.path.insert(0, "tools")
    for naam in ("projector", "doek", "stoeltje", "popcornmachine"):
        try:
            beeld = r.render(r.model_quads(f"guhs:block/guhbioscoop_{naam}"), 150 if naam != "doek" else 30, -25, 320)
            beeld.save(os.path.join(r.OUT, f"guhbioscoop_{naam}.png"))
            beeld.resize((64, 64), Image.LANCZOS).save(os.path.join(r.OUT, f"icon_guhbioscoop_{naam}.png"))
        except Exception as e:      # (a missing model must not stop the other slices' pictures)
            print("no render for guhbioscoop_" + naam, e)
    try:
        r.item_icon("guhs:item/guhbioscoop_popcorn").save(os.path.join(r.OUT, "icon_guhbioscoop_popcorn.png"))
    except Exception as e:
        print("no icon for guhbioscoop_popcorn", e)
    from features import guhpixel_bioscoop_film as film
    from features import guhpixel_bioscoop_films as films
    tijden = {fid: t for fid, *_, t in FILMS}
    for f in films.alle():
        film.beeld(f, tijden.get(f.id, 300), schaal=4).convert("RGB").save(os.path.join(r.OUT, f"guhbioscoop_film_{f.id}.png"))
    print("rendered the guhbioscoop pictures")
