package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;

/**
 * What the other slices may call or listen to (server side):
 * <ul>
 *   <li>{@link #meld}: report an event a quest may wait for. Known: {@code meld(p, "klasse", klasseId)} (klassen, after a class
 *       pick), {@code meld(p, "rijdier", "hoedhupper")} (economie, the first mount), {@code meld(p, "advancement", "ridder:1")}
 *       (klassen, after the ceremony), {@code meld(p, "spion", "verslagen")} (bazen, after the rooftop fight).</li>
 *   <li>{@link #zone1Klaar}: the zone-1 story is done (wereld recolours the scars per player); {@link #ZONE1_KLAAR} fires once.</li>
 *   <li>{@link #QUEST_KLAAR}: (player, quest id) after every hand-in.</li>
 * </ul>
 */
public final class NpcApi {
    public static final String ZONE1_VLAG = "zone1_klaar";
    public static final List<Consumer<ServerPlayer>> ZONE1_KLAAR = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, String>> QUEST_KLAAR = new CopyOnWriteArrayList<>();

    private NpcApi() {
    }

    public static void meld(ServerPlayer p, String soort, String waarde) {
        if (soort.equals("klasse")) {
            Questlog.tel(p, Quest.Doel.Type.KLASSE, d -> true, 1);
        }
        String sleutel = soort + ":" + waarde;
        Questlog.tel(p, Quest.Doel.Type.MELD, d -> d.doel().equals(sleutel) || d.doel().equals(soort + ":*"), 1);
    }

    /** Is the zone-1 story finished for this player? ({@code SpelerData.extra("npc").zone1_klaar}) */
    public static boolean zone1Klaar(ServerPlayer p) {
        return Questlog.vlag(p, ZONE1_VLAG);
    }

    static void zetZone1Klaar(ServerPlayer p) {
        if (!zone1Klaar(p)) {
            Questlog.zetVlag(p, ZONE1_VLAG, true);
            ZONE1_KLAAR.forEach(h -> h.accept(p));
        }
    }

    public static boolean questKlaar(ServerPlayer p, String quest) {
        return Questlog.klaar(p, quest);
    }

    public static boolean questActief(ServerPlayer p, String quest) {
        return Questlog.actief(p, quest);
    }
}
