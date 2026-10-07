"""
einde2 (the Guheinde expansion), slice sponsguh_wezens: the cast of the Sponsguh story as creatures, the Knabbelburgermobiel and
the story's pure-content rewards (food, blocks, a toy, outfits). DESIGN 5b; no story logic (phase B).
Java: nl.juiced.guhs.feature.einde2.sponsguh (E2SponsguhFeature; the table of the cast: Cast).

  einde2_sponsguh_wezens_modellen   the twenty-one GeckoLib models with their animations and textures
  einde2_sponsguh_wezens_blokken    the nine blocks and two foods: textures, models, loot, recipes, tags
  einde2_sponsguh_wezens_tekst      every Dutch text, the sounds
  this module                       the seven clothes (bones, textures, icons), the two Guhdex pages, the spawn eggs, the game
                                    test rooms, the self-check

The clothes textures and bones only appear after `python tools/make_guh_variants.py` (+ make_sleep_eyes.py,
make_clothes_icons.py). Nothing of the slice is in the FTB quests (phase B's story is).
"""
import os

import numpy as np

from features import einde2_kit as kit
from features import einde2_sponsguh_wezens_blokken as blokken
from features import einde2_sponsguh_wezens_modellen as modellen
from features import einde2_sponsguh_wezens_tekst as tekst
from features import spelen

TEXTS = tekst.TEXTS

# --- clothes ---------------------------------------------------------------------------------------------------------------
CLOTHES = ["sponsguh_vierkante_broek", "sponsguh_knabbelpetje", "sponsguh_bloemetjesbroek", "sponsguh_luchthelm", "sponsguh_antennes",
           "sponsguh_ijshoedje", "sponsguh_heldencape"]
_H, _B = [0, 6, -2], [0, 6, 6]      # head and body pivots of the guh model
BONES = {
    # the square trousers: the suit bones carry the trousers and the shirt; under the chin a white collar and a red tie
    "outfit_sponsguh_das": ("head", _H, "sponsguh_das", [([-0.7, 0.2, -12.7], [1.4, 2.2, 0.6], 0), ([-0.9, 2.2, -12.8], [1.8, 1.1, 0.7], 0)]),
    "outfit_sponsguh_das_kraag": ("head", _H, "sponsguh_kraag", [([-3.5, 2.3, -12.55], [2.6, 1.2, 0.5], 0), ([0.9, 2.3, -12.55], [2.6, 1.2, 0.5], 0)]),
    # the Krokante Knabbel cap: the cap bones in white, a blue anchor on the front
    "outfit_sponsguh_anker": ("head", _H, "sponsguh_anker", [([-1.0, 15.3, -10.35], [2.0, 1.4, 0.4], 0)]),
    # Sandy's air helmet: a glass dome round the head and the ears (its swatch is see-through but for the frame), a flower on top
    "outfit_sponsguh_helm": ("head", _H, "sponsguh_glas", [([-12.2, 0.6, -13.4], [24.4, 17.6, 12.6], 0)]),
    "outfit_sponsguh_helm_bloem": ("head", _H, "sponsguh_bloem", [([-1.8, 18.2, -8.8], [3.6, 0.9, 3.6], 0), ([-0.7, 19.1, -7.7], [1.4, 0.6, 1.4], 0)]),
}
# Plankmika's antennas: a thin stalk with a knob on each guh ear
BONES.update(spelen.oren("outfit_oren_sponsguh", "sponsguh_antenne", cubes=[([7.6, 15.6, -5.9], [0.8, 5.2, 0.8], 0),
                                                                            ([7.1, 20.6, -6.4], [1.8, 1.8, 1.8], 0)]))
BRUIN, WIT, ROOD, GROEN, PAARS = (136, 86, 44), (248, 248, 250), (214, 44, 48), (112, 196, 84), (150, 92, 200)
ANTENNE_ICON = ["................", "..kk........kk..", "..kk........kk..", "...g........g...", "...g........g...", "....g......g....",
                "....g......g....", ".....g....g.....", ".....g....g.....", "......g..g......", "......g..g......", ".....pppppp.....",
                "....pppppppp....", "................", "................", "................"]
IJS_ICON = ["................", "......rrrr......", ".....rRRRRr.....", "....rRRwRRRr....", "....rRRRRRRr....", "....rrrrrrrr....",
            "....wkwkwkwk....", ".....kwkwkw.....", ".....wkwkwk.....", "......kwkw......", "......wkwk......", ".......kw.......",
            ".......wk.......", "................", "................", "................"]


