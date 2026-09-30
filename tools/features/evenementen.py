"""
Guh events in the Guhmension (evenementen): the kaasregen, the Vadsparade and the sterrenregen, started by a scheduler
(about every 2-3 Minecraft days per player) or by an op with /guhs evenement <kaasregen|parade|sterrenregen>.
Java: nl.juiced.guhs.feature.evenementen. No structures: events happen wherever you are (outdoors).

  build(h)   item textures and models, texts, the hidden quest advancements, a self-check
  ftb(fq)    the FTB quests (row y=48)
  BONES / clothes / CLOTHES / icons   the parade outfit on a guh (sjako, paradejasje, trommeltje)
"""
import os

import numpy as np
from PIL import Image

CLOTHES = ["vadsparade_sjako", "vadsparade_jasje", "vadsparade_trommeltje"]
ITEMS = ["gouden_kaasknabbel", "sterrenstof"]
ENTITIES = ["parade_guh", "vallende_knabbel", "vallende_ster"]
QUEST_ADVANCEMENTS = ["evenement_eerste", "evenement_kaasregen", "evenement_gouden_knabbel", "evenement_parade",
                      "evenement_paradepakje", "evenement_sterrenguh", "evenement_alle"]

# =====================================================================================================================
# the parade outfit on a guh (make_guh_variants.py): a drum major's sjako with a golden band and a white plume, a
# parade jacket (the suit bones) with golden epaulettes, and a little drum on a strap at the guh's left side. The bones
# reuse existing swatch spots (every piece has its own texture), so they take no new texture space.
# =====================================================================================================================
_H = [0, 6, -2]          # (make_guh_variants HEAD_PIVOT / BODY_PIVOT)
_B = [0, 6, 6]
BONES = {
    # the sjako: sits between the ears (they start at x=4.5), a little visor over the eyes
    "outfit_parade_sjako": ("head", _H, "tall_hat", [
        ([-3, 15, -8.6], [6, 5.2, 5.2], 0), ([-3.3, 19.8, -8.9], [6.6, 0.6, 5.8], 0), ([-3, 15, -10.4], [6, 0.4, 1.9], 0)]),
    "outfit_parade_sjako_band": ("head", _H, "santa_trim", [
        ([-3.2, 15.3, -8.8], [6.4, 1.0, 5.6], 0), ([-1, 16.8, -8.95], [2, 2, 0.4], 0)]),
    "outfit_parade_sjako_pluim": ("head", _H, "feather", [
        ([-0.5, 20.4, -6.5], [1, 1.6, 1], 0), ([-1.4, 21.8, -7.4], [2.8, 2.4, 2.8], 0), ([-0.9, 24.2, -6.9], [1.8, 0.8, 1.8], 0)]),
    # golden epaulettes on both shoulders (just behind the head), with a fringe hanging over the sides
    "outfit_parade_epaulet": ("body", _B, "crown", [
        ([3.8, 10.6, 0.5], [3.4, 1.2, 3], 0), ([-7.2, 10.6, 0.5], [3.4, 1.2, 3], 0),
        ([6.8, 9.4, 0.5], [0.5, 1.2, 3], 0), ([-7.3, 9.4, 0.5], [0.5, 1.2, 3], 0)]),
    # the trommeltje: a little drum at the left side, rims, two sticks on top and a strap over the back
    "outfit_parade_trommel": ("body", _B, "backpack", [([6.6, 3.2, 2.5], [3.4, 3.2, 3.4], 0)]),
    "outfit_parade_trommel_rand": ("body", _B, "chain", [
        ([6.4, 6.2, 2.3], [3.8, 0.6, 3.8], 0), ([6.4, 3.0, 2.3], [3.8, 0.6, 3.8], 0)]),
    "outfit_parade_trommel_band": ("body", _B, "stethoscope", [
        ([-6.6, 11.0, 3.4], [13.2, 0.4, 1.3], 0), ([6.5, 6.8, 3.4], [0.4, 4.4, 1.3], 0), ([-6.9, 8.6, 3.4], [0.4, 2.8, 1.3], 0),
        ([7.3, 6.8, 1.2], [0.4, 0.4, 5.2], 0), ([8.9, 6.8, 1.8], [0.4, 0.4, 4.6], 0)]),
}

