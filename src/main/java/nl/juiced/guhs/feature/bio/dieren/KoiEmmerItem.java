package nl.juiced.guhs.feature.bio.dieren;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluids;

/**
 * De koi-emmer: a koi in a bucket of water (a vanilla mob bucket, like the guhvis-emmer). Scoop a koi up with a water
 * bucket, empty the bucket in your own pond: the same koi (its colour, a kleintje stays a kleintje) swims there and
 * stays for good. The tooltip says which koi is inside.
 */
public class KoiEmmerItem extends MobBucketItem {
    public KoiEmmerItem(Properties properties) {
        super(DierenSlice.KOI.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties);
    }

    /** The colour of the koi inside, or null for an emmer that never held a real one (creative: a random koi comes out). */
    @javax.annotation.Nullable
    public static KoiEntity.Kleur kleur(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        return data == null ? null : KoiEntity.Kleur.van(data.copyTag().getStringOr("Kleur", ""));
    }

    public static boolean klein(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        return data != null && data.copyTag().getBooleanOr("Klein", false);
    }

    /** A koi-emmer holding a koi of this colour (tests, and a present of the visser-guh). */
    public static ItemStack met(KoiEntity.Kleur kleur, boolean klein) {
        ItemStack stack = new ItemStack(DierenSlice.KOI_EMMER.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Kleur", kleur.id());
        tag.putBoolean("Klein", klein);
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        KoiEntity.Kleur kleur = kleur(stack);
        if (kleur != null) {
            tooltip.accept(Component.translatable("item.guhs.koi_emmer.kleur", Component.translatable(kleur.langKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (klein(stack)) {
                tooltip.accept(Component.translatable("item.guhs.koi_emmer.kleintje").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        tooltip.accept(Component.translatable("item.guhs.koi_emmer.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
