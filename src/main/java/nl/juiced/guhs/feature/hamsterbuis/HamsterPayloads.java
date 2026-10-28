package nl.juiced.guhs.feature.hamsterbuis;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.network.ModNetworking;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The hamsterbuis station's screen: server -> client {@link Open} (the station's data and where you may go), client ->
 * server {@link Actie} (travel, rename, open/close to everyone). The client's side is {@link #KLANT} (set by
 * {@code client.HamsterbuisClient}: common code never touches client classes).
 */
public final class HamsterPayloads {
    public static volatile Consumer<Open> KLANT;

    private HamsterPayloads() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
    }

    static void stuur(ServerPlayer p, CustomPacketPayload payload) {
        ModNetworking.sendTo(p, payload);
    }

    public record Open(BlockPos station, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("hamsterbuis_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Open::station,
                ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Open payload, IPayloadContext context) {
            Consumer<Open> k = KLANT;
            if (k != null) {
                context.enqueueWork(() -> k.accept(payload));
            }
        }
    }

    public record Actie(BlockPos station, int actie, String tekst) implements CustomPacketPayload {
        public static final int REIS = 0, HERNOEM = 1, OPENBAAR = 2;
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("hamsterbuis_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Actie::station,
                ByteBufCodecs.VAR_INT, Actie::actie, ByteBufCodecs.stringUtf8(64), Actie::tekst, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Actie payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer p && p.position().distanceToSqr(Vec3.atCenterOf(payload.station())) <= 64
                        && p.level().getBlockEntity(payload.station()) instanceof Station.BlockEntityImpl s) {
                    s.actie(p, payload.actie(), payload.tekst());
                }
            });
        }
    }
}
