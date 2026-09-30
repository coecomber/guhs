package nl.juiced.guhs.feature.creche;

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
 * The Knuffelcreche's messages: Juf Knuffel's screen (guhs:creche_open / guhs:creche_action) and the lullaby rhythm game
 * (guhs:creche_liedje opens it for a crib, guhs:creche_liedje_klaar sends back how many notes were tapped in time).
 */
public final class CrechePayloads {
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("creche_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.creche.client.CrecheClient.open(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("creche_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc) {
                CrecheGame.action(npc, player, p.action());
            }
        }
    }

    /** Opens the lullaby screen: sing this song for the baby in the crib at pos. */
    public record Liedje(int npcId, BlockPos pos, int liedje) implements CustomPacketPayload {
        public static final Type<Liedje> TYPE = new Type<>(Guhs.id("creche_liedje"));
        public static final StreamCodec<FriendlyByteBuf, Liedje> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Liedje::npcId, BlockPos.STREAM_CODEC, Liedje::pos, ByteBufCodecs.VAR_INT, Liedje::liedje, Liedje::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Liedje payload, IPayloadContext context) {
            nl.juiced.guhs.feature.creche.client.CrecheClient.liedje(payload);
        }
    }

    /** The lullaby is over: this many of the song's notes were tapped in time. */
    public record LiedjeKlaar(int npcId, BlockPos pos, int liedje, int raak) implements CustomPacketPayload {
        public static final Type<LiedjeKlaar> TYPE = new Type<>(Guhs.id("creche_liedje_klaar"));
        public static final StreamCodec<FriendlyByteBuf, LiedjeKlaar> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, LiedjeKlaar::npcId, BlockPos.STREAM_CODEC, LiedjeKlaar::pos, ByteBufCodecs.VAR_INT, LiedjeKlaar::liedje,
                ByteBufCodecs.VAR_INT, LiedjeKlaar::raak, LiedjeKlaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(LiedjeKlaar p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc) {
                CrecheGame.of(npc).liedjeKlaar(npc, player, p.pos(), p.liedje(), p.raak());
            }
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        registrar.playToClient(Liedje.TYPE, Liedje.STREAM_CODEC, Liedje::handle);
        registrar.playToServer(LiedjeKlaar.TYPE, LiedjeKlaar.STREAM_CODEC, LiedjeKlaar::handle);
    }

    private CrechePayloads() {
    }
}
