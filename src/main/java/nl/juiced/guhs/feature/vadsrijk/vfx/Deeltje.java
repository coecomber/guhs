package nl.juiced.guhs.feature.vadsrijk.vfx;

/**
 * The VFX kit's own particle kinds (roadmap 3.2): sprites in {@code textures/vadsrijk/vfx/deeltjes.png} (8 x 4 cells of 32 px,
 * white, tinted per particle; made by tools/features/vadsrijk_t_vfx.py). {@code cel} = the first cell, {@code beelden} = animation
 * frames (cells after it), {@code zwaarte} = gravity, {@code rem} = drag per tick, {@code leven} = life in ticks.
 */
public enum Deeltje {
    STER(0, 4, -0.002, 0.90, 18),
    PLUIS(4, 4, 0.006, 0.93, 34),
    VONK(8, 2, 0.03, 0.92, 14),
    SLIJM(10, 2, 0.05, 0.95, 22),
    SPOOR(12, 2, -0.004, 0.94, 40),
    KRISTAL(14, 2, 0.045, 0.96, 26),
    HONING(16, 2, 0.04, 0.97, 30),
    CONFETTI(18, 2, 0.02, 0.92, 40),
    HART(20, 2, -0.01, 0.93, 30),
    ROOK(22, 2, -0.006, 0.92, 32),
    VEER(24, 2, 0.008, 0.9, 40),
    BLAD(26, 2, 0.012, 0.9, 40),
    GLOED(28, 1, 0.0, 0.85, 10);

    public final int cel, beelden, leven;
    public final double zwaarte, rem;

    Deeltje(int cel, int beelden, double zwaarte, double rem, int leven) {
        this.cel = cel;
        this.beelden = beelden;
        this.zwaarte = zwaarte;
        this.rem = rem;
        this.leven = leven;
    }

    public static Deeltje van(String naam) {
        try {
            return valueOf(naam.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return STER;
        }
    }
}
