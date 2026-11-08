package nl.juiced.guhs.feature.vadsrijk.opslag.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagMenu;

/**
 * T-OPSLAG: the knapzak / bank screen: the familiar chest look, tab flaps above it (1..5; a locked one shows how to unlock it,
 * a buyable one asks for a second click), and three buttons to the right: Sorteren, Materialen naar de tas, Materiaaltas.
 */
public class OpslagScherm extends AbstractContainerScreen<OpslagMenu> implements nl.juiced.guhs.feature.vadsrijk.menu.client.Geschaald {   // U-TABS: in the Vadsrijk-menu (knapzak only)
    private static final Identifier ACHTERGROND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int FLAP_B = 26, FLAP_H = 15, FLAP_STAP = 28;
    private static final int TAB_AAN = 0xFFC6C6C6, TAB_UIT = 0xFF8B8B8B, TAB_DICHT = 0xFF5A4A55, RAND = 0xFF373737, ROZE = 0xFFFF7FC0;
    private long bevestigTot;
    /** U-OPSLAGVIP: the "WORD GUH VIP++" joke on top of this screen (null = not running). */
    @javax.annotation.Nullable
    private VipGrapOverlay vip;

    public OpslagScherm(OpslagMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 114 + menu.rijen * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        menuPlaats();   // U-TABS
        if (vip == null && menu.info.getBooleanOr("vip_nu", false)) {   // U-OPSLAGVIP (dev command: the joke at once)
            menu.info.remove("vip_nu");
            startVip();
        }
        int x = leftPos + imageWidth + 3, y = topPos + 4;
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.opslag.knop.sorteer"), b -> knop(OpslagMenu.KNOP_SORTEER))
                .bounds(x, y, 66, 16).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.sorteer.uitleg"))).build());
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.opslag.knop.materialen"), b -> knop(OpslagMenu.KNOP_MATERIALEN))
                .bounds(x, y + 19, 66, 16).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.materialen.uitleg"))).build());
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.opslag.knop.tas"), b -> knop(OpslagMenu.KNOP_TAS))
                .bounds(x, y + 38, 66, 16).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.tas.uitleg"))).build());
    }


    // ---- U-TABS: the Vadsrijk-menu (frame around the chest look, tabs on top) --------------------------------------------------
    private nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader kader;

