package nl.juiced.guhs.feature.weer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.slaapzorg.Slapen;
import nl.juiced.guhs.feature.sterrenveld.SterrenveldFeature;
import nl.juiced.guhs.feature.sterrenveld.VallendeSter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 1.8.0: the little bit of behaviour of the guh-weertjes (server; what you see falling is the client's: WeerClient). Once a
 * second, around every player inside a zone (within {@link #BEREIK} blocks of them, inside the zone):
 * <ul>
 *   <li>pluisjessneeuw: now and then a guh hops up to catch a fluff;</li>
 *   <li>bellenbui: the Blub-guhs are happy and blow bubbles along;</li>
 *   <li>knabbelbuitje: at most one kaasknabbel per zone every {@link #KNABBEL_TICKS} ticks falls out of the sky, and never
 *       when {@link #KNABBEL_MAX} already lie around (guhs go for dropped knabbels by themselves);</li>
 *   <li>regenboogweer: guhs and Blubs jump in the puddles;</li>
 *   <li>hartjesregen: tamed guhs and Blubs give hearts much more often;</li>
 *   <li>knusmist: guhs get sleepy (zzz);</li>
 *   <li>onweer: now and then a harmless lightning flash (visual only: no fire, no damage);</li>
 *   <li>droomweer: as in Guhdromenland: players float (soft slow falling and a little higher jump, no particles; it wears off
 *       outside the zone), a few catchable dream stars fall (at most {@link #DROOM_STERREN} per zone), and the guhs yawn more
 *       and fall asleep sooner.</li>
 * </ul>
 */
public final class WeerEffecten {
    public static final int BEREIK = 24, KNABBEL_TICKS = 600, KNABBEL_MAX = 3, DROOM_STERREN = 3;
    /** Per dimension and zone centre: the game time of the last knabbel. */
    private static final Map<Long, Long> LAATSTE_KNABBEL = new HashMap<>();

    private WeerEffecten() {
    }

    static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        List<WeerZones.Zone> zones = WeerZones.zones(level);
        if (zones.isEmpty()) {
            return;
        }
        RandomSource r = level.getRandom();
        for (WeerZones.Zone zone : zones) {
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || !zone.binnen(p.getX(), p.getZ())) {
                    continue;
                }
                rondSpeler(level, zone, p, r);
            }
        }
    }

    private static void rondSpeler(ServerLevel level, WeerZones.Zone zone, ServerPlayer p, RandomSource r) {
        Weertje w = zone.weertje();
        AABB box = p.getBoundingBox().inflate(BEREIK, 8, BEREIK);
        switch (w) {
            case PLUISJES -> {
                for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, box, g -> zone.binnen(g.getX(), g.getZ()))) {
                    if (r.nextInt(6) == 0 && guh.onGround() && !guh.isInSittingPose()) {
                        guh.setDeltaMovement(guh.getDeltaMovement().add(0, 0.42, 0));
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 3, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
            case BELLEN -> {
                for (BlubGuhEntity blub : level.getEntitiesOfClass(BlubGuhEntity.class, box, b -> zone.binnen(b.getX(), b.getZ()))) {
                    if (r.nextInt(3) == 0) {
                        level.sendParticles(KnuffelbadFeature.ZEEPBELLETJE.get(), blub.getX(), blub.getY() + blub.getBbHeight() * 0.6, blub.getZ(),
                                5, 0.25, 0.2, 0.25, 0.02);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, blub.getX(), blub.getY() + blub.getBbHeight() + 0.2, blub.getZ(), 2, 0.2, 0.1, 0.2, 0);
                    }
                }
            }
            case KNABBEL -> knabbel(level, zone, p, r);
            case REGENBOOG -> {
                for (TamableAnimal dier : level.getEntitiesOfClass(TamableAnimal.class, box,
                        d -> (d instanceof GuhEntity || d instanceof BlubGuhEntity) && zone.binnen(d.getX(), d.getZ()))) {
                    if (r.nextInt(5) == 0 && dier.onGround() && !dier.isInSittingPose() && WeerZones.regentOp(level, dier.blockPosition().above())) {
                        dier.setDeltaMovement(dier.getDeltaMovement().add(0, 0.36, 0));
                        level.sendParticles(ParticleTypes.SPLASH, dier.getX(), dier.getY() + 0.05, dier.getZ(), 12, 0.3, 0.05, 0.3, 0.1);
                    }
                }
            }
            case HARTJES -> {
                for (TamableAnimal dier : level.getEntitiesOfClass(TamableAnimal.class, box,
                        d -> (d instanceof GuhEntity || d instanceof BlubGuhEntity) && d.isTame() && zone.binnen(d.getX(), d.getZ()))) {
                    if (r.nextInt(3) == 0) {
                        level.sendParticles(ParticleTypes.HEART, dier.getX(), dier.getY() + dier.getBbHeight() + 0.2, dier.getZ(), 1, 0.2, 0.1, 0.2, 0);
                    }
                }
            }
            case KNUSMIST -> {
                for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, box, g -> zone.binnen(g.getX(), g.getZ()))) {
                    if (r.nextInt(6) == 0) {
                        level.sendParticles(EmotesFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.1, 0.05, 0.1, 0);
                    }
                }
            }
            case ONWEER -> {
                if (r.nextInt(25) == 0) {
                    flits(level, zone, p, r);
                }
            }
            case DROOM -> droom(level, zone, p, box, r);
            default -> {
            }
        }
    }

    /** The droomweer around one player (once a second). */
    private static void droom(ServerLevel level, WeerZones.Zone zone, ServerPlayer p, AABB box, RandomSource r) {
        // floaty: a few seconds at a time, refreshed while you stay in the zone (so it wears off soon after you leave)
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, true, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 50, 0, true, false, true));
        // a few catchable dream stars
        if (r.nextInt(4) == 0) {
            AABB zoneBox = new AABB(zone.midden()).inflate(WeerZones.STRAAL, 64, WeerZones.STRAAL);
            if (level.getEntities(SterrenveldFeature.VALLENDE_STER.get(), zoneBox, e -> true).size() < DROOM_STERREN) {
                double x = p.getX() + (r.nextDouble() * 2 - 1) * 14, z = p.getZ() + (r.nextDouble() * 2 - 1) * 14;
                if (zone.binnen(x, z)) {
                    int grond = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                    VallendeSter.nieuw(level, new Vec3(x, Math.max(grond, p.getY()) + 18 + r.nextInt(8), z));
                }
            }
        }
        // sleepy guhs: more yawns, and now and then one nods off (after its yawn)
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, box, g -> zone.binnen(g.getX(), g.getZ()))) {
            if (Slapen.slaapt(guh) || guh.isPassenger()) {
                continue;
            }
            int k = r.nextInt(60);
            if (k < 2) {
                guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
            } else if (k == 2) {
                guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
                Slapen.slaapStraksIn(guh, 50);
            }
        }
    }

    private static void knabbel(ServerLevel level, WeerZones.Zone zone, ServerPlayer p, RandomSource r) {
        long sleutel = level.dimension().hashCode() * 31L + zone.midden().asLong();
        long nu = level.getGameTime();
        if (nu - LAATSTE_KNABBEL.getOrDefault(sleutel, Long.MIN_VALUE / 2) < KNABBEL_TICKS) {
            return;
        }
        int liggend = level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(BEREIK, 16, BEREIK),
                e -> e.getItem().is(ModItems.KAAS_KNABBELS.get())).size();
        if (liggend >= KNABBEL_MAX) {
            return;
        }
        double x = p.getX() + (r.nextDouble() * 2 - 1) * 10, z = p.getZ() + (r.nextDouble() * 2 - 1) * 10;
        if (!zone.binnen(x, z)) {
            return;
        }
        int grond = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        ItemEntity knabbel = new ItemEntity(level, x, grond + 14, z, new ItemStack(ModItems.KAAS_KNABBELS.get()));
        knabbel.setDeltaMovement(Vec3.ZERO);
        knabbel.setDefaultPickUpDelay();
        level.addFreshEntity(knabbel);
        level.sendParticles(ParticleTypes.CLOUD, x, grond + 14.5, z, 6, 0.3, 0.2, 0.3, 0.01);
        LAATSTE_KNABBEL.put(sleutel, nu);
    }

    private static void flits(ServerLevel level, WeerZones.Zone zone, ServerPlayer p, RandomSource r) {
        double x = p.getX() + (r.nextDouble() * 2 - 1) * 40, z = p.getZ() + (r.nextDouble() * 2 - 1) * 40;
        if (!zone.binnen(x, z)) {
            return;
        }
        BlockPos grond = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(x, 0, z));
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt != null) {
            bolt.snapTo(Vec3.atBottomCenterOf(grond));
            bolt.setVisualOnly(true);       // a flash and a rumble: no fire, nobody gets hurt
            level.addFreshEntity(bolt);
        }
    }
}
