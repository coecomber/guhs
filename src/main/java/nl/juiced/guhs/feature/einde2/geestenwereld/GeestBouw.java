package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * einde2, slice geestenwereld: puts the build of the Geestenwereld down, chunk by chunk. The dimension is a flat world of
 * nothing with one biome whose only feature is this one ({@code guhs:einde2_geestenwereld_stukken}, step raw generation):
 * in every chunk it places the part of the tile {@code guhs:geestenwereld/stuk_<i>_<j>} that lies in that chunk (template
 * 0,0,0 at {@link Geestenwereld#OORSPRONG}; a tile is 32 x 32, two chunks each way, and lines up with them). The tiles keep
 * their block states as they are (known shape: nothing breaks off at a seam).
 * <p>
 * A later build reaches a world that has the old one: {@code data/guhs/einde2/geestenwereld_versie.json} holds a number
 * made from the build (tools/features/einde2_geestenwereld.py). The level remembers which chunks hold the build of that
 * number ({@link Stand}); a chunk of the build that loads without it is cleared and put down again a moment later (a few
 * chunks per tick). Nobody can build or break there, so nothing of a player is lost.
 */
public class GeestBouw extends Feature<NoneFeatureConfiguration> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int TILE = 32;
    /** How many waiting chunks are put down again per tick. */
    private static final int PER_TICK = 2;
    private static final Queue<Long> WACHT = new ConcurrentLinkedQueue<>();
    private static int versie = -1;

    public GeestBouw(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        ChunkPos chunk = ChunkPos.containing(context.origin());
        return plaats(context.level(), chunk, context.level().getLevel().getServer().getStructureManager(), Geestenwereld.OORSPRONG);
    }

    /** The template of tile i, j. */
    public static Identifier tile(int i, int j) {
        return Guhs.id(Geestenwereld.NAAM + "/stuk_" + i + "_" + j);
    }

    /** Does this chunk hold a part of the build (with this origin)? */
    public static boolean inBouw(ChunkPos chunk, BlockPos oorsprong) {
        int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
        return x0 + 15 >= oorsprong.getX() && x0 < oorsprong.getX() + Geestenwereld.BREEDTE
                && z0 + 15 >= oorsprong.getZ() && z0 < oorsprong.getZ() + Geestenwereld.BREEDTE;
    }

    /**
     * Places the part of the build that lies in this chunk (template 0,0,0 at {@code oorsprong}); false when there is none.
     * Writes only inside the chunk.
     */
    public static boolean plaats(ServerLevelAccessor level, ChunkPos chunk, StructureTemplateManager manager, BlockPos oorsprong) {
        if (!inBouw(chunk, oorsprong)) {
            return false;
        }
        int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
        BoundingBox doos = new BoundingBox(x0, level.getMinY(), z0, x0 + 15, level.getMaxY(), z0 + 15);
        boolean iets = false;
        for (int i = Math.floorDiv(x0 - oorsprong.getX(), TILE); i <= Math.floorDiv(x0 + 15 - oorsprong.getX(), TILE); i++) {
            for (int j = Math.floorDiv(z0 - oorsprong.getZ(), TILE); j <= Math.floorDiv(z0 + 15 - oorsprong.getZ(), TILE); j++) {
                if (i < 0 || j < 0) {
                    continue;
                }
                Optional<StructureTemplate> t = manager.get(tile(i, j));
                if (t.isEmpty()) {
                    continue;   // (an empty tile is left out by the builder)
                }
                BlockPos at = oorsprong.offset(i * TILE, 0, j * TILE);
                iets |= t.get().placeInWorld(level, at, at, settings(doos), RandomSource.create(0L), Block.UPDATE_CLIENTS);
            }
        }
        return iets;
    }

    static StructurePlaceSettings settings(BoundingBox doos) {
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setBoundingBox(doos)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                .setFinalizeEntities(true).setKnownShape(true);
    }

    // --- the version of the build ---------------------------------------------------------------------------------------------

    /** The version of the build in this jar (data/guhs/einde2/geestenwereld_versie.json; 1 when it can't be read). */
    public static int versie() {
        if (versie < 0) {
            versie = 1;
            try (InputStream in = GeestBouw.class.getResourceAsStream("/data/guhs/einde2/geestenwereld_versie.json")) {
                if (in != null) {
                    versie = Math.max(1, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().get("versie").getAsInt());
                }
            } catch (Exception e) {
                LOGGER.warn("Guhs einde2: can't read the version of the Geestenwereld", e);
            }
        }
        return versie;
    }

    /** Per level: the version of the build its chunks were made from, and the chunks that hold it. */
    public static class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("einde2_geestenwereld_stand", Stand::new, Stand::load,
                s -> s.save(new CompoundTag()));
        int versie;
        final LongSet chunks = new LongOpenHashSet();

        static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            s.versie = tag.getIntOr("Versie", 0);
            tag.getLongArray("Chunks").ifPresent(a -> {
                for (long l : a) {
                    s.chunks.add(l);
                }
            });
            return s;
        }

        CompoundTag save(CompoundTag tag) {
            tag.putInt("Versie", versie);
            tag.putLongArray("Chunks", chunks.toLongArray());
            return tag;
        }

        public static Stand van(ServerLevel level) {
            Stand s = level.getDataStorage().computeIfAbsent(TYPE);
            if (s.versie != versie()) {
                if (s.versie != 0) {
                    LOGGER.info("Guhs einde2: the Geestenwereld has a new build ({} -> {}): its chunks are put down again when they load", s.versie, versie());
                }
                s.versie = versie();
                s.chunks.clear();
                s.setDirty();
            }
            return s;
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

    static void opLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Geestenwereld.DIM) {
            versie = -1;
            WACHT.clear();
            Stand.van(level);
        }
    }

    static void opChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Geestenwereld.DIM) {
            return;
        }
        ChunkPos chunk = event.getChunk().getPos();
        if (!inBouw(chunk, Geestenwereld.OORSPRONG)) {
            return;
        }
        Stand stand = Stand.van(level);
        if (event.isNewChunk()) {
            stand.markeer(chunk);           // (just made by the feature, from this build)
        } else if (!stand.heeft(chunk)) {
            WACHT.add(chunk.pack());
        }
    }

    static void opTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Geestenwereld.DIM || WACHT.isEmpty()) {
            return;
        }
        Stand stand = Stand.van(level);
        for (int n = 0; n < PER_TICK && !WACHT.isEmpty(); n++) {
            ChunkPos chunk = ChunkPos.unpack(WACHT.poll());
            if (stand.heeft(chunk) || !level.hasChunk(chunk.x(), chunk.z())) {
                continue;
            }
            opnieuw(level, chunk);
            stand.markeer(chunk);
        }
    }

    /** Clears the build's part of this chunk (blocks only) and puts it down again from the templates in this jar. */
    public static void opnieuw(ServerLevel level, ChunkPos chunk) {
        BlockPos o = Geestenwereld.OORSPRONG;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = o.getY(); y < o.getY() + Geestenwereld.HOOGTE; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
        plaats(level, chunk, level.getServer().getStructureManager(), o);
    }
}
