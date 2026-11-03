package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * The area plan of zone 1 (CONTRACT_VADSRIJK §2). Boxes are inclusive x/z ranges; the first matching area wins, so the
 * catch-all "ring" is last. Slices may {@link #registreer} extra (smaller) areas before the big ones with {@link #voegVooraan}.
 */
public final class Gebieden {
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLevel, int maxLevel, boolean stad) {
        public boolean bevat(double x, double z) {
            return x >= minX && x <= maxX + 0.999 && z >= minZ && z <= maxZ + 0.999;
        }

        public BlockPos midden(int y) {
            return new BlockPos((minX + maxX) / 2, y, (minZ + maxZ) / 2);
        }

        public String naamKey() {
            return "vadsrijk.kern.gebied." + id;
        }
    }

    /** Where a player arrives the first time (Knuffelkamp dock). */
    public static final BlockPos AANKOMST = new BlockPos(0, 65, 1130);

    private static final List<Gebied> ALLE = new ArrayList<>(List.of(
            new Gebied("zwamzoet", -160, -160, 160, 160, 1, 20, true),
            new Gebied("haven", -120, 400, 120, 600, 1, 20, true),
            new Gebied("klodderweide", 160, -300, 600, 400, 1, 6, false),
            new Gebied("sporenwoud", -600, -160, -160, 400, 6, 13, false),
            new Gebied("druipgrotten", -600, -600, -160, -160, 12, 18, false),
            new Gebied("zwamtempel", 160, -600, 600, -300, 18, 20, false),
            new Gebied("knuffelkamp", -128, 900, 128, 1150, 1, 3, true),
            new Gebied("proeftuin", 3000, 3000, 3192, 3192, 1, 20, false),
            new Gebied("ring", -600, -600, 600, 600, 1, 20, false)));

    private Gebieden() {
    }

    public static List<Gebied> alle() {
        return Collections.unmodifiableList(ALLE);
    }

    @Nullable
    public static Gebied op(double x, double z) {
        for (Gebied g : ALLE) {
            if (g.bevat(x, z)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public static Gebied van(String id) {
        for (Gebied g : ALLE) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** A smaller area (a dungeon, an arena) that must win over the big boxes. */
    public static synchronized void voegVooraan(Gebied g) {
        ALLE.removeIf(x -> x.id().equals(g.id()));
        ALLE.add(0, g);
    }

    /** The proeftuin (kern test area): its centre at ground level + 1. */
    public static BlockPos proeftuin() {
        return new BlockPos(3096, Vadsrijk.GROND + 1, 3096);
    }
}
