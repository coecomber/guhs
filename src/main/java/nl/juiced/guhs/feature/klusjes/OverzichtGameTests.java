package nl.juiced.guhs.feature.klusjes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeOverzicht;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.TunnelBlock;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the huisje screen's overview "Wat kan hier?" (1.2.8, {@link HuisjeOverzicht}): fishing is no without
 * water, still no with one water block (three are needed) and yes with a little pond; a toy counts 0 and then 1 once it
 * stands there (a block toy, the ball entity and the jukebox); every chore is listed in the screen's order with how many
 * residents can do it; someone who isn't the owner (or stands too far away) gets no answer. (Template klusjes_test_tuin.)
 */
public class OverzichtGameTests {
    private static final String TUIN = "klusjes_test_tuin";

    private static CompoundTag rij(CompoundTag data, String lijst, String id) {
        ListTag l = data.getListOrEmpty(lijst);
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompoundOrEmpty(i).getStringOr("Id", "").equals(id)) {
                return l.getCompoundOrEmpty(i);
            }
        }
        throw new IllegalStateException("no row " + id + " in " + lijst);
    }

    private static String staat(CompoundTag data, String klus) {
        CompoundTag c = rij(data, "Klusjes", klus);
        return KlusStand.Staat.values()[c.getByteOr("Staat", (byte) 0)] + ":" + c.getStringOr("Reden", "") + ":" + c.getIntOr("Aantal", 0);
    }

    /** A fresh look (the tests change the world on purpose: the chores' scan is forgotten first). */
    private static CompoundTag vraag(GameTestHelper helper, ServerPlayer p, Huisje h) {
        KlusGebied.vergeet();
        CompoundTag data = HuisjeOverzicht.vraag(p, h.pos());
        helper.assertTrue(data != null, "the owner gets an answer");
        return data;
    }

    @GuhTest(template = TUIN, batch = "overzicht_vissen")
    public static void overzichtVissenNeeZonderWaterJaMetVijver(GameTestHelper helper) {
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(18, 2, 18));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        CompoundTag data = vraag(helper, p, h);
        // every chore, in the screen's order
        ListTag rijen = data.getListOrEmpty("Klusjes");
        helper.assertTrue(rijen.size() == Klusjes.alle().size(), "a row per chore");
        int i = 0;
        for (Klus k : Klusjes.alle()) {
            helper.assertTrue(rijen.getCompoundOrEmpty(i++).getStringOr("Id", "").equals(k.id()), "order: " + k.id());
        }
        helper.assertTrue(staat(data, "vissen").equals("NEE:geen:0"), "no water: " + staat(data, "vissen"));
        helper.assertTrue(rij(data, "Klusjes", "vissen").getStringOr("Wie", "").equals("guhs_schildpadjes"), "who can fish");
        helper.assertTrue(rij(data, "Klusjes", "vissen").getIntOr("Kunnen", -1) == 0, "nobody lives here yet");
        helper.assertTrue(staat(data, "opgraven").startsWith("JA:plekjes:"), "grass to dig in: " + staat(data, "opgraven"));
        helper.assertTrue(staat(data, "waken").equals("JA:rustig:0"), "on guard: " + staat(data, "waken"));
        helper.assertTrue(staat(data, "lampjes").equals("NEE:geen:0"), "no lamps: " + staat(data, "lampjes"));
        // one water source block: seen, but a pond needs three
        helper.setBlock(new BlockPos(15, 1, 16), Blocks.WATER);
        data = vraag(helper, p, h);
        helper.assertTrue(staat(data, "vissen").equals("NEE:te_weinig:1"), "one water block is too little: " + staat(data, "vissen"));
        helper.setBlock(new BlockPos(14, 1, 16), Blocks.WATER);
        helper.setBlock(new BlockPos(16, 1, 16), Blocks.WATER);
        data = vraag(helper, p, h);
        helper.assertTrue(staat(data, "vissen").equals("JA:water:3"), "a little pond: " + staat(data, "vissen"));
        // a lamp: there, but it is day (straks)
        helper.setBlock(new BlockPos(8, 2, 8), KlusjesFeature.GUHLAMPJE.get());
        // a resident that can fish
        KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), "vissen");
        data = vraag(helper, p, h);
        helper.assertTrue(staat(data, "lampjes").equals("STRAKS:tijd:1"), "a lamp by day: " + staat(data, "lampjes"));
        CompoundTag vissen = rij(data, "Klusjes", "vissen");
        helper.assertTrue(vissen.getIntOr("Kunnen", 0) == 1 && vissen.getIntOr("Aan", 0) == 1, "one resident fishes");
        CompoundTag farmen = rij(data, "Klusjes", "farmen");
        helper.assertTrue(farmen.getIntOr("Kunnen", 0) == 1 && farmen.getIntOr("Aan", -1) == 0, "it could farm, but that is switched off");
        helper.assertTrue(rij(data, "Klusjes", "polijsten").getIntOr("Kunnen", -1) == 0, "a guh doesn't polish stones");
        KlusjesGameTests.weg(helper, h, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "overzicht_speeltjes")
    public static void overzichtSpeeltjesNulDanEen(GameTestHelper helper) {
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(18, 2, 18));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        CompoundTag data = vraag(helper, p, h);
        for (String id : new String[]{"knabbelbal", "guh_glijbaantje", "pluizige_tunnel", "guh_wip", "guh_schommel", "jukebox", "kampvuur"}) {
            helper.assertTrue(rij(data, "Dingen", id).getIntOr("Aantal", -1) == 0, id + " is listed with 0: " + rij(data, "Dingen", id));
        }
        ToestelBlock.bouw(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 5)), SpeelgoedFeature.GLIJBAANTJE.get(), Direction.NORTH);
        helper.spawn(SpeelgoedFeature.KNABBELBAL.get(), new BlockPos(16, 2, 8));
        helper.setBlock(new BlockPos(18, 2, 5), Blocks.JUKEBOX);
        helper.setBlock(new BlockPos(5, 2, 18), Blocks.CAMPFIRE);
        // two tunnel pieces joined together: one tunnel
        BlockPos[] stukken = {helper.absolutePos(new BlockPos(16, 2, 16)), helper.absolutePos(new BlockPos(17, 2, 16))};
        for (int ronde = 0; ronde < 2; ronde++) {
            for (BlockPos stuk : stukken) {
                net.minecraft.world.level.block.state.BlockState s = ronde == 0 ? SpeelgoedFeature.TUNNEL.get().defaultBlockState()
                        .setValue(TunnelBlock.AXIS, Direction.Axis.X) : helper.getLevel().getBlockState(stuk);
                helper.getLevel().setBlock(stuk, TunnelBlock.vorm(s, helper.getLevel(), stuk), 3);
            }
        }
        data = vraag(helper, p, h);
        for (String id : new String[]{"knabbelbal", "guh_glijbaantje", "pluizige_tunnel", "jukebox", "kampvuur"}) {
            helper.assertTrue(rij(data, "Dingen", id).getIntOr("Aantal", -1) == 1, id + " counts 1: " + rij(data, "Dingen", id));
        }
        helper.assertTrue(rij(data, "Dingen", "guh_wip").getIntOr("Aantal", -1) == 0 && rij(data, "Dingen", "guh_schommel").getIntOr("Aantal", -1) == 0,
                "still no wip or schommel");
        helper.assertTrue(rij(data, "Dingen", "guh_glijbaantje").getStringOr("Naam", "").equals("block.guhs.guh_glijbaantje"), "named by its block");
        KlusjesGameTests.weg(helper, h, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "overzicht_eigenaar")
    public static void overzichtAlleenVoorDeEigenaar(GameTestHelper helper) {
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(18, 2, 18));
        ServerPlayer ander = KlusjesGameTests.speler(helper, new BlockPos(16, 2, 18));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        helper.assertTrue(HuisjeOverzicht.vraag(ander, h.pos()) == null, "someone else's request is refused");
        helper.assertTrue(HuisjeOverzicht.vraag(p, h.pos()) != null, "the owner's is answered");
        helper.assertTrue(HuisjeOverzicht.vraag(p, h.pos().above(3)) == null, "no huisje there");
        p.snapTo(p.getX() + 40, p.getY(), p.getZ());
        helper.assertTrue(HuisjeOverzicht.vraag(p, h.pos()) == null, "too far away");
        KlusjesGameTests.weg(helper, h, p, ander);
        helper.succeed();
    }
}
