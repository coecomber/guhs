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
 * Blub-Guh-Meter ("Blub ■■■······· Guh 30%"), soort, tint and size (and its title when it is a Pure Blub or a Volle Guh),
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
        kop = 18 + 11 * (2 + (blub.titel() != null ? 1 : 0) + (blub.isBaby() ? 1 : 0)) + 4;
        hoogte = kop + 5 * (ROW + GAP) + 6;
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

    /** The meter lines (name, meter, soort / tint / size, title, baby) at x, y; returns the y under them. */
    static int kop(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, BlubGuhEntity blub, int tx, int y, int tw) {
        Component titel = Component.literal("♥ ").append(blub.getDisplayName()).append(" ♥");
        g.text(font, font.plainSubstrByWidth(titel.getString(), tw), tx, y, KLEUR_TITEL, false);
        y += 13;
        int meter = blub.meter();
        g.text(font, Component.translatable("gui.guhs.blubguh.menu.meter", BlubTrekking.metertje(meter), meter).getString(), tx, y, KLEUR_METER, false);
        y += 11;
        BlubTrekking.Soort soort = blub.blubSoort();
        Component sub = Component.translatable("gui.guhs.blubguh.menu.soort", Component.translatable("gui.guhs.blubguh.soort." + soort.id()),
                Component.translatable("gui.guhs.blubguh.tint." + soort.id() + "." + blub.tint()), Math.round(blub.grootte() * 100));
        g.text(font, font.plainSubstrByWidth(sub.getString(), tw), tx, y, KLEUR_SUB, false);
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
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
