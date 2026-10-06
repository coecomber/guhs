package nl.juiced.guhs.feature.bestaand.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * bbq2 (bestaand), client side: the two NPCs' own models (the Wachter-guh: a dark iron helmet with a saté plume, a
 * breastplate with a guh face, a grill skewer as his halberd; the Knuffelmaker-guh: a pincushion on his head, little round
 * glasses, a measuring tape, an apron and a half-finished plush under his arm; geckolib/models/entity/guh_npc_*.geo.json
 * from tools/features/bestaand.py) and the tooltip line of this slice's items (item.guhs.bestaand_*.lore).
 * The blocks need nothing here: their models carry their render type, and what a player sees for themselves (a burning
 * fire bowl, a weed) is an ordinary block state on the client.
 */
public final class BestaandClient {
    public static void init(IEventBus modBus) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.WACHTERGUH, Guhs.id("entity/guh_npc_wachterguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.KNUFFELMAKERGUH, Guhs.id("entity/guh_npc_knuffelmakerguh"));
        NeoForge.EVENT_BUS.addListener(BestaandClient::lore);
    }

    /** The items of this slice get their little line of text, when they have one. */
    private static void lore(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!Guhs.MODID.equals(id.getNamespace()) || !id.getPath().startsWith("bestaand_")) {
            return;
        }
        String key = "item.guhs." + id.getPath() + ".lore";
        if (!I18n.exists(key)) {
            key = "block.guhs." + id.getPath() + ".lore";
        }
        if (I18n.exists(key)) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()), Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    private BestaandClient() {
    }
}
