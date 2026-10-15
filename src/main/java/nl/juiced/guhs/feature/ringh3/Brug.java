package nl.juiced.guhs.feature.ringh3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h3): the Brug van Knabbel-dum breaks in the bridge scene, and it breaks for the VIEWER only. The real bridge
 * never changes (it is a shared thing, and two players may watch the scene a few seconds apart): the viewer's own game takes
 * the stones of the span away at the exact tick of the scene ({@code client.BrugBreuk}), and from here the server keeps it
 * that way for that player for {@link #KAPOT_TICKS} ticks after the scene ("shared things reset after a minute"), then tells
 * their game to show the real bridge again. While "their" bridge is broken a player who walks up to the gap is gently
 * shoved back ({@link Duwtje}): to the server the stones are still there, and nobody should walk on what they can't see.
 * Nothing is saved: after a logout the bridge is simply whole.
 */
public final class Brug {
    /** How long the bridge stays broken for whoever saw it break. */
    public static final int KAPOT_TICKS = 1200;

    private record Kapot(long tot, ServerLevel level, BlockPos nul, int draai, long gezegd) {
    }

    private static final Map<UUID, Kapot> KAPOT = new ConcurrentHashMap<>();

    private Brug() {
    }

    public static boolean isKapot(ServerPlayer p) {
        Kapot k = KAPOT.get(p.getUUID());
        return k != null && k.level == p.level() && k.tot > p.level().getGameTime();
    }

    /** The bridge of this copy is broken for this player from now on, for {@code ticks} ticks. */
    public static void breek(ServerPlayer p, Mijn m, int ticks) {
        KAPOT.put(p.getUUID(), new Kapot(m.level().getGameTime() + ticks, m.level(), m.nul(), m.draai().ordinal(), 0));
        ModNetworking.sendTo(p, new RingH3Payloads.BrugKapot(m.nul(), m.draai().ordinal(), true));
    }

    /** Whole again for this player (their game puts the stones back with a puff). */
    public static void heel(ServerPlayer p) {
        Kapot k = KAPOT.remove(p.getUUID());
        if (k != null) {
            ModNetworking.sendTo(p, new RingH3Payloads.BrugKapot(k.nul, k.draai, false));
        }
    }

    static void vergeet(UUID speler) {
        KAPOT.remove(speler);
    }

    static void wisAlles() {
        KAPOT.clear();
    }

    /** (a few times a second, a player in the mine) is the time up? Are they too close to a gap only they can see? */
    static void tick(ServerPlayer p, Mijn m, BlockPos lokaal) {
        Kapot k = KAPOT.get(p.getUUID());
        if (k == null) {
            return;
        }
        long nu = p.level().getGameTime();
        if (k.level != p.level() || nu >= k.tot) {
            heel(p);
            if (k.level == p.level()) {
                p.sendSystemMessage(Component.translatable("quest.guhs.ringh3.brug.heel").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        Plekken.Doos d = Plekken.BRUG_KAPOT;
        if (lokaal.getY() >= Plekken.DIEP - 1 && lokaal.getY() <= Plekken.DIEP + 3 && lokaal.getZ() >= d.z0() - 1 && lokaal.getZ() <= d.z1() + 1
                && lokaal.getX() >= d.x0() - 1 && lokaal.getX() <= d.x1() + 1 && Duwtje.mag(p)) {
            boolean west = lokaal.getX() < (d.x0() + d.x1()) / 2;
            Vec3 veilig = m.midden(west ? Plekken.BRUG_RAND_WEST.west(2) : Plekken.BRUG_RAND_OOST.east(2));
            Duwtje.duw(p, veilig.subtract(p.position()), 0.7);
            if (nu - k.gezegd > 60) {
                KAPOT.put(p.getUUID(), new Kapot(k.tot, k.level, k.nul, k.draai, nu));
                GuhQuests.hint(p, "quest.guhs.ringh3.brug.weg");
            }
        }
    }
}
