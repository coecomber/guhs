package nl.juiced.guhs.feature.guhpixel.kantoor.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorTeksten;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorTeksten.Stijl;
import nl.juiced.guhs.feature.guhpixel.kantoor.Papier;
import nl.juiced.guhs.taal.Tekst;

/**
 * Reading a paper of the Guhkantoor: a cream sheet with the lines of {@link KantoorTeksten#blad} (the same sheet for the
 * item in your hand and the one on the wall), drawn line by line in its style: headings, small print, a red stamp a
 * little askew, and for a kwartaalrapport the three graphs, which are all perfectly flat.
 */
public class PapierScherm extends Screen {
    private static final int W = 216;
    private static final int PAPIER = 0xFFFAF4E2, RAND = 0xFFE2D8BE, SCHADUW = 0x60000000, INKT = 0xFF3A2A30, GRIJS = 0xFF8A7A80, ROZE = 0xFFD6609C,
            ROOD = 0xFFD64660, GOUD = 0xFFC49430, BLAUW = 0xFF7896DC;

    private final CompoundTag papier;
    private final GidsLijst lijst = new GidsLijst();
    private int left, top, hoog;

    public PapierScherm(CompoundTag papier) {
        super(Component.translatable("block.guhs.guhkantoor_" + switch (Papier.soort(papier)) {
            case LOON -> "loonstrookje";
            case KWARTAAL -> "kwartaalrapport";
            case OORKONDE -> "oorkonde";
        }));
        this.papier = papier;
    }

