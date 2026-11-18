package nl.juiced.guhs.feature.guhwinter.npc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guhwinter (npc slice): the idle lines of the Winterplein's guhs, lang {@code guhwinter.npc.praat.<naam>.<fase>.<n>}
 * (n = 1, 2, ...; 3 to 6 per guh and phase; tools/features/guhwinter_npc.py). Every click gives the next line of that guh
 * for that player (starting somewhere random), so you hear them all before one comes back.
 */
public final class NpcPraat {
    /** player -> (naam.fase -> the line it said last). */
    private static final Map<UUID, Map<String, Integer>> GEZEGD = new ConcurrentHashMap<>();
    /** naam.fase -> how many lines there are. */
    private static final Map<String, Integer> AANTAL = new ConcurrentHashMap<>();

    private NpcPraat() {
    }

    public static String key(String naam, NpcFase fase, int n) {
        return "guhwinter.npc.praat." + naam + "." + fase.id() + "." + n;
    }

    /** How many idle lines this guh has in this phase (0: none). */
    public static int aantal(String naam, NpcFase fase) {
        return AANTAL.computeIfAbsent(naam + "." + fase.id(), k -> {
            int n = 0;
            while (n < 12 && Language.getInstance().has(key(naam, fase, n + 1))) {
                n++;
            }
            return n;
        });
    }

    /** The next idle line of this guh for this player (key), or null when it has none in this phase. */
    public static String volgende(ServerPlayer speler, String naam, NpcFase fase) {
        int n = aantal(naam, fase);
        if (n == 0) {
            return null;
        }
        Map<String, Integer> m = GEZEGD.computeIfAbsent(speler.getUUID(), u -> new ConcurrentHashMap<>());
        String k = naam + "." + fase.id();
        Integer vorige = m.get(k);
        int nu = vorige == null ? speler.getRandom().nextInt(n) : (vorige + 1) % n;
        m.put(k, nu);
        return key(naam, fase, nu + 1);
    }

    /** Say the next idle line to this player (in chat). */
    public static void praat(ServerPlayer speler, String naam, NpcFase fase) {
        String key = volgende(speler, naam, fase);
        if (key != null) {
            GuhwinterNpcs.say(speler, naam, Component.translatable(key));
        }
    }

    static void vergeet(UUID speler) {
        GEZEGD.remove(speler);
    }
}
