package nl.juiced.guhs.feature.spiesburcht;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;

/**
 * What can bubble in a Guhbrouwketel: the Guhdrankjes. Each one is made by stirring one ingredient into a pan of
 * kaassaus; the ingredients are item tags (data/guhs/tags/item/brouwsel/*.json), so other parts of the mod can add
 * theirs. {@code guhs:moeraskaas} (from the Kaasmoeras) is an optional entry: without it the tags still work.
 */
public enum Brouwsel {
    /** Plain kaasbouillon: the pan is filled but nothing has been stirred in yet. */
    BOUILLON(0xF2C23C),
    /** Speed and a full belly: VAHOEG! */
    VAHOEGHEID(0xF08CB4),
    /** Fire resistance: walk through the smoke (and the frying sauce). */
    ROOKLOOP(0xF0782A),
    /** guhs:stil (you can't be heard); without that effect, invisibility. */
    SLUIPKNABBEL(0x7E9A5A),
    /** Jump boost and slow falling. */
    GUHSPRONG(0x8CD2F0),
    // bbq2 (CONTRACT_130 5.4): reserved for the slices, which give them their drankje with {@link #zetDrankje}
    /** (sausdieren) from blubroom, the Sausblubje's drop. */
    BLUBROOM(0xE8A23C),
    /** (toren-peper) the hot pepper drink of the Pepertuin. */
    PEPERVUUR(0xD8322A),
    /** (toren-peper) the sweet pepper drink of the Pepertuin. */
    PEPERZOET(0xF27A8C);

    /** The effect Sluipknabbel gives: registered by another part of the mod (the Stille Voorraadkelder). */
    public static final Identifier STIL = Guhs.id("stil");

    /** bbq2: the drankjes (and their effects) of the Brouwsels that came after the first five, set by their owner slices. */
    private static final java.util.Map<Brouwsel, java.util.function.Supplier<? extends Item>> DRANKJES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Brouwsel, java.util.function.Supplier<List<MobEffectInstance>>> EFFECTEN = new java.util.concurrent.ConcurrentHashMap<>();

    public final int colour;

    Brouwsel(int colour) {
        this.colour = colour;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The item tag of the ingredient that turns kaasbouillon into this (none for the bouillon itself). */
    @Nullable
    public TagKey<Item> ingredient() {
        return this == BOUILLON ? null : TagKey.create(Registries.ITEM, Guhs.id("brouwsel/" + id()));
    }

    /** The Guhdrankje you bottle from a pan of this (empty for plain bouillon). */
    public ItemStack drankje() {
        return switch (this) {
            case BOUILLON -> ItemStack.EMPTY;
            case VAHOEGHEID -> new ItemStack(SpiesburchtFeature.DRANKJE_VAN_VAHOEGHEID.get());
            case ROOKLOOP -> new ItemStack(SpiesburchtFeature.ROOKLOOPDRANKJE.get());
            case SLUIPKNABBEL -> new ItemStack(SpiesburchtFeature.SLUIPKNABBELDRANKJE.get());
            case GUHSPRONG -> new ItemStack(SpiesburchtFeature.GUHSPRONGDRANKJE.get());
            default -> {
                java.util.function.Supplier<? extends Item> item = DRANKJES.get(this);
                yield item == null ? ItemStack.EMPTY : new ItemStack(item.get());
            }
        };
    }

    /**
     * bbq2: the Guhdrankje of a Brouwsel that has none of its own above (BLUBROOM, PEPERVUUR, PEPERZOET), from the owner slice's
     * {@code register}; its ingredients go in the item tag {@code guhs:brouwsel/<id>}. Until then the pan can't brew it.
     */
    public static void zetDrankje(Brouwsel b, java.util.function.Supplier<? extends Item> item) {
        DRANKJES.put(b, item);
    }

    /** bbq2: like {@link #zetDrankje(Brouwsel, java.util.function.Supplier)}, with the effects a {@link GuhdrankjeItem} of it gives. */
    public static void zetDrankje(Brouwsel b, java.util.function.Supplier<? extends Item> item, java.util.function.Supplier<List<MobEffectInstance>> effecten) {
        DRANKJES.put(b, item);
        EFFECTEN.put(b, effecten);
    }

    /** bbq2: can the pan brew this now (the first five always; a later one once its slice gave it a drankje)? */
    public boolean heeftDrankje() {
        return this != BOUILLON && !drankje().isEmpty();
    }

    /** What this ingredient brews (null: nothing, it's not an ingredient). */
    @Nullable
    public static Brouwsel forIngredient(ItemStack stack) {
        for (Brouwsel b : values()) {
            if (b.ingredient() != null && b.heeftDrankje() && stack.is(b.ingredient())) {
                return b;
            }
        }
        return null;
    }

    public static Brouwsel byIndex(int i) {
        return values()[Math.floorMod(i, values().length)];
    }

    /** The effect guhs:stil, when it is there (another slice registers it). */
    public static Optional<Holder.Reference<MobEffect>> stil() {
        return BuiltInRegistries.MOB_EFFECT.get(ResourceKey.create(Registries.MOB_EFFECT, STIL));
    }

    /** The effects of the drankje (3 minutes of fun, like a vanilla potion). */
    public List<MobEffectInstance> effects() {
        return switch (this) {
            case BOUILLON -> List.of();
            case VAHOEGHEID -> List.of(new MobEffectInstance(MobEffects.SPEED, 3600, 1), new MobEffectInstance(MobEffects.SATURATION, 4, 1));
            case ROOKLOOP -> List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0));
            case SLUIPKNABBEL -> List.of(stil().<MobEffectInstance>map(h -> new MobEffectInstance(h, 2400, 0))
                    .orElseGet(() -> new MobEffectInstance(MobEffects.INVISIBILITY, 1800, 0)));
            case GUHSPRONG -> List.of(new MobEffectInstance(MobEffects.JUMP_BOOST, 1800, 1), new MobEffectInstance(MobEffects.SLOW_FALLING, 1800, 0));
            default -> {
                java.util.function.Supplier<List<MobEffectInstance>> e = EFFECTEN.get(this);
                yield e == null ? List.of() : e.get();
            }
        };
    }
}
