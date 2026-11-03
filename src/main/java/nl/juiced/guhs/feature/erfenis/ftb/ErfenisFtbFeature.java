package nl.juiced.guhs.feature.erfenis.ftb;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.erfenis.ErfenisMomenten;

/**
 * 1.9 "Een opknapper!": the FTB Quests chapter guhs_erfenis (tools/features/erfenis_ftb.py) needs the village to tick its
 * quests off. {@link ErfenisFtb} looks at the village state (every second for online players, and at once on every
 * {@link ErfenisMomenten} moment) and grants the hidden advancements guhs:quest/erfenis_* and the custom statistics
 * guhs:erfenis_* (registered here). The client tells the server when the overview or the world map opens
 * ({@link Geopend}, sent by client/ErfenisFtbClient).
 */
public final class ErfenisFtbFeature {
    public static final DeferredRegister<Identifier> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, Guhs.MODID);
    /** stat name -> its registered id (same names as STATS in erfenis_ftb.py). */
    public static final Map<String, DeferredHolder<Identifier, Identifier>> STAT = new LinkedHashMap<>();

    static {
        for (String naam : ErfenisFtb.STATS) {
            STAT.put(naam, STATS.register(naam, () -> Guhs.id(naam)));
        }
    }

    private ErfenisFtbFeature() {
    }

    public static void register(IEventBus modBus) {
        STATS.register(modBus);
        ErfenisMomenten.luister((p, moment) -> ErfenisFtb.kijk(p));
        NeoForge.EVENT_BUS.addListener(ErfenisFtb::tick);
        NeoForge.EVENT_BUS.addListener(ErfenisFtb::opLogin);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToServer(Geopend.TYPE, Geopend.STREAM_CODEC, ErfenisFtbFeature::opGeopend);
    }

    /** Client -> server: a screen of the village opened ("overzicht" or "kaart"). */
    public record Geopend(String wat) implements CustomPacketPayload {
        public static final Type<Geopend> TYPE = new Type<>(Guhs.id("erfenis_ftb_geopend"));
        public static final StreamCodec<FriendlyByteBuf, Geopend> STREAM_CODEC = StreamCodec.ofMember(Geopend::write, Geopend::new);

        private Geopend(FriendlyByteBuf buf) {
            this(buf.readUtf(16));
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(wat, 16);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void opGeopend(Geopend g, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer p) {
            ErfenisFtb.geopend(p, g.wat());
        }
    }
}
