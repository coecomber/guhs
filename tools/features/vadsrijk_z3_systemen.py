"""
1.10.0 "Het Vadsrijk" zone 3 "De Schemerwortel", slice "systemen" (Z3_BRIEF.md). Java: feature/vadsrijk/zone3/systemen.

  - pixel art (16x16, the economie painter `Doek`) for the 6 zone-3 weapons, 4 crystals, the Gouden Veer, the Vahoegzegel,
    the Premiekist and the 3 tier-3 potions; item models + item definitions;
  - every text (Dutch; en_us gets the same Dutch): tree rows 6/7, the 4th job advancement, bounties, contracts, the board,
    the penningwinkel, Gouden Veren, Vahoeg, the Honingkraam.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_systemen.py
"""
import json
import os

from vadsrijk_economie import Doek, _licht, zwaard, boog, staf, drankje
from vadsrijk_z2_systemen import kristal, hamer, kruisboog, _kristaltop

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
ITEM_TEX = "vadsrijk/zone3/systemen"


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ======================================================================================================================
# icons
# ======================================================================================================================
def knots(hout, blad):
    d = Doek()
    d.lijn(3, 14, 9, 8, _licht(hout, 0.8), dik=2)
    d.disc(10.5, 5.5, 4, hout)
    d.disc(9.5, 4.5, 2, _licht(hout, 1.25))
    for x, y in ((13, 2), (14, 6), (7, 3)):
        d.px(x, y, blad)
        d.px(x + 1, y, _licht(blad, 1.2))
    d.schaduw()
    return d.rand()


def lantaarntop(d):
    d.rect(9, 1, 14, 2, (90, 60, 40))
    d.rect(9, 2, 14, 8, (255, 200, 90))
    d.rect(10, 3, 13, 7, (255, 240, 170))
    d.rect(9, 8, 14, 9, (90, 60, 40))
    d.px(11, 5, (255, 255, 255))


def bloesemtop(d):
    for x, y in ((11, 2), (9, 4), (13, 4), (10, 6), (12, 6)):
        d.disc(x, y, 1.6, (255, 150, 200))
    d.disc(11, 4.5, 1.3, (255, 230, 120))


def veer(kleur=(255, 205, 60)):
    d = Doek()
    d.lijn(3, 14, 12, 3, _licht(kleur, 0.7))
    for i in range(8):
        x, y = 4 + i, 13 - i
        d.lijn(x, y, x - 2, y - 3, kleur)
        d.lijn(x, y, x + 3, y + 1, _licht(kleur, 1.15))
    d.px(12, 3, (255, 255, 230))
    d.px(9, 5, (255, 255, 255))
    d.schaduw()
    return d.rand()


def zegel():
    d = Doek()
    d.disc(8, 8, 6, (200, 60, 120))
    d.disc(8, 8, 4.6, (255, 110, 170))
    d.poly([(8, 4), (9.5, 7), (12.5, 7.5), (10, 9.5), (11, 12.5), (8, 10.8), (5, 12.5), (6, 9.5), (3.5, 7.5), (6.5, 7)], (255, 220, 90))
    d.px(7, 6, (255, 255, 255))
    d.schaduw()
    return d.rand()


def kist():
    d = Doek()
    d.rect(2, 6, 14, 14, (170, 110, 60))
    d.rect(2, 4, 14, 7, (200, 140, 80))
    d.rect(2, 7, 14, 8, (240, 200, 70))
    d.rect(7, 6, 9, 10, (255, 220, 90))
    d.rect(3, 9, 13, 13, (150, 95, 50))
    d.px(8, 8, (255, 255, 255))
    for x, y in ((4, 2), (12, 1), (8, 1)):
        d.px(x, y, (255, 240, 150))
    d.schaduw()
    return d.rand()


def thee():
    d = Doek()
    d.rect(3, 7, 11, 14, (250, 235, 245))
    d.rect(4, 8, 10, 13, (255, 245, 250))
    d.rect(4, 7, 10, 8, (255, 160, 200))
    d.lijn(12, 9, 14, 10, (250, 235, 245))
    d.lijn(14, 10, 12, 12, (250, 235, 245))
    d.disc(7, 10.5, 1.5, (255, 130, 190))
    for x, y in ((5, 4), (7, 3), (9, 4)):
        d.px(x, y, (240, 230, 240))
    return d.rand()


