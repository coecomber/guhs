package nl.juiced.guhs.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModParticles;

/** The guh blossom tree: leaves that drop tiny guhs (like cherry leaves) and its sapling. */
public final class GuhBloesemBlocks {
    public static final TreeGrower GROWER = new TreeGrower("guhs:guhbloesem", Optional.empty(),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("guhbloesem"))), Optional.empty());

    /** Guh blossom: now and then a tiny guh floats down from it. */
    public static class Leaves extends LeavesBlock {
        public static final com.mojang.serialization.MapCodec<Leaves> CODEC = simpleCodec(Leaves::new);

        public Leaves(Properties properties) {
            // 26.1: the falling-leaves particle is LeavesBlock's own now; 0.1 = the old 1-in-10 per animateTick
            super(0.1f, properties);
        }

        @Override
        public com.mojang.serialization.MapCodec<Leaves> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ModParticles.GUH_BLAADJE.get());
        }
    }

    /** Grows into a guh blossom tree; happy on grass, dirt and the Guhmension's wool and kaasknabbels. */
    public static class Sapling extends SaplingBlock {
        public Sapling(Properties properties) {
            super(GROWER, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || state.isFaceSturdy(level, pos, Direction.UP);
        }
    }

    private GuhBloesemBlocks() {
    }
}
