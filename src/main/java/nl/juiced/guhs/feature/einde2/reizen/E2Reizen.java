package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2 phase B (CONTRACT_E2_B 3), owner: slice reizen. The story flights on Appa and the guest rule of co-op.
 * Skelet stand-in: a flight is a teleport to the {@code aankomst} of the structure and {@code daarna} runs on the next tick;
 * nobody is anybody's guest. The slice keeps the names and signatures and fills in the bodies.
 */
public final class E2Reizen {
    /** The kind of flight: the start (from the kaasbol), the three between the chapters, a free one (the whistle). */
    public enum Vlucht { START, H1_H2, H2_H3, H3_H4, VRIJ }

    private E2Reizen() {
    }

    /**
     * Seats p and their guests on p's Appa and flies to {@code Plekken.aankomst(structuur)} (team chat, Zuka's chase on
     * H1_H2 / H2_H3); {@code daarna} once on landing, also when the player steered there. False = could not start, nothing
     * happened.
     */
    public static boolean vlieg(ServerPlayer p, String structuur, Vlucht soort, @Nullable Consumer<ServerPlayer> daarna) {
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        Vec3 naar = guheinde == null ? null : Plekken.aankomst(guheinde, structuur);
        if (naar == null) {
            return false;
        }
        p.teleportTo(guheinde, naar.x, naar.y + 0.1, naar.z, Set.of(), p.getYRot(), 0f, true);
        p.resetFallDistance();
        if (daarna != null) {
            p.level().getServer().execute(() -> daarna.accept(p));
        }
        return true;
    }

    /** The host this player travels with as a guest (null: none). */
    @Nullable
    public static ServerPlayer gastheer(ServerPlayer p) {
        return null;
    }

    /** The guests of this host. */
    public static List<ServerPlayer> gasten(ServerPlayer host) {
        return List.of();
    }
}
