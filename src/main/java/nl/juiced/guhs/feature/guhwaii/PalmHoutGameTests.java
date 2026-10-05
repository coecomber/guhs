package nl.juiced.guhs.feature.guhwaii;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the guh-palm wood set (1.2.8, {@link GuhwaiiFeature#PALM_HOUTSET}): every block has an item that places
 * it, the axe strips the trunk (and the face trunk), the recipes craft, the sign holds text, the tags, the drops and
 * the fire. Each test has its own batch (palmhout_*); run them all with {@code -Pgt=PalmHoutGameTests}.
 */
public class PalmHoutGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhwaiiGameTests.speler(helper, new BlockPos(0, 2, 0));
        p.setGameMode(GameType.CREATIVE);
        return p;
    }

    /** A click with this stack on a face of a (relative) block. */
    private static void klik(GameTestHelper helper, ServerPlayer p, ItemStack stack, BlockPos op, Direction kant) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(op);
        Vec3 plek = Vec3.atCenterOf(abs).add(kant.getStepX() * 0.5, kant.getStepY() * 0.5, kant.getStepZ() * 0.5);
        stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(plek, kant, abs, false)));
    }

    /** Every block of the set has an item, and that item (in a player's hand, on a floor) places the block. */
    @GuhTest(template = EMPTY, batch = "palmhout_blokken")
    public static void palmhoutElkBlokHeeftEenItemEnIsTePlaatsen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            List<DeferredBlock<?>> set = GuhwaiiFeature.PALM_HOUTSET;
            helper.assertTrue(set.size() == 8, "stripped log, stairs, slab, fence, gate, door, trapdoor, sign");
            BlockPos vloer = new BlockPos(2, 1, 2);
            helper.setBlock(vloer, Blocks.STONE);
            for (DeferredBlock<?> blok : set) {
                Item item = blok.get().asItem();
                helper.assertTrue(item != Items.AIR, blok.getId() + " has an item");
                helper.assertTrue(item.getDescriptionId().equals(blok.get().getDescriptionId()), blok.getId() + ": the item is named like the block");
                klik(helper, p, new ItemStack(item), vloer, Direction.UP);
                helper.assertBlockPresent(blok.get(), vloer.above());
                if (blok == GuhwaiiFeature.PALM_DEUR) {
                    // the door stands two high
                    helper.assertBlockPresent(blok.get(), vloer.above(2));
                    helper.assertTrue(helper.getBlockState(vloer.above(2)).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER, "the door's upper half");
                }
                helper.setBlock(vloer.above(2), Blocks.AIR);
                helper.setBlock(vloer.above(), Blocks.AIR);
            }
            // the wall sign: the same item against the side of a block; the same name
            Block wand = GuhwaiiFeature.PALM_WANDBORD.get();
            helper.assertTrue(wand.asItem() == GuhwaiiFeature.PALM_BORD.get().asItem() && wand.asItem() instanceof SignItem, "one sign item for both");
            helper.assertTrue(wand.getDescriptionId().equals("block.guhs.guhwaii_palm_bord"), "the wall sign is named like the standing one");
            BlockPos muur = new BlockPos(2, 2, 2);
            helper.setBlock(muur, Blocks.STONE);
            klik(helper, p, new ItemStack(wand.asItem()), muur, Direction.NORTH);
            helper.assertBlockPresent(wand, muur.north());
            helper.assertTrue(helper.getBlockState(muur.north()).getValue(WallSignBlock.FACING) == Direction.NORTH, "it hangs on the wall, facing out");
        } finally {
            GuhwaiiGameTests.weg(helper, p);
        }
        helper.succeed();
    }

    /** An axe strips the palm trunk (keeping its axis) and the trunk with a face. */
    @GuhTest(template = EMPTY, batch = "palmhout_strippen")
    public static void palmhoutBijlStriptDeStam(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            BlockPos stam = new BlockPos(2, 2, 2);
            helper.setBlock(stam, GuhwaiiFeature.PALM_STAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            klik(helper, p, new ItemStack(Items.IRON_AXE), stam, Direction.UP);
            helper.assertBlockPresent(GuhwaiiFeature.PALM_GESTRIPT.get(), stam);
            helper.assertTrue(helper.getBlockState(stam).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "the stripped log lies the same way");
            klik(helper, p, new ItemStack(Items.IRON_AXE), stam, Direction.UP);
            helper.assertBlockPresent(GuhwaiiFeature.PALM_GESTRIPT.get(), stam);

            BlockPos gezicht = new BlockPos(4, 2, 2);
            helper.setBlock(gezicht, GuhwaiiFeature.PALM_GEZICHT.get());
            klik(helper, p, new ItemStack(Items.IRON_AXE), gezicht, Direction.UP);
            helper.assertBlockPresent(GuhwaiiFeature.PALM_GESTRIPT.get(), gezicht);
            // (not with an empty hand or another tool)
            helper.setBlock(gezicht, GuhwaiiFeature.PALM_STAM.get());
            klik(helper, p, new ItemStack(Items.IRON_PICKAXE), gezicht, Direction.UP);
            helper.assertBlockPresent(GuhwaiiFeature.PALM_STAM.get(), gezicht);
        } finally {
            GuhwaiiGameTests.weg(helper, p);
        }
        helper.succeed();
    }

    private static void recept(GameTestHelper helper, int breed, int hoog, List<Item> rooster, Item uit, int aantal) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item i : rooster) {
            stacks.add(i == null ? ItemStack.EMPTY : new ItemStack(i));
        }
        CraftingInput input = CraftingInput.of(breed, hoog, stacks);
        ServerLevel level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), "a recipe that makes " + uit);
        ItemStack result = recipe.get().value().assemble(input);
        helper.assertTrue(result.is(uit) && result.getCount() == aantal, "the grid makes " + aantal + " x " + uit + ", got " + result);
    }

    /** The whole set crafts on the workbench, the vanilla shapes and amounts. */
    @GuhTest(template = EMPTY, batch = "palmhout_recepten")
    public static void palmhoutReceptenWerken(GameTestHelper helper) {
        Item w = GuhwaiiFeature.PALM_PLANKEN.get().asItem(), s = Items.STICK;
        Item o = null;
        recept(helper, 1, 1, List.of(GuhwaiiFeature.PALM_GESTRIPT.get().asItem()), w, 4);
        recept(helper, 1, 1, List.of(GuhwaiiFeature.PALM_STAM.get().asItem()), w, 4);
        recept(helper, 1, 1, List.of(GuhwaiiFeature.PALM_GEZICHT.get().asItem()), w, 4);
        recept(helper, 3, 3, java.util.Arrays.asList(w, o, o, w, w, o, w, w, w), GuhwaiiFeature.PALM_TRAP.get().asItem(), 4);
        recept(helper, 3, 1, List.of(w, w, w), GuhwaiiFeature.PALM_PLAAT.get().asItem(), 6);
        recept(helper, 3, 2, List.of(w, s, w, w, s, w), GuhwaiiFeature.PALM_HEK.get().asItem(), 3);
        recept(helper, 3, 2, List.of(s, w, s, s, w, s), GuhwaiiFeature.PALM_POORT.get().asItem(), 1);
        recept(helper, 2, 3, List.of(w, w, w, w, w, w), GuhwaiiFeature.PALM_DEUR.get().asItem(), 3);
        recept(helper, 3, 2, List.of(w, w, w, w, w, w), GuhwaiiFeature.PALM_LUIK.get().asItem(), 2);
        recept(helper, 3, 3, java.util.Arrays.asList(w, w, w, w, w, w, o, s, o), GuhwaiiFeature.PALM_BORD.get().asItem(), 3);
        helper.succeed();
    }

    /** The palm sign is a real sign: the vanilla sign block entity accepts both blocks, and it holds its text. */
    @GuhTest(template = EMPTY, batch = "palmhout_bord")
    public static void palmhoutBordHoudtTekstVast(GameTestHelper helper) {
        helper.assertTrue(WoodType.values().anyMatch(t -> t == GuhwaiiFeature.PALM_WOOD), "the palm's wood type is registered");
        helper.assertTrue(GuhwaiiFeature.PALM_WOOD.name().equals("guhs:guhwaii_palm"), "namespaced: the sign texture is guhs:entity/signs/guhwaii_palm");
        ServerPlayer p = speler(helper);
        try {
            BlockPos vloer = new BlockPos(1, 1, 2);
            helper.setBlock(vloer, Blocks.STONE);
            klik(helper, p, new ItemStack(GuhwaiiFeature.PALM_BORD.get()), vloer, Direction.UP);
            BlockPos muur = new BlockPos(3, 2, 3);
            helper.setBlock(muur, Blocks.STONE);
            klik(helper, p, new ItemStack(GuhwaiiFeature.PALM_BORD.get()), muur, Direction.NORTH);
            for (BlockPos pos : List.of(vloer.above(), muur.north())) {
                BlockState state = helper.getBlockState(pos);
                helper.assertTrue(state.getBlock() instanceof SignBlock sign && sign.type() == GuhwaiiFeature.PALM_WOOD, "a palm sign at " + pos);
                helper.assertTrue(BlockEntityType.SIGN.isValid(state), "the sign block entity type takes " + state);
                helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof SignBlockEntity, "with a sign block entity");
                SignBlockEntity sign = (SignBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                sign.setText(new SignText().setMessage(0, Component.literal("Aloha")).setMessage(1, Component.literal("njeg!")), true);
                // through its saved data and back (what a chunk reload does)
                var nbt = sign.saveWithFullMetadata(helper.getLevel().registryAccess());
                helper.setBlock(pos, Blocks.AIR);
                helper.setBlock(pos, state);
                SignBlockEntity terug = (SignBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                helper.assertTrue(terug != sign && terug.getFrontText().getMessage(0, false).getString().isEmpty(), "a fresh sign is empty");
                terug.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                        net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), nbt));
                helper.assertTrue(terug.getFrontText().getMessage(0, false).getString().equals("Aloha")
                        && terug.getFrontText().getMessage(1, false).getString().equals("njeg!"), "the sign holds its text");
            }
        } finally {
            GuhwaiiGameTests.weg(helper, p);
        }
        helper.succeed();
    }

    /** The tags that make the set behave like wood (tools, fuel, fences that connect, doors villagers open...). */
    @GuhTest(template = EMPTY, batch = "palmhout_tags")
    public static void palmhoutTags(GameTestHelper helper) {
        Block gestript = GuhwaiiFeature.PALM_GESTRIPT.get(), trap = GuhwaiiFeature.PALM_TRAP.get(), plaat = GuhwaiiFeature.PALM_PLAAT.get();
        Block hek = GuhwaiiFeature.PALM_HEK.get(), poort = GuhwaiiFeature.PALM_POORT.get(), deur = GuhwaiiFeature.PALM_DEUR.get();
        Block luik = GuhwaiiFeature.PALM_LUIK.get(), bord = GuhwaiiFeature.PALM_BORD.get(), wandbord = GuhwaiiFeature.PALM_WANDBORD.get();
        Block planken = GuhwaiiFeature.PALM_PLANKEN.get();
        helper.assertTrue(planken.defaultBlockState().is(BlockTags.PLANKS) && new ItemStack(planken).is(ItemTags.PLANKS), "planks");
        helper.assertTrue(gestript.defaultBlockState().is(BlockTags.LOGS_THAT_BURN) && new ItemStack(gestript).is(ItemTags.LOGS_THAT_BURN)
                && gestript.defaultBlockState().is(BlockTags.LOGS), "the stripped log: logs_that_burn");
        helper.assertTrue(trap.defaultBlockState().is(BlockTags.WOODEN_STAIRS) && new ItemStack(trap).is(ItemTags.WOODEN_STAIRS), "wooden_stairs");
        helper.assertTrue(plaat.defaultBlockState().is(BlockTags.WOODEN_SLABS) && new ItemStack(plaat).is(ItemTags.WOODEN_SLABS), "wooden_slabs");
        helper.assertTrue(hek.defaultBlockState().is(BlockTags.WOODEN_FENCES) && new ItemStack(hek).is(ItemTags.WOODEN_FENCES), "wooden_fences");
        helper.assertTrue(poort.defaultBlockState().is(BlockTags.FENCE_GATES) && new ItemStack(poort).is(ItemTags.FENCE_GATES), "fence_gates");
        helper.assertTrue(deur.defaultBlockState().is(BlockTags.WOODEN_DOORS) && new ItemStack(deur).is(ItemTags.WOODEN_DOORS), "wooden_doors");
        helper.assertTrue(luik.defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS) && new ItemStack(luik).is(ItemTags.WOODEN_TRAPDOORS), "wooden_trapdoors");
        helper.assertTrue(bord.defaultBlockState().is(BlockTags.STANDING_SIGNS) && bord.defaultBlockState().is(BlockTags.SIGNS), "standing_signs");
        helper.assertTrue(wandbord.defaultBlockState().is(BlockTags.WALL_SIGNS) && wandbord.defaultBlockState().is(BlockTags.SIGNS), "wall_signs");
        helper.assertTrue(new ItemStack(bord).is(ItemTags.SIGNS), "the sign item: signs");
        for (Block b : List.of(gestript, trap, plaat, hek, poort, deur, luik, bord, wandbord)) {
            helper.assertTrue(b.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), b + ": mineable with an axe");
        }
        // fire: like the planks and the trunk (doors, trapdoors and signs do not catch fire, like vanilla's)
        BlockPos hier = helper.absolutePos(new BlockPos(1, 2, 1));
        for (Block b : List.of(gestript, trap, plaat, hek, poort)) {
            helper.assertTrue(b.defaultBlockState().isFlammable(helper.getLevel(), hier, Direction.UP), b + " burns");
        }
        // as fuel in a furnace (vanilla takes it from the tags)
        var fuel = helper.getLevel().fuelValues();
        for (Block b : List.of(gestript, trap, plaat, hek, poort, deur, luik, bord)) {
            helper.assertTrue(fuel.isFuel(new ItemStack(b)), b + " is furnace fuel");
        }
        helper.succeed();
    }

    /** Drops: everything drops itself, a door once, a double slab two, the wall sign the sign item. */
    @GuhTest(template = EMPTY, batch = "palmhout_buit")
    public static void palmhoutBuit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hier = helper.absolutePos(new BlockPos(1, 2, 1));
        for (DeferredBlock<?> blok : GuhwaiiFeature.PALM_HOUTSET) {
            List<ItemStack> drops = Block.getDrops(blok.get().defaultBlockState(), level, hier, null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(blok.get().asItem()) && drops.get(0).getCount() == 1,
                    blok.getId() + " drops itself once, got " + drops);
        }
        BlockState boven = GuhwaiiFeature.PALM_DEUR.get().defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        helper.assertTrue(Block.getDrops(boven, level, hier, null).isEmpty(), "the door's upper half drops nothing (one door per door)");
        List<ItemStack> dubbel = Block.getDrops(GuhwaiiFeature.PALM_PLAAT.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), level, hier, null);
        helper.assertTrue(dubbel.size() == 1 && dubbel.get(0).getCount() == 2, "a double slab drops two, got " + dubbel);
        List<ItemStack> wand = Block.getDrops(GuhwaiiFeature.PALM_WANDBORD.get().defaultBlockState(), level, hier, null);
        helper.assertTrue(wand.size() == 1 && wand.get(0).is(GuhwaiiFeature.PALM_BORD.get().asItem()), "the wall sign drops the sign, got " + wand);
        helper.succeed();
    }
}
