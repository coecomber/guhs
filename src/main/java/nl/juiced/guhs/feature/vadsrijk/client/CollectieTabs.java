package nl.juiced.guhs.feature.vadsrijk.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * (fix round 3, i-ui) Tabs on top of the Vadsrijk collection screens (Rijdieren, Huisdieren, and later more), so you can switch
 * between them with one click. A tab is an id, an order, a label and what clicking it does (usually: ask the server for that
 * screen; the new screen replaces the old one).
 * <pre>
 * // once, in your client init:
 * CollectieTabs.registreer("voltooiing", 30, () -&gt; Component.translatable("vadsrijk.voltooiing.tab"), VoltooiingClient::open);
 *
 * // a screen built on economie's EcoScherm: just return its tab id
 * &#64;Override protected String collectieTab() { return "voltooiing"; }
 *
 * // any other Screen: reserve {@link #HOOGTE} pixels above your panel, then
 * CollectieTabs.teken(g, font, left, top, w, "voltooiing", mouseX, mouseY);       // in extractRenderState / extractBackground
 * if (CollectieTabs.klik(event.x(), event.y(), left, top, w, "voltooiing")) return true;   // first thing in mouseClicked
 * </pre>
 * Tabs are drawn as little flaps just above the panel's top edge (between the guh ears), the active one in the title band's pink
 * and joined to it. With fewer than two tabs registered nothing is drawn.
 */
public final class CollectieTabs {
    /** Height of the tab row above the panel (a screen should keep this much room above its panel). */
    public static final int HOOGTE = 16;
    private static final int GAT = 3, MARGE = 18, BAND = 0xFFFF7FC0, RAND = 0xFFFF5FAF, UIT = 0xFFFFC8E4, UIT_HOVER = 0xFFFFDDEE,
            TEKST_AAN = 0xFFFFFFFF, TEKST_UIT = 0xFF8A5A84;

    /** One tab. */
    public record Tab(String id, int volgorde, Supplier<Component> naam, Runnable open) {
    }

    private static final List<Tab> TABS = new ArrayList<>();

    private CollectieTabs() {
    }

    /**
     * Adds (or replaces, same id) a tab. volgorde: lower = more to the left (rijdieren 10, huisdieren 20; leave room).
     * open: what a click on it does while another tab is showing.
     */
    public static synchronized void registreer(String id, int volgorde, Supplier<Component> naam, Runnable open) {
        TABS.removeIf(t -> t.id().equals(id));
        TABS.add(new Tab(id, volgorde, naam, open));
        TABS.sort(Comparator.comparingInt(Tab::volgorde));
    }

    public static synchronized List<Tab> alle() {
        return List.copyOf(TABS);
    }

    /** Are there tabs to show (two or more)? */
    public static boolean zichtbaar() {
        return alle().size() >= 2;
    }

    private record Vlak(Tab tab, int x, int y, int w) {
    }

    private static List<Vlak> indeling(Font font, int left, int top, int w) {
        List<Vlak> l = new ArrayList<>();
        List<Tab> tabs = alle();
        if (tabs.size() < 2) {
            return l;
        }
        int x = left + MARGE;
        int ruimte = w - 2 * MARGE - GAT * (tabs.size() - 1);
        int max = Math.max(30, ruimte / tabs.size());
        for (Tab t : tabs) {
            int tw = Math.min(max, font.width(t.naam().get().copy().withStyle(ChatFormatting.BOLD)) + 14);
            l.add(new Vlak(t, x, top - 3 - HOOGTE, tw));
            x += tw + GAT;
        }
        return l;
    }

    /** Draws the tab row above a panel at (left, top) of width w; {@code actief} = the tab of this screen. */
    public static void teken(GuiGraphicsExtractor g, Font font, int left, int top, int w, @Nullable String actief, int mx, int my) {
        for (Vlak v : indeling(font, left, top, w)) {
            boolean aan = v.tab().id().equals(actief);
            boolean over = !aan && binnen(mx, my, v);
            int x0 = v.x(), y0 = v.y() + (aan ? 0 : 2), x1 = v.x() + v.w(), y1 = top;
            g.fill(x0, y0, x1, y1, RAND);
            g.fill(x0 + 1, y0 + 1, x1 - 1, aan ? y1 : top - 3, aan ? BAND : over ? UIT_HOVER : UIT);
            if (aan) {
                g.fill(x0 + 1, top - 3, x1 - 1, top, BAND);   // (joined to the title band: no border in between)
            }
            Component c = v.tab().naam().get().copy().withStyle(aan ? ChatFormatting.BOLD : ChatFormatting.RESET);
            String s = font.plainSubstrByWidth(c.getString(), v.w() - 6);
            Component k = s.length() < c.getString().length() ? Component.literal(s).withStyle(c.getStyle()) : c;
            g.centeredText(font, k, x0 + v.w() / 2, y0 + (aan ? 5 : 4), aan ? TEKST_AAN : TEKST_UIT);
        }
    }

    /** A click on a tab other than {@code actief}: runs its open action. True when the click hit a tab (also the active one). */
    public static boolean klik(double mx, double my, int left, int top, int w, @Nullable String actief) {
        Minecraft mc = Minecraft.getInstance();
        for (Vlak v : indeling(mc.font, left, top, w)) {
            if (binnen((int) mx, (int) my, v)) {
                if (!v.tab().id().equals(actief)) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
                    v.tab().open().run();
                }
                return true;
            }
        }
        return false;
    }

    private static boolean binnen(int mx, int my, Vlak v) {
        return mx >= v.x() && mx < v.x() + v.w() && my >= v.y() && my < v.y() + HOOGTE + 3;
    }
}
