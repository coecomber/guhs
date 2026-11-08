package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** U-LEVEN-STAD network: the client's "Stadsleven: VOL / ZACHT / UIT" setting to the server (on join and when it changes). */
public final class StadslevenPayloads {
    /** Client -> server: 0 = VOL, 1 = ZACHT, 2 = UIT. */
    public record Stand(int niveau) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("vr_stadsleven_stand"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Stand::niveau,
                Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private StadslevenPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToServer(Stand.TYPE, Stand.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                StadsLeven.Niveau[] n = StadsLeven.Niveau.values();
                StadsLeven.NIVEAU.put(sp.getUUID(), n[Math.max(0, Math.min(n.length - 1, p.niveau()))]);
            }
        });
    }
}
