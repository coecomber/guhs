package nl.juiced.guhs.feature.vadsrijk.menu.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * U-TABS: "Het Vadsrijk-menu", ONE big screen with main tabs on top (Karakter, Skillboom, Questlog, Verkenningsboek, Rijdieren,
 * Huisdieren, Kledingkast, Knapzak, Ambachten, Puzzels, Ridders, Reiskaart, Instellingen, ...). Every tab shows its panel in the
 * SAME frame ({@link #kader}); a tab may have sub-tabs (Rijdieren | Zeilbootje), drawn as a second row in the title band.
 * <p>A panel is just a Screen; the frame, the tab row, the scaling and the tooltips come for free when it extends
 * {@link MenuPaneel} (or economie's EcoScherm and returns its tab id from {@code collectieTab()}).
 * <pre>
 * // in your client init (once):
 * VadsrijkMenu.tab("gilden", () -&gt; new ItemStack(Items.WHITE_BANNER), () -&gt; mijnServer.vraag(), 115);       // server sends data, opens the panel
 * VadsrijkMenu.scherm("hud", () -&gt; new ItemStack(Items.PAINTING), HudPaneel::new, 135);                        // client-only panel
 * VadsrijkMenu.subtab("instellingen", "hud_editor", () -&gt; Component.translatable("..."), HudBewerker::open, 10); // a sub-tab
 * VadsrijkMenu.toets("gilden", MijnClient.TOETS);                                                                // key shown in the tooltip
 * // the tab name = lang key "vadsrijk.menu.tab.&lt;id&gt;", the hover line = "vadsrijk.menu.tab.&lt;id&gt;.uitleg" (when present)
 * </pre>
 * Order of the main tabs (leave room): karakter 10, skillboom 20, questlog 30, verkenningsboek 40, rijdieren 50, huisdieren 60,
 * kledingkast 70, knapzak 80, ambachten 90, puzzels 100, ridders 110, reiskaart 120, instellingen 130.
 */
public final class VadsrijkMenu {
    /** Height of the main tab row above the frame. */
    public static final int TABS_H = 20;
    /** Height of the title band (and of the sub-tab row inside it). */
    public static final int BAND = 18;
    /** The smallest logical frame every panel can count on (the screen scales down below this) and the largest it grows to. */
    public static final int MIN_W = 430, MIN_H = 250, MAX_W = 560, MAX_H = 330;
    private static final int MARGE = 6;
    static final int RAND = 0xFFFF5FAF, BAND_KLEUR = 0xFFFF7FC0, UIT = 0xFFFFC8E4, UIT_HOVER = 0xFFFFDDEE, TEKST_UIT = 0xFF8A5A84;

    /**
     * One tab. {@code ouder} = null for a main tab, else the main tab it belongs under. {@code open} shows its panel (often:
     * ask the server, which sends the data that opens the screen).
     */
    public record Tab(String id, Supplier<ItemStack> icoon, Supplier<Component> naam, Runnable open, int volgorde, @Nullable String ouder) {
    }

    private static final List<Tab> TABS = new ArrayList<>();
    private static final java.util.Map<String, KeyMapping> TOETSEN = new java.util.HashMap<>();
    /** The main tab shown last (so the menu key opens where you were). */
    private static String laatste = "karakter";

    private VadsrijkMenu() {
    }

    // ---- registry -------------------------------------------------------------------------------------------------------------

    /** A main tab whose name is the lang key {@code vadsrijk.menu.tab.<id>}. Same id again = replaced. */
    public static Tab tab(String id, Supplier<ItemStack> icoon, Runnable open, int volgorde) {
        return tab(id, icoon, () -> Component.translatable("vadsrijk.menu.tab." + id), open, volgorde);
    }

    /** A main tab with its own name. */
    public static synchronized Tab tab(String id, Supplier<ItemStack> icoon, Supplier<Component> naam, Runnable open, int volgorde) {
        return zet(new Tab(id, icoon, naam, open, volgorde, null));
    }

    /** A main tab whose panel is client-only: {@code maak} builds the screen (a {@link MenuPaneel}). */
    public static Tab scherm(String id, Supplier<ItemStack> icoon, Supplier<? extends Screen> maak, int volgorde) {
        return tab(id, icoon, () -> Minecraft.getInstance().setScreen(maak.get()), volgorde);
    }

    /** A sub-tab under main tab {@code ouder} (the main tab itself is always the first sub-tab). */
    public static synchronized Tab subtab(String ouder, String id, Supplier<Component> naam, Runnable open, int volgorde) {
        return zet(new Tab(id, ItemStack::empty, naam, open, volgorde, ouder));
    }

    /** The key that opens this tab (shown in its tooltip). */
    public static synchronized void toets(String id, KeyMapping k) {
        TOETSEN.put(id, k);
    }

    /** (fix round 3 shim, {@code CollectieTabs.registreer}) adds a main tab only when the id is not there yet. */
    public static synchronized void alsNieuw(String id, int volgorde, Supplier<Component> naam, Runnable open) {
        if (vind(id) == null) {
            zet(new Tab(id, () -> new ItemStack(net.minecraft.world.item.Items.BOOK), naam, open, volgorde, null));
        }
    }

    private static Tab zet(Tab t) {
        TABS.removeIf(o -> o.id().equals(t.id()));
        TABS.add(t);
        TABS.sort(Comparator.comparingInt(Tab::volgorde));
        return t;
    }

    public static synchronized List<Tab> alle() {
        return List.copyOf(TABS);
    }

    public static synchronized List<Tab> hoofdtabs() {
        return TABS.stream().filter(t -> t.ouder() == null).toList();
    }

    /** The sub-tabs of a main tab (empty = none; else the main tab first, then its sub-tabs). */
    public static synchronized List<Tab> subtabs(String hoofd) {
        List<Tab> l = new ArrayList<>();
        for (Tab t : TABS) {
            if (hoofd.equals(t.ouder())) {
                l.add(t);
            }
        }
        if (l.isEmpty()) {
            return l;
        }
        Tab h = vind(hoofd);
        if (h != null) {
            l.add(0, h);
        }
        return l;
    }

    @Nullable
    public static synchronized Tab vind(String id) {
        for (Tab t : TABS) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** The main tab a (sub-)tab id belongs to. */
    public static String hoofdVan(String id) {
        Tab t = vind(id);
        return t != null && t.ouder() != null ? t.ouder() : id;
    }

    @Nullable
    public static KeyMapping toetsVan(String id) {
        return TOETSEN.get(id);
    }

    /** Opens a tab (or the last shown main tab for null). */
    public static void open(@Nullable String id) {
        Tab t = vind(id == null ? laatste : id);
        if (t == null && !hoofdtabs().isEmpty()) {
            t = hoofdtabs().get(0);
        }
        if (t != null) {
            t.open().run();
        }
    }

    static void gezien(String id) {
        laatste = hoofdVan(id);
    }

    public static String laatste() {
        return laatste;
    }

    // ---- geometry -------------------------------------------------------------------------------------------------------------

    /**
     * The frame on a screen of (gui-scaled) size sw x sh. {@code schaal} &lt; 1 when even the smallest frame does not fit: the
     * panel then works in a LOGICAL screen of {@code lw x lh} (= sw/schaal x sh/schaal) and is drawn scaled down.
     * left/top/w/h = the panel (title band included); the main tab row sits above top.
     */
    public record Kader(float schaal, int lw, int lh, int left, int top, int w, int h) {
    }

    public static Kader kader(int sw, int sh) {
        int nodigW = MIN_W + 2 * MARGE, nodigH = MIN_H + TABS_H + 2 * MARGE + 4;
        float s = Math.min(1f, Math.min(sw / (float) nodigW, sh / (float) nodigH));
        int lw = (int) Math.floor(sw / s), lh = (int) Math.floor(sh / s);
        int w = Math.max(MIN_W, Math.min(MAX_W, lw - 2 * MARGE - 8));
        int h = Math.max(MIN_H, Math.min(MAX_H, lh - 2 * MARGE - TABS_H - 4));
        int left = (lw - w) / 2;
        int top = Math.max(MARGE + TABS_H + 2, (lh - h + TABS_H) / 2);
        return new Kader(s, lw, lh, left, top, w, h);
    }

    // ---- drawing --------------------------------------------------------------------------------------------------------------

    private record Vlak(Tab tab, int x, int y, int w, int h) {
        boolean binnen(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** The main tab row: equal widths that fill the frame; icon + name when the name fits, else only the icon. */
    private static List<Vlak> hoofdIndeling(int left, int top, int w) {
        List<Vlak> l = new ArrayList<>();
        List<Tab> tabs = hoofdtabs();
        if (tabs.isEmpty()) {
            return l;
        }
        int gat = 2, n = tabs.size();
        int tw = Math.max(18, (w - 8 - gat * (n - 1)) / n);
        int x = left + 4 + Math.max(0, (w - 8 - n * tw - gat * (n - 1)) / 2);
        for (Tab t : tabs) {
            l.add(new Vlak(t, x, top - TABS_H, tw, TABS_H));
            x += tw + gat;
        }
        return l;
    }

    /** The sub-tab row, right-aligned inside the title band (shrinks names to fit; never past the title). */
    private static List<Vlak> subIndeling(Font font, String actief, int left, int top, int w, int titelBreed) {
        List<Vlak> l = new ArrayList<>();
        List<Tab> subs = subtabs(hoofdVan(actief));
        if (subs.isEmpty()) {
            return l;
        }
        int ruimte = w - titelBreed - 24, gat = 2;
        int elk = Math.max(30, Math.min(110, (ruimte - gat * (subs.size() - 1)) / subs.size()));
        int x = left + w - 6 - subs.size() * elk - gat * (subs.size() - 1);
        for (Tab t : subs) {
            l.add(new Vlak(t, x, top + 2, elk, BAND - 2));
            x += elk + gat;
        }
        return l;
    }

    /** Draws the main tabs above the panel at (left, top, w); {@code actief} = the (sub-)tab id of this panel. */
    public static void tekenTabs(GuiGraphicsExtractor g, Font font, int left, int top, int w, String actief, int mx, int my) {
        String hoofd = hoofdVan(actief);
        gezien(actief);
        for (Vlak v : hoofdIndeling(left, top, w)) {
            boolean aan = v.tab().id().equals(hoofd);
            boolean over = !aan && v.binnen(mx, my);
            int x0 = v.x(), y0 = v.y() + (aan ? 0 : 3), x1 = v.x() + v.w(), y1 = top;
            g.fill(x0, y0, x1, y1, RAND);
            g.fill(x0 + 1, y0 + 1, x1 - 1, aan ? y1 : top - 1, aan ? BAND_KLEUR : over ? UIT_HOVER : UIT);
            if (aan) {
                g.fill(x0 + 1, top - 1, x1 - 1, top, BAND_KLEUR);   // joined to the title band
            }
            ItemStack ic = v.tab().icoon().get();
            Component naam = v.tab().naam().get();
            int tekstBreed = font.width(naam);
            boolean metNaam = !ic.isEmpty() && v.w() >= 16 + tekstBreed + 8;
            int iy = y0 + (TABS_H - (aan ? 0 : 3) - 16) / 2 + 1;
            if (ic.isEmpty()) {
                String s = font.plainSubstrByWidth(naam.getString(), v.w() - 4);
                g.centeredText(font, Component.literal(s), x0 + v.w() / 2, iy + 4, aan ? 0xFFFFFFFF : TEKST_UIT);
            } else if (metNaam) {
                int bx = x0 + (v.w() - 16 - 2 - tekstBreed) / 2;
                g.item(ic, bx, iy);
                g.text(font, naam, bx + 18, iy + 4, aan ? 0xFFFFFFFF : TEKST_UIT, aan);
            } else {
                g.item(ic, x0 + (v.w() - 16) / 2, iy);
            }
            if (v.binnen(mx, my)) {
                g.setComponentTooltipForNextFrame(font, tip(v.tab()), mx, my);
            }
        }
    }

    /** The tooltip of a tab: its name, its key, and its one-line explanation (lang key {@code vadsrijk.menu.tab.<id>.uitleg}). */
    public static List<Component> tip(Tab t) {
        List<Component> l = new ArrayList<>();
        l.add(t.naam().get().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        String k = "vadsrijk.menu.tab." + t.id() + ".uitleg";
        if (net.minecraft.locale.Language.getInstance().has(k)) {
            l.add(Component.translatable(k).withStyle(ChatFormatting.GRAY));
        }
        KeyMapping km = TOETSEN.get(t.id());
        if (km != null && !km.isUnbound()) {
            l.add(Component.translatable("vadsrijk.menu.toets", km.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return l;
    }

    /** Draws the sub-tab row in the title band (when the active main tab has sub-tabs). */
    public static void tekenSubtabs(GuiGraphicsExtractor g, Font font, int left, int top, int w, int titelBreed, String actief, int mx, int my) {
        for (Vlak v : subIndeling(font, actief, left, top, w, titelBreed)) {
            boolean aan = v.tab().id().equals(actief);
            boolean over = !aan && v.binnen(mx, my);
            g.fill(v.x(), v.y(), v.x() + v.w(), v.y() + v.h(), aan ? 0xFFFFE6F3 : over ? 0xFFFFB0D8 : 0xFFFF9ACC);
            String s = font.plainSubstrByWidth(v.tab().naam().get().getString(), v.w() - 6);
            g.centeredText(font, Component.literal(s), v.x() + v.w() / 2, v.y() + 4, aan ? 0xFF4A1E46 : 0xFFFFFFFF);
            if (over) {
                g.setComponentTooltipForNextFrame(font, tip(v.tab()), mx, my);
            }
        }
    }

    /** A click on the main tabs or sub-tabs; true when it hit one (the active one does nothing). */
    public static boolean klik(Font font, double mx, double my, int left, int top, int w, int titelBreed, String actief) {
        String hoofd = hoofdVan(actief);
        for (Vlak v : hoofdIndeling(left, top, w)) {
            if (v.binnen(mx, my)) {
                if (!v.tab().id().equals(hoofd)) {
                    klikGeluid();
                    v.tab().open().run();
                }
                return true;
            }
        }
        for (Vlak v : subIndeling(font, actief, left, top, w, titelBreed)) {
            if (v.binnen(mx, my)) {
                if (!v.tab().id().equals(actief)) {
                    klikGeluid();
                    v.tab().open().run();
                }
                return true;
            }
        }
        return false;
    }

    /** Ctrl+Tab / Ctrl+Shift+Tab style: the next main tab (richting +1 / -1). */
    public static void volgende(String actief, int richting) {
        List<Tab> l = hoofdtabs();
        String hoofd = hoofdVan(actief);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id().equals(hoofd)) {
                klikGeluid();
                l.get(Math.floorMod(i + richting, l.size())).open().run();
                return;
            }
        }
    }

    static void klikGeluid() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
    }
}
