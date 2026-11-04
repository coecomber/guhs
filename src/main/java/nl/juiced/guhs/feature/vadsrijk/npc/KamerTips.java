package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenPlekken;

/**
 * Fix round 2 (g-ux): while you stand in a puzzle room (the Zwamtempel's three rooms, the Spiegelburcht's light rooms and
 * gauntlet) its tip stays on screen, a line at the bottom above the action bar (client: {@code VolgHud}). It never replaces the
 * rooms' own action-bar messages. Zone 2 adds its rooms in {@code Z2Haken}. Lang: {@code vadsrijk.npc.kamer.<id>}.
 */
public final class KamerTips {
    /** One room: inside(x, y, z); tekst = the lang key of the tip for this player now (null = no tip). */
    public record Kamer(String id, Binnen binnen, Function<ServerPlayer, String> tekst) {
    }

    @FunctionalInterface
    public interface Binnen {
        boolean in(double x, double y, double z);
    }

    public static final List<Kamer> KAMERS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, String> LAATST = new ConcurrentHashMap<>();

    private KamerTips() {
    }

    static void init() {
        // the Zwamtempel (bazen: BazenPlekken K1..K3 inner boxes)
        voegToe("lichtzwamkamer", doos(BazenPlekken.K1_MIN, BazenPlekken.K1_MAX));
        voegToe("hendelkamer", doos(BazenPlekken.K2_MIN, BazenPlekken.K2_MAX));
        voegToe("ridderzaal", doos(BazenPlekken.K3_MIN, BazenPlekken.K3_MAX));
    }

    /** A room with one fixed tip ({@code vadsrijk.npc.kamer.<id>}). */
    public static void voegToe(String id, Binnen binnen) {
        KAMERS.add(new Kamer(id, binnen, p -> "vadsrijk.npc.kamer." + id));
    }

    /** The inner box between two corners (inclusive, a little height margin like the rooms' own checks). */
    public static Binnen doos(BlockPos a, BlockPos b) {
        return (x, y, z) -> x >= Math.min(a.getX(), b.getX()) && x <= Math.max(a.getX(), b.getX()) + 1 && z >= Math.min(a.getZ(), b.getZ())
                && z <= Math.max(a.getZ(), b.getZ()) + 1 && y >= Math.min(a.getY(), b.getY()) - 1 && y <= Math.max(a.getY(), b.getY()) + 2;
    }

    /** The tip key for where this player stands now (null = in no puzzle room). */
    @Nullable
    public static String tip(ServerPlayer p) {
        for (Kamer k : KAMERS) {
            if (k.binnen().in(p.getX(), p.getY(), p.getZ())) {
                String t = k.tekst().apply(p);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    /** Sends the bottom line when it changes (called twice a second from NpcVolg's tick). */
    static void stuur(ServerPlayer p) {
        String t = p.isSpectator() ? null : tip(p);
        String key = t == null ? "" : t;
        if (!key.equals(LAATST.getOrDefault(p.getUUID(), ""))) {
            LAATST.put(p.getUUID(), key);
            NpcPayloads.stuur(p, new NpcPayloads.KamerTip(key));
        }
    }

    static void vergeet(UUID id) {
        LAATST.remove(id);
    }
}
