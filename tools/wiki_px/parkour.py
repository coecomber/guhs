"""
The wiki part of the guhpixel slice "parkour": the Guh-parkour (its own section "Guh Parkour" / "Guh-parkour").

body(w) returns the HTML, built with the helpers of tools/make_wiki.py (w.h3, w.p, w.ul, w.table, w.entry, w.img,
w.recipe_card); every text is written twice, (English, Dutch), the English by hand following tools/lang/GLOSSARY.md and
tools/lang/glossary_px/parkour.md. renders(r) adds the pictures docs/wiki/img/guhparkour_*.png (r is tools/wiki_renders.py)
and the few vanilla ingredient icons the recipe cards need that the wiki did not have yet.
"""
import os

# id, English name, Dutch name, what a guh does there (en, nl), lap time it costs (en, nl)
STUKKEN = [
    ("guhparkour_horde", "Guh Hurdle", "Guh-horde",
     ("A short run-up and a jump over the bar.", "Een korte aanloop en een sprong over de lat."),
     ("about 1 second", "ongeveer 1 seconde")),
    ("guhparkour_springplank", "Guh Springboard", "Guh-springplank",
     ("Up the little ramp, boing, and a long way through the air. One way only (up the ramp); keep three blocks free behind it.",
      "Het plankje op, boing, en een heel eind door de lucht. Alleen in één richting (het plankje op); laat erachter drie blokken vrij."),
     ("about 1.5 seconds", "ongeveer 1,5 seconde")),
    ("guhparkour_kruiptunnel", "Crawl Tunnel", "Kruiptunnel",
     ("Three blocks of cloth tunnel. The guh disappears into it and you see a bump travel through.",
      "Drie blokken stoffen tunnel. De guh verdwijnt erin en je ziet een bobbel erdoorheen lopen."),
     ("about 2 seconds", "ongeveer 2 seconden")),
    ("guhparkour_slalompaaltjes", "Slalom Poles", "Slalompaaltjes",
     ("Three poles in a row: left, right, left.", "Drie paaltjes op een rij: links, rechts, links."),
     ("about 1.5 seconds", "ongeveer 1,5 seconde")),
    ("guhparkour_evenwichtsbalk", "Balance Beam", "Evenwichtsbalk",
     ("Hop on, shuffle across carefully, hop off. One time in four the guh falls off halfway (oops!), sits there dazed for a moment and climbs "
      "back on. It never hurts.",
      "Erop springen, voorzichtig eroverheen schuifelen, eraf springen. Eén op de vier keer valt de guh er halverwege af (oeps!), zit even "
      "beduusd te kijken en klimt er weer op. Het doet nooit pijn."),
     ("about 2.5 seconds, 4.5 with a tumble", "ongeveer 2,5 seconde, 4,5 met een tuimeling")),
    ("guhparkour_knabbeltafeltje", "Nibble Table", "Knabbeltafeltje",
     ("The mandatory pit stop: the guh stops and eats a nibble first. Slowly.",
      "De verplichte pitstop: de guh stopt en eet eerst een knabbel. Rustig aan."),
     ("4 seconds", "4 seconden")),
]


