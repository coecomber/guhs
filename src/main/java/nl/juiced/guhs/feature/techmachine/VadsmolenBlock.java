package nl.juiced.guhs.feature.techmachine;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Vadsmolen: the guh-molentje's big sister, two blocks high, with sails that turn on vadskracht instead of wind
 * ({@link VadsmolenBlockEntity}; the sails are drawn by {@code client.MachineRenderer}).
 */
public class VadsmolenBlock extends TechBlock {
    public static final MapCodec<VadsmolenBlock> CODEC = simpleCodec(VadsmolenBlock::new);

    public VadsmolenBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int hoog() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VadsmolenBlockEntity(pos, state);
    }
}
