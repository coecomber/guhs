import erfkit as K
S = K.Scene(6, 3, 4)
S.set(0,0,0,"minecraft:grass_block")
S.guh(2.5, 0, 1.5, 0)
S.guh(4.5, 0, 1.5, 30)
K.render(S, "_proef_guh", yaw=10, pitch=15, scale=90, ss=1)
