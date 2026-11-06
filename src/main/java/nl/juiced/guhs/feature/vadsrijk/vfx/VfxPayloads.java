package nl.juiced.guhs.feature.vadsrijk.vfx;

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
 * T-VFX network: one S2C payload {@code guhs:vr_vfx} = a kind + a small tag (like the beleving payload). Kinds (see
 * {@code VfxClient}): {@code decal} (a ground telegraph), {@code weg} (remove a decal group), {@code deeltjes} (a particle burst),
 * {@code golf} (shockwave ring), {@code straal} (beam), {@code flits} (impact flash), {@code spoor} (ribbon trail behind an
 * entity), {@code zuil} (light pillar), {@code pose} (wind-up/hit/phase pose for renderers without GeckoLib), {@code cast} (the
 * cast bar), {@code niveau} (set the player's own VFX level from the dev command).
 */
public final class VfxPayloads {
    /** Client hook (set by VfxClient). */
    public static Consumer<Fx> ontvanger = m -> {
    };

    public record Fx(String soort, CompoundTag d) implements CustomPacketPayload {
        public static final Type<Fx> TYPE = new Type<>(Guhs.id("vr_vfx"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Fx> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Fx::soort,
                ByteBufCodecs.COMPOUND_TAG, Fx::d, Fx::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private VfxPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Fx.TYPE, Fx.STREAM_CODEC, (m, c) -> ontvanger.accept(m));
    }

    /** Counts what went out (gametests: did a telegraph reach the players?). */
    public static int verstuurd;

    static void stuur(ServerPlayer p, String soort, CompoundTag d) {
        verstuurd++;
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, new Fx(soort, d));
        }
    }
}
