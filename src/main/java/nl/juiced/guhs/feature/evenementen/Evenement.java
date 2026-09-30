package nl.juiced.guhs.feature.evenementen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * One running guh event (see {@link EvenementType}): where it is, who takes part (they see its boss bar with the time
 * left) and every entity it made. Players close by join in by themselves; whoever logs out, dies, leaves the dimension
 * or walks far away drops out, and an event without anyone left stops at once. When it ends (for whatever reason)
 * everything it made is cleaned up: no entities, items or boss bars stay behind. Events never place or break blocks.
 */
public abstract class Evenement {
    /** Players this close to an event join it; further than {@link #LEAVE_RANGE} away they drop out. */
    public static final double JOIN_RANGE = 48, LEAVE_RANGE = 96;

    public final EvenementType type;
    public final ServerLevel level;
    protected Vec3 center;
    protected int age;
    protected int duration;
    /** Tests switch this off: then only the players it is started with take part. */
    public boolean autoJoin = true;
    /** Tests switch this off too: then nothing falls or turns up by itself (they drop their own knabbels and stars). */
    public boolean spontaneous = true;
    final Set<UUID> participants = new LinkedHashSet<>();
    /** Everything this event made (falling knabbels, stars, parade guhs, starry guhs): gone when it ends. */
    final Set<UUID> entities = new HashSet<>();
    final ServerBossEvent bar;
    private boolean ended;

    protected Evenement(EvenementType type, ServerLevel level, Vec3 center, int duration) {
        this.type = type;
        this.level = level;
        this.center = center;
        this.duration = duration;
        this.bar = new ServerBossEvent(java.util.UUID.randomUUID(), type.displayName(), type.bar, BossEvent.BossBarOverlay.NOTCHED_10);
    }

    public Vec3 center() {
        return center;
    }

    public int age() {
        return age;
    }

    public int duration() {
        return duration;
    }

    public int timeLeft() {
        return Math.max(0, duration - age);
    }

    public boolean isEnded() {
        return ended;
    }

    public Set<UUID> participants() {
        return java.util.Collections.unmodifiableSet(participants);
    }

    public boolean takesPart(Player player) {
        return participants.contains(player.getUUID());
    }

    /** Did this event make that entity? */
    public boolean owns(Entity entity) {
        return entities.contains(entity.getUUID());
    }

    /** The boss bar (tests look at it). */
    public ServerBossEvent bar() {
        return bar;
    }

    /** The participants that are still here. */
    public List<ServerPlayer> players() {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : participants) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer player) {
                list.add(player);
            }
        }
        return list;
    }

    // --- life cycle --------------------------------------------------------------------------------------------------

    /** Right after it's started (the first player has joined): spawn what it needs. */
    protected void begin() {
    }

    /** One tick of the event itself. */
    protected abstract void tickEvent();

    /** It's over: the last messages and rewards (the participants are still there). */
    protected abstract void finish(boolean completed);

    /** Cleaning up one of its own entities (default: gone). */
    protected void cleanUp(Entity entity) {
        entity.discard();
    }

    /** Called every server tick by {@link Evenementen}. */
    final void tick() {
        if (ended) {
            return;
        }
        if (age % 10 == 0) {
            updateParticipants();
        }
        if (participants.isEmpty()) {
            end(false);
            return;
        }
        tickEvent();
        if (ended) {
            return;
        }
        age++;
        if (age % 20 == 1) {
            updateBar();
        }
        if (age >= duration) {
            end(true);
        }
    }

    /** Stops the event (completed: its time ran out normally) and cleans everything up. */
    public final void end(boolean completed) {
        if (ended) {
            return;
        }
        ended = true;
        try {
            finish(completed);
        } finally {
            for (UUID id : List.copyOf(entities)) {
                Entity entity = level.getEntity(id);
                if (entity != null && !entity.isRemoved()) {
                    cleanUp(entity);
                }
            }
            bar.removeAllPlayers();
            bar.setVisible(false);
            participants.clear();
        }
    }

    // --- who takes part ----------------------------------------------------------------------------------------------

    private void updateParticipants() {
        for (UUID id : List.copyOf(participants)) {
            Player player = level.getPlayerByUUID(id);
            if (!(player instanceof ServerPlayer sp) || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
                drop(id);
            } else if (player.position().distanceTo(center) > LEAVE_RANGE) {
                drop(id);
                sp.sendSystemMessage(Component.translatable("gui.guhs.evenement.left", type.displayName()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (!autoJoin || participants.isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!participants.contains(player.getUUID()) && player.isAlive() && !player.isSpectator()
                    && player.position().distanceTo(center) < JOIN_RANGE && Evenementen.eventOf(player) == null) {
                join(player);
            }
        }
    }

    /** A player takes part: the boss bar, a hello in the chat. */
    public void join(ServerPlayer player) {
        if (ended || !participants.add(player.getUUID())) {
            return;
        }
        bar.addPlayer(player);
        updateBar();
        player.sendSystemMessage(announce(Component.translatable("gui.guhs.evenement.start." + type.id())));
        Evenementen.joined(player, this);
    }

    /** A player drops out (logged out, died, went away): no boss bar any more. */
    public void drop(UUID id) {
        if (participants.remove(id) && level.getPlayerByUUID(id) instanceof ServerPlayer player) {
            bar.removePlayer(player);
        } else {
            for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
                if (player.getUUID().equals(id)) {
                    bar.removePlayer(player);
                }
            }
        }
    }

    // --- the boss bar and the chat -------------------------------------------------------------------------------------

    protected void updateBar() {
        bar.setName(Component.translatable("gui.guhs.evenement.bar", type.displayName(), time(timeLeft())));
        bar.setProgress(duration <= 0 ? 0f : Math.max(0f, Math.min(1f, timeLeft() / (float) duration)));
    }

    /** "1:05" */
    public static String time(int ticks) {
        int seconds = (ticks + 19) / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    /** A chat line in the event's colour, with a little star in front. */
    protected MutableComponent announce(Component text) {
        return Component.literal("✦ ").withStyle(type.colour).append(text.copy().withStyle(type.colour));
    }

    /** Tells every participant something in the chat. */
    protected void say(String key, Object... args) {
        for (ServerPlayer player : players()) {
            player.sendSystemMessage(announce(Component.translatable(key, args)));
        }
    }

    /** A spot near one of the participants (outdoors ones first), or the middle of the event. */
    protected Vec3 aroundSomeone() {
        List<ServerPlayer> outside = new ArrayList<>();
        for (ServerPlayer player : players()) {
            if (Evenementen.outdoors(level, player.blockPosition())) {
                outside.add(player);
            }
        }
        if (outside.isEmpty()) {
            return center;
        }
        return outside.get(level.getRandom().nextInt(outside.size())).position();
    }

    /** Moves the middle of the event to the middle of its participants. */
    protected void followParticipants() {
        List<ServerPlayer> list = players();
        if (list.isEmpty()) {
            return;
        }
        double x = 0, y = 0, z = 0;
        for (ServerPlayer player : list) {
            x += player.getX();
            y += player.getY();
            z += player.getZ();
        }
        center = new Vec3(x / list.size(), y / list.size(), z / list.size());
    }

    @Nullable
    protected ServerPlayer player(UUID id) {
        return level.getPlayerByUUID(id) instanceof ServerPlayer player && participants.contains(id) ? player : null;
    }
}
