package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * De Erfenis: the network. The client only sends intents ({@link Actie}); the server checks them against the base of
 * the plot the player stands on, changes and stamps it, and sends the whole base back ({@link Sync}). For a POC a full
 * sync after every change is fine (a base is a few KB).
 */
public final class ErfenisPayloads {
    /** Client hooks (set by ErfenisClient; the common code never touches client classes). */
    public static Consumer<Sync> syncOntvanger = s -> {
    };
    public static Consumer<Vraag> vraagOntvanger = v -> {
    };
    public static Consumer<Melding> meldingOntvanger = m -> {
    };
    public static Consumer<PensionData> pensionOntvanger = m -> {
    };

    /** The Guhpension screen's lists (see {@link Guhpension#data}); opens or refreshes it. */
    public record PensionData(CompoundTag data) implements CustomPacketPayload {
        public static final Type<PensionData> TYPE = new Type<>(Guhs.id("erfenis_pension"));
        public static final StreamCodec<FriendlyByteBuf, PensionData> STREAM_CODEC = StreamCodec.ofMember(PensionData::write, PensionData::new);

        private PensionData(FriendlyByteBuf buf) {
            this(buf.readNbt());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeNbt(data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * The whole base (as {@link Base#save()}), the server's clock (millis) when sent, whether the receiver owns it (false:
     * read-only overview), whether the overview should open now, and the dev switch "snel".
     */
    public record Sync(CompoundTag base, long serverNu, boolean eigen, boolean open, boolean snel) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Guhs.id("erfenis_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.ofMember(Sync::write, Sync::new);

        private Sync(FriendlyByteBuf buf) {
            this(buf.readNbt(), buf.readLong(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeNbt(base);
            buf.writeLong(serverNu);
            buf.writeBoolean(eigen);
            buf.writeBoolean(open);
            buf.writeBoolean(snel);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Naar je erfenis?" (the overview key was pressed outside the dimension). */
    public record Vraag() implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("erfenis_vraag"));
        public static final StreamCodec<FriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.unit(new Vraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A short message for the overview (its own toast: the HUD is hidden there). Args: numbers or lang keys. */
    public record Melding(String key, List<String> args, boolean goed) implements CustomPacketPayload {
        public static final Type<Melding> TYPE = new Type<>(Guhs.id("erfenis_melding"));
        public static final StreamCodec<FriendlyByteBuf, Melding> STREAM_CODEC = StreamCodec.ofMember(Melding::write, Melding::new);

        private Melding(FriendlyByteBuf buf) {
            this(buf.readUtf(), lees(buf), buf.readBoolean());
        }

        private static List<String> lees(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<String> l = new ArrayList<>();
            for (int i = 0; i < Math.min(n, 8); i++) {
                l.add(buf.readUtf());
            }
            return l;
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(key);
            buf.writeVarInt(args.size());
            for (String a : args) {
                buf.writeUtf(a);
            }
            buf.writeBoolean(goed);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * What the client wants. {@code soort}: one of the constants below; the other fields as that soort needs them
     * ({@code tekst}: a good / item / uuid / list of block ids, up to 8192 chars).
     */
    public record Actie(int soort, int id, int x, int z, int x2, int z2, int rot, String tekst) implements CustomPacketPayload {
        public static final int OPEN = 0, PLAATS = 1, VERPLAATS = 3, DRAAI = 4, VERBETER = 5, OPHALEN = 6, GA = 7, TERUG = 8,
                OPRUIMEN = 9, KAVEL = 10, VERF = 11, KIES = 12, KLEUR = 13, SELECTIE = 14, NAAR_HUIS = 15, LOKET = 16, BUREAU = 17,
                MARKT = 18, PENSION = 19, OPHALEN_ALLES = 20;
        /** {@link #PENSION} sub-actions in {@code x}: the lists, send a guh, bring one home, which building it helps ({@code id}). */
        public static final int PENSION_OPEN = 0, PENSION_STUUR = 1, PENSION_HAAL = 2, PENSION_HELPT = 3;
        /** {@link #GA} with this as {@code id}: and open the own overview once there ("Terug naar mijn erfenis"). */
        public static final int GA_EN_OPEN = 1;
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("erfenis_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.ofMember(Actie::write, Actie::new);

        private Actie(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readUtf(8192));
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeVarInt(soort);
            buf.writeVarInt(id);
            buf.writeVarInt(x);
            buf.writeVarInt(z);
            buf.writeVarInt(x2);
            buf.writeVarInt(z2);
            buf.writeVarInt(rot);
            buf.writeUtf(tekst, 8192);
        }

        /** The POC name of {@link #tekst} (a building type id). */
        public String gebouw() {
            return tekst;
        }

        public static Actie simpel(int soort) {
            return new Actie(soort, 0, 0, 0, 0, 0, 0, "");
        }

        public static Actie op(int soort, int id) {
            return new Actie(soort, id, 0, 0, 0, 0, 0, "");
        }

        public static Actie met(int soort, int id, int x, String tekst) {
            return new Actie(soort, id, x, 0, 0, 0, 0, tekst);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (p, c) -> syncOntvanger.accept(p));
        registrar.playToClient(Vraag.TYPE, Vraag.STREAM_CODEC, (p, c) -> vraagOntvanger.accept(p));
        registrar.playToClient(Melding.TYPE, Melding.STREAM_CODEC, (p, c) -> meldingOntvanger.accept(p));
        registrar.playToClient(PensionData.TYPE, PensionData.STREAM_CODEC, (p, c) -> pensionOntvanger.accept(p));
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, ErfenisPayloads::opActie);
    }

    private static void opActie(Actie a, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer p) {
            Erfenis.actie(p, a);
        }
    }

    private ErfenisPayloads() {
    }
}
