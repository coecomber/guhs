package nl.juiced.guhs.feature.bio.kompas;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * biomes3: the biome search asks the biome source, and that only tells what a chunk WOULD become if it were made now. A
 * chunk that was made before an update moved the biomes (the server's world is never reset, and it is generated far out)
 * keeps the biome it got then. So a spot the search finds is checked against the saved chunk, when there is one: a chunk
 * that was saved with its biomes and does not hold the biome is skipped, and the search goes on to the next spot.
 * <p>
 * Only spots that match are checked (one chunk read from the region file each, on the search thread; the chunk is not
 * loaded into the world). A chunk that was never saved, or only got as far as its structure starts, passes: it gets its
 * biomes from the biome source of today.
 */
final class Opgeslagen {
    /** The chunk statuses in which the biomes are not filled in yet. */
    private static final Set<String> ZONDER_BIOMES = Set.of("", "minecraft:empty", "minecraft:structure_starts", "minecraft:structure_references",
            "empty", "structure_starts", "structure_references");

    /**
     * The check for one search in this dimension: can the chunk at this spot be (or become) this biome (id with
     * namespace)? When the chunk storage does not answer (the server is stopping), it stops checking and lets everything pass.
     */
    static Predicate<BlockPos> controle(ServerLevel level, String biome) {
        boolean[] kapot = {false};
        return plek -> {
            if (kapot[0]) {
                return true;
            }
            try {
                Optional<CompoundTag> chunk = level.getChunkSource().chunkMap.read(ChunkPos.containing(plek)).get(5, TimeUnit.SECONDS);
                return chunk.isEmpty() || kanZijn(chunk.get(), biome);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                kapot[0] = true;
                return true;
            } catch (Exception e) {
                kapot[0] = true;
                return true;
            }
        };
    }

    /** Can this saved chunk be (or become) this biome: its biomes are not filled in yet, or one of its sections holds it. */
    static boolean kanZijn(CompoundTag chunk, String biome) {
        if (ZONDER_BIOMES.contains(chunk.getStringOr("Status", ""))) {
            return true;
        }
        ListTag secties = chunk.getListOrEmpty("sections");
        for (int i = 0; i < secties.size(); i++) {
            ListTag palet = secties.getCompoundOrEmpty(i).getCompoundOrEmpty("biomes").getListOrEmpty("palette");
            for (int j = 0; j < palet.size(); j++) {
                if (biome.equals(palet.getStringOr(j, ""))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Opgeslagen() {
    }
}
