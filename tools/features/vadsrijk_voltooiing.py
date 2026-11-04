"""
1.10.0 "Het Vadsrijk", I-VOLTOOIING: the Verkenningsboek (zone completion) texts.
Java: feature/vadsrijk/voltooiing (Voltooiing, ZoneBoeken, VoltooiingRegistry, client/BoekScherm, client/VoltooiingClient).
Only texts (no assets: the icons are vanilla items). en_us gets the same Dutch text.
A new zone page: add "vadsrijk.voltooiing.zone.<id>" + the title "gui.guhs.titels.naam|hint.<titelId>" here.
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_voltooiing.py
"""
import json
import os


def teksten():
    t = {
        "key.guhs.vr_boek": "Verkenningsboek",
        "vadsrijk.voltooiing.titel": "Verkenningsboek",
        "vadsrijk.voltooiing.zone.zone1": "De Knuffelzwammen",
        "vadsrijk.voltooiing.zone.zone2": "De Splinterpieken",
        "vadsrijk.voltooiing.verkend": "%s van de %s gedaan",
        "vadsrijk.voltooiing.bezig": "Hier ben je mee bezig!",
        # categories
        "vadsrijk.voltooiing.cat.hoofd": "Hoofdverhaal",
        "vadsrijk.voltooiing.cat.zij": "Zijmissies",
        "vadsrijk.voltooiing.cat.uitkijk": "Uitkijkpunten",
        "vadsrijk.voltooiing.cat.kampvuur": "Kampvuren",
        "vadsrijk.voltooiing.cat.geheim": "Geheimen",
        "vadsrijk.voltooiing.cat.knuffel": "Verloren knuffels",
        "vadsrijk.voltooiing.cat.baas": "Bazen",
        "vadsrijk.voltooiing.cat.huisdier": "Huisdieren",
        "vadsrijk.voltooiing.cat.rijdier": "Rijdieren",
        "vadsrijk.voltooiing.cat.gebieden": "Gebieden",
        "vadsrijk.voltooiing.cat.later": "%s (later)",
        "vadsrijk.voltooiing.later.tip": "Rijdieren krijg je pas verderop in het Vadsrijk. Ze tellen niet mee voor je procent.",
        # entries
        "vadsrijk.voltooiing.kampvuur.naam": "Kampvuur: %s %s",
        "vadsrijk.voltooiing.kampvuur.los": "Onderweg",
        # hints (never spoil: just a nudge)
        "vadsrijk.voltooiing.hint.verhaal": "Volg het verhaal...",
        "vadsrijk.voltooiing.hint.quest": "Vraag eens rond bij %s.",
        "vadsrijk.voltooiing.hint.ergens": "Ergens in %s...",
        "vadsrijk.voltooiing.hint.uitkijk": "Een hoge naald in %s. Klim naar de top!",
        "vadsrijk.voltooiing.hint.baas": "Woont in %s.",
        "vadsrijk.voltooiing.hint.rijdier.wolkenschaapje": "Heb je eenmaal een rijdier? Stalmeester Hoefje heeft een klusje voor je.",
        "vadsrijk.voltooiing.hint.rijdier.knuffelhert": "Heb je eenmaal een rijdier? Boswachter Mos weet iets over het Sporenwoud.",
        "vadsrijk.voltooiing.hint.rijdier.guhwolf": "Heb je eenmaal een rijdier? In Kandijkom huilt er iets...",
        # milestones + reward
        "vadsrijk.voltooiing.toast.titel": "%s%% verkend!",
        "vadsrijk.voltooiing.toast.tekst": "%s - druk op B voor je boek",
        "vadsrijk.voltooiing.mijlpaal.chat": "Verkenningsboek: %s is voor %s%% verkend! (B)",
        "vadsrijk.voltooiing.vol.titel": "Helemaal verkend!",
        "vadsrijk.voltooiing.vol.onder": "%s: 100%%",
        "vadsrijk.voltooiing.beloning.chat": "Je hebt alles gedaan in %s! Je krijgt de titel %s en %s.",
        "vadsrijk.voltooiing.beloning.nog": "Bij 100%%: de titel %s + %s",
        "vadsrijk.voltooiing.beloning.behaald": "Behaald: de titel %s!",
        # collection tabs
        "vadsrijk.voltooiing.tab.boek": "Verkenningsboek",
        # titles (titles feature)
        "gui.guhs.titels.naam.vr_kenner_van_zwamzoet": "Kenner van Zwamzoet",
        "gui.guhs.titels.hint.vr_kenner_van_zwamzoet": "Doe alles in De Knuffelzwammen van het Vadsrijk (Verkenningsboek, B).",
        "gui.guhs.titels.naam.vr_kenner_van_de_splinterpieken": "Kenner van de Splinterpieken",
        "gui.guhs.titels.hint.vr_kenner_van_de_splinterpieken": "Doe alles in De Splinterpieken van het Vadsrijk (Verkenningsboek, B).",
    }
    return t


def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def build(h):
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        _schrijf_json(p, data)
    print(f"vadsrijk_voltooiing: {len(t)} texts")


if __name__ == "__main__":
    _direct()
