package nl.juiced.guhs.feature.techbuis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Where a Richtingstuk or Filterstuk can send things: every block that holds items and is reached through the tubes in
 * front of the piece, nearest first. A walk through the tubes (breadth first), with the rules of {@link Buizen}: a plain
 * tube goes on to every side, a piece is only entered at its back and left at its front.
 * <p>
 * The walk does not look at the item: a route only remembers which pieces it passes ({@link Route#stukken}); whether a
 * given item gets through them (a Filterstuk's list, a piece locked by redstone, a Filterstuk without vadskracht) is asked
 * when something is sent ({@link BuisStukBlockEntity}). Each tube is walked once, so a place has one route per side it is
 * reached at: the shortest.
 */
public final class BuisRoutes {
    /**
     * One place to send to.
     *
     * @param doel    the block that holds items
     * @param kant    the side of it the tube arrives at
     * @param pad     the blocks an item rolls through, the sending piece first, the tube that touches {@code doel} last
     * @param stukken the pieces (Richtingstuk, Filterstuk) on the way, the sending piece not included
     */
    public record Route(BlockPos doel, Direction kant, List<BlockPos> pad, List<BlockPos> stukken) {
        public int lengte() {
            return pad.size();
        }
    }

    private record Stap(BlockPos pos, @Nullable Stap vorige, int diepte, boolean stuk) {
    }

    /** Every place the piece at {@code stuk} (arrow towards {@code voor}) can send to, nearest first. */
    public static List<Route> zoek(Level level, BlockPos stuk, Direction voor) {
        List<Route> uit = new ArrayList<>();
        Set<BlockPos> gezien = new HashSet<>();
        Set<BlockPos> eigen = bron(level, stuk, voor);
        ArrayDeque<Stap> rij = new ArrayDeque<>();
        gezien.add(stuk);
        rij.add(new Stap(stuk, null, 1, true));
        while (!rij.isEmpty()) {
            Stap stap = rij.poll();
            BlockState hier = level.getBlockState(stap.pos);
            if (stap.stuk) {
                if (Buizen.isStuk(hier)) {
                    verder(level, stap, hier.getValue(BuisStukBlock.FACING), gezien, eigen, rij, uit);
                }
            } else {
                for (Direction kant : Direction.values()) {
                    verder(level, stap, kant, gezien, eigen, rij, uit);
                }
            }
        }
        return uit;
    }

    /** One step from a tube or a piece towards {@code kant}: on into the next tube, or a place that holds items. */
    private static void verder(Level level, Stap van, Direction kant, Set<BlockPos> gezien, Set<BlockPos> eigen, ArrayDeque<Stap> rij,
                               List<Route> uit) {
        BlockPos naar = van.pos.relative(kant);
        if (!level.isLoaded(naar)) {
            return;
        }
        BlockState state = level.getBlockState(naar);
        boolean buis = Buizen.isBuis(state);
        if (buis || Buizen.isStuk(state)) {
            // a piece is only entered at its back: its arrow must point the way we are going
            if (!buis && state.getValue(BuisStukBlock.FACING) != kant) {
                return;
            }
            if (van.diepte < Buizen.MAX_PAD && gezien.size() < Buizen.MAX_BUIZEN && gezien.add(naar)) {
                rij.add(new Stap(naar, van, van.diepte + 1, !buis));
            }
            return;
        }
        if (state.isAir() || eigen.contains(naar) || level.getCapability(Capabilities.Item.BLOCK, naar, kant.getOpposite()) == null) {
            return;
        }
        List<BlockPos> pad = new ArrayList<>(van.diepte);
        List<BlockPos> stukken = new ArrayList<>(2);
        for (Stap s = van; s != null; s = s.vorige) {
            pad.add(0, s.pos);
            if (s.stuk && s.vorige != null) {
                stukken.add(0, s.pos);
            }
        }
        uit.add(new Route(naar, kant.getOpposite(), List.copyOf(pad), List.copyOf(stukken)));
    }

    /**
     * What the piece takes its items out of (the block behind it; both halves of a double chest): never a place to send to,
     * or things would go round in circles.
     */
    private static Set<BlockPos> bron(Level level, BlockPos stuk, Direction voor) {
        BlockPos achter = stuk.relative(voor.getOpposite());
        Set<BlockPos> eigen = new HashSet<>(2);
        eigen.add(achter);
        if (level.isLoaded(achter)) {
            BlockState state = level.getBlockState(achter);
            if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                eigen.add(achter.relative(ChestBlock.getConnectedDirection(state)));
            }
        }
        return eigen;
    }

    private BuisRoutes() {
    }
}
