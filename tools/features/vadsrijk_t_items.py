"""
1.10.0 "Het Vadsrijk", night round T, slice T-ITEMS ("itemisatie 2.0"). Java: feature/vadsrijk/items.

  - pixel art (16x16, the economie painter `Doek`) for the 16 new gloves/capes/armbands, the Groeipluis and the Knuffeltalisman;
    item models + item definitions;
  - every text (Dutch; en_us gets English where cheap): the CLEAR stat names (R4, also the existing ones), the new slots,
    the smid tabs (Sterren / Bonussen / Doorgroeien / Overvadsen), Erfstukken, the drankjesriem.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_items.py
"""
import json
import os

from vadsrijk_economie import Doek, _licht

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
ITEM_TEX = "vadsrijk/items"


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ======================================================================================================================
# icons
# ======================================================================================================================
def handschoen(c, manchet=(250, 200, 60), stip=None):
    d = Doek()
    d.poly([(4, 15), (4, 7), (5, 3), (7, 3), (7, 6), (8, 2), (10, 2), (10, 6), (11, 3), (13, 4), (12, 9), (13, 6), (15, 7), (12, 12), (11, 15)], c)
    d.rect(4, 13, 11, 15, manchet)
    if stip:
        d.disc(8.5, 9, 1.4, stip)
    d.px(6, 4, _licht(c, 1.3))
    d.px(9, 3, _licht(c, 1.3))
    d.schaduw()
    return d.rand()


def cape_icoon(c, zoom=(250, 200, 60), speld=(255, 110, 180)):
    d = Doek()
    d.poly([(4, 1), (11, 1), (14, 15), (8, 13), (1, 15)], c)
    d.poly([(5, 2), (8, 2), (8, 12), (3, 14)], _licht(c, 1.15))
    d.rect(4, 1, 11, 2, zoom)
    d.disc(7.5, 3, 1.5, speld)
    d.schaduw()
    return d.rand()


def armband(c, steen=(255, 110, 180)):
    d = Doek()
    for i in range(0, 360, 6):
        import math
        a = math.radians(i)
        x, y = 7.5 + math.cos(a) * 5.5, 8.5 + math.sin(a) * 3.2
        d.px(x, y, c)
        d.px(x, y + 1, _licht(c, 0.8))
    d.disc(7.5, 12, 1.8, steen)
    d.px(7, 11, (255, 255, 255))
    d.schaduw()
    return d.rand()


def groeipluis():
    d = Doek()
    d.lijn(8, 15, 8, 8, (90, 160, 70), dik=1)
    d.disc(8, 6, 4.2, (255, 235, 140))
    d.disc(8, 6, 3, (255, 250, 220))
    for x, y in ((4, 3), (12, 3), (3, 7), (13, 7), (8, 1)):
        d.px(x, y, (255, 200, 90))
    d.px(6, 12, (120, 200, 90))
    d.px(10, 11, (120, 200, 90))
    d.schaduw()
    return d.rand()


def talisman():
    d = Doek()
    d.lijn(4, 1, 8, 5, (200, 160, 60))
    d.lijn(12, 1, 8, 5, (200, 160, 60))
    d.disc(5.5, 9, 3.2, (255, 120, 180))
    d.disc(10.5, 9, 3.2, (255, 120, 180))
    d.poly([(2.5, 10), (13.5, 10), (8, 15.5)], (255, 120, 180))
    d.disc(8, 10, 1.6, (255, 230, 120))
    d.px(5, 7, (255, 220, 240))
    d.schaduw()
    return d.rand()