KRISTALLEN = {
    "amberroze": ("Amberroze", 0xFF9A6A, "Roze hars uit de stam. Kleverig en warm. Elke klap geeft je een beetje Vads terug."),
    "honingkristal": ("Honingkristal", 0xFFC830, "Ruikt naar de raattrossen van Honingtak. Je heals worden zoeter (sterker)."),
    "wortelkristal": ("Wortelkristal", 0x6AC8A0, "Gegroeid tussen de oude wortels. Stevig als de boom zelf: meer Verdediging."),
    "schemerkristal": ("Schemerkristal", 0x9A7AE8, "Vangt het licht van de schemer. Je vaardigheden zijn sneller weer klaar."),
}

WAPENS = {
    "takkenknots": ("Takkenknots", lambda: knots((140, 95, 60), (120, 200, 90)), [
        "Een tak van de wereldboom. Hij groeit nog een beetje als niemand kijkt.",
        "Bonk. Er vallen blaadjes uit.",
        "De boom heeft hem zelf laten vallen. Echt waar.",
    ]),
    "schorshamer": ("Schorshamer", lambda: hamer((170, 110, 70)), [
        "Zo zwaar als een stuk schors van tien jaar oud.",
        "Langzaam. Heel langzaam. Maar dan: BOEM.",
        "Het boomvolk timmerde er hun huisjes mee.",
    ]),
    "liaanboog": ("Liaanboog", lambda: boog((110, 160, 80), (200, 255, 160)), [
        "De pees is een liaan die graag wil helpen.",
        "Schiet zo snel als een eekhoorntje rent.",
        "Ruikt naar regen in het bladerdak.",
    ]),
    "raatkruisboog": ("Raatkruisboog", lambda: kruisboog((150, 100, 50), (255, 200, 60)), [
        "Gebouwd door een imkerguh met een plan.",
        "De pijlen plakken een beetje. Dat hoort zo.",
        "Bzzz... klik. Raak!",
    ]),
    "bloesemstaf": ("Bloesemstaf", lambda: staf(bloesemtop), [
        "Er bloeit altijd wel iets aan. Ook in de winter.",
        "Je heals ruiken naar lente.",
        "Een vlinder woont in de bovenste bloem. Hij betaalt geen huur.",
    ]),
    "lantaarnstaf": ("Lantaarnstaf", lambda: staf(lantaarntop), [
        "Een oude lantaarn van het boomvolk, nog altijd aan.",
        "Licht in het donker, vuur voor wie dichtbij komt.",
        "Oude Knoest zegt dat hij er vroeger mee kon lezen.",
    ]),
}

SPUL = {
    # id: (naam, icoon, uitleg)
    "vr_z3_gouden_veer": ("Gouden Veer", veer, "Rechtsklik: op je Gouden Veren-teller (sluipen: de hele stapel). 25 veren = de Gouden Pluimguh bij stalmeester Veertje."),
    "vr_z3_vahoegzegel": ("Vahoegzegel", zegel, "Leg hem op het Vahoeg-altaar bij een kerkerdeur: de bazen worden Vahoeg (lv 50, sterker, betere buit en Gouden Veren)."),
    "vr_z3_premiekist": ("Premiekist", kist, "Je weekdoel van het Premiebord! Rechtsklik: een Episch of Legendarisch wapen, een kristal en een Gouden Veer."),
}

DRANKEN = {
    "vr_z3_reuzen_vadsdrank": ("Reuzen Vadsdrankje", lambda: drankje((255, 40, 130)), "Geeft je 1500 Vads terug.", "GLUG GLUG GLUG... VAHOEG!"),
    "vr_z3_reuzen_energiedrank": ("Reuzen Energiedrankje", lambda: drankje((60, 200, 255)), "Geeft je 200 energie terug.", "Bzzzzzzt! Je oortjes knetteren!"),
    "vr_z3_bloesemthee": ("Bloesemthee", thee, "Geeft je 400 Vads en 60 energie, en wast Plakkerig en Gesporen weg.",
                          "Een slokje lente. Je voelt je weer helemaal schoon."),
}

HANDHELD = set(WAPENS)

