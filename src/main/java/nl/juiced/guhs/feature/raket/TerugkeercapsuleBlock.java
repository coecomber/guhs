package nl.juiced.guhs.feature.raket;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The return capsule in the moon base (unbreakable): click it and you are home again ({@link KaasMaan#terug}). */
public class TerugkeercapsuleBlock extends Block {
    public static final MapCodec<TerugkeercapsuleBlock> CODEC = simpleCodec(TerugkeercapsuleBlock::new);
    private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 15, 14);

    public TerugkeercapsuleBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            KaasMaan.terug(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
