package nl.juiced.guhs.feature.waterdiertjes;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.piep.PiepDierItem;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * Het guhxolotl-emmertje: a guhxolotl in a bucket of water (a {@link PiepDierItem}, so "waar is hij", the guhhuisje and the
 * band id all work). Scooped up with a water bucket, or picked up by its owner (sneak + empty hand, or "Oppakken" in its
 * menu). Use it on a block: the guhxolotl plops out there, exactly as it was, with a splash of water (sneak: without the
 * water). An emmertje that was a real water bucket gives the empty bucket back; one made by picking it up by hand is a
 * little knuffel-emmertje that simply melts away ("MetEmmer" in its data).
 */
public class GuhxolotlEmmertje extends PiepDierItem {
    /** Custom data: this emmertje was a real water bucket (you get the empty bucket back). */
    public static final String MET_EMMER = "guhs_waterdiertjes_emmer";

    public GuhxolotlEmmertje(Properties properties) {
        super(() -> WaterdiertjesFeature.GUHXOLOTL.get(), properties);
    }

    /** The emmertje holding this guhxolotl (it stays in the world: the caller removes it). */
    public static ItemStack vol(GuhxolotlEntity x, boolean metEmmer) {
        ItemStack stack = PiepDierItem.van(x, WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get());
        if (metEmmer) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MET_EMMER, true));
        }
        return stack;
    }

    public static boolean metEmmer(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(MET_EMMER, false);
    }

    /** The colour of the guhxolotl inside (roze for an empty/new one). */
    public static GuhxolotlEntity.Kleur kleur(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? GuhxolotlEntity.Kleur.ROZE : GuhxolotlEntity.Kleur.van(data.copyTag().getStringOr("Kleur", ""));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() && !level.getBlockState(clicked).liquid()
                ? clicked : clicked.relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        boolean water = player == null || !player.isSecondaryUseActive();
        BlockState at = level.getBlockState(pos);
        if (water && !level.dimensionType().ultraWarm() && (at.isAir() || at.canBeReplaced()) && at.getFluidState().isEmpty()) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 11);
        }
        boolean emmer = metEmmer(stack);
        GuhxolotlEntity x = PiepDierItem.zetNeer(stack, level, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                player == null ? 0 : player.getYRot() + 180, WaterdiertjesFeature.GUHXOLOTL.get());
        if (x == null) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_AXOLOTL, SoundSource.NEUTRAL, 1f, 1f);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.neergezet.guhxolotl", x.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        stack.shrink(1);
        if (emmer && player != null && !player.hasInfiniteMaterials()) {
            ItemStack leeg = new ItemStack(Items.BUCKET);
            if (stack.isEmpty()) {
                player.setItemInHand(context.getHand(), leeg);
            } else if (!player.getInventory().add(leeg)) {
                player.drop(leeg, false);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.guhs.guhxolotl_emmertje.met", Component.translatable(kleur(stack).langKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("Owner")) {
                tooltip.accept(Component.translatable("item.guhs.guhxolotl_emmertje.getemd").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (tag.getIntOr("Age", 0) < 0) {
                tooltip.accept(Component.translatable("item.guhs.guhxolotl_emmertje.kleintje").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        tooltip.accept(Component.translatable("item.guhs.guhxolotl_emmertje.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
