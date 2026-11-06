"""
T-GELUID: the music pipeline of Het Vadsrijk (area themes, boss themes, stingers).

    compose (stukken.py, seeded)  ->  MIDI  (tools/vadsrijk/geluid/midi/<groep>/<naam>.mid, committed)
                                   ->  render with a General MIDI SoundFont (sf2.py, pure numpy)  ->  OGG Vorbis mono 32 kHz
                                       (src/main/resources/assets/guhs/sounds/vadsrijk/muziek/<groep>/<naam>.ogg)

The SoundFont is NOT in the repo (32 MB): GeneralUser GS by S. Christian Collins (free to use, also commercially; the rendered
audio is ours). Point GUHS_SF2 at it, default: ../_geluidkit/GeneralUser-GS.sf2 next to the worktrees.
Any other GM .sf2 works too (it will sound different).

Run from the project root:
    python tools/vadsrijk/geluid/maak_muziek.py                 everything
    python tools/vadsrijk/geluid/maak_muziek.py klodderweide    only pieces whose name contains a word
    python tools/vadsrijk/geluid/maak_muziek.py --alleen-midi   compose only (no SoundFont needed)
    python tools/vadsrijk/geluid/maak_muziek.py --van-midi x    render an existing / foreign MIDI (keeps the file, re-renders)
"""
import os
import sys
import time
from concurrent.futures import ProcessPoolExecutor

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)

import stukken  # noqa: E402
from componist import schrijf_midi  # noqa: E402

SR = 32000
MIDI = os.path.join(HIER, "midi")
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "vadsrijk", "muziek")


def sf2_pad():
    p = os.environ.get("GUHS_SF2")
    if p:
        return p
    return os.path.abspath(os.path.join(HIER, "..", "..", "..", "..", "_geluidkit", "GeneralUser-GS.sf2"))


_SF = None


def _sf():
    global _SF
    if _SF is None:
        from sf2 import SF2
        _SF = SF2(sf2_pad())
    return _SF


def render_een(item):
    groep, naam, mid, opties = item
    from render import bewaar_ogg, master, render_midi
    t = time.time()
    x = render_midi(mid, _sf(), SR, lus=opties.get("lus", False), galm_s=opties.get("galm", 2.4), helder=opties.get("helder", 5200))
    x = master(x, SR, rms_db=opties.get("rms", -24.0), uitval=opties.get("uitval", 1.5), lus=opties.get("lus", False))
    pad = os.path.join(OUT, groep, naam + ".ogg")
    kb = bewaar_ogg(pad, x, SR, opties.get("kwaliteit", 0.68)) // 1024
    import numpy as np
    return f"{groep}/{naam:24s} {len(x) / SR:6.1f}s  {kb:5d} kB  peak {np.max(np.abs(x)):.2f}  ({time.time() - t:.0f}s)"


def main(argv):
    filters = [a for a in argv if not a.startswith("--")]
    alleen_midi = "--alleen-midi" in argv
    werk = []
    for groep, naam, maak, opties in stukken.alle():
        if filters and not any(f in naam or f == groep for f in filters):
            continue
        mid = os.path.join(MIDI, groep, naam + ".mid")
        os.makedirs(os.path.dirname(mid), exist_ok=True)
        if "--van-midi" not in argv:
            st = maak()
            schrijf_midi(st, mid)
        werk.append((groep, naam, mid, opties))
    print(f"{len(werk)} pieces composed -> {MIDI}")
    if alleen_midi:
        return
    if not os.path.exists(sf2_pad()):
        raise SystemExit(f"SoundFont not found: {sf2_pad()} (set GUHS_SF2)")
    n = int(os.environ.get("GUHS_GELUID_PROC", "6"))
    with ProcessPoolExecutor(max_workers=n) as ex:
        for regel in ex.map(render_een, werk):
            print(regel, flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
