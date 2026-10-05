package nl.juiced.guhs.feature.verhaal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.network.ModNetworking;

/**
 * bbq2 (verhaal engine): the lock of a player who watches a cutscene or reads a narrator card. While it holds, the player
 * cannot be hurt, pushed, moved, mounted or teleported by anything else, takes no fall, is not chased by mobs, cannot open
 * screens, use items or hit anything; the server puts them back on their spot every tick. It ends when the client reports
 * the end (not before {@code min} ticks), at the time-out, or without its {@code daarna} when the player logs out, dies or
 * changes dimension (so the story step, which is only set in {@code daarna}, replays the scene next time). A short
 * after-care keeps the player safe for {@link #NAZORG} ticks after it ended.
 */
final class Vast {
    /** Ticks of protection (no damage, no fall) after a lock ended. */
    static final int NAZORG = 40;
    /** A player whose client cannot receive the scene (game-test mock players) "watches" for this many ticks. */
    static final int MOCK_TICKS = 2;

    /** One lock: what is shown, where the player is held, when the client may end it and when it ends by itself. */
    static final class Slot {
        final String soort, id;
        final ResourceKey<Level> dim;
        final Vec3 plek;
        final float yaw, pitch;
        final long begin;
        final int min, max;
        final boolean ontvangt;
        /** Runs on a normal end (not on a break-off). */
        final Consumer<ServerPlayer> einde;

        Slot(ServerPlayer p, String soort, String id, int min, int max, boolean ontvangt, Consumer<ServerPlayer> einde) {
            this.soort = soort;
            this.id = id;
            this.dim = p.level().dimension();
            this.plek = grond(p);
            this.yaw = p.getYRot();
            this.pitch = p.getXRot();
            this.begin = p.level().getServer().getTickCount();
            this.min = min;
            this.max = max;
            this.ontvangt = ontvangt;
            this.einde = einde;
        }
    }

    private static final Map<UUID, Slot> SLOTEN = new ConcurrentHashMap<>();
    /** Until which server tick a player is still looked after. */
    private static final Map<UUID, Long> ZORG = new ConcurrentHashMap<>();
    /** Client: the local player is watching (set by the client player of scenes and cards). */
    static volatile boolean clientBezig;

    static boolean is(Player p) {
        return p.level().isClientSide() ? clientBezig && p.isLocalPlayer() : SLOTEN.containsKey(p.getUUID());
    }

    @Nullable
    static Slot van(ServerPlayer p) {
        return SLOTEN.get(p.getUUID());
    }

    /** Still in the after-care of a lock (or locked)? */
    static boolean veilig(Player p) {
        if (SLOTEN.containsKey(p.getUUID())) {
            return true;
        }
        Long tot = ZORG.get(p.getUUID());
        return tot != null && p.level().getServer() != null && p.level().getServer().getTickCount() <= tot;
    }

    /** Can this player's client show our scenes (false for the mock players of the game tests)? */
    static boolean ontvangt(ServerPlayer p) {
        return !(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && p.connection != null
                && p.connection.hasChannel(VerhaalPayloads.Speel.TYPE);
    }

    /** Locks the player (false: already locked, or not alive). */
    static boolean zet(ServerPlayer p, Slot s) {
        if (SLOTEN.containsKey(p.getUUID()) || !p.isAlive()) {
            return false;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.ejectPassengers();
        if (p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
        p.stopUsingItem();
        // whoever was after them forgets them
        for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32))) {
            if (mob.getTarget() == p) {
                mob.setTarget(null);
            }
        }
        SLOTEN.put(p.getUUID(), s);
        houd(p, s);
        return true;
    }

    /** The spot a player is held on: where they stand, or the ground right below when they are in the air. */
    private static Vec3 grond(ServerPlayer p) {
        Vec3 pos = p.position();
        if (p.onGround() || p.getAbilities().flying || p.isInWater()) {
            return pos;
        }
        ServerLevel level = p.level();
        for (double d = 0.25; d <= 6.0; d += 0.25) {
            if (!level.noCollision(p, p.getBoundingBox().move(0, -d, 0))) {
                return pos.add(0, -(d - 0.25), 0);
            }
        }
        return pos;
    }

    private static void houd(ServerPlayer p, Slot s) {
        if (p.position().distanceToSqr(s.plek) > 0.0004) {
            p.connection.teleport(s.plek.x, s.plek.y, s.plek.z, s.yaw, s.pitch);
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        p.clearFire();
        p.setAirSupply(p.getMaxAirSupply());
    }

    /** The client says the scene or card is over (ignored when it is not this one, or too early). */
    static void klaar(ServerPlayer p, String soort, String id) {
        Slot s = SLOTEN.get(p.getUUID());
        if (s != null && s.soort.equals(soort) && s.id.equals(id) && p.level().getServer().getTickCount() - s.begin >= s.min) {
            eindig(p, s);
        }
    }

    private static void eindig(ServerPlayer p, Slot s) {
        if (!SLOTEN.remove(p.getUUID(), s)) {
            return;
        }
        ZORG.put(p.getUUID(), (long) p.level().getServer().getTickCount() + NAZORG);
        p.fallDistance = 0;
        s.einde.accept(p);
    }

    /** Ends the lock without its daarna (logout, death, another dimension); the client is told to stop when it still listens. */
    static void breekAf(ServerPlayer p, boolean stuurStop) {
        Slot s = SLOTEN.remove(p.getUUID());
        if (s == null) {
            return;
        }
        if (p.level().getServer() != null) {
            ZORG.put(p.getUUID(), (long) p.level().getServer().getTickCount() + NAZORG);
        }
        if (stuurStop) {
            ModNetworking.sendTo(p, new VerhaalPayloads.Stop(s.soort, s.id));
        }
    }

    // =====================================================================================================================
    // events (registered by VerhaalFeature)
    // =====================================================================================================================

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Slot s = SLOTEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        if (!p.isAlive() || p.level().dimension() != s.dim) {
            breekAf(p, true);
            return;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        if (p.isVehicle()) {
            p.ejectPassengers();
        }
        if (p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
        houd(p, s);
        long duurt = p.level().getServer().getTickCount() - s.begin;
        if (duurt >= s.max || !s.ontvangt && duurt >= MOCK_TICKS) {
            if (s.ontvangt) {
                ModNetworking.sendTo(p, new VerhaalPayloads.Stop(s.soort, s.id));   // (the client never said it was over)
            }
            eindig(p, s);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && veilig(p) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInvulnerable(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && veilig(p) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && veilig(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer p && is(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && (event.getEntityMounting() instanceof ServerPlayer a && is(a)
                || event.getEntityBeingMounted() instanceof ServerPlayer b && is(b))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSpread(EntityTeleportEvent.SpreadPlayersCommand event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemTeleport(EntityTeleportEvent.ItemConsumption event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseEntity2(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            breekAf(p, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            breekAf(p, false);
            ZORG.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            breekAf(p, true);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        SLOTEN.clear();
        ZORG.clear();
    }

    private Vast() {
    }
}
