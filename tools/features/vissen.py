"""
The Guhvis-wedstrijd (fishing contest) at the guhvis pond: the Visguh lends you a rod, three minutes of fishing for
special fish (kaasvis, vadsbaars, guhpuffer, njegforel, Mika-meerval, Gouden Guhvis), visbonnen for points and the
angler outfit (vissershoedje, visvest, vis-aan-de-haak) at her stall. Java: nl.juiced.guhs.feature.vissen.

  build(h)   textures, models, lang, advancements, the NPC texture and the structure guhvis_vijver (with a geometry check)
  ftb(fq)    the FTB quests (row y=38)
  BONES / clothes / CLOTHES / icons   the angler outfit on a guh
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

STRUCTURE = "guhvis_vijver"
FISH = ["kaasvis", "vadsbaars", "guhpuffer", "njegforel", "mika_meerval", "gouden_guhvis"]
CLOTHES = ["vissershoedje", "visvest", "vis_aan_de_haak"]

# =====================================================================================================================
# the angler outfit on a guh (make_guh_variants.py): a bucket hat, a vest (the suit bones), and a little rod on the
# back with a fish dangling in front of the guh's nose. They reuse existing swatch spots (each piece has its own texture).
# =====================================================================================================================
_H = [0, 6, -2]          # (make_guh_variants HEAD_PIVOT / BODY_PIVOT)
_B = [0, 6, 6]
BONES = {
    "outfit_vishoed": ("head", _H, "rain_hat", [
        ([-4.5, 15, -9.8], [9, 3, 7.3], 0), ([-4, 18, -9.3], [8, 0.5, 6.3], 0),
        ([-6.5, 14.6, -12.3], [13, 0.6, 2.5], 0), ([-6.5, 14.6, -2.5], [13, 0.6, 2], 0),
        ([-6.5, 14.6, -9.8], [2, 0.6, 7.3], 0), ([4.5, 14.6, -9.8], [2, 0.6, 7.3], 0)]),
    "outfit_vishoed_band": ("head", _H, "santa_trim", [
        ([-4.75, 15, -10.05], [9.5, 0.9, 7.8], 0), ([4.8, 15.9, -7.5], [0.6, 1.3, 0.6], 0)]),
    "outfit_vishengel": ("body", _B, "stethoscope", [
        ([-1, 11, 4], [2, 1.2, 2], 0), ([-0.3, 12, 4.7], [0.6, 9, 0.6], 0),
        ([-0.3, 20.4, -15], [0.6, 0.6, 20.3], 0), ([-0.1, 15.5, -14.9], [0.2, 4.9, 0.2], 0)]),
    "outfit_vishengel_vis": ("body", _B, "pom", [
        ([-0.6, 13.2, -15.6], [1.2, 2.3, 1.6], 0), ([-0.9, 12.4, -15.1], [1.8, 0.8, 0.6], 0)]),
}


def clothes(rng, v):
    def vest():
        a = v.fabric((112, 128, 78), rng, 8)
        a[:, 14:18] = (236, 150, 190)                     # the pink zip
        for (y, x) in ((6, 4), (6, 22), (18, 4), (18, 22)):  # four pockets with a darker flap
            a[y:y + 7, x:x + 7] = (92, 106, 62)
            a[y:y + 2, x:x + 7] = (70, 82, 48)
        a[24:, :] = (236, 150, 190)                       # pink hem
        return np.clip(a, 0, 255)

    def rod():
        a = v.fabric((236, 120, 170), rng, 6)
        a[::6, :] = (250, 240, 245)                       # white rings round the pink rod
        return a

    def kaasvis():
        a = v.fabric((250, 206, 70), rng, 8)
        for (y, x) in ((5, 6), (14, 18), (22, 9), (9, 25)):   # cheese holes
            a[y:y + 3, x:x + 3] = (214, 160, 40)
        return a

    return {
        "vissershoedje": {"rain_hat": lambda: v.band((120, 138, 84), (100, 116, 68), rng, (6, 18)),
                          "santa_trim": lambda: v.stripes((236, 120, 170), (250, 245, 245), rng, 2)},
        "visvest": {"suit": vest},
        "vis_aan_de_haak": {"stethoscope": rod, "pom": kaasvis},
    }


def icons(ic):
    olive, dark, pink, white = (120, 138, 84), (70, 82, 48), (236, 120, 170), (250, 245, 245)
    hat = ic.icon(ic.pad([
        ".....aaaaaa.....", "....abbbbbba....", "....abbbbbba.w..", "...acccccccca.w.", "..abbbbbbbbbbab.",
        ".abbbbbbbbbbbba.", "aaaaaaaaaaaaaaaa"]), {"a": dark, "b": olive, "c": pink, "w": white})
    vest = ic.icon(ic.pad([
        "...aaa....aaa...", "..abbba..abbba..", ".abbbbaccabbbba.", ".abbbbaccabbbba.", ".abddbaccabddba.",
        ".abeebaccabeeba.", ".abbbbaccabbbba.", ".abbbbaccabbbba.", ".abddbaccabddba.", ".abeebaccabeeba.",
        ".abbbbaccabbbba.", ".aaaaaaaaaaaaaa."]), {"a": dark, "b": olive, "c": pink, "d": (92, 106, 62), "e": (70, 82, 48)})
    haak = ic.icon([
        "..............pw", ".............pw.", "............wp..", "...........pw...", "..........wp..l.",
        ".........pw...l.", "........wp....l.", ".......pw.....l.", "......wp.....kyk", ".....pw......kyk",
        "....wp.......kok", "...pw........kyk", "..bb..........k.", ".bbb.........kyk", ".bb.............",
        "................"], {"p": pink, "w": white, "b": (120, 70, 40), "l": (230, 230, 235), "k": (150, 100, 20),
                               "y": (250, 206, 70), "o": (214, 160, 40)})
    return {"vissershoedje": hat, "visvest": vest, "vis_aan_de_haak": haak}


# =====================================================================================================================
# item icons (16x16)
# =====================================================================================================================
FISH_ICONS = {  # a = body, b = belly, d = fins, k = outline, w/e = eye, x = spots
    "kaasvis": ([
        "................", "................", "................", ".....kkkkkk.....", "....kaaaaaak..kk",
        "...kaxaaaxaak.kd", "..kawaaaaaaaakdk", ".kaeaaxaaaxaaddk", ".kaaaaaaaaaaaddk", "..kbbbxbbbbakdk.",
        "...kbbbbbbbak.kd", "....kkkkkkkk..kk", "................", "................", "................",
        "................"], {"a": (250, 206, 70), "b": (255, 232, 140), "d": (230, 150, 40), "x": (214, 160, 40)}),
    "vadsbaars": ([
        "................", "......kkkk......", "....kkdddkk.....", "...kaxaxaxakk...", "..kawaxaxaxaak.k",
        ".kaeaaxaxaxaakdk", ".kaaaaxaxaxaaddk", "kaaaaaxaxaxaaddk", "kbbbbbxbxbxbaddk", ".kbbbbbbbbbbakdk",
        "..kbbbbbbbbbk.kk", "...kkbbbbbkk....", ".....kkkkk......", "................", "................",
        "................"], {"a": (240, 140, 180), "b": (255, 205, 222), "d": (120, 180, 90), "x": (200, 90, 140)}),
    "guhpuffer": ([
        "................", "...d...d...d....", "....kkkkkkk.....", "..dkaaaaaaakd...", "..kaawaaawaak...",
        ".dkaaekaaekaakd.", "..kaaaaaaaaak.kk", ".dkaaaammaaaakdk", "..kaaaaaaaaaakdk", ".dkbbbbbbbbbk.kk",
        "..kbbbbbbbbbkd..", "..dkkbbbbbkk....", "....dkkkkkd.....", "......d.........", "................",
        "................"], {"a": (238, 150, 190), "b": (255, 214, 230), "d": (170, 90, 130), "m": (200, 70, 120)}),
    "njegforel": ([
        "................", "................", "................", "................", ".....kkkkkkkk...",
        "...kkaaaaaaaakk.", ".kkawaxaaxaaaaak", "kaaeaaaaaaaxaadk", "krrrrrrrrrrrrrdk", "kbbbbbbbbbbbbadk",
        ".kkbbbbbbbbbbkkk", "...kkkkkkkkkk...", "................", "................", "................",
        "................"], {"a": (230, 130, 190), "b": (255, 210, 230), "d": (170, 80, 140), "x": (150, 60, 120),
                             "r": (150, 210, 250)}),
    "mika_meerval": ([
        "................", "................", "................", "l...kkkkkkkk....", ".l.kaaaaaaaakk.k",
        "..kaaaaaaaaaaakd", ".kawaaaaaaaaaadk", "lkaeaaaaaaaaaadk", "lkaaaaaaaaaaaadk", ".kbbbbbbbbbbbkdk",
        "l.kkbbbbbbbbk.kd", ".l..kkkkkkkk..kk", "................", "................", "................",
        "................"], {"a": (110, 70, 100), "b": (150, 110, 140), "d": (70, 40, 60), "w": (255, 255, 255),
                             "e": (220, 30, 50), "l": (60, 40, 50)}),
    "gouden_guhvis": ([
        "....y.y.y.......", "....yyyyy.......", "....kkkkk.......", "...kaaaaak...kk.", "..kawbaaaak.kdk.",
        ".kaabkaaaaakddk.", ".kaaaaaaaaaaadk.", ".kdaabbbbaaaadk.", "..kdabbbbbaakdk.", "...kkabbbbak.kk.",
        ".....kkkkkk.....", "...w........w...", "..www......www..", "...w........w...", "................",
        "................"], {"a": (250, 200, 60), "b": (255, 236, 120), "d": (200, 140, 30), "w": (255, 255, 230),
                             "y": (255, 220, 80)}),
}
FISH_COMMON = {".": (0, 0, 0, 0), "k": (40, 20, 30), "w": (255, 255, 255), "e": (30, 30, 60)}

VISBON = [
    "................", "................", "................", "..kkkkkkkkkkkk..", ".kCCCCCCCCCCCCk.",
    ".kCwwCCCCCCwwCk.", ".kC.cccccccc.Ck.", ".kC.ckppppcc.Ck.", ".kC.pwpppppp.Ck.", ".kC.cppppcpc.Ck.",
    ".kC.cccccccc.Ck.", ".kCwwCCCCCCwwCk.", ".kCCCCCCCCCCCCk.", "..kkkkkkkkkkkk..", "................",
    "................"]


def grid16(rows, palette):
    assert len(rows) == 16, len(rows)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            c = palette.get(ch, (0, 0, 0, 0))
            img.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))
    return img


def items(h):
    for name, (rows, pal) in FISH_ICONS.items():
        palette = dict(FISH_COMMON)
        palette.update(pal)
        h.save(grid16(rows, palette), "item", f"{name}.png")
        h.item_model(name)
    h.save(grid16(VISBON, h.ITEM_PAL), "item", "visbon.png")
    h.item_model("visbon")
    # the loaned rod: the vanilla rod in pink, with golden reel pixels
    for suffix in ("", "_cast"):
        rod = h.ramp(h.vanilla(f"item/fishing_rod{suffix}"), (150, 50, 100), (255, 190, 220))
        a = np.asarray(rod).copy()
        van = np.asarray(h.vanilla(f"item/fishing_rod{suffix}"))
        line = (van[..., 3] > 0) & (van[..., :3].min(-1) > 150)          # keep the white fishing line white
        a[line] = van[line]
        h.save(Image.fromarray(a), "item", f"guhvis_hengel{suffix}.png")
    h.w(f"{h.A}/models/item/guhvis_hengel_cast.json", {"parent": "minecraft:item/fishing_rod", "textures": {"layer0": "guhs:item/guhvis_hengel_cast"}})
    h.w(f"{h.A}/models/item/guhvis_hengel.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": "guhs:item/guhvis_hengel"},
                                                  "overrides": [{"predicate": {"cast": 1}, "model": "guhs:item/guhvis_hengel_cast"}]})


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    # items
    "item.guhs.visbon": ("Fish Voucher", "Visbon"),
    "item.guhs.guhvis_hengel": ("Guhfish Rod (on loan)", "Guhvis-hengel (geleend)"),
    "item.guhs.guhvis_hengel.lore": ("Borrowed from the Visguh for the contest. It swims right back afterwards!",
                                     "Geleend van de Visguh voor de wedstrijd. Na afloop zwemt hij vanzelf terug!"),
    "item.guhs.kaasvis": ("Cheese Fish", "Kaasvis"),
    "item.guhs.kaasvis.lore": ("Full of holes, smells like kaasknabbels", "Zit vol gaatjes en ruikt naar kaasknabbels"),
    "item.guhs.vadsbaars": ("Chubby Perch", "Vadsbaars"),
    "item.guhs.vadsbaars.lore": ("The chubbiest fish in the Guhmension. So vads!", "De vadsigste vis van de Guhmensie. Zo vads!"),
    "item.guhs.guhpuffer": ("Guh Puffer", "Guhpuffer"),
    "item.guhs.guhpuffer.lore": ("Blows itself up when it's scared. VAHOEG!", "Blaast zich op als hij schrikt. VAHOEG!"),
    "item.guhs.njegforel": ("Pink Njeg Trout", "Roze njegforel"),
    "item.guhs.njegforel.lore": ("Rare and fast. Says 'njeg' when you catch it", "Zeldzaam en snel. Zegt 'njeg' als je hem vangt"),
    "item.guhs.mika_meerval": ("Mika Catfish", "Mika-meerval"),
    "item.guhs.mika_meerval.lore": ("Grumpy whiskers. Costs points and tastes like Mika. Bah!", "Chagrijnige snorharen. Kost punten en smaakt naar Mika. Bah!"),
    "item.guhs.gouden_guhvis": ("Golden Guhfish", "Gouden Guhvis"),
    "item.guhs.gouden_guhvis.lore": ("Legendary! Only the luckiest anglers ever see one", "Legendarisch! Alleen de gelukkigste vissers zien er ooit een"),
    "item.guhs.vissen.fish_info": ("%s - base %s points", "%s - basis %s punten"),
    "item.guhs.vissen.puffer_eaten": ("PUFF! You puffed up like a guhpuffer!", "PUF! Je blaast op als een guhpuffer!"),
    "gui.guhs.vissen.rarity.kaasvis": ("Common", "Gewoon"),
    "gui.guhs.vissen.rarity.vadsbaars": ("Common", "Gewoon"),
    "gui.guhs.vissen.rarity.guhpuffer": ("Uncommon", "Bijzonder"),
    "gui.guhs.vissen.rarity.njegforel": ("Rare", "Zeldzaam"),
    "gui.guhs.vissen.rarity.mika_meerval": ("Bad (njeg!)", "Fout (njeg!)"),
    "gui.guhs.vissen.rarity.gouden_guhvis": ("Legendary", "Legendarisch"),
    "item.guhs.vissershoedje": ("Chubby Bucket Hat", "Vadsig vissershoedje"),
    "item.guhs.visvest": ("Guhfish Vest", "Guhvisvest"),
    "item.guhs.vis_aan_de_haak": ("Fish on a Hook", "Vis-aan-de-haak"),
    "entity.guhs.guh_npc.visguh": ("Fish Guh", "Visguh"),
    # the Visguh talks
    "quest.guhs.vissen.hello": ("NJEG! Hello angler! Fancy a Guhfish contest? Three minutes of fishing in my pond with my own rod. Heavier fish, more points, more visbonnen. VAHOEG!",
                                "NJEG! Hoi visser! Zin in een Guhvis-wedstrijd? Drie minuten vissen in mijn vijver, met mijn eigen hengel. Hoe zwaarder de vis, hoe meer punten, hoe meer visbonnen. VAHOEG!"),
    "quest.guhs.vissen.hello_again": ("There you are again! The fish have gotten even chubbier. Another round?",
                                      "Daar ben je weer! De vissen zijn nog vadsiger geworden. Nog een rondje?"),
    "quest.guhs.vissen.running": ("Shh... there's a contest on! You can still join in.",
                                  "Sssst... er wordt gevist! Je kunt nog meedoen."),
    "quest.guhs.vissen.watch": ("The contest is almost over. Watch and cheer, the next one is yours!",
                                "De wedstrijd is bijna afgelopen. Kijk lekker mee, de volgende is van jou!"),
    "quest.guhs.vissen.playing": ("Keep fishing! Still %s to go, you have %s points.", "Vissen maar! Nog %s te gaan, je hebt %s punten."),
    "quest.guhs.vissen.start": ("Here's my Guhfish rod. Cast it into the pond when I shout FISH! And watch out for the Mika catfish... njeg!",
                                "Hier is mijn Guhvis-hengel. Gooi hem uit in de vijver zodra ik VISSEN! roep. En pas op voor de Mika-meerval... njeg!"),
    "quest.guhs.vissen.no_room": ("Your pockets are full to bursting! Make a little room for my rod first.",
                                  "Je zakken puilen uit! Maak eerst een plekje vrij voor mijn hengel."),
    "quest.guhs.vissen.full": ("Too late to join this one, sorry! Wait for the next contest.",
                               "Te laat om nog mee te doen, sorry! Wacht op de volgende wedstrijd."),
    "quest.guhs.vissen.go": ("The Guhfish contest! %s minutes. Right-click to cast, right-click again when the float dips. Only the pond counts!",
                             "De Guhvis-wedstrijd! %s minuten. Rechtsklik om uit te werpen, en nog eens als de dobber onder gaat. Alleen de vijver telt!"),
    "quest.guhs.vissen.joined": ("%s is fishing along!", "%s vist mee!"),
    "quest.guhs.vissen.walked_off": ("You walked away from the pond: your contest is over.", "Je liep weg van de vijver: jouw wedstrijd is voorbij."),
    "quest.guhs.vissen.stopped": ("The contest stopped. The rod swam back to the Visguh.", "De wedstrijd is gestopt. De hengel zwom terug naar de Visguh."),
    "quest.guhs.vissen.ranking": ("The results:", "De uitslag:"),
    "gui.guhs.vissen.points_fish": ("%s points (%s fish)", "%s punten (%s vissen)"),
    "quest.guhs.vissen.result": ("Time's up! You caught %s fish for %s points.", "Tijd! Je ving %s vissen, goed voor %s punten."),
    "quest.guhs.vissen.result_heaviest": ("Your heaviest: %s of %s.", "Je zwaarste: een %s van %s."),
    "quest.guhs.vissen.winner": ("You won the contest! +%s bonus visbonnen. VAHOEG!", "Jij hebt gewonnen! +%s extra visbonnen. VAHOEG!"),
    "quest.guhs.vissen.record": ("NEW RECORD: %s points! +%s bonus visbonnen.", "NIEUW RECORD: %s punten! +%s extra visbonnen."),
    "quest.guhs.vissen.first_record": ("Your first record: %s points!", "Je eerste record: %s punten!"),
    "quest.guhs.vissen.best_was": ("(Your record is %s points.)", "(Je record staat op %s punten.)"),
    "quest.guhs.vissen.bonnen": ("You get %s visbon(nen)!", "Je krijgt %s visbon(nen)!"),
    "quest.guhs.vissen.no_bonnen": ("No points, no visbonnen... the Mika catfish ate them. Njeg!", "Geen punten, geen visbonnen... de Mika-meerval heeft ze opgegeten. Njeg!"),
    "quest.guhs.vissen.no_bonnen_early": ("You stopped early: under %s points there are no visbonnen. Fish to the end next time!",
                                          "Je bent vroeg gestopt: onder de %s punten zijn er geen visbonnen. Vis de volgende keer tot het eind!"),
    "quest.guhs.vissen.first": ("Your very first Guhfish contest! A present from the Visguh: 4 visbonnen, fried guhfish and kaasknabbels. Vads!",
                                "Je allereerste Guhvis-wedstrijd! Een cadeautje van de Visguh: 4 visbonnen, gebakken guhvis en kaasknabbels. Vads!"),
    "quest.guhs.vissen.mika": ("NJEG! A Mika catfish... -%s points. Throw it back! (Or eat it. Bah.)", "NJEG! Een Mika-meerval... -%s punten. Gooi terug! (Of eet hem op. Bah.)"),
    "quest.guhs.vissen.golden": ("%s caught a GOLDEN GUHFISH of %s! VAHOEG!", "%s ving een GOUDEN GUHVIS van %s! VAHOEG!"),
    "quest.guhs.vissen.heaviest": ("Your heaviest fish ever: %s of %s!", "Je zwaarste vis ooit: een %s van %s!"),
    "quest.guhs.vissen.pond_record": ("WORLD RECORD! %s landed a %s of %s: the chubbiest fish ever!", "WERELDRECORD! %s haalde een %s van %s binnen: de vadsigste vis ooit!"),
    # during the contest
    "gui.guhs.vissen.ready": ("Get your rod ready...", "Maak je hengel klaar..."),
    "gui.guhs.vissen.go_title": ("FISH!", "VISSEN!"),
    "gui.guhs.vissen.go_subtitle": ("Cast into the pond!", "Werp uit in de vijver!"),
    "gui.guhs.vissen.last_seconds": ("Just %s seconds left!", "Nog maar %s seconden!"),
    "gui.guhs.vissen.bar_countdown": ("Guhfish contest - starts in %s", "Guhvis-wedstrijd - start over %s"),
    "gui.guhs.vissen.bar": ("Guhfish contest - %s - %s points (%s fish)", "Guhvis-wedstrijd - %s - %s punten (%s vissen)"),
    "gui.guhs.vissen.caught": ("%s of %s! %s points (total %s)", "%s van %s! %s punten (totaal %s)"),
    "gui.guhs.vissen.caught_bad": ("%s of %s... %s points (total %s)", "%s van %s... %s punten (totaal %s)"),
    "gui.guhs.vissen.golden_title": ("GOLDEN GUHFISH!", "GOUDEN GUHVIS!"),
    "gui.guhs.vissen.golden_subtitle": ("%s of pure gold. VAHOEG!", "%s puur goud. VAHOEG!"),
    "gui.guhs.vissen.too_early": ("Not yet! Wait for the starting signal...", "Nog niet! Wacht op het startsein..."),
    "gui.guhs.vissen.wrong_water": ("Only the contest pond counts!", "Alleen de wedstrijdvijver telt!"),
    "gui.guhs.vissen.no_drop": ("That's the Visguh's rod: you don't throw it away!", "Dat is de hengel van de Visguh: die gooi je niet weg!"),
    "gui.guhs.vissen.rod_gone": ("The Guhfish rod swam back to the Visguh.", "De Guhvis-hengel zwom terug naar de Visguh."),
    "gui.guhs.vissen.end_title": ("Time's up!", "Tijd!"),
    "gui.guhs.vissen.end_record": ("NEW RECORD!", "NIEUW RECORD!"),
    "gui.guhs.vissen.end_subtitle": ("%s points - %s visbonnen", "%s punten - %s visbonnen"),
    "gui.guhs.vissen.no_build": ("Njeg! The guhfish pond is sacred: no breaking or building here.", "Njeg! De guhvisvijver is heilig: hier mag je niks slopen of bouwen."),
    # the screen
    "gui.guhs.vissen.question": ("A Guhfish contest: %s minutes of fishing in the pond with my rod. Every fish has a weight: the heavier, the more points. Rare fish are worth a lot, the Mika catfish costs points!",
                                 "Een Guhvis-wedstrijd: %s minuten vissen in de vijver met mijn hengel. Elke vis heeft een gewicht: hoe zwaarder, hoe meer punten. Zeldzame vissen leveren veel op, de Mika-meerval kost punten!"),
    "gui.guhs.vissen.running": ("%s angler(s) are fishing, still %s to go.", "Er vissen %s visser(s), nog %s te gaan."),
    "gui.guhs.vissen.playing": ("You're fishing! Still %s to go. Stop now and you get visbonnen for your points so far.",
                                "Je bent aan het vissen! Nog %s te gaan. Stop je nu, dan krijg je visbonnen voor je punten tot nu toe."),
    "gui.guhs.vissen.start": ("Start the contest!", "Start de wedstrijd!"),
    "gui.guhs.vissen.start.tooltip": ("%s minutes, a rod on loan, all your fish are yours to keep", "%s minuten, een geleende hengel, alle vissen mag je houden"),
    "gui.guhs.vissen.join": ("Join in", "Meedoen"),
    "gui.guhs.vissen.join.tooltip": ("Fish along: everyone for their own score, the winner gets 3 extra visbonnen", "Vis mee: iedereen voor zijn eigen score, de winnaar krijgt 3 extra visbonnen"),
    "gui.guhs.vissen.join.closed": ("Less than a minute left: wait for the next contest", "Nog minder dan een minuut: wacht op de volgende wedstrijd"),
    "gui.guhs.vissen.stop": ("Stop fishing", "Stoppen met vissen"),
    "gui.guhs.vissen.stop.tooltip": ("The rod goes back, you get visbonnen for your points (the extra one only if you fish to the end)", "De hengel gaat terug, je krijgt visbonnen voor je punten (de extra alleen als je tot het eind vist)"),
    "gui.guhs.vissen.shop": ("Stall", "Kraampje"),
    "gui.guhs.vissen.shop.tooltip": ("The angler outfit for your guh (only here!), for visbonnen", "Het vispakje voor je guh (alleen hier!), voor visbonnen"),
    "gui.guhs.vissen.mine": ("Your records", "Jouw records"),
    "gui.guhs.vissen.mine.best": ("Best contest: %s points", "Beste wedstrijd: %s punten"),
    "gui.guhs.vissen.mine.heaviest": ("Heaviest fish:", "Zwaarste vis:"),
    "gui.guhs.vissen.mine.games": ("Contests: %s", "Wedstrijden: %s"),
    "gui.guhs.vissen.board": ("World top 3", "Top 3 van de wereld"),
    "gui.guhs.vissen.board.empty": ("Nobody yet... you?", "Nog niemand... jij?"),
    "gui.guhs.vissen.board.heaviest": ("Heaviest: %s", "Zwaarste: %s"),
    "gui.guhs.scorebord.vissen": ("Top 3 guhfish anglers", "Top 3 guhvissers"),
    "gui.guhs.scorebord.vissen.punten": ("Most points in a contest", "Meeste punten in een wedstrijd"),
    "gui.guhs.scorebord.vissen.zwaarste": ("Heaviest fish ever", "Vadsigste vis ooit"),
    "item.guhs.vissen.golden_caught": ("Caught by %s - %s - golden guhfish no. %s", "Gevangen door %s - %s - gouden guhvis nr. %s"),
    "item.guhs.vissen.golden_collect": ("A collector's piece: every caught one gets its angler's name and weight", "Een verzamelstuk: elke gevangen vis krijgt de naam van de visser en zijn gewicht"),
    "quest.guhs.vissen.golden_nr": ("That's golden guhfish no. %s for your collection. Keep it, frame it, eat it... VAHOEG!", "Dat is gouden guhvis nr. %s voor je verzameling. Bewaren, inlijsten of opsmullen... VAHOEG!"),
    # the super compass
    f"structure.guhs.{STRUCTURE}": ("Guhfish Pond", "Guhvisvijver"),
    f"structure.guhs.{STRUCTURE}.tooltip": ("Minigame: the Guhfish contest with the Visguh, visbonnen and the angler outfit",
                                            "Minigame: de Guhvis-wedstrijd bij de Visguh, visbonnen en het vispakje"),
}


def advancements(h):
    for name in ("vissen_eerste", "vissen_300", "vissen_goud", "vissen_alle_soorten"):
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, crit, title, desc in [
        ("find_guhvis_vijver", "enter_guhmension", "guhs:guhmensie_superkompas", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": [f"guhs:{STRUCTURE}"]}}}},
         ("Something's fishy", "Er zwemt iets vads"), ("Find the guhfish pond", "Vind de guhvisvijver")),
        ("vissen_gouden_guhvis", "find_guhvis_vijver", "guhs:gouden_guhvis", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:gouden_guhvis"}]}},
         ("Pure gold", "Puur goud"), ("Catch a Golden Guhfish in the Guhfish contest", "Vang een Gouden Guhvis in de Guhvis-wedstrijd")),
        ("vissen_outfit", "find_guhvis_vijver", "guhs:vissershoedje", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:vissershoedje"}, {"items": "guhs:visvest"}, {"items": "guhs:vis_aan_de_haak"}]}},
         ("Master angler", "Meester-visguh"), ("Buy the whole angler outfit from the Visguh", "Koop het hele vispakje bij de Visguh")),
    ]:
        h.w(f"{h.D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", *title)
        h.lang(f"advancements.guhs.guhmension.{name}.description", *desc)


# =====================================================================================================================
# the structure: the guhfish pond
# =====================================================================================================================
W, D, G, HT = 96, 96, 7, 26       # size; G = the level you walk on (the ground's top block is G-1)
POND_C, POND_R = (48, 42), (33, 27)
FACE_C, FACE_R, EARS, EAR_R = (48, 40), 9.5, ((40, 31.5), (56, 31.5)), 4.2
NPC = (52.5, 75.5)
POND_RADIUS = 68                    # (VisWedstrijd.POND_RADIUS: every bit of the pond must be this close to the Visguh)
LEAVES = {"persistent": "true", "distance": "7", "waterlogged": "false"}
NO_WATER = {"waterlogged": "false"}


def pond_d(x, z):
    """Distance from the pond's middle (1 = the shore), a little wobbly so it looks like a real pond."""
    dx, dz = (x - POND_C[0]) / POND_R[0], (z - POND_C[1]) / POND_R[1]
    a = math.atan2(dz, dx)
    return math.hypot(dx, dz) / (1 + 0.05 * math.sin(3 * a) + 0.03 * math.cos(5 * a + 1))


