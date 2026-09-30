package nl.juiced.guhs.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config (config/guhs-client.toml). Only registered on the physical client (GuhsClient.init). */
public final class GuhsClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ADD_OFFICIAL_SERVER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ADD_OFFICIAL_SERVER = b
                .comment("Add the official Guhs server (\"Guhs Server\", guhs.nl) to the top of the multiplayer server list.",
                        "This happens only once per installation, the first time the title screen opens; a server you removed is never added again,",
                        "and nothing is added when guhs.nl, play.guhs.nl or 2.28.142.15 is already in the list.",
                        "Modpack makers: set this to false to leave the server list alone.")
                .translation("guhs.configuration.addOfficialServer")
                .define("addOfficialServer", true);
        SPEC = b.build();
    }

    private GuhsClientConfig() {
    }
}
