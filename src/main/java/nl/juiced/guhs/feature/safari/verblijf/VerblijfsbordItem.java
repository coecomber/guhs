package nl.juiced.guhs.feature.safari.verblijf;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import nl.juiced.guhs.feature.safari.Safari;

/**
 * Op Safari! step 2 (S5): the verblijfsbord item. One item for every species: the species rides along as custom data
 * ({@link #SOORT}), so the name reads "Verblijfsbord: Leeuw". Placing checks: not in the safari park itself, a species
 * that needs water (narwhal, sea cow, octopus) has enough water around, and a board without species places nothing.
 */
public class VerblijfsbordItem extends BlockItem {
    public static final String SOORT = "guhs_verblijf_soort";

    public VerblijfsbordItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static String soort(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getStringOr(SOORT, "");
    }

    public static ItemStack bord(String soort) {
        ItemStack stack = new ItemStack(VerblijfFeature.BORD_ITEM.get());
        if (!soort.isEmpty()) {
            CompoundTag t = new CompoundTag();
            t.putString(SOORT, soort);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
        return stack;
    }

    public static Component naam(String soort) {
        return Component.translatable("entity.guhs.safari." + soort);
    }

    @Override
    public Component getName(ItemStack stack) {
        String s = soort(stack);
        return s.isEmpty() ? super.getName(stack) : Component.translatable("item.guhs.safari_verblijfsbord.met", naam(s));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        String s = soort(stack);
        tooltip.accept(Component.translatable(s.isEmpty() ? "item.guhs.safari_verblijfsbord.leeg" : "item.guhs.safari_verblijfsbord.lore")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        String soort = soort(context.getItemInHand());
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer p) {
            VerblijfData.Dier d = VerblijfData.dier(soort);
            if (d == null) {
                p.sendOverlayMessage(Component.translatable("item.guhs.safari_verblijfsbord.leeg").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (Safari.echt(level)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.niet_in_park").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (d.waterNodig() > 0) {
                int water = water(level, context.getClickedPos(), VerblijfsbordBlockEntity.START_STRAAL);
                if (water < d.waterNodig()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.water_nodig", naam(soort), d.waterNodig()).withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }
        } else if (soort.isEmpty()) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    /** Water blocks in the (default) home around p. */
    static int water(ServerLevel level, BlockPos p, int r) {
        int n = 0;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                for (int dy = -Tevredenheid.ONDER; dy <= 2; dy++) {
                    var f = level.getFluidState(q.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz));
                    if (f.is(Fluids.WATER) || f.is(Fluids.FLOWING_WATER)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