def body(w):
    intro = w.p(
        "With the <b>Guh Parkour</b> you build an obstacle course for your own guhs. You lay out a route past toys and obstacles, put up to "
        "<b>four</b> of your guhs on it and off they trot: from piece to piece, really using every one of them, and then all over again, until "
        "you take them off. A scoreboard keeps the laps and the best lap time of every guh. It works anywhere, in any dimension you can build in. "
        "Nothing here can hurt a guh, and failing costs nothing: a piece a guh cannot reach is simply skipped.",
        "Met het <b>Guh-parkour</b> bouw je een hindernisbaan voor je eigen guhs. Je zet een route uit langs speelgoed en hindernissen, zet er "
        "tot <b>vier</b> van je guhs op en daar draven ze: van stuk naar stuk, ze gebruiken ze allemaal echt, en dan gewoon opnieuw, tot je ze "
        "eraf haalt. Een scorebord houdt van elke guh de rondjes en de beste rondetijd bij. Het werkt overal, in elke dimensie waar je mag bouwen. "
        "Niets kan een guh hier pijn doen en mislukken kost niets: een stuk waar een guh niet bij kan slaat hij gewoon over.")
    palen = w.entry(w.img("guhparkour_baan", "A Guh Parkour course"), "Building a course", "Een parkour bouwen", w.ul([
        ("Hold the <b>Start Post</b> in your hand and <b>right-click</b> the toys and obstacles one by one, in the order the guhs should take them: "
         "1, 2, 3 ... and the <b>Finish Post</b> last. A pink dotted line shows the route while you do it.",
         "Houd het <b>Startpaaltje</b> in je hand en <b>rechtsklik</b> het speelgoed en de hindernissen één voor één aan, in de volgorde waarin "
         "de guhs ze moeten nemen: 1, 2, 3 ... en als laatste het <b>Finishpaaltje</b>. Een roze stippellijn laat de route zien terwijl je bezig bent."),
        ("Then <b>place</b> the Start Post where the lap should begin. Every piece must be within <b>32 blocks</b> of it; a route holds at most "
         "<b>16 pieces</b> (the Finish Post does not count).",
         "Zet daarna het Startpaaltje <b>neer</b> waar het rondje moet beginnen. Elk stuk moet binnen <b>32 blokken</b> daarvan staan; in een route "
         "passen hooguit <b>16 stukken</b> (het Finishpaaltje telt niet mee)."),
        ("Forgot one? <b>Sneak + right-click</b> the placed post (or use <i>Click pieces</i> in its screen) and click more pieces; click the post "
         "again when you are done. Clicking a piece a second time takes it out of the route.",
         "Eentje vergeten? <b>Sluip + rechtsklik</b> op het geplaatste paaltje (of gebruik <i>Stukken aanklikken</i> in zijn scherm) en klik meer "
         "stukken aan; klik weer op het paaltje als je klaar bent. Een stuk nog een keer aanklikken haalt het uit de route."),
        ("<b>Right-click</b> the post for its screen: the numbered list of pieces (move a piece earlier or later, or take it out), your guhs "
         "(pick one and put it on the route, or take it off) and the scores. Other players may look, only the owner changes things.",
         "<b>Rechtsklik</b> op het paaltje voor zijn scherm: de genummerde lijst met stukken (zet een stuk eerder of later, of haal het eruit), "
         "je guhs (kies er een en zet hem op de route, of haal hem eraf) en de scores. Andere spelers mogen kijken, alleen de eigenaar verandert iets."),
        ("Break the Start Post and it <b>remembers its route</b>: put it down again and the course is back.",
         "Breek je het Startpaaltje af, dan <b>onthoudt het zijn route</b>: zet het weer neer en het parkour is terug."),
        ("Without a Finish Post the course still works: the lap time then stops at the end of the last piece.",
         "Zonder Finishpaaltje werkt het parkour ook: de rondetijd stopt dan aan het eind van het laatste stuk.")]))
    rijen = [[w.icon(i, en), (f"<b>{en}</b>", f"<b>{nl}</b>"), doet, tijd] for i, en, nl, doet, tijd in STUKKEN]
    rijen += [
        [w.icon("guh_wip", "Guh Seesaw"), ("<b>Guh Seesaw</b> (the existing toy)", "<b>Guh-wip</b> (het bestaande speeltje)"),
         ("The guh sits on it for a little while. Two guhs on one route seesaw together.", "De guh gaat er even op zitten. Twee guhs op één route wippen samen."),
         ("5 seconds", "5 seconden")],
        [w.icon("guh_schommel", "Guh Swing"), ("<b>Guh Swing</b>", "<b>Guh-schommel</b>"),
         ("A short swing.", "Even schommelen."), ("5 seconds", "5 seconden")],
        [w.icon("guh_glijbaantje", "Guh Slide"), ("<b>Guh Slide</b>", "<b>Guh-glijbaantje</b>"),
         ("Up the climbing frame and down the slide, once. Wheee!", "Het klimrek op en de glijbaan af, één keer. Wieee!"),
         ("about 2.5 seconds", "ongeveer 2,5 seconde")],
        [w.icon("pluizige_tunnel", "Fluffy Tunnel"), ("<b>Fluffy Tunnel</b>", "<b>Pluizige tunnel</b>"),
         ("In at the nearest hole, a moment of hiding inside (you can knock to find it), out at the far end.",
          "Erin bij het dichtstbijzijnde gat, binnen even verstoppen (je kunt kloppen om hem te vinden), eruit aan het andere eind."),
         ("5 to 12 seconds", "5 tot 12 seconden")],
    ]
    stukken = w.h3("The pieces", "De stukken") + w.p(
        "Six new obstacles, plus the toys you already know: every one of them can be a piece of a route. Obstacles face you when you place them "
        "(a guh runs in at the front); the hurdle, the tunnel, the poles and the beam can be taken from both ends.",
        "Zes nieuwe hindernissen, plus het speelgoed dat je al kent: ze kunnen allemaal een stuk van een route zijn. Hindernissen staan met hun "
        "voorkant naar je toe als je ze neerzet (een guh rent er aan de voorkant in); de horde, de tunnel, de paaltjes en de balk kunnen van "
        "twee kanten genomen worden.") + w.table(
        [("", ""), ("Piece", "Stuk"), ("What a guh does", "Wat een guh doet"), ("Time", "Tijd")], rijen)
    lopen = w.entry(w.img("guhparkour_scorebord", "The Parkour Scoreboard"), "Laps and scores", "Rondjes en scores", w.ul([
        ("A lap runs from the Start Post to the Finish Post. The clock starts when the guh leaves the start and stops when it touches the finish; "
         "then it cheers, trots back and starts again.",
         "Een rondje loopt van het Startpaaltje naar het Finishpaaltje. De klok start als de guh bij de start vertrekt en stopt als hij de finish "
         "aantikt; dan juicht hij, draaft terug en begint opnieuw."),
        ("Put a <b>Parkour Scoreboard</b> within 32 blocks of the Start Post: it shows, per guh, the <b>best lap time</b> and the number of laps, "
         "fastest on top. Right-click it for the whole list with the last lap time too.",
         "Zet een <b>Parkour-scorebord</b> binnen 32 blokken van het Startpaaltje: daarop staat per guh de <b>beste rondetijd</b> en het aantal "
         "rondjes, de snelste bovenaan. Rechtsklik erop voor de hele lijst, met ook de laatste rondetijd."),
        ("Every finished lap gives the guh a <b>heart</b> (the daily toy limit applies).",
         "Elk uitgelopen rondje geeft de guh een <b>hartje</b> (de daggrens van speelgoed geldt)."),
        ("Tell a guh to <b>sit</b> and it pauses; let it stand up and it goes on. A guh you take away from the course waits until it is back.",
         "Laat je een guh <b>zitten</b>, dan pauzeert hij; laat hem weer opstaan en hij gaat verder. Een guh die je meeneemt wacht tot hij terug is."),
        ("A guh that cannot get to a piece skips it, and that lap does not count. The post's screen marks the piece in yellow.",
         "Een guh die niet bij een stuk kan komen slaat het over, en dat rondje telt dan niet. Het scherm van het paaltje kleurt het stuk geel."),
        ("The Guhdex tab <b>Guhpixel &amp; Outings</b> keeps your totals: routes built, laps run, your longest route and the fastest laps of your guhs.",
         "Het Guhdex-tabblad <b>Guhpixel &amp; uitjes</b> houdt je totalen bij: gebouwde routes, gelopen rondjes, je langste route en de snelste "
         "rondjes van je guhs.")]))
    recepten = w.h3("Recipes", "Recepten") + '<div class="recipes">' + "".join([
        w.recipe_card("guhparkour_startpaaltje", "Start Post", "Startpaaltje"),
        w.recipe_card("guhparkour_finishpaaltje", "Finish Post", "Finishpaaltje"),
        w.recipe_card("guhparkour_horde", "Guh Hurdle (2)", "Guh-horde (2)"),
        w.recipe_card("guhparkour_springplank", "Guh Springboard", "Guh-springplank"),
        w.recipe_card("guhparkour_kruiptunnel", "Crawl Tunnel", "Kruiptunnel"),
        w.recipe_card("guhparkour_slalompaaltjes", "Slalom Poles", "Slalompaaltjes"),
        w.recipe_card("guhparkour_evenwichtsbalk", "Balance Beam", "Evenwichtsbalk"),
        w.recipe_card("guhparkour_knabbeltafeltje", "Nibble Table", "Knabbeltafeltje"),
        w.recipe_card("guhparkour_scorebord", "Parkour Scoreboard", "Parkour-scorebord")]) + "</div>"
    return intro + palen + stukken + lopen + recepten


