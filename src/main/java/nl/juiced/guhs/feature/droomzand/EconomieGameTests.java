package nl.juiced.guhs.feature.droomzand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhSlimeEntity;
import nl.juiced.guhs.feature.dirigent.Concert;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.droomweer.DroomKistje;
import nl.juiced.guhs.feature.droomwinkel.DroomWinkel;
import nl.juiced.guhs.feature.molentje.Molen;
import nl.juiced.guhs.feature.sterrenveld.SterrenveldFeature;
import nl.juiced.guhs.feature.vliegdroom.VliegParcours;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.10.10 economie (batch "economie_1110"): the droomzand numbers, a zandhoopje once per dream, cuddling a guh slime (with its
 * saved cooldown) and the guh slimeball as a slimeball in recipes. (The golden star and the Sterrenreeks: SterrenveldGameTests.)
 */
public class EconomieGameTests {
    private static final String BATCH = "economie_1110", KAMER = "vadskracht_test_kamer";

    /** x3 droomzand, the cheaper shop, three heaps on each of the five islands. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void droomzandGetallen(GameTestHelper helper) {
        helper.assertTrue(Concert.ZAND_PER_STER == 3 && Molen.ZANDGUH_ZAND == 9 && DroomKistje.ZAND_MIN == 8 && DroomKistje.ZAND_MAX == 12
                && VliegParcours.DIPLOMA_ZAND == 15 && VliegParcours.RECORD_ZAND == 6 && SterrenveldFeature.GROTE_STER_ZAND == 9,
                "the x3 numbers");
        DroomWinkel.Aanbod potje = DroomWinkel.van("weerpotje_bellen");
        helper.assertTrue(potje != null && potje.prijs() == 25 && potje.max() == 1, "a weerpotje costs 25, once");
        int bedden = 0;
        for (DroomWinkel.Aanbod a : DroomWinkel.alle()) {
            if (a.groep().equals("bedden")) {
                helper.assertTrue(a.prijs() == 10, "a bed costs 10: " + a.id() + " " + a.prijs());
                bedden++;
            }
            if (a.groep().equals("weer")) {
                helper.assertTrue(a.prijs() == 25, "every weerpotje costs 25: " + a.id());
            }
        }
        helper.assertTrue(bedden > 0, "beds are on sale");
        Map<String, Integer> perEiland = new HashMap<>();
        for (Zandhoopjes.Hoopje h : Zandhoopjes.alle()) {
            perEiland.merge(h.eiland(), 1, Integer::sum);
            helper.assertTrue(Zandhoopjes.op(h.wereld()) == h, "a heap is found on its spot: " + h.id());
        }
        helper.assertTrue(perEiland.equals(Map.of("sterren", 3, "molentje", 3, "badje", 3, "herinnering", 3, "liedjes", 3)),
                "three heaps on each dream island: " + perEiland);
        helper.succeed();
    }

    /** A heap: 3-5 droomzand once per dream; a new dream fills it again; outside a dream it gives nothing; unbreakable. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void zandhoopjeEenKeerPerDroom(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.getInventory().clearContent();
        Item zand = DroomblokkenFeature.DROOMZAND.get();
        DroomReis.nieuweSessie(p);
        long sessie = DroomReis.sessie(p);
        int n = Zandhoopjes.schep(p, "sterren_0", sessie);
        helper.assertTrue(n >= Zandhoopjes.MIN && n <= Zandhoopjes.MAX && p.getInventory().countItem(zand) == n, "the first scoop: 3-5, got " + n);
        helper.assertTrue(Zandhoopjes.geschept(p, "sterren_0", sessie), "remembered");
        helper.assertTrue(Zandhoopjes.schep(p, "sterren_0", sessie) == 0, "the same dream: empty");
        int m = Zandhoopjes.schep(p, "sterren_1", sessie);
        helper.assertTrue(m >= Zandhoopjes.MIN, "another heap in the same dream is still full");
        DroomReis.nieuweSessie(p);
        helper.assertTrue(!Zandhoopjes.geschept(p, "sterren_0", DroomReis.sessie(p)), "a new dream: full again");
        helper.assertTrue(Zandhoopjes.schep(p, "sterren_0", DroomReis.sessie(p)) >= Zandhoopjes.MIN, "and you can scoop it");
        // the block: unbreakable, and outside a dream a click gives nothing
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, DroomzandFeature.ZANDHOOPJE.get().defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockState(pos).getDestroySpeed(helper.getLevel(), pos) < 0, "a heap can't be broken");
        p.getInventory().clearContent();
        Zandhoopjes.klik(p, pos);
        helper.assertTrue(p.getInventory().countItem(zand) == 0, "outside a dream: nothing");
        helper.succeed();
    }

    /** Cuddling: a guh slimeball, then 5 minutes uitgeknuffeld (also after saving and loading); sneaking does nothing. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhSlimeKnuffelen(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        GuhSlimeEntity slime = helper.spawn(ModEntities.GUH_SLIME.get(), new BlockPos(2, 2, 2));
        slime.setSize(2, true);
        p.setShiftKeyDown(true);
        slime.interact(p, InteractionHand.MAIN_HAND, slime.position());
        helper.assertTrue(p.getInventory().countItem(ModItems.GUH_SLIMEBALL.get()) == 0, "sneak-click: nothing (kept free for picking up)");
        p.setShiftKeyDown(false);
        helper.assertTrue(slime.interact(p, InteractionHand.MAIN_HAND, slime.position()).consumesAction(), "a cuddle is handled");
        helper.assertTrue(p.getInventory().countItem(ModItems.GUH_SLIMEBALL.get()) == 1, "a cuddle: one guh slimeball");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "not into the hand you cuddle with");
        slime.interact(p, InteractionHand.MAIN_HAND, slime.position());
        helper.assertTrue(p.getInventory().countItem(ModItems.GUH_SLIMEBALL.get()) == 1, "uitgeknuffeld: no second one");
        long wacht = slime.knuffelWacht();
        helper.assertTrue(wacht > GuhSlimeEntity.KNUFFEL_PAUZE - 20 && wacht <= GuhSlimeEntity.KNUFFEL_PAUZE, "5 minutes: " + wacht);
        // saved on the slime
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        slime.saveWithoutId(out);
        GuhSlimeEntity kopie = ModEntities.GUH_SLIME.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        kopie.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), out.buildResult()));
        helper.assertTrue(kopie.knuffelWacht() > GuhSlimeEntity.KNUFFEL_PAUZE - 20, "the cooldown is saved: " + kopie.knuffelWacht());
        // another slime is not uitgeknuffeld
        GuhSlimeEntity ander = helper.spawn(ModEntities.GUH_SLIME.get(), new BlockPos(3, 2, 3));
        ander.interact(p, InteractionHand.MAIN_HAND, ander.position());
        helper.assertTrue(p.getInventory().countItem(ModItems.GUH_SLIMEBALL.get()) == 2, "per slime: the other one gives one too");
        slime.discard();
        ander.discard();
        helper.succeed();
    }

    /** The guh slimeball is in guhs:slijmballen and c:slime_balls, fits the mod's slimeball recipes and turns into a slimeball. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhSlijmbalIsSlijmbal(GameTestHelper helper) {
        ItemStack guh = new ItemStack(ModItems.GUH_SLIMEBALL.get());
        TagKey<Item> eigen = TagKey.create(Registries.ITEM, Guhs.id("slijmballen"));
        TagKey<Item> c = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "slime_balls"));
        helper.assertTrue(guh.is(eigen) && new ItemStack(Items.SLIME_BALL).is(eigen), "both balls in guhs:slijmballen");
        helper.assertTrue(guh.is(c), "the guh slimeball in c:slime_balls");
        CraftingRecipe kussen = recept(helper, "stuiterkussen_roze");
        ItemStack blok = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Guhs.id("knuffelblok_roze")));
        helper.assertTrue(kussen.matches(CraftingInput.of(2, 1, List.of(blok, guh)), helper.getLevel()), "a stuiterkussen with a guh slimeball");
        helper.assertTrue(kussen.matches(CraftingInput.of(2, 1, List.of(blok, new ItemStack(Items.SLIME_BALL))), helper.getLevel()),
                "and still with a plain one");
        CraftingRecipe om = recept(helper, "slime_ball_from_guh_slimeball");
        CraftingInput een = CraftingInput.of(1, 1, List.of(guh));
        helper.assertTrue(om.matches(een, helper.getLevel()) && om.assemble(een).is(Items.SLIME_BALL), "one guh slimeball = one slimeball");
        helper.succeed();
    }

    private static CraftingRecipe recept(GameTestHelper helper, String id) {
        RecipeHolder<?> h = helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).orElse(null);
        helper.assertTrue(h != null && h.value() instanceof CraftingRecipe, "the recipe " + id);
        return (CraftingRecipe) h.value();
    }

    private EconomieGameTests() {
    }
}
