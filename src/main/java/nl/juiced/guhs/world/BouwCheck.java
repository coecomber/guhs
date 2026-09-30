package nl.juiced.guhs.world;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * /guhs bouwcheck: checks whether the buildings of a dimension generate whole (operators only, for testing worldgen).
 * <ul>
 *   <li>overlap: collects the structure starts in a square (straal blocks around 0,0) the way the chunk generator does,
 *       for the world seed and some other seeds, without generating chunks, and counts the starts whose pieces cut into
 *       each other (at least one of them a guhs structure), plus pieces the game would never place (more than 8 chunks
 *       from their start, or outside the build height);</li>
 *   <li>compleet: generates the guhs buildings of the world seed in that square (a ticket per building) and compares the
 *       placed blocks with their templates: how much is right, how much is missing or overwritten (by another building
 *       or by the terrain), and how much of the bottom layer hangs above air.</li>
 * </ul>
 * The report goes to the chat (short) and to &lt;world&gt;/bouwcheck/&lt;dimension&gt;.txt (long).
 */
public final class BouwCheck {
    private static final TicketType<ChunkPos> TICKET = TicketType.create("guhs_bouwcheck", Comparator.comparingLong(ChunkPos::toLong));
    private static final int PARALLEL = 3;
    private static final int TIMEOUT_TICKS = 20 * 180;

    private static volatile Job job;
    private static volatile boolean scanning;

    private BouwCheck() {
    }

    /** One structure start found by the scan. */
    record Start(String id, String set, boolean guhs, long seed, ChunkPos chunk, BoundingBox box, List<BoundingBox> pieces, Structure structure) {
    }

