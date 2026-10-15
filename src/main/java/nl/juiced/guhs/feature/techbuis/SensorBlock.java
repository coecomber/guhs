package nl.juiced.guhs.feature.techbuis;

import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * The block of the four sensors (Voorraadmeter, Snuffelsensor, Guhklok, Guhteller): a little guh machine
 * ({@link MachineBlock}: it faces you, has a face, needs {@code VadsGetallen.SENSOR} vadskracht) that gives a redstone
 * signal ({@link #SIGNAAL}: 15 to every side, or 0) and tells a comparator more ({@link SensorBlockEntity#sterkte}). The
 * face is surprised while the signal is on. Which sensor it is, is decided by the block entity it makes. Use it to set it
 * ({@link SensorBlockEntity#klik}); what it measures is in the hover readout, like the vadskracht.
 */
public class SensorBlock extends MachineBlock {
    public static final BooleanProperty SIGNAAL = BooleanProperty.create("signaal");

    private final BiFunction<BlockPos, BlockState, ? extends SensorBlockEntity> maker;
    private final boolean achterDicht;
    private final MapCodec<SensorBlock> codec;

    /**
     * @param maker       makes the block entity
     * @param achterDicht true: no signal comes out of the back (the Guhteller listens there)
     */
    public SensorBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends SensorBlockEntity> maker, boolean achterDicht) {
        super(properties);
        this.maker = maker;
        this.achterDicht = achterDicht;
        this.codec = simpleCodec(p -> new SensorBlock(p, maker, achterDicht));
        registerDefaultState(defaultBlockState().setValue(SIGNAAL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIGNAAL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return maker.apply(pos, state);
    }

    /** The machine's own tick, and after it the sensor's (it must also notice that its vadskracht is gone). */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> machine = super.getTicker(level, state, type);
        if (level.isClientSide() || machine == null) {
            return machine;
        }
        return (l, pos, s, be) -> {
            machine.tick(l, pos, s, be);
            if (be instanceof SensorBlockEntity sensor) {
                sensor.naTick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SensorBlockEntity sensor) {
            sensor.klik(sp, player.isShiftKeyDown());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    // --- redstone out ---

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** (direction: from the block that asks towards this one.) */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(SIGNAAL) || (achterDicht && direction == state.getValue(FACING))) {
            return 0;
        }
        return 15;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SensorBlockEntity sensor ? sensor.sterkte() : 0;
    }
}
