package nl.juiced.guhs.feature.techmachine.client;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;

/**
 * Client side of bbq2 (tech-machines): the screens ({@link MachineScreen} for every machine, {@link TekentafelScreen}),
 * the moving parts ({@link MachineRenderer}), and the line of text on each machine in your inventory
 * ({@code block.guhs.<id>.lore}: what it does and how much vadskracht it asks).
 */
public final class TechmachineClient {
    /** The blocks of this slice that tell what they do in their tooltip. */
    private static final Set<String> LORE = Set.of("oogster", "knabbelaar", "neerzetter", "knutselmachine", "tekentafel", "plantagebak", "vadsmolen");

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(TechmachineFeature.MACHINE_MENU.get(), MachineScreen::new);
            event.register(TechmachineFeature.TEKENTAFEL_MENU.get(), TekentafelScreen::new);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(TechmachineFeature.OOGSTER_BE.get(), MachineRenderer::new);
            event.registerBlockEntityRenderer(TechmachineFeature.KNABBELAAR_BE.get(), MachineRenderer::new);
            event.registerBlockEntityRenderer(TechmachineFeature.NEERZETTER_BE.get(), MachineRenderer::new);
            event.registerBlockEntityRenderer(TechmachineFeature.KNUTSELMACHINE_BE.get(), MachineRenderer::new);
            event.registerBlockEntityRenderer(TechmachineFeature.PLANTAGEBAK_BE.get(), MachineRenderer::new);
            event.registerBlockEntityRenderer(TechmachineFeature.VADSMOLEN_BE.get(), MachineRenderer::new);
        });
        modBus.addListener((ModelEvent.RegisterStandalone event) -> MachineRenderer.registerModels(event));
        NeoForge.EVENT_BUS.addListener(TechmachineClient::lore);
    }

    private static void lore(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (Guhs.MODID.equals(id.getNamespace()) && LORE.contains(id.getPath())) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    Component.translatable("block.guhs." + id.getPath() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private TechmachineClient() {
    }
}
