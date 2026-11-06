package nl.juiced.guhs.feature.vadsrijk.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** U-HUD: the network messages (client handlers are set by {@code hud.client.UHudClient}). */
public final class UHudPayloads {
    public static final int STAAT_OK = 0, STAAT_FLAUW = 1, STAAT_OFFLINE = 2;

    /**
     * One party member for the frames. {@code buffs} = kern Status ordinals (at most {@link PartyFrames#MAX_BUFFS});
     * {@code dim} = the dimension id ("" when offline); x/y/z = where they are (for the distance arrow).
     */
    public record Frame(UUID id, String naam, String klasse, String spec, int level, float vads, float max, List<Integer> buffs, String dim,
            float x, float y, float z, int staat, boolean leider) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Frame> STREAM_CODEC = StreamCodec.ofMember((f, b) -> {
            UUIDUtil.STREAM_CODEC.encode(b, f.id());
            b.writeUtf(f.naam(), 32);
            b.writeUtf(f.klasse(), 64);
            b.writeUtf(f.spec(), 128);
            b.writeVarInt(f.level());
            b.writeFloat(f.vads());
            b.writeFloat(f.max());
            b.writeVarInt(f.buffs().size());
            for (int s : f.buffs()) {
                b.writeByte(s);
            }
            b.writeUtf(f.dim(), 128);
            b.writeFloat(f.x());
            b.writeFloat(f.y());
            b.writeFloat(f.z());
            b.writeByte(f.staat());
            b.writeBoolean(f.leider());
        }, b -> {
            UUID id = UUIDUtil.STREAM_CODEC.decode(b);
            String naam = b.readUtf(32), klasse = b.readUtf(64), spec = b.readUtf(128);
            int level = b.readVarInt();
            float vads = b.readFloat(), max = b.readFloat();
            int n = Math.min(16, b.readVarInt());
            List<Integer> buffs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                buffs.add((int) b.readByte());
            }
            String dim = b.readUtf(128);
            float x = b.readFloat(), y = b.readFloat(), z = b.readFloat();
            return new Frame(id, naam, klasse, spec, level, vads, max, buffs, dim, x, y, z, b.readByte(), b.readBoolean());
        });
    }

    /** Server -> client, every 10 ticks while you are in a party (an empty list once when it ends): the OTHER members. */
    public record Party(List<Frame> leden, boolean nep) implements CustomPacketPayload {
        public static final Type<Party> TYPE = new Type<>(Guhs.id("vr_u_hud_party"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Party> STREAM_CODEC = StreamCodec.composite(
                Frame.STREAM_CODEC.apply(ByteBufCodecs.list(8)), Party::leden, ByteBufCodecs.BOOL, Party::nep, Party::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: target this party member (a click on their frame with the chat open). */
    public record Doel(UUID lid) implements CustomPacketPayload {
        public static final Type<Doel> TYPE = new Type<>(Guhs.id("vr_u_hud_doel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doel> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Doel::lid, Doel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: play this emote ("" = stop). */
    public record EmoteVraag(String emote) implements CustomPacketPayload {
        public static final Type<EmoteVraag> TYPE = new Type<>(Guhs.id("vr_u_hud_emote_vraag"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmoteVraag> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32),
                EmoteVraag::emote, EmoteVraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client (the player + everyone tracking them): entity {@code entity} plays {@code emote} ("" = stops), already {@code verstreken} ticks in. */
    public record EmoteSpeel(int entity, String emote, int verstreken) implements CustomPacketPayload {
        public static final Type<EmoteSpeel> TYPE = new Type<>(Guhs.id("vr_u_hud_emote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmoteSpeel> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, EmoteSpeel::entity,
                ByteBufCodecs.stringUtf8(32), EmoteSpeel::emote, ByteBufCodecs.VAR_INT, EmoteSpeel::verstreken, EmoteSpeel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: open the HUD editor (the dev command; players use the pause menu or /hudbewerker). */
    public record OpenBewerker() implements CustomPacketPayload {
        public static final Type<OpenBewerker> TYPE = new Type<>(Guhs.id("vr_u_hud_bewerker"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBewerker> STREAM_CODEC = StreamCodec.unit(new OpenBewerker());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static Consumer<Party> partyOntvanger = p -> {
    };
    public static Consumer<EmoteSpeel> emoteOntvanger = e -> {
    };
    public static Runnable bewerkerOntvanger = () -> {
    };

    private UHudPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Party.TYPE, Party.STREAM_CODEC, (p, c) -> partyOntvanger.accept(p));
        r.playToClient(EmoteSpeel.TYPE, EmoteSpeel.STREAM_CODEC, (p, c) -> emoteOntvanger.accept(p));
        r.playToClient(OpenBewerker.TYPE, OpenBewerker.STREAM_CODEC, (p, c) -> bewerkerOntvanger.run());
        r.playToServer(Doel.TYPE, Doel.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                PartyFrames.kies(sp, p.lid());
            }
        });
        r.playToServer(EmoteVraag.TYPE, EmoteVraag.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                Emote e = Emote.van(p.emote());
                if (e == null) {
                    Emotes.stop(sp);
                } else {
                    Emotes.speel(sp, e, true);
                }
            }
        });
    }
}
