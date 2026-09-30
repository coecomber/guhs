package nl.juiced.guhs.feature.landdiertjes;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A shuckle-plekje (CONTRACT_30 §4.10): an invisible, no-collision marker the mewtwo slice puts (5-8 of them) on the rocky
 * coast of the kloon-eiland. It keeps one or two Sjokkels around: on a random tick, when there are fewer than {@link #MAX}
 * Sjokkels within {@link #BEREIK} blocks (tame ones count too), a wild Sjokkel crawls out from between the rocks next to it,
 * but never where a player within {@link #NIET_BIJ_SPELER} blocks could see it pop up. The Sjokkel stays around its plekje.
 */
public class ShucklePlekjeBlock extends Block {
    public static final int MAX = 2, BEREIK = 16, NIET_BIJ_SPELER = 8;

    public ShucklePlekjeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            probeer(level, pos, random);
        }
    }

    /** How many Sjokkels are around this plekje. */
    public static int aantal(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ShuckleEntity.class, new AABB(pos).inflate(BEREIK), e -> e.isAlive()).size();
    }

    /** One try: a Sjokkel crawls out when there is room for one more (and nobody is watching). Returns it, or null. */
    @Nullable
    public static ShuckleEntity probeer(ServerLevel level, BlockPos pos, RandomSource random) {
        int n = aantal(level, pos);
        if (n >= MAX || n == 1 && random.nextInt(3) != 0) {
            return null;
        }
        BlockPos plek = zoekPlek(level, pos, random);
        if (plek == null) {
            return null;
        }
        List<net.minecraft.server.level.ServerPlayer> spelers = level.getPlayers(p -> !p.isSpectator()
                && p.distanceToSqr(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5) < NIET_BIJ_SPELER * NIET_BIJ_SPELER);
        if (!spelers.isEmpty()) {
            return null;
        }
        ShuckleEntity shuckle = LanddiertjesFeature.SHUCKLE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (shuckle == null) {
            return null;
        }
        shuckle.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, random.nextFloat() * 360f, 0);
        shuckle.finalizeSpawn(level, level.getCurrentDifficultyAt(plek), EntitySpawnReason.STRUCTURE, null);
        shuckle.thuisBij(pos);
        shuckle.setPersistenceRequired();
        level.addFreshEntity(shuckle);
        return shuckle;
    }

    /** A free spot next to the plekje (the plekje itself first) with sturdy ground under it and room for a Sjokkel. */
    @Nullable
    static BlockPos zoekPlek(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 12; i++) {
            BlockPos p = i == 0 ? pos : pos.offset(random.nextInt(7) - 3, random.nextInt(5) - 2, random.nextInt(7) - 3);
            if (vrij(level, p) && vrij(level, p.above()) && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                    && level.getFluidState(p).isEmpty()) {
                return p;
            }
        }
        return null;
    }

    private static boolean vrij(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty();
    }
}
