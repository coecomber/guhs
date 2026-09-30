package nl.juiced.guhs.feature.vissen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A fish of the Guhvis-wedstrijd: a trophy you keep, and you can eat it (raw!). The Guhpuffer puffs you up into the
 * air, the Mika-meerval tastes like Mika (bah) and the Gouden Guhvis makes you feel golden.
 */
public class VisItem extends Item {
    public final VisSoort soort;

    public VisItem(VisSoort soort, Properties properties) {
        super(properties.rarity(soort.rarity).food(food(soort)));
        this.soort = soort;
    }

    private static FoodProperties food(VisSoort soort) {
        return switch (soort) {
            case KAASVIS -> new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build();
            case VADSBAARS -> new FoodProperties.Builder().nutrition(6).saturationModifier(0.5f).build();
            case GUHPUFFER -> new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build();
            case NJEGFOREL -> new FoodProperties.Builder().nutrition(5).saturationModifier(0.8f).build();
            case MIKA_MEERVAL -> new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 20 * 8), 1f)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 20 * 15), 1f).build();
            case GOUDEN_GUHVIS -> new FoodProperties.Builder().nutrition(8).saturationModifier(1.2f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1), 1f)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1), 1f).build();
        };
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && soort == VisSoort.GUHPUFFER) {
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 6));
            level.playSound(null, entity.blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 1f, 1.2f);
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("item.guhs.vissen.puffer_eaten").withStyle(ChatFormatting.GREEN), true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** Marks a caught Gouden Guhvis as a collector's piece: caught by whom, how heavy, and the how-manieth of theirs. */
    public static void stamp(ItemStack stack, String angler, int grams, int nr) {
        CompoundTag tag = new CompoundTag();
        tag.putString("GuhvisVanger", angler);
        tag.putInt("GuhvisGram", grams);
        tag.putInt("GuhvisNr", nr);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** The Gouden Guhvis always shines. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return soort == VisSoort.GOUDEN_GUHVIS || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.guhs.vissen.fish_info", Component.translatable("gui.guhs.vissen.rarity." + soort.id()),
                soort.isBad() ? String.valueOf(soort.base) : "+" + soort.base).withStyle(soort.colour));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("GuhvisVanger")) {
            tooltip.add(Component.translatable("item.guhs.vissen.golden_caught", tag.getString("GuhvisVanger"),
                    VisSoort.kg(tag.getInt("GuhvisGram")), tag.getInt("GuhvisNr")).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        } else if (soort == VisSoort.GOUDEN_GUHVIS) {
            tooltip.add(Component.translatable("item.guhs.vissen.golden_collect").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }
}
