package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The grey dragon scars (design ronde 6: "het spoor van de draak, dat per speler terugkleurt na de zonequest").
 * <p>
 * The map has strips of {@code guhs:vr_w_litteken_gras} and {@code guhs:vr_w_litteken_plant} along the dragon's path. They are
 * ordinary blocks for everyone; only their <b>tint</b> is per player: the client draws them grey (withered flowers) until its
 * own flag says the zone is healed, then green grass with pink flowers. Nothing in the world changes.
 * <p>
 * <b>The flag:</b> {@code SpelerData.extra("npc")} boolean key {@value #SLEUTEL} (set by the npc slice when the zone-1 main quest
 * is done; {@link #zetKlaar} is a helper for that). The server checks it every 2 s for players in the dimension and sends
 * {@link Vlag} when it changes (and on entering); the client re-tints its chunks.
 */
public final class Littekens {
    public static final String SLEUTEL = "zone1_klaar";
    /** Client hook (set by WereldClient). */
    public static Consumer<Boolean> ontvanger = b -> {
    };
    private static final Map<UUID, Boolean> GESTUURD = new HashMap<>();

    public record Vlag(boolean klaar) implements CustomPacketPayload {
        public static final Type<Vlag> TYPE = new Type<>(Guhs.id("vr_wereld_littekens"));
        public static final StreamCodec<ByteBuf, Vlag> STREAM_CODEC = ByteBufCodecs.BOOL.map(Vlag::new, Vlag::klaar);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Littekens() {
    }

    public static boolean klaar(ServerPlayer p) {
        return Spelers.data(p).extra("npc").getBooleanOr(SLEUTEL, false);
    }

    /** Helper for the npc slice / dev command: heal (or grey again) the scars for this player. */
    public static void zetKlaar(ServerPlayer p, boolean klaar) {
        Spelers.data(p).extra("npc").putBoolean(SLEUTEL, klaar);
        Spelers.opgeslagen(p);
        controleer(p, true);
    }

    static void controleer(ServerPlayer p, boolean altijd) {
        if (!Vadsrijk.in(p)) {
            GESTUURD.remove(p.getUUID());
            return;
        }
        boolean k = klaar(p);
        Boolean oud = GESTUURD.get(p.getUUID());
        if (altijd || oud == null || oud != k) {
            GESTUURD.put(p.getUUID(), k);
            PacketDistributor.sendToPlayer(p, new Vlag(k));
        }
    }

    static void vergeet(UUID id) {
        GESTUURD.remove(id);
    }
}
