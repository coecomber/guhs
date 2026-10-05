package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * The block of a machine that holds sauce (the Sauspomp, the Brouwautomaat, the Frituurautomaat, the Grillkoolpers): a
 * {@link MachineBlock} (vadskracht, the face, the owner, the parts) that also
 * <ul>
 *   <li>ticks {@link SausMachineBlockEntity#sausTick} (slurping and pushing through the hoses) before the machine's own tick;</li>
 *   <li>is worked by hand, like the Guhbrouwketel and the frying pan: no screen, only right-clicks
 *       ({@link SausMachineBlockEntity#klik} / {@link SausMachineBlockEntity#klikLeeg});</li>
 *   <li>tells the hose nets when it comes and goes ({@link Slangen#veranderd}).</li>
 * </ul>
 */
public abstract class SausMachineBlock extends MachineBlock {
    protected SausMachineBlock(Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> machine = super.getTicker(level, state, type);
        if (level.isClientSide() || machine == null) {
            return machine;
        }
        return (l, pos, s, be) -> {
            if (be instanceof SausMachineBlockEntity saus) {
                saus.sausTick();
            }
            machine.tick(l, pos, s, be);
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Slangen.veranderd(level);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Slangen.veranderd(level);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof SausMachineBlockEntity machine)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return machine.wil(stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return player instanceof ServerPlayer sp && machine.klik(sp, hand) ? InteractionResult.SUCCESS_SERVER : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SausMachineBlockEntity machine) {
            machine.klikLeeg(sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** A comparator reads how full the tanks are (0 empty .. 15 full); a machine with an output shows the fuller of the two. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        int items = super.getAnalogOutputSignal(state, level, pos, direction);
        return level.getBlockEntity(pos) instanceof SausMachineBlockEntity machine ? Math.max(items, machine.tankSignaal()) : items;
    }
}
