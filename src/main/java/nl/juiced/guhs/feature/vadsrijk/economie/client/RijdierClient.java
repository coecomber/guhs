package nl.juiced.guhs.feature.vadsrijk.economie.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;

/**
 * G-MOUNTS, client: the two mount keys (Vadsrijk category, rebindable): {@link #RIJDEN} (default X, mount/dismount) and
 * {@link #SCHERM} (default N, the mount screen), and the HUD icon of the active mount right of the skill slots: the mount's icon,
 * the key, and its state (ready = pink rim, riding = green, cooldown = a dark sweep with seconds, scared by a hit = red with a
 * "!", in a boss arena = grey with a cross).
 */
public final class RijdierClient {
    public static final KeyMapping RIJDEN = new KeyMapping("key.guhs.vr_rijdier", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, KernClient.CATEGORIE);
    public static final KeyMapping SCHERM = new KeyMapping("key.guhs.vr_rijdieren", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, KernClient.CATEGORIE);
    /** (fix round 3) Its tab in {@link nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs}. */
    public static final String TAB = "rijdieren";

    private static EcoPayloads.RijdierHud hud;
    private static long hudTijd;

    private RijdierClient() {
    }

    public static void init(IEventBus modBus) {
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(TAB, 10, () -> Component.translatable("vadsrijk.economie.rijdieren.titel"),
                () -> ClientPacketDistributor.sendToServer(new EcoPayloads.RijdierToets(1)));
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(RIJDEN);
            e.register(SCHERM);
        });
        EcoPayloads.hudOntvanger = h -> Minecraft.getInstance().execute(() -> {
            hud = h;
            hudTijd = System.currentTimeMillis();
        });
        // In the dimension our keys win over other Guhs keys on the same key (the snuffel boekje is N too).
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !Vadsrijk.in(mc.player)) {
                return;
            }
            for (KeyMapping k : mc.options.keyMappings) {
                if (k == RIJDEN || k == SCHERM || k.getCategory() != nl.juiced.guhs.client.GuhKeys.CATEGORY) {
                    continue;
                }
                if (k.same(RIJDEN) || k.same(SCHERM)) {
                    while (k.consumeClick()) {
                        // (swallowed)
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null;
            while (RIJDEN.consumeClick()) {
                if (binnen) {
                    ClientPacketDistributor.sendToServer(new EcoPayloads.RijdierToets(0));
                }
            }
            while (SCHERM.consumeClick()) {
                if (binnen) {
                    ClientPacketDistributor.sendToServer(new EcoPayloads.RijdierToets(1));
                }
            }
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_rijdier_hud"), RijdierClient::teken));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> hud = null);
    }

    private static void teken(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        EcoPayloads.RijdierHud h = hud;
        if (mc.player == null || !Vadsrijk.in(mc.player) || mc.options.hideGui || h == null || h.staat() == Rijdieren.HUD_GEEN) {
            return;
        }
        RijdierSoort s = RijdierSoort.van(h.soort());
        if (s == null) {
            return;
        }
        Font f = mc.font;
        // right of the 4 skill slots + the Knuffelsprong (RijkHud: c + 98 + 4 * 23 + 1 + 16)
        int x = g.guiWidth() / 2 + 98 + 4 * 23 + 22, y = g.guiHeight() - 22;
        int cd = h.cd() - (int) ((System.currentTimeMillis() - hudTijd) / 50);
        boolean rijdt = h.staat() == Rijdieren.HUD_RIJDT, schrik = h.staat() == Rijdieren.HUD_GESCHROKKEN && cd > 0,
                wacht = (h.staat() == Rijdieren.HUD_WACHT || h.staat() == Rijdieren.HUD_GESCHROKKEN) && cd > 0, arena = h.staat() == Rijdieren.HUD_ARENA;
        int rand = rijdt ? 0xFF5BD18A : schrik ? 0xFFE0405A : arena ? 0xFF6A6070 : wacht ? 0xFF6A3A5A : 0xFFFF7FC0;
        g.fill(x - 1, y - 1, x + 21, y + 21, rand);
        g.fill(x, y, x + 20, y + 20, 0xE0281A24);
        ItemStack icoon = EcoSpul.icoon(s, RijdierSoort.PluimKleur.van(h.kleur()));
        g.item(icoon, x + 2, y + 2);
        if (wacht) {
            int hoog = (int) Math.ceil(20f * cd / Math.max(1, h.cdMax()));
            g.fill(x, y + 20 - Math.min(20, hoog), x + 20, y + 20, 0xB0000000);
            String sec = String.valueOf((cd + 19) / 20);
            g.text(f, sec, x + 10 - f.width(sec) / 2, y + 6, 0xFFFFFFFF, true);
        }
        if (schrik) {
            g.text(f, "!", x + 16, y - 4, 0xFFFF5070, true);
        }
        if (arena) {
            g.fill(x, y, x + 20, y + 20, 0x90000000);
            for (int i = 3; i < 17; i++) {
                g.fill(x + i, y + i, x + i + 1, y + i + 1, 0xFFE0405A);
                g.fill(x + 19 - i, y + i, x + 20 - i, y + i + 1, 0xFFE0405A);
            }
        }
        String toets = RIJDEN.getTranslatedKeyMessage().getString();
        if (toets.length() > 3) {
            toets = toets.substring(0, 3);
        }
        g.pose().pushMatrix();
        g.pose().translate(x + 1, y + 1);
        g.pose().scale(0.6f, 0.6f);
        g.text(f, toets, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        // a tiny state word under the slot when not just "ready"
        String woord = rijdt ? "vadsrijk.economie.rijdier.hud.rijdt" : schrik ? "vadsrijk.economie.rijdier.hud.schrik"
                : arena ? "vadsrijk.economie.rijdier.hud.arena" : null;
        if (woord != null) {
            String t = Component.translatable(woord).getString();
            g.pose().pushMatrix();
            g.pose().translate(x + 10, y - 7);
            g.pose().scale(0.6f, 0.6f);
            g.text(f, t, -f.width(t) / 2, 0, rand, true);
            g.pose().popMatrix();
        }
    }
}
