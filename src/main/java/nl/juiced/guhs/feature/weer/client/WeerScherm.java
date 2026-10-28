package nl.juiced.guhs.feature.weer.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.weer.WeerFeature;
import nl.juiced.guhs.feature.weer.WeerPayloads;
import nl.juiced.guhs.feature.weer.WeerZones;
import nl.juiced.guhs.feature.weer.Weertje;

/**
 * The Weer vadser's menu (1.8.0): a button per weather. "Normaal" (off), zon, regen and onweer always; the eight guh-weertjes
 * once their weerpotje is used up, otherwise a padlock and a hint where the potje is. The chosen one has a little star.
 * Every choice is a guhs:weer_kies.
 */
public class WeerScherm extends Screen {
    private static final int W = 284, KOLOMMEN = 3, KNOP_B = 86, KNOP_H = 20, GAP = 4;
    private static final int KLEUR_RAND = 0xFFB98CFF, KLEUR_PANEEL = 0xFFF8F4FF, KLEUR_TITEL = 0xFF5A3C8A, KLEUR_SUB = 0xFF8A7AB0;

    private final WeerPayloads.Open open;
    private final Set<String> ontgrendeld;
    private Weertje gekozen;
    private int left, top, hoogte;

    public WeerScherm(WeerPayloads.Open open) {
        super(Component.translatable("block.guhs.weer_vadser"));
        this.open = open;
        this.ontgrendeld = new HashSet<>(open.ontgrendeld());
        this.gekozen = Weertje.byIndex(open.weertje());
    }

    private boolean heeft(Weertje w) {
        return w.altijd || ontgrendeld.contains(w.id());
    }

    @Override
    protected void init() {
        Weertje[] alle = Weertje.values();
        int rijen = (alle.length + KOLOMMEN - 1) / KOLOMMEN;
        hoogte = 46 + rijen * (KNOP_H + GAP) + 26;
        left = (width - W) / 2;
        top = (height - hoogte) / 2;
        int x0 = left + (W - (KOLOMMEN * KNOP_B + (KOLOMMEN - 1) * GAP)) / 2;
        for (int i = 0; i < alle.length; i++) {
            Weertje w = alle[i];
            int x = x0 + (i % KOLOMMEN) * (KNOP_B + GAP), y = top + 42 + (i / KOLOMMEN) * (KNOP_H + GAP);
            boolean mag = heeft(w);
            Component label = mag ? Component.literal(w == gekozen ? "★ " : "").append(Component.translatable(w.key()))
                    : Component.literal("? ").append(Component.translatable(w.key())).withStyle(ChatFormatting.GRAY);
            Button b = Button.builder(label, k -> kies(w)).bounds(x, y, KNOP_B, KNOP_H)
                    .tooltip(Tooltip.create(mag ? Component.translatable(w.key() + ".uitleg")
                            : Component.translatable("gui.guhs.weer.slot_hint", Component.translatable(w.key() + ".bron")))).build();
            b.active = mag;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.piep.menu.doei"), k -> onClose())
                .bounds(left + W / 2 - 40, top + hoogte - 24, 80, 18).build());
    }

    private void kies(Weertje w) {
        ClientPacketDistributor.sendToServer(new WeerPayloads.Kies(open.pos(), w.ordinal()));
        gekozen = w;
        rebuildWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + hoogte + 2, KLEUR_RAND);
        g.fill(left, top, left + W, top + hoogte, KLEUR_PANEEL);
        Component titel = Component.literal("☁ ").append(Component.translatable("block.guhs.weer_vadser")).append(" ☁");
        g.text(font, titel.getString(), left + (W - font.width(titel)) / 2, top + 8, KLEUR_TITEL, false);
        Component sub = gekozen == Weertje.NORMAAL ? Component.translatable("gui.guhs.weer.scherm.uit")
                : open.kracht() ? Component.translatable("gui.guhs.weer.scherm.aan", Component.translatable(gekozen.key()), open.straal())
                : Component.translatable("gui.guhs.weer.scherm.slaapt", open.kosten());
        g.text(font, font.plainSubstrByWidth(sub.getString(), W - 16), left + 8, top + 24, KLEUR_SUB, false);
        // a coloured line under every button: its weather's colour
        Weertje[] alle = Weertje.values();
        int x0 = left + (W - (KOLOMMEN * KNOP_B + (KOLOMMEN - 1) * GAP)) / 2;
        for (int i = 0; i < alle.length; i++) {
            int x = x0 + (i % KOLOMMEN) * (KNOP_B + GAP), y = top + 42 + (i / KOLOMMEN) * (KNOP_H + GAP);
            g.fill(x + 2, y + KNOP_H, x + KNOP_B - 2, y + KNOP_H + 2, heeft(alle[i]) ? alle[i].kleur : 0xFFCCCCCC);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
