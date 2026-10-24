package nl.juiced.guhs.feature.mijnhulp.client;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.mijnhulp.Wolkenliftje;

/**
 * 1.7.0 mining helpers, client side: the Wolkenliftje's keys (a jump on one = up, starting to sneak on one = down; the
 * server checks it all again, {@link Wolkenliftje.Rit}) and the little tooltip lines of the new items.
 */
public final class MijnhulpClient {
    /** The items with a {@code .lore} line (the Vadsknakker writes its own). */
    private static final Set<String> LORE = Set.of("vahoegietstaaf", "super_vads_sjabloon", "wolkenliftje", "pluisladder", "terrariumbodem",
            "mijnlampje", "super_vahoege_vads_sword", "super_vahoege_vads_pickaxe", "super_vahoege_vads_axe", "super_vahoege_vads_shovel",
            "super_vahoege_vads_hoe", "super_vahoege_vads_paxel", "super_vahoege_vads_helmet", "super_vahoege_vads_chestplate",
            "super_vahoege_vads_leggings", "super_vahoege_vads_boots");
    private static boolean sprong, sloop;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(MijnhulpClient::tick);
        NeoForge.EVENT_BUS.addListener(MijnhulpClient::lore);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || p.isSpectator()) {
            sprong = sloop = false;
            return;
        }
        boolean springt = p.input.keyPresses.jump(), sluipt = p.input.keyPresses.shift();
        if (springt && !sprong && Wolkenliftje.onder(p) != null) {
            ClientPacketDistributor.sendToServer(new Wolkenliftje.Rit(true));
        }
        if (sluipt && !sloop && p.onGround() && Wolkenliftje.onder(p) != null) {
            ClientPacketDistributor.sendToServer(new Wolkenliftje.Rit(false));
        }
        sprong = springt;
        sloop = sluipt;
    }

    private static void lore(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!Guhs.MODID.equals(id.getNamespace()) || !LORE.contains(id.getPath())) {
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

    private MijnhulpClient() {
    }
}
