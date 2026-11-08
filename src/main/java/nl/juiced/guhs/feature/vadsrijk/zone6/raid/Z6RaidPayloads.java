package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

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

/** U-Z6RAID network: one S2C payload {@code guhs:vr_z6raid} = the raid HUD state (a tag, every 10 ticks to each group member). */
public final class Z6RaidPayloads {
    /** Client hook (set by Z6RaidHud). */
    public static Consumer<CompoundTag> ontvanger = t -> {
    };
    /** Gametests: how many went out. */
    public static int verstuurd;

    public record Staat(CompoundTag t) implements CustomPacketPayload {
        public static final Type<Staat> TYPE = new Type<>(Guhs.id("vr_z6raid"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Staat> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Staat::t, Staat::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Z6RaidPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Staat.TYPE, Staat.STREAM_CODEC, (m, c) -> ontvanger.accept(m.t()));
    }

    static void stuur(ServerPlayer p, CompoundTag t) {
        verstuurd++;
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, new Staat(t));
        }
    }
}
