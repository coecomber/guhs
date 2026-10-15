package nl.juiced.guhs.feature.einde2.h3aarde;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The two messages of chapter 3, both about a game of Pai Sho ({@link PaiSho}): the server shows a board ({@link Stand}, the
 * client opens or refreshes its screen) and the player puts a tile down or asks for something ({@link Zet}).
 */
public final class H3Payloads {
    /** Stand.spel: the story game with Miroh, or a placed table. */
    public static final String MIROH = "miroh", TAFEL = "tafel";
    /** Stand.status. */
    public static final int BEZIG = 0, WACHT = 1, KLAAR = 2;
    /** Zet.cel besides a square: close the screen, ask for a new game (a table). */
    public static final int SLUIT = -1, NIEUW = -2;

    private H3Payloads() {
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
        registrar.playToServer(Zet.TYPE, Zet.STREAM_CODEC, Zet::handle);
    }

    /**
     * A board for the screen: spel + tafel (BlockPos.ZERO for Miroh's game) say which game; bord = {@link PaiSho} string;
     * naamA / naamB = a player's name, or "#" + a lang key; jij = 0 you only watch, 1 you are 'a', 2 you are 'b';
     * status = {@link #BEZIG} / {@link #WACHT} / {@link #KLAAR}; uitslag = a lang key ("" while it runs).
     */
    public record Stand(String spel, BlockPos tafel, String bord, String naamA, String naamB, int jij, int status, String uitslag)
            implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("einde2_h3_aarde_pai_sho"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            buf.writeUtf(s.spel());
            buf.writeBlockPos(s.tafel());
            buf.writeUtf(s.bord());
            buf.writeUtf(s.naamA());
            buf.writeUtf(s.naamB());
            buf.writeVarInt(s.jij());
            buf.writeVarInt(s.status());
            buf.writeUtf(s.uitslag());
        }, buf -> new Stand(buf.readUtf(), buf.readBlockPos(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand s, IPayloadContext context) {
            context.enqueueWork(() -> nl.juiced.guhs.feature.einde2.h3aarde.client.PaiShoScherm.toon(s));
        }
    }

    /** A tile on a square (0..24), or {@link #SLUIT} / {@link #NIEUW}. */
    public record Zet(String spel, BlockPos tafel, int cel) implements CustomPacketPayload {
        public static final Type<Zet> TYPE = new Type<>(Guhs.id("einde2_h3_aarde_pai_sho_zet"));
        public static final StreamCodec<FriendlyByteBuf, Zet> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Zet::spel,
                BlockPos.STREAM_CODEC, Zet::tafel, ByteBufCodecs.VAR_INT, Zet::cel, Zet::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Zet z, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer p) {
                    PaiShoSpel.zet(p, z.spel(), z.tafel(), z.cel());
                }
            });
        }
    }
}
