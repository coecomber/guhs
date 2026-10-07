"""
The wiki part of the guhpixel slice "grap1": Skyblok, Bedwars and Vadsnite (three entries, no spoiler of the punchline in the
first line of an entry: the punchlines sit in a fold-out).

body(w) returns the HTML of this slice's part, built with the helpers of tools/make_wiki.py: w.h3(en, nl), w.p(en, nl),
w.ul([(en, nl), ...]), w.table(head, rows), w.entry(image, title_en, title_nl, body, stats), w.img(name, alt),
w.recipe_card(id). Every text is written twice, (English, Dutch); the English is hand-written here and follows
tools/lang/GLOSSARY.md and tools/lang/glossary_px. No version numbers.
renders(r) adds this slice's pictures docs/wiki/img/<namespace>_*.png: r is tools/wiki_renders.py, r.OUT the folder.
"""
import os


def _clou(w, en, nl):
    """A fold-out, so nobody reads the punchline by accident."""
    return ('<details class="spoiler"><summary>' + w.t("The punchline (spoiler!)", "De clou (spoiler!)") + "</summary>"
            + w.p(en, nl) + "</details>")


def body(w):
    intro = w.h3("Joke games: Skyblok, Bedwars & Chonknite", "Grapspelletjes: Skyblok, Bedwars & Vadsnite") + \
        w.p("Three famous minigames, the guh way. Each has its own game guh in the Guhpixel lobby (a floating sign above its head says "
            "<b>CLICK TO PLAY</b>). Talk to it and you are taken to an arena of your very own, so any number of players can play at the same "
            "time. A game is a little questline of four steps and takes a few minutes. Your own things are put away safely while you play "
            "and come back exactly as they were. Nothing can hurt you and you cannot lose anything.",
            "Drie beroemde minigames, op z'n guhs. Elk heeft zijn eigen spelguh in de Guhpixel-lobby (boven zijn hoofd zweeft een bordje "
            "<b>KLIK OM TE SPELEN</b>). Praat met hem en je gaat naar een arena helemaal voor jou alleen, dus iedereen kan tegelijk spelen. "
            "Een spel is een klein questlijntje van vier stappen en duurt een paar minuten. Je eigen spullen worden veilig opgeborgen terwijl "
            "je speelt en komen precies zo terug. Niets doet je pijn en je kunt niks kwijtraken.") + \
        w.ul([("<b>The first time</b> you finish a game: 100 Guhpixel Coins, a keepsake and a new movie for the Guh Cinema.",
               "<b>De eerste keer</b> dat je een spel uitspeelt: 100 Guhpixel-muntjes, een aandenken en een nieuwe film voor de Guhbioscoop."),
              ("<b>After that</b> you can replay it as often as you like (no more coins, just as fun).",
               "<b>Daarna</b> mag je het zo vaak opnieuw spelen als je wilt (geen muntjes meer, wel net zo leuk)."),
              ("Want to stop? Type <b>/lobby</b>: you are back on the plaza with your own things.",
               "Wil je stoppen? Typ <b>/lobby</b>: je staat weer op het plein met je eigen spullen."),
              ("Your steps are in the Guhdex (tab Guhpixel &amp; Outings) and in the quest book (chapter Guhpixel).",
               "Je stappen staan in de Guhdex (tab Guhpixel &amp; uitjes) en in het questboek (hoofdstuk Guhpixel).")])

    skyblok = w.entry(
        w.img("skyblok_guh", "Skyblok Guh"), "Skyblok", "Skyblok",
        w.p("The <b>Skyblok Guh</b> is a deadly serious pro with a headset. He sends you to THE island: an L of dirt, one tree, a chest with "
            "ice and a bucket of lava, and a bed. A manual at the top of your screen counts: <i>Step 1 of 4,812: make a cobblestone "
            "generator</i>. Good luck, you are going to need it.",
            "De <b>Skyblok-guh</b> is een bloedserieuze pro met een headset. Hij stuurt je naar hét eiland: een L van aarde, één boom, een "
            "kist met ijs en een emmer lava, en een bed. Bovenin je scherm telt een handleiding: <i>Stap 1 van 4.812: maak een cobblestone "
            "generator</i>. Succes, je zult het nodig hebben.") +
        _clou(w, "Nothing works. You cannot break or place anything, the lava stays in its bucket, and the sky turns out to be a box of light "
                 "blue terracotta with painted clouds, visible seams, a ladder and a staff door. The only thing that works is lying down in "
                 "the bed: <b>SKYBLOK COMPLETED!</b>, with fireworks and credits. The goal in life is chonking, not building a cobblestone "
                 "generator.",
              "Niks werkt. Je kunt niks slopen of neerzetten, de lava blijft in de emmer, en de lucht blijkt een doos van lichtblauwe "
              "terracotta met geschilderde wolkjes, zichtbare naden, een ladder en een personeelsdeur. Het enige dat werkt is in het bed gaan "
              "liggen: <b>SKYBLOK UITGESPEELD!</b>, met vuurwerk en een aftiteling. Het doel in het leven is vadsen, niet een cobblestone "
              "generator bouwen."),
        stats=[(("Where", "Waar"), w.t("Guhpixel lobby, the Skyblok Guh", "Guhpixel-lobby, de Skyblok-guh")),
               (("Keepsake", "Aandenken"), w.icon("skyblok_fles", "Island in a Bottle") + " " + w.t("Island in a Bottle (decoration block)",
                                                                                                    "Eilandje-in-een-fles (decoratieblok)")),
               (("Lost it?", "Kwijt?"), w.t("The Skyblok Guh gives you a new one", "De Skyblok-guh geeft je een nieuwe"))])

    bedwars = w.entry(
        w.img("bedwars_guh", "Bedwars Guh"), "Bedwars", "Bedwars",
        w.p("The <b>Bedwars Guh</b> wears an armor made of pillows. You get a little island with THE bed; around you live four teams, each on "
            "a tiny island of its own: Red, Blue, Green and Yellow, with nightcaps in their team color. They slowly bridge over with wool. "
            "<b>Defend your bed!</b>",
            "De <b>Bedwars-guh</b> draagt een harnas van kussens. Jij krijgt een eilandje met hét bed; om je heen wonen vier teams, elk op een "
            "eigen piepklein eilandje: Rood, Blauw, Groen en Geel, met slaapmutsen in hun teamkleur. Ze bruggen langzaam naar je toe met wol. "
            "<b>Verdedig je bed!</b>") +
        _clou(w, "You defend your bed by lying in it. The team guhs wait politely at the edge of your island until you do, and then come "
                 "and lie down around your bed. End screen: <b>BED DEFENDED</b> - beds destroyed: 0, naps: 5.",
              "Je verdedigt je bed door erin te gaan liggen. De teamguhs wachten netjes op het randje van je eiland tot je ligt, en komen dan "
              "om je bed heen liggen. Eindscherm: <b>BED VERDEDIGD</b> - bedden vernield: 0, dutjes: 5."),
        stats=[(("Where", "Waar"), w.t("Guhpixel lobby, the Bedwars Guh", "Guhpixel-lobby, de Bedwars-guh")),
               (("Keepsake", "Aandenken"), " ".join(w.icon(f"bedwars_slaapmuts_{k}", "Team Nightcap") for k in ("rood", "blauw", "groen", "geel"))
                + " " + w.t("the four Team Nightcaps (outfits for your guhs, head)", "de vier Teamslaapmutsen (pakjes voor je guhs, hoofd)"))])

    vadsnite = w.entry(
        w.img("vadsnite_guh", "Chonknite Guh"), "Chonknite", "Vadsnite",
        w.p("The <b>Chonknite Guh</b> already has his parachute pack on. You fly over a little island in the <b>Chonk Bus</b> with 99 guhs. "
            "After the countdown everyone jumps through the hatch in the floor and floats down. The counter <i>Still awake</i> starts at 100 "
            "and a pink, soft and perfectly harmless <b>sleep cloud</b> closes in. Be the last one left!",
            "De <b>Vadsnite-guh</b> heeft zijn parachuterugzakje al om. Je vliegt met 99 guhs in de <b>Vadsbus</b> boven een eilandje. Na het "
            "aftellen springt iedereen door het luik in de vloer en zweeft naar beneden. De teller <i>Nog wakker</i> begint bij 100 en een "
            "roze, zachte en volkomen ongevaarlijke <b>slaapwolk</b> komt steeds dichterbij. Blijf als laatste over!") +
        _clou(w, "Everyone lands and falls asleep at once: the counter races from 100 down. When the last guh nods off you are the only one "
                 "awake: <b>#1 CHONK ROYALE</b>. There are four sleeping spots on the island; lie down in one yourself and it is <i>Everyone is "
                 "asleep. It's a draw, Nyeg.</i> That counts too.",
              "Iedereen landt en valt meteen in slaap: de teller racet van 100 naar beneden. Als de laatste guh indut ben jij de enige die nog "
              "wakker is: <b>#1 VADSOVERWINNING</b>. Op het eiland liggen vier slaapplekjes; ga je er zelf in liggen, dan is het <i>Iedereen "
              "slaapt. Gelijkspel, njeg.</i> Dat telt ook."),
        stats=[(("Where", "Waar"), w.t("Guhpixel lobby, the Chonknite Guh", "Guhpixel-lobby, de Vadsnite-guh")),
               (("Keepsake", "Aandenken"), w.icon("vadsnite_parachuterugzakje", "Parachute Pack") + " "
                + w.t("Parachute Pack (outfit for your guhs, back)", "Parachuterugzakje (pakje voor je guhs, rug)"))])
    arenas = w.p(w.img("bedwars_eilanden", "The Bedwars islands", "wide") + w.img("vadsnite_eiland", "The Chonk Bus above its island", "wide"),
                 w.img("bedwars_eilanden", "De Bedwars-eilandjes", "wide") + w.img("vadsnite_eiland", "De Vadsbus boven zijn eiland", "wide"))
    return intro + skyblok + bedwars + vadsnite + arenas


