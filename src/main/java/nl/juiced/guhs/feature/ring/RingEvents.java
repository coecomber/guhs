package nl.juiced.guhs.feature.ring;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.Mikas;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-kern): the events of the Knabbelring (registered by {@link RingFeature}).
 * <ul>
 *   <li>Every tick of a player: the gifts ({@link Gaven#tick}); once a second: the ring's upkeep (worn = not to be seen,
 *       its weight, its whispering), the Nine, Sam-guh, Smikagol, the rest fires, the first-steps help.</li>
 *   <li>The ring's craving: now and then it whispers that you could just eat it, and wild guhs that smell it come after
 *       you, drooling. No penalty, ever.</li>
 *   <li>A Mika can't pick a player who wears the ring as its target.</li>
 *   <li>Logging out, dying and changing dimension: the ring is off, no rock, no rope, no hunt left behind.</li>
 * </ul>
 */
public final class RingEvents {
    /** The ring whispers every TREK_MIN .. TREK_MIN + TREK_SPREIDING ticks; it has this many different whispers. */
    public static final int TREK_MIN = 2400, TREK_SPREIDING = 2400, TREK_REGELS = 8;
    /** Wild guhs smell the ring from this far. */
    public static final double KWIJL_AFSTAND = 10;
    private static final String WEGWIJS = "guhs_ring_wegwijs";

    private static final Map<UUID, Long> TREK = new ConcurrentHashMap<>();
    /** The players who carry the ring right now (for the drooling guhs). */
    private static final Set<UUID> DRAGERS = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Gaven.tick(p);
        if ((p.tickCount + p.getId()) % 20 == 0) {
            seconde(p);
        }
    }

    /** (also for the tests) what happens once a second for this player. */
    static void seconde(ServerPlayer p) {
        boolean heeft = Ring.heeft(p);
        if (heeft) {
            DRAGERS.add(p.getUUID());
        } else {
            DRAGERS.remove(p.getUUID());
        }
        // worn: nobody sees you (the Eye does); the ring comes off by itself when it leaves your pockets
        if (Ring.om(p)) {
            if (!heeft) {
                Ring.doeOm(p, false);
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 50, 0, true, false, false));
                p.level().sendParticles(p, ParticleTypes.WITCH, false, false, p.getX(), p.getY() + 0.3, p.getZ(), 3, 0.3, 0.1, 0.3, 0);
            }
        }
        Ring.pasZwaarteToe(p);
        Negen.tick(p);
        Sam.tick(p);
        Smikagol.tick(p);
        if (Ring.verhaalWereld(p.level())) {
            Rustpunt.zoek(p);
        }
        if (heeft) {
            trek(p);
        }
        wegwijs(p);
    }

    /** The first time the story runs for this player: the tab opens and they hear how to follow it. */
    private static void wegwijs(ServerPlayer p) {
        if (!Ring.begonnen(p) || GuhQuests.saved(p).getBooleanOr(WEGWIJS, false)) {
            return;
        }
        GuhQuests.saved(p).putBoolean(WEGWIJS, true);
        GidsFeature.grant(p, "knabbelring/root");
        Ring.behaald(p, "ring_wegwijs");
        p.sendSystemMessage(Component.translatable("quest.guhs.ring.wegwijs").withStyle(ChatFormatting.GOLD));
    }

    /** The ring whispers (only its bearer hears it). */
    private static void trek(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long volgende = TREK.get(p.getUUID());
        if (volgende == null) {
            TREK.put(p.getUUID(), nu + TREK_MIN / 2 + p.getRandom().nextInt(TREK_SPREIDING));
        } else if (nu >= volgende && !nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p)) {
            TREK.put(p.getUUID(), nu + TREK_MIN + p.getRandom().nextInt(TREK_SPREIDING));
            p.sendSystemMessage(Component.translatable("quest.guhs.ring.trek." + p.getRandom().nextInt(TREK_REGELS)).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    /**
     * (GuhHooks.tick, every guh, server) a wild guh that smells the ring trots after its bearer, drooling. Only a real guh
     * (entity type guhs:guh) does: the stand-ins and game guhs that other features build on GuhEntity (the sleeper in a
     * Guhhuisje's room, the Guhmon and Boer zoekt Guh stand-ins, the crew of Among Guhs, race and parade guhs) are
     * nobody's guh either, and they stay where their own feature put them.
     */
    static void guhTick(GuhEntity guh) {
        Sam.guhTick(guh);
        if (DRAGERS.isEmpty() || (guh.tickCount + guh.getId()) % 20 != 0 || guh.isTame() || guh.isRemoved() || VerhaalGuhs.isKopie(guh)
                || guh.getType() != nl.juiced.guhs.registry.ModEntities.GUH.get()
                || GuhHooks.isBewoner(guh) || GuhHooks.isBezig(guh) || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        Player dichtst = level.getNearestPlayer(guh, KWIJL_AFSTAND);
        if (!(dichtst instanceof ServerPlayer p) || !DRAGERS.contains(p.getUUID()) || Ring.om(p) || p.isSpectator()) {
            return;
        }
        guh.getLookControl().setLookAt(p, 30f, 30f);
        if (guh.distanceToSqr(p) > 9) {
            guh.getNavigation().moveTo(p, 1.0);
        }
        Vec3 snoet = guh.position().add(Vec3.directionFromRotation(0, guh.getYHeadRot()).scale(0.55 * guh.getGuhScale())).add(0, 0.35 * guh.getGuhScale(), 0);
        level.sendParticles(ParticleTypes.FALLING_WATER, snoet.x, snoet.y, snoet.z, 2, 0.05, 0.02, 0.05, 0);
        if (guh.getRandom().nextInt(6) == 0) {
            Ring.behaald(p, "ring_kwijlen");
        }
    }

    /** Anybody who talks to a Guhdalf (whatever role he has there) has "met Guhdalf": the tab of the story opens. */
    @SubscribeEvent
    public static void onKlik(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer p && event.getTarget() instanceof GuhNpcEntity npc
                && npc.getKind() == GuhNpcEntity.Kind.GUHDALF) {
            Ring.behaald(p, "ring_guhdalf");
            GidsFeature.grant(p, "knabbelring/root");
        }
    }

    /** A Mika looks straight through whoever wears the ring. */
    @SubscribeEvent
    public static void onDoelwit(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && Ring.onzichtbaarVoorMikas(p) && Mikas.isMika(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ruimOp(p);
            Sam.vergeet(p);
            Smikagol.vergeet(p);
            TREK.remove(p.getUUID());
            DRAGERS.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDood(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ruimOp(p);
        }
    }

    @SubscribeEvent
    public static void onDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ruimOp(p);
            if (Ring.verhaalWereld(p.level())) {
                Rustpunt.aangekomen(p);
            }
        }
    }

    /** The ring is off, no rock, no rope, no lamp block, no hunt. */
    private static void ruimOp(ServerPlayer p) {
        if (Ring.om(p)) {
            Ring.doeOm(p, false);
        }
        Ring.vergeet(p.getUUID());
        Gaven.vergeet(p);
        Negen.einde(p);
    }

    /** What was saved with a chunk but belongs to a moment that is over (a rock, a hunter): it never comes back. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() || event.getLevel().isClientSide()) {
            return;
        }
        Entity e = event.getEntity();
        if (Gaven.isWeesRots(e) || Negen.isWees(e)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGestopt(ServerStoppedEvent event) {
        Gaven.wisAlles(null);   // (every player logged out before this: their lamps are out)
        Ring.wisAlles();
        Negen.wisAlles();
        Sam.wisAlles();
        Smikagol.wisAlles();
        TREK.clear();
        DRAGERS.clear();
    }

    /** (tests) the ring whispers at the next second. */
    static void zetTrekNu(ServerPlayer p) {
        TREK.put(p.getUUID(), p.level().getGameTime());
    }

    private RingEvents() {
    }
}
