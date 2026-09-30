package nl.juiced.guhs.feature.mewtwo;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * A part for the kloontank (soort 1..4 in its custom data {@code guhs_mewtwo_onderdeel}; its own name and picture through
 * custom_model_data): 1 a curved glass panel, 2 a copper knabbel tube, 3 a little bubble pump, 4 a bottle of pink knabbelsap.
 * Found in the parts crates ({@link MewtwoBlokken.Onderdelenkist}), built in with a click on the tank.
 */
public class TankonderdeelItem extends Item {
    public static final String SOORT = "guhs_mewtwo_onderdeel";

    public TankonderdeelItem(Properties properties) {
        super(properties);
    }

    public static ItemStack maak(int n) {
        ItemStack s = new ItemStack(MewtwoFeature.TANKONDERDEEL.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(SOORT, n);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(java.util.List.of((float) n), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        s.set(DataComponents.ITEM_NAME, Component.translatable("item.guhs.mewtwo_tankonderdeel." + n));
        return s;
    }

    public static int soort(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getIntOr(SOORT, 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int n = soort(stack);
        if (n >= 1 && n <= MewtwoFeature.ONDERDELEN) {
            tooltip.accept(Component.translatable("item.guhs.mewtwo_tankonderdeel." + n + ".lore").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.guhs.mewtwo_tankonderdeel.lore").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
