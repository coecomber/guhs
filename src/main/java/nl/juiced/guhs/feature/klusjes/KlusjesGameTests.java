package nl.juiced.guhs.feature.klusjes;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.Recept;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.boerderij.GuhschaapjeEntity;
import nl.juiced.guhs.feature.boerderij.KippennestjeBlock;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlockEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeGoal;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.feature.tuintjes.TuinPlant;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;
import nl.juiced.guhs.feature.vadswoud.KnabbelbessenstruikBlock;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the klusjes (2.10): every chore does its job from a real Guhhuisje (template klusjes_test_tuin: 24 x 24
 * grass at y 0, the huisje in the middle, door south): opgraven (loot, the ground stays, hearts + dagboek + first times),
 * farmen (harvest + replant, guhtuintjes), opruimen (ground items, the chest sorted into the Bank Guh), dieren (nest,
 * knabbelkorf, pet + feed, wool), bakken (knabbelmeel into pastries, graan into the molentje), vissen, waken (a Mika is
 * pushed away, never hurt), plukken (bushes stay, flowers stay), lampjes (on in the evening, off in the morning), oppas
 * (a hurt guh gets a snack, a muisje a cuddle); a resident with its chores switched off does nothing. Every test switches
 * off every other chore of its resident and resets the chores' wait times, so it runs quickly. Each in its own batch
 * (the home base is bigger than a test plot).
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class KlusjesGameTests {
    private static final String TUIN = "klusjes_test_tuin";
    private static final BlockPos HUISJE = new BlockPos(11, 2, 11);
    /** Right next to the huisje (east side). */
    private static final BlockPos KIST = new BlockPos(12, 2, 11);

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, Huisje h, ServerPlayer... players) {
        HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static Huisje huisje(GameTestHelper helper, ServerPlayer owner) {
        Huisje h = HuisjeBlock.bouw(helper.getLevel(), helper.absolutePos(HUISJE), Direction.SOUTH, HuisjeMaat.KLEIN, owner.getUUID());
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.DAG);
        return h;
    }

    /** A tamed guh that moves in and only does this one chore (and never waits long between two tries). */
    static GuhEntity bewoner(GameTestHelper helper, Huisje h, ServerPlayer owner, BlockPos at, String klus) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        alleen(helper, h, guh, klus);
        return guh;
    }

    static void alleen(GameTestHelper helper, Huisje h, Mob mob, String klus) {
        helper.assertTrue(Huisjes.trekIn(h, mob), "moves in");
        for (Klus k : Klusjes.alle()) {
            h.zetKlus(Band.id(mob), k.id(), k.id().equals(klus));
        }
        helper.onEachTick(() -> mob.getPersistentData().remove("guhs_huisje_klus"));
    }

    static ChestBlockEntity kist(GameTestHelper helper) {
        helper.setBlock(KIST, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(KIST));
    }

    static int telKist(ChestBlockEntity kist, java.util.function.Predicate<ItemStack> wat) {
        int n = 0;
        for (int i = 0; i < kist.getContainerSize(); i++) {
            ItemStack s = kist.getItem(i);
            if (!s.isEmpty() && wat.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Where it is and what it does (for the failure messages). */
    static String staat(GameTestHelper helper, Mob mob) {
        String taak = "-";
        for (var w : mob.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof HuisjeGoal g) {
                taak = (g.taak() == null ? "geen taak" : g.taak().getClass().getSimpleName()) + (w.isRunning() ? " (loopt)" : " (goal staat stil)");
            }
        }
        return " [" + helper.relativePos(mob.blockPosition()) + ", " + taak + ", klusjes " + Dagboek.stat(mob.getServer(), Band.eigenaar(mob), Band.id(mob), DagboekStat.KLUSJES) + "]";
    }

    static void normaal(GameTestHelper helper) {
        if (helper.getLevel().getDifficulty() == Difficulty.PEACEFUL) {
            helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        }
    }

    // =====================================================================================================================

    @GameTest(template = TUIN, batch = "klusjes_register")
    public static void klusjesTienInDeGoedeVolgorde(GameTestHelper helper) {
        List<String> ids = Klusjes.alle().stream().map(Klus::id).filter(KlusjesFeature.IDS::contains).toList();
        helper.assertTrue(ids.equals(KlusjesFeature.IDS), "the ten chores in contract order: " + ids);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(5, 2, 3));
        TamableAnimal schilly = helper.spawn(PiepFeature.SCHILLY.get(), new BlockPos(7, 2, 3));
        for (String id : KlusjesFeature.IDS) {
            Klus k = Klusjes.van(id);
            helper.assertTrue(k != null && !k.icoon().isEmpty() && k.kan(guh), id + ": guhs can do it");
        }
        helper.assertTrue(Klusjes.van("opgraven").kan(muis) && Klusjes.van("opruimen").kan(muis) && Klusjes.van("waken").kan(muis)
                && Klusjes.van("plukken").kan(muis), "muisjes dig, tidy, peep and pick");
        helper.assertTrue(!Klusjes.van("bakken").kan(muis) && !Klusjes.van("farmen").kan(muis), "muisjes don't bake or farm");
        helper.assertTrue(Klusjes.van("vissen").kan(schilly) && !Klusjes.van("vissen").kan(muis) && !Klusjes.van("opgraven").kan(schilly),
                "the turtles fish");
        // the loot tables: kaasknabbels mostly, never anything strange
        ServerLevel level = helper.getLevel();
        int knabbels = 0;
        for (int i = 0; i < 60; i++) {
            for (ItemStack s : OpgravenKlus.vondst(level, guh, helper.absolutePos(new BlockPos(i % 20, 0, i / 20)))) {
                helper.assertTrue(!s.isEmpty(), "no empty loot");
                if (s.is(ModItems.KAAS_KNABBELS.get())) {
                    knabbels++;
                }
            }
            helper.assertTrue(!VissenKlus.vangst(level, guh, helper.absolutePos(new BlockPos(i % 20, 0, 3))).isEmpty(), "always a catch");
        }
        helper.assertTrue(knabbels > 20, "digging mostly finds kaasknabbels: " + knabbels);
        helper.succeed();
    }

    @GameTest(template = TUIN, batch = "klusjes_opgraven", timeoutTicks = 800)
    public static void klusjesOpgravenVindtKnabbels(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        Huisje h = huisje(helper, p);
        ChestBlockEntity kist = kist(helper);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(14, 2, 15), "opgraven");
        UUID id = Band.id(guh);
        int hartjes = Band.hartjes(guh);
        helper.succeedWhen(() -> {
            helper.assertTrue(!kist.isEmpty(), "something dug up in the chest");
            helper.assertTrue(Dagboek.stat(p.server, p.getUUID(), id, DagboekStat.KLUSJES) >= 1, "counted in the dagboek");
            helper.assertTrue(Band.hartjes(guh) > hartjes, "hearts for the chore");
            helper.assertTrue(Dagboek.heeftEersteKeer(p.server, p.getUUID(), id, "eerste_klusje")
                    && Dagboek.heeftEersteKeer(p.server, p.getUUID(), id, "klusjes_opgraven"), "the first times");
            helper.assertTrue((GuhQuests.saved(p).getInt(KlusBeloning.GEDAAN) & 1) != 0, "the owner's chore bits");
            for (int x = 0; x < 24; x++) {
                for (int z = 0; z < 24; z++) {
                    helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(x, 1, z));
                }
            }
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_farmen", timeoutTicks = 900)
    public static void klusjesFarmenOogstEnPlantOpnieuw(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        BlockPos[] tarwe = {new BlockPos(6, 2, 15), new BlockPos(7, 2, 15), new BlockPos(8, 2, 15)};
        helper.setBlock(new BlockPos(7, 1, 16), Blocks.WATER);
        for (BlockPos t : tarwe) {
            helper.setBlock(t.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            helper.setBlock(t, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        }
        BlockPos bak = new BlockPos(15, 2, 15);
        helper.setBlock(bak, TuintjesFeature.GUH_MOESTUINBAK.get().defaultBlockState().setValue(TuinBlock.PLANT, TuinPlant.KNABBELPLANTJE)
                .setValue(TuinBlock.GROEI, TuinBlock.RIJP));
        BlockPos dorst = new BlockPos(16, 2, 15);
        helper.setBlock(dorst, TuintjesFeature.GUH_MOESTUINBAK.get().defaultBlockState().setValue(TuinBlock.PLANT, TuinPlant.THEEKRUID)
                .setValue(TuinBlock.GROEI, 1));
        Huisje h = huisje(helper, p);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(12, 2, 14), "farmen");
        helper.succeedWhen(() -> {
            for (BlockPos t : tarwe) {
                helper.assertTrue(helper.getBlockState(t).is(Blocks.WHEAT) && helper.getBlockState(t).getValue(CropBlock.AGE) < 7,
                        "harvested and planted again at " + t + staat(helper, guh));
            }
            helper.assertTrue(helper.getBlockState(bak).getValue(TuinBlock.GROEI) == 0
                    && helper.getBlockState(bak).getValue(TuinBlock.PLANT) == TuinPlant.KNABBELPLANTJE, "the moestuinbak was harvested (the plant stays)");
            helper.assertTrue(helper.getBlockState(dorst).getValue(TuinBlock.GEWATERD) || helper.getBlockState(dorst).getValue(TuinBlock.GROEI) > 1,
                    "the thirsty one got water");
            helper.assertTrue(telKist(kist, s -> s.is(Items.WHEAT)) >= 3, "the wheat is in the chest");
            helper.assertTrue(telKist(kist, s -> s.is(TuintjesFeature.KNABBELGRAAN.get())) >= 2, "the knabbelgraan too");
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_opruimen", timeoutTicks = 900)
    public static void klusjesOpruimenEnSorterenInDeBankGuh(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        kist.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        BlockPos bank = new BlockPos(8, 2, 14);
        helper.setBlock(bank, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity be = (BankGuhBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(bank));
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 3; i++) {
            Vec3 v = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(14 + i, 2, 16)));
            ItemEntity item = new ItemEntity(level, v.x, v.y, v.z, new ItemStack(Items.APPLE, 2));
            item.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(item);
        }
        Huisje h = huisje(helper, p);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(12, 2, 14), "opruimen");
        float leven = guh.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, h.gebied()).isEmpty(), "nothing lies around any more" + staat(helper, guh));
            helper.assertTrue(be.getStorage().count(new ItemStack(Items.APPLE)) == 6, "the apples are sorted into the Bank Guh");
            helper.assertTrue(be.getStorage().count(new ItemStack(Items.COBBLESTONE)) == 10 && kist.isEmpty(), "the chest is sorted into the Bank Guh");
            helper.assertTrue(guh.getHealth() >= leven, "(nobody got hurt)");
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_dieren", timeoutTicks = 1600)
    public static void klusjesDierenVerzorgenEnOogsten(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        kist.setItem(0, new ItemStack(BoerderijFeature.KNABBELVOER.get(), 4));
        BlockPos nest = new BlockPos(6, 2, 8);
        helper.setBlock(nest, BoerderijFeature.KIPPENNESTJE.get().defaultBlockState().setValue(KippennestjeBlock.EIEREN, 2));
        BlockPos korf = new BlockPos(16, 2, 8);
        helper.setBlock(korf, ModBlocks.KNABBELKORF.get().defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
        GuhschaapjeEntity schaap = helper.spawn(BoerderijFeature.GUHSCHAAPJE.get(), new BlockPos(7, 2, 16));
        schaap.vergeetZorg();
        schaap.setNoAi(true);
        Huisje h = huisje(helper, p);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(12, 2, 14), "dieren");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(nest).getValue(KippennestjeBlock.EIEREN) == 0, "the eggs are taken out of the nest");
            helper.assertTrue(helper.getBlockState(korf).getValue(BeehiveBlock.HONEY_LEVEL) == 0, "the knabbelkorf is harvested");
            helper.assertTrue(schaap.heeftZorg(BoerderijDier.Zorg.AAIEN) && schaap.heeftZorg(BoerderijDier.Zorg.VOEREN), "the schaapje was petted and fed"
                    + staat(helper, guh));
            helper.assertTrue(telKist(kist, s -> s.is(BoerderijFeature.KNABBELEI.get())) == 2, "two knabbeleieren in the chest");
            helper.assertTrue(telKist(kist, s -> s.is(ModItems.KAAS_KNABBELS.get()) || s.is(ModItems.KAASHONING.get())) > 0, "the korf's knabbels");
            helper.assertTrue(telKist(kist, s -> s.is(BoerderijFeature.PLUISWOL.get())) >= 2, "the content schaapje's pluiswol");
            helper.assertTrue(schaap.getHealth() == schaap.getMaxHealth(), "(never hurt)");
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_bakken", timeoutTicks = 1600)
    public static void klusjesBakkenMetMeelEnMolen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        kist.setItem(0, new ItemStack(GuhpolderFeature.KNABBELMEEL.get(), 1));
        kist.setItem(1, new ItemStack(ModItems.KAAS_KNABBELS.get(), 1));
        kist.setItem(2, new ItemStack(Items.BREAD, 1));
        kist.setItem(3, new ItemStack(TuintjesFeature.KNABBELGRAAN.get(), 6));
        BlockPos oven = new BlockPos(7, 2, 14);
        helper.setBlock(oven, BakkerijFeature.KNABBELOVEN.get());
        BlockPos molen = new BlockPos(16, 2, 14);
        helper.setBlock(molen, GuhpolderFeature.GUH_MOLENTJE.get());
        Huisje h = huisje(helper, p);
        bewoner(helper, h, p, new BlockPos(12, 2, 14), "bakken");
        ItemStack broodje = new ItemStack(BakkerijFeature.bakje(Recept.KNABBELBROODJE));
        helper.succeedWhen(() -> {
            helper.assertTrue(telKist(kist, s -> s.is(broodje.getItem())) == 2 * 2, "knabbelmeel: twice two knabbelbroodjes in the chest");
            helper.assertTrue(telKist(kist, s -> s.is(Items.BREAD) || s.is(GuhpolderFeature.KNABBELMEEL.get())) == 0, "the ingredients were used");
            BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(molen));
            helper.assertTrue(be instanceof MolentjeBlockEntity m && (m.graan().getCount() + m.meel().getCount() > 0
                    || telKist(kist, s -> s.is(GuhpolderFeature.KNABBELMEEL.get())) > 0), "the knabbelgraan went to the molentje");
            helper.assertTrue(telKist(kist, s -> s.is(TuintjesFeature.KNABBELGRAAN.get())) == 0, "all the graan is in the molentje");
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_vissen", timeoutTicks = 900)
    public static void klusjesVissenInDeVijver(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        for (int x = 14; x <= 16; x++) {
            for (int z = 15; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
            }
        }
        Huisje h = huisje(helper, p);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(12, 2, 14), "vissen");
        helper.succeedWhen(() -> {
            helper.assertTrue(!kist.isEmpty(), "a catch in the chest" + staat(helper, guh));
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_waken", timeoutTicks = 900)
    public static void klusjesWakenDuwtMikaZachtjesWeg(GameTestHelper helper) {
        normaal(helper);
        ServerPlayer p = speler(helper, new BlockPos(18, 2, 6));
        Huisje h = huisje(helper, p);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(12, 2, 14), "waken");
        MikaEntity mika = helper.spawn(ModEntities.MIKA.get(), new BlockPos(6, 2, 16));
        mika.goalSelector.removeAllGoals(g -> true);          // (it stands still and never attacks, but can be pushed)
        mika.targetSelector.removeAllGoals(g -> true);
        float leven = mika.getHealth();
        float guhLeven = guh.getHealth();
        Vec3 m = h.midden();
        double start = mika.position().distanceToSqr(m.x, mika.getY(), m.z);
        AtomicBoolean nooitPijn = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (mika.isAlive() && mika.getHealth() < leven || guh.getHealth() < guhLeven || guh.getTarget() != null) {
                nooitPijn.set(false);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(nooitPijn.get(), "nobody ever got hurt, the guh never fought: mika " + mika.getHealth() + "/" + leven + ", guh "
                    + guh.getHealth() + "/" + guhLeven + ", target " + guh.getTarget());
            helper.assertTrue(mika.isAlive() && mika.getHealth() == leven, "the Mika is fine");
            double nu = mika.position().distanceToSqr(m.x, mika.getY(), m.z);
            helper.assertTrue(Math.sqrt(nu) > Math.sqrt(start) + 1.5, "pushed away from the huisje: " + Math.sqrt(start) + " -> " + Math.sqrt(nu)
                    + staat(helper, guh));
            helper.assertTrue((GuhQuests.saved(p).getInt(KlusBeloning.GEDAAN) & (1 << KlusjesFeature.IDS.indexOf("waken"))) != 0, "the owner knows");
            mika.discard();
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_plukken", timeoutTicks = 900)
    public static void klusjesPlukkenBessenEnBloemetjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        BlockPos bes = new BlockPos(7, 2, 15), zoet = new BlockPos(9, 2, 16), bloem = new BlockPos(15, 2, 15);
        helper.setBlock(bes, VadswoudFeature.KNABBELBESSENSTRUIK.get().defaultBlockState().setValue(KnabbelbessenstruikBlock.AGE, 3));
        helper.setBlock(zoet, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        helper.setBlock(bloem, ModBlocks.KAASBLOEM.get());
        Huisje h = huisje(helper, p);
        bewoner(helper, h, p, new BlockPos(12, 2, 14), "plukken");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(bes).getValue(KnabbelbessenstruikBlock.AGE) < 3
                    && helper.getBlockState(zoet).getValue(SweetBerryBushBlock.AGE) < 3, "both bushes picked (and they stay)");
            helper.assertTrue(telKist(kist, s -> s.is(VadswoudFeature.KNABBELBESSEN.get())) > 0 && telKist(kist, s -> s.is(Items.SWEET_BERRIES)) > 0,
                    "the berries are in the chest");
            helper.assertTrue(telKist(kist, s -> s.is(ModBlocks.KAASBLOEM.get().asItem())) > 0, "a kaasbloem picked");
            helper.assertBlockPresent(ModBlocks.KAASBLOEM.get(), bloem);
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_lampjes", timeoutTicks = 900)
    public static void klusjesLampjesAvondAanOchtendUit(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        BlockPos[] lampen = {new BlockPos(6, 2, 14), new BlockPos(16, 2, 14), new BlockPos(11, 2, 18)};
        for (BlockPos l : lampen) {
            helper.setBlock(l, KlusjesFeature.GUHLAMPJE.get().defaultBlockState().setValue(GuhlampjeBlock.LIT, false));
        }
        BlockPos kaars = new BlockPos(8, 2, 18);
        helper.setBlock(kaars, Blocks.PINK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, false));
        Huisje h = huisje(helper, p);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.AVOND);
        bewoner(helper, h, p, new BlockPos(12, 2, 14), "lampjes");
        AtomicInteger fase = new AtomicInteger();
        helper.onEachTick(() -> {
            if (fase.get() == 0 && alle(helper, lampen, kaars, true)) {
                fase.set(1);
                HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.OCHTEND);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase.get() == 1, "all on in the evening");
            helper.assertTrue(alle(helper, lampen, kaars, false), "all off in the morning");
            weg(helper, h, p);
        });
    }

    private static boolean alle(GameTestHelper helper, BlockPos[] lampen, BlockPos kaars, boolean aan) {
        for (BlockPos l : lampen) {
            if (helper.getBlockState(l).getValue(GuhlampjeBlock.LIT) != aan) {
                return false;
            }
        }
        return helper.getBlockState(kaars).getValue(CandleBlock.LIT) == aan;
    }

    @GameTest(template = TUIN, batch = "klusjes_oppas", timeoutTicks = 1200)
    public static void klusjesOppasSnackjeEnKnuffel(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = kist(helper);
        kist.setItem(0, new ItemStack(ModItems.GUH_CUPCAKE.get(), 1));
        GuhEntity patient = helper.spawn(ModEntities.GUH.get(), new BlockPos(7, 2, 16));
        patient.tame(p);
        patient.setOrderedToSit(true);
        patient.setHealth(400f);
        TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(16, 2, 16));
        muis.tame(p);
        muis.setOrderedToSit(true);
        Huisje h = huisje(helper, p);
        GuhEntity oppas = bewoner(helper, h, p, new BlockPos(12, 2, 14), "oppas");
        float oppasLeven = oppas.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(patient.getHealth() >= 400f + OppasKlus.SNACK_HEAL, "the hurt guh got a snack: " + patient.getHealth());
            helper.assertTrue(telKist(kist, s -> s.is(ModItems.GUH_CUPCAKE.get())) == 0, "the cupcake came out of the chest");
            helper.assertTrue(muis.getPersistentData().contains(OppasKlus.VERZORGD), "the muisje got a cuddle" + staat(helper, oppas));
            helper.assertTrue(oppas.getHealth() >= oppasLeven, "(the babysitter is fine too)");
            weg(helper, h, p);
        });
    }

    @GameTest(template = TUIN, batch = "klusjes_schakelaar", timeoutTicks = 900)
    public static void klusjesSchakelaarUitIsUit(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(21, 2, 21));
        kist(helper);
        BlockPos t = new BlockPos(7, 2, 15);
        helper.setBlock(new BlockPos(7, 1, 16), Blocks.WATER);
        helper.setBlock(t.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.setBlock(t, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        Huisje h = huisje(helper, p);
        GuhEntity guh = bewoner(helper, h, p, new BlockPos(12, 2, 14), "(niks)");
        UUID id = Band.id(guh);
        AtomicBoolean ooitBezig = new AtomicBoolean();
        AtomicInteger ticks = new AtomicInteger();
        helper.onEachTick(() -> {
            int n = ticks.incrementAndGet();
            if (n < 300 && BandVlaggen.heeft(guh, BandVlaggen.KLUSJE)) {
                ooitBezig.set(true);
            }
            if (n == 300) {
                helper.assertTrue(!ooitBezig.get(), "with every chore off it never did one");
                helper.assertTrue(helper.getBlockState(t).getValue(CropBlock.AGE) == 7, "the wheat is still ripe");
                h.zetKlus(id, "farmen", true);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(ticks.get() > 300, "(first the quiet part)");
            helper.assertTrue(helper.getBlockState(t).is(Blocks.WHEAT) && helper.getBlockState(t).getValue(CropBlock.AGE) < 7, "switched on: harvested and replanted");
            weg(helper, h, p);
        });
    }
}
