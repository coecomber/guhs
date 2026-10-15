package nl.juiced.guhs.feature.techbron;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * De Gloeisterkern: the gloeister of the Aangebrande Mika in a cage of zoutkristal on a guh-faced foot. It costs one
 * gloeister (the recipe) and then gives {@link VadsGetallen#GLOEISTERKERN} VK for ever: no guh, no food, it never runs
 * out. Only one counts per net ({@link BronSoort#GLOEISTERKERN}); a second one sleeps.
 */
public class GloeisterkernBlock extends BronBlock {
    public static final MapCodec<GloeisterkernBlock> CODEC = simpleCodec(GloeisterkernBlock::new);
    private static final VoxelShape VORM = Shapes.or(Block.box(1, 0, 1, 15, 6, 15), Block.box(3, 6, 3, 13, 16, 13));

    public GloeisterkernBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** (block properties) The star shines, a little less while the kern sleeps. */
    public static int licht(BlockState state) {
        return state.getValue(SNOET) == Snoet.SLAAPT ? 7 : 15;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    /** The Gloeisterkern's source: always {@link VadsGetallen#GLOEISTERKERN} VK. */
    public static class Kern extends BronBlockEntity {
        public Kern(BlockPos pos, BlockState state) {
            super(TechbronFeature.GLOEISTERKERN_BE.get(), pos, state);
        }

        @Override
        public BronSoort vadsSoort() {
            return BronSoort.GLOEISTERKERN;
        }

        @Override
        public int vadsAanbod() {
            return VadsGetallen.GLOEISTERKERN;
        }

        @Override
        protected Snoet gezicht() {
            return Snoet.WERKT;
        }

        @Override
        protected void tik(net.minecraft.server.level.ServerLevel level) {
            if (teltMee()) {
                beloon("kern");
            }
        }

        /** Client: the star throws a spark now and then, and chimes very softly (not while the kern sleeps). */
        @Override
        protected void clientTick() {
            if (level == null || getBlockState().getValue(SNOET) == Snoet.SLAAPT) {
                return;
            }
            var r = level.getRandom();
            if (r.nextInt(8) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, worldPosition.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
                        worldPosition.getY() + 0.7 + (r.nextDouble() - 0.5) * 0.3, worldPosition.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
                        (r.nextDouble() - 0.5) * 0.03, 0.02 + r.nextDouble() * 0.02, (r.nextDouble() - 0.5) * 0.03);
            }
            if (r.nextInt(160) == 0) {
                level.playLocalSound(worldPosition, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS,
                        0.3f, 0.8f + r.nextFloat() * 0.6f, false);
            }
        }
    }
}
