package nl.juiced.guhs.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.nbt.NbtIo;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ScreenEvent;
import nl.juiced.guhs.OfficialServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.0.1: puts the official Guhs server ("Guhs Server", guhs.nl) at the top of the multiplayer server list, once per
 * installation. Runs the first time the title screen opens; a marker file in the config folder remembers that it was
 * done, so a player who removes the server never gets it back. Skipped when the server is already in the list (under any
 * of its addresses) and when the client config option addOfficialServer is false. Client only (GuhsClient.init).
 */
public final class OfficialServerEntry {
    private static final Logger LOG = LoggerFactory.getLogger("guhs");
    static final String MARKER = "guhs-official-server-added.txt";
    private static boolean triedThisSession;

    private OfficialServerEntry() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (triedThisSession || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        triedThisSession = true;
        try {
            if (!GuhsClientConfig.SPEC.isLoaded() || !GuhsClientConfig.ADD_OFFICIAL_SERVER.get()) {
                return;
            }
            addOnce(Minecraft.getInstance());
        } catch (Exception e) {
            LOG.warn("Guhs: could not add the official server to the server list", e);
        }
    }

    private static void addOnce(Minecraft mc) throws IOException {
        Path marker = FMLPaths.CONFIGDIR.get().resolve(MARKER);
        if (Files.exists(marker)) {
            return;
        }
        Path serversDat = mc.gameDirectory.toPath().resolve("servers.dat");
        if (Files.exists(serversDat)) {
            // an unreadable servers.dat: leave it alone (ServerList.load would give an empty list and save() would wipe it)
            try {
                NbtIo.read(serversDat);
            } catch (Exception e) {
                LOG.warn("Guhs: servers.dat can't be read, not adding the official server", e);
                return;
            }
        }
        ServerList list = new ServerList(mc);
        list.load();
        boolean present = false;
        for (int i = 0; i < list.size(); i++) {
            if (OfficialServer.isOfficial(list.get(i).ip)) {
                present = true;
                break;
            }
        }
        if (!present) {
            list.add(new ServerData(OfficialServer.NAME, OfficialServer.ADDRESS, ServerData.Type.OTHER), false);
            for (int i = list.size() - 1; i > 0; i--) {
                list.swap(i, i - 1);                         // bubble it to the top
            }
            list.save();
            LOG.info("Guhs: added the official server ({}) to the top of the server list", OfficialServer.ADDRESS);
        }
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "The Guhs mod added the official Guhs server (" + OfficialServer.ADDRESS + ") to the multiplayer server list once"
                + (present ? " (it was already there)" : "") + ".\n"
                + "While this file exists it is never added again. To turn the feature off for good: addOfficialServer = false in guhs-client.toml.\n",
                StandardCharsets.UTF_8);
    }
}
