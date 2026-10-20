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


def build(h):
    """make_resources hook: FEATURES names "safari_park", and the import features.safari_park finds THIS package (it shadows
    tools/features/safari_park.py), so the hook lives here and runs the generator script (from the project root)."""
    import importlib.util
    import os
    pad = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "safari_park.py")
    spec = importlib.util.spec_from_file_location("safari_park_generator", pad)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.maak(".")
