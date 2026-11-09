"""
1.10 round U, slice U-LEVEN-STAD: life, coziness and guh-ness in the towns of zones 1-6 (Java: feature/vadsrijk/stadsleven).

  - texts: speech bubbles (cosy chit-chat, njeg, ~10 lines of gossip per zone, children, cart, musician, picnic, walkers, cats),
    entity names, town names, the Instellingen option "Stadsleven";
  - the light-blue guh-blob texture (the zone-1 klodder's happy face texture, recoloured light blue);
  - the moving parts of the playground (seesaw plank, swing, merry-go-round) and the picnic blanket: GeckoLib models + textures.
  - the playgrounds IN THE MAP are not made here: tools/vadsrijk/stadsleven/speeltuin.py (runs inside maak_kaart).

Keep the pool sizes in sync with Babbels.java (NJEG, KLETS, ROD, KIND, IJS, SOEP, MUZIEK, PICKNICK, WANDEL, KAT, SPEEL).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_leven_stad.py
"""
import colorsys
import json
import os
import random

from PIL import Image

POOLS = {   # pool -> [(nl, en), ...]
    "njeg": [("Njeg!", "Njeg!"), ("Njeg njeg!", "Njeg njeg!"), ("Vahoeg!", "Vahoeg!"), ("Hihi, njeg!", "Hehe, njeg!"),
             ("Mmm, njeg.", "Mmm, njeg."), ("Njeeeg!", "Njeeeg!")],
    "klets": [
        ("Wat een heerlijk zonnetje vandaag, hè?", "Lovely bit of sunshine today, isn't it?"),
        ("Heb jij die nieuwe knuffelbakker al geprobeerd?", "Have you tried the new cuddle bakery yet?"),
        ("Mijn oma zegt: een knuffel per dag houdt de grauwheid weg.", "My granny says: a cuddle a day keeps the greyness away."),
        ("Nog een kopje warme cacao?", "Another cup of hot cocoa?"),
        ("Ik heb vannacht gedroomd dat ik een wolkje was.", "Last night I dreamt I was a little cloud."),
        ("Zullen we straks een rondje wandelen?", "Shall we go for a little walk later?"),
        ("Mijn muisje heeft vandaag drie keer gepiept. Een record!", "My mouse squeaked three times today. A record!"),
        ("Die ridders van de Knuffelorde zijn toch zo dapper.", "Those knights of the Cuddle Order are so brave."),
        ("Pas op, je hebt slagroom op je oortje!", "Careful, you've got cream on your ear!"),
        ("Ik ben gewoon hier om te zitten. En te knabbelen.", "I'm just here to sit. And to nibble."),
        ("Zachte dag, zachte guh, zo hoort het.", "Soft day, soft guh, that's how it should be."),
        ("Hoor je de kinderen in de speeltuin? Wat een plezier!", "Hear the children at the playground? Such fun!"),
        ("Volgens mij gaat het straks roze regenen.", "I think it's going to rain pink later."),
        ("Iedereen is welkom aan ons tafeltje!", "Everyone is welcome at our little table!"),
    ],
    "roddel.z1": [
        ("Hebben jullie het gehoord? Er woont een grote draak achter de bergen.", "Have you heard? A big dragon lives behind the mountains."),
        ("Die draak is niet boos, zegt Grootmeester Vadsvoet. Hij is verdrietig.", "That dragon isn't angry, says Grandmaster Vadsvoet. He's sad."),
        ("Koning Klodder heeft de hele Klodderweide weer plakkerig gemaakt.", "King Klodder made the whole Klodderweide sticky again."),
        ("Ik zag laatst een Mika door Zwamzoet sluipen. Met een nepsnor!", "I saw a Mika sneak through Zwamzoet. With a fake moustache!"),
        ("In de Zwamtempel hoor je 's nachts de Opperzwam snurken.", "At night you can hear the Opperzwam snore in the Mushroom Temple."),
        ("Er ligt een drakenfossiel in de bergen. Zou dat familie van de draak zijn?", "There's a dragon fossil in the mountains. Family of the dragon?"),
        ("Mijn neefje wil later ook ridder worden bij de Knuffelorde.", "My nephew wants to join the Cuddle Order when he grows up."),
        ("Ze zeggen dat de Keizer lange armen heeft. Gelukkig zijn de mijne kort.", "They say the Emperor has long arms. Luckily mine are short."),
        ("Het luchtschip naar de Splinterpieken zit weer helemaal vol.", "The airship to the Splinter Peaks is fully booked again."),
        ("Als je je knabbel niet deelt, komt de Keizer je halen. Zegt mijn oma.", "If you don't share your nibble, the Emperor comes for you. Says my granny."),
    ],
    "roddel.z2": [
        ("Brrr! Maar met warme soep is de kou best gezellig.", "Brrr! But with warm soup the cold is quite cosy."),
        ("Ordemeester Kristalijn zegt dat de kristallen weer zachtjes zingen.", "Order Master Kristalijn says the crystals are softly singing again."),
        ("Vrouwe Spiegelzucht kijkt de hele dag naar zichzelf in de Spiegelburcht.", "Lady Spiegelzucht looks at herself all day in the Mirror Castle."),
        ("In de mijnen boort een Mika gaten in onze mooie kristallen!", "A Mika is drilling holes in our lovely crystals in the mines!"),
        ("Die grauwe splinter in het kristal is geen gewone splinter, hoor.", "That grey splinter in the crystal is no ordinary splinter."),
        ("De Galmwolf huilt zo hard dat de sneeuw van de daken valt.", "The Echo Wolf howls so loudly the snow falls off the roofs."),
        ("Wist je dat de draak vroeger boven de pieken vloog?", "Did you know the dragon used to fly over the peaks?"),
        ("In de roze geode is het altijd een beetje feest.", "In the pink geode it's always a little party."),
        ("Mijn sjaal is gebreid door mijn opa. Hij is honderdtwee!", "My scarf was knitted by my grandpa. He's a hundred and two!"),
        ("Ze zeggen dat de Keizer zelfs hier zijn Mika's heeft rondgestuurd.", "They say the Emperor sent his Mikas even here."),
    ],
    "roddel.z3": [
        ("Graaf Piepvel houdt muisjes in kooitjes. Dat is toch niet lief?", "Count Piepvel keeps mice in little cages. That's not nice, is it?"),
        ("Ik hoorde de Kruinroofvogel weer krijsen bij de kruin.", "I heard the Crown Raptor screeching up at the crown again."),
        ("Ordemeester Wasja zegt dat de draak ooit in het oude nest woonde.", "Order Master Wasja says the dragon once lived in the old nest."),
        ("Zullen we een Pluimguh nemen en de wenteltrap op rennen?", "Shall we take a Plumeguh and race up the spiral stairs?"),
        ("De bijtjes maken dit jaar extra zoete honing.", "The bees are making extra sweet honey this year."),
        ("Baron Bleu ruikt naar oude kaas. Echt waar!", "Baron Bleu smells of old cheese. Really!"),
        ("Als je goed luistert, hoor je de boom kraken. Hij groeit nog!", "If you listen closely you can hear the tree creak. It's still growing!"),
        ("Er zit een Mika in de wortels die Roquefort heet. Brrr.", "There's a Mika in the roots called Roquefort. Brrr."),
        ("Ze zeggen dat de draak weer roze wordt als iemand hem opvrolijkt.", "They say the dragon turns pink again if someone cheers him up."),
        ("Van hier zie je in de verte een zwevende stad. Hoogvads!", "From here you can see a floating city far away. Hoogvads!"),
    ],
    "roddel.z4": [
        ("Weer een lampion gestolen! Die Vliegervanger moet echt eens ophouden.", "Another lantern stolen! That Kite Catcher really must stop."),
        ("Bij de Knuffeltij spoelen de liefste schelpjes aan.", "The sweetest little shells wash up at the Knuffeltij."),
        ("Daar boven drijft Hoogvads. Op de rug van een walvis, stel je voor!", "Hoogvads floats up there. On a whale's back, imagine that!"),
        ("Gisteren kwamen er weer magere guhs aan met de boot. Uit het oosten.", "More thin guhs arrived by boat yesterday. From the east."),
        ("Die vluchtelingen vertellen over grauwe wegen en Mika-karavanen. Eng, hoor.", "Those refugees tell of grey roads and Mika caravans. Scary."),
        ("Mijn vlieger ging zo hoog dat hij bijna Hoogvads raakte!", "My kite went so high it almost touched Hoogvads!"),
        ("Vanavond gaan alle lampionnen weer aan. Mooi, hè?", "Tonight all the lanterns go on again. Pretty, isn't it?"),
        ("Ouwe Dobber dobbert al honderd jaar rond het Schildpadeiland, zeggen ze.", "Old Dobber has bobbed round Turtle Island for a hundred years, they say."),
        ("Bij laag tij kun je bij de Knuffeltij naar schatkistjes zoeken.", "At low tide you can look for little treasure chests at the Knuffeltij."),
        ("De vuurtoren knipoogt naar de luchtschepen. Dat is beleefd.", "The lighthouse winks at the airships. How polite."),
    ],
    "roddel.z5": [
        ("Moeder Wolkwal zwemt vandaag wel heel rustig, vind je niet?", "Mother Cloudwhale is swimming very calmly today, don't you think?"),
        ("Ze zeggen dat Moeder Wolkwal grauwe dromen heeft. Arme walvis.", "They say Mother Cloudwhale has grey dreams. Poor whale."),
        ("In de Raadszaal ligt een boek met een gescheurde bladzijde. Wat stond daar?", "In the Council Hall lies a book with a torn page. What did it say?"),
        ("De Hoge Raad praat over de Keizer, maar alleen als niemand luistert.", "The High Council talks about the Emperor, but only when nobody listens."),
        ("Op de universiteit leren ze hoe je vads kunt meten. Ingewikkeld, hoor.", "At the university they learn how to measure vads. Complicated."),
        ("De Uitgevadsten worden elke dag een beetje voller en vrolijker. Fijn!", "The Uitgevadsten get a little rounder and happier every day. Lovely!"),
        ("De kadetten van de Wolkwacht oefenen weer op het plein. Hup, hup!", "The Cloud Guard cadets are drilling on the square again. Hup, hup!"),
        ("Als Moeder Wolkwal zingt, trillen alle kopjes op het terras.", "When Mother Cloudwhale sings, all the cups on the terrace tremble."),
        ("Hier boven de wolken krijg je tenminste nooit natte voeten.", "Up here above the clouds you never get wet feet, at least."),
        ("Er wordt gefluisterd dat Moeder Wolkwal ooit naar het vasteland zwemt.", "It's whispered that Mother Cloudwhale will swim to the mainland one day."),
    ],
    "roddel.z6": [
        ("De karavaan vertrekt pas als de sterren goed staan.", "The caravan only leaves when the stars are right."),
        ("Er gaan weer sterrenbakens uit. Wie doet nou zoiets gemeens?", "More star beacons have gone out. Who would do something so mean?"),
        ("Hertogin Ringeling draagt ringen om al haar tien tenen. Tien!", "Duchess Ringeling wears rings on all ten of her toes. Ten!"),
        ("In de bazaar zag ik een vliegend tapijt dat niet wilde landen.", "In the bazaar I saw a flying carpet that wouldn't land."),
        ("Mijn bultguh is koppig, maar hij loopt dagenlang zonder water.", "My humpguh is stubborn, but he walks for days without water."),
        ("Ergens achter de duinen ligt de verborgen Moederoase.", "Somewhere beyond the dunes lies the hidden Mother Oasis."),
        ("In de spiegelzaal van het paleis zie je jezelf wel honderd keer.", "In the palace's hall of mirrors you see yourself a hundred times."),
        ("Die grauwe toren bij de oase... daar ga ik echt niet naartoe.", "That grey tower by the oasis... I'm really not going there."),
        ("In Rozenkloof is alles roze. Zelfs de zandkorrels!", "In Rozenkloof everything is pink. Even the grains of sand!"),
        ("Zonder sterren raakt de karavaan de weg kwijt. Gelukkig ben jij er!", "Without stars the caravan gets lost. Good thing you're here!"),
    ],
    "kind": [
        ("Tikkie, jij bent 'm!", "Tag, you're it!"), ("Nog een keer, nog een keer!", "Again, again!"), ("Kom je meespelen?", "Want to play?"),
        ("Ik ben de snelste guh van de hele stad!", "I'm the fastest guh in the whole town!"), ("Hihi, je kan me toch niet pakken!", "Hehe, you can't catch me!"),
        ("Mama, kijk! Zonder handen!", "Mum, look! No hands!"), ("Wiiii!", "Wheee!"), ("Later word ik ook ridder!", "When I grow up I'll be a knight too!"),
    ],
    "speel": [
        ("Hogerrr!", "Higherrr!"), ("Wiii, de glijbaan!", "Wheee, the slide!"), ("Op en neer, op en neer!", "Up and down, up and down!"),
        ("Draai, draai, draai!", "Spin, spin, spin!"), ("Ik bouw een zandkasteel voor de draak!", "I'm building a sandcastle for the dragon!"),
        ("Mama, kijk dan!", "Mum, look!"),
    ],
    "kar.ijs": [("IJsjes! Lekkere ijsjes!", "Ice cream! Lovely ice cream!"), ("Aardbei, vanille of knuffelsmaak?", "Strawberry, vanilla or cuddle flavour?"),
                ("Tring tring, de ijskar is er!", "Ring ring, the ice-cream cart is here!")],
    "kar.soep": [("Warme soep! Lekkere warme soep!", "Warm soup! Lovely warm soup!"), ("Een kommetje soep tegen de kou?", "A bowl of soup against the cold?"),
                 ("Tring tring, de soepkar is er!", "Ring ring, the soup cart is here!")],
    "muziek": [("♪ La-la-guh, la-la-vads ♪", "♪ La-la-guh, la-la-vads ♪"), ("Een verzoeknummer? ♪", "A request? ♪"),
               ("♪ Njeg njeg, wat een mooie dag ♪", "♪ Njeg njeg, what a lovely day ♪"),
               ("Dit liedje heb ik voor de draak geschreven.", "I wrote this song for the dragon.")],
    "picknick": [("Wil je een boterhammetje met hagelslag?", "Fancy a sandwich with sprinkles?"),
                 ("Pas op, er kruipt een mier naar de taart!", "Careful, an ant is crawling towards the cake!"),
                 ("Picknicken is het allerleukst!", "Picnics are the best!"), ("Ik heb aardbeien meegenomen!", "I brought strawberries!"),
                 ("Nog een stukje knabbeltaart?", "Another slice of nibble cake?")],
    "wandel": [("Dit is Piepje. Zeg maar hoi!", "This is Squeaky. Say hi!"), ("We maken ons dagelijkse rondje.", "We're doing our daily stroll."),
               ("Niet aan de lantaarnpaal snuffelen, Piepje!", "Don't sniff the lamp post, Squeaky!"),
               ("Hij is klein, maar heel dapper.", "He's small, but very brave."),
               ("Wil je hem even aaien? Hij vindt je lief!", "Want to pet him? He likes you!")],
    "kat": [("Miauw! (De kat kijkt je tevreden aan.)", "Meow! (The cat looks at you, content.)"),
            ("Prrrr... (De kat knippert langzaam naar je.)", "Prrrr... (The cat blinks slowly at you.)"),
            ("(De kat rekt zich uit en gaat er lekker bij zitten.)", "(The cat stretches and settles down comfortably.)")],
}

