"""
bbq2 (bank): the Bank Guh's cap and upgrade, the Hapluikje and its link key. Java: feature/bank (+ storage/Bank*,
menu/BankGuhMenu, block/BankGuhBlock, block/entity/BankGuhBlockEntity, client/screen/BankGuhScreen, item/BankGuhItem).

A Bank Guh holds at most 256 of one kind of item (BankStorage.CAP) until it got the upgrade. This module makes:
  - block hapluikje: a guh machine (features/vadskracht.py machine(): face asleep / happy / surprised, two ears) whose
    hatch is a mouth: shut while it sleeps, wide open with two little teeth and a tongue while it has vadskracht, shut
    tight when it had to refuse something. Zout tier recipe (needs guhs:zoutkristal)
  - item bank_sleutel (the link key: a golden key with guh ears; Zout tier recipe) and item bank_upgrade ("Bodemloos
    Knabbelmaagje": a pink tummy with a starry hole in it; NO recipe: the uitvinder-guh gives it, tech-quests)
  - the texts of all three, the new texts of the Bank Guh's screen and tooltip (the old bank texts and the bank's loot
    table and blockstate are in make_resources.py, also this slice's), the hidden advancements of the FTB quests, two
    visible advancements in the tab Guh-technologie, the FTB section "Het Hapluikje & het knabbelmaagje" (chapter
    guhs_guhmensie, after the Bank Guh's own quests), the game test room bank_test_kamer
"""
import os
import re

from features import bbq2, vadskracht

# the section of this module in its FTB chapter (make_ftbquests.py FTB_SECTIES): (sid, title, portrait, keys or None = the rest)
FTB_SECTIES = [("bank", "Het Hapluikje & het knabbelmaagje", "item:guhs:bank_guh", None)]

LUIKJE = "hapluikje"
SLEUTEL = "bank_sleutel"
UPGRADE = "bank_upgrade"

ROZE = (238, 150, 190)           # the luikje itself
GOUD = (250, 204, 84)            # its hatch (and the key)
BEK = (58, 24, 44)               # inside the open mouth
TONG = (244, 110, 150)
TAND = (255, 250, 240)


def cap():
    """BankStorage.CAP, read from the Java source (so a text never says another number than the game uses)."""
    pad = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "storage", "BankStorage.java")
    m = re.search(r"int CAP = (\d+);", open(pad, encoding="utf-8").read())
    if not m:
        raise SystemExit("bank: BankStorage.CAP not found")
    return int(m.group(1))


# =====================================================================================================================
# textures
# =====================================================================================================================
def _bek(img, staat):
    """The hatch of the Hapluikje (rows 9..13, columns 4..11 of the front, under the face): its mouth."""
    px = img.load()
    donker = tuple(int(c * 0.6) for c in GOUD) + (255,)
    licht = tuple(min(255, int(c * 1.12)) for c in GOUD) + (255,)
    goud = GOUD + (255,)
    hoeken = ((4, 9), (11, 9), (4, 13), (11, 13))
    for y in range(9, 14):
        for x in range(4, 12):
            if (x, y) in hoeken:
                continue
            if staat == "werkt":
                px[x, y] = BEK + (255,)                   # wide open
            else:
                px[x, y] = donker if staat == "slaapt" else goud
    if staat == "werkt":
        for x in (6, 9):                                  # two little teeth
            px[x, 9] = TAND + (255,)
        for x in range(6, 10):                            # the tongue
            px[x, 13] = TONG + (255,)
        px[7, 12] = px[8, 12] = TONG + (255,)
        for x in range(5, 11):                            # the lifted hatch: a golden rim above the mouth
            px[x, 8] = goud
    else:
        for x in range(5, 11):                            # the hinge and the seam of the shut hatch
            px[x, 9] = licht if staat == "vol" else goud
            px[x, 11] = tuple(int(c * 0.45) for c in GOUD) + (255,)
        px[7, 12] = px[8, 12] = licht                     # the little knob


