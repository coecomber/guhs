package nl.juiced.guhs.feature.blub.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubMenu;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMenu;

/**
 * The Blub-guh's menu (like the piep-maatje menu, a bit bigger): the Blub on the left; on the right its name, its
 * Guh-ified meter (a bar with a (?) tooltip), its size (a bar, 75..125%), soort + tint with a swatch (and its title when it is a
 * Pure Blub or a Volle Guh),
 * then: name, Volgen / Rondvadsen, Kledingkast ({@link BlubKledingkastScreen}), Trucjes ({@link BlubTrucScreen}: every trick
 * and emote), Op je schouder, Oppakken and Doei!. Every button is a guhs:blub_actie.
 */
public class BlubMenuScreen extends Screen {
    private static final int W = 300, PREVIEW = 76, ROW = 18, GAP = 3;
    static final int KLEUR_RAND = 0xFF8AC9F7, KLEUR_PANEEL = 0xFFF4F8FF, KLEUR_VAKJE = 0xFFDDEBFB, KLEUR_TITEL = 0xFF3A5C8A,
            KLEUR_SUB = 0xFF7A8FB0, KLEUR_METER = 0xFFB0508A;

    private final BlubGuhEntity blub;
    private final int record;
    private final List<Runnable> verversers = new ArrayList<>();
    private int left, top, hoogte, kop;
    private EditBox naam;

    public BlubMenuScreen(BlubGuhEntity blub, int record) {
        super(blub.getDisplayName());
        this.blub = blub;
        this.record = record;
    }

