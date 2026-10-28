package nl.juiced.guhs.feature.dexdromen.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.dexdromen.BlubMeterkaart;

/**
 * 1.8.0: the Blub-meterkaart (opened from the Blub-guh page of the Guhdex). Per soort a row: ten boxes for the tenths of the
 * Blub-Guh-Meter (from Blub to Guh), then the two golden boxes Pure Blub and Volle Guh. Filled boxes are coloured, empty ones
 * a soft outline; hovering a box says which Blubs fill it.
 */
public class BlubMeterkaartScreen extends Screen {
    private static final int VAK = 14, GAP = 2, NAAM_W = 62, GOUD_GAP = 8;
    private static final int W = 10 + NAAM_W + 10 * (VAK + GAP) + GOUD_GAP + 2 * (VAK + GAP) + 8;
    private static final int RIJ_Y = 40, RIJ_H = VAK + 6;
    private static final int H = RIJ_Y + RIJ_H * BlubTrekking.Soort.values().length + 34;
    private static final int KLEUR_RAND = 0xFF8AC9F7, KLEUR_PANEEL = 0xFFF4F8FF, KLEUR_LEEG = 0xFFDDEBFB, KLEUR_TITEL = 0xFF3A5C8A,
            KLEUR_SUB = 0xFF7A8FB0, KLEUR_GOUD = 0xFFF2C14E, KLEUR_GOUD_LEEG = 0xFFF6E7BC;

    private final Screen parent;
    private int left, top;

    public BlubMeterkaartScreen(Screen parent) {
        super(Component.translatable("gui.guhs.meterkaart.titel"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.meterkaart.terug"), b -> onClose())
                .bounds(left + W / 2 - 40, top + H - 24, 80, 18).build());
    }

    /** The colour of a filled tenth: from Blub-blue (left) to guh-pink (right), the same way the Blubs themselves go. */
    static int kleur(int tiende) {
        float t = tiende / 9f;
        int r = (int) (0x8A + (0xF7 - 0x8A) * t), gr = (int) (0xC9 + (0xA8 - 0xC9) * t), b = (int) (0xF7 + (0xCC - 0xF7) * t);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private int vakX(int i) {
        return left + 10 + NAAM_W + i * (VAK + GAP) + (i >= 10 ? GOUD_GAP : 0);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, KLEUR_RAND);
        g.fill(left, top, left + W, top + H, KLEUR_PANEEL);
        int[] kaart = BlubMeterkaart.Client.kaart;
        g.centeredText(font, Component.translatable("gui.guhs.meterkaart.titel"), left + W / 2, top + 8, KLEUR_TITEL);
        g.centeredText(font, Component.translatable("gui.guhs.meterkaart.teller", BlubMeterkaart.gevuld(kaart), BlubMeterkaart.VAKJES),
                left + W / 2, top + 19, KLEUR_SUB);
        // the axis: Blub on the left, Guh on the right, the golden ones apart
        g.text(font, Component.translatable("gui.guhs.meterkaart.as_blub"), vakX(0), top + RIJ_Y - 10, KLEUR_SUB, false);
        Component guh = Component.translatable("gui.guhs.meterkaart.as_guh");
        g.text(font, guh, vakX(9) + VAK - font.width(guh), top + RIJ_Y - 10, KLEUR_SUB, false);
        g.text(font, "✦", vakX(10) + VAK / 2 + 1, top + RIJ_Y - 10, KLEUR_GOUD, false);
        BlubTrekking.Soort[] soorten = BlubTrekking.Soort.values();
        Component tip = null;
        for (int s = 0; s < soorten.length; s++) {
            int y = top + RIJ_Y + s * RIJ_H, rij = s < kaart.length ? kaart[s] : 0;
            Component naam = Component.translatable("gui.guhs.blubguh.soort." + soorten[s].id());
            g.text(font, font.plainSubstrByWidth(naam.getString(), NAAM_W - 4), left + 10, y + 3, KLEUR_TITEL, false);
            for (int i = 0; i < 12; i++) {
                int x = vakX(i);
                boolean vol = (rij & (1 << i)) != 0, goud = i >= 10;
                g.fill(x, y, x + VAK, y + VAK, goud ? (vol ? 0xFFC9962E : 0xFFE9D9A8) : (vol ? 0xFF6A8AB0 : 0xFFC6D6EA));
                g.fill(x + 1, y + 1, x + VAK - 1, y + VAK - 1, goud ? (vol ? KLEUR_GOUD : KLEUR_GOUD_LEEG) : (vol ? kleur(i) : KLEUR_LEEG));
                if (vol) {
                    g.centeredText(font, goud ? "✦" : "♥", x + VAK / 2, y + 3, 0xFFFFFFFF);
                }
                if (mouseX >= x && mouseX < x + VAK && mouseY >= y && mouseY < y + VAK) {
                    Component wat = i == BlubMeterkaart.PUUR ? Component.translatable("gui.guhs.meterkaart.vak.puur", naam)
                            : i == BlubMeterkaart.VOL ? Component.translatable("gui.guhs.meterkaart.vak.vol", naam)
                            : Component.translatable("gui.guhs.meterkaart.vak", naam, i * 10, i == 9 ? 100 : i * 10 + 9);
                    tip = wat.copy().append(Component.literal("\n")).append(Component.translatable(vol ? "gui.guhs.meterkaart.vak.ja" : "gui.guhs.meterkaart.vak.nee"));
                }
            }
        }
        if (tip != null) {
            g.setTooltipForNextFrame(font.split(tip.copy().withStyle(ChatFormatting.WHITE), 200), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
