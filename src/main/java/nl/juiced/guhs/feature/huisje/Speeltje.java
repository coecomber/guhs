package nl.juiced.guhs.feature.huisje;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * A kind of toy (implementations: the speelgoed feature, registered with {@link Speelgoed#registreer}; ids knabbelbal,
 * glijbaantje, tunnel, wip_schommel). Residents of a huisje sometimes play with one near their home at random.
 */
public interface Speeltje {
    String id();

    /** A toy of this kind within bereik of rond for this mob to play with, or null. */
    @Nullable
    KlusTaak zoek(ServerLevel level, Mob speler, BlockPos rond, int bereik);

    /** 1.2.8: one line of the overview in the huisje screen: a thing residents play with, its icon (and name) and how many there are. */
    record Telling(String id, ItemStack icoon, int aantal) {
    }

    /**
     * 1.2.8: how many toys of this kind {@link #zoek} could find within bereik of rond (the same search), counting only
     * the ones {@code binnen} accepts (the huisje's blue area). One line per thing (the wip and the schommel are one kind
     * but two lines); always every line, also when there are none.
     */
    default List<Telling> tel(ServerLevel level, BlockPos rond, int bereik, Predicate<BlockPos> binnen) {
        return List.of();
    }
}
