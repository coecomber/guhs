package nl.juiced.guhs.feature.weerder;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
 * The Wilde-guhweerder screen's messages: server to client "open (or refresh) the screen of this weerder"
 * ({@code guhs:weerder_open}: radius, owner, may-edit), client to server "set the radius" ({@code guhs:weerder_straal}).
 * Only the owner (or an op) within 16 blocks changes it.
 */
public final class WeerderPayloads {
    /** Client: opens/refreshes the screen (set by client.WeerderClient). */
    public static volatile Consumer<Open> opener = p -> {
    };

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("weerder_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Straal(BlockPos pos, int straal) implements CustomPacketPayload {
        public static final Type<Straal> TYPE = new Type<>(Guhs.id("weerder_straal"));
        public static final StreamCodec<FriendlyByteBuf, Straal> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Straal::pos,
                ByteBufCodecs.VAR_INT, Straal::straal, Straal::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Straal p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                zet(sp, p.pos(), p.straal());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Straal.TYPE, Straal.STREAM_CODEC, Straal::handle);
    }

    /** Opens (or refreshes) the screen of this weerder for this player (read-only for anyone but its owner or an op). */
    public static void open(ServerPlayer player, WeerderBlockEntity be) {
        ModNetworking.sendTo(player, new Open(data(player, be)));
    }

    public static CompoundTag data(ServerPlayer player, WeerderBlockEntity be) {
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", be.getBlockPos().asLong());
        t.putInt("Straal", be.straal());
        t.putString("EigenaarNaam", be.eigenaarNaam());
        t.putBoolean("MagBewerken", be.magBewerken(player));
        return t;
    }

    /** A radius from the screen: true when it was set (the owner or an op, close enough, a real step). */
    public static boolean zet(ServerPlayer player, BlockPos pos, int straal) {
        if (player.distanceToSqr(pos.getCenter()) > 16 * 16 || !(player.level().getBlockEntity(pos) instanceof WeerderBlockEntity be)) {
            return false;
        }
        if (!be.magBewerken(player)) {
            player.sendOverlayMessage(be.vanWie().copy().withStyle(ChatFormatting.GRAY));
            return false;
        }
        boolean gezet = be.zetStraal(straal);
        open(player, be);
        return gezet;
    }

    private WeerderPayloads() {
    }
}
