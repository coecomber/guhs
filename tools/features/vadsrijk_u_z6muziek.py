"""Het Vadsrijk 1.10, U-Z6MUZIEK: the music + ambience of zone 6, Het Zijdeland (ZONE6_PLAN §9.4).
Java: feature/vadsrijk/geluid (GeluidPlan: themes, bosses, stingers, beds, streken, the u-z6raid id alias) and
feature/vadsrijk/stadsmuziek (StadsMuziek: the towns Rozenkloof + Saffraanpoort).

Writes into sounds.json (add-only):
  vr.geluid.gebied.<thema>        6 area themes (streamed)                muziek/gebied/<thema>.ogg
  vr.geluid.baas.<baas>[_fase]    9 boss themes + their last-phase loops   muziek/baas/<baas>[_fase].ogg
  vr.geluid.fanfare.<baas>        9 victory fanfares                       muziek/fanfare/<baas>.ogg
  vr.geluid.stinger.<soort>       baken, moederoase                        muziek/stinger/<soort>.ogg
  vr.geluid.bed.<bed> / sfeer.<bed>  duinen, oase, bazaar, sterren, toren  sfeer/bed_<bed>.ogg + one-shots
  vr.stad.muziek.<stad>_<n>       2 town tunes each for Rozenkloof + Saffraanpoort   muziek/stad/<stad>_<n>.ogg
  vr.stad.sfeer.<stad>, vr.stad.bed.bazaar   the town ambience
  vr.z6raid.muziek.opgevrolijkt / .wachtzaal   u-z6raid's two non-boss ids (Ringeling's sweet fanfare, the tower drone)
The OGGs are committed; make them with
  python tools/vadsrijk/geluid/maak_muziek.py <names>   (pieces in tools/vadsrijk/geluid/zijdeland.py)
  python tools/vadsrijk/geluid/maak_sfeer.py duinen oase bazaar sterren toren belletje sterretje zoutzang
Run standalone from the project root:  PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6muziek.py
"""
import json
import os

A = os.path.join("src", "main", "resources")
SND = os.path.join(A, "assets", "guhs", "sounds", "vadsrijk")

GEBIED = ["roze_duinen", "palmenkloven", "zoutvlakte", "zoutvlakte_nacht", "ruinedal", "moederoase"]
BAZEN = ["wormelien", "zoutkolos", "sterrenwacht", "glimmertje", "verzonken_sterrenguh",
         "zandwachter", "spiegelvizier", "sterrenbeelden", "ringeling"]
STINGERS = ["baken", "moederoase"]
STADMUZIEK = ["rozenkloof_1", "rozenkloof_2", "saffraanpoort_1", "saffraanpoort_2"]
BEDDEN = ["duinen", "oase", "bazaar", "sterren", "toren"]
LOS = {"belletje": 3, "sterretje": 3, "zoutzang": 3, "vogel": 4, "druppel": 4, "krekel": 2, "glinster": 3}


def f(soort, w=1, vol=1.0, pitch=1.0):
    return [{"name": f"guhs:vadsrijk/sfeer/{soort}_{i}", "weight": w, "volume": vol, "pitch": pitch} for i in range(1, LOS[soort] + 1)]


def ev(naam, w=1, vol=1.0, pitch=1.0):
    return {"name": naam, "type": "event", "weight": w, "volume": vol, "pitch": pitch}


def guh(nrs, w=1, vol=0.5, pitch=1.1):
    return [{"name": f"guhs:guh_ambient{n}", "weight": w, "volume": vol, "pitch": pitch} for n in nrs]


