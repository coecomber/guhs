package nl.juiced.guhs.feature.vadsrijk.questrobot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * U-QUESTROBOT: writes a run to {@code <game dir>/questrobot/robot.json} (read by tools/vadsrijk/questrobot/rapport.py, which adds
 * the static checks and the standability of every target on the map tiles) and a short Dutch {@code robot.md}.
 */
public final class RobotRapport {
    private RobotRapport() {
    }

    public static Path map() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("questrobot");
    }

    /** Per zone: [gehaald, vast, niet gestart]. */
    public static Map<Integer, int[]> tellingen(QuestRobot.Rapport r) {
        Map<Integer, int[]> m = new TreeMap<>();
        for (QuestRobot.Uitslag u : r.quests.values()) {
            int[] t = m.computeIfAbsent(u.zone, z -> new int[3]);
            t[u.status.equals("gehaald") ? 0 : u.status.equals("vast") ? 1 : 2]++;
        }
        return m;
    }

    public static String samenvatting(QuestRobot.Rapport r) {
        StringBuilder sb = new StringBuilder();
        for (var e : tellingen(r).entrySet()) {
            int[] t = e.getValue();
            sb.append("zone ").append(e.getKey()).append(": ").append(t[0]).append(" gehaald, ").append(t[1]).append(" vast, ").append(t[2])
                    .append(" niet gestart\n");
        }
        return sb.toString();
    }

    public static Path schrijf(QuestRobot.Rapport r) throws IOException {
        Path dir = map();
        Files.createDirectories(dir);
        JsonObject o = new JsonObject();
        o.addProperty("totZone", r.totZone);
        o.addProperty("ms", r.ms);
        JsonArray alg = new JsonArray();
        r.algemeen.forEach(alg::add);
        o.add("algemeen", alg);
        JsonArray qs = new JsonArray();
        for (QuestRobot.Uitslag u : r.quests.values()) {
            JsonObject q = new JsonObject();
            q.addProperty("quest", u.quest);
            q.addProperty("zone", u.zone);
            q.addProperty("soort", u.soort);
            q.addProperty("status", u.status);
            q.addProperty("stap", u.stap);
            q.addProperty("waarom", u.waarom);
            JsonArray n = new JsonArray();
            u.notities.forEach(n::add);
            q.add("notities", n);
            JsonArray pr = new JsonArray();
            u.problemen.forEach(pr::add);
            q.add("problemen", pr);
            qs.add(q);
        }
        o.add("quests", qs);
        JsonArray ds = new JsonArray();
        for (QuestRobot.Doel d : r.doelen) {
            JsonObject x = new JsonObject();
            x.addProperty("quest", d.quest());
            x.addProperty("wat", d.wat());
            x.addProperty("id", d.id());
            x.addProperty("x", d.pos().getX());
            x.addProperty("y", d.pos().getY());
            x.addProperty("z", d.pos().getZ());
            ds.add(x);
        }
        o.add("doelen", ds);
        Path json = dir.resolve("robot.json");
        Files.writeString(json, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        StringBuilder md = new StringBuilder("# Questrobot (alleen de speelronde; tools/vadsrijk/questrobot/rapport.py maakt het hele rapport)\n\n");
        md.append(samenvatting(r).replace("\n", "  \n")).append('\n');
        r.algemeen.forEach(s -> md.append("- ").append(s).append('\n'));
        md.append('\n');
        for (QuestRobot.Uitslag u : r.quests.values()) {
            if (!u.status.equals("gehaald") || !u.problemen.isEmpty()) {
                md.append("- **").append(u.quest).append("** (zone ").append(u.zone).append(", ").append(u.soort).append("): ").append(u.status);
                if (!u.stap.isEmpty()) {
                    md.append(" bij ").append(u.stap);
                }
                if (!u.waarom.isEmpty()) {
                    md.append(": ").append(u.waarom);
                }
                md.append('\n');
                u.problemen.forEach(s -> md.append("  - ").append(s).append('\n'));
            }
        }
        Files.writeString(dir.resolve("robot.md"), md.toString(), StandardCharsets.UTF_8);
        return json;
    }
}
