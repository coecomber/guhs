package nl.juiced.guhs.feature.band;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The band's messages. Server to client: the "Mijn guhs" data for the Guhdex ({@code guhs:band_mijn_guhs}) and floating
 * hearts over a guh ({@code guhs:band_hartjes}). Client to server: "send me the Mijn guhs data" ({@code guhs:band_vraag}).
 */
public final class BandPayloads {
    /** Client handlers (set by client.BandClient; the dedicated server never gets these payloads). */
    public static volatile Consumer<MijnGuhsData> mijnGuhsOntvanger = p -> {
    };
    public static volatile Consumer<Hartjes> hartjesOntvanger = p -> {
    };

    /** The owner's guhs (see {@link MijnGuhs#snapshot}); "Focus" = the band id of the guh whose page should open. */
    public record MijnGuhsData(CompoundTag data) implements CustomPacketPayload {
        public static final Type<MijnGuhsData> TYPE = new Type<>(Guhs.id("band_mijn_guhs"));
        public static final StreamCodec<FriendlyByteBuf, MijnGuhsData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, MijnGuhsData::data, MijnGuhsData::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MijnGuhsData p, IPayloadContext context) {
            mijnGuhsOntvanger.accept(p);
        }
    }

    /** Floating hearts over a guh: n &gt; 0 hearts just added; n &lt; 0: a level-up to level (-n - 1). */
    public record Hartjes(int guh, int n) implements CustomPacketPayload {
        public static final Type<Hartjes> TYPE = new Type<>(Guhs.id("band_hartjes"));
        public static final StreamCodec<FriendlyByteBuf, Hartjes> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Hartjes::guh, ByteBufCodecs.INT, Hartjes::n, Hartjes::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Hartjes p, IPayloadContext context) {
            hartjesOntvanger.accept(p);
        }
    }

    /** Client to server: send me my guhs (the Guhdex tab asks when it opens). */
    public record Vraag(String focus) implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("band_vraag"));
        public static final StreamCodec<FriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Vraag::focus, Vraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Vraag p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                MijnGuhs.stuur(sp, null);
            }
        }
    }

    /** Sends the owner's Mijn guhs data (e.g. before the Guhdex opens). */
    public static void sync(ServerPlayer player) {
        MijnGuhs.stuur(player, null);
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(MijnGuhsData.TYPE, MijnGuhsData.STREAM_CODEC, MijnGuhsData::handle);
        registrar.playToClient(Hartjes.TYPE, Hartjes.STREAM_CODEC, Hartjes::handle);
        registrar.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, Vraag::handle);
    }

    private BandPayloads() {
    }
}
