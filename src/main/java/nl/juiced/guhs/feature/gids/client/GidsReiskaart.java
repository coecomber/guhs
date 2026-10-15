package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Halte;
import nl.juiced.guhs.feature.verhaal.Reiskaart;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.5): the travel map page of the Guhdex tab Verhalen: the drawn map
 * ({@code textures/gui/verhaal/reiskaart_<id>.png}, 256x160) with the route and the haltes on it (a tick where you have
 * been, "Je bent hier" where you are, "?" where the story hasn't taken you yet), one sentence "Dit moet je nu doen", then
 * every halte with its steps ticked off (a later one only as "???"), and the scenes and narrator cards you can watch again.
 */
public final class GidsReiskaart {
    private static final int KAART_W = 256, KAART_H = 160, MAX_H = 118;

    private GidsReiskaart() {
    }

    /** The picture of a travel map. */
    public static Identifier plaatje(Reiskaart k) {
        return Guhs.id("textures/gui/verhaal/reiskaart_" + k.id() + ".png");
    }

    /** The halte the player is at: the first one in travel order whose line is open and not done (null: all done). */
    @Nullable
    public static Halte hier(Reiskaart k) {
        for (Halte h : k.haltes()) {
            if (!klaar(h) && !GidsVerhalenTab.geheim(h.lijn())) {
                return h;
            }
        }
        return null;
    }

    static boolean klaar(Halte h) {
        Verhaallijn l = Verhaallijnen.van(h.lijn());
        return l != null && VerhaalSync.Client.stap(l.id()) >= l.stappen();
    }

    static List<GidsLijst.Regel> pagina(GidsVerhalenTab tab, Reiskaart k) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        Halte hier = hier(k);
        out.add(new Plaat(k, hier));
        VerhaalStand nu = hier == null ? null : VerhalenCache.van(hier.lijn());
        if (nu != null) {
            out.add(GidsVerhalenTab.kop(Component.translatable("gui.guhs.verhalen.kaart.nu")));
            out.add(tab.tekst(nu.nu(), 0.875f, GidsVerhalenTab.DONKER, 4));
            out.add(tab.tekst(Component.literal("➜ ").append(nu.waar()), 0.75f, GidsVerhalenTab.ROZE, 4));
        } else if (hier == null) {
            out.add(GidsVerhalenTab.kop(Component.translatable("gui.guhs.verhalen.kaart.klaar")));
        }
        for (Halte h : k.haltes()) {
            boolean geheim = GidsVerhalenTab.geheim(h.lijn());
            out.add(new HalteKop(tab, h, geheim, h == hier));
            VerhaalStand v = geheim ? null : VerhalenCache.van(h.lijn());
            if (v != null) {
                out.addAll(tab.stappen(v));
            }
        }
        List<GidsLijst.Regel> herbekijk = new ArrayList<>();
        for (Halte h : k.haltes()) {
            herbekijk.addAll(tab.herbekijkRegels(h.lijn()));
        }
        if (!herbekijk.isEmpty()) {
            out.add(GidsVerhalenTab.kop(Component.translatable("gui.guhs.verhalen.kop.herbekijk")));
            out.addAll(herbekijk);
        }
        return out;
    }

    /** The map picture with the route and the haltes. */
    private static final class Plaat implements GidsLijst.Regel {
        private final Reiskaart k;
        @Nullable
        private final Halte hier;

        Plaat(Reiskaart k, @Nullable Halte hier) {
            this.k = k;
            this.hier = hier;
        }

        private int hoog(int w) {
            int breed = Math.min(KAART_W, w - 8);
            return Math.min(MAX_H, breed * KAART_H / KAART_W);
        }

        @Override
        public int hoogte() {
            return MAX_H + 8;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            int kh = hoog(w), kw = kh * KAART_W / KAART_H, kx = x + (w - kw) / 2, ky = y + 3;
            float s = kw / (float) KAART_W;
            g.fill(kx - 2, ky - 2, kx + kw + 2, ky + kh + 2, 0xFF7A2848);
            g.blit(RenderPipelines.GUI_TEXTURED, plaatje(k), kx, ky, 0f, 0f, kw, kh, KAART_W, KAART_H, KAART_W, KAART_H);
            // the route: dots from halte to halte (gold where you have been, grey where you still go)
            List<Halte> haltes = k.haltes();
            for (int i = 0; i + 1 < haltes.size(); i++) {
                Halte a = haltes.get(i), b = haltes.get(i + 1);
                boolean gegaan = klaar(a);
                double ax = kx + a.kaartX() * s, ay = ky + a.kaartY() * s, bx = kx + b.kaartX() * s, by = ky + b.kaartY() * s;
                int n = Math.max(2, (int) (Math.hypot(bx - ax, by - ay) / 5));
                for (int j = 1; j < n; j++) {
                    int px = (int) Math.round(Mth.lerp(j / (double) n, ax, bx)), py = (int) Math.round(Mth.lerp(j / (double) n, ay, by));
                    g.fill(px - 1, py - 1, px + 1, py + 1, gegaan ? 0xFFB8862A : 0xB0705848);
                }
            }
            long tijd = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            for (Halte h : haltes) {
                int px = (int) Math.round(kx + h.kaartX() * s), py = (int) Math.round(ky + h.kaartY() * s);
                boolean geheim = GidsVerhalenTab.geheim(h.lijn()), klaar = klaar(h), nu = h == hier;
                int r = nu ? 5 + (int) (tijd / 6 % 2) : 4;
                g.fill(px - r, py - r, px + r, py + r, nu ? 0xFFF7D27A : 0xFF3A1C30);
                g.fill(px - r + 1, py - r + 1, px + r - 1, py + r - 1, klaar ? 0xFF3E9A5A : nu ? 0xFFD8322A : geheim ? 0xFF8A7A80 : 0xFFF3C6D6);
                Component c = Component.literal(klaar ? "✔" : geheim ? "?" : String.valueOf(h.nr()));
                GidsTekst.schaal(g, c, px - Math.round(GidsTekst.font().width(c) * 0.375f), py - 3, 0.75f, 0xFFFFFFFF, false);
                if (nu) {
                    Component hierTekst = Component.translatable("gui.guhs.verhalen.kaart.hier").withStyle(ChatFormatting.BOLD);
                    int tw = Math.round(GidsTekst.font().width(hierTekst) * 0.75f);
                    int tx = Mth.clamp(px - tw / 2, kx + 2, kx + kw - tw - 2), ty = py - r - 10 < ky + 1 ? py + r + 2 : py - r - 10;
                    g.fill(tx - 2, ty - 1, tx + tw + 2, ty + 8, 0xD0301A26);
                    GidsTekst.schaal(g, hierTekst, tx, ty + 1, 0.75f, 0xFFF7D27A, false);
                }
            }
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            int kh = hoog(w), kw = kh * KAART_W / KAART_H, kx = x + (w - kw) / 2, ky = y + 3;
            float s = kw / (float) KAART_W;
            for (Halte h : k.haltes()) {
                if (Math.abs(mx - (kx + h.kaartX() * s)) <= 6 && Math.abs(my - (ky + h.kaartY() * s)) <= 6) {
                    return List.of(naam(h, GidsVerhalenTab.geheim(h.lijn())));
                }
            }
            return null;
        }
    }

    static Component naam(Halte h, boolean geheim) {
        return geheim ? Component.translatable("gui.guhs.verhalen.geheim")
                : Component.literal(h.nr() + ". ").append(Component.translatable("gui.guhs.verhalen." + h.lijn() + ".naam"));
    }

    /** The heading of a halte in the list under the map: its number and name (click: the questline's page), or "???". */
    private static final class HalteKop implements GidsLijst.Regel {
        private final GidsVerhalenTab tab;
        private final Halte h;
        private final boolean geheim, hier;

        HalteKop(GidsVerhalenTab tab, Halte h, boolean geheim, boolean hier) {
            this.tab = tab;
            this.h = h;
            this.geheim = geheim;
            this.hier = hier;
        }

        @Override
        public int hoogte() {
            return 16;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean klaar = klaar(h);
            g.fill(x + 1, y + 2, x + w - 1, y + 15, hier ? 0x50F7D27A : hover && !geheim ? 0x40F7B6CB : 0x18F7B6CB);
            int kleur = geheim ? GidsVerhalenTab.GRIJS : klaar ? GidsVerhalenTab.GROEN : GidsVerhalenTab.DONKER;
            Component rechts = hier ? Component.translatable("gui.guhs.verhalen.kaart.hier") : klaar ? Component.literal("✔") : Component.empty();
            int rw = Math.round(GidsTekst.font().width(rechts) * 0.75f);
            GidsTekst.passend(g, naam(h, geheim).copy().withStyle(ChatFormatting.BOLD), x + 5, y + 5, w - 14 - rw, 0.875f, kleur, false);
            GidsTekst.schaal(g, rechts, x + w - 5, y + 6, 0.75f, hier ? GidsVerhalenTab.ROZE : GidsVerhalenTab.GROEN, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (geheim || VerhalenCache.van(h.lijn()) == null) {
                return false;
            }
            tab.ga(h.lijn());
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable(geheim ? "gui.guhs.verhalen.geheim.tooltip" : "gui.guhs.verhalen.klik_rij").withStyle(ChatFormatting.GRAY));
        }
    }
}
