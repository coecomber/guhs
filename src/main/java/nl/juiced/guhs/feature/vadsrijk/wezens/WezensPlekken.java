package nl.juiced.guhs.feature.vadsrijk.wezens;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Every coordinate the wezens slice uses (SLICE_BRIEF: one class). Integration 1.10.0: the caves and the temple are aligned with
 * the real map (WereldPlekken grot_knopen / kristalkamer / zwamtempel_voorhal, POI v1); before: POI_VADSRIJK.md v0, section 10
 * "Mob spawn zones" (+ the arenas, ponds, slime hill and temple rooms it lists). Integration aligns them with v1.
 * <p>Surface spots go to kern's heightmap spawners (only x/z matter). Cave and temple spots use the slice's floor-search
 * spawners ({@link WezensSpawners}): there the y must be INSIDE the cave / temple room (the floor is searched from y down to
 * y-24, then up to y+6). The cave y values are still a guess (POI v0: "caves under y 75 around the 3 entrances").
 */
public final class WezensPlekken {
    private WezensPlekken() {
    }

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, Vadsrijk.GROND + 1, z);
    }

    private static BlockPos p(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    /** A spot of the real map (WereldPlekken), moved a little; for cave spawners the y must be the stand y inside the cave. */
    private static BlockPos w(String id, int dx, int dz) {
        return WereldPlekken.plek(id).offset(dx, 0, dz);
    }

    // ---- Klodderweide (POI: (180,-280)..(580,380) minus the Koning Klodder arena (410,90) r40 and the slime hill (300,300) r50),
    //      lv 1-6. Bubbelklodders at the pond rims: ponds (330,-70) r30, (470,160) r25, (250,260) r25, (520,40) r20.
    public static final BlockPos[] KLODDER = {p(250, -130), p(420, -210), p(330, 160), p(490, 30), p(390, 290), p(545, -100), p(215, 190)};
    public static final BlockPos[] BUBBELKLODDER = {p(330, -28), p(470, 196), p(212, 262), p(520, 70)};
    public static final BlockPos[] STUITERKLODDER = {p(455, -130), p(540, 320), p(350, 200)};
    public static final BlockPos[] MOEDERKLODDER = {p(230, 40), p(560, -240)};

    // ---- Sporenwoud (POI: (-580,-140)..(-180,380)), lv 6-13 ---------------------------------------------------------------------
    public static final BlockPos[] HOEDJESBOL = {p(-240, -60), p(-300, 180), p(-420, 40), p(-520, 260), p(-210, 330)};
    public static final BlockPos[] SPORENPUFFER = {p(-360, -80), p(-480, 160), p(-260, 320), p(-540, 20)};
    public static final BlockPos[] ZWAMWACHTER = {p(-400, 280), p(-540, -60), p(-320, 60)};
    public static final BlockPos[] MOSGEEST = {p(-460, -110), p(-250, 100), p(-560, 340), p(-380, 200)};

    // ---- Druipgrotten (POI: surface (-580,-580)..(-180,-180); caves under y 75 around the entrances (-300,-220), (-420,-300),
    //      (-180,-360); the hidden crystal chamber (-490,52,-520)), lv 12-18 --------------------------------------------------------
    /** A guess for "inside the caves" (floor search goes 24 down from here). */
    public static final int GROT_Y = 62;
    public static final BlockPos[] DRUIPSLIJM = {w("grot_knopen.N1", 0, 0), w("grot_knopen.N2", 0, 0), w("grot_knopen.N3", 0, 0),
            w("grot_knopen.N4", 0, 0)};
    public static final BlockPos[] KRISTALKLODDER_GROT = {w("kristalkamer", 3, 3), w("grot_knopen.N4", 4, 4), w("grot_knopen.N2", -4, 4)};
    public static final BlockPos[] KRISTALKLODDER = {p(-520, -440), p(-300, -480)};
    public static final BlockPos[] KNAAG = {p(-240, -400), p(-380, -250), p(-480, -420), p(-320, -560), p(-540, -230)};
    public static final BlockPos[] KNAAG_GROT = {w("grot_knopen.N1", 4, -4), w("grot_knopen.N3", -4, 4)};
    public static final BlockPos[] GLIMZWAM = {p(-400, -400), p(-260, -300), p(-500, -300)};
    public static final BlockPos[] GLIMZWAM_GROT = {w("kristalkamer", -3, -3)};

    // ---- Zwamtempel (POI: interior x 280..380, z -500..-395, floor stand y 152; NOT the Opperzwam hall (330,152,-470) r22;
    //      the mountain path (180,-580)..(580,-310)), lv 18-20 ---------------------------------------------------------------------
    public static final int TEMPEL_Y = 152;
    /** The voorhal (x 314..346, z -404..-394); the puzzle rooms and the Ridderzaal are bazen's (its waves). */
    public static final BlockPos[] TEMPELRIDDER = {w("zwamtempel_voorhal", -9, 0), w("zwamtempel_voorhal", 9, 0)};
    public static final BlockPos[] SPORENPRIESTER = {w("zwamtempel_voorhal", 0, -3)};
    /** Outside, on the mountain path (surface). */
    public static final BlockPos[] TEMPELRIDDER_PAD = {p(250, -350), p(450, -340), p(380, -560)};
    public static final BlockPos[] SPORENPRIESTER_PAD = {p(300, -330)};

    // ---- Knuffelkamp: the Kampklodder's home at the rim of the camp pond (the Klodderreus arena ~(54,87,1069) r18) ------------
    public static final BlockPos KAMPKLODDER = new BlockPos(70, 87, 1052);
}