    @Override
    protected void init() {
        hoog = Math.min(232, height - 34);
        left = (width - W) / 2;
        top = Math.max(4, (height - hoog - 26) / 2);
        lijst.plaats(left + 10, top + 8, W - 20, hoog - 16);
        List<GidsLijst.Regel> regels = new ArrayList<>();
        for (KantoorTeksten.Regel r : KantoorTeksten.blad(papier)) {
            regels.add(new Rij(r));
        }
        lijst.zet(regels);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 40, top + hoog + 4, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left + 3, top + 3, left + W + 3, top + hoog + 3, SCHADUW);
        boolean oorkonde = Papier.soort(papier) == Papier.Soort.OORKONDE;
        g.fill(left - 1, top - 1, left + W + 1, top + hoog + 1, oorkonde ? GOUD : RAND);
        g.fill(left, top, left + W, top + hoog, PAPIER);
        if (oorkonde) {
            g.fill(left + 3, top + 3, left + W - 3, top + 4, GOUD);
            g.fill(left + 3, top + hoog - 4, left + W - 3, top + hoog - 3, GOUD);
            g.fill(left + 3, top + 3, left + 4, top + hoog - 3, GOUD);
            g.fill(left + W - 4, top + 3, left + W - 3, top + hoog - 3, GOUD);
        }
        lijst.teken(g, mouseX, mouseY, RAND, 0x30E2D8BE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return lijst.wiel(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || lijst.klik(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return lijst.sleep(event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        lijst.los();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    /** A line across the middle of a row, centred. */
    private static void midden(GuiGraphicsExtractor g, Component tekst, int x, int y, int w, float schaal, int kleur) {
        int tw = Math.round(font().width(tekst) * schaal);
        if (tw > w) {
            GidsTekst.passend(g, tekst, x, y, w, schaal, kleur, false);
        } else {
            GidsTekst.schaal(g, tekst, x + (w - tw) / 2, y, schaal, kleur, false);
        }
    }

    /** One line of the sheet. */
    private final class Rij implements GidsLijst.Regel {
        private final Stijl stijl;
        private final Component tekst;

        Rij(KantoorTeksten.Regel regel) {
            this.stijl = regel.stijl();
            this.tekst = switch (regel.stijl()) {
                case VET, TITEL, NAAM -> regel.tekst().copy().withStyle(ChatFormatting.BOLD);
                case SCHUIN, HANDTEKENING -> regel.tekst().copy().withStyle(ChatFormatting.ITALIC);
                case PUNT -> Component.literal("• ").append(regel.tekst());
                default -> regel.tekst();
            };
        }

        @Override
        public int hoogte() {
            int w = lijst.rijBreedte();
            return switch (stijl) {
                case TITEL -> 15;
                case ONDER -> 10;
                case KLEIN -> 8;
                case REGEL, VET, SCHUIN -> GidsTekst.hoogte(tekst, w, 0.75f) + 2;
                case PUNT -> GidsTekst.hoogte(tekst, w - 6, 0.75f) + 2;
                case STREEP -> 7;
                case WIT -> 6;
                case STEMPEL -> 26;
                case NAAM -> 20;
                case HANDTEKENING -> 12;
                case GRAFIEK_LIJN -> 52;
                case GRAFIEK_STAAF -> 60;
                case GRAFIEK_TAART -> 38;
            };
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            switch (stijl) {
                case TITEL -> midden(g, tekst, x, y + 2, w, 1.25f, INKT);
                case ONDER -> midden(g, tekst, x, y + 1, w, 0.75f, GRIJS);
                case KLEIN -> GidsTekst.passend(g, tekst, x, y + 1, w, 0.625f, GRIJS, false);
                case REGEL, VET -> GidsTekst.alinea(g, tekst, x, y + 1, w, 0.75f, INKT);
                case SCHUIN -> GidsTekst.alinea(g, tekst, x, y + 1, w, 0.75f, ROZE);
                case PUNT -> GidsTekst.alinea(g, tekst, x + 6, y + 1, w - 6, 0.75f, INKT);
                case STREEP -> g.fill(x, y + 3, x + w, y + 4, RAND);
                case WIT -> {
                }
                case STEMPEL -> stempel(g, x, y, w);
                case NAAM -> midden(g, tekst, x, y + 3, w, 1.5f, ROZE);
                case HANDTEKENING -> GidsTekst.passend(g, tekst, x + w, y + 2, w, 0.75f, INKT, true);
                case GRAFIEK_LIJN -> lijn(g, x, y, w);
                case GRAFIEK_STAAF -> staven(g, x, y, w);
                case GRAFIEK_TAART -> taart(g, x, y, w);
            }
        }

        /** A red stamp on the right, a little askew. */
        private void stempel(GuiGraphicsExtractor g, int x, int y, int w) {
            int tw = Math.min(w - 12, Math.round(font().width(tekst) * 0.75f)), bw = tw + 10, bh = 14;
            g.pose().pushMatrix();
            g.pose().translate(x + w - bw - 4, y + 9);
            g.pose().rotate(-0.07f);
            g.fill(0, 0, bw, 1, ROOD);
            g.fill(0, bh - 1, bw, bh, ROOD);
            g.fill(0, 0, 1, bh, ROOD);
            g.fill(bw - 1, 0, bw, bh, ROOD);
            GidsTekst.passend(g, tekst, 5, 4, tw, 0.75f, ROOD, false);
            g.pose().popMatrix();
        }

        private void titel(GuiGraphicsExtractor g, int x, int y, int w) {
            GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x, y + 2, w, 0.75f, INKT, false);
        }

        /** The line graph: an axis from "niks" to "veel", and a line that lies flat on "niks" the whole quarter. */
        private void lijn(GuiGraphicsExtractor g, int x, int y, int w) {
            titel(g, x, y, w);
            int gx = x + 26, gy = y + 13, gw = w - 34, gh = 28;
            g.fill(gx, gy, gx + 1, gy + gh, INKT);
            g.fill(gx, gy + gh, gx + gw, gy + gh + 1, INKT);
            for (int i = 1; i <= 3; i++) {
                g.fill(gx + 1, gy + gh - i * 8, gx + gw, gy + gh - i * 8 + 1, 0x30A09098);
            }
            g.fill(gx + 1, gy + gh - 2, gx + gw - 2, gy + gh - 1, ROOD);
            GidsTekst.passend(g, Component.translatable("book.guhs.guhkantoor.kwartaal.as.veel"), gx - 2, gy, 24, 0.5f, GRIJS, true);
            GidsTekst.passend(g, Component.translatable("book.guhs.guhkantoor.kwartaal.as.niks"), gx - 2, gy + gh - 6, 24, 0.5f, GRIJS, true);
            GidsTekst.passend(g, Component.translatable("book.guhs.guhkantoor.kwartaal.as.tijd"), gx + gw, gy + gh + 3, 30, 0.5f, GRIJS, true);
        }

        /** The bar graph: one bar per employee, all exactly as high. */
        private void staven(GuiGraphicsExtractor g, int x, int y, int w) {
            titel(g, x, y, w);
            ListTag medewerkers = papier.getListOrEmpty("Medewerkers");
            int n = Math.max(1, Math.min(4, medewerkers.size()));
            int gx = x + 8, gy = y + 13, gw = w - 16, gh = 30, vak = gw / n;
            g.fill(gx, gy + gh, gx + gw, gy + gh + 1, INKT);
            for (int i = 0; i < n; i++) {
                int bx = gx + i * vak + vak / 2 - 9;
                g.fill(bx, gy + 4, bx + 18, gy + gh, i % 2 == 0 ? ROZE : BLAUW);
                g.fill(bx, gy + 4, bx + 18, gy + 5, 0x60FFFFFF);
                Component naam = i < medewerkers.size() ? Tekst.get(medewerkers.getCompoundOrEmpty(i), "Naam")
                        : Component.translatable("book.guhs.guhkantoor.kwartaal.niemand");
                int nw = Math.min(vak - 2, Math.round(font().width(naam) * 0.5f));
                GidsTekst.passend(g, naam, gx + i * vak + (vak - nw) / 2, gy + gh + 4, vak - 2, 0.5f, GRIJS, false);
            }
        }

        /** The pie: one slice, 100 %. */
        private void taart(GuiGraphicsExtractor g, int x, int y, int w) {
            int r = 15, cx = x + 8 + r, cy = y + 4 + r;
            for (int dy = -r; dy <= r; dy++) {
                int dx = (int) Math.round(Math.sqrt(r * r - dy * dy));
                g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, ROZE);
            }
            g.fill(cx, cy - r, cx + 1, cy, 0x80FFFFFF);
            GidsTekst.alinea(g, tekst.copy().withStyle(ChatFormatting.BOLD), cx + r + 10, cy - 8, w - 2 * r - 22, 0.75f, INKT);
        }
    }
}
