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
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubMenu;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMenu;

/**
 * The Blub-guh's menu (like the piep-maatje menu, a bit bigger): the Blub on the left; on the right its name, its
 * Blub-Guh-Meter ("Blub ■■■······· Guh 30%"), soort, tint and size (and its title when it is a Pure Blub or a Volle Guh),
 * then the buttons: name, Volgen / Rondvadsen, strikje (click: the next colour, or none), Op je schouder, Slaapliedje,
 * Verstoppertje, four trucjes, Hupsen!, Oppakken and Doei!. Every button is a guhs:blub_actie.
 */
public class BlubMenuScreen extends Screen {
    private static final int W = 300, PREVIEW = 76, ROW = 18, GAP = 3;
    private static final int KLEUR_RAND = 0xFF8AC9F7, KLEUR_PANEEL = 0xFFF4F8FF, KLEUR_VAKJE = 0xFFDDEBFB, KLEUR_TITEL = 0xFF3A5C8A,
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

    private void stuur(BlubMenu.Actie actie, int waarde, String tekst) {
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
        int rijen = 6;
        hoogte = kop + rijen * (ROW + GAP) + 6;
        left = (width - W) / 2;
        top = (height - hoogte) / 2;
        int x = left + PREVIEW + 10, col = W - PREVIEW - 18;
        int half = (col - GAP) / 2;
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
            Button b = Button.builder(label(instelling), k -> stuur(BlubMenu.Actie.WISSEL, instelling.ordinal(), ""))
                    .bounds(x + i * (half + GAP), y, half, ROW).tooltip(Tooltip.create(Component.translatable(instelling.key() + ".tooltip"))).build();
            addRenderableWidget(b);
            verversers.add(() -> b.setMessage(label(instelling)));
        }
        y += ROW + GAP;

        // strikje, shoulder
        Button strik = Button.builder(strikLabel(), b -> stuur(BlubMenu.Actie.STRIK, volgendeStrik(), ""))
                .bounds(x, y, half, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.strik.tooltip"))).build();
        addRenderableWidget(strik);
        verversers.add(() -> strik.setMessage(strikLabel()));
        knop(BlubMenu.Actie.SCHOUDER, 0, "gui.guhs.blubguh.menu.schouder", x + half + GAP, y, col - half - GAP, true);
        y += ROW + GAP;

        // lullaby, hide and seek
        knop(BlubMenu.Actie.SLAAPLIEDJE, 0, "gui.guhs.blubguh.menu.liedje", x, y, half, true);
        knop(BlubMenu.Actie.VERSTOPPERTJE, 0, "gui.guhs.blubguh.menu.verstop", x + half + GAP, y, col - half - GAP, true);
        y += ROW + GAP;

        // tricks
        int kw = (col - 3 * GAP) / 4;
        for (int i = 0; i < 4; i++) {
            int truc = i;
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.menu.truc." + i), b -> stuur(BlubMenu.Actie.TRUC, truc, ""))
                    .bounds(x + i * (kw + GAP), y, i == 3 ? col - 3 * (kw + GAP) : kw, ROW)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.truc." + i + ".tooltip"))).build());
        }
        y += ROW + GAP;

        // hop, pick up, bye
        int derde = (col - 2 * GAP) / 3;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.blubguh.menu.hupsen"), b -> {
            stuur(BlubMenu.Actie.HUPSEN, 0, "");
            onClose();
        }).bounds(x, y, derde, ROW).tooltip(Tooltip.create(Component.translatable("gui.guhs.blubguh.menu.hupsen.tooltip", record))).build());
        knop(BlubMenu.Actie.OPPAKKEN, 0, "gui.guhs.piep.menu.oppakken", x + derde + GAP, y, derde, true);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.doei"), b -> onClose())
                .bounds(x + 2 * (derde + GAP), y, col - 2 * (derde + GAP), ROW).build());
        verversers.forEach(Runnable::run);
    }

    private void knop(BlubMenu.Actie actie, int waarde, String key, int x, int y, int w, boolean sluit) {
        String tip = key + ".tooltip";
        Button.Builder b = Button.builder(Component.translatable(key), k -> {
            stuur(actie, waarde, "");
            if (sluit) {
                onClose();
            }
        }).bounds(x, y, w, ROW);
        if (net.minecraft.client.resources.language.I18n.exists(tip)) {
            b.tooltip(Tooltip.create(Component.translatable(tip)));
        }
        addRenderableWidget(b.build());
    }

    private Component label(PiepInstelling instelling) {
        return Component.translatable(instelling.key(), aanUit(blub.aan(instelling)));
    }

    private Component strikLabel() {
        int s = blub.strik();
        Component kleur = s < 0 ? Component.translatable("gui.guhs.blubguh.menu.strik.geen")
                : Component.translatable("color.minecraft." + DyeColor.byId(s).getName());
        return Component.translatable("gui.guhs.blubguh.menu.strik", kleur);
    }

    /** -1 (none) -> 0 .. 15 -> -1. */
    private int volgendeStrik() {
        int s = blub.strik();
        return s >= 15 ? -1 : s + 1;
    }

    private void hernoem() {
        stuur(BlubMenu.Actie.NAAM, 0, naam.getValue());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (naam.isFocused() && (event.key() == 257 || event.key() == 335)) {      // enter
            hernoem();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (blub.isRemoved() || !blub.isAlive() || minecraft.player == null || blub.isBezig()
                || minecraft.player.distanceToSqr(blub) > (PiepMenu.BEREIK + 4) * (PiepMenu.BEREIK + 4)) {
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
        int tx = left + PREVIEW + 10, tw = W - PREVIEW - 18;
        Component titel = Component.literal("♥ ").append(blub.getDisplayName()).append(" ♥");
        g.text(font, font.plainSubstrByWidth(titel.getString(), tw), tx, top + 7, KLEUR_TITEL, false);
        int y = top + 20;
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
        }
        // the Blub itself (it looks at the mouse)
        int px = left + 6, py = top + 6, pw = PREVIEW, ph = hoogte - 12;
        g.fill(px, py, px + pw, py + ph, KLEUR_VAKJE);
        float groot = Math.max(blub.getBbHeight(), blub.getBbWidth() * 0.9f);
        int schaal = (int) Math.min(70, 34 / Math.max(0.2f, groot));
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph, schaal, 0.0625f, mouseX, mouseY, blub);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
