package nl.juiced.guhs.feature.vadsrijk.kern;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-SPEL: no raw Vadsrijk keys in game. The user saw "vadsrijk.kern.gebied.z5_kathedraal - veilig gebied" on a zone change: the
 * zone-5 districts had lost their names. This checks the keys that are BUILT at run time from registered ids against nl_nl.json
 * and en_us.json (read from the jar, so both languages count): every area that is really registered (Gebieden, incl. the zones'
 * own and generated boxes), every NPC name and every quest title/text. The static half (literal keys and prefix + literal in the
 * code, keys in data files) is tools/vadsrijk/taalcheck.py.
 */
public class VadsrijkTaalGameTests {
    private static final String BATCH = "vadsrijk_taal";

    private static Set<String> taal(String naam) {
        Set<String> s = new HashSet<>();
        try (InputStream in = VadsrijkTaalGameTests.class.getResourceAsStream("/assets/guhs/lang/" + naam + ".json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                s.addAll(o.keySet());
            }
        } catch (Exception ignored) {
            // (an empty set fails every check below, which is what we want)
        }
        return s;
    }

    private static void eis(GameTestHelper helper, String wat, List<String> keys) {
        Set<String> nl = taal("nl_nl"), en = taal("en_us");
        TreeSet<String> mis = new TreeSet<>();
        for (String k : keys) {
            if (!nl.contains(k)) {
                mis.add(k + " (nl)");
            }
            if (!en.contains(k)) {
                mis.add(k + " (en)");
            }
        }
        if (!mis.isEmpty()) {
            helper.fail(wat + ": " + mis.size() + " ontbreken: " + mis);
        }
    }

    /** Every registered area has a name for the zone-change title card, plus the "veilig gebied" line. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void gebiedNamen(GameTestHelper helper) {
        List<String> keys = new java.util.ArrayList<>(Gebieden.alle().stream().map(Gebieden.Gebied::naamKey).distinct().toList());
        keys.add("vadsrijk.kern.gebied.veilig");
        helper.assertTrue(keys.size() > 40, "the zones registered their areas (" + keys.size() + ")");
        eis(helper, "gebiedsnamen", keys);
        helper.succeed();
    }

    /** Every NPC has a name; every quest a title, a text and its objective lines. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcEnQuestTeksten(GameTestHelper helper) {
        List<String> keys = new java.util.ArrayList<>();
        for (NpcDef d : Npcs.alle()) {
            keys.add(d.naamKey());
        }
        for (Quest q : Quests.alle().values()) {
            keys.add(q.titelKey());
            keys.add(q.tekstKey());
            for (int i = 0; i < q.doelen().size(); i++) {
                keys.add(q.doelKey(i));
            }
        }
        eis(helper, "npc/quest-teksten", keys);
        helper.succeed();
    }
}
