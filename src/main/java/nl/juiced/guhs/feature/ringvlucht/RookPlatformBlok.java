package nl.juiced.guhs.feature.ringvlucht;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
 * 1.7.0: the Rookguh-platform ({@link RookPlatform}): put down, it is your Rookguh's landing place (one per player, in any
 * dimension: the newest counts). A click on your own one calls the Rookguh (its flight screen); a click on another one makes
 * that one yours. Broken by you while it is yours: forgotten.
 */
public class RookPlatformBlok extends Block {
    public RookPlatformBlok(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer p && level instanceof ServerLevel server) {
            neem(p, server, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p && level instanceof ServerLevel server) {
            if (!RookPlatform.isVan(p, server, pos)) {
                neem(p, server, pos);
            } else if (RookPlatform.mag(p)) {
                RookPlatform.open(p);
            } else {
                p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.platform.te_vroeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** This block becomes the player's platform (when they may have one). */
    static void neem(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (!RookPlatform.mag(p)) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.platform.te_vroeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        boolean verhuisd = RookPlatform.van(p) != null;
        RookPlatform.zet(p, level.dimension(), pos);
        p.sendSystemMessage(Component.translatable(verhuisd ? "quest.guhs.ringvlucht.platform.verhuisd" : "quest.guhs.ringvlucht.platform.neergezet")
                .withStyle(ChatFormatting.AQUA));
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.6, 0.2, 0.6, 0.01);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player instanceof ServerPlayer p && level instanceof ServerLevel server && RookPlatform.isVan(p, server, pos)) {
            RookPlatform.vergeet(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.platform.opgeruimd").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
