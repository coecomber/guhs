package nl.juiced.guhs.feature.guhvillagers;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModVillagers;

/** 1.7.0: the four new professions' trades, picking up villagers, the IJzerguhlem of the guh villagers. */
public class GuhVillagersGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "guhvillagers";

    private static Villager villager(GameTestHelper helper, BlockPos pos, boolean guh, net.minecraft.core.Holder<VillagerProfession> beroep) {
        Villager v = helper.spawn(EntityType.VILLAGER, pos);
        var access = helper.getLevel().registryAccess();
        v.setVillagerData(v.getVillagerData().withType(guh ? ModVillagers.GUH : access.lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_TYPE)
                .getOrThrow(VillagerType.PLAINS)).withProfession(beroep));
        return v;
    }

    /** Every item in the trade table exists (only Spreukjes enchantments may be missing), and every level has trades. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void handelAlleItemsBestaan(GameTestHelper helper) {
        for (var beroep : List.of(GuhVillagersFeature.TOVERGUH, GuhVillagersFeature.VADSMONTEUR, GuhVillagersFeature.MIJNWERKER, GuhVillagersFeature.KRISTALSLIJPER)) {
            List<List<GuhVillagerHandel.Ruil>> tabel = GuhVillagerHandel.tabel(beroep);
            helper.assertTrue(tabel != null && tabel.size() == 5, beroep.getKey() + ": five levels");
            for (List<GuhVillagerHandel.Ruil> niveau : tabel) {
                helper.assertTrue(niveau.size() >= 2, beroep.getKey() + ": at least two trades per level");
                for (GuhVillagerHandel.Ruil r : niveau) {
                    helper.assertTrue(GuhVillagerHandel.item(r.wants()) != null, "exists: " + r.wants());
                    helper.assertTrue(r.wants2() == null || GuhVillagerHandel.item(r.wants2()) != null, "exists: " + r.wants2());
                    helper.assertTrue(r.gives() == null || GuhVillagerHandel.item(r.gives()) != null, "exists: " + r.gives());
                }
            }
        }
        helper.succeed();
    }

    /** A Spreukjesboekje: an enchanted book of an existing enchantment; a book of only unknown enchantments is skipped. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void handelBoekjeVerdraagtOntbrekendeSpreuk(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var random = helper.getLevel().getRandom();
        MerchantOffer boek = GuhVillagerHandel.offer(new GuhVillagerHandel.Ruil("minecraft:emerald", 0, "minecraft:book", 1, null, 1,
                List.of(new GuhVillagerHandel.Boekje("guhs:bestaat_echt_niet", 1, 5), new GuhVillagerHandel.Boekje("minecraft:unbreaking", 9, 5)), 12, 1),
                registries, random);
        helper.assertTrue(boek != null && boek.getResult().is(Items.ENCHANTED_BOOK), "an enchanted book");
        helper.assertTrue(boek.getCostB().is(Items.BOOK), "costs a book too");
        MerchantOffer niets = GuhVillagerHandel.offer(new GuhVillagerHandel.Ruil("minecraft:emerald", 0, "minecraft:book", 1, null, 1,
                List.of(new GuhVillagerHandel.Boekje("guhs:bestaat_echt_niet", 1, 5)), 12, 1), registries, random);
        helper.assertTrue(niets == null, "no offer without an existing enchantment");
        helper.succeed();
    }

    /** A guh villager of a new profession gets two trades of its level (also the Toverguh, with or without the Spreukjes). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void handelNieuwBeroepKrijgtRuilen(GameTestHelper helper) {
        for (var beroep : List.of(GuhVillagersFeature.TOVERGUH, GuhVillagersFeature.VADSMONTEUR, GuhVillagersFeature.MIJNWERKER, GuhVillagersFeature.KRISTALSLIJPER)) {
            Villager v = villager(helper, new BlockPos(2, 1, 2), true, beroep);
            helper.assertTrue(v.getOffers().size() == 2, beroep.getKey() + ": two trades, got " + v.getOffers().size());
            v.discard();
        }
        helper.succeed();
    }

    /** Picking up keeps profession, level, xp, trades and name; putting it back gives the same villager. Not while trading. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void oppakkenEnNeerzetten(GameTestHelper helper) {
        Villager v = villager(helper, new BlockPos(2, 1, 2), true, GuhVillagersFeature.MIJNWERKER);
        v.setVillagerXp(42);
        v.setCustomName(net.minecraft.network.chat.Component.literal("Mijnie"));
        int ruilen = v.getOffers().size();
        ItemStack stack = VillagerOppakken.oppakken(v);
        helper.assertTrue(v.isRemoved(), "gone from the world");
        helper.assertTrue(stack.is(GuhVillagersFeature.OPGEPAKTE_VILLAGER.get()), "the item");
        CompoundTag data = OpgepakteVillagerItem.data(stack);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        Entity terug = PickedUpGuhItem.release(helper.getLevel(), data, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0);
        helper.assertTrue(terug instanceof Villager, "a villager again");
        Villager w = (Villager) terug;
        helper.assertTrue(w.getVillagerData().profession().is(GuhVillagersFeature.MIJNWERKER.getKey()), "same profession");
        helper.assertTrue(ModVillagers.isGuhVillager(w), "still a guh villager");
        helper.assertTrue(w.getVillagerXp() == 42, "same xp");
        helper.assertTrue(w.getOffers().size() == ruilen, "same trades");
        helper.assertTrue(w.hasCustomName() && "Mijnie".equals(w.getCustomName().getString()), "same name");
        helper.succeed();
    }

    /** A child and a vanilla villager can be picked up too. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void oppakkenKindEnGewoon(GameTestHelper helper) {
        Villager kind = villager(helper, new BlockPos(2, 1, 2), true, GuhVillagersFeature.TOVERGUH);
        kind.setAge(-24000);
        CompoundTag data = OpgepakteVillagerItem.data(VillagerOppakken.oppakken(kind));
        helper.assertTrue(data.getIntOr("Age", 0) < 0, "still a child");
        Villager gewoon = villager(helper, new BlockPos(4, 1, 4), false,
                helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER));
        CompoundTag data2 = OpgepakteVillagerItem.data(VillagerOppakken.oppakken(gewoon));
        helper.assertTrue("minecraft:plains".equals(data2.getCompoundOrEmpty("VillagerData").getStringOr("type", "")), "a vanilla villager keeps its type");
        helper.succeed();
    }

    private static void vloer(GameTestHelper helper, BlockPos midden) {
        ServerLevel level = helper.getLevel();
        for (int x = -9; x <= 9; x++) {
            for (int z = -9; z <= 9; z++) {
                level.setBlockAndUpdate(midden.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 8; y++) {
                    level.setBlockAndUpdate(midden.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static List<IronGolem> golems(GameTestHelper helper, BlockPos midden) {
        return helper.getLevel().getEntitiesOfClass(IronGolem.class, new AABB(midden).inflate(16));
    }

    /** Guh villagers that slept and want a golem summon an IJzerguhlem (the vanilla rules); vanilla villagers an iron golem. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void golemGuhVillagersMakenIJzerguhlem(GameTestHelper helper) {
        for (boolean guh : new boolean[]{true, false}) {
            BlockPos midden = helper.absolutePos(new BlockPos(10, 3, 10));   // (both rounds on the same spot: far away the chunks take no entities)
            vloer(helper, midden);
            long nu = helper.getLevel().getGameTime();
            Villager eerste = null;
            for (int i = 0; i < 5; i++) {
                Villager v = EntityType.VILLAGER.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                v.snapTo(midden.getX() + 0.5 + i, midden.getY(), midden.getZ() + 0.5, 0, 0);
                if (guh) {
                    v.setVillagerData(v.getVillagerData().withType(ModVillagers.GUH));
                }
                helper.getLevel().addFreshEntity(v);
                v.getBrain().setMemory(MemoryModuleType.LAST_SLEPT, nu);
                if (eerste == null) {
                    eerste = v;
                }
            }
            eerste.spawnGolemIfNeeded(helper.getLevel(), nu, 5);
            List<IronGolem> g = golems(helper, midden);
            if (g.size() != 1) {
                final long t = nu;
                List<Villager> buren = helper.getLevel().getEntitiesOfClass(Villager.class, eerste.getBoundingBox().inflate(10));
                helper.fail((guh ? "guh" : "vanilla") + " villagers: one golem, got " + g.size() + "; villagers near " + buren.size()
                        + ", wanting " + buren.stream().filter(b -> b.wantsToSpawnGolem(t)).count() + ", detected "
                        + buren.stream().filter(b -> b.getBrain().hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY)).count()
                        + ", floor " + helper.getLevel().getBlockState(midden.below()) + ", golem test spawn "
                        + net.minecraft.util.SpawnUtil.trySpawnMob(EntityType.IRON_GOLEM, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED,
                        helper.getLevel(), midden, 10, 8, 6, net.minecraft.util.SpawnUtil.Strategy.LEGACY_IRON_GOLEM, false).isPresent());
            }
            helper.assertTrue(guh == (g.get(0) instanceof IJzerguhlem), guh ? "an IJzerguhlem" : "a vanilla iron golem");
            for (IronGolem golem : g) {
                golem.discard();
            }
            for (Villager v : helper.getLevel().getEntitiesOfClass(Villager.class, new AABB(midden).inflate(16))) {
                v.discard();
            }
        }
        helper.succeed();
    }

    /** A villager that sees an IJzerguhlem remembers "a golem nearby" (so no new one is summoned), like with an iron golem. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void golemSensorZietIJzerguhlem(GameTestHelper helper) {
        Villager v = villager(helper, new BlockPos(2, 1, 2), true, helper.getLevel().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.NONE));
        IJzerguhlem golem = helper.spawn(GuhVillagersFeature.IJZERGUHLEM.get(), new BlockPos(5, 1, 5));
        v.getBrain().eraseMemory(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
        v.getBrain().setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, List.<LivingEntity>of(golem));
        GolemSensor.checkForNearbyGolem(v);
        helper.assertTrue(v.getBrain().hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY), "sees the IJzerguhlem as a golem");
        helper.succeed();
    }
}
