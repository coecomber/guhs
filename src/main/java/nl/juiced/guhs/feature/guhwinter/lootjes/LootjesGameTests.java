package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.List;
import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;

/** Guhwinter lootjes: the draw is a derangement, the builder's stash goes there and back exactly, the deny-list, the gift. */
public class LootjesGameTests {
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY)
    public static void lootjesKern(GameTestHelper helper) {
        // --- 1. the draw: everybody gives once, receives once, never to themselves -------------------------------------------
        Random r = new Random(1109);
        for (int n = 2; n <= Lootjes.MAX; n++) {
            for (int k = 0; k < 400; k++) {
                int[] p = Trekking.trek(n, r);
                helper.assertTrue(p.length == n && Trekking.geldig(p), "derangement of " + n + ": " + java.util.Arrays.toString(p));
            }
        }
        helper.assertTrue(Trekking.trek(1, r).length == 0 && Trekking.trek(0, r).length == 0, "no draw below two");
        helper.assertTrue(!Trekking.geldig(new int[]{1, 0, 2}) && !Trekking.geldig(new int[]{1, 1, 0}), "the check itself");

        // --- 2. the builder's stash: in, creative junk, back out --------------------------------------------------------------
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        p.getInventory().setItem(20, new ItemStack(Items.COBBLESTONE, 64));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 3));
        p.getInventory().setSelectedSlot(4);
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 1));
        p.setHealth(13f);
        p.getFoodData().setFoodLevel(11);
        p.setExperienceLevels(7);
        Bouwkist.begin(p, 2);
        helper.assertTrue(Bouwkist.bouwt(p) && Bouwkist.veld(p) == 2, "building on plot 3");
        helper.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.CREATIVE, "creative while building");
        helper.assertTrue(p.getInventory().isEmpty() && p.getActiveEffects().isEmpty(), "creative starts empty");
        // creative junk that must NOT come out
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_BLOCK, 64));
        p.getInventory().setItem(30, new ItemStack(Items.BEACON));
        p.setHealth(20f);
        helper.assertTrue(Bouwkist.herstel(p), "back out");
        helper.assertTrue(!Bouwkist.bouwt(p) && !p.getPersistentData().contains(Bouwkist.SLEUTEL), "the stash is gone");
        helper.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "survival again");
        helper.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND) && p.getInventory().getItem(0).getCount() == 5, "slot 0 back");
        helper.assertTrue(p.getInventory().getItem(20).is(Items.COBBLESTONE) && p.getInventory().getItem(20).getCount() == 64, "slot 20 back");
        helper.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "helmet back");
        helper.assertTrue(p.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TORCH) && p.getItemBySlot(EquipmentSlot.OFFHAND).getCount() == 3, "offhand back");
        helper.assertTrue(p.getInventory().getSelectedSlot() == 4, "selected slot back");
        int stuks = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            stuks += p.getInventory().getItem(i).isEmpty() ? 0 : 1;
        }
        helper.assertTrue(stuks == 4, "nothing from creative came along (" + stuks + " stacks)");
        helper.assertTrue(p.hasEffect(MobEffects.SPEED) && p.getEffect(MobEffects.SPEED).getAmplifier() == 1, "effect back");
        helper.assertTrue(Math.abs(p.getHealth() - 13f) < 0.01f, "health back: " + p.getHealth());
        helper.assertTrue(p.getFoodData().getFoodLevel() == 11, "hunger back");
        helper.assertTrue(p.experienceLevel == 7, "xp back");
        helper.assertTrue(!Bouwkist.herstel(p), "nothing to restore twice");
        // the raw snapshot round trip too
        CompoundTag k = Bouwkist.pak(p);
        Bouwkist.leeg(p);
        Bouwkist.zetTerug(p, k);
        helper.assertTrue(p.getInventory().getItem(0).getCount() == 5 && p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "snapshot round trip");

        // --- 3. the deny-list ---------------------------------------------------------------------------------------------------
        List<Block> mag = List.of(Blocks.DIAMOND_BLOCK, Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.OAK_HANGING_SIGN, Blocks.LANTERN,
                Blocks.RED_BANNER, Blocks.PLAYER_HEAD, Blocks.POPPY, Blocks.CANDLE, Blocks.WHITE_CARPET, Blocks.OAK_STAIRS, Blocks.OAK_SLAB,
                Blocks.COBBLESTONE_WALL, Blocks.OAK_DOOR, Blocks.RED_BED, Blocks.BELL, Blocks.GLASS, Blocks.OAK_LEAVES, Blocks.OAK_FENCE,
                Blocks.FLOWER_POT, Blocks.CRAFTING_TABLE, Blocks.SNOW_BLOCK, Blocks.GLOWSTONE);
        List<Block> nee = List.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER,
                Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.REDSTONE_WIRE, Blocks.REPEATER, Blocks.COMPARATOR, Blocks.LEVER,
                Blocks.STONE_BUTTON, Blocks.OAK_PRESSURE_PLATE, Blocks.TNT, Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE,
                Blocks.BREWING_STAND, Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW,
                Blocks.BARRIER, Blocks.LIGHT, Blocks.SHULKER_BOX, Blocks.ENDER_CHEST, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_BLOCK,
                Blocks.DAYLIGHT_DETECTOR, Blocks.CRAFTER, Blocks.WATER, Blocks.LAVA, Blocks.LECTERN, Blocks.JUKEBOX, Blocks.BEACON,
                Blocks.CAMPFIRE, Blocks.DECORATED_POT, Blocks.BEEHIVE, Blocks.TARGET, Blocks.TRIPWIRE_HOOK, Blocks.POWERED_RAIL,
                ModBlocks.GUH_SPAWNER.get(), LootjesFeature.SURPRISE.get(), LootjesFeature.SURPRISE_DEEL.get());
        for (Block b : mag) {
            helper.assertTrue(BouwRegels.magBlok(b.defaultBlockState()), "allowed: " + b);
        }
        for (Block b : nee) {
            helper.assertTrue(!BouwRegels.magBlok(b.defaultBlockState()), "forbidden: " + b);
        }
        BlockState nat = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        helper.assertTrue(!BouwRegels.schoon(nat).getValue(BlockStateProperties.WATERLOGGED), "the water goes out of a gift");
        helper.assertTrue(BouwRegels.schoon(Blocks.HOPPER.defaultBlockState()).isAir(), "a machine becomes air");

        // --- 4. the gift: turning four times is the same, saving is exact ---------------------------------------------------
        Inhoud in = new Inhoud();
        in.zet(Inhoud.index(0, 0, 0), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), null);
        in.zet(Inhoud.index(6, 3, 2), Blocks.LANTERN.defaultBlockState(), null);
        CompoundTag bord = new CompoundTag();
        bord.putString("id", "minecraft:sign");
        in.zet(Inhoud.index(1, 1, 5), Blocks.OAK_SIGN.defaultBlockState(), bord);
        helper.assertTrue(in.aantal() == 3 && in.eerste() == 0, "three blocks");
        Inhoud rond = in.gedraaid(Rotation.CLOCKWISE_90).gedraaid(Rotation.CLOCKWISE_90).gedraaid(Rotation.CLOCKWISE_90).gedraaid(Rotation.CLOCKWISE_90);
        helper.assertTrue(rond.zelfde(in), "four quarter turns = the same gift");
        Inhoud kwart = in.gedraaid(Rotation.CLOCKWISE_90);
        helper.assertTrue(kwart.get(6, 0, 0).getValue(StairBlock.FACING) == Direction.EAST, "a turned stair faces along");
        helper.assertTrue(kwart.be(Inhoud.index(1, 1, 1)) != null, "the sign's data moves with its block");
        helper.assertTrue(Inhoud.laad(in.opslaan()).zelfde(in) && Inhoud.aantal(in.opslaan()) == 3, "save/load");
        SurpriseData d = SurpriseData.van(in, "Annie", "Bert");
        helper.assertTrue(!d.leeg() && d.aantal() == 3 && d.lees().zelfde(in), "the item component");
        helper.assertTrue(SurpriseData.van(new Inhoud(), "Annie", "Bert").leeg(), "an empty plot gives an empty gift");
        helper.succeed();
    }
}
