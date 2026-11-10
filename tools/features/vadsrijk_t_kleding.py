"""
1.10.0 "Het Vadsrijk", T-KLEDING: the Kledingkast (wardrobe), wings, cosmetic capes, head accessories and effects.
Java: feature/vadsrijk/kleding (Cosmetica = the catalogue; ids here must match it).
Writes:
  - lang (nl_nl + en_us, real English where cheap),
  - item definitions assets/guhs/items/vr_t_kleding_<vleugel|accessoire>_<naam>.json (dye tint 0, default white),
  - models assets/guhs/models/item/vr_t_kleding/<...>.json (cuboid feathers / hats, every face tintindex 0),
  - textures textures/item/vr_t_kleding/<...>.png (16x16) and textures/vadsrijk/kleding/kaap_<naam>.png (64x32 capes).
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_kleding.py
"""
import json
import math
import os
import random

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover
    Image = None

A = os.path.join("src", "main", "resources", "assets", "guhs")
P = "vadsrijk.kleding."

# ---------------------------------------------------------------------------------------------------------------------
# texts: (nl, en)
# ---------------------------------------------------------------------------------------------------------------------
DINGEN = {
    # wings
    "vleugel.vahoegveertjes": ("Vahoegveertjes", "Vahoeg Feathers",
                               "Een cadeautje van Vahoegnir, als hij weer roze is (einde van De Schemerwortel).",
                               "A gift from Vahoegnir once he is pink again (end of De Schemerwortel)."),
    "vleugel.kristal": ("Kristalvleugels", "Crystal Wings", "Verken De Splinterpieken voor 100% (Verkenningsboek).",
                        "Explore De Splinterpieken to 100% (exploration book)."),
    "vleugel.honingbij": ("Honingbijvleugeltjes", "Honeybee Wings", "Verken De Schemerwortel voor 100% (Verkenningsboek).",
                          "Explore De Schemerwortel to 100% (exploration book)."),
    "vleugel.goudveer": ("Goudveren Vleugels", "Golden Feather Wings", "Verzamel 25 Gouden Veren (Vahoeg-bazen, premies, jachtcontracten).",
                         "Collect 25 Golden Feathers (Vahoeg bosses, bounties, hunting contracts)."),
    "vleugel.schemer": ("Schemervleugels", "Twilight Wings", "Heel soms een cadeautje van een baas in Vahoeg-vorm.",
                        "Very rarely a gift from a boss in Vahoeg form."),
    "vleugel.pluis": ("Pluisvleugeltjes", "Fluff Wings", "Een geheimpje... iets met heel hoog en heel lang zweven.",
                      "A secret... something about very high and very long gliding."),
    "vleugel.lampion": ("Lampionvlieger", "Lantern Kite", "Verken De Lampionkust voor 100% (komt later).",
                        "Explore De Lampionkust to 100% (coming later)."),
    "vleugel.sterrenstof": ("Sterrenstofvleugels", "Stardust Wings", "Een puzzelbeloning... ergens schittert iets.", "A puzzle reward... something sparkles somewhere."),
    "vleugel.poollicht": ("Poollichtvleugels", "Aurora Wings", "Een puzzelbeloning... ergens in de kou.", "A puzzle reward... somewhere in the cold."),
    "vleugel.lantaarnmot": ("Lantaarnmotvleugels", "Lantern Moth Wings", "Een puzzelbeloning... motjes houden van licht.", "A puzzle reward... moths love light."),
    # capes
    "kaap.sneeuwvlok": ("Sneeuwvlokcape", "Snowflake Cape", "Een puzzelbeloning... ergens in de sneeuw.", "A puzzle reward... somewhere in the snow."),
    "kaap.honingraat": ("Honingraatcape", "Honeycomb Cape", "Een puzzelbeloning... iets met bijtjes.", "A puzzle reward... something with bees."),
    "kaap.mos": ("Moscape", "Moss Cape", "Een puzzelbeloning... diep in het groen.", "A puzzle reward... deep in the green."),
    "kaap.knuffelorde": ("Knuffelorde-cape", "Cuddle Order Cape", "Speel het verhaal van De Knuffelzwammen uit.",
                         "Finish the story of De Knuffelzwammen."),
    "kaap.roze": ("Roze Pluiscape", "Pink Fluff Cape", "Verken De Knuffelzwammen voor 50%.", "Explore De Knuffelzwammen to 50%."),
    "kaap.sterren": ("Sterrencape", "Star Cape", "Verken De Splinterpieken voor 50%.", "Explore De Splinterpieken to 50%."),
    "kaap.bloesem": ("Bloesemcape", "Blossom Cape", "Verken De Schemerwortel voor 50%.", "Explore De Schemerwortel to 50%."),
    "kaap.reis": ("Reiscape", "Traveller's Cape", "Een geheimpje... de kleermakers van het Vadsrijk kennen elkaar.",
                  "A secret... the tailors of the Vadsrijk know each other."),
    # head accessories
    "accessoire.oortjes": ("Guh-oortjes", "Guh Ears", "Je eerste stapje in het Vadsrijk.", "Your first step into the Vadsrijk."),
    "accessoire.strikje": ("Roze strikje", "Pink Bow", "Verken De Knuffelzwammen voor 25%.", "Explore De Knuffelzwammen to 25%."),
    "accessoire.paddenhoedje": ("Paddenstoelhoedje", "Mushroom Hat", "Verken De Knuffelzwammen voor 75%.", "Explore De Knuffelzwammen to 75%."),
    "accessoire.kristaltiara": ("Kristaltiara", "Crystal Tiara", "Speel het verhaal van De Splinterpieken uit.",
                                "Finish the story of De Splinterpieken."),
    "accessoire.honingkroon": ("Honingkroontje", "Honey Crown", "Verken De Schemerwortel voor 25%.", "Explore De Schemerwortel to 25%."),
    "accessoire.feestmuts": ("Feestmutsje", "Party Hat", "Haal level 20 met een klasse.", "Reach level 20 with a class."),
    "accessoire.lampionspeld": ("Lampion-haarspeld", "Lantern Hairpin", "Verken De Lampionkust voor 25% (komt later).",
                                "Explore De Lampionkust to 25% (coming later)."),
    # name colours
    "naamkleur.roze": ("Roze naam", "Pink name", "Speel het verhaal van De Knuffelzwammen uit.", "Finish the story of De Knuffelzwammen."),
    "naamkleur.ijsblauw": ("IJsblauwe naam", "Ice-blue name", "Speel het verhaal van De Splinterpieken uit.",
                           "Finish the story of De Splinterpieken."),
    "naamkleur.honing": ("Honingkleurige naam", "Honey name", "Speel het verhaal van De Schemerwortel uit.",
                         "Finish the story of De Schemerwortel."),
    "naamkleur.paars": ("Paarse naam", "Purple name", "Vrolijk 500 wezens op.", "Cheer up 500 creatures."),
    "naamkleur.goud": ("Gouden naam", "Golden name", "Verken elk deel van het Vadsrijk voor 100%.", "Explore every part of the Vadsrijk to 100%."),
    # dyes
    "verf.roze": ("Roze", "Pink", "Heb je al!", "You have it!"),
    "verf.wit": ("Wit", "White", "Heb je al!", "You have it!"),
    "verf.zachtgeel": ("Zachtgeel", "Soft yellow", "Verken De Knuffelzwammen voor 25%.", "Explore De Knuffelzwammen to 25%."),
    "verf.mint": ("Mint", "Mint", "Verken De Knuffelzwammen voor 75%.", "Explore De Knuffelzwammen to 75%."),
    "verf.ijsblauw": ("IJsblauw", "Ice blue", "Verken De Splinterpieken voor 25%.", "Explore De Splinterpieken to 25%."),
    "verf.lila": ("Lila", "Lilac", "Verken De Splinterpieken voor 75%.", "Explore De Splinterpieken to 75%."),
    "verf.honing": ("Honing", "Honey", "Verken De Schemerwortel voor 25%.", "Explore De Schemerwortel to 25%."),
    "verf.bloesem": ("Bloesem", "Blossom", "Verken De Schemerwortel voor 75%.", "Explore De Schemerwortel to 75%."),
    # cheer-up effects
    "plof.confetti": ("Confetti", "Confetti", "Heb je al!", "You have it!"),
    "plof.regenboog": ("Regenboogje", "Little rainbow", "Verken De Knuffelzwammen voor 100%.", "Explore De Knuffelzwammen to 100%."),
    "plof.hartjes": ("Hartjesballon", "Heart balloon", "Verken De Splinterpieken voor 75%.", "Explore De Splinterpieken to 75%."),
    "plof.bloemen": ("Bloemenregen", "Flower shower", "Verken De Schemerwortel voor 25%.", "Explore De Schemerwortel to 25%."),
    "plof.knuffels": ("Knuffelregen", "Plushie shower", "Vrolijk je eerste baas in Vahoeg-vorm op.", "Cheer up your first boss in Vahoeg form."),
    "plof.giechel": ("Guh-giechel", "Guh giggle", "Vrolijk 100 wezens op.", "Cheer up 100 creatures."),
    "plof.njeg": ("\"Njeg!\"-letters", "\"Njeg!\" letters", "Vrolijk 1000 wezens op.", "Cheer up 1000 creatures."),
    # trails
    "spoor.bloemblaadjes": ("Bloemblaadjes", "Petals", "Verken De Knuffelzwammen voor 50%.", "Explore De Knuffelzwammen to 50%."),
    "spoor.sneeuwvlokjes": ("Sneeuwvlokjes", "Snowflakes", "Verken De Splinterpieken voor 50%.", "Explore De Splinterpieken to 50%."),
    "spoor.sterretjes": ("Sterretjes", "Little stars", "Verken De Splinterpieken voor 100%.", "Explore De Splinterpieken to 100%."),
    "spoor.honingdruppels": ("Honingdruppels", "Honey drops", "Verken De Schemerwortel voor 50%.", "Explore De Schemerwortel to 50%."),
    "spoor.muzieknootjes": ("Muzieknootjes", "Music notes", "Haal level 30 met een klasse.", "Reach level 30 with a class."),
    # auras
    "aura.rozegloed": ("Roze gloed", "Pink glow", "Verken De Knuffelzwammen voor 75%.", "Explore De Knuffelzwammen to 75%."),
    "aura.pluisjes": ("Ronddraaiende pluisjes", "Swirling fluff", "Verken De Splinterpieken voor 25%.", "Explore De Splinterpieken to 25%."),
    "aura.vuurvliegjes": ("Vuurvliegjes", "Fireflies", "Verken De Schemerwortel voor 75%.", "Explore De Schemerwortel to 75%."),
}

