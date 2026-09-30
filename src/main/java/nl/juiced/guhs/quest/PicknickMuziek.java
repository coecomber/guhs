package nl.juiced.guhs.quest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModItems;

/**
 * The jukebox at the guh picnic plays "Ze hangen aan me veh" whenever someone is near (a record in a structure doesn't
 * start by itself), and starts it again when the song is over. Take the record out and it stops, of course.
 */
public final class PicknickMuziek {
    public static final ResourceKey<Structure> PICNIC = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_picnic"));
    /** How often each player looks around (ticks), and how far (chunks). */
    private static final int EVERY = 60, CHUNKS = 2;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % EVERY != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.level();
        Structure picnic = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(PICNIC);
        if (picnic == null) {
            return;
        }
        ChunkPos here = player.chunkPosition();
        for (int dx = -CHUNKS; dx <= CHUNKS; dx++) {
            for (int dz = -CHUNKS; dz <= CHUNKS; dz++) {
                if (!(level.getChunkSource().getChunkNow(here.x() + dx, here.z() + dz) instanceof LevelChunk chunk)) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof JukeboxBlockEntity jukebox
                            && play(jukebox, level.structureManager().getStructureAt(jukebox.getBlockPos(), picnic).isValid())) {
                        return;
                    }
                }
            }
        }
    }

    /** Starts the guh record in this jukebox if it's a picnic jukebox (atPicnic) that isn't playing. */
    public static boolean play(JukeboxBlockEntity jukebox, boolean atPicnic) {
        if (!atPicnic || !jukebox.getTheItem().is(ModItems.MUSIC_DISC_ZE_HANGEN.get()) || jukebox.getSongPlayer().isPlaying()) {
            return false;
        }
        jukebox.tryForcePlaySong();
        return true;
    }

    private PicknickMuziek() {
    }
}
