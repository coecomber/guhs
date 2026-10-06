"""
The wiki part of the guhpixel slice "lobby": how to get into Guhpixel (the Guh-internetcafé, the Netwerkkabeltje, the
Guhpixel-poort, the commands), the lobby island, the ranks, the lobby parkour and the golden knabbels.

body(w) returns the HTML of this slice's part, built with the helpers of tools/make_wiki.py: w.h3(en, nl), w.p(en, nl),
w.ul([(en, nl), ...]), w.table(head, rows), w.entry(image, title_en, title_nl, body, stats), w.img(name, alt),
w.recipe_card(id). Every text is written twice, (English, Dutch); the English is hand-written here and follows
tools/lang/GLOSSARY.md and tools/lang/glossary_px. No version numbers.
renders(r) adds this slice's pictures docs/wiki/img/<namespace>_*.png: r is tools/wiki_renders.py, r.OUT the folder.
"""
import os

RANGEN = [("[GUH]", "[GUH]", 0), ("[CHONK]", "[VADS]", 250), ("[CHONK+]", "[VADS+]", 750), ("[MVG]", "[MVG]", 1250), ("[MVG++]", "[MVG++]", 1750)]


def body(w):
    h3, p, ul, table, entry, img = w.h3, w.p, w.ul, w.table, w.entry, w.img
    out = []
    out.append(p(
        "<b>Guhpixel</b> is a parody of a big minigame server, in a dimension of its own: a floating lobby island full of guhs, joke minigames, one real "
        "game (Among Guhs), a shop and a currency of its own, the <b>Guhpixel Coins</b>. Nothing in Guhpixel can hurt you, you cannot lose anything, and "
        "everything can be done by every player on a server: all progress is your own.",
        "<b>Guhpixel</b> is een parodie op een grote minigame-server, in een eigen dimensie: een zwevend lobby-eiland vol guhs, grapspelletjes, één echt spel "
        "(Among Guhs), een winkel en een eigen munt, de <b>Guhpixel-muntjes</b>. Niets in Guhpixel doet pijn, je kunt niets kwijtraken, en alles kan door elke "
        "speler op een server gedaan worden: alle voortgang is van jezelf."))
    out.append(h3("How do you get in?", "Hoe kom je erin?"))
    out.append(entry(
        img("internetcafe_gebouw", "Guh Internet Café \"The Slow Connection\""),
        "Guh Internet Café \"The Slow Connection\"", "Guh-internetcafé \"De Trage Verbinding\"",
        p("Somewhere in the Guhmension stands a beige box of a building with an antenna and a satellite dish on its roof (<code>guhs:internetcafe</code>). "
          "Your <b>Super Compass</b> points the way (tab Minigames, heading Guhpixel). It only appears in <b>newly generated chunks</b>; one is guaranteed "
          "between 4300 and 5600 blocks from the middle of the Guhmension, and more are scattered around in new terrain. Inside, guhs are asleep behind old "
          "beige computers, and the <b>Admin Guh</b> asks: \"Have you tried turning it off and on again, Nyeg?\" Against the back wall stands a "
          "<b>giant CRT monitor</b>. Walk straight through its screen: the first time, that <b>unlocks Guhpixel</b> for you.",
          "Ergens in de Guhmensie staat een beige doos van een gebouw met een antenne en een schotel op het dak (<code>guhs:internetcafe</code>). Je "
          "<b>superkompas</b> wijst de weg (tab Minigames, kopje Guhpixel). Het verschijnt alleen in <b>nieuw gemaakte chunks</b>; er staat er gegarandeerd één "
          "tussen 4300 en 5600 blokken van het midden van de Guhmensie, en verder staan ze verspreid in nieuw terrein. Binnen slapen guhs achter oude beige "
          "computers, en de <b>Beheerder-guh</b> vraagt: \"Heb je hem al uit en weer aan gezet, njeg?\" Tegen de achterwand staat een "
          "<b>reusachtig beeldscherm</b>. Loop dwars door het scherm: de eerste keer <b>ontgrendelt</b> dat Guhpixel voor jou."),
        [(("Where", "Waar"), w.t("Guhmension, new chunks", "Guhmensie, nieuwe chunks")),
         (("Find it", "Vinden"), w.t("Super Compass or <code>/locate structure guhs:internetcafe</code>", "superkompas of <code>/locate structure guhs:internetcafe</code>"))]))
    out.append(img("internetcafe_binnen", "Inside the Guh Internet Café"))
    out.append(ul([
        ("After that the commands <code>/lobby</code>, <code>/hub</code> and <code>/l</code> work everywhere. Before, they only give a silly refusal that "
         "points to the café.",
         "Daarna werken de commando's <code>/lobby</code>, <code>/hub</code> en <code>/l</code> overal. Daarvoor geven ze alleen een grappige weigering die "
         "naar het café wijst."),
        ("In the lobby, the <b>Welcome Guh</b> gives you a <b>Network Cable</b> (once; lost it? ask for a new one) and teaches you the recipe of the "
         "<b>Guhpixel Gate</b>: a little beige monitor for at home. Right-click it and you are in the lobby.",
         "In de lobby geeft de <b>Welkomstguh</b> je een <b>Netwerkkabeltje</b> (één keer; kwijt? vraag een nieuwe) en leert hij je het recept van de "
         "<b>Guhpixel-poort</b>: een klein beige beeldschermpje voor thuis. Rechtsklik erop en je staat in de lobby."),
        ("Going home: walk through the <b>front door</b> at the south side of the plaza (\"Terug naar huis\") or type <code>/lobby</code> in the lobby. "
         "Both bring you back to exactly where you came from.",
         "Naar huis: loop door de <b>voordeur</b> aan de zuidkant van het plein (\"Terug naar huis\") of typ <code>/lobby</code> in de lobby. Allebei "
         "brengen je precies terug naar waar je vandaan kwam."),
        ("Your guhs and pets stay at home. You keep your inventory, but nothing can be built or broken in Guhpixel.",
         "Je guhs en huisdieren blijven thuis. Je houdt je spullen, maar in Guhpixel kan niets gebouwd of gesloopt worden."),
    ]))
    out.append('<div class="recipes">' + w.recipe_card("guhpixel_poort", "Guhpixel Gate", "Guhpixel-poort") + "</div>")
    out.append(h3("The lobby", "De lobby"))
    out.append(entry(
        img("lobby_eiland", "The Guhpixel lobby"), "A floating cheese island", "Een zwevend kaaseiland",
        p("The lobby is a pink plaza on a floating island of cheese. In the north hangs the big <b>GUHPIXEL</b> logo; in front of it stands a row of market "
          "stalls, one per game, each with a guh and a floating label (\"CLICK TO PLAY\", \"0 players · 99 guhs\"). In the west is the <b>shop</b> of the "
          "<b>Shopkeeper Guh</b> (\"Cosmetic only, honest, Nyeg\", \"Now 0% off!\"), where you spend your coins on things for at home. In the east is the "
          "<b>AFK corner</b> where the lobby guhs are \"afk (sleeping)\". In the south stand two boards, <b>your own stats</b> (everybody sees their own "
          "numbers) and <b>Players online: n (and 47 guhs)</b>, and the door home. Lobby guhs with gamer names chat now and then: \"gg\", \"anyone party?\", "
          "\"lag!!\", \"who has my nibble\".",
          "De lobby is een roze plein op een zwevend eiland van kaas. In het noorden hangt het grote <b>GUHPIXEL</b>-logo; daarvoor staat een rij "
          "marktkraampjes, één per spel, elk met een guh en een zwevend bordje (\"KLIK OM TE SPELEN\", \"0 spelers · 99 guhs\"). In het westen staat de "
          "<b>winkel</b> van de <b>Verkoper-guh</b> (\"Alleen cosmetisch, echt waar, njeg\", \"Nu 0% korting!\"), waar je je muntjes uitgeeft aan dingen voor "
          "thuis. In het oosten is de <b>AFK-hoek</b> waar de lobbyguhs \"afk (slaap)\" zijn. In het zuiden staan twee borden, <b>jouw eigen stats</b> "
          "(iedereen ziet zijn eigen getallen) en <b>Spelers online: n (en 47 guhs)</b>, en de voordeur naar huis. Lobbyguhs met gamernamen chatten af en "
          "toe: \"gg\", \"iemand party?\", \"lag!!\", \"wie heeft mijn knabbel\"."),
        [(("Dimension", "Dimensie"), "<code>guhs:guhpixel</code>"), (("Commands", "Commando's"), "<code>/lobby</code> <code>/hub</code> <code>/l</code>")]))
    out.append(h3("Ranks", "Rangen"))
    out.append(p(
        "Your rank is a prefix in front of your name (in chat, in the player list and above your head). It depends on all the coins you <b>ever earned</b>: "
        "spending them does not cost you your rank. You can switch the prefix off in the Guhdex (tab Guhpixel &amp; Outings, click your rank).",
        "Je rang staat voor je naam (in de chat, in de spelerslijst en boven je hoofd). Hij hangt af van alle muntjes die je <b>ooit verdiende</b>: uitgeven "
        "kost je je rang niet. Je kunt hem uitzetten in de Guhdex (tab Guhpixel &amp; uitjes, klik op je rang)."))
    out.append(table([("Rank", "Rang"), ("Coins ever earned", "Muntjes ooit verdiend")],
                     [[w.t(en, nl), str(n)] for en, nl, n in RANGEN]))
    out.append(p("MVG? Most Valuable Guh.", "MVG? Meest Vadsige Guh."))
    out.append(h3("Lobby parkour and golden nibbles", "Lobby-parkour en gouden knabbels"))
    out.append(entry(
        img("lobby_gouden_knabbel", "A golden lobby nibble"), "Two things to do between games", "Twee dingen voor tussendoor",
        ul([
            ("<b>Lobby parkour</b>: a green start plate lies at the east side. Step off it and the clock runs: up the AFK corner, over floating cushions and "
             "the roofs of all the stalls (three of them are checkpoints) to the checkered finish on the shop roof. Falling does not hurt; just start again. "
             "Your <b>personal best</b> floats at the start, next to the three fastest roof runners of the server. The first finish pays <b>50 coins</b>. "
             "Flying does not count.",
             "<b>Lobby-parkour</b>: aan de oostkant ligt een groene startplaat. Stap eraf en de tijd loopt: de AFK-hoek op, over zwevende kussens en de daken "
             "van alle kraampjes (drie ervan zijn tussenpunten) naar de geblokte finish op het dak van de winkel. Vallen doet geen pijn; begin gewoon opnieuw. "
             "Je <b>eigen record</b> zweeft bij de start, naast de drie snelste daklopers van de server. De eerste finish levert <b>50 muntjes</b> op. "
             "Vliegen telt niet."),
            ("<b>Ten golden nibbles</b> are hidden in the lobby: behind things, on roofs, and one under the ground. Right-click one: <b>10 coins</b>, once per "
             "nibble. They stay where they are, so every player can find all ten. All ten earn the title <i>Nibble Sleuth</i>.",
             "<b>Tien gouden knabbels</b> liggen verstopt in de lobby: achter dingen, op daken, en eentje onder de grond. Rechtsklik erop: <b>10 muntjes</b>, "
             "één keer per knabbel. Ze blijven liggen, dus elke speler kan ze alle tien vinden. Alle tien leveren de titel <i>Knabbelspeurder</i> op."),
        ]),
        [(("Parkour", "Parkour"), w.t("50 coins, once", "50 muntjes, één keer")), (("Nibbles", "Knabbels"), w.t("10 x 10 coins", "10 x 10 muntjes"))]))
    out.append(p(
        "Together with the six games (100 coins each, the first time) that is 750 coins you can earn once. Only Among Guhs pays again and again.",
        "Samen met de zes spellen (elk 100 muntjes, de eerste keer) is dat 750 muntjes die je één keer kunt verdienen. Alleen Among Guhs betaalt steeds opnieuw."))
    return "".join(out)


