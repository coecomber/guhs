package nl.juiced.guhs.feature.knuffelbad;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The three water slides of the Knuffelbad. Each has its own path (assets/guhs/knuffelbad/&lt;id&gt;.json, made by
 * tools/features/knuffelbad_glij.py together with the slide's blocks), its own highscore board and its own special ducks.
 */
public enum Glijbaan implements StringRepresentable {
    /** Two funnels shaped like guh faces looking up with their mouths wide open, then the pink foam bath. */
    ROZE_TRECHTER(0xFFFF8FC8, 0xFFFFE0F0),
    /** A dark tube with glowing stars spiralling round the Sterrenguh's tower, light rings, out into the pool. */
    GLIMTUNNEL(0xFF7C8CFF, 0xFFDDE4FF),
    /** Out of a giant guh's mouth over its tongue: a steep drop, a banked turn, a hump and a launch into the pool. */
    GROTE_PLONS(0xFF5EC8FF, 0xFFE0F6FF);

    /** Colours for the screen and the panel. */
    public final int kleur, licht;

    Glijbaan(int kleur, int licht) {
        this.kleur = kleur;
        this.licht = licht;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    /** The Scorebord board (and Highscores row) of this slide. */
    public String board() {
        return "glijbaan_" + id();
    }

    public Component naam() {
        return Component.translatable("gui.guhs.knuffelbad.glijbaan." + id());
    }

    /** The slide's path (loaded once, the same on both sides). */
    public GlijPad pad() {
        return GlijPad.van(this);
    }

    @Nullable
    public static Glijbaan byId(String id) {
        for (Glijbaan g : values()) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    public static Glijbaan byIndex(int i) {
        return values()[Math.floorMod(i, values().length)];
    }
}
