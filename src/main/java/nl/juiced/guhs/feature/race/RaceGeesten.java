package nl.juiced.guhs.feature.race;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import net.minecraft.core.UUIDUtil;
/**
 * The golden ghosts (2.9): per track and level (by its whole-race board) the fastest race of the world, with its
 * recording. When someone else races there, that race drives along in gold (unless they switched it off:
 * {@link RaceRecords#goudOn}). Saved with the world (overworld data "guhs_race_geesten").
 */
public final class RaceGeesten extends SavedData {
    /** The world record of one board: who, how fast, and the recording (as RaceRecords.ghost). */
    public record Geest(UUID player, String name, int ticks, int[] samples) {
    }

    private final Map<String, Geest> geesten = new HashMap<>();

    public static RaceGeesten get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(RaceGeesten::new, RaceGeesten::load, null), "guhs_race_geesten");
    }

    /** The golden ghost of a board, or null. */
    @Nullable
    public static Geest geest(MinecraftServer server, String board) {
        return get(server).geesten.get(board);
    }

    /** A finished race: kept as the golden ghost when it's the fastest ever on that board. Returns true if so. */
    public static boolean offer(ServerPlayer player, String board, int ticks, int[] samples) {
        RaceGeesten data = get(player.level().getServer());
        Geest old = data.geesten.get(board);
        if (samples.length < 6 || old != null && old.ticks() <= ticks) {
            return false;
        }
        data.geesten.put(board, new Geest(player.getUUID(), player.getGameProfile().name(), ticks, samples));
        data.setDirty();
        return true;
    }

    /** (Tests) forget a board's golden ghost. */
    public static void forget(MinecraftServer server, String board) {
        RaceGeesten data = get(server);
        if (data.geesten.remove(board) != null) {
            data.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        geesten.forEach((board, g) -> {
            CompoundTag t = new CompoundTag();
            t.store("Player", UUIDUtil.CODEC, g.player());
            t.putString("Name", g.name());
            t.putInt("Ticks", g.ticks());
            t.putIntArray("Samples", g.samples());
            all.put(board, t);
        });
        tag.put("Geesten", all);
        return tag;
    }

    static RaceGeesten load(CompoundTag tag, HolderLookup.Provider registries) {
        RaceGeesten data = new RaceGeesten();
        CompoundTag all = tag.getCompoundOrEmpty("Geesten");
        for (String board : all.keySet()) {
            CompoundTag t = all.getCompoundOrEmpty(board);
            data.geesten.put(board, new Geest(t.read("Player", UUIDUtil.CODEC).orElseThrow(), t.getStringOr("Name", ""), t.getIntOr("Ticks", 0), t.getIntArray("Samples").orElse(new int[0])));
        }
        return data;
    }
}