def in_face(x, z):
    return math.dist((x, z), FACE_C) <= FACE_R or any(math.dist((x, z), e) <= EAR_R for e in EARS)


def in_boardwalk(x, z):
    return 46 <= x <= 50 and 49 <= z <= 71


BOATHOUSE = (70, 34, 87, 48)       # x0, z0, x1, z1
CHANNEL = (70, 38, 80, 44)
STALL = (5, 37, 13, 47)
PLAZA = (33, 71, 63, 81)


def inside(box, x, z):
    return box[0] <= x <= box[2] and box[1] <= z <= box[3]


def sign_nbt(h, lines, colour="black"):
    """1.2.0: every line is a translate key sign.guhs.vissen.<slug> with the Dutch as fallback (tools/sign_text.py); the
    record board is sign.guhs.vissen.visrecords (VisWedstrijd.recordSign finds it by that key)."""
    import sign_text
    msgs = sign_text.messages("sign.guhs.vissen", lines)
    empty = h.ms.NbtList(8, ['""'] * 4)
    return {"id": "minecraft:sign", "is_waxed": h.Byte(1),
            "front_text": {"messages": msgs, "color": colour, "has_glowing_text": h.Byte(1)},
            "back_text": {"messages": empty, "color": "black", "has_glowing_text": h.Byte(0)}}


