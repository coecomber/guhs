"""
Ronde 4 (testronde 4, ontwerp hoofdstuk 6: "geen bevers die met takjes lopen"): kleine voorwerpjes in de bek of op de buik
die alleen tijdens hun eigen actie te zien zijn. De client (SafariClient) verbergt elk bot "prop_<actie>" behalve als het
dier precies die actie (animatie) speelt.

    bever    prop_draag    een takje met twee blaadjes dwars in de bek (draag: naar de dam)
    giraffe  prop_pluk     een acaciablaadje in de bek (pluk)
    otter    prop_schelp   een schelpje op de buik (schelp: op de rug drijven en tikken)

Het voorwerpje is een bot met eigen blokjes, aan het bot van de kop (of het lijf) gehangen, met per-vlak UV's naar een
klein geverfd lapje in een leeg (doorzichtig) stuk van ELKE texture die met dit model getekend wordt (basis, _man en hun
_slaap). Het lapje ligt op dezelfde plek in al die textures, zodat één geo bij allemaal past. Herhaalbaar: een model dat
het bot al heeft wordt overgeslagen.
"""
import json
import os
import random

from PIL import Image

# soort -> (bot, ouderbot, [(origin, size, kleur-index)], [kleuren])
PROPS = {
    "bever": ("prop_draag", "head", [
        ([-8.5, 4.4, -16.2], [17, 1.4, 1.4], 0),          # het takje dwars door de bek
        ([7.2, 5.6, -16.6], [2.6, 2.6, 0.4], 1),          # blaadje rechts
        ([-9.6, 5.2, -16.6], [2.4, 2.4, 0.4], 1),         # blaadje links
    ], [(118, 80, 46), (96, 158, 58)]),
    "giraffe": ("prop_pluk", "head", [
        ([-0.4, 35.2, -19.8], [0.8, 0.8, 4.2], 0),        # steeltje uit de bek
        ([-1.8, 35.0, -22.2], [3.6, 0.5, 3.0], 1),        # het blaadje
        ([-1.2, 35.3, -19.6], [2.4, 0.5, 2.2], 1),
    ], [(110, 86, 52), (104, 166, 64)]),
    "otter": ("prop_schelp", "body", [
        ([-1.8, 1.2, -4.4], [3.6, 0.8, 3.4], 0),          # schelp op de buik (onderkant; bij drijven boven)
        ([-1.2, 0.8, -4.0], [2.4, 0.5, 2.6], 1),          # bolle bovenkant
    ], [(232, 196, 172), (246, 222, 204)]),
}

KLEUR_TEXELS = 4          # per kleur een lapje van 4x4 modeltexels


def _vrij(texturen, tw, th, schaal, breedte, hoogte):
    """The first (u, v) in model texels where a breedte x hoogte patch is fully transparent in every texture."""
    alfa = [t.split()[-1].load() for t in texturen]
    for v in range(th - hoogte, -1, -1):
        for u in range(tw - breedte, -1, -1):
            x0, y0 = u * schaal, v * schaal
            if all(a[x, y] == 0 for a in alfa for y in range(y0, y0 + hoogte * schaal) for x in range(x0, x0 + breedte * schaal)):
                return u, v
    return None


def _schilder(img, u, v, schaal, kleur, rnd):
    px = img.load()
    for y in range(v * schaal, (v + KLEUR_TEXELS) * schaal):
        for x in range(u * schaal, (u + KLEUR_TEXELS) * schaal):
            d = rnd.randint(-10, 10)
            px[x, y] = tuple(max(0, min(255, c + d)) for c in kleur) + (255,)


def voeg_toe(soort, geo_dir, tex_dir):
    """Adds the soort's prop bone to its base and _man geo (and paints its patch into their textures); returns the files
    changed (empty when it has no prop or every model already has it)."""
    if soort not in PROPS:
        return []
    bot, ouder, blokjes, kleuren = PROPS[soort]
    geos = [os.path.join(geo_dir, f"{n}.geo.json") for n in (soort, f"{soort}_man")]
    geos = [g for g in geos if os.path.exists(g)]
    namen = [soort] + ([f"{soort}_man"] if os.path.exists(os.path.join(tex_dir, f"{soort}_man.png")) else [])
    pngs = [os.path.join(tex_dir, f"{n}{s}.png") for n in namen for s in ("", "_slaap")]
    pngs = [p for p in pngs if os.path.exists(p)]
    data = [json.load(open(g, encoding="utf-8")) for g in geos]
    if all(any(b["name"] == bot for b in d["minecraft:geometry"][0]["bones"]) for d in data):
        return []
    g0 = data[0]["minecraft:geometry"][0]["description"]
    tw, th = g0["texture_width"], g0["texture_height"]
    beelden = [Image.open(p).convert("RGBA") for p in pngs]
    schaal = beelden[0].width // tw
    plek = _vrij(beelden, tw, th, schaal, KLEUR_TEXELS * len(kleuren), KLEUR_TEXELS)
    if plek is None:
        raise SystemExit(f"props: geen leeg plekje in de textures van {soort}")
    u0, v0 = plek
    for img, pad in zip(beelden, pngs):
        for i, k in enumerate(kleuren):
            _schilder(img, u0 + i * KLEUR_TEXELS, v0, schaal, k, random.Random(f"{soort}{i}"))
        img.save(pad)
    gewijzigd = list(pngs)
    for d, g in zip(data, geos):
        bones = d["minecraft:geometry"][0]["bones"]
        if any(b["name"] == bot for b in bones):
            continue
        o = next(b for b in bones if b["name"] == ouder)
        cubes = []
        for origin, size, ki in blokjes:
            uv = {"uv": [u0 + ki * KLEUR_TEXELS + 0.5, v0 + 0.5], "uv_size": [KLEUR_TEXELS - 1, KLEUR_TEXELS - 1]}
            cubes.append({"origin": origin, "size": size,
                          "uv": {f: dict(uv) for f in ("north", "south", "east", "west", "up", "down")}})
        bones.append({"name": bot, "parent": ouder, "pivot": list(o.get("pivot", [0, 0, 0])), "cubes": cubes})
        with open(g, "w", encoding="utf-8") as f:
            json.dump(d, f, indent=1)
        gewijzigd.append(g)
    return gewijzigd
