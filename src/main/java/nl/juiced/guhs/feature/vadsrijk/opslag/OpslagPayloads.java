package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.storage.BankContents;

/** T-OPSLAG: the network messages (client handlers are set by {@code opslag.client.OpslagClient}). */
public final class OpslagPayloads {
    /** Server -> client: what is in the materiaaltas (sent when it changed while its screen is open). */
    public record TasInhoud(int containerId, BankContents inhoud, boolean auto) implements CustomPacketPayload {
        public static final Type<TasInhoud> TYPE = new Type<>(Guhs.id("vr_t_opslag_tas"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TasInhoud> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TasInhoud::containerId, BankContents.STREAM_CODEC, TasInhoud::inhoud, ByteBufCodecs.BOOL, TasInhoud::auto,
                TasInhoud::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: an action in the materiaaltas screen ({@link MateriaalMenu#NEEM} ...). */
    public record TasActie(int containerId, int actie, ItemStack soort, int knop, boolean shift) implements CustomPacketPayload {
        public static final Type<TasActie> TYPE = new Type<>(Guhs.id("vr_t_opslag_tas_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TasActie> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TasActie::containerId, ByteBufCodecs.VAR_INT, TasActie::actie, ItemStack.OPTIONAL_STREAM_CODEC, TasActie::soort,
                ByteBufCodecs.VAR_INT, TasActie::knop, ByteBufCodecs.BOOL, TasActie::shift, TasActie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: open 0 = knapzak (key I), 1 = materiaaltas. */
    public record Open(int wat) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("vr_t_opslag_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Open::wat, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** One status on the player for the HUD row: kern Status ordinal, ticks left, strength. */
    public record StatusRegel(int status, int ticks, float sterkte) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StatusRegel> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StatusRegel::status, ByteBufCodecs.VAR_INT, StatusRegel::ticks, ByteBufCodecs.FLOAT, StatusRegel::sterkte,
                StatusRegel::new);
    }

    /**
     * Server -> client, every 5 ticks while something shows: the player's statuses, the last thing they hit (entity id, -1 =
     * none) and the statuses on that target.
     */
    public record HudInfo(List<StatusRegel> statussen, int doel, List<StatusRegel> doelStatussen) implements CustomPacketPayload {
        public static final Type<HudInfo> TYPE = new Type<>(Guhs.id("vr_t_opslag_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HudInfo> STREAM_CODEC = StreamCodec.composite(
                StatusRegel.STREAM_CODEC.apply(ByteBufCodecs.list()), HudInfo::statussen, ByteBufCodecs.VAR_INT, HudInfo::doel,
                StatusRegel.STREAM_CODEC.apply(ByteBufCodecs.list()), HudInfo::doelStatussen, HudInfo::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> client: a cast bar. {@code bron} = the casting entity's id (the player itself, or a boss); {@code ticks} = how
     * long (0 = stop now; {@code onderbroken} = it was interrupted); {@code kleur} = bar colour (RGB); {@code onderbreekbaar} =
     * draw it in the "interrupt me" colour frame.
     */
    public record Giet(int bron, String naamKey, int ticks, int kleur, int vlaggen) implements CustomPacketPayload {
        public static final int BAAS = 1, ONDERBREEKBAAR = 2, ONDERBROKEN = 4;
        public static final Type<Giet> TYPE = new Type<>(Guhs.id("vr_t_opslag_giet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Giet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Giet::bron, ByteBufCodecs.STRING_UTF8, Giet::naamKey, ByteBufCodecs.VAR_INT, Giet::ticks,
                ByteBufCodecs.INT, Giet::kleur, ByteBufCodecs.VAR_INT, Giet::vlaggen, Giet::new);

        public boolean baas() {
            return (vlaggen & BAAS) != 0;
        }

        public boolean onderbreekbaar() {
            return (vlaggen & ONDERBREEKBAAR) != 0;
        }

        public boolean onderbroken() {
            return (vlaggen & ONDERBROKEN) != 0;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static Consumer<TasInhoud> tasOntvanger = t -> {
    };
    public static Consumer<HudInfo> hudOntvanger = h -> {
    };
    public static Consumer<Giet> gietOntvanger = g -> {
    };

    private OpslagPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(TasInhoud.TYPE, TasInhoud.STREAM_CODEC, (p, c) -> tasOntvanger.accept(p));
        r.playToClient(HudInfo.TYPE, HudInfo.STREAM_CODEC, (p, c) -> hudOntvanger.accept(p));
        r.playToClient(Giet.TYPE, Giet.STREAM_CODEC, (p, c) -> gietOntvanger.accept(p));
        r.playToServer(TasActie.TYPE, TasActie.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp && sp.containerMenu instanceof MateriaalMenu m && m.containerId == p.containerId()) {
                m.actie(sp, p.actie(), p.soort(), p.knop(), p.shift());
            }
        });
        r.playToServer(Open.TYPE, Open.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                if (p.wat() == 1) {
                    MateriaalMenu.open(sp);
                } else {
                    Knapzak.open(sp);
                }
            }
        });
    }
}
