package nl.juiced.guhs.feature.park;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * 1.4.2 (Het Knabbelpark): the Java side of the plekjes API (the templates and pools: tools/features/park_bouw.py, the
 * section "PLEKJES API" there and in reports/slice_park.md).
 * <p>
 * A plekje is a rigid piece in the pool {@link #POOL} that hangs on a side connector of a park walk. The park is protected
 * like every quest building ({@link Bescherming}: no breaking, placing or using items on blocks in survival); doors,
 * chests, levers, benches, plaques, entities and the blocks' own right-click keep working. What a plekje needs on top of
 * that:
 * <ul>
 *   <li>blocks in the block tag {@link #VRIJ} ({@code guhs:knabbelpark/vrij}) may be broken, placed and used with an item
 *       by everyone inside a park (the sand of a sandbox, a flower bed to plant in...); put them back with
 *       {@code feature.wereld.Herstel} when the next player needs them whole;</li>
 *   <li>{@link #magVeranderen}: an exception in code (a spot, a condition) for the same three things.</li>
 * </ul>
 * Inhabitants of a plekje (an eendguh, a seller-guh, a sulking Mika...) go through {@code feature.wereld.Bezetting} with
 * structure {@link ParkFeature#STRUCTUUR} and {@code stuk} = the plekje's template name (unique as a part of a name), so
 * also parks that were generated earlier get them and they are never doubled.
 */
public final class ParkPlekjes {
    /** The pool of the plekjes (rigid; every element has one entrance jigsaw named {@link #INGANG}). */
    public static final String POOL = "guhs:knabbelpark/plekjes";
    /** The name of a plekje's entrance jigsaw (on its south edge, at walk level y = G + 1, facing south). */
    public static final String INGANG = "guhs:park_plek";
    /** A walk's side connector: target {@link #INGANG}, pool {@link #POOL}. */
    public static final String ZIJ = "guhs:park_zij";
    /** The walks' own connectors (name = target = this, pool guhs:knabbelpark/paden): a plekje may carry them too. */
    public static final String PAD = "guhs:park_pad";

    /** Blocks that everyone may change inside a park. */
    public static final TagKey<Block> VRIJ = TagKey.create(Registries.BLOCK, Guhs.id("knabbelpark/vrij"));

    private static final List<BiPredicate<ServerPlayer, BlockPos>> UITZONDERINGEN = new CopyOnWriteArrayList<>();

    private ParkPlekjes() {
    }

    /** An exception to the park's protection (from your Feature.register): true = this player may change this block. */
    public static void magVeranderen(BiPredicate<ServerPlayer, BlockPos> mag) {
        UITZONDERINGEN.add(mag);
    }

    /** (ParkFeature) the park's protection and its exceptions. */
    static void bescherm() {
        Bescherming.registreer(ParkFeature.STRUCTUUR, ParkFeature.RAND);
        Bescherming.uitzondering(ParkFeature.STRUCTUUR, (p, pos) -> p.level().getBlockState(pos).is(VRIJ));
        Bescherming.uitzondering(ParkFeature.STRUCTUUR, (p, pos) -> {
            for (BiPredicate<ServerPlayer, BlockPos> u : UITZONDERINGEN) {
                if (u.test(p, pos)) {
                    return true;
                }
            }
            return false;
        });
    }
}
