"""
Het Vadsig eetfestijn (feature "smul"): a big guh food festival in the Guhmension. A giant striped tent with a guh face for
a facade and the mouth for a door; inside the catching arena (guh-face floor, high tent roof, four food chutes), stands for
the spectators, the Smulguh on her podium, tables and snack counters, a giant cake and milkshakes; outside a plaza with food
stalls and a guh-face kaassaus fountain. The game itself: nl.juiced.guhs.feature.smul.SmulGame.

Also: the smul outfit (bib, baker's hat, pink apron), the smulmunt, the borrowed smulschaal, the golden smulknabbel, the
arena markers, the Smulguh's texture, lang, advancements, FTB quests, a small arena for the GameTests, and a geometry
self-check of the festival (floors, entrance, the Smulguh's podium, floating blocks, light).
"""
import math
import os
import random
from collections import deque

import numpy as np
from PIL import Image

# ======================================================================================================================
# guh clothes: the smul outfit
# ======================================================================================================================
_H = [0, 6, -2]          # (make_guh_variants.HEAD_PIVOT)
_BODY = [0, 6, 6]        # (make_guh_variants.BODY_PIVOT)
BONES = {
    # a bib under the chin: a thin strap round the neck and a rounded bib hanging from it
    "outfit_smul_bib": ("head", _H, "smul_bib", [([-4.5, 2.2, -11.6], [9, 0.5, 0.5], 0), ([-3.2, -0.6, -11.9], [6.4, 2.8, 0.6], 0),
                                                 ([-2.2, -1.1, -11.9], [4.4, 0.5, 0.6], 0)]),
    # a baker's cap: a tall straight paper cap with a flat, slightly wider top (the chef's hat is low and puffy)
    "outfit_smul_baker_hat": ("head", _H, "smul_baker_hat", [([-3.6, 15, -8.6], [7.2, 3.8, 5.2], 0), ([-4.1, 18.8, -9.1], [8.2, 1.0, 6.2], 0)]),
    "outfit_smul_baker_hat_band": ("head", _H, "smul_baker_band", [([-3.8, 15.3, -8.8], [7.6, 0.9, 5.6], 0)]),
    # a pink apron: a skirt round the belly, straps over the back and a big white bow above the tail
    "outfit_smul_apron": ("body", _BODY, "smul_apron", [([-6.5, 1.2, -2.5], [13, 5, 14], 0.7)]),
    "outfit_smul_apron_bow": ("body", _BODY, "smul_apron_bow", [
        ([-4.2, 11.2, -1.8], [1.2, 0.5, 12.5], 0), ([3.0, 11.2, -1.8], [1.2, 0.5, 12.5], 0),
        ([-0.8, 5.0, 12.0], [1.6, 1.6, 0.8], 0), ([-3.2, 4.6, 12.2], [2.4, 2.4, 0.5], 0), ([0.8, 4.6, 12.2], [2.4, 2.4, 0.5], 0),
        ([-1.6, 2.8, 12.3], [1, 2.0, 0.4], 0), ([0.6, 2.8, 12.3], [1, 2.0, 0.4], 0)]),
}
CLOTHES = ["smul_slabbetje", "smul_bakkersmuts", "smul_schort"]


