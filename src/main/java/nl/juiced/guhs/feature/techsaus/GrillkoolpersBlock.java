package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * De Grillkoolpers ({@code guhs:grillkoolpers}): two blocks high. A bucket of kaasfrituursaus and a bucket of water, squeezed
 * together on vadskracht, come out as a block of grillkool ({@link GrillkoolpersBlockEntity}): what happens when the two
 * meet in the world, but in a machine and without burnt paws. {@link #PERST}: the stamp is down.
 * Resources: tools/features/tech_vloeistof.py (the kern's model draws both blocks; the upper one is a {@code machine_deel}).
 */
public class GrillkoolpersBlock extends SausMachineBlock {
    public static final MapCodec<GrillkoolpersBlock> CODEC = simpleCodec(GrillkoolpersBlock::new);
    public static final BooleanProperty PERST = BooleanProperty.create("perst");

    public GrillkoolpersBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(PERST, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PERST);
    }

    @Override
    public int hoog() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrillkoolpersBlockEntity(pos, state);
    }

    /** While the stamp is down it hisses: steam from between the plates. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PERST)) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.95,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