def clothes(rng, v):
    """The textures of the seven pieces (the hook's rng shifts with the module's place in FEATURES: kit.rng does not)."""
    r = kit.rng("sponsguh_wezens/kleding")
    px = 32

    def broek():
        a = v.fabric(BRUIN, r, 8)
        a[0:12, :] = v.fabric(WIT, r, 4)[0:12, :]                # the white shirt above the belt
        a[12:16, :] = (50, 34, 26)                               # the belt
        a[13:15, 4:28:6] = (30, 22, 18)
        return np.clip(a, 0, 255)

    def bloemetjes():
        a = v.dots(GROEN, PAARS, r, 8, 3)
        for y in range(1, px, 8):
            for x in range(((y // 8) % 2) * 4 + 1, px, 8):
                a[y:y + 1, x:x + 1] = (250, 230, 120)            # the heart of each flower
        return np.clip(a, 0, 255)

    def petje():
        return np.clip(v.band(WIT, (70, 110, 200), r, [26]), 0, 255)

    def glas():
        a = np.zeros((px, px, 4), np.float32)
        a[..., :3] = (214, 240, 250)
        a[0:2, :, 3] = a[-2:, :, 3] = a[:, 0:2, 3] = a[:, -2:, 3] = 255     # the frame of the dome
        for i in range(5):                                                   # a glint
            a[6 + i, 9 - i, :] = (255, 255, 255, 255)
            a[7 + i, 9 - i, :] = (255, 255, 255, 255)
        return a

    def bloem():
        a = v.fabric((255, 130, 190), r, 6)
        a[12:20, 12:20] = (250, 220, 90)
        return np.clip(a, 0, 255)

    def wafel():
        a = v.fabric((226, 170, 90), r, 6)
        for i in range(0, px, 6):
            a[i:i + 1, :] = (176, 120, 50)
            a[:, i:i + 1] = (176, 120, 50)
        return np.clip(a, 0, 255)

    def cape():
        a = v.fabric((70, 130, 220), r, 8)
        a[28:32, :] = (250, 214, 90)                             # a golden hem
        return np.clip(a, 0, 255)
    return {
        "sponsguh_vierkante_broek": {"suit": broek, "sponsguh_das": (lambda: v.fabric(ROOD, r, 6)), "sponsguh_kraag": (lambda: v.fabric(WIT, r, 4))},
        "sponsguh_knabbelpetje": {"cap": petje, "sponsguh_anker": (lambda: v.fabric((70, 110, 200), r, 5))},
        "sponsguh_bloemetjesbroek": {"suit": bloemetjes},
        "sponsguh_luchthelm": {"sponsguh_glas": glas, "sponsguh_bloem": bloem},
        "sponsguh_antennes": {"sponsguh_antenne": (lambda: v.fabric((44, 104, 62), r, 6))},
        "sponsguh_ijshoedje": {"party_hat": wafel, "pom": (lambda: v.fabric((255, 150, 200), r, 6))},
        "sponsguh_heldencape": {"cape": cape},
    }


def icons(ic):
    return {
        "sponsguh_vierkante_broek": ic.shirt(BRUIN, (86, 52, 26), ROOD, "buttons"),
        "sponsguh_knabbelpetje": ic.shaped("cap", (150, 160, 180), WIT, (70, 110, 200)),
        "sponsguh_bloemetjesbroek": ic.shirt(GROEN, (60, 130, 50), PAARS, "stars"),
        "sponsguh_luchthelm": ic.shaped("helmet", (130, 180, 204), (214, 240, 250), (255, 130, 190)),
        "sponsguh_antennes": ic.icon(ANTENNE_ICON, {"k": (30, 76, 44), "g": (44, 104, 62), "p": (236, 128, 168)}),
        "sponsguh_ijshoedje": ic.icon(IJS_ICON, {"r": (226, 110, 160), "R": (255, 150, 200), "w": (226, 170, 90), "k": (176, 120, 50)}),
        "sponsguh_heldencape": ic.shaped("cape", (40, 70, 150), (70, 130, 220)),
    }


# =====================================================================================================================
def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)
    for id, (naam, zeldzaamheid, info) in tekst.PAGINAS.items():
        kit.pagina(h, id, naam, zeldzaamheid, info)


def sounds(h):
    def patch(d):
        for event in [e for e in d if e.startswith("einde2_sponsguh_wezens.")]:
            del d[event]                                         # (ours first out, then in: idempotent)
        for event, entries in tekst.SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for event, text in tekst.ONDERTITELS.items():
        kit.lang(h, f"subtitles.guhs.{event}", text)


def wezens(h):
    """Spawn eggs and (empty) loot tables of the cast: nobody of them ever drops anything."""
    for id, (naam, basis, vlek) in tekst.NAMEN.items():
        kit.spawn_ei(h, id, f"{naam}-spawnei", basis, vlek)
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})
    h.w(f"{h.D}/loot_table/entities/knabbelburgermobiel.json", {"type": "minecraft:entity", "pools": []})


