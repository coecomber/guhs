package nl.juiced.guhs.feature.knus;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The four seasons of the Guhmensie (2.8, Knuffeldal). A season lasts {@link #DAGEN} Minecraft days; the days are the
 * overworld's days (the Guhmensie shares its clock) plus an offset that an op can shift with
 * {@code /guhs seizoen <lente|zomer|herfst|winter>} (that season starts now; the season number {@link #nummer} never
 * goes back, so "once per season" things can't be done twice).
 * <ul>
 *   <li>Server and client: {@link #huidig}, {@link #dag}, {@link #dagInSeizoen}, {@link #nummer}. The client gets the
 *       offset with the payload {@code guhs:seizoen_sync} (on login and whenever it changes).</li>
 *   <li>{@link #bijWissel}: listeners hear about every change (checked every 20 server ticks); when the server starts
 *       each listener gets {@code (null, current)} once. Register them in your Feature.register.</li>
 * </ul>
 * The offset is kept in the SavedData {@code guhs_knus} of the overworld.
 */
public enum Seizoen implements net.minecraft.util.StringRepresentable {
    LENTE, ZOMER, HERFST, WINTER;

    /** A season lasts 7 in-game days. */
    public static final int DAGEN = 7;

    @FunctionalInterface
    public interface Wissel {
        /** The season changed on this server (oud = null: the server just started). */
        void gewisseld(MinecraftServer server, @Nullable Seizoen oud, Seizoen nieuw);
    }

    private static final List<Wissel> LISTENERS = new CopyOnWriteArrayList<>();
    /** Client: the offset (days) the server told us. */
    static volatile long clientOffset;
    /** Server: the season last announced (null = not yet, after a start). */
    @Nullable
    private static Seizoen laatst;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** (block states: e.g. seizoen=lente) */
    @Override
    public String getSerializedName() {
        return id();
    }

    /** The name, lang gui.guhs.seizoen.&lt;id&gt; ("Lente", ...). */
    public Component naam() {
        return Component.translatable("gui.guhs.seizoen." + id());
    }

    /** The next season. */
    public Seizoen volgende() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Nullable
    public static Seizoen byId(String id) {
        for (Seizoen s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Register a listener (in your Feature.register). */
    public static void bijWissel(Wissel listener) {
        LISTENERS.add(listener);
    }

    /** The day number: overworld dayTime / 24000 plus the offset (server and client). */
    public static long dag(Level level) {
        if (level.isClientSide) {
            return Math.floorDiv(level.getDayTime(), 24000L) + clientOffset;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return Math.floorDiv(level.getDayTime(), 24000L);
        }
        return Math.floorDiv(server.overworld().getDayTime(), 24000L) + Data.get(server).offset;
    }

    /** Seasons since day 0 (dag / 7): use it as the key of "once per season" things. */
    public static long nummer(Level level) {
        return Math.floorDiv(dag(level), DAGEN);
    }

    /** 0..6: the day within the season. */
    public static int dagInSeizoen(Level level) {
        return (int) Math.floorMod(dag(level), DAGEN);
    }

    /** The season now (server and client). */
    public static Seizoen huidig(Level level) {
        return van(nummer(level));
    }

    /** The season of season number n. */
    public static Seizoen van(long nummer) {
        return values()[(int) Math.floorMod(nummer, values().length)];
    }

    // --- server --------------------------------------------------------------------------------------------------------

    /**
     * Makes this season start today (op command, tests). The offset only moves forward: the season number goes up by
     * 1 to 4, never back. Tells every listener and every client right away.
     */
    public static void zet(MinecraftServer server, Seizoen seizoen) {
        Data data = Data.get(server);
        long raw = Math.floorDiv(server.overworld().getDayTime(), 24000L);
        long cycle = (long) DAGEN * values().length;
        long now = raw + data.offset;
        long want = seizoen.ordinal() * (long) DAGEN;
        // the first day of the next season with this name, counted from the start of the current season
        long startOfCurrent = Math.floorDiv(now, DAGEN) * DAGEN;
        long target = Math.floorDiv(startOfCurrent, cycle) * cycle + want;
        while (target <= startOfCurrent) {
            target += cycle;
        }
        data.offset += target - now;
        data.setDirty();
        check(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    /** (Tests) the raw offset. */
    public static long offset(MinecraftServer server) {
        return Data.get(server).offset;
    }

    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 == 0) {
            check(server);
        }
    }

    /** Looks whether the season changed; tells the listeners and the players. */
    static void check(MinecraftServer server) {
        Seizoen now = huidig(server.overworld());
        if (now == laatst) {
            return;
        }
        Seizoen old = laatst;
        laatst = now;
        for (Wissel listener : LISTENERS) {
            try {
                listener.gewisseld(server, old, now);
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger("guhs").error("Seizoen listener failed", e);
            }
        }
        if (old != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sync(player);
            }
        }
    }

    static void onServerStarted(ServerStartedEvent event) {
        laatst = null;
        check(event.getServer());
    }

    static void onServerStopped(ServerStoppedEvent event) {
        laatst = null;
    }

    /** Sends the offset to a client. */
    public static void sync(ServerPlayer player) {
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new KnusPayloads.SeizoenSync(Data.get(player.server).offset));
    }

    /** The saved offset (overworld data storage, "guhs_knus"). */
    static final class Data extends SavedData {
        long offset;

        static Data get(MinecraftServer server) {
            ServerLevel overworld = server.overworld();
            return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "guhs_knus");
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.offset = tag.getLong("SeizoenOffset");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLong("SeizoenOffset", offset);
            return tag;
        }
    }
}
