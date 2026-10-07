package nl.juiced.guhs.feature.bestaand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * bbq2 (bestaand): "schijn", what one player sees and nobody else. The quests of this slice happen at buildings that
 * every player shares and that are never reset, so nothing a player does there may change the world: a fire that one
 * player lit must still be cold for the next. Instead, the block state a player earned (a burning fire bowl, a pulled weed,
 * an empty cage, their plush guhs in the naaihoek) is SENT to that player alone, as a plain block update: their client
 * shows it, lights it and plays its particles, while the server's world, and so every other player, keeps the real block.
 * <p>
 * A {@link Bron} says what a player should see ({@link #registreer}); every {@link #TICKS} ticks each player's list is
 * made again and sent again, and what is no longer wanted goes back to the real block. A chunk that is sent to the client
 * (anew) gets its shown blocks right behind it ({@link #opChunk}), and a real update of a shown block asks for a refresh in
 * the player's next tick ({@link #opBlokUpdate}); whatever slips through shows the real block for at most a second. {@link #toon} shows one at once (right after the
 * click that earned it) and asks for a refresh in the player's next tick ({@link #straks}): the game answers every click
 * on a block with the REAL state of that block and of the one next to it, which would put a fire out again for up to a
 * second. Nothing is saved here: the sources read the player's questline flags.
 * <p>
 * Clicks on such a block reach the server as clicks on the REAL block at that spot (the interact events fire before the
 * server looks at the block), which is how a weed that only one player sees can still be pulled: {@link BestaandEvents}.
 */
public final class Schijn {
    /** Says which block states this player should see instead of the real ones (only spots near the player). */
    @FunctionalInterface
    public interface Bron {
        void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst);
    }

    /** A player's list is made and sent again once per this many ticks. */
    public static final int TICKS = 20;
    /** Only spots this near (blocks, per axis) are shown: further away the client has no chunk for them anyway. */
    public static final int BEREIK = 80;

    private static final List<Bron> BRONNEN = new CopyOnWriteArrayList<>();
    /** (not saved) player -> what was sent at the last refresh. */
    private static final Map<UUID, Map<BlockPos, BlockState>> GETOOND = new ConcurrentHashMap<>();
    /** (not saved) the players whose list is sent again in their next tick, whatever the clock says. */
    private static final Set<UUID> STRAKS = ConcurrentHashMap.newKeySet();

    private Schijn() {
    }

    public static void registreer(Bron bron) {
        BRONNEN.add(bron);
    }

    /** Is this spot near enough to this player to be shown? */
    public static boolean dichtbij(ServerPlayer p, BlockPos pos) {
        return Math.abs(pos.getX() - p.getBlockX()) <= BEREIK && Math.abs(pos.getZ() - p.getBlockZ()) <= BEREIK
                && Math.abs(pos.getY() - p.getBlockY()) <= BEREIK && p.level().isLoaded(pos);
    }

    /** What this player should see right now, asked of every source (nothing is sent). */
    public static Map<BlockPos, BlockState> gewenst(ServerPlayer p) {
        Map<BlockPos, BlockState> gewenst = new HashMap<>();
        for (Bron bron : BRONNEN) {
            bron.vul(p, gewenst);
        }
        return gewenst;
    }

    /** Makes this player's list again and sends it; what is no longer on it shows the real block again. */
    public static void ververs(ServerPlayer p) {
        Map<BlockPos, BlockState> wil = gewenst(p);
        Map<BlockPos, BlockState> had = GETOOND.getOrDefault(p.getUUID(), Map.of());
        ServerLevel level = p.level();
        for (BlockPos pos : had.keySet()) {
            if (!wil.containsKey(pos) && level.isLoaded(pos)) {
                stuur(p, pos, level.getBlockState(pos));
            }
        }
        wil.forEach((pos, state) -> stuur(p, pos, state));
        if (wil.isEmpty()) {
            GETOOND.remove(p.getUUID());
        } else {
            GETOOND.put(p.getUUID(), wil);
        }
    }

    /** Shows this state at this spot to this player at once (the next refresh keeps it only when a source still wants it). */
    public static void toon(ServerPlayer p, BlockPos pos, BlockState state) {
        GETOOND.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).put(pos.immutable(), state);
        stuur(p, pos, state);
        straks(p);
    }

    /**
     * Sends this player's list again in their next tick: after a click, when the game's own answer (the real blocks at
     * and next to the click) has gone out.
     */
    public static void straks(ServerPlayer p) {
        STRAKS.add(p.getUUID());
    }

    /** What this player sees at this spot: what was last sent to them, else the real block. */
    public static BlockState ziet(ServerPlayer p, BlockPos pos) {
        BlockState state = GETOOND.getOrDefault(p.getUUID(), Map.of()).get(pos);
        return state != null ? state : p.level().getBlockState(pos);
    }

    private static void stuur(ServerPlayer p, BlockPos pos, BlockState state) {
        if (!(p instanceof FakePlayer) && p.connection != null) {
            p.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        }
    }

    /** A sound that only this player hears (their own fire, their own weed). */
    public static void geluid(ServerPlayer p, SoundEvent sound, BlockPos pos, float volume, float pitch) {
        if (!(p instanceof FakePlayer) && p.connection != null) {
            p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.BLOCKS, pos.getX() + 0.5,
                    pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch, p.getRandom().nextLong()));
        }
    }

    /** Particles that only this player sees. */
    public static void deeltjes(ServerPlayer p, ParticleOptions deeltje, Vec3 plek, int aantal, double spreiding, double snelheid) {
        if (!(p instanceof FakePlayer) && p.connection != null) {
            p.level().sendParticles(p, deeltje, false, false, plek.x, plek.y, plek.z, aantal, spreiding, spreiding, spreiding, snelheid);
        }
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    static void opTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (STRAKS.remove(p.getUUID()) | (p.tickCount + p.getId()) % TICKS == 0)) {
            ververs(p);
        }
    }

    /**
     * A chunk was sent to a player (again: they came back, or their client dropped and reloaded it): it holds the REAL
     * blocks, so what this player was shown in it goes out again right behind it, instead of up to a second later.
     */
    static void opChunk(ChunkWatchEvent.Sent event) {
        Map<BlockPos, BlockState> had = GETOOND.get(event.getPlayer().getUUID());
        if (had == null || had.isEmpty()) {
            return;
        }
        ChunkPos chunk = event.getPos();
        had.forEach((pos, state) -> {
            if (pos.getX() >> 4 == chunk.x() && pos.getZ() >> 4 == chunk.z()) {
                stuur(event.getPlayer(), pos, state);
            }
        });
    }

    /**
     * The real block at a spot that somebody is shown otherwise changed (the game tells every client the real state): that
     * player's list is sent again in their next tick. Fired for every block update of the server, so nothing happens here
     * unless a player is shown something at all.
     */
    static void opBlokUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (GETOOND.isEmpty() || event.getLevel().isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        GETOOND.forEach((speler, had) -> {
            if (had.containsKey(pos)) {
                STRAKS.add(speler);
            }
        });
    }

    static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        GETOOND.remove(event.getEntity().getUUID());
        STRAKS.remove(event.getEntity().getUUID());
        Kooien.vergeet(event.getEntity());
    }

    /** Another dimension: the client forgot everything of the old one. */
    static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        GETOOND.remove(event.getEntity().getUUID());
    }

    static void opStop(ServerStoppedEvent event) {
        GETOOND.clear();
        STRAKS.clear();
        Vuren.wis();
    }
}
