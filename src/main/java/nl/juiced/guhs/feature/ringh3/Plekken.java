// WRITTEN BY tools/features/ring_h3_java.py - do not edit by hand: change ring_h3_bouw.py / ring_h3_scene.py and run that tool again.

package nl.juiced.guhs.feature.ringh3;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * bbq2 (ring-h3): where everything is in the template of De Mijnen van Knabbelmoria (template coordinates: {@link Mijn} turns
 * them into the world for a copy). The numbers come from tools/features/ring_h3_bouw.py, which builds the template.
 */
public final class Plekken {
    /** A box of template blocks, both corners included. */
    public record Doos(int x0, int y0, int z0, int x1, int y1, int z1) {
        public boolean binnen(Vec3i p) {
            return p.getX() >= x0 && p.getX() <= x1 && p.getY() >= y0 && p.getY() <= y1 && p.getZ() >= z0 && p.getZ() <= z1;
        }

        public BlockPos hoek() {
            return new BlockPos(x0, y0, z0);
        }

        public BlockPos midden() {
            return new BlockPos((x0 + x1) / 2, y0, (z0 + z1) / 2);
        }
    }

    /** A character of the cast in the template: only there for players whose step of ring_h3 is van..tot. */
    public record Rol(String kind, String id, BlockPos plek, float yaw, @Nullable String rol, int van, int tot) {
    }

