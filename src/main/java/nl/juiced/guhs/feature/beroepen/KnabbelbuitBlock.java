package nl.juiced.guhs.feature.beroepen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * The town's stolen knabbel stock: a sack full of kaasknabbels with a pink Mika paw on it, knabbels spilling out. In
 * Inspecteur Vahoegsma's case, right-click it to take it back ({@link Politie#gevonden}).
 */
public class KnabbelbuitBlock extends Block {
    public static final MapCodec<KnabbelbuitBlock> CODEC = simpleCodec(KnabbelbuitBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public KnabbelbuitBlock(Properties properties) {
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
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Politie.gevonden((ServerLevel) level, pos, sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
