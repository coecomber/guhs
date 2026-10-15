package nl.juiced.guhs.feature.einde2.reizen;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2 phase B, slice reizen: the old captain's horn (the reward of the whale questline). Blow it in the Guheinde and your
 * own Leegtewalvisguh comes ({@link Walvis#roep}); sneak + blow: the furniture of its back ({@link Walvis#openInrichting}).
 * Hold it while you stand on its back and it goes where you look ({@link Walvis#stuurt}).
 */
public class WalvishoornItem extends Item {
    /** Ticks between two blows. */
    public static final int WACHT = 80;

    public WalvishoornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer p = (ServerPlayer) player;
        if (!Walvis.heeft(p)) {
            zeg(p, "gui.guhs.einde2_reizen.hoorn.geen");
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            Walvis.openInrichting(p);
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, p.blockPosition(), E2ReizenFeature.HOORN.get(), SoundSource.PLAYERS, 3f, 0.8f + level.getRandom().nextFloat() * 0.1f);
        player.getCooldowns().addCooldown(stack, WACHT);
        if (!GuheindeFeature.isGuheinde(level)) {
            zeg(p, "gui.guhs.einde2_reizen.hoorn.niet_hier");
        } else if (Walvis.roep(p)) {
            zeg(p, "gui.guhs.einde2_reizen.hoorn.komt");
        } else {
            zeg(p, "gui.guhs.einde2_reizen.hoorn.niet_hier");
        }
        return InteractionResult.SUCCESS;
    }

    private static void zeg(ServerPlayer p, String key) {
        p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.einde2_reizen_walvishoorn.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.einde2_reizen_walvishoorn.tooltip").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("item.guhs.einde2_reizen_walvishoorn.tooltip.stuur").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("item.guhs.einde2_reizen_walvishoorn.tooltip.inrichting").withStyle(ChatFormatting.DARK_AQUA));
    }
}