TEKSTEN = {
    "key.guhs.vr_kledingkast": ("Kledingkast", "Wardrobe"),
    "titel": ("Kledingkast", "Wardrobe"),
    "tab": ("Kledingkast", "Wardrobe"),
    "tab.rug": ("Rug", "Back"),
    "tab.hoofd": ("Hoofd", "Head"),
    "tab.harnas": ("Harnas", "Armour"),
    "tab.verf": ("Verf", "Dye"),
    "tab.naamkleur": ("Naamkleur", "Name colour"),
    "tab.titel": ("Titel", "Title"),
    "tab.plof": ("Opvrolijken", "Cheer-up"),
    "tab.spoor": ("Spoor", "Trail"),
    "tab.aura": ("Aura", "Aura"),
    "niets.rug": ("Niets op je rug", "Nothing on your back"),
    "niets.hoofd": ("Niets op je hoofd", "Nothing on your head"),
    "niets.verf": ("Geen verf (eigen kleur)", "No dye (own colour)"),
    "niets.naamkleur": ("Gewone naam", "Normal name"),
    "niets.plof": ("Alleen de roze plof", "Just the pink poof"),
    "niets.spoor": ("Geen spoor", "No trail"),
    "niets.aura": ("Geen aura", "No aura"),
    "geen_titel": ("Geen titel", "No title"),
    "geen_titels": ("Nog geen titels. Die verdien je met grote dingen!", "No titles yet. You earn them with big deeds!"),
    "aan": ("Aan", "On"),
    "kies": ("Kies", "Pick"),
    "dicht": ("Dicht", "Locked"),
    "heb": ("Je hebt dit!", "You have this!"),
    "slot": ("Zo krijg je dit: %s", "How to get it: %s"),
    "later": ("Komt later: %s", "Coming later: %s"),
    "later.kort": ("Later", "Later"),
    "rug.zweven": ("Zweven: spatie vasthouden in de lucht.", "Glide: hold space in the air."),
    "rug.nogniet": ("Zweven leer je later in het verhaal...", "You learn to glide later in the story..."),
    "harnas.uitleg": ("Elk harnas dat je ooit had, wordt een uiterlijk. Alleen in het Vadsrijk.",
                      "Every armour you ever had becomes a look. Only in the Vadsrijk."),
    "harnas.thuis": ("(Je bent thuis: je harnas-uiterlijk zie je in het Vadsrijk.)", "(You are home: armour looks show in the Vadsrijk.)"),
    # Y-COSMETICA: the Garderobe (the wardrobe at home: Vadsrijk outfits as looks in survival); English in tools/lang/en/c465
    "garderobe.titel": ("Garderobe", "Wardrobe"),
    "garderobe.thuis": ("Thuis is je Vadsrijk-kleding alleen voor de looks: geen krachten, geen harnas.",
                        "At home your Chonkdom outfits are just for looks: no powers, no armor."),
    "garderobe.leeg": ("Nog niks hier! Kleding verdien je in het Vadsrijk.", "Nothing here yet! You earn outfits in the Chonkdom."),
    "garderobe.meer": ("Meer kleding vind je later, in nieuwe delen van het Vadsrijk.", "More outfits come later, in new parts of the Chonkdom."),
    "slot.head": ("Helm", "Helmet"),
    "slot.chest": ("Borst", "Chest"),
    "slot.legs": ("Benen", "Legs"),
    "slot.feet": ("Voeten", "Feet"),
    "tm.eigen": ("Je eigen stuk", "Your own piece"),
    "tm.verborgen": ("Verborgen", "Hidden"),
    "anderen": ("Anderen: %s", "Others: %s"),
    "eigen": ("Eigen: %s", "Own: %s"),
    "anderen.tip": ("Effecten van andere spelers (sporen, aura's, opvrolijk-effecten). Minder of Uit = meer FPS.",
                    "Effects of other players (trails, auras, cheer-ups). Less or Off = more FPS."),
    "eigen.tip": ("Je eigen effecten.", "Your own effects."),
    "niveau.alles": ("Alle", "All"),
    "niveau.minder": ("Minder", "Less"),
    "niveau.uit": ("Uit", "Off"),
    "nieuw": ("Nieuw in je Kledingkast: %s (%s)! Druk op O.", "New in your wardrobe: %s (%s)! Press O."),
    "soort.vleugel": ("vleugels", "wings"),
    "soort.kaap": ("cape", "cape"),
    "soort.accessoire": ("hoofdding", "head accessory"),
    "soort.naamkleur": ("naamkleur", "name colour"),
    "soort.verf": ("verf", "dye"),
    "soort.plof": ("opvrolijk-effect", "cheer-up effect"),
    "soort.spoor": ("spoor", "trail"),
    "soort.aura": ("aura", "aura"),
    "kracht.zweven": ("Zweven: houd spatie in de lucht, dan zak je heel langzaam.", "Glide: hold space in the air and you sink slowly."),
    "kracht.dubbele_sprong": ("Dubbele sprong: tik nog eens op spatie in de lucht.", "Double jump: tap space again in the air."),
    "kracht.sneller_zweven": ("Sneller zweven: je zweeft vlotter vooruit.", "Faster glide: you glide forward quicker."),
    "kracht.langer_zweven": ("Langer zweven: je zakt nog langzamer.", "Longer glide: you sink even slower."),
    "kracht.luchtdash": ("Luchtdash: tik nog eens op spatie in de lucht voor een zwiepje vooruit.",
                         "Air dash: tap space again in the air for a little dash forward."),
    "kracht.zachte_landing": ("Zachte landing: nooit valschade, en een roze plofje als je landt.",
                              "Soft landing: never fall damage, and a pink poof when you land."),
    "kracht.alleen_rijk": ("Werkt alleen in het Vadsrijk (niet in bazen-arena's en kerkers). Thuis zie je ze wel!",
                           "Only works in the Vadsrijk (not in boss arenas and dungeons). At home they still show!"),
    "vleugel.onder.eerste": ("Houd spatie in de lucht om te zweven!", "Hold space in the air to glide!"),
    "vleugel.onder.kracht": ("%s", "%s"),
    "vahoeg.chat": ("Vahoegnir schudt zijn roze veren... en een paar dwarrelen op je rug. Je kunt nu ZWEVEN! (Kledingkast: O)",
                    "Vahoegnir shakes his pink feathers... and a few flutter onto your back. You can GLIDE now! (Wardrobe: O)"),
    "schemer.melding": ("%s kreeg de zeldzame Schemervleugels van een Vahoeg-baas!", "%s got the rare Twilight Wings from a Vahoeg boss!"),
    "npc.optie": ("Naar de Kledingkast", "To the wardrobe"),
    "tip.zweven": ("Vleugels! In de lucht spatie ingedrukt houden = zweven. Je stijgt nooit, maar je zakt heel zachtjes. In arena's en kerkers werkt het niet. Je Kledingkast: O.",
                   "Wings! Hold space in the air = glide. You never rise, but you sink very softly. Not in arenas and dungeons. Your wardrobe: O."),
    "tip.zweven.titel": ("Zweven", "Gliding"),
}


