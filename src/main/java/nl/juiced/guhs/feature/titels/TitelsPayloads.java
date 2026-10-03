package nl.juiced.guhs.feature.titels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The titles over the network:
 * <ul>
 *   <li>{@code guhs:titels} (to the client): this player's earned titles and the one that shows (the Guhdex tab);</li>
 *   <li>{@code guhs:titels_vraag} (to the server): the tab asks for fresh data;</li>
 *   <li>{@code guhs:titels_kies} (to the server): the player picks a title ("geen": none);</li>
 *   <li>{@code guhs:titels_actief} (to every client): which online player shows which title (the names above the heads).</li>
 * </ul>
 */
public final class TitelsPayloads {
    /** Client handlers (set by client.TitelsCache). */
    public static volatile Consumer<Stand> ontvanger = p -> {
    };
    public static volatile Consumer<Actief> actiefOntvanger = p -> {
    };

    /** The earned titles (ids) and the one that shows ("" = none). */
    public record Stand(List<String> behaald, String actief) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("titels"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(64)), Stand::behaald,
                ByteBufCodecs.stringUtf8(64), Stand::actief, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }

    public record Vraag() implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("titels_vraag"));
        public static final StreamCodec<FriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.unit(new Vraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Vraag p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                sync(sp);
            }
        }
    }

    public record Kies(String id) implements CustomPacketPayload {
        public static final Type<Kies> TYPE = new Type<>(Guhs.id("titels_kies"));
        public static final StreamCodec<FriendlyByteBuf, Kies> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Kies::id, Kies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kies p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Titels.kies(sp, p.id());
                sync(sp);
            }
        }
    }

    public record Actief(Map<UUID, String> titels) implements CustomPacketPayload {
        public static final Type<Actief> TYPE = new Type<>(Guhs.id("titels_actief"));
        public static final StreamCodec<FriendlyByteBuf, Actief> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.stringUtf8(64), 1024), Actief::titels, Actief::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actief p, IPayloadContext context) {
            actiefOntvanger.accept(p);
        }
    }

    /** This player's titles, as the Guhdex tab shows them. */
    public static Stand stand(ServerPlayer player) {
        Titels.Titel actief = Titels.actief(player);
        return new Stand(Titels.ids(Titels.behaald(player)), actief == null ? "" : actief.id());
    }

    /** Sends this player's titles. */
    public static void sync(ServerPlayer player) {
        nl.juiced.guhs.network.ModNetworking.sendTo(player, stand(player));
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
        registrar.playToClient(Actief.TYPE, Actief.STREAM_CODEC, Actief::handle);
        registrar.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, Vraag::handle);
        registrar.playToServer(Kies.TYPE, Kies.STREAM_CODEC, Kies::handle);
    }

    private TitelsPayloads() {
    }
}
