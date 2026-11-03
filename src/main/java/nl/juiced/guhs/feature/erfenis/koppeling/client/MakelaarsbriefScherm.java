package nl.juiced.guhs.feature.erfenis.koppeling.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.erfenis.koppeling.KoppelingPayloads;

/**
 * The makelaarsbrief of Guhrrit on cream paper: the offer (an "opknapper" for one kaasknabbel), the stamp "BOUWKUNDIG RAPPORT:
 * VADS", and the buttons "Kopen voor 1 kaasknabbel" / "Nog even niet". Bought already: a "VERKOCHT" stamp and the button
 * "Extra Erfhekje (1 kaasknabbel)".
 */
public class MakelaarsbriefScherm extends Screen {
    private static final int W = 310, H = 200;
    private static final int PAPIER = 0xFFFFF6DC, RAND = 0xFFC9A46A, INKT = 0xFF4A3424, DOF = 0xFF9A8060, ROZE = 0xFFE0629E,
            ROOD = 0xFFC8423C, GROEN = 0xFF4E9A52;

    private final boolean gekocht;
    private final int knabbels;
    private int left, top;

    public MakelaarsbriefScherm(boolean gekocht, int knabbels) {
        super(Component.translatable("erfenis.koppeling.brief.titel"));
        this.gekocht = gekocht;
        this.knabbels = knabbels;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Component koop = Component.translatable(gekocht ? "erfenis.koppeling.brief.knop_extra" : "erfenis.koppeling.brief.knop_koop");
        Button b = addRenderableWidget(Button.builder(koop, x -> {
            if (minecraft != null && minecraft.getConnection() != null) {
                ClientPacketDistributor.sendToServer(new KoppelingPayloads.BriefKoop());
            }
            onClose();
        }).bounds(left + W / 2 - 154, top + H + 6, 150, 20).build());
        b.active = knabbels > 0;
        if (knabbels <= 0) {
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("erfenis.koppeling.brief.geen_knabbel")));
        }
        addRenderableWidget(Button.builder(Component.translatable(gekocht ? "gui.done" : "erfenis.koppeling.brief.knop_later"), x -> onClose())
                .bounds(left + W / 2 + 4, top + H + 6, 150, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        for (int y = top + 44; y < top + H - 30; y += 10) {
            g.fill(left + 12, y + 9, left + W - 12, y + 10, 0x30A08050);   // (ruled paper)
        }
        // the letterhead: "Makelaardij Guhrrit & Zn."
        g.text(font, Component.translatable("erfenis.koppeling.brief.kop").withStyle(ChatFormatting.BOLD), left + 14, top + 10, ROZE, false);
        g.text(font, Component.translatable("erfenis.koppeling.brief.kop2"), left + 14, top + 21, DOF, false);
        // a stamp with a little house, top right
        g.fill(left + W - 36, top + 8, left + W - 10, top + 34, ROZE);
        g.fill(left + W - 34, top + 10, left + W - 12, top + 32, 0xFFFFE6EE);
        g.centeredText(font, Component.literal("⌂"), left + W - 23, top + 16, ROZE);
        Component speler = minecraft != null && minecraft.player != null ? minecraft.player.getName() : Component.literal("guh");
        g.text(font, Component.translatable("erfenis.koppeling.brief.aanhef", speler).withStyle(ChatFormatting.ITALIC), left + 14, top + 34, INKT, false);
        List<FormattedCharSequence> regels = font.split(Component.translatable("erfenis.koppeling.brief.tekst"), W - 28);
        int y = top + 46;
        for (FormattedCharSequence r : regels) {
            if (y > top + H - 34) {
                break;
            }
            g.text(font, r, left + 14, y, INKT, false);
            y += 10;
        }
        Component groet = Component.translatable("erfenis.koppeling.brief.groet").withStyle(ChatFormatting.ITALIC);
        g.text(font, groet, left + W - 14 - font.width(groet), top + H - 22, INKT, false);
        // the stamp: the report (red, a bit crooked: two lines) or SOLD (green)
        Component stempel = Component.translatable(gekocht ? "erfenis.koppeling.brief.stempel_verkocht" : "erfenis.koppeling.brief.stempel_rapport");
        int sw = font.width(stempel) + 10, sx = left + 14, sy = top + H - 26;
        int kleur = gekocht ? GROEN : ROOD;
        g.fill(sx - 1, sy - 1, sx + sw + 1, sy + 13, kleur);
        g.fill(sx, sy, sx + sw, sy + 12, PAPIER);
        g.text(font, stempel, sx + 5, sy + 2, kleur, false);
        g.text(font, Component.translatable("erfenis.koppeling.brief.knabbels", knabbels), left + W - 14 - font.width(
                Component.translatable("erfenis.koppeling.brief.knabbels", knabbels)), top + H - 34, DOF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
