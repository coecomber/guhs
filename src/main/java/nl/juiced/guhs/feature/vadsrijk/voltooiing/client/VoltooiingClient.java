package nl.juiced.guhs.feature.vadsrijk.voltooiing.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.knus.client.KnusToast;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.VoltooiingPayloads;

/**
 * I-VOLTOOIING, client: the book key {@link #BOEK} (Vadsrijk category, rebindable, default B; in the dimension it wins over other
 * Guhs keys on the same key, e.g. the snuffel "blaf" on B), the book screen, the milestone toasts and the collection tabs.
 */
public final class VoltooiingClient {
    /** Its id in CollectieTabs. */
    public static final String TAB = "voltooiing";
    public static final KeyMapping BOEK = new KeyMapping("key.guhs.vr_boek", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KernClient.CATEGORIE);

    private VoltooiingClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(BOEK));
        VoltooiingPayloads.boekOntvanger = b -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof BoekScherm s) {
                s.update(b.data(), java.util.List.of());
            } else {
                mc.setScreen(new BoekScherm(b.data()));
            }
        });
        VoltooiingPayloads.toastOntvanger = t -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().getToastManager().addToast(
                new KnusToast(Component.translatable("vadsrijk.voltooiing.toast.titel", t.procent()),
                        Component.translatable("vadsrijk.voltooiing.toast.tekst", Component.translatable(t.zoneKey())), new ItemStack(Items.WRITABLE_BOOK))));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !Vadsrijk.in(mc.player)) {
                return;
            }
            for (KeyMapping k : mc.options.keyMappings) {
                if (k != BOEK && k.getCategory() == nl.juiced.guhs.client.GuhKeys.CATEGORY && k.same(BOEK)) {
                    while (k.consumeClick()) {
                        // (swallowed)
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null;
            while (BOEK.consumeClick()) {
                if (binnen) {
                    open();
                }
            }
        });
        // the book's tab next to Rijdieren (10) and Huisdieren (20) on top of the collection screens
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(TAB, 30, () -> Component.translatable("vadsrijk.voltooiing.tab.boek"),
                VoltooiingClient::open);
    }

    /** Asks the server for the book (the key, the tab). */
    public static void open() {
        ClientPacketDistributor.sendToServer(new VoltooiingPayloads.Open(""));
    }
}
