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

    /** 1.10.3 "pech zichtbaar": the mount screen data carries the pity counter per source, the pet screen data the cave pet's. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void pechInSchermData(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            String id = "opperzwam_zwamzwijntje";
            for (int i = 0; i < 8; i++) {
                RijdierBuit.testRol = 0.99;   // a miss
                RijdierBuit.rol(p, id);
            }
            net.minecraft.nbt.CompoundTag e = pechVan(economieData(p), RijdierSoort.ZWAMZWIJNTJE, id);
            helper.assertTrue(e != null, "the Zwamzwijntje row carries its drop source");
            helper.assertTrue(e.getIntOr("pech", -1) == 8 && e.getIntOr("pity", 0) == RijdierBuit.PITY && e.getIntOr("kans", 0) == 10,
                    "pech 8/15 at 10 %: " + e);
            helper.assertTrue("kill".equals(e.getStringOr("per", "")) && e.getStringOr("kleur", "x").isEmpty() && e.getBooleanOr("mag", false),
                    "per kill, the mount itself, counts");
            helper.assertTrue(RijdierBuit.misTekst("x", 8).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                    && "vadsrijk.vint.buit.mis".equals(tc.getKey()) && Integer.valueOf(7).equals(tc.getArgs()[4]), "miss line: 7 more until a sure drop");
            helper.assertTrue(RijdierBuit.misTekst("x", 15).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tz
                    && "vadsrijk.vint.buit.mis.zeker".equals(tz.getKey()), "miss line at 15/15: the next one is sure");
            // the Kristal colour: no Pluimguh yet = shown, but it doesn't count
            net.minecraft.nbt.CompoundTag k = pechVan(economieData(p), RijdierSoort.PLUIMGUH, "spiegelzucht_kristal");
            helper.assertTrue(k != null && "kristal".equals(k.getStringOr("kleur", "")) && !k.getBooleanOr("mag", true), "Kristal: needs a Pluimguh first");
            // a cave colour with a Pluimguh: its counter + where it drops
            Rijdieren.geef(p, "pluimguh:honing");
            String grot = "grot_zwamgrot_pluim";
            for (int i = 0; i < RijdierBuit.PITY; i++) {
                RijdierBuit.testRol = 0.99;
                RijdierBuit.rol(p, grot);
            }
            net.minecraft.nbt.CompoundTag g = pechVan(economieData(p), RijdierSoort.PLUIMGUH, grot);
            helper.assertTrue(g != null && g.getIntOr("pech", 0) == RijdierBuit.PITY && "glimzwam".equals(g.getStringOr("kleur", ""))
                    && "vadsrijk.grotten.naam.zwamgrot".equals(g.getStringOr("waarArg", "")), "Glimzwam 15/15 from the Zwamgrot: " + g);
            // owned: no entry any more
            RijdierBuit.testRol = 0.99;
            helper.assertTrue(RijdierBuit.rol(p, id), "the sure one");
            helper.assertTrue(pechVan(economieData(p), RijdierSoort.ZWAMZWIJNTJE, id) == null, "owned: no pity line");
            // the pet screen: the cave pet's counter
            nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBazen.Def d = nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBazen.def("zwamgrot");
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).extra(nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.DATA).putInt("zwamgrot.petpech", 9);
            net.minecraft.nbt.CompoundTag h = new net.minecraft.nbt.CompoundTag();
            nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerLoot.schermData(p, h);
            net.minecraft.nbt.CompoundTag pp = h.getCompoundOrEmpty("petpech").getCompoundOrEmpty(d.pet());
            helper.assertTrue(pp.getIntOr("pech", 0) == 9 && pp.getIntOr("pity", 0) == nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerLoot.PET_PITY
                    && pp.getIntOr("kans", 0) == 6, "pet pech 9/25 at 6 %: " + pp);
            helper.succeed();
        } finally {
            RijdierBuit.testRol = null;
            Rijdieren.reset(p);
        }
    }

    private static net.minecraft.nbt.CompoundTag economieData(ServerPlayer p) {
        return nl.juiced.guhs.feature.vadsrijk.economie.Stal.data(p, false);
    }

    @javax.annotation.Nullable
    private static net.minecraft.nbt.CompoundTag pechVan(net.minecraft.nbt.CompoundTag d, RijdierSoort s, String bron) {
        net.minecraft.nbt.ListTag l = d.getListOrEmpty("rijdieren");
        for (int i = 0; i < l.size(); i++) {
            net.minecraft.nbt.CompoundTag t = l.getCompoundOrEmpty(i);
            if (s.id().equals(t.getStringOr("soort", ""))) {
                net.minecraft.nbt.ListTag pl = t.getListOrEmpty("pech");
                for (int j = 0; j < pl.size(); j++) {
                    if (bron.equals(pl.getCompoundOrEmpty(j).getStringOr("bron", ""))) {
                        return pl.getCompoundOrEmpty(j);
                    }
                }
            }
        }
        return null;
    }
}
