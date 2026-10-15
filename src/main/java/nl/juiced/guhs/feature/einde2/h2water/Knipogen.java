package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;

/**
 * einde2 h2_water: the two winks of chapter 2 (DESIGN 5c): wink 1 goes on (Sjokkel creeps along the palace terrace behind
 * the training square, a little further than in chapter 1) and wink 2 (Baltoguh trudges onto the square during Pakguh's
 * lesson, took a wrong turn again). Each is a scene of 5-10 seconds, seen ONCE per player, whether or not they did the old
 * story; it is anchored at the last point of its camera marker (CONTRACT_E2 6.6). A wink never holds the story up: when it
 * cannot be shown (seen already, a game that cannot show scenes, busy) the story goes on without it.
 */
public final class Knipogen {
    private Knipogen() {
    }

    /** Can this player's game show a cutscene (a real client; not the mock players of the game tests)? */
    static boolean kanZien(ServerPlayer p) {
        return !(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && p.connection != null && p.connection.hasChannel(VerhaalPayloads.Speel.TYPE);
    }

    /** The anchor of a wink: the last point of its camera marker at the copy. */
    @Nullable
    static BlockPos anker(ServerPlayer p, String camera) {
        E2H2Water.Oord o = E2H2Water.oord(p.level().getServer());
        List<BlockPos> punten = o == null ? List.of() : o.punten(camera);
        return punten.isEmpty() ? null : punten.get(punten.size() - 1);
    }

    /** Shows the wink once. False (nothing happens, daarna is not called): seen, cannot be shown, or busy. */
    static boolean toon(ServerPlayer p, Cutscene knipoog, String camera, @Nullable Consumer<ServerPlayer> daarna) {
        BlockPos anker = anker(p, camera);
        if (anker == null || Cutscenes.gezien(p, knipoog.id()) || !kanZien(p)) {
            return false;
        }
        return Cutscenes.speel(p, knipoog, anker, Rotation.NONE, daarna);
    }

    /** A wink right after a story scene (in its daarna): true when it started (then daarna follows it). */
    static boolean na(ServerPlayer p, Cutscene knipoog, String camera, Consumer<ServerPlayer> daarna) {
        return toon(p, knipoog, camera, daarna);
    }
}
