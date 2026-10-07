package nl.juiced.guhs.feature.guhpixel.kantoor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;

/**
 * A paper of the Guhkantoor on the wall (loonstrookje, kwartaalrapport, oorkonde): it keeps what is written on it
 * ({@link PapierBlockEntity}; the loot table copies it back onto the item) and a right-click reads it.
 */
public class PapierBlock extends MuurDecoBlock implements EntityBlock {
    public static final VoxelShape STROOKJE = Block.box(5, 2, 15, 11, 14, 16);
    public static final VoxelShape RAPPORT = Block.box(3, 1, 15, 13, 15, 16);
    public static final VoxelShape OORKONDE = Block.box(1, 3, 15, 15, 13, 16);

    public PapierBlock(Properties properties, VoxelShape noord) {
        super(properties, noord);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PapierBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof PapierBlockEntity be) {
            KantoorSlice.papierLezer.accept(be.papier());
        }
        return InteractionResult.SUCCESS;
    }
}
