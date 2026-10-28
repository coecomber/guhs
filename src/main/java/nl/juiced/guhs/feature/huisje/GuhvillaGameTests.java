package nl.juiced.guhs.feature.huisje;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0: the Guhvilla (twenty residents), the range slider of the huisjes (clamped per size, the home base follows it) and
 * the villa's hall: a guh sleeps in a guh vakje of the new beds, a muisje in a muisje vakje.
 */
public class GuhvillaGameTests {
    private static final String TUIN = "huisje_test_tuin";
    private static final String VELD = "huisje_binnen_test";
    private static final String BATCH = "huisje_villa";

    @GuhTest(template = TUIN, batch = BATCH)
    public static void villaNeemtTwintigBewoners(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(12, 1, 12));
        Huisje h = HuisjeGameTests.bouw(helper, new BlockPos(5, 1, 4), HuisjeMaat.VILLA, p);
        helper.assertTrue(h.maat().plekken() == 20 && h.maat().hoogte() == 5, "a villa: twenty places, five tall");
        TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(12, 1, 18));
        muis.tame(p);
        helper.assertTrue(Huisjes.trekIn(h, muis), "a muisje moves in");
        for (int i = 1; i < 20; i++) {
            GuhEntity g = HuisjeGameTests.guh(helper, p, new BlockPos(2 + i, 1, 14 + i % 3));
            helper.assertTrue(Huisjes.trekIn(h, g), "guh " + i + " moves in");
        }
        helper.assertTrue(h.isVol() && h.bewoners().size() == 20, "the villa is full with twenty");
        GuhEntity teVeel = HuisjeGameTests.guh(helper, p, new BlockPos(20, 1, 20));
        helper.assertTrue(!Huisjes.trekIn(h, teVeel), "the twenty-first stays outside");
        HuisjeGameTests.weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void schuifjeBegrenstHetBereik(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(12, 1, 20));
        Huisje klein = HuisjeGameTests.bouw(helper, new BlockPos(3, 1, 3), HuisjeMaat.KLEIN, p);
        Huisje groot = HuisjeGameTests.bouw(helper, new BlockPos(12, 1, 3), HuisjeMaat.GROOT, p);
        Huisje villa = HuisjeGameTests.bouw(helper, new BlockPos(19, 1, 10), HuisjeMaat.VILLA, p);
        helper.assertTrue(klein.bereik() == 16 && groot.bereik() == 16 && villa.bereik() == 20, "the defaults: 16, 16 and the villa 20");
        helper.assertTrue(klein.zetBereik(999) == 17 && groot.zetBereik(999) == 22 && villa.zetBereik(999) == 32,
                "up to 10% (klein) and 40% (groot) more, the villa up to 32: " + klein.bereik() + "/" + groot.bereik() + "/" + villa.bereik());
        helper.assertTrue(klein.zetBereik(1) == 8 && villa.zetBereik(1) == 10, "down to half the default");
        villa.zetBereik(24);
        Vec3 m = villa.midden();
        helper.assertTrue(villa.inGebied(BlockPos.containing(m.x + 23, m.y, m.z)) && !villa.inGebied(BlockPos.containing(m.x + 26, m.y, m.z)),
                "the home base follows the slider");
        // the screen's button (as the client sends it) clamps too, and the residents get the new home radius
        GuhEntity g = HuisjeGameTests.guh(helper, p, new BlockPos(16, 1, 18));
        helper.assertTrue(Huisjes.trekIn(villa, g), "a guh moves in");
        p.snapTo(helper.absolutePos(new BlockPos(19, 1, 16)).getCenter());
        HuisjePayloads.doe(p, new HuisjePayloads.Doe(villa.pos(), HuisjePayloads.Actie.BEREIK.ordinal(), "", "", false, 30));
        helper.assertTrue(villa.bereik() == 30 && (int) g.getHomeRadius() == 30, "the slider's 30: " + villa.bereik() + ", home radius " + g.getHomeRadius());
        HuisjeGameTests.weg(helper, p);
        helper.succeed();
    }

    /** The villa's hall: the new beds stand, a guh sleeps in a guh vakje and the muisje in a muisje vakje. */
    @GuhTest(template = VELD, batch = BATCH + "_binnen", timeoutTicks = 300)
    public static void villaBinnenSlaaptInEenVakje(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 20));
        ServerLevel level = helper.getLevel();
        MinecraftServer s = level.getServer();
        Huisje h = HuisjeGameTests.bouw(helper, new BlockPos(3, 2, 3), HuisjeMaat.VILLA, p);
        Binnen.testKamer(h, level, helper.absolutePos(new BlockPos(8, 2, 2)));
        BinnenKamer k = BinnenKamer.van(s, HuisjeMaat.VILLA);
        helper.assertTrue(k != null && k.getypt() && k.bedden().stream().filter(b -> b.soort().equals("guh")).count() >= 20,
                "kamers.json: the villa has vakjes for twenty guhs");
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        GuhEntity guh = HuisjeGameTests.guh(helper, p, new BlockPos(2, 2, 14));
        guh.setCustomName(net.minecraft.network.chat.Component.literal("Villadutje"));
        helper.assertTrue(Huisjes.trekIn(h, guh), "the guh moves in");
        Huisjes.naarBinnen(guh, h);
        TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(3, 2, 14));
        muis.tame(p);
        helper.assertTrue(Huisjes.trekIn(h, muis), "the muisje moves in");
        Huisjes.naarBinnen(muis, h);
        helper.assertTrue(Binnen.betreed(p, h), "in");
        BlockPos o = Binnen.oorsprong(h.cel());
        for (BinnenKamer.Bed b : k.bedden()) {
            helper.assertTrue(!level.getBlockState(o.offset(b.bed())).isAir(), "a bed stands at vakje " + b.bed());
        }
        List<BinnenInrichting.Slot> slots = BinnenInrichting.slots(s, h);
        int guhBed = -1, muisBed = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) != null && slots.get(i).id().equals(Band.id(guh))) {
                guhBed = i;
            }
            if (slots.get(i) != null && slots.get(i).id().equals(Band.id(muis))) {
                muisBed = i;
            }
        }
        helper.assertTrue(slots.size() == k.bedden().size() && guhBed >= 0 && k.bedden().get(guhBed).soort().equals("guh"),
                "the guh got a guh vakje: " + (guhBed < 0 ? "none" : k.bedden().get(guhBed).soort()));
        helper.assertTrue(muisBed >= 0 && k.bedden().get(muisBed).soort().equals("muis"), "the muisje got a muisje vakje");
        Vec3 plek = BinnenInrichting.slaapPlek(o, k.bedden().get(guhBed));
        List<BinnenGuh> standIns = BinnenInrichting.dingen(level, h).stream().filter(e -> e instanceof BinnenGuh).map(e -> (BinnenGuh) e).toList();
        helper.assertTrue(standIns.size() == 1 && standIns.get(0).slaapt() && standIns.get(0).position().distanceTo(plek) < 0.05,
                "the guh sleeps right in its vakje: " + standIns.stream().map(Entity::position).toList() + " / " + plek);
        Binnen.naarBuiten(p, null);
        HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
        GuhQuests.saved(p).remove(Binnen.DATA);
        Binnen.testKlaar(h);
        HuisjeGameTests.weg(helper, p);
        helper.succeed();
    }
}
