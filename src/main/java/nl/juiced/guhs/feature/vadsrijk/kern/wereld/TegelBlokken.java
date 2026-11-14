package nl.juiced.guhs.feature.vadsrijk.kern.wereld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * 1.10.3 (npc-vloer): the blocks of the shipped map tiles by world position, as {@link KaartFeature} places them (64 x 320 x 64,
 * corner (tx * 64, 0, tz * 64); a cell the tile does not store is air). For checks on the tile data (game tests) and for
 * restoring a spot of the live world to what the map generator made ({@code TegelPatch}). Tiles are decoded lazily and kept
 * while this object lives: make one per job.
 */
public final class TegelBlokken {
    public static final int T = KaartFeature.TEGEL, H = 320;
    private final ServerLevel sl;
    private final HolderGetter<Block> blokken;
    private final Map<Long, Optional<Tegel>> tegels = new HashMap<>();

    /** One decoded tile: its states and the block-entity nbt of the cells that have one. */
    private record Tegel(BlockState[] staten, Map<Integer, CompoundTag> nbt) {
    }

    public TegelBlokken(ServerLevel sl) {
        this.sl = sl;
        this.blokken = sl.holderLookup(Registries.BLOCK);
    }

    private static int index(int x, int y, int z) {
        return (y * T + z) * T + x;
    }

    private Optional<Tegel> tegel(int tx, int tz) {
        return tegels.computeIfAbsent(((long) tx << 32) | (tz & 0xFFFFFFFFL), k -> {
            Optional<StructureTemplate> t = KaartFeature.tegel(sl, tx, tz);
            if (t.isEmpty()) {
                return Optional.empty();
            }
            BlockState[] a = new BlockState[T * H * T];
            Map<Integer, CompoundTag> nbt = new HashMap<>();
            CompoundTag tag = t.get().save(new CompoundTag());
            ListTag palet = tag.getListOrEmpty("palette");
            List<BlockState> staten = new ArrayList<>();
            for (int i = 0; i < palet.size(); i++) {
                staten.add(NbtUtils.readBlockState(blokken, palet.getCompoundOrEmpty(i)));
            }
            ListTag lijst = tag.getListOrEmpty("blocks");
            for (int i = 0; i < lijst.size(); i++) {
                CompoundTag e = lijst.getCompoundOrEmpty(i);
                ListTag pos = e.getListOrEmpty("pos");
                int x = pos.getIntOr(0, 0), y = pos.getIntOr(1, 0), z = pos.getIntOr(2, 0), s = e.getIntOr("state", 0);
                if (x >= 0 && x < T && y >= 0 && y < H && z >= 0 && z < T && s >= 0 && s < staten.size()) {
                    a[index(x, y, z)] = staten.get(s);
                    if (e.get("nbt") instanceof CompoundTag n) {
                        nbt.put(index(x, y, z), n);
                    }
                }
            }
            return Optional.of(new Tegel(a, nbt));
        });
    }

    public boolean heeftTegel(BlockPos p) {
        return tegel(Math.floorDiv(p.getX(), T), Math.floorDiv(p.getZ(), T)).isPresent();
    }

    /** The tile's block here (air outside the tiles / not stored / structure void). */
    public BlockState staat(BlockPos p) {
        if (p.getY() < 0 || p.getY() >= H) {
            return Blocks.AIR.defaultBlockState();
        }
        Optional<Tegel> t = tegel(Math.floorDiv(p.getX(), T), Math.floorDiv(p.getZ(), T));
        if (t.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState s = t.get().staten()[index(Math.floorMod(p.getX(), T), p.getY(), Math.floorMod(p.getZ(), T))];
        return s == null || s.is(Blocks.STRUCTURE_VOID) ? Blocks.AIR.defaultBlockState() : s;
    }

    /** The tile's block-entity nbt here (null: none). */
    @Nullable
    public CompoundTag nbt(BlockPos p) {
        if (p.getY() < 0 || p.getY() >= H) {
            return null;
        }
        Optional<Tegel> t = tegel(Math.floorDiv(p.getX(), T), Math.floorDiv(p.getZ(), T));
        return t.isEmpty() ? null : t.get().nbt().get(index(Math.floorMod(p.getX(), T), p.getY(), Math.floorMod(p.getZ(), T)));
    }
}
