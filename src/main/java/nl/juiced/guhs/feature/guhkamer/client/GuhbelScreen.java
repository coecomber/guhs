package nl.juiced.guhs.feature.guhkamer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.band.client.MijnGuhsTab;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhkamer.GuhkamerPayloads;

/**
 * The Guhbel's screen (2.10): on the left your own guhs nearby ("Bij jou": click one to send it to the Guhkamer), on the
 * right the guests of your Guhkamer ("In de Guhkamer": click one to call it to you), and how big your room is (it grows
 * with every zielsguh).
 */
public class GuhbelScreen extends Screen {
    private static final int W = 320, H = 222, RIJ = 28;
    private static final int PANEEL = 0xF0FFF4F8, RAND = 0xFFF7B6CB, DONKER = 0xFF3A1C30, LICHT = 0xFFB0708A, ROZE = 0xFF7A2848;

    private CompoundTag data;
    private int left, top;
    private final GidsLijst bij = new GidsLijst(), gasten = new GidsLijst();
    private final Map<String, LivingEntity> poppen = new HashMap<>();

    public GuhbelScreen(CompoundTag data) {
        super(Component.translatable("gui.guhs.guhkamer.bel.titel"));
        this.data = data;
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        poppen.clear();
        rebuildWidgets();
    }

    private ListTag lijst(String key) {
        return data.getListOrEmpty(key);
    }

    private void stuur(GuhkamerPayloads.Actie actie, String id) {
        PacketDistributor.sendToServer(new GuhkamerPayloads.Doe(actie.ordinal(), id));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int kolom = (W - 24) / 2;
        bij.plaats(left + 8, top + 58, kolom, H - 58 - 30);
        gasten.plaats(left + 16 + kolom, top + 58, kolom, H - 58 - 30);
        bij.zet(rijen("Bij", GuhkamerPayloads.Actie.STUUR, "gui.guhs.guhkamer.bel.stuur", "gui.guhs.guhkamer.bel.niemand_bij"));
        gasten.zet(rijen("Gasten", GuhkamerPayloads.Actie.ROEP, "gui.guhs.guhkamer.bel.roep", "gui.guhs.guhkamer.bel.niemand_logeert"));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 68, top + H - 25, 60, 18).build());
    }

    private List<GidsLijst.Regel> rijen(String key, GuhkamerPayloads.Actie actie, String klikKey, String leegKey) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        ListTag l = lijst(key);
        if (!data.getBooleanOr("Maag", false)) {
            out.add(new Tekst(Component.translatable("gui.guhs.guhkamer.bel.geen_maag"), LICHT));
            return out;
        }
        if (l.isEmpty()) {
            out.add(new Tekst(Component.translatable(leegKey), LICHT));
        }
        for (int i = 0; i < l.size(); i++) {
            out.add(new GuhRij(l.getCompoundOrEmpty(i), actie, klikKey));
        }
        return out;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, 0xFFFBE0EA);
        g.fill(left, top + 18, left + W, top + 19, 0xFFD27A9C);
        g.text(font, Component.translatable("gui.guhs.guhkamer.bel.titel").withStyle(ChatFormatting.BOLD), left + 8, top + 5, ROZE, false);
        int b = data.getIntOr("Breedte", 0);
        Component maat = Component.translatable("gui.guhs.guhkamer.bel.kamer", b, b, lijst("Gasten").size(), data.getIntOr("Plekken", 0));
        GidsTekst.passend(g, maat, left + W - 8, top + 5, 180, 0.875f, ROZE, true);
        Component groei = Component.translatable("gui.guhs.guhkamer.bel.groei", data.getIntOr("Zielsguhs", 0));
        GidsTekst.alinea(g, groei, left + 8, top + 23, W - 16, 0.75f, LICHT);
        int kolom = (W - 24) / 2;
        GidsTekst.schaal(g, Component.translatable("gui.guhs.guhkamer.bel.kop.bij").withStyle(ChatFormatting.BOLD), left + 8, top + 47, 0.875f, ROZE, false);
        GidsTekst.schaal(g, Component.translatable("gui.guhs.guhkamer.bel.kop.gasten").withStyle(ChatFormatting.BOLD), left + 16 + kolom, top + 47, 0.875f,
                ROZE, false);
        bij.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
        gasten.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = bij.tip(mouseX, mouseY);
        if (tip == null) {
            tip = gasten.tip(mouseX, mouseY);
        }
        if (tip != null && !tip.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return super.mouseClicked(mx, my, button) || bij.klik(mx, my, button) || gasten.klik(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return bij.wiel(mx, my, sy) || gasten.wiel(mx, my, sy) || super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return bij.sleep(my) || gasten.sleep(my) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        bij.los();
        gasten.los();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    private LivingEntity pop(CompoundTag c) {
        return c.contains("Looks") && !c.getCompoundOrEmpty("Looks").isEmpty()
                ? poppen.computeIfAbsent(c.getStringOr("Id", ""), k -> GuhPop.van(c.getCompoundOrEmpty("Looks"))) : null;
    }

    /** A guh: a little picture, its name and hearts level; click to send it / call it. */
    private final class GuhRij implements GidsLijst.Regel {
        private final CompoundTag c;
        private final GuhkamerPayloads.Actie actie;
        private final String klikKey;

        GuhRij(CompoundTag c, GuhkamerPayloads.Actie actie, String klikKey) {
            this.c = c;
            this.actie = actie;
            this.klikKey = klikKey;
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y, x + w, y + RIJ - 2, hover ? 0x40F77AB0 : 0x18F7B6CB);
            LivingEntity pop = pop(c);
            if (pop != null) {
                GuhPop.teken(g, x + 1, y + 1, x + 29, y + RIJ - 3, pop, 30, -5);
            }
            GidsTekst.passend(g, Component.literal(c.getStringOr("Naam", "")).withStyle(ChatFormatting.BOLD), x + 32, y + 4, w - 36, 0.875f, DONKER, false);
            BandNiveau n = BandNiveau.byIndex(c.getIntOr("Niveau", 0));
            Component onder = MijnGuhsTab.hartje(n).append(" ").append(MijnGuhsTab.niveauNaam(n));
            if (!c.getStringOr("Woont", "").isEmpty()) {
                onder = Component.translatable("gui.guhs.guhkamer.bel.woont", c.getStringOr("Woont", "")).append(" · ").append(onder);
            }
            GidsTekst.passend(g, onder, x + 32, y + 16, w - 36, 0.625f, MijnGuhsTab.kleur(n), false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            stuur(actie, c.getStringOr("Id", ""));
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable(klikKey, c.getStringOr("Naam", "")));
        }
    }

    /** A wrapped text. */
    private final class Tekst implements GidsLijst.Regel {
        private final Component tekst;
        private final int kleur;

        Tekst(Component tekst, int kleur) {
            this.tekst = tekst;
            this.kleur = kleur;
        }

        @Override
        public int hoogte() {
            return GidsTekst.hoogte(tekst, bij.rijBreedte() - 4, 0.75f) + 4;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, tekst, x + 2, y + 2, w - 4, 0.75f, kleur);
        }
    }
}
