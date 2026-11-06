package nl.juiced.guhs.feature.vadsrijk.opslag.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.client.screen.BankGuhScreen;
import nl.juiced.guhs.feature.vadsrijk.opslag.MateriaalMenu;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagPayloads;
import nl.juiced.guhs.storage.BankContents;

/**
 * T-OPSLAG: the Materiaaltas screen: a scrollable grid of every material kind with its count (any size), a search box, and your
 * inventory below. Left click = a stack on the cursor, right click = half, shift = straight into your inventory; click with a
 * material on the cursor (or shift-click one in your inventory) = put it in. Buttons: Alles opbergen, Automatisch aan/uit,
 * Knapzak.
 */
public class MateriaalScherm extends AbstractContainerScreen<MateriaalMenu> implements nl.juiced.guhs.feature.vadsrijk.menu.client.Geschaald {   // U-TABS: in the Vadsrijk-menu
    private static final int RAND = 0xFF3A1F2C, PANEEL = 0xFFF7D4E0, DONKER = 0xFFD99AB2, VAK = 0xFF8B5A6E, VAK_IN = 0xFFB98398, TEKST = 0xFF5A2640;
    private EditBox zoek;
    private Button auto;
    private int scroll;
    private List<BankContents.Entry> zicht = List.of();
    private BankContents laatst;
    private String laatsteZoek = "";

