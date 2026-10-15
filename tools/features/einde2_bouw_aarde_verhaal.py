"""
einde2 phase B, slice h3_aarde: what the STORY of chapter 3 adds to the build of Ba Sing Guh (CONTRACT_E2_B 4.1, 7.4).
Called from einde2_bouw_aarde.stad(); everything stays inside the build's box and goes through the build's own self-check.

  heuvel(b)    the hill with the tree on the landing lobe, outside the outer wall (Miroh's song for Lu Ten): a soft mound of
               moss with a cherry tree on top, two stone lanterns; markers lu_ten_heuvel (zone), lu_ten_boom (prop),
               lu_ten_miroh (npc: where Miroh sits, OOM_MIROH), lu_ten_speler (prop: where you stand)
  plekken(b)   the camera of every story scene of the chapter (b.camera: the self-check wants a free line from every eye to
               what it looks at, and to the actors it names), plus the few spots the scenes need that the build had not:
               the Long Feng, Appa and Dai Li spots in the base, the spots of the fall

The Java side (feature/einde2/h3aarde/H3Scenes) reads the cameras from the sidecar: the pad <scene>_camera = [eye, ..., look].
"""
import math

from features import einde2_bouw_aarde_bouw as bouw

ONDER = bouw.ONDER
HEUVEL = (12, 108)          # the middle of the hill on the landing lobe (template x, z)
HEUVEL_R = 6.6
CHERRY = "minecraft:cherry_leaves"
BLAD = {"distance": "1", "persistent": "true", "waterlogged": "false"}

# every marker this module adds (einde2_bouw_aarde.VERWACHT asks them all)
MARKERS = ["lu_ten_heuvel", "lu_ten_boom", "lu_ten_miroh", "lu_ten_speler", "lu_ten_camera", "lu_ten_dicht_camera",
           "h3_aankomst_camera", "h3_loket_camera", "h3_aardrommel_camera", "h3_aardrommel_toph_camera", "h3_thee_buiten_camera",
           "h3_thee_binnen_camera", "h3_thee_zuka_camera", "h3_appa_kamp_camera", "h3_appa_stad_camera", "h3_appa_leeg_camera",
           "h3_laogai_camera", "h3_laogai_long_feng_camera", "h3_weerzien_camera", "h3_val_wijd_camera", "h3_val_zuka_camera",
           "h3_val_bliksem_camera", "h3_val_katara_camera", "h3_val_schacht_camera", "h3_koning_camera",
           "laogai_long_feng", "laogai_team", "val_katara_cel", "val_zuka_cel", "val_appa_boven"]


def hoogte(x, z):
    """Blocks the hill rises above the lobe at this column (0: not on the hill)."""
    d = math.hypot(x - HEUVEL[0], z - HEUVEL[1])
    if d >= HEUVEL_R:
        return 0
    return 4 if d < 1.6 else 3 if d < 3.0 else 2 if d < 4.3 else 1 if d < 5.6 else 0


