package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-GEROEZEMOES: the intensity logic of the crowd murmur (places, guh count, market spot, night, the bed crossfade, the one-shot
 * pauses and choice) and the sounds registered with their files. Run: {@code runGameTestServer -Pgt=Geroezemoes}.
 */
public class GeroezemoesGameTests {
    private static final String BATCH = "vadsrijk_geroezemoes";

    @GuhTest(template = "empty", batch = BATCH)
    public static void intensiteit(GameTestHelper helper) {
        // the places by area id (Hoogvads = every town area of zone 5)
        helper.assertTrue("hoogvads".equals(Geroezemoes.plekVan("z5_marktlaan", true)) && "hoogvads".equals(Geroezemoes.plekVan("z5_hoogvads", true)),
                "Hoogvads wijken");
        helper.assertTrue(Geroezemoes.plekVan("z5_staartvlakte", false) == null, "zone-5 wilderness is not a busy place");
        helper.assertTrue("kandijkom".equals(Geroezemoes.plekVan("z2_kandijkom_dok", true)) && "lampionhaven".equals(Geroezemoes.plekVan("z4_lampionhaven", true))
                && "saffraanpoort".equals(Geroezemoes.plekVan("z6_saffraanpoort", true)) && Geroezemoes.plekVan("klodderweide", false) == null
                && Geroezemoes.plekVan(null, false) == null, "place ids");
        // the user's order: Hoogvads > Saffraanpoort > Kandijkom > Zwamzoet > Lampionhaven > Honingtak > Rozenkloof > Knuffelkamp
        List<String> volgorde = List.of("hoogvads", "saffraanpoort", "kandijkom", "zwamzoet", "lampionhaven", "honingtak", "rozenkloof", "knuffelkamp");
        float vorig = 2;
        for (String plek : volgorde) {
            float i = Geroezemoes.intensiteit(plek, 10, true, false, false);
            helper.assertTrue(i < vorig && i > 0, plek + " should be quieter than the one before: " + i);
            vorig = i;
        }
        // more guhs = busier; a town never fully silent; night quieter; the market spot busier; wilderness only with a group
        float leeg = Geroezemoes.intensiteit("hoogvads", 0, false, false, false), vol = Geroezemoes.intensiteit("hoogvads", 30, true, false, false);
        helper.assertTrue(leeg > 0.2f && leeg < vol && vol <= 1f, "guh count: " + leeg + " < " + vol);
        helper.assertTrue(Geroezemoes.intensiteit("hoogvads", 30, true, false, true) < vol * 0.5f, "night quieter");
        helper.assertTrue(Geroezemoes.intensiteit("hoogvads", 8, true, true, false) > Geroezemoes.intensiteit("hoogvads", 8, true, false, false), "market spot");
        helper.assertTrue(Geroezemoes.intensiteit(null, 10, false, false, false) == 0f, "wilderness without a group: silent");
        float elders = Geroezemoes.intensiteit(null, 4, true, false, false);
        helper.assertTrue(elders > 0 && elders < Geroezemoes.intensiteit("knuffelkamp", 4, true, false, false), "a group elsewhere: a little");
        // the beds: quiet = only rustig (fading in from 0), full = only markt, in between a crossfade; never louder than 1
        float[] m0 = Geroezemoes.bedMix(0), m1 = Geroezemoes.bedMix(0.2f), m2 = Geroezemoes.bedMix(0.5f), m3 = Geroezemoes.bedMix(1f);
        helper.assertTrue(m0[0] == 0 && m0[1] == 0 && m0[2] == 0, "silent at 0");
        helper.assertTrue(m1[0] > 0 && m1[1] == 0 && m1[2] == 0, "quiet: rustig only");
        helper.assertTrue(m2[0] > 0 && m2[1] > 0 && m2[2] == 0, "mid: rustig -> druk");
        helper.assertTrue(m3[2] > 0.99f && m3[0] == 0 && m3[1] < 0.01f, "full: markt");
        for (float i = 0; i <= 1.001f; i += 0.05f) {
            float[] m = Geroezemoes.bedMix(i);
            helper.assertTrue(m[0] + m[1] + m[2] <= 1.001f, "never more than full at " + i);
        }
        // the one-shots: sparse when quiet, more often at a market (never below the 6 s floor); the choice covers every kind
        helper.assertTrue(Geroezemoes.pauze(0.1f, 0) > Geroezemoes.pauze(1f, 1) && Geroezemoes.pauze(1f, 0) >= 120, "pauses");
        java.util.Set<String> gezien = new java.util.HashSet<>();
        for (int k = 0; k < 100; k++) {
            gezien.add(Geroezemoes.kiesLos("hoogvads", k / 100.0));
        }
        helper.assertTrue(gezien.containsAll(Geroezemoes.LOS), "every one-shot reachable: " + gezien);
        helper.assertTrue("blub".equals(Geroezemoes.kiesLos("lampionhaven", 0.85)) && !"blub".equals(Geroezemoes.kiesLos("hoogvads", 0.85)),
                "more blub on the Lampionkust");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geluiden(GameTestHelper helper) {
        for (String b : Geroezemoes.BEDDEN) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id(Geroezemoes.bedId(b))), "bed not registered: " + b);
            String r = "/assets/guhs/sounds/vadsrijk/geroezemoes/bed_" + b + ".ogg";
            helper.assertTrue(GeroezemoesGameTests.class.getResource(r) != null, "missing " + r);
        }
        for (String s : Geroezemoes.LOS) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id(Geroezemoes.losId(s))), "one-shot not registered: " + s);
        }
        helper.succeed();
    }
}
