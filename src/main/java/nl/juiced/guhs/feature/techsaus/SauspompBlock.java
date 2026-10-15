package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * De Sauspomp ({@code guhs:sauspomp}): stands on a source block of sauce (kaassaus, kaasfrituursaus or water) and, on
 * vadskracht, lifts it into its little tank and pushes it on through the Sausslangen ({@link SauspompBlockEntity}). The source
 * never runs dry. Resources: tools/features/tech_vloeistof.py.
 */
public class SauspompBlock extends SausMachineBlock {
    public static final MapCodec<SauspompBlock> CODEC = simpleCodec(SauspompBlock::new);

    public SauspompBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SauspompBlockEntity(pos, state);
    }

    /** A pump that runs dribbles a little. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(SNOET) == Snoet.WERKT && random.nextInt(4) == 0
                && level.getBlockEntity(pos) instanceof SauspompBlockEntity pomp && pomp.bezig()) {
            // water drips blue, sauce drips like honey
            boolean water = level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER);
            level.addParticle(water ? ParticleTypes.DRIPPING_WATER : ParticleTypes.DRIPPING_HONEY, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.05, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
        }
    }
}
