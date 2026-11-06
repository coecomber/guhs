"""
Sample live JSON in exactly the shape WebExport (src/main/java/.../vadsrijk/web/WebExport.java) writes, for local previews:
status.json, wereld.json, hiscores/<lijst>.json, spelers/<naam>.json, spelers/index.json. Made-up players; real arena ids,
classes, zones and base types from the data, so the pages show what the real thing will look like.
"""
import datetime
import json
import os
import random

LIJSTEN = ("generiek", "knuffelridder", "pluisjager", "vadsmagier", "opgevrolijkt", "bazen", "verkenning")
NAMEN = ["Lieke", "Juiced", "PluisPiet", "Knuffelkoning", "Vadsvos", "Kaasknabbel", "Njeggie", "Zwamzoetje", "Klodderkid", "Honingbeer",
         "Sjokkel", "Wolwinter", "MikaMepper", "Pluimpje"]


def schrijf(uit, data):
    r = random.Random(1110)
    nu = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    iso = lambda d: d.isoformat().replace("+00:00", "Z")  # noqa: E731
    os.makedirs(os.path.join(uit, "hiscores"), exist_ok=True)
    os.makedirs(os.path.join(uit, "spelers"), exist_ok=True)

    def put(naam, obj):
        with open(os.path.join(uit, naam), "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, ensure_ascii=False, indent=1)
    online = NAMEN[:4]
    put("status.json", dict(online=True, tijd=iso(nu), versie="1.10.0", uptime_s=18240, aantal=len(online), max=40, spelers=online, in_vadsrijk=3))
    bazen = []
    for b in data["bazen"]:
        if not b.get("arena") or b.get("spoiler"):
            continue
        over = r.choice([0, 0, r.randint(30, b["arena"]["respawn_s"] or 60)])
        bazen.append(dict(arena=b["arena"]["id"], titel="", terug_over_s=over, staat="verslagen" if over else "wacht",
                          respawn_s=b["arena"]["respawn_s"], zone=int(b["zone"][-1])))
    reset = (nu + datetime.timedelta(hours=7, minutes=12))
    karavaan = dict(fase="stop", stop="zwamzoet", richting=1, route_fractie=0.42, x=40, z=120, vertrekt_over_s=95)
    for p in data["punten"]:
        if p["id"].endswith("kampvuur_klodderweide_1"):
            karavaan.update(x=p["pos"][0], z=p["pos"][2], fase="rijden", stop="")
            karavaan.pop("vertrekt_over_s")
    put("wereld.json", dict(tijd=iso(nu), dagreset=iso(reset), weekzegel="vadsrijk.z3bazen.zegel.haastig",
                            contracten=["vadsrijk.z3.systemen.contract.de_harsreus", "vadsrijk.z3.systemen.contract.moeder_wesp"], bazen=bazen, karavaan=karavaan))
    klassen = [k["id"] for k in data["klassen"]]
    types = [i["id"] for i in data["items"]]
    spelers = []
    for i, naam in enumerate(NAMEN):
        lv = max(1, 50 - i * 3 - r.randint(0, 3))
        kl = {k: max(1, lv - r.randint(0, 12)) for k in klassen}
        boek = {z["id"]: max(0, min(100, 100 - i * 7 - r.randint(0, 20) - z["nr"] * 10)) for z in data["zones"]}
        bz = {b["arena"]["id"]: r.randint(0, 9) for b in data["bazen"] if b.get("arena") and r.random() < 0.6}
        gear = {}
        for s in ("wapen", "hoofd", "borst", "benen", "voeten", "ring"):
            passend = [x["id"] for x in data["items"] if x.get("slot") == s] or types
            if r.random() < 0.85 and passend:
                gear[s] = dict(type=r.choice(passend), zeldzaamheid=r.choice(["gewoon", "ongewoon", "zeldzaam", "episch", "legendarisch", "guhddelijk"][:3 + (i < 5) * 3]),
                               lv=max(1, lv - r.randint(0, 5)), sterren=r.randint(0, 7 if i < 4 else 3), sokjes=r.randint(0, 2))
        spelers.append(dict(naam=naam, uuid="", klasse=r.choice(klassen), titel="vadsrijk.klassen.titel." + r.choice(klassen) + ".1",
                            gezien=(nu - datetime.timedelta(days=r.randint(0, 9))).date().isoformat(), lv=lv, xp=lv * 9000 + r.randint(0, 8000),
                            opgevrolijkt=r.randint(50, 4000) // (i + 1), boek_totaal=sum(boek.values()) // len(boek), huisdieren=r.randint(0, 6), huisdieren_max=6,
                            rijdieren=r.randint(0, 5), rijdieren_max=5, bazen_totaal=sum(bz.values()), klassen=kl, boek=boek, bazen=bz, gear=gear,
                            **({"eerste_50": "2026-11-21"} if lv >= 50 else {})))
    for s in spelers:
        put(f"spelers/{s['naam'].lower()}.json", {k: v for k, v in s.items() if k != "xp"})
    put("spelers/index.json", dict(tijd=iso(nu), spelers=[dict(naam=s["naam"], lv=s["lv"]) for s in spelers]))

    def lijst(id_, score, toon, met_xp=True):
        rij = sorted(spelers, key=lambda s: (-score(s), s["naam"]))
        put(f"hiscores/{id_}.json", dict(lijst=id_, tijd=iso(nu), rijen=[dict(plek=i + 1, naam=s["naam"], waarde=toon(s), **({"xp": score(s)} if met_xp else {}))
                                                                         for i, s in enumerate(x for x in rij if score(x) > 0)]))
    lijst("generiek", lambda s: s["xp"], lambda s: s["lv"])
    for k in klassen:
        lijst(k, lambda s, k=k: s["klassen"][k] * 9000, lambda s, k=k: s["klassen"][k])
    lijst("opgevrolijkt", lambda s: s["opgevrolijkt"], lambda s: s["opgevrolijkt"], False)
    lijst("bazen", lambda s: s["bazen_totaal"], lambda s: s["bazen_totaal"], False)
    lijst("verkenning", lambda s: s["boek_totaal"], lambda s: s["boek_totaal"], False)
    # T-INT: t-ambacht's lists (made-up levels 1..99, xp grows with the level)
    for i_, a in enumerate(["mijnen", "houthakken", "plukken", "vissen", "imkeren", "smeden", "naaien", "koken", "brouwen", "totaal"]):
        lv_ = {s["naam"]: (r.randint(1, 99) if a != "totaal" else r.randint(9, 891)) for s in spelers}
        lijst("ambacht_" + a, lambda s, lv_=lv_: lv_[s["naam"]] * 1200, lambda s, lv_=lv_: lv_[s["naam"]])
