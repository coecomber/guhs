package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The tekenen payloads ({@code guhs:guhwinter_teken_*}, {@code guhs:guhwinter_album_*}). Drawings travel deflated
 * ({@link TekenPixels#pak}, at most {@link TekenPixels#MAX_PAK} bytes: the codec refuses more before anything is decoded).
 */
public final class TekenPayloads {
    private static final StreamCodec<ByteBuf, byte[]> PIXELS = ByteBufCodecs.byteArray(TekenPixels.MAX_PAK);

    private TekenPayloads() {
    }

    /** server -> client: open the editor (session token, purpose, the drawing to edit or an empty array). */
    public record Open(int token, String doel, byte[] pixels) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("guhwinter_teken_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::token, ByteBufCodecs.stringUtf8(32), Open::doel, PIXELS, Open::pixels, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Open p, IPayloadContext c) {
            nl.juiced.guhs.feature.guhwinter.tekenen.client.TekenenClient.open(p);
        }
    }

    /** client -> server: save the drawing of this session. */
    public record Opslaan(int token, byte[] pixels) implements CustomPacketPayload {
        public static final Type<Opslaan> TYPE = new Type<>(Guhs.id("guhwinter_teken_opslaan"));
        public static final StreamCodec<ByteBuf, Opslaan> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Opslaan::token, PIXELS, Opslaan::pixels, Opslaan::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Opslaan p, IPayloadContext c) {
            if (c.player() instanceof ServerPlayer sp) {
                Tekeningen.opslaan(sp, p.token(), p.pixels());
            }
        }
    }

    /** server -> client: how the save went (ok: the editor closes; else the lang key of the reason). */
    public record Antwoord(int token, boolean ok, String sleutel) implements CustomPacketPayload {
        public static final Type<Antwoord> TYPE = new Type<>(Guhs.id("guhwinter_teken_antwoord"));
        public static final StreamCodec<ByteBuf, Antwoord> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Antwoord::token, ByteBufCodecs.BOOL, Antwoord::ok, ByteBufCodecs.stringUtf8(128), Antwoord::sleutel,
                Antwoord::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Antwoord p, IPayloadContext c) {
            nl.juiced.guhs.feature.guhwinter.tekenen.client.TekenenClient.antwoord(p);
        }
    }

    /** client -> server: the editor was closed without saving. */
    public record Annuleer(int token) implements CustomPacketPayload {
        public static final Type<Annuleer> TYPE = new Type<>(Guhs.id("guhwinter_teken_annuleer"));
        public static final StreamCodec<ByteBuf, Annuleer> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Annuleer::token, Annuleer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Annuleer p, IPayloadContext c) {
            if (c.player() instanceof ServerPlayer sp) {
                Tekeningen.annuleer(sp, p.token());
            }
        }
    }

    /** client -> server: send me this drawing. */
    public record Vraag(UUID id) implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("guhwinter_teken_vraag"));
        public static final StreamCodec<ByteBuf, Vraag> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Vraag::id, Vraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Vraag p, IPayloadContext c) {
            if (c.player() instanceof ServerPlayer sp) {
                Tekeningen.vraag(sp, p.id());
            }
        }
    }

    /** server -> client: a drawing (empty pixels: unknown id). */
    public record Data(UUID id, String maker, byte[] pixels) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(Guhs.id("guhwinter_teken_data"));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Data::id, ByteBufCodecs.stringUtf8(64), Data::maker, PIXELS, Data::pixels, Data::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Data p, IPayloadContext c) {
            nl.juiced.guhs.feature.guhwinter.tekenen.client.TekeningCache.ontvang(p.id(), p.maker(), p.pixels());
        }
    }

    /** server -> client: this drawing changed, drop the cached picture. */
    public record Vergeet(UUID id) implements CustomPacketPayload {
        public static final Type<Vergeet> TYPE = new Type<>(Guhs.id("guhwinter_teken_vergeet"));
        public static final StreamCodec<ByteBuf, Vergeet> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Vergeet::id, Vergeet::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Vergeet p, IPayloadContext c) {
            nl.juiced.guhs.feature.guhwinter.tekenen.client.TekeningCache.vergeet(p.id());
        }
    }

    /** One page of the Kerstalbum. */
    public record AlbumRij(UUID id, String maker, boolean gepakt) {
        public static final StreamCodec<ByteBuf, AlbumRij> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, AlbumRij::id, ByteBufCodecs.stringUtf8(64), AlbumRij::maker, ByteBufCodecs.BOOL, AlbumRij::gepakt,
                AlbumRij::new);
    }

    /** server -> client: open (or refresh) the Kerstalbum at this page. */
    public record AlbumOpen(List<AlbumRij> rijen, int pagina) implements CustomPacketPayload {
        public static final Type<AlbumOpen> TYPE = new Type<>(Guhs.id("guhwinter_album_open"));
        public static final StreamCodec<ByteBuf, AlbumOpen> STREAM_CODEC = StreamCodec.composite(
                AlbumRij.STREAM_CODEC.apply(ByteBufCodecs.list(4096)), AlbumOpen::rijen, ByteBufCodecs.VAR_INT, AlbumOpen::pagina, AlbumOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(AlbumOpen p, IPayloadContext c) {
            nl.juiced.guhs.feature.guhwinter.tekenen.client.TekenenClient.album(p);
        }
    }

    /** client -> server: "Ophangen-versie pakken" for this drawing (the page to come back to). */
    public record AlbumPak(UUID id, int pagina) implements CustomPacketPayload {
        public static final Type<AlbumPak> TYPE = new Type<>(Guhs.id("guhwinter_album_pak"));
        public static final StreamCodec<ByteBuf, AlbumPak> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, AlbumPak::id, ByteBufCodecs.VAR_INT, AlbumPak::pagina, AlbumPak::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(AlbumPak p, IPayloadContext c) {
            if (c.player() instanceof ServerPlayer sp) {
                Kerstalbum.pak(sp, p.id(), p.pagina());
            }
        }
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        r.playToServer(Opslaan.TYPE, Opslaan.STREAM_CODEC, Opslaan::handle);
        r.playToClient(Antwoord.TYPE, Antwoord.STREAM_CODEC, Antwoord::handle);
        r.playToServer(Annuleer.TYPE, Annuleer.STREAM_CODEC, Annuleer::handle);
        r.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, Vraag::handle);
        r.playToClient(Data.TYPE, Data.STREAM_CODEC, Data::handle);
        r.playToClient(Vergeet.TYPE, Vergeet.STREAM_CODEC, Vergeet::handle);
        r.playToClient(AlbumOpen.TYPE, AlbumOpen.STREAM_CODEC, AlbumOpen::handle);
        r.playToServer(AlbumPak.TYPE, AlbumPak.STREAM_CODEC, AlbumPak::handle);
    }
}
