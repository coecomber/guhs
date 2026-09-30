package nl.juiced.guhs.world.grond;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * The ground of our structures (2.10, DESIGN 9.1): see {@link GrondPoolElement}.
 * <ul>
 *   <li>{@link #delta}: the ground level delta of a start pool (all its elements share it; 1 = vanilla);</li>
 *   <li>{@link #startY}: for our own starts WITHOUT heightmap projection (the Knuffeldal town, the Elf-Guhjestocht): vanilla
 *       sinks such a start piece by its delta, so the y to ask for keeps the anchor on the top block of the ground;</li>
 *   <li>{@link #rand}: measures the terrain right around a placed piece (for /guhs bouwcheck): how far the land next to
 *       the walls lies below (or above) the floor.</li>
 * </ul>
 */
public final class Grond {
    private Grond() {
    }

    /** The ground level delta of a start pool (the largest of its elements; vanilla elements say 1). */
    public static int delta(Holder<StructureTemplatePool> pool) {
        int delta = 1;
        boolean any = false;
        for (StructurePoolElement e : pool.value().getShuffledTemplates(RandomSource.create(0L))) {
            delta = any ? Math.max(delta, e.getGroundLevelDelta()) : e.getGroundLevelDelta();
            any = true;
        }
        return delta;
    }

    /**
     * The y to hand to {@code JigsawPlacement.addPieces} (no heightmap projection) so that the start jigsaw ends up on the
     * top block of the ground ({@code surface} = first free y): vanilla moves the start piece down by its delta.
     */
    public static int startY(Holder<StructureTemplatePool> pool, int surface) {
        return surface - 1 + delta(pool);
    }

    /** Heights of the land around a piece, per ring distance (1 = right next to the box). */
    public interface Hoogte {
        /** The first free y of the column (x, z), or Integer.MIN_VALUE when it isn't there. */
        int at(int x, int z);
    }

    /**
     * The terrain around a piece: for ring d = 1..rings (d blocks outside the box) the average of
     * {@code (surface - 1) - floor} over the ring's columns, where floor = {@code minY + delta - 1} (the template's top ground
     * block). Near 0 = the land meets the floor; the old moat showed as -3, -2, -1 for d = 1, 2, 3.
     * Returns NaN for a ring without any measured column.
     */
    public static double[] rand(BoundingBox box, int delta, int rings, Hoogte hoogte) {
        double[] out = new double[rings + 1];
        int floor = box.minY() + delta - 1;
        for (int d = 1; d <= rings; d++) {
            long sum = 0;
            int n = 0;
            int x0 = box.minX() - d, x1 = box.maxX() + d, z0 = box.minZ() - d, z1 = box.maxZ() + d;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (x != x0 && x != x1 && z != z0 && z != z1) {
                        continue;
                    }
                    int h = hoogte.at(x, z);
                    if (h == Integer.MIN_VALUE) {
                        continue;
                    }
                    sum += (h - 1) - floor;
                    n++;
                }
            }
            out[d] = n == 0 ? Double.NaN : (double) sum / n;
        }
        return out;
    }
}
