package nl.juiced.guhs.feature.vadsrijk.grotten;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken;
import nl.juiced.guhs.feature.vadsrijk.biomen.VadsrijkBiomen;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.puzzels.PuzzelDef;
import nl.juiced.guhs.feature.vadsrijk.puzzels.Puzzels;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-GROTTEN: the data (one cave per zone 1-5, every POI, the puzzle with a resolvable anchor, both Ambacht groups, the two
 * spawners, the biome, the sound streek, the area in front of the zone areas and only below the surface), and the player side
 * (discovery once, the treasure locked until the puzzle, once; the elite bonus once marked; the book category counts 4 per cave).
 * Run: {@code runGameTestServer -Pgt=VadsrijkGrotten}.
 */
public class VadsrijkGrottenGameTests {
    private static final String BATCH = "vadsrijk_grotten";

    @GuhTest(template = "empty", batch = BATCH)
    public static void data(GameTestHelper helper) {
        helper.assertTrue(GrotData.ALLE.size() == 5, "five caves");
        Set<Integer> zones = new HashSet<>();
        Grotten.gebieden();
        for (GrotData.Grot g : GrotData.ALLE) {
            zones.add(g.zone());
            for (String k : List.of("ingang", "binnen", "hal", "puzzel", "elite", "schat", "amb_1", "amb_2")) {
                helper.assertTrue(g.plek(k) != null, g.id() + ": POI " + k);
                helper.assertTrue(Grotten.plek("grot_" + g.id() + "_" + k) != null, g.id() + ": POI id grot_" + g.id() + "_" + k);
            }
            for (String k : List.of("binnen", "hal", "elite", "schat")) {
                BlockPos b = g.plek(k);
                helper.assertTrue(g.bevat(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5), g.id() + ": " + k + " inside the cave box");
                helper.assertTrue(VadsrijkBiomen.id(b.getX(), b.getY(), b.getZ()).equals(g.biome()), g.id() + ": the biome at " + k);
                Gebieden.Gebied a = Gebieden.op(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
                helper.assertTrue(a != null && a.id().equals(Grotten.gebiedId(g)), g.id() + ": the area at " + k + " = " + (a == null ? null : a.id()));
            }
            BlockPos boven = g.plek("hal").above(40);
            Gebieden.Gebied a = Gebieden.op(boven.getX() + 0.5, boven.getY(), boven.getZ() + 0.5);
            helper.assertTrue(a == null || !a.id().equals(Grotten.gebiedId(g)), g.id() + ": 40 blocks up is not the cave any more");
            PuzzelDef d = Puzzels.def(Grotten.puzzelId(g));
            helper.assertTrue(d != null, g.id() + ": its puzzle " + Grotten.puzzelId(g));
            helper.assertTrue(d.anker().punt() != null && d.anker().punt().equals(g.plek("puzzel")), g.id() + ": the puzzle anchor resolves");
            int groepen = 0;
            for (AmbachtPlekken.Groep gr : AmbachtPlekken.GROEPEN) {
                if (gr.poi().startsWith("grot_" + g.id() + "_")) {
                    groepen++;
                    helper.assertTrue(gr.anker() != null, gr.id() + ": anchor");
                }
            }
            helper.assertTrue(groepen == 2, g.id() + ": two Ambacht groups, got " + groepen);
            helper.assertTrue(Spawners.van("grot:" + g.id() + "#elite") != null && Spawners.van("grot:" + g.id() + "#mob") != null, g.id() + ": spawners");
            helper.assertTrue(Spawners.van("grot:" + g.id() + "#elite").eliteKans() >= 1.0, g.id() + ": the elite is always an elite");
            BlockPos h = g.plek("hal");
            GeluidPlan.Streek s = GeluidPlan.streekOp(h.getX() + 0.5, h.getY() + 0.5, h.getZ() + 0.5);
            helper.assertTrue(s != null && (s.bed().equals("grot") || s.bed().equals("wortel")), g.id() + ": the cave sound bed");
        }
        helper.assertTrue(zones.equals(Set.of(1, 2, 3, 4, 5)), "one cave in every zone " + zones);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void spelerKant(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        GrotData.Grot g = Grotten.grot("zwamgrot");
        Spelers.data(p).extra.remove(Grotten.DATA);
        Spelers.data(p).extra("puzzels").remove("opgelost");
        // discovery: once
        Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, Grotten.gebiedId(g)));
        helper.assertTrue(Grotten.heeft(p, g, "ontdekt"), "discovered on entering the area");
        // the treasure is locked until the puzzle is solved, then it comes once
        helper.assertFalse(Grotten.schat(p, g, false), "locked without the puzzle");
        helper.assertFalse(Grotten.heeft(p, g, "schat"), "nothing given while locked");
        CompoundTag o = new CompoundTag();
        o.putBoolean(Grotten.puzzelId(g), true);
        Spelers.data(p).extra("puzzels").put("opgelost", o);
        helper.assertTrue(Grotten.schat(p, g, false), "open after the puzzle");
        helper.assertFalse(Grotten.schat(p, g, false), "only once");
        // the elite: a mob from the cave's elite spawner marks it for every helper
        VadsrijkMob m = WezensRegistry.ZWAMWACHTER.get().create(helper.getLevel(), EntitySpawnReason.SPAWNER);
        helper.assertTrue(m != null, "elite mob");
        m.spawner = "grot:zwamgrot#elite";
        m.snapTo(p.getX(), p.getY(), p.getZ(), 0, 0);
        Haken.VERSLAGEN.forEach(h -> h.accept(new Haken.Verslagen(m, Set.of(p))));
        helper.assertTrue(Grotten.heeft(p, g, "elite"), "elite marked");
        // the book: 4 entries per cave, all 4 done now
        Voltooiing.Categorie c = Grotten.categorie(1);
        helper.assertTrue(c.dingen().size() == 4, "4 entries for the zone-1 cave");
        helper.assertTrue(Voltooiing.tel(p, c).klaar() == 4, "all 4 counted, got " + Voltooiing.tel(p, c).klaar());
        Spelers.data(p).extra.remove(Grotten.DATA);
        Spelers.data(p).extra("puzzels").remove("opgelost");
        helper.succeed();
    }
}
