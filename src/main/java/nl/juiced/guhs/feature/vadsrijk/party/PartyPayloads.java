package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;

/** W-PARTY: the network messages of the Party tab and the nep-speler setting (client handlers: {@code party.client.PartyClient}). */
public final class PartyPayloads {
    /** Server -> client: the Party tab's data ({@code open}: open the tab now; otherwise only refresh it when it is open). */
    public record Staat(CompoundTag data, boolean open) implements CustomPacketPayload {
        public static final Type<Staat> TYPE = new Type<>(Guhs.id("vr_party_staat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Staat> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Staat::data,
                ByteBufCodecs.BOOL, Staat::open, Staat::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: somebody invited you (a toast; the clickable [Accepteer]/[Weiger] is in the chat). */
    public record Uitnodiging(String van) implements CustomPacketPayload {
        public static final Type<Uitnodiging> TYPE = new Type<>(Guhs.id("vr_party_uitnodiging"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Uitnodiging> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32),
                Uitnodiging::van, Uitnodiging::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: send me the Party tab data ({@code open}: and open it). */
    public record Vraag(boolean open) implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("vr_party_vraag"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Vraag::open, Vraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: a button in the Party tab ({@link PartyDienst#actie}). */
    public record Actie(String actie, String arg) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("vr_party_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32), Actie::actie,
                ByteBufCodecs.stringUtf8(64), Actie::arg, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: this player's setting "Nep-spelers: aan/uit" (sent at login and when it changes). */
    public record NepStand(boolean aan) implements CustomPacketPayload {
        public static final Type<NepStand> TYPE = new Type<>(Guhs.id("vr_party_nepstand"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NepStand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, NepStand::aan,
                NepStand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static Consumer<Staat> staatOntvanger = s -> {
    };
    public static Consumer<Uitnodiging> uitnodigingOntvanger = u -> {
    };

    private PartyPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Staat.TYPE, Staat.STREAM_CODEC, (p, c) -> staatOntvanger.accept(p));
        r.playToClient(Uitnodiging.TYPE, Uitnodiging.STREAM_CODEC, (p, c) -> uitnodigingOntvanger.accept(p));
        r.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                PartyDienst.stuur(sp, p.open());
            }
        });
        r.playToServer(Actie.TYPE, Actie.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                PartyDienst.actie(sp, p.actie(), p.arg());
            }
        });
        r.playToServer(NepStand.TYPE, NepStand.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                NepSpelers.zetStand(sp, p.aan());
            }
        });
    }
}
