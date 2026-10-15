package nl.juiced.guhs.feature.snuffeldorp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * What the residents of Snuffeldorp say. A conversation has an id ("dokter.verhaal") and one or more pages; its Dutch
 * text is {@code quest.guhs.snuffeldorp.<id>.<page>} (tools/features/snuffel_dorp_tekst.py {@code GESPREKKEN}; how many
 * pages it has travels in dorp.json, see {@link Plekken#paginas}). {@link #toon} opens it in the mod's talking screen
 * with the resident as the speaker; {@link #zeg} says a one-page conversation as a chat line.
 * <p>
 * Nothing waits for an answer: what a conversation sets in motion happens when it OPENS (a player who closes the screen
 * halfway loses nothing; the Guhdex and the objective line repeat what to do).
 */
public final class Gesprek {
    static final String SLEUTEL = "snuffeldorp_praat", OKE = "gui.guhs.snuffeldorp.optie.oke", PREFIX = "quest.guhs.snuffeldorp.";

    private Gesprek() {
    }

    static void init() {
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            // (nothing: a conversation of the village asks no question)
        });
    }

    public static String key(String id, int pagina) {
        return PREFIX + id + "." + pagina;
    }

    /** Opens the conversation in the talking screen. */
    public static void toon(ServerPlayer p, Entity spreker, String id, Object... args) {
        int n = Math.max(1, Plekken.paginas(id));
        List<Praat.Regel> regels = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            regels.add(new Praat.Regel(spreker, "", key(id, i), args));
        }
        Praat.scene(p, SLEUTEL, regels, new Praat.Optie(1, OKE));
    }

    /** Says the (first page of the) conversation as a chat line of the resident. */
    public static void zeg(ServerPlayer p, Entity spreker, String id, Object... args) {
        GuhQuests.say(p, spreker, key(id, 0), args);
    }
}
