package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.GereedschapPayloads;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.DONKER;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.KOP;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.LICHT;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.PANEEL;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.RAND;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.TEKST;

/** A tool's settings (sneak-click in the air with it): its options ({@link OptieLijst}) in the keet's colours. */
public class InstelScherm extends Screen implements OptieLijst.Houder {
    static final int W = 340;
    private CompoundTag data;
    private final String gereedschap;
    private int left, top, hoog;

    InstelScherm(CompoundTag data) {
        super(Component.translatable("item.guhs." + data.getStringOr("Gereedschap", "vormstaf")));
        this.data = data;
        this.gereedschap = data.getStringOr("Gereedschap", "vormstaf");
    }

    String gereedschap() {
        return gereedschap;
    }

    void update(CompoundTag nieuw) {
        this.data = nieuw;
        for (var w : children()) {
            if (w instanceof EditBox e && e.isFocused()) {
                return;
            }
        }
        rebuildWidgets();
    }

    @Override
    protected void init() {
        hoog = 46 + OptieLijst.rijen(data, W - 20) * OptieLijst.RIJ + 22 + 26;
        left = (width - W) / 2;
        top = (height - hoog) / 2;
        OptieLijst.knoppen(this, data, left + 10, top + 34, W - 20, hoog - 34 - 30, true,
                (key, waarde, tekst) -> ClientPacketDistributor.sendToServer(new GereedschapPayloads.Doe("optie", gereedschap, key, waarde, tekst)));
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwkeet.sluit"), b -> onClose()).bounds(left + W - 58, top + hoog - 24, 50, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + hoog + 1, RAND);
        g.fill(left, top, left + W, top + hoog, PANEEL);
        g.fill(left, top, left + W, top + 18, KOP);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        int n = data.getIntOr("Niveau", 0);
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.niveau." + n), left + W - 8, top + 5, 160, 0.75f, LICHT, true);
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.uitleg." + gereedschap), left + 10, top + 22, W - 20, 0.7f, LICHT, false);
        OptieLijst.teken(g, data, left + 10, top + 34, W - 20, hoog - 34 - 30, TEKST, LICHT);
    }

    // --- OptieLijst.Houder ---

    @Override
    public Button knop(Component tekst, int x, int y, int w, Runnable actie, boolean actief) {
        Button b = Button.builder(tekst, x2 -> actie.run()).bounds(x, y, w, 18).build();
        b.active = actief;
        return addRenderableWidget(b);
    }

    @Override
    public <T extends AbstractWidget> T voegToe(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public Font font() {
        return font;
    }

    @Override
    public void opnieuw() {
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        OptieLijst.kiezer = null;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