STEDEN = {"knuffelkamp": ("Knuffelkamp", "Knuffelkamp"), "zwamzoet": ("Zwamzoet", "Zwamzoet"), "haven": ("De Haven", "The Harbour"),
          "kandijkom": ("Kandijkom", "Kandijkom"), "honingtak": ("Honingtak", "Honingtak"), "lampionhaven": ("Lampionhaven", "Lampionhaven"),
          "knuffeltij": ("De Knuffeltij", "The Knuffeltij"), "strandfeest": ("Het Strandfeest", "The Beach Party"),
          "bloemeneiland": ("Het Bloemeneiland", "Flower Island"), "vliegereiland": ("Het Vliegereiland", "Kite Island"),
          "vuurtoreneiland": ("Het Vuurtoreneiland", "Lighthouse Island"), "schildpadeiland": ("Het Schildpadeiland", "Turtle Island"),
          "hoogvads": ("Hoogvads", "Hoogvads"), "rozenkloof": ("Rozenkloof", "Rozenkloof"), "saffraanpoort": ("Saffraanpoort", "Saffraanpoort")}


def teksten():
    t = {}
    for pool, regels in POOLS.items():
        for i, (nl, en) in enumerate(regels):
            t[f"vadsrijk.stadsleven.{pool}.{i}"] = (nl, en)
    for k, (nl, en) in STEDEN.items():
        t[f"vadsrijk.stadsleven.stad.{k}"] = (nl, en)
    t.update({
        "vadsrijk.stadsleven.zegt": ("%s: \"%s\"", "%s: \"%s\""),
        "vadsrijk.stadsleven.naam.guh": ("Stadsguh", "Town guh"),
        "vadsrijk.stadsleven.naam.kind": ("Guhtje", "Little guh"),
        "vadsrijk.stadsleven.tikkie": ("Tikkie! Jij bent 'm!", "Tag! You're it!"),
        "vadsrijk.stadsleven.wiii": ("Wiiiii!", "Wheeee!"),
        "entity.guhs.vr_sl_prop": ("Stadsdiertje", "Town critter"),
        "entity.guhs.vr_sl_guh": ("Stadsguh", "Town guh"),
        "entity.guhs.vr_sl_kat": ("Poes van een guh", "A guh's cat"),
        "vadsrijk.menu.instellingen.stadsleven": ("Stadsleven", "Town life"),
        "vadsrijk.menu.instellingen.stadsleven.uitleg": (
            "Muisjes, blauwe klodders, katten, eendjes, wandelende en kletsende guhs, de ijskar en de speeltuin in de stadjes. "
            "Zacht = de helft en alleen dichtbij, Uit = niets (beter voor je FPS).",
            "Mice, blue blobs, cats, ducklings, strolling and chatting guhs, the ice-cream cart and the playground in the towns. "
            "Soft = half and only close by, Off = nothing (better for your FPS)."),
    })
    return t