def clothes(rng, v):
    def apron():
        a = v.fabric((245, 128, 178), rng, 8)
        px = v.SWATCH * 4
        a[25:, :] = (252, 250, 248)                          # a white frill along the bottom, scalloped
        for x in range(px):
            if (x // 2) % 2 == 0:
                a[24, x] = (252, 250, 248)
        for (hx, hy) in ((6, 6), (22, 12), (12, 17), (26, 3)):   # little white hearts
            a[hy, hx - 1] = a[hy, hx + 1] = (255, 236, 244)
            a[hy + 1, hx - 1:hx + 2] = (255, 236, 244)
            a[hy + 2, hx] = (255, 236, 244)
        return np.clip(a, 0, 255)

    return {
        "smul_slabbetje": {"smul_bib": lambda: v.dots((252, 250, 246), (240, 120, 170), rng, every=6, size=2)},
        "smul_bakkersmuts": {"smul_baker_hat": lambda: v.fabric((252, 248, 238), rng, 5),
                             "smul_baker_band": lambda: v.fabric((238, 110, 165), rng, 6)},
        "smul_schort": {"smul_apron": apron, "smul_apron_bow": lambda: v.fabric((252, 252, 252), rng, 4)},
    }


def icons(ic):
    pink, dark, white, gold = (240, 120, 170), (160, 50, 100), (252, 250, 246), (250, 200, 60)
    bib = ["....a......a....", "....a......a....", "...abaaaaaaba...", "..abbbbbbbbbba..", ".abbcbbbbbbcbba.", ".abbbbbbbbbbbba.",
           ".abbbbbccbbbbba.", ".abbbbbccbbbbba.", ".abbbbbbbbbbbba.", "..abcbbbbbbcba..", "...abbbbbbbba...", "....aabbbbaa....",
           "......aaaa......"]
    hat = ["...aaaaaaaaaa...", "..abbbbbbbbbba..", "..abbbbbbbbbba..", "...abbbbbbbba...", "...abbbbbbdba...", "...abbbbbbdba...",
           "...abbbbbbbba...", "...acccccccca...", "...acccccccca...", "...aaaaaaaaaa..."]
    apron = ["....a......a....", "....a......a....", "....abbbbbba....", "....abbbbbba....", "....abbccbba....", "aaaaabbbbbbaaaaa",
             "...abbbbbbbbba..", "..abbbbbbbbbbba.", "..abbbbccbbbbba.", "..abbbbccbbbbba.", ".abbbbbbbbbbbbba", ".awwwwwwwwwwwwwa",
             ".awawawawawawawa"]
    return {
        "smul_slabbetje": ic.icon(ic.pad(bib), {"a": dark, "b": white, "c": pink}),
        "smul_bakkersmuts": ic.icon(ic.pad(hat), {"a": (150, 130, 110), "b": white, "c": pink, "d": (225, 220, 210)}),
        "smul_schort": ic.icon(ic.pad(apron), {"a": dark, "b": pink, "c": (255, 200, 225), "w": white}),
    }


# ======================================================================================================================
# resources
# ======================================================================================================================
ICON_ROWS = {
    "smulmunt": ["................", ".....gggggg.....", "...ggyyyyyygg...", "..gyyGyyyyGyyg..", "..gyGkGyyGkGyg..", ".gyyyGyyyyGyyyg.",
                 ".gyyyyyyyyyyyyg.", ".gyPPyymmyyPPyg.", ".gyyyykyykyyyyg.", ".gyyyyykkyyyyyg.", "..gyyyyyyyyyyg..", "..gyyyyyyyyyyg..",
                 "...ggyyyyyygg...", ".....gggggg.....", "................", "................"],
    "smulschaal": ["................", "................", "......y..P......", "....yYyPpwP.....", "...yYgyPwwpPy...", ".kkkkkkkkkkkkkk.",
                   ".kwwwwwwwwwwwwk.", ".kppppppppppppk.", "..kppkppppkppk..", "..kppppmmppppk..", "...kppppppppk...", "....kkkkkkkk....",
                   ".....kmmmmk.....", "....kkkkkkkk....", "................", "................"],
    "gouden_smulknabbel": ["................", "..........w.....", "....yyy..www....", "...yYYYy..w.....", "..yYggGYy.......",
                           "..yggyygGy......", "...yGgyyggy.....", "....yyGggyGy....", ".....ygyyggy..w.", "......yGggGy.www",
                           ".......yyGgy..w.", "........yyy.....", "..w.............", ".www............", "..w.............",
                           "................"],
}
ICON_PAL = {'.': (0, 0, 0, 0), 'k': (70, 30, 50, 255), 'p': (240, 140, 180, 255), 'P': (255, 190, 215, 255),
            'm': (200, 70, 130, 255), 'w': (255, 255, 255, 255), 'g': (220, 150, 30, 255), 'G': (170, 100, 20, 255),
            'y': (255, 215, 80, 255), 'Y': (255, 240, 150, 255)}


def _fix_rows(rows):
    return [(r + "." * 16)[:16] for r in rows]


def build(h):
    # --- items ---
    for name, rows in ICON_ROWS.items():
        h.save(h.grid(_fix_rows(rows), ICON_PAL), "item", f"{name}.png")
        h.item_model(name)
    # --- the invisible arena markers ---
    for marker in ("smul_start", "smul_hoek", "smul_trechter"):
        h.w(f"{h.A}/models/block/{marker}.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
        h.w(f"{h.A}/blockstates/{marker}.json", {"variants": {"": {"model": f"guhs:block/{marker}"}}})
    # --- the Smulguh: a caramel-coloured sitting guh ---
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.07, sat=1.35, val=1.05, only=h.pinkish), "entity", "npc_smulguh.png")

    # --- the structure: rare, big, on flat ground, nothing spawns inside ---
    h.TEMPLATE_SIZES["vadsig_eetfestijn"] = 96
    h.FLATNESS["vadsig_eetfestijn"] = 30
    none = {"bounding_box": "full", "spawns": []}
    h.structure("vadsig_eetfestijn", h.GUHMENSION_LAND, spacing=32, separation=12, salt=20240155,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    s = festijn(h)
    problems = check_festijn(h, s)
    for p in problems:
        print("vadsig_eetfestijn CHECK:", p)
    assert not problems, f"{len(problems)} problems in the vadsig_eetfestijn template"
    s.save("vadsig_eetfestijn")
    test_arena(h)

    # --- advancements: hidden quest ones (granted by the game) and two visible ones ---
    for name in ("smul_gespeeld", "smul_100", "smul_200", "smul_goud"):
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, crit, (en_t, en_d), (nl_t, nl_d) in [
        ("find_vadsig_eetfestijn", "enter_guhmension", "guhs:smulmunt", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:vadsig_eetfestijn"]}}}},
         ("Something smells vadsig...", "Find the Vadsig eetfestijn, the big guh food festival"),
         ("Hier ruikt het vadsig...", "Vind het Vadsig eetfestijn, het grote guh-smulfeest")),
        ("smul_pakje", "find_vadsig_eetfestijn", "guhs:smul_bakkersmuts", "challenge",
         # (one criterion per piece: their progress is kept, so dressing your guh between two purchases still counts)
         {piece: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:smul_{piece}"}]}}
          for piece in ("slabbetje", "bakkersmuts", "schort")},
         ("Ready to smul", "Buy the bib, the baker's hat and the pink apron from the Smulguh"),
         ("Helemaal smulklaar", "Koop het slabbetje, de bakkersmuts en het roze schort bij de Smulguh")),
    ]:
        h.w(f"{h.D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": crit if "trigger" not in crit else {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", en_t, nl_t)
        h.lang(f"advancements.guhs.guhmension.{name}.description", en_d, nl_d)

    for key, (en, nl) in LANG.items():
        h.lang(key, en, nl)


LANG = {
    # names
    "entity.guhs.guh_npc.smulguh": ("Smulguh", "Smulguh"),
    "entity.guhs.smul_hapje": ("Falling snack", "Vallend hapje"),
    "item.guhs.smulmunt": ("Smulmunt", "Smulmunt"),
    "item.guhs.smulschaal": ("Smulschaal (borrowed)", "Smulschaal (geleend)"),
    "item.guhs.smulschaal.lore": ("Borrowed from the Smulguh: it goes back after the game", "Geleend van de Smulguh: na het spel gaat hij terug"),
    "item.guhs.gouden_smulknabbel": ("Golden Smulknabbel", "Gouden smulknabbel"),
    "item.guhs.smul_slabbetje": ("Smul Bib", "Smulslabbetje"),
    "item.guhs.smul_bakkersmuts": ("Vadsig Baker's Hat", "Vadsige bakkersmuts"),
    "item.guhs.smul_schort": ("Pink Smul Apron", "Roze smulschort"),
    "block.guhs.smul_start": ("Eetfestijn start", "Eetfestijn-start"),
    "block.guhs.smul_hoek": ("Eetfestijn arena corner", "Eetfestijn-arenahoek"),
    "block.guhs.smul_trechter": ("Eetfestijn chute end", "Eetfestijn-trechter"),
    "structure.guhs.vadsig_eetfestijn": ("Vadsig eetfestijn", "Vadsig eetfestijn"),
    "structure.guhs.vadsig_eetfestijn.tooltip": ("Minigame: catch falling food with the Smulguh, smulmunten and the smul outfit",
                                                 "Minigame: vang vallend eten bij de Smulguh, smulmunten en het smulpakje"),
    "gui.guhs.smul.no_build": ("Njeg! The eetfestijn is for eating, not for breaking or building.",
                               "Njeg! Op het eetfestijn wordt gesmuld, niet gesloopt of gebouwd."),
    # the Smulguh talks (quest.* is Dutch in the chat: in English it's English)
    "quest.guhs.smul.hello": ("NJEG! Welcome to the Vadsig eetfestijn! Hungry? In my arena food rains from the sky. Catch as much as you can in one minute!",
                              "NJEG! Welkom op het Vadsig eetfestijn! Honger? In mijn arena regent het eten. Vang zoveel je kan in één minuut!"),
    "quest.guhs.smul.busy": ("Shh... someone is smulling right now! Watch from the stands, you're next.",
                             "Sssst... er wordt nu gesmuld! Kijk mee vanaf de tribune, straks ben jij."),
    "quest.guhs.smul.busy_you": ("You're playing! Catch that food, vads!", "Je bent aan het spelen! Vangen, vads!"),
    "quest.guhs.smul.elsewhere": ("You're already smulling somewhere else. One feast at a time!", "Je smult al ergens anders. Eén feestmaal tegelijk!"),
    "quest.guhs.smul.broken": ("Njeg... my arena is broken, the food has nowhere to fall.", "Njeg... mijn arena is kapot, het eten kan nergens vallen."),
    "quest.guhs.smul.full": ("Your pockets are too full! Make room for one thing: my big smulschaal.",
                             "Je zakken zitten te vol! Maak één plekje vrij voor mijn grote smulschaal."),
    "quest.guhs.smul.shop": ("My shop! The smul outfit is only for sale here. VAHOEG!", "Mijn winkeltje! Het smulpakje koop je alleen hier. VAHOEG!"),
    "quest.guhs.smul.go": ("Here's my smulschaal: walk into the food to catch it. Avoid the dark Mika-vet! Golden ones are worth a lot. Ready...?",
                           "Hier is mijn smulschaal: loop tegen het eten aan om het te vangen. Pas op voor het zwarte Mika-vet! Gouden hapjes zijn veel waard. Klaar...?"),
    "quest.guhs.smul.invite1": ("Hungry? Come and smul! Talk to me.", "Trek? Kom smullen! Praat maar met mij."),
    "quest.guhs.smul.invite2": ("Kaasknabbels are falling from the sky today... njeg njeg!", "Vandaag regent het kaasknabbels... njeg njeg!"),
    "quest.guhs.smul.invite3": ("Who dares? One minute, one bowl, all the cake you can catch!", "Wie durft? Eén minuut, één schaal, alle taart die je kan vangen!"),
    "quest.guhs.smul.ready": ("Get ready...", "Klaar voor de start..."),
    "quest.guhs.smul.title.go": ("SMUL!", "SMULLEN!"),
    "quest.guhs.smul.title.go.sub": ("Catch the falling food!", "Vang het vallende eten!"),
    "quest.guhs.smul.cheer.start": ("Go go go! Vads vads vads!", "Hup hup hup! Vads vads vads!"),
    "quest.guhs.smul.cheer.half": ("Half way! Keep smulling, you vadsig champion!", "Nog 30 seconden! Doorsmullen, vadsige kampioen!"),
    "quest.guhs.smul.cheer.ten": ("Ten more seconds! EAT EAT EAT!", "Nog tien seconden! SMULLEN SMULLEN SMULLEN!"),
    "quest.guhs.smul.cheer.mika": ("Bah! Mika-vet! Njeg njeg njeg, spit it out!", "Bah! Mika-vet! Njeg njeg njeg, uitspugen!"),
    "quest.guhs.smul.cheer.gold": ("A GOLDEN SMULKNABBEL! Everything counts double! VAHOEG!", "EEN GOUDEN SMULKNABBEL! Alles telt dubbel! VAHOEG!"),
    "quest.guhs.smul.cheer.combo": ("Ten in a row! What a vadsig stomach!", "Tien achter elkaar! Wat een vadsige buik!"),
    "quest.guhs.smul.mika": ("Bah, Mika-vet! -%s and you're slow...", "Bah, Mika-vet! -%s en je bent traag..."),
    "quest.guhs.smul.gold": ("GOLDEN SMUL MODE: double points!", "GOUDEN SMULMODUS: dubbele punten!"),
    "quest.guhs.smul.combo": ("Combo x%s!", "Combo x%s!"),
    "quest.guhs.smul.caught": ("Yum! +%s  (%s points)", "Smul! +%s  (%s punten)"),
    "quest.guhs.smul.bar": ("Points: %s  -  %s s  -  combo x%s", "Punten: %s  -  nog %s s  -  combo x%s"),
    "quest.guhs.smul.bar.gold": ("GOLDEN! Points: %s  -  %s s  -  combo x%s (x2)", "GOUD! Punten: %s  -  nog %s s  -  combo x%s (x2)"),
    "quest.guhs.smul.done": ("Time's up! You smulled %s points (%s snacks caught, %s golden, longest combo %s).",
                             "Tijd om! Je smulde %s punten (%s hapjes gevangen, %s gouden, langste combo %s)."),
    "quest.guhs.smul.munten": ("You get %s smulmunt(en).", "Je krijgt %s smulmunt(en)."),
    "quest.guhs.smul.first": ("Your very first eetfestijn! Here's a welcome treat: a guh cake and %s extra smulmunten. Njeg!",
                              "Je allereerste eetfestijn! Een welkomstcadeautje: een guhtaart en %s extra smulmunten. Njeg!"),
    "quest.guhs.smul.record": ("New record: %s points! VAHOEG!", "Nieuw record: %s punten! VAHOEG!"),
    "quest.guhs.smul.best": ("Your record is still %s points.", "Je record staat nog op %s punten."),
    "quest.guhs.smul.title.record": ("NEW RECORD!", "NIEUW RECORD!"),
    "quest.guhs.smul.title.end": ("Time's up!", "Tijd om!"),
    "quest.guhs.smul.title.points": ("%s points", "%s punten"),
    "quest.guhs.smul.end.super": ("I've never seen anything this vadsig! You're the smulkampioen of the Guhmension!",
                                  "Zoiets vadsigs heb ik nog nooit gezien! Jij bent de smulkampioen van de Guhmensie!"),
    "quest.guhs.smul.end.good": ("Wow, what a feast! My tummy rumbles just watching you.", "Wauw, wat een feestmaal! Mijn buikje rommelt ervan."),
    "quest.guhs.smul.end.ok": ("Tasty! Come back and play again, practice makes vads.", "Lekker gesmuld! Kom nog eens spelen, oefening baart vads."),
    "quest.guhs.smul.stopped": ("You stopped smulling (%s points, no smulmunten). The smulschaal went back to the Smulguh.",
                                "Je bent gestopt met smullen (%s punten, geen smulmunten). De smulschaal ging terug naar de Smulguh."),
    # the world's top 3, floating above the Smulguh (nl.juiced.guhs.quest.Scorebord)
    "gui.guhs.scorebord.smul": ("Top 3 vadsig smullers", "Top 3 vadsigste smullers"),
    "gui.guhs.scorebord.smul.punten": ("Most points in one minute", "Meeste punten in één minuut"),
    # the screen
    "gui.guhs.smul.play": ("Smul!", "Smullen!"),
    "gui.guhs.smul.play.tooltip": ("To the arena: one minute of catching food. You get a smulschaal to borrow",
                                   "Naar de arena: één minuut eten vangen. Je krijgt een smulschaal te leen"),
    "gui.guhs.smul.shop": ("Shop", "Winkeltje"),
    "gui.guhs.smul.shop.tooltip": ("The smul outfit for your guh (bib, baker's hat, pink apron) and treats, for smulmunten",
                                   "Het smulpakje voor je guh (slabbetje, bakkersmuts, roze schort) en lekkers, voor smulmunten"),
    "gui.guhs.smul.rules": ("One minute of food falling from the sky and out of the chutes. Walk into it to catch it! Kaasknabbel 1, cupcake 2, "
                            "macaron and milkshake 3, guh cake 5, golden smulknabbel 10 (and double points for a while). Five in a row: combo x2, "
                            "ten: x3. Mika-vet: -5 and slow! The dots on the floor show where it lands.",
                            "Eén minuut lang valt er eten uit de lucht en uit de trechters. Loop ertegenaan om het te vangen! Kaasknabbel 1, "
                            "cupcake 2, macaron en milkshake 3, guhtaart 5, gouden smulknabbel 10 (en even dubbele punten). Vijf op rij: combo x2, "
                            "tien: x3. Mika-vet: -5 en traag! De stipjes op de vloer tonen waar het landt."),
    "gui.guhs.smul.busy": ("%s is smulling now (%s s left, %s points). Watch from the stands!",
                           "%s is nu aan het smullen (nog %s s, %s punten). Kijk mee vanaf de tribune!"),
    "gui.guhs.smul.first": ("Your first time? You'll get a welcome treat afterwards!", "Je eerste keer? Na afloop krijg je een welkomstcadeautje!"),
    "gui.guhs.smul.free": ("The arena is free. Hungry?", "De arena is vrij. Trek?"),
    "gui.guhs.smul.record": ("Your record: %s points  -  played %s times", "Jouw record: %s punten  -  %s keer gespeeld"),
}


# ======================================================================================================================
# the festival template
# ======================================================================================================================
W, H, D = 96, 38, 96
G = 1                                   # floor layer (you walk at y = 2)
HX0, HX1, HZ0, HZ1 = 8, 87, 4, 75       # the tent hall (walls)
WALL_TOP = 12                           # the walls go up to here, the roof starts one higher
AX0, AX1, AZ0, AZ1 = 36, 59, 28, 51     # the arena floor (inside its glass walls)
START = (47, G + 1, 39)
CHUTES = [(40, 32), (55, 32), (40, 47), (55, 47)]
CHUTE_Y = G + 17                        # the chute markers (where the food drops out)
NPC = (47.5, G + 2.0, 55.5)             # the Smulguh on her podium (facing south, towards the door)
DOOR_X = (45, 50)                       # the mouth of the facade: the way in
FOUNTAIN = (47.5, 87.5)

ONE = {"waterlogged": "false"}
LAMP = {"hanging": "false", "waterlogged": "false"}
HANG = {"hanging": "true", "waterlogged": "false"}


def fence_props(**sides):
    p = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    p.update({k: "true" for k, v in sides.items() if v})
    return p


def inset(x, z):
    return min(x - HX0, HX1 - x, z - HZ0, HZ1 - z)


def roof_y(x, z):
    return WALL_TOP + 1 + inset(x, z) // 2


def guh_face(u, v, r):
    """What's at (u, v) in a guh face of radius r seen from the front / from above (v up = the top of the head).
    Returns None (outside), or one of: face, rim, ear, ear_in, eye, shine, nose, mouth, muzzle, cheek."""
    for sx in (-1, 1):
        ex, ey = sx * r * 0.36, r * 0.2
        if math.hypot(u - ex, v - ey) <= max(1.0, r * 0.17):
            return "shine" if (u - ex) * sx < -0.2 and v - ey > 0.2 and r >= 6 and math.hypot(u - ex, v - ey) < r * 0.1 + 0.2 else "eye"
    if abs(u) <= max(0.8, r * 0.1) and -r * 0.12 <= v <= r * 0.02:
        return "nose"
    for sx in (-1, 1):                           # the mouth: a little "w" under the nose
        mu = u - sx * r * 0.12
        if abs(math.hypot(mu, v + r * 0.12) - r * 0.13) < 0.55 and v < -r * 0.12 and abs(u) <= r * 0.26:
            return "mouth"
    for sx in (-1, 1):
        if math.hypot(u - sx * r * 0.6, v + r * 0.2) <= max(0.8, r * 0.13):
            return "cheek"
    d = math.hypot(u, v)
    if d <= r:
        if math.hypot(u / 1.25, v + r * 0.22) <= r * 0.3:
            return "muzzle"
        return "rim" if d > r - 1.0 else "face"
    for sx in (-1, 1):
        de = math.hypot(u - sx * r * 0.68, v - r * 0.74)
        if de <= r * 0.38:
            return "ear_in" if de <= r * 0.22 else "ear_rim" if de > r * 0.38 - 1 else "ear"
    return None


FACE_BLOCKS = {"face": "pink_concrete", "rim": "magenta_concrete", "ear": "pink_concrete", "ear_rim": "magenta_concrete", "ear_in": "pink_wool",
               "eye": "black_concrete", "shine": "white_concrete", "nose": "magenta_concrete", "mouth": "black_concrete",
               "muzzle": "white_concrete", "cheek": "guhs:guh_kristal_lamp"}


def face_block(kind, overrides=None):
    name = (overrides or {}).get(kind, FACE_BLOCKS[kind])
    return name if ":" in name else "minecraft:" + name


def festijn(h):
    mc, S = h.mc, h.Structure
    s = S((W, H, D))
    rng = random.Random(24155)
    fp = [(x, z) for x in range(W) for z in range(D)]

    # --- ground: dirt, grass all round, the plaza paving ---
    for x, z in fp:
        s.set(x, 0, z, mc("dirt"))
        s.set(x, G, z, mc("grass_block"), {"snowy": "false"})

    # --- the tent hall: floor, striped canvas walls with a few windows, the striped pyramid roof ---
    for x in range(HX0, HX1 + 1):
        for z in range(HZ0, HZ1 + 1):
            s.set(x, G, z, mc("cherry_planks") if (x + z) % 7 else mc("stripped_cherry_wood"), None if (x + z) % 7 else {"axis": "y"})
    for x in range(HX0, HX1 + 1):
        for z in range(HZ0, HZ1 + 1):
            if inset(x, z) != 0:
                continue
            corner = x in (HX0, HX1) and z in (HZ0, HZ1)
            along = x if z in (HZ0, HZ1) else z
            for y in range(G, WALL_TOP + 1):
                if corner:
                    block = mc("yellow_concrete") if y % 4 == 0 else mc("white_concrete")
                elif y == G or y == G + 1:
                    block = mc("magenta_concrete") if y == G else mc("pink_terracotta")
                elif y == WALL_TOP:
                    block = mc("yellow_concrete")
                elif 8 <= y <= 10 and along % 8 in (3, 4):
                    block = mc("pink_stained_glass")
                else:
                    block = mc("pink_wool") if (along // 2) % 2 else mc("white_wool")
                s.set(x, y, z, block)
    cx, cz = (HX0 + HX1) / 2, (HZ0 + HZ1) / 2
    for x in range(HX0, HX1 + 1):
        for z in range(HZ0, HZ1 + 1):
            i = inset(x, z)
            y = roof_y(x, z)
            if i == 0:
                block = mc("yellow_wool")
            else:
                sector = int((math.atan2(z - cz, x - cx) + math.pi) / (2 * math.pi) * 32) % 2
                block = mc("pink_wool") if sector else mc("white_wool")
            s.set(x, y, z, block)
            if i > 0 and i % 2 == 0:                        # (a second block under the outer edge of each ring: no gaps)
                s.set(x, y - 1, z, block)
    top = max(roof_y(x, z) for x in range(HX0, HX1 + 1) for z in range(HZ0, HZ1 + 1))
    ridge = [(x, z) for x in range(HX0, HX1 + 1) for z in range(HZ0, HZ1 + 1) if roof_y(x, z) == top]
    for (x, z) in (min(ridge), max(ridge)):                    # flag poles with a lampgion on each end of the ridge
        s.fill(x, top + 1, z, x, top + 3, z, mc("cherry_fence"), fence_props())
        s.set(x, top + 4, z, "guhs:lampion_roze", LAMP)
    # vlaggetjes along the top of the walls, inside, under the edge of the roof
    for x in range(HX0 + 1, HX1):
        for z in (HZ0 + 1, HZ1 - 1):
            if x % 3:
                s.set(x, WALL_TOP, z, "guhs:vlaggetjes", {"axis": "x"})
    for z in range(HZ0 + 1, HZ1):
        for x in (HX0 + 1, HX1 - 1):
            if z % 3:
                s.set(x, WALL_TOP, z, "guhs:vlaggetjes", {"axis": "z"})
    # wall lanterns (on a little post against the wall) all round the inside
    for x in range(HX0 + 3, HX1 - 2, 6):
        for z, dz in ((HZ0 + 1, 1), (HZ1 - 1, -1)):
            if not DOOR_X[0] - 3 <= x <= DOOR_X[1] + 3:
                wall_lamp(s, h, x, z)
    for z in range(HZ0 + 3, HZ1 - 2, 6):
        for x in (HX0 + 1, HX1 - 1):
            wall_lamp(s, h, x, z)

    # --- the facade: a giant guh face on the south wall, its mouth is the door ---
    fcx, fcy, fr = 47.5, 13.0, 11.5
    for x in range(int(fcx - fr - 5), int(fcx + fr + 6)):
        for y in range(G + 1, H):
            kind = guh_face(x - fcx, y - fcy, fr)
            if kind is None:
                continue
            for z in (HZ1, HZ1 + 1):
                if z == HZ1 + 1 and kind in ("ear", "ear_in", "ear_rim") and y < WALL_TOP:
                    continue
                s.set(x, y, z, face_block(kind))
    # the ears stand on the wall: fill under them down to the wall top so nothing floats
    for x in range(int(fcx - fr - 5), int(fcx + fr + 6)):
        ys = [y for y in range(WALL_TOP + 1, H) if s.get(x, y, HZ1) not in (None, "minecraft:air")]
        if ys:
            for y in range(WALL_TOP + 1, min(ys)):
                s.set(x, y, HZ1, mc("pink_concrete"))
    # the door: the open mouth, with a golden frame and lanterns
    for x in range(DOOR_X[0], DOOR_X[1] + 1):
        for y in range(G + 1, G + 6):
            for z in (HZ1, HZ1 + 1):
                s.set(x, y, z, mc("air"))
        for z in (HZ1, HZ1 + 1):
            s.set(x, G, z, mc("yellow_concrete"))
            s.set(x, G + 6, z, mc("yellow_concrete"))
    for y in range(G + 1, G + 7):
        for z in (HZ1, HZ1 + 1):
            s.set(DOOR_X[0] - 1, y, z, mc("yellow_concrete"))
            s.set(DOOR_X[1] + 1, y, z, mc("yellow_concrete"))
    for x in (DOOR_X[0] - 2, DOOR_X[1] + 2):
        s.set(x, G + 4, HZ1 + 2, mc("lantern"), HANG)
        s.set(x, G + 5, HZ1 + 2, mc("yellow_concrete"))

    # --- the arena: guh-face floor, glass walls, the markers and the four food chutes ---
    arena(s, h)
    tribunes(s, h)
    podium(s, h)
    lobby(s, h, rng)
    north_hall(s, h, rng)
    plaza(s, h, rng)

    # the Smulguh
    s.entity(NPC[0], NPC[1], NPC[2], {"id": "guhs:guh_npc", "Kind": "smulguh", "PersistenceRequired": h.Byte(1),
                                      "Rotation": h.floats(0.0, 0.0)})
    s.clear_above(fp, G + 1)
    return s


def wall_lamp(s, h, x, z):
    s.set(x, G + 5, z, h.mc("yellow_concrete"))
    s.set(x, G + 4, z, h.mc("lantern"), HANG)


def lamp_post(s, h, x, z, colour="roze", height=3, y0=None):
    y0 = G + 1 if y0 is None else y0
    s.fill(x, y0, z, x, y0 + height - 1, z, h.mc("cherry_fence"), fence_props())
    s.set(x, y0 + height, z, f"guhs:lampion_{colour}", LAMP)


def arena(s, h):
    mc = h.mc
    acx, acz = (AX0 + AX1) / 2, (AZ0 + AZ1) / 2
    r = 9.6
    for x in range(AX0, AX1 + 1):
        for z in range(AZ0, AZ1 + 1):
            kind = guh_face(x - acx, acz - z, r)          # (the top of the head points north)
            edge = min(x - AX0, AX1 - x, z - AZ0, AZ1 - z)
            if kind is not None:
                block = face_block(kind)
            elif edge == 0:
                block = mc("ochre_froglight") if (x + z) % 3 == 0 else mc("yellow_concrete")
            else:
                block = mc("white_concrete") if (x + z) % 2 else mc("white_terracotta")
            s.set(x, G, z, block, {"axis": "y"} if block.endswith("froglight") else None)
    # glass walls with white posts and a rail on top; a door in the south wall, left of the podium
    for x in range(AX0 - 1, AX1 + 2):
        for z in range(AZ0 - 1, AZ1 + 2):
            if AX0 <= x <= AX1 and AZ0 <= z <= AZ1:
                continue
            s.set(x, G, z, mc("magenta_concrete"))
            along = x if z in (AZ0 - 1, AZ1 + 1) else z
            post = (along - AX0) % 4 == 0 or (x in (AX0 - 1, AX1 + 1) and z in (AZ0 - 1, AZ1 + 1))
            for y in range(G + 1, G + 5):
                s.set(x, y, z, mc("white_concrete") if post else mc("pink_stained_glass"))
            s.set(x, G + 5, z, mc("white_concrete"))
            s.set(x, G + 6, z, "guhs:lampion_roze" if post else mc("air"), LAMP if post else None)
    for x in (41, 42):                                            # the door
        for y in range(G + 1, G + 4):
            s.set(x, y, AZ1 + 1, mc("air"))
        s.set(x, G, AZ1 + 1, mc("yellow_concrete"))
    # markers: the start in the middle, two opposite corners
    s.set(*START, "guhs:smul_start")
    s.set(AX0, G + 1, AZ0, "guhs:smul_hoek")
    s.set(AX1, G + 1, AZ1, "guhs:smul_hoek")
    # the chutes: pink glass tubes hanging from the tent roof, a golden rim at the bottom, a funnel of kaasknabbels on the roof
    for (x, z) in CHUTES:
        top = min(roof_y(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1))
        for y in range(CHUTE_Y + 1, top + 1):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if dx == 0 and dz == 0:
                        s.set(x, y, z, mc("air") if y < roof_y(x, z) else mc("yellow_concrete"))
                    elif y == CHUTE_Y + 1:
                        s.set(x + dx, y, z + dz, mc("yellow_concrete"))
                    elif y < roof_y(x + dx, z + dz):
                        s.set(x + dx, y, z + dz, mc("pink_stained_glass"))
        s.set(x, CHUTE_Y, z, "guhs:smul_trechter")
        for dx in (-1, 0, 1):                                     # the funnel on the roof
            for dz in (-1, 0, 1):
                ry = roof_y(x + dx, z + dz)
                for y in range(ry + 1, top + 2):
                    s.set(x + dx, y, z + dz, mc("yellow_concrete"))
                s.set(x + dx, top + 2, z + dz, "guhs:block_of_kaasknabbels" if dx == 0 and dz == 0 else mc("yellow_concrete"))
        s.set(x, top + 3, z, "guhs:block_of_kaasknabbels")


def tribunes(s, h):
    """Stands along the east and west glass walls: five rows of cherry-stair seats facing the arena."""
    mc = h.mc
    for side in (1, -1):
        base = AX1 + 3 if side == 1 else AX0 - 3
        for row in range(5):
            y = G + 1 + row
            seat_x = base + side * 2 * row
            back_x = seat_x + side
            for z in range(AZ0 + 1, AZ1):
                if z in (AZ0 + 1 + 10, AZ0 + 2 + 10):            # an aisle up the middle: plain steps
                    s.fill(seat_x, G + 1, z, seat_x, y, z, mc("pink_terracotta"))
                    s.fill(back_x, G + 1, z, back_x, y, z, mc("pink_terracotta"))
                    continue
                s.fill(seat_x, G + 1, z, seat_x, y - 1, z, mc("pink_terracotta"))
                s.set(seat_x, y, z, mc("cherry_stairs"), {"facing": "east" if side == 1 else "west", "half": "bottom",
                                                           "shape": "straight", "waterlogged": "false"})
                s.fill(back_x, G + 1, z, back_x, y, z, mc("white_concrete") if row % 2 else mc("pink_concrete"))
        # a railing behind the top row, lampgions on it
        rail_x = base + side * 10
        for z in range(AZ0 + 1, AZ1):
            s.fill(rail_x, G + 1, z, rail_x, G + 5, z, mc("pink_terracotta"))
            s.set(rail_x, G + 6, z, mc("cherry_fence"), fence_props(north=z > AZ0 + 1, south=z < AZ1 - 1))
        for z in (AZ0 + 1, AZ0 + 8, AZ0 + 15, AZ1 - 1):
            s.set(rail_x, G + 7, z, "guhs:lampion_geel", LAMP)
        # the ends of the stands: a pink wall, so the rows don't hang in the air
        for z in (AZ0, AZ1):
            for k in range(11):
                s.fill(base + side * k, G + 1, z, base + side * k, G + 1 + min(k // 2, 4), z, mc("magenta_concrete"))


def podium(s, h):
    """The Smulguh's podium in front of the arena: gold under her, pink steps, kaasknabbel pillars with lamps."""
    mc = h.mc
    nx, nz = int(NPC[0]), int(NPC[2])
    for x in range(nx - 2, nx + 3):
        for z in range(nz - 2, nz + 2):
            s.set(x, G + 1, z, mc("pink_concrete") if max(abs(x - nx), abs(z - nz)) == 2 or z == nz - 2 else mc("white_concrete"))
    s.set(nx, G + 1, nz, mc("gold_block"))
    for x in range(nx - 2, nx + 3):
        s.set(x, G + 1, nz + 2, mc("cherry_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for x in (nx - 3, nx + 3):
        s.fill(x, G + 1, nz - 1, x, G + 3, nz - 1, "guhs:block_of_kaasknabbels")
        s.set(x, G + 4, nz - 1, "guhs:lampion_geel", LAMP)


def lobby(s, h, rng):
    """The south part of the hall: a guh-face floor behind the door, tables with cake counters, lamp posts."""
    mc = h.mc
    fcx, fcz, r = 47.5, 66.5, 6.3
    for x in range(int(fcx - r - 3), int(fcx + r + 4)):
        for z in range(int(fcz - r - 3), int(fcz + r + 4)):
            kind = guh_face(x - fcx, fcz - z, r)
            if kind is not None:
                block = face_block(kind, {"cheek": "pearlescent_froglight", "rim": "yellow_concrete"})
                s.set(x, G, z, block, {"axis": "y"} if block.endswith("froglight") else None)
    # picnic corners: tables with chairs, flower pots on the tables
    for (tx, tz) in ((20, 60), (28, 66), (20, 70), (67, 60), (75, 66), (67, 70), (34, 58), (61, 58)):
        s.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "north"})
        s.set(tx, G + 2, tz, rng.choice(["guhs:potted_kaasbloem", "guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos", "guhs:potted_guhoortjes"]))
        for (dx, dz, facing) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
            s.set(tx + dx, G + 1, tz + dz, "guhs:guh_stoel", {"facing": facing})
    # snack counters along the side walls: white counters with cakes, kaasknabbels and baskets
    for x0 in (HX0 + 2, HX1 - 5):
        for z in range(56, 72):
            s.set(x0 + 1, G + 1, z, mc("white_concrete") if z % 5 else mc("pink_concrete"))
            s.set(x0 + 2, G + 1, z, mc("white_concrete") if z % 5 else mc("pink_concrete"))
            goods = ["guhs:guh_taart", "guhs:block_of_kaasknabbels", "guhs:knabbelkorf", "guhs:guh_taart", mc("cake")]
            g = goods[z % len(goods)]
            if z % 3 == 0:
                s.set(x0 + 1 + (z % 2), G + 2, z, g, {"bites": "0"} if g in ("guhs:guh_taart", mc("cake")) else
                      ({"facing": "east" if x0 < 40 else "west", "honey_level": "0"} if g == "guhs:knabbelkorf" else None))
    # lamp posts in the lobby
    for (x, z) in ((38, 62), (57, 62), (38, 71), (57, 71), (24, 56), (71, 56), (14, 64), (81, 64), (30, 73), (65, 73)):
        lamp_post(s, h, x, z, ("roze", "geel", "mint")[(x + z) % 3])


def north_hall(s, h, rng):
    """Behind the arena: the giant guh cake, two giant milkshakes and more tables."""
    mc = h.mc
    ccx, ccz = 47.5, 15.5
    tiers = [(7.2, 4, "pink_wool", "white_wool"), (5.2, 3, "white_wool", "pink_wool"), (3.2, 2, "pink_wool", "white_wool")]
    y = G + 1
    for r, height, body, frosting in tiers:
        for x in range(int(ccx - r - 1), int(ccx + r + 2)):
            for z in range(int(ccz - r - 1), int(ccz + r + 2)):
                d = math.hypot(x - ccx, z - ccz)
                if d <= r:
                    for yy in range(y, y + height):
                        drip = d > r - 1 and yy == y + height - 1 or (d > r - 1 and yy == y + height - 2 and (x * 3 + z) % 4 == 0)
                        s.set(x, yy, z, mc(frosting if drip else body))
        y += height
    # candles on top (with a lampgion in the middle), a guh face on the front of the bottom tier
    for (dx, dz) in ((-1, -1), (1, 1), (-1, 1), (1, -1)):
        s.set(int(ccx + dx * 1.5), y, int(ccz + dz * 1.5), mc("pink_candle"), {"candles": "3", "lit": "false", "waterlogged": "false"})
    s.set(int(ccx), y, int(ccz), "guhs:lampion_roze", LAMP)
    for (x, yy, block) in ((45, G + 3, "black_concrete"), (50, G + 3, "black_concrete"), (47, G + 2, "magenta_concrete"),
                           (48, G + 2, "magenta_concrete"), (46, G + 1, "black_concrete"), (49, G + 1, "black_concrete"),
                           (44, G + 2, "pink_terracotta"), (51, G + 2, "pink_terracotta")):
        zf = max(z for z in range(int(ccz), int(ccz) + 9) if s.get(x, yy, z) in (mc("pink_wool"), mc("white_wool")))
        s.set(x, yy, zf, mc(block))
    # the giant milkshakes: a glass with a pink shake, whipped cream and a straw
    for (mx, mz) in ((22, 14), (73, 14)):
        for yy in range(G + 1, G + 7):
            for x in range(mx - 2, mx + 3):
                for z in range(mz - 2, mz + 3):
                    d = math.hypot(x - mx, z - mz)
                    if d <= 2.3:
                        if yy == G + 1:
                            s.set(x, yy, z, mc("white_concrete"))
                        elif d > 1.5:
                            s.set(x, yy, z, mc("white_stained_glass"))
                        else:
                            s.set(x, yy, z, mc("pink_concrete") if yy < G + 5 else mc("white_wool"))
        s.set(mx, G + 7, mz, mc("white_wool"))
        s.fill(mx + 1, G + 7, mz, mx + 1, G + 10, mz, mc("red_concrete"))
        s.set(mx + 1, G + 11, mz, mc("red_concrete"))
        s.set(mx + 2, G + 11, mz, mc("red_concrete"))
    # tables around the cake
    for (tx, tz) in ((30, 10), (30, 20), (65, 10), (65, 20), (14, 22), (81, 22)):
        s.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "north"})
        s.set(tx, G + 2, tz, rng.choice(["guhs:potted_kaasbloem", "guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos"]))
        for (dx, dz, facing) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
            s.set(tx + dx, G + 1, tz + dz, "guhs:guh_stoel", {"facing": facing})
    for (x, z) in ((38, 8), (57, 8), (38, 23), (57, 23), (14, 8), (81, 8), (14, 36), (81, 36), (14, 46), (81, 46), (18, 19), (77, 19),
                   (26, 25), (69, 25), (22, 8), (73, 8), (33, 15), (62, 15)):
        lamp_post(s, h, x, z, ("roze", "geel", "mint")[(x + z) % 3])
    # bean bags to watch from the back
    for i, x in enumerate(range(40, 56, 3)):
        s.set(x, G + 1, 25, f"guhs:{['pink', 'white', 'magenta', 'yellow'][i % 4]}_zitzak", {"facing": "south"})


def stall(s, h, x0, z0, awning, goods):
    """A 7x5 food stall with its counter on the south side (towards the path) and a striped awning."""
    mc = h.mc
    for x in range(x0, x0 + 7):
        for z in range(z0, z0 + 5):
            s.set(x, G, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
    s.fill(x0, G + 1, z0, x0 + 6, G + 3, z0, mc(awning + "_concrete"))                 # back wall
    for x in (x0, x0 + 6):
        s.fill(x, G + 1, z0 + 4, x, G + 3, z0 + 4, mc("cherry_fence"), fence_props())
        s.fill(x, G + 1, z0 + 1, x, G + 3, z0 + 1, mc("cherry_fence"), fence_props())
    s.fill(x0 + 1, G + 1, z0 + 4, x0 + 5, G + 1, z0 + 4, mc("white_concrete"))      # the counter
    for x in range(x0 - 1, x0 + 8):
        for z in range(z0, z0 + 6):
            s.set(x, G + 4, z, mc(awning + "_wool") if (x // 1) % 2 else mc("white_wool"))
    s.set(x0 + 3, G + 3, z0 + 2, "guhs:lampion_geel", HANG)
    for i, g in enumerate(goods):
        name, props = g if isinstance(g, tuple) else (g, None)
        s.set(x0 + 1 + i, G + 2, z0 + 4, name, props)


def plaza(s, h, rng):
    """In front of the tent: paving, the path to the mouth, four food stalls, the guh-face kaassaus fountain, lamps and flags."""
    mc = h.mc
    for x in range(4, W - 4):
        for z in range(HZ1 + 2, D - 1):
            s.set(x, G, z, mc("smooth_quartz") if (x + z) % 2 else mc("pink_concrete"))
    for x in range(DOOR_X[0] - 1, DOOR_X[1] + 2):
        for z in range(HZ1 + 2, D):
            s.set(x, G, z, mc("yellow_concrete") if x in (DOOR_X[0] - 1, DOOR_X[1] + 1) else mc("white_concrete"))
    # the fountain: a guh face seen from above, filled with kaassaus
    fx, fz = FOUNTAIN
    r = 5.6
    for x in range(int(fx - r - 3), int(fx + r + 4)):
        for z in range(int(fz - r - 4), int(fz + r + 3)):
            kind = guh_face(x - fx, fz - z, r)
            if kind is None:
                continue
            s.set(x, G, z, mc("pink_concrete"))
            if kind in ("rim", "ear", "ear_rim"):
                s.set(x, G + 1, z, mc("pink_concrete") if kind == "ear" else mc("magenta_concrete"))
            elif kind == "ear_in":
                s.set(x, G + 1, z, mc("pink_terracotta"))
            elif kind in ("eye", "nose", "cheek", "shine"):
                s.set(x, G + 1, z, face_block(kind, {"cheek": "pearlescent_froglight"}))
            else:
                s.set(x, G + 1, z, "guhs:kaas_saus", {"level": "0"})
    # the four stalls
    stall(s, h, 12, 78, "yellow", ["guhs:block_of_kaasknabbels", ("guhs:knabbelkorf", {"facing": "south", "honey_level": "0"}),
                                   "guhs:block_of_kaasknabbels", ("guhs:knabbelkorf", {"facing": "south", "honey_level": "0"}),
                                   "guhs:block_of_kaasknabbels"])
    stall(s, h, 25, 78, "pink", [("guhs:guh_taart", {"bites": "0"}), (mc("cake"), {"bites": "0"}), ("guhs:guh_taart", {"bites": "0"}),
                                 (mc("cake"), {"bites": "0"}), ("guhs:guh_taart", {"bites": "0"})])
    stall(s, h, 64, 78, "magenta", [mc("pink_stained_glass"), mc("white_stained_glass"), mc("pink_stained_glass"),
                                    mc("white_stained_glass"), mc("pink_stained_glass")])
    stall(s, h, 77, 78, "lime", [mc("pink_concrete"), mc("lime_concrete"), mc("yellow_concrete"), mc("brown_concrete"), mc("pink_concrete")])
    # giant macarons and a milkshake cup next to the stalls
    for (mx, mz, c) in ((34, 90, "pink"), (60, 90, "lime"), (86, 90, "yellow"), (9, 90, "brown")):
        for x in range(mx - 1, mx + 2):
            for z in range(mz - 1, mz + 2):
                s.set(x, G + 1, z, mc(c + "_concrete"))
                s.set(x, G + 2, z, mc("white_wool"))
                s.set(x, G + 3, z, mc(c + "_concrete"))
    # lamp posts along the path with vlaggetjes strung between them
    for z in range(HZ1 + 4, D - 1, 5):
        for x in (DOOR_X[0] - 6, DOOR_X[1] + 6):
            lamp_post(s, h, x, z, "roze" if z % 2 else "geel", height=4)
    for x in (DOOR_X[0] - 6, DOOR_X[1] + 6):
        for z in range(HZ1 + 4, D - 1):
            if (z - HZ1 - 4) % 5:
                s.set(x, G + 4, z, "guhs:vlaggetjes", {"axis": "z"})
    # flowers round the sides of the tent
    for x, z in [(x, z) for x in range(W) for z in range(D)]:
        outside = not (HX0 - 1 <= x <= HX1 + 1 and HZ0 - 1 <= z <= HZ1 + 2) and z < HZ1 + 2
        if outside and s.get(x, G + 1, z) is None and (x * 7 + z * 3) % 5 == 0:
            s.set(x, G + 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
    for (x, z) in ((3, 3), (3, 40), (3, 72), (92, 3), (92, 40), (92, 72)):
        lamp_post(s, h, x, z, "mint")


def test_arena(h):
    """A small arena for the GameTests: 20x18x20, the Smulguh south of it (see SmulGameTests)."""
    mc = h.mc
    s = h.Structure((20, 18, 20))
    for x in range(20):
        for z in range(20):
            s.set(x, 0, z, mc("white_concrete"))
    s.set(9, 1, 7, "guhs:smul_start")
    s.set(4, 1, 2, "guhs:smul_hoek")
    s.set(14, 1, 12, "guhs:smul_hoek")
    s.set(6, 12, 4, "guhs:smul_trechter")
    s.set(12, 12, 10, "guhs:smul_trechter")
    s.entity(9.5, 1.0, 15.5, {"id": "guhs:guh_npc", "Kind": "smulguh", "PersistenceRequired": h.Byte(1), "Rotation": h.floats(0.0, 0.0)})
    s.save("smul_testarena")


# ======================================================================================================================
# geometry self-check of the festival
# ======================================================================================================================
PASSABLE = ("minecraft:air", "guhs:smul_start", "guhs:smul_hoek", "guhs:smul_trechter", "guhs:vlaggetjes", "guhs:roze_guhbloem",
            "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes", "minecraft:pink_candle")
MARKERS = ("guhs:smul_start", "guhs:smul_hoek", "guhs:smul_trechter")
NOT_STANDABLE = ("fence", "lampion", "lantern", "guh_stoel", "guh_tafel", "zitzak", "potted", "kaas_saus", "vlaggetjes", "candle",
                 "guh_taart", "cake", "knabbelkorf")
LIGHTS = {"lantern": 15, "lampion_": 15, "froglight": 15, "guh_kristal_lamp": 15, "glowstone": 15}
TRANSPARENT = ("glass", "fence", "lantern", "lampion", "stairs", "guh_stoel", "guh_tafel", "zitzak", "potted", "vlaggetjes", "candle",
               "guh_taart", "cake", "roze_guhbloem", "knabbelroos", "kaasbloem", "guhoortjes", "leaves", "smul_", "kaas_saus")


def check_festijn(h, s):
    problems = []
    get = lambda x, y, z: s.get(x, y, z) or "minecraft:air"

    def passable(x, y, z):
        return get(x, y, z) in PASSABLE

    def standable(x, y, z):
        b = get(x, y, z)
        return b not in PASSABLE and not any(k in b for k in NOT_STANDABLE)

    # 1. no holes in the floors: every column has a floor block at y = G (and dirt under it)
    for x in range(W):
        for z in range(D):
            if get(x, G, z) in PASSABLE or get(x, 0, z) in PASSABLE:
                problems.append(f"hole in the floor at {x},{z}")
    # 2. the Smulguh stands on solid ground with room above her, and faces the door
    nx, ny, nz = int(NPC[0]), int(NPC[1]), int(NPC[2])
    if not standable(nx, ny - 1, nz) or not passable(nx, ny, nz) or not passable(nx, ny + 1, nz):
        problems.append("the Smulguh has no proper spot on her podium")
    # 3. walk from the edge of the plaza: the Smulguh (in front of her), the arena start and the stands must be reachable
    start = (47, G + 1, D - 1)
    seen = {start}
    todo = deque([start])
    while todo:
        x, y, z = todo.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx2, nz2 = x + dx, z + dz
            if not (0 <= nx2 < W and 0 <= nz2 < D):
                continue
            for dy in (0, 1, -1, -2, -3):
                ty = y + dy
                if ty < 1 or ty + 1 >= H:
                    continue
                if dy == 1 and not passable(x, y + 2, z):
                    continue
                if passable(nx2, ty, nz2) and passable(nx2, ty + 1, nz2) and standable(nx2, ty - 1, nz2) and \
                        all(passable(nx2, yy, nz2) for yy in range(ty, y + 2)):
                    p = (nx2, ty, nz2)
                    if p not in seen:
                        seen.add(p)
                        todo.append(p)
                    break
    targets = {"in front of the Smulguh": (47, G + 1, 58), "the arena start": START, "the top row of the east stands": (AX1 + 3 + 9, G + 6, 35),
               "the top row of the west stands": (AX0 - 3 - 9, G + 6, 35), "the giant cake": (47, G + 1, 24), "the snack counters": (HX0 + 6, G + 1, 60)}
    for name, t in targets.items():
        if t not in seen:
            problems.append(f"can't walk to {name} {t}")
    # 4. nothing floats: every block is connected (6-neighbours) to the ground plate
    blocks = {p for p, b in s.blocks.items() if b[0] != "minecraft:air" and b[0] not in MARKERS}
    grounded = {p for p in blocks if p[1] == 0}
    todo = deque(grounded)
    while todo:
        x, y, z = todo.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in blocks and n not in grounded:
                grounded.add(n)
                todo.append(n)
    floating = blocks - grounded
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:5]}")
    # 5. light: block light spreads through see-through blocks; no dark walkable spot under the tent roof
    level = {}
    todo = deque()
    for p, b in s.blocks.items():
        for k, v in LIGHTS.items():
            if k in b[0]:
                level[p] = v
                todo.append(p)
    while todo:
        p = todo.popleft()
        lv = level[p] - 1
        if lv <= 0:
            continue
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
            if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                continue
            b = get(*n)
            if b != "minecraft:air" and not any(t in b for t in TRANSPARENT):
                continue
            if level.get(n, 0) < lv:
                level[n] = lv
                todo.append(n)
    dark = [p for p in seen if HX0 < p[0] < HX1 and HZ0 < p[2] < HZ1 and level.get(p, 0) < 1]
    if dark:
        problems.append(f"{len(dark)} dark spots inside the tent, e.g. {sorted(dark)[:40]}")
    # 6. the arena: a closed floor under the whole drop area, air above it up to the drop height, markers in place
    for x in range(AX0, AX1 + 1):
        for z in range(AZ0, AZ1 + 1):
            if not standable(x, G, z):
                problems.append(f"arena floor missing at {x},{z}")
            for y in range(G + 2, G + 17):
                if not passable(x, y, z):
                    problems.append(f"something in the arena air at {x},{y},{z}: {get(x, y, z)}")
                    break
    for (x, z) in CHUTES:
        if get(x, CHUTE_Y, z) != "guhs:smul_trechter" or not passable(x, CHUTE_Y - 1, z):
            problems.append(f"chute {x},{z} is not open")
    print(f"vadsig_eetfestijn check: {len(seen)} reachable spots, {len(blocks)} blocks, {len(problems)} problems")
    return problems


# ======================================================================================================================
# FTB quests (row y = 36)
# ======================================================================================================================
def ftb(fq):
    q, adv, item, structure = fq.q, fq.adv, fq.item, fq.structure
    q("smul_festijn", "Het Vadsig eetfestijn",
      "Zoek het &dVadsig eetfestijn&r (superkompas: Minigames > Vadsig eetfestijn): een reuzentent met een guhgezicht, vol eten. "
      "Binnen wacht de &dSmulguh&r.", "guhs:guhmensie_superkompas", [structure("vadsig_eetfestijn")], x=-8, y=36)
    q("smul_eerste", "Smullen maar!",
      "Praat met de Smulguh en speel een potje: één minuut lang eten vangen met haar geleende smulschaal. Je hoeft niks mee te nemen!",
      "guhs:smulschaal", [adv("smul_gespeeld")], rewards=(("guhs:kaas_knabbels", 16),), x=-6, y=36)
    q("smul_goud", "Goud waard",
      "Vang een &6gouden smulknabbel&r: 10 punten en een tijdje dubbele punten. VAHOEG!",
      "guhs:gouden_smulknabbel", [adv("smul_goud")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-4, y=36)
    q("smul_100", "Vadsige vangst",
      "Haal 100 punten in één potje. Tip: vijf op rij is combo x2, tien op rij x3. En blijf weg van het Mika-vet!",
      "guhs:guh_cupcake", [adv("smul_100")], rewards=(("guhs:smulmunt", 3),), x=-2, y=36)
    q("smul_200", "Smulkampioen",
      "Haal 200 punten in één potje. De Smulguh heeft nog nooit iets zo vadsigs gezien! "
      "Kom je in de &6top 3&r van de wereld? Die zweeft boven haar hoofd. VAHOEG!",
      "guhs:guh_taart", [adv("smul_200")], rewards=(("guhs:smulmunt", 5),), x=0, y=36, shape="gear")
    q("smul_slabbetje", "Eerst een slabbetje",
      "Koop het smulslabbetje voor je guh bij de Smulguh (smulmunten). Knoeien mag!",
      "guhs:smul_slabbetje", [item("guhs:smul_slabbetje")], x=2, y=36)
    q("smul_pakje", "Helemaal smulklaar",
      "Koop het hele smulpakje: het slabbetje, de vadsige bakkersmuts en het roze smulschort. Alleen te koop op het eetfestijn!",
      "guhs:smul_schort", [adv("guhs:guhmension/smul_pakje")], rewards=(("guhs:gouden_smulknabbel", 2),), x=4, y=36)
