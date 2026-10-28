package nl.juiced.guhs.feature.kaaskas;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.techsaus.SausMachineBlock;

/**
 * De Kaaskas ({@code guhs:kaaskas}): the kern of a glass greenhouse. Put it among your crops (inside your own glass kas):
 * while it has vadskracht and kaassaus, the crops around it grow much faster ({@link KaaskasBlockEntity}). Fill it with a
 * bucket of kaassaus or through a Sausslang, like the other sauce machines; no screen.
 */
public class KaaskasBlock extends SausMachineBlock {
    public static final MapCodec<KaaskasBlock> CODEC = simpleCodec(KaaskasBlock::new);

    public KaaskasBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KaaskasBlockEntity(pos, state);
    }
}
