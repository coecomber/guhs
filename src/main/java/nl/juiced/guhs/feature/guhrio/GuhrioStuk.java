package nl.juiced.guhs.feature.guhrio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that is a piece of a Super Guhrio level (a coin, a ?-block, a flag, a pipe, the spot of a creature...). The
 * engine finds every such block in the lanes of a level ({@link GuhrioSpel#zoekStukken}) and gives it its turn. All of it
 * runs on the server and is per player: whatever a piece remembers about a player goes in that player's
 * {@link GuhrioSpel.Sessie#staat} ({@link GuhrioSpel#zetStaat}), the block in the world never changes.
 * <p>
 * A new kind of piece is a new block that implements this; nothing has to be registered anywhere.
 */
public interface GuhrioStuk {
    /** The player is in this block's space (every tick; the player's game also reports it at once for a snappy coin). */
    default void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
    }

    /** The player's head bumped this block from below. */
    default void bots(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
    }

    /** The player ducked (S) standing on this block. */
    default void duik(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
    }

    /** The player pressed W standing in this block's space. */
    default void deur(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
    }

    /** Once a second while somebody plays the level: keep what belongs to this piece alive (a Guhmba on its spot). */
    default void wek(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
    }
}
