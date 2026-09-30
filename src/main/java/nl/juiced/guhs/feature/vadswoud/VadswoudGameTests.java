package nl.juiced.guhs.feature.vadswoud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the Vadswoud: the wood set and the bark faces, the bush that never pricks, taming babies, guh families
 * (forming, the line behind the parent), sleeping in nests, the two shops, the giant guh tree (worldgen and saplings),
 * the tree-house village template, and the biome's share of the Guhmension (worked out from the dimension's own
 * biome source and noise, so it also runs on the GameTest server, which has no Guhmension).
 */
public class VadswoudGameTests {
    private static final String EMPTY = "empty";
    private static final String WEIDE = "vadswoud_weide";
    private static final String BOOMGROND = "vadswoud_boomgrond";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    private static GuhEntity guh(GameTestHelper helper, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setPersistenceRequired();
        return guh;
    }

    // --- blocks ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void vadshoutStripsAndFacesChangeTheirMood(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
        try {
            BlockPos log = new BlockPos(2, 1, 2);
            helper.setBlock(log, VadswoudFeature.VADSHOUT_STAM.get());
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            BlockPos abs = helper.absolutePos(log);
            axe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            helper.assertBlockPresent(VadswoudFeature.VADSHOUT_GESTRIPT.get(), log);

            BlockPos face = new BlockPos(3, 1, 3);
            helper.setBlock(face, VadswoudFeature.VADSHOUT_GEZICHT.get().defaultBlockState().setValue(VadshoutBlocks.Gezicht.STEMMING, 0));
            BlockPos absFace = helper.absolutePos(face);
            helper.getBlockState(face).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(absFace), Direction.NORTH, absFace, false));
            helper.assertTrue(helper.getBlockState(face).getValue(VadshoutBlocks.Gezicht.STEMMING) == 1, "a tap: another mood");

            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String r : List.of("vadshout_planken", "vadshout_trap", "vadshout_plaat", "vadshout_hek", "vadshout_poort", "vadshout_deur",
                    "vadshout_luik", "guhnestje", "knabbelbessentaartje", "vadstouw", "vadshout_gezicht")) {
                helper.assertTrue(recipes.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Guhs.id(r))).isPresent(), "a recipe for " + r);
            }
            helper.assertTrue(helper.getLevel().getBlockState(abs).is(net.minecraft.tags.BlockTags.LOGS_THAT_BURN), "vadshout burns like wood");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void knabbelbessenNeverPrickAndGrowBack(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(2, 1, 2));
        try {
            BlockPos bush = new BlockPos(2, 1, 2);
            helper.setBlock(bush.below(), VadswoudFeature.VADSMOS.get());
            helper.setBlock(bush, VadswoudFeature.KNABBELBESSENSTRUIK.get().defaultBlockState().setValue(KnabbelbessenstruikBlock.AGE, 3));
            BlockPos abs = helper.absolutePos(bush);
            float health = player.getHealth();
            for (int i = 0; i < 20; i++) {
                player.xOld = player.getX() - 0.3;          // walking through it
                helper.getBlockState(bush).entityInside(helper.getLevel(), abs, player, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
            }
            helper.assertTrue(player.getHealth() == health, "no pricking: " + player.getHealth());
            helper.getBlockState(bush).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
            helper.assertTrue(helper.getBlockState(bush).getValue(KnabbelbessenstruikBlock.AGE) == 1, "picked: it grows again from age 1");
            int berries = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2),
                    e -> e.getItem().is(VadswoudFeature.KNABBELBESSEN.get())).stream().mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(berries >= 2, "a ripe bush gives 2-3 knabbelbessen: " + berries);
            helper.assertTrue(VadswoudFeature.KNABBELBESSENSTRUIK.get().defaultBlockState().canSurvive(helper.getLevel(), abs),
                    "it grows on vadsmos");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    // --- taming babies ------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void babiesLoveKnabbelbessenGrownUpsDont(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
        try {
            int tamed = 0;
            for (int i = 0; i < 40; i++) {
                GuhEntity baby = guh(helper, new BlockPos(3, 1, 3));
                baby.setBaby(true);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(VadswoudFeature.KNABBELBESSEN.get(), 1));
                player.interactOn(baby, InteractionHand.MAIN_HAND, baby.position());
                helper.assertTrue(player.getMainHandItem().isEmpty(), "the baby eats the knabbelbes");
                if (baby.isTame()) {
                    tamed++;
                    helper.assertTrue(player.getUUID().equals(baby.getOwnerUUID()), "and comes along with you");
                }
                baby.discard();
            }
            helper.assertTrue(tamed >= 10 && tamed <= 34, "about half of the babies come along: " + tamed + " of 40");
            for (int i = 0; i < 10; i++) {
                GuhEntity grownUp = guh(helper, new BlockPos(3, 1, 3));
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(VadswoudFeature.KNABBELBESSEN.get(), 1));
                player.interactOn(grownUp, InteractionHand.MAIN_HAND, grownUp.position());
                helper.assertTrue(!grownUp.isTame() && player.getMainHandItem().getCount() == 1, "a grown-up guh wants kaasknabbels");
                grownUp.discard();
            }
            // kaasknabbels: a baby gets an extra chance on top of its own roll
            int babies = 0, adults = 0;
            for (int i = 0; i < 60; i++) {
                for (boolean isBaby : new boolean[]{true, false}) {
                    GuhEntity g = guh(helper, new BlockPos(3, 1, 3));
                    g.setBaby(isBaby);
                    g.setPersonality(nl.juiced.guhs.entity.GuhPersonality.SHY);   // (the hardest to tame: 1 in 5)
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 1));
                    player.interactOn(g, InteractionHand.MAIN_HAND, g.position());
                    if (g.isTame()) {
                        if (isBaby) {
                            babies++;
                        } else {
                            adults++;
                        }
                    }
                    g.discard();
                }
            }
            helper.assertTrue(babies > adults, "babies are easier to tame with kaasknabbels too: " + babies + " babies vs " + adults + " grown-ups (of 60)");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    // --- families -----------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void guhsThatSpawnTogetherBecomeAFamily(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<GuhEntity> guhs = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
                BlockPos at = helper.absolutePos(new BlockPos(1 + i, 1, 2));
                g.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                g.setVariant(i == 0 ? GuhVariant.CHOCO : GuhVariant.MINT);
                GuhGezin.join(level, g, 2, 2);                // (a family of two parents and two babies)
                guhs.add(g);
            }
            long id = GuhGezin.familyOf(guhs.get(0));
            helper.assertTrue(id != 0 && guhs.stream().allMatch(g -> GuhGezin.familyOf(g) == id), "one family");
            helper.assertTrue(GuhGezin.isParent(guhs.get(0)) && !guhs.get(0).isBaby(), "the first one is a parent");
            helper.assertTrue(guhs.stream().allMatch(g -> g.getVariant() == GuhVariant.CHOCO), "all the same variant as the first");
            long parents = guhs.stream().filter(GuhGezin::isParent).count();
            long babies = guhs.stream().filter(g -> g.isBaby() && !GuhGezin.isParent(g)).count();
            helper.assertTrue(parents == 2 && babies == 2, "2 parents and 2 babies: " + parents + " + " + babies);
            helper.assertTrue(GuhGezin.placeOf(guhs.get(2)) == 1 && GuhGezin.placeOf(guhs.get(3)) == 2, "the babies in line: 1, 2");
            helper.assertTrue(guhs.stream().filter(g -> !GuhGezin.isParent(g)).map(GuhGezin::placeOf).distinct().count() == babies,
                    "every baby has its own place in the line");

            // a guh that spawns alone: it gets babies of its own after its spawn tick
            GuhEntity alone = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            BlockPos far = helper.absolutePos(new BlockPos(2, 1, 2)).offset(12, 0, 0);
            alone.snapTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
            alone.setVariant(GuhVariant.SNOW);
            alone.setPersistenceRequired();
            GuhGezin.join(level, alone, 1, 2);
            level.addFreshEntity(alone);
            guhs.add(alone);
            int born = GuhGezin.closeFamilies(level, level.getGameTime() + 1);
            List<GuhEntity> kids = level.getEntitiesOfClass(GuhEntity.class, alone.getBoundingBox().inflate(3),
                    g -> g != alone && GuhGezin.familyOf(g) == GuhGezin.familyOf(alone));
            guhs.addAll(kids);
            helper.assertTrue(born >= 1 && kids.size() == born, "babies for the lonely parent: " + born);
            helper.assertTrue(kids.stream().allMatch(k -> k.isBaby() && k.getVariant() == GuhVariant.SNOW), "its own babies, snow guhs like it");
        } finally {
            guhs.forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GuhTest(template = WEIDE, timeoutTicks = 800)
    public static void babiesWalkInALineBehindTheirParent(GameTestHelper helper) {
        GuhEntity parent = guh(helper, new BlockPos(3, 1, 4));
        parent.setNoAi(true);
        GuhGezin.tag(parent, 4242, GuhGezin.OUDER, 0);
        List<GuhEntity> babies = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            GuhEntity baby = guh(helper, new BlockPos(3 + i, 1, 4));
            baby.setBaby(true);
            baby.setAge(-200000);                             // (they stay small during the test)
            baby.setPersonality(nl.juiced.guhs.entity.GuhPersonality.CUDDLY);
            GuhGezin.tag(baby, 4242, GuhGezin.BABY, i);
            babies.add(baby);
        }
        BlockPos end = helper.absolutePos(new BlockPos(22, 1, 4));
        helper.runAfterDelay(10, () -> parent.snapTo(end.getX() + 0.5, end.getY(), end.getZ() + 0.5));
        helper.succeedWhen(() -> {
            GuhEntity ahead = parent;
            for (GuhEntity baby : babies) {
                double d = baby.distanceTo(ahead);
                helper.assertTrue(d < 3.2, "baby " + GuhGezin.placeOf(baby) + " walks right behind the one ahead: " + String.format("%.1f", d));
                ahead = baby;
            }
            helper.assertTrue(babies.get(0).distanceTo(parent) < babies.get(1).distanceTo(parent)
                    && babies.get(1).distanceTo(parent) < babies.get(2).distanceTo(parent), "in a line: 1, 2, 3");
        });
    }

    @GuhTest(template = WEIDE, timeoutTicks = 900)
    public static void guhsSleepInANestAtNight(GameTestHelper helper) {
        BlockPos nest = new BlockPos(20, 1, 4);
        helper.setBlock(nest, VadswoudFeature.GUHNESTJE.get());
        GuhEntity guh = guh(helper, new BlockPos(4, 1, 4));
        guh.setPersonality(nl.juiced.guhs.entity.GuhPersonality.CUDDLY);
        BlockPos absNest = helper.absolutePos(nest);
        helper.startSequence()
                .thenExecute(() -> SleepInNestGoal.TEST_NIGHT.add(guh.getUUID()))
                .thenWaitUntil(() -> {
                    double d = Math.hypot(guh.getX() - absNest.getX() - 0.5, guh.getZ() - absNest.getZ() - 0.5);
                    helper.assertTrue(SleepInNestGoal.isAsleep(guh) && guh.isInSittingPose() && d < 0.9,
                            "asleep in the nest: " + SleepInNestGoal.isAsleep(guh) + ", " + String.format("%.1f", d) + " from it");
                })
                .thenExecute(() -> SleepInNestGoal.TEST_NIGHT.remove(guh.getUUID()))
                .thenWaitUntil(() -> helper.assertTrue(!SleepInNestGoal.isAsleep(guh) && !guh.isInSittingPose(), "morning: awake again"))
                .thenSucceed();
    }

    @GuhTest(template = WEIDE, timeoutTicks = 900)
    public static void tameGuhsOnlyGoToBedWhileYoureNear(GameTestHelper helper) {
        BlockPos nest = new BlockPos(20, 1, 4);
        helper.setBlock(nest, VadswoudFeature.GUHNESTJE.get());
        ServerPlayer owner = player(helper, new BlockPos(12, 1, 6));
        GuhEntity guh = guh(helper, new BlockPos(4, 1, 4));
        guh.tame(owner);
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        helper.startSequence()
                .thenExecute(() -> SleepInNestGoal.TEST_NIGHT.add(guh.getUUID()))
                .thenWaitUntil(() -> helper.assertTrue(SleepInNestGoal.isAsleep(guh), "your guh sleeps in the nest while you're around"))
                .thenExecute(() -> owner.snapTo(owner.getX() + 60, owner.getY(), owner.getZ()))
                .thenWaitUntil(() -> helper.assertTrue(!SleepInNestGoal.isAsleep(guh), "you walk away: it wakes up and comes along"))
                .thenExecute(() -> {
                    SleepInNestGoal.TEST_NIGHT.remove(guh.getUUID());
                    leave(helper, owner);
                })
                .thenSucceed();
    }

    // --- the two characters ----------------------------------------------------------------------------------------------------

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    @GuhTest(template = EMPTY)
    public static void theBoswachterguhAndTheKnabbelplukkerTrade(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
        GuhNpcEntity boswachter = npc(helper, GuhNpcEntity.Kind.BOSWACHTERGUH);
        GuhNpcEntity plukker = npc(helper, GuhNpcEntity.Kind.KNABBELPLUKKER);
        try {
            var offers = boswachter.getOffers();
            for (var result : List.of(VadswoudFeature.VADSHOUT_ZAAILING.get().asItem(), VadswoudFeature.GUHNESTJE.get().asItem(),
                    VadswoudFeature.VADSHOUT_STAM.get().asItem(), ModItems.clothingItem(GuhClothes.BOSWACHTERSHOED), ModItems.clothingItem(GuhClothes.BOSWACHTERSJAS))) {
                helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(result) && o.getBaseCostA().is(ModItems.KAAS_KNABBELS.get())),
                        "the Boswachterguh sells " + result + " for kaasknabbels");
            }
            var trades = plukker.getOffers();
            helper.assertTrue(trades.stream().anyMatch(o -> o.getResult().is(VadswoudFeature.KNABBELBESSEN.get())), "she sells knabbelbessen");
            helper.assertTrue(trades.stream().anyMatch(o -> o.getBaseCostA().is(VadswoudFeature.KNABBELBESSEN.get()) && o.getResult().is(ModItems.KAAS_KNABBELS.get())),
                    "and buys them");
            for (GuhClothes c : List.of(GuhClothes.PLUKMUTS, GuhClothes.PLUKMANDJE)) {
                helper.assertTrue(trades.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(c)) && o.getBaseCostA().is(VadswoudFeature.KNABBELBESSEN.get())),
                        "the picker's outfit for knabbelbessen: " + c.id());
            }
            VadswoudFeature.boswachterguh().talk(boswachter, player);
            player.closeContainer();
            VadswoudFeature.knabbelplukker().talk(plukker, player);
            player.closeContainer();
            helper.assertTrue(GuhQuests.saved(player).getBooleanOr(Boswachterguh.MET_KEY, false) && GuhQuests.saved(player).getBooleanOr(Knabbelplukker.MET_KEY, false),
                    "they remember you");
            helper.assertTrue(GuhVariant.ofCharacter(GuhNpcEntity.Kind.BOSWACHTERGUH) == GuhVariant.BOSWACHTERGUH
                    && GuhDex.ENTRIES.contains(GuhVariant.BOSWACHTERGUH) && GuhDex.ENTRIES.contains(GuhVariant.KNABBELPLUKKER)
                    && !GuhDex.TAMEABLE.contains(GuhVariant.KNABBELPLUKKER), "both have a Guhdex page (seen, not tamed)");
            helper.assertTrue(SuperkompasItem.allowed("boomhutdorp"), "the super compass finds the boomhutdorp");
            helper.assertTrue(GuhClothes.PLUKMANDJE.slot == GuhClothes.Slot.BACK && GuhClothes.BOSWACHTERSHOED.slot == GuhClothes.Slot.HEAD,
                    "the clothes go in the right slots");
        } finally {
            boswachter.discard();
            plukker.discard();
            leave(helper, player);
        }
        helper.succeed();
    }

    // --- the giant guh tree -------------------------------------------------------------------------------------------------------

    /** (The floor of the tree room, again: the game test may have cleared it for a neighbour.) */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 33; x++) {
            for (int z = 0; z < 33; z++) {
                if (!helper.getBlockState(new BlockPos(x, 0, z)).is(VadswoudFeature.VADSMOS.get())) {
                    helper.setBlock(new BlockPos(x, 0, z), VadswoudFeature.VADSMOS.get());
                }
            }
        }
    }

    @GuhTest(template = BOOMGROND, timeoutTicks = 100)
    public static void aGiantGuhTreeWithFacesThatDoesNotWilt(GameTestHelper helper) {
        floor(helper);
        BlockPos origin = helper.absolutePos(new BlockPos(15, 1, 15));
        var tree = ReuzenguhboomFeature.grow(helper.getLevel(), helper.getLevel().getRandom(), origin, true);
        helper.assertTrue(tree != null, "the tree grows (floor: " + helper.getBlockState(new BlockPos(15, 0, 15)) + ")");
        helper.assertTrue(tree.logs() >= 250 && tree.leaves() >= 300 && tree.faces() >= 2 && tree.height() >= 22,
                "a real giant: " + tree);
        int bad = 0, leaves = 0;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-15, 0, -15), origin.offset(16, 46, 16))) {
            BlockState s = helper.getLevel().getBlockState(p);
            if (s.is(VadswoudFeature.VADSHOUT_BLADEREN.get())) {
                leaves++;
                if (s.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE || s.getValue(LeavesBlock.PERSISTENT)) {
                    bad++;
                }
            }
        }
        helper.assertTrue(leaves == tree.leaves() && bad == 0, "every leaf is fed by a log (no decay): " + bad + " of " + leaves);
        helper.succeed();
    }

    @GuhTest(template = BOOMGROND, timeoutTicks = 100)
    public static void fourSaplingsGrowAGiantGuhTree(GameTestHelper helper) {
        floor(helper);
        for (BlockPos p : List.of(new BlockPos(15, 1, 15), new BlockPos(16, 1, 15), new BlockPos(15, 1, 16), new BlockPos(16, 1, 16))) {
            helper.setBlock(p, VadswoudFeature.VADSHOUT_ZAAILING.get());
        }
        BlockPos corner = helper.absolutePos(new BlockPos(15, 1, 15));
        BlockState sapling = helper.getLevel().getBlockState(corner).setValue(SaplingBlock.STAGE, 1);
        ((SaplingBlock) sapling.getBlock()).advanceTree(helper.getLevel(), corner, sapling, helper.getLevel().getRandom());
        int trunk = 0;
        for (BlockPos p : BlockPos.betweenClosed(corner.offset(-1, 5, -1), corner.offset(2, 5, 2))) {
            if (helper.getLevel().getBlockState(p).is(VadswoudFeature.VADSHOUT_STAM.get()) || helper.getLevel().getBlockState(p).is(VadswoudFeature.VADSHOUT_GEZICHT.get())) {
                trunk++;
            }
        }
        helper.assertTrue(trunk == 16, "a thick trunk where the four saplings were: " + trunk + " of 16");
        helper.succeed();
    }

    // --- the tree-house village ------------------------------------------------------------------------------------------------------

    @GuhTest(template = "boomhutdorp", timeoutTicks = 100)
    public static void theTreeHouseVillageIsComplete(GameTestHelper helper) {
        AABB area = helper.getBounds().inflate(1);
        ServerLevel level = helper.getLevel();
        var npcs = level.getEntitiesOfClass(GuhNpcEntity.class, area);
        helper.assertTrue(npcs.stream().filter(n -> n.getKind() == GuhNpcEntity.Kind.BOSWACHTERGUH).count() == 1
                && npcs.stream().filter(n -> n.getKind() == GuhNpcEntity.Kind.KNABBELPLUKKER).count() == 1, "the Boswachterguh and the Knabbelplukker");
        var guhs = level.getEntitiesOfClass(GuhEntity.class, area);
        long babies = guhs.stream().filter(g -> g.isBaby() && GuhGezin.familyOf(g) != 0).count();
        helper.assertTrue(babies >= 8, "guh families with babies: " + babies);
        Map<String, Integer> counts = new HashMap<>();
        BlockPos lo = new BlockPos((int) area.minX, (int) area.minY, (int) area.minZ), hi = new BlockPos((int) area.maxX, (int) area.maxY, (int) area.maxZ);
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            BlockState s = level.getBlockState(p);
            String key = s.is(VadswoudFeature.GUHNESTJE.get()) ? "nests" : s.is(VadswoudFeature.VADSHOUT_GEZICHT.get()) ? "faces"
                    : s.is(Blocks.LADDER) ? "ladders" : s.is(VadswoudFeature.VADSTOUW.get()) ? "rope" : s.getBlock() instanceof ChestBlock ? "chests"
                    : s.is(VadswoudFeature.KNABBELBESSENSTRUIK.get()) ? "bushes" : s.is(VadswoudFeature.VADSHOUT_DEUR.get()) ? "doors"
                    : s.is(VadswoudFeature.VADSHOUT_STAM.get()) ? "logs" : null;
            if (key != null) {
                counts.merge(key, 1, Integer::sum);
            }
        }
        LOGGER.info("boomhutdorp: {}", counts);
        for (var want : Map.of("nests", 8, "faces", 20, "ladders", 90, "rope", 60, "chests", 5, "bushes", 20, "doors", 12, "logs", 5000).entrySet()) {
            helper.assertTrue(counts.getOrDefault(want.getKey(), 0) >= want.getValue(), want.getKey() + ": " + counts.getOrDefault(want.getKey(), 0));
        }
        helper.succeed();
    }

    // --- worldgen: the Vadswoud's share of the Guhmension --------------------------------------------------------------------------

    /**
     * Samples the Guhmension's surface biomes with and without the Vadswoud (from the dimension JSON and its noise
     * settings, three seeds): the Vadswoud gets a fair share and every other surface biome keeps most of its own.
     */
    @GuhTest(template = EMPTY, timeoutTicks = 400)
    public static void theVadswoudTakesItsShareAndLeavesTheRest(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        JsonObject without = source.deepCopy();
        JsonArray kept = new JsonArray();
        for (JsonElement e : without.getAsJsonArray("biomes")) {
            if (!e.getAsJsonObject().get("biome").getAsString().equals("guhs:vadswoud")) {
                kept.add(e);
            }
        }
        without.add("biomes", kept);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource with = BiomeSource.CODEC.parse(ops, source).getOrThrow();
        BiomeSource before = BiomeSource.CODEC.parse(ops, without).getOrThrow();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        Map<String, Integer> now = new HashMap<>(), then = new HashMap<>();
        int samples = 0;
        for (long seed : new long[]{1L, 20270501L, -778899L}) {
            Climate.Sampler sampler = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed).sampler();
            for (int x = -3000; x < 3000; x += 40) {
                for (int z = -3000; z < 3000; z += 40) {
                    int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(100), qz = QuartPos.fromBlock(z);
                    now.merge(with.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().identifier().getPath(), 1, Integer::sum);
                    then.merge(before.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().identifier().getPath(), 1, Integer::sum);
                    samples++;
                }
            }
        }
        StringBuilder report = new StringBuilder();
        for (String b : new java.util.TreeSet<>(then.keySet())) {
            report.append(String.format("%s %.1f%% -> %.1f%%; ", b, 100.0 * then.get(b) / samples, 100.0 * now.getOrDefault(b, 0) / samples));
        }
        double share = now.getOrDefault("vadswoud", 0) / (double) samples;
        report.append(String.format("vadswoud %.1f%%", 100 * share));
        LOGGER.info("Guhmension surface biomes (before -> with the Vadswoud): {}", report);
        helper.assertTrue(share >= 0.03 && share <= 0.30, "the Vadswoud's share: " + report);
        for (var e : then.entrySet()) {
            double was = e.getValue() / (double) samples, is = now.getOrDefault(e.getKey(), 0) / (double) samples;
            if (was >= 0.01) {
                helper.assertTrue(is >= 0.4 * was, e.getKey() + " keeps most of its share: " + report);
            }
        }
        helper.succeed();
    }
}