def renders(r):
    out = r.OUT

    def blok(ref, naam, size=320, yaw=30, pitch=-25):
        try:
            beeld = r.render(r.model_quads(ref), yaw, pitch, size)
            beeld.save(os.path.join(out, f"{naam}.png"))
            return beeld
        except Exception as e:          # (a missing texture must not stop the other pictures)
            print("no render for", ref, e)
            return None

    for bid in ["guhparkour_startpaaltje", "guhparkour_finishpaaltje", "guhparkour_scorebord"] + [s[0] for s in STUKKEN]:
        beeld = blok(f"guhs:block/{bid}", bid, yaw=35 if bid.endswith(("kruiptunnel", "slalompaaltjes", "evenwichtsbalk")) else 30)
        if beeld is not None:
            beeld.resize((64, 64), r.Image.LANCZOS).save(os.path.join(out, f"icon_{bid}.png"))
    # every piece of a course side by side in one picture (a guh runs in at the front of each)
    try:
        quads = []
        rij = ["guhparkour_startpaaltje", "guhparkour_horde", "guhparkour_springplank", "guhparkour_slalompaaltjes", "guhparkour_kruiptunnel",
               "guhparkour_evenwichtsbalk", "guhparkour_knabbeltafeltje", "guhparkour_finishpaaltje", "guhparkour_scorebord"]
        for i, bid in enumerate(rij):
            quads += r.model_quads(f"guhs:block/{bid}", offset=(i * 2.0 + (1.0 if i == len(rij) - 1 else 0.0), 0, 0))
        r.render(quads, 28, -24, 900).save(os.path.join(out, "guhparkour_baan.png"))
        print("rendered guhparkour_baan and", len(rij) + 1, "block pictures")
    except Exception as e:
        print("no render for the guhparkour course", e)
    # ingredient icons the recipe cards use and the wiki did not have yet
    for naam, ref, plat in (("lime_wool", "minecraft:block/lime_wool", False), ("light_blue_wool", "minecraft:block/light_blue_wool", False),
                            ("logs", "minecraft:block/oak_log", False), ("clock", "minecraft:item/clock_00", True)):
        pad = os.path.join(out, f"icon_{naam}.png")
        if os.path.exists(pad):
            continue
        try:
            if plat:
                r.item_icon(ref).save(pad)
            else:
                r.render(r.model_quads(ref), 30, -25, 128).resize((64, 64), r.Image.LANCZOS).save(pad)
        except Exception as e:
            print("no icon for", naam, e)
