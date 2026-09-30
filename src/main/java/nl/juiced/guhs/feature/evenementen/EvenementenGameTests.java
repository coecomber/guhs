package nl.juiced.guhs.feature.evenementen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * GameTests of the guh events: the kaasregen (catching, happy guhs, the golden knabbel), the parade (route, marching,
 * the reward, cleaning up), the sterrenregen (stars, starry guhs that stay or fly back), dropping out, leftovers and the
 * scheduler. The tests run in the overworld, so they start their events directly (the dimension check is the
 * scheduler's and the command's job), with auto-join off so neighbouring tests don't join in.
 */
public class EvenementenGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static Kaasregen kaasregen(GameTestHelper helper, ServerPlayer player) {
        Kaasregen rain = new Kaasregen(helper.getLevel(), player.position());
        rain.autoJoin = false;
        rain.spontaneous = false;
        rain.guhRange = 3;
        rain.radius = 1;
        rain.height = 3;
        Evenementen.begin(rain, player);
        return rain;
    }

    // --- kaasregen -----------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 120)
    public static void kaasregenKnabbelsFallAndAreCaught(GameTestHelper helper) {
        ServerPlayer player = player(helper, 2, 2);
        Kaasregen rain = kaasregen(helper, player);
        helper.assertTrue(rain.takesPart(player) && Evenementen.eventOf(player) == rain, "the first player takes part");
        helper.assertTrue(rain.bar().getPlayers().contains(player), "and sees the boss bar");
        VallendeKnabbelEntity plain = rain.dropSnack(player.blockPosition(), false);
        VallendeKnabbelEntity golden = rain.dropSnack(player.blockPosition(), true);
        helper.assertTrue(!plain.landed(), "it starts up in the air");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(plain.isRemoved() && golden.isRemoved(), "both knabbels were caught on the way down or on landing");
            helper.assertTrue(count(player, ModItems.KAAS_KNABBELS.get()) == 1, "a kaasknabbel in the pocket");
            helper.assertTrue(count(player, EvenementenFeature.GOUDEN_KAASKNABBEL.get()) == 1, "and a golden one");
            helper.assertTrue(rain.caught(player) == 2, "two caught");
            VallendeKnabbelEntity lying = rain.dropSnack(player.blockPosition().offset(3, 0, 3), false);
            rain.end(false);
            helper.assertTrue(lying.isRemoved(), "an ended kaasregen leaves no knabbels behind");
            helper.assertTrue(rain.bar().getPlayers().isEmpty() && !rain.bar().isVisible(), "and no boss bar");
            helper.assertTrue(Evenementen.eventOf(player) == null, "and nobody takes part any more");
            leave(helper, player);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void aGuhThatAteFromTheSkyIsEasyToTame(GameTestHelper helper) {
        ServerPlayer player = player(helper, 0, 0);
        Kaasregen rain = kaasregen(helper, player);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        helper.assertTrue(!Evenementen.boosted(guh), "a normal wild guh isn't boosted");
        VallendeKnabbelEntity snack = rain.dropSnack(guh.blockPosition(), false);
        rain.eat(guh, snack);
        helper.assertTrue(snack.isRemoved() && Evenementen.boosted(guh), "it ate the knabbel and is happy now");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        for (int i = 0; i < 40 && !guh.isTame(); i++) {
            player.interactOn(guh, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(guh.isTame() && player.getUUID().equals(guh.getOwnerUUID()), "tamed");
        rain.end(false);
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void goldenKnabbelTamesAWildGuhAtOnce(GameTestHelper helper) {
        ServerPlayer player = player(helper, 0, 0);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        ParadeGuhEntity parader = helper.spawn(EvenementenFeature.PARADE_GUH.get(), new BlockPos(3, 1, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EvenementenFeature.GOUDEN_KAASKNABBEL.get(), 2));
        player.interactOn(guh, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.isTame() && player.getUUID().equals(guh.getOwnerUUID()), "one golden knabbel: tamed");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "and it's eaten");
        player.interactOn(parader, InteractionHand.MAIN_HAND);
        helper.assertTrue(!parader.isTame() && player.getMainHandItem().getCount() == 1, "a parade guh is nobody's: not even with gold");
        helper.assertTrue(!Evenementen.wild(parader), "a parade guh isn't a wild guh");
        leave(helper, player);
        helper.succeed();
    }

    // --- the parade ----------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void paradeRouteWindsOverWalkableGround(GameTestHelper helper) {
        RandomSource random = RandomSource.create(5);
        // hilly ground with a deep ditch at x = 30..32 and water north of z = -20
        ParadeRoute.Terrain terrain = (x, z) -> z < -20 ? null : x >= 30 && x <= 32 ? 50 : 64 + (x / 7) % 2;
        for (int i = 0; i < 20; i++) {
            ParadeRoute route = ParadeRoute.plan(terrain, random, 0.5, 0.5, random.nextDouble() * Math.PI * 2, Vadsparade.ROUTE_LENGTH,
                    Vadsparade.MIN_ROUTE);
            if (route == null) {
                continue;
            }
            helper.assertTrue(route.length() >= Vadsparade.MIN_ROUTE, "long enough");
            List<Vec3> points = route.points();
            for (int k = 1; k < points.size(); k++) {
                Vec3 a = points.get(k - 1), b = points.get(k);
                helper.assertTrue(Math.abs(a.y - b.y) <= 1, "at most one block up or down per step");
                helper.assertTrue(Math.abs(Math.hypot(a.x - b.x, a.z - b.z) - 1) < 1.0e-6, "one block per step");
                Integer ground = terrain.groundY((int) Math.floor(b.x), (int) Math.floor(b.z));
                helper.assertTrue(ground != null && ground == b.y, "on the ground, never in the water");
            }
        }
        helper.assertTrue(ParadeRoute.plan((x, z) -> null, random, 0, 0, 0, 100, 10) == null, "no ground: no route");
        ParadeRoute east = new ParadeRoute(List.of(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(2, 0, 0)));
        helper.assertTrue(Math.abs(east.yawAt(1) + 90) < 0.01, "walking east faces east (yaw -90): " + east.yawAt(1));
        helper.assertTrue(east.at(0.5).equals(new Vec3(0.5, 0, 0)) && east.at(9).equals(new Vec3(2, 0, 0)), "positions along it");
        helper.succeed();
    }

    /** A zigzag route that fits in the test area (5 x 5 blocks). */
    private static ParadeRoute zigzag(GameTestHelper helper) {
        List<Vec3> points = new ArrayList<>();
        for (int z = 0; z < 5; z++) {
            for (int i = 0; i < 5; i++) {
                int x = z % 2 == 0 ? i : 4 - i;
                points.add(helper.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5)));
            }
        }
        return new ParadeRoute(points);
    }

    @GuhTest(template = EMPTY, timeoutTicks = 400)
    public static void paradeMarchesRewardsWalkersAndCleansUp(GameTestHelper helper) {
        ServerPlayer walker = player(helper, 2, 2);
        Vadsparade parade = new Vadsparade(helper.getLevel(), zigzag(helper));
        parade.autoJoin = false;
        Evenementen.begin(parade, walker);
        List<ParadeGuhEntity> guhs = List.copyOf(parade.guhs());
        helper.assertTrue(guhs.size() == Vadsparade.GUHS + 1, "a Tamboerguh and ten paraders");
        helper.assertTrue(guhs.get(0).isDrummer() && guhs.get(0).getClothes(GuhClothes.Slot.HEAD) == GuhClothes.VADSPARADE_SJAKO,
                "the Tamboerguh wears the parade outfit");
        for (ParadeGuhEntity guh : guhs.subList(1, guhs.size())) {
            for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
                GuhClothes worn = guh.getClothes(slot);
                helper.assertTrue(worn == null || !List.of(Vadsparade.OUTFIT).contains(worn), "the paraders never wear the parade outfit");
            }
        }
        double start = parade.distanceAlong();
        int duration = parade.duration();
        helper.assertTrue(duration == Vadsparade.walkingTicks(parade.route()) + Vadsparade.FINALE, "walking time plus the finale");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(parade.distanceAlong() > start, "the parade moves along");
            Vec3 drummer = guhs.get(0).position();
            helper.assertTrue(drummer.distanceTo(parade.route().at(parade.distanceAlong())) < 0.3, "the Tamboerguh follows the route");
        });
        helper.runAfterDelay(duration + 5, () -> {
            helper.assertTrue(parade.isEnded(), "the parade is over");
            helper.assertTrue(guhs.stream().allMatch(Entity::isRemoved), "all parade guhs are gone");
            helper.assertTrue(parade.rewarded(walker), "whoever walked along is rewarded");
            helper.assertTrue(count(walker, ModItems.clothingItem(GuhClothes.VADSPARADE_SJAKO)) == 1, "with the first piece: the sjako");
            helper.assertTrue(parade.bar().getPlayers().isEmpty(), "no boss bar left");
            helper.assertTrue(!Evenementen.active().contains(parade), "and it's not running any more");
            // the next parades bring the jasje, then the trommeltje; after that a random piece
            helper.assertTrue(Vadsparade.nextPiece(walker, RandomSource.create()) == GuhClothes.VADSPARADE_JASJE, "then the jasje");
            helper.assertTrue(!Vadsparade.hasWholeOutfit(walker), "not complete yet");
            helper.assertTrue(Vadsparade.nextPiece(walker, RandomSource.create()) == GuhClothes.VADSPARADE_TROMMELTJE, "then the trommeltje");
            helper.assertTrue(Vadsparade.hasWholeOutfit(walker), "the whole outfit");
            helper.assertTrue(List.of(Vadsparade.OUTFIT).contains(Vadsparade.nextPiece(walker, RandomSource.create())), "then any piece again");
            leave(helper, walker);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void paradeWithoutAnyoneStopsAndLeavesNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper, 2, 2);
        Vadsparade parade = new Vadsparade(helper.getLevel(), zigzag(helper));
        parade.autoJoin = false;
        Evenementen.begin(parade, player);
        List<ParadeGuhEntity> guhs = List.copyOf(parade.guhs());
        leave(helper, player); // logged out / other dimension: gone from the level
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(parade.isEnded(), "nobody left: the parade stops");
            helper.assertTrue(guhs.stream().allMatch(Entity::isRemoved), "and its guhs are gone");
            helper.assertTrue(!parade.rewarded(player), "no reward for leaving");
            helper.succeed();
        });
    }

    // --- the sterrenregen ------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void sterrenregenStarsLandAndStarryGuhsStayOrGoBack(GameTestHelper helper) {
        ServerPlayer player = player(helper, 0, 0);
        Sterrenregen stars = new Sterrenregen(helper.getLevel(), player.position());
        stars.autoJoin = false;
        stars.spontaneous = false;
        Evenementen.begin(stars, player);
        VallendeSterEntity star = stars.dropStar(helper.absolutePos(new BlockPos(4, 1, 4)), 6);
        GuhEntity kept = stars.spawnStarGuh(helper.absoluteVec(new Vec3(1.5, 1, 3.5)));
        GuhEntity gone = stars.spawnStarGuh(helper.absoluteVec(new Vec3(3.5, 1, 1.5)));
        GuhEntity late = stars.spawnStarGuh(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        helper.assertTrue(kept.getVariant() == GuhVariant.STARRY && Evenementen.boosted(kept) && kept.hasGlowingTag(), "a glowing starry guh, easy to tame");
        helper.assertTrue(Evenementen.tameNow(kept, player), "tamed");
        late.getPersistentData().putLong(Evenementen.STER, helper.getLevel().getGameTime()); // its time is up
        // (wait for what we expect instead of a fixed time: many tests run at the same time)
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(star.landed() && !star.isRemoved(), "the star lands and lies there glowing"))
                .thenWaitUntil(() -> helper.assertTrue(late.isRemoved(), "an untamed starry guh goes back to the stars when its time is up"))
                .thenWaitUntil(() -> helper.assertTrue(!kept.getPersistentData().contains(Evenementen.STER) && !kept.hasGlowingTag()
                        && !stars.owns(kept), "a tamed one is simply yours now"))
                .thenExecute(() -> {
                    helper.assertTrue(!stars.isEnded(), "the sterrenregen is still going");
                    stars.end(false);
                    helper.assertTrue(gone.isRemoved() && star.isRemoved(), "at the end: untamed starry guhs and stars are gone");
                    helper.assertTrue(!kept.isRemoved() && kept.isTame(), "the tamed one stays");
                    leave(helper, player);
                })
                .thenSucceed();
    }

    /**
     * A star comes from far away and high up, often from a chunk that doesn't tick entities (outside the simulation
     * distance; in the GameTestServer: outside the test's own chunks). It must still fall and land there: its sterrenregen
     * moves it, not its own entity tick.
     */
    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void starsFallAlsoFromChunksThatDontTick(GameTestHelper helper) {
        ServerPlayer player = player(helper, 0, 0);
        Sterrenregen stars = new Sterrenregen(helper.getLevel(), player.position());
        stars.autoJoin = false;
        stars.spontaneous = false;
        Evenementen.begin(stars, player);
        List<VallendeSterEntity> dropped = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            dropped.add(stars.dropStar(helper.absolutePos(new BlockPos(4, 1, 1 + i % 3)), Sterrenregen.FALL_FROM));
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dropped.stream().allMatch(s -> s.landed() && !s.isRemoved()), "every star lands"))
                .thenExecute(() -> {
                    stars.end(false);
                    helper.assertTrue(dropped.stream().allMatch(Entity::isRemoved), "and is cleaned up at the end");
                    leave(helper, player);
                })
                .thenSucceed();
    }

    @GuhTest(template = EMPTY)
    public static void walkingIntoALandedStarGivesSterrenstof(GameTestHelper helper) {
        ServerPlayer player = player(helper, 2, 2);
        Sterrenregen stars = new Sterrenregen(helper.getLevel(), player.position());
        stars.autoJoin = false;
        stars.spontaneous = false;
        Evenementen.begin(stars, player);
        VallendeSterEntity star = stars.dropStar(player.blockPosition(), 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(star.isRemoved(), "picked up");
            helper.assertTrue(count(player, EvenementenFeature.STERRENSTOF.get()) == 1, "one sterrenstof");
            stars.end(false);
            leave(helper, player);
        });
    }

    // --- dropping out, leftovers ----------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 60)
    public static void dyingDropsYouOutAndAnEmptyEventStops(GameTestHelper helper) {
        ServerPlayer player = player(helper, 2, 2);
        Kaasregen rain = kaasregen(helper, player);
        VallendeKnabbelEntity snack = rain.dropSnack(player.blockPosition().offset(2, 0, 2), false);
        Evenementen.onDeath(new LivingDeathEvent(player, helper.getLevel().damageSources().generic()));
        helper.assertTrue(!rain.takesPart(player) && !rain.bar().getPlayers().contains(player), "out of the event, no boss bar");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(rain.isEnded() && snack.isRemoved(), "nobody left: over, nothing left behind");
            leave(helper, player);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void leftoversCleanThemselvesUp(GameTestHelper helper) {
        ParadeGuhEntity stray = helper.spawn(EvenementenFeature.PARADE_GUH.get(), new BlockPos(1, 1, 1));
        VallendeKnabbelEntity snack = helper.spawn(EvenementenFeature.VALLENDE_KNABBEL.get(), new BlockPos(3, 1, 3));
        GuhEntity starry = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        starry.getPersistentData().putLong(Evenementen.STER, helper.getLevel().getGameTime() + 1000);
        EntityJoinLevelEvent loaded = new EntityJoinLevelEvent(starry, helper.getLevel(), true);
        Evenementen.onJoinLevel(loaded);
        helper.assertTrue(loaded.isCanceled(), "a starry guh of an event that's long over doesn't come back after a restart");
        GuhEntity tamed = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        tamed.getPersistentData().putLong(Evenementen.STER, helper.getLevel().getGameTime() + 1000);
        tamed.setTame(true, false);
        EntityJoinLevelEvent mine = new EntityJoinLevelEvent(tamed, helper.getLevel(), true);
        Evenementen.onJoinLevel(mine);
        helper.assertTrue(!mine.isCanceled() && !tamed.getPersistentData().contains(Evenementen.STER), "a tamed one does, as a normal guh");
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(stray.isRemoved(), "a parade guh without a parade poofs");
            helper.assertTrue(snack.isRemoved(), "a knabbel without a kaasregen too");
            helper.succeed();
        });
    }

    // --- the scheduler and the rest ----------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void schedulerIsFairAndOnlyInTheGuhmension(GameTestHelper helper) {
        RandomSource random = RandomSource.create(1);
        long sum = 0;
        for (int i = 0; i < 2000; i++) {
            int delay = Evenementen.nextDelay(random, false);
            helper.assertTrue(delay >= 48000 && delay <= 72000, "between 2 and 3 days: " + delay);
            sum += delay;
            int first = Evenementen.nextDelay(random, true);
            helper.assertTrue(first >= 24000 && first <= 48000, "the first one a bit sooner: " + first);
        }
        helper.assertTrue(Math.abs(sum / 2000.0 - 60000) < 1500, "on average two and a half days");
        int[] seen = new int[EvenementType.values().length];
        for (int i = 0; i < 3000; i++) {
            EvenementType day = EvenementType.choose(random, false, null);
            helper.assertTrue(day != EvenementType.STERRENREGEN, "no falling stars in the daytime");
            EvenementType night = EvenementType.choose(random, true, EvenementType.PARADE);
            helper.assertTrue(night != EvenementType.PARADE, "never the same kind twice in a row");
            seen[night.ordinal()]++;
        }
        helper.assertTrue(seen[EvenementType.KAASREGEN.ordinal()] > 800 && seen[EvenementType.STERRENREGEN.ordinal()] > 800, "both others happen");
        ServerPlayer player = player(helper, 2, 2);
        helper.assertTrue(Evenementen.scheduleTick(player, 100000) == null, "no events outside the Guhmension");
        helper.assertTrue(!GuhQuests.saved(player).contains(Evenementen.WAITED), "and the clock doesn't run there");
        helper.assertTrue(Evenementen.whyNot(player) != null, "the command says why not");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void paradeOutfitAndCommandExist(GameTestHelper helper) {
        helper.assertTrue(GuhClothes.VADSPARADE_SJAKO.slot == GuhClothes.Slot.HEAD && GuhClothes.VADSPARADE_JASJE.slot == GuhClothes.Slot.BODY
                && GuhClothes.VADSPARADE_TROMMELTJE.slot == GuhClothes.Slot.NECK, "three pieces in three slots");
        try (var in = Guhs.class.getResourceAsStream("/assets/guhs/geo/entity/guh.geo.json")) {
            String geo = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String bone : List.of("outfit_parade_sjako", "outfit_parade_epaulet", "outfit_parade_trommel")) {
                helper.assertTrue(geo.contains("\"" + bone + "\""), "the guh model has " + bone);
            }
        } catch (Exception e) {
            helper.fail("can't read the guh model: " + e);
        }
        for (GuhClothes piece : Vadsparade.OUTFIT) {
            helper.assertTrue(Guhs.class.getResource("/assets/guhs/textures/entity/guh_clothes/" + piece.id() + ".png") != null, "texture of " + piece.id());
        }
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("guhs");
        var node = root == null ? null : root.getChild("evenement");
        helper.assertTrue(node != null && node.getChild("kaasregen") != null && node.getChild("parade") != null
                && node.getChild("sterrenregen") != null && node.getChild("stop") != null, "/guhs evenement <kaasregen|parade|sterrenregen|stop>");
        var source = helper.getLevel().getServer().createCommandSourceStack();
        helper.assertTrue(!node.canUse(source.withPermission(0)) && node.canUse(source.withPermission(2)), "for ops only");
        helper.succeed();
    }
}
