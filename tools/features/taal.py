"""
1.2.0 (Guhs in two languages): the Dutch of the texts the Java side needed for the NL/EN switch and for text that used to be
resolved (or hard-coded in Dutch) on the server. No registry content: this module only writes lang (Dutch; the English is in
tools/lang/en/*.json like every other key).

  gui.guhs.menu.taal / gui.guhs.taal.*          the language button in the guh menu (client/GuhsTaal: Auto / NL / EN)
  entity.guhs.reisguh.plek.<id>                 the place names the templates give their Reisguh (quest/Reisguh.PLEKKEN)
  quest.guhs.reis.naam_xz                       "Guhkermis (12, -40)": a template name that was already taken
  gui.guhs.huisje.standaardnaam.<i>             the default huisje names (feature/huisje/Huisjes.NAMEN, same order)
  entity.guhs.beauty_model.naam.<i>             the Showguh's models (feature/beauty/BeautyShow.MODEL_NAMES)
  entity.guhs.race_guh.naam.<i>                 the race guhs (feature/race/RaceGame.GUH_NAMES)
  gui.guhs.kleding.prijs.munt.<coin> / .hint.*  where a clothing piece comes from (feature/kleding/KledingBronLijst + the minigames)
  gui.guhs.wistjedat.een_guh / .mijn_baasje     fallbacks in the wist-je-datjes (feature/favorietjes/Verhaaltjes)
  gui.guhs.vissen.kg                            a fish weight with the decimal sign of the language
  commands.guhs.*                               the op commands' answers

The self-check compares the name lists and the keys the Java code uses with this file, so no raw key shows in the game.
"""
import os
import re

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs")

TAAL = {
    "gui.guhs.menu.taal": "Taal: %s",
    "gui.guhs.menu.taal.tooltip": "In welke taal je alle Guhs-teksten leest. Auto volgt de taal van Minecraft (Nederlands bij "
                                  "Nederlands, anders Engels); of kies zelf Nederlands of Engels. Alleen voor jou, ook op servers. "
                                  "(Een bord dat al in beeld is wisselt mee; een scorebord pas als het weer ververst.)",
    "gui.guhs.taal.auto": "Auto",
    "gui.guhs.taal.nl": "Nederlands",
    "gui.guhs.taal.en": "English",
    "gui.guhs.taal.auto_is": "Auto (%s)",
    "gui.guhs.taal.kort.nl": "NL",
    "gui.guhs.taal.kort.en": "EN",
}

# quest/Reisguh.PLEKKEN: the Dutch name in the template -> id
PLEKKEN = {"Knuffeldal": "knuffeldal", "Guhkermis": "guhkermis", "Guhland": "guhland", "Nomguh": "nomguh", "Guhwarden": "guhwarden",
           "Ballonfestival": "ballonfestival", "Guhcircuit": "guhcircuit", "Ohana op Guhwai'i": "ohana",
           "De capsule van 626": "capsule", "Guhkasteel": "guhkasteel", "Kloon-eiland": "kloon_eiland"}

# feature/huisje/Huisjes.NAMEN (same order: the index is the key)
HUISJE_NAMEN = ["Knabbelkasteeltje", "Villa Vads", "Huize Njeg", "Het Pluisnestje", "Snoetjeshuis",
                "Oortjeshof", "Vadsig Paleisje", "Knuffelhoekje", "Kaasknabbelkot", "Guhlief", "Roze Wolkje", "Het Warme Snoetje",
                "Pootjeshuis", "Zoete Knabbelstee", "Villa Vahoeg", "Het Dikke Kussentje", "Huize Pluisoor", "Knabbelkoepeltje",
                "Snurkhuisje", "Het Lieve Vadsje", "Slaapsnoetje", "Njegnestje", "Knusse Kaaskamer", "Guhtje Thuis"]
# feature/beauty/BeautyShow.MODEL_NAMES and feature/race/RaceGame.GUH_NAMES
MODEL_NAMEN = ["Vadsy", "Guhlia", "Knabbeline", "Bolleke", "Pluisje", "Vadsiena", "Guhnther", "Mollie", "Kaasje", "Poekie"]
RACE_NAMEN = ["Bliksemvads", "Turbo Njeg", "Vahoeg 3000", "Roze Donder", "Kaasknabbel Express", "Vadsraket",
              "Snelle Gerrit", "Pluizige Pijl", "Knabbelknaller", "Wervelguh"]

