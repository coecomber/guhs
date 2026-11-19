"""
Guhwinter 1.10.10 "Guhwinter", slice npc (Java: feature/guhwinter/npc; CONTRACT.md "## npc").

  BONES     four new guh model bones for the event's own look pieces: Sinterklaas' golden staff, a pakjesguh's tiny soot
            smudge, Anthony's camera, Liek's golden nose ring (hidden on every other guh). tools/make_guh_variants.py can no
            longer run (no free swatch space), so build() appends them to guh.geo.json itself (_bones_erin), each sampling the
            UV spot of an existing swatch bone (crown, eyepatch, helmet, monocle).
  looks     the look textures guh_clothes/guhwinter_<look>.png (NO wardrobe clothes: no items, no source; drawn only on the
            Winterplein's guhs by client/GuhwinterNpcClient, whose BOTTEN must name the bones listed in LOOKS here), painted on
            the UV spots read from guh.geo.json (like vadsrijk_economie's clothes)
  build     the above, the names and idle lines (lang guhwinter.npc.*), their English overlay tools/lang/en/c509_guhwinter_npc.json,
            and checks (3-6 lines per guh and phase, no Mika's, every bone a look shows is painted)

Idle lines: 3 to 6 per guh per phase (sint / kerst / oudjaar); Java counts them (NpcPraat.aantal).
"""
import json
import os
import re
import zlib

import numpy as np

P = "guhwinter.npc."
NL = chr(10)
CHUNK = "c509_guhwinter_npc"


def _rng(naam):
    return np.random.default_rng(zlib.crc32(("guhwinter_npc/" + naam).encode("utf-8")))


# =====================================================================================================================
# bones (make_guh_variants format: name: (parent, pivot, swatch, [(origin, size, inflate), ...]))
# (the guh texture has no free swatch space left: the bones sample swatches that already exist (crown, eyepatch, helmet,
#  monocle); only our own look textures paint those spots, so what the other clothes paint there does not matter)
# =====================================================================================================================
_B = [0, 6, 6]
_H = [0, 6, -2]
BONES = {
    # the Sint's golden crozier, standing beside his right front paw, the crook bending forward over his head
    "outfit_guhwinter_staf": ("body", _B, "crown", [
        ([-9.4, 0.0, -4.4], [0.8, 21.0, 0.8], 0), ([-9.4, 21.0, -7.2], [0.8, 0.8, 3.6], 0),
        ([-9.4, 19.4, -7.2], [0.8, 1.6, 0.8], 0), ([-9.7, 13.0, -4.7], [1.4, 0.7, 1.4], 0)]),
    # a pakjesguh's one tiny soot smudge on the cheek
    "outfit_guhwinter_roet": ("head", _H, "eyepatch", [([2.6, 5.4, -12.75], [2.2, 1.0, 0.3], 0)]),
    # Anthony's camera on his chest, with its lens and the strap round his neck
    "outfit_guhwinter_camera": ("head", _H, "helmet", [
        ([-2.2, 0.6, -12.6], [4.4, 2.6, 1.4], 0), ([-0.9, 1.1, -13.4], [1.8, 1.6, 0.8], 0),
        ([-4.6, 3.0, -11.4], [9.2, 0.5, 0.5], 0)]),
    # Liek's golden nose ring (like her statue in Het Knabbelpark)
    "outfit_guhwinter_neusring": ("head", _H, "monocle", [([0.4, 4.4, -13.25], [1.0, 1.0, 0.35], 0)]),
}

# look -> the bone prefixes its pass shows (client/GuhwinterNpcClient.BOTTEN: keep the same)
PAKJE_BOTTEN = ["outfit_suit", "outfit_beret", "outfit_scarf", "outfit_guhwinter_roet"]
LOOKS = {
    "sint": ["outfit_ring_baard", "outfit_long_cape", "outfit_guhwinter_staf"],
    "anthony": ["outfit_guhwinter_camera"],
    "liek": ["outfit_guhwinter_neusring"],
    **{f"pakje_{i}": PAKJE_BOTTEN for i in range(4)},
}

# the four pietenpakjes: (left half, right half, beret, feather), cheerful colours
PAKJES = [
    ((214, 52, 70), (246, 196, 60), (60, 70, 170), (250, 250, 250)),     # rood/geel, blauwe baret
    ((70, 120, 220), (250, 140, 50), (200, 40, 70), (250, 230, 90)),      # blauw/oranje, rode baret
    ((60, 170, 110), (150, 80, 200), (240, 120, 180), (250, 250, 250)),   # groen/paars, roze baret
    ((250, 120, 170), (60, 190, 200), (250, 200, 60), (120, 60, 200)),    # roze/turquoise, gele baret
]
GOUD, GOUD_DONKER, GOUD_LICHT = (236, 190, 64), (176, 128, 36), (255, 228, 130)


def _stof(c, rng, ruis=8):
    """A fleece-ish 32x32 swatch: the colour with soft speckles (like make_guh_variants.fabric)."""
    a = np.array(c, np.float32) + rng.normal(0, ruis / 2, (32, 32, 1)) + rng.normal(0, ruis / 4, (32, 32, 3))
    return np.clip(a, 0, 255)


