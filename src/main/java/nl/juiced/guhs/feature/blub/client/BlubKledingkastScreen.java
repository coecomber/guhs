package nl.juiced.guhs.feature.blub.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubKleding;
import nl.juiced.guhs.feature.blub.BlubMenu;

/**
 * The Blub-guh's Kledingkast (like the guh's wardrobe, small): a big preview of the Blub on the left, per place
 * ({@link BlubKleding.Plek}: for now only the strikje) a tab, and the place's pieces as a grid of colour swatches plus
 * "niets". A click dresses the Blub at once (guhs:blub_actie KLEED) and the preview shows it. Back to the menu.
 */
public class BlubKledingkastScreen extends Screen {
    private static final int W = 300, H = 196, PREVIEW_W = 110, VAK = 22, KOLOMMEN = 6, TAB_W = 70, TAB_H = 18;
    private static final int GOUD = 0xFFF2C14E;

    private final BlubGuhEntity blub;
    private final Screen parent;
    private BlubKleding.Plek plek = BlubKleding.Plek.STRIK;
    private int left, top;

    public BlubKledingkastScreen(BlubGuhEntity blub, Screen parent) {
        super(Component.translatable("gui.guhs.blubguh.kast.titel", blub.getDisplayName()));
        this.blub = blub;
        this.parent = parent;
    }

    /** "niets" first, then the pieces of this place. */
    private List<BlubKleding.Stuk> keuzes() {
        List<BlubKleding.Stuk> out = new ArrayList<>();
        out.add(null);
        out.addAll(BlubKleding.van(plek));
        return out;
    }

    private int lijstX() {
        return left + PREVIEW_W + 16;
    }

    private int lijstY() {
        return top + 30 + TAB_H + 8;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        BlubKleding.Plek[] plekken = BlubKleding.Plek.values();
        for (int i = 0; i < plekken.length; i++) {
            BlubKleding.Plek p = plekken[i];
            addRenderableWidget(Button.builder(p.naam(), b -> plek = p).bounds(lijstX() + i * (TAB_W + 2), top + 30, TAB_W, TAB_H).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(left + W - 98, top + H - 26, 90, 20).build());
    }

    @Nullable
    private Object onder(double mx, double my) {
        List<BlubKleding.Stuk> keuzes = keuzes();
        for (int i = 0; i < keuzes.size(); i++) {
            int x = lijstX() + (i % KOLOMMEN) * (VAK + 2), y = lijstY() + (i / KOLOMMEN) * (VAK + 2);
            if (mx >= x && mx < x + VAK && my >= y && my < y + VAK) {
                return keuzes.get(i) == null ? "niets" : keuzes.get(i);
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Object o = onder(event.x(), event.y());
        if (o != null && event.button() == 0) {
            BlubMenuScreen.stuur(blub, BlubMenu.Actie.KLEED, plek.ordinal(), o instanceof BlubKleding.Stuk s ? s.id() : "");
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, GOUD);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BlubMenuScreen.KLEUR_RAND);
        g.fill(left, top, left + W, top + H, BlubMenuScreen.KLEUR_PANEEL);
        g.text(font, getTitle(), left + 8, top + 8, BlubMenuScreen.KLEUR_TITEL, false);
        g.text(font, Component.translatable("gui.guhs.blubguh.kast.uitleg"), left + 8, top + 19, BlubMenuScreen.KLEUR_SUB, false);
        BlubMenuScreen.blub(g, blub, left + 8, top + 32, PREVIEW_W, H - 40, mouseX, mouseY);
        // the swatches
        BlubKleding.Stuk aan = blub.draagt(plek);
        List<BlubKleding.Stuk> keuzes = keuzes();
        Object over = onder(mouseX, mouseY);
        for (int i = 0; i < keuzes.size(); i++) {
            BlubKleding.Stuk s = keuzes.get(i);
            int x = lijstX() + (i % KOLOMMEN) * (VAK + 2), y = lijstY() + (i / KOLOMMEN) * (VAK + 2);
            boolean gekozen = s == aan;
            boolean hover = over != null && (s == null ? "niets".equals(over) : over == s);
            g.fill(x - 1, y - 1, x + VAK + 1, y + VAK + 1, gekozen ? GOUD : hover ? 0xFF3A5C8A : 0xFFB8C8E0);
            if (s == null) {
                g.fill(x, y, x + VAK, y + VAK, 0xFFFFFFFF);
                g.centeredText(font, Component.literal("✕"), x + VAK / 2, y + 7, 0xFFB04060);
            } else {
                g.fill(x, y, x + VAK, y + VAK, 0xFF000000 | s.kleur());
            }
        }
        if (over != null) {
            Component naam = over instanceof BlubKleding.Stuk s ? s.naam() : Component.translatable("gui.guhs.kleding.niets");
            g.setTooltipForNextFrame(font.split(naam, 200), mouseX, mouseY);
        }
        g.text(font, Component.translatable("gui.guhs.blubguh.kast.aan", aan == null ? Component.translatable("gui.guhs.kleding.niets") : aan.naam()),
                lijstX(), top + H - 40, BlubMenuScreen.KLEUR_SUB, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
