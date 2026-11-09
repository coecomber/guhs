package nl.juiced.guhs.feature.vadsrijk.party.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** W-PARTY client config (config/guhs-vadsrijk-nepspelers-client.toml): "Nep-spelers: aan/uit" for this player. */
public final class NepClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AAN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        AAN = b.comment("Het Vadsrijk: nep-spelers (fake spelers met gamer-namen). false = je ziet ze niet, ze vullen jouw dungeon-wachtrij",
                        "niet en je kunt ze niet meevragen.")
                .define("nepspelers", true);
        SPEC = b.build();
    }

    private NepClientConfig() {
    }

    public static boolean aan() {
        try {
            return !SPEC.isLoaded() || AAN.get();
        } catch (RuntimeException e) {
            return true;
        }
    }
}
