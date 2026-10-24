"""
1.7.0 "Onder de Wol" structures: freezing the design renders (MineScapeStory/mijnen-ontwerp/_bron/rNN_*.py) into voxel files.

The renders of the design rounds (ontwerp-guhs170-mijnen.md, rounds 13-20) hold the exact block layouts the user approved.
This script runs a render script WITHOUT rendering (mijnkit.render / legenda are replaced by no-ops) and saves its scene:

    tools/features/onderwol_bouw_bron/<naam>.npz
        v       uint16 [X, Y, Z]  the palette index of every voxel (0 = air)
        gezet   bool   [X, Y, Z]  set by a Scene.set/fill/... call (False: the scene's untouched start, e.g. the air of an
                                  open-box scene or the numpy-filled rock of a carved one)
        pal     json list         the block strings ("guhs:c_viltsteen", "minecraft:oak_stairs[facing=east]", ...)
        guhs    json list         the guh figures [x, y, z, yaw, skin, sitting]
        meta    json dict         the script's own constants that the structure modules need (DY, VL, ...)

The structure modules (onderwol_structuren1.py) only read these files, so the build never needs the render kit or its
jars. Run it again only when a render script changes (from the project root):

    python tools/features/onderwol_bouw_scene.py [r2_mijnguhdorp ...]
"""
import json
import os
import runpy
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
BRON = os.path.join(HIER, "onderwol_bouw_bron")
RENDERS = os.path.abspath(os.path.join(HIER, "..", "..", "..", "..", "mijnen-ontwerp", "_bron"))
SCRIPTS = ["r2_mijnguhdorp", "r3_knuffelslaapzaal", "r4_paddenstoelendorpje", "r05_verlaten_guhmijn", "r06_grotschilderingen",
           "r07_sausmeer_vissershutje", "r08_mijningang_hutje"]
# the module constants worth keeping per script (when the script has them)
META = ["X", "Y", "Z", "DY", "Y0", "VL", "TOP", "ZS", "CUT", "MX", "MZ", "MRX", "MRZ", "WX", "WZ", "LX", "LZ", "CX", "CZ", "RX", "RZ", "RY",
        "SP", "H0X", "H1X", "H0Z", "H1Z", "HV", "SX", "SZ", "VA", "VB", "BX", "BZ", "WN", "WW", "PB", "PT", "HB", "HT", "MID", "ZW",
        "KAMERS", "TERRAS", "ZIJ", "HUIZEN", "TRAP", "inst_z"]


def laad(naam):
    """The frozen scene: (v, gezet, pal, guhs, meta)."""
    d = np.load(os.path.join(BRON, naam + ".npz"), allow_pickle=False)
    return d["v"], d["gezet"], json.loads(str(d["pal"])), json.loads(str(d["guhs"])), json.loads(str(d["meta"]))


def bevries(naam):
    sys.path.insert(0, RENDERS)
    import mijnkit as M
    M.render = lambda *a, **k: os.devnull
    M.legenda = lambda *a, **k: None
    Scene = M.V.Scene
    oud = Scene.set
    scenes = []

    def set_(self, x, y, z, s, roof=False, keep=False):
        if not hasattr(self, "_gezet"):
            self._gezet = np.zeros(self.v.shape, bool)
            scenes.append(self)
        x, y, z = int(x), int(y), int(z)
        if self.inb(x, y, z) and not (keep and self.v[x, y, z] != 0):
            self._gezet[x, y, z] = True
        oud(self, x, y, z, s, roof, keep)
    Scene.set = set_
    cwd, argv = os.getcwd(), sys.argv
    os.chdir(RENDERS)
    sys.argv = [naam]
    try:
        g = runpy.run_path(os.path.join(RENDERS, naam + ".py"), run_name="scene")
    finally:
        os.chdir(cwd)
        sys.argv = argv
        Scene.set = oud
    S = g["S"]
    gezet = getattr(S, "_gezet", np.zeros(S.v.shape, bool))
    meta = {}
    for k in META:
        if k in g:
            try:
                json.dumps(g[k], default=lambda o: o.tolist() if hasattr(o, "tolist") else int(o))
                meta[k] = json.loads(json.dumps(g[k], default=lambda o: o.tolist() if hasattr(o, "tolist") else int(o)))
            except (TypeError, ValueError):
                pass
    os.makedirs(BRON, exist_ok=True)
    np.savez_compressed(os.path.join(BRON, naam + ".npz"), v=S.v.astype(np.uint16), gezet=gezet, pal=json.dumps(S.pal),
                        guhs=json.dumps([[float(a) if isinstance(a, (int, float, np.floating, np.integer)) and not isinstance(a, bool) else a
                                          for a in t] for t in S.guhs]),
                        meta=json.dumps(meta))
    print(f"{naam}: {S.v.shape}, {len(S.pal)} blocks in the palette, {len(S.guhs)} guhs, {int(gezet.sum())} set voxels")


if __name__ == "__main__":
    for n in sys.argv[1:] or SCRIPTS:
        bevries(n)
