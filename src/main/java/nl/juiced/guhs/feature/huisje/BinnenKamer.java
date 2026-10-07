package nl.juiced.guhs.feature.huisje;

import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.Guhs;
import org.slf4j.Logger;

/**
 * 1.3.2: where everything stands in the room of one huisje size (template coordinates), read from
 * {@code data/guhs/huisje_binnen/kamers.json}, which tools/features/huisje_binnen.py writes together with the three room
 * templates {@code guhs:huisje_binnen/<maat>}. The file also holds the template version: a room stamped from a lower
 * version is stamped again the next time somebody goes in ({@link Binnen#zorg}).
 */
public record BinnenKamer(Vec3i maat, BlockPos mat, float yaw, List<BlockPos> deur, BlockPos prikbord, BlockPos logeerHoofd,
                          BlockPos logeerVoet, List<Bed> bedden) {
    /** One guh bed: the bed block, which way the sleeper looks (away from the wall), its name sign, bedside table and hook. */
    public record Bed(BlockPos bed, Direction kijk, BlockPos bord, BlockPos kastje, BlockPos haak) {
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier BESTAND = Guhs.id("huisje_binnen/kamers.json");
    /** No room is larger than this (the box that is cleared before a room is stamped). */
    public static final Vec3i MAX = new Vec3i(16, 6, 16);

    private static final Map<HuisjeMaat, BinnenKamer> KAMERS = new EnumMap<>(HuisjeMaat.class);
    private static int versie = -1;

    public static Identifier template(HuisjeMaat maat) {
        return Guhs.id("huisje_binnen/" + maat.id());
    }

    /** The version of the room templates in this jar (at least 1). */
    public static synchronized int versie(MinecraftServer server) {
        laad(server);
        return versie;
    }

    /** The layout of this size's room (null: the data file is missing or broken). */
    @Nullable
    public static synchronized BinnenKamer van(MinecraftServer server, HuisjeMaat maat) {
        laad(server);
        return KAMERS.get(maat);
    }

    /** (Server start) read the file again. */
    static synchronized void vergeet() {
        versie = -1;
        KAMERS.clear();
    }

    private static void laad(MinecraftServer server) {
        if (versie >= 0) {
            return;
        }
        versie = 1;
        KAMERS.clear();
        try {
            var res = server.getResourceManager().getResource(BESTAND);
            if (res.isEmpty()) {
                LOGGER.error("Guhs: {} is missing, huisjes cannot be entered", BESTAND);
                return;
            }
            try (Reader r = res.get().openAsReader()) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                versie = Math.max(1, o.get("versie").getAsInt());
                JsonObject kamers = o.getAsJsonObject("kamers");
                for (HuisjeMaat maat : HuisjeMaat.values()) {
                    JsonObject k = kamers.getAsJsonObject(maat.id());
                    if (k == null) {
                        continue;
                    }
                    List<Bed> bedden = new ArrayList<>();
                    for (var e : k.getAsJsonArray("bedden")) {
                        JsonObject b = e.getAsJsonObject();
                        Direction kijk = Direction.byName(b.get("kijk").getAsString());
                        bedden.add(new Bed(pos(b.getAsJsonArray("bed")), kijk == null ? Direction.SOUTH : kijk, pos(b.getAsJsonArray("bord")),
                                pos(b.getAsJsonArray("kastje")), pos(b.getAsJsonArray("haak"))));
                    }
                    List<BlockPos> deur = new ArrayList<>();
                    for (var e : k.getAsJsonArray("deur")) {
                        deur.add(pos(e.getAsJsonArray()));
                    }
                    BlockPos m = pos(k.getAsJsonArray("maat"));
                    JsonObject logeer = k.getAsJsonObject("logeerbed");
                    KAMERS.put(maat, new BinnenKamer(new Vec3i(m.getX(), m.getY(), m.getZ()), pos(k.getAsJsonArray("mat")), k.get("yaw").getAsFloat(),
                            List.copyOf(deur), pos(k.getAsJsonArray("prikbord")), pos(logeer.getAsJsonArray("hoofd")),
                            pos(logeer.getAsJsonArray("voet")), List.copyOf(bedden)));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Guhs: could not read {}", BESTAND, e);
        }
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }
}