def renders(r):
    out = r.OUT

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)
    geo = lambda n: os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity", n + ".geo.json")
    for kind in ("skyblok_guh", "bedwars_guh", "vadsnite_guh"):
        try:
            save(r.render(r.geo_quads(geo(f"guh_npc_{kind}"), f"guhs:entity/npc_{kind}"), 28, -12, 360), kind)
        except Exception as e:  # noqa: BLE001
            print("no render for", kind, e)
    try:
        # (the picture renderer cuts out instead of blending: only the bottle's edges and highlights are drawn, so the island shows)
        quads = r.model_quads("guhs:block/skyblok_fles")
        glas = r.tex_array("guhs:block/skyblok_fles_glas").copy()
        glas[..., 3] = (glas[..., 3] > 100) * 255
        for q in quads:
            if isinstance(q.tex, str) and q.tex.endswith("skyblok_fles_glas"):
                q.tex = glas
        fles = r.render(quads, 30, -20, 320)
        save(fles, "skyblok_fles")
        save(fles.resize((64, 64), r.Image.LANCZOS), "icon_skyblok_fles")
    except Exception as e:  # noqa: BLE001
        print("no render for skyblok_fles", e)
    for item in ("bedwars_slaapmuts_rood", "bedwars_slaapmuts_blauw", "bedwars_slaapmuts_groen", "bedwars_slaapmuts_geel", "vadsnite_parachuterugzakje"):
        try:
            save(r.item_icon(f"guhs:item/{item}"), f"icon_{item}")
        except Exception as e:  # noqa: BLE001
            print("no icon for", item, e)
    for sid, naam, cutaway in (("guhpixel/skyblok_eiland", "skyblok_eiland", True), ("guhpixel/bedwars_eilanden", "bedwars_eilanden", False),
                               ("guhpixel/vadsnite_eiland", "vadsnite_eiland", False)):
        try:
            st = r.load_structure(sid)
            st.blocks = {k: v for k, v in st.blocks.items() if v[0] != "minecraft:light"}      # (invisible in the game)
            w, h, d = st.size
            save(r.render_structure(st, {}, px=max(6, min(14, int(1100 / (w + d)))), max_size=1100, cutaway=cutaway), naam)
        except Exception as e:  # noqa: BLE001
            print("no render for", sid, e)
