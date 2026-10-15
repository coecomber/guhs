package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2: the static residents of the big places and the Mika-vesting (CONTRACT_E2 11.7 M1 and 11.9). bbq2's
 * {@code feature/wereld/Bezetting} in small, with the SAME constants and the SAME tag: at the merge onto bbq2 this becomes a
 * loop of {@code Bezetting.npc(...)} / {@code Bezetting.wezen(...)} calls, and every resident that already stands is
 * recognised by its tag.
 * <p>
 * The source is the sidecar of each structure ({@link Plekken#van}): every {@code npc} marker gets a resident (a
 * {@link GuhNpcEntity.Kind} of the marker's kind name, one of the six creatures of cast_schurk whose marker name is its
 * entity id, or the entity type the marker names), except the story moments, visitors and doubles of the table in 11.7, and
 * a few prop markers get a creature ({@link #tabel}). While a player is within {@link #BEREIK} blocks of a copy (any copy:
 * the guaranteed one, a rare random one, every Mika-vesting), every {@link #CHECK_TICKS} ticks: a resident that is missing
 * twice, {@link #BEVESTIG} ticks apart, is made at its spot (looking the marker's way, turned with a jigsaw piece). It can't
 * be hurt, leashed or tamed and never despawns; a double is discarded. Nothing else: no story, no state per player, no
 * path finding, no chunk loading.
 * <p>
 * The tag: persistent data {@value #TAG} = {@code einde2_<structuur>_<plek>@<start chunk>} ({@code Bezetting.tag}). A
 * {@link GuhNpcEntity} gets {@code roleData} {@link NpcRollen#PLEK} = {@code <structuur>/<plek>}, so
 * {@code NpcRollen.zet(kind, "<structuur>/<plek>", rol)} is its role (hook fase B); a creature gets the same string in its
 * persistent data under {@link #PLEK}, is told to stand still and gets its look ({@code Soort}).
 * Residents come for everybody: on this branch nothing is hidden yet (hook fase B: the veil, {@code Zicht.alleenBij}).
 */
public final class Bewoners {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The key in an entity's persistent data (NeoForgeData) that says which resident it is (bbq2's Bezetting.TAG). */
    public static final String TAG = "guhs_bezetting";
    /** The spot's name in a creature's persistent data (a GuhNpcEntity has it in its roleData: NpcRollen.PLEK). */
    public static final String PLEK = NpcRollen.PLEK;
    /** A player's surroundings are looked at once per this many ticks. */
    public static final int CHECK_TICKS = 40;
    /** A resident has to be seen missing twice, this many ticks apart, before a new one comes. */
    public static final int BEVESTIG = 60;
    /** How near (blocks, to a piece of the copy) a player has to be. */
    public static final int BEREIK = 48;
    /** A resident is looked for this far around its spot (12; 16 for a creature). */
    public static final int ZOEK_NPC = 12, ZOEK_WEZEN = 16;
    /** The six creature names a marker uses as its kind (CONTRACT_E2 11.2 A): the entity id is the name in lower case. */
    public static final Set<String> WEZEN_NAMEN = Set.of("DRAAKGUH", "BOSCO", "WAN_SHI_GUH", "VOSGUH", "LEEUWSCHILDPAD_GUH", "FLOPSIE");

    /**
     * One resident spot. {@code kind}: a GuhNpcEntity of that kind, else {@code entity}: a creature of that type;
     * {@code soort}: its look (-1: its own); {@code stil}: stays on its mark; {@code honk}: how far it strays; {@code houding}:
     * the lasting animation of a CastWezen; {@code yaw}: the way it looks in template coordinates; {@code zoek}: how far
     * around the spot it is looked for.
     */
    public record Bewoner(String structuur, String plek, @Nullable GuhNpcEntity.Kind kind, @Nullable Identifier entity, int soort,
                          boolean stil, int honk, @Nullable String houding, float yaw, int zoek) {
        /** The id of the registration (bbq2: {@code Bezetting.npc(id, ...)}). */
        public String id() {
            return "einde2_" + structuur + "_" + plek;
        }

        /** {@code <structuur>/<plek>}: the spot a phase B role is registered for. */
        public String naam() {
            return structuur + "/" + plek;
        }
    }

    // --- the table of 11.7 ---------------------------------------------------------------------------------------------------

    /** Spots that are not a resident: story moments, visitors, doubles (besides every knipoog_* and team_*). */
    private static final Set<String> NIET = Set.of(
            "zuidelijke_luchttempel/momo",
            "vuurnatie_hoofdstad/mikai_bunker", "vuurnatie_hoofdstad/leeuwschildpad",
            "leegtehaven/mika_kok",
            "leeuwschildpad_eiland/leeuwschildpad",
            "bikinibodem/neptuguh", "bikinibodem/plan_z_plankmika", "bikinibodem/ijs_sponsguh", "bikinibodem/ijs_patguh",
            "bikinibodem/podium_octo", "bikinibodem/podium_sponsguh", "bikinibodem/octoliam", "bikinibodem/sponsguh", "bikinibodem/octo",
            "bikinibodem/kwallen_patguh", "bikinibodem/spookkapitein",
            "schelpenstad/mindy", "schelpenstad/mindy_strand", "schelpenstad/dennis");
    /** The fanfare of Bikinibodem ({@code band_*}) plays only in its story moment. */
    private static final String NIET_BAND = "bikinibodem/band_";
    /** Entity markers that are not a resident (11.7 "all"): the ordinary Mika's and guhs some templates mark. */
    private static final Set<String> NIET_ENTITY = Set.of("guhs:mika", "guhs:guh");

    /** A resident whose look or type is not the marker's own (11.7 and 11.9), and the prop markers that get one. */
    private record Anders(@Nullable GuhNpcEntity.Kind kind, @Nullable String entity, int soort, boolean stil, int honk,
                          @Nullable String houding, @Nullable Float yaw) {
        static Anders kind(GuhNpcEntity.Kind kind) {
            return new Anders(kind, null, -1, true, 0, null, null);
        }

        static Anders wezen(String entity, int soort) {
            return new Anders(null, entity, soort, true, 0, null, null);
        }
    }

    private static final Map<String, Anders> ANDERS = new LinkedHashMap<>();

    static {
        ANDERS.put("noordelijke_waterstam/vis_maan", new Anders(null, "guhs:maanvis", -1, false, 0, null, null));
        ANDERS.put("noordelijke_waterstam/vis_oceaan", new Anders(null, "guhs:oceaanvis", -1, false, 0, null, null));
        ANDERS.put("vuurnatie_hoofdstad/suki", Anders.kind(GuhNpcEntity.Kind.SUKI_GUH_GEVANGENE));
        ANDERS.put("vuurnatie_hoofdstad/miroh_tent", Anders.kind(GuhNpcEntity.Kind.OOM_MIROH_LOTUS));
        ANDERS.put("vuurnatie_hoofdstad/acteur_1", Anders.kind(GuhNpcEntity.Kind.TONEELSPELER_GUH));
        ANDERS.put("vuurnatie_hoofdstad/acteur_2", Anders.kind(GuhNpcEntity.Kind.TONEELSPELER_GUH_KATARA));
        ANDERS.put("vuurnatie_hoofdstad/acteur_3", Anders.kind(GuhNpcEntity.Kind.TONEELSPELER_GUH_SOKKA));
        ANDERS.put("vuurnatie_hoofdstad/acteur_4", Anders.kind(GuhNpcEntity.Kind.TONEELSPELER_GUH_TOPH));
        ANDERS.put("vuurnatie_hoofdstad/acteur_5", Anders.kind(GuhNpcEntity.Kind.TONEELSPELER_GUH_ZUKA));
        ANDERS.put("vuurnatie_hoofdstad/draak_rood", Anders.wezen("guhs:draakguh", 0));
        ANDERS.put("vuurnatie_hoofdstad/draak_blauw", Anders.wezen("guhs:draakguh", 1));
        // 11.9: the prince in his old room in the east wing (a zone: he stands on its floor in the middle, looking west, to the door)
        ANDERS.put("vuurnatie_hoofdstad/kamer_zuka", new Anders(GuhNpcEntity.Kind.ZUKA_PRINS, null, -1, true, 0, null, 90f));
        for (int i = 1; i <= 5; i++) {
            ANDERS.put("westelijke_luchttempel/kudde_" + i, new Anders(null, "guhs:luchtbizon_guh", i - 1, true, 0, "vlieg", null));
        }
        for (int i = 1; i <= 4; i++) {
            ANDERS.put("kyoshi_dorp/bewoner_" + i, Anders.kind(GuhNpcEntity.Kind.KYOSHI_DORPSGUH));
        }
        for (int i = 1; i <= 3; i++) {
            ANDERS.put("bibliotheek_wan_shi_guh/vosguh_" + i, new Anders(null, "guhs:vosguh", -1, false, 6, null, null));
        }
        ANDERS.put("leeuwschildpad_eiland/kop", new Anders(null, "guhs:leeuwschildpad_guh", 0, true, 0, null, 0f));
    }

    // <einde2_geestenwereld> (CONTRACT_E2 10, [geestenwereld]: a build at a fixed spot of a dimension of its own)
    /** A build at a fixed spot of its own dimension (no structure): the name of its sidecar, its dimension, its template 0,0,0. */
    public record Vast(String structuur, ResourceKey<Level> dim, BlockPos oorsprong) {
    }

    private static final Map<String, Vast> VAST = new ConcurrentHashMap<>();
    private static volatile Map<String, List<Bewoner>> vastTabel;

    /** (a slice's register) The residents of this build come from its sidecar, at {@code oorsprong + marker}, in {@code dim}. */
    public static void vast(String structuur, ResourceKey<Level> dim, BlockPos oorsprong) {
        VAST.put(structuur, new Vast(structuur, dim, oorsprong));
        vastTabel = null;
    }

    /** (a slice's register) The creature of the marker {@code <structuur>/<plek>} with this look, still or walking about its spot. */
    public static void anders(String naam, String entity, int soort, boolean stil, int honk) {
        ANDERS.put(naam, new Anders(null, entity, soort, stil, honk, null, null));
        tabel = null;
        vastTabel = null;
    }

    /** The residents of the builds at a fixed spot ({@link #vast}), per build (apart from {@link #tabel}: those are structures). */
    public static Map<String, List<Bewoner>> vasteTabel() {
        Map<String, List<Bewoner>> t = vastTabel;
        if (t == null) {
            Map<String, List<Bewoner>> nieuw = new LinkedHashMap<>();
            for (String structuur : VAST.keySet()) {
                List<Bewoner> rij = new ArrayList<>();
                for (Plekken.Plek p : Plekken.van(structuur).values()) {
                    Bewoner b = bewoner(structuur, p);
                    if (b != null && (b.entity() == null || BuiltInRegistries.ENTITY_TYPE.containsKey(b.entity()))) {
                        rij.add(b);
                    } else if (b != null && GEMELD.add(b.naam())) {
                        LOGGER.info("Guhs einde2: the marker {} names {}: not there (yet), no resident", b.naam(), b.entity());
                    }
                }
                nieuw.put(structuur, List.copyOf(rij));
            }
            vastTabel = t = Collections.unmodifiableMap(nieuw);
        }
        return t;
    }

    /** The residents of the fixed builds of this level near this spot; returns how many were made. */
    public static int controleerVast(ServerLevel level, BlockPos bij) {
        int gemaakt = 0;
        for (Map.Entry<String, List<Bewoner>> e : vasteTabel().entrySet()) {
            Vast v = VAST.get(e.getKey());
            if (v == null || v.dim() != level.dimension()) {
                continue;
            }
            ChunkPos start = ChunkPos.containing(v.oorsprong());
            for (Bewoner b : e.getValue()) {
                BlockPos pos = v.oorsprong().offset(lokaal(Plekken.plek(b.structuur(), b.plek())));
                if (pos.distSqr(bij) <= (double) (BEREIK + b.zoek()) * (BEREIK + b.zoek())) {
                    gemaakt += plek(level, tag(b.id(), start), pos, Rotation.NONE, b) ? 1 : 0;
                }
            }
        }
        return gemaakt;
    }

    private static boolean inVast(ServerLevel level) {
        for (Vast v : VAST.values()) {
            if (v.dim() == level.dimension()) {
                return true;
            }
        }
        return false;
    }
    // </einde2_geestenwereld>

    /** Every structure whose markers are read: the fourteen big places and the Mika-vesting. */
    public static final List<String> STRUCTUREN;

    static {
        List<String> s = new ArrayList<>(Einde2Ids.LEEGTE_STRUCTUREN);
        s.add(Einde2Ids.MIKA_VESTING);
        STRUCTUREN = List.copyOf(s);
    }

    private static volatile Map<String, List<Bewoner>> tabel;
    private static final Set<String> GEMELD = ConcurrentHashMap.newKeySet();

    /** Every resident spot, per structure (worked out once from the sidecars). */
    public static Map<String, List<Bewoner>> tabel() {
        Map<String, List<Bewoner>> t = tabel;
        if (t == null) {
            Map<String, List<Bewoner>> nieuw = new LinkedHashMap<>();
            for (String structuur : STRUCTUREN) {
                List<Bewoner> rij = new ArrayList<>();
                for (Plekken.Plek p : Plekken.van(structuur).values()) {
                    Bewoner b = bewoner(structuur, p);
                    if (b != null) {
                        rij.add(b);
                    }
                }
                nieuw.put(structuur, List.copyOf(rij));
            }
            tabel = t = Collections.unmodifiableMap(nieuw);
        }
        return t;
    }

    /** Every resident spot of all structures. */
    public static List<Bewoner> alle() {
        List<Bewoner> alle = new ArrayList<>();
        tabel().values().forEach(alle::addAll);
        return alle;
    }

    /** The resident of one marker, or null (not a resident, or a name that is neither a kind nor a creature: logged once). */
    @Nullable
    static Bewoner bewoner(String structuur, Plekken.Plek p) {
        String naam = structuur + "/" + p.id();
        Anders anders = ANDERS.get(naam);
        if (anders != null) {
            Identifier entity = anders.entity == null ? null : Identifier.parse(anders.entity);
            return new Bewoner(structuur, p.id(), anders.kind, entity, anders.soort, anders.stil, anders.honk, anders.houding,
                    anders.yaw != null ? anders.yaw : p.yaw(), anders.kind != null ? ZOEK_NPC : ZOEK_WEZEN);
        }
        if (!p.soort().equals(Plekken.NPC) || p.id().startsWith("knipoog_") || p.id().startsWith("team_") || NIET.contains(naam)
                || naam.startsWith(NIET_BAND)) {
            return null;
        }
        if (p.kind() != null) {
            String kind = p.kind().toUpperCase(Locale.ROOT);
            if (WEZEN_NAMEN.contains(kind)) {
                return new Bewoner(structuur, p.id(), null, Guhs.id(kind.toLowerCase(Locale.ROOT)), -1, true, 0, null, p.yaw(), ZOEK_WEZEN);
            }
            for (GuhNpcEntity.Kind k : GuhNpcEntity.Kind.values()) {
                if (k.name().equals(kind)) {
                    return new Bewoner(structuur, p.id(), k, null, -1, true, 0, null, p.yaw(), ZOEK_NPC);
                }
            }
        } else if (p.entity() != null && !NIET_ENTITY.contains(p.entity())) {
            Identifier id = Identifier.tryParse(p.entity());
            if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return new Bewoner(structuur, p.id(), null, id, -1, true, 0, null, p.yaw(), ZOEK_WEZEN);
            }
        }
        if (p.kind() != null || (p.entity() != null && !NIET_ENTITY.contains(p.entity()))) {
            if (GEMELD.add(naam)) {
                LOGGER.info("Guhs einde2: the marker {} names {} {}: no resident there", naam, p.kind(), p.entity());
            }
        }
        return null;
    }

    // --- registering ---------------------------------------------------------------------------------------------------------

    /** (Einde2.register) The check per player, and protection for whatever carries the tag. */
    static void register() {
        NeoForge.EVENT_BUS.addListener(Bewoners::opSpelerTick);
        NeoForge.EVENT_BUS.addListener(Bewoners::opJoin);
        NeoForge.EVENT_BUS.addListener(Bewoners::opTem);
    }

    // --- the check -----------------------------------------------------------------------------------------------------------

    /** (not saved) dimension|tag -> the game time it was first seen missing. */
    private static final Map<String, Long> GEMIST = new ConcurrentHashMap<>();

    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % CHECK_TICKS == 0 && !p.isSpectator()
                && GuheindeFeature.isGuheinde(p.level())) {
            controleer(p.level(), p.blockPosition());
        } else if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % CHECK_TICKS == 0 && !p.isSpectator()
                && inVast(p.level())) {
            controleerVast(p.level(), p.blockPosition());      // <einde2_geestenwereld/>
        }
    }

    /** Looks at the copies around this spot now; returns how many residents were made. */
    public static int controleer(ServerLevel level, BlockPos bij) {
        int gemaakt = 0;
        for (Map.Entry<String, List<Bewoner>> e : tabel().entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }
            StructureStart start = Plekken.bij(level, e.getKey(), bij, BEREIK);
            if (start == null) {
                continue;
            }
            for (Bewoner b : e.getValue()) {
                List<Spot> spots = spots(start, b);
                for (int i = 0; i < spots.size(); i++) {
                    String id = i == 0 ? b.id() : b.id() + "_" + i;     // (a vesting may have a piece more than once)
                    gemaakt += plek(level, tag(id, start.getChunkPos()), spots.get(i).pos(), spots.get(i).draai(), b) ? 1 : 0;
                }
            }
        }
        return gemaakt;
    }

    /** The tag of the resident {@code id} of the copy with this start chunk ({@code Bezetting.tag}). */
    public static String tag(String id, ChunkPos start) {
        return id + "@" + start.pack();
    }

    /** A world spot of a marker in a copy, with the turn of its piece. */
    record Spot(BlockPos pos, Rotation draai) {
    }

    /** Every world spot of a resident in this copy: a leegte_bouw has it once, a jigsaw copy once per piece it is in. */
    static List<Spot> spots(StructureStart start, Bewoner b) {
        Plekken.Plek p = Plekken.plek(b.structuur(), b.plek());
        if (p == null) {
            return List.of();
        }
        BlockPos lokaal = lokaal(p);
        if (start.getStructure() instanceof LeegteBouwStructure bouw) {
            return List.of(new Spot(bouw.oorsprong(start.getChunkPos()).offset(lokaal), Rotation.NONE));
        }
        if (p.stuk() == null) {
            return List.of();
        }
        Identifier stuk = Guhs.id(p.stuk());
        List<Spot> uit = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pool && stuk.equals(PleinSlot.template(piece))) {
                uit.add(new Spot(pool.getPosition().offset(StructureTemplate.transform(lokaal, Mirror.NONE, pool.getRotation(), BlockPos.ZERO)),
                        pool.getRotation()));
            }
        }
        return uit;
    }

    /** Where in the template the resident stands: the marker's block, or for a zone the middle of its floor. */
    static BlockPos lokaal(Plekken.Plek p) {
        if (p.soort().equals(Plekken.ZONE) && p.tot() != null) {
            BlockPos a = p.pos(), b = p.tot();
            return new BlockPos((a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()), (a.getZ() + b.getZ()) / 2);
        }
        return p.pos();
    }

    /**
     * One resident at one spot of one copy: there (a double is discarded), or, seen missing twice {@link #BEVESTIG} ticks
     * apart, made now. True when it was made. Only where the spot is entity-ticking and the entities around it are loaded.
     */
    static boolean plek(ServerLevel level, String tag, BlockPos pos, Rotation draai, Bewoner b) {
        AABB zoek = new AABB(pos).inflate(b.zoek());
        if (!level.isPositionEntityTicking(pos) || !geladen(level, zoek)) {
            return false;
        }
        String key = level.dimension().identifier() + "|" + tag;
        boolean er = false;
        for (Entity e : level.getEntitiesOfClass(Entity.class, zoek, Entity::isAlive)) {
            if (!e.getPersistentData().getStringOr(TAG, "").equals(tag)) {
                continue;
            }
            if (er) {
                e.discard();                                            // (a double: the first one stays)
                continue;
            }
            er = true;
            bescherm(e);
        }
        long nu = level.getGameTime();
        if (er) {
            GEMIST.remove(key);
            return false;
        }
        Long eerst = GEMIST.putIfAbsent(key, nu);
        if (eerst == null || nu - eerst < BEVESTIG) {
            return false;
        }
        GEMIST.remove(key);
        Entity e;
        try {
            e = maak(level, Vec3.atBottomCenterOf(pos), draai, b);
        } catch (RuntimeException ex) {
            LOGGER.warn("Guhs einde2: could not make the resident {} at {}", b.naam(), pos, ex);
            return false;
        }
        if (e == null) {
            return false;
        }
        e.getPersistentData().putString(TAG, tag);
        bescherm(e);
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.6, e.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    /** Makes the resident (not yet in the world), looking the marker's way turned with its piece; null when it can't be made. */
    @Nullable
    public static Entity maak(ServerLevel level, Vec3 plek, Rotation draai, Bewoner b) {
        if (b.kind() != null) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
            if (npc == null) {
                return null;
            }
            npc.setKind(b.kind());
            npc.roleData.putString(NpcRollen.PLEK, b.naam());
            zet(npc, plek, b.yaw(), draai);
            return npc;
        }
        EntityType<?> type = b.entity() == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(b.entity());
        Entity e = type == null ? null : type.create(level, EntitySpawnReason.STRUCTURE);
        if (e == null) {
            return null;
        }
        zet(e, plek, b.yaw(), draai);
        e.getPersistentData().putString(PLEK, b.naam());
        if (e instanceof CastWezen w) {
            if (b.soort() >= 0) {
                w.zetSoort(b.soort());
            }
            w.zetStil(b.stil());
            if (b.honk() > 0) {
                w.zetHonk(b.honk());
            }
            if (b.houding() != null) {
                w.zetAnimatie(b.houding());
            }
        } else if (e instanceof SponsguhWezen w) {
            if (b.soort() >= 0) {
                w.zetSoort(b.soort());
            }
            w.zetStil(b.stil());
        } else if (b.soort() >= 0 || b.honk() > 0 || b.houding() != null) {
            // <einde2_geestenwereld> a creature class this rule does not know (the spirits): its look and its spot as NBT
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
            nbt.putString("id", b.entity().toString());
            if (b.soort() >= 0) {
                nbt.putInt("Soort", b.soort());
            }
            nbt.putBoolean("Stil", b.stil());
            if (b.honk() > 0) {
                nbt.putInt("Honk", b.honk());
            }
            if (b.houding() != null) {
                nbt.putString("Houding", b.houding());
            }
            Entity geladen = EntityType.loadEntityRecursive(nbt, level, EntitySpawnReason.STRUCTURE, x -> x);
            if (geladen != null) {
                e.discard();
                e = geladen;
                zet(e, plek, b.yaw(), draai);
                e.getPersistentData().putString(PLEK, b.naam());
            }
            // </einde2_geestenwereld>
        }
        return e;
    }

    private static void zet(Entity e, Vec3 plek, float yaw, Rotation draai) {
        e.setYRot(yaw);
        float y = e.rotate(draai);
        e.snapTo(plek.x, plek.y, plek.z, y, 0f);
        e.setYHeadRot(y);
        e.setYBodyRot(y);
    }

    private static void bescherm(Entity e) {
        e.setInvulnerable(true);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
            if (mob.isLeashed()) {
                mob.dropLeash();
            }
        }
    }

    /** Is this entity a resident (it carries the tag)? */
    public static boolean isBewoner(Entity e) {
        return !e.getPersistentData().getStringOr(TAG, "").isEmpty();
    }

    private static void opJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && isBewoner(event.getEntity())) {
            bescherm(event.getEntity());
        }
    }

    private static void opTem(AnimalTameEvent event) {
        if (isBewoner(event.getAnimal())) {
            event.setCanceled(true);
        }
    }

    /** Are the chunks of this area loaded, with their entities? ({@code Bezetting.geladen}) */
    public static boolean geladen(ServerLevel level, AABB area) {
        int x0 = (int) Math.floor(area.minX) >> 4, x1 = (int) Math.floor(area.maxX) >> 4;
        int z0 = (int) Math.floor(area.minZ) >> 4, z1 = (int) Math.floor(area.maxZ) >> 4;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!level.hasChunk(x, z) || !level.areEntitiesLoaded(ChunkPos.pack(x, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private Bewoners() {
    }
}
