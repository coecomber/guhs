package nl.juiced.guhs.feature.theehuis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * The Knabbelthee-huisje's messages: Mevrouw Theelepel's screen (guhs:theehuis_open / guhs:theehuis_action) and the
 * wishes of the guests (guhs:theehuis_wensen: which guh wants tea or cake, for the bubbles above their heads).
 */
public final class TheehuisPayloads {
    /** Client: guh entity id -> its wish (Theekransje.Wens ordinal; 0 = a guest that wants nothing right now). */
    public static final Map<Integer, Integer> CLIENT_WENSEN = new ConcurrentHashMap<>();
    /** Client: per Theelepel (entity id) the flat list she sent last (id, wish, id, wish...). */
    private static final Map<Integer, List<Integer>> PER_THEELEPEL = new ConcurrentHashMap<>();

    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("theehuis_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.theehuis.client.TheehuisClient.open(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("theehuis_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc) {
                Theekransje.action(npc, player, p.action());
            }
        }
    }

    /** The guests of one Theelepel's kransje and what they want: pairs (guh entity id, wish). Empty: the kransje is over. */
    public record Wensen(int theelepel, List<Integer> data) implements CustomPacketPayload {
        public static final Type<Wensen> TYPE = new Type<>(Guhs.id("theehuis_wensen"));
        public static final StreamCodec<FriendlyByteBuf, Wensen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Wensen::theelepel, ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT), Wensen::data, Wensen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Wensen p, IPayloadContext context) {
            PER_THEELEPEL.put(p.theelepel(), List.copyOf(p.data()));
            CLIENT_WENSEN.clear();
            for (List<Integer> list : PER_THEELEPEL.values()) {
                for (int i = 0; i + 1 < list.size(); i += 2) {
                    CLIENT_WENSEN.put(list.get(i), list.get(i + 1));
                }
            }
        }
    }

    /** (Client, leaving a world) no bubbles from an old world. */
    public static void clientVergeet() {
        PER_THEELEPEL.clear();
        CLIENT_WENSEN.clear();
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        registrar.playToClient(Wensen.TYPE, Wensen.STREAM_CODEC, Wensen::handle);
    }

    private TheehuisPayloads() {
    }
}
