package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the Guheinde: the portal ring and the Oog van Vadsig, the knabbelkristallen, Opper-Mika and his starved
 * Enderguh (the floor while riding, feeding, stealing knabbels), Mika-larfjes, magere guhs, the Koningguh's story, the
 * rewards, the Knabbelkroon, the Enderguh-ei, the Guhvleugels, Mika-tranen, recipes and the templates. The tests that
 * need the Guheinde dimension itself are optional (dev server: /test runall).
 */
public class GuheindeGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static <T extends Entity> T spawn(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z) {
        T e = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        e.snapTo(at.x, at.y, at.z, 0f, 0f);
        helper.getLevel().addFreshEntity(e);
        return e;
    }

    // --- the portal ---------------------------------------------------------------------------------------------------

    private static BlockState frame(boolean oog) {
        return GuheindeFeature.KNABBELPORTAALFRAME.get().defaultBlockState().setValue(KnabbelportaalframeBlock.OOG, oog);
    }

    /** Twelve frames around a 3x3: the last Oog van Vadsig opens the portal; eleven don't. */
    @GuhTest(template = EMPTY)
    public static void twelveEyesOpenThePortal(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos c = new BlockPos(2, 1, 2);
        BlockPos last = c.offset(-1, 0, -2);
        for (int i = -1; i <= 1; i++) {
            for (BlockPos p : List.of(c.offset(i, 0, -2), c.offset(i, 0, 2), c.offset(-2, 0, i), c.offset(2, 0, i))) {
                helper.setBlock(p, frame(!p.equals(last)));
            }
        }
        helper.assertTrue(!KnabbelportaalframeBlock.ringComplete(helper.getLevel(), helper.absolutePos(c)), "eleven eyes are not enough");
        ItemStack eye = new ItemStack(GuheindeFeature.OOG_VAN_VADSIG.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, eye);
        BlockPos abs = helper.absolutePos(last);
        eye.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        helper.assertTrue(eye.getCount() == 1, "the eye went into the frame");
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                helper.assertBlockPresent(GuheindeFeature.GUHEINDE_PORTAAL.get(), c.offset(i, 0, j));
            }
        }
        done(helper, player);
    }

    /** Outside the Guhmension an Oog van Vadsig only blinks: not thrown, not used up. */
    @GuhTest(template = EMPTY)
    public static void theEyeOnlyWorksInTheGuhmension(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack eye = new ItemStack(GuheindeFeature.OOG_VAN_VADSIG.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, eye);
        eye.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(eye.getCount() == 3, "not used up");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(OogVanVadsigEntity.class, new AABB(player.blockPosition()).inflate(8)).isEmpty(), "not thrown");
        done(helper, player);
    }

    /** A thrown eye flies off and after 4 seconds drops back as an item (or breaks). */
    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void aThrownEyeFliesAndComesDown(GameTestHelper helper) {
        OogVanVadsigEntity eye = spawn(helper, GuheindeFeature.OOG_ENTITY.get(), 2.5, 2, 2.5);
        eye.setItem(new ItemStack(GuheindeFeature.OOG_VAN_VADSIG.get()));
        // (a close target, so the eye stays in the ticking chunks of the test; far signals are clamped to 12 blocks anyway)
        eye.signalTo(helper.absolutePos(new BlockPos(8, 2, 2)));
        boolean survives = eye.survives();
        Vec3 start = eye.position();
        boolean[] checked = {false};
        // polled: the eye is done after its 80 ticks of flight (the first ticks of a new entity may be skipped in a big batch)
        helper.onEachTick(() -> {
            if (checked[0] || !eye.isRemoved()) {
                return;
            }
            checked[0] = true;
            helper.assertTrue(eye.position().x > start.x + 2, "it flew towards the Knabbelkelder");
            boolean item = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(eye.blockPosition()).inflate(3),
                    i -> i.getItem().is(GuheindeFeature.OOG_VAN_VADSIG.get())).isEmpty();
            helper.assertTrue(item == survives, "an item exactly when it survives");
            helper.succeed();
        });
    }

    // --- crystals, Opper-Mika and his Enderguh ------------------------------------------------------------------------

    /** Smashing a knabbelkristal: it's gone and the stolen knabbels fly out. Opper-Mika can't smash his own. */
    @GuhTest(template = EMPTY)
    public static void smashedCrystalsGiveTheKnabbelsBack(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        KnabbelkristalEntity crystal = spawn(helper, GuheindeFeature.KNABBELKRISTAL_ENTITY.get(), 2.5, 1, 2.5);
        OpperMikaEntity mika = spawn(helper, GuheindeFeature.OPPER_MIKA.get(), 4.5, 1, 4.5);
        helper.assertTrue(!crystal.hurtOrSimulate(helper.getLevel().damageSources().mobAttack(mika), 5f) && !crystal.isRemoved(), "not by Opper-Mika");
        crystal.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 1f);
        helper.assertTrue(crystal.isRemoved(), "smashed");
        long knabbels = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(crystal.blockPosition()).inflate(4),
                i -> i.getItem().is(ModItems.KAAS_KNABBELS.get())).size();
        helper.assertTrue(knabbels == 6, "six knabbels, got " + knabbels);
        mika.discard();
        done(helper, player);
    }

    /** On his Enderguh, Opper-Mika never goes below half his health; on foot he does. */
    @GuhTest(template = EMPTY)
    public static void opperMikaHoldsOnWhileRiding(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        HongerigeEnderguhEntity mount = spawn(helper, GuheindeFeature.HONGERIGE_ENDERGUH.get(), 2.5, 3, 2.5);
        mount.setNoAi(true);
        OpperMikaEntity mika = spawn(helper, GuheindeFeature.OPPER_MIKA.get(), 2.5, 5, 2.5);
        helper.assertTrue(mika.startRiding(mount, true, true) && mika.isRiding(), "he rides");
        mika.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 120f);
        helper.assertTrue(mika.getHealth() == OpperMikaEntity.RIDING_FLOOR, "floor while riding, got " + mika.getHealth());
        mika.stopRiding();
        mika.invulnerableTime = 0;
        mika.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 20f);
        helper.assertTrue(mika.getHealth() < OpperMikaEntity.RIDING_FLOOR, "on foot it goes down");
        mount.discard();
        mika.discard();
        done(helper, player);
    }

    /** You can't hurt the Enderguh; it's scared until it's worn out; then a knabbel frees it and Opper-Mika falls off. */
    @GuhTest(template = EMPTY)
    public static void aKnabbelFreesTheEnderguh(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        HongerigeEnderguhEntity mount = spawn(helper, GuheindeFeature.HONGERIGE_ENDERGUH.get(), 2.5, 1, 2.5);
        mount.setNoAi(true);
        OpperMikaEntity mika = spawn(helper, GuheindeFeature.OPPER_MIKA.get(), 2.5, 3, 2.5);
        mika.startRiding(mount, true, true);
        helper.assertTrue(mount.getBbWidth() > 3f && mount.getBbHeight() > 2.5f, "a hitbox as big as its model, got " + mount.getBbWidth() + "x" + mount.getBbHeight());
        helper.assertTrue(!mount.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 50f), "no hitting the guh");
        mount.addVahoeg();
        helper.assertTrue(mount.getVahoeg() == 1, "a bit of colour back per crystal");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        mount.interact(player, InteractionHand.MAIN_HAND, mount.position());
        helper.assertTrue(player.getMainHandItem().getCount() == 3 && mika.isPassenger(), "too scared while there are crystals");
        mount.exhaust();
        mount.interact(player, InteractionHand.MAIN_HAND, mount.position());
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "it ate a knabbel");
        helper.assertTrue(mount.getToestand() == HongerigeEnderguhEntity.Toestand.VRIJ && mount.getVahoeg() == HongerigeEnderguhEntity.MAX_VAHOEG, "VAHOEG");
        helper.assertTrue(!mika.isPassenger(), "Opper-Mika is thrown off");
        mount.discard();
        mika.discard();
        done(helper, player);
    }

    /** On foot Opper-Mika steals your knabbels (four at a time) and drops them all when he's beaten. */
    @GuhTest(template = EMPTY)
    public static void opperMikaStealsKnabbelsAndGivesThemBack(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 10));
        OpperMikaEntity mika = spawn(helper, GuheindeFeature.OPPER_MIKA.get(), 2.5, 1, 2.5);
        mika.steal(player);
        helper.assertTrue(GuhQuests.count(player, ModItems.KAAS_KNABBELS.get()) == 6 && mika.buit() == 4, "four stolen");
        mika.dropBuit();
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(mika.blockPosition()).inflate(3),
                i -> i.getItem().is(ModItems.KAAS_KNABBELS.get())).stream().mapToInt(i -> i.getItem().getCount()).sum();
        helper.assertTrue(dropped == 4, "all four back, got " + dropped);
        mika.discard();
        done(helper, player);
    }

    /** A Mika-larfje steals a knabbel per bite and gives it back when squashed; aangevreten stones hide one. */
    @GuhTest(template = EMPTY)
    public static void mikaLarfjesStealKnabbels(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        helper.setBlock(new BlockPos(3, 1, 3), GuheindeFeature.AANGEVRETEN_KAASKORST_STENEN.get());
        BlockPos stone = helper.absolutePos(new BlockPos(3, 1, 3));
        Block.dropResources(helper.getLevel().getBlockState(stone), helper.getLevel(), stone, null, player, ItemStack.EMPTY);
        List<MikaLarfjeEntity> larfjes = helper.getLevel().getEntitiesOfClass(MikaLarfjeEntity.class, new AABB(stone).inflate(2));
        helper.assertTrue(larfjes.size() == 1, "a larfje crawls out");
        MikaLarfjeEntity larfje = larfjes.get(0);
        helper.assertTrue(larfje.getScale() < 0.5f, "tiny");
        larfje.doHurtTarget(helper.getLevel(), player);
        larfje.doHurtTarget(helper.getLevel(), player);
        helper.assertTrue(GuhQuests.count(player, ModItems.KAAS_KNABBELS.get()) == 3 && larfje.gestolen() == 2, "two knabbels gone");
        larfje.discard();
        done(helper, player);
    }

    // --- the Knabbelkelder's magere guhs, the Koningguh ----------------------------------------------------------------

    /**
     * A magere guh eats a knabbel: VAHOEG! A colourful guh runs off, the Guhdex gets a page with its star. 1.2.7: the grey
     * guh itself stays for the next player (IedereenGameTests has the second player).
     */
    @GuhTest(template = EMPTY)
    public static void aMagereGuhGoesVahoeg(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        GuhEntity guh = spawn(helper, ModEntities.GUH.get(), 3.5, 1, 3.5);
        guh.setVariant(GuhVariant.MAGER);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        player.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        List<GuhEntity> vrij = MagereCellen.vrij().stream().filter(g -> g.distanceToSqr(guh) < 64).toList();
        helper.assertTrue(vrij.size() == 1 && vrij.get(0).getVariant() != GuhVariant.MAGER, "a guh with its colour back");
        helper.assertTrue(guh.getVariant() == GuhVariant.MAGER && guh.isAlive(), "the grey one stays in its cell for the next player");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "it ate the knabbel");
        helper.assertTrue(!vrij.get(0).isTame(), "it runs off, it isn't yours");
        vrij.get(0).discard();
        GuhWorldData.PlayerData p = GuhWorldData.get(player.level().getServer()).player(player.getUUID());
        helper.assertTrue(p.seen.contains(GuhVariant.MAGER) && p.tamed.contains(GuhVariant.MAGER), "Guhdex page + star");
        helper.assertTrue(GuhQuests.saved(player).getIntOr(GuheindeEvents.GERED, 0) == 1, "counted");
        guh.discard();
        done(helper, player);
    }

    /** The Koningguh tells the story (and gives the book); after a win he knights you. */
    @GuhTest(template = EMPTY)
    public static void theKoningguhTellsTheStoryAndKnightsYou(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        GuhEntity koning = spawn(helper, ModEntities.GUH.get(), 3.5, 1, 3.5);
        koning.setVariant(GuhVariant.KONING);
        player.interactOn(koning, InteractionHand.MAIN_HAND, koning.position());
        helper.assertTrue(GuhQuests.saved(player).getIntOr(GuheindeEvents.KONING, 0) == 1, "story told");
        boolean book = false;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            book |= Guhboek.of(stack) == Guhboek.GUHEINDE;
        }
        helper.assertTrue(book, "the Guheinde book");
        player.interactOn(koning, InteractionHand.MAIN_HAND, koning.position());
        helper.assertTrue(GuhQuests.saved(player).getIntOr(GuheindeEvents.KONING, 0) == 1, "no knighthood before a win");
        GuhQuests.saved(player).putInt(GuheindeGevecht.WINS, 1);
        player.interactOn(koning, InteractionHand.MAIN_HAND, koning.position());
        helper.assertTrue(GuhQuests.saved(player).getIntOr(GuheindeEvents.KONING, 0) == 2, "Ridder van het Guheinde");
        koning.discard();
        done(helper, player);
    }

    // --- rewards and items ------------------------------------------------------------------------------------------

    /** First win: the Knabbelkroon, the trophy and a tamed Vahoege Enderguh; second win: an Enderguh-ei. */
    @GuhTest(template = EMPTY)
    public static void rewardsForBeatingOpperMika(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        GuheindeGevecht.reward(player, null, 250);
        helper.assertTrue(GuhQuests.count(player, GuheindeFeature.KNABBELKROON.get()) == 1, "the crown");
        helper.assertTrue(GuhQuests.count(player, GuheindeFeature.OPPER_MIKATROFEE.get().asItem()) == 1, "the trophy");
        List<GuhEntity> guhs = helper.getLevel().getEntitiesOfClass(GuhEntity.class, new AABB(player.blockPosition()).inflate(6),
                g -> g.getVariant() == GuhVariant.VAHOEGE_ENDER);
        helper.assertTrue(guhs.size() == 1 && guhs.get(0).isOwnedBy(player) && guhs.get(0).isSaddled() && guhs.get(0).isEnder(),
                "a tamed, saddled, flying Vahoege Enderguh");
        GuheindeGevecht.reward(player, null, 400);
        helper.assertTrue(GuhQuests.count(player, GuheindeFeature.ENDERGUH_EI.get().asItem()) == 1, "an egg the second time");
        helper.assertTrue(GuhQuests.saved(player).getIntOr(GuheindeGevecht.WINS, 0) == 2, "two wins");
        guhs.forEach(Entity::discard);
        done(helper, player);
    }

    /** The Knabbelkroon: Mika's take half as much again. */
    @GuhTest(template = EMPTY)
    public static void theCrownHitsMikasHarder(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        MikaEntity plain = spawn(helper, ModEntities.MIKA.get(), 3.5, 1, 1.5);
        MikaEntity crowned = spawn(helper, ModEntities.MIKA.get(), 3.5, 1, 3.5);
        plain.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 10f);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GuheindeFeature.KNABBELKROON.get()));
        crowned.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 10f);
        float a = MikaEntity.HEALTH - plain.getHealth(), b = MikaEntity.HEALTH - crowned.getHealth();
        helper.assertTrue(Math.abs(b - a * KnabbelkroonItem.MIKA_DAMAGE) < 0.01f, "1.5x: " + a + " vs " + b);
        plain.discard();
        crowned.discard();
        done(helper, player);
    }

    /** An Enderguh-ei cracks twice and then a baby Vahoege Enderguh hops out. */
    @GuhTest(template = EMPTY)
    public static void anEnderguhEiHatches(GameTestHelper helper) {
        BlockPos egg = new BlockPos(2, 1, 2);
        helper.setBlock(egg, GuheindeFeature.ENDERGUH_EI.get());
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(egg);
        for (int i = 0; i < 3; i++) {
            EnderguhEiBlock.crack(level.getBlockState(abs), level, abs);
        }
        helper.assertBlockNotPresent(GuheindeFeature.ENDERGUH_EI.get(), egg);
        List<GuhEntity> babies = level.getEntitiesOfClass(GuhEntity.class, new AABB(abs).inflate(2), g -> g.getVariant() == GuhVariant.VAHOEGE_ENDER);
        helper.assertTrue(babies.size() == 1 && babies.get(0).isBaby() && !babies.get(0).isTame(), "a wild baby Vahoege Enderguh");
        babies.forEach(Entity::discard);
        helper.succeed();
    }

    /** The Guhvleugels glide like an elytra and are mended with Mika's vet. */
    @GuhTest(template = EMPTY)
    public static void guhvleugelsGlide(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack wings = new ItemStack(GuheindeFeature.GUHVLEUGELS.get());
        helper.assertTrue(net.minecraft.world.entity.LivingEntity.canGlideUsing(wings, EquipmentSlot.CHEST), "can glide");
        helper.assertTrue(wings.isValidRepairItem(new ItemStack(ModItems.MIKA_VET.get())), "mend with Mika's vet");
        wings.setDamageValue(wings.getMaxDamage() - 1);
        helper.assertTrue(!net.minecraft.world.entity.LivingEntity.canGlideUsing(wings, EquipmentSlot.CHEST), "broken wings don't glide");
        done(helper, player);
    }

    /** Big Mika always cries Mika-tranen when beaten. */
    @GuhTest(template = EMPTY)
    public static void bigMikaCriesMikaTranen(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        MikaEntity mika = spawn(helper, ModEntities.MIKA.get(), 3.5, 1, 3.5);
        mika.makeBoss();
        mika.hurtOrSimulate(helper.getLevel().damageSources().playerAttack(player), 10000f);
        helper.runAfterDelay(2, () -> {
            int tears = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 1, 3))).inflate(4),
                    i -> i.getItem().is(GuheindeFeature.MIKA_TRAAN.get())).stream().mapToInt(i -> i.getItem().getCount()).sum();
            helper.assertTrue(tears >= 2, "at least two Mika-tranen, got " + tears);
            done(helper, player);
        });
    }

    /** The recipes: the eye, the crystal; and the Knabbelkelder library chest holds the Guheinde book. */
    @GuhTest(template = EMPTY)
    public static void recipesAndTheLibraryBook(GameTestHelper helper) {
        var recipes = helper.getLevel().recipeAccess();
        helper.assertTrue(recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Guhs.id("oog_van_vadsig"))).isPresent(), "the eye recipe");
        helper.assertTrue(recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Guhs.id("knabbelkristal"))).isPresent(), "the crystal recipe");
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Guhs.id("chests/knabbelkelder_bieb")));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        boolean book = table.getRandomItems(params).stream().anyMatch(s -> Guhboek.of(s) == Guhboek.GUHEINDE);
        helper.assertTrue(book, "the Guheinde book is in the library chest");
        helper.succeed();
    }

    /** The Knabbelberg template has the size GuheindeGevecht counts on; the Knabbelkelder and Mika-vesting exist. */
    @GuhTest(template = EMPTY)
    public static void theTemplatesAreThere(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        var berg = manager.get(Guhs.id("guheinde_knabbelberg"));
        helper.assertTrue(berg.isPresent(), "the Knabbelberg");
        var size = berg.get().getSize();
        helper.assertTrue(size.getX() == GuheindeGevecht.BERG_SIZE_X && size.getY() == GuheindeGevecht.BERG_SIZE_Y && size.getZ() == GuheindeGevecht.BERG_SIZE_Z,
                "Knabbelberg size " + size);
        for (String name : List.of("knabbelkelder", "mika_vesting", "mika_vesting_schip")) {
            helper.assertTrue(manager.get(Guhs.id(name)).isPresent(), name);
        }
        helper.succeed();
    }

    // --- the Guheinde itself (dev server only: the game test server has no mod dimensions) ------------------------------

    /** The island: the Knabbelberg, eight pillars with crystals, Opper-Mika on his Enderguh; the win opens everything. */
    @GuhTest(template = EMPTY, required = false, timeoutTicks = 400)
    public static void theGuheindeIslandAndTheFight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (level == null) {
            helper.fail("no Guheinde dimension (run this on the dev server)");
            return;
        }
        GuheindeGevecht fight = GuheindeGevecht.of(level);
        if (!fight.islandBuilt) {
            fight.buildIsland(level);
        }
        helper.assertTrue(level.getBlockState(fight.sokkels().get(0)).is(GuheindeFeature.KNABBELSOKKEL.get()), "the sokkels are where the fight expects them");
        for (int i = 0; i < GuheindeGevecht.PILLARS; i++) {
            helper.assertTrue(level.getBlockState(fight.pillarTop(i)).is(GuheindeFeature.KAASKORST_STENEN.get())
                    || level.getBlockState(fight.pillarTop(i)).is(nl.juiced.guhs.registry.ModBlocks.BLOCK_OF_KAASKNABBELS.get()), "pillar " + i);
        }
        helper.succeed();
    }

    /** Dying in the Guheinde: you keep everything except your kaasknabbels. */
    @GuhTest(template = EMPTY, required = false)
    public static void dyingInTheGuheindeKeepsYourThings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (level == null) {
            helper.fail("no Guheinde dimension (run this on the dev server)");
            return;
        }
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
                java.util.UUID.randomUUID(), "guheinde_test"));
        player.snapTo(GuheindeGevecht.ARRIVAL_X + 0.5, 100, 0.5);
        java.util.List<ItemEntity> drops = new java.util.ArrayList<>(List.of(
                new ItemEntity(level, 0, 100, 0, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5)),
                new ItemEntity(level, 0, 100, 0, new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD))));
        var event = new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(player, level.damageSources().generic(), drops, true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getDrops().size() == 1 && event.getDrops().iterator().next().getItem().is(ModItems.KAAS_KNABBELS.get()), "only the knabbels drop");
        helper.assertTrue(GuheindeReis.keptFor(player.getUUID()).size() == 1, "the sword is kept");
        helper.succeed();
    }

    // --- 2.8: half the Mika-vestingen have a vetschip, and Terugpoorten on the outer islands -----------------------------

    /** The start pool of the Mika-vesting: the tower alone and the tower with the vetschip, 1:1. */
    @GuhTest(template = EMPTY)
    public static void guheindeVestingIsHalfShips(GameTestHelper helper) {
        var pool = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL)
                .getValue(Guhs.id("mika_vesting/start"));
        helper.assertTrue(pool != null, "the vesting's start pool");
        int ship = 0, tower = 0;
        for (var element : pool.getShuffledTemplates(net.minecraft.util.RandomSource.create(1))) {
            String e = element.toString();
            if (e.contains("guhs:mika_vesting_schip")) {
                ship++;
            } else if (e.contains("guhs:mika_vesting")) {
                tower++;
            }
        }
        helper.assertTrue(ship > 0 && ship == tower, "vetschip : vesting = 1:1, not " + ship + ":" + tower);
        var json = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/data/guhs/worldgen/template_pool/mika_vesting/start.json");
        helper.assertTrue(json != null, "the pool file");
        for (var e : json.getAsJsonArray("elements")) {
            helper.assertTrue(e.getAsJsonObject().get("weight").getAsInt() == 1, "weight 1: " + e);
        }
        helper.succeed();
    }

    /**
     * The Terugpoort: its template (a gate of twelve poort blocks that lead back), its structure (only on the outer
     * islands, like the vestingen), its structure set (salt 20280901, kept away from the vestingen).
     */
    @GuhTest(template = EMPTY)
    public static void guheindeTerugpoortTemplateAndWorldgen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var template = level.getStructureManager().get(Guhs.id("guheinde_terugpoort"));
        helper.assertTrue(template.isPresent(), "the Terugpoort template");
        var infos = template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                GuheindeFeature.KNABBELPOORT.get());
        helper.assertTrue(infos.size() == 12, "a gate of 12 poort blocks: " + infos.size());
        for (var info : infos) {
            var be = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(info.pos(), info.state(), info.nbt(), level.registryAccess());
            helper.assertTrue(be instanceof KnabbelpoortBlock.Entity poort && poort.terug, "every poort block leads back: " + info.nbt());
        }
        var structure = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getValue(Guhs.id("guheinde_terugpoort"));
        helper.assertTrue(structure instanceof GuheindeEilandStructure, "only on the outer islands: " + structure);
        var set = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/data/guhs/worldgen/structure_set/guheinde_terugpoort.json");
        helper.assertTrue(set != null, "the structure set");
        var placement = set.getAsJsonObject("placement");
        helper.assertTrue(placement.get("salt").getAsInt() == 20280901, "salt 20280901");
        helper.assertTrue(placement.get("spacing").getAsInt() <= 32, "common: every few hundred blocks");
        helper.assertTrue("guhs:mika_vesting".equals(placement.getAsJsonObject("exclusion_zone").get("other_set").getAsString()),
                "kept away from the Mika-vestingen");
        helper.assertTrue(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET)
                .containsKey(Guhs.id("guheinde_terugpoort")), "the set is loaded");
        helper.succeed();
    }

    /**
     * Stepping into a Terugpoort far out on the outer islands brings you back to the main island: on its arrival
     * platform, on solid ground with room to stand, never in the void (dev server only: needs the Guheinde; forceload
     * the chunks at 0,0 .. 80,0 and 1504,0 first).
     */
    @GuhTest(template = EMPTY, required = false, timeoutTicks = 200)
    public static void guheindeTerugpoortBringsYouHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (level == null) {
            helper.fail("no Guheinde dimension (run this on the dev server)");
            return;
        }
        BlockPos at = new BlockPos(1504, level.getMaxY() + 1 - 20, 8);
        BlockState old = level.getBlockState(at);
        level.setBlock(at, GuheindeFeature.KNABBELPOORT.get().defaultBlockState(), 3);
        try {
            helper.assertTrue(level.getBlockEntity(at) instanceof KnabbelpoortBlock.Entity, "a poort");
            ((KnabbelpoortBlock.Entity) level.getBlockEntity(at)).terug = true;
            MikaEntity someone = ModEntities.MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
            someone.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            var transition = GuheindeFeature.KNABBELPOORT.get().getPortalDestination(level, someone, at);
            helper.assertTrue(transition != null && transition.newLevel() == level, "it leads somewhere in the Guheinde");
            Vec3 p = transition.position();
            helper.assertTrue(p.x * p.x + p.z * p.z < 150 * 150, "on the main island: " + p);
            BlockPos stand = BlockPos.containing(p);
            helper.assertTrue(p.y > level.getMinY() + 20, "not in the void: " + p);
            helper.assertTrue(level.getBlockState(stand.below()).isSolid(), "solid ground under your feet at " + stand);
            helper.assertTrue(level.getBlockState(stand).isAir() && level.getBlockState(stand.above()).isAir(), "room to stand at " + stand);
            someone.discard();
            com.mojang.logging.LogUtils.getLogger().info("guheindeTerugpoortBringsYouHome: OK, a Terugpoort at {} lands you on {} (block below: {})", at, stand, level.getBlockState(stand.below()));
        } finally {
            level.setBlock(at, old, 3);
        }
        helper.succeed();
    }
}