    private boolean inMenu() {
        return menu.soort == OpslagMenu.KNAPZAK;
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

    // ---- U-OPSLAGVIP: the VIP joke --------------------------------------------------------------------------------------------

    private boolean vipActief() {
        return vip != null && vip.actief();
    }

    /** Starts the joke and tells the server at once (once per account, even if the game closes mid-joke). */
    private void startVip() {
        menu.info.putBoolean("vip", true);
        knop(OpslagMenu.KNOP_VIP_GEZIEN);
        long p = prijs() > 0 ? prijs() : menu.info.getLongOr("koop_prijs", 0);
        vip = new VipGrapOverlay(p, () -> knop(OpslagMenu.KNOP_KOOP));
    }

    /** Is tab i one that nothing can unlock yet ("Binnenkort!")? */
    private boolean binnenkort(int i) {
        return i >= menu.openTabs() && i >= menu.info.getIntOr("binnenkort", menu.maxTabs) && !koopbaar(i);
    }

    /** A small padlock centred on (cx, y). */
    private static void slotje(GuiGraphicsExtractor g, int cx, int y, int kleur) {
        g.fill(cx - 2, y, cx + 3, y + 1, kleur);
        g.fill(cx - 3, y + 1, cx - 2, y + 4, kleur);
        g.fill(cx + 3, y + 1, cx + 4, y + 4, kleur);
        g.fill(cx - 4, y + 4, cx + 5, y + 10, kleur);
        g.fill(cx, y + 6, cx + 1, y + 8, 0xFF3A3A3A);
    }

    private void knop(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        super.extractBackground(g, mx, my, a);
        if (inMenu() && kader != null) {   // U-TABS
            nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.tekenKader(g, font, kader.left(), kader.top(), kader.w(), kader.h(), net.minecraft.network.chat.Component.translatable("vadsrijk.menu.tab.knapzak"), "knapzak", mx, my);
        }
        int x = leftPos, y = topPos, rijen = menu.rijen;
        // the tab flaps (behind the panel's top edge, the active one joined to it)
        for (int i = 0; i < menu.maxTabs; i++) {
            int fx = flapX(i), fy = topPos - FLAP_H + 2;
            boolean actief = i == menu.huidigeTab(), open = i < menu.openTabs();
            int kleur = actief ? TAB_AAN : open ? TAB_UIT : binnenkort(i) ? 0xFF4A4448 : TAB_DICHT;   // U-OPSLAGVIP: greyed "Binnenkort!"
            g.fill(fx - 1, fy - 1, fx + FLAP_B + 1, fy + FLAP_H + (actief ? 2 : 0), RAND);
            g.fill(fx, fy, fx + FLAP_B, fy + FLAP_H + (actief ? 2 : 0), kleur);
            if (actief) {
                g.fill(fx, fy, fx + FLAP_B, fy + 2, ROZE);
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, ACHTERGROND, x, y, 0, 0, imageWidth, rijen * 18 + 17, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ACHTERGROND, x, y + rijen * 18 + 17, 0, 126, imageWidth, 96, 256, 256);
        for (int i = 0; i < menu.maxTabs; i++) {   // the flap labels on top
            int fx = flapX(i), fy = topPos - FLAP_H + 2;
            boolean open = i < menu.openTabs();
            if (binnenkort(i)) {   // U-OPSLAGVIP: a little lock instead of "?"
                slotje(g, fx + FLAP_B / 2, fy + 2, 0xFF9A9498);
                if (isHovering(fx - leftPos, fy - topPos, FLAP_B, FLAP_H, mx, my)) {
                    g.fill(fx, fy, fx + FLAP_B, fy + FLAP_H, 0x20FFFFFF);
                }
                continue;
            }
            String s = open ? String.valueOf(i + 1) : koopbaar(i) ? "+" : "?";
            int kleur = open ? (i == menu.huidigeTab() ? 0xFF404040 : 0xFF2A2A2A) : koopbaar(i) ? 0xFFFFD86B : 0xFFB8A8B0;
            g.text(font, s, fx + FLAP_B / 2 - font.width(s) / 2, fy + 4, kleur, false);
            if (isHovering(fx - leftPos, fy - topPos, FLAP_B, FLAP_H, mx, my)) {
                g.fill(fx, fy, fx + FLAP_B, fy + FLAP_H, 0x40FFFFFF);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        super.extractLabels(g, mx, my);
        String tab = Component.translatable("vadsrijk.opslag.tab", menu.huidigeTab() + 1, menu.openTabs()).getString();
        g.text(font, tab, imageWidth - 8 - font.width(tab), titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        if (vipActief()) {   // U-OPSLAGVIP: the joke covers everything (no hover, no tooltips underneath)
            super.extractRenderState(g, -10000, -10000, a);
            vip.teken(g, font, width, height, mx, my);
            return;
        }
        super.extractRenderState(g, mx, my, a);
        int i = flapOnder(mx, my);
        if (i >= 0 && menu.getCarried().isEmpty()) {
            g.setTooltipForNextFrame(font, tabUitleg(i), Optional.empty(), mx, my);
        }
    }

    private List<Component> tabUitleg(int i) {
        List<Component> l = new ArrayList<>();
        if (i < menu.openTabs()) {
            l.add(Component.translatable("vadsrijk.opslag.tab.naar", i + 1, i + 1));
            return l;
        }
        if (binnenkort(i)) {   // U-OPSLAGVIP
            l.add(Component.translatable("vadsrijk.opslag.tab.binnenkort").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            l.add(Component.translatable("vadsrijk.opslag.tab.binnenkort.uitleg").withStyle(ChatFormatting.GRAY));
            return l;
        }
        l.add(Component.translatable("vadsrijk.opslag.tab.dicht").withStyle(ChatFormatting.GOLD));
        if (menu.soort == OpslagMenu.KNAPZAK && menu.info.getList("regels").isPresent()) {
            for (Tag t : menu.info.getList("regels").get()) {
                String s = t.asString().orElse("0");
                boolean klaar = s.startsWith("1");
                String key = s.substring(1);
                Component regel = key.endsWith(".kopen")
                        ? Component.translatable(key, Spelers.munten(menu.info.getLongOr("koop_prijs", prijs())))
                        : Component.translatable(key);
                l.add(Component.literal(klaar ? "✔ " : "• ").append(regel).withStyle(klaar ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        if (koopbaar(i)) {
            boolean bevestig = Util.getMillis() < bevestigTot;
            l.add(Component.translatable(bevestig ? "vadsrijk.opslag.tab.bevestig" : "vadsrijk.opslag.tab.kopen", Spelers.munten(prijs()))
                    .withStyle(bevestig ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.YELLOW));
        } else if (menu.soort == OpslagMenu.BANK) {
            l.add(Component.translatable("vadsrijk.opslag.bank.eerst_vorige").withStyle(ChatFormatting.GRAY));
        }
        return l;
    }

    private long prijs() {
        return menu.info.getLongOr("prijs", 0);
    }

    /** Can this locked tab be bought now? (only the first locked one, and only while a purchase is still open) */
    private boolean koopbaar(int i) {
        return i == menu.openTabs() && prijs() > 0;
    }

    private int flapX(int i) {
        return leftPos + 4 + i * FLAP_STAP;
    }

    private int flapOnder(double mx, double my) {
        int fy = topPos - FLAP_H + 2;
        if (my < fy || my >= fy + FLAP_H) {
            return -1;
        }
        for (int i = 0; i < menu.maxTabs; i++) {
            int fx = flapX(i);
            if (mx >= fx && mx < fx + FLAP_B) {
                return i;
            }
        }
        return -1;
    }

    // ---- input ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (vipActief()) {   // U-OPSLAGVIP
            if (event.button() == 0) {
                vip.klik(event.x(), event.y());
            }
            return true;
        }
        if (inMenu() && kader != null && event.button() == 0 && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.klik(font, event.x(), event.y(), kader.left(), kader.top(), kader.w(),
                font.width(net.minecraft.network.chat.Component.translatable("vadsrijk.menu.tab.knapzak").withStyle(ChatFormatting.BOLD)), "knapzak")) {   // U-TABS
            return true;
        }
        int i = flapOnder(event.x(), event.y());
        if (i >= 0 && event.button() == 0) {
            if (i < menu.openTabs()) {
                if (i != menu.huidigeTab()) {
                    knop(OpslagMenu.KNOP_TAB + i);
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2f));
                }
            } else if (koopbaar(i)) {
                if (!menu.info.getBooleanOr("vip", true)) {   // U-OPSLAGVIP: the very first buy attempt of this account
                    startVip();
                    return true;
                }
                if (Util.getMillis() < bevestigTot) {
                    bevestigTot = 0;
                    knop(OpslagMenu.KNOP_KOOP);
                } else {
                    bevestigTot = Util.getMillis() + 4000;
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return vipActief() || super.mouseReleased(event);   // U-OPSLAGVIP
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return vipActief() || super.mouseDragged(event, dragX, dragY);   // U-OPSLAGVIP
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return vipActief() || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);   // U-OPSLAGVIP
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        if (flapOnder(mx, my) >= 0) {
            return false;   // (a flap is not "outside": never drop the carried item there)
        }
        if (inMenu() && inKader(mx, my)) {
            return false;   // U-TABS: (nor anywhere in the menu frame)
        }
        if (mx >= leftPos + imageWidth && mx < leftPos + imageWidth + 72 && my >= topPos && my < topPos + 60) {
            return false;   // (nor next to the buttons)
        }
        return super.hasClickedOutside(mx, my, xo, yo);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (vipActief()) {   // U-OPSLAGVIP: swallow every key (Esc skips the joke / says no)
            vip.toets(event.key());
            return true;
        }
        if (inMenu() && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.toetsInMenu(this, event, "knapzak")) {   // U-TABS
            return true;
        }
        if (OpslagClient.KNAPZAK.matches(event) && menu.soort == OpslagMenu.KNAPZAK) {
            onClose();
            return true;
        }
        int nr = event.key() - org.lwjgl.glfw.GLFW.GLFW_KEY_1;   // (Ctrl+1..5: switch tab; plain 1..9 stays the vanilla hotbar swap)
        if (event.hasControlDown() && nr >= 0 && nr < menu.openTabs()) {
            knop(OpslagMenu.KNOP_TAB + nr);
            return true;
        }
        return super.keyPressed(event);
    }
}
