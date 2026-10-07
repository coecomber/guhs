"""
bbq2 (guhrio-beloning): everything of Super Guhrio that is not a level. Java: feature/guhriobeloning.

  this module               the slots it fills in the castle (VOORPLEIN, TORENKAMER: guhrio_kasteel.py calls them while
                            guhrio.build runs, so before build(h) here), the outfits (BONES / CLOTHES / clothes / icons),
                            the block models, every text, the questline "guhrio_beloning" with its advancements, the FTB
                            section, the test room
  guhrio_beloning_bouw      what stands on the forecourt and in the tower room
  guhrio_beloning_tex       block textures and the painters of the outfits
  guhrio_beloning_modellen  the models of Pad-guh and Prinses Perzikguh
  guhrio_beloning_wiki      the wiki entries (not in FEATURES)

  blocks   guhriobeloning_vraagblok (one kaasknabbel per day per player), guhriobeloning_vlaggenmast (deel voet / paal /
           top / los, facing), guhriobeloning_pijp (kleur = a dye colour, mond: the working green pipe),
           guhriobeloning_scorebord (facing; the castle's highscore board)
  outfits  guhriobeloning_rode_pet (+ moustache), _groene_pet, _schild (Pad-guh's shop), _prinsessenkroon (the princess),
           _gouden_pet (all 18 big vadsmunten); clothes source guhrio_beloning
  NPCs     padguh, perzikguh (their kinds exist since the skelet); the Guhshi of the tower room is a story copy
"""
import os
import re

FTB_SECTIES = [("guhrio_beloning", "Pad-guh's kraam & de beloningen", "npc:padguh", None)]

# what Pad-guh asks (the same numbers as feature/guhriobeloning/Winkel.java: selfcheck compares them)
PRIJZEN = {"rode_pet": 30, "groene_pet": 30, "schild": 40, "vraagblok": 20, "vlaggenmast": 10, "pijp": 25}
BLOKKEN = ("guhriobeloning_vraagblok", "guhriobeloning_vlaggenmast", "guhriobeloning_pijp", "guhriobeloning_scorebord")
VERBORGEN = ("guhrio_beloning_padguh", "guhrio_beloning_gekocht", "guhrio_beloning_vraagblok", "guhrio_beloning_pijp", "guhrio_beloning_bord",
             "guhrio_beloning_kroon", "guhrio_beloning_guhshi", "guhrio_beloning_tong", "guhrio_beloning_gouden_pet")
TESTKAMER = "guhriobeloning_test_kamer"
# where the three of the castle stand, in the coordinates of the whole build (filled when the castle is built)
GEBOUWD = {}


# =====================================================================================================================
# the slots of the castle (called by guhrio_kasteel while guhrio.build runs)
# =====================================================================================================================
def _onthoud(plek, naam):
    from features import guhrio_beloning_bouw as bouw
    for wie, (waar, cel) in bouw.PLEKKEN.items():
        if waar == naam:
            GEBOUWD[wie] = plek.bouw(*cel)


def bouw_voorplein(plek):
    from features import guhrio_beloning_bouw as bouw
    bouw.voorplein(plek)
    _onthoud(plek, "voorplein")
    GEBOUWD["pijpen"] = [plek.bouw(*p) for p in bouw.PIJPEN]
    GEBOUWD["scorebord"] = plek.bouw(*bouw.SCOREBORD)


def bouw_torenkamer(plek):
    from features import guhrio_beloning_bouw as bouw
    bouw.torenkamer(plek)
    _onthoud(plek, "torenkamer")


VOORPLEIN = bouw_voorplein
TORENKAMER = bouw_torenkamer

