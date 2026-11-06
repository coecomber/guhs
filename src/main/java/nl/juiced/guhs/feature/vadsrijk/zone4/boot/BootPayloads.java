package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

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
 * T-Z4BOOT network: the boat screen's data to the client ({@link Scherm}) and one action back ({@link Doe}: "open" (the tab),
 * "kies" id, "koop" id). The server checks ownership and money.
 */
public final class BootPayloads {
    public record Scherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_z4boot_scherm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Scherm::data,
                Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Doe(String actie, String id) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("vr_z4boot_doe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Doe::actie,
                ByteBufCodecs.STRING_UTF8, Doe::id, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Scherm> schermOntvanger = s -> {
    };

    private BootPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> schermOntvanger.accept(p));
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                Boot.doe(sp, p.actie(), p.id());
            }
        });
    }

    static void stuur(ServerPlayer p, Scherm s) {
        PacketDistributor.sendToPlayer(p, s);
    }
}
