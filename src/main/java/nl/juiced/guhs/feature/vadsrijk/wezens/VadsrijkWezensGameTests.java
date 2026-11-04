package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.SetBonussen;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Klodders;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Het Vadsrijk wezens: only the basics (spawner, defeat, loot, sets). Run: {@code runGameTestServer -Pgt=VadsrijkWezens}.
 */
public class VadsrijkWezensGameTests {
    private static final String BATCH = "vadsrijk_wezens";
    /** Without mock players (a spawner never spawns within 12 of a player). */
    private static final String BATCH_LEVEN = "vadsrijk_wezens_leven";

    /** The floor-search spawner puts a cave creature ON the cave floor (not on the roof) with a level in range. */
    @GuhTest(template = "empty", batch = BATCH_LEVEN)
    public static void grotSpawnerVindtDeVloer(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE.defaultBlockState());
            }
        }
        BlockPos midden = helper.absolutePos(new BlockPos(2, 3, 2));
        Spawners.Spawner s = new Spawners.Spawner("wezens:test_grot", midden, 1, WezensRegistry.KNAAG, 3, 100, 12, 14, Element.GEEN, 0);
        VadsrijkMob m = WezensSpawners.spawn(sl, WezensSpawners.grot(s));
        helper.assertTrue(m != null, "spawned");
        helper.assertTrue(m.getBlockY() == helper.absolutePos(new BlockPos(2, 2, 2)).getY(), "on the cave floor, not the roof: y=" + m.getBlockY());
        helper.assertTrue(m.mobLevel() >= 12 && m.mobLevel() <= 14, "level in range");
        helper.assertTrue("wezens:test_grot".equals(m.spawner), "spawner id");
        m.discard();
        helper.succeed();
    }

    /** A Moederklodder is always an elite; cheering her up = the plof (no death) and three kloddertjes hop out. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void moederklodderPloftEnSplitst(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        RijkWezen m = Klodders.maak(sl, WezensRegistry.MOEDERKLODDER.get(), pos, 4, false);
        helper.assertTrue(m != null && m.elite() && m.eliteZichtbaar(), "always elite");
        m.krijgSchade(m.vads() + 1, p);
        helper.assertTrue(m.geplofd() && m.isAlive(), "plof, not dead");
        List<Klodders.Klodder> kleintjes = sl.getEntitiesOfClass(Klodders.Klodder.class, new AABB(pos, pos).inflate(4), Klodders.Klodder::kloddertje);
        helper.assertTrue(kleintjes.size() == 3, "three kloddertjes, got " + kleintjes.size());
        helper.assertTrue(kleintjes.stream().noneMatch(Klodders.Klodder::geplofd), "the kloddertjes are still ontvadst");
        kleintjes.forEach(net.minecraft.world.entity.Entity::discard);
        helper.runAfterDelay(230, () -> {
            helper.assertTrue(m.isRemoved(), "hopped away and vanished");
            helper.succeed();
        });
    }

    /** Every creature has a table and an elite table, and what they roll is real gear of a known base type. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootTabellenGeldig(GameTestHelper helper) {
        RandomSource r = RandomSource.create(110);
        int items = 0;
        for (String soort : WezensRegistry.SOORTEN.keySet()) {
            if (soort.equals("kloddertje")) {
                continue;
            }
            LootTabel t = LootTabellen.van("wezens:" + soort), e = LootTabellen.van("wezens:" + soort + "_elite");
            helper.assertTrue(t != null && e != null, "tables of " + soort);
            for (int i = 0; i < 20; i++) {
                LootTabel.Resultaat res = e.rol(10, r);
                helper.assertTrue(res.munten() > 0, "coins");
                for (ItemStack s : res.items()) {
                    Uitrusting u = Rollen.van(s);
                    helper.assertTrue(u != null && BasisTypes.van(u.type()) != null && u.level() == 10, "a rolled piece of " + soort);
                    items++;
                }
            }
        }
        helper.assertTrue(items > 20, "elites drop plenty, got " + items);
        helper.succeed();
    }

    /**
     * Integration 1.10.0: every entry of every loot table (kern, wezens, bazen, klassen, economie) is a registered base type whose
     * item exists, and every weapon base type's kind is one its class accepts (klassen's wapenSoorten).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void alleTabellenEnWapensKloppen(GameTestHelper helper) {
        int regels = 0;
        for (var e : LootTabellen.alle().entrySet()) {
            for (LootTabel.Regel g : e.getValue().regels()) {
                helper.assertTrue(BasisTypes.van(g.basisType()) != null, "table " + e.getKey() + ": unknown base type " + g.basisType());
                regels++;
            }
        }
        helper.assertTrue(LootTabellen.van("economie:baas_koning_klodder") != null && LootTabellen.van("economie:zone1") != null, "economie boss tables");
        for (var t : BasisTypes.alle()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse(t.itemId()));
            helper.assertTrue(item.isPresent(), "item of base type " + t.id() + ": " + t.itemId());
            if (t.slot() == nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.WAPEN) {
                var k = nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.van(t.klasse());
                helper.assertTrue(k != null && k.wapenSoorten().contains(t.soort()), "weapon " + t.id() + " (" + t.soort() + ") fits class " + t.klasse());
            }
        }
        helper.assertTrue(regels > 100, "tables have entries: " + regels);
        helper.succeed();
    }

    /** Wearing the four Klodderset pieces counts as 4 (and gives the stat part); one Zwamset piece is 1. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void setStukkenTellen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        RandomSource r = RandomSource.create(4);
        for (int i = 0; i < 4; i++) {
            ItemStack s = Rollen.stack(Rollen.rol(BasisTypes.van(SetBonussen.KLODDERSET.get(i)), 3, Zeldzaamheid.GEWOON, r));
            helper.assertTrue(!s.isEmpty(), "item of " + SetBonussen.KLODDERSET.get(i));
            p.setItemSlot(slots[i], s);
        }
        helper.assertTrue(SetBonussen.stukken(p, SetBonussen.KLODDER) == 4, "4 pieces");
        helper.assertTrue(SetBonussen.stats(p).get(nl.juiced.guhs.feature.vadsrijk.kern.Stat.BEHENDIGHEID) == 4, "stat part");
        p.setItemSlot(EquipmentSlot.HEAD, Rollen.stack(Rollen.rol(BasisTypes.van("zwamhoed"), 3, Zeldzaamheid.GEWOON, r)));
        helper.assertTrue(SetBonussen.stukken(p, SetBonussen.KLODDER) == 3 && SetBonussen.stukken(p, SetBonussen.ZWAM) == 1, "3 + 1");
        helper.assertTrue(Set.copyOf(SetBonussen.KLODDERSET).size() == 4, "4 distinct");
        helper.succeed();
    }

    /**
     * Fix round 1 ("leven"): a waking spawner fills up at once but never over its max, the area room caps it, a beaten mob
     * comes back only after the respawn time, nothing spawns within the safe radius of a campfire, mushroom caps are no
     * ground, and the zone plan has MMO density (the sums of the spawner maxima per area: ~2.5x the area cap, so one player's
     * 80-block bubble is full while the area caps keep the total at the target).
     */
    @GuhTest(template = "empty", batch = BATCH_LEVEN, timeoutTicks = 200)
    public static void spawnerCapsEnVeiligeStraal(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 6, z), Blocks.STONE.defaultBlockState());
            }
        }
        BlockPos midden = helper.absolutePos(new BlockPos(4, 2, 4));
        Spawners.Spawner s = new Spawners.Spawner("wezens:test_caps", midden, 3, WezensRegistry.KLODDER, 3, 100, 1, 2, Element.WATER, 0)
                .grot(midden.getY() - 3, midden.getY() + 2);
        Spawners.vergeetToestand();
        long nu = sl.getGameTime();
        int n = Spawners.stap(sl, s, 0, nu, 100);
        helper.assertTrue(n == 3, "a fresh spawner fills up at once: " + n);
        List<VadsrijkMob> mobs = sl.getEntitiesOfClass(VadsrijkMob.class, new AABB(midden).inflate(8), m -> "wezens:test_caps".equals(m.spawner));
        helper.assertTrue(mobs.size() == 3 && Spawners.levend(sl, s) == 3, "3 alive: " + mobs.size());
        helper.assertTrue(mobs.stream().allMatch(m -> m.getBlockY() == midden.getY()), "all on the floor");
        helper.assertTrue(Spawners.stap(sl, s, 3, nu + 20, 100) == 0, "never over the max");
        mobs.get(0).discard();
        helper.assertTrue(Spawners.stap(sl, s, 2, nu + 40, 100) == 0, "a beaten one waits for its respawn time");
        helper.assertTrue(Spawners.stap(sl, s, 2, nu + 60, 100) == 0, "still waiting");
        helper.assertTrue(Spawners.stap(sl, s, 2, nu + 160, 100) == 1, "back after the respawn time");
        // the area room
        Spawners.Spawner s2 = new Spawners.Spawner("wezens:test_caps2", midden, 3, WezensRegistry.KLODDER, 4, 100, 1, 2, Element.WATER, 0)
                .grot(midden.getY() - 3, midden.getY() + 2);
        helper.assertTrue(Spawners.stap(sl, s2, 0, nu, 1) == 1, "the area cap allows only 1");
        // the safe radius (a campfire spot right here)
        Spawners.VEILIGE_PLEKKEN.add(midden);
        try {
            for (int i = 0; i < 20; i++) {
                helper.assertTrue(Spawners.spawn(sl, s) == null, "nothing within the safe radius of a campfire");
            }
        } finally {
            Spawners.VEILIGE_PLEKKEN.remove(midden);
        }
        helper.assertTrue(Spawners.slechteVloer(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()) && Spawners.slechteVloer(Blocks.OAK_LEAVES.defaultBlockState())
                && !Spawners.slechteVloer(Blocks.GRASS_BLOCK.defaultBlockState()) && !Spawners.slechteVloer(Blocks.DIRT_PATH.defaultBlockState()), "ground test");
        sl.getEntitiesOfClass(VadsrijkMob.class, new AABB(midden).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
        Spawners.vergeetToestand();
        // the plan
        var plan = WezensSpawners.PLAN;
        com.mojang.logging.LogUtils.getLogger().info("[leven] spawner plan (sum of maxima per area): {} ({} surface + {} floor-search spawners)", plan,
                WezensSpawners.aantalOppervlak(), WezensSpawners.aantalVloer());
        helper.assertTrue(plan.getOrDefault("klodderweide", 0) >= 160, "klodderweide dense: " + plan);
        helper.assertTrue(plan.getOrDefault("sporenwoud", 0) >= 170, "sporenwoud dense: " + plan);
        helper.assertTrue(plan.getOrDefault("druipgrotten", 0) >= 160, "druipgrotten dense: " + plan);
        helper.assertTrue(plan.getOrDefault("zwamtempel", 0) + plan.getOrDefault("ring", 0) >= 22, "temple path: " + plan);
        helper.succeed();
    }
}
