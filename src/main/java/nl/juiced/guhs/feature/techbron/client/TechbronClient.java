package nl.juiced.guhs.feature.techbron.client;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbron.TechbronFeature;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * Client side of bbq2 (tech-bronnen): the light show and the turning disc of the Disco-dynamo ({@link DiscoDynamoRenderer}),
 * the bobbing Sausblubje of the Blubkacheltje ({@link BlubkacheltjeRenderer}), the turning star of the Gloeisterkern
 * ({@link GloeisterkernRenderer}), and the lines of text on the five items ({@code block.guhs.<id>.lore}; a Knabbelbatterij
 * also says what it holds).
 */
public final class TechbronClient {
    /** The blocks of this slice whose item gets a line of text. */
    private static final Set<String> LORE = Set.of("knuffelgenerator", "disco_dynamo", "blubkacheltje", "gloeisterkern", "knabbelbatterij");

    public static void init(IEventBus modBus) {
        modBus.addListener(TechbronClient::renderers);
        modBus.addListener(TechbronClient::models);
        NeoForge.EVENT_BUS.addListener(TechbronClient::lore);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TechbronFeature.DISCO_DYNAMO_BE.get(), DiscoDynamoRenderer::new);
        event.registerBlockEntityRenderer(TechbronFeature.BLUBKACHELTJE_BE.get(), BlubkacheltjeRenderer::new);
        event.registerBlockEntityRenderer(TechbronFeature.GLOEISTERKERN_BE.get(), GloeisterkernRenderer::new);
    }

    private static void models(ModelEvent.RegisterStandalone event) {
        DiscoDynamoRenderer.registerModels(event);
        BlubkacheltjeRenderer.registerModels(event);
        GloeisterkernRenderer.registerModels(event);
    }

    private static void lore(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!Guhs.MODID.equals(id.getNamespace()) || !LORE.contains(id.getPath())) {
            return;
        }
        int plek = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(plek, Component.translatable("block.guhs." + id.getPath() + ".lore").withStyle(ChatFormatting.GRAY));
        if (event.getItemStack().is(TechbronFeature.KNABBELBATTERIJ_ITEM.get())) {
            long lading = event.getItemStack().getOrDefault(TechbronFeature.LADING.get(), 0L);
            event.getToolTip().add(plek + 1, Component.translatable("gui.guhs.techbron.batterij.lading", lading, VadsGetallen.BATTERIJ)
                    .withStyle(lading > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
    }

    private TechbronClient() {
    }
}
