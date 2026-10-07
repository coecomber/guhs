package nl.juiced.guhs.feature.einde2.bouwschelpenstad;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2, slice bouw_schelpenstad: the one thing the build of the road trip does by itself. Whoever follows the road all the
 * way (past De Ruige Kroeg, through the loopgraaf, down the hill into Schelpenstad) and walks out to the head of the pier
 * gets the advancement "Groetjes uit Schelpenstad". Per player, for as many players as come; no story is needed for it.
 * <p>
 * hook fase B: {@link #wereld} is the place of a marker of the build in the world (the cast, the props, the routes: build
 * contract 6.7); the names of the markers a later phase asks for most are the constants below.
 */
public final class Schelpenstad {
    /** The visible advancement of the end of the road. */
    public static final String ZEE = "guheinde/einde2_bouw_schelpenstad_zee";
    /** Markers of the build (sidecar ids). */
    public static final String PIERKOP = "hasselguh_opstap", PARKEER = "kroeg_parkeer", RAND_WEST = "loopgraaf_rand_west",
            RAND_OOST = "loopgraaf_rand_oost", DROOGTAFEL = "droogtafel", KROON = "kroon";
    private static final int INTERVAL = 40;
    /** How near to the head of the pier counts (blocks, across and up or down). */
    private static final int BIJ = 4, HOOG = 3;

    private Schelpenstad() {
    }

    /** The place in the world of a marker of the one Schelpenstad (null: no such marker, or no copy in this world). */
    @Nullable
    public static BlockPos wereld(ServerLevel guheinde, String plek) {
        return Plekken.wereld(guheinde, Einde2Ids.SCHELPENSTAD, plek);
    }

    /** Is somebody who stands at {@code pos} out on the head of the pier at {@code kop}? */
    public static boolean opDePier(BlockPos kop, BlockPos pos) {
        return Math.abs(pos.getX() - kop.getX()) <= BIJ && Math.abs(pos.getZ() - kop.getZ()) <= BIJ && Math.abs(pos.getY() - kop.getY()) <= HOOG;
    }

    /** This player reached the end of the road. */
    public static void bereikt(ServerPlayer player) {
        GidsFeature.grant(player, ZEE);
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % INTERVAL != 0 || player.isSpectator()
                || player.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        BlockPos kop = wereld(player.level(), PIERKOP);
        if (kop != null && opDePier(kop, player.blockPosition())) {
            bereikt(player);
        }
    }
}