# id: (nl name, en name, icon, [2 flavour lines nl])
STUKKEN = {
    "pluiswantjes": ("Pluiswantjes", "Fluff Mittens", lambda: handschoen((255, 190, 220)),
                     ["Warm, zacht en toch een stevige klap.", "Gebreid door een oma-guh."]),
    "kloddergrijpers": ("Kloddergrijpers", "Blob Grabbers", lambda: handschoen((110, 200, 240), (80, 150, 200), (200, 240, 255)),
                        ["Plakken een beetje. Dat is de bedoeling.", "Hoort bij de Klodderset."]),
    "knuffelcapeje": ("Knuffelcapeje", "Cuddle Cape", lambda: cape_icoon((255, 110, 180)),
                      ["Wappert als je rent. Heel belangrijk.", "Voor kleine helden."]),
    "sporencape": ("Sporencape", "Spore Cape", lambda: cape_icoon((150, 110, 80), (120, 200, 90), (200, 160, 120)),
                   ["Ruikt naar bos na de regen.", "Hoort bij de Zwamset."]),
    "pluisarmband": ("Pluisarmband", "Fluff Bracelet", lambda: armband((255, 190, 220)),
                     ["Kriebelt een beetje. Je slaat sneller.", "Van echt pluis!"]),
    "rozenarmbandje": ("Rozenarmbandje", "Rose Bracelet", lambda: armband((220, 80, 120), (120, 200, 90)),
                       ["Je vaardigheden zijn sneller weer klaar.", "Ruikt naar rozen."]),
    "ijswantjes": ("IJswantjes", "Ice Mittens", lambda: handschoen((200, 235, 255), (150, 200, 240)),
                   ["Koud van buiten, warm van binnen.", "Tegen elementen, niet tegen knuffels."]),
    "kristalhandschoenen": ("Kristalhandschoenen", "Crystal Gloves", lambda: handschoen((180, 140, 255), (120, 90, 200), (240, 220, 255)),
                            ["Klinken als belletjes.", "Hoort bij de Kristalset."]),
    "galmcape": ("Galmcape", "Echo Cape", lambda: cape_icoon((110, 130, 220), (200, 220, 255)),
                 ["Je hoort je eigen voetstappen twee keer.", "Uit de galmgrotten."]),
    "buitcape": ("Buitcape", "Loot Cape", lambda: cape_icoon((90, 80, 70), (250, 200, 60), (250, 200, 60)),
                 ["Mika's verloren cape. Vol kaaskruimels.", "Hoort bij de Mika-buitset."]),
    "kristalarmband": ("Kristalarmband", "Crystal Bracelet", lambda: armband((180, 140, 255), (240, 220, 255)),
                       ["Glinstert bij elke klap.", "Licht, maar o zo scherp."]),
    "bloesemhandjes": ("Bloesemhandjes", "Blossom Gloves", lambda: handschoen((255, 200, 225), (255, 150, 200), (255, 230, 120)),
                       ["Laten blaadjes achter op alles wat je raakt.", "Hoort bij de Bloesemset."]),
    "schorshandschoenen": ("Schorshandschoenen", "Bark Gauntlets", lambda: handschoen((120, 85, 55), (90, 60, 40)),
                           ["Ruw en taai, net als de Schorsstad.", "Hoort bij de Schorsset."]),
    "wortelcape": ("Wortelcape", "Root Cape", lambda: cape_icoon((90, 70, 50), (120, 200, 90), (255, 220, 120)),
                   ["Gloeit een beetje in het donker.", "Hoort bij de Wortelset."]),
    "schemercape": ("Schemercape", "Dusk Cape", lambda: cape_icoon((70, 50, 110), (255, 200, 90), (255, 200, 90)),
                    ["Gemaakt van het laatste stukje avond.", "Je loopt er net iets sneller in."]),
    "honingarmband": ("Honingarmband", "Honey Bracelet", lambda: armband((240, 180, 60), (255, 230, 120)),
                      ["Plakt niet. Echt niet. Een beetje.", "Je heals worden er zoeter van."]),
    "harsarmband": ("Harsarmband", "Resin Bracelet", lambda: armband((200, 120, 50), (255, 200, 120)),
                    ["Een druppel hars met een vliegje erin.", "Geeft je een beetje Vads terug."]),
}

