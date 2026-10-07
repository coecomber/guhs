package nl.juiced.guhs.feature.guhpixel.lobby;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.Guhs;

/**
 * Where the lobby's own things are: the ten golden knabbels, the spots of the chat guhs, the parkour's start, finish and
 * checkpoints. One source of truth: tools/features/guhpixel_lobby_bouw.py builds the template from these tables and writes
 * them to {@code data/guhs/guhpixel/lobby_kaart.json}; this class only reads that file (world coordinates).
 */
public final class LobbyKaart {
    private static final Identifier BESTAND = Guhs.id("guhpixel/lobby_kaart.json");

    /** A chat guh's spot. */
    public record Plek(BlockPos pos, float yaw) {
    }

    public record Kaart(List<BlockPos> knabbels, List<Plek> chatguhs, BlockPos start, BlockPos finish, List<BlockPos> tussen) {
    }

    private static final Kaart LEEG = new Kaart(List.of(), List.of(), BlockPos.ZERO, BlockPos.ZERO, List.of());
    private static Kaart kaart;

    public static Kaart van(MinecraftServer server) {
        if (kaart == null) {
            kaart = LEEG;
            try {
                var res = server.getResourceManager().getResource(BESTAND);
                if (res.isPresent()) {
                    try (Reader r = res.get().openAsReader()) {
                        JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                        List<Plek> guhs = new ArrayList<>();
                        for (JsonElement e : o.getAsJsonArray("chatguhs")) {
                            JsonArray a = e.getAsJsonArray();
                            guhs.add(new Plek(pos(a), a.get(3).getAsFloat()));
                        }
                        JsonObject parkour = o.getAsJsonObject("parkour");
                        kaart = new Kaart(lijst(o.getAsJsonArray("knabbels")), List.copyOf(guhs), pos(parkour.getAsJsonArray("start")),
                                pos(parkour.getAsJsonArray("finish")), lijst(parkour.getAsJsonArray("tussen")));
                    }
                }
            } catch (Exception e) {
                LogUtils.getLogger().warn("Guhpixel: could not read {}", BESTAND, e);
            }
        }
        return kaart;
    }

    /** (Server start) read the file again. */
    static void vergeet() {
        kaart = null;
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static List<BlockPos> lijst(JsonArray a) {
        List<BlockPos> uit = new ArrayList<>();
        for (JsonElement e : a) {
            uit.add(pos(e.getAsJsonArray()));
        }
        return List.copyOf(uit);
    }

    private LobbyKaart() {
    }
}
