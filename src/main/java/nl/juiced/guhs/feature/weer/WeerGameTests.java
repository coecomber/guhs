package nl.juiced.guhs.feature.weer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Weer vadser (1.8.0, batch "weer"): unlocking per player with a weerpotje, the zone API inside and outside
 * its circle, the zone of a Weer vadser follows its vadskracht (back to normal without it), and chests can hold a weerpotje.
 */
public class WeerGameTests {
    private static final String BATCH = "weer", KAMER = "vadskracht_test_kamer";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        WeerUnlocks.wis(p);
        return p;
    }

    /** A weerpotje unlocks its weertje for that player only, and is used up; a second one is not (already had it). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void weerpotjeOntgrendeltPerSpeler(GameTestHelper helper) {
        ServerPlayer a = speler(helper), b = speler(helper);
        helper.assertTrue(WeerUnlocks.heeft(a, Weertje.REGEN) && WeerUnlocks.heeft(a, Weertje.ZON) && WeerUnlocks.heeft(a, Weertje.ONWEER),
                "zon, regen and onweer are always there");
        helper.assertFalse(WeerUnlocks.heeft(a, Weertje.PLUISJES), "pluisjessneeuw starts locked");
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WeerFeature.potje(Weertje.PLUISJES), 2));
        WeerFeature.POTJES.get(Weertje.PLUISJES).get().use(helper.getLevel(), a, InteractionHand.MAIN_HAND);
        helper.assertTrue(WeerUnlocks.heeft(a, Weertje.PLUISJES), "unlocked for a");
        helper.assertTrue(a.getMainHandItem().getCount() == 1, "one potje used up");
        helper.assertFalse(WeerUnlocks.heeft(b, Weertje.PLUISJES), "not for b");
        WeerFeature.POTJES.get(Weertje.PLUISJES).get().use(helper.getLevel(), a, InteractionHand.MAIN_HAND);
        helper.assertTrue(a.getMainHandItem().getCount() == 1, "already had it: nothing used up");
        helper.assertTrue(WeerUnlocks.potjeEenmalig(b, Weertje.STERREN) && !WeerUnlocks.potjeEenmalig(b, Weertje.STERREN),
                "a one-time source gives its potje once");
        WeerUnlocks.wis(a);
        WeerUnlocks.wis(b);
        helper.succeed();
    }

    /** In a zone its weather; 150 blocks away the world's own; rain in a rain zone under the open sky, not in a sunny one. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void weerZoneBinnenEnBuiten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos midden = helper.absolutePos(new BlockPos(4, 2, 5));
        WeerZones.testZone(level, midden, Weertje.REGEN);
        try {
            helper.assertTrue(WeerZones.weerOp(level, midden.offset(35, 0, 25)) == Weertje.REGEN, "35/25 blocks away: still the zone");
            helper.assertTrue(WeerZones.STRAAL == 60 && WeerZones.weerOp(level, midden.offset(62, 0, 0)) == Weertje.NORMAAL, "62 blocks away: outside the 60");
            WeerZones.Zone zone = WeerZones.zones(level).get(0);
            helper.assertTrue(zone.gewicht(midden.getX() + 56.5, midden.getZ() + 0.5) < 1f && zone.gewicht(midden.getX() + 50.5, midden.getZ() + 0.5) == 1f,
                    "a soft edge of about 8 blocks");
            helper.assertTrue(WeerZones.weerOp(level, midden.offset(150, 0, 0)) == Weertje.NORMAAL, "150 blocks away: the world's own");
            BlockPos lucht = new BlockPos(midden.getX() + 45, level.getMaxY() - 2, midden.getZ());
            helper.assertTrue(WeerZones.regentOp(level, lucht), "it rains under the open sky in the zone");
            WeerZones.wisTest(level);
            WeerZones.testZone(level, midden, Weertje.ZON);
            helper.assertFalse(WeerZones.regentOp(level, lucht), "a sunny zone is dry");
            WeerZones.wisTest(level);
            WeerZones.testZone(level, midden, Weertje.REGENBOOG);
            helper.assertTrue(WeerZones.regentOp(level, lucht), "the rainbow weather rains softly (puddles)");
        } finally {
            WeerZones.wisTest(level);
        }
        helper.succeed();
    }

    /** A Weer vadser with vadskracht holds its zone; without it (source gone) the zone fades and the weather is normal again. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void weerVadserVolgtVadskracht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bron = new BlockPos(1, 2, 1), blok = new BlockPos(2, 2, 1);
        helper.setBlock(bron, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 1));   // 10 VK: too little
        helper.setBlock(blok, WeerFeature.WEER_VADSER.get());
        WeerVadserBlockEntity be = (WeerVadserBlockEntity) level.getBlockEntity(helper.absolutePos(blok));
        be.zet(Weertje.HARTJES);
        BlockPos abs = helper.absolutePos(blok);
        helper.assertTrue(WeerFeature.KRACHT == 20, "the Weer vadser asks 20 vadskracht");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(WeerZones.weerOp(level, abs) == Weertje.NORMAAL, "10 vadskracht is not enough"))
                .thenExecute(() -> helper.setBlock(bron, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 2)))
                .thenWaitUntil(() -> helper.assertTrue(WeerZones.weerOp(level, abs) == Weertje.HARTJES, "the zone is there with 20 vadskracht"))
                .thenExecute(() -> helper.setBlock(bron, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(WeerZones.weerOp(level, abs) == Weertje.NORMAAL, "no vadskracht: normal weather again"))
                .thenExecute(() -> {
                    helper.setBlock(bron, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 2));
                })
                .thenWaitUntil(() -> helper.assertTrue(WeerZones.weerOp(level, abs) == Weertje.HARTJES, "vadskracht back: the zone too"))
                .thenExecute(() -> be.zet(Weertje.NORMAAL))
                .thenExecute(() -> helper.assertTrue(WeerZones.weerOp(level, abs) == Weertje.NORMAAL, "switched to normaal: gone at once"))
                .thenSucceed();
    }

    /** The chests of the sources can hold a weerpotje (about 1 in 8). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void weerpotjeInKisten(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/guh_picnic")));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 1, 1)))
                .create(LootContextParamSets.CHEST);
        int keer = 0, rollen = 400;
        for (int i = 0; i < rollen; i++) {
            for (ItemStack s : table.getRandomItems(params)) {
                if (s.is(WeerFeature.potje(Weertje.HARTJES))) {
                    keer++;
                }
            }
        }
        helper.assertTrue(keer > rollen / 8 / 3 && keer < rollen / 8 * 3, "a hartjesregen potje in about 1 in 8 picnic chests: " + keer + "/" + rollen);
        helper.succeed();
    }

    /** 1.8.0 weerrest: the four places without a chest got one (tools/features/weerrest.py), and it can hold their potje. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void weerpotjeKistjesOpNieuwePlekken(GameTestHelper helper) {
        var level = helper.getLevel();
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 1, 1)))
                .create(LootContextParamSets.CHEST);
        String[][] plekken = {{"wolkenhoeder_hut", "pluisjes"}, {"regenboogbrug", "regenboog"}, {"knuffelbad", "knusmist"},
                {"dal_theehuisje", "bloesem"}};
        for (String[] plek : plekken) {
            var template = level.getStructureManager().get(Guhs.id(plek[0]));
            helper.assertTrue(template.isPresent(), plek[0] + ": the template loads");
            var kisten = template.get().filterBlocks(BlockPos.ZERO,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), Blocks.CHEST);
            helper.assertTrue(!kisten.isEmpty(), plek[0] + ": has its little chest");
            var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/weer/" + plek[0])));
            Weertje w = java.util.Arrays.stream(Weertje.values()).filter(x -> x.id().equals(plek[1])).findFirst().orElseThrow();
            boolean gevonden = false;
            for (int i = 0; i < 400 && !gevonden; i++) {
                for (ItemStack s : table.getRandomItems(params)) {
                    gevonden |= s.is(WeerFeature.potje(w));
                }
            }
            helper.assertTrue(gevonden, plek[0] + ": its chest can hold the " + plek[1] + " potje");
        }
        helper.succeed();
    }
}
