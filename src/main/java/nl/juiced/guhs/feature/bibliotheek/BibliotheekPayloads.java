package nl.juiced.guhs.feature.bibliotheek;

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
 * The network messages of the guh library: the Bibliothecaris' screen (to the client) and its buttons (to the server), and
 * a guh book on a lectern (to the client) with its "take a copy" button (to the server).
 */
public final class BibliotheekPayloads {

    /** Opens the Bibliothecaris' screen with the player's library card (see Bibliothecaris.screenData). */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("bibliotheek_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.bibliotheek.client.BibliotheekClient.open(payload);
        }
    }

    /** A button in the screen (Bibliothecaris.SHOP, REFRESH, ANSWER + i). */
    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("bibliotheek_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc) {
                Bibliothecaris.action(npc, player, p.action());
            }
        }
    }

    /** Opens the guh book lying on the lectern (or book stand) at pos; canTake: this player may still take their copy. */
    public record OpenBook(BlockPos pos, int book, boolean canTake) implements CustomPacketPayload {
        public static final Type<OpenBook> TYPE = new Type<>(Guhs.id("bibliotheek_open_book"));
        public static final StreamCodec<FriendlyByteBuf, OpenBook> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenBook::pos, ByteBufCodecs.VAR_INT, OpenBook::book, ByteBufCodecs.BOOL, OpenBook::canTake, OpenBook::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(OpenBook payload, IPayloadContext context) {
            nl.juiced.guhs.feature.bibliotheek.client.BibliotheekClient.openBook(payload);
        }
    }

    /** The "take a copy" button of the lectern book at pos. */
    public record TakeBook(BlockPos pos) implements CustomPacketPayload {
        public static final Type<TakeBook> TYPE = new Type<>(Guhs.id("bibliotheek_take_book"));
        public static final StreamCodec<FriendlyByteBuf, TakeBook> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TakeBook::pos, TakeBook::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(TakeBook p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Leeszaal.take(player, p.pos());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        registrar.playToClient(OpenBook.TYPE, OpenBook.STREAM_CODEC, OpenBook::handle);
        registrar.playToServer(TakeBook.TYPE, TakeBook.STREAM_CODEC, TakeBook::handle);
    }

    private BibliotheekPayloads() {
    }
}
