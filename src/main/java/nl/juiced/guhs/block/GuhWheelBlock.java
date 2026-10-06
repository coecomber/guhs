package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Big pink guh running wheel (3x3 blocks; the other 8 blocks are invisible GuhWheelPartBlocks). Pick up your tamed guh (sneak + right-click it) and right-click the wheel with it to put
 * it in; right-click the running wheel to get your guh back (as the picked-up guh item).
 * bbq2: while a guh runs, the wheel gives vadskracht (the block entity is the source, see GuhWheelBlockEntity); Guhdraad
 * carries it to the machines, and a machine right next to any of the wheel's 3x3 blocks gets it too. The wheel itself is
 * still a full-strength (15) redstone source while a guh runs, as it always was.
 */
public class GuhWheelBlock extends BaseEntityBlock {
    public static final MapCodec<GuhWheelBlock> CODEC = simpleCodec(GuhWheelBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public GuhWheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    /** The big wheel is 3 wide (along the wheel) and 3 tall; the wheel block itself is the bottom middle. */
    public static java.util.List<BlockPos> partPositions(BlockPos pos, Direction facing) {
        java.util.List<BlockPos> parts = new java.util.ArrayList<>();
        Direction along = GuhWheelPartBlock.along(facing);
        for (int side = 0; side <= 2; side++) {
            for (int height = 0; height <= 2; height++) {
                if (side != 1 || height != 0) {
                    parts.add(pos.relative(along, side - 1).above(height));
                }
            }
        }
        return parts;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos part : partPositions(context.getClickedPos(), facing)) {
            if (!context.getLevel().getBlockState(part).canBeReplaced(context)) {
                return null; // not enough room for the big wheel
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction facing = state.getValue(FACING);
        Direction along = GuhWheelPartBlock.along(facing);
        for (BlockPos part : partPositions(pos, facing)) {
            int side = part.get(along.getAxis()) - pos.get(along.getAxis());
            side = along.getAxisDirection() == Direction.AxisDirection.POSITIVE ? side : -side;
            level.setBlock(part, ModBlocks.GUH_WHEEL_PART.get().defaultBlockState()
                    .setValue(GuhWheelPartBlock.FACING, facing)
                    .setValue(GuhWheelPartBlock.SIDE, side + 1)
                    .setValue(GuhWheelPartBlock.HEIGHT, part.getY() - pos.getY()), 3);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // drawn (big) by GuhWheelRenderer
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuhWheelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ModBlockEntities.GUH_WHEEL.get(), GuhWheelBlockEntity::clientTick) : null;
    }

    /** Right-click with a picked-up guh: in it goes. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.PICKED_UP_GUH.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof GuhWheelBlockEntity wheel)) {
            return InteractionResult.SUCCESS;
        }
        if (wheel.hasGuh()) {
            player.sendOverlayMessage(Component.translatable("block.guhs.guh_wheel.occupied"));
            return InteractionResult.CONSUME;
        }
        wheel.insert(PickedUpGuhItem.guhData(stack));
        nl.juiced.guhs.feature.band.GuhVolger.item(stack, nl.juiced.guhs.feature.band.PlekSoort.GUHWIEL, level.dimension(), pos, "",
                level.getGameTime());   // 2.10: "waar is mijn guh": running in the wheel
        stack.consume(1, player);
        level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, wheel.isBlij() ? 1.25f : 1f);
        level.setBlock(pos, state.setValue(RUNNING, true), 3);
        // tech-bronnen: what this guh gives, said at once (a happy guh runs extra hard)
        player.sendOverlayMessage(Component.translatable(wheel.isBlij() ? "gui.guhs.techbron.guhrad.rent_blij" : "gui.guhs.techbron.guhrad.rent",
                wheel.vadsAanbod()));
        return InteractionResult.SUCCESS;
    }

    /** Right-click the running wheel: you get your guh back (as an item). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof GuhWheelBlockEntity wheel)) {
            return InteractionResult.SUCCESS;
        }
        if (!wheel.hasGuh()) {
            player.sendOverlayMessage(Component.translatable("block.guhs.guh_wheel.no_guh"));
            return InteractionResult.CONSUME;
        }
        if (!player.getUUID().equals(wheel.getGuhOwner()) && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.translatable("block.guhs.guh_wheel.not_yours"));
            return InteractionResult.CONSUME;
        }
        CompoundTag guh = wheel.takeOut();
        if (guh != null) {
            ItemStack item = PickedUpGuhItem.of(guh);
            nl.juiced.guhs.feature.band.GuhVolger.inZakken(item, player);   // 2.10: "waar is mijn guh": back in your pockets
            player.getInventory().placeItemBackInInventory(item);
        }
        level.setBlock(pos, state.setValue(RUNNING, false), 3);
        return InteractionResult.SUCCESS;
    }

    // Breaking the wheel: see GuhWheelBlockEntity#preRemoveSideEffects (26.1: onRemove is gone)

    // --- vadskracht: the nets around the wheel are rebuilt when it appears or disappears ---

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            nl.juiced.guhs.feature.vadskracht.VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        nl.juiced.guhs.feature.vadskracht.VadsKracht.veranderd(level, pos);
    }

    // --- redstone: full power in every direction while a guh is running ---

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(RUNNING) ? 15 : 0;
    }
}
