package nl.juiced.guhs.feature.techbeurs;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code guhs:vadslamp_licht}: the invisible light a burning Vadslamp keeps in the air around it (light {@link #LICHT}). No
 * item, no drops, nothing to see or touch; anything placed or flowing into it replaces it.
 * <p>
 * Never left behind: the lamps that burn register themselves here ({@link #aan}, per level, by chunk), and every light
 * block checks for itself every {@link #KIJK} ticks and on random ticks whether a registered lamp is near enough; if not
 * (the lamp went out, was broken or unloaded, the server crashed with lights saved in a chunk) it turns back into air.
 */
public class VadslampLicht extends Block {
    public static final MapCodec<VadslampLicht> CODEC = simpleCodec(VadslampLicht::new);
    /** The light level of one light block. */
    public static final int LICHT = 14;
    /** Every this many ticks a light block checks that its lamp still burns. */
    static final int KIJK = 60;
    /** The lamps that burn now, per level: chunk -> lamp positions (server thread only). */
    private static final Map<Level, Long2ObjectOpenHashMap<LongOpenHashSet>> LAMPEN = new WeakHashMap<>();

    public VadslampLicht(Properties properties) {
        super(properties);
    }

    // =================================================================================================================
    // the register of burning lamps
    // =================================================================================================================

    static void aan(Level level, BlockPos lamp) {
        LAMPEN.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(ChunkPos.pack(lamp.getX() >> 4, lamp.getZ() >> 4), k -> new LongOpenHashSet()).add(lamp.asLong());
    }

    static void uit(Level level, BlockPos lamp) {
        Long2ObjectOpenHashMap<LongOpenHashSet> perChunk = LAMPEN.get(level);
        if (perChunk == null) {
            return;
        }
        long chunk = ChunkPos.pack(lamp.getX() >> 4, lamp.getZ() >> 4);
        LongOpenHashSet set = perChunk.get(chunk);
        if (set != null && set.remove(lamp.asLong()) && set.isEmpty()) {
            perChunk.remove(chunk);
        }
    }

    /** Is a burning lamp near enough to this spot to own a light here (other than {@code behalve}, may be null)? */
    static boolean lampBij(Level level, BlockPos p, BlockPos behalve) {
        Long2ObjectOpenHashMap<LongOpenHashSet> perChunk = LAMPEN.get(level);
        if (perChunk == null || perChunk.isEmpty()) {
            return false;
        }
        int bereik = VadslampBlockEntity.BEREIK + 1;
        int cx = p.getX() >> 4, cz = p.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LongOpenHashSet set = perChunk.get(ChunkPos.pack(cx + dx, cz + dz));
                if (set == null) {
                    continue;
                }
                for (LongIterator it = set.iterator(); it.hasNext(); ) {
                    long l = it.nextLong();
                    if (behalve != null && l == behalve.asLong()) {
                        continue;
                    }
                    if (Math.abs(BlockPos.getX(l) - p.getX()) <= bereik && Math.abs(BlockPos.getY(l) - p.getY()) <= bereik
                            && Math.abs(BlockPos.getZ(l) - p.getZ()) <= bereik) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** (Tests) how many lamps burn in this level. */
    public static int aantalLampen(Level level) {
        Long2ObjectOpenHashMap<LongOpenHashSet> perChunk = LAMPEN.get(level);
        int n = 0;
        if (perChunk != null) {
            for (LongOpenHashSet s : perChunk.values()) {
                n += s.size();
            }
        }
        return n;
    }

    // =================================================================================================================
    // the block
    // =================================================================================================================

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oud, boolean piston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, KIJK);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        kijk(level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        kijk(level, pos);
    }

    private void kijk(ServerLevel level, BlockPos pos) {
        if (lampBij(level, pos, null)) {
            if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, KIJK);
            }
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
