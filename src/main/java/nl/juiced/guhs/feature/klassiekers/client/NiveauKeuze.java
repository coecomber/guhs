package nl.juiced.guhs.feature.klassiekers.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The level choice on the screens of the klassiekers: three buttons in a row (Makkelijk / Medium / Lastig, the chosen one
 * framed "» Lastig «"), each with a tooltip that tells what that level means for this game
 * (gui.guhs.klassiekers.&lt;spel&gt;.&lt;niveau&gt;). The choice is remembered per game while the game runs (medium at first),
 * and the play buttons send it along with {@link Klassiekers#metNiveau}.
 */
public final class NiveauKeuze {
    private static final Map<String, Niveau> GEKOZEN = new HashMap<>();

    /** The level chosen last on this game's screen (medium at first). */
    public static Niveau gekozen(String spel) {
        return GEKOZEN.getOrDefault(spel, Niveau.MEDIUM);
    }

    /** The action to send for this button on the chosen level. */
    public static int actie(String spel, int action) {
        return Klassiekers.metNiveau(action, gekozen(spel));
    }

    /** Adds the three level buttons in a row at (x, y), together w wide. */
    public static List<Button> knoppen(Consumer<Button> add, String spel, int x, int y, int w) {
        List<Button> buttons = new ArrayList<>();
        int gap = 4, bw = (w - 2 * gap) / 3;
        for (Niveau n : Niveau.values()) {
            Button b = Button.builder(label(spel, n), btn -> {
                        GEKOZEN.put(spel, n);
                        for (int i = 0; i < buttons.size(); i++) {
                            buttons.get(i).setMessage(label(spel, Niveau.of(i)));
                        }
                    }).bounds(x + n.ordinal() * (bw + gap), y, bw, 18)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.klassiekers." + spel + "." + n.id())
                            .append("\n").append(Component.translatable("gui.guhs.klassiekers.munten." + n.id()).withStyle(ChatFormatting.GOLD))))
                    .build();
            buttons.add(b);
            add.accept(b);
        }
        return buttons;
    }

    private static Component label(String spel, Niveau n) {
        MutableComponent name = n.naam().copy().withStyle(Klassiekers.kleur(n));
        return gekozen(spel) == n ? Component.literal("» ").append(name.withStyle(ChatFormatting.BOLD)).append(" «").withStyle(Klassiekers.kleur(n))
                : name;
    }

    /** "Jouw records: Makkelijk 120 · Medium 300 · Lastig -" centred at y (the screen's data has Best_&lt;niveau&gt;). */
    public static void records(GuiGraphicsExtractor g, Font font, CompoundTag data, int centreX, int y, java.util.function.IntFunction<String> format,
                               int none) {
        MutableComponent line = Component.translatable("gui.guhs.klassiekers.records").withStyle(ChatFormatting.GOLD);
        for (Niveau n : Niveau.values()) {
            int best = data.getIntOr("Best_" + n.id(), 0);
            line.append(Component.literal(n == Niveau.MAKKELIJK ? " " : "  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(n.naam().copy().withStyle(Klassiekers.kleur(n)))
                    .append(Component.literal(" " + (best == none ? "-" : format.apply(best))).withStyle(ChatFormatting.WHITE));
        }
        g.centeredText(font, line, centreX, y, 0xFFFFD27A);
    }

    private NiveauKeuze() {
    }
}
