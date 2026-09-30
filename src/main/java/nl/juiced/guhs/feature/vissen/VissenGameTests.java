package nl.juiced.guhs.feature.vissen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/** GameTests of the Guhvis-wedstrijd (run with the others:  gradlew runGameTestServer). */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class VissenGameTests {
    private static final String EMPTY = "empty";

    private static GuhNpcEntity visguh(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.VISGUH);
        return npc;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer angler(GameTestHelper helper, GuhNpcEntity npc) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.moveTo(npc.getX() + 1, npc.getY(), npc.getZ());
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            VisWedstrijd.leave(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int count(ServerPlayer player, VisSoort soort) {
        return GuhQuests.count(player, VissenFeature.vis(soort));
    }

    /** On the (world-wide, shared by all tests) top-3 board with this score, or the board is full of better ones. */
    private static boolean onBoardOrBeaten(GameTestHelper helper, ServerPlayer player, String board, int score) {
        List<Scorebord.Entry> top = Scorebord.top(helper.getLevel().getServer(), board);
        return top.stream().anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == score)
                || top.size() == Scorebord.PLACES && top.stream().allMatch(e -> e.score() >= score);
    }

    /** Walk in with empty pockets, get a rod, catch fish (points by weight), time's up: visbonnen, records, rod gone. */
    @GameTest(template = EMPTY)
    public static void vissenContestWithoutOwnItems(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, VisWedstrijd.START);
        helper.assertTrue(VisWedstrijd.isFishing(player), "the contest started");
        helper.assertTrue(VisWedstrijd.rods(player) == 1 && player.getMainHandItem().getItem() instanceof GuhvisHengel, "a rod on loan, in hand");
        VisWedstrijd contest = VisWedstrijd.of(npc);
        helper.assertTrue(contest != null && contest.isCountingDown(helper.getLevel().getGameTime()), "first the countdown");
        VisWedstrijd.action(npc, player, VisWedstrijd.START);
        helper.assertTrue(VisWedstrijd.rods(player) == 1, "starting twice doesn't give two rods");

        VisWedstrijd.skipCountdown(npc);
        Vec3 water = npc.position().add(0, -1, -10);
        var a = contest.land(npc, player, VisSoort.KAASVIS, 1000, water);
        var b = contest.land(npc, player, VisSoort.VADSBAARS, 4000, water);
        var c = contest.land(npc, player, VisSoort.MIKA_MEERVAL, 3000, water);
        helper.assertTrue(a.points() == 15 && b.points() == 40 && c.points() == -25, "points by weight (and the Mika catfish costs)");
        var score = VisWedstrijd.score(player);
        helper.assertTrue(score != null && score.points == 30 && score.fish == 3, "the score adds up: " + (score == null ? "-" : score.points));
        helper.assertTrue(score.heaviest == VisSoort.VADSBAARS && score.heaviestGrams == 4000, "the Mika catfish never counts as the heaviest");
        helper.assertTrue(count(player, VisSoort.KAASVIS) == 1 && count(player, VisSoort.VADSBAARS) == 1 && count(player, VisSoort.MIKA_MEERVAL) == 1,
                "the fish are yours");

        VisWedstrijd.endNow(npc);
        helper.assertTrue(!VisWedstrijd.isFishing(player) && VisWedstrijd.of(npc) == null, "the contest is over");
        helper.assertTrue(VisWedstrijd.rods(player) == 0, "the rod went back");
        helper.assertTrue(count(player, VisSoort.KAASVIS) == 1, "the fish stay");
        int bonnen = GuhQuests.count(player, VissenFeature.VISBON.get());
        helper.assertTrue(bonnen == VisWedstrijd.bonnen(30) + VisWedstrijd.FIRST_BONNEN, "visbonnen for the points + the first-contest present: " + bonnen);
        helper.assertTrue(GuhQuests.count(player, ModItems.GEBAKKEN_GUH_VIS.get()) == 4, "the present has fried guhfish");
        helper.assertTrue(VisWedstrijd.best(player) == 30 && VisWedstrijd.heaviest(player) == 4000 && VisWedstrijd.games(player) == 1, "records saved");
        helper.assertTrue(onBoardOrBeaten(helper, player, VisWedstrijd.BOARD_POINTS, 30), "the world's top 3 of points");
        helper.assertTrue(onBoardOrBeaten(helper, player, VisWedstrijd.BOARD_HEAVIEST, 4000), "the world's top 3 of heaviest fish");
        leave(helper, player);
        helper.succeed();
    }

    /** A second, better contest: a new record with a bonus; a worse one doesn't overwrite it. No second present. */
    @GameTest(template = EMPTY)
    public static void vissenRecordsSurviveAndImprove(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        int[] totals = {30, 195, 15};
        int[] expected = {VisWedstrijd.bonnen(30) + VisWedstrijd.FIRST_BONNEN, VisWedstrijd.bonnen(195) + VisWedstrijd.BONUS, VisWedstrijd.bonnen(15)};
        for (int round = 0; round < 3; round++) {
            player.getInventory().clearContent();
            VisWedstrijd.action(npc, player, VisWedstrijd.START);
            VisWedstrijd.skipCountdown(npc);
            VisWedstrijd contest = VisWedstrijd.of(npc);
            if (round == 1) {
                contest.land(npc, player, VisSoort.GOUDEN_GUHVIS, 3000, npc.position());          // 150 + 45 = 195
                ItemStack gold = player.getInventory().items.stream().filter(st -> st.is(VissenFeature.vis(VisSoort.GOUDEN_GUHVIS)))
                        .findFirst().orElse(ItemStack.EMPTY);
                var tag = gold.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                helper.assertTrue(tag.getInt("GuhvisNr") == 1 && tag.getInt("GuhvisGram") == 3000 && VisWedstrijd.golden(player) == 1,
                        "the golden guhfish is a numbered collector's piece: " + tag);
            } else {
                contest.land(npc, player, VisSoort.KAASVIS, round == 0 ? 2500 : 1000, npc.position());
            }
            helper.assertTrue(VisWedstrijd.score(player).points == totals[round], "round " + round + ": " + VisWedstrijd.score(player).points);
            VisWedstrijd.endNow(npc);
            int bonnen = GuhQuests.count(player, VissenFeature.VISBON.get());
            helper.assertTrue(bonnen == expected[round], "round " + round + " visbonnen: " + bonnen + " (expected " + expected[round] + ")");
        }
        helper.assertTrue(VisWedstrijd.best(player) == 195 && VisWedstrijd.games(player) == 3, "the best stays 195");
        helper.assertTrue(VisWedstrijd.heaviest(player) == 3000, "the golden guhfish is the heaviest");
        leave(helper, player);
        helper.succeed();
    }

    /** Vanilla fishing in the pond during the contest lands one of our fish instead of the vanilla loot. */
    @GameTest(template = EMPTY)
    public static void vissenRealBiteGivesContestFish(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, VisWedstrijd.START);

        FishingHook early = new FishingHook(player, helper.getLevel(), 0, 0);
        early.setPos(npc.getX(), npc.getY() - 1, npc.getZ() - 8);
        ItemFishedEvent tooEarly = NeoForge.EVENT_BUS.post(new ItemFishedEvent(List.of(new ItemStack(net.minecraft.world.item.Items.COD)), 1, early));
        early.discard();
        helper.assertTrue(tooEarly.isCanceled() && VisWedstrijd.score(player).fish == 0, "nothing counts during the countdown");

        VisWedstrijd.skipCountdown(npc);
        FishingHook hook = new FishingHook(player, helper.getLevel(), 0, 0);
        hook.setPos(npc.getX(), npc.getY() - 1, npc.getZ() - 8);
        ItemFishedEvent bite = NeoForge.EVENT_BUS.post(new ItemFishedEvent(List.of(new ItemStack(net.minecraft.world.item.Items.COD)), 1, hook));
        hook.discard();
        int ours = 0;
        for (VisSoort soort : VisSoort.values()) {
            ours += count(player, soort);
        }
        helper.assertTrue(bite.isCanceled() && ours == 1 && VisWedstrijd.score(player).fish == 1, "one contest fish, no cod");

        FishingHook far = new FishingHook(player, helper.getLevel(), 0, 0);
        far.setPos(npc.getX() + VisWedstrijd.POND_RADIUS + 20, npc.getY(), npc.getZ());
        ItemFishedEvent elsewhere = NeoForge.EVENT_BUS.post(new ItemFishedEvent(List.of(new ItemStack(net.minecraft.world.item.Items.COD)), 1, far));
        far.discard();
        helper.assertTrue(elsewhere.isCanceled() && VisWedstrijd.score(player).fish == 1, "the loaned rod catches nothing outside the pond");
        VisWedstrijd.endNow(npc);
        leave(helper, player);
        helper.succeed();
    }

    /** The loaned rod can't be thrown away, stored or kept, and anglers can't get hurt or hungry. */
    @GameTest(template = EMPTY)
    public static void vissenRodIsNeverKept(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, VisWedstrijd.START);
        ItemStack rod = player.getMainHandItem();
        helper.assertTrue(!rod.getItem().onDroppedByPlayer(rod, player), "Q doesn't drop it");
        player.getInventory().removeItem(rod);
        ItemEntity thrown = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), rod);
        ItemTossEvent toss = NeoForge.EVENT_BUS.post(new ItemTossEvent(thrown, player));
        helper.assertTrue(toss.isCanceled() && VisWedstrijd.rods(player) == 1, "thrown out of the inventory: it jumps back");
        helper.assertTrue(!rod.getItem().canFitInsideContainerItems(), "not into a shulker box or bundle");
        var barrel = new net.minecraft.world.SimpleContainer(27);
        var menu = net.minecraft.world.inventory.ChestMenu.threeRows(1, player.getInventory(), barrel);
        player.getInventory().clearContent();
        barrel.setItem(0, new ItemStack(VissenFeature.GUHVIS_HENGEL.get()));
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close(player, menu));
        helper.assertTrue(barrel.getItem(0).isEmpty() && VisWedstrijd.rods(player) == 1, "put in a barrel: back in your pockets when it closes");
        var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                net.minecraft.core.Direction.UP);
        var framed = NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(player,
                net.minecraft.world.InteractionHand.MAIN_HAND, frame));
        helper.assertTrue(player.getMainHandItem().getItem() instanceof GuhvisHengel && framed.isCanceled() && frame.getItem().isEmpty(),
                "not hung in an item frame");

        VisWedstrijd.action(npc, player, VisWedstrijd.STOP);
        helper.assertTrue(!VisWedstrijd.isFishing(player) && VisWedstrijd.rods(player) == 0, "stopping gives the rod back");

        ItemStack kept = new ItemStack(VissenFeature.GUHVIS_HENGEL.get());   // a rod somehow left over (a chest, a restart...)
        player.getInventory().add(kept);
        kept.inventoryTick(helper.getLevel(), player, 0, false);
        helper.assertTrue(kept.isEmpty(), "outside a contest a rod swims back by itself");
        ItemEntity lying = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(VissenFeature.GUHVIS_HENGEL.get()));
        helper.getLevel().addFreshEntity(lying);
        lying.tick();
        helper.assertTrue(lying.isRemoved(), "a rod on the ground is gone at once");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void vissenAnglersAreSafe(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, VisWedstrijd.START);
        player.getFoodData().setFoodLevel(3);
        var event = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().fall(), 5f));
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "no damage while fishing");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "and never hungry");
            VisWedstrijd.endNow(npc);
            var after = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
                    new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().fall(), 5f));
            NeoForge.EVENT_BUS.post(after);
            helper.assertTrue(!after.isCanceled(), "after the contest: normal again");
            leave(helper, player);
            helper.succeed();
        });
    }

    /** Walking off (or logging out, dying, another dimension) takes you out of the contest, and the rod goes back. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void vissenWalkingOffEndsIt(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, VisWedstrijd.START);
        player.moveTo(npc.getX() + VisWedstrijd.AREA_RADIUS + 20, npc.getY(), npc.getZ());
        helper.succeedWhen(() -> {
            helper.assertTrue(!VisWedstrijd.isFishing(player), "still fishing");
            helper.assertTrue(VisWedstrijd.rods(player) == 0, "the rod went back");
            helper.assertTrue(VisWedstrijd.of(npc) == null, "nobody left: the contest ends");
            leave(helper, player);
        });
    }

    /** Fishing together: a friend joins during the contest, both get their own score, the winner a bonus. */
    @GameTest(template = EMPTY)
    public static void vissenTogetherHasAWinner(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer first = angler(helper, npc), friend = angler(helper, npc);
        VisWedstrijd.action(npc, first, VisWedstrijd.START);
        VisWedstrijd.action(npc, friend, VisWedstrijd.JOIN);
        VisWedstrijd contest = VisWedstrijd.of(npc);
        helper.assertTrue(contest.players() == 2 && VisWedstrijd.rods(friend) == 1, "the friend joined with a rod of their own");
        VisWedstrijd.skipCountdown(npc);
        contest.land(npc, first, VisSoort.NJEGFOREL, 2000, npc.position());      // 35 + 24 = 59
        contest.land(npc, friend, VisSoort.KAASVIS, 500, npc.position());        // 5 + 5 = 10
        VisWedstrijd.endNow(npc);
        int a = GuhQuests.count(first, VissenFeature.VISBON.get()), b = GuhQuests.count(friend, VissenFeature.VISBON.get());
        helper.assertTrue(a == VisWedstrijd.bonnen(59) + VisWedstrijd.BONUS + VisWedstrijd.FIRST_BONNEN, "the winner gets a bonus: " + a);
        helper.assertTrue(b == VisWedstrijd.bonnen(10) + VisWedstrijd.FIRST_BONNEN, "the other one doesn't: " + b);
        helper.assertTrue(VisWedstrijd.rods(first) == 0 && VisWedstrijd.rods(friend) == 0, "both rods went back");
        leave(helper, first, friend);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void visguhSellsTheAnglerOutfit(GameTestHelper helper) {
        GuhNpcEntity npc = visguh(helper);
        var offers = npc.getOffers();
        var results = offers.stream().map(o -> o.getResult().getItem()).toList();
        for (GuhClothes piece : VissenFeature.OUTFIT) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "sells " + piece);
        }
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(VissenFeature.VISBON.get())), "for visbonnen");
        helper.assertTrue(offers.stream().noneMatch(o -> o.getResult().getItem() instanceof GuhvisHengel), "the rod is never for sale");
        helper.succeed();
    }

    /** The real pond: the Visguh on solid ground at her desk, water close by, and the record board fills in. */
    @GameTest(template = "guhvis_vijver", timeoutTicks = 100)
    public static void vissenThePondItself(GameTestHelper helper) {
        var npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                n -> n.getKind() == GuhNpcEntity.Kind.VISGUH);
        helper.assertTrue(npcs.size() == 1, "one Visguh at the pond: " + npcs.size());
        GuhNpcEntity npc = npcs.get(0);
        helper.assertTrue(!helper.getLevel().getBlockState(npc.blockPosition().below()).isAir(), "she stands on the floor");
        int water = 0;
        BlockPos c = npc.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-40, -6, -66), c.offset(40, 0, 0))) {
            if (helper.getLevel().getFluidState(p).is(Fluids.WATER)) {
                water++;
                helper.assertTrue(VisWedstrijd.inPond(npc, Vec3.atCenterOf(p)), "all the pond counts: " + p);
            }
        }
        helper.assertTrue(water > 3000, "a big pond: " + water);
        ServerPlayer champ = angler(helper, npc);
        Scorebord.submit(champ, VisWedstrijd.BOARD_POINTS, 99999, false);
        VisWedstrijd.showScores(npc);
        BlockPos sign = null;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-18, -2, -4), c.offset(18, 4, 4))) {
            if (helper.getLevel().getBlockEntity(p) instanceof SignBlockEntity s && s.getFrontText().getMessage(0, false).getString().contains("Visrecords")) {
                sign = p.immutable();
            }
        }
        helper.assertTrue(sign != null, "the record board is there");
        var boards = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                new net.minecraft.world.phys.AABB(sign).inflate(1, 4, 1), d -> d.getTags().contains(Scorebord.TAG));
        helper.assertTrue(boards.size() == 1, "one floating top 3 above the record board: " + boards.size());
        var saved = boards.get(0).saveWithoutId(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(saved.getString("text").contains("99999"), "the top 3 shows the champion: " + saved.getString("text"));
        leave(helper, champ);
        var shown = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class, helper.getBounds());
        helper.assertTrue(shown.size() == 11 && shown.stream().allMatch(d -> !d.getSlot(0).get().isEmpty()), "the fish on show: " + shown.size());
        helper.assertTrue(shown.stream().anyMatch(d -> d.getSlot(0).get().is(VissenFeature.vis(VisSoort.GOUDEN_GUHVIS))), "the golden guhfish trophy");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, helper.getBounds()).size() == 2, "two boats");
        helper.succeed();
    }

    // --- 2.9: makkelijk / medium / lastig -----------------------------------------------------------------------------------

    /** The bite window per level: makkelijk long, medium vanilla's own, lastig short; the float knows the contest's level. */
    @GameTest(template = EMPTY)
    public static void vissenBiteWindowPerLevel(GameTestHelper helper) {
        helper.assertTrue(GuhvisDobber.werkt(), "the float can set the bite window");
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7);
        for (int i = 0; i < 100; i++) {
            int m = GuhvisDobber.venster(nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK, random);
            int l = GuhvisDobber.venster(nl.juiced.guhs.feature.spelen.Niveau.LASTIG, random);
            helper.assertTrue(m >= 40 && m <= 70 && l >= 10 && l <= 18, "makkelijk 2-3,5 s, lastig about half a second: " + m + " " + l);
        }
        helper.assertTrue(GuhvisDobber.venster(nl.juiced.guhs.feature.spelen.Niveau.MEDIUM, random) < 0, "medium: vanilla's own window");
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(VisWedstrijd.START, nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK));
        helper.assertTrue(VisWedstrijd.niveauVan(player) == nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK, "a contest on makkelijk");
        GuhvisDobber dobber = new GuhvisDobber(player, helper.getLevel(), 0, GuhvisHengel.LURE_TICKS, VisWedstrijd.niveauVan(player));
        dobber.setNibble(25);
        helper.assertTrue(dobber.nibble() == 25 && dobber.niveau() == nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK, "the window is set on the float");
        dobber.discard();
        leave(helper, player);
        helper.succeed();
    }

    /** Lastig: fish can wriggle off the hook (not the Mika-meerval), its own board and record, and more visbonnen. */
    @GameTest(template = EMPTY)
    public static void vissenLastigFishEscapeAndMoreBonnen(GameTestHelper helper) {
        var lastig = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        helper.assertTrue(VisWedstrijd.ontsnapKans(VisSoort.MIKA_MEERVAL, 7000, lastig) == 0f, "the Mika-meerval never lets go");
        helper.assertTrue(VisWedstrijd.ontsnapKans(VisSoort.KAASVIS, 1000, nl.juiced.guhs.feature.spelen.Niveau.MEDIUM) == 0f
                && VisWedstrijd.ontsnapKans(VisSoort.KAASVIS, 1000, nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK) == 0f, "no escapes below lastig");
        helper.assertTrue(VisWedstrijd.ontsnapKans(VisSoort.VADSBAARS, 4500, lastig) > VisWedstrijd.ontsnapKans(VisSoort.VADSBAARS, 800, lastig),
                "heavy fish wriggle harder");
        helper.assertTrue(VisWedstrijd.ontsnapKans(VisSoort.GOUDEN_GUHVIS, 3000, lastig) > VisWedstrijd.ontsnapKans(VisSoort.KAASVIS, 200, lastig),
                "the golden one is the hardest to hold");
        GuhNpcEntity npc = visguh(helper);
        ServerPlayer player = angler(helper, npc);
        VisWedstrijd.action(npc, player, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(VisWedstrijd.START, lastig));
        VisWedstrijd contest = VisWedstrijd.of(npc);
        helper.assertTrue(contest != null && contest.niveau() == lastig, "a contest on lastig");
        VisWedstrijd.skipCountdown(npc);
        Vec3 water = npc.position();
        helper.assertTrue(contest.hooked(npc, player, VisSoort.KAASVIS, 1000, water, 0f) == null, "it got away!");
        helper.assertTrue(count(player, VisSoort.KAASVIS) == 0 && VisWedstrijd.score(player).points == 0 && VisWedstrijd.score(player).escaped == 1,
                "no fish, no points");
        var caught = contest.hooked(npc, player, VisSoort.KAASVIS, 1000, water, 0.99f);
        helper.assertTrue(caught != null && caught.points() == 15 && count(player, VisSoort.KAASVIS) == 1, "held on to this one: 15 points");
        VisWedstrijd.endNow(npc);
        helper.assertTrue(!VisWedstrijd.isFishing(player), "time's up");
        int bonnen = lastig.munten(VisWedstrijd.bonnen(15)) + VisWedstrijd.FIRST_BONNEN;
        helper.assertTrue(GuhQuests.count(player, VissenFeature.VISBON.get()) == bonnen && bonnen == 3 + VisWedstrijd.FIRST_BONNEN,
                "visbonnen on lastig: " + GuhQuests.count(player, VissenFeature.VISBON.get()));
        helper.assertTrue(VisWedstrijd.best(player, lastig) == 15 && VisWedstrijd.best(player) == 0, "the lastig record, medium untouched");
        helper.assertTrue(onBoardOrBeaten(helper, player, "vissen_punten_lastig", 15), "on the lastig board");
        helper.assertTrue(onBoardOrBeaten(helper, player, "vissen_zwaarste_lastig", 1000), "and the lastig heaviest-fish board");
        helper.assertTrue(nl.juiced.guhs.feature.klassiekers.Klassiekers.done(player, "grote_guhspelen/klassiekers_vissen_lastig"), "the lastig advancement");
        leave(helper, player);
        helper.succeed();
    }
}
