package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.function.IntConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

/**
 * Test round 5: a choice with many options (the shapes, the roofs, the patterns) as a grid of tiles, each with a little
 * picture and its name, instead of clicking through them one by one. A locked one is grey and says why. Picking one
 * goes back to the screen it came from.
 */
public class KeuzeRaster extends Screen {
    private static final int TEGEL_W = 92, TEGEL_H = 54, KOLOMMEN = 5;
    private static final int PANEEL = 0xF0FFF9EC, RAND = 0xFFE7B04A, KOP = 0xFFFFE9B8, DONKER = 0xFF5A3A1E, TEKST = 0xFF6A4A2A,
            ROZE = 0xFFE07AAE, GRIJS = 0xFFB8A890;

    private final Screen terug;
    private final String key;
    private final ListTag keuzes;
    private final int huidig;
    private final IntConsumer kies;
    private int left, top, w, h;

    public KeuzeRaster(Screen terug, String key, ListTag keuzes, int huidig, IntConsumer kies) {
        super(Component.translatable("gui.guhs.bouwhulp.raster.titel", Component.translatable("gui.guhs.bouwhulp.optie." + key)));
        this.terug = terug;
        this.key = key;
        this.keuzes = keuzes;
        this.huidig = huidig;
        this.kies = kies;
    }

    @Override
    protected void init() {
        int rijen = (keuzes.size() + KOLOMMEN - 1) / KOLOMMEN;
        w = KOLOMMEN * (TEGEL_W + 4) + 16;
        h = 30 + rijen * (TEGEL_H + 4) + 30;
        left = (width - w) / 2;
        top = Math.max(4, (height - h) / 2);
        for (int i = 0; i < keuzes.size(); i++) {
            CompoundTag k = keuzes.getCompoundOrEmpty(i);
            int tx = left + 8 + (i % KOLOMMEN) * (TEGEL_W + 4), ty = top + 26 + (i / KOLOMMEN) * (TEGEL_H + 4);
            final int j = i;
            Button b = Button.builder(Component.empty(), x -> {
                kies.accept(j);
                onClose();
            }).bounds(tx, ty, TEGEL_W, TEGEL_H).build();
            boolean dicht = k.contains("Dicht");
            b.active = !dicht;
            Component naam = naam(k);
            b.setTooltip(Tooltip.create(dicht ? naam.copy().append("\n").append(Component.translatable(k.getStringOr("Dicht", "")).withStyle(ChatFormatting.RED))
                    : naam));
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwkeet.sluit"), x -> onClose()).bounds(left + w - 62, top + h - 24, 54, 18).build());
    }

    private Component naam(CompoundTag k) {
        return Component.translatable("gui.guhs.bouwhulp.keuze." + key + "." + k.getStringOr("Id", ""));
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(terug);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + w + 1, top + h + 1, RAND);
        g.fill(left, top, left + w, top + h, PANEEL);
        g.fill(left, top, left + w, top + 18, KOP);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.raster.uitleg"), left + 8, top + h - 18, w - 80, 0.75f, TEKST, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // the pictures and names on top of the tiles
        for (int i = 0; i < keuzes.size(); i++) {
            CompoundTag k = keuzes.getCompoundOrEmpty(i);
            int tx = left + 8 + (i % KOLOMMEN) * (TEGEL_W + 4), ty = top + 26 + (i / KOLOMMEN) * (TEGEL_H + 4);
            boolean dicht = k.contains("Dicht");
            if (i == huidig) {
                g.fill(tx - 1, ty - 1, tx + TEGEL_W + 1, ty, ROZE);
                g.fill(tx - 1, ty + TEGEL_H, tx + TEGEL_W + 1, ty + TEGEL_H + 1, ROZE);
            }
            VormIcoon.teken(g, k.getStringOr("Id", ""), tx + TEGEL_W / 2 - 14, ty + 4, 28, dicht ? GRIJS : i == huidig ? ROZE : 0xFFB06A3A);
            GidsTekst.passend(g, naam(k), tx + 4, ty + TEGEL_H - 14, TEGEL_W - 8, 0.75f, dicht ? GRIJS : DONKER, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
