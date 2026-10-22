"""
1.6.4: the verblijfsbord's family dialog (feature/safari/verblijf: VerblijfsbordBlockEntity.zetFamilie, VerblijfScreen
samenstellen): per member a parent or a little one, and for a species whose male and female look different (an own _man
model) also a male or a female. It opens by itself when a board is placed, later via "Familie samenstellen..." on the
Familie tab.

  python tools/features/familie164.py     (from the project root): writes these texts into nl_nl.json and, with the English
                                          overlay tools/lang/en/c189_familie164.json, into en_us.json
"""
import json
import os

EN_BESTAND = os.path.join("tools", "lang", "en", "c189_familie164.json")

TEKSTEN = {
    "gui.guhs.verblijf.samenstellen.knop": "Familie samenstellen...",
    "gui.guhs.verblijf.samenstellen.knop.uitleg": "Kies wie er in deze familie wonen: per dier een ouder of een kind (en bij sommige "
                                                  "soorten een mannetje of een vrouwtje). Wie verandert, wordt met een poef "
                                                  "omgewisseld; het blijven er altijd vier.",
    "gui.guhs.verblijf.samenstellen.titel": "Stel de familie samen",
    "gui.guhs.verblijf.samenstellen.uitleg": "Kies voor elk dier of het een ouder of een kind is. Bij de %s zien mannetjes en "
                                             "vrouwtjes er hetzelfde uit.",
    "gui.guhs.verblijf.samenstellen.uitleg.geslacht": "Kies voor elk dier of het een ouder of een kind is, en of een ouder een "
                                                      "mannetje of een vrouwtje is: bij de %s zie je het verschil.",
    "gui.guhs.verblijf.samenstellen.dier": "Dier %s",
    "gui.guhs.verblijf.samenstellen.wie.man": "Mannetje",
    "gui.guhs.verblijf.samenstellen.wie.vrouw": "Vrouwtje",
    "gui.guhs.verblijf.samenstellen.wie.ouder": "Ouder",
    "gui.guhs.verblijf.samenstellen.wie.baby": "Kleintje",
    "gui.guhs.verblijf.samenstellen.ouder": "Ouder",
    "gui.guhs.verblijf.samenstellen.kind": "Kind",
    "gui.guhs.verblijf.samenstellen.man": "Mannetje",
    "gui.guhs.verblijf.samenstellen.vrouw": "Vrouwtje",
    "gui.guhs.verblijf.samenstellen.ouder_kind.uitleg": "Klik om te wisselen tussen een ouder (volwassen) en een kind (een kleintje).",
    "gui.guhs.verblijf.samenstellen.geslacht.uitleg": "Klik om te wisselen tussen een mannetje en een vrouwtje. Bij deze soort zie "
                                                      "je het verschil.",
    "gui.guhs.verblijf.samenstellen.wissel": "Wie je verandert, wordt met een poef omgewisseld. Zijn naam blijft.",
    "gui.guhs.verblijf.samenstellen.klaar": "Zo is het goed!",
    "gui.guhs.verblijf.samenstellen.klaar.uitleg": "De familie wordt zo samengesteld. Wie verandert, wordt omgewisseld; het "
                                                   "blijven er altijd vier.",
    "gui.guhs.verblijf.samenstellen.annuleren": "Annuleren",
    "gui.guhs.verblijf.samenstellen.zo_laten": "Zo laten",
    "gui.guhs.verblijf.samenstellen.annuleren.uitleg": "Terug naar het bord; de familie blijft zoals hij is. Je kunt het later "
                                                       "altijd nog wijzigen met Familie samenstellen op de tab Familie.",
    "gui.guhs.verblijf.samenstellen.reden.geen_ouder": "Er moet minstens één ouder in de familie zijn.",
    "gui.guhs.verblijf.samenstellen.ongeldig": "Dat kan niet: een familie heeft minstens één ouder.",
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
    print(f"familie164: {len(TEKSTEN)} texts ({len([k for k in TEKSTEN if k not in en])} without English)")


if __name__ == "__main__":
    _direct()
