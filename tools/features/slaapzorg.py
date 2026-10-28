"""
1.8.0 (Guhdromenland), part B "slaapzorg" (dromen-ontwerp/BOUWPLAN_180.md): the slaapkamer-set (Muziekdoosje, Mobiel,
Nachtkastje, Droomvanger, Slaapmasker-kapstok), the maannachtlampjes (maan, ster, wolkje, slapende guh) and the krabpalen
(roze, blauw), the tag of sleeping spots, and the texts. Java: feature/slaapzorg (SlaapzorgFeature, SlaapBlokken, Slapen,
SlaapplekGoal, InslaapGoal, Moeheid, Slaapliedje, OntbijtOpBed, KrabpaalGoal, SlaapzorgCommando, client/MobielRenderer).

  build(h)    textures (16x16 pixel art), block models (elements), block states (facing; lit / speelt), item models,
              recipes, self-drop loot, the tags (#guhs:guh_slaapplekken, mineable/axe), the texts (Dutch in nl_nl, English in
              tools/lang/en/c212_slaapzorg.json) and a self-check

Standalone (from the project root): PYTHONPATH=tools python tools/features/slaapzorg.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

LAMPJES = {"maan": ("Maannachtlampje", "Moon Night Light"), "ster": ("Sterrennachtlampje", "Star Night Light"),
           "wolkje": ("Wolkjesnachtlampje", "Cloud Night Light"), "guh": ("Slapende-guh-nachtlampje", "Sleeping Guh Night Light")}

NAMEN = {  # id -> (Dutch, English, Dutch lore, English lore)
    "muziekdoosje": ("Muziekdoosje", "Music Box",
                     "Rechtsklik: een slaapliedje. Alle guhs in de buurt gapen en vallen zachtjes in slaap. Sst!",
                     "Right-click: a lullaby. Every guh around yawns and softly falls asleep. Shh!"),
    "mobiel": ("Mobiel", "Baby Mobile",
               "Hang hem boven een guhbedje. Hij draait rustig rond; rechtsklik en hij speelt het slaapliedje.",
               "Hang it above a guh bed. It turns slowly; right-click and it plays the lullaby."),
    "nachtkastje": ("Nachtkastje", "Bedside Table", "Met een lampje erop. Rechtsklik: lampje aan of uit.",
                    "With a little lamp on top. Right-click: lamp on or off."),
    "droomvanger": ("Droomvanger", "Dreamcatcher", "Vangt nare dromen, laat lieve dromen door. Hang hem aan de muur.",
                    "Catches bad dreams, lets sweet ones through. Hang it on a wall."),
    "slaapmasker_kapstok": ("Slaapmasker-kapstok", "Sleep Mask Stand", "Voor alle slaapmaskertjes van de guhs. Eentje met oortjes!",
                            "For all the guhs' sleep masks. One with little ears!"),
    "krabpaal": ("Krabpaal", "Scratching Post", "Tamme guhs hupsen er overdag bovenop en kijken rond, net een poesje.",
                 "Tame guhs hop on top by day and look around, just like a kitty."),
    "krabpaal_blauw": ("Blauwe krabpaal", "Blue Scratching Post", "Tamme guhs hupsen er overdag bovenop en kijken rond, net een poesje.",
                       "Tame guhs hop on top by day and look around, just like a kitty."),
}
for _k, (_nl, _en) in LAMPJES.items():
    NAMEN[f"maanlampje_{_k}"] = (_nl, _en, "Rechtsklik: aan of uit. Zacht licht, zodat niemand bang is in het donker.",
                                 "Right-click: on or off. A soft light, so nobody is scared of the dark.")

TEKSTEN = {
    "gui.guhs.slaapzorg.ontbijt.te_vroeg": ("Sst... %s slaapt nog. Ontbijt op bed is voor 's ochtends, njeg!",
                                            "Shh... %s is still asleep. Breakfast in bed is for the morning, njeg!"),
    "gui.guhs.slaapzorg.ontbijt.lekker": ("Ontbijt op bed! %s rekt zich uit, smikkelt alles op en is de hele dag uitgerust. VAHOEG!",
                                          "Breakfast in bed! %s stretches, munches it all up and is well rested all day. VAHOEG!"),
    "gui.guhs.slaapzorg.ontbijt.nog_een": ("%s smikkelt nog een knabbeltje mee. Vadsig hoor!", "%s munches one more nibble. So vadsig!"),
}
for _id, (_nl, _en, _lnl, _len) in NAMEN.items():
    TEKSTEN[f"block.guhs.{_id}"] = (_nl, _en)
    TEKSTEN[f"block.guhs.{_id}.lore"] = (_lnl, _len)

AXE = ["muziekdoosje", "mobiel", "nachtkastje", "droomvanger", "slaapmasker_kapstok", "krabpaal", "krabpaal_blauw"]

# --- colours -------------------------------------------------------------------------------------------------------------
ROZE, ROZE_D, ROZE_L = (246, 168, 200), (214, 120, 162), (255, 214, 232)
HOUT, HOUT_D = (232, 170, 176), (196, 128, 140)
GOUD, GOUD_D = (250, 214, 100), (206, 160, 60)
BLAUW, BLAUW_D, BLAUW_L = (150, 196, 246), (104, 150, 214), (206, 230, 255)
NACHT = (70, 64, 140)
WIT = (252, 250, 255)
GEEL, GEEL_L = (255, 226, 120), (255, 244, 190)


def tex(rows, kleuren):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, rij in enumerate(rows):
        for x, ch in enumerate(rij):
            if ch in kleuren:
                c = kleuren[ch]
                img.putpixel((x, y), c + (255,) if len(c) == 3 else c)
    return img


def vlak(kleur, rand=None, stip=None, seed=0):
    img = Image.new("RGBA", (16, 16), kleur + (255,))
    d = ImageDraw.Draw(img)
    if rand:
        d.rectangle([0, 0, 15, 15], outline=rand + (255,))
    if stip:
        for i in range(0, 256, 23 + seed):
            img.putpixel(((i * 7 + seed) % 16, (i * 3) % 16), stip + (255,))
    return img


def gezichtje(img, x, y, slaapt=True, kleur=(90, 50, 80)):
    """A tiny guh face: closed eyes (^ ^ when asleep), blush, snoetje."""
    d = ImageDraw.Draw(img)
    if slaapt:
        d.line([x, y + 1, x + 1, y], fill=kleur + (255,))
        d.line([x + 4, y, x + 5, y + 1], fill=kleur + (255,))
    else:
        d.point([(x, y), (x + 5, y)], fill=kleur + (255,))
    d.point([(x - 1, y + 2), (x + 6, y + 2)], fill=(255, 140, 170, 255))
    d.point([(x + 2, y + 2), (x + 3, y + 2)], fill=(214, 96, 140, 255))


def textures():
    T = {}
    # muziekdoosje: pink wood with a gold trim and a heart; the lid with a little twirling guh painted on it
    zij = vlak(HOUT, rand=GOUD_D)
    d = ImageDraw.Draw(zij)
    d.point([(6, 6), (7, 5), (8, 6), (9, 5), (10, 6), (7, 7), (8, 8), (9, 7)], fill=(255, 110, 150, 255))
    T["muziekdoosje_zij"] = zij
    top = vlak(HOUT_D, rand=GOUD)
    gezichtje(top, 5, 7, slaapt=False)
    T["muziekdoosje_top"] = top
    T["muziekdoosje_goud"] = vlak(GOUD, stip=GOUD_D)
    T["muziekdoosje_guhtje"] = vlak(ROZE, stip=ROZE_L)
    # mobiel: the hook/bar (thin wood), the hanging moon, star, cloud and guh
    T["mobiel_hout"] = vlak(WIT, stip=(230, 226, 240))
    T["mobiel_maan"] = vlak(GEEL, stip=GEEL_L)
    T["mobiel_ster"] = vlak(GOUD, stip=GEEL_L)
    T["mobiel_wolk"] = vlak(WIT, stip=BLAUW_L)
    g = vlak(ROZE, stip=ROZE_L)
    gezichtje(g, 5, 7)
    T["mobiel_guh"] = g
    T["mobiel_touwtje"] = vlak((240, 236, 246))
    # nachtkastje: a drawer with a heart knob; the lamp shade on and off
    kast = vlak(HOUT, rand=HOUT_D)
    d = ImageDraw.Draw(kast)
    d.rectangle([2, 3, 13, 8], outline=HOUT_D + (255,))
    d.rectangle([2, 10, 13, 14], outline=HOUT_D + (255,))
    d.point([(7, 5), (8, 5), (7, 6), (8, 6), (7, 12), (8, 12)], fill=GOUD + (255,))
    T["nachtkastje_voor"] = kast
    T["nachtkastje_zij"] = vlak(HOUT, rand=HOUT_D)
    T["nachtkastje_top"] = vlak(HOUT_D, rand=HOUT)
    T["nachtkastje_kap_aan"] = vlak(GEEL_L, stip=WIT)
    T["nachtkastje_kap_uit"] = vlak((236, 220, 200), stip=(220, 204, 186))
    # droomvanger: a hoop with a web, beads, two feathers and a guh charm (cut out)
    dv = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(dv)
    d.ellipse([2, 0, 13, 10], outline=HOUT_D + (255,))
    d.line([7, 1, 7, 9], fill=(250, 240, 250, 200))
    d.line([3, 5, 12, 5], fill=(250, 240, 250, 200))
    d.line([4, 2, 11, 8], fill=(250, 240, 250, 160))
    d.line([11, 2, 4, 8], fill=(250, 240, 250, 160))
    d.point([(7, 5)], fill=BLAUW + (255,))
    d.point([(5, 3), (10, 7)], fill=(255, 140, 180, 255))
    for x in (4, 11):
        d.line([x, 10, x, 12], fill=(250, 240, 250, 255))
        d.line([x, 13, x, 15], fill=BLAUW_L + (255,))
        d.point([(x - 1, 14), (x + 1, 14)], fill=BLAUW + (255,))
    d.line([7, 10, 7, 11], fill=(250, 240, 250, 255))
    d.rectangle([6, 12, 8, 14], fill=ROZE + (255,))
    d.point([(6, 11), (8, 11)], fill=ROZE_D + (255,))
    T["droomvanger"] = dv
    # slaapmasker-kapstok: the post and two masks (one with little ears)
    T["kapstok_hout"] = vlak(HOUT, stip=HOUT_D)
    m1 = vlak(ROZE, stip=ROZE_L)
    ImageDraw.Draw(m1).line([3, 8, 12, 8], fill=ROZE_D + (255,))
    T["kapstok_masker_roze"] = m1
    m2 = vlak(BLAUW, stip=BLAUW_L)
    ImageDraw.Draw(m2).line([3, 8, 12, 8], fill=BLAUW_D + (255,))
    T["kapstok_masker_blauw"] = m2
    # maannachtlampjes: a flat cut-out lamp (front and back), on and off
    vormen = {
        "maan": ["....XXXX........", "..XXXXX.........", ".XXXXX..........", "XXXXX...........", "XXXX............",
                 "XXXX............", "XXXX............", "XXXXX...........", ".XXXXX......X...", "..XXXXXX..XX....",
                 "....XXXXXXX.....", "......XXX.......", "................", "................", "................",
                 "................"],
        "ster": [".......XX.......", "......XXXX......", "......XXXX......", "XXXXXXXXXXXXXXXX", ".XXXXXXXXXXXXXX.",
                 "...XXXXXXXXXX...", "....XXXXXXXX....", "...XXXXXXXXXX...", "...XXXX..XXXX...", "..XXX......XXX..",
                 "..XX........XX..", "................", "................", "................", "................",
                 "................"],
        "wolkje": ["................", "......XXXX......", "....XXXXXXXX....", "..XXXXXXXXXXXX..", ".XXXXXXXXXXXXXXX",
                   "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXX", ".XXXXXXXXXXXXXX.", "................", "................",
                   "................", "................", "................", "................", "................",
                   "................"],
        "guh": ["..XX........XX..", ".XXXX......XXXX.", ".XXXXXXXXXXXXXX.", "..XXXXXXXXXXXX..", ".XXXXXXXXXXXXXX.",
                "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXX", ".XXXXXXXXXXXXXX.", "..XXXXXXXXXXXX..",
                "................", "................", "................", "................", "................",
                "................"],
    }
    aan_kleur = {"maan": (GEEL, GEEL_L), "ster": (GOUD, GEEL_L), "wolkje": (WIT, BLAUW_L), "guh": (ROZE, ROZE_L)}
    for k, rows in vormen.items():
        for staat in ("aan", "uit"):
            k1, k2 = aan_kleur[k]
            if staat == "uit":
                k1 = tuple(int(c * 0.72) for c in k1)
                k2 = tuple(int(c * 0.8) for c in k2)
            img = tex(rows, {"X": k1})
            d = ImageDraw.Draw(img)
            for (x, y) in ((3, 2), (6, 4), (9, 6), (2, 6), (11, 3)):
                if img.getpixel((x, y))[3]:
                    img.putpixel((x, y), k2 + (255,))
            if k == "guh":
                gezichtje(img, 5, 5)
            elif k in ("maan", "wolkje"):
                gezichtje(img, 2 if k == "maan" else 5, 5)
            T[f"maanlampje_{k}_{staat}"] = img
    T["maanlampje_voet"] = vlak(HOUT_D, stip=HOUT)
    # krabpalen: plush platforms with a guh face, a rope post
    for kleur, (a, b, c) in {"": (ROZE, ROZE_D, ROZE_L), "_blauw": (BLAUW, BLAUW_D, BLAUW_L)}.items():
        T[f"krabpaal{kleur}_pluche"] = vlak(a, stip=c)
        voor = vlak(a, stip=c)
        gezichtje(voor, 5, 6, slaapt=False)
        T[f"krabpaal{kleur}_voor"] = voor
        T[f"krabpaal{kleur}_oor"] = vlak(b, stip=a)
    touw = Image.new("RGBA", (16, 16), (214, 186, 140, 255))
    d = ImageDraw.Draw(touw)
    for y in range(0, 16, 2):
        d.line([0, y, 15, y + 1], fill=(184, 152, 106, 255))
    T["krabpaal_touw"] = touw
    return T


def E(frm, to, faces):
    """A model element; faces: dict side -> texture key (#name), or one name for all six."""
    if isinstance(faces, str):
        faces = {s: faces for s in ("down", "up", "north", "south", "west", "east")}
    return {"from": frm, "to": to, "faces": {s: {"texture": f"#{t}"} for s, t in faces.items()}}


def model(h, naam, elements, textures):
    """(26.1: no render_type any more; the layer comes from the texture's transparency.)"""
    tx = {k: f"guhs:block/slaapzorg/{v}" for k, v in textures.items()}
    tx.setdefault("particle", next(iter(tx.values())))
    h.w(f"{h.A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": tx, "elements": elements})


