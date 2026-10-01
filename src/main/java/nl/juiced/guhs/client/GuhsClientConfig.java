package nl.juiced.guhs.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config (config/guhs-client.toml). Only registered on the physical client (GuhsClient.init). */
public final class GuhsClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ADD_OFFICIAL_SERVER;
    /** 1.2.0: the language of the Guhs texts (client/GuhsTaal). */
    public static final ModConfigSpec.EnumValue<nl.juiced.guhs.taal.Taal> LANGUAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ADD_OFFICIAL_SERVER = b
                .comment("Add the official Guhs server (\"Guhs Server\", guhs.nl) to the top of the multiplayer server list.",
                        "This happens only once per installation, the first time the title screen opens; a server you removed is never added again,",
                        "and nothing is added when guhs.nl, play.guhs.nl or 2.28.142.15 is already in the list.",
                        "Modpack makers: set this to false to leave the server list alone.")
                .translation("guhs.configuration.addOfficialServer")
                .define("addOfficialServer", true);
        LANGUAGE = b
                .comment("Language of the Guhs texts (items, menus, quests, signs...): AUTO follows the Minecraft language",
                        "(nl_nl -> Dutch, anything else -> English), NL is always Dutch, EN always English. Only your own client;",
                        "also works on servers. The guh menu has a button for it too.")
                .translation("guhs.configuration.language")
                .defineEnum("language", nl.juiced.guhs.taal.Taal.AUTO);
        SPEC = b.build();
    }

    /** The chosen language; AUTO while the config isn't loaded yet (early during startup). */
    public static nl.juiced.guhs.taal.Taal language() {
        return SPEC.isLoaded() ? LANGUAGE.get() : nl.juiced.guhs.taal.Taal.AUTO;
    }

    private GuhsClientConfig() {
    }
}
