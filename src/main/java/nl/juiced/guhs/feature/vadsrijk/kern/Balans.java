package nl.juiced.guhs.feature.vadsrijk.kern;

import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.juiced.guhs.Guhs;

/**
 * The Vadsrijk balance knobs (config/guhs-vadsrijk-balans.toml, a COMMON config: the server's copy counts). Balans 2026-11
 * (second playtest): section {@code mobs}, key {@code schade} = a global multiplier on the hits of normal zone mobs and
 * elites (bosses are not touched), default 1.3 (+30 %).
 * <p>Integration note: if another slice adds keys to the same file, they belong in this one spec (one file = one spec).
 */
public final class Balans {
    public static final String BESTAND = "guhs-vadsrijk-balans.toml";
    public static final double MOB_SCHADE_STANDAARD = 1.3;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MOB_SCHADE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Het Vadsrijk: creatures").push("mobs");
        MOB_SCHADE = b.comment("Multiplier on the damage of normal zone creatures and elites (zone 1 and 2). Bosses are not affected.",
                        "1.0 = the old numbers, 1.3 = +30 % (balance round 2026-11).")
                .defineInRange("schade", MOB_SCHADE_STANDAARD, 0.1, 5.0);
        b.pop();
        SPEC = b.build();
    }

    private Balans() {
    }

    /** From KernRegistry.register. */
    static void registreer() {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.COMMON, SPEC, BESTAND));
    }

    /** The mob damage multiplier (the default while the config is not loaded, e.g. early in a gametest). */
    public static double mobSchade() {
        return SPEC.isLoaded() ? MOB_SCHADE.get() : MOB_SCHADE_STANDAARD;
    }
}
