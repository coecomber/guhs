package nl.juiced.guhs.world;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import nl.juiced.guhs.registry.ModStructureTypes;

/**
 * 1.1.2: placement {@code guhs:gegarandeerd}: exactly one guaranteed copy of a structure per world, in a ring of
 * {@code min_afstand}..{@code max_afstand} blocks around 0,0 (the minigames 700-1500, the landmarks 1500-2500; their normal
 * random_spread sets stay as well).
 * <p>
 * Where: the ring is cut into {@code sectoren} slices (a per-world turn, the same for every set), this set takes slice
 * {@code sector}, so the guaranteed buildings lie spread around spawn. In that slice (then anywhere on the ring, then without the
 * quick biome filter, then with a flatness check relaxed x2 and x3, see {@link #RONDES}) spots are tried in a fixed order (from
 * the seed and the salt) until the structure really starts there:
 * its biome, its flatness check ({@link FlatJigsawStructure}), fits in the world and doesn't give way ({@link BouwRuimte}: the
 * guaranteed sets go before every normal set, only the region/story structures and the Knabbelkelders go before them). So the
 * copy always appears, unless the whole ring has no spot for it at all (then a warning is logged).
 * <p>
 * It is a {@link ConcentricRingsStructurePlacement} with no rings of its own (count 0): {@link #installeer} puts the copy's
 * chunk in the structure state as its ring position, so /locate (which compares ring positions by real distance with the
 * normal copies), the Superkompas, the guh compasses and {@link BouwRuimte} find it the usual way. The search needs the
 * level (chunk generator, templates), so every world registers itself ({@link #onthoud}: at level load, and /guhs bouwcheck for
 * its other seeds).
 * <p>
 * bbq2: two optional fields for the sets that are added to worlds that exist already (the old sets don't have them, their
 * spots stay exactly what they were):
 * <ul>
 *   <li>{@code "alleen_nieuw": true}: a structure start is only made when its chunk is generated for the first time, so in an
 *       existing world a new set whose spot lies in old chunks would never appear. Such a set only takes a spot where no chunk
 *       within its reach + 16 blocks (+ the reach of the buildings that would have to give way to it, but that may stand there
 *       already) exists yet ({@link NieuwTerrein}); when its ring has none, the search goes on in a ring of 1.5x and then 2x the
 *       distances, and as a last resort 3x and 4x ({@link #RUIMER}: a longer walk is better than a building, or a chapter of a
 *       story, that does not exist in this world at all). The spot is saved in the dimension ({@link GegarandeerdData}) the first time it is found and is the answer for
 *       ever after. In a brand-new world nothing exists yet, so the search finds what it always found.</li>
 *   <li>{@code "rond": "guhs:<other set>"}: the ring lies around the guaranteed copy of that set instead of around 0,0 (a
 *       chain of story places a walk apart). That set must go first (a higher voorrang), else nothing is placed.</li>
 * </ul>
 * bbq2, phase 3: the search never runs on the server thread by itself ({@link #vooruit} has its own thread, and while that
 * thread is busy a question from the server thread, e.g. a compass, gets "not known yet" instead of waiting for it:
 * {@link Ring#join}); a set without a spot is named loudly in the log at world load, and an operator can give it a spot
 * near a point of their choice ({@link #zoekRond}: /guhs bouwcheck plek); {@link #doos} tells where a guaranteed copy
 * stands (its box) before a single chunk of it is loaded.
 */
