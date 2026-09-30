package nl.juiced.guhs.feature.piep;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.world.ModDimensions;

/** Piep's game events: the muisje spawn hook, verstoppertje clicks, the shoulder, the nest fights, tidying up. */
public final class PiepEvents {
    /** Tag of the floating "zieli..." texts (removed if one is ever loaded from disk). */
    public static final String ZIELI_TAG = "guhs_piep_zieli";

    private record Kans(ServerLevel level, BlockPos pos, long seed) {
    }

    private record Weg(Entity entity, long tot) {
    }

    private static final ConcurrentLinkedQueue<Kans> KANSEN = new ConcurrentLinkedQueue<>();
    private static final List<Weg> WEG = new CopyOnWriteArrayList<>();

    private PiepEvents() {
    }

    /** Removes this entity after a few ticks (the zieli texts). */
    static void weg(Entity entity, int ticks) {
        WEG.add(new Weg(entity, entity.level().getGameTime() + ticks));
    }

    // --- joining the world ---------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity e = event.getEntity();
        if (e.getTags().contains(ZIELI_TAG) && event.loadedFromDisk()) {
            event.setCanceled(true);
            return;
        }
        if (e instanceof BozeKaasknabbelEntity k && !k.nest().isEmpty() && event.loadedFromDisk() && !KaasknabbelNest.hoortBijGevecht(level, k)) {
            event.setCanceled(true);                            // a knabbel of a fight that is over: back into its hole
            return;
        }
        // the muisje hook: every guh a structure template puts into the Guhmensie has one chance
        if ((e instanceof GuhEntity || e instanceof GuhNpcEntity) && level.dimension() == ModDimensions.GUHMENSION
                && ((Mob) e).getSpawnType() == MobSpawnType.STRUCTURE && !e.getPersistentData().getBoolean(PiepSpawns.GEHAD)) {
            e.getPersistentData().putBoolean(PiepSpawns.GEHAD, true);
            if (e instanceof GuhEntity guh && (guh.getTags().contains("guhs_caged_guhbert") || guh.getVariant() == nl.juiced.guhs.entity.GuhVariant.MAGER)) {
                return;
            }
            KANSEN.add(new Kans(level, e.blockPosition(), e.getUUID().getLeastSignificantBits()));
        }
    }

    // --- every tick ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Kans k; (k = KANSEN.poll()) != null; ) {
            if (k.level().isLoaded(k.pos())) {
                PiepSpawns.maybeAddMuisje(k.level(), k.pos(), net.minecraft.util.RandomSource.create(k.seed() ^ k.level().getGameTime()));
            }
        }
        for (Weg w : WEG) {
            if (!w.entity().isAlive() || w.entity().level().getGameTime() >= w.tot()) {
                w.entity().discard();
                WEG.remove(w);
            }
        }
        KaasknabbelNest.tick();
        var server = event.getServer();
        if (server.getTickCount() % 20 == 0) {
            ServerLevel guhmensie = server.getLevel(ModDimensions.GUHMENSION);
            if (guhmensie != null) {
                for (ServerPlayer p : guhmensie.players()) {
                    KaasknabbelNest.spelerTick(p);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        KANSEN.clear();
        WEG.clear();
        KaasknabbelNest.vergeet();
    }

    // --- clicks ---------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos pos = event.getPos();
        // verstoppertje: is a muisje hiding here?
        for (PieppiepmuisjeEntity muis : player.level().getEntitiesOfClass(PieppiepmuisjeEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2))) {
            if (muis.isVerstopt() && muis.verstop().bij(pos)) {
                muis.verstop().gevonden(player);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        // sneak + empty hand on a block: the muisje on your shoulder hops down
        if (player.isSecondaryUseActive() && player.getMainHandItem().isEmpty() && Schouder.heeft(player)) {
            BlockPos at = pos.relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace());
            Schouder.eraf(player, new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // --- the shoulder ----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher && Schouder.heeft(target)) {
            nl.juiced.guhs.network.ModNetworking.sendTo(watcher, Schouder.bericht(target));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Schouder.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Schouder.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Schouder.sync(player);
        }
    }

    /**
     * /guhs piep schouder | verstop | wiebel (op, for testing and the autocheck): a new tame muisje on your shoulder; the
     * nearest tame muisje hides now; the nearest guh wiggles as if Poepschilly were inside.
     */
    @SubscribeEvent
    public static void onCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        var piep = net.minecraft.commands.Commands.literal("piep").requires(s -> s.hasPermission(2))
                .then(net.minecraft.commands.Commands.literal("schouder").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PieppiepmuisjeEntity muis = PiepFeature.PIEPPIEPMUISJE.get().create(p.serverLevel());
                    if (muis == null || Schouder.heeft(p)) {
                        return 0;
                    }
                    muis.tame(p);
                    Schouder.zet(p, muis);
                    return 1;
                }))
                .then(net.minecraft.commands.Commands.literal("verstop").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (PieppiepmuisjeEntity muis : p.level().getEntitiesOfClass(PieppiepmuisjeEntity.class, p.getBoundingBox().inflate(16))) {
                        if (muis.isTame() && !muis.isVerstopt() && muis.verstop().kiesPlek()) {
                            muis.verstop().verstop();
                            return 1;
                        }
                    }
                    return 0;
                }))
                .then(net.minecraft.commands.Commands.literal("wiebel").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhEntity guh = p.level().getNearestEntity(GuhEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(),
                            p, p.getX(), p.getY(), p.getZ(), p.getBoundingBox().inflate(16));
                    if (guh == null) {
                        return 0;
                    }
                    PiepPayloads.naarKijkers(guh, new PiepPayloads.Wiebel(guh.getId(), 400));
                    return 1;
                }));
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("guhs").then(piep));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Schouder.heeft(player)) {
            Schouder.eraf(player, player.position());          // it jumps off (and stays where you fell)
        }
    }
}
