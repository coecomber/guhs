package nl.juiced.guhs.feature.disco;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * The music of one Guhdisco (2.9): the real songs instead of the old note-block beat. Everybody in the club hears a
 * song from the dance floor: outside a game the approved 70's remix of "Ze hangen aan me vet" (started for you when you
 * come in, again when it's over), during a game the game's song for everyone at once (started with the game so the
 * dancer's beat and the tiles are in sync, and put on again when the song is through). Leaving the club, or the end of
 * a game, stops it. The sounds are streamed (sounds.json: stream, attenuation_distance), sent to each listener with
 * their own packet, so we know exactly who hears what since when.
 */
public final class DiscoMuziek {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final Map<DiscoLiedje, DeferredHolder<SoundEvent, SoundEvent>> LIEDJES = new EnumMap<>(DiscoLiedje.class);

    static {
        for (DiscoLiedje l : DiscoLiedje.values()) {
            LIEDJES.put(l, SOUNDS.register("disco." + l.id, () -> SoundEvent.createVariableRangeEvent(Guhs.id("disco." + l.id))));
        }
    }

    /** How far around the middle of the dance floor you hear the music (the club, and a bit of the plaza). */
    public static final double HEAR_XZ = 31, HEAR_Y = 10;
    /** The DJ takes a breath before he puts the standard record on again (after a game, or when it's over). */
    public static final int PAUZE = 50;

    /** Who hears what, since which game tick. */
    public record Luisteraar(DiscoLiedje liedje, long sinds) {
    }

    private final Map<UUID, Luisteraar> luisteraars = new HashMap<>();
    /** No idle music before this game tick (a breather after a game). */
    private long rustTot;

    @Nullable
    public Luisteraar luistert(UUID player) {
        return luisteraars.get(player);
    }

    public Map<UUID, Luisteraar> luisteraars() {
        return Map.copyOf(luisteraars);
    }

    /** Every few ticks: who's in the club hears the right song; who left hears nothing any more. */
    public void tick(ServerLevel world, BlockPos centre, @Nullable DiscoLiedje spel, long spelStart) {
        long now = world.getGameTime();
        List<ServerPlayer> near = world.getEntitiesOfClass(ServerPlayer.class, new AABB(centre).inflate(HEAR_XZ, HEAR_Y, HEAR_XZ));
        java.util.Set<UUID> here = new java.util.HashSet<>();
        for (ServerPlayer p : near) {
            here.add(p.getUUID());
            Luisteraar l = luisteraars.get(p.getUUID());
            if (spel != null) {
                if (l == null || l.liedje != spel || l.sinds < spelStart) {          // (late watchers: from the start)
                    speel(world, p, centre, spel, now);
                }
            } else if (now >= rustTot) {
                DiscoLiedje idle = DiscoLiedje.DISCO70;
                if (l == null || l.liedje != idle || now - l.sinds >= (long) Math.ceil(idle.seconds * 20) + PAUZE) {
                    speel(world, p, centre, idle, now);
                }
            } else if (l != null) {
                stop(world, p.getUUID(), l.liedje);
            }
        }
        for (Iterator<Map.Entry<UUID, Luisteraar>> it = luisteraars.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Luisteraar> e = it.next();
            if (!here.contains(e.getKey())) {                                    // walked out (or logged out, or died)
                ServerPlayer p = world.getServer().getPlayerList().getPlayer(e.getKey());
                if (p != null) {
                    stuurStop(p, e.getValue().liedje);
                }
                it.remove();
            }
        }
    }

    /** The game starts (or loops): the song from the start for everyone in the club, all at the same moment. */
    public void start(ServerLevel world, BlockPos centre, DiscoLiedje liedje) {
        long now = world.getGameTime();
        for (ServerPlayer p : world.getEntitiesOfClass(ServerPlayer.class, new AABB(centre).inflate(HEAR_XZ, HEAR_Y, HEAR_XZ))) {
            speel(world, p, centre, liedje, now);
        }
        // 2.10: tamed guhs on (or around) the dance floor hear the song
        for (nl.juiced.guhs.entity.GuhEntity guh : world.getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, new AABB(centre).inflate(16, 6, 16),
                nl.juiced.guhs.feature.band.Band::isBandGuh)) {
            nl.juiced.guhs.feature.band.Band.moment(guh, guh.getOwner() instanceof ServerPlayer owner && owner.level() == world ? owner : null,
                    nl.juiced.guhs.feature.band.Moment.LIEDJE, "disco:" + liedje.id);
        }
    }

    /** The game is over: the music stops for everybody, and the DJ takes a breath before the standard record. */
    public void stopAlles(ServerLevel world) {
        for (Map.Entry<UUID, Luisteraar> e : luisteraars.entrySet()) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                stuurStop(p, e.getValue().liedje);
            }
        }
        luisteraars.clear();
        rustTot = world.getGameTime() + PAUZE;
    }

    public void vergeet() {
        luisteraars.clear();
        rustTot = 0;
    }

    private void speel(ServerLevel world, ServerPlayer p, BlockPos centre, DiscoLiedje liedje, long now) {
        Luisteraar before = luisteraars.get(p.getUUID());
        if (before != null) {
            stuurStop(p, before.liedje);
        }
        luisteraars.put(p.getUUID(), new Luisteraar(liedje, now));
        if (p.connection != null) {
            p.connection.send(new ClientboundSoundPacket(LIEDJES.get(liedje), SoundSource.RECORDS, centre.getX() + 0.5, centre.getY() + 1.5,
                    centre.getZ() + 0.5, 1.0f, 1.0f, world.getRandom().nextLong()));
        }
    }

    private void stop(ServerLevel world, UUID id, DiscoLiedje liedje) {
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
        if (p != null) {
            stuurStop(p, liedje);
        }
        luisteraars.remove(id);
    }

    private static void stuurStop(ServerPlayer p, DiscoLiedje liedje) {
        if (p.connection != null) {
            p.connection.send(new ClientboundStopSoundPacket(Guhs.id("disco." + liedje.id), SoundSource.RECORDS));
        }
    }
}
