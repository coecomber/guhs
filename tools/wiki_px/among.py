"""
The wiki part of the guhpixel slice "among": Among Guhs: the practice round, the real game (rules, roles, the ship, the
eight task mini-games, sabotage, meetings, rewards), the shop table and the ship things at home. body(w) returns the HTML;
renders(r) draws the ship map, the ship blocks, the SUS-stickerbord and the icons.

Every text is written twice, (English, Dutch); the English follows tools/lang/GLOSSARY.md and tools/lang/glossary_px.
"""
import os
import sys


def body(w):
    p, ul, t, table, entry, img, h3 = w.p, w.ul, w.t, w.table, w.entry, w.img, w.h3
    out = h3("Among Guhs", "Among Guhs")
    out += entry(img("among_sus_bord", "SUS-stickerbord"), "The practice round", "Het oefenrondje",
                 p("The first time <b>Captain Guh</b> in the Guhpixel lobby takes you along for a practice round: a short flight on The "
                   "Chonkfarer, alone with a crew of guhs, in four steps and about four minutes. You do one real task, you have a look at how "
                   "the rest of the crew is getting on, there is a meeting with a vote, and somewhere on board there is said to be a Mika. "
                   "Nothing can go wrong and there is no clock.",
                   "De eerste keer neemt de <b>Kapitein-guh</b> in de Guhpixel-lobby je mee voor een oefenrondje: een kort vluchtje op De "
                   "Vadsvaarder, alleen met een crew van guhs, in vier stappen en ongeveer vier minuten. Je doet één echte taak, je gaat kijken "
                   "hoe de rest van de crew opschiet, er is een vergadering met een stemming, en ergens aan boord schijnt een Mika te zitten. "
                   "Er kan niks misgaan en er loopt geen klok.") +
                 p("The first time you finish it you get <b>100 Guhpixel Coins</b>, the <b>SUS Sticker Board</b> for on your wall and a film for "
                   "the Guh Cinema, and the real game opens. You can play it again whenever you like (no second reward); a lost board comes back "
                   "from the Captain.",
                   "De eerste keer dat je het uitspeelt krijg je <b>100 Guhpixel-muntjes</b>, het <b>SUS-stickerbord</b> voor aan je muur en een "
                   "film voor de Guhbioscoop, en gaat het echte spel open. Je mag het zo vaak overdoen als je wilt (geen tweede beloning); een "
                   "kwijtgeraakt bord krijg je terug van de Kapitein."),
                 stats=[(("Steps", "Stappen"), "4"), (("Reward (once)", "Beloning (eenmalig)"), t("100 coins, SUS Sticker Board", "100 muntjes, SUS-stickerbord")),
                        (("Unlocks", "Maakt vrij"), t("the real game", "het echte spel"))], wide=True)
    out += entry(img("among_vadsvaarder", "De Vadsvaarder"), "The real game on The Chonkfarer", "Het echte spel op De Vadsvaarder",
                 p("Talk to <b>Captain Guh</b> in the Guhpixel lobby to join the queue. Everybody in the queue presses <i>ready</i>; when all real "
                   "players are ready the round starts on its own ship (alone: at once). Empty places are filled with guhs in space suits, each in "
                   "its own color. Several groups can play at the same time. A round takes about ten to fifteen minutes. You have to finish the "
                   "practice round first.",
                   "Praat met de <b>Kapitein-guh</b> in de Guhpixel-lobby om in de wachtrij te komen. Iedereen in de wachtrij drukt op <i>klaar</i>; "
                   "als alle echte spelers klaar zijn begint de ronde op een eigen schip (alleen: meteen). Lege plekken worden gevuld met guhs in "
                   "ruimtepakjes, elk in een eigen kleur. Meerdere groepen kunnen tegelijk spelen. Een ronde duurt zo'n tien tot vijftien minuten. "
                   "Je moet eerst het oefenrondje uitspelen.") +
                 p("Nobody is ever hurt. Out of the game means <b>pushed asleep</b>: you go on as a <b>dream guh</b> (see-through, floating, "
                   "through walls), you still finish your tasks and only other dream guhs hear you. Your own inventory waits in the safe and "
                   "comes back exactly as it was.",
                   "Niemand wordt ooit pijn gedaan. Uit het spel betekent <b>in slaap geduwd</b>: je gaat verder als <b>droomguh</b> (doorzichtig, "
                   "zwevend, door muren), je maakt je taken nog af en alleen andere droomguhs horen je. Je eigen spullen wachten in de kluis en "
                   "komen precies zo terug."),
                 stats=[(("Participants", "Deelnemers"), t("9 (Normal) or 10 (Hard)", "9 (Normaal) of 10 (Lastig)")),
                        (("Mikas", "Mika's"), t("1 (Normal) or 2 (Hard)", "1 (Normaal) of 2 (Lastig)")),
                        (("Tasks per crew guh", "Taken per crewguh"), "10")], wide=True)
    out += h3("Crew and Mika", "Crew en Mika")
    out += table([("Role", "Rol"), ("What you do", "Wat je doet"), ("You win when", "Je wint als")], [
        ["<b>Crew</b>", ("Do your tasks at the panels (the list is on the left of your screen), right-click a sleeper to report it, press the "
                         "emergency button in the Cafeteria once per round when you saw something odd.",
                         "Doe je taken bij de panelen (de lijst staat links op je scherm), meld een slaper met rechtsklik, druk een keer per ronde "
                         "op de noodknop in de Kantine als je iets geks zag."),
         ("every task of the crew is done, or no Mika is awake any more.", "alle taken van de crew af zijn, of er geen Mika meer wakker is.")],
        ["<b>Mika</b>", ("Push crew guhs asleep with the Mika Pillow (two blocks away, a cooldown of almost a minute), crawl through the vents, "
                         "sabotage with the Sabotage Map and pretend to do tasks.",
                         "Duw crewguhs in slaap met het Mika-kussentje (twee blokken ver, een afkoeltijd van bijna een minuut), kruip door de "
                         "luiken, saboteer met de Saboteerkaart en doe alsof je taken doet."),
         ("as many Mikas as crew guhs are awake, or the Nibble Alarm runs out.", "er net zoveel Mika's als crewguhs wakker zijn, of het Knabbelalarm afloopt.")],
    ])
    out += h3("The ship", "Het schip")
    out += p("Nine rooms with corridors between them: the <b>Cafeteria</b> (the meeting table with the emergency button), the <b>Dorm</b> (everybody who "
             "sleeps is carried to a bed there), the <b>Reactor</b>, the <b>Engine Room</b>, <b>Navigation</b>, the <b>Storage Room</b>, the "
             "<b>Sick Bay</b>, <b>Electrical</b> and the <b>Shield Room</b>. Three vent networks connect Dorm - Reactor - Engine Room, "
             "Sick Bay - Electrical - Storage Room and Navigation - Shield Room: only a Mika fits through.",
             "Negen kamers met gangen ertussen: de <b>Kantine</b> (de vergadertafel met de noodknop), de <b>Slaapzaal</b> (wie slaapt wordt daar naar "
             "een bed gedragen), de <b>Reactor</b>, de <b>Machinekamer</b>, de <b>Navigatie</b>, de <b>Voorraadkamer</b>, de <b>Ziekenboeg</b>, "
             "<b>Elektra</b> en de <b>Schildkamer</b>. Drie netwerken van luiken verbinden Slaapzaal - Reactor - Machinekamer, "
             "Ziekenboeg - Elektra - Voorraadkamer en Navigatie - Schildkamer: daar past alleen een Mika door.")
    out += h3("Tasks", "Taken")
    out += p("Every crew guh gets ten tasks out of sixteen. Right-click the panel of a task (the list is in the top left of your screen): every "
             "kind is a mini-game of its own, the classics in guh style. The server checks every answer, and an answer that comes too fast does "
             "not count. Closing the panel early gives the work up.",
             "Elke crewguh krijgt tien taken uit zestien. Rechtsklik op het paneel van een taak (de lijst staat linksboven in beeld): elke soort "
             "is een eigen spelletje, de klassiekers in guh-stijl. De server controleert elk antwoord, en een antwoord dat te vlug komt telt "
             "niet. Wie het paneel eerder sluit geeft het werk op.")
    out += table([("Task", "Taak"), ("What you do", "Wat je doet"), ("Where", "Waar")], [
        [("Tying sausages", "Worstjes knopen"),
         ("Drag every sausage to the hook of its own color.", "Sleep elk worstje naar de haak van zijn eigen kleur."),
         ("Electrical, Navigation or the Storage Room", "Elektra, de Navigatie of de Voorraadkamer")],
        [("Card through the reader", "Pasje door de lezer"),
         ("Swipe the card through in one go: not too fast, not too slow.", "Haal het pasje in een keer door de lezer: niet te vlug, niet te traag."),
         ("Navigation", "De Navigatie")],
        [("Emptying the crumb tray", "Kruimelbak legen"),
         ("Hold the lever until every crumb has flown into space. Let go and it springs back.",
          "Houd de hendel vast tot alle kruimels de ruimte in zijn. Loslaten en hij veert terug."),
         ("the Cafeteria or the Dorm", "De Kantine of de Slaapzaal")],
        [("Fuelling peanut sauce", "Pindasaus tanken"),
         ("Two panels: fill the jerrycan up to the line, then pour it into the engine up to the line. Too much is spilled, and you start over.",
          "Twee panelen: tank de jerrycan vol tot de streep, giet hem daarna in de motor tot de streep. Te veel is gemorst, en dan begin je opnieuw."),
         ("the Storage Room, then the Engine Room", "De Voorraadkamer, daarna de Machinekamer")],
        [("Sorting nibbles", "Knabbels sorteren"),
         ("Drag six pieces into the right bin: cheese nibbles, plain nibbles, crumbs.",
          "Sleep zes stukjes in de goede bak: kaasknabbels, gewone knabbels, kruimels."),
         ("the Storage Room or the Sick Bay", "De Voorraadkamer of de Ziekenboeg")],
        [("Downloading dreams", "Dromen downloaden"),
         ("Two panels: press Download and wait for the bar, then Upload in the Cafeteria. It takes a while, and the estimate is never right.",
          "Twee panelen: druk op Download en wacht op de balk, daarna Upload in de Kantine. Dat duurt even, en de schatting klopt nooit."),
         ("the Dorm, Navigation or the Shield Room, then the Cafeteria", "De Slaapzaal, de Navigatie of de Schildkamer, daarna de Kantine")],
        [("Weighing", "Wegen"),
         ("Keep your mouse dead still on the scale. Result: chonk.", "Houd je muis doodstil op de weegschaal. Resultaat: vads."),
         ("the Sick Bay", "De Ziekenboeg")],
        [("Setting the switches", "Schakelaars goedzetten"),
         ("Six switches, each with a little lamp: set every switch the way its lamp shows.",
          "Zes schakelaars met elk een lampje: zet elke schakelaar zoals zijn lampje aangeeft."),
         ("Electrical, the Reactor or the Shield Room", "Elektra, de Reactor of de Schildkamer")],
    ])
    out += h3("Sabotage", "Saboteren")
    out += ul([
        ("<b>Lights out</b>: the crew sees very little until somebody flips all five switches up at the light panel in Electrical.",
         "<b>Licht uit</b>: de crew ziet bijna niks tot iemand bij het lichtpaneel in Elektra alle vijf de schakelaars omhoog zet."),
        ("<b>Nibble Alarm</b>: the four-digit code on the note must be typed in at the Reactor AND in Navigation within 45 seconds, or the Mika wins.",
         "<b>Knabbelalarm</b>: de code van vier cijfers op het briefje moet binnen 45 tellen bij de Reactor EN in de Navigatie worden ingetoetst, "
         "anders wint de Mika."),
        ("<b>Doors shut</b>: the doors of one room are closed for 10 seconds.", "<b>Deuren dicht</b>: de deuren van een kamer zitten 10 tellen dicht."),
    ])
    out += p("The Mika's <b>Sabotage Map</b> shows the whole ship: click a room to shut its doors, or press Lights Out or Nibble Alarm. A "
             "right-click on a vent shows the same map with the rooms you can crawl to in gold.",
             "De <b>Saboteerkaart</b> van de Mika laat het hele schip zien: klik op een kamer om zijn deuren te sluiten, of druk op Licht uit of "
             "Knabbelalarm. Een rechtsklik op een luik toont dezelfde kaart met in goud de kamers waar je heen kunt kruipen.")
    out += h3("Meetings", "Vergaderingen")
    out += p("A reported sleeper or the emergency button brings everybody who is awake to the table. First you talk, then you vote for a guh or skip. "
             "The guh NPCs say what they <b>really</b> saw: who was where and with whom, somebody coming out of a vent, a push. Sometimes they forget "
             "something or mix two guhs up, and an NPC Mika lies. You pick ready-made statements (<i>I was in..., I saw..., I suspect..., I vouch "
             "for...</i>); the NPCs weigh them in their vote, and a guh who knows better will say so. Real players can simply chat as well: "
             "<i>Chat</i> closes the screen, the Voting Slip opens it again.",
             "Een gemelde slaper of de noodknop brengt iedereen die wakker is naar de tafel. Eerst praat je, daarna stem je op een guh of sla je over. "
             "De guh-NPC's zeggen wat ze <b>echt</b> gezien hebben: wie waar was en met wie, iemand die uit een luik kwam, een duw. Soms vergeten ze "
             "iets of halen ze twee guhs door elkaar, en een NPC-Mika liegt. Jij kiest kant-en-klare uitspraken (<i>Ik was in..., Ik zag..., Ik "
             "verdenk..., Ik sta in voor...</i>); de NPC's wegen ze mee in hun stem, en een guh die beter weet zegt dat ook. Echte spelers kunnen ook "
             "gewoon chatten: <i>Chatten</i> sluit het scherm, het Stembriefje opent het weer.")
    out += p("Whoever gets the most votes (more than the skips) flies to the Dorm with a pillow, and everybody hears whether that was a Mika.",
             "Wie de meeste stemmen krijgt (meer dan er overslaan) vliegt met een kussen naar de Slaapzaal, en iedereen hoort of dat een Mika was.")
    out += h3("Rewards", "Beloning")
    out += table([("Round", "Ronde"), ("Normal", "Normaal"), ("Hard", "Lastig")], [
        [("Won", "Gewonnen"), "45", "68"], [("Lost", "Verloren"), "25", "38"],
        [("Per own finished task (crew, ten tasks)", "Per eigen afgemaakte taak (crew, tien taken)"), "+2", "+3"],
        [("At most per real day", "Hooguit per echte dag"), "200", "200"],
    ])
    out += p("Only a round played to the end pays Guhpixel Coins. The <b>Logbook Guh</b> beside the Captain keeps your personal stats board "
             "(rounds, wins as crew and as Mika, times voted out and wrongly voted out, tasks, today's coins); the numbers are also in the Guhdex.",
             "Alleen een ronde die je uitspeelt geeft Guhpixel-muntjes. De <b>Logboek-guh</b> naast de Kapitein houdt je eigen cijferbord bij "
             "(rondes, winst als crew en als Mika, keren weggestemd en onterecht weggestemd, taken, de muntjes van vandaag); de cijfers staan "
             "ook in de Guhdex.")
    out += table([("Title", "Titel"), ("How", "Hoe")], [
        ["<b>Sus</b>", ("Get voted out three times.", "Word drie keer weggestemd.")],
        [("<b>Wrongly Voted Out</b>", "<b>Onterecht weggestemd</b>"), ("Get voted out while you were not the Mika.", "Word weggestemd terwijl je niet de Mika was.")],
        [("<b>Pillow Champion</b>", "<b>Kussenkampioen</b>"), ("Win five rounds as Mika.", "Win vijf rondes als Mika.")],
        [("<b>Sleuth Guh</b>", "<b>Speurguh</b>"), ("Win ten rounds as crew.", "Win tien rondes als crew.")],
        [("<b>Task Guh</b>", "<b>Taakjesguh</b>"), ("Do a hundred tasks.", "Doe honderd taken.")],
    ])
    out += h3("In the shop", "In de winkel")
    out += p("After your first real round the <b>Shopkeeper Guh</b> sells the Among Guhs things for Guhpixel Coins. Suits and hats are clothes for "
             "your own guhs (you buy each once, every guh of yours can wear it); the ship things are real blocks you can buy as often as you like.",
             "Na je eerste echte ronde verkoopt de <b>Verkoper-guh</b> de spullen van Among Guhs voor Guhpixel-muntjes. Pakjes en hoedjes zijn "
             "kleren voor je eigen guhs (je koopt ze één keer, al je guhs kunnen ze dragen); de scheepsdingen zijn echte blokken die je zo vaak "
             "kunt kopen als je wilt.")
    out += table([("What", "Wat"), ("Coins", "Muntjes"), ("", "")], [
        [("<b>Space Suit</b> in red, blue, green, yellow, pink, orange, purple or white", "<b>Ruimtepakje</b> in rood, blauw, groen, geel, roze, oranje, paars of wit"),
         "30", ("With an air tank on the back. Whoever wears the red one is always sus.", "Met een luchttank op de rug. Wie het rode draagt is altijd sus.")],
        [("<b>Hats</b>: Little Plant, Fried Egg, Toilet Roll, Cheese Wedge, Sus Note, Nibble", "<b>Hoedjes</b>: plantje, ei, wc-rol, kaaspunt, sus-briefje, knabbel"),
         "25", ("Six hats for on your guh's head.", "Zes hoedjes voor op het hoofd van je guh.")],
        [("<b>Emergency Button</b>", "<b>Noodknop</b>"), "60",
         ("Calls your own guhs nearby to a meeting: they gather around the button, go through three agenda items and decide nothing.",
          "Roept je eigen guhs in de buurt bij elkaar voor een vergadering: ze gaan om de knop staan, lopen drie agendapunten af en besluiten niks.")],
        [("<b>Vent Hatch</b>", "<b>Ventilatieluik</b>"), "40",
         ("For in the floor. Now and then (and when you click it) a guh in a space suit peeks out.",
          "Voor in de vloer. Af en toe (en als je erop klikt) gluurt er een guh in een ruimtepakje uit.")],
        [("<b>Task Panel</b>", "<b>Taakjes-paneel</b>"), "40",
         ("Opens one of the eight task mini-games, just for fun. Reward: nothing.", "Opent een van de acht taakspelletjes, gewoon voor de lol. Beloning: niks.")],
    ])
    out += entry(img("among_noodknop", "Noodknop"), "The ship's things at home", "De scheepsdingen thuis",
                 p("A guh that sits, rides, sleeps in a little house or is busy with something else does not come to the meeting. If one of the "
                   "guhs at the meeting wears the red space suit, there is a decision after all.",
                   "Een guh die zit, rijdt, in een huisje slaapt of met iets anders bezig is komt niet naar de vergadering. Draagt een van de guhs "
                   "op de vergadering het rode ruimtepakje, dan valt er toch een besluit."))
    return out


