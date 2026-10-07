package nl.juiced.guhs.feature.guhpixel;

import java.io.Reader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import nl.juiced.guhs.Guhs;
import org.slf4j.Logger;

/**
 * The lobby island: template {@code guhs:guhpixel/lobby}, stamped once with its min corner at {@link Guhpixel#LOBBY_MIN}
 * the first time the dimension is needed ({@link #zorg}).
 * <p>
 * The template has a version: the number in {@code data/guhs/guhpixel/lobby_versie.json} ({@code {"versie": n}}, written
 * by tools/features/guhpixel_lobby_bouw.py from its VERSIE). When it is higher than the stored one, the lobby box is
 * cleared and stamped again ({@link #herbouw}), so a later update reaches a live server. A rebuild first loads the box
 * for a few seconds (its old entities must be loaded to be removed), then clears and stamps.
 */
public final class Lobby {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier TEMPLATE = Guhs.id("guhpixel/lobby");
    private static final Identifier VERSIE_BESTAND = Guhs.id("guhpixel/lobby_versie.json");
    private static final String OPGESLAGEN = "LobbyVersie";
    private static final Vec3i MAAT = new Vec3i(Guhpixel.LOBBY_BREEDTE, Guhpixel.LOBBY_HOOGTE, Guhpixel.LOBBY_BREEDTE);
    private static final int WACHT = 60, STRAAL = 4;
    /** Ticks until a pending rebuild happens (-1: none). */
    private static int herbouwOver = -1;
    private static int versie = -1;

    /** The version of the template in this jar (at least 1). */
    public static int versie(MinecraftServer server) {
        if (versie < 0) {
            versie = 1;
            try {
                var res = server.getResourceManager().getResource(VERSIE_BESTAND);
                if (res.isPresent()) {
                    try (Reader r = res.get().openAsReader()) {
                        JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                        versie = Math.max(1, o.get("versie").getAsInt());
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Guhpixel: could not read {}", VERSIE_BESTAND, e);
            }
        }
        return versie;
    }

    /** The version the lobby in this world was stamped from (0: never). */
    public static int gebouwd(MinecraftServer server) {
        return PxData.algemeen(server).getIntOr(OPGESLAGEN, 0);
    }

    /** Makes sure the lobby stands (stamps it the very first time). False when the template is missing. */
    public static boolean zorg(ServerLevel level) {
        if (gebouwd(level.getServer()) > 0) {
            return true;
        }
        return stempel(level);
    }

    /** Server start: a newer template replaces the lobby. */
    static void opStart(MinecraftServer server) {
        versie = -1;
        herbouwOver = -1;
        ServerLevel level = Guhpixel.level(server);
        int oud = gebouwd(server);
        if (level != null && oud > 0 && oud < versie(server)) {
            LOGGER.info("Guhpixel: the lobby template is newer ({} -> {}), rebuilding the lobby", oud, versie(server));
            herbouw(level);
        }
    }

    /** Clears the lobby box and stamps the template again, a few seconds from now. */
    public static void herbouw(ServerLevel level) {
        if (herbouwOver < 0) {
            level.getChunkSource().addTicketWithRadius(GuhpixelFeature.TICKET.get(), ChunkPos.containing(BlockPos.ZERO), STRAAL);
            herbouwOver = WACHT;
        }
    }

    public static boolean herbouwBezig() {
        return herbouwOver >= 0;
    }

    static void tick(MinecraftServer server) {
        if (herbouwOver < 0) {
            return;
        }
        ServerLevel level = Guhpixel.level(server);
        if (level == null) {
            herbouwOver = -1;
            return;
        }
        if (--herbouwOver < 0) {
            Stempel.ruim(level, Guhpixel.lobbyDoos());
            Stempel.leeg(level, Guhpixel.LOBBY_MIN, MAAT);
            stempel(level);
            level.getChunkSource().removeTicketWithRadius(GuhpixelFeature.TICKET.get(), ChunkPos.containing(BlockPos.ZERO), STRAAL);
            LobbyNpcs.meteen();
        }
    }

    private static boolean stempel(ServerLevel level) {
        if (!Stempel.plaats(level, TEMPLATE, Guhpixel.LOBBY_MIN)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        PxData.algemeen(server).putInt(OPGESLAGEN, versie(server));
        PxData.vuil(server);
        return true;
    }

    private Lobby() {
    }
}
