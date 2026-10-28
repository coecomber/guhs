"""
1.8.0 (Guhdromenland), TEST BUILD: the Weer vadser and its weerpotjes (ontwerp-guhs180-dromen.md, "Testbuild-feedback 1").
Java: feature/weer (WeerFeature, Weertje, WeerZones, WeerVadserBlock(+Entity), WeerPotjeItem, WeerUnlocks, WeerEffecten,
WeerCommando, client/WeerClient + WeerScherm + WeerDeeltje) and mixin/client/WeerNeerslagMixin.

  build(h)    the Weer vadser (a guh machine: features/vadskracht.machine, with a cloud and a sun on its front) and its
              recipe, the eight weerpotjes (item textures + models), the five falling particles (the textures of the
              existing guh particles), the chest loot (data/guhs/loot_table/weer/potje_<id>.json, added to the chests of
              BRONNEN by the loot modifiers data/guhs/loot_modifiers/weerpotje_<id>__<chest>.json, about 1 in 8), the texts
              (Dutch in nl_nl, English in tools/lang/en/c211_weer.json) and a self-check

Standalone (from the project root): python tools/features/weer.py   (writes only the Weer vadser's own files, including the
26.1 item definitions; a full tools/mc26.py run is not needed)
"""
import json
import os
import sys

from PIL import Image, ImageDraw

try:
    from features import vadskracht
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadskracht

BLOK = "weer_vadser"
KRACHT = 20           # (Java: WeerFeature.KRACHT)
KANS = 0.125          # a weerpotje in about 1 in 8 of the chests of BRONNEN

# the guh-weertjes: id -> (Dutch, English)
WEERTJES = {
    "pluisjes": ("Pluisjessneeuw", "Fluff Snow"),
    "bellen": ("Bellenbui", "Bubble Shower"),
    "knabbel": ("Knabbelbuitje", "Nibble Drizzle"),
    "regenboog": ("Regenboogweer", "Rainbow Weather"),
    "hartjes": ("Hartjesregen", "Heart Rain"),
    "sterren": ("Sterrenregen", "Star Shower"),
    "knusmist": ("Knusmist", "Cosy Fog"),
    "bloesem": ("Bloesemwind", "Blossom Breeze"),
}
ALTIJD = {"normaal": ("Normaal", "Normal"), "zon": ("Zon", "Sun"), "regen": ("Regen", "Rain"), "onweer": ("Onweer", "Thunderstorm")}

# the chests (oude content) where the weerpotjes hide. TODO 1.8.0: the dream sources (WeerFeature.DROOMBRONNEN).
# Not a chest, so in Java: knabbel (Knabbelspelen zeskamp won), regenboog (Guh-Circuit gold; the Regenboogje souvenir on a
# Weer vadser), sterren (a constellation at the telescope). The Wolkenhoeder-hut, the Regenboogbrug, the Knuffelbad and the
# Klaterdal tea house got a little chest of their own (tools/features/weerrest.py, chests/weer/*).
BRONNEN = {
    "pluisjes": ["guhs:chests/onderwol/oude_wolspinnerij", "guhs:chests/zwevende_eilanden_wolkenkist", "guhs:chests/weer/wolkenhoeder_hut"],
    "bellen": ["guhs:chests/onderwater_wrak", "guhs:chests/einde2_bouw_bikinibodem_huis", "guhs:chests/einde2_bouw_bikinibodem_keuken",
               "guhs:chests/einde2_bouw_bikinibodem_lab"],
    "sterren": ["guhs:chests/guh_sterrenwacht", "guhs:chests/onderwol/sterrengrot"],
    "knusmist": ["guhs:chests/onderwol/knuffelslaapzaal", "guhs:chests/weer/knuffelbad"],
    "hartjes": ["guhs:chests/guh_picnic", "guhs:chests/onderwol/verrassingsfeestje"],
    "regenboog": ["guhs:chests/weer/regenboogbrug"],
    "knabbel": ["guhs:chests/kaasknabbel_nest_schat"],
    "bloesem": ["guhs:chests/picknickeilandje", "guhs:chests/weer/dal_theehuisje"],
}

