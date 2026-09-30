"""
Guhrace (2.4): the guh racebaan in the Guhmension, the Raceguh, the rental race guh, checkpoint rings, VAHOEG launch pads,
the ghost of your best race, raceprijsjes and the jockey outfit (cap, striped silk jacket, racing goggles).

build(h)  textures, models, blockstates, lang, advancements and the racebaan structure (with a geometry self-check)
ftb(fq)   the FTB quests (row y=28)
The Java side lives in nl.juiced.guhs.feature.race.
"""
import math
import random

import numpy as np
from PIL import Image

NAME = "guh_racebaan"
W, H, D = 100, 34, 100
G = 4                       # ground: grass at y=3, you walk at y=4 (the template is sunk 3 into the terrain)
START = (43, G, 80)         # the start marker, facing east (keep in sync with RaceTrack.TEMPLATE_START)
NPC = (28.5, G, 89.5)       # the Raceguh in the pit stop, looking north at the track
HALF, KERB, WALL = 3.4, 4.4, 5.4   # |offset| from the middle of the track: lanes, kerbs, barrier

CLOTHES = ["jockey_pet", "jockey_jasje", "racebril"]

# --- guh model bones for the jockey outfit (make_guh_variants format: parent, pivot, swatch, [(origin, size, inflate)]) ---
_H = [0, 6, -2]
BONES = {
    # the pom-pom on top of the jockey cap (the cap itself is the normal cap bone)
    "outfit_jockey_pom": ("head", _H, "jockey_pom", [([-1, 17, -7.5], [2, 1.3, 2], 0)]),
    # racing goggles: two big lenses in a rim, a bridge, and a strap all around the head
    "outfit_racebril": ("head", _H, "racebril", [([0.8, 7.2, -12.9], [6.4, 5.4, 0.9], 0), ([-7.2, 7.2, -12.9], [6.4, 5.4, 0.9], 0),
                                                ([-1.2, 9.4, -12.8], [2.4, 1, 0.7], 0)]),
    "outfit_racebril_band": ("head", _H, "racebril_band", [([7.25, 9.1, -12.5], [0.6, 1.3, 12.6], 0), ([-7.85, 9.1, -12.5], [0.6, 1.3, 12.6], 0),
                                                          ([-7.85, 9.1, 0.1], [15.7, 1.3, 0.6], 0)]),
}


