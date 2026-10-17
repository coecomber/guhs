package nl.juiced.guhs.feature.einde2.spcast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 4.3, 7.2), slice sp_cast: the residents of De Ruige Kroeg in Schelpenstad: the barman,
 * the uitsmijter, six stoere Mikas and the slapende Mika on the markers {@code schelpenstad/barman}, {@code uitsmijter},
 * {@code stoere_mika_1..6}, {@code slapende_mika} (the build marks them as plain {@code guhs:mika}, which {@link Bewoners}
 * never makes, and {@code Bewoners.anders} only takes a creature: so a small placer of its own).
 * <p>
 * It works like {@link Bewoners}: every {@link Bewoners#CHECK_TICKS} ticks around a player in the Guheinde, a resident that
 * is missing twice {@link Bewoners#BEVESTIG} ticks apart is made ({@link Bewoners#maak}: the Kind, its role spot
 * {@code schelpenstad/<plek>}); it carries {@link Bewoners#TAG} (protected, never tamed, and the visibility listeners of
 * {@link Bewoners#BIJ_MAKEN} are told: d2_heen owns their visibility); a double is discarded.
 */
public final class KroegBewoners {
    public static final String STRUCTUUR = Einde2Ids.SCHELPENSTAD;
    /** spot -> Kind (the six stoere Mikas take turns between the two looks). */
    public static final Map<String, Kind> PLEKKEN;

    static {
        Map<String, Kind> m = new java.util.LinkedHashMap<>();
        m.put("barman", Kind.KROEG_BARMAN_MIKA);
        m.put("uitsmijter", Kind.KROEG_UITSMIJTER_MIKA);
        for (int i = 1; i <= 6; i++) {
            m.put("stoere_mika_" + i, i % 2 == 1 ? Kind.STOERE_MIKA : Kind.STOERE_MIKA_2);
        }
        m.put("slapende_mika", Kind.SLAPENDE_MIKA);
        PLEKKEN = java.util.Collections.unmodifiableMap(m);
    }

    /** (not saved) dimension|tag -> the game time it was first seen missing. */
    private static final Map<String, Long> GEMIST = new ConcurrentHashMap<>();

    private KroegBewoners() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(KroegBewoners::opSpelerTick);
    }

    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId() + 7) % Bewoners.CHECK_TICKS == 0 && !p.isSpectator()
                && GuheindeFeature.isGuheinde(p.level())) {
            controleer(p.level(), p.blockPosition());
        }
    }

    /** The resident of every spot (the spots the sidecar knows). */
    public static List<Bewoners.Bewoner> bewoners() {
        List<Bewoners.Bewoner> uit = new ArrayList<>();
        for (Map.Entry<String, Kind> e : PLEKKEN.entrySet()) {
            Plekken.Plek p = Plekken.plek(STRUCTUUR, e.getKey());
            if (p != null) {
                uit.add(new Bewoners.Bewoner(STRUCTUUR, e.getKey(), e.getValue(), null, -1, true, 0, null, p.yaw(), Bewoners.ZOEK_NPC));
            }
        }
        return uit;
    }

    /** Looks at the Kroeg around this spot now; returns how many residents were made. */
    public static int controleer(ServerLevel level, BlockPos bij) {
        StructureStart start = Plekken.bij(level, STRUCTUUR, bij, Bewoners.BEREIK);
        return start == null ? 0 : alle(level, start, false).size();
    }

    /** (dev, tests) Makes every missing resident of this copy's Kroeg at once (no wait); returns those made. */
    public static List<Entity> nu(ServerLevel level, StructureStart start) {
        return alle(level, start, true);
    }

    private static List<Entity> alle(ServerLevel level, StructureStart start, boolean meteen) {
        List<Entity> uit = new ArrayList<>();
        for (Bewoners.Bewoner b : bewoners()) {
            BlockPos pos = Plekken.wereld(start, Plekken.plek(STRUCTUUR, b.plek()));
            Entity e = pos == null ? null : plek(level, Bewoners.tag(b.id(), start.getChunkPos()), pos, b, meteen);
            if (e != null) {
                uit.add(e);
            }
        }
        return uit;
    }

    /** One resident (as Bewoners.plek): there, or made after it was seen missing twice (meteen: at once); the one made. */
    @Nullable
    static Entity plek(ServerLevel level, String tag, BlockPos pos, Bewoners.Bewoner b, boolean meteen) {
        AABB zoek = new AABB(pos).inflate(b.zoek());
        if (!level.isPositionEntityTicking(pos) || !Bewoners.geladen(level, zoek)) {
            return null;
        }
        String key = level.dimension().identifier() + "|" + tag;
        boolean er = false;
        for (Entity e : level.getEntitiesOfClass(Entity.class, zoek, Entity::isAlive)) {
            if (!e.getPersistentData().getStringOr(Bewoners.TAG, "").equals(tag)) {
                continue;
            }
            if (er) {
                e.discard();                                            // (a double: the first one stays)
                continue;
            }
            er = true;
        }
        if (er) {
            GEMIST.remove(key);
            return null;
        }
        long nu = level.getGameTime();
        if (!meteen) {
            Long eerst = GEMIST.putIfAbsent(key, nu);
            if (eerst == null || nu - eerst < Bewoners.BEVESTIG) {
                return null;
            }
        }
        GEMIST.remove(key);
        Entity e = Bewoners.maak(level, Vec3.atBottomCenterOf(pos), Rotation.NONE, b);
        if (e == null) {
            return null;
        }
        e.getPersistentData().putString(Bewoners.TAG, tag);
        e.setInvulnerable(true);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.6, e.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        for (java.util.function.BiConsumer<Entity, String> luisteraar : Bewoners.BIJ_MAKEN) {
            try {
                luisteraar.accept(e, b.naam());
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger("guhs").warn("Guhs einde2 sp_cast: a BIJ_MAKEN listener failed for {}", b.naam(), ex);
            }
        }
        return e;
    }
}
