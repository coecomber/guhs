package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * De Erfenis, wereldkaart: the network. Client -&gt; server {@link Vraag} (open / refresh the map) and {@link Actie} (visit a
 * village); server -&gt; client {@link Kaart} (every village, compact, plus the news). The client side is {@link #KLANT}
 * (set by client.KaartClient: common code never touches client classes).
 */
public final class KaartPayloads {
    public static volatile Consumer<Kaart> KLANT;

    private KaartPayloads() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, Vraag::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
        registrar.playToClient(Kaart.TYPE, Kaart.STREAM_CODEC, Kaart::handle);
    }

    /** "Show me the map" ({@code open}: open the screen; false: just a refresh while it is open). */
    public record Vraag(boolean open) implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("erfenis_kaart_vraag"));
        public static final StreamCodec<FriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.ofMember((v, b) -> b.writeBoolean(v.open),
                b -> new Vraag(b.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Vraag v, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer p) {
                    KaartServer.stuur(p, v.open);
                }
            });
        }
    }

    /** Visit a village ({@link #BEZOEK}) or visit and look from above ({@link #BOVENAANZICHT}). */
    public record Actie(int soort, UUID dorp) implements CustomPacketPayload {
        public static final int BEZOEK = 0, BOVENAANZICHT = 1;
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("erfenis_kaart_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.ofMember((a, b) -> {
            b.writeVarInt(a.soort);
            b.writeUUID(a.dorp);
        }, b -> new Actie(b.readVarInt(), b.readUUID()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Actie a, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer p) {
                    KaartServer.actie(p, a.soort, a.dorp);
                }
            });
        }
    }

    /**
     * The whole map: the world's map seed (the land shapes), the viewer's own village (or null), whether to open the screen,
     * the server's clock (news ages), every village and the news (newest first).
     */
    public record Kaart(long seed, @Nullable UUID eigen, boolean open, long serverNu, List<KaartDorp> dorpen, List<KaartNieuws> nieuws)
            implements CustomPacketPayload {
        public static final Type<Kaart> TYPE = new Type<>(Guhs.id("erfenis_kaart"));
        public static final StreamCodec<FriendlyByteBuf, Kaart> STREAM_CODEC = StreamCodec.ofMember(Kaart::write, Kaart::read);

        private void write(FriendlyByteBuf buf) {
            buf.writeLong(seed);
            buf.writeBoolean(eigen != null);
            if (eigen != null) {
                buf.writeUUID(eigen);
            }
            buf.writeBoolean(open);
            buf.writeLong(serverNu);
            buf.writeVarInt(dorpen.size());
            for (KaartDorp d : dorpen) {
                d.write(buf);
            }
            buf.writeVarInt(nieuws.size());
            for (KaartNieuws n : nieuws) {
                n.write(buf);
            }
        }

        private static Kaart read(FriendlyByteBuf buf) {
            long seed = buf.readLong();
            UUID eigen = buf.readBoolean() ? buf.readUUID() : null;
            boolean open = buf.readBoolean();
            long nu = buf.readLong();
            int n = Math.min(4096, buf.readVarInt());
            List<KaartDorp> d = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                d.add(KaartDorp.read(buf));
            }
            int m = Math.min(256, buf.readVarInt());
            List<KaartNieuws> l = new ArrayList<>(m);
            for (int i = 0; i < m; i++) {
                l.add(KaartNieuws.read(buf));
            }
            return new Kaart(seed, eigen, open, nu, d, l);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Kaart k, IPayloadContext context) {
            Consumer<Kaart> klant = KLANT;
            if (klant != null) {
                context.enqueueWork(() -> klant.accept(k));
            }
        }
    }
}
