package nl.juiced.guhs.feature.vadsrijk.web;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * (S-SITE) guhs.nl/vadsrijk: the wiki dump has every section, and it is written to {@code vadsrijk_runtime.json} in the run dir
 * (or the path in {@code -Dguhs.wikiexport}); the live export's allowlist holds no position/IP/coins; files are written atomically.
 * Run: {@code runGameTestServer -Pgt=VadsrijkWeb}.
 */
public class VadsrijkWebGameTests {
    private static final String BATCH = "vadsrijk_web";

    @GuhTest(template = "empty", batch = BATCH)
    public static void wikiExport(GameTestHelper helper) {
        JsonObject o = WikiExport.bouw();
        String fout = WikiExport.controleer(o);
        helper.assertTrue(fout == null, "wiki export: " + fout);
        try {
            Path p = Path.of(System.getProperty("guhs.wikiexport", "vadsrijk_runtime.json"));
            WikiExport.schrijf(p);
            helper.assertTrue(Files.size(p) > 1000, "the wiki export file has content");
            System.out.println("[vadsrijk web] wiki export -> " + p.toAbsolutePath());
        } catch (java.io.IOException e) {
            helper.fail("wiki export not written: " + e);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void allowlist(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        CompoundTag t = WebExport.snapshot(p);
        for (String verboden : new String[] {"x", "y", "z", "pos", "ip", "munten", "dimensie", "gebied", "chat"}) {
            helper.assertTrue(!t.contains(verboden), "the snapshot never holds '" + verboden + "'");
        }
        helper.assertTrue(t.getStringOr("gezien", "").matches("\\d{4}-\\d{2}-\\d{2}"), "last seen is a date only");
        helper.assertTrue(t.getIntOr("lv", 0) >= 1, "the snapshot has a level");
        // status + world build without a crash (also without the Vadsrijk dimension)
        JsonObject s = WebExport.status(helper.getLevel().getServer());
        helper.assertTrue(s.has("online") && s.has("aantal"), "status.json has online + count");
        JsonObject w = WebExport.wereld(helper.getLevel().getServer());
        helper.assertTrue(w.has("bazen") && w.has("dagreset"), "wereld.json has boss timers + the reset");
        // atomic write
        try {
            Path dir = Files.createTempDirectory("guhs-web");
            Path f = dir.resolve("hiscores").resolve("test.json");
            WebExport.schrijfAtomisch(f, "{\"a\":1}");
            helper.assertTrue(Files.readString(f).equals("{\"a\":1}") && !Files.exists(f.resolveSibling("test.json.tmp")), "atomic write leaves no .tmp");
        } catch (java.io.IOException e) {
            helper.fail("atomic write: " + e);
        }
        helper.succeed();
    }
}