OVERIG = {
    "quest.guhs.reis.naam_xz": "%s (%s, %s)",
    "gui.guhs.wistjedat.een_guh": "een guh",
    "gui.guhs.wistjedat.mijn_baasje": "mijn baasje",
    "gui.guhs.vissen.kg": "%s,%s kg",
    "chat.guhs.secret_note.whisper": "shhh. niet doorvertellen. Lees dit...",
    "gui.guhs.elftocht.vahoeg_titel": "VAHOEG!",
    "item.guhs.bieb_boek.door": "door %s",
}

# where a clothing piece comes from (KledingBronnen.prijs: the amount is the argument)
KLEDING = {
    "gui.guhs.kleding.prijs.munt.smaragden": "%s smaragden",
    "gui.guhs.kleding.prijs.munt.bakmunten_bij_bakker_korstje": "%s bakmunten bij Bakker Korstje",
    "gui.guhs.kleding.prijs.hint.je_eerste_klusje_voor_boerin_hooibaal": "je eerste klusje voor Boerin Hooibaal",
    "gui.guhs.kleding.prijs.munt.klusjes_voor_boerin_hooibaal": "%s klusjes voor Boerin Hooibaal",
    "gui.guhs.kleding.prijs.hint.help_brandweercommandant_blusguh": "help Brandweercommandant Blusguh",
    "gui.guhs.kleding.prijs.hint.los_de_knabbeldief_zaak_op": "los de Knabbeldief-zaak op",
    "gui.guhs.kleding.prijs.hint.help_dokter_snotneus_guh": "help Dokter Snotneus-guh",
    "gui.guhs.kleding.prijs.hint.help_bob_de_guhbouwer": "help Bob de Guhbouwer",
    "gui.guhs.kleding.prijs.hint.in_de_picknickmand": "in de picknickmand",
    "gui.guhs.kleding.prijs.hint.5_guhs_gezien_in_de_guhdex": "5 guhs gezien in de Guhdex",
    "gui.guhs.kleding.prijs.hint.8_guhs_gezien_en_3_getemd": "8 guhs gezien en 3 getemd",
    "gui.guhs.kleding.prijs.hint.de_hele_guhdex_vol": "de hele Guhdex vol",
    "gui.guhs.kleding.prijs.hint.tem_de_geheime_brococolief_guh": "tem de geheime Brococolief-guh",
    "gui.guhs.kleding.prijs.hint.zelf_maken_touw_leer_kist_en_roze_wol": "zelf maken: touw, leer, kist en roze wol",
    "gui.guhs.kleding.prijs.hint.de_schatkamer_van_het_guhkasteel": "de schatkamer van het guhkasteel",
    "gui.guhs.kleding.prijs.hint.de_wolkenkist_op_de_zwevende_eilandjes": "de wolkenkist op de zwevende eilandjes",
    "gui.guhs.kleding.prijs.hint.schatkisten_in_grotten_en_kasteel": "schatkisten in grotten en kasteel",
    "gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_grillguh": "%s kaasknabbels bij de Grillguh",
    "gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_boswachterguh": "%s kaasknabbels bij de Boswachterguh",
    "gui.guhs.kleding.prijs.munt.knabbelbessen_bij_de_knabbelplukker": "%s knabbelbessen bij de Knabbelplukker",
    "gui.guhs.kleding.prijs.hint.schatkisten_in_hamsterhuizen": "schatkisten in hamsterhuizen",
    "gui.guhs.kleding.prijs.hint.kisten_in_mika_huizen": "kisten in Mika-huizen",
    "gui.guhs.kleding.prijs.hint.schatkisten_in_de_guhgrotten": "schatkisten in de guhgrotten",
    "gui.guhs.kleding.prijs.hint.de_schatten_van_de_guhramide": "de schatten van de guhramide",
    "gui.guhs.kleding.prijs.munt.kermisbonnen": "%s kermisbonnen",
    "gui.guhs.kleding.prijs.munt.verstopguhtickets": "%s verstopguhtickets",
    "gui.guhs.kleding.prijs.munt.showrozetten": "%s showrozetten",
    "gui.guhs.kleding.prijs.munt.raceprijsjes": "%s raceprijsjes",
    "gui.guhs.kleding.prijs.munt.mepmunten": "%s mepmunten",
    "gui.guhs.kleding.prijs.munt.discomunten": "%s discomunten",
    "gui.guhs.kleding.prijs.munt.golfballetjes": "%s golfballetjes",
    "gui.guhs.kleding.prijs.munt.smulmunten": "%s smulmunten",
    "gui.guhs.kleding.prijs.munt.visbonnen": "%s visbonnen",
    "gui.guhs.kleding.prijs.munt.kaasbrokken": "%s kaasbrokken",
    "gui.guhs.kleding.prijs.munt.boekenbonnen": "%s boekenbonnen",
    "gui.guhs.kleding.prijs.munt.parels": "%s parels",
    "gui.guhs.kleding.prijs.hint.loop_de_vadsparade_helemaal_mee": "loop de Vadsparade helemaal mee",
    "gui.guhs.kleding.prijs.hint.de_finale_van_het_grote_knusfeest": "de finale van het Grote Knusfeest",
    "gui.guhs.kleding.prijs.hint.de_lente_in_knuffeldal": "de lente in Knuffeldal",
    "gui.guhs.kleding.prijs.hint.de_zomer_in_knuffeldal": "de zomer in Knuffeldal",
    "gui.guhs.kleding.prijs.hint.de_winter_in_knuffeldal": "de winter in Knuffeldal",
    "gui.guhs.kleding.prijs.munt.speenmunten": "%s speenmunten",
    "gui.guhs.kleding.prijs.hint.je_eerste_gezellige_theekransje": "je eerste gezellige theekransje",
    "gui.guhs.kleding.prijs.munt.krulmunten": "%s krulmunten",
    "gui.guhs.kleding.prijs.munt.kaasknabbels_bij_boerin_hooibaal": "%s kaasknabbels bij Boerin Hooibaal",
    "gui.guhs.kleding.prijs.munt.wenssterren": "%s wenssterren",
    "gui.guhs.kleding.prijs.munt.ballonmunten": "%s ballonmunten",
    "gui.guhs.kleding.prijs.munt.kaasknabbels_bij_opa_guh": "%s kaasknabbels bij Opa Guh",
    "gui.guhs.kleding.prijs.munt.eendjesmunten": "%s eendjesmunten",
    "gui.guhs.kleding.prijs.hint.zing_alle_liedjes_van_het_koortje": "zing alle liedjes van het koortje",
    "gui.guhs.kleding.prijs.munt.kaasknabbels_bij_ijscoguh_tingeling": "%s kaasknabbels bij IJscoguh Tingeling",
    "gui.guhs.kleding.prijs.munt.circuitbekers": "%s circuitbekers",
    "gui.guhs.kleding.prijs.munt.doolhofknabbels": "%s doolhofknabbels",
    "gui.guhs.kleding.prijs.munt.elfstempels": "%s elfstempels",
    "gui.guhs.kleding.prijs.munt.katapultsterren": "%s katapultsterren",
    "gui.guhs.kleding.prijs.munt.spelenlintjes": "%s spelenlintjes",
    "gui.guhs.kleding.prijs.munt.sjoelschijfjes": "%s sjoelschijfjes",
}