def renders(r):
    """The ship map (drawn from the layout of guhpixel_among_bouw) and the three ship blocks."""
    from PIL import Image, ImageDraw
    sys.path.insert(0, "tools")
    from features import guhpixel_among_bouw as bouw
    from features import guhpixel_among_tekst as tekst
    try:
        px = 8
        kaart = Image.new("RGBA", (bouw.W * px, bouw.D * px), (24, 14, 30, 255))
        d = ImageDraw.Draw(kaart)
        kleuren = {"slaapzaal": (242, 140, 190), "kantine": (240, 210, 90), "navigatie": (120, 190, 240), "reactor": (140, 210, 110),
                   "ziekenboeg": (236, 236, 240), "elektra": (226, 180, 80), "machinekamer": (150, 150, 160), "voorraadkamer": (220, 140, 80),
                   "schildkamer": (90, 190, 200)}
        for gid, (x0, z0, x1, z1) in bouw.GANGEN.items():
            if gid == "gang_vm":
                x0, z0, x1, z1 = bouw.VM_BINNEN
                d.rectangle([35 * px, 24 * px, 40 * px - 1, 27 * px - 1], fill=(190, 186, 200, 255))
            d.rectangle([x0 * px, z0 * px, (x1 + 1) * px - 1, (z1 + 1) * px - 1], fill=(190, 186, 200, 255))
        for kid, (x0, z0, x1, z1, *_rest) in bouw.KAMERS.items():
            d.rectangle([x0 * px - 2, z0 * px - 2, (x1 + 1) * px + 1, (z1 + 1) * px + 1], fill=(250, 250, 252, 255))
            d.rectangle([x0 * px, z0 * px, (x1 + 1) * px - 1, (z1 + 1) * px - 1], fill=kleuren[kid] + (255,))
            naam = tekst.KAMERS[kid].replace("de ", "")
            hoog = 30 if kid == "kantine" else 5      # (the Kantine's name stands above its table)
            d.text(((x0 + x1 + 1) * px / 2 - len(naam) * 3, (z0 + z1 + 1) * px / 2 - hoog), naam, fill=(40, 22, 48, 255))
        for did, (kamer, x0, z0, x1, z1, *_rest) in bouw.DEUREN.items():
            d.rectangle([x0 * px, z0 * px, (x1 + 1) * px - 1, (z1 + 1) * px - 1], fill=(190, 186, 200, 255))
        for pid in bouw.PANELEN:
            bx, by, bz, facing, sx, sz = bouw.paneel_plek(pid)
            d.rectangle([sx * px + 1, sz * px + 1, (sx + 1) * px - 2, (sz + 1) * px - 2], fill=(60, 30, 70, 255))
        for lid, (kamer, net, x, z) in bouw.LUIKEN.items():
            d.rectangle([x * px, z * px, (x + 1) * px - 1, (z + 1) * px - 1], fill=(40, 40, 50, 255), outline=(230, 230, 240, 255))
        x0, z0, x1, z1 = bouw.TAFEL
        d.rectangle([x0 * px, z0 * px, (x1 + 1) * px - 1, (z1 + 1) * px - 1], fill=(250, 250, 250, 255))
        d.ellipse([bouw.KNOP_POS[0] * px, bouw.KNOP_POS[2] * px, (bouw.KNOP_POS[0] + 1) * px - 1, (bouw.KNOP_POS[2] + 1) * px - 1], fill=(224, 54, 48, 255))
        kaart.save(os.path.join(r.OUT, "among_vadsvaarder.png"))
        for blok in ("among_taakpaneel", "among_noodknop", "among_ventilatieluik", "among_sus_bord"):
            plaatje = r.render(r.model_quads(f"guhs:block/{blok}"), 30, -25, 320)
            if blok == "among_sus_bord":
                plaatje = plaatje.transpose(Image.FLIP_LEFT_RIGHT)     # (this view shows a north face mirrored: the letters must read "SUS")
            plaatje.save(os.path.join(r.OUT, f"{blok}.png"))
            plaatje.resize((64, 64), Image.LANCZOS).save(os.path.join(r.OUT, f"icon_{blok}.png"))
        for item in ("among_kussen", "among_saboteerkaart", "among_stembriefje"):
            r.item_icon(f"guhs:item/{item}").save(os.path.join(r.OUT, f"icon_{item}.png"))
        print("rendered among_vadsvaarder, the three ship blocks, the SUS-stickerbord, the three game items")
    except Exception as e:
        print("no render for the guhpixel slice among", e)