# the CLEAR stat names (R4): (nl, en)
STATS = {
    "vads": ("Max. Vads", "Max Vads"),
    "kracht": ("Kracht (schade van dichtbij)", "Strength (melee damage)"),
    "behendigheid": ("Behendigheid (schade op afstand)", "Dexterity (ranged damage)"),
    "wijsheid": ("Wijsheid (magieschade)", "Wisdom (magic damage)"),
    "verdediging": ("Verdediging (minder schade)", "Defence (less damage)"),
    "kritiek": ("Kans op kritieke treffer", "Critical hit chance"),
    "kritiekschade": ("Schade van kritieke treffers", "Critical hit damage"),
    "aanvalssnelheid": ("Aanvalssnelheid", "Attack speed"),
    "loopsnelheid": ("Loopsnelheid", "Walk speed"),
    "cooldownreductie": ("Kortere wachttijd vaardigheden", "Shorter skill cooldowns"),
    "elementschade": ("Schade met je element", "Element damage"),
    "elementweerstand": ("Minder schade van elementen", "Element resistance"),
    "levensroof": ("Vads terug van je schade", "Vads back from your damage"),
    "healkracht": ("Sterkere heals", "Stronger heals"),
    "verdediging_negeren": ("Verdediging negeren", "Ignore defence"),
    "bazenschade": ("Schade tegen bazen", "Damage against bosses"),
    "leven_per_treffer": ("Leven per treffer", "Life per hit"),
    "kans_op_effect": ("Kans op effect", "Chance to apply effect"),
}

# slot: (nl, en, short)
SLOTS = {
    "ring": ("Ring", "Ring", "R"), "ring2": ("Ring 2", "Ring 2", "R2"), "ketting": ("Ketting", "Necklace", "K"),
    "oorbel": ("Oorbel", "Earring", "O"), "armband": ("Armband", "Bracelet", "A"), "handschoenen": ("Handschoenen", "Gloves", "H"),
    "cape": ("Cape", "Cape", "C"),
}

SETS = {"klodder": ("Klodderset", "Blob set"), "zwam": ("Zwamset", "Mushroom set"), "kristal": ("Kristalset", "Crystal set"),
        "mika": ("Mika-buitset", "Mika loot set"), "bloesem": ("Bloesemset", "Blossom set"), "schors": ("Schorsset", "Bark set"),
        "wortel": ("Wortelset", "Root set")}

