package nl.juiced.guhs.feature.einde2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * einde2: where a structure of the outer islands is, and where its markers ("plekken") are.
 * <p>
 * Phase A places no character and no quest object: where the design puts one, the builder left the spot free and recorded
 * a marker in the sidecar {@code data/guhs/einde2/plekken/<structuur>.json} (written by tools/features/einde2_kit.py, in
 * TEMPLATE coordinates of the whole build; a jigsaw structure records them per piece). This class reads the sidecars
 * ({@link #van}, {@link #plek}) and turns a marker into a world position:
 * <ul>
 *   <li>the ONE guaranteed copy of a {@code guhs:leegte_bouw} ({@link #startChunk}, {@link #oorsprong}, {@link #doos},
 *       {@link #wereld(ServerLevel, String, String)}, {@link #aankomst}): pure functions of the world seed, no chunk is
 *       loaded or generated (it is what /locate and the Superkompas point at). A build that stands next to another one
 *       (its {@code bij}: Schelpenstad next to Bikinibodem) has its one copy at a fixed offset from that neighbour's.
 *       The search behind a spot can take tens of seconds right after a level loads (Bikinibodem's), so these never make
 *       the server thread wait for it: there they answer null ("not known yet") until {@link #zoekVooruit} has the spots
 *       of the level, and are a map lookup from then on. They say where the copy BELONGS; {@link #start} says whether it
 *       really stands there (in terrain that existed before einde2 it does not);</li>
 *   <li>any copy that is already there ({@link #bij}, {@link #wereld(StructureStart, Plek)}): a random copy of a side
 *       place, a Mika-vesting.</li>
 * </ul>
 * And it puts the overlay templates of a build ({@code <structuur>/laag_<naam>}) at the guaranteed copy and takes them
 * away again ({@link #plaatsLaag}, {@link #ruimLaag}).
 */
public final class Plekken {
    /** The kinds of marker. */
    public static final String NPC = "npc", PROP = "prop", ZONE = "zone", PAD = "pad", RING = "ring", WEZEN = "wezen";

    /**
     * A marker. {@code pos}: the block (an npc's feet, a prop, a ring's centre, the first corner of a zone, the first
     * point of a pad); {@code tot}: the other corner of a zone; {@code punten}: the points of a pad; {@code stuk}: the
     * template of the jigsaw piece the coordinates are local to; {@code kind}: the GuhNpcEntity.Kind name reserved for
     * the character; {@code entity}: the creature placed there; {@code straal} and {@code as}: a ring's radius and axis;
     * {@code wie}: a note for whoever fills the spot (who stands there, what it is).
     */
    public record Plek(String id, String soort, BlockPos pos, float yaw, @Nullable BlockPos tot, List<BlockPos> punten,
                       @Nullable String stuk, @Nullable String kind, @Nullable String entity, int straal, @Nullable Direction.Axis as,
                       String wie) {
    }

    /** An overlay template of a build: its template, where it goes (template coordinates) and its size. */
    public record Laag(String naam, Identifier template, BlockPos pos, Vec3i grootte) {
    }

    /** A whole sidecar. */
    public record Blad(String structuur, String soort, Vec3i grootte, BlockPos anker, Map<String, Plek> plekken, Map<String, Laag> lagen) {
        static final Blad LEEG = new Blad("", "", Vec3i.ZERO, BlockPos.ZERO, Map.of(), Map.of());
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Map<String, Blad> BLADEN = new ConcurrentHashMap<>();

    private Plekken() {
    }

    // --- the sidecars ---------------------------------------------------------------------------------------------------------

    /** The sidecar of a structure (an empty one when there is none). Read once, from the mod's own data. */
    public static Blad blad(String structuur) {
        return BLADEN.computeIfAbsent(structuur, Plekken::lees);
    }

    /** The markers of a structure, in template coordinates; empty when it has no sidecar. */
    public static Map<String, Plek> van(String structuur) {
        return blad(structuur).plekken();
    }

    @Nullable
    public static Plek plek(String structuur, String id) {
        return van(structuur).get(id);
    }

    private static Blad lees(String structuur) {
        String path = "/data/" + Guhs.MODID + "/einde2/plekken/" + structuur + ".json";
        try (InputStream in = Plekken.class.getResourceAsStream(path)) {
            if (in == null) {
                return Blad.LEEG;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Plek> plekken = new LinkedHashMap<>();
            if (root.has("plekken")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("plekken").entrySet()) {
                    plekken.put(e.getKey(), plek(e.getKey(), e.getValue().getAsJsonObject()));
                }
            }
            Map<String, Laag> lagen = new LinkedHashMap<>();
            if (root.has("lagen")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("lagen").entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    lagen.put(e.getKey(), new Laag(e.getKey(), Identifier.parse(o.get("template").getAsString()), pos(o.getAsJsonArray("pos")),
                            pos(o.getAsJsonArray("grootte"))));
                }
            }
            return new Blad(structuur, root.has("soort") ? root.get("soort").getAsString() : "", root.has("grootte") ? pos(root.getAsJsonArray("grootte")) : Vec3i.ZERO,
                    root.has("anker") ? pos(root.getAsJsonArray("anker")) : BlockPos.ZERO, java.util.Collections.unmodifiableMap(plekken),
                    java.util.Collections.unmodifiableMap(lagen));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Guhs einde2: can't read the plekken of {}", structuur, e);
            return Blad.LEEG;
        }
    }

    private static Plek plek(String id, JsonObject o) {
        List<BlockPos> punten = new ArrayList<>();
        if (o.has("punten")) {
            for (JsonElement p : o.getAsJsonArray("punten")) {
                punten.add(pos(p.getAsJsonArray()));
            }
        }
        BlockPos pos = o.has("pos") ? pos(o.getAsJsonArray("pos")) : punten.isEmpty() ? BlockPos.ZERO : punten.get(0);
        return new Plek(id, o.get("soort").getAsString(), pos, o.has("yaw") ? o.get("yaw").getAsFloat() : 0f,
                o.has("tot") ? pos(o.getAsJsonArray("tot")) : null, List.copyOf(punten), tekst(o, "stuk"), tekst(o, "kind"), tekst(o, "entity"),
                o.has("straal") ? o.get("straal").getAsInt() : 0, o.has("as") ? Direction.Axis.byName(o.get("as").getAsString()) : null,
                o.has("wie") ? o.get("wie").getAsString() : "");
    }

    @Nullable
    private static String tekst(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsString() : null;
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    // --- the one guaranteed copy of a leegte_bouw: pure functions of the world -------------------------------------------------

    /** The structure when it is a leegte_bouw (null: it isn't one, or it doesn't exist). */
    @Nullable
    public static LeegteBouwStructure leegteBouw(RegistryAccess access, String structuur) {
        return access.lookupOrThrow(Registries.STRUCTURE).getValue(Guhs.id(structuur)) instanceof LeegteBouwStructure l ? l : null;
    }

    // (per level: the start chunks that are worked out; an empty Optional: this world has no spot for it)
    private static final Map<ServerLevel, Map<String, Optional<ChunkPos>>> BEKEND = Collections.synchronizedMap(new WeakHashMap<>());
    // (the levels whose spots are being worked out, or are)
    private static final Map<ServerLevel, Boolean> GEZOCHT = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * The start chunk of the guaranteed copy: the ring position of the set {@code <structuur>_gegarandeerd}, or, for a build
     * with a {@code bij}, the chunk at its offset from the guaranteed copy of its neighbour.
     * <p>
     * Never makes the server thread wait for the search: asked there before the spot is worked out it answers null and
     * {@link #zoekVooruit} gets going (the tick loop would stand still for as long as the search takes, up to tens of
     * seconds for Bikinibodem). Any other thread waits for the answer.
     */
    @Nullable
    public static ChunkPos startChunk(ServerLevel guheinde, String structuur) {
        Map<String, Optional<ChunkPos>> bekend = bekend(guheinde);
        Optional<ChunkPos> al = bekend.get(structuur);
        if (al != null) {
            return al.orElse(null);
        }
        if (guheinde.getServer().isSameThread()) {
            zoekVooruit(guheinde);
            return null;
        }
        ChunkPos chunk = startChunk(guheinde.registryAccess(), guheinde.getChunkSource().getGeneratorState(), guheinde.getSeed(), structuur);
        bekend.put(structuur, Optional.ofNullable(chunk));
        return chunk;
    }

    private static Map<String, Optional<ChunkPos>> bekend(ServerLevel level) {
        return BEKEND.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
    }

    /** Is the spot of this structure worked out for this level ({@link #startChunk} then answers at once, also with "none")? */
    public static boolean bekend(ServerLevel guheinde, String structuur) {
        return bekend(guheinde).containsKey(structuur);
    }

    /**
     * Works out the spots of the fourteen big places of this level on a thread of its own, once per level (at level load,
     * and when the server thread asks before that is done). It waits for the searches the world itself runs at level load
     * (GegarandeerdPlacement.vooruit: the same ones, so nothing is searched twice) and keeps the answers.
     */
    public static void zoekVooruit(ServerLevel guheinde) {
        if (GEZOCHT.putIfAbsent(guheinde, Boolean.TRUE) != null) {
            return;
        }
        Map<String, Optional<ChunkPos>> bekend = bekend(guheinde);
        RegistryAccess access = guheinde.registryAccess();
        ChunkGeneratorStructureState state = guheinde.getChunkSource().getGeneratorState();
        long seed = guheinde.getSeed();
        // (who goes first is searched first, as the world does it: no search then waits inside another)
        List<String> namen = new ArrayList<>(Einde2Ids.LEEGTE_STRUCTUREN);
        namen.sort((a, b) -> Integer.compare(voorrang(access, b), voorrang(access, a)));
        Thread thread = new Thread(() -> {
            for (String structuur : namen) {
                try {
                    bekend.put(structuur, Optional.ofNullable(startChunk(access, state, seed, structuur)));
                } catch (RuntimeException ex) {
                    LOGGER.warn("Guhs: the spot of {} could not be worked out", structuur, ex);
                    bekend.put(structuur, Optional.empty());
                }
            }
        }, "Guhs einde2 plekken");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static int voorrang(RegistryAccess access, String structuur) {
        LeegteBouwStructure bouw = leegteBouw(access, structuur);
        return bouw == null ? 0 : bouw.voorrang();
    }

    /** (Level load, after the world registered its structure state: the spots of the Guheinde are worked out ahead.) */
    static void onLevelLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && nl.juiced.guhs.feature.guheinde.GuheindeFeature.isGuheinde(level)) {
            zoekVooruit(level);
        }
    }

    /**
     * The structure start of the guaranteed copy when it really stands in the world (null: no spot, not known yet, or the
     * copy is not there: its start chunk was made before einde2 came, which is what happens in terrain players had already
     * been to). Loads that one chunk as far as its structure starts (as /locate does). A copy whose start chunk is new while
     * chunks around it are old stands there cut off at those chunks: this cannot tell.
     * <p>
     * hook fase B: ask this (not {@link #doos}) before a player is sent to a place or it is offered as a destination.
     */
    @Nullable
    public static StructureStart start(ServerLevel guheinde, String structuur) {
        Structure structure = guheinde.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Guhs.id(structuur));
        ChunkPos chunk = structure == null ? null : startChunk(guheinde, structuur);
        if (chunk == null) {
            return null;
        }
        ChunkAccess daar = guheinde.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = guheinde.structureManager().getStartForStructure(SectionPos.bottomOf(daar), structure, daar);
        return start != null && start.isValid() ? start : null;
    }

    /** The same for a structure state that isn't a level's own (the game tests' Guheinde, see {@link Einde2Test}). */
    @Nullable
    public static ChunkPos startChunk(RegistryAccess access, @Nullable ChunkGeneratorStructureState state, long seed, String structuur) {
        LeegteBouwStructure bouw = leegteBouw(access, structuur);
        if (bouw != null && bouw.bij().isPresent()) {
            return startChunk(access, state, seed, bouw);
        }
        StructureSet set = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(structuur + "_gegarandeerd"));
        return set != null && set.placement() instanceof GegarandeerdPlacement g ? g.plek(state, seed).orElse(null) : null;
    }

    /** The one start chunk of a build that stands next to another one (null: no {@code bij}, or its neighbour has no spot). */
    @Nullable
    public static ChunkPos startChunk(RegistryAccess access, @Nullable ChunkGeneratorStructureState state, long seed, LeegteBouwStructure bouw) {
        if (bouw.bij().isEmpty()) {
            return null;
        }
        String naam = bouw.bij().get().structuur().getPath();
        LeegteBouwStructure buur = leegteBouw(access, naam);
        ChunkPos start = buur == null || buur.bij().isPresent() ? null : startChunk(access, state, seed, naam);
        return start == null ? null : bouw.bijChunk(buur, start);
    }

    /** The world position of template 0,0,0 of the guaranteed copy. */
    @Nullable
    public static BlockPos oorsprong(ServerLevel guheinde, String structuur) {
        LeegteBouwStructure bouw = leegteBouw(guheinde.registryAccess(), structuur);
        ChunkPos start = bouw == null ? null : startChunk(guheinde, structuur);
        return start == null ? null : bouw.oorsprong(start);
    }

    /** The whole build of the guaranteed copy (for the veil, the map, the ferry). */
    @Nullable
    public static BoundingBox doos(ServerLevel guheinde, String structuur) {
        LeegteBouwStructure bouw = leegteBouw(guheinde.registryAccess(), structuur);
        ChunkPos start = bouw == null ? null : startChunk(guheinde, structuur);
        return start == null ? null : bouw.doos(start);
    }

    /** The world position of a marker of the guaranteed copy: oorsprong + its pos. */
    @Nullable
    public static BlockPos wereld(ServerLevel guheinde, String structuur, String plek) {
        Plek p = plek(structuur, plek);
        BlockPos o = p == null ? null : oorsprong(guheinde, structuur);
        return o == null ? null : o.offset(p.pos());
    }

    /** Where players are set down at the guaranteed copy: the centre of its {@code aankomst} zone, on its floor. */
    @Nullable
    public static Vec3 aankomst(ServerLevel guheinde, String structuur) {
        Plek p = plek(structuur, "aankomst");
        BlockPos o = p == null ? null : oorsprong(guheinde, structuur);
        if (o == null) {
            return null;
        }
        BlockPos tot = p.tot() == null ? p.pos() : p.tot();
        return new Vec3(o.getX() + (p.pos().getX() + tot.getX()) / 2.0 + 0.5, o.getY() + Math.min(p.pos().getY(), tot.getY()),
                o.getZ() + (p.pos().getZ() + tot.getZ()) / 2.0 + 0.5);
    }

    // --- any copy: from a structure start ------------------------------------------------------------------------------------

    /**
     * The nearest copy of this structure with a piece within {@code straal} blocks of this spot (null: none). Only looks
     * in chunks that are loaded.
     */
    @Nullable
    public static StructureStart bij(ServerLevel level, String structuur, BlockPos pos, int straal) {
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Guhs.id(structuur));
        if (structure == null) {
            return null;
        }
        StructureStart best = null;
        long bestD = Long.MAX_VALUE;
        java.util.Set<StructureStart> gezien = new java.util.HashSet<>();
        for (int cx = (pos.getX() - straal) >> 4; cx <= (pos.getX() + straal) >> 4; cx++) {
            for (int cz = (pos.getZ() - straal) >> 4; cz <= (pos.getZ() + straal) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (StructureStart start : level.structureManager().startsForStructure(SectionPos.of(new ChunkPos(cx, cz), 0), structure)) {
                    if (!start.isValid() || !gezien.add(start)) {
                        continue;
                    }
                    for (StructurePiece piece : start.getPieces()) {
                        if (piece instanceof LeegteBouwStructure.Rand) {
                            continue;
                        }
                        long d = afstand(piece.getBoundingBox(), pos);
                        if (d <= (long) straal * straal && d < bestD) {
                            bestD = d;
                            best = start;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Squared distance from a spot to a box (0 inside it). */
    private static long afstand(BoundingBox b, BlockPos p) {
        long dx = Math.max(0, Math.max(b.minX() - p.getX(), p.getX() - b.maxX()));
        long dy = Math.max(0, Math.max(b.minY() - p.getY(), p.getY() - b.maxY()));
        long dz = Math.max(0, Math.max(b.minZ() - p.getZ(), p.getZ() - b.maxZ()));
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * The world position of a marker in this copy. A leegte_bouw: a plain shift. A jigsaw structure: the transform of
     * the piece made from the marker's {@code stuk} (the first such piece; null when this copy has none).
     */
    @Nullable
    public static BlockPos wereld(StructureStart start, Plek plek) {
        if (start.getStructure() instanceof LeegteBouwStructure bouw) {
            return bouw.oorsprong(start.getChunkPos()).offset(plek.pos());
        }
        if (plek.stuk() == null) {
            return null;
        }
        Identifier stuk = Guhs.id(plek.stuk());
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pool && stuk.equals(PleinSlot.template(piece))) {
                return pool.getPosition().offset(StructureTemplate.transform(plek.pos(), Mirror.NONE, pool.getRotation(), BlockPos.ZERO));
            }
        }
        return null;
    }

    /** Every world position of a marker in this copy (a jigsaw structure may have its piece more than once: the cells). */
    public static List<BlockPos> alle(StructureStart start, Plek plek) {
        if (start.getStructure() instanceof LeegteBouwStructure || plek.stuk() == null) {
            BlockPos een = wereld(start, plek);
            return een == null ? List.of() : List.of(een);
        }
        Identifier stuk = Guhs.id(plek.stuk());
        List<BlockPos> out = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pool && stuk.equals(PleinSlot.template(piece))) {
                out.add(pool.getPosition().offset(StructureTemplate.transform(plek.pos(), Mirror.NONE, pool.getRotation(), BlockPos.ZERO)));
            }
        }
        return out;
    }

    // --- overlay templates ---------------------------------------------------------------------------------------------------

    /** Puts an overlay template at the guaranteed copy (false: unknown, no copy, or its chunks aren't loaded). */
    public static boolean plaatsLaag(ServerLevel level, String structuur, String laag) {
        return laag(level, structuur, laag, false);
    }

    /** Takes an overlay away again: every block of it becomes air. */
    public static boolean ruimLaag(ServerLevel level, String structuur, String laag) {
        return laag(level, structuur, laag, true);
    }

    private static boolean laag(ServerLevel level, String structuur, String naam, boolean ruim) {
        Laag laag = blad(structuur).lagen().get(naam);
        BlockPos o = laag == null ? null : oorsprong(level, structuur);
        if (o == null) {
            return false;
        }
        return laag(level, laag, o.offset(laag.pos()), ruim);
    }

    /** The same at a given spot (the world position of the overlay's own 0,0,0): the game tests, and copies that aren't the guaranteed one. */
    public static boolean laag(ServerLevel level, Laag laag, BlockPos at, boolean ruim) {
        StructureTemplate template = level.getStructureManager().get(laag.template()).orElse(null);
        if (template == null || !level.hasChunksAt(at, at.offset(laag.grootte()))) {
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(ruim);
        if (ruim) {
            settings.addProcessor(LUCHT);
        }
        return template.placeInWorld(level, at, at, settings, level.getRandom(), 2);
    }

    /** Turns every block of a template into air (to take an overlay away). */
    private static final StructureProcessor LUCHT = new StructureProcessor() {
        @Override
        public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos targetPosition, BlockPos referencePos,
                                                                 StructureTemplate.StructureBlockInfo originalBlockInfo,
                                                                 StructureTemplate.StructureBlockInfo processedBlockInfo, StructurePlaceSettings settings) {
            return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), Blocks.AIR.defaultBlockState(), null);
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.NOP;
        }
    };
}
