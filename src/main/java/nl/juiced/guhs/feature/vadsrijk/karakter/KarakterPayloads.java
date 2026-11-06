package nl.juiced.guhs.feature.vadsrijk.karakter;

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
 * T-KARAKTER network: the character screen ({@link Scherm}, server -> client), an action from it ({@link Doe}, client -> server:
 * "open", "wissel" {slot}, "maak" {slot, naam, modus}, "naam" {slot, naam}, "modus" {slot, modus}) and the badges of all players
 * ({@link Badges}, server -> client).
 */
public final class KarakterPayloads {
    public record Scherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_karakter_scherm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Scherm::data,
                Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Doe(String actie, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("vr_karakter_doe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Doe::actie,
                ByteBufCodecs.COMPOUND_TAG, Doe::args, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Badges(CompoundTag codes) implements CustomPacketPayload {
        public static final Type<Badges> TYPE = new Type<>(Guhs.id("vr_karakter_badges"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Badges> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Badges::codes,
                Badges::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Scherm> schermOntvanger = s -> {
    };
    /** Set by the client: refresh the names after new badges. */
    public static Runnable badgesOntvangen = () -> {
    };

    private KarakterPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> schermOntvanger.accept(p));
        r.playToClient(Badges.TYPE, Badges.STREAM_CODEC, (p, c) -> {
            nl.juiced.guhs.feature.vadsrijk.karakter.Badges.ontvang(p.codes());
            badgesOntvangen.run();
        });
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                KarakterScherm.doe(sp, p.actie(), p.args());
            }
        });
    }

    static void stuur(ServerPlayer p, CompoundTag data) {
        PacketDistributor.sendToPlayer(p, new Scherm(data));
    }
}
