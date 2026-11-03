package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The klassen network ({@code guhs:vr_klassen_*}):
 * <ul>
 *   <li>Schiet (C2S): the left button is down with a ranged/magic weapon;</li>
 *   <li>Vraag (C2S): open the tree screen (key K);</li>
 *   <li>Actie (C2S): koop/respec/wissel/kies/adv + argument;</li>
 *   <li>Staat (S2C): everything the screens show (a CompoundTag, see {@link KlasseBalie#staat}); modus "boom", "balie" or "kies".</li>
 * </ul>
 */
public final class KlassenPayloads {
    /** Client hook (set by KlassenClient). */
    public static Consumer<Staat> staatOntvanger = s -> {
    };

    public record Schiet() implements CustomPacketPayload {
        public static final Type<Schiet> TYPE = new Type<>(Guhs.id("vr_klassen_schiet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Schiet> STREAM_CODEC = StreamCodec.unit(new Schiet());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Vraag() implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("vr_klassen_vraag"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.unit(new Vraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Actie(String soort, String arg) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("vr_klassen_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Actie::soort,
                ByteBufCodecs.STRING_UTF8, Actie::arg, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Staat(CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Staat> TYPE = new Type<>(Guhs.id("vr_klassen_staat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Staat> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Staat::tag,
                Staat::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private KlassenPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToServer(Schiet.TYPE, Schiet.STREAM_CODEC, (s, c) -> {
            if (c.player() instanceof ServerPlayer p) {
                Basisaanval.verzoek(p);
            }
        });
        r.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, (s, c) -> {
            if (c.player() instanceof ServerPlayer p) {
                KlasseBalie.boom(p);
            }
        });
        r.playToServer(Actie.TYPE, Actie.STREAM_CODEC, KlassenPayloads::actie);
        r.playToClient(Staat.TYPE, Staat.STREAM_CODEC, (s, c) -> staatOntvanger.accept(s));
    }

    private static void actie(Actie a, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer p) {
            KlasseBalie.actie(p, a.soort(), a.arg());
        }
    }
}
