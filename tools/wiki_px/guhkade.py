"""
The wiki part of the guhpixel slice "guhkade": the two Guhkade cabinets, their games, the top 5 and the guhs that play.

body(w) returns the HTML of this slice's part, built with the helpers of tools/make_wiki.py. Every text is written twice,
(English, Dutch); the English follows tools/lang/GLOSSARY.md and tools/lang/glossary_px/guhkade.md.
renders(r) adds the pictures docs/wiki/img/guhkade_*.png: the cabinets (from the block models) and four pictures of the
games' own screens. Those are put together from guhkade_schermen.json: the draw commands (rectangles and sprites) of four
moments of the real games, written out once from the Java simulation (guhkade.spel: FlappySim, PongSim, Schermen), drawn
here with the mod's own sprite sheet. Change a game's looks? Then that file wants a fresh dump.
"""
import json
import os

# the sprite table of the sheet (name: u, v, b, h) lives in the generator of the slice
SCHERMEN = os.path.join(os.path.dirname(__file__), "guhkade_schermen.json")
PLAFONDS = [
    # (English kind, Dutch kind, Flappy Guh, Mika-Pong, (English note, Dutch note))
    ("Guh (and most others)", "Guh (en de meeste andere)", 16, 40, ("the plain all-rounder", "de gewone alleskunner")),
    ("Cloud Guh", "Wolkguh", 38, 36, ("it can really fly", "die kan echt vliegen")),
    ("Wahoog Enderguh", "Vahoege Enderguh", 40, 64, ("the best there is at both", "de allerbeste in allebei")),
    ("Ghost Guh", "Spookguh", 32, 34, ("floats nicely; the rolling guh rolls right through it", "zweeft mooi; de rollende guh rolt er dwars doorheen")),
    ("Dachshund Guh", "Teckelguh", 12, 70, ("one long paddle on short legs", "één lang batje op korte pootjes")),
    ("Pinguh", "Pinguh", 10, 66, ("cannot fly, slides like the best", "kan niet vliegen, glijdt als de beste")),
    ("Brontosaurus Guh", "Brontosaurusguh", 12, 62, ("that neck keeps bumping the pillars", "die nek tikt steeds de pilaren aan")),
]


