package nl.juiced.guhs.taal;

import java.util.Locale;

/**
 * 1.2.0: the language of the Guhs texts, picked per player on the client (guh menu button, config "language").
 * AUTO follows the Minecraft language: nl_nl gives Dutch, anything else English. Common code (no client classes),
 * so the gametests can check the mapping; the switch itself is client/GuhsTaal.
 */
public enum Taal {
    AUTO, NL, EN;

    /** Is this choice Dutch when Minecraft's language is {@code mcLanguage} (e.g. "nl_nl", "en_us")? */
    public boolean dutch(String mcLanguage) {
        return switch (this) {
            case NL -> true;
            case EN -> false;
            case AUTO -> mcLanguage != null && mcLanguage.toLowerCase(Locale.ROOT).equals("nl_nl");
        };
    }

    /** The Guhs lang file this choice reads: "nl_nl" or "en_us". */
    public String code(String mcLanguage) {
        return dutch(mcLanguage) ? "nl_nl" : "en_us";
    }

    /**
     * The locale FTB Quests should show (compat/FtbQuestsTaal) when its own choice is {@code original} (its editing locale or
     * the Minecraft language). AUTO changes nothing; NL is nl_nl; EN is en_us only instead of a Dutch language: our chapters
     * only have nl_nl and en_us, and en_us is FTB Quests' fallback, so any other language already shows our English (and
     * keeps the pack's other chapters in the player's own language).
     */
    public String ftbLocale(String original) {
        return switch (this) {
            case AUTO -> original;
            case NL -> "nl_nl";
            case EN -> original.startsWith("nl_") ? "en_us" : original;
        };
    }

    /** The next choice of the menu button: AUTO, NL, EN, AUTO... */
    public Taal next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Lang key of the name of this choice ("Auto", "Nederlands", "English"). */
    public String key() {
        return "gui.guhs.taal." + name().toLowerCase(Locale.ROOT);
    }
}