# =====================================================================================================================
# the outfits (make_guh_variants: BONES / clothes; make_clothes_icons: icons; make_resources: CLOTHES item models)
# =====================================================================================================================
_H, _B = [0, 6, -2], [0, 6, 6]
BONES = {
    # the cap: a dome with a button on top, a peak, a white badge on the front (red, green and golden cap share the bones)
    "outfit_guhriobeloning_pet": ("head", _H, "guhriobeloning_pet", [([-5.6, 15.0, -10.6], [11.2, 2.3, 8.8], 0), ([-4.6, 17.3, -9.8], [9.2, 1.4, 7.0], 0),
                                                                      ([-0.8, 18.7, -7.1], [1.6, 0.5, 1.6], 0)]),
    "outfit_guhriobeloning_pet_klep": ("head", _H, "guhriobeloning_pet_klep", [([-5.0, 15.0, -13.9], [10.0, 0.7, 3.5], 0)]),
    "outfit_guhriobeloning_pet_embleem": ("head", _H, "guhriobeloning_embleem", [([-2.0, 15.1, -10.95], [4.0, 3.3, 0.4], 0)]),
    # the moustache (the red cap only): two bushy halves under the nose, the tips curl up
    "outfit_guhriobeloning_snor": ("head", _H, "guhriobeloning_snor", [([-5.6, 3.4, -13.4], [5.3, 2.1, 0.9], 0), ([0.3, 3.4, -13.4], [5.3, 2.1, 0.9], 0),
                                                                       ([-6.9, 4.4, -13.2], [1.5, 1.9, 0.8], 0), ([5.4, 4.4, -13.2], [1.5, 1.9, 0.8], 0)]),
    # the princess crown: a little band with four points and a taller one in front, a ruby and two sapphires
    "outfit_guhriobeloning_kroon": ("head", _H, "guhriobeloning_kroon", [([-3.0, 15.0, -8.6], [6.0, 1.4, 5.0], 0)]
                                    + [([x, 16.4, z], [1.2, 1.3, 1.2], 0) for x in (-3.0, 1.8) for z in (-8.6, -4.8)]
                                    + [([-0.7, 16.4, -8.6], [1.4, 2.2, 1.2], 0)]),
    "outfit_guhriobeloning_kroon_robijn": ("head", _H, "guhriobeloning_juweel_rood", [([-0.6, 15.25, -8.9], [1.2, 1.0, 0.4], 0)]),
    "outfit_guhriobeloning_kroon_saffier": ("head", _H, "guhriobeloning_juweel_blauw", [([-3.3, 15.25, -6.7], [0.4, 1.0, 1.2], 0),
                                                                                       ([2.9, 15.25, -6.7], [0.4, 1.0, 1.2], 0)]),
    # the turtle shell on the back: three layers of green plates on a white rim
    "outfit_guhriobeloning_schild": ("body", _B, "guhriobeloning_schild", [([-5.6, 11.4, 0.4], [11.2, 1.7, 10.4], 0), ([-4.6, 13.1, 1.4], [9.2, 1.6, 8.4], 0),
                                                                           ([-3.0, 14.7, 3.0], [6.0, 1.0, 5.2], 0)]),
    "outfit_guhriobeloning_schild_rand": ("body", _B, "guhriobeloning_schild_rand", [([-6.3, 10.5, -0.3], [12.6, 1.0, 11.8], 0)]),
}
CLOTHES = ["guhriobeloning_rode_pet", "guhriobeloning_groene_pet", "guhriobeloning_schild", "guhriobeloning_prinsessenkroon",
           "guhriobeloning_gouden_pet"]


def clothes(rng, v):
    from features import guhrio_beloning_tex
    return guhrio_beloning_tex.kleding(rng, v)


def icons(ic):
    from features import guhrio_beloning_tex
    return guhrio_beloning_tex.iconen(ic)


# =====================================================================================================================
# textures, models, blockstates, loot, tags
# =====================================================================================================================
def textures(h):
    from PIL import Image
    from features import guhrio_beloning_tex as tex
    # (the engine's three green pipe pictures were written by guhrio.build just before)
    groen = [Image.open(os.path.join(h.TEX, "block", f"{n}.png")).convert("RGBA") for n in ("guhrio_pijp", "guhrio_pijp_lijf", "guhrio_pijp_boven")]
    tex.blokken(h, *groen)


def _zijden(tex, uv):
    return {f: {"texture": tex, "uv": uv} for f in ("north", "south", "west", "east")}


