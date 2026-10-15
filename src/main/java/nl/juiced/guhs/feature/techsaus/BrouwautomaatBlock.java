package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;

/**
 * De Brouwautomaat ({@code guhs:brouwautomaat}): the Guhbrouwketel that brews by itself, on vadskracht instead of
 * grillspiespoeder ({@link BrouwautomaatBlockEntity}). The Guhbrouwketel you stir by hand stays what it was.
 * Resources: tools/features/tech_vloeistof.py.
 */
public class BrouwautomaatBlock extends SausMachineBlock {
    public static final MapCodec<BrouwautomaatBlock> CODEC = simpleCodec(BrouwautomaatBlock::new);

    public BrouwautomaatBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrouwautomaatBlockEntity(pos, state);
    }

    /** While it brews, the pan bubbles in the colour of the brew (like the Guhbrouwketel). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BrouwautomaatBlockEntity automaat) || automaat.brouwsel() == null) {
            return;
        }
        Brouwsel brouwsel = automaat.brouwsel();
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        level.addParticle(new DustParticleOptions(brouwsel.colour & 0xFFFFFF, 1.0f), x + (random.nextDouble() - 0.5) * 0.6, y,
                z + (random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0, 0.04, 0);
        }
    }
}
