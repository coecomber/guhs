"""U-GROOTTE: packs the Vadsrijk map tiles (and all other guhs structures) into the compact packs the jar ships (data/guhs/vadsrijk/kaart.gpak).

The generators keep writing vanilla structure tiles (src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt):
that stays the working format every checker reads. The jar no longer ships those .nbt files (build.gradle excludes them); it
ships the pack, which `gradlew kaartPak` builds automatically before processResources. This script does the same without
gradle (javac + java on KaartPak.java), verifies every tile and prints the size per zone. maak_kaart.py calls it at the end.

    python tools/vadsrijk/kaartpak.py            # pack + verify + per-zone report
    python tools/vadsrijk/kaartpak.py --rapport  # only the report of an existing pack

Format and runtime: src/main/java/nl/juiced/guhs/feature/vadsrijk/grootte/KaartPak.java (+ KaartPakLader.java).
"""
import argparse
import collections
import os
import shutil
import struct
import subprocess
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
STRUCTUUR = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure")
UIT = os.path.join(ROOT, "build", "generated", "kaartpak")
PAK = os.path.join(UIT, "data", "guhs", "vadsrijk", "kaart.gpak")
STRUCTUREN = os.path.join(UIT, "data", "guhs", "structuren.gpak")
BRON = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "grootte", "KaartPak.java")
KLASSEN = os.path.join(HIER, "_cache", "kaartpak")
KLASSE = "nl.juiced.guhs.feature.vadsrijk.grootte.KaartPak"


def java_bin(naam):
    for basis in (os.environ.get("JAVA_HOME"), os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "java", "java-runtime-delta")):
        if basis:
            for ext in (".exe", ""):
                p = os.path.join(basis, "bin", naam + ext)
                if os.path.isfile(p):
                    return p
    p = shutil.which(naam)
    if not p:
        raise SystemExit(f"kaartpak: no {naam} found (set JAVA_HOME)")
    return p


def compileer():
    doel = os.path.join(KLASSEN, *KLASSE.split(".")) + ".class"
    if os.path.isfile(doel) and os.path.getmtime(doel) >= os.path.getmtime(BRON):
        return
    os.makedirs(KLASSEN, exist_ok=True)
    subprocess.run([java_bin("javac"), "-d", KLASSEN, BRON], check=True)


def draai(*args):
    subprocess.run([java_bin("java"), "-Xmx2g", "-cp", KLASSEN, KLASSE, *args], check=True)


def zone(tx, tz):
    """Rough zone of a world tile (for the report only)."""
    if -11 <= tx <= 11 and -11 <= tz <= 18:
        return "zone 1"
    if 6 <= tx <= 15 and -23 <= tz <= -13:
        return "zone 2"
    if 16 <= tx <= 26 and -16 <= tz <= -3:
        return "zone 3"
    if 27 <= tx <= 37 and -12 <= tz <= -5:
        return "zone 4"
    if 38 <= tx <= 48 and -12 <= tz <= -5:
        return "zone 5 (+ shared z4 tiles 38..39)"
    return "edges / open sea"


def lees_index(pad):
    with open(pad, "rb") as f:
        d = f.read()
    p = 0

    def i4():
        nonlocal p
        v = struct.unpack_from(">i", d, p)[0]
        p += 4
        return v
    assert i4() == 0x4750414B
    i4()
    for _ in range(i4()):
        n = i4()        # (not "p += i4()": that reads p before i4 moves it)
        p += n
    blobs = []
    for _ in range(i4()):
        n = i4()
        blobs.append(n)
        p += n
    tegels = {}
    for _ in range(i4()):
        n = struct.unpack_from(">H", d, p)[0]
        naam = d[p + 2:p + 2 + n].decode("utf-8")
        p += 2 + n
        b = i4()
        if naam.startswith("t_"):
            tx, tz = (int(v) for v in naam[2:].split("_"))
            tegels[(tx, tz)] = b
    return blobs, tegels


def rapport(pad=PAK):
    blobs, tegels = lees_index(pad)
    oud = collections.Counter()
    nieuw = collections.Counter()
    aantal = collections.Counter()
    gezien = set()
    for (tx, tz), b in tegels.items():
        z = zone(tx, tz)
        f = os.path.join(TEGELS, f"t_{tx}_{tz}.nbt")
        oud[z] += os.path.getsize(f) if os.path.isfile(f) else 0
        aantal[z] += 1
        if b not in gezien:              # a shared (deduplicated) tile counts once, at its first zone
            gezien.add(b)
            nieuw[z] += blobs[b]
    print(f"{'zone':36s} {'tiles':>6s} {'.nbt MB':>9s} {'pack MB':>9s}")
    for z in sorted(aantal):
        print(f"{z:36s} {aantal[z]:6d} {oud[z] / 1e6:9.2f} {nieuw[z] / 1e6:9.2f}")
    print(f"{'total':36s} {sum(aantal.values()):6d} {sum(oud.values()) / 1e6:9.2f} {os.path.getsize(pad) / 1e6:9.2f}"
          f"   ({len(blobs)} unique tiles)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--rapport", action="store_true", help="only the per-zone report of the existing pack")
    ap.add_argument("--geen-controle", action="store_true", help="skip the tile-by-tile verification")
    a = ap.parse_args()
    if not a.rapport:
        compileer()
        draai("beide", STRUCTUUR, UIT)          # the map tiles + all other guhs structures
        if not a.geen_controle:
            draai("controleerBeide", STRUCTUUR, UIT)
    rapport()
    if os.path.isfile(STRUCTUREN):
        print(f"other guhs structures: structuren.gpak {os.path.getsize(STRUCTUREN) / 1e6:.2f} MB")


if __name__ == "__main__":
    sys.exit(main())