# the game test rooms (Java: E2SponsguhGameTests): a plain floor, and a floor with a raised road that ends in the air
TEST_VLOER, TEST_WEG = "einde2_sponsguh_wezens_test_vloer", "einde2_sponsguh_wezens_test_weg"
WEG_Y, WEG_X0, WEG_X1, WEG_Z0, WEG_Z1 = 5, 2, 12, 5, 9           # the road: template y of its deck, and its box


def test_kamers(h):
    for naam, maat in ((TEST_VLOER, (15, 8, 15)), (TEST_WEG, (21, 10, 15))):
        s = h.Structure(maat)
        for x in range(maat[0]):
            for z in range(maat[2]):
                s.set(x, 0, z, ("minecraft:yellow_concrete", "minecraft:white_concrete")[(x + z) % 2])
        if naam == TEST_WEG:
            s.fill(WEG_X0, WEG_Y, WEG_Z0, WEG_X1, WEG_Y, WEG_Z1, "minecraft:smooth_stone")
        s.save(naam)


def build(h):
    modellen.build(h)
    blokken.build(h)
    texts(h)
    sounds(h)
    wezens(h)
    test_kamers(h)
    selfcheck(h)


def selfcheck(h):
    problems = modellen.check(h) + blokken.check(h)
    for id in tekst.NAMEN:
        for pad in (f"{h.A}/models/item/{id}_spawn_egg.json", f"{h.D}/loot_table/entities/{id}.json"):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
        for key in (f"entity.guhs.{id}", f"item.guhs.{id}_spawn_egg", f"gui.guhs.einde2_sponsguh_wezens.praatje.{id}"):
            if key not in h.NL:
                problems.append(f"no text {key}")
    for stuk in CLOTHES:
        if f"item.guhs.{stuk}" not in h.NL:
            problems.append(f"no name for the clothes piece {stuk}")
    for event in tekst.SOUNDS:
        if f"subtitles.guhs.{event}" not in h.NL:
            problems.append(f"sound {event} has no subtitle")
    for naam in (TEST_VLOER, TEST_WEG):
        if not os.path.exists(f"{h.D}/structure/{naam}.nbt"):
            problems.append(f"missing the test room {naam}")
    # the Java side: the cast table, the clothes, the sounds, the numbers of the test road
    J = os.path.join("src", "main", "java", "nl", "juiced", "guhs")

    def java(*pad):
        return open(os.path.join(J, *pad), encoding="utf-8").read()
    cast = java("feature", "einde2", "sponsguh", "Cast.java")
    for id, (_, _, eigen) in modellen.CAST.items():
        regel = next((r for r in cast.splitlines() if r.strip().startswith(id.upper() + "(")), None)
        if regel is None:
            problems.append(f"Cast.java has no {id.upper()}")
            continue
        for an in eigen:
            if not an.startswith(("vlag_", "soort_")) and f'"{an}"' not in regel:
                problems.append(f"Cast.{id.upper()} does not list the animation {an}")
    for id in tekst.NAMEN:
        if id.upper() + "(" not in cast:
            problems.append(f"{id} has a name but is not in Cast.java")
    kleren = java("entity", "GuhClothes.java")
    blok = kleren[kleren.find("// <einde2_sponsguh>"):kleren.find("// </einde2_sponsguh>")]
    for stuk in CLOTHES:
        if f"    {stuk.upper()}(" not in blok:
            problems.append(f"clothes {stuk} is not in GuhClothes between the einde2_sponsguh markers")
    feature = java("feature", "einde2", "sponsguh", "E2SponsguhFeature.java")
    for event in tekst.SOUNDS:
        if f'"{event}"' not in feature:
            problems.append(f"sound {event} is not registered in E2SponsguhFeature")
    tests = java("feature", "einde2", "sponsguh", "E2SponsguhGameTests.java")
    wil = f"WEG_Y = {WEG_Y}, WEG_X0 = {WEG_X0}, WEG_X1 = {WEG_X1}, WEG_Z0 = {WEG_Z0}, WEG_Z1 = {WEG_Z1}"
    if wil not in tests:
        problems.append(f"E2SponsguhGameTests does not say '{wil}' (the test road changed?)")
    if problems:
        raise SystemExit("einde2_sponsguh_wezens self-check failed:\n  " + "\n  ".join(problems))