# what each weertje's potje holds (the colours of its jar's contents)
INHOUD = {
    "pluisjes": [(255, 196, 222), (255, 226, 240), (246, 160, 200)],
    "bellen": [(186, 236, 255), (232, 250, 255), (150, 206, 250)],
    "knabbel": [(246, 200, 82), (255, 226, 140), (214, 160, 50)],
    "regenboog": [(255, 96, 110), (255, 170, 80), (255, 226, 96), (126, 224, 122), (106, 184, 255), (176, 140, 255)],
    "hartjes": [(255, 120, 168), (255, 186, 210), (226, 76, 130)],
    "sterren": [(64, 60, 140), (255, 236, 140), (120, 110, 200)],
    "knusmist": [(240, 214, 228), (252, 236, 244), (220, 190, 210)],
    "bloesem": [(255, 176, 200), (255, 222, 232), (130, 196, 120)],
}

DEELTJES = {"weer_pluisje": [f"guhs:pluisje_{i}" for i in range(4)], "weer_zeepbel": [f"guhs:zeepbelletje_{i}" for i in range(3)],
            "weer_hartje": [f"guhs:band_hartje_{i}" for i in range(3)], "weer_bloesem": [f"guhs:bloesemblaadje_{i}" for i in range(3)],
            "weer_ster": [f"guhs:wensster_{i}" for i in range(3)]}

