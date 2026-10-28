package nl.juiced.guhs.feature.droombadje;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Wolkenbadje (batch "droombadje"). The game test server has no datapack dimensions, so these test the
 * island's data and the chapter's logic outside the dream: the template and its json, the float route with its three
 * stops, taking the bath things, and Snoes' bath giving the piece "bad".
 */
public class BadjeGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "droombadje";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        DroomVoortgang.reset(p);
        Badje.vergeet(p);
        return p;
    }

    /** The template exists and fits; the island json is the real one (not the placeholder); the route has its three stops. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void eilandEnRoute(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("dromen/eiland_badje")).orElse(null);
        helper.assertTrue(t != null, "the island template exists");
        helper.assertTrue(t.getSize().getX() <= DroomEilanden.MAX_MAAT.getX() && t.getSize().getY() <= DroomEilanden.MAX_MAAT.getY()
                && t.getSize().getZ() <= DroomEilanden.MAX_MAAT.getZ(), "it fits: " + t.getSize());
        DroomEilanden.Opzet opzet = DroomEilanden.opzet(DroomEilanden.BADJE);
        helper.assertTrue(opzet.versie() >= 1 && opzet.steiger() != null, "the real island json with a steiger");
        BadjeOpzet o = BadjeOpzet.get();
        helper.assertTrue(o != null && o.route().size() >= 6, "the float route reads");
        EnumSet<BadspulletjeBlock.Soort> soorten = EnumSet.noneOf(BadspulletjeBlock.Soort.class);
        int vorige = -1;
        for (BadjeOpzet.Stop s : o.stops()) {
            helper.assertTrue(s.soort() != null && soorten.add(s.soort()), "each thing once: " + s.soort());
            helper.assertTrue(s.index() > vorige && s.index() < o.route().size() - 1, "the stops come in order, before the end");
            helper.assertTrue(o.route().get(s.index()).distanceTo(s.hover()) < 0.01, "the route hovers at the stop");
            helper.assertTrue(s.hover().distanceTo(Vec3.atCenterOf(BlockPos.containing(s.spulletje()))) < 4.5, "the thing is in reach");
            vorige = s.index();
        }
        helper.assertTrue(soorten.size() == 3, "badeend, zeepje and schuim");
        helper.assertTrue(o.start().distanceTo(o.route().get(0)) < 8, "the route starts by the bubble pad");
        helper.succeed();
    }

    /** Taking the three things (once each), Snoes waiting until they're all there, then his bath gives the piece "bad". */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void spulletjesEnSnoes(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Badje.praat(p, null);
        helper.assertFalse(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.BAD), "no bath without the things");
        Badje.pak(p, BadspulletjeBlock.Soort.BADEEND, pos);
        Badje.pak(p, BadspulletjeBlock.Soort.BADEEND, pos);
        helper.assertTrue(Badje.aantal(p) == 1, "a thing counts once");
        Badje.pak(p, BadspulletjeBlock.Soort.ZEEPJE, pos);
        Badje.praat(p, null);
        helper.assertFalse(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.BAD), "two of three: Snoes still doesn't dare");
        Badje.pak(p, BadspulletjeBlock.Soort.SCHUIM, pos);
        helper.assertTrue(Badje.aantal(p) == 3, "all three");
        Badje.praat(p, null);
        helper.assertTrue(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.BAD), "Snoes' bath: the piece 'bad'");
        DroomVoortgang.reset(p);
        Badje.vergeet(p);
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** The bath thing block has its three kinds, and clicking one (as the block does) takes it along. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void spulletjeBlok(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos rel = new BlockPos(4, 2, 4);
        for (BadspulletjeBlock.Soort s : BadspulletjeBlock.Soort.values()) {
            helper.setBlock(rel, DroombadjeFeature.BADSPULLETJE.get().defaultBlockState().setValue(BadspulletjeBlock.SOORT, s));
            BlockPos abs = helper.absolutePos(rel);
            Badje.pak(p, helper.getLevel().getBlockState(abs).getValue(BadspulletjeBlock.SOORT), abs);
            helper.assertTrue(Badje.heeft(p, s), "took " + s.id());
        }
        Badje.vergeet(p);
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
