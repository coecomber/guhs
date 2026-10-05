package nl.juiced.guhs.feature.guhpixel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * Guhpixel over the network:
 * <ul>
 *   <li>{@code guhs:guhpixel_hud} (to the client): unlocked, saldo, total, rank (the HUD in the lobby);</li>
 *   <li>{@code guhs:guhpixel_rangen} (to every client): which online player shows which rank (the names above the heads);</li>
 *   <li>{@code guhs:guhpixel_winkel} (to the client): open / refresh the shop screen; {@code guhs:guhpixel_koop} (to the server);</li>
 *   <li>{@code guhs:guhpixel_gids} (to the client): the Guhdex tab's page; {@code guhs:guhpixel_gids_vraag} (to the server);
 *   {@code guhs:guhpixel_rang_kies} (to the server): the rank prefix on or off.</li>
 * </ul>
 * Client handlers are set by client.GuhpixelClient; the server never calls them.
 */
public final class GuhpixelPayloads {
    public static volatile Consumer<Hud> hudOntvanger = p -> { };
    public static volatile Consumer<Rangen> rangenOntvanger = p -> { };
    public static volatile Consumer<WinkelOpen> winkelOpener = p -> { };
    public static volatile Consumer<Gids> gidsOntvanger = p -> { };

    public record Hud(boolean toegang, int saldo, int totaal, int rang) implements CustomPacketPayload {
        public static final Type<Hud> TYPE = new Type<>(Guhs.id("guhpixel_hud"));
        public static final StreamCodec<FriendlyByteBuf, Hud> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Hud::toegang,
                ByteBufCodecs.VAR_INT, Hud::saldo, ByteBufCodecs.VAR_INT, Hud::totaal, ByteBufCodecs.VAR_INT, Hud::rang, Hud::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Hud p, IPayloadContext context) {
            hudOntvanger.accept(p);
        }
    }

    public record Rangen(Map<UUID, Integer> rangen) implements CustomPacketPayload {
        public static final Type<Rangen> TYPE = new Type<>(Guhs.id("guhpixel_rangen"));
        public static final StreamCodec<FriendlyByteBuf, Rangen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_INT, 1024), Rangen::rangen, Rangen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Rangen p, IPayloadContext context) {
            rangenOntvanger.accept(p);
        }
    }

    public record WinkelOpen(CompoundTag data) implements CustomPacketPayload {
        public static final Type<WinkelOpen> TYPE = new Type<>(Guhs.id("guhpixel_winkel"));
        public static final StreamCodec<FriendlyByteBuf, WinkelOpen> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, WinkelOpen::data,
                WinkelOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(WinkelOpen p, IPayloadContext context) {
            winkelOpener.accept(p);
        }
    }

    public record Koop(String id) implements CustomPacketPayload {
        public static final Type<Koop> TYPE = new Type<>(Guhs.id("guhpixel_koop"));
        public static final StreamCodec<FriendlyByteBuf, Koop> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(96), Koop::id, Koop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Koop p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Winkel.opKoop(sp, p.id());
            }
        }
    }

    public record Gids(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Gids> TYPE = new Type<>(Guhs.id("guhpixel_gids"));
        public static final StreamCodec<FriendlyByteBuf, Gids> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Gids::data, Gids::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Gids p, IPayloadContext context) {
            gidsOntvanger.accept(p);
        }
    }

    public record GidsVraag() implements CustomPacketPayload {
        public static final Type<GidsVraag> TYPE = new Type<>(Guhs.id("guhpixel_gids_vraag"));
        public static final StreamCodec<FriendlyByteBuf, GidsVraag> STREAM_CODEC = StreamCodec.unit(new GidsVraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(GidsVraag p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                ModNetworking.sendTo(sp, new Gids(GidsBlad.stand(sp)));
            }
        }
    }

    public record RangKies(boolean aan) implements CustomPacketPayload {
        public static final Type<RangKies> TYPE = new Type<>(Guhs.id("guhpixel_rang_kies"));
        public static final StreamCodec<FriendlyByteBuf, RangKies> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, RangKies::aan, RangKies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RangKies p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                nl.juiced.guhs.feature.guhpixel.Rangen.zet(sp, p.aan());
                ModNetworking.sendTo(sp, new Gids(GidsBlad.stand(sp)));
            }
        }
    }

    /** Sends this player's HUD numbers. */
    public static void hud(ServerPlayer p) {
        ModNetworking.sendTo(p, new Hud(Toegang.heeft(p), Muntjes.saldo(p), Muntjes.totaal(p), Muntjes.rang(p).ordinal()));
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Hud.TYPE, Hud.STREAM_CODEC, Hud::handle);
        registrar.playToClient(Rangen.TYPE, Rangen.STREAM_CODEC, Rangen::handle);
        registrar.playToClient(WinkelOpen.TYPE, WinkelOpen.STREAM_CODEC, WinkelOpen::handle);
        registrar.playToClient(Gids.TYPE, Gids.STREAM_CODEC, Gids::handle);
        registrar.playToServer(Koop.TYPE, Koop.STREAM_CODEC, Koop::handle);
        registrar.playToServer(GidsVraag.TYPE, GidsVraag.STREAM_CODEC, GidsVraag::handle);
        registrar.playToServer(RangKies.TYPE, RangKies.STREAM_CODEC, RangKies::handle);
    }

    private GuhpixelPayloads() {
    }
}
