package nl.juiced.guhs.feature.einde2.reizen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * einde2 fix4-appa: the Appa-platform block, the middle of a player's own Appa landing place (see {@link AppaPlatform}).
 * Placed: it becomes the placer's platform in that dimension and says what the 5x5 square still misses; a click with an
 * empty hand checks it again (or makes someone else's platform yours too); broken by its owner: forgotten.
 */
public class AppaPlatformBlok extends Block {
    public AppaPlatformBlok(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer p && level instanceof ServerLevel server) {
            AppaPlatform.geplaatst(p, server, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p && level instanceof ServerLevel server) {
            AppaPlatform.klik(p, server, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player instanceof ServerPlayer p && level instanceof ServerLevel server) {
            AppaPlatform.gesloopt(p, server, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
