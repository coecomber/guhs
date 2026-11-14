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

    /** 1.10.3: ticks left of the pink fade after a campfire trip (and its length). */
    private static int fade, fadeMax = 1;

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
                t.procent() == nl.juiced.guhs.feature.vadsrijk.voltooiing.KampvuurReizen.TOAST_VRIJ   // 1.10.3: campfire travel unlocked
                        ? new KnusToast(Component.translatable("vadsrijk.voltooiing.reis.vrij.toast.titel"),
                                Component.translatable("vadsrijk.voltooiing.reis.vrij.toast.tekst", Component.translatable(t.zoneKey())), new ItemStack(Items.CAMPFIRE))
                        : new KnusToast(Component.translatable("vadsrijk.voltooiing.toast.titel", t.procent()),
                                Component.translatable("vadsrijk.voltooiing.toast.tekst", Component.translatable(t.zoneKey())), new ItemStack(Items.WRITABLE_BOOK))));
        // 1.10.3 Kampvuurreizen: the travel screen, and a short pink fade after the trip
        VoltooiingPayloads.reisOntvanger = r -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ReisScherm s) {
                s.update(r.data(), java.util.List.of());
            } else {
                mc.setScreen(new ReisScherm(r.data()));
            }
        });
        VoltooiingPayloads.reisKlaarOntvanger = r -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ReisScherm) {
                mc.setScreen(null);
            }
            fadeMax = Math.max(1, r.ticks());
            fade = fadeMax;
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            if (fade > 0) {
                fade--;
            }
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) -> e.registerAboveAll(nl.juiced.guhs.Guhs.id("vr_kampvuur_reis_fade"),
                (g, dt) -> {
                    if (fade <= 0) {
                        return;
                    }
                    float a = net.minecraft.util.Mth.clamp((fade - dt.getGameTimeDeltaPartialTick(false)) / fadeMax, 0f, 1f);
                    g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (a * a * 235) << 24) | 0xFFD6EC);
                }));
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
