package nl.juiced.guhs.feature.snuffel;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.UUIDUtil;
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
 * The messages of Het Snuffeleiland.
 * <ul>
 *   <li>to the client: {@code guhs:snuffel_vorm} (this player is / is no longer a dog, and which dog), {@code
 *   guhs:snuffel_houding} (what the dog holds: sniffing, sitting, wagging), {@code guhs:snuffel_gebaar} (a bark, a dig),
 *   {@code guhs:snuffel_stand} (your own rank, scents, deeds, tree, companion), {@code guhs:snuffel_meter} (what your nose
 *   smells now), {@code guhs:snuffel_open} (a screen: the choice, the pause menu, the Guhstation);</li>
 *   <li>to the server: {@code guhs:snuffel_actie} (the poses you hold), {@code guhs:snuffel_doe} (bark, dig),
 *   {@code guhs:snuffel_kies} (the answer of the choice screen), {@code guhs:snuffel_knop} (a button of a screen).</li>
 * </ul>
 * The client handlers are set by client.SnuffelClient.
 */
public final class SnuffelPayloads {
    public static volatile Consumer<Vorm> vormOntvanger = p -> {
    };
    public static volatile Consumer<Houding> houdingOntvanger = p -> {
    };
    public static volatile Consumer<Gebaar> gebaarOntvanger = p -> {
    };
    public static volatile Consumer<StandBericht> standOntvanger = p -> {
    };
    public static volatile Consumer<Meter> meterOntvanger = p -> {
    };
    public static volatile Consumer<Open> openOntvanger = p -> {
    };

