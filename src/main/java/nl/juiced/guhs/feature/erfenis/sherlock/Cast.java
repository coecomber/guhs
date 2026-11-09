package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The story's characters in the world. Everything here is temporary: spawned when needed (a scene next to the player, the
 * witnesses of the running case at their spots, the courtroom), tagged {@link #TAG}, and gone again when nobody needs them
 * (never saved: what a chunk saved of them is thrown away when it loads again). Who an entity is: its roleData
 * {@code SherlockRol} ("sherlock", "vadson", "getuige", "guhrrit", "rechtbank", ...), {@code SherlockEigenaar},
 * {@code SherlockGetuige}, {@code SherlockZaak}.
 */
public final class Cast {
    public static final String TAG = "guhs_sherlock";
    public static final String ROL = "SherlockRol", EIGENAAR = "SherlockEigenaar", GETUIGE = "SherlockGetuige", ZAAK = "SherlockZaak";
    /** Shared characters (the courtroom) belong to nobody: this owner. */
    public static final UUID IEDEREEN = new UUID(0, 0);
    /** A scene character with its owner further away than this (or offline) goes; one that stood idle this long too. */
    private static final double WEG = 40;
    private static final int SCENE_TICKS = 20 * 120;

    private record Plek(UUID entity, ServerLevel level, long sinds, boolean scene) {
    }

    /** owner to (role key to the entity). */
    private static final Map<UUID, Map<String, Plek>> SPELERS = new ConcurrentHashMap<>();
    /** The entities alive this session (anything else with the tag is a leftover from a save). */
    private static final Set<UUID> LEVEND = ConcurrentHashMap.newKeySet();

    private Cast() {
    }

    // =====================================================================================================================
    // making
    // =====================================================================================================================

    /** A sitting guh character (any kind), tagged; not yet in the world. */
    @Nullable
    public static GuhNpcEntity npc(ServerLevel level, Kind kind, Vec3 pos, float yaw) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        npc.addTag(TAG);
        return npc;
    }

    /** Hendrik de Haan: a chicken that stays put. */
    @Nullable
    private static Mob kip(ServerLevel level, Vec3 pos, float yaw) {
        Mob kip = EntityType.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
        if (kip == null) {
            return null;
        }
        kip.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        kip.setNoAi(true);
        kip.setInvulnerable(true);
        kip.setSilent(false);
        kip.addTag(TAG);
        return kip;
    }

    private static CompoundTagHolder data(Entity e) {
        return new CompoundTagHolder(e);
    }

    /** roleData of a GuhNpcEntity, the persistent data of anything else. */
    record CompoundTagHolder(Entity e) {
        net.minecraft.nbt.CompoundTag tag() {
            return e instanceof GuhNpcEntity n ? n.roleData : e.getPersistentData();
        }
    }

    public static String rol(Entity e) {
        return data(e).tag().getStringOr(ROL, "");
    }

    public static String getuige(Entity e) {
        return data(e).tag().getStringOr(GETUIGE, "");
    }

    @Nullable
    public static UUID eigenaar(Entity e) {
        String s = data(e).tag().getStringOr(EIGENAAR, "");
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException x) {
            return null;
        }
    }

    /**
     * Puts a character in the world for this owner under a role key (or gives back the one that is still there). kind null =
     * the chicken. naamKey: a name above the head (null: the kind's own name).
     */
    @Nullable
    public static Entity zet(ServerLevel level, UUID eigenaar, String sleutel, String rol, @Nullable Kind kind, Vec3 pos, float yaw,
                             @Nullable String naamKey, boolean scene) {
        Entity bestaand = van(level.getServer(), eigenaar, sleutel);
        if (bestaand != null && bestaand.level() == level) {
            if (bestaand.position().distanceToSqr(pos) > 9 * 9) {
                bestaand.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            }
            SPELERS.get(eigenaar).put(sleutel, new Plek(bestaand.getUUID(), level, level.getGameTime(), scene));
            return bestaand;
        }
        Entity e = kind == null ? kip(level, pos, yaw) : npc(level, kind, pos, yaw);
        if (e == null) {
            return null;
        }
        net.minecraft.nbt.CompoundTag t = data(e).tag();
        t.putString(ROL, rol);
        t.putString(EIGENAAR, eigenaar.toString());
        if (naamKey != null) {
            e.setCustomName(Component.translatable(naamKey));
            e.setCustomNameVisible(true);
        }
        LEVEND.add(e.getUUID());
        level.addFreshEntity(e);
        SPELERS.computeIfAbsent(eigenaar, k -> new ConcurrentHashMap<>()).put(sleutel, new Plek(e.getUUID(), level, level.getGameTime(), scene));
        return e;
    }

    /** The character of this owner under this key, if it is still there. */
    @Nullable
    public static Entity van(MinecraftServer server, UUID eigenaar, String sleutel) {
        Map<String, Plek> m = SPELERS.get(eigenaar);
        Plek p = m == null ? null : m.get(sleutel);
        if (p == null) {
            return null;
        }
        Entity e = p.level().getEntity(p.entity());
        if (e == null || !e.isAlive()) {
            m.remove(sleutel);
            // 1.9.6: it may only be unloaded (its chunk saved it): forget it for good, so it is thrown away when that chunk
            // loads again instead of standing next to the new one (two Pims / Bonnies at the same building)
            LEVEND.remove(p.entity());
            return null;
        }
        return e;
    }

    /** Sends away this owner's characters whose key starts with this (e.g. "getuige:"); "" = all. */
    public static void weg(MinecraftServer server, UUID eigenaar, String begin) {
        Map<String, Plek> m = SPELERS.get(eigenaar);
        if (m == null) {
            return;
        }
        for (Iterator<Map.Entry<String, Plek>> it = m.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Plek> e = it.next();
            if (e.getKey().startsWith(begin)) {
                Entity ent = e.getValue().level().getEntity(e.getValue().entity());
                if (ent != null) {
                    poef(ent);
                }
                it.remove();
            }
        }
    }

    private static void poef(Entity e) {
        if (e.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, e.getX(), e.getY() + 0.6, e.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
        }
        LEVEND.remove(e.getUUID());
        e.discard();
    }

    // =====================================================================================================================
    // a character next to the player (for a scene)
    // =====================================================================================================================

    /** Where the n-th character of a scene stands: a half circle ~3 blocks in front of the player, on the ground. */
    public static Vec3 voorSpeler(ServerPlayer p, int n) {
        float[] hoeken = {-38, 38, -75, 75, 0, -110, 110};
        float hoek = p.getYRot() + hoeken[Math.floorMod(n, hoeken.length)];
        double afstand = n == 4 ? 3.6 : 2.8;
        double x = p.getX() - Mth.sin(hoek * Mth.DEG_TO_RAD) * afstand;
        double z = p.getZ() + Mth.cos(hoek * Mth.DEG_TO_RAD) * afstand;
        return opGrond(p.level(), x, p.getY(), z);
    }

    /**
     * (1.9 afwerking) The n-th character of a scene in front of the player's own Detectivebureau (a little half circle before
     * its door), when the player is in the village near it; null: no bureau (yet) or far away, then {@link #voorSpeler}.
     */
    @Nullable
    static Vec3 bijBureau(ServerPlayer p, int n) {
        if (p.level().dimension() != nl.juiced.guhs.feature.erfenis.Plot.DIM) {
            return null;
        }
        net.minecraft.server.MinecraftServer server = p.level().getServer();
        BlockPos voor = Sherlock.bureauPlek.apply(server, p.getUUID());
        net.minecraft.core.Direction uit = Sherlock.bureauUit.apply(server, p.getUUID());
        if (voor == null || uit == null || p.distanceToSqr(Vec3.atBottomCenterOf(voor)) > 48 * 48) {
            return null;
        }
        double[][] plekken = {{-1.3, 0.6}, {1.3, 0.6}, {-2.4, 1.6}, {2.4, 1.6}, {0, 2.4}, {-3.4, 2.6}, {3.4, 2.6}};
        double[] pl = plekken[Math.floorMod(n, plekken.length)];
        double ox = uit.getStepX(), oz = uit.getStepZ(), sx = -oz, sz = ox;   // out of the door, and sideways
        double x = voor.getX() + 0.5 + sx * pl[0] + ox * pl[1], z = voor.getZ() + 0.5 + sz * pl[0] + oz * pl[1];
        return opGrond(p.level(), x, voor.getY(), z);
    }

    /** The highest place to stand at (x, z) not much above y (so never on a roof far above the player). */
    public static Vec3 opGrond(ServerLevel level, double x, double y, double z) {
        BlockPos b = BlockPos.containing(x, y + 2, z);
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(b.below()).isAir() && level.getBlockState(b).getCollisionShape(level, b).isEmpty()) {
                return new Vec3(x, b.getY(), z);
            }
            b = b.below();
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return new Vec3(x, Math.min(top, y + 1), z);
    }

    /** Yaw from a spot towards a point. */
    public static float kijkNaar(Vec3 van, Vec3 naar) {
        return (float) (Mth.atan2(naar.z - van.z, naar.x - van.x) * Mth.RAD_TO_DEG) - 90f;
    }

    /** A story character (by speaker id) next to the player for a scene: the same one again while it is there. */
    @Nullable
    public static Entity bijSpeler(ServerPlayer p, String wie, int n) {
        Kind kind = Sherlock.kindVan(wie);
        if (kind == null) {
            return null;
        }
        Vec3 pos = bijBureau(p, n);
        boolean bureau = pos != null;
        if (pos == null) {
            pos = voorSpeler(p, n);
        }
        Entity al = van(p.level().getServer(), p.getUUID(), "scene:" + wie);
        if (al != null && (bureau ? al.position().distanceTo(pos) < 4 : al.distanceTo(p) < 12)) {
            SPELERS.get(p.getUUID()).put("scene:" + wie, new Plek(al.getUUID(), p.level(), p.level().getGameTime(), true));
            return al;
        }
        return zet(p.level(), p.getUUID(), "scene:" + wie, wie, kind, pos, kijkNaar(pos, p.position()), null, true);
    }

    // =====================================================================================================================
    // tidying
    // =====================================================================================================================

    /** Every 5 s: characters of players who left (or of a scene long over) go. */
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        for (Map.Entry<UUID, Map<String, Plek>> s : SPELERS.entrySet()) {
            ServerPlayer p = s.getKey().equals(IEDEREEN) ? null : server.getPlayerList().getPlayer(s.getKey());
            Map<String, Plek> m = s.getValue();
            for (Iterator<Map.Entry<String, Plek>> it = m.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Plek> e = it.next();
                Plek pl = e.getValue();
                Entity ent = pl.level().getEntity(pl.entity());
                if (ent == null) {
                    it.remove();
                    LEVEND.remove(pl.entity());   // (1.9.6: unloaded = gone; a saved copy is thrown away when it loads)
                    continue;
                }
                boolean weg;
                if (s.getKey().equals(IEDEREEN)) {
                    weg = pl.level().players().stream().noneMatch(sp -> sp.distanceToSqr(ent) < WEG * WEG);
                } else {
                    weg = p == null || p.level() != ent.level() || p.distanceToSqr(ent) > WEG * WEG
                            || pl.scene() && pl.level().getGameTime() - pl.sinds() > SCENE_TICKS && p.distanceToSqr(ent) > 6 * 6;
                }
                if (weg) {
                    poef(ent);
                    it.remove();
                }
            }
        }
    }

    /** What a chunk saved of a story character is thrown away when it loads (they are spawned fresh when needed). */
    static void opLaden(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e.entityTags().contains(TAG) && !LEVEND.contains(e.getUUID())) {
            event.setCanceled(true);
        }
    }

    /** (tests, reset) forgets everything of this owner. */
    static void vergeet(MinecraftServer server, UUID eigenaar) {
        weg(server, eigenaar, "");
        SPELERS.remove(eigenaar);
    }

    /** (tests) how many characters this owner has now. */
    static int aantal(UUID eigenaar) {
        Map<String, Plek> m = SPELERS.get(eigenaar);
        return m == null ? 0 : m.size();
    }

    static Map<String, Entity> alle(MinecraftServer server, UUID eigenaar) {
        Map<String, Entity> uit = new HashMap<>();
        Map<String, Plek> m = SPELERS.get(eigenaar);
        if (m != null) {
            for (String k : m.keySet()) {
                Entity e = van(server, eigenaar, k);
                if (e != null) {
                    uit.put(k, e);
                }
            }
        }
        return uit;
    }
}
