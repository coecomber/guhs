"""
bbq2 (ring-kern): the core of "In de ban van de Knabbelring" (DESIGN_130 4). Java: feature/ring. Manual for the seven chapter
slices: guhs_work130/reports/slice_ring-kern.md.

  ring_modellen.py   the cast: Guhdalf (grey and white), Araguh, Leguhlas, Gimguh, Merrie, Pippguh, Guhrond, Guhladriel, Boromika,
                     Smikagol (NPC kind and walking entity), the Knekel-Mika rider of the Nine; their animation files
  ring_tex.py        the items (the Knabbelring, the Lichtflesje, the Elfenmanteltje, the Elfentouw, the Stoofpotje, the
                     Feestknabbel, the Vissenbotje, two spawn eggs), the Elfentouwhaak and the Rustvuurtje, the travel map
  ring_bouw.py       the rest points (structure guhs:ring_rustpunt: three little camps) and the test room
  ring_tekst.py      every Dutch text
  ring_wiki.py       the wiki entries (CONTRACT_130 2.4; not built here)
  here               Sam-guh's fur and variant bones, the four reward outfits (BONES / variants / clothes / icons / CLOTHES),
                     the structure set, recipes, tags, advancements, the Guhdex page, the FTB section "De Knabbelring"

Fixed ids of CONTRACT_130 7 that this module owns: knabbelring, lichtflesje, elfenmanteltje, elfentouw (items),
elfentouw_haak (block), smikagol, knekel_ruiter (entities).

  cast(h, kind, id, lijn, van, tot, plek=None, yaw=0.0)   (for the chapter modules) the entity NBT of a cast character in a
        template that only exists for players whose step of questline `lijn` is van..tot (Java: Zicht / Cast.zetBij)
"""
import json
import os

import numpy as np

from features import bbq2
from features import ring_bouw as bouw
from features import ring_modellen as modellen
from features import ring_tekst as tekst
from features import ring_tex as tex
from features import verhaal_motor
from features import wereld

# The chapter guhs_knabbelring is the one chapter whose quests really depend on each other (make_ftbquests.py FTB_LINEAIR):
# every ring module sets it. Its quests are one chain in quest order (FEATURES order: ring, ring_h1 ... ring_h6,
# ring_sausuman); a quest that must come after another one names it in deps=[...] (the Toren van Sausuman: a quest of h4).
FTB_LINEAIR = True
FTB_SECTIES = [("ring", "De Knabbelring", "npc:guhdalf", None)]

STRUCTUUR = "ring_rustpunt"
SALT = 21301501

# =====================================================================================================================
# Sam-guh's variant bones (prefix samguh: only the Sam-guh shows them) and the four outfits (prefix outfit_ring_)
# =====================================================================================================================
# Every swatch below is an OLD, shared swatch name of make_guh_variants (cape, scarf, helmet, pom, tall_hat, crown, suit,
# bowtie): the guh sheet has next to no free space left, a variant paints its own fur texture and every clothes piece its
# own clothes texture, so reusing those spots costs nothing and takes nothing from the other slices.
_H = [0, 6, -2]
_B = [0, 6, 6]
_VOETEN = {"al": ("leg_back_left", [6, 1, 9], [4.5, 0, 7]), "ar": ("leg_back_right", [-6, 1, 9], [-8, 0, 7]),
           "vl": ("leg_front_left", [2, 1, 0], [4.25, 0, -2.5]), "vr": ("leg_front_right", [-6, 1, 0], [-7.75, 0, -2.5])}
