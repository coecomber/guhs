package nl.juiced.guhs.feature.ringh6;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * bbq2 (ring-h6): the lock of a Rookguh cage on the Frituurberg (block {@code guhs:ringh6_kooislot}; {@link #NR} = which of
 * the three cages). It can't be broken and nothing in the world changes when it is "opened": a click frees that cage's
 * Rookguhje for the player whose story is exactly there ({@link Klim#slot}), so any number of players open the same lock.
 */
public class KooislotBlock extends Block {
    public static final IntegerProperty NR = IntegerProperty.create("nr", 1, 3);

    public KooislotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NR, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NR);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer p) {
            Klim.slot(p, pos, state.getValue(NR));
        }
        return InteractionResult.SUCCESS;
    }
}
