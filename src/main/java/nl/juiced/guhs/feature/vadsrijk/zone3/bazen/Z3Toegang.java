package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;

/**
 * (M-BAZEN) Who may fight which zone-3 boss right now. Two rules, both by arena id (no coordinates, so they survive the zone-3
 * map revamp):
 * <ul>
 *   <li><b>One fight at a time.</b> A player who got the intro of boss A is "in A's fight" while A lives and the player is in A's
 *       arena (plus {@link #GRATIE} ticks after stepping out: knocked off a deck, a dodge over the edge). Meanwhile no other zone-3
 *       boss greets them (no intro camera), targets them or takes their hits.</li>
 *   <li><b>Dungeon order</b> ({@link #VOLGORDE}): Kaaszeppelinwerf Taleggio -> Brokkel -> Roquefort, Hart van de Boom Wortelwurger ->
 *       Nestwachter -> Baron Bleu. A boss only wakes for a player when the boss before it is beaten: by this player in the last
 *       {@link #RUN_TICKS} (one dungeon run), or it is down right now (respawn timer running: a friend beat it), or the player is in
 *       creative. The dev commands {@code tp <baas>} / {@code spawn <baas>} open the order for the caller ({@link #geefVrij}).</li>
 * </ul>
 */
public final class Z3Toegang {
    public static final int GRATIE = 200, RUN_TICKS = 20 * 60 * 60;
    public static final Map<String, List<String>> VOLGORDE = Map.of(
            "werf", List.of(Z3BazenRegistry.ARENA_TALEGGIO, Z3BazenRegistry.ARENA_BROKKEL, Z3BazenRegistry.ARENA_ROQUEFORT),
            "hart", List.of(Z3BazenRegistry.ARENA_WURGER, Z3BazenRegistry.ARENA_WACHTER, Z3BazenRegistry.ARENA_BLEU));

    private record Actief(UUID baas, long laatst) {
    }

    private static final Map<UUID, Actief> ACTIEF = new ConcurrentHashMap<>();
    /** player -> (arena id -> game time beaten). */
    private static final Map<UUID, Map<String, Long>> VERSLAGEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BERICHT = new ConcurrentHashMap<>();

    private Z3Toegang() {
    }

    // ---- one fight at a time --------------------------------------------------------------------------------------------

    /** The zone-3 boss this player is fighting right now, or null. */
    @Nullable
    public static Z3Baas actief(ServerPlayer p) {
        Actief a = ACTIEF.get(p.getUUID());
        if (a == null || !(p.level() instanceof ServerLevel sl)) {
            return null;
        }
        long nu = sl.getGameTime();
        if (!(sl.getEntity(a.baas()) instanceof Z3Baas b) || b.isRemoved() || b.geplofd() || !p.isAlive()) {
            ACTIEF.remove(p.getUUID());
            return null;
        }
        Arena ar = b.arena();
        if (ar == null || ar.binnen(p.getX(), p.getY(), p.getZ())) {
            if (nu - a.laatst() > 20) {
                ACTIEF.put(p.getUUID(), new Actief(a.baas(), nu));
            }
            return b;
        }
        if (nu - a.laatst() <= GRATIE) {
            return b;
        }
        ACTIEF.remove(p.getUUID());
        return null;
    }

    /** The intro ran: this player is now in this boss's fight. */
    static void start(ServerPlayer p, Z3Baas b) {
        ACTIEF.put(p.getUUID(), new Actief(b.getUUID(), b.level().getGameTime()));
    }

    /** The boss is beaten / gone: nobody is in its fight any more. */
    static void klaar(Z3Baas b) {
        ACTIEF.values().removeIf(a -> a.baas().equals(b.getUUID()));
    }

    // ---- dungeon order ----------------------------------------------------------------------------------------------------

    /** The arena before this one in its dungeon, or null (first boss / not a dungeon boss). */
    @Nullable
    public static String voorganger(String arenaId) {
        for (List<String> l : VOLGORDE.values()) {
            int i = l.indexOf(arenaId);
            if (i > 0) {
                return l.get(i - 1);
            }
        }
        return null;
    }

    static void verslagen(ServerPlayer p, String arenaId, long t) {
        VERSLAGEN.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(arenaId, t);
    }

    /** Dev: every boss before this one counts as beaten for this player (this run). */
    public static void geefVrij(ServerPlayer p, String arenaId) {
        long t = p.level().getGameTime();
        for (String v = voorganger(arenaId); v != null; v = voorganger(v)) {
            verslagen(p, v, t);
        }
    }

    /** Does the boss of this arena wake for this player (its predecessor is beaten)? */
    public static boolean vrijgegeven(ServerPlayer p, String arenaId) {
        String v = voorganger(arenaId);
        if (v == null || p.getAbilities().instabuild) {
            return true;
        }
        Map<String, Long> m = VERSLAGEN.get(p.getUUID());
        Long t = m == null ? null : m.get(v);
        if (t != null && p.level().getGameTime() - t <= RUN_TICKS) {
            return true;
        }
        Arena a = Arenas.van(v);
        return a != null && p.level() instanceof ServerLevel sl && sl.isLoaded(a.centrum()) && Arenas.baas(sl, v) == null;
    }

    // ---- both ---------------------------------------------------------------------------------------------------------------

    /** May this boss fight / greet this player right now? */
    public static boolean mag(ServerPlayer p, Z3Baas b) {
        Z3Baas ander = actief(p);
        if (ander != null && ander != b) {
            return false;
        }
        return ander == b || b.arenaId.isEmpty() || vrijgegeven(p, b.arenaId);
    }

    /** Tell a player who stands in the boss's arena but may not fight it why (an overlay, every 3 s at most). */
    static void waarschuw(ServerPlayer p, Z3Baas b) {
        long nu = p.level().getGameTime();
        Long vorig = BERICHT.get(p.getUUID());
        if (vorig != null && nu - vorig < 60 && nu >= vorig) {
            return;
        }
        BERICHT.put(p.getUUID(), nu);
        Z3Baas ander = actief(p);
        if (ander != null && ander != b) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.ander_gevecht", naam(ander.baasId())).withStyle(ChatFormatting.GOLD));
            return;
        }
        String v = voorganger(b.arenaId);
        if (v != null) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.volgorde", naam(b.baasId()), naam(v.substring(v.indexOf(':') + 1)))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    static Component naam(String baasId) {
        return Component.translatable("entity.guhs.vr_z3_baas_" + baasId);
    }

    /** Gametests / a dev reset: forget everything about this player. */
    public static void vergeet(ServerPlayer p) {
        ACTIEF.remove(p.getUUID());
        VERSLAGEN.remove(p.getUUID());
        BERICHT.remove(p.getUUID());
    }
}