# ======================================================================================================================
# texts
# ======================================================================================================================
S = "vadsrijk.z3.systemen."
TEKSTEN = {
    # 4th advancement
    S + "adv4.klaar": "✦ Niveau 50! Je bent klaar voor je vierde job-advancement tot %s! Ga naar de Nestkapel in Honingtak.",
    S + "tip.adv4.titel": "Vierde job-advancement!",
    S + "tip.adv4": "Op niveau 50 wacht de Nestkapel: een nieuwe titel en de Nestvorm van je ult (dezelfde toets, veel groter).",
    # Gouden Veren
    S + "veer.erbij": "✦ +%s Gouden Veer! Je hebt er nu %s van de %s.",
    S + "veer.erbij_al": "✦ +%s Gouden Veer! (%s; je hebt de Gouden Pluimguh al, ze glimmen gewoon mooi.)",
    S + "veer.te_weinig": "Je hebt %s van de %s Gouden Veren nodig.",
    S + "veer.al_goud": "Je hebt de Gouden Pluimguh al! (%s veren, nodig %s)",
    S + "veer.melding": "✦ %s heeft de GOUDEN PLUIMGUH! ✦",
    # Vahoeg
    S + "vahoeg.te_laag": "De Vahoeg-modus is pas vanaf niveau %s.",
    S + "vahoeg.geen_zegel": "Je hebt geen Vahoegzegel. De Jagersguh in Honingtak verkoopt ze voor Jachtpenningen.",
    # board
    S + "bord.titel": "Het Premiebord",
    S + "bord.tab.premies": "Premies",
    S + "bord.tab.jacht": "Jacht",
    S + "bord.tab.winkel": "Penningwinkel",
    S + "bord.penningen": "%s Jachtpenningen",
    S + "bord.veren": "Gouden Veren: %s/%s",
    S + "bord.reset": "Nieuwe premies over %su %smin",
    S + "bord.claim": "Ophalen",
    S + "bord.ruil": "Ruilen",
    S + "bord.geclaimd": "✔ Opgehaald",
    S + "bord.week": "Weekdoel: %s/%s premies (dan een Premiekist)",
    S + "bord.week_klaar": "Weekdoel gehaald (%s/%s): je Premiekist is binnen!",
    S + "bord.jacht_uitleg": "Neem een contract en het doelwit wacht bij zijn hol. Alleen voor jou en je party.",
    S + "bord.neem": "Contract",
    S + "bord.bezig": "Bezig...",
    S + "bord.prijs": "%s ✦",
    # bounties
    S + "premie.klaar": "✦ Premie gehaald: %s! Haal je beloning op bij het Premiebord.",
    S + "premie.te_laag": "Het Premiebord is pas vanaf niveau 40.",
    S + "premie.te_laag_lv": "Kom terug op niveau %s: dan heeft de Jagersguh premies voor je.",
    S + "premie.weg": "Die premie bestaat niet (meer).",
    S + "premie.al": "Die heb je al opgehaald.",
    S + "premie.nog_niet": "Die premie is nog niet klaar.",
    S + "premie.geclaimd": "Premie opgehaald! Xp, munten en Jachtpenningen.",
    S + "premie.al_geruild": "Je hebt vandaag al geruild. Morgen weer!",
    S + "premie.niet_ruilen": "Die kun je niet ruilen.",
    S + "premie.geruild": "Geruild! Een nieuwe premie op het bord.",
    S + "premie.weekkist": "✦ Weekdoel gehaald! Er zit een Premiekist in je tas.",
    # contracts
    S + "contract.te_laag": "Jachtcontracten zijn pas vanaf niveau 45.",
    S + "contract.te_laag_lv": "De Jagersguh geeft je pas contracten op niveau %s.",
    S + "contract.niet_vandaag": "Dat doelwit staat vandaag niet op het bord.",
    S + "contract.verstopt": "Het doelwit laat zich vandaag niet zien. Probeer het later nog eens.",
    S + "contract.genomen": "Contract genomen! Het doelwit wacht bij zijn hol (kijk op je kaart).",
    S + "contract.klaar": "✦ Contract gehaald: %s! +%s Jachtpenningen.",
    S + "contract.weg": "%s is weggelopen. Je kunt het contract opnieuw nemen.",
    # shop
    S + "winkel.te_weinig": "Daar heb je niet genoeg Jachtpenningen voor.",
    S + "winkel.gekocht": "Gekocht! Het zit in je tas.",
    S + "winkel.vahoegzegel": "Vahoegzegel",
    S + "winkel.kristal_amberroze": "Amberroze (ruw)",
    S + "winkel.kristal_honingkristal": "Honingkristal (ruw)",
    S + "winkel.kristal_wortelkristal": "Wortelkristal (ruw)",
    S + "winkel.kristal_schemerkristal": "Schemerkristal (ruw)",
    S + "winkel.kristal_geslepen": "Geslepen verrassing",
    S + "winkel.herrolknabbel": "Herrolknabbel",
    "vadsrijk.economie.winkel.z3_honingkraam": "De Honingkraam",
}