def mast_modellen(h):
    """The flagpole's four parts. The flag hangs to the east of a pole that looks north (the blockstate turns it)."""
    A = h.A
    T = {"paal": "guhs:block/guhriobeloning_mast", "vlag": "guhs:block/guhriobeloning_vlag", "voet": "guhs:block/guhrio_blok",
         "particle": "guhs:block/guhriobeloning_mast"}

    def paal(y0, y1):
        return {"from": [6.5, y0, 6.5], "to": [9.5, y1, 9.5],
                "faces": {**_zijden("#paal", [0, 16 - y1, 4, 16 - y0]), "up": {"texture": "#paal", "uv": [0, 0, 4, 4]},
                          "down": {"texture": "#paal", "uv": [0, 0, 4, 4]}}}

    voet = {"from": [3, 0, 3], "to": [13, 4, 13],
            "faces": {**_zijden("#voet", [3, 12, 13, 16]), "up": {"texture": "#voet", "uv": [3, 3, 13, 13]},
                      "down": {"texture": "#voet", "uv": [3, 3, 13, 13]}}}
    bal = {"from": [5.5, 11, 5.5], "to": [10.5, 16, 10.5],
           "faces": {f: {"texture": "#vlag", "uv": [8, 0, 16, 8]} for f in ("north", "south", "west", "east", "up", "down")}}
    vlag = {"from": [9.5, 2.5, 7.6], "to": [16, 10.5, 8.4],
            "faces": {"south": {"texture": "#vlag", "uv": [0, 0, 7, 8]}, "north": {"texture": "#vlag", "uv": [7, 0, 0, 8]},
                      "east": {"texture": "#vlag", "uv": [6, 0, 7, 8]}, "up": {"texture": "#vlag", "uv": [0, 0, 7, 1]},
                      "down": {"texture": "#vlag", "uv": [0, 7, 7, 8]}}}
    delen = {"voet": [voet, paal(4, 16)], "paal": [paal(0, 16)], "top": [paal(0, 11), bal, vlag], "los": [voet, paal(4, 11), bal, vlag]}
    for deel, elements in delen.items():
        h.w(f"{A}/models/block/guhriobeloning_vlaggenmast_{deel}.json", {"parent": "minecraft:block/block", "textures": T, "elements": elements})
    h.w(f"{A}/blockstates/guhriobeloning_vlaggenmast.json", {"variants": {
        f"deel={deel},facing={f}": {"model": f"guhs:block/guhriobeloning_vlaggenmast_{deel}", **({"y": y} if y else {})}
        for deel in delen for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    h.w(f"{A}/models/item/guhriobeloning_vlaggenmast.json", {"parent": "guhs:block/guhriobeloning_vlaggenmast_los"})


def pijp_modellen(h):
    """The pipe in sixteen colours: a mouth (a body with a wide rim on top) and a body."""
    from features import guhrio_beloning_tex as tex
    A = h.A
    variants = {}
    for kleur in tex.KLEUREN:
        T = {"mond": tex.pijp_textuur(kleur, ""), "lijf": tex.pijp_textuur(kleur, "lijf"), "boven": tex.pijp_textuur(kleur, "boven")}
        T["particle"] = T["lijf"]
        mond = [{"from": [1, 0, 1], "to": [15, 10, 15],
                 "faces": {**_zijden("#lijf", [1, 6, 15, 16]), "down": {"texture": "#boven", "uv": [1, 1, 15, 15]}}},
                {"from": [0, 10, 0], "to": [16, 16, 16],
                 "faces": {**_zijden("#mond", [0, 0, 16, 6]), "up": {"texture": "#boven", "uv": [0, 0, 16, 16]},
                           "down": {"texture": "#mond", "uv": [0, 0, 16, 16]}}}]
        lijf = [{"from": [1, 0, 1], "to": [15, 16, 15],
                 "faces": {**_zijden("#lijf", [1, 0, 15, 16]), "up": {"texture": "#boven", "uv": [1, 1, 15, 15]},
                           "down": {"texture": "#boven", "uv": [1, 1, 15, 15]}}}]
        for naam, elements in ((f"guhriobeloning_pijp_{kleur}", mond), (f"guhriobeloning_pijp_lijf_{kleur}", lijf)):
            h.w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": T, "elements": elements})
        variants[f"kleur={kleur},mond=true"] = {"model": f"guhs:block/guhriobeloning_pijp_{kleur}"}
        variants[f"kleur={kleur},mond=false"] = {"model": f"guhs:block/guhriobeloning_pijp_lijf_{kleur}"}
    h.w(f"{A}/blockstates/guhriobeloning_pijp.json", {"variants": variants})
    h.w(f"{A}/models/item/guhriobeloning_pijp.json", {"parent": "guhs:block/guhriobeloning_pijp_green"})


def blocks_and_items(h):
    A = h.A
    # the ?-block to build with: the engine's own ?-block picture on a plain cube
    h.simple_block("guhriobeloning_vraagblok", "guhs:block/guhrio_vraagblok")
    mast_modellen(h)
    pijp_modellen(h)
    h.w(f"{A}/models/block/guhriobeloning_scorebord.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": "guhs:block/guhriobeloning_scorebord_boven", "front": "guhs:block/guhriobeloning_scorebord_voor",
        "side": "guhs:block/guhriobeloning_scorebord_zij"}})
    h.w(f"{A}/blockstates/guhriobeloning_scorebord.json", {"variants": h.facing_states("guhriobeloning_scorebord")})
    h.w(f"{A}/models/item/guhriobeloning_scorebord.json", {"parent": "guhs:block/guhriobeloning_scorebord"})
    for name in BLOKKEN:
        h.self_drop(name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in BLOKKEN])


