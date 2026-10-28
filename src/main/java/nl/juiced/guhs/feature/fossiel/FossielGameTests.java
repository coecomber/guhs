package nl.juiced.guhs.feature.fossiel;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Fossiel-broedmachine and the oerguhs (1.8.0, batch "fossiel"): a fossil with vadskracht and knabbels
 * broods into a baby of the right oerguh, the Mammoet-guh's chores go twice as fast, the T-Guh watches further, the four
 * pages are bonus pages, and the old fossil chests can hold a fossil.
 */
public class FossielGameTests {
    private static final String BATCH = "fossiel", KAMER = "vadskracht_test_kamer";

    /** A T-Guh fossil, 16 knabbels and 20 vadskracht: it eats the knabbels, broods, and hatches a baby T-Guh on top. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void fossielBroedtJuisteVariant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bron = new BlockPos(1, 2, 1), blok = new BlockPos(2, 2, 1);
        helper.setBlock(bron, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 2));   // 20 VK
        helper.setBlock(blok, FossielFeature.BROEDMACHINE.get());
        BroedmachineBlockEntity be = (BroedmachineBlockEntity) level.getBlockEntity(helper.absolutePos(blok));
        Kisten.stop(be.handler(null), new ItemStack(FossielFeature.FOSSIELEN.get(OerSoort.TGUH).get()));
        Kisten.stop(be.handler(null), new ItemStack(ModItems.KAAS_KNABBELS.get(), 20));
        helper.assertTrue(Kisten.stop(be.handler(null), new ItemStack(FossielFeature.FOSSIELEN.get(OerSoort.MAMMOET).get())).getCount() == 1,
                "a second kind of fossil does not go in");
        helper.assertTrue(be.ei() == OerSoort.TGUH, "the T-Guh fossil in the glass");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(be.voortgangTicks() > 0, "it starts brooding with vadskracht"))
                .thenExecute(() -> {
                    helper.assertTrue(Kisten.tel(be.vakken(), s -> s.is(ModItems.KAAS_KNABBELS.get())) == 20 - FossielFeature.KNABBELS,
                            "16 knabbels eaten at the start");
                    be.zetVoortgang(FossielFeature.BROED_TICKS - 2);
                })
                .thenWaitUntil(() -> {
                    List<GuhEntity> babies = level.getEntitiesOfClass(GuhEntity.class, new AABB(helper.absolutePos(blok)).inflate(3));
                    helper.assertTrue(babies.size() == 1 && babies.get(0).getVariant() == GuhVariant.TGUH && babies.get(0).isBaby(),
                            "a baby T-Guh hatched");
                })
                .thenExecute(() -> {
                    helper.assertTrue(be.ei() == null && be.voortgangTicks() == 0 && Kisten.tel(be.vakken(), s -> OerSoort.van(s) != null) == 0,
                            "the fossil is used up, the glass is empty");
                    level.getEntitiesOfClass(GuhEntity.class, new AABB(helper.absolutePos(blok)).inflate(3)).forEach(GuhEntity::discard);
                })
                .thenSucceed();
    }

    /** The oerguhs' bonuses: a Mammoet-guh's chores twice as fast, a T-Guh watches further; four bonus Guhdex pages. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void oerguhsBonussen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhEntity mammoet = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
        GuhEntity gewoon = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
        GuhEntity tguh = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
        mammoet.setVariant(GuhVariant.MAMMOETGUH);
        gewoon.setVariant(GuhVariant.NORMAL);
        tguh.setVariant(GuhVariant.TGUH);
        helper.assertTrue(Band.klusSnelheid(mammoet) == 2f * Band.klusSnelheid(gewoon), "the Mammoet-guh's chores go twice as fast");
        helper.assertTrue(OerGuhs.wakenExtra(tguh, 6) == OerGuhs.TGUH_EXTRA && OerGuhs.wakenExtra(gewoon, 6) == 6, "the T-Guh watches further");
        for (OerSoort s : OerSoort.values()) {
            helper.assertTrue(s.variant.isOerGuh() && GuhDex.ENTRIES.contains(s.variant) && GuhDex.EXTRA.contains(s.variant)
                    && s.variant.geenKnuffel(), s.variant + ": a bonus page, no plush");
        }
        helper.succeed();
    }

    /** The reuzenguh-fossiel's chest can hold a Mammoet-guh fossil (about 1 in 5). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void fossielInKisten(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Guhs.id("chests/onderwol/reuzenguh_fossiel")));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 1, 1)))
                .create(LootContextParamSets.CHEST);
        int keer = 0;
        for (int i = 0; i < 300; i++) {
            for (ItemStack s : table.getRandomItems(params)) {
                if (s.is(FossielFeature.FOSSIELEN.get(OerSoort.MAMMOET).get())) {
                    keer++;
                }
            }
        }
        helper.assertTrue(keer > 20 && keer < 120, "about 1 in 5 chests: " + keer + " of 300");
        helper.succeed();
    }
}
