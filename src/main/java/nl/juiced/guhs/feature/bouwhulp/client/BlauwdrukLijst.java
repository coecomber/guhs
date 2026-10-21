package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdrukken;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

/**
 * The blueprint browser's page (the keet's Blauwdrukken tab and the Ontwerpbureau): the three sources and the page
 * arrows on top, four blueprints below, each with its picture, name, maker, size and thumbs, and its buttons.
 */
final class BlauwdrukLijst {
    interface Doe {
        void doe(int actie, int waarde, String tekst);
    }

    static final String[] BRONNEN = {"eigen", "catalogus", "bibliotheek"};

    static void knoppen(OptieLijst.Houder h, CompoundTag t, int x, int y, int w, int hoog, boolean mag, Doe d) {
        int bron = t.getIntOr("Bron", 1);
        for (int i = 0; i < 3; i++) {
            int b = i;
            h.knop(Component.translatable("gui.guhs.blauwdruk.bron." + BRONNEN[i]), x + i * 78, y, 76, () -> d.doe(Blauwdrukken.BRON, b, ""), bron != i);
        }
        int pagina = t.getIntOr("Pagina", 0), paginas = t.getIntOr("Paginas", 1);
        h.knop(Component.literal("◀"), x + w - 62, y, 18, () -> d.doe(Blauwdrukken.PAGINA, 255, ""), pagina > 0);
        h.knop(Component.literal("▶"), x + w - 18, y, 18, () -> d.doe(Blauwdrukken.PAGINA, 1, ""), pagina < paginas - 1);
        boolean keet = t.getBooleanOr("Keet", false);
        ListTag l = t.getListOrEmpty("Lijst");
        int cw = w / 2, ch = (hoog - 30) / 2;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            if (c.getBooleanOr("Binnenkort", false)) {
                continue;
            }
            String id = c.getStringOr("Id", "");
            int cx = x + (i % 2) * cw, cy = y + 22 + (i / 2) * ch;
            int by = cy + ch - 20, bx = cx + 52;
            if (keet) {
                h.knop(Component.translatable("gui.guhs.blauwdruk.plaats"), bx, by, 42, () -> d.doe(Blauwdrukken.PLAATS, 0, id), mag)
                        .setTooltip(Tooltip.create(Component.translatable("gui.guhs.blauwdruk.plaats.uitleg")));
            } else {
                h.knop(Component.translatable("gui.guhs.blauwdruk.print"), bx, by, 42, () -> d.doe(Blauwdrukken.PRINT, 0, id), true)
                        .setTooltip(Tooltip.create(Component.translatable("gui.guhs.blauwdruk.print.uitleg")));
            }
            bx += 44;
            if (c.getBooleanOr("Publiceer", false)) {
                h.knop(Component.literal("⇪"), bx, by, 18, () -> d.doe(Blauwdrukken.PUBLICEER, 0, id), mag && c.getBooleanOr("PubliceerKan", false))
                        .setTooltip(Tooltip.create(Component.translatable(c.getBooleanOr("PubliceerKan", false) ? "gui.guhs.blauwdruk.publiceer.uitleg"
                                : "gui.guhs.blauwdruk.publiceer.vanaf")));
                bx += 20;
            }
            if (c.getBooleanOr("Duim", false)) {
                h.knop(Component.literal((c.getBooleanOr("Mijn", false) ? "♥ " : "♡ ") + c.getIntOr("Duimpjes", 0)), bx, by, 30,
                        () -> d.doe(Blauwdrukken.DUIMPJE, 0, id), true);
                bx += 32;
            }
            if (c.getBooleanOr("Weg", false)) {
                h.knop(Component.literal("✕"), bx, by, 18, () -> d.doe(Blauwdrukken.VERWIJDER, 0, id), true)
                        .setTooltip(Tooltip.create(Component.translatable("gui.guhs.blauwdruk.weg")));
            }
        }
    }

    static void teken(GuiGraphicsExtractor g, CompoundTag t, int x, int y, int w, int hoog, int donker, int tekst, int licht) {
        int pagina = t.getIntOr("Pagina", 0), paginas = t.getIntOr("Paginas", 1);
        GidsTekst.passend(g, Component.literal((pagina + 1) + "/" + paginas), x + w - 40, y + 5, 20, 0.75f, tekst, false);
        ListTag l = t.getListOrEmpty("Lijst");
        int cw = w / 2, ch = (hoog - 30) / 2;
        if (l.isEmpty()) {
            Component leeg = Component.translatable("gui.guhs.blauwdruk.leeg." + BRONNEN[t.getIntOr("Bron", 1)]);
            GidsTekst.alinea(g, leeg, x, y + 28, w, 0.8f, licht);
        }
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            int cx = x + (i % 2) * cw, cy = y + 22 + (i / 2) * ch;
            g.fill(cx, cy, cx + cw - 4, cy + ch - 2, 0x30A0845E);
            if (c.getBooleanOr("Binnenkort", false)) {
                GidsTekst.alinea(g, Component.translatable("gui.guhs.blauwdruk.binnenkort").withStyle(ChatFormatting.ITALIC), cx + 6, cy + ch / 2 - 6,
                        cw - 14, 0.8f, licht);
                continue;
            }
            plaatje(g, c.getIntArray("Voorbeeld").orElse(new int[0]), cx + 2, cy + 2);
            String sleutel = c.getStringOr("Sleutel", "");
            Component naam = sleutel.isEmpty() ? Component.literal(c.getStringOr("Naam", "?")) : Component.translatable(sleutel);
            GidsTekst.passend(g, naam.copy().withStyle(ChatFormatting.BOLD), cx + 52, cy + 3, cw - 58, 0.75f, donker, false);
            int[] gr = c.getIntArray("Grootte").orElse(new int[] {0, 0, 0});
            GidsTekst.passend(g, Component.translatable("gui.guhs.blauwdruk.maat", gr[0], gr[1], gr[2], c.getIntOr("Aantal", 0)), cx + 52, cy + 13,
                    cw - 58, 0.65f, tekst, false);
            Component maker = c.contains("Groep") ? Component.translatable("gui.guhs.blauwdruk.groep." + c.getStringOr("Groep", ""))
                    : Component.translatable("gui.guhs.blauwdruk.maker", c.getStringOr("Maker", "?"));
            GidsTekst.passend(g, maker, cx + 52, cy + 22, cw - 58, 0.65f, licht, false);
        }
        GidsTekst.passend(g, Component.translatable("gui.guhs.blauwdruk.tellers", t.getIntOr("Eigen", 0), t.getIntOr("MaxEigen", 30),
                t.getIntOr("Gepubliceerd", 0), t.getIntOr("MaxPubliceer", 10)), x, y + hoog - 6, w, 0.6f, licht, false);
    }

    /** A blueprint's picture (N × N ARGB, 0 = see-through), drawn as runs of pixels. */
    static void plaatje(GuiGraphicsExtractor g, int[] pix, int x, int y) {
        int n = (int) Math.round(Math.sqrt(pix.length));
        if (n * n != pix.length || n == 0) {
            return;
        }
        g.fill(x, y, x + n, y + n, 0x40FFFFFF);
        for (int r = 0; r < n; r++) {
            int k = 0;
            while (k < n) {
                int c = pix[r * n + k];
                int e = k + 1;
                while (e < n && pix[r * n + e] == c) {
                    e++;
                }
                if ((c >>> 24) != 0) {
                    g.fill(x + k, y + r, x + e, y + r + 1, c);
                }
                k = e;
            }
        }
    }

    private BlauwdrukLijst() {
    }
}
