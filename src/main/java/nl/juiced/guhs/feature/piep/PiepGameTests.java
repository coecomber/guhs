package nl.juiced.guhs.feature.piep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.Bakken;
import nl.juiced.guhs.feature.bakkerij.Recept;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of Piep: taming a muisje, picking it up and putting it back (name and owner kept), the shoulder, verstoppertje,
 * Poepschilly's poetsbeurt ("Fris van binnen"), the recipe unlock and the Knabbeloven, the koek tray (6 koeken, eating, drops),
 * the nest fight (3 waves, the Oppernabbel, the treasure chest with the recipe once), the spawn hook (roll and dimension
 * filter), the knabbel loot, and the FTB chapter file. (Templates piep_test_wei: grass at helper y 1; piep_test_arena:
 * a 23 x 23 floor at helper y 1 with the kern in the middle.)
 */
public class PiepGameTests {
    private static final String WEI = "piep_test_wei";
    private static final String ARENA = "piep_test_arena";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    // --- the pieppiepmuisje ------------------------------------------------------------------------------------------------------

    @GuhTest(template = WEI, timeoutTicks = 60)
    public static void piepMuisjeAaienEnTemmen(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(4, 2, 4));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        muis.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.GEAAID) == 1 && advancement(p, "piep_muisje_geaaid"), "petting counts");
        helper.assertTrue(KnusVoortgang.heeft(p, PiepVoortgang.PIEPBOEK, "pieppiepmuisje"), "the piepboek page is filled in");
        // "Lief kijken": the first kaasknabbel always tames it
        p.addEffect(new MobEffectInstance(PiepFeature.LIEF_KIJKEN, 200));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        muis.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(muis.isTame() && muis.isOwnedBy(p), "tamed with one knabbel while looking lief");
        helper.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2, "the knabbel is eaten");
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.GETAMED) == 1 && advancement(p, "piep_muisje_getamed"), "taming counts");
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, timeoutTicks = 60)
    public static void piepMuisjeOppakkenEnNeerzetten(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(4, 2, 4));
        muis.tame(p);
        muis.setCustomName(Component.literal("Pieps"));
        muis.setHealth(5f);
        MuisjeItem.pakOp(muis, p);
        helper.assertTrue(muis.isRemoved(), "the muisje left the world");
        int slot = -1;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(PiepFeature.PIEPPIEPMUISJE_ITEM.get())) {
                slot = i;
            }
        }
        helper.assertTrue(slot >= 0, "it is in the pockets");
        ItemStack stack = p.getInventory().getItem(slot);
        helper.assertTrue(stack.has(DataComponents.CUSTOM_DATA) && stack.getHoverName().getString().equals("Pieps"), "the item keeps the muisje (and its name)");
        ServerLevel level = helper.getLevel();
        PieppiepmuisjeEntity terug = MuisjeItem.zetNeer(stack, level, helper.absoluteVec(new Vec3(6.5, 2, 6.5)), 0);
        helper.assertTrue(terug != null && terug.isTame() && terug.isOwnedBy(p), "put back: still tame and still yours");
        helper.assertTrue(terug.getCustomName() != null && terug.getCustomName().getString().equals("Pieps") && terug.getHealth() == 5f,
                "its name and health came along");
        // the shoulder: on, and off again onto a block
        Schouder.zet(p, terug);
        helper.assertTrue(terug.isRemoved() && Schouder.heeft(p), "on the shoulder (not in the world)");
        PieppiepmuisjeEntity eraf = (PieppiepmuisjeEntity) Schouder.eraf(p, helper.absoluteVec(new Vec3(3.5, 2, 3.5)));
        helper.assertTrue(eraf != null && !Schouder.heeft(p) && eraf.isOwnedBy(p) && "Pieps".equals(eraf.getCustomName().getString()),
                "hopped down, still Pieps and yours");
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, timeoutTicks = 60)
    public static void piepVerstoppertje(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pot = new BlockPos(6, 2, 6);
        helper.setBlock(pot, Blocks.FLOWER_POT);
        helper.assertTrue(helper.getBlockState(pot).is(PiepFeature.VERSTOPPLEKKEN), "a flower pot is a hiding place");
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(3, 2, 3));
        muis.tame(p);
        helper.assertTrue(muis.verstop().kiesPlek() && helper.absolutePos(pot).equals(pickedSpot(muis)), "it picks the pot to hide in");
        muis.verstop().verstopBij(helper.absolutePos(pot), true);
        helper.assertTrue(muis.isVerstopt() && muis.isInvisible(), "hidden (and invisible)");
        helper.assertTrue(muis.verstop().bij(helper.absolutePos(pot)), "right-clicking the pot finds it");
        muis.verstop().gevonden(p);
        helper.assertTrue(!muis.isVerstopt() && !muis.isInvisible(), "found: out it comes");
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.GEVONDEN) == 1, "the Guhdex counts it");
        for (int i = 0; i < 2; i++) {
            muis.verstop().verstopBij(helper.absolutePos(pot), true);
            muis.verstop().gevonden(p);
        }
        helper.assertTrue(advancement(p, "piep_verstoppertje_3"), "found 3 times: the milestone (and its quest)");
        leave(helper, p);
        helper.succeed();
    }

    private static BlockPos pickedSpot(PieppiepmuisjeEntity muis) {
        muis.verstop().verstop();
        BlockPos p = muis.verstop().plek();
        muis.verstop().uit();
        return p;
    }

    // --- Poepschilly ---------------------------------------------------------------------------------------------------------------

    @GuhTest(template = WEI, timeoutTicks = 400)
    public static void piepPoepschillyPoetsbeurt(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 5));
        guh.tame(p);
        guh.setNoAi(true);
        PoepschillyEntity schilly = helper.spawn(PiepFeature.POEPSCHILLY.get(), new BlockPos(3, 2, 5));
        p.addEffect(new MobEffectInstance(PiepFeature.LIEF_KIJKEN, 200));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.KELP, 2));
        schilly.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(schilly.isTame() && advancement(p, "piep_schilly_getamed"), "kelp tames it (looking lief)");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        schilly.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(!schilly.klaarVoor(p), "a click opens its menu (piepmenu)");
        PiepMenu.doe(p, schilly, PiepMenu.Actie.SPECIAAL, 0, "");
        helper.assertTrue(schilly.klaarVoor(p), "ready after Kontje poetsen!");
        PoepschillyEntity.klikOpGuh(guh, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(schilly.isAanHetPoetsen(), "clicking the guh starts the poetsbeurt");
        boolean[] binnen = {false};
        helper.onEachTick(() -> binnen[0] |= schilly.isBinnen() && schilly.isInvisible());
        helper.succeedWhen(() -> {
            helper.assertTrue(binnen[0], "it was inside the guh");
            helper.assertTrue(!schilly.isBinnen() && !schilly.isAanHetPoetsen(), "popped out again");
            helper.assertTrue(guh.hasEffect(PiepFeature.FRIS_VAN_BINNEN), "the guh is fris van binnen");
            helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.POETSBEURTEN) == 1 && advancement(p, "piep_poetsbeurt"), "the poetsbeurt counts");
            PiepMenu.doe(p, schilly, PiepMenu.Actie.SPECIAAL, 0, "");
            helper.assertTrue(!schilly.klaarVoor(p), "and now it rests a while");
            leave(helper, p);
        });
    }

    @GuhTest(template = WEI, timeoutTicks = 200)
    public static void piepSchillyBestieEnBeef(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 5));
        guh.tame(p);
        guh.setNoAi(true);
        SchillyEntity schilly = helper.spawn(PiepFeature.SCHILLY.get(), new BlockPos(3, 2, 5));
        p.addEffect(new MobEffectInstance(PiepFeature.LIEF_KIJKEN, 200));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        schilly.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(schilly.isTame() && advancement(p, "piep_schilly2_getamed") && !advancement(p, "piep_schilly_getamed"),
                "Schilly is tamed (its own quest, not Poepschilly's)");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PiepMenu.doe(p, schilly, PiepMenu.Actie.SPECIAAL, 0, "");
        helper.assertTrue(schilly.klaarVoor(p), "ready after Bestie-moment!");
        PoepschillyEntity.klikOpGuh(guh, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(!schilly.isAanHetPoetsen() && !schilly.isBinnen(), "Schilly never cleans a guh");
        helper.assertTrue(guh.hasEffect(PiepFeature.BESTIES) && schilly.hasEffect(PiepFeature.BESTIES), "a bestie-moment: both are Besties");
        helper.assertTrue(!guh.hasEffect(PiepFeature.FRIS_VAN_BINNEN), "and nobody is cleaned");
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.BESTIES) == 1 && advancement(p, "piep_schilly_bestie"), "the bestie-moment counts");
        PiepMenu.doe(p, schilly, PiepMenu.Actie.SPECIAAL, 0, "");
        helper.assertTrue(!schilly.klaarVoor(p), "then it rests");
        // beef: backs turned, and made up again (counts for everyone near)
        schilly.beefBegin(guh);
        float schillyYaw = schilly.getYRot(), guhYaw = guh.getYRot();
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(schillyYaw - guhYaw)) > 150, "they turn their backs on each other");
        helper.assertTrue(nl.juiced.guhs.feature.knus.GuhHooks.isBezig(guh), "the guh sulks a moment");
        schilly.beefBijgelegd(guh);
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.BEEF) == 1 && advancement(p, "piep_schilly_beef"), "...toch besties: counted");
        helper.assertTrue(!nl.juiced.guhs.feature.knus.GuhHooks.isBezig(guh), "and the guh is free again");
        helper.assertTrue(KnusVoortgang.heeft(p, PiepVoortgang.PIEPBOEK, "schilly"), "Schilly has its own piepboek page");
        leave(helper, p);
        helper.succeed();
    }

    // --- the recipe and the koek ---------------------------------------------------------------------------------------------------

    @GuhTest(template = WEI, timeoutTicks = 60)
    public static void piepReceptLerenEnBakken(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        helper.assertTrue(Recept.van(Recept.Deeg.ZOETDEEG, Recept.Vorm.PLAATJE, Recept.Topping.GLAZUUR) == Recept.ROZE_GUH_KOEK
                && !Recept.ROZE_GUH_KOEK.inBoek() && !Recept.BOEK.contains(Recept.ROZE_GUH_KOEK), "zoetdeeg + plaatje + glazuur, not in the receptenboek");
        helper.assertTrue(BakkerijFeature.bakje(Recept.ROZE_GUH_KOEK) == PiepFeature.ROZE_GUH_KOEK_ITEM.get(), "the oven makes our koek");
        helper.assertTrue(new ItemStack(PiepFeature.ROZE_GUH_KOEK_ITEM.get()).is(KnusTags.GEBAK), "the koek is gebak");
        BlockPos oven = new BlockPos(4, 2, 4);
        helper.setBlock(oven, BakkerijFeature.KNABBELOVEN.get());
        BlockPos abs = helper.absolutePos(oven);
        p.snapTo(abs.getX() + 1.5, abs.getY(), abs.getZ() + 0.5);
        helper.assertTrue("gui.guhs.piep.recept_onbekend".equals(Bakken.start(p, abs, Recept.Deeg.ZOETDEEG, Recept.Vorm.PLAATJE, Recept.Topping.GLAZUUR)),
                "unknown recipe: the oven says no");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PiepFeature.ROZE_GUH_KOEK_RECEPT.get()));
        PiepFeature.ROZE_GUH_KOEK_RECEPT.get().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(ReceptItem.kent(p) && advancement(p, "piep_recept"), "learned (per player)");
        helper.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the recipe paper is used up");
        String waarom = Bakken.start(p, abs, Recept.Deeg.ZOETDEEG, Recept.Vorm.PLAATJE, Recept.Topping.GLAZUUR);
        helper.assertTrue(!"gui.guhs.piep.recept_onbekend".equals(waarom), "now the oven would bake it (" + waarom + ")");
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, timeoutTicks = 60)
    public static void piepKoekBakje(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, PiepFeature.ROZE_GUH_KOEK.get().defaultBlockState().setValue(RozeGuhKoekBlock.KOEKEN, 6));
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        BlockState vol = level.getBlockState(abs);
        var drops = net.minecraft.world.level.block.Block.getDrops(vol, level, abs, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(PiepFeature.ROZE_GUH_KOEK_ITEM.get()) && drops.get(0).getCount() == 6,
                "a full tray drops all six: " + drops);
        // eat one with an empty hand
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.getFoodData().setFoodLevel(10);
        vol.useWithoutItem(level, p, new net.minecraft.world.phys.BlockHitResult(abs.getCenter(), Direction.UP, abs, false));
        helper.assertTrue(level.getBlockState(abs).getValue(RozeGuhKoekBlock.KOEKEN) == 5, "one eaten");
        helper.assertTrue(p.hasEffect(PiepFeature.LIEF_KIJKEN) && p.getFoodData().getFoodLevel() > 10, "food and Lief kijken");
        helper.assertTrue(advancement(p, "piep_koek") && KnusVoortgang.teller(p, PiepVoortgang.KOEKJES) == 1, "counts for the quest and the Guhdex");
        // sneak: take one
        p.setShiftKeyDown(true);
        level.getBlockState(abs).useWithoutItem(level, p, new net.minecraft.world.phys.BlockHitResult(abs.getCenter(), Direction.UP, abs, false));
        helper.assertTrue(level.getBlockState(abs).getValue(RozeGuhKoekBlock.KOEKEN) == 4 && count(p, PiepFeature.ROZE_GUH_KOEK_ITEM.get()) == 1,
                "sneaking picks one up");
        p.setShiftKeyDown(false);
        for (int i = 0; i < 4; i++) {
            RozeGuhKoekBlock.eentjeMinder(level, abs, level.getBlockState(abs));
        }
        helper.assertTrue(level.getBlockState(abs).isAir(), "the tray is gone when it is empty");
        leave(helper, p);
        helper.succeed();
    }

    // --- the nest ---------------------------------------------------------------------------------------------------------------------

    // (1.1.0: own batch - 26.1 runs tests in id order, so piepNestTweedeKeerZonderRecept's arena stood next to this one
    //  and its wins counted for this test's player too)
    @GuhTest(template = ARENA, batch = "piep_nest_golven", timeoutTicks = 1400)
    public static void piepNestGolvenEnOppernabbel(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        p.setGameMode(GameType.CREATIVE);                      // (the knabbels leave a creative player alone)
        ServerLevel level = helper.getLevel();
        BlockPos kern = helper.absolutePos(new BlockPos(11, 1, 11));
        p.snapTo(kern.getX() + 0.5, kern.getY() + 1, kern.getZ() + 0.5);
        KaasknabbelNest.Gevecht g = KaasknabbelNest.gevecht(level, "test_" + kern.asLong(), kern);
        KaasknabbelNest.start(level, g);
        helper.assertTrue(g.fase() == KaasknabbelNest.Fase.GOLF && g.levend(level) == KaasknabbelNest.GOLVEN[0], "wave 1: four knabbels");
        int[] golvenGezien = {1};
        helper.onEachTick(() -> {
            if (g.fase() == KaasknabbelNest.Fase.GOLF && g.golf() + 1 > golvenGezien[0]) {
                golvenGezien[0] = g.golf() + 1;
            }
            // beat everything that is out (the boss too)
            for (BozeKaasknabbelEntity k : level.getEntitiesOfClass(BozeKaasknabbelEntity.class, new net.minecraft.world.phys.AABB(kern).inflate(14))) {
                if (k.isAlive() && k.nest().equals(g.key())) {
                    k.hurt(level.damageSources().playerAttack(p), 1000f);
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(g.fase() == KaasknabbelNest.Fase.GEWONNEN, "won (now " + g.fase() + ", wave " + g.golf() + ")");
            helper.assertTrue(golvenGezien[0] == KaasknabbelNest.GOLVEN.length, "three waves came: " + golvenGezien[0]);
            helper.assertTrue(g.receptGegeven(), "the recipe came out");
            helper.assertTrue(level.getBlockEntity(kern.above()) instanceof ChestBlockEntity, "the treasure chest stands on the kern");
            helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.NEST) == 1 && advancement(p, "piep_nest_gewonnen"), "the win counts");
            helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.KNABBELS) >= 18, "every beaten knabbel counts: " + KnusVoortgang.teller(p, PiepVoortgang.KNABBELS));
            level.removeBlock(kern.above(), false);
            leave(helper, p);
        });
    }

    @GuhTest(template = ARENA, timeoutTicks = 100)
    public static void piepNestTweedeKeerZonderRecept(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos kern = helper.absolutePos(new BlockPos(11, 1, 11));
        KaasknabbelNest.Gevecht g = KaasknabbelNest.gevecht(level, "test2_" + kern.asLong(), kern);
        KaasknabbelNest.gewonnen(level, g);
        ChestBlockEntity eerste = (ChestBlockEntity) level.getBlockEntity(kern.above());
        helper.assertTrue(eerste != null && KaasknabbelNest.SCHAT.equals(eerste.getLootTable()), "the first treasure has the recipe table");
        level.removeBlock(kern.above(), false);
        KaasknabbelNest.gewonnen(level, g);
        ChestBlockEntity tweede = (ChestBlockEntity) level.getBlockEntity(kern.above());
        helper.assertTrue(tweede != null && KaasknabbelNest.KNABBELS.equals(tweede.getLootTable()), "a rematch only gives knabbels");
        level.removeBlock(kern.above(), false);
        helper.succeed();
    }

    @GuhTest(template = ARENA, timeoutTicks = 60)
    public static void piepOppernabbelStampt(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ServerLevel level = helper.getLevel();
        BozeOppernabbelEntity baas = helper.spawn(PiepFeature.BOZE_OPPERNABBEL.get(), new BlockPos(11, 2, 11));
        baas.setNoAi(true);
        BlockPos at = helper.absolutePos(new BlockPos(13, 2, 11));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setDeltaMovement(Vec3.ZERO);
        baas.slam(level);                                        // (the test player counts as creative: the wave passes it)
        helper.assertTrue(p.getDeltaMovement().lengthSqr() < 1e-6, "creative players are left alone");
        baas.duw(p);
        // (the damage itself can't be seen here: a test player is invulnerable for its first seconds)
        helper.assertTrue(p.getDeltaMovement().y > 0.2 && p.getDeltaMovement().x > 0.3, "the stamp knocks you up and away: " + p.getDeltaMovement());
        p.setOnGround(false);
        helper.assertTrue(!BozeOppernabbelEntity.geraakt(p), "jumping over it: missed");
        helper.assertTrue(baas.getMaxHealth() >= 100, "a mini-boss: lots of health");
        baas.discard();
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, timeoutTicks = 40)
    public static void piepKnabbelsLatenKnabbelsVallen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(PiepFeature.BOZE_KAASKNABBEL.get().getDefaultLootTable().orElseThrow());
        BozeKaasknabbelEntity k = helper.spawn(PiepFeature.BOZE_KAASKNABBEL.get(), new BlockPos(4, 2, 4));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, k)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, k.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        var drops = table.getRandomItems(params);
        helper.assertTrue(!drops.isEmpty() && drops.stream().allMatch(s -> s.is(ModItems.KAAS_KNABBELS.get())), "normal kaasknabbels: " + drops);
        helper.assertTrue(!k.shouldDespawnInPeaceful() && k.getType().isAllowedInPeaceful(), "they stay in peaceful (the nest must stay doable)");
        k.discard();
        helper.succeed();
    }

    // --- the spawn hook -----------------------------------------------------------------------------------------------------------------

    @GuhTest(template = WEI, timeoutTicks = 40)
    public static void piepSpawnHookKansEnDimensie(GameTestHelper helper) {
        RandomSource random = RandomSource.create(2811);
        int ja = 0;
        for (int i = 0; i < 5000; i++) {
            if (PiepSpawns.kans(random)) {
                ja++;
            }
        }
        helper.assertTrue(ja > 400 && ja < 600, "about one per ten guhs: " + ja + " of 5000");
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertTrue(!PiepSpawns.magHier(level, pos), "never outside the Guhmensie");
        for (int i = 0; i < 50; i++) {
            helper.assertTrue(PiepSpawns.maybeAddMuisje(level, pos, random) == null, "no muisjes here, whatever the roll");
        }
        helper.assertTrue(PiepSpawns.NIET_HIER.containsAll(java.util.List.of("mika_kamp", "knabbelkelder", "moerasheks_hut", "barbecueput")),
                "the Mika places and the Barbecuether are left out");
        helper.succeed();
    }

    @GuhTest(template = WEI, timeoutTicks = 40)
    public static void piepFtbHoofdstuk(GameTestHelper helper) {
        try {
            java.nio.file.Path quests = java.nio.file.Files.createTempDirectory("guhs-ftbpiep");
            java.nio.file.Path lang = quests.resolve("lang").resolve("en_us.snbt");
            java.nio.file.Files.createDirectories(lang.getParent());
            java.nio.file.Files.writeString(lang, "{\n}\n");
            helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.installInto(quests), "installs");
            String piep = java.nio.file.Files.readString(quests.resolve("chapters").resolve("guhs_piep.snbt"));
            String onderwater = java.nio.file.Files.readString(quests.resolve("chapters").resolve("guhs_onderwater.snbt"));
            java.util.regex.Matcher versie = java.util.regex.Pattern.compile("guhs_chapter_version: (\\d+)").matcher(piep);
            helper.assertTrue(versie.find() && Integer.parseInt(versie.group(1)) >= 15 && onderwater.contains(versie.group(0)),
                    "the Piep chapter has the current chapter version");
            // (piepmenu) the turtles' quests moved to the sea chapter; the muisjes, the nest and the koek stay in Piep!
            for (String q : java.util.List.of("piep_schilly_getamed", "piep_poetsbeurt", "piep_schilly2_getamed", "piep_schilly_bestie", "piep_schilly_beef")) {
                helper.assertTrue(onderwater.contains("guhs:quest/" + q + "\"") && !piep.contains("guhs:quest/" + q + "\""), q + " is in Onderwater");
            }
            for (String q : java.util.List.of("piep_muisje_geaaid", "piep_verstoppertje_3", "piep_nest_gewonnen", "piep_koek")) {
                helper.assertTrue(piep.contains("guhs:quest/" + q + "\"") && !onderwater.contains("guhs:quest/" + q + "\""), q + " stays in Piep!");
            }
            helper.assertTrue(piep.contains("progression_mode: \"flexible\"") && !piep.contains("linear"), "nothing is locked");
            String text = java.nio.file.Files.readString(lang);
            helper.assertTrue(text.contains("Piep!") && text.contains("&dGuhs\""), "the Piep chapter's texts are in the lang file");
        } catch (java.io.IOException e) {
            helper.fail(e.toString());
        }
        helper.succeed();
    }
    // --- 2.10: names above their heads ------------------------------------------------------------------------------------

    /** Schilly, Poepschilly and the muisje show their name like a guh; not while the muisje hides or a schilly is inside a guh. */
    @GuhTest(template = WEI, timeoutTicks = 40)
    public static void piepNamenBovenHetHoofd(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(3, 2, 3));
        PoepschillyEntity poep = helper.spawn(PiepFeature.POEPSCHILLY.get(), new BlockPos(5, 2, 3));
        SchillyEntity schilly = helper.spawn(PiepFeature.SCHILLY.get(), new BlockPos(7, 2, 3));
        for (net.minecraft.world.entity.Mob m : java.util.List.of(muis, poep, schilly)) {
            helper.assertTrue(m.shouldShowName() && !m.getDisplayName().getString().isBlank(), m.getType() + " shows its name: " + m.getDisplayName().getString());
        }
        muis.setCustomName(Component.literal("Pieps"));
        helper.assertTrue(muis.shouldShowName() && muis.getDisplayName().getString().equals("Pieps"), "a named muisje shows its own name");
        BlockPos pot = new BlockPos(6, 2, 6);
        helper.setBlock(pot, Blocks.FLOWER_POT);
        muis.tame(p);
        muis.verstop().verstopBij(helper.absolutePos(pot), true);
        helper.assertTrue(muis.isVerstopt() && !muis.shouldShowName(), "a hidden muisje doesn't give itself away with its name");
        muis.verstop().gevonden(p);
        helper.assertTrue(muis.shouldShowName(), "found: its name is back");
        poep.setBinnen(true);
        schilly.setBinnen(true);
        helper.assertTrue(!poep.shouldShowName() && !schilly.shouldShowName(), "inside a guh: no name floating out of its kontje");
        poep.setBinnen(false);
        schilly.setBinnen(false);
        helper.assertTrue(poep.shouldShowName() && schilly.shouldShowName(), "back out: names again");
        for (Entity e : new Entity[]{muis, poep, schilly}) {
            e.discard();
        }
        leave(helper, p);
        helper.succeed();
    }
}
