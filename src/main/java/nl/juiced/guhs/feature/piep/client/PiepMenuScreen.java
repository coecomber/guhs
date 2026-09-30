package nl.juiced.guhs.feature.piep.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.PiepMenu;
import nl.juiced.guhs.feature.piep.PiepPayloads;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The little menu of a pieppiepmuisje, Poepschilly or Schilly (much smaller than the guh menu): the creature on the left, on
 * the right its name (rename like a guh), its settings as aan/uit buttons, its big button (Op je schouder! / Kontje poetsen! /
 * Bestie-moment!), and Oppakken / Doei!. Every button is a guhs:piep_actie; the labels follow the synced state.
 */
public class PiepMenuScreen extends Screen {
    private static final int W = 236, PREVIEW = 64, COL = 156, ROW = 18, GAP = 3;
    private static final int KLEUR_RAND = 0xFFE58CB8, KLEUR_PANEEL = 0xFFFFF2F8, KLEUR_VAKJE = 0xFFFBDDEB, KLEUR_TITEL = 0xFF8A2E62,
            KLEUR_SUB = 0xFFB07A98;

    private final PiepMaatje maatje;
    private final TamableAnimal dier;
    /** Game time until which the big button still rests (0: ready). */
    private final long rustTot;
    private final List<Runnable> verversers = new ArrayList<>();
    private int left, top, hoogte;
    private EditBox naam;

    public PiepMenuScreen(PiepMaatje maatje, int rustSeconden) {
        super(maatje.dier().getDisplayName());
        this.maatje = maatje;
        this.dier = maatje.dier();
        this.rustTot = rustSeconden > 0 ? dier.level().getGameTime() + rustSeconden * 20L : 0;
    }

    private void stuur(PiepMenu.Actie actie, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new PiepPayloads.MenuActie(dier.getId(), actie.ordinal(), waarde, tekst));
    }

    private static Component aanUit(boolean aan) {
        return Component.translatable(aan ? "gui.guhs.piep.menu.aan" : "gui.guhs.piep.menu.uit")
                .withStyle(aan ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY);
    }

    @Override
    protected void init() {
        verversers.clear();
        List<PiepInstelling> instellingen = maatje.instellingen();
        int rijen = 1 + instellingen.size() + 2;
        hoogte = 30 + rijen * (ROW + GAP) + 8;
        left = (width - W) / 2;
        top = (height - hoogte) / 2;
        int x = left + PREVIEW + 8;
        int y = top + 30;

        // the name (the same as renaming a guh)
        naam = new EditBox(font, x + 1, y + 1, COL - 42, ROW - 2, Component.translatable("gui.guhs.piep.menu.naam"));
        naam.setMaxLength(PiepMenu.MAX_NAAM);
        naam.setValue(dier.hasCustomName() ? dier.getCustomName().getString() : "");
        naam.setHint(Component.translatable("gui.guhs.piep.menu.naam").withStyle(ChatFormatting.GRAY));
        naam.setTooltip(Tooltip.create(Component.translatable("gui.guhs.piep.menu.naam.tooltip")));
        addRenderableWidget(naam);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.naam_oke"), b -> hernoem())
                .bounds(x + COL - 38, y, 38, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.piep.menu.naam.tooltip"))).build());
        y += ROW + GAP;

        // the settings
        for (PiepInstelling instelling : instellingen) {
            Button b = Button.builder(label(instelling), knop -> stuur(PiepMenu.Actie.WISSEL, instelling.ordinal(), ""))
                    .bounds(x, y, COL, ROW).tooltip(Tooltip.create(Component.translatable(instelling.key() + ".tooltip"))).build();
            addRenderableWidget(b);
            verversers.add(() -> b.setMessage(label(instelling)));
            y += ROW + GAP;
        }

        // the big button
        String soort = maatje.soort();
        Button speciaal = Button.builder(speciaalLabel(), b -> {
            stuur(PiepMenu.Actie.SPECIAAL, 0, "");
            onClose();
        }).bounds(x, y, COL, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.piep.menu.speciaal." + soort + ".tooltip"))).build();
        addRenderableWidget(speciaal);
        verversers.add(() -> {
            speciaal.setMessage(speciaalLabel());
            speciaal.active = rustOver() <= 0;
        });
        y += ROW + GAP;

        // pick up, bye
        int half = (COL - GAP) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.oppakken"), b -> {
            stuur(PiepMenu.Actie.OPPAKKEN, 0, "");
            onClose();
        }).bounds(x, y, half, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.piep.menu.oppakken.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.doei"), b -> onClose())
                .bounds(x + half + GAP, y, COL - half - GAP, ROW).build());
        verversers.forEach(Runnable::run);
    }

    private Component label(PiepInstelling instelling) {
        return Component.translatable(instelling.key(), aanUit(maatje.aan(instelling)));
    }

    private int rustOver() {
        return rustTot <= 0 ? 0 : (int) Math.max(0, (rustTot - dier.level().getGameTime() + 19) / 20);
    }

    private Component speciaalLabel() {
        Component label = Component.translatable("gui.guhs.piep.menu.speciaal." + maatje.soort());
        int rust = rustOver();
        if (rust <= 0) {
            return label;
        }
        return Component.translatable("gui.guhs.piep.menu.rust", label, rust / 60, String.format(java.util.Locale.ROOT, "%02d", rust % 60));
    }

    private void hernoem() {
        stuur(PiepMenu.Actie.NAAM, 0, naam.getValue());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (naam.isFocused() && (keyCode == 257 || keyCode == 335)) {      // enter
            hernoem();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (dier.isRemoved() || !dier.isAlive() || minecraft.player == null || maatje.isBezig()
                || minecraft.player.distanceToSqr(dier) > (PiepMenu.BEREIK + 4) * (PiepMenu.BEREIK + 4)) {
            onClose();
            return;
        }
        verversers.forEach(Runnable::run);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + hoogte + 2, KLEUR_RAND);
        g.fill(left, top, left + W, top + hoogte, KLEUR_PANEEL);
        // title: the name, and what it is
        Component titel = Component.literal("♥ ").append(dier.getDisplayName()).append(" ♥");
        g.text(font, font.plainSubstrByWidth(titel.getString(), W - 16), left + 8, top + 7, KLEUR_TITEL, false);
        Component sub = Component.translatable("gui.guhs.piep.menu.sub." + maatje.soort());
        g.text(font, font.plainSubstrByWidth(sub.getString(), W - 16), left + 8, top + 18, KLEUR_SUB, false);
        // the creature itself (it looks at the mouse)
        int px = left + 6, py = top + 30, pw = PREVIEW, ph = hoogte - 38;
        g.fill(px, py, px + pw, py + ph, KLEUR_VAKJE);
        float groot = Math.max(dier.getBbHeight(), dier.getBbWidth() * 0.9f);
        int schaal = (int) Math.min(80, 30 / Math.max(0.2f, groot));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph, schaal, 0.0625f, mouseX, mouseY, dier);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
