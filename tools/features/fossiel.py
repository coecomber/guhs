"""
1.8.0 (vadskracht, ontwerp-guhs180-vadskracht.md V3-V5): the Fossiel-broedmachine and the four oerguhs. Java: feature/fossiel
(FossielFeature, BroedmachineBlock(Entity), OerSoort, OerGuhs, client/BroedmachineRenderer, client/FossielClient).

  build(h)    the broedmachine (a low guh machine with a glass bell on top: the egg in it is the BlockEntityRenderer's vanilla
              sniffer egg), the four fossils (items, textures, where you find them: own loot tables guhs:fossiel/<id> added
              to the old fossil chests by loot modifiers), the recipe, the advancements of the quests, the texts (Dutch in
              nl_nl, English in tools/lang/en/c217_fossiel.json)
  bones(h)    the oerguhs' bones in guh.geo.json (prefix "oer_", hidden on every other guh: GuhVariant.VARIANT_BONES). Like
              the Beverguh's they have NO swatch of their own: every cube samples a swatch that another variant's bone
              already owns (SWATCH_VAN), and only the oerguh textures paint it. Idempotent: the oer_ bones are taken out and
              put back. (Not tools/make_guh_variants.py: that one repaints the hand-painted variant textures.)
  textures(h) guh_<variant>.png and guh_slaap/guh_<variant>.png of the four oerguhs, painted on guh.png
  ftb(fq)     the section "Fossielen en oerguhs" in Guh-technologie

Standalone (from the project root): PYTHONPATH=tools python tools/features/fossiel.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("fossiel", "Fossielen en oerguhs", "item:guhs:fossiel_tguh", None)]

BLOK = "fossiel_broedmachine"
T = "gui.guhs.fossiel."
# OerSoort id -> (GuhVariant id, Dutch name, English name)
SOORTEN = {
    "mammoet": ("mammoetguh", "Mammoet-guh", "Mammoth Guh"),
    "tguh": ("tguh", "T-Guh", "T-Guh"),
    "pteroguh": ("pteroguh", "Pteroguh", "Pteroguh"),
    "stokstaartje": ("stokstaartjeguh", "Oer-stokstaartje-guh", "Ancient Meerkat Guh"),
}
# where the fossils lie: OerSoort id -> [(chest loot table, chance)]
VINDPLEKKEN = {
    "mammoet": [("guhs:chests/onderwol/reuzenguh_fossiel", 0.2), ("guhs:chests/fossielmijn_opgraving", 0.12)],
    "tguh": [("guhs:chests/fossielmijn_opgraving", 0.12), ("guhs:chests/fossielmijn_mijn", 0.12)],
    "pteroguh": [("guhs:chests/fossielmijn_mijn", 0.12), ("guhs:chests/onderwol/grotschilderingen", 0.15)],
    "stokstaartje": [("guhs:chests/onderwol/verlaten_guhmijn", 0.15), ("guhs:chests/onderwol/grotschilderingen", 0.12)],
}

# key -> (Dutch, English)
TEKSTEN = {
    f"block.guhs.{BLOK}": ("Fossiel-broedmachine", "Fossil Hatchery"),
    f"block.guhs.{BLOK}.lore": ("Stop er een fossiel en kaasknabbels in: onder de glazen stolp broedt hij er een ei uit. Na een hele dag kruipt er een oerguh uit!",
                                "Put a fossil and cheese nibbles in: under the glass bell it hatches an egg. After a whole day an ancient guh crawls out!"),
    f"block.guhs.{BLOK}.lore.kracht": ("%s vadskracht zolang hij broedt, %s kaasknabbels per ei", "%s vadskracht while it hatches, %s cheese nibbles per egg"),
    T + "al_een": ("Er ligt al een fossiel onder de stolp, vadsje. Eén ei tegelijk!", "There is already a fossil under the bell, vadsje. One egg at a time!"),
    T + "uitgekomen": ("Krak! Uit het oer-ei kruipt een baby-%s. Hij piept meteen naar jou: VAHOEG!",
                       "Crack! A baby %s crawls out of the ancient egg. It squeaks at you right away: VAHOEG!"),
    T + "status.leeg": ("De stolp is leeg. Stop er een fossiel in (en 16 kaasknabbels).", "The bell is empty. Put a fossil in (and 16 cheese nibbles)."),
    T + "status.broedt": ("Het ei van een %s broedt: %s%%", "The egg of a %s is hatching: %s%%"),
    T + "status.broedt_slaapt": ("Het ei van een %s staat stil op %s%%: geen vadskracht!", "The egg of a %s is paused at %s%%: no vadskracht!"),
    T + "status.knabbels": ("Een fossiel van een %s. Nog kaasknabbels nodig: %s van de %s.", "A fossil of a %s. Still needs cheese nibbles: %s of %s."),
    T + "status.kracht": ("Een fossiel van een %s, klaar om te broeden. Nu nog %s vadskracht!", "A fossil of a %s, ready to hatch. Now %s vadskracht!"),
    T + "mammoet_wol": ("%s schudt zijn vacht uit: wol voor jou!", "%s shakes its coat: wool for you!"),
    T + "stokstaartje_ziet": ("%s staat op zijn achterpootjes en wijst: een %s, %s, %s blokken verderop!",
                              "%s stands on its hind paws and points: a %s, %s, %s blocks away!"),
    T + "richting.noord": ("naar het noorden", "to the north"),
    T + "richting.zuid": ("naar het zuiden", "to the south"),
    T + "richting.oost": ("naar het oosten", "to the east"),
    T + "richting.west": ("naar het westen", "to the west"),
    T + "richting.noordoost": ("naar het noordoosten", "to the northeast"),
    T + "richting.noordwest": ("naar het noordwesten", "to the northwest"),
    T + "richting.zuidoost": ("naar het zuidoosten", "to the southeast"),
    T + "richting.zuidwest": ("naar het zuidwesten", "to the southwest"),
    # the fossils
    "item.guhs.fossiel_mammoet": ("Mammoet-guhfossiel", "Mammoth Guh Fossil"),
    "item.guhs.fossiel_mammoet.lore": ("Een stukje slagtand in steen. Diep onder de wol lag ooit een hele wollige guh, njeg.",
                                       "A bit of tusk in stone. Deep under the wool there once lay a whole woolly guh, njeg."),
    "item.guhs.fossiel_tguh": ("T-Guhfossiel", "T-Guh Fossil"),
    "item.guhs.fossiel_tguh.lore": ("Een schedeltje met heel veel tandjes. Gelukkig eet hij alleen kaasknabbels.",
                                    "A little skull with very many teeth. Luckily it only eats cheese nibbles."),
    "item.guhs.fossiel_pteroguh": ("Pteroguhfossiel", "Pteroguh Fossil"),
    "item.guhs.fossiel_pteroguh.lore": ("Een vleugelbotje met een kam. Deze guh vloog al rond voordat er kaas bestond!",
                                        "A wing bone with a crest. This guh flew around before cheese even existed!"),
    "item.guhs.fossiel_stokstaartje": ("Oer-stokstaartjefossiel", "Ancient Meerkat Fossil"),
    "item.guhs.fossiel_stokstaartje.lore": ("Een klein skeletje dat nog steeds rechtop staat. Hij houdt de wacht, ook als fossiel.",
                                            "A tiny skeleton that still stands upright. It keeps watch, even as a fossil."),
    # the oerguhs (Guhdex pages: bonus pages)
    "entity.guhs.guh.mammoetguh": ("Mammoet-guh", "Mammoth Guh"),
    "gui.guhs.guhdex.rarity.mammoetguh": ("Zeldzaamheid: Alleen uit de Fossiel-broedmachine", "Rarity: Only from the Fossil Hatchery"),
    "gui.guhs.guhdex.info.mammoetguh": (
        "Een wollige bruine oerguh met een slurfje en twee slagtanden. Sterk als een vadsbeer: in een guhhuisje doet hij zijn klusjes "
        "twee keer zo snel. Knuffel hem en hij schudt een beetje wol uit zijn vacht. Komt uit een Mammoet-guhfossiel, njeg!",
        "A woolly brown ancient guh with a little trunk and two tusks. Strong as a vads bear: in a guh house it does its chores "
        "twice as fast. Cuddle it and it shakes a bit of wool out of its coat. Hatches from a Mammoth Guh Fossil, njeg!"),
    "entity.guhs.guh.tguh": ("T-Guh", "T-Guh"),
    "gui.guhs.guhdex.rarity.tguh": ("Zeldzaamheid: Alleen uit de Fossiel-broedmachine", "Rarity: Only from the Fossil Hatchery"),
    "gui.guhs.guhdex.info.tguh": (
        "Een groene oerguh met piepkleine armpjes en een dikke staart. De beste waker die er is: met de klus Waken ziet hij "
        "monsters al van heel ver aankomen en jaagt hij ze weg. Kan zijn eigen neus niet aanraken. Komt uit een T-Guhfossiel!",
        "A green ancient guh with teeny tiny arms and a thick tail. The best watcher there is: with the Watch chore it spots "
        "monsters coming from very far away and chases them off. Can't touch its own nose. Hatches from a T-Guh Fossil!"),
    "entity.guhs.guh.pteroguh": ("Pteroguh", "Pteroguh"),
    "gui.guhs.guhdex.rarity.pteroguh": ("Zeldzaamheid: Alleen uit de Fossiel-broedmachine", "Rarity: Only from the Fossil Hatchery"),
    "gui.guhs.guhdex.info.pteroguh": (
        "Een blauwgroene oerguh met vleugeltjes en een kam op zijn kop. Woont hij in een guhhuisje, dan vliegt hij wat de klusjes "
        "opleveren naar kisten die veel verder weg staan. Fladder fladder, vads! Komt uit een Pteroguhfossiel.",
        "A teal ancient guh with little wings and a crest on its head. When it lives in a guh house it flies what the chores "
        "bring in to chests that stand much further away. Flap flap, vads! Hatches from a Pteroguh Fossil."),
    "entity.guhs.guh.stokstaartjeguh": ("Oer-stokstaartje-guh", "Ancient Meerkat Guh"),
    "gui.guhs.guhdex.rarity.stokstaartjeguh": ("Zeldzaamheid: Alleen uit de Fossiel-broedmachine", "Rarity: Only from the Fossil Hatchery"),
    "gui.guhs.guhdex.info.stokstaartjeguh": (
        "Een zandkleurige oerguh met donkere vlekjes om zijn ogen. Staat hij stil, dan gaat hij rechtop staan en houdt hij de "
        "wacht. Ziet hij een zeldzame guh in de buurt, dan wijst hij je de weg. Komt uit een Oer-stokstaartjefossiel!",
        "A sand-coloured ancient guh with dark patches around its eyes. When it stands still it stands up and keeps watch. "
        "When it spots a rare guh nearby it shows you the way. Hatches from an Ancient Meerkat Fossil!"),
}

ADV = ["fossiel_gevonden", "fossiel_broedmachine", "fossiel_uitgebroed"] + [f"fossiel_{s}" for s in SOORTEN]

# --- the FTB section: (key, Dutch title, Dutch text, icon, advancement) ---------------------------------------------------
FTB = [
    ("gevonden", "Een fossiel!", "Diep in oude fossielplekken liggen soms &6fossielen&r: in de kisten van de &dFossiel-opgraving&r en de "
     "&dfossielmijn&r van de Barbecuether, bij het &dreuzenguh-fossiel&r, in de &dverlaten guhmijn&r en bij de &dgrotschilderingen&r "
     "onder de wol. Ook de dagvondst van de opgraving is soms een fossiel. Vind er een!", "guhs:fossiel_mammoet", "fossiel_gevonden"),
    ("broedmachine", "De Fossiel-broedmachine", "Maak een &6Fossiel-broedmachine&r. Stop er een fossiel en &616 kaasknabbels&r in en geef "
     "hem &d20 vadskracht&r: onder de glazen stolp draait het ei rond.", "guhs:fossiel_broedmachine", "fossiel_broedmachine"),
    ("uitgebroed", "Krak!", "Na een hele Minecraft-dag broeden kruipt er een baby-oerguh uit het ei. Hij is meteen van jou!",
     "minecraft:sniffer_egg", "fossiel_uitgebroed"),
    ("mammoet", "Mammoet-guh", "Broed een &6Mammoet-guh&r uit. In een guhhuisje doet hij zijn klusjes twee keer zo snel, en knuffelen "
     "geeft wol.", "guhs:fossiel_mammoet", "fossiel_mammoet"),
    ("tguh", "T-Guh", "Broed een &6T-Guh&r uit. Geef hem de klus &dWaken&r: hij ziet monsters van heel ver aankomen.",
     "guhs:fossiel_tguh", "fossiel_tguh"),
    ("pteroguh", "Pteroguh", "Broed een &6Pteroguh&r uit. Woont hij in een guhhuisje, dan vliegt hij de spullen naar kisten tot ver om "
     "het huisje heen.", "guhs:fossiel_pteroguh", "fossiel_pteroguh"),
    ("stokstaartje", "Oer-stokstaartje-guh", "Broed een &6Oer-stokstaartje-guh&r uit. Neem hem mee: hij gaat rechtop staan en wijst je "
     "zeldzame guhs in de buurt aan.", "guhs:fossiel_stokstaartje", "fossiel_stokstaartje"),
]
FTB_EN = {
    "gevonden": ("A fossil!", "Deep in old fossil places there are sometimes &6fossils&r: in the chests of the &dFossil Dig&r and the "
                 "&dfossil mine&r of the Barbecuether, at the &dgiant guh fossil&r, in the &dabandoned guh mine&r and at the &dcave "
                 "paintings&r under the wool. The dig's daily find is sometimes a fossil too. Find one!"),
    "broedmachine": ("The Fossil Hatchery", "Make a &6Fossil Hatchery&r. Put a fossil and &616 cheese nibbles&r in and give it &d20 "
                     "vadskracht&r: under the glass bell the egg turns round."),
    "uitgebroed": ("Crack!", "After a whole Minecraft day of hatching a baby ancient guh crawls out of the egg. It is yours right away!"),
    "mammoet": ("Mammoth Guh", "Hatch a &6Mammoth Guh&r. In a guh house it does its chores twice as fast, and cuddling gives wool."),
    "tguh": ("T-Guh", "Hatch a &6T-Guh&r. Give it the &dWatch&r chore: it spots monsters coming from very far away."),
    "pteroguh": ("Pteroguh", "Hatch a &6Pteroguh&r. When it lives in a guh house it flies the goods to chests far around the house."),
    "stokstaartje": ("Ancient Meerkat Guh", "Hatch an &6Ancient Meerkat Guh&r. Take it along: it stands up and points out rare guhs nearby."),
}

# --- the oerguhs' bones ------------------------------------------------------------------------------------------------
_H = [0, 6, -2]            # the guh's head pivot
_STAART = [0, 1.5, 12]     # its tail pivot
_LIJF = [0, 6, 6]          # its body pivot
# bone -> the bone whose swatch it samples (that one's variant never shows the oer_ bones, so the oerguh textures may paint it)
SWATCH_VAN = {
    "oer_mammoet_slurf": "bever_staart",
    "oer_mammoet_slagtanden": "bever_tanden",
    "oer_mammoet_wol": "wolk_wolkje",
    "oer_tguh_armpjes": "balto_staart",
    "oer_tguh_staart": "balto_staart",
    "oer_ptero_vleugel_links": "pinguh_vleugel_links",
    "oer_ptero_vleugel_rechts": "pinguh_vleugel_links",
    "oer_ptero_kam": "balto_staart_punt",
}
# bone -> (parent, pivot, [(origin, size, inflate)])
OER_BONES = {
    "oer_mammoet_slurf": ("head", _H, [([-1.3, 2.4, -14.0], [2.6, 2.4, 2.2], 0), ([-1.1, 0.7, -14.6], [2.2, 2.0, 1.8], 0),
                                       ([-1.0, 0.4, -15.9], [2.0, 1.0, 1.4], 0)]),
    "oer_mammoet_slagtanden": ("head", _H, [([-4.8, 2.6, -13.8], [1.0, 1.0, 2.8], 0), ([-4.8, 3.3, -15.4], [1.0, 1.8, 1.0], 0),
                                            ([3.8, 2.6, -13.8], [1.0, 1.0, 2.8], 0), ([3.8, 3.3, -15.4], [1.0, 1.8, 1.0], 0)]),
    "oer_mammoet_wol": ("body", _LIJF, [([-7.0, 9.4, 0.0], [14.0, 2.2, 11.6], 0.1), ([-7.4, 3.0, 0.6], [0.9, 6.6, 10.4], 0.1),
                                        ([6.5, 3.0, 0.6], [0.9, 6.6, 10.4], 0.1)]),
    "oer_tguh_armpjes": ("body", _LIJF, [([7.0, 3.2, -1.6], [1.3, 1.3, 3.0], 0), ([7.0, 2.3, -2.2], [1.3, 0.9, 0.9], 0),
                                         ([-8.3, 3.2, -1.6], [1.3, 1.3, 3.0], 0), ([-8.3, 2.3, -2.2], [1.3, 0.9, 0.9], 0)]),
    "oer_tguh_staart": ("tail", _STAART, [([-2.0, 2.4, 11.0], [4.0, 3.6, 6.0], 0), ([-1.4, 2.8, 17.0], [2.8, 2.7, 5.0], 0),
                                          ([-0.8, 3.2, 22.0], [1.6, 1.7, 4.0], 0)]),
    "oer_ptero_vleugel_links": ("body", [6.2, 9.0, 5.0], [([6.2, 8.7, 1.5], [9.0, 0.6, 7.0], 0), ([15.2, 8.7, 2.5], [3.0, 0.6, 4.0], 0)]),
    "oer_ptero_vleugel_rechts": ("body", [-6.2, 9.0, 5.0], [([-15.2, 8.7, 1.5], [9.0, 0.6, 7.0], 0), ([-18.2, 8.7, 2.5], [3.0, 0.6, 4.0], 0)]),
    "oer_ptero_kam": ("head", _H, [([-0.6, 14.6, -7.0], [1.2, 2.2, 9.0], 0), ([-0.5, 15.8, 1.5], [1.0, 1.4, 3.0], 0)]),
}

# --- colours ----------------------------------------------------------------------------------------------------------
MAMMOET, MAMMOET_D, IVOOR = (122, 80, 50), (84, 54, 36), (238, 228, 200)
TGUH, TGUH_L, TGUH_D = (100, 158, 82), (182, 208, 132), (64, 112, 58)
PTERO, PTERO_L, VLIES, KAM = (78, 146, 160), (176, 214, 214), (232, 156, 112), (226, 106, 64)
ZAND, ZAND_L, ZAND_D, VLEK = (204, 168, 118), (236, 214, 172), (150, 116, 80), (64, 46, 36)


def _geo_pad(h):
    return os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json")


def bones(h):
    """The oer_ bones in guh.geo.json (taken out and put back, so running twice changes nothing)."""
    pad = _geo_pad(h)
    with open(pad, encoding="utf-8") as f:
        doc = json.load(f)
    geo = doc["minecraft:geometry"][0]
    geo["bones"] = [b for b in geo["bones"] if not b["name"].startswith("oer_")]
    uv_van = {}
    for b in geo["bones"]:
        if b["name"] in SWATCH_VAN.values() and b.get("cubes"):
            uv_van[b["name"]] = b["cubes"][0]["uv"]["north"]["uv"]
    for naam, (parent, pivot, cubes) in OER_BONES.items():
        uv = uv_van[SWATCH_VAN[naam]]
        bone = {"name": naam, "parent": parent, "pivot": pivot, "cubes": []}
        for origin, size, inflate in cubes:
            cube = {"origin": origin, "size": size, "uv": {k: {"uv": list(uv), "uv_size": [8, 8]}
                                                           for k in ("north", "south", "east", "west", "up", "down")}}
            if inflate:
                cube["inflate"] = inflate
            bone["cubes"].append(cube)
        geo["bones"].append(bone)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(doc, f, indent=2, ensure_ascii=False)
        f.write("\n")


# --- the oerguh textures ----------------------------------------------------------------------------------------------
def _fabric(kleur, rng, ruis=12):
    import make_guh_variants as v
    return v.fabric(kleur, rng, ruis)


def _wol(rng):
    a = _fabric(MAMMOET_D, rng, 14)
    px = a.shape[0]
    for _ in range(70):                                   # shaggy strands hanging down
        x, y = rng.integers(0, px), rng.integers(0, px - 4)
        a[y:y + 4, x] = np.clip(a[y:y + 4, x] + rng.integers(-22, 26), 0, 255)
    return a


def _slurf(rng):
    a = _fabric(MAMMOET, rng, 8)
    a[::5, :] = a[::5, :] * 0.8                           # the wrinkles of the trunk
    return np.clip(a, 0, 255)


def _tand(rng):
    a = _fabric(IVOOR, rng, 5)
    a[:3, :] = (252, 248, 236)
    return a


def _schubben(kleur, rng):
    a = _fabric(kleur, rng, 8)
    px = a.shape[0]
    for y in range(0, px, 4):
        a[y, :] = a[y, :] * 0.8
        for x in range((y // 4 % 2) * 2, px, 4):
            a[y:y + 4, x] = a[y:y + 4, x] * 0.86
    return np.clip(a, 0, 255)


def _vlies(rng):
    a = _fabric(VLIES, rng, 8)
    px = a.shape[0]
    for x in range(0, px, 8):                             # the long finger bones through the membrane
        a[:, x:x + 2] = a[:, x:x + 2] * 0.72
    a[:3, :] = PTERO                                      # the leading edge in body colour
    return np.clip(a, 0, 255)


def _paint(base, g, vacht, licht, donker, fluff=0.04):
    """The fur in vacht, a lighter belly and chest, darker paws and ears (like the Beverguh)."""
    from features import guhpolder_tex as gt
    p = gt.Painter(base, vacht, fluff=fluff)
    for c in gt._faces(g, "body"):
        p.rect(c["down"], licht)
        p.rect(c["north"], licht, rows=(0.4, 1.0))
    for deel in ("ear_left", "ear_right", "leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for c in gt._faces(g, deel):
            for k, fr in c.items():
                if not k.startswith("_"):
                    p.rect(fr, donker)
    return p


def _plak(p, g, rng, stukken):
    from features import bio_dieren_modellen as bm
    for bone, swatch in stukken:
        rect = bm._swatch_rect(g, SWATCH_VAN[bone] if bone in SWATCH_VAN else bone)
        bm._plak(p.a, rect, swatch(rng))


def _ooglapjes(p, base):
    """The meerkat's dark patches around its eyes (only on fur: the eyes themselves stay)."""
    import make_sleep_eyes as mse
    boxes = mse.eye_boxes(base, mse.fur_colour(base))
    mask = np.zeros(p.fur.shape, bool)
    for x0, y0, x1, y1 in boxes:
        mask[max(0, y0 - 5):y1 + 6, max(0, x0 - 4):x1 + 5] = True
    p.recolour(mask, VLEK)