SLEUTEL_PIXELS = [
    "................",
    "..kk....kk......",
    ".kppk..kppk.....",
    ".kpqkkkkqpk.....",
    "..kgyyyygk......",
    ".kgykkkkygk.....",
    ".kgyk..kggk.....",
    ".kggk..kgdk.....",
    ".kggykkgddk.....",
    "..kgggggdk......",
    "...kkkkgygk.....",
    ".......kgygk....",
    "........kgygkk..",
    ".........kgyggk.",
    "..........kdkdk.",
    "...........k.k..",
]
UPGRADE_PIXELS = [
    "................",
    ".....kkkkkk.....",
    "...kkppppppkk...",
    "..kpwwppppppqk..",
    ".kpwppkkkkpppqk.",
    ".kppkkvvvvkkpqk.",
    "kppkvvnnnnvvkpqk",
    "kppkvnnsnnnvkpqk",
    "kppkvnnnnnsvkpqk",
    "kppkvnsnnnnvkpqk",
    "kppkvvnnnnvvkpqk",
    ".kppkkvvvvkkpqk.",
    ".kpppqkkkkqqqqk.",
    "..kpqqqqqqqqqk..",
    "...kkqqqqqqkk...",
    ".....kkkkkk.....",
]
PALET = {"k": (92, 44, 72, 255), "p": (255, 176, 208, 255), "q": (226, 126, 170, 255), "w": (255, 226, 238, 255),
         "g": (250, 204, 84, 255), "y": (255, 238, 150, 255), "d": (206, 150, 40, 255),
         "v": (120, 70, 170, 255), "n": (44, 22, 66, 255), "s": (255, 244, 180, 255)}


def textures(h):
    h.save(h.grid(SLEUTEL_PIXELS, PALET), "item", f"{SLEUTEL}.png")
    h.save(h.grid(UPGRADE_PIXELS, PALET), "item", f"{UPGRADE}.png")


# =====================================================================================================================
# blocks, items, recipes, advancements
# =====================================================================================================================
def blocks_and_items(h):
    # the placeholder cube of bbq2.py (question mark) is not used any more
    for oud in (f"{h.A}/models/block/{LUIKJE}.json", os.path.join(h.TEX, "block", f"{LUIKJE}.png")):
        if os.path.exists(oud):
            os.remove(oud)
    vadskracht.machine(h, LUIKJE, ROZE, GOUD, voor=_bek)
    h.item_model(SLEUTEL)
    h.item_model(UPGRADE)
    # Zout tier (CONTRACT_130 7): both need a zoutkristal from the Barbecuether. The upgrade has no recipe.
    h.shaped(LUIKJE, ["IZI", "ILI", "IKI"], {"I": "minecraft:iron_ingot", "Z": "guhs:zoutkristal", "L": "#minecraft:wooden_trapdoors",
                                             "K": "guhs:kaas_knabbels"}, f"guhs:{LUIKJE}", 1)
    h.shaped(SLEUTEL, ["Z", "G", "K"], {"Z": "guhs:zoutkristal", "G": "minecraft:gold_ingot", "K": "guhs:kaas_knabbels"}, f"guhs:{SLEUTEL}", 1)


def advancements(h):
    for naam in ("bank_vol", "bank_opgevoerd", "bank_gekoppeld", "bank_gehapt"):
        bbq2.verborgen(h, naam)
    bbq2.zichtbaar(h, "techniek", "bank_gehapt", "root", f"guhs:{LUIKJE}", "task", "Hap!",
                   "Voer een Hapluikje iets: het ligt meteen in je Bankguh, hoe ver die ook weg staat")
    bbq2.zichtbaar(h, "techniek", "bank_opgevoerd", "bank_gehapt", f"guhs:{UPGRADE}", "goal", "Bodemloos buikje",
                   "Geef je Bankguh het Bodemloos Knabbelmaagje: nu past er van alles oneindig veel in. Vahoeg!")


