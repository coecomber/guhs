"""
1.8.0 (vadskracht): de Fabriekshal. Java: feature/fabriek (FabriekFeature, FabriekBlock, the block entities, Koppelen,
Bouwwegen, Verven, Kleuren), klusjes/ScherenKlus + BloemetjesKlus, the Wolkenschaapje as a maatje (bio/dieren).

  build(h)    the seven machines (features/vadskracht.machine: Spinmachine, Verfbad, Knuffelrecycler, Scheerstal,
              Bouwblokken-fabriek, Kleurmolen, Regenboogpers), the items pluis and wolkenschaapje_item, their recipes, the
              tags guhs:spinbaar / guhs:recyclebaar, the Wolkenschaapje's spawn in De Zingende Weide (low weight), the hidden
              advancements guhs:quest/fabriek_*, the texts (Dutch in nl_nl, English in tools/lang/en/c217_fabriek.json)
  ftb(fq)     the quests of the Fabriekshal in guhs_techniek (English overlay tools/lang/en/c217_fabriek_ftb.json)

The blokuitleg entries live in tools/wiki_site/blokuitleg.py (<fabriek> block).
Standalone (from the project root): PYTHONPATH=tools python tools/features/fabriek.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

from features import vadskracht

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("fabriek", "De Fabriekshal: wol, kleur en bouwblokken", "item:guhs:block/bouwblokken_fabriek_voor_werkt", None)]

# machine -> (basis, accent)
MACHINES = {
    "spinmachine": ((244, 236, 240), (246, 150, 196)),
    "verfbad": ((120, 190, 236), (255, 120, 170)),
    "knuffelrecycler": ((236, 150, 190), (120, 210, 170)),
    "scheerstal": ((196, 150, 104), (250, 246, 236)),
    "bouwblokken_fabriek": ((226, 160, 92), (250, 236, 210)),
    "kleurmolen": ((246, 214, 110), (150, 110, 220)),
    "regenboogpers": ((160, 110, 214), (255, 214, 92)),
}
ITEMS = ["pluis", "wolkenschaapje_item"]
ADVS = ["fabriek_spin", "fabriek_verf", "fabriek_recycler", "fabriek_scheerstal", "fabriek_bouwblokken", "fabriek_kleurmolen",
        "fabriek_regenboogpers"]

F = "gui.guhs.fabriek."
TEKSTEN = {  # key -> (Dutch, English)
    # --- the machines ---
    "block.guhs.spinmachine": ("Spinmachine", "Spinning Machine"),
    "block.guhs.spinmachine.lore": ("Spint pluis tot witte wol: 2 pluis is 1 wol. Draait op vadskracht. Snorrr!",
                                    "Spins fluff into white wool: 2 fluff is 1 wool. Runs on vadskracht. Whirrr!"),
    "block.guhs.verfbad": ("Verfbad", "Dye Bath"),
    "block.guhs.verfbad.lore": ("Wol (of glas, klei, knuffelblokken...) + kleurstof = gekleurd. Plons!",
                                "Wool (or glass, clay, cuddle blocks...) + dye = coloured. Splash!"),
    "block.guhs.knuffelrecycler": ("Knuffelrecycler", "Plushie Recycler"),
    "block.guhs.knuffelrecycler.lore": ("Oude knuffels, guhkleding, wol en kussens worden weer pluis. Niks gaat verloren, njeg!",
                                        "Old plushies, guh clothes, wool and cushions turn back into fluff. Nothing is wasted, njeg!"),
    "block.guhs.scheerstal": ("Scheerstal", "Shearing Stable"),
    "block.guhs.scheerstal.lore": ("Zet je tamme Wolkenschaapjes eromheen (6 blokken): de stal scheert ze vanzelf en bewaart het pluis.",
                                   "Keep your tame Cloud Lambs around it (6 blocks): the stable shears them by itself and keeps the fluff."),
    "block.guhs.bouwblokken_fabriek": ("Bouwblokken-fabriek", "Building Block Factory"),
    "block.guhs.bouwblokken_fabriek.lore": ("Een slimme steenzaag die ook verft. Leg een voorbeeld neer en hij maakt het in bulk. Koppel een Bank Guh of kist!",
                                            "A smart stonecutter that paints too. Put down an example and it makes it in bulk. Link a Bank Guh or a chest!"),
    "block.guhs.kleurmolen": ("Kleurmolen", "Colour Mill"),
    "block.guhs.kleurmolen.lore": ("Maalt bloemetjes tot kleurstof, met bonus: 2 tot 3 per bloem. Alle 16 kleuren!",
                                   "Grinds flowers into dye, with a bonus: 2 to 3 per flower. All 16 colours!"),
    "block.guhs.regenboogpers": ("Regenboogpers", "Rainbow Press"),
    "block.guhs.regenboogpers.lore": ("Perst pluis met kaassaus tot elke kleur die je kiest. Geen bloemetje nodig!",
                                      "Presses fluff and cheese sauce into any colour you pick. No flowers needed!"),
    # --- the items ---
    "item.guhs.pluis": ("Pluis", "Fluff"),
    "item.guhs.pluis.lore": ("Zacht, wit en een beetje kriebelig. Spin het tot wol in de Spinmachine.",
                             "Soft, white and a little tickly. Spin it into wool in the Spinning Machine."),
    "item.guhs.wolkenschaapje_item": ("Wolkenschaapje", "Cloud Lamb"),
    "item.guhs.wolkenschaapje_item.tooltip": ("Een opgepakt wolkenschaapje (met naam en al). Rechtsklik op een blok: het huppelt er weer vanaf. Bleh!",
                                              "A picked-up cloud lamb (name and all). Right-click a block: it hops off again. Bleh!"),
    # --- screen lines ---
    F + "spinmachine.leeg": ("Wacht op pluis (2 per wol)", "Waiting for fluff (2 per wool)"),
    F + "verfbad.geen_verf": ("Wacht op kleurstof", "Waiting for dye"),
    F + "knuffelrecycler.leeg": ("Wacht op oude knuffels, kleding, wol of kussens", "Waiting for old plushies, clothes, wool or cushions"),
    F + "scheerstal.geen_schaapje": ("Geen pluizig Wolkenschaapje binnen %s blokken", "No fluffy Cloud Lamb within %s blocks"),
    F + "kleurmolen.leeg": ("Wacht op bloemetjes", "Waiting for flowers"),
    F + "regenboogpers.kleur": ("Kleur: %s", "Colour: %s"),
    F + "regenboogpers.saus": ("Kaassaus: %s / %s mB", "Cheese sauce: %s / %s mB"),
    F + "regenboogpers.geen_saus": ("Wacht op kaassaus (emmer, Sausvat of sausbuis ernaast)", "Waiting for cheese sauce (bucket, Sauce Vat or sauce pipe next to it)"),
    F + "bouwblokken.bank": ("Gekoppeld aan een Bank Guh", "Linked to a Bank Guh"),
    F + "bouwblokken.bank_weg": ("Gekoppelde Bank Guh is nergens te vinden", "The linked Bank Guh is nowhere to be found"),
    F + "bouwblokken.kist": ("Gekoppelde kist: %s %s %s", "Linked chest: %s %s %s"),
    F + "bouwblokken.geen_voorbeeld": ("Leg een voorbeeld in het middelste vakje: dat blok maakt hij", "Put an example in the middle slot: that is the block it makes"),
    F + "bouwblokken.geen_bron": ("Geen blokken (of kleurstof) om dat van te maken", "No blocks (or dye) to make that from"),
    # --- linking ---
    F + "koppel.lege_sleutel": ("Deze Banksleutel kent nog geen bank. Klik eerst op een Bank Guh!", "This Bank Key doesn't know a bank yet. Click a Bank Guh first!"),
    F + "koppel.bank": ("Gekoppeld aan de Bank Guh! Vahoeg!", "Linked to the Bank Guh! Vahoeg!"),
    F + "koppel.klik_kist": ("Klik nu op een kist (binnen %s blokken). Nog eens op de fabriek: alle koppelingen los.",
                             "Now click a chest (within %s blocks). The factory again: forget all links."),
    F + "koppel.kist": ("Kist gekoppeld: %s %s %s", "Chest linked: %s %s %s"),
    F + "koppel.geen_kist": ("Dat is geen kist. Koppelen gestopt.", "That's not a chest. Linking stopped."),
    F + "koppel.te_ver": ("Te ver weg! Een kist mag hoogstens %s blokken ver staan.", "Too far! A chest can be at most %s blocks away."),
    F + "koppel.los": ("Alle koppelingen los. Njeg.", "All links forgotten. Njeg."),
    # --- the Wolkenschaapje as a maatje ---
    "gui.guhs.piep.menu.sub.wolkenschaapje": ("Wolkenschaapje: een wolkje op pootjes", "Cloud Lamb: a little cloud on legs"),
    "gui.guhs.piep.menu.speciaal.wolkenschaapje": ("Pluisknuffel!", "Fluff hug!"),
    "gui.guhs.piep.menu.speciaal.wolkenschaapje.tooltip": ("Knuffel je schaapje: je wordt zo licht als een wolkje en zweeft zachtjes naar beneden.",
                                                           "Hug your lamb: you become as light as a cloud and float down gently."),
    "gui.guhs.piep.opgepakt.wolkenschaapje": ("Je pakt %s op. Het is net een kussentje. Bleh!", "You pick up %s. It's just like a pillow. Bleh!"),
    "gui.guhs.piep.neergezet.wolkenschaapje": ("%s huppelt weer rond. Bleh bleh!", "%s hops around again. Bleh bleh!"),
    "gui.guhs.wolkenschaapje.knuffel": ("%s knuffelt je pluizig: je bent zo licht als een wolkje!", "%s gives you a fluffy hug: you're as light as a cloud!"),
    "gui.guhs.wolkenschaapje.knuffel_rust": ("Je schaapje moet even bijkomen (%s s)", "Your lamb needs a little rest (%s s)"),
    # --- chores ---
    "gui.guhs.klus.scheren": ("Scheren", "Shearing"),
    "gui.guhs.klus.scheren.tip": ("Scheert de tamme Wolkenschaapjes in de klus-area en brengt het wolkenpluis naar de kist. (Guhs)",
                                  "Shears the tame Cloud Lambs in the chore area and brings the cloud fluff to the chest. (Guhs)"),
    "gui.guhs.huisje.overzicht.klus.scheren.pluizig": ("Pluizige schaapjes: %s", "Fluffy lambs: %s"),
    "gui.guhs.huisje.overzicht.klus.scheren.kaal": ("De schaapjes zijn nog kaal: het pluis groeit weer aan", "The lambs are still bald: the fluff is growing back"),
    "gui.guhs.huisje.overzicht.klus.scheren.geen": ("Geen tamme Wolkenschaapjes", "No tame Cloud Lambs"),
    "gui.guhs.huisje.overzicht.klus.scheren.nodig": ("tamme Wolkenschaapjes in de klus-area", "tame Cloud Lambs in the chore area"),
    "gui.guhs.dagboek.eerste.klusjes_scheren": ("Knip knip", "Snip snip"),
    "gui.guhs.dagboek.eerste.klusjes_scheren.tekst": ("Ik heb een wolkenschaapje geschoren. Het zei bleh. Ik zei ook bleh. Nu hebben we allebei pluis.",
                                                      "I sheared a cloud lamb. It said bleh. I said bleh too. Now we both have fluff."),
    "gui.guhs.klus.bloemetjes": ("Bloemetjes plukken", "Picking Flowers"),
    "gui.guhs.klus.bloemetjes.tip": ("Plukt de bloemen in de klus-area (ze blijven staan) en brengt precies die bloemen naar de kist: voer voor de Kleurmolen. (Guhs)",
                                     "Picks the flowers in the chore area (they keep standing) and brings exactly those flowers to the chest: food for the Colour Mill. (Guhs)"),
    "gui.guhs.huisje.overzicht.klus.bloemetjes.bloemen": ("Bloemetjes om te plukken: %s", "Flowers to pick: %s"),
    "gui.guhs.huisje.overzicht.klus.bloemetjes.later": ("De bloemetjes groeien weer aan", "The flowers are growing back"),
    "gui.guhs.huisje.overzicht.klus.bloemetjes.geen": ("Geen bloemen in de klus-area", "No flowers in the chore area"),
    "gui.guhs.huisje.overzicht.klus.bloemetjes.nodig": ("bloemen in de klus-area", "flowers in the chore area"),
    "gui.guhs.dagboek.eerste.klusjes_bloemetjes": ("Bloemetjesdag", "Flower day"),
    "gui.guhs.dagboek.eerste.klusjes_bloemetjes.tekst": ("Ik heb bloemetjes geplukt en ze bleven gewoon staan. Toverbloemetjes! Of ik ben heel voorzichtig.",
                                                         "I picked flowers and they just kept standing. Magic flowers! Or I'm very careful."),
}

FTB = [  # key, Dutch title, Dutch text, English title, English text, icon, advancement, x, y, deps
    ("spin", "Snorrende spinmachine", "Bouw een &dSpinmachine&r en spin &fpluis&r tot wol: 2 pluis is 1 witte wol. Pluis krijg je van Wolkenschaapjes (scheren) of uit de Knuffelrecycler.",
     "Whirring Spinner", "Build a &dSpinning Machine&r and spin &ffluff&r into wool: 2 fluff is 1 white wool. Fluff comes from Cloud Lambs (shearing) or the Plushie Recycler.",
     "guhs:spinmachine", "fabriek_spin", 0, 0, []),
    ("verf", "Plons in het verfbad", "Leg wol en kleurstof in het &dVerfbad&r: het verft alles wat het spel kan verven, ook glas, klei en knuffelblokken.",
     "Splash in the Dye Bath", "Put wool and dye in the &dDye Bath&r: it dyes everything the game can dye, glass, clay and cuddle blocks too.",
     "guhs:verfbad", "fabriek_verf", 2, 0, ["spin"]),
    ("scheerstal", "Scheerstal", "Zet tamme &dWolkenschaapjes&r rond een &dScheerstal&r: hij scheert ze vanzelf. Tem ze met pluisgras of wolkensuikerspin en kweek ze met wolkensuikerspin!",
     "Shearing Stable", "Keep tame &dCloud Lambs&r around a &dShearing Stable&r: it shears them by itself. Tame them with fluffgrass or cloud candy floss and breed them with cloud candy floss!",
     "guhs:scheerstal", "fabriek_scheerstal", 0, 2, ["spin"]),
    ("recycler", "Niks gaat verloren", "Stop oude knuffels, guhkleding, wol of kussens in de &dKnuffelrecycler&r: er komt pluis uit.",
     "Nothing Is Wasted", "Put old plushies, guh clothes, wool or cushions into the &dPlushie Recycler&r: fluff comes out.",
     "guhs:knuffelrecycler", "fabriek_recycler", 2, 2, ["spin"]),
    ("bouwblokken", "Bouwblokken in bulk", "Leg een &dvoorbeeld&r in de &dBouwblokken-fabriek&r: hij maakt trappen, platen, muurtjes, gladde en gekleurde blokken. Koppel een Bank Guh (Banksleutel) of een kist (sneak + klik, dan de kist).",
     "Building Blocks in Bulk", "Put an &dexample&r in the &dBuilding Block Factory&r: it makes stairs, slabs, walls, smooth and coloured blocks. Link a Bank Guh (Bank Key) or a chest (sneak + click, then the chest).",
     "guhs:bouwblokken_fabriek", "fabriek_bouwblokken", 4, 0, ["verf"]),
    ("kleurmolen", "Alle kleuren van de regenboog", "Maal bloemetjes in de &dKleurmolen&r: 2 tot 3 kleurstof per bloem. Laat een guh het klusje &dBloemetjes plukken&r doen voor een eindeloze voorraad!",
     "All the Colours of the Rainbow", "Grind flowers in the &dColour Mill&r: 2 to 3 dye per flower. Let a guh do the chore &dPicking Flowers&r for an endless supply!",
     "guhs:kleurmolen", "fabriek_kleurmolen", 4, 2, ["verf"]),
    ("regenboogpers", "Regenboogpers", "Pers pluis met &6kaassaus&r tot elke kleur in de &dRegenboogpers&r. Kies de kleur met sneak + klik, of leg een voorbeeld-kleurstof neer.",
     "Rainbow Press", "Press fluff and &6cheese sauce&r into any colour in the &dRainbow Press&r. Pick the colour with sneak + click, or put down an example dye.",
     "guhs:regenboogpers", "fabriek_regenboogpers", 6, 2, ["kleurmolen"]),
]


def _pluis_img():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for (x, y, r) in ((5, 9, 4), (10, 9, 4), (8, 6, 4), (7, 10, 4)):
        d.ellipse((x - r, y - r, x + r, y + r), fill=(250, 246, 250, 255))
    for (x, y) in ((4, 11), (11, 11), (8, 12), (12, 8)):
        img.putpixel((x, y), (236, 214, 230, 255))
    for (x, y) in ((6, 4), (9, 4), (4, 7)):
        img.putpixel((x, y), (255, 255, 255, 255))
    img.putpixel((6, 8), (60, 40, 60, 255))
    img.putpixel((9, 8), (60, 40, 60, 255))
    img.putpixel((7, 10), (246, 150, 196, 255))
    img.putpixel((8, 10), (246, 150, 196, 255))
    return img


def _schaapje_img():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for (x, y, r) in ((5, 7, 3), (9, 6, 3), (11, 8, 3), (7, 9, 3)):
        d.ellipse((x - r, y - r, x + r, y + r), fill=(248, 248, 255, 255))
    d.rectangle((4, 11, 5, 14), fill=(120, 110, 140, 255))
    d.rectangle((10, 11, 11, 14), fill=(120, 110, 140, 255))
    d.ellipse((1, 6, 5, 10), fill=(250, 214, 226, 255))
    img.putpixel((2, 7), (50, 40, 60, 255))
    img.putpixel((4, 7), (50, 40, 60, 255))
    img.putpixel((3, 9), (230, 120, 160, 255))
    return img


def build(h):
    for naam, (basis, accent) in MACHINES.items():
        vadskracht.machine(h, naam, basis, accent, extra_tags=("guhs/tags/block/vadskracht", "guhs/tags/block/techklus/machines"))
    h.save(_pluis_img(), "item", "pluis.png")
    h.item_model("pluis")
    h.save(_schaapje_img(), "item", "wolkenschaapje_item.png")
    h.item_model("wolkenschaapje_item")
    # --- recipes ---
    S = "#minecraft:stone_crafting_materials"
    h.shaped("spinmachine", ["IWI", "SDS", "PKP"], {"I": "minecraft:iron_ingot", "W": "#minecraft:wool", "S": "minecraft:string",
                                                    "D": "guhs:guh_wire", "P": "#minecraft:planks", "K": "guhs:kaas_knabbels"}, "guhs:spinmachine")
    h.shaped("verfbad", ["I I", "WDW", "SKS"], {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket", "D": "guhs:guh_wire",
                                                "S": S, "K": "guhs:kaas_knabbels"}, "guhs:verfbad")
    h.shaped("knuffelrecycler", ["ISI", "CDC", "IKI"], {"I": "minecraft:iron_ingot", "S": "minecraft:shears", "C": "minecraft:chest",
                                                        "D": "guhs:guh_wire", "K": "guhs:kaas_knabbels"}, "guhs:knuffelrecycler")
    h.shaped("scheerstal", ["PHP", "FSF", "PDP"], {"P": "#minecraft:planks", "H": "minecraft:hay_block", "F": "#minecraft:wooden_fences",
                                                   "S": "minecraft:shears", "D": "guhs:guh_wire"}, "guhs:scheerstal")
    h.shaped("bouwblokken_fabriek", ["IZI", "VDV", "SKS"], {"I": "minecraft:iron_ingot", "Z": "minecraft:stonecutter", "V": "#c:dyes",
                                                            "D": "guhs:guh_wire", "S": S, "K": "guhs:kaas_knabbels"}, "guhs:bouwblokken_fabriek")
    h.shaped("kleurmolen", ["IBI", "GDG", "SKS"], {"I": "minecraft:iron_ingot", "B": "#minecraft:flowers", "G": "minecraft:grindstone",
                                                   "D": "guhs:guh_wire", "S": S, "K": "guhs:kaas_knabbels"}, "guhs:kleurmolen")
    h.shaped("regenboogpers", ["GPG", "EDE", "IKI"], {"G": "minecraft:gold_ingot", "P": "minecraft:piston", "E": "guhs:kaas_saus_bucket",
                                                      "D": "guhs:guh_wire", "I": "minecraft:iron_ingot", "K": "guhs:kleurmolen"}, "guhs:regenboogpers")
    # --- tags ---
    h.add_tag("guhs/tags/item/spinbaar", ["guhs:pluis", "guhs:wolkenpluis"])
    h.add_tag("guhs/tags/item/recyclebaar", [])   # (empty: knuffel_* items, wool, carpets, clothes and *kussen go by themselves)
    # --- the Wolkenschaapje also lives in De Zingende Weide (a little) ---
    h.w(f"{h.D}/neoforge/biome_modifier/wolkenschaapje_zingende_weide.json", {
        "type": "neoforge:add_spawns", "biomes": ["guhs:zingende_weide"],
        "spawners": [{"type": "guhs:wolkenschaapje", "weight": 6, "minCount": 1, "maxCount": 2}]})
    # --- hidden advancements for the quests ---
    for a in ADVS:
        h.w(f"{h.D}/advancement/quest/{a}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def ftb(fq):
    for key, nl_t, nl_d, en_t, en_d, icoon, adv, x, y, deps in FTB:
        fq.q(f"fabriek_{key}", nl_t, nl_d, icoon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 16),),
             deps=[f"fabriek_{d}" for d in deps], x=x, y=y, shape="gear" if key in ("bouwblokken", "regenboogpers") else None, xp=100)


def selfcheck(h):
    weg = []
    for n in MACHINES:
        for p in (f"{h.A}/blockstates/{n}.json", f"{h.A}/models/item/{n}.json", f"{h.D}/loot_table/blocks/{n}.json", f"{h.D}/recipe/{n}.json"):
            if not os.path.exists(p):
                weg.append(p)
    for n in ITEMS:
        if not os.path.exists(f"{h.A}/models/item/{n}.json"):
            weg.append(n)
    if weg:
        raise SystemExit(f"fabriek self-check failed: {weg}")


def _schrijf(pad, d):
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    _schrijf(os.path.join("tools", "lang", "en", "c217_fabriek.json"), {k: en for k, (nl, en) in TEKSTEN.items()})
    ftb_en = {"ftb.guhs_techniek.section.fabriek": "The Factory Hall: wool, colour and building blocks"}
    for key, nl_t, nl_d, en_t, en_d, *_ in FTB:
        ftb_en[f"ftb.guhs_techniek.q.fabriek_{key}.title"] = en_t
        ftb_en[f"ftb.guhs_techniek.q.fabriek_{key}.desc"] = en_d
    _schrijf(os.path.join("tools", "lang", "en", "c217_fabriek_ftb.json"), ftb_en)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ITEMS + list(MACHINES):
        _schrijf(os.path.join(a, n + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}})
    print(f"fabriek: {len(MACHINES)} machines, {len(ITEMS)} items, {len(ADVS)} advancements, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