# key: (nl, en)
TEKSTEN = {
    # kern texts made general (gloves and capes are worn like jewellery)
    "vadsrijk.kern.sieraad.te_hoog": ("Dit stuk is voor niveau %s.", "This piece is for level %s."),
    "vadsrijk.kern.sieraad.vol": ("Je rugzak is vol: het stuk blijft om.", "Your bag is full: the piece stays on."),
    "vadsrijk.kern.sieraad.leeg": ("%s: leeg. Rechtsklik een stuk of Shift+klik het in je inventaris om het om te doen.",
                                   "%s: empty. Right-click a piece or Shift+click it in your inventory to wear it."),
    "vadsrijk.kern.sieraad.hoe": ("Rechtsklik om te dragen (of Shift+klik in je inventaris).", "Right-click to wear (or Shift+click in your inventory)."),
    "item.guhs.vr_t_groeipluis": ("Groeipluis", "Growth Fluff"),
    "item.guhs.vr_t_groeipluis.uitleg": ("Van elke baas. Bij de smid laat je er een Legendarisch of Guhddelijk stuk mee doorgroeien.",
                                         "From every boss. At the smith it lets a Legendary or Guhdly piece grow."),
    "item.guhs.vr_t_knuffeltalisman": ("Knuffeltalisman", "Cuddle Talisman"),
    "item.guhs.vr_t_knuffeltalisman.uitleg": ("Bij de smid: zet hem aan en een mislukte ster kost je geen ster. Pas dan raak je hem kwijt.",
                                              "At the smith: switch it on and a failed star won't cost you a star. Only then is it used up."),
    "key.guhs.vr_t_drankje1": ("Drankjesriem 1", "Potion belt 1"),
    "key.guhs.vr_t_drankje2": ("Drankjesriem 2", "Potion belt 2"),
    "key.guhs.vr_t_drankje3": ("Drankjesriem 3", "Potion belt 3"),
    # drankjesriem
    "vadsrijk.items.riem.kort": ("Riem", "Belt"),
    "vadsrijk.items.riem.leeg": ("Vakje %s van je drankjesriem is leeg.", "Slot %s of your potion belt is empty."),
    "vadsrijk.items.riem.wacht": ("Je drankjes moeten nog even afkoelen.", "Your potions need a moment."),
    "vadsrijk.items.riem.vol": ("Je drankjesriem is vol.", "Your potion belt is full."),
    "vadsrijk.items.riem.uitleg_vol": ("Druk %s om te drinken. Klik om terug te leggen.", "Press %s to drink. Click to put it back."),
    "vadsrijk.items.riem.uitleg_leeg": ("Drankjesriem vakje %s (toets %s). Klik hier met een drankje, of Shift+klik een drankje in je inventaris.",
                                        "Potion belt slot %s (key %s). Click here holding a potion, or Shift+click a potion in your inventory."),
    # tooltip
    "vadsrijk.items.tip.vertrouwd": ("Vertrouwd %s (%s/%s xp): +%s%% basisstats", "Trusted %s (%s/%s xp): +%s%% base stats"),
    "vadsrijk.items.tip.vertrouwd_max": ("★ Vertrouwd 5: +%s%% basisstats", "★ Trusted 5: +%s%% base stats"),
    "vadsrijk.items.tip.gegroeid": ("Doorgegroeid %s", "Grown %s"),
    "vadsrijk.items.tip.kan_groeien": ("Kan doorgroeien bij de smid (Vertrouwd 5 nodig).", "Can grow at the smith (needs Trusted 5)."),
    "vadsrijk.items.tip.geduld": ("De smid is ongeduldig: +%s%% kans op je volgende ster", "The smith is impatient: +%s%% chance on your next star"),
    "vadsrijk.items.tip.set": ("Telt als stuk van de %s (in het handschoenen- of capevak).", "Counts as a piece of the %s (in the gloves or cape slot)."),
    "vadsrijk.items.vertrouwd.omhoog": ("%s is nu Vertrouwd %s!", "%s is now Trusted %s!"),
    # talisman
    "vadsrijk.items.talisman.beschermd": ("De Knuffeltalisman beschermde je ster! (talisman op)", "The Cuddle Talisman saved your star! (talisman used)"),
    "vadsrijk.items.talisman.gekocht": ("Je kocht een Knuffeltalisman.", "You bought a Cuddle Talisman."),
    # smid tabs
    "vadsrijk.items.smid.tab.sterren": ("Sterren", "Stars"),
    "vadsrijk.items.smid.tab.bonussen": ("Bonussen", "Bonuses"),
    "vadsrijk.items.smid.tab.doorgroeien": ("Groeien", "Grow"),
    "vadsrijk.items.smid.tab.overvadsen": ("Overvadsen", "Infuse"),
    "vadsrijk.items.smid.met_geduld": (" (+%s geduld)", " (+%s patience)"),
    "vadsrijk.items.smid.geduld": ("Smidguh wordt ongeduldig...", "The smith is getting impatient..."),
    "vadsrijk.items.smid.geduld_leeg": ("Geduld van de smid:", "Smith's patience:"),
    "vadsrijk.items.smid.geen_geduld": ("Vanaf ster %s helpt het geduld niet meer.", "From star %s on patience no longer helps."),
    "vadsrijk.items.smid.talisman": ("Knuffeltalisman (je hebt er %s)", "Cuddle Talisman (you have %s)"),
    "vadsrijk.items.smid.talisman_aan": ("Mislukt het? Dan verlies je geen ster.", "If it fails, you won't lose a star."),
    "vadsrijk.items.smid.koop": ("Koop 1", "Buy 1"),
    "vadsrijk.items.smid.bij_succes": ("Als het lukt:", "If it works:"),
    "vadsrijk.items.smid.schade_van_naar": ("Schade per klap %s → %s", "Damage per hit %s → %s"),
    "vadsrijk.items.smid.polijst_kies": ("Klik een bonus om hem te kunnen polijsten.", "Click a bonus to polish it."),
    "vadsrijk.items.smid.polijst_bereik": ("Polijsten: %s kan %s tot %s worden (nu %s)", "Polish: %s can become %s to %s (now %s)"),
    "vadsrijk.items.smid.knop.polijst": ("Polijsten", "Polish"),
    "vadsrijk.items.smid.doorgroei_uitleg": ("Legendarisch 2x, Guhddelijk altijd.", "Legendary 2x, Guhdly always."),
    "vadsrijk.items.smid.vertrouwd": ("Vertrouwd %s / %s", "Trusted %s / %s"),
    "vadsrijk.items.smid.level_van_naar": ("Niveau %s → %s", "Level %s → %s"),
    "vadsrijk.items.smid.doorgroei_gevolg": ("Daarna: Gebonden, Vertrouwd begint opnieuw.", "After: Bound, Trusted starts over."),
    "vadsrijk.items.smid.knop.doorgroei": ("Laat groeien!", "Let it grow!"),
    "vadsrijk.items.smid.overvads_uitleg": ("Kies een oud stuk om één bonus van over te nemen:", "Pick an old piece to take one bonus from:"),
    "vadsrijk.items.smid.overvads_geen": ("Je hebt geen ander stuk met bonussen bij je.", "You carry no other piece with bonuses."),
    "vadsrijk.items.smid.overvads_kies_offer": ("Klik hierboven een oud stuk aan.", "Click an old piece above."),
    "vadsrijk.items.smid.overvads_van": ("Van het oude stuk:", "From the old piece:"),
    "vadsrijk.items.smid.overvads_naar": ("Op de plek van:", "In place of:"),
    "vadsrijk.items.smid.overvads_erbij": ("(een nieuwe erbij)", "(add a new one)"),
    "vadsrijk.items.smid.overvads_wordt": ("Wordt: %s %s (%s kans op een lieve mislukking: %s)", "Becomes: %s %s (%s chance of a sweet miss: %s)"),
    "vadsrijk.items.smid.overvads_weg": ("Het oude stuk verdwijnt. Dit stuk blijft altijd heel.", "The old piece is used up. This piece always stays whole."),
    "vadsrijk.items.smid.knop.overvads": ("Overvadsen", "Infuse"),
    "vadsrijk.items.smid.groot.gepolijst": ("Gepolijst!", "Polished!"),
    "vadsrijk.items.smid.groot.doorgegroeid": ("Doorgegroeid!", "It grew!"),
    "vadsrijk.items.smid.groot.overgevadst": ("Overgevadst!", "Infused!"),
    "vadsrijk.items.smid.groot.lief": ("Lieve mislukking", "Sweet miss"),
    # smid outcomes / reasons
    "vadsrijk.items.polijst.kies": ("Kies eerst een bonus.", "Pick a bonus first."),
    "vadsrijk.items.polijst.klaar": ("Opgepoetst! Kijk wat de bonus nu is.", "Polished! See what the bonus is now."),
    "vadsrijk.items.doorgroei.verpluisd": ("Laat het eerst repareren.", "Have it repaired first."),
    "vadsrijk.items.doorgroei.op": ("Dit Legendarische stuk is al twee keer doorgegroeid.", "This Legendary piece already grew twice."),
    "vadsrijk.items.doorgroei.zeldzaamheid": ("Alleen Legendarische en Guhddelijke stukken kunnen doorgroeien.", "Only Legendary and Guhdly pieces can grow."),
    "vadsrijk.items.doorgroei.cap": ("Dit stuk is al op het hoogste niveau.", "This piece is already at the top level."),
    "vadsrijk.items.doorgroei.vertrouwd": ("Draag het eerst tot Vertrouwd 5 (versla wezens terwijl je het draagt).",
                                           "Wear it to Trusted 5 first (cheer up creatures while you wear it)."),
    "vadsrijk.items.doorgroei.geen_pluis": ("Je hebt niet genoeg Groeipluis (van bazen).", "You don't have enough Growth Fluff (from bosses)."),
    "vadsrijk.items.doorgroei.klaar": ("Je stuk is doorgegroeid!", "Your piece grew!"),
    "vadsrijk.items.overvads.verpluisd": ("Een verpluisd stuk kan dat niet. Eerst repareren.", "A fluffed piece can't. Repair it first."),
    "vadsrijk.items.overvads.kies": ("Kies een bonus van het oude stuk.", "Pick a bonus of the old piece."),
    "vadsrijk.items.overvads.kies_offer": ("Kies een ander stuk als offer.", "Pick another piece to offer."),
    "vadsrijk.items.overvads.vol": ("Er past geen bonus meer bij: kies er een om te vervangen.", "No room for another bonus: pick one to replace."),
    "vadsrijk.items.overvads.past_niet": ("Die bonus kan niet op dit soort stuk.", "That bonus can't go on this kind of piece."),
    "vadsrijk.items.overvads.dubbel": ("Dit stuk heeft die bonus al.", "This piece already has that bonus."),
    "vadsrijk.items.overvads.klaar": ("Overgevadst! De bonus zit nu op je stuk.", "Infused! The bonus is on your piece now."),
    "vadsrijk.items.overvads.lief": ("Een lieve mislukking: de bonus kwam over, maar iets lager.", "A sweet miss: the bonus came over, a bit lower."),
}


