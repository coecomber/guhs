package nl.juiced.guhs.feature.snuffel.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;

/**
 * The memory card's small pause menu (the world stays visible around it): your rank, your scents and your good deeds, and
 * three buttons: "Opslaan en naar huis" (back to exactly where you left, a player with your own things), "Verder spelen",
 * and the snuffelboekje.
 */
public final class PauzeScherm extends Screen {
    private static final int W = 208, H = 148;
    private static final int PAPIER = 0xF2F6ECD6, RAND = 0xFF8A6A3A, INKT = 0xFF4A3220, ZACHT = 0xFF7A5A3A;

    private final Rang rang;
    private final int geuren, daden;
    private int left, top;

    public PauzeScherm(CompoundTag stand) {
        super(Component.translatable("gui.guhs.snuffel.pauze.titel"));
        this.rang = Rang.metNummer(stand.getIntOr("Rang", 1));
        this.geuren = stand.getIntOr("Aantal", 0);
        this.daden = EigenStand.daden(stand).size();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.pauze.naar_huis"), b -> {
            ClientPacketDistributor.sendToServer(new SnuffelPayloads.Knop(SnuffelPayloads.Knop.NAAR_HUIS));
            onClose();
        }).bounds(left + 14, top + 70, W - 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.pauze.verder"), b -> onClose()).bounds(left + 14, top + 94, W - 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.pauze.boekje"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new BoekjeScherm());
            }
        }).bounds(left + 14, top + 118, W - 28, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // (no dimmed world: this is a small window)
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), left + W / 2, top + 8, INKT);
        int y = top + 24;
        for (var regel : font.split(rang.regel(), W - 24)) {
            g.centeredText(font, regel, left + W / 2, y, INKT);
            y += 10;
        }
        g.centeredText(font, Component.translatable("gui.guhs.snuffel.pauze.stand", geuren, daden), left + W / 2, top + 56, ZACHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