    /** The template y of the top block of the cave floor, and the three walking levels. */
    public static final int G = 30, BOVEN = 31, MIDDEL = 21, DIEP = 12;
    public static final Vec3i MAAT = new Vec3i(92, 46, 76);
    public static final BlockPos POORT_SCHRIFT = new BlockPos(37, 38, 62);
    public static final BlockPos POORT_BUITEN = new BlockPos(35, 31, 62);
    public static final Doos PLEIN = new Doos(22, 30, 53, 37, 40, 70);
    public static final Doos HEFBOOMHAL = new Doos(34, 21, 28, 50, 26, 41);
    public static final Doos PUTKAMER = new Doos(14, 21, 28, 28, 26, 42);
    public static final BlockPos PUT = new BlockPos(21, 21, 35);
    public static final BlockPos GIMGUH = new BlockPos(15, 21, 37);
    public static final Doos ZUILENHAL = new Doos(18, 12, 3, 46, 26, 25);
    public static final Doos KLOOF = new Doos(18, 0, 3, 86, 8, 25);
    public static final Doos OOSTOEVER = new Doos(78, 11, 4, 86, 18, 24);
    public static final BlockPos BRUG_ANKER = new BlockPos(71, 12, 14);
    public static final Doos BRUG_KAPOT = new Doos(66, 9, 13, 72, 11, 15);
    public static final BlockPos BRUG_RAND_WEST = new BlockPos(65, 12, 14);
    public static final BlockPos BRUG_RAND_OOST = new BlockPos(73, 12, 14);
    public static final BlockPos ROG_START = new BlockPos(11, 12, 14);
    public static final BlockPos ROG_EINDE = new BlockPos(43, 12, 14);
    public static final BlockPos HAL_INGANG = new BlockPos(21, 12, 25);
    public static final BlockPos RUSTVUUR_HAL = new BlockPos(21, 12, 29);
    public static final BlockPos RUSTVUUR_PUT = new BlockPos(25, 21, 38);
    public static final BlockPos RUSTVUUR_PLEIN = new BlockPos(27, 31, 59);
    public static final BlockPos RUSTVUUR_OOST = new BlockPos(68, 31, 59);
    public static final BlockPos ARAGUH = new BlockPos(64, 31, 62);
    public static final Doos OOSTPLEIN = new Doos(60, 30, 53, 75, 40, 70);
    // the doors: the blocks that slide away (templates guhs:ringh3_deur_<name>)
    public static final Doos DEUR_WEST = new Doos(38, 31, 61, 38, 35, 63);
    public static final Doos DEUR_VALHEK = new Doos(31, 21, 34, 31, 24, 36);
    public static final Doos DEUR_GEHEIM = new Doos(13, 21, 35, 13, 23, 36);
    public static final Doos DEUR_OOST = new Doos(58, 31, 61, 58, 35, 63);
    /** The four levers of the lever hall (lever nr = index), and the order they want to be pulled in. */
    public static final List<BlockPos> HENDELS = List.of(new BlockPos(37, 22, 27), new BlockPos(40, 22, 27), new BlockPos(44, 22, 27), new BlockPos(47, 22, 27));
    public static final int[] HENDEL_VOLGORDE = {1, 0, 2, 3};
    /** The six rune stones round Gimguh's door (their teken: the block state), the one to knock on and how often. */
    public static final List<BlockPos> RUNEN = List.of(new BlockPos(13, 22, 31), new BlockPos(13, 22, 32), new BlockPos(13, 22, 33), new BlockPos(13, 22, 38), new BlockPos(13, 22, 39), new BlockPos(13, 22, 40));
    public static final int[] RUNE_TEKENS = {4, 1, 5, 3, 0, 2};
    public static final int RUNE_GOED = 0, RUNE_KLOPPEN = 3, RUNE_NJEG = 6;
    public static final List<Rol> CAST = List.of(
            new Rol("guhdalf", "ringh3_guhdalf_poort", new BlockPos(35, 31, 60), 250.0f, "ringh3_poort", 0, 1),
            new Rol("gimguh", "ringh3_gimguh_poort", new BlockPos(34, 31, 65), 290.0f, null, 0, 1),
            new Rol("leguhlas", "ringh3_leguhlas_poort", new BlockPos(31, 31, 66), 300.0f, null, 0, 1),
            new Rol("araguh", "ringh3_araguh_poort", new BlockPos(31, 31, 58), 240.0f, null, 0, 1),
            new Rol("boromika", "ringh3_boromika_poort", new BlockPos(28, 31, 65), 290.0f, null, 0, 1),
            new Rol("merrie", "ringh3_merrie_poort", new BlockPos(26, 31, 60), 100.0f, null, 0, 1),
            new Rol("pippguh", "ringh3_pippguh_poort", new BlockPos(25, 31, 62), 80.0f, null, 0, 1),
            new Rol("guhdalf", "ringh3_guhdalf_hal", new BlockPos(45, 21, 38), 30.0f, "ringh3_hal", 2, 2),
            new Rol("gimguh", "ringh3_gimguh_hal", new BlockPos(38, 21, 39), 330.0f, null, 2, 2),
            new Rol("gimguh", "ringh3_gimguh_gang", new BlockPos(15, 21, 37), 300.0f, "ringh3_gang", 3, 4),
            new Rol("pippguh", "ringh3_pippguh_put", new BlockPos(24, 21, 33), 100.0f, "ringh3_put", 3, 4),
            new Rol("merrie", "ringh3_merrie_put", new BlockPos(25, 21, 37), 60.0f, null, 3, 4),
            new Rol("guhdalf", "ringh3_guhdalf_put", new BlockPos(17, 21, 31), 320.0f, "ringh3_put_guhdalf", 3, 4),
            new Rol("araguh", "ringh3_araguh_buiten", new BlockPos(64, 31, 62), 90.0f, "ringh3_buiten", 6, 6),
            new Rol("leguhlas", "ringh3_leguhlas_buiten", new BlockPos(66, 31, 58), 120.0f, null, 6, 6),
            new Rol("gimguh", "ringh3_gimguh_buiten", new BlockPos(68, 31, 65), 60.0f, null, 6, 6),
            new Rol("boromika", "ringh3_boromika_buiten", new BlockPos(71, 31, 62), 100.0f, null, 6, 6),
            new Rol("merrie", "ringh3_merrie_buiten", new BlockPos(67, 31, 67), 20.0f, null, 6, 6),
            new Rol("pippguh", "ringh3_pippguh_buiten", new BlockPos(69, 31, 67), 340.0f, null, 6, 6));

    private Plekken() {
    }
}
