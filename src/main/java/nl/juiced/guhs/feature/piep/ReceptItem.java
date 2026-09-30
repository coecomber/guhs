package nl.juiced.guhs.feature.piep;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The recipe of the roze guh koek (from the kaasknabbel-nest's treasure). Right-click: you know it now (per player, it
 * stays), and the Knabbeloven of the Knabbelbakkerij can bake it: zoetdeeg + plaatje + glazuur. The paper is used up.
 */
public class ReceptItem extends Item {
    public static final String GELEERD = "ReceptRozeGuhKoek";

    public ReceptItem(Properties properties) {
        super(properties);
    }

    /** Does this player know the recipe? */
    public static boolean kent(ServerPlayer player) {
        return PiepVoortgang.data(player).getBoolean(GELEERD);
    }

    /** Learns the recipe; false when the player already knew it. */
    public static boolean leer(ServerPlayer player) {
        if (kent(player)) {
            return false;
        }
        PiepVoortgang.data(player).putBoolean(GELEERD, true);
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "piep_recept");
        PiepVoortgang.pagina(player, "roze_guh_koek");
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (!leer(sp)) {
            sp.displayClientMessage(Component.translatable("gui.guhs.piep.recept_al").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
        sp.sendSystemMessage(Component.translatable("gui.guhs.piep.recept_geleerd").withStyle(ChatFormatting.LIGHT_PURPLE));
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.roze_guh_koek_recept.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
