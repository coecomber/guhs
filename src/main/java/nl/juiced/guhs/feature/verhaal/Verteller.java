package nl.juiced.guhs.feature.verhaal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.3): the narrator card at the start of a chapter: a full-screen card with a
 * drawn map, a title and lines that appear one by one, "Verder" after the last. The same lock as a cutscene
 * ({@link Vast}); {@code daarna} runs on the server when the player clicked "Verder". Seen cards can be read again from the
 * Guhdex. Register from common code, show from the server:
 * <pre>
 * Verteller.registreer("ring_h2", 5, "ring_h2");
 * Verteller.toon(p, "ring_h2", speler -&gt; Cutscenes.speel(speler, AANKOMST, anker, draai, s -&gt; LIJN.verder(s, 0)));
 * </pre>
 * Picture and texts: tools/features/verhaal_motor.py {@code vertelkaart(h, id, titel, regels, kaart)}
 * ({@code textures/gui/verhaal/kaart_<id>.png}, {@code gui.guhs.verhaal.kaart.<id>.titel / .regel.<i>}).
 */
public final class Verteller {
    /** A card: how many lines, and the questline whose Guhdex page gets its "read again" button (null: none). */
    public record Kaart(String id, int regels, @Nullable String lijn) {
        public String titelKey() {
            return "gui.guhs.verhaal.kaart." + id + ".titel";
        }

        public String regelKey(int i) {
            return "gui.guhs.verhaal.kaart." + id + ".regel." + i;
        }
    }

    /** Ticks a line takes to appear on the client; the server does not accept "Verder" much sooner than the lines took. */
    public static final int TICKS_PER_REGEL = 25;

    private static final Map<String, Kaart> ALLE = new LinkedHashMap<>();

    public static void registreer(String id, int regels) {
        registreer(id, regels, null);
    }

    /** The same, attached to a questline (its Guhdex page and travel map show "read again" once seen). */
    public static void registreer(String id, int regels, @Nullable String lijn) {
        synchronized (ALLE) {
            ALLE.put(id, new Kaart(id, Math.max(1, regels), lijn));
        }
    }

    @Nullable
    public static Kaart van(@Nullable String id) {
        synchronized (ALLE) {
            return id == null ? null : ALLE.get(id);
        }
    }

    public static List<String> ids() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.keySet());
        }
    }

    private static String key(String id) {
        return "guhs_kaart_" + id;
    }

    /**
     * Shows the card; {@code daarna} runs when the player read it to the end. False: unknown card, or the player is
     * already watching something (nothing happens, {@code daarna} is not called).
     */
    public static boolean toon(ServerPlayer p, String id, @Nullable Consumer<ServerPlayer> daarna) {
        return start(p, id, false, daarna);
    }

    private static boolean start(ServerPlayer p, String id, boolean herhaling, @Nullable Consumer<ServerPlayer> daarna) {
        Kaart k = van(id);
        if (k == null) {
            return false;
        }
        int min = herhaling ? 0 : (k.regels() - 1) * TICKS_PER_REGEL / 2, max = k.regels() * TICKS_PER_REGEL + 20 * 300;
        if (!Vast.zet(p, new Vast.Slot(p, VerhaalPayloads.KAART, id, min, max, Vast.ontvangt(p), speler -> {
            if (!herhaling) {
                GuhQuests.saved(speler).putBoolean(key(id), true);
                VerhaalSync.sync(speler);
                if (daarna != null) {
                    daarna.accept(speler);
                }
            }
        }))) {
            return false;
        }
        ModNetworking.sendTo(p, new VerhaalPayloads.Kaart(VerhaalPayloads.KAART, id));
        return true;
    }

    public static boolean gezien(ServerPlayer p, String id) {
        return GuhQuests.saved(p).getBooleanOr(key(id), false);
    }

    /** The Guhdex button: read a seen card again (never runs a daarna). */
    public static void herbekijk(ServerPlayer p, String id) {
        if (gezien(p, id)) {
            start(p, id, true, null);
        }
    }

    /** (dev command, tests) forgets that this player read the card. */
    public static void vergeet(ServerPlayer p, String id) {
        GuhQuests.saved(p).remove(key(id));
        VerhaalSync.sync(p);
    }

    private Verteller() {
    }
}
