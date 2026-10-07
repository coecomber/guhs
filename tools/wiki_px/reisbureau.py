"""
The wiki part of the guhpixel slice "reisbureau": Reisbureau "De Vadsvakantie" (its own wiki section).

body(w) returns the HTML, built with the helpers of tools/make_wiki.py (every text twice: English, Dutch; the English is
hand-written here and follows tools/lang/GLOSSARY.md and tools/lang/glossary_px/reisbureau.md). The table of the sixteen
destinations is made from tools/features/guhpixel_reisbureau_tekst.py (Dutch) and the English names below.
renders(r) adds the pictures docs/wiki/img/reisbureau_*.png and icon_reisbureau_*.png.
"""
import os

N = "reisbureau"

# id -> (English trip name, English souvenir, English rare souvenir)
EN = {
    "lingsesdijk": ("Chonking at House Lingsesdijk 86", "Painting \"House Lingsesdijk 86\"", "\"House Lingsesdijk 86\" in a Golden Frame"),
    "kaasmarkt": ("Day at the Cheese Market", "Little Stack of Cheese Wheels", "Golden Cheese"),
    "vadswoud": ("Afternoon Nap in Chonkwood Forest", "Moss Cushion", "Glowguh Lantern"),
    "knuffeldal": ("Snuggledale Petting Farm", "Snuggle Sheep", "Golden Bell"),
    "guhwaii": ("Beach Afternoon on Guhwai'i", "Little Sandcastle", "Golden Surfboard"),
    "barbecuether": ("Wellness in the Barbecuether", "Sauna Bucket", "Steaming Bathtub"),
    "efteguh": ("Efteguh Theme Park", "Hungry Bulgy Guh Trash Can", "Fairy Tale Mushroom"),
    "guhkenhof": ("Guhkenhof", "Tulip Vase", "Rainbow Tulips"),
    "nomguh": ("Winter Sports in Nomguh", "Snow Globe", "Snow Globe Where It Really Snows"),
    "guhrijs": ("City Trip to Guhris", "Little Eiffel Nibble Tower", "Lit Little Eiffel Nibble Tower"),
    "camping": ("Camp Chonky Tent", "Mini Tent", "Little Campfire with Marshmallow"),
    "guhnetie": ("Guhnice", "Little Gondola", "Carnival Mask"),
    "kaasmaan": ("Trip to the Cheese Moon", "Cheese Moon Rock", "Mini Rocket"),
    "wereldreis": ("Around the World in 80 Naps", "Globe", "Spinning Golden Globe"),
    "cruise": ("Cruise on the Guh Sea", "Little Ship in a Bottle", "Life Buoy with Golden Anchor"),
    "balkonie": ("Staycation \"Balconia\"", "Painting \"Home Is Chonk Too\"", "\"Balconia\" Sign"),
}
DUUR = {60: ("1 hour", "1 uur"), 120: ("2 hours", "2 uur"), 480: ("8 hours", "8 uur"), 1440: ("24 hours", "24 uur")}


def _bestemmingen():
    import sys
    if "tools" not in sys.path:
        sys.path.insert(0, "tools")
    from features import guhpixel_reisbureau_tekst as tekst
    return tekst.BESTEMMINGEN


