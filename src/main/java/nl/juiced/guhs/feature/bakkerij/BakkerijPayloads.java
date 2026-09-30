package nl.juiced.guhs.feature.bakkerij;

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
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The bakery's screens: guhs:bakkerij_open (server -> client: Korstje's screen or the baking screen of an oven),
 * guhs:bakkerij_action (Korstje's buttons), guhs:bakkerij_bak (slide it in / take it out) and guhs:bakkerij_status
 * (server -> client: the baking screen's update).
 */
public final class BakkerijPayloads {
    /** What {@link Open} opens: Korstje's screen (ref = his entity id) or the baking screen (ref = the oven's BlockPos). */
    public static final int KORSTJE = 0, BAKSCHERM = 1;
    /** {@link Bak} steps. */
    public static final int IN_DE_OVEN = 0, ERUIT = 1;

    public record Open(int soort, long ref, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("bakkerij_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::soort, ByteBufCodecs.VAR_LONG, Open::ref, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.bakkerij.client.BakkerijClient.open(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("bakkerij_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npcId()) instanceof GuhNpcEntity npc) {
                BakkerijGame.action(npc, player, payload.action());
            }
        }
    }

    /** Slide it in (stap 0: deeg / vorm / topping) or take it out (stap 1: ticks = how long the screen saw it bake). */
    public record Bak(long oven, int stap, int deeg, int vorm, int topping, int ticks) implements CustomPacketPayload {
        public static final Type<Bak> TYPE = new Type<>(Guhs.id("bakkerij_bak"));
        public static final StreamCodec<FriendlyByteBuf, Bak> STREAM_CODEC = StreamCodec.of((buf, b) -> {
            buf.writeLong(b.oven());
            buf.writeVarInt(b.stap());
            buf.writeVarInt(b.deeg());
            buf.writeVarInt(b.vorm());
            buf.writeVarInt(b.topping());
            buf.writeVarInt(b.ticks() + 1);
        }, buf -> new Bak(buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt() - 1));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Bak b, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos oven = BlockPos.of(b.oven());
            if (b.stap() == IN_DE_OVEN) {
                Recept.Deeg d = Recept.Deeg.byIndex(b.deeg());
                Recept.Vorm v = Recept.Vorm.byIndex(b.vorm());
                Recept.Topping t = Recept.Topping.byIndex(b.topping());
                String nee = d == null || v == null || t == null ? "gui.guhs.bakkerij.geen_recept" : Bakken.start(player, oven, d, v, t);
                BakkerijGame game = BakkerijGame.gameOf(player);
                CompoundTag data = Bakken.status(player, game != null && game.oven(oven) ? game : null);
                if (nee != null) {
                    data.putString("Nee", nee);
                } else {
                    Recept r = Recept.van(d, v, t);
                    data.putInt("Bakt", r.ordinal());
                    data.putInt("Al", 0);
                    data.putInt("BakTicks", r.bakTicks);
                }
                nl.juiced.guhs.network.ModNetworking.sendTo(player, new Status(data));
            } else if (b.stap() == ERUIT) {
                if (Bakken.bakt(player) && !Bakken.bijDeOven(player)) {
                    BakkerijGame game = BakkerijGame.gameOf(player);
                    CompoundTag data = Bakken.status(player, game != null && game.oven(oven) ? game : null);
                    data.putString("Nee", "gui.guhs.bakkerij.te_ver");
                    nl.juiced.guhs.network.ModNetworking.sendTo(player, new Status(data));
                    return;
                }
                Bakken.Uit uit = Bakken.eruit(player, b.ticks());
                if (uit != null) {
                    nl.juiced.guhs.network.ModNetworking.sendTo(player, new Status(Bakken.uitStatus(player, uit)));
                }
            }
        }
    }

    public record Status(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(Guhs.id("bakkerij_status"));
        public static final StreamCodec<FriendlyByteBuf, Status> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Status::data, Status::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Status payload, IPayloadContext context) {
            nl.juiced.guhs.feature.bakkerij.client.BakkerijClient.status(payload);
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        registrar.playToServer(Bak.TYPE, Bak.STREAM_CODEC, Bak::handle);
        registrar.playToClient(Status.TYPE, Status.STREAM_CODEC, Status::handle);
    }

    private BakkerijPayloads() {
    }
}
