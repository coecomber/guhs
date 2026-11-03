package nl.juiced.guhs.feature.erfenis.ftb;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.ErfenisData;
import nl.juiced.guhs.feature.erfenis.ErfenisMomenten;
import nl.juiced.guhs.feature.erfenis.Gebouw;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Goed;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/** The quests of "Een opknapper!" tick themselves off from the village state (ErfenisFtb). */
public class ErfenisFtbGameTests {
    private static final String BATCH = "erfenis_ftb";

    private static boolean klaar(ServerPlayer p, String naam) {
        AdvancementHolder h = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    private static int stat(ServerPlayer p, String naam) {
        return p.getStats().getValue(Stats.CUSTOM.get(ErfenisFtbFeature.STAT.get(naam).get()));
    }

    /** Every advancement the Java grants exists, and every statistic is registered. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void allesBestaat(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        List<String> namen = new java.util.ArrayList<>(ErfenisFtb.GEBOUWD.values());
        namen.addAll(ErfenisFtb.GEMAAKT.values());
        namen.addAll(List.of("erfenis_koop", "erfenis_overzicht", "erfenis_kaart", "erfenis_bezoek", "erfenis_bosje", "erfenis_glas",
                "erfenis_wolkenschaap", "erfenis_kleurglas", "erfenis_werkplaats", "erfenis_naar_huis", "erfenis_loket", "erfenis_gekleurde_wol",
                "erfenis_vadsmast", "erfenis_pension", "erfenis_deco", "erfenis_fontein", "erfenis_vondst", "erfenis_bedankje",
                "erfenis_handje", "erfenis_sterbestelling", "erfenis_hoger_dorp", "erfenis_bezwaar", "erfenis_zaak_eieren", "erfenis_zaak_voorbereiding"));
        for (String n : namen) {
            helper.assertTrue(s.getAdvancements().get(Guhs.id("quest/" + n)) != null, "advancement quest/" + n);
        }
        for (String n : ErfenisFtb.STATS) {
            helper.assertTrue(BuiltInRegistries.CUSTOM_STAT.containsKey(Guhs.id(n)), "statistic " + n);
        }
        helper.succeed();
    }

    /** Buildings, Hoeve, residents and plukjes; a moment makes it look at once; "made" counts only what rose after the first look. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dorpVinktAf(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        Base b = ErfenisData.get(s).vanSpeler(p.getUUID(), "Testguh", System.currentTimeMillis());
        b.goederen.put(Goed.MEEL, 5.0);
        ErfenisFtb.kijk(p);
        helper.assertTrue(!klaar(p, "erfenis_molen"), "meel that was already there is no meel made");
        helper.assertTrue(stat(p, "erfenis_hoeve") == b.hoeveLevel(), "the Hoeve statistic");
        b.hoeve().level = 5;
        b.gebouwen.add(new Gebouw(b.volgendeId++, GebouwType.KIPPENHOK, 1, 53, 41, 0));
        b.gebouwen.add(new Gebouw(b.volgendeId++, GebouwType.BOSJE, 1, 61, 41, 0));
        b.bewoners = 12;
        b.opgeruimd[0] = true;
        b.goederen.put(Goed.MEEL, 7.0);
        ErfenisMomenten.melding(s, p.getUUID(), "hoeve_level:5");   // (a moment: ErfenisFtb looks at once)
        helper.assertTrue(klaar(p, "erfenis_kippenhok"), "kippenhok built");
        helper.assertTrue(!klaar(p, "erfenis_bosje"), "a Bosje alone is not enough (Houtzagerij too)");
        helper.assertTrue(klaar(p, "erfenis_molen"), "2 meel made");
        helper.assertTrue(stat(p, "erfenis_hoeve") == 5 && stat(p, "erfenis_bewoners") == 12 && stat(p, "erfenis_plukjes") == 1, "statistics");
        b.bewoners = 3;
        ErfenisFtb.kijk(p);
        helper.assertTrue(stat(p, "erfenis_bewoners") == 12, "a statistic never goes down");
        helper.succeed();
    }

    /** Sending bouwspul home lowers the Leverkist allowance: that is what the quest Bouwpakket counts. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void naarHuisTelt(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        Base b = ErfenisData.get(s).vanSpeler(p.getUUID(), "Testguh", System.currentTimeMillis());
        b.goederen.put(Goed.STAM, 500.0);
        ErfenisFtb.kijk(p);
        helper.assertTrue(!klaar(p, "erfenis_naar_huis"), "nothing sent yet");
        Object r = b.naarHuis(Goed.STAM, 100, b.leverBijgewerkt + 1000);
        helper.assertTrue(r instanceof java.util.Map, "100 logs go home: " + r);
        ErfenisFtb.kijk(p);
        helper.assertTrue(klaar(p, "erfenis_naar_huis") && stat(p, "erfenis_naar_huis") == 100, "100 counted, got " + stat(p, "erfenis_naar_huis"));
        helper.succeed();
    }
}
