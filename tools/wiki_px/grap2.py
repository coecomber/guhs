"""
The wiki part of the guhpixel slice "grap2": the Guhmon-gevecht and Boer zoekt Guh (two entries in the section Guhpixel).

body(w) returns the HTML, built with the helpers of tools/make_wiki.py; every text is written twice, (English, Dutch);
the English is hand-written and follows tools/lang/GLOSSARY.md and tools/lang/glossary_px/grap2.md. The first line of an
entry does not give the punchline away. renders(r) makes the pictures docs/wiki/img/guhmon_*.png and bzg_*.png.
"""
import os


def body(w):
    t, p, ul, entry, img, icon, table, h3 = w.t, w.p, w.ul, w.entry, w.img, w.icon, w.table, w.h3
    guhmon = entry(
        img("guhmon_gymleider", "Gymleider Dutjes"), "Guhmon Battle", "Guhmon-gevecht",
        p("<b>Gym Leader Naps</b> stands in the Guhpixel lobby and takes on every challenger in his gym. You pick one of your own tamed "
          "guhs from a list (a copy appears in the gym; your real guh stays comfy at home), or the gym's <b>Loaner Guh</b> if you have "
          "none. Then a real battle screen opens: two guhs, four moves, a text box.",
          "<b>Gymleider Dutjes</b> staat in de lobby van Guhpixel en neemt het in zijn gym op tegen elke uitdager. Je kiest een van je "
          "eigen tamme guhs uit een lijst (er verschijnt een kopie in de gym; je echte guh blijft lekker thuis), of de <b>Leenguh</b> van "
          "de gym als je er geen hebt. Daarna opent een echt gevechtsscherm: twee guhs, vier zetten, een tekstvak.")
        + p("There is no HP. Each guh has a <b>SLEEP bar</b>, no move hurts, and whoever falls asleep <i>first</i> wins. Snorkel, the "
            "gym leader's guh, is a little drowsy already and starts at 30. Lose? Nothing is lost: press <i>Try again!</i>.",
            "Er is geen HP. Elke guh heeft een <b>SLAAPbalk</b>, geen enkele zet doet pijn, en wie het <i>eerst</i> slaapt, wint. Snurkel, "
            "de guh van de gymleider, is al een beetje slaperig en begint op 30. Verloren? Je raakt niets kwijt: druk op <i>Nog een keer!</i>.")
        + table([("Move", "Zet"), ("What it does", "Wat het doet")], [
            [("Chonk", "Vadsen"), ("+25 sleep for your own guh. \"It's super chonky!\"", "+25 slaap voor je eigen guh. \"Het is super vadsig!\"")],
            [("Nyeg", "Njeg"), ("-15 sleep for the other guh (\"it's not very effective...\"), and its next Nap fails.",
                                "-15 slaap bij de andere guh (\"het is niet erg effectief...\"), en zijn volgende Dutje mislukt.")],
            [("Eat Nibble", "Knabbel eten"), ("+10 now and +10 at the start of your next two turns (a full tummy).",
                                              "+10 nu en +10 aan het begin van je volgende twee beurten (een vol buikje).")],
            [("Nap", "Dutje"), ("+40, but it fails if the last thing the other guh did was Nyeg.",
                                "+40, maar het mislukt als de andere guh als laatste Njeg deed.")]])
        + p("The first win pays <b>100 Guhpixel Coins</b> and unlocks the film in the Guh Cinema. Keepsakes: the outfit "
            + icon("guhmon_trainerspet", "Guhmon Trainer Cap") + " <b>Guhmon Trainer Cap</b>, the " + icon("guhmon_badgedoos", "Badge Case")
            + " <b>Badge Case</b> (a decoration with room for eight badges) and three gym badges to put in it by right-clicking the case "
              "with a badge. Lost them? Gym Leader Naps has spares.",
            "De eerste winst levert <b>100 Guhpixel-muntjes</b> op en ontgrendelt de film in de Guhbioscoop. Aandenkens: de outfit "
            + icon("guhmon_trainerspet", "Guhmon-trainerspet") + " <b>Guhmon-trainerspet</b>, het " + icon("guhmon_badgedoos", "Badgedoosje")
            + " <b>Badgedoosje</b> (een decoratie met plek voor acht badges) en drie gymbadges die je erin legt door met een badge op het "
              "doosje te klikken. Kwijt? Gymleider Dutjes heeft reserve.")
        + table([("Badge", "Badge"), ("How", "Hoe")], [
            [icon("guhmon_badge_dutjes", "Nap Badge") + " " + t("Nap Badge", "Dutjesbadge"), ("Win the battle.", "Win het gevecht.")],
            [icon("guhmon_badge_njeg", "Nyeg Badge") + " " + t("Nyeg Badge", "Njegbadge"),
             ("Win a battle in which your Nyeg made one of Snorkel's Naps fail.", "Win een gevecht waarin jouw Njeg een Dutje van Snurkel liet mislukken.")],
            [icon("guhmon_badge_knabbel", "Nibble Badge") + " " + t("Nibble Badge", "Knabbelbadge"),
             ("Fall asleep while your tummy is still full from Eat Nibble.", "Val in slaap terwijl je buikje nog vol zit van Knabbel eten.")]])
        + p("The other five places in the case stay empty: those gyms are closed for a nap.",
            "De andere vijf plekjes in het doosje blijven leeg: die gyms zijn dicht wegens dutje."),
        stats=[(("Where", "Waar"), t("Guhpixel lobby", "Lobby van Guhpixel")), (("Players", "Spelers"), t("1 per gym, any number of gyms", "1 per gym, zoveel gyms als nodig")),
               (("Time", "Duur"), t("about 5 minutes", "ongeveer 5 minuten"))])
    bzg = entry(
        img("bzg_presentatrice", "Presentatrice Guhvon"), "Farmer Wants a Guh", "Boer zoekt Guh",
        p("<b>Host Guhvon</b> records her TV show in a studio next to the lobby, and you are in it. <b>Farmer Guhrrit</b> is looking "
          "for a guh to share his hay with. It is a TV parody and nothing more: when the credits have rolled, it is over.",
          "<b>Presentatrice Guhvon</b> neemt haar tv-programma op in een studio naast de lobby, en jij doet mee. <b>Boer Guhrrit</b> zoekt "
          "een guh om zijn hooi mee te delen. Het is een tv-parodie en meer niet: na de aftiteling is het klaar.")
        + ul([("<b>The intro</b>: talk to Guhvon on the stage.", "<b>De intro</b>: praat met Guhvon op het podium."),
              ("<b>The letters</b>: take the three letters out of the " + icon("bzg_brievenbus", "Mailbox") + " mailbox and read them all. "
               "Snoozie, Dozeline and Snorebert mostly write about how much they love sleeping.",
               "<b>De brieven</b>: haal de drie brieven uit de " + icon("bzg_brievenbus", "Brievenbus") + " brievenbus en lees ze allemaal. "
               "Tukkie, Dommelien en Snurkbert schrijven vooral hoe graag ze slapen."),
              ("<b>The stay-over week</b>: walk through the set wall to the farm. Everybody is asleep already; tuck all four of them in "
               "with a click.",
               "<b>De logeerweek</b>: loop door de decorwand naar de boerderij. Iedereen slaapt al; stop ze alle vier in met een klik."),
              ("<b>The choice</b>: Farmer Guhrrit wakes up and asks your advice.", "<b>De keuze</b>: Boer Guhrrit wordt wakker en vraagt jouw advies."),
              ("<b>The credits</b>.", "<b>De aftiteling</b>.")])
        + p("The first time pays <b>100 Guhpixel Coins</b> and unlocks the film. Keepsakes: the outfit " + icon("bzg_strohoed", "Straw hat")
            + " <b>Farmer Guhrrit's Straw Hat</b> with " + icon("bzg_overall", "Overalls") + " <b>Overalls</b>, and the "
            + icon("bzg_ingelijste_brief", "Framed Letter") + " <b>Framed Letter</b>: his thank-you note for on your wall (right-click to "
              "read it). You can also craft a mailbox for at home; it never has any mail.",
            "De eerste keer levert <b>100 Guhpixel-muntjes</b> op en ontgrendelt de film. Aandenkens: de outfit " + icon("bzg_strohoed", "Strohoed")
            + " <b>Strohoed van Boer Guhrrit</b> met " + icon("bzg_overall", "Overall") + " <b>Overall</b>, en de "
            + icon("bzg_ingelijste_brief", "Ingelijste brief") + " <b>Ingelijste brief</b>: zijn bedankbriefje voor aan je muur (rechtsklik "
              "om te lezen). Je kunt ook een brievenbus voor thuis maken; er zit nooit post in.")
        + w.recipe_card("bzg_brievenbus", "Mailbox", "Brievenbus"),
        stats=[(("Where", "Waar"), t("Guhpixel lobby", "Lobby van Guhpixel")), (("Players", "Spelers"), t("1 per studio, any number of studios", "1 per studio, zoveel studio's als nodig")),
               (("Time", "Duur"), t("about 5 minutes", "ongeveer 5 minuten"))])
    def kaarten(rijen):
        return '<div class="cards">' + "".join(
            f'<div class="card"><figure class="stage">{img(n, en)}</figure><h3>{t(en, nl)}</h3><p>{t(den, dnl)}</p></div>'
            for n, en, nl, den, dnl in rijen) + "</div>"

    guhmon_kaarten = kaarten([
        ("guhmon_gym", "The Nap Gym", "De Dutjesgym", "A field with a nibble ball in the middle, the challenger's mat, and stands "
         "full of sleeping fans.", "Een veld met een knabbelbal in het midden, de mat van de uitdager en tribunes vol slapende fans."),
        ("guhmon_badgedoos", "Badge Case", "Badgedoosje", "Here with all three badges in it.", "Hier met alle drie de badges erin.")])
    bzg_kaarten = kaarten([
        ("bzg_studio", "Studio and farm set", "Studio en boerderijdecor", "The stage on the left, the farm behind the set wall on the right.",
         "Links het podium, rechts achter de decorwand de boerderij."),
        ("bzg_boer", "Farmer Guhrrit", "Boer Guhrrit", "Straw hat, overalls, and a straw in his mouth.", "Strohoed, overall en een strootje in zijn mond."),
        ("bzg_brievenbus", "Mailbox", "Brievenbus", "The flag is up: there is mail (only in the show).", "Het vlaggetje staat omhoog: er is post (alleen in de show)."),
        ("bzg_ingelijste_brief", "Framed Letter", "Ingelijste brief", "Right-click to read the thank-you note.", "Rechtsklik om het bedankbriefje te lezen.")])
    return guhmon + guhmon_kaarten + bzg + bzg_kaarten


