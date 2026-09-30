package nl.juiced.guhs.feature.smul;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Vadsig eetfestijn: starting without your own things, catching and scoring, the end (smulmunten, record, the bowl goes
 * back), stopping early, the borrowed bowl, the shop and the protection. Most run in a small test arena (smul_testarena),
 * one in the real festival.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class SmulGameTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "smul_testarena";
    /** Their own batch: the big festival and the mock players stay away from the other tests. */
    private static final String BATCH = "smul";

    private static GuhNpcEntity smulguh(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.SMULGUH);
        helper.assertTrue(npcs.size() == 1, "there is one Smulguh: " + npcs.size());
        return npcs.get(0);
    }

    /** A survival player without anything, right next to the Smulguh. */
    private static ServerPlayer player(GameTestHelper helper, GuhNpcEntity npc) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.moveTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
        return p;
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        return GuhQuests.count(p, item);
    }

    private static boolean hasBowl(ServerPlayer p) {
        return p.getInventory().contains(new ItemStack(SmulFeature.SMULSCHAAL.get()));
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            SmulGame.stopFor(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.server.getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void smulStartsWithoutOwnThingsAndProtectsYou(GameTestHelper helper) {
        GuhNpcEntity npc = smulguh(helper);
        ServerPlayer p = player(helper, npc);
        ServerPlayer other = player(helper, npc);
        SmulGame.action(npc, p, SmulGame.START);
        SmulGame game = SmulGame.of(npc);
        helper.assertTrue(game != null && SmulGame.isPlaying(p) && game.counting(), "the game starts with a countdown");
        helper.assertTrue(p.getMainHandItem().is(SmulFeature.SMULSCHAAL.get()), "you get the smulschaal in your hand");
        SmulGame.Arena arena = game.arena();
        helper.assertTrue(arena.chutes().size() == 2 && arena.max().getX() - arena.min().getX() == 10 && arena.max().getZ() - arena.min().getZ() == 10,
                "the arena is found from its markers: " + arena);
        helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(arena.start())) < 1, "you're put at the start");
        SmulGame.action(npc, other, SmulGame.START);
        helper.assertTrue(!SmulGame.isPlaying(other) && !hasBowl(other) && SmulGame.of(npc) == game, "one game at a time");
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    p.getFoodData().setFoodLevel(3);
                    p.hurt(helper.getLevel().damageSources().fall(), 6f);
                    helper.assertTrue(p.getHealth() == p.getMaxHealth(), "no damage while playing");
                    p.getInventory().clearContent();                // (thrown away / put in a chest...)
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(hasBowl(p), "the bowl comes back");
                    helper.assertTrue(p.getFoodData().getFoodLevel() == 20, "and no hunger");
                })
                .thenWaitUntil(() -> helper.assertTrue(!game.counting(), "the countdown ends"))
                .thenExecuteAfter(40, () -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(SmulHapje.class, arena.box()).isEmpty(),
                        "food falls"))
                .thenExecute(() -> {
                    leave(helper, p, other);
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(SmulHapje.class, arena.box(), Entity::isAlive).isEmpty(), "the food is gone");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void smulCatchingScoresCombosGoldAndMikaVet(GameTestHelper helper) {
        GuhNpcEntity npc = smulguh(helper);
        ServerPlayer p = player(helper, npc);
        helper.assertTrue(SmulGame.start(npc, p), "started");
        SmulGame game = SmulGame.of(npc);
        game.spawning = false;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!game.counting(), "the countdown ends"))
                .thenExecute(() -> {
                    var world = helper.getLevel();
                    Vec3 far = Vec3.atBottomCenterOf(game.arena().min()).add(1, 0, 1);
                    for (int i = 0; i < 5; i++) {
                        game.catchHapje(npc, p, game.spawnAt(world, SmulHapje.Soort.KAASKNABBEL, far, 0.1f));
                    }
                    helper.assertTrue(game.score() == 6 && game.streak() == 5, "4x1 + the fifth at combo x2 = 6: " + game.score());
                    game.catchHapje(npc, p, game.spawnAt(world, SmulHapje.Soort.MIKA_VET, far, 0.1f));
                    helper.assertTrue(game.score() == 1 && game.streak() == 0 && p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                            "Mika-vet: -5, the combo is broken and you're slow");
                    game.catchHapje(npc, p, game.spawnAt(world, SmulHapje.Soort.GOUD, far, 0.1f));
                    helper.assertTrue(game.score() == 11 && game.goldMode(), "golden: +10 and double points for a while");
                    game.catchHapje(npc, p, game.spawnAt(world, SmulHapje.Soort.TAART, far, 0.1f));
                    helper.assertTrue(game.score() == 21, "a guh cake in gold mode: 5 x 2 = 10: " + game.score());
                    game.spawnAt(world, SmulHapje.Soort.CUPCAKE, p.position().add(0, 0.5, 0), 0.1f);   // right into your bowl
                })
                .thenExecuteAfter(3, () -> helper.assertTrue(game.score() == 25 && game.caught() == 8,
                        "walking into it catches it: cupcake 2 x gold 2 = 4: " + game.score()))
                .thenExecute(() -> game.spawnAt(helper.getLevel(), SmulHapje.Soort.MACARON, Vec3.atBottomCenterOf(game.arena().max()).add(-1, 0, -1), 0.3f))
                .thenExecuteAfter(SmulHapje.LIE_TICKS + 10, () -> helper.assertTrue(game.streak() == 0 && game.score() == 25,
                        "a snack that went splat breaks the combo"))
                .thenExecute(() -> {
                    helper.assertTrue(SmulGame.combo(4) == 1 && SmulGame.combo(5) == 2 && SmulGame.combo(12) == 3, "combo x2 from 5, x3 from 10");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void smulEndGivesMuntenRecordAndTakesTheBowl(GameTestHelper helper) {
        GuhNpcEntity npc = smulguh(helper);
        ServerPlayer p = player(helper, npc);
        SmulGame.start(npc, p);
        SmulGame first = SmulGame.of(npc);
        first.spawning = false;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!first.counting(), "the countdown ends"))
                .thenExecute(() -> {
                    first.score = 130;
                    first.golden = 1;
                    first.finish(npc, p);
                    helper.assertTrue(!SmulGame.isPlaying(p) && SmulGame.of(npc) == null, "the game is over");
                    helper.assertTrue(!hasBowl(p), "the bowl went back");
                    int munten = SmulGame.munten(130);
                    helper.assertTrue(munten == 6, "130 points = 6 smulmunten: " + munten);
                    helper.assertTrue(count(p, SmulFeature.SMULMUNT.get()) == munten + SmulGame.FIRST_BONUS, "plus the welcome bonus the first time");
                    helper.assertTrue(count(p, ModItems.GUH_TAART.get()) == 1, "and a guh cake");
                    helper.assertTrue(SmulGame.best(p) == 130, "a record");
                    var top = nl.juiced.guhs.quest.Scorebord.top(p.server, SmulGame.BOARD);
                    helper.assertTrue(top.stream().anyMatch(e -> e.player().equals(p.getUUID()) && e.score() == 130)
                            || (top.size() == 3 && top.stream().allMatch(e -> e.score() >= 130)), "the score went to the world's top 3: " + top);
                    helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npc.getBoundingBox().inflate(1, 4, 1),
                            d -> d.getTags().contains(nl.juiced.guhs.quest.Scorebord.TAG)).isEmpty(), "and the top 3 board floats above the Smulguh");
                    helper.assertTrue(advancement(p, "smul_gespeeld") && advancement(p, "smul_100") && advancement(p, "smul_goud")
                            && !advancement(p, "smul_200"), "the advancements");
                    helper.assertTrue(p.getZ() > npc.getZ() + 1.5 && Math.abs(p.getY() - npc.getY()) < 1.01, "back in front of the Smulguh, on the floor: " + p.position());
                    helper.assertTrue(SmulGame.munten(0) == 0 && SmulGame.munten(1) == 2 && SmulGame.munten(250) == 8, "nothing for standing still, 2 for playing, 8 at most");
                    p.moveTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
                    helper.assertTrue(SmulGame.start(npc, p), "play again");
                    SmulGame.of(npc).spawning = false;
                })
                .thenWaitUntil(() -> helper.assertTrue(SmulGame.of(npc) != null && !SmulGame.of(npc).counting(), "the countdown ends"))
                .thenExecute(() -> {
                    SmulGame second = SmulGame.of(npc);
                    second.score = 50;
                    second.finish(npc, p);
                    helper.assertTrue(count(p, SmulFeature.SMULMUNT.get()) == SmulGame.munten(130) + SmulGame.FIRST_BONUS + SmulGame.munten(50), "50 points: 4 more, no bonus");
                    helper.assertTrue(SmulGame.best(p) == 130 && GuhQuests.saved(p).getInt(SmulGame.GAMES_KEY) == 2, "the record stays, 2 games");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void smulWalkingOffLoggingOutOrDyingStopsWithoutMunten(GameTestHelper helper) {
        GuhNpcEntity npc = smulguh(helper);
        ServerPlayer p = player(helper, npc);
        SmulGame.start(npc, p);
        helper.startSequence()
                .thenExecuteAfter(70, () -> p.moveTo(p.getX() + 30, p.getY(), p.getZ()))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(!SmulGame.isPlaying(p) && SmulGame.of(npc) == null && !hasBowl(p), "walking off stops the game");
                    helper.assertTrue(count(p, SmulFeature.SMULMUNT.get()) == 0 && SmulGame.best(p) == 0, "without smulmunten or a record");
                    p.moveTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
                    SmulGame.start(npc, p);
                    SmulGame.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                    helper.assertTrue(!SmulGame.isPlaying(p) && SmulGame.of(npc) == null && !hasBowl(p), "logging out stops it");
                    p.moveTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
                    SmulGame.start(npc, p);
                    SmulGame.onDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(p, helper.getLevel().damageSources().fellOutOfWorld()));
                    helper.assertTrue(!SmulGame.isPlaying(p) && SmulGame.of(npc) == null && !hasBowl(p), "dying stops it (before the bowl drops)");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void smulschaalCantBeKept(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(SmulFeature.SMULSCHAAL.get()));
        p.getInventory().tick();
        helper.assertTrue(!hasBowl(p), "outside a game it vanishes from your inventory");
        ItemEntity dropped = new ItemEntity(helper.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(SmulFeature.SMULSCHAAL.get()));
        helper.getLevel().addFreshEntity(dropped);
        dropped.tick();
        helper.assertTrue(dropped.isRemoved(), "dropped, it's gone");
        var chest = new net.minecraft.world.SimpleContainer(27);
        var menu = net.minecraft.world.inventory.ChestMenu.threeRows(1, p.getInventory(), chest);
        chest.setItem(4, new ItemStack(SmulFeature.SMULSCHAAL.get()));
        SmulGame.onContainerClose(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close(p, menu));
        helper.assertTrue(chest.isEmpty(), "put in a chest, it's gone when the chest closes");
        var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                net.minecraft.core.Direction.NORTH);
        var use = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND, frame);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(SmulFeature.SMULSCHAAL.get()));
        SmulGame.onInteractEntity(use);
        helper.assertTrue(use.isCanceled(), "it can't be put in an item frame");
        leave(helper, p);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void smulguhSellsTheSmulOutfitForSmulmunten(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.SMULGUH);
        var offers = npc.getOffers();
        var results = offers.stream().map(o -> o.getResult().getItem()).toList();
        for (GuhClothes piece : List.of(GuhClothes.SMUL_SLABBETJE, GuhClothes.SMUL_BAKKERSMUTS, GuhClothes.SMUL_SCHORT)) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "sells " + piece);
        }
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(SmulFeature.SMULMUNT.get())), "all for smulmunten");
        helper.assertTrue(GuhClothes.SMUL_SCHORT.slot == GuhClothes.Slot.BODY && GuhClothes.SMUL_BAKKERSMUTS.slot == GuhClothes.Slot.HEAD
                && GuhClothes.SMUL_SLABBETJE.slot == GuhClothes.Slot.NECK, "a bib, a hat and an apron");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void smulFestijnCantBeBrokenBySurvivalPlayers(GameTestHelper helper) {
        var survival = helper.makeMockPlayer(GameType.SURVIVAL);
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertTrue(SmulProtection.denies(survival, true) && !SmulProtection.denies(survival, false), "survival players can't build inside");
        helper.assertTrue(!SmulProtection.denies(creative, true), "creative players can");
        helper.assertTrue(!SmulProtection.inFestijn(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)), "a test room is no festival");
        helper.succeed();
    }

    /** The real festival: the Smulguh finds her arena (24x24, four chutes), food falls inside it, you come out on the floor. */
    @GameTest(template = "vadsig_eetfestijn", timeoutTicks = 400, batch = BATCH)
    public static void smulRealFestijnArenaWorks(GameTestHelper helper) {
        GuhNpcEntity npc = smulguh(helper);
        SmulGame.Arena arena = SmulGame.arena(npc);
        helper.assertTrue(arena != null && arena.chutes().size() == 4, "the arena and its four chutes: " + arena);
        helper.assertTrue(arena.max().getX() - arena.min().getX() == 23 && arena.max().getZ() - arena.min().getZ() == 23, "24x24: " + arena);
        Vec3 exit = SmulGame.exitSpot(npc, arena);
        BlockPos feet = BlockPos.containing(exit);
        helper.assertTrue(helper.getLevel().getBlockState(feet).isAir() && helper.getLevel().getBlockState(feet.above()).isAir()
                && helper.getLevel().getBlockState(feet.below()).isSolid(), "the way out is on the lobby floor: " + exit);
        ServerPlayer p = player(helper, npc);
        helper.assertTrue(SmulGame.start(npc, p), "started");
        SmulGame game = SmulGame.of(npc);
        helper.startSequence()
                .thenExecuteAfter(SmulGame.COUNTDOWN_TICKS + 80, () -> {
                    var food = helper.getLevel().getEntitiesOfClass(SmulHapje.class, arena.box());
                    helper.assertTrue(!food.isEmpty(), "food falls in the arena");
                    helper.assertTrue(food.stream().allMatch(h -> h.getY() >= arena.start().getY() - 0.01), "and lands on its floor");
                    helper.assertTrue(SmulGame.isPlaying(p), "still playing");
                    game.finish(npc, p);
                    helper.assertTrue(p.position().distanceTo(exit) < 1 && count(p, SmulFeature.SMULMUNT.get()) >= 1, "out with smulmunten");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    // --- 2.9: makkelijk / medium / lastig -----------------------------------------------------------------------------------

    /** The levels: more Mika-vet and faster food on lastig, less and slower on makkelijk (medium = the old game). */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void smulLevelsChangeMikaVetAndFallSpeed(GameTestHelper helper) {
        var m = nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK;
        var n = nl.juiced.guhs.feature.spelen.Niveau.MEDIUM;
        var l = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        helper.assertTrue(SmulGame.valSnelheid(m) < SmulGame.valSnelheid(n) && SmulGame.valSnelheid(n) == 1f
                && SmulGame.valSnelheid(n) < SmulGame.valSnelheid(l), "the food falls faster per level");
        for (float progress : new float[]{0f, 0.5f, 1f}) {
            int wm = SmulHapje.Soort.MIKA_VET.weight(progress, m), wn = SmulHapje.Soort.MIKA_VET.weight(progress, n),
                    wl = SmulHapje.Soort.MIKA_VET.weight(progress, l);
            helper.assertTrue(wm < wn && wn < wl, "more Mika-vet per level at " + progress + ": " + wm + " " + wn + " " + wl);
            helper.assertTrue(SmulHapje.Soort.TAART.weight(progress, m) == SmulHapje.Soort.TAART.weight(progress, l), "the good food stays the same");
        }
        int[] vet = new int[3];
        for (var niveau : nl.juiced.guhs.feature.spelen.Niveau.values()) {
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(42);
            for (int i = 0; i < 3000; i++) {
                if (SmulHapje.Soort.pick(random, 0.5f, niveau) == SmulHapje.Soort.MIKA_VET) {
                    vet[niveau.ordinal()]++;
                }
            }
        }
        helper.assertTrue(vet[0] < vet[1] && vet[1] < vet[2], "and it really falls more often: " + java.util.Arrays.toString(vet));
        helper.succeed();
    }

    /** A game on lastig: half as many smulmunten more, its own record and board, and the lastig advancement. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void smulLastigGivesMoreMuntenAndItsOwnBoard(GameTestHelper helper) {
        GuhNpcEntity npc = smulguh(helper);
        ServerPlayer p = player(helper, npc);
        var lastig = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        SmulGame.action(npc, p, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(SmulGame.START, lastig));
        SmulGame game = SmulGame.of(npc);
        helper.assertTrue(game != null && game.niveau() == lastig, "a game on lastig");
        game.spawning = false;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!game.counting(), "the countdown ends"))
                .thenExecute(() -> {
                    SmulHapje hapje = game.spawn(helper.getLevel(), 0.5f);
                    helper.assertTrue(hapje != null && hapje.isAlive(), "food falls");
                    hapje.discard();
                    game.score = 130;
                    game.finish(npc, p);
                    int munten = lastig.munten(SmulGame.munten(130));
                    helper.assertTrue(munten == 9, "130 points on lastig: 6 smulmunten become 9: " + munten);
                    helper.assertTrue(count(p, SmulFeature.SMULMUNT.get()) == munten + SmulGame.FIRST_BONUS, "smulmunten: " + count(p, SmulFeature.SMULMUNT.get()));
                    helper.assertTrue(SmulGame.best(p, lastig) == 130 && SmulGame.best(p) == 0, "the lastig record, medium untouched");
                    helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(p.server, "smul_punten_lastig").stream()
                            .anyMatch(e -> e.player().equals(p.getUUID()) && e.score() == 130), "on the lastig board");
                    helper.assertTrue(nl.juiced.guhs.feature.klassiekers.Klassiekers.done(p, "grote_guhspelen/klassiekers_smul_lastig"),
                            "the lastig advancement");
                    leave(helper, p);
                })
                .thenSucceed();
    }
}
