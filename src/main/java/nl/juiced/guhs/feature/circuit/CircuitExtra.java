package nl.juiced.guhs.feature.circuit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceGuhEntity;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.race.RaceRit;
import nl.juiced.guhs.feature.race.RaceTrack;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * What the Guh-Circuit's tracks add to a race ({@link RaceBaan.Extra}):
 * <ul>
 *     <li>Mika-pikkers on their spots beside the track (markers circuit_mikaplek; makkelijk fewer, lastig more: a spot counts
 *     from its level on): a race guh that runs past too close loses its VAHOEG (lastig: they reach further).</li>
 *     <li>Rolling kaasknabbels down the Kaasberg's Knabbelhelling (markers circuit_rolplek, with a pushing Mika at each):
 *     makkelijk now and then, lastig often.</li>
 *     <li>The Vadslooping (marker circuit_looping): run into it and your race guh goes round the loop by itself (RaceRit).</li>
 *     <li>The Regenboogbaan's rainbow trail behind your guh.</li>
 * </ul>
 * The old racebaan uses it too, for its Mika-pikkers on lastig. Everything it puts out goes away with the race.
 */
public final class CircuitExtra implements RaceBaan.Extra {
    /** The Mika-pikkers, pushers and rolling knabbels of running races (the rest poofs by itself). */
    private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();
    /** How close a Mika-pikker gets you (horizontal, per level), and how often a kaasknabbel rolls (ticks, per level). */
    public static final float[] REACH = {2.4f, 3.3f, 4.2f};
    public static final int[] ROL_TICKS = {70, 46, 30};
    public static final int MAX_KNABBELS = 10;
    public static final String PIKKERS = "pikkers", DUWERS = "duwers", KNABBELS = "knabbels", ROL = "rol";

    private final boolean trail;

    public CircuitExtra(boolean trail) {
        this.trail = trail;
    }

    public static boolean isLive(Entity entity) {
        return LIVE.contains(entity.getUUID());
    }

    @SuppressWarnings("unchecked")
    public static List<UUID> list(RaceGame game, String key) {
        return (List<UUID>) game.extraState.computeIfAbsent(key, k -> new ArrayList<UUID>());
    }

    @Override
    public void start(RaceGame game, ServerLevel level, ServerPlayer racer) {
        int n = game.niveau().ordinal();
        RaceTrack track = game.track();
        for (RaceTrack.Marker m : track.markers("circuit_mikaplek")) {
            if (m.vanaf() <= n) {
                MikaPikkerEntity pikker = spawn(level, m.centre(), m.direction().toYRot(), false);
                if (pikker != null) {
                    list(game, PIKKERS).add(pikker.getUUID());
                }
            }
        }
        for (RaceTrack.Marker m : track.markers("circuit_rolplek")) {
            if (m.vanaf() <= n) {
                Vec3 back = m.direction().getUnitVec3().scale(-1.3);
                MikaPikkerEntity duwer = spawn(level, m.centre().add(back), m.direction().toYRot(), true);
                if (duwer != null) {
                    list(game, DUWERS).add(duwer.getUUID());
                }
            }
        }
        game.extraState.put(ROL, 0);
    }

