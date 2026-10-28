package nl.juiced.guhs.feature.dromen.liedjes;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Slaapliedjesheuvels' Blub-koortje (batch "koortje"). The game test server has no dream dimension, so
 * the choir runs on five spots in a test garden: a Blub on someone else's hill only flattens its ears, on its own hill it
 * stays; with all five right the choir sings, and when the song is over the helper has Snoes' piece "het liedje".
 */
public class KoortjeGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "koortje";

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 100)
    public static void koortjeOpDeGoedeHeuvels(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        DroomVoortgang.reset(p);
        Map<String, Vec3> heuvels = new LinkedHashMap<>();
        double[][] plek = {{1.5, 1.5}, {7.5, 1.5}, {4.5, 4.5}, {1.5, 7.5}, {7.5, 7.5}};    // (more than a hill apart)
        for (int i = 0; i < BlubKoortje.NOTEN.size(); i++) {
            heuvels.put(BlubKoortje.NOTEN.get(i), helper.absoluteVec(new Vec3(plek[i][0], 2, plek[i][1])));
        }
        BlubKoortje.Koor koor = new BlubKoortje.Koor(heuvels, heuvels.get("mi"), 16);
        p.snapTo(heuvels.get("mi").x, heuvels.get("mi").y, heuvels.get("mi").z + 2, 0, 0);

        // the do Blub, led onto the re hill: wrong, its ears go flat and it is not on its spot
        BlubGuhEntity doBlub = blub(helper, "do", heuvels.get("re"));
        koor.volg(doBlub, p);
        koor.tick(helper.getLevel());
        helper.assertFalse(koor.opPlek(doBlub), "the do Blub on the re hill is not on its spot");
        helper.assertTrue(doBlub.emotie() == BlubGuhEntity.PLAT, "ears flat on the wrong hill");

        // on its own hill it stays (ears up)
        Vec3 d = heuvels.get("do");
        doBlub.snapTo(d.x, d.y, d.z, 0, 0);
        koor.tick(helper.getLevel());
        helper.assertTrue(koor.opPlek(doBlub), "the do Blub on the do hill stands right");
        helper.assertTrue(doBlub.emotie() == BlubGuhEntity.RECHTOP, "ears up on its own hill");
        helper.assertTrue(koor.zingt() == 0, "one of five: no song yet");

        // the other four, each on its own hill: the choir sings
        for (String n : BlubKoortje.NOTEN.subList(1, 5)) {
            BlubGuhEntity b = blub(helper, n, heuvels.get(n));
            koor.volg(b, p);
        }
        koor.tick(helper.getLevel());
        helper.assertTrue(koor.zingt() > 0, "all five right: the choir sings");
        helper.assertFalse(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.LIEDJE), "the piece only after the song");

        // the song is over: the helper has Snoes' piece, and the choir rests
        koor.afronden(helper.getLevel());
        helper.assertTrue(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.LIEDJE), "the helper found Snoes' song");
        helper.assertFalse(koor.opPlek(doBlub), "after the song they are free again");
        DroomVoortgang.reset(p);
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** A choir Blub: its note's tag and strikje. */
    static BlubGuhEntity blub(GameTestHelper helper, String noot, Vec3 waar) {
        BlubGuhEntity b = BlubFeature.BLUBGUH.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
        b.snapTo(waar.x, waar.y, waar.z, 0, 0);
        b.setNoAi(true);                                                // (stands still: the test moves it)
        helper.getLevel().addFreshEntity(b);
        BlubKoortje.maakKoorBlub(b, noot);
        helper.assertTrue(noot.equals(BlubKoortje.noot(b)), "tagged " + noot);
        return b;
    }
}