PINK, DEEP, GOLD, GOLD_DARK, WHITE, BLUE = (236, 96, 160), (190, 40, 110), (248, 204, 70), (196, 140, 30), (250, 248, 250), (60, 84, 196)


def clothes(rng, v):
    px = v.SWATCH * 4

    def sjako():
        a = v.fabric(PINK, rng, 8)
        for x in range(0, px, 8):                    # soft vertical pleats
            a[:, x] = np.array(DEEP)
        return np.clip(a, 0, 255)

    def band():
        a = v.fabric(GOLD, rng, 6)
        a[::4, :] = np.array(GOLD_DARK)
        a[:, ::7] = np.array((255, 236, 150))        # a shine here and there
        return np.clip(a, 0, 255)

    def pluim():
        a = v.fabric(WHITE, rng, 5)
        a[:10, :] = np.array((255, 180, 215))        # pink tips
        return np.clip(a, 0, 255)

    def jasje():
        a = v.fabric(BLUE, rng, 8)
        for y in range(3, px, 7):                    # golden braid (brandebourgs) with two rows of buttons
            a[y:y + 1, 6:26] = np.array(GOLD)
            a[y - 1:y + 2, 9:11] = np.array(GOLD_DARK)
            a[y - 1:y + 2, 21:23] = np.array(GOLD_DARK)
        a[:, 15:17] = np.array(WHITE)                # a white stripe down the middle
        a[26:, :] = np.array(PINK)                   # pink hem
        return np.clip(a, 0, 255)

    def epaulet():
        a = v.fabric(GOLD, rng, 6)
        for x in range(0, px, 3):                    # fringe
            a[16:, x] = np.array(GOLD_DARK)
        return np.clip(a, 0, 255)

    def trommel():
        a = v.fabric(WHITE, rng, 4)
        for x in range(px):                          # the red rope zigzag of a real marching drum
            for off in (4, 18):
                y = off + abs((x % 12) - 6)
                a[y:y + 2, x] = np.array((210, 40, 60))
        return np.clip(a, 0, 255)

    def rand():
        a = v.fabric(DEEP, rng, 6)
        a[::5, :] = np.array(PINK)
        return np.clip(a, 0, 255)

    def riem():
        a = v.fabric((245, 240, 235), rng, 5)
        a[:, ::9] = np.array((200, 190, 180))
        return np.clip(a, 0, 255)

    return {
        "vadsparade_sjako": {"tall_hat": sjako, "santa_trim": band, "feather": pluim},
        "vadsparade_jasje": {"suit": jasje, "crown": epaulet},
        "vadsparade_trommeltje": {"backpack": trommel, "chain": rand, "stethoscope": riem},
    }


def icons(ic):
    sjako = ic.icon([
        ".......ww.......", "......wppw......", ".......ww.......", ".......ww.......", "....kkkkkkkk....",
        "....kppppppk....", "....kpdppdpk....", "....kppppppk....", "....kpdggdpk....", "....kppggppk....",
        "....kpdppdpk....", "....kggggggk....", "....kGGGGGGk....", "...kkkkkkkkkk...", "...kkkkkkkk.....",
        "................"], {"w": WHITE, "p": PINK, "d": DEEP, "g": GOLD, "G": GOLD_DARK, "k": (70, 20, 50)})
    jasje = ic.icon(ic.pad([
        "..ggg.....ggg...", ".gGGGbbbbbGGGg..", ".abbbbbwwbbbbba.", ".abgbgbwwbgbgba.", ".aabbbbwwbbbbaa.",
        "..abgbgwwgbgba..", "..abbbbwwbbbba..", "..abgbgwwgbgba..", "..abbbbwwbbbba..", "..apppppppppppa.",
        "..aaaaaaaaaaaa.."]), {"a": (30, 40, 110), "b": BLUE, "g": GOLD, "G": GOLD_DARK, "w": WHITE, "p": PINK})
    trommel = ic.icon([
        "..s.........s...", "...s.......s....", "....s.....s.....", ".....s...s......", "...kkkkkkkkkk...",
        "..kddddddddddk..", "..kwwwwwwwwwwk..", "..kwrwwwrwwwrk..", "..kwwrwrwrwrwk..", "..kwwwrwwwrwwk..",
        "..kwwwwwwwwwwk..", "..kddddddddddk..", "...kkkkkkkkkk...", "................", "................",
        "................"], {"s": (245, 235, 220), "k": (60, 20, 40), "d": DEEP, "w": WHITE, "r": (210, 40, 60)})
    return {"vadsparade_sjako": sjako, "vadsparade_jasje": jasje, "vadsparade_trommeltje": trommel}


