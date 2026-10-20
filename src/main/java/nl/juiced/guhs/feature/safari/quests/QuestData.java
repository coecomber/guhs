package nl.juiced.guhs.feature.safari.quests;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

/**
 * What the park generator wrote for the quests (tools/features/safari_park/zone_zz_s4.py): the eight geheimpjes
 * ({@code data/guhs/safari/geheimpjes.json}) and Dikkie's stal with the olifantenloopband ({@code quests.json}).
 * Read once from the jar; missing files give an empty list / no stal (the quests then simply have nothing to click).
 */
public final class QuestData {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** One geheimpje: its id (= its zone), the zone, the block to click. */
    public record Geheimpje(String id, String zone, BlockPos pos) {
    }

    public final List<Geheimpje> geheimpjes;
    @Nullable
    public final BlockPos stal, poort, loopband;

    private static volatile QuestData data;

    private QuestData(List<Geheimpje> geheimpjes, @Nullable BlockPos stal, @Nullable BlockPos poort, @Nullable BlockPos loopband) {
        this.geheimpjes = List.copyOf(geheimpjes);
        this.stal = stal;
        this.poort = poort;
        this.loopband = loopband;
    }

    public static QuestData get() {
        QuestData d = data;
        if (d == null) {
            synchronized (QuestData.class) {
                if (data == null) {
                    data = laad();
                }
                d = data;
            }
        }
        return d;
    }

    /** Forget the cached data (tests, a dev reload). */
    public static synchronized void vergeet() {
        data = null;
    }

    @Nullable
    public Geheimpje geheimpjeOp(BlockPos pos) {
        for (Geheimpje g : geheimpjes) {
            if (g.pos().equals(pos)) {
                return g;
            }
        }
        return null;
    }

    private static QuestData laad() {
        List<Geheimpje> lijst = new ArrayList<>();
        JsonElement g = lees("geheimpjes.json");
        if (g != null && g.isJsonObject() && g.getAsJsonObject().has("geheimpjes")) {
            for (JsonElement e : g.getAsJsonObject().getAsJsonArray("geheimpjes")) {
                JsonObject o = e.getAsJsonObject();
                lijst.add(new Geheimpje(o.get("id").getAsString(), o.get("zone").getAsString(), pos(o.getAsJsonArray("pos"))));
            }
        }
        BlockPos stal = null, poort = null, loopband = null;
        JsonElement q = lees("quests.json");
        if (q != null && q.isJsonObject() && q.getAsJsonObject().has("olifant")) {
            JsonObject o = q.getAsJsonObject().getAsJsonObject("olifant");
            stal = pos(o.getAsJsonArray("stal"));
            poort = pos(o.getAsJsonArray("poort"));
            loopband = pos(o.getAsJsonArray("loopband"));
        }
        LOGGER.info("Safari quests: {} geheimpjes, Dikkie's stal {}", lijst.size(), stal);
        return new QuestData(lijst, stal, poort, loopband);
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    @Nullable
    private static JsonElement lees(String bestand) {
        try (InputStream in = QuestData.class.getResourceAsStream("/data/guhs/safari/" + bestand)) {
            if (in == null) {
                return null;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r);
            }
        } catch (Exception e) {
            LOGGER.error("Safari quests: could not read {}", bestand, e);
            return null;
        }
    }
}
