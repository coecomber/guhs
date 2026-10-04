"""
1.2.0 (Guhs in two languages): the Dutch of the texts the Java side needed for the NL/EN switch and for text that used to be
resolved (or hard-coded in Dutch) on the server. No registry content: this module only writes lang (Dutch; the English is in
tools/lang/en/*.json like every other key).

  gui.guhs.menu.taal / gui.guhs.taal.*          the language button of the Guhdex and the Superkompas (client/GuhsTaal: Auto / NL / EN)
  gui.guhs.taalvraag.*                          the first-join question "Welke taal wil je voor Guhs?" (client/screen/TaalVraagScreen)
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
                                  "(Ook bij Mods > Guhs > Config. Een bord dat al in beeld is wisselt mee; een scorebord pas als het "
                                  "weer ververst.)",
    "gui.guhs.taal.auto": "Auto",
    "gui.guhs.taal.nl": "Nederlands",
    "gui.guhs.taal.en": "English",
    "gui.guhs.taal.auto_is": "Auto (%s)",
    "gui.guhs.taal.kort.nl": "NL",
    "gui.guhs.taal.kort.en": "EN",
    # the first-join question (client/screen/TaalVraagScreen): shown in BOTH languages, so the English of these keys is
    # used even when the switch says Dutch
    "gui.guhs.taalvraag.vraag": "Welke taal wil je voor Guhs?",
    "gui.guhs.taalvraag.auto": "Automatisch",
    "gui.guhs.taalvraag.auto.tooltip": "Volgt de taal van Minecraft: Nederlands als Minecraft Nederlands is, anders Engels.",
    "gui.guhs.taalvraag.later": "Je kunt het altijd later veranderen in je Guhdex (je krijgt een nieuwe als je naar de Guhmensie "
                                "gaat) of bij Mods → Guhs → Config.",
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
    "gui.guhs.huisje.meldingen": "Meldingen van zeldzame vondsten: %s",   # 1.2.5
    "gui.guhs.huisje.meldingen.tooltip": "Klik om de berichten in de chat aan of uit te zetten als een bewoner iets zeldzaams vindt (marshmallowknabbel, guhkristal...). Het dagboekje schrijft het altijd op.",   # 1.2.5
    # 1.2.6: the short place of a guh in the Mijn guhs list (same args as the long gui.guhs.band.plek.* texts)
    "gui.guhs.band.plek.wereld.kort": "Loopt rond (%2$s)",
    "gui.guhs.band.plek.zit.kort": "Zit te wachten (%2$s)",
    "gui.guhs.band.plek.huisje.kort": "Guhhuisje %1$s",
    "gui.guhs.band.plek.slaapt_in_huisje.kort": "Slaapt in %1$s",
    "gui.guhs.band.plek.rijdt_op.kort": "%1$s zit bovenop",
    "gui.guhs.band.plek.rijdt_mee.kort": "Rijdt mee op %1$s",
    "gui.guhs.band.plek.item_speler.kort": "In de zakken van %1$s",
    "gui.guhs.band.plek.item_kist.kort": "In een kist (%2$s)",
    "gui.guhs.band.plek.item_rugzak.kort": "In de rugzak van %1$s",
    "gui.guhs.band.plek.item_bank.kort": "In een Bank Guh (%2$s)",
    "gui.guhs.band.plek.item_grond.kort": "Pakketje op de grond (%2$s)",
    "gui.guhs.band.plek.guhwiel.kort": "In een Guh Wheel (%2$s)",
    "gui.guhs.band.plek.guhkamer.kort": "Logeert in de Guhkamer",
    "gui.guhs.band.plek.schouder.kort": "Op de schouder van %1$s",
    "gui.guhs.band.plek.in_guh.kort": "In guh %1$s",
    "gui.guhs.band.plek.onbekend.kort": "Geen idee, njeg...",
    "gui.guhs.band.plek.bij_jou.kort": "Bij jou!",
    "gui.guhs.band.plek.in_de_wolkjes.kort": "In de wolkjes",
    "gui.guhs.beroepen.brandweer.guhtje_eerst_blussen": "Eerst blussen! Er branden nog %s vuurtjes. Daarna springt het guhtje in je armen.",   # 1.2.6
    "gui.guhs.beroepen.brandweer.guhtje_zelf": "Het guhtje is zelf maar naar beneden geklommen. Praat met Blusguh om opnieuw te helpen!",   # 1.2.6
    # 1.2.7: the multiplayer fixes (everyone can finish every quest)
    "quest.guhs.cake.again": "De picknick-guhs hadden nog een guh-taart voor je! Breng hem met 3 guh-ballonnen naar Moeder Vadsig.",
    "gui.guhs.guheinde.mager.al_gevoerd": "Deze magere guh heb je al een knabbel gegeven. In de andere cellen zitten er nog meer!",
    "gui.guhs.guheinde.koning.pakje": "De Koningguh geeft je wat je nog mist van zijn koningspakje! Houd het rechtsklik ingedrukt om het te ontgrendelen.",
    "quest.guhs.eilanden.pakje": "De Wolkguh geeft je zijn reserve-wolkenmuts en -wolkenkraag (wat je nog niet had)! Houd ze rechtsklik ingedrukt om ze te ontgrendelen.",
    "entity.guhs.quest_guh.al_gehad": "Guh! Jij hebt mijn vriendje de Bankguh al gekregen. Wil je er nog een? Die kun je zelf maken: een kist met gefrituurde kaasknabbels eromheen!",
    "gui.guhs.piep.recept_gekregen": "Jij hielp ook mee: hier is het recept van de Roze Guh Koek voor jou! Rechtsklik om het te leren.",
    "gui.guhs.gatenkaas.voorraadboek_kopie": "Je schrijft het dagboek van de Voorraadmika snel over: nu heb je een eigen exemplaar!",
    "gui.guhs.guheinde.beloning.later": "Opper-Mika is verslagen terwijl jij even weg was. Jij vocht mee, dus dit is ook voor jou!",
    "gui.guhs.guheinde.gevecht_verlaten": "Niemand meer te zien... Opper-Mika vliegt mopperend weg. Het terugportaal gaat weer open.",
    "quest.guhs.burgemeester.kwijt": "Njeg, ik zie je %s nergens! Kwijt? Geeft niks: maak maar een nieuwe (of haal hem even op), dan tik ik hem af.",
    "quest.guhs.burgemeester.feest_wacht": "Ahum! Alles is er, maar jij bent nog druk met iets anders. Kom straks bij me terug, dan begint het Knusfeest!",
    "gui.guhs.kermis.no_build": "Njeg! Van de Guhkermis blijf je af: hier mag je niks slopen of bouwen. Lekker een rondje rijden!",
    "quest.guhs.kermis.first_full": "Rondje! Maar je zakken zitten vol: maak 3 vakjes vrij, dan krijg je na je volgende rondje je prijzenzakje. Njeg!",
    "quest.guhs.doolhof.uit_de_heg": "Oeps, je zat vast in de heg! Meneer Vadskronkel zet je op het plein.",
    "quest.guhs.circuit.gouden_tijd": "Een gouden tijd: je bent zo snel als de legende! VAHOEG!",
    "quest.guhs.race.ended.idle": "Anderhalve minuut geen ring gehaald? De renguh is ingedut, njeg. Race voorbij!",
    "quest.guhs.golf.wachtende": "%s wil ook graag golfen! Speel lekker door: wie een minuut niks doet, moet de baan vrijgeven.",
    "quest.guhs.golf.te_lang": "Njeg, een kwartier op de baan en er staat iemand te wachten! De Golfguh stopt je rondje.",
    "quest.guhs.verstop.loopt_al": "Er loopt al een spelletje op %s! Je zoekt gezellig mee (samen zoeken telt niet voor records).",
    "quest.guhs.verstop.te_lang": "Een half uur gezocht (%s van %s gevonden): de guhs komen zelf tevoorschijn. Tot de volgende keer!",
    "quest.guhs.verstop.idle": "Je staat al een tijdje stil, dus je stopt met zoeken (%s van %s gevonden). Tot de volgende keer!",
    "quest.guhs.vissen.loopt_al": "Deze wedstrijd is op %s: dat koos de eerste visser.",
    "gui.guhs.elftocht.zak_vol": "Je zakken zitten vol! Maak een vakje vrij voor: %s",
    "gui.guhs.elftocht.kruisje_vol": "Je zakken zitten vol: het Elf-Guhjeskruisje (en de extra elfstempels) krijg je na je volgende tocht. Maak een vakje vrij!",
    "quest.guhs.reis.hoort_hier": "Deze Reisguh hoort bij dit gebouw, njeg! Hij blijft hier lekker zitten.",   # 1.2.1
    # 1.2.6: the titles (feature/titels): the Guhdex tab Titels, the new titles and their hints
    "gui.guhs.guhdex.tab.titels": "Titels",
    "gui.guhs.titels.naam.vriend_van_guhtwo": "Vriend van Guhtwo",
    "gui.guhs.titels.naam.ohana_guh": "Ohana-guh",
    "gui.guhs.titels.naam.wolkenvriend": "Wolkenvriend",
    "gui.guhs.titels.naam.huisjesbouwer": "Huisjesbouwer",
    "gui.guhs.titels.naam.opper_vadser": "Opper-vadser",
    "gui.guhs.titels.naam.guhkenner": "Guhkenner",
    "gui.guhs.titels.hint.held_van_nomguh": "Help Baltoguh in Nomguh",
    "gui.guhs.titels.hint.knuffelburgemeester": "Vier het Grote Knusfeest in het Knuffeldal",
    "gui.guhs.titels.hint.vriend_van_guhtwo": "Help Guhtwo op het kloon-eiland",
    "gui.guhs.titels.hint.ohana_guh": "Word familie van 626-guh op Guhwai'i",
    "gui.guhs.titels.hint.wolkenvriend": "Laat het Knuffelhart in het Hemelkapelletje weer kloppen",
    "gui.guhs.titels.hint.huisjesbouwer": "Bouw samen met de Timmerguh een guhhuisje",
    "gui.guhs.titels.hint.opper_vadser": "Versla Opper-Mika in het Guheinde",
    "gui.guhs.titels.hint.guhkenner": "Maak je Guhdex helemaal vol",
    "gui.guhs.titels.geen": "Geen titel",
    "gui.guhs.titels.geen.tip": "Klik om alleen je naam te laten zien, zonder titel.",
    "gui.guhs.titels.uitleg": "Kies de titel die anderen achter je naam zien",
    "gui.guhs.titels.gekozen": "Dit is je titel!",
    "gui.guhs.titels.kies": "Klik om deze titel te kiezen",
    "gui.guhs.titels.tip.waar": "Je gekozen titel staat achter je naam in de spelerslijst, boven je hoofd en in de chat.",
    "gui.guhs.titels.tip.voorbeeld": "Zo ziet je naam eruit:",
    "gui.guhs.titels.tip.kies": "Klik om deze titel te kiezen.",
    "gui.guhs.titels.tip.weg": "Klik nog een keer om 'm weg te halen.",
    "gui.guhs.titels.tip.op_slot": "Nog op slot. Zo verdien je 'm:",
    "gui.guhs.titels.nieuw": "Nieuwe titel: %s! Kies 'm in je Guhdex (tabblad Titels).",
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
                    gebruikt.update(re.findall(r'"((?:gui\.guhs\.kleding\.prijs|commands\.guhs|gui\.guhs\.taal|gui\.guhs\.taalvraag|gui\.guhs\.menu\.taal)\.[a-z0-9_.]+)"', fh.read()))
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