def oerguh(base, g, variant):
    rng = np.random.default_rng(18000 + sorted(v for v, *_ in SOORTEN.values()).index(variant))
    if variant == "mammoetguh":
        p = _paint(base, g, MAMMOET, (150, 106, 72), MAMMOET_D, fluff=0.1)
        _plak(p, g, rng, [("oer_mammoet_slurf", _slurf), ("oer_mammoet_slagtanden", _tand), ("oer_mammoet_wol", _wol)])
    elif variant == "tguh":
        p = _paint(base, g, TGUH, TGUH_L, TGUH_D, fluff=0.03)
        _plak(p, g, rng, [("oer_tguh_staart", lambda r: _schubben(TGUH, r))])
    elif variant == "pteroguh":
        p = _paint(base, g, PTERO, PTERO_L, (52, 104, 118), fluff=0.03)
        _plak(p, g, rng, [("oer_ptero_vleugel_links", _vlies), ("oer_ptero_kam", lambda r: _fabric(KAM, r, 8))])
    else:
        p = _paint(base, g, ZAND, ZAND_L, ZAND_D, fluff=0.05)
        _ooglapjes(p, base)
    return Image.fromarray(np.clip(p.a, 0, 255).astype(np.uint8))


def textures(h):
    from features import bio_dieren_modellen as bm
    with open(_geo_pad(h), encoding="utf-8") as f:
        g = json.load(f)["minecraft:geometry"][0]
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    for variant, *_ in SOORTEN.values():
        img = oerguh(base, g, variant)
        h.save(img, "entity", f"guh_{variant}.png")
        vul = VLEK if variant == "stokstaartjeguh" else None
        h.save(bm.slaap(img, base, (236, 220, 190, 255), vul=vul), "entity", "guh_slaap", f"guh_{variant}.png")


