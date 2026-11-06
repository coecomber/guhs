package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;

/**
 * The pink base of every economie screen: a light pink panel with a hot-pink title band, dark plum text (readable), pink buttons
 * drawn by hand ({@link #knop}), item slots with tooltips, the wallet with coin icons, a message line. The server sends the
 * data; actions go back with {@link #doe}. Subclasses draw in {@link #inhoud}.
 */
public abstract class EcoScherm extends Screen implements nl.juiced.guhs.feature.vadsrijk.menu.client.Geschaald {   // U-TABS: Geschaald
    public static final int RAND = 0xFFFF5FAF, KOP = 0xFFFF7FC0, PANEEL = 0xFFFFE6F3, RIJ_A = 0xFFFFD6EB, RIJ_B = 0xFFFFEEF7, HOVER = 0xFFFFB8DC,
            TEKST = 0xFF4A1E46, ZACHT = 0xFF8A5A84, GOUD = 0xFFB07800, ROOD = 0xFFD0306A, GROEN = 0xFF2E9E5A, WIT = 0xFFFFFFFF;

    protected final String scherm;
    protected CompoundTag data;
    protected List<ItemStack> items;
    protected int left, top, w, h;
    /** U-TABS: the scale of the Vadsrijk-menu frame (1 = none; below 1 the screen works in a logical size, see Geschaald). */
    protected float menuSchaal = 1f;

    private record Knop(int x, int y, int w, int h, Runnable actie) {
    }

    private final List<Knop> knoppen = new ArrayList<>();
    /** U-TABS: width of the title in the band (the sub-tabs start after it). */
    protected int titelBreed;
    @Nullable
    private ItemStack hoverItem;
    @Nullable
    private List<Component> hoverTekst;

    protected EcoScherm(String scherm, Component titel, CompoundTag data, List<ItemStack> items, int w, int h) {
        super(titel);
        this.scherm = scherm;
        this.data = data;
        this.items = items;
        this.w = w;
        this.h = h;
    }

    /** New data from the server for this same screen (keeps scroll/selection). */
    public void update(CompoundTag data, List<ItemStack> items) {
        this.data = data;
        this.items = items;
        opNieuweData();
    }

    protected void opNieuweData() {
    }

    @Override
    protected void init() {
        if (heeftTabs()) {
            // U-TABS: a tab of the Vadsrijk-menu: the shared frame (same size for every tab, grows with the window, scales down)
            nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader k = nl.juiced.guhs.feature.vadsrijk.menu.client.MenuClient.kader();
            menuSchaal = k.schaal();
            width = k.lw();
            height = k.lh();
            left = k.left();
            top = k.top();
            w = k.w();
            h = k.h();
            return;
        }
        menuSchaal = 1f;
        left = (width - w) / 2;
        top = (height - h) / 2;
    }

    @Override
    public float menuSchaal() {
        return menuSchaal;
    }

    /**
     * (fix round 3) The collection tab of this screen ({@link nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs}), or null
     * for none: then the tab row is drawn above the panel and switches to the other collection screens.
     */
    @Nullable
    protected String collectieTab() {
        return null;
    }

    /** U-TABS: is this screen a tab of the Vadsrijk-menu (then it gets the shared frame + the tab rows)? */
    protected boolean heeftTabs() {
        return collectieTab() != null;
    }

    protected ItemStack item(int i) {
        return i >= 0 && i < items.size() ? items.get(i) : ItemStack.EMPTY;
    }

    protected void doe(String actie, CompoundTag args) {
        ClientPacketDistributor.sendToServer(new EcoPayloads.Doe(scherm, actie, args));
    }

    protected void doe(String actie) {
        doe(actie, new CompoundTag());
    }

