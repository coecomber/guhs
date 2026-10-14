package nl.juiced.guhs.feature.park;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * 1.4.2 (Het Knabbelpark): the walk check of a REAL, generated park (the blocks in the world): starting at the Reisguh in
 * the middle, every spot a player can reach on foot (a floor under, two free blocks, at most 1 up or 3 down per step;
 * water can be waded through) inside the park's box. A park is right when from there the player reaches the
 * parkwachter, the kooi-verzorger, the three plaques and the hertenkooi's fence (to feed over it), and every piece.
 * It also counts the floor blocks of the rigid pieces that hang over air ("zwevend"). Used by {@code /guhs park keur}
 * and the game tests.
 */
public final class ParkKeur {
    /** What the check found. */
    public record Uitslag(boolean compleet, int stukken, List<String> nietBereikt, int zwevend, int stappen) {
        public boolean goed() {
            return compleet && nietBereikt.isEmpty() && zwevend == 0;
        }

        @Override
        public String toString() {
            return (goed() ? "GOED" : "NIET GOED") + ": " + stukken + " stukken, " + (compleet ? "compleet" : "NIET COMPLEET")
                    + ", niet bereikt: " + (nietBereikt.isEmpty() ? "-" : String.join(", ", nietBereikt)) + ", zwevend: " + zwevend
                    + ", bekeken plekken: " + stappen;
        }
    }

    private ParkKeur() {
    }

    public static Uitslag keur(ServerLevel level, StructureStart start) {
        ParkStructure park = (ParkStructure) start.getStructure();
        boolean compleet = park.compleet(new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(start.getPieces()));
        BoundingBox doos = start.getBoundingBox().inflatedBy(3);
        BlockPos van = Kopieen.wereld(start, ParkPlekken.PLEIN, ParkPlekken.REISGUH);
        Set<BlockPos> bereikt = van == null ? Set.of() : loop(level, van.offset(1, 0, 0), doos);
        if (van != null && bereikt.isEmpty()) {
            bereikt = loop(level, van, doos);
        }
        Map<String, BlockPos> doelen = new LinkedHashMap<>();
        doelen.put("parkwachter", Kopieen.wereld(start, ParkPlekken.PARKWACHTER, ParkPlekken.WACHTER));
        doelen.put("kooi-verzorger", Kopieen.wereld(start, ParkPlekken.HERTENKOOI, ParkPlekken.VERZORGER));
        List<String> niet = new ArrayList<>();
        for (Map.Entry<String, BlockPos> d : doelen.entrySet()) {
            if (d.getValue() == null || !dichtbij(bereikt, d.getValue(), 3.0)) {
                niet.add(d.getKey());
            }
        }
        // the plaques and the kooi fence: found as blocks in their piece
        int plaquettes = 0, hek = 0;
        for (StructurePiece piece : start.getPieces()) {
            Identifier id = PleinSlot.template(piece);
            if (id == null) {
                continue;
            }
            BoundingBox b = piece.getBoundingBox();
            if (id.getPath().equals(ParkPlekken.BEELDENTUIN)) {
                for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                    if (level.getBlockState(p).is(ParkFeature.PLAQUETTE.get())) {
                        plaquettes++;
                        if (!dichtbij(bereikt, p, 2.5)) {
                            niet.add("plaquette bij " + p.toShortString());
                        }
                    }
                }
            } else if (id.getPath().equals(ParkPlekken.HERTENKOOI)) {
                boolean ergens = false;
                for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                    BlockState s = level.getBlockState(p);
                    if (s.is(net.minecraft.world.level.block.Blocks.CHERRY_FENCE)) {
                        hek++;
                        ergens |= dichtbij(bereikt, p, 1.8);
                    }
                }
                if (!ergens) {
                    niet.add("hek van de hertenkooi");
                }
            }
        }
        if (plaquettes != Park.PLAQUETTES) {
            niet.add("plaquettes: " + plaquettes);
        }
        if (hek == 0) {
            niet.add("geen hek");
        }
        // every piece: a reached spot inside it
        int zwevend = 0;
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox b = piece.getBoundingBox();
            boolean in = false;
            for (BlockPos p : bereikt) {
                if (p.getX() >= b.minX() && p.getX() <= b.maxX() && p.getZ() >= b.minZ() && p.getZ() <= b.maxZ()) {
                    in = true;
                    break;
                }
            }
            Identifier id = PleinSlot.template(piece);
            if (!in) {
                niet.add("stuk " + (id == null ? "?" : id.getPath()) + " " + b.getCenter().toShortString());
            }
            if (piece instanceof PoolElementStructurePiece pool && pool.getElement().getProjection() == StructureTemplatePool.Projection.RIGID) {
                zwevend += zwevend(level, b);
            }
        }
        return new Uitslag(compleet, start.getPieces().size(), niet, zwevend, bereikt.size());
    }

    /** The foundation layer (the box's bottom) of a rigid piece: blocks with air (or water) under them. */
    private static int zwevend(ServerLevel level, BoundingBox b) {
        int n = 0;
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                BlockPos p = new BlockPos(x, b.minY(), z);
                if (!level.getBlockState(p).isAir() && level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean dichtbij(Set<BlockPos> bereikt, BlockPos doel, double r) {
        for (BlockPos p : bereikt) {
            double dx = p.getX() - doel.getX(), dz = p.getZ() - doel.getZ(), dy = p.getY() - doel.getY();
            if (dx * dx + dz * dz <= r * r && Math.abs(dy) <= 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean vrij(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    /** Can a player stand with the feet in this block? */
    private static boolean staan(ServerLevel level, BlockPos p) {
        BlockPos onder = p.below();
        BlockState s = level.getBlockState(onder);
        boolean vloer = !s.getCollisionShape(level, onder).isEmpty() && s.getCollisionShape(level, onder).max(Direction.Axis.Y) <= 1.0
                || !level.getFluidState(onder).isEmpty();
        return vloer && vrij(level, p) && vrij(level, p.above());
    }

    /** Every spot reached on foot from {@code van} inside {@code doos}. */
    static Set<BlockPos> loop(ServerLevel level, BlockPos van, BoundingBox doos) {
        Set<BlockPos> gezien = new HashSet<>();
        BlockPos begin = null;
        for (int dy = 0; dy <= 3 && begin == null; dy++) {
            for (int s : new int[]{dy, -dy}) {
                if (staan(level, van.above(s))) {
                    begin = van.above(s);
                    break;
                }
            }
        }
        if (begin == null) {
            return gezien;
        }
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(begin);
        gezien.add(begin);
        while (!rij.isEmpty() && gezien.size() < 200_000) {
            BlockPos p = rij.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!doos.isInside(n)) {
                    continue;
                }
                for (int dy : new int[]{0, 1, -1, -2, -3}) {
                    BlockPos q = n.above(dy);
                    if (gezien.contains(q)) {
                        break;
                    }
                    if (dy == 1 && !vrij(level, p.above(2))) {
                        continue;   // (no room to jump)
                    }
                    if (dy < 0) {
                        boolean open = true;
                        for (int k = 0; k > dy; k--) {
                            open &= vrij(level, n.above(k)) && vrij(level, n.above(k + 1));
                        }
                        if (!open) {
                            break;
                        }
                    }
                    if (staan(level, q)) {
                        gezien.add(q);
                        rij.add(q);
                        break;
                    }
                }
            }
        }
        return gezien;
    }
}
