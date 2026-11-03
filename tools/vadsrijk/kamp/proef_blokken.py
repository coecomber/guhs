import sys, erfkit as K
B = sys.argv[1].split(",")
C = 8
S = K.Scene(C * 3, 3, ((len(B) + C - 1) // C) * 3)
lab = []
for i, b in enumerate(B):
    x, z = (i % C) * 3, (i // C) * 3
    if not K.has_block(b.split("[")[0]):
        print("MISSING", b); continue
    S.set(x, 0, z, b)
    lab.append((x + .5, 1.1, z + .5, str(i)))
K.render(S, sys.argv[2], yaw=float(sys.argv[3]) if len(sys.argv)>3 else 0, pitch=30, scale=40, ss=1, labels=lab)
