package nl.juiced.guhs.feature.centrale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * 1.8.0 (vadskracht): the Mega-knuffelcentrale (5 x 3 with its parts, 16 cared-for guhs give 352 VK, hungry or stale
 * blankets give less, one per net) and the export trades of the guh villagers (batch "centrale").
 */
public class CentraleGameTests {
    private static final String BATCH = "centrale", KAMER = "techbron_test_kamer";

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    private static MegaKnuffelcentraleBlockEntity zet(GameTestHelper helper, BlockPos kern) {
        helper.setBlock(kern, CentraleFeature.CENTRALE.get());
        return (MegaKnuffelcentraleBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(kern));
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void centraleGeeftVeelMetZorg(GameTestHelper helper) {
        BlockPos kern = p(4, 4);
        MegaKnuffelcentraleBlockEntity c = zet(helper, kern);
        int delen = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (helper.getBlockState(kern.offset(dx, 0, dz)).is(CentraleFeature.DEEL.get())) {
                    delen++;
                }
            }
        }
        gelijk(helper, 14, delen, "the parts of the 5 x 3 cushion");
        gelijk(helper, 16, c.plekken().size(), "sixteen spots");
        gelijk(helper, 1, BronSoort.MEGA_KNUFFELCENTRALE.max, "one per net");

        c.zetGuhs(16);
        c.verzorg(helper.getLevel());
        gelijk(helper, false, c.gevoerd(), "no food yet");
        gelijk(helper, Math.round(16 * CentraleGetallen.PER_GUH * CentraleGetallen.HONGER_PROCENT / 100f), c.vadsAanbod(), "hungry guhs give little");

        gelijk(helper, 4, c.stop(new ItemStack(ModItems.KAAS_KNABBELS.get(), 4), false), "knabbels go in");
        c.verzorg(helper.getLevel());
        gelijk(helper, true, c.gevoerd(), "fed");
        gelijk(helper, Math.round(16 * CentraleGetallen.PER_GUH * CentraleGetallen.GEEN_DEKENS_PROCENT / 100f), c.vadsAanbod(), "fed, stale blankets");

        gelijk(helper, 1, c.stop(new ItemStack(Items.PINK_WOOL), false), "wool goes in");
        c.verzorg(helper.getLevel());
        gelijk(helper, true, c.dekensVers(), "fresh blankets");
        gelijk(helper, 16 * CentraleGetallen.PER_GUH, c.vadsAanbod(), "sixteen cared-for guhs");

        // they eat: 16 guhs eat a portion every 120/16 = 7.5 s; four knabbels are gone after ~30 s
        for (int s = 0; s < 40; s++) {
            c.verzorg(helper.getLevel());
        }
        gelijk(helper, false, c.gevoerd(), "the food ran out");
        gelijk(helper, Math.round(16 * CentraleGetallen.PER_GUH * CentraleGetallen.HONGER_PROCENT / 100f), c.vadsAanbod(), "hungry again");
        gelijk(helper, 0, c.stop(new ItemStack(Items.DIRT), false), "dirt does not go in");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void centraleExportHandel(GameTestHelper helper) {
        Villager v = helper.spawn(EntityType.VILLAGER, p(7, 7));
        v.setVillagerData(v.getVillagerData().withType(ModVillagers.GUH).withProfession(GuhVillagersFeature.VADSMONTEUR));
        helper.assertTrue(Export.ververs(v, 5L), "a new day: export trades");
        long export = v.getOffers().stream().filter(Export::isExport).count();
        gelijk(helper, (long) Export.PER_DAG, export, "export trades");
        for (MerchantOffer o : v.getOffers()) {
            if (Export.isExport(o)) {
                helper.assertTrue(o.getBaseCostA().getCount() >= 32, "bulk: " + o.getBaseCostA());
                gelijk(helper, Export.KEER, o.getMaxUses(), "uses per day");
            }
        }
        helper.assertFalse(Export.ververs(v, 5L), "the same day: no new ones");
        helper.assertTrue(Export.ververs(v, 6L), "the next day: new ones");
        gelijk(helper, (long) Export.PER_DAG, v.getOffers().stream().filter(Export::isExport).count(), "still two, the old ones are gone");
        // every profession's pool has at least two trades whose items exist today
        for (var lijst : Export.POOL.values()) {
            long bestaand = lijst.stream().filter(r -> BuiltInRegistries.ITEM.containsKey(Identifier.parse(r.wants()))
                    && BuiltInRegistries.ITEM.containsKey(Identifier.parse(r.gives()))).count();
            helper.assertTrue(bestaand >= Export.PER_DAG, "pool with too few existing trades: " + lijst);
        }
        helper.succeed();
    }
}