PREMIES = {
    "jacht_kruinzee": "Versla %s wezens in De Kruinzee",
    "jacht_takken": "Versla %s wezens op De Takkenbanen",
    "jacht_schors": "Versla %s wezens in De Schorsstad",
    "jacht_velden": "Versla %s wezens in De Wortelvelden",
    "jacht_duister": "Versla %s wezens in Het Wortelduister",
    "jacht_overal": "Versla %s wezens in De Schemerwortel",
    "elite_kruin": "Versla %s elites in de kruin of op de takken",
    "elite_schors": "Versla %s elites in De Schorsstad",
    "elite_velden": "Versla %s elites in De Wortelvelden",
    "elite_duister": "Versla %s elites in Het Wortelduister",
    "elite_drie": "Versla %s elites in De Schemerwortel",
    "baas_werf": "Versla een baas van de Kaaszeppelinwerf",
    "baas_kruin": "Versla een baas in De Kruinzee",
    "baas_schors": "Versla een baas in De Schorsstad",
    "baas_hart": "Versla een baas in het Hart van de Boom",
    "rit_kort": "Rij %s blokken op je rijdier door De Schemerwortel",
    "rit_lang": "Rij %s blokken op je rijdier door De Schemerwortel",
    "wind": "Laat je %s keer omhoog blazen door een windkolom",
    "ketels": "Zet %s schimmelketels uit",
    "sporen": "Verzamel %s schimmelsporen",
    "honingpot": "Bezorg %s honingpot op tijd",
    "muisjes": "Bevrijd %s pieppiepmuisjes",
    "lantaarns": "Steek %s lantaarns aan in het donker",
}
for _id, _t in PREMIES.items():
    TEKSTEN[S + "premie." + _id] = _t

CONTRACTEN = {
    "ouwe_rotkeiler": ("Ouwe Rotkeiler", "Een oud rotzwijn dat twee keer achter elkaar stormt."),
    "moeder_wesp": ("Moeder Wesp", "Splitst haar zwerm als ze half op is."),
    "de_harsreus": ("De Harsreus", "Een harsgolem met drie ringen schokgolven."),
    "kapitein_kaaskop": ("Kapitein Kaaskop", "Schiet drie kaasballen in een waaier."),
    "grootvader_rups": ("Grootvader Rups", "Rolt drie keer achter elkaar over het veld."),
    "de_stille_ziener": ("De Stille Ziener", "Hoort je sluipen. Laat drie schaduwen los."),
    "slangenkoningin_sapsie": ("Slangenkoningin Sapsie", "Waar ze bovenkomt, blijft een sapplas liggen."),
    "bramzwijn_de_brede": ("Bramzwijn de Brede", "Een stormloop van drie breed, met braamstruiken."),
}
for _id, (_n, _u) in CONTRACTEN.items():
    TEKSTEN[S + "contract." + _id] = _n
    TEKSTEN[S + "contract." + _id + ".uitleg"] = _u

ADV4 = {
    "knuffelridder": ("Nestwachter", "Nieuw: de Nestvorm van Vahoege Plof!",
                      "Nestvorm: Vahoege Plof wordt een kussen met vleugels. 30 % meer schade, en de wind trekt alle vijanden binnen 8 "
                      "blokken naar het midden (bazen niet). Plus Kracht, Verdediging en Vads."),
    "pluisjager": ("Kruinvalk", "Nieuw: de Nestvorm van Sterrenschot!",
                   "Nestvorm: Sterrenschot doet 40 % meer schade en doorboort schilden: een kwart van de schade komt er nog eens "
                   "dwars doorheen. Plus Behendigheid, Kritiekschade en Vads."),
    "vadsmagier": ("Schemerwijze", "Nieuw: de Nestvorm van Meteoorguh!",
                   "Nestvorm: Meteoorguh laat 5 s een roze vuurveld achter (straal 3,5): elke seconde 30 % wapenschade en Brandend. "
                   "Plus Wijsheid, Elementschade en Heal-kracht."),
}
for _k, (_t, _o, _u) in ADV4.items():
    TEKSTEN[f"vadsrijk.klassen.titel.{_k}.4"] = _t
    TEKSTEN[f"vadsrijk.klassen.adv.onder.{_k}.4"] = _o
    TEKSTEN[f"vadsrijk.klassen.adv.uitleg.{_k}.4"] = _u
