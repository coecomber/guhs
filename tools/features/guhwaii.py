"""
3.0 (Guhverhalen), slice guhwaii: Lilo & Stitch op Guhwai'i - het eiland, het ohana-huisje, de capsule en 626-guh (DESIGN_30 §5,
without the surf/hula minigames: those are guhwaii_spellen). Java: nl.juiced.guhs.feature.guhwaii.

  build(h)     textures (guhwaii_tex), the blocks and items (palm with a face, kokosnoot, kokosmelk, tropical flowers + pots,
               Schilly-eitjes, vadsigheid-scanner, poster + painting, rommeltjes, ukelele), the Guhwai'i biome's features
               (palms, flowers, the kaaskoraal reef, seagrass, turtle nests) and its surface, the two buildings (guhwaii_bouw:
               the stilt house guhwaii_ohana on the beach, the capsule guhwaii_capsule on the hill), the looks (guhwaii_modellen:
               Lilo-guh, Nani-guh, the 626-guh's fur), all texts (Dutch, also in en_us), the advancements (tab Guhverhalen) and
               the test rooms; a self-check at the end
  ftb(fq)      the section "Ohana op Guhwai'i" of the chapter guhs_verhalen
  BONES / variants / clothes / CLOTHES / icons / animations: the 626-guh's bones and fur, the four outfit pieces
               (hula-rokje, bloemenkrans, Stitch-oren + antennes, surfplankje), the ukelele emote
"""
import json
import os
import random
import zipfile

import numpy as np

from features import guhwaii_bouw as bouw
from features import guhwaii_modellen as modellen
from features import guhwaii_tex as tex
from features import knuffeldal_wereld as kw
from features import spelen
from features import verhaal
from features import verhaal_wereld

FTB_SECTION = "Ohana op Guhwai'i"
MODULE = "guhwaii"
TAB = "verhalen"

# =====================================================================================================================
# bones: the 626-guh (variant) and the four outfit pieces
# =====================================================================================================================
_B = [0, 6, 6]
_H = [0, 6, -2]
BONES = dict(modellen.BONES)
BONES.update({
    # the hula skirt: a band of flowers round the belly and long grass blades down to the paws
    "outfit_guhwaii_rokje": ("body", _B, "guhwaii_rokje_gras", [([-7.0, 1.2, -3.0], [14.0, 4.6, 15.0], 0)]),
    "outfit_guhwaii_rokje_band": ("body", _B, "guhwaii_rokje_band", [([-7.2, 5.4, -3.2], [14.4, 1.4, 15.4], 0)]),
    # the lei: flowers all round the neck (a ring at the front of the body) and a garland under the chin
    "outfit_guhwaii_krans": ("body", _B, "guhwaii_krans", [([-6.6, 10.6, -1.8], [13.2, 1.6, 1.8], 0), ([-7.6, 2.4, -1.8], [1.6, 8.4, 1.8], 0),
                                                        ([6.0, 2.4, -1.8], [1.6, 8.4, 1.8], 0)]),
    "outfit_guhwaii_krans_voor": ("head", _H, "guhwaii_krans", [([-5.6, 0.2, -11.8], [11.2, 1.8, 1.8], 0), ([-1.2, -1.4, -12.0], [2.4, 1.8, 1.8], 0)]),
    # the surfboard strapped on the back: a long rounded board with a fin
    "outfit_guhwaii_surfplank": ("body", _B, "guhwaii_surfplank", [([-2.6, 11.6, -1.0], [5.2, 0.7, 13.5], 0), ([-1.8, 11.6, -2.4], [3.6, 0.7, 1.4], 0),
                                                                ([-1.8, 11.6, 12.5], [3.6, 0.7, 1.2], 0)]),
    "outfit_guhwaii_surfplank_vin": ("body", _B, "guhwaii_surfplank_vin", [([-0.3, 12.3, 10.0], [0.6, 1.8, 2.2], 0),
                                                                        ([-6.9, 6.0, 4.0], [13.8, 0.6, 1.2], 0)]),
    # the antennes of the Stitch-oren (on the head)
    "outfit_oren_guhwaii_antenne": ("head", _H, "guhwaii_antenne", [([-3.2, 15.0, -6.2], [0.7, 4.2, 0.7], 0), ([2.5, 15.0, -6.2], [0.7, 4.2, 0.7], 0),
                                                                  ([-3.6, 19.0, -6.6], [1.5, 1.5, 1.5], 0), ([2.1, 19.0, -6.6], [1.5, 1.5, 1.5], 0)]),
})
# the Stitch-oren: big blue ears over the guh ears (a notch at the top), pink inside
BONES.update(spelen.oren("outfit_oren_guhwaii", "guhwaii_oren", cubes=[([4.2, 9.4, -6.45], [11.4, 8.2, 1.3], 0), ([14.4, 12.4, -6.45], [2.6, 4.2, 1.3], 0)]))
BONES.update({"outfit_oren_guhwaii_binnen_links": ("ear_left", spelen.EAR_LEFT_PIVOT, "guhwaii_oren_binnen", [([5.8, 10.8, -6.75], [8.4, 5.4, 0.3], 0)]),
              "outfit_oren_guhwaii_binnen_rechts": ("ear_right", spelen.EAR_RIGHT_PIVOT, "guhwaii_oren_binnen", [([-14.2, 10.8, -6.75], [8.4, 5.4, 0.3], 0)])})
CLOTHES = ["guhwaii_hularokje", "guhwaii_bloemenkrans", "guhwaii_stitchoren", "guhwaii_surfplankje"]


def variants(rng, v):
    """The 626-guh: blue fur; his bones' swatches (the ears, the extra arms). build() paints the rest of his look."""
    return {"stitch626": (modellen.BLAUW, modellen.variant_painters(rng, v))}


def animations():
    return modellen.animations()


def _gras(rng, v):
    """Grass blades: vertical stripes of straw green and sun yellow."""
    a = np.zeros((32, 32, 3), np.float32)
    for x in range(32):
        c = (128, 196, 70) if (x // 2) % 3 else (214, 206, 104)
        a[:, x] = c
    return np.clip(a + rng.normal(0, 6, (32, 32, 1)), 0, 255)


def _bloemenband(rng, v, basis=(90, 170, 80)):
    a = v.fabric(basis, rng, 6)
    for i, x in enumerate(range(2, 32, 6)):
        c = [(255, 120, 180), (252, 250, 240), (255, 214, 70)][i % 3]
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (2, 1), (0, -1), (1, 2)):
            if 0 <= x + dx < 32:
                a[max(0, min(31, 14 + dy)), x + dx] = c
                a[max(0, min(31, 4 + dy + (i % 2) * 18)), (x + dx + 3) % 32] = c
    return a


