package nl.juiced.guhs.feature.vadsrijk.kern.beleving.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The client options of the Vadsrijk combat feel (config/guhs-vadsrijk-client.toml, also in the mod list: Guhs > Config):
 * one slider "schermschud" 0-100 % (hit-stop, camera kick, screen shake and the camera swoops; 0 = all off).
 */
public final class BelevingConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SCHUD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SCHUD = b.comment("Het Vadsrijk: strength of the screen shake, the hit-stop, the camera kick and the camera swoops of boss moments,",
                        "in percent (0 = all off, 100 = full).")
                .translation("guhs.configuration.vadsrijkSchud")
                .defineInRange("vadsrijkSchud", 100, 0, 100);
        SPEC = b.build();
    }

    private BelevingConfig() {
    }

    /** 0..1 (1 while the config is not loaded yet). */
    public static float sterkte() {
        return SPEC.isLoaded() ? SCHUD.get() / 100f : 1f;
    }
}
