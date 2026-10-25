package nl.juiced.guhs.feature.techbeurs;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;

/**
 * 1.7.0 (ronde 45-47): {@code guhs:vadsschakelaar}, the Vadsschakelaar: a little lever IN a line of Guhdraad. On
 * ({@link #AAN}): the vadskracht goes through it as through a piece of Guhdraad (every face but the top). Off: it cuts the
 * line, so everything behind it has no vadskracht (asleep). Right-click flips it, for everyone (also in a protected
 * building: the switches of the Guhtechniek-beurs). A knoop without supply or demand ({@link Kern}).
 */
public class VadsschakelaarBlock extends BaseEntityBlock {
    public static final MapCodec<VadsschakelaarBlock> CODEC = simpleCodec(VadsschakelaarBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty AAN = BlockStateProperties.POWERED;
    private static final VoxelShape VORM = Block.box(3, 0, 3, 13, 4, 13);

    public VadsschakelaarBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AAN, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AAN);
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

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            boolean aan = zet(server, pos, !state.getValue(AAN));
            player.sendOverlayMessage(Component.translatable(aan ? "block.guhs.vadsschakelaar.aan" : "block.guhs.vadsschakelaar.uit"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Switches it (code and tests); returns the new state. */
    public static boolean zet(ServerLevel level, BlockPos pos, boolean aan) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(AAN)) {
            return false;
        }
        if (state.getValue(AAN) != aan) {
            level.setBlock(pos, state.setValue(AAN, aan), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, aan ? 0.75f : 0.6f);
            VadsKracht.herbouw(level, pos);
        }
        return aan;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
    }

    /** The switch's knoop: joins the net through every face but the top while it is on. */
    public static class Kern extends BlockEntity implements nl.juiced.guhs.feature.vadskracht.VadsKnoop {
        public Kern(BlockPos pos, BlockState state) {
            super(TechbeursFeature.VADSSCHAKELAAR_BE.get(), pos, state);
        }

        @Override
        public BlockPos vadsPlek() {
            return worldPosition;
        }

        @Override
        public boolean vadsVerbindt(Direction kant) {
            BlockState s = getBlockState();
            return kant != Direction.UP && s.hasProperty(AAN) && s.getValue(AAN);
        }

        @Override
        public void vadsRegels(java.util.function.Consumer<Component> regels) {
            BlockState s = getBlockState();
            regels.accept(Component.translatable(s.hasProperty(AAN) && s.getValue(AAN) ? "block.guhs.vadsschakelaar.aan" : "block.guhs.vadsschakelaar.uit"));
        }
    }
}