def _krans(rng, v):
    """Round flowers all over: pink, white, yellow, with a darker heart."""
    a = v.fabric((255, 150, 200), rng, 6)
    kleuren = [(255, 120, 180), (252, 250, 240), (255, 214, 70), (255, 170, 90)]
    for y in range(0, 32, 5):
        for x in range((y // 5 % 2) * 3, 32, 6):
            c = kleuren[(x // 6 + y // 5) % 4]
            a[max(0, y - 1):y + 2, max(0, x - 1):x + 2] = c
            a[y, x] = (200, 60, 120)
    return a


def _surfplank(rng, v):
    a = v.fabric((70, 206, 214), rng, 4)
    a[14:18, :] = (252, 250, 240)
    a[15:17, :] = (255, 120, 180)
    for y in range(22, 30):
        for x in range(10, 22):
            if (x - 16) ** 2 + (y - 26) ** 2 < 12:
                a[y, x] = (255, 120, 180)
    return a


def clothes(rng, v):
    return {
        "guhwaii_hularokje": {"guhwaii_rokje_gras": lambda: _gras(rng, v), "guhwaii_rokje_band": lambda: _bloemenband(rng, v, (250, 150, 200))},
        "guhwaii_bloemenkrans": {"guhwaii_krans": lambda: _krans(rng, v)},
        "guhwaii_stitchoren": {"guhwaii_oren": lambda: v.fabric(modellen.BLAUW, rng, 6), "guhwaii_oren_binnen": lambda: v.fabric(modellen.OOR_ROZE, rng, 5),
                               "guhwaii_antenne": lambda: v.fabric(modellen.DONKERBLAUW, rng, 5)},
        "guhwaii_surfplankje": {"guhwaii_surfplank": lambda: _surfplank(rng, v), "guhwaii_surfplank_vin": lambda: v.fabric((252, 250, 240), rng, 4)},
    }


ROKJE_ICON = ["................", "................", "..pwpypwpypwpy..", "..pppppppppppp..", "..gygggygggyggg.", "..gygggygggyggg.",
              ".ggyggggyggggyg.", ".ggyggggyggggyg.", ".gygggyggggyggg.", "gggyggggygggyggg", "gggyggggygggyggg", "g.g.g.g.g.g.g.g.",
              "................", "................", "................", "................"]
KRANS_ICON = ["................", "....pwyppwyp....", "..wp........yw..", ".yp..........py.", ".p............p.", "w..............w",
              "p..............p", "y..............y", ".p............p.", ".wy..........pw.", "..pyw......ypw..", "....pwypywpy....",
              "......pwyp......", ".......py.......", "................", "................"]
OREN_ICON = ["................", "...d........d...", "...dd......dd...", "....d......d....", "bb..d......d..bb", "bbb..........bbb",
             "bpbb........bbpb", "bppbb......bbppb", "bpppbb....bbpppb", "bpppbb....bbpppb", ".bppbb....bbppb.", ".bbbb......bbbb.",
             "................", "................", "................", "................"]
SURF_ICON = ["............tt..", "...........tttt.", "..........tttwt.", ".........tttwtt.", "........tttwtt..", ".......tttwtt...",
             "......tppwtt....", ".....tppptt.....", "....tttwtt......", "...tttwtt.......", "..tttwtt........", ".tttwtt.........",
             ".ttwtt..........", "..tt............", "................", "................"]


def icons(ic):
    return {
        "guhwaii_hularokje": ic.icon(ROKJE_ICON, {"g": (128, 196, 70), "y": (214, 206, 104), "p": (255, 120, 180), "w": (252, 250, 240)}),
        "guhwaii_bloemenkrans": ic.icon(KRANS_ICON, {"p": (255, 120, 180), "w": (252, 250, 240), "y": (255, 214, 70)}),
        "guhwaii_stitchoren": ic.icon(OREN_ICON, {"b": modellen.BLAUW, "p": modellen.OOR_ROZE, "d": modellen.DONKERBLAUW}),
        "guhwaii_surfplankje": ic.icon(SURF_ICON, {"t": (70, 206, 214), "w": (252, 250, 240), "p": (255, 120, 180)}),
    }


# =====================================================================================================================
# texts (Dutch, also in en_us)
# =====================================================================================================================
LANG = {
    # --- the story guh and the characters ---
    "entity.guhs.guh.stitch626": "626-guh",
    "entity.guhs.stitch626": "626-guh",
    "gui.guhs.guhdex.rarity.stitch626": "Zeldzaamheid: Eén per speler, na het verhaal van Guhwai'i",
    "gui.guhs.guhdex.info.stitch626": "Experiment 626! Een blauwe alien-guh met grote oren, vier pootjes en een extra paar armpjes. Hij werd "
                                      "gemaakt om TE VADS te zijn... maar een guh kan nooit te vads zijn: de vadsigheid-scanner slaat door tot "
                                      "ONBEREKENBAAR VAHOEG. Hij maakt soms rommel, maar hij doet nooit iemand pijn. Hij klimt tegen muren op, "
                                      "hangt ondersteboven aan het plafond, draagt met zijn extra armpjes twee dingen tegelijk (dubbel sjouwen bij "
                                      "klusjes!) en speelt ukelele: \"Aloha, njeg!\" Na het verhaal van Lilo-guh eenmalig tembaar met een kaasknabbel.",
    "entity.guhs.guh_npc.lilo_guh": "Lilo-guh",
    "gui.guhs.guhdex.rarity.lilo_guh": "Zeldzaamheid: Eén in elk paalhuisje op Guhwai'i",
    "gui.guhs.guhdex.info.lilo_guh": "Een klein, eigenwijs guhmeisje met lang zwart haar, een rode jurk met witte blaadjes en een roze hibiscus "
                                     "achter haar oor. Ze houdt van Elvis-guhmuziek, surfen en rare blauwe guhs. Zij adopteerde 626-guh uit het asiel, "
                                     "want: ohana betekent familie!",
    "entity.guhs.guh_npc.nani_guh": "Nani-guh",
    "gui.guhs.guhdex.rarity.nani_guh": "Zeldzaamheid: Eén in elk paalhuisje op Guhwai'i",
    "gui.guhs.guhdex.info.nani_guh": "Lilo-guhs grote zus. Ze zorgt voor iedereen: voor Lilo-guh, voor het huisje op palen en nu ook voor 626-guh "
                                     "(en zijn rommel). Een beetje streng, maar heel erg lief. Ze draagt een bloem in haar knotje.",
    # --- the structures ---
    "structure.guhs.guhwaii_ohana": "Het paalhuisje van Lilo en Nani",
    "structure.guhs.guhwaii_ohana.tooltip": "Het paalhuisje van Lilo-guh en Nani-guh op het strand van Guhwai'i, met het guh-asiel",
    "structure.guhs.guhwaii_capsule": "De neergestorte capsule",
    "structure.guhs.guhwaii_capsule.tooltip": "De capsule van 626-guh op de heuvel van Guhwai'i, met de vadsigheid-scanner",
    # --- blocks and items ---
    "block.guhs.guhwaii_palm_stam": "Guh-palmstam",
    "block.guhs.guhwaii_palm_gezicht": "Guh-palmstam met gezichtje",
    "block.guhs.guhwaii_palm_gezicht.lore": "Elke guh-palm heeft een gezichtje. Klik erop: hij knipoogt! Njeg.",
    "block.guhs.guhwaii_palm_blad": "Guh-palmblad",
    "block.guhs.guhwaii_palm_blad.lore": "Lange wuivende bladeren. Er groeien kokosnoten onder!",
    "block.guhs.guhwaii_palm_kiemplant": "Kiemende kokosnoot",
    "block.guhs.guhwaii_palm_planken": "Guh-palmplanken",
    # the guh-palm wood set (1.2.8)
    "block.guhs.guhwaii_palm_gestript": "Gestripte guh-palmstam",
    "block.guhs.guhwaii_palm_trap": "Guh-palmtrap",
    "block.guhs.guhwaii_palm_plaat": "Guh-palmplaat",
    "block.guhs.guhwaii_palm_hek": "Guh-palmhek",
    "block.guhs.guhwaii_palm_poort": "Guh-palmpoort",
    "block.guhs.guhwaii_palm_deur": "Guh-palmdeur",
    "block.guhs.guhwaii_palm_luik": "Guh-palmluik",
    "block.guhs.guhwaii_palm_bord": "Guh-palmbord",
    "block.guhs.kokosnoot": "Kokosnoot",
    "item.guhs.kokosnoot": "Kokosnoot",
    "item.guhs.kokosnoot.lore": "Eet hem op (krak, slurp!), geef hem aan je guh, of plant hem in het zand: dan groeit er een guh-palm.",
    "item.guhs.kokosmelk": "Kokosmelk",
    "item.guhs.kokosmelk.lore": "Koel en zoet, uit een echte Guhwai'i-kokosnoot. Je voelt je meteen een beetje beter. Het flesje krijg je terug.",
    "item.guhs.guhwaii_ukelele": "Reserve-ukelele van 626-guh",
    "item.guhs.guhwaii_ukelele.lore": "Tokkel erop (rechtsklik): alle guhs om je heen gaan dansen. En een 626-guh speelt mee!",
    "block.guhs.roze_hibiscus": "Roze hibiscus",
    "block.guhs.roze_hibiscus.lore": "De mooiste bloem van Guhwai'i. Lilo-guh draagt er een achter haar oor.",
    "block.guhs.guhwaii_plumeria": "Plumeria",
    "block.guhs.guhwaii_plumeria.lore": "Witte sterretjesbloemen met een zonnig hartje. Ze ruiken naar vakantie.",
    "block.guhs.guhwaii_paradijsbloem": "Paradijsvogelbloem",
    "block.guhs.guhwaii_paradijsbloem.lore": "Een oranje bloem die eruitziet als een vogeltje. Tjilp? Njeg!",
    "block.guhs.guhwaii_orchidee": "Guhwai'i-orchidee",
    "block.guhs.guhwaii_orchidee.lore": "Een paarse orchidee aan een sierlijk boogje.",
    "block.guhs.potted_roze_hibiscus": "Roze hibiscus in een pot",
    "block.guhs.potted_guhwaii_plumeria": "Plumeria in een pot",
    "block.guhs.potted_guhwaii_paradijsbloem": "Paradijsvogelbloem in een pot",
    "block.guhs.potted_guhwaii_orchidee": "Orchidee in een pot",
    "block.guhs.schilly_eitjes": "Schilly-eitjes",
    "block.guhs.schilly_eitjes.lore": "Ze liggen warm in het zand. Straks: knak, knak... kleine Schillytjes! Er lopen nooit pootjes over heen, beloofd.",
    "block.guhs.vadsigheid_scanner": "Vadsigheid-scanner",
    "block.guhs.vadsigheid_scanner.lore": "Uit de capsule van 626-guh. Zet een guh op de plaat en klik: VADSIGHEIDSNIVEAU... ONBEREKENBAAR VAHOEG!",
    "block.guhs.vadsigheid_poster": "Poster: Onberekenbaar vahoeg",
    "block.guhs.vadsigheid_poster.lore": "Het beroemde plaatje: de meter slaat door tot ONBEREKENBAAR VAHOEG. Hang hem aan de muur!",
    "block.guhs.guhwaii_rommeltje": "Rommeltje van 626",
    "block.guhs.guhwaii_rommeltje.lore": "Alleen maar rommel, niks kapot! Klik erop om op te ruimen.",
    "painting.guhs.vadsigheidsniveau.title": "Onberekenbaar vahoeg",
    "painting.guhs.vadsigheidsniveau.author": "Experiment 626",
    # --- the scanner ---
    "gui.guhs.guhwaii.scanner.titel": "VADSIGHEID-SCANNER",
    "gui.guhs.guhwaii.scanner.niveau": "VADSIGHEIDSNIVEAU",
    "gui.guhs.guhwaii.scanner.niveau.1": "een beetje vads",
    "gui.guhs.guhwaii.scanner.niveau.2": "vads",
    "gui.guhs.guhwaii.scanner.niveau.3": "heel vads",
    "gui.guhs.guhwaii.scanner.niveau.4": "VAHOEG",
    "gui.guhs.guhwaii.scanner.meten": "Meten",
    "gui.guhs.guhwaii.scanner.onberekenbaar.kort": "ONBEREKENBAAR VAHOEG!!!",
    "gui.guhs.guhwaii.scanner.onberekenbaar": "ONBEREKENBAAR VAHOEG",
    "gui.guhs.guhwaii.scanner.meting.1": "Wangetjes: 100% knijpbaar",
    "gui.guhs.guhwaii.scanner.meting.2": "Buikje: perfect rond",
    "gui.guhs.guhwaii.scanner.meting.3": "Knuffelwaarde: oneindig",
    "gui.guhs.guhwaii.scanner.meting.4": "Te vads? Bestaat niet. Njeg!",
    "gui.guhs.guhwaii.scanner.njeg": "Njeg!",
    "gui.guhs.guhwaii.scanner.uitslag": "✦ Vadsigheid-scanner: het VADSIGHEIDSNIVEAU van %s is... ONBEREKENBAAR VAHOEG! De meter is doorgeslagen. ✦",
    "gui.guhs.guhwaii.scanner.leeg": "Zet eerst een guh op de scanner! (Of houd een opgepakte guh vast, of neem een guh mee.)",
    "gui.guhs.guhwaii.scanner.bezig": "De scanner is nog aan het meten... biep biep!",
    "gui.guhs.guhwaii.beschermd": "Dit is het huisje van Lilo-guh en Nani-guh (of de capsule van 626): hier bouwen of slopen we niet, njeg!",
    # --- 626-guh ---
    "gui.guhs.guhwaii.ukelele": "Ukelele",
    "gui.guhs.guhwaii.ukelele.tooltip": "626-guh pakt zijn ukelele en speelt een liedje: \"Aloha, njeg!\"",
    "gui.guhs.guhwaii.ukelele.niet_nu": "Blub? (Hij kan nu even niet spelen. Straks weer!)",
    "gui.guhs.guhwaii.aloha": "Aloha, njeg! ♪ Tokkel tokkel ♪",
    "gui.guhs.guhwaii.kokos_onrijp": "Deze kokosnoot is nog groen. Nog even wachten, njeg!",
    "gui.guhs.guhwaii.eitjes_uit": "Knak, knak! Er komen kleine Schillytjes uit de eitjes. Welkom op Guhwai'i!",
    "gui.guhs.guhwaii.rommel.teller": "Opgeruimd: %s van %s",
    # --- the ohana questline ---
    "gui.guhs.guhwaii.lilo.intro.1": "Aloha! Ik ben Lilo-guh. Welkom op Guhwai'i! Wil je een kokosnoot? Nee? Oké, dan eet ik hem zelf. Njeg.",
    "gui.guhs.guhwaii.lilo.intro.2": "Lilo-guh! Je moet niet met je mond vol praten. Hallo, ik ben Nani-guh, haar grote zus.",
    "gui.guhs.guhwaii.lilo.intro.3": "Gisteravond zag ik een vallende ster! Hij viel boven op de heuvel, met heel veel lawaai. BOEM!",
    "gui.guhs.guhwaii.lilo.intro.4": "Ik mag er niet alleen heen van Nani-guh. Wil jij gaan kijken? Neem een guh mee, dat is veiliger!",
    "gui.guhs.guhwaii.lilo.capsule": "Heb je de vallende ster al gevonden? Hij ligt boven op de heuvel! (Je superkompas weet de weg: tab Verhalen.)",
    "gui.guhs.guhwaii.lilo.adoptie.1": "Een capsule?! Met een rare meter erin? En was hij... leeg? Dan weet ik waar dat blauwe guhtje vandaan komt!",
    "gui.guhs.guhwaii.lilo.adoptie.2": "Vanochtend zat er een blauw guhtje met vier pootjes en grote oren in het asiel. Hij heet Experiment 626!",
    "gui.guhs.guhwaii.lilo.adoptie.3": "Lilo-guh... het asiel heeft gezegd dat hij 'een beetje wild' is. Weet je het zeker?",
    "gui.guhs.guhwaii.lilo.adoptie.4": "Heel zeker! Niemand wil hem, en dan moet IK hem hebben. Hij heet vanaf nu 626-guh. Kom maar, 626-guh!",
    "gui.guhs.guhwaii.lilo.adoptie.5": "Blub-njeg! (626-guh rent naar binnen, trekt alle kussens van de bank en gooit het zand-emmertje om.)",
    "gui.guhs.guhwaii.lilo.adoptie.6": "Oh nee, overal rommel! Niks kapot, gelukkig... Wil jij me helpen opruimen? Ik sta in de keuken.",
    "gui.guhs.guhwaii.lilo.opruimen": "626-guh bedoelt het niet zo, hij weet gewoon niet beter! Help jij Nani-guh met opruimen? (%s van %s)",
    "gui.guhs.guhwaii.lilo.lief": "626-guh moet leren lief te zijn. Laat hem zien wat lief is: geef hem iets liefs! Hij staat buiten bij het asiel.",
    "gui.guhs.guhwaii.lilo.klaar.1": "626-guh is de liefste guh van het hele eiland. Vind je ook niet? Njeg!",
    "gui.guhs.guhwaii.lilo.klaar.2": "Heb je de vadsigheid-scanner al geprobeerd? Elke guh is ONBEREKENBAAR VAHOEG. Elke!",
    "gui.guhs.guhwaii.lilo.klaar.3": "Weet je wat het fijnste is? Iemand hebben om samen kokosmelk mee te drinken.",
    "gui.guhs.guhwaii.lilo.klaar.4": "Aan de andere kant van het eiland kun je surfen! Kom je ook? Ik ben er vaak!",
    "gui.guhs.guhwaii.ohana.1": "Kijk nou! 626-guh heeft een kokosnoot, een bloem én kaasknabbels gekregen. En hij heeft niks omgegooid!",
    "gui.guhs.guhwaii.ohana.2": "Blub... lief? 626... lief? (Hij knuffelt Lilo-guh voorzichtig met al zijn vier armpjes.)",
    "gui.guhs.guhwaii.ohana.3": "Ze zeiden dat hij gemaakt was om TE VADS te zijn. Maar weet je wat? Een guh kan nooit te vads zijn!",
    "gui.guhs.guhwaii.ohana.citaat": "Ohana betekent familie. Familie betekent dat niemand wordt achtergelaten... of vergeten. Njeg.",
    "gui.guhs.guhwaii.ohana.4": "En jij hoort er nu ook bij. Dank je wel, echt. Hier, iets voor jou: de poster, een ukelele en onze eilandkleertjes!",
    "gui.guhs.guhwaii.ohana.5": "Aloha... NJEG! (626-guh zwaait met vier armpjes tegelijk.)",
    "gui.guhs.guhwaii.nani.begin": "Hoi, ik ben Nani-guh. Let je een beetje op Lilo-guh voor me? Ze heeft altijd van die... ideeën.",
    "gui.guhs.guhwaii.nani.opruimen": "Help je mee? Klik de rommeltjes weg: kussenveertjes, een omgevallen emmertje, kokosschilletjes... Nog %s!",
    "gui.guhs.guhwaii.nani.nog": "Dank je! Nog %s rommeltjes...",
    "gui.guhs.guhwaii.nani.opgeruimd": "Alles weer netjes! Er is niks kapot, gelukkig. Maar 626-guh moet wel leren lief te zijn...",
    "gui.guhs.guhwaii.nani.lief": "626-guh houdt van kokosnoten, van bloemen en natuurlijk van kaasknabbels. Wie niet?",
    "gui.guhs.guhwaii.nani.ohana": "Ik geloof dat Lilo-guh je iets wil zeggen. Ga maar gauw!",
    "gui.guhs.guhwaii.nani.klaar.1": "Sinds 626-guh er is, is het hier nooit meer stil. Maar wel gezellig. Njeg.",
    "gui.guhs.guhwaii.nani.klaar.2": "Wil je kokosmelk? Kokosnoot en een glazen flesje, meer heb je niet nodig!",
    "gui.guhs.guhwaii.626.vreemd": "Blub-njeg! (Een blauw guhtje met vier armpjes kijkt je met grote ogen aan en verstopt snel iets achter zijn rug.)",
    "gui.guhs.guhwaii.626.asiel": "Blub? (Hij drukt zijn neus tegen het hek van het asiel. Hij wil zo graag een thuis.)",
    "gui.guhs.guhwaii.626.rommel": "Blub-blub... (Hij kijkt een beetje schuldig naar de rommel. Een heel klein beetje.)",
    "gui.guhs.guhwaii.626.ohana": "Ohana? (Hij trekt aan je mouw, naar Lilo-guh!)",
    "gui.guhs.guhwaii.626.wat_lief": "Blub? (Hij wil iets liefs: een kokosnoot, een roze hibiscus of kaasknabbels.)",
    "gui.guhs.guhwaii.626.al_gehad": "Njeg! (Die heeft hij al. Hij wil iets anders liefs!)",
    "gui.guhs.guhwaii.626.meer_knabbels": "Blub! (Hij wijst naar je kaasknabbels en steekt %s vingertjes op. Met al zijn handjes.)",
    "gui.guhs.guhwaii.626.gave.kokos": "Krak, slurp! (626-guh drinkt de kokosnoot leeg en geeft je het halve dopje terug. Als cadeautje.)",
    "gui.guhs.guhwaii.626.gave.bloem": "Ooh... (Hij stopt de roze hibiscus heel voorzichtig achter zijn grote oor. Hij bloost!)",
    "gui.guhs.guhwaii.626.gave.knabbels": "NOM NOM NOM! (Hij eet de kaasknabbels op met vier handjes tegelijk. Dat is best vahoeg.)",
    "gui.guhs.guhwaii.626.lief_klaar": "626... lief! (Hij geeft je een knuffel. Een hele zachte. Ga maar gauw naar Lilo-guh!)",
    "gui.guhs.guhwaii.626.getemd": "Aloha, njeg! (626-guh springt in je armen. Hij gaat met je mee naar huis!)",
    "gui.guhs.guhwaii.626.al_thuis": "Aloha! (Deze 626-guh blijft bij Lilo-guh. Die van jou wacht thuis op je!)",
    "gui.guhs.guhwaii.626.knabbel": "Blub? (Geef hem een kaasknabbel, dan gaat hij met je mee!)",
    "gui.guhs.guhwaii.lijstje": "626-guh wil iets liefs: %s een kokosnoot  %s een roze hibiscus  %s %s kaasknabbels",
    "gui.guhs.guhwaii.hint.capsule": "Zoek de vallende ster boven op de heuvel van het eiland (superkompas: tab Verhalen, 'De neergestorte capsule').",
    "gui.guhs.guhwaii.hint.capsule_gevonden": "Een capsule! Met een rare meter erin... en hij is leeg. Vertel het aan Lilo-guh!",
    "gui.guhs.guhwaii.hint.opruimen": "Help Nani-guh opruimen: klik op de rommeltjes in en om het paalhuisje.",
    "gui.guhs.guhwaii.hint.ohana": "626-guh is lief geworden! Ga terug naar Lilo-guh.",
    "gui.guhs.guhwaii.hint.temmen": "Geef 626-guh (bij het asiel) een kaasknabbel: dan gaat hij met je mee naar huis. Maar één keer, dus zorg goed voor hem!",
    # the clothes (the sources' names are in verhaal.py)
    "item.guhs.guhwaii_hularokje": "Hula-rokje",
    "item.guhs.guhwaii_bloemenkrans": "Bloemenkrans",
    "item.guhs.guhwaii_stitchoren": "626-oren met antennes",
    "item.guhs.guhwaii_surfplankje": "Surfplankje",
    # the emote (overrides the fundament's text)
    "emote.guhs.ukelele": "Ukelele",
    "emote.guhs.ukelele.description": "Alleen 626-guh: hij pakt zijn ukelele en speelt een liedje. \"Aloha, njeg!\"",
}

# visible advancements in the Guhverhalen tab (and hidden quest/guhwaii_<name> for the ones granted by code)
ADVANCEMENTS = [  # name, parent, icon, frame, title, description, criteria (None: granted by code)
    ("aloha", "verhaal_guhwaii", "guhs:roze_hibiscus", "task", "Aloha, Lilo-guh!", "Maak kennis met Lilo-guh en Nani-guh in hun paalhuisje", None),
    ("capsule", "aloha", "minecraft:light_blue_stained_glass", "task", "Een vallende ster?", "Vind de neergestorte capsule boven op de heuvel", None),
    ("scanner", "capsule", "guhs:vadsigheid_scanner", "goal", "ONBEREKENBAAR VAHOEG", "Zet een guh op de vadsigheid-scanner. De meter slaat door!", None),
    ("adoptie", "capsule", "minecraft:oak_fence_gate", "task", "Uit het asiel", "Lilo-guh adopteert 626-guh uit het guh-asiel", None),
    ("opgeruimd", "adoptie", "minecraft:brush", "task", "Alleen maar rommel", "Ruim de rommel van 626-guh op (er is niks kapot!)", None),
    ("lief", "opgeruimd", "guhs:kokosnoot", "task", "Lief zijn is best vahoeg", "Geef 626-guh een kokosnoot, een roze hibiscus en kaasknabbels", None),
    ("ohana", "lief", "guhs:vadsigheid_poster", "goal", "Ohana betekent familie", "Niemand wordt achtergelaten... of vergeten. Njeg.", None),
    ("getemd", "ohana", "guhs:guhwaii_ukelele", "challenge", "Experiment 626, thuis!", "Neem 626-guh mee naar huis (één keer per speler)", None),
    ("ukelele", "getemd", "guhs:guhwaii_ukelele", "task", "Aloha, njeg!", "Laat je 626-guh ukelele spelen (knop in het guhmenu)", None),
    ("ukelele_speler", "ohana", "minecraft:note_block", "task", "Tokkel tokkel", "Speel zelf op de reserve-ukelele: iedereen danst!", None),
    ("kleding", "ohana", "guhs:guhwaii_hularokje", "goal", "Eilandkleertjes", "Heb het hula-rokje, de bloemenkrans, de 626-oren en het surfplankje",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"} for c in CLOTHES]}}}),
    ("poster", "ohana", "guhs:vadsigheid_poster", "task", "Aan de muur!", "Hang de poster 'Onberekenbaar vahoeg' op",
     {"done": {"trigger": "minecraft:placed_block", "conditions": {"location": [{"condition": "minecraft:block_state_property",
                                                                                "block": "guhs:vadsigheid_poster"}]}}}),
    ("kokosnoot", "verhaal_guhwaii", "guhs:kokosnoot", "task", "Krak, slurp!", "Eet een kokosnoot van een guh-palm", None),
    ("kokosmelk", "kokosnoot", "guhs:kokosmelk", "task", "Kokosmelk!", "Drink een flesje kokosmelk", None),
    ("kiemplant", "kokosnoot", "guhs:kokosnoot", "task", "Een eigen palmpje", "Plant een kokosnoot in het zand: er groeit een guh-palm uit", None),
    ("palm_knipoog", "verhaal_guhwaii", "guhs:guhwaii_palm_gezicht", "task", "Knipoog!", "Klik op het gezichtje van een guh-palm", None),
    ("hibiscus", "verhaal_guhwaii", "guhs:roze_hibiscus", "task", "Een bloem achter je oor", "Pluk een roze hibiscus op Guhwai'i",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:roze_hibiscus"}]}}}),
    ("eitjes", "verhaal_guhwaii", "guhs:schilly_eitjes", "task", "Knak, knak!", "Zie Schilly-eitjes uitkomen op het strand", None),
    ("snorkelen", "verhaal_guhwaii", "minecraft:tube_coral", "task", "Snorkelen", "Zwem in de warme lagune van Guhwai'i, bij het kaaskoraal",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": "guhs:guhwaii",
                                                                                      "fluid": {"fluids": "#minecraft:water"}}}}}}),
]


# =====================================================================================================================
# blocks, items: models, blockstates, loot, recipes, tags
# =====================================================================================================================
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def el(frm, to, tex_, faces=None, uv=None, rot=None):
    e = {"from": frm, "to": to, "faces": {f: ({"texture": tex_, "uv": uv} if uv else {"texture": tex_})
                                          for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    return e


def scanner_elements():
    """The scanner facing north (the screen looks north; the post stands at the south edge)."""
    m, p, s = "#metaal", "#plaat", "#scherm"
    return [
        {"from": [0, 0, 0], "to": [16, 3, 16], "faces": {"up": {"texture": p}, "down": {"texture": m}, "north": {"texture": m, "uv": [0, 12, 16, 15]},
                                                          "south": {"texture": m, "uv": [0, 12, 16, 15]}, "east": {"texture": m, "uv": [0, 12, 16, 15]},
                                                          "west": {"texture": m, "uv": [0, 12, 16, 15]}}},
        el([6, 3, 13], [10, 16, 16], m),
        # (3.0 QA: parts above y 16 need explicit uvs; the automatic ones ran off the texture into the atlas)
        {"from": [0.5, 14, 11.5], "to": [15.5, 30, 13.5], "faces": {"north": {"texture": s, "uv": [0, 0, 16, 16]},
                                                                    "south": {"texture": m, "uv": [0.5, 0, 15.5, 16]},
                                                                    "east": {"texture": m, "uv": [2.5, 0, 4.5, 16]},
                                                                    "west": {"texture": m, "uv": [11.5, 0, 13.5, 16]},
                                                                    "up": {"texture": m, "uv": [0.5, 11.5, 15.5, 13.5]},
                                                                    "down": {"texture": m, "uv": [0.5, 2.5, 15.5, 4.5]}}},
        antenne([2, 30, 12], [3, 32, 13], m), antenne([13, 30, 12], [14, 32, 13], m),
        el([7, 3, 11], [9, 4, 13], m),
    ]


def antenne(van, tot, t):
    """A little box above y 16 with uvs that stay on its texture."""
    w, hg, d = tot[0] - van[0], tot[1] - van[1], tot[2] - van[2]
    return {"from": van, "to": tot, "faces": {
        "north": {"texture": t, "uv": [7, 7, 7 + w, 7 + hg]}, "south": {"texture": t, "uv": [7, 7, 7 + w, 7 + hg]},
        "east": {"texture": t, "uv": [7, 7, 7 + d, 7 + hg]}, "west": {"texture": t, "uv": [7, 7, 7 + d, 7 + hg]},
        "up": {"texture": t, "uv": [7, 7, 7 + w, 7 + d]}, "down": {"texture": t, "uv": [7, 7, 7 + w, 7 + d]}}}


def ukelele_elements():
    """The ukelele (item model), lying along x: a round body, a neck, a head with pegs."""
    h, v, n = "#hout", "#voor", "#hals"
    return [
        {"from": [1, 6, 7], "to": [8, 11, 9], "faces": {"north": {"texture": v}, "south": {"texture": h}, "east": {"texture": h},
                                                       "west": {"texture": h}, "up": {"texture": h}, "down": {"texture": h}}},
        {"from": [2, 5, 7], "to": [7, 12, 9], "faces": {"north": {"texture": v}, "south": {"texture": h}, "east": {"texture": h},
                                                       "west": {"texture": h}, "up": {"texture": h}, "down": {"texture": h}}},
        el([8, 7.5, 7.5], [14, 9.5, 8.5], n),
        el([14, 7, 7.25], [16, 10, 8.75], h),
    ]


def rommeltje_models(h):
    """Four rommeltjes: a toppled sand bucket with spilt sand, pillow feathers, coconut shells, a knocked-over flower pot."""
    A = h.A
    modellen_ = {
        0: ({"zand": "minecraft:block/sand", "emmer": "minecraft:block/red_terracotta", "particle": "minecraft:block/sand"},
            [el([1, 0, 1], [9, 1, 9], "#zand"), el([3, 1, 3], [7, 2, 7], "#zand"), el([8, 0, 7], [13, 4, 12], "#emmer"),
             el([9, 4, 8], [12, 5, 11], "#emmer")]),
        1: ({"kussen": "minecraft:block/pink_wool", "veer": "minecraft:block/white_wool", "particle": "minecraft:block/pink_wool"},
            [el([2, 0, 3], [10, 3, 10], "#kussen"), el([11, 0, 2], [12, 1, 4], "#veer"), el([12, 0, 9], [14, 1, 10], "#veer"),
             el([4, 0, 12], [6, 1, 13], "#veer"), el([13, 0, 5], [14, 1, 6], "#veer"), el([1, 0, 12], [2, 1, 14], "#veer")]),
        2: ({"schil": "guhs:block/kokosnoot_bruin", "binnen": "minecraft:block/white_concrete", "particle": "guhs:block/kokosnoot_bruin"},
            [el([2, 0, 2], [7, 3, 7], "#schil"), el([3, 3, 3], [6, 3.1, 6], "#binnen", faces=("up",)), el([9, 0, 8], [14, 3, 13], "#schil"),
             el([10, 3, 9], [13, 3.1, 12], "#binnen", faces=("up",)), el([6, 0, 11], [8, 1, 12], "#schil")]),
        3: ({"pot": "minecraft:block/flower_pot", "aarde": "minecraft:block/dirt", "bloem": "guhs:block/roze_hibiscus",
             "particle": "minecraft:block/flower_pot"},
            [el([4, 0, 5], [10, 5, 11], "#pot"), el([10, 0, 4], [15, 1, 12], "#aarde"),
             {"from": [10, 1, 6], "to": [16, 6, 6.01], "faces": {"north": {"texture": "#bloem"}, "south": {"texture": "#bloem"}}}]),
    }
    for soort, (textures, elements) in modellen_.items():
        h.w(f"{A}/models/block/guhwaii_rommeltje_{soort}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
    h.w(f"{A}/blockstates/guhwaii_rommeltje.json", {"variants": {
        f"soort={s}": [{"model": f"guhs:block/guhwaii_rommeltje_{s}", **({"y": r} if r else {})} for r in (0, 90, 180, 270)] for s in range(4)}})
    h.w(f"{A}/models/item/guhwaii_rommeltje.json", {"parent": "guhs:block/guhwaii_rommeltje_0"})


# the guh-palm wood set (1.2.8): our block -> the vanilla oak block whose blockstate, models, loot and recipe it borrows
PALM_HOUT = {"guhwaii_palm_trap": "oak_stairs", "guhwaii_palm_plaat": "oak_slab", "guhwaii_palm_hek": "oak_fence",
             "guhwaii_palm_poort": "oak_fence_gate", "guhwaii_palm_deur": "oak_door", "guhwaii_palm_luik": "oak_trapdoor"}
PALM_SET = ["guhwaii_palm_gestript", *PALM_HOUT, "guhwaii_palm_bord"]          # (+ guhwaii_palm_wandbord: the sign on a wall)


def palm_houtset(h):
    """
    The palm's stripped log, stairs, slab, fence, gate, door, trapdoor and sign (standing guhwaii_palm_bord + wall
    guhwaii_palm_wandbord, one item): vanilla oak's blockstates, models, loot and recipes with the palm's textures (the same
    way as vadswoud.wood_set), their recipes, loot and tags. Textures: guhwaii_tex.palm_houtset.
    """
    A, D, w = h.A, h.D, h.w
    P = "guhs:guhwaii_palm_planken"
    # the stripped log
    for model, parent in (("guhwaii_palm_gestript", "cube_column"), ("guhwaii_palm_gestript_horizontal", "cube_column_horizontal")):
        w(f"{A}/models/block/{model}.json", {"parent": f"minecraft:block/{parent}", "textures": {
            "end": "guhs:block/guhwaii_palm_gestript_top", "side": "guhs:block/guhwaii_palm_gestript"}})
    w(f"{A}/blockstates/guhwaii_palm_gestript.json", {"variants": {
        "axis=y": {"model": "guhs:block/guhwaii_palm_gestript"},
        "axis=z": {"model": "guhs:block/guhwaii_palm_gestript_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/guhwaii_palm_gestript_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/guhwaii_palm_gestript.json", {"parent": "guhs:block/guhwaii_palm_gestript"})
    h.self_drop("guhwaii_palm_gestript")
    h.shapeless("guhwaii_palm_planken_gestript", ["guhs:guhwaii_palm_gestript"], P, 4)
    # stairs, slab, fence, gate, door, trapdoor: vanilla oak, repainted
    tex_ = {"minecraft:block/oak_planks": "guhs:block/guhwaii_palm_planken", "minecraft:block/oak_door_top": "guhs:block/guhwaii_palm_deur_top",
            "minecraft:block/oak_door_bottom": "guhs:block/guhwaii_palm_deur_bottom", "minecraft:block/oak_trapdoor": "guhs:block/guhwaii_palm_luik"}
    with zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")) as z:
        names = z.namelist()

        def recipe(oak, ours):
            text = json.dumps(json.loads(z.read(f"data/minecraft/recipe/{oak}.json")))
            text = text.replace('"item": "minecraft:oak_planks"', f'"item": "{P}"')
            text = text.replace(f'"id": "minecraft:{oak}"', f'"id": "guhs:{ours}"').replace('"group": "wooden_', '"group": "guhwaii_palm_')
            w(f"{D}/recipe/{ours}.json", json.loads(text))

        for ours, oak in PALM_HOUT.items():
            state = json.dumps(json.loads(z.read(f"assets/minecraft/blockstates/{oak}.json")))
            state = state.replace("minecraft:block/oak_planks", "guhs:block/guhwaii_palm_planken").replace(f"minecraft:block/{oak}", f"guhs:block/{ours}")
            w(f"{A}/blockstates/{ours}.json", json.loads(state))
            # (fence models are called oak_fence_*, so the gate's models must not be caught by the fence prefix)
            for path in names:
                if not path.startswith(f"assets/minecraft/models/block/{oak}") or not path.endswith(".json"):
                    continue
                rest = path[len(f"assets/minecraft/models/block/{oak}"):-5]
                if oak == "oak_fence" and rest.startswith("_gate"):
                    continue
                model = json.loads(z.read(path))
                model["textures"] = {k: tex_.get(v, v) for k, v in model.get("textures", {}).items()}
                if oak in ("oak_door", "oak_trapdoor"):
                    model["render_type"] = "minecraft:cutout"
                w(f"{A}/models/block/{ours}{rest}.json", model)
            loot = z.read(f"data/minecraft/loot_table/blocks/{oak}.json").decode("utf-8")
            w(f"{D}/loot_table/blocks/{ours}.json", json.loads(loot.replace(f"minecraft:{oak}", f"guhs:{ours}")))
            recipe(oak, ours)
        recipe("oak_sign", "guhwaii_palm_bord")
    w(f"{A}/models/item/guhwaii_palm_trap.json", {"parent": "guhs:block/guhwaii_palm_trap"})
    w(f"{A}/models/item/guhwaii_palm_plaat.json", {"parent": "guhs:block/guhwaii_palm_plaat"})
    w(f"{A}/models/item/guhwaii_palm_hek.json", {"parent": "guhs:block/guhwaii_palm_hek_inventory"})
    w(f"{A}/models/item/guhwaii_palm_poort.json", {"parent": "guhs:block/guhwaii_palm_poort"})
    w(f"{A}/models/item/guhwaii_palm_luik.json", {"parent": "guhs:block/guhwaii_palm_luik_bottom"})
    h.item_model("guhwaii_palm_deur")
    # the sign: the block model only carries the break particles (the sign itself is drawn by the vanilla sign renderer with
    # guhs:entity/signs/guhwaii_palm, Java: GuhwaiiFeature.PALM_WOOD); the wall sign drops the standing one's loot table
    w(f"{A}/models/block/guhwaii_palm_bord.json", {"textures": {"particle": "guhs:block/guhwaii_palm_planken"}})
    for bord in ("guhwaii_palm_bord", "guhwaii_palm_wandbord"):
        w(f"{A}/blockstates/{bord}.json", {"variants": {"": {"model": "guhs:block/guhwaii_palm_bord"}}})
    h.item_model("guhwaii_palm_bord")
    h.self_drop("guhwaii_palm_bord")
    # tags (all the palm set's lines together)
    add = h.add_tag
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/logs_that_burn", ["guhs:guhwaii_palm_gestript"])
        add(f"minecraft/tags/{kind}/wooden_stairs", ["guhs:guhwaii_palm_trap"])
        add(f"minecraft/tags/{kind}/wooden_slabs", ["guhs:guhwaii_palm_plaat"])
        add(f"minecraft/tags/{kind}/wooden_fences", ["guhs:guhwaii_palm_hek"])
        add(f"minecraft/tags/{kind}/fence_gates", ["guhs:guhwaii_palm_poort"])
        add(f"minecraft/tags/{kind}/wooden_doors", ["guhs:guhwaii_palm_deur"])
        add(f"minecraft/tags/{kind}/wooden_trapdoors", ["guhs:guhwaii_palm_luik"])
    add("minecraft/tags/block/standing_signs", ["guhs:guhwaii_palm_bord"])
    add("minecraft/tags/block/wall_signs", ["guhs:guhwaii_palm_wandbord"])
    add("minecraft/tags/item/signs", ["guhs:guhwaii_palm_bord"])
    add("minecraft/tags/block/mineable/axe", [f"guhs:{b}" for b in PALM_SET + ["guhwaii_palm_wandbord"]])


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # --- the guh-palm ---
    w(f"{A}/models/block/guhwaii_palm_stam.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "guhs:block/guhwaii_palm_stam_top", "side": "guhs:block/guhwaii_palm_stam"}})
    w(f"{A}/models/block/guhwaii_palm_stam_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
        "end": "guhs:block/guhwaii_palm_stam_top", "side": "guhs:block/guhwaii_palm_stam"}})
    w(f"{A}/blockstates/guhwaii_palm_stam.json", {"variants": {
        "axis=y": {"model": "guhs:block/guhwaii_palm_stam"},
        "axis=z": {"model": "guhs:block/guhwaii_palm_stam_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/guhwaii_palm_stam_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/guhwaii_palm_stam.json", {"parent": "guhs:block/guhwaii_palm_stam"})
    for naam, voor in (("guhwaii_palm_gezicht", "guhwaii_palm_gezicht"), ("guhwaii_palm_gezicht_knipoog", "guhwaii_palm_gezicht_knipoog")):
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": f"guhs:block/{voor}", "side": "guhs:block/guhwaii_palm_stam", "top": "guhs:block/guhwaii_palm_stam_top"}})
    w(f"{A}/blockstates/guhwaii_palm_gezicht.json", {"variants": {
        f"facing={f},knipoog={k}": {"model": "guhs:block/guhwaii_palm_gezicht" + ("_knipoog" if k == "true" else ""), **({"y": r} if r else {})}
        for f, r in ROT.items() for k in ("false", "true")}})
    w(f"{A}/models/item/guhwaii_palm_gezicht.json", {"parent": "guhs:block/guhwaii_palm_gezicht"})
    w(f"{A}/models/block/guhwaii_palm_blad.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout_mipped",
                                                   "textures": {"all": "guhs:block/guhwaii_palm_blad"}})
    w(f"{A}/blockstates/guhwaii_palm_blad.json", {"variants": {"": [{"model": "guhs:block/guhwaii_palm_blad", **({"y": r} if r else {})}
                                                                    for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/item/guhwaii_palm_blad.json", {"parent": "guhs:block/guhwaii_palm_blad"})
    w(f"{A}/models/block/guhwaii_palm_kiemplant.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                        "textures": {"cross": "guhs:block/guhwaii_palm_kiemplant"}})
    w(f"{A}/blockstates/guhwaii_palm_kiemplant.json", {"variants": {f"stage={s}": {"model": "guhs:block/guhwaii_palm_kiemplant"} for s in (0, 1)}})
    h.simple_block("guhwaii_palm_planken")
    palm_houtset(h)
    # --- the kokosnoot (hanging or lying; its face on one side) ---
    for i, naam in enumerate(("groen", "half", "bruin")):
        t = {"voor": f"guhs:block/kokosnoot_{naam}", "vacht": f"guhs:block/kokosnoot_{naam}_vacht", "particle": f"guhs:block/kokosnoot_{naam}_vacht"}
        hang = [{"from": [5, 7, 5], "to": [11, 14, 11], "faces": {"north": {"texture": "#voor", "uv": [3, 4, 13, 14]},
                                                                  **{f: {"texture": "#vacht"} for f in ("south", "east", "west", "up", "down")}}},
                el([7.5, 14, 7.5], [8.5, 16, 8.5], "#vacht")]
        lig = [{"from": [5, 0, 5], "to": [11, 6, 11], "faces": {"north": {"texture": "#voor", "uv": [3, 4, 13, 14]},
                                                                **{f: {"texture": "#vacht"} for f in ("south", "east", "west", "up", "down")}}}]
        w(f"{A}/models/block/kokosnoot_hangend_{i}.json", {"parent": "minecraft:block/block", "textures": t, "elements": hang})
        w(f"{A}/models/block/kokosnoot_liggend_{i}.json", {"parent": "minecraft:block/block", "textures": t, "elements": lig})
    w(f"{A}/blockstates/kokosnoot.json", {"variants": {
        f"hangend={hg},rijp={i}": [{"model": f"guhs:block/kokosnoot_{'hangend' if hg == 'true' else 'liggend'}_{i}", **({"y": r} if r else {})}
                                   for r in (0, 90, 180, 270)] for i in range(3) for hg in ("true", "false")}})
    h.item_model("kokosnoot")
    h.item_model("kokosmelk")
    # --- flowers and their pots ---
    for b in ("roze_hibiscus", "guhwaii_plumeria", "guhwaii_paradijsbloem", "guhwaii_orchidee"):
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
        h.item_model(b, f"guhs:block/{b}")
        w(f"{A}/models/block/potted_{b}.json", {"parent": "minecraft:block/flower_pot_cross", "render_type": "minecraft:cutout",
                                                "textures": {"plant": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/potted_{b}.json", {"variants": {"": {"model": f"guhs:block/potted_{b}"}}})
        w(f"{D}/loot_table/blocks/potted_{b}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}], "conditions": [{"condition": "minecraft:survives_explosion"}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        h.self_drop(b)
    # --- the Schilly-eitjes (the vanilla turtle egg models with our texture) ---
    vorm = {1: "template_turtle_egg", 2: "template_two_turtle_eggs", 3: "template_three_turtle_eggs", 4: "template_four_turtle_eggs"}
    for n, parent in vorm.items():
        for r in range(3):
            w(f"{A}/models/block/schilly_eitjes_{n}_{r}.json", {"parent": f"minecraft:block/{parent}", "textures": {"all": f"guhs:block/schilly_eitje_{r}"}})
    w(f"{A}/blockstates/schilly_eitjes.json", {"variants": {
        f"eitjes={n},rijp={r}": [{"model": f"guhs:block/schilly_eitjes_{n}_{r}", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
        for n in vorm for r in range(3)}})
    w(f"{A}/models/item/schilly_eitjes.json", {"parent": "guhs:block/schilly_eitjes_3_0"})
    w(f"{D}/loot_table/blocks/schilly_eitjes.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "guhs:schilly_eitjes", "functions": [
            {"function": "minecraft:set_count", "count": n, "conditions": [{"condition": "minecraft:block_state_property", "block": "guhs:schilly_eitjes",
                                                                           "properties": {"eitjes": str(n)}}]} for n in (2, 3, 4)]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # --- the scanner, the poster, the rommeltjes, the ukelele ---
    stex = {"particle": "guhs:block/vadsigheid_scanner_metaal", "metaal": "guhs:block/vadsigheid_scanner_metaal",
            "plaat": "guhs:block/vadsigheid_scanner_plaat", "scherm": "guhs:block/vadsigheid_scanner_scherm"}
    w(f"{A}/models/block/vadsigheid_scanner.json", {"parent": "minecraft:block/block", "textures": stex, "elements": scanner_elements()})
    w(f"{A}/blockstates/vadsigheid_scanner.json", {"variants": {f"facing={f}": {"model": "guhs:block/vadsigheid_scanner", **({"y": r} if r else {})}
                                                                for f, r in ROT.items()}})
    w(f"{A}/models/item/vadsigheid_scanner.json", {"parent": "guhs:block/vadsigheid_scanner", "display": {
        "gui": {"rotation": [30, 200, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -2, 0], "scale": [0.45, 0.45, 0.45]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}})
    w(f"{A}/models/block/vadsigheid_poster.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/vadsigheid_poster", "poster": "guhs:block/vadsigheid_poster", "rand": "minecraft:block/white_concrete"},
        "elements": [{"from": [0, 0, 15], "to": [16, 16, 16], "faces": {"north": {"texture": "#poster"}, "south": {"texture": "#rand"},
                                                                        "east": {"texture": "#rand"}, "west": {"texture": "#rand"},
                                                                        "up": {"texture": "#rand"}, "down": {"texture": "#rand"}}}]})
    w(f"{A}/blockstates/vadsigheid_poster.json", {"variants": {f"facing={f}": {"model": "guhs:block/vadsigheid_poster", **({"y": r} if r else {})}
                                                               for f, r in ROT.items()}})
    h.item_model("vadsigheid_poster", "guhs:block/vadsigheid_poster")
    rommeltje_models(h)
    w(f"{A}/models/item/guhwaii_ukelele.json", {"parent": "minecraft:item/generated", "textures": {
        "particle": "guhs:item/guhwaii_ukelele_hout", "hout": "guhs:item/guhwaii_ukelele_hout", "voor": "guhs:item/guhwaii_ukelele_voor",
        "hals": "guhs:item/guhwaii_ukelele_hals"}, "elements": ukelele_elements(), "display": {
        "gui": {"rotation": [0, 0, 35], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 90, 40], "translation": [0, 2, 1], "scale": [0.7, 0.7, 0.7]},
        "firstperson_righthand": {"rotation": [0, 100, 30], "translation": [1, 3, 1], "scale": [0.7, 0.7, 0.7]}}})
    # the painting (2 x 2)
    w(f"{D}/painting_variant/vadsigheidsniveau.json", {"asset_id": "guhs:vadsigheidsniveau", "width": 2, "height": 2})
    h.add_tag("minecraft/tags/painting_variant/placeable", ["guhs:vadsigheidsniveau"])

    # --- loot ---
    for blk in ("guhwaii_palm_stam", "guhwaii_palm_gezicht", "vadsigheid_scanner", "vadsigheid_poster"):
        h.self_drop(blk)
    w(f"{D}/loot_table/blocks/guhwaii_rommeltje.json", {"type": "minecraft:block", "pools": []})
    rijp = {"condition": "minecraft:block_state_property", "block": "guhs:kokosnoot", "properties": {"rijp": "2"}}
    w(f"{D}/loot_table/blocks/kokosnoot.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:kokosnoot"}], "conditions": [rijp, {"condition": "minecraft:survives_explosion"}]}]})
    w(f"{D}/loot_table/blocks/guhwaii_palm_kiemplant.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:kokosnoot"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    shears = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
    silk_or_shears = {"condition": "minecraft:any_of", "terms": [shears, {"condition": "minecraft:match_tool", "predicate": {
        "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}
    w(f"{D}/loot_table/blocks/guhwaii_palm_blad.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:guhwaii_palm_blad", "conditions": [silk_or_shears]},
            {"type": "minecraft:item", "name": "guhs:kokosnoot", "conditions": [{"condition": "minecraft:random_chance", "chance": 0.05}]},
            {"type": "minecraft:item", "name": "minecraft:stick", "conditions": [{"condition": "minecraft:random_chance", "chance": 0.2}],
             "functions": h.count_fn(1, 2)}]}]}]})
    h.self_drop("guhwaii_palm_planken")

    # --- recipes ---
    h.shapeless("guhwaii_palm_planken", ["guhs:guhwaii_palm_stam"], "guhs:guhwaii_palm_planken", 4)
    h.shapeless("guhwaii_palm_planken_gezicht", ["guhs:guhwaii_palm_gezicht"], "guhs:guhwaii_palm_planken", 4)
    h.shapeless("kokosmelk", ["guhs:kokosnoot", "minecraft:glass_bottle"], "guhs:kokosmelk", 1)
    for b, dye in (("roze_hibiscus", "pink_dye"), ("guhwaii_plumeria", "white_dye"), ("guhwaii_paradijsbloem", "orange_dye"),
                   ("guhwaii_orchidee", "purple_dye")):
        h.shapeless(f"{b}_kleurstof", [f"guhs:{b}"], f"minecraft:{dye}", 2)
    h.shaped("vadsigheid_poster", ["PBP", "RGR", "PPP"], {"P": "minecraft:paper", "B": "minecraft:light_blue_dye", "R": "minecraft:pink_dye",
                                                           "G": "minecraft:glowstone_dust"}, "guhs:vadsigheid_poster", 2)
    h.shaped("guhwaii_ukelele", ["  S", "PS ", "PP "], {"P": "guhs:guhwaii_palm_planken", "S": "minecraft:string"}, "guhs:guhwaii_ukelele", 1)

    # --- tags ---
    add = h.add_tag
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/logs_that_burn", ["guhs:guhwaii_palm_stam", "guhs:guhwaii_palm_gezicht"])
        add(f"minecraft/tags/{kind}/planks", ["guhs:guhwaii_palm_planken"])
        add(f"minecraft/tags/{kind}/leaves", ["guhs:guhwaii_palm_blad"])
        add(f"minecraft/tags/{kind}/small_flowers", ["guhs:roze_hibiscus", "guhs:guhwaii_plumeria", "guhs:guhwaii_paradijsbloem", "guhs:guhwaii_orchidee"])
    add("minecraft/tags/block/saplings", ["guhs:guhwaii_palm_kiemplant"])
    add("minecraft/tags/block/flower_pots", ["guhs:potted_roze_hibiscus", "guhs:potted_guhwaii_plumeria", "guhs:potted_guhwaii_paradijsbloem",
                                             "guhs:potted_guhwaii_orchidee"])
    add("minecraft/tags/block/mineable/axe", ["guhs:guhwaii_palm_stam", "guhs:guhwaii_palm_gezicht", "guhs:guhwaii_palm_planken", "guhs:kokosnoot"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:vadsigheid_scanner"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:guhwaii_palm_blad"])
    add("minecraft/tags/block/sword_efficient", ["guhs:roze_hibiscus", "guhs:guhwaii_plumeria", "guhs:guhwaii_paradijsbloem", "guhs:guhwaii_orchidee"])
    add("guhs/tags/item/band/snacks", ["guhs:kokosnoot"])


# =====================================================================================================================
# the biome's content (its features, steps 2+) and the buildings
# =====================================================================================================================
def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    grond = {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ["minecraft:sand", "minecraft:grass_block", "minecraft:dirt"]}
    # the guh-palm, the reef, the turtle nests (Java features)
    for naam in ("guhwaii_palm", "guhwaii_rif", "guhwaii_nestje"):
        w(f"{wg}/configured_feature/{naam}.json", {"type": f"guhs:{naam}", "config": {}})
    land = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    vloer = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]
    w(f"{wg}/placed_feature/guhwaii_palm.json", {"feature": "guhs:guhwaii_palm", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
            {"data": 1, "weight": 3}, {"data": 2, "weight": 3}, {"data": 3, "weight": 1}]}}] + land})
    w(f"{wg}/placed_feature/guhwaii_rif.json", {"feature": "guhs:guhwaii_rif", "placement": [{"type": "minecraft:count", "count": 2}] + vloer})
    w(f"{wg}/placed_feature/guhwaii_nestje.json", {"feature": "guhs:guhwaii_nestje", "placement": [{"type": "minecraft:rarity_filter", "chance": 3}] + land})
    # flowers on the sand and the grass, ferns and grass, seagrass and kaaskoraal on the lagoon floor
    bloemen = kw.plant_patch("guhs:roze_hibiscus", 24, 5)
    bloemen["config"]["feature"]["feature"]["config"]["to_place"] = {"type": "minecraft:weighted_state_provider", "entries": [
        {"weight": 4, "data": {"Name": "guhs:roze_hibiscus"}}, {"weight": 3, "data": {"Name": "guhs:guhwaii_plumeria"}},
        {"weight": 2, "data": {"Name": "guhs:guhwaii_paradijsbloem"}}, {"weight": 2, "data": {"Name": "guhs:guhwaii_orchidee"}}]}
    bloemen["config"]["feature"]["placement"][0]["predicate"]["predicates"][1] = grond
    w(f"{wg}/configured_feature/guhwaii_bloemen.json", bloemen)
    w(f"{wg}/placed_feature/guhwaii_bloemen.json", {"feature": "guhs:guhwaii_bloemen", "placement": [{"type": "minecraft:count", "count": 3}] + land})
    varens = kw.plant_patch("minecraft:fern", 32, 6)
    varens["config"]["feature"]["feature"]["config"]["to_place"] = {"type": "minecraft:weighted_state_provider", "entries": [
        {"weight": 3, "data": {"Name": "minecraft:short_grass"}}, {"weight": 2, "data": {"Name": "minecraft:fern"}}]}
    varens["config"]["feature"]["placement"][0]["predicate"]["predicates"][1] = {"type": "minecraft:matching_blocks", "offset": [0, -1, 0],
                                                                                  "blocks": ["minecraft:grass_block"]}
    w(f"{wg}/configured_feature/guhwaii_varens.json", varens)
    w(f"{wg}/placed_feature/guhwaii_varens.json", {"feature": "guhs:guhwaii_varens", "placement": [{"type": "minecraft:count", "count": 4}] + land})
    w(f"{wg}/placed_feature/guhwaii_zeegras.json", {"feature": "minecraft:seagrass_short", "placement": [{"type": "minecraft:count", "count": 40}] + vloer})
    w(f"{wg}/placed_feature/guhwaii_kaaskoraal.json", {"feature": "guhs:diepzee_kaaskoraal",
                                                      "placement": [{"type": "minecraft:rarity_filter", "chance": 2}] + vloer})

    def biome(d):
        stappen = d["features"]
        while len(stappen) < 11:
            stappen.append([])
        # (steps 0, 1 = the fundament's water, 6 = the ores; ours: the vegetation step)
        stappen[9] = ["guhs:guhwaii_rif", "guhs:guhwaii_kaaskoraal", "guhs:guhwaii_zeegras", "guhs:guhwaii_palm", "guhs:guhwaii_bloemen",
                      "guhs:guhwaii_varens", "minecraft:patch_sugar_cane", "guhs:guhwaii_nestje"]
        d["effects"]["particle"] = {"options": {"type": "minecraft:cherry_leaves"}, "probability": 0.0015}
    h.patch_json(f"{wg}/biome/guhwaii.json", biome)


def structures(h):
    pool, g = bouw.ohana(h)
    verhaal_wereld.regio_structuur(h, "guhwaii_ohana", pool, "guhwaii", "kust", 20300201, hoek=0, reach=36, voorrang=820, grond_y=g)
    pool, g = bouw.capsule(h)
    verhaal_wereld.regio_structuur(h, "guhwaii_capsule", pool, "guhwaii", "piek", 20300202, reach=30, voorrang=810, grond_y=g)


def advancements(h):
    for name, parent, icon, frame, title, desc, crit in ADVANCEMENTS:
        verhaal.zichtbaar(h, TAB, f"guhwaii_{name}", f"guhwaii_{parent}" if parent != "verhaal_guhwaii" else parent, icon, frame, title,
                          desc, crit)
        if crit is None:
            h.w(f"{h.D}/advancement/quest/guhwaii_{name}.json", {"criteria": verhaal.IMPOSSIBLE})
    for page in ("stitch626", "lilo_guh", "nani_guh"):
        h.w(f"{h.D}/advancement/quest/seen_{page}.json", {"criteria": verhaal.IMPOSSIBLE})


def selfcheck(h):
    problems = []
    for key, text in LANG.items():
        low = text.lower()
        if "hamster" in low:
            problems.append(f"lore (hamster): {key}")
        if "te vads" in low and not any(ok in low for ok in ("nooit te vads", "om te vads", "te vads?")):
            problems.append(f"lore (te vads): {key}")
    for b in ("guhwaii_palm_stam", "guhwaii_palm_gezicht", "guhwaii_palm_blad", "guhwaii_palm_kiemplant", "guhwaii_palm_planken", "kokosnoot",
              "roze_hibiscus", "guhwaii_plumeria", "guhwaii_paradijsbloem", "guhwaii_orchidee", "schilly_eitjes", "vadsigheid_scanner",
              "vadsigheid_poster", "guhwaii_rommeltje", *PALM_SET):
        if not os.path.exists(f"{h.A}/blockstates/{b}.json"):
            problems.append(f"block {b}: no blockstate")
        if f"block.guhs.{b}" not in h.NL:
            problems.append(f"block {b}: no name")
    # the palm wood set: an item model, a loot table and a recipe each, and every texture its models name
    for b in PALM_SET:
        for wat, pad in (("item model", f"{h.A}/models/item/{b}.json"), ("loot table", f"{h.D}/loot_table/blocks/{b}.json")):
            if not os.path.exists(pad):
                problems.append(f"block {b}: no {wat}")
        if b != "guhwaii_palm_gestript" and not os.path.exists(f"{h.D}/recipe/{b}.json"):
            problems.append(f"block {b}: no recipe")
    if not os.path.exists(f"{h.A}/blockstates/guhwaii_palm_wandbord.json"):
        problems.append("block guhwaii_palm_wandbord: no blockstate")
    for f in sorted(os.listdir(f"{h.A}/models/block")):
        if f.startswith("guhwaii_palm_"):
            with open(f"{h.A}/models/block/{f}", encoding="utf-8") as fh:
                for t in json.load(fh).get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{h.TEX}/{t[5:]}.png"):
                        problems.append(f"texture {t} ({f}) missing")
    for t in ("entity/signs/guhwaii_palm", "item/guhwaii_palm_bord", "item/guhwaii_palm_deur"):
        if not os.path.exists(f"{h.TEX}/{t}.png"):
            problems.append(f"texture {t} missing")
    for i in ("kokosnoot", "kokosmelk", "guhwaii_ukelele", "vadsigheid_scanner", "vadsigheid_poster", "schilly_eitjes", "roze_hibiscus"):
        if not os.path.exists(f"{h.A}/models/item/{i}.json"):
            problems.append(f"item {i}: no item model")
    for t in ("entity/guh_stitch626", "entity/npc_lilo_guh", "entity/npc_nani_guh", "painting/vadsigheidsniveau", "block/vadsigheid_poster",
              "entity/guh_slaap/guh_stitch626"):
        if not os.path.exists(f"{h.TEX}/{t}.png"):
            problems.append(f"texture {t} missing")
    for name, *_ in ADVANCEMENTS:
        if not os.path.exists(f"{h.D}/advancement/{TAB}/guhwaii_{name}.json"):
            problems.append(f"advancement {name} missing")
    if problems:
        raise SystemExit("guhwaii self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    tex.textures(h)
    for naam, kleur in (("groen", tex.KOKOS_GROEN), ("half", tex.KOKOS_HALF), ("bruin", tex.KOKOS_BRUIN)):
        h.save(tex.noisy(kleur, 14, random.Random(30113 + len(naam))), "block", f"kokosnoot_{naam}_vacht.png")
    blocks_and_items(h)
    modellen.stitch_look(h)
    modellen.npcs(h)
    worldgen(h)
    structures(h)
    advancements(h)
    for key, text in LANG.items():
        h.lang(key, text, text)
    bouw.tests(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: "Ohana op Guhwai'i" (chapter guhs_verhalen)
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    A = "guhs:verhalen/guhwaii_"
    q("guhwaii_lilo", "Aloha, Lilo-guh!", "Op het strand van &bGuhwai'i&r staat een huisje op palen. Daar wonen &dLilo-guh&r en haar grote zus "
      "&dNani-guh&r. Lilo-guh zag gisteravond een vallende ster... Praat met haar!", "guhs:roze_hibiscus", [adv(A + "aloha")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["verhaal_guhwaii"], xp=50)
    q("guhwaii_capsule", "Een vallende ster?", "Boven op de heuvel van het eiland ligt een neergestorte &bcapsule&r. Een krater, rook, een open "
      "luikje... en binnen een rare meter. Van wie zou hij zijn?", "minecraft:light_blue_stained_glass", [adv(A + "capsule")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["guhwaii_lilo"], xp=100)
    q("guhwaii_scanner", "ONBEREKENBAAR VAHOEG", "In de capsule staat de &bvadsigheid-scanner&r. Zet een guh op de plaat (of klik gewoon: je "
      "dichtstbijzijnde guh springt erop) en kijk naar de meter &eVADSIGHEIDSNIVEAU&r: een beetje vads... vads... heel vads... VAHOEG... "
      "&dONBEREKENBAAR VAHOEG&r! Hij werkt op elke guh. Een guh kan nooit te vads zijn!", "guhs:vadsigheid_scanner", [adv(A + "scanner")],
      rewards=(("guhs:vadsigheid_poster", 1),), deps=["guhwaii_capsule"], shape="gear", xp=150)
    q("guhwaii_adoptie", "Uit het asiel", "Vertel Lilo-guh over de capsule. Ze weet meteen wie erin zat: het blauwe guhtje met vier armpjes uit het "
      "&aasiel&r! Ze adopteert hem: vanaf nu heet hij &b626-guh&r.", "minecraft:oak_fence_gate", [adv(A + "adoptie")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["guhwaii_capsule"], xp=100)
    q("guhwaii_opruimen", "Alleen maar rommel", "626-guh heeft het huisje overhoop gehaald! Kussenveertjes, een omgevallen emmertje, "
      "kokosschilletjes... Het is alleen maar rommel, niks is kapot. Help &dNani-guh&r en klik de rommeltjes weg.", "minecraft:brush",
      [adv(A + "opgeruimd")], rewards=(("guhs:kokosmelk", 2),), deps=["guhwaii_adoptie"], xp=100)
    q("guhwaii_lief", "Lief zijn is best vahoeg", "626-guh moet leren lief te zijn. Laat het hem zien: geef hem een &6kokosnoot&r, een &droze "
      "hibiscus&r en &e8 kaasknabbels&r (rechtsklik op hem, bij het asiel).", "guhs:kokosnoot", [adv(A + "lief")],
      rewards=(("guhs:kaas_knabbels", 16),), deps=["guhwaii_opruimen"], xp=150)
    q("guhwaii_ohana", "Ohana betekent familie", "Ga terug naar Lilo-guh. &d\"Ohana betekent familie. Familie betekent dat niemand wordt "
      "achtergelaten... of vergeten. Njeg.\"&r Je krijgt de poster, een ukelele en de eilandkleertjes, en 626-guh mag met je mee!",
      "guhs:vadsigheid_poster", [adv(A + "ohana")], rewards=(("guhs:gouden_kaasknabbel", 1),), deps=["guhwaii_lief"], shape="octagon", xp=300)
    q("guhwaii_626", "Experiment 626, thuis!", "Geef 626-guh bij het asiel een &ekaasknabbel&r: dan springt hij in je armen en gaat hij met je "
      "mee naar huis. Maar één keer per speler! Hij klimt tegen muren op, hangt aan het plafond en draagt bij klusjes dubbel zoveel.",
      "guhs:guhwaii_ukelele", [adv(A + "getemd")], rewards=(("guhs:kaas_knabbels", 32),), deps=["guhwaii_ohana"], shape="heart", xp=300)
    q("guhwaii_ukelele", "Aloha, njeg!", "Open het guhmenu van je 626-guh en druk op &bUkelele&r (of kies het emote): hij tokkelt een liedje. "
      "\"Aloha, njeg!\"", "guhs:guhwaii_ukelele", [adv(A + "ukelele")], rewards=(("guhs:kokosmelk", 2),), deps=["guhwaii_626"], xp=100)
    q("guhwaii_tokkel", "Tokkel tokkel", "Speel zelf op de &breserve-ukelele&r van 626-guh (rechtsklik): alle guhs om je heen gaan dansen!",
      "guhs:guhwaii_ukelele", [adv(A + "ukelele_speler")], rewards=(("guhs:kaas_knabbels", 8),), deps=["guhwaii_ohana"])
    q("guhwaii_kleding", "Eilandkleertjes", "Het &dhula-rokje&r, de &dbloemenkrans&r, de &b626-oren met antennes&r en het &bsurfplankje&r: "
      "de kleertjes van Guhwai'i! Houd ze ingedrukt om ze te ontgrendelen voor al je guhs.", "guhs:guhwaii_hularokje",
      [item(f"guhs:{c}") for c in CLOTHES], rewards=(("guhs:kaas_knabbels", 16),), deps=["guhwaii_ohana"], xp=150)
    q("guhwaii_poster", "Aan de muur!", "Hang de poster &dOnberekenbaar vahoeg&r op. (Je kunt er meer maken: papier, lichtblauwe en roze "
      "kleurstof en gloeisteenpoeder. En er is ook een groot schilderij van!)", "guhs:vadsigheid_poster", [adv(A + "poster")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["guhwaii_ohana"])
    q("guhwaii_kokosnoot", "Krak, slurp!", "Onder de bladeren van een &aguh-palm&r hangen kokosnoten: groen, dan geel, dan bruin. Pluk een "
      "bruine (klik erop) en eet hem op. Of geef hem aan je guh: guhs zijn dol op kokosnoot!", "guhs:kokosnoot", [adv(A + "kokosnoot")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["verhaal_guhwaii"])
    q("guhwaii_kokosmelk", "Kokosmelk!", "Een kokosnoot en een glazen flesje: &fkokosmelk&r! Koel, zoet en je voelt je meteen beter.",
      "guhs:kokosmelk", [adv(A + "kokosmelk")], rewards=(("guhs:kaas_knabbels", 8),), deps=["guhwaii_kokosnoot"])
    q("guhwaii_kiemplant", "Een eigen palmpje", "Plant een kokosnoot in het zand of op gras: er groeit een &aguh-palm&r uit, met een gezichtje "
      "in de stam. Klik op dat gezichtje: hij knipoogt!", "guhs:guhwaii_palm_gezicht", [adv(A + "kiemplant"), adv(A + "palm_knipoog")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["guhwaii_kokosnoot"])
    q("guhwaii_hibiscus", "Een bloem achter je oor", "Op Guhwai'i bloeien &droze hibiscus&r, witte &fplumeria&r, oranje "
      "&6paradijsvogelbloemen&r en paarse &5orchideeën&r. Pluk een roze hibiscus!", "guhs:roze_hibiscus", [adv(A + "hibiscus")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["verhaal_guhwaii"])
    q("guhwaii_eitjes", "Knak, knak!", "In het warme zand liggen &aSchilly-eitjes&r. 's Nachts gaan ze sneller... knak, knak! Er komen kleine "
      "Poepschillytjes en Schillytjes uit. Wacht erbij en zie ze uitkomen!", "guhs:schilly_eitjes", [adv(A + "eitjes")],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["verhaal_guhwaii"], xp=100)
    q("guhwaii_snorkelen", "Snorkelen", "Zwem in de warme lagune van Guhwai'i. Op de bodem groeit een rif van &ekaaskoraal&r (bij kaaskoraal "
      "kun je onder water ademen!), met koraal en &dguhvisjes&r.", "minecraft:tube_coral", [adv(A + "snorkelen")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["verhaal_guhwaii"])
    q("guhwaii_guhdex", "Guhdex: Guhwai'i", "Zet &b626-guh&r, &dLilo-guh&r en &dNani-guh&r in je Guhdex (kom gewoon dichtbij).",
      "guhs:guhdex", [adv("seen_stitch626"), adv("seen_lilo_guh"), adv("seen_nani_guh")], rewards=(("guhs:kaas_knabbels", 12),),
      deps=["guhwaii_lilo"])
