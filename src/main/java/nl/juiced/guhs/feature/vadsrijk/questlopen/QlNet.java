package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * U-QUESTLOPEN: the two payloads of the Questlopen screen. Client -> server {@link Actie}(wat, arg) for every button; server ->
 * client {@link Stand}(meta, texts): {@code meta.open} = open the screen, {@code meta.sluit} = close it (a scene/dialogue comes),
 * {@code meta.stappen} = the step list (each step's 4 texts are {@code texts[t..t+3]}: quest title, what, where, who), and the
 * state line. Every action is checked for OP on the server.
 */
public final class QlNet {
    public record Actie(String wat, int arg) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("vr_questlopen_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Actie::wat,
                ByteBufCodecs.VAR_INT, Actie::arg, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Stand(CompoundTag meta, List<Component> teksten) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("vr_questlopen_stand"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::meta,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list()), Stand::teksten, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Stand> ontvanger = s -> {
    };

    private QlNet() {
    }

    public static void payloads(PayloadRegistrar r) {
        r.playToServer(Actie.TYPE, Actie.STREAM_CODEC, (m, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                c.enqueueWork(() -> QuestLoper.actie(sp, m.wat(), m.arg()));
            }
        });
        r.playToClient(Stand.TYPE, Stand.STREAM_CODEC, (m, c) -> ontvanger.accept(m));
    }

    static void stuur(ServerPlayer p, Stand s) {
        if (!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && p.connection != null) {
            PacketDistributor.sendToPlayer(p, s);
        }
    }
}
