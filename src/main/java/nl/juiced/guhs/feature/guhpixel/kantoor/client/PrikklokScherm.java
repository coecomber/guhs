package nl.juiced.guhs.feature.guhpixel.kantoor.client;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.client.GuhKiezerLijst;
import nl.juiced.guhs.feature.guhpixel.kantoor.Kantoor;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Prikklok: its (at most four) desks, who sleeps at each and for how long, when the next loonstrookje is due, and
 * the Werknemer van de maand. "Aan het werk" turns the screen into a guh picker; "Naar huis" clocks a guh out; "Loon"
 * collects its loonstrookjes. The server decides everything (Kantoor.actie) and answers with fresh data.
 */
public class PrikklokScherm extends Screen {
    private static final int W = 300, H = 226, RIJ = 36, RIJ_Y = 34, KNOP_W = 62;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A;
    private static final String G = "gui.guhs.guhkantoor.";

    private CompoundTag data;
    private final GuhKiezerLijst kiezer;
    /** -1: the desks; else the picker for this desk. */
    private int kiesVoor = -1;
    private int left, top, ticks;
    private Button inklokken;

    public PrikklokScherm(CompoundTag data) {
        super(Component.translatable(G + "scherm.titel"));
        this.data = data;
        this.kiezer = new GuhKiezerLijst(id -> {
            if (inklokken != null) {
                inklokken.active = true;
            }
        });
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        if (kiesVoor >= 0 && (kiesVoor >= bureaus().size() || bureaus().getCompoundOrEmpty(kiesVoor).getBooleanOr("Bezet", false))) {
            kiesVoor = -1;   // (that desk got taken, or went away)
        }
        if (width > 0) {
            rebuildWidgets();
        }
    }

    private ListTag bureaus() {
        return data.getListOrEmpty("Bureaus");
    }

