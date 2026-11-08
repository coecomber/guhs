package nl.juiced.guhs.feature.vadsrijk.ritme.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client.SfeerConfig;

/**
 * U-LEVEN-RITME client config ({@code config/guhs-vadsrijk-ritme.toml}): how strong the weather with charm is (a slider in the
 * Vadsrijk menu, tab Instellingen) and the fireflies. Everything also follows the atmosphere level (SfeerConfig: UIT = nothing,
 * ZACHT = less).
 */
public final class RitmeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue WEER;
    public static final ModConfigSpec.BooleanValue VUURVLIEGJES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        WEER = b.comment("Het Vadsrijk: weer met charme (buitjes met regenboog, sneeuw in zone 2, bloesemregen in zone 3, lampiongloed in zone 4,",
                        "wolkenslierten in zone 5, zandwervels in zone 6, ochtendmist). 0 = uit, 100 = vol. Volgt ook de sfeer-stand.")
                .defineInRange("weer", 70, 0, 100);
        VUURVLIEGJES = b.comment("Vuurvliegjes in de steden van het Vadsrijk 's nachts.").define("vuurvliegjes", true);
        SPEC = b.build();
    }

    private RitmeConfig() {
    }

    /** 0..1: the slider times the atmosphere level (VOL 1, ZACHT 0.45, UIT 0). */
    static float sterkte() {
        float w;
        try {
            w = SPEC.isLoaded() ? WEER.get() / 100f : 0.7f;
        } catch (RuntimeException e) {
            w = 0.7f;
        }
        return w * sfeer();
    }

    static float sfeer() {
        try {
            if (!SfeerConfig.SPEC.isLoaded()) {
                return 1f;
            }
            return switch (SfeerConfig.NIVEAU.get()) {
                case VOL -> 1f;
                case ZACHT -> 0.45f;
                case UIT -> 0f;
            };
        } catch (RuntimeException e) {
            return 1f;
        }
    }

    static boolean vuurvliegjes() {
        try {
            return !SPEC.isLoaded() || VUURVLIEGJES.get();
        } catch (RuntimeException e) {
            return true;
        }
    }
}