# ======================================================================================================================
def alle_teksten():
    """{key: (nl, en)}"""
    t = dict(TEKSTEN)
    for sid, (nl, en) in STATS.items():
        t[f"vadsrijk.kern.stat.{sid}"] = (nl, en)
    for sid, (nl, en, kort) in SLOTS.items():
        t[f"vadsrijk.kern.slot.{sid}"] = (nl, en)
        t[f"vadsrijk.kern.slot.{sid}.kort"] = (kort, kort)
    for sid, (nl, en) in SETS.items():
        t[f"vadsrijk.items.set.{sid}"] = (nl, en)
    for iid, (nl, en, _icoon, fl) in STUKKEN.items():
        t[f"item.guhs.vr_t_{iid}"] = (nl, en)
        t[f"vadsrijk.loot.{iid}"] = (nl, en)
        for i, regel in enumerate(fl):
            t[f"vadsrijk.loot.{iid}.flavour.{i}"] = ('"' + regel + '"', '"' + regel + '"')
    return t


def _item(naam, doek):
    doek.png(os.path.join(A, "textures", "item", ITEM_TEX, f"{naam}.png"))
    _schrijf(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}})
    _schrijf(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def _assets():
    for iid, (_nl, _en, icoon, _fl) in STUKKEN.items():
        _item(f"vr_t_{iid}", icoon())
    _item("vr_t_groeipluis", groeipluis())
    _item("vr_t_knuffeltalisman", talisman())


def build(h):
    _assets()
    for k, (nl, en) in alle_teksten().items():
        h.lang(k, en, nl)


def _direct():
    _assets()
    t = alle_teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf(p, data)
    print(f"vadsrijk_t_items: {len(t)} texts, {len(STUKKEN) + 2} item icons")


if __name__ == "__main__":
    _direct()
