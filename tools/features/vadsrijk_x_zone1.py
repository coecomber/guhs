"""
1.10 "Het Vadsrijk", round X, slice X-ZONE1 (Java: feature/vadsrijk/zonepoort): the zone gate.

  - server config guhs-vadsrijk-zones.toml (openZones) + /guhs vadsrijk zones open <n>;
  - every way into a closed zone says "binnenkort" (ship, travel map, quests, dungeons), the tracker says "Wordt vervolgd";
  - the safety net at the zone border and the soft cap (level stops at the next zone's entry level, extra xp banked).

No assets. Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_x_zone1.py
"""
import json
import os

K = "vadsrijk.zonepoort."
ZONES = {1: ("De Knuffelzwammen", "the Snuggle Shrooms"), 2: ("De Splinterpieken", "the Splinter Peaks"), 3: ("De Schemerwortel", "the Duskroot"),
         4: ("De Lampionkust", "the Lantern Coast"), 5: ("Hoogvads", "Highchonk"), 6: ("Het Zijdeland", "the Silklands")}   # (= vadsrijk.voltooiing.zone.*)
PLEKKEN = {"kandijkom": 2, "schemerwortel": 3, "lampionkust": 4, "hoogvads": 5, "zijdeland": 6}

TEKSTEN = {
    K + "schip": ("Het schip naar %s vaart binnenkort! Houd Discord in de gaten.", "The ship to %s sails soon! Keep an eye on Discord."),
    K + "kort": ("binnenkort! Houd Discord in de gaten.", "soon! Keep an eye on Discord."),
    K + "kaart": ("Binnenkort! Houd Discord in de gaten.", "Coming soon! Keep an eye on Discord."),
    K + "volg.titel": ("Wordt vervolgd", "To be continued"),
    K + "volg.wat": ("Het schip naar %s vaart binnenkort", "The ship to %s sails soon"),
    K + "volg.waar": ("Houd Discord in de gaten!", "Keep an eye on Discord!"),
    K + "dungeon": ("%s ligt in %s: die zone gaat binnenkort open!", "%s lies in %s: that zone opens soon!"),
    K + "grens": ("%s is nog dicht: het schip daarheen vaart binnenkort! Houd Discord in de gaten.",
                  "%s is still closed: the ship there sails soon! Keep an eye on Discord."),
    K + "eis": ('Naar %1$s: %2$s hoofdverhaal %3$s · %4$s Verkenningsboek %5$s%% / %6$s%% (toets B)', 'To %1$s: %2$s main story %3$s · %4$s Exploring Book %5$s%% / %6$s%% (key B)'),
    K + "eis.kort": ('eerst het hoofdverhaal en je Verkenningsboek (B)', 'first the main story and your Exploring Book (B)'),
    K + "eis.kaart": ('Verken eerst de vorige zone: Verkenningsboek %s%% / 75%% (toets B)', 'Explore the previous zone first: Exploring Book %s%% / 75%% (key B)'),
    K + "eis.kaart_verhaal": ('Speel eerst het hoofdverhaal uit en verken de vorige zone (Verkenningsboek %s%% / 75%%)', 'Finish the main story first and explore the previous zone (Exploring Book %s%% / 75%%)'),
    K + "verken.titel": ('Volgende: verken %s tot %s%%', 'Next: explore %s to %s%%'),
    K + "verken.wat": ('Je Verkenningsboek (toets B): nu %s%% van %s%%', 'Your Exploring Book (key B): %s%% of %s%% now'),
    K + "verken.waar": ('Daarna mag je met het schip naar %s!', 'Then the ship takes you to %s!'),
    K + "dungeon.eis": ('%s: %s', '%s: %s'),
    K + "weg_open": ('De weg naar %s is open! Het schip wacht op je.', 'The way to %s is open! The ship is waiting for you.'),
    K + "plafond": ('Level %s: je hoogste level tot je %s betreedt! Extra xp spaar je op (tot %s levels) en die krijg je er straks dubbel bij.', 'Level %s: your highest level until you enter %s! Extra xp is saved up (up to %s levels) and you get it as a double bonus later.'),
    K + "menu.max": ('Hoogste level voor nu: %s (betreed %s voor meer). Gespaarde xp: %s', 'Highest level for now: %s (enter %s for more). Saved xp: %s'),
}
for _z, (_nl, _en) in ZONES.items():
    TEKSTEN[K + f"zone.{_z}"] = (_nl, _en[0].upper() + _en[1:])
    TEKSTEN[K + f"lijn.{_z}"] = (f"Het schip naar {_nl} vaart binnenkort! Houd Discord in de gaten.",
                                 f"The ship to {_en} sails soon! Keep an eye on Discord.")
for _p, _z in PLEKKEN.items():
    _nl, _en = ZONES[_z]
    TEKSTEN[K + f"reis.{_p}"] = (f"Het schip naar {_nl} vaart binnenkort! Houd Discord in de gaten.",
                                 f"The ship to {_en} sails soon! Keep an eye on Discord.")


def build(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, d)
    print(f"vadsrijk_x_zone1: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
