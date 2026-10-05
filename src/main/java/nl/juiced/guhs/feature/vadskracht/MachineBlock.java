package nl.juiced.guhs.feature.vadskracht;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;

/**
 * The block of a guh machine (the base class of every machine that uses vadskracht): it faces the one who places it
 * ({@link #FACING}), shows its face ({@link #SNOET}: asleep / happy / surprised, set by {@link MachineBlockEntity}), ticks its
 * block entity on the server (and {@link MachineBlockEntity#clientTick} on the client), remembers who placed it, tells the
 * vadskracht nets when it appears and disappears, and gives a comparator how full it is. The items in it drop when it is
 * removed ({@link MachineBlockEntity#preRemoveSideEffects}).
 * <p>
 * A machine bigger than one block overrides {@link #breed}, {@link #hoog}, {@link #diep} ({@link Meerblok}): the part
 * blocks ({@link #deel}) are placed and removed with it, and it only fits where the whole box is free.
 * <p>
 * A subclass gives: {@code codec()}, {@code newBlockEntity} (a {@link MachineBlockEntity}), and registers the capabilities
 * of its block entity type with {@link VadskrachtFeature#machineCapabilities}. Resources: tools/features/vadskracht.py
 * {@code machine(h, name, ...)}.
 */
public abstract class MachineBlock extends BaseEntityBlock implements Meerblok.Vorm {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Snoet> SNOET = EnumProperty.create("snoet", Snoet.class);

    protected MachineBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SNOET, Snoet.SLAAPT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SNOET);
    }

    // --- the size (Meerblok.Vorm): one block unless a subclass says otherwise ---

    @Override
    public int breed() {
        return 1;
    }

    @Override
    public int hoog() {
        return 1;
    }

    @Override
    public int diep() {
        return 1;
    }

    /** The part block of a machine bigger than one block. */
    protected Block deel() {
        return VadskrachtFeature.MACHINE_DEEL.get();
    }

    private boolean isGroot() {
        return breed() * hoog() * diep() > 1;
    }

    // --- placing and removing ---

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (isGroot()) {
            for (BlockPos plek : Meerblok.delen(context.getClickedPos(), state)) {
                if (context.getLevel().isOutsideBuildHeight(plek) || !context.getLevel().getBlockState(plek).canBeReplaced(context)) {
                    return null;   // not enough room for the whole machine
                }
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.zetEigenaar(player.getUUID());
        }
        if (isGroot() && !level.isClientSide()) {
            Meerblok.plaats(level, pos, state, deel());
        }
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
        level.updateNeighbourForOutputSignal(pos, this);
    }

    // --- looks, ticking, comparator ---

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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? (l, pos, s, be) -> {
                    if (be instanceof MachineBlockEntity machine) {
                        machine.clientTick();
                    }
                }
                : (l, pos, s, be) -> {
                    if (be instanceof MachineBlockEntity machine) {
                        machine.serverTick();
                    }
                };
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full the machine is (like a chest: 0 empty .. 15 every slot full). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity machine
                ? ResourceHandlerUtil.getRedstoneSignalFromResourceHandler(machine.vakken()) : 0;
    }
}
