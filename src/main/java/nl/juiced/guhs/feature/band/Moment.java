package nl.juiced.guhs.feature.band;

import java.util.Locale;

/**
 * Things that happen to a band guh: the event bus between the 2.10 features ({@link Band#moment}, {@link Band#opMoment}).
 * The value ("waarde") is the string in the comment. Append-only.
 * <ul>
 *   <li>fundament: GETEMD, GEGETEN, AANGEAAID, GEKNUFFELD, PLEK, KNUFFEL, LIEDJE, EMOTE, KLEDING, RIT, REIS, DIMENSIE,
 *       MINIGAME_START, MINIGAME_EINDE, RECORD, HUISJE_IN, SLAAP, WAKKER;</li>
 *   <li>klusjes: KLUSJE; speelgoed: SPEELTJE, GUHKAMER; samen: VRIENDJE.</li>
 * </ul>
 */
public enum Moment {
    /** Tamed (or born tamed). "" */
    GETEMD,
    /** Fed a snack. "&lt;item id&gt;" */
    GEGETEN,
    /** Petted (a tap). "" */
    AANGEAAID,
    /** The big "Knuffelen!" from the menu. "" */
    GEKNUFFELD,
    /** Entered a biome (checked every 100 ticks). "&lt;biome id&gt;" */
    PLEK,
    /** Hugged a plush. "&lt;knuffel id&gt;" */
    KNUFFEL,
    /** Heard a song. "koortje:&lt;id&gt;" or "disco:&lt;id&gt;" */
    LIEDJE,
    /** Did an emote. "&lt;emote id&gt;" */
    EMOTE,
    /** Put on a piece of clothing. "&lt;clothes id&gt;" */
    KLEDING,
    /** Its owner started riding it. "" */
    RIT,
    /** Every 64 blocks travelled together. "64" */
    REIS,
    /** Arrived in a dimension. "&lt;dim id&gt;" */
    DIMENSIE,
    /** Its owner (nearby) started a minigame. "&lt;Minigames id&gt;" */
    MINIGAME_START,
    /** Its owner (nearby) finished or left a minigame. "&lt;Minigames id&gt;" */
    MINIGAME_EINDE,
    /** Its owner (nearby) set a new personal best. "&lt;board&gt;" */
    RECORD,
    /** Finished a chore (klusjes). "&lt;klus id&gt;" */
    KLUSJE,
    /** Played with a toy (speelgoed). "&lt;speeltje id&gt;" */
    SPEELTJE,
    /** Became friends / played with another guh (samen). "&lt;band id of the other guh&gt;" */
    VRIENDJE,
    /** Moved into a Guhhuisje. "&lt;huisje naam&gt;" */
    HUISJE_IN,
    /** Went to sleep in its huisje. "" */
    SLAAP,
    /** Came out of its huisje in the morning. "" */
    WAKKER,
    /** Moved into the Guhkamer (speelgoed). "" */
    GUHKAMER,
    // --- 3.0 (fundament: Wolkjes) ---
    /** It died and went to the wolkjes. "" */
    DOOD,
    /** It came back from the wolkjes (the Knuffelhart). "" */
    TERUG;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
