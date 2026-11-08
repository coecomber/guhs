package nl.juiced.guhs.feature.vadsrijk.vint;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * V-INT: the user's mount-drop rule (10 % per player per kill/clear, sure after 15 misses, per character per source) and the old
 * first-kill mount sources being drops now. Run: {@code runGameTestServer -Pgt=VInt}.
 */
public class VIntGameTests {
    private static final String BATCH = "vadsrijk_vint";

    @GuhTest(template = "empty", batch = BATCH)
    public static void rijdierBuitPity(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        String id = "opperzwam_zwamzwijntje";
        helper.assertTrue(RijdierBuit.KANS == 0.10 && RijdierBuit.PITY == 15, "10 % + pity 15");
        helper.assertTrue(RijdierBuit.BRONNEN.containsKey(id) && RijdierBuit.BRONNEN.containsKey("spiegelzucht_kristal")
                && RijdierBuit.BRONNEN.containsKey("vahoeg_goud"), "the three mount drop sources are registered");
        helper.assertTrue(!Rijdieren.heeft(p, RijdierSoort.ZWAMZWIJNTJE), "fresh player has no Zwamzwijntje");
        for (int i = 0; i < RijdierBuit.PITY; i++) {
            RijdierBuit.testRol = 0.99;   // a miss
            helper.assertTrue(!RijdierBuit.rol(p, id), "miss " + (i + 1));
        }
        helper.assertTrue(RijdierBuit.pech(p, id) == RijdierBuit.PITY, "15 misses counted");
        RijdierBuit.testRol = 0.99;       // would be a miss, but the pity makes it sure
        helper.assertTrue(RijdierBuit.rol(p, id) && Rijdieren.heeft(p, RijdierSoort.ZWAMZWIJNTJE), "the 16th try is sure");
        helper.assertTrue(RijdierBuit.pech(p, id) == 0, "counter reset");
        helper.assertTrue(!RijdierBuit.rol(p, id), "owned: no more rolls");
        // a lucky roll right away on another source
        ServerPlayer q = GuhMockPlayer.of(helper);
        RijdierBuit.testRol = 0.05;
        helper.assertTrue(RijdierBuit.rol(q, id), "a 5 % roll is under 10 %: dropped");
        // the Kristal Pluimguh only rolls for Pluimguh owners
        RijdierBuit.testRol = 0.0;
        helper.assertTrue(!RijdierBuit.rol(q, "spiegelzucht_kristal") && RijdierBuit.pech(q, "spiegelzucht_kristal") == 0,
                "no Pluimguh: no roll, no count");
        RijdierBuit.testRol = null;
        // no longer handed out on the first kill
        helper.assertTrue(!nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.SOORT_BRON.get(RijdierSoort.ZWAMZWIJNTJE).vanzelf(),
                "Zwamzwijntje is a drop, not a first-kill gift");
        helper.succeed();
    }

    /** V-INT: u-z6raid's wiring: the Sterrennacht-Bultguh as a mount drop, the raid frames' HUD spot, the tower soundscape. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void raidKoppelingen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(RijdierBuit.BRONNEN.containsKey(VInt.Z6RAID_BRON), "raid mount source registered");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHaken.OPGEVROLIJKT.size() >= 1, "raid clear hook wired");
        helper.assertTrue(!nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHalster.heeftSterrennacht(p), "fresh: no Sterrennacht");
        RijdierBuit.testRol = 0.05;
        helper.assertTrue(RijdierBuit.rol(p, VInt.Z6RAID_BRON), "a 5 % roll drops it");
        helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.BULTGUH) && nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHalster.heeftSterrennacht(p),
                "Bultguh + Sterrennacht coat given");
        RijdierBuit.testRol = 0.0;
        helper.assertTrue(!RijdierBuit.rol(p, VInt.Z6RAID_BRON), "owned: no more rolls");
        RijdierBuit.testRol = null;
        net.minecraft.core.BlockPos a = nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPlekken.midden(0);
        nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.Streek s = nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.streek(null, a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        helper.assertTrue(s == VInt.RAID_STREEK && s.themas().isEmpty() && "toren".equals(s.bed()), "Wachtzaal = the tower drone, no area music");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.themaVanId("guhs:vr.z6raid.muziek.ringeling") != null,
                "Ringeling's theme known (her fanfare follows Haken.VERSLAGEN)");
        helper.succeed();
    }
}
