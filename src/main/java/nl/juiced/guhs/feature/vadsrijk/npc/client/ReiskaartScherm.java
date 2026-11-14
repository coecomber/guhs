package nl.juiced.guhs.feature.vadsrijk.npc.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;
import nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart;
import nl.juiced.guhs.feature.vadsrijk.npc.ReiskaartIndeling;

/**
 * The airship's travel map (fix round 2, g-ux; reworked in 1.10.3): an illustrated map of the WHOLE Vadsrijk, every zone at its real
 * spot (art: tools/features/vadsrijk_reiskaart_art.py; layout + world -> image mapping: {@link ReiskaartIndeling}, drawn 1 image
 * pixel = 1 GUI pixel). Hold the mouse and drag to pan (clamped to the map). Each place has a round badge with an icon and its
 * name; dotted airship routes connect them; the place you are at has a pulsing marker. Places you have not reached through the
 * story (and the zones closed for you) are greyed with a big "?" under a grey mist, their silhouette still visible. 1.10.3: small
 * area names, your lit campfires (tiny flames) and your real position (an arrow) for the zones open to you (the server sends only
 * those). Clicking an open place sends {@link NpcPayloads.ReisKies}; the server checks and flies (the existing flight scenes).
 */
public final class ReiskaartScherm extends Screen implements nl.juiced.guhs.feature.vadsrijk.menu.client.Geschaald {   // U-TABS: Geschaald
    static final Identifier KAART = Guhs.id("textures/gui/vadsrijk/reiskaart.png");
    static final int KW = ReiskaartIndeling.W, KH = ReiskaartIndeling.H;
    private static final int INKT = 0xFF4A3A55, PAPIER = 0xF2FFF6EC, ROZE = 0xFFF2A9C4, GRIJS = 0xFF9A93A2, GOUD = 0xFFFFD54A;

    private final NpcPayloads.Reiskaart kaart;
    private float camX, camY;
    private boolean druk;
    private double sleep;
    @Nullable
    private String hover;
    /** 1.10.3: the tooltip of a campfire / your own marker under the mouse (no click action). */
    @Nullable
    private Component hoverTip;

    public ReiskaartScherm(NpcPayloads.Reiskaart kaart) {
        this(kaart, false);
    }

    /** U-TABS: in the Vadsrijk-menu (tab Reiskaart): the map in the menu frame, look only (flying = at an airship NPC). */
    private final boolean menu;
    private nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader kader;

    public ReiskaartScherm(NpcPayloads.Reiskaart kaart, boolean menu) {
        super(Component.translatable("vadsrijk.npc.reiskaart.titel"));
        this.kaart = kaart;
        this.menu = menu;
    }

    @Override
    public float menuSchaal() {
        return menu && kader != null ? kader.schaal() : 1f;
    }

    // ---- layout -----------------------------------------------------------------------------------------------------------------

    private int vx0() {
        return menu ? kader.left() + 4 : 12;
    }

    private int vy0() {
        return menu ? kader.top() + nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.BAND + 3 : 30;
    }

    private int vx1() {
        return menu ? kader.left() + kader.w() - 4 : width - 12;
    }

    private int vy1() {
        return menu ? kader.top() + kader.h() - 14 : height - 30;
    }

    /** The close button (the ✕, top right in the title band). */
    private static final int SLUIT = 16;

    private int sluitX() {
        return vx1() - SLUIT;
    }

    private int sluitY() {
        return (vy0() - SLUIT) / 2 - 1;
    }

    private boolean opSluit(double mx, double my) {
        return mx >= sluitX() && mx < sluitX() + SLUIT && my >= sluitY() && my < sluitY() + SLUIT;
    }

    @Override
    protected void init() {
        if (menu) {   // U-TABS
            kader = nl.juiced.guhs.feature.vadsrijk.menu.client.MenuClient.kader();
            width = kader.lw();
            height = kader.lh();
        }
        float fx, fy;
        ReiskaartIndeling.Plek h = ReiskaartIndeling.plek(kaart.hier());
        if (h != null) {   // centred on you (1.10.3: your real spot), else on De Haven
            fx = ReiskaartIndeling.px(kaart.x());
            fy = ReiskaartIndeling.py(kaart.z(), h.zone());
        } else {
            h = ReiskaartIndeling.plek("zwamzoet");
            fx = h.bx();
            fy = h.by();
        }
        camX = fx - (vx1() - vx0()) / 2f;
        camY = fy - (vy1() - vy0()) / 2f;
        klem();
    }

