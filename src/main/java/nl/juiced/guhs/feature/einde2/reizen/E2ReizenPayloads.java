package nl.juiced.guhs.feature.einde2.reizen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * einde2 phase B, slice reizen: its messages.
 * <ul>
 *   <li>{@code guhs:einde2_reizen_bestemmingen} (server to client): Appa's whistle list ({@link Fluitlijst});</li>
 *   <li>{@code guhs:einde2_reizen_kies} (client to server): the place picked (checked again);</li>
 *   <li>{@code guhs:einde2_reizen_inrichting} (server to client): the furniture screen of an own whale ({@link Walvis});</li>
 *   <li>{@code guhs:einde2_reizen_inrichting_zet} (client to server): a part on or off ({@code waarde} 1/0), or the flag's
 *       colour ({@code deel} = "vlag_kleur").</li>
 * </ul>
 */
public final class E2ReizenPayloads {
    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Bestemmingen.TYPE, Bestemmingen.STREAM_CODEC, Bestemmingen::handle);
        registrar.playToServer(Kies.TYPE, Kies.STREAM_CODEC, Kies::handle);
        registrar.playToClient(Inrichting.TYPE, Inrichting.STREAM_CODEC, Inrichting::handle);
        registrar.playToServer(InrichtingZet.TYPE, InrichtingZet.STREAM_CODEC, InrichtingZet::handle);
    }

    /** data: "Haltes" = a list of {Id, Naam (lang key), Afstand (blocks, -1 unknown), Verder (the waiting story flight)}. */
    public record Bestemmingen(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Bestemmingen> TYPE = new Type<>(Guhs.id("einde2_reizen_bestemmingen"));
        public static final StreamCodec<FriendlyByteBuf, Bestemmingen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Bestemmingen::data, Bestemmingen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Bestemmingen payload, IPayloadContext context) {
            nl.juiced.guhs.feature.einde2.reizen.client.E2ReizenClient.fluitlijst(payload.data());
        }
    }

    public record Kies(String structuur) implements CustomPacketPayload {
        public static final Type<Kies> TYPE = new Type<>(Guhs.id("einde2_reizen_kies"));
        public static final StreamCodec<FriendlyByteBuf, Kies> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Kies::structuur, Kies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kies payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p) {
                Fluitlijst.kies(p, payload.structuur());
            }
        }
    }

    /** data: Delen (bits of Walvis.DELEN), Vlag (colour 0..5). */
    public record Inrichting(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Inrichting> TYPE = new Type<>(Guhs.id("einde2_reizen_inrichting"));
        public static final StreamCodec<FriendlyByteBuf, Inrichting> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Inrichting::data, Inrichting::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Inrichting payload, IPayloadContext context) {
            nl.juiced.guhs.feature.einde2.reizen.client.E2ReizenClient.inrichting(payload.data());
        }
    }

    public record InrichtingZet(String deel, int waarde) implements CustomPacketPayload {
        public static final Type<InrichtingZet> TYPE = new Type<>(Guhs.id("einde2_reizen_inrichting_zet"));
        public static final StreamCodec<FriendlyByteBuf, InrichtingZet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, InrichtingZet::deel, ByteBufCodecs.VAR_INT, InrichtingZet::waarde, InrichtingZet::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(InrichtingZet payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer p)) {
                return;
            }
            if (payload.deel().equals("vlag_kleur")) {
                Walvis.zetVlag(p, payload.waarde());
            } else {
                Walvis.zetDeel(p, payload.deel(), payload.waarde() != 0);
            }
        }
    }

    private E2ReizenPayloads() {
    }
}
