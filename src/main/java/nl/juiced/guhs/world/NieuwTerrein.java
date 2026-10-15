package nl.juiced.guhs.world;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongPredicate;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.level.ChunkDataEvent;

/**
 * bbq2: which chunks of a dimension exist already, so a new guaranteed building ({@link GegarandeerdPlacement},
 * {@code "alleen_nieuw": true}) can be put in terrain that still has to be generated. A structure start is only made when its
 * chunk is generated for the first time, and its pieces are only placed in chunks that are generated after that: in an
 * existing world a building whose spot lies in old chunks would never appear.
 * <p>
 * A chunk "exists" when it has reached the structure-starts status:
 * <ul>
 *   <li>in memory: its holder in the chunk map says so (read from any thread: the visible chunk map is a snapshot);</li>
 *   <li>saved this session ({@link #opSave}: the chunk may still be on its way to the region file);</li>
 *   <li>on disk: it has an entry in the header of its region file (r.x.z.mca, the first 4096 bytes: 1024 offsets). The header
 *       is read straight from the file, once per region and search: asking the game's own storage would create an empty
 *       region file for every region that is looked at.</li>
 * </ul>
 * A region file that can't be read counts as full (nothing is put there). An index is made for one search and thrown away.
 */
public final class NieuwTerrein {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BitSet LEEG = new BitSet(0);
    private static final BitSet VOL = new BitSet(0);

    /** Chunks saved in this session, per level (only filled while the level still has a search to do, see {@link #volg}). */
    private static final Map<ServerLevel, Set<Long>> BEWAARD = Collections.synchronizedMap(new WeakHashMap<>());

    private final Path regio;
    private final LongPredicate inGeheugen;
    private final Set<Long> bewaard;
    private final Map<Long, BitSet> regios = new HashMap<>();

    private NieuwTerrein(Path regio, LongPredicate inGeheugen, Set<Long> bewaard) {
        this.regio = regio;
        this.inGeheugen = inGeheugen;
        this.bewaard = bewaard;
    }

    /** The index of this level as it is now. */
    public static NieuwTerrein van(ServerLevel level) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        ChunkMap chunks = level.getChunkSource().chunkMap;
        Set<Long> bewaard = BEWAARD.get(level);
        return new NieuwTerrein(DimensionType.getStorageFolder(level.dimension(), root).resolve("region"), key -> {
            ChunkStatus status = chunks.getLatestStatus(key);
            return status != null && status.isOrAfter(ChunkStatus.STRUCTURE_STARTS);
        }, bewaard == null ? Set.of() : bewaard);
    }

    /** (tests) an index over a folder of region files and a set of chunks that count as loaded. */
    public static NieuwTerrein van(Path regioMap, Set<Long> geladen) {
        return new NieuwTerrein(regioMap, geladen::contains, Set.of());
    }

    /** (tests) an index in which exactly the chunks of this predicate (packed positions) exist. */
    public static NieuwTerrein van(LongPredicate bestaat) {
        return new NieuwTerrein(Path.of("guhs-geen-regio-map"), bestaat, Set.of());
    }

    /** From now on the chunks this level saves are remembered (until {@link #klaar}). */
    public static void volg(ServerLevel level) {
        BEWAARD.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet());
    }

    /** Every search of this level is done: nothing has to be remembered any more. */
    public static void klaar(ServerLevel level) {
        BEWAARD.remove(level);
    }

    /** A chunk is written (it may still be in the save queue when a search looks at the region file). */
    public static void opSave(ChunkDataEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Set<Long> set = BEWAARD.get(level);
            if (set != null) {
                set.add(event.getChunk().getPos().pack());
            }
        }
    }

    /** Does this chunk exist already (it reached the structure starts, in memory or on disk)? */
    public boolean bestaat(int x, int z) {
        long key = ChunkPos.pack(x, z);
        if (inGeheugen.test(key) || bewaard.contains(key)) {
            return true;
        }
        BitSet r = regios.computeIfAbsent(ChunkPos.pack(x >> 5, z >> 5), k -> lees(x >> 5, z >> 5));
        return r == VOL || (r != LEEG && r.get((x & 31) + (z & 31) * 32));
    }

    /**
     * Is all the terrain new within {@code straal} blocks (per axis) of the middle of this chunk: no chunk that touches that
     * square exists yet?
     */
    public boolean nieuw(ChunkPos chunk, int straal) {
        int mx = chunk.getMiddleBlockX(), mz = chunk.getMiddleBlockZ();
        int x0 = (mx - straal) >> 4, x1 = (mx + straal) >> 4, z0 = (mz - straal) >> 4, z1 = (mz + straal) >> 4;
        if (bestaat(chunk.x(), chunk.z())) {
            return false;   // (the usual answer in an explored area: no need to walk the square)
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (bestaat(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The chunks of a region that have an entry in its header. */
    private BitSet lees(int rx, int rz) {
        Path file = regio.resolve("r." + rx + "." + rz + ".mca");
        try (SeekableByteChannel in = Files.newByteChannel(file, StandardOpenOption.READ)) {
            ByteBuffer header = ByteBuffer.allocate(4096);
            while (header.hasRemaining() && in.read(header) > 0) {
                // (keep reading)
            }
            if (header.position() < 4096) {
                return LEEG;   // (a region file that was only just made: no chunks in it)
            }
            header.flip();
            BitSet out = new BitSet(1024);
            for (int i = 0; i < 1024; i++) {
                if (header.getInt() != 0) {
                    out.set(i);
                }
            }
            return out.isEmpty() ? LEEG : out;
        } catch (NoSuchFileException e) {
            return LEEG;
        } catch (IOException e) {
            LOGGER.warn("Guhs: could not read the header of {}: its chunks count as existing", file, e);
            return VOL;
        }
    }
}