def body(w):
    out = [w.h3("The Guhcade", "De Guhkade")]
    out.append(w.entry(
        w.img("guhkade_kasten", "The two Guhcade cabinets"), "Arcade cabinets for your home", "Speelkasten voor thuis",
        w.p("The <b>Shopkeeper Guh</b> in the Guhpixel lobby sells two real arcade cabinets: <b>Flappy Guh</b> "
            "(<code>guhs:guhkade_kast_flappy</code>) and <b>Mika Pong</b> (<code>guhs:guhkade_kast_pong</code>). Your first cabinet, whichever you "
            "pick, costs <b>250 coins</b>; every one after that <b>150</b>. You can buy as many as you like. A cabinet is two blocks tall, "
            "gives a little light, and its screen really moves: while nobody plays it shows a demo and the best score. Break it (an axe is "
            "quickest) and it drops itself; its score list stays with the place, not with the item.",
            "De <b>Verkoper-guh</b> in de Guhpixel-lobby verkoopt twee echte speelkasten: <b>Flappy Guh</b> "
            "(<code>guhs:guhkade_kast_flappy</code>) en <b>Mika-Pong</b> (<code>guhs:guhkade_kast_pong</code>). Je eerste kast, welke je ook "
            "kiest, kost <b>250 muntjes</b>; elke volgende <b>150</b>. Je mag er zoveel kopen als je wilt. Een kast is twee blokken hoog, "
            "geeft een beetje licht en het scherm beweegt echt: speelt er niemand, dan zie je een demo en de beste score. Sloop je hem (met "
            "een bijl gaat het het snelst), dan krijg je de kast terug; de scorelijst hoort bij de plek, niet bij het voorwerp.")
        + w.p("Right-click either half to play. Only one player (or guh) at a time: somebody else has to wait until you walk away.",
              "Rechtsklik op de kast om te spelen. Er kan maar één speler (of guh) tegelijk aan de knoppen: een ander wacht tot je wegloopt."),
        stats=[(("First cabinet", "Eerste kast"), w.t("250 coins", "250 muntjes")), (("Every next one", "Elke volgende"), w.t("150 coins", "150 muntjes")),
               (("Recipe", "Recept"), w.t("none: shop only", "geen: alleen in de winkel"))], wide=True))
    out.append(w.entry(
        w.img("guhkade_flappy", "Flappy Guh"), "Flappy Guh", "Flappy Guh",
        w.p("A guh with teeny-tiny wings between <b>cheese nibble pillars</b>. One button: <b>space</b>, <b>W</b> or a <b>click</b> makes it "
            "flap. Every pillar you pass is a point. The gaps start out wide and get a little narrower, and the pillars come a little faster, "
            "until about pillar 28; after that it stays the same. Touch a pillar or the floor and the guh plops down, dizzy (nothing hurts): "
            "game over.",
            "Een guh met piepkleine vleugeltjes tussen de <b>kaasknabbelpilaren</b>. Eén knop: met <b>spatie</b>, <b>W</b> of een <b>klik</b> "
            "fladdert hij. Elke pilaar die je voorbij bent is een punt. De gaten beginnen ruim en worden een beetje smaller, en de pilaren "
            "komen een beetje sneller, tot ongeveer pilaar 28; daarna blijft het zo. Raak je een pilaar of de vloer, dan ploft de guh duizelig "
            "neer (niks doet pijn): potje voorbij."),
        stats=[(("Points", "Punten"), w.t("1 for every pillar", "1 per pilaar")), (("Keys", "Toetsen"), w.t("space, W, arrow up or click", "spatie, W, pijl omhoog of klik"))]))
    out.append(w.entry(
        w.img("guhkade_pong", "Mika Pong"), "Mika Pong", "Mika-Pong",
        w.p("The ball is a <b>rolling guh</b> (it thinks this is great fun), your paddle is a cheese nibble stick on the left, and on the right "
            "a <b>Mika</b> shoves the guh back. Move your paddle with <b>W</b> and <b>S</b> or the arrow keys. Rolling the guh back is "
            "<b>1 point</b>, rolling it past the Mika is <b>5</b>. Where the guh touches your paddle decides the angle. It rolls a little "
            "faster with every tap, and the Mika gets quicker with every goal against it. Three times past you (the three hearts) and the "
            "game is over.",
            "Het balletje is een <b>rollende guh</b> (die vindt het prachtig), je batje is een kaasknabbelstokje links, en rechts duwt een "
            "<b>Mika</b> de guh terug. Beweeg je batje met <b>W</b> en <b>S</b> of de pijltjes. De guh terugrollen is <b>1 punt</b>, hem langs "
            "de Mika rollen is er <b>5</b>. Waar de guh je batje raakt bepaalt de hoek. Hij rolt bij elke tik een beetje sneller, en de Mika "
            "wordt vlugger bij elk doelpunt tegen. Drie keer langs jou (de drie hartjes) en het potje is voorbij."),
        stats=[(("Points", "Punten"), w.t("1 for a tap, 5 past the Mika", "1 per tik, 5 langs de Mika")), (("Keys", "Toetsen"), w.t("W / S or the arrow keys", "W / S of de pijltjes")),
               (("Longest game", "Langste potje"), w.t("10 minutes", "10 minuten"))]))
    out.append(w.entry(
        w.img("guhkade_titel", "The title screen with the best score"), "The top 5", "De top 5",
        w.p("Every cabinet keeps its <b>own</b> top 5 with names, shown next to the game. Everybody has one line with their best score on that "
            "cabinet; a guh's name is pink with a little guh in front of it. A new cabinet comes with three easy names on it (GUH, VDS and "
            "NJG), like a real one. The score is counted by the server: your game is played again there, step by step. Your best scores ever "
            "and the guhs you have beaten are in the Guhdex (tab Guhpixel & Outings).",
            "Elke kast houdt zijn <b>eigen</b> top 5 met namen bij, naast het spel. Iedereen heeft één regel met zijn beste score op die kast; "
            "de naam van een guh is roze met een guhje ervoor. Op een nieuwe kast staan al drie makkelijke namen (GUH, VDS en NJG), net als "
            "op een echte. De server telt de score: je potje wordt daar stap voor stap nagespeeld. Je beste scores ooit en de guhs die je "
            "verslagen hebt staan in de Guhdex (tab Guhpixel & uitjes).")))
    out.append(w.entry(
        w.img("guhkade_af", "A guh's game is over"), "Your guhs play too", "Je guhs spelen mee",
        w.p("Tame guhs that walk around freely within 12 blocks of a cabinet (also the residents of a Guh House when they are outside) now "
            "and then walk up to it, stand in front of the screen and play a whole game: you see their game on the cabinet, it beeps, the "
            "guh hops along. Its score goes on the list under its own name. They only do it when a player is around to see it, and they "
            "rest a few minutes between games.",
            "Tamme guhs die vrij rondlopen binnen 12 blokken van een kast (ook de bewoners van een Guhhuisje als ze buiten zijn) lopen er af "
            "en toe naartoe, gaan voor het scherm staan en spelen een heel potje: je ziet hun spel op de kast, hij piept, de guh hupt mee. "
            "Zijn score komt onder zijn eigen naam op de lijst. Ze doen het alleen als er een speler in de buurt is om het te zien, en ze "
            "rusten een paar minuten tussen twee potjes.")
        + w.p("A guh starts out clumsy (about a fifth of what it will ever manage) and gets better with every game, creeping up to a "
              "<b>ceiling</b> that depends on its kind and a little on its character (a playful guh +15 %, a lazy one -15 %). After about "
              "twenty games it is two thirds of the way; the ceiling itself takes a long time, and no guh ever gets above it. Tough, but "
              "you can beat it.",
              "Een guh begint onhandig (ongeveer een vijfde van wat hij ooit haalt) en wordt met elk potje beter, tot een <b>plafond</b> dat "
              "van zijn soort afhangt en een beetje van zijn karakter (een speelse guh +15 %, een luie -15 %). Na een potje of twintig is hij "
              "op twee derde; het plafond zelf duurt lang, en geen guh komt er ooit boven. Lastig, maar te verslaan.")
        + w.p("<b>Beat a guh</b> (a higher score than its line on the same cabinet) and it looks glum for a moment: the Sniffles emote and "
              "a tear. Then it <b>practices extra</b>: it comes back much sooner for three games, and those count double. If it gets its "
              "place back, you get a message. A guh that was not around finds out the next time it walks by.",
              "<b>Versla een guh</b> (een hogere score dan zijn regel op dezelfde kast) en hij kijkt even sip: de emote Verdrietje en een "
              "traantje. Daarna gaat hij <b>extra oefenen</b>: hij komt drie potjes lang veel sneller terug, en die tellen dubbel. Pakt hij "
              "zijn plek terug, dan krijg je een berichtje. Een guh die er niet bij was merkt het de volgende keer dat hij langsloopt.")
        + w.table([("Kind of guh", "Soort guh"), ("Flappy Guh", "Flappy Guh"), ("Mika Pong", "Mika-Pong"), ("Why", "Waarom")],
                  [[(en, nl), str(f), str(p), note] for en, nl, f, p, note in PLAFONDS]), wide=True))
    return "".join(out)


