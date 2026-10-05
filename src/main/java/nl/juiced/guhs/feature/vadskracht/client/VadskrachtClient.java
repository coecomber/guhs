package nl.juiced.guhs.feature.vadskracht.client;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadsPayloads;

/**
 * Client side of the vadskracht (bbq2): the hover readout under the crosshair ({@link VadsHover}, GUI layer
 * {@code guhs:vadskracht_hover}) and the line of text on the Guhrad and the Guhdraad in your inventory
 * ({@code block.guhs.<id>.lore}).
 */
public final class VadskrachtClient {
    /** Old blocks (plain block items) that get a line of text about vadskracht. */
    private static final Set<String> LORE = Set.of("guh_wheel", "guh_wire");

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("vadskracht_hover"), VadsHover::extractRenderState));
        VadsPayloads.ontvanger = VadsHover::ontvang;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> VadsHover.tick());
        NeoForge.EVENT_BUS.addListener(VadskrachtClient::lore);
    }

    /**
     * The front texture of a machine made with tools/features/vadskracht.py {@code machine(h, name, ...)} for this face:
     * {@code guhs:textures/block/<name>_voor_<slaapt|werkt|vol>.png}. For a machine that draws itself (a block entity
     * renderer, GeckoLib) and wants the same three faces as the block-model machines.
     */
    public static Identifier snoetTextuur(String name, nl.juiced.guhs.feature.vadskracht.Snoet snoet) {
        return Guhs.id("textures/block/" + name + "_voor_" + snoet.getSerializedName() + ".png");
    }

    private static void lore(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (Guhs.MODID.equals(id.getNamespace()) && LORE.contains(id.getPath())) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    Component.translatable("block.guhs." + id.getPath() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private VadskrachtClient() {
    }
}
