package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Vahoege-vadsschaar (1.3.1): it never breaks (no durability at all), it is crafted like shears from two vahoege
 * vads, and it counts as shears everywhere: the tag, the shears abilities, the blocks shears cut, a sheep, the
 * knabbelkorf and the dispenser.
 */
public class VadsSchaarGameTests {
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY)
    public static void vadsSchaarIsEenSchaarDieNooitKapotGaat(GameTestHelper helper) {
        ItemStack schaar = new ItemStack(ModItems.VADS_SHEARS.get());
        // never breaks: no durability, no bar
        helper.assertTrue(schaar.has(DataComponents.UNBREAKABLE) && !schaar.isDamageableItem() && schaar.getMaxDamage() == 0 && !schaar.isBarVisible(),
                "no durability at all");
        // counts as shears
        helper.assertTrue(schaar.getItem() instanceof net.minecraft.world.item.ShearsItem && schaar.is(Tags.Items.TOOLS_SHEAR),
                "a ShearsItem in c:tools/shear");
        for (ItemAbility a : ItemAbilities.DEFAULT_SHEARS_ACTIONS) {
            helper.assertTrue(schaar.canPerformAction(a) == new ItemStack(Items.SHEARS).canPerformAction(a) && schaar.canPerformAction(a), "ability " + a.name());
        }
        ItemStack gewoon = new ItemStack(Items.SHEARS);
        for (var block : List.of(Blocks.OAK_LEAVES, Blocks.COBWEB, Blocks.WHITE_WOOL, Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.STONE)) {
            helper.assertTrue(schaar.getDestroySpeed(block.defaultBlockState()) == gewoon.getDestroySpeed(block.defaultBlockState()),
                    "as fast as shears on " + block);
        }
        helper.assertTrue(schaar.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()) && schaar.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState()) > 1f,
                "cuts cobweb and leaves");
        helper.assertTrue(schaar.is(nl.juiced.guhs.event.KeepOnDeathHandler.KEEP_ON_DEATH), "vads gear: kept on death");
        helper.assertTrue(DispenserBlock.DISPENSER_REGISTRY.get(schaar.getItem()) instanceof net.minecraft.core.dispenser.ShearsDispenseItemBehavior,
                "a dispenser uses it like shears");
        Language lang = Language.getInstance();
        helper.assertTrue(lang.has("item.guhs.vahoege_vads_shears") && lang.has("item.guhs.vahoege_vads_shears.lore"), "name and tooltip");
        // the recipe: the shears shape, with vahoege vads (both mirrors); iron or the ingot does not give it
        ItemStack v = new ItemStack(ModItems.VAHOEGE_VADS.get()), leeg = ItemStack.EMPTY;
        for (List<ItemStack> rooster : List.of(List.of(leeg, v, v, leeg), List.of(v, leeg, leeg, v))) {
            var recept = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, rooster), helper.getLevel());
            helper.assertTrue(recept.isPresent() && recept.get().value().assemble(CraftingInput.of(2, 2, rooster)).is(ModItems.VADS_SHEARS.get()),
                    "two vahoege vads diagonally make the shears");
        }
        ItemStack staaf = new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get());
        var metStaaf = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, List.of(leeg, staaf, staaf, leeg)), helper.getLevel());
        helper.assertTrue(metStaaf.isEmpty(), "not from ingots");
        // a sheep: sheared, and the shears are as new, again and again
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, schaar);
        for (int i = 0; i < 5; i++) {
            Sheep schaap = helper.spawn(EntityType.SHEEP, new BlockPos(2, 1, 2));
            helper.assertTrue(schaap.readyForShearing(), "a woolly sheep");
            helper.assertTrue(schaar.interactLivingEntity(player, schaap, InteractionHand.MAIN_HAND).consumesAction() && schaap.isSheared(), "sheared " + i);
            schaap.discard();
        }
        helper.assertTrue(!schaar.isEmpty() && schaar.getDamageValue() == 0 && player.getMainHandItem() == schaar, "still whole after five sheep");
        // mining with it costs nothing either
        schaar.mineBlock(helper.getLevel(), Blocks.OAK_LEAVES.defaultBlockState(), helper.absolutePos(new BlockPos(1, 1, 1)), player);
        schaar.hurtAndBreak(500, player, InteractionHand.MAIN_HAND.asEquipmentSlot());
        helper.assertTrue(!schaar.isEmpty() && schaar.getDamageValue() == 0, "nothing wears it out");
        // the mod's own knabbelkorf takes it too
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.KNABBELKORF.get().defaultBlockState().setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 5));
        BlockPos abs = helper.absolutePos(pos);
        helper.getLevel().getBlockState(abs).useItemOn(schaar, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
        helper.assertTrue(helper.getBlockState(pos).getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 0 && !schaar.isEmpty(),
                "the knabbelkorf is emptied with it");
        helper.succeed();
    }
}
