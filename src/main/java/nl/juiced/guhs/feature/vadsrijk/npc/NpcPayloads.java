package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/** The npc slice's network: the quest markers above the NPCs, the questlog (J). */
public final class NpcPayloads {
    /** Client hooks (set by NpcClient). */
    public static Consumer<Markers> markerOntvanger = m -> {
    };
    public static Consumer<Log> logOntvanger = m -> {
    };
    public static Consumer<Volg> volgOntvanger = m -> {
    };
    public static Consumer<KamerTip> kamerOntvanger = m -> {
    };
    public static Consumer<Reiskaart> reiskaartOntvanger = m -> {
    };

    /** One place on the travel map: id, state (Reiskaart.OPEN/HIER/ONBEKEND/TEASER), the reason key, its argument. */
    public record ReisPlek(String id, int staat, String reden, int arg) {
        static final StreamCodec<RegistryFriendlyByteBuf, ReisPlek> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ReisPlek::id,
                ByteBufCodecs.VAR_INT, ReisPlek::staat, ByteBufCodecs.STRING_UTF8, ReisPlek::reden, ByteBufCodecs.VAR_INT, ReisPlek::arg, ReisPlek::new);
    }

    /** Server -> client: open the airship's travel map (where you are + every place). */
    public record Reiskaart(String hier, List<ReisPlek> plekken) implements CustomPacketPayload {
        public static final Type<Reiskaart> TYPE = new Type<>(Guhs.id("vr_npc_reiskaart"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Reiskaart> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8,
                Reiskaart::hier, ReisPlek.STREAM_CODEC.apply(ByteBufCodecs.list(16)), Reiskaart::plekken, Reiskaart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: a place was clicked on the travel map. */
    public record ReisKies(String id) implements CustomPacketPayload {
        public static final Type<ReisKies> TYPE = new Type<>(Guhs.id("vr_npc_reiskies"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReisKies> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ReisKies::id,
                ReisKies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The puzzle room you stand in: its tip key ("" = none), shown at the bottom of the screen while you are inside (KamerTips). */
    public record KamerTip(String tekstKey) implements CustomPacketPayload {
        public static final Type<KamerTip> TYPE = new Type<>(Guhs.id("vr_npc_kamertip"));
        public static final StreamCodec<RegistryFriendlyByteBuf, KamerTip> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8,
                KamerTip::tekstKey, KamerTip::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private NpcPayloads() {
    }

    /** Which NPC entities show which marker for you: 1 = "!" (side), 2 = "!" (main), 3 = "?" (hand in), 4 = "?" (talk to). */
    public record Markers(int[] ids, byte[] soort) implements CustomPacketPayload {
        public static final Type<Markers> TYPE = new Type<>(Guhs.id("vr_npc_markers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Markers> STREAM_CODEC = StreamCodec.ofMember((m, b) -> {
            b.writeVarIntArray(m.ids);
            b.writeByteArray(m.soort);
        }, b -> new Markers(b.readVarIntArray(), b.readByteArray()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The questlog's contents (built by {@link NpcLog}); open = open the screen (else just refresh it when open). */
    public record Log(CompoundTag data, boolean open) implements CustomPacketPayload {
        public static final Type<Log> TYPE = new Type<>(Guhs.id("vr_npc_log"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Log> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Log::data,
                ByteBufCodecs.BOOL, Log::open, Log::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** One line of the quest tracker: the quest, its step (what / where / an extra line) and the spot to point at (or none). */
    public record VolgRegel(String quest, boolean hoofd, Component titel, Component wat, Component waar, Optional<Component> extra,
            Optional<BlockPos> doel) {
        static final StreamCodec<RegistryFriendlyByteBuf, VolgRegel> STREAM_CODEC = StreamCodec.ofMember((r, b) -> {
            b.writeUtf(r.quest);
            b.writeBoolean(r.hoofd);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, r.titel);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, r.wat);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, r.waar);
            b.writeBoolean(r.extra.isPresent());
            r.extra.ifPresent(c -> ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, c));
            b.writeBoolean(r.doel.isPresent());
            r.doel.ifPresent(b::writeBlockPos);
        }, b -> new VolgRegel(b.readUtf(), b.readBoolean(), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b),
                ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b),
                b.readBoolean() ? Optional.of(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b)) : Optional.empty(),
                b.readBoolean() ? Optional.of(b.readBlockPos()) : Optional.empty()));
    }

    /** The quest tracker (top right) and the light beam: on/off + the main story's step + the pinned quest's step. */
    public record Volg(boolean aan, boolean straal, List<VolgRegel> regels) implements CustomPacketPayload {
        public static final Type<Volg> TYPE = new Type<>(Guhs.id("vr_npc_volg"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Volg> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Volg::aan,
                ByteBufCodecs.BOOL, Volg::straal, VolgRegel.STREAM_CODEC.apply(ByteBufCodecs.list(4)), Volg::regels, Volg::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: tracker choices. soort: PIN (quest = id, "" = unpin), TRACKER / STRAAL (toggle). */
    public record VolgKeuze(int soort, String quest) implements CustomPacketPayload {
        public static final int PIN = 0, TRACKER = 1, STRAAL = 2;
        public static final Type<VolgKeuze> TYPE = new Type<>(Guhs.id("vr_npc_volgkeuze"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VolgKeuze> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, VolgKeuze::soort,
                ByteBufCodecs.STRING_UTF8, VolgKeuze::quest, VolgKeuze::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: J was pressed (in the dimension). */
    public record LogVraag() implements CustomPacketPayload {
        public static final Type<LogVraag> TYPE = new Type<>(Guhs.id("vr_npc_logvraag"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LogVraag> STREAM_CODEC = StreamCodec.unit(new LogVraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Markers.TYPE, Markers.STREAM_CODEC, (m, c) -> markerOntvanger.accept(m));
        r.playToClient(Log.TYPE, Log.STREAM_CODEC, (m, c) -> logOntvanger.accept(m));
        r.playToServer(LogVraag.TYPE, LogVraag.STREAM_CODEC, NpcPayloads::opVraag);
        r.playToClient(Volg.TYPE, Volg.STREAM_CODEC, (m, c) -> volgOntvanger.accept(m));
        r.playToClient(KamerTip.TYPE, KamerTip.STREAM_CODEC, (m, c) -> kamerOntvanger.accept(m));
        r.playToClient(Reiskaart.TYPE, Reiskaart.STREAM_CODEC, (m, c) -> reiskaartOntvanger.accept(m));
        r.playToServer(ReisKies.TYPE, ReisKies.STREAM_CODEC, (m, c) -> {
            if (c.player() instanceof ServerPlayer p) {
                nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart.kies(p, m.id());
            }
        });
        r.playToServer(VolgKeuze.TYPE, VolgKeuze.STREAM_CODEC, (m, c) -> {
            if (c.player() instanceof ServerPlayer p) {
                NpcVolg.keuze(p, m);
            }
        });
    }

    private static void opVraag(LogVraag v, IPayloadContext c) {
        if (c.player() instanceof ServerPlayer p && Vadsrijk.in(p)) {
            stuur(p, new Log(NpcLog.bouw(p), true));
        }
    }

    static void stuur(ServerPlayer p, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }
}
