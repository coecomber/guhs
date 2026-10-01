package nl.juiced.guhs.client.screen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.client.GuhsClientConfig;
import nl.juiced.guhs.client.GuhsTaal;
import nl.juiced.guhs.taal.Taal;

/**
 * 1.2.0: the language question, once per installation, the first time you join a world or a server (GuhsTaal.onLogin; the
 * config's languageChosen remembers the answer). Everything is shown in BOTH languages, whatever the switch says now:
 * "Welke taal wil je voor Guhs? / Which language for Guhs?", the buttons Nederlands, English and Automatisch / Automatic
 * (follows Minecraft), and where to change it later (the Guhdex, or Mods, Guhs, Config). Esc keeps the current choice.
 */
public class TaalVraagScreen extends Screen {
    private static final int W = 270, PAD = 12, ROW_H = 20, GAP = 4;
    private static final int COLOR_PANEL = 0xF0301A26, COLOR_BORDER = 0xFFF7B6CB, COLOR_TITLE = 0xFFFFE6EE, COLOR_EN = 0xFFF7B6CB,
            COLOR_SOFT = 0xFFC8A8B8;
    private static final float SMALL = 0.75f;

    private int left, top, h;
    private List<FormattedCharSequence> laterNl = List.of(), laterEn = List.of();
    private int knoppenY;

    public TaalVraagScreen() {
        super(Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.vraag", true) + " / " + GuhsTaal.tekst("gui.guhs.taalvraag.vraag", false)));
    }

    @Override
    protected void init() {
        int tekstW = (int) Math.floor((W - 2 * PAD) / SMALL);
        laterNl = font.split(Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.later", true)), tekstW);
        laterEn = font.split(Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.later", false)), tekstW);
        int laterH = Math.round((laterNl.size() + laterEn.size()) * 10 * SMALL) + 4;
        // the questions (two lines), the buttons (two rows), the "change it later" lines
        knoppenY = 46;
        h = knoppenY + 2 * ROW_H + GAP + 10 + laterH + PAD - 2;
        left = (width - W) / 2;
        top = Math.max(4, (height - h) / 2);
        int half = (W - 2 * PAD - GAP) / 2;
        int y = top + knoppenY;
        addRenderableWidget(Button.builder(Component.literal(GuhsTaal.tekst("gui.guhs.taal.nl", true)), b -> kies(Taal.NL))
                .bounds(left + PAD, y, half, ROW_H).build());
        addRenderableWidget(Button.builder(Component.literal(GuhsTaal.tekst("gui.guhs.taal.en", false)), b -> kies(Taal.EN))
                .bounds(left + PAD + half + GAP, y, W - 2 * PAD - half - GAP, ROW_H).build());
        Component auto = Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.auto", true) + " / " + GuhsTaal.tekst("gui.guhs.taalvraag.auto", false));
        Component autoTip = Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.auto.tooltip", true)).append("\n")
                .append(Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.auto.tooltip", false)).withStyle(ChatFormatting.GRAY));
        addRenderableWidget(Button.builder(auto, b -> kies(Taal.AUTO))
                .bounds(left + PAD, y + ROW_H + GAP, W - 2 * PAD, ROW_H).tooltip(Tooltip.create(autoTip)).build());
    }

    private void kies(Taal taal) {
        GuhsTaal.set(taal);   // (also saves languageChosen)
        onClose();
    }

    @Override
    public void onClose() {
        if (GuhsClientConfig.SPEC.isLoaded() && !GuhsClientConfig.LANGUAGE_CHOSEN.get()) {
            GuhsClientConfig.LANGUAGE_CHOSEN.set(true);   // Esc: keep what it is (Auto), don't ask again
            GuhsClientConfig.SPEC.save();
        }
        super.onClose();
    }

    private static ItemStack knuffel() {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:knuffel_normal"));
        return new ItemStack(item == Items.AIR ? Items.PINK_DYE : item);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, COLOR_BORDER);
        g.fill(left, top, left + W, top + h, COLOR_PANEL);
        // a little plushie on both sides of the question
        ItemStack k = knuffel();
        g.item(k, left + 8, top + 11);
        g.item(k, left + W - 24, top + 11);
        int cx = left + W / 2;
        g.centeredText(font, Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.vraag", true)).withStyle(ChatFormatting.BOLD), cx, top + 10, COLOR_TITLE);
        g.centeredText(font, Component.literal(GuhsTaal.tekst("gui.guhs.taalvraag.vraag", false)).withStyle(ChatFormatting.BOLD), cx, top + 23, COLOR_EN);
        g.fill(left + 40, top + 37, left + W - 40, top + 38, 0x60F7B6CB);
        // where to change it later, in both languages
        int y = top + knoppenY + 2 * ROW_H + GAP + 10;
        g.pose().pushMatrix();
        g.pose().translate(left + PAD, y);
        g.pose().scale(SMALL, SMALL);
        int ly = 0;
        for (FormattedCharSequence line : laterNl) {
            g.text(font, line, 0, ly, COLOR_SOFT, false);
            ly += 10;
        }
        ly += 4;
        for (FormattedCharSequence line : laterEn) {
            g.text(font, line, 0, ly, COLOR_SOFT, false);
            ly += 10;
        }
        g.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
