package nl.juiced.guhs.feature.toveren;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 toveren: the Glinsteraltaar (power only from Knuffelboekenkasten in the ring, its pool, lapis + levels, the
 * Spreukjesboekje), the guh enchantments' rules (no durability on unbreakable gear, Mika-mepper not on the Mika-mepper,
 * the book-only spells never in a table) and their effects (Pluisval, Vadsvlijt, Bakvads, Spaarguhtje, Aaizacht,
 * Kaasknabbelgeur). The room: bank_test_kamer (7 x 5 x 7; its stone floor is relative y 1); the altar stands in its middle.
 */
public class ToverenGameTests {
    private static final String KAMER = "bank_test_kamer";
    private static final String BATCH = "toveren";
    /** (relative y 1 is the room's stone floor: everything stands on it, at y 2) */
    private static final BlockPos ALTAAR = new BlockPos(3, 2, 3);

    private static Holder<Enchantment> h(ServerLevel level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static BlockPos altaar(GameTestHelper helper) {
        helper.setBlock(ALTAAR, ToverenFeature.GLINSTERALTAAR.get());
        return helper.absolutePos(ALTAAR);
    }

    /** Fills the lower ring (16 spots) with this block. */
    private static void ring(GameTestHelper helper, BlockState state, int hoeveel) {
        int n = 0;
        for (BlockPos o : GlinsteraltaarBlock.KAST_PLEKKEN) {
            if (o.getY() == 0 && n < hoeveel) {
                helper.setBlock(ALTAAR.offset(o), state);
                n++;
            }
        }
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void altaarKrachtAlleenKnuffelboekenkasten(GameTestHelper helper) {
        BlockPos abs = altaar(helper);
        ServerLevel level = helper.getLevel();
        ring(helper, Blocks.BOOKSHELF.defaultBlockState(), 16);
        helper.assertTrue(GlinsteraltaarBlock.kracht(level, abs) == 0, "vanilla bookshelves give the altar no power");
        ring(helper, ToverenFeature.KNUFFELBOEKENKAST.get().defaultBlockState(), 6);
        helper.assertTrue(GlinsteraltaarBlock.kracht(level, abs) == 6, "six Knuffelboekenkasten: power 6, got " + GlinsteraltaarBlock.kracht(level, abs)
               );
        // a block between a shelf and the altar takes that shelf away
        BlockPos eerste = GlinsteraltaarBlock.KAST_PLEKKEN.stream().filter(o -> o.getY() == 0).findFirst().orElseThrow();
        helper.setBlock(ALTAAR.offset(eerste.getX() / 2, 0, eerste.getZ() / 2), Blocks.STONE);
        helper.assertTrue(GlinsteraltaarBlock.kracht(level, abs) < 6, "a stone in between blocks a shelf");
        // and the Knuffelboekenkast gives the vanilla table nothing
        helper.assertTrue(ToverenFeature.KNUFFELBOEKENKAST.get().defaultBlockState().getEnchantPowerBonus(level, abs) == 0,
                "a Knuffelboekenkast is no power for the vanilla enchanting table");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void altaarBetovertMetLapisEnLevels(GameTestHelper helper) {
        BlockPos abs = altaar(helper);
        ServerLevel level = helper.getLevel();
        ring(helper, ToverenFeature.KNUFFELBOEKENKAST.get().defaultBlockState(), 16);
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.experienceLevel = 40;
        GlinsteraltaarMenu menu = new GlinsteraltaarMenu(1, player.getInventory(), ContainerLevelAccess.create(level, abs));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 10));
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(menu.costs[2] >= 15 && menu.costs[2] <= 30, "15 shelves: a top offer like vanilla's (15..30), got " + menu.costs[2]);
        net.minecraft.core.IdMap<Holder<Enchantment>> reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        for (int i = 0; i < 3; i++) {
            int clue = menu.enchantClue[i];
            helper.assertTrue(clue >= 0, "offer " + i + " shows a clue");
            Holder<Enchantment> e = reg.byId(clue);
            helper.assertTrue(e != null && e.is(Spreuken.ALTAAR), "offer " + i + " comes from the altar's pool, got " + e);
        }
        helper.assertTrue(menu.clickMenuButton(player, 2), "the third offer can be bought");
        ItemStack uit = menu.getSlot(0).getItem();
        helper.assertTrue(uit.isEnchanted(), "the pickaxe is enchanted");
        for (Holder<Enchantment> e : EnchantmentHelper.getEnchantmentsForCrafting(uit).keySet()) {
            helper.assertTrue(e.is(Spreuken.ALTAAR) && !e.is(Spreuken.BOEKJE), "only pool enchantments, got " + e);
        }
        helper.assertTrue(menu.getSlot(1).getItem().getCount() == 7, "three lapis paid");
        helper.assertTrue(player.experienceLevel == 37, "three levels paid, like vanilla, got " + player.experienceLevel);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void altaarBoekWordtSpreukjesboekje(GameTestHelper helper) {
        BlockPos abs = altaar(helper);
        ServerLevel level = helper.getLevel();
        ring(helper, ToverenFeature.KNUFFELBOEKENKAST.get().defaultBlockState(), 16);
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.experienceLevel = 1000;
        // enchant books until one gets a guh spell (the seed changes after every enchantment)
        for (int poging = 0; poging < 60; poging++) {
            GlinsteraltaarMenu menu = new GlinsteraltaarMenu(1, player.getInventory(), ContainerLevelAccess.create(level, abs));
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
            menu.getSlot(0).set(new ItemStack(Items.BOOK));
            menu.clickMenuButton(player, 2);
            ItemStack boek = menu.getSlot(0).getItem();
            helper.assertTrue(boek.is(Items.ENCHANTED_BOOK), "a book becomes an enchanted book");
            if (Spreukjesboekje.heeftGuhSpreuk(boek)) {
                helper.assertTrue(Spreukjesboekje.isSpreukjesboekje(boek), "a book with a guh spell is dressed as a Spreukjesboekje");
                helper.succeed();
                return;
            }
            helper.assertFalse(Spreukjesboekje.isSpreukjesboekje(boek), "a book with only vanilla enchantments stays a plain enchanted book");
        }
        helper.fail("60 books and never a guh spell");
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geenDuurzaamheidOpOnbreekbaar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> unbreaking = h(level, Enchantments.UNBREAKING), mending = h(level, Enchantments.MENDING);
        for (ItemStack vads : List.of(new ItemStack(ModItems.VADS_PICKAXE.get()), new ItemStack(ModItems.VADS_CHESTPLATE.get()),
                new ItemStack(ModItems.VADS_SWORD.get()), new ItemStack(ModItems.MIKA_MEPPER.get()))) {
            helper.assertFalse(vads.supportsEnchantment(unbreaking) || vads.supportsEnchantment(mending), "no durability enchantment fits " + vads);
            helper.assertFalse(GlinsteraltaarMenu.pool(level.registryAccess(), vads).anyMatch(e -> e.is(Spreuken.DUURZAAMHEID)),
                    "the altar never offers durability for " + vads);
        }
        ItemStack diamant = new ItemStack(Items.DIAMOND_PICKAXE);
        helper.assertTrue(diamant.supportsEnchantment(unbreaking) && diamant.supportsEnchantment(mending), "a diamond pickaxe still takes both");
        helper.assertTrue(GlinsteraltaarMenu.pool(level.registryAccess(), diamant).anyMatch(e -> e.is(Enchantments.MENDING)),
                "mending is in the altar's pool (rare, as in vanilla's weight)");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void spreukenPoolEnRegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> mepper = h(level, Spreuken.MIKA_MEPPER);
        helper.assertFalse(new ItemStack(ModItems.MIKA_MEPPER.get()).supportsEnchantment(mepper), "Mika-mepper does not go on the Mika-mepper");
        helper.assertTrue(new ItemStack(Items.DIAMOND_SWORD).supportsEnchantment(mepper), "Mika-mepper goes on a sword");
        for (ResourceKey<Enchantment> boek : List.of(Spreuken.SPAARGUHTJE, Spreuken.AAIZACHT, Spreuken.KAASKNABBELGEUR)) {
            Holder<Enchantment> e = h(level, boek);
            helper.assertTrue(e.is(Spreuken.BOEKJE) && e.is(Spreuken.ALLE), boek + " is a book-only spell");
            helper.assertFalse(e.is(Spreuken.ALTAAR) || e.is(EnchantmentTags.IN_ENCHANTING_TABLE), boek + " never comes from a table");
        }
        for (ResourceKey<Enchantment> tafel : List.of(Spreuken.MIKA_MEPPER, Spreuken.PLUISVAL, Spreuken.GLITTERZICHT, Spreuken.VADSVLIJT, Spreuken.BAKVADS)) {
            Holder<Enchantment> e = h(level, tafel);
            helper.assertTrue(e.is(Spreuken.ALTAAR) && !e.is(EnchantmentTags.IN_ENCHANTING_TABLE), tafel + ": the altar, not the vanilla table");
        }
        helper.assertFalse(Enchantment.areCompatible(h(level, Spreuken.BAKVADS), h(level, Enchantments.SILK_TOUCH)), "Bakvads and silk touch clash");
        ItemStack kist = new ItemStack(Items.DIAMOND_CHESTPLATE);
        helper.assertTrue(kist.supportsEnchantment(h(level, Spreuken.KAASKNABBELGEUR)), "Kaasknabbelgeur goes on any armour piece");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void spreukjesboekjeOpAambeeld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.experienceLevel = 30;
        helper.setBlock(ALTAAR, Blocks.ANVIL);
        ItemStack boek = Spreukjesboekje.maak(h(level, Spreuken.VADSVLIJT), 3);
        helper.assertTrue(Spreukjesboekje.isSpreukjesboekje(boek) && boek.is(Items.ENCHANTED_BOOK), "a Spreukjesboekje is a dressed enchanted book");
        AnvilMenu menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(level, helper.absolutePos(ALTAAR)));
        menu.getSlot(0).set(new ItemStack(Items.IRON_PICKAXE));
        menu.getSlot(1).set(boek);
        menu.createResult();
        ItemStack uit = menu.getSlot(2).getItem();
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(h(level, Spreuken.VADSVLIJT), uit) == 3,
                "the anvil puts Vadsvlijt III from the Spreukjesboekje on the pickaxe, got " + uit);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void pluisvalLandtZacht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.PINK_WOOL);
        ServerPlayer player = GuhMockPlayer.of(helper);
        BlockPos op = helper.absolutePos(new BlockPos(2, 2, 2));
        player.snapTo(op.getX() + 0.5, op.getY(), op.getZ() + 0.5);
        ItemStack laarzen = new ItemStack(Items.IRON_BOOTS);
        player.setItemSlot(EquipmentSlot.FEET, laarzen);
        LivingFallEvent zonder = NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 8, 1f));
        helper.assertTrue(zonder.getDamageMultiplier() == 1f, "without Pluisval a fall hurts");
        laarzen.enchant(h(level, Spreuken.PLUISVAL), 1);
        LivingFallEvent wol = NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 8, 1f));
        helper.assertTrue(wol.getDamageMultiplier() == 0f, "with Pluisval on wool: no fall damage");
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        LivingFallEvent steen = NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 8, 1f));
        helper.assertTrue(steen.getDamageMultiplier() == 1f, "on stone Pluisval does nothing");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void vadsvlijtHaktSnellerInWol(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GuhMockPlayer.of(helper);
        ItemStack pikhouweel = new ItemStack(Items.IRON_PICKAXE);
        pikhouweel.enchant(h(level, Spreuken.VADSVLIJT), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, pikhouweel);
        PlayerEvent.BreakSpeed wol = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(player, Blocks.PINK_WOOL.defaultBlockState(), 1f, null));
        helper.assertTrue(wol.getNewSpeed() == 1f + Spreuken.VLIJT_PER_NIVEAU * 3, "Vadsvlijt III: wool much faster, got " + wol.getNewSpeed());
        PlayerEvent.BreakSpeed vilt = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(player,
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(nl.juiced.guhs.Guhs.id("viltsteen")).defaultBlockState(), 1f, null));
        helper.assertTrue(vilt.getNewSpeed() > 1f, "and viltsteen");
        PlayerEvent.BreakSpeed steen = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(player, Blocks.STONE.defaultBlockState(), 1f, null));
        helper.assertTrue(steen.getNewSpeed() == 1f, "stone is not faster");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bakvadsBakt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Spreuken.Gebakken ijzer = Spreuken.bak(level, new ItemStack(Items.RAW_IRON, 3));
        helper.assertTrue(ijzer != null && ijzer.stack().is(Items.IRON_INGOT) && ijzer.stack().getCount() == 3 && ijzer.xp() > 0,
                "three raw iron (fortune) become three ingots, with smelting xp");
        Spreuken.Gebakken zand = Spreuken.bak(level, new ItemStack(Items.SAND));
        helper.assertTrue(zand != null && zand.stack().is(Items.GLASS), "sand becomes glass");
        helper.assertTrue(Spreuken.bak(level, new ItemStack(Items.DIRT)) == null, "dirt does not smelt: stays dirt");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void spaarguhtjeNaarDeBank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 2, 1), ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
        ServerPlayer player = GuhMockPlayer.of(helper);
        ItemStack pikhouweel = new ItemStack(Items.IRON_PICKAXE);
        pikhouweel.enchant(h(level, Spreuken.SPAARGUHTJE), 1);
        pikhouweel.enchant(h(level, Spreuken.BAKVADS), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, pikhouweel);
        BlockPos erts = helper.absolutePos(new BlockPos(4, 2, 4));
        // no key: the drops stay drops (but baked)
        List<ItemEntity> drops = new ArrayList<>(List.of(new ItemEntity(level, erts.getX(), erts.getY(), erts.getZ(), new ItemStack(Items.RAW_GOLD, 2))));
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(level, erts, Blocks.GOLD_ORE.defaultBlockState(), null, drops, player, pikhouweel));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(Items.GOLD_INGOT) && drops.get(0).getItem().getCount() == 2,
                "without a Banksleutel the (baked) drops stay");
        // with a key bound to the bank: straight into the bank
        ItemStack sleutel = new ItemStack(BankFeature.BANK_SLEUTEL.get());
        BankSleutelItem.onthoud(sleutel, bank);
        player.getInventory().add(sleutel);
        drops = new ArrayList<>(List.of(new ItemEntity(level, erts.getX(), erts.getY(), erts.getZ(), new ItemStack(Items.RAW_GOLD, 2))));
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(level, erts, Blocks.GOLD_ORE.defaultBlockState(), null, drops, player, pikhouweel));
        helper.assertTrue(drops.isEmpty(), "with the key the drops are gone from the ground");
        helper.assertTrue(bank.getStorage().count(new ItemStack(Items.GOLD_INGOT)) == 2, "two gold ingots in the bank");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void aaizachtSpaartVillagersEnHuisdieren(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack zwaard = new ItemStack(Items.IRON_SWORD);
        zwaard.enchant(h(level, Spreuken.AAIZACHT), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, zwaard);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 4));
        guh.tame(player);
        float v = villager.getHealth(), z = zombie.getHealth(), g = guh.getHealth();
        villager.hurtServer(level, level.damageSources().playerAttack(player), 4f);
        guh.hurtServer(level, level.damageSources().playerAttack(player), 4f);
        zombie.hurtServer(level, level.damageSources().playerAttack(player), 4f);
        helper.assertTrue(villager.getHealth() == v, "the villager gets a pat, not a hit");
        helper.assertTrue(guh.getHealth() == g, "my own tamed guh is not hurt");
        helper.assertTrue(zombie.getHealth() < z, "a zombie is hit as usual");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void kaasknabbelgeurLoktWildeGuhs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 5));
        KaasknabbelGeurGoal goal = new KaasknabbelGeurGoal(wild);
        helper.assertFalse(goal.canUse(), "no scent, no interest");
        ItemStack kist = new ItemStack(Items.LEATHER_CHESTPLATE);
        kist.enchant(h(level, Spreuken.KAASKNABBELGEUR), 1);
        player.setItemSlot(EquipmentSlot.CHEST, kist);
        helper.assertTrue(goal.canUse(), "a wild guh smells the kaasknabbels and comes");
        helper.assertTrue(wild.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof KaasknabbelGeurGoal),
                "every guh gets the goal when it joins the level");
        wild.tame(player);
        helper.assertFalse(goal.canUse(), "a tamed guh just follows its owner the normal way");
        helper.succeed();
    }
}
