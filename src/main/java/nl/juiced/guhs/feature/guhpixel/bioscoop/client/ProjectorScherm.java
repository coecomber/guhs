package nl.juiced.guhs.feature.guhpixel.bioscoop.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.bioscoop.Bioscoop;
import nl.juiced.guhs.feature.guhpixel.bioscoop.BioscoopPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The projector's screen: the list of films. YOUR films have a "Speel" button (the one that runs: "Stop"); a film you do
 * not have yet is greyed out and says how to get it. Above the list: the screen the projector found (or that it sees
 * none). The server decides everything (Bioscoop.speel / stop) and answers with a fresh list and a message.
 */
public class ProjectorScherm extends Screen {
    private static final int W = 300, H = 226, RIJ = 30, KNOP_W = 50;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A,
            ROOD = 0xFFFF8A8A;

    private CompoundTag data;
    private final GidsLijst lijst = new GidsLijst();
    private int left, top;

    public ProjectorScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.guhbioscoop.scherm.titel"));
        this.data = data;
    }

    /** The projector this screen belongs to (BlockPos as a long). */
    public long pos() {
        return data.getLongOr("Pos", 0L);
    }

    public void update(CompoundTag nieuw) {
        double scroll = lijst.scroll();
        this.data = nieuw;
        if (width > 0) {
            rebuildWidgets();
            lijst.scrollNaar(scroll);
        }
    }

    private boolean doek() {
        return data.getIntOr("DoekB", 0) > 0;
    }

    private void stuur(int actie, String film) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        if (mc.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new BioscoopPayloads.Doe(BlockPos.of(pos()), actie, film));
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        lijst.plaats(left + 8, top + 44, W - 16, H - 44 - 46);
        List<GidsLijst.Regel> regels = new ArrayList<>();
        ListTag films = data.getListOrEmpty("Films");
        String speelt = data.getStringOr("Speelt", "");
        for (int i = 0; i < films.size(); i++) {
            regels.add(new Rij(films.getCompoundOrEmpty(i), speelt));
        }
        lijst.zet(regels);
        boolean draait = !speelt.isEmpty();
        Button stop = Button.builder(Component.translatable("gui.guhs.guhbioscoop.scherm.stop"), b -> stuur(BioscoopPayloads.STOP, ""))
                .bounds(left + W / 2 - 84, top + H - 26, 80, 20).build();
        stop.active = draait;
        addRenderableWidget(stop);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W / 2 + 4, top + H - 26, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        int heb = 0;
        ListTag films = data.getListOrEmpty("Films");
        for (int i = 0; i < films.size(); i++) {
            if (films.getCompoundOrEmpty(i).getBooleanOr("Heeft", false)) {
                heb++;
            }
        }
        Component aantal = Component.translatable("gui.guhs.guhbioscoop.scherm.aantal", heb, films.size());
        g.text(font, aantal, left + W - 8 - font.width(aantal), top + 7, GOUD, false);
        Component doek = doek() ? Component.translatable("gui.guhs.guhbioscoop.scherm.doek", data.getIntOr("DoekB", 0), data.getIntOr("DoekH", 0))
                : Component.translatable("gui.guhs.guhbioscoop.scherm.geen_doek");
        GidsTekst.alinea(g, doek, left + 8, top + 20, W - 16, 0.75f, doek() ? GROEN : ROOD);
        lijst.teken(g, mouseX, mouseY, RAND, 0x30F7B6CB);
        Component melding = Tekst.get(data, "Melding");
        if (!Tekst.empty(melding)) {
            GidsTekst.alinea(g, melding, left + 8, top + H - 44, W - 16, 0.75f, TEKST);
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

    /** One film. */
    private final class Rij implements GidsLijst.Regel {
        private final String id;
        private final boolean heeft, draait;
        private final int duur, gezien;

        Rij(CompoundTag f, String speelt) {
            this.id = f.getStringOr("Id", "");
            this.heeft = f.getBooleanOr("Heeft", false);
            this.duur = f.getIntOr("Duur", 0);
            this.gezien = f.getIntOr("Gezien", 0);
            this.draait = !id.isEmpty() && id.equals(speelt);
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        private boolean opKnop(double mx, int x, int w) {
            return mx >= x + w - KNOP_W - 3 && mx < x + w - 3;
        }

        private boolean kan() {
            return heeft && (draait || doek());
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y + 1, x + w, y + RIJ - 1, draait ? 0x5068D88A : hover ? 0x40F7B6CB : 0x20F7B6CB);
            // a little film strip in front
            g.fill(x + 4, y + 5, x + 20, y + RIJ - 5, heeft ? 0xFF1C1420 : 0xFF2A2228);
            for (int i = 0; i < 4; i++) {
                g.fill(x + 5, y + 7 + i * 5, x + 7, y + 10 + i * 5, heeft ? 0xFFFFE6EE : 0xFF6A5A64);
                g.fill(x + 17, y + 7 + i * 5, x + 19, y + 10 + i * 5, heeft ? 0xFFFFE6EE : 0xFF6A5A64);
            }
            g.fill(x + 8, y + 7, x + 16, y + RIJ - 7, heeft ? (draait ? GROEN : 0xFFFF9AC8) : 0xFF4A3A44);
            int tw = w - 26 - KNOP_W - 10;
            Component naam = heeft ? Bioscoop.naam(id).withStyle(ChatFormatting.BOLD) : Bioscoop.naam(id);
            GidsTekst.passend(g, naam, x + 26, y + 4, tw, 1f, heeft ? TEKST : DOF, false);
            Component onder = heeft ? Component.translatable(gezien > 0 ? "gui.guhs.guhbioscoop.scherm.duur_gezien" : "gui.guhs.guhbioscoop.scherm.duur",
                    Math.round(duur / 20f), gezien) : Bioscoop.slot(id);
            GidsTekst.passend(g, onder, x + 26, y + 16, tw, 0.75f, heeft ? GOUD : DOF, false);
            int bx = x + w - KNOP_W - 3;
            boolean actief = kan();
            boolean bh = hover && opKnop(mouseX, x, w);
            g.fill(bx, y + 6, bx + KNOP_W, y + RIJ - 6, actief ? (bh ? 0xFFFF9AC8 : RAND) : 0xFF5A4450);
            g.fill(bx + 1, y + 7, bx + KNOP_W - 1, y + RIJ - 7, actief ? (bh ? 0xFF7A2848 : 0xFF5A1E3A) : 0xFF3A2A32);
            Component knop = Component.translatable(!heeft ? "gui.guhs.guhbioscoop.scherm.op_slot"
                    : draait ? "gui.guhs.guhbioscoop.scherm.stop" : "gui.guhs.guhbioscoop.scherm.speel");
            int kw = font.width(knop);
            float s = Math.min(1f, (KNOP_W - 6) / (float) Math.max(1, kw));
            GidsTekst.schaal(g, knop, bx + (KNOP_W - Math.round(kw * s)) / 2, y + 11 + Math.round(4 * (1 - s)), s, actief ? TEKST : DOF, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (kan() && opKnop(mx, x, w)) {
                stuur(draait ? BioscoopPayloads.STOP : BioscoopPayloads.SPEEL, id);
            }
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            tip.add(Bioscoop.naam(id).withStyle(ChatFormatting.LIGHT_PURPLE));
            Component tekst = heeft ? Bioscoop.uitleg(id) : Bioscoop.slot(id);
            for (var regel : font.getSplitter().splitLines(tekst, 190, net.minecraft.network.chat.Style.EMPTY)) {
                tip.add(Component.literal(regel.getString()).withStyle(heeft ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            }
            if (heeft && !draait && !doek()) {
                tip.add(Component.translatable("gui.guhs.guhbioscoop.melding.geen_doek").withStyle(ChatFormatting.GOLD));
            }
            return tip;
        }
    }
}
