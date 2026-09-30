package nl.juiced.guhs.feature.kleding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.boerderij.BoerderijVoortgang;
import nl.juiced.guhs.feature.boerderij.Hooibaal;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.menu.GuhWardrobeMenu;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModVillagers;

/** Clothing as a one-time unlock (2.9): using it up, dressing, the wardrobe rules, favourites, sources and the source moves. */
public class KledingGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        KledingUnlocks.wis(player);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean klaar(ServerPlayer player, String advancement) {
        var holder = player.level().getServer().getAdvancements().get(Guhs.id(advancement));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static List<GuhClothes> outfit(GuhClothes... pieces) {
        List<GuhClothes> out = new ArrayList<>(Collections.nCopies(GuhClothes.Slot.kleding().size(), null));
        for (GuhClothes c : pieces) {
            out.set(GuhWardrobeMenu.index(c.slot), c);
        }
        return out;
    }

    /** Holding right-click uses the item up once (+1 unlock), a second one is refused ("Deze heb je al") and stays whole. */
    @GuhTest(template = EMPTY)
    public static void kledingOntgrendelVerbruiktEenStuk(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack hoed = new ItemStack(ModItems.RAIN_HAT.get(), 1);
        GuhClothingItem item = (GuhClothingItem) hoed.getItem();
        helper.assertTrue(item.getUseDuration(hoed, player) == GuhClothingItem.ONTGRENDEL_TICKS && GuhClothingItem.ONTGRENDEL_TICKS == 30,
                "about 1.5 seconds of holding");
        helper.assertFalse(KledingUnlocks.heeft(player, GuhClothes.RAIN_HAT), "not unlocked yet");
        player.setItemInHand(InteractionHand.MAIN_HAND, hoed);
        helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction() && player.isUsingItem(),
                "holding right-click starts using it");
        player.stopUsingItem();
        ItemStack rest = item.finishUsingItem(hoed, helper.getLevel(), player);
        helper.assertTrue(KledingUnlocks.heeft(player, GuhClothes.RAIN_HAT) && KledingUnlocks.alle(player).size() == 1, "unlocked: exactly +1");
        helper.assertTrue(rest.isEmpty(), "the item is used up");
        helper.assertTrue(klaar(player, "grote_guhspelen/kleding_eerste") && klaar(player, "quest/kleding_eerste"), "the first-unlock advancements");
        ItemStack tweede = new ItemStack(ModItems.RAIN_HAT.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tweede);
        helper.assertFalse(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "the second one can't be used up");
        item.finishUsingItem(tweede, helper.getLevel(), player);
        helper.assertTrue(tweede.getCount() == 1 && KledingUnlocks.alle(player).size() == 1, "still whole (give it to a friend), still one unlock");
        // the unlocks survive in the saved data, and hair is never an unlock
        helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.saved(player).getListOrEmpty(KledingUnlocks.KEY).size() == 1, "saved per player");
        GuhClothingItem kapsel = (GuhClothingItem) ModItems.clothingItem(GuhClothes.KAPSEL_KRULLEN);
        helper.assertTrue(!kapsel.isOntgrendelbaar() && !KledingOntgrendel.ontgrendel(player, GuhClothes.KAPSEL_KRULLEN), "hair stays with the kapper");
        leave(helper, player);
        helper.succeed();
    }

    /** Only the owner dresses a guh, only with their own unlocks; a guh keeps what it wears when it changes owner. */
    @GuhTest(template = EMPTY)
    public static void kledingAlleenEigenaarMetEigenUnlocks(GameTestHelper helper) {
        ServerPlayer baas = player(helper), ander = player(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        guh.tame(baas);
        helper.assertTrue(KledingKast.mag(guh, ander, GuhClothes.Slot.HEAD, null) == KledingKast.Uitkomst.NIET_JOUW_GUH, "not someone else's guh");
        helper.assertTrue(KledingKast.mag(guh, baas, GuhClothes.Slot.HEAD, GuhClothes.PARTY_HAT) == KledingKast.Uitkomst.NIET_ONTGRENDELD,
                "not without the unlock");
        helper.assertTrue(KledingKast.mag(guh, baas, GuhClothes.Slot.EYES, GuhClothes.PARTY_HAT) == KledingKast.Uitkomst.VERKEERD_VAKJE, "wrong slot");
        KledingUnlocks.ontgrendel(baas, GuhClothes.PARTY_HAT);
        KledingUnlocks.ontgrendel(baas, GuhClothes.OORSTRIKJE_ROZE);
        KledingUnlocks.ontgrendel(ander, GuhClothes.SUNGLASSES);
        helper.assertTrue(KledingKast.kleed(baas, guh, outfit(GuhClothes.PARTY_HAT, GuhClothes.OORSTRIKJE_ROZE)) == 2
                && guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.PARTY_HAT && guh.getClothes(GuhClothes.Slot.OREN) == GuhClothes.OORSTRIKJE_ROZE,
                "the owner dresses it from their unlocks (ear bows on the OREN slot)");
        KledingKast.kleed(ander, guh, outfit(GuhClothes.SUNGLASSES));
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.EYES) == null && guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.PARTY_HAT,
                "someone else can't dress it");
        guh.setOwnerUUID(ander.getUUID());                                  // given away
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.PARTY_HAT, "it keeps what it wears");
        KledingKast.kleed(ander, guh, outfit(GuhClothes.PARTY_HAT, GuhClothes.SUNGLASSES));
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.EYES) == GuhClothes.SUNGLASSES && guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.PARTY_HAT
                        && guh.getClothes(GuhClothes.Slot.OREN) == null,
                "the new owner dresses it from their own unlocks (keeping what it wore is fine, taking off too)");
        KledingKast.kleed(ander, guh, outfit(GuhClothes.PARTY_HAT, GuhClothes.SUNGLASSES, GuhClothes.OORSTRIKJE_ROZE));
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.OREN) == null, "but not with the old owner's unlocks");
        CompoundTag tag = new CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(guh, tag);
        helper.assertTrue(tag.getStringOr("ClothesEyes", "").equals("sunglasses"), "saved");
        leave(helper, baas, ander);
        helper.succeed();
    }

    /** Wild guhs wear clothes for looks but drop none; what's in a backpack does fall out. */
    @GuhTest(template = EMPTY, timeoutTicks = 60)
    public static void kledingWildeGuhsLatenNietsVallen(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        guh.wear(GuhClothes.CHEF_HAT);
        guh.wear(GuhClothes.CHEF_JACKET);
        guh.wear(GuhClothes.GUH_BACKPACK);
        guh.getBackpack().setItem(2, new ItemStack(Items.DIAMOND, 4));
        guh.kill();
        helper.succeedWhen(() -> {
            List<ItemEntity> items = helper.getEntities(EntityType.ITEM);
            helper.assertTrue(items.stream().anyMatch(i -> i.getItem().is(Items.DIAMOND) && i.getItem().getCount() == 4), "the backpack's diamonds fall out");
            helper.assertTrue(items.stream().noneMatch(i -> i.getItem().getItem() instanceof GuhClothingItem), "no clothes drop: " + items);
        });
    }

    /** The kleermaker's fixed full offer: his everyday set and the three ear bows, nothing that moved elsewhere. */
    @GuhTest(template = EMPTY)
    public static void kledingKleermakerVastAanbod(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        villager.setVillagerData(villager.getVillagerData().setType(ModVillagers.GUH.get()).setProfession(ModVillagers.GUH_KLEERMAKER.get()));
        helper.assertTrue(KledingKleermaker.zorgVoorAanbod(villager), "the offer is put in");
        List<GuhClothes> sold = villager.getOffers().stream().map(MerchantOffer::getResult)
                .filter(s -> s.getItem() instanceof GuhClothingItem).map(s -> ((GuhClothingItem) s.getItem()).getClothes()).toList();
        List<GuhClothes> expected = List.of(GuhClothes.RED_BOWTIE, GuhClothes.BLACK_BOWTIE, GuhClothes.SUNGLASSES, GuhClothes.RAIN_HAT,
                GuhClothes.STRIPED_SWEATER, GuhClothes.RAINCOAT, GuhClothes.OORSTRIKJE_ROZE, GuhClothes.OORSTRIKJE_MINT, GuhClothes.OORSTRIKJE_GEEL);
        helper.assertTrue(sold.equals(expected), "exactly his own set, all of it: " + sold);
        helper.assertTrue(villager.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.ROZE_LINT.get())), "still the pink ribbon (the sled)");
        helper.assertFalse(KledingKleermaker.zorgVoorAanbod(villager), "only once");
        for (GuhClothes c : expected) {
            helper.assertTrue("kleermaker".equals(KledingBronnen.bron(c)), c + " comes from the kleermaker");
        }
        helper.succeed();
    }

    /** Every piece (not the hair) has exactly one registered source, and the moves of DESIGN_29 §10.1. */
    @GuhTest(template = EMPTY)
    public static void kledingElkStukEenBron(GameTestHelper helper) {
        List<GuhClothes> zonder = new ArrayList<>();
        for (GuhClothes c : GuhClothes.values()) {
            if (c.slot != GuhClothes.Slot.HAAR && KledingBronnen.bron(c) == null) {
                zonder.add(c);
            }
        }
        helper.assertTrue(zonder.isEmpty(), "pieces without a source: " + zonder);
        helper.assertTrue(KledingBronnen.bron(GuhClothes.KAPSEL_KRULLEN) == null, "hair isn't an unlock");
        String[][] moves = {{"chef_hat", "bakkerij"}, {"chef_jacket", "bakkerij"}, {"straw_hat", "boerderij"}, {"overalls", "boerderij"},
                {"firefighter_helmet", "beroep_brandweer"}, {"police_uniform", "beroep_politie"}, {"stethoscope", "beroep_apotheek"},
                {"safety_vest", "beroep_bouw"}, {"party_hat", "loot_picknick"}, {"heart_glasses", "guhdex"}, {"monocle", "guhdex"},
                {"royal_crown", "guhdex"}, {"pink_onesie", "brococolief"}, {"guh_backpack", "crafting"}, {"koning_kroon", "loot_kasteel"},
                {"wolkenmuts", "loot_eilanden"}, {"knight_armour", "loot_grotten"}, {"grill_koksmuts", "barbecuether"},
                {"boswachtershoed", "vadswoud"}, {"plukmuts", "vadswoud"}};
        for (String[] m : moves) {
            helper.assertTrue(m[1].equals(KledingBronnen.bron(GuhClothes.byId(m[0]))), m[0] + " should come from " + m[1]);
        }
        // the Guhdex milestones keep their clothes (they are the source)
        helper.assertTrue(GuhDex.MILESTONES.get(0).reward().get() == ModItems.clothingItem(GuhClothes.HEART_GLASSES)
                && GuhDex.MILESTONES.get(3).reward().get() == ModItems.clothingItem(GuhClothes.ROYAL_CROWN), "the Guhdex milestones");
        helper.assertTrue(KledingBronnen.aantalOntgrendelbaar() == GuhClothes.values().length - 8, "everything but the 8 kapsels");
        helper.succeed();
    }

    /** Bakker Korstje sells the chef set; Boerin Hooibaal gives the straw hat (first chore) and the overalls (third chore). */
    @GuhTest(template = EMPTY)
    public static void kledingBakkerEnBoerin(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        GuhNpcEntity bakker = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 1, 2));
        bakker.setKind(GuhNpcEntity.Kind.BAKKERGUH);
        var offers = Features.role(GuhNpcEntity.Kind.BAKKERGUH).offers(bakker);
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.CHEF_HAT.get()))
                && offers.stream().anyMatch(o -> o.getResult().is(ModItems.CHEF_JACKET.get())), "the chef set at Bakker Korstje");
        for (int klusje = 1; klusje <= 3; klusje++) {
            Hooibaal.zetKlus(player, Hooibaal.Klus.AAIEN);
            BoerderijVoortgang.data(player).putInt("Stand", Hooibaal.Klus.AAIEN.doel);
            helper.assertTrue(Hooibaal.rondAf(player), "chore " + klusje + " done");
            boolean hoed = player.getInventory().contains(new ItemStack(ModItems.clothingItem(GuhClothes.STRAW_HAT)));
            boolean broek = player.getInventory().contains(new ItemStack(ModItems.clothingItem(GuhClothes.OVERALLS)));
            helper.assertTrue(hoed, "the straw hat after the first chore");
            helper.assertTrue(broek == (klusje >= KledingFeature.OVERALL_KLUSJES), "the overalls after the third chore (" + klusje + ")");
        }
        int hoeden = player.getInventory().countItem(ModItems.clothingItem(GuhClothes.STRAW_HAT));
        helper.assertTrue(hoeden == 1, "only one straw hat: " + hoeden);
        leave(helper, player);
        helper.succeed();
    }

    /** Taming a Brococolief guh gives its pink onesie, once per player. */
    @GuhTest(template = EMPTY)
    public static void kledingBrococoliefOnesieEenKeer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        nl.juiced.guhs.quest.GuhQuests.saved(player).remove(KledingFeature.ONESIE_KEY);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        guh.setVariant(nl.juiced.guhs.entity.GuhVariant.BROCOCOLIEF);
        guh.wear(GuhClothes.PINK_ONESIE);
        net.neoforged.neoforge.event.EventHooks.onAnimalTame(guh, player);         // (the tame event: the onesie comes along)
        helper.assertTrue(player.getInventory().countItem(ModItems.PINK_ONESIE.get()) == 1, "the onesie for the tamer");
        helper.assertFalse(KledingFeature.brococoliefBeloning(player), "only once");
        helper.assertTrue(player.getInventory().countItem(ModItems.PINK_ONESIE.get()) == 1 && guh.getClothes(GuhClothes.Slot.BODY) == GuhClothes.PINK_ONESIE,
                "still one; the guh keeps its own");
        leave(helper, player);
        helper.succeed();
    }

    /** Favourite outfits: saved per player, five of them, back in one piece. */
    @GuhTest(template = EMPTY)
    public static void kledingFavorieten(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<GuhClothes> mooi = outfit(GuhClothes.PARTY_HAT, GuhClothes.SUNGLASSES, GuhClothes.OORSTRIKJE_GEEL);
        KledingFavorieten.bewaar(player, 2, mooi);
        List<String> alle = KledingFavorieten.alle(player);
        helper.assertTrue(alle.size() == KledingFavorieten.AANTAL && alle.get(0).isEmpty() && !alle.get(2).isEmpty(), "favourite 3 saved: " + alle);
        helper.assertTrue(KledingFavorieten.decodeer(alle.get(2)).equals(mooi), "and back: " + KledingFavorieten.decodeer(alle.get(2)));
        helper.assertTrue(klaar(player, "grote_guhspelen/kleding_favoriet"), "the advancement");
        KledingFavorieten.bewaar(player, 2, outfit());
        helper.assertTrue(KledingFavorieten.alle(player).get(2).isEmpty(), "forgotten again");
        helper.assertTrue(KledingFavorieten.decodeer("sunglasses,party_hat,,,,").stream().allMatch(java.util.Objects::isNull),
                "pieces in the wrong slot are dropped");
        leave(helper, player);
        helper.succeed();
    }

    /** "Get the whole outfit" advancements of other features count unlocked pieces (they aren't in your inventory any more). */
    @GuhTest(template = EMPTY)
    public static void kledingSetAdvancementsTellenUnlocks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertFalse(klaar(player, "guhmension/koning_pakje"), "not yet");
        KledingOntgrendel.ontgrendel(player, GuhClothes.KONING_KROON);
        KledingOntgrendel.ontgrendel(player, GuhClothes.KONING_MANTEL);
        helper.assertFalse(klaar(player, "guhmension/koning_pakje"), "two of three is not enough");
        KledingOntgrendel.ontgrendel(player, GuhClothes.KONING_KETTING);
        helper.assertTrue(klaar(player, "guhmension/koning_pakje"), "the whole king's outfit unlocked");
        helper.assertTrue(klaar(player, "grote_guhspelen/kleding_set"), "a whole set: Van top tot teen");
        helper.assertFalse(klaar(player, "grote_guhspelen/kleding_oren"), "nothing for the ears yet");
        KledingOntgrendel.ontgrendel(player, GuhClothes.OORSTRIKJE_MINT);
        helper.assertTrue(klaar(player, "grote_guhspelen/kleding_oren"), "ear bows");
        leave(helper, player);
        helper.succeed();
    }

    /** The beauty show's "own pieces" are your unlocks (not loaners, and only as many as fit the dressing screen). */
    @GuhTest(template = EMPTY)
    public static void kledingBeautyEigenStukken(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        KledingUnlocks.ontgrendel(player, GuhClothes.SHOWSTER_TIARA);
        KledingUnlocks.ontgrendel(player, GuhClothes.RAIN_HAT);            // (a loaner piece anyway)
        KledingUnlocks.ontgrendel(player, GuhClothes.OORSTRIKJE_ROZE);     // (not a show slot)
        List<GuhClothes.Slot> slots = List.of(nl.juiced.guhs.feature.beauty.ShowTheme.SLOTS);
        List<GuhClothes> eigen = KledingUnlocks.eigenStukken(player, slots, 11, List.of());
        helper.assertTrue(eigen.equals(List.of(GuhClothes.SHOWSTER_TIARA)), "only the unlocked non-loaner show pieces: " + eigen);
        for (GuhClothes c : GuhClothes.values()) {
            if (c.slot == GuhClothes.Slot.HEAD) {
                KledingUnlocks.voegToe(player, c);
            }
        }
        long leen = nl.juiced.guhs.feature.beauty.ShowTheme.loaners().stream().filter(c -> c.slot == GuhClothes.Slot.HEAD).count();
        int ruimte = (int) ((leen + 10) / 11 * 11 - leen);
        eigen = KledingUnlocks.eigenStukken(player, List.of(GuhClothes.Slot.HEAD), 11, List.of());
        helper.assertTrue(eigen.size() == ruimte, "no more than fit in the last row: " + eigen.size() + " / " + ruimte);
        leave(helper, player);
        helper.succeed();
    }
}