def teksten():
    nl, en = {}, {}
    for k, (n, e) in TEKSTEN.items():
        key = k if k.startswith("key.") else P + k
        nl[key], en[key] = n, e
    for k, (n, e, bn, be) in DINGEN.items():
        nl[P + "ding." + k], en[P + "ding." + k] = n, e
        nl[P + "ding." + k + ".bron"], en[P + "ding." + k + ".bron"] = bn, be
    return nl, en


# ---------------------------------------------------------------------------------------------------------------------
# models
# ---------------------------------------------------------------------------------------------------------------------
def _box(fr, to, tex="#t", rot=None, uv=None):
    """A cuboid with every face textured (uv from its own size unless given), tintindex 0."""
    x0, y0, z0 = fr
    x1, y1, z1 = to
    def cl(v):
        return max(0.0, min(16.0, v))
    fu = uv or [cl(x0), cl(16 - y1), cl(x1), cl(16 - y0)]
    su = [cl(z0), cl(16 - y1), cl(z1), cl(16 - y0)]
    tu = [cl(x0), cl(z0), cl(x1), cl(z1)]
    faces = {
        "north": {"uv": fu, "texture": tex, "tintindex": 0},
        "south": {"uv": fu, "texture": tex, "tintindex": 0},
        "east": {"uv": su, "texture": tex, "tintindex": 0},
        "west": {"uv": su, "texture": tex, "tintindex": 0},
        "up": {"uv": tu, "texture": tex, "tintindex": 0},
        "down": {"uv": tu, "texture": tex, "tintindex": 0},
    }
    for f in faces.values():
        u = f["uv"]
        if u[0] == u[2]:
            u[2] = min(16, u[0] + 0.5)
        if u[1] == u[3]:
            u[3] = min(16, u[1] + 0.5)
    e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def _plaat(x0, x1, y0, y1, z=8.0, d=0.5, tex="#t", rot=None):
    return _box([x0, y0, z - d / 2], [x1, y1, z + d / 2], tex, rot)


