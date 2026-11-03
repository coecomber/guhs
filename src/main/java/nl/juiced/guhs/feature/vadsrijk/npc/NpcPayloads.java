package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/** The npc slice's network: the quest markers above the NPCs, the questlog (J). */
public final class NpcPayloads {
    /** Client hooks (set by NpcClient). */
    public static Consumer<Markers> markerOntvanger = m -> {
    };
    public static Consumer<Log> logOntvanger = m -> {
    };

    private NpcPayloads() {
    }

    /** Which NPC entities show which marker for you: 1 = "!" (side), 2 = "!" (main), 3 = "?" (hand in), 4 = "?" (talk to). */
    public record Markers(int[] ids, byte[] soort) implements CustomPacketPayload {
        public static final Type<Markers> TYPE = new Type<>(Guhs.id("vr_npc_markers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Markers> STREAM_CODEC = StreamCodec.ofMember((m, b) -> {
            b.writeVarIntArray(m.ids);
            b.writeByteArray(m.soort);
        }, b -> new Markers(b.readVarIntArray(), b.readByteArray()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The questlog's contents (built by {@link NpcLog}); open = open the screen (else just refresh it when open). */
    public record Log(CompoundTag data, boolean open) implements CustomPacketPayload {
        public static final Type<Log> TYPE = new Type<>(Guhs.id("vr_npc_log"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Log> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Log::data,
                ByteBufCodecs.BOOL, Log::open, Log::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: J was pressed (in the dimension). */
    public record LogVraag() implements CustomPacketPayload {
        public static final Type<LogVraag> TYPE = new Type<>(Guhs.id("vr_npc_logvraag"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LogVraag> STREAM_CODEC = StreamCodec.unit(new LogVraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Markers.TYPE, Markers.STREAM_CODEC, (m, c) -> markerOntvanger.accept(m));
        r.playToClient(Log.TYPE, Log.STREAM_CODEC, (m, c) -> logOntvanger.accept(m));
        r.playToServer(LogVraag.TYPE, LogVraag.STREAM_CODEC, NpcPayloads::opVraag);
    }

    private static void opVraag(LogVraag v, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer p && Vadsrijk.in(p)) {
            stuur(p, new Log(NpcLog.bouw(p), true));
        }
    }

    static void stuur(ServerPlayer p, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }
}