    static void stuur(BlubGuhEntity blub, BlubMenu.Actie actie, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new BlubMenu.MenuActie(blub.getId(), actie.ordinal(), waarde, tekst));
    }

    private static Component aanUit(boolean aan) {
        return Component.translatable(aan ? "gui.guhs.piep.menu.aan" : "gui.guhs.piep.menu.uit")
                .withStyle(aan ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY);
    }

    @Override
    protected void init() {
        verversers.clear();
        kop = 7 + kopHoogte(blub) + 4;
        hoogte = kop + 6 * (ROW + GAP) + 6;
        left = (width - W) / 2;
        top = (height - hoogte) / 2;
        int x = left + PREVIEW + 10, col = W - PREVIEW - 18;
        int half = (col - GAP) / 2, rechts = x + half + GAP, rb = col - half - GAP;
        int y = top + kop;

        // the name
        naam = new EditBox(font, x + 1, y + 1, col - 42, ROW - 2, Component.translatable("gui.guhs.piep.menu.naam"));
        naam.setMaxLength(PiepMenu.MAX_NAAM);
        naam.setValue(blub.hasCustomName() ? blub.getCustomName().getString() : "");
        naam.setHint(Component.translatable("gui.guhs.piep.menu.naam").withStyle(ChatFormatting.GRAY));
        addRenderableWidget(naam);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.naam_oke"), b -> hernoem())
                .bounds(x + col - 38, y, 38, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.piep.menu.naam.tooltip"))).build());
        y += ROW + GAP;

        // follow / stay
        for (int i = 0; i < 2; i++) {
            PiepInstelling instelling = i == 0 ? PiepInstelling.VOLGEN : PiepInstelling.RONDVADSEN;
            Button b = Button.builder(label(instelling), k -> stuur(blub, BlubMenu.Actie.WISSEL, instelling.ordinal(), ""))
                    .bounds(i == 0 ? x : rechts, y, i == 0 ? half : rb, ROW).tooltip(Tooltip.create(Component.translatable(instelling.key() + ".tooltip"))).build();
            addRenderableWidget(b);
            verversers.add(() -> b.setMessage(label(instelling)));
        }
        y += ROW + GAP;

        // the wardrobe, the tricks (each its own screen, like the guh's wardrobe and emotes)
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.menu.kast"), b -> minecraft.setScreen(new BlubKledingkastScreen(blub, this)))
                .bounds(x, y, half, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.kast.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.menu.trucjes"), b -> minecraft.setScreen(new BlubTrucScreen(blub, this, record)))
                .bounds(rechts, y, rb, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.trucjes.tooltip"))).build());
        y += ROW + GAP;

        // shoulder, pick up
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.menu.schouder"), b -> {
            stuur(blub, BlubMenu.Actie.SCHOUDER, 0, "");
            onClose();
        }).bounds(x, y, half, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.schouder.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.oppakken"), b -> {
            stuur(blub, BlubMenu.Actie.OPPAKKEN, 0, "");
            onClose();
        }).bounds(rechts, y, rb, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.piep.menu.oppakken.tooltip"))).build());
        y += ROW + GAP;

        // 1.8.0: a plush of this Blub (for one wool)
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.menu.knuffel"), b -> stuur(blub, BlubMenu.Actie.KNUFFEL, 0, ""))
                .bounds(x, y, col, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.knuffel.tooltip"))).build());
        y += ROW + GAP;

        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.doei"), b -> onClose()).bounds(x, y, col, ROW).build());
        verversers.forEach(Runnable::run);
    }

    private Component label(PiepInstelling instelling) {
        return Component.translatable(instelling.key(), aanUit(blub.aan(instelling)));
    }

    private void hernoem() {
        stuur(blub, BlubMenu.Actie.NAAM, 0, naam.getValue());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (naam.isFocused() && (event.key() == 257 || event.key() == 335)) {      // enter
            hernoem();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Closes the screen when the Blub is gone, busy or too far away. */
    static boolean weg(net.minecraft.client.Minecraft mc, BlubGuhEntity blub) {
        return blub.isRemoved() || !blub.isAlive() || mc.player == null || blub.isBezig()
                || mc.player.distanceToSqr(blub) > (PiepMenu.BEREIK + 4) * (PiepMenu.BEREIK + 4);
    }

    @Override
    public void tick() {
        if (weg(minecraft, blub)) {
            onClose();
            return;
        }
        verversers.forEach(Runnable::run);
    }

    /** Lijfkleur per soort en tint (de swatch in het menu; dezelfde kleuren als de textures, uit blubmeter.py). */
    private static final int[][] TINT_KLEUR = {
            {0xFF8AC9F7, 0xFFA4DAF8, 0xFFA7CBFA},     // blauw: normaal, ijsblauw, babyblauw
            {0xFF5CB28C, 0xFF8FCFA5, 0xFF96CCA0},     // weide: normaal, muntgroen, lentegroen
            {0xFFA0D6B6, 0xFFC6DBC3, 0xFFB5DED0},     // schildje: normaal, perzik, zeepbel
            {0xFFA06CAC, 0xFFC095CB, 0xFFC291C0},     // ravijn: normaal, lavendel, framboos
            {0xFFE2E8EE, 0xFFEDECE6, 0xFFE2EAF3}};    // zwam: normaal, vanille, maanwit
    static final int KLEUR_BALK_LEEG = 0xFFE3E9F4, KLEUR_BALK_RAND = 0xFFB9C6DC, KLEUR_BLUB = 0xFF8AC9F7, KLEUR_GUH = 0xFFF2A0C8,
            KLEUR_GROOT = 0xFF9FD8B0;
    private static final int BALK = 9;

    /** Where the (?) of the guh meter was drawn last frame (for its tooltip): x, y, w, h. */
    private static final int[] VRAAG = new int[4];

    static int kopHoogte(BlubGuhEntity blub) {
        return 13 + 10 + BALK + 4 + 10 + BALK + 4 + 11 + (blub.titel() != null ? 11 : 0) + (blub.isBaby() ? 11 : 0);
    }

    private static int meng(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gr = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | gr << 8 | bl;
    }

    /** A rounded-ish bar with a fill (0..1) that runs from colour a to colour b, and a text in the middle. */
    private static void balk(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int x, int y, int w, float vul, int a, int b, String tekst) {
        g.fill(x, y, x + w, y + BALK, KLEUR_BALK_RAND);
        g.fill(x + 1, y + 1, x + w - 1, y + BALK - 1, KLEUR_BALK_LEEG);
        int vol = Math.round((w - 2) * Math.max(0f, Math.min(1f, vul)));
        for (int i = 0; i < vol; i++) {
            g.fill(x + 1 + i, y + 1, x + 2 + i, y + BALK - 1, meng(a, b, (w - 2) <= 1 ? 1f : i / (float) (w - 3)));
        }
        g.text(font, tekst, x + (w - font.width(tekst)) / 2, y + 1, 0xFF3A3050, false);
    }

    /**
     * The head of the menu at x, y: the name, the Guh-ified meter (a bar from Blub-blue to guh-pink, with a (?) whose
     * tooltip explains it), the size (a bar from 75% to 125% with a tick at 100%), soort + tint with a colour swatch,
     * the title (Pure Blub / Volle Guh) and "baby". Returns the y under it.
     */
    static int kop(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, BlubGuhEntity blub, int tx, int y, int tw) {
        Component titel = Component.literal("♥ ").append(blub.getDisplayName()).append(" ♥");
        g.text(font, font.plainSubstrByWidth(titel.getString(), tw), tx, y, KLEUR_TITEL, false);
        y += 13;

        // the Guh-ified meter
        int meter = blub.meter();
        g.text(font, Component.translatable("gui.guhs.blubguh.menu.meter_label").getString(), tx, y, KLEUR_METER, false);
        int qx = tx + tw - 9;
        g.fill(qx, y - 1, qx + 9, y + 8, KLEUR_RAND);
        g.fill(qx + 1, y, qx + 8, y + 7, KLEUR_PANEEL);
        g.text(font, "?", qx + 2, y, KLEUR_TITEL, false);
        VRAAG[0] = qx; VRAAG[1] = y - 1; VRAAG[2] = 9; VRAAG[3] = 9;
        String eind = Component.translatable("gui.guhs.blubguh.menu.meter_eind").getString();
        g.text(font, eind, qx - 3 - font.width(eind), y, KLEUR_SUB, false);
        y += 10;
        balk(g, font, tx, y, tw, meter / 100f, KLEUR_BLUB, KLEUR_GUH, meter + "%");
        y += BALK + 4;

        // the size
        int procent = blub.grootteProcent();
        g.text(font, Component.translatable("gui.guhs.blubguh.menu.grootte_label").getString(), tx, y, KLEUR_SUB, false);
        String uiterst = Component.translatable("gui.guhs.blubguh.menu.grootte_eind").getString();
        g.text(font, uiterst, tx + tw - font.width(uiterst), y, KLEUR_SUB, false);
        y += 10;
        float vul = (procent - BlubTrekking.GROOTTE_MIN_PROCENT) / (float) (BlubTrekking.GROOTTE_MAX_PROCENT - BlubTrekking.GROOTTE_MIN_PROCENT);
        balk(g, font, tx, y, tw, vul, 0xFFCDEBD6, KLEUR_GROOT, procent + "%");
        int midden = tx + 1 + Math.round((tw - 2) * 0.5f);
        g.fill(midden, y - 1, midden + 1, y, KLEUR_BALK_RAND);
        g.fill(midden, y + BALK, midden + 1, y + BALK + 1, KLEUR_BALK_RAND);
        y += BALK + 4;

        // soort + tint, with a colour swatch
        BlubTrekking.Soort soort = blub.blubSoort();
        int kleur = TINT_KLEUR[soort.ordinal() % TINT_KLEUR.length][Math.max(0, Math.min(2, blub.tint()))];
        g.fill(tx, y, tx + 8, y + 8, KLEUR_BALK_RAND);
        g.fill(tx + 1, y + 1, tx + 7, y + 7, kleur);
        Component sub = Component.translatable("gui.guhs.blubguh.menu.soort_tint", Component.translatable("gui.guhs.blubguh.soort." + soort.id()),
                Component.translatable("gui.guhs.blubguh.tint." + soort.id() + "." + blub.tint()));
        g.text(font, font.plainSubstrByWidth(sub.getString(), tw - 11), tx + 11, y, KLEUR_SUB, false);
        y += 11;
        if (blub.titel() != null) {
            g.text(font, font.plainSubstrByWidth(Component.translatable("gui.guhs.blubguh.menu.titel." + blub.titel()).getString(), tw), tx, y,
                    0xFFC08A10, false);
            y += 11;
        }
        if (blub.isBaby()) {
            g.text(font, Component.translatable("gui.guhs.blubguh.menu.baby").getString(), tx, y, KLEUR_SUB, false);
            y += 11;
        }
        return y;
    }

    /** The tooltip of the meter's (?) when the mouse is on it (call after {@link #kop}). */
    static void kopTooltip(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int mouseX, int mouseY) {
        if (mouseX >= VRAAG[0] && mouseX < VRAAG[0] + VRAAG[2] && mouseY >= VRAAG[1] && mouseY < VRAAG[1] + VRAAG[3]) {
            g.setTooltipForNextFrame(font, font.split(Component.translatable("gui.guhs.blubguh.menu.meter_uitleg"), 220), mouseX, mouseY);
        }
    }

    /** The Blub itself in a box (it looks at the mouse). */
    static void blub(GuiGraphicsExtractor g, BlubGuhEntity blub, int px, int py, int pw, int ph, int mouseX, int mouseY) {
        g.fill(px, py, px + pw, py + ph, KLEUR_VAKJE);
        float groot = Math.max(blub.getBbHeight(), blub.getBbWidth() * 0.9f);
        int schaal = (int) Math.min(Math.min(pw, ph) * 0.9f, 34 / Math.max(0.2f, groot) * (pw / 76f));
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph, schaal, 0.0625f, mouseX, mouseY, blub);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + hoogte + 2, KLEUR_RAND);
        g.fill(left, top, left + W, top + hoogte, KLEUR_PANEEL);
        kop(g, font, blub, left + PREVIEW + 10, top + 7, W - PREVIEW - 18);
        blub(g, blub, left + 6, top + 6, PREVIEW, hoogte - 12, mouseX, mouseY);
        kopTooltip(g, font, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
