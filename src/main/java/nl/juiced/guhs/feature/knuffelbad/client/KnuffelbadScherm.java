package nl.juiced.guhs.feature.knuffelbad.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.knuffelbad.Badmeester;
import nl.juiced.guhs.feature.knuffelbad.Glijbaan;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadPayloads;

/**
 * Badmeester Bubbel's screen: the three slides (your record, how often you went down, the world record), your
 * eendjesmunten, how many guhs you washed and special ducks you found; his shop, a tip, and how washing works.
 */
public class KnuffelbadScherm extends Screen {
    private static final int W = 320, H = 200;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public KnuffelbadScherm(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.badmeesterguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int actie) {
        PacketDistributor.sendToServer(new KnuffelbadPayloads.Actie(npcId, actie));
        onClose();
    }

    private static Tooltip tip(String key) {
        return Tooltip.create(Component.translatable(key));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = (W - 40 - 12) / 4;
        int y = top + H - 30;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffelbad.scherm.winkel"), b -> send(Badmeester.WINKEL))
                .bounds(left + 20, y, bw, 20).tooltip(tip("gui.guhs.knuffelbad.scherm.winkel.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffelbad.scherm.wassen"), b -> send(Badmeester.WASSEN))
                .bounds(left + 24 + bw, y, bw, 20).tooltip(tip("gui.guhs.knuffelbad.scherm.wassen.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffelbad.scherm.tip"), b -> send(Badmeester.TIP))
                .bounds(left + 28 + bw * 2, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 32 + bw * 3, y, bw, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF7CD8FF);
        g.fill(left, top, left + W, top + H, 0xEE16263A);
        // water waves along the top, rubber ducks bobbing on them
        long t = minecraft == null || minecraft.level == null ? 0 : minecraft.level.getGameTime();
        for (int i = 0; i < W; i += 5) {
            int h = (int) (4 + Math.sin((i + t * 1.5) * 0.18) * 2);
            g.fill(left + i, top, left + i + 5, top + h, 0xFF5EC8FF);
        }
        for (int k = 0; k < 4; k++) {
            int dx = left + 30 + k * 80 + (int) (Math.sin(t * 0.05 + k) * 6);
            int dy = top + 2 + (int) (Math.sin(t * 0.12 + k * 2) * 1.5);
            g.fill(dx, dy, dx + 6, dy + 4, 0xFFFFD83C);
            g.fill(dx + 4, dy - 3, dx + 8, dy + 1, 0xFFFFD83C);
            g.fill(dx + 8, dy - 1, dx + 10, dy, 0xFFFF8A2A);
        }
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 11, 0xFFFFFFFF);
        g.centeredText(font, Component.translatable("gui.guhs.knuffelbad.scherm.intro"), width / 2, top + 24, 0xFFBFE8FF);
        int y = top + 40;
        int colW = (W - 24) / 3;
        int i = 0;
        for (Glijbaan baan : Glijbaan.values()) {
            CompoundTag b = data.getCompoundOrEmpty(baan.id());
            int x = left + 12 + i * colW;
            g.fill(x, y, x + colW - 6, y + 64, 0x40FFFFFF);
            g.fill(x, y, x + colW - 6, y + 2, baan.kleur);
            g.centeredText(font, baan.naam().copy().withStyle(ChatFormatting.BOLD), x + (colW - 6) / 2, y + 6, baan.licht);
            g.centeredText(font, Component.translatable("gui.guhs.knuffelbad.scherm.jouw_record", b.getIntOr("Best", 0)), x + (colW - 6) / 2, y + 20, 0xFFFFFFFF);
            g.centeredText(font, Component.translatable("gui.guhs.knuffelbad.scherm.ritten", b.getIntOr("Ritten", 0)), x + (colW - 6) / 2, y + 32, 0xFFD0D8E8);
            int rec = b.getIntOr("Record", 0);
            Component wr = rec < 0 ? Component.translatable("gui.guhs.knuffelbad.scherm.geen_record")
                    : Component.translatable("gui.guhs.knuffelbad.scherm.wereldrecord", rec);
            g.centeredText(font, wr, x + (colW - 6) / 2, y + 44, 0xFFFFD27A);
            if (rec >= 0) {
                g.centeredText(font, Component.literal(b.getStringOr("Naam", "")), x + (colW - 6) / 2, y + 54, 0xFFB8A8C8);
            }
            i++;
        }
        y += 72;
        g.centeredText(font, Component.translatable("gui.guhs.knuffelbad.scherm.munten", data.getIntOr("Munten", 0)), width / 2, y, 0xFFFFE27A);
        g.centeredText(font, Component.translatable("gui.guhs.knuffelbad.scherm.wassen_telt", data.getIntOr("Wassen", 0), data.getIntOr("Eendjes", 0),
                data.getIntOr("EendjesTotaal", 0)), width / 2, y + 12, 0xFFD8C8E8);
        g.centeredText(font, Component.translatable("gui.guhs.knuffelbad.scherm.besturing"), width / 2, y + 28, 0xFF9FB8D0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
