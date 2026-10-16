package nl.juiced.guhs.feature.einde2.reizen.client;

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
import nl.juiced.guhs.feature.einde2.reizen.E2ReizenPayloads;

/**
 * einde2 phase B, slice reizen: Appa's menu, "Waar vliegen we heen?": one button per building the player knows (the server's
 * list, with the distance), and "Blijf hier". einde2 fix-reizen: opened by a right-click on him it starts with "Opstappen"
 * (just climb on); the place where the story goes on comes first, gold, with a star and a gold frame; from another dimension
 * the places say "door de leegte". A click sends the choice; the server checks it again and Appa takes off. Long lists
 * scroll with the mouse wheel.
 */
public class FluitScherm extends Screen {
    private static final int W = 270, RIJ = 22, KOP = 46, MAX_RIJEN = 9;
    private static final int PANEL = 0xF0202A3A, BORDER = 0xFFF2C46B, TEXT = 0xFFFFF2DC;
    private final List<CompoundTag> haltes = new ArrayList<>();
    private int left, top, eerste;
    /** The top of the row where the story goes on (a gold frame round it), -1: not on screen. */
    private int verhaalY = -1;

    public FluitScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.einde2_reizen.fluit.titel"));
        for (Tag t : data.getListOrEmpty("Haltes")) {
            if (t instanceof CompoundTag c) {
                haltes.add(c);
            }
        }
    }

    private int rijen() {
        return Math.max(1, Math.min(MAX_RIJEN, haltes.size()));
    }

    private int hoogte() {
        return KOP + rijen() * RIJ + 32;
    }

    @Override
    protected void init() {
        clearWidgets();
        left = (width - W) / 2;
        top = Math.max(4, (height - hoogte()) / 2);
        int y = top + KOP;
        verhaalY = -1;
        for (int i = eerste; i < haltes.size() && i < eerste + MAX_RIJEN; i++) {
            CompoundTag h = haltes.get(i);
            String id = h.getStringOr("Id", "");
            boolean verder = h.getBooleanOr("Verder", false);
            boolean verhaal = h.getBooleanOr("Verhaal", false) || verder;
            boolean opstappen = h.getBooleanOr("Opstappen", false);
            int afstand = h.getIntOr("Afstand", -1);
            MutableComponent tekst = Component.empty();
            if (verhaal) {
                tekst.append(Component.literal("★ ").withStyle(ChatFormatting.GOLD));
                verhaalY = y;
            }
            tekst.append(Component.translatable(h.getStringOr("Naam", ""))
                    .withStyle(verhaal ? ChatFormatting.GOLD : opstappen ? ChatFormatting.AQUA : ChatFormatting.WHITE));
            if (afstand >= 0) {
                tekst.append(Component.literal("  ")).append(Component.translatable("gui.guhs.einde2_reizen.fluit.afstand", afstand).withStyle(ChatFormatting.GRAY));
            } else if (!opstappen && h.getBooleanOr("Leegte", false)) {
                tekst.append(Component.literal("  ")).append(Component.translatable("gui.guhs.einde2_reizen.fluit.leegte").withStyle(ChatFormatting.GRAY));
            }
            if (verhaal) {
                tekst.append(Component.literal("  ")).append(Component.translatable(verder ? "gui.guhs.einde2_reizen.fluit.verder"
                        : "gui.guhs.einde2_reizen.fluit.verhaal").withStyle(ChatFormatting.GOLD));
            }
            addRenderableWidget(Button.builder(tekst, b -> kies(id)).bounds(left + 10, y, W - 20, 20).build());
            y += RIJ;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_reizen.fluit.blijf"), b -> onClose())
                .bounds(left + W - 10 - 96, top + hoogte() - 26, 96, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, haltes.size() - MAX_RIJEN);
        int nieuw = Math.max(0, Math.min(max, eerste - (int) Math.signum(scrollY)));
        if (nieuw != eerste) {
            eerste = nieuw;
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void kies(String id) {
        ClientPacketDistributor.sendToServer(new E2ReizenPayloads.Kies(id));
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + hoogte() + 1, BORDER);
        g.fill(left, top, left + W, top + hoogte(), PANEL);
        if (verhaalY >= 0) {
            g.fill(left + 8, verhaalY - 2, left + W - 8, verhaalY + 22, BORDER);     // (the story's row: a gold frame)
        }
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFD27A, false);
        Component uitleg = Component.translatable(haltes.isEmpty() ? "gui.guhs.einde2_reizen.fluit.leeg" : "gui.guhs.einde2_reizen.fluit.uitleg");
        List<FormattedCharSequence> regels = font.split(uitleg, W - 20);
        for (int i = 0; i < regels.size() && i < 2; i++) {
            g.text(font, regels.get(i), left + 10, top + 21 + i * 10, TEXT, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
