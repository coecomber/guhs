package nl.juiced.guhs.feature.guhpixel.gezel.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.gezel.BordSpel;
import nl.juiced.guhs.feature.guhpixel.gezel.GezelPayloads;
import nl.juiced.guhs.feature.guhpixel.gezel.OpEenRij;
import nl.juiced.guhs.feature.guhpixel.gezel.WieIsHet;

/**
 * The screen of the two board games (Guh op een rij and Wie is het). It only shows what the server sends
 * (BordSpel#staat) and sends what you click (GezelPayloads.Doe): choose a bot level or look for an opponent, drop a disc,
 * ask a question, guess a guh.
 */
public class GezelScherm extends Screen {
    private static final int W = 340, H = 236;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, DOF = 0xFFB090A0, GOUD = 0xFFFFD86B;
    private static final int CEL = 22, KAART_W = 40, KAART_H = 44;

    private CompoundTag data;
    private int left, top;
    private int vraag;
    private boolean raden;

    public GezelScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.gezel.titel." + data.getStringOr("Spel", BordSpel.OPEENRIJ)));
        this.data = data;
    }

    public String spel() {
        return data.getStringOr("Spel", BordSpel.OPEENRIJ);
    }

    public void update(CompoundTag nieuw) {
        boolean ander = !nieuw.getStringOr("Fase", "").equals(data.getStringOr("Fase", ""));
        this.data = nieuw;
        if (ander) {
            raden = false;
        }
        rebuildWidgets();
    }

    private void doe(int actie, int arg) {
        if (minecraft != null && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new GezelPayloads.Doe(spel(), actie, arg));
        }
    }

    private String fase() {
        return data.getStringOr("Fase", "kies");
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int rx = left + 226, rw = 106;
        switch (fase()) {
            case "kies" -> {
                for (int n = 0; n < 3; n++) {
                    int niveau = n;
                    addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.bot." + n), b -> doe(BordSpel.KIES_BOT, niveau))
                            .bounds(left + W / 2 - 80, top + 86 + n * 24, 160, 20).build());
                }
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.zoek"), b -> doe(BordSpel.ZOEK, 0))
                        .bounds(left + W / 2 - 80, top + 166, 160, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.sluit"), b -> onClose())
                        .bounds(left + W / 2 - 40, top + H - 26, 80, 20).build());
            }
            case "wacht" -> addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.annuleer"), b -> doe(BordSpel.ANNULEER, 0))
                    .bounds(left + W / 2 - 60, top + 140, 120, 20).build());
            default -> {
                boolean einde = "einde".equals(fase());
                if (einde) {
                    addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.opnieuw"), b -> doe(BordSpel.OPNIEUW, 0))
                            .bounds(rx, top + H - 50, rw, 20).build());
                } else if (BordSpel.WIEISHET.equals(spel())) {
                    addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                        vraag = (vraag + WieIsHet.VRAGEN.size() - 1) % WieIsHet.VRAGEN.size();
                    }).bounds(rx, top + 78, 16, 20).build());
                    addRenderableWidget(Button.builder(Component.literal(">"), b -> vraag = (vraag + 1) % WieIsHet.VRAGEN.size())
                            .bounds(rx + rw - 16, top + 78, 16, 20).build());
                    Button stel = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.vraag"), b -> doe(BordSpel.VRAAG, vraag))
                            .bounds(rx, top + 102, rw, 20).build());
                    Button raad = addRenderableWidget(Button.builder(Component.translatable(raden ? "gui.guhs.gezel.knop.niet_raden" : "gui.guhs.gezel.knop.raden"), b -> {
                        raden = !raden;
                        rebuildWidgets();
                    }).bounds(rx, top + 126, rw, 20).build());
                    boolean mijn = data.getBooleanOr("Mijn", false);
                    stel.active = mijn && !raden;
                    raad.active = mijn;
                }
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.gezel.knop.stop"), b -> {
                    doe(BordSpel.SLUIT, 0);
                    onClose();
                }).bounds(rx, top + H - 26, rw, 20).build());
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        switch (fase()) {
            case "kies" -> {
                g.centeredText(font, Component.translatable("gui.guhs.gezel.uitleg." + spel()), width / 2, top + 30, DOF);
                g.centeredText(font, Component.translatable("gui.guhs.gezel.uitleg2." + spel()), width / 2, top + 42, DOF);
                g.centeredText(font, Component.translatable("gui.guhs.gezel.gewonnen", data.getIntOr("Gewonnen", 0)), width / 2, top + 62, GOUD);
            }
            case "wacht" -> {
                g.centeredText(font, Component.translatable("gui.guhs.gezel.wacht"), width / 2, top + 90, TEKST);
                g.centeredText(font, Component.translatable("gui.guhs.gezel.wacht2"), width / 2, top + 104, DOF);
            }
            default -> {
                if (BordSpel.OPEENRIJ.equals(spel())) {
                    tekenRij(g, mouseX, mouseY);
                } else {
                    tekenWie(g, mouseX, mouseY);
                }
                tekenZij(g);
            }
        }
    }

    // --- Guh op een rij ---------------------------------------------------------------------------------------------

    private int bordX() {
        return left + 12 + (210 - OpEenRij.BREED * CEL) / 2;
    }

    private int bordY() {
        return top + 26 + (190 - OpEenRij.HOOG * CEL) / 2;
    }

    private void tekenRij(GuiGraphicsExtractor g, int mx, int my) {
        byte[] bord = data.getByteArray("Bord").orElse(new byte[OpEenRij.BREED * OpEenRij.HOOG]);
        int bx = bordX(), by = bordY();
        // the standing board: a wooden frame with holes
        g.fill(bx - 6, by - 6, bx + OpEenRij.BREED * CEL + 6, by + OpEenRij.HOOG * CEL + 6, 0xFFB98A5E);
        g.fill(bx - 3, by - 3, bx + OpEenRij.BREED * CEL + 3, by + OpEenRij.HOOG * CEL + 3, 0xFF8BC4E8);
        g.fill(bx - 10, by + OpEenRij.HOOG * CEL + 6, bx - 4, by + OpEenRij.HOOG * CEL + 16, 0xFFB98A5E);
        g.fill(bx + OpEenRij.BREED * CEL + 4, by + OpEenRij.HOOG * CEL + 6, bx + OpEenRij.BREED * CEL + 10, by + OpEenRij.HOOG * CEL + 16, 0xFFB98A5E);
        int hover = data.getBooleanOr("Mijn", false) && mx >= bx && mx < bx + OpEenRij.BREED * CEL && my >= by - 20 && my < by + OpEenRij.HOOG * CEL
                ? (mx - bx) / CEL : -1;
        for (int k = 0; k < OpEenRij.BREED; k++) {
            if (k == hover) {
                g.fill(bx + k * CEL, by, bx + (k + 1) * CEL, by + OpEenRij.HOOG * CEL, 0x40FFFFFF);
            }
            for (int r = 0; r < OpEenRij.HOOG; r++) {
                int x = bx + k * CEL, y = by + (OpEenRij.HOOG - 1 - r) * CEL;
                byte v = bord[k + r * OpEenRij.BREED];
                schijf(g, x + 2, y + 2, CEL - 4, v);
            }
        }
        if (hover >= 0) {
            schijf(g, bx + hover * CEL + 2, by - 22, CEL - 4, data.getByteOr("Ik", OpEenRij.GUH));
        }
    }

    /** A hole (empty), a pink guh disc (with ears and a face) or a blue Blub disc (block body, flat ears). */
    static void schijf(GuiGraphicsExtractor g, int x, int y, int s, byte wie) {
        if (wie == OpEenRij.LEEG) {
            rond(g, x, y, s, 0xFF2E4A66);
            return;
        }
        if (wie == OpEenRij.GUH) {
            g.fill(x + 2, y, x + 6, y + 4, 0xFFE58AB0);
            g.fill(x + s - 6, y, x + s - 2, y + 4, 0xFFE58AB0);
            rond(g, x, y + 1, s, 0xFFF4A6C8);
        } else {
            g.fill(x + 4, y - 2, x + 6, y + 4, 0xFF6E9FDB);
            g.fill(x + s - 6, y - 2, x + s - 4, y + 4, 0xFF6E9FDB);
            g.fill(x + 1, y + 2, x + s - 1, y + s - 1, 0xFF8CB8F0);
        }
        int oy = y + s / 2 - 1;
        g.fill(x + s / 2 - 4, oy, x + s / 2 - 2, oy + 2, 0xFF201018);
        g.fill(x + s / 2 + 2, oy, x + s / 2 + 4, oy + 2, 0xFF201018);
        g.fill(x + s / 2 - 1, oy + 4, x + s / 2 + 1, oy + 5, wie == OpEenRij.GUH ? 0xFFB0406A : 0xFF6A3040);
    }

    private static void rond(GuiGraphicsExtractor g, int x, int y, int s, int kleur) {
        int r = s / 2;
        for (int dy = 0; dy < s; dy++) {
            double yy = dy - r + 0.5;
            int half = (int) Math.round(Math.sqrt(Math.max(0, r * r - yy * yy)));
            g.fill(x + r - half, y + dy, x + r + half, y + dy + 1, kleur);
        }
    }

    // --- Wie is het ------------------------------------------------------------------------------------------------------

    private List<CompoundTag> kaarten() {
        List<CompoundTag> l = new ArrayList<>();
        for (Tag t : data.getListOrEmpty("Kaarten")) {
            if (t instanceof CompoundTag c) {
                l.add(c);
            }
        }
        return l;
    }

    private int kaartX(int i) {
        return left + 8 + (i % 5) * (KAART_W + 2);
    }

    private int kaartY(int i) {
        return top + 22 + (i / 5) * (KAART_H + 2);
    }

    private void tekenWie(GuiGraphicsExtractor g, int mx, int my) {
        List<CompoundTag> kaarten = kaarten();
        long open = data.getLongOr("Open", 0L);
        for (int i = 0; i < kaarten.size(); i++) {
            int x = kaartX(i), y = kaartY(i);
            boolean op = (open & (1L << i)) != 0;
            boolean hover = mx >= x && mx < x + KAART_W && my >= y && my < y + KAART_H;
            kaart(g, x, y, kaarten.get(i), op, raden && op && hover);
        }
        int geheim = data.getIntOr("Geheim", -1);
        if (geheim >= 0 && geheim < kaarten.size()) {
            int rx = left + 226;
            g.text(font, Component.translatable("gui.guhs.gezel.jouw_guh"), rx, top + 22, DOF, false);
            kaart(g, rx + 66, top + 20, kaarten.get(geheim), true, false);
        }
        int anderGeheim = data.getIntOr("AnderGeheim", -1);
        if (anderGeheim >= 0 && anderGeheim < kaarten.size()) {
            g.text(font, Component.translatable("gui.guhs.gezel.zijn_guh", kaarten.get(anderGeheim).getStringOr("N", "")), left + 226, top + 70, GOUD, false);
        } else if (!"einde".equals(fase())) {
            WieIsHet.Vraag v = WieIsHet.VRAGEN.get(Math.min(vraag, WieIsHet.VRAGEN.size() - 1));
            Component tekst = BordSpel.vraagTekst(v);
            int rx = left + 226 + 18, rw = 106 - 36;
            List<net.minecraft.util.FormattedCharSequence> regels = font.split(tekst, rw);
            for (int i = 0; i < Math.min(2, regels.size()); i++) {
                g.text(font, regels.get(i), rx + (rw - font.width(regels.get(i))) / 2, top + 79 + i * 9, TEKST, false);
            }
        }
    }

    /** One card: a little guh in its fur colour with its hat, glasses and bow; the name and the personality below. */
    private void kaart(GuiGraphicsExtractor g, int x, int y, CompoundTag k, boolean op, boolean gloed) {
        g.fill(x, y, x + KAART_W, y + KAART_H, gloed ? 0xFFFFD86B : 0xFFF7E9DC);
        if (!op) {
            g.fill(x + 1, y + 1, x + KAART_W - 1, y + KAART_H - 1, 0xFF5A3A48);
            g.centeredText(font, Component.literal("?"), x + KAART_W / 2, y + KAART_H / 2 - 4, 0xFF8A6A78);
            return;
        }
        g.fill(x + 1, y + 1, x + KAART_W - 1, y + KAART_H - 1, 0xFFFFF6EE);
        int kleur = 0xFF000000 | WieIsHet.KLEUR_RGB[Math.max(0, Math.min(WieIsHet.KLEUR_RGB.length - 1, k.getIntOr("K", 0)))];
        int cx = x + KAART_W / 2, gy = y + 12;
        // ears and head
        g.fill(cx - 10, gy, cx - 5, gy + 5, kleur);
        g.fill(cx + 5, gy, cx + 10, gy + 5, kleur);
        g.fill(cx - 9, gy + 3, cx + 9, gy + 18, kleur);
        g.fill(cx - 5, gy + 8, cx - 3, gy + 10, 0xFF201018);
        g.fill(cx + 3, gy + 8, cx + 5, gy + 10, 0xFF201018);
        g.fill(cx - 8, gy + 11, cx - 6, gy + 12, 0xFFF08AA8);
        g.fill(cx + 6, gy + 11, cx + 8, gy + 12, 0xFFF08AA8);
        g.fill(cx - 1, gy + 12, cx + 1, gy + 13, 0xFF6A3040);
        if (k.getBooleanOr("B", false)) {
            g.fill(cx - 7, gy + 7, cx - 1, gy + 8, 0xFF303030);
            g.fill(cx + 1, gy + 7, cx + 7, gy + 8, 0xFF303030);
            g.fill(cx - 7, gy + 10, cx - 1, gy + 11, 0xFF303030);
            g.fill(cx + 1, gy + 10, cx + 7, gy + 11, 0xFF303030);
            g.fill(cx - 1, gy + 8, cx + 1, gy + 9, 0xFF303030);
        }
        if (k.getBooleanOr("S", false)) {
            g.fill(cx - 5, gy + 17, cx - 1, gy + 21, 0xFFE0405A);
            g.fill(cx + 1, gy + 17, cx + 5, gy + 21, 0xFFE0405A);
            g.fill(cx - 1, gy + 18, cx + 1, gy + 20, 0xFFB02040);
        }
        switch (k.getIntOr("H", 0)) {
            case 1 -> {   // slaapmuts
                g.fill(cx - 8, gy - 2, cx + 8, gy + 3, 0xFF7FA8E8);
                g.fill(cx - 2, gy - 6, cx + 8, gy - 2, 0xFF7FA8E8);
                g.fill(cx + 8, gy - 6, cx + 11, gy - 3, 0xFFFFFFFF);
            }
            case 2 -> {   // kroontje
                g.fill(cx - 6, gy - 1, cx + 6, gy + 3, 0xFFF2C230);
                g.fill(cx - 6, gy - 4, cx - 4, gy - 1, 0xFFF2C230);
                g.fill(cx - 1, gy - 5, cx + 1, gy - 1, 0xFFF2C230);
                g.fill(cx + 4, gy - 4, cx + 6, gy - 1, 0xFFF2C230);
            }
            case 3 -> {   // koksmuts
                g.fill(cx - 6, gy - 8, cx + 6, gy + 3, 0xFFFFFFFF);
                g.fill(cx - 7, gy - 10, cx + 7, gy - 5, 0xFFF0F0F0);
            }
            case 4 -> {   // strohoed
                g.fill(cx - 12, gy + 1, cx + 12, gy + 3, 0xFFE2C27A);
                g.fill(cx - 6, gy - 4, cx + 6, gy + 1, 0xFFE2C27A);
                g.fill(cx - 6, gy - 1, cx + 6, gy, 0xFFD04060);
            }
            default -> {
            }
        }
        Component naam = Component.literal(k.getStringOr("N", ""));
        g.pose().pushMatrix();
        g.pose().translate(cx, y + KAART_H - 12);
        g.pose().scale(0.6f, 0.6f);
        g.centeredText(font, naam, 0, 0, 0xFF402030);
        g.centeredText(font, Component.translatable("gui.guhs.gezel.aard." + WieIsHet.AARDEN[Math.max(0, Math.min(3, k.getIntOr("A", 0)))]), 0, 9, 0xFF8A6A78);
        g.pose().popMatrix();
    }

    // --- the side panel ----------------------------------------------------------------------------------------------

    private void tekenZij(GuiGraphicsExtractor g) {
        int rx = left + 226, rw = 106;
        String tegen = data.getStringOr("Tegen", "");
        int y = BordSpel.WIEISHET.equals(spel()) ? top + 152 : top + 22;
        g.text(font, Component.translatable("gui.guhs.gezel.tegen", tegen), rx, y, DOF, false);
        Component status;
        int uitslag = data.getIntOr("Uitslag", -1);
        if ("einde".equals(fase())) {
            status = Component.translatable(switch (uitslag) {
                case 0 -> "gui.guhs.gezel.uitslag.gewonnen";
                case 1 -> "gui.guhs.gezel.uitslag.verloren";
                case 2 -> "gui.guhs.gezel.uitslag.gelijk";
                default -> "gui.guhs.gezel.uitslag.gestopt";
            });
        } else {
            status = Component.translatable(data.getBooleanOr("Mijn", false) ? (raden ? "gui.guhs.gezel.raad_nu" : "gui.guhs.gezel.jouw_beurt")
                    : "gui.guhs.gezel.wacht_beurt", tegen);
        }
        g.text(font, status, rx, y + 11, uitslag == 0 ? GOUD : TEKST, false);
        if (BordSpel.WIEISHET.equals(spel()) && data.contains("AnderOpen")) {
            g.text(font, Component.translatable("gui.guhs.gezel.ander_open", data.getIntOr("AnderOpen", 0)), rx, y + 22, DOF, false);
        }
        // the log: the last lines, newest at the bottom, wrapped to the panel
        ListTag log = data.getListOrEmpty("Log");
        List<net.minecraft.util.FormattedCharSequence> regels = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        for (Tag t : log) {
            Component c = mc.level == null ? Component.empty()
                    : ComponentSerialization.CODEC.parse(mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), t).result().orElse(Component.empty());
            regels.addAll(font.split(c, (int) (rw / 0.75f)));
        }
        int maxRegels = BordSpel.WIEISHET.equals(spel()) ? 5 : 14;
        int eerste = Math.max(0, regels.size() - maxRegels);
        int ly = BordSpel.WIEISHET.equals(spel()) ? y + 34 : y + 36;
        g.pose().pushMatrix();
        g.pose().translate(rx, ly);
        g.pose().scale(0.75f, 0.75f);
        for (int i = eerste; i < regels.size(); i++) {
            g.text(font, regels.get(i), 0, (i - eerste) * 10, 0xFFE8D0DC, false);
        }
        g.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (!"spel".equals(fase()) || !data.getBooleanOr("Mijn", false)) {
            return false;
        }
        double mx = event.x(), my = event.y();
        if (BordSpel.OPEENRIJ.equals(spel())) {
            int bx = bordX(), by = bordY();
            if (mx >= bx && mx < bx + OpEenRij.BREED * CEL && my >= by - 20 && my < by + OpEenRij.HOOG * CEL) {
                doe(BordSpel.ZET, (int) ((mx - bx) / CEL));
                return true;
            }
        } else if (raden) {
            long open = data.getLongOr("Open", 0L);
            List<CompoundTag> kaarten = kaarten();
            for (int i = 0; i < kaarten.size(); i++) {
                int x = kaartX(i), y = kaartY(i);
                if ((open & (1L << i)) != 0 && mx >= x && mx < x + KAART_W && my >= y && my < y + KAART_H) {
                    doe(BordSpel.RAAD, i);
                    raden = false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
