package nl.juiced.guhs.feature.dirigent;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.zingendeweide.ZingendeWeideFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/** 1.8.0 uitbreiding: the Dirigent in de Koorkom (judging the beats, the stars, the once-a-day prize). */
public class DirigentGameTests {
    private static final String BATCH = "dirigent", TUIN = "landdiertjes_test_tuin";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlubSnoes.zet(p, true);
        GuhQuests.saved(p).remove(Concert.RECORD);
        GuhQuests.saved(p).remove(Concert.DAG);
        GuhQuests.saved(p).remove(Concert.GOUD_OOIT);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    /** A podium with five wild Blubs around it, and a concert that skips straight to the playing. */
    static Concert concert(GameTestHelper helper, ServerPlayer p) {
        BlockPos podium = helper.absolutePos(new BlockPos(3, 1, 4));
        helper.getLevel().setBlockAndUpdate(podium, ZingendeWeideFeature.KOORKOM_PODIUM.get().defaultBlockState());
        List<BlubGuhEntity> blubs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BlubGuhEntity b = helper.spawn(BlubFeature.BLUBGUH.get(), new BlockPos(1 + i, 1, 7));
            b.zetWaarden(new BlubTrekking.Waarden(30, BlubTrekking.Soort.BLAUW, 0, 1f));
            blubs.add(b);
        }
        Concert c = Concert.begin(p, podium);
        helper.assertTrue(c != null, "the concert starts");
        helper.assertTrue(Concert.bezig(helper.getLevel(), podium), "the podium is busy (its dusk song waits)");
        helper.assertTrue(c.blubs.size() == 5, "five singers");
        c.speelNu();
        return c;
    }

    static int tel(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Right on the beat at the right Blub: 2; a bit off: 1; the wrong Blub: 0; and a beat is judged once. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void dirigentBeoordeeltDeTellen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Concert c = concert(helper, p);
        c.zetTick(c.beatTick(0));
        p.lookAt(EntityAnchorArgument.Anchor.EYES, c.zanger(0).position().add(0, 0.3, 0));
        c.tik(p);
        helper.assertTrue(c.punten() == 2, "perfect: 2 points (got " + c.punten() + ")");
        c.tik(p);
        helper.assertTrue(c.punten() == 2, "a beat counts once");
        c.zetTick(c.beatTick(1) + 4);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, c.zanger(1).position().add(0, 0.3, 0));
        c.tik(p);
        helper.assertTrue(c.punten() == 3, "a little off: 1 point (got " + c.punten() + ")");
        c.zetTick(c.beatTick(2));
        p.lookAt(EntityAnchorArgument.Anchor.EYES, c.zanger(0).position().add(0, 0.3, 0));   // beat 2 is another Blub
        helper.assertTrue(c.zanger(2) != c.zanger(0), "beat 2 belongs to another Blub");
        c.tik(p);
        helper.assertTrue(c.punten() == 3, "the wrong Blub: nothing");
        Concert.wisAlles();
        helper.succeed();
    }

    /** All perfect: three stars, droomzand, the day's big prize once; the next concert that day only droomzand. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void dirigentDrieSterrenEnEenKeerPerDag(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Concert c = concert(helper, p);
        for (int i = 0; i < Concert.MAAT; i++) {
            c.beoordeeld[i] = true;
            c.raak(i, 2);
        }
        helper.assertTrue(Concert.sterren(c.punten()) == 3, "all perfect: three stars");
        c.eindeNu(p);
        Item zand = DroomblokkenFeature.DROOMZAND.get();
        int na1 = tel(p.getInventory(), zand);
        helper.assertTrue(na1 >= 3, "droomzand for the stars (got " + na1 + ")");
        helper.assertTrue(GuhQuests.saved(p).getIntOr(Concert.RECORD, 0) == Concert.MAX, "the record is kept");
        Concert.wisAlles();
        p.getInventory().clearContent();
        GuhQuests.saved(p).remove(Concert.GOUD_OOIT);
        Concert c2 = concert(helper, p);
        for (int i = 0; i < Concert.MAAT; i++) {
            c2.beoordeeld[i] = true;
            c2.raak(i, 2);
        }
        c2.eindeNu(p);
        helper.assertTrue(tel(p.getInventory(), zand) == 3, "the same day: only the stars' droomzand (got "
                + tel(p.getInventory(), zand) + ")");
        helper.assertTrue(tel(p.getInventory(), DroomblokkenFeature.MAANBLOEMETJE.get().asItem()) == 0, "and no big prize");
        Concert.wisAlles();
        helper.succeed();
    }
}
