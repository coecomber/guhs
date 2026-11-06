"""
1.10 round U, slice U-RUITERS: ambient guhs riding Pluimguhs (Java: feature/vadsrijk/ruiters).

  - texts: entity names, the riders' names, their click lines;
  - the ROUTES are NOT made here (that reads the map tiles, ~1 minute): tools/vadsrijk/ruiters/routes.py, rerun it after a
    zone-1/zone-3 map change (it writes src/main/resources/data/guhs/vadsrijk_ruiters/routes.txt).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_ruiters.py
"""
import json
import os

NAMEN = [   # 0 = the zone-1 courier, 1.. = zone 3 riders
    ("Koerier Zoef", "Courier Zoef"), ("Ruiter Veertje", "Rider Featherly"), ("Draf", "Trot"), ("Galopje", "Little Gallop"),
    ("Tippel", "Tippy"), ("Ruiter Honingdrup", "Rider Honeydrop"), ("Pluk", "Pluck"), ("Wiebel", "Wobble"),
    ("Ruiter Dons", "Rider Fluff"), ("Hupsje", "Hopsy"), ("Zoemer", "Buzzer"), ("Ruiter Kruimel", "Rider Crumb"),
]

TEKSTEN = {   # key -> (nl, en)
    "entity.guhs.vr_ruiter_pluim": ("Pluimguh", "Plumeguh"),
    "entity.guhs.vr_ruiter_guh": ("Rijdende guh", "Riding guh"),
    "vadsrijk.ruiters.zegt": ("%s: \"%s\"", "%s: \"%s\""),
    "vadsrijk.ruiters.z1.0": ("Ooit krijg jij er ook één!", "Someday you'll get one too!"),
    "vadsrijk.ruiters.z1.1": ("Dit is een Pluimguh! Ze komen uit De Schemerwortel, de reuzenboom ver weg.",
                              "This is a Plumeguh! They come from the Schemerwortel, the giant tree far away."),
    "vadsrijk.ruiters.z1.2": ("Post voor Zwamzoet! Opzij, opzij, de koerier komt eraan!", "Mail for Zwamzoet! Make way, the courier is coming!"),
    "vadsrijk.ruiters.z3.0": ("Hup, Pluimpje, hup!", "Go, Plumey, go!"),
    "vadsrijk.ruiters.z3.1": ("Heb jij al een eigen Pluimguh? Vraag het bij de Pluimstal!", "Do you have your own Plumeguh yet? Ask at the Pluimstal!"),
    "vadsrijk.ruiters.z3.2": ("Zachte veren, snelle pootjes!", "Soft feathers, quick little feet!"),
    "vadsrijk.ruiters.z3.3": ("Pas op, bochtje!", "Careful, corner!"),
    "vadsrijk.ruiters.z3.4": ("Mijn Pluimguh kent elke tak van deze boom.", "My Plumeguh knows every branch of this tree."),
    "vadsrijk.ruiters.z3.5": ("Vahoeg! Even wat honing halen.", "Vahoeg! Just fetching some honey."),
    "vadsrijk.ruiters.trap": ("Traptreden zijn het allerleukst: hup, hup, hup!", "Stairs are the best: hop, hop, hop!"),
    "vadsrijk.ruiters.wacht": ("Even wachten tot Pluimpje klaar is met eten.", "Just waiting until Plumey is done eating."),
    "vadsrijk.ruiters.verzorger": ("Nog even de veren borstelen... zo, glanzend!", "Just brushing the feathers... there, shiny!"),
}
for i, (nl, en) in enumerate(NAMEN):
    TEKSTEN[f"vadsrijk.ruiters.naam.{i}"] = (nl, en)


def build(h):
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)    # make_v2.lang(key, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs", "lang")
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print(f"vadsrijk_u_ruiters: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
