package nl.juiced.guhs.feature.einde2.h4vuur;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * einde2 phase B, slice h4_vuur: {@code guhs:guhvatar_beeld}, the reward of the finale: a placeable statue of yourself (your
 * skin, glowing eyes, standing on a round plinth with a little pair of guh ears carved in its front). The block is the
 * plinth; the figure is drawn on it by client.BeeldRenderer with the skin of the player it was made for (the item's
 * {@code minecraft:profile}). {@link #SPIEGEL}: the empty pedestal of the Hall of the Guhvatars shows, for each player who
 * finished the story, a statue with THEIR OWN skin (only they see it: main's Schijn; {@link Beeld}).
 */
public class GuhvatarBeeldBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Shows the skin of whoever looks at it (the Hall's pedestal), not of an owner. */
    public static final BooleanProperty SPIEGEL = BooleanProperty.create("spiegel");
    private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 16, 15);

    public GuhvatarBeeldBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPIEGEL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPIEGEL);
    }

    /** 1.7.0: a structure placed turned or mirrored turns this block along (Block.rotate/mirror keep the state as it is). */
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return nl.juiced.guhs.block.Draaien.facing(state, FACING, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return nl.juiced.guhs.block.Draaien.facing(state, FACING, mirror);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    /** A statue without an owner (from the creative tab) becomes one of whoever places it. */
    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof net.minecraft.world.entity.player.Player speler
                && level.getBlockEntity(pos) instanceof GuhvatarBeeldBlockEntity be && be.eigenaar() == null) {
            be.zetEigenaar(net.minecraft.world.item.component.ResolvableProfile.createResolved(speler.getGameProfile()));
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuhvatarBeeldBlockEntity(pos, state);
    }
}
