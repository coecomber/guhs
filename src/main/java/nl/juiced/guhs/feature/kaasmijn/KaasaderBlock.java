package nl.juiced.guhs.feature.kaasmijn;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3f;

/**
 * A cheese vein in the kaasmijn (stone, deepslate or gold). Mine it with any pickaxe for kaasbrokken (gold: goudkaas).
 * A mined vein doesn't leave a hole: it becomes a {@link MinedOut} vein that grows back by itself after a few minutes,
 * so the mine never runs dry and its floors and walls stay whole. (No silk touch: a vein always drops its cheese.)
 */
public class KaasaderBlock extends DropExperienceBlock {
    private final Supplier<? extends Block> minedOut;

    public KaasaderBlock(IntProvider xp, Supplier<? extends Block> minedOut, Properties properties) {
        super(xp, properties);
        this.minedOut = minedOut;
    }

    /** Mined by a player: the mined-out vein takes its place (the drops still come from the full vein). */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.item.ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        return level.setBlock(pos, minedOut.get().defaultBlockState(), level.isClientSide() ? 11 : 3);
    }

    /** What's left of a vein: plain rock with crumbs of cheese. Grows back on random ticks (about 1 in 5 per tick, times slowness). */
    public static class MinedOut extends Block {
        public static final MapCodec<MinedOut> CODEC = simpleCodec(p -> new MinedOut(() -> net.minecraft.world.level.block.Blocks.STONE, 1, p));
        public static final int REGROW_CHANCE = 5;
        private final Supplier<? extends Block> vein;
        private final int slowness;

        public MinedOut(Supplier<? extends Block> vein, int slowness, Properties properties) {
            super(properties);
            this.vein = vein;
            this.slowness = slowness;
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(REGROW_CHANCE * slowness) == 0) {
                regrow(level, pos);
            }
        }

        /** The cheese is back. */
        public void regrow(ServerLevel level, BlockPos pos) {
            level.setBlock(pos, vein.get().defaultBlockState(), 3);
            level.sendParticles(new DustParticleOptions(0xFFCC40 /* 1, 0.8, 0.25 */, 1.1f), pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
    }
}