# ======================================================================================================================
# guh clothes: textures, icons
# ======================================================================================================================
def clothes(rng, v):
    px = v.SWATCH * 4

    def silk_stripes():
        a = np.zeros((px, px, 3), np.float32)
        for x in range(px):
            a[:, x] = (255, 105, 180) if (x // 4) % 2 == 0 else (252, 248, 250)
        sheen = 1 + 0.12 * np.sin(np.linspace(0, math.pi * 3, px))[:, None, None]   # silky shine running down
        a = a * sheen
        a[:, 15:17] = (250, 200, 60)                                                # golden seam down the middle
        return np.clip(a + rng.normal(0, 3, (px, px, 1)), 0, 255)

    def quartered():
        a = np.zeros((px, px, 3), np.float32)
        for y in range(px):
            for x in range(px):
                a[y, x] = (255, 105, 180) if (x < px // 2) == (y < px // 2) else (252, 248, 250)
        a[px // 2 - 1:px // 2 + 1, :] = (250, 200, 60)
        a[:, px // 2 - 1:px // 2 + 1] = (250, 200, 60)
        return np.clip(a + rng.normal(0, 4, (px, px, 1)), 0, 255)

    def lens():
        a = np.zeros((px, px, 3), np.float32)
        for y in range(px):
            for x in range(px):
                edge = min(x, y, px - 1 - x, px - 1 - y)
                if edge < 4:
                    a[y, x] = (250, 200, 60) if edge >= 1 else (170, 120, 30)       # golden rim
                else:
                    t = y / px
                    a[y, x] = (60 + 90 * t, 30 + 40 * t, 90 + 60 * t)            # tinted purple-pink glass
                    if abs((x - y) - 4) < 2 and 6 < x < 20:
                        a[y, x] = (240, 230, 255)                                    # a glint
        return a

    return {
        "jockey_pet": {"cap": quartered, "jockey_pom": lambda: v.fabric((250, 200, 60), rng, 8)},
        "jockey_jasje": {"suit": silk_stripes},
        "racebril": {"racebril": lens, "racebril_band": lambda: v.stripes((225, 60, 150), (250, 250, 250), rng, 3, diagonal=True)},
    }


def icons(ic):
    pink, dark, white, gold = (255, 105, 180), (150, 40, 100), (252, 248, 250), (250, 200, 60)
    cap = ic.pad(["......gg........", ".....gggg.......", "....aaaaaaa.....", "...abbbgwwwa....", "..abbbbgwwwwa...",
                  "..abbbbgwwwwa...", "..aggggggggga...", "..awwwwgbbbba...", "..awwwwgbbbbaaa.", "..aaaaaaaaaaaaaa"])
    jacket = [r[:4] + "".join(("c" if (i % 3 == 0 and ch == "b") else ch) for i, ch in enumerate(r[4:12], 4)) + r[12:] for r in ic.SHIRT]
    goggles = ic.pad(["gggggg....gggggg", "gkvvvg....gvvvkg", "gvkvvgbbbbgvvkvg", "gvvvvg....gvvvvg", "gggggg....gggggg",
                      "bb............bb"])
    return {
        "jockey_pet": ic.icon(cap, {"a": dark, "b": pink, "w": white, "g": gold}),
        "jockey_jasje": ic.icon(ic.pad(jacket), {"a": dark, "b": pink, "c": white}),
        "racebril": ic.icon(goggles, {"g": gold, "v": (120, 70, 160), "k": (240, 230, 255), "b": (225, 60, 150)}),
    }


# ======================================================================================================================
# resources
# ======================================================================================================================
PRIJSJE = [
    "....rr....rr....", "...rPPr..rPPr...", "...rPPPrrPPPr...", "....rPPggPPr....", ".....gggggg.....", "....gGyyyyGg....",
    "...gGyggggyGg...", "...gyggwwggyg...", "...gyggwwggyg...", "...gGyggggyGg...", "....gGyyyyGg....", ".....gggggg.....",
    "......m..m......", ".....m....m.....", "....m......m....", "................"]


def textures(h):
    # the checkpoint block: pink and white chequers in a golden frame (it glows)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (250, 205, 70, 255) if edge else (255, 250, 252, 255) if ((x - 1) // 2 + (y - 1) // 2) % 2 else (255, 120, 190, 255))
    h.save(img, "block", "race_checkpoint.png")
    # the VAHOEG pad: magenta with golden chevrons pointing north (the model turns them the way the pad faces)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = (200, 50, 140)
            for off in (1, 8):
                yy = y - off
                if 0 <= yy <= 6 and abs(x - 7.5) <= 7 and abs((abs(x - 7.5) * 0.8) - yy) < 1.3:
                    c = (255, 215, 70)
            if x in (0, 15):
                c = (250, 246, 240)
            img.putpixel((x, y), c + (255,))
    h.save(img, "block", "race_vahoegpad.png")
    h.save(h.noise_tex((200, 50, 140), 8, 811), "block", "race_vahoegpad_side.png")
    # 2.9: the silver pad (it doesn't work on lastig): the same, with silver chevrons on lilac
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = (150, 110, 190)
            for off in (1, 8):
                yy = y - off
                if 0 <= yy <= 6 and abs(x - 7.5) <= 7 and abs((abs(x - 7.5) * 0.8) - yy) < 1.3:
                    c = (225, 228, 238)
            if x in (0, 15):
                c = (250, 246, 240)
            img.putpixel((x, y), c + (255,))
    h.save(img, "block", "race_vahoegpad_zilver.png")
    h.save(h.grid(PRIJSJE, {**h.ITEM_PAL, "r": (225, 60, 150, 255), "m": (200, 70, 130, 255)}), "item", "raceprijsje.png")
    h.item_model("raceprijsje")
    # the Raceguh: an orange-gold racing guh
    src = Image.open(f"{h.TEX}/entity/guh_sitting.png")
    h.save(h.recolour(src, hue=0.075, sat=1.3, val=1.05, only=h.pinkish), "entity", "npc_raceguh.png")


def models(h):
    A = h.A
    h.w(f"{A}/models/block/race_checkpoint.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/race_checkpoint"}})
    h.w(f"{A}/blockstates/race_checkpoint.json", {"variants": {f"baan={b},nummer={n}": {"model": "guhs:block/race_checkpoint"}
                                                               for n in range(16) for b in range(4)}})
    h.w(f"{A}/models/item/race_checkpoint.json", {"parent": "guhs:block/race_checkpoint"})
    h.w(f"{A}/models/block/race_vahoegpad.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/race_vahoegpad", "top": "guhs:block/race_vahoegpad", "side": "guhs:block/race_vahoegpad_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 1, 16], "faces": {
            "up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#side", "cullface": "down"},
            **{f: {"texture": "#side", "uv": [0, 15, 16, 16]} for f in ("north", "south", "east", "west")}}}]})
    h.w(f"{A}/models/block/race_vahoegpad_zilver.json", {"parent": "guhs:block/race_vahoegpad", "textures": {
        "particle": "guhs:block/race_vahoegpad_zilver", "top": "guhs:block/race_vahoegpad_zilver", "side": "guhs:block/race_vahoegpad_side"}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{A}/blockstates/race_vahoegpad.json", {"variants": {
        f"facing={f},lastig={l}": {"model": "guhs:block/race_vahoegpad" + ("" if l == "true" else "_zilver"), **({"y": y} if y else {})}
        for f, y in rot.items() for l in ("true", "false")}})
    h.item_model("race_vahoegpad", "guhs:block/race_vahoegpad")
    h.w(f"{A}/models/block/race_start.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
    h.w(f"{A}/blockstates/race_start.json", {"variants": {f"baan={b},facing={f}": {"model": "guhs:block/race_start"} for f in rot for b in range(4)}})


TRACK_BLOCKS = ["minecraft:cherry_planks", "minecraft:cherry_slab", "minecraft:smooth_quartz", "minecraft:smooth_quartz_slab",
                "minecraft:purpur_block", "minecraft:purpur_slab", "minecraft:black_concrete", "minecraft:white_concrete", "guhs:tong",
                "guhs:race_vahoegpad", "guhs:race_checkpoint"]


def tags(h):
    """What a race guh may run on (RaceGame.TRACK_BLOCKS): off it for more than a second = back to the last ring."""
    h.w(f"{h.D}/tags/block/race_track.json", {"replace": False, "values": TRACK_BLOCKS})


def advancements(h):
    # 2.9: the old racebaan on lastig (tab De Grote Guhspelen; granted by RaceGame)
    h.w(f"{h.D}/advancement/grote_guhspelen/race_lastig.json", {
        "parent": "guhs:grote_guhspelen/root",
        "display": {"icon": {"id": "guhs:raceprijsje"}, "title": {"translate": "advancements.guhs.grote_guhspelen.race_lastig.title"},
                    "description": {"translate": "advancements.guhs.grote_guhspelen.race_lastig.description"},
                    "frame": "goal", "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
    h.lang("advancements.guhs.grote_guhspelen.race_lastig.title", "Racen op lastig", "Racen op lastig")
    h.lang("advancements.guhs.grote_guhspelen.race_lastig.description", "Rij de oude guhracebaan uit op lastig", "Rij de oude guhracebaan uit op lastig")
    for name in ("race_eerste", "race_brons", "race_zilver", "race_goud", "race_geest"):
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, crit, en, nl in [
        ("find_guh_racebaan", "enter_guhmension", "guhs:raceprijsje", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": [f"guhs:{NAME}"]}}}},
         ("Start your engines!", "Find the guh racebaan"), ("Op je plaatsen, vads!", "Vind de guhracebaan")),
        ("race_jockey", "find_guh_racebaan", "guhs:jockey_pet", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:jockey_pet"}, {"items": "guhs:jockey_jasje"}, {"items": "guhs:racebril"}]}},
         ("Vroom vroom, guh!", "Buy the whole jockey outfit from the Raceguh"), ("Vroem vroem, guh!", "Koop het hele jockeypakje bij de Raceguh")),
    ]:
        h.w(f"{h.D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", nl[0], nl[0])
        h.lang(f"advancements.guhs.guhmension.{name}.description", nl[1], nl[1])


TEXTS = {  # key: (English, Dutch)
    "block.guhs.race_checkpoint": ("Race Checkpoint", "Racecheckpoint"),
    "block.guhs.race_vahoegpad": ("VAHOEG Launch Pad", "VAHOEG-lanceerpad"),
    "block.guhs.race_start": ("Race Start", "Racestart"),
    "item.guhs.raceprijsje": ("Race Prize", "Raceprijsje"),
    "item.guhs.jockey_pet": ("Jockey Cap", "Jockeypetje"),
    "item.guhs.jockey_jasje": ("Striped Silk Jockey Jacket", "Gestreept zijden jockeyjasje"),
    "item.guhs.racebril": ("Racing Goggles", "Vahoege racebril"),
    "entity.guhs.race_guh": ("Rental Race Guh", "Huur-renguh"),
    "entity.guhs.race_ghost": ("Record Ghost", "Recordgeest"),
    "entity.guhs.race_ghost.name": ("Your record ghost (%s)", "Jouw recordgeest (%s)"),
    "entity.guhs.guh_npc.raceguh": ("Race Guh", "Raceguh"),
    f"structure.guhs.{NAME}": ("Guh Racebaan", "Guhracebaan"),
    f"structure.guhs.{NAME}.tooltip": ("Minigame: race a rental guh, beat your own ghost and win the jockey outfit",
                                        "Minigame: race op een huur-renguh, versla je eigen geest en win het jockeypakje"),
    # the Raceguh talking
    "quest.guhs.race.hello_new": ("VROEM VROEM, NJEG! Welcome to the guh racebaan! I lend you a race guh: 3 laps through all the glowing rings, over the bridge and right through the big guh head. The VAHOEG pads give you a boost! No race guh of your own needed, vads.",
                                  "VROEM VROEM, NJEG! Welkom op de guhracebaan! Ik leen je een renguh: 3 rondjes door alle lichtgevende ringen, over de brug en dwars door het grote guhhoofd. De VAHOEG-pads geven je een zet! Je hoeft zelf niks mee te nemen, vads."),
    "quest.guhs.race.hello": ("Back for more? My race guhs are warmed up and fed. Beat your ghost! VAHOEG!",
                              "Ben je er weer? Mijn renguhs zijn warmgelopen en hebben hun kaasknabbels op. Versla je geest! VAHOEG!"),
    "quest.guhs.race.hello_busy": ("Shh, someone is racing right now! Watch from the grandstand and cheer: VAHOEG!",
                                   "Sssst, er wordt nu geracet! Kijk mee vanaf de tribune en roep maar hard: VAHOEG!"),
    "quest.guhs.race.busy": ("%s is racing right now (lap %s of %s). Wait a moment, or cheer from the grandstand!",
                             "%s racet nu (ronde %s van %s). Even wachten, of juich mee vanaf de tribune!"),
    "quest.guhs.race.broken": ("Njeg... my racebaan is broken, I can't find the rings any more.",
                               "Njeg... mijn racebaan is kapot, ik kan de ringen niet meer vinden."),
    "quest.guhs.race.go": ("Here's your race guh! Hold on tight... on your marks...", "Hier is je renguh! Hou je goed vast... op je plaatsen..."),
    "quest.guhs.race.go_go": ("Why are you talking to me? RACE! VAHOEG!", "Waarom sta je met mij te kletsen? RACEN! VAHOEG!"),
    "quest.guhs.race.controls": ("W = run (your guh picks up speed), mouse = steer, S = brake, shift = get off (you have 5 seconds to hop back on). Go through every ring in order and stay on the road!",
                                 "W = rennen (je guh komt steeds beter op gang), muis = sturen, S = remmen, shift = afstappen (dan heb je 5 seconden om weer op te stappen). Door alle ringen, op volgorde, en blijf op de baan!"),
    "quest.guhs.race.ghost_on": ("Your record ghost races along again. Wooo...", "Je recordgeest racet weer mee. Oehoeoeoe..."),
    "quest.guhs.race.ghost_off": ("No ghost this time: just you and the racebaan.", "Geen geest deze keer: alleen jij en de racebaan."),
    "quest.guhs.race.shop": ("The jockey outfit! Only here, only for raceprijsjes. Your guh will be the fastest-looking guh in the Guhmension.",
                             "Het jockeypakje! Alleen hier, alleen voor raceprijsjes. Dan is jouw guh de snelst uitziende guh van de Guhmensie."),
    # during the race
    "quest.guhs.race.ready": ("Get ready...", "Klaar voor de start..."),
    "quest.guhs.race.vahoeg": ("VAHOEG!", "VAHOEG!"),
    "quest.guhs.race.off": ("You got off your race guh! Hop back on within %s seconds (right-click it), or the race is over.",
                            "Je bent van je renguh afgestapt! Stap binnen %s seconden weer op (rechtsklik op je renguh), anders is de race voorbij."),
    "quest.guhs.race.missed": ("Njeg! You skipped a ring. Go back through %s first!", "Njeg! Je hebt een ring overgeslagen. Ga eerst terug door %s!"),
    "quest.guhs.race.missed.short": ("Missed %s!", "%s gemist!"),
    "quest.guhs.race.finish_line": ("the finish", "de finish"),
    "quest.guhs.race.gate": ("ring %s", "ring %s"),
    "quest.guhs.race.checkpoint": ("Ring %s of %s!", "Ring %s van %s!"),
    "quest.guhs.race.lap": ("Lap %s of %s", "Ronde %s van %s"),
    "quest.guhs.race.last_lap": ("LAST LAP! (%s of %s)", "LAATSTE RONDE! (%s van %s)"),
    "quest.guhs.race.lap_time": ("Lap time %s", "Rondetijd %s"),
    "quest.guhs.race.pad": ("VAHOEG!", "VAHOEG!"),
    "quest.guhs.race.reset": ("Oops! Back to the last ring...", "Oepsie! Terug naar de laatste ring..."),
    "quest.guhs.race.off_track": ("Njeg! No shortcuts over the grass, vads!", "Njeg! Niet afsnijden over het gras, vads!"),
    "quest.guhs.race.finish": ("FINISH!", "FINISH!"),
    "quest.guhs.race.new_record": (" - NEW RECORD!", " - NIEUW RECORD!"),
    "quest.guhs.race.result": ("Finished in %s (laps: %s). %s! You get %s raceprijsje(s).",
                               "Over de finish in %s (rondes: %s). %s! Je krijgt %s raceprijsje(s)."),
    "quest.guhs.race.medal.goud": ("GOLD", "GOUD"),
    "quest.guhs.race.medal.zilver": ("Silver", "Zilver"),
    "quest.guhs.race.medal.brons": ("Bronze", "Brons"),
    "quest.guhs.race.medal.finish": ("Finished", "Uitgereden"),
    "quest.guhs.race.first_record": ("Your first race time: %s. From now on your ghost races along!", "Je eerste racetijd: %s. Vanaf nu racet je geest mee!"),
    "quest.guhs.race.record": ("NEW PERSONAL RECORD: %s (was %s)! Your ghost gets the new one. +2 raceprijsjes", "NIEUW PERSOONLIJK RECORD: %s (was %s)! Je geest rijdt voortaan deze race. +2 raceprijsjes"),
    "quest.guhs.race.no_record": ("Your record is still %s (%s).", "Je record blijft %s (%s)."),
    "quest.guhs.race.lap_record": ("Fastest lap ever: %s!", "Snelste ronde ooit: %s!"),
    "quest.guhs.race.first": ("Your very first guhrace! A welcome bag: 4 extra raceprijsjes and a handful of kaasknabbels. VAHOEG!",
                              "Je allereerste guhrace! Een welkomstzakje: 4 extra raceprijsjes en een handvol kaasknabbels. VAHOEG!"),
    "quest.guhs.race.track_record": ("TRACK RECORD! %s raced the guh racebaan in %s: first place on the board! VAHOEG!", "BAANRECORD! %s racete de guhracebaan in %s: plek 1 op het scorebord! VAHOEG!"),
    "quest.guhs.race.ended.gave_up": ("You got off: race over. The race guh trots back to the pit stop.", "Je bent afgestapt: race voorbij. De renguh draaft terug naar de pitstop."),
    "quest.guhs.race.ended.gone": ("The race is over.", "De race is voorbij."),
    "quest.guhs.race.ended.too_late": ("Six minutes and still no finish? The race guh is tired, njeg. Race over!", "Zes minuten en nog steeds niet binnen? De renguh is moe, njeg. Race voorbij!"),
    "quest.guhs.race.ended.stopped": ("The race was stopped.", "De race is gestopt."),
    # the screen
    "gui.guhs.race.question": ("3 rondes door alle ringen: hoe sneller, hoe meer raceprijsjes (goud, zilver, brons). Makkelijk, medium of lastig? Je beste race rijdt mee als geest!",
                               "3 rondes door alle ringen: hoe sneller, hoe meer raceprijsjes (goud, zilver, brons). Makkelijk, medium of lastig? Je beste race rijdt mee als geest!"),
    "gui.guhs.race.busy": ("%s is racing (lap %s). Wait a moment, or watch from the grandstand!", "%s is aan het racen (ronde %s). Even wachten, of kijk mee vanaf de tribune!"),
    "gui.guhs.race.start": ("Race! (3 laps)", "Racen! (3 rondes)"),
    "gui.guhs.race.start.tooltip": ("You get a rental race guh for the race; afterwards it goes back to the pit stop", "Je krijgt een huur-renguh voor de race; daarna gaat hij terug naar de pitstop"),
    "gui.guhs.race.ghost.on": ("Ghost: on", "Geest: aan"),
    "gui.guhs.race.ghost.off": ("Ghost: off", "Geest: uit"),
    "gui.guhs.race.ghost.tooltip": ("Your best race drives along, see-through (you need a record first)", "Je beste race rijdt doorzichtig met je mee (daar heb je eerst een record voor nodig)"),
    "gui.guhs.race.shop": ("Shop", "Winkeltje"),
    "gui.guhs.race.shop.tooltip": ("The jockey outfit for your guh, for raceprijsjes", "Het jockeypakje voor je guh, voor raceprijsjes"),
    "gui.guhs.race.records": ("Your records:", "Jouw records:"),
    "gui.guhs.race.best": ("Race %s   Lap %s   (%s races)", "Race %s   Ronde %s   (%s races)"),
    "gui.guhs.race.track_record": ("Track record: %s in %s", "Baanrecord: %s in %s"),
    "gui.guhs.race.track_record.none": ("No track record yet: go for it!", "Nog geen baanrecord: pak hem!"),
    "gui.guhs.race.prizes": ("Goud 6, zilver 4, brons 3, uitgereden 2 raceprijsjes (+2 bij een record; lastig +50 %)",
                             "Goud 6, zilver 4, brons 3, uitgereden 2 raceprijsjes (+2 bij een record; lastig +50 %)"),
    # 2.9: the levels, the golden ghost
    "gui.guhs.race.start.niveau": ("Racen: %s", "Racen: %s"),
    "gui.guhs.race.start.tooltip.makkelijk": ("Een rustige renguh, en val je van de baan dan gaat hij maar een klein stukje terug", "Een rustige renguh, en val je van de baan dan gaat hij maar een klein stukje terug"),
    "gui.guhs.race.start.tooltip.medium": ("De gewone race, zoals altijd", "De gewone race, zoals altijd"),
    "gui.guhs.race.start.tooltip.lastig": ("Een supersnelle maar eigenwijze renguh, Mika-pikkers langs de baan en de zilveren pads doen het niet. +50 % raceprijsjes!", "Een supersnelle maar eigenwijze renguh, Mika-pikkers langs de baan en de zilveren pads doen het niet. +50 % raceprijsjes!"),
    "gui.guhs.race.goud.on": ("Goud: aan", "Goud: aan"),
    "gui.guhs.race.goud.off": ("Goud: uit", "Goud: uit"),
    "gui.guhs.race.goud.tooltip": ("Het baanrecord van de wereld rijdt mee als gouden geest (als iemand anders het heeft)", "Het baanrecord van de wereld rijdt mee als gouden geest (als iemand anders het heeft)"),
    "gui.guhs.scorebord.race.niveau": ("Snelste vadsen: %s", "Snelste vadsen: %s"),
    "quest.guhs.race.niveau.makkelijk": ("Makkelijk: een rustige renguh, en val je van de baan dan zet ik je een klein stukje terug.", "Makkelijk: een rustige renguh, en val je van de baan dan zet ik je een klein stukje terug."),
    "quest.guhs.race.niveau.lastig": ("Lastig! Een supersnelle maar eigenwijze renguh, Mika-pikkers langs de baan, en de ZILVEREN VAHOEG-pads doen het niet. Succes, vads!", "Lastig! Een supersnelle maar eigenwijze renguh, Mika-pikkers langs de baan, en de ZILVEREN VAHOEG-pads doen het niet. Succes, vads!"),
    "quest.guhs.race.reset_makkelijk": ("Oepsie! Een klein stukje terug...", "Oepsie! Een klein stukje terug..."),
    "quest.guhs.race.goud_on": ("De gouden geest racet weer mee. Kun jij het baanrecord verslaan?", "De gouden geest racet weer mee. Kun jij het baanrecord verslaan?"),
    "quest.guhs.race.goud_off": ("Geen gouden geest deze keer.", "Geen gouden geest deze keer."),
    "entity.guhs.race_ghost.goud": ("Baanrecord van %s (%s)", "Baanrecord van %s (%s)"),
    "gui.guhs.race.hud.lap.baan": ("%s (%s) - ronde %s/%s", "%s (%s) - ronde %s/%s"),
    "gui.guhs.race.hud.goud": ("Gouden geest: %s", "Gouden geest: %s"),
    "gui.guhs.race.baan.racebaan": ("Guhracebaan", "Guhracebaan"),

    "gui.guhs.race.hud.lap": ("GUHRACE - lap %s/%s", "GUHRACE - ronde %s/%s"),
    "gui.guhs.race.hud.this_lap": ("Lap %s", "Ronde %s"),
    "gui.guhs.race.hud.best": ("Record %s", "Record %s"),
    "gui.guhs.race.hud.best_lap": ("Best lap %s", "Beste ronde %s"),
    "gui.guhs.race.hud.next": ("Next: ring %s/%s", "Volgende: ring %s/%s"),
    "gui.guhs.race.hud.next_finish": ("Next: the FINISH", "Volgende: de FINISH"),
    # the top-3 board above the Raceguh (Scorebord)
    "gui.guhs.scorebord.race": ("Fastest racers of the world", "Snelste vadsen van de wereld"),
    "gui.guhs.race.board.total": ("Whole race (3 laps)", "Hele race (3 rondes)"),
    "gui.guhs.race.board.lap": ("Fastest lap", "Snelste ronde"),
    "gui.guhs.race.no_build": ("Njeg! No digging holes in the racebaan: the race guhs would trip.", "Njeg! Geen gaten graven in de racebaan: dan struikelen de renguhs."),
}


def texts(h):
    for key, (en, nl) in TEXTS.items():
        h.lang(key, nl, nl)          # (2.9: Dutch in every language, like all guh texts)


# ======================================================================================================================
# the racebaan
# ======================================================================================================================
SEGMENTS = [  # the middle of the track: straights and quarter circles (centre, radius, from/to angle; x = cx + r sin a, z = cz + r cos a)
    ("line", (30.5, 80.5), (76.5, 80.5)),     # 0: start/finish straight along the grandstand (east)
    ("arc", (76.5, 64.5), 16, 0, 90),         # 1: banked curve
    ("line", (92.5, 64.5), (92.5, 36.5)),     # 2: up the bridge over the kaas saus river (north)
    ("arc", (76.5, 36.5), 16, 90, 180),       # 3: banked curve
    ("line", (76.5, 20.5), (30.5, 20.5)),     # 4: into the big guh head (west)
    ("arc", (30.5, 36.5), 16, 180, 270),      # 5: banked curve
    ("line", (14.5, 36.5), (14.5, 64.5)),     # 6: VAHOEG pads and the vadsbult (south)
    ("arc", (30.5, 64.5), 16, 270, 360),      # 7: banked curve back to the start
]
CENTROID = (53, 50)
STEP = 0.2


def samples():
    """Points along the middle of the track: (x, z, tangent, outward normal, segment, distance along the segment, segment length)."""
    out = []
    for i, seg in enumerate(SEGMENTS):
        if seg[0] == "line":
            (x0, z0), (x1, z1) = seg[1], seg[2]
            length = math.dist((x0, z0), (x1, z1))
            tx, tz = (x1 - x0) / length, (z1 - z0) / length
            nx, nz = -tz, tx
            if (x0 - CENTROID[0]) * nx + (z0 - CENTROID[1]) * nz < 0:
                nx, nz = -nx, -nz
            n = int(length / STEP)
            for k in range(n):
                s = k * STEP
                out.append((x0 + tx * s, z0 + tz * s, (tx, tz), (nx, nz), i, s, length))
        else:
            (cx, cz), r, a0, a1 = seg[1], seg[2], seg[3], seg[4]
            length = r * math.radians(a1 - a0)
            n = int(length / STEP)
            for k in range(n):
                s = k * STEP
                a = math.radians(a0) + s / r
                out.append((cx + r * math.sin(a), cz + r * math.cos(a), (math.cos(a), -math.sin(a)), (math.sin(a), math.cos(a)), i, s, length))
    return out


def base_h2(seg, z):
    """Height of the track in half blocks above the ground (column z)."""
    zc = z + 0.5
    if seg == 2:                      # the bridge: up over 8 blocks from z 64, a deck from z 56 to 44, down to z 36
        return int(max(0, min(8, 64.5 - zc, zc - 36.5)))
    if seg == 6:                      # the vadsbult: a 2 block hump from z 45 to 56
        return int(max(0, min(4, zc - 44.5, 56.5 - zc)))
    return 0


def bank(seg, s, length, off):
    """Banked curves: the outside of a curve goes up (half a block per lane), easing in and out over 6 blocks."""
    if seg % 2 == 0 or off <= 0:
        return 0
    f = max(0.0, min(1.0, s / 6, (length - s) / 6))
    return int(round(min(off, KERB) * f))


def classify():
    """For every column: nearest point of the middle of the track -> offset, height (half blocks), segment, s."""
    pts = samples()
    P = np.array([(p[0], p[1]) for p in pts])
    N = np.array([p[3] for p in pts])
    cols = {}
    for x in range(W):
        cx = np.full(D, x + 0.5)
        cz = np.arange(D) + 0.5
        dx = cx[:, None] - P[None, :, 0]
        dz = cz[:, None] - P[None, :, 1]
        d2 = dx * dx + dz * dz
        idx = d2.argmin(1)
        for z in range(D):
            i = idx[z]
            off = dx[z, i] * N[i, 0] + dz[z, i] * N[i, 1]
            if abs(off) > WALL + 0.8:
                continue
            _, _, _, _, seg, s, length = pts[i]
            h2 = base_h2(seg, z) + bank(seg, s, length, off)
            cols[(x, z)] = (off, h2, seg, s, length)
    return cols, pts


RIVER_Z = range(47, 54)
POND = (58, 50, 7.5)
HEAD = (58, G + 7, 20, 12)         # the big guh head: centre x, y, z, radius (the tunnel runs through it along x)
GATE0_X = 40                       # the start/finish gate: a guh face over the track
PADS = [((56, 57), "x", 80, "east", True), ((73, 74), "x", 20, "west", False), ((41, 42), "z", 14, "south", True)]
# 2.9: Mika-pikker spots for lastig (circuit_mikaplek, vanaf 2): beside the start straight, the head straight, the west straight
MIKAS = [((66, G, 73), "south"), ((40, G, 27), "north"), ((21, G, 62), "west")]
# 2.9: the floating scoreboards of makkelijk and lastig on the pit stop's roof (RaceRole.BOARD_MAKKELIJK / BOARD_LASTIG)
BOARDS = [(23, 11, 86), (33, 11, 86)]


def in_river(x, z):
    return (z in RIVER_Z and 58 <= x <= 98) or (x - POND[0]) ** 2 + (z - POND[1]) ** 2 <= POND[2] ** 2


def in_head(x, y, z):
    cx, cy, cz, r = HEAD
    return (x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2 <= r * r


def level_blocks(h2):
    """(top full block y, slab y or None) for a surface h2 half blocks above the ground; you walk at G + h2 / 2."""
    if h2 % 2 == 0:
        return G + h2 // 2 - 1, None
    return G + h2 // 2 - 1, G + h2 // 2


def build_structure(h):
    mc, Structure = h.mc, h.Structure
    s = Structure((W, H, D))
    rng = random.Random(2404)
    cols, pts = classify()
    LEAVES = {"persistent": "true", "distance": "7", "waterlogged": "false"}
    lamp = lambda hanging="false": ("guhs:lampion_" + rng.choice(["roze", "geel", "mint"]), {"hanging": hanging, "waterlogged": "false"})

    def track(x, z):
        c = cols.get((x, z))
        return c if c and abs(c[0]) <= KERB else None

    def wall_needed(x, z, c):
        off, h2, seg, sd, length = c
        if not KERB < abs(off) <= WALL:
            return False
        if seg == 0 and off > 0 and 31 <= x <= 33:
            return False                                     # the way from the pit stop onto the track
        if off > 0:
            return True                                      # always on the outside
        return base_h2(seg, z) > 0                          # inside only where it's high (bridge, vadsbult)

    # --- the ground: dirt, grass; a path of pink concrete to the pit stop -------------------------------------------------
    fp = [(x, z) for x in range(W) for z in range(D)]
    for x, z in fp:
        s.fill(x, 0, z, x, G - 2, z, mc("dirt"))
        s.set(x, G - 1, z, mc("grass_block"), {"snowy": "false"})
    # --- the kaas saus pond and river (under the bridge) --------------------------------------------------------------------
    for x, z in fp:
        if in_river(x, z) and not (track(x, z) and not (87 <= x <= 97)) and x < 99:
            for y in (G - 2, G - 1):
                s.set(x, y, z, "guhs:kaas_saus", {"level": "0"})
            s.set(x, G - 3, z, mc("yellow_terracotta"))
    for x, z in fp:                                           # a pink rim around the kaas saus
        if not in_river(x, z) and not cols.get((x, z)) and any(in_river(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            s.set(x, G - 1, z, mc("pink_terracotta"))
            s.set(x, G - 2, z, mc("pink_terracotta"))
    for z in range(46, 55):
        s.fill(99, G - 3, z, 99, G - 1, z, mc("pink_terracotta"))

    # --- the big guh head (the track goes right through it: in at the mouth, out at the back) -------------------------------
    cx, cy, cz, r = HEAD
    for x in range(cx - r - 1, cx + r + 2):
        for y in range(G - 1, cy + r + 1):
            for z in range(cz - r - 1, cz + r + 2):
                if in_head(x, y, z):
                    s.set(x, y, z, mc("pink_wool"))
    for ez in (cz - 8, cz + 8):                               # ears: flat round discs on top, pink inside
        for y in range(cy + 7, cy + 17):
            for z in range(ez - 5, ez + 6):
                for x in range(cx - 2, cx + 2):
                    if (y - (cy + 12)) ** 2 / 25 + (z - ez) ** 2 / 20 <= 1:
                        s.set(x, y, z, mc("magenta_wool") if x == cx + 1 and (y - (cy + 12)) ** 2 / 10 + (z - ez) ** 2 / 8 <= 1 else mc("pink_wool"))

    def face_front(y, z):
        for x in range(cx + r + 1, cx - 1, -1):
            if s.get(x, y, z) == mc("pink_wool"):
                return x
        return None

    def face_spot(y0, z0, rad, block):
        for y in range(int(y0 - rad - 1), int(y0 + rad + 2)):
            for z in range(int(z0 - rad - 1), int(z0 + rad + 2)):
                if (y - y0) ** 2 + (z - z0) ** 2 <= rad * rad:
                    fx = face_front(y, z)
                    if fx is not None:
                        s.set(fx, y, z, mc(block))
    for ez in (cz - 5, cz + 5):
        face_spot(cy + 5, ez, 1.9, "black_wool")
        face_spot(cy + 6, ez + 1, 0.5, "white_wool")
        face_spot(cy + 1, ez + (-3 if ez < cz else 3), 1.6, "magenta_wool")   # blushing cheeks
    face_spot(cy + 2.5, cz, 1.0, "magenta_wool")                                # the nose

    # --- the track: surface, kerbs, embankments, the bridge deck; room above it ----------------------------------------------
    for (x, z), (off, h2, seg, sd, length) in cols.items():
        c = cols[(x, z)]
        if abs(off) > WALL:
            continue
        if abs(off) > KERB and not wall_needed(x, z, c):
            continue
        top, slab = level_blocks(h2)
        over_river = in_river(x, z) and seg == 2
        bottom = top - 1 if over_river else 0
        if abs(off) <= KERB:
            if abs(off) <= 0.5 and int(sd) // 3 % 2 == 0:
                full, half = mc("smooth_quartz"), mc("smooth_quartz_slab")            # the dashed middle line
            elif abs(off) > HALF:
                white = int(sd) // 2 % 2 == 0
                full, half = (mc("smooth_quartz"), mc("smooth_quartz_slab")) if white else (mc("purpur_block"), mc("purpur_slab"))
            else:
                full, half = mc("cherry_planks"), mc("cherry_slab")
            for y in range(bottom, top + 1):
                s.set(x, y, z, full if y == top else (mc("pink_terracotta") if y >= G else mc("dirt")))
            if slab is not None:
                s.set(x, slab, z, half, {"type": "bottom", "waterlogged": "false"})
            walk = G + (h2 + 1) // 2
            for y in range(walk, walk + 7):
                if (x, y, z) in s.blocks and not (in_head(x, y, z) and y >= G + 7):
                    s.set(x, y, z, mc("air"))
            if walk + 7 < H and not in_head(x, walk + 7, z):
                for y in range(walk, H):
                    s.set(x, y, z, mc("air"))
        else:
            # the barrier: two blocks (white, pink glass) on top of the kerb's height; a post with a lampgion now and then
            kerb_h2 = h2
            base = G + (kerb_h2 + 1) // 2
            if cx - r - 1 <= x <= cx + r + 1 and in_head(x, G + 2, z):
                continue                                    # (inside the head the wool is the wall)
            for y in range(bottom, base):
                s.set(x, y, z, mc("white_concrete") if y >= G else mc("dirt"))
            post = int(sd) % 8 == 0 and off > 0
            if post:
                s.fill(x, base, z, x, base + 2, z, mc("quartz_pillar"), {"axis": "y"})
                s.set(x, base + 3, z, *lamp())
            else:
                s.set(x, base, z, mc("white_concrete"))
                s.set(x, base + 1, z, mc("pink_stained_glass"))
            for y in range(base + (4 if post else 2), base + 8):
                if (x, y, z) in s.blocks and not in_head(x, y, z):
                    s.set(x, y, z, mc("air"))

    # --- the tunnel through the head: the mouth (tongue and two front teeth), lamps in the walls ----------------------------
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(15, 26):
            if track(x, z) and in_head(x, G + 2, z):
                for y in range(G, G + 7):
                    s.set(x, y, z, mc("air"))
    mouth = max(x for x in range(cx, cx + r + 2) if s.get(x, G + 7, cz) == mc("pink_wool"))
    for z in (cz - 1, cz + 1):                                # the front teeth hang from the top of the mouth
        s.set(max(x for x in range(cx, cx + r + 2) if s.get(x, G + 7, z) == mc("pink_wool")), G + 6, z, "guhs:tand")
    for x in range(mouth - 5, mouth + 1):
        for z in range(cz - 2, cz + 3):
            s.set(x, G - 1, z, "guhs:tong")
    for x in range(cx - r + 2, mouth, 4):
        for z in (15, 25):
            if s.get(x, G + 3, z) == mc("pink_wool"):
                s.set(x, G + 3, z, "guhs:guh_kristal_lamp")
        if s.get(x, G + 7, cz) == mc("pink_wool"):
            s.set(x, G + 7, cz, "guhs:guh_kristal_lamp")
    for x in range(cx - r - 1, cx + r + 2):                  # the inside of the throat is a darker pink
        for z in (15, 25):
            for y in range(G, G + 7):
                if s.get(x, y, z) == mc("pink_wool") and track(x, z + (1 if z == 15 else -1)):
                    s.set(x, y, z, mc("magenta_wool"))

    # --- the checkpoint rings ------------------------------------------------------------------------------------------------
    def ring(number, axis, at, lo, hi, walk):
        """Posts at both sides from the walking level, a beam on top (glowing checkpoint blocks), guh ears on the beam."""
        props = {"nummer": str(number), "baan": "0"}
        top = walk + 6
        for a in (lo, hi):
            for y in range(walk, top):
                s.set(*((at, y, a) if axis == "x" else (a, y, at)), "guhs:race_checkpoint", props)
            for y in range(G - 3, walk):
                p = (at, y, a) if axis == "x" else (a, y, at)
                if s.get(*p) in (None, mc("air")):
                    s.set(*p, mc("white_concrete"))
        for a in range(lo, hi + 1):
            s.set(*((at, top, a) if axis == "x" else (a, top, at)), "guhs:race_checkpoint", props)
        mid = (lo + hi) // 2
        for e in (mid - 3, mid + 3):
            for dy, width in ((1, 1), (2, 0)):
                for d in range(-width, width + 1):
                    s.set(*((at, top + dy, e + d) if axis == "x" else (e + d, top + dy, at)), mc("pink_wool"))

    def walk_at(x, z):
        return G + (cols[(x, z)][1] + 1) // 2

    ring(1, "z", 62, 87, 97, walk_at(92, 62))                 # after the first curve, up the bridge
    ring(2, "z", 50, 87, 97, walk_at(92, 50))                 # on the bridge
    ring(4, "z", 39, 9, 19, walk_at(14, 39))                  # the start of the west straight
    ring(5, "z", 50, 9, 19, walk_at(14, 50))                  # on top of the vadsbult
    # ring 3 is in the head: the walls and ceiling of the throat glow
    for z in (15, 25):
        for y in range(G, G + 7):
            s.set(cx, y, z, "guhs:race_checkpoint", {"nummer": "3", "baan": "0"})
    for z in range(15, 26):
        s.set(cx, G + 7, z, "guhs:race_checkpoint", {"nummer": "3", "baan": "0"})

    # --- the start/finish gate: a giant guh face over the track (you ride into its mouth) ---------------------------------------
    fy, fz, fr = G + 7, 80, 10
    for x in (GATE0_X, GATE0_X + 1):
        for y in range(G - 2, fy + fr + 1):
            for z in range(fz - fr, fz + fr + 1):
                if (y - fy) ** 2 + (z - fz) ** 2 <= fr * fr + 2:
                    s.set(x, y, z, mc("pink_wool"))
        for ez in (fz - 7, fz + 7):                                                     # ears
            for y in range(fy + fr - 3, fy + fr + 5):
                for z in range(ez - 4, ez + 5):
                    d = (y - (fy + fr + 1)) ** 2 / 12 + (z - ez) ** 2 / 12
                    if d <= 1:
                        s.set(x, y, z, mc("magenta_wool") if d <= 0.35 and x == GATE0_X else mc("pink_wool"))
        for z in range(76, 85):                                                          # the mouth (the way through)
            for y in range(G, G + 7):
                s.set(x, y, z, mc("air"))
        for y in range(G, G + 7):                                                        # glowing lips = checkpoint 0
            s.set(x, y, 75, "guhs:race_checkpoint", {"nummer": "0", "baan": "0"})
            s.set(x, y, 85, "guhs:race_checkpoint", {"nummer": "0", "baan": "0"})
        for z in range(75, 86):
            s.set(x, G + 7, z, "guhs:race_checkpoint", {"nummer": "0", "baan": "0"})
    for ez in (fz - 4, fz + 4):                                                          # eyes, cheeks, nose (on the west side)
        for y in (fy + 4, fy + 5):
            for z in (ez, ez + 1):
                s.set(GATE0_X, y, z, mc("black_wool"))
        s.set(GATE0_X, fy + 5, ez, mc("white_wool"))
        for z in range(ez + (-3 if ez < fz else 2), ez + (-1 if ez < fz else 4)):
            s.set(GATE0_X, fy + 2, z, mc("magenta_wool"))
    s.set(GATE0_X, fy + 2, fz, mc("magenta_wool"))
    s.set(GATE0_X, fy + 2, fz - 1, mc("magenta_wool"))
    s.set(GATE0_X, fy + 3, fz - 1, mc("magenta_wool"))
    s.set(GATE0_X, fy + 3, fz, mc("magenta_wool"))
    for x in (GATE0_X, GATE0_X + 1):                                                     # the chequered finish line under it
        for z in range(76, 85):
            s.set(x, G - 1, z, mc("black_concrete") if (x + z) % 2 else mc("white_concrete"))

    # --- VAHOEG pads, the start marker ------------------------------------------------------------------------------------------
    for (a0, a1), axis, mid, facing, lastig in PADS:
        for a in range(a0, a1 + 1):
            for d in range(-3, 4):
                x, z = (a, mid + d) if axis == "x" else (mid + d, a)
                s.set(x, walk_at(x, z), z, "guhs:race_vahoegpad", {"facing": facing, "lastig": "true" if lastig else "false"})
    s.set(*START, "guhs:race_start", {"facing": "east", "baan": "0"})
    for pos, facing in MIKAS:
        s.set(*pos, "guhs:circuit_mikaplek", {"facing": facing, "vanaf": "2"})

    # --- the grandstand (tribune): five rows of vadszakken under a striped roof, a guh portrait on the back wall ----------------
    X0, X1 = 45, 75
    for i in range(5):
        z0 = 88 + 2 * i
        for z in (z0, z0 + 1):
            for x in range(X0, X1 + 1):
                for y in range(G, G + i):
                    s.set(x, y, z, mc("white_concrete") if (x + y) % 2 else mc("pink_concrete"))
        for x in range(X0 + 1, X1):
            if (x - X0) % 7 == 0:
                if i > 0:
                    s.set(x, G + i - 1, z0, mc("quartz_stairs"), {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            else:
                colour = ("pink", "white", "magenta", "yellow")[(x + i) % 4]
                s.set(x, G + i, z0, f"guhs:{colour}_zitzak", {"facing": "north"})
    for x in range(X0, X1 + 1):
        s.fill(x, G, 98, x, G + 10, 98, mc("white_concrete"))
        for z in range(86, 99):
            s.set(x, G + 11, z, mc("pink_wool") if (x // 2) % 2 else mc("white_wool"))
        s.set(x, G + 10, 86, "guhs:vlaggetjes", {"axis": "x"})
    for x in (X0, 53, 61, 69, X1):
        s.fill(x, G, 86, x, G + 10, 86, mc("quartz_pillar"), {"axis": "y"})
        s.fill(x, G + 4, 97, x, G + 10, 97, mc("quartz_pillar"), {"axis": "y"})
    for x in range(X0 + 3, X1, 6):
        s.set(x, G + 10, 92, *lamp("true"))
    h.ms.guh_portrait(s, 57, G + 3, 98, facing_south=False)
    for z in range(86, 99):                                        # side walls up to the roof, open at the front rows
        for x in (X0, X1):
            for y in range(G + (z - 86) // 2, G + 11):
                if z >= 90:
                    s.set(x, y, z, mc("white_concrete") if (y + z) % 3 else mc("pink_concrete"))

    # --- the pit stop: the Raceguh's garage (chequered floor, spare guh wheel, kaas saus tank, barrels) ----------------------------
    PX0, PX1, PZ0, PZ1 = 20, 36, 87, 97
    for x in range(PX0, PX1 + 1):
        for z in range(PZ0, PZ1 + 1):
            s.set(x, G - 1, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
            for y in range(G, G + 6):
                s.set(x, y, z, mc("air"))
            wall = x in (PX0, PX1) or z == PZ1
            if wall:
                s.fill(x, G, z, x, G + 5, z, mc("white_concrete") if (x + z) % 4 else mc("pink_terracotta"))
        for z in range(PZ0 - 1, PZ1 + 1):
            s.set(x, G + 6, z, mc("pink_wool") if (x + z) % 2 else mc("white_wool"))
        s.set(x, G + 5, PZ0 - 1, "guhs:vlaggetjes", {"axis": "x"})
    for x in (PX0, PX1):
        s.fill(x, G, PZ0 - 1, x, G + 5, PZ0 - 1, mc("quartz_pillar"), {"axis": "y"})
    for x in range(27, 30):                                        # the door at the back
        for y in range(G, G + 3):
            s.set(x, y, PZ1, mc("air"))
    for x in (24, 32):
        s.set(x, G + 6, 92, "guhs:guh_kristal_lamp")
        s.set(x, G + 5, 89, *lamp("true"))
    h.ms.big_wheel_block(s, 23, G, 96, "north")                    # the spare "tyre"
    for z in range(93, 97):                                        # the kaas saus tank (for thirsty race guhs)
        for x in range(32, 36):
            for y in range(G, G + 4):
                edge = x in (32, 35) or z in (93, 96) or y == G + 3
                s.set(x, y, z, mc("orange_stained_glass") if edge else "guhs:kaas_saus", None if edge else {"level": "0"})
    for z in (88, 89, 90):
        s.set(21, G, z, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(21, G + 1, 89, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(35, G, 88, "guhs:block_of_kaasknabbels")
    s.set(35, G + 1, 88, "guhs:block_of_kaasknabbels")
    s.set(34, G, 88, "guhs:block_of_kaasknabbels")
    s.set(21, G, 93, "guhs:guh_kast", {"facing": "east", "open": "false"})
    s.set(21, G, 94, "guhs:guh_kast", {"facing": "east", "open": "false"})
    s.set(26, G, 96, "guhs:guh_tafel")
    s.set(26, G + 1, 96, "guhs:guh_taart", {"bites": "0"})
    s.entity(NPC[0], NPC[1] + 0.0, NPC[2], {"id": "guhs:guh_npc", "Kind": "raceguh", "PersistenceRequired": h.Byte(1),
                                           "Rotation": h.floats(180.0, 0.0)})
    for x in range(31, 34):                                        # the pit lane: from the garage onto the track
        for z in (85, 86):
            s.set(x, G - 1, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
            for y in range(G, G + 4):
                s.set(x, y, z, mc("air"))

    # --- the entrance: a path from the south edge to the pit stop door, under a welcome arch with guh ears ------------------------
    for z in (98, 99):
        for x in range(26, 31):
            s.set(x, G - 1, z, mc("pink_concrete") if x in (26, 30) else mc("white_concrete"))
            for y in range(G, G + 6):
                s.set(x, y, z, mc("air"))
    for x in (25, 31):
        s.fill(x, G, 99, x, G + 5, 99, mc("quartz_pillar"), {"axis": "y"})
        s.set(x, G + 6, 99, *lamp())
    for x in range(25, 32):
        s.set(x, G + 5, 99, mc("pink_wool"))
    for x in (26, 30):
        s.set(x, G + 6, 99, mc("pink_wool"))

    # --- the infield: the winners' podium, guhbloesem trees, flowers, lampgion posts -----------------------------------------------
    for x0, height, top in ((46, 2, mc("iron_block")), (48, 3, mc("gold_block")), (50, 1, mc("copper_block"))):
        for x in (x0, x0 + 1):
            for z in (69, 70):
                s.fill(x, G, z, x, G + height - 2, z, mc("white_concrete")) if height > 1 else None
                s.set(x, G + height - 1, z, top)
    s.set(48, G + 3, 69, "guhs:guh_taart", {"bites": "0"})
    s.set(49, G + 3, 69, *lamp())
    s.set(46, G + 2, 69, *lamp())
    s.set(51, G + 1, 69, *lamp())

    def free(x, z, rad):
        for dx in range(-rad, rad + 1):
            for dz in range(-rad, rad + 1):
                p = (x + dx, z + dz)
                if not (0 <= p[0] < W and 0 <= p[1] < D) or cols.get(p) or in_river(*p) or s.get(p[0], G, p[1]) not in (None, mc("air")):
                    return False
        return True

    for tx, tz in ((30, 45), (40, 60), (72, 60), (78, 45), (40, 38), (70, 36), (6, 6), (93, 6), (6, 93), (4, 78), (85, 92), (96, 30), (40, 5)):
        if not free(tx, tz, 3):
            continue
        for y in range(G, G + 5):
            s.set(tx, y, tz, "guhs:guhbloesem_log", {"axis": "y"})
        for dx in range(-3, 4):
            for dy in range(-1, 3):
                for dz in range(-3, 4):
                    if dx * dx + dz * dz + dy * dy * 2 <= 9 and (dx, dz) != (0, 0) or (dx, dz) == (0, 0) and dy >= 1:
                        p = (tx + dx, G + 4 + dy, tz + dz)
                        if s.get(*p) in (None, mc("air")):
                            s.set(*p, "guhs:guhbloesem_leaves", LEAVES)
    for x, z in fp:
        if cols.get((x, z)) or in_river(x, z) or s.get(x, G, z) not in (None,) or s.get(x, G - 1, z) != mc("grass_block"):
            continue
        roll = rng.random()
        if roll < 0.05:
            s.set(x, G, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
        elif roll < 0.10:
            s.set(x, G, z, "guhs:roze_gras")
    # lampgion posts along the inside of the track and around the field (so nothing spawns in the dark)
    posts = []
    for p in pts[::45]:
        x, z = int(p[0] - p[3][0] * 7), int(p[1] - p[3][1] * 7)
        posts.append((x, z))
    posts += [(x, z) for x in range(4, 100, 12) for z in (2, 60)] + [(x, z) for z in range(4, 100, 12) for x in (2, 97)]
    for x, z in posts:
        if 0 <= x < W and 0 <= z < D and not cols.get((x, z)) and not in_river(x, z) and s.get(x, G, z) in (None, mc("air"), "guhs:roze_gras",
                                                                                                         "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes") \
                and s.get(x, G - 1, z) == mc("grass_block") and not in_head(x, G + 2, z):
            s.fill(x, G, z, x, G + 1, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
            s.set(x, G + 2, z, *lamp())

    s.clear_above(fp, G)
    return s, cols


# ----------------------------------------------------------------------------------------------------------------------
# the geometry self-check
# ----------------------------------------------------------------------------------------------------------------------
PASSABLE = ("minecraft:air", "guhs:race_vahoegpad", "guhs:race_start", "guhs:circuit_mikaplek", "guhs:roze_gras", "guhs:roze_guhbloem", "guhs:knabbelroos",
            "guhs:kaasbloem", "guhs:guhoortjes", "guhs:vlaggetjes")
LIQUID = ("guhs:kaas_saus",)


def passable(s, x, y, z):
    b = s.get(x, y, z)
    return b is None or b in PASSABLE


def solid(s, x, y, z):
    b = s.get(x, y, z)
    return b is not None and b not in PASSABLE and b not in LIQUID


def check(s, cols):
    problems = []
    # 1. the track: no holes, a gentle surface (at most half a block between neighbours), room for a rider (4 blocks)
    track = {p: c for p, c in cols.items() if abs(c[0]) <= KERB}
    for (x, z), (off, h2, seg, sd, length) in track.items():
        top, slab = level_blocks(h2)
        if not solid(s, x, top, z) or (slab is not None and not solid(s, x, slab, z)):
            problems.append(f"hole in the track at {x},{z}")
        surface = s.get(x, top if slab is None else slab, z)
        if surface not in TRACK_BLOCKS:
            problems.append(f"the race guh may not run on {surface} at {x},{z} (not in the race_track tag)")
        walk = G + (h2 + 1) // 2
        for y in range(walk, walk + 4):
            if not passable(s, x, y, z):
                problems.append(f"no room above the track at {x},{y},{z}: {s.get(x, y, z)}")
                break
        for dx, dz in ((1, 0), (0, 1)):
            n = track.get((x + dx, z + dz))
            if n and abs(n[1] - h2) > 1:
                problems.append(f"step of {abs(n[1] - h2) / 2} blocks in the track at {x},{z}")
    # 2. every checkpoint ring spans the track (the ring's box covers the track across), the start marker is free
    rings = {}
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b == "guhs:race_checkpoint":
            rings.setdefault(int(props["nummer"]), []).append((x, y, z))
    if sorted(rings) != list(range(6)):
        problems.append(f"rings: {sorted(rings)}")
    for n, blocks in rings.items():
        xs, zs = [b[0] for b in blocks], [b[2] for b in blocks]
        if max(max(xs) - min(xs), max(zs) - min(zs)) < 10:
            problems.append(f"ring {n} is only {max(max(xs) - min(xs), max(zs) - min(zs))} wide")
    sx, sy, sz = START
    if not (passable(s, sx, sy + 1, sz) and solid(s, sx, sy - 1, sz)) or s.get(sx, sy, sz) != "guhs:race_start":
        problems.append("the start marker isn't on the track")
    # the race line: driving one lap along the middle from the start goes through rings 1, 2, 3, 4, 5 and then 0, in that
    # order, with the same boxes as RaceTrack (the blocks of a ring, grown by 0.5 / 1 / 0.5); the start is outside ring 0
    boxes = {}
    for n, blocks in rings.items():
        lo = [min(b[i] for b in blocks) for i in range(3)]
        hi = [max(b[i] for b in blocks) + 1 for i in range(3)]
        boxes[n] = (lo[0] - 0.5, lo[1] - 1, lo[2] - 0.5, hi[0] + 0.5, hi[1] + 1, hi[2] + 0.5)

    def inside(box, p):
        return box[0] <= p[0] < box[3] and box[1] <= p[1] < box[4] and box[2] <= p[2] < box[5]
    if 0 in boxes and inside(boxes[0], (sx + 0.5, sy, sz + 0.5)):
        problems.append("the start marker is inside the finish ring")
    pts = samples()
    first = min(range(len(pts)), key=lambda i: math.dist(pts[i][:2], (sx + 0.5, sz + 0.5)))
    order, current = [], None
    for k in range(len(pts) + 1):
        x, z = pts[(first + k) % len(pts)][:2]
        c = cols.get((int(x), int(z)))
        y = G + (c[1] + 1) // 2 if c else G
        now = [n for n, box in boxes.items() if inside(box, (x, y + 0.1, z))]
        if now and now[0] != current:
            order.append(now[0])
        current = now[0] if now else None
    if order != [1, 2, 3, 4, 5, 0]:
        problems.append(f"driving a lap goes through the rings {order}")
    # 2.9: the lastig Mika-pikker spots stand on the ground beside the track; the boards of makkelijk and lastig float free
    for pos, facing in MIKAS:
        if s.get(*pos) != "guhs:circuit_mikaplek" or not solid(s, pos[0], pos[1] - 1, pos[2]) or cols.get((pos[0], pos[2])):
            problems.append(f"Mika-pikker spot {pos} isn't beside the track on solid ground")
    for pos in BOARDS:
        if s.get(*pos) not in (None, "minecraft:air"):
            problems.append(f"the scoreboard spot {pos} isn't free: {s.get(*pos)}")
    # 3. the Raceguh stands on solid ground with room around her
    nx, ny, nz = int(NPC[0]), int(NPC[1]), int(NPC[2])
    if not (solid(s, nx, ny - 1, nz) and passable(s, nx, ny, nz) and passable(s, nx, ny + 1, nz)):
        problems.append("the Raceguh isn't standing on solid ground")
    # 4. walking: from the south edge you reach the Raceguh, the track and the grandstand (step up 1, fall at most 3)
    def standable(x, y, z):
        return 0 <= x < W and 0 <= z < D and 0 < y < H - 1 and passable(s, x, y, z) and passable(s, x, y + 1, z) and solid(s, x, y - 1, z)
    start = [(x, G, D - 1) for x in range(26, 31) if standable(x, G, D - 1)]
    seen, todo = set(start), list(start)
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if n in seen or not standable(*n):
                    continue
                if dy == 1 and not passable(s, x, y + 2, z):
                    continue
                seen.add(n)
                todo.append(n)
                break
    for name, spot in (("the Raceguh", (nx, ny, nz - 2)), ("the start", (sx + 3, sy, sz)), ("the top of the grandstand", (60, G + 4, 97)),
                       ("the infield", (48, G, 60))):
        if spot not in seen:
            problems.append(f"can't walk to {name} at {spot}")
    # 5. nothing floats: every block hangs together with the ground
    solid_blocks = {p for p, (b, _, _) in s.blocks.items() if b != "minecraft:air"}
    todo = [p for p in solid_blocks if p[1] == 0]
    connected = set(todo)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid_blocks and n not in connected:
                connected.add(n)
                todo.append(n)
    floating = solid_blocks - connected
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:5]}")
    # 6. dark spots on the track or the stands would let mobs spawn: a light within 8 blocks of every walkable track column
    lights = [p for p, (b, _, _) in s.blocks.items() if "lampion" in b or b in ("guhs:race_checkpoint", "guhs:guh_kristal_lamp", "guhs:race_vahoegpad")]
    L = np.array(lights)
    far = 0
    for (x, z), c in track.items():
        if np.min(np.abs(L[:, 0] - x) + np.abs(L[:, 2] - z)) > 12:
            far += 1
    if far:
        problems.append(f"{far} track columns far from any light")
    return problems


def lap_length():
    return sum((math.dist(seg[1], seg[2]) if seg[0] == "line" else seg[2] * math.radians(seg[4] - seg[3])) for seg in SEGMENTS)


def structure(h):
    h.TEMPLATE_SIZES[NAME] = W
    h.FLATNESS[NAME] = 24                  # (sampled 100 blocks around the start: the track is sunk 3 and cleared 30 up)
    none = {"bounding_box": "full", "spawns": []}
    h.structure(NAME, h.GUHMENSION_LAND, spacing=32, separation=12, salt=20240110 + 1,
                spawn_overrides={"creature": none, "monster": none, "ambient": none}, start_y=-(G - 1))
    s, cols = build_structure(h)
    problems = check(s, cols)
    if problems:
        raise SystemExit("guh racebaan self-check failed:\n  " + "\n  ".join(problems[:40]))
    print(f"guh racebaan: lap {lap_length():.0f} blocks, {sum(1 for c in cols.values() if abs(c[0]) <= KERB)} track columns, self-check ok")
    s.save(NAME)


def build(h):
    textures(h)
    models(h)
    tags(h)
    advancements(h)
    texts(h)
    structure(h)


# ======================================================================================================================
# FTB quests (row y=28)
# ======================================================================================================================
def ftb(fq):
    fq.q("race_baan", "Op je plaatsen, vads!", "Vind de &dguhracebaan&r (superkompas: Minigames > Guhracebaan). In de pitstop wacht de Raceguh met een huur-renguh voor je.",
         "guhs:guhmensie_superkompas", [fq.structure(NAME)], x=-8, y=28)
    fq.q("race_eerste", "VROEM VROEM!", "Rij je eerste guhrace: 3 rondes door alle lichtgevende ringen. Je hoeft niks mee te nemen: de Raceguh leent je een renguh.",
         "guhs:raceprijsje", [fq.adv("race_eerste")], rewards=(("guhs:kaas_knabbels", 16),), x=-6, y=28, xp=100)
    fq.q("race_brons", "Brons!", "Rij de race binnen 1:45. Tip: de &6VAHOEG-pads&r geven je een flinke zet!",
         "minecraft:copper_block", [fq.adv("race_brons")], rewards=(("guhs:raceprijsje", 2),), x=-4, y=28, xp=100)
    fq.q("race_goud", "GOUD! VAHOEG!", "Rij de race binnen 1:12. Neem de binnenbochten en mis geen enkele pad!",
         "minecraft:gold_block", [fq.adv("race_goud")], rewards=(("guhs:raceprijsje", 4),), x=-2, y=28, shape="gear", xp=300)
    fq.q("race_geest", "Spookrijder", "Verbeter je eigen record terwijl je doorzichtige &bgeest&r meerijdt. Wie is er sneller: jij of jij?",
         "minecraft:phantom_membrane", [fq.adv("race_geest")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=0, y=28, xp=200)
    fq.q("race_prijsjes", "Prijzenkast", "Spaar 10 raceprijsjes.", "guhs:raceprijsje", [fq.item("guhs:raceprijsje", 10)],
         rewards=(("guhs:kaas_knabbels", 24),), x=2, y=28)
    fq.q("race_bril", "Vahoege racebril", "Koop een &dvahoege racebril&r voor je guh bij de Raceguh (raceprijsjes).",
         "guhs:racebril", [fq.item("guhs:racebril")], x=4, y=28)
    fq.q("race_lastig", "Racen op lastig", "De guhracebaan heeft nu &amakkelijk&r, &emedium&r en &clastig&r. Rij hem uit op lastig: een supersnelle maar eigenwijze renguh, Mika-pikkers langs de baan, en de zilveren VAHOEG-pads doen het niet!",
         "guhs:raceprijsje", [fq.adv("guhs:grote_guhspelen/race_lastig")], rewards=(("guhs:raceprijsje", 3),), x=8, y=28, xp=200)
    fq.q("race_jockey", "Vroem vroem, guh!", "Koop het hele jockeypakje: jockeypetje, gestreept zijden jockeyjasje en racebril. Alleen bij de Raceguh!",
         "guhs:jockey_pet", [fq.adv("guhs:guhmension/race_jockey")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=6, y=28, shape="gear", xp=300)
