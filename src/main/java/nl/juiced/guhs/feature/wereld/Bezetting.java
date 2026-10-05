package nl.juiced.guhs.feature.wereld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * bbq2: things that must also be at the copies of a structure that were generated long ago (the official server world is
 * never reset): the quest NPCs of this update at the existing Spiesburchten, Mika-grillpaleizen and big barbecueputten, and
 * small props next to them. The {@code feature/knuffeldal/Bewoners} pattern, for everyone.
 * <p>
 * An owner slice registers what belongs at a structure (from its Feature.register):
 * <ul>
 *   <li>{@link #npc}: a quest NPC at a spot of a template;</li>
 *   <li>{@link #wezen}: any other entity, made by the owner's {@link Maker};</li>
 *   <li>{@link #blokken}: a small prop template, at the first of a few spots that is free.</li>
 * </ul>
 * While a player is near a copy (looked at about every two seconds, only where the chunks and their entities are loaded):
 * <ul>
 *   <li>an NPC or wezen that is not there is made at its spot. It is recognised by its tag ({@link #TAG} in the entity's
 *       persistent data: {@code "<id>@<start chunk>"}, or just {@code "<id>"} on one that came with the template), and it has
 *       to be missing twice, {@link #BEVESTIG} ticks apart (entities load a little after their chunk). It can't be hurt,
 *       tamed or leashed and never despawns.</li>
 *   <li>a prop is placed ONCE per copy (remembered in SavedData {@code guhs:bezetting}), and only where every block of its
 *       volume is air or a plant (its bottom layer may also be natural ground: block tag {@code guhs:wereld/natuurlijk})
 *       without a block entity, with ground under at least half of it: never over something a player built. A spot is also
 *       tried one and two blocks higher and lower (the land is not flat). When no spot is free the copy goes without
 *       (logged).</li>
 * </ul>
 * Put the same NPC with the same tag in the template too ({@code wereld.npc(...)} in tools/features/wereld.py): then new
 * copies have it from worldgen and this only repairs.
 */
public final class Bezetting {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The key in an entity's persistent data (NeoForgeData) that says which registered inhabitant it is. */
    public static final String TAG = "guhs_bezetting";
    /** A player's surroundings are looked at once per this many ticks. */
    public static final int CHECK_TICKS = 40;
    /** An inhabitant has to be seen missing twice, this many ticks apart, before a new one comes. */
    public static final int BEVESTIG = 60;
    /** How near (blocks, to a piece of the copy) a player has to be. */
    public static final int BEREIK = 48;
    /** An inhabitant is looked for this far around its spot (NPCs sit still; a wezen may walk a little). */
    public static final int ZOEK_NPC = 12, ZOEK_WEZEN = 16;
    /** The ground and the plants a prop may replace. */
    public static final TagKey<Block> NATUURLIJK = TagKey.create(Registries.BLOCK, Guhs.id("wereld/natuurlijk"));

    /** Makes the entity of a {@link #wezen} registration (not yet in the world), or null when it can't be made now. */
    @FunctionalInterface
    public interface Maker {
        @Nullable
        Entity maak(ServerLevel level, Vec3 plek, Rotation draai);
    }

    private record Wezen(String id, String structuur, @Nullable String stuk, BlockPos lokaal, Maker maker, int zoek) {
    }

    private record Prop(String id, String structuur, @Nullable String stuk, List<BlockPos> keuzes, Identifier template) {
    }

    private static final Map<String, Wezen> WEZENS = new LinkedHashMap<>();
    private static final Map<String, Prop> PROPS = new LinkedHashMap<>();
    /** (not saved) dimension|id@chunk -> the game time it was first seen missing. */
    private static final Map<String, Long> GEMIST = new ConcurrentHashMap<>();

    private Bezetting() {
    }

    // --- registering -----------------------------------------------------------------------------------------------------

    /**
     * A quest NPC at every copy of {@code guhs:<structuur>}: at the block {@code lokaal} of the piece whose template name
     * contains {@code stuk} (null: the start piece; a guhs:burcht: coordinates of the whole build). {@code plek}: its
     * NpcRollen plek (RoleData guhs_plek), or null. {@code id} must be unique ("&lt;pkg&gt;_&lt;naam&gt;").
     */
    public static void npc(String id, String structuur, @Nullable String stuk, BlockPos lokaal, GuhNpcEntity.Kind kind, @Nullable String plek) {
        npc(id, structuur, stuk, lokaal, kind, plek, 0f);
    }

    /** Like {@link #npc(String, String, String, BlockPos, GuhNpcEntity.Kind, String)}, with the way it looks in the template (yaw, turned with the copy). */
    public static void npc(String id, String structuur, @Nullable String stuk, BlockPos lokaal, GuhNpcEntity.Kind kind, @Nullable String plek, float yaw) {
        zet(new Wezen(id, structuur, stuk, lokaal, (level, pos, draai) -> {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
            if (npc == null) {
                return null;
            }
            npc.setKind(kind);
            if (plek != null) {
                npc.roleData.putString(NpcRollen.PLEK, plek);
            }
            npc.setYRot(yaw);
            float y = npc.rotate(draai);
            npc.snapTo(pos.x, pos.y, pos.z, y, 0f);
            npc.setYBodyRot(y);
            npc.setYHeadRot(y);
            return npc;
        }, ZOEK_NPC));
    }

    /** Any entity at every copy of {@code guhs:<structuur>}, made by {@code maker} (see {@link #npc} for the other arguments). */
    public static void wezen(String id, String structuur, @Nullable String stuk, BlockPos lokaal, Maker maker) {
        zet(new Wezen(id, structuur, stuk, lokaal, maker, ZOEK_WEZEN));
    }

    /** Like {@link #wezen(String, String, String, BlockPos, Maker)}, for one that walks: it is looked for {@code zoek} blocks around its spot. */
    public static void wezen(String id, String structuur, @Nullable String stuk, BlockPos lokaal, Maker maker, int zoek) {
        zet(new Wezen(id, structuur, stuk, lokaal, maker, Math.max(4, zoek)));
    }

    /**
     * A small prop template at every copy of {@code guhs:<structuur>}: its corner (template 0,0,0) goes at the first of
     * {@code lokaalKeuzes} (template coordinates of the piece {@code stuk}, turned with the copy) where it fits; see the class
     * text for "fits". Placed once per copy.
     */
    public static void blokken(String id, String structuur, @Nullable String stuk, List<BlockPos> lokaalKeuzes, Identifier template) {
        synchronized (PROPS) {
            PROPS.put(id, new Prop(id, structuur, stuk, List.copyOf(lokaalKeuzes), template));
        }
    }

    private static void zet(Wezen w) {
        synchronized (WEZENS) {
            WEZENS.put(w.id, w);
        }
    }

    // --- where things are ------------------------------------------------------------------------------------------------

    /** The copy of {@code guhs:<structuur>} with a piece within {@link #BEREIK} blocks of this spot (the nearest; null: none loaded there). */
    @Nullable
    public static StructureStart start(ServerLevel level, String structuur, BlockPos bij) {
        Structure s = Kopieen.structuur(level, structuur);
        if (s == null) {
            return null;
        }
        StructureStart best = null;
        double bestD = Double.MAX_VALUE;
        for (StructureStart start : Kopieen.bij(level, s, bij, BEREIK)) {
            double d = start.getBoundingBox().getCenter().distSqr(bij);
            if (d < bestD) {
                bestD = d;
                best = start;
            }
        }
        return best;
    }

    /**
     * Template coordinates -> world, for jigsaw pieces, burcht tiles and the barbecueput ({@link Kopieen#wereld}). Null when
     * this copy has no piece {@code stuk}.
     */
    @Nullable
    public static BlockPos wereld(StructureStart start, @Nullable String stuk, BlockPos lokaal) {
        return Kopieen.wereld(start, stuk, lokaal);
    }

    /** The tag of the inhabitant {@code id} of this copy. */
    public static String tag(String id, StructureStart start) {
        return id + "@" + start.getChunkPos().pack();
    }

    /** Is this entity a registered inhabitant (made here, or come with a template)? */
    public static boolean isBezetting(Entity e) {
        return !e.getPersistentData().getStringOr(TAG, "").isEmpty();
    }

    // --- the check -------------------------------------------------------------------------------------------------------

    /** Every {@link #CHECK_TICKS} for a player: the copies around them get what they miss. */
    static void opSpelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % CHECK_TICKS == 0 && !p.isSpectator()) {
            controleer(p.level(), p.blockPosition());
        }
    }

    /** Looks at the copies around this spot now; returns how many entities and props were made. */
    public static int controleer(ServerLevel level, BlockPos bij) {
        List<Wezen> wezens;
        List<Prop> props;
        synchronized (WEZENS) {
            wezens = new ArrayList<>(WEZENS.values());
        }
        synchronized (PROPS) {
            props = new ArrayList<>(PROPS.values());
        }
        if (wezens.isEmpty() && props.isEmpty()) {
            return 0;
        }
        int gemaakt = 0;
        Map<String, List<StructureStart>> starts = new HashMap<>();
        for (Wezen w : wezens) {
            for (StructureStart start : starts.computeIfAbsent(w.structuur, n -> kopieen(level, n, bij))) {
                gemaakt += wezen(level, start, w) ? 1 : 0;
            }
        }
        for (Prop prop : props) {
            for (StructureStart start : starts.computeIfAbsent(prop.structuur, n -> kopieen(level, n, bij))) {
                gemaakt += prop(level, start, prop) ? 1 : 0;
            }
        }
        return gemaakt;
    }

    private static List<StructureStart> kopieen(ServerLevel level, String structuur, BlockPos bij) {
        Structure s = Kopieen.structuur(level, structuur);
        return s == null ? List.of() : Kopieen.bij(level, s, bij, BEREIK);
    }

    /** One inhabitant of one copy: there, or (seen missing twice) made now. True when it was made. */
    private static boolean wezen(ServerLevel level, StructureStart start, Wezen w) {
        BlockPos pos = Kopieen.wereld(start, w.stuk, w.lokaal);
        if (pos == null) {
            return false;   // (this copy has no such piece)
        }
        AABB zoek = new AABB(pos).inflate(w.zoek);
        if (!level.isPositionEntityTicking(pos) || !geladen(level, zoek)) {
            return false;
        }
        String tag = tag(w.id, start);
        String key = level.dimension().identifier() + "|" + tag;
        boolean er = false;
        for (Entity e : level.getEntitiesOfClass(Entity.class, zoek, Entity::isAlive)) {
            String t = e.getPersistentData().getStringOr(TAG, "");
            if (t.equals(tag) || t.equals(w.id)) {
                if (t.equals(w.id)) {
                    e.getPersistentData().putString(TAG, tag);   // (came with the template: it is this copy's from now on)
                }
                if (er) {
                    e.discard();   // (a double: the first one stays)
                    continue;
                }
                er = true;
                bescherm(e);
            }
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
        Vec3 plek = Vec3.atBottomCenterOf(pos);
        Entity e;
        try {
            e = w.maker.maak(level, plek, Kopieen.draai(start, w.stuk));
        } catch (RuntimeException ex) {
            LOGGER.warn("Guhs: could not make the inhabitant {} at {}", w.id, pos, ex);
            return false;
        }
        if (e == null) {
            return false;
        }
        e.getPersistentData().putString(TAG, tag);
        bescherm(e);
        if (!e.isAddedToLevel()) {
            level.addFreshEntity(e);
        }
        level.sendParticles(ParticleTypes.POOF, plek.x, plek.y + 0.6, plek.z, 8, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    /** An inhabitant can't be hurt or leashed and never despawns. */
    private static void bescherm(Entity e) {
        e.setInvulnerable(true);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
            if (mob.isLeashed()) {
                mob.dropLeash();
            }
        }
    }

    /** An inhabitant comes into the world (also one that came with a template): protected. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && isBezetting(event.getEntity())) {
            bescherm(event.getEntity());
        }
    }

    /** An inhabitant is never tamed away. */
    static void opTem(AnimalTameEvent event) {
        if (isBezetting(event.getAnimal())) {
            event.setCanceled(true);
        }
    }

    /** Are the chunks of this area loaded, with their entities? */
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

    // --- props -----------------------------------------------------------------------------------------------------------

    /** The prop of one copy: placed now when it was not tried yet and a spot is free. True when it was placed. */
    private static boolean prop(ServerLevel level, StructureStart start, Prop prop) {
        Geplaatst data = Geplaatst.get(level);
        String tag = tag(prop.id, start);
        if (data.plekken.containsKey(tag)) {
            return false;
        }
        Optional<StructureTemplate> template = level.getStructureManager().get(prop.template);
        if (template.isEmpty()) {
            return false;
        }
        Rotation draai = Kopieen.draai(start, prop.stuk);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(draai).setMirror(Mirror.NONE);
        boolean alles = true;
        for (BlockPos keuze : prop.keuzes) {
            BlockPos basis = Kopieen.wereld(start, prop.stuk, keuze);
            if (basis == null) {
                return false;   // (this copy has no such piece: nothing to remember, another copy may)
            }
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos hoek = basis.above(dy);
                BoundingBox box = template.get().getBoundingBox(settings, hoek);
                Boolean past = past(level, box);
                if (past == null) {
                    alles = false;   // (not loaded: look again later)
                    break;
                }
                if (past) {
                    template.get().placeInWorld(level, hoek, hoek, settings, level.getRandom(), Block.UPDATE_CLIENTS);
                    data.plekken.put(tag, hoek.asLong());
                    data.setDirty();
                    LOGGER.info("Guhs: placed {} at {} ({} {})", prop.template, hoek.toShortString(), prop.structuur, start.getChunkPos());
                    return true;
                }
            }
        }
        if (alles) {
            data.plekken.put(tag, GEEN);
            data.setDirty();
            LOGGER.info("Guhs: no free spot for {} at {} {}: this copy goes without", prop.template, prop.structuur, start.getChunkPos());
        }
        return false;
    }

    /**
     * May a prop take this box: every block air, a plant or natural ground without a block entity, and solid ground under at
     * least half of it? Null: not all of it is loaded.
     */
    @Nullable
    private static Boolean past(ServerLevel level, BoundingBox box) {
        if (box.minY() <= level.getMinY() || box.maxY() >= level.getMaxY()) {
            return false;
        }
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                if (!level.hasChunk(x, z)) {
                    return null;
                }
            }
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int kolommen = 0, grond = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    BlockState state = level.getBlockState(at.set(x, y, z));
                    // (natural ground only in the bottom layer: a wall of pink wool is a player's, a floor of it is the land)
                    if (!(y == box.minY() ? vrij(state) : leeg(state)) || level.getBlockEntity(at) != null) {
                        return false;
                    }
                }
                kolommen++;
                BlockState onder = level.getBlockState(at.set(x, box.minY() - 1, z));
                grond += onder.isSolidRender() || onder.is(NATUURLIJK) && !onder.canBeReplaced() ? 1 : 0;
            }
        }
        if (Bescherming.beschermd(level, new BlockPos(box.minX(), box.minY(), box.minZ())) || Bescherming.beschermd(level, new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) {
            return false;   // (inside another quest building)
        }
        return grond * 2 >= kolommen;
    }

    /** Air, a plant or natural ground: what the bottom layer of a prop may replace (never a fluid, never something built). */
    public static boolean vrij(BlockState state) {
        return leeg(state) || (state.is(NATUURLIJK) && state.getFluidState().isEmpty());
    }

    /** Air or a plant (something that gives way when you build there; no fluid, no fire): what the rest of a prop may replace. */
    public static boolean leeg(BlockState state) {
        return state.isAir() || (state.getFluidState().isEmpty() && state.canBeReplaced() && !state.is(BlockTags.FIRE));
    }

    /** "No spot was free" in {@link Geplaatst}. */
    private static final long GEEN = Long.MIN_VALUE;

    /** SavedData guhs:bezetting (per dimension): which props were placed (or found no spot) at which copy. */
    public static class Geplaatst extends SavedData {
        public static final SavedDataType<Geplaatst> TYPE = GuhSavedData.tagType("bezetting", Geplaatst::new, Geplaatst::load, Geplaatst::save);
        private final Map<String, Long> plekken = new HashMap<>();

        public static Geplaatst get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        /** Where the prop {@code id} of this copy was placed (empty: not yet, or no spot was free). */
        public Optional<BlockPos> plek(String id, StructureStart start) {
            Long p = plekken.get(tag(id, start));
            return p == null || p == GEEN ? Optional.empty() : Optional.of(BlockPos.of(p));
        }

        /** Was the prop {@code id} of this copy handled already (placed, or no spot)? */
        public boolean gehad(String id, StructureStart start) {
            return plekken.containsKey(tag(id, start));
        }

        /** (tests) as if the prop {@code id} of this copy was never handled. */
        public void vergeet(String id, StructureStart start) {
            if (plekken.remove(tag(id, start)) != null) {
                setDirty();
            }
        }

        /** (tests, dev) as if nothing was ever placed here. */
        public void wis() {
            plekken.clear();
            setDirty();
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            CompoundTag p = new CompoundTag();
            plekken.forEach(p::putLong);
            tag.put("Plekken", p);
            return tag;
        }

        private static Geplaatst load(CompoundTag tag) {
            Geplaatst g = new Geplaatst();
            CompoundTag p = tag.getCompoundOrEmpty("Plekken");
            for (String key : p.keySet()) {
                g.plekken.put(key, p.getLongOr(key, GEEN));
            }
            return g;
        }
    }

    // --- game tests ------------------------------------------------------------------------------------------------------

    /** (Tests) forget a registration and what was seen missing. */
    public static void vergeet(String id) {
        synchronized (WEZENS) {
            WEZENS.remove(id);
        }
        synchronized (PROPS) {
            PROPS.remove(id);
        }
        GEMIST.keySet().removeIf(k -> k.contains("|" + id + "@"));
    }

    /** (dev command, tests) every inhabitant of this level that was seen missing counts as missing long enough. */
    public static void bevestigAlles(ServerLevel level) {
        String dim = level.dimension().identifier() + "|";
        GEMIST.replaceAll((k, v) -> k.startsWith(dim) ? v - BEVESTIG : v);
    }

    /** (Tests) pretends the inhabitant {@code id} of this copy was first seen missing this many ticks ago. */
    public static void zetGemist(ServerLevel level, String id, StructureStart start, long ticksGeleden) {
        GEMIST.put(level.dimension().identifier() + "|" + tag(id, start), level.getGameTime() - ticksGeleden);
    }
}
