package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * biomes3 wereld: the air that buildings of the Wolkenweide have reserved.
 * <p>
 * A {@code guhs:bio_plek} structure of kind {@code lucht} brings its own island in its template and starts in the middle
 * of its start chunk, {@code hoogte} blocks above the meadow. Where a structure set can start follows from the world
 * seed alone (its {@code random_spread} placement), so the terrain model can know it before anything is generated:
 * {@link WolkTerrein} makes no island, stair, lift or cloud within {@code ruimte} blocks (plus its own size) of such a
 * start chunk's middle. The room is kept free at every possible start, also the few that do not start in the end.
 * <p>
 * Filled once per server start from the structure sets ({@link #laad}); a set only counts with a
 * {@code minecraft:random_spread} placement.
 */
public final class Luchtruim {
    private record Plek(RandomSpreadStructurePlacement plaatsing, int ruimte) {
    }

    private static volatile List<Plek> plekken = List.of();
    private static volatile long seed;

    /** Reads the structure sets of this server (before any chunk is made). */
    public static void laad(MinecraftServer server) {
        List<Plek> nieuw = new ArrayList<>();
        var sets = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (StructureSet set : sets) {
            if (!(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
                continue;
            }
            int ruimte = -1;
            for (StructureSet.StructureSelectionEntry e : set.structures()) {
                if (e.structure().value() instanceof BioPlekStructure b && b.soort() == BioPlekken.Soort.LUCHT && b.doetMee()) {
                    ruimte = Math.max(ruimte, b.ruimte());
                }
            }
            if (ruimte >= 0) {
                nieuw.add(new Plek(spread, ruimte));
            }
        }
        seed = server.getWorldGenSettings().options().seed();
        plekken = List.copyOf(nieuw);
        BioModel.vergeet();
    }

    /** Is something of radius {@code straal} around (x, z) inside the room of a possible building in the air? */
    public static boolean bezet(int x, int z, int straal) {
        for (Plek p : plekken) {
            int s = p.plaatsing.spacing();
            int gx = Math.floorDiv(x >> 4, s), gz = Math.floorDiv(z >> 4, s);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ChunkPos c = p.plaatsing.getPotentialStructureChunk(seed, (gx + dx) * s, (gz + dz) * s);
                    double d = Math.hypot(c.getMiddleBlockX() - x, c.getMiddleBlockZ() - z);
                    if (d <= p.ruimte + straal) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** How many structure sets reserve air (for the self test). */
    public static int aantal() {
        return plekken.size();
    }

    private Luchtruim() {
    }
}
