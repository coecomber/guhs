"""
Het Guheinde (2.6): the endgame, a parody of the End and the Ender Dragon (Java: nl.juiced.guhs.feature.guheinde).

Opper-Mika steals all the kaasknabbels of the guh kingdom (and without knabbels a guh is never vahoeg). Mika's cry
Mika-tranen; guhkristal + kaasknabbel + Mika-traan = an Oog van Vadsig, which flies to the nearest Knabbelkelder (four
in a ring, underground in the Guhmension). Twelve eyes in the portal room open the way to the Guheinde: an island of
kaaskorst with the Knabbelberg, kaaspilaren with knabbelkristallen, and Opper-Mika on his starved Enderguh.

build(h) makes: the dimension (an End-like island, kaaskorst instead of end stone), blocks, items, entity textures,
texts, recipes, loot, advancements; guheinde_bouw.build(h) makes the structures. variants() adds the Guhdex guhs
(the magere guh and the Vahoege Enderguh), ftb() the quests.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

from features import guheinde_bouw

CHEESE_DARK, CHEESE_LIGHT = (196, 132, 40), (255, 226, 132)
MIKA_DARK, MIKA_LIGHT = (52, 24, 58), (150, 82, 150)
PINK = (255, 140, 190)
GOLD = (250, 200, 60)

# every text is Dutch (en_us gets the Dutch too): key -> text
TEXTS = {
    # --- blocks ---
    "block.guhs.kaaskorst": "Kaaskorst",
    "block.guhs.kaaskorst_stenen": "Kaaskorststenen",
    "block.guhs.gebarsten_kaaskorst_stenen": "Gebarsten kaaskorststenen",
    "block.guhs.kaaskorst_stenen_trap": "Kaaskorststenen trap",
    "block.guhs.kaaskorst_stenen_plaat": "Kaaskorststenen plaat",
    "block.guhs.kaaskorst_stenen_muur": "Kaaskorststenen muur",
    "block.guhs.aangevreten_kaaskorst_stenen": "Aangevreten kaaskorststenen",
    "block.guhs.mika_steen": "Mika-steen",
    "block.guhs.mika_steen_pilaar": "Mika-steenpilaar",
    "block.guhs.mika_steen_trap": "Mika-steentrap",
    "block.guhs.mika_steen_plaat": "Mika-steenplaat",
    "block.guhs.knabbelportaalframe": "Knabbelportaalframe",
    "block.guhs.guheinde_portaal": "Guheindeportaal",
    "block.guhs.knabbelsokkel": "Knabbelsokkel",
    "block.guhs.knabbelslot": "Knabbelslot",
    "block.guhs.knabbelpoort": "Knabbelpoort",
    "block.guhs.enderguh_ei": "Enderguh-ei",
    "block.guhs.opper_mikatrofee": "Opper-Mikatrofee",
    # --- items ---
    "item.guhs.mika_traan": "Mika-traan",
    "item.guhs.oog_van_vadsig": "Oog van Vadsig",
    "item.guhs.oog_van_vadsig.lore": "Gooi hem in de Guhmensie: hij vliegt naar de dichtstbijzijnde Knabbelkelder",
    "item.guhs.knabbelkristal": "Knabbelkristal",
    "item.guhs.knabbelkristal.lore": "Vol gestolen kaasknabbels. Vier op de knabbelsokkels roepen Opper-Mika terug",
    "item.guhs.knabbelkroon": "Knabbelkroon",
    "item.guhs.knabbelkroon.lore": "De kroon van Opper-Mika. Nu van jou. VAHOEG!",
    "item.guhs.knabbelkroon.lore2": "Vahoeg-aura: nooit echt honger, blije guhs, harde meppen tegen Mika's",
    "item.guhs.guhvleugels": "Guhvleugels",
    "item.guhs.guhvleugels.lore": "Kleine Enderguhvleugeltjes om mee te zweven. Repareren met Mika's vet",
    "item.guhs.mika_larfje_spawn_egg": "Mika-larfje-spawnei",
    # --- entities ---
    "entity.guhs.opper_mika": "Opper-Mika",
    "entity.guhs.hongerige_enderguh": "Hongerige Enderguh",
    "entity.guhs.knabbelkristal": "Knabbelkristal",
    "entity.guhs.mika_larfje": "Mika-larfje",
    "entity.guhs.oog_van_vadsig": "Oog van Vadsig",
    "entity.guhs.mika_vetbal": "Mika-vetbal",
    "entity.guhs.guh.mager": "Magere guh",
    "entity.guhs.guh.vahoege_ender": "Vahoege Enderguh",
    "gui.guhs.guhdex.rarity.mager": "Zeldzaamheid: Zielig (in de cellen van de Knabbelkelder)",
    "gui.guhs.guhdex.info.mager": "Een grauwe, magere guh die de Mika's al maanden geen knabbel gaven. Geef hem er een en hij wordt meteen weer VAHOEG! (Je ster krijg je voor het redden.)",
    "gui.guhs.guhdex.rarity.vahoege_ender": "Zeldzaamheid: Legendarisch (het Guheinde)",
    "gui.guhs.guhdex.info.vahoege_ender": "De bevrijde Enderguh van Opper-Mika: goud en roze, met drakenvleugels. Je krijgt hem na je eerste overwinning, en daarna Enderguh-eieren. Zadel hem en vlieg!",
    # --- the world ---
    "biome.guhs.guheinde": "Het Guheinde",
    "structure.guhs.knabbelkelder": "Knabbelkelder",
    "structure.guhs.knabbelkelder.tooltip": "Ondergronds: cellen met magere guhs, Mika-larfjes en het portaal naar het Guheinde",
    "structure.guhs.mika_vesting": "Mika-vesting",
    "structure.guhs.guheinde_terugpoort": "Terugpoort",
    "structure.guhs.guheinde_terugpoort.tooltip": "Een klein Knabbelpoort-altaartje op de buiteneilanden: stap erin en je zweeft vahoeg terug naar het grote eiland",
    "gui.guhs.guheinde.terugpoort": "Woesj! De Terugpoort zweeft je vahoeg terug naar het grote eiland. Tuut tuut!",
    "gui.guhs.superkompas.einde": "Einde",
    "gui.guhs.superkompas.einde.tooltip": "Op weg naar het Guheinde. Njeg!",
    # --- messages ---
    "gui.guhs.guheinde.oog_alleen_guhmensie": "Njeg... het Oog van Vadsig knippert alleen in de Guhmensie",
    "gui.guhs.guheinde.oog_niks": "Het oog kijkt verward rond. Hier is geen Knabbelkelder te vinden",
    "gui.guhs.guheinde.oog_kapot": "NJEG! Het Oog van Vadsig is kapot. Er zit Mika-traan in, dat verklaart veel",
    "gui.guhs.guheinde.portaal_open": "Twaalf ogen van Vadsig kijken je aan... Het portaal naar het Guheinde gaat open. VAHOEG!",
    "gui.guhs.guheinde.titel": "Het Guheinde",
    "gui.guhs.guheinde.subtitel": "Waar alle knabbels naartoe gingen. Njeg.",
    "gui.guhs.guheinde.aankomst": "Hoor je dat? NJEG NJEG NJEG! Opper-Mika komt eraan op zijn arme, uitgehongerde Enderguh!",
    "gui.guhs.guheinde.kristal_kapot": "Een knabbelkristal is kapot: de knabbels regenen terug! Nog %s te gaan.",
    "gui.guhs.guheinde.uitgeput": "Alle knabbelkristallen zijn kapot! De Enderguh is uitgeput en landt op de Knabbelberg... Geef hem een kaasknabbel!",
    "gui.guhs.guheinde.gevoerd": "VAHOEG!!! De Enderguh heeft eindelijk een knabbel. Hij gooit Opper-Mika eraf!",
    "gui.guhs.guheinde.schat_open": "Klik-klak... Opper-Mika's knabbelschat onder de Knabbelberg is open!",
    "gui.guhs.guheinde.oproep": "De knabbelkristallen gloeien... Opper-Mika voelt dat er knabbels zijn. Hij komt terug!",
    "gui.guhs.guheinde.bewaard": "De Mika's hebben alleen je kaasknabbels ingepikt. De rest heb je nog. Njeg!",
    "gui.guhs.guheinde.beloning.eerste": "Voor jou: de Knabbelkroon, de Opper-Mikatrofee en een eigen Vahoege Enderguh! VAHOEG!",
    "gui.guhs.guheinde.beloning.ei": "Opper-Mika laat een Enderguh-ei achter. Zet het neer en wacht tot het uitkomt!",
    "gui.guhs.guheinde.scorebord": "Guheinde: snelste overwinning",
    "gui.guhs.guheinde.scorebord.snelst": "Minuten:seconden",
    "gui.guhs.guheinde.mika_huilt": "De Mika-baas huilt twee dikke Mika-tranen. Njeg...",
    "gui.guhs.guheinde.larfje.roof": "Een Mika-larfje pikt een kaasknabbel in! Njeg!",
    "gui.guhs.guheinde.mager.honger": "De magere guh kijkt je hongerig aan. Heb je een kaasknabbel?",
    "gui.guhs.guheinde.mager.vahoeg": "G... guh? GUH! VAHOEG!!! Bedankt, lieve held! Ik ren naar huis!",
    "gui.guhs.guheinde.enderguh.niet_meppen": "Niet de guh meppen! Hij kan er niks aan doen. Mep Opper-Mika!",
    "gui.guhs.guheinde.enderguh.bang": "De Enderguh is te bang voor Opper-Mika. Sla eerst de knabbelkristallen stuk!",
    "gui.guhs.guheinde.enderguh.honger": "Guhhh... honger... knabbel...?",
    "gui.guhs.guheinde.enderguh.blij": "De Enderguh zwaait blij met zijn vleugels. VAHOEG!",
    "gui.guhs.guheinde.ei.0": "Het Enderguh-ei wiebelt een beetje",
    "gui.guhs.guheinde.ei.1": "Er zit een barstje in het Enderguh-ei! Je hoort zacht 'guh'",
    "gui.guhs.guheinde.ei.2": "Het Enderguh-ei kraakt! Nog heel even...",
    "gui.guhs.guheinde.opper.vloer": "NJEG HEHE! Zolang ik op mijn Enderguh zit, krijg je mij niet klein!",
    "gui.guhs.guheinde.opper.hulpjes": "MIKA'S! Hierheen! Pak die knabbels af!",
    "gui.guhs.guheinde.opper.lui": "Vlieg dan, lui vadsig beest! NJEG!",
    "gui.guhs.guheinde.opper.eraf": "NJEEEEG! Mijn Enderguh! Wie heeft hem een knabbel gegeven?!",
    "gui.guhs.guheinde.opper.roof": "Opper-Mika pikt %s kaasknabbels van je in! Njeg hehe!",
    "gui.guhs.guheinde.opper.gevadst": "NJEG... IK BEN... OPPER-GEVADST!",
    # --- the Koningguh ---
    "gui.guhs.guheinde.koning.verhaal1": "Guh... Welkom, held. Ik moet je iets vertellen. De kaasknabbels in mijn rijk raken op. Overal. Mijn guhs worden grauw en mager.",
    "gui.guhs.guheinde.koning.verhaal2": "Het is Opper-Mika, de baas van alle Mika's. Hij pikt ze in en brengt ze naar het Guheinde, een eiland ver weg. Zonder knabbels is geen guh vahoeg!",
    "gui.guhs.guheinde.koning.verhaal3": "Maak een Oog van Vadsig: een guhkristal, een kaasknabbel en een Mika-traan. Mika's huilen soms, vooral Grote Mika en de Mika-baas. Gooi het oog in de Guhmensie: het vliegt naar een Knabbelkelder.",
    "gui.guhs.guheinde.koning.verhaal4": "In de kelder is een portaal. Vul het met twaalf ogen en breng de knabbels terug. Hier, lees dit boek. Vads op, held!",
    "gui.guhs.guheinde.koning.hint_traan": "Nog geen Oog van Vadsig? Laat Mika's huilen: Grote Mika in de uitdagende guhgrotten huilt het hardst. Of versla de Mika-baas drie keer met VADS.",
    "gui.guhs.guheinde.koning.hint_oog": "Gooi je Oog van Vadsig in de Guhmensie en volg het. Waar het naar beneden duikt, graaf je naar de Knabbelkelder!",
    "gui.guhs.guheinde.koning.ridder1": "Je hebt het gedaan! Opper-Mika is gevadst en de knabbels komen terug. Kniel, held...",
    "gui.guhs.guheinde.koning.ridder2": "*tik tik met een kaasknabbel* Ik sla je tot Ridder van het Guheinde! VAHOEG!",
    "gui.guhs.guheinde.koning.dank": "Ridder! Dankzij jou zijn mijn guhs weer vahoeg. Kom je nog eens een knabbel eten?",
}

# (the lore book, Guhboek GUHEINDE, is with the other books in bibliotheek.py)

BLOCKS_SIMPLE = ["kaaskorst", "kaaskorst_stenen", "gebarsten_kaaskorst_stenen", "mika_steen", "knabbelslot"]


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def lang(h, key, text):
    h.lang(key, text, text)


# =====================================================================================================================
# textures
# =====================================================================================================================
def noise(img, seed, amount=10):
    rng = np.random.default_rng(seed)
    a = np.asarray(img).astype(np.int32)
    a[..., :3] += rng.integers(-amount, amount + 1, a[..., :3].shape)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def bites(img, seed, n=3):
    """Round bite marks along the edges (a Mika-larfje has been at it)."""
    img = img.copy()
    px = img.load()
    rng = random.Random(seed)
    for _ in range(n):
        cx, cy = rng.choice([(rng.randint(2, 13), 0), (rng.randint(2, 13), 15), (0, rng.randint(2, 13)), (15, rng.randint(2, 13))])
        r = rng.uniform(2.0, 3.2)
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    px[x, y] = (110, 62, 24, 255) if d > r - 1 else (70, 38, 16, 255)
    return img


def block_textures(h):
    ramp = h.ramp
    save = h.save
    save(noise(ramp(h.vanilla("block/end_stone"), CHEESE_DARK, CHEESE_LIGHT), 1, 6), "block", "kaaskorst.png")
    stenen = ramp(h.vanilla("block/end_stone_bricks"), CHEESE_DARK, CHEESE_LIGHT)
    save(stenen, "block", "kaaskorst_stenen.png")
    save(ramp(h.vanilla("block/cracked_stone_bricks"), (170, 110, 30), CHEESE_LIGHT), "block", "gebarsten_kaaskorst_stenen.png")
    save(bites(stenen, 7), "block", "aangevreten_kaaskorst_stenen.png")
    # Mika-steen: dark purple purpur with little red Mika eyes here and there
    mika = ramp(h.vanilla("block/purpur_block"), MIKA_DARK, MIKA_LIGHT).copy()
    px = mika.load()
    for (x, y) in ((4, 4), (6, 4), (11, 11), (13, 11)):
        px[x, y] = (220, 40, 60, 255)
    save(mika, "block", "mika_steen.png")
    save(ramp(h.vanilla("block/purpur_pillar"), MIKA_DARK, MIKA_LIGHT), "block", "mika_steen_pilaar.png")
    save(ramp(h.vanilla("block/purpur_pillar_top"), MIKA_DARK, MIKA_LIGHT), "block", "mika_steen_pilaar_top.png")
    # the portal frame: cheese crust with a pink inlay, and the eye
    save(ramp(h.vanilla("block/end_portal_frame_top"), (150, 96, 30), CHEESE_LIGHT), "block", "knabbelportaalframe_top.png")
    save(ramp(h.vanilla("block/end_portal_frame_side"), (150, 96, 30), CHEESE_LIGHT), "block", "knabbelportaalframe_side.png")
    eye = h.vanilla("block/end_portal_frame_eye")
    save(h.recolour(eye, hue=0.93, sat=1.0, val=1.1), "block", "knabbelportaalframe_oog.png")
    # the knabbelsokkel (gold with a pink knabbel top), the knabbelslot (a vault door of cheese)
    sok = ramp(h.vanilla("block/gold_block"), (170, 110, 20), (255, 236, 150))
    save(sok, "block", "knabbelsokkel_side.png")
    top = sok.copy()
    d = ImageDraw.Draw(top)
    d.ellipse((3, 3, 12, 12), fill=(240, 120, 170, 255), outline=(180, 70, 120, 255))
    save(top, "block", "knabbelsokkel_top.png")
    slot = ramp(h.vanilla("block/iron_block"), (170, 90, 20), (250, 190, 70)).copy()
    d = ImageDraw.Draw(slot)
    d.rectangle((6, 5, 9, 10), fill=(60, 30, 10, 255))
    d.rectangle((7, 10, 8, 12), fill=(60, 30, 10, 255))
    d.ellipse((5, 3, 10, 8), outline=(60, 30, 10, 255))
    save(slot, "block", "knabbelslot.png")
    # the egg: black-purple with gold and pink speckles; three stages of cracks
    rng = random.Random(3)
    egg = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = rng.random()
            c = (250, 200, 70) if r < 0.05 else (255, 140, 200) if r < 0.1 else (40 + rng.randint(-6, 6), 22, 52 + rng.randint(-6, 6))
            egg.putpixel((x, y), c + (255,))
    for stage in range(3):
        img = egg.copy()
        d = ImageDraw.Draw(img)
        if stage >= 1:
            d.line((3, 6, 6, 8, 5, 11, 8, 13), fill=(255, 230, 150, 255))
        if stage >= 2:
            d.line((9, 2, 11, 5, 10, 8, 13, 10), fill=(255, 230, 150, 255))
            d.line((6, 8, 9, 7), fill=(255, 230, 150, 255))
        save(img, "block", f"enderguh_ei_{stage}.png")
    # the trophy: a golden Mika head on a plinth
    for part in ("top", "front", "side"):
        src = os.path.join(h.TEX, "block", f"mikatrofee_{part}.png")
        save(h.recolour(Image.open(src), hue=0.12, sat=1.4, val=1.1, only=h.pinkish), "block", f"opper_mikatrofee_{part}.png")
    # the portals: animated pink swirls (the portal) and pink stars (the Knabbelpoort)
    frames = 16
    for name, base, seed in (("guheinde_portaal", (60, 20, 60), 11), ("knabbelpoort", (90, 30, 80), 12)):
        strip = Image.new("RGBA", (16, 16 * frames))
        r = random.Random(seed)
        dots = [(r.uniform(0, 16), r.uniform(0, 16), r.random()) for _ in range(26)]
        for f in range(frames):
            for y in range(16):
                for x in range(16):
                    t = f / frames * 2 * math.pi
                    v = 0.5 + 0.5 * math.sin(x * 0.7 + y * 0.4 + t) * math.cos(y * 0.6 - x * 0.3 + t)
                    c = tuple(int(base[i] + (PINK[i] - base[i]) * v * 0.6) for i in range(3))
                    strip.putpixel((x, f * 16 + y), c + (230 if name == "knabbelpoort" else 255,))
            for (dx, dy, ph) in dots:
                if (math.sin(ph * 6.28 + f / frames * 6.28) > 0.3):
                    x, y = int(dx + f * 0.3) % 16, int(dy + f * 0.6) % 16
                    strip.putpixel((x, f * 16 + y), (255, 230, 150, 255) if ph > 0.5 else (255, 200, 230, 255))
        save(strip, "block", f"{name}.png")
        h.w(os.path.join(h.TEX, "block", f"{name}.png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})


def item_textures(h):
    save = h.save
    # the eye: an ender eye with a cheese-yellow iris and a pink shine
    eye = h.vanilla("item/ender_eye")
    save(h.recolour(eye, hue=0.12, sat=1.2, val=1.15, only=lambda hh, s, v: s > 0.15), "item", "oog_van_vadsig.png")
    save(h.recolour(h.vanilla("item/ghast_tear"), hue=0.8, sat=0.6, val=0.85), "item", "mika_traan.png")
    save(h.recolour(h.vanilla("item/end_crystal"), hue=0.12, sat=1.3, val=1.1, only=lambda hh, s, v: s > 0.1), "item", "knabbelkristal.png")
    wings = h.colorize(h.vanilla("item/elytra"), (190, 110, 210))
    save(wings, "item", "guhvleugels.png")
    # the crown's texture (for its 3D item model): gold with a pink knabbel gem
    tex = Image.new("RGBA", (16, 16))
    rng = random.Random(9)
    for y in range(16):
        for x in range(16):
            v = rng.randint(-14, 14)
            tex.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in GOLD) + (255,))
    d = ImageDraw.Draw(tex)
    d.rectangle((12, 12, 15, 15), fill=(255, 120, 190, 255))
    d.point([(13, 13)], fill=(255, 220, 240, 255))
    d.rectangle((12, 0, 15, 3), fill=(250, 170, 40, 255))
    save(tex, "item", "knabbelkroon.png")


def entity_textures(h):
    save = h.save
    TEX = h.TEX
    mika = Image.open(os.path.join(TEX, "entity", "mika.png")).convert("RGBA")
    save(h.recolour(mika, hue=0.8, sat=1.5, val=0.62, only=h.pinkish), "entity", "opper_mika.png")
    crystal = h.vanilla("entity/end_crystal/end_crystal")
    save(h.recolour(crystal, hue=0.12, sat=1.4, val=1.1, only=lambda hh, s, v: s > 0.08), "entity", "knabbelkristal.png")
    elytra = h.vanilla("entity/elytra")
    a = np.asarray(elytra).astype(np.float32)
    lum = (a[..., 0] * .3 + a[..., 1] * .59 + a[..., 2] * .11)[..., None] / 255
    purple, pink = np.array((70, 30, 100), np.float32), np.array((255, 150, 210), np.float32)
    out = purple * (1 - lum) + pink * lum
    save(Image.fromarray(np.concatenate([out, a[..., 3:]], -1).astype(np.uint8)), "entity", "guhvleugels.png")
    # the starved Enderguh: from grey and dull (0) back to all the Vahoege Enderguh's colours (8)
    ender = np.asarray(Image.open(os.path.join(TEX, "entity", "guh_ender.png")).convert("RGBA")).astype(np.float32)
    vahoeg = np.asarray(Image.open(os.path.join(TEX, "entity", "guh_vahoege_ender.png")).convert("RGBA")).astype(np.float32)
    grey = ender[..., :3].mean(-1, keepdims=True) * 0.55 + 95
    starved = np.concatenate([np.repeat(grey, 3, -1), ender[..., 3:]], -1)
    for i in range(9):
        t = i / 8
        mix = starved * (1 - t) + vahoeg * t
        mix[..., 3] = np.maximum(starved[..., 3], vahoeg[..., 3])
        save(Image.fromarray(np.clip(mix, 0, 255).astype(np.uint8)), "entity", f"guh_hongerig_{i}.png")
    # the Vahoege Enderguh glows where its wings are pink
    v = vahoeg.astype(np.int32)
    wing = (np.abs(v[..., 0] - WING[0]) + np.abs(v[..., 1] - WING[1]) + np.abs(v[..., 2] - WING[2]) < 90) & (v[..., 3] > 0)
    glow = np.zeros_like(v)
    glow[wing] = v[wing]
    save(Image.fromarray(glow.astype(np.uint8)), "entity", "guh_vahoege_ender_glowmask.png")
    # the crown on a player's head (armour layer 1: only the head, a gold band with points)
    armor = Image.new("RGBA", (64, 32))
    px = armor.load()
    for fx0 in (0, 8, 16, 24):            # the four sides of the head (right, front, left, back), rows 8..15
        for x in range(fx0, fx0 + 8):
            for y in (8, 9, 10):
                px[x, y] = GOLD + (255,)
    for fx0 in (0, 8, 16, 24):
        for x in range(fx0, fx0 + 8, 3):
            px[x, 8] = (255, 236, 150, 255)
    px[11, 9] = px[12, 9] = (255, 110, 180, 255)         # the pink gem at the front
    for x in range(8, 16):                                # the rim on top of the head
        px[x, 0] = px[x, 7] = GOLD + (255,)
    for y in range(8):
        px[8, y] = px[15, y] = GOLD + (255,)
    # 26.1 equipment asset guhs:knabbelkroon (tools/mc26.py writes assets/guhs/equipment/knabbelkroon.json)
    for layer in ("humanoid", "humanoid_leggings"):
        os.makedirs(os.path.join(TEX, "entity", "equipment", layer), exist_ok=True)
        armor.save(os.path.join(TEX, "entity", "equipment", layer, "knabbelkroon.png"))
    # the sky: slow swirls of knabbel crumbs
    sky = h.vanilla("environment/end_sky")
    a = np.asarray(sky).astype(np.float32)
    lum = a[..., :3].mean(-1, keepdims=True) / 255
    base, crumb = np.array((120, 70, 120), np.float32), np.array((255, 214, 120), np.float32)
    out = base * (1 - lum) + crumb * lum
    save(Image.fromarray(np.concatenate([out, np.full_like(lum, 255)], -1).astype(np.uint8)), "environment", "guheinde_sky.png")


WING = (255, 120, 200)


# =====================================================================================================================
# blocks, items, models
# =====================================================================================================================
def blocks_and_items(h):
    A, D = h.A, h.D
    w = h.w
    for name in BLOCKS_SIMPLE:
        h.simple_block(name)
    for name in ("kaaskorst", "kaaskorst_stenen", "gebarsten_kaaskorst_stenen", "mika_steen", "mika_steen_pilaar", "mika_steen_trap",
                 "kaaskorst_stenen_trap", "kaaskorst_stenen_muur", "enderguh_ei", "opper_mikatrofee"):
        h.self_drop(name)
    # the bitten one: exactly like kaaskorststenen (you can't tell!), only with silk touch you get it
    h.simple_block("aangevreten_kaaskorst_stenen", tex="guhs:block/kaaskorst_stenen")
    w(f"{D}/loot_table/blocks/aangevreten_kaaskorst_stenen.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:aangevreten_kaaskorst_stenen"}],
        "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}]})
    # stairs, slabs, walls, the pillar
    for stair, tex in (("kaaskorst_stenen_trap", "guhs:block/kaaskorst_stenen"), ("mika_steen_trap", "guhs:block/mika_steen")):
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            w(f"{A}/models/block/{stair}{suffix}.json", {"parent": f"minecraft:block/{parent}",
                                                          "textures": {"bottom": tex, "top": tex, "side": tex}})
        w(f"{A}/blockstates/{stair}.json", {"variants": stair_states(f"guhs:block/{stair}")})
        w(f"{A}/models/item/{stair}.json", {"parent": f"guhs:block/{stair}"})
    for slab, tex, full in (("kaaskorst_stenen_plaat", "guhs:block/kaaskorst_stenen", "guhs:block/kaaskorst_stenen"),
                            ("mika_steen_plaat", "guhs:block/mika_steen", "guhs:block/mika_steen")):
        w(f"{A}/models/block/{slab}.json", {"parent": "minecraft:block/slab", "textures": {"bottom": tex, "top": tex, "side": tex}})
        w(f"{A}/models/block/{slab}_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": tex, "top": tex, "side": tex}})
        w(f"{A}/blockstates/{slab}.json", {"variants": {"type=bottom": {"model": f"guhs:block/{slab}"},
                                                       "type=top": {"model": f"guhs:block/{slab}_top"},
                                                       "type=double": {"model": full}}})
        w(f"{A}/models/item/{slab}.json", {"parent": f"guhs:block/{slab}"})
        w(f"{D}/loot_table/blocks/{slab}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": f"guhs:{slab}", "functions": [{"function": "minecraft:set_count", "count": 2, "add": False,
                                                                           "conditions": [{"condition": "minecraft:block_state_property", "block": f"guhs:{slab}",
                                                                                           "properties": {"type": "double"}}]},
                                                                          {"function": "minecraft:explosion_decay"}]}]}]})
    wall, tex = "kaaskorst_stenen_muur", "guhs:block/kaaskorst_stenen"
    for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"), ("_side_tall", "template_wall_side_tall"),
                           ("_inventory", "wall_inventory")):
        w(f"{A}/models/block/{wall}{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"wall": tex}})
    parts = [{"when": {"up": "true"}, "apply": {"model": f"guhs:block/{wall}_post"}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for height, suffix in (("low", "_side"), ("tall", "_side_tall")):
            apply = {"model": f"guhs:block/{wall}{suffix}", "uvlock": True}
            if y:
                apply["y"] = y
            parts.append({"when": {side: height}, "apply": apply})
    w(f"{A}/blockstates/{wall}.json", {"multipart": parts})
    w(f"{A}/models/item/{wall}.json", {"parent": f"guhs:block/{wall}_inventory"})
    w(f"{A}/models/block/mika_steen_pilaar.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "guhs:block/mika_steen_pilaar_top", "side": "guhs:block/mika_steen_pilaar"}})
    w(f"{A}/models/block/mika_steen_pilaar_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
        "end": "guhs:block/mika_steen_pilaar_top", "side": "guhs:block/mika_steen_pilaar"}})
    w(f"{A}/blockstates/mika_steen_pilaar.json", {"variants": {
        "axis=y": {"model": "guhs:block/mika_steen_pilaar"},
        "axis=x": {"model": "guhs:block/mika_steen_pilaar_horizontal", "x": 90, "y": 90},
        "axis=z": {"model": "guhs:block/mika_steen_pilaar_horizontal", "x": 90}}})
    w(f"{A}/models/item/mika_steen_pilaar.json", {"parent": "guhs:block/mika_steen_pilaar"})
    # the portal frame (the end portal frame's shape; the eye gets the Oog van Vadsig)
    frame_tex = {"particle": "guhs:block/knabbelportaalframe_side", "bottom": "guhs:block/kaaskorst",
                 "top": "guhs:block/knabbelportaalframe_top", "side": "guhs:block/knabbelportaalframe_side"}
    w(f"{A}/models/block/knabbelportaalframe.json", {"parent": "minecraft:block/end_portal_frame", "textures": frame_tex})
    w(f"{A}/models/block/knabbelportaalframe_oog.json", {"parent": "minecraft:block/end_portal_frame_filled",
                                                         "textures": {**frame_tex, "eye": "guhs:block/knabbelportaalframe_oog"}})
    variants = {}
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for oog in ("false", "true"):
            v = {"model": "guhs:block/knabbelportaalframe" + ("_oog" if oog == "true" else "")}
            if y:
                v["y"] = y
            variants[f"facing={facing},oog={oog}"] = v
    w(f"{A}/blockstates/knabbelportaalframe.json", {"variants": variants})
    w(f"{A}/models/item/knabbelportaalframe.json", {"parent": "guhs:block/knabbelportaalframe"})
    # the portal (a flat layer in the middle of the block, like the end portal) and the Knabbelpoort (a glowing cube)
    w(f"{A}/models/block/guheinde_portaal.json", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": {
        "particle": "guhs:block/guheinde_portaal", "portal": "guhs:block/guheinde_portaal"}, "elements": [
        {"from": [0, 11.5, 0], "to": [16, 11.5, 16], "shade": False, "faces": {"up": {"texture": "#portal"}, "down": {"texture": "#portal"}}}]})
    w(f"{A}/blockstates/guheinde_portaal.json", {"variants": {"": {"model": "guhs:block/guheinde_portaal"}}})
    h.simple_block("knabbelpoort", render_type="minecraft:translucent")
    w(f"{A}/models/block/knabbelsokkel.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/knabbelsokkel_top", "bottom": "guhs:block/knabbelsokkel_side", "side": "guhs:block/knabbelsokkel_side"}})
    w(f"{A}/blockstates/knabbelsokkel.json", {"variants": {"": {"model": "guhs:block/knabbelsokkel"}}})
    w(f"{A}/models/item/knabbelsokkel.json", {"parent": "guhs:block/knabbelsokkel"})
    # the egg: the dragon egg's shape
    for stage in range(3):
        w(f"{A}/models/block/enderguh_ei_{stage}.json", {"parent": "minecraft:block/dragon_egg", "textures": {
            "particle": f"guhs:block/enderguh_ei_{stage}", "all": f"guhs:block/enderguh_ei_{stage}"}})
    w(f"{A}/blockstates/enderguh_ei.json", {"variants": {f"hatch={s}": {"model": f"guhs:block/enderguh_ei_{s}"} for s in range(3)}})
    w(f"{A}/models/item/enderguh_ei.json", {"parent": "guhs:block/enderguh_ei_0"})
    # the trophy: like the Mika trophy
    w(f"{A}/models/block/opper_mikatrofee.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": "guhs:block/opper_mikatrofee_top", "front": "guhs:block/opper_mikatrofee_front", "side": "guhs:block/opper_mikatrofee_side"}})
    w(f"{A}/blockstates/opper_mikatrofee.json", {"variants": h.facing_states("opper_mikatrofee")})
    w(f"{A}/models/item/opper_mikatrofee.json", {"parent": "guhs:block/opper_mikatrofee"})

    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in (
        "kaaskorst", "kaaskorst_stenen", "gebarsten_kaaskorst_stenen", "kaaskorst_stenen_trap", "kaaskorst_stenen_plaat", "kaaskorst_stenen_muur",
        "aangevreten_kaaskorst_stenen", "mika_steen", "mika_steen_pilaar", "mika_steen_trap", "mika_steen_plaat", "opper_mikatrofee")])
    h.add_tag("minecraft/tags/block/walls", ["guhs:kaaskorst_stenen_muur"])
    h.add_tag("minecraft/tags/item/walls", ["guhs:kaaskorst_stenen_muur"])
    h.add_tag("minecraft/tags/block/stairs", ["guhs:kaaskorst_stenen_trap", "guhs:mika_steen_trap"])
    h.add_tag("minecraft/tags/item/stairs", ["guhs:kaaskorst_stenen_trap", "guhs:mika_steen_trap"])
    h.add_tag("minecraft/tags/block/slabs", ["guhs:kaaskorst_stenen_plaat", "guhs:mika_steen_plaat"])
    h.add_tag("minecraft/tags/item/slabs", ["guhs:kaaskorst_stenen_plaat", "guhs:mika_steen_plaat"])
    h.add_tag("minecraft/tags/block/dragon_immune", ["guhs:knabbelportaalframe", "guhs:guheinde_portaal", "guhs:knabbelsokkel",
                                                     "guhs:knabbelslot", "guhs:knabbelpoort"])
    h.add_tag("minecraft/tags/block/wither_immune", ["guhs:knabbelportaalframe", "guhs:guheinde_portaal", "guhs:knabbelsokkel",
                                                     "guhs:knabbelslot", "guhs:knabbelpoort"])
    h.add_tag("minecraft/tags/item/head_armor", ["guhs:knabbelkroon"])

    # items
    for name in ("oog_van_vadsig", "mika_traan", "knabbelkristal", "guhvleugels"):
        h.item_model(name)
    w(f"{A}/models/item/mika_larfje_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    w(f"{A}/models/item/knabbelkroon.json", crown_model())


def stair_states(model):
    """The blockstate of a vanilla stair, pointing at model, model_inner, model_outer (copied from the end stone brick stairs)."""
    import zipfile
    jar = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
    text = jar.read("assets/minecraft/blockstates/end_stone_brick_stairs.json").decode()
    return json.loads(text.replace("minecraft:block/end_stone_brick_stairs", model))["variants"]


def crown_model():
    """The Knabbelkroon as a little 3D crown: a band, four points and a pink knabbel gem (in the hand and on Opper-Mika)."""
    t = "#crown"
    elements = [
        {"from": [4, 0, 4], "to": [12, 3, 5], "faces": all_faces(t)},
        {"from": [4, 0, 11], "to": [12, 3, 12], "faces": all_faces(t)},
        {"from": [4, 0, 5], "to": [5, 3, 11], "faces": all_faces(t)},
        {"from": [11, 0, 5], "to": [12, 3, 11], "faces": all_faces(t)},
    ]
    for (x, z) in ((4, 4), (11, 4), (4, 11), (11, 11), (7.5, 4), (7.5, 11), (4, 7.5), (11, 7.5)):
        elements.append({"from": [x, 3, z], "to": [x + 1, 5, z + 1], "faces": all_faces(t)})
    elements.append({"from": [7, 1, 3.5], "to": [9, 3, 4], "faces": all_faces("#crown", uv=[12, 12, 16, 16])})
    return {"textures": {"crown": "guhs:item/knabbelkroon", "particle": "guhs:item/knabbelkroon"}, "elements": elements,
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [1.3, 1.3, 1.3]},
                        "ground": {"translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
                        "fixed": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [1, 1, 1]},
                        "head": {"translation": [0, 14, 0], "scale": [1.6, 1.6, 1.6]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]}}}


def all_faces(tex, uv=None):
    return {f: {"texture": tex, **({"uv": uv} if uv else {"uv": [0, 0, 4, 4]})} for f in ("down", "up", "north", "south", "west", "east")}


# =====================================================================================================================
# the dimension
# =====================================================================================================================
def dimension(h):
    D = h.D
    w = h.w
    w(f"{D}/dimension_type/guheinde.json", {
        "ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": False,
        "has_raids": False, "has_skylight": False, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.0,
        "fixed_time": 6000, "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_end",
        "effects": "guhs:guheinde", "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7},
        "monster_spawn_block_light_limit": 0})
    # the End's islands (a big one in the middle, a ring of nothing, outer islands from ~1000 blocks), made of kaaskorst
    import zipfile
    jar = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
    end = json.loads(jar.read("data/minecraft/worldgen/noise_settings/end.json"))
    end["default_block"] = {"Name": "guhs:kaaskorst"}
    end["surface_rule"] = {"type": "minecraft:block", "result_state": {"Name": "guhs:kaaskorst"}}
    w(f"{D}/worldgen/noise_settings/guheinde.json", end)
    w(f"{D}/dimension/guheinde.json", {"type": "guhs:guheinde", "generator": {
        "type": "minecraft:noise", "settings": "guhs:guheinde", "biome_source": {"type": "minecraft:fixed", "biome": "guhs:guheinde"}}})
    w(f"{D}/worldgen/biome/guheinde.json", {
        "has_precipitation": False, "temperature": 0.5, "downfall": 0.5,
        "effects": {"sky_color": 0x2A1028, "fog_color": 0x3C1E34, "water_color": 0xF08CC0, "water_fog_color": 0x6A2A50,
                    "grass_color": 0xE8B83C, "foliage_color": 0xE8B83C,
                    "particle": {"options": {"type": "minecraft:dust", "color": [1.0, 0.82, 0.3], "scale": 1.0}, "probability": 0.002},
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}},
        "spawners": {"monster": [{"type": "guhs:mika", "weight": 4, "minCount": 1, "maxCount": 1}]},
        "spawn_costs": {}, "carvers": {}, "features": []})


# =====================================================================================================================
# recipes and loot
# =====================================================================================================================
def recipes(h):
    D = h.D
    h.shapeless("oog_van_vadsig", ["guhs:guh_kristal", "guhs:kaas_knabbels", "guhs:mika_traan"], "guhs:oog_van_vadsig")
    h.shaped("knabbelkristal", ["GGG", "GOG", "GTG"], {"G": "minecraft:glass", "O": "guhs:oog_van_vadsig", "T": "guhs:mika_traan"},
             "guhs:knabbelkristal")
    # 1.2.7: one pair of wings per vetschip isn't enough for a whole server: with kaaskorst from the Guheinde you can make them
    h.shaped("guhvleugels", ["MKM", "MTM", "C C"], {"M": "minecraft:phantom_membrane", "K": "guhs:knabbelkristal", "T": "guhs:mika_traan",
                                                    "C": "guhs:kaaskorst"}, "guhs:guhvleugels")
    h.shaped("kaaskorst_stenen", ["KK", "KK"], {"K": "guhs:kaaskorst"}, "guhs:kaaskorst_stenen", 4)
    h.shaped("kaaskorst_stenen_trap", ["K  ", "KK ", "KKK"], {"K": "guhs:kaaskorst_stenen"}, "guhs:kaaskorst_stenen_trap", 4)
    h.shaped("kaaskorst_stenen_plaat", ["KKK"], {"K": "guhs:kaaskorst_stenen"}, "guhs:kaaskorst_stenen_plaat", 6)
    h.shaped("kaaskorst_stenen_muur", ["KKK", "KKK"], {"K": "guhs:kaaskorst_stenen"}, "guhs:kaaskorst_stenen_muur", 6)
    h.shapeless("mika_steen", ["guhs:kaaskorst", "guhs:kaaskorst", "guhs:kaaskorst", "guhs:kaaskorst", "guhs:mika_vet"], "guhs:mika_steen", 4)
    h.shaped("mika_steen_pilaar", ["P", "P"], {"P": "guhs:mika_steen_plaat"}, "guhs:mika_steen_pilaar")
    h.shaped("mika_steen_trap", ["K  ", "KK ", "KKK"], {"K": "guhs:mika_steen"}, "guhs:mika_steen_trap", 4)
    h.shaped("mika_steen_plaat", ["KKK"], {"K": "guhs:mika_steen"}, "guhs:mika_steen_plaat", 6)
    h.w(f"{D}/recipe/gebarsten_kaaskorst_stenen.json", {"type": "minecraft:smelting", "category": "blocks",
                                                        "ingredient": {"item": "guhs:kaaskorst_stenen"},
                                                        "result": {"id": "guhs:gebarsten_kaaskorst_stenen"}, "experience": 0.1, "cookingtime": 200})
    for src, out, n in (("kaaskorst", "kaaskorst_stenen", 1), ("kaaskorst", "kaaskorst_stenen_trap", 1), ("kaaskorst", "kaaskorst_stenen_plaat", 2),
                        ("kaaskorst", "kaaskorst_stenen_muur", 1), ("kaaskorst_stenen", "kaaskorst_stenen_trap", 1),
                        ("kaaskorst_stenen", "kaaskorst_stenen_plaat", 2), ("kaaskorst_stenen", "kaaskorst_stenen_muur", 1),
                        ("mika_steen", "mika_steen_pilaar", 1), ("mika_steen", "mika_steen_trap", 1), ("mika_steen", "mika_steen_plaat", 2)):
        h.w(f"{D}/recipe/{out}_van_{src}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{src}"},
                                                            "result": {"id": f"guhs:{out}", "count": n}})


def item(name, lo=1, hi=1, weight=1, extra=None):
    e = {"type": "minecraft:item", "name": mc(name), "weight": weight}
    fns = []
    if (lo, hi) != (1, 1):
        fns += [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    fns += extra or []
    if fns:
        e["functions"] = fns
    return e


def enchanted(name, weight):
    return item(name, weight=weight, extra=[{"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 20, "max": 39},
                                             "options": "#minecraft:on_random_loot"}])


def book_entry():
    """The Guheinde book (Guhboek GUHEINDE, like Guhboek.stack()): text components go in as JSON strings."""
    j = lambda c: json.dumps(c, ensure_ascii=False)
    return {"type": "minecraft:item", "name": "minecraft:written_book", "functions": [{"function": "minecraft:set_components", "components": {
        "minecraft:written_book_content": {"title": "Guhboek 13", "author": "De Bibliothecaris", "resolved": True,
                                           "pages": [j({"translate": f"book.guhs.bieb.guheinde.{i}"}) for i in range(5)]},
        "minecraft:custom_data": {"GuhsBoek": "guheinde"},
        "minecraft:custom_name": j({"translate": "book.guhs.bieb.guheinde.title", "italic": False, "color": "#F7B6CB"}),
        "minecraft:lore": [j({"translate": "item.guhs.bieb_boek.lore", "with": ["13", "13"], "italic": False, "color": "gray"})],
        "minecraft:enchantment_glint_override": False}}]}


def loot(h):
    D = h.D
    w = h.w

    def chest(name, pools):
        w(f"{D}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": pools})

    def roll(lo, hi, entries):
        return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}

    chest("knabbelkelder_gang", [roll(3, 6, [
        item("guhs:kaas_knabbels", 2, 6, 6), item("guhs:guh_kristal", 1, 4, 4), item("iron_ingot", 1, 5, 4), item("gold_ingot", 1, 3, 3),
        item("guhs:mika_traan", 1, 2, 3), item("guhs:mika_vet", 1, 3, 3), item("guhs:oog_van_vadsig", 1, 1, 1), item("bread", 1, 3, 3),
        item("guhs:gefrituurde_kaasknabbels", 1, 2, 2), enchanted("iron_sword", 1), enchanted("iron_chestplate", 1)])])
    chest("knabbelkelder_bieb", [{"rolls": 1, "entries": [book_entry()]}, roll(2, 5, [
        item("paper", 2, 7, 5), item("book", 1, 3, 4), item("guhs:guh_kristal", 1, 3, 3), enchanted("book", 3), item("guhs:mika_traan", 1, 1, 2)])])
    chest("knabbelkelder_cel", [roll(1, 3, [
        item("guhs:kaas_knabbels", 1, 3, 5), item("bone", 1, 2, 3), item("guhs:mika_vet", 1, 1, 2), item("stick", 1, 3, 2)])])
    chest("mika_vesting", [roll(4, 7, [
        item("diamond", 1, 3, 3), item("iron_ingot", 3, 8, 6), item("gold_ingot", 2, 7, 5), item("emerald", 2, 6, 3),
        item("guhs:mika_vet", 2, 6, 5), item("guhs:vahoege_vads_ingot", 1, 2, 2), item("guhs:mika_traan", 1, 3, 3),
        item("guhs:knabbelkristal", 1, 1, 1), enchanted("diamond_sword", 2), enchanted("diamond_boots", 2), enchanted("diamond_helmet", 2),
        enchanted("iron_pickaxe", 3), item("guhs:gefrituurde_kaasknabbels", 2, 5, 3)])])
    chest("vetschip", [roll(4, 7, [
        item("diamond", 2, 5, 4), item("emerald", 3, 8, 4), item("guhs:mika_vet", 4, 10, 6), item("guhs:vahoege_vads_ingot", 1, 3, 3),
        item("guhs:knabbelkristal", 1, 1, 2), enchanted("diamond_chestplate", 2), enchanted("diamond_leggings", 2), enchanted("diamond_pickaxe", 2),
        item("guhs:gefrituurde_kaasknabbels", 3, 8, 4), item("golden_apple", 1, 2, 2)])])
    chest("knabbelschat", [
        {"rolls": 1, "entries": [item("guhs:block_of_kaasknabbels", 4, 10)]},
        roll(4, 8, [item("guhs:kaas_knabbels", 16, 48, 8), item("guhs:gefrituurde_kaasknabbels", 8, 24, 5), item("guhs:vahoege_vads_ingot", 2, 5, 4),
                    item("guhs:guh_kristal", 6, 16, 5), item("diamond", 3, 8, 4), item("golden_apple", 1, 3, 3), item("guhs:knabbelkristal", 1, 2, 2),
                    item("enchanted_golden_apple", 1, 1, 1), item("guhs:kaasfondue", 1, 2, 2)])])
    w(f"{D}/loot_table/entities/opper_mika.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [item("guhs:mika_vet", 4, 8)]}, {"rolls": 1, "entries": [item("guhs:mika_traan", 2, 4)]}]})
    w(f"{D}/loot_table/entities/mika_larfje.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.3}], "entries": [item("guhs:mika_vet")]}]})
    w(f"{D}/loot_table/entities/hongerige_enderguh.json", {"type": "minecraft:entity", "pools": []})


# =====================================================================================================================
# advancements (tab "Het Guheinde"; most are given by the mod) and the texts
# =====================================================================================================================
ADVANCEMENTS = [
    # name, parent, icon, frame, criterion (None = given by the mod), title, description, hidden
    ("root", None, "guhs:oog_van_vadsig", "task", {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:mika_traan"}]}},
     "Het Guheinde", "Een Mika huilt... Dat is het begin van een groot avontuur", False),
    ("guheinde_opdracht", "root", "guhs:guh_kristal", "task", None, "Een koninklijke opdracht", "Luister naar het verhaal van de Koningguh", False),
    ("guheinde_oog", "root", "guhs:oog_van_vadsig", "task", None, "Een oog voor vads", "Gooi een Oog van Vadsig in de Guhmensie", False),
    ("find_knabbelkelder", "guheinde_oog", "guhs:kaaskorst_stenen", "goal",
     {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:knabbelkelder"]}}}},
     "De Knabbelkelder", "Vind een Knabbelkelder diep onder de Guhmensie", False),
    ("guheinde_larfje", "find_knabbelkelder", "guhs:aangevreten_kaaskorst_stenen", "task",
     {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                             "predicate": {"type": "guhs:mika_larfje"}}]}},
     "Larfjesmepper", "Mep een Mika-larfje dat je knabbels wil inpikken", False),
    ("guheinde_gered", "find_knabbelkelder", "guhs:kaas_knabbels", "task", None, "VAHOEG, gered!", "Geef een magere guh in de Knabbelkelder een kaasknabbel", False),
    ("guheinde_bevrijder", "guheinde_gered", "guhs:gefrituurde_kaasknabbels", "challenge", None, "Guhbevrijder",
     "Red zes magere guhs uit de cellen van de Mika's", False),
    ("guheinde_portaal", "find_knabbelkelder", "guhs:knabbelportaalframe", "goal", None, "Twaalf ogen", "Open het portaal naar het Guheinde", False),
    ("guheinde_binnen", "guheinde_portaal", "guhs:kaaskorst", "goal", None, "Waar zijn de knabbels?", "Stap in het Guheinde", False),
    ("guheinde_kristal", "guheinde_binnen", "guhs:knabbelkristal", "task", None, "Knabbels terug!", "Sla een knabbelkristal kapot", False),
    ("guheinde_voeren", "guheinde_kristal", "guhs:kaas_knabbels", "goal", None, "Eindelijk een knabbel",
     "Geef de uitgeputte Enderguh een kaasknabbel", False),
    ("guheinde_winst", "guheinde_voeren", "guhs:knabbelkroon", "challenge", None, "OPPER-GEVADST!", "Versla Opper-Mika en breng de knabbels thuis", False),
    ("guheinde_ridder", "guheinde_winst", "guhs:vahoege_vads_sword", "goal", None, "Ridder van het Guheinde",
     "Laat je door de Koningguh tot ridder slaan", False),
    ("guheinde_nog_eens", "guheinde_winst", "guhs:enderguh_ei", "goal", None, "Nog een rondje", "Roep Opper-Mika terug en versla hem nog een keer", False),
    ("guheinde_snel", "guheinde_winst", "minecraft:clock", "challenge", None, "Vads-vlug", "Versla Opper-Mika binnen vijf minuten", True),
    ("guheinde_poort", "guheinde_winst", "guhs:mika_steen", "task", None, "Door de Knabbelpoort", "Reis door een Knabbelpoort naar de buiteneilanden", False),
    ("guheinde_terugpoort", "guheinde_poort", "minecraft:recovery_compass", "task", None, "Tuut, terug naar het midden",
     "Stap in een Terugpoort op de buiteneilanden en zweef terug naar het grote eiland", False),
    ("guheinde_vesting", "guheinde_poort", "guhs:mika_steen_pilaar", "goal",
     {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:mika_vesting"]}}}},
     "Een Mika-vesting", "Vind een Mika-vesting op de buiteneilanden", False),
    ("guheinde_vleugels", "guheinde_vesting", "guhs:guhvleugels", "challenge",
     {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:guhvleugels"}]}},
     "Guhvleugels!", "Pak de Guhvleugels van het vetschip", False),
]
QUEST_ADVANCEMENTS = ["seen_mager", "seen_vahoege_ender"]


def advancements(h):
    D = h.D
    for name, parent, icon, frame, crit, title, desc, hidden in ADVANCEMENTS:
        adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guheinde.{name}.title"},
                           "description": {"translate": f"advancements.guhs.guheinde.{name}.description"},
                           "frame": frame, "show_toast": name != "root", "announce_to_chat": name != "root", "hidden": hidden},
               "criteria": {"done": crit or {"trigger": "minecraft:impossible"}}}
        if parent:
            adv["parent"] = f"guhs:guheinde/{parent}"
        else:
            adv["display"]["background"] = "guhs:textures/block/kaaskorst.png"
        h.w(f"{D}/advancement/guheinde/{name}.json", adv)
        lang(h, f"advancements.guhs.guheinde.{name}.title", title)
        lang(h, f"advancements.guhs.guheinde.{name}.description", desc)
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    h.w(f"{D}/advancement/quest/tamed_vahoege_ender.json", {"criteria": {"done": {"trigger": "minecraft:tame_animal", "conditions": {
        "entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                    "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"vahoege_ender\"}"}}]}}}})


def texts(h):
    for key, text in TEXTS.items():
        lang(h, key, text)


def selfcheck(h):
    """Every block and item of the Guheinde has its model, texture and name (check_assets.py only knows the registries)."""
    A = h.A
    missing = []
    blocks = ["kaaskorst", "kaaskorst_stenen", "gebarsten_kaaskorst_stenen", "kaaskorst_stenen_trap", "kaaskorst_stenen_plaat",
              "kaaskorst_stenen_muur", "aangevreten_kaaskorst_stenen", "mika_steen", "mika_steen_pilaar", "mika_steen_trap", "mika_steen_plaat",
              "knabbelportaalframe", "guheinde_portaal", "knabbelsokkel", "knabbelslot", "knabbelpoort", "enderguh_ei", "opper_mikatrofee"]
    for b in blocks:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ["kaaskorst", "kaaskorst_stenen", "gebarsten_kaaskorst_stenen", "kaaskorst_stenen_trap", "kaaskorst_stenen_plaat", "kaaskorst_stenen_muur",
              "aangevreten_kaaskorst_stenen", "mika_steen", "mika_steen_pilaar", "mika_steen_trap", "mika_steen_plaat", "knabbelportaalframe",
              "knabbelsokkel", "knabbelslot", "enderguh_ei", "opper_mikatrofee", "oog_van_vadsig", "mika_traan", "knabbelkristal", "knabbelkroon",
              "guhvleugels", "mika_larfje_spawn_egg"]:
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
    for t in ["entity/opper_mika", "entity/knabbelkristal", "entity/guhvleugels", "entity/guh_mager", "entity/guh_vahoege_ender",
              "entity/guh_vahoege_ender_glowmask", "environment/guheinde_sky", "entity/equipment/humanoid/knabbelkroon"] + \
             [f"entity/guh_hongerig_{i}" for i in range(9)]:
        if not os.path.exists(os.path.join(h.TEX, t + ".png")):
            missing.append(f"texture {t}")
    if missing:
        raise SystemExit(f"guheinde assets missing: {missing}")


def build(h):
    block_textures(h)
    item_textures(h)
    entity_textures(h)
    blocks_and_items(h)
    dimension(h)
    recipes(h)
    loot(h)
    advancements(h)
    texts(h)
    guheinde_bouw.build(h)
    selfcheck(h)


# =====================================================================================================================
# the Guhdex guhs (make_guh_variants): the magere guh and the Vahoege Enderguh
# =====================================================================================================================
def variants(rng, v):
    return {
        "mager": ((150, 140, 146), {}),
        "vahoege_ender": ((236, 180, 70), {"ender_bone": lambda: v.fabric((150, 40, 110), rng, 6),
                                           "ender_wing": lambda: v.fabric(WING, rng, 10)}),
    }


# =====================================================================================================================
# FTB quests (rows y = 56 and 58): never locked
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    adv = lambda n: fq.adv(f"guhs:guheinde/{n}")
    y1, y2 = 56, 58
    q("guheinde_traan", "Een Mika huilt", "De Mika's pikken alle kaasknabbels in... maar soms huilen ze ook. Versla Mika's (Grote Mika huilt het hardst) of win drie keer van de Mika-baas met VADS voor een &5Mika-traan&r.",
      "guhs:mika_traan", [fq.item("guhs:mika_traan")], x=-8, y=y1, shape="hexagon")
    q("guheinde_koning", "Een koninklijke opdracht", "Praat met een wilde &dKoningguh&r op zijn troon in het guhkasteel. Hij vertelt waar alle knabbels gebleven zijn. Njeg.",
      "guhs:guh_kristal", [adv("guheinde_opdracht")], x=-6, y=y1)
    q("guheinde_ogen", "Twaalf Ogen van Vadsig", "Guhkristal + kaasknabbel + Mika-traan = een &6Oog van Vadsig&r. Je hebt er twaalf nodig voor het portaal (en een paar extra om te gooien, want ze gaan soms &5njeg&r kapot).",
      "guhs:oog_van_vadsig", [fq.item("guhs:oog_van_vadsig", 12)], rewards=(("guhs:guh_kristal", 6),), x=-4, y=y1)
    q("guheinde_kelder", "De Knabbelkelder", "Gooi een Oog van Vadsig in de Guhmensie en volg het. Duikt het naar beneden? Graven maar! Of gebruik het superkompas (Einde).",
      "guhs:kaaskorst_stenen", [fq.structure("knabbelkelder")], rewards=(("guhs:kaas_knabbels", 16),), x=-2, y=y1, shape="octagon", xp=150)
    q("guheinde_gered", "Guhbevrijder", "In de cellen van de Knabbelkelder zitten grauwe, magere guhs. Geef ze een kaasknabbel en ze worden meteen weer VAHOEG! Red er zes.",
      "guhs:kaas_knabbels", [adv("guheinde_bevrijder")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=0, y=y1, xp=150)
    q("guheinde_portaal", "Twaalf ogen", "Stop een Oog van Vadsig in elk van de twaalf knabbelportaalframes in de portaalkamer. Pas op voor de Mika-larfjes!",
      "guhs:knabbelportaalframe", [adv("guheinde_portaal")], x=2, y=y1)
    q("guheinde_binnen", "Het Guheinde", "Spring in het portaal. Welkom in het Guheinde: een eiland van kaaskorst, hoog boven het niets. Waar zijn al die knabbels?",
      "guhs:kaaskorst", [fq.dim("guhs:guheinde")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=4, y=y1, shape="octagon", xp=200)
    q("guheinde_kristal", "Knabbels terug!", "Op de kaaspilaren staan knabbelkristallen vol gestolen knabbels: ze genezen Opper-Mika. Schiet ze kapot (of klim omhoog)!",
      "guhs:knabbelkristal", [adv("guheinde_kristal")], x=-8, y=y2)
    q("guheinde_voeren", "Een knabbel voor de Enderguh", "Zijn alle kristallen kapot? Dan landt de uitgeputte Enderguh op de Knabbelberg. Geef hem een kaasknabbel: VAHOEG!",
      "guhs:kaas_knabbels", [adv("guheinde_voeren")], x=-6, y=y2)
    q("guheinde_winst", "OPPER-GEVADST!", "Versla Opper-Mika te voet. Pas op: hij pikt je knabbels in, maakt vetplassen en roept Mika-hulpjes. Hij laat alles vallen als je wint!",
      "guhs:knabbelkroon", [adv("guheinde_winst")], rewards=(("guhs:gefrituurde_kaasknabbels", 16),), x=-4, y=y2, shape="gear", xp=1000)
    q("guheinde_enderguh", "Je eigen Vahoege Enderguh", "Na je eerste overwinning krijg je de bevrijde Enderguh als rijdier. Stap op en vlieg! (Later krijg je Enderguh-eieren.)",
      "guhs:guhdex", [fq.adv("tamed_vahoege_ender")], x=-2, y=y2, xp=200)
    q("guheinde_ridder", "Ridder van het Guheinde", "Ga terug naar de Koningguh en laat je tot ridder slaan. Kniel!",
      "guhs:vahoege_vads_sword", [adv("guheinde_ridder")], rewards=(("guhs:guh_kristal", 8),), x=0, y=y2, xp=200)
    q("guheinde_schat", "Opper-Mika's knabbelschat", "Na de eerste overwinning gaat de knabbelschat onder de Knabbelberg open. Loop naar de zuidkant van de berg!",
      "guhs:block_of_kaasknabbels", [fq.item("guhs:block_of_kaasknabbels", 4)], x=2, y=y2)
    q("guheinde_poort", "Door de Knabbelpoort", "Na elke overwinning verschijnt er een Knabbelpoort rond het eiland. Spring erin: hij gooit je naar de buiteneilanden.",
      "guhs:mika_steen", [adv("guheinde_poort")], x=4, y=y2)
    q("guheinde_vesting", "Een Mika-vesting", "Op de buiteneilanden staan Mika-vestingen van paarse Mika-steen, vol Mika's en buit.",
      "guhs:mika_steen_pilaar", [fq.structure("mika_vesting")], rewards=(("guhs:kaas_knabbels", 16),), x=6, y=y2, shape="octagon", xp=200)
    q("guheinde_vleugels", "Guhvleugels!", "Bij de helft van de vestingen ligt een vettig Mika-vetschip. Op de boeg hangen de &dGuhvleugels&r: zweven maar!",
      "guhs:guhvleugels", [fq.item("guhs:guhvleugels")], x=8, y=y2, shape="gear", xp=500)
    q("guheinde_terugpoort", "Tuut, terug naar het midden", "Overal op de buiteneilanden staat een &dTerugpoort&r: een poortje met een guhkop erop. Stap erin en je zweeft vahoeg terug naar het grote eiland.",
      "guhs:mika_steen", [adv("guheinde_terugpoort")], x=8, y=y1, xp=100)
    q("guheinde_terug", "Nog een rondje", "Zet vier knabbelkristallen op de vier knabbelsokkels rond het terugportaal en Opper-Mika komt terug. Versla hem nog eens voor een Enderguh-ei.",
      "guhs:enderguh_ei", [adv("guheinde_nog_eens")], rewards=(("guhs:mika_traan", 4),), x=6, y=y1, xp=300)
