package nl.juiced.guhs.feature.snackbar;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.frituur.FrituurFeature;
import nl.juiced.guhs.feature.frituur.SnackRecepten;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.registry.ModItems;

/** Game tests of De Guhsnackbar (batch "snackbar"): the template, its signs, the shop, learning a recipe, the compass. */
public class SnackbarGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "snackbar";

    @GuhTest(template = TUIN, batch = BATCH)
    public static void templateKlopt(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("snackbar")).orElseThrow();
        int tegels = 0;
        boolean[] gezien = new boolean[SnackbarBordBlock.TEGELS];
        var infos = t.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                SnackbarFeature.BORD.get());
        for (var info : infos) {
            BlockState s = info.state();
            int tegel = s.getValue(SnackbarBordBlock.TEGEL);
            helper.assertTrue(!gezien[tegel], "tile " + tegel + " twice");
            gezien[tegel] = true;
            tegels++;
        }
        helper.assertTrue(tegels == SnackbarBordBlock.TEGELS, "every tile of every sign is in the template: " + tegels);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void winkelVerkooptInKnabbels(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.SNACKBARGUH);
        var offers = SnackbarGuh.ROL.offers(npc);
        helper.assertTrue(offers.size() == 8, "eight snacks: " + offers.size());
        offers.forEach(o -> helper.assertTrue(o.getCostA().is(ModItems.KAAS_KNABBELS.get()), "paid in kaas knabbels"));
        helper.assertTrue(offers.stream().noneMatch(o -> o.getResult().is(FrituurFeature.VETSTE_HAP.get())), "De Vetste Hap you make yourself");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void receptLerenKost(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(SnackRecepten.ALLE.stream().allMatch(SnackbarGuh.PRIJZEN::containsKey), "every recipe has a price");
        helper.assertTrue(SnackbarGuh.nogTeLeren(p).size() == SnackRecepten.ALLE.size(), "knows nothing yet");
        helper.assertTrue(!SnackbarGuh.leer(p, "kaaskroket"), "no knabbels, no recipe");
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        helper.assertTrue(SnackbarGuh.leer(p, "kaaskroket") && SnackRecepten.kent(p, "kaaskroket"), "paid and learned");
        helper.assertTrue(p.getInventory().countItem(ModItems.KAAS_KNABBELS.get()) == 32, "32 knabbels gone");
        helper.assertTrue(!SnackbarGuh.leer(p, "kaaskroket"), "learned for good: no paying twice");
        helper.assertTrue(p.getInventory().countItem(ModItems.KAAS_KNABBELS.get()) == 32, "nothing taken twice");
        helper.assertTrue(!SnackbarGuh.nogTeLeren(p).contains("kaaskroket"), "not offered again");
        helper.assertTrue("vetste_hap".equals(SnackbarGuh.nogTeLeren(p).getLast()), "the summit comes last");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void kompasEnBescherming(GameTestHelper helper) {
        helper.assertTrue(SnackbarFeature.KOMPAS.get() instanceof GuhCompassItem, "a guh compass");
        helper.assertTrue(SuperkompasItem.categoryOf("snackbar") >= 0, "in the Superkompas too");
        Player speler = GuhMockPlayer.of(helper);
        helper.assertTrue(!SnackbarBescherming.denied(speler, false), "outside: fine");
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getValue(SnackbarFeature.STRUCTUUR) != null, "the structure exists");
        helper.assertTrue(((Block) SnackbarFeature.BORD.get()) != null, "the sign block exists");
        helper.succeed();
    }
}
