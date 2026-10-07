package nl.juiced.guhs.feature.einde2.bouwbikinibodem;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2, slice bouw_bikinibodem: the purple leegtemist inside the dome of Bikinibodem (the "sea water" of the town). The
 * server knows where the one Bikinibodem lies (feature/einde2/Plekken) and tells the client of a player when the player goes
 * in or out of the dome (the zone {@code koepel} of the sidecar: the half bubble that fits in that box); the client then
 * tints the fog purple and lets slow particles drift (client/E2BouwBikinibodemClient). The same place knows the
 * Kwallenvelden (the zones {@code kwallenveld_*}), for the swarms of Kruimelkwallen.
 * <p>
 * hook fase B: the cutscenes of the Sponsguh story ask {@link #inMist}.
 */
public final class Leegtemist {
    /** How often the server looks where a player is (ticks). */
    public static final int INTERVAL = 10;
    private static final Set<UUID> IN = ConcurrentHashMap.newKeySet();
    /** The answer on the client (set by {@link Mist}); the server never reads it. */
    private static volatile boolean hier;

    /** Server to client: you are (not) in the leegtemist now. */
    public record Mist(boolean in) implements CustomPacketPayload {
        public static final Type<Mist> TYPE = new Type<>(Guhs.id("einde2_bouw_bikinibodem_mist"));
        public static final StreamCodec<ByteBuf, Mist> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Mist::in, Mist::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Mist payload, IPayloadContext context) {
            hier = payload.in();
        }
    }

    /** Client: leaving a world or a server (the next one tells again when the player is in the mist there). */
    public static void wisClient() {
        hier = false;
    }

    /** Is this player inside the dome (the zone {@code koepel} of the one Bikinibodem)? Works on both sides. */
    public static boolean inMist(Player p) {
        return p.level().isClientSide() ? hier : IN.contains(p.getUUID());
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Leegtemist::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> IN.remove(event.getEntity().getUUID()));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % INTERVAL != 0) {
            return;
        }
        zet(player, inKoepel(player.level(), player.blockPosition()));
    }

    /** Records where the player is and tells their client when that changed (the game tests call it too). */
    public static void zet(ServerPlayer player, boolean in) {
        if (in == IN.contains(player.getUUID())) {
            return;
        }
        if (in) {
            IN.add(player.getUUID());
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_bouw_bikinibodem.mist"));
        } else {
            IN.remove(player.getUUID());
        }
        ModNetworking.sendTo(player, new Mist(in));
    }

    /** The spot in the coordinates of the build (null: not the Guheinde, or this world has no Bikinibodem). */
    @Nullable
    private static BlockPos lokaal(ServerLevel level, BlockPos pos) {
        BlockPos o = level.dimension() == GuheindeFeature.GUHEINDE ? Plekken.oorsprong(level, Einde2Ids.BIKINIBODEM) : null;
        return o == null ? null : pos.subtract(o);
    }

    /** Is this spot under the dome of the one Bikinibodem? */
    public static boolean inKoepel(ServerLevel level, BlockPos pos) {
        BlockPos l = lokaal(level, pos);
        return l != null && inKoepel(l);
    }

    /**
     * The same in the coordinates of the build: inside the half bubble that fits in the zone {@code koepel} (its foot is the
     * bottom of the box, its top the top).
     */
    public static boolean inKoepel(BlockPos lokaal) {
        Plekken.Plek zone = Plekken.plek(Einde2Ids.BIKINIBODEM, "koepel");
        if (zone == null || zone.tot() == null) {
            return false;
        }
        BlockPos a = zone.pos(), b = zone.tot();
        if (lokaal.getY() < a.getY() - 1 || lokaal.getY() > b.getY()) {
            return false;
        }
        double rx = (b.getX() - a.getX() + 1) / 2.0, rz = (b.getZ() - a.getZ() + 1) / 2.0, ry = b.getY() - a.getY() + 1;
        double dx = (lokaal.getX() + 0.5 - (a.getX() + rx)) / rx, dz = (lokaal.getZ() + 0.5 - (a.getZ() + rz)) / rz;
        double dy = Math.max(0, lokaal.getY() - a.getY()) / ry;
        return dx * dx + dy * dy + dz * dz <= 1.0;
    }

    /** Is this spot in one of the Kwallenvelden of the one Bikinibodem (the rule the Kruimelkwallen ask)? */
    public static boolean inKwallenveld(ServerLevel level, BlockPos pos) {
        BlockPos l = lokaal(level, pos);
        return l != null && inKwallenveld(l);
    }

    /** The same in the coordinates of the build: in one of the zones {@code kwallenveld_*} (or a little under it: the hills). */
    public static boolean inKwallenveld(BlockPos lokaal) {
        for (Map.Entry<String, Plekken.Plek> e : Plekken.van(Einde2Ids.BIKINIBODEM).entrySet()) {
            Plekken.Plek p = e.getValue();
            if (e.getKey().startsWith("kwallenveld_") && p.tot() != null && lokaal.getX() >= p.pos().getX() && lokaal.getX() <= p.tot().getX()
                    && lokaal.getY() >= p.pos().getY() - 1 && lokaal.getY() <= p.tot().getY() && lokaal.getZ() >= p.pos().getZ()
                    && lokaal.getZ() <= p.tot().getZ()) {
                return true;
            }
        }
        return false;
    }

    private Leegtemist() {
    }
}
