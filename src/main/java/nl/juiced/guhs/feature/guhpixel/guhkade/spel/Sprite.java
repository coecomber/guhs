package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

/**
 * The sprites of the Guhkade games on the sheet textures/guhkade/sprites.png (64 x 64, drawn by
 * tools/features/guhpixel_guhkade_tex.py, which checks that this table and its own are the same).
 */
public enum Sprite {
    /** Flappy Guh: the guh with its tiny wings up / down, and after it bumped into something. */
    GUH_OP(0, 0, 14, 12),
    GUH_NEER(14, 0, 14, 12),
    GUH_AF(28, 0, 14, 12),
    /** Mika-Pong: the ball is a rolling guh (four turns). */
    BAL_0(0, 12, 10, 10),
    BAL_1(10, 12, 10, 10),
    BAL_2(20, 12, 10, 10),
    BAL_3(30, 12, 10, 10),
    /** Mika-Pong: the Mika behind its paddle, and when the guh rolled past it. */
    MIKA(42, 0, 12, 12),
    MIKA_BOOS(42, 12, 12, 12),
    /** Flappy Guh: one slice of a kaasknabbel pillar (stacked) and the wider cap at the end of it. */
    PILAAR(0, 22, 22, 8),
    KAP(0, 30, 26, 6),
    WOLK(26, 24, 20, 9),
    HARTJE(46, 26, 7, 6),
    HARTJE_LEEG(53, 26, 7, 6),
    /** A paddle: a kaasknabbel stick (yours) and a dark one (the Mika's). */
    BATJE(56, 0, 4, 22),
    BATJE_MIKA(60, 0, 4, 22);

    /** The sheet's size, and where its one white pixel is (rectangles on the block's screen use it). */
    public static final int BLAD = 64, WIT_U = 63, WIT_V = 63;

    public final int u, v, b, h;

    Sprite(int u, int v, int b, int h) {
        this.u = u;
        this.v = v;
        this.b = b;
        this.h = h;
    }
}
