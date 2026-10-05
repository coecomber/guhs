package nl.juiced.guhs.feature.guhpixel.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.GuhpixelPayloads;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.feature.guhpixel.Rangen;
import nl.juiced.guhs.feature.guhpixel.among.client.AmongClient;
import nl.juiced.guhs.feature.guhpixel.bioscoop.client.BioscoopClient;
import nl.juiced.guhs.feature.guhpixel.grap1.client.Grap1Client;
import nl.juiced.guhs.feature.guhpixel.grap2.client.Grap2Client;
import nl.juiced.guhs.feature.guhpixel.guhkade.client.GuhkadeClient;
import nl.juiced.guhs.feature.guhpixel.kantoor.client.KantoorClient;
import nl.juiced.guhs.feature.guhpixel.lobby.client.LobbyClient;
import nl.juiced.guhs.feature.guhpixel.parkour.client.ParkourClient;
import nl.juiced.guhs.feature.guhpixel.reisbureau.client.ReisbureauClient;

/**
 * Client side of the guhpixel kern: the little muntjes HUD (only in the guhpixel dimension), the shop screen, the cache of
 * the Guhdex tab, the ranks above the heads; then every slice's client entry in table order.
 */
public final class GuhpixelClient {
    private static boolean toegang;
    private static int saldo, totaal, rang;
    /** A slice's own HUD (a game's timer, a task list) may hide the muntjes panel while it shows. */
    public static volatile boolean hudVerborgen;

    public static void init(IEventBus modBus) {
        GuhpixelPayloads.hudOntvanger = p -> opClient(() -> {
            toegang = p.toegang();
            saldo = p.saldo();
            totaal = p.totaal();
            rang = p.rang();
        });
        GuhpixelPayloads.rangenOntvanger = p -> opClient(() -> {
            Rangen.zetClient(p.rangen());
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                for (Player speler : mc.level.players()) {
                    speler.refreshDisplayName();
                }
            }
        });
        GuhpixelPayloads.winkelOpener = p -> opClient(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof WinkelScherm s) {
                s.update(p.data());
            } else {
                mc.setScreen(new WinkelScherm(p.data()));
            }
        });
        GuhpixelPayloads.gidsOntvanger = p -> opClient(() -> GidsGuhpixelTab.zet(p.data()));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("guhpixel_hud"), GuhpixelClient::tekenHud));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            toegang = false;
            saldo = totaal = rang = 0;
            hudVerborgen = false;
            GidsGuhpixelTab.vergeet();
        });
        LobbyClient.init(modBus);
        Grap1Client.init(modBus);
        Grap2Client.init(modBus);
        AmongClient.init(modBus);
        GuhkadeClient.init(modBus);
        KantoorClient.init(modBus);
        BioscoopClient.init(modBus);
        ReisbureauClient.init(modBus);
        ParkourClient.init(modBus);
    }

    /** The saldo the server told last (screens of the slices may show it). */
    public static int saldo() {
        return saldo;
    }

    public static int totaal() {
        return totaal;
    }

    public static Rang rang() {
        return Rang.op(rang);
    }

    public static boolean toegang() {
        return toegang;
    }

    static void opClient(Runnable r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            r.run();
        } else {
            mc.execute(r);
        }
    }

    /** Top left, only in guhpixel: "GUHPIXEL", the muntjes and the rank. */
    private static void tekenHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || hudVerborgen || mc.level.dimension() != Guhpixel.DIM || mc.screen != null) {
            return;
        }
        Component kop = Component.literal("GUHPIXEL").withStyle(ChatFormatting.BOLD);
        Component munt = Component.translatable("gui.guhs.guhpixel.hud.muntjes", saldo);
        Component r = rang().naam();
        int w = Math.max(mc.font.width(kop), Math.max(mc.font.width(munt), mc.font.width(r))) + 10;
        int x = 4, y = 4;
        g.fill(x - 1, y - 1, x + w + 1, y + 35, 0xFFF7B6CB);
        g.fill(x, y, x + w, y + 34, 0xE8301A26);
        g.text(mc.font, kop, x + 5, y + 3, 0xFFFF9AC8, false);
        g.text(mc.font, munt, x + 5, y + 14, 0xFFFFD27A, false);
        g.text(mc.font, r, x + 5, y + 24, 0xFFFFFFFF, false);
    }

    private GuhpixelClient() {
    }
}