    @Override
    public void onClose() {
        doe("dicht");
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected static void geluid(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    protected static void geluid(Holder<SoundEvent> s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    // ---- drawing ------------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        if (heeftTabs()) {   // U-TABS: the menu frame (tabs on top, sub-tabs in the band)
            titelBreed = nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.tekenKader(g, font, left, top, w, h, title, collectieTab(), mouseX, mouseY);
            if (data.contains("munten")) {
                munten(g, left + w - 8, top + 5, data.getLongOr("munten", 0), true, true);
            }
            return;
        }
        g.fill(left - 3, top - 3, left + w + 3, top + h + 3, RAND);
        g.fill(left - 1, top - 1, left + w + 1, top + h + 1, 0xFFFFB0D8);
        g.fill(left, top, left + w, top + h, PANEEL);
        g.fill(left, top, left + w, top + 18, KOP);
        // little guh ears on the title band
        g.fill(left + 6, top - 7, left + 14, top, KOP);
        g.fill(left + w - 14, top - 7, left + w - 6, top, KOP);
        g.fill(left + 8, top - 5, left + 12, top, 0xFFFFC8E4);
        g.fill(left + w - 12, top - 5, left + w - 8, top, 0xFFFFC8E4);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, WIT, true);
        if (data.contains("munten")) {
            munten(g, left + w - 8, top + 5, data.getLongOr("munten", 0), true, true);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        knoppen.clear();
        hoverItem = null;
        hoverTekst = null;
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        inhoud(g, mouseX, mouseY, partialTick);
        String m = data.getStringOr("melding", "");
        if (!m.isEmpty()) {
            Component c = Component.translatable(m, data.getStringOr("meldingArg", ""));
            int mw = font.width(c) + 10;
            g.fill(left + (w - mw) / 2, top + h - 13, left + (w + mw) / 2, top + h - 2, 0xF0FFFFFF);
            g.centeredText(font, c, left + w / 2, top + h - 11, TEKST);
        }
        if (hoverItem != null && !hoverItem.isEmpty()) {
            g.setTooltipForNextFrame(font, hoverItem, mouseX, mouseY);
        } else if (hoverTekst != null) {
            g.setComponentTooltipForNextFrame(font, hoverTekst, mouseX, mouseY);
        }
    }

    /** Draw the screen's own content here (after the widgets). */
    protected abstract void inhoud(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick);

    protected boolean binnen(int mx, int my, int x, int y, int bw, int bh) {
        return mx >= x && mx < x + bw && my >= y && my < y + bh;
    }

    /** A pink button (drawn now, clickable until the next frame). */
    protected void knop(GuiGraphicsExtractor g, int mx, int my, int x, int y, int bw, int bh, Component label, boolean actief, Runnable actie) {
        boolean hover = actief && binnen(mx, my, x, y, bw, bh);
        g.fill(x, y, x + bw, y + bh, actief ? (hover ? 0xFFC0287C : 0xFFE0489A) : 0xFFC8A8BC);
        g.fill(x + 1, y + 1, x + bw - 1, y + bh - 1, actief ? (hover ? 0xFFFF8CC8 : 0xFFFF6FB5) : 0xFFE4D0DC);
        g.fill(x + 1, y + 1, x + bw - 1, y + 2, actief ? 0x60FFFFFF : 0x30FFFFFF);
        g.centeredText(font, label, x + bw / 2, y + (bh - 8) / 2, actief ? WIT : 0xFF9A8094);
        if (actief) {
            knoppen.add(new Knop(x, y, bw, bh, actie));
        }
    }

    /** A clickable area without drawing (rows, slots). */
    protected void klikvlak(int x, int y, int bw, int bh, Runnable actie) {
        knoppen.add(new Knop(x, y, bw, bh, actie));
    }

    /** An item slot (18x18) with its tooltip on hover. */
    protected void slot(GuiGraphicsExtractor g, int mx, int my, int x, int y, ItemStack s, boolean gekozen) {
        g.fill(x, y, x + 18, y + 18, gekozen ? 0xFFFF4FA8 : 0xFFE8A8CC);
        g.fill(x + 1, y + 1, x + 17, y + 17, gekozen ? 0xFFFFD0EA : 0xFFFFF4FA);
        if (!s.isEmpty()) {
            g.item(s, x + 1, y + 1);
            g.itemDecorations(font, s, x + 1, y + 1);
            if (binnen(mx, my, x, y, 18, 18)) {
                hoverItem = s;
            }
        }
    }

    protected void tip(int mx, int my, int x, int y, int bw, int bh, List<Component> tekst) {
        if (binnen(mx, my, x, y, bw, bh)) {
            hoverTekst = tekst;
        }
    }

    /** The wallet as coin icons + numbers (right-aligned when {@code rechts}). Returns the width. */
    protected int munten(GuiGraphicsExtractor g, int x, int y, long brons, boolean rechts, boolean schaduw) {
        long goud = brons / 10000, zilver = brons / 100 % 100, b = brons % 100;
        List<Object[]> delen = new ArrayList<>();
        if (goud > 0) {
            delen.add(new Object[] {new ItemStack(EcoSpul.MUNT_GOUD.get()), String.valueOf(goud), 0xFFFFD84A});
        }
        if (goud > 0 || zilver > 0) {
            delen.add(new Object[] {new ItemStack(EcoSpul.MUNT_ZILVER.get()), String.valueOf(zilver), 0xFFE8E8F4});
        }
        delen.add(new Object[] {new ItemStack(EcoSpul.MUNT_BRONS.get()), String.valueOf(b), 0xFFF0A868});
        int breed = 0;
        for (Object[] d : delen) {
            breed += 11 + font.width((String) d[1]) + 4;
        }
        int cx = rechts ? x - breed : x;
        for (Object[] d : delen) {
            g.pose().pushMatrix();
            g.pose().translate(cx, y - 1);
            g.pose().scale(0.625f, 0.625f);
            g.item((ItemStack) d[0], 0, 0);
            g.pose().popMatrix();
            cx += 11;
            g.text(font, (String) d[1], cx, y, schaduw ? (int) d[2] : TEKST, schaduw);
            cx += font.width((String) d[1]) + 4;
        }
        return breed;
    }

    // ---- U-TABS: a search box for long lists ----------------------------------------------------------------------------------
    /** The text typed in the search box (lower case; empty = show everything). */
    protected String zoekTekst = "";
    @Nullable
    protected net.minecraft.client.gui.components.EditBox zoekVeld;

    /**
     * Adds a small search box (call in init, after super.init()); {@code klaar} runs on every change (reset your scroll). The typed
     * text survives a re-init (window resize). Filter your rows with {@link #past}.
     */
    protected net.minecraft.client.gui.components.EditBox zoekveld(int x, int y, int bw, Runnable klaar) {
        net.minecraft.client.gui.components.EditBox e = new net.minecraft.client.gui.components.EditBox(font, x, y, bw, 12,
                Component.translatable("vadsrijk.menu.zoek"));
        e.setHint(Component.translatable("vadsrijk.menu.zoek").withStyle(ChatFormatting.GRAY));
        e.setMaxLength(40);
        e.setValue(zoekTekst);
        e.setResponder(t -> {
            zoekTekst = t.toLowerCase(java.util.Locale.ROOT).trim();
            klaar.run();
        });
        zoekVeld = addRenderableWidget(e);
        return e;
    }

    /** Does this text match the search box (always true when it is empty)? */
    protected boolean past(String tekst) {
        return zoekTekst.isEmpty() || tekst.toLowerCase(java.util.Locale.ROOT).contains(zoekTekst);
    }

    protected void kop(GuiGraphicsExtractor g, Component c, int x, int y) {
        g.text(font, c.copy().withStyle(ChatFormatting.BOLD), x, y, TEKST, false);
    }

    /** U-TABS: in the Vadsrijk-menu the tab keys switch tabs, the menu key / own key closes, Ctrl+Tab walks the tabs. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (heeftTabs() && !nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.typt(this)
                && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.toetsInMenu(this, event, collectieTab())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && heeftTabs()
                && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.klik(font, event.x(), event.y(), left, top, w, titelBreed, collectieTab())) {
            return true;
        }
        if (event.button() == 0) {
            for (Knop k : List.copyOf(knoppen)) {
                if (binnen((int) event.x(), (int) event.y(), k.x(), k.y(), k.w(), k.h())) {
                    geluid(SoundEvents.UI_BUTTON_CLICK, 1.2f);
                    k.actie().run();
                    return true;
                }
            }
        }
        return false;
    }
}
