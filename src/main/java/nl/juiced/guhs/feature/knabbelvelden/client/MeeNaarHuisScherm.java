package nl.juiced.guhs.feature.knabbelvelden.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.knabbelvelden.KnabbelPayloads;
import nl.juiced.guhs.taal.Tekst;

/** "Neem Vadsje mee naar huis?" for a ghost in the Knabbelvelden: yes = it comes home as a picked-up guh. */
public class MeeNaarHuisScherm extends Screen {
    private static final int W = 220, H = 92;
    private final int entity;
    private final Component naam;

    public MeeNaarHuisScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.knabbelvelden.vraag.titel"));
        this.entity = data.getIntOr("Entity", -1);
        this.naam = Tekst.get(data, "Naam");
    }

    @Override
    protected void init() {
        int left = (width - W) / 2, top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knabbelvelden.vraag.ja"), b -> {
            ClientPacketDistributor.sendToServer(new KnabbelPayloads.MeeNaarHuis(entity));
            onClose();
        }).bounds(left + 10, top + H - 28, (W - 30) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knabbelvelden.vraag.nee"), b -> onClose())
                .bounds(left + 20 + (W - 30) / 2, top + H - 28, (W - 30) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int left = (width - W) / 2, top = (height - H) / 2;
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, KnabbelScherm.RAND);
        g.fill(left, top, left + W, top + H, KnabbelScherm.PANEEL);
        g.fillGradient(left, top, left + W, top + 26, KnabbelScherm.KOP, KnabbelScherm.PANEEL);
        Component t = title.copy().withStyle(ChatFormatting.BOLD);
        g.text(font, t, left + (W - font.width(t)) / 2, top + 9, KnabbelScherm.ROZE, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.vraag.tekst", naam), left + 10, top + 32, W - 20, 1f, KnabbelScherm.DONKER, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.vraag.uitleg"), left + 10, top + 46, W - 20, 0.75f, KnabbelScherm.GRIJS, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