    public MateriaalScherm(MateriaalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, MateriaalMenu.INV_Y + 84);
        this.inventoryLabelY = MateriaalMenu.INV_Y - 11;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        menuPlaats();   // U-TABS
        zoek = new EditBox(font, leftPos + 8, topPos + 16, 98, 11, Component.translatable("vadsrijk.opslag.tas.zoek"));
        zoek.setHint(Component.translatable("vadsrijk.opslag.tas.zoek").withStyle(ChatFormatting.GRAY));
        zoek.setMaxLength(40);
        addRenderableWidget(zoek);
        int x = leftPos + imageWidth + 3, y = topPos + 4;
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.opslag.knop.alles"), b -> stuur(MateriaalMenu.ALLES, ItemStack.EMPTY, 0, false))
                .bounds(x, y, 70, 16).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.alles.uitleg"))).build());
        auto = addRenderableWidget(Button.builder(autoTekst(), b -> stuur(MateriaalMenu.AUTO, ItemStack.EMPTY, 0, false))
                .bounds(x, y + 19, 70, 16).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.auto.uitleg"))).build());
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.opslag.knop.knapzak"), b -> stuur(MateriaalMenu.KNAPZAK, ItemStack.EMPTY, 0, false))
                .bounds(x, y + 38, 70, 16).build());
        ververs(true);
    }


    // ---- U-TABS: the Vadsrijk-menu (frame around the chest look, tabs on top) --------------------------------------------------
    private nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader kader;

    private boolean inMenu() {
        return true;
    }

    @Override
    public float menuSchaal() {
        return inMenu() && kader != null ? kader.schaal() : 1f;
    }

    /** Before the buttons: the frame, the logical size, and the chest centred in it (buttons to its right). */
    private void menuPlaats() {
        if (!inMenu()) {
            return;
        }
        var win = net.minecraft.client.Minecraft.getInstance().getWindow();
        int boven = nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.BAND + 18;
        kader = nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.kader(win.getGuiScaledWidth(), win.getGuiScaledHeight(), imageWidth + 2 * 80, imageHeight + boven + 8);
        width = kader.lw();
        height = kader.lh();
        leftPos = kader.left() + (kader.w() - imageWidth) / 2;
        topPos = kader.top() + boven + Math.max(0, (kader.h() - boven - 8 - imageHeight) / 2);
    }

    private boolean inKader(double mx, double my) {
        return kader != null && mx >= kader.left() && mx < kader.left() + kader.w() && my >= kader.top() - nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.TABS_H
                && my < kader.top() + kader.h();
    }

    private Component autoTekst() {
        return Component.translatable(menu.auto() ? "vadsrijk.opslag.knop.auto.aan" : "vadsrijk.opslag.knop.auto.uit");
    }

    private void stuur(int actie, ItemStack soort, int knop, boolean shift) {
        ClientPacketDistributor.sendToServer(new OpslagPayloads.TasActie(menu.containerId, actie, soort, knop, shift));
    }

    private void ververs(boolean altijd) {
        BankContents b = menu.inhoud();
        String q = zoek == null ? "" : zoek.getValue().trim().toLowerCase(Locale.ROOT);
        if (!altijd && b == laatst && q.equals(laatsteZoek)) {
            return;
        }
        laatst = b;
        laatsteZoek = q;
        List<BankContents.Entry> l = new ArrayList<>();
        for (BankContents.Entry e : b.entries()) {
            if (q.isEmpty() || e.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                l.add(e);
            }
        }
        l.sort(Comparator.comparing((BankContents.Entry e) -> e.item().getHoverName().getString()));
        zicht = l;
        scroll = Mth.clamp(scroll, 0, maxScroll());
        if (auto != null) {
            auto.setMessage(autoTekst());
        }
    }

    private int maxScroll() {
        return Math.max(0, (zicht.size() + MateriaalMenu.KOLOMMEN - 1) / MateriaalMenu.KOLOMMEN - MateriaalMenu.RIJEN);
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        ververs(false);
        super.extractRenderState(g, mx, my, a);
        int i = index(mx, my);
        if (i >= 0 && menu.getCarried().isEmpty()) {
            BankContents.Entry e = zicht.get(i);
            List<Component> regels = new ArrayList<>(getTooltipFromContainerItem(e.item()));
            regels.add(Component.translatable("vadsrijk.opslag.tas.aantal", String.format(Locale.ROOT, "%,d", e.count())).withStyle(ChatFormatting.LIGHT_PURPLE));
            regels.add(Component.translatable("vadsrijk.opslag.tas.klik").withStyle(ChatFormatting.GRAY));
            g.setTooltipForNextFrame(font, regels, e.item().getTooltipImage(), e.item(), mx, my);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        super.extractBackground(g, mx, my, a);
        if (kader != null) {   // U-TABS
            nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.tekenKader(g, font, kader.left(), kader.top(), kader.w(), kader.h(), Component.translatable("vadsrijk.menu.tab.knapzak"), "materiaaltas", mx, my);
        }
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, RAND);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEEL);
        for (int r = 0; r < MateriaalMenu.RIJEN; r++) {
            for (int c = 0; c < MateriaalMenu.KOLOMMEN; c++) {
                vak(g, x + MateriaalMenu.GRID_X - 1 + c * 18, y + MateriaalMenu.GRID_Y - 1 + r * 18);
            }
        }
        int bx = x + MateriaalMenu.GRID_X + MateriaalMenu.KOLOMMEN * 18, by = y + MateriaalMenu.GRID_Y - 1, bh = MateriaalMenu.RIJEN * 18;
        g.fill(bx, by, bx + 4, by + bh, DONKER);
        int rijen = (zicht.size() + MateriaalMenu.KOLOMMEN - 1) / MateriaalMenu.KOLOMMEN;
        int duim = maxScroll() == 0 ? bh : Math.max(8, bh * MateriaalMenu.RIJEN / Math.max(1, rijen));
        int dy = maxScroll() == 0 ? by : by + (bh - duim) * scroll / maxScroll();
        g.fill(bx, dy, bx + 4, dy + duim, VAK);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                vak(g, x + 7 + c * 18, y + MateriaalMenu.INV_Y - 1 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            vak(g, x + 7 + c * 18, y + MateriaalMenu.INV_Y + 57);
        }
    }

    private static void vak(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, VAK);
        g.fill(x + 1, y + 1, x + 17, y + 17, VAK_IN);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, title, titleLabelX, titleLabelY, TEKST, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEKST, false);
        long totaal = menu.inhoud().totalItems();
        String s = Component.translatable("vadsrijk.opslag.tas.samen", BankGuhScreen.shortCount(totaal), menu.inhoud().entries().size()).getString();
        g.text(font, s, imageWidth - 8 - font.width(s), titleLabelY, 0xFF8B4A68, false);
        int start = scroll * MateriaalMenu.KOLOMMEN;
        for (int i = 0; i < MateriaalMenu.KOLOMMEN * MateriaalMenu.RIJEN && start + i < zicht.size(); i++) {
            BankContents.Entry e = zicht.get(start + i);
            int sx = MateriaalMenu.GRID_X + (i % MateriaalMenu.KOLOMMEN) * 18, sy = MateriaalMenu.GRID_Y + (i / MateriaalMenu.KOLOMMEN) * 18;
            g.item(e.item(), sx, sy);
            String n = BankGuhScreen.shortCount(e.count());
            g.pose().pushMatrix();
            g.pose().scale(0.66f, 0.66f);
            g.text(font, n, (int) ((sx + 17 - font.width(n) * 0.66f) / 0.66f), (int) ((sy + 11) / 0.66f), 0xFFFFFFFF, true);
            g.pose().popMatrix();
            if (isHovering(sx - 1, sy - 1, 18, 18, mx, my)) {
                g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
            }
        }
        if (zicht.isEmpty()) {
            Component leeg = Component.translatable(laatsteZoek.isEmpty() ? "vadsrijk.opslag.tas.leeg" : "vadsrijk.opslag.tas.niets_gevonden");
            for (var r : font.split(leeg, MateriaalMenu.KOLOMMEN * 18 - 8)) {
                g.text(font, r, MateriaalMenu.GRID_X + 4, MateriaalMenu.GRID_Y + 30, 0xFF8B4A68, false);
                break;
            }
        }
        String hint = Component.translatable(menu.auto() ? "vadsrijk.opslag.tas.auto_aan" : "vadsrijk.opslag.tas.auto_uit").getString();
        g.pose().pushMatrix();
        g.pose().scale(0.75f, 0.75f);
        g.text(font, hint, (int) (MateriaalMenu.GRID_X / 0.75f), (int) ((MateriaalMenu.GRID_Y + MateriaalMenu.RIJEN * 18 + 3) / 0.75f), 0xFF8B4A68, false);
        g.pose().popMatrix();
    }

    // ---- input ---------------------------------------------------------------------------------------------------------------

    private int index(double mx, double my) {
        double gx = mx - leftPos - MateriaalMenu.GRID_X, gy = my - topPos - MateriaalMenu.GRID_Y;
        if (gx < 0 || gy < 0 || gx >= MateriaalMenu.KOLOMMEN * 18 || gy >= MateriaalMenu.RIJEN * 18) {
            return -1;
        }
        int i = (scroll + (int) gy / 18) * MateriaalMenu.KOLOMMEN + (int) gx / 18;
        return i < zicht.size() ? i : -2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (kader != null && event.button() == 0 && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.klik(font, event.x(), event.y(), kader.left(), kader.top(), kader.w(),
                font.width(Component.translatable("vadsrijk.menu.tab.knapzak").withStyle(ChatFormatting.BOLD)), "materiaaltas")) {   // U-TABS
            return true;
        }
        int i = index(event.x(), event.y());
        if (i != -1 && (event.button() == 0 || event.button() == 1)) {
            if (!menu.getCarried().isEmpty()) {
                stuur(MateriaalMenu.STOP_HAND, ItemStack.EMPTY, event.button(), false);
            } else if (i >= 0) {
                stuur(MateriaalMenu.NEEM, zicht.get(i).item(), event.button(), Minecraft.getInstance().hasShiftDown());
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        if (mx >= leftPos + imageWidth && mx < leftPos + imageWidth + 76 && my >= topPos && my < topPos + 60) {
            return false;
        }
        if (inKader(mx, my)) {
            return false;   // U-TABS: (nor anywhere in the menu frame)
        }
        return super.hasClickedOutside(mx, my, xo, yo);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (maxScroll() > 0 && my < topPos + MateriaalMenu.GRID_Y + MateriaalMenu.RIJEN * 18) {
            scroll = Mth.clamp(scroll - (int) Math.signum(sy), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (zoek.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            zoek.keyPressed(event);
            return true;
        }
        if (OpslagClient.KNAPZAK.matches(event)) {
            stuur(MateriaalMenu.KNAPZAK, ItemStack.EMPTY, 0, false);
            return true;
        }
        if (nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.toetsInMenu(this, event, "materiaaltas")) {   // U-TABS
            return true;
        }
        return super.keyPressed(event);
    }
}
