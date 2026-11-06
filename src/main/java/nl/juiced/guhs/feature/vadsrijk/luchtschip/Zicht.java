package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-LUCHTSCHIP (W-3 "Zicht"): an own view distance for the Vadsrijk dimension only, set in code on its ServerLevel's ChunkMap
 * ({@code ServerChunkCache#setViewDistance}); server.properties (and every other dimension) stay as they are.
 * <ul>
 *   <li>config: {@code config/guhs-vadsrijk-zicht.properties}, key {@code zichtafstand} (chunks, 2..32; default {@link #STANDAARD};
 *       0 = off, the dimension uses the server's normal view distance); live with {@code /guhs vadsrijk luchtschip zicht <n>};</li>
 *   <li>never lower than the server's own distance;</li>
 *   <li>the client is told the radius when it enters the dimension (and the server's normal radius again when it leaves), so it
 *       keeps the extra chunks; the player's own render distance still caps it (as always);</li>
 *   <li>re-applied every second: vanilla resets every level when the server's distance changes (singleplayer does that when you
 *       change your render distance).</li>
 * </ul>
 */
public final class Zicht {
    public static final int STANDAARD = 20;
    static final String BESTAND = "guhs-vadsrijk-zicht.properties";
    private static int ingesteld = STANDAARD;
    private static int laatsteServer = -1;
    private static boolean gelezen;

    private Zicht() {
    }

    /** The configured value (0 = off). */
    public static int ingesteld() {
        if (!gelezen) {
            lees();
        }
        return ingesteld;
    }

    /** The view distance the Vadsrijk level really uses. */
    public static int effectief(MinecraftServer server) {
        int basis = server.getPlayerList().getViewDistance();
        int z = ingesteld();
        return z <= 0 ? basis : Math.max(basis, Math.min(32, z));
    }

    static void lees() {
        gelezen = true;
        Path p = FMLPaths.CONFIGDIR.get().resolve(BESTAND);
        if (!Files.exists(p)) {
            schrijf();
            return;
        }
        Properties pr = new Properties();
        try (Reader r = Files.newBufferedReader(p)) {
            pr.load(r);
            ingesteld = Math.max(0, Math.min(32, Integer.parseInt(pr.getProperty("zichtafstand", "" + STANDAARD).trim())));
        } catch (IOException | NumberFormatException e) {
            ingesteld = STANDAARD;
        }
    }

    static void schrijf() {
        Properties pr = new Properties();
        pr.setProperty("zichtafstand", "" + ingesteld);
        Path p = FMLPaths.CONFIGDIR.get().resolve(BESTAND);
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                pr.store(w, "Guhs 1.10 Het Vadsrijk: view distance (chunks, 2..32) of the Vadsrijk dimension only; 0 = the server's normal one."
                        + " Live: /guhs vadsrijk luchtschip zicht <n>");
            }
        } catch (IOException ignored) {
            // read-only config dir: keep the value in memory
        }
    }

    /** (dev command) set + save, apply at once. */
    static void zet(MinecraftServer server, int n) {
        gelezen = true;
        ingesteld = Math.max(0, Math.min(32, n));
        schrijf();
        laatsteServer = -1;
        pas(server);
    }

    /** Every second: (re)apply to the Vadsrijk level and tell its players when the radius changed. */
    static void pas(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        int z = effectief(server);
        int basis = server.getPlayerList().getViewDistance();
        sl.getChunkSource().setViewDistance(z);   // (no-op when unchanged)
        int sleutel = z * 100 + basis;
        if (sleutel != laatsteServer) {
            laatsteServer = sleutel;
            for (ServerPlayer p : sl.players()) {
                p.connection.send(new ClientboundSetChunkCacheRadiusPacket(z));
            }
        }
    }

    /** A player entered a dimension: the radius of that dimension. */
    static void binnen(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        int r = Vadsrijk.in(p) ? effectief(server) : server.getPlayerList().getViewDistance();
        p.connection.send(new ClientboundSetChunkCacheRadiusPacket(r));
    }
}
