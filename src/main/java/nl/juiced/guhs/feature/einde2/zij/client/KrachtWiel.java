package nl.juiced.guhs.feature.einde2.zij.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.einde2.zij.KrachtPayloads;

/**
 * einde2 zij: the element wheel. Opens while the bending key is held: every power you may use here in a ring around the
 * middle; point at one with the mouse and let go of the key (or click) to choose it. The tap of the key then uses it.
 */
public class KrachtWiel extends Screen {
    private static final int STRAAL = 58, VAK = 26;
    private int gewezen = -1;

    public KrachtWiel() {
        super(Component.translatable("gui.guhs.einde2_zij.kracht.wiel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<KrachtPayloads.Regel> lijst() {
        return E2ZijClient.krachten();
    }

    private int plekX(int i, int n) {
        return width / 2 + (int) Math.round(Math.sin(2 * Math.PI * i / n) * STRAAL);
    }

    private int plekY(int i, int n) {
        return height / 2 - (int) Math.round(Math.cos(2 * Math.PI * i / n) * STRAAL);
    }

    /** The power the mouse points at (-1: none, the mouse is in the middle). */
    private int wijst(double mx, double my) {
        int n = lijst().size();
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (n == 0 || dx * dx + dy * dy < 14 * 14) {
            return -1;
        }
        if (n == 1) {
            return 0;
        }
        double hoek = Math.atan2(dx, -dy);
        if (hoek < 0) {
            hoek += 2 * Math.PI;
        }
        return (int) Math.round(hoek / (2 * Math.PI) * n) % n;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // (no blur: the world stays in view behind the wheel)
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        List<KrachtPayloads.Regel> l = lijst();
        int n = l.size();
        gewezen = wijst(mouseX, mouseY);
        int cx = width / 2, cy = height / 2;
        g.fill(cx - STRAAL - VAK, cy - STRAAL - VAK, cx + STRAAL + VAK, cy + STRAAL + VAK, 0x70100810);
        KrachtPayloads.Regel gekozen = E2ZijClient.keuze();
        for (int i = 0; i < n; i++) {
            KrachtPayloads.Regel r = l.get(i);
            int x = plekX(i, n) - VAK / 2, y = plekY(i, n) - VAK / 2;
            int kleur = E2ZijClient.KLEUR[Math.floorMod(r.element(), E2ZijClient.KLEUR.length)];
            boolean hier = i == gewezen;
            int rand = hier ? 3 : 1;
            g.fill(x - rand, y - rand, x + VAK + rand, y + VAK + rand, hier ? 0xFFFFFFFF : kleur);
            g.fill(x, y, x + VAK, y + VAK, gekozen != null && gekozen.id().equals(r.id()) ? 0xF0402A30 : 0xE0201418);
            g.item(E2ZijClient.icoon(r), x + 5, y + 5);
            int rest = E2ZijClient.rest(r);
            if (rest > 0 && r.afkoel() > 0) {
                int h = Math.max(1, Math.round((float) VAK * rest / r.afkoel()));
                g.fill(x, y + VAK - h, x + VAK, y + VAK, 0xA0000000);
            }
        }
        Component midden = gewezen >= 0 && gewezen < n ? E2ZijClient.naam(l.get(gewezen)) : Component.translatable("gui.guhs.einde2_zij.kracht.kies");
        g.centeredText(font, midden, cx, cy - 4, 0xFFFFF0E0);
        Component uitleg = gewezen >= 0 && gewezen < n ? E2ZijClient.uitleg(l.get(gewezen))
                : gekozen != null ? E2ZijClient.uitleg(gekozen) : null;
        if (uitleg != null) {                                           // (fix4: what the power does, under the wheel)
            int y = cy + STRAAL + VAK + 6;
            for (net.minecraft.util.FormattedCharSequence regel : font.split(uitleg, Math.min(260, width - 32))) {
                g.centeredText(font, regel, cx, y, 0xFFE8DCC8);
                y += font.lineHeight + 1;
            }
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void kies() {
        List<KrachtPayloads.Regel> l = lijst();
        if (gewezen >= 0 && gewezen < l.size()) {
            E2ZijClient.kies(l.get(gewezen).id());
        }
        onClose();
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (E2ZijClient.KRACHT.matches(event)) {
            kies();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        gewezen = wijst(event.x(), event.y());
        kies();
        return true;
    }
}