def look_painters():
    """look -> {bone name: painter () -> 32x32x3}: painted on that bone's own UV spot of guhwinter_<look>.png."""
    S = 32

    def goud(naam, ring=False):
        def paint():
            a = _stof(GOUD, _rng(naam), 10)
            a[:, :3] = GOUD_LICHT
            a[:, -3:] = GOUD_DONKER
            if ring:
                a[12:20, 12:20] = GOUD_DONKER
            return a
        return paint

    def baard():
        a = _stof((246, 246, 248), _rng("baard"), 8)
        for x in range(0, S, 4):
            a[:, x] = a[:, x] * 0.9          # soft curls
        return a

    def mantel():
        a = _stof((176, 22, 40), _rng("mantel"), 10)   # deep red velvet with a golden hem
        a[:3, :] = GOUD
        a[-3:, :] = GOUD
        a[:, :3] = GOUD
        a[:, -3:] = GOUD
        for y in range(8, S - 6, 8):
            a[y:y + 2, 13:19] = GOUD_LICHT   # little gold crosses down the middle
            a[y - 2:y + 4, 15:17] = GOUD_LICHT
        return a

    def kraag_goud():
        a = _stof(GOUD, _rng("kraag"), 10)
        a[::4, :] = GOUD_LICHT
        return a

    def camera():
        a = _stof((34, 34, 40), _rng("camera"), 6)
        a[:2, :] = (120, 120, 130)
        a[-2:, :] = (120, 120, 130)
        yy, xx = np.mgrid[0:S, 0:S]
        d = np.sqrt((yy - 15.5) ** 2 + (xx - 15.5) ** 2)
        a[(d < 9) & (d >= 6)] = (170, 172, 180)   # the silver lens ring
        a[d < 4] = (90, 140, 200)                 # glass
        a[d < 2] = (200, 230, 250)                # a twinkle
        return a

    out = {
        "sint": {"outfit_ring_baard": baard, "outfit_long_cape": mantel, "outfit_long_cape_collar": kraag_goud,
                 "outfit_guhwinter_staf": goud("staf")},
        "liek": {"outfit_guhwinter_neusring": goud("neusring", ring=True)},
        "anthony": {"outfit_guhwinter_camera": camera},
    }
    for i, (links, rechts, baret, veer) in enumerate(PAKJES):
        def pak(links=links, rechts=rechts, i=i):
            a = _stof(links, _rng(f"pak{i}"), 10)
            a[:, S // 2:] = _stof(rechts, _rng(f"pak{i}b"), 10)[:, S // 2:]
            a[:, S // 2 - 1:S // 2 + 1] = GOUD     # a golden seam with buttons
            for y in range(4, S, 8):
                a[y:y + 2, S // 2 - 2:S // 2 + 2] = GOUD_LICHT
            return a

        def muts(baret=baret, i=i):
            return _stof(baret, _rng(f"baret{i}"), 8)

        def pluim(veer=veer, i=i):
            return _stof(veer, _rng(f"veer{i}"), 6)

        def kraag(i=i):
            a = _stof((250, 250, 252), _rng(f"kraag{i}"), 5)
            yy, xx = np.mgrid[0:S, 0:S]
            a[((xx + yy) % 6 == 0)] = (226, 226, 232)   # lace
            return a

        def roet(i=i):
            return _stof((70, 64, 66), _rng(f"roet{i}"), 14)

        out[f"pakje_{i}"] = {"outfit_suit": pak, "outfit_suit_tail": pak, "outfit_beret": muts, "outfit_beret_feather": pluim,
                             "outfit_scarf": kraag, "outfit_guhwinter_roet": roet}
    return out


def _geo_pad(h):
    return os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json")


def _bones_erin(h):
    """Our four bones into guh.geo.json (tools/make_guh_variants.py no longer runs: no free swatch space): appended after
    all others, sampling the UV spot of an existing swatch bone (BONES' swatch name = that bone without outfit_). Idempotent."""
    pad = _geo_pad(h)
    raw = open(pad, encoding="utf-8").read()
    geo_file = json.loads(raw)
    geo = geo_file["minecraft:geometry"][0]
    uv = {b["name"]: b["cubes"][0]["uv"]["north"]["uv"] for b in geo["bones"] if b.get("cubes")}
    geo["bones"] = [b for b in geo["bones"] if b["name"] not in BONES]
    for name, (parent, pivot, swatch, cubes) in BONES.items():
        u, v = uv["outfit_" + swatch]
        face = {"uv": [u, v], "uv_size": [8, 8]}
        geo["bones"].append({"name": name, "parent": parent, "pivot": pivot, "cubes": [
            {"origin": origin, "size": size, **({"inflate": inflate} if inflate else {}),
             "uv": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}
            for origin, size, inflate in cubes]})
    out = json.dumps(geo_file, indent=2) + NL
    if out != raw:
        with open(pad, "w", encoding="utf-8", newline=NL) as f:
            f.write(out)


def _texturen(h):
    """guh_clothes/guhwinter_<look>.png: each bone's painter on that bone's UV spot (checked: no two bones of one look share
    a spot with different painters)."""
    from PIL import Image
    geo = json.load(open(_geo_pad(h), encoding="utf-8"))["minecraft:geometry"][0]
    uv = {b["name"]: tuple(b["cubes"][0]["uv"]["north"]["uv"]) for b in geo["bones"] if b.get("cubes")}
    w, hh = geo["description"]["texture_width"] * 4, geo["description"]["texture_height"] * 4
    problemen = []
    for look, painters in look_painters().items():
        a = np.zeros((hh, w, 4), np.uint8)
        bezet = {}
        for bone, paint in painters.items():
            u, v = uv[bone]
            if (u, v) in bezet and bezet[(u, v)] is not paint:
                problemen.append(f"{look}: {bone} and {bezet[(u, v)].__name__} share the UV spot {u},{v}")
            bezet[(u, v)] = paint
            a[v * 4:v * 4 + 32, u * 4:u * 4 + 32, :3] = paint().astype(np.uint8)
            a[v * 4:v * 4 + 32, u * 4:u * 4 + 32, 3] = 255
        pad = os.path.join(h.A, "textures", "entity", "guh_clothes", f"guhwinter_{look}.png")
        Image.fromarray(a).save(pad, optimize=True)
    return problemen


# =====================================================================================================================
# names
# =====================================================================================================================
NAMEN = {
    "joost": ("Joostguh", "Joostguh"),
    "suus": ("Suusguh", "Suusguh"),
    "liek": ("Liekguh", "Liekguh"),
    "lin": ("Linguh", "Linguh"),
    "evi": ("Eviguh", "Eviguh"),
    "niels": ("Nielsguh", "Nielsguh"),
    "krijn": ("Papa Krijnguh", "Papa Krijnguh"),
    "corine": ("Mama Corineguh", "Mama Corineguh"),
    "marrie": ("Oma Marrieguh", "Oma Marrieguh"),
    "wijnand": ("Opa Wijnand", "Opa Wijnand"),
    "rins": ("Rinsguh", "Rinsguh"),
    "anthony": ("Anthonyguh", "Anthonyguh"),
    "daan": ("Daanguh", "Daanguh"),
    "kim": ("Kimguh", "Kimguh"),
    "finn": ("Finnguh", "Finnguh"),
    "liv": ("Livguh", "Livguh"),
    "sint": ("Sinterklaas", "Sinterklaas"),
    "pakjesguh_0": ("Pakjesguh Pepernootje", "Parcel Guh Pepernootje"),
    "pakjesguh_1": ("Pakjesguh Kruimel", "Parcel Guh Kruimel"),
    "pakjesguh_2": ("Pakjesguh Strikje", "Parcel Guh Strikje"),
    "pakjesguh_3": ("Pakjesguh Hupsakee", "Parcel Guh Hupsakee"),
    "ozovads": ("Ozovads", "Ozovads"),
    "winkelguh": ("Winkelguh Toffie", "Shopkeeper Toffie"),
}

# =====================================================================================================================
# idle lines: naam -> fase -> [(Dutch, English), ...]
# =====================================================================================================================
PRAAT = {
    "joost": {
        "sint": [
            ("Njeg! Welkom op het Winterplein. Mama heeft al chocomel klaargezet. Drie pannen. Voor de zekerheid.",
             "Njeg! Welcome to the Winterplein. Mum already made hot chocolate. Three pans. Just in case."),
            ("De Sint is verdwaald? Bij Slot Loevestijn verkeerd afgeslagen. Kan de beste overkomen. Nou ja, de beste Sint.",
             "The Sint got lost? Took a wrong turn at Slot Loevestijn. Happens to the best of us. Well, the best Sint."),
            ("Suus en ik hebben een streak van tien carpaccio's bij La Cubanita. Tien! Die breek je niet zomaar af voor een stoomboot.",
             "Suus and I have a ten-carpaccio streak at La Cubanita. Ten! You don't just break that for a steamboat."),
            ("Heb jij je al ingeschreven in Het Grote Boek? Lootjes trekken is het leukste van het hele jaar. Na carpaccio.",
             "Have you signed up in the Big Book yet? Drawing lots is the best thing of the whole year. After carpaccio."),
            ("Suus vraagt steeds of ik iets kleins en glimmends voor haar heb. Ik heb haar een pepernoot gegeven. Die glimt ook een beetje.",
             "Suus keeps asking if I have something small and shiny for her. I gave her a pepernoot. That's a bit shiny too."),
        ],
        "kerst": [
            ("We hebben de kerstboom gehaald, Niels en ik. Niels wilde met de trein. De boom niet.",
             "Niels and I fetched the Christmas tree. Niels wanted to take the train. The tree didn't."),
            ("Hang jij ook een tekening in de boom? Ik heb Suus getekend. Ze vroeg waarom ik er geen ring bij had getekend. Rare vraag.",
             "Will you hang a drawing in the tree too? I drew Suus. She asked why I didn't draw a ring with it. Strange question."),
            ("Gourmetten! Het mooiste aan kerst is dat je zelf je pannetje mag vullen. Ik doe er carpaccio in. Als eerbetoon.",
             "Gourmet night! The best part of Christmas is filling your own little pan. I put carpaccio in it. As a tribute."),
            ("Opa is er! Opa Jo... Wijnand. Opa Wijnand. Vahoeg, dat ging bijna goed.",
             "Grandpa is here! Grandpa Jo... Wijnand. Grandpa Wijnand. Vahoeg, nearly got it right."),
        ],
        "oudjaar": [
            ("Oliebollen! Ik eet er dit jaar maar twaalf. Goed voornemen. Nou ja, dertien.",
             "Oliebollen! I'm only having twelve this year. Good resolution. Well, thirteen."),
            ("Mijn voornemen: de carpaccio-streak bij La Cubanita naar twintig. Suus zegt dat ze een ander voornemen voor mij heeft. Ze wil niet zeggen welk.",
             "My resolution: get the carpaccio streak at La Cubanita to twenty. Suus says she has a different resolution for me. She won't say which."),
            ("Lin bakt in een airfryer. Ik lachte erom. Toen proefde ik er een. Ik lach niet meer. Ik eet.",
             "Lin bakes hers in an air fryer. I laughed. Then I tasted one. I'm not laughing anymore. I'm eating."),
            ("Alvast een gelukkig nieuwjaar, vadsje! Op een jaar vol knuffels en vahoeg.",
             "Happy new year in advance, vadsje! Here's to a year full of cuddles and vahoeg."),
        ],
    },
    "suus": {
        "sint": [
            ("Op mijn verlanglijstje heb ik 'iets kleins' gezet. Met een tekeningetje van een rondje erbij. Gewoon, een rondje.",
             "On my wish list I wrote 'something small'. With a little drawing of a circle. Just a circle."),
            ("Zou de Sint ook dingen in een klein doosje doen? Zo'n fluwelen doosje? Ik vraag het voor een vriendin.",
             "Would the Sint also put things in a small box? A velvet one? Asking for a friend."),
            ("Tien carpaccio's op rij bij La Cubanita. Joost en ik zijn een goed team. Zo'n team dat... je weet wel... voor altijd... njeg.",
             "Ten carpaccios in a row at La Cubanita. Joost and I are a good team. The kind of team that... you know... forever... njeg."),
            ("Mijn schoentje staat al klaar! Met een wortel voor Ozovads. En een briefje voor de Sint. Over glimmende dingetjes.",
             "My shoe is already out! With a carrot for Ozovads. And a note for the Sint. About shiny little things."),
        ],
        "kerst": [
            ("Ik heb een bal in de boom gehangen. Hij lijkt toevallig op een ringdoosje. Toevallig hè, Joost? JOOST?",
             "I hung a bauble in the tree. It happens to look like a ring box. Funny coincidence, right Joost? JOOST?"),
            ("Bij kerst denk ik altijd aan grote vragen. Vragen waarvoor je op één knie gaat zitten. Bijvoorbeeld.",
             "Christmas always makes me think of big questions. Questions you get down on one knee for. For example."),
            ("Gourmetten is net trouwen: je deelt een pannetje en je belooft dat je niet alles opeet. Zei ik trouwen? Ik zei gourmetten.",
             "Gourmet night is just like getting married: you share a little pan and promise not to eat everything. Did I say married? I said gourmet."),
            ("Opa heet Wijnand. Joost noemt hem al de hele dag Jochum. Joost weet ook nooit aan welke vinger... laat maar.",
             "Grandpa's name is Wijnand. Joost has been calling him Jochum all day. Joost never knows which finger either... never mind."),
        ],
        "oudjaar": [
            ("Mijn goede voornemen? Geduld. Heel. Veel. Geduld.",
             "My good resolution? Patience. Lots. Of. Patience."),
            ("Om twaalf uur geef je elkaar een kus. En soms... iets anders. Iets met een doosje. Gewoon een idee, hoor.",
             "At midnight you give each other a kiss. And sometimes... something else. Something with a little box. Just an idea."),
            ("Lins airfryer-oliebollen zijn de lekkerste. Niet tegen Joost zeggen, die denkt dat de zijne het lekkerst zijn.",
             "Lin's air fryer oliebollen are the best. Don't tell Joost, he thinks his are."),
            ("Het nieuwe jaar wordt vast een heel bijzonder jaar. Toch? Toch, Joost? ...Joost eet een oliebol.",
             "The new year is going to be a very special year. Right? Right, Joost? ...Joost is eating an oliebol."),
        ],
    },
    "liek": {
        "sint": [
            ("Welkom! Pas op het ijs, dat is gladder dan het eruitziet. Ik heb het getest. Met mijn achterkant.",
             "Welcome! Mind the ice, it's slipperier than it looks. I tested it. With my bottom."),
            ("Lin en ik hebben het hele plein versierd. Nou ja, Lin versierde. Ik hield de ladder vast en zei 'mooi!'.",
             "Lin and I decorated the whole square. Well, Lin decorated. I held the ladder and said 'pretty!'."),
            ("Mijn neusringetje glimt zo mooi in het lantaarnlicht. Suus vraagt steeds hoe je aan zo'n ringetje komt. Verdacht vaak.",
             "My nose ring sparkles so nicely in the lantern light. Suus keeps asking how you get a ring like that. Suspiciously often."),
            ("Lootjes trekken! Ik hoop dat ik iemand trek die van schattig houdt. Ik kan alleen maar schattig.",
             "Drawing lots! I hope I draw someone who likes cute things. Cute is all I can do."),
        ],
        "kerst": [
            ("De kerstwandeling om de vijver is mijn favoriet. Je weet nooit wat je tegenkomt. Vahoeg, wie weet!",
             "The Christmas walk around the pond is my favourite. You never know what you'll run into. Vahoeg, who knows!"),
            ("Mama Corine heeft acht soorten saus voor het gourmetten. Acht! Ik heb ze allemaal geproefd. Voor de wetenschap.",
             "Mum Corine has eight kinds of sauce for gourmet night. Eight! I tasted them all. For science."),
            ("Lin wil niks gefrituurds op tafel. Niet eens een kroketje. Ik respecteer dat. Ik eet stiekem haar kroketje.",
             "Lin doesn't want anything fried on the table. Not even a croquette. I respect that. I secretly eat her croquette."),
            ("Ik heb de hele familie getekend voor in de boom. Iedereen is een rondje met oortjes. Herkenbaar, toch?",
             "I drew the whole family for the tree. Everyone is a circle with ears. Recognisable, right?"),
        ],
        "oudjaar": [
            ("Mijn voornemen: elke dag een guh knuffelen die er niet op rekent. Jij bent de eerste. Knuffel!",
             "My resolution: hug a guh every day who isn't expecting it. You're the first. Hug!"),
            ("Lin bakt ze in een airfryer en ze zijn ECHT de lekkerste. Ik zeg dat niet omdat ze mijn vriendin is. Nou ja, ook een beetje.",
             "Lin bakes them in an air fryer and they really ARE the best. I'm not saying that because she's my girlfriend. Well, a little."),
            ("Schrijf jij ook een voornemen op het prikbord? Iedereen kan ze lezen. Dus schrijf geen geheimen op. Of juist wel.",
             "Are you writing a resolution on the board too? Everyone can read them. So don't write down secrets. Or do."),
            ("Vuurwerk vind ik eng, oliebollen niet. Daarom sta ik hier. Bij de oliebollen.",
             "Fireworks scare me, oliebollen don't. That's why I'm standing here. By the oliebollen."),
        ],
    },
    "lin": {
        "sint": [
            ("Pepernoten worden gebakken. Niet gefrituurd. Daarom vertrouw ik ze.",
             "Pepernoten are baked. Not fried. That's why I trust them."),
            ("Liek en ik hebben de lampjes opgehangen. Ik ben drie keer van de ladder gevallen. Liek ving me twee keer.",
             "Liek and I put up the lights. I fell off the ladder three times. Liek caught me twice."),
            ("Een stoomboot in de Guhmension. Ik heb dit jaar al raardere dingen gezien. Vooral op de kermis. Daar praten we niet over.",
             "A steamboat in the Guhmension. I've seen stranger things this year. Mostly at the kermis. We don't talk about that."),
            ("Voor mijn lootje bouw ik iets heel schattigs. Met bloemetjes. Geen frituurpan. Nooit een frituurpan.",
             "For my lootje I'm building something really cute. With flowers. No deep fryer. Never a deep fryer."),
        ],
        "kerst": [
            ("Gourmetten is veilig. Het is bakken op een plaatje. Niemand frituurt iets. Toch? TOCH?",
             "Gourmet night is safe. It's cooking on a little hot plate. Nobody deep-fries anything. Right? RIGHT?"),
            ("Waarom ik niks gefrituurds eet? Het churro-incident. Meer zeg ik er niet over. Er waren veel suiker en veel tranen.",
             "Why I don't eat anything fried? The churro incident. That's all I'll say. There was a lot of sugar and a lot of tears."),
            ("Ik heb een rendier getekend voor in de boom. Liek zegt dat het op een guh lijkt. Alles lijkt op een guh, Liek.",
             "I drew a reindeer for the tree. Liek says it looks like a guh. Everything looks like a guh, Liek."),
            ("Ik hoop zo dat we bij de vijver een bijzondere vogel zien. Een ijsvogel! Of... iets nog bijzonders.",
             "I really hope we see a special bird at the pond. A kingfisher! Or... something even more special."),
        ],
        "oudjaar": [
            ("Iedereen frituurt zijn oliebollen. Ik niet. Ik heb mijn airfryer meegenomen. Hij heet Bertus.",
             "Everyone deep-fries their oliebollen. Not me. I brought my air fryer. His name is Bertus."),
            ("Het churro-incident heeft me gevormd. Nu bak ik alles in de airfryer en het is fantastisch. Vahoeg, Bertus!",
             "The churro incident shaped me. Now I bake everything in the air fryer and it's fantastic. Vahoeg, Bertus!"),
            ("Mijn voornemen: nooit meer frituren. Dat had ik vorig jaar ook. Het gaat heel goed.",
             "My resolution: never deep-fry again. I had that one last year too. It's going really well."),
            ("Proef maar! Een airfryer-oliebol. Krokant, luchtig, geen incidenten. Njeg.",
             "Have a taste! An air fryer oliebol. Crispy, fluffy, no incidents. Njeg."),
        ],
    },
    "evi": {
        "sint": [
            ("Bij het guhmeentehuis deel ik paspoorten uit. Ozovads heeft er nog geen. Hoe moet hij dan ooit naar Japan?",
             "At the guhmeentehuis I hand out passports. Ozovads doesn't have one yet. How will he ever get to Japan?"),
            ("In Japan zetten ze geen schoentje, maar Niels en ik doen het toch. Met een onigiri erin. Voor de zekerheid.",
             "In Japan they don't put out a shoe, but Niels and I do it anyway. With an onigiri in it. Just in case."),
            ("Sinterklaas is eigenlijk net een animeheld. Lange mantel, dramatische entree, een trouw dier. Njeg, ik zie het helemaal voor me.",
             "Sinterklaas is basically an anime hero. Long cloak, dramatic entrance, a loyal animal companion. Njeg, I can totally see it."),
            ("Niels zegt dat de Sint met de trein had moeten komen. Dat zegt hij over alles. Ook over mijn fiets.",
             "Niels says the Sint should've come by train. He says that about everything. Even about my bike."),
        ],
        "kerst": [
            ("In Japan eet je met kerst kip en aardbeientaart. Hier gourmetten we. Allebei vahoeg.",
             "In Japan you eat chicken and strawberry cake at Christmas. Here we do gourmet. Both vahoeg."),
            ("Ik heb een kitsune getekend voor in de boom. Met een kerstmuts op. Cultuuruitwisseling!",
             "I drew a kitsune for the tree. Wearing a Santa hat. Cultural exchange!"),
            ("Papa en mama hebben het hele huis vol lichtjes gehangen. Het lijkt Tokio wel. Een heel klein, heel gezellig Tokio.",
             "Dad and Mum covered the whole house in lights. It looks like Tokyo. A very small, very cosy Tokyo."),
            ("Opa heet Wijnand. Joost blijft Jochum zeggen. In Japan buig je als je iemands naam verkeerd zegt. Joost buigt veel vandaag.",
             "Grandpa's name is Wijnand. Joost keeps saying Jochum. In Japan you bow when you get someone's name wrong. Joost is bowing a lot today."),
        ],
        "oudjaar": [
            ("Mijn voornemen: dit jaar drie keer naar Japan. Niels wil met de trein. Ik heb hem de zee laten zien op de kaart.",
             "My resolution: go to Japan three times this year. Niels wants to go by train. I showed him the sea on the map."),
            ("In Japan luiden ze op oudjaar 108 keer een klok. Wij eten 108 oliebollen. Ongeveer hetzelfde.",
             "In Japan they ring a bell 108 times on New Year's Eve. We eat 108 oliebollen. About the same."),
            ("Akemashite omedetou! Dat is Japans voor 'gelukkig nieuwjaar'. En 'vahoeg' is guh voor alles.",
             "Akemashite omedetou! That's Japanese for 'happy new year'. And 'vahoeg' is guh for everything."),
            ("Lins oliebollen uit de airfryer zijn net mochi. Zacht en rond, en je wordt er blij van.",
             "Lin's air fryer oliebollen are just like mochi. Soft and round, and they make you happy."),
        ],
    },
    "niels": {
        "sint": [
            ("Een stoomboot? Had gewoon de trein genomen. Een stoomboot is geen echt OV.",
             "A steamboat? Should've just taken the train. A steamboat isn't real public transport."),
            ("Van Spanje naar hier: Intercity naar Parijs, TGV naar Madrid, klaar. Dan sla je nooit verkeerd af bij Slot Loevestijn.",
             "From Spain to here: Intercity to Paris, TGV to Madrid, done. Then you never take a wrong turn at Slot Loevestijn."),
            ("Ozovads op een boot. Paarden horen in de trein. In de bagagewagon. Met een kaartje.",
             "Ozovads on a boat. Horses belong on trains. In the luggage car. With a ticket."),
            ("Na pakjesavond gaan Evi en ik naar Japan. Met de Shinkansen. Dát is pas OV, njeg.",
             "After pakjesavond Evi and I are off to Japan. On the Shinkansen. Now THAT is public transport, njeg."),
        ],
        "kerst": [
            ("De kerstboom paste niet in de trein. De trein was het probleem niet. De boom was het probleem.",
             "The Christmas tree didn't fit on the train. The train wasn't the problem. The tree was the problem."),
            ("Ik heb een trein getekend voor in de boom. Met een kerstboom erin. Zo had het moeten gaan.",
             "I drew a train for the tree. With a Christmas tree inside it. That's how it should have gone."),
            ("Mijn kerstwens: een treinverbinding van de Guhmension naar Goringuhm. Dan was de Sint ook nooit verdwaald.",
             "My Christmas wish: a rail link from the Guhmension to Goringuhm. Then the Sint would never have got lost either."),
            ("Gourmetten is eigenlijk een rangeerterrein. Al die pannetjes, elk op een eigen spoortje. Prachtig.",
             "Gourmet night is basically a shunting yard. All those little pans, each on its own little track. Beautiful."),
        ],
        "oudjaar": [
            ("Mijn voornemen: elke maand een nieuwe treinreis. Een luchtballon telt niet. Een luchtballon is geen echt OV.",
             "My resolution: a new train trip every month. A hot air balloon doesn't count. A hot air balloon isn't real public transport."),
            ("Wist je dat de eerste trein in Nederland in 1839 reed? Dat vier ik elk oudjaar een beetje mee. Verder niemand.",
             "Did you know the first train in the Netherlands ran in 1839? I celebrate that a little every New Year's Eve. Nobody else does."),
            ("Evi wil naar Japan vliegen. Ik heb een route gevonden met alleen treinen. Hij duurt drie weken. Vahoeg!",
             "Evi wants to fly to Japan. I found a route with only trains. It takes three weeks. Vahoeg!"),
            ("Lins airfryer is efficiënt, op tijd en betrouwbaar. Eigenlijk is hij net een goede trein.",
             "Lin's air fryer is efficient, punctual and reliable. It's basically a good train."),
        ],
    },
    "krijn": {
        "sint": [
            ("Welkom, welkom! Kom binnen, warm je pootjes. Bij ons staat de deur altijd open. Behalve als het tocht.",
             "Welcome, welcome! Come in, warm your paws. Our door is always open. Except when there's a draught."),
            ("Vijf kinderen, een plein vol gasten en nu ook nog de Sint. Gezellig! Corine, hebben we genoeg stoelen?",
             "Five kids, a square full of guests and now the Sint as well. Cosy! Corine, do we have enough chairs?"),
            ("De Sint mag hier blijven zolang hij wil. Ik heb de logeerkamer al opgemaakt. En een emmer klaargezet voor Ozovads.",
             "The Sint can stay as long as he likes. I've already made up the guest room. And put out a bucket for Ozovads."),
            ("Zet je schoentje maar bij onze schoorsteen. Die is extra breed. Voor de zekerheid.",
             "Go ahead and put your shoe by our chimney. It's extra wide. Just in case."),
        ],
        "kerst": [
            ("Mijn vader is er! Opa Wijnand. Joost stelde hem voor als Jochum. Ik weet echt niet waar dat vandaan komt.",
             "My father is here! Grandpa Wijnand. Joost introduced him as Jochum. I truly have no idea where that came from."),
            ("Het gourmetstel is uit de kast. Zestien pannetjes. Ik heb ze geteld. Twee keer.",
             "The gourmet set is out of the cupboard. Sixteen little pans. I counted them. Twice."),
            ("Ik heb zelf de piek in de boom gezet. Op mijn tenen. Op een stoel. Op nog een stoel.",
             "I put the star on top of the tree myself. On tiptoe. On a chair. On another chair."),
            ("Kerst met de hele familie. Daar wacht ik het hele jaar op. Nou ja, daarop en op de oliebollen.",
             "Christmas with the whole family. That's what I wait for all year. Well, that and the oliebollen."),
        ],
        "oudjaar": [
            ("Om twaalf uur zingen we allemaal 'Gelukkig nieuwjaar'. Vals. Maar met heel veel liefde.",
             "At midnight we all sing 'Happy New Year'. Off-key. But with a lot of love."),
            ("Mijn voornemen: de dakgoot schoonmaken. Dat is al elf jaar mijn voornemen. Het is een traditie.",
             "My resolution: clean the gutter. It's been my resolution for eleven years. It's a tradition."),
            ("Poedersuiker op je oliebol, poedersuiker op je snuit, poedersuiker op de hond. We hebben geen hond. Toch zit het erop.",
             "Powdered sugar on your oliebol, powdered sugar on your snout, powdered sugar on the dog. We don't have a dog. It's on there anyway."),
            ("Wat een jaar, vadsje. Fijn dat je er was. Kom volgend jaar weer, hoor!",
             "What a year, vadsje. Lovely that you were here. Come again next year!"),
        ],
    },
    "corine": {
        "sint": [
            ("Hallo lieverd! Heb je al chocomel gehad? Nee? Dan krijg je er twee.",
             "Hello sweetie! Have you had hot chocolate yet? No? Then you get two."),
            ("Mijn kinderen zijn allemaal groot, maar met Sinterklaas worden ze weer klein. Vooral Joost.",
             "My children are all grown up, but at Sinterklaas they're little again. Especially Joost."),
            ("De Sint ziet er moe uit van zijn reis. Ik heb hem een dekentje en soep gegeven. Mamadingen.",
             "The Sint looks tired from his trip. I gave him a blanket and some soup. Mum things."),
            ("Suus vroeg of ik weet wat Joost voor haar in de zak heeft. Ik zei: pepernoten, lieverd. Ze keek een beetje teleurgesteld.",
             "Suus asked if I knew what Joost has in his bag for her. I said: pepernoten, sweetie. She looked a little disappointed."),
        ],
        "kerst": [
            ("Het gourmetten begint pas als iedereen zit. Jij ook, lieverd. Er is altijd een plekje.",
             "Gourmet night only starts when everyone is seated. You too, sweetie. There's always a spot."),
            ("Lin krijgt een eigen pannetje zonder vet. Ik heb er een strikje omheen gedaan. Dan voelt ze zich welkom.",
             "Lin gets her own little pan without oil. I tied a bow around it. So she feels welcome."),
            ("Anthony vroeg waarom we uit mini-pannetjes eten. Ik zei: lieverd, dat is gourmetten. Hij heeft er vier foto's van gemaakt.",
             "Anthony asked why we eat out of mini pans. I said: sweetie, that's gourmet night. He took four photos of it."),
            ("Mijn moeder zit bij de haard. Ze breit al drie uur aan één sjaal. Hij is al vier meter. Voor iedereen een stukje.",
             "My mother is sitting by the fire. She's been knitting one scarf for three hours. It's already four metres long. A piece for everyone."),
        ],
        "oudjaar": [
            ("Het beslag staat sinds vanochtend te rijzen. Mijn geheim? Liefde. En gist. Vooral gist.",
             "The batter has been rising since this morning. My secret? Love. And yeast. Mostly yeast."),
            ("Lins airfryer-oliebollen zijn lekkerder dan de mijne. Ik ben er heel trots op. En een heel klein beetje jaloers.",
             "Lin's air fryer oliebollen are tastier than mine. I'm very proud of her. And a tiny bit jealous."),
            ("Mijn voornemen: meer tijd voor mezelf. Maar eerst bak ik nog even een appelflap voor jou.",
             "My resolution: more time for myself. But first let me bake you an apple turnover."),
            ("Doe je sjaal om als je naar buiten gaat, lieverd. Ook als je een guh bent. Juist als je een guh bent.",
             "Wear your scarf when you go outside, sweetie. Even if you're a guh. Especially if you're a guh."),
        ],
    },
    "marrie": {
        "sint": [
            ("Kom eens bij oma zitten, vadsje. Wil je een pepernootje? Ik heb er altijd een paar in mijn schort.",
             "Come and sit with Grandma, vadsje. Would you like a pepernoot? I always keep a few in my apron."),
            ("Vroeger kwam de Sint gewoon over het dak. Nu verdwaalt hij met een boot. Alles verandert.",
             "In my day the Sint just came over the roof. Now he gets lost in a boat. Everything changes."),
            ("Ik brei voor iedereen een sjaal. Ook voor Ozovads. Die wordt wel wat langer.",
             "I'm knitting a scarf for everyone. For Ozovads too. His will be a bit longer."),
            ("Corine was vroeger zo bang voor de Sint. Nu maakt ze soep voor hem. Kinderen worden zo snel groot.",
             "Corine used to be so scared of the Sint. Now she makes him soup. Children grow up so fast."),
        ],
        "kerst": [
            ("Lekker bij de haard, vadsje. Warme pootjes, warm hartje. Zo hoort kerst.",
             "Nice and cosy by the fire, vadsje. Warm paws, warm heart. That's how Christmas should be."),
            ("Wijnand ken ik al zestig jaar. Hij heette toen ook al Wijnand. Ik weet niet waar die Jochum vandaan komt.",
             "I've known Wijnand for sixty years. He was called Wijnand back then too. I don't know where this Jochum comes from."),
            ("Anthony heeft een foto van mijn breiwerk gemaakt. En van mijn theekopje. En van mij. Hij is een lieve jongen.",
             "Anthony took a photo of my knitting. And of my teacup. And of me. He's a sweet boy."),
            ("Ik heb voor de boom al mijn kleinkinderen getekend. Het werd een hele rij rondjes. Allemaal even lief.",
             "I drew all my grandchildren for the tree. It became a long row of circles. All equally sweet."),
        ],
        "oudjaar": [
            ("Mijn voornemen: nog meer sjaals. Er is altijd wel iemand die het koud heeft.",
             "My resolution: even more scarves. There's always someone who's cold."),
            ("Om twaalf uur krijgt iedereen een kus van oma. Jij ook. Daar ontkom je niet aan.",
             "At midnight everyone gets a kiss from Grandma. You too. There's no escaping it."),
            ("Oliebollen zijn net kleinkinderen. Rond, zoet, en je kunt er nooit genoeg van hebben.",
             "Oliebollen are just like grandchildren. Round, sweet, and you can never have too many."),
            ("Weet je wat het mooiste is aan oudjaar? Dat iedereen er is. Dat is genoeg, vadsje.",
             "Do you know the best thing about New Year's Eve? That everyone's here. That's enough, vadsje."),
        ],
    },
    "wijnand": {
        "kerst": [
            ("Goedendag. Ik ben opa Wijnand. Niet Jochum. Wijnand.",
             "Good day. I'm Grandpa Wijnand. Not Jochum. Wijnand."),
            ("Ze zeiden: 'Opa, het is in de Guhmension.' Ik dacht dat dat een dorpje bij Goringuhm was. Ik heb lang gefietst.",
             "They said: 'Grandpa, it's in the Guhmension.' I thought that was a village near Goringuhm. I cycled for a long time."),
            ("Jochum? Wie is Jochum? Ik ken geen Jochum. ...Wijnand.",
             "Jochum? Who's Jochum? I don't know any Jochum. ...Wijnand."),
            ("Mijn kleinkinderen. Allemaal zo groot. En die kleintjes bij de vijver zijn mijn achterkleinkinderen. Vahoeg, wat word ik oud.",
             "My grandchildren. All so big. And those little ones by the pond are my great-grandchildren. Vahoeg, I'm getting old."),
            ("Gourmetten. In mijn tijd heette dat: alles in één pan gooien. Maar goed, ik doe mee.",
             "Gourmet night. In my day that was called: throw everything in one pan. But fine, I'll join in."),
        ],
        "oudjaar": [
            ("Mijn voornemen: dat iedereen dit jaar mijn naam goed zegt. Wijnand. W-IJ-N-A-N-D.",
             "My resolution: that everyone gets my name right this year. Wijnand. W-I-J-N-A-N-D."),
            ("Een oliebol uit zo'n luchtfrituurding. Nooit gedacht dat ik dat zou eten. Hij is heerlijk. Niet verder vertellen.",
             "An oliebol out of one of those air-frying things. Never thought I'd eat that. It's delicious. Don't tell anyone."),
            ("Toen ik jong was, deden we op oudjaar aan carbidschieten. Nu gooien de kleinkinderen met poedersuiker. Ook gezellig.",
             "When I was young we did carbide shooting on New Year's Eve. Now the grandkids throw powdered sugar around. Also nice."),
            ("Gelukkig nieuwjaar, vadsje. Zeg maar opa. Opa Wijnand.",
             "Happy new year, vadsje. Just call me Grandpa. Grandpa Wijnand."),
        ],
    },
    "rins": {
        "sint": [
            ("Dit is Anthony's eerste Sinterklaas. Hij heeft al twaalf vragen gesteld. Drie kon ik er beantwoorden.",
             "This is Anthony's first Sinterklaas. He's asked twelve questions already. I could answer three."),
            ("Als oudste zus maakte ik vroeger alle surprises voor de kleintjes. Nu doet Daan het voor die van hem. De cirkel van het leven.",
             "As the eldest sister I used to make all the surprises for the little ones. Now Daan does it for his. The circle of life."),
            ("Anthony vindt de stoomboot het allermooiste. Ik heb hem maar niet verteld dat de Sint hier per ongeluk is.",
             "Anthony thinks the steamboat is the best part. I haven't told him the Sint is only here by accident."),
            ("Ik heb een gedicht geschreven voor mijn lootje. Het rijmt nergens op, maar het komt uit mijn hart. Njeg.",
             "I wrote a poem for my lootje. It doesn't rhyme at all, but it comes from the heart. Njeg."),
        ],
        "kerst": [
            ("Anthony heeft nog nooit gegourmet. Ik heb gezegd dat het 'Dutch barbecue' is. Hij is er heel zenuwachtig voor.",
             "Anthony has never done gourmet before. I told him it's 'Dutch barbecue'. He's very nervous about it."),
            ("In Guhmerika eten ze kalkoen met kerst. Hier hebben we zestien pannetjes en een opa die Wijnand heet. Ik weet wat ik liever heb.",
             "In Guhmerika they eat turkey at Christmas. Here we have sixteen little pans and a grandpa called Wijnand. I know which I prefer."),
            ("Ik heb Anthony en mij getekend voor in de boom. Hij met zijn camera, ik met een boek. Klopt precies.",
             "I drew Anthony and me for the tree. Him with his camera, me with a book. Spot on."),
            ("Kijk ze daar staan bij de vijver, Daan en zijn kleintjes. Ik word er helemaal zacht van.",
             "Look at them by the pond, Daan and his little ones. It makes me all soft inside."),
        ],
        "oudjaar": [
            ("Anthony probeert 'oliebol' te zeggen. Het klinkt als 'oily ball'. Dat klopt eigenlijk wel.",
             "Anthony is trying to say 'oliebol'. It comes out as 'oily ball'. Which is actually accurate."),
            ("Mijn voornemen: Anthony leren fietsen in de regen. Echte inburgering.",
             "My resolution: teach Anthony to cycle in the rain. Proper integration."),
            ("Volgend jaar vieren we kerst in Guhmerika. Anthony's moeder wil weten wat gourmetten is. Ik neem het stel mee.",
             "Next year we're spending Christmas in Guhmerika. Anthony's mum wants to know what gourmet is. I'm bringing the set."),
            ("Fijne jaarwisseling, vadsje. Pas op met vuurwerk. En met Joost bij de oliebollen.",
             "Happy new year, vadsje. Careful with fireworks. And with Joost near the oliebollen."),
        ],
    },
    # Anthony speaks Dunglish (in both languages)
    "anthony": {
        "sint": [
            ("Why is the bishop on a boat?? Is dit normaal? Rins zegt dat dit normaal is.",
             "Why is the bishop on a boat?? Is this normal? Rins says this is normaal."),
            ("Pepernoten! Little cookies, heel hard, and you throw them at people? Wat een land. I love it.",
             "Pepernoten! Little cookies, very hard, and you throw them at people? What a country. I love it."),
            ("*klik* Sorry, ik maak een foto of everything. Dat paard. Dat dak. Jou. *klik*",
             "*click* Sorry, I take a foto of everything. That horse. That roof. You. *click*"),
            ("In Guhmerika we have Santa. Hij heeft een slee. Jullie Sint has a boat and a horse en hij is verdwaald. Much more spannend!",
             "In Guhmerika we have Santa. He has a sleigh. Your Sint has a boat and a horse and he got verdwaald. Much more spannend!"),
            ("You put a shoe bij de... schoorsteen? And then candy komt in je schoen? Your SHOE? Okay. Okay. Ik doe mee.",
             "You put a shoe by the... schoorsteen? And then candy comes in your shoe? Your SHOE? Okay. Okay. Ik doe mee."),
        ],
        "kerst": [
            ("Tonight we do 'gourmet'. Iedereen krijgt een tiny pannetje. A TINY pan! I have never been zo blij.",
             "Tonight we do 'gourmet'. Everybody gets a tiny pannetje. A TINY pan! I have never been so blij."),
            ("Opa Jochum is very nice. Wat? Wijnand? Oh no. Sorry, opa. Sorry, Wijnand.",
             "Opa Jochum is very nice. What? Wijnand? Oh no. Sorry, opa. Sorry, Wijnand."),
            ("*klik* De boom! *klik* De ballen! *klik* Nog een keer de boom! My camera is vol. Ik koop een nieuwe.",
             "*click* The tree! *click* The baubles! *click* The tree again! My camera is vol. I'm buying a new one."),
            ("In Guhmerika we eat turkey. Hier maak ik my own little... hoe heet dat... pannetje. With a little egg! Prachtig!",
             "In Guhmerika we eat turkey. Here I make my own little... how do you say... pannetje. With a little egg! Prachtig!"),
        ],
        "oudjaar": [
            ("Oliebollen! It is a donut, maar... ronder? With raisins? En suikersneeuw? Ik heb er al zes op.",
             "Oliebollen! It's a donut, but... rounder? With raisins? And sugar snow? I've already had zes."),
            ("Lin says 'never fried' en ze bakt ze in een little machine. Bertus! Bertus is the best kok. *klik*",
             "Lin says 'never fried' and she bakes them in a little machine. Bertus! Bertus is the best kok. *click*"),
            ("Mijn goede voornemen: Nederlands leren. 'Gezellig'. 'Lekker'. 'Njeg'. I am al bijna fluent.",
             "My good resolution: learn Dutch. 'Hagelslag'. 'Pindakaas'. 'Njeg'. I'm already almost fluent."),
            ("Next year I come back. Volgend jaar! Met een grotere camera. And a bigger maag.",
             "Next year I come back. Volgend jaar! With a bigger camera. And a bigger maag."),
        ],
    },
    "daan": {
        "sint": [
            ("Hup Finn, mooi zo! Kijk eens hoe hij schaatst. Dat heeft hij van mij. Het vallen ook.",
             "Go Finn, well done! Look at him skate. He gets that from me. The falling too."),
            ("Liv wil alleen nog maar op de slee. Ik ben de motor. Een hele moeie motor.",
             "Liv only wants to ride the sled. I'm the engine. A very tired engine."),
            ("Kim en ik hebben de surprises voor de kinderen al klaar. Finn krijgt een kartonnen schaats. Liv een kartonnen slee. Ik krijg rust. Hoop ik.",
             "Kim and I have the surprises for the kids ready. Finn gets a cardboard skate. Liv a cardboard sled. I get some rest. I hope."),
            ("Toen ik zo klein was als Finn, kwam de Sint gewoon in Goringuhm aan. Nu staat hij in de Guhmension. Kinderen van nu hebben geluk.",
             "When I was as small as Finn, the Sint just arrived in Goringuhm. Now he's in the Guhmension. Kids these days are lucky."),
        ],
        "kerst": [
            ("Wij vieren kerst lekker met z'n viertjes. Maar even langs de vijver bij opa en oma kan altijd.",
             "We celebrate Christmas just the four of us. But a quick stop at the pond by Grandpa and Grandma's is always fine."),
            ("Nog één rondje, Liv? Dat zei je twintig rondjes geleden ook.",
             "One more round, Liv? You said that twenty rounds ago too."),
            ("Finn wil later de Elftocht schaatsen. Ik zei: eerst een rondje om de vijver zonder te vallen. Hij viel.",
             "Finn wants to skate the Elftocht when he's older. I said: first one round of the pond without falling. He fell."),
            ("Opa Wijnand heeft Liv een belletje voor haar slee gegeven. Nu krijgen we haar er nooit meer af. Van de slee, bedoel ik.",
             "Grandpa Wijnand gave Liv a little bell for her sled. Now we'll never get her off it. The sled, I mean."),
        ],
        "oudjaar": [
            ("De kinderen mogen opblijven tot twaalf uur. Finn is van plan wakker te blijven. Finn slaapt om negen uur.",
             "The kids can stay up till midnight. Finn is planning to stay awake. Finn will be asleep by nine."),
            ("Mijn voornemen: als Liv 'nog één rondje' zegt, echt nog maar één rondje. Dat ga ik niet halen.",
             "My resolution: when Liv says 'one more round', really only one more round. I'm not going to make it."),
            ("Oliebollen met de kinderen is vooral poedersuiker opruimen. Maar wel heel gezellige poedersuiker.",
             "Oliebollen with the kids is mostly cleaning up powdered sugar. But very cosy powdered sugar."),
            ("Gelukkig nieuwjaar, vadsje! Op een jaar vol sleerondjes en schaatsjes.",
             "Happy new year, vadsje! Here's to a year full of sled rides and skating."),
        ],
    },
    "kim": {
        "sint": [
            ("Finn oefent de hele dag al voor de Sint. Hij wil laten zien dat hij een pirouette kan. Hij kan bijna een pirouette.",
             "Finn has been practising all day for the Sint. He wants to show he can do a pirouette. He can almost do a pirouette."),
            ("Liv heeft een tekening gemaakt voor de Sint. Het is een paard. Of een wolk. Ze zegt dat het allebei is.",
             "Liv made a drawing for the Sint. It's a horse. Or a cloud. She says it's both."),
            ("Wij zijn een eigen gezinnetje, maar met Sinterklaas komen we natuurlijk gewoon langs. Pepernoten zijn voor iedereen.",
             "We're our own little family, but of course we drop by for Sinterklaas. Pepernoten are for everyone."),
            ("Daan duwt de slee al een uur. Hij zegt dat het goed is voor zijn conditie. Hij hijgt een beetje.",
             "Daan has been pushing the sled for an hour. He says it's good for his fitness. He's a bit out of breath."),
        ],
        "kerst": [
            ("Kijk die kleintjes. Rode wangetjes, koude pootjes. Straks lekker warme chocomel.",
             "Look at those little ones. Rosy cheeks, cold paws. Hot chocolate in a bit."),
            ("Wij vieren kerst thuis met z'n viertjes, maar deze vijver is gewoon te mooi om over te slaan.",
             "We celebrate Christmas at home, just the four of us, but this pond is simply too lovely to skip."),
            ("Finn vroeg of de eendjes hier ook kerst vieren. Ik zei: vast wel, lieverd. Met kleine pannetjes.",
             "Finn asked if the ducks here celebrate Christmas too. I said: I'm sure they do, sweetie. With tiny little pans."),
            ("Liv wilde een kerstmuts voor haar slee. Ik heb er een gebreid. Nu heeft de slee het warmer dan ik.",
             "Liv wanted a Santa hat for her sled. I knitted one. Now the sled is warmer than me."),
        ],
        "oudjaar": [
            ("Mijn voornemen: vaker schaatsen met Finn. Al ben ik minder goed dan hij. Veel minder.",
             "My resolution: skate with Finn more often. Even though I'm not as good as him. Not nearly."),
            ("Liv heeft haar eerste oliebol gegeten. Er zat meer poedersuiker op haar dan in haar. Zo schattig.",
             "Liv ate her first oliebol. More powdered sugar ended up on her than in her. So cute."),
            ("Om twaalf uur houden we de kleintjes even vast en kijken we samen naar de lucht. Mooiste moment van het jaar.",
             "At midnight we hold the little ones for a moment and look at the sky together. Best moment of the year."),
            ("Fijne jaarwisseling, vadsje. Neem een oliebol mee voor onderweg. Of twee.",
             "Happy new year, vadsje. Take an oliebol for the road. Or two."),
        ],
    },
    "finn": {
        "sint": [
            ("Kijk! Kijk! Ik schaats zonder handen! ...Ik schaats altijd zonder handen. Guhs hebben pootjes.",
             "Look! Look! I'm skating with no hands! ...I always skate with no hands. Guhs have paws."),
            ("Ik ga de Sint vragen of Ozovads ook kan schaatsen. Paarden kunnen alles.",
             "I'm going to ask the Sint if Ozovads can skate too. Horses can do anything."),
            ("Ik ben zes. Bijna zeven. Nou ja, zes. Maar wel een grote zes.",
             "I'm six. Nearly seven. Well, six. But a big six."),
        ],
        "kerst": [
            ("Een pirouette! ...Bijna. Ik oefen nog even.",
             "A pirouette! ...Almost. Let me practise a bit more."),
            ("Overopa Wijnand zegt dat hij vroeger de Elftocht heeft geschaatst. Twee keer! Of was het de vijver? Hij weet het niet meer.",
             "Great-grandpa Wijnand says he once skated the Elftocht. Twice! Or was it the pond? He doesn't remember."),
            ("Ik heb voor in de boom een tekening van mezelf gemaakt. Schaatsend. Heel snel. Je ziet alleen maar strepen.",
             "I made a drawing of myself for the tree. Skating. Really fast. You only see streaks."),
        ],
        "oudjaar": [
            ("Ik blijf vannacht op tot twaalf uur! Ik ben helemaal niet moe. *gaap*",
             "I'm staying up till midnight tonight! I'm not tired at all. *yawn*"),
            ("Mijn voornemen: een echte pirouette. En nooit meer vallen. En een hond.",
             "My resolution: a real pirouette. And never fall again. And a dog."),
            ("Oliebollen zijn net schaatsers. Ze rollen overal heen. Vooral van je bord af.",
             "Oliebollen are just like skaters. They roll everywhere. Mostly off your plate."),
        ],
    },
    "liv": {
        "sint": [
            ("Papa! Nog een rondje! Nog een rondje!", "Daddy! One more round! One more round!"),
            ("Ik ben bijna vier. Dat is heel groot. Groter dan drie.", "I'm almost four. That's very big. Bigger than three."),
            ("Paardje! Mag ik op het paardje? Nee? Dan op de slee. Papa, duwen!",
             "Horsey! Can I ride the horsey? No? Then the sled. Daddy, push!"),
        ],
        "kerst": [
            ("Mijn slee heeft een muts. Mama heeft hem gebreid. Nu heeft hij het niet koud.",
             "My sled has a hat. Mummy knitted it. Now it's not cold."),
            ("Overopa heeft me een belletje gegeven! Tingeling! Tingeling! Tingeling!",
             "Great-grandpa gave me a little bell! Jingle! Jingle! Jingle!"),
            ("Ik heb een tekening gemaakt. Het is een kerstboom. Of een paardje. Het is allebei.",
             "I made a drawing. It's a Christmas tree. Or a horsey. It's both."),
        ],
        "oudjaar": [
            ("Poedersuiker! Het sneeuwt op mijn oliebol!", "Powdered sugar! It's snowing on my oliebol!"),
            ("Ik blijf ook op tot twaalf. Net als Finn. ...Wat is twaalf?",
             "I'm staying up till twelve too. Just like Finn. ...What's twelve?"),
            ("Papa, nog een rondje! Het is een nieuw jaar, dus het telt niet.",
             "Daddy, one more round! It's a new year, so it doesn't count."),
        ],
    },
    "sint": {
        "sint": [
            ("Welkom, lieve kinderen van... van... Dit is niet Goringuhm, hè?",
             "Welcome, dear children of... of... This isn't Goringuhm, is it?"),
            ("Bij Slot Loevestijn links... of was het nou rechts? Volgend jaar neem ik een kaart mee. Of een Niels.",
             "At Slot Loevestijn left... or was it right? Next year I'll bring a map. Or a Niels."),
            ("Het Grote Boek weet alles. Wie er lief was, wie er een lootje wil, en wie er stiekem van de pepernoten heeft gesnoept. Ja, jij.",
             "The Big Book knows everything. Who was good, who wants a lootje, and who secretly nibbled the pepernoten. Yes, you."),
            ("Wat een warm onthaal in deze koude Guhmension. Hier voel ik me thuis, njeg.",
             "What a warm welcome in this cold Guhmension. I feel at home here, njeg."),
            ("Mijn pakjesguhs klimmen over alle daken. Ze zeggen dat het voor de pakjes is. Ik denk dat ze het gewoon leuk vinden.",
             "My pakjesguhs climb over every roof. They say it's for the presents. I think they just enjoy it."),
        ],
    },
    "pakjesguh_0": {
        "sint": [
            ("Hoi! Wil je een pepernoot? Vang! ...Oeps, die ging over het dak.",
             "Hi! Want a pepernoot? Catch! ...Oops, that one went over the roof."),
            ("Ik heb de schoorstenen geteld. Er is er één. Dat scheelt een hoop klimmen.",
             "I counted the chimneys. There's one. Saves a lot of climbing."),
            ("Dit roetvegje? Van de schoorsteen! Ik draag het met trots. Een klein beetje maar.",
             "This soot smudge? From the chimney! I wear it with pride. Just a tiny bit."),
        ],
    },
    "pakjesguh_1": {
        "sint": [
            ("De helft van de pakjes is overboord geslagen. Ze drijven nu ergens rond. Heel feestelijk, maar wel nat.",
             "Half the presents went overboard. They're floating around somewhere now. Very festive, but wet."),
            ("Mijn lievelingsklusje? Pepernoten strooien. Mijn tweede lievelingsklusje? Pepernoten opeten.",
             "My favourite job? Throwing pepernoten. My second favourite job? Eating pepernoten."),
            ("Ik ben roze, ik heb een veer op mijn baret en ik kan op elk dak klimmen. Vahoeg!",
             "I'm pink, I've got a feather on my beret and I can climb any roof. Vahoeg!"),
        ],
    },
    "pakjesguh_2": {
        "sint": [
            ("Ik pak de cadeautjes in. Elk pakje krijgt een strikje. Daarom heet ik zo.",
             "I wrap the presents. Every present gets a bow. That's why they call me Strikje."),
            ("Ozovads liep weg toen we aanmeerden. Hij houdt van wortels. Heel erg van wortels.",
             "Ozovads ran off when we docked. He loves carrots. Really loves carrots."),
            ("Het Grote Boek is zwaarder dan ik. Gelukkig is de Sint sterk. En zijn stoel ook.",
             "The Big Book is heavier than me. Luckily the Sint is strong. And so is his chair."),
        ],
    },
    "pakjesguh_3": {
        "sint": [
            ("Hupsakee! Van dak naar dak! Dat mag alleen met een pakjesdiploma.",
             "Hupsakee! From roof to roof! Only allowed with a parcel diploma."),
            ("Op pakjesavond bonzen we op de deur. Heel hard. En dan rennen we heel snel weg. Zo hoort het.",
             "On pakjesavond we bang on the door. Really loud. And then we run away really fast. That's how it's done."),
            ("Wat ik het mooiste vind? Blije gezichtjes. En pepernoten. Blije gezichtjes met pepernoten.",
             "What I love most? Happy faces. And pepernoten. Happy faces with pepernoten."),
        ],
    },
    "ozovads": {
        "sint": [
            ("Hinnik... vads!", "Neigh... vads!"),
            ("*snuffelt aan je zakken* Hinnikvads? Wortel?", "*sniffs at your pockets* Neighvads? Carrot?"),
            ("*schudt met zijn manen en kijkt heel trots* Hinnik!", "*shakes his mane and looks very proud* Neigh!"),
        ],
    },
    "winkelguh": {
        "sint": [
            ("Welkom in mijn winkeltje! Alles is te koop voor pepernoten. Echte pepernoten, geen knopen.",
             "Welcome to my little shop! Everything's for sale for pepernoten. Real pepernoten, not buttons."),
            ("Chocoladeletters! Ik heb alle letters. Behalve de Q. Die wilde niemand.",
             "Chocolate letters! I have every letter. Except the Q. Nobody wanted it."),
            ("Warme chocomel, met slagroom en een guh-gezichtje erop. Het gezichtje is gratis.",
             "Hot chocolate, with whipped cream and a little guh face on top. The face is free."),
            ("Pepernoten verdien je met spelletjes en door goed te zoeken op het plein. Ze liggen overal. Echt overal.",
             "You earn pepernoten with games and by searching the square well. They're everywhere. Really everywhere."),
        ],
        "kerst": [
            ("Kerstmutsen! Ze smelten wel op 3 januari, maar tot die tijd ben jij de mooiste guh van het plein.",
             "Santa hats! They do melt on 3 January, but until then you're the prettiest guh on the square."),
            ("Kerstlampjes voor thuis! Tijdelijk, net als sneeuw. Daarom zijn ze juist zo bijzonder.",
             "Christmas lights for your home! Temporary, like snow. That's exactly what makes them special."),
            ("Schaatsen! Sneller op het ijs. Niet sneller dan Finn, die is onverslaanbaar.",
             "Skates! Faster on the ice. Not faster than Finn though, he's unbeatable."),
            ("Het is druk in de winkel met kerst. Nou ja, druk. Jij bent er. Dat is druk genoeg.",
             "The shop is busy at Christmas. Well, busy. You're here. That's busy enough."),
        ],
        "oudjaar": [
            ("Vuurwerkpijlen! Afsteken mag, maar met beleid. En niet in de richting van oma Marrie.",
             "Firework rockets! You can set them off, but carefully. And not in the direction of Grandma Marrie."),
            ("Alles in mijn winkeltje is tijdelijke pret. Op 3 januari smelt het weg. Dus geniet ervan, vadsje!",
             "Everything in my shop is temporary fun. On 3 January it melts away. So enjoy it, vadsje!"),
            ("Mijn voornemen: volgend jaar een groter winkeltje. Met een hoekje voor airfryers. Voor Lin.",
             "My resolution: a bigger shop next year. With a corner for air fryers. For Lin."),
            ("Pepernoten over? Die blijven gewoon eetbaar, hoor. Ook in het nieuwe jaar. Pepernoten zijn voor altijd.",
             "Pepernoten left over? They stay edible, you know. Even in the new year. Pepernoten are forever."),
        ],
    },
}


# the guh words in English (tools/lang/GLOSSARY.md): njeg -> nyeg, vahoeg -> wahoog, vadsje (you) -> chonky, vads -> chonk
_GLOSSARIUM = [(r"NJEG", "NYEG"), (r"Njeg", "Nyeg"), (r"njeg", "nyeg"), (r"VAHOEG", "WAHOOG"), (r"Vahoeg", "Wahoog"),
               (r"vahoeg", "wahoog"), (r"vadsje", "chonky"), (r"vads", "chonk")]


def _engels(en):
    for a, b in _GLOSSARIUM:
        en = re.sub(a, b, en)
    return en


def teksten():
    """{key: (Dutch, English)}"""
    t = {"entity.guhs.guhwinter_guh": ("Winterguh", "Winter Guh")}
    for naam, (nl, en) in NAMEN.items():
        t[P + "naam." + naam] = (nl, en)
    for naam, fasen in PRAAT.items():
        for fase, regels in fasen.items():
            for i, (nl, en) in enumerate(regels, 1):
                t[f"{P}praat.{naam}.{fase}.{i}"] = (nl, _engels(en))
    return t


def _controle():
    problemen = []
    alle_fasen = {"sint", "kerst", "oudjaar"}
    alleen_sint = {"sint", "ozovads"} | {f"pakjesguh_{i}" for i in range(4)}
    for naam in NAMEN:
        fasen = PRAAT.get(naam, {})
        nodig = {"sint"} if naam in alleen_sint else ({"kerst", "oudjaar"} if naam == "wijnand" else alle_fasen)
        if set(fasen) != nodig:
            problemen.append(f"{naam}: lines for {sorted(fasen)}, needs {sorted(nodig)}")
        for fase, regels in fasen.items():
            if not 3 <= len(regels) <= 6:
                problemen.append(f"{naam}.{fase}: {len(regels)} lines (3-6)")
    for k, (nl, en) in teksten().items():
        if "mika" in (nl + en).lower():
            problemen.append(f"{k}: no Mika's in this event")
    return problemen


def _check_looks(h):
    """Every bone a look's pass shows must have its swatch painted in that look's texture."""
    from PIL import Image
    geo = os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json")
    bones = json.load(open(geo, encoding="utf-8"))["minecraft:geometry"][0]["bones"]
    problemen = []
    for look, prefixes in LOOKS.items():
        pad = os.path.join(h.A, "textures", "entity", "guh_clothes", f"guhwinter_{look}.png")
        if not os.path.exists(pad):
            problemen.append(f"{look}: no texture (run tools/make_guh_variants.py)")
            continue
        img = np.asarray(Image.open(pad).convert("RGBA"))
        gevonden = set()
        for b in bones:
            if not any(b["name"].startswith(p) for p in prefixes):
                continue
            gevonden.add(next(p for p in prefixes if b["name"].startswith(p)))
            for c in b.get("cubes", []):
                u, v = c["uv"]["north"]["uv"]
                if img[int(v) * 4 + 16, int(u) * 4 + 16, 3] == 0:
                    problemen.append(f"{look}: bone {b['name']} (uv {u},{v}) is not painted")
                    break
        for p in prefixes:
            if p not in gevonden:
                problemen.append(f"{look}: no bone {p}* in guh.geo.json (run tools/make_guh_variants.py)")
    return problemen


def build(h):
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)
    _overlay()   # the English overlay (tools/lang/en/<chunk>.json) straight from the same table
    _bones_erin(h)
    problemen = _controle() + _texturen(h) + _check_looks(h)
    for p in problemen:
        print("guhwinter_npc:", p)
    if problemen:
        raise SystemExit("guhwinter_npc: fix the problems above")


def _overlay():
    pad = os.path.join("tools", "lang", "en", f"{CHUNK}.json")
    with open(pad, "w", encoding="utf-8", newline=NL) as f:
        json.dump({k: en for k, (nl, en) in sorted(teksten().items())}, f, indent=2, ensure_ascii=False)
        f.write(NL)


def _lang_direct(a):
    """Standalone: our keys straight into nl_nl.json (Dutch) and en_us.json (English); keys we no longer make go."""
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        pad = os.path.join(a, "lang", f"{taal}.json")
        with open(pad, encoding="utf-8") as f:
            data = json.load(f)
        for k in [k for k in data if k.startswith(P) and k not in t]:
            del data[k]
        data.update({k: v[i] for k, v in t.items()})
        with open(pad, "w", encoding="utf-8", newline=NL) as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write(NL)


if __name__ == "__main__":
    # standalone (from the project root; make_resources.py needs a 21.1 jar this machine no longer has): bones, textures,
    # lang (nl_nl + en_us + the overlay) and the checks
    import types
    _a = os.path.join("src", "main", "resources", "assets", "guhs")
    _h = types.SimpleNamespace(A=_a)
    _bones_erin(_h)
    _overlay()
    _lang_direct(_a)
    _p = _controle() + _texturen(_h) + _check_looks(_h)
    print(NL.join(_p) or f"guhwinter_npc: ok ({len(teksten())} texts)")