def test_templates(h):
    """bank_test_kamer: 7 x 5 x 7 with a stone floor."""
    s = h.Structure((7, 5, 7))
    s.fill(0, 0, 0, 6, 0, 6, "minecraft:stone")
    s.save("bank_test_kamer")


# =====================================================================================================================
# texts
# =====================================================================================================================
def _teksten():
    n = cap()
    vk = vadskracht.getal("HAPLUIKJE")
    return {
        # the Bank Guh: tooltip and screen (block.guhs.bank_guh, .lore, .contents and the old screen texts: make_resources.py)
        "block.guhs.bank_guh.opgevoerd": "Met Bodemloos Knabbelmaagje: van alles past er oneindig veel in. Vahoeg!",
        "block.guhs.bank_guh.volste": "Volste soort: %s/%s",
        "gui.guhs.bank.cap": "Hooguit %s van elke soort. Vol is vol, njeg!",
        "gui.guhs.bank.opgevoerd": "Bodemloos Knabbelmaagje: van alles past er oneindig veel in!",
        "gui.guhs.bank.vol": "Vol! Van %s past er niks meer bij (hooguit %s)",
        "gui.guhs.bank.stored.cap": "In het buikje: %s/%s",
        "gui.guhs.bank.stored.vol": "Vol! Hier past niks meer van bij, njeg",
        "gui.guhs.bank.stored.te_veel": "Meer dan %s, nog van vroeger. Eruit halen mag altijd, erbij stoppen even niet",
        # the upgrade
        f"item.guhs.{UPGRADE}": "Bodemloos Knabbelmaagje",
        f"item.guhs.{UPGRADE}.lore": "Klik ermee op je Bankguh: dan past er niet meer hooguit %s van elke soort in zijn buikje, maar oneindig veel",
        f"item.guhs.{UPGRADE}.lore.blijft": "Het blijft voor altijd bij die Bankguh, ook als je hem oppakt en ergens anders neerzet",
        f"item.guhs.{UPGRADE}.lore.uit": "Daarna mag een Filterstuk er ook spullen uit happen. Trechters en gewone buizen krijgen niks, njeg",
        f"item.guhs.{UPGRADE}.gelukt": "Slok! Het buikje van je Bankguh is nu bodemloos. Vahoeg!",
        f"item.guhs.{UPGRADE}.al": "Deze Bankguh heeft al een Bodemloos Knabbelmaagje. Twee buikjes is te vadsig, njeg",
        # the link key
        f"item.guhs.{SLEUTEL}": "Banksleutel",
        f"item.guhs.{SLEUTEL}.lore": "Klik eerst op je Bankguh en dan op elk Hapluikje dat voor hem moet happen. De sleutel raakt nooit op",
        f"item.guhs.{SLEUTEL}.leeg": "Kent nog geen Bankguh",
        f"item.guhs.{SLEUTEL}.gebonden": "Kent een Bankguh. Njeg!",
        f"item.guhs.{SLEUTEL}.gebonden.plek": "Kent de Bankguh die bij %s, %s, %s stond (%s)",
        f"item.guhs.{SLEUTEL}.onthouden": "De sleutel kent nu de Bankguh bij %s, %s, %s. Klik ermee op een Hapluikje!",
        # the Hapluikje
        f"block.guhs.{LUIKJE}": "Hapluikje",
        f"block.guhs.{LUIKJE}.lore": "Hap! Alles wat je erin stopt ligt meteen in je Bankguh, hoe ver die ook weg staat. Koppel het met een "
                                     "Banksleutel. Het hapt alleen: er komt nooit iets uit",
        f"block.guhs.{LUIKJE}.lore.kracht": "Heeft %s vadskracht nodig. Zonder vadskracht doet het een dutje",
        f"block.guhs.{LUIKJE}.slaapt": "Het Hapluikje doet een dutje: het heeft %s vadskracht nodig. Njeg...",
        f"block.guhs.{LUIKJE}.los": "Dit Hapluikje hoort nog bij geen enkele Bankguh. Klik met een Banksleutel eerst op je Bankguh en dan hier",
        f"block.guhs.{LUIKJE}.weg": "De Bankguh van dit luikje staat nergens. Zet hem weer neer, dan hapt het luikje verder",
        f"block.guhs.{LUIKJE}.klaar": "Dit luikje hapt voor de Bankguh bij %s, %s, %s (%s). Stop er maar iets in!",
        f"block.guhs.{LUIKJE}.hap": "Hap! %s× %s ligt in je Bankguh",
        f"block.guhs.{LUIKJE}.vol": "Je Bankguh zit vol met %s (%s/%s). Het luikje houdt zijn bekje dicht!",
        f"block.guhs.{LUIKJE}.geleend": "Geleende spulletjes lust het Hapluikje niet: die zijn niet van jou, njeg",
        f"block.guhs.{LUIKJE}.sleutel_leeg": "Deze Banksleutel kent nog geen Bankguh. Klik er eerst mee op je Bankguh",
        f"block.guhs.{LUIKJE}.gekoppeld": "Gekoppeld! Dit luikje hapt nu voor de Bankguh bij %s, %s, %s (%s)",
        f"block.guhs.{LUIKJE}.gekoppeld.weg": "Gekoppeld! Maar die Bankguh staat nu nergens: zet hem neer, dan hapt het luikje",
        # the hover readout of the Hapluikje (under the vadskracht lines)
        "gui.guhs.bank.luikje.los": "Nog niet gekoppeld: klik met een Banksleutel",
        "gui.guhs.bank.luikje.weg": "Zijn Bankguh staat nergens, dus het hapt niet",
        "gui.guhs.bank.luikje.klaar": "Hapt voor de Bankguh bij %s, %s, %s (%s)",
    }, n, vk


