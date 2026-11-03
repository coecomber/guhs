package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The economie slice's network: ONE screen payload to the client (which screen, its data, the item stacks it shows) and ONE action
 * payload back. The server is the boss: it remembers which economie screen each player opened through the API
 * ({@link #open}); an action for a screen the player did not open (a cheat client) is ignored.
 * Screens: {@link #WINKEL}, {@link #SMID}, {@link #MARKT}, {@link #HANDEL}, {@link #STAL}.
 */
public final class EcoPayloads {
    public static final String WINKEL = "winkel", SMID = "smid", MARKT = "markt", HANDEL = "handel", STAL = "stal", DICHT = "dicht";

    /** Server -> client: open (or refresh) a screen. {@code scherm} = {@link #DICHT} closes it. */
    public record Scherm(String scherm, CompoundTag data, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_economie_scherm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Scherm::scherm, ByteBufCodecs.COMPOUND_TAG, Scherm::data,
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Scherm::items, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: an action on an open screen. */
    public record Doe(String scherm, String actie, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("vr_economie_doe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Doe::scherm, ByteBufCodecs.STRING_UTF8, Doe::actie, ByteBufCodecs.COMPOUND_TAG, Doe::args, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: the wallet (for the coin pop-up on the HUD). */
    public record Munten(long brons) implements CustomPacketPayload {
        public static final Type<Munten> TYPE = new Type<>(Guhs.id("vr_economie_munten"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Munten> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Munten::brons,
                Munten::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by EconomieClient (client only). */
    public static Consumer<Scherm> schermOntvanger = s -> {
    };
    public static Consumer<Munten> muntenOntvanger = s -> {
    };

    /** Server: which economie screen each player has open (and its context, e.g. the winkel id). */
    private static final Map<UUID, Open> OPEN = new ConcurrentHashMap<>();

    public record Open(String scherm, String context) {
    }

    private EcoPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> schermOntvanger.accept(p));
        r.playToClient(Munten.TYPE, Munten.STREAM_CODEC, (p, c) -> muntenOntvanger.accept(p));
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                opDoe(sp, p);
            }
        });
    }

    /** Server: opens (or refreshes) a screen and remembers it. */
    public static void open(ServerPlayer p, String scherm, String context, CompoundTag data, List<ItemStack> items) {
        OPEN.put(p.getUUID(), new Open(scherm, context));
        PacketDistributor.sendToPlayer(p, new Scherm(scherm, data, items));
    }

    /** Server: closes the economie screen of this player (if any). */
    public static void sluit(ServerPlayer p) {
        if (OPEN.remove(p.getUUID()) != null) {
            PacketDistributor.sendToPlayer(p, new Scherm(DICHT, new CompoundTag(), List.of()));
        }
    }

    public static Open open(ServerPlayer p) {
        return OPEN.get(p.getUUID());
    }

    static void vergeet(UUID id) {
        OPEN.remove(id);
    }

    private static void opDoe(ServerPlayer p, Doe d) {
        if (d.actie().equals("dicht")) {
            Open nu = OPEN.get(p.getUUID());
            if (nu != null && nu.scherm().equals(d.scherm())) {
                OPEN.remove(p.getUUID());
            }
            if (d.scherm().equals(HANDEL)) {
                Handel.annuleer(p, "vadsrijk.economie.handel.geannuleerd");
            }
            return;
        }
        Open o = OPEN.get(p.getUUID());
        if (o == null || !o.scherm().equals(d.scherm())) {
            return;   // (not opened through the API: ignored)
        }
        switch (d.scherm()) {
            case WINKEL -> Winkels.doe(p, o.context(), d.actie(), d.args());
            case SMID -> Smid.doe(p, d.actie(), d.args());
            case MARKT -> Markt.doe(p, d.actie(), d.args());
            case HANDEL -> Handel.doe(p, d.actie(), d.args());
            case STAL -> Stal.doe(p, d.actie(), d.args());
            default -> {
            }
        }
    }

    /** Server: send the wallet value. */
    static void stuurMunten(ServerPlayer p, long brons) {
        PacketDistributor.sendToPlayer(p, new Munten(brons));
    }
}
