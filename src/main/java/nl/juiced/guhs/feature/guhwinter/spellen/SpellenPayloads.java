package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;

/**
 * The Guhwinter games over the network:
 * <ul>
 *   <li>Pepernotenrace: {@code guhwinter_race_open} (to the client: seed, top 10, your best), {@code _start}, {@code _klaar}
 *   (to the server: the input of every step, never a score), {@code _uitslag} (to the client: what the server counted and
 *   gave), {@code _stop};</li>
 *   <li>Oliebollen bakken: {@code guhwinter_olie_open} (to the client: game number, airfryer or pan), {@code _klaar} (to the
 *   server: the score);</li>
 *   <li>the shop: {@code guhwinter_winkel_open} (your pepernoten and the prices), {@code _koop}, {@code _stand}.</li>
 * </ul>
 */
public final class SpellenPayloads {
    public static volatile Consumer<RaceOpen> raceOpener = p -> { };
    public static volatile Consumer<RaceUitslag> raceUitslag = p -> { };
    public static volatile Consumer<OlieOpen> olieOpener = p -> { };
    public static volatile Consumer<WinkelOpen> winkelOpener = p -> { };
    public static volatile Consumer<WinkelStand> winkelStand = p -> { };
    private static final int MAX_INVOER = Sim.MAX_STAPPEN * 3 / 8 + 64;

    // --- the top 10 as a tag -----------------------------------------------------------------------------------------------

    /** A board's lines for the client: name and score. */
    public record Regel(String naam, int score) {
    }

