package nl.juiced.guhs.feature.sjoelen;

import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * Guh-sjoelen: the physics of the bak (pure, no world), the points and the sjoelschijfjes (+1 per rule), Opoe's shop and
 * the clothing sources, the loaned pucks, the protection, and a whole turn in the real Sjoelhuisje.
 */
public class SjoelenGameTests {
    private static final String BATCH = "sjoelen_huisje";

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer p) {
        SjoelGame.leave(p);
        Minigames.forget(p);
        helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    private static int count(Player p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    private static int slideUntilStill(SjoelBak bak, SjoelBak.Puck puck) {
        int t = 0;
        while (!bak.allStill() && t < 2000) {
            bak.tick();
            t++;
        }
        return SjoelBak.gate(puck);
    }

    /** Straight slides into every gate, too weak stops short, the corner of a divider sends a puck off, pucks push each other. */
    @GuhTest(template = "empty")
    public static void sjoelenBakNatuurkunde(GameTestHelper helper) {
        for (int k = 0; k < 4; k++) {
            SjoelBak bak = new SjoelBak();
            double v = (SjoelBak.OPENINGS[k][0] + SjoelBak.OPENINGS[k][1]) / 2;
            SjoelBak.Puck p = bak.slide(v, SjoelBak.speed(0.75), 0);
            helper.assertTrue(slideUntilStill(bak, p) == k, "a good straight slide lands in gate " + k + ": " + p.u + " " + p.v);
        }
        SjoelBak weak = new SjoelBak();
        SjoelBak.Puck w = weak.slide(0.625, SjoelBak.speed(0.3), 0);
        helper.assertTrue(slideUntilStill(weak, w) == -1 && w.u < SjoelBak.BAR, "too soft stops on the bak: " + w.u);
        SjoelBak divider = new SjoelBak();
        SjoelBak.Puck d = divider.slide(2.5, SjoelBak.speed(0.75), 0);
        helper.assertTrue(slideUntilStill(divider, d) == -1 && d.u < SjoelBak.BAR, "straight into a divider bounces back: " + d.u);
        SjoelBak push = new SjoelBak();
        SjoelBak.Puck lying = push.place(10, 2.5);
        SjoelBak.Puck hitter = push.slide(2.5, SjoelBak.speed(0.85), 0);
        slideUntilStill(push, hitter);
        helper.assertTrue(lying.u > 15 && hitter.u < 11, "a puck pushes the one lying in its way: " + lying.u + " " + hitter.u);
        SjoelBak angled = new SjoelBak();
        SjoelBak.Puck a = angled.slide(2.5, SjoelBak.speed(0.75), SjoelBak.angle(-0.3));
        helper.assertTrue(slideUntilStill(angled, a) == 0, "aiming to the left reaches gate 2 (left): " + a.v);
        helper.assertTrue(SjoelBak.angle(5) == SjoelBak.MAX_ANGLE * SjoelBak.AIM, "the aim is limited");
        helper.succeed();
    }

    /** Sets of 2-3-4-1 are 20 points; the rest counts per gate; sjoelschijfjes: 1, +1 per set, +1 for a record. */
    @GuhTest(template = "empty")
    public static void sjoelenPuntenEnMunten(GameTestHelper helper) {
        helper.assertTrue(SjoelBak.score(new int[] {5, 5, 5, 5}) == 100, "five sets: 100");
        helper.assertTrue(SjoelBak.score(new int[] {3, 2, 2, 6}) == 46, "2 sets (40) + 1x2 + 4x1 = 46");
        helper.assertTrue(SjoelBak.score(new int[] {0, 0, 0, 20}) == 20, "all in gate 1: 20");
        helper.assertTrue(SjoelBak.score(new int[] {0, 0, 0, 0}) == 0 && SjoelBak.sets(new int[] {1, 0, 3, 3}) == 0, "nothing");
        helper.assertTrue(SjoelGame.munten(0, false) == 1, "a turn: 1");
        for (int s = 0; s < 5; s++) {
            helper.assertTrue(SjoelGame.munten(s + 1, false) == SjoelGame.munten(s, false) + 1, "+1 per set");
        }
        helper.assertTrue(SjoelGame.munten(3, true) == SjoelGame.munten(3, false) + 1, "+1 for a record");
        helper.assertTrue(SjoelSchijvenItem.power(0) < 0.1f && Math.abs(SjoelSchijvenItem.power(SjoelSchijvenItem.CYCLE / 2) - 1f) < 1e-4
                && SjoelSchijvenItem.power(SjoelSchijvenItem.CYCLE) < 0.1f, "the power bar goes up and down");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void sjoelenWinkelEnBronnen(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.SJOELGUH);
        var offers = npc.getOffers();
        var results = offers.stream().map(o -> o.getResult().getItem()).toList();
        for (GuhClothes piece : SjoelenFeature.KLEDING) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "Opoe sells " + piece);
            helper.assertTrue("sjoelen".equals(KledingBronnen.bron(piece)) && KledingBronnen.prijs(piece) != null, "one source: sjoelen, " + piece);
        }
        helper.assertTrue(offers.size() == 3 && offers.stream().allMatch(o -> o.getCostA().is(SjoelenFeature.SJOELSCHIJFJE.get())), "for sjoelschijfjes");
        helper.assertTrue(offers.stream().mapToInt(o -> o.getCostA().getCount()).sorted().boxed().toList().equals(List.of(4, 6, 10)),
                "petje 4, broche 6, vestje 10");
        helper.assertTrue(GuhClothes.SJOELEN_PETJE.slot == GuhClothes.Slot.HEAD && GuhClothes.SJOELEN_VESTJE.slot == GuhClothes.Slot.BODY
                && GuhClothes.SJOELEN_BROCHE.slot == GuhClothes.Slot.NECK, "the slots");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void sjoelenSchijvenBlijvenGeleend(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ItemStack pucks = new ItemStack(SjoelenFeature.SCHIJVEN.get(), 20);
        helper.assertTrue(Features.isLoaned(pucks), "the pucks are loaned (tag guhs:loaned)");
        helper.assertTrue(!pucks.getItem().onDroppedByPlayer(pucks, p) && !pucks.getItem().canFitInsideContainerItems(), "can't be dropped or bagged");
        p.getInventory().add(pucks);
        p.getInventory().tick();
        helper.assertTrue(count(p, SjoelenFeature.SCHIJVEN.get()) == 0, "someone who isn't sjoeling loses them at once");
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void sjoelenBescherming(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.OAK_PLANKS);
        BlockPos abs = helper.absolutePos(floor);
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(4, 4, 4)));
        SjoelenProtection.TEST_AREAS.add(box);
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(Protected.at(helper.getLevel(), abs), "the Sjoelhuisje is protected");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), p)).isCanceled(),
                    "survival players can't break it");
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), p)).isCanceled(),
                    "creative may");
            helper.assertTrue(!SjoelenProtection.inHuisje(helper.getLevel(), abs.offset(20, 0, 0)), "outside it isn't");
        } finally {
            SjoelenProtection.TEST_AREAS.remove(box);
            leave(helper, p);
        }
        helper.succeed();
    }

    /** A whole turn at Opoe's: 20 loaned pucks, a real slide into gate 4, the rest landed, Opoe counts: 100! */
    @GuhTest(template = "sjoelhuisje", timeoutTicks = 500, batch = BATCH)
    public static void sjoelenBeurtInHetHuisje(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(), n -> n.getKind() == GuhNpcEntity.Kind.SJOELGUH);
        helper.assertTrue(npcs.size() == 1, "Opoe Njegschuif is in her Sjoelhuisje: " + npcs.size());
        GuhNpcEntity npc = npcs.get(0);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        SjoelGame.action(npc, p, SjoelGame.START);
        SjoelGame game = SjoelGame.of(npc);
        helper.assertTrue(game.isPlayedBy(p) && SjoelGame.isPlaying(p) && game.kop() != null, "the turn is on, the bak found");
        helper.assertTrue(count(p, SjoelenFeature.SCHIJVEN.get()) == 20, "twenty loaned pucks");
        ServerPlayer other = player(helper);
        other.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1);
        SjoelGame.action(npc, other, SjoelGame.START);
        helper.assertTrue(!SjoelGame.isPlaying(other), "one player at a time");
        leave(helper, other);
        game.testSkipCountdown();
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(game.phase() == SjoelGame.Phase.PLAYING, "the countdown is over: " + game.phase());
            Vec3 spot = game.world(-1.2, 3.125);
            p.snapTo(spot.x, spot.y - 1, spot.z, game.facing().toYRot(), 10);
            helper.assertTrue(game.slideWith(p, 0.75f, 0), "slid");
            helper.assertTrue(game.thrown() == 1 && count(p, SjoelenFeature.SCHIJVEN.get()) == 19, "one puck gone from the stack");
            helper.assertTrue(game.testEntities(helper.getLevel()).size() == 1, "a real puck entity on the bak");
        });
        helper.runAfterDelay(26, () -> {
            var e = game.testEntities(helper.getLevel()).get(0);
            double[] at = game.local(e.position());
            helper.assertTrue(at[0] > 5, "the puck slides along the bak: u = " + at[0]);
        });
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(game.bak().allStill() && game.bak().counts()[2] == 1, "it slid into gate 4: " + java.util.Arrays.toString(game.bak().counts()));
            game.testLand(npc, new int[] {5, 5, 4, 5});
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(game.phase() == SjoelGame.Phase.TALLY, "Opoe counts: " + game.phase());
            helper.assertTrue(SjoelGame.best(p) == 100, "100 points: " + SjoelGame.best(p));
            helper.assertTrue(count(p, SjoelenFeature.SJOELSCHIJFJE.get()) == 6, "1 + 5 sets = 6 sjoelschijfjes: " + count(p, SjoelenFeature.SJOELSCHIJFJE.get()));
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == 16 && count(p, SjoelenFeature.STAPEL_ITEM.get()) == 1, "and a present");
            helper.assertTrue(count(p, SjoelenFeature.SCHIJVEN.get()) == 0, "the pucks went back");
            helper.assertTrue(SjoelenFeature.has(p, "sjoelen_gespeeld") && SjoelenFeature.has(p, "sjoelen_honderd"), "advancements");
            helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), SjoelGame.BOARD).stream().anyMatch(en -> en.player().equals(p.getUUID())
                    && en.score() == 100), "on the house top 3");
            helper.assertTrue(GuhQuests.saved(p).getIntOr(SjoelGame.GAMES_KEY, 0) == 1, "one turn counted");
        });
        helper.runAfterDelay(150 + SjoelGame.TALLY_TICKS + 5, () -> {
            helper.assertTrue(!game.isRunning() && !SjoelGame.isPlaying(p), "the turn is over");
            helper.assertTrue(game.testEntities(helper.getLevel()).isEmpty(), "the pucks are gone");
            helper.assertTrue(p.distanceTo(npc) < 5, "and you're back next to Opoe");
            leave(helper, p);
            helper.succeed();
        });
    }
}
