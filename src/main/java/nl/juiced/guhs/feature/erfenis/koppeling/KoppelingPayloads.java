package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/** The letter's screen: the server opens it ({@link BriefOpen}), the buy button asks the server ({@link BriefKoop}). */
public final class KoppelingPayloads {
    /** Client hook (set by client.KoppelingClient; the common code never touches client classes). */
    public static Consumer<BriefOpen> briefOntvanger = b -> {
    };

    /** Open the makelaarsbrief: already bought (then the stamp and "extra hekje"), and how many kaasknabbels the reader has. */
    public record BriefOpen(boolean gekocht, int knabbels) implements CustomPacketPayload {
        public static final Type<BriefOpen> TYPE = new Type<>(Guhs.id("erfenis_brief_open"));
        public static final StreamCodec<FriendlyByteBuf, BriefOpen> STREAM_CODEC = StreamCodec.ofMember(BriefOpen::write, BriefOpen::new);

        private BriefOpen(FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readVarInt());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeBoolean(gekocht);
            buf.writeVarInt(knabbels);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Kopen voor 1 kaasknabbel". */
    public record BriefKoop() implements CustomPacketPayload {
        public static final Type<BriefKoop> TYPE = new Type<>(Guhs.id("erfenis_brief_koop"));
        public static final StreamCodec<FriendlyByteBuf, BriefKoop> STREAM_CODEC = StreamCodec.unit(new BriefKoop());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(BriefOpen.TYPE, BriefOpen.STREAM_CODEC, (p, c) -> briefOntvanger.accept(p));
        registrar.playToServer(BriefKoop.TYPE, BriefKoop.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                Makelaarsbrief.koop(sp);
            }
        });
    }

    static void stuur(ServerPlayer p, CustomPacketPayload payload) {
        ModNetworking.sendTo(p, payload);
    }

    private KoppelingPayloads() {
    }
}
