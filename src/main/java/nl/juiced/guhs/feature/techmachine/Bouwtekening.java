package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;

/**
 * What is drawn on a Bouwtekening: one crafting recipe, exactly as it lay on the Tekentafel. This is the value of the
 * data component {@code guhs:bouwtekening} ({@link TechmachineFeature#TEKENING}) and the format other features read
 * (the Knutselmachine now, the Bestelguh of the next update): use {@link Bouwtekeningen#lees}.
 * <ul>
 *   <li>{@code rooster}: always {@link #VAKJES} stacks, the 3 x 3 grid row by row (index = row * 3 + column); an empty
 *       cell is {@link ItemStack#EMPTY}, a filled one is ONE of the exact item (with its components) that lay there;</li>
 *   <li>{@code resultaat}: what one craft gives (the count is what one craft makes);</li>
 *   <li>{@code recept}: the id of the recipe that matched when it was drawn (a hint: the recipe is looked up again by
 *       the grid when this id is gone or no longer fits, {@link Bouwtekeningen#recept}).</li>
 * </ul>
 * Saved as {@code {rooster: [{id, count, components} or {} x 9], resultaat: {id, count, components}, recept: "ns:path"}}.
 * Never change a stack you get from here: copy it.
 */
public record Bouwtekening(List<ItemStack> rooster, ItemStack resultaat, Optional<ResourceKey<Recipe<?>>> recept) {
    public static final int BREED = 3, VAKJES = BREED * BREED;

    public static final Codec<Bouwtekening> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf(VAKJES, VAKJES).fieldOf("rooster").forGetter(Bouwtekening::rooster),
            ItemStack.CODEC.fieldOf("resultaat").forGetter(Bouwtekening::resultaat),
            ResourceKey.codec(Registries.RECIPE).optionalFieldOf("recept").forGetter(Bouwtekening::recept)
    ).apply(i, Bouwtekening::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Bouwtekening> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, Bouwtekening::rooster,
            ItemStack.STREAM_CODEC, Bouwtekening::resultaat,
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.RECIPE)), Bouwtekening::recept,
            Bouwtekening::new);

    public Bouwtekening {
        if (rooster.size() != VAKJES) {
            throw new IllegalArgumentException("Bouwtekening: the grid has " + rooster.size() + " cells, not " + VAKJES);
        }
        rooster = List.copyOf(rooster);
    }

    /** What lies in this cell (0..8; empty for an empty cell). */
    public ItemStack vakje(int nr) {
        return rooster.get(nr);
    }

    /**
     * What one craft needs, added up: one stack per different item (item + components), its count = how many cells hold
     * it. In the order of the grid. New stacks: yours to keep.
     */
    public List<ItemStack> ingredienten() {
        List<ItemStack> uit = new ArrayList<>();
        for (ItemStack cel : rooster) {
            if (cel.isEmpty()) {
                continue;
            }
            boolean erbij = false;
            for (ItemStack al : uit) {
                if (ItemStack.isSameItemSameComponents(al, cel)) {
                    al.grow(1);
                    erbij = true;
                    break;
                }
            }
            if (!erbij) {
                uit.add(cel.copyWithCount(1));
            }
        }
        return uit;
    }

    /** How many cells of the grid hold exactly this item (0: it is no ingredient). */
    public int nodig(ItemStack wat) {
        int n = 0;
        for (ItemStack cel : rooster) {
            if (!cel.isEmpty() && ItemStack.isSameItemSameComponents(cel, wat)) {
                n++;
            }
        }
        return n;
    }

    /** The grid as the input of a crafting recipe (copies of the cells). */
    public CraftingInput invoer() {
        List<ItemStack> kopie = new ArrayList<>(VAKJES);
        for (ItemStack cel : rooster) {
            kopie.add(cel.copy());
        }
        return CraftingInput.of(BREED, BREED, kopie);
    }

    // (ItemStack has no equals: a data component needs one, or two equal drawings would not stack)
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Bouwtekening b && ItemStack.listMatches(rooster, b.rooster) && ItemStack.matches(resultaat, b.resultaat)
                && recept.equals(b.recept);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * ItemStack.hashStackList(rooster) + ItemStack.hashItemAndComponents(resultaat)) + recept.hashCode();
    }
}
