package nl.juiced.guhs.feature.vadsrijk.wezens;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Every coordinate the wezens slice uses (SLICE_BRIEF: one class). POI_VADSRIJK.md did not exist yet when this was written, so
 * the spots are chosen inside the contract's area boxes after the zone map (renders-vadsrijk/_bron/stad/zonekaart.py: map
 * x/z 0..1200 = world -600..600). Integration aligns them with the real map.
 * <p>Surface areas (Klodderweide, Sporenwoud) use kern's heightmap spawners, so only x/z matter there. The Druipgrotten
 * and the Zwamtempel use the slice's floor-search spawners ({@link WezensSpawners}): there the y must be INSIDE the cave /
 * temple hall (the floor is searched from y+6 down to y-24), still a guess until the wereld slice publishes its POIs.
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

    // ---- Klodderweide (160..600, -300..400), lv 1-6 ----------------------------------------------------------------------
    public static final BlockPos[] KLODDER = {p(250, -130), p(420, -210), p(300, 110), p(480, 50), p(380, 300), p(545, -70), p(215, 200)};
    public static final BlockPos[] BUBBELKLODDER = {p(330, -40), p(500, 200), p(255, 290), p(440, -120)};
    public static final BlockPos[] STUITERKLODDER = {p(455, -130), p(540, 320), p(350, 210)};
    public static final BlockPos[] MOEDERKLODDER = {p(400, 140), p(560, -240)};

    // ---- Sporenwoud (-600..-160, -160..400), lv 6-13 -----------------------------------------------------------------------
    public static final BlockPos[] HOEDJESBOL = {p(-240, -60), p(-300, 180), p(-420, 40), p(-520, 260), p(-210, 330)};
    public static final BlockPos[] SPORENPUFFER = {p(-360, -80), p(-480, 160), p(-260, 320), p(-540, 20)};
    public static final BlockPos[] ZWAMWACHTER = {p(-400, 280), p(-540, -60), p(-320, 60)};
    public static final BlockPos[] MOSGEEST = {p(-460, -110), p(-250, 100), p(-560, 340), p(-380, 200)};

    // ---- Druipgrotten (-600..-160, -600..-160), lv 12-18: floor search, y inside the caves -------------------------------
    public static final int GROT_Y = Vadsrijk.GROND - 14;
    public static final BlockPos[] DRUIPSLIJM = {p(-280, GROT_Y, -260), p(-420, GROT_Y, -340), p(-520, GROT_Y, -480), p(-300, GROT_Y, -500)};
    public static final BlockPos[] KRISTALKLODDER = {p(-460, GROT_Y, -240), p(-340, GROT_Y, -440), p(-540, GROT_Y, -560)};
    public static final BlockPos[] KNAAG = {p(-240, GROT_Y, -400), p(-380, GROT_Y, -260), p(-480, GROT_Y, -420), p(-320, GROT_Y, -560)};
    public static final BlockPos[] GLIMZWAM = {p(-400, GROT_Y, -400), p(-260, GROT_Y, -300), p(-500, GROT_Y, -300)};

    // ---- Zwamtempel (160..600, -600..-300), lv 18-20: floor search, y = the temple halls on the mountain --------------------
    public static final int TEMPEL_Y = Vadsrijk.GROND + 36;
    public static final BlockPos[] TEMPELRIDDER = {p(330, TEMPEL_Y, -400), p(290, TEMPEL_Y, -450), p(380, TEMPEL_Y, -470), p(330, TEMPEL_Y, -520)};
    public static final BlockPos[] SPORENPRIESTER = {p(310, TEMPEL_Y, -430), p(360, TEMPEL_Y, -500)};

    // ---- Knuffelkamp: the Kampklodder's default home (npc/kern may pass their own spot to Kampklodder.plaats) --------------
    public static final BlockPos KAMPKLODDER = new BlockPos(18, Vadsrijk.GROND + 1, 1040);
}
