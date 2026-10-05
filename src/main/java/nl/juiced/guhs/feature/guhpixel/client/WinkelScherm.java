package nl.juiced.guhs.feature.guhpixel.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.GuhpixelPayloads;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The shop of the Verkoper-guh: every offer in its group (a scrolling list), with its icon, name, price in muntjes and a
 * "Koop" button; the explanation is the tooltip. The server decides everything (Winkel.koop) and answers with a fresh
 * list and a message.
 * <p>
 * The over-the-top store dressing (lobby slice; texts gui.guhs.lobby.winkel.*): a "-0%" sale tag in the corner, a line of
 * sale and EULA jokes that changes every few seconds, how many things each group has, how many muntjes are still missing
 * for something too expensive, and the small print at the bottom.
 */
public class WinkelScherm extends Screen {
    private static final int W = 300, H = 222, RIJ = 24, KOP = 15, KNOP_W = 52;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A;
    private static final int UITVERKOOP = 0xFFD8344A;
    /** The jokes under the title: the kern's three (grap.1..3) and the lobby's (gui.guhs.lobby.winkel.grap.0..n-1), 4 seconds each. */
    private static final int GRAPPEN_KERN = 3, GRAPPEN_LOBBY = 6;
    private static final long GRAP_MS = 4000L;

    private CompoundTag data;
    private final GidsLijst lijst = new GidsLijst();
    private int left, top;

