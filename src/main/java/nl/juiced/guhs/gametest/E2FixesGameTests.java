package nl.juiced.guhs.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.bibliotheek.BibliotheekProtection;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.GuhbrouwketelBlockEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.registry.ModItems;

/** 1.5.0 fixes: two Vads pieces in an anvil, and the pepper brew (kaassaus into a Guhbrouwketel) inside a guh building. */
public class E2FixesGameTests {
    private static final String EMPTY = "empty";

    /** Two Vads helmets with Protection III give one with Protection IV (and it stays unbreakable). */
    @GuhTest(template = EMPTY)
    public static void tweeVadsHelmenWordenSamengevoegd(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        Holder<Enchantment> protection = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        ItemStack a = new ItemStack(ModItems.VADS_HELMET.get()), b = new ItemStack(ModItems.VADS_HELMET.get());
        a.enchant(protection, 3);
        b.enchant(protection, 3);
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(a);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(b);
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.is(ModItems.VADS_HELMET.get()), "a Vads helmet comes out, not nothing");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(protection, result) == 4, "Protection III + III = IV");
        helper.assertTrue(result.has(DataComponents.UNBREAKABLE), "still unbreakable");
        helper.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST, 0) == 1, "the vanilla repair cost penalty");
        helper.assertTrue(menu.getCost() > 0 && menu.getCost() < 40, "an XP cost: " + menu.getCost());
        // the name field keeps working
        menu.setItemName("Vadspet");
        helper.assertTrue("Vadspet".equals(menu.getSlot(AnvilMenu.RESULT_SLOT).getItem().getHoverName().getString()), "renamed too");
        menu.setItemName("");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(protection, menu.getSlot(AnvilMenu.RESULT_SLOT).getItem()) == 4, "still IV without a name");
        // taking it uses up both helmets
        player.giveExperienceLevels(30);
        ItemStack taken = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem().copy();
        menu.getSlot(AnvilMenu.RESULT_SLOT).onTake(player, taken);
        helper.assertTrue(menu.getSlot(AnvilMenu.INPUT_SLOT).getItem().isEmpty() && menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().isEmpty(),
                "both helmets are used up");
        // the same helmet twice at the max level: nothing would change, so nothing comes out
        ItemStack c = new ItemStack(ModItems.VADS_HELMET.get()), d = new ItemStack(ModItems.VADS_HELMET.get());
        c.enchant(protection, 4);
        d.enchant(protection, 4);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(c);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(d);
        helper.assertTrue(menu.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(), "IV + IV changes nothing: no result");
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /**
     * The pepper brew in a protected kas: a survival player pours the kaassaus into the Guhbrouwketel (it used to say "Geen
     * vuur of emmers naast een guhgebouw"), lights it with grillspiespoeder and stirs an ingredient in.
     */
    @GuhTest(template = EMPTY)
    public static void pepersoepInEenBeschermdeKas(GameTestHelper helper) {
        BoundingBox kas = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(4, 3, 4)));
        BibliotheekProtection.TEST_AREAS.add(kas);
        try {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                }
            }
            BlockPos ketelPos = new BlockPos(2, 1, 2);
            helper.setBlock(ketelPos, SpiesburchtFeature.GUHBROUWKETEL.get());
            BlockPos abs = helper.absolutePos(ketelPos);
            helper.assertTrue(Protected.at(helper.getLevel(), abs), "the ketel stands in a protected building");
            ServerPlayer player = GuhMockPlayer.of(helper);
            player.setGameMode(GameType.SURVIVAL);
            player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5);
            GuhbrouwketelBlockEntity ketel = (GuhbrouwketelBlockEntity) helper.getLevel().getBlockEntity(abs);

            ItemStack hand = klik(helper, player, abs, new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()));
            helper.assertTrue(ketel.portions() > 0 && ketel.contents() == Brouwsel.BOUILLON, "the kaassaus goes into the ketel");
            helper.assertTrue(hand.is(Items.BUCKET), "and the bucket is empty");
            klik(helper, player, abs, new ItemStack(SpiesburchtFeature.GRILLSPIESPOEDER.get()));
            helper.assertTrue(ketel.fuel() > 0, "a fire under it");
            Item ingredient = null;
            for (Brouwsel b : Brouwsel.values()) {
                if (ingredient == null && b.ingredient() != null) {
                    for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(b.ingredient())) {
                        ingredient = h.value();
                        break;
                    }
                }
            }
            helper.assertTrue(ingredient != null, "an ingredient");
            klik(helper, player, abs, new ItemStack(ingredient));
            helper.assertTrue(ketel.isBrewing(), "and it bubbles");
            // a bucket poured out next to the building is still refused
            ItemStack lava = klik(helper, player, helper.absolutePos(new BlockPos(0, 0, 2)), new ItemStack(Items.LAVA_BUCKET));
            helper.assertTrue(lava.is(Items.LAVA_BUCKET), "no lava next to it");
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        } finally {
            BibliotheekProtection.TEST_AREAS.remove(kas);
        }
        helper.succeed();
    }

    private static ItemStack klik(GameTestHelper helper, ServerPlayer p, BlockPos abs, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.gameMode.useItemOn(p, helper.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
        return p.getMainHandItem();
    }
}
