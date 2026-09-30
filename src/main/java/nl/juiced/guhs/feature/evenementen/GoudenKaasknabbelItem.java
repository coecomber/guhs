package nl.juiced.guhs.feature.evenementen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import nl.juiced.guhs.entity.GuhEntity;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The golden kaasknabbel, only from the kaasregen: a wild guh can't resist it (tamed at once), and it's a tasty
 * golden snack for yourself too (as filling as a golden carrot, no magic).
 */
public class GoudenKaasknabbelItem extends Item {
    public GoudenKaasknabbelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof GuhEntity guh) || !Evenementen.wild(guh)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            stack.consume(1, player);
            ((ServerLevel) guh.level()).sendParticles(ParticleTypes.WAX_OFF, guh.getX(), guh.getY() + guh.getBbHeight(), guh.getZ(),
                    12, 0.4, 0.3, 0.4, 0.1);
            Evenementen.tameNow(guh, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.gouden_kaasknabbel.lore").withStyle(ChatFormatting.GOLD));
    }
}
