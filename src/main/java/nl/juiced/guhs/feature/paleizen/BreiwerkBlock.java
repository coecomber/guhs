package nl.juiced.guhs.feature.paleizen;

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

/**
 * Mika-oma's knitting basket (bbq2): it blew off her gallery and landed on the roof garden of the low block of the
 * Mika-woonblokken. Right-click it on the right step of her questline and you take the knitting out ({@link OmaQuest#breiwerk});
 * the basket itself stays, so every player finds it (it can't be broken in survival and has no item).
 */
public class BreiwerkBlock extends Block {
    public static final MapCodec<BreiwerkBlock> CODEC = simpleCodec(BreiwerkBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 7, 13);

    public BreiwerkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer p) {
            OmaQuest.breiwerk(p, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
