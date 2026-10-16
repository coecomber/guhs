package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;

/**
 * einde2 phase B, slice geest_spel: where the markers of the Geestenwereld build are (CONTRACT_E2_B 4.5, geest_bouw's
 * sidecar {@code geestenwereld.json}). Every answer is null / empty when a marker is not in the build (yet): whatever asks
 * then simply does nothing, so this slice works with any version of the build.
 */
public final class GeestPlek {
    private GeestPlek() {
    }

    /** The world position of a marker (null: not in the build). */
    @Nullable
    public static BlockPos pos(String id) {
        return Geestenwereld.wereld(id);
    }

    /** The middle of a marker's block floor (null: not in the build). */
    @Nullable
    public static Vec3 midden(String id) {
        BlockPos p = pos(id);
        return p == null ? null : Vec3.atBottomCenterOf(p);
    }

    @Nullable
    public static Plekken.Plek plek(String id) {
        return Plekken.plek(Geestenwereld.NAAM, id);
    }

    /** The box of a zone marker in the world (null: no such zone). The box includes both corners. */
    @Nullable
    public static AABB doos(String zone) {
        Plekken.Plek p = plek(zone);
        if (p == null || p.tot() == null) {
            return null;
        }
        BlockPos a = Geestenwereld.OORSPRONG.offset(p.pos()), b = Geestenwereld.OORSPRONG.offset(p.tot());
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** Is the player inside this zone marker (in the Geestenwereld)? */
    public static boolean in(ServerPlayer p, String zone) {
        AABB d = doos(zone);
        return d != null && Geestenwereld.in(p) && d.contains(p.position());
    }

    /** The points of a pad marker in the world (empty: no such pad). */
    public static List<Vec3> pad(String id) {
        Plekken.Plek p = plek(id);
        List<Vec3> uit = new ArrayList<>();
        if (p == null) {
            return uit;
        }
        for (BlockPos q : p.punten()) {
            uit.add(Vec3.atBottomCenterOf(Geestenwereld.OORSPRONG.offset(q)));
        }
        return uit;
    }

    /** The numbered markers {@code <prefix>1}, {@code <prefix>2}, ... that the build has (stops at the first missing one). */
    public static List<String> reeks(String prefix) {
        List<String> uit = new ArrayList<>();
        for (int i = 1; i < 64 && plek(prefix + i) != null; i++) {
            uit.add(prefix + i);
        }
        return uit;
    }

    /** Every marker whose id starts with this prefix (sidecar order). */
    public static List<String> alleMet(String prefix) {
        List<String> uit = new ArrayList<>();
        for (String id : Plekken.van(Geestenwereld.NAAM).keySet()) {
            if (id.startsWith(prefix)) {
                uit.add(id);
            }
        }
        return uit;
    }

    private static volatile Map<Long, String> props;

    /**
     * The prop marker at this block, or at the block right under it (a prop is often the floor under a thing, or the thing
     * on a floor); null: none. Only props, never a zone, pad or npc.
     */
    @Nullable
    public static String propOp(BlockPos pos) {
        Map<Long, String> m = props;
        if (m == null) {
            m = new HashMap<>();
            for (Plekken.Plek p : Plekken.van(Geestenwereld.NAAM).values()) {
                if (Plekken.PROP.equals(p.soort())) {
                    m.putIfAbsent(Geestenwereld.OORSPRONG.offset(p.pos()).asLong(), p.id());
                }
            }
            props = m;
        }
        String id = m.get(pos.asLong());
        return id != null ? id : m.get(pos.below().asLong());
    }

    /** The resident placed at this marker (a creature or an NPC of the shared build), near the marker; null: none (yet). */
    @Nullable
    public static Entity bewoner(ServerLevel level, String marker) {
        BlockPos at = pos(marker);
        if (at == null) {
            return null;
        }
        String naam = Geestenwereld.NAAM + "/" + marker;
        for (Entity e : level.getEntities((Entity) null, new AABB(at).inflate(24), e -> naam.equals(plekVan(e)))) {
            return e;
        }
        return null;
    }

    /** The {@code <structuur>/<plek>} of a resident (or ""). */
    public static String plekVan(Entity e) {
        if (e instanceof nl.juiced.guhs.entity.GuhNpcEntity npc) {
            return npc.roleData.getStringOr(Bewoners.PLEK, "");
        }
        return e.getPersistentData().getStringOr(Bewoners.PLEK, "");
    }

    /** The marker of a resident of the Geestenwereld ("echo_bumi"), or "" when it is not one. */
    public static String markerVan(Entity e) {
        String p = plekVan(e);
        String pre = Geestenwereld.NAAM + "/";
        return p.startsWith(pre) ? p.substring(pre.length()) : "";
    }

    /** Within this many blocks of a marker (same level, in the Geestenwereld)? */
    public static boolean bij(ServerPlayer p, String marker, double straal) {
        Vec3 m = midden(marker);
        return m != null && Geestenwereld.in(p) && p.position().distanceToSqr(m) <= straal * straal;
    }
}
