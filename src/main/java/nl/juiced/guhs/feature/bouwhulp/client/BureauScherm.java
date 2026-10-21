package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
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

/** The Ontwerpbureau: the blueprint browser ({@link BlauwdrukLijst}) with "print" (a sheet of paper each). */
public class BureauScherm extends Screen implements OptieLijst.Houder {
    static final int W = 340, H = 196;
    private CompoundTag data;
    private int left, top;

    BureauScherm(CompoundTag data) {
        super(Component.translatable("block.guhs.ontwerpbureau"));
        this.data = data;
    }

    void update(CompoundTag nieuw) {
        this.data = nieuw;
        rebuildWidgets();
    }

    private void doe(int actie, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new GereedschapPayloads.Doe("bureau", "", "", actie << 8 | waarde & 255, tekst));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        BlauwdrukLijst.knoppen(this, data, left + 10, top + 24, W - 20, H - 52, true, this::doe);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwkeet.sluit"), b -> onClose()).bounds(left + W - 58, top + H - 22, 50, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, KOP);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        BlauwdrukLijst.teken(g, data, left + 10, top + 24, W - 20, H - 52, DONKER, TEKST, LICHT);
        GidsTekst.passend(g, Component.translatable("gui.guhs.blauwdruk.bureau.uitleg"), left + 10, top + H - 16, W - 80, 0.65f, LICHT, false);
    }

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
    public boolean isPauseScreen() {
        return false;
    }
}
