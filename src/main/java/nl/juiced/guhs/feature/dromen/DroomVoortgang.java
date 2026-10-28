package nl.juiced.guhs.feature.dromen;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The story API of Snoes' dream (for part F, the story agent, and the island agents): per player, which of the five pieces
 * of Snoes' dream were found, whether the finale was played, and "Snoes geholpen" (that last one also opens the wild
 * Blub-guhs: {@link BlubSnoes}). Saved in {@code GuhQuests.saved(player)["guhs_droom_voortgang"]} (survives death).
 * <p>
 * Listeners ({@link #luister}) hear every change as (player, what): a stukje id, {@code "finale"}, {@code "snoes"} or
 * {@code "reset"}; that is where the story hooks its rewards, without this class knowing the story.
 */
public final class DroomVoortgang {
    static final String KEY = "guhs_droom_voortgang";

    /** The five pieces of Snoes' dream, one per island (ontwerp 1b). */
    public enum Stukje {
        LIEDJE(DroomEilanden.LIEDJES), STER(DroomEilanden.STERREN), HERINNERING(DroomEilanden.HERINNERING), BAD(DroomEilanden.BADJE),
        DROOMZAND(DroomEilanden.MOLENTJE);

        /** The island this piece is found on. */
        public final String eiland;

        Stukje(String eiland) {
            this.eiland = eiland;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Stukje van(String id) {
            for (Stukje s : values()) {
                if (s.id().equals(id)) {
                    return s;
                }
            }
            return null;
        }
    }

    private static final List<BiConsumer<ServerPlayer, String>> LUISTERAARS = new CopyOnWriteArrayList<>();

    private DroomVoortgang() {
    }

    /** Hear every change (player, "liedje".."droomzand" / "finale" / "snoes" / "reset"). */
    public static void luister(BiConsumer<ServerPlayer, String> luisteraar) {
        LUISTERAARS.add(luisteraar);
    }

    private static CompoundTag tag(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY);
        saved.put(KEY, t);
        return t;
    }

    public static boolean heeft(ServerPlayer p, Stukje s) {
        return tag(p).getBooleanOr(s.id(), false);
    }

    /** Gives a piece; false when the player already had it (nothing happens then). */
    public static boolean geef(ServerPlayer p, Stukje s) {
        if (heeft(p, s)) {
            return false;
        }
        tag(p).putBoolean(s.id(), true);
        meld(p, s.id());
        return true;
    }

    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (Stukje s : Stukje.values()) {
            n += heeft(p, s) ? 1 : 0;
        }
        return n;
    }

    /** All five pieces: the finale on the Droomplein may start. */
    public static boolean alleStukjes(ServerPlayer p) {
        return aantal(p) == Stukje.values().length;
    }

    public static boolean finaleKlaar(ServerPlayer p) {
        return tag(p).getBooleanOr("finale", false);
    }

    /** The finale was played (Snoes sleeps in his box on the plein). Also helps Snoes ({@link #snoesGeholpen}). */
    public static void zetFinale(ServerPlayer p) {
        if (!finaleKlaar(p)) {
            tag(p).putBoolean("finale", true);
            meld(p, "finale");
        }
        snoesGeholpen(p);
    }

    /** Snoes is helped: the player may see and tame wild Blub-guhs from now on (the existing Blub flag). */
    public static void snoesGeholpen(ServerPlayer p) {
        if (!BlubSnoes.heeft(p)) {
            BlubSnoes.zet(p, true);
            meld(p, "snoes");
        }
    }

    /** Forgets everything (test command); the Blub flag goes too. */
    public static void reset(ServerPlayer p) {
        GuhQuests.saved(p).remove(KEY);
        BlubSnoes.zet(p, false);
        meld(p, "reset");
    }

    private static void meld(ServerPlayer p, String wat) {
        for (BiConsumer<ServerPlayer, String> l : LUISTERAARS) {
            l.accept(p, wat);
        }
    }
}
