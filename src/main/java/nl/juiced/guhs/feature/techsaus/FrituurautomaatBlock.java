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

/**
 * De Frituurautomaat ({@code guhs:frituurautomaat}): the frying pan that fries by itself, in kaasfrituursaus and on
 * vadskracht ({@link FrituurautomaatBlockEntity}). The frying pan you fill with Mika's vet stays what it was.
 * The sauce stays inside: nobody gets burnt. Resources: tools/features/tech_vloeistof.py.
 */
public class FrituurautomaatBlock extends SausMachineBlock {
    public static final MapCodec<FrituurautomaatBlock> CODEC = simpleCodec(FrituurautomaatBlock::new);

    public FrituurautomaatBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrituurautomaatBlockEntity(pos, state);
    }

    /** While it fries, a little steam rises from the basket. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0 && level.getBlockEntity(pos) instanceof FrituurautomaatBlockEntity automaat && automaat.bezig()) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.95,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03, 0);
        }
    }
}