def vleugel_elementen(naam):
    """Wing half in item space: the hinge at (0, 8, 8), the wing reaching out along +x."""
    el = []
    if naam == "vahoegveertjes":
        el.append(_plaat(0, 11, 7, 9.5, d=1))
        for i, (ln) in enumerate([5, 6.5, 7.5, 7, 5.5]):
            x = 1 + i * 2.1
            el.append(_plaat(x, x + 2.6, 9 - ln, 8.5, d=0.6))
    elif naam == "kristal":
        el.append(_plaat(0, 10, 7.5, 9, d=1))
        for i, (ln, up) in enumerate([(5, 3), (7, 4), (6, 5), (4, 3)]):
            x = 1 + i * 2.4
            el.append(_plaat(x, x + 1.8, 8 - ln, 8 + up, d=0.8))
    elif naam == "honingbij":
        el.append(_plaat(0, 12, 8.5, 14, d=0.4))
        el.append(_plaat(0, 9, 3, 8, d=0.4))
    elif naam == "goudveer":
        el.append(_plaat(0, 13, 7, 10, d=1))
        for i, ln in enumerate([6, 7.5, 8.5, 9, 8, 6]):
            x = 1 + i * 2.1
            el.append(_plaat(x, x + 2.4, 9 - ln, 9, d=0.6))
    elif naam == "schemer":
        el.append(_plaat(0, 12, 8, 9.5, d=0.8))
        for i, ln in enumerate([6, 4, 6, 4, 5]):
            x = i * 2.4
            el.append(_plaat(x, x + 2.4, 8.5 - ln, 8.5, d=0.4))
    elif naam == "sterrenstof":
        el.append(_plaat(0, 12, 8, 9.5, d=0.6))
        for i, ln in enumerate([7, 5, 7, 5, 6]):
            x = i * 2.4
            el.append(_plaat(x, x + 2.2, 8.5 - ln, 8.5, d=0.4))
    elif naam == "poollicht":
        el.append(_plaat(0, 11, 7.5, 9, d=0.8))
        for i, (ln, up) in enumerate([(4, 5), (6, 6), (7, 5), (5, 3)]):
            x = 1 + i * 2.4
            el.append(_plaat(x, x + 2.0, 8 - ln, 8 + up, d=0.5))
    elif naam == "lantaarnmot":
        el.append(_plaat(0, 11, 8.5, 14.5, d=0.4))
        el.append(_plaat(0, 8, 2.5, 8.5, d=0.4))
        el.append(_box([9, 11, 7.6], [11, 13, 8.4]))
    elif naam == "pluis":
        el.append(_box([0, 6, 7], [5, 10, 9]))
        el.append(_box([4, 5, 7.2], [9, 9.5, 8.8]))
        el.append(_box([7.5, 4.5, 7.4], [11, 8, 8.6]))
    elif naam == "lampion":
        el.append(_plaat(0, 9, 7.5, 8.5, d=0.6))
        el.append(_plaat(3, 11, 5, 12, d=0.4))
        el.append(_box([6, 1, 7.25], [7.5, 4, 8.75]))
    return el


