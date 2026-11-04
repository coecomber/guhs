package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The beleving network: one S2C payload {@code guhs:vr_kern_beleving} = a kind + a small tag. Kinds (see BelevingClient):
 * {@code klap} (hit-stop: id, ticks, kick), {@code schud} (sterkte, ticks), {@code reeks} (n, mijlpaal), {@code reeks_weg} (n),
 * {@code fase} (id, fase, key, terugval), {@code overwinning} (id), {@code flits} (kleur, ticks).
 */
public final class BelevingPayloads {
    /** Client hook (set by BelevingClient). */
    public static Consumer<Moment> ontvanger = m -> {
    };

    public record Moment(String soort, CompoundTag d) implements CustomPacketPayload {
        public static final Type<Moment> TYPE = new Type<>(Guhs.id("vr_kern_beleving"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Moment> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Moment::soort,
                ByteBufCodecs.COMPOUND_TAG, Moment::d, Moment::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private BelevingPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Moment.TYPE, Moment.STREAM_CODEC, (m, c) -> ontvanger.accept(m));
    }

    static void stuur(ServerPlayer p, String soort, CompoundTag d) {
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, new Moment(soort, d));
        }
    }
}
