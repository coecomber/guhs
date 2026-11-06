package nl.juiced.guhs.feature.vadsrijk.dubbel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-DUBBEL: one of every unique thing (boss, area leader, NPC, airship, kite, pet...). The playtest showed Kristalwacht Splinter
 * twice on one spot and three Kruinroofvogels in one arena. Root causes (vanilla 26.1 entity storage, see NOTITIES_U_DUBBEL.md):
 * <ol>
 * <li><b>Ghosts</b>: an entity whose {@code shouldBeSaved()} is false (all our code-spawned NPCs, airships, kites, props...) is
 * never removed when its chunk unloads: vanilla's {@code PersistentEntitySectionManager.storeChunkSections} only unloads the
 * entities it saves. It stays hidden in memory and comes back when the chunk loads again, next to the copy the manager spawned
 * in the meantime. {@link #verborgen} queues such an entity the moment it goes hidden and {@link #tick} discards it.</li>
 * <li><b>The load window</b>: a chunk is "loaded" ({@code isLoaded}) before its entities are visible again (saved entities load
 * from disk asynchronously; the visibility switch is scheduled a tick later). A manager that looks in that window sees nothing
 * and spawns a second boss. {@link #klaar} is the test a manager must use before it concludes "missing".</li>
 * <li><b>No uniqueness key</b>: nothing tied a spawned boss/NPC to "the one of arena x". {@link #merk} gives it a stable key
 * (an entity tag, so it is saved with the entity); {@link #binnen} keeps one per key per level: a copy loaded from disk while
 * the key is alive is refused, a fresh spawn replaces a leftover; {@link #zoek} finds the live one.</li>
 * </ol>
 * Dev: {@code /guhs vadsrijk dubbel scan | opruimen | stats}.
 */
public final class Dubbel {
    private static final Logger LOG = LogUtils.getLogger();
    /** The entity tag prefix of a unique key ("guhs_vr_uniek=arena:zone3:kruinroofvogel"). */
    public static final String PREFIX = "guhs_vr_uniek=";

    /** Per level: key -> the entity that is "the one". */
    private static final Map<ServerLevel, Map<String, Entity>> EIGENAAR = new WeakHashMap<>();
    /** Unsaved entities that just went hidden (their chunk unloads): discarded at the end of the tick. */
    private static final List<Entity> GEESTEN = new ArrayList<>();

    /** Counters since server start (dev: stats). */
    public static int geweigerd, vervangen, geestenWeg, opgeruimd;

    private Dubbel() {
    }

    // ---- the API for managers --------------------------------------------------------------------------------------------------

    /**
     * May a manager judge "is my entity there?" at this spot now: the chunk's entities are loaded from disk AND visible
     * (entity ticking). False in the window between "chunk loaded" and "its entities are back" — spawning then makes a double.
     */
    public static boolean klaar(ServerLevel sl, BlockPos pos) {
        return sl.areEntitiesActuallyLoadedAndTicking(ChunkPos.containing(pos));
    }

    /** Gives this entity its unique key (call before addFreshEntity). Saved with the entity (a tag). */
    public static <E extends Entity> E merk(E e, String sleutel) {
        if (e.entityTags().contains(PREFIX + sleutel)) {
            return e;
        }
        e.entityTags().removeIf(t -> t.startsWith(PREFIX));
        e.addTag(PREFIX + sleutel);
        // already in the world (a manager found an old untagged one): it becomes the owner of the key unless a live one is
        if (e.level() instanceof ServerLevel sl && !e.isRemoved() && sl.getEntity(e.getUUID()) == e) {
            Map<String, Entity> m = index(sl);
            Entity oud = m.get(sleutel);
            if (oud == null || oud.isRemoved()) {
                m.put(sleutel, e);
            }
        }
        return e;
    }

    /**
     * The unique key of an entity, or null. Its tag first; for entities saved before U-DUBBEL (no tag) derived: a boss of a
     * registered arena -> "arena:&lt;id&gt;", a mob of a spawner that keeps only one (area leaders) -> "spawner:&lt;id&gt;".
     */
    @Nullable
    public static String sleutel(Entity e) {
        for (String t : e.entityTags()) {
            if (t.startsWith(PREFIX)) {
                return t.substring(PREFIX.length());
            }
        }
        if (e instanceof VadsrijkBaas b && !b.arenaId.isEmpty() && Arenas.van(b.arenaId) != null) {
            return "arena:" + b.arenaId;
        }
        if (e instanceof VadsrijkMob m && !m.spawner.isEmpty()) {
            Spawners.Spawner s = Spawners.van(m.spawner);
            if (s != null && s.max() == 1) {
                return "spawner:" + s.id();
            }
        }
        return null;
    }

    /** The live, visible entity with this key in this level, or null. */
    @Nullable
    public static Entity zoek(ServerLevel sl, String sleutel) {
        Map<String, Entity> m = EIGENAAR.get(sl);
        Entity e = m == null ? null : m.get(sleutel);
        return e != null && zichtbaar(sl, e) ? e : null;
    }

    private static boolean zichtbaar(ServerLevel sl, Entity e) {
        return !e.isRemoved() && sl.getEntity(e.getUUID()) == e;
    }

    private static Map<String, Entity> index(ServerLevel sl) {
        return EIGENAAR.computeIfAbsent(sl, k -> new HashMap<>());
    }

    // ---- events ------------------------------------------------------------------------------------------------------------

    /** (EntityJoinLevelEvent, lowest priority) one per key: a disk copy of a live key is refused, a fresh spawn replaces. */
    static void binnen(EntityJoinLevelEvent ev) {
        if (ev.isCanceled() || !(ev.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        Entity n = ev.getEntity();
        String k = sleutel(n);
        if (k == null) {
            return;
        }
        Map<String, Entity> m = index(sl);
        Entity oud = m.get(k);
        if (oud != null && oud != n && !oud.isRemoved()) {
            if (ev.loadedFromDisk()) {
                ev.setCanceled(true);   // (the chunk came back with its old copy; the manager already has the one)
                geweigerd++;
                LOG.debug("[dubbel] refused disk copy of {} at {}", k, n.blockPosition());
                return;
            }
            oud.discard();   // (a fresh spawn: the manager decided the old one is gone; it was a hidden leftover)
            vervangen++;
            LOG.debug("[dubbel] {} respawned, old copy at {} removed", k, oud.blockPosition());
        }
        m.put(k, n);
    }

    /** (EntityLeaveLevelEvent) forget removed owners; queue unsaved entities that only went hidden (= would become ghosts). */
    static void verborgen(EntityLeaveLevelEvent ev) {
        if (!(ev.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        Entity e = ev.getEntity();
        if (e.isRemoved()) {
            String k = sleutel(e);
            Map<String, Entity> m = k == null ? null : EIGENAAR.get(sl);
            if (m != null && m.get(k) == e) {
                m.remove(k);
            }
            return;
        }
        if (!(e instanceof Player) && !e.shouldBeSaved() && vanOns(e) && e.getPassengers().stream().noneMatch(p -> p instanceof Player)) {
            synchronized (GEESTEN) {
                GEESTEN.add(e);
            }
        }
    }

    /** Ours: a Vadsrijk class, or anything with a unique key. */
    static boolean vanOns(Entity e) {
        return e.getClass().getName().startsWith("nl.juiced.guhs.feature.vadsrijk.") || sleutel(e) != null;
    }

    /** End of every server tick: the queued ghosts that are still hidden go for good. Every 10 s: keyed doubles tidied. */
    static void tick(MinecraftServer server) {
        List<Entity> l;
        synchronized (GEESTEN) {
            if (GEESTEN.isEmpty()) {
                l = List.of();
            } else {
                l = new ArrayList<>(GEESTEN);
                GEESTEN.clear();
            }
        }
        for (Entity e : l) {
            if (!e.isRemoved() && e.level() instanceof ServerLevel sl && sl.getEntity(e.getUUID()) != e) {
                e.discard();
                geestenWeg++;
            }
        }
        if (server.getTickCount() % 200 == 37) {
            for (ServerLevel sl : server.getAllLevels()) {
                opruimen(sl, false);
            }
        }
    }

    static void wis() {
        synchronized (GEESTEN) {
            GEESTEN.clear();
        }
        EIGENAAR.clear();
        geweigerd = vervangen = geestenWeg = opgeruimd = 0;
    }

    // ---- scan ------------------------------------------------------------------------------------------------------------------

    /** A group of doubles: what it is and the copies (the first is the one that stays). */
    public record Groep(String wat, boolean streng, List<Entity> kopieen) {
    }

    /**
     * Every double among the visible entities of this level: same unique key (strict), or (heuristic, not keyed) an NPC guh with
     * the same NPC id, or a named Vadsrijk non-mob with the same type + name within 2 blocks.
     */
    public static List<Groep> scan(ServerLevel sl) {
        Map<String, List<Entity>> per = new LinkedHashMap<>();
        Map<String, Boolean> streng = new HashMap<>();
        for (Entity e : sl.getAllEntities()) {
            if (e == null || e.isRemoved()) {
                continue;
            }
            String k = sleutel(e);
            if (k != null) {
                per.computeIfAbsent(k, x -> new ArrayList<>()).add(e);
                streng.put(k, true);
                continue;
            }
            String h = null;
            if (e instanceof RijkGuh g && !g.npcId().isEmpty()) {
                h = "npc?:" + g.npcId();
            } else if (e.hasCustomName() && e.getClass().getName().startsWith("nl.juiced.guhs.feature.vadsrijk.") && !(e instanceof VadsrijkMob m && !(m instanceof VadsrijkBaas))) {
                h = "naam?:" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()) + ":" + e.getCustomName().getString()
                        + "@" + (e.getBlockX() >> 1) + "," + (e.getBlockY() >> 1) + "," + (e.getBlockZ() >> 1);
            }
            if (h != null) {
                per.computeIfAbsent(h, x -> new ArrayList<>()).add(e);
                streng.putIfAbsent(h, false);
            }
        }
        List<Groep> uit = new ArrayList<>();
        Map<String, Entity> eig = EIGENAAR.getOrDefault(sl, Collections.emptyMap());
        for (Map.Entry<String, List<Entity>> en : per.entrySet()) {
            List<Entity> l = en.getValue();
            if (l.size() < 2) {
                continue;
            }
            Entity baas = eig.get(en.getKey());
            if (baas != null && l.remove(baas)) {
                l.add(0, baas);
            }
            uit.add(new Groep(en.getKey(), streng.get(en.getKey()), l));
        }
        return uit;
    }

    /** Removes the extra copies (keeps the first of each group). {@code ookHeuristisch}: also the not-keyed groups. */
    public static int opruimen(ServerLevel sl, boolean ookHeuristisch) {
        int n = 0;
        for (Groep g : scan(sl)) {
            if (!g.streng() && !ookHeuristisch) {
                continue;
            }
            for (int i = 1; i < g.kopieen().size(); i++) {
                g.kopieen().get(i).discard();
                n++;
            }
            if (g.streng()) {
                index(sl).put(g.wat(), g.kopieen().get(0));
            }
        }
        if (n > 0) {
            opgeruimd += n;
            LOG.info("[dubbel] removed {} double(s) in {}", n, sl.dimension().identifier());
        }
        return n;
    }

    /** How many live keyed entities this level has (dev stats). */
    public static int bekend(ServerLevel sl) {
        Map<String, Entity> m = EIGENAAR.get(sl);
        if (m == null) {
            return 0;
        }
        int n = 0;
        for (Entity e : m.values()) {
            if (!e.isRemoved()) {
                n++;
            }
        }
        return n;
    }
}
