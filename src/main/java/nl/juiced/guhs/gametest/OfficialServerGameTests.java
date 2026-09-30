package nl.juiced.guhs.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.OfficialServer;

/**
 * 1.0.1: the address matching behind "add the official server to the server list once" (client.OfficialServerEntry).
 * Runs on the dedicated gametest server, which also shows the client-only part isn't loaded there.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class OfficialServerGameTests {
    private static final String BATCH = "officialserver";

    @GameTest(template = "empty", batch = BATCH)
    public static void officieleServerHerkennen(GameTestHelper helper) {
        helper.assertTrue(OfficialServer.ADDRESS.equals("guhs.nl") && OfficialServer.NAME.equals("Guhs Server"), "name and address");
        for (String yes : new String[]{"guhs.nl", "GUHS.NL", " guhs.nl ", "guhs.nl.", "guhs.nl:25565", "play.guhs.nl", "Play.Guhs.nl:25565",
                "2.28.142.15", "2.28.142.15:25565", "guhs.nl:"}) {
            helper.assertTrue(OfficialServer.isOfficial(yes), "is the official server: '" + yes + "'");
        }
        for (String no : new String[]{null, "", "localhost", "guhs.nl:25566", "2.28.142.16", "mc.guhs.nl.example.com", "notguhs.nl",
                "hypixel.net", "::1", "[2001:db8::1]:25565"}) {
            helper.assertTrue(!OfficialServer.isOfficial(no), "is not the official server: '" + no + "'");
        }
        helper.succeed();
    }
}
