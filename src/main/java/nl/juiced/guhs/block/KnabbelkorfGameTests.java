package nl.juiced.guhs.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhBeeEntity;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;

/** 1.6.0 (F0-fix, a player's bug report): a knabbelkorf picked up with silk touch keeps its bees, like a vanilla beehive. */
public class KnabbelkorfGameTests {
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY)
    public static void knabbelkorfHoudtBijenMetSilkTouch(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.KNABBELKORF.get().defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 3));
        BeehiveBlockEntity korf = helper.getBlockEntity(pos, BeehiveBlockEntity.class);
        GuhBeeEntity bij = helper.spawn(ModEntities.GUH_BEE.get(), new BlockPos(2, 2, 2));
        korf.addOccupant(bij);
        helper.assertTrue(korf.getOccupantCount() == 1, "the guh bee went in");

        ItemStack pikhouweel = new ItemStack(Items.DIAMOND_PICKAXE);
        pikhouweel.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockState staat = helper.getBlockState(pos);
        List<ItemStack> buit = Block.getDrops(staat, helper.getLevel(), helper.absolutePos(pos), korf, null, pikhouweel);
        helper.assertTrue(buit.size() == 1 && buit.get(0).is(ModBlocks.KNABBELKORF.get().asItem()), "one knabbelkorf: " + buit);
        ItemStack item = buit.get(0);
        Bees bijen = item.get(DataComponents.BEES);
        helper.assertTrue(bijen != null && bijen.bees().size() == 1, "the item knows its bee: " + bijen);
        BlockItemStateProperties honing = item.get(DataComponents.BLOCK_STATE);
        helper.assertTrue(honing != null && Integer.valueOf(3).equals(honing.get(BeehiveBlock.HONEY_LEVEL)), "and its honey: " + honing);

        // without silk touch: an empty korf (like vanilla)
        List<ItemStack> kaal = Block.getDrops(staat, helper.getLevel(), helper.absolutePos(pos), korf, null, new ItemStack(Items.DIAMOND_AXE));
        helper.assertTrue(kaal.size() == 1 && kaal.get(0).getOrDefault(DataComponents.BEES, Bees.EMPTY).bees().isEmpty(), "no bees without silk touch");
        helper.assertTrue(new ItemStack(ModBlocks.KNABBELKORF.get()).has(DataComponents.BEES), "a fresh korf item shows 'Bijen: 0' like a beehive");

        // placing it again puts the bee back
        BlockPos plek = new BlockPos(4, 1, 2);
        helper.setBlock(plek, ModBlocks.KNABBELKORF.get().defaultBlockState());
        BeehiveBlockEntity nieuw = helper.getBlockEntity(plek, BeehiveBlockEntity.class);
        nieuw.applyComponentsFromItemStack(item);
        helper.assertTrue(nieuw.getOccupantCount() == 1, "the bee is back in the placed korf");
        helper.succeed();
    }
}
