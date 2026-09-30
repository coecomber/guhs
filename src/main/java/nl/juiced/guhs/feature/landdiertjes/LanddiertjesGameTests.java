package nl.juiced.guhs.feature.landdiertjes;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeGoal;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.Schouder;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the landdiertjes (3.0): taming + the pick-up round trip of all four (name, owner, band id, the bunny's fur,
 * Sjokkel's berries kept), they move into a Guhhuisje, the eekhoorntje on the shoulder and back, Sjokkel's polijsten chore
 * (cobblestone from the chest into smooth stone / guhsteentjes), the shuckle-plekje keeps at most two Sjokkels (and none pop
 * up next to a player), rolling up / the shell (no damage), the bessensapje, a squirrel's present stash, the Guhdex pages.
 * Templates landdiertjes_test_wei (10 x 10 grass) and landdiertjes_test_tuin (24 x 24 grass).
 */
public class LanddiertjesGameTests {
    private static final String WEI = "landdiertjes_test_wei";
    private static final String TUIN = "landdiertjes_test_tuin";
    private static final BlockPos HUISJE = new BlockPos(11, 2, 11);
    private static final BlockPos KIST = new BlockPos(12, 2, 11);

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Feeds its favourite food from the player's hand until it is tame (the chance is 1 in 3-4: 80 tries is plenty). */
    static void tem(GameTestHelper helper, Landdiertje d, ServerPlayer p, ItemStack voer) {
        p.setItemInHand(InteractionHand.MAIN_HAND, voer.copyWithCount(64));
        for (int i = 0; i < 80 && !d.isTame(); i++) {
            if (p.getMainHandItem().isEmpty()) {
                p.setItemInHand(InteractionHand.MAIN_HAND, voer.copyWithCount(64));
            }
            d.mobInteract(p, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(d.isTame() && d.isOwnedBy(p), d.soort() + " is tamed by its food");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    // =====================================================================================================================

    @GuhTest(template = WEI, batch = "landdiertjes")
    public static void landdiertjesTemmenEnOppakken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        List<Object[]> soorten = List.of(
                new Object[]{LanddiertjesFeature.PLUISEGELTJE, new ItemStack(Items.SWEET_BERRIES)},
                new Object[]{LanddiertjesFeature.GUH_KONIJNTJE, new ItemStack(Items.CARROT)},
                new Object[]{LanddiertjesFeature.PLUISEEKHOORNTJE, new ItemStack(ModItems.KAAS_KNABBELS.get())},
                new Object[]{LanddiertjesFeature.SHUCKLE, new ItemStack(Items.SWEET_BERRIES)});
        int x = 2;
        for (Object[] s : soorten) {
            @SuppressWarnings("unchecked")
            EntityType<? extends Landdiertje> type = ((Supplier<EntityType<? extends Landdiertje>>) s[0]).get();
            Landdiertje d = helper.spawn(type, new BlockPos(x, 2, 6));
            x += 2;
            ItemStack voer = (ItemStack) s[1];
            helper.assertTrue(d.isVoer(voer) && !d.isTame(), d.soort() + ": wild, likes its food");
            if (d instanceof ShuckleEntity sh) {
                // in its shell it can't be tamed (the berry stays in your hand)
                sh.inSchelp(40);
                p.setItemInHand(InteractionHand.MAIN_HAND, voer.copyWithCount(5));
                sh.mobInteract(p, InteractionHand.MAIN_HAND);
                helper.assertTrue(p.getMainHandItem().getCount() == 5 && !sh.isTame(), "Sjokkel in its shell: not now");
                sh.setUitSchelpVoorTest();
            }
            tem(helper, d, p, voer);
            if (d instanceof GuhKonijntjeEntity k) {
                k.setKleur(GuhKonijntjeEntity.Kleur.CHOCO);
            }
            if (d instanceof ShuckleEntity sh) {
                sh.zetBessen(5);
            }
            d.setCustomName(Component.literal("Knuffel" + d.soort()));
            UUID band = Band.id(d);
            // sneak + empty hand (the owner): picked up
            p.setShiftKeyDown(true);
            d.mobInteract(p, InteractionHand.MAIN_HAND);
            p.setShiftKeyDown(false);
            helper.assertTrue(d.isRemoved(), d.soort() + " is picked up");
            ItemStack item = ItemStack.EMPTY;
            for (ItemStack st : p.getInventory().getNonEquipmentItems()) {
                if (st.getItem() instanceof PiepDierItem pi && pi.type() == type) {
                    item = st;
                }
            }
            helper.assertTrue(!item.isEmpty() && item.getItem() == d.oppakItem(), d.soort() + ": its item is in the pockets");
            Landdiertje terug = (Landdiertje) PiepDierItem.zetNeer(item, level, helper.absolutePos(new BlockPos(x - 2, 2, 3)).getCenter(), 0f,
                    type);
            helper.assertTrue(terug != null && terug.isTame() && terug.isOwnedBy(p), d.soort() + ": put down, still yours");
            helper.assertTrue(band.equals(Band.id(terug)), d.soort() + ": the same band id");
            helper.assertTrue(terug.hasCustomName() && terug.getCustomName().getString().equals("Knuffel" + d.soort()), d.soort() + ": its name");
            if (terug instanceof GuhKonijntjeEntity k) {
                helper.assertTrue(k.kleur() == GuhKonijntjeEntity.Kleur.CHOCO, "the bunny keeps its fur");
            }
            if (terug instanceof ShuckleEntity sh) {
                helper.assertTrue(sh.bessen() == 5, "Sjokkel keeps its berries");
            }
            helper.assertTrue(!terug.wantsToAttack(p, p), "always lief");
        }
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "landdiertjes_huisje")
    public static void landdiertjesWonenInEenHuisje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        Huisje h = HuisjeBlock.bouw(helper.getLevel(), helper.absolutePos(HUISJE), Direction.SOUTH, HuisjeMaat.MEDIUM, p.getUUID());
        int x = 3;
        for (EntityType<? extends Landdiertje> type : List.<EntityType<? extends Landdiertje>>of(LanddiertjesFeature.PLUISEGELTJE.get(),
                LanddiertjesFeature.GUH_KONIJNTJE.get(), LanddiertjesFeature.PLUISEEKHOORNTJE.get(), LanddiertjesFeature.SHUCKLE.get())) {
            Landdiertje d = helper.spawn(type, new BlockPos(x, 2, 18));
            x += 3;
            helper.assertTrue(!Huisjes.kanBewoner(d), d.soort() + ": a wild one can't move in");
            d.tame(p);
            helper.assertTrue(Huisjes.kanBewoner(d) && Huisjes.trekIn(h, d) && Huisjes.isBewoner(d), d.soort() + " moves into the huisje");
            boolean goal = d.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof HuisjeGoal);
            helper.assertTrue(goal, d.soort() + " has the home-base goal");
        }
        helper.assertTrue(h.bewoners().size() == 4, "four critters live there: " + h.bewoners().size());
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "landdiertjes_schouder")
    public static void landdiertjesEekhoorntjeOpJeSchouder(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        PluiseekhoorntjeEntity e = helper.spawn(LanddiertjesFeature.PLUISEEKHOORNTJE.get(), new BlockPos(3, 2, 3));
        e.tame(p);
        UUID band = Band.id(e);
        helper.assertTrue(e.kanOpSchouder(), "the eekhoorntje fits on a shoulder");
        PluisegeltjeEntity egel = helper.spawn(LanddiertjesFeature.PLUISEGELTJE.get(), new BlockPos(7, 2, 3));
        helper.assertTrue(!egel.kanOpSchouder(), "the egeltje doesn't");
        e.speciaal(p);
        helper.assertTrue(Schouder.heeft(p) && e.isRemoved(), "on the shoulder (and out of the world)");
        CompoundTag tag = p.getPersistentData().getCompoundOrEmpty(Schouder.KEY);
        helper.assertTrue("guhs:pluiseekhoorntje".equals(tag.getStringOr("id", "")), "the shoulder knows it is an eekhoorntje: " + tag.getStringOr("id", ""));
        // a second one: the shoulder is full
        PluiseekhoorntjeEntity tweede = helper.spawn(LanddiertjesFeature.PLUISEEKHOORNTJE.get(), new BlockPos(2, 2, 7));
        tweede.tame(p);
        tweede.speciaal(p);
        helper.assertTrue(!tweede.isRemoved(), "one at a time");
        PiepMaatje eraf = Schouder.eraf(p, helper.absolutePos(new BlockPos(4, 2, 7)).getBottomCenter());
        helper.assertTrue(eraf instanceof PluiseekhoorntjeEntity pe && pe.isTame() && pe.isOwnedBy(p) && band.equals(Band.id(pe)),
                "it hops down: the same eekhoorntje");
        helper.assertTrue(!Schouder.heeft(p), "the shoulder is free again");
        // its item used in the air: onto the shoulder too
        ItemStack item = tweede.alsItem();
        tweede.discard();
        p.setItemInHand(InteractionHand.MAIN_HAND, item);
        item.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(Schouder.heeft(p) && p.getMainHandItem().isEmpty(), "the item in the air: on the shoulder");
        Schouder.eraf(p, p.position());
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "landdiertjes_polijsten", timeoutTicks = 1600)
    public static void landdiertjesShucklePolijstStenen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        Huisje h = HuisjeBlock.bouw(helper.getLevel(), helper.absolutePos(HUISJE), Direction.SOUTH, HuisjeMaat.KLEIN, p.getUUID());
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.DAG);
        helper.setBlock(KIST, Blocks.CHEST);
        ChestBlockEntity kist = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(KIST));
        kist.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        ShuckleEntity shuckle = helper.spawn(LanddiertjesFeature.SHUCKLE.get(), new BlockPos(13, 2, 15));
        shuckle.tame(p);
        helper.assertTrue(Huisjes.trekIn(h, shuckle), "Sjokkel moves in");
        Klus polijsten = Klusjes.van(PolijstenKlus.ID);
        helper.assertTrue(polijsten != null && polijsten.kan(shuckle), "the chore polijsten is registered, for Sjokkel");
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        helper.assertTrue(!polijsten.kan(guh) && !polijsten.kan(helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(5, 2, 3))),
                "only Sjokkel polishes");
        for (Klus k : Klusjes.alle()) {
            h.zetKlus(Band.id(shuckle), k.id(), k.id().equals(PolijstenKlus.ID));
        }
        helper.onEachTick(() -> shuckle.getPersistentData().remove("guhs_huisje_klus"));
        helper.succeedWhen(() -> {
            int glad = 0, steentjes = 0, keien = 0;
            for (int i = 0; i < kist.getContainerSize(); i++) {
                ItemStack s = kist.getItem(i);
                if (s.is(Items.SMOOTH_STONE)) {
                    glad += s.getCount();
                } else if (s.is(LanddiertjesFeature.GUHSTEENTJE.get())) {
                    steentjes += s.getCount();
                } else if (s.is(Items.COBBLESTONE)) {
                    keien += s.getCount();
                }
            }
            helper.assertTrue(glad + steentjes > 0, "polished stones in the chest (keien " + keien + ")");
            helper.assertTrue(keien <= 8 - PolijstenKlus.PER_KEER, "the cobblestones were used: " + keien);
            helper.assertTrue(glad + steentjes / 2 + keien == 8, "nothing lost: glad " + glad + ", steentjes " + steentjes + ", keien " + keien);
            HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
            weg(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = "landdiertjes")
    public static void landdiertjesPolijstenOpbrengst(GameTestHelper helper) {
        net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create(7);
        int glad = 0, steentjes = 0;
        for (int i = 0; i < 50; i++) {
            final int[] n = new int[2];
            PolijstenKlus.opbrengst(new ItemStack(Items.COBBLESTONE, 4), rng, s -> {
                if (s.is(Items.SMOOTH_STONE)) {
                    n[0] += s.getCount();
                } else if (s.is(LanddiertjesFeature.GUHSTEENTJE.get())) {
                    n[1] += s.getCount();
                }
            });
            helper.assertTrue(n[0] + n[1] / 2 == 4 && n[1] % 2 == 0, "every stone becomes smooth stone or two guhsteentjes");
            glad += n[0];
            steentjes += n[1];
        }
        helper.assertTrue(glad > 100 && steentjes > 20, "mostly smooth stone, now and then pebbles: " + glad + " / " + steentjes);
        helper.assertTrue(PolijstenKlus.isSteen(new ItemStack(Items.COBBLESTONE)) && !PolijstenKlus.isSteen(new ItemStack(Items.STONE)),
                "cobblestone is polished, stone isn't");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "landdiertjes_plekje", timeoutTicks = 200)
    public static void landdiertjesPlekjeHoudtTweeShuckles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plek = new BlockPos(11, 2, 11);
        helper.setBlock(plek, LanddiertjesFeature.SHUCKLE_PLEKJE.get());
        BlockPos abs = helper.absolutePos(plek);
        net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create(11);
        helper.assertTrue(ShucklePlekjeBlock.aantal(level, abs) == 0, "no Sjokkels yet");
        for (int i = 0; i < 40; i++) {
            ShucklePlekjeBlock.probeer(level, abs, rng);
        }
        int n = ShucklePlekjeBlock.aantal(level, abs);
        helper.assertTrue(n >= 1 && n <= ShucklePlekjeBlock.MAX, "one or two Sjokkels around the plekje: " + n);
        for (ShuckleEntity s : level.getEntitiesOfClass(ShuckleEntity.class, new AABB(abs).inflate(16))) {
            helper.assertTrue(s.plekje() != null && s.plekje().equals(abs) && !s.isTame(), "a wild Sjokkel of this plekje");
            helper.assertTrue(s.hasRestriction() && s.getRestrictCenter().equals(abs), "it stays around its plekje");
            s.discard();
        }
        // a player next to the plekje: nothing pops up
        ServerPlayer p = speler(helper, new BlockPos(13, 2, 11));
        for (int i = 0; i < 40; i++) {
            ShucklePlekjeBlock.probeer(level, abs, rng);
        }
        helper.assertTrue(level.getEntitiesOfClass(ShuckleEntity.class, new AABB(abs).inflate(16), e -> e.isAlive()).isEmpty(),
                "never in view of a player within 8");
        helper.assertTrue(LanddiertjesFeature.SHUCKLE_PLEKJE.get().defaultBlockState().isRandomlyTicking(), "the plekje ticks");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "landdiertjes_schrik", timeoutTicks = 400)
    public static void landdiertjesOprollenEnSchelp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PluisegeltjeEntity egel = helper.spawn(LanddiertjesFeature.PLUISEGELTJE.get(), new BlockPos(3, 2, 3));
        ShuckleEntity shuckle = helper.spawn(LanddiertjesFeature.SHUCKLE.get(), new BlockPos(7, 2, 7));
        egel.hurt(level.damageSources().generic(), 1f);
        shuckle.hurt(level.damageSources().generic(), 1f);
        helper.assertTrue(egel.isOpgerold() && egel.isBezig(), "a fright: the egeltje rolls up");
        helper.assertTrue(shuckle.isInSchelp() && shuckle.isBezig(), "a fright: Sjokkel pulls into its shell");
        float e1 = egel.getHealth(), s1 = shuckle.getHealth();
        helper.assertTrue(!egel.hurt(level.damageSources().generic(), 3f) && egel.getHealth() == e1, "rolled up nothing hurts it");
        helper.assertTrue(!shuckle.hurt(level.damageSources().generic(), 3f) && shuckle.getHealth() == s1, "in its shell nothing hurts it");
        // Sjokkel's juice: three berries -> a bessensapje pops out
        shuckle.zetBessen(3);
        helper.runAfterDelay(PluisegeltjeEntity.ROL_TICKS + 20, () -> {
            helper.assertTrue(!egel.isOpgerold(), "after a quiet while it peeks out again");
            helper.assertTrue(!shuckle.isInSchelp(), "Sjokkel too");
            helper.assertTrue(shuckle.maakSapje() && shuckle.bessen() == 0, "juice needs 3 berries");
            helper.assertTrue(!shuckle.maakSapje(), "not two at once");
        });
        helper.succeedWhen(() -> {
            List<ItemEntity> sap = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(7, 2, 7))).inflate(4),
                    i -> i.getItem().is(LanddiertjesFeature.BESSENSAPJE.get()));
            helper.assertTrue(!sap.isEmpty(), "a bessensapje popped out");
        });
    }

    @GuhTest(template = TUIN, batch = "landdiertjes_voorraadje", timeoutTicks = 800)
    public static void landdiertjesEekhoorntjeGraaftEenVoorraadjeOp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 12));
        PluiseekhoorntjeEntity e = helper.spawn(LanddiertjesFeature.PLUISEEKHOORNTJE.get(), new BlockPos(9, 2, 9));
        e.tame(p);
        helper.runAfterDelay(5, e::vondstNu);
        helper.succeedWhen(() -> {
            BlockPos gevonden = null;
            for (BlockPos b : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(22, 4, 22)))) {
                if (level.getBlockState(b).getBlock() instanceof KnabbelvoorraadjeBlock) {
                    gevonden = b.immutable();
                }
            }
            helper.assertTrue(gevonden != null, "a stash appeared next to its owner");
            int n = level.getBlockState(gevonden).getValue(KnabbelvoorraadjeBlock.KNABBELS);
            helper.assertTrue(n >= 2 && gevonden.distSqr(p.blockPosition()) <= 4 * 4, "2-4 knabbels, right next to you: " + n);
            level.getBlockState(gevonden).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(gevonden), Direction.UP, gevonden, false));
            helper.assertTrue(level.getBlockState(gevonden).isAir(), "dug up");
            helper.assertTrue(p.getInventory().countItem(ModItems.KAAS_KNABBELS.get()) == n, "the knabbels are yours");
            weg(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = "landdiertjes")
    public static void landdiertjesGuhdexPaginas(GameTestHelper helper) {
        for (GuhVariant v : List.of(GuhVariant.PLUISEGELTJE, GuhVariant.GUH_KONIJNTJE, GuhVariant.PLUISEEKHOORNTJE, GuhVariant.SHUCKLE)) {
            helper.assertTrue(GuhDex.isCreaturePage(v), v.id() + " is a creature page");
            helper.assertTrue(GuhDex.ENTRIES.contains(v) && GuhDex.TELLEND.contains(v), v.id() + " counts");
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id(v.id())), v.id() + ": its entity");
        }
        helper.succeed();
    }
}
