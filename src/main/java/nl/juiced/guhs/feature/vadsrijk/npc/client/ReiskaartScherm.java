package nl.juiced.guhs.feature.vadsrijk.npc.client;

import java.util.Map;

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

/**
 * The airship's travel map (fix round 2, g-ux): an illustrated map of the Vadsrijk (art: tools/features/vadsrijk_reiskaart_art.py,
 * 768x480, drawn 1 image pixel = 1 GUI pixel). Hold the mouse and drag to pan (clamped to the map). Each place has a round badge
 * with an icon and its name; dotted airship routes connect them; the place you are at has a pulsing marker. Places you have not
 * reached through the story (and the teasers zone 3 / zone 4) are greyed with a big "?", their silhouette still visible.
 * Clicking an open place sends {@link NpcPayloads.ReisKies}; the server checks and flies (the existing flight scenes).
 */
public final class ReiskaartScherm extends Screen {
    static final Identifier KAART = Guhs.id("textures/gui/vadsrijk/reiskaart.png");
    static final int KW = 768, KH = 480;
    /** Place -> its spot on the image (fractions) and the half size of its land (for the grey veil). Keep in sync with the art. */
    static final Map<String, float[]> PLEKKEN = Map.of(
            "knuffelkamp", new float[] {0.12f, 0.80f, 56, 38},
            "zwamzoet", new float[] {0.30f, 0.50f, 132, 92},
            "kandijkom", new float[] {0.52f, 0.22f, 104, 72},
            "schemerwortel", new float[] {0.77f, 0.56f, 150, 108},
            "zone4", new float[] {0.93f, 0.14f, 52, 30},
            "hoogvads", new float[] {0.97f, 0.05f, 36, 22});   // T-Z5VERHAAL (no art yet: a marker in the top-right corner)
    /** The airship routes (dotted). (R-INT, WERELDINDELING §4) line 0 camp-Haven, A Zwamzoet-Kandijkom, B Galmdok (zone 2) -> Weststeiger
     *  (zone 3), C Oosttaksteiger (zone 3) -> Lampionhaven (zone 4). No direct line Zwamzoet-Schemerwortel any more. */
    static final String[][] ROUTES = {{"knuffelkamp", "zwamzoet"}, {"zwamzoet", "kandijkom"}, {"kandijkom", "schemerwortel"}, {"schemerwortel", "zone4"}, {"zone4", "hoogvads"}};   // T-Z5VERHAAL: + zone4-hoogvads
    private static final int INKT = 0xFF4A3A55, PAPIER = 0xF2FFF6EC, ROZE = 0xFFF2A9C4, GRIJS = 0xFF9A93A2, GOUD = 0xFFFFD54A;

    private final NpcPayloads.Reiskaart kaart;
    private float camX, camY;
    private boolean druk;
    private double sleep;
    @Nullable
    private String hover;

    public ReiskaartScherm(NpcPayloads.Reiskaart kaart) {
        super(Component.translatable("vadsrijk.npc.reiskaart.titel"));
        this.kaart = kaart;
    }

    // ---- layout -----------------------------------------------------------------------------------------------------------------

    private int vx0() {
        return 12;
    }

    private int vy0() {
        return 30;
    }

    private int vx1() {
        return width - 12;
    }

    private int vy1() {
        return height - 30;
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
        float[] h = PLEKKEN.getOrDefault(kaart.hier(), PLEKKEN.get("zwamzoet"));
        camX = h[0] * KW - (vx1() - vx0()) / 2f;
        camY = h[1] * KH - (vy1() - vy0()) / 2f;
        klem();
    }

    private void klem() {
        int vw = vx1() - vx0(), vh = vy1() - vy0();
        camX = vw >= KW ? (KW - vw) / 2f : Mth.clamp(camX, 0, KW - vw);
        camY = vh >= KH ? (KH - vh) / 2f : Mth.clamp(camY, 0, KH - vh);
    }

    /** Screen x/y of an image point. */
    private int sx(float fx) {
        return Math.round(vx0() - camX + fx * KW);
    }

    private int sy(float fy) {
        return Math.round(vy0() - camY + fy * KH);
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
        g.fill(vx0() - 3, vy0() - 3, vx1() + 3, vy1() + 3, ROZE);
        g.enableScissor(vx0(), vy0(), vx1(), vy1());
        g.blit(RenderPipelines.GUI_TEXTURED, KAART, sx(0), sy(0), 0f, 0f, KW, KH, KW, KH);
        // the grey veil over the places you have not reached (and the teasers): the silhouette stays visible
        for (NpcPayloads.ReisPlek p : kaart.plekken()) {
            if (p.staat() == Reiskaart.ONBEKEND || p.staat() == Reiskaart.TEASER) {
                float[] d = PLEKKEN.get(p.id());
                if (d != null) {
                    sluier(g, sx(d[0]), sy(d[1]), d[2] * 1.05f, d[3] * 1.08f, 0x9A6E6878);
                }
            }
        }
        // the airship routes, dotted (bright between two open places)
        long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        for (String[] r : ROUTES) {
            NpcPayloads.ReisPlek a0 = plek(r[0]), b0 = plek(r[1]);
            float[] a1 = PLEKKEN.get(r[0]), b1 = PLEKKEN.get(r[1]);
            if (a0 == null || b0 == null || a1 == null || b1 == null) {
                continue;
            }
            boolean open = a0.staat() <= Reiskaart.HIER && b0.staat() <= Reiskaart.HIER;
            route(g, sx(a1[0]), sy(a1[1]), sx(b1[0]), sy(b1[1]), open ? 0xFFFFFFFF : 0x90E0DCE6, open ? (int) (t % 8) : 0);
        }
        g.disableScissor();
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
        g.enableScissor(vx0(), vy0(), vx1(), vy1());
        long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        for (NpcPayloads.ReisPlek p : kaart.plekken()) {
            float[] d = PLEKKEN.get(p.id());
            if (d == null) {
                continue;
            }
            int x = sx(d[0]), y = sy(d[1]);
            boolean over = Math.abs(mouseX - x) <= 13 && Math.abs(mouseY - y) <= 13 && mouseY >= vy0() && mouseY <= vy1();
            if (over) {
                hover = p.id();
            }
            badge(g, p, x, y, over, t, a);
        }
        g.disableScissor();
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
        if (hover != null) {
            NpcPayloads.ReisPlek p = plek(hover);
            if (p != null) {
                Component n = naam(p), r = regel(p);
                int w = Math.max(font.width(n), font.width(r)) + 8;
                int tx = Math.min(mouseX + 12, width - w - 2), ty = Math.max(2, mouseY - 26);
                g.fill(tx, ty, tx + w, ty + 23, 0xF0301A26);
                g.fill(tx, ty, tx + 2, ty + 23, ROZE);
                g.text(font, n.copy().withStyle(ChatFormatting.BOLD), tx + 5, ty + 3, 0xFFFFE6EE, true);
                g.text(font, r, tx + 5, ty + 13, 0xFFFFFFFF, true);
            }
        }
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
        if (event.button() == 0 && opSluit(event.x(), event.y())) {
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
            if (sleep < 4 && hover != null) {
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
}
