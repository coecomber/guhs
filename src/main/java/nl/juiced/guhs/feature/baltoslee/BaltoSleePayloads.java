package nl.juiced.guhs.feature.baltoslee;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The sled's messages: the rider's game telling the server where its sled is (baltoslee_stuur, every tick while riding).
 * Everything else the rider sees comes with the sled's synced data and entity events.
 */
public final class BaltoSleePayloads {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Stuur.TYPE, Stuur.STREAM_CODEC, Stuur::handle);
    }

    /** (Client) where the rider's own sled is now. */
    static void stuur(int slee, int gen, int been, float s, float lat, float v, float latV) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new Stuur(slee, gen, been, s, lat, v, latV));
    }

    /** Client -> server: sled slee (in generation gen of its resets) is on leg been at s, lat, going v (and sliding latV). */
    public record Stuur(int slee, int gen, int been, float s, float lat, float v, float latV) implements CustomPacketPayload {
        public static final Type<Stuur> TYPE = new Type<>(Guhs.id("baltoslee_stuur"));
        public static final StreamCodec<ByteBuf, Stuur> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Stuur decode(ByteBuf buf) {
                return new Stuur(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
            }

            @Override
            public void encode(ByteBuf buf, Stuur p) {
                ByteBufCodecs.VAR_INT.encode(buf, p.slee());
                ByteBufCodecs.VAR_INT.encode(buf, p.gen());
                ByteBufCodecs.VAR_INT.encode(buf, p.been());
                buf.writeFloat(p.s());
                buf.writeFloat(p.lat());
                buf.writeFloat(p.v());
                buf.writeFloat(p.latV());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stuur p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof SleeEntity slee && slee.getId() == p.slee()
                    && Float.isFinite(p.s()) && Float.isFinite(p.lat()) && Float.isFinite(p.v()) && Float.isFinite(p.latV())) {
                SleeRit rit = SleeRit.van(player);
                if (rit != null && slee.getUUID().equals(rit.slee())) {
                    rit.meld(player, p.gen(), p.been(), p.s(), p.lat(), p.v(), p.latV());
                }
            }
        }
    }

    private BaltoSleePayloads() {
    }
}