# =====================================================================================================================
# item icons (16x16)
# =====================================================================================================================
STERRENSTOF = [
    "................", "...y.......w....", "..yWy...........", "...y......p.....", ".......y..pp....",
    "......yYy.......", ".....yYWYy......", "..yyyYWWWYyyy...", "...yYYWWWYYy....", "....yYYWYYy.....",
    "....yYy.yYy.....", "...yy.....yy..w.", "..p.............", ".ppp......W.....", "..p......WWW....",
    "..........W....."]


def grid16(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch] + (255,))
    return img


def items(h):
    # the golden kaasknabbel: the knabbel bag in shiny gold, with a few sparkles
    bag = Image.open(os.path.join(h.TEX, "item", "kaas_knabbels.png")).convert("RGBA")
    gold = np.asarray(h.ramp(bag, (150, 92, 10), (255, 244, 170))).copy()
    for (x, y) in ((1, 1), (14, 2), (2, 13), (13, 14)):
        gold[y, x] = (255, 255, 235, 255)
    h.save(Image.fromarray(gold), "item", "gouden_kaasknabbel.png")
    h.item_model("gouden_kaasknabbel")
    h.save(grid16(STERRENSTOF, {"y": (250, 214, 90), "Y": (255, 236, 140), "W": (255, 255, 240), "w": (230, 230, 255),
                                "p": (255, 150, 210)}), "item", "sterrenstof.png")
    h.item_model("sterrenstof")


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    # items and entities
    "item.guhs.gouden_kaasknabbel": ("Golden Kaasknabbel", "Gouden kaasknabbel"),
    "item.guhs.gouden_kaasknabbel.lore": ("Fell from the sky in a kaasregen. No wild guh can resist it: tamed at once!",
                                          "Uit de lucht gevallen bij een kaasregen. Geen wilde guh kan hem weerstaan: meteen tam!"),
    "item.guhs.sterrenstof": ("Stardust", "Sterrenstof"),
    "item.guhs.sterrenstof.lore": ("Left behind by a falling star. Throw it up and make a wish!", "Achtergelaten door een vallende ster. Gooi het omhoog en doe een wens!"),
    "item.guhs.sterrenstof.wish": ("You make a wish... and the dark isn't so dark any more. VAHOEG!", "Je doet een wens... en het donker is ineens niet meer zo donker. VAHOEG!"),
    "item.guhs.vadsparade_sjako": ("Parade Shako", "Paradesjako"),
    "item.guhs.vadsparade_jasje": ("Parade Jacket", "Paradejasje"),
    "item.guhs.vadsparade_trommeltje": ("Little Parade Drum", "Paradetrommeltje"),
    "entity.guhs.parade_guh": ("Parade Guh", "Paradeguh"),
    "entity.guhs.tamboerguh": ("Drummer Guh", "Tamboerguh"),
    "entity.guhs.vallende_knabbel": ("Falling Kaasknabbel", "Vallende kaasknabbel"),
    "entity.guhs.vallende_ster": ("Falling Star", "Vallende ster"),
    # the events
    "gui.guhs.evenement.kaasregen": ("Kaasregen", "Kaasregen"),
    "gui.guhs.evenement.parade": ("Vadsparade", "Vadsparade"),
    "gui.guhs.evenement.sterrenregen": ("Sterrenregen", "Sterrenregen"),
    "gui.guhs.evenement.bar": ("%s - %s left", "%s - nog %s"),
    "gui.guhs.evenement.start.kaasregen": ("It's raining KAASKNABBELS! Walk into them to catch them, before the wild guhs gobble them up. Keep an eye out for golden ones!",
                                           "Het regent KAASKNABBELS! Loop ertegenaan om ze te vangen, voordat de wilde guhs ze opsmullen. Let op de gouden!"),
    "gui.guhs.evenement.start.parade": ("Boom boom! The VADSPARADE is coming by! Walk along with the Tamboerguh until the very end for a surprise. VAHOEG!",
                                        "Boem boem! De VADSPARADE komt voorbij! Loop mee met de Tamboerguh tot het allerlaatste eind voor een verrassing. VAHOEG!"),
    "gui.guhs.evenement.start.sterrenregen": ("Look up: a STERRENREGEN! Where a star lands, sometimes a starry guh turns up. Tame it quickly, before it goes back to the stars!",
                                              "Kijk omhoog: een STERRENREGEN! Waar een ster landt, verschijnt soms een sterrenguh. Tem hem snel, voordat hij teruggaat naar de sterren!"),
    "gui.guhs.evenement.left": ("You walked away from the %s.", "Je bent weggelopen van de %s."),
    "gui.guhs.evenement.stopped": ("The %s has stopped.", "De %s is gestopt."),
    "gui.guhs.evenement.kaasregen.caught": ("Caught! %s kaasknabbels", "Gevangen! %s kaasknabbels"),
    "gui.guhs.evenement.kaasregen.golden": ("A GOLDEN kaasknabbel! No wild guh can resist that one...", "Een GOUDEN kaasknabbel! Daar kan geen wilde guh aan weerstaan..."),
    "gui.guhs.evenement.kaasregen.end": ("The kaasregen is over! You caught %s kaasknabbels. Vads!", "De kaasregen is voorbij! Je ving %s kaasknabbels. Vads!"),
    "gui.guhs.evenement.kaasregen.end_none": ("The kaasregen is over... and you didn't catch a single one. Njeg! The guhs are very happy though.",
                                              "De kaasregen is voorbij... en je ving er niet een. Njeg! De guhs zijn er wel heel blij mee."),
    "gui.guhs.evenement.parade.gone": ("The Vadsparade marched out of sight...", "De Vadsparade is uit het zicht gemarcheerd..."),
    "gui.guhs.evenement.parade.stopped": ("The Vadsparade has stopped. Next time, walk along to the end!", "De Vadsparade is gestopt. Loop de volgende keer mee tot het eind!"),
    "gui.guhs.evenement.parade.too_late": ("The Vadsparade is at the end! Walk along all the way next time for a surprise.",
                                           "De Vadsparade is aan het eind! Loop de volgende keer helemaal mee voor een verrassing."),
    "gui.guhs.evenement.parade.reward": ("You walked along all the way! The Tamboerguh gives you: %s (and kaasknabbels). Only from the parade!",
                                         "Je liep helemaal mee! De Tamboerguh geeft je: %s (en kaasknabbels). Alleen bij de parade te krijgen!"),
    "gui.guhs.evenement.parade.title": ("VAHOEG!", "VAHOEG!"),
    "gui.guhs.evenement.parade.subtitle": ("You get: %s", "Je krijgt: %s"),
    "gui.guhs.evenement.sterrenregen.guh": ("A starry guh landed with a star! Quick, feed it kaasknabbels!", "Er landde een sterrenguh met een ster! Snel, voer hem kaasknabbels!"),
    "gui.guhs.evenement.sterrenregen.tamed": ("The starry guh stays with you, for good. Make a wish!", "De sterrenguh blijft bij je, voor altijd. Doe een wens!"),
    "gui.guhs.evenement.sterrenregen.end": ("The %s is over. The last starry guhs twinkle back up into the sky.", "De %s is voorbij. De laatste sterrenguhs twinkelen terug de lucht in."),
    # why not (the command)
    "gui.guhs.evenement.not_here": ("Guh events only happen in the Guhmension.", "Guh-evenementen gebeuren alleen in de Guhmensie."),
    "gui.guhs.evenement.already": ("There's an event going on around you already!", "Er is al een evenement bij jou in de buurt!"),
    "gui.guhs.evenement.busy": ("Finish your minigame first.", "Speel eerst je minigame uit."),
    "gui.guhs.evenement.indoors": ("Events only happen outdoors, under the open sky (not in a building or underground).",
                                   "Evenementen gebeuren alleen buiten, onder de open lucht (niet in een gebouw of onder de grond)."),
    "gui.guhs.evenement.no_route": ("No room for a parade here: find a nice open field.", "Geen plek voor een parade hier: zoek een mooi open veld."),
    "gui.guhs.evenement.command.started": ("Started: %s (%s)", "Gestart: %s (%s)"),
    "gui.guhs.evenement.command.none": ("You're not in an event.", "Je doet niet mee aan een evenement."),
    "gui.guhs.evenement.command.stopped": ("Stopped: %s", "Gestopt: %s"),
    "gui.guhs.evenement.command.when": ("Your next guh event: in about %s Minecraft days (%s of Guhmension time).",
                                        "Je volgende guh-evenement: over ongeveer %s Minecraft-dagen (%s Guhmensie-tijd)."),
    "gui.guhs.evenement.command.when_unknown": ("You haven't been in the Guhmension yet: your event clock hasn't started.",
                                                "Je bent nog niet in de Guhmensie geweest: je evenementenklok loopt nog niet."),
}


