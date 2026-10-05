package nl.juiced.guhs.feature.guhpixel.parkour.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.client.GuhKiezerLijst;
import nl.juiced.guhs.feature.guhpixel.parkour.ParkourPayloads;
import nl.juiced.guhs.feature.guhpixel.parkour.ParkourStats;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The screen of a Startpaaltje. Left: the route, piece by piece with its number (move a piece up or down, take it out;
 * the Finishpaaltje stays last). Right: your guhs (pick one, put it on the route or take it off; at most four) and, of
 * the guhs that run here, their laps and best lap time. Below: "Stukken aanklikken" (lay out more pieces in the world),
 * "Scores wissen" and "Klaar". The server decides everything and answers with a fresh state and a message.
 */
public class ParkourScherm extends Screen {
    private static final int W = 344, H = 232, RIJ = 20, KNOP = 13;
    private static final int LIJST_X = 8, LIJST_W = 168, RECHTS_X = 184, RECHTS_W = 152, BOVEN = 44;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, ROOD = 0xFFFF8A8A,
            GROEN = 0xFF68D88A;

    private CompoundTag data;
    private final GidsLijst lijst = new GidsLijst();
    private final GuhKiezerLijst kiezer = new GuhKiezerLijst(id -> rebuildWidgets());
    private int left, top;