# =====================================================================================================================
# textures + models
# =====================================================================================================================
KLEUREN = [(232, 160, 184), (192, 96, 128), (244, 240, 240), (154, 160, 168), (255, 192, 216), (159, 224, 200), (255, 224, 128),
           (200, 168, 232)]   # 0 cherry pink, 1 dark pink, 2 white, 3 chain grey, 4 light pink, 5 mint, 6 yellow, 7 lilac


def blauwe_klodder(bron):
    """The happy zone-1 klodder texture, its green body turned light blue (face, cheeks and eyes stay)."""
    img = Image.open(bron).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if 0.18 < h < 0.5 and s > 0.12:
                h2 = 0.55 + (h - 0.33) * 0.3
                s2 = min(1.0, s * 0.85)
                v2 = min(1.0, v * 1.12 + 0.07)
                r2, g2, b2 = colorsys.hsv_to_rgb(h2, s2, v2)
                px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return img


def stalen():
    """32x32: 16 swatches of 8x8 (KLEUREN, with a little noise)."""
    rnd = random.Random(1107)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for i in range(16):
        c = KLEUREN[i % len(KLEUREN)]
        sx, sy = (i % 4) * 8, (i // 4) * 8
        for y in range(8):
            for x in range(8):
                n = rnd.randint(-8, 8) + (6 if (x + y) % 5 == 0 and i == 0 else 0)
                px[sx + x, sy + y] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n)), max(0, min(255, c[2] + n)), 255)
    return img