WINDGONG = "guhs:klaterdal.windgong"
# the now-and-then sounds per bed (soft; no wind)
SFEER = {
    "duinen": f("belletje", 3, 0.45) + f("vogel", 1, 0.35) + f("glinster", 1, 0.25),
    "oase": f("vogel", 3, 0.55) + f("druppel", 1, 0.4) + [ev("minecraft:entity.frog.ambient", 2, 0.3, 1.3)],
    "bazaar": guh([2, 5, 9, 12], 2, 0.5, 1.15) + guh([3, 7], 1, 0.45, 0.95) + f("belletje", 1, 0.35) + [ev(WINDGONG, 1, 0.35, 1.1)],
    "sterren": f("krekel", 3, 0.45) + f("sterretje", 3, 0.45) + f("zoutzang", 2, 0.35) + [ev("guhs:vogels.guh_uiltje", 2, 0.4)],
    "toren": f("druppel", 1, 0.3) + [{"name": f"guhs:vadsrijk/sfeer/zoutzang_{i}", "weight": 1, "volume": 0.25, "pitch": 0.5}
                                     for i in range(1, 4)],
}
STAD_SFEER = {
    # Rozenkloof: echo in the canyon, swallows, the airport bell now and then, camel bells at the stable, guh voices
    "rozenkloof": f("vogel", 3, 0.5) + f("belletje", 2, 0.45) + guh([3, 7, 10], 1, 0.45, 1.05)
                  + [ev("minecraft:block.bell.use", 1, 0.15, 0.8), ev(WINDGONG, 1, 0.4)],
    # Saffraanpoort: the bazaar murmur bed + guh merchants, wind chimes, the noria creak, fountains
    "saffraanpoort": guh([2, 5, 9, 12], 3, 0.5, 1.15) + f("belletje", 1, 0.35) + [ev(WINDGONG, 2, 0.4, 1.1)]
                     + [{"name": "guhs:vadsrijk/sfeer/kraak_1", "weight": 1, "volume": 0.35, "pitch": 0.8}]
                     + f("druppel", 1, 0.3),
}


def sounds(d):
    for t in GEBIED:
        d[f"vr.geluid.gebied.{t}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/gebied/{t}", "stream": True}]}
    for b in BAZEN:
        d[f"vr.geluid.baas.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/baas/{b}", "stream": True}]}
        d[f"vr.geluid.baas.{b}_fase"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/baas/{b}_fase", "stream": True}]}
        d[f"vr.geluid.fanfare.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/fanfare/{b}"}]}
    for s in STINGERS:
        d[f"vr.geluid.stinger.{s}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/stinger/{s}"}]}
    for b in BEDDEN:
        d[f"vr.geluid.bed.{b}"] = {"sounds": [{"name": f"guhs:vadsrijk/sfeer/bed_{b}"}]}
        d[f"vr.geluid.sfeer.{b}"] = {"sounds": SFEER[b]}
    for n in STADMUZIEK:
        d[f"vr.stad.muziek.{n}"] = {"sounds": [{"name": f"guhs:vadsrijk/muziek/stad/{n}", "stream": True}]}
    for stad, l in STAD_SFEER.items():
        d[f"vr.stad.sfeer.{stad}"] = {"sounds": l}
    d["vr.stad.bed.bazaar"] = {"sounds": [{"name": "guhs:vadsrijk/sfeer/bed_bazaar"}]}
    # u-z6raid's non-boss ids (sounds.json only; the boss ids themselves are mapped in GeluidPlan.alias)
    d["vr.z6raid.muziek.opgevrolijkt"] = {"sounds": [{"name": "guhs:vadsrijk/muziek/fanfare/ringeling"}]}
    d["vr.z6raid.muziek.wachtzaal"] = {"sounds": [{"name": "guhs:vadsrijk/sfeer/bed_toren"}]}


def bestanden():
    l = [os.path.join("muziek", "gebied", t) for t in GEBIED]
    l += [os.path.join("muziek", "baas", b + x) for b in BAZEN for x in ("", "_fase")]
    l += [os.path.join("muziek", "fanfare", b) for b in BAZEN] + [os.path.join("muziek", "stinger", s) for s in STINGERS]
    l += [os.path.join("muziek", "stad", n) for n in STADMUZIEK] + [os.path.join("sfeer", "bed_" + b) for b in BEDDEN]
    l += [os.path.join("sfeer", f"{s}_{i}") for s, n in LOS.items() for i in range(1, n + 1)]
    l += ["sfeer/kraak_1"]
    return l


def controleer():
    mis = [p for p in bestanden() if not os.path.exists(os.path.join(SND, p + ".ogg"))]
    if mis:
        raise SystemExit(f"vadsrijk_u_z6muziek: sounds missing: {mis} (run tools/vadsrijk/geluid/maak_muziek.py + maak_sfeer.py)")


def build(h):
    controleer()
    h.patch_json(os.path.join(A, "assets", "guhs", "sounds.json"), sounds)


def _direct():
    controleer()
    p = os.path.join(A, "assets", "guhs", "sounds.json")
    with open(p, encoding="utf-8") as fh:
        d = json.load(fh)
    sounds(d)
    with open(p, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(d, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    print(f"vadsrijk_u_z6muziek: {len(bestanden())} files checked, sounds.json patched")


if __name__ == "__main__":
    _direct()
