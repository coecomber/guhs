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
 * De Erfenis (POC): the network. The client only sends intents ({@link Actie}); the server checks them against the base of
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

    /** What the client wants. {@code soort}: one of the constants below; the other fields as that soort needs them. */
    public record Actie(int soort, int id, int x, int z, int x2, int z2, int rot, String gebouw) implements CustomPacketPayload {
        public static final int OPEN = 0, PLAATS = 1, LIJN = 2, VERPLAATS = 3, DRAAI = 4, VERBETER = 5, OPHALEN = 6, GA = 7, TERUG = 8;
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("erfenis_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.ofMember(Actie::write, Actie::new);

        private Actie(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readUtf(32));
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeVarInt(soort);
            buf.writeVarInt(id);
            buf.writeVarInt(x);
            buf.writeVarInt(z);
            buf.writeVarInt(x2);
            buf.writeVarInt(z2);
            buf.writeVarInt(rot);
            buf.writeUtf(gebouw, 32);
        }

        public static Actie simpel(int soort) {
            return new Actie(soort, 0, 0, 0, 0, 0, 0, "");
        }

        public static Actie op(int soort, int id) {
            return new Actie(soort, id, 0, 0, 0, 0, 0, "");
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
