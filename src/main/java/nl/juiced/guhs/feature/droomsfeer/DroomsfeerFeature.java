package nl.juiced.guhs.feature.droomsfeer;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * 1.8.0 (Guhdromenland, afronding "droomsfeer"): the feel of the dream.
 * <ul>
 *   <li>the dream sky (client.DroomLucht: an endless purple-pink twilight, a big sleepy guh moon with a nightcap, stars and
 *       now and then a falling star; the dimension type's {@code "neoforge:custom_skybox": "guhs:guhdromenland"});</li>
 *   <li>the soft fade when you fall asleep, wake up or float to another island ({@link #fade}, drawn by
 *       client.DroomsfeerClient instead of the old moment of blindness and a title);</li>
 *   <li>the music of every island and the talking of the dream NPCs ({@link DroomGeluid}: the user's own recordings go in
 *       inspiration/Dromen audio, imported by tools/dromen_audio_import.py).</li>
 * </ul>
 * Texts and the moon texture: tools/features/droomsfeer.py.
 */
public final class DroomsfeerFeature {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** How many lines each kind of fade picks from (gui.guhs.droomsfeer.&lt;soort&gt;.&lt;n&gt;). */
    public static final int REGELS_INSLAPEN = 6, REGELS_WAKKER = 4;

    private DroomsfeerFeature() {
    }

    public static void register(IEventBus modBus) {
        DroomGeluid.values();                       // (the enum registers its sound events)
        SOUNDS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Fade.TYPE, Fade.STREAM_CODEC, Fade::handle);
    }

    /** The kinds of fade: into the dream, out of it, and a short one when you float to another island. */
    public enum Soort {
        INSLAPEN, WAKKER, ZWEEF
    }

    /**
     * Shows a player the soft fade (send it BEFORE teleporting: it starts fully covered and clears up slowly, so the
     * jump itself is never seen). {@code eiland}: the island's name key under it, or "" for none.
     */
    public static void fade(ServerPlayer p, Soort soort, String eiland) {
        String regel = switch (soort) {
            case INSLAPEN -> "gui.guhs.droomsfeer.inslapen." + p.getRandom().nextInt(REGELS_INSLAPEN);
            case WAKKER -> "gui.guhs.droomsfeer.wakker." + p.getRandom().nextInt(REGELS_WAKKER);
            case ZWEEF -> "";
        };
        String naam = eiland.isEmpty() ? "" : "gui.guhs.dromen.eiland." + eiland;
        ModNetworking.sendTo(p, new Fade(soort.ordinal(), regel, naam));
    }

    /** Server to client: start a fade (soort = Soort ordinal; regel and naam are lang keys, "" for none). */
    public record Fade(int soort, String regel, String naam) implements CustomPacketPayload {
        public static final Type<Fade> TYPE = new Type<>(Guhs.id("droom_fade"));
        public static final StreamCodec<FriendlyByteBuf, Fade> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Fade::soort, ByteBufCodecs.stringUtf8(96), Fade::regel, ByteBufCodecs.stringUtf8(96), Fade::naam,
                Fade::new);

        /** Set by the client side (DroomsfeerClient); nothing client-only in here. */
        public static Consumer<Fade> ontvanger = f -> {
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Fade p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }
}
