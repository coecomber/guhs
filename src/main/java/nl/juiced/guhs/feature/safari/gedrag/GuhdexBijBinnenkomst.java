package nl.juiced.guhs.feature.safari.gedrag;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.registry.ModItems;

/**
 * Round 4 (test round 4: "geen Guhdex als de speler binnenkomst? Geef speler een Guhdex"): whoever enters the safari park
 * without a Guhdex in the inventory gets one (the Safari dex lives in a tab of it).
 */
public final class GuhdexBijBinnenkomst {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && Safari.echt(p.level())) {
                geef(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && Safari.echt(p.level())) {
                geef(p);
            }
        });
    }

    /** Gives a Guhdex when the player has none (true when given). */
    public static boolean geef(ServerPlayer p) {
        if (p.getInventory().hasAnyMatching(s -> s.is(ModItems.GUHDEX.get()))) {
            return false;
        }
        ItemStack dex = new ItemStack(ModItems.GUHDEX.get());
        if (!p.getInventory().add(dex)) {
            p.drop(dex, false);
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.guhdex_gekregen").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private GuhdexBijBinnenkomst() {
    }
}