public class GegarandeerdPlacement extends ConcentricRingsStructurePlacement {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Which slice of the ring (sector of sectoren); flat fields in the placement JSON. */
    public record Sector(int sector, int sectoren) {
        public static final MapCodec<Sector> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(0, 255).optionalFieldOf("sector", 0).forGetter(Sector::sector),
                Codec.intRange(1, 256).optionalFieldOf("sectoren", 1).forGetter(Sector::sectoren)
        ).apply(i, Sector::new));
    }

    /**
     * bbq2: only in terrain that does not exist yet (alleen_nieuw), and around the guaranteed copy of another set instead of
     * 0,0 (rond: that structure set's id); flat fields in the placement JSON, both optional.
     */
    public record Nieuw(boolean alleenNieuw, Optional<Identifier> rond) {
        public static final Nieuw GEEN = new Nieuw(false, Optional.empty());
        public static final MapCodec<Nieuw> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("alleen_nieuw", false).forGetter(Nieuw::alleenNieuw),
                Identifier.CODEC.optionalFieldOf("rond").forGetter(Nieuw::rond)
        ).apply(i, Nieuw::new));
    }

    /** (the slice of the ring and the bbq2 fields together: the codec builder takes three more fields at most) */
    private record Extra(Sector sector, Nieuw nieuw) {
        static final MapCodec<Extra> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Sector.CODEC.forGetter(Extra::sector),
                Nieuw.CODEC.forGetter(Extra::nieuw)
        ).apply(i, Extra::new));
    }

    public static final MapCodec<GegarandeerdPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> placementCodec(i).and(i.group(
            Codec.intRange(0, 100000).fieldOf("min_afstand").forGetter(p -> p.minAfstand),
            Codec.intRange(16, 100000).fieldOf("max_afstand").forGetter(p -> p.maxAfstand),
            Extra.CODEC.forGetter(p -> new Extra(new Sector(p.sector, p.sectoren), p.nieuw))
    )).apply(i, (offset, method, frequency, salt, exclusion, min, max, extra) ->
            new GegarandeerdPlacement(offset, method, frequency, salt, exclusion, min, max, extra.sector(), extra.nieuw())));

    /**
     * bbq2: the ring grows this much when an alleen_nieuw set finds no new terrain in it. Everything beyond
     * {@link #RUIMER_GEWOON} is the last resort (logged as a warning): the building stands further away than it was meant
     * to, but it stands. Without it one link of the story chain that finds no spot would end the story for everybody.
     */
    private static final double[] RUIMER = {1.0, 1.5, 2.0, 3.0, 4.0};
    /** bbq2: up to this factor a wider ring is nothing special. */
    public static final double RUIMER_GEWOON = 2.0;
    /** bbq2: blocks of new terrain kept around the reach of an alleen_nieuw copy. */
    public static final int NIEUW_RAND = 16;

    private final int minAfstand;
    private final int maxAfstand;
    private final int sector;
    private final int sectoren;
    private final Nieuw nieuw;

    public GegarandeerdPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                                 Optional<ExclusionZone> exclusion, int minAfstand, int maxAfstand, Sector sector) {
        this(locateOffset, method, frequency, salt, exclusion, minAfstand, maxAfstand, sector, Nieuw.GEEN);
    }

    public GegarandeerdPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                                 Optional<ExclusionZone> exclusion, int minAfstand, int maxAfstand, Sector sector, Nieuw nieuw) {
        super(locateOffset, method, frequency, salt, exclusion, 1, 1, 0, HolderSet.empty());
        this.minAfstand = minAfstand;
        this.maxAfstand = Math.max(maxAfstand, minAfstand + 64);
        this.sector = sector.sector();
        this.sectoren = sector.sectoren();
        this.nieuw = nieuw;
    }

    /** The slice of the ring this set takes (sector of sectoren), and its salt. */
    public int sector() {
        return sector;
    }

    public int sectoren() {
        return sectoren;
    }

    public int zout() {
        return salt();
    }

    /** bbq2: does this set only stand in terrain that did not exist yet when its spot was chosen? */
    public boolean alleenNieuw() {
        return nieuw.alleenNieuw();
    }

    /** bbq2: the set whose guaranteed copy is the middle of this one's ring (empty: 0,0). */
    public Optional<Identifier> rond() {
        return nieuw.rond();
    }

    public int minAfstand() {
        return minAfstand;
    }

    public int maxAfstand() {
        return maxAfstand;
    }

    // --- the worlds ------------------------------------------------------------------------------------------------------

    /**
     * A world the search can generate structures in: a level (templates, registries), the chunk generator and build height of
     * the dimension (null: the level's own), its structure state and seed.
     */
    private static final class Wereld {
        final WeakReference<ServerLevel> level;
        final WeakReference<ChunkGeneratorStructureState> state;
        @Nullable
        final ChunkGenerator generator;
        @Nullable
        final LevelHeightAccessor height;
        final long seed;
        final Map<GegarandeerdPlacement, Optional<Plek>> plekken = new ConcurrentHashMap<>();
        /**
         * bbq2: the saved spots of the alleen_nieuw sets, when this is the level's own world (its own generator state): only
         * there chunks exist and spots are kept. Null for the other seeds of /guhs bouwcheck and the gametests' generators:
         * those are brand-new worlds, searched like before.
         */
        @Nullable
        final GegarandeerdData opslag;
        /** bbq2: which chunks of that world exist (asked when a search starts); null when {@link #opslag} is. */
        @Nullable
        final java.util.function.Supplier<NieuwTerrein> terrein;
        /** bbq2: the level's own world (a found spot is written to disk at once). */
        final boolean echt;

        Wereld(ServerLevel level, ChunkGeneratorStructureState state, long seed, @Nullable ChunkGenerator generator, @Nullable LevelHeightAccessor height) {
            this.level = new WeakReference<>(level);
            this.state = new WeakReference<>(state);
            this.seed = seed;
            this.generator = generator;
            this.height = height;
            this.opslag = generator == null && level.getChunkSource().getGeneratorState() == state ? OPSLAG.get(level) : null;
            this.terrein = opslag == null ? null : () -> NieuwTerrein.van(level);
            this.echt = opslag != null;
        }

        /** (tests) a world with its own saved spots and its own idea of which chunks exist. */
        Wereld(ServerLevel level, ChunkGeneratorStructureState state, long seed, ChunkGenerator generator, LevelHeightAccessor height,
               GegarandeerdData opslag, java.util.function.Supplier<NieuwTerrein> terrein) {
            this.level = new WeakReference<>(level);
            this.state = new WeakReference<>(state);
            this.seed = seed;
            this.generator = generator;
            this.height = height;
            this.opslag = opslag;
            this.terrein = terrein;
            this.echt = false;
        }
    }

    /** bbq2: the saved spots per level, loaded on the server thread ({@link #laad}) before anything searches. */
    private static final Map<ServerLevel, GegarandeerdData> OPSLAG = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * bbq2: loads the saved spots of this level's alleen_nieuw sets (server thread, at level load, before {@link #onthoud}),
     * and starts remembering the chunks it saves while one of them still has to be found.
     */
    public static void laad(ServerLevel level) {
        boolean heeft = false, open = false;
        GegarandeerdData data = null;
        for (Holder<StructureSet> set : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
            if (set.value().placement() instanceof GegarandeerdPlacement g && g.alleenNieuw()) {
                if (data == null) {
                    data = GegarandeerdData.get(level);
                }
                heeft = true;
                open |= data.plek(naam(set)) == null;
            }
        }
        if (heeft) {
            OPSLAG.put(level, data);
        }
        if (open) {
            NieuwTerrein.volg(level);
        }
    }

    /** bbq2: the saved spots of this level (null: it has no alleen_nieuw set). */
    @Nullable
    public static GegarandeerdData opslag(ServerLevel level) {
        return OPSLAG.get(level);
    }

    private static final List<Wereld> WERELDEN = new ArrayList<>();

    /** Registers a world (a level and the structure state of one seed: the level's own, or /guhs bouwcheck's). */
    public static void onthoud(ServerLevel level, ChunkGeneratorStructureState state, long seed) {
        onthoud(level, state, seed, null, null);
    }

    /**
     * Registers a world whose chunk generator and build height are not the level's (the gametests: the Guhmension's generator,
     * with the test level only for the templates and registries).
     */
    public static void onthoud(ServerLevel level, ChunkGeneratorStructureState state, long seed, @Nullable ChunkGenerator generator,
                               @Nullable LevelHeightAccessor height) {
        synchronized (WERELDEN) {
            WERELDEN.removeIf(w -> w.level.get() == null || w.state.get() == null || w.state.get() == state);
            WERELDEN.add(new Wereld(level, state, seed, generator, height));
        }
        installeer(state);
    }

    /**
     * bbq2 (tests: the test server has none of our dimensions): registers a world like
     * {@link #onthoud(ServerLevel, ChunkGeneratorStructureState, long, ChunkGenerator, LevelHeightAccessor)}, in which the
     * alleen_nieuw sets search as in a world that exists: {@code terrein} says which chunks are there, {@code opslag} keeps
     * the spots.
     */
    public static void onthoudBestaand(ServerLevel level, ChunkGeneratorStructureState state, long seed, ChunkGenerator generator,
                                       LevelHeightAccessor height, GegarandeerdData opslag, java.util.function.Supplier<NieuwTerrein> terrein) {
        synchronized (WERELDEN) {
            Wereld nieuw = new Wereld(level, state, seed, generator, height, opslag, terrein);
            for (Wereld w : WERELDEN) {
                if (w.state.get() == state && w.seed == seed) {
                    // (the sets that are not alleen_nieuw stand where they stood: no need to search them again)
                    w.plekken.forEach((g, plek) -> {
                        if (!g.alleenNieuw()) {
                            nieuw.plekken.put(g, plek);
                        }
                    });
                }
            }
            WERELDEN.removeIf(w -> w.level.get() == null || w.state.get() == null || w.state.get() == state);
            WERELDEN.add(nieuw);
        }
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (set.value().placement() instanceof GegarandeerdPlacement g) {
                g.laatste = null;   // (the answer of an earlier world of this state)
            }
        }
        installeer(state);
    }

    /** The newest registered world of this state (or else: of this seed) that has this placement. */
    @Nullable
    private Wereld wereld(@Nullable ChunkGeneratorStructureState state, long seed) {
        synchronized (WERELDEN) {
            for (int k = WERELDEN.size() - 1; k >= 0; k--) {
                Wereld w = WERELDEN.get(k);
                ChunkGeneratorStructureState s = w.state.get();
                if (s != null && s == state && set(s) != null) {
                    return w;
                }
            }
            for (int k = WERELDEN.size() - 1; k >= 0; k--) {
                Wereld w = WERELDEN.get(k);
                ChunkGeneratorStructureState s = w.state.get();
                if (s != null && w.seed == seed && w.level.get() != null && set(s) != null) {
                    return w;
                }
            }
        }
        return null;
    }

    /** The set of this placement among the sets that can be in this world (null: not in this world). */
    @Nullable
    private Holder<StructureSet> set(ChunkGeneratorStructureState state) {
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (set.value().placement() == this) {
                return set;
            }
        }
        return null;
    }

    // --- the placement ---------------------------------------------------------------------------------------------------

    /** The guaranteed copy: its start chunk and how much its flatness check was relaxed (1: not). */
    public record Plek(ChunkPos chunk, int vlak) {
    }

    /**
     * Makes the guaranteed copies of this structure state its ring positions (vanilla made an empty list for count 0): a future
     * whose join() asks {@link #plek} (the search, or its answer). Called for every state that is registered ({@link #onthoud}).
     */
    public static void installeer(ChunkGeneratorStructureState state) {
        state.ensureStructuresGenerated();
        synchronized (state) {
            for (Holder<StructureSet> set : state.possibleStructureSets()) {
                if (set.value().placement() instanceof GegarandeerdPlacement g && !(state.ringPositions.get(g) instanceof Ring)) {
                    state.ringPositions.put(g, new Ring(g, new WeakReference<>(state)));
                }
            }
        }
    }

    /** The "ring position" of a guaranteed copy: its chunk (or none), worked out when someone asks. */
    private static final class Ring extends java.util.concurrent.CompletableFuture<List<ChunkPos>> {
        private final GegarandeerdPlacement placement;
        private final WeakReference<ChunkGeneratorStructureState> state;

        Ring(GegarandeerdPlacement placement, WeakReference<ChunkGeneratorStructureState> state) {
            this.placement = placement;
            this.state = state;
        }

        @Override
        public List<ChunkPos> join() {
            ChunkGeneratorStructureState s = state.get();
            if (s == null || placement.nogBezig(s)) {
                return List.of();
            }
            return placement.plek(s, s.getLevelSeed()).map(List::of).orElse(List.of());
        }

        @Override
        public List<ChunkPos> get() {
            return join();
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return plek(state, state.getLevelSeed()).filter(p -> p.x() == x && p.z() == z).isPresent();
    }

    /** The chunk of the guaranteed copy in this world (empty: no world known, or no spot on the whole ring). */
    public Optional<ChunkPos> plek(@Nullable ChunkGeneratorStructureState state, long seed) {
        return info(state, seed).map(Plek::chunk);
    }

    /**
     * How much the flatness check of a start in this chunk is relaxed for this set (0: not this set's start): the round of
     * the search that tries it, or the round that found the copy.
     */
    public int vlakFactor(@Nullable ChunkGeneratorStructureState state, long seed, ChunkPos chunk) {
        for (Zoek z = ZOEKT.get(); z != null; z = z.vorige) {
            if (z.placement == this) {
                return z.chunk.equals(chunk) ? z.vlak : 0;
            }
        }
        return info(state, seed).filter(p -> p.chunk.equals(chunk)).map(Plek::vlak).orElse(0);
    }

    /** The last answer (state, seed, spot): isPlacementChunk asks for every chunk, this keeps that cheap. */
    private volatile Object[] laatste;

    @SuppressWarnings("unchecked")
    private Optional<Plek> info(@Nullable ChunkGeneratorStructureState state, long seed) {
        for (Zoek z = ZOEKT.get(); z != null; z = z.vorige) {
            if (z.placement == this) {
                return Optional.of(new Plek(z.chunk, z.vlak));   // (asked again while searching: the spot being tried)
            }
        }
        Object[] l = laatste;
        if (l != null && (state != null ? l[0] == state : (Long) l[1] == seed)) {
            return (Optional<Plek>) l[2];
        }
        Wereld w = wereld(state, seed);
        if (w == null) {
            return Optional.empty();
        }
        Optional<Plek> known = w.plekken.get(this);
        if (known == null) {
            synchronized (this) {
                known = w.plekken.get(this);
                if (known == null) {
                    known = zoek(w);
                    w.plekken.put(this, known);
                }
            }
        }
        laatste = new Object[]{w.state.get(), w.seed, known};
        return known;
    }

    /** Is this search trying this chunk right now (on this thread)? Then that start belongs to this set. */
    public boolean probeert(ChunkPos chunk) {
        for (Zoek z = ZOEKT.get(); z != null; z = z.vorige) {
            if (z.placement == this) {
                return z.chunk.equals(chunk);
            }
        }
        return false;
    }

    /** Is this search running on this thread? */
    public boolean zoekt() {
        for (Zoek z = ZOEKT.get(); z != null; z = z.vorige) {
            if (z.placement == this) {
                return true;
            }
        }
        return false;
    }

    private record Zoek(GegarandeerdPlacement placement, ChunkPos chunk, int vlak, @Nullable Zoek vorige) {
    }

    private static final ThreadLocal<Zoek> ZOEKT = new ThreadLocal<>();

    /** bbq2: the structure states whose guaranteed copies are being searched by {@link #vooruit} right now. */
    private static final Set<ChunkGeneratorStructureState> BEZIG = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
    /**
     * bbq2: told (on the search thread) when {@link #vooruit} has looked for every guaranteed copy of a level: who wants to
     * know where they stand before anybody gets near ({@code feature.verhaal.Sluiers}: the spoiler walls of the story).
     */
    public static final List<java.util.function.Consumer<ServerLevel>> NA_VOORUIT = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * bbq2: is the search thread of this world still looking for this set's spot, while the SERVER thread asks? Then the
     * answer is "not known yet" (an empty ring, not kept): a compass shows nothing for a few seconds after a world load
     * instead of the whole server waiting for a search that can take many seconds. Worldgen threads, the search thread
     * itself and commands that ask {@link #plek} directly (/guhs bouwcheck) still wait for the real answer.
     */
    private boolean nogBezig(ChunkGeneratorStructureState state) {
        if (!BEZIG.contains(state)) {
            return false;
        }
        Wereld w = wereld(state, state.getLevelSeed());
        ServerLevel level = w == null ? null : w.level.get();
        return w != null && level != null && !w.plekken.containsKey(this) && level.getServer().isSameThread();
    }

    /**
     * Searches every guaranteed copy of this level right away, on a thread of its own (the ones that go first first, so no
     * search waits inside another): then the first chunks of the dimension don't wait for them. Chunks that need one earlier
     * simply wait for it; the server thread never does ({@link #nogBezig}).
     */
    public static void vooruit(ServerLevel level) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        List<Object[]> todo = new ArrayList<>();   // {placement, voorrang, name}
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (set.value().placement() instanceof GegarandeerdPlacement g) {
                todo.add(new Object[]{g, voorrang(set), naam(set)});
            }
        }
        if (todo.isEmpty()) {
            return;
        }
        todo.sort((a, b) -> (Integer) a[1] != (int) (Integer) b[1] ? Integer.compare((Integer) b[1], (Integer) a[1])
                : ((String) a[2]).compareTo((String) b[2]));
        long seed = level.getSeed();
        String dim = level.dimension().identifier().toString();
        BEZIG.add(state);
        Thread thread = new Thread(() -> {
            long t = System.nanoTime();
            int found = 0;
            List<String> geen = new ArrayList<>();
            try {
                for (Object[] o : todo) {
                    GegarandeerdPlacement g = (GegarandeerdPlacement) o[0];
                    boolean er = false;
                    try {
                        er = g.info(state, seed).isPresent();
                    } catch (RuntimeException ex) {
                        LOGGER.warn("Guhs: guaranteed {} failed", o[2], ex);
                        // (not searched again in this session: a question from the server thread must never start it)
                        Wereld w = g.wereld(state, seed);
                        if (w != null) {
                            w.plekken.putIfAbsent(g, Optional.empty());
                        }
                    }
                    if (er) {
                        found++;
                    } else {
                        geen.add((String) o[2]);
                    }
                }
            } finally {
                BEZIG.remove(state);
            }
            LOGGER.info("Guhs: {} of {} guaranteed buildings placed in {} ({} s)", found, todo.size(), dim, (System.nanoTime() - t) / 1_000_000_000);
            if (!geen.isEmpty()) {
                meldGeenPlek(dim, geen);
            }
            NieuwTerrein.klaar(level);   // (bbq2: the searches of this load are done: the saved chunks need not be remembered any more)
            for (java.util.function.Consumer<ServerLevel> l : NA_VOORUIT) {
                try {
                    l.accept(level);
                } catch (RuntimeException ex) {
                    LOGGER.warn("Guhs: a listener of the guaranteed search failed in {}", dim, ex);
                }
            }
        }, "Guhs gegarandeerd " + dim);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * bbq2: GEEN PLEK, loudly (one block in the log per dimension and world load). A guaranteed building without a spot
     * only exists as random copies in terrain that is still to be explored, and a place of a STORY (it has no random copies)
     * does not exist at all: that story cannot be finished in this world until an operator gives it a spot.
     */
    private static void meldGeenPlek(String dim, List<String> geen) {
        StringBuilder b = new StringBuilder();
        b.append("\n==================== Guhs: GEEN PLEK (").append(dim).append(") ====================");
        b.append("\n").append(geen.size()).append(" guaranteed building(s) found NO spot in this world (not even in a ring ")
                .append((int) RUIMER[RUIMER.length - 1]).append("x as wide):");
        for (String naam : geen) {
            b.append("\n  - ").append(naam);
        }
        b.append("\nA building with random copies still appears in terrain nobody has been to yet. A place of a story (the chain of the");
        b.append("\nKnabbelring: guhvendel, knabbelmoria, guhladriel_boomstad, zwarte_roosterpoort, frituurberg, sausuman_toren) has no random");
        b.append("\ncopies: without it that story stops there for EVERY player. The search is tried again at every world load.");
        b.append("\nAn operator can give a set a spot near a point of their choice, in terrain that was never generated:");
        b.append("\n  /execute in ").append(dim).append(" run guhs bouwcheck plek <set without guhs:> <x> <z> [straal]");
        b.append("\n  (/execute in ").append(dim).append(" run locate biome ... finds the biome it needs; /guhs bouwcheck gegarandeerd ").append(dim)
                .append(" lists every set)");
        b.append("\n=====================================================================================");
        LOGGER.error(b.toString());
    }

    /**
     * bbq2 (/guhs bouwcheck plek, server thread, an operator's emergency tool): gives a guaranteed "alleen_nieuw" set that
     * has NO spot in this level a spot near a point of the operator's choice: a place within {@code straal} blocks of
     * (x, z) where the set really starts, in terrain that was never generated, in its own biome. The spot is saved like any
     * other, and the sets that were waiting for this one (the next links of a story chain lie around it) are searched again
     * on the search thread. Returns the spot, or a text (Dutch, like the rest of bouwcheck) that says why not.
     */
    public static com.mojang.datafixers.util.Either<Plek, String> zoekRond(ServerLevel level, Identifier setId, int x, int z, int straal) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        if (BEZIG.contains(state)) {
            return com.mojang.datafixers.util.Either.right("de zoektocht van deze wereld loopt nog: probeer het zo nog eens");
        }
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (naam(set).equals(setId.toString()) && set.value().placement() instanceof GegarandeerdPlacement g) {
                com.mojang.datafixers.util.Either<Plek, String> uit = g.zoekRond(state, level.getSeed(), x, z, straal);
                if (uit.left().isPresent()) {
                    vooruit(level);   // (the sets that were waiting for this one: once more, on the search thread)
                }
                return uit;
            }
        }
        return com.mojang.datafixers.util.Either.right("geen gegarandeerde set " + setId + " in deze dimensie (/guhs bouwcheck gegarandeerd)");
    }

    /**
     * The search of {@link #zoekRond(ServerLevel, Identifier, int, int, int)} for this set in a registered world (also for
     * the tests, which have their own structure state): the spot is saved and becomes the answer; the sets of that world
     * that had found nothing are forgotten, so they are searched again the next time somebody asks.
     */
    public com.mojang.datafixers.util.Either<Plek, String> zoekRond(ChunkGeneratorStructureState state, long seed, int x, int z, int straal) {
        int bereik = Math.max(64, straal);
        Holder<StructureSet> set = set(state);
        Wereld w = wereld(state, seed);
        String naam = set == null ? "?" : naam(set);
        if (set == null || w == null || w.opslag == null || !alleenNieuw()) {
            return com.mojang.datafixers.util.Either.right(naam + " is geen 'alleen_nieuw' set van deze wereld: zijn plek ligt vast door de seed");
        }
        Optional<Plek> nu = info(state, seed);
        if (nu.isPresent()) {
            return com.mojang.datafixers.util.Either.right(naam + " heeft al een plek (chunk " + nu.get().chunk() + "): een set verhuist nooit");
        }
        Optional<Plek> plek;
        synchronized (this) {
            plek = zoek(w, set, x, z, 0, bereik, new double[]{1.0}, true);
            if (plek.isPresent()) {
                w.plekken.put(this, plek);
            }
            laatste = null;
        }
        if (plek.isEmpty()) {
            return com.mojang.datafixers.util.Either.right("geen plek voor " + naam + " binnen " + bereik + " blokken van " + x + ", " + z
                    + " (nieuw terrein, zijn eigen biome, vlak genoeg): zie het log, probeer een ander punt");
        }
        for (Holder<StructureSet> other : state.possibleStructureSets()) {
            if (other.value().placement() instanceof GegarandeerdPlacement o && w.plekken.remove(o, Optional.<Plek>empty())) {
                o.laatste = null;
            }
        }
        return com.mojang.datafixers.util.Either.left(plek.get());
    }

    /**
     * bbq2: the box of the guaranteed copy of this set in this world (all its pieces, as its structure start will have it),
     * worked out from the generator alone: no chunk is loaded or made for it, so it is known before anybody was ever near.
     * Empty: no spot, or the world is not registered. Any thread (the search thread asks, {@link #NA_VOORUIT}).
     */
    public Optional<net.minecraft.world.level.levelgen.structure.BoundingBox> doos(@Nullable ChunkGeneratorStructureState state, long seed) {
        Optional<Plek> plek = info(state, seed);
        Wereld w = wereld(state, seed);
        ServerLevel level = w == null ? null : w.level.get();
        ChunkGeneratorStructureState s = w == null ? null : w.state.get();
        Holder<StructureSet> set = s == null ? null : set(s);
        if (plek.isEmpty() || level == null || set == null) {
            return Optional.empty();
        }
        ChunkGenerator generator = w.generator != null ? w.generator : level.getChunkSource().getGenerator();
        LevelHeightAccessor height = w.height != null ? w.height : level;
        for (StructureSet.StructureSelectionEntry e : set.value().structures()) {
            Structure structure = e.structure().value();
            try {
                StructureStart start = structure.generate(e.structure(), level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(),
                        s.randomState(), level.getStructureManager(), w.seed, plek.get().chunk(), 0, height, structure.biomes()::contains);
                if (start.isValid()) {
                    return Optional.of(start.getBoundingBox());
                }
            } catch (RuntimeException ex) {
                LOGGER.debug("Guhs: the box of the guaranteed {} at {} failed", naam(set), plek.get().chunk(), ex);
            }
        }
        return Optional.empty();
    }

    /** The per-world turn of the slices (the same for every set, so the slices never overlap). */
    static double draai(long seed) {
        long h = mix(seed ^ 0x6A09E667F3BCC908L);
        return (h >>> 11) * 0x1.0p-53 * Math.PI * 2;
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** The search rounds: {tries, in the own slice (1) or the whole ring (0), quick biome filter (1/0), flatness factor}. */
    private static final int[][] RONDES = {{256, 1, 1, 1}, {512, 0, 1, 1}, {256, 0, 0, 1}, {384, 0, 1, 2}, {384, 0, 1, 3}};

    private Optional<Plek> zoek(Wereld w) {
        ServerLevel level = w.level.get();
        ChunkGeneratorStructureState state = w.state.get();
        if (level == null || state == null) {
            return Optional.empty();
        }
        Holder<StructureSet> set = set(state);
        if (set == null) {
            return Optional.empty();
        }
        // bbq2: an alleen_nieuw set in the level's own world: the saved spot is the answer for ever
        GegarandeerdData opslag = alleenNieuw() ? w.opslag : null;
        if (opslag != null) {
            Plek bewaard = opslag.plek(naam(set));
            if (bewaard != null) {
                return Optional.of(bewaard);
            }
        }
        // bbq2: the middle of the ring: 0,0, or the guaranteed copy of the set this one lies around
        int mx = 0, mz = 0;
        if (rond().isPresent()) {
            Optional<ChunkPos> midden = middenVan(state, w.seed, set);
            if (midden.isEmpty()) {
                return Optional.empty();
            }
            mx = midden.get().getMinBlockX();
            mz = midden.get().getMinBlockZ();
        }
        return zoek(w, set, mx, mz, minAfstand, maxAfstand, RUIMER, false);
    }

    /**
     * The search itself: a ring of {@code minAfstand}..{@code maxAfstand} blocks around (mx, mz), then (an alleen_nieuw set
     * in a world where chunks exist) the wider rings of {@code ruimerLijst}. {@code hand}: an operator chose the middle
     * ({@link #zoekRond}): the whole disc around it, from the first try on.
     */
    private Optional<Plek> zoek(Wereld w, Holder<StructureSet> set, int mx, int mz, int minAfstand, int maxAfstand, double[] ruimerLijst, boolean hand) {
        ServerLevel level = w.level.get();
        ChunkGeneratorStructureState state = w.state.get();
        if (level == null || state == null) {
            return Optional.empty();
        }
        GegarandeerdData opslag = alleenNieuw() ? w.opslag : null;
        long start = System.nanoTime();
        ChunkGenerator generator = w.generator != null ? w.generator : level.getChunkSource().getGenerator();
        LevelHeightAccessor height = w.height != null ? w.height : level;
        RandomState random = state.randomState();
        // bbq2: only where nothing exists yet (the level's own world; elsewhere the world is brand new)
        NieuwTerrein terrein = opslag != null && w.terrein != null ? w.terrein.get() : null;
        int vrij = terrein == null ? 0 : vrij(set, level, generator);
        double midden = draai(w.seed) + Math.PI * 2 * sector / sectoren, half = Math.PI / sectoren;
        SplittableRandom rng = new SplittableRandom(mix(w.seed * 31 + salt()));
        int tries = 0, biomeOk = 0, vlakOk = 0, nieuwOk = 0;
        for (double ruimer : terrein == null ? new double[]{1.0} : ruimerLijst) {
            int min = (int) Math.round(minAfstand * ruimer), max = (int) Math.round(maxAfstand * ruimer);
            for (int[] ronde : RONDES) {
                Set<ChunkPos> gehad = new HashSet<>();
                for (int k = 0; k < ronde[0]; k++) {
                    double a = ronde[1] == 1 && !hand ? midden + (rng.nextDouble() * 2 - 1) * half : rng.nextDouble() * Math.PI * 2;
                    // (evenly over the area of the ring; the locate spot, the chunk's corner, stays inside the ring)
                    double lo = hand ? 0 : min + 24, hi = max - 24;
                    double r = Math.sqrt(lo * lo + rng.nextDouble() * (hi * hi - lo * lo));
                    ChunkPos c = new ChunkPos(Math.floorDiv(mx + (int) Math.round(Math.cos(a) * r), 16), Math.floorDiv(mz + (int) Math.round(Math.sin(a) * r), 16));
                    double d = Math.hypot(c.getMinBlockX() - mx, c.getMinBlockZ() - mz);
                    if (d < min || d > max || !gehad.add(c)) {
                        continue;
                    }
                    tries++;
                    if (terrein != null && !terrein.nieuw(c, vrij)) {
                        continue;   // (the cheapest check first: around the players most of a ring exists already)
                    }
                    nieuwOk++;
                    if (ronde[2] == 1 && !biomePast(set, generator, random, height, c)) {
                        continue;
                    }
                    biomeOk++;
                    if (!vlakGenoeg(set, generator, random, height, c, ronde[3])) {
                        continue;
                    }
                    vlakOk++;
                    if (start(set, level, height, generator, random, w.seed, c, ronde[3])) {
                        LOGGER.debug("Guhs: guaranteed {} at chunk {} ({} blocks, flatness x{}, {} tries, {} ms)", naam(set), c, (int) d, ronde[3], tries,
                                (System.nanoTime() - start) / 1_000_000);
                        Plek plek = new Plek(c, ronde[3]);
                        if (opslag != null) {
                            plek = opslag.zet(naam(set), plek);
                            LOGGER.info("Guhs: the guaranteed {} stands at chunk {} ({} blocks from {}, new terrain within {} blocks{})", naam(set),
                                    plek.chunk(), (int) d, hand ? mx + ", " + mz + " (chosen by an operator)" : rond().map(Identifier::toString).orElse("0,0"), vrij,
                                    ruimer > 1 ? ", ring x" + ruimer : "");
                            if (ruimer > RUIMER_GEWOON) {
                                LOGGER.warn("Guhs: LAST RESORT: the guaranteed {} found no spot in new terrain within {} blocks of {}; it stands {} blocks "
                                        + "away (ring x{}). A longer walk than meant, but it exists.", naam(set), (int) Math.round(maxAfstand * RUIMER_GEWOON),
                                        rond().map(Identifier::toString).orElse("0,0"), (int) d, ruimer);
                            }
                            if (w.echt) {
                                bewaar(level);
                            }
                        }
                        return Optional.of(plek);
                    }
                }
            }
        }
        if (terrein != null) {
            LOGGER.warn("Guhs: GEEN PLEK: no spot in new terrain for the guaranteed {} between {} and {} blocks{} ({} spots, {} in new "
                    + "terrain, {} of those in its biome, {} flat enough); it is searched again the next time the world loads", naam(set), minAfstand,
                    maxAfstand, hand ? " of " + mx + ", " + mz : ", also not up to x" + (int) ruimerLijst[ruimerLijst.length - 1], tries, nieuwOk, biomeOk, vlakOk);
        } else {
            LOGGER.warn("Guhs: GEEN PLEK: no spot for the guaranteed {} between {} and {} blocks ({} spots, {} in its biome, {} flat enough)", naam(set),
                    minAfstand, maxAfstand, tries, biomeOk, vlakOk);
        }
        return Optional.empty();
    }

    /**
     * bbq2: the guaranteed copy of the set this one lies around (rond). That set must go first (a higher voorrang): a search
     * waits for the sets that go before it, never the other way round, so two searches can't wait for each other.
     */
    private Optional<ChunkPos> middenVan(ChunkGeneratorStructureState state, long seed, Holder<StructureSet> set) {
        Identifier id = rond().orElseThrow();
        for (Holder<StructureSet> other : state.possibleStructureSets()) {
            if (other.unwrapKey().filter(k -> k.identifier().equals(id)).isPresent() && other.value().placement() instanceof GegarandeerdPlacement g) {
                if (g == this || g.zoekt() || voorrang(other) <= voorrang(set)) {
                    LOGGER.error("Guhs: the guaranteed {} lies around {}, which doesn't go before it (it needs a higher voorrang): not placed", naam(set), id);
                    return Optional.empty();
                }
                Optional<ChunkPos> plek = g.plek(state, seed);
                if (plek.isEmpty()) {
                    LOGGER.warn("Guhs: the guaranteed {} lies around {}, which has no spot: not placed", naam(set), id);
                }
                return plek;
            }
        }
        LOGGER.warn("Guhs: the guaranteed {} lies around {}, which is no guaranteed set of this dimension: not placed", naam(set), id);
        return Optional.empty();
    }

    /** The voorrang of a set: the highest of its structures (0: none takes part in BouwRuimte). */
    private static int voorrang(Holder<StructureSet> set) {
        int voorrang = 0;
        for (StructureSet.StructureSelectionEntry e : set.value().structures()) {
            if (e.structure().value() instanceof BouwRuimte.Ruimte r) {
                voorrang = Math.max(voorrang, r.voorrang());
            }
        }
        return voorrang;
    }

    /**
     * bbq2: how far around the middle of its start chunk an alleen_nieuw copy wants new terrain (blocks, per axis): its own
     * reach + {@link #NIEUW_RAND}, + the reach of the furthest-reaching building that would have to give way to it. Such a
     * building gives way while it is generated, but one that stands in old chunks already can't: with this margin its start
     * chunk is new terrain too, or it is too far away to touch the copy.
     */
    private static int vrij(Holder<StructureSet> set, ServerLevel level, ChunkGenerator generator) {
        int reach = 0;
        for (StructureSet.StructureSelectionEntry e : set.value().structures()) {
            if (e.structure().value() instanceof BouwRuimte.Ruimte r) {
                reach = Math.max(reach, r.keepClear());
            }
        }
        return reach + NIEUW_RAND + BouwRuimte.reikwijdteNa(level.registryAccess(), generator.getBiomeSource(), set);
    }

    /** bbq2: writes the saved spots to disk right away (server thread): a crash must not make a second copy. */
    private static void bewaar(ServerLevel level) {
        level.getServer().execute(() -> level.getDataStorage().scheduleSave());
    }

    /**
     * bbq2 (/guhs bouwcheck gegarandeerd, tests): the name, spot and middle of the ring of every guaranteed set of this state,
     * the sets that go first first.
     */
    public record Kopie(String set, GegarandeerdPlacement placement, Optional<ChunkPos> plek, int middenX, int middenZ) {
    }

    public static List<Kopie> kopieen(ChunkGeneratorStructureState state, long seed) {
        List<Kopie> uit = new ArrayList<>();
        List<Holder<StructureSet>> sets = new ArrayList<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (set.value().placement() instanceof GegarandeerdPlacement) {
                sets.add(set);
            }
        }
        sets.sort((a, b) -> voorrang(a) != voorrang(b) ? Integer.compare(voorrang(b), voorrang(a)) : naam(a).compareTo(naam(b)));
        for (Holder<StructureSet> set : sets) {
            GegarandeerdPlacement g = (GegarandeerdPlacement) set.value().placement();
            Optional<ChunkPos> plek = g.plek(state, seed);
            int mx = 0, mz = 0;
            if (g.rond().isPresent()) {
                for (Kopie k : uit) {
                    if (k.set.equals(g.rond().get().toString()) && k.plek.isPresent()) {
                        mx = k.plek.get().getMinBlockX();
                        mz = k.plek.get().getMinBlockZ();
                    }
                }
            }
            uit.add(new Kopie(naam(set), g, plek, mx, mz));
        }
        return uit;
    }

    private static String naam(Holder<StructureSet> set) {
        return set.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
    }

    /** Quick filter: the biome at the surface of the chunk's middle suits one of the set's structures. */
    private static boolean biomePast(Holder<StructureSet> set, ChunkGenerator generator, RandomState random, LevelHeightAccessor height, ChunkPos c) {
        int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler());
        for (StructureSet.StructureSelectionEntry e : set.value().structures()) {
            if (e.structure().value().biomes().contains(biome)) {
                return true;
            }
        }
        return false;
    }

    /** Quick filter: the ground is flat enough for one of the set's structures ({@link FlatJigsawStructure}; others: yes). */
    private static boolean vlakGenoeg(Holder<StructureSet> set, ChunkGenerator generator, RandomState random, LevelHeightAccessor height, ChunkPos c, int factor) {
        for (StructureSet.StructureSelectionEntry e : set.value().structures()) {
            if (!(e.structure().value() instanceof FlatJigsawStructure flat) || flat.vlakGenoeg(generator, random, height, c, factor)) {
                return true;
            }
        }
        return false;
    }

    /** Does the set really start in chunk c (like ChunkGenerator.createStructures), with this search owning that start? */
    private boolean start(Holder<StructureSet> set, ServerLevel level, LevelHeightAccessor height, ChunkGenerator generator, RandomState random, long seed,
                          ChunkPos c, int vlak) {
        Zoek vorige = ZOEKT.get();
        ZOEKT.set(new Zoek(this, c, vlak, vorige));
        try {
            for (StructureSet.StructureSelectionEntry e : set.value().structures()) {
                Structure structure = e.structure().value();
                StructureStart s;
                try {
                    s = structure.generate(e.structure(), level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(), random,
                            level.getStructureManager(), seed, c, 0, height, structure.biomes()::contains);
                } catch (RuntimeException ex) {
                    LOGGER.debug("Guhs: guaranteed {} at {} failed", naam(set), c, ex);
                    continue;
                }
                if (s.isValid()) {
                    return true;
                }
            }
            return false;
        } finally {
            ZOEKT.set(vorige);
        }
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructureTypes.GEGARANDEERD.get();
    }
}
