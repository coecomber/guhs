"""
1.10.0 "Het Vadsrijk" zone 2, slice "systemen" (Z2_BRIEF.md). Java: feature/vadsrijk/zone2/systemen.

  - pixel art (16x16, the economie painter `Doek`) for the 6 raw crystals, the 6 zone-2 weapons and the 3 tier-2 potions;
  - item models + item definitions;
  - every text (Dutch; en_us gets the same Dutch): crystals, sockets, juwelier, Bevroren, the tree rows, the 3rd job advancement.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z2_systemen.py
"""
import json
import math
import os

from vadsrijk_economie import Doek, _licht, zwaard, boog, staf, drankje

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
ITEM_TEX = "vadsrijk/zone2/systemen"


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _hex(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


# ======================================================================================================================
# icons
# ======================================================================================================================
def kristal(kleur, vorm=0):
    """A raw crystal cluster: one big point and two small ones, a light facet, a sparkle."""
    c = _hex(kleur)
    d = Doek()
    d.poly([(5, 15), (3, 9), (6, 3), (9, 9), (8, 15)], _licht(c, 0.8))
    d.poly([(8, 15), (8, 6), (10.5, 0.5), (13, 6), (12, 15)], c)
    d.poly([(10.5, 0.5), (10.5, 15), (8, 15), (8, 6)], _licht(c, 1.25))
    if vorm % 2 == 0:
        d.poly([(1, 15), (1, 12), (3, 10), (4.5, 12), (4, 15)], _licht(c, 0.65))
    else:
        d.poly([(12, 15), (13, 11), (15, 9), (15, 15)], _licht(c, 0.65))
    d.px(10, 3, (255, 255, 255))
    d.px(5, 6, (255, 255, 255))
    d.px(2, 4, (255, 250, 220))
    d.px(14, 2, (255, 250, 220))
    d.schaduw()
    return d.rand()


def hamer(kop, steel=(150, 100, 70)):
    d = Doek()
    d.lijn(3, 14, 10, 7, steel, dik=2)
    d.poly([(7, 2), (13, 8), (10, 11), (4, 5)], kop)
    d.poly([(7, 2), (10, 5), (7, 8), (4, 5)], _licht(kop, 1.25))
    d.px(9, 4, (255, 255, 255))
    d.px(11, 7, (255, 255, 255))
    return d.rand()


def kruisboog(hout, boog_kleur):
    d = Doek()
    d.lijn(3, 13, 12, 4, hout, dik=2)
    for t in range(24):
        hoek = t / 23 * math.pi
        x = 9 + math.cos(hoek + math.pi * 0.75) * 5.5
        y = 7 + math.sin(hoek + math.pi * 0.75) * 5.5
        d.px(x, y, boog_kleur)
    d.lijn(5, 3, 13, 11, (240, 240, 250))
    d.px(13, 3, (255, 255, 255))
    d.px(12, 4, _licht(boog_kleur, 1.3))
    return d.rand()


def boek(kaft, hoek_kleur):
    d = Doek()
    d.rect(3, 2, 12, 14, kaft)
    d.rect(4, 3, 11, 13, _licht(kaft, 1.15))
    d.rect(12, 3, 13, 14, (250, 245, 235))
    d.rect(3, 2, 4, 14, _licht(kaft, 0.7))
    d.disc(8, 8, 2.4, hoek_kleur)
    d.px(7, 7, (255, 255, 255))
    for x, y in ((6, 4), (10, 12), (5, 11)):
        d.px(x, y, (255, 250, 220))
    return d.rand()


def _kristaltop(kleur):
    c = _hex(kleur)

    def top(d):
        d.poly([(11, 0), (14, 4), (11.5, 8), (8.5, 4)], c)
        d.poly([(11, 0), (11.5, 8), (8.5, 4)], _licht(c, 1.25))
        d.px(10, 3, (255, 255, 255))
        d.px(14, 7, (255, 250, 220))
    return top


def chocomelk():
    d = Doek()
    d.rect(3, 6, 11, 14, (250, 240, 235))
    d.rect(4, 7, 10, 13, (255, 250, 248))
    d.rect(4, 6, 10, 7, (120, 70, 45))
    d.lijn(12, 8, 14, 9, (250, 240, 235))
    d.lijn(14, 9, 12, 12, (250, 240, 235))
    d.disc(7, 5, 2, (255, 255, 255))          # whipped cream
    d.px(6, 10, (40, 26, 40))
    d.px(8, 10, (40, 26, 40))
    d.px(5, 11, (255, 140, 180))
    d.px(9, 11, (255, 140, 180))
    for i, (x, y) in enumerate(((5, 2), (7, 1), (9, 2))):  # steam
        d.px(x, y, (230, 230, 240))
    return d.rand()


KRISTALLEN = {
    # id: (name, colour, what it does (flavour line), Java Kristallen.SOORTEN)
    "rozekwarts": ("Rozekwarts", 0xFF8AC8, "Warm roze en een beetje plakkerig. Geeft je meer Vads."),
    "zonnesteen": ("Zonnesteen", 0xFFC84A, "Vangt het laatste zonnetje van de dag. Je klappen vinden vaker de zwakke plek."),
    "galmkristal": ("Galmkristal", 0x9AD8FF, "Zingt zachtjes terug als je ertegen tikt. Je slaat sneller."),
    "spiegelkwarts": ("Spiegelkwarts", 0xE4E8F0, "Je ziet jezelf erin, maar dan dapperder. Meer Verdediging."),
    "vonkkristal": ("Vonkkristal", 0xFF7A4A, "Knettert in je zak. Je elementen worden sterker."),
    "ijsglans": ("IJsglans", 0x7AF0F0, "Altijd koud, ook in de chocomelk. Soms bevriest wat je raakt."),
}

WAPENS = {
    # id: (name, icon(), [3 flavour lines])
    "kristalkling": ("Kristalkling", lambda: zwaard((150, 220, 255), (255, 140, 200)), [
        "Gesmeed uit een kristalnaald die niet wilde zingen. Nu zingt hij wel.",
        "Ting! Ting! Au. Ting!",
        "Glinstert zo hard dat de Galmwolf ervan moet niezen.",
    ]),
    "galmhamer": ("Galmhamer", lambda: hamer((255, 150, 90)), [
        "Elke klap galmt nog drie dalen verder.",
        "Zwaar, traag en heel erg overtuigend.",
        "De smid van Kandijkom gebruikt hem ook om noten te kraken.",
    ]),
    "ijspeesboog": ("IJspeesboog", lambda: boog((120, 190, 230), (230, 250, 255)), [
        "De pees is van bevroren knuffelwol. Niet aan likken.",
        "Schiet snel. Je vingers worden er wel koud van.",
        "Een Hemelvalk zou er jaloers op zijn.",
    ]),
    "splinterkruisboog": ("Splinterkruisboog", lambda: kruisboog((110, 90, 80), (140, 220, 120)), [
        "Gebouwd van een Mika-boortoren. Ze willen hem terug.",
        "Eén schot, heel veel splinters.",
        "Laadt langzaam, raakt hard, zegt niks.",
    ]),
    "kristalstaf": ("Kristalstaf", lambda: staf(_kristaltop(0xFF8AC8)), [
        "Een stukje roze kwarts uit de muur van Kandijkom. Niemand mist het. Bijna niemand.",
        "Gloeit als je aan chocomelk denkt.",
        "Toveren met kristal: net als gewoon toveren, maar glimmender.",
    ]),
    "poollichtboek": ("Poollichtboek", lambda: boek((60, 110, 120), (140, 255, 200)), [
        "Elke bladzijde is een stukje noorderlicht. Lees het 's nachts.",
        "Hoofdstuk 1: hoe je warm blijft. Hoofdstuk 2: vuur. Veel vuur.",
        "Ruikt naar dennen en warme sokken.",
    ]),
}

DRANKEN = {
    "vr_z2_grote_vadsdrank": ("Groot Vadsdrankje", lambda: drankje((255, 70, 150)), "Geeft je 700 Vads terug.", "Glug glug glug... Vahoeg!"),
    "vr_z2_grote_energiedrank": ("Groot Energiedrankje", lambda: drankje((90, 230, 255)), "Geeft je 120 energie terug.", "Bzzzt! Helemaal opgeladen!"),
    "vr_z2_warme_chocomelk": ("Warme Chocomelk", chocomelk,
                              "Geeft je 250 Vads, ontdooit je (Bevroren is weg) en houdt je 3 minuten warm (je bevriest niet).",
                              "Mmm, warme chocomelk. Je tenen ontdooien."),
}

HANDHELD = set(WAPENS)

# ======================================================================================================================
# texts
# ======================================================================================================================
S = "vadsrijk.z2.systemen."
TEKSTEN = {
    # crystals + sockets
    S + "kristal.naam": "%s %s",
    S + "kwaliteit.ruw": "ruw",
    S + "kwaliteit.geslepen": "geslepen",
    S + "kwaliteit.stralend": "stralend",
    S + "kwaliteit.bijv.ruw": "Ruwe",
    S + "kwaliteit.bijv.geslepen": "Geslepen",
    S + "kwaliteit.bijv.stralend": "Stralende",
    S + "kristal.uitleg": "De juweliersguh in Kandijkom zet dit in een sokje van je uitrusting.",
    S + "kristal.ijsglans.effect": "%s%% kans dat wat je raakt Bevroren raakt",
    S + "sokje.leeg": "◇ Leeg sokje",
    # juwelier
    S + "juwelier.titel": "De Glinsterkist",
    S + "juwelier.gear": "Spullen met sokjes",
    S + "juwelier.kristallen": "Jouw kristallen",
    S + "juwelier.geen_gear": "Niks met sokjes.",
    S + "juwelier.geen_kristal": "Geen kristallen.",
    S + "juwelier.kies": "Kies links iets met sokjes. Dan zet ik er een kristal in!",
    S + "juwelier.kies_kristal": "Kies eerst links een kristal.",
    S + "juwelier.aantal": "%s sokje(s)",
    S + "juwelier.zet": "Inzetten",
    S + "juwelier.haal": "Uithalen",
    S + "juwelier.uitleg": "Inzetten is gratis. Uithalen kost een paar munten, en je krijgt je kristal altijd terug.",
    S + "juwelier.gezet": "Klik! Het kristal zit erin: %s glinstert.",
    S + "juwelier.gehaald": "Voorzichtig eruit gewipt (%s). Je kristal zit weer in je tas.",
    S + "juwelier.vol": "Dat sokje is al vol.",
    S + "juwelier.weg": "Dat ligt niet meer in je tas.",
    S + "juwelier.te_weinig": "Uithalen kost %s. Dat heb je nog niet.",
    # Bevroren (kern status id, text from this slice)
    "vadsrijk.kern.status.bevroren": "Bevroren",
    # shop
    "vadsrijk.economie.winkel.z2_kandijkraam": "De Kandijkraam",
    # tree screen
    "vadsrijk.klassen.scherm.rij_lv": "Nv %s",
    "vadsrijk.klassen.scherm.pas_op": "Deze rij gaat open op niveau %s.",
    "vadsrijk.klassen.boom.nee.level": "Deze rij gaat pas open op een hoger niveau.",
    # 3rd advancement
    S + "adv3.klaar": "✦ Niveau 30! Je bent klaar voor je derde job-advancement tot %s! Ga naar de kristalkapel in Kandijkom of de ordetempel in Zwamzoet.",
    S + "tip.adv3.titel": "Derde job-advancement!",
    S + "tip.adv3": "Op niveau 30 wacht je laatste ceremonie: een nieuwe titel en een nieuwe kracht. In de kristalkapel van Kandijkom of in de ordetempel.",
}

ADV3 = {
    # klasse: (titel, onder, uitleg)
    "knuffelridder": ("Kristalkampioen", "Nieuw: Kristalwal!",
                      "Kristalwal: zolang je Knuffelschild op staat, bevriest elke vijand die jou raakt 3 s. Kussenklap doet 40 % meer schade "
                      "op bevroren vijanden. Plus Kracht, Verdediging en Vads."),
    "pluisjager": ("Poolstervalk", "Nieuw: IJsster!",
                   "IJsster: elk 5e gewone schot dat raakt, schiet een ijsster mee (1x wapenschade, 2 s Bevroren). Sterrenschot bevriest alles "
                   "wat hij doorboort 4 s. Plus Behendigheid en Kritiek."),
    "vadsmagier": ("Kristalorakel", "Nieuw: Spiegelecho!",
                   "Spiegelecho: Roze Vuurbal bevriest 1,5 s. Raakt hij een vijand die al bevroren was, dan spat het ijs in een tweede "
                   "ontploffing (1,2x wapenschade, straal 3). Plus Wijsheid en Elementschade."),
}
for _k, (_t, _o, _u) in ADV3.items():
    TEKSTEN[f"vadsrijk.klassen.titel.{_k}.3"] = _t
    TEKSTEN[f"vadsrijk.klassen.adv.onder.{_k}.3"] = _o
    TEKSTEN[f"vadsrijk.klassen.adv.uitleg.{_k}.3"] = _u

KNOPEN = {
    # Knuffelridder
    "r_ijsklap": ("IJsklap", "Kussenklap laat vijanden 2,5 s Bevroren raken: ze lopen trager en zware klappen doen 30 % meer."),
    "r_ijspantser": ("IJspantser", "Veel meer Vads, Verdediging en Elementweerstand. Een knuffel als een gletsjer."),
    "r_onverzettelijk": ("Onverzettelijk", "Brandend, Gesporen, Plakkerig, Verdoofd en Bevroren duren op jou maar half zo lang."),
    "r_galmstoot": ("Galmstoot", "Stormsprong laadt 2 s sneller op, en de landing doet dubbele schade op bevroren vijanden."),
    "r_kristalhart": ("Kristalhart", "Meer Kracht, hardere kritieke klappen en extra Vads."),
    "r_ijzeren_knuffel": ("IJzeren Knuffel", "Zak je onder 30 % Vads, dan krijg je 5 s een Knuffelschild van 30 % van je max Vads (eens per minuut)."),
    # Pluisjager
    "j_ijspijl": ("IJspijl", "Je gewone schoten hebben 15 % kans om een vijand 2 s te laten bevriezen."),
    "j_poolvacht": ("Poolvacht", "Meer Behendigheid, Vads en Loopsnelheid. Warm en snel."),
    "j_jachtinstinct": ("Jachtinstinct", "20 % meer schade op vijanden onder 35 % Vads. Afmaken!"),
    "j_splinterregen": ("Splinterregen", "Pluizenregen laadt 1,5 s sneller op en doet 60 % meer schade op bevroren vijanden."),
    "j_sterrenoog": ("Sterrenoog", "Veel meer kans op kritiek en hardere kritieke klappen."),
    "j_windloper": ("Windloper", "Elke vijand die je helpt verslaan, maakt Rolsalto meteen weer klaar."),
    # Vadsmagier
    "m_ijsbollen": ("IJsbollen", "Je gewone toverbollen hebben 20 % kans om een vijand 2 s te laten bevriezen."),
    "m_kristalgeest": ("Kristalgeest", "Meer Wijsheid, Vads en Elementweerstand."),
    "m_heldere_bron": ("Heldere Bron", "Als jij iemand healt, wast dat ook Brandend, Gesporen, Plakkerig en Bevroren weg."),
    "m_ijsnova": ("IJsnova", "Meteoorguh bevriest alles wat hij raakt 4 s en geeft je 30 energie terug."),
    "m_sterrenwijsheid": ("Sterrenwijsheid", "15 % meer Elementschade en 8 % Cooldownreductie."),
    "m_manastroom": ("Manastroom", "Elke vijand die je helpt verslaan, geeft je 8 Vadsenergie."),
}
for _id, (_n, _u) in KNOPEN.items():
    TEKSTEN[f"vadsrijk.klassen.knoop.{_id}"] = _n
    TEKSTEN[f"vadsrijk.klassen.knoop.{_id}.uitleg"] = _u
for _k in ("r", "j", "m"):
    TEKSTEN[f"vadsrijk.klassen.knoop.{_k}_adv3"] = "Derde job-advancement"
    TEKSTEN[f"vadsrijk.klassen.knoop.{_k}_adv3.uitleg"] = "Behaald in de ceremonie op niveau 30."

for _id, (_n, _k, _f) in KRISTALLEN.items():
    TEKSTEN[f"item.guhs.vr_z2_kristal_{_id}"] = _n
    TEKSTEN[f"item.guhs.vr_z2_kristal_{_id}.flavour"] = _f
for _id, (_n, _icoon, _fl) in WAPENS.items():
    TEKSTEN[f"item.guhs.vr_z2_{_id}"] = _n
    TEKSTEN[f"vadsrijk.loot.{_id}"] = _n
    for _i, _l in enumerate(_fl):
        TEKSTEN[f"vadsrijk.loot.{_id}.flavour.{_i}"] = _l
for _id, (_n, _icoon, _u, _op) in DRANKEN.items():
    TEKSTEN[f"item.guhs.{_id}"] = _n
    TEKSTEN[f"item.guhs.{_id}.uitleg"] = _u
    TEKSTEN[f"item.guhs.{_id}.op"] = _op


# ======================================================================================================================
def _item(naam, doek, handheld=False):
    doek.png(os.path.join(A, "textures", "item", ITEM_TEX, f"{naam}.png"))
    parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
    _schrijf(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": parent, "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}})
    _schrijf(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def _assets():
    for i, (sid, (_n, kleur, _f)) in enumerate(KRISTALLEN.items()):
        _item(f"vr_z2_kristal_{sid}", kristal(kleur, i))
    for wid, (_n, icoon, _fl) in WAPENS.items():
        _item(f"vr_z2_{wid}", icoon(), handheld=True)
    for did, (_n, icoon, _u, _op) in DRANKEN.items():
        _item(did, icoon())


def build(h):
    _assets()
    for k, nl in TEKSTEN.items():
        h.lang(k, nl, nl)


def _direct():
    _assets()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(TEKSTEN)
        _schrijf(p, data)
    print(f"vadsrijk_z2_systemen: {len(TEKSTEN)} texts, {len(KRISTALLEN) + len(WAPENS) + len(DRANKEN)} item icons")


if __name__ == "__main__":
    _direct()
