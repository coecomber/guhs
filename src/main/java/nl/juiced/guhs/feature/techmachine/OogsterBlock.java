package nl.juiced.guhs.feature.techmachine;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Oogster: a green guh machine with a straw hat and a reel of blades in front of its snoet. It cuts the ripe plants
 * of the field in front of it and plants them again ({@link OogsterBlockEntity}, {@link Oogst}).
 */
public class OogsterBlock extends TechBlock {
    public static final MapCodec<OogsterBlock> CODEC = simpleCodec(OogsterBlock::new);

    public OogsterBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OogsterBlockEntity(pos, state);
    }
}