def _scherm(r, frame, sheet, sprites, schaal=3):
    from PIL import Image
    img = Image.new("RGBA", (160, 120), (0, 0, 0, 255))
    for cmd in frame:
        if cmd[0] == "r":
            _, x, y, b, h, kleur = cmd
            x0, y0, x1, y1 = max(0, x), max(0, y), min(160, x + b), min(120, y + h)
            if x1 > x0 and y1 > y0:
                img.paste(tuple(int(kleur[i:i + 2], 16) for i in (0, 2, 4)) + (255,), (x0, y0, x1, y1))
        else:
            _, naam, x, y = cmd
            u, v, b, h = sprites[naam]
            _deel(img, sheet.crop((u, v, u + b, v + h)), x, y)
    # a dark bezel around it, like the cabinet's
    groot = img.resize((160 * schaal, 120 * schaal), Image.NEAREST)
    lijst = Image.new("RGBA", (groot.width + 16, groot.height + 16), (26, 12, 24, 255))
    lijst.paste((90, 46, 68, 255), (4, 4, lijst.width - 4, lijst.height - 4))
    lijst.paste(groot, (8, 8))
    return lijst


def _deel(img, sprite, x, y):
    """Pastes a sprite; one that sticks out on the left or the top: only the part on the screen."""
    links, boven = max(0, -x), max(0, -y)
    if links < sprite.width and boven < sprite.height:
        img.alpha_composite(sprite.crop((links, boven, sprite.width, sprite.height)), (max(0, x), max(0, y)))


def renders(r):
    from PIL import Image
    import sys
    sys.path.insert(0, "tools")
    from features import guhpixel_guhkade_tex as tex
    out = r.OUT
    try:
        beide = []
        for i, spel in enumerate(("flappy", "pong")):
            q = r.model_quads(f"guhs:block/guhkade_kast_{spel}_onder") + r.offset_quads(r.model_quads(f"guhs:block/guhkade_kast_{spel}_boven"), dy=1.0)
            img = r.render(q, 30, -18, 320)
            img.save(os.path.join(out, f"guhkade_kast_{spel}.png"))
            beide += r.offset_quads(q, dx=i * 1.5)
            r.item_icon(f"guhs:item/guhkade_kast_{spel}").save(os.path.join(out, f"icon_guhkade_kast_{spel}.png"))
        r.render(beide, 30, -18, 640).save(os.path.join(out, "guhkade_kasten.png"))
        print("rendered guhkade_kasten, guhkade_kast_flappy, guhkade_kast_pong")
    except Exception as e:  # noqa: BLE001
        print("no render for the guhkade cabinets", e)
    try:
        sheet = Image.open(os.path.join("src", "main", "resources", "assets", "guhs", "textures", "guhkade", "sprites.png")).convert("RGBA")
        frames = json.load(open(SCHERMEN, encoding="utf-8"))
        for naam, frame in frames.items():
            _scherm(r, frame, sheet, tex.SPRITES).save(os.path.join(out, f"guhkade_{naam}.png"))
        print("rendered the guhkade screens:", ", ".join(frames))
    except Exception as e:  # noqa: BLE001
        print("no pictures of the guhkade screens", e)