def body(w):
    p, ul, h3, table, img, entry = w.p, w.ul, w.h3, w.table, w.img, w.entry
    intro = p("Somewhere far away in the Guhmension stands a little white-and-pink building with a giant suitcase on its roof: <b>Travel "
              "Agency \"The Chonk Vacation\"</b> (Reisbureau \"De Vadsvakantie\"). Here you send one of your guhs on vacation. A trip takes "
              "<b>real time</b> (1, 2, 8 or 24 hours) and simply goes on while you are offline. Your guh comes back wearing sunglasses, with "
              "a <b>postcard</b> and a <b>souvenir</b> for your house. The Travel Agency has nothing to do with Guhpixel: you do not need "
              "the internet café for it.",
              "Ergens ver weg in de Guhmensie staat een wit-roze huisje met een reuzenkoffer op het dak: <b>Reisbureau \"De "
              "Vadsvakantie\"</b>. Hier stuur je één van je guhs op vakantie. Een reis duurt <b>echte tijd</b> (1, 2, 8 of 24 uur) en loopt "
              "gewoon door als je offline bent. Je guh komt terug met een zonnebril op, een <b>ansichtkaart</b> en een <b>souvenir</b> voor in "
              "huis. Het Reisbureau heeft niks met Guhpixel te maken: je hebt er het internetcafé niet voor nodig.")
    vinden = entry(img(f"{N}_gebouw", "Travel Agency The Chonk Vacation"), "Finding the Travel Agency", "Het Reisbureau vinden",
                   p("The <b>Super Compass</b> points to it (tab Cozy). The building only appears in chunks that have not been generated yet; "
                     "one copy is guaranteed in the ring between 4300 and 5600 blocks from the middle of the Guhmension, and others stand "
                     "scattered beyond that. Inside is the counter (it cannot be broken) with the <b>Travel Agent Guh</b> behind it.",
                     "Het <b>Superkompas</b> wijst ernaartoe (tabblad Knus). Het gebouwtje verschijnt alleen in chunks die nog niet gemaakt "
                     "zijn; één exemplaar staat gegarandeerd in de ring tussen 4300 en 5600 blokken van het midden van de Guhmensie, en "
                     "verder staan er her en der meer. Binnen staat de balie (die kan niet kapot) met de <b>Reisagent-guh</b> erachter.")
                   + w.cmd("/execute in guhs:guhmension run locate structure guhs:reisbureau"))
    agent = h3("The Travel Agent Guh", "De Reisagent-guh") + \
        ul([("<b>Step 1, getting to know him</b>: talk to him and say your guh would like to go.",
             "<b>Stap 1, kennismaken</b>: praat met hem en zeg dat je guh ook wel weg wil."),
            ("<b>Step 2, packing the little suitcase</b>: bring him 4 cheese nibbles (for the road), 1 wool of any color (a little pillow) "
             "and 1 paper (for the postcard).",
             "<b>Stap 2, het koffertje inpakken</b>: breng hem 4 kaasknabbels (voor onderweg), 1 wol in welke kleur dan ook (een kussentje) "
             "en 1 papier (voor de ansichtkaart)."),
            ("<b>Step 3, the trial trip</b>: put one of your guhs next to the counter and send it on the \"Trial Trip Around the Corner\". "
             "That takes five real minutes. Collect your guh at the counter: you get the <b>Travel Stamp</b>.",
             "<b>Stap 3, het proefreisje</b>: zet één van je guhs bij de balie en stuur hem op het \"Proefreisje om de hoek\". Dat duurt vijf "
             "echte minuten. Haal je guh op bij de balie: je krijgt de <b>Reisstempel</b>."),
            ("Lost the stamp, or want a second desk? The Travel Agent Guh gives you a new stamp whenever you carry none.",
             "Stempel kwijt, of wil je een tweede balie? De Reisagent-guh geeft je een nieuwe stempel als je er geen bij je hebt.")])
    balie = entry(img(f"{N}_balie", "Travel Desk"), "The Travel Desk", "De Reisbalie",
                  p("With the Travel Stamp, paper and planks you make your own <b>Travel Desk</b> for at home. Right-click it: it opens "
                    "exactly the same screen as the counter in the Travel Agency. The desk itself holds nothing: breaking it loses nothing, "
                    "and you can collect a returning guh at <b>any</b> desk.",
                    "Met de Reisstempel, papier en planken maak je je eigen <b>Reisbalie</b> voor thuis. Rechtsklik erop: je krijgt precies "
                    "hetzelfde scherm als bij de balie in het Reisbureau. In de balie zelf zit niks: als hij kapot gaat raak je niks kwijt, "
                    "en een guh die terug is haal je op bij <b>elke</b> balie.")
                  + w.recipe_card(f"{N}_balie"))
    reizen = h3("Trips", "Reizen") + \
        ul([("Every real day there are <b>four trips</b>, one of 1, 2, 8 and 24 hours, the same for everyone on the server. They come out of "
             "sixteen destinations; within four days every destination of a duration has come by.",
             "Elke echte dag zijn er <b>vier reizen</b>, één van 1, 2, 8 en 24 uur, voor iedereen op de server dezelfde. Ze komen uit zestien "
             "bestemmingen; binnen vier dagen is elke bestemming van een duur een keer langs geweest."),
            ("The screen shows per trip how long it takes, what your guh will <b>probably</b> bring and what it brings <b>with luck</b>. "
             "Hover over a trip for the explanation. A green dot means you already have that souvenir.",
             "Het scherm laat per reis zien hoe lang hij duurt, wat je guh <b>waarschijnlijk</b> meeneemt en wat hij <b>met geluk</b> "
             "meeneemt. Houd je muis op een reis voor de uitleg. Een groen stipje betekent dat je dat souvenir al hebt."),
            ("<b>One guh at a time</b> per player. The guh must stand near the desk (within 24 blocks); babies, guhs asleep in their house "
             "and guhs that are being ridden stay home.",
             "<b>Eén guh tegelijk</b> per speler. De guh moet bij de balie staan (binnen 24 blokken); baby's, guhs die in hun huisje "
             "slapen en guhs waar iemand op rijdt blijven thuis."),
            ("Your guh picks up its little suitcase, walks off and is gone. In the Guhdex (My Guhs) you read \"On vacation in ..., back in "
             "...\". It cannot be called while it is away.",
             "Je guh pakt zijn koffertje, loopt weg en is vertrokken. In de Guhdex (Mijn guhs) lees je \"Op vakantie in ..., terug over "
             "...\". Roepen kan niet zolang hij weg is."),
            ("<b>A guh can never get lost.</b> It is kept safe in your own saved data, not in the desk: a broken desk, logging out, a "
             "server restart, none of it matters. When the time is up you get a message; collect it at any Travel Desk.",
             "<b>Een guh kan nooit kwijtraken.</b> Hij wordt veilig bewaard bij jouw eigen gegevens, niet in de balie: een kapotte balie, "
             "uitloggen, een herstart van de server, het maakt allemaal niks uit. Als de tijd om is krijg je een berichtje; haal hem op bij "
             "welke Reisbalie je maar wilt."),
            ("Changed your mind? <b>Call back early</b> brings your guh home right away, without postcard and souvenir.",
             "Bedacht? Met <b>Eerder terugroepen</b> is je guh meteen thuis, zonder ansichtkaart en zonder souvenir."),
            ("Back home the guh wears <b>sunglasses</b> for about twenty minutes and gets two hearts.",
             "Thuis draagt de guh nog zo'n twintig minuten een <b>zonnebril</b> en krijgt hij twee hartjes.")])
    kansen = h3("What your guh brings", "Wat je guh meeneemt") + \
        p("Always the destination's <b>postcard</b> (right-click to read it: written by your guh, signed with its name) and a souvenir: the "
          "common one, or with luck the rare one instead.",
          "Altijd de <b>ansichtkaart</b> van de bestemming (rechtsklik om te lezen: geschreven door je guh, met zijn naam eronder) en een "
          "souvenir: het gewone, of met geluk het zeldzame.") + \
        table([("Trip", "Reis"), ("Chance on the rare souvenir", "Kans op het zeldzame souvenir")],
              [[DUUR[60], "5%"], [DUUR[120], "8%"], [DUUR[480], "15%"], [DUUR[1440], "30%"]]) + \
        p("A souvenir you already had becomes a <b>stamp on your travel pass</b> instead of a second item. Ten stamps: the Travel Agent "
          "Guh gives you a <b>Golden Suitcase</b>, and the pass starts again. The Guhdex tab \"Guhpixel & outings\" has the album with all "
          "32 souvenirs and the 16 postcards.",
          "Een souvenir dat je al had wordt een <b>stempel op je reispas</b> in plaats van een tweede exemplaar. Tien stempels: je krijgt "
          "van de Reisagent-guh een <b>Gouden koffertje</b>, en de pas begint opnieuw. In het Guhdex-tabblad \"Guhpixel & uitjes\" staat "
          "het album met alle 32 souvenirs en de 16 ansichtkaarten.")
    rijen = []
    for (bid, minuten, _kans, naam, _plek, _uitleg, _kaart, souvenir, zeldzaam) in _bestemmingen():
        en = EN[bid]
        rijen.append([DUUR[minuten], (en[0], naam),
                      w.icon(f"{N}_souvenir_{bid}", en[1]) + " " + w.t(en[1], souvenir[0]),
                      w.icon(f"{N}_zeldzaam_{bid}", en[2]) + " " + w.t(en[2], zeldzaam[0])])
    bestemmingen = h3("The sixteen destinations", "De zestien bestemmingen") + \
        table([("Takes", "Duur"), ("Trip", "Reis"), ("Souvenir", "Souvenir"), ("Rare souvenir", "Zeldzaam souvenir")], rijen) + \
        p("All souvenirs are small decoration blocks. The paintings, the \"Balconia\" sign, the carnival mask and the life buoy hang on a "
          "wall; the rest stands on the floor. Some do a little something: the bathtub steams, the campfire burns (harmlessly), it snows "
          "in the special snow globe, the golden globe spins, the lantern and the lit tower give light.",
          "Alle souvenirs zijn kleine decoratieblokken. De schilderijen, het bordje \"Balkonië\", het carnavalsmasker en de reddingsboei "
          "hangen aan de muur; de rest staat op de grond. Sommige doen iets kleins: het badkuipje stoomt, het kampvuurtje brandt "
          "(ongevaarlijk), in de bijzondere sneeuwbol sneeuwt het, de gouden wereldbol draait, de lantaarn en het verlichte torentje "
          "geven licht.") + \
        '<div class="gallery">' + img(f"{N}_souvenirs", "Souvenirs") + "</div>"
    return intro + vinden + agent + balie + reizen + kansen + bestemmingen


