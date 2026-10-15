package nl.juiced.guhs.feature.techbron;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Meerblok;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * The block of a vadskracht source of tech-bronnen (Knuffelgenerator, Disco-dynamo, Blubkacheltje, Gloeisterkern): what
 * {@link MachineBlock} is for a machine. It faces the one who places it ({@link #FACING}), shows its face ({@link #SNOET}:
 * asleep while it gives nothing, happy while it gives, surprised when it is full; set by {@link BronBlockEntity}), ticks
 * its block entity, and tells the vadskracht nets when it appears and disappears. A source bigger than one block
 * overrides {@link #breed}, {@link #hoog}, {@link #diep} and {@link #deel} ({@link Meerblok}): its parts come and go with it.
 */
public abstract class BronBlock extends BaseEntityBlock implements Meerblok.Vorm {
    public static final EnumProperty<Direction> FACING = MachineBlock.FACING;
    public static final EnumProperty<Snoet> SNOET = MachineBlock.SNOET;

    protected BronBlock(Properties p) {
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

    /** The part block of a source bigger than one block. */
    public Block deel() {
        return VadskrachtFeature.MACHINE_DEEL.get();
    }

    private boolean isGroot() {
        return breed() * hoog() * diep() > 1;
    }

    /** The box of all blocks of the source whose kern stands here (one block high per layer). */
    public static AABB vloer(BlockPos kern, BlockState state) {
        AABB box = new AABB(kern);
        for (BlockPos deel : Meerblok.delen(kern, state)) {
            box = box.minmax(new AABB(deel));
        }
        return box;
    }

    // --- placing and removing ---

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (isGroot()) {
            for (BlockPos plek : Meerblok.delen(context.getClickedPos(), state)) {
                if (context.getLevel().isOutsideBuildHeight(plek) || !context.getLevel().getBlockState(plek).canBeReplaced(context)) {
                    return null;   // not enough room for the whole thing
                }
            }
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            if (isGroot() && !level.isClientSide()) {
                Meerblok.plaats(level, pos, state, deel());   // (however the kern got here: its parts come with it)
            }
            VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
    }

    // --- looks and ticking ---

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
                    if (be instanceof BronBlockEntity bron) {
                        bron.clientTick();
                    }
                }
                : (l, pos, s, be) -> {
                    if (be instanceof BronBlockEntity bron) {
                        bron.serverTick();
                    }
                };
    }
}