def renders(r):
    out = r.OUT
    geo = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    try:
        for kind in ("guhmon_gymleider", "bzg_presentatrice", "bzg_boer"):
            q = r.geo_quads(os.path.join(geo, f"guh_npc_{kind}.geo.json"), f"guhs:entity/npc_{kind}")
            r.render(q, yaw=32, pitch=-14, size=420).save(os.path.join(out, f"{kind}.png"))
        for blok, yaw in (("guhmon_badgedoos", 30), ("bzg_brievenbus", 30), ("bzg_ingelijste_brief", 25)):
            quads = r.model_quads(f"guhs:block/{blok}")
            if blok == "guhmon_badgedoos":          # (shown with all three badges in it)
                for badge in ("dutjes", "njeg", "knabbel"):
                    quads = quads + r.model_quads(f"guhs:block/guhmon_badgedoos_{badge}")
            beeld = r.render(quads, yaw, -30, 320)
            beeld.save(os.path.join(out, f"{blok}.png"))
            beeld.resize((64, 64), r.Image.LANCZOS).save(os.path.join(out, f"icon_{blok}.png"))
        for item in ("guhmon_badge_dutjes", "guhmon_badge_njeg", "guhmon_badge_knabbel", "guhmon_trainerspet", "bzg_strohoed", "bzg_overall"):
            r.item_icon(f"guhs:item/{item}").save(os.path.join(out, f"icon_{item}.png"))
        for naam, sid in (("guhmon_gym", "guhpixel/guhmon_gym"), ("bzg_studio", "guhpixel/bzg_studio")):
            st = r.load_structure(sid)
            if st is not None:
                r.render_structure(st, {}, px=10, max_size=1100, cutaway=True).save(os.path.join(out, f"{naam}.png"))
        print("rendered the grap2 pictures (guhmon_*, bzg_*)")
    except Exception as e:      # (a missing generator output: the wiki still builds, the picture check of the site reports it)
        print("no render for guhpixel grap2:", e)