def show_item(h, s, x, y, z, facing, item):
    """An item on show in block (x, y, z) against its wall or lying on its floor (an item display: can't be taken or
    broken, and no fishing line gets stuck on it). facing: which way it looks (up = lying on the block below)."""
    yaw, pitch = {"up": (0.0, -90.0), "north": (180.0, 0.0), "south": (0.0, 0.0), "west": (90.0, 0.0), "east": (270.0, 0.0)}[facing]
    off = {"up": (0.5, 0.04, 0.5), "north": (0.5, 0.5, 0.94), "south": (0.5, 0.5, 0.06), "west": (0.94, 0.5, 0.5), "east": (0.06, 0.5, 0.5)}[facing]
    s.entity(x + off[0], y + off[1], z + off[2], {
        "id": "minecraft:item_display", "item": {"id": item, "count": 1}, "item_display": "fixed",
        "Rotation": h.floats(yaw, pitch),
        "transformation": {"left_rotation": h.floats(0, 0, 0, 1), "right_rotation": h.floats(0, 0, 0, 1),
                           "translation": h.floats(0, 0, 0), "scale": h.floats(0.6, 0.6, 0.6)}})


def lamp_post(h, s, x, z, colour="roze", height=2, y=G):
    s.fill(x, y, z, x, y + height - 1, z, h.mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    s.set(x, y + height, z, f"guhs:lampion_{colour}", {"hanging": "false", "waterlogged": "false"})


def facing_to(x, z, tx, tz):
    dx, dz = tx - x, tz - z
    if abs(dx) > abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def vijver_structure(h):
    mc, Byte, floats = h.mc, h.Byte, h.floats
    s = h.Structure((W, HT, D))
    rng = random.Random(2407)
    fp = [(x, z) for x in range(W) for z in range(D)]
    water = set()

    # --- the ground: dirt, grass on top (the path, the plaza and the buildings come later) ---
    for x, z in fp:
        s.set(x, 0, z, mc("stone"))
        for y in range(1, G - 1):
            s.set(x, y, z, mc("dirt"))
        s.set(x, G - 1, z, mc("grass_block"), {"snowy": "false"})

    # --- the pond: 1 deep at the shore, 4 in the middle; sand and clay, seagrass, sea pickles ---
    for x, z in fp:
        d = pond_d(x, z)
        if d > 1 and not inside(CHANNEL, x, z):
            continue
        depth = 1 if d > 0.85 else 2 if d > 0.6 else 3 if d > 0.35 else 4
        bottom = G - 2 - depth
        s.set(x, bottom, z, mc("clay") if rng.random() < 0.15 else mc("sand"))
        for y in range(bottom + 1, G - 1):
            s.set(x, y, z, mc("water"), {"level": "0"})
            water.add((x, y, z))
        s.set(x, G - 1, z, mc("air"))
        if depth >= 2 and rng.random() < 0.08:
            s.set(x, bottom + 1, z, mc("seagrass"))
        elif depth >= 2 and rng.random() < 0.015:
            s.set(x, bottom + 1, z, mc("sea_pickle"), {"pickles": str(rng.randint(1, 3)), "waterlogged": "true"})

    deck = set()

    def deck_at(x, z, block, props=None):
        s.set(x, G - 1, z, block, props)
        deck.add((x, z))
        if pond_d(x, z) <= 1 and x % 4 == 2 and z % 4 == 0:          # posts down to the bottom
            y = G - 2
            while s.get(x, y, z) == mc("water") or s.get(x, y, z) in (mc("seagrass"), mc("sea_pickle")):
                s.set(x, y, z, "guhs:guhbloesem_log", {"axis": "y"})
                water.discard((x, y, z))
                y -= 1

    # --- the shore path round the pond, and the grass with flowers ---
    for x, z in fp:
        d = pond_d(x, z)
        if 1 < d <= 1.14:
            s.set(x, G - 1, z, mc("pink_concrete_powder") if (x * 7 + z * 3) % 5 else mc("white_concrete_powder"))

    # --- the pier: a boardwalk out to a platform in the shape of a guh's face (seen from above) ---
    for x in range(46, 51):
        for z in range(49, 72):
            deck_at(x, z, "guhs:guhbloesem_planks")
    for x, z in fp:
        if not in_face(x, z):
            continue
        fx, fz = x - FACE_C[0], z - FACE_C[1]
        r = math.hypot(fx, fz)
        ear = next((e for e in EARS if math.dist((x, z), e) <= EAR_R), None)
        if ear and r > FACE_R - 0.5:
            block = mc("magenta_wool") if math.dist((x, z), ear) <= 2.2 else mc("pink_wool")
        elif r > FACE_R - 1:
            block = mc("pink_concrete")                                         # the outline
        elif abs(fx + 4) <= 1 and -4 <= fz <= -2 or abs(fx - 4) <= 1 and -4 <= fz <= -2:
            block = mc("white_concrete") if (fx in (-5, 3) and fz == -4) else mc("black_concrete")   # the eyes, with a shine
        elif fz == 2 and fx in (-1, 0):
            block = "guhs:guh_kristal_lamp"                                     # a glowing nose
        elif (fx, fz) in ((-2, 4), (-1, 5), (0, 5), (1, 4), (-3, 3), (2, 3)):
            block = mc("magenta_concrete")                                      # the mouth
        elif ((fx + 0.5) / 4) ** 2 + ((fz - 3.5) / 3) ** 2 <= 1:
            block = mc("white_wool")                                            # the muzzle
        elif math.dist((fx, fz), (-6.5, 2.5)) <= 1.6 or math.dist((fx, fz), (5.5, 2.5)) <= 1.6:
            block = mc("pink_concrete")                                         # blushing cheeks
        else:
            block = mc("pink_wool")
        deck_at(x, z, block)
    for (ex, ez) in EARS:                                                       # lampions on the ears and cheeks
        lamp_post(h, s, int(ex), int(ez), "roze", 2)
    lamp_post(h, s, 40, 43, "mint", 1)
    lamp_post(h, s, 56, 43, "mint", 1)
    for (bx, bz, f) in ((44, 44, "south"), (52, 44, "south")):               # two benches on the chin, looking back at the shore
        s.set(bx, G, bz, "guhs:guh_bank", {"facing": f})
    for z in range(52, 71, 6):                                                  # lamps along the boardwalk
        lamp_post(h, s, 46, z, "geel" if z % 12 else "roze", 2)
        lamp_post(h, s, 50, z + 3, "mint" if z % 12 else "geel", 2)

    # --- the boathouse (east shore): half over the water, a channel with two boats, a trophy wall ---
    x0, z0, x1, z1 = BOATHOUSE
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not inside(CHANNEL, x, z):
                deck_at(x, z, "guhs:guhbloesem_planks")
    for y in range(G, G + 5):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                door = 82 <= x <= 84 and y <= G + 2
                window = y in (G + 2, G + 3) and x in (73, 74, 77, 78) and not door
                if door:
                    continue
                s.set(x, y, z, mc("pink_stained_glass_pane") if window else "guhs:guhbloesem_planks",
                      {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"} if window else None)
        for z in range(z0, z1 + 1):
            s.set(x1, y, z, mc("stripped_cherry_log") if z in (z0, z1) else "guhs:guhbloesem_planks", {"axis": "y"} if z in (z0, z1) else None)
        for (px, pz) in ((x0, z0), (x0, z1), (80, z0), (80, z1)):
            s.set(px, y, pz, mc("stripped_cherry_log"), {"axis": "y"})
    for k, y in enumerate(range(G + 5, G + 9)):                                 # a striped roof, stepping up to a ridge
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1 + 2 * k, z1 + 2 - 2 * k):
                s.set(x, y, z, mc("pink_wool") if (x // 2) % 2 else mc("white_wool"))
    for x in range(x0, x1 + 1, 4):
        s.set(x, G + 4, 41, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    s.set(x0 - 1, G + 4, 41, "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})
    for z, fish in zip((36, 38, 40, 42, 44, 46), FISH):                         # the trophy wall
        show_item(h, s, x1 - 1, G + 2, z, "west", f"guhs:{fish}")
    for z in (37, 45):
        show_item(h, s, x1 - 1, G + 1, z, "west", "minecraft:fishing_rod")
    s.set(x1 - 1, G, 41, "guhs:guh_kast", {"facing": "west", "open": "false"})
    s.set(x1 - 1, G, 40, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(x1 - 1, G, 42, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(85, G, 36, "guhs:guh_bank", {"facing": "south"})
    s.set(85, G, 46, "guhs:guh_bank", {"facing": "north"})
    s.set(82, G, 36, "guhs:pink_zitzak", {"facing": "south"})
    for bz in (39.5, 43.0):
        s.entity(75.5, G - 1.0, bz, {"id": "minecraft:boat", "Type": "cherry", "Rotation": floats(90.0, 0.0)})

    # --- the fish stall (west shore): a counter with fish on show under a striped awning ---
    x0, z0, x1, z1 = STALL
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            deck_at(x, z, mc("stripped_cherry_wood"), {"axis": "y"})
    for z in range(z0 + 2, z1 - 1):
        s.set(x1 - 2, G, z, mc("barrel") if z % 2 else mc("smooth_quartz"), {"facing": "up", "open": "false"} if z % 2 else None)
    for z, fish in zip((40, 42, 44), ("kaasvis", "vadsbaars", "njegforel")):
        show_item(h, s, x1 - 2, G + 1, z, "up", f"guhs:{fish}")
    s.set(x1 - 2, G + 1, 41, mc("water_cauldron"), {"level": "3"})
    s.set(x1 - 2, G + 1, 43, mc("packed_ice"))
    for (px, pz) in ((x0 + 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z0 + 1), (x1 - 1, z1 - 1)):
        s.fill(px, G, pz, px, G + 3, pz, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, G + 4, z, mc("pink_wool") if (z // 2) % 2 else mc("white_wool"))
    s.set(x0 + 4, G + 3, 42, "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})
    for z in range(z0 + 2, z1 - 1):
        s.set(x0 + 1, G, z, "guhs:guh_kast" if z % 3 == 0 else mc("barrel"), {"facing": "east", "open": "false"})
    s.set(x1 - 1, G, 42, mc("cherry_wall_sign"), {"facing": "east", "waterlogged": "false"},
          sign_nbt(h, ["~ Viskraam ~", "Vers uit de", "guhvisvijver!", "Vads!"], "pink"))

    # --- the plaza at the end of the pier: the Visguh, her weighing scale and the record board ---
    x0, z0, x1, z1 = PLAZA
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                s.set(x, G - 1, z, mc("pink_concrete"))
            else:
                s.set(x, G - 1, z, mc("white_concrete") if ((x // 2) + (z // 2)) % 2 else mc("pink_terracotta"))
    s.entity(NPC[0], float(G), NPC[1], {"id": "guhs:guh_npc", "Kind": "visguh", "PersistenceRequired": Byte(1),
                                        "Rotation": floats(0.0, 0.0)})
    s.set(54, G, 77, "guhs:guh_tafel", {"facing": "north"})
    s.set(54, G + 1, 77, "guhs:guh_taart", {"bites": "0"})
    s.set(51, G, 77, "guhs:guh_stoel", {"facing": "south"})
    # the weighing scale: a golden beam on a quartz pillar with two hanging pans
    for x in range(56, 61):
        for z in range(74, 77):
            s.set(x, G - 1, z, mc("smooth_quartz"))
    s.fill(58, G, 75, 58, G + 3, 75, mc("quartz_pillar"), {"axis": "y"})
    s.fill(56, G + 4, 75, 60, G + 4, 75, mc("gold_block"))
    s.set(58, G + 5, 75, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    for px in (56, 60):
        s.set(px, G + 3, 75, mc("chain"), {"axis": "y", "waterlogged": "false"})
        s.set(px, G + 2, 75, mc("cauldron"))
    s.set(55, G, 75, mc("smooth_quartz_slab"), {"type": "bottom", "waterlogged": "false"})
    # the record board: the world's top 3 floats above it (nl.juiced.guhs.quest.Scorebord, kept up to date by the Visguh)
    for x in range(37, 42):
        for y in range(G, G + 3):
            s.set(x, y, 74, "guhs:guhbloesem_planks")
    for px in (36, 42):
        s.fill(px, G, 74, px, G + 3, 74, "guhs:guhbloesem_log", {"axis": "y"})
        s.set(px, G + 4, 74, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    for x in range(37, 42):
        s.set(x, G + 3, 74, "guhs:vlaggetjes", {"axis": "x"})
    ws = {"facing": "south", "waterlogged": "false"}
    s.set(39, G + 2, 75, mc("cherry_wall_sign"), ws, sign_nbt(h, ["~ Visrecords ~", "van de", "guhvisvijver", ""], "magenta"))
    s.set(38, G + 1, 75, mc("cherry_wall_sign"), ws, sign_nbt(h, ["~ Topvissers ~", "De top 3 van", "de wereld hangt", "hierboven!"], "blue"))
    s.set(40, G + 1, 75, mc("cherry_wall_sign"), ws, sign_nbt(h, ["~ Zwaarste vis ~", "Wie vangt de", "vadsigste?", "Njeg!"], "blue"))
    s.set(39, G + 1, 75, mc("cherry_wall_sign"), ws, sign_nbt(h, ["Vis mee!", "Praat met", "de Visguh", "VAHOEG!"], "pink"))
    for px in (33, 63):
        for pz in (71, 81):
            lamp_post(h, s, px, pz, "roze", 3)
    # bunting over the start of the pier
    for x in (44, 52):
        lamp_post(h, s, x, 71, "geel", 3)
    for x in range(45, 52):
        s.set(x, G + 3, 71, "guhs:vlaggetjes", {"axis": "x"})

    # --- the way in: a path from the gate, through a big guh face (you walk in through its mouth) ---
    for x in range(45, 51):
        for z in range(81, 95):
            s.set(x, G - 1, z, mc("pink_concrete") if x in (45, 50) else mc("white_concrete") if (z // 2) % 2 else mc("pink_terracotta"))
    for x in range(W):                                                          # a blossom hedge all round, open at the gate
        for z in range(D):
            if min(x, z, W - 1 - x, D - 1 - z) == 0 and not (z == D - 1 and 45 <= x <= 50):
                s.set(x, G, z, "guhs:guhbloesem_leaves", LEAVES)
                if (x + z) % 3:
                    s.set(x, G + 1, z, "guhs:guhbloesem_leaves", LEAVES)
    cy = G + 6
    for x in range(38, 58):
        for y in range(G, G + 17):
            dx, dy = x - 47.5, y - cy
            r = math.hypot(dx, dy)
            ear = next((e for e in ((41.5, G + 13), (53.5, G + 13)) if math.dist((x, y), e) <= 2.9), None)
            if r > 7.3 and not ear:
                continue
            if 46 <= x <= 49 and y <= G + 2 or x in (47, 48) and y == G + 3:
                continue                                                         # the mouth: the door
            if ear and r > 6.8:
                block = mc("magenta_wool") if math.dist((x, y), ear) <= 1.5 else mc("pink_wool")
            elif r > 6.4:
                block = mc("pink_concrete")
            elif x in (43, 44, 51, 52) and G + 8 <= y <= G + 10:
                block = mc("white_concrete") if (x in (43, 51) and y == G + 10) else mc("black_concrete")
            elif x in (47, 48) and y == G + 6:
                block = "guhs:guh_kristal_lamp"
            elif ((x - 47.5) / 3.6) ** 2 + ((y - (G + 4.5)) / 2.6) ** 2 <= 1:
                block = mc("white_wool")
            elif math.dist((x, y), (41.5, G + 5.5)) <= 1.2 or math.dist((x, y), (53.5, G + 5.5)) <= 1.2:
                block = mc("pink_concrete")
            else:
                block = mc("pink_wool")
            for z in (92, 93):
                s.set(x, y, z, block)
    s.set(44, G, 90, mc("cherry_sign"), {"rotation": "0", "waterlogged": "false"},
          sign_nbt(h, ["~ Guhvisvijver ~", "Guhvis-wedstrijd", "bij de Visguh!", "Vissen = VADS"], "magenta"))
    for x in (44, 51):
        lamp_post(h, s, x, 88, "roze", 2)

    # --- benches along the shore, lamps, blossom trees, flowers, lilies and fish ---
    for i in range(16):
        a = i * math.tau / 16 + 0.2
        bx = round(POND_C[0] + math.cos(a) * POND_R[0] * 1.2)
        bz = round(POND_C[1] + math.sin(a) * POND_R[1] * 1.2)
        if not (0 < bx < W - 1 and 0 < bz < D - 1) or any(inside(b, bx, bz) for b in (BOATHOUSE, STALL, PLAZA)) or 43 <= bx <= 52 and bz > 60:
            continue
        if s.get(bx, G, bz) is None and pond_d(bx, bz) > 1.14:
            if i % 2:
                lamp_post(h, s, bx, bz, ("roze", "geel", "mint")[i % 3], 2)
            else:
                s.set(bx, G, bz, "guhs:guh_bank", {"facing": facing_to(bx, bz, *POND_C)})
    trees = []
    for (tx, tz) in ((8, 8), (22, 7), (74, 8), (88, 10), (6, 26), (90, 26), (7, 60), (89, 62), (10, 86), (24, 88), (72, 88),
                     (88, 84), (30, 78), (68, 80), (14, 74), (82, 74), (36, 8), (60, 7)):
        if pond_d(tx, tz) > 1.35 and not any(inside((b[0] - 4, b[1] - 4, b[2] + 4, b[3] + 4), tx, tz) for b in (BOATHOUSE, STALL, PLAZA))\
                and not (40 <= tx <= 56 and tz >= 80):
            h.blossom_tree(s, rng, tx, G, tz)
            trees.append((tx, tz))
    for (px, pz) in ((22, 80), (74, 76), (20, 14)):                              # picnic spots: eat your catch on the lawn
        s.set(px, G, pz, "guhs:guh_tafel", {"facing": "north"})
        s.set(px, G + 1, pz, rng.choice(["guhs:guh_taart", mc("cake")]), {"bites": "0"})
        s.set(px - 1, G, pz, "guhs:guh_stoel", {"facing": "east"})
        s.set(px + 1, G, pz, "guhs:guh_stoel", {"facing": "west"})
        s.set(px, G, pz - 1, "guhs:pink_kussen", {"facing": "south"})
        lamp_post(h, s, px + 2, pz + 2, "geel", 2)
    for x, z in fp:
        if s.get(x, G - 1, z) == mc("grass_block") and s.get(x, G, z) is None and rng.random() < 0.09:
            s.set(x, G, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes", "guhs:roze_gras",
                                       "guhs:roze_gras"]))
    for (x, y, z) in list(water):
        if y == G - 2 and rng.random() < 0.02 and not any((x + dx, z + dz) in deck for dx in range(-3, 4) for dz in range(-3, 4)) \
                and pond_d(x, z) < 0.95:
            s.set(x, G - 1, z, "guhs:guh_waterlelie")
    for _ in range(7):
        while True:
            fx, fz = rng.randint(20, 76), rng.randint(18, 66)
            if (fx, G - 4, fz) in water and (fx, G - 3, fz) in water and not in_face(fx, fz):
                break
        s.entity(fx + 0.5, G - 4.0, fz + 0.5, {"id": "guhs:guh_vis", "PersistenceRequired": Byte(1)})

    s.clear_above(fp, G)
    prune_leaves(s)
    check_vijver(h, s, water, deck)
    s.save(STRUCTURE)
    preview(h, s)


def prune_leaves(s):
    """Blossom leaves that ended up on their own (a random tree edge) go: nothing may float."""
    solid = {p for p, (b, _, _) in s.blocks.items() if b != "minecraft:air"}
    reached = {p for p in solid if p[1] <= 1}
    todo = list(reached)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid and n not in reached:
                reached.add(n)
                todo.append(n)
    for p in solid - reached:
        if s.blocks[p][0] == "guhs:guhbloesem_leaves":
            s.blocks[p] = ("minecraft:air", {}, None)


# --- the geometry self-check ---------------------------------------------------------------------------------------------
PASSABLE = ("minecraft:air", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes", "guhs:roze_gras",
            "minecraft:cherry_wall_sign", "minecraft:cherry_sign", "guhs:vlaggetjes", "guhs:guh_waterlelie")
NOT_SOLID = ("minecraft:air", "minecraft:water", "minecraft:seagrass", "minecraft:sea_pickle") + PASSABLE


def check_vijver(h, s, water, deck):
    problems = []
    get = s.get

    def solid(x, y, z):
        b = get(x, y, z)
        return b is not None and b not in NOT_SOLID

    def free(x, y, z):
        b = get(x, y, z)
        return b is None or b in PASSABLE

    # 1. walk floors without holes: the plaza, the path, the pier, the boathouse and the stall
    floors = [(x, z) for x in range(W) for z in range(D) if inside(PLAZA, x, z) or (45 <= x <= 50 and 81 <= z <= 94)] + sorted(deck)
    for (x, z) in floors:
        if not solid(x, G - 1, z):
            problems.append(f"hole in the floor at {x},{z}")
    # 2. the entrance leads everywhere that matters (walking: solid floor, two free blocks above; one step up or down)
    start = (47, D - 1)
    seen, todo = {start}, [start]
    while todo:
        x, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if 0 <= n[0] < W and 0 <= n[1] < D and n not in seen and solid(n[0], G - 1, n[1]) and free(n[0], G, n[1]) and free(n[0], G + 1, n[1]):
                seen.add(n)
                todo.append(n)
    npc = (int(NPC[0]), int(NPC[1]))
    targets = {"the Visguh": [(npc[0] + dx, npc[1] + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))],
               "the face on the pier": [FACE_C], "the ears": [(40, 34), (56, 34)], "the boathouse": [(84, 41)],
               "the stall": [(STALL[2] - 1, 42)], "the record board": [(39, 76)], "the scale": [(58, 77)]}
    for what, cells in targets.items():
        if not any(c in seen for c in cells):
            problems.append(f"can't walk from the entrance to {what}")
    # 3. the Visguh stands on solid ground, with room for her
    if not solid(npc[0], G - 1, npc[1]) or not free(npc[0], G, npc[1]) or not free(npc[0], G + 1, npc[1]):
        problems.append("the Visguh doesn't stand on solid ground with room above her")
    # 4. nothing floats: every block hangs together with the ground
    blocks = {p for p, (b, _, _) in s.blocks.items() if b not in ("minecraft:air",)}
    ground = [p for p in blocks if p[1] <= 1]
    reached, todo = set(ground), list(ground)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in blocks and n not in reached:
                reached.add(n)
                todo.append(n)
    floating = sorted(blocks - reached)
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {floating[:5]}")
    # 5. the pond holds its water, and every bit of it is within the Visguh's reach
    for (x, y, z) in water:
        for d in ((1, 0, 0), (-1, 0, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if get(*n) in (None, "minecraft:air") or not s.inside(*n):
                problems.append(f"water leaks at {x},{y},{z}")
                break
        if math.dist((x, z), NPC) > POND_RADIUS - 2:
            problems.append(f"water at {x},{z} is too far from the Visguh")
    # 6. no dark corners: a lamp within 9 blocks of every floor cell
    lamps = [p for p, (b, _, _) in s.blocks.items() if "lampion" in b or b == "guhs:guh_kristal_lamp"]
    dark = [(x, z) for (x, z) in floors if not any(abs(x - lx) + abs(z - lz) <= 11 for lx, _, lz in lamps)]
    if dark:
        problems.append(f"{len(dark)} dark floor cells, e.g. {dark[:4]}")
    if problems:
        raise SystemExit("guhvis_vijver geometry check failed:\n  " + "\n  ".join(problems[:30]))
    print(f"{STRUCTURE}: geometry ok ({len(water)} water, {len(deck)} deck, {len(lamps)} lamps, {len(seen)} walkable cells)")


def preview(h, s):
    """A top-down picture of the pond (tools/features/vissen_preview.png is NOT written into the mod; scratch only)."""
    out = os.environ.get("GUHS_PREVIEW")
    if not out:
        return
    colours = {"water": (60, 120, 220), "grass_block": (90, 170, 70), "pink_wool": (238, 141, 173), "pink_concrete": (214, 101, 143),
               "white_wool": (240, 240, 240), "white_concrete": (207, 213, 214), "black_concrete": (10, 10, 15),
               "magenta_wool": (190, 69, 180), "magenta_concrete": (169, 49, 159), "guh_kristal_lamp": (255, 150, 220),
               "guhbloesem_planks": (230, 170, 190), "guhbloesem_leaves": (250, 190, 220), "pink_terracotta": (162, 78, 79),
               "pink_concrete_powder": (228, 153, 181), "white_concrete_powder": (225, 227, 227), "sand": (220, 210, 160),
               "stripped_cherry_wood": (220, 150, 150), "smooth_quartz": (235, 230, 225), "white_wool_": (255, 255, 255)}
    img = Image.new("RGB", (W, D), (0, 0, 0))
    for x in range(W):
        for z in range(D):
            for y in range(HT - 1, -1, -1):
                b = s.get(x, y, z)
                if b and b != "minecraft:air":
                    img.putpixel((x, z), colours.get(b.split(":")[1], (120, 120, 120)))
                    break
    img.resize((W * 6, D * 6), Image.NEAREST).save(out)


# =====================================================================================================================
def build(h):
    items(h)
    for key, (en, nl) in TEXTS.items():
        h.lang(key, en, nl)
    advancements(h)
    # the Visguh: a sea-green sitting guh
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.46, sat=1.05, val=1.0, only=h.pinkish), "entity", "npc_visguh.png")
    # the pond: rare, big, on flat ground; nothing spawns in it
    h.TEMPLATE_SIZES[STRUCTURE] = 96
    h.FLATNESS[STRUCTURE] = 30
    none = {"bounding_box": "full", "spawns": []}
    h.structure(STRUCTURE, h.GUHMENSION_LAND, spacing=32, separation=12, salt=20240166, start_y=-G,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    vijver_structure(h)
    self_check(h)


def self_check(h):
    """Every item of the feature has a model, a texture and a name (check_assets.py only knows the mod's own registries)."""
    missing = []
    for name in FISH + ["visbon", "guhvis_hengel"] + CLOTHES:
        if not os.path.exists(f"{h.A}/models/item/{name}.json"):
            missing.append(f"model {name}")
        if f"item.guhs.{name}" not in h.EN:
            missing.append(f"name {name}")
    for name in FISH + ["visbon", "guhvis_hengel", "guhvis_hengel_cast"]:
        if not os.path.exists(os.path.join(h.TEX, "item", f"{name}.png")):
            missing.append(f"texture {name}")
    if missing:
        raise SystemExit("vissen: missing " + ", ".join(missing))


# =====================================================================================================================
def ftb(fq):
    y = 38
    fq.q("vissen_vijver", "Er zwemt iets vads", "Zoek de &bguhvisvijver&r (superkompas: Minigames > Guhvisvijver). Een vijver met een steiger in de vorm van een guhgezicht: daar woont de &bVisguh&r.",
         "guhs:guhmensie_superkompas", [fq.structure(STRUCTURE)], x=-8, y=y)
    fq.q("vissen_eerste", "Beet!", "Doe mee aan een Guhvis-wedstrijd bij de Visguh. Je krijgt een hengel te leen: drie minuten vissen en alle vissen mag je houden!",
         "guhs:visbon", [fq.adv("vissen_eerste")], rewards=(("guhs:kaas_knabbels", 16),), x=-6, y=y, xp=100)
    fq.q("vissen_300", "Vadse vangst", "Haal 300 punten in een wedstrijd. Tip: zware vissen en zeldzame njegforellen leveren het meest op, de Mika-meerval kost punten!",
         "guhs:vadsbaars", [fq.adv("vissen_300")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-4, y=y, xp=200)
    fq.q("vissen_soorten", "Het visboek", "Vang elke soort uit de guhvisvijver een keer: kaasvis, vadsbaars, guhpuffer, roze njegforel, Mika-meerval en de Gouden Guhvis.",
         "guhs:njegforel", [fq.adv("vissen_alle_soorten")], rewards=(("guhs:visbon", 5),), x=-2, y=y, xp=300)
    fq.q("vissen_goud", "Puur goud", "Vang een &6Gouden Guhvis&r: legendarisch zeldzaam. Hij is van jou: elke gouden vis krijgt jouw naam, zijn gewicht en een nummer. Een echt verzamelstuk!",
         "guhs:gouden_guhvis", [fq.item("guhs:gouden_guhvis")], rewards=(("guhs:guh_kristal", 8),), x=0, y=y, shape="gear", xp=300)
    fq.q("vissen_hoedje", "Een vadsig hoedje", "Koop het vadsige vissershoedje bij het kraampje van de Visguh, voor visbonnen.",
         "guhs:vissershoedje", [fq.item("guhs:vissershoedje")], rewards=(("guhs:gebakken_guh_vis", 4),), x=2, y=y)
    fq.q("vissen_pakje", "Meester-visguh", "Koop het hele vispakje voor je guh: het vissershoedje, het guhvisvest en de vis-aan-de-haak.",
         "guhs:vis_aan_de_haak", [fq.adv("guhs:guhmension/vissen_outfit")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=4, y=y,
         shape="gear", xp=300)
