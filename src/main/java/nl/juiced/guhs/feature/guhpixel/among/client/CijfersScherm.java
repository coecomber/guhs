package nl.juiced.guhs.feature.guhpixel.among.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.taal.Tekst;

/**
 * The personal stats board of Among Guhs (the logbook the Logboek-guh keeps beside the Kapitein-guh): your rounds, wins as
 * crew and as Mika, how often you were voted out (and how often wrongly), tasks, pushes, today's muntjes against the daily
 * cap, and the titles with what is still needed for them. Everything is per player; the server sends the numbers.
 */
public class CijfersScherm extends Screen {
    private static final int W = 300, H = 222;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, VAK = 0x40000000, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0,
            GROEN = 0xFF68D88A;
    private static final String[] LINKS = {"Rondes", "WinstCrew", "WinstMika", "LastigWinst", "Taken"};
    private static final String[] RECHTS = {"Weggestemd", "Onterecht", "Betrapt", "Geduwd", "Oefenrondjes"};

    private final CompoundTag data;
    private int left, top;

    public CijfersScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.among.cijfers.titel"));
        this.data = data;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.wachtrij.sluiten"), b -> onClose()).bounds(left + W / 2 - 40, top + H - 24, 80, 20).build());
    }

    private void kolom(GuiGraphicsExtractor g, String[] sleutels, int x, int w) {
        for (int i = 0; i < sleutels.length; i++) {
            int y = top + 36 + i * 13;
            g.fill(x, y - 2, x + w, y + 10, VAK);
            g.text(font, Component.translatable("gui.guhs.among.cijfers." + sleutels[i].toLowerCase(java.util.Locale.ROOT)), x + 4, y, TEKST, false);
            Component n = Component.literal(String.valueOf(data.getIntOr(sleutels[i], 0)));
            g.text(font, n, x + w - 4 - font.width(n), y, GOUD, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        g.centeredText(font, Component.translatable("gui.guhs.among.cijfers.van", Tekst.get(data, "Naam")), width / 2, top + 19, DOF);
        kolom(g, LINKS, left + 8, 140);
        kolom(g, RECHTS, left + 152, 140);
        // today's muntjes against the daily cap
        int y = top + 106, vandaag = data.getIntOr("Vandaag", 0), max = Math.max(1, data.getIntOr("DagMax", 1));
        g.text(font, Component.translatable("gui.guhs.among.cijfers.vandaag", vandaag, max), left + 8, y, TEKST, false);
        g.fill(left + 8, y + 11, left + W - 8, y + 17, 0xFF1A0E15);
        g.fill(left + 8, y + 11, left + 8 + (W - 16) * Math.min(vandaag, max) / max, y + 17, GROEN);
        // the titles
        y += 24;
        g.text(font, Component.translatable("gui.guhs.among.cijfers.titels").withStyle(ChatFormatting.BOLD), left + 8, y, TEKST, false);
        ListTag titels = data.getListOrEmpty("Titels");
        for (int i = 0; i < titels.size(); i++) {
            CompoundTag t = titels.getCompoundOrEmpty(i);
            boolean heeft = t.getBooleanOr("Heeft", false);
            int ty = y + 12 + i * 11;
            g.text(font, Component.literal(heeft ? "✔ " : "□ ").append(Tekst.get(t, "Naam")), left + 8, ty, heeft ? GROEN : TEKST, false);
            if (!heeft) {
                Component hint = Tekst.get(t, "Hint");
                g.text(font, Component.literal(font.plainSubstrByWidth(hint.getString(), W - 130)), left + 122, ty, DOF, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
