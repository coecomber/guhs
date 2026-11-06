package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * T-KLEDING network: the wardrobe screen ({@link Scherm} / {@link Actie} / {@link Open}), how a player looks for every client
 * ({@link Uiterlijk}), the glide rules for your own client ({@link Staat}) and its glide state back ({@link Zweef}), and a
 * cheer-up effect to show ({@link Plof}).
 */
public final class KledingPayloads {
    /** Server -> client: open or refresh the wardrobe (see {@link Kledingkast#schermData}). */
    public record Scherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_t_kleding_scherm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,
                Scherm::data, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: a click in the wardrobe ("kies" soort id | "tm" slot item | "titel" - id | "ververs"). */
    public record Actie(String actie, String a, String b) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("vr_t_kleding_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Actie::actie,
                ByteBufCodecs.STRING_UTF8, Actie::a, ByteBufCodecs.STRING_UTF8, Actie::b, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: open the wardrobe (the key, the tab). */
    public record Open(String via) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("vr_t_kleding_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Open::via, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> every client: how this player looks (see {@link Kledingkast#uiterlijk}). */
    public record Uiterlijk(UUID speler, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Uiterlijk> TYPE = new Type<>(Guhs.id("vr_t_kleding_uiterlijk"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Uiterlijk> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Uiterlijk::speler,
                ByteBufCodecs.COMPOUND_TAG, Uiterlijk::data, Uiterlijk::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> own client: may you glide (unlocked), may you here (place rules), the trick of your wings ("" = no wings),
     * and are you in a boss arena (effects are muted there).
     */
    public record Staat(boolean zweven, boolean hier, String kracht, boolean arena) implements CustomPacketPayload {
        public static final Type<Staat> TYPE = new Type<>(Guhs.id("vr_t_kleding_staat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Staat> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Staat::zweven,
                ByteBufCodecs.BOOL, Staat::hier, ByteBufCodecs.STRING_UTF8, Staat::kracht, ByteBufCodecs.BOOL, Staat::arena, Staat::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: 1 = gliding, 0 = stopped, 2 = used the wing trick. */
    public record Zweef(int staat) implements CustomPacketPayload {
        public static final Type<Zweef> TYPE = new Type<>(Guhs.id("vr_t_kleding_zweef"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Zweef> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Zweef::staat, Zweef::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> nearby clients: show this cheer-up effect at this spot (the client applies the effects slider). */
    public record Plof(UUID speler, String effect, double x, double y, double z) implements CustomPacketPayload {
        public static final Type<Plof> TYPE = new Type<>(Guhs.id("vr_t_kleding_plof"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Plof> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Plof::speler,
                ByteBufCodecs.STRING_UTF8, Plof::effect, ByteBufCodecs.DOUBLE, Plof::x, ByteBufCodecs.DOUBLE, Plof::y, ByteBufCodecs.DOUBLE, Plof::z,
                Plof::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Scherm> schermOntvanger = s -> {
    };
    public static Consumer<Uiterlijk> uiterlijkOntvanger = s -> {
    };
    public static Consumer<Staat> staatOntvanger = s -> {
    };
    public static Consumer<Plof> plofOntvanger = s -> {
    };

    private KledingPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> schermOntvanger.accept(p));
        r.playToClient(Uiterlijk.TYPE, Uiterlijk.STREAM_CODEC, (p, c) -> uiterlijkOntvanger.accept(p));
        r.playToClient(Staat.TYPE, Staat.STREAM_CODEC, (p, c) -> staatOntvanger.accept(p));
        r.playToClient(Plof.TYPE, Plof.STREAM_CODEC, (p, c) -> plofOntvanger.accept(p));
        r.playToServer(Actie.TYPE, Actie.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                sp.level().getServer().execute(() -> Kledingkast.actie(sp, p.actie(), p.a(), p.b()));
            }
        });
        r.playToServer(Open.TYPE, Open.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                sp.level().getServer().execute(() -> Kledingkast.open(sp, null));
            }
        });
        r.playToServer(Zweef.TYPE, Zweef.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                sp.level().getServer().execute(() -> Zweven.ontvang(sp, p.staat()));
            }
        });
    }

    static void stuur(ServerPlayer p, CustomPacketPayload s) {
        if (p.connection == null) {
            return;
        }
        try {
            PacketDistributor.sendToPlayer(p, s);
        } catch (RuntimeException e) {
            // (a gametest mock player without a real channel)
        }
    }
}