# =====================================================================================================================
# texts
# =====================================================================================================================
G = "gui.guhs.guhriobeloning."
Q = "quest.guhs.guhriobeloning."
TEXTS = {
    "block.guhs.guhriobeloning_vraagblok": "Knabbel-vraagtekenblok",
    "block.guhs.guhriobeloning_vraagblok.lore": "Spring er met je hoofd tegenaan (of klik): elke dag één kaasknabbel per speler. Njam-njeg!",
    "block.guhs.guhriobeloning_vlaggenmast": "Kasteelvlaggenmast",
    "block.guhs.guhriobeloning_vlaggenmast.lore": "Stapel ze op tot een mast: onderaan komt de voet, bovenaan het vlaggetje. Klik voor een feestje.",
    "block.guhs.guhriobeloning_pijp": "Groene reispijp",
    "block.guhs.guhriobeloning_pijp.lore": "Ga erop staan en sluip: je komt eruit bij de dichtstbijzijnde pijp van dezelfde kleur, tot 50 blokken ver. "
                                           "Met verf maak je een ander paar.",
    "block.guhs.guhriobeloning_scorebord": "Highscorebord van het kasteel",
    "block.guhs.guhriobeloning_scorebord.lore": "Laat de snelste tijden van Super Guhrio zien. Klik voor je eigen tijden.",
    "item.guhs.guhriobeloning_rode_pet": "Rode Guhrio-pet met snor",
    "item.guhs.guhriobeloning_groene_pet": "Groene Luiguh-pet",
    "item.guhs.guhriobeloning_schild": "Schild-Mika-schild",
    "item.guhs.guhriobeloning_prinsessenkroon": "Kroontje van Prinses Perzikguh",
    "item.guhs.guhriobeloning_gouden_pet": "Gouden vadspet",
    "entity.guhs.guh_npc.padguh": "Pad-guh",
    "entity.guhs.guh_npc.perzikguh": "Prinses Perzikguh",
    "gui.guhs.kledingbron.guhrio_beloning": "Super Guhrio (Pad-guh's kraam en de torenkamer)",
    G + "prijs.munten": "%s Guhrio-munten bij Pad-guh",
    G + "prijs.kroon": "van Prinses Perzikguh, na het duel",
    G + "prijs.gouden_pet": "alle 18 grote vadsmunten (Pad-guh)",
    # Pad-guh
    Q + "padguh.intro.1": "Njeg! Een held! Ik ben Pad-guh. De Grote Nether-Mika heeft Prinses Perzikguh meegenomen. 'Voor een stukje taart', zei hij. "
                          "Een STUKJE? Het was de hele taart, vads!",
    Q + "padguh.intro.2": "Door de poort kom je in de levelhal: zes levels, van opzij bekeken. Niemand doet je pijn, Mika's duwen alleen maar. Wie "
                          "valt, staat weer bij zijn vlaggetje.",
    Q + "padguh.intro.3": "De munten uit de levels geef je bij mij uit: outfits en bouwblokken. En zoek in elk level de drie grote vadsmunten. Voor "
                          "alle achttien heb ik iets heel glimmends, vahoeg!",
    Q + "padguh.hallo": "Njeg, daar ben je weer! Je hebt %s munten in je buidel. Waar heb je zin in?",
    Q + "padguh.gag": "Bedankt! Maar de prinses is in een ander kasteeldeel, njeg. (Wel knap hoor: %s helemaal uitgespeeld. Vahoeg!)",
    Q + "padguh.na_duel": "Je hebt de prinses gevonden! En, zat ze gewoon taart te eten? Dacht ik al, njeg. Bedankt voor alles, held. De kraam blijft "
                          "open!",
    Q + "padguh.gouden_pet": "ACHTTIEN grote vadsmunten! Super vahoeg! Hier is hij dan: de gouden vadspet. Alleen voor de allergrootste verzamelaars, "
                             "njeg.",
    Q + "padguh.outfits": "Outfits! Vers uit de paddenstoel. Je hebt %s munten. Wat mag het zijn?",
    Q + "padguh.blokken": "Bouwblokken, voor thuis. Echt uit het kasteel! Je hebt %s munten. Wat mag het zijn?",
    Q + "padguh.gekocht": "Alsjeblieft: %s! Veel plezier ermee, njeg. Je hebt nog %s munten.",
    Q + "padguh.tekort": "Njeg... je komt %s munten tekort (je hebt er %s). Haal er nog wat in de levels: bij elke vlaggenmast krijg je ook fooi!",
    Q + "padguh.vadsmunten": "Jouw grote vadsmunten. 1-1: %s  1-2: %s  2-1: %s  2-2: %s  3-1: %s  3-2: %s. Samen %s van de 18, vads!",
    G + "optie.outfits": "Outfits kopen",
    G + "optie.blokken": "Bouwblokken kopen",
    G + "optie.vadsmunten": "Mijn grote vadsmunten",
    G + "waar.rode_pet": f"Rode pet met snor ({PRIJZEN['rode_pet']} munten)",
    G + "waar.groene_pet": f"Groene pet ({PRIJZEN['groene_pet']} munten)",
    G + "waar.schild": f"Schild voor op de rug ({PRIJZEN['schild']} munten)",
    G + "waar.vraagblok": f"Knabbel-vraagtekenblok ({PRIJZEN['vraagblok']} munten)",
    G + "waar.vlaggenmast": f"Vlaggenmast, 4 stuks ({PRIJZEN['vlaggenmast']} munten)",
    G + "waar.pijp": f"Groene reispijp, 2 stuks ({PRIJZEN['pijp']} munten)",
    G + "wereld.1": "de binnentuin",
    G + "wereld.2": "de kelders",
    G + "wereld.3": "de burcht",
    G + "fooi": "Pad-guh stopt je %s munten fooi toe. Je hebt er nu %s in je buidel, njeg!",
    # Prinses Perzikguh
    Q + "perzik.wacht": "Sst! Niet doorvertellen: ik ben helemaal niet ontvoerd. De taart hier is gewoon erg lekker, njeg. Maar de Grote Nether-Mika "
                        "speelt zo graag ridder... Haal eerst alle levels!",
    Q + "perzik.bijna": "Alle levels gehaald, knap! Nu nog het duel. Hij duwt alleen maar, hoor. Daarna heb ik iets moois voor je, njeg.",
    Q + "perzik.dank.1": "Mijn held! Je hebt het duel gewonnen. De Grote Nether-Mika zit nu te mokken in de saus. Dat gaat wel over: hij krijgt "
                         "straks gewoon taart.",
    Q + "perzik.dank.2": "Hier, mijn kroontje. Voor jouw guh, als dank. En neem een taart mee. Hij had gewoon trek, snap je? Njeg.",
    Q + "perzik.dank.3": "En kijk eens wie er met je mee wil! Guhshi kwispelt al de hele tijd. Klik maar op hem: hij is van jou. Vahoeg!",
    Q + "perzik.dank.3_getemd": "En Guhshi heb je al bij je, zie ik. Zorg goed voor hem: hij lust knabbels van heel ver weg. Vahoeg!",
    Q + "perzik.kroon_kwijt": "Ben je je kroontje kwijt? Njeg, helden... Hier heb je een nieuwe. Deze keer meteen opzetten!",
    Q + "perzik.dag": "Dag held! Guhshi staat daar nog op je te wachten. Klik maar op hem, njeg.",
    Q + "perzik.dag_guhshi": "Dag held! Hoe gaat het met Guhshi? Gooi hem een knabbel toe van mij: dat vindt hij het leukst, njeg.",
    # Guhshi
    Q + "guhshi.vraag": "Guhshi kijkt je aan en kwispelt. Njeg? Mag hij met je mee?",
    Q + "guhshi.ja": "Ja! Kom maar mee, Guhshi",
    Q + "guhshi.nee": "Nu even niet",
    Q + "guhshi.wacht": "Guhshi snuffelt aan je. Hij gaat pas mee met wie het duel met de Grote Nether-Mika heeft gewonnen, njeg.",
    Q + "guhshi.wacht_ei": "Guhshi herkent je van zijn ei! Win het duel met de Grote Nether-Mika, dan gaat hij met je mee. Njeg njeg!",
    Q + "guhshi.al_getemd": "Njam-njeg! Deze Guhshi blijft bij de prinses. Die van jou heb je al.",
    G + "guhshi.vrij": "Guhshi wil met je mee! Hij wacht bij Prinses Perzikguh in de torenkamer: klik op hem.",
    G + "guhshi.gelukt": "Guhshi is van jou! Hij draagt zijn zadel al: stap op, spring, en houd spatie vast om te fladderen. Vahoeg!",
    G + "guhshi.tong": "Tong aan/uit",
    G + "guhshi.tong.aan": "%s hapt weer knabbels van de grond, njam!",
    G + "guhshi.tong.uit": "%s houdt zijn tong binnen. Njeg.",
    # the building blocks
    G + "vraagblok.knabbel": "Ploink! Een kaasknabbel uit het vraagtekenblok. Morgen weer eentje, njeg!",
    G + "vraagblok.morgen": "Tok. Leeg voor vandaag: morgen zit er weer een knabbel in, njeg.",
    G + "pijp.hint": "Ga op de pijp staan en sluip (Shift): je komt eruit bij de dichtstbijzijnde pijp van dezelfde kleur.",
    G + "pijp.geen_partner": "Deze pijp komt nergens uit. Zet binnen %s blokken nog een pijp van dezelfde kleur, njeg.",
    G + "pijp.afstappen": "Stap eerst af: je guh past niet in de pijp, njeg.",
    G + "pijp.bezig": "Nu even niet: je bent ergens mee bezig, njeg.",
    G + "pijp.geverfd": "De hele pijp heeft een nieuw kleurtje. Pijpen van dezelfde kleur horen bij elkaar.",
    G + "mast.vahoeg": "Vlaggetje gehaald! Vahoeg!",
    # the highscore board
    G + "bord.kop": "Super Guhrio: de snelste tijden",
    G + "bord.kasteel": "Het hele kasteel in één keer",
    G + "bord.levels": "Per level",
    G + "bord.level": "Level %s",
    G + "bord.leeg": "%s: nog geen tijd",
    G + "bord.record": "%s: %s (%s)",
    G + "bord.klik": "Klik op het gouden blok voor je eigen tijden",
    G + "bord.eigen.kop": "Jouw tijden in het Kasteel van de Grote Nether-Mika",
    G + "bord.eigen.regel": "%s: jij %s, record %s",
    G + "bord.eigen.geen": "nog niet gehaald",
    G + "bord.eigen.geen_record": "nog geen",
    G + "bord.eigen.record": "%s van %s",
    # the Guhdex page
    G + "beloning.guhshi": "Guhshi gaat met je mee (na het duel, in de torenkamer)",
    G + "beloning.guhriobeloning_rode_pet": f"Rode Guhrio-pet met snor (Pad-guh's kraam, {PRIJZEN['rode_pet']} munten)",
    G + "beloning.guhriobeloning_groene_pet": f"Groene Luiguh-pet (Pad-guh's kraam, {PRIJZEN['groene_pet']} munten)",
    G + "beloning.guhriobeloning_schild": f"Schild-Mika-schild (Pad-guh's kraam, {PRIJZEN['schild']} munten)",
    G + "beloning.guhriobeloning_prinsessenkroon": "Kroontje van Prinses Perzikguh (van de prinses, na het duel)",
    G + "beloning.guhriobeloning_gouden_pet": "Gouden vadspet (van Pad-guh, voor alle 18 grote vadsmunten)",
    G + "beloning.bouwblokken": "Een bouwblok uit Pad-guh's kraam: vraagtekenblok, vlaggenmast of groene reispijp",
}
WERELDEN = {"1": "de binnentuin", "2": "de kelders", "3": "de burcht"}
RANG = ("eerste", "tweede", "derde")
for _w in "123":
    for _n in "12":
        for _i in range(3):
            TEXTS[G + f"vads.{_w}_{_n}.{_i}"] = f"Level {_w}-{_n}: de {RANG[_i]} grote vadsmunt"


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# the questline (the Guhdex page), its advancements and the FTB section
# =====================================================================================================================
def verhaal(h):
    from features import bbq2, verhaal_motor
    kraam = "Pad-guh's kraam op het voorplein van het Kasteel van de Grote Nether-Mika (Guhbarbecuether)"
    verhaal_motor.verhaallijn(
        h, "guhrio_beloning", "Pad-guh's kraam en de beloningen",
        "Wat je aan Super Guhrio overhoudt: Guhshi, outfits, bouwblokken en achttien grote vadsmunten. Njeg!",
        [("Praat met Pad-guh", "Praat met Pad-guh. Hij zit op de toonbank van zijn paddenstoelenkraam, links op het voorplein van het kasteel.", kraam),
         ("Guhshi gaat met je mee", "Win het duel met de Grote Nether-Mika. In de torenkamer erachter wacht Guhshi bij Prinses Perzikguh: klik op "
                                    "hem en zeg ja.", "De torenkamer van het kasteel (ook via de trap in de levelhal)"),
         ("De gouden vadspet", "Vind alle achttien grote vadsmunten, drie per level (hieronder zie je welke je al hebt), en haal bij Pad-guh je "
                               "gouden vadspet.", "De zes levels van het kasteel, daarna Pad-guh's kraam")],
        klaar=("Guhshi, de gouden vadspet en alle achttien vadsmunten zijn van jou. Super vahoeg!", kraam),
        kort={"0": "Praat met Pad-guh op het voorplein", "1": "Win het duel en klik op Guhshi", "2": "Vind alle 18 grote vadsmunten"})
    for naam in VERBORGEN:
        bbq2.verborgen(h, naam)
    bbq2.zichtbaar(h, "guhrio", "guhrio_beloning_pijp", "root", "guhs:guhriobeloning_pijp", "task", "Loodguhter",
                   "Reis door een groene reispijp naar een andere pijp")
    bbq2.zichtbaar(h, "guhrio", "guhrio_beloning_guhshi", "guhrio_duel", "guhs:guhrio_guhshi_ei", "goal", "Guhshi is van jou!",
                   "Neem na het duel Guhshi mee uit de torenkamer van Prinses Perzikguh")
    bbq2.zichtbaar(h, "guhrio", "guhrio_beloning_gouden_pet", "guhrio_vadsmunten", "guhs:guhriobeloning_gouden_pet", "challenge",
                   "Goud op je guh", "Haal bij Pad-guh de gouden vadspet voor alle achttien grote vadsmunten")


