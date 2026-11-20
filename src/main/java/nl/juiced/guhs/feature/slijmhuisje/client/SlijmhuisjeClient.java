package nl.juiced.guhs.feature.slijmhuisje.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.feature.slijmhuisje.SlijmhuisjeFeature;

/** Client side of the Slijmhuisje: the slime and the slimeballs ({@link SlijmhuisjeRenderer}) and the line on the item. */
public final class SlijmhuisjeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(SlijmhuisjeFeature.SLIJMHUISJE_BE.get(), SlijmhuisjeRenderer::new));
        modBus.addListener((ModelEvent.RegisterStandalone event) -> SlijmhuisjeRenderer.registerModels(event));
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            if (event.getItemStack().is(SlijmhuisjeFeature.SLIJMHUISJE_ITEM.get())) {
                event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                        Component.translatable("block.guhs.slijmhuisje.lore").withStyle(ChatFormatting.GRAY));
            }
        });
    }

    private SlijmhuisjeClient() {
    }
}
