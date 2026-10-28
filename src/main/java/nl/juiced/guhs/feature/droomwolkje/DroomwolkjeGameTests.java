package nl.juiced.guhs.feature.droomwolkje;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the droomwolkje (batch "droomwolkje"). The game test server has no datapack dimensions, so these test the
 * grid, the building rights and the guests' messages without the dream itself.
 */
public class DroomwolkjeGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "droomwolkje";

    /** Two players, two wolkjes on different grid spots: you build on yours (and a friend's), never on someone else's or in between. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void droomwolkjeBouwrechten(GameTestHelper helper) {
        Wolkjes.Stand s = new Wolkjes.Stand();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        Wolkjes.Wolkje wa = s.maak(a), wb = s.maak(b);
        helper.assertTrue(s.maak(a) == wa, "one wolkje per player");
        helper.assertTrue(wa.nummer() != wb.nummer(), "different grid spots");
        BlockPos ma = Wolkjes.midden(wa.nummer()), mb = Wolkjes.midden(wb.nummer());
        helper.assertTrue(Math.abs(ma.getX() - mb.getX()) + Math.abs(ma.getZ() - mb.getZ()) >= Wolkjes.RASTER, "wolkjes never touch");
        helper.assertTrue(Wolkjes.RASTER > 2 * Wolkjes.HALF, "sky between two wolkje boxes");
        BlockPos opA = ma.offset(5, 2, -3), opB = mb.offset(-Wolkjes.HALF, 1, Wolkjes.HALF);
        helper.assertTrue(s.eigenaarVan(opA).equals(a) && s.eigenaarVan(opB).equals(b), "who owns which block");
        helper.assertTrue(s.mag(a, opA) && !s.mag(a, opB), "a builds on a's wolkje, not on b's");
        helper.assertTrue(s.mag(b, opB) && !s.mag(c, opA), "b on b's; a stranger nowhere");
        BlockPos tussen = ma.offset(Wolkjes.RASTER / 2, 0, 0);
        helper.assertTrue(s.eigenaarVan(tussen) == null && !s.mag(a, tussen), "the sky between wolkjes is nobody's");
        helper.assertTrue(s.eigenaarVan(new BlockPos(0, 96, 0)) == null, "the plein is no wolkje");
        helper.assertTrue(s.eigenaarVan(ma.atY(Wolkjes.ONDER - 1)) == null, "below the box: nobody's");
        wa.vrienden().add(c);
        helper.assertTrue(s.mag(c, opA) && !s.mag(c, opB), "a friend builds along on a's wolkje only");
        s.vergeet(a);
        helper.assertTrue(s.van(a) == null && s.maak(a).nummer() != wa.nummer() && s.maak(a).nummer() != wb.nummer(), "a forgotten wolkje's spot is never handed out again");
        helper.succeed();
    }

    /** A knuffel for a sleeping guh's guest: an offline owner gets the message at the next login (once). */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void droomgastBerichtBijInloggen(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        UUID eigenaar = UUID.randomUUID();
        Component bericht = Droomgasten.bericht("knuffel", Component.literal("Juiced"), Component.literal("Pien"));
        helper.assertTrue(bericht.getString().contains("Juiced") || bericht.getString().contains("gui.guhs"), "the message names the giver");
        Droomgasten.meld(server, eigenaar, bericht);
        Droomgasten.meld(server, eigenaar, Droomgasten.bericht("knabbel", Component.literal("Juiced"), Component.literal("Pien")));
        List<Component> post = Droomgasten.bezorg(server, eigenaar);
        helper.assertTrue(post.size() == 2, "two messages waited: " + post.size());
        helper.assertTrue(Droomgasten.bezorg(server, eigenaar).isEmpty(), "delivered once");
        helper.succeed();
    }
}
