package nl.juiced.guhs.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config (config/guhs-client.toml). Only registered on the physical client (GuhsClient.init). */
public final class GuhsClientConfig {
    public static final ModConfigSpec SPEC;
    /** 1.2.0: the language of the Guhs texts (client/GuhsTaal). */
    public static final ModConfigSpec.EnumValue<nl.juiced.guhs.taal.Taal> LANGUAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
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
