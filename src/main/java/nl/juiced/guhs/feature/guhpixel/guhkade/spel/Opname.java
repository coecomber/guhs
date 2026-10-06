package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

import java.util.Arrays;

/** The input of every step of one game, packed: 1 bit a step for Flappy Guh, 2 bits a step for Mika-Pong. */
public final class Opname {
    private byte[] data = new byte[256];
    private final int bits;
    private int stappen;

    public Opname(int bits) {
        this.bits = bits;
    }

    public void schrijf(int invoer) {
        int bit = stappen * bits;
        if ((bit + bits + 7) / 8 > data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
        for (int i = 0; i < bits; i++) {
            if ((invoer >> i & 1) != 0) {
                data[(bit + i) >> 3] |= (byte) (1 << ((bit + i) & 7));
            }
        }
        stappen++;
    }

    public int stappen() {
        return stappen;
    }

    public byte[] bytes() {
        return Arrays.copyOf(data, (stappen * bits + 7) / 8);
    }

    /** How many whole steps fit in these bytes. */
    public static int stappen(byte[] opname, int bits) {
        return opname.length * 8 / bits;
    }

    public static int lees(byte[] opname, int bits, int stap) {
        int bit = stap * bits, uit = 0;
        for (int i = 0; i < bits; i++) {
            int b = bit + i;
            if ((b >> 3) < opname.length && (opname[b >> 3] >> (b & 7) & 1) != 0) {
                uit |= 1 << i;
            }
        }
        return uit;
    }
}
