package nl.juiced.guhs.feature.guhvillagers;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModDataComponents;

/**
 * A villager you picked up (sneak + right-click it with an empty hand; {@link VillagerOppakken}). Keeps its whole saved
 * data; right-click a block to put it down there again.
 */
public class OpgepakteVillagerItem extends Item {
    /** (item data) the villager's own name, any language. */
    public static final String NAAM = "VillagerNaam";

    public OpgepakteVillagerItem(Properties properties) {
        super(properties);
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUH_DATA.get(), CustomData.EMPTY).copyTag();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Entity villager = PickedUpGuhItem.release(level, data(context.getItemInHand()), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                context.getRotation() + 180f);
        if (villager != null) {
            level.playSound(null, pos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1f, 1f);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean kind(CompoundTag data) {
        return data.getIntOr("Age", 0) < 0;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag data = data(stack);
        if (data.contains(NAAM)) {
            return Component.translatable("item.guhs.opgepakte_villager.named", nl.juiced.guhs.taal.Tekst.get(data, NAAM));
        }
        return kind(data) ? Component.translatable("item.guhs.opgepakte_villager.kind") : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = data(stack);
        CompoundTag vd = data.getCompoundOrEmpty("VillagerData");
        Identifier beroepId = Identifier.tryParse(vd.getStringOr("profession", "minecraft:none"));
        VillagerProfession beroep = beroepId == null ? null : BuiltInRegistries.VILLAGER_PROFESSION.getValue(beroepId);
        boolean guh = "guhs:guh".equals(vd.getStringOr("type", ""));
        Component soort = Component.translatable(guh ? "item.guhs.opgepakte_villager.guh" : "item.guhs.opgepakte_villager.gewoon");
        if (beroep != null && !beroepId.getPath().equals("none") && !kind(data)) {
            int level = Math.max(1, Math.min(5, vd.getIntOr("level", 1)));
            tooltip.accept(beroep.name().copy().append(" · ").append(Component.translatable("merchant.level." + level)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.accept(soort.copy().withStyle(ChatFormatting.GRAY));
        int ruilen = data.getListOrEmpty("Offers").size();
        if (ruilen == 0 && data.getCompoundOrEmpty("Offers").contains("Recipes")) {
            ruilen = data.getCompoundOrEmpty("Offers").getListOrEmpty("Recipes").size();
        }
        if (ruilen > 0) {
            tooltip.accept(Component.translatable("item.guhs.opgepakte_villager.ruilen", ruilen).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.guhs.opgepakte_villager.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
