package nl.juiced.guhs.feature.knus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The network messages of the Knus framework: guhs:knus_data, guhs:knus_claim and guhs:seizoen_sync. */
public final class KnusPayloads {
    /** Server -> client: the player's Knus data (see {@link KnusVoortgang}) and toasts to show ("Toasts" in extra). */
    public record KnusData(CompoundTag data, CompoundTag extra) implements CustomPacketPayload {
        public static final Type<KnusData> TYPE = new Type<>(Guhs.id("knus_data"));
        public static final StreamCodec<FriendlyByteBuf, KnusData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, KnusData::data, ByteBufCodecs.COMPOUND_TAG, KnusData::extra, KnusData::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(KnusData payload, IPayloadContext context) {
            KnusVoortgang.Client.set(payload.data());
            nl.juiced.guhs.feature.knus.client.KnusClient.knusData(payload);
        }
    }

    /** Client -> server: claim the reward of a milestone. */
    public record KnusClaim(String mijlpaal) implements CustomPacketPayload {
        public static final Type<KnusClaim> TYPE = new Type<>(Guhs.id("knus_claim"));
        public static final StreamCodec<FriendlyByteBuf, KnusClaim> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(KnusClaim::new, KnusClaim::mijlpaal).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(KnusClaim p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                KnusVoortgang.claim(player, p.mijlpaal());
            }
        }
    }

    /** Server -> client: the season offset (days). */
    public record SeizoenSync(long offset) implements CustomPacketPayload {
        public static final Type<SeizoenSync> TYPE = new Type<>(Guhs.id("seizoen_sync"));
        public static final StreamCodec<FriendlyByteBuf, SeizoenSync> STREAM_CODEC =
                ByteBufCodecs.VAR_LONG.map(SeizoenSync::new, SeizoenSync::offset).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SeizoenSync p, IPayloadContext context) {
            Seizoen.clientOffset = p.offset();
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(KnusData.TYPE, KnusData.STREAM_CODEC, KnusData::handle);
        registrar.playToServer(KnusClaim.TYPE, KnusClaim.STREAM_CODEC, KnusClaim::handle);
        registrar.playToClient(SeizoenSync.TYPE, SeizoenSync.STREAM_CODEC, SeizoenSync::handle);
    }

    private KnusPayloads() {
    }
}
