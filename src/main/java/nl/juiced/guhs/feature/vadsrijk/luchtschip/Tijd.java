package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;

/**
 * T-LUCHTSCHIP: the timetable clock. Normally the level's game time; the dev command {@code vertrek} shifts one line's clock so
 * its next departure is in a few seconds ({@link #verschuiving}); the shift is sent to the clients (they fly the ships
 * themselves) and is not saved. Also the payloads of the slice (the shifts, the silhouette mode for the client).
 */
public final class Tijd {
    private static final Map<String, Long> SCHUIF = new ConcurrentHashMap<>();

    private Tijd() {
    }

    public static long verschuiving(@Nullable Lijnen.Lijn l) {
        return l == null ? 0 : SCHUIF.getOrDefault(l.id(), 0L);
    }

    /** The timetable time of a line on the server. */
    public static double nu(ServerLevel sl, Lijnen.Lijn l) {
        return sl.getGameTime() + verschuiving(l);
    }

    /** (dev) the next departure of this line in {@code over} ticks. */
    static void vertrekOver(ServerLevel sl, Lijnen.Lijn l, int over) {
        int nu = Dienstregeling.totVertrek(l, (long) nu(sl, l));
        SCHUIF.merge(l.id(), (long) (nu - over), Long::sum);
        stuurAlle(sl);
    }

    static void wis(ServerLevel sl) {
        SCHUIF.clear();
        stuurAlle(sl);
    }

    static void stuurAlle(ServerLevel sl) {
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            stuur(p);
        }
    }

    static void stuur(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        SCHUIF.forEach(t::putLong);
        PacketDistributor.sendToPlayer(p, new Schuif(t));
    }

    /** (client) take the server's shifts. */
    public static void ontvang(CompoundTag t) {
        SCHUIF.clear();
        for (String k : t.keySet()) {
            SCHUIF.put(k, t.getLongOr(k, 0L));
        }
    }

    // ------------------------------------------------------------------------------------------------ payloads

    /** Server -&gt; client: the timetable shifts per line. */
    public record Schuif(CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Schuif> TYPE = new Type<>(Guhs.id("vr_luchtschip_schuif"));
        public static final StreamCodec<ByteBuf, Schuif> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(Schuif::new, Schuif::tag);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -&gt; client: the silhouettes (0 off, 1 normal, 2 always + full strength, to look at them; dev). */
    public record Schimmen(int stand) implements CustomPacketPayload {
        public static final Type<Schimmen> TYPE = new Type<>(Guhs.id("vr_luchtschip_schimmen"));
        public static final StreamCodec<ByteBuf, Schimmen> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Schimmen::new, Schimmen::stand);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static IntConsumer schimmenOntvanger = s -> {
    };
    /** Set by the client (runs on the client thread). */
    public static Consumer<CompoundTag> schuifOntvanger = Tijd::ontvang;
}
