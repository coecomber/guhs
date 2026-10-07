package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * einde2, slice sponsguh_wezens: the order table of De Krokante Knabbel (block {@code guhs:besteltafel}): a right-click
 * rings its bell.
 * Skeleton stub: the list is real, the block that rings comes with the sponsguh_wezens slice.
 * <p>
 * hook fase B: the story hears an order through {@link #BIJ_BEL}.
 */
public final class Besteltafel {
    /** Called when a player rings the bell of a besteltafel, with the player and the table's spot. */
    public static final List<BiConsumer<ServerPlayer, BlockPos>> BIJ_BEL = new CopyOnWriteArrayList<>();

    private Besteltafel() {
    }
}
