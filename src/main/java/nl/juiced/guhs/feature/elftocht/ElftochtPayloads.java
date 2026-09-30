package nl.juiced.guhs.feature.elftocht;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * guhs:elftocht_open (server -> client: Schaatsmeester Guhglij's screen), guhs:elftocht_action (its buttons) and
 * guhs:elftocht_plof (server -> client: a Stempelguh stamps: its animation).
 */
public final class ElftochtPayloads {
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("elftocht_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.elftocht.client.ElftochtClient.open(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("elftocht_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npcId()) instanceof GuhNpcEntity npc) {
                SchaatsmeesterRole.action(npc, player, payload.action());
            }
        }
    }

    public record Plof(int entityId) implements CustomPacketPayload {
        public static final Type<Plof> TYPE = new Type<>(Guhs.id("elftocht_plof"));
        public static final StreamCodec<FriendlyByteBuf, Plof> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Plof::entityId, Plof::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Plof payload, IPayloadContext context) {
            nl.juiced.guhs.feature.elftocht.client.ElftochtClient.plof(payload.entityId());
        }
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        r.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        r.playToClient(Plof.TYPE, Plof.STREAM_CODEC, Plof::handle);
    }

    private ElftochtPayloads() {
    }
}
