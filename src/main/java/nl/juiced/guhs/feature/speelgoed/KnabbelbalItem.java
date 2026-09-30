package nl.juiced.guhs.feature.speelgoed;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** The Knabbelbal as an item: use it on the ground to put the ball down (full, unless it was picked up empty). */
public class KnabbelbalItem extends Item {
    public KnabbelbalItem(Properties properties) {
        super(properties);
    }

    /** A ball item, full or empty. */
    public static ItemStack stack(boolean vol) {
        ItemStack s = new ItemStack(SpeelgoedFeature.KNABBELBAL_ITEM.get());
        if (!vol) {
            CompoundTag t = new CompoundTag();
            t.putBoolean("Leeg", true);
            s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
        return s;
    }

    public static boolean isVol(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Leeg");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 at = context.getClickLocation();
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return InteractionResult.FAIL;
        }
        KnabbelbalEntity.maak(level, new Vec3(at.x, pos.getY() + (context.getClickedFace().getAxis().isVertical() ? 0 : 0.05), at.z),
                isVol(context.getItemInHand()));
        level.playSound(null, pos, SpeelgoedFeature.BAL.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(isVol(stack) ? "item.guhs.knabbelbal.vol" : "item.guhs.knabbelbal.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.guhs.knabbelbal.uitleg").withStyle(ChatFormatting.GRAY));
    }
}
