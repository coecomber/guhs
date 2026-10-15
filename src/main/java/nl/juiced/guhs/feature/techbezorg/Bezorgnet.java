package nl.juiced.guhs.feature.techbezorg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The numbers of the Bezorgguhtje (tools/features/tech_bezorg.py reads them for its texts) and the register of the
 * Stepstations and Haltepaaltjes that are loaded right now, per dimension. Nothing here is saved: a block entity signs in
 * when it loads and out when it is removed or its chunk unloads. A Bezorgguhtje only works in loaded chunks, so "loaded" is
 * exactly what a new pole, a new station and the whistle need to know.
 */
public final class Bezorgnet {
    /** How far a Haltepaaltje may stand from its Stepstation (blocks, in a straight line), and how far the whistle carries. */
    public static final int BEREIK = 96;
    /** Haltepaaltjes per Stepstation. */
    public static final int MAX_HALTES = 8;
    /** Stacks in the backpack. */
    public static final int RUGZAK = 9;
    /** Kinds of item in the filter of one Haltepaaltje (none = everything). */
    public static final int FILTER = 9;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> STATIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> HALTES = new ConcurrentHashMap<>();

    static void stationErbij(Level level, BlockPos pos) {
        STATIONS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    static void stationWeg(Level level, BlockPos pos) {
        Set<BlockPos> set = STATIONS.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    static void halteErbij(Level level, BlockPos pos) {
        HALTES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    static void halteWeg(Level level, BlockPos pos) {
        Set<BlockPos> set = HALTES.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /** (Server stopped / a dimension unloaded.) */
    static void vergeet(ResourceKey<Level> dimensie) {
        STATIONS.remove(dimensie);
        HALTES.remove(dimensie);
    }

    static void vergeetAlles() {
        STATIONS.clear();
        HALTES.clear();
    }

    /** Is this within reach of that (a straight line of at most {@link #BEREIK} blocks)? */
    public static boolean binnenBereik(BlockPos a, BlockPos b) {
        return a.distSqr(b) <= (double) BEREIK * BEREIK;
    }

    /** The loaded Stepstations within reach of this spot, nearest first. */
    public static List<StepstationBlockEntity> stations(ServerLevel level, BlockPos bij) {
        List<StepstationBlockEntity> uit = new ArrayList<>();
        for (BlockPos pos : STATIONS.getOrDefault(level.dimension(), Set.of())) {
            if (binnenBereik(pos, bij) && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StepstationBlockEntity station) {
                uit.add(station);
            }
        }
        uit.sort(Comparator.comparingDouble((StepstationBlockEntity s) -> s.getBlockPos().distSqr(bij))
                .thenComparing(s -> s.getBlockPos().asLong()));
        return uit;
    }

    /** The loaded Haltepaaltjes within reach of this spot, nearest first. */
    public static List<HaltepaaltjeBlockEntity> haltes(ServerLevel level, BlockPos bij) {
        List<HaltepaaltjeBlockEntity> uit = new ArrayList<>();
        for (BlockPos pos : HALTES.getOrDefault(level.dimension(), Set.of())) {
            if (binnenBereik(pos, bij) && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity halte) {
                uit.add(halte);
            }
        }
        uit.sort(Comparator.comparingDouble((HaltepaaltjeBlockEntity h) -> h.getBlockPos().distSqr(bij))
                .thenComparing(h -> h.getBlockPos().asLong()));
        return uit;
    }

    private Bezorgnet() {
    }
}
