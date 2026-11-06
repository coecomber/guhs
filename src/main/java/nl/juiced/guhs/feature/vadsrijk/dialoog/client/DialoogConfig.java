package nl.juiced.guhs.feature.vadsrijk.dialoog.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * T-DIALOOG: the client options of the Vadsrijk talking screen (config/guhs-vadsrijk-dialoog.toml, also in the mod list:
 * Guhs > Config, and in the screen itself behind the gear button): how fast the text types itself, and how loud the little
 * voices are.
 */
public final class DialoogConfig {
    public static final ModConfigSpec SPEC;
    /** 0 = at once, 1 = slow, 2 = normal, 3 = fast. */
    public static final ModConfigSpec.IntValue SNELHEID;
    public static final ModConfigSpec.IntValue STEMMEN;
    /** Letters per second per speed (0: no typing). */
    static final int[] LETTERS_PER_SECONDE = {0, 28, 50, 100};

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SNELHEID = b.comment("Het Vadsrijk talking screen: how fast the text appears (0 = at once, 1 = slow, 2 = normal, 3 = fast).")
                .translation("guhs.configuration.vadsrijkTekstSnelheid")
                .defineInRange("vadsrijkTekstSnelheid", 2, 0, 3);
        STEMMEN = b.comment("Het Vadsrijk talking screen: volume of the little voices (blips per word), in percent (0 = off).")
                .translation("guhs.configuration.vadsrijkStemmen")
                .defineInRange("vadsrijkStemmen", 60, 0, 100);
        SPEC = b.build();
    }

    private DialoogConfig() {
    }

    public static int snelheid() {
        return SPEC.isLoaded() ? SNELHEID.get() : 2;
    }

    /** 0..1 */
    public static float stemmen() {
        return SPEC.isLoaded() ? STEMMEN.get() / 100f : 0.6f;
    }

    static void zetSnelheid(int v) {
        if (SPEC.isLoaded()) {
            SNELHEID.set(Math.floorMod(v, 4));
            SNELHEID.save();
        }
    }

    static void zetStemmen(int v) {
        if (SPEC.isLoaded()) {
            STEMMEN.set(Math.max(0, Math.min(100, v)));
            STEMMEN.save();
        }
    }
}
