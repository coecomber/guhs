package nl.juiced.guhs.feature.barbecuether;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.NetherWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.storage.GuhSavedData;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * 1.7.0 (ontwerp-guhs170-mijnen.md, ronde 25/26): no real lava in the Barbecuether any more, everywhere kaasfrituursaus.
 * <ul>
 *   <li>New chunks: the only real lava the Barbecuether ever made came from its cave carver: vanilla's nether_cave
 *       carver puts lava in every cave below y 31 (minGenY + 31; it ignores lava_level). {@code guhs:barbecuegrot} now
 *       uses {@link SausGrot} ({@code guhs:sausgrot}): the very same caves, with kaasfrituursaus. (The terrain's sea,
 *       springs and deltas already were kaasfrituursaus; no Barbecuether structure template holds lava.)</li>
 *   <li>Chunks that already exist (the live server: no reset): every chunk of the Barbecuether that loads is looked at
 *       once, a few per tick on the server thread. Sections whose palette has no lava are skipped at once; each lava
 *       block becomes the frying sauce with the same level (source stays source, flowing keeps its level, falling stays
 *       falling), without neighbour or shape updates. The sauce block schedules its own fluid tick, so what was flowing
 *       keeps flowing. The level remembers which chunks are done ({@link Stand}), so a chunk is never scanned again.</li>
 * </ul>
 */
public final class BbqZonderLava {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<WorldCarver<?>> CARVERS = DeferredRegister.create(Registries.CARVER, Guhs.MODID);
    public static final DeferredHolder<WorldCarver<?>, SausGrot> SAUS_GROT = CARVERS.register("sausgrot", () -> new SausGrot(CaveCarverConfiguration.CODEC));

    /** At most this many waiting chunks per tick ... */
    static final int CHUNKS_PER_TICK = 8;
    /** ... and after this many converted blocks the rest waits for the next tick. */
    static final int BLOKKEN_PER_TICK = 4096;
    /** set block, tell the clients, no neighbour updates and no shape updates of the neighbours (onPlace still runs). */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final Queue<Long> WACHT = new ConcurrentLinkedQueue<>();

    public static void register(IEventBus modBus) {
        CARVERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(BbqZonderLava::opLevelLoad);
        NeoForge.EVENT_BUS.addListener(BbqZonderLava::opChunkLoad);
        NeoForge.EVENT_BUS.addListener(BbqZonderLava::opTick);
    }

    // --- new chunks: the cave carver ------------------------------------------------------------------------------------

    /** Vanilla's nether cave carver, with kaasfrituursaus in the bottom of the caves instead of lava. */
    public static class SausGrot extends NetherWorldCarver {
        public SausGrot(Codec<CaveCarverConfiguration> codec) {
            super(codec);
        }

        @Override
        protected boolean carveBlock(CarvingContext context, CaveCarverConfiguration configuration, ChunkAccess chunk,
                                     Function<BlockPos, Holder<Biome>> biomeGetter, CarvingMask mask, BlockPos.MutableBlockPos blockPos,
                                     BlockPos.MutableBlockPos helperPos, Aquifer aquifer, MutableBoolean hasGrass) {
            if (this.canReplaceBlock(configuration, chunk.getBlockState(blockPos))) {
                BlockState state = blockPos.getY() <= context.getMinGenY() + 31
                        ? BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState() : CAVE_AIR;
                chunk.setBlockState(blockPos, state);
                return true;
            }
            return false;
        }
    }

    // --- existing chunks: once per chunk, when it loads -----------------------------------------------------------------

    /** The frying sauce that takes the place of this lava block (same level), or null when it isn't lava. */
    public static BlockState saus(BlockState state) {
        if (!state.is(Blocks.LAVA)) {
            return null;
        }
        return BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState().setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL));
    }

    static boolean isLava(BlockState state) {
        return state.is(Blocks.LAVA);
    }

    public static boolean hoortErbij(ServerLevel level) {
        return level.dimension() == BarbecuetherFeature.BARBECUETHER;
    }

    static void opLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && hoortErbij(level)) {
            WACHT.clear();
        }
    }

    static void opChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && hoortErbij(level) && event.getChunk() instanceof LevelChunk) {
            WACHT.add(event.getChunk().getPos().pack());   // (never change blocks while a chunk is loading: next tick)
        }
    }

    static void opTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !hoortErbij(level) || WACHT.isEmpty()) {
            return;
        }
        Stand stand = Stand.van(level);
        int blokken = 0;
        for (int n = 0; n < CHUNKS_PER_TICK && blokken < BLOKKEN_PER_TICK && !WACHT.isEmpty(); n++) {
            ChunkPos pos = ChunkPos.unpack(WACHT.poll());
            if (stand.heeft(pos)) {
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
            if (chunk == null) {
                continue;   // (unloaded again before its turn: it comes back with its next load)
            }
            int om = zetOm(level, chunk, null);
            blokken += om;
            stand.markeer(pos);
            if (om > 0) {
                LOGGER.debug("Guhs: {} lava blocks in Barbecuether chunk {} became kaasfrituursaus", om, pos);
            }
        }
    }

    /**
     * Turns every lava block of this chunk (only inside {@code box} when it isn't null) into kaasfrituursaus with the same
     * level. Sections without lava in their palette are skipped. Returns how many blocks changed.
     */
    public static int zetOm(ServerLevel level, LevelChunk chunk, BoundingBox box) {
        LevelChunkSection[] sections = chunk.getSections();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int om = 0;
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(BbqZonderLava::isLava)) {
                continue;
            }
            int y0 = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState saus = saus(section.getBlockState(x, y, z));
                        if (saus == null) {
                            continue;
                        }
                        pos.set(x0 + x, y0 + y, z0 + z);
                        if (box != null && !box.isInside(pos)) {
                            continue;
                        }
                        level.setBlock(pos, saus, FLAGS);
                        om++;
                    }
                }
            }
        }
        return om;
    }

    /** Per level: the chunks that have been looked at (and from then on hold no lava from before 1.7.0). */
    public static class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("bbq_zonder_lava", Stand::new, Stand::load,
                s -> s.save(new CompoundTag()));
        final LongSet chunks = new LongOpenHashSet();

        static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            tag.getLongArray("Chunks").ifPresent(a -> {
                for (long l : a) {
                    s.chunks.add(l);
                }
            });
            return s;
        }

        CompoundTag save(CompoundTag tag) {
            tag.putLongArray("Chunks", chunks.toLongArray());
            return tag;
        }

        public static Stand van(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        public boolean heeft(ChunkPos chunk) {
            return chunks.contains(chunk.pack());
        }

        public void markeer(ChunkPos chunk) {
            if (chunks.add(chunk.pack())) {
                setDirty();
            }
        }
    }

    private BbqZonderLava() {
    }
}
