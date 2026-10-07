package nl.juiced.guhs.feature.snuffel;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.network.ModNetworking;

/**
 * De geheugenkaart ({@code guhs:snuffel_geheugenkaart}): the one thing in a dog's pockets, fixed in the last hotbar slot
 * ({@link Hondvorm#geefKaart}); it cannot be thrown away, moved out or taken home, and it does not exist outside the
 * island. Using it opens the small pause menu (client.PauzeScherm): "Opslaan en naar huis" ({@link Reis#naarHuis}: back
 * to exactly where you left, a player with your own things), "Verder spelen", your rank and your good deeds.
 */
public class GeheugenkaartItem extends Item {
    public GeheugenkaartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            open(sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** Opens the pause menu (only for a dog). */
    public static boolean open(ServerPlayer p) {
        if (!Hondvorm.actief(p) || Cutscenes.bezig(p)) {
            return false;
        }
        ModNetworking.sendTo(p, new SnuffelPayloads.Open(SnuffelPayloads.Open.PAUZE, Stand.van(p)));
        return true;
    }

    /** The menu's "Opslaan en naar huis". */
    static boolean naarHuis(ServerPlayer p) {
        if (!Hondvorm.actief(p) || Cutscenes.bezig(p)) {
            return false;
        }
        return Reis.naarHuis(p);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.snuffel_geheugenkaart.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.snuffel_geheugenkaart.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
