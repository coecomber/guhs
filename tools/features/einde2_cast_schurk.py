"""
einde2 (the Guheinde expansion), slice cast_schurk: the Mika's of the Guhvatar story as GuhNpcEntity kinds and its creatures as
entity types of their own (CONTRACT_E2 11.6). Phase A only gives them a body, a name and two lines; no story logic.
Java: nl.juiced.guhs.feature.einde2.castschurk (E2CastSchurkFeature; the tables CastSchurk and Wezens).

  einde2_cast_schurk_modellen   the tables, the writing of every model, texture and animation file, the check, the preview
  einde2_cast_schurk_mikas      the twelve Mika kinds (with the drawing kit einde2_cast_schurk_blad)
  einde2_cast_schurk_wezens     the nine creatures
  einde2_cast_schurk_tekst      every Dutch text
  this module                   names and small talk, the spawn eggs, the (empty) loot tables, the game test rooms, the self-check
"""
import os

from features import einde2_cast_schurk_modellen as modellen
from features import einde2_cast_schurk_tekst as tekst
from features import einde2_kit as kit

# the game test rooms (Java: E2CastSchurkGameTests): a plain floor, and a floor with a pond 2 deep (the fish)
TEST_VLOER, TEST_VIJVER = "einde2_cast_schurk_test_vloer", "einde2_cast_schurk_test_vijver"
VIJVER_X0, VIJVER_X1, VIJVER_Z0, VIJVER_Z1 = 2, 14, 2, 14       # the water's box (template x, z), at y 1 .. 2


def texts(h):
    for key, text in tekst.texts().items():
        kit.lang(h, key, text)


def wezens(h):
    """The spawn eggs and the (empty) loot tables of the creatures: nobody of them ever drops anything."""
    for id, (naam, basis, vlek) in tekst.WEZENS.items():
        kit.spawn_ei(h, id, f"{naam}-spawnei", basis, vlek)
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})


def test_kamers(h):
    s = h.Structure((15, 8, 15))
    for x in range(15):
        for z in range(15):
            s.set(x, 0, z, ("minecraft:red_concrete", "minecraft:black_concrete")[(x + z) % 2])
    s.save(TEST_VLOER)
    s = h.Structure((17, 8, 17))
    for x in range(17):
        for z in range(17):
            s.set(x, 0, z, "minecraft:stone")
            water = VIJVER_X0 <= x <= VIJVER_X1 and VIJVER_Z0 <= z <= VIJVER_Z1
            for y in (1, 2):
                s.set(x, y, z, "minecraft:water" if water else "minecraft:stone")
    s.save(TEST_VIJVER)


def build(h):
    modellen.build(h)
    texts(h)
    wezens(h)
    test_kamers(h)
    selfcheck(h)


def selfcheck(h):
    problems = modellen.check(h)
    for kind in modellen.CAST:
        for key in (f"entity.guhs.guh_npc.{kind}", f"{tekst.P}{kind}.0", f"{tekst.P}{kind}.1"):
            if key not in h.NL:
                problems.append(f"no text {key}")
    if set(tekst.KINDS) != set(modellen.CAST):
        problems.append("the names (tekst.KINDS) and the table (modellen.CAST) list other kinds")
    if set(tekst.WEZENS) != set(modellen.WEZENS):
        problems.append("the names (tekst.WEZENS) and the table (modellen.WEZENS) list other creatures")
    for id in modellen.WEZENS:
        for pad in (f"{h.A}/models/item/{id}_spawn_egg.json", f"{h.D}/loot_table/entities/{id}.json"):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
        for key in (f"entity.guhs.{id}", f"item.guhs.{id}_spawn_egg", f"{tekst.P}{id}.0", f"{tekst.P}{id}.1"):
            if key not in h.NL:
                problems.append(f"no text {key}")
    for naam in (TEST_VLOER, TEST_VIJVER):
        if not os.path.exists(f"{h.D}/structure/{naam}.nbt"):
            problems.append(f"missing the test room {naam}")
    tests = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "castschurk", "E2CastSchurkGameTests.java")
    wil = f"VIJVER_X0 = {VIJVER_X0}, VIJVER_X1 = {VIJVER_X1}, VIJVER_Z0 = {VIJVER_Z0}, VIJVER_Z1 = {VIJVER_Z1}"
    if not os.path.exists(tests) or wil not in open(tests, encoding="utf-8").read():
        problems.append(f"E2CastSchurkGameTests does not say '{wil}' (the pond changed?)")
    if problems:
        raise SystemExit("einde2_cast_schurk self-check failed:\n  " + "\n  ".join(problems))
