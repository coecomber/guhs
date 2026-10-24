package nl.juiced.guhs.feature.ringvlucht.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.ringvlucht.VluchtPayloads;

/**
 * 1.4.2 (rookguh): the Reis-Rookguh's screen: "Waar vliegen we heen?", one button per place this player may fly to (the
 * server's list: the next place of the story first, with a star), and "Blijf hier". A click sends the choice
 * ({@link VluchtPayloads.Kies}); the server checks it again and plays the flight.
 */
public class VluchtScherm extends Screen {
    private static final int W = 300, RIJ = 22, KOP = 46;
    private static final int PANEL = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    private final int rookguh;
    private final List<CompoundTag> haltes = new ArrayList<>();
    private int left, top;

    public VluchtScherm(int rookguh, CompoundTag data) {
        super(Component.translatable("gui.guhs.ringvlucht.titel"));
        this.rookguh = rookguh;
        for (Tag t : data.getListOrEmpty("Haltes")) {
            if (t instanceof CompoundTag c) {
                haltes.add(c);
            }
        }
    }

    private int hoogte() {
        return KOP + haltes.size() * RIJ + 32;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - hoogte()) / 2);
        int y = top + KOP;
        for (CompoundTag h : haltes) {
            String id = h.getStringOr("Id", "");
            boolean volgende = h.getBooleanOr("Volgende", false);
            if (h.getBooleanOr("Grijs", false)) {             // (1.7.0: shown, not to pick yet: the reason instead of the world)
                MutableComponent grijs = Component.translatable(h.getStringOr("Naam", "")).withStyle(ChatFormatting.GRAY).append(Component.literal("  "))
                        .append(Component.translatable(h.getStringOr("Waarom", "")).withStyle(ChatFormatting.DARK_GRAY));
                Button knop = Button.builder(grijs, b -> { }).bounds(left + 10, y, W - 20, 20).build();
                knop.active = false;
                addRenderableWidget(knop);
                y += RIJ;
                continue;
            }
            MutableComponent tekst = Component.empty();
            if (volgende) {
                tekst.append(Component.literal("★ ").withStyle(ChatFormatting.GOLD));
            }
            tekst.append(Component.translatable(h.getStringOr("Naam", "")).withStyle(volgende ? ChatFormatting.GOLD : ChatFormatting.WHITE));
            tekst.append(Component.literal("  ")).append(Component.translatable(h.getStringOr("Wereld", "")).withStyle(ChatFormatting.GRAY));
            if (volgende) {
                tekst.append(Component.literal("  (").withStyle(ChatFormatting.GOLD)).append(Component.translatable("gui.guhs.ringvlucht.volgende")
                        .withStyle(ChatFormatting.GOLD)).append(Component.literal(")").withStyle(ChatFormatting.GOLD));
            }
            addRenderableWidget(Button.builder(tekst, b -> kies(id)).bounds(left + 10, y, W - 20, 20).build());
            y += RIJ;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.ringvlucht.blijf"), b -> onClose())
                .bounds(left + W - 10 - 96, top + hoogte() - 26, 96, 20).build());
    }

    private void kies(String halte) {
        ClientPacketDistributor.sendToServer(new VluchtPayloads.Kies(rookguh, halte));
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + hoogte() + 1, BORDER);
        g.fill(left, top, left + W, top + hoogte(), PANEL);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFB6D8, false);
        List<FormattedCharSequence> regels = font.split(Component.translatable("gui.guhs.ringvlucht.uitleg"), W - 20);
        for (int i = 0; i < regels.size() && i < 2; i++) {
            g.text(font, regels.get(i), left + 10, top + 21 + i * 10, TEXT, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
