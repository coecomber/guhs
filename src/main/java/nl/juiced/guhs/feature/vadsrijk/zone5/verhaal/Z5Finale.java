package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;

/**
 * THE hook between the zone-5 story (this slice) and the zone-5 bosses (t-z5wezens): the finale of "Verraad in de stad" in
 * De Wolkwaldroom, deep inside Moeder Wolkwal (z5_hq08_wolkwaldroom). Two phases:
 * <ol>
 *   <li>{@link Fase#SPION}: the unmasked spy, <b>Kootje Zilverstaart</b> (a Mika of the Ringvinger; a fight). Done =
 *       {@link #spionVerslagen(ServerPlayer)} (or {@code NpcApi.meld(p, "z5_zilverstaart", "verslagen")}).</li>
 *   <li>{@link Fase#DROOM}: <b>de Grauwe Droom van Moeder Wolkwal</b> (the part-2 deelbaas: a victim, cheered up, never killed).
 *       Done = {@link #droomOpgevrolijkt(ServerPlayer)} (or {@code NpcApi.meld(p, "z5_droom", "opgevrolijkt")}).</li>
 * </ol>
 * <b>How t-z5wezens plugs in</b> (any one of these; the first that answers wins):
 * <ul>
 *   <li>{@code Z5Finale.registreer(Fase.SPION, p -> { ...start the fight for p (and their party)...; return true; })} and the same
 *       for {@code Fase.DROOM}, from its registry class (common setup). Return false when it cannot start now (then our fallback
 *       scene plays);</li>
 *   <li>or an NpcRollen binding {@code "z5_finale"} with arg {@code "spion"} / {@code "droom"};</li>
 *   <li>or a static {@code boolean start(ServerPlayer, String fase)} on {@code zone5.wezens.Z5Bazen} (reflection).</li>
 * </ul>
 * Kills are also seen by entity id: a {@code guhs:vr_z5_*} entity whose id contains "zilverstaart" counts as the spy, one with
 * "droom" as the dream (Z5Haken). Suggested ids: {@code guhs:vr_z5_baas_zilverstaart}, {@code guhs:vr_z5_baas_grauwe_droom}; the
 * arena = {@link Z5VerhaalPlekken#WOLKWALDROOM} (entrance {@link Z5VerhaalPlekken#SPUITGAT}). Without any implementation the
 * story plays its own camera scene for both phases (Z5Verhaal.droom), so the chain never blocks.
 */
public final class Z5Finale {
    public enum Fase {
        SPION, DROOM
    }

    /** Starts one phase of the finale for this player; true = it runs (the implementer reports the end). */
    @FunctionalInterface
    public interface Start {
        boolean start(ServerPlayer p);
    }

    public static final String BAAS_SPION = "guhs:vr_z5_baas_zilverstaart", BAAS_DROOM = "guhs:vr_z5_baas_grauwe_droom";
    private static final Map<Fase, Start> STARTS = new ConcurrentHashMap<>();
    /** When a phase was started for a player (game tick), so the ticker does not restart a running fight every second. */
    private static final Map<UUID, Long> GESTART = new ConcurrentHashMap<>();
    static final int OPNIEUW_NA = 20 * 45;

    private Z5Finale() {
    }

    /** t-z5wezens: register the start of a phase. */
    public static void registreer(Fase fase, Start s) {
        STARTS.put(fase, s);
    }

    /** (tests/dev) */
    static void vergeetAlles() {
        STARTS.clear();
    }

    /** Which phase is next for this player (null = the finale is done or not reached). */
    @Nullable
    public static Fase volgende(ServerPlayer p) {
        if (!Z5Haken.actief(p, Z5Haken.HQ08) || !Z5Haken.doelKlaar(p, Z5Haken.HQ08, 0)) {
            return null;
        }
        if (!Z5Haken.doelKlaar(p, Z5Haken.HQ08, 1)) {
            return Fase.SPION;
        }
        return Z5Haken.doelKlaar(p, Z5Haken.HQ08, 2) ? null : Fase.DROOM;
    }

    /**
     * THE entry point: start the next phase of the finale for this player (called when they reach the blowhole, and again after
     * a phase ends). Order: the registered start, the binding "z5_finale", the reflection hook, else our own scene.
     */
    public static void start(ServerPlayer p) {
        Fase f = volgende(p);
        if (f == null) {
            return;
        }
        GESTART.put(p.getUUID(), p.level().getGameTime());
        String arg = f.name().toLowerCase(java.util.Locale.ROOT);
        Start s = STARTS.get(f);
        if (s != null && s.start(p)) {
            return;
        }
        if (NpcRollen.doe("z5_finale", p, arg) || Z5Verhaal.reflectie(p, arg, "zone5.wezens.Z5Bazen#start", "zone5.bazen.Z5Bazen#start")) {
            return;
        }
        Z5Verhaal.droom(p, f);   // (fallback: a camera scene that ends the phase itself)
    }

    /** Is a phase running for this player (started less than {@link #OPNIEUW_NA} ticks ago)? */
    static boolean bezig(ServerPlayer p) {
        Long t = GESTART.get(p.getUUID());
        return t != null && p.level().getGameTime() - t < OPNIEUW_NA;
    }

    static void vergeet(ServerPlayer p) {
        GESTART.remove(p.getUUID());
    }

    /** The spy is beaten: the next phase (the dream) starts at once. */
    public static void spionVerslagen(ServerPlayer p) {
        NpcApi.meld(p, "z5_zilverstaart", "verslagen");
        GESTART.remove(p.getUUID());
        if (volgende(p) == Fase.DROOM) {
            start(p);
        }
    }

    /** The grey dream is cheered up: the whale sleeps sweetly again; back to the queen. */
    public static void droomOpgevrolijkt(ServerPlayer p) {
        NpcApi.meld(p, "z5_droom", "opgevrolijkt");
        GESTART.remove(p.getUUID());
    }
}