def texts(h):
    teksten, _, _ = _teksten()
    for key, nl in teksten.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# FTB quests: section "Het Hapluikje & het knabbelmaagje" of the chapter guhs_guhmensie (no locking)
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    n = cap()
    vk = vadskracht.getal("HAPLUIKJE")
    q("bank_vol", "Vol is vol, njeg", f"Hoe vadsig hij ook is: van &eelke soort&r past er hooguit &e{n}&r in het buikje van een &dBank Guh&r. "
      f"Zijn scherm laat het zien (&e{n}/{n}&r wordt rood) en wat er niet meer bij past houd je gewoon zelf. Vul er eentje tot hij van iets "
      "vol zit. Had je bank al meer dan dat? Geen paniek: er raakt niks kwijt, je kunt het er altijd uit halen.",
      "guhs:bank_guh", [adv("bank_vol")], rewards=(("guhs:kaas_knabbels", 8),), deps=["bank_plaatsen"], xp=50)
    q("bank_opgevoerd", "Het Bodemloos Knabbelmaagje", "De &duitvinder-guh&r in de &6Oude Guhrad-centrale&r (Guhbarbecuether) heeft iets "
      "geknutseld: het &dBodemloos Knabbelmaagje&r. Help hem met zijn oefenhal en je krijgt er een. Klik ermee op je Bank Guh en er past van "
      "alles &eoneindig veel&r in. Het blijft bij die bank, ook als je hem oppakt. Pas dan kan een &eKnabbelbuis&r er ook spullen uit halen, "
      "en alleen met een &dFilterstuk&r: daarop zet jij wat eruit mag en hoeveel er moet blijven liggen. Een trechter of een gewoon "
      "Richtingstuk krijgt nooit iets uit je bank.",
      f"guhs:{UPGRADE}", [adv("bank_opgevoerd")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), deps=["bank_vol"], shape="rsquare", xp=150)
    q("bank_sleutel", "De Banksleutel", "Maak een &dBanksleutel&r (een zoutkristal, een goudstaaf en een kaasknabbel) en klik ermee op je "
      "&dBank Guh&r: de sleutel kent hem nu. Hij raakt nooit op, dus je koppelt er zoveel Hapluikjes mee als je wilt.",
      f"guhs:{SLEUTEL}", [item(f"guhs:{SLEUTEL}")], rewards=(("guhs:kaas_knabbels", 8),), deps=["bank_plaatsen"], xp=50)
    q("bank_hapluikje", "Het Hapluikje", f"Maak een &dHapluikje&r: een machientje met een bekje. Het heeft &e{vk} vadskracht&r nodig (zet het "
      "naast een Guhrad met een guh erin, of sluit het aan met Guhdraad). Zonder vadskracht doet het een dutje.",
      f"guhs:{LUIKJE}", [item(f"guhs:{LUIKJE}")], rewards=(("guhs:kaas_knabbels", 8),), deps=["bank_sleutel"], xp=50)
    q("bank_gekoppeld", "Sleutel erop", "Klik met de Banksleutel (die je Bank Guh al kent) op het Hapluikje. Vanaf nu hapt dat luikje voor "
      "precies die ene bank. Meer luikjes voor dezelfde bank? Mag! Kijk naar het luikje en je leest voor welke bank het hapt.",
      f"guhs:{SLEUTEL}", [adv("bank_gekoppeld")], rewards=(("guhs:kaas_knabbels", 8),), deps=["bank_hapluikje"], xp=50)
    q("bank_gehapt", "Hap!", "Klik met iets in je hand op het Hapluikje (of zet er een trechter of Knabbelbuis op): &dhap&r, het ligt in je "
      "Bank Guh. Hoe ver weg die ook staat, zelfs in een &eandere dimensie&r. Het luikje hapt alleen: er komt nooit iets uit. Staat je bank "
      "nergens of zit hij vol met dat ding? Dan houdt het luikje zijn bekje dicht en houd jij je spullen.",
      f"guhs:{LUIKJE}", [adv("bank_gehapt")], rewards=(("guhs:gefrituurde_kaasknabbels", 2),), deps=["bank_gekoppeld"], shape="circle", xp=100)