    private void stuur(int actie, int bureau, UUID guh) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new KantoorPayloads.Actie(BlockPos.of(data.getLongOr("Klok", 0L)), actie, Math.max(0, bureau), guh));
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        inklokken = null;
        if (kiesVoor >= 0) {
            kiezer.plaats(left + 8, top + RIJ_Y + 12, W - 16, 118);
            kiezer.zet(data.getListOrEmpty("Guhs"));
            inklokken = addRenderableWidget(Button.builder(Component.translatable(G + "scherm.knop.inklokken"), b -> {
                UUID guh = kiezer.gekozen();
                if (guh != null) {
                    stuur(Kantoor.KLOK_IN, kiesVoor, guh);
                    kiesVoor = -1;
                    rebuildWidgets();
                }
            }).bounds(left + W / 2 - 104, top + H - 26, 100, 20).build());
            inklokken.active = kiezer.gekozen() != null;
            addRenderableWidget(Button.builder(Component.translatable(G + "scherm.knop.terug"), b -> {
                kiesVoor = -1;
                rebuildWidgets();
            }).bounds(left + W / 2 + 4, top + H - 26, 100, 20).build());
            return;
        }
        ListTag bureaus = bureaus();
        for (int i = 0; i < bureaus.size() && i < Kantoor.MAX_BUREAUS; i++) {
            CompoundTag b = bureaus.getCompoundOrEmpty(i);
            int y = top + RIJ_Y + i * RIJ + 8, x = left + W - 12;
            final int nr = i;
            if (!b.getBooleanOr("Bezet", false)) {
                addRenderableWidget(Button.builder(Component.translatable(G + "scherm.knop.werk"), k -> {
                    kiesVoor = nr;
                    rebuildWidgets();
                }).bounds(x - 2 * KNOP_W - 4, y, 2 * KNOP_W + 4, 20).build());
            } else if (b.getBooleanOr("Mijn", false)) {
                int wachtend = b.getIntOr("Wachtend", 0);
                Button loon = addRenderableWidget(Button.builder(Component.translatable(G + "scherm.knop.loon", wachtend), k -> stuur(Kantoor.LOON, nr, Util.NIL_UUID))
                        .bounds(x - KNOP_W, y, KNOP_W, 20).tooltip(Tooltip.create(Component.translatable(G + "scherm.tip.loon"))).build());
                loon.active = wachtend > 0;
                addRenderableWidget(Button.builder(Component.translatable(G + "scherm.knop.huis"), k -> stuur(Kantoor.KLOK_UIT, nr, Util.NIL_UUID))
                        .bounds(x - 2 * KNOP_W - 4, y, KNOP_W, 20).tooltip(Tooltip.create(Component.translatable(G + "scherm.tip.huis"))).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W / 2 - 40, top + H - 26, 80, 20).build());
    }

    private static Component tijd(long ms) {
        long min = Math.max(0L, ms / 60000L);
        return Component.translatable(G + "scherm.tijd", min / 60, min % 60);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        g.centeredText(font, Component.translatable(G + "scherm.grap." + Math.max(1, Math.min(3, data.getIntOr("Grap", 1)))), width / 2, top + 19, DOF);
        if (kiesVoor >= 0) {
            g.centeredText(font, Component.translatable(G + "scherm.kies"), width / 2, top + RIJ_Y, GOUD);
            kiezer.teken(g, mouseX, mouseY);
        } else {
            ListTag bureaus = bureaus();
            for (int i = 0; i < Kantoor.MAX_BUREAUS; i++) {
                rij(g, i, i < bureaus.size() ? bureaus.getCompoundOrEmpty(i) : null);
            }
            CompoundTag maand = data.getCompoundOrEmpty("Maand");
            Component m = maand.isEmpty() ? Component.translatable(G + "scherm.maand.geen")
                    : Component.translatable(G + "scherm.maand", Tekst.get(maand, "Naam"), maand.getIntOr("Uren", 0));
            GidsTekst.passend(g, m, left + 10, top + RIJ_Y + Kantoor.MAX_BUREAUS * RIJ + 2, W - 20, 0.875f, GOUD, false);
        }
        Component melding = Tekst.get(data, "Melding");
        if (!Tekst.empty(melding)) {
            GidsTekst.passend(g, melding, left + 10, top + H - 38, W - 20, 0.875f, TEKST, false);
        }
    }

    /** One desk: its number, who sleeps there, the shift so far as a bar. */
    private void rij(GuiGraphicsExtractor g, int i, CompoundTag b) {
        int x = left + 8, y = top + RIJ_Y + i * RIJ, w = W - 16, tw = w - 2 * KNOP_W - 20;
        g.fill(x, y + 1, x + w, y + RIJ - 2, b == null ? 0x14F7B6CB : 0x28F7B6CB);
        Component kop = Component.translatable(G + "scherm.bureau", i + 1).withStyle(ChatFormatting.BOLD);
        if (b == null) {
            GidsTekst.passend(g, kop, x + 5, y + 5, 60, 1f, DOF, false);
            GidsTekst.passend(g, Component.translatable(G + "scherm.geen_bureau"), x + 5, y + 18, w - 10, 0.75f, DOF, false);
            return;
        }
        if (!b.getBooleanOr("Bezet", false)) {
            GidsTekst.passend(g, kop, x + 5, y + 5, tw, 1f, TEKST, false);
            GidsTekst.passend(g, Component.translatable(G + "scherm.leeg"), x + 5, y + 18, tw, 0.75f, DOF, false);
            return;
        }
        boolean mijn = b.getBooleanOr("Mijn", false);
        Component naam = Tekst.get(b, "Naam");
        Component wie = mijn ? naam.copy().withStyle(ChatFormatting.BOLD) : Component.translatable(G + "scherm.van", naam, Tekst.get(b, "Baas"));
        int breed = mijn ? tw : w - 10;
        GidsTekst.passend(g, wie, x + 5, y + 4, breed, 1f, mijn ? TEKST : DOF, false);
        int wachtend = b.getIntOr("Wachtend", 0);
        Component status = wachtend > 1 ? Component.translatable(G + "scherm.klaar", wachtend)
                : wachtend == 1 ? Component.translatable(G + "scherm.klaar.1")
                : Component.translatable(G + "scherm.slaapt", tijd(b.getLongOr("Verstreken", 0L)), tijd(b.getLongOr("Tot", 0L)));
        GidsTekst.passend(g, status, x + 5, y + 15, breed, 0.75f, wachtend > 0 ? GROEN : GOUD, false);
        float deel = 1f - b.getLongOr("Tot", Kantoor.DIENST) / (float) Kantoor.DIENST;
        GidsTekst.balk(g, x + 6, y + 26, Math.min(breed, 150), 4, wachtend >= Kantoor.POSTVAK ? 1f : deel);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (kiesVoor >= 0) {
            List<Component> tip = kiezer.tip(mouseX, mouseY);
            if (tip != null && !tip.isEmpty()) {
                g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
            return;
        }
        ListTag bureaus = bureaus();
        for (int i = 0; i < bureaus.size() && i < Kantoor.MAX_BUREAUS; i++) {
            CompoundTag b = bureaus.getCompoundOrEmpty(i);
            int y = top + RIJ_Y + i * RIJ;
            if (b.getBooleanOr("Bezet", false) && !b.getBooleanOr("Mijn", false) && mouseX >= left + 8 && mouseX < left + W - 8 && mouseY >= y && mouseY < y + RIJ) {
                g.setComponentTooltipForNextFrame(font, List.of(Component.translatable(G + "scherm.tip.ander")), mouseX, mouseY);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 100 == 0 && kiesVoor < 0) {
            stuur(Kantoor.VERVERS, 0, Util.NIL_UUID);   // (the clock runs on: fresh times every five seconds)
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return (kiesVoor >= 0 && kiezer.wiel(mouseX, mouseY, scrollY)) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || (kiesVoor >= 0 && kiezer.klik(event.x(), event.y(), event.button()));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return (kiesVoor >= 0 && kiezer.sleep(event.x(), event.y(), dragX, dragY)) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        kiezer.los();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
