"""
biomes3: the small helper library of the kern (not in FEATURES; every bio_<slice> module may import it, none edits it).

  rng(naam)                  a random generator that depends on a NAME only (never on a module's place in FEATURES: another
                             update's modules will be merged in before ours, which moves every index)
  blok(h, id, standin)       "guhs:<id>" once that block's blockstate file exists, the stand-in until then: how worldgen
                             and templates name a block of another slice that is not merged yet
  bestaat(h, id)             does that block exist yet
  teksten(h, tabel)          Dutch texts {key: text} (English comes later, in the slice's chunk file)
  biome_plaatshouder(h, id)  a minimal valid biome file, so the id exists before the wereld slice writes the real one
"""
import os
import zlib

import numpy as np


def rng(naam):
    return np.random.default_rng(zlib.crc32(naam.encode("utf-8")))


def bestaat(h, bid):
    return os.path.exists(os.path.join(h.A, "blockstates", f"{bid}.json"))


def blok(h, bid, standin):
    return f"guhs:{bid}" if bestaat(h, bid) else standin


def teksten(h, tabel):
    for key, nl in tabel.items():
        if "te vads" in nl.lower() or "hamster" in nl.lower():
            raise SystemExit(f"biomes3 text check failed: {key}: {nl}")
        h.lang(key, nl, nl)


def biome_plaatshouder(h, bid):
    # (the old shape, like every other biome: mc26.py converts its effects)
    h.w(f"{h.D}/worldgen/biome/{bid}.json", {
        "has_precipitation": False, "temperature": 0.7, "downfall": 0.4,
        "effects": {"sky_color": 0xF7B6D8, "fog_color": 0xFFE4F1, "water_color": 0x5FD3D6, "water_fog_color": 0x2F9FA8},
        "spawners": {}, "spawn_costs": {}, "carvers": {"air": []}, "features": []})