# --- the broedmachine and the fossils ---------------------------------------------------------------------------------
BASIS, ACCENT = (214, 150, 92), (120, 196, 214)


def _glas():
    img = Image.new("RGBA", (16, 16), (196, 232, 246, 64))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(236, 250, 255, 200))
    d.line([3, 2, 3, 11], fill=(255, 255, 255, 150))      # a shine
    d.line([4, 2, 4, 5], fill=(255, 255, 255, 120))
    return img


def _dop():
    img = Image.new("RGBA", (16, 16), (232, 186, 96, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(176, 128, 56, 255))
    d.point([(4, 4), (11, 4), (4, 11), (11, 11)], fill=(255, 226, 150, 255))
    return img


def _machine_model(staat):
    t = lambda k: f"guhs:block/{BLOK}_{k}"
    f = lambda tex, uv, cull=None: {"texture": tex, "uv": uv, **({"cullface": cull} if cull else {})}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
            "textures": {"particle": t("zij"), "voor": t(f"voor_{staat}"), "zij": t("zij"), "boven": t("boven"),
                         "glas": t("glas"), "dop": t("dop")},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 9, 16], "faces": {
                    "north": f("#voor", [0, 2, 16, 11], "north"), "south": f("#zij", [0, 7, 16, 16], "south"),
                    "east": f("#zij", [0, 7, 16, 16], "east"), "west": f("#zij", [0, 7, 16, 16], "west"),
                    "up": f("#boven", [0, 0, 16, 16]), "down": f("#zij", [0, 0, 16, 16], "down")}},
                {"from": [3, 9, 3], "to": [13, 15, 13], "faces": {
                    k: f("#glas", [0, 0, 16, 16] if k in ("up", "down") else [0, 4, 16, 16])
                    for k in ("north", "south", "east", "west", "up")}},
                {"from": [5.5, 15, 5.5], "to": [10.5, 16, 10.5], "faces": {
                    k: f("#dop", [0, 0, 16, 16] if k == "up" else [0, 0, 16, 3]) for k in ("north", "south", "east", "west", "up")}},
            ]}


