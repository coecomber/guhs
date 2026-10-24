"""
1.7.0 "Onder de Wol": runs every onderwol module in the right order (from the project root):
    PYTHONPATH=tools python tools/features/onderwol_alles.py
onderwol.py rewrites the cave biomes, so the modules that add to them come after it.
"""
import runpy
import sys

for module in ("onderwol", "onderwol_grot", "onderwol_oppervlak", "onderwol_water"):
    sys.argv = [module]
    runpy.run_path(f"tools/features/{module}.py", run_name="__main__")