def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("guhrio_beloning_padguh", "Een held! Njeg!", "Links op het voorplein van het kasteel staat een reuzenpaddenstoel: de kraam van &6Pad-guh&r. "
      "Hij vertelt wat er aan de hand is: de Grote Nether-Mika heeft Prinses Perzikguh meegenomen. Voor een stukje taart.",
      "guhs:guhriobeloning_vraagblok", [adv("guhrio_beloning_padguh")], deps=["guhrio_kasteel"])
    q("guhrio_beloning_winkel", "Munten uitgeven", "De &6munten&r uit de levels zitten in je buidel (een munt telt de eerste keer dat je hem pakt). "
      "Bij Pad-guh koop je er &doutfits&r en &dbouwblokken&r voor. Bij elke vlaggenmast krijg je ook wat fooi: één munt per vijf op je paneel.",
      "guhs:guhrio_munt", [adv("guhrio_beloning_gekocht")], deps=["guhrio_beloning_padguh"])
    q("guhrio_beloning_rode_pet", "Het is-a mij, Guhrio!", f"De &crode pet met snor&r kost {PRIJZEN['rode_pet']} munten. Houd hem vast (rechtsklik "
      "ingedrukt) om hem te ontgrendelen en zet hem je guh op in de kledingkast. De snor zit er gratis bij.", "guhs:guhriobeloning_rode_pet",
      [item("guhs:guhriobeloning_rode_pet")], rewards=(("guhs:kaas_knabbels", 6),), deps=["guhrio_beloning_winkel"])
    q("guhrio_beloning_groene_pet", "De groene broer", f"De &agroene pet&r van Luiguh kost {PRIJZEN['groene_pet']} munten. Zonder snor, wel net zo "
      "dapper. Meestal.", "guhs:guhriobeloning_groene_pet", [item("guhs:guhriobeloning_groene_pet")], rewards=(("guhs:kaas_knabbels", 6),),
      deps=["guhrio_beloning_winkel"])
    q("guhrio_beloning_schild", "Schild op je rug", f"Het &aSchild-Mika-schild&r kost {PRIJZEN['schild']} munten. Je guh gaat er niet sneller van "
      "glijden, maar het staat wel stoer.", "guhs:guhriobeloning_schild", [item("guhs:guhriobeloning_schild")],
      rewards=(("guhs:kaas_knabbels", 6),), deps=["guhrio_beloning_winkel"])
    q("guhrio_beloning_vraagblok", "Elke dag een knabbel", "Het &6Knabbel-vraagtekenblok&r kun je thuis neerzetten. Spring er met je hoofd "
      "tegenaan: elke dag komt er één kaasknabbel uit, voor elke speler eentje. Op het voorplein hangt er ook een: probeer maar!",
      "guhs:guhriobeloning_vraagblok", [adv("guhrio_beloning_vraagblok")], deps=["guhrio_beloning_padguh"])
    q("guhrio_beloning_pijp", "Door de groene pijp", "De &agroene reispijp&r werkt echt. Zet er twee neer (hooguit 50 blokken uit elkaar), ga op "
      "een pijp staan en &dsluip&r: je glijdt erin en komt uit de andere. Met verf maak je een tweede paar in een andere kleur. Op het "
      "voorplein staat een paar klaar.", "guhs:guhriobeloning_pijp", [adv("guhrio_beloning_pijp")], deps=["guhrio_beloning_padguh"])
    q("guhrio_beloning_mast", "Je eigen vlaggenmast", "Stapel &akasteelvlaggenmasten&r op elkaar: onderaan komt vanzelf een voet, bovenaan de "
      "gouden bol met het roze vlaggetje. Klik erop voor een feestje.", "guhs:guhriobeloning_vlaggenmast",
      [item("guhs:guhriobeloning_vlaggenmast")], deps=["guhrio_beloning_winkel"])
    q("guhrio_beloning_bord", "Wie is de snelste?", "Rechts op het voorplein staat het &6highscorebord&r: de snelste tijden van de server, per "
      "level en voor het hele kasteel in één keer. Klik op het gouden blok en je ziet je eigen tijden ernaast.", "minecraft:clock",
      [adv("guhrio_beloning_bord")], deps=["guhrio_beloning_padguh"])
    q("guhrio_beloning_perzik", "Prinses Perzikguh", "Win het duel met de Grote Nether-Mika en praat met &dPrinses Perzikguh&r in de torenkamer. "
      "Ze geeft je haar kroontje en een taart. Ontvoerd? Welnee: de taart was gewoon lekker.", "guhs:guhriobeloning_prinsessenkroon",
      [adv("guhrio_beloning_kroon")], rewards=(("guhs:kaas_knabbels", 12),), deps=["guhrio_levels"], shape="hexagon", xp=100)
    q("guhrio_beloning_guhshi", "Guhshi is van jou!", "Naast de prinses wacht &aGuhshi&r. Klik op hem en zeg ja: je krijgt je eigen Guhshi, met "
      "zadel en al. Eén per speler; die in de torenkamer blijft daar voor de volgende held.", "guhs:guhrio_guhshi_ei",
      [adv("guhrio_beloning_guhshi")], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhrio_beloning_perzik"], shape="gear", xp=200)
    q("guhrio_beloning_guhshi_pagina", "Guhshi in de Guhdex", "Een eigen Guhshi krijgt zijn bladzijde in de &dGuhdex&r. Op zijn rug spring je met "
      "&dspatie&r; houd spatie vast in de lucht en hij &dfladdert&r over gaten van wel zeven blokken.", "guhs:guhrio_guhshi_plek",
      [adv("seen_guhshi")], deps=["guhrio_beloning_guhshi"])
    q("guhrio_beloning_tong", "Njam op afstand", "Leg een kaasknabbel op de grond bij je Guhshi. Flits: zijn lange tong hapt hem weg van wel vijf "
      "blokken ver. In zijn guh-menu zet je de tong uit als hij van je spullen moet afblijven.", "guhs:kaas_knabbels",
      [adv("guhrio_beloning_tong")], deps=["guhrio_beloning_guhshi"])
    q("guhrio_beloning_gouden_pet", "De gouden vadspet", "Heb je alle &6achttien grote vadsmunten&r? Ga dan naar Pad-guh: hij heeft de "
      "&6gouden vadspet&r voor je. De Guhdex (Verhalen > Pad-guh's kraam en de beloningen) laat per level zien welke vadsmunten je nog mist.",
      "guhs:guhriobeloning_gouden_pet", [adv("guhrio_beloning_gouden_pet")], rewards=(("guhs:kaas_knabbels", 32),),
      deps=["guhrio_vadsmunten"], shape="diamond", xp=300)


# =====================================================================================================================
# the test room and the self-check
# =====================================================================================================================
def structures(h):
    s = h.Structure((13, 9, 13))
    s.fill(0, 0, 0, 12, 0, 12, "minecraft:smooth_stone")
    s.save(TESTKAMER)


def selfcheck(h):
    from features import guhrio_beloning_modellen as modellen
    from features import guhrio_beloning_tex as tex
    A, D = h.A, h.D
    missing = [p for p in [f"{A}/blockstates/{n}.json" for n in BLOKKEN]
               + [f"{A}/models/block/guhriobeloning_pijp_{k}.json" for k in tex.KLEUREN]
               + [f"{D}/loot_table/blocks/{n}.json" for n in BLOKKEN] + [f"{D}/structure/{TESTKAMER}.nbt"]
               + [f"{D}/advancement/quest/{n}.json" for n in VERBORGEN]
               if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    missing += modellen.check(h)
    # the same prices and the same spots as the Java side
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhriobeloning")
    winkel = open(os.path.join(java, "Winkel.java"), encoding="utf-8").read()
    for waar, prijs in PRIJZEN.items():
        m = re.search(r'\("%s", (\d+),' % waar, winkel)
        if not m or int(m.group(1)) != prijs:
            missing.append(f"Winkel.java: the price of {waar} is not {prijs}")
    feature = open(os.path.join(java, "GuhrioBeloningFeature.java"), encoding="utf-8").read()
    for wie, veld in (("padguh", "PADGUH_PLEK"), ("perzikguh", "PERZIKGUH_PLEK"), ("guhshi", "GUHSHI_PLEK")):
        m = re.search(veld + r" = new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)", feature)
        if wie not in GEBOUWD:
            missing.append(f"the castle was built without {wie} (guhrio_kasteel did not call VOORPLEIN / TORENKAMER)")
        elif not m or tuple(int(g) for g in m.groups()) != tuple(GEBOUWD[wie]):
            missing.append(f"GuhrioBeloningFeature.{veld} must be new BlockPos{tuple(GEBOUWD[wie])}")
    if missing:
        raise SystemExit(f"guhrio_beloning: missing {missing}")


def build(h):
    from features import guhrio_beloning_modellen as modellen
    textures(h)
    blocks_and_items(h)
    texts(h)
    modellen.padguh(h)
    modellen.perzikguh(h)
    verhaal(h)
    structures(h)
    selfcheck(h)
