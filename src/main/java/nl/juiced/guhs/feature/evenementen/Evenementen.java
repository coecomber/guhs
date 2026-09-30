package nl.juiced.guhs.feature.evenementen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Runs the guh events: the running ones ({@link #tick}), the scheduler (every player in the Guhmension gets one about
 * every 2 to 3 Minecraft days of their own time there, see {@link #scheduleTick}), the op command
 * {@code /guhs evenement <kaasregen|parade|sterrenregen|stop|wanneer>}, the cleaning up (logout, death, another
 * dimension, server stop, leftovers after a restart) and the easier taming of happy and starry guhs.
 */
public final class Evenementen {
    /** The scheduler: between 2 and 3 Minecraft days of Guhmension time between two events (the first one sooner). */
    public static final int MIN_DELAY = 24000 * 2, MAX_DELAY = 24000 * 3, FIRST_MIN = 24000, FIRST_MAX = 24000 * 2;
    /** Saved per player (GuhQuests.saved: survives dying). */
    static final String WAITED = "guhs_evenement_wacht", NEXT = "guhs_evenement_na", LAST = "guhs_evenement_vorige",
            SEEN = "guhs_evenementen_gezien";
    /** On a guh: happy after eating from the kaasregen until this game time; a starry guh from the sky: tameable until then. */
    static final String BLIJ = "guhs_kaasregen_blij", STER = "guhs_sterrenguh";
    /** 1 in this many knabbels tames a happy or starry guh straight away (on top of its own chance). */
    public static final int BOOST_CHANCE = 2;

    private static final List<Evenement> ACTIVE = new CopyOnWriteArrayList<>();

    private Evenementen() {
    }

    public static List<Evenement> active() {
        return List.copyOf(ACTIVE);
    }

    /** The event this player takes part in (null: none). */
    @Nullable
    public static Evenement eventOf(ServerPlayer player) {
        for (Evenement event : ACTIVE) {
            if (!event.isEnded() && event.takesPart(player)) {
                return event;
            }
        }
        return null;
    }

    /** Is this entity part of a running event? */
    public static boolean owns(Entity entity) {
        for (Evenement event : ACTIVE) {
            if (!event.isEnded() && event.owns(entity)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    static Evenement near(ServerLevel level, Vec3 pos, double range) {
        for (Evenement event : ACTIVE) {
            if (!event.isEnded() && event.level == level && event.center().distanceTo(pos) < range) {
                return event;
            }
        }
        return null;
    }

    // --- starting --------------------------------------------------------------------------------------------------------

    /** Why this player can't have an event right here (null: they can). */
    @Nullable
    public static Component whyNot(ServerPlayer player) {
        if (player.level().dimension() != ModDimensions.GUHMENSION) {
            return Component.translatable("gui.guhs.evenement.not_here");
        }
        if (eventOf(player) != null) {
            return Component.translatable("gui.guhs.evenement.already");
        }
        if (Minigames.playing(player) != null) {
            return Component.translatable("gui.guhs.evenement.busy");
        }
        if (!outdoors(player.level(), player.blockPosition())) {
            return Component.translatable("gui.guhs.evenement.indoors");
        }
        return null;
    }

    /**
     * Starts an event around this player (without the checks of {@link #whyNot}: those are for the caller). Null when
     * it can't (a parade that finds no room for its route).
     */
    @Nullable
    public static Evenement start(EvenementType type, ServerPlayer anchor) {
        ServerLevel level = anchor.level();
        Evenement event = switch (type) {
            case KAASREGEN -> new Kaasregen(level, anchor.position());
            case STERRENREGEN -> new Sterrenregen(level, anchor.position());
            case PARADE -> {
                ParadeRoute route = Vadsparade.planNear(level, anchor.position(), level.getRandom());
                yield route == null ? null : new Vadsparade(level, route);
            }
            case KNUSFEEST -> nl.juiced.guhs.feature.knuffeldal.KnusfeestEvenement.maak(level, anchor);
        };
        if (event == null) {
            return null;
        }
        begin(event, anchor);
        return event;
    }

    /** Runs an event that's been made already (tests make their own), with this player as its first participant. */
    public static void begin(Evenement event, ServerPlayer first) {
        ACTIVE.add(event);
        event.join(first);
        event.begin();
        event.level.playSound(null, first.getX(), first.getY(), first.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.AMBIENT, 1f, 1.2f);
        event.level.playSound(null, first.getX(), first.getY(), first.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.2f, 1f);
    }

    /** A player joined an event: their own timer starts over, and it counts for the quests. */
    static void joined(ServerPlayer player, Evenement event) {
        CompoundTag data = GuhQuests.saved(player);
        data.putInt(WAITED, 0);
        data.putInt(NEXT, nextDelay(player.getRandom(), false));
        data.putInt(LAST, event.type.ordinal() + 1);
        int seen = data.getIntOr(SEEN, 0) | (1 << event.type.ordinal());
        data.putInt(SEEN, seen);
        GuhAdvancements.grant(player, "evenement_eerste");
        int all = 0;
        for (EvenementType type : EvenementType.RANDOM) {   // (the seasonal Knusfeest doesn't count: it isn't a random event)
            all |= 1 << type.ordinal();
        }
        if ((seen & all) == all) {
            GuhAdvancements.grant(player, "evenement_alle");
        }
    }

    /** Stops every running event (and cleans up after it). */
    public static void endAll() {
        for (Evenement event : ACTIVE) {
            event.end(false);
        }
        ACTIVE.clear();
    }

    public static void tick(ServerTickEvent.Post event) {
        for (Evenement e : ACTIVE) {
            e.tick();
            if (e.isEnded()) {
                ACTIVE.remove(e);
            }
        }
    }

    // --- the scheduler ----------------------------------------------------------------------------------------------------

    /** How long until the next event: fair and random, between 2 and 3 days (the very first one: 1 to 2 days). */
    public static int nextDelay(RandomSource random, boolean first) {
        return first ? FIRST_MIN + random.nextInt(FIRST_MAX - FIRST_MIN + 1) : MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            scheduleTick(player, 20);
        }
    }

    /**
     * Counts this player's time in the Guhmension (not while they're in an event); when it's their turn, they get an
     * event: they join one that's already going on nearby, or a new one starts around them (only outdoors, not while
     * playing a minigame: then it waits and tries again). Returns the event it started or joined.
     */
    @Nullable
    public static Evenement scheduleTick(ServerPlayer player, int ticks) {
        if (player.level().dimension() != ModDimensions.GUHMENSION || player.isSpectator() || !player.isAlive() || eventOf(player) != null) {
            return null;
        }
        CompoundTag data = GuhQuests.saved(player);
        if (!data.contains(NEXT)) {
            data.putInt(NEXT, nextDelay(player.getRandom(), true));
        }
        int next = data.getIntOr(NEXT, 0);
        int waited = Math.min(next, data.getIntOr(WAITED, 0) + ticks);
        data.putInt(WAITED, waited);
        if (waited < next) {
            return null;
        }
        Evenement nearby = near(player.level(), player.position(), Evenement.JOIN_RANGE);
        if (nearby != null && nearby.autoJoin) {
            nearby.join(player);
            return nearby;
        }
        if (whyNot(player) != null) {
            return null; // not now: try again in a moment
        }
        int last = data.getIntOr(LAST, 0) - 1;
        EvenementType type = EvenementType.choose(player.getRandom(), player.level().isDarkOutside(),
                last >= 0 && last < EvenementType.values().length ? EvenementType.values()[last] : null);
        Evenement started = start(type, player);
        if (started == null) {
            started = start(EvenementType.KAASREGEN, player); // no room for a parade here: it rains knabbels instead
        }
        return started;
    }

    /** How long this player still has to wait for their next event (in ticks of Guhmension time). */
    public static int waitLeft(ServerPlayer player) {
        CompoundTag data = GuhQuests.saved(player);
        return data.contains(NEXT) ? Math.max(0, data.getIntOr(NEXT, 0) - data.getIntOr(WAITED, 0)) : -1;
    }

    // --- where events can happen -------------------------------------------------------------------------------------------

    /** Outdoors: nothing but sky (and leaves) above, and not in a guh building. */
    public static boolean outdoors(ServerLevel level, BlockPos feet) {
        if (!level.hasChunk(feet.getX() >> 4, feet.getZ() >> 4)) {
            return false;
        }
        return feet.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX(), feet.getZ())
                && !Protected.at(level, feet) && !Protected.at(level, feet.below());
    }

    /**
     * The spot on top of the ground at this column (where feet would be), if something may land or walk there: loaded,
     * outdoors by definition, not on or in a guh building, no lava (and no water at all when {@code dry}), and not
     * much higher or lower than {@code nearY}.
     */
    @Nullable
    public static BlockPos ground(ServerLevel level, int x, int z, double nearY, boolean dry) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY() || Math.abs(y - nearY) > 10) {
            return null;
        }
        BlockPos feet = new BlockPos(x, y, z);
        FluidState fluid = level.getFluidState(feet.below());
        if (fluid.is(FluidTags.LAVA) || dry && !fluid.isEmpty() || !level.getFluidState(feet).isEmpty()) {
            return null;
        }
        if (Protected.at(level, feet) || Protected.at(level, feet.below())) {
            return null;
        }
        return feet;
    }

    // --- easier taming ----------------------------------------------------------------------------------------------------

    /**
     * A wild, ordinary guh (no parade guh, no race guh, not hidden by Verstopguhtje, and not the one and only Wolkguh:
     * that one only trusts you after a lot of patience, see the eilanden feature).
     */
    public static boolean wild(GuhEntity guh) {
        return guh.getType() == ModEntities.GUH.get() && !guh.isTame() && guh.getHiddenBy() == null && guh.isAlive()
                && guh.getVariant() != nl.juiced.guhs.entity.GuhVariant.WOLK;
    }

    /** It ate a knabbel from the kaasregen: happy (and easy to tame) for this long. */
    public static void makeHappy(GuhEntity guh, int ticks) {
        long until = guh.level().getGameTime() + ticks;
        guh.getPersistentData().putLong(BLIJ, Math.max(until, guh.getPersistentData().getLongOr(BLIJ, 0L)));
    }

    /** Happy from the kaasregen, or a starry guh fresh from the sky: easier to tame right now. */
    public static boolean boosted(GuhEntity guh) {
        long now = guh.level().getGameTime();
        return guh.getPersistentData().getLongOr(BLIJ, 0L) > now || guh.getPersistentData().getLongOr(STER, 0L) > now;
    }

    /** Feeding a boosted guh a kaas knabbel: now and then it's tamed at once (otherwise its own chance still follows). */
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof GuhEntity guh) || !wild(guh) || !boosted(guh)
                || !event.getItemStack().is(ModItems.KAAS_KNABBELS.get()) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (guh.getRandom().nextInt(BOOST_CHANCE) != 0) {
            return;
        }
        event.getItemStack().consume(1, player);
        tameNow(guh, player);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Tames this wild guh for this player right away (unless another mod or feature says no); true if it worked. */
    public static boolean tameNow(GuhEntity guh, ServerPlayer player) {
        guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
        if (net.neoforged.neoforge.event.EventHooks.onAnimalTame(guh, player)) {
            guh.level().broadcastEntityEvent(guh, (byte) 6); // smoke: not this time
            return false;
        }
        guh.tame(player);
        guh.getNavigation().stop();
        guh.setTarget(null);
        guh.level().broadcastEntityEvent(guh, (byte) 7); // hearts
        guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
        guh.triggerAnim("action", "happy");
        return true;
    }

    // --- cleaning up ------------------------------------------------------------------------------------------------------

    /** Leaving the game, the dimension or this life: out of the event (an event without anyone left stops). */
    static void leave(ServerPlayer player) {
        for (Evenement event : ACTIVE) {
            if (event.takesPart(player)) {
                event.drop(player.getUUID());
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    /** A starry guh from a sterrenregen that's long over (saved while its chunk was unloaded): back to the stars. */
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof GuhEntity guh) || !guh.getPersistentData().contains(STER)) {
            return;
        }
        if (guh.isTame()) {
            guh.getPersistentData().remove(STER);
            guh.setGlowingTag(false);
        } else if (event.loadedFromDisk() && !owns(guh)) {
            event.setCanceled(true);
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        endAll();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    // --- /guhs evenement ---------------------------------------------------------------------------------------------------

    public static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> evenement = Commands.literal("evenement").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (EvenementType type : EvenementType.values()) {
            evenement.then(Commands.literal(type.id()).executes(c -> commandStart(c.getSource(), type)));
        }
        evenement.then(Commands.literal("stop").executes(c -> commandStop(c.getSource())));
        evenement.then(Commands.literal("wanneer").executes(c -> commandWhen(c.getSource())));
        event.getDispatcher().register(Commands.literal("guhs").then(evenement));
    }

    private static int commandStart(CommandSourceStack source, EvenementType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Component why = whyNot(player);
        if (why != null) {
            source.sendFailure(why);
            return 0;
        }
        Evenement event = start(type, player);
        if (event == null) {
            source.sendFailure(Component.translatable("gui.guhs.evenement.no_route"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("gui.guhs.evenement.command.started", type.displayName(), Evenement.time(event.duration()))
                .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int commandStop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Evenement event = eventOf(player);
        if (event == null) {
            source.sendFailure(Component.translatable("gui.guhs.evenement.command.none"));
            return 0;
        }
        event.end(false);
        ACTIVE.remove(event);
        player.level().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, 0.5, 0.5, 0.02);
        source.sendSuccess(() -> Component.translatable("gui.guhs.evenement.command.stopped", event.type.displayName())
                .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int commandWhen(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int left = waitLeft(player);
        source.sendSuccess(() -> left < 0 ? Component.translatable("gui.guhs.evenement.command.when_unknown")
                : Component.translatable("gui.guhs.evenement.command.when", String.format("%.1f", left / 24000.0), Evenement.time(left)), false);
        return 1;
    }
}
