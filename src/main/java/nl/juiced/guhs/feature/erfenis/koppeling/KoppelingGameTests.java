package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import nl.juiced.guhs.feature.bio.Bezocht;
import nl.juiced.guhs.feature.erfenis.ErfenisMomenten;
import nl.juiced.guhs.feature.erfenis.ErfenisVadskracht;
import nl.juiced.guhs.feature.erfenis.LeverkistPost;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;

/** Koppeling: the letter's clock and the buy, the Leverkist queue (order, overflow, hoppers), the cap, the Dorpszender, the hekje. */
public class KoppelingGameTests {
    private static final String BATCH = "erfenis_koppeling";
    private static final Set<String> MOMENTEN = ConcurrentHashMap.newKeySet();
    private static boolean luistert;

    private static Leverkist.BlockEntityImpl kist(GameTestHelper helper, BlockPos pos, UUID eigenaar) {
        helper.setBlock(pos, KoppelingFeature.LEVERKIST.get());
        Leverkist.BlockEntityImpl k = (Leverkist.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        k.zetEigenaar(eigenaar);
        return k;
    }

    private static int tel(Leverkist.BlockEntityImpl k) {
        int n = 0;
        for (int i = 0; i < k.getContainerSize(); i++) {
            n += k.getItem(i).getCount();
        }
        return n;
    }

    /** 40 stacks into one chest: 27 fit, the flag goes up, 13 wait; a second chest gets the rest; the first one first. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void leverkistVolgordeEnWachten(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        UUID wie = UUID.randomUUID();
        Leverkist.BlockEntityImpl a = kist(helper, new BlockPos(1, 2, 1), wie);
        helper.assertTrue(!helper.getBlockState(new BlockPos(1, 2, 1)).getValue(Leverkist.POST), "an empty chest: flag down");
        List<ItemStack> spul = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            spul.add(new ItemStack(Items.WHITE_WOOL, 64));
        }
        LeverkistPost.verstuur(s, wie, spul);
        helper.assertTrue(tel(a) == 27 * 64, "the first chest is full: " + tel(a));
        helper.assertTrue(LeverkistPost.wacht(s, wie) == 13 * 64, "13 stacks wait: " + LeverkistPost.wacht(s, wie));
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 1)).getValue(Leverkist.POST), "post inside: flag up");
        Leverkist.BlockEntityImpl b = kist(helper, new BlockPos(3, 2, 1), wie);
        Leverkisten.bezorg(s, wie);
        helper.assertTrue(tel(b) == 13 * 64 && LeverkistPost.wacht(s, wie) == 0, "the second chest got the rest");
        helper.assertTrue(Leverkisten.kisten(s, wie).size() == 2, "two chests on the list");
        // merging: a half stack goes onto the same item first
        a.setItem(0, new ItemStack(Items.OAK_LOG, 10));
        a.setItem(1, ItemStack.EMPTY);
        LeverkistPost.verstuur(s, wie, List.of(new ItemStack(Items.OAK_LOG, 60)));
        helper.assertTrue(a.getItem(0).getCount() == 64 && a.getItem(1).getCount() == 6, "onto the same logs first, then an empty slot");
        // a removed chest goes off the list
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.AIR);
        helper.assertTrue(Leverkisten.kisten(s, wie).size() == 1, "the broken chest is off the list");
        helper.succeed();
    }

    /** A hopper under the Leverkist takes the post out. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void leverkistHopper(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        UUID wie = UUID.randomUUID();
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        Leverkist.BlockEntityImpl k = kist(helper, new BlockPos(1, 2, 1), wie);
        LeverkistPost.verstuur(s, wie, List.of(new ItemStack(ModItems.KAAS_KNABBELS.get(), 3)));
        helper.assertTrue(tel(k) == 3, "the post is in the chest");
        helper.succeedWhen(() -> {
            HopperBlockEntity h = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
            int n = 0;
            for (int i = 0; i < h.getContainerSize(); i++) {
                n += h.getItem(i).getCount();
            }
            helper.assertTrue(n == 3 && tel(k) == 0, "the hopper took the knabbels: " + n);
        });
    }

    /** The cap: logs 150/h, saved up to 8 h (1200), starts full; wool and eggs never capped; an hour later 150 more. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void leverkistPlafond(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        UUID wie = UUID.randomUUID();
        long t0 = 5_000_000L;
        helper.assertTrue("stammen".equals(LeverkistPlafond.soort(new ItemStack(Items.OAK_LOG))), "logs are stammen");
        helper.assertTrue(LeverkistPlafond.soort(new ItemStack(Items.WHITE_WOOL)) == null, "wool has no cap");
        helper.assertTrue("glas".equals(LeverkistPlafond.soort(new ItemStack(KoppelingFeature.GLASSCHERF.get()))), "glasscherven are glass");
        LeverkistPlafond.Beperkt een = LeverkistPlafond.beperk(s, wie, List.of(new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.WHITE_WOOL, 64)), t0);
        helper.assertTrue(een.mee().size() == 2 && een.blijft().isEmpty(), "a little goes at once");
        List<ItemStack> veel = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            veel.add(new ItemStack(Items.OAK_LOG, 64));
        }
        veel.add(new ItemStack(Items.EGG, 16));
        LeverkistPlafond.Beperkt twee = LeverkistPlafond.beperk(s, wie, veel, t0);
        int mee = twee.mee().stream().filter(x -> x.is(Items.OAK_LOG)).mapToInt(ItemStack::getCount).sum();
        int blijft = twee.blijft().stream().mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(mee == 1200 - 64, "the rest of 8 hours goes: " + mee);
        helper.assertTrue(blijft == 20 * 64 - mee, "the rest stays in the village: " + blijft);
        helper.assertTrue(twee.mee().stream().anyMatch(x -> x.is(Items.EGG) && x.getCount() == 16), "eggs always go");
        helper.assertTrue(LeverkistPlafond.ruimte(s, wie, new ItemStack(Items.OAK_LOG, 64), t0) == 0, "nothing left now");
        helper.assertTrue(LeverkistPlafond.ruimte(s, wie, new ItemStack(Items.OAK_LOG, 64), t0 + 3_600_000L) == 64, "an hour later: room again");
        helper.assertTrue(Math.abs(LeverkistPlafond.tegoed(s, wie, "stammen", t0 + 3_600_000L) - 150) < 0.01, "150 per hour");
        helper.assertTrue(Math.abs(LeverkistPlafond.tegoed(s, wie, "stammen", t0 + 100 * 3_600_000L) - 1200) < 0.01, "never above 8 hours");
        helper.succeed();
    }

    /** The Dorpszender on a test source: 100 VK sent = 75 in the village; too much for the net = nothing; two add up. */
    @GuhTest(template = "vadskracht_test_kamer", batch = BATCH, timeoutTicks = 200)
    public static void dorpszenderVerlies(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        UUID wie = UUID.randomUUID();
        helper.setBlock(new BlockPos(1, 2, 1), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 15));   // 150 VK
        helper.setBlock(new BlockPos(2, 2, 1), ModBlocks.GUH_WIRE.get());
        helper.setBlock(new BlockPos(3, 2, 1), KoppelingFeature.DORPSZENDER.get());
        Dorpszender.BlockEntityImpl z = (Dorpszender.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 2, 1)));
        z.zetEigenaar(wie);
        z.zetVermogen(100);
        helper.assertTrue(Dorpszender.aankomst(100) == 75 && Dorpszender.aankomst(40) == 30, "25% loss");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ErfenisVadskracht.huidig().beschikbaar(s, wie) == 75, "100 sent, 75 arrive: "
                        + ErfenisVadskracht.huidig().beschikbaar(s, wie)))
                .thenExecute(() -> z.zetVermogen(200))
                .thenWaitUntil(() -> helper.assertTrue(ErfenisVadskracht.huidig().beschikbaar(s, wie) == 0, "200 is too much for 150: nothing"))
                .thenExecute(() -> {
                    z.zetVermogen(80);
                    helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.GUH_WIRE.get());
                    helper.setBlock(new BlockPos(2, 2, 3), ModBlocks.GUH_WIRE.get());
                    helper.setBlock(new BlockPos(3, 2, 3), KoppelingFeature.DORPSZENDER.get());
                    Dorpszender.BlockEntityImpl z2 = (Dorpszender.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 2, 3)));
                    z2.zetEigenaar(wie);
                    z2.zetVermogen(40);
                })
                .thenWaitUntil(() -> helper.assertTrue(ErfenisVadskracht.huidig().beschikbaar(s, wie) == 60 + 30, "two zenders add up: "
                        + ErfenisVadskracht.huidig().beschikbaar(s, wie)))
                .thenExecute(() -> helper.setBlock(new BlockPos(3, 2, 3), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(ErfenisVadskracht.huidig().beschikbaar(s, wie) == 60, "a broken zender stops counting"))
                .thenSucceed();
    }

    /** The clock starts at the first Guhmension visit, the Postguh brings the letter after 30 minutes, buying = hekje + moment. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void briefKlokEnKoop(GameTestHelper helper) {
        if (!luistert) {
            luistert = true;
            ErfenisMomenten.luister((p, m) -> MOMENTEN.add(p.getUUID() + "/" + m));
        }
        ServerPlayer p = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        KoppelingData d = KoppelingData.get(s);
        helper.assertTrue(!Makelaarsbrief.klok(p, d, 20 * 60 * 40) && Makelaarsbrief.ticks(s, p.getUUID()) == 0, "no clock before the Guhmension");
        Bezocht.zetDimensie(p, "guhmension");
        helper.assertTrue(!Makelaarsbrief.klok(p, d, Makelaarsbrief.WACHTTIJD - 20), "29:59 counted, no letter yet");
        helper.assertTrue(Makelaarsbrief.ticks(s, p.getUUID()) == Makelaarsbrief.WACHTTIJD - 20, "the clock counts");
        helper.assertTrue(!Makelaarsbrief.klok(p, d, 20), "the last second is counted");
        helper.assertTrue(Makelaarsbrief.klok(p, d, 20), "30 minutes: the Postguh comes");
        helper.assertTrue(Makelaarsbrief.heeftBrief(p) && Makelaarsbrief.staat(s, p.getUUID()) == KoppelingData.BEZORGD, "the letter is in the pockets");
        helper.assertTrue(!Makelaarsbrief.klok(p, d, 20 * 60 * 60), "no second letter while you have it");
        helper.assertTrue(Makelaarsbrief.koop(p) == Makelaarsbrief.Koop.GEEN_KNABBEL, "no kaasknabbel: no deal");
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        helper.assertTrue(Makelaarsbrief.koop(p) == Makelaarsbrief.Koop.GEKOCHT, "bought for one kaasknabbel");
        helper.assertTrue(Makelaarsbrief.gekocht(s, p.getUUID()) && Makelaarsbrief.telKnabbels(p) == 1, "one knabbel paid");
        helper.assertTrue(p.getInventory().contains(x -> x.is(KoppelingFeature.ERFHEKJE_ITEM.get())), "the Erfhekje is handed out");
        helper.assertTrue(MOMENTEN.contains(p.getUUID() + "/brief_gelezen"), "the moment brief_gelezen fired");
        helper.assertTrue(Makelaarsbrief.koop(p) == Makelaarsbrief.Koop.EXTRA && Makelaarsbrief.telKnabbels(p) == 0, "an extra hekje for the last knabbel");
        helper.succeed();
    }

    /** The hekje is two blocks: breaking the lower half takes the upper with it. Decobonnen count. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void hekjeEnDecobon(GameTestHelper helper) {
        BlockState onder = KoppelingFeature.ERFHEKJE.get().defaultBlockState();
        helper.setBlock(new BlockPos(1, 2, 1), onder);
        helper.setBlock(new BlockPos(1, 3, 1), onder.setValue(ErfhekjeBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertBlockPresent(KoppelingFeature.ERFHEKJE.get(), new BlockPos(1, 3, 1));
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        helper.assertBlockNotPresent(KoppelingFeature.ERFHEKJE.get(), new BlockPos(1, 3, 1));
        MinecraftServer s = helper.getLevel().getServer();
        UUID wie = UUID.randomUUID();
        helper.assertTrue(Dorpsbeloningen.decobonnen(s, wie) == 0 && !Dorpsbeloningen.neemDecobon(s, wie), "no bonnen at first");
        Dorpsbeloningen.geefDecobon(s, wie, 2);
        helper.assertTrue(Dorpsbeloningen.neemDecobon(s, wie) && Dorpsbeloningen.decobonnen(s, wie) == 1, "one used, one left");
        helper.succeed();
    }
}