STEEN, STEEN_D, BOT, BOT_D = (156, 142, 122), (112, 100, 86), (238, 226, 198), (196, 178, 146)


def _fossiel(soort):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 2, 14, 14], fill=STEEN + (255,), outline=STEEN_D + (255,))
    rng = np.random.default_rng(len(soort) * 7 + ord(soort[0]))
    for _ in range(12):
        x, y = int(rng.integers(3, 13)), int(rng.integers(4, 13))
        if img.getpixel((x, y))[:3] == STEEN:
            d.point((x, y), fill=tuple(c - 14 for c in STEEN) + (255,))
    b, bd = BOT + (255,), BOT_D + (255,)
    if soort == "mammoet":                                # a curved tusk
        d.arc([3, 4, 13, 14], 180, 330, fill=bd, width=3)
        d.arc([3, 4, 13, 14], 180, 330, fill=b, width=2)
        d.point((12, 7), fill=b)
    elif soort == "tguh":                                 # a skull with a row of teeth
        d.polygon([(3, 6), (12, 6), (13, 9), (3, 10)], fill=b, outline=bd)
        d.point([(5, 7), (6, 7)], fill=STEEN_D + (255,))  # the eye socket
        for x in range(4, 13, 2):
            d.point((x, 11), fill=b)
        d.line([3, 10, 12, 10], fill=bd)
    elif soort == "pteroguh":                             # a long wing bone with fingers and the crest
        d.line([3, 11, 12, 5], fill=b, width=1)
        d.line([12, 5, 13, 9], fill=b)
        d.line([8, 8, 10, 11], fill=bd)
        d.polygon([(4, 5), (7, 4), (6, 6)], fill=b, outline=bd)
    else:                                                 # a small upright skeleton
        d.ellipse([6, 3, 9, 6], fill=b, outline=bd)
        d.line([7, 7, 7, 12], fill=b)
        for y in (8, 10):
            d.line([5, y, 9, y], fill=bd)
        d.line([6, 12, 5, 13], fill=b)
        d.line([8, 12, 9, 13], fill=b)
    return img


