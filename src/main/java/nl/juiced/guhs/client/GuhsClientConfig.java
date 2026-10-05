package nl.juiced.guhs.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config (config/guhs-client.toml). Only registered on the physical client (GuhsClient.init). */
public final class GuhsClientConfig {
    public static final ModConfigSpec SPEC;
    /** 1.2.0: the language of the Guhs texts (client/GuhsTaal). */
    public static final ModConfigSpec.EnumValue<nl.juiced.guhs.taal.Taal> LANGUAGE;
    /** 1.2.0: the language question (client/screen/TaalVraagScreen) was answered once on this installation. */
    public static final ModConfigSpec.BooleanValue LANGUAGE_CHOSEN;
    /** bbq2: the objective line of the story you follow, on the screen (feature.verhaal.client.VerhaalHud; the Guhdex has the switch). */
    public static final ModConfigSpec.BooleanValue OBJECTIVE_LINE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        LANGUAGE = b
                .comment("Language of the Guhs texts (items, menus, quests, signs...): AUTO follows the Minecraft language",
                        "(nl_nl -> Dutch, anything else -> English), NL is always Dutch, EN always English. Only your own client;",
                        "also works on servers. The Guhdex (and the Superkompas) have a button for it too.")
                .translation("guhs.configuration.language")
                .defineEnum("language", nl.juiced.guhs.taal.Taal.AUTO);
        LANGUAGE_CHOSEN = b
                .comment("Set by the game: true once the language question (the first time you join a world or server) was answered.",
                        "Set it to false to get the question again.")
                .translation("guhs.configuration.languageChosen")
                .define("languageChosen", false);
        OBJECTIVE_LINE = b
                .comment("Show what to do next in the story you follow as a small line at the top of the screen.",
                        "The Guhdex tab Verhalen has a button for it too.")
                .translation("guhs.configuration.objectiveLine")
                .define("objectiveLine", true);
        SPEC = b.build();
    }

    /** The chosen language; AUTO while the config isn't loaded yet (early during startup). */
    public static nl.juiced.guhs.taal.Taal language() {
        return SPEC.isLoaded() ? LANGUAGE.get() : nl.juiced.guhs.taal.Taal.AUTO;
    }

    /** Was the language question answered (or the language chosen some other way)? True while the config isn't loaded. */
    public static boolean languageChosen() {
        return !SPEC.isLoaded() || LANGUAGE_CHOSEN.get();
    }

    /** bbq2: is the objective line on (true while the config isn't loaded)? */
    public static boolean objectiveLine() {
        return !SPEC.isLoaded() || OBJECTIVE_LINE.get();
    }

    /** bbq2: switches the objective line on or off and saves that. */
    public static void objectiveLine(boolean aan) {
        if (SPEC.isLoaded()) {
            OBJECTIVE_LINE.set(aan);
            SPEC.save();
        }
    }

    private GuhsClientConfig() {
    }
}
