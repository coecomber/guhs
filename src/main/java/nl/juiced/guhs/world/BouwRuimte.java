package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Keeps the guhs buildings from growing into each other (all kinds: surface, underground, Barbecuether, Guheinde).
 * <p>
 * Every guhs structure that takes part ({@link Ruimte}, keep_clear &gt; 0) claims the real bounding box of its pieces.
 * Before it starts, it looks at the starts nearby of every structure set that goes first, and gives way (doesn't start)
 * when one of them really starts there (checked the way the game itself does: the set's placement, its weighted pick,
 * its ground and biome checks and, in turn, who that one gives way to) with pieces within {@link #MARGIN} blocks of its
 * own. Order: the set with the higher voorrang first (default: its keep_clear), then by name; within one set the start
 * in the lower chunk (x, then z) goes first. The order is strict, so the checks always end. The structures of others
 * (vanilla villages, other mods) always go first: ours give way to them.
 * <p>
 * keep_clear is the reach of the structure: how far (blocks, per axis) its pieces can lie from the middle of its start
 * chunk; it only limits how far away the other starts are looked for. /guhs bouwcheck reports when it is too small.
 * <p>
 * A structure that takes part also only starts where all of it fits in the world: above the bottom layer (nothing can
 * be built there) and below the top.
 * <p>
 * 1.1.2: the guaranteed copies ({@link GegarandeerdPlacement}) go before every normal set ({@link #GEGARANDEERD_VOORRANG} +
 * their own voorrang; the region/story structures and the Knabbelkelders, 800+, still go first), and the story structures
 * (structure tag {@code guhs:verhaal}) never start within {@link #VERHAAL_AFSTAND} blocks of 0,0.
 */
public final class BouwRuimte {
    /** Blocks of room kept between two buildings. */
    public static final int MARGIN = 4;
    /** How far the pieces of someone else's structure may reach (a jigsaw may go 128 from its start, plus a piece). */
    private static final int FOREIGN_REACH = 128 + 24;
    /** 1.1.2: the voorrang a guaranteed copy gets on top of its own (above every normal set, below the story ones: 800+). */
    public static final int GEGARANDEERD_VOORRANG = 500;
    /** 1.1.2: no story structure (tag guhs:verhaal) nearer to 0,0 than this (blocks: the nearest point of its pieces and start chunk). */
    public static final int VERHAAL_AFSTAND = 600;
    public static final net.minecraft.tags.TagKey<Structure> VERHAAL = net.minecraft.tags.TagKey.create(Registries.STRUCTURE,
            nl.juiced.guhs.Guhs.id("verhaal"));

    /** A structure that claims room. */
    public interface Ruimte {
        /** Reach in blocks around the middle of the start chunk (0: doesn't take part). */
        int keepClear();

        /** Who goes first (higher first); by default the reach. */
        int voorrang();
    }

    private BouwRuimte() {
    }

    // (per level: the structure state, for the concentric rings and the exclusion zones)
    private static final Map<RandomState, ChunkGeneratorStructureState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    public static void remember(RandomState random, ChunkGeneratorStructureState state) {
        STATES.put(random, state);
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            remember(level.getChunkSource().randomState(), level.getChunkSource().getGeneratorState());
            GegarandeerdPlacement.onthoud(level, level.getChunkSource().getGeneratorState(), level.getSeed());
            GegarandeerdPlacement.vooruit(level);
        }
    }

    private record SetInfo(Holder<StructureSet> set, String name, int voorrang, int reach) {
    }

    private record Index(Registry<StructureSet> registry, List<SetInfo> sets, Map<Structure, SetInfo> byStructure,
                         Map<Structure, List<SetInfo>> gegarandeerd, java.util.Set<Structure> verhaal) {
    }

    private static volatile Index index;
    private static final Map<BiomeSource, Map<SetInfo, Boolean>> POSSIBLE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, List<BoundingBox>> STARTS = new ConcurrentHashMap<>();

    private static Index index(RegistryAccess access) {
        Registry<StructureSet> registry = access.lookupOrThrow(Registries.STRUCTURE_SET);
        Index i = index;
        if (i != null && i.registry == registry) {
            return i;
        }
        List<SetInfo> sets = new ArrayList<>();
        Map<Structure, SetInfo> by = new HashMap<>();
        Map<Structure, List<SetInfo>> guaranteed = new HashMap<>();
        for (Holder.Reference<StructureSet> set : registry.listElements().toList()) {
            int voorrang = Integer.MIN_VALUE, reach = 0;
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                if (entry.structure().value() instanceof Ruimte r && r.keepClear() > 0) {
                    voorrang = Math.max(voorrang, r.voorrang());
                    reach = Math.max(reach, r.keepClear());
                }
            }
            if (reach > 0) {
                boolean g = set.value().placement() instanceof GegarandeerdPlacement;
                SetInfo info = new SetInfo(set, set.key().identifier().toString(), g ? GEGARANDEERD_VOORRANG + voorrang : voorrang, reach);
                sets.add(info);
                for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                    if (g) {
                        guaranteed.computeIfAbsent(entry.structure().value(), k -> new ArrayList<>()).add(info);
                    } else {
                        by.putIfAbsent(entry.structure().value(), info);
                    }
                }
            } else {
                // someone else's structures (vanilla villages, other mods): those always go first
                sets.add(new SetInfo(set, set.key().identifier().toString(), Integer.MAX_VALUE, FOREIGN_REACH));
            }
        }
        // (a structure that only has a guaranteed set: that set is its own)
        guaranteed.forEach((structure, list) -> by.putIfAbsent(structure, list.get(0)));
        java.util.Set<Structure> verhaal = new java.util.HashSet<>();
        for (Holder.Reference<Structure> structure : access.lookupOrThrow(Registries.STRUCTURE).listElements().toList()) {
            if (structure.is(VERHAAL)) {
                verhaal.add(structure.value());
            }
        }
        i = new Index(registry, List.copyOf(sets), Map.copyOf(by), Map.copyOf(guaranteed), java.util.Set.copyOf(verhaal));
        index = i;
        STARTS.clear();
        return i;
    }

    /**
     * The stub of a structure that claims room, or nothing when it gives way (or is in the wrong biome anyway). The
     * pieces are built here once and handed on, so the game doesn't build them again.
     */
    public static Optional<Structure.GenerationStub> claim(Structure.GenerationContext context, Structure me, Optional<Structure.GenerationStub> stub) {
        if (stub.isEmpty()) {
            return stub;
        }
        if (index(context.registryAccess()).verhaal.contains(me) && nearOrigin(context.chunkPos(), stub.get())) {
            return Optional.empty();   // (1.1.2: no story structure near spawn)
        }
        if (!(me instanceof Ruimte r) || r.keepClear() <= 0) {
            return stub;
        }
        var pos = stub.get().position();
        Holder<Biome> biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ()), context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return Optional.empty();
        }
        StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
        if (builder.isEmpty()) {
            return Optional.empty();
        }
        // only where it fits: nothing can be built in the bottom layer of the world or above the top
        BoundingBox all = builder.getBoundingBox();
        if (all.minY() <= context.heightAccessor().getMinY() || all.maxY() >= context.heightAccessor().getMaxY() + 1) {
            return Optional.empty();
        }
        List<BoundingBox> pieces = boxes(builder);
        if (givesWay(context, me, r.keepClear(), builder.getBoundingBox(), pieces)) {
            return Optional.empty();
        }
        return Optional.of(new Structure.GenerationStub(pos, Either.right(builder)));
    }

    /**
     * The pieces (boxes) of every structure in the chunks this area touches, for features that dig or build in the
     * rock and must leave buildings alone (the structures are already placed when a neighbouring chunk's features run).
     */
    public static List<BoundingBox> piecesNear(net.minecraft.world.level.WorldGenLevel level, int x0, int z0, int x1, int z1) {
        List<BoundingBox> out = new ArrayList<>();
        if (!(level instanceof net.minecraft.server.level.WorldGenRegion region)) {
            return out;
        }
        java.util.Set<net.minecraft.world.level.levelgen.structure.StructureStart> seen = new java.util.HashSet<>();
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                for (var start : startsAround(region, cx, cz)) {
                    if (seen.add(start)) {
                        start.getPieces().forEach(piece -> out.add(piece.getBoundingBox()));
                    }
                }
            }
        }
        return out;
    }

    /**
     * The structure starts that reach into this chunk (its references), as far as the region may look: during the
     * features step the starts within 8 chunks of the chunk being decorated (the same ones the game places pieces of).
     */
    private static List<net.minecraft.world.level.levelgen.structure.StructureStart> startsAround(
            net.minecraft.server.level.WorldGenRegion region, int cx, int cz) {
        List<net.minecraft.world.level.levelgen.structure.StructureStart> out = new ArrayList<>();
        ChunkPos centre = region.getCenter();
        if (!region.hasChunk(cx, cz) || centre.getChessboardDistance(new ChunkPos(cx, cz)) > 1) {
            return out;
        }
        var chunk = region.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_REFERENCES, false);
        if (chunk == null) {
            return out;
        }
        for (var entry : chunk.getAllReferences().entrySet()) {
            for (long ref : entry.getValue()) {
                ChunkPos at = ChunkPos.unpack(ref);
                if (centre.getChessboardDistance(at) > 8 || !region.hasChunk(at.x(), at.z())) {
                    continue;
                }
                var home = region.getChunk(at.x(), at.z(), net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, false);
                var start = home == null ? null : home.getStartForStructure(entry.getKey());
                if (start != null && start.isValid()) {
                    out.add(start);
                }
            }
        }
        return out;
    }

    /** Whether this spot lies in a piece of a structure (during worldgen: the chunk's structure references). */
    public static boolean inBuilding(net.minecraft.world.level.WorldGenLevel level, net.minecraft.core.BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.WorldGenRegion region) || !region.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        for (var start : startsAround(region, pos.getX() >> 4, pos.getZ() >> 4)) {
            for (var piece : start.getPieces()) {
                if (piece.getBoundingBox().isInside(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a feature working in this box would touch a building (one of the pieces of a structure around). */
    public static boolean touchesBuilding(net.minecraft.world.level.WorldGenLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        BoundingBox area = new BoundingBox(x0, y0, z0, x1, y1, z1);
        for (BoundingBox b : piecesNear(level, x0, z0, x1, z1)) {
            if (b.intersects(area)) {
                return true;
            }
        }
        return false;
    }

    /** Whether this spot is in (or right next to) one of these pieces. */
    public static boolean inAny(List<BoundingBox> pieces, net.minecraft.core.BlockPos pos) {
        for (BoundingBox b : pieces) {
            if (pos.getX() >= b.minX() - 1 && pos.getX() <= b.maxX() + 1 && pos.getY() >= b.minY() - 1 && pos.getY() <= b.maxY() + 1
                    && pos.getZ() >= b.minZ() - 1 && pos.getZ() <= b.maxZ() + 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.1.2: does this start (its pieces and its start chunk) come within {@link #VERHAAL_AFSTAND} blocks of 0,0? (The stub's
     * pieces are built here once; the builder keeps them for claim and the game.)
     */
    private static boolean nearOrigin(ChunkPos chunk, Structure.GenerationStub stub) {
        int minX = chunk.getMinBlockX(), maxX = chunk.getMaxBlockX(), minZ = chunk.getMinBlockZ(), maxZ = chunk.getMaxBlockZ();
        // (far away: no need to build the pieces for this; a structure reaches at most 8 chunks from its start chunk)
        long far = VERHAAL_AFSTAND + 9 * 16;
        long cx = Math.max(0, Math.max(minX, -maxX)), cz = Math.max(0, Math.max(minZ, -maxZ));
        if (cx * cx + cz * cz > far * far) {
            return false;
        }
        StructurePiecesBuilder builder = stub.getPiecesBuilder();
        if (!builder.isEmpty()) {
            BoundingBox box = builder.getBoundingBox();
            minX = Math.min(minX, box.minX());
            maxX = Math.max(maxX, box.maxX());
            minZ = Math.min(minZ, box.minZ());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        long dx = Math.max(0, Math.max(minX, -maxX)), dz = Math.max(0, Math.max(minZ, -maxZ));
        return dx * dx + dz * dz < (long) VERHAAL_AFSTAND * VERHAAL_AFSTAND;
    }

    /**
     * 1.1.2: the set this start belongs to: the guaranteed set of this structure when its search is trying this chunk or this is
     * its chunk, else the structure's normal set.
     */
    private static SetInfo eigenSet(Index i, Structure.GenerationContext context, Structure me) {
        for (SetInfo g : i.gegarandeerd.getOrDefault(me, List.of())) {
            GegarandeerdPlacement p = (GegarandeerdPlacement) g.set.value().placement();
            if (p.zoekt()) {
                if (p.probeert(context.chunkPos())) {
                    return g;
                }
                continue;
            }
            if (p.plek(STATES.get(context.randomState()), context.seed()).filter(context.chunkPos()::equals).isPresent()) {
                return g;
            }
        }
        return i.byStructure.get(me);
    }

    /**
     * 1.1.2: the flatness factor for a start of this structure in this chunk: more than 1 only for a guaranteed copy that found
     * no flat enough spot on its ring (see {@link GegarandeerdPlacement}).
     */
    public static int vlakFactor(Structure.GenerationContext context, Structure me) {
        List<SetInfo> list = index(context.registryAccess()).gegarandeerd.getOrDefault(me, List.of());
        for (SetInfo g : list) {
            int f = ((GegarandeerdPlacement) g.set.value().placement()).vlakFactor(STATES.get(context.randomState()), context.seed(), context.chunkPos());
            if (f > 0) {
                return f;
            }
        }
        return 1;
    }

    private static List<BoundingBox> boxes(StructurePiecesBuilder builder) {
        List<BoundingBox> out = new ArrayList<>();
        builder.build().pieces().forEach(piece -> out.add(piece.getBoundingBox()));
        return out;
    }

    private static boolean givesWay(Structure.GenerationContext context, Structure me, int myReach, BoundingBox box, List<BoundingBox> mine2) {
        Index i = index(context.registryAccess());
        SetInfo mine = eigenSet(i, context, me);
        if (mine == null) {
            return false;
        }
        ChunkPos here = context.chunkPos();
        ChunkGeneratorStructureState state = STATES.get(context.randomState());
        Map<SetInfo, Boolean> possible = POSSIBLE.computeIfAbsent(context.biomeSource(), b -> new ConcurrentHashMap<>());
        for (SetInfo other : i.sets) {
            boolean same = other == mine;
            if (!same && (other.voorrang < mine.voorrang || (other.voorrang == mine.voorrang && other.name.compareTo(mine.name) > 0))) {
                continue; // this one goes first
            }
            if (same && other.set.value().placement() instanceof GegarandeerdPlacement) {
                continue; // (a guaranteed set has only this one start)
            }
            if (!possible.computeIfAbsent(other, o -> canBeIn(o, context.biomeSource()))) {
                continue;
            }
            int range = myReach + other.reach + MARGIN + 16;
            for (ChunkPos c : candidates(other, state, context.seed(), here, range)) {
                if (same && (c.x() > here.x() || (c.x() == here.x() && c.z() >= here.z()))) {
                    continue; // (within one set the lower chunk goes first)
                }
                // (where its pieces could be at most: skip it when that is nowhere near our pieces)
                int cx = c.getMiddleBlockX(), cz = c.getMiddleBlockZ(), reach = other.reach + MARGIN;
                if (cx + reach < box.minX() || cx - reach > box.maxX() || cz + reach < box.minZ() || cz - reach > box.maxZ()) {
                    continue;
                }
                List<BoundingBox> theirs = startPieces(context, other, c);
                if (theirs.isEmpty()) {
                    continue;
                }
                for (BoundingBox t : theirs) {
                    BoundingBox around = t.inflatedBy(MARGIN);
                    if (!around.intersects(box)) {
                        continue;
                    }
                    for (BoundingBox m : mine2) {
                        if (around.intersects(m)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean canBeIn(SetInfo set, BiomeSource source) {
        var biomes = source.possibleBiomes();
        for (StructureSet.StructureSelectionEntry entry : set.set.value().structures()) {
            for (Holder<Biome> biome : entry.structure().value().biomes()) {
                if (biomes.contains(biome)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The chunks within range (per axis, blocks) where this set may start. */
    private static List<ChunkPos> candidates(SetInfo set, ChunkGeneratorStructureState state, long seed, ChunkPos here, int range) {
        StructurePlacement placement = set.set.value().placement();
        int r = range / 16 + 1;
        List<ChunkPos> out = new ArrayList<>();
        if (placement instanceof RandomSpreadStructurePlacement spread) {
            int s = spread.spacing();
            for (int rx = Math.floorDiv(here.x() - r, s); rx <= Math.floorDiv(here.x() + r, s); rx++) {
                for (int rz = Math.floorDiv(here.z() - r, s); rz <= Math.floorDiv(here.z() + r, s); rz++) {
                    ChunkPos c = spread.getPotentialStructureChunk(seed, rx * s, rz * s);
                    if (Math.abs(c.x() - here.x()) <= r && Math.abs(c.z() - here.z()) <= r
                            && (state == null || placement.isStructureChunk(state, c.x(), c.z()))) {
                        out.add(c);
                    }
                }
            }
        } else if (placement instanceof ConcentricRingsStructurePlacement rings && state != null) {
            List<ChunkPos> ring = state.getRingPositionsFor(rings);
            if (ring != null) {
                for (ChunkPos c : ring) {
                    if (Math.abs(c.x() - here.x()) <= r && Math.abs(c.z() - here.z()) <= r) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    /** The pieces (boxes) of what this set really starts in chunk c (like ChunkGenerator.createStructures), if anything. */
    private static List<BoundingBox> startPieces(Structure.GenerationContext context, SetInfo set, ChunkPos c) {
        String key = System.identityHashCode(context.randomState()) + "@" + context.seed() + "@" + set.name + "@" + c.pack();
        List<BoundingBox> known = STARTS.get(key);
        if (known != null) {
            return known;
        }
        List<StructureSet.StructureSelectionEntry> list = new ArrayList<>(set.set.value().structures());
        List<BoundingBox> found = List.of();
        if (list.size() == 1) {
            found = tryStart(context, list.get(0).structure().value(), c);
        } else {
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(context.seed(), c.x(), c.z());
            int total = list.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
            while (!list.isEmpty()) {
                int j = random.nextInt(total), k = 0;
                for (StructureSet.StructureSelectionEntry e : list) {
                    j -= e.weight();
                    if (j < 0) {
                        break;
                    }
                    k++;
                }
                StructureSet.StructureSelectionEntry entry = list.get(k);
                found = tryStart(context, entry.structure().value(), c);
                if (!found.isEmpty()) {
                    break;
                }
                list.remove(k);
                total -= entry.weight();
            }
        }
        if (STARTS.size() > 50000) {
            STARTS.clear();
        }
        STARTS.put(key, found);
        return found;
    }

    private static List<BoundingBox> tryStart(Structure.GenerationContext context, Structure structure, ChunkPos c) {
        Structure.GenerationContext there = new Structure.GenerationContext(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
                context.randomState(), context.structureTemplateManager(), context.seed(), c, context.heightAccessor(), structure.biomes()::contains);
        Optional<Structure.GenerationStub> stub = structure.findValidGenerationPoint(there);
        if (stub.isEmpty()) {
            return List.of();
        }
        StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
        return builder.isEmpty() ? List.of() : List.copyOf(boxes(builder));
    }
}
