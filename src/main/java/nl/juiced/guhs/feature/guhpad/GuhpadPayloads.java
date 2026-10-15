package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;

/**
 * Het Guhpad over the network: {@code guhs:guhpad_stand} (to the client): where this player is on the path, for the
 * Guhdex tab Verhalen (the path map, the locks, the counter). Sent at login and whenever it changes
 * ({@link GuhpadEvents#kijk}).
 */
public final class GuhpadPayloads {
    /** A big story as the client shows it: its id, its world (ordinal of {@link Wereld}), its icon item, finished or not. */
    public record Verhaal(String id, int wereld, String icoon, boolean klaar) {
        static final StreamCodec<FriendlyByteBuf, Verhaal> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Verhaal::id, ByteBufCodecs.VAR_INT, Verhaal::wereld,
                ByteBufCodecs.stringUtf8(128), Verhaal::icoon, ByteBufCodecs.BOOL, Verhaal::klaar, Verhaal::new);

        public String naamSleutel() {
            return "gui.guhs.guhpad.verhaal." + id;
        }
    }

    /** One thing a world asks ({@link Guhpad.Eis}), with the world that asks it (ordinal of {@link Wereld}). */
    public record Eis(int wereld, String sleutel, String icoon, boolean voldaan) {
        static final StreamCodec<FriendlyByteBuf, Eis> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Eis::wereld, ByteBufCodecs.stringUtf8(128), Eis::sleutel,
                ByteBufCodecs.stringUtf8(128), Eis::icoon, ByteBufCodecs.BOOL, Eis::voldaan, Eis::new);
    }

    /** Where a player is on the Guhpad: every big story of this game and everything each world asks. */
    public record Stand(List<Verhaal> verhalen, List<Eis> eisen) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("guhpad_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(
                Verhaal.STREAM_CODEC.apply(ByteBufCodecs.list(128)), Stand::verhalen,
                Eis.STREAM_CODEC.apply(ByteBufCodecs.list(512)), Stand::eisen, Stand::new);
        /** Nothing known yet (the client before the first sync). */
        public static final Stand LEEG = new Stand(List.of(), List.of());

        public Stand {
            verhalen = List.copyOf(verhalen);
            eisen = List.copyOf(eisen);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand p, IPayloadContext context) {
            Client.zet(p);
        }

        /** The big stories of one world. */
        public List<Verhaal> verhalen(Wereld wereld) {
            return verhalen.stream().filter(v -> v.wereld() == wereld.ordinal()).toList();
        }

        /** Everything this world asks. */
        public List<Eis> eisen(Wereld wereld) {
            return eisen.stream().filter(e -> e.wereld() == wereld.ordinal()).toList();
        }

        /** What is still missing for this world. */
        public List<Eis> ontbreekt(Wereld wereld) {
            return eisen(wereld).stream().filter(e -> !e.voldaan()).toList();
        }

        /** May the player go into this world? */
        public boolean open(Wereld wereld) {
            return ontbreekt(wereld).isEmpty();
        }

        /** "Verhalen gevolgd: n van m": n. */
        public int gevolgd() {
            return (int) verhalen.stream().filter(Verhaal::klaar).count();
        }

        /** "Verhalen gevolgd: n van m": m. */
        public int totaal() {
            return verhalen.size();
        }

        /** Is this questline of the Guhdex (a part of) a big story of this game? */
        public boolean isGroot(String lijn) {
            GrootVerhaal v = GroteVerhalen.vanLijn(lijn);
            return v != null && verhalen.stream().anyMatch(x -> x.id().equals(v.id()));
        }
    }

    /** The client's copy (plain data; the Guhdex tab reads it). */
    public static final class Client {
        private static volatile Stand stand = Stand.LEEG;
        /** Called when a new stand arrived: the client init makes the open Guhdex redraw. */
        public static volatile Runnable vernieuwd = () -> {
        };

        public static Stand stand() {
            return stand;
        }

        public static void zet(Stand nieuw) {
            stand = nieuw;
            vernieuwd.run();
        }

        private Client() {
        }
    }

    /** This player's place on the Guhpad. */
    public static Stand stand(ServerPlayer p) {
        List<Verhaal> verhalen = new ArrayList<>();
        for (GrootVerhaal v : GroteVerhalen.alle()) {
            verhalen.add(new Verhaal(v.id(), v.wereld().ordinal(), v.icoon(), v.klaar(p)));
        }
        List<Eis> eisen = new ArrayList<>();
        for (Wereld w : Wereld.values()) {
            for (Guhpad.Eis e : Guhpad.eisen(p, w)) {
                eisen.add(new Eis(w.ordinal(), e.sleutel(), e.icoon(), e.voldaan()));
            }
        }
        return new Stand(verhalen, eisen);
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    private GuhpadPayloads() {
    }
}
