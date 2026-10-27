package nl.juiced.guhs.feature.blub.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubMenu;
import nl.juiced.guhs.feature.blub.BlubTruc;

/**
 * The Blub-guh's Trucjes (its menu &gt; Trucjes, like the guh's emote picker): every {@link BlubTruc} in two columns, its
 * name (hover: what it does) and "Doe!". Tricks that walk off (hupsen, verstoppertje) close the screen. Back to the menu.
 */
public class BlubTrucScreen extends Screen {
    private static final int COLS = 2, ROWS = (BlubTruc.values().length + COLS - 1) / COLS;
    private static final int COL_W = 160, GAP = 8, W = COLS * COL_W + (COLS + 1) * GAP;
    private static final int ROW_H = 19, BUTTON_H = 18, LIST_Y = 30, H = LIST_Y + ROW_H * ROWS + 4 + 20 + 8;
    private static final int NAME_X = 4, NAME_W = 100, DOE_X = 108, DOE_W = 52;
    /** Tricks that make it walk off: the screen closes. */
    private static final List<BlubTruc> SLUIT = List.of(BlubTruc.HUPSEN, BlubTruc.VERSTOPPERTJE);

    private final BlubGuhEntity blub;
    private final Screen parent;
    private final int record;
    private int left, top;

    public BlubTrucScreen(BlubGuhEntity blub, Screen parent, int record) {
        super(Component.translatable("gui.guhs.blubguh.menu.trucjes"));
        this.blub = blub;
        this.parent = parent;
        this.record = record;
    }

    private int colX(int i) {
        return left + GAP + (i / ROWS) * (COL_W + GAP);
    }

    private int rowY(int i) {
        return top + LIST_Y + (i % ROWS) * ROW_H;
    }

    private Component uitleg(BlubTruc truc) {
        return truc == BlubTruc.HUPSEN ? Component.translatable("gui.guhs.blubguh.truc.hupsen.tooltip", record) : truc.uitleg();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        BlubTruc[] trucs = BlubTruc.values();
        for (int i = 0; i < trucs.length; i++) {
            BlubTruc truc = trucs[i];
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.truc.doe"), b -> {
                BlubMenuScreen.stuur(blub, BlubMenu.Actie.TRUC, truc.ordinal(), "");
                if (SLUIT.contains(truc)) {
                    onClose();
                }
            }).bounds(colX(i) + DOE_X, rowY(i), DOE_W, BUTTON_H).tooltip(Tooltip.create(uitleg(truc))).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(left + W - GAP - 90, top + LIST_Y + ROWS * ROW_H + 4, 90, 20).build());
    }

    @Override
    public void tick() {
        if (BlubMenuScreen.weg(minecraft, blub)) {
            onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, BlubMenuScreen.KLEUR_RAND);
        g.fill(left, top, left + W, top + H, BlubMenuScreen.KLEUR_PANEEL);
        g.centeredText(font, Component.translatable("gui.guhs.blubguh.trucjes.titel", blub.getDisplayName()), left + W / 2, top + 9,
                BlubMenuScreen.KLEUR_TITEL);
        BlubTruc[] trucs = BlubTruc.values();
        for (int i = 0; i < trucs.length; i++) {
            BlubTruc truc = trucs[i];
            int x = colX(i), y = rowY(i);
            g.fill(x, y, x + COL_W, y + BUTTON_H, BlubMenuScreen.KLEUR_VAKJE);
            g.text(font, font.plainSubstrByWidth(truc.naam().getString(), NAME_W), x + NAME_X, y + 5, BlubMenuScreen.KLEUR_TITEL, false);
            if (mouseX >= x && mouseX < x + DOE_X && mouseY >= y && mouseY < y + BUTTON_H) {
                g.setTooltipForNextFrame(font.split(uitleg(truc).copy().withStyle(ChatFormatting.WHITE), 200), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