    // --- command --------------------------------------------------------------------------------------------------------

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bouwcheck").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.argument("dimensie", DimensionArgument.dimension())
                        .then(Commands.argument("straal", IntegerArgumentType.integer(16, 30000))
                                .executes(c -> run(c, 3, true, 0))
                                .then(Commands.literal("overlap")
                                        .executes(c -> run(c, 3, false, 0))
                                        .then(Commands.argument("seeds", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> run(c, IntegerArgumentType.getInteger(c, "seeds"), false, 0))))
                                .then(Commands.literal("seed")
                                        .then(Commands.argument("seed", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                                                .executes(c -> run(c, -1, false, 0))))
                                .then(Commands.literal("compleet")
                                        .executes(c -> run(c, 1, true, 0))
                                        .then(Commands.argument("max", IntegerArgumentType.integer(1, 10000))
                                                .executes(c -> run(c, 1, true, IntegerArgumentType.getInteger(c, "max")))))))));
    }

    private static int stop(CommandSourceStack source) {
        Job j = job;
        if (j == null) {
            source.sendFailure(Component.literal("Er loopt geen bouwcheck."));
            return 0;
        }
        j.stopped = true;
        source.sendSuccess(() -> Component.literal("Bouwcheck wordt gestopt."), true);
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> c, int seeds, boolean complete, int max) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerLevel level = DimensionArgument.getDimension(c, "dimensie");
        int radius = IntegerArgumentType.getInteger(c, "straal");
        if (job != null || scanning) {
            source.sendFailure(Component.literal("Er loopt al een bouwcheck (/guhs bouwcheck stop)."));
            return 0;
        }
        scanning = true;
        String dim = level.dimension().identifier().toString();
        source.sendSuccess(() -> Component.literal("Bouwcheck " + dim + ": gebouwen zoeken binnen " + radius + " blokken, "
                + (seeds < 0 ? "seed " + com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed") : seeds + (seeds == 1 ? " seed" : " seeds"))
                + "...").withStyle(ChatFormatting.GRAY), true);
        MinecraftServer server = source.getServer();
        long worldSeed = seeds < 0 ? com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "seed") : level.getSeed();
        int count = Math.max(1, seeds);
        CompletableFuture.supplyAsync(() -> {
            Map<Long, List<Start>> found = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                long seed = i == 0 ? worldSeed : worldSeed * 31L + i * 7919L + 12345L;
                found.put(seed, scan(level, seed, radius));
            }
            return found;
        }, Util.backgroundExecutor()).whenComplete((found, error) -> server.execute(() -> {
            scanning = false;
            if (error != null) {
                source.sendFailure(Component.literal("Bouwcheck mislukt: " + error));
                return;
            }
            StringBuilder report = new StringBuilder();
            List<String> chat = overlapReport(found, radius, report, level.getMinY(), level.getMaxY() + 1);
            write(server, level, "overlap", report.toString());
            chat.forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
            if (complete) {
                List<Start> mine = new ArrayList<>(found.get(worldSeed).stream().filter(Start::guhs).toList());
                if (max > 0) {
                    Map<String, Integer> per = new HashMap<>();
                    mine.sort(Comparator.comparingLong(s -> (long) s.chunk.x() * s.chunk.x() + (long) s.chunk.z() * s.chunk.z()));
                    mine.removeIf(s -> per.merge(s.id, 1, Integer::sum) > max);
                }
                job = new Job(source, level, mine, found.get(worldSeed));
                source.sendSuccess(() -> Component.literal("Volledigheid: " + job.todo.size() + " gebouwen genereren en vergelijken...")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }));
        return 1;
    }

    // --- scan -----------------------------------------------------------------------------------------------------------

    /** All structure starts in the square, like ChunkGenerator.createStructures would make them for this seed. */
    static List<Start> scan(ServerLevel level, long seed, int radius) {
        ServerChunkCache chunks = level.getChunkSource();
        ChunkGenerator generator = chunks.getGenerator();
        RegistryAccess access = level.registryAccess();
        RandomState random;
        ChunkGeneratorStructureState state;
        if (seed == level.getSeed()) {
            random = chunks.randomState();
            state = chunks.getGeneratorState();
        } else if (generator instanceof NoiseBasedChunkGenerator noise) {
            random = RandomState.create(noise.generatorSettings().value(), access.lookupOrThrow(Registries.NOISE), seed);
            state = ChunkGeneratorStructureState.createForNormal(random, seed, generator.getBiomeSource(), access.lookupOrThrow(Registries.STRUCTURE_SET));
        } else {
            return List.of();
        }
        state.ensureStructuresGenerated();
        BouwRuimte.remember(random, state);
        StructureTemplateManager templates = level.getStructureManager();
        var structures = access.lookupOrThrow(Registries.STRUCTURE);
        int lo = SectionPos.blockToSectionCoord(-radius), hi = SectionPos.blockToSectionCoord(radius);
        List<Start> out = new ArrayList<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            StructurePlacement placement = set.value().placement();
            List<ChunkPos> candidates = new ArrayList<>();
            if (placement instanceof RandomSpreadStructurePlacement spread) {
                for (int rx = Math.floorDiv(lo, spread.spacing()); rx <= Math.floorDiv(hi, spread.spacing()); rx++) {
                    for (int rz = Math.floorDiv(lo, spread.spacing()); rz <= Math.floorDiv(hi, spread.spacing()); rz++) {
                        ChunkPos c = spread.getPotentialStructureChunk(seed, rx * spread.spacing(), rz * spread.spacing());
                        if (c.x() >= lo && c.x() <= hi && c.z() >= lo && c.z() <= hi && placement.isStructureChunk(state, c.x(), c.z())) {
                            candidates.add(c);
                        }
                    }
                }
            } else if (placement instanceof ConcentricRingsStructurePlacement rings) {
                for (ChunkPos c : state.getRingPositionsFor(rings)) {
                    if (c.x() >= lo && c.x() <= hi && c.z() >= lo && c.z() <= hi && placement.isStructureChunk(state, c.x(), c.z())) {
                        candidates.add(c);
                    }
                }
            }
            String setId = set.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            for (ChunkPos c : candidates) {
                List<StructureSet.StructureSelectionEntry> list = new ArrayList<>(set.value().structures());
                WorldgenRandom pick = new WorldgenRandom(new LegacyRandomSource(0L));
                pick.setLargeFeatureSeed(seed, c.x(), c.z());
                int total = list.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
                while (!list.isEmpty()) {
                    int k = 0;
                    if (list.size() > 1 || set.value().structures().size() > 1) {
                        int j = pick.nextInt(total);
                        for (StructureSet.StructureSelectionEntry e : list) {
                            j -= e.weight();
                            if (j < 0) {
                                break;
                            }
                            k++;
                        }
                    }
                    StructureSet.StructureSelectionEntry entry = list.get(k);
                    Structure structure = entry.structure().value();
                    StructureStart start;
                    try {
                        start = structure.generate(access, generator, generator.getBiomeSource(), random, templates, seed, c, 0, level,
                                structure.biomes()::contains);
                    } catch (RuntimeException e) {
                        start = StructureStart.INVALID_START;
                    }
                    if (start.isValid()) {
                        Identifier id = structures.getKey(structure);
                        List<BoundingBox> pieces = start.getPieces().stream().map(StructurePiece::getBoundingBox).toList();
                        out.add(new Start(String.valueOf(id), setId, id != null && id.getNamespace().equals("guhs"), seed, c,
                                start.getBoundingBox(), pieces, structure));
                        break;
                    }
                    list.remove(k);
                    total -= entry.weight();
                }
            }
        }
        return out;
    }

    // --- overlap --------------------------------------------------------------------------------------------------------

    private static List<String> overlapReport(Map<Long, List<Start>> found, int radius, StringBuilder report, int minY, int maxY) {
        Map<String, int[]> perStructure = new TreeMap<>();   // starts, overlapping, too far, outside build height
        Map<String, long[]> pairs = new TreeMap<>();         // times, total volume, biggest volume
        for (var entry : found.entrySet()) {
            List<Start> starts = new ArrayList<>(entry.getValue());
            starts.sort(Comparator.comparingInt(s -> s.box.minX()));
            boolean[] hit = new boolean[starts.size()];
            for (int i = 0; i < starts.size(); i++) {
                Start a = starts.get(i);
                for (int j = i + 1; j < starts.size() && starts.get(j).box.minX() <= a.box.maxX(); j++) {
                    Start b = starts.get(j);
                    if (!(a.guhs || b.guhs) || !a.box.intersects(b.box)) {
                        continue;
                    }
                    long volume = 0;
                    for (BoundingBox pa : a.pieces) {
                        for (BoundingBox pb : b.pieces) {
                            volume += intersection(pa, pb);
                        }
                    }
                    if (volume > 0) {
                        hit[i] = hit[j] = true;
                        String key = a.id.compareTo(b.id) <= 0 ? a.id + " x " + b.id : b.id + " x " + a.id;
                        long[] p = pairs.computeIfAbsent(key, k -> new long[3]);
                        p[0]++;
                        p[1] += volume;
                        p[2] = Math.max(p[2], volume);
                        report.append(String.format("  overlap seed %d: %s @%s x %s @%s, %d blokken%n", entry.getKey(), a.id, a.box.getCenter().toShortString(),
                                b.id, b.box.getCenter().toShortString(), volume));
                    }
                }
            }
            for (int i = 0; i < starts.size(); i++) {
                Start s = starts.get(i);
                if (!s.guhs) {
                    continue;
                }
                int[] p = perStructure.computeIfAbsent(s.id, k -> new int[]{0, 0, 0, 0, 0, Integer.MAX_VALUE, Integer.MIN_VALUE, 0});
                int mx = s.chunk.getMiddleBlockX(), mz = s.chunk.getMiddleBlockZ();
                boolean outside = false;
                for (BoundingBox b : s.pieces) {
                    p[5] = Math.min(p[5], b.minY());
                    p[6] = Math.max(p[6], b.maxY());
                    outside |= b.minY() <= minY || b.maxY() >= maxY;
                    p[3] = Math.max(p[3], Math.max(Math.max(mx - b.minX(), b.maxX() - mx), Math.max(mz - b.minZ(), b.maxZ() - mz)));
                }
                if (outside) {
                    p[7]++;
                }
                p[4] = s.structure instanceof BouwRuimte.Ruimte r ? r.keepClear() : -1;
                p[0]++;
                if (hit[i]) {
                    p[1]++;
                }
                int cx = s.chunk.getMinBlockX() - 8 * 16, cz = s.chunk.getMinBlockZ() - 8 * 16;
                boolean far = false;
                for (BoundingBox b : s.pieces) {
                    far |= b.minX() < cx || b.minZ() < cz || b.maxX() > cx + 17 * 16 - 1 || b.maxZ() > cz + 17 * 16 - 1;
                }
                if (far) {
                    p[2]++;
                }
            }
        }
        StringBuilder head = new StringBuilder();
        head.append(String.format("Bouwcheck overlap, straal %d, seeds %s%n%n", radius, found.keySet()));
        head.append(String.format("%-40s %7s %8s %8s %6s %6s %11s %7s%n", "structuur", "starts", "overlap", "te ver", "reikt", "ruimte",
                "stukken y", "hoogte"));
        List<String> chat = new ArrayList<>();
        int total = 0, overlapping = 0;
        for (var e : perStructure.entrySet()) {
            int[] p = e.getValue();
            total += p[0];
            overlapping += p[1];
            head.append(String.format("%-40s %7d %8d %8d %6d %6d %11s %7d%s%n", e.getKey(), p[0], p[1], p[2], p[3], p[4],
                p[5] + ".." + p[6], p[7], p[4] >= 0 && p[3] > p[4] ? "  RUIMTE TE KLEIN" : ""));
            if (p[7] > 0) {
                chat.add("  " + e.getKey() + ": " + p[7] + " starts komen buiten de bouwhoogte");
            }
            if (p[4] >= 0 && p[3] > p[4]) {
                chat.add("  " + e.getKey() + ": reikt " + p[3] + " blokken, keep_clear maar " + p[4]);
            }
            if (p[1] > 0 || p[2] > 0) {
                chat.add("  " + e.getKey() + ": " + p[0] + " starts, " + p[1] + " met overlap" + (p[2] > 0 ? ", " + p[2] + " te ver van hun start" : ""));
            }
        }
        head.append(String.format("%nParen:%n"));
        for (var e : pairs.entrySet()) {
            long[] p = e.getValue();
            head.append(String.format("  %-70s %4dx, samen %d blokken, grootste %d%n", e.getKey(), p[0], p[1], p[2]));
        }
        head.append(String.format("%nDetails:%n"));
        report.insert(0, head);
        chat.add(0, "Bouwcheck overlap: " + total + " guhs-gebouwen, " + overlapping + " met overlap, " + pairs.size() + " soorten paren.");
        return chat;
    }

    private static long intersection(BoundingBox a, BoundingBox b) {
        long x = Math.min(a.maxX(), b.maxX()) - Math.max(a.minX(), b.minX()) + 1;
        long y = Math.min(a.maxY(), b.maxY()) - Math.max(a.minY(), b.minY()) + 1;
        long z = Math.min(a.maxZ(), b.maxZ()) - Math.max(a.minZ(), b.minZ()) + 1;
        return x > 0 && y > 0 && z > 0 ? x * y * z : 0;
    }

    // --- completeness ---------------------------------------------------------------------------------------------------

    /** The comparison of one generated building with its templates. */
    static final class Result {
        String id;
        BlockPos at;
        long solid, right, missing, byOther, air, airFilled, base, floating, unknownPieces;
        boolean noStart, timeout;
        /** 2.10: the land around the building, per ring d blocks outside it: (land top) - (floor), see Grond.rand; delta = ground level delta. */
        double[] rand;
        int delta;
        final Map<String, Integer> wrong = new HashMap<>();
        final Map<String, BlockPos> sample = new HashMap<>();

        double pct() {
            return solid == 0 ? 100 : 100.0 * right / solid;
        }

        double airPct() {
            return air == 0 ? 0 : 100.0 * airFilled / air;
        }

        double floatPct() {
            return base == 0 ? 0 : 100.0 * floating / base;
        }
    }

    private static final class Job {
        final CommandSourceStack source;
        final ServerLevel level;
        final Deque<Start> todo;
        final List<Start> all;
        final List<Object[]> busy = new ArrayList<>();   // {Start, ChunkPos centre, radius, ticks}
        final List<Result> results = new ArrayList<>();
        final int count;
        boolean stopped;

        Job(CommandSourceStack source, ServerLevel level, List<Start> todo, List<Start> all) {
            this.source = source;
            this.level = level;
            this.todo = new ArrayDeque<>(todo);
            this.all = all;
            this.count = todo.size();
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Job j = job;
        if (j == null) {
            return;
        }
        ServerChunkCache chunks = j.level.getChunkSource();
        if (j.stopped) {
            j.todo.clear();
        }
        while (!j.stopped && j.busy.size() < PARALLEL && !j.todo.isEmpty()) {
            Start s = j.todo.poll();
            BoundingBox b = s.box;
            int x0 = b.minX() >> 4, x1 = b.maxX() >> 4, z0 = b.minZ() >> 4, z1 = b.maxZ() >> 4;
            ChunkPos centre = new ChunkPos((x0 + x1) >> 1, (z0 + z1) >> 1);
            int radius = Math.max(Math.max(centre.x() - x0, x1 - centre.x()), Math.max(centre.z() - z0, z1 - centre.z())) + 2;
            chunks.addRegionTicket(TICKET, centre, radius, centre);
            j.busy.add(new Object[]{s, centre, radius, 0});
        }
        long until = System.nanoTime() + 30_000_000L;
        for (int i = 0; i < j.busy.size() && System.nanoTime() < until; i++) {
            Object[] b = j.busy.get(i);
            Start s = (Start) b[0];
            int ticks = (int) b[3];
            b[3] = ticks + 1;
            boolean ready = true;
            for (int x = (s.box.minX() >> 4) - 1; x <= (s.box.maxX() >> 4) + 1 && ready; x++) {
                for (int z = (s.box.minZ() >> 4) - 1; z <= (s.box.maxZ() >> 4) + 1 && ready; z++) {
                    ready = chunks.getChunkNow(x, z) != null;
                }
            }
            if (!ready && ticks < TIMEOUT_TICKS && !j.stopped) {
                continue;
            }
            Result r;
            if (ready) {
                r = compare(j, s);
            } else {
                r = new Result();
                r.id = s.id;
                r.at = s.box.getCenter();
                r.timeout = true;
            }
            j.results.add(r);
            chunks.removeRegionTicket(TICKET, (ChunkPos) b[1], (int) b[2], (ChunkPos) b[1]);
            j.busy.remove(i--);
            if (j.results.size() % 25 == 0) {
                int done = j.results.size();
                j.source.sendSuccess(() -> Component.literal("  bouwcheck: " + done + "/" + j.count + " vergeleken").withStyle(ChatFormatting.GRAY), false);
            }
        }
        if (j.busy.isEmpty() && j.todo.isEmpty()) {
            job = null;
            StringBuilder report = new StringBuilder();
            List<String> chat = completeReport(j, report);
            write(j.source.getServer(), j.level, "compleet", report.toString());
            chat.forEach(line -> j.source.sendSuccess(() -> Component.literal(line), false));
        }
    }

    private static Field palettes, template;

    @SuppressWarnings("unchecked")
    private static Result compare(Job j, Start s) {
        Result r = new Result();
        r.id = s.id;
        r.at = s.box.getCenter();
        ServerLevel level = j.level;
        LevelChunk home = level.getChunkSource().getChunkNow(s.chunk.x(), s.chunk.z());
        StructureStart start = home == null ? null : home.getStartForStructure(s.structure);
        if (start == null || !start.isValid()) {
            r.noStart = true;
            return r;
        }
        List<BoundingBox> others = new ArrayList<>();
        for (Start o : j.all) {
            if (o != s && o.box.intersects(s.box)) {
                others.addAll(o.pieces);
            }
        }
        try {
            if (palettes == null) {
                palettes = StructureTemplate.class.getDeclaredField("palettes");
                palettes.setAccessible(true);
                template = SinglePoolElement.class.getDeclaredField("template");
                template.setAccessible(true);
            }
            for (StructurePiece piece : start.getPieces()) {
                StructureTemplate t;
                StructurePlaceSettings settings;
                BlockPos pos;
                if (piece instanceof TemplateStructurePiece tp) {
                    t = tp.template();
                    settings = tp.placeSettings();
                    pos = tp.templatePosition();
                } else if (piece instanceof PoolElementStructurePiece pp && pp.getElement() instanceof SinglePoolElement single) {
                    Either<Identifier, StructureTemplate> either = (Either<Identifier, StructureTemplate>) template.get(single);
                    t = either.map(level.getStructureManager()::getOrCreate, x -> x);
                    settings = new StructurePlaceSettings().setRotation(pp.getRotation());
                    pos = pp.getPosition();
                } else {
                    r.unknownPieces++;
                    continue;
                }
                List<StructureTemplate.Palette> list = (List<StructureTemplate.Palette>) palettes.get(t);
                if (list.isEmpty()) {
                    continue;
                }
                for (StructureTemplate.StructureBlockInfo info : settings.getRandomPalette(list, pos).blocks()) {
                    BlockState want = info.state();
                    if (want.is(Blocks.JIGSAW) || want.is(Blocks.STRUCTURE_BLOCK) || want.is(Blocks.STRUCTURE_VOID)) {
                        continue;
                    }
                    BlockPos at = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(pos);
                    if (level.isOutsideBuildHeight(at)) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
                    if (chunk == null) {
                        continue;
                    }
                    BlockState have = chunk.getBlockState(at);
                    boolean other = inAny(others, at);
                    if (want.isAir()) {
                        r.air++;
                        if (!have.isAir()) {
                            r.airFilled++;
                            if (other) {
                                r.byOther++;
                            }
                        }
                        continue;
                    }
                    r.solid++;
                    if (same(want, have)) {
                        r.right++;
                    } else {
                        if (have.isAir()) {
                            r.missing++;
                        }
                        if (other) {
                            r.byOther++;
                        }
                        String key = name(want) + ">" + name(have);
                        r.wrong.merge(key, 1, Integer::sum);
                        r.sample.putIfAbsent(key, at);
                    }
                    if (info.pos().getY() == 0 && !level.isOutsideBuildHeight(at.below())) {
                        r.base++;
                        if (chunk.getBlockState(at.below()).isAir()) {
                            r.floating++;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            r.unknownPieces++;
        }
        grond(level, start, others, r);
        return r;
    }

    /** 2.10 (DESIGN 9.1): how the land meets the building: rings 1..6 blocks outside the start's box, land top vs floor. */
    private static void grond(ServerLevel level, StructureStart start, List<BoundingBox> others, Result r) {
        if (start.getPieces().isEmpty() || !(start.getPieces().get(0) instanceof PoolElementStructurePiece first)
                || start.getStructure().terrainAdaptation() != net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_BOX) {
            return;   // (only the buildings that stand in the land: no caves, cellars or pits)
        }
        // (the pieces themselves: StructureStart.getBoundingBox() is inflated by 12 for the terrain adaptation)
        BoundingBox box = BoundingBox.encapsulatingBoxes(start.getPieces().stream().map(StructurePiece::getBoundingBox).toList()).orElseThrow();
        BoundingBox piece = first.getBoundingBox();
        r.delta = first.getGroundLevelDelta();
        // (the floor of the start piece, moved into the box of the whole start: same world y)
        BoundingBox whole = new BoundingBox(box.minX(), piece.minY(), box.minZ(), box.maxX(), Math.max(piece.minY(), box.maxY()), box.maxZ());
        r.rand = nl.juiced.guhs.world.grond.Grond.rand(whole, r.delta, 6, (x, z) -> {
            if (inAny(others, new BlockPos(x, piece.minY() + r.delta, z))) {
                return Integer.MIN_VALUE;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            return chunk == null ? Integer.MIN_VALUE : land(chunk, x, z);
        });
    }

    /** The first free y above the land of a column: through leaves, logs, plants, snow and fluids down to real ground. */
    static int land(LevelChunk chunk, int x, int z) {
        int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, y, z);
        while (y > chunk.getMinY()) {
            BlockState state = chunk.getBlockState(at.setY(y));
            if (!(state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced()
                    || !state.getFluidState().isEmpty() || state.is(Blocks.SNOW))) {
                return y + 1;
            }
            y--;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean inAny(List<BoundingBox> boxes, BlockPos at) {
        for (BoundingBox b : boxes) {
            if (b.isInside(at)) {
                return true;
            }
        }
        return false;
    }

    private static String name(BlockState state) {
        return state.getBlock().builtInRegistryHolder().key().identifier().getPath();
    }

    /** Same block, or a change the game makes on its own (grass under a block turns to dirt, fluids flow...). */
    private static boolean same(BlockState want, BlockState have) {
        if (want.getBlock() == have.getBlock()) {
            return true;
        }
        if (want.is(BlockTags.DIRT) && (have.is(BlockTags.DIRT) || have.is(Blocks.FARMLAND) || have.is(Blocks.DIRT_PATH))) {
            return true;
        }
        if (!want.getFluidState().isEmpty() && want.getFluidState().getType().isSame(have.getFluidState().getType())) {
            return true;
        }
        if (want.getBlock() instanceof net.minecraft.world.level.block.ConcretePowderBlock && name(have).equals(name(want).replace("_powder", ""))) {
            return true; // (hardens next to water)
        }
        return want.is(BlockTags.LEAVES) && have.is(BlockTags.LEAVES);
    }

    private static List<String> completeReport(Job j, StringBuilder report) {
        Map<String, List<Result>> per = new TreeMap<>();
        List<String> grond = new ArrayList<>();
        List<String> grondChat = new ArrayList<>();
        for (Result r : j.results) {
            per.computeIfAbsent(r.id, k -> new ArrayList<>()).add(r);
        }
        report.append(String.format("Bouwcheck volledigheid %s (wereldseed %d)%n%n", j.level.dimension().identifier(), j.level.getSeed()));
        report.append(String.format("%-40s %4s %7s %7s %6s %7s %7s %7s %s%n", "structuur", "n", "gem%", "min%", "<95%", "lucht%", "zweef%", "ander", "fout"));
        List<String> chat = new ArrayList<>();
        int bad = 0;
        for (var e : per.entrySet()) {
            List<Result> rs = e.getValue();
            double sum = 0, min = 100, air = 0, fl = 0;
            int below = 0, broken = 0;
            long other = 0;
            for (Result r : rs) {
                if (r.noStart || r.timeout) {
                    broken++;
                    continue;
                }
                sum += r.pct();
                min = Math.min(min, r.pct());
                air += r.airPct();
                fl += r.floatPct();
                other += r.byOther;
                if (r.pct() < 95) {
                    below++;
                }
            }
            int n = rs.size() - broken;
            Map<String, Integer> wrong = new HashMap<>();
            rs.forEach(r -> r.wrong.forEach((k, v) -> wrong.merge(k, v, Integer::sum)));
            String top = wrong.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(4)
                    .map(x -> x.getKey() + " " + x.getValue()).reduce((a, b) -> a + ", " + b).orElse("");
            double[] rand = new double[7];
            int[] randN = new int[7];
            int delta = 0;
            for (Result r : rs) {
                if (r.rand != null) {
                    delta = r.delta;
                    for (int d = 1; d <= 6; d++) {
                        if (!Double.isNaN(r.rand[d])) {
                            rand[d] += r.rand[d];
                            randN[d]++;
                        }
                    }
                }
            }
            if (randN[1] > 0) {
                StringBuilder g = new StringBuilder();
                for (int d = 1; d <= 6; d++) {
                    g.append(String.format(" %+.1f", randN[d] == 0 ? Double.NaN : rand[d] / randN[d]));
                }
                grond.add(String.format("  %s (grond %d):%s", e.getKey(), delta, g));
                if (delta > 1) {
                    grondChat.add(String.format("  %s:%s", e.getKey(), g));
                }
            }
            report.append(String.format("%-40s %4d %7.2f %7.2f %6d %7.2f %7.2f %7d %s%s%n", e.getKey(), n, n == 0 ? 0 : sum / n, min, below,
                    n == 0 ? 0 : air / n, n == 0 ? 0 : fl / n, other, broken > 0 ? "[" + broken + " zonder start/timeout] " : "", top));
            if (below > 0 || broken > 0) {
                bad++;
                String line = String.format("  %s: %d gebouwd, gemiddeld %.1f%%, slechtste %.1f%%, %d onder 95%%", e.getKey(), n, n == 0 ? 0 : sum / n, min, below)
                        + (broken > 0 ? ", " + broken + " zonder start" : "");
                chat.add(line);
            }
        }
        // 2.10: the land around the buildings, ring 1..6 blocks outside: (land top) - (floor), ~0 = meets the floor
        report.append(String.format("%nGrond rond de gebouwen (ring 1..6 blokken buiten het gebouw: landhoogte - vloer, ~0 = sluit aan):%n"));
        grond.forEach(line -> report.append(line).append(System.lineSeparator()));
        report.append(String.format("%nPer gebouw:%n"));
        for (Result r : j.results) {
            report.append(String.format("  %-38s %-18s %7.2f%% goed, %d weg, %d door ander gebouw, lucht gevuld %.1f%%, zweeft %.1f%%%s%n", r.id,
                    r.at.toShortString(), r.pct(), r.missing, r.byOther, r.airPct(), r.floatPct(),
                    r.noStart ? " GEEN START" : r.timeout ? " TIMEOUT" : ""));
            if (r.rand != null) {
                StringBuilder g = new StringBuilder("      grond " + r.delta + ", rand:");
                for (int d = 1; d <= 6; d++) {
                    g.append(String.format(" %+.1f", r.rand[d]));
                }
                report.append(g).append(System.lineSeparator());
            }
            r.wrong.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(3).forEach(x ->
                    report.append(String.format("      %s %d (bv. %s)%n", x.getKey(), x.getValue(), r.sample.get(x.getKey()).toShortString())));
        }
        chat.add(0, "Bouwcheck volledigheid: " + j.results.size() + " gebouwen vergeleken, " + bad + " soorten met problemen.");
        if (!grondChat.isEmpty()) {
            chat.add("Grond naast verzonken gebouwen (ring 1..6 blokken buiten, land - vloer; ~0 = geen slootje):");
            chat.addAll(grondChat);
        }
        return chat;
    }

    private static void write(MinecraftServer server, ServerLevel level, String kind, String text) {
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("bouwcheck");
            Files.createDirectories(dir);
            Path file = dir.resolve(level.dimension().identifier().getPath() + "_" + kind + ".txt");
            Files.writeString(file, text);
        } catch (IOException ignored) {
        }
    }
}