    private void klem() {
        int vw = vx1() - vx0(), vh = vy1() - vy0();
        camX = vw >= KW ? (KW - vw) / 2f : Mth.clamp(camX, 0, KW - vw);
        camY = vh >= KH ? (KH - vh) / 2f : Mth.clamp(camY, 0, KH - vh);
    }

    /** Screen x/y of an image point (image pixels). */
    private int sx(float ix) {
        return Math.round(vx0() - camX + ix);
    }

    private int sy(float iy) {
        return Math.round(vy0() - camY + iy);
    }

    private boolean inVenster(double mx, double my) {
        return mx >= vx0() && mx < vx1() && my >= vy0() && my < vy1();
    }

    @Nullable
    private NpcPayloads.ReisPlek plek(String id) {
        for (NpcPayloads.ReisPlek p : kaart.plekken()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    // ---- drawing ----------------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0xE0281A30);
        if (menu) {   // U-TABS: the menu frame around the map
            nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.tekenKader(g, font, kader.left(), kader.top(), kader.w(), kader.h(), title,
                    "reiskaart", mouseX, mouseY);
        }
        g.fill(vx0() - (menu ? 1 : 3), vy0() - (menu ? 1 : 3), vx1() + (menu ? 1 : 3), vy1() + (menu ? 1 : 3), ROZE);
        g.enableScissor(vx0(), vy0(), vx1(), vy1());
        g.blit(RenderPipelines.GUI_TEXTURED, KAART, sx(0), sy(0), 0f, 0f, KW, KH, KW, KH);
        // the grey mist over the places you have not reached and the zones closed for you: the silhouette stays visible
        for (NpcPayloads.ReisPlek p : kaart.plekken()) {
            ReiskaartIndeling.Plek d = ReiskaartIndeling.plek(p.id());
            if (d != null && (p.staat() == Reiskaart.ONBEKEND || p.staat() == Reiskaart.TEASER)) {
                mist(g, d, p.staat() == Reiskaart.TEASER);
            }
        }
        // the airship routes, dotted (bright between two open places)
        long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        for (String[] r : ReiskaartIndeling.ROUTES) {
            NpcPayloads.ReisPlek a0 = plek(r[0]), b0 = plek(r[1]);
            ReiskaartIndeling.Plek a1 = ReiskaartIndeling.plek(r[0]), b1 = ReiskaartIndeling.plek(r[1]);
            if (a0 == null || b0 == null || a1 == null || b1 == null) {
                continue;
            }
            boolean open = a0.staat() <= Reiskaart.HIER && b0.staat() <= Reiskaart.HIER;
            route(g, sx(a1.bx()), sy(a1.by()), sx(b1.bx()), sy(b1.by()), open ? 0xFFFFFFFF : 0x90E0DCE6, open ? (int) (t % 8) : 0);
        }
        // 1.10.3: the area names (small, ink), only for zones open for you (the server sends only those)
        for (NpcPayloads.ReisNaam n : kaart.namen()) {
            Component c = Component.translatable("vadsrijk.kern.gebied." + n.gebied());
            int x = sx(ReiskaartIndeling.px(n.x())), y = sy(ReiskaartIndeling.py(n.z(), n.zone()));
            int w = font.width(c);
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(0.5f, 0.5f);
            g.text(font, c, -w / 2 + 1, -3, 0xD0FFFBF4, false);
            g.text(font, c, -w / 2, -4, 0xFF3E3048, false);
            g.pose().popMatrix();
        }
        g.disableScissor();
    }

    /** The grey mist over a place's land box (two soft ellipses; zone 5's land is drawn floating). */
    private void mist(GuiGraphicsExtractor g, ReiskaartIndeling.Plek d, boolean dicht) {
        int[] b = d.land();
        float x0 = ReiskaartIndeling.px(b[0]), x1 = ReiskaartIndeling.px(b[2]);
        float y0 = ReiskaartIndeling.py(b[1], d.zone()), y1 = ReiskaartIndeling.py(b[3], d.zone());
        int cx = sx((x0 + x1) / 2), cy = sy((y0 + y1) / 2);
        float rx = (x1 - x0) / 2f * 1.12f + 6, ry = (y1 - y0) / 2f * 1.15f + 6;
        sluier(g, cx, cy, rx, ry, dicht ? 0x8A746E80 : 0x70746E80);
        sluier(g, cx, cy, rx * 0.8f, ry * 0.8f, dicht ? 0x50857F90 : 0x30857F90);
    }

    /** A filled ellipse made of horizontal lines. */
    private static void sluier(GuiGraphicsExtractor g, int cx, int cy, float rx, float ry, int kleur) {
        for (int dy = (int) -ry; dy <= ry; dy++) {
            int w = (int) (rx * Math.sqrt(Math.max(0, 1 - (dy * dy) / (ry * ry))));
            g.fill(cx - w, cy + dy, cx + w, cy + dy + 1, kleur);
        }
    }

    private static void route(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int kleur, int fase) {
        double l = Math.hypot(x1 - x0, y1 - y0);
        // a gentle arc: the airship flies a curve
        double nx = -(y1 - y0) / l, ny = (x1 - x0) / l, bocht = l * 0.12;
        for (double s = 26 + fase; s < l - 26; s += 8) {
            double f = s / l, boog = Math.sin(f * Math.PI) * bocht;
            int x = (int) (x0 + (x1 - x0) * f + nx * boog), y = (int) (y0 + (y1 - y0) * f + ny * boog);
            g.fill(x - 1, y - 1, x + 2, y + 2, kleur);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        hover = null;
        hoverTip = null;
        g.enableScissor(vx0(), vy0(), vx1(), vy1());
        long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        boolean binnen = inVenster(mouseX, mouseY);
        // 1.10.3: your lit campfires (tiny flames)
        for (NpcPayloads.ReisVuur v : kaart.vuren()) {
            int x = sx(ReiskaartIndeling.px(v.x())), y = sy(ReiskaartIndeling.py(v.z(), v.zone()));
            vuurtje(g, x, y, t);
            if (binnen && Math.abs(mouseX - x) <= 4 && mouseY >= y - 6 && mouseY <= y + 2) {
                hoverTip = Component.translatable("vadsrijk.npc.reiskaart.kampvuur");
            }
        }
        for (NpcPayloads.ReisPlek p : kaart.plekken()) {
            ReiskaartIndeling.Plek d = ReiskaartIndeling.plek(p.id());
            if (d == null) {
                continue;
            }
            int x = sx(d.bx()), y = sy(d.by());
            boolean over = binnen && Math.abs(mouseX - x) <= 13 && Math.abs(mouseY - y) <= 13;
            if (over) {
                hover = p.id();
            }
            badge(g, p, x, y, over, t, a);
        }
        // 1.10.3: your real position (an arrow that looks where you look)
        ReiskaartIndeling.Plek hier = ReiskaartIndeling.plek(kaart.hier());
        if (hier != null) {
            int x = sx(ReiskaartIndeling.px(kaart.x())), y = sy(ReiskaartIndeling.py(kaart.z(), hier.zone()));
            float yaw = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 180f;
            pijl(g, x, y, yaw, t, a);
            if (binnen && hover == null && Math.abs(mouseX - x) <= 6 && Math.abs(mouseY - y) <= 6) {
                hoverTip = Component.translatable("vadsrijk.npc.reiskaart.jij");
            }
        }
        g.disableScissor();
        if (menu) {   // U-TABS: no own title / close button; a hint line: look only
            g.centeredText(font, Component.translatable("vadsrijk.menu.reiskaart.uitleg"), kader.left() + kader.w() / 2, vy1() + 3, 0xFF8A5A84);
            hoverKaartje(g, mouseX, mouseY);
            return;
        }
        // title and the hint line
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 11, 0xFFFFE6EE);
        g.centeredText(font, Component.translatable("vadsrijk.npc.reiskaart.uitleg"), width / 2, height - 20, 0xFFE6D6EE);
        // (fix round 3) the close button, top right
        boolean overX = opSluit(mouseX, mouseY);
        int kx = sluitX(), ky = sluitY();
        g.fill(kx - 1, ky - 1, kx + SLUIT + 1, ky + SLUIT + 1, INKT);
        g.fill(kx, ky, kx + SLUIT, ky + SLUIT, overX ? ROZE : PAPIER);
        g.centeredText(font, Component.literal("✕"), kx + SLUIT / 2, ky + (SLUIT - 8) / 2, overX ? 0xFFFFFFFF : INKT);
        if (overX) {
            g.setTooltipForNextFrame(font, Component.translatable("vadsrijk.npc.reiskaart.sluit"), mouseX, mouseY);
        }
        hoverKaartje(g, mouseX, mouseY);
    }

    private void hoverKaartje(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (hover == null && hoverTip != null) {
            g.setTooltipForNextFrame(font, hoverTip, mouseX, mouseY);
        }
        if (hover != null) {
            NpcPayloads.ReisPlek p = plek(hover);
            if (p != null) {
                Component n = naam(p), r = menu && p.staat() == Reiskaart.OPEN
                        ? Component.translatable("vadsrijk.menu.reiskaart.open").withStyle(ChatFormatting.LIGHT_PURPLE) : regel(p);
                int w = Math.max(font.width(n), font.width(r)) + 8;
                int tx = Math.min(mouseX + 12, width - w - 2), ty = Math.max(2, mouseY - 26);
                g.fill(tx, ty, tx + w, ty + 23, 0xF0301A26);
                g.fill(tx, ty, tx + 2, ty + 23, ROZE);
                g.text(font, n.copy().withStyle(ChatFormatting.BOLD), tx + 5, ty + 3, 0xFFFFE6EE, true);
                g.text(font, r, tx + 5, ty + 13, 0xFFFFFFFF, true);
            }
        }
    }

    /** 1.10.3: a tiny campfire: logs and a flickering flame (about 8x8 GUI pixels, standing on y). */
    private static void vuurtje(GuiGraphicsExtractor g, int x, int y, long t) {
        g.fill(x - 4, y, x + 4, y + 2, INKT);
        g.fill(x - 3, y, x + 3, y + 1, 0xFF8A5A3A);
        boolean flik = ((t / 6) + x) % 2 == 0;
        g.fill(x - 2, y - 3, x + 2, y, 0xFFFF7A3A);
        g.fill(x - 1, y - (flik ? 6 : 5), x + 1, y - 3, 0xFFFF9A40);
        g.fill(x - 1, y - 2, x + 1, y, 0xFFFFE070);
    }

    /** 1.10.3: you: a gold arrow in a white ring, pointing where you look (Minecraft yaw: 0 = south = down on the map). */
    private static void pijl(GuiGraphicsExtractor g, int x, int y, float yaw, long t, float a) {
        float puls = 0.5f + 0.5f * Mth.sin((t + a) * 0.25f);
        sluier(g, x, y, 7 + puls * 3, 7 + puls * 3, ((int) (110 * (1 - puls)) + 40) << 24 | 0xFFFFFF);
        sluier(g, x, y, 6, 6, 0xFFFFFFFF);
        sluier(g, x, y, 5, 5, INKT);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate((float) Math.toRadians(yaw) + (float) Math.PI);
        for (int i = 0; i < 7; i++) {   // the tip points "up" before the rotation
            int w = i / 2 + 1;
            g.fill(-w, -4 + i, w, -3 + i, GOUD);
        }
        g.pose().popMatrix();
    }

    private void badge(GuiGraphicsExtractor g, NpcPayloads.ReisPlek p, int x, int y, boolean over, long t, float a) {
        boolean onbekend = p.staat() >= Reiskaart.ONBEKEND;
        int rand = p.staat() == Reiskaart.HIER ? GOUD : onbekend ? GRIJS : over ? 0xFFFFFFFF : ROZE;
        if (p.staat() == Reiskaart.HIER) {   // you are here: a pulsing ring
            float puls = 0.5f + 0.5f * Mth.sin((t + a) * 0.2f);
            int r = 15 + (int) (puls * 4);
            sluier(g, x, y, r, r, ((int) (90 * (1 - puls)) + 40) << 24 | 0xFFD54A);
        }
        sluier(g, x, y, 12, 12, rand);
        sluier(g, x, y, 10, 10, onbekend ? 0xFF6E6878 : PAPIER);
        if (onbekend) {
            g.pose().pushMatrix();
            g.pose().translate(x, y - 7);
            g.pose().scale(2f, 2f);
            g.centeredText(font, Component.literal("?"), 0, 0, 0xFFFFFFFF);
            g.pose().popMatrix();
        } else {
            g.item(icoon(p.id()), x - 8, y - 8);
        }
        Component n = naam(p);
        int w = font.width(n);
        g.fill(x - w / 2 - 3, y + 14, x + w / 2 + 3, y + 25, onbekend ? 0xC0403848 : 0xD0FFF6EC);
        g.centeredText(font, n, x, y + 16, onbekend ? 0xFFD8D2DE : INKT);
        if (p.staat() == Reiskaart.HIER) {
            Component h = Component.translatable("vadsrijk.npc.reiskaart.jij");
            g.centeredText(font, h, x, y - 24, GOUD);
        }
    }

    static ItemStack icoon(String id) {
        return new ItemStack(switch (id) {
            case "knuffelkamp" -> Items.CAMPFIRE;
            case "zwamzoet" -> Items.RED_MUSHROOM;
            case "kandijkom" -> Items.AMETHYST_CLUSTER;
            case "schemerwortel" -> Items.OAK_SAPLING;
            case "lampionkust" -> Items.LANTERN;
            case "hoogvads" -> Items.FEATHER;
            case "zijdeland" -> Items.STRING;
            default -> Items.MAP;
        });
    }

    static Component naam(NpcPayloads.ReisPlek p) {
        return Component.translatable("vadsrijk.npc.reiskaart.plek." + p.id());
    }

    static Component regel(NpcPayloads.ReisPlek p) {
        return switch (p.staat()) {
            case Reiskaart.HIER -> Component.translatable("vadsrijk.npc.reiskaart.hier").withStyle(ChatFormatting.GOLD);
            case Reiskaart.OPEN -> Component.translatable("vadsrijk.npc.reiskaart.vlieg").withStyle(ChatFormatting.LIGHT_PURPLE);
            default -> Component.translatable(p.reden(), p.arg()).withStyle(ChatFormatting.GRAY);
        };
    }

    // ---- input ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (menu && event.button() == 0 && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.klik(font, event.x(), event.y(), kader.left(), kader.top(),
                kader.w(), font.width(title.copy().withStyle(ChatFormatting.BOLD)), "reiskaart")) {   // U-TABS
            return true;
        }
        if (menu && (event.x() < vx0() || event.x() >= vx1() || event.y() < vy0() || event.y() >= vy1())) {
            return false;
        }
        if (!menu && event.button() == 0 && opSluit(event.x(), event.y())) {
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
            onClose();
            return true;
        }
        if (event.button() == 0) {
            druk = true;
            sleep = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (druk) {
            camX -= (float) dragX;
            camY -= (float) dragY;
            sleep += Math.abs(dragX) + Math.abs(dragY);
            klem();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (druk) {
            druk = false;
            if (sleep < 4 && hover != null && !menu) {   // (U-TABS: the menu's map is look only)
                NpcPayloads.ReisPlek p = plek(hover);
                if (p != null && p.staat() == Reiskaart.OPEN) {
                    ClientPacketDistributor.sendToServer(new NpcPayloads.ReisKies(p.id()));
                    onClose();
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    /** (AutoCheck / tests) Fly to a place as if it was clicked. */
    public void kies(String id) {
        NpcPayloads.ReisPlek p = plek(id);
        if (p != null && p.staat() == Reiskaart.OPEN) {
            ClientPacketDistributor.sendToServer(new NpcPayloads.ReisKies(id));
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** U-TABS: the menu keys (tab keys switch, H closes, Ctrl+Tab). */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (menu && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.toetsInMenu(this, event, "reiskaart")) {
            return true;
        }
        return super.keyPressed(event);
    }
}
