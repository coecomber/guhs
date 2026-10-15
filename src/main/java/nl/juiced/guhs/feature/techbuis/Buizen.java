package nl.juiced.guhs.feature.techbuis;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * The rules of the Knabbelbuizen in one place: what counts as a tube, what a tube joins, how fast things roll, and the
 * "version" of all tubes of a level (so the pieces know when to look for their routes again).
 * <p>
 * The model (cheap on the server: a plain tube has no block entity and never ticks, and an item in a tube is no entity):
 * <ul>
 *   <li>a <b>Knabbelbuis</b> joins every tube next to it and every block that holds items (any item capability);</li>
 *   <li>a <b>Richtingstuk</b> and a <b>Filterstuk</b> ({@link BuisStukBlock}) are straight pieces with an arrow: things only
 *       go in at the back and out at the front. With something that holds items behind it, the piece takes items out of
 *       it ("hapt") and sends them into the tubes in front; anywhere else it is a one-way valve (the Filterstuk: a
 *       filter);</li>
 *   <li>a piece that sends an item picks where it goes right away ({@link BuisRoutes}: first the places behind a Filterstuk
 *       that asks for exactly this item, then the nearest place where it fits) and keeps the item as "onderweg" until the
 *       ride is over ({@link BuisStukBlockEntity}); the clients only get one small message per ride and draw the item
 *       rolling along ({@code client.BuisRitten}).</li>
 * </ul>
 */
public final class Buizen {
    /** Ticks between two bites of a piece (a hopper moves an item every 8 ticks too). */
    public static final int TEMPO = 8;
    /** How many items one bite is: a Richtingstuk nibbles, a Filterstuk (on vadskracht) takes a mouthful. */
    public static final int HAP_RICHTING = 1, HAP_FILTER = 4;
    /** How long an item rolls through one block of tube, in ticks. */
    public static final int TIKKEN_PER_BLOK = 3;
    /** How many rides one piece can have going at the same time. */
    public static final int MAX_ONDERWEG = 32;
    /** How many tubes a piece looks through for places to send to, and how long a ride can be. */
    public static final int MAX_BUIZEN = 1024, MAX_PAD = 192;
    /** How many slots of what is behind it a piece looks at per bite (a Bank Guh can have very many). */
    public static final int MAX_ZOEK = 54;
    /** A piece that found nothing to send rests this long before it tries again. */
    public static final int RUST = 20;
    /** Routes are looked up again at least this often, whatever the version says (nothing stays wrong for long). */
    public static final int ROUTE_GELDIG = 100;
    /** Who sees an item roll: players within this many blocks of the piece that sent it. */
    public static final double ZICHT = 72;

    private static final Map<Level, int[]> VERSIES = new WeakHashMap<>();

    /** Goes up whenever a tube or a piece appears, disappears or turns, or something next to a tube changes. */
    public static int versie(Level level) {
        synchronized (VERSIES) {
            int[] v = VERSIES.get(level);
            return v == null ? 0 : v[0];
        }
    }

    public static void veranderd(Level level) {
        if (level == null || level.isClientSide()) {
            return;
        }
        synchronized (VERSIES) {
            VERSIES.computeIfAbsent(level, l -> new int[1])[0]++;
        }
    }

    /** A plain Knabbelbuis. */
    public static boolean isBuis(BlockState state) {
        return state.getBlock() instanceof BuisBlock;
    }

    /** A Richtingstuk or a Filterstuk. */
    public static boolean isStuk(BlockState state) {
        return state.getBlock() instanceof BuisStukBlock;
    }

    /**
     * Does a plain tube at {@code pos} have an arm towards {@code kant}: is there a tube, the front or back of a piece, or
     * something that holds items?
     */
    public static boolean verbindt(Level level, BlockPos pos, Direction kant) {
        BlockPos buur = pos.relative(kant);
        if (!level.isLoaded(buur)) {
            return false;
        }
        BlockState state = level.getBlockState(buur);
        if (isBuis(state)) {
            return true;
        }
        if (isStuk(state)) {
            return state.getValue(BuisStukBlock.FACING).getAxis() == kant.getAxis();
        }
        return !state.isAir() && level.getCapability(Capabilities.Item.BLOCK, buur, kant.getOpposite()) != null;
    }

    /**
     * Does a redstone signal reach this block? What Guhdraad and a Guhrad give does not count: they give a redstone signal
     * while they run (so old builds keep working), and a Filterstuk or a sensor often sits right next to the Guhdraad or
     * Guhrad that feeds it.
     */
    public static boolean redstone(Level level, BlockPos pos) {
        for (Direction kant : Direction.values()) {
            if (redstone(level, pos, kant)) {
                return true;
            }
        }
        return false;
    }

    /** Does a redstone signal reach this block from this side (see {@link #redstone(Level, BlockPos)})? */
    public static boolean redstone(Level level, BlockPos pos, Direction kant) {
        BlockPos buur = pos.relative(kant);
        BlockState state = level.getBlockState(buur);
        if (isVadskracht(state)) {
            return false;
        }
        if (state.getSignal(level, buur, kant) > 0) {
            return true;
        }
        if (state.isRedstoneConductor(level, buur)) {
            // a solid block passes on what powers it directly (a repeater into it, a lever on it): again not Guhdraad
            for (Direction verder : Direction.values()) {
                BlockPos bron = buur.relative(verder);
                BlockState bronState = level.getBlockState(bron);
                if (!bron.equals(pos) && !isVadskracht(bronState) && bronState.getDirectSignal(level, bron, verder) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Guhdraad or (a block of) a Guhrad: their redstone signal means "vadskracht", not "stop". */
    private static boolean isVadskracht(BlockState state) {
        return state.is(ModBlocks.GUH_WIRE.get()) || state.is(ModBlocks.GUH_WHEEL.get()) || state.is(ModBlocks.GUH_WHEEL_PART.get());
    }

    private Buizen() {
    }
}
