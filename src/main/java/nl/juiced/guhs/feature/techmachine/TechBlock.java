package nl.juiced.guhs.feature.techmachine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * The block of a machine of this slice: a {@link MachineBlock} (facing, snoet, vadskracht, parts, comparator) that opens
 * its screen ({@link MachineMenu}) on a right-click.
 */
public abstract class TechBlock extends MachineBlock {
    protected TechBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof TechBlockEntity machine) {
            machine.open(speler);
        }
        return InteractionResult.SUCCESS;
    }
}