BONES = {
    # Sam-guh: his pack with the bedroll on top, the frying pan and the ladle hanging from it, a curly sandy tuft
    "samguh_rugzak": ("body", _B, "cape", [([-4.2, 11.0, 2.4], [8.4, 5.2, 6.4], 0), ([-3.4, 12.0, 8.8], [6.8, 3.2, 1.2], 0),
                                          ([-5.0, 9.4, 3.4], [0.9, 2.2, 4.4], 0), ([4.1, 9.4, 3.4], [0.9, 2.2, 4.4], 0)]),
    "samguh_deken": ("body", _B, "scarf", [([-4.8, 16.2, 3.6], [9.6, 2.6, 2.6], 0), ([-5.2, 16.0, 3.4], [0.8, 3.0, 3.0], 0),
                                          ([4.4, 16.0, 3.4], [0.8, 3.0, 3.0], 0)]),
    "samguh_pan": ("body", _B, "helmet", [([4.4, 11.6, 3.2], [0.9, 4.2, 4.2], 0), ([4.5, 9.2, 4.9], [0.7, 2.6, 0.9], 0),
                                         ([-5.2, 10.4, 6.4], [0.7, 4.8, 0.7], 0), ([-5.4, 9.2, 6.0], [1.1, 1.4, 1.5], 0)]),
    "samguh_haar": ("head", _H, "pom", [([-3.4, 15.0, -9.4], [6.8, 1.5, 5.4], 0), ([-2.6, 16.4, -8.8], [2.6, 1.1, 2.4], 0),
                                       ([0.4, 16.4, -9.2], [2.8, 1.3, 2.6], 0), ([-1.2, 16.4, -6.2], [2.4, 1.0, 2.0], 0)]),
    # the outfits
    "outfit_ring_hoed": ("head", _H, "tall_hat", [([-7.0, 15.0, -11.8], [14, 0.7, 11.6], 0), ([-3.8, 15.7, -9.2], [7.6, 3.0, 6.4], 0),
                                                 ([-2.8, 18.7, -8.2], [5.6, 2.8, 4.6], 0), ([-1.8, 21.5, -7.2], [3.6, 2.6, 3.0], 0),
                                                 ([-1.0, 23.9, -6.0], [2.0, 1.8, 2.6], 0), ([-0.6, 23.2, -3.8], [1.2, 1.4, 1.6], 0)]),
    "outfit_ring_baard": ("head", _H, "pom", [([-3.4, 4.2, -13.2], [6.8, 1.1, 0.8], 0), ([-4.4, 0.4, -13.0], [8.8, 4.0, 1.2], 0),
                                             ([-3.2, -0.2, -12.8], [6.4, 1.0, 1.0], 0),
                                             ([-7.0, 12.6, -12.5], [4.6, 1.1, 0.7], 0), ([2.4, 12.6, -12.5], [4.6, 1.1, 0.7], 0)]),
    "outfit_ring_mantel": ("body", _B, "cape", [([-6.9, 11.3, -1.6], [13.8, 0.5, 12.8], 0), ([-6.9, 3.6, 10.8], [13.8, 8.0, 0.5], 0),
                                               ([-7.3, 4.6, -1.6], [0.5, 7.0, 12.4], 0), ([6.8, 4.6, -1.6], [0.5, 7.0, 12.4], 0),
                                               ([-5.2, 11.6, -3.0], [10.4, 1.6, 2.6], 0)]),
    "outfit_ring_speld": ("body", _B, "crown", [([7.2, 8.4, -1.2], [0.6, 2.6, 2.0], 0), ([7.3, 10.8, -0.6], [0.5, 1.0, 0.8], 0)]),
    "outfit_ring_ketting": ("body", _B, "scarf", [([-7.3, 10.9, -0.9], [14.6, 0.5, 0.9], 0), ([6.9, 3.6, -0.9], [0.5, 7.4, 0.9], 0),
                                                 ([-7.4, 3.6, -0.9], [0.5, 7.4, 0.9], 0)]),
    "outfit_ring_ringetje": ("body", _B, "bowtie", [([7.4, 1.0, -1.8], [0.5, 2.8, 2.8], 0)]),
}
for _naam, (_poot, _pivot, _o) in _VOETEN.items():
    # hairy hobbit feet: a furry sock over the paw and a tuft on top of it
    BONES[f"outfit_ring_voet_{_naam}"] = (_poot, _pivot, "suit", [(_o, [3.5, 2.5, 4.5], 0.4),
                                                                   ([_o[0] + 0.6, 2.7, _o[2] + 0.5], [2.3, 1.0, 2.8], 0)])
CLOTHES = ["ring_guhdalfhoed", "ring_elfenmantel", "ring_hobbitvoeten", "ring_ringketting"]


