package nl.juiced.guhs.feature.ringh5;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * bbq2 (ring-h5): every spot of the Zwarte Roosterpoort the code works with, in the coordinates of the whole build (what
 * Bezetting and Kopieen want for a guhs:burcht; {@link Terrein} turns them into the world). The single source is
 * tools/features/ring_h5_bouw.py (PLEKKEN): its check builds the valley, makes sure every spot is what it should be there
 * (a place to stand, a Rustvuurtje, out of or in the Eye's sight), reads THIS file and fails the generator run when a number
 * differs. So: change a number in both places, in exactly this shape ({@code NAME = new BlockPos(x, y, z);} or
 * {@code NAME = List.of(new BlockPos(x, y, z), ...);}; python tools/features/ring_h5_bouw.py --java prints the lot).
 * A box is two corners (smallest, largest; both inclusive).
 */
public final class Plekken {
    /** Where the Eye floats (the entity's feet), in the socket of the Mika head on the tower. */
    public static final BlockPos OOG = new BlockPos(48, 48, 83);

    /** The six Rustvuurtjes (the block itself), in the order you pass them. */
    public static final BlockPos VUUR_KAMP = new BlockPos(48, 7, 6);
    public static final BlockPos VUUR_UITKIJK = new BlockPos(54, 7, 20);
    public static final BlockPos VUUR_SLAKKENHUT = new BlockPos(13, 7, 44);
    public static final BlockPos VUUR_HOLTE = new BlockPos(85, 7, 59);
    public static final BlockPos VUUR_POORTJE = new BlockPos(19, 7, 63);
    public static final BlockPos VUUR_ACHTER = new BlockPos(20, 7, 85);

    /** The camp at the mouth: Boromika, the provisions Smikagol sniffs at, where he sits, and the box that counts as "arrived". */
    public static final BlockPos BOROMIKA = new BlockPos(52, 7, 5);
    public static final BlockPos PROVIAND = new BlockPos(41, 7, 9);
    public static final BlockPos SMIKAGOL_KAMP = new BlockPos(42, 7, 10);
    public static final List<BlockPos> KAMP = List.of(new BlockPos(40, 7, 2), new BlockPos(58, 11, 10));

    /** The crest of the stair over the ridge: the first look at the gate and the Eye. */
    public static final BlockPos UITKIJK = new BlockPos(48, 10, 14);

    /** Het Asveld: the box that counts as "in the field", the line the gaze runs up and down, its hiding places. */
    public static final List<BlockPos> ZONE_A = List.of(new BlockPos(10, 6, 21), new BlockPos(60, 13, 41));
    public static final List<BlockPos> BLIK_A = List.of(new BlockPos(48, 7, 24), new BlockPos(41, 7, 26), new BlockPos(34, 7, 30),
            new BlockPos(27, 7, 34), new BlockPos(20, 7, 38), new BlockPos(18, 7, 39));
    public static final List<BlockPos> SCHUIL_A = List.of(new BlockPos(43, 7, 23), new BlockPos(36, 7, 27), new BlockPos(29, 7, 31),
            new BlockPos(22, 7, 35), new BlockPos(16, 7, 39));

    /** De Kale Vlakte: its box and the line of the gaze. */
    public static final List<BlockPos> ZONE_B = List.of(new BlockPos(16, 6, 46), new BlockPos(83, 13, 56));
    public static final List<BlockPos> BLIK_B = List.of(new BlockPos(24, 7, 49), new BlockPos(38, 7, 50), new BlockPos(60, 7, 51),
            new BlockPos(80, 7, 54));

    /** Where the Eye looks when nobody is in a zone (a slow round), and the valley in which a worn ring draws it. */
    public static final List<BlockPos> BLIK_RUST = List.of(new BlockPos(30, 7, 26), new BlockPos(66, 7, 30), new BlockPos(70, 7, 50),
            new BlockPos(26, 7, 52));
    public static final List<BlockPos> DOMEIN = List.of(new BlockPos(6, 5, 11), new BlockPos(89, 31, 69));

    /** The curtain of smoke in the lane (a box) and the spot on the side you come from. */
    public static final List<BlockPos> ROOK = List.of(new BlockPos(71, 7, 62), new BlockPos(76, 11, 68));
    public static final BlockPos ROOK_TERUG = new BlockPos(79, 7, 65);

    /** The two riders of the Nine in the lane: each the two ends of its round. */
    public static final List<BlockPos> RUITER_1 = List.of(new BlockPos(66, 7, 63), new BlockPos(50, 7, 63));
    public static final List<BlockPos> RUITER_2 = List.of(new BlockPos(50, 7, 64), new BlockPos(66, 7, 64));

    /** Het Wachthek: its three guards (they look east, down the lane) and the post of skulls in front of it. */
    public static final BlockPos WACHTER_1 = new BlockPos(26, 7, 63);
    public static final BlockPos WACHTER_2 = new BlockPos(26, 7, 68);
    public static final BlockPos WACHTER_3 = new BlockPos(23, 7, 67);
    public static final BlockPos SCHEDELPAAL = new BlockPos(33, 7, 62);

    /** Het Roosterpoortje: the bars a player sees until Guhdalf opened it (really air), the tunnel, the yard in front, where the scene plays. */
    public static final List<BlockPos> DEUR = List.of(new BlockPos(12, 7, 70), new BlockPos(14, 9, 70));
    public static final List<BlockPos> TUNNEL = List.of(new BlockPos(12, 7, 70), new BlockPos(14, 10, 77));
    public static final List<BlockPos> VOOR_DEUR = List.of(new BlockPos(10, 7, 66), new BlockPos(16, 10, 69));
    public static final BlockPos DEUR_SCENE = new BlockPos(13, 7, 67);

    /** Behind the wall: the end of the chapter. */
    public static final List<BlockPos> ACHTER = List.of(new BlockPos(10, 7, 79), new BlockPos(30, 11, 92));

    /** Smikagol's routes (standing cells). */
    public static final List<BlockPos> ROUTE_UITKIJK = List.of(new BlockPos(48, 7, 10), new BlockPos(48, 10, 14));
    public static final List<BlockPos> ROUTE_B = List.of(new BlockPos(18, 7, 47), new BlockPos(38, 7, 49), new BlockPos(60, 7, 50),
            new BlockPos(80, 7, 55), new BlockPos(84, 7, 58));
    public static final List<BlockPos> ROUTE_LAAN = List.of(new BlockPos(80, 7, 62), new BlockPos(78, 7, 65), new BlockPos(68, 7, 66),
            new BlockPos(48, 7, 66), new BlockPos(36, 7, 64));
    public static final List<BlockPos> ROUTE_HEK = List.of(new BlockPos(33, 7, 65), new BlockPos(24, 7, 65), new BlockPos(20, 7, 65));
    public static final List<BlockPos> ROUTE_DEUR = List.of(new BlockPos(16, 7, 66), new BlockPos(13, 7, 68));
    public static final List<BlockPos> ROUTE_ACHTER = List.of(new BlockPos(13, 7, 72), new BlockPos(13, 7, 79), new BlockPos(18, 7, 83));

    /** The three ways in, where they meet the edge of the build: the mouth, and a tunnel through the cliff on either side of the camp. */
    public static final List<BlockPos> INGANGEN = List.of(new BlockPos(48, 7, 0), new BlockPos(0, 7, 6), new BlockPos(95, 7, 6));
    public static final List<BlockPos> UITGANGEN = List.of(new BlockPos(20, 7, 95), new BlockPos(0, 7, 85), new BlockPos(95, 7, 85));

    /** The size of the whole build and its anchor (the burcht's anchor: tools/features/ring_h5.py). */
    public static final BlockPos MAAT = new BlockPos(96, 64, 96);
    public static final BlockPos ANKER = new BlockPos(48, 6, 48);

    /** Where the Eye's sight starts (the front of its pupil). */
    public static final Vec3 OOG_KIJK = new Vec3(48.5, 49.6, 82.1);

    private Plekken() {
    }
}