def advancements(h):
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
def build(h):
    items(h)
    for key, (en, nl) in TEXTS.items():
        h.lang(key, en, nl)
    advancements(h)
    self_check(h)


def self_check(h):
    """Every item of the feature has a model, a texture and a name; every entity a name (check_assets.py only knows ModEntities)."""
    missing = []
    for name in ITEMS + CLOTHES:
        if f"item.guhs.{name}" not in h.EN or f"item.guhs.{name}" not in h.NL:
            missing.append(f"name {name}")
    for name in ITEMS:
        if not os.path.exists(f"{h.A}/models/item/{name}.json"):
            missing.append(f"model {name}")
        if not os.path.exists(os.path.join(h.TEX, "item", f"{name}.png")):
            missing.append(f"texture {name}")
    for name in ENTITIES:
        if f"entity.guhs.{name}" not in h.EN:
            missing.append(f"entity name {name}")
    # the outfit bones may only use swatches that exist (make_guh_variants.BONES), and the paint must match them
    import make_guh_variants as v
    swatches = {b[2] for b in v.BONES.values()}
    for bone, (_, _, swatch, _) in BONES.items():
        if swatch not in swatches:
            missing.append(f"swatch {swatch} of {bone}")
    if missing:
        raise SystemExit("evenementen: missing " + ", ".join(missing))


