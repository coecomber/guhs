package nl.juiced.guhs.feature.einde2.ballon;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.einde2.vetbootje.Heteluchtballon;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase C, slice trap5_ballon: the core of the Vredesballon (CONTRACT_E2_B 8: few, focused).
 * <ul>
 *   <li>{@link #einde2BallonLijn}: two players; the gate; handing in by portions (only what counts, only up to the amount,
 *       only for the player whose stage it is); a stage complete = a step on and a bigger balloon; the launch refuses
 *       outside the temple; landing gives the rewards once; the bag of the day once a day; the board and the scene.</li>
 *   <li>{@link #einde2BallonLagen}: the five overlays of the building site are read, grow cumulatively, and in the air only
 *       the yard's gear stays.</li>
 * </ul>
 */
public class E2BallonGameTests {
    private static final String BATCH = "einde2_trap5_ballon";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BallonLijn(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        try {
            Vredesballon.LIJN.wis(p);
            Vredesballon.LIJN.wis(q);
            // the gate: closed for a new player; open after the Werktuigguh's line (here: as the dev command opens it)
            helper.assertTrue(!Vredesballon.magBeginnen(p) && !Vredesballon.magBeginnen(q), "closed for new players");
            BallonCommando.ervoor(p);
            helper.assertTrue(Vredesballon.magBeginnen(p) && !Vredesballon.magBeginnen(q), "open for p only");
            helper.assertTrue(Vredesballon.LIJN.verder(p, 0) && Vredesballon.fase(p) == 1 && Vredesballon.laag(p) == 0 && Vredesballon.LIJN.stap(q) == 0,
                    "begun: stage 1, nothing on the site yet");
            // stage 1 by portions: the tent canvas of any colour counts, its stairs do not; never more than asked
            Item rood = item("guhs:campingmarkt_tentdoek_rood"), trap = item("guhs:campingmarkt_tentdoek_rood_trap"), draad = item("guhs:guh_wire");
            p.getInventory().add(new ItemStack(rood, 64));
            p.getInventory().add(new ItemStack(rood, 36));
            p.getInventory().add(new ItemStack(trap, 10));
            p.getInventory().add(new ItemStack(draad, 64));
            q.getInventory().add(new ItemStack(rood, 64));
            Vredesballon.Uitkomst uit = Vredesballon.lever(p);
            Vredesballon.Levering doek = Vredesballon.leveringen(1).get(0), naad = Vredesballon.leveringen(1).get(1);
            helper.assertTrue(uit.genomen() == 164 && !uit.faseKlaar() && Vredesballon.geleverd(p, 1, doek) == 100 && Vredesballon.geleverd(p, 1, naad) == 64
                    && tel(p, trap) == 10 && tel(p, rood) == 0 && Vredesballon.LIJN.stap(p) == 1, "a first portion: " + uit);
            helper.assertTrue(Vredesballon.lever(q).genomen() == 0 && tel(q, rood) == 64 && Vredesballon.geleverd(q, 1, doek) == 0,
                    "q has not begun: nothing is taken from q");
            p.getInventory().add(new ItemStack(item("guhs:campingmarkt_tentdoek_blauw"), 40));
            uit = Vredesballon.lever(p);
            helper.assertTrue(uit.genomen() == 28 && uit.faseKlaar() && Vredesballon.LIJN.stap(p) == 2 && Vredesballon.laag(p) == 1
                    && tel(p, item("guhs:campingmarkt_tentdoek_blauw")) == 12, "the cloth is complete: stage 2, the first overlay: " + uit);
            helper.assertTrue(Vredesballon.nodig(p, 2).size() == 2 && Vredesballon.nodig(p, 0).isEmpty(), "the Guhdex shows what stage 2 needs");
            // the burner: the things of trap5_machines count only when they exist in this build
            boolean machines = BuiltInRegistries.ITEM.containsKey(Identifier.parse("guhs:vuurkorf"));
            helper.assertTrue(Vredesballon.leveringen(3).size() == (machines ? 4 : 2) && Vredesballon.FASEN.stream().mapToInt(java.util.List::size).sum() == 12,
                    "the stages and the burner's machines");
            // ready: the launch only plays in the temple; landing gives the rewards once
            Vredesballon.LIJN.zet(p, Vredesballon.KLAAR);
            helper.assertTrue(Vredesballon.laag(p) == 5 && !Vredesballon.LIJN.klaar(p) && !Vredesballon.lanceer(p) && !Vredesballon.OPGESTEGEN.contains(p.getUUID()),
                    "ready, but no launch outside the Guheinde");
            Vredesballon.geland(p);
            Vredesballon.geland(p);
            Titels.Titel titel = Titels.van(Vredesballon.TITEL);
            helper.assertTrue(Vredesballon.LIJN.klaar(p) && tel(p, E2BallonFeature.VREDESBALLON_ITEM.get()) == 1 && Heteluchtballon.heeft(p)
                    && titel != null && Titels.heeft(p, titel) && !Titels.heeft(q, titel), "landed: one statuette, the balloon, the title (only p)");
            // the bag of the day
            Item knabbel = item("guhs:kaas_knabbels");
            int voor = tel(p, knabbel);
            helper.assertTrue(Vredesballon.zakjeKlaar(p) && Vredesballon.zakje(p) && tel(p, knabbel) == voor + Vredesballon.ZAKJE
                    && !Vredesballon.zakjeKlaar(p) && !Vredesballon.zakje(p) && !Vredesballon.zakjeKlaar(q), "one bag a day");
            // the board: its frame, and its text follows the step
            BlockPos t = new BlockPos(100, 50, 100);
            helper.assertTrue(BallonBord.isBord(t, t.offset(3, 3, -1)) && BallonBord.isBord(t, t.offset(-3, 0, -1)) && !BallonBord.isBord(t, t)
                    && !BallonBord.isBord(t, t.offset(4, 1, -1)) && !BallonBord.isBord(t, t.offset(0, 4, -1)), "the board's frame");
            helper.assertTrue(!BallonBord.tekst(p).getString().equals(BallonBord.tekst(q).getString()), "each player reads their own board");
            // the scene: 43.5 s running time, ten spoken lines that wait, nothing that runs on
            var s = BallonScene.VREDESBALLON;
            helper.assertTrue(s.duur() == 870 && s.pauzes().length == 10 && s.zinnen().stream().noneMatch(z -> z.lied()) && s.lijn().equals(Vredesballon.ID),
                    "the scene: " + s.duur() + " ticks, " + s.pauzes().length + " pauses");
        } finally {
            Vredesballon.LIJN.wis(p);
            Vredesballon.LIJN.wis(q);
            GuhQuests.saved(p).remove(Vredesballon.DEV_VRIJ);
            GuhQuests.saved(p).remove(Vredesballon.DAG);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(q, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BallonLagen(GameTestHelper helper) {
        BallonLagen.vergeetBlokken();
        Map<BlockPos, BlockState> vorige = Map.of();
        for (int n = 1; n <= Vredesballon.AANTAL_FASEN; n++) {
            Map<BlockPos, BlockState> laag = BallonLagen.blokken(helper.getLevel(), n);
            final Map<BlockPos, BlockState> eerder = vorige;
            helper.assertTrue(laag.size() > eerder.size() && laag.keySet().containsAll(eerder.keySet()), "stage " + n + " holds stage " + (n - 1)
                    + " and more: " + laag.size() + " blocks");
            helper.assertTrue(laag.keySet().stream().allMatch(b -> b.getX() >= 0 && b.getX() < BallonLagen.SX && b.getY() >= 0 && b.getY() < BallonLagen.SY
                    && b.getZ() >= 0 && b.getZ() < BallonLagen.SZ), "stage " + n + " inside its 25 x 42 x 25");
            vorige = laag;
        }
        long blijft = vorige.keySet().stream().filter(BallonLagen::blijftStaan).count();
        helper.assertTrue(blijft > 100 && blijft < vorige.size() / 2, "in the air only the yard's gear stays: " + blijft + " of " + vorige.size());
        helper.assertTrue(BallonLagen.blijftStaan(new BlockPos(1, 20, 1)) && BallonLagen.blijftStaan(new BlockPos(5, 0, 20))
                && BallonLagen.blijftStaan(new BlockPos(5, 2, 5)) && BallonLagen.blijftStaan(new BlockPos(12, 39, 1))
                && !BallonLagen.blijftStaan(new BlockPos(12, 2, 12)) && !BallonLagen.blijftStaan(new BlockPos(12, 26, 3))
                && !BallonLagen.blijftStaan(new BlockPos(16, 4, 11)), "masts, ground and frame stay; basket and cloth go up");
        helper.succeed();
    }
}