# key -> (Dutch, English)
TEKSTEN = {
    f"block.guhs.{BLOK}": ("Weer vadser", "Weather Vadser"),
    f"block.guhs.{BLOK}.lore": ("Maakt zijn eigen weertje, tot 60 blokken in de rondte. Rechtsklik: kies een weertje!",
                                "Makes its own weather, up to 60 blocks around. Right-click: pick a weather!"),
    f"block.guhs.{BLOK}.lore.kracht": ("Heeft %s vadskracht nodig zolang er een ander weertje dan normaal aanstaat",
                                       "Needs %s vadskracht while it makes any weather but normal"),
    "item.guhs.weerpotje.gebruik": ("Rechtsklik: het weertje is voor altijd van jou, in elke Weer vadser",
                                    "Right-click: the weather is yours for good, in every Weather Vadser"),
    "gui.guhs.weer.ontgrendeld": ("Ontgrendeld: %s! Kies hem in je Weer vadser. VAHOEG!", "Unlocked: %s! Pick it in your Weather Vadser. VAHOEG!"),
    "gui.guhs.weer.al_gehad": ("%s had je al, njeg! Geef het potje aan een vriend.", "You already had %s, njeg! Give the jar to a friend."),
    "gui.guhs.weer.potje_gekregen": ("Je kreeg een weerpotje: %s!", "You got a weather jar: %s!"),
    "gui.guhs.weer.op_slot": ("%s zit nog op slot. Zoek eerst zijn weerpotje!", "%s is still locked. Find its weather jar first!"),
    "gui.guhs.weer.uitgezet": ("De Weer vadser doet weer normaal. Gewoon weer, vads.", "The Weather Vadser is back to normal. Just weather, vads."),
    "gui.guhs.weer.gekozen": ("Weertje: %s! Tot 60 blokken in de rondte.", "Weather: %s! Up to 60 blocks around."),
    "gui.guhs.weer.gekozen_slaapt": ("Weertje: %s, maar de Weer vadser doet een dutje: hij heeft %s vadskracht nodig.",
                                     "Weather: %s, but the Weather Vadser is napping: it needs %s vadskracht."),
    "gui.guhs.weer.uitlezing": ("Weertje: %s", "Weather: %s"),
    "gui.guhs.weer.scherm.uit": ("Nu: gewoon het weer van de wereld. Kies een weertje!", "Now: just the world's weather. Pick one!"),
    "gui.guhs.weer.scherm.aan": ("Nu: %s, tot %s blokken in de rondte", "Now: %s, up to %s blocks around"),
    "gui.guhs.weer.scherm.slaapt": ("Zzz... de Weer vadser heeft %s vadskracht nodig", "Zzz... the Weather Vadser needs %s vadskracht"),
    "gui.guhs.weer.slot_hint": ("Op slot! Het weerpotje vind je hier: %s", "Locked! You find its weather jar here: %s"),
}
UITLEG = {
    "normaal": ("Gewoon het weer van de wereld. De Weer vadser doet een dutje en vraagt geen vadskracht.",
                "Just the world's own weather. The Weather Vadser takes a nap and asks no vadskracht."),
    "zon": ("Lekker zonnig, ook als het overal anders regent. Guhs houden van zonnetjes.", "Nice and sunny, even when it rains everywhere else."),
    "regen": ("Regen! In koude streken wordt het sneeuw. Blubs springen in de plassen.", "Rain! In cold places it snows. Blubs jump in the puddles."),
    "onweer": ("Rommel rommel! Af en toe een flits, maar niemand wordt geraakt en niks vliegt in brand.",
               "Rumble rumble! A flash now and then, but nobody gets hit and nothing catches fire."),
    "pluisjes": ("Zachte roze pluisjes dwarrelen naar beneden. Guhs springen om ze te vangen.", "Soft pink fluff drifts down. Guhs hop to catch it."),
    "bellen": ("Het regent zeepbellen! Blub-guhs worden er dolblij van en blazen mee.", "It rains soap bubbles! Blub-guhs love it and blow along."),
    "knabbel": ("Af en toe valt er een kaasknabbel uit de lucht. Niet te veel hoor, vads.", "Now and then a cheese nibble drops from the sky. Not too many, vads."),
    "regenboog": ("Zachte zonnige regen met een grote regenboog. Guhs en Blubs dansen in de plasjes.",
                  "Soft sunny rain with a big rainbow. Guhs and Blubs dance in the puddles."),
    "hartjes": ("Kleine hartjes dwarrelen neer. Je tamme guhs en Blubs worden extra knuffelig.", "Little hearts float down. Your tame guhs and Blubs get extra cuddly."),
    "sterren": ("'s Nachts vallen er sterretjes met een glitterstaart. Doe een wens!", "At night stars fall with glittering tails. Make a wish!"),
    "knusmist": ("Een zachte roze mist. Alles wordt knus en de guhs worden slaperig.", "A soft pink fog. Everything gets cosy and the guhs get sleepy."),
    "bloesem": ("Bloesemblaadjes waaien voorbij. Ruikt naar het Bloesemmeertje.", "Blossom petals blow by. Smells like the Bloesemmeertje."),
}
BRON_TEKST = {
    "pluisjes": ("kisten in de Oude wolspinnerij, de wolkenkisten van de Wolkenweide en het kistje in de Wolkenhoeder-hut",
                 "chests in the Old Wool Mill, the Cloud Meadow's cloud chests and the little chest in the Cloud Herder's hut"),
    "bellen": ("kisten in de Guhbubbel en in Bikinibodem", "chests in the Guh Bubble and in Bikini Bottom"),
    "knabbel": ("de schatkist van een Kaasknabbel-nest, of win de Knabbelspelen", "the treasure chest of a Cheese Nibble Nest, or win the Nibble Games"),
    "regenboog": ("het kistje aan het eind van de Regenboogbrug, het souvenir 'Regenboogje in een potje' van het Reisbureau, of een gouden medaille op het Guh-Circuit",
                  "the little chest at the end of the Rainbow Bridge, the Travel Agency's souvenir 'Rainbow in a Jar', or a gold medal on the Guh Circuit"),
    "hartjes": ("kisten bij een Guh-picknick en een Verrassingsfeestje", "chests at a Guh Picnic and a Surprise Party"),
    "sterren": ("de Guh-Sterrenwacht, de Sterrengrot, of maak een sterrenbeeld met de telescoop",
                "the Guh Observatory, the Star Cave, or connect a constellation with the telescope"),
    "knusmist": ("kisten in de Knuffelslaapzaal en het kistje in het Knuffelbad", "chests in the Cuddle Dormitory and the little chest in the Cuddle Bath"),
    "bloesem": ("de kist op het picknickeilandje van het Bloesemmeertje en het kistje in het theehuisje van het Klaterdal",
                "the chest on the Bloesemmeertje's picnic island and the little chest in the Klaterdal tea house"),
}
LORE = {
    "pluisjes": ("Er dwarrelen roze pluisjes in. Zacht!", "Pink fluff swirls inside. Soft!"),
    "bellen": ("Een potje vol zeepbelletjes. Niet schudden!", "A jar full of soap bubbles. Don't shake!"),
    "knabbel": ("Kaasknabbels met een wolkje erboven. Mmm.", "Cheese nibbles under a little cloud. Mmm."),
    "regenboog": ("Een stukje regenboog met een buitje erbij.", "A bit of rainbow with a little shower."),
    "hartjes": ("Hartjes! Het potje is warm als je het vasthoudt.", "Hearts! The jar is warm when you hold it."),
    "sterren": ("Een stukje nachtlucht met vallende sterretjes.", "A bit of night sky with falling stars."),
    "knusmist": ("Roze mist. Als je eraan ruikt word je slaperig.", "Pink fog. Sniff it and you get sleepy."),
    "bloesem": ("Bloesemblaadjes die nooit ophouden met waaien.", "Blossom petals that never stop blowing."),
}

