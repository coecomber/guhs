"""
1.6.2: "Roep naar mij" in the Guhdex always works (feature/band/Roepen + UitItem + RoepPost) and first asks "weet je het
zeker?" with what happens where the guh is now (band/client/MijnGuhsTab.vraagRoep): one text per PlekSoort.

  python tools/features/roep162.py     (from the project root): writes these texts into nl_nl.json and, with the English
                                       overlay tools/lang/en/c188_roep162.json, into en_us.json
"""
import json
import os

EN_BESTAND = os.path.join("tools", "lang", "en", "c188_roep162.json")

TEKSTEN = {
    "gui.guhs.mijnguhs.roep.tip": "Roep %s naar je toe, waar hij ook is: in een andere dimensie, in zijn Guhhuisje of de Guhkamer, "
                                  "als opgepakte guh in een kist, in een Guhrad, op vakantie... Je krijgt eerst te zien wat er gebeurt.",
    "gui.guhs.mijnguhs.roep.later": "%1$s zit in de zakken van %2$s, en die is nu niet online. Zodra %2$s inlogt, komt %1$s naar je toe!",
    "gui.guhs.mijnguhs.roep.vraag.titel": "%s naar je toe roepen?",
    "gui.guhs.mijnguhs.roep.vraag.zeker": "Weet je het zeker?",
    "gui.guhs.mijnguhs.roep.vraag.ja": "Ja, kom maar!",
    "gui.guhs.mijnguhs.roep.vraag.nee": "Nee, laat maar",
    # one per place (PlekSoort): what happens when it comes
    "gui.guhs.mijnguhs.roep.vraag.wereld": "%s loopt nu ergens rond. Hij laat alles vallen en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.zit": "%s zit nu ergens (je had hem laten zitten). Hij staat op en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.huisje": "Let op: %s woont nu in zijn Guhhuisje. Als je hem roept, verhuist hij uit het huisje: "
                                          "zijn plekje en zijn klusjes daar vervallen.",
    "gui.guhs.mijnguhs.roep.vraag.slaapt_in_huisje": "Let op: %s ligt te slapen in zijn Guhhuisje. Hij wordt wakker en verhuist "
                                                    "uit het huisje: zijn plekje en zijn klusjes daar vervallen.",
    "gui.guhs.mijnguhs.roep.vraag.rijdt_op": "%1$s wordt nu bereden. De ruiter stapt af en %1$s komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.rijdt_mee": "%s rijdt nu ergens mee. Hij stapt uit en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.item_speler": "Let op: %s zit als opgepakte guh in de zakken van een speler. Hij gaat uit die zakken "
                                               "en komt naar je toe. Is die speler niet online, dan komt hij zodra die inlogt.",
    "gui.guhs.mijnguhs.roep.vraag.item_kist": "Let op: %s zit nu als opgepakte guh in een kist. Hij gaat uit de kist en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.item_rugzak": "Let op: %s zit als opgepakte guh in de rugzak van een andere guh. Hij gaat uit de "
                                               "rugzak en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.item_bank": "Let op: %s zit als opgepakte guh in een Bank Guh. Hij gaat uit de Bank Guh en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.item_grond": "%s ligt als opgepakte guh op de grond. Hij springt op en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.guhwiel": "Let op: %s rent nu in een Guhrad. Als je hem hierheen haalt, stopt het rad en wekt het "
                                           "geen vadskracht meer op.",
    "gui.guhs.mijnguhs.roep.vraag.guhkamer": "%s logeert nu in de Guhkamer. Hij checkt uit en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.schouder": "%s zit op iemands schouder. Hij springt eraf en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.in_guh": "%s zit in een andere guh. Hij komt eruit en komt naar je toe.",
    "gui.guhs.mijnguhs.roep.vraag.onbekend": "We weten niet precies waar %s is. We gaan hem zoeken; lukt dat niet, dan hoor je het.",
    "gui.guhs.mijnguhs.roep.vraag.in_de_wolkjes": "%s is in de wolkjes. Roepen helpt niet, maar het Knuffelhart wel.",
    "gui.guhs.mijnguhs.roep.vraag.bij_jou": "%s was al bij je. Hij komt nog even dichterbij.",
    "gui.guhs.mijnguhs.roep.vraag.op_vakantie": "Let op: %s is op vakantie (Reisbureau). Als je hem nu roept, breekt hij zijn reis af: "
                                               "geen ansichtkaart en geen souvenir.",
    "gui.guhs.mijnguhs.roep.vraag.op_kantoor": "Let op: %s werkt nu op kantoor. Hij klokt meteen uit; het loon van de diensten die "
                                              "al af zijn krijg je nog wel.",
    # 1.6.2: the Knabbelportaal takes picked-up guhs and every kind of "Mijn andere vadsjes" too
    "gui.guhs.knabbelvelden.bezig.opgepakt": "Zit opgepakt ergens (zie hieronder). Hij wordt er eerst uit gehaald.",
    "gui.guhs.knabbelvelden.niet.niet_jouw": "Dit is niet van jou.",
    "gui.guhs.knabbelvelden.scherm.sub": "Njeg, niet dood hoor, gewoon met pensioen! Kies guhs en andere vadsjes en stuur ze heen of haal ze terug.",
    "gui.guhs.knabbelvelden.scherm.thuis": "Bij jou (%s)",
    "gui.guhs.knabbelvelden.scherm.soort": "Soort: %s",
    "gui.guhs.knabbelvelden.scherm.soort.alles": "alles",
    "gui.guhs.knabbelvelden.scherm.soort.guh": "guhs",
    "gui.guhs.knabbelvelden.scherm.vraag.tekst": "Deze vadsjes zijn ergens mee bezig: %s",
    # 1.6.3: a picked-up guh whose item burned away (before 1.6.3) goes to the wolkjes when it is called
    "gui.guhs.mijnguhs.roep.verloren": "%s lag als pakketje op de grond, maar het pakketje is er niet meer (verbrand of kwijt). "
                                       "Njeg... Hij is nu in de wolkjes: met het Knuffelhart haal je hem terug!",
    "gui.guhs.knabbelvelden.scherm.vraag.uitleg": "Ze stoppen ermee en gaan met pensioen (uit hun huisje, uit het Guhrad, uit de kist waar ze opgepakt in zitten...).",

}


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v


def _direct():
    with open(EN_BESTAND, encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, v) for k, v in TEKSTEN.items()})):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"roep162: {len(TEKSTEN)} texts ({len([k for k in TEKSTEN if k not in en])} without English)")


if __name__ == "__main__":
    _direct()
