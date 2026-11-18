package nl.juiced.guhs.feature.guhwinter.lootjes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** {@code guhs:guhwinter_surprise_plaats} (to the server): put the Surprise in this hand down here (min corner, turn 0..3). */
public final class LootjesPayloads {
    public record Plaats(boolean hoofdhand, BlockPos oorsprong, int draai) implements CustomPacketPayload {
        public static final Type<Plaats> TYPE = new Type<>(Guhs.id("guhwinter_surprise_plaats"));
        public static final StreamCodec<FriendlyByteBuf, Plaats> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Plaats::hoofdhand,
                BlockPos.STREAM_CODEC, Plaats::oorsprong, ByteBufCodecs.VAR_INT, Plaats::draai, Plaats::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Plaats.TYPE, Plaats.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                SurprisePlaatsen.plaats(sp, p.hoofdhand(), p.oorsprong(), p.draai());
            }
        });
    }

    private LootjesPayloads() {
    }
}
