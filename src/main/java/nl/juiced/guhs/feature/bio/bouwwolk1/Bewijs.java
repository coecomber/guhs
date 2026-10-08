package nl.juiced.guhs.feature.bio.bouwwolk1;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The hidden proof advancements of this slice (data/guhs/advancement/quest/&lt;name&gt;.json), for the FTB quests that
 * slice systemen writes later: found each of the three places, finished the wolkenhoeder's lesson, took a schaapje
 * home, the first balloon ride, the first sterrenstof from the ruin's telescope.
 */
public final class Bewijs {
    public static final String HUT_GEVONDEN = "wolkenhoeder_hut_gevonden", HUT_LES = "wolkenhoeder_hut_les", HUT_SCHAAPJE = "wolkenhoeder_hut_schaapje";
    public static final String RUINE_GEVONDEN = "sterrenwacht_ruine_gevonden", RUINE_STERRENSTOF = "sterrenwacht_ruine_sterrenstof";
    public static final String HAVEN_GEVONDEN = "luchtballon_haven_gevonden", HAVEN_VAART = "luchtballon_haven_vaart";

    public static void geef(ServerPlayer speler, String naam) {
        GuhAdvancements.grant(speler, naam);
    }

    private Bewijs() {
    }
}
