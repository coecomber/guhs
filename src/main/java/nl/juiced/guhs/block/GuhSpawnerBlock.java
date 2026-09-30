package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.block.entity.GuhSpawnerBlockEntity;
import nl.juiced.guhs.registry.ModBlockEntities;

/**
 * Guh spawner, found in hamster houses. Unlike vanilla spawners it can be mined with a pickaxe
 * (it drops itself), so you can move it to your own guh farm.
 */
public class GuhSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<GuhSpawnerBlock> CODEC = simpleCodec(GuhSpawnerBlock::new);

    public GuhSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuhSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GUH_SPAWNER.get(),
                level.isClientSide ? GuhSpawnerBlockEntity::clientTick : GuhSpawnerBlockEntity::serverTick);
    }
}