    public static CompoundTag regels(List<SpellenData.Regel> top) {
        ListTag l = new ListTag();
        for (SpellenData.Regel r : top) {
            CompoundTag t = new CompoundTag();
            t.putString("N", r.naam());
            t.putInt("S", r.score());
            l.add(t);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("R", l);
        return tag;
    }

    public static List<Regel> leesRegels(CompoundTag tag) {
        List<Regel> uit = new ArrayList<>();
        for (Tag t : tag.getListOrEmpty("R")) {
            if (t instanceof CompoundTag c) {
                uit.add(new Regel(c.getStringOr("N", "?"), c.getIntOr("S", 0)));
            }
        }
        return uit;
    }

    // --- Pepernotenrace ----------------------------------------------------------------------------------------------------

    public record RaceOpen(BlockPos pos, long seed, CompoundTag top, int best, boolean eersteGehad) implements CustomPacketPayload {
        public static final Type<RaceOpen> TYPE = new Type<>(Guhs.id("guhwinter_race_open"));
        public static final StreamCodec<FriendlyByteBuf, RaceOpen> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, RaceOpen::pos,
                ByteBufCodecs.VAR_LONG, RaceOpen::seed, ByteBufCodecs.COMPOUND_TAG, RaceOpen::top, ByteBufCodecs.VAR_INT, RaceOpen::best,
                ByteBufCodecs.BOOL, RaceOpen::eersteGehad, RaceOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RaceStart(BlockPos pos, long seed) implements CustomPacketPayload {
        public static final Type<RaceStart> TYPE = new Type<>(Guhs.id("guhwinter_race_start"));
        public static final StreamCodec<FriendlyByteBuf, RaceStart> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, RaceStart::pos,
                ByteBufCodecs.VAR_LONG, RaceStart::seed, RaceStart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RaceKlaar(BlockPos pos, long seed, int stappen, byte[] invoer) implements CustomPacketPayload {
        public static final Type<RaceKlaar> TYPE = new Type<>(Guhs.id("guhwinter_race_klaar"));
        public static final StreamCodec<FriendlyByteBuf, RaceKlaar> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, RaceKlaar::pos,
                ByteBufCodecs.VAR_LONG, RaceKlaar::seed, ByteBufCodecs.VAR_INT, RaceKlaar::stappen, ByteBufCodecs.byteArray(MAX_INVOER),
                RaceKlaar::invoer, RaceKlaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * The server's count: geldig = false when the run couldn't have been played in the time it took (nothing counted);
     * gekregen = the pepernoten it gave (with the first-run bonus); plaats = place in the top 10 (0: not in it).
     */
    public record RaceUitslag(BlockPos pos, boolean geldig, int score, int gekregen, int plaats, boolean record, long seed, CompoundTag top, int best)
            implements CustomPacketPayload {
        public static final Type<RaceUitslag> TYPE = new Type<>(Guhs.id("guhwinter_race_uitslag"));
        public static final StreamCodec<FriendlyByteBuf, RaceUitslag> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeBlockPos(p.pos());
            buf.writeBoolean(p.geldig());
            buf.writeVarInt(p.score());
            buf.writeVarInt(p.gekregen());
            buf.writeVarInt(p.plaats());
            buf.writeBoolean(p.record());
            buf.writeLong(p.seed());
            buf.writeNbt(p.top());
            buf.writeVarInt(p.best());
        }, buf -> new RaceUitslag(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                buf.readLong(), orEmpty(buf.readNbt()), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RaceStop(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RaceStop> TYPE = new Type<>(Guhs.id("guhwinter_race_stop"));
        public static final StreamCodec<FriendlyByteBuf, RaceStop> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, RaceStop::pos, RaceStop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // --- Oliebollen bakken -------------------------------------------------------------------------------------------------

    public record OlieOpen(int spel, boolean airfryer, long seed) implements CustomPacketPayload {
        public static final Type<OlieOpen> TYPE = new Type<>(Guhs.id("guhwinter_olie_open"));
        public static final StreamCodec<FriendlyByteBuf, OlieOpen> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, OlieOpen::spel,
                ByteBufCodecs.BOOL, OlieOpen::airfryer, ByteBufCodecs.VAR_LONG, OlieOpen::seed, OlieOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** klaar = the 45 seconds ran out (false: the screen closed early, the score so far). */
    public record OlieKlaar(int spel, int score, boolean klaar) implements CustomPacketPayload {
        public static final Type<OlieKlaar> TYPE = new Type<>(Guhs.id("guhwinter_olie_klaar"));
        public static final StreamCodec<FriendlyByteBuf, OlieKlaar> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, OlieKlaar::spel,
                ByteBufCodecs.VAR_INT, OlieKlaar::score, ByteBufCodecs.BOOL, OlieKlaar::klaar, OlieKlaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // --- the shop ----------------------------------------------------------------------------------------------------------

    public record WinkelOpen(int pepernoten, List<Integer> prijzen) implements CustomPacketPayload {
        public static final Type<WinkelOpen> TYPE = new Type<>(Guhs.id("guhwinter_winkel_open"));
        public static final StreamCodec<FriendlyByteBuf, WinkelOpen> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, WinkelOpen::pepernoten,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(32)), WinkelOpen::prijzen, WinkelOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record WinkelKoop(int waar) implements CustomPacketPayload {
        public static final Type<WinkelKoop> TYPE = new Type<>(Guhs.id("guhwinter_winkel_koop"));
        public static final StreamCodec<FriendlyByteBuf, WinkelKoop> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, WinkelKoop::waar, WinkelKoop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** After a purchase: your pepernoten now, and whether it worked. */
    public record WinkelStand(int pepernoten, int waar, boolean gelukt) implements CustomPacketPayload {
        public static final Type<WinkelStand> TYPE = new Type<>(Guhs.id("guhwinter_winkel_stand"));
        public static final StreamCodec<FriendlyByteBuf, WinkelStand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, WinkelStand::pepernoten,
                ByteBufCodecs.VAR_INT, WinkelStand::waar, ByteBufCodecs.BOOL, WinkelStand::gelukt, WinkelStand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static CompoundTag orEmpty(CompoundTag t) {
        return t == null ? new CompoundTag() : t;
    }

    private static void server(IPayloadContext context, Consumer<ServerPlayer> doe) {
        if (context.player() instanceof ServerPlayer player) {
            doe.accept(player);
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(RaceOpen.TYPE, RaceOpen.STREAM_CODEC, (p, c) -> raceOpener.accept(p));
        registrar.playToServer(RaceStart.TYPE, RaceStart.STREAM_CODEC, (p, c) -> server(c, pl -> Pepernotenrace.start(pl, p.pos(), p.seed())));
        registrar.playToServer(RaceKlaar.TYPE, RaceKlaar.STREAM_CODEC, (p, c) -> server(c, pl -> Pepernotenrace.klaar(pl, p.pos(), p.seed(), p.stappen(), p.invoer())));
        registrar.playToClient(RaceUitslag.TYPE, RaceUitslag.STREAM_CODEC, (p, c) -> raceUitslag.accept(p));
        registrar.playToServer(RaceStop.TYPE, RaceStop.STREAM_CODEC, (p, c) -> server(c, pl -> Pepernotenrace.stop(pl)));
        registrar.playToClient(OlieOpen.TYPE, OlieOpen.STREAM_CODEC, (p, c) -> olieOpener.accept(p));
        registrar.playToServer(OlieKlaar.TYPE, OlieKlaar.STREAM_CODEC, (p, c) -> server(c, pl -> Oliebollen.klaar(pl, p.spel(), p.score(), p.klaar())));
        registrar.playToClient(WinkelOpen.TYPE, WinkelOpen.STREAM_CODEC, (p, c) -> winkelOpener.accept(p));
        registrar.playToServer(WinkelKoop.TYPE, WinkelKoop.STREAM_CODEC, (p, c) -> server(c, pl -> GuhwinterWinkel.koop(pl, p.waar())));
        registrar.playToClient(WinkelStand.TYPE, WinkelStand.STREAM_CODEC, (p, c) -> winkelStand.accept(p));
    }

    private SpellenPayloads() {
    }
}
