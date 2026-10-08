package nl.juiced.guhs.feature.snuffeldorp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Reis;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * (1.4.1) A dog does not swim in the SEA. Swimming around the ridge's rocks brought a dog onto the ridge and into the
 * pocket behind the roadblock, where nothing can be climbed; and whoever falls off the jetty cannot get out of the
 * harbour basin. So: a dog that is in the sea for about three seconds ({@link #TICKS}) is put back where it last stood on
 * land, unharmed, with a friendly line.
 * <p>
 * <b>What is the sea</b> is not guessed from the water but read from the island's own map ({@link Landkaart}): a dog is
 * "in the sea" while it is on a column of the kind {@code ~} (or {@code S}, a deck) and does not stand dry on something
 * there. So a swimmer who bobs or hops out of the water for a moment is still in the sea (the count goes on), and a dog
 * on the jetty, the boat or a rock is not. The pond, the well, the moestuin's ditch and the wet edge of the beach (the
 * sand ledge and the strip where the sea is one block deep) are columns of the kind {@code e}: a dog may splash there as
 * long as it likes.
 * <p>
 * <b>Where it is put back</b>: the last spot it stood on the ground, out of the water, on walkable land of the start
 * zone or on a deck ({@link Landkaart#veilig}), remembered every tick. Without such a spot (a login in the water, a
 * server that started again): the last spot the kern remembers when that is such land, else the beach.
 * <p>
 * Left alone: a builder in creative mode, a spectator, and whoever watches a scene (the arrival on the beach, the
 * captain's boat). The kern's soft border far outside the island ({@code Hondvorm.bewaak}) stays what it was.
 */
public final class Zee {
    /** How long a dog may be in the sea (ticks): about three seconds. */
    public static final int TICKS = 60;
    /** State that was not looked at for this long (the player was away from the island) is forgotten. */
    private static final int OUD = 40;

    private static final class Staat {
        @Nullable
        Vec3 veilig;
        float yaw;
        int nat;
        long gezien;
    }

    private static final Map<UUID, Staat> STAAT = new ConcurrentHashMap<>();

    private Zee() {
    }

    /** Every tick for a dog on the island (from {@link Dorp#tick}). */
    static void tick(ServerPlayer p, Eiland.Plaats plaats, Plekken pl) {
        Landkaart kaart = Landkaart.van(plaats);
        if (kaart == null) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        Staat s = STAAT.computeIfAbsent(p.getUUID(), u -> new Staat());
        if (nu - s.gezien > OUD) {
            s.veilig = null;      // (was away: where it stood then says nothing about now)
            s.nat = 0;
        }
        s.gezien = nu;
        if (p.isCreative() || p.isSpectator() || Cutscenes.bezig(p) || p.isPassenger()) {
            s.nat = 0;
            return;
        }
        Vec3 pos = p.position();
        boolean droog = p.onGround() && !inWater(p);     // stands on something, out of the water
        if (kaart.zee(plaats, pos) && !droog) {
            if (++s.nat >= TICKS && Duwtje.mag(p)) {
                s.nat = 0;
                Vec3 terug = s.veilig != null && goed(plaats, pl, kaart, s.veilig) ? s.veilig : zonderPlek(p, plaats, pl, kaart);
                Duwtje.terug(p, p.level().dimension(), terug, s.veilig == terug ? s.yaw : p.getYRot());
                p.sendOverlayMessage(Component.translatable("gui.guhs.snuffeldorp.zee").withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        s.nat = 0;
        if (droog && goed(plaats, pl, kaart, pos)) {
            s.veilig = pos;
            s.yaw = p.getYRot();
        }
    }

    /** Is this dog in water? (Asked of the block at its feet too: the game only knows it after a tick of its own.) */
    private static boolean inWater(ServerPlayer p) {
        if (p.isInWater()) {
            return true;
        }
        BlockPos voet = BlockPos.containing(p.getX(), p.getY() + 0.1, p.getZ());
        // (never load a chunk for this: asked every tick, also right after a change of dimension)
        return p.level().isLoaded(voet) && p.level().getFluidState(voet).is(FluidTags.WATER);
    }

    /** A spot to put a dog back on: walkable land (or a deck) of the start zone, in front of the roadblock. */
    private static boolean goed(Eiland.Plaats plaats, Plekken pl, Landkaart kaart, Vec3 plek) {
        return kaart.veilig(plaats, plek) && !kaart.dicht(plaats, plek) && !pl.dicht(plaats, plek);
    }

    /** No spot of its own: the last one the kern remembers (when that is good land with room for a dog), else the beach. */
    private static Vec3 zonderPlek(ServerPlayer p, Eiland.Plaats plaats, Plekken pl, Landkaart kaart) {
        Vec3 rel = Reis.laatste(p);
        if (rel != null) {
            Vec3 w = plaats.wereld(rel);
            if (goed(plaats, pl, kaart, w) && vrij(plaats.level(), w)) {
                return w;
            }
        }
        return plaats.strand();
    }

    private static boolean vrij(ServerLevel level, Vec3 v) {
        double r = Honden.BREEDTE / 2.0;
        BlockPos voet = BlockPos.containing(v.x, v.y + 0.01, v.z);
        return level.isLoaded(voet) && level.getFluidState(voet).isEmpty() && level.noCollision(new AABB(v.x - r, v.y + 0.01, v.z - r, v.x + r, v.y + 0.9, v.z + r));
    }

    /** (Tests) the spot this dog would be put back on now; null: none of its own. */
    @Nullable
    static Vec3 veilig(ServerPlayer p) {
        Staat s = STAAT.get(p.getUUID());
        return s == null ? null : s.veilig;
    }

    static void vergeet(UUID speler) {
        STAAT.remove(speler);
    }

    static void wis() {
        STAAT.clear();
    }
}