for _k in ("r", "j", "m"):
    TEKSTEN[f"vadsrijk.klassen.knoop.{_k}_adv4"] = "Vierde job-advancement"
    TEKSTEN[f"vadsrijk.klassen.knoop.{_k}_adv4.uitleg"] = "Behaald in de ceremonie op niveau 50."

KNOPEN = {
    "r_schorshuid": ("Schorshuid", "Onder 40 % Vads krijg je 15 % minder schade. Taai als boomschors."),
    "r_afstijgstorm": ("Afstijgstorm", "Stormsprong doet 35 % meer schade en is 1 s sneller weer klaar."),
    "r_lantaarnschild": ("Lantaarnschild", "Zolang je Knuffelschild op staat, krijgt wie jou raakt een kwart van die klap terug."),
    "r_drakenkussen": ("Drakenkussen", "Vahoege Plof geeft jou en je party binnen 10 blokken 4 s een Knuffelschild van 20 % van je max Vads."),
    "j_zweefschot": ("Zweefschot", "Een schot vanuit de lucht (springen, zweven of op je rijdier) doet 50 % meer schade. Eens per 8 s."),
    "j_lichtpijl": ("Lichtpijl", "Knuffelpijl markeert zijn doel 6 s: alle schade op dat doel is 15 % hoger, ook die van je vrienden."),
    "j_sporenzoeker": ("Sporenzoeker", "20 % meer schade op wezens met een status (Brandend, Gesporen, Plakkerig, Verdoofd of Bevroren)."),
    "j_sterrenzwerm": ("Sterrenzwerm", "Sterrenschot laat 3 nalasers na: elk 40 % wapenschade op je doel."),
    "m_lantaarnvlam": ("Lantaarnvlam", "Roze Vuurbal zet zijn doel 3 s in brand."),
    "m_wortelband": ("Wortelband", "Vadsbron maakt vijanden binnen 5 blokken 2 s Plakkerig."),
    "m_blinkspoor": ("Blinkspoor", "Na Blink heel je jou en je party binnen 6 blokken 8 % van hun max Vads."),
    "m_meteoorzwerm": ("Meteoorzwerm", "Meteoorguh splitst: 3 kleinere inslagen rond je doel (elk 60 % wapenschade, straal 2,5)."),
}
for _id, (_n, _u) in KNOPEN.items():
    TEKSTEN[f"vadsrijk.klassen.knoop.{_id}"] = _n
    TEKSTEN[f"vadsrijk.klassen.knoop.{_id}.uitleg"] = _u

for _id, (_n, _k, _f) in KRISTALLEN.items():
    TEKSTEN[f"item.guhs.vr_z3_kristal_{_id}"] = _n
    TEKSTEN[f"item.guhs.vr_z3_kristal_{_id}.flavour"] = _f
for _id, (_n, _icoon, _fl) in WAPENS.items():
    TEKSTEN[f"item.guhs.vr_z3_{_id}"] = _n
    TEKSTEN[f"vadsrijk.loot.{_id}"] = _n
    for _i, _l in enumerate(_fl):
        TEKSTEN[f"vadsrijk.loot.{_id}.flavour.{_i}"] = _l
for _id, (_n, _icoon, _u) in SPUL.items():
    TEKSTEN[f"item.guhs.{_id}"] = _n
    TEKSTEN[f"item.guhs.{_id}.uitleg"] = _u
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
        _item(f"vr_z3_kristal_{sid}", kristal(kleur, i))
    for wid, (_n, icoon, _fl) in WAPENS.items():
        _item(f"vr_z3_{wid}", icoon(), handheld=True)
    for sid, (_n, icoon, _u) in SPUL.items():
        _item(sid, icoon())
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
    print(f"vadsrijk_z3_systemen: {len(TEKSTEN)} texts, {len(KRISTALLEN) + len(WAPENS) + len(SPUL) + len(DRANKEN)} item icons")


if __name__ == "__main__":
    _direct()
