package nl.juiced.guhs.feature.erfenis.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads;

/** De Erfenis (POC): "Naar je erfenis?" (the overview key outside the dimension). Ja = teleport there (made on first use). */
final class VraagScherm extends Screen {
    private static final int W = 220, H = 96;

    VraagScherm() {
        super(Component.translatable("erfenis.ui.vraag"));
    }

    @Override
    protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("erfenis.ui.ja"), b -> {
            ErfenisClient.stuur(ErfenisPayloads.Actie.simpel(ErfenisPayloads.Actie.GA));
            onClose();
        }).bounds(x + 12, y + H - 28, 94, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("erfenis.ui.nee"), b -> onClose()).bounds(x + W - 106, y + H - 28, 94, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2, y = (height - H) / 2;
        Teken.paneel(g, x, y, x + W, y + H);
        g.centeredText(font, title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), width / 2, y + 9, Teken.DONKER);
        g.textWithWordWrap(font, Component.translatable("erfenis.ui.vraag_uitleg"), x + 12, y + 24, W - 24, Teken.TEKST, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