for _id, (_nl, _en) in {**ALTIJD, **WEERTJES}.items():
    TEKSTEN[f"gui.guhs.weer.weertje.{_id}"] = (_nl, _en)
    TEKSTEN[f"gui.guhs.weer.weertje.{_id}.uitleg"] = UITLEG[_id]
for _id, (_nl, _en) in WEERTJES.items():
    TEKSTEN[f"item.guhs.weerpotje_{_id}"] = (f"Weerpotje: {_nl}", f"Weather Jar: {_en}")
    TEKSTEN[f"item.guhs.weerpotje_{_id}.lore"] = LORE[_id]
    TEKSTEN[f"gui.guhs.weer.weertje.{_id}.bron"] = BRON_TEKST[_id]


# =====================================================================================================================
def _voorkant(img, staat):
    """A little cloud with a sun peeking out, above the face (the hatch stays below)."""
    d = ImageDraw.Draw(img)
    zon = (255, 214, 92) if staat != "slaapt" else (190, 160, 90)
    d.rectangle([10, 1, 13, 4], fill=zon)
    d.point([(9, 2), (14, 3), (11, 0), (12, 5)], fill=zon)
    wolk = (250, 250, 255) if staat != "slaapt" else (200, 200, 214)
    d.rectangle([3, 2, 10, 3], fill=wolk)
    d.rectangle([4, 1, 7, 1], fill=wolk)
    d.rectangle([2, 3, 11, 3], fill=wolk)
    if staat != "slaapt":
        druppel = (120, 176, 240)
        d.point([(4, 4), (7, 4), (10, 4)], fill=druppel)


