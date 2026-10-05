package nl.juiced.guhs.feature.sausdieren;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.world.WildeDieren;

/**
 * bbq2 (sausdieren): the game-bus side. The test lap's tick, the questline's "lure a Sausloper to the Verzorger-guh" step,
 * the bounce of the Stuiterdrankje, and wild Sauslopers that come and go like the other wild Guhs animals.
 */
public final class SausdierenEvents {
    /** A fall shorter than this does nothing special (and never hurts with the effect). */
    public static final float STUITER_VANAF = 1.5f;
    /** A Sausloper that follows the player this close, with the Verzorger-guh this close, is lured. */
    public static final double LOK_BIJ_SPELER = 5.0, LOK_BIJ_VERZORGER = 7.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Proefrit.tick(p);
        if (p.tickCount % 10 == 3 && SausdierenFeature.LIJN.stap(p) == 1) {
            lokCheck(p);
        }
    }

    /** Step 1 of the questline: a Sausloper followed this player's stick to the Verzorger-guh. */
    static boolean lokCheck(ServerPlayer p) {
        ServerLevel level = p.level();
        GuhNpcEntity npc = Stal.verzorger(level, p.blockPosition(), LOK_BIJ_VERZORGER + LOK_BIJ_SPELER);
        if (npc == null) {
            return false;
        }
        for (SausloperEntity loper : level.getEntitiesOfClass(SausloperEntity.class, p.getBoundingBox().inflate(LOK_BIJ_SPELER),
                l -> l.isAlive() && l.lokker() == p)) {
            if (loper.distanceToSqr(npc) <= LOK_BIJ_VERZORGER * LOK_BIJ_VERZORGER) {
                VerzorgerRol.gelokt(p, npc);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Proefrit.stop(p);
        }
    }

    /** Stuiterblub: no fall damage, and a real fall bounces you back up (less high every time; sneak to land). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.hasEffect(SausdierenFeature.STUITER)) {
            return;
        }
        double afstand = event.getDistance();
        event.setDamageMultiplier(0f);
        if (afstand <= STUITER_VANAF || e.isSuppressingBounce()) {
            return;
        }
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(v.x, stuiterSnelheid(afstand), v.z);
        e.hurtMarked = true;
        if (e.level() instanceof ServerLevel level) {
            level.playSound(null, e.blockPosition(), SausdierenFeature.STUITER_GELUID.get(), e.getSoundSource(), 0.8f, 1.0f + e.getRandom().nextFloat() * 0.3f);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, SausdierenFeature.BLUBROOM.get()), e.getX(), e.getY() + 0.1,
                    e.getZ(), 8, 0.25, 0.05, 0.25, 0.08);
        }
    }

    /** How fast a fall of this many blocks throws you back up: about a third of the height, at most some six blocks. */
    public static double stuiterSnelheid(double afstand) {
        return Math.min(1.1, 0.3 * Math.sqrt(afstand));
    }

    /** A wild Sausloper that the spawner brings while you play comes and goes (never piles up, never saved). */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof SausloperEntity loper && WildeDieren.komtEnGaat(event.getSpawnType()) && !loper.isPersistenceRequired()) {
            WildeDieren.markeer(loper);
        }
    }

    private SausdierenEvents() {
    }
}