# the op commands' answers (/guhs ballonvlucht, baltoslee, guhwaiispellen, hemel, samen)
COMMANDS = {
    "commands.guhs.ballonvlucht.geen_ballon": "Njeg: geen guh-luchtballon in de buurt (binnen %s blokken).",
    "commands.guhs.ballonvlucht.kan_niet": "Njeg: deze ballon kan nu niet opstijgen.",
    "commands.guhs.ballonvlucht.stijgt_op": "VAHOEG! De ballon stijgt op: %s",
    "commands.guhs.ballonvlucht.geland": "De ballon is weer geland.",
    "commands.guhs.ballonvlucht.niet_in_ballon": "Njeg: je zit niet in een vliegende ballon.",
    "commands.guhs.baltoslee.vrij": "De sledesprint is vrij voor %s speler(s). Njeg!",
    "commands.guhs.baltoslee.geen_nomguh": "Njeg: geen Nomguh in de buurt. Geef een anker op, of probeer /guhs baltoslee proef.",
    "commands.guhs.baltoslee.bezig": "Njeg: je bent al met iets anders bezig.",
    "commands.guhs.baltoslee.steele": "Steele-Mika staat klaar voor de sledesprint. Njeh-heh!",
    "commands.guhs.guhwaiispellen.geen_surf": "Njeg: geen surf-Lilo-guh in de buurt.",
    "commands.guhs.guhwaiispellen.geen_hula": "Njeg: geen hula-Lilo-guh in de buurt.",
    "commands.guhs.guhwaiispellen.geen_surf_binnen": "Njeg: geen surf-Lilo-guh binnen %s blokken.",
    "commands.guhs.guhwaiispellen.niet_bezig": "Je surft en danst niet.",
    "commands.guhs.hemel.klopt": "Knuffelhart klopt voor %s",
    "commands.guhs.hemel.slaapt": "Knuffelhart slaapt voor %s",
    "commands.guhs.hemel.geen_hart": "Geen Knuffelhart in de buurt",
    "commands.guhs.samen.geen_guh": "Njeg: geen eigen tamme guh binnen 24 blokken, of onbekend niveau (lief, mega, zielsguh).",
    "commands.guhs.samen.hartjes": "%s heeft nu %s hartjes: %s",
    "commands.guhs.samen.mislukt": "Njeg: dat lukte nu niet (een eigen, vrije guh in de buurt?).",
}


