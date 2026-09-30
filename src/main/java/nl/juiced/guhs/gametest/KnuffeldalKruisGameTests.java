package nl.juiced.guhs.gametest;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bakkerij.Bakken;
import nl.juiced.guhs.feature.kamperen.KampvuurMarshmallow;
import nl.juiced.guhs.feature.knuffeldal.Burgemeester;
import nl.juiced.guhs.feature.knuffeldal.Feestbuffet;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.theehuis.TheeBlocks;
import nl.juiced.guhs.feature.theehuis.Theekransje;

/**
 * 2.8 merge QA: the cross-slice paths that no single slice could test on its own (KNUFFEL_CONTRACT §7/§11). Every
 * {@code #guhs:knus/*} tag holds the real items of its owner, the consumers accept them (bakery ingredients, teas,
 * recipes, the grijpmachine, the feestbuffet, marshmallows at the campfire, and (TheehuisKruisGameTests) home-baked cake at the theekransje), and
 * the Grote Knusfeest takes the six real feest-items.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class KnuffeldalKruisGameTests {
    private static final String EMPTY = "empty";

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(Guhs.id(id));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(item(id));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.server.getAdvancements().get(Guhs.id(name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Every Knus tag holds what its owner promised (§7), and every id exists. */
    @GameTest(template = EMPTY)
    public static void knuffeldal28TagsZijnGevuld(GameTestHelper helper) {
        Object[][] tags = {
                {KnusTags.KAASMELK, List.of("kaasmelk")}, {KnusTags.KNABBELEI, List.of("knabbelei")}, {KnusTags.PLUISWOL, List.of("pluiswol")},
                {KnusTags.THEEKRUID, List.of("theekruid")}, {KnusTags.KNABBELGRAAN, List.of("knabbelgraan")}, {KnusTags.GUHBLOEM, List.of("guhbloemetje")},
                {KnusTags.OOGST, List.of("knabbelgraan", "theekruid", "guhbloemetje")},
                {KnusTags.GEBAK, List.of("knabbelbroodje", "kaaskrakeling", "vadsvlaai", "guhcroissant", "knabbelkoekje", "kaasbolletje", "pluismuffin",
                        "theetaartje", "knabbeltompouce", "vadsdonut", "guhwafel", "sterrenkoekje", "feesttaart")},
                {KnusTags.THEE, List.of("knabbelthee", "kaasmelkthee", "theekruidthee", "guhbloementhee")},
                {KnusTags.MARSHMALLOW, List.of("marshmallow_knabbel")},
                {KnusTags.LEKKERNIJ, List.of("kaas_knabbels", "knabbelbroodje", "feesttaart")},
                {KnusTags.GRIJPTICKETS, List.of("kermisbon", "bakmunt", "speenmunt", "krulmunt", "eendjesmunt")},
                {KnusTags.FEESTTAART, List.of("feesttaart")}, {KnusTags.THEESERVIES, List.of("feest_theeservies")},
                {KnusTags.FEESTKAPSELS, List.of("feestkapselset")}, {KnusTags.FEESTSLINGERS, List.of("feestslingers")},
                {KnusTags.FEESTBLOEMEN, List.of("feestboeket")}, {KnusTags.STERRENLANTAARNS, List.of("sterrenlantaarn")},
        };
        for (Object[] row : tags) {
            @SuppressWarnings("unchecked")
            TagKey<Item> tag = (TagKey<Item>) row[0];
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) row[1];
            for (String id : ids) {
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Guhs.id(id)), "item guhs:" + id + " exists");
                helper.assertTrue(stack(id).is(tag), "guhs:" + id + " is in #" + tag.location());
            }
        }
        // the grijpmachine's tickets are not the Ballonfestival/Sterrenwacht coins (they have no machine money)
        helper.assertTrue(!stack("ballonmunt").is(KnusTags.GRIJPTICKETS) && !stack("wensster").is(KnusTags.GRIJPTICKETS), "no ballonmunt/wensster tickets");
        // the plushies: 21 variants (2.9: + the Pinguh) + the golden one, all in the block tag the guhs cuddle
        int knuffels = 0;
        for (Block b : BuiltInRegistries.BLOCK) {
            if (b.defaultBlockState().is(KnusTags.KNUFFELS)) {
                knuffels++;
            }
        }
        helper.assertTrue(knuffels == 22, "22 plushies in #guhs:knus/knuffels: " + knuffels);
        helper.succeed();
    }

    /** The consumers take the real products: bakery ingredients are 'fresh', the tea pot knows them, recipes use them. */
    @GameTest(template = EMPTY)
    public static void knuffeldal28ProductenWordenGebruikt(GameTestHelper helper) {
        // the Knabbelbakkerij: farm and garden products are fresh ingredients
        helper.assertTrue(Bakken.Nodig.GRAAN.past(stack("knabbelgraan")) && Bakken.Nodig.KAASMELK.past(stack("kaasmelk"))
                && Bakken.Nodig.EI.past(stack("knabbelei")), "knabbelgraan, kaasmelk and knabbelei go into the dough");
        // the theepotje: kaasmelk, theekruid and guhbloemetjes make their own tea
        helper.assertTrue(TheeBlocks.thee(stack("kaasmelk")) == TheeBlocks.Soort.KAASMELKTHEE, "kaasmelk -> kaasmelkthee");
        helper.assertTrue(TheeBlocks.thee(stack("theekruid")) == TheeBlocks.Soort.THEEKRUIDTHEE, "theekruid -> theekruidthee");
        helper.assertTrue(TheeBlocks.thee(stack("guhbloemetje")) == TheeBlocks.Soort.GUHBLOEMENTHEE, "guhbloemetje -> guhbloementhee");
        helper.assertTrue(Theekransje.isGebak(stack("vadsdonut")) && Theekransje.isGebak(stack("feesttaart")), "bakery cakes on the tea table");
        // the feestbuffet: baked, grown and poured
        for (String id : List.of("knabbelbroodje", "theekruid", "knabbelgraan", "guhbloemetje", "kaasmelkthee", "kaasmelk")) {
            helper.assertTrue(Feestbuffet.buffetEten(stack(id)), id + " goes on the feestbuffet");
        }
        // marshmallows at the campfire
        helper.assertTrue(KampvuurMarshmallow.isMarshmallow(stack("marshmallow_knabbel")), "a marshmallow_knabbel roasts at the campfire");
        // recipes with a Knus tag accept the real product
        var recipes = helper.getLevel().getRecipeManager();
        var access = helper.getLevel().registryAccess();
        Object[][] need = {{"knuffeldekentje", "pluiswol"}, {"guh_slaapzak", "pluiswol"}, {"babyflesje", "kaasmelk"}};
        for (Object[] n : need) {
            Item result = item((String) n[0]);
            ItemStack product = stack((String) n[1]);
            boolean found = false;
            for (var holder : recipes.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
                CraftingRecipe r = holder.value();
                if (r.getResultItem(access).is(result)) {
                    for (Ingredient ing : r.getIngredients()) {
                        found |= ing.test(product);
                    }
                }
            }
            helper.assertTrue(found, "a recipe for " + n[0] + " takes " + n[1]);
        }
        helper.succeed();
    }

    /** A real marshmallow_knabbel roasted over a burning campfire: a bite, the Knus counter, the guhs come and sit. */
    @GameTest(template = EMPTY)
    public static void knuffeldal28MarshmallowBijHetKampvuur(GameTestHelper helper) {
        BlockPos vuur = new BlockPos(2, 1, 2);
        helper.setBlock(vuur, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ServerPlayer p = player(helper, new BlockPos(3, 1, 3));
        try {
            p.getFoodData().setFoodLevel(10);
            ItemStack mm = stack("marshmallow_knabbel").copyWithCount(2);
            int n = KampvuurMarshmallow.rooster(p, helper.absolutePos(vuur), mm);
            helper.assertTrue(n == 1 && mm.getCount() == 1 && KnusVoortgang.teller(p, KampvuurMarshmallow.GEROOSTERD) == 1, "roasted and eaten");
            helper.assertTrue(p.getFoodData().getFoodLevel() > 10, "a warm sticky bite");
        } finally {
            leave(helper, p);
            helper.setBlock(vuur, Blocks.AIR);
        }
        helper.succeed();
    }

    /** The Grote Knusfeest with the six real feest-items: the Burgemeester takes them all (and the lanterns count as made). */
    @GameTest(template = EMPTY)
    public static void knuffeldal28KnusfeestMetEchteSpullen(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 1, 1));
        try {
            Knusfeest.vergeet(p);
            Knusfeest.nieuweRonde(p, 0, EnumSet.allOf(Feesttaak.class));
            for (String id : List.of("feesttaart", "feest_theeservies", "feestkapselset", "feestslingers", "feestboeket", "sterrenlantaarn")) {
                p.getInventory().add(stack(id));
            }
            for (Feesttaak t : Feesttaak.values()) {
                helper.assertTrue(Knusfeest.open(p, t), "asked: " + t.id());
            }
            helper.assertTrue(Burgemeester.lever(p) == 6, "the Burgemeester takes all six feest-items");
            for (Feesttaak t : Feesttaak.values()) {
                helper.assertTrue(Knusfeest.gebracht(p, t), "brought: " + t.id());
            }
            helper.assertTrue(Knusfeest.alleGebracht(p), "everything for the feast is there");
            helper.assertTrue(p.getInventory().isEmpty(), "the items are handed in");
            helper.assertTrue(advancement(p, "quest/knusfeest_sterrenlantaarns_gemaakt"), "handed-in lanterns count as made too");
        } finally {
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        helper.succeed();
    }
}