def renders(r):
    """The building, the Travel Desk, a row of souvenirs, and an icon of every souvenir (for the table)."""
    import sys
    from PIL import Image, ImageOps
    if "tools" not in sys.path:
        sys.path.insert(0, "tools")
    from features import guhpixel_reisbureau_modellen as modellen
    out = r.OUT

    def model(bid, size, ss=2):
        # (mirrored back: the renderer's view of a north face is a mirror image, and these have pictures and letters on them)
        muur = modellen.alle().get(bid, ("vloer",))[0] == "muur"
        return ImageOps.mirror(r.render(r.model_quads(f"guhs:block/{bid}"), 20 if muur else 30, -22, size, ss=ss))

    try:
        struct = r.load_structure(N)
        if struct is not None:
            for pos, blok in list(struct.blocks.items()):   # (small things the structure renderer has no colour for)
                if blok[0].endswith("_sign") or "potted_" in blok[0] or blok[0].endswith("lantern") or blok[0].endswith("jigsaw"):
                    del struct.blocks[pos]
            r.render_structure(struct, {}, px=12, max_size=900).save(os.path.join(out, f"{N}_gebouw.png"))
        model(f"{N}_balie", 320).save(os.path.join(out, f"{N}_balie.png"))
        model(f"{N}_balie", 64, 4).save(os.path.join(out, f"icon_{N}_balie.png"))
        r.item_icon(f"guhs:item/{N}_stempel").save(os.path.join(out, f"icon_{N}_stempel.png"))
        ids = list(modellen.alle())
        for bid in ids:
            model(bid, 64, 4).save(os.path.join(out, f"icon_{bid}.png"))
        toon = [f"{N}_souvenir_lingsesdijk", f"{N}_souvenir_kaasmarkt", f"{N}_zeldzaam_vadswoud", f"{N}_souvenir_knuffeldal", f"{N}_souvenir_guhwaii",
                f"{N}_zeldzaam_barbecuether", f"{N}_souvenir_efteguh", f"{N}_zeldzaam_guhkenhof", f"{N}_zeldzaam_nomguh", f"{N}_zeldzaam_guhrijs",
                f"{N}_zeldzaam_camping", f"{N}_souvenir_guhnetie", f"{N}_zeldzaam_kaasmaan", f"{N}_souvenir_wereldreis", f"{N}_souvenir_cruise",
                f"{N}_gouden_koffertje"]
        cel = 150
        vel = Image.new("RGBA", (8 * cel, 2 * cel), (0, 0, 0, 0))
        for i, bid in enumerate(toon):
            beeld = model(bid, cel - 10)
            vel.paste(beeld, ((i % 8) * cel + 5, (i // 8) * cel + 5), beeld)
        vel.save(os.path.join(out, f"{N}_souvenirs.png"))
        print(f"rendered {N}_gebouw, {N}_balie, {N}_souvenirs, {len(ids)} souvenir icons")
    except Exception as e:   # (a missing picture never stops the other slices)
        print("no render for the reisbureau", e)