    public WinkelScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.guhpixel.winkel.titel"));
        this.data = data;
    }

    public void update(CompoundTag nieuw) {
        double scroll = lijst.scroll();
        this.data = nieuw;
        if (width > 0) {
            vul();
            lijst.scrollNaar(scroll);
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        lijst.plaats(left + 8, top + 34, W - 16, H - 34 - 46);
        vul();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W / 2 - 40, top + H - 26, 80, 20).build());
    }

    private void vul() {
        List<GidsLijst.Regel> regels = new ArrayList<>();
        ListTag aanbod = data.getListOrEmpty("Aanbod");
        String groep = null;
        int saldo = data.getIntOr("Saldo", 0);
        java.util.Map<String, Integer> perGroep = new java.util.HashMap<>();
        for (int i = 0; i < aanbod.size(); i++) {
            perGroep.merge(aanbod.getCompoundOrEmpty(i).getStringOr("Groep", ""), 1, Integer::sum);
        }
        for (int i = 0; i < aanbod.size(); i++) {
            CompoundTag a = aanbod.getCompoundOrEmpty(i);
            String g = a.getStringOr("Groep", "");
            if (!g.equals(groep)) {
                groep = g;
                regels.add(new Kop(Component.translatable("gui.guhs.guhpixel.winkel.groep." + g), perGroep.getOrDefault(g, 0)));
            }
            regels.add(new Rij(a, saldo));
        }
        if (aanbod.isEmpty()) {
            regels.add(new Kop(Component.translatable("gui.guhs.guhpixel.winkel.leeg"), 0));
        }
        lijst.zet(regels);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        int grap = (int) ((System.currentTimeMillis() / GRAP_MS) % (GRAPPEN_KERN + GRAPPEN_LOBBY));
        g.centeredText(font, Component.translatable(grap < GRAPPEN_KERN ? "gui.guhs.guhpixel.winkel.grap." + (grap + 1) : "gui.guhs.lobby.winkel.grap." + (grap - GRAPPEN_KERN)),
                width / 2, top + 19, DOF);
        // the sale tag in the corner: "-0%"
        g.fill(left + 5, top + 4, left + 37, top + 17, 0xFF7A1424);
        g.fill(left + 6, top + 5, left + 36, top + 16, UITVERKOOP);
        g.centeredText(font, Component.translatable("gui.guhs.lobby.winkel.korting").withStyle(ChatFormatting.BOLD), left + 21, top + 7, 0xFFFFFFFF);
        Component saldo = Component.translatable("gui.guhs.guhpixel.hud.muntjes", data.getIntOr("Saldo", 0));
        g.text(font, saldo, left + W - 8 - font.width(saldo), top + 7, GOUD, false);
        lijst.teken(g, mouseX, mouseY, RAND, 0x30F7B6CB);
        Component melding = Tekst.get(data, "Melding");
        if (!Tekst.empty(melding)) {
            GidsTekst.passend(g, melding, left + 8, top + H - 40, W - 16, 1f, TEKST, false);
        } else {
            GidsTekst.passend(g, Component.translatable("gui.guhs.lobby.winkel.kleine_lettertjes"), left + 8, top + H - 38, W - 16, 0.75f, DOF, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = lijst.tip(mouseX, mouseY);
        if (tip != null && !tip.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
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

    /** A group heading. */
    private record Kop(Component tekst, int aantal) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return KOP;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y + KOP - 2, x + w, y + KOP - 1, 0x60F7B6CB);
            GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x + 2, y + 3, w - 64, 1f, 0xFFFF9AC8, false);
            if (aantal > 0) {
                Component n = Component.translatable(aantal == 1 ? "gui.guhs.lobby.winkel.aantal.1" : "gui.guhs.lobby.winkel.aantal", aantal);
                GidsTekst.schaal(g, n, x + w - 3, y + 5, 0.75f, DOF, true);
            }
        }
    }

    /** One offer. */
    private static final class Rij implements GidsLijst.Regel {
        private final CompoundTag a;
        @Nullable
        private ItemStack icoon;
        private final Component naam, uitleg;
        private final int prijs, gekocht, max, tekort;
        private final Winkel.Uitkomst kan;

        Rij(CompoundTag a, int saldo) {
            this.a = a;
            this.tekort = Math.max(0, a.getIntOr("Prijs", 0) - saldo);
            this.naam = Tekst.get(a, "Naam");
            this.uitleg = Tekst.get(a, "Uitleg");
            this.prijs = a.getIntOr("Prijs", 0);
            this.gekocht = a.getIntOr("Gekocht", 0);
            this.max = a.getIntOr("Max", 0);
            this.kan = Winkel.Uitkomst.values()[Math.floorMod(a.getIntOr("Kan", 0), Winkel.Uitkomst.values().length)];
        }

        private ItemStack icoon() {
            if (icoon == null) {
                Minecraft mc = Minecraft.getInstance();
                icoon = mc.level == null ? ItemStack.EMPTY : Nbt.parseStack(mc.level.registryAccess(), a.getCompoundOrEmpty("Icoon"));
            }
            return icoon;
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        private boolean opKnop(double mx, int x, int w) {
            return mx >= x + w - KNOP_W - 3 && mx < x + w - 3;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y + 1, x + w, y + RIJ - 1, hover ? 0x40F7B6CB : 0x20F7B6CB);
            g.item(icoon(), x + 4, y + 4);
            int tw = w - 26 - KNOP_W - 10;
            GidsTekst.passend(g, naam, x + 26, y + 4, tw, 1f, TEKST, false);
            boolean op = kan == Winkel.Uitkomst.MAX;
            Component onder = op ? Component.translatable("gui.guhs.guhpixel.winkel.heb_je")
                    : Component.translatable(gekocht > 0 ? "gui.guhs.guhpixel.winkel.prijs_al" : "gui.guhs.guhpixel.winkel.prijs", prijs, gekocht);
            GidsTekst.passend(g, onder, x + 26, y + 14, tw, 0.75f, op ? GROEN : kan == Winkel.Uitkomst.TE_DUUR ? DOF : GOUD, false);
            int bx = x + w - KNOP_W - 3;
            boolean actief = kan == Winkel.Uitkomst.OK;
            boolean bh = hover && opKnop(mouseX, x, w);
            g.fill(bx, y + 4, bx + KNOP_W, y + RIJ - 4, actief ? (bh ? 0xFFFF9AC8 : RAND) : 0xFF5A4450);
            g.fill(bx + 1, y + 5, bx + KNOP_W - 1, y + RIJ - 5, actief ? (bh ? 0xFF7A2848 : 0xFF5A1E3A) : 0xFF3A2A32);
            Component knop = Component.translatable(op ? "gui.guhs.guhpixel.winkel.knop.op" : "gui.guhs.guhpixel.winkel.knop.koop");
            int kw = Minecraft.getInstance().font.width(knop);
            float s = Math.min(1f, (KNOP_W - 6) / (float) Math.max(1, kw));
            GidsTekst.schaal(g, knop, bx + (KNOP_W - Math.round(kw * s)) / 2, y + 8 + Math.round(4 * (1 - s)), s, actief ? TEKST : DOF, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (kan == Winkel.Uitkomst.OK && opKnop(mx, x, w)) {
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                if (mc.getConnection() != null) {
                    ClientPacketDistributor.sendToServer(new GuhpixelPayloads.Koop(a.getStringOr("Id", "")));
                }
            }
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            tip.add(naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            if (!Tekst.empty(uitleg)) {
                for (var regel : Minecraft.getInstance().font.getSplitter().splitLines(uitleg, 190, net.minecraft.network.chat.Style.EMPTY)) {
                    tip.add(Component.literal(regel.getString()).withStyle(ChatFormatting.WHITE));
                }
            }
            if (max > 0) {
                tip.add(Component.translatable("gui.guhs.guhpixel.winkel.max", gekocht, max).withStyle(ChatFormatting.GRAY));
            }
            switch (kan) {
                case TE_DUUR -> {
                    tip.add(Component.translatable("gui.guhs.guhpixel.winkel.nee.te_duur").withStyle(ChatFormatting.GOLD));
                    tip.add(Component.translatable("gui.guhs.lobby.winkel.tekort", tekort).withStyle(ChatFormatting.GRAY));
                }
                case EIS -> {
                    Component eis = Tekst.get(a, "Eis");
                    tip.add((Tekst.empty(eis) ? Component.translatable("gui.guhs.guhpixel.winkel.nee.eis") : eis.copy()).withStyle(ChatFormatting.GOLD));
                }
                case NIET_HIER -> tip.add(Component.translatable("gui.guhs.guhpixel.winkel.nee.niet_hier").withStyle(ChatFormatting.GOLD));
                default -> {
                }
            }
            return tip;
        }
    }
}