def variants(rng, v):
    """Sam-guh: warm sandy-brown fur, a leather pack, a green bedroll, iron pans, a curly tuft."""
    def pan():
        a = v.fabric((62, 62, 70), rng, 8)
        a[:3, :] = (110, 112, 124)
        a[12:20, 12:20] = (44, 44, 52)
        return a

    def haar():
        a = v.fabric((150, 104, 62), rng, 18)
        for x in range(0, 32, 4):
            a[:, x] = a[:, x] * 0.8
        return a

    def rugzak():
        a = v.fabric((126, 84, 52), rng, 12)
        a[:, 14:18] = (92, 60, 38)
        a[10:13, :] = (92, 60, 38)
        a[9:14, 13:19] = (236, 196, 80)
        return a

    return {"sam_guh": ((196, 150, 104), {"cape": rugzak, "scarf": lambda: v.stripes((84, 140, 88), (70, 116, 76), rng, 4),
                                         "helmet": pan, "pom": haar})}


def clothes(rng, v):
    def hoed():
        a = v.fabric((96, 104, 128), rng, 10)
        a[26:32, :] = (60, 66, 86)
        return a

    def baard():
        a = v.fabric((232, 232, 236), rng, 10)
        for x in range(0, 32, 3):
            a[:, x] = a[:, x] * 0.86
        return a

    def mantel():
        a = v.fabric((118, 150, 126), rng, 10)
        for y in range(0, 32, 6):
            a[y, :] = a[y, :] * 0.9
        a[29:32, :] = (88, 118, 96)
        return a

    def speld():
        a = v.fabric((70, 150, 80), rng, 8)
        for i in range(32):
            a[i, max(0, 15 - (16 - abs(i - 16)) // 3):16 + (16 - abs(i - 16)) // 3] = (120, 200, 120)
        a[:, 15:17] = (220, 240, 210)
        return a

    def voet():
        a = v.fabric((126, 86, 54), rng, 22)
        for x in range(0, 32, 3):
            a[:, x] = a[:, x] * 0.78
        return a

    def ketting():
        a = v.fabric((214, 220, 232), rng, 8)
        for x in range(0, 32, 4):
            a[:, x:x + 1] = (150, 156, 170)
        return a

    def ringetje():
        a = np.zeros((32, 32, 4), np.float32)
        for y in range(32):
            for x in range(32):
                d = ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5
                if 7.5 <= d <= 14.5:
                    a[y, x] = (250, 196, 60, 255) if (x + y) % 9 else (255, 236, 150, 255)
                    if d > 13.2 or d < 8.6:
                        a[y, x] = (160, 100, 20, 255)
        return a

    return {"ring_guhdalfhoed": {"tall_hat": hoed, "pom": baard}, "ring_elfenmantel": {"cape": mantel, "crown": speld},
            "ring_hobbitvoeten": {"suit": voet}, "ring_ringketting": {"scarf": ketting, "bowtie": ringetje}}


def icons(ic):
    hoed = [".......aa.......", "......ahha......", "......ahha......", ".....ahhhha.....", ".....ahhhha.....", "....ahhhhhha....",
            "....abbbbbba....", "aaaahhhhhhhhaaaa", "ahhhhhhhhhhhhhha", ".aaaaaaaaaaaaaa.", "....wwwwwwww....", "....wwwwwwww....",
            ".....wwwwww.....", "......wwww......", ".......ww......."]
    mantel = ["....aaaaaaaa....", "...ammmmmmmma...", "..ammmllmmmmma..", "..ammlLLlmmmma..", ".ammmmllmmmmmma.", ".ammmmmmmmmmmma.",
              ".ammMmmmmmmMmma.", ".ammMmmmmmmMmma.", ".ammMmmmmmmMmma.", ".ammMMmmmmMMmma.", "..ammMMMMMMmma..", "...aammmmmmaa...",
              ".....aaaaaa....."]
    voeten = ["................", "..aaa......aaa..", ".ahhha....ahhha.", ".ahHhha..ahHhha.", ".ahhhha..ahhhha.", "ahhHhhhaahhhHhha",
              "ahhhhhhaahhhhhha", "atthtthaatthttha", ".aaaaaa..aaaaaa."]
    ketting = ["..c..........c..", "..c..........c..", "...c........c...", "....c......c....", ".....c....c.....", "......cccc......",
               ".......cc.......", ".....aaaaaa.....", "....aGGGGGGa....", "...aGGaaaaGGa...", "...aGa....aGa...", "...aGa....aGa...",
               "...aGGaaaaGGa...", "....aGGGGGGa....", ".....aaaaaa....."]
    return {
        "ring_guhdalfhoed": ic.icon(ic.pad(hoed), {"a": (50, 56, 76), "h": (96, 104, 128), "b": (60, 66, 86), "w": (232, 232, 236)}),
        "ring_elfenmantel": ic.icon(ic.pad(mantel), {"a": (60, 84, 70), "m": (118, 150, 126), "M": (92, 124, 100), "l": (90, 170, 90), "L": (210, 240, 200)}),
        "ring_hobbitvoeten": ic.icon(ic.pad(voeten), {"a": (70, 46, 28), "h": (150, 104, 66), "H": (110, 74, 46), "t": (236, 214, 190)}),
        "ring_ringketting": ic.icon(ic.pad(ketting), {"c": (190, 196, 210), "a": (150, 96, 20), "G": (250, 196, 60)}),
    }


# =====================================================================================================================
# for the chapter modules
# =====================================================================================================================
def cast(h, kind, id, lijn, van, tot, plek=None, yaw=0.0):
    """The entity NBT of a cast character for a chapter's template that only exists for players whose step of questline
    `lijn` is van..tot inclusive (tot 99: from van on, for ever): s.entity(x + 0.5, y, z + 0.5, ring.cast(h, "guhrond",
    "ringh2_guhrond", "ring_h2", 1, 99, plek="raad")). `id` is the Bezetting id (Java: Bezetting.npc(id, ...) repairs it),
    `plek` the NpcRollen plek of the scene's own role. Java twin: Cast.zetBij / Zicht.alleenBij."""
    nbt = wereld.npc(h, kind, id, plek, yaw)
    nbt["Tags"] = h.ms.NbtList(8, ["guhs_ring_zicht"])
    nbt["NeoForgeData"]["guhs_ring_bij"] = f"{lijn}:{van}-{tot}"
    return nbt


# =====================================================================================================================
# the build
# =====================================================================================================================
def texts(h):
    for key, text in tekst.TEKSTEN.items():
        h.lang(key, text, text)
    bbq2.pagina(h, "sam_guh", *tekst.SAM_PAGINA)
    for c in CLOTHES:
        h.item_model(c)


def advancements(h):
    for name, parent, icon, frame, titel, text in tekst.ADVANCEMENTS:
        bbq2.zichtbaar(h, "knabbelring", name, parent, icon, frame, titel, text)
        bbq2.verborgen(h, name)
    for name in tekst.VERBORGEN:
        bbq2.verborgen(h, name)


def data(h):
    # the ring stays with its bearer (never into storage; Sam-guh hands a lost one back)
    h.add_tag("guhs/tags/item/loaned", ["guhs:knabbelring"])
    # afterwards: hooks for the Elfentouw and a Rustvuurtje for at home can be made
    h.shaped("elfentouw_haak", ["I ", "IS"], {"I": "minecraft:iron_ingot", "S": "minecraft:string"}, "guhs:elfentouw_haak", 2)
    h.shaped("ring_rustvuur", [" K ", "SCS", "LLL"], {"K": "guhs:kaas_knabbels", "S": "minecraft:stick", "C": "minecraft:cauldron", "L": "#minecraft:logs"},
             "guhs:ring_rustvuur")


def structuren(h):
    templates = []
    for i, naam in enumerate(bouw.VARIANTEN):
        s, problems = bouw.bouw(h, i)
        if problems:
            raise SystemExit(f"ring: the rest point '{naam}' is not right:\n  " + "\n  ".join(problems))
        s.save(f"{STRUCTUUR}_{naam}")
        templates.append((f"{STRUCTUUR}_{naam}", 1))
    titel, tooltip = tekst.STRUCTUUR
    wereld.bbq_structuur(h, STRUCTUUR, soort="grot", titel=titel, tooltip=tooltip, biomes=wereld.BBQ, salt=SALT, templates=templates,
                         spacing=9, separation=4, grootte=12, vlak=5, hoogte=6)
    bouw.test_template(h).save("ring_test_kamer")


def kaart(h):
    verhaal_motor.reiskaart(h, "knabbelring", tex.reiskaart(), naam=tekst.REISKAART_NAAM)


def selfcheck(h):
    problems = modellen.check(h)
    for key in ("item.guhs.knabbelring", "quest.guhs.ring.portaal_dicht", "gui.guhs.titels.naam.ringdrager", "gui.guhs.verhaal.reiskaart.knabbelring",
                f"structure.guhs.{STRUCTUUR}", "entity.guhs.guh_npc.guhdalf", "quest.guhs.ring.cast.guhladriel.na.1", "item.guhs.ring_ringketting"):
        if key not in h.NL:
            problems.append(f"lang {key}")
    for kind in modellen.KINDS:
        for fase in ("voor", "reis", "na"):
            for i in range(2):
                if f"quest.guhs.ring.cast.{kind}.{fase}.{i}" not in h.NL:
                    problems.append(f"cast line {kind}.{fase}.{i}")
    for name in [f"{STRUCTUUR}_{v}" for v in bouw.VARIANTEN] + ["ring_test_kamer"]:
        if not os.path.exists(f"{h.D}/structure/{name}.nbt"):
            problems.append(f"template {name}")
    for item in list(tex.ICONS) + list(tex.EIEREN) + CLOTHES:
        if not os.path.exists(f"{h.A}/models/item/{item}.json"):
            problems.append(f"item model {item}")
    if "guhs:knabbelring" not in json.load(open(f"{h.R}/data/guhs/tags/item/loaned.json", encoding="utf-8"))["values"]:
        problems.append("the ring is not a loaned item")
    # the haltes of the Java Reiskaart are the pixels of the picture
    java = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ring", "RingFeature.java"), encoding="utf-8").read()
    for lijn, (x, y) in tex.HALTES.items():
        if f'new Halte("{lijn}", ' not in java or f", {x}, {y}, " not in java:
            problems.append(f"halte {lijn} is not at ({x}, {y}) in RingFeature")
    if problems:
        raise SystemExit("ring self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    tex.build(h)
    modellen.build(h)
    texts(h)
    advancements(h)
    data(h)
    structuren(h)
    kaart(h)
    selfcheck(h)


# =====================================================================================================================
# FTB: the section "De Knabbelring" (chapter guhs_knabbelring), the frame the seven chapters hang in
# =====================================================================================================================
def ftb(fq):
    """The chapter is linear (FTB_LINEAIR): its quests are one chain in quest order. So the order here matters:
    ring_begin FIRST (the start of the chain), then the side quests (each names ring_begin: a branch that locks nothing),
    and ring_wegwijs LAST (it also names ring_begin, and it is what the first quest of ring_h1 comes after)."""
    na = ["ring_begin"]
    fq.q("ring_begin", "Een tovenaar komt nooit te laat",
         "Bij de &6grote barbecueput&r in de Guhmensie staat een kar met vuurwerk. Dat is &dGuhdalf&r. Praat met hem: hij zoekt iemand met stevige "
         "pootjes voor een héél belangrijk klusje. Het &6grillportaal&r naar de Guhbarbecuether doet het pas weer als je hem geholpen hebt, njeg.",
         "minecraft:firework_rocket", [fq.adv("ring_guhdalf")], shape="gear")
    fq.q("ring_sam", "Ik ga niet zonder jou", "&dSam-guh&r loopt de hele reis met je mee, met al zijn pannen op zijn rug. Klik op hem: hij zegt wat je nu "
         "moet doen en welke kant je op moet. Iedereen heeft zijn eigen Sam-guh.", "guhs:ring_stoofpotje", [fq.adv("seen_sam_guh")], deps=na)
    fq.q("ring_rust", "Even uitpuffen", "Onderweg vind je &6rustpunten&r: een kampje met een &6Rustvuurtje&r. Loop ernaartoe en het is jouw rustpunt. "
         "Word je gezien of gepakt, dan kom je daar weer terug. Je verliest nooit iets. En Sam-guh kookt een stoofpotje, vahoeg!",
         "guhs:ring_rustvuur", [fq.adv("ring_rustpunt")], deps=na)
    fq.q("ring_om", "Niet omdoen!", "Rechtsklik met de &6Knabbelring&r en je doet hem om. Mika's zien je dan niet meer... maar het &cOog van Sausron&r "
         "ziet je juist heel goed, en in de Guhbarbecuether komen de &cNegen&r je halen. Doe hem dus snel weer af. Of niet, njeg.",
         "guhs:knabbelring", [fq.adv("ring_omgedaan")], deps=na)
    fq.q("ring_negen", "De Negen te snel af", "Negen &cKnekel-Mika-ruiters&r. Ze doen je niets: ze duwen je alleen terug naar je laatste rustpunt. "
         "Doe de ring af, ren hard weg of verblind ze met het Lichtflesje tot ze je kwijt zijn.", "minecraft:wither_skeleton_skull",
         [fq.adv("ring_negen_ontsnapt")], deps=na)
    fq.q("ring_gaven", "De gaven van Guhladriel", "Drie cadeautjes voor onderweg: het &bLichtflesje&r (een flits, en een lampje in je hand), het "
         "&aElfenmanteltje&r (bukken en stilstaan: je lijkt op een rots) en het &7Elfentouw&r (trekt je naar een haak).", "guhs:lichtflesje",
         [fq.adv("ring_gaven")], deps=na)
    fq.q("ring_rots", "Ik ben een rots, njeg", "Met het Elfenmanteltje bij je: &6buk en sta heel stil&r. Het Oog en de ruiters kijken zo langs je heen. "
         "Niet niezen.", "guhs:elfenmanteltje", [fq.adv("ring_rots")], deps=na)
    fq.q("ring_touw", "Hup, omhoog", "Kijk naar een &6Elfentouwhaak&r en rechtsklik met het Elfentouw: het trekt je erheen, tot 24 blokken ver. "
         "Na het verhaal kun je zelf haken maken en overal ophangen.", "guhs:elfentouw", [fq.adv("ring_elfentouw")], deps=na)
    fq.q("ring_gedragen", "Maar wel jou", "Bij de Frituurberg wordt de ring zwaarder en zwaarder. Klik dan op Sam-guh: &dik kan de ring niet dragen, "
         "maar wel jou, njeg!&r", "minecraft:saddle", [fq.adv("ring_gedragen")], deps=na)
    fq.q("ring_einde", "Ringdrager", "De Knabbelring is gefrituurd en gedeeld. Je hebt de titel &6Ringdrager&r, vier outfits (Guhdalfs punthoed met "
         "baard, de elfenmantel, harige hobbitvoetjes en de ring aan een kettinkje) en het beeldje van het Oog dat knippert.",
         "guhs:knabbelring", [fq.adv("ring_klaar")], rewards=(("guhs:kaas_knabbels", 16),), deps=na, shape="gear", xp=250)
    fq.q("ring_eigen_sam", "Sam-guh mag mee naar huis", "Na het feest staat Sam-guh nog naast je. Klik op hem en hij is voor altijd van jou. "
         "Eén keer, alleen voor jou.", "guhs:ring_stoofpotje", [fq.adv("verhaal_getemd_sam_guh")], deps=na)
    fq.q("ring_maatje", "Lekkere vissss", "&dSmikagol&r is nu je maatje. Zet hem bij water (buk en klik: dan blijft hij zitten) en hij vangt vis voor "
         "je. Kwijt? Het &6vissenbotje&r roept hem.", "guhs:ring_vissenbotje", [fq.adv("ring_smikagol_vis")], rewards=(("minecraft:cod", 4),), deps=na)
    fq.q("ring_feest", "Er is altijd wel iets te vieren", "In de Knabbelgouw is het nu elke dag feest. Eén &6Feestknabbel&r per dag, anders rol je de "
         "heuvel af.", "guhs:ring_feestknabbel", [fq.adv("ring_feest")], deps=na)
    fq.q("ring_wegwijs", "Op avontuur!", "Zo raak je de weg nooit kwijt: &6linksboven&r staat wat je nu moet doen, in de &dGuhdex&r (tab Verhalen) "
         "staat je &6reiskaart&r, het &6Superkompas&r wijst naar 'Mijn verhaal' en Sam-guh weet het ook. De hoofdstukken hieronder gaan één voor één open.",
         "guhs:guhdex", [fq.adv("ring_wegwijs")], deps=na)
