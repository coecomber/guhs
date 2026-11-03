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

    /** The floor-search spawner puts a cave creature ON the cave floor (not on the roof) with a level in range. */
    @GuhTest(template = "empty", batch = BATCH)
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
        VadsrijkMob m = WezensSpawners.spawn(sl, s);
        helper.assertTrue(m != null, "spawned");
        helper.assertTrue(m.getBlockY() == helper.absolutePos(new BlockPos(2, 2, 2)).getY(), "on the cave floor, not the roof: y=" + m.getBlockY());
        helper.assertTrue(m.mobLevel() >= 12 && m.mobLevel() <= 14, "level in range");
        helper.assertTrue("wezens:test_grot".equals(m.spawner), "spawner id");
        m.discard();
        helper.succeed();
    }

    /** A Moederklodder is always an elite; cheering her up = the plof (no death) and three kloddertjes hop out. */
    @GuhTest(template = "empty", batch = BATCH)
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
        helper.runAfterDelay(40, () -> {
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
}
