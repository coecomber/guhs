package nl.juiced.guhs.feature.guhwinter.spellen;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A chocoladeletter: the first letter of a name, in chocolate (edible). The letter is a custom-model-data string (the
 * item model shows that letter; tools/features/guhwinter_spellen.py draws all 26) and it is in the name: "Chocoladeletter
 * J". The pepernoten shop gives the buyer's own letter; the story's schoentje can use {@link #maak} as well.
 */
public class ChocoladeletterItem extends Item {
    public ChocoladeletterItem(Properties properties) {
        super(properties);
    }

    /** The letter of a name: its first letter A-Z (accents dropped); a name without one gets a G (of guh). */
    public static String letter(String naam) {
        String plat = Normalizer.normalize(naam == null ? "" : naam, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        for (int i = 0; i < plat.length(); i++) {
            char c = plat.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                return String.valueOf(c);
            }
        }
        return "G";
    }

    /** A chocoladeletter with the first letter of this name. */
    public static ItemStack maak(String naam) {
        ItemStack s = new ItemStack(SpellenFeature.CHOCOLADELETTER.get());
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(letter(naam)), List.of()));
        return s;
    }

    /** The letter of this stack, or null for a plain one. */
    public static String letterVan(ItemStack s) {
        CustomModelData d = s.get(DataComponents.CUSTOM_MODEL_DATA);
        return d == null || d.strings().isEmpty() ? null : d.strings().get(0);
    }

    @Override
    public Component getName(ItemStack stack) {
        String l = letterVan(stack);
        return l == null ? super.getName(stack) : Component.translatable("item.guhs.guhwinter_chocoladeletter.met", l);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guhwinter_chocoladeletter.lore").withStyle(ChatFormatting.GRAY));
    }
}