def heuvel(b):
    """The hill: a mound with a moss top (one step per block, so you walk up), a cherry tree at the top, two stone lanterns
    at its foot. Built right after the island, so nothing of the city lands on it (its columns are taken)."""
    cx, cz = HEUVEL
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 7, cz + 8):
            k = b.kolom.get((x, z))
            hh = hoogte(x, z)
            if k is None or hh == 0:
                continue
            top, bodem, d = k
            for y in range(top, top + hh):
                b.set(x, y, z, bouw.KLIP)
            b.set(x, top + hh, z, "minecraft:moss_block")
            b.kolom[(x, z)] = (top + hh, bodem, d)
    b.neem(cx - 7, cz - 7, cx + 7, cz + 7)
    # the tree: a cherry with a crooked crown, its trunk on the top
    y0 = ONDER + hoogte(cx, cz)
    for i in range(1, 6):
        b.set(cx, y0 + i, cz, "minecraft:cherry_log", {"axis": "y"})
    b.set(cx - 1, y0 + 5, cz, "minecraft:cherry_log", {"axis": "x"})
    b.set(cx + 1, y0 + 4, cz + 1, "minecraft:cherry_wood", {"axis": "y"})
    for dx in range(-3, 4):
        for dy in range(-1, 3):
            for dz in range(-3, 4):
                r = dx * dx + dy * dy * 2.2 + dz * dz
                if r <= 10.5 and not (dy == -1 and r > 5) and b.naam(cx + dx, y0 + 5 + dy, cz + dz) is None:
                    b.set(cx + dx, y0 + 5 + dy, cz + dz, CHERRY, BLAD)
    # pink petals at the foot of the trunk (thin: you walk through them)
    for dx, dz in ((1, -1), (-1, 1), (2, 1), (-2, -1), (1, 2)):
        x, z = cx + dx, cz + dz
        if b.naam(x, b.kolom[(x, z)][0] + 1, z) is None:
            b.set(x, b.kolom[(x, z)][0] + 1, z, "minecraft:pink_petals", {"facing": "north", "flower_amount": "3"})
    # two stone lanterns (a toro: post, light, little roof) at the foot of the hill
    for x, z in ((7, 113), (17, 103)):
        y = b.kolom[(x, z)][0]
        b.set(x, y + 1, z, bouw.KLIPM)
        b.set(x, y + 2, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        b.set(x, y + 3, z, "minecraft:stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    b.zone("lu_ten_heuvel", cx - 7, ONDER + 1, cz - 7, cx + 7, ONDER + 12, cz + 7, wie="de heuvel met de boom op de landingslob (Miroh's lied voor Lu Ten)")
    b.prop("lu_ten_boom", cx, y0 + 1, cz, wie="de voet van de kersenboom op de heuvel: hier legt Miroh zijn knabbel neer")
    b.plek("lu_ten_miroh", cx + 1, ONDER + hoogte(cx + 1, cz + 1) + 1, cz + 1, yaw=90.0, wie="Oom Miroh, onder de boom, met zijn rug naar de stad",
           kind="OOM_MIROH")
    b.prop("lu_ten_speler", cx + 3, ONDER + hoogte(cx + 3, cz + 2) + 1, cz + 2, wie="hier sta je als je Miroh hoort zingen")


def plekken(b):
    """The cameras of the scenes and the spots the scenes need (after the whole city stands: the check sees every block)."""
    p = b.plekken
    pos = lambda id: tuple(p[id]["pos"])  # noqa: E731
    cx, cz = HEUVEL
    top = ONDER + hoogte(cx, cz)
    # --- Lu Ten: a close shot of Miroh, then the slow pull-back to the north-west over the void (no dialogue) ---------------
    mx, my, mz = pos("lu_ten_miroh")
    b.camera("lu_ten_dicht", (mx - 3, my + 1, mz + 2), (mx, my + 1, mz), zicht=["lu_ten_miroh"], wie="dichtbij Miroh onder de boom")
    b.camera("lu_ten", [(cx - 3, top + 3, cz + 3), (cx - 6, top + 5, cz - 3), (cx - 9, top + 8, cz - 12), (cx - 11, top + 12, cz - 22)],
             (cx, top + 2, cz), zicht=["lu_ten_miroh", "lu_ten_speler"], wie="de camera trekt langzaam terug, over de leegte")
    # --- arriving: from the view over the lobe to the open gate ---------------------------------------------------------------
    b.camera("h3_aankomst", [(2, 54, 128), (6, 50, 128)], (22, 49, 128), zicht=["aankomst_speler"], wie="het eerste shot op de buitenpoort")
    # --- the passport desk ---------------------------------------------------------------------------------------------------
    lx, ly, lz = pos("loketguh")
    b.camera("h3_loket", (lx - 2, ly + 2, lz + 5), (lx, ly + 1, lz + 1), wie="schuin op het raampje van de loketguh")
    # --- the Aardrommel: the whole ring, then low on Toph -------------------------------------------------------------------
    tx, ty, tz = pos("toph")
    b.camera("h3_aardrommel", (tx - 9, ty + 6, tz - 6), (tx, ty + 1, tz + 3), zicht=["toph", "kei"], wie="hoog over de ring")
    b.camera("h3_aardrommel_toph", (tx - 3, ty + 1, tz - 3), (tx, ty + 1, tz), zicht=["toph"], wie="laag op De Blinde Bandiet")
    # --- De Jasmijn Draak: the dragon over the door, Miroh behind the counter, Zuka with his cup ------------------------------
    mx, my, mz = pos("miroh")
    b.camera("h3_thee_buiten", (80, 53, 129), (80, 53, 137), wie="voor de deur, onder de drakenkop")
    b.camera("h3_thee_binnen", (84, 51, 139), (mx, my + 1, mz), zicht=["miroh"], wie="binnen, schuin op de toonbank")
    zx, zy, zz = pos("zuka_ober")
    b.camera("h3_thee_zuka", (zx + 3, zy + 1, zz - 2), (zx, zy + 1, zz), zicht=["zuka_ober"], wie="op Zuka met zijn kopje")
    # --- Appa's lost days: the camp, the city by night, the empty camp ------------------------------------------------------
    ax, ay, az = pos("appa_kamp")
    b.camera("h3_appa_kamp", (ax - 7, ay + 4, az - 5), (ax, ay + 1, az), zicht=["appa_kamp"], wie="op Appa bij het kamp")
    b.camera("h3_appa_stad", (6, 76, 104), (40, 66, 104), wie="de stad bij nacht, over de buitenmuur")
    b.camera("h3_appa_leeg", (ax - 4, ay + 2, az - 3), (ax, ay, az), wie="het lege kamp")
    # --- Meer Laogai: Long Feng in the cross corridor, the bars of Appa's chamber -------------------------------------------
    px, py, pz = pos("appa_poort")
    b.plek("laogai_long_feng", px - 4, py, pz - 3, yaw=-90.0, wie="Long Feng-Mika stapt hier uit de schaduw (de gang naar de lampkamer)",
           kind="LONG_FENG_MIKA")
    b.plek("laogai_team", px, py, pz - 6, yaw=0.0, wie="hier komt het team aanrennen (de gang met de cellen)")
    b.camera("h3_laogai", (px + 4, py + 2, pz - 4), (px, py + 1, pz - 1), zicht=["laogai_long_feng"], wie="in de dwarsgang, op de tralies")
    b.camera("h3_laogai_long_feng", (px - 1, py + 1, pz - 2), (px - 4, py + 1, pz - 3), zicht=["laogai_long_feng"], wie="dichtbij Long Feng")
    gx, gy, gz = pos("appa_gevangen")
    b.camera("h3_weerzien", (gx - 4, gy + 3, gz - 3), (px, py + 1, pz + 1), zicht=["appa_gevangen"], wie="in Appa's kamer, op de kapotte tralies")
    # --- the palace: the king, Long Feng, Bosco ----------------------------------------------------------------------------
    kx, ky, kz = pos("aardekoning")
    b.camera("h3_koning", (kx - 6, ky + 2, kz + 1), (kx, ky + 1, kz), zicht=["aardekoning"], wie="voor de troon")
    # --- the fall of Ba Sing Guh: the crystal chamber ------------------------------------------------------------------------
    sx, sy, sz = pos("bliksem_speler")
    kx, ky, kz = pos("kristalcel")
    b.prop("val_katara_cel", kx, ky, kz, wie="Katara-guh in de kristalcel", los=True)
    b.prop("val_zuka_cel", kx + 1, ky, kz, wie="Zuka in de kristalcel", los=True)
    b.prop("val_appa_boven", 161, 40, 127, wie="Appa-guh in de Koningsput, op weg naar beneden", los=True)
    b.camera("h3_val_wijd", (sx + 10, sy + 9, sz + 12), (sx - 2, sy + 1, sz), zicht=["azula", "zuka_keuze", "bliksem_speler"], wie="over de hele kristalkamer")
    zx, zy, zz = pos("zuka_keuze")
    b.camera("h3_val_zuka", (zx + 4, zy + 2, zz + 3), (zx, zy + 1, zz), zicht=["zuka_keuze"], wie="op Zuka's keuze")
    b.camera("h3_val_bliksem", (sx + 5, sy + 3, sz - 6), (sx, sy + 2, sz), zicht=["bliksem_speler", "bliksem_azula"], wie="de bliksem")
    kx, ky, kz = pos("katara_redt")
    b.camera("h3_val_katara", (kx + 3, ky + 2, kz + 3), (kx, ky + 1, kz), zicht=["katara_redt"], wie="Katara-guh en het geestenwater")
    b.camera("h3_val_schacht", (155, 20, 121), (161, 34, 127), wie="omhoog de Koningsput in")
