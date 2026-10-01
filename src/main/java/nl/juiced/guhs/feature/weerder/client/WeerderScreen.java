package nl.juiced.guhs.feature.weerder.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.weerder.WeerderFeature;
import nl.juiced.guhs.feature.weerder.WeerderPayloads;

/**
 * The screen of a Wilde-guhweerder (1.2.0), in the style of the Guhhuisje screen: a short lief explanation, the area radius
 * as a row of steps ({@link WeerderFeature#STRALEN}), the toggle "laat de area zien" (the blue dome, {@link WeerderKoepel})
 * and "Klaar". Someone else's weerder opens read-only: "Dit is de Wilde-guhweerder van X" and grey radius buttons (the dome
 * toggle still works: it is only on your own screen).
 */
public class WeerderScreen extends Screen {
    private static final int W = 260, H = 150;
    private static final int PANEEL = 0xF0FFF4F8, RAND = 0xFFF7B6CB, TEKST = 0xFF5A3A4A, LICHT = 0xFFB0708A, ROZE = 0xFF7A2848;

    private CompoundTag data;
    private BlockPos pos;
    private int left, top;

    public WeerderScreen(CompoundTag data) {
        super(Component.translatable("block.guhs.wilde_guhweerder"));
        this.data = data;
        this.pos = BlockPos.of(data.getLongOr("Pos", 0L));
    }

    public BlockPos pos() {
        return pos;
    }

    /** New data from the server (after a button). */
    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        this.pos = BlockPos.of(nieuw.getLongOr("Pos", 0L));
        rebuildWidgets();
    }

    private boolean mag() {
        return data.getBooleanOr("MagBewerken", false);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean mag = mag();
        int straal = data.getIntOr("Straal", WeerderFeature.STANDAARD);
        int n = WeerderFeature.STRALEN.size(), bw = 40, gap = 6;
        int x0 = left + (W - (n * bw + (n - 1) * gap)) / 2;
        for (int i = 0; i < n; i++) {
            int s = WeerderFeature.STRALEN.get(i);
            Component label = s == straal ? Component.literal("[" + s + "]").withStyle(ChatFormatting.BOLD) : Component.literal(String.valueOf(s));
            Button b = Button.builder(label, k -> ClientPacketDistributor.sendToServer(new WeerderPayloads.Straal(pos, s)))
                    .bounds(x0 + i * (bw + gap), top + 86, bw, 18)
                    .tooltip(Tooltip.create(mag ? Component.translatable("gui.guhs.weerder.straal.tooltip", s)
                            : Component.translatable("gui.guhs.timmerguh.alleen_kijken"))).build();
            b.active = mag && s != straal;
            addRenderableWidget(b);
        }
        int by = top + H - 25;
        addRenderableWidget(Button.builder(koepelLabel(), b -> {
            WeerderKoepel.zet(pos, !WeerderKoepel.aan(pos));
            b.setMessage(koepelLabel());
        }).bounds(left + 8, by, 170, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.weerder.koepel.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 8 - 66, by, 66, 18).build());
    }

    private Component koepelLabel() {
        return Component.translatable("gui.guhs.weerder.koepel", Component.translatable(WeerderKoepel.aan(pos) ? "gui.guhs.huisje.aan" : "gui.guhs.huisje.uit"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, 0xFFFBE0EA);
        g.fill(left, top + 18, left + W, top + 19, 0xFFD27A9C);
        g.text(font, Component.translatable("block.guhs.wilde_guhweerder").withStyle(ChatFormatting.BOLD), left + 8, top + 5, ROZE, false);
        int y = top + 24;
        if (!mag()) {
            String eigenaar = data.getStringOr("EigenaarNaam", "");
            Component van = Component.translatable("gui.guhs.weerder.van_wie", eigenaar.isEmpty() ? "?" : eigenaar).withStyle(ChatFormatting.BOLD);
            g.fill(left + 6, y - 2, left + W - 6, y + 9, 0x40D27A9C);
            GidsTekst.passend(g, van.copy().append(Component.literal("  ")).append(Component.translatable("gui.guhs.timmerguh.alleen_kijken_kort")
                    .withStyle(ChatFormatting.ITALIC)), left + 9, y, W - 18, 0.75f, ROZE, false);
            y += 12;
        }
        GidsTekst.alinea(g, Component.translatable("gui.guhs.weerder.uitleg"), left + 9, y, W - 18, 0.75f, TEKST);
        GidsTekst.passend(g, Component.translatable("gui.guhs.weerder.straal", data.getIntOr("Straal", WeerderFeature.STANDAARD))
                .withStyle(ChatFormatting.BOLD), left + 9, top + 74, W - 18, 0.875f, ROZE, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.weerder.lief"), left + 9, top + 108, W - 18, 0.75f, LICHT, false);
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(pos.getCenter()) > 24 * 24) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
