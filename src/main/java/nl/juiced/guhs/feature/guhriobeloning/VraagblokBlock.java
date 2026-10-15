package nl.juiced.guhs.feature.guhriobeloning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The ?-block to build with (a reward of Super Guhrio, sold by Pad-guh): a plain solid block with the look of the ?-block
 * of the levels. Jump against its underside (or click it) and, once per day per player, a kaasknabbel pops out
 * ({@link Vraagblok}). Unlike the ?-block of a level it is an ordinary block: it is drawn like any block, things can hang
 * on it, and it does nothing inside a level.
 */
public class VraagblokBlock extends Block {
    public VraagblokBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            Vraagblok.bots(p, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