def kleed_textuur():
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        for x in range(32):
            roze = ((x // 4) + (y // 4)) % 2 == 0
            rand = x in (0, 31) or y in (0, 31)
            c = (255, 140, 190) if rand else (255, 182, 214) if roze else (255, 250, 250)
            px[x, y] = c + (255,)
    return img


def _vlak(i):
    return {"uv": [(i % 4) * 8, (i // 4) * 8], "uv_size": [8, 8]}


def kubus(origin, size, kleur):
    return {"origin": origin, "size": size, "uv": {k: _vlak(kleur) for k in ("north", "south", "east", "west", "up", "down")}}


def geo(naam, bones, breed=4, hoog=4, tex=32):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{naam}", "texture_width": tex, "texture_height": tex, "visible_bounds_width": breed,
                        "visible_bounds_height": hoog, "visible_bounds_offset": [0, hoog / 4, 0]},
        "bones": bones}]}


def modellen():
    wip = geo("sl_wipwap", [{"name": "root", "pivot": [0, 0, 0], "cubes": [kubus([-1, -1.5, -1], [2, 2, 2], 3)]},
                            {"name": "plank", "parent": "root", "pivot": [0, 0, 0], "cubes": [
                                kubus([-3, 0, -28], [6, 2, 56], 0),
                                kubus([-3, 2, -28], [6, 1, 7], 4), kubus([-3, 2, 21], [6, 1, 7], 4),
                                kubus([-0.5, 2, -20], [1, 5, 1], 1), kubus([-2.5, 7, -20], [5, 1, 1], 1),
                                kubus([-0.5, 2, 19], [1, 5, 1], 1), kubus([-2.5, 7, 19], [5, 1, 1], 1)]}], breed=5, hoog=2)
    schommel = geo("sl_schommel", [{"name": "root", "pivot": [0, 0, 0], "cubes": [
        kubus([-5, -41, -0.3], [0.6, 41, 0.6], 3), kubus([4.4, -41, -0.3], [0.6, 41, 0.6], 3),
        kubus([-6, -43, -3], [12, 2, 6], 4), kubus([-6.5, -1, -1], [13, 1, 2], 3)]}], breed=3, hoog=6)
    hand = []
    for (x, z) in ((0, 18), (0, -18), (18, 0), (-18, 0)):
        hand += [kubus([x - 0.5, 3, z - 0.5], [1, 8, 1], 1),
                 kubus([x - (3 if z else 0.5), 11, z - (0.5 if z else 3)], [6 if z else 1, 1, 1 if z else 6], 1)]
    molen = geo("sl_draaimolen", [{"name": "root", "pivot": [0, 0, 0], "cubes": [
        kubus([-22, 0, -22], [44, 3, 44], 0), kubus([-16, 3, -16], [32, 0.3, 32], 4), kubus([-1, 3, -1], [2, 16, 2], 2),
        kubus([-6, 19, -6], [12, 2, 12], 1), kubus([-3, 21, -3], [6, 1.5, 6], 6)] + hand}], breed=4, hoog=3)
    kleed = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.sl_kleed", "texture_width": 32, "texture_height": 32, "visible_bounds_width": 4,
                        "visible_bounds_height": 1, "visible_bounds_offset": [0, 0.25, 0]},
        "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": [
            {"origin": [-20, 0, -20], "size": [40, 0.3, 40], "uv": {
                "up": {"uv": [0, 0], "uv_size": [32, 32]}, "down": {"uv": [0, 0], "uv_size": [32, 32]},
                **{k: {"uv": [0, 0], "uv_size": [32, 1]} for k in ("north", "south", "east", "west")}}},
            kubus([8, 0.3, -12], [6, 4, 5], 6), kubus([8, 4.3, -10], [6, 1, 1], 1)]}]}]}   # + a little picnic basket
    return {"sl_wipwap": wip, "sl_schommel": schommel, "sl_draaimolen": molen, "sl_kleed": kleed}


LEEG = {"format_version": "1.8.0", "animations": {"idle": {"loop": True, "animation_length": 1.0, "bones": {}}}}


def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def assets(a, w=_schrijf_json):
    tex = os.path.join(a, "textures", "vadsrijk", "stadsleven")
    os.makedirs(tex, exist_ok=True)
    # W-MODELLEN: the town blobs wear the Blub-guh model now (stadsleven/BlubLook.java): blauwe_klodder.png is no longer made
    stalen().save(os.path.join(tex, "sl_speeltoestel.png"))
    kleed_textuur().save(os.path.join(tex, "sl_kleed.png"))
    for naam, m in modellen().items():
        w(os.path.join(a, "geckolib", "models", "entity", "vadsrijk", "stadsleven", f"{naam}.geo.json"), m)
        w(os.path.join(a, "geckolib", "animations", "entity", "vadsrijk", "stadsleven", f"{naam}.animation.json"), LEEG)


def build(h):
    assets(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    assets(a)
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf_json(p, data)
    print(f"vadsrijk_u_leven_stad: {len(t)} texts, blue blob texture, {len(modellen())} playground/picnic models")


if __name__ == "__main__":
    _direct()