def renders(r):
    out = r.OUT
    # blocks whose colour the structure renderer cannot find by name (it takes a block's own texture)
    r.BLOCK_TEXTURES.update({
        "minecraft:smooth_sandstone": "block/sandstone_top", "minecraft:smooth_sandstone_slab": "block/sandstone_top",
        "minecraft:smooth_quartz": "block/quartz_block_bottom", "minecraft:smooth_quartz_slab": "block/quartz_block_bottom",
        "minecraft:birch_sign": "block/birch_planks", "minecraft:birch_wall_sign": "block/birch_planks",
        "guhs:internetcafe_computer": "guhs:block/guhpixel_poort_kast", "guhs:seizoensbloembak": "guhs:block/seizoensbloembak_lente",
        "guhs:guh_bank": "block/pink_wool"})
    r.SPECIAL_COLOURS.update({"guhs:guhpixel_portaal": (120, 240, 170)})

    def bewaar(naam, maak):
        try:
            maak().save(os.path.join(out, naam + ".png"))
            print("rendered", naam)
        except Exception as e:            # (a missing template or texture must not stop the other pictures)
            print("no render for", naam, e)

    def laad(sid):
        st = r.load_structure(sid)
        if st is None:
            raise FileNotFoundError(sid)
        return st

    def draai(st):
        """The structure turned half a turn: the renderer looks from the north-west, the fronts here face south and east."""
        import make_structures as ms
        w, h, d = st.size
        uit = ms.Structure(st.size)
        uit.blocks = {(w - 1 - x, y, d - 1 - z): b for (x, y, z), b in st.blocks.items()}
        uit.entities = [(w - x, y, d - z, nbt) for x, y, z, nbt in st.entities]
        return uit

    def spiegel(st):
        """North and south swapped. The renderer draws a mirror image of the world (nobody sees that on a building without
        letters); mirrored once more the GUHPIXEL logo faces the viewer and reads the right way round."""
        import make_structures as ms
        w, h, d = st.size
        uit = ms.Structure(st.size)
        uit.blocks = {(x, y, d - 1 - z): b for (x, y, z), b in st.blocks.items()}
        return uit

    def binnen(st, x0, x1, y0, y1, z0, z1):
        """Only the box, without its two near walls: a look inside."""
        import make_structures as ms
        uit = ms.Structure((x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1))
        uit.blocks = {(x - x0, y - y0, z - z0): b for (x, y, z), b in st.blocks.items()
                      if x0 < x <= x1 and y0 <= y <= y1 and z0 < z <= z1}
        return uit

    from features import guhpixel_lobby_cafe as cafe
    bewaar("lobby_eiland", lambda: r.render_structure(spiegel(laad("guhpixel/lobby")), {}, px=6, max_size=1400))
    bewaar("internetcafe_gebouw", lambda: r.render_structure(draai(laad("internetcafe")), {}, px=14, max_size=1100))
    # (turned, the door wall and the east wall are the near ones: they go, and so does the roof)
    bewaar("internetcafe_binnen", lambda: r.render_structure(
        binnen(draai(laad("internetcafe")), cafe.W - 1 - cafe.X1, cafe.W - 1 - cafe.X0, cafe.G, cafe.DAK - 1, cafe.D - 1 - cafe.Z1, cafe.D - 1 - cafe.Z0),
        {}, px=18, max_size=1100))
    bewaar("internetcafe_computer", lambda: r.render(r.model_quads("guhs:block/internetcafe_computer"), 30, -25, 320))
    bewaar("lobby_gouden_knabbel", lambda: r.render(r.model_quads("guhs:block/lobby_gouden_knabbel"), 35, -25, 320))
