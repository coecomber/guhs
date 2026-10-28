package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.BitSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the guhpixel slice "gezel" (batch px_gezel; run with {@code -Pgt=px_gezel}): the win rules and the bot of
 * Guh op een rij, the flipping down (wegstrepen) and guessing of Wie is het, and the pillow push of the Kussengevecht
 * (plus a round in its own arena: the bed, the bots that fill up).
 */
public class PxGezelGameTests {
    private static final String BATCH = "px_gezel";
    private static final String KLEIN = "px_test_16", GROOT = "px_test_48";

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void opEenRijWinregels(GameTestHelper helper) {
        OpEenRij b = new OpEenRij();
        for (int k = 0; k < 3; k++) {
            b.laat(k, OpEenRij.GUH);
            helper.assertTrue(b.winnaar() == OpEenRij.LEEG, "three is not four");
        }
        b.laat(3, OpEenRij.GUH);
        helper.assertTrue(b.winnaar() == OpEenRij.GUH, "four guhs in a row win");
        // up
        b = new OpEenRij();
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(b.laat(6, OpEenRij.BLUB) == i, "a disc falls onto the one below it");
        }
        helper.assertTrue(b.winnaar() == OpEenRij.BLUB, "four Blubs up win");
        // diagonal: a staircase
        b = new OpEenRij();
        int[][] zetten = {{0, 1}, {1, 2}, {1, 1}, {2, 2}, {2, 2}, {2, 1}, {3, 2}, {3, 2}, {3, 2}, {3, 1}};
        for (int[] z : zetten) {
            b.laat(z[0], (byte) z[1]);
        }
        helper.assertTrue(b.winnaar() == OpEenRij.GUH, "a diagonal of guhs wins");
        // a full column refuses
        b = new OpEenRij();
        for (int i = 0; i < OpEenRij.HOOG; i++) {
            b.laat(0, i % 2 == 0 ? OpEenRij.GUH : OpEenRij.BLUB);
        }
        helper.assertTrue(b.laat(0, OpEenRij.GUH) == -1 && !b.kan(0), "a full column takes no more discs");
        // the bot: wins when it can, blocks when it must (normal and hard)
        RandomSource rnd = RandomSource.create(7);
        b = new OpEenRij();
        b.laat(0, OpEenRij.BLUB);
        b.laat(1, OpEenRij.BLUB);
        b.laat(2, OpEenRij.BLUB);
        for (int niveau = 0; niveau < 3; niveau++) {
            helper.assertTrue(b.botZet(OpEenRij.BLUB, niveau, rnd) == 3, "the bot takes its win (level " + niveau + ")");
        }
        b = new OpEenRij();
        b.laat(2, OpEenRij.GUH);
        b.laat(3, OpEenRij.GUH);
        b.laat(4, OpEenRij.GUH);
        int blok = b.botZet(OpEenRij.BLUB, OpEenRij.NORMAAL, rnd);
        helper.assertTrue(blok == 1 || blok == 5, "the normal bot blocks an open three, got " + blok);
        helper.assertTrue(b.botZet(OpEenRij.BLUB, OpEenRij.LASTIG, rnd) >= 0, "the hard bot finds a move");
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void wieIsHetWegstrepen(GameTestHelper helper) {
        List<WieIsHet.Kaart> bord = WieIsHet.bord(42);
        helper.assertTrue(bord.size() == WieIsHet.KAARTEN, "a board of " + WieIsHet.KAARTEN + " guhs");
        helper.assertTrue(bord.stream().map(WieIsHet.Kaart::naam).distinct().count() == WieIsHet.KAARTEN, "every guh has its own name");
        WieIsHet.Kaart geheim = bord.get(5);
        WieIsHet.Vraag kleur = new WieIsHet.Vraag("kleur", geheim.kleur());
        BitSet ja = WieIsHet.wegstrepen(bord, WieIsHet.alles(), kleur, true);
        BitSet nee = WieIsHet.wegstrepen(bord, WieIsHet.alles(), kleur, false);
        helper.assertTrue(ja.get(5) && !nee.get(5) && ja.cardinality() + nee.cardinality() == WieIsHet.KAARTEN, "yes keeps the secret guh, no flips it");
        for (int i = ja.nextSetBit(0); i >= 0; i = ja.nextSetBit(i + 1)) {
            helper.assertTrue(bord.get(i).kleur() == geheim.kleur(), "after 'yes' only guhs of that colour stay up");
        }
        // a whole game against the bot, without a world: asking flips down, a right guess wins
        BordSpel.Partij partij = new BordSpel.Partij(BordSpel.WIEISHET, java.util.UUID.randomUUID(), "Tester", null, "Spelguh", 1,
                RandomSource.create(3));
        int voor = partij.openA.cardinality();
        helper.assertTrue(BordSpel.vraag(partij, 0, 0, null), "the player may ask first");
        helper.assertTrue(partij.openA.cardinality() <= voor && partij.openA.get(partij.geheimB), "the other's guh always stays up");
        helper.assertTrue(!BordSpel.vraag(partij, 0, 1, null), "not twice in a row");
        partij.beurt = 0;
        helper.assertTrue(BordSpel.raad(partij, 0, partij.geheimB, null) && partij.uitslag == 0, "the right guess wins");
        // the bot's question splits its cards
        int v = WieIsHet.botVraag(bord, WieIsHet.alles(), RandomSource.create(1));
        int n = WieIsHet.wegstrepen(bord, WieIsHet.alles(), WieIsHet.VRAGEN.get(v), true).cardinality();
        helper.assertTrue(v >= 0 && n > 0 && n < WieIsHet.KAARTEN, "the bot asks a question that splits the board");
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void kussenDuwt(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        Vec3 a = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 1, 8)));
        Vec3 b = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 8)));
        GuhNpcEntity van = KussenSessie.maakBot(level, a, 0);
        GuhNpcEntity naar = KussenSessie.maakBot(level, b, 0);
        float hp = naar.getHealth();
        KussenSessie.duw(van, naar);
        helper.assertTrue(naar.getDeltaMovement().x > 0.3 && naar.getDeltaMovement().y > 0.2, "the pillow pushes away and a little up: "
                + naar.getDeltaMovement());
        helper.assertTrue(naar.getHealth() == hp, "a pillow never hurts");
        van.discard();
        naar.discard();
        helper.succeed();
    }

    @GuhTest(template = GROOT, batch = BATCH, timeoutTicks = 200)
    public static void kussengevechtRonde(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        Sessie sessie = Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), KussenSessie.SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(sessie instanceof KussenSessie, "the pillow fight starts");
        KussenSessie s = (KussenSessie) sessie;
        BlockPos bed = s.arena().wereld(15, KussenSessie.BED_Y, 15);
        helper.assertTrue(!level.getBlockState(bed).isAir(), "the bed is there at local y " + KussenSessie.BED_Y);
        long bots = level.getEntitiesOfClass(GuhNpcEntity.class, s.arena().doos(), e -> e.entityTags().contains(KussenSessie.BOT_TAG)).size();
        helper.assertTrue(bots == KussenSessie.MIN_DEELNEMERS - 1, "guh-bots fill the bed up to " + KussenSessie.MIN_DEELNEMERS + ", got " + bots);
        helper.assertTrue(p.getInventory().contains(new net.minecraft.world.item.ItemStack(GezelSlice.KUSSEN.get())), "a pillow in the pockets");
        helper.assertTrue(s.opBed(p), "the player stands on the bed");
        PxTest.klaar(helper, p);
        helper.assertTrue(level.getEntitiesOfClass(GuhNpcEntity.class, s.arena().doos(), e -> e.entityTags().contains(KussenSessie.BOT_TAG)).isEmpty(),
                "the bots are gone after the game");
        helper.succeed();
    }
}