    private SnuffelPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Vorm.TYPE, Vorm.STREAM_CODEC, Vorm::handle);
        r.playToClient(Houding.TYPE, Houding.STREAM_CODEC, Houding::handle);
        r.playToClient(Gebaar.TYPE, Gebaar.STREAM_CODEC, Gebaar::handle);
        r.playToClient(StandBericht.TYPE, StandBericht.STREAM_CODEC, StandBericht::handle);
        r.playToClient(Meter.TYPE, Meter.STREAM_CODEC, Meter::handle);
        r.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        r.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
        r.playToServer(Kies.TYPE, Kies.STREAM_CODEC, Kies::handle);
        r.playToServer(Knop.TYPE, Knop.STREAM_CODEC, Knop::handle);
    }

    /** This player is a dog now (which breed, coat and dog name), or no dog any more. */
    public record Vorm(UUID speler, boolean actief, String ras, String kleur, String naam) implements CustomPacketPayload {
        public static final Type<Vorm> TYPE = new Type<>(Guhs.id("snuffel_vorm"));
        public static final StreamCodec<FriendlyByteBuf, Vorm> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Vorm::speler, ByteBufCodecs.BOOL, Vorm::actief, ByteBufCodecs.STRING_UTF8, Vorm::ras,
                ByteBufCodecs.STRING_UTF8, Vorm::kleur, ByteBufCodecs.STRING_UTF8, Vorm::naam, Vorm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Vorm p, IPayloadContext context) {
            context.enqueueWork(() -> vormOntvanger.accept(p));
        }
    }

    /** The poses a dog holds: bits {@link Hondvorm#SNUFFELT}, {@link Hondvorm#ZIT}, {@link Hondvorm#KWISPELT}. */
    public record Houding(UUID speler, int vlaggen) implements CustomPacketPayload {
        public static final Type<Houding> TYPE = new Type<>(Guhs.id("snuffel_houding"));
        public static final StreamCodec<FriendlyByteBuf, Houding> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Houding::speler, ByteBufCodecs.VAR_INT, Houding::vlaggen, Houding::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Houding p, IPayloadContext context) {
            context.enqueueWork(() -> houdingOntvanger.accept(p));
        }
    }

    /** A dog does something short: {@link Hondvorm#BLAF} or {@link Hondvorm#GRAAF} (ticks = how long). */
    public record Gebaar(UUID speler, int wat, int ticks) implements CustomPacketPayload {
        public static final Type<Gebaar> TYPE = new Type<>(Guhs.id("snuffel_gebaar"));
        public static final StreamCodec<FriendlyByteBuf, Gebaar> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Gebaar::speler, ByteBufCodecs.VAR_INT, Gebaar::wat, ByteBufCodecs.VAR_INT, Gebaar::ticks, Gebaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Gebaar p, IPayloadContext context) {
            context.enqueueWork(() -> gebaarOntvanger.accept(p));
        }
    }

    /** Your own island state (see {@link Stand}). */
    public record StandBericht(CompoundTag data) implements CustomPacketPayload {
        public static final Type<StandBericht> TYPE = new Type<>(Guhs.id("snuffel_stand"));
        public static final StreamCodec<FriendlyByteBuf, StandBericht> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, StandBericht::data, StandBericht::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(StandBericht p, IPayloadContext context) {
            context.enqueueWork(() -> standOntvanger.accept(p));
        }
    }

    /**
     * What your nose smells while you sniff: the kind of scent ({@link GeurSoort} ordinal, -1 = nothing), how strong (0..1),
     * whether you stand on the spot, and whether it has to be dug up there.
     */
    public record Meter(int soort, float sterkte, boolean plek, boolean graven) implements CustomPacketPayload {
        public static final Type<Meter> TYPE = new Type<>(Guhs.id("snuffel_meter"));
        public static final StreamCodec<FriendlyByteBuf, Meter> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Meter::soort, ByteBufCodecs.FLOAT, Meter::sterkte, ByteBufCodecs.BOOL, Meter::plek,
                ByteBufCodecs.BOOL, Meter::graven, Meter::new);
        public static final Meter NIETS = new Meter(-1, 0f, false, false);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Meter p, IPayloadContext context) {
            context.enqueueWork(() -> meterOntvanger.accept(p));
        }
    }

    /** Opens a screen: {@link #KEUZE} (data: the choice so far), {@link #PAUZE} (the memory card), {@link #GUHSTATION}. */
    public record Open(int scherm, CompoundTag data) implements CustomPacketPayload {
        public static final int KEUZE = 0, PAUZE = 1, GUHSTATION = 2, BOEKJE = 3;
        public static final Type<Open> TYPE = new Type<>(Guhs.id("snuffel_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::scherm, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            context.enqueueWork(() -> openOntvanger.accept(p));
        }
    }

    /** The poses the player's keys hold now. */
    public record Actie(int vlaggen) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("snuffel_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Actie::vlaggen, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> Hondvorm.zetHouding(sp, p.vlaggen()));
            }
        }
    }

    /** A bark or a dig. */
    public record Doe(int wat) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("snuffel_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Doe::wat, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> {
                    if (p.wat() == Hondvorm.BLAF) {
                        Hondvorm.blaf(sp);
                    } else if (p.wat() == Hondvorm.GRAAF) {
                        Snuffelen.graaf(sp);
                    }
                });
            }
        }
    }

    /** The answer of the choice screen. */
    public record Kies(String ras, String kleur, String naam, String maatje) implements CustomPacketPayload {
        public static final Type<Kies> TYPE = new Type<>(Guhs.id("snuffel_kies"));
        public static final StreamCodec<FriendlyByteBuf, Kies> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Kies::ras, ByteBufCodecs.stringUtf8(32), Kies::kleur, ByteBufCodecs.stringUtf8(64), Kies::naam,
                ByteBufCodecs.stringUtf8(8), Kies::maatje, Kies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kies p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> Keuze.ontvang(sp, new Keuze(p.ras(), p.kleur(), p.naam(), p.maatje())));
            }
        }
    }

    /** A button: {@link #NAAR_HUIS} (the memory card's "Opslaan en naar huis"), {@link #START} (the Guhstation). */
    public record Knop(int wat) implements CustomPacketPayload {
        public static final int NAAR_HUIS = 1, START = 2;
        public static final Type<Knop> TYPE = new Type<>(Guhs.id("snuffel_knop"));
        public static final StreamCodec<FriendlyByteBuf, Knop> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Knop::wat, Knop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Knop p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> {
                    if (p.wat() == NAAR_HUIS) {
                        GeheugenkaartItem.naarHuis(sp);
                    } else if (p.wat() == START) {
                        GuhstationBlock.start(sp);
                    }
                });
            }
        }
    }
}