def selfcheck(h):
    A, D = h.A, h.D
    paden = [f"{A}/blockstates/{LUIKJE}.json", f"{A}/models/item/{LUIKJE}.json", f"{A}/models/item/{SLEUTEL}.json",
             f"{A}/models/item/{UPGRADE}.json", f"{D}/loot_table/blocks/{LUIKJE}.json", f"{D}/recipe/{LUIKJE}.json",
             f"{D}/recipe/{SLEUTEL}.json", f"{D}/structure/bank_test_kamer.nbt"]
    paden += [f"{A}/models/block/{LUIKJE}_{s}.json" for s in vadskracht.STATEN]
    paden += [f"{D}/advancement/quest/{n}.json" for n in ("bank_vol", "bank_opgevoerd", "bank_gekoppeld", "bank_gehapt")]
    paden += [os.path.join(h.TEX, "item", f"{n}.png") for n in (SLEUTEL, UPGRADE)]
    missing = [p for p in paden if not os.path.exists(p)]
    if os.path.exists(f"{D}/recipe/{UPGRADE}.json"):
        missing.append("the upgrade must not have a recipe")
    teksten, n, _ = _teksten()
    missing += [k for k in teksten if k not in h.NL]
    # the bank's loot table (make_resources.py) must carry the stomach, the upgrade and the id onto the item
    import json
    loot = json.dumps(json.load(open(f"{D}/loot_table/blocks/bank_guh.json", encoding="utf-8")))
    missing += [c for c in ("guhs:bank_contents", "guhs:bank_opgevoerd", "guhs:bank_id") if c not in loot]
    if missing:
        raise SystemExit(f"bank: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    advancements(h)
    test_templates(h)
    texts(h)
    selfcheck(h)
