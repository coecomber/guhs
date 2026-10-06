package nl.juiced.guhs.feature.paleizen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (paleizen): where the things of the three questlines are, in the coordinates of the templates, and how a spot of a
 * template becomes a spot of the copy a player stands in.
 * <p>
 * The numbers between the {@code // <structure>} markers are the builder's: tools/features/paleizen_bouw.py fills
 * {@code PLEKKEN} while it builds, and {@code paleizen.selfcheck} fails the resource build when a number here differs (it
 * prints the line to paste). The two burchten count in the coordinates of the whole build, the stal in those of its one
 * template ({@link Kopieen#wereld}).
 */
public final class PaleisPlekken {
    public static final String WOONBLOKKEN = "mika_woonblokken", STAL = "mika_stal", BRUGPALEIS = "mika_brugpaleis";

    /** Het Mika-brugpaleis. */
    public static final class Brug {
        // <mika_brugpaleis>
        public static final BlockPos TOLWACHTER_MIKA = new BlockPos(35, 29, 13);
        public static final BlockPos ANKER = new BlockPos(55, 28, 16);
        /** The passage through the tolhuis: whoever has not paid is shoved back out of it. */
        public static final BoundingBox TOLPOORT = new BoundingBox(38, 29, 13, 46, 34, 17);
        /** Where the Tolwachter shoves you to: the deck in front of the mouth. */
        public static final BlockPos TOLPOORT_BUITEN = new BlockPos(30, 29, 15);
        /** The five missing rows of planks (a row = the blocks with the same x). */
        public static final BoundingBox GAT = new BoundingBox(58, 28, 13, 62, 28, 17);
        public static final BlockPos BEL = new BlockPos(82, 33, 15);
        public static final BlockPos STEIGER = new BlockPos(60, 26, 15);
        // </mika_brugpaleis>

        private Brug() {
        }
    }

    /** De Mika-woonblokken. */
    public static final class Woon {
        // <mika_woonblokken>
        public static final BlockPos BREIWERK = new BlockPos(52, 28, 38);
        public static final BlockPos MIKA_OMA = new BlockPos(28, 33, 17);
        public static final BlockPos MOPPER_0 = new BlockPos(11, 13, 36);
        public static final BlockPos MOPPER_0_VOOR = new BlockPos(14, 13, 37);
        public static final BlockPos MOPPER_1 = new BlockPos(51, 23, 23);
        public static final BlockPos MOPPER_1_VOOR = new BlockPos(48, 23, 23);
        public static final BlockPos MOPPER_2 = new BlockPos(36, 23, 11);
        public static final BlockPos MOPPER_2_VOOR = new BlockPos(38, 23, 14);
        public static final BlockPos MOPPER_3 = new BlockPos(17, 18, 33);
        public static final BlockPos MOPPER_4 = new BlockPos(33, 13, 36);
        public static final BlockPos MOPPER_5 = new BlockPos(10, 33, 34);
        public static final BlockPos ANKER = new BlockPos(24, 12, 23);
        // </mika_woonblokken>
        /** The six neighbours by number (0, 1, 2: the grumpy three of the questline) and how they look in the template. */
        public static final BlockPos[] MOPPERS = {MOPPER_0, MOPPER_1, MOPPER_2, MOPPER_3, MOPPER_4, MOPPER_5};
        public static final float[] MOPPER_YAW = {-90f, 90f, 0f, -90f, 180f, 90f};

        private Woon() {
        }
    }

    /** De Mika-stal. */
    public static final class Stal {
        // <mika_stal>
        public static final BlockPos SCHUIL_1 = new BlockPos(9, 12, 16);
        public static final BlockPos ZWIJNTJE_0 = new BlockPos(8, 7, 7);
        public static final BlockPos ZWIJNTJE_1 = new BlockPos(13, 7, 7);
        public static final BlockPos ZWIJNTJE_2 = new BlockPos(18, 7, 7);
        public static final BlockPos ZWIJNTJE_3 = new BlockPos(8, 7, 17);
        public static final BlockPos VOERBAK = new BlockPos(7, 7, 12);
        public static final BlockPos STALKNECHTGUH = new BlockPos(32, 7, 12);
        public static final BlockPos SCHUIL_0 = new BlockPos(35, 7, 34);
        public static final BlockPos SCHUIL_2 = new BlockPos(25, 7, 33);
        public static final BlockPos ZWIJNTJE_4 = new BlockPos(15, 7, 28);
        public static final BlockPos ONTSNAPT = new BlockPos(22, 7, 31);
        // </mika_stal>
        /** The stable's own Worstzwijntjes by number (4 = the piglet in the paddock) and how they look in the template. */
        public static final BlockPos[] ZWIJNTJES = {ZWIJNTJE_0, ZWIJNTJE_1, ZWIJNTJE_2, ZWIJNTJE_3, ZWIJNTJE_4};
        public static final float[] ZWIJNTJE_YAW = {0f, 0f, 0f, 180f, 45f};
        /** Where the runaway hides, in the order it flees. */
        public static final BlockPos[] SCHUILPLEKKEN = {SCHUIL_0, SCHUIL_2, SCHUIL_1};

        private Stal() {
        }
    }

    // --- from a template to the world ------------------------------------------------------------------------------------

    /** The copy of {@code guhs:<structuur>} this spot belongs to (a piece of it within 48 blocks; loaded chunks only), or null. */
    @Nullable
    public static StructureStart kopie(ServerLevel level, String structuur, BlockPos bij) {
        return Bezetting.start(level, structuur, bij);
    }

    /** The world position of a template block of this copy (null: the copy has no start piece, which does not happen). */
    @Nullable
    public static BlockPos wereld(StructureStart start, BlockPos lokaal) {
        return Kopieen.wereld(start, null, lokaal);
    }

    /** The middle of the floor of that block. */
    @Nullable
    public static Vec3 voet(StructureStart start, BlockPos lokaal) {
        BlockPos pos = wereld(start, lokaal);
        return pos == null ? null : Vec3.atBottomCenterOf(pos);
    }

    /** A box of the template in the world (turned with the copy). */
    @Nullable
    public static BoundingBox wereld(StructureStart start, BoundingBox lokaal) {
        BlockPos a = wereld(start, new BlockPos(lokaal.minX(), lokaal.minY(), lokaal.minZ()));
        BlockPos b = wereld(start, new BlockPos(lokaal.maxX(), lokaal.maxY(), lokaal.maxZ()));
        return a == null || b == null ? null : BoundingBox.fromCorners(a, b);
    }

    /** How this copy is turned. */
    public static Rotation draai(StructureStart start) {
        return Kopieen.draai(start, null);
    }

    /** Is this world position the template block {@code lokaal} of a copy of {@code structuur} here? */
    public static boolean is(ServerLevel level, String structuur, BlockPos wereld, BlockPos lokaal) {
        StructureStart start = kopie(level, structuur, wereld);
        return start != null && wereld.equals(wereld(start, lokaal));
    }

    private PaleisPlekken() {
    }
}
