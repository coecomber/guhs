package nl.juiced.guhs.feature.vadsrijk.hud.client;

import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * U-HUD: the HUD editor. Every part of {@link HudIndeling} shows as a box with its name (the real HUD is visible behind it).
 * Drag = move (snaps to the screen edges/middle, other parts and a 4 px grid; hold Shift for free), scroll = bigger/smaller,
 * right click = that part back to normal, R over a part = the same. Buttons: "Alles terug" and "Klaar". Saved on close.
 */
public final class HudBewerker extends Screen {
    private static final int SNAP = 4;
    @Nullable
    private final Screen terug;
    @Nullable
    private String sleept;
    private double grijpX, grijpY;
    @Nullable
    private String zweeft;
    private boolean bevestigReset;

    public HudBewerker(@Nullable Screen terug) {
        super(Component.translatable("vadsrijk.uhud.bewerker"));
        this.terug = terug;
    }

    @Override
    protected void init() {
        int bw = 90, y = height - 26;
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.uhud.bewerker.alles_terug"), b -> {
            if (bevestigReset) {
                HudIndeling.resetAlles();
                bevestigReset = false;
                b.setMessage(Component.translatable("vadsrijk.uhud.bewerker.alles_terug"));
            } else {
                bevestigReset = true;
                b.setMessage(Component.translatable("vadsrijk.uhud.bewerker.zeker"));
            }
        }).bounds(width / 2 - bw - 2, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("vadsrijk.uhud.bewerker.klaar"), b -> onClose())
                .bounds(width / 2 + 2, y, bw, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        HudIndeling.bewaar();
        if (minecraft != null) {
            minecraft.setScreen(terug);
        }
    }

    @Override
    public void removed() {
        HudIndeling.bewaar();
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0x38000000);   // (no blur: the HUD itself must stay visible)
        // the middle lines (the snap guides)
        g.fill(width / 2, 0, width / 2 + 1, height, 0x30FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x30FFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        zweeft = sleept != null ? sleept : onder(mouseX, mouseY);
        for (HudIndeling.Deel d : HudIndeling.delen()) {
            float[] b = HudIndeling.huidig(d.id(), width, height);
            int x0 = (int) b[0], y0 = (int) b[1], x1 = (int) Math.ceil(b[0] + b[2]), y1 = (int) Math.ceil(b[1] + b[3]);
            boolean aan = d.id().equals(zweeft);
            boolean veranderd = !HudIndeling.stand(d.id()).standaard();
            int rand = d.id().equals(sleept) ? 0xFFFF7AC8 : aan ? 0xFFFFD24A : veranderd ? 0xC0A0E0FF : 0x90FFFFFF;
            g.fill(x0, y0, x1, y1, aan ? 0x40FFD24A : 0x20FFFFFF);
            g.fill(x0, y0, x1, y0 + 1, rand);
            g.fill(x0, y1 - 1, x1, y1, rand);
            g.fill(x0, y0, x0 + 1, y1, rand);
            g.fill(x1 - 1, y0, x1, y1, rand);
            g.pose().pushMatrix();
            g.pose().translate(x0 + 2, y0 + 2);
            g.pose().scale(0.6f, 0.6f);
            g.text(font, d.naam(), 0, 0, aan ? 0xFFFFFFFF : 0xC0FFFFFF, true);
            g.pose().popMatrix();
        }
        // the hint line + what is under the mouse
        g.centeredText(font, Component.translatable("vadsrijk.uhud.bewerker.uitleg"), width / 2, 4, 0xFFFFE8F4);
        if (zweeft != null) {
            HudIndeling.Deel d = HudIndeling.deel(zweeft);
            HudIndeling.Stand s = HudIndeling.stand(zweeft);
            if (d != null) {
                Component c = Component.translatable("vadsrijk.uhud.bewerker.deel", d.naam(), Math.round(s.schaal() * 100));
                g.centeredText(font, c, width / 2, height - 38, 0xFFFFD24A);
            }
        }
    }

    /** The smallest part under the mouse (small parts lie on top of big ones). */
    @Nullable
    private String onder(double mx, double my) {
        String beste = null;
        float opp = Float.MAX_VALUE;
        for (HudIndeling.Deel d : HudIndeling.delen()) {
            float[] b = HudIndeling.huidig(d.id(), width, height);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3] && b[2] * b[3] < opp) {
                opp = b[2] * b[3];
                beste = d.id();
            }
        }
        return beste;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        String id = onder(event.x(), event.y());
        if (id == null) {
            return false;
        }
        if (event.button() == 1) {
            HudIndeling.reset(id);
            return true;
        }
        if (event.button() == 0) {
            float[] b = HudIndeling.huidig(id, width, height);
            sleept = id;
            grijpX = event.x() - b[0];
            grijpY = event.y() - b[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (sleept == null) {
            return super.mouseDragged(event, dx, dy);
        }
        int[] b = HudIndeling.standaardBox(sleept, width, height);
        HudIndeling.Stand s = HudIndeling.stand(sleept);
        float bw = b[2] * s.schaal(), bh = b[3] * s.schaal();
        float x = (float) (event.x() - grijpX), y = (float) (event.y() - grijpY);
        if (!event.hasShiftDown()) {
            x = snap(x, bw, width, true);
            y = snap(y, bh, height, false);
        }
        x = Mth.clamp(x, 0, Math.max(0, width - bw));
        y = Mth.clamp(y, 0, Math.max(0, height - bh));
        HudIndeling.zet(sleept, Math.round(x) - b[0], Math.round(y) - b[1], s.schaal());
        return true;
    }

    /** Snaps one side of a box: screen edge, middle, the other parts' edges, else a 4 px grid. */
    private float snap(float v, float maat, int scherm, boolean xas) {
        float beste = Float.NaN, afstand = SNAP + 0.01f;
        float[] kandidaten = {0, scherm - maat, scherm / 2f - maat / 2f};
        for (float k : kandidaten) {
            if (Math.abs(v - k) < afstand) {
                afstand = Math.abs(v - k);
                beste = k;
            }
        }
        List<HudIndeling.Deel> delen = HudIndeling.delen();
        for (HudIndeling.Deel d : delen) {
            if (d.id().equals(sleept)) {
                continue;
            }
            float[] o = HudIndeling.huidig(d.id(), width, height);
            float a = xas ? o[0] : o[1], e = xas ? o[0] + o[2] : o[1] + o[3];
            for (float k : new float[] {a, e, a - maat, e - maat}) {
                if (Math.abs(v - k) < afstand) {
                    afstand = Math.abs(v - k);
                    beste = k;
                }
            }
        }
        return Float.isNaN(beste) ? Math.round(v / SNAP) * SNAP : beste;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (sleept != null) {
            sleept = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        String id = onder(mx, my);
        if (id == null || sy == 0) {
            return false;
        }
        HudIndeling.Stand s = HudIndeling.stand(id);
        float n = Math.round((s.schaal() + (sy > 0 ? 0.1f : -0.1f)) * 10) / 10f;
        HudIndeling.zet(id, s.dx(), s.dy(), n);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_R && zweeft != null) {
            HudIndeling.reset(zweeft);
            return true;
        }
        return super.keyPressed(event);
    }
}
