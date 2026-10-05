package nl.juiced.guhs.feature.guhpixel.among;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Sessies;

/**
 * Among Guhs over the network: two payloads.
 * <ul>
 *   <li>{@code guhs:among_scherm} (to the client): {@code soort} + one tag. Kinds: {@link #HUD} (role, tasks, bar, sabotage,
 *   cooldowns; an empty tag clears it), {@link #WACHTRIJ} (the queue at the Kapitein-guh), {@link #VERGADERING} (the whole
 *   meeting: participants, statements, votes, result), {@link #TAAK} (open a task panel), {@link #KAART} (the ship map: sabotage
 *   or vents).</li>
 *   <li>{@code guhs:among_actie} (to the server): what the player did: {@code soort} + two numbers + a free tag (the result of a
 *   task panel). The server checks everything again ({@link AmongSessie}, {@link AmongWachtrij}).</li>
 * </ul>
 */
public final class AmongPayloads {
    public static final String HUD = "hud", WACHTRIJ = "wachtrij", VERGADERING = "vergadering", TAAK = "taak", KAART = "kaart";
    public static final int STEM = 1, ZEG = 2, TAAK_KLAAR = 3, TAAK_STOP = 4, SABOTEER = 5, LUIK = 6, WACHTRIJ_KLAAR = 7, WACHTRIJ_NIVEAU = 8, WACHTRIJ_WEG = 9;

    public static volatile Consumer<Scherm> ontvanger = p -> { };

    public record Scherm(String soort, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("among_scherm"));
        public static final StreamCodec<FriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32), Scherm::soort,
                ByteBufCodecs.COMPOUND_TAG, Scherm::data, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Scherm p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }

    public record Actie(int soort, int a, int b, CompoundTag extra) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("among_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Actie::soort,
                ByteBufCodecs.INT, Actie::a, ByteBufCodecs.INT, Actie::b, ByteBufCodecs.COMPOUND_TAG, Actie::extra, Actie::new);

        public Actie(int soort, int a, int b) {
            this(soort, a, b, new CompoundTag());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                opActie(sp, p);
            }
        }
    }

    /** What a client asked for; everything is checked again where it is handled. */
    static void opActie(ServerPlayer p, Actie a) {
        if (a.soort() == WACHTRIJ_KLAAR || a.soort() == WACHTRIJ_NIVEAU || a.soort() == WACHTRIJ_WEG) {
            AmongWachtrij.actie(p, a.soort());
            return;
        }
        if (!(Sessies.van(p) instanceof AmongSessie s)) {
            return;
        }
        switch (a.soort()) {
            case STEM -> s.stem(p, a.a());
            case ZEG -> s.zeg(p, a.a(), a.b(), a.extra().getIntOr("Zone", -1));
            case TAAK_KLAAR -> s.taakKlaar(p, a.a(), a.extra());
            case TAAK_STOP -> s.taakStop(p);
            case SABOTEER -> s.saboteer(p, a.a(), a.b());
            case LUIK -> s.luik(p, a.a(), a.b());
            default -> {
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, Scherm::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
    }

    private AmongPayloads() {
    }
}