def build(h):
    from features import vadskracht as vk
    vk.machine(h, BLOK, BASIS, ACCENT)
    h.save(_glas(), "block", f"{BLOK}_glas.png")
    h.save(_dop(), "block", f"{BLOK}_dop.png")
    for staat in vk.STATEN:
        h.w(f"{h.A}/models/block/{BLOK}_{staat}.json", _machine_model(staat))
    h.shaped(BLOK, ["GGG", "GBG", "IKI"], {"G": "minecraft:glass", "B": "minecraft:bone_block", "I": "minecraft:iron_ingot",
                                         "K": "guhs:kaas_knabbels"}, f"guhs:{BLOK}")
    for soort in SOORTEN:
        naam = f"fossiel_{soort}"
        h.save(_fossiel(soort), "item", f"{naam}.png")
        h.item_model(naam)
        h.w(f"{h.D}/loot_table/fossiel/{soort}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}"}]}]})
        for tafel, kans in VINDPLEKKEN[soort]:
            h.w(f"{h.D}/loot_table/fossiel/{soort}__{tafel.split('/')[-1]}.json", {"type": "minecraft:chest", "pools": [{
                "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:random_chance", "chance": kans}],
                "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}"}]}]})
            h.w(f"{h.D}/loot_modifiers/fossiel_{soort}__{tafel.split('/')[-1]}.json", {
                "type": "neoforge:add_table", "table": f"guhs:fossiel/{soort}__{tafel.split('/')[-1]}",
                "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": tafel}]})
    h.add_tag("guhs/tags/item/fossielen", [f"guhs:fossiel_{s}" for s in SOORTEN])
    for a in ADV:
        if a == "fossiel_gevonden":
            crit = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "#guhs:fossielen"}]}}
        elif a == "fossiel_broedmachine":
            crit = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{BLOK}"}]}}
        else:
            crit = {"trigger": "minecraft:impossible"}
        h.w(f"{h.D}/advancement/quest/{a}.json", {"criteria": {"done": crit}})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def ftb(fq):
    plek = {"gevonden": (0, 0), "broedmachine": (2, 0), "uitgebroed": (4, 0), "mammoet": (6, -1.5), "tguh": (6, -0.5),
            "pteroguh": (6, 0.5), "stokstaartje": (6, 1.5)}
    na = {"broedmachine": "gevonden", "uitgebroed": "broedmachine", "mammoet": "uitgebroed", "tguh": "uitgebroed",
          "pteroguh": "uitgebroed", "stokstaartje": "uitgebroed"}
    for key, titel, tekst, icoon, adv in FTB:
        x, y = plek[key]
        fq.q(f"fossiel_{key}", titel, tekst, icoon, [fq.adv(adv)], rewards=(("guhs:kaas_knabbels", 32 if key == "uitgebroed" else 16),),
             deps=[f"fossiel_{na[key]}"] if key in na else [], x=x, y=y, shape="gear" if key == "uitgebroed" else None,
             xp=200 if key == "uitgebroed" else 100)


def _items(h):
    a = os.path.join(h.A, "items")
    os.makedirs(a, exist_ok=True)
    for naam in [BLOK] + [f"fossiel_{s}" for s in SOORTEN]:
        with open(os.path.join(a, naam + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}}, f, indent=2)
            f.write("\n")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    bones(h)
    textures(h)
    _items(h)
    en = {k: en for k, (nl, en) in TEKSTEN.items()}
    with open(os.path.join("tools", "lang", "en", "c217_fossiel.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    ftb_en = {"ftb.guhs_techniek.section.fossiel": "Fossils and ancient guhs"}
    for key, (titel, tekst) in FTB_EN.items():
        ftb_en[f"ftb.guhs_techniek.q.fossiel_{key}.title"] = titel
        ftb_en[f"ftb.guhs_techniek.q.fossiel_{key}.desc"] = tekst
    with open(os.path.join("tools", "lang", "en", "c217_fossiel_ftb.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(ftb_en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"fossiel: broedmachine, {len(SOORTEN)} fossils, {len(OER_BONES)} oer_ bones, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
