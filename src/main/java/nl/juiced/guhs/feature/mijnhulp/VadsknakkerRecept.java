package nl.juiced.guhs.feature.mijnhulp;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 1.7.0 (ronde 31): the Vahoege Vadsknakker's recipe. A plain shapeless recipe (vadspaxel + vahoege vadsschaar + vahoege
 * vads-schoffel; it shows in the recipe book like any other), except that the result gets the enchantments of all the
 * pieces, merged like two items on an anvil ({@link nl.juiced.guhs.event.VadsAambeeld}): equal levels go up one (up to the
 * maximum), otherwise the highest level wins; what the Vadsknakker can't have, or what clashes with what it already has,
 * is left out. The highest REPAIR_COST of the pieces goes along. Data: {@code "type": "guhs:vadsknakker"} with the fields of
 * {@code minecraft:crafting_shapeless}.
 */
public class VadsknakkerRecept extends ShapelessRecipe {
    public static final MapCodec<VadsknakkerRecept> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.uitkomst),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.delen)
    ).apply(i, VadsknakkerRecept::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, VadsknakkerRecept> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ItemStackTemplate.STREAM_CODEC, o -> o.uitkomst,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.delen,
            VadsknakkerRecept::new);
    public static final RecipeSerializer<VadsknakkerRecept> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate uitkomst;
    private final List<Ingredient> delen;

    public VadsknakkerRecept(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ItemStackTemplate uitkomst, List<Ingredient> delen) {
        super(common, book, uitkomst, delen);
        this.uitkomst = uitkomst;
        this.delen = delen;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack uit = super.assemble(input);
        List<ItemStack> stukken = new ArrayList<>();
        for (ItemStack s : input.items()) {
            if (!s.isEmpty()) {
                stukken.add(s);
            }
        }
        voegSamen(uit, stukken);
        return uit;
    }

    /** Merges the enchantments of {@code stukken} (in order) into {@code uit}, the anvil way. */
    public static void voegSamen(ItemStack uit, List<ItemStack> stukken) {
        ItemEnchantments.Mutable samen = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(uit));
        int kosten = 0;
        for (ItemStack stuk : stukken) {
            kosten = Math.max(kosten, stuk.getOrDefault(DataComponents.REPAIR_COST, 0));
            for (Object2IntMap.Entry<Holder<Enchantment>> e : EnchantmentHelper.getEnchantmentsForCrafting(stuk).entrySet()) {
                Holder<Enchantment> h = e.getKey();
                if (!uit.supportsEnchantment(h)) {
                    continue;
                }
                boolean past = true;
                for (Holder<Enchantment> ander : samen.keySet()) {
                    if (!ander.equals(h) && !Enchantment.areCompatible(h, ander)) {
                        past = false;
                        break;
                    }
                }
                if (!past) {
                    continue;
                }
                int nu = samen.getLevel(h), lvl = e.getIntValue();
                int nieuw = nu == lvl ? lvl + 1 : Math.max(lvl, nu);
                samen.set(h, Math.min(nieuw, h.value().getMaxLevel()));
            }
        }
        EnchantmentHelper.setEnchantments(uit, samen.toImmutable());
        if (kosten > 0) {
            uit.set(DataComponents.REPAIR_COST, kosten);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }
}