    public ParkourScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.guhparkour.scherm.titel"));
        this.data = data;
    }

    public BlockPos pos() {
        return BlockPos.of(data.getLongOr("Pos", 0L));
    }

    public void update(CompoundTag nieuw) {
        double scroll = lijst.scroll();
        this.data = nieuw;
        if (width > 0) {
            rebuildWidgets();
            lijst.scrollNaar(scroll);
        }
    }

    private boolean mag() {
        return data.getBooleanOr("Mag", false);
    }

    private void stuur(int actie, int index) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new ParkourPayloads.Doe(pos(), actie, index));
        }
    }

    private void stuur(int actie, UUID id) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new ParkourPayloads.Doe(pos(), actie, id));
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        lijst.plaats(left + LIJST_X, top + BOVEN, LIJST_W, 146);
        List<GidsLijst.Regel> regels = new ArrayList<>();
        ListTag stukken = data.getListOrEmpty("Stukken");
        for (int i = 0; i < stukken.size(); i++) {
            regels.add(new Stuk(i, stukken.size(), stukken.getCompoundOrEmpty(i)));
        }
        if (regels.isEmpty()) {
            regels.add(new Leeg());
        }
        lijst.zet(regels);
        // your guhs: the ones that run here get a star
        ListTag guhs = data.getListOrEmpty("Guhs").copy();
        for (int i = 0; i < guhs.size(); i++) {
            CompoundTag g = guhs.getCompoundOrEmpty(i);
            if (g.getBooleanOr("Op", false)) {
                Tekst.put(g, "Naam", Component.literal("★ ").append(Tekst.get(g, "Naam")));
            }
        }
        kiezer.plaats(left + RECHTS_X, top + BOVEN, RECHTS_W, 80);
        kiezer.zet(guhs);
        CompoundTag gekozen = kiezer.gekozenTag();
        boolean op = gekozen != null && gekozen.getBooleanOr("Op", false);
        Button guhKnop = Button.builder(Component.translatable(op ? "gui.guhs.guhparkour.scherm.knop.af" : "gui.guhs.guhparkour.scherm.knop.op"), b -> {
            UUID id = kiezer.gekozen();
            if (id != null) {
                stuur(op ? ParkourPayloads.GUH_AF : ParkourPayloads.GUH_OP, id);
            }
        }).bounds(left + RECHTS_X, top + 172, RECHTS_W, 18).build();
        guhKnop.active = mag() && gekozen != null && (op || data.getListOrEmpty("Leden").size() < data.getIntOr("MaxGuhs", 4));
        addRenderableWidget(guhKnop);
        int y = top + H - 26;
        Button uitzetten = Button.builder(Component.translatable("gui.guhs.guhparkour.scherm.knop.uitzetten"), b -> {
            stuur(ParkourPayloads.UITZETTEN, 0);
            onClose();
        }).bounds(left + 8, y, 118, 20).build();
        uitzetten.active = mag();
        addRenderableWidget(uitzetten);
        Button wis = Button.builder(Component.translatable("gui.guhs.guhparkour.scherm.knop.wis"), b -> stuur(ParkourPayloads.WIS_SCORES, 0))
                .bounds(left + 132, y, 100, 20).build();
        wis.active = mag();
        addRenderableWidget(wis);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 8 - 96, y, 96, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        boolean finish = data.getBooleanOr("Finish", false);
        Component onder = mag() ? Component.translatable(finish ? "gui.guhs.guhparkour.scherm.onder.klaar" : "gui.guhs.guhparkour.scherm.onder.geen_finish")
                : Component.translatable("gui.guhs.guhparkour.scherm.onder.van", Tekst.get(data, "Eigenaar"));
        g.centeredText(font, onder, width / 2, top + 19, finish || !mag() ? DOF : GOUD);
        GidsTekst.passend(g, Component.translatable("gui.guhs.guhparkour.scherm.kop.route", data.getIntOr("Aantal", 0), data.getIntOr("Max", 16))
                .withStyle(ChatFormatting.BOLD), left + LIJST_X, top + 33, LIJST_W, 1f, 0xFFFF9AC8, false);
        ListTag leden = data.getListOrEmpty("Leden");
        GidsTekst.passend(g, Component.translatable("gui.guhs.guhparkour.scherm.kop.guhs", leden.size(), data.getIntOr("MaxGuhs", 4))
                .withStyle(ChatFormatting.BOLD), left + RECHTS_X, top + 33, RECHTS_W, 1f, 0xFFFF9AC8, false);
        lijst.teken(g, mouseX, mouseY, RAND, 0x30F7B6CB);
        kiezer.teken(g, mouseX, mouseY);
        int y = top + 128;
        if (leden.isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.guhparkour.scherm.geen_guhs"), left + RECHTS_X, y, RECHTS_W, 0.75f, DOF);
        }
        for (int i = 0; i < Math.min(4, leden.size()); i++) {
            CompoundTag l = leden.getCompoundOrEmpty(i);
            int rondjes = l.getIntOr("Rondjes", 0);
            Component score = rondjes <= 0 ? Component.translatable("gui.guhs.guhparkour.scherm.nog_geen_rondje")
                    : Component.translatable("gui.guhs.guhparkour.bord.score", ParkourStats.tijd(l.getIntOr("Beste", 0)), rondjes);
            int gebruikt = GidsTekst.passend(g, score, left + RECHTS_X + RECHTS_W, y + 1, 74, 0.75f, rondjes > 0 ? GOUD : DOF, true);
            GidsTekst.passend(g, Tekst.get(l, "Naam"), left + RECHTS_X, y, RECHTS_W - gebruikt - 4, 0.875f, TEKST, false);
            y += 10;
        }
        Component melding = Tekst.get(data, "Melding");
        if (!Tekst.empty(melding)) {
            GidsTekst.passend(g, melding, left + 8, top + H - 38, W - 16, 0.875f, GOUD, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = lijst.tip(mouseX, mouseY);
        if (tip == null) {
            tip = kiezer.tip(mouseX, mouseY);
        }
        if (tip != null && !tip.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return lijst.wiel(mouseX, mouseY, scrollY) || kiezer.wiel(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || lijst.klik(event.x(), event.y(), event.button()) || kiezer.klik(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return lijst.sleep(event.y()) || kiezer.sleep(event.x(), event.y(), dragX, dragY) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        lijst.los();
        kiezer.los();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void klikGeluid() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    /** No pieces yet: how to lay out a route. */
    private final class Leeg implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 120;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.guhparkour.scherm.leeg"), x + 4, y + 6, w - 8, 0.875f, DOF);
        }
    }

    /** One piece of the route. */
    private final class Stuk implements GidsLijst.Regel {
        private final int index, totaal;
        private final CompoundTag s;
        private final Component naam;
        private final boolean weg, finish, hapert;
        @Nullable
        private ItemStack icoon;

        Stuk(int index, int totaal, CompoundTag s) {
            this.index = index;
            this.totaal = totaal;
            this.s = s;
            this.naam = Tekst.get(s, "Naam");
            this.weg = s.getBooleanOr("Weg", false);
            this.finish = s.getBooleanOr("Finish", false);
            this.hapert = s.getBooleanOr("Hapert", false);
        }

        private ItemStack icoon() {
            if (icoon == null) {
                Minecraft mc = Minecraft.getInstance();
                icoon = mc.level == null || !s.contains("Icoon") ? ItemStack.EMPTY : Nbt.parseStack(mc.level.registryAccess(), s.getCompoundOrEmpty("Icoon"));
            }
            return icoon;
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        /** Which button the mouse is on: 0 up, 1 down, 2 out, -1 none. */
        private int knop(double mx, int x, int w) {
            int k = (int) Math.floor((mx - (x + w - 3 * KNOP - 3)) / KNOP);
            return k < 0 || k > 2 ? -1 : k;
        }

        private boolean kan(int knop) {
            if (!mag()) {
                return false;
            }
            boolean laatsteVrij = index == totaal - 1 || (index == totaal - 2 && data.getBooleanOr("Finish", false));
            return switch (knop) {
                case 0 -> !finish && index > 0;
                case 1 -> !finish && !laatsteVrij;
                default -> true;
            };
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y + 1, x + w, y + RIJ - 1, hover ? 0x40F7B6CB : 0x20F7B6CB);
            Component nummer = finish ? Component.literal("⚑") : Component.literal(Integer.toString(index + 1));
            GidsTekst.passend(g, nummer, x + 3, y + 6, 12, 1f, finish ? GOUD : 0xFFFF9AC8, false);
            g.item(icoon(), x + 16, y + 2);
            int tw = w - 36 - 3 * KNOP - 6;
            GidsTekst.passend(g, naam, x + 36, y + 6, tw, 1f, weg ? ROOD : hapert ? GOUD : TEKST, false);
            int bx = x + w - 3 * KNOP - 3;
            String[] tekens = {"▲", "▼", "✕"};
            int over = hover ? knop(mouseX, x, w) : -1;
            for (int k = 0; k < 3; k++) {
                boolean kan = kan(k);
                int kx = bx + k * KNOP;
                g.fill(kx, y + 4, kx + KNOP - 1, y + RIJ - 4, !kan ? 0xFF3A2A32 : over == k ? 0xFF7A2848 : 0xFF5A1E3A);
                GidsTekst.schaal(g, Component.literal(tekens[k]), kx + 3, y + 6, 0.875f, !kan ? 0xFF6A5560 : k == 2 ? ROOD : GROEN, false);
            }
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            int k = knop(mx, x, w);
            if (k >= 0 && kan(k)) {
                klikGeluid();
                stuur(k == 0 ? ParkourPayloads.OMHOOG : k == 1 ? ParkourPayloads.OMLAAG : ParkourPayloads.WEG, index);
            }
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            int k = knop(mx, x, w);
            if (k >= 0) {
                tip.add(Component.translatable("gui.guhs.guhparkour.scherm.tip.knop_" + k));
                return tip;
            }
            tip.add(naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            if (weg) {
                tip.add(Component.translatable("gui.guhs.guhparkour.scherm.tip.weg").withStyle(ChatFormatting.RED));
            } else if (hapert) {
                tip.add(Component.translatable("gui.guhs.guhparkour.scherm.tip.hapert").withStyle(ChatFormatting.GOLD));
            } else if (finish) {
                tip.add(Component.translatable("gui.guhs.guhparkour.scherm.tip.finish").withStyle(ChatFormatting.GRAY));
            }
            return tip;
        }
    }
}