def potje(weertje):
    """A glass jar with a cork and its weather inside."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rand, glas, kurk, kurk_d = (150, 170, 196, 255), (214, 232, 248, 140), (186, 132, 84, 255), (140, 96, 58, 255)
    d.rectangle([5, 1, 10, 3], fill=kurk)
    d.line([5, 3, 10, 3], fill=kurk_d)
    d.rectangle([4, 4, 11, 4], fill=rand)
    d.rectangle([3, 5, 12, 14], fill=glas)
    d.line([3, 5, 3, 14], fill=rand)
    d.line([12, 5, 12, 14], fill=rand)
    d.line([3, 15, 12, 15], fill=rand)
    d.line([4, 4, 3, 5], fill=rand)
    kl = INHOUD[weertje]
    if weertje == "regenboog":
        for i, c in enumerate(kl):
            d.arc([4, 7 + i // 2, 11, 15 + i // 2], 180, 360, fill=c + (255,))
        d.point([(5, 13), (8, 12), (10, 13)], fill=(120, 176, 240, 255))
    elif weertje == "sterren":
        d.rectangle([4, 6, 11, 14], fill=kl[0] + (255,))
        d.point([(5, 7), (9, 8), (6, 11), (10, 12), (8, 13)], fill=kl[1] + (255,))
        d.line([7, 9, 9, 7], fill=kl[1] + (255,))
    elif weertje == "knabbel":
        d.rectangle([4, 6, 11, 8], fill=(246, 246, 252, 255))
        for (x, y) in ((5, 11), (8, 10), (10, 12), (6, 13), (9, 13)):
            d.rectangle([x, y, x + 1, y + 1], fill=kl[0] + (255,))
            d.point((x, y), fill=kl[1] + (255,))
    elif weertje == "hartjes":
        for (x, y) in ((5, 7), (9, 9), (5, 11), (9, 12)):
            d.point([(x, y), (x + 2, y), (x, y + 1), (x + 1, y + 1), (x + 2, y + 1), (x + 1, y + 2)], fill=kl[0] + (255,))
    elif weertje == "bellen":
        for (x, y, r) in ((6, 8, 1), (9, 10, 2), (5, 12, 1), (9, 6, 1)):
            d.ellipse([x - r, y - r, x + r, y + r], outline=kl[2] + (255,), fill=kl[1] + (200,))
    elif weertje == "bloesem":
        for (x, y) in ((5, 7), (9, 8), (6, 11), (10, 12), (7, 13)):
            d.point([(x, y), (x + 1, y)], fill=kl[0] + (255,))
            d.point((x, y + 1), fill=kl[1] + (255,))
        d.point([(4, 14), (11, 14)], fill=kl[2] + (255,))
    elif weertje == "knusmist":
        d.rectangle([4, 7, 11, 14], fill=kl[0] + (230,))
        d.line([4, 9, 11, 9], fill=kl[1] + (255,))
        d.line([5, 12, 10, 12], fill=kl[1] + (255,))
    else:  # pluisjes
        for (x, y) in ((5, 7), (9, 8), (6, 10), (10, 11), (7, 13), (4, 12)):
            d.point([(x, y), (x + 1, y), (x, y + 1)], fill=kl[0] + (255,))
            d.point((x + 1, y + 1), fill=kl[1] + (255,))
    d.point([(4, 6), (4, 7)], fill=(255, 255, 255, 200))      # a glint on the glass
    return img


def build(h):
    A, D, w = h.A, h.D, h.w
    vadskracht.machine(h, BLOK, (176, 196, 236), (255, 196, 222), voor=_voorkant)
    h.shaped(BLOK, ["WWW", "IGI", "IKI"], {"W": "minecraft:white_wool", "G": "minecraft:glass", "I": "minecraft:iron_ingot",
                                           "K": "guhs:kaas_knabbels"}, f"guhs:{BLOK}", 1)
    for wid in WEERTJES:
        n = f"weerpotje_{wid}"
        h.save(potje(wid), "item", f"{n}.png")
        h.item_model(n)
    for naam, tex in DEELTJES.items():
        w(f"{A}/particles/{naam}.json", {"textures": tex})
    for wid, kisten in BRONNEN.items():
        w(f"{D}/loot_table/weer/potje_{wid}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:random_chance", "chance": KANS}],
            "entries": [{"type": "minecraft:item", "name": f"guhs:weerpotje_{wid}"}]}]})
        for kist in kisten:
            kort = kist.split("/")[-1]
            w(f"{D}/loot_modifiers/weerpotje_{wid}__{kort}.json", {
                "type": "neoforge:add_table", "table": f"guhs:weer/potje_{wid}",
                "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": kist}]})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for p in (f"{h.A}/blockstates/{BLOK}.json", f"{h.A}/models/item/{BLOK}.json", f"{h.D}/recipe/{BLOK}.json",
              f"{h.D}/loot_table/blocks/{BLOK}.json"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    for wid, kisten in BRONNEN.items():
        for kist in kisten:
            ns, pad = kist.split(":")
            if not os.path.exists(os.path.join(h.R, "data", ns, "loot_table", pad + ".json")):
                problems.append(f"no chest table {kist}")
    for tex in sum(DEELTJES.values(), []):
        if not os.path.exists(os.path.join(h.TEX, "particle", tex.split(":")[1] + ".png")):
            problems.append(f"missing particle texture {tex}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("weer self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    """Standalone: only the Weer vadser's own files (nl_nl gets the Dutch texts, en_us the English ones; the English also goes
    into tools/lang/en/c211_weer.json so a full make_resources run keeps it)."""
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c211_weer.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    _mc26()
    print(f"weer: block, {len(WEERTJES)} weerpotjes, {sum(len(v) for v in BRONNEN.values())} chest sources, {len(TEKSTEN)} texts")


def _mc26():
    """Only the Weer vadser's items in their 26.1 shape (client item definitions; tools/mc26.py does this for the whole tree)."""
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in [BLOK] + [f"weerpotje_{wid}" for wid in WEERTJES]:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    _direct()
