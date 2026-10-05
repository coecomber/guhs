"""
The wiki part of the guhpixel slice "among": Among Guhs, the real game (the engine: rules, roles, the ship, sabotage,
meetings, rewards). body(w) returns the HTML; renders(r) draws the ship map and the three ship blocks.

Every text is written twice, (English, Dutch); the English follows tools/lang/GLOSSARY.md and tools/lang/glossary_px.
"""
import os
import sys


def body(w):
    p, ul, t, table, entry, img, h3 = w.p, w.ul, w.t, w.table, w.entry, w.img, w.h3
    out = h3("Among Guhs", "Among Guhs")
    out += entry(img("among_vadsvaarder", "De Vadsvaarder"), "The real game on The Chonkfarer", "Het echte spel op De Vadsvaarder",
                 p("Talk to <b>Captain Guh</b> in the Guhpixel lobby to join the queue. Everybody in the queue presses <i>ready</i>; when all real "
                   "players are ready the round starts on its own ship (alone: at once). Empty places are filled with guhs in space suits, each in "
                   "its own color. Several groups can play at the same time. A round takes about ten to fifteen minutes.",
                   "Praat met de <b>Kapitein-guh</b> in de Guhpixel-lobby om in de wachtrij te komen. Iedereen in de wachtrij drukt op <i>klaar</i>; "
                   "als alle echte spelers klaar zijn begint de ronde op een eigen schip (alleen: meteen). Lege plekken worden gevuld met guhs in "
                   "ruimtepakjes, elk in een eigen kleur. Meerdere groepen kunnen tegelijk spelen. Een ronde duurt zo'n tien tot vijftien minuten.") +
                 p("Nobody is ever hurt. Out of the game means <b>pushed asleep</b>: you go on as a <b>dream guh</b> (see-through, floating, "
                   "through walls), you still finish your tasks and only other dream guhs hear you. Your own inventory waits in the safe and "
                   "comes back exactly as it was.",
                   "Niemand wordt ooit pijn gedaan. Uit het spel betekent <b>in slaap geduwd</b>: je gaat verder als <b>droomguh</b> (doorzichtig, "
                   "zwevend, door muren), je maakt je taken nog af en alleen andere droomguhs horen je. Je eigen spullen wachten in de kluis en "
                   "komen precies zo terug."),
                 stats=[(("Participants", "Deelnemers"), t("9 (Normal) or 10 (Hard)", "9 (Normaal) of 10 (Lastig)")),
                        (("Mikas", "Mika's"), t("1 (Normal) or 2 (Hard)", "1 (Normaal) of 2 (Lastig)")),
                        (("Tasks per crew guh", "Taken per crewguh"), "7")], wide=True)
    out += h3("Crew and Mika", "Crew en Mika")
    out += table([("Role", "Rol"), ("What you do", "Wat je doet"), ("You win when", "Je wint als")], [
        ["<b>Crew</b>", ("Do your tasks at the panels (the list is on the left of your screen), right-click a sleeper to report it, press the "
                         "emergency button in the Cafeteria once per round when you saw something odd.",
                         "Doe je taken bij de panelen (de lijst staat links op je scherm), meld een slaper met rechtsklik, druk een keer per ronde "
                         "op de noodknop in de Kantine als je iets geks zag."),
         ("every task of the crew is done, or no Mika is awake any more.", "alle taken van de crew af zijn, of er geen Mika meer wakker is.")],
        ["<b>Mika</b>", ("Push crew guhs asleep with the Mika Pillow (two blocks away, a cooldown of about half a minute), crawl through the vents, "
                         "sabotage with the Sabotage Map and pretend to do tasks.",
                         "Duw crewguhs in slaap met het Mika-kussentje (twee blokken ver, een afkoeltijd van ruim een halve minuut), kruip door de "
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
    out += table([("Task", "Taak"), ("Where", "Waar")], [
        [("Tying sausages", "Worstjes knopen"), ("Electrical, Navigation or the Storage Room", "Elektra, de Navigatie of de Voorraadkamer")],
        [("Card through the reader", "Pasje door de lezer"), ("Navigation", "De Navigatie")],
        [("Emptying the crumb tray", "Kruimelbak legen"), ("the Cafeteria or the Dorm", "De Kantine of de Slaapzaal")],
        [("Fuelling peanut sauce", "Pindasaus tanken"), ("the Storage Room, then the Engine Room", "De Voorraadkamer, daarna de Machinekamer")],
        [("Sorting nibbles", "Knabbels sorteren"), ("the Storage Room or the Sick Bay", "De Voorraadkamer of de Ziekenboeg")],
        [("Downloading dreams", "Dromen downloaden"), ("the Dorm, Navigation or the Shield Room, then the Cafeteria", "De Slaapzaal, de Navigatie of de Schildkamer, daarna de Kantine")],
        [("Weighing", "Wegen"), ("the Sick Bay (result: chonk)", "De Ziekenboeg (resultaat: vads)")],
        [("Setting the switches", "Schakelaars goedzetten"), ("Electrical, the Reactor or the Shield Room", "Elektra, de Reactor of de Schildkamer")],
    ])
    out += p("For now every panel is a waiting panel: stay at it until the bar is full. The real mini-games come later; the list of tasks stays the same.",
             "Voorlopig is elk paneel een wachtpaneel: blijf erbij tot de balk vol is. De echte spelletjes komen later; de lijst met taken blijft gelijk.")
    out += h3("Sabotage", "Saboteren")
    out += ul([
        ("<b>Lights out</b>: the crew sees very little until somebody sets the switches in Electrical.",
         "<b>Licht uit</b>: de crew ziet bijna niks tot iemand in Elektra de schakelaars goedzet."),
        ("<b>Nibble Alarm</b>: the code must be typed in at the Reactor AND in Navigation within 45 seconds, or the Mika wins.",
         "<b>Knabbelalarm</b>: de code moet binnen 45 tellen bij de Reactor EN in de Navigatie worden ingetoetst, anders wint de Mika."),
        ("<b>Doors shut</b>: the doors of one room are closed for 10 seconds.", "<b>Deuren dicht</b>: de deuren van een kamer zitten 10 tellen dicht."),
    ])
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
        [("Per own finished task (crew)", "Per eigen afgemaakte taak (crew)"), "+2", "+3"],
        [("At most per real day", "Hooguit per echte dag"), "200", "200"],
    ])
    out += p("Only a round played to the end pays Guhpixel Coins. The <b>Logbook Guh</b> beside the Captain tells your own numbers (rounds, wins as "
             "crew and as Mika, times wrongly voted out); they are also in the Guhdex. Titles: <b>Sus</b> (voted out three times), <b>Wrongly "
             "Voted Out</b> and <b>Pillow Champion</b> (five wins as Mika).",
             "Alleen een ronde die je uitspeelt geeft Guhpixel-muntjes. De <b>Logboek-guh</b> naast de Kapitein vertelt je eigen cijfers (rondes, "
             "winst als crew en als Mika, keren onterecht weggestemd); ze staan ook in de Guhdex. Titels: <b>Sus</b> (drie keer weggestemd), "
             "<b>Onterecht weggestemd</b> en <b>Kussenkampioen</b> (vijf keer gewonnen als Mika).")
    out += entry(img("among_noodknop", "Noodknop"), "The ship's things", "De spullen van het schip",
                 p("The <b>Task Panel</b>, the <b>Emergency Button</b> and the <b>Vent Hatch</b> are real blocks. At home they are decoration with a "
                   "little joke when you click them.",
                   "Het <b>Taakjes-paneel</b>, de <b>Noodknop</b> en het <b>Ventilatieluik</b> zijn echte blokken. Thuis zijn het versieringen met "
                   "een grapje als je erop klikt."))
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
        for blok in ("among_taakpaneel", "among_noodknop", "among_ventilatieluik"):
            plaatje = r.render(r.model_quads(f"guhs:block/{blok}"), 30, -25, 320)
            plaatje.save(os.path.join(r.OUT, f"{blok}.png"))
            plaatje.resize((64, 64), Image.LANCZOS).save(os.path.join(r.OUT, f"icon_{blok}.png"))
        for item in ("among_kussen", "among_saboteerkaart", "among_stembriefje"):
            r.item_icon(f"guhs:item/{item}").save(os.path.join(r.OUT, f"icon_{item}.png"))
        print("rendered among_vadsvaarder, the three ship blocks, the three game items")
    except Exception as e:
        print("no render for the guhpixel slice among", e)
