"""
Op Safari! (1.6.0) - the park generator: Safaripark Vadse Bergen as 6 x 6 structure tiles of 128 x 128 x 160.

  kit.py      the block volume (numpy), palette with block states, helpers (fill, sphere, trees), fence connections,
              the fast .nbt writer per tile and the previews
  terrein.py  plattegrond C: zones, heights, river loop, surfaces, fences + gates, route, entreedorp plaza, vegetation,
              decor ring
  plekken.py  data/guhs/safari/plekken.json: the homes of every species in the park
  zone_<naam>.py (later, the zone slices): bouw(park) adds the landmarks at the reserved spots (kit.RESERVERINGEN)

Run from the project root:  python tools/features/safari_park.py [--preview] [--snel]
"""
