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

    /** U-SITE: the live map's privacy: an opted-out player is nowhere, a player in a dungeon only has a status (no coordinates). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void posities(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer gewoon = GuhMockPlayer.of(helper), weg = GuhMockPlayer.of(helper), kerker = GuhMockPlayer.of(helper);
        nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied burcht = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.van("z2_spiegelburcht");
        helper.assertTrue(burcht != null, "the Spiegelburcht area exists");
        nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied stad = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.van("zwamzoet");
        helper.assertTrue(stad != null, "Zwamzoet exists");
        gewoon.snapTo((stad.minX() + stad.maxX()) / 2.0 + 0.5, 70, (stad.minZ() + stad.maxZ()) / 2.0 + 0.5);
        kerker.snapTo((burcht.minX() + burcht.maxX()) / 2.0 + 0.5, 60, (burcht.minZ() + burcht.maxZ()) / 2.0 + 0.5);
        WebStats st = WebStats.get(server);
        st.zetVerborgen(weg.getUUID(), true);
        java.util.Map<ServerPlayer, String> namen = java.util.Map.of(gewoon, "Gewoontje", weg, "Verstopje", kerker, "Kerkermuis");
        try {
            JsonObject o = WebPosities.bouw(server, java.util.List.of(gewoon, weg, kerker), p -> true, namen::get);
            String json = o.toString();
            helper.assertTrue(!json.contains("Verstopje"), "an opted-out player is nowhere in posities.json");
            com.google.gson.JsonArray sp = o.getAsJsonArray("spelers"), stat = o.getAsJsonArray("status");
            helper.assertTrue(sp.size() == 1, "one visible player, got " + sp);
            JsonObject r = sp.get(0).getAsJsonObject();
            helper.assertTrue(r.get("naam").getAsString().equals("Gewoontje") && r.has("x") && r.has("z") && r.has("level") && r.has("klasse") && r.has("zone"),
                    "the visible player has name, x, z, level and class: " + r);
            for (String k : new String[] {"ip", "uuid", "adres", "munten"}) {
                helper.assertTrue(!r.has(k), "a position row never holds '" + k + "'");
            }
            helper.assertTrue(stat.size() == 1, "the dungeon player is a status row: " + stat);
            JsonObject s = stat.get(0).getAsJsonObject();
            helper.assertTrue(s.get("naam").getAsString().equals("Kerkermuis") && s.get("waar").getAsString().equals(WebPosities.KERKER)
                    && !s.has("x") && !s.has("z"), "the dungeon player has a status and no coordinates: " + s);
            helper.assertTrue(o.has("schepen") && o.has("tijd"), "posities.json has the airships + a time stamp");
            helper.assertTrue(WebPosities.verborgenPlek(gewoon) == null, "the test spot is not a hidden place");
        } finally {
            st.zetVerborgen(weg.getUUID(), false);
        }
        helper.succeed();
    }

    /** U-SITE: zone 6's hidden places: the raid tower + the underground dungeon are hooked in (status only, never a position). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void positiesZone6(GameTestHelper helper) {
        helper.assertTrue(WebPosities.VERBORGEN_PLEKKEN.size() >= 2, "the raid + the zone-6 dungeon hooks are registered");
        helper.assertTrue(WebPosities.inZ6Kerker(4000.5, 31, -970.5), "the Verzonken Sterrenwacht rooms are a hidden place");
        helper.assertTrue(!WebPosities.inZ6Kerker(4000.5, 75, -970.5), "the desert above the dungeon is not hidden");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPlekken.inToren(
                nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPlekken.anker().getX() + 0.5,
                nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPlekken.anker().getY() + 1,
                nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPlekken.anker().getZ() + 0.5), "the raid tower anchor is inside the tower");
        helper.succeed();
    }
}