    private static MikaPikkerEntity spawn(ServerLevel level, Vec3 at, float yaw, boolean duwer) {
        MikaPikkerEntity mika = CircuitFeature.MIKAPIKKER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mika == null) {
            return null;
        }
        mika.setDuwer(duwer);
        mika.setHome(at, yaw);
        LIVE.add(mika.getUUID());
        level.addFreshEntity(mika);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.4, at.z, 8, 0.3, 0.3, 0.3, 0.02);
        return mika;
    }

    @Override
    public void tick(RaceGame game, ServerLevel level, ServerPlayer racer, RaceGuhEntity mount) {
        int n = game.niveau().ordinal();
        if (trail && game.ticks() % 1 == 0) {
            regenboogspoor(level, mount, game.ticks());
        }
        mount.setSprongen(trail);          // 2.10.1: the Regenboogbaan's gaps: the rainbow jump (RaceGuhEntity)
        // the Mika-pikkers: too close = your VAHOEG is gone
        if (!mount.inRit()) {
            for (UUID id : list(game, PIKKERS)) {
                if (level.getEntity(id) instanceof MikaPikkerEntity pikker && pikker.kanPikken()) {
                    double dx = pikker.getX() - mount.getX(), dz = pikker.getZ() - mount.getZ();
                    if (dx * dx + dz * dz < REACH[n] * REACH[n] && Math.abs(pikker.getY() - mount.getY()) < 2.5) {
                        pikker.pik(mount, racer);
                    }
                }
            }
        }
        // the rolling kaasknabbels of the Knabbelhelling
        List<RaceTrack.Marker> rollen = game.track().markers("circuit_rolplek").stream().filter(m -> m.vanaf() <= n).toList();
        if (!rollen.isEmpty()) {
            int count = (Integer) game.extraState.getOrDefault(ROL, 0) + 1;
            game.extraState.put(ROL, count);
            List<UUID> knabbels = list(game, KNABBELS);
            knabbels.removeIf(id -> !(level.getEntity(id) instanceof RolknabbelEntity k) || !k.isAlive());
            if (count % ROL_TICKS[n] == 0 && knabbels.size() < MAX_KNABBELS) {
                RaceTrack.Marker m = rollen.get((count / ROL_TICKS[n]) % rollen.size());
                RolknabbelEntity knabbel = rolKnabbel(level, m);
                if (knabbel != null) {
                    knabbels.add(knabbel.getUUID());
                    for (UUID id : list(game, DUWERS)) {
                        if (level.getEntity(id) instanceof MikaPikkerEntity duwer && duwer.home() != null && duwer.home().distanceToSqr(m.centre()) < 4) {
                            duwer.duw();
                        }
                    }
                }
            }
        }
        // the Vadslooping: run in and round you go
        if (!mount.inRit()) {
            for (RaceTrack.Marker m : game.track().markers("circuit_looping")) {
                if (inLooping(m, mount)) {
                    startLooping(level, racer, mount, m);
                    break;
                }
            }
        }
    }

    /** A kaasknabbel starts rolling down from a rolplek. */
    static RolknabbelEntity rolKnabbel(ServerLevel level, RaceTrack.Marker m) {
        RolknabbelEntity knabbel = CircuitFeature.ROLKNABBEL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (knabbel == null) {
            return null;
        }
        Vec3 at = m.centre().add(0, 0.1, 0);
        knabbel.snapTo(at.x, at.y, at.z, 0, 0);
        knabbel.rol(m.direction().getUnitVec3());
        LIVE.add(knabbel.getUUID());
        level.addFreshEntity(knabbel);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1f, 0.6f);
        return knabbel;
    }

    /** Is the race guh running into the looping (the marker's row across the road, going its way)? */
    static boolean inLooping(RaceTrack.Marker m, RaceGuhEntity mount) {
        Direction f = m.direction();
        Direction right = f.getClockWise();
        Vec3 d = mount.position().subtract(m.centre());
        double along = d.x * f.getStepX() + d.z * f.getStepZ();
        double side = d.x * right.getStepX() + d.z * right.getStepZ();
        float look = net.minecraft.util.Mth.wrapDegrees(mount.getYRot() - f.toYRot());
        return along >= -0.6 && along <= 1.6 && Math.abs(side) <= 4.7 && Math.abs(d.y) <= 1.6 && Math.abs(look) < 100;
    }

    /** The ride of the Vadslooping, from its entrance marker. */
    public static RaceRit looping(RaceTrack.Marker m) {
        return new RaceRit(m.centre(), m.direction(), CircuitBanen.LOOP_R, CircuitBanen.LOOP_L, CircuitBanen.LOOP_W, CircuitBanen.LOOP_TICKS);
    }

    static void startLooping(ServerLevel level, ServerPlayer racer, RaceGuhEntity mount, RaceTrack.Marker m) {
        mount.startRit(looping(m));
        RaceGame.title(racer, Component.translatable("quest.guhs.circuit.looping").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("quest.guhs.circuit.looping.sub").withStyle(ChatFormatting.YELLOW), 0, 30, 10);
        level.playSound(null, mount.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.NEUTRAL, 1f, 0.8f);
        level.playSound(null, mount.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.2f, 1.6f);
        Vec3 c = m.centre();
        level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 1, c.z, 30, 1, 0.5, 1, 0.1);
    }

    /** A rainbow trail behind the race guh (the Regenboogbaan). */
    private static void regenboogspoor(ServerLevel level, RaceGuhEntity mount, int ticks) {
        Vec3 back = Vec3.directionFromRotation(0, mount.getYRot()).scale(-0.9);
        for (int i = 0; i < 2; i++) {
            float hue = ((ticks * 2 + i * 7) % 60) / 60f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1f);
            DustParticleOptions dust = new DustParticleOptions(rgb & 0xFFFFFF, 1.6f);
            level.sendParticles(dust, mount.getX() + back.x, mount.getY() + 0.35 + i * 0.3, mount.getZ() + back.z, 2, 0.15, 0.1, 0.15, 0);
        }
    }

    @Override
    public void end(RaceGame game, ServerLevel level) {
        for (String key : new String[]{PIKKERS, DUWERS, KNABBELS}) {
            for (UUID id : list(game, key)) {
                LIVE.remove(id);
                Entity e = level.getEntity(id);
                if (e != null) {
                    level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                    e.discard();
                }
            }
        }
    }

    @Override
    public void finish(RaceGame game, ServerLevel level, ServerPlayer racer, int total, RaceGame.Medal medal, boolean record) {
        if (game.baan().isRacebaan()) {
            return;
        }
        RaceGame.grant(racer, "grote_guhspelen/circuit_eerste");
        RaceGame.grant(racer, "grote_guhspelen/circuit_" + game.baan().id);
        if (CircuitBanen.BANEN.stream().allMatch(b -> RaceRecords.finishedOnce(racer, b.id))) {
            RaceGame.grant(racer, "grote_guhspelen/circuit_alle_banen");
        }
        if (game.niveau() == Niveau.LASTIG) {
            RaceGame.grant(racer, "grote_guhspelen/circuit_lastig");
            if (medal == RaceGame.Medal.GOUD) {
                RaceGame.grant(racer, "grote_guhspelen/circuit_goud_lastig");
            }
        }
        // 1.2.7: "Sneller dan de legende" no longer hangs on the live world record (the record holder never saw a golden
        // ghost, nor did the first racer on an empty board, and a sharp record locked everyone else out for good): a
        // gold-medal time on any circuit track is the legend's time. Beating the golden ghost itself still counts too.
        boolean geest = game.goudTicks() >= 0 && total < game.goudTicks();
        if (geest || medal == RaceGame.Medal.GOUD) {
            boolean nieuw = !RaceGame.has(racer, "grote_guhspelen/circuit_gouden_geest");
            RaceGame.grant(racer, "grote_guhspelen/circuit_gouden_geest");
            if (geest) {
                racer.sendSystemMessage(Component.translatable("quest.guhs.circuit.gouden_geest").withStyle(ChatFormatting.GOLD));
            } else if (nieuw) {
                racer.sendSystemMessage(Component.translatable("quest.guhs.circuit.gouden_tijd").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** (Tests) a Mika-pikker or knabbel that doesn't belong to a race, kept alive anyway. */
    static void markLive(Entity entity) {
        LIVE.add(entity.getUUID());
    }

    /** (Tests) forget every live Mika-pikker and knabbel. */
    static void forgetAll() {
        LIVE.clear();
    }
}