# =====================================================================================================================
def ftb(fq):
    y = 48
    fq.q("evenement_eerste", "Er is iets aan de hand!", "Soms gebeurt er iets in de Guhmensie, gewoon buiten waar jij bent (ongeveer eens per 2 a 3 dagen): "
         "een &6kaasregen&r, de &dVadsparade&r of 's nachts een &bsterrenregen&r. Je ziet het in de chat en aan de balk bovenin.",
         "minecraft:bell", [fq.adv("evenement_eerste")], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, xp=50)
    fq.q("evenement_kaasregen", "Kaas uit de lucht", "Vang 20 kaasknabbels in een kaasregen. Loop er gewoon tegenaan, maar wees sneller dan de wilde guhs!",
         "guhs:kaas_knabbels", [fq.adv("evenement_kaasregen")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-6, y=y, xp=100)
    fq.q("evenement_goud", "Goud uit de lucht", "Vang een &6gouden kaasknabbel&r in een kaasregen. Geef hem aan een wilde guh: die is meteen tam!",
         "guhs:gouden_kaasknabbel", [fq.adv("evenement_gouden_knabbel")], rewards=(("guhs:kaas_knabbels", 16),), x=-4, y=y, xp=150)
    fq.q("evenement_parade", "Loop mee!", "Loop met de &dVadsparade&r mee, achter de Tamboerguh aan, helemaal tot het eind. Confetti!",
         "guhs:vadsparade_sjako", [fq.adv("evenement_parade")], rewards=(("guhs:kaas_knabbels", 16),), x=-2, y=y, xp=150)
    fq.q("evenement_paradepakje", "Tamboer-majoor", "Verzamel het hele paradepakje voor je guh: de paradesjako, het paradejasje en het paradetrommeltje. "
         "Je krijgt er een per parade, en alleen bij de parade!",
         "guhs:vadsparade_trommeltje", [fq.adv("evenement_paradepakje")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=0, y=y,
         shape="gear", xp=300)
    fq.q("evenement_sterrenguh", "Een wens", "Tem een sterrenguh die met een vallende ster is geland. Snel zijn: na een minuut gaat hij terug naar de sterren!",
         "guhs:sterrenstof", [fq.adv("evenement_sterrenguh")], rewards=(("guhs:guh_kristal", 4),), x=2, y=y, shape="gear", xp=300)
    fq.q("evenement_alle", "Feestneus", "Maak alle drie de evenementen mee: de kaasregen, de Vadsparade en de sterrenregen.",
         "minecraft:firework_rocket", [fq.adv("evenement_alle")], rewards=(("guhs:gouden_kaasknabbel", 1),), x=4, y=y, xp=200)