def teksten():
    """Every key of this module with its Dutch."""
    out = dict(TAAL)
    for naam, plek in PLEKKEN.items():
        out[f"entity.guhs.reisguh.plek.{plek}"] = naam
    for i, n in enumerate(HUISJE_NAMEN):
        out[f"gui.guhs.huisje.standaardnaam.{i}"] = n
    for i, n in enumerate(MODEL_NAMEN):
        out[f"entity.guhs.beauty_model.naam.{i}"] = n
    for i, n in enumerate(RACE_NAMEN):
        out[f"entity.guhs.race_guh.naam.{i}"] = n
    out.update(OVERIG)
    out.update(KLEDING)
    out.update(COMMANDS)
    return out


def _java(*pad):
    with open(os.path.join(JAVA, *pad), encoding="utf-8") as f:
        return f.read()


def _lijst(bron, naam):
    m = re.search(naam + r"\s*=\s*(?:List\.of\(|\{)(.*?)(?:\);|\};)", bron, re.S)
    if not m:
        raise ValueError(f"taal.py: no {naam} in the Java")
    return re.findall(r'"([^"]*)"', m.group(1))


def check(alles):
    """The Java lists must be these lists, and every key the Java code names must be here."""
    problems = []
    if _lijst(_java("feature", "huisje", "Huisjes.java"), "NAMEN") != HUISJE_NAMEN:
        problems.append("Huisjes.NAMEN differs from HUISJE_NAMEN")
    if _lijst(_java("feature", "beauty", "BeautyShow.java"), "MODEL_NAMES") != MODEL_NAMEN:
        problems.append("BeautyShow.MODEL_NAMES differs from MODEL_NAMEN")
    if _lijst(_java("feature", "race", "RaceGame.java"), "GUH_NAMES") != RACE_NAMEN:
        problems.append("RaceGame.GUH_NAMES differs from RACE_NAMEN")
    reis = dict(re.findall(r'Map\.entry\("([^"]+)", "([^"]+)"\)', _java("quest", "Reisguh.java")))
    if reis != PLEKKEN:
        problems.append(f"Reisguh.PLEKKEN differs from PLEKKEN: {reis}")
    gebruikt = set()
    for root, _dirs, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                with open(os.path.join(root, f), encoding="utf-8") as fh:
                    gebruikt.update(re.findall(r'"((?:gui\.guhs\.kleding\.prijs|commands\.guhs|gui\.guhs\.taal|gui\.guhs\.menu\.taal)\.[a-z0-9_.]+)"', fh.read()))
    for k in sorted(gebruikt):
        if k not in alles and not k.endswith("."):
            problems.append(f"the Java uses {k}, but taal.py has no Dutch for it")
    if problems:
        raise ValueError("taal.py:\n  " + "\n  ".join(problems))


def build(h):
    alles = teksten()
    check(alles)
    for k, v in alles.items():
        h.lang(k, v, v)
