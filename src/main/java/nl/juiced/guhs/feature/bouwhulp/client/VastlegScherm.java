package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.GereedschapPayloads;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.DONKER;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.GROEN;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.KOP;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.LICHT;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.PANEEL;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.RAND;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.ROOD;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.TEKST;

/**
 * Capturing a blueprint (design 2.4): the frame's size, a − and + for each of its six sides (the frame shows in the world
 * as little sparkles), a name, and save.
 */
public class VastlegScherm extends Screen {
    static final int W = 260, H = 200;
    private static final String[] KANTEN = {"west", "oost", "onder", "boven", "noord", "zuid"};
    private CompoundTag data;
    private int left, top;
    private EditBox naam;
    private String getypt = "";

    VastlegScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.bouwhulp.vastleg.titel"));
        this.data = data;
    }

    CompoundTag data() {
        return data;
    }

    void update(CompoundTag nieuw) {
        this.data = nieuw;
        if (naam != null) {
            getypt = naam.getValue();
        }
        rebuildWidgets();
    }

    private void doe(int actie, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new GereedschapPayloads.Doe("vastleg", "", "", actie << 8 | waarde & 255, tekst));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        for (int k = 0; k < 6; k++) {
            int kant = k, y = top + 46 + (k / 2) * 22, x = left + 10 + (k % 2) * 124;
            addRenderableWidget(Button.builder(Component.literal("−"), b -> doe(1, kant * 2, "")).bounds(x + 72, y, 18, 18).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> doe(1, kant * 2 + 1, "")).bounds(x + 94, y, 18, 18).build());
        }
        naam = new EditBox(font, left + 10, top + 128, W - 20, 18, Component.translatable("gui.guhs.bouwhulp.vastleg.naam"));
        naam.setMaxLength(32);
        naam.setHint(Component.translatable("gui.guhs.bouwhulp.vastleg.naam"));
        naam.setValue(getypt);
        addRenderableWidget(naam);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwhulp.vastleg.opslaan").withStyle(ChatFormatting.BOLD),
                b -> doe(2, 0, naam.getValue())).bounds(left + 10, top + H - 26, 110, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwhulp.vastleg.annuleer"), b -> {
            doe(3, 0, "");
            onClose();
        }).bounds(left + W - 100, top + H - 26, 90, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, KOP);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        BlockPos a = BlockPos.of(data.getLongOr("Min", 0L)), b = BlockPos.of(data.getLongOr("Max", 0L));
        int x = b.getX() - a.getX() + 1, y = b.getY() - a.getY() + 1, z = b.getZ() - a.getZ() + 1, max = data.getIntOr("MaxMaat", 16);
        boolean past = Math.max(x, Math.max(y, z)) <= max;
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.vastleg.maat", x, y, z, max), left + 10, top + 24, W - 20, 0.8f, past ? GROEN : ROOD, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.vastleg.uitleg"), left + 10, top + 35, W - 20, 0.65f, LICHT, false);
        for (int k = 0; k < 6; k++) {
            int ry = top + 46 + (k / 2) * 22, rx = left + 10 + (k % 2) * 124;
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.vastleg.kant." + KANTEN[k]), rx, ry + 5, 70, 0.75f, TEKST, false);
        }
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.vastleg.eigen", data.getIntOr("Eigen", 0), data.getIntOr("MaxEigen", 30)),
                left + 10, top + 152, W - 20, 0.65f, LICHT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
