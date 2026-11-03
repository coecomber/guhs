"""Renders van Zwamzoet. python render_stad.py <shot> <ss>   shot = overzicht | skyline | deur | alle"""
import sys, time
import stadkit as SK, zwamzoet as Z

shot = sys.argv[1] if len(sys.argv) > 1 else "overzicht"
ss = int(sys.argv[2]) if len(sys.argv) > 2 else 1
t = time.time()
S, info = Z.nieuw()
print("gebouwd %.1fs, palet %d" % (time.time() - t, len(S.pal)), flush=True)
px, pz, pr = Z.PLEIN
G = Z.G
LAB = [(128, 172, 86, "Tempel van De Knuffelorde"), (px, G + 18, pz, "Plein + knuffelbeeld"),
       (px, G + 6, 198, "Markt"), (px, G + 26, 206, "Guhpoort"), (236, 76, 238, "Luchtschipmast"),
       (34, 54, 234, "Vuurtoren"), (120, G + 2, 238, "Haven"),
       (62, 96, 98, "Westhoed"), (194, 106, 92, "Oosthoed"), (50, 74, 160, "Bruine hoed"),
       (206, 82, 160, "Rode hoed")]
sfx = "" if ss > 1 else "_schets"
if shot in ("overzicht", "alle"):
    SK.render2(S, "stad_zwamzoet_overzicht" + sfx, yaw=32, pitch=32, scale=5.2, ss=ss, labels=LAB,
               titel="Zwamzoet", onderschrift="hoofdstad van zone 1 - Het Vadsrijk (Guhs 1.10.0) - ca. 256 x 256")
if shot in ("skyline", "alle"):
    SK.render2(S, "stad_zwamzoet_skyline" + sfx, yaw=8, pitch=4, scale=6.5, ss=ss, schemer=True, sterren=True,
               sky=((38, 26, 74), (246, 150, 168)), titel="Zwamzoet bij schemer",
               onderschrift="aankomst vanaf de haven - de lichtjes van de hoeden")
if shot in ("deur", "alle"):
    x1, yv, z1, dw, dh = info["deur"]
    ox, oy, oz = 92, 100, 52
    T = SK.deel(S, ox, oy, oz, 166, 200, 124)
    for (gx, gz, gy) in ((-2.5, 9, 180), (3.5, 11, 200), (0.5, 15, 170)):
        T.guh(128 - ox + gx, yv - oy, z1 + gz - oz, gy)
    SK.render2(T, "stad_tempeldeur" + sfx, yaw=18, pitch=12, scale=14, ss=ss,
               titel="De Knuffeldeur", onderschrift="tempel van De Knuffelorde op de hoogste hoed - deur 15 x 30")
print("totaal %.1fs" % (time.time() - t))
