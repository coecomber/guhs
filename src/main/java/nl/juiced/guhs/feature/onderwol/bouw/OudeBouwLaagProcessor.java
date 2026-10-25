package nl.juiced.guhs.feature.onderwol.bouw;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;
import org.jspecify.annotations.Nullable;

/**
 * Processor {@code guhs:onderwol_oude_bouw} (1.7.0, ontwerpronde 22: "behouden + aanpassen"): the underground structures
 * from before the stone layers (guh_caves, challenging_guh_caves, kaasmijn, gatenkaas_mijnschacht, stille_voorraadkelder)
 * were built in a world of pink wool. In the Guhmension they now lie in viltsteen, knuffelsteen or guhkristalsteen, so
 * their rock is made of the layer of the depth each block sits at:
 * <ul>
 *   <li>{@link #GROND} (the templates' imitation rock: stone, dirt, deepslate, tuff, gatenkaas...) always becomes layer stone;</li>
 *   <li>{@link #VULLING} (pink wool, pink/white concrete, blackstone: walls and fill) only where it does not face the inside of
 *   the template (no air or other non-solid template block next to it), so the rooms keep their own walls as seen from
 *   inside, and only the hidden fill and the outside become stone;</li>
 *   <li>the mantle: where a solid template block faces the open world (another cave, a hall, a lake) under the surface, a block
 *   of layer stone is put there, so an old room never hangs half open in one of the new halls.</li>
 * </ul>
 * The layer stone: the world's own block at that spot when it is a natural layer stone (so veins and wool bubbles carry
 * on), else {@link GrotSchilProcessor#laagsteen(net.minecraft.world.level.LevelReader, BlockPos, BoundingBox)} (the nearest
 * layer stone around it, else the average layer by height: the same borders as OnderWolLagen). Deterministic: it only
 * reads the template and the world inside the chunk being placed. Not in the biomes without layers ({@link #ZONDER_LAGEN},
 * the Gatenkaasgrotten) and not outside the Guhmension. Decorative blocks (glass tubes, planks, terracotta, ores, lamps,
 * chests...) are never touched. Only new structure pieces use it (pieces saved before keep their own processor list).
 */
public class OudeBouwLaagProcessor extends StructureProcessor {
    public static final OudeBouwLaagProcessor INSTANCE = new OudeBouwLaagProcessor();
    public static final MapCodec<OudeBouwLaagProcessor> CODEC = MapCodec.unit(() -> INSTANCE);
    public static final TagKey<Block> GROND = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/oude_bouw_grond"));
    public static final TagKey<Block> VULLING = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/oude_bouw_vulling"));
    public static final TagKey<Biome> ZONDER_LAGEN = TagKey.create(Registries.BIOME, Guhs.id("onderwol/zonder_lagen"));
    /** No mantle in the top blocks under the surface (the entrances of the kaasmijn stay open). */
    public static final int MANTEL_ONDER_OPPERVLAK = 3;

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pos,
                                                                         List<StructureTemplate.StructureBlockInfo> original,
                                                                         List<StructureTemplate.StructureBlockInfo> processed,
                                                                         StructurePlaceSettings settings) {
        BoundingBox box = settings.getBoundingBox();
        if (box == null || !level.getLevel().dimension().equals(ModDimensions.GUHMENSION)) {
            return processed;
        }
        return verwerk(level, box, processed);
    }

    /** The work, for one chunk's box (public for the gametests, which run outside the Guhmension). */
    public static List<StructureTemplate.StructureBlockInfo> verwerk(ServerLevelAccessor level, BoundingBox box,
                                                                     List<StructureTemplate.StructureBlockInfo> processed) {
        // the template's blocks in and right around the box (the neighbours of everything placed now)
        Long2ObjectOpenHashMap<BlockState> mal = new Long2ObjectOpenHashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            BlockPos p = info.pos();
            if (p.getX() >= box.minX() - 1 && p.getX() <= box.maxX() + 1 && p.getY() >= box.minY() - 1 && p.getY() <= box.maxY() + 1
                    && p.getZ() >= box.minZ() - 1 && p.getZ() <= box.maxZ() + 1) {
                mal.put(p.asLong(), info.state());
            }
        }
        if (mal.isEmpty()) {
            return processed;
        }
        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processed.size());
        LongLinkedOpenHashSet mantel = new LongLinkedOpenHashSet();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            BlockPos p = info.pos();
            if (!box.isInside(p) || zonderLagen(level, p)) {
                out.add(info);
                continue;
            }
            BlockState s = info.state();
            if (s.is(GROND) || s.is(VULLING) && !binnenkant(mal, p)) {
                out.add(new StructureTemplate.StructureBlockInfo(p, steen(level, p, box), null));
            } else {
                out.add(info);
            }
            if (!GrotSchilProcessor.vast(s) || p.getY() >= oppervlak(level, p) - MANTEL_ONDER_OPPERVLAK) {
                continue;
            }
            for (Direction d : Direction.values()) {
                q.setWithOffset(p, d);
                if (box.isInside(q) && !mal.containsKey(q.asLong()) && !GrotSchilProcessor.vast(level.getBlockState(q))) {
                    mantel.add(q.asLong());
                }
            }
        }
        for (long m : mantel) {
            BlockPos p = BlockPos.of(m);
            out.add(new StructureTemplate.StructureBlockInfo(p, steen(level, p, box), null));
        }
        return out;
    }

    /** Next to a non-solid block of the template (its rooms, carpets, torches): a wall seen from inside. */
    static boolean binnenkant(Long2ObjectOpenHashMap<BlockState> mal, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockState n = mal.get(BlockPos.asLong(p.getX() + d.getStepX(), p.getY() + d.getStepY(), p.getZ() + d.getStepZ()));
            if (n != null && !GrotSchilProcessor.vast(n)) {
                return true;
            }
        }
        return false;
    }

    /** The layer stone for this spot (see the class comment). */
    public static BlockState steen(ServerLevelAccessor level, BlockPos p, @Nullable BoundingBox box) {
        BlockState world = level.getBlockState(p);
        return world.is(GrotSchilProcessor.LAAGSTEEN) ? world : GrotSchilProcessor.laagsteen(level, p, box);
    }

    private static boolean zonderLagen(ServerLevelAccessor level, BlockPos p) {
        return level.getBiome(p).is(ZONDER_LAGEN);
    }

    private static int oppervlak(ServerLevelAccessor level, BlockPos p) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, p.getX(), p.getZ());
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return OnderWolBouw.OUDE_BOUW.get();
    }
}
