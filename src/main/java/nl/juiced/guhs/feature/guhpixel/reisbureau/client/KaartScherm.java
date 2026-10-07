package nl.juiced.guhs.feature.guhpixel.reisbureau.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.reisbureau.Bestemming;
import nl.juiced.guhs.taal.Tekst;

/**
 * An ansichtkaart, read: the back of a postcard. Left the message in the guh's own voice (the destination's text) signed
 * with its name, right the stamp ("1 knabbel"), the address ("Aan mijn baas") and where it came from.
 */
public class KaartScherm extends Screen {
    private static final int W = 270, H = 164;
    private static final int PAPIER = 0xFFFBF3E2, RAND = 0xFFD8C8A8, INKT = 0xFF4A2A3A, ROZE = 0xFFD0507E, LIJN = 0xFFCDBB9C, DOF = 0xFF9A8674;
    private static final String G = "gui.guhs.reisbureau.kaart.";

    private final String bestemming;
    private final Component naam;
    private int left, top;
    @Nullable
    private ItemStack kaart;

    public KaartScherm(CompoundTag data) {
        super(Component.translatable(G + "titel"));
        this.bestemming = data.getStringOr("Bestemming", "");
        this.naam = Tekst.get(data, "Naam");
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2 - 12;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 40, top + H + 6, 80, 20).build());
    }

    private ItemStack kaart() {
        if (kaart == null) {
            Bestemming b = Bestemming.vanId(bestemming);
            kaart = b == null ? ItemStack.EMPTY : new ItemStack(b.kaart());
        }
        return kaart;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0x60000000);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        g.fill(left + 3, top + 3, left + W - 3, top + 4, LIJN);
        g.fill(left + 3, top + H - 4, left + W - 3, top + H - 3, LIJN);
        Component plek = Component.translatable("gui.guhs.reisbureau.bestemming." + bestemming);
        GidsTekst.passend(g, plek.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 9, 160, 1f, ROZE, false);
        int midden = left + 178;
        g.fill(midden, top + 10, midden + 1, top + H - 10, LIJN);
        // the message, in the guh's voice
        int y = top + 26;
        y += GidsTekst.alinea(g, Component.translatable("book.guhs.reisbureau.kaart." + bestemming), left + 10, y, 162, 0.75f, INKT);
        int onder = Math.max(y + 6, top + H - 32);
        GidsTekst.schaal(g, Component.translatable(G + "groetjes"), left + 10, onder, 0.75f, DOF, false);
        GidsTekst.passend(g, naam.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.BOLD), left + 10, onder + 9, 162, 1f, ROZE, false);
        // the stamp
        int sx = left + W - 48, sy = top + 12;
        g.fill(sx, sy, sx + 38, sy + 44, ROZE);
        g.fill(sx + 2, sy + 2, sx + 36, sy + 42, 0xFFFFE6EE);
        g.pose().pushMatrix();
        g.pose().translate(sx + 3, sy + 3);
        g.pose().scale(2f, 2f);
        g.item(kaart(), 0, 0);
        g.pose().popMatrix();
        GidsTekst.passend(g, Component.translatable(G + "postzegel"), sx + 19 - 16, sy + 35, 32, 0.625f, ROZE, false);
        // the address
        int ax = midden + 8, aw = left + W - 10 - ax;
        GidsTekst.passend(g, Component.translatable(G + "aan"), ax, top + 70, aw, 0.875f, INKT, false);
        for (int i = 0; i < 4; i++) {
            g.fill(ax, top + 82 + i * 14, ax + aw, top + 83 + i * 14, LIJN);
        }
        GidsTekst.passend(g, Component.translatable(G + "uit", plek), ax, top + 100, aw, 0.75f, DOF, false);
        GidsTekst.passend(g, Component.translatable(G + "van", naam), ax, top + 114, aw, 0.75f, DOF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
