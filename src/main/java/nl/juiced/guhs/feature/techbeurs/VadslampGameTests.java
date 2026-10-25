package nl.juiced.guhs.feature.techbeurs;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * The Vadslamp (feature/techbeurs): it burns (light 15, awake face) on vadskracht and puts its invisible light blocks in the
 * air around it, not through a wall; they all go when the power goes and at once when the lamp is broken. Template
 * techbeurs/vadslamp_kamer: 15 x 9 x 15, stone floor (things stand at helper y 2), a stone wall at x 10.
 */
public class VadslampGameTests {
    private static final String KAMER = "techbeurs/vadslamp_kamer", BATCH = "vadslamp";
    private static final BlockPos LAMP = new BlockPos(7, 2, 7), DRAAD = new BlockPos(7, 2, 6), BRON = new BlockPos(7, 2, 5);

    private static VadslampBlockEntity opstelling(GameTestHelper helper) {
        helper.setBlock(BRON, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 1));
        helper.setBlock(DRAAD, ModBlocks.GUH_WIRE.get());
        helper.setBlock(LAMP, TechbeursFeature.VADSLAMP.get());
        return (VadslampBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(LAMP));
    }

    private static int lichtenRond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int n = 0;
        BlockPos lamp = helper.absolutePos(LAMP);
        for (BlockPos p : BlockPos.betweenClosed(lamp.offset(-8, -2, -8), lamp.offset(8, 7, 8))) {
            if (level.getBlockState(p).is(TechbeursFeature.VADSLAMP_LICHT.get())) {
                n++;
            }
        }
        return n;
    }

    private static void brandt(GameTestHelper helper, VadslampBlockEntity lamp) {
        BlockState s = helper.getBlockState(LAMP);
        helper.assertTrue(lamp.heeftKracht() && lamp.snoet() == Snoet.WERKT, "the lamp has vadskracht and is awake");
        helper.assertTrue(s.getLightEmission() == 15, "the lamp burns at 15, not " + s.getLightEmission());
        helper.assertTrue(lamp.lichten().size() >= 8, "at least 8 light blocks around it, got " + lamp.lichten());
    }

    /** With power: light 15, awake, its light blocks in the air around it (never through the wall, never right next to it). */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void vadslampBrandtMetKracht(GameTestHelper helper) {
        VadslampBlockEntity lamp = opstelling(helper);
        helper.assertTrue(helper.getBlockState(LAMP).getLightEmission() == 0, "a new lamp is dark");
        helper.succeedWhen(() -> {
            brandt(helper, lamp);
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(LAMP);
            helper.assertTrue(VadsKracht.net(level, abs).vraag() == 1, "it asks 1 VK");
            for (BlockPos p : lamp.lichten()) {
                helper.assertTrue(level.getBlockState(p).is(TechbeursFeature.VADSLAMP_LICHT.get()), "a light block at " + p);
                helper.assertTrue(p.distManhattan(abs) >= 2, "not right next to the lamp: " + p);
                helper.assertTrue(p.getX() - abs.getX() < 3, "not through the wall at x 10: " + p);
                helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p) >= VadslampLicht.LICHT, "it shines at " + p);
            }
            BlockPos ver = abs.offset(-6, 0, 0);
            helper.assertTrue(lamp.lichten().contains(ver), "a light 6 blocks out where the room is open: " + lamp.lichten());
            helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, abs.offset(-6, 0, -3)) >= 10,
                    "9 blocks out it is still bright (the lamp alone gives 6 there)");
            helper.assertTrue(lichtenRond(helper) == lamp.lichten().size(), "no other light blocks around");
        });
    }

    /** The power goes: after a moment the lamp sleeps and every light block is gone. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void vadslampLichtenWegZonderKracht(GameTestHelper helper) {
        VadslampBlockEntity lamp = opstelling(helper);
        helper.runAfterDelay(60, () -> {
            brandt(helper, lamp);
            helper.setBlock(BRON, Blocks.AIR);
            helper.runAfterDelay(VadslampBlockEntity.UIT_NA + 30, () -> {
                helper.assertTrue(helper.getBlockState(LAMP).getLightEmission() == 0 && lamp.snoet() == Snoet.SLAAPT, "the lamp sleeps");
                helper.assertTrue(lichtenRond(helper) == 0 && lamp.lichten().isEmpty(), "every light block is gone, found " + lichtenRond(helper));
                helper.succeed();
            });
        });
    }

    /** Broken while it burns: its lights go at once; a light that lost its lamp otherwise goes by itself. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void vadslampLichtenWegNaBreken(GameTestHelper helper) {
        VadslampBlockEntity lamp = opstelling(helper);
        helper.runAfterDelay(60, () -> {
            brandt(helper, lamp);
            List<BlockPos> waren = lamp.lichten();
            helper.getLevel().destroyBlock(helper.absolutePos(LAMP), true);
            helper.assertTrue(lichtenRond(helper) == 0, "the lights go with the lamp at once, found " + lichtenRond(helper));
            // a stray light (as after a crash): no lamp burns near it, so it goes by itself
            BlockPos los = helper.absolutePos(new BlockPos(3, 3, 3));
            helper.getLevel().setBlock(los, TechbeursFeature.VADSLAMP_LICHT.get().defaultBlockState(), 3);
            helper.runAfterDelay(VadslampLicht.KIJK + 10, () -> {
                helper.assertTrue(helper.getLevel().getBlockState(los).isAir(), "a light without a lamp goes by itself");
                helper.assertTrue(!waren.isEmpty(), "(it had lights)");
                helper.succeed();
            });
        });
    }

    /** A block placed into a light block replaces it; the lamp then puts that light in the next free spot of the ray. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void vadslampGeeftRuimteAanBlokken(GameTestHelper helper) {
        VadslampBlockEntity lamp = opstelling(helper);
        helper.runAfterDelay(60, () -> {
            BlockPos abs = helper.absolutePos(LAMP), ver = abs.offset(-6, 0, 0);
            helper.assertTrue(lamp.lichten().contains(ver), "a light 6 blocks out: " + lamp.lichten());
            helper.assertTrue(helper.getLevel().getBlockState(ver).canBeReplaced(), "a light block can be built over");
            helper.getLevel().setBlock(ver, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            helper.succeedWhen(() -> helper.assertTrue(lamp.lichten().contains(abs.offset(-5, 0, 0)) && !lamp.lichten().contains(ver),
                    "the light moved in to 5 blocks out: " + lamp.lichten()));
        });
    }

    /** The recipe (a lantern, two Guhdraad, two glowstone dust) and the names. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void vadslampReceptEnNaam(GameTestHelper helper) {
        ItemStack leeg = ItemStack.EMPTY, g = new ItemStack(Items.GLOWSTONE_DUST), d = new ItemStack(ModBlocks.GUH_WIRE.get().asItem());
        CraftingInput in = CraftingInput.of(3, 3, List.of(leeg, g, leeg, d, new ItemStack(Items.LANTERN), d, leeg, g, leeg));
        var recept = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, helper.getLevel());
        helper.assertTrue(recept.isPresent() && recept.get().value().assemble(in).is(TechbeursFeature.VADSLAMP_ITEM.get()), "the recipe makes a Vadslamp");
        helper.assertTrue(Language.getInstance().has("block.guhs.vadslamp") && Language.getInstance().has("block.guhs.vadslamp.lore"), "it has a name");
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                .getOrThrow(VadsKracht.TOON).stream().anyMatch(h -> h.value() == TechbeursFeature.VADSLAMP.get()), "the hover readout works on it");
        helper.succeed();
    }
}
