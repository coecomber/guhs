package nl.juiced.guhs.feature.guhpixel.among;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import nl.juiced.guhs.feature.guhpixel.among.model.Balans;

/**
 * The task interface between the engine and the task panels. The engine only knows: a participant has to do a step of a
 * task of some KIND at a panel. What the player sees and does at that panel is the kind's business:
 * <ul>
 *   <li>server side: a {@link TaakSoort} here says how long the step takes at least and whether the result the client sent
 *   is good ({@link TaakSoort#geldig});</li>
 *   <li>client side: {@code among.client.TaakSchermen} maps the same id to the screen that opens.</li>
 * </ul>
 * Every kind of the ship table (worstjes, pasje, kruimelbak, pindasaus, sorteren, dromen, wegen, schakelaars) and the two
 * repair jobs ({@link #HERSTEL_LICHT}, {@link #HERSTEL_ALARM}) is a real mini-game ({@link Taken}); a kind nobody
 * registered falls back to the {@link Wachtpaneel}: a panel with a bar that fills while you stay.
 */
public final class TaakSoorten {
    public static final String HERSTEL_LICHT = "herstel_licht", HERSTEL_ALARM = "herstel_alarm";

    public interface TaakSoort {
        String id();

        /** How many ticks the panel shows its work for (the client fills its bar in this time). */
        int duur(Balans balans);

        /** The server accepts "done" only this many ticks after the panel opened. */
        default int minTicks(Balans balans) {
            return duur(balans) * 4 / 5;
        }

        /** What this panel asks this time (sent to the client with the panel); stap: 0 for the first panel of a task. */
        default CompoundTag opgave(Random rng, int stap) {
            return new CompoundTag();
        }

        /** Is the result the client sent the answer to that opgave? (Never trust a score: check what can be checked.) */
        default boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
            return true;
        }
    }

    /** The placeholder: wait at the panel until the bar is full. */
    public record Wachtpaneel(String id, boolean herstel) implements TaakSoort {
        @Override
        public int duur(Balans balans) {
            return herstel ? balans.herstelTijd : balans.spelerTaakTijd;
        }
    }

    private static final Map<String, TaakSoort> SOORTEN = new LinkedHashMap<>();

    static {
        Taken.registreer();
    }

    public static void registreer(TaakSoort soort) {
        SOORTEN.put(soort.id(), soort);
    }

    /** The kind with this id; an unknown one is a placeholder panel. */
    public static TaakSoort van(String id) {
        return SOORTEN.computeIfAbsent(id, k -> new Wachtpaneel(k, false));
    }

    private TaakSoorten() {
    }
}