def facing(naam, model_van, extra_props=None):
    """facing x (extra) -> model; model_van(extra dict) -> model id."""
    variants = {}
    combos = [{}]
    for prop, waarden in (extra_props or {}).items():
        combos = [dict(c, **{prop: w}) for c in combos for w in waarden]
    for c in combos:
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            key = ",".join([f"facing={f}"] + [f"{k}={v}" for k, v in c.items()])
            variants[key] = {"model": model_van(c), **({"y": y} if y else {})}
    return {"variants": variants}


def build(h):
    for naam, img in textures().items():
        h.save(img, "block", "slaapzorg", f"{naam}.png")
    A = h.A
    # Muziekdoosje
    model(h, "muziekdoosje", [
        E([4, 0, 5], [12, 6, 11], {"down": "zij", "up": "top", "north": "zij", "south": "zij", "west": "zij", "east": "zij"}),
        E([12, 2.5, 7.5], [14, 3.5, 8.5], "goud"), E([13.5, 3.5, 7.5], [14.5, 5.5, 8.5], "goud"),
        E([7, 6, 7], [9, 8, 9], "guhtje"), E([7, 8, 7.5], [7.5, 8.5, 8], "guhtje"), E([8.5, 8, 7.5], [9, 8.5, 8], "guhtje"),
    ], {"zij": "muziekdoosje_zij", "top": "muziekdoosje_top", "goud": "muziekdoosje_goud", "guhtje": "muziekdoosje_guhtje"})
    h.w(f"{A}/blockstates/muziekdoosje.json", facing("muziekdoosje", lambda c: "guhs:block/muziekdoosje",
                                                     {"speelt": range(13)}))
    # Mobiel: the hook + the turning part (drawn by MobielRenderer)
    model(h, "mobiel", [E([7.5, 13, 7.5], [8.5, 16, 8.5], "hout"), E([6.5, 12, 6.5], [9.5, 13, 9.5], "hout")], {"hout": "mobiel_hout"})
    draai = [E([2, 11.5, 7.5], [14, 12, 8.5], "hout"), E([7.5, 11.5, 2], [8.5, 12, 14], "hout")]
    for (x, z, t, maat, y0) in ((3, 8, "maan", 2, 5), (13, 8, "ster", 2, 6), (8, 3, "guh", 2.5, 5), (8, 13, "wolk", 2.5, 6)):
        draai.append(E([x - 0.1, y0 + maat, z - 0.1], [x + 0.1, 11.5, z + 0.1], "touw"))
        draai.append(E([x - maat / 2, y0, z - maat / 2], [x + maat / 2, y0 + maat, z + maat / 2], t))
    model(h, "mobiel_draai", draai, {"hout": "mobiel_hout", "touw": "mobiel_touwtje", "maan": "mobiel_maan", "ster": "mobiel_ster",
                                     "guh": "mobiel_guh", "wolk": "mobiel_wolk"})
    h.w(f"{A}/blockstates/mobiel.json", facing("mobiel", lambda c: "guhs:block/mobiel", {"speelt": range(13)}))
    # Nachtkastje (lamp on / off)
    for staat in ("aan", "uit"):
        model(h, f"nachtkastje_{staat}", [
            E([1, 0, 1], [15, 13, 15], {"down": "top", "up": "top", "north": "voor", "south": "zij", "west": "zij", "east": "zij"}),
            E([7, 13, 7], [9, 14, 9], "zij"), E([5, 14, 5], [11, 16, 11], "kap"),
        ], {"voor": "nachtkastje_voor", "zij": "nachtkastje_zij", "top": "nachtkastje_top", "kap": f"nachtkastje_kap_{staat}"})
    h.w(f"{A}/blockstates/nachtkastje.json", facing("nachtkastje", lambda c: f"guhs:block/nachtkastje_{'aan' if c['lit'] == 'true' else 'uit'}",
                                                    {"lit": ("true", "false")}))
    # Droomvanger (flat on the wall behind it: the south side of a north-facing one)
    model(h, "droomvanger", [E([1, 0, 15.5], [15, 16, 15.5], {"north": "dv", "south": "dv"})], {"dv": "droomvanger"})
    h.w(f"{A}/blockstates/droomvanger.json", facing("droomvanger", lambda c: "guhs:block/droomvanger"))
    # Slaapmasker-kapstok
    model(h, "slaapmasker_kapstok", [
        E([4, 0, 4], [12, 1, 12], "hout"), E([7, 1, 7], [9, 15, 9], "hout"), E([3, 12, 7.5], [13, 13, 8.5], "hout"),
        E([2, 8.5, 7.6], [6.5, 11, 8.4], "roze"), E([9.5, 8.5, 7.6], [14, 11, 8.4], "blauw"),
        E([9.8, 11, 7.6], [10.8, 12, 8.4], "blauw"), E([12.7, 11, 7.6], [13.7, 12, 8.4], "blauw"),
    ], {"hout": "kapstok_hout", "roze": "kapstok_masker_roze", "blauw": "kapstok_masker_blauw"})
    h.w(f"{A}/blockstates/slaapmasker_kapstok.json", facing("slaapmasker_kapstok", lambda c: "guhs:block/slaapmasker_kapstok"))
    # Maannachtlampjes: a little foot and a flat cut-out shape, lit or not
    for k in LAMPJES:
        hoog = 9 if k in ("maan", "ster") else 7 if k == "wolkje" else 8
        for staat in ("aan", "uit"):
            model(h, f"maanlampje_{k}_{staat}", [
                E([5, 0, 6.5], [11, 1, 9.5], "voet"),
                E([4 if k != "wolkje" else 3, 1, 7.5], [12 if k != "wolkje" else 13, 1 + hoog, 8.5],
                  {"north": "vorm", "south": "vorm"}),
            ], {"voet": "maanlampje_voet", "vorm": f"maanlampje_{k}_{staat}"})
        h.w(f"{A}/blockstates/maanlampje_{k}.json", facing(f"maanlampje_{k}",
            lambda c, k=k: f"guhs:block/maanlampje_{k}_{'aan' if c['lit'] == 'true' else 'uit'}", {"lit": ("true", "false")}))
    # Krabpalen
    for kleur in ("", "_blauw"):
        n = f"krabpaal{kleur}"
        model(h, n, [
            E([2, 0, 2], [14, 2, 14], "pluche"), E([6, 2, 6], [10, 13, 10], "touw"),
            E([2, 13, 2], [14, 16, 14], {"down": "pluche", "up": "pluche", "north": "voor", "south": "pluche", "west": "pluche", "east": "pluche"}),
            E([3, 16, 3], [6, 18, 5], "oor"), E([10, 16, 3], [13, 18, 5], "oor"),
        ], {"pluche": f"{n}_pluche", "voor": f"{n}_voor", "oor": f"{n}_oor", "touw": "krabpaal_touw"})
        h.w(f"{A}/blockstates/{n}.json", facing(n, lambda c, n=n: f"guhs:block/{n}"))
    # items, loot, recipes
    for n in NAMEN:
        item_model = {"maanlampje_" + k: f"guhs:block/maanlampje_{k}_aan" for k in LAMPJES}
        item_model["nachtkastje"] = "guhs:block/nachtkastje_aan"
        if n == "droomvanger":
            h.item_model(n, tex="guhs:block/slaapzorg/droomvanger")
        else:
            h.w(f"{A}/models/item/{n}.json", {"parent": item_model.get(n, f"guhs:block/{n}")})
        h.self_drop(n)
    planken = "minecraft:cherry_planks"
    h.shaped("muziekdoosje", ["GNG", "PPP"], {"G": "minecraft:gold_nugget", "N": "minecraft:note_block", "P": planken}, "guhs:muziekdoosje")
    h.shaped("mobiel", [" S ", "SSS", "YGW"], {"S": "minecraft:string", "Y": "minecraft:yellow_dye", "G": "minecraft:glowstone_dust",
                                              "W": "minecraft:pink_wool"}, "guhs:mobiel")
    h.shaped("nachtkastje", [" T ", "PPP", "P P"], {"T": "minecraft:torch", "P": planken}, "guhs:nachtkastje")
    h.shaped("droomvanger", [" T ", "TST", "F F"], {"T": "minecraft:stick", "S": "minecraft:string", "F": "minecraft:feather"}, "guhs:droomvanger")
    h.shaped("slaapmasker_kapstok", ["WTW", " T ", " T "], {"W": "minecraft:pink_wool", "T": "minecraft:stick"}, "guhs:slaapmasker_kapstok")
    for k, x in (("maan", "minecraft:yellow_dye"), ("ster", "minecraft:gold_nugget"), ("wolkje", "minecraft:white_wool"),
                 ("guh", "minecraft:pink_dye")):
        h.shaped(f"maanlampje_{k}", ["X", "G", "P"], {"X": x, "G": "minecraft:glowstone_dust", "P": planken}, f"guhs:maanlampje_{k}")
    h.shaped("krabpaal", ["W", "S", "W"], {"W": "minecraft:pink_wool", "S": "minecraft:string"}, "guhs:krabpaal")
    h.shaped("krabpaal_blauw", ["W", "S", "W"], {"W": "minecraft:light_blue_wool", "S": "minecraft:string"}, "guhs:krabpaal_blauw")
    # tags
    h.add_tag("guhs/tags/block/guh_slaapplekken", ["guhs:huisje_bedje", "guhs:guhnestje"])
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{n}" for n in AXE])
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for n in NAMEN:
        for p in (f"{h.A}/blockstates/{n}.json", f"{h.A}/models/item/{n}.json", f"{h.D}/recipe/{n}.json", f"{h.D}/loot_table/blocks/{n}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for p in os.listdir(os.path.join(h.A, "models", "block")):
        pass
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("slaapzorg self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c212_slaapzorg.json")
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
    print(f"slaapzorg: {len(NAMEN)} blocks, {len(TEKSTEN)} texts")


def _mc26():
    """Only slaapzorg's items in their 26.1 shape (client item definitions)."""
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in NAMEN:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    _direct()