# The pair of wings as ONE model per pose, posed here (Java only switches models; element rotations are one axis, 22.5 deg
# steps). Item space = the BODY (like the head accessories): px (8, 0, 8) = the neck, y up, the back at z 10, front = north.
VLEUGEL_POSES = {"dicht": ("y", 45.0), "open": ("y", 22.5), "op": ("z", 22.5)}
VL_SCHAAL, VL_Y, VL_Z = 0.85, -3.0, 10.6


def vleugel_paar(naam, pose):
    as_, hoek = VLEUGEL_POSES[pose]
    uit = []
    for kant in (1, -1):   # +1 = the player's left (+x), -1 = right
        for e in vleugel_elementen(naam):
            (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
            xa, xb = 8 + kant * (1 + x0 * VL_SCHAAL), 8 + kant * (1 + x1 * VL_SCHAAL)
            fr = [round(min(xa, xb), 3), round((y0 - 8) * VL_SCHAAL + VL_Y, 3), round((z0 - 8) * VL_SCHAAL + VL_Z, 3)]
            to = [round(max(xa, xb), 3), round((y1 - 8) * VL_SCHAAL + VL_Y, 3), round((z1 - 8) * VL_SCHAAL + VL_Z, 3)]
            # "y": tips swing back (+z) on both sides; "z": tips up on both sides
            hk = -kant * hoek if as_ == "y" else kant * hoek
            rot = {"angle": hk, "axis": as_, "origin": [8 + kant, VL_Y, VL_Z]}
            uit.append(_box(fr, to, rot=rot))
    return uit


def accessoire_elementen(naam):
    """Head accessory in item space: the head is px x 4..12, y 0..8, z 4..12 (front = north, z 4)."""
    el = []
    if naam == "oortjes":
        el.append(_box([3.8, 8, 7], [12.2, 8.6, 8]))
        el.append(_box([3.8, 3, 7], [4.4, 8, 8]))
        el.append(_box([11.6, 3, 7], [12.2, 8, 8]))
        el.append(_box([4, 8.4, 7], [6.6, 11.6, 8]))
        el.append(_box([9.4, 8.4, 7], [12, 11.6, 8]))
    elif naam == "strikje":
        el.append(_box([7, 8, 5.5], [9, 9.6, 7]))
        el.append(_box([4.4, 8, 5.5], [7, 10.4, 7]))
        el.append(_box([9, 8, 5.5], [11.6, 10.4, 7]))
    elif naam == "paddenhoedje":
        el.append(_box([3, 8, 3], [13, 10, 13]))
        el.append(_box([4.5, 10, 4.5], [11.5, 11.6, 11.5]))
        el.append(_box([6, 11.6, 6], [10, 12.4, 10]))
    elif naam == "kristaltiara":
        el.append(_box([3.9, 7, 3.6], [12.1, 7.8, 4.1]))
        el.append(_box([3.6, 7, 3.6], [4.1, 7.8, 12.1]))
        el.append(_box([11.9, 7, 3.6], [12.4, 7.8, 12.1]))
        el.append(_box([7, 7.8, 3.6], [9, 11, 4.1]))
        el.append(_box([5, 7.8, 3.6], [6.2, 9.6, 4.1]))
        el.append(_box([9.8, 7.8, 3.6], [11, 9.6, 4.1]))
    elif naam == "honingkroon":
        el.append(_box([3.8, 8, 3.8], [12.2, 9.4, 4.4]))
        el.append(_box([3.8, 8, 11.6], [12.2, 9.4, 12.2]))
        el.append(_box([3.8, 8, 4.4], [4.4, 9.4, 11.6]))
        el.append(_box([11.6, 8, 4.4], [12.2, 9.4, 11.6]))
        for x in (4.2, 7.2, 10.2):
            el.append(_box([x, 9.4, 3.8], [x + 1.6, 11, 4.4]))
    elif naam == "feestmuts":
        el.append(_box([5, 8, 5], [11, 9.5, 11]))
        el.append(_box([6, 9.5, 6], [10, 11, 10]))
        el.append(_box([7, 11, 7], [9, 12.5, 9]))
        el.append(_box([7.25, 12.5, 7.25], [8.75, 14, 8.75]))
    elif naam == "lampionspeld":
        el.append(_box([11.8, 5, 6.5], [13.2, 8.2, 8]))
        el.append(_box([12.2, 8.2, 6.9], [12.8, 9.2, 7.6]))
    return el


VLEUGELS = ["vahoegveertjes", "kristal", "honingbij", "goudveer", "schemer", "pluis", "lampion", "sterrenstof", "poollicht", "lantaarnmot"]
ACCESSOIRES = ["oortjes", "strikje", "paddenhoedje", "kristaltiara", "honingkroon", "feestmuts", "lampionspeld"]
KAPEN = ["knuffelorde", "roze", "sterren", "bloesem", "reis", "sneeuwvlok", "honingraat", "mos"]


# ---------------------------------------------------------------------------------------------------------------------
# textures (16x16 item textures, 64x32 capes): light colours, so the dye tint reads well
# ---------------------------------------------------------------------------------------------------------------------
def _rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def tex_vleugel(naam):
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    rnd = random.Random(naam)
    for y in range(16):
        for x in range(16):
            if naam == "vahoegveertjes":
                c = 0xFFB8DC if (x + y // 3) % 4 else 0xFF8CC8
                if y > 12:
                    c = 0xFFE4F2
            elif naam == "kristal":
                c = 0xC8ECFF if (x + y) % 5 else 0xFFFFFF
                if (x - y) % 7 == 0:
                    c = 0x9AD8FF
            elif naam == "honingbij":
                c = 0xFFF4C8 if (x // 2 + y // 2) % 3 else 0xFFE08A
                if x in (0, 15) or y in (0, 15) or (x + y) % 6 == 0:
                    c = 0xC89A3A
            elif naam == "goudveer":
                c = 0xFFE07A if (x + y // 2) % 3 else 0xFFC83A
                if y < 2:
                    c = 0xFFF4C0
            elif naam == "schemer":
                c = 0x5A4A9A if (x + y) % 4 else 0x7A64C8
                if rnd.random() < 0.06:
                    c = 0xFFF4A0
            elif naam == "pluis":
                c = 0xFFFFFF if rnd.random() < 0.7 else 0xF0F0FA
            elif naam == "sterrenstof":
                c = 0x3A3A8A if (x + y) % 5 else 0x5A5AC0
                if rnd.random() < 0.12:
                    c = 0xFFFFFF if rnd.random() < 0.5 else 0xFFF0A0
            elif naam == "poollicht":
                c = [0x8CFFC8, 0x8CDCFF, 0xC8A8FF, 0xFF9FD0][(y // 4 + x // 6) % 4]
            elif naam == "lantaarnmot":
                c = 0xF0E0C0 if (x + y) % 4 else 0xD8C8A0
                if 5 <= x <= 9 and 5 <= y <= 9:
                    c = 0xFFD870
            else:  # lampion
                c = 0xFF9A5A if (y // 4) % 2 else 0xFFC870
                if x in (0, 15):
                    c = 0xC85A3A
            px[x, y] = _rgb(c)
    return im


def tex_accessoire(naam):
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            if naam == "oortjes":
                c = 0xFFC8E4 if 3 < x < 12 and y < 10 else 0xFF8CC8
            elif naam == "strikje":
                c = 0xFF6FB5 if (x + y) % 5 else 0xFFB8DC
            elif naam == "paddenhoedje":
                c = 0xE84A4A
                if (x * 7 + y * 3) % 11 == 0 or (x * 5 + y * 9) % 13 == 0:
                    c = 0xFFFFFF
            elif naam == "kristaltiara":
                c = 0xE8E8F4 if y > 10 else (0xA8DCFF if (x + y) % 3 else 0xFFFFFF)
            elif naam == "honingkroon":
                c = 0xFFD24A if (x + y) % 4 else 0xFFF0A0
            elif naam == "feestmuts":
                c = 0x8CDCFF if (x + y) % 6 < 3 else 0xFF8CC8
                if y < 3:
                    c = 0xFFFFFF
            else:  # lampionspeld
                c = 0xFF9A5A if y % 4 else 0xC85A3A
            px[x, y] = _rgb(c)
    return im


def tex_kaap(naam):
    """64x32, vanilla cape layout: the outside at (1,1)-(11,17), the inside at (12,1)-(22,17), edges around."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    basis = {"knuffelorde": 0xFF7FC0, "roze": 0xFFB8DC, "sterren": 0x2A2A6A, "bloesem": 0xFFF4FA, "reis": 0x4A8A8A,
             "sneeuwvlok": 0xA8D8FF, "honingraat": 0xFFC83A, "mos": 0x5A8A3A}[naam]
    rand = {"knuffelorde": 0xC0287C, "roze": 0xFFFFFF, "sterren": 0xFFD24A, "bloesem": 0xFF8CC8, "reis": 0x8A5A3A,
            "sneeuwvlok": 0xFFFFFF, "honingraat": 0xB07800, "mos": 0x3A5A2A}[naam]
    d.rectangle([0, 0, 22, 17], fill=_rgb(basis))
    d.rectangle([12, 1, 21, 16], fill=_rgb(rand))   # inside
    for x in range(1, 11):
        im.putpixel((x, 16), _rgb(rand))
    for y in range(1, 17):
        im.putpixel((1, y), _rgb(rand))
        im.putpixel((10, y), _rgb(rand))
    if naam == "knuffelorde":
        # a white shield with a pink heart
        d.rectangle([3, 4, 8, 10], fill=_rgb(0xFFFFFF))
        d.polygon([(3, 10), (8, 10), (5.5, 13)], fill=_rgb(0xFFFFFF))
        for (x, y) in [(4, 6), (5, 6), (6, 6), (7, 6), (4, 5), (7, 5), (5, 7), (6, 7), (5, 8), (6, 8), (4, 7), (7, 7)]:
            im.putpixel((x, y), _rgb(0xFF4FA8))
    elif naam == "roze":
        for y in range(1, 17):
            for x in range(2, 10):
                t = y / 16
                c = (255, int(184 + 40 * t), int(220 + 20 * t), 255)
                im.putpixel((x, y), c)
        for x in range(1, 11, 2):
            im.putpixel((x, 15), _rgb(0xFFFFFF))
    elif naam == "sterren":
        for (x, y) in [(3, 3), (7, 5), (4, 9), (8, 11), (5, 14), (2, 12), (8, 2)]:
            im.putpixel((x, y), _rgb(0xFFF4A0))
        im.putpixel((5, 6), _rgb(0xFFFFFF))
    elif naam == "bloesem":
        for (cx, cy) in [(4, 4), (7, 9), (3, 13)]:
            for (dx, dy) in [(0, -1), (1, 0), (0, 1), (-1, 0)]:
                im.putpixel((cx + dx, cy + dy), _rgb(0xFF8CC8))
            im.putpixel((cx, cy), _rgb(0xFFE04A))
    elif naam == "sneeuwvlok":
        for (cx, cy) in [(5, 5), (4, 11)]:
            for d in range(-2, 3):
                im.putpixel((cx + d, cy), _rgb(0xFFFFFF))
                im.putpixel((cx, cy + d), _rgb(0xFFFFFF))
    elif naam == "honingraat":
        for y in range(1, 17):
            for x in range(2, 10):
                if (x + (y // 3) % 2 * 2) % 4 == 0 or y % 3 == 0:
                    im.putpixel((x, y), _rgb(0xE0A020))
    elif naam == "mos":
        rnd = random.Random("mos")
        for y in range(1, 17):
            for x in range(2, 10):
                if rnd.random() < 0.35:
                    im.putpixel((x, y), _rgb(0x7AAA4A if rnd.random() < 0.6 else 0x4A6A2A))
    elif naam == "reis":
        for y in range(1, 17):
            for x in range(2, 10):
                if (x // 4 + y // 4) % 2:
                    im.putpixel((x, y), _rgb(0x3A6A7A))
        for (x, y) in [(5, 5), (6, 5), (5, 6), (6, 6), (5, 4), (6, 7), (4, 5), (7, 6)]:
            im.putpixel((x, y), _rgb(0xFFD24A))
    return im


# ---------------------------------------------------------------------------------------------------------------------
# writing
# ---------------------------------------------------------------------------------------------------------------------
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _model(soort, naam, elementen, root, tex=None):
    tex = f"guhs:item/vr_t_kleding/{tex or soort + '_' + naam}"
    _schrijf_json(os.path.join(root, "models", "item", "vr_t_kleding", f"{soort}_{naam}.json"),
                  {"textures": {"t": tex, "particle": tex}, "elements": elementen})
    _schrijf_json(os.path.join(root, "items", f"vr_t_kleding_{soort}_{naam}.json"),
                  {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_t_kleding/{soort}_{naam}",
                             "tints": [{"type": "minecraft:dye", "default": -1}]}})


def assets(root=A):
    for v in VLEUGELS:
        for pose in VLEUGEL_POSES:
            _model("vleugel", f"{v}_{pose}", vleugel_paar(v, pose), root, tex=f"vleugel_{v}")
    for a in ACCESSOIRES:
        _model("accessoire", a, accessoire_elementen(a), root)
    if Image is None:
        print("vadsrijk_t_kleding: no PIL, textures skipped")
        return
    td = os.path.join(root, "textures", "item", "vr_t_kleding")
    os.makedirs(td, exist_ok=True)
    for v in VLEUGELS:
        tex_vleugel(v).save(os.path.join(td, f"vleugel_{v}.png"))
    for a in ACCESSOIRES:
        tex_accessoire(a).save(os.path.join(td, f"accessoire_{a}.png"))
    kd = os.path.join(root, "textures", "vadsrijk", "kleding")
    os.makedirs(kd, exist_ok=True)
    for k in KAPEN:
        tex_kaap(k).save(os.path.join(kd, f"kaap_{k}.png"))


def build(h):
    nl, en = teksten()
    for k in nl:
        h.lang(k, en[k], nl[k])   # (T-INT: make_v2.lang(key, en, nl))
    assets()


def _direct():
    nl, en = teksten()
    for taal, t in (("nl_nl", nl), ("en_us", en)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        _schrijf_json(p, data)
    assets()
    print(f"vadsrijk_t_kleding: {len(nl)} texts, {len(VLEUGELS)} wings, {len(ACCESSOIRES)} accessories, {len(KAPEN)} capes")


if __name__ == "__main__":
    _direct()
