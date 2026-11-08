package nl.juiced.guhs.feature.vadsrijk.party.nep;

import javax.annotation.Nullable;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * W-PARTY server config ({@code serverconfig/guhs-vadsrijk-nepspelers.toml} in the world): the nep-spelers on or off for the whole
 * server. {@code /guhs vadsrijk nepspeler aan|uit} sets it (and saves it). Off = no wild nep-spelers, no recruiting, no queue
 * filling; the ones that are out leave at once.
 */
public final class NepConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AAN;
    /** Gametests / a server without the config loaded: overrides the config when not null. */
    @Nullable
    public static Boolean TEST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        AAN = b.comment("Het Vadsrijk: nep-spelers (fake players met gamer-namen die in de mobzones rondlopen, mee kunnen in je party",
                        "en lege plekken in een dungeon-wachtrij vullen). false = helemaal uit voor iedereen.",
                        "Spelers kunnen ze ook zelf uitzetten: Vadsrijk-menu > Instellingen > Nep-spelers.")
                .define("nepspelers", true);
        SPEC = b.build();
    }

    private NepConfig() {
    }

    public static boolean aan() {
        if (TEST != null) {
            return TEST;
        }
        try {
            return !SPEC.isLoaded() || AAN.get();
        } catch (RuntimeException e) {
            return true;
        }
    }

    public static void zet(boolean aan) {
        try {
            if (SPEC.isLoaded()) {
                AAN.set(aan);
                AAN.save();
                TEST = null;
                return;
            }
        } catch (RuntimeException ignored) {
            // (fall through)
        }
        TEST = aan;
    }
}
